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
import androidx.core.widget.doAfterTextChanged
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private var lastQuery = ""
    private var searchJob: Job? = null
    private var authFailed = false
    private var placeName = "선택한 장소"
    private var previousAuthListener: NaverMapSdk.OnAuthFailedListener? = null
    private var authListener: NaverMapSdk.OnAuthFailedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(android.R.style.Theme_Material_Light_NoActionBar)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window,false)
        WindowCompat.getInsetsController(window,window.decorView).isAppearanceLightStatusBars=true
        WindowCompat.getInsetsController(window,window.decorView).isAppearanceLightNavigationBars=true
        val layout = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(android.graphics.Color.rgb(245,246,248)); setPadding(24,24,24,24) }
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
        val query=EditText(this).apply { hint="장소명 검색 · 예: 수원월드컵경기장";isSingleLine=true }
        layout.addView(query)
        status=TextView(this).apply { text="장소 이름을 입력하세요. 지도에서도 직접 선택할 수 있어요.";textSize=14f;setPadding(0,24,0,24) }
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
        val results=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        val resultScroll=ScrollView(this).apply { addView(results); visibility=View.GONE }
        layout.addView(resultScroll,LinearLayout.LayoutParams(-1,0,1f))
        layout.addView(status)
        val frame=FrameLayout(this).apply { id=R.id.visit_map_container }
        layout.addView(frame,LinearLayout.LayoutParams(-1,0,1f))
        confirm=Button(this).apply { text="이 위치로 저장";isEnabled=false;styleButton(this,true) }
        layout.addView(confirm)
        layout.addView(Button(this).apply { text="취소";styleButton(this,false);setOnClickListener { finish() } })
        val fragment=(supportFragmentManager.findFragmentById(frame.id) as? MapFragment)
            ?: MapFragment.newInstance().also { supportFragmentManager.beginTransaction().replace(frame.id,it).commitNow() }
        fragment.getMapAsync { map ->
            naver=map
            map.locationSource=locationSource
            map.uiSettings.isLocationButtonEnabled=true
            map.locationTrackingMode=LocationTrackingMode.Follow
            map.setOnMapClickListener { _, point -> searchJob?.cancel(); resultScroll.visibility=View.GONE; queryVersion++; choose(point) }
            val lat=savedInstanceState?.getDouble("latitude") ?: intent.getDoubleExtra("latitude",Double.NaN)
            val lon=savedInstanceState?.getDouble("longitude") ?: intent.getDoubleExtra("longitude",Double.NaN)
            if(lat.isFinite() && lon.isFinite()) choose(LatLng(lat,lon),savedInstanceState?.getString("placeName") ?: intent.getStringExtra("name"))
        }
        query.doAfterTextChanged {
            val address=it.toString().trim()
            if(address==lastQuery) return@doAfterTextChanged
            lastQuery=address
            val request=++queryVersion
            searchJob?.cancel()
            results.removeAllViews()
            resultScroll.visibility=View.GONE
            frame.visibility=View.VISIBLE
            selected=null
            marker.map=null
            confirm.isEnabled=false
            if(address.length<2) {
                status.text="장소명을 두 글자 이상 입력하거나 지도를 눌러 주세요."
                return@doAfterTextChanged
            }
            status.text="장소를 찾고 있어요…"
            searchJob=lifecycleScope.launch {
                delay(350)
                val result=withContext(Dispatchers.IO) { runCatching { PlaceSearch.find(address) } }
                if(request!=queryVersion || isFinishing || isDestroyed) return@launch
                result.fold(onSuccess={ places ->
                    status.text=if(places.isEmpty()) "검색 결과가 없어요. 지역을 함께 입력해 주세요." else "검색 결과 · 장소를 눌러 선택하세요"
                    resultScroll.visibility=if(places.isEmpty()) View.GONE else View.VISIBLE
                    frame.visibility=if(places.isEmpty()) View.VISIBLE else View.GONE
                    places.forEach { item ->
                        results.addView(Button(this@PlaceMapActivity).apply {
                            text="${item.name}\n${item.address}"
                            styleButton(this,false)
                            textSize=17f
                            text=android.text.SpannableString(text).apply {
                                setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD),0,item.name.length,0)
                                setSpan(android.text.style.RelativeSizeSpan(0.82f),item.name.length+1,length,0)
                            }
                            isAllCaps=false
                            gravity=android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
                            minHeight=(64*resources.displayMetrics.density).toInt()
                            setOnClickListener {
                                searchJob?.cancel()
                                resultScroll.visibility=View.GONE
                                frame.visibility=View.VISIBLE
                                query.clearFocus()
                                (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).hideSoftInputFromWindow(query.windowToken,0)
                                choose(LatLng(item.latitude,item.longitude),item.name,item.address)
                            }
                        })
                    }
                },onFailure={
                    status.text="검색 서버가 응답하지 않아요. 잠시 후 글자를 수정해 다시 검색하거나 지도에서 선택해 주세요."
                })
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
    private fun styleButton(button:Button,primary:Boolean) {
        val density=resources.displayMetrics.density
        button.isAllCaps=false
        button.minHeight=(60*density).toInt()
        button.setPadding((16*density).toInt(),(12*density).toInt(),(16*density).toInt(),(12*density).toInt())
        button.background=android.graphics.drawable.GradientDrawable().apply {
            cornerRadius=16*density
            setColor(if(primary) android.graphics.Color.rgb(49,94,232) else android.graphics.Color.WHITE)
        }
        button.backgroundTintList=android.content.res.ColorStateList(
            arrayOf(intArrayOf(-android.R.attr.state_enabled),intArrayOf()),
            intArrayOf(android.graphics.Color.rgb(225,229,237),if(primary) android.graphics.Color.rgb(49,94,232) else android.graphics.Color.WHITE))
        button.setTextColor(android.content.res.ColorStateList(
            arrayOf(intArrayOf(-android.R.attr.state_enabled),intArrayOf()),
            intArrayOf(android.graphics.Color.rgb(89,97,109),if(primary) android.graphics.Color.WHITE else android.graphics.Color.rgb(23,27,34))))
        button.layoutParams=LinearLayout.LayoutParams(-1,-2).apply { topMargin=(8*density).toInt() }
    }
    private fun choose(point: LatLng, name:String?=null, address:String?=null) {
        if(authFailed) return
        placeName=name ?: "선택한 장소"
        selected=point
        naver?.locationTrackingMode=LocationTrackingMode.None
        marker.position=point
        marker.map=naver
        naver?.moveCamera(CameraUpdate.scrollAndZoomTo(point,16.0))
        status.text="선택한 장소를 확인한 뒤 ‘이 위치로 저장’을 눌러 주세요."
        val request=++queryVersion
        if(name!=null) {
            status.text="$name\n${address.orEmpty()}"
            confirm.isEnabled=true
            return
        }
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
        outState.putString("placeName",placeName)
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
