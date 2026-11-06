package io.changyeoga.app

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WirelessTransferDialog(
    sender: Boolean,
    snapshot: suspend () -> ByteArray,
    applyArchive: suspend (ByteArray) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val owner = LocalLifecycleOwner.current
    val window = (LocalContext.current as? Activity)?.window
    var server by remember { mutableStateOf<LocalTransferServer?>(null) }
    var addresses by remember { mutableStateOf<List<String>>(emptyList()) }
    var status by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var peer by remember { mutableStateOf<PeerAddress?>(null) }
    var pin by remember { mutableStateOf<String?>(null) }
    var foreground by remember { mutableStateOf(true) }

    DisposableEffect(owner,window) {
        val keptAwake = window?.attributes?.flags?.and(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val observer = LifecycleEventObserver { _, event ->
            if(event == Lifecycle.Event.ON_STOP) {
                foreground = false
                server?.close(); server = null
                status = "화면을 나가 연결을 종료했습니다. 다시 시작해 주세요."
            } else if(event == Lifecycle.Event.ON_START) foreground = true
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            server?.close()
            owner.lifecycle.removeObserver(observer)
            if(!keptAwake) window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    AlertDialog(
        onDismissRequest = { if(!working) onDismiss() },
        title = { Text(if(sender) "차량으로 보내기" else "휴대폰에서 받기") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max=540.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("휴대폰과 차량을 같은 Wi-Fi 또는 핫스팟에 연결해 주세요.")
                if(sender) {
                    Text("이 화면을 열어 두세요. 시작한 시점의 목록을 한 번 보내며, 이후 수정 내용은 다시 보내야 합니다.")
                    val active = server
                    if(active != null) {
                        Text("차량에 입력할 주소",fontWeight=FontWeight.Bold)
                        addresses.forEach { Text("$it:${active.port}") }
                        Text("연결 코드  ${active.code}",fontWeight=FontWeight.Bold)
                        Text("두 화면에서 비교할 확인 문구")
                        Text(verificationWords(active.fingerprint),fontWeight=FontWeight.Bold)
                        Text("5분 동안 유효 · 한 번 전송하면 종료")
                        OutlinedButton(onClick={active.close();server=null;status="보내기를 종료했습니다."}) { Text("보내기 종료") }
                    } else {
                        Button(enabled=!working,onClick={
                            working=true; status="목록과 사진을 준비하고 있습니다."
                            scope.launch {
                                var created: LocalTransferServer? = null
                                try {
                                    addresses=withContext(Dispatchers.IO) { LocalTransferServer.localAddresses() }
                                    require(addresses.isNotEmpty()) { "Wi-Fi 연결을 확인해 주세요." }
                                    val bytes=snapshot()
                                    try {
                                        created=withContext(Dispatchers.IO) {
                                            created=LocalTransferServer.start(bytes) { message -> scope.launch { server=null; status=message } }
                                            created!!
                                        }
                                    } finally { bytes.fill(0) }
                                    ensureActive()
                                    if(!foreground) { created?.close(); status="화면으로 돌아온 뒤 다시 시작해 주세요." }
                                    else { server=created;status="연결 대기 중" }
                                } catch(e: CancellationException) { created?.close();throw e }
                                catch(_:Exception) { created?.close();status="보내기를 시작하지 못했습니다. 네트워크와 목록 크기를 확인해 주세요." }
                                finally { working=false }
                            }
                        }) { Text(if(working) "준비 중" else "보내기 시작") }
                    }
                } else {
                    OutlinedTextField(value=address,onValueChange={address=it.take(30);pin=null;peer=null},enabled=!working,label={Text("휴대폰 주소:포트")},placeholder={Text("192.168.0.10:12345")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Uri))
                    if(pin == null) {
                        Button(enabled=!working&&address.isNotBlank(),onClick={
                            working=true;status="휴대폰을 확인하고 있습니다."
                            scope.launch {
                                try {
                                    val target=PeerAddress.parse(address)
                                    val observed=withContext(Dispatchers.IO) { LocalTransferClient.inspect(target) }
                                    peer=target;pin=observed;status="두 화면의 확인 문구가 모두 같은지 확인해 주세요."
                                } catch(e:CancellationException) { throw e }
                                catch(_:Exception) { status="연결하지 못했습니다. 같은 Wi-Fi인지, 휴대폰의 보내기 화면이 열려 있는지 확인해 주세요." }
                                finally { working=false }
                            }
                        }) { Text(if(working) "확인 중" else "휴대폰 확인") }
                    } else {
                        Text("휴대폰과 같은 문구인가요?")
                        Text(verificationWords(pin!!),fontWeight=FontWeight.Bold)
                        Text("하나라도 다르면 연결하지 말고 주소를 다시 확인해 주세요.")
                        OutlinedTextField(value=code,onValueChange={code=it.filter(Char::isDigit).take(6)},enabled=!working,label={Text("휴대폰의 연결 코드 6자리")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword))
                        Text("받은 목록으로 현재 방문지·사진·체크 상태를 교체합니다.")
                        Button(enabled=!working&&code.length==6,onClick={
                            val target=peer ?: return@Button
                            val confirmedPin=pin ?: return@Button
                            working=true;status="사진과 목록을 받고 있습니다."
                            scope.launch {
                                var bytes: ByteArray?=null
                                try {
                                    bytes=withContext(Dispatchers.IO) { LocalTransferClient.download(target,confirmedPin,code) }
                                    ensureActive()
                                    check(foreground)
                                    applyArchive(bytes!!)
                                    onDismiss()
                                } catch(e:CancellationException) { throw e }
                                catch(_:Exception) { status="받지 못했습니다. 코드·시간 만료·연결 상태를 확인하고 다시 보내 주세요." }
                                finally { bytes?.fill(0);working=false }
                            }
                        }) { Text(if(working) "받는 중" else "문구가 같아요 · 받아서 교체") }
                    }
                }
                if(status.isNotEmpty()) Text(status)
            }
        },
        confirmButton = { TextButton(enabled=!working,onClick=onDismiss) { Text("닫기") } },
    )
}
