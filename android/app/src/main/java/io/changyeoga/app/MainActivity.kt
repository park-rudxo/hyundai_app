package io.changyeoga.app

import android.os.Bundle
import android.graphics.BitmapFactory
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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


class MainActivity: ComponentActivity() {
    private var vehicleGate: VehicleGate? = null
    private var allowed by mutableStateOf(!BuildConfig.IS_VEHICLE || BuildConfig.IS_DEMO)
    private var signal by mutableStateOf("차량 상태 확인 중")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle=SystemBarStyle.light(android.graphics.Color.TRANSPARENT,android.graphics.Color.TRANSPARENT),
            navigationBarStyle=SystemBarStyle.light(android.graphics.Color.TRANSPARENT,android.graphics.Color.TRANSPARENT))
        setContent {
            MaterialTheme(colorScheme=appColors(),typography=appTypography(),shapes=Shapes(
                small=RoundedCornerShape(4.dp),medium=RoundedCornerShape(8.dp),large=RoundedCornerShape(12.dp),extraLarge=RoundedCornerShape(16.dp))) {
                if(BuildConfig.IS_VEHICLE) App(allowed,signal) else StartupGate { App(allowed,signal) }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun App(vehicleAllowed: Boolean, signal: String) {
    val context=LocalContext.current
    @Suppress("DEPRECATION")
    val installedVersion=remember { context.packageManager.getPackageInfo(context.packageName,0).versionName.orEmpty() }
    val compact=LocalConfiguration.current.screenWidthDp < 600
    val store=remember { TripStore(context) }
    val loaded=remember { runCatching { store.load() } }
    var trip by remember { mutableStateOf(loaded.getOrDefault(Trip())) }
    var error by remember { mutableStateOf(if(loaded.isFailure) "저장된 목록을 읽지 못했습니다. 기존 파일을 보존했습니다." else "") }
    var loadFailed by remember { mutableStateOf(loaded.isFailure) }
    var delivery by rememberSaveable { mutableStateOf(false) }
    val flowPrefs=remember { context.getSharedPreferences("trip-flow",android.content.Context.MODE_PRIVATE) }
    var step by rememberSaveable { mutableStateOf(if(trip.stops.isEmpty()) 0 else flowPrefs.getInt("step",0).coerceIn(0,4)) }
    LaunchedEffect(step) { if(!BuildConfig.IS_VEHICLE) flowPrefs.edit().putInt("step",step).apply() }
    LaunchedEffect(step) { if(!BuildConfig.IS_VEHICLE) delivery=step==4 }
    var more by remember { mutableStateOf(false) }
    var tools by remember { mutableStateOf(false) }
    var assistantPanel by rememberSaveable { mutableStateOf(false) }
    val phone = !BuildConfig.IS_VEHICLE
    val steps = listOf("방문지 정하기", "짐 사진 등록", "차에 싣기", "차량에 보내기", "도착 후 전달")
    var simulatedParked by rememberSaveable { mutableStateOf(true) }
    var addStop by rememberSaveable { mutableStateOf(false) }
    var stopName by rememberSaveable { mutableStateOf("") }
    var reset by rememberSaveable { mutableStateOf(false) }
    var pendingStop by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingMapStop by rememberSaveable { mutableStateOf<String?>(null) }
    var mapCreating by rememberSaveable { mutableStateOf(false) }
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
    val placeMap=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id=pendingMapStop
        pendingMapStop=null
        if(result.resultCode==android.app.Activity.RESULT_OK && (id!=null || mapCreating)) {
            val lat=result.data?.getDoubleExtra("latitude",Double.NaN) ?: Double.NaN
            val lon=result.data?.getDoubleExtra("longitude",Double.NaN) ?: Double.NaN
            if(lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0) {
                val name=result.data?.getStringExtra("name")?.trim().orEmpty().ifBlank { "선택한 장소" }.take(30)
                if(mapCreating) {
                    val stop=Stop(name=name,latitude=lat,longitude=lon)
                    commit(trip.copy(stops=trip.stops+stop,selectedStopId=stop.id))
                } else commit(trip.copy(stops=trip.stops.map { if(it.id==id) it.copy(name=name,latitude=lat,longitude=lon) else it }))
            } else error="선택한 위치를 읽지 못했습니다. 다시 선택해 주세요."
        }
    }
    fun openMap(stop:Stop?=null) {
        mapCreating=stop==null
        pendingMapStop=stop?.id
        val intent=android.content.Intent().setClassName(context,"io.changyeoga.app.PlaceMapActivity")
        stop?.let { intent.putExtra("name",it.name) }
        stop?.latitude?.let { intent.putExtra("latitude",it) }
        stop?.longitude?.let { intent.putExtra("longitude",it) }
        runCatching { placeMap.launch(intent) }.onFailure { pendingMapStop=null;mapCreating=false;error="지도 화면을 열지 못했습니다." }
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
                    loadFailed=false; delivery=false; step=0; error="가져오기 완료 · 파일에 저장된 시점의 목록입니다."
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
    if(phone) PhonePreparation(
        trip=trip, step=step, busy=busy, error=error, loadFailed=loadFailed, store=store,
        onTools={tools=true}, onMap={openMap(it)}, onExample={commit(Trip.example())},
        onStep={step=it}, onSelect={commit(trip.selectStop(it))},
        onPhoto={ useCamera ->
            pendingStop=trip.selectedStopId
            runCatching { if(useCamera) camera.launch(null) else picker.launch("image/*") }
                .onFailure { pendingStop=null;error="사진 앱을 열지 못했어요. 다시 시도해 주세요." }
        },
        onState={id,state -> commit(trip.changeState(id,state))},
        onDelete={item -> val next=trip.removeItem(item.id);commit(next);if(trip==next)store.deletePhoto(item.photo)},
        onSend={wireless=true}, onReset={reset=true},
        assistant={
            if(assistantPanel) TextButton(onClick={assistantPanel=false}) { Text("안내 닫기") }
            TripAssistant(trip,allowed&&!busy&&!loadFailed,showControls=assistantPanel,onSave={commit(it)},onArrive={id -> commit(trip.selectStop(id));step=4},onPrepare={step=2})
        }
    )
    if(!phone) VehiclePreparation(
        trip=trip,store=store,allowed=allowed,signal=signal,busy=busy,error=error,loadFailed=loadFailed,
        delivery=delivery,onDelivery={delivery=it},onSelect={commit(trip.selectStop(it))},
        onState={id,state->commit(trip.changeState(id,state))},
        onDelete={item->val next=trip.removeItem(item.id);commit(next);if(trip==next)store.deletePhoto(item.photo)},
        onReceive={wireless=true},onTools={tools=true},onExample={commit(Trip.example())},
        simulatedParked=simulatedParked,onParked={simulatedParked=it})
    if(tools && allowed) AlertDialog(onDismissRequest={tools=false},title={Text("외출 설정")},text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("방문지부터 차량 전송까지 한 단계씩 진행해요.")
            if(phone) TextButton(onClick={tools=false;openMap()},modifier=Modifier.fillMaxWidth()) { Text("지도에서 방문지 추가") }
            TextButton(onClick={tools=false;wireless=true},enabled=trip.items.isNotEmpty(),modifier=Modifier.fillMaxWidth()) { Text(if(phone) "차량으로 보내기" else "휴대폰에서 받기") }
            TextButton(onClick={tools=false;runCatching{exporter.launch("changyeoga-trip.zip")}.onFailure{error="파일 저장 앱을 열 수 없습니다."}},enabled=!loadFailed,modifier=Modifier.fillMaxWidth()) { Text("목록 내보내기") }
            TextButton(onClick={tools=false;importConfirm=true},modifier=Modifier.fillMaxWidth()) { Text("목록 가져오기") }
            TextButton(onClick={tools=false;reset=true},modifier=Modifier.fillMaxWidth()) { Text("새 외출") }
            Text("설치 버전 ${installedVersion}",color=MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick={tools=false;assistantPanel=true}) { Text("위치·음성 안내") }
        }
    },confirmButton={TextButton(onClick={tools=false}){Text("닫기")}})
    if(!phone&&assistantPanel&&allowed) AlertDialog(onDismissRequest={assistantPanel=false},title={Text("음성 안내")},text={
        TripAssistant(trip,!busy&&!loadFailed,showControls=true,onSave={commit(it)},onArrive={id->commit(trip.selectStop(id));delivery=true},onPrepare={delivery=false})
    },confirmButton={CAction("닫기"){assistantPanel=false}})
    if(addStop && allowed) AlertDialog(onDismissRequest={addStop=false},title={Text("방문지 추가")},text={OutlinedTextField(value=stopName,onValueChange={stopName=it.take(30)},label={Text("예: 외가, 친구 집")},singleLine=true,modifier=Modifier.testTag("stopName"))},confirmButton={TextButton(enabled=stopName.isNotBlank(),onClick={commit(trip.addStop(stopName));stopName="";addStop=false}){Text("추가")}},dismissButton={TextButton(onClick={addStop=false}){Text("취소")}})
    if(reset && allowed) AlertDialog(onDismissRequest={reset=false},title={Text("새 외출을 준비할까요?")},text={Text("현재 방문지, 짐과 사진이 이 기기에서 삭제됩니다.")},confirmButton={TextButton(enabled=!busy,onClick={val previous=trip; commit(Trip());if(trip==Trip()){runCatching { DepartureCheckStore(context).clear() };previous.items.forEach{store.deletePhoto(it.photo)};loadFailed=false;delivery=false;step=0};reset=false}){Text("초기화")}},dismissButton={TextButton(onClick={reset=false}){Text("취소")}})
}

