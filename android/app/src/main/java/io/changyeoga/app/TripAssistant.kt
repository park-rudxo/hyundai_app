package io.changyeoga.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TripAssistant(trip: Trip, allowed: Boolean, showControls: Boolean = true, onSave: (Trip)->Unit, onArrive: (String)->Unit, onPrepare: ()->Unit) {
    val context=LocalContext.current
    val owner=LocalLifecycleOwner.current
    val current by rememberUpdatedState(trip)
    val arrive by rememberUpdatedState(onArrive)
    var message by remember { mutableStateOf("") }
    var gps by remember { mutableStateOf(false) }
    var fix by remember { mutableStateOf<Position?>(null) }
    var recognized by remember { mutableStateOf<String?>(null) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var speechReady by remember { mutableStateOf(false) }
    LaunchedEffect(allowed) { if(!allowed) tts?.stop() }
    fun say(text: String) {
        message=text
        if(speechReady) tts?.speak(text,TextToSpeech.QUEUE_FLUSH,null,"trip-reminder")
    }
    DisposableEffect(context) {
        val engine=TextToSpeech(context) { result ->
            speechReady=result==TextToSpeech.SUCCESS && (tts?.setLanguage(Locale.KOREAN) ?: -1)>=0
        }
        tts=engine
        onDispose { engine.stop();engine.shutdown();tts=null }
    }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        gps=grants[Manifest.permission.ACCESS_FINE_LOCATION]==true
        if(!gps) message="정확한 위치 권한이 필요합니다. 방문지는 직접 선택할 수 있어요."
    }
    val voice=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        recognized=if(result.resultCode==Activity.RESULT_OK) result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.take(150) else null
        if(recognized==null) message="음성 입력을 받지 못했습니다. 버튼으로도 조작할 수 있어요."
    }
    // Monitoring is phone-only and visible-activity-only. No stored location history.
    DisposableEffect(gps,owner) {
        val manager=context.getSystemService(LocationManager::class.java)
        var detector=ArrivalDetector()
        var configured=current.stops
        val listener=object: LocationListener {
            override fun onLocationChanged(location: Location) {
                if(configured!=current.stops) { configured=current.stops;detector=ArrivalDetector() }
                val point=Position(location.latitude,location.longitude,if(location.hasAccuracy()) location.accuracy else Float.POSITIVE_INFINITY,location.elapsedRealtimeNanos/1000000)
                val previous=fix
                if(previous!=null && previous.usable(SystemClock.elapsedRealtime()) && point.accuracy>previous.accuracy) return
                fix=point
                message=if(point.usable(SystemClock.elapsedRealtime())) "현재 위치를 찾았습니다." else "대략적인 위치를 받았습니다. 정확도를 높이는 중입니다."
                val stop=detector.update(current.stops,point,SystemClock.elapsedRealtime())
                if(stop!=null) {
                    arrive(stop.id)
                    say("${stop.name} 근처입니다. 안전하게 정차한 뒤 확인하세요. "+luggageSummary(current,stop.id))
                }
                else if(!point.usable(SystemClock.elapsedRealtime())) message="위치 정확도가 낮습니다. 실외에서 기다리거나 방문지를 직접 선택해 주세요."
            }
            override fun onProviderDisabled(provider: String) { message="위치 제공 설정이 변경됐습니다. 위치 찾기를 다시 켜 주세요." }
            override fun onProviderEnabled(provider: String) { message="GPS 위치를 기다리는 중입니다." }
            @Deprecated("Legacy callback") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
        fun stop() { manager.removeUpdates(listener);fix=null;detector=ArrivalDetector() }
        fun start() {
            if(!gps || BuildConfig.IS_VEHICLE) return
            try {
                if(context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) { gps=false;message="정확한 위치 권한을 허용해 주세요.";return }
                val providers=listOf(LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER).filter { manager.allProviders.contains(it) && manager.isProviderEnabled(it) }
                if(providers.isEmpty()) { message="휴대폰 위치가 꺼져 있습니다. 위치 설정을 열어 켜 주세요.";return }
                message="현재 위치를 찾고 있습니다. 실내에서는 Wi-Fi도 켜 주세요."
                var requests=0
                providers.forEach { provider ->
                    runCatching {
                        manager.requestLocationUpdates(provider,2000L,0f,listener,Looper.getMainLooper())
                        requests++
                        manager.getLastKnownLocation(provider)?.takeIf { SystemClock.elapsedRealtime()-it.elapsedRealtimeNanos/1000000 in 0..15000 }?.let { listener.onLocationChanged(it) }
                    }
                }
                if(requests==0) { gps=false;message="위치를 요청하지 못했습니다. 앱의 정확한 위치 권한과 휴대폰 위치 설정을 확인해 주세요." }
            } catch(_:Exception) { gps=false;message="GPS를 사용할 수 없습니다. 직접 방문지를 선택해 주세요." }
        }
        val observer=LifecycleEventObserver { _,event ->
            if(event==Lifecycle.Event.ON_START) start()
            if(event==Lifecycle.Event.ON_STOP) { stop();tts?.stop() }
        }
        owner.lifecycle.addObserver(observer)
        if(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) start()
        onDispose { stop();owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(gps,fix) {
        if(gps && fix==null) {
            kotlinx.coroutines.delay(30000)
            message="아직 위치를 받지 못했습니다. 휴대폰 위치·정확한 위치 권한을 확인하거나 실외에서 다시 시도해 주세요. 위치 없이도 짐 등록을 계속할 수 있습니다."
        }
    }
    if(allowed && showControls) {
        FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            if(!BuildConfig.IS_VEHICLE) {
                OutlinedButton(onClick={
                    if(gps) gps=false
                    else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))
                }) { Text(if(gps) "위치 찾기 중지" else "현재 위치 찾기") }
                TextButton(onClick={context.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))}) { Text("위치 설정") }

            }
            OutlinedButton(onClick={
                tts?.stop()
                runCatching { voice.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR").putExtra(RecognizerIntent.EXTRA_PROMPT,"짐 알려줘 / 외가 도착 / 출발 준비")) }
                    .onFailure { message="음성 인식 앱이 없습니다. 방문지 버튼과 ‘짐 읽기’를 이용하세요." }
            }) { Text("음성 명령") }
            TextButton(onClick={say(luggageSummary(current,current.selectedStopId ?: ""))}) { Text("짐 읽기") }
        }
        fix?.let { Text("현재 위치를 확인했어요 · 오차 약 %.0fm".format(it.accuracy),style=MaterialTheme.typography.bodySmall) }
        if(message.isNotBlank()) Text(message,style=MaterialTheme.typography.bodySmall)
        if(!speechReady) Text("한국어 음성 출력 준비 전 · 안내는 글로도 표시됩니다.",style=MaterialTheme.typography.bodySmall)
    }
    if(recognized!=null && allowed) AlertDialog(onDismissRequest={recognized=null},title={Text("이 명령을 실행할까요?")},text={Text(recognized!!)},confirmButton={TextButton(onClick={
        when(val action=parseVoice(recognized!!,current.stops)) {
            VoiceAction.Read -> say(luggageSummary(current,current.selectedStopId ?: ""))
            VoiceAction.Prepare -> { onPrepare();say("출발 준비 화면입니다.") }
            is VoiceAction.Arrive -> { arrive(action.id);say(luggageSummary(current,action.id)) }
            VoiceAction.Unknown -> message="지원 명령: 짐 알려줘, 방문지 이름 + 도착, 출발 준비. 적재·전달 완료는 직접 확인해 주세요."
        }
        recognized=null
    }){Text("실행")}},dismissButton={TextButton(onClick={recognized=null}){Text("취소")}})
}
