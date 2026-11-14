package io.changyeoga.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test

class MapScreenTest {
    @Test fun configuredPhoneOpensMapSearchInsteadOfWaitingScreen() {
        if(BuildConfig.IS_VEHICLE) return
        // map-sdk compiles without this optional dependency but crashes on location setup.
        assertNotNull(Class.forName("com.google.android.gms.location.LocationServices"))
        val context=ApplicationProvider.getApplicationContext<Context>()
        @Suppress("DEPRECATION")
        val key=context.packageManager.getApplicationInfo(context.packageName,PackageManager.GET_META_DATA)
            .metaData?.getString("com.naver.maps.map.NCP_KEY_ID")
        assertFalse("Phone APK must contain a map key",key.isNullOrBlank())
        val intent=Intent().setClassName(context,"io.changyeoga.app.PlaceMapActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ActivityScenario.launch<Activity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                fun views(view:View):List<View> = listOf(view) + if(view is ViewGroup)
                    (0 until view.childCount).flatMap { views(view.getChildAt(it)) } else emptyList()
                val all=views(activity.window.decorView)
                assertTrue(all.filterIsInstance<EditText>().any { it.hint.toString().contains("장소명 검색") })
                assertFalse(all.filterIsInstance<TextView>().any { it.text.contains("설정을 기다리고") })
            }
        }
    }
}
