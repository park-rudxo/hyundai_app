package io.changyeoga.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun VehiclePreparation(trip:Trip,store:TripStore,allowed:Boolean,signal:String,busy:Boolean,error:String,
    loadFailed:Boolean,delivery:Boolean,onDelivery:(Boolean)->Unit,onSelect:(String)->Unit,
    onState:(String,ItemState)->Unit,onDelete:(Luggage)->Unit,onReceive:()->Unit,onTools:()->Unit,
    onExample:()->Unit,simulatedParked:Boolean,onParked:(Boolean)->Unit) {
    val stop=trip.stops.firstOrNull { it.id==trip.selectedStopId } ?: trip.stops.firstOrNull()
    val luggage=stop?.let { trip.forStop(it.id) }.orEmpty()
    Surface(Modifier.fillMaxSize(),color=Canvas) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal=32.dp,vertical=16.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("챙겨가",fontSize=24.sp)
                Spacer(Modifier.width(20.dp))
                Text(if(BuildConfig.IS_DEMO) "차량 시연" else "오늘의 외출",color=MutedInk,fontSize=16.sp)
                Spacer(Modifier.weight(1f))
                if(allowed) CAction("휴대폰에서 받기",!busy,onClick=onReceive)
                Spacer(Modifier.width(16.dp))
                CAction("더보기",allowed&&!busy,onClick=onTools)
            }
            if(BuildConfig.IS_DEMO) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Switch(checked=simulatedParked,onCheckedChange=onParked,modifier=Modifier.testTag("parkingSwitch"))
                    Text(if(simulatedParked) "모의 P단" else "모의 주행 · 조작 잠금",style=MaterialTheme.typography.bodyMedium,color=MutedInk)
                    Text("주차·방문지는 직접 선택하는 시연 환경",style=MaterialTheme.typography.bodySmall,color=MutedInk)
                }
            } else Text(signal,style=MaterialTheme.typography.bodyMedium,color=MutedInk,modifier=Modifier.padding(bottom=16.dp))
            HorizontalDivider(color=Rule)
            if(!allowed) {
                Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {
                    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)) {
                        AppSymbol(R.drawable.symbol_directions_car)
                        Text("주차한 뒤 확인해 주세요",style=MaterialTheme.typography.headlineLarge)
                        Text("주행 중에는 준비물 사진과 확인 버튼을 잠급니다.",fontSize=20.sp,color=MutedInk)
                    }
                }
            } else if(trip.stops.isEmpty()||loadFailed) {
                Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {
                    Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
                        Text(if(loadFailed) "목록을 다시 확인해 주세요." else "휴대폰에서 준비한 목록을 받아요.",style=MaterialTheme.typography.headlineLarge)
                        Text("방문지와 준비물, 적재 상태를 이어서 확인할 수 있어요.",color=MutedInk,fontSize=20.sp)
                        CAction("휴대폰에서 받기",!busy,arrow=true,onClick=onReceive)
                        if(BuildConfig.IS_DEMO) CAction("예시로 둘러보기",!busy,onClick=onExample)
                    }
                }
            } else {
                Row(Modifier.weight(1f).fillMaxWidth().padding(top=28.dp),horizontalArrangement=Arrangement.spacedBy(28.dp)) {
                    Column(Modifier.weight(.28f).fillMaxHeight()) {
                    LazyColumn(Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(bottom=16.dp)) {
                        item { Text("오늘의 방문",color=MutedInk,fontSize=16.sp);Spacer(Modifier.height(24.dp)) }
                        items(trip.stops,key={it.id}) { destination ->
                            val selected=stop?.id==destination.id
                            Row(Modifier.fillMaxWidth().clickable(enabled=!busy){onSelect(destination.id)}.padding(vertical=16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.width(2.dp).height(52.dp).background(if(selected) ActionBlue else Rule))
                                Column {
                                    Text(destination.name,fontSize=22.sp,color=if(selected) ActionBlue else TextInk)
                                    Spacer(Modifier.height(6.dp))
                                    Text("남은 짐 ${trip.forStop(destination.id).count { it.state!=ItemState.DELIVERED }}개",color=MutedInk,fontSize=15.sp)
                                }
                            }
                        }
                    }
                    stop?.let { VehicleMapActions(it,!busy) }
                    }
                    VerticalDivider(color=Rule)
                    Column(Modifier.weight(.72f).fillMaxHeight()) {
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                            Text(if(delivery) "이곳에 건넬 짐이에요." else "출발 전에 확인해요.",style=MaterialTheme.typography.headlineLarge,modifier=Modifier.weight(1f))
                            CAction(if(delivery) "출발 준비" else "도착·전달",!busy){onDelivery(!delivery)}
                        }
                        Text("${stop?.name.orEmpty()} · "+if(delivery) "전달 ${luggage.count { it.state==ItemState.DELIVERED }}/${luggage.size}" else "적재 ${luggage.count { it.state!=ItemState.READY }}/${luggage.size}",color=MutedInk,fontSize=18.sp)
                        if(error.isNotBlank()) Text(error,color=ActionBlue,style=MaterialTheme.typography.bodyMedium,modifier=Modifier.padding(top=8.dp))
                        Spacer(Modifier.height(16.dp))
                        LazyColumn(Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(bottom=16.dp)) {
                            if(luggage.isEmpty()) item { Text("이 방문지에 등록한 준비물이 없어요.",fontSize=20.sp,color=MutedInk,modifier=Modifier.padding(vertical=24.dp)) }
                            items(luggage,key={it.id}) { item -> ItemCard(item,store,delivery,!busy,onState={onState(item.id,it)},onDelete={onDelete(item)}) }
                        }

                    }
                }
            }
        }
    }
}
