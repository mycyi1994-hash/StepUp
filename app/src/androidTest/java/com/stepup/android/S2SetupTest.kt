package com.stepup.android

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.BodyProfile
import com.stepup.android.domain.RunMode
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.setup.S2SetupFlow
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** S2 첫 설정(키 · 몸무게 → 목표 → 모드). 저장한 값은 이 기기에만 있고, 검사가 끝나면 지운다. */
@RunWith(AndroidJUnit4::class)
class S2SetupTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @After fun reset() = runBlocking {
        ServiceLocator.userPrefs.setBodyProfile(BodyProfile())
        ServiceLocator.userPrefs.setRunMode(RunMode.LITE)
    }

    @Test fun setupSavesBodyGoalAndMode() {
        runBlocking {
            ServiceLocator.userPrefs.setBodyProfile(BodyProfile())
            ServiceLocator.userPrefs.setRunMode(RunMode.LITE)
            ServiceLocator.userPrefs.setReducedMotion(true)
        }
        var done = false
        compose.setContent { StepUpTheme(ThemeMode.DARK) { ExperienceProvider { S2SetupFlow(onDone = { done = true }) } } }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("setup-height").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("setup-height-value", useUnmergedTree = true).assertTextEquals("165")
        compose.onNodeWithTag("setup-height-minus").performClick()
        compose.onNodeWithTag("setup-height-minus").performClick()
        compose.onNodeWithTag("setup-height-minus").performClick()
        compose.onNodeWithTag("setup-weight-minus").performClick()
        compose.onNodeWithTag("setup-weight-minus").performClick()
        compose.onNodeWithTag("setup-height-value", useUnmergedTree = true).assertTextEquals("162")
        compose.onNodeWithTag("setup-weight-value", useUnmergedTree = true).assertTextEquals("59.0")
        compose.onNodeWithTag("setup-bmi-scale").assertIsDisplayed()
        capture("setup-1-body.png")

        compose.onNodeWithTag("setup-primary").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("setup-goal").fetchSemanticsNodes().isNotEmpty() }
        repeat(13) { compose.onNodeWithTag("setup-goal-minus").performClick() }
        compose.onNodeWithTag("setup-goal-value", useUnmergedTree = true).assertTextEquals("52.5")
        compose.onNodeWithTag("setup-weeks-12").assertIsSelected()
        compose.onNodeWithTag("setup-goal-caution").assertDoesNotExist()
        capture("setup-2-goal.png")
        compose.onNodeWithTag("setup-weeks-8").performClick()
        repeat(4) { compose.onNodeWithTag("setup-goal-minus").performClick() }
        compose.onNodeWithTag("setup-goal-caution").assertIsDisplayed()
        repeat(4) { compose.onNodeWithTag("setup-goal-plus").performClick() }
        compose.onNodeWithTag("setup-weeks-12").performClick()

        compose.onNodeWithTag("setup-primary").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("mode-runner").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("mode-lite").assertIsSelected()
        compose.onNodeWithTag("mode-runner").performClick().assertIsSelected()
        capture("setup-3-mode.png")
        compose.onNodeWithTag("setup-primary").performClick()
        compose.waitUntil(5_000) { done }

        val saved = runBlocking { ServiceLocator.userPrefs.bodyProfile.first() }
        assertEquals(BodyProfile(heightCm = 162, weightKg = 59.0, goalWeightKg = 52.5, goalWeeks = 12), saved)
        assertEquals(RunMode.RUNNER, runBlocking { ServiceLocator.userPrefs.runMode.first() })
        assertTrue(runBlocking { ServiceLocator.userPrefs.s2SetupSeen.first() })
    }

    @Test fun skippingEveryStepSavesNothing() {
        runBlocking {
            ServiceLocator.userPrefs.setBodyProfile(BodyProfile())
            ServiceLocator.userPrefs.setRunMode(RunMode.LITE)
        }
        var done = false
        compose.setContent { StepUpTheme(ThemeMode.DARK) { ExperienceProvider { S2SetupFlow(onDone = { done = true }) } } }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("setup-secondary").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("setup-height-plus").performClick()
        compose.onNodeWithTag("setup-secondary").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("mode-runner").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("mode-runner").performClick()
        compose.onNodeWithTag("setup-secondary").performClick()
        compose.waitUntil(5_000) { done }
        assertEquals(BodyProfile(), runBlocking { ServiceLocator.userPrefs.bodyProfile.first() })
        assertEquals(RunMode.LITE, runBlocking { ServiceLocator.userPrefs.runMode.first() })
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val directory = File(compose.activity.getExternalFilesDir(null), "form-checks").apply { mkdirs() }
        captureDisplay(File(directory, name))
    }
}
