package io.changyeoga.app

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal class DepartureCheckStore(context:Context) {
    private val prefs=context.getSharedPreferences("departure-check",Context.MODE_PRIVATE)
    fun checkedAt():Long=prefs.getLong("gasCheckedAt",0L)
    fun save(time:Long) { check(prefs.edit().putLong("gasCheckedAt",time).commit()) }
    fun clear()=save(0L)
}

@Composable
internal fun DepartureCheck(enabled:Boolean) {
    val context=LocalContext.current
    val store=remember { DepartureCheckStore(context) }
    var checkedAt by remember { mutableLongStateOf(store.checkedAt()) }
    var error by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical=12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        HorizontalDivider()
        Text("출발 전 집안 확인",style=MaterialTheme.typography.titleMedium)
        Text("가스레인지를 직접 확인해 주세요.",style=MaterialTheme.typography.bodyMedium)
        if(checkedAt>0) Text(
            "직접 확인한 시각 · "+SimpleDateFormat("M월 d일 HH:mm",Locale.KOREA).format(Date(checkedAt)),
            style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(enabled=enabled,onClick={
            val time=if(checkedAt==0L) System.currentTimeMillis() else 0L
            runCatching { store.save(time) }.onSuccess { checkedAt=time;error=false }.onFailure { error=true }
        }) { Text(if(checkedAt>0) "확인 취소" else "가스레인지 확인했어요") }
        if(error) Text("저장하지 못했어요. 다시 확인해 주세요.",color=MaterialTheme.colorScheme.error)
        Text("내가 남긴 확인 기록이에요. 현재 가스 상태를 감지하지 않아요.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
