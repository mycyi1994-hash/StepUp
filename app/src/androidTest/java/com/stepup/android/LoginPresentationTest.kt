package com.stepup.android

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.stepup.android.ui.screens.login.LoginContent
import com.stepup.android.ui.screens.login.LoginNotice
import com.stepup.android.ui.screens.login.LoginPhase
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
        var phase by mutableStateOf(LoginPhase.Idle)
        var notice by mutableStateOf<LoginNotice?>(null)
        var clicks = 0
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1.8f, font)) {
                StepUpTheme(ThemeMode.DARK) {
                    Box(Modifier.requiredSize(360.dp, 640.dp).testTag("login-viewport")) {
                        // 누르면 계정 선택 창이 뜬 것처럼 버튼을 잠근다(실제 LoginScreen 과 같은 순서)
                        key(font) { LoginContent(phase, notice, onSignIn = { clicks++; phase = LoginPhase.Picking; notice = null }) }
                    }
                }
            }
        }
        for (scale in listOf(1f, 1.3f, 2f)) {
            compose.runOnIdle { font = scale; phase = LoginPhase.Idle; notice = null }
            val before = clicks
            compose.onNodeWithTag("login-google").performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
            compose.onNodeWithTag("login-google").assertIsNotEnabled()
            compose.runOnIdle { assertEquals(before + 1, clicks) }
            // 계정을 고른 뒤 서버 확인 중(05) — 버튼은 잠긴 채 "로그인 중", 확인 중 안내를 읽는다
            compose.runOnIdle { phase = LoginPhase.Verifying }
            compose.onNodeWithTag("login-verifying").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("login-google").assertIsNotEnabled()
            capture("$scale-busy")
            compose.runOnIdle { phase = LoginPhase.Idle; notice = LoginNotice.Offline }
            compose.onNodeWithTag("login-notice-offline").performScrollTo().assertIsDisplayed()
            capture("$scale-error")
            compose.onNodeWithTag("login-google").performScrollTo().assertIsEnabled().performClick()
            compose.onNodeWithTag("login-notice-offline").assertDoesNotExist()
            compose.runOnIdle { assertEquals(before + 2, clicks); phase = LoginPhase.Idle }
            for (tag in listOf("login-terms", "login-privacy")) {
                compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed().assertHasClickAction()
            }
            capture("$scale-legal")
        }
    }

    private fun capture(name: String) {
        val directory = java.io.File(compose.activity.getExternalFilesDir(null), "login-checks").apply { mkdirs() }
        captureDisplay(java.io.File(directory, "$name.png"))
    }
}
