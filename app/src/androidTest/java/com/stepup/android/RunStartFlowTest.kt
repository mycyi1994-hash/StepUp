package com.stepup.android

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.domain.RunPermissionSheet
import com.stepup.android.ui.screens.walk.RunCountdown
import com.stepup.android.ui.screens.walk.RunPermissionSheetView
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * S2 러닝 시작 전 화면 — 권한 안내(시작·로그인·첫 사용 v1 시안 13~19)와 3-2-1(시안 24).
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
        compose.onNodeWithTag("run-countdown-cancel").performClick()
        compose.mainClock.advanceTimeBy(5_000)
        assertEquals(0, starts)
    }

    /** 멈춘 시계로는 화면이 그려지지 않아 캡처가 비었다 — 캡처는 보통 시계로 따로 찍는다 */
    @Test fun countdownCapture() = countdownCapture(located = true, name = "run-countdown.png")

    // 한 화면에서 값만 바꿔 다시 찍으면 같은 장면이 두 번 찍혔다 — 장면마다 새로 그린다
    @Test fun countdownNoGpsCapture() = countdownCapture(located = false, name = "run-countdown-no-gps.png")

    private fun countdownCapture(located: Boolean, name: String) {
        compose.setContent {
            StepUpTheme(ThemeMode.DARK) { ExperienceProvider {
                RunCountdown(courseName = if (located) null else "Hangang 5K", locationAllowed = located, onGo = {}, onCancel = {})
            } }
        }
        compose.onNodeWithTag("run-countdown-cancel").assertIsDisplayed()
        val gps = compose.activity.getString(
            if (located) R.string.run_countdown_gps_on else R.string.run_countdown_gps_off,
        )
        compose.onNodeWithText(gps).assertIsDisplayed()
        capture(name)
    }

    /**
     * 러닝 권한 안내(시작·로그인·첫 사용 v1 시안 13~19) — 장면마다 주 버튼 · 보조 버튼 · 닫기가 각각 한 번씩만 불린다.
     * 시스템 권한 창을 띄우지 않도록 안내 부품만 그린다(차례 · 건너뛰기 판단은 단위 테스트 OnboardingRulesTest).
     */
    @Test fun permissionSheetsHandOffEachChoiceOnce() {
        var sheet by mutableStateOf(RunPermissionSheet.ActivityRationale)
        var primary = 0
        var secondary = 0
        var dismissed = 0
        compose.setContent {
            StepUpTheme(ThemeMode.DARK) { ExperienceProvider {
                androidx.compose.runtime.key(sheet) {
                    RunPermissionSheetView(sheet, onPrimary = { primary++ }, onSecondary = { secondary++ }, onDismiss = { dismissed++ })
                }
            } }
        }
        val tags = mapOf(
            RunPermissionSheet.ActivityRationale to "perm-sheet-activity",
            RunPermissionSheet.ActivityDenied to "perm-sheet-activity-denied",
            RunPermissionSheet.ActivitySettings to "perm-sheet-activity-settings",
            RunPermissionSheet.LocationRationale to "perm-sheet-location",
            RunPermissionSheet.ApproximateLocation to "perm-sheet-location-approximate",
            RunPermissionSheet.WithoutLocation to "perm-sheet-location-off",
            RunPermissionSheet.NotificationRationale to "perm-sheet-notification",
        )
        RunPermissionSheet.entries.forEachIndexed { index, next ->
            compose.runOnIdle { sheet = next }
            compose.waitUntil(5_000) { compose.onAllNodesWithTag(tags.getValue(next)).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("perm-primary").assertIsDisplayed().performClick()
            compose.onNodeWithTag("perm-secondary").assertIsDisplayed().performClick()
            // 닫기 — 시트의 X · 화면의 뒤로(꼬리표 같음). 메뉴 위 가운데 창(P01)은 닫기 단추 없이 뒤로 가기로
            if (compose.onAllNodesWithTag("onboarding-sheet-close").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithTag("onboarding-sheet-close").performClick()
            } else {
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                    .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
                compose.waitForIdle()
            }
            compose.runOnIdle {
                assertEquals(index + 1, primary)
                assertEquals(index + 1, secondary)
                assertEquals(index + 1, dismissed)
            }
        }
    }

    @Test fun togetherRankingOrdersSharedDistanceAndHidesTheRest() {
        val members = listOf(
            com.stepup.android.data.repo.PartyMember("a", "Sora", ready = true, isMe = false, km = 3.80, sharing = true),
            com.stepup.android.data.repo.PartyMember("b", "Me", ready = true, isMe = true, sharing = true),
            com.stepup.android.data.repo.PartyMember("c", "Yeonsu", ready = true, isMe = false),
        )
        compose.setContent {
            StepUpTheme(ThemeMode.DARK) { ExperienceProvider {
                // 앱에서는 러닝 화면의 어두운 바탕 위에 있다 — 캡처도 같은 바탕 위에서
                androidx.compose.foundation.layout.Box(
                    androidx.compose.ui.Modifier.fillMaxSize()
                        // 앱에서는 러닝 화면 안쪽이라 상태 표시줄과 겹치지 않는다 — 캡처도 같은 여백으로
                        .background(com.stepup.android.ui.theme.Night)
                        .then(androidx.compose.ui.Modifier.systemBarsPadding()).padding(16.dp),
                ) {
                    com.stepup.android.ui.screens.walk.TogetherRanking(members, myKm = 3.62)
                }
            } }
        }
        compose.onNodeWithText("3.80 km").assertIsDisplayed()
        compose.onNodeWithText("3.62 km").assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.run_together_hidden)).assertIsDisplayed()
        val sora = compose.onNodeWithText("Sora").fetchSemanticsNode().boundsInRoot.top
        val me = compose.onNodeWithText(compose.activity.getString(R.string.run_together_me)).fetchSemanticsNode().boundsInRoot.top
        val hidden = compose.onNodeWithText("Yeonsu").fetchSemanticsNode().boundsInRoot.top
        org.junit.Assert.assertTrue("shared distances first, longest on top; hidden last", sora < me && me < hidden)
        capture("run-together-ranking.png")
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val directory = File(compose.activity.getExternalFilesDir(null), "form-checks").apply { mkdirs() }
        captureDisplay(File(directory, name))
    }
}
