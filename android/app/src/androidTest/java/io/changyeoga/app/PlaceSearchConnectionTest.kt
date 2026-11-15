package io.changyeoga.app
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Button
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.action.ViewActions.click
import org.hamcrest.Matchers.containsString
import org.junit.Test
class PlaceSearchConnectionTest {
 @Test fun stadiumSearchSelectsPlaceAndOffersNickname() {
  if(BuildConfig.IS_VEHICLE) return
  val context=ApplicationProvider.getApplicationContext<Context>()
  val intent=Intent().setClassName(context,"io.changyeoga.app.PlaceMapActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  ActivityScenario.launch<Activity>(intent).use { scenario ->
   scenario.onActivity { activity ->
    fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap { all(v.getChildAt(it)) } else emptyList()
    val views=all(activity.window.decorView)
    views.filterIsInstance<EditText>().first().setText("수원월드컵경기장")
    check(views.filterIsInstance<Button>().none { it.text=="장소 찾기" })
   }
   var found=false
   repeat(20) {
    if(!found) { Thread.sleep(1000);try { onView(withText(containsString("경기도 수원시 팔달구 월드컵로 310"))).check(matches(isDisplayed()));found=true } catch(_:androidx.test.espresso.NoMatchingViewException) {} }
   }
   check(found) { "Stadium result did not appear" }
   onView(withText(containsString("경기도 수원시 팔달구 월드컵로 310"))).perform(click())
   onView(withText("이 위치로 저장")).perform(click())
   onView(withText("별명을 붙일까요?")).check(matches(isDisplayed()))
  }
 }
}
