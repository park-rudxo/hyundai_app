package io.changyeoga.app

import android.os.SystemClock
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.util.Collections
import java.util.Date
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager
import javax.security.auth.x500.X500Principal

private const val PROTOCOL = "CHANGYEOGA/1"
private const val MAX_ARCHIVE_BYTES = 22 * 1024 * 1024
private const val SESSION_MS = 5 * 60 * 1000L

data class PeerAddress(val host: String, val port: Int) {
    companion object {
        fun parse(text: String): PeerAddress {
            val parts = text.trim().split(':')
            require(parts.size == 2) { "휴대폰에 표시된 주소와 포트를 입력해 주세요." }
            val octets = parts[0].split('.')
            require(octets.size == 4 && octets.all { it.matches(Regex("[0-9]{1,3}")) && it.toInt() in 0..255 })
            val host = octets.joinToString(".") { it.toInt().toString() }
            val address = InetAddress.getByName(host)
            require(address.isSiteLocalAddress || address.isLinkLocalAddress || (BuildConfig.DEBUG && address.isLoopbackAddress)) { "같은 네트워크의 휴대폰 주소를 입력해 주세요." }
            val port = parts[1].toIntOrNull() ?: error("포트를 확인해 주세요.")
            require(port in 1024..65535)
            return PeerAddress(host,port)
        }
    }
}

fun certificateFingerprint(certificate: X509Certificate): String =
    MessageDigest.getInstance("SHA-256").digest(certificate.encoded).joinToString("") { "%02X".format(it.toInt() and 255) }

fun verificationWords(fingerprint: String): String = fingerprint.take(24).chunked(4).joinToString(" ")

/** A frozen snapshot, one successful download, five guesses, five minutes. */
class LocalTransferServer private constructor(
    private val archive: ByteArray,
    private val socket: SSLServerSocket,
    val fingerprint: String,
    val code: String,
    private val changed: (String) -> Unit,
    private val lifetimeMs: Long,
) : Closeable {
    val port: Int get() = socket.localPort
    private val createdAt = SystemClock.elapsedRealtime()
    private val worker = Executors.newSingleThreadExecutor()
    private val timer = Executors.newSingleThreadScheduledExecutor()
    @Volatile private var closed = false
    @Volatile private var active: SSLSocket? = null
    private var attempts = 0
    private var consumed = false

    private fun start(): LocalTransferServer {
        timer.schedule({ finish("연결 시간이 끝났습니다. 다시 보내기를 시작해 주세요.") },lifetimeMs,TimeUnit.MILLISECONDS)
        worker.execute {
            while(!closed) {
                try {
                    val client = socket.accept() as SSLSocket
                    active = client
                    client.use {
                        it.soTimeout = 6000
                        it.startHandshake()
                        val input = DataInputStream(it.inputStream)
                        // Inspection connections send nothing and disclose no trip data.
                        if(input.readUTF() != PROTOCOL) return@use
                        val supplied = input.readUTF()
                        val output = DataOutputStream(it.outputStream)
                        if(closed || consumed || SystemClock.elapsedRealtime()-createdAt >= lifetimeMs) return@use
                        if(!MessageDigest.isEqual(supplied.toByteArray(),code.toByteArray())) {
                            attempts++
                            output.writeUTF("DENIED"); output.flush()
                            if(attempts >= 5) finish("연결 코드가 5회 틀려 보내기를 종료했습니다.")
                            return@use
                        }
                        consumed = true
                        output.writeUTF("OK")
                        output.writeInt(archive.size)
                        output.write(archive)
                        output.flush()
                        // Acknowledgement means the receiver downloaded all bytes, not applied them.
                        val acknowledged = input.readUTF() == "RECEIVED"
                        finish(if(acknowledged) "파일 전송 완료 · 차량에서 가져오기 확인이 필요합니다." else "전송 확인을 받지 못했습니다. 다시 시작해 주세요.")
                    }
                } catch(e: Exception) {
                    if(BuildConfig.DEBUG && e is javax.net.ssl.SSLException) android.util.Log.w("LocalTransfer", "TLS handshake failed", e)
                    if(consumed && !closed) finish("전송이 중단됐습니다. 다시 보내기를 시작해 주세요.")
                } finally { active = null }
            }
        }
        return this
    }

    private fun finish(message: String) { if(!closed) { close(); changed(message) } }
    override fun close() {
        closed = true
        runCatching { active?.close() }
        runCatching { socket.close() }
        timer.shutdownNow()
        worker.shutdownNow()
        archive.fill(0)
    }

    companion object {
        @Synchronized private fun serverContext(): Pair<SSLContext,String> {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val alias = "changyeoga-local-transfer-v2"
            if(!keyStore.containsAlias(alias)) {
                val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC,"AndroidKeyStore")
                generator.initialize(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                    // Conscrypt hashes the TLS transcript before invoking NONEwithECDSA.
                    .setDigests(KeyProperties.DIGEST_NONE,KeyProperties.DIGEST_SHA256,KeyProperties.DIGEST_SHA384,KeyProperties.DIGEST_SHA512)
                    .setCertificateSubject(X500Principal("CN=Changyeoga Local Transfer"))
                    .setCertificateNotBefore(Date(System.currentTimeMillis()-86400000L))
                    .setCertificateNotAfter(Date(System.currentTimeMillis()+10L*365*86400000L))
                    .build())
                generator.generateKeyPair()
            }
            val manager = object : X509ExtendedKeyManager() {
                override fun getPrivateKey(name: String?) = if(name == alias) keyStore.getKey(alias,null) as java.security.PrivateKey else null
                override fun getCertificateChain(name: String?) = if(name == alias) arrayOf(keyStore.getCertificate(alias) as X509Certificate) else null
                override fun getServerAliases(type: String?, issuers: Array<java.security.Principal>?) = if(type == "EC") arrayOf(alias) else null
                override fun chooseServerAlias(type: String?, issuers: Array<java.security.Principal>?, socket: java.net.Socket?) = if(type == "EC") alias else null
                override fun chooseEngineServerAlias(type: String?, issuers: Array<java.security.Principal>?, engine: javax.net.ssl.SSLEngine?) = if(type == "EC") alias else null
                override fun getClientAliases(type: String?, issuers: Array<java.security.Principal>?) : Array<String>? = null
                override fun chooseClientAlias(types: Array<String>?, issuers: Array<java.security.Principal>?, socket: java.net.Socket?) : String? = null
            }
            val context = SSLContext.getInstance("TLS").apply { init(arrayOf(manager),null,SecureRandom()) }
            return context to certificateFingerprint(keyStore.getCertificate(alias) as X509Certificate)
        }
        fun start(archive: ByteArray, lifetimeMs: Long = SESSION_MS, changed: (String)->Unit = {}): LocalTransferServer {
            require(archive.size in 1..MAX_ARCHIVE_BYTES)
            val (context,fingerprint) = serverContext()
            val socket = context.serverSocketFactory.createServerSocket(0) as SSLServerSocket
            socket.enabledProtocols = arrayOf("TLSv1.2")
            val code = "%06d".format(java.util.Locale.US,SecureRandom().nextInt(1_000_000))
            return LocalTransferServer(archive.copyOf(),socket,fingerprint,code,changed,lifetimeMs).start()
        }
        fun localAddresses(): List<String> = Collections.list(NetworkInterface.getNetworkInterfaces()).flatMap {
            Collections.list(it.inetAddresses)
        }.filter { it is Inet4Address && !it.isLoopbackAddress && (it.isSiteLocalAddress || it.isLinkLocalAddress) }.map { it.hostAddress!! }.distinct()
    }
}

