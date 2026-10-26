package com.stepup.android

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stepup.android.service.WalkSessionState
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.walk.LockRunPanel
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 잠금 화면 가운데 러닝 현황(2026-10-26) — 시간 · 거리 · 페이스, 주 버튼 하나(일시정지 ↔ 다시 달리기) */
@RunWith(AndroidJUnit4::class)
class LockRunPanelTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun showsTheRunInTheMiddleAndTogglesPause() {
        var state by mutableStateOf(WalkSessionState(isActive = true, elapsedSec = 1_234, gpsKm = 3.21, steps = 3_100))
        var toggles = 0
        var opens = 0
        compose.setContent {
            StepUpTheme(ThemeMode.LIGHT) { ExperienceProvider {
                LockRunPanel(state = state, onToggle = { toggles++ }, onOpen = { opens++ })
            } }
        }
        val activity = compose.activity
        compose.onNodeWithText(activity.getString(R.string.run_notif_title)).assertIsDisplayed()
        compose.onNodeWithText("20:34").assertIsDisplayed()
        compose.onNodeWithText("3.21").assertIsDisplayed()
        // 1,234초 ÷ 3.21km = 384초 → 6'24"
        compose.onNodeWithText("6'24\"").assertIsDisplayed()
        capture("lock-run-running.png")

        compose.onNodeWithText(activity.getString(R.string.run_notif_pause)).performClick()
        compose.runOnIdle { assertEquals(1, toggles) }
        compose.runOnIdle { state = state.copy(isPaused = true) }
        compose.onNodeWithText(activity.getString(R.string.run_notif_title_paused)).assertIsDisplayed()
        compose.onNodeWithText(activity.getString(R.string.run_notif_resume)).assertIsDisplayed()
        capture("lock-run-paused.png")

        compose.onNodeWithText(activity.getString(R.string.lock_run_open)).performClick()
        compose.runOnIdle { assertEquals(1, opens) }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val directory = File(compose.activity.getExternalFilesDir(null), "form-checks").apply { mkdirs() }
        captureDisplay(File(directory, name))
    }
}
