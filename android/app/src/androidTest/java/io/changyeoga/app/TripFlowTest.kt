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
        ui.onNodeWithText("더보기").performClick()
        ui.onNodeWithText("새 외출").performClick()
        ui.onNodeWithText("초기화").performClick()
    }
    @Test fun phoneFollowsStepsAndBlocksUnloadedDeparture() {
        if(BuildConfig.IS_VEHICLE) return
        ui.onNodeWithText("예시로 둘러보기").performClick()
        ui.onNodeWithText("다음 · 짐 사진 등록").performClick()
        ui.onNodeWithText("실었어요").assertDoesNotExist()
        ui.onNodeWithText("다음 · 차에 싣기").performClick()
        ui.onNodeWithText("다음 · 차량에 보내기").assertIsNotEnabled()
        ui.onNodeWithText("실었어요").performClick()
        ui.onNodeWithText("친가",useUnmergedTree=true).performClick()
        ui.onNodeWithText("실었어요").performClick()
        ui.onNodeWithText("다음 · 차량에 보내기").performClick()
        ui.onNodeWithText("차량으로 보내기").assertIsDisplayed()
        ui.onNodeWithText("다음 · 도착 후 전달").performClick()
        ui.onNodeWithText("건넸어요").assertIsDisplayed()
        ui.onNodeWithText("외출 마치기").assertIsNotEnabled()
    }
    @Test fun prepareDeliverUndoAndRestart() {
        if(!BuildConfig.IS_VEHICLE) return
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
    @Test fun transferRoundTripAndRejectUnsafeArchive() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val store=TripStore(context)
        val bitmap=Bitmap.createBitmap(32,32,Bitmap.Config.ARGB_8888)
        val photo=store.savePhoto(bitmap)
        val original=Trip.example().addItem(Luggage(stopId="grandpa",photo=photo)).changeState("fruit",ItemState.LOADED)
        val bytes=java.io.ByteArrayOutputStream()
        store.exportTrip(original,bytes)
        store.save(Trip())
        val imported=store.importTrip(bytes.toByteArray().inputStream())
        assertEquals(original.stops,imported.stops)
        assertEquals(original.items.map{it.state},imported.items.map{it.state})
        assertNotEquals(photo,imported.items.last().photo)
        assertArrayEquals(store.photoFile(photo).readBytes(),store.photoFile(imported.items.last().photo!!).readBytes())
        val bad=java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(bad).use { it.putNextEntry(java.util.zip.ZipEntry("../trip.json"));it.write(byteArrayOf(1));it.closeEntry() }
        assertTrue(runCatching { store.importTrip(bad.toByteArray().inputStream()) }.isFailure)
        assertEquals(imported,store.load())
        store.deletePhoto(photo);store.deletePhoto(imported.items.last().photo);store.save(Trip());bitmap.recycle()
    }
}
