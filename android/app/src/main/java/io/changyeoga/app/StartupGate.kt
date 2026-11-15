package io.changyeoga.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI

/** One readiness check per activity session; returning from the map does not reload. */
@Composable
internal fun StartupGate(
    prepare:suspend ()->Unit = { prepareSearchServer() },
    content:@Composable ()->Unit,
) {
    var entered by rememberSaveable { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    var failed by remember { mutableStateOf(false) }
    var slow by remember { mutableStateOf(false) }
    if(entered) { content();return }
    LaunchedEffect(attempt) {
        failed=false
        slow=false
        val slowNotice=launch { delay(8000);slow=true }
        try {
            prepare()
            entered=true
        } catch(cancelled:CancellationException) {
            throw cancelled
        } catch(_:Exception) {
            failed=true
        } finally {
            slowNotice.cancel()
        }
    }
    Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(28.dp),
            verticalArrangement=Arrangement.Center,
            horizontalAlignment=Alignment.Start,
        ) {
            Text("챙겨가",fontSize=40.sp,fontWeight=FontWeight.Normal,color=MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(32.dp))
            if(!failed) LinearProgressIndicator(modifier=Modifier.width(80.dp).height(2.dp))
            Spacer(Modifier.height(24.dp))
            Text(if(failed) "연결을 확인해 주세요" else "장소 검색을 준비하고 있어요",fontSize=23.sp,fontWeight=FontWeight.Normal)
            Spacer(Modifier.height(12.dp))
            Text(when {
                failed -> "검색 서버에 연결하지 못했어요. 다시 시도하거나 먼저 짐을 준비할 수 있어요."
                slow -> "쉬고 있던 검색 서버를 깨우는 중이에요. 약 1분 걸릴 수 있어요."
                else -> "준비되면 자동으로 시작해요."
            },color=MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
            if(failed) CAction("다시 시도",arrow=true){attempt++}
            CAction("준비 화면 먼저 열기",arrow=true){entered=true}
        }
    }
}

private suspend fun prepareSearchServer() = withContext(Dispatchers.IO) {
    val base=BuildConfig.PLACE_SEARCH_URL.trimEnd('/')
    check(base.isNotBlank())
    val connection=URI.create("$base/health").toURL().openConnection() as HttpURLConnection
    connection.connectTimeout=5000
    connection.readTimeout=65000
    connection.instanceFollowRedirects=false
    try {
        check(connection.responseCode==200)
        val body=connection.inputStream.bufferedReader().use { it.readText() }
        check(JSONObject(body).optBoolean("ok",false))
    } finally {
        connection.disconnect()
    }
}