object LocalTransferClient {
    private fun connect(peer: PeerAddress, pin: String?): SSLSocket {
        if(pin != null) require(pin.matches(Regex("[A-F0-9]{64}")))
        val trust = object: X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) { throw CertificateException("Client certificates not supported") }
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                if(chain.isEmpty()) throw CertificateException("Missing certificate")
                chain[0].checkValidity()
                if(pin != null && certificateFingerprint(chain[0]) != pin) throw CertificateException("Phone identity changed")
            }
        }
        val context = SSLContext.getInstance("TLS").apply { init(null,arrayOf(trust),SecureRandom()) }
        val socket = context.socketFactory.createSocket() as SSLSocket
        try {
            socket.enabledProtocols = arrayOf("TLSv1.2")
            socket.soTimeout = 15000
            socket.connect(InetSocketAddress(peer.host,peer.port),5000)
            socket.startHandshake()
            return socket
        } catch(e: Exception) { socket.close(); throw e }
    }
    /** No code, personal data or HTTP request is sent before the user compares words. */
    fun inspect(peer: PeerAddress): String = connect(peer,null).use {
        certificateFingerprint(it.session.peerCertificates[0] as X509Certificate)
    }
    fun download(peer: PeerAddress, pin: String, code: String): ByteArray {
        require(code.matches(Regex("[0-9]{6}")))
        return connect(peer,pin).use {
            val output = DataOutputStream(it.outputStream)
            output.writeUTF(PROTOCOL); output.writeUTF(code); output.flush()
            val input = DataInputStream(it.inputStream)
            require(input.readUTF() == "OK") { "연결 코드가 맞지 않습니다." }
            val size = input.readInt()
            require(size in 1..MAX_ARCHIVE_BYTES)
            val bytes = ByteArray(size)
            input.readFully(bytes)
            output.writeUTF("RECEIVED"); output.flush()
            bytes
        }
    }
}
