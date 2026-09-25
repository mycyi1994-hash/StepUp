package com.stepup.android

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.text.AnnotatedString
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

    @Test fun meetupFormKeepsInvalidNumbersAndRequiresCorrectionBeforeSubmission() {
        runBlocking {
            ServiceLocator.userPrefs.setReducedMotion(true)
            ServiceLocator.userPrefs.setGuideSeen()
        }
        compose.setContent {
            StepUpTheme(ThemeMode.DARK) {
                ExperienceProvider { MainScaffold(initialRoute = Routes.postCompose("")) }
            }
        }
        fun fill(label: Int, value: String) {
            compose.onNodeWithContentDescription(compose.activity.getString(label))
                .performScrollTo().performTextReplacement(value)
        }
        compose.onNodeWithText(compose.activity.getString(R.string.post_cat_flash)).performClick()
        fill(R.string.post_field_title, "Riverside run")
        fill(R.string.post_field_place, "Bridge entrance")
        compose.onNodeWithTag("post-submit").assertIsEnabled()
        fill(R.string.post_field_capacity, "201")
        compose.onNodeWithTag("post-submit").assertIsNotEnabled()
        fill(R.string.post_field_capacity, "2")
        fill(R.string.post_field_starts_in, "")
        compose.onNodeWithTag("post-submit").assertIsNotEnabled()
        fill(R.string.post_field_starts_in, "30")
        fill(R.string.post_field_distance, "1..2")
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.post_field_distance))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("1..2")))
        compose.onNodeWithTag("post-submit").assertIsNotEnabled()
        fill(R.string.post_field_distance, "NaN")
        compose.onNodeWithTag("post-submit").assertIsNotEnabled()
        fill(R.string.post_field_distance, "1,5")
        compose.onNodeWithTag("post-submit").assertIsEnabled()
        // Deliberately do not publish a real meetup from instrumentation.
    }

    @Test fun nameInputAndKeyboardLeaveSubmitReachable() = exerciseCrewForm(1f)

    @Test fun nameInputAndKeyboardLeaveSubmitReachableAtLargeFont() = exerciseCrewForm(1.3f)

    private fun exerciseCrewForm(font: Float) {
        runBlocking {
            ServiceLocator.userPrefs.setReducedMotion(true)
            ServiceLocator.userPrefs.setGuideSeen()
        }
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1.8f, font)) {
                StepUpTheme(ThemeMode.DARK) {
                    ExperienceProvider {
                        Box(Modifier.requiredSize(360.dp, 780.dp).testTag("crew-form-viewport")) {
                            com.stepup.android.ui.components.NightCanvas(Modifier.fillMaxSize())
                            MainScaffold(initialRoute = Routes.CREW_CREATE)
                        }
                    }
                }
            }
        }
        // Each font size gets a fresh rule-owned Activity. Replacing a focused
        // editor in-place races its pending IME hide against the next editor's show.
        run {
            compose.onNodeWithTag("crew-create-submit").assertIsDisplayed().assertIsNotEnabled()
            val name = compose.onNodeWithContentDescription(compose.activity.getString(R.string.crew_field_name))
            name.performScrollTo().performClick().performTextInput("River runners")
            try {
                compose.waitUntil(timeoutMillis = 5_000) {
                    androidx.core.view.ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                        ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == true
                }
            } finally {
                // Preserve the focused form before JUnit tears down its activity,
                // including a missing keyboard; a post-test adb screenshot is too late.
                capture("$font-keyboard-wait")
            }
            name.assertIsDisplayed()
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
        captureDisplay(java.io.File(directory, "$name.png"))
    }
}
