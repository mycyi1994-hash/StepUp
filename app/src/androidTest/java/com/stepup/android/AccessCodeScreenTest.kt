package com.stepup.android

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stepup.android.data.remote.AccessRedeemRow
import com.stepup.android.data.remote.AccessStatusRow
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.access.AccessCodeScreen
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 접속 코드 입력(서버 0079) — 틀린 코드는 까닭을 보이고, 맞으면 앱으로 들어간다 */
@RunWith(AndroidJUnit4::class)
class AccessCodeScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun wrongCodeShowsWhyAndRightCodeLetsIn() {
        var granted = 0
        val tried = mutableListOf<String>()
        compose.setContent {
            StepUpTheme(ThemeMode.LIGHT) { ExperienceProvider {
                AccessCodeScreen(
                    status = AccessStatusRow(mode = "CLOSED_BETA", granted = false),
                    signedIn = true,
                    onGranted = { granted++ },
                    onSignOut = {},
                    redeem = { code ->
                        tried += code
                        ServerResult.Ok(if (code == "GOOD-CODE") AccessRedeemRow(ok = true) else AccessRedeemRow(error = "expired"))
                    },
                )
            } }
        }
        val activity = compose.activity
        compose.onNodeWithText(activity.getString(R.string.access_title_closed_beta)).assertIsDisplayed()
        compose.onNodeWithTag("access-enter").assertIsNotEnabled()
        capture("access-gate-empty.png")

        compose.onNode(hasSetTextAction()).performTextInput("OLD-CODE")
        compose.onNodeWithTag("access-enter").assertIsEnabled().performClick()
        compose.onNodeWithText(activity.getString(R.string.access_error_expired)).assertIsDisplayed()
        capture("access-gate-error.png")
        compose.runOnIdle { assertEquals(0, granted) }
    }

    @Test fun signedOutVisitorIsAskedToSignInFirst() {
        var signOut = 0
        compose.setContent {
            StepUpTheme(ThemeMode.DARK) { ExperienceProvider {
                AccessCodeScreen(
                    status = AccessStatusRow(mode = "OPEN_BETA", granted = false),
                    signedIn = false, onGranted = {}, onSignOut = { signOut++ },
                )
            } }
        }
        val activity = compose.activity
        compose.onNodeWithText(activity.getString(R.string.access_title_open_beta)).assertIsDisplayed()
        compose.onNodeWithText(activity.getString(R.string.access_body_sign_in)).assertIsDisplayed()
        compose.onNodeWithText(activity.getString(R.string.access_sign_in)).performClick()
        compose.runOnIdle { assertEquals(1, signOut) }
        capture("access-gate-signed-out.png")
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val directory = File(compose.activity.getExternalFilesDir(null), "form-checks").apply { mkdirs() }
        captureDisplay(File(directory, name))
    }
}
