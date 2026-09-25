package com.stepup.android

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.rule.GrantPermissionRule
import com.stepup.android.core.ServiceLocator
import com.stepup.android.service.RunCheckpoint
import com.stepup.android.service.WalkSessionService
import com.stepup.android.service.WalkSessionState
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/**
 * 앱이 죽은 뒤 다시 열었을 때 — 러닝 저장본이 남아 있으면 "멈춘 러닝"을 묻고, 이어 달리거나
 * 끝내고 저장할 수 있다. 실제 러닝 서비스 · Room 을 쓰고, 프로세스가 죽은 것은 저장본만 남기고
 * 서비스 상태를 비워 흉내 낸다.
 */
class RunCrashRecoveryTest {
    @get:Rule(order = 0) val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.ACTIVITY_RECOGNITION,
        android.Manifest.permission.POST_NOTIFICATIONS,
    )
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()

    private fun leaveInterruptedRun(startedAt: Long, mock: Boolean = false) = runBlocking {
        val prefs = ServiceLocator.userPrefs
        prefs.setGuideSeen()
        prefs.setReducedMotion(true)
        prefs.setSounds(false)
        prefs.setSelectedCourse(-1)
        WalkSessionService.showStateForTest(WalkSessionState()) // 프로세스가 죽어 메모리의 러닝은 없다
        ServiceLocator.runCheckpoints.save(
            RunCheckpoint(
                WalkSessionState(isActive = true, startedAt = startedAt, recordingOwner = "guest",
                    steps = 400, elapsedSec = 300, mockLocation = mock),
                goalKm = 5.0, savedAt = startedAt + 300_000,
            ),
        )
    }

    private fun show() {
        compose.setContent {
            StepUpTheme(ThemeMode.DARK) {
                ExperienceProvider { MainScaffold() }
            }
        }
    }

    private fun cleanUp(startedAt: Long) {
        compose.activity.stopService(Intent(compose.activity, WalkSessionService::class.java))
        WalkSessionService.showStateForTest(WalkSessionState())
        runBlocking { runCatching { ServiceLocator.runCheckpoints.clear(startedAt, "guest") } }
    }

    @Test fun finishSavesTheInterruptedRunOnceAndClearsTheCheckpoint() {
        val startedAt = System.currentTimeMillis() - 900_000
        leaveInterruptedRun(startedAt)
        try {
            show()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("run-recover-finish").fetchSemanticsNodes().isNotEmpty() }
            captureDisplay(File(compose.activity.getExternalFilesDir(null), "form-checks/run-recover-dialog.png"))
            compose.onNodeWithTag("run-recover-finish").performClick()
            compose.waitUntil(15_000) {
                !WalkSessionService.state.value.isActive && WalkSessionService.state.value.lastStartedAt == startedAt
            }
            assertEquals(400, WalkSessionService.state.value.lastSessionSteps)
            assertEquals(300L, WalkSessionService.state.value.lastElapsedSec)
            runBlocking {
                assertNotNull(ServiceLocator.database.runSettlementDao().find("guest", startedAt))
                assertNull("저장이 끝나면 저장본을 지운다", ServiceLocator.runCheckpoints.read())
            }
            ServiceLocator.database.openHelper.readableDatabase.query(
                "SELECT COUNT(*) FROM walk_sessions WHERE recordingOwner = 'guest' AND startedAt = $startedAt",
            ).use { assertTrue(it.moveToFirst()); assertEquals(1, it.getInt(0)) }
        } finally {
            cleanUp(startedAt)
        }
    }

    @Test fun resumeBringsTheRunBackPausedWithoutAddingDowntime() {
        val startedAt = System.currentTimeMillis() - 900_000
        leaveInterruptedRun(startedAt, mock = true)
        try {
            show()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("run-recover-resume").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("run-recover-resume").performClick()
            compose.waitUntil(15_000) { WalkSessionService.state.value.let { it.isActive && it.startedAt == startedAt } }
            val state = WalkSessionService.state.value
            assertTrue("되살린 러닝은 일시정지로 시작한다", state.isPaused)
            assertEquals(400, state.steps)
            assertEquals("꺼져 있던 15분을 더하지 않는다", 300L, state.elapsedSec)
            assertTrue("가짜 위치 표시가 남는다", state.mockLocation)
        } finally {
            cleanUp(startedAt)
        }
    }
}
