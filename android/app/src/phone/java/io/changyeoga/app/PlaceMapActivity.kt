package io.changyeoga.app

import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
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
    private var previousAuthListener: NaverMapSdk.OnAuthFailedListener? = null
    private var authListener: NaverMapSdk.OnAuthFailedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(24,24,24,24) }
        setContentView(layout)
        layout.addView(TextView(this).apply { text="방문지 위치 선택"; textSize=24f })
        if(!BuildConfig.HAS_NAVER_MAP_KEY) {
            layout.addView(TextView(this).apply { text="지도 연결 설정을 기다리고 있어요.\n지금은 방문지 이름과 사진으로 계속 준비할 수 있습니다." })
            layout.addView(Button(this).apply { text="돌아가기";setOnClickListener { finish() } })
            return
        }
        locationSource=FusedLocationSource(this,100)
        val query=EditText(this).apply { hint="도로명·지번 주소 검색";isSingleLine=true }
        layout.addView(query)
        status=TextView(this).apply { text="지도를 눌러 방문지를 선택하세요. 현재 위치는 오른쪽 아래 버튼으로 확인합니다." }
        val sdk=NaverMapSdk.getInstance(this)
        previousAuthListener=sdk.onAuthFailedListener
        authListener=NaverMapSdk.OnAuthFailedListener {
            runOnUiThread {
                if(!isFinishing && !isDestroyed) {
                    authFailed=true
                    selected=null
                    confirm.isEnabled=false
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
                        choose(LatLng(item.latitude,item.longitude))
                    }.setNegativeButton("취소",null).show()
            }
        }
        confirm.setOnClickListener {
            selected?.let { setResult(RESULT_OK,Intent().putExtra("latitude",it.latitude).putExtra("longitude",it.longitude));finish() }
        }
    }
    private fun choose(point: LatLng) {
        if(authFailed) return
        selected=point
        marker.position=point
        marker.map=naver
        naver?.moveCamera(CameraUpdate.scrollAndZoomTo(point,16.0))
        status.text="선택한 위치 %.5f, %.5f\n맞는 방문지인지 확인한 뒤 저장해 주세요.".format(point.latitude,point.longitude)
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
