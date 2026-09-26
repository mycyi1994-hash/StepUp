package com.stepup.android

import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.walk.RunCountdown
import com.stepup.android.ui.screens.walk.RunPermissionItem
import com.stepup.android.ui.screens.walk.RunPermissionPrimer
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * S2 러닝 시작 전 화면 — 권한 안내(시안 23)와 3-2-1(시안 24).
 * 러닝 서비스를 실제로 켜지 않도록 부품을 직접 그리고, 시작 콜백 횟수만 센다.
 */
@RunWith(AndroidJUnit4::class)
class RunStartFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun countdownStartsExactlyOnceAndTapSkipsAhead() {
        var starts = 0
        compose.mainClock.autoAdvance = false
        compose.setContent {
            StepUpTheme(ThemeMode.DARK) { ExperienceProvider {
                RunCountdown(courseName = null, locationAllowed = true, onGo = { starts++ }, onCancel = {})
            } }
        }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("3").assertIsDisplayed()
        capture("run-countdown-3.png")
        compose.mainClock.advanceTimeBy(1_100)
        compose.onNodeWithText("2").assertIsDisplayed()
        compose.onNodeWithTag("run-countdown").performClick()
        compose.mainClock.advanceTimeBy(5_000)
        assertEquals("tap starts once and the timer does not start again", 1, starts)
    }

    @Test fun cancelledCountdownNeverStarts() {
        var starts = 0
        var shown by mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            StepUpTheme(ThemeMode.DARK) { ExperienceProvider {
                if (shown) {
                    RunCountdown(courseName = "Hangang 5K", locationAllowed = false,
                        onGo = { starts++ }, onCancel = { shown = false })
                }
            } }
        }
        compose.mainClock.advanceTimeByFrame()
        capture("run-countdown-no-gps.png")
        compose.onNodeWithTag("run-countdown-cancel").performClick()
        compose.mainClock.advanceTimeBy(5_000)
        assertEquals(0, starts)
    }

    @Test fun primerExplainsEachPermissionAndHandsOff() {
        var allowed = 0
        var later = 0
        compose.setContent {
            StepUpTheme(ThemeMode.DARK) { ExperienceProvider {
                RunPermissionPrimer(
                    items = listOf(
                        RunPermissionItem(Icons.Filled.DirectionsRun, R.string.run_perm_activity,
                            R.string.run_perm_activity_body, R.string.run_perm_required, granted = true),
                        RunPermissionItem(Icons.Filled.LocationOn, R.string.run_perm_location,
                            R.string.run_perm_location_body, R.string.run_perm_recommended, granted = false),
                        RunPermissionItem(Icons.Filled.Notifications, R.string.run_perm_notification,
                            R.string.run_perm_notification_body, R.string.run_perm_optional, granted = false),
                    ),
                    onAllow = { allowed++ }, onLater = { later++ },
                )
            } }
        }
        compose.onNodeWithText(compose.activity.getString(R.string.run_perm_location)).assertIsDisplayed()
        compose.onNodeWithText("1/3").assertIsDisplayed()
        capture("run-permission-primer.png")
        compose.onNodeWithTag("run-permission-allow").performClick()
        compose.onNodeWithTag("run-permission-later").performClick()
        assertEquals(1, allowed)
        assertEquals(1, later)
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val directory = File(compose.activity.getExternalFilesDir(null), "form-checks").apply { mkdirs() }
        captureDisplay(File(directory, name))
    }
}
