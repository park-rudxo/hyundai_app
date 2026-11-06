package io.changyeoga.app

import android.os.Bundle
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Ink = Color(0xFF0C191D)
private val Panel = Color(0xFF172B31)
private val Mint = Color(0xFF9BE4C5)
private val Soft = Color(0xFFB4C8CC)

class MainActivity: ComponentActivity() {
    private var vehicleGate: VehicleGate? = null
    private var allowed by mutableStateOf(!BuildConfig.IS_VEHICLE || BuildConfig.IS_DEMO)
    private var signal by mutableStateOf("차량 상태 확인 중")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary=Mint,background=Ink,surface=Panel,onSurface=Color.White,onPrimary=Ink)) {
                val density=LocalDensity.current
                val scale=if(BuildConfig.IS_VEHICLE) (LocalConfiguration.current.screenWidthDp / 1000f).coerceIn(1f,1.5f) else 1f
                CompositionLocalProvider(LocalDensity provides Density(density.density*scale,density.fontScale)) { App(allowed,signal) }
            }
        }
    }
    override fun onStart() {
        super.onStart()
        if(BuildConfig.IS_VEHICLE && !BuildConfig.IS_DEMO) {
            allowed=false
            vehicleGate=VehicleGate(this) { ok,message -> allowed=ok; signal=message }
        }
    }
    override fun onStop() {
        if(BuildConfig.IS_VEHICLE && !BuildConfig.IS_DEMO) { allowed=false; vehicleGate?.close(); vehicleGate=null }
        super.onStop()
    }
}

