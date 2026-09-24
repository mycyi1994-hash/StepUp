package com.stepup.android

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stepup.android.core.ServiceLocator
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.components.HomeBackgrounds
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A small capture of the new draw entry and the home scenery controls. */
@RunWith(AndroidJUnit4::class)
class MysteryDesignTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun drawScreenAndBackgroundSwitch() {
        runBlocking {
            ServiceLocator.userPrefs.setReducedMotion(true)
            ServiceLocator.userPrefs.setSounds(false)
            ServiceLocator.userPrefs.setHaptics(false)
            ServiceLocator.userPrefs.setGuideSeen()
            ServiceLocator.sneakerRepository.ensureStarter()
        }
        compose.activityRule.scenario.onActivity {
            it.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            )
        }
        var largeType by mutableStateOf(false)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (largeType) 1.3f else 1f)) {
                StepUpTheme(ThemeMode.DARK) { ExperienceProvider { MainScaffold() } }
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("home-character-ready").fetchSemanticsNodes().isNotEmpty() }
        val out = File(compose.activity.getExternalFilesDir(null), "screen-gallery").apply { mkdirs() }
        compose.onNodeWithText(compose.activity.getString(R.string.tab_draw)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.mystery_title)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.mystery_draw_shoe)).assertIsNotEnabled()
        compose.onNodeWithText(compose.activity.getString(R.string.mystery_draw_outfit)).assertIsNotEnabled()
        captureDisplay(File(out, "mystery-01-normal.png"))

        compose.runOnIdle { largeType = true }
        compose.waitForIdle()
        captureDisplay(File(out, "mystery-02-large-type.png"))

        compose.onNodeWithText(compose.activity.getString(R.string.tab_run)).performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("home-character-ready").fetchSemanticsNodes().isNotEmpty() }
        val before = HomeBackgrounds.settings.first { scene ->
            compose.onAllNodesWithTag("home-scene-${scene.name}").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("home-background-next").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("home-scene-${before.name}").fetchSemanticsNodes().isEmpty()
        }
        captureDisplay(File(out, "home-01-background-next.png"))
    }
}
