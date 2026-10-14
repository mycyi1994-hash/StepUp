package com.stepup.android

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.RunPlans
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
            prefs.setCourseRecording(false)
            previous
        }
        // 자유 러닝(코스 없음 · 계획 자유) 화면에서 본다 — 앞 테스트가 고른 계획 · 이벤트 챌린지가 남아 있으면 지운다
        RunPlans.clear()
        com.stepup.android.ui.screens.events.ChallengeRunFocus.clear()
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
            compose.onNodeWithTag("run-end-save").performClick()
            compose.waitUntil(10_000) { WalkSessionService.state.value.saveStatus == RunSaveStatus.FAILED }
            // 07 — 자유 러닝은 실패를 창 대신 지도 위 띠와 판 아래 "화면에 머물기"로 한 번 알린다.
            // "화면에 머물기"를 누르면 그 버튼만 숨고, 띠와 다시 저장은 화면에 남는다
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("run-save-stay").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("run-save-error").assertIsDisplayed()
            compose.onNodeWithTag("run-save-kept").assertIsDisplayed()
            compose.onNodeWithTag("run-save-failed-dialog").assertDoesNotExist()
            // 저장 실패 화면에는 종료 · 러닝 정보(⋯)가 없다 — 할 일은 다시 저장 하나
            compose.onNodeWithTag("run-finish").assertDoesNotExist()
            compose.onNodeWithTag("run-details-open").assertDoesNotExist()
            captureDisplay(File(compose.activity.getExternalFilesDir(null), "form-checks/run-save-failed-stay.png"))
            compose.onNodeWithTag("run-save-stay").performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("run-save-stay").assertDoesNotExist()
            compose.onNodeWithTag("run-save-failed-dialog").assertDoesNotExist()
            compose.onNodeWithTag("run-save-error").assertIsDisplayed()
            compose.onNodeWithTag("run-primary-action").assertIsEnabled()
                .assertTextContains(compose.activity.getString(R.string.runflow_save_again))
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
            // 다시 저장한 뒤에도 자유 러닝 결과다 — 기록 저장 완료만 알린다
            compose.onNodeWithTag("run-result-saved").assertIsDisplayed()
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