@Composable
private fun App(vehicleAllowed: Boolean, signal: String) {
    val context=LocalContext.current
    val compact=LocalConfiguration.current.screenWidthDp < 600
    val store=remember { TripStore(context) }
    val loaded=remember { runCatching { store.load() } }
    var trip by remember { mutableStateOf(loaded.getOrDefault(Trip())) }
    var error by remember { mutableStateOf(if(loaded.isFailure) "저장된 목록을 읽지 못했습니다. 기존 파일을 보존했습니다." else "") }
    var loadFailed by remember { mutableStateOf(loaded.isFailure) }
    var delivery by rememberSaveable { mutableStateOf(false) }
    var simulatedParked by rememberSaveable { mutableStateOf(true) }
    var addStop by rememberSaveable { mutableStateOf(false) }
    var stopName by rememberSaveable { mutableStateOf("") }
    var reset by rememberSaveable { mutableStateOf(false) }
    var pendingStop by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var importConfirm by remember { mutableStateOf(false) }
    var wireless by remember { mutableStateOf(false) }
    val scope=rememberCoroutineScope()
    val allowed=if(BuildConfig.IS_DEMO) simulatedParked else vehicleAllowed
    val latestAllowed by rememberUpdatedState(allowed)
    fun commit(next: Trip) {
        if(!latestAllowed || busy) return
        try { store.save(next); trip=next; error="" } catch(_: Exception) { error="저장하지 못했습니다. 여유 공간을 확인하고 다시 시도해 주세요." }
    }
    fun acceptPhoto(save: () -> String) {
        val target=pendingStop ?: return
        // The picker was opened while parked. Save its result even if the car
        // service is reconnecting after onStart; the UI stays locked by the gate.
        busy=true
        scope.launch {
            var photo: String?=null
            try {
                photo=withContext(Dispatchers.IO) { save() }
                if(trip.stops.any { it.id==target }) {
                    val next=trip.addItem(Luggage(stopId=target,photo=photo))
                    store.save(next)
                    trip=next
                    error=""
                } else { store.deletePhoto(photo) }
            } catch(e: Exception) { if(BuildConfig.DEBUG) android.util.Log.e("Changyeoga", "Photo import failed", e); store.deletePhoto(photo); error="사진을 저장하지 못했습니다. 다른 사진으로 다시 시도해 주세요." }
            finally { busy=false; pendingStop=null }
        }
    }
    val camera=rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if(bitmap != null) acceptPhoto { store.savePhoto(bitmap) } else pendingStop=null
    }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if(uri != null) acceptPhoto { store.importPhoto(uri) } else pendingStop=null
    }
    val exporter=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if(uri!=null) {
            busy=true
            val snapshot=trip
            scope.launch {
                try { withContext(Dispatchers.IO) { store.exportTrip(snapshot,requireNotNull(context.contentResolver.openOutputStream(uri))) }; error="내보내기 완료 · 파일을 다른 기기로 옮겨 가져오세요." }
                catch(_:Exception) { error="내보내지 못했습니다. 저장 위치를 확인해 주세요." }
                finally { busy=false }
            }
        }
    }
    val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) {
            busy=true
            scope.launch {
                try {
                    val previous=trip
                    trip=withContext(Dispatchers.IO) { store.importTrip(requireNotNull(context.contentResolver.openInputStream(uri))) }
                    previous.items.forEach { store.deletePhoto(it.photo) }
                    loadFailed=false; delivery=false; error="가져오기 완료 · 파일에 저장된 시점의 목록입니다."
                } catch(_:Exception) { error="가져오지 못했습니다. 기존 목록은 유지됩니다." }
                finally { busy=false }
            }
        }
    }
    if(importConfirm && allowed) AlertDialog(onDismissRequest={importConfirm=false},title={Text("목록을 가져올까요?")},text={Text("현재 목록을 파일의 방문지·사진·체크 상태로 교체합니다. 필요한 목록은 먼저 내보내 주세요.")},confirmButton={TextButton(onClick={importConfirm=false;runCatching { importer.launch(arrayOf("application/zip","application/octet-stream")) }.onFailure { error="파일 선택 앱을 열 수 없습니다." }}){Text("파일 선택")}},dismissButton={TextButton(onClick={importConfirm=false}){Text("취소")}})
    if(wireless && allowed) WirelessTransferDialog(
        sender=!BuildConfig.IS_VEHICLE,
        snapshot={
            val current=trip
            withContext(Dispatchers.IO) { java.io.ByteArrayOutputStream().also { store.exportTrip(current,it) }.toByteArray() }
        },
        applyArchive={ bytes ->
            val previous=trip
            val received=withContext(Dispatchers.IO) { store.importTrip(bytes.inputStream()) }
            trip=received
            previous.items.forEach { runCatching { store.deletePhoto(it.photo) } }
            loadFailed=false;delivery=false;error="휴대폰의 목록과 사진을 받았습니다."
        },
        onDismiss={wireless=false},
    )
    Surface(Modifier.fillMaxSize(),color=Ink) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal=if(compact) 16.dp else 28.dp,vertical=16.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("챙겨가",fontSize=30.sp,fontWeight=FontWeight.Bold,color=Mint)
                    Spacer(Modifier.width(18.dp))
                    Text(if(BuildConfig.IS_VEHICLE) "오늘의 방문" else "외출 준비",color=Soft,fontSize=18.sp)
                }
                TextButton(onClick={reset=true},enabled=allowed&&!busy) { Text("새 외출") }
            }
            Text(when { BuildConfig.IS_DEMO -> "시연 전용 · 주차와 도착을 직접 선택합니다"; BuildConfig.IS_VEHICLE -> signal; else -> "사진으로 준비하고 · 같은 Wi-Fi에서 차량으로 전송" },color=Soft,fontSize=14.sp)
            if(allowed) Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(enabled=!busy&&!loadFailed,onClick={wireless=true}) { Text(if(BuildConfig.IS_VEHICLE) "휴대폰에서 받기" else "차량으로 보내기") }
                TextButton(enabled=!busy&&!loadFailed,onClick={runCatching { exporter.launch("changyeoga-trip.zip") }.onFailure { error="파일 저장 앱을 열 수 없습니다." }}) { Text("목록 내보내기") }
                TextButton(enabled=!busy,onClick={importConfirm=true}) { Text("목록 가져오기") }
            }
            TripAssistant(trip,allowed&&!busy&&!loadFailed,onSave={commit(it)},onArrive={ id ->
                if(allowed&&!busy&&!loadFailed) { commit(trip.selectStop(id));delivery=true }
            },onPrepare={delivery=false})
            if(BuildConfig.IS_DEMO) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Switch(checked=simulatedParked,onCheckedChange={simulatedParked=it},modifier=Modifier.testTag("parkingSwitch"))
                    Text(if(simulatedParked) "모의 P단" else "모의 주행 · 조작 잠금",Modifier.padding(start=8.dp))
                }
            }
            if(!allowed) {
                Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
                    Column(horizontalAlignment=Alignment.CenterHorizontally) {
                        Text("주차한 뒤 확인해 주세요",fontSize=30.sp,fontWeight=FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text("차량 상태를 확인할 수 없거나 주행 제한이 적용되면\n사진과 확인 버튼을 잠급니다.",color=Soft)
                    }
                }
            } else {
                if(error.isNotBlank()) Text(error,color=Color(0xFFFFBC9E),modifier=Modifier.padding(vertical=8.dp))
                if(loadFailed) {
                    Text("‘새 외출’을 선택하면 저장 내용을 초기화할 수 있습니다.")
                } else if(trip.stops.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {
                        Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)) {
                            Text("오늘 어디에 들르나요?",fontSize=32.sp,fontWeight=FontWeight.Bold)
                            Text("방문지를 정하고, 그곳에 가져갈 짐을 사진으로 담아요.",color=Soft)
                            Button(onClick={addStop=true},modifier=Modifier.heightIn(min=56.dp)) { Text("첫 방문지 추가",fontSize=18.sp) }
                            OutlinedButton(onClick={commit(Trip.example())}) { Text("예시로 둘러보기") }
                        }
                    }
                } else {
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        FilterChip(selected=!delivery,onClick={delivery=false},label={Text("1  출발 준비")})
                        FilterChip(selected=delivery,onClick={delivery=true},label={Text("2  도착·전달")})
                        if(trip.example) Text("예시 방문지 · 직접 선택",Modifier.padding(12.dp),color=Soft)
                    }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        trip.stops.forEach { stop ->
                            FilterChip(selected=stop.id==trip.selectedStopId,onClick={commit(trip.selectStop(stop.id))},label={Text(stop.name,fontSize=18.sp)})
                        }
                        TextButton(onClick={addStop=true}) { Text("+ 방문지") }
                    }
                    val stop=trip.stops.firstOrNull { it.id==trip.selectedStopId } ?: trip.stops.first()
                    val items=trip.forStop(stop.id)
                    Column(Modifier.fillMaxWidth().padding(vertical=12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Column {
                            Text(if(delivery) "${stop.name}에서 건넬 짐" else "${stop.name}에 가져갈 짐",fontSize=26.sp,fontWeight=FontWeight.Bold)
                            Text(if(delivery) "방문지 직접 선택 · 전달 ${items.count { it.state==ItemState.DELIVERED }}/${items.size}" else "적재 ${items.count { it.state!=ItemState.READY }}/${items.size} · 사진만 찍으면 아직 준비 중이에요",color=Soft)
                        }
                        if(!delivery) Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            if(!BuildConfig.IS_VEHICLE) OutlinedButton(enabled=!busy,onClick={pendingStop=stop.id; runCatching { camera.launch(null) }.onFailure { pendingStop=null; error="카메라 앱이 없습니다. 사진 선택을 이용해 주세요." }}) { Text("촬영") }
                            Button(enabled=!busy,onClick={pendingStop=stop.id; runCatching { picker.launch("image/*") }.onFailure { pendingStop=null; error="사진 선택 앱을 열 수 없습니다." }}) { Text(if(busy) "저장 중" else "+ 사진 선택") }
                        }
                    }
                    LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=20.dp)) {
                        if(items.isEmpty()) item { Text("아직 등록한 짐이 없어요. 사진을 추가해 주세요.",Modifier.padding(24.dp),color=Soft) }
                        items(items,key={it.id}) { luggage ->
                            ItemCard(luggage,store,delivery,!busy,onState={commit(trip.changeState(luggage.id,it))},onDelete={
                                val next=trip.removeItem(luggage.id)
                                commit(next)
                                if(trip==next) store.deletePhoto(luggage.photo)
                            })
                        }
                    }
                }
            }
        }
    }
    if(addStop && allowed) AlertDialog(onDismissRequest={addStop=false},title={Text("방문지 추가")},text={OutlinedTextField(value=stopName,onValueChange={stopName=it.take(30)},label={Text("예: 외가, 친구 집")},singleLine=true,modifier=Modifier.testTag("stopName"))},confirmButton={TextButton(enabled=stopName.isNotBlank(),onClick={commit(trip.addStop(stopName));stopName="";addStop=false}){Text("추가")}},dismissButton={TextButton(onClick={addStop=false}){Text("취소")}})
    if(reset && allowed) AlertDialog(onDismissRequest={reset=false},title={Text("새 외출을 준비할까요?")},text={Text("현재 방문지, 짐과 사진이 이 기기에서 삭제됩니다.")},confirmButton={TextButton(enabled=!busy,onClick={val previous=trip; commit(Trip());if(trip==Trip()){previous.items.forEach{store.deletePhoto(it.photo)};loadFailed=false;delivery=false};reset=false}){Text("초기화")}},dismissButton={TextButton(onClick={reset=false}){Text("취소")}})
}

