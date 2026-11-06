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

@Composable
fun TripAssistant(trip: Trip, allowed: Boolean, onSave: (Trip)->Unit, onArrive: (String)->Unit, onPrepare: ()->Unit) {
    val context=LocalContext.current
    val owner=LocalLifecycleOwner.current
    val current by rememberUpdatedState(trip)
    val arrive by rememberUpdatedState(onArrive)
    var message by remember { mutableStateOf("") }
    var gps by remember { mutableStateOf(false) }
    var fix by remember { mutableStateOf<Position?>(null) }
    var editor by remember { mutableStateOf(false) }
    var lat by remember { mutableStateOf("") }
    var lon by remember { mutableStateOf("") }
    var editId by remember { mutableStateOf<String?>(null) }
    var editError by remember { mutableStateOf("") }
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
                fix=point
                val stop=detector.update(current.stops,point,SystemClock.elapsedRealtime())
                if(stop!=null) {
                    arrive(stop.id)
                    say("${stop.name} 근처입니다. 안전하게 정차한 뒤 확인하세요. "+luggageSummary(current,stop.id))
                }
                else if(!point.usable(SystemClock.elapsedRealtime())) message="위치 정확도가 낮습니다. 실외에서 기다리거나 방문지를 직접 선택해 주세요."
            }
            override fun onProviderDisabled(provider: String) { fix=null;detector=ArrivalDetector();message="GPS가 꺼졌습니다. 휴대폰 위치 설정을 확인해 주세요." }
            override fun onProviderEnabled(provider: String) { message="GPS 위치를 기다리는 중입니다." }
            @Deprecated("Legacy callback") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
        fun stop() { manager.removeUpdates(listener);fix=null;detector=ArrivalDetector() }
        fun start() {
            if(!gps || BuildConfig.IS_VEHICLE) return
            try {
                if(context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) { gps=false;message="정확한 위치 권한을 허용해 주세요.";return }
                manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,2000L,0f,listener,Looper.getMainLooper())
                message=if(manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) "GPS 위치를 기다리는 중입니다." else "휴대폰에서 위치를 켜 주세요."
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
    if(allowed) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            if(!BuildConfig.IS_VEHICLE) {
                OutlinedButton(onClick={
                    if(gps) gps=false
                    else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))
                }) { Text(if(gps) "GPS 끄기" else "휴대폰 GPS 켜기") }
                OutlinedButton(enabled=trip.stops.isNotEmpty(),onClick={
                    val stop=trip.stops.firstOrNull { it.id==trip.selectedStopId } ?: trip.stops.first()
                    editId=stop.id;lat=stop.latitude?.toString() ?: "";lon=stop.longitude?.toString() ?: "";editError="";editor=true
                }) { Text("방문지 위치") }
            }
            OutlinedButton(onClick={
                tts?.stop()
                runCatching { voice.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR").putExtra(RecognizerIntent.EXTRA_PROMPT,"짐 알려줘 / 외가 도착 / 출발 준비")) }
                    .onFailure { message="음성 인식 앱이 없습니다. 방문지 버튼과 ‘짐 읽기’를 이용하세요." }
            }) { Text("음성 명령") }
            TextButton(onClick={say(luggageSummary(current,current.selectedStopId ?: ""))}) { Text("짐 읽기") }
        }
        if(gps && !BuildConfig.IS_VEHICLE) Text("GPS 시험 · 이 화면이 켜져 있을 때만 감지 · 차량에 실시간 중계하지 않음",style=MaterialTheme.typography.bodySmall)
        if(message.isNotBlank()) Text(message,style=MaterialTheme.typography.bodySmall)
        if(!speechReady) Text("한국어 음성 출력 준비 전 · 안내는 글로도 표시됩니다.",style=MaterialTheme.typography.bodySmall)
    }
    if(editor && allowed) AlertDialog(onDismissRequest={editor=false},title={Text("${trip.stops.firstOrNull { it.id==editId }?.name ?: "방문지"} 위치")},text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("목적지의 위도·경도를 입력하거나, 지금 그 장소에 있다면 현재 GPS를 저장하세요. 집에서 현재 위치를 누르면 목적지가 집으로 설정됩니다.")
            OutlinedTextField(value=lat,onValueChange={lat=it.take(25)},label={Text("위도 (-90~90)")},singleLine=true)
            OutlinedTextField(value=lon,onValueChange={lon=it.take(25)},label={Text("경도 (-180~180)")},singleLine=true)
            TextButton(onClick={val point=fix;if(point!=null&&point.usable(SystemClock.elapsedRealtime())){lat=point.latitude.toString();lon=point.longitude.toString();editError=""}else editError="먼저 GPS를 켜고 정확한 위치를 기다려 주세요."}) { Text("현재 GPS 사용") }
            Text("등록한 좌표는 사진·목록과 함께 전송됩니다. 위치 기록은 저장하지 않습니다.")
            if(editError.isNotBlank()) Text(editError,color=MaterialTheme.colorScheme.error)
        }
    },confirmButton={TextButton(onClick={
        val stop=runCatching { Stop(id=editId ?: "",name=trip.stops.first { it.id==editId }.name,latitude=lat.toDouble(),longitude=lon.toDouble()) }.getOrNull()
        if(stop==null) editError="올바른 위도·경도를 입력해 주세요."
        else { onSave(trip.copy(stops=trip.stops.map { if(it.id==editId) stop else it }));editor=false }
    }){Text("위치 저장")}},dismissButton={TextButton(onClick={editor=false}){Text("취소")}})
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
