package io.changyeoga.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A single task per page; secondary actions open only when requested. */
@Composable
internal fun PhonePreparation(
    trip:Trip, step:Int, busy:Boolean, error:String, loadFailed:Boolean, store:TripStore,
    onTools:()->Unit, onMap:(Stop?)->Unit, onExample:()->Unit, onStep:(Int)->Unit,
    onSelect:(String)->Unit, onPhoto:(Boolean)->Unit, onState:(String,ItemState)->Unit,
    onDelete:(Luggage)->Unit, onSend:()->Unit, onReset:()->Unit, assistant:@Composable ()->Unit,
) {
    val planContext=androidx.compose.ui.platform.LocalContext.current
    var chooseStop by remember { mutableStateOf(false) }
    var choosePhoto by remember { mutableStateOf(false) }
    val titles=listOf("어디에 들르나요?","무엇을 가져가나요?","차에 실었나요?","차량으로 보내세요","도착하면 건네주세요")
    val next=listOf("짐 사진 등록","차에 싣기","차량에 보내기","도착 후 전달")
    val stop=trip.stops.firstOrNull { it.id==trip.selectedStopId } ?: trip.stops.firstOrNull()
    val luggage=stop?.let { trip.forStop(it.id) }.orEmpty()
    val canNext=when(step) {
        0 -> trip.stops.isNotEmpty()
        1 -> trip.stops.isNotEmpty() && trip.stops.all { trip.forStop(it.id).isNotEmpty() }
        2 -> trip.items.isNotEmpty() && trip.items.none { it.state==ItemState.READY }
        4 -> trip.items.isNotEmpty() && trip.items.all { it.state==ItemState.DELIVERED }
        else -> true
    }
    Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                if(step>0) TextButton(onClick={onStep(step-1)},enabled=!busy) { AppSymbol(R.drawable.symbol_arrow_back); Spacer(Modifier.width(4.dp)); Text("이전") }
                Text("챙겨가  ·  ${step+1}/5",Modifier.padding(top=12.dp),color=MaterialTheme.colorScheme.primary)
                TextButton(onClick=onTools,enabled=!busy) { AppSymbol(R.drawable.symbol_more_horiz); Spacer(Modifier.width(4.dp)); Text("파일·새 외출") }
            }
            LinearProgressIndicator(progress={(step+1)/5f},modifier=Modifier.fillMaxWidth().padding(top=8.dp,bottom=24.dp))
            Text(titles[step],style=MaterialTheme.typography.headlineMedium)
            if(error.isNotBlank()) Text(error,color=MaterialTheme.colorScheme.error)
            if(loadFailed) { Text("저장된 목록을 확인할 수 없어요.");TextButton(onClick=onTools){Text("파일·새 외출")};return@Column }
            assistant()
            LazyColumn(Modifier.weight(1f).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(vertical=24.dp)) {
                when(step) {
                    0 -> {
                        item { TextButton(onClick={
                            planContext.startActivity(android.content.Intent().setClassName(planContext,"io.changyeoga.app.SharedPlanActivity")
                                .putExtra("title","외출 준비").putExtra("place",trip.stops.joinToString(" → "){it.name}))
                        }) { Text("Google 캘린더 · 공유 메모 →") } }
                        item { Text(if(trip.stops.isEmpty()) "들를 곳을 찾은 다음, 가져갈 짐을 담아요." else "방문지 ${trip.stops.size}곳을 정했어요. 사진 등록으로 넘어가세요.") }
                        items(trip.stops,key={it.id}) { destination ->
                            Card(onClick={onMap(destination)},colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),modifier=Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                                    AppSymbol(R.drawable.symbol_location_on)
                                    Column(Modifier.weight(1f)) {
                                    Text(destination.name,style=MaterialTheme.typography.titleMedium)
                                    Text("눌러서 위치 변경",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    AppSymbol(R.drawable.symbol_edit)
                                }
                            }
                        }
                        item {
                            if(trip.stops.isEmpty()) LargeAction("지도에서 첫 방문지 찾기",!busy){onMap(null)}
                            else OutlinedButton(onClick={onMap(null)},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp),shape=RoundedCornerShape(12.dp)) { AppSymbol(R.drawable.symbol_add); Spacer(Modifier.width(8.dp)); Text("지도에서 방문지 찾기") }
                        }
                        if(trip.stops.isEmpty()) item { TextButton(onClick=onExample) { Text("예시로 둘러보기") } }
                    }
                    3 -> {
                        item { Text("방문지 ${trip.stops.size}곳 · 짐 ${trip.items.size}개 준비 완료",fontSize=20.sp) }
                        item { Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                            Text("1  폰과 차량을 같은 Wi-Fi에 연결")
                            Text("2  차량에서 ‘휴대폰에서 받기’ 선택")
                            Text("3  아래 버튼으로 전송")
                        } } }
                        item { LargeAction("차량으로 보내기",!busy,onSend) }
                    }
                    else -> {
                        item { OutlinedButton(onClick={chooseStop=true},modifier=Modifier.fillMaxWidth().heightIn(min=64.dp)) { Text("${stop?.name.orEmpty()}  ·  방문지 변경",fontSize=17.sp) } }
                        if(step==1) item { OutlinedButton(onClick={choosePhoto=true},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp),shape=RoundedCornerShape(12.dp)) { Text("짐 사진 추가") } }
                        if(step==2) item { DepartureCheck(!busy) }
                        if(step==2) item { Text("총 ${trip.items.size}개 중 ${trip.items.count { it.state!=ItemState.READY }}개 실었어요",fontWeight=FontWeight.SemiBold) }
                        if(luggage.isEmpty()) item { Text("이곳에 가져갈 짐을 사진으로 추가하세요.") }
                        items(if(step==2) luggage.sortedBy { it.state!=ItemState.READY } else luggage,key={it.id}) { item ->
                            ItemCard(item,store,step==4,!busy,showActions=step!=1,onState={onState(item.id,it)},onDelete={onDelete(item)})
                        }
                        if(!canNext) item { Text(when(step) {
                            1 -> "방문지마다 짐 사진을 추가하면 다음으로 갈 수 있어요."
                            2 -> "방문지를 바꿔가며 모든 짐을 ‘실었어요’로 확인해 주세요."
                            else -> "건넨 짐을 확인해 주세요."
                        }) }
                    }
                }
            }
            if(trip.stops.isNotEmpty()) LargeAction(if(step==4) "새 외출 준비" else "다음 · ${next[step]}",!busy&&canNext) {
                if(step==4) onReset() else onStep(step+1)
            }
        }
    }
    if(chooseStop) AlertDialog(onDismissRequest={chooseStop=false},title={Text("어느 방문지인가요?")},text={
        LazyColumn { items(trip.stops) { destination -> TextButton(onClick={onSelect(destination.id);chooseStop=false},modifier=Modifier.fillMaxWidth().heightIn(min=64.dp)) { Text(destination.name,fontSize=20.sp) } } }
    },confirmButton={TextButton(onClick={chooseStop=false}){Text("닫기")}})
    if(choosePhoto) AlertDialog(onDismissRequest={choosePhoto=false},title={Text("짐 사진 추가")},text={
        Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
            LargeAction("촬영",!busy){choosePhoto=false;onPhoto(true)}
            LargeAction("사진에서 선택",!busy){choosePhoto=false;onPhoto(false)}
        }
    },confirmButton={TextButton(onClick={choosePhoto=false}){Text("취소")}})
}

@Composable
private fun LargeAction(label:String,enabled:Boolean=true,onClick:()->Unit) {
    Button(onClick=onClick,enabled=enabled,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp),shape=RoundedCornerShape(12.dp),contentPadding=PaddingValues(16.dp)) {
        Text(label,style=MaterialTheme.typography.labelLarge)
    }
}