@Composable
internal fun ItemCard(item: Luggage,store: TripStore,delivery: Boolean,enabled: Boolean,showActions: Boolean=true,onState: (ItemState)->Unit,onDelete: ()->Unit) {
    val bitmap by produceState<android.graphics.Bitmap?>(null,item.photo) {
        value=withContext(Dispatchers.IO) { item.photo?.let { runCatching { BitmapFactory.decodeFile(store.photoFile(it).absolutePath) }.getOrNull() } }
    }
    var preview by remember { mutableStateOf(false) }
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical=20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            if(bitmap!=null) Image(bitmap!!.asImageBitmap(),contentDescription=item.label,
                modifier=Modifier.size(if(BuildConfig.IS_VEHICLE) 88.dp else 72.dp).clickable { preview=true },contentScale=ContentScale.Crop)
            else AppSymbol(R.drawable.symbol_inventory_2)
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(item.label,fontSize=if(BuildConfig.IS_VEHICLE) 24.sp else 17.sp)
                Text(when(item.state){ItemState.READY->"준비 중";ItemState.LOADED->"차에 실었어요";ItemState.DELIVERED->"전달 완료"},color=MutedInk,fontSize=if(BuildConfig.IS_VEHICLE) 18.sp else 13.sp)
                if(delivery&&item.state==ItemState.READY) Text("먼저 적재를 확인해 주세요.",color=MutedInk,style=MaterialTheme.typography.bodySmall)
            }
            if(showActions&&!delivery&&item.state==ItemState.READY) CAction("실었어요",enabled){onState(ItemState.LOADED)}
            if(showActions&&delivery&&item.state==ItemState.LOADED) CAction("건넸어요",enabled){onState(ItemState.DELIVERED)}
            if(item.state!=ItemState.READY) CAction(if(item.state==ItemState.DELIVERED) "전달 취소" else "적재 취소",enabled){onState(if(item.state==ItemState.DELIVERED) ItemState.LOADED else ItemState.READY)}
            if(!delivery&&item.state==ItemState.READY) {
                var menu by remember { mutableStateOf(false) }
                Box { IconButton(onClick={menu=true},enabled=enabled){AppSymbol(R.drawable.symbol_more_horiz,"준비물 옵션")}
                    DropdownMenu(expanded=menu,onDismissRequest={menu=false}) { DropdownMenuItem(text={Text("삭제")},onClick={menu=false;onDelete()}) }
                }
            }
        }
        HorizontalDivider(color=Rule)
    }
    if(preview&&bitmap!=null) AlertDialog(onDismissRequest={preview=false},title={Text(item.label)},text={
        Image(bitmap!!.asImageBitmap(),contentDescription=item.label,modifier=Modifier.fillMaxWidth().heightIn(max=360.dp),contentScale=ContentScale.Fit)
    },confirmButton={CAction("닫기"){preview=false}})
}
