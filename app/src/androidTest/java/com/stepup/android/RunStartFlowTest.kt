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
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
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
 * 자유 러닝(2026-10-14 v2)은 3-2-1 과 권한 안내를 다른 모습(지도 3-2-1 · 짧은 카드)으로 그린다 — 세기 · 시작 · 버튼의 뜻은 같다.
 */
@RunWith(AndroidJUnit4::class)
class RunStartFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    /** 권한 안내 장면마다의 꼬리표 — 예전 안내와 자유 러닝의 짧은 카드가 같은 꼬리표를 쓴다 */
    private val permissionSheetTags = mapOf(
        RunPermissionSheet.ActivityRationale to "perm-sheet-activity",
        RunPermissionSheet.ActivityDenied to "perm-sheet-activity-denied",
        RunPermissionSheet.ActivitySettings to "perm-sheet-activity-settings",
        RunPermissionSheet.LocationRationale to "perm-sheet-location",
        RunPermissionSheet.ApproximateLocation to "perm-sheet-location-approximate",
        RunPermissionSheet.WithoutLocation to "perm-sheet-location-off",
        RunPermissionSheet.NotificationRationale to "perm-sheet-notification",
    )

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
        val tags = permissionSheetTags
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

    /**
     * 자유 러닝의 짧은 권한 카드(minimal) — 꼬리표는 예전 안내와 같고, 각 버튼이 부르는 행동도 그대로다.
     * 위치 권한 없음만 보이는 차례가 바뀐다: 앞 버튼(perm-primary)이 "경로 없이 시작"(시간만 기록 = onSecondary),
     * 옆 버튼(perm-secondary)이 "위치 설정"(설정 열기 = onPrimary). 닫기(뒤로)는 장면마다 onDismiss 한 번.
     */
    @Test fun minimalPermissionCardsKeepEachMeaning() {
        var sheet by mutableStateOf(RunPermissionSheet.ActivityRationale)
        var primary = 0
        var secondary = 0
        var dismissed = 0
        compose.setContent {
            StepUpTheme(ThemeMode.DARK) { ExperienceProvider {
                androidx.compose.runtime.key(sheet) {
                    RunPermissionSheetView(
                        sheet, onPrimary = { primary++ }, onSecondary = { secondary++ }, onDismiss = { dismissed++ },
                        minimal = true,
                    )
                }
            } }
        }
        RunPermissionSheet.entries.forEachIndexed { index, next ->
            compose.runOnIdle { sheet = next }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithTag(permissionSheetTags.getValue(next)).fetchSemanticsNodes().isNotEmpty()
            }
            val swapped = next == RunPermissionSheet.WithoutLocation
            if (swapped) {
                compose.onNodeWithTag("perm-primary").assertTextEquals(compose.activity.getString(R.string.run_free_pm_no_route))
                compose.onNodeWithTag("perm-secondary").assertTextEquals(compose.activity.getString(R.string.run_pm_location_settings))
            }
            val primaryBefore = primary
            val secondaryBefore = secondary
            compose.onNodeWithTag("perm-primary").assertIsDisplayed().performClick()
            compose.runOnIdle {
                // 보이는 앞 버튼 — 위치 권한 없음이면 시간만 기록(onSecondary), 그 밖에는 주 행동(onPrimary)
                assertEquals("$next primary", primaryBefore + if (swapped) 0 else 1, primary)
                assertEquals("$next primary", secondaryBefore + if (swapped) 1 else 0, secondary)
            }
            compose.onNodeWithTag("perm-secondary").assertIsDisplayed().performClick()
            compose.runOnIdle {
                // 두 버튼을 다 누르면 두 행동이 한 번씩 — 어느 쪽도 두 번 불리지 않는다
                assertEquals("$next secondary", primaryBefore + 1, primary)
                assertEquals("$next secondary", secondaryBefore + 1, secondary)
            }
            // 짧은 카드는 닫기 단추가 없다 — 뒤로 가기로 닫는다
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            compose.waitForIdle()
            compose.runOnIdle { assertEquals("$next dismiss", index + 1, dismissed) }
        }
    }

    /**
     * 자유 러닝 3-2-1(지도 · 고리 숫자 · 바로 시작 | 취소) — 바로 시작을 두 번 누르고 화면을 또 눌러도, 세기가 끝나도 시작은 한 번뿐이다.
     * 위치 권한이 없으면 지도를 그리지 않는다(지금 자리를 꾸미지 않는다).
     */
    @Test fun freeCountdownStartsOnce() {
        var starts = 0
        var cancels = 0
        compose.mainClock.autoAdvance = false
        compose.setContent {
            StepUpTheme(ThemeMode.DARK) { ExperienceProvider {
                RunCountdown(
                    courseName = null, locationAllowed = false, onGo = { starts++ }, onCancel = { cancels++ },
                    locationServicesOn = false, title = "", freeRun = true,
                )
            } }
        }
        compose.mainClock.advanceTimeByFrame()
        // 바탕 전체가 누를 수 있는 한 덩어리로 묶여 있어 안쪽 숫자 · 안내 · 지도는 묶이기 전 노드에서 찾는다
        compose.onNodeWithTag("run-countdown-digit", useUnmergedTree = true).assertTextEquals("3")
        // 안내 줄은 줄(Row)에 꼬리표가 있고 글은 그 안에 있다
        val gpsOff = compose.activity.getString(R.string.run_countdown_gps_off)
        compose.onNode(hasTestTag("run-countdown-caption") and hasAnyDescendant(hasText(gpsOff)), useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithTag("run-countdown-map", useUnmergedTree = true).assertDoesNotExist()
        assertEquals("누르기 전에는 시작하지 않는다", 0, starts)

        compose.onNodeWithTag("run-countdown-go").assertIsDisplayed().performClick()
        compose.mainClock.advanceTimeByFrame()
        assertEquals("바로 시작은 바로 한 번 시작한다", 1, starts)
        compose.onNodeWithTag("run-countdown-go").performClick()
        compose.onNodeWithTag("run-countdown").performClick()
        compose.mainClock.advanceTimeBy(5_000)
        assertEquals("다시 눌러도 · 화면을 눌러도 · 세기가 끝나도 더 시작하지 않는다", 1, starts)
        assertEquals(0, cancels)
    }

    /** 자유 러닝 3-2-1 의 취소 — 취소는 한 번, 시작은 한 번도 없다(앱처럼 취소하면 3-2-1 을 치운다) */
    @Test fun freeCountdownCancelNeverStarts() {
        var starts = 0
        var cancels = 0
        var shown by mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            StepUpTheme(ThemeMode.DARK) { ExperienceProvider {
                if (shown) {
                    RunCountdown(
                        courseName = null, locationAllowed = false, onGo = { starts++ },
                        onCancel = { cancels++; shown = false }, locationServicesOn = false, title = "", freeRun = true,
                    )
                }
            } }
        }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("run-countdown-cancel").assertIsDisplayed().performClick()
        compose.mainClock.advanceTimeBy(5_000)
        compose.onNodeWithTag("run-countdown").assertDoesNotExist()
        assertEquals(1, cancels)
        assertEquals("취소한 3-2-1 은 시작하지 않는다", 0, starts)
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
