package com.stepup.android

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.stepup.android.ui.screens.login.LoginContent
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Presentation and input gating only; this does not authenticate a Google account. */
class LoginPresentationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun compactAndLargeTextAllowLoginAndRetryWithReachableLegalLinks() {
        var font by mutableFloatStateOf(1f)
        var busy by mutableStateOf(false)
        var error by mutableStateOf<Int?>(null)
        var clicks = 0
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1.8f, font)) {
                StepUpTheme(ThemeMode.DARK) {
                    Box(Modifier.requiredSize(360.dp, 640.dp).testTag("login-viewport")) {
                        key(font) { LoginContent(busy, error) { clicks++; busy = true; error = null } }
                    }
                }
            }
        }
        for (scale in listOf(1f, 1.3f, 2f)) {
            compose.runOnIdle { font = scale; busy = false; error = null }
            val before = clicks
            compose.onNodeWithTag("login-google").performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
            compose.onNodeWithTag("login-google").assertIsNotEnabled()
            compose.runOnIdle { assertEquals(before + 1, clicks) }
            capture("$scale-busy")
            compose.runOnIdle { busy = false; error = R.string.login_offline }
            compose.onNodeWithTag("login-error").performScrollTo().assertIsDisplayed()
            capture("$scale-error")
            compose.onNodeWithTag("login-google").performScrollTo().assertIsEnabled().performClick()
            compose.onNodeWithTag("login-error").assertDoesNotExist()
            compose.runOnIdle { assertEquals(before + 2, clicks); busy = false }
            for (tag in listOf("login-terms", "login-privacy")) {
                compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed().assertHasClickAction()
            }
            capture("$scale-legal")
        }
    }

    private fun capture(name: String) {
        val directory = java.io.File(compose.activity.getExternalFilesDir(null), "login-checks").apply { mkdirs() }
        val bitmap = compose.onNodeWithTag("login-viewport").captureToImage().asAndroidBitmap()
        java.io.File(directory, "$name.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
