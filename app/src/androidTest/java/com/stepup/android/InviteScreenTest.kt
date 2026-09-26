package com.stepup.android

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stepup.android.data.remote.InviteStatusRow
import com.stepup.android.data.remote.InviteeRow
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.invite.InviteContent
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * S2 친구 초대 화면(시안 28). 서버 값 모양만 흉내 내 그린다 — 이름 · 코드는 이 검사용이다.
 * 적립액이 0이면 보상 문구가 없어야 하고, 정해지면 그 금액만 보인다.
 */
@RunWith(AndroidJUnit4::class)
class InviteScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun rewardWordingFollowsTheServerAmount() {
        var reward by mutableStateOf(0.0)
        val invitees = listOf(
            InviteeRow("Haeun", "2026-09-26T01:00:00+00:00", "2026-09-26T03:00:00+00:00"),
            InviteeRow("Yeonsu", "2026-09-25T01:00:00+00:00", null),
        )
        compose.setContent {
            StepUpTheme(ThemeMode.DARK) { ExperienceProvider {
                InviteContent(
                    InviteStatusRow(code = "STEP-AB23CD", rewardSup = reward, invited = 2, rewarded = 1, canRedeem = true),
                    invitees, modifier = Modifier.fillMaxSize(),
                )
            } }
        }
        compose.onNodeWithText("STEP-AB23CD").assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.invite_headline)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.invite_per_friend)).assertDoesNotExist()
        compose.onNodeWithTag("invite-redeem").assertIsDisplayed()
        capture("invite-no-reward.png")
        compose.runOnIdle { reward = 5.0 }
        compose.onNodeWithText(compose.activity.getString(R.string.invite_headline_reward, "5")).assertIsDisplayed()
        compose.onNodeWithText("5 SUP").assertIsDisplayed()
        capture("invite-reward.png")
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val directory = File(compose.activity.getExternalFilesDir(null), "form-checks").apply { mkdirs() }
        captureDisplay(File(directory, name))
    }
}
