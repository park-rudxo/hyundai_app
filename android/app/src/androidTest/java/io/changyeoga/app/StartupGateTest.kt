package io.changyeoga.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StartupGateTest {
    @get:Rule val ui=createComposeRule()

    @Test fun opensOnlyAfterServerReady() {
        val ready=CompletableDeferred<Unit>()
        ui.setContent { MaterialTheme { StartupGate(prepare={ready.await()}) { Text("준비 화면") } } }
        ui.onNodeWithText("장소 검색을 준비하고 있어요").assertIsDisplayed()
        ui.onNodeWithText("준비 화면").assertDoesNotExist()
        ready.complete(Unit)
        ui.waitUntil(5000) { ui.onAllNodesWithText("준비 화면").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithText("준비 화면").assertIsDisplayed()
    }

    @Test fun failedConnectionCanRetry() {
        var calls=0
        ui.setContent { MaterialTheme { StartupGate(prepare={if(++calls==1) error("offline")}) { Text("준비 화면") } } }
        ui.onNodeWithText("연결을 확인해 주세요").assertIsDisplayed()
        ui.onNodeWithText("다시 시도").performClick()
        ui.onNodeWithText("준비 화면").assertIsDisplayed()
        assertEquals(2,calls)
    }

    @Test fun canContinueWhileServerIsUnavailable() {
        ui.setContent { MaterialTheme { StartupGate(prepare={awaitCancellation()}) { Text("준비 화면") } } }
        ui.onNodeWithText("준비 화면 먼저 열기").performClick()
        ui.onNodeWithText("준비 화면").assertIsDisplayed()
        ui.onNodeWithText("장소 검색을 준비하고 있어요").assertDoesNotExist()
    }
}
