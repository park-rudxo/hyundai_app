package io.changyeoga.app

import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.content.pm.PackageManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import android.view.View
import android.widget.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.util.FusedLocationSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/** User chooses a destination, never silently replaces it with the phone position. */
class PlaceMapActivity : FragmentActivity() {
    private lateinit var locationSource: FusedLocationSource
    private var selected: LatLng? = null
    private var naver: NaverMap? = null
    private val marker = Marker()
    private lateinit var status: TextView
    private lateinit var confirm: Button
    private var queryVersion = 0
    private var authFailed = false
    private var placeName = "선택한 장소"
    private var previousAuthListener: NaverMapSdk.OnAuthFailedListener? = null
    private var authListener: NaverMapSdk.OnAuthFailedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window,false)
        val layout = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(24,24,24,24) }
        setContentView(layout)
        ViewCompat.setOnApplyWindowInsetsListener(layout) { view,insets ->
            val bars=insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(24+bars.left,24+bars.top,24+bars.right,24+bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(layout)
        layout.addView(TextView(this).apply { text="방문지 위치 선택"; textSize=24f })
        @Suppress("DEPRECATION")
        val key=packageManager.getApplicationInfo(packageName,PackageManager.GET_META_DATA).metaData?.getString("com.naver.maps.map.NCP_KEY_ID").orEmpty()
        if(key.isBlank()) {
            layout.addView(TextView(this).apply { text="이 설치 파일에는 지도 설정이 없습니다. 앱 ${BuildConfig.VERSION_NAME} · 지도 설정이 포함된 최신 APK로 업데이트해 주세요." })
            layout.addView(Button(this).apply { text="돌아가기";setOnClickListener { finish() } })
            return
        }
        locationSource=FusedLocationSource(this,100)
        val query=EditText(this).apply { hint="도로명·지번 주소 검색";isSingleLine=true }
        layout.addView(query)
        status=TextView(this).apply { text="지도를 눌러 방문지를 선택하세요. 현재 위치는 오른쪽 아래 버튼으로 확인합니다." }
        val sdk=NaverMapSdk.getInstance(this)
        sdk.client=NaverMapSdk.NcpKeyClient(key)
        previousAuthListener=sdk.onAuthFailedListener
        authListener=NaverMapSdk.OnAuthFailedListener {
            runOnUiThread {
                if(!isFinishing && !isDestroyed) {
                    authFailed=true
                    selected=null
                    if(::confirm.isInitialized) confirm.isEnabled=false
                    status.text="지도 인증에 실패했습니다. Client ID, Dynamic Map 활성화와 등록한 Android 패키지를 확인해 주세요."
                }
            }
        }
        sdk.onAuthFailedListener=authListener
        val search=Button(this).apply { text="주소 찾기" }
        layout.addView(search)
        layout.addView(status)
        val frame=FrameLayout(this).apply { id=R.id.visit_map_container }
        layout.addView(frame,LinearLayout.LayoutParams(-1,0,1f))
        confirm=Button(this).apply { text="이 위치로 저장";isEnabled=false }
        layout.addView(confirm)
        layout.addView(Button(this).apply { text="취소";setOnClickListener { finish() } })
        val fragment=(supportFragmentManager.findFragmentById(frame.id) as? MapFragment)
            ?: MapFragment.newInstance().also { supportFragmentManager.beginTransaction().replace(frame.id,it).commitNow() }
        fragment.getMapAsync { map ->
            naver=map
            map.locationSource=locationSource
            map.uiSettings.isLocationButtonEnabled=true
            map.locationTrackingMode=LocationTrackingMode.Follow
            map.setOnMapClickListener { _, point -> queryVersion++; choose(point) }
            val lat=savedInstanceState?.getDouble("latitude") ?: intent.getDoubleExtra("latitude",Double.NaN)
            val lon=savedInstanceState?.getDouble("longitude") ?: intent.getDoubleExtra("longitude",Double.NaN)
            if(lat.isFinite() && lon.isFinite()) choose(LatLng(lat,lon))
        }
        search.setOnClickListener {
            val address=query.text.toString().trim()
            if(address.isEmpty()) { status.text="검색할 주소를 입력해 주세요.";return@setOnClickListener }
            val request=++queryVersion
            search.isEnabled=false
            status.text="주소를 찾고 있어요…"
            lifecycleScope.launch {
                val result=withContext(Dispatchers.IO) {
                    runCatching {
                        @Suppress("DEPRECATION")
                        Geocoder(this@PlaceMapActivity,Locale.KOREA).getFromLocationName(address,5).orEmpty()
                    }.getOrDefault(emptyList())
                }
                search.isEnabled=true
                if(request!=queryVersion) return@launch
                if(result.isEmpty()) status.text="주소를 찾지 못했습니다. 자세한 주소를 입력하거나 지도에서 선택하세요."
                else android.app.AlertDialog.Builder(this@PlaceMapActivity)
                    .setTitle("방문할 주소를 선택하세요")
                    .setItems(result.map { it.getAddressLine(0) ?: address }.toTypedArray()) { _, index ->
                        val item=result[index]
                        placeName=item.getAddressLine(0) ?: address
                        choose(LatLng(item.latitude,item.longitude))
                    }.setNegativeButton("취소",null).show()
            }
        }
        confirm.setOnClickListener {
            if(selected==null || authFailed) return@setOnClickListener
            val nickname=EditText(this).apply { hint="예: 외가, 친구 집 (선택)";setSingleLine(true) }
            fun save(name:String) {
                val point=selected ?: return
                if(authFailed) return
                setResult(RESULT_OK,Intent().putExtra("latitude",point.latitude).putExtra("longitude",point.longitude).putExtra("name",name.take(30)))
                finish()
            }
            android.app.AlertDialog.Builder(this).setTitle("별명을 붙일까요?")
                .setMessage("별명 없이 저장하면 선택한 주소나 장소 이름을 사용해요.")
                .setView(nickname).setPositiveButton("저장") { _,_ -> save(nickname.text.toString().trim().ifBlank { placeName }) }
                .setNeutralButton("별명 없이 저장") { _,_ -> save(placeName) }
                .setNegativeButton("취소",null).show()
        }
    }
    private fun choose(point: LatLng) {
        if(authFailed) return
        placeName="선택한 장소"
        selected=point
        naver?.locationTrackingMode=LocationTrackingMode.None
        marker.position=point
        marker.map=naver
        naver?.moveCamera(CameraUpdate.scrollAndZoomTo(point,16.0))
        status.text="선택한 장소를 확인한 뒤 ‘이 위치로 저장’을 눌러 주세요."
        val request=++queryVersion
        lifecycleScope.launch {
            val address=withContext(Dispatchers.IO) { runCatching {
                @Suppress("DEPRECATION")
                Geocoder(this@PlaceMapActivity,Locale.KOREA).getFromLocation(point.latitude,point.longitude,1)?.firstOrNull()?.getAddressLine(0)
            }.getOrNull() }
            if(request==queryVersion && selected==point) {
                placeName=address ?: "선택한 장소"
                status.text="$placeName\n이 장소가 맞으면 저장해 주세요."
            }
        }
        confirm.isEnabled=true
    }
    override fun onSaveInstanceState(outState: Bundle) {
        selected?.let { outState.putDouble("latitude",it.latitude);outState.putDouble("longitude",it.longitude) }
        super.onSaveInstanceState(outState)
    }
    override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults)
        if(::locationSource.isInitialized) locationSource.onRequestPermissionsResult(requestCode,permissions,grantResults)
    }
    override fun onDestroy() {
        if(authListener!=null) {
            val sdk=NaverMapSdk.getInstance(this)
            if(sdk.onAuthFailedListener===authListener) sdk.onAuthFailedListener=previousAuthListener
        }
        super.onDestroy()
    }
}