@Composable
private fun ItemCard(item: Luggage,store: TripStore,delivery: Boolean,enabled: Boolean,onState: (ItemState)->Unit,onDelete: ()->Unit) {
    val bitmap by produceState<android.graphics.Bitmap?>(null,item.photo) {
        value=withContext(Dispatchers.IO) { item.photo?.let { runCatching { BitmapFactory.decodeFile(store.photoFile(it).absolutePath) }.getOrNull() } }
    }
    Card(colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
          Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(110.dp).background(Color(0xFF284148),RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center) {
                if(bitmap!=null) Image(bitmap!!.asImageBitmap(),contentDescription=item.label,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                else Text(if(item.photo==null) "사진 없음" else "사진 읽기 실패",color=Soft)
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(item.label,fontSize=22.sp,fontWeight=FontWeight.Bold)
                Text(when(item.state){ItemState.READY->"준비 중";ItemState.LOADED->"차에 실었어요";ItemState.DELIVERED->"전달 완료"},color=Mint,fontSize=18.sp)
                if(delivery && item.state==ItemState.READY) Text("출발 준비에서 적재를 먼저 확인해 주세요.",color=Soft,fontSize=14.sp)
            }
          }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End,verticalAlignment=Alignment.CenterVertically) {
                if(!delivery && item.state==ItemState.READY) Button(enabled=enabled,onClick={onState(ItemState.LOADED)},modifier=Modifier.heightIn(min=56.dp)) { Text("실었어요") }
                if(delivery && item.state==ItemState.LOADED) Button(enabled=enabled,onClick={onState(ItemState.DELIVERED)},modifier=Modifier.heightIn(min=56.dp)) { Text("건넸어요") }
                if(item.state!=ItemState.READY) TextButton(enabled=enabled,onClick={onState(if(item.state==ItemState.DELIVERED) ItemState.LOADED else ItemState.READY)}) { Text("되돌리기") }
                if(!delivery && item.state==ItemState.READY) TextButton(enabled=enabled,onClick=onDelete) { Text("삭제") }
            }
        }
    }
}
