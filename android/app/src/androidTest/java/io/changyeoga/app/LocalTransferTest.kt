package io.changyeoga.app

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

class LocalTransferTest {
    private fun isolatedStore(): TripStore {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val folder=File(context.cacheDir,"transfer-test-${UUID.randomUUID()}").apply { mkdirs() }
        return TripStore(object: ContextWrapper(context) { override fun getFilesDir() = folder })
    }

    @Test fun encryptedSnapshotAcrossIndependentStores() {
        val sender=isolatedStore()
        val receiver=isolatedStore()
        val bitmap=Bitmap.createBitmap(50,50,Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.BLUE) }
        val photo=sender.savePhoto(bitmap)
        bitmap.recycle()
        val original=Trip.example().addItem(Luggage(stopId="grandma",photo=photo)).changeState("fruit",ItemState.LOADED)
        val output=ByteArrayOutputStream()
        sender.exportTrip(original,output)
        LocalTransferServer.start(output.toByteArray()).use { server ->
            val peer=PeerAddress("127.0.0.1",server.port)
            val pin=LocalTransferClient.inspect(peer)
            assertEquals(server.fingerprint,pin)
            val wrong=if(server.code=="000000") "000001" else "000000"
            assertTrue(runCatching { LocalTransferClient.download(peer,pin,wrong) }.isFailure)
            val received=LocalTransferClient.download(peer,pin,server.code)
            assertArrayEquals(output.toByteArray(),received)
            val imported=receiver.importTrip(received.inputStream())
            assertEquals(original.stops,imported.stops)
            assertEquals(original.items.map { it.state },imported.items.map { it.state })
            assertArrayEquals(sender.photoFile(photo).readBytes(),receiver.photoFile(imported.items.last().photo!!).readBytes())
            assertEquals(imported,receiver.load())
            assertTrue(runCatching { LocalTransferClient.download(peer,pin,server.code) }.isFailure)
        }
    }

    @Test fun wrongCertificateCannotFetchAndDoesNotConsumeCode() {
        LocalTransferServer.start(byteArrayOf(1,2,3)).use { server ->
            val peer=PeerAddress("127.0.0.1",server.port)
            assertTrue(runCatching { LocalTransferClient.download(peer,"0".repeat(64),server.code) }.isFailure)
            assertArrayEquals(byteArrayOf(1,2,3),LocalTransferClient.download(peer,server.fingerprint,server.code))
        }
    }

    @Test fun fiveWrongCodesEndTheSession() {
        LocalTransferServer.start(byteArrayOf(1)).use { server ->
            val peer=PeerAddress("127.0.0.1",server.port)
            assertEquals(server.fingerprint,LocalTransferClient.inspect(peer))
            val wrong=if(server.code=="000000") "000001" else "000000"
            repeat(5) {
                val failure=runCatching { LocalTransferClient.download(peer,server.fingerprint,wrong) }.exceptionOrNull()
                assertTrue(failure is IllegalArgumentException)
            }
            assertTrue(runCatching { LocalTransferClient.download(peer,server.fingerprint,server.code) }.isFailure)
        }
    }

    @Test fun expiryAndExplicitCloseStopListening() {
        val server=LocalTransferServer.start(byteArrayOf(1),lifetimeMs=100)
        val peer=PeerAddress("127.0.0.1",server.port)
        Thread.sleep(200)
        assertTrue(runCatching { LocalTransferClient.inspect(peer) }.isFailure)
        server.close()
        val second=LocalTransferServer.start(byteArrayOf(1))
        val other=PeerAddress("127.0.0.1",second.port)
        second.close()
        assertTrue(runCatching { LocalTransferClient.inspect(other) }.isFailure)
    }

    @Test fun addressInputIsLocalAndBounded() {
        assertEquals(PeerAddress("192.168.0.2",9000),PeerAddress.parse("192.168.0.2:9000"))
        listOf("8.8.8.8:9000","example.com:9000","192.168.0.2:0","192.168.0.999:9000","0.0.0.0:9000").forEach {
            assertTrue(runCatching { PeerAddress.parse(it) }.isFailure)
        }
    }
}
