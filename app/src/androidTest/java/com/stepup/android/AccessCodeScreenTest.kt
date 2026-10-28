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

/** 접속 코드 입력(서버 0079, 로그인 위 모달 2026-10-29) — 틀린 코드는 까닭을 보이고, 맞으면 앱으로 들어간다 */
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
        capture("access-modal-empty.png")

        compose.onNode(hasSetTextAction()).performTextInput("OLD-CODE")
        compose.onNodeWithTag("access-enter").assertIsEnabled().performClick()
        compose.onNodeWithText(activity.getString(R.string.access_error_expired)).assertIsDisplayed()
        capture("access-modal-error.png")
        compose.runOnIdle { assertEquals(0, granted) }
    }

    /** 핸드오프 2026-10-29 — X 는 모달만 닫고(로그인 화면이 남는다) 앱에 들이지 않는다. 구글 버튼이 다시 연다 */
    @Test fun closingOnlyHidesTheModalAndLoginButtonReopensIt() {
        var granted = 0
        var signOut = 0
        compose.setContent {
            StepUpTheme(ThemeMode.DARK) { ExperienceProvider {
                AccessCodeScreen(
                    status = AccessStatusRow(mode = "CLOSED_BETA", granted = false),
                    signedIn = true,
                    onGranted = { granted++ },
                    onSignOut = { signOut++ },
                    redeem = { ServerResult.Ok(AccessRedeemRow(error = "not_found")) },
                )
            } }
        }
        compose.onNodeWithTag("access-modal").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextInput("BETA-2026")
        capture("access-modal-entered.png")
        compose.onNodeWithTag("access-close").performClick()
        compose.onNodeWithTag("access-modal").assertDoesNotExist()
        compose.onNodeWithTag("login-google").assertIsDisplayed()
        capture("access-modal-closed.png")
        compose.runOnIdle {
            assertEquals(0, granted)
            assertEquals(0, signOut)
        }
        compose.onNodeWithTag("login-google").performClick()
        compose.onNodeWithTag("access-modal").assertIsDisplayed()
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
