package io.changyeoga.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun PhonePreparation(
    trip:Trip, step:Int, busy:Boolean, error:String, loadFailed:Boolean, store:TripStore,
    onTools:()->Unit, onMap:(Stop?)->Unit, onExample:()->Unit, onStep:(Int)->Unit,
    onSelect:(String)->Unit, onPhoto:(Boolean)->Unit, onState:(String,ItemState)->Unit,
    onDelete:(Luggage)->Unit, onSend:()->Unit, onReset:()->Unit, assistant:@Composable ()->Unit,
) {
    val context=androidx.compose.ui.platform.LocalContext.current
    var chooseStop by remember { mutableStateOf(false) }
    var choosePhoto by remember { mutableStateOf(false) }
    val phases=listOf("방문지 정하기","준비물 등록","적재 확인","차량 연결","도착 후 전달")
    val titles=listOf("오늘 어디에 들르나요?","무엇을 가져가나요?","차에 실었나요?","차량으로 이어가요.","이곳에 건넬 짐이에요.")
    val descriptions=listOf("들를 곳을 정하고, 가져갈 물건을 연결해요.","가져갈 물건을 사진으로 등록해 주세요.","직접 확인한 물건만 표시해 주세요.","폰과 차량을 같은 Wi-Fi에 연결해 주세요.","주차 후 물건을 건네고 확인해 주세요.")
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
    Surface(Modifier.fillMaxSize(),color=Canvas) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal=24.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min=64.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                if(step>0) CAction("이전",!busy){onStep(step-1)} else Spacer(Modifier.width(48.dp))
                Text("챙겨가",style=MaterialTheme.typography.titleMedium)
                CAction("파일·새 외출",!busy,onClick=onTools)
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(top=28.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(0.dp)) {
                item { CSection("${step+1} / 5  ·  ${phases[step]}",titles[step],descriptions[step]);Spacer(Modifier.height(28.dp)) }
                if(error.isNotBlank()) item { Text(error,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodyMedium);Spacer(Modifier.height(16.dp)) }
                if(loadFailed) item { Text("저장된 목록을 확인할 수 없어요.");CAction("파일·새 외출",onClick=onTools) }
                else {
                    item { assistant() }
                    when(step) {
                        0 -> {
                            items(trip.stops,key={it.id}) { destination ->
                                Row(Modifier.fillMaxWidth().clickable(enabled=!busy){onMap(destination)}.padding(vertical=20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                                    Text((trip.stops.indexOf(destination)+1).toString().padStart(2,'0'),style=MaterialTheme.typography.bodySmall,color=MutedInk)
                                    Column(Modifier.weight(1f)) { Text(destination.name,style=MaterialTheme.typography.titleMedium);Text("눌러서 장소·별명 변경",style=MaterialTheme.typography.bodySmall,color=MutedInk) }
                                    CAction("편집",!busy){onMap(destination)}
                                }
                                HorizontalDivider(color=Rule)
                            }
                            item {
                                CAction(if(trip.stops.isEmpty()) "지도에서 첫 방문지 찾기" else "지도에서 방문지 찾기",!busy,arrow=true){onMap(null)}
                                if(trip.stops.isEmpty()) CAction("예시로 둘러보기",!busy,onClick=onExample)
                                Spacer(Modifier.height(32.dp))
                                CAction("Google 캘린더 · 공유 메모",!busy,arrow=true) {
                                    context.startActivity(android.content.Intent().setClassName(context,"io.changyeoga.app.SharedPlanActivity")
                                        .putExtra("title","외출 준비").putExtra("place",trip.stops.joinToString(" → "){it.name}))
                                }
                            }
                        }
                        3 -> {
                            item {
                                listOf("차량에서 받기 열기" to "차량 앱에서 ‘휴대폰에서 받기’를 선택해 주세요.","준비한 목록 보내기" to "방문지 ${trip.stops.size}곳 · 준비물 ${trip.items.size}개").forEachIndexed { i,pair ->
                                    Row(Modifier.fillMaxWidth().padding(vertical=20.dp),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                                        Text("0${i+1}",color=MutedInk,style=MaterialTheme.typography.bodySmall)
                                        Column { Text(pair.first,style=MaterialTheme.typography.titleMedium);Spacer(Modifier.height(6.dp));Text(pair.second,style=MaterialTheme.typography.bodyMedium,color=MutedInk) }
                                    };HorizontalDivider(color=Rule)
                                }
                                CAction("차량으로 보내기",!busy,arrow=true,onClick=onSend)
                            }
                        }
                        else -> {
                            item {
                                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                                    Text(stop?.name.orEmpty(),style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f))
                                    CAction("방문지 변경",!busy){chooseStop=true}
                                }
                                HorizontalDivider(color=Rule)
                                if(step==1) CAction("짐 사진 추가",!busy,arrow=true){choosePhoto=true}
                                if(step==2) { Spacer(Modifier.height(20.dp));Text("총 ${trip.items.size}개 중 ${trip.items.count { it.state!=ItemState.READY }}개 실었어요",style=MaterialTheme.typography.bodySmall,color=MutedInk) }
                                if(step==4) Text("전달 ${luggage.count { it.state==ItemState.DELIVERED }}/${luggage.size}",style=MaterialTheme.typography.bodySmall,color=MutedInk,modifier=Modifier.padding(top=16.dp))
                            }
                            items(luggage,key={it.id}) { item ->
                                ItemCard(item,store,step==4,!busy,showActions=step!=1,onState={onState(item.id,it)},onDelete={onDelete(item)})
                            }
                            if(luggage.isEmpty()) item { Text("가져갈 물건을 사진으로 추가해 주세요.",style=MaterialTheme.typography.bodyMedium,color=MutedInk,modifier=Modifier.padding(vertical=24.dp)) }
                            if(step==2) item { Spacer(Modifier.height(32.dp));DepartureCheck(!busy) }
                        }
                    }
                }
            }
            if(trip.stops.isNotEmpty()&&!loadFailed) {
                HorizontalDivider(color=Rule)
                Column(Modifier.fillMaxWidth().padding(top=12.dp,bottom=12.dp)) {
                    if(!canNext) Text(when(step) {1->"방문지마다 준비물을 등록해 주세요.";2->"아직 싣지 않은 물건을 확인해 주세요.";else->"건넨 물건을 확인해 주세요."},style=MaterialTheme.typography.bodySmall,color=MutedInk)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
                        CAction(if(step==4) "새 외출 준비" else "다음 · ${next[step]}",!busy&&canNext,arrow=true) { if(step==4) onReset() else onStep(step+1) }
                    }
                }
            }
        }
    }
    if(chooseStop) AlertDialog(onDismissRequest={chooseStop=false},title={Text("어느 방문지인가요?")},text={
        LazyColumn { items(trip.stops) { destination -> CAction(destination.name,modifier=Modifier.fillMaxWidth()){onSelect(destination.id);chooseStop=false} } }
    },confirmButton={CAction("닫기"){chooseStop=false}})
    if(choosePhoto) AlertDialog(onDismissRequest={choosePhoto=false},title={Text("짐 사진 추가")},text={
        Column { CAction("촬영",!busy,arrow=true){choosePhoto=false;onPhoto(true)};CAction("사진에서 선택",!busy,arrow=true){choosePhoto=false;onPhoto(false)} }
    },confirmButton={CAction("취소"){choosePhoto=false}})
}
