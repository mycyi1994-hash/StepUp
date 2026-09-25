package com.stepup.android

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.stepup.android.core.ServiceLocator
import com.stepup.android.service.RunSaveStatus
import com.stepup.android.service.WalkSessionService
import com.stepup.android.service.WalkSessionState
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Routes
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Real service/Room failure with a synthetic stopped run; no GPS or server reward is simulated. */
class RunSaveRecoveryTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun failedFinishCanRetryFromTheRunScreenWithoutLosingOrDuplicatingTheRecord() {
        val db = ServiceLocator.database
        val prefs = ServiceLocator.userPrefs
        val previousCourse = runBlocking {
            prefs.setGuideSeen()
            prefs.setReducedMotion(true)
            prefs.setSounds(false)
            val previous = prefs.selectedCourseNow()
            prefs.setSelectedCourse(-1)
            previous
        }
        val startedAt = System.currentTimeMillis() - 500_000
        clearAnyRunCheckpointForTest() // 앞 테스트가 남긴 다른 러닝의 저장본이 있으면 이 러닝을 저장할 수 없다
        WalkSessionService.showStateForTest(WalkSessionState(isActive = true, isPaused = true,
            startedAt = startedAt, recordingOwner = "guest", steps = 500, elapsedSec = 500))
        db.openHelper.writableDatabase.execSQL("""
            CREATE TRIGGER fail_test_run_save BEFORE INSERT ON run_settlements
            WHEN NEW.startedAt = $startedAt
            BEGIN SELECT RAISE(ABORT, 'injected local storage failure'); END
        """.trimIndent())
        try {
            compose.setContent {
                StepUpTheme(ThemeMode.DARK) {
                    ExperienceProvider { MainScaffold(initialRoute = Routes.RUN) }
                }
            }
            compose.onNodeWithTag("run-finish").performClick()
            compose.onNodeWithText(compose.activity.getString(R.string.run_stop_confirm_yes)).performClick()
            compose.waitUntil(10_000) { WalkSessionService.state.value.saveStatus == RunSaveStatus.FAILED }
            compose.onNodeWithTag("run-save-error").assertIsDisplayed()
            compose.onNodeWithTag("run-primary-action").assertIsEnabled()
            assertEquals(500, WalkSessionService.state.value.steps)
            assertEquals(startedAt, WalkSessionService.state.value.startedAt)
            runBlocking { assertNull(db.runSettlementDao().find("guest", startedAt)) }
            captureDisplay(File(compose.activity.getExternalFilesDir(null), "form-checks/run-save-failed.png"))

            db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_test_run_save")
            compose.onNodeWithTag("run-primary-action").performClick()
            compose.waitUntil(10_000) { !WalkSessionService.state.value.isActive &&
                WalkSessionService.state.value.lastStartedAt == startedAt }
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("run-result-done").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("run-result-done").assertIsDisplayed()
            runBlocking { assertNotNull(db.runSettlementDao().find("guest", startedAt)) }
            db.openHelper.readableDatabase.query(
                "SELECT COUNT(*) FROM walk_sessions WHERE recordingOwner = 'guest' AND startedAt = $startedAt",
            ).use { assertTrue(it.moveToFirst()); assertEquals(1, it.getInt(0)) }
            assertEquals(500, WalkSessionService.state.value.lastSessionSteps)
            captureDisplay(File(compose.activity.getExternalFilesDir(null), "form-checks/run-save-retried.png"))
        } finally {
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER IF EXISTS fail_test_run_save")
            compose.activity.stopService(Intent(compose.activity, WalkSessionService::class.java))
            WalkSessionService.showStateForTest(WalkSessionState())
            clearAnyRunCheckpointForTest()
            runBlocking { prefs.setSelectedCourse(previousCourse) }
        }
    }
}
