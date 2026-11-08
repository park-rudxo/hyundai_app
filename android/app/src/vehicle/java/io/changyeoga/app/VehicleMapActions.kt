package io.changyeoga.app

import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.NavigationScreenRequest
import ai.pleos.playground.navi.data.RouteInfo
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
internal fun VehicleMapActions(stop: Stop, allowed: Boolean) {
    val context=LocalContext.current
    val owner=LocalLifecycleOwner.current
    var helper by remember { mutableStateOf<NaviHelper?>(null) }
    var ready by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Pleos 내비 연결 중") }
    DisposableEffect(owner,context) {
        val handler=Handler(Looper.getMainLooper())
        var active=false
        val listener=object: NaviHelperEventListener {
            override fun onRoutingCompleted() { handler.post { if(active) message="Pleos 내비에서 경로를 찾았습니다." } }
            override fun onError(errorCode: NaviErrorCode) { handler.post { if(active) message="내비 요청을 처리하지 못했습니다: $errorCode" } }
        }
        fun close() {
            active=false;ready=false
            helper?.let { runCatching { it.removeListener(listener);it.release() } }
            helper=null
        }
        fun start() {
            if(active) return
            active=true
            try {
                val candidate=NaviHelper(context)
                helper=candidate
                candidate.initialize({ handler.post { if(active && helper===candidate) {
                    runCatching { candidate.addListener(listener) }.onSuccess { ready=true;message="Pleos 내비 연결됨" }
                        .onFailure { ready=false;message="Pleos 내비 이벤트 연결 실패" }
                } } },
                    { handler.post { if(active && helper===candidate) { ready=false;message="Pleos 내비 연결 실패 · 기본 내비와 권한을 확인해 주세요." } } })
            } catch(_:Exception) { close();message="Pleos 내비 서비스를 사용할 수 없습니다." }
        }
        val observer=LifecycleEventObserver { _,event ->
            if(event==Lifecycle.Event.ON_START) start()
            if(event==Lifecycle.Event.ON_STOP) close()
        }
        owner.lifecycle.addObserver(observer)
        if(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) start()
        onDispose { owner.lifecycle.removeObserver(observer);close() }
    }
    fun request(action: (NaviHelper)->Unit) {
        val current=helper ?: return
        if(!allowed || !ready) return
        try { action(current) } catch(_:Exception) { message="내비 요청에 실패했습니다. 연결과 권한을 확인해 주세요." }
    }
    Column {
        Row {
            OutlinedButton(enabled=allowed&&ready,onClick={request { it.requestNavigationScreen(NavigationScreenRequest.Foreground) }}) { Text("Pleos 지도 열기") }
            Button(enabled=allowed&&ready&&stop.latitude!=null,onClick={request {
                it.requestRoute(RouteInfo(latitude=requireNotNull(stop.latitude),longitude=requireNotNull(stop.longitude),displayLatitude=null,displayLongitude=null,poiName=stop.name,poiId="",poiSubId="",address=""))
                message="경로를 요청했습니다. 내비의 응답을 기다립니다."
            }}) { Text("이 방문지로 길 안내") }
        }
        Text(if(stop.latitude==null) "$message · 휴대폰에서 방문지 위치를 먼저 등록하세요." else message,style=MaterialTheme.typography.bodySmall)
    }
}
