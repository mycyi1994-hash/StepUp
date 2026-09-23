package com.stepup.android

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.stepup.android.core.ServiceLocator
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Routes
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

/** Exercises the production form without submitting a real crew to the server. */
class CrewFormTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun nameInputAndKeyboardLeaveSubmitReachableAtLargeFont() {
        runBlocking {
            ServiceLocator.userPrefs.setReducedMotion(true)
            ServiceLocator.userPrefs.setGuideSeen()
        }
        var scale by mutableFloatStateOf(1f)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1.8f, scale)) {
                StepUpTheme(ThemeMode.DARK) {
                    ExperienceProvider {
                        Box(Modifier.requiredSize(360.dp, 780.dp).testTag("crew-form-viewport")) {
                            com.stepup.android.ui.components.NightCanvas(Modifier.fillMaxSize())
                            key(scale) { MainScaffold(initialRoute = Routes.CREW_CREATE) }
                        }
                    }
                }
            }
        }
        for (font in listOf(1f, 1.3f)) {
            compose.runOnIdle { scale = font }
            compose.onNodeWithTag("crew-create-submit").assertIsDisplayed().assertIsNotEnabled()
            val name = compose.onNodeWithContentDescription(compose.activity.getString(R.string.crew_field_name))
            name.performScrollTo().performClick().performTextInput("River runners")
            compose.waitUntil(timeoutMillis = 5_000) {
                androidx.core.view.ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                    ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == true
            }
            compose.onNodeWithTag("crew-create-submit").assertIsDisplayed().assertIsEnabled()
            val area = compose.onNodeWithContentDescription(compose.activity.getString(R.string.crew_field_area))
            area.performScrollTo().performClick().performTextInput("Seoul")
            compose.onNodeWithTag("crew-create-submit").assertIsDisplayed()
            compose.onNodeWithTag("main-header").assertDoesNotExist()
            capture("$font-input")
            name.performScrollTo().performTextClearance()
            compose.onNodeWithTag("crew-create-submit").assertIsNotEnabled()
        }
    }

    private fun capture(name: String) {
        val directory = java.io.File(compose.activity.getExternalFilesDir(null), "form-checks").apply { mkdirs() }
        val bitmap = compose.onNodeWithTag("crew-form-viewport").captureToImage().asAndroidBitmap()
        java.io.File(directory, "$name.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
