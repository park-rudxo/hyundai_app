package io.changyeoga.app

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TripFlowTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    @Before fun reset() {
        ui.onNodeWithText("새 외출").performClick()
        ui.onNodeWithText("초기화").performClick()
    }
    @Test fun prepareDeliverUndoAndRestart() {
        ui.onNodeWithText("예시로 둘러보기").performClick()
        ui.onNodeWithText("실었어요").performClick()
        ui.onNodeWithText("2  도착·전달").performClick()
        ui.onNodeWithText("건넸어요").performClick()
        ui.onNodeWithText("전달 완료").assertIsDisplayed()
        ui.onNodeWithText("친가",useUnmergedTree=true).performClick()
        ui.onNodeWithText("선물 상자").assertIsDisplayed()
        ui.onNodeWithText("과일 바구니").assertDoesNotExist()
        ui.onNodeWithText("건넸어요").assertDoesNotExist()
        ui.activityRule.scenario.recreate()
        ui.onNodeWithText("선물 상자").assertIsDisplayed()
        ui.onNodeWithText("외가",useUnmergedTree=true).performClick()
        ui.onNodeWithText("전달 완료").assertIsDisplayed()
        ui.onNodeWithText("되돌리기").performClick()
        ui.onNodeWithText("차에 실었어요").assertIsDisplayed()
    }
    @Test fun drivingHidesPhotosAndActions() {
        if(!BuildConfig.IS_DEMO) return
        ui.onNodeWithText("예시로 둘러보기").performClick()
        ui.onNodeWithTag("parkingSwitch").performClick()
        ui.onNodeWithText("주차한 뒤 확인해 주세요").assertIsDisplayed()
        ui.onNodeWithText("실었어요").assertDoesNotExist()
        ui.onNodeWithText("과일 바구니").assertDoesNotExist()
        ui.onNodeWithTag("parkingSwitch").performClick()
        ui.onNodeWithText("과일 바구니").assertIsDisplayed()
    }
    @Test fun photoAndTripSurviveStoreReload() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val store=TripStore(context)
        val bitmap=Bitmap.createBitmap(80,80,Bitmap.Config.ARGB_8888).apply{eraseColor(android.graphics.Color.GREEN)}
        val name=store.savePhoto(bitmap)
        val trip=Trip.example().addItem(Luggage(stopId="grandma",photo=name)).changeState("fruit",ItemState.LOADED)
        store.save(trip)
        assertEquals(trip,TripStore(context).load())
        assertTrue(store.photoFile(name).length()>0)
        store.deletePhoto(name)
        assertFalse(store.photoFile(name).exists())
        store.save(Trip())
        bitmap.recycle()
    }
}
