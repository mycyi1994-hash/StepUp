package com.stepup.android

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.domain.RunPermissionSheet
import com.stepup.android.domain.Faction
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.ServerStats
import com.stepup.android.domain.ShoeTier
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.tier
import com.stepup.android.ui.screens.walk.FreeRunShoeCheckContent
import com.stepup.android.ui.screens.walk.FreeRunStartSequence
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
 * 자유 러닝(2026-10-14 v2)은 권한 안내를 짧은 카드로 그린다 — 차례 · 버튼의 뜻은 같다. 자유러닝의 3-2-1 은 신발 점검 →
 * 3·2·1·0 → StepUP! → 지도 펼침(2026-10-17 전달본, FreeRunStartSequence)이다.
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
    // 3-2-1 은 GPS 를 잡은 뒤에만 온다(2026-10-16). 위치를 모르는 3-2-1 은 위치 안내 줄을 꾸미지 않는다(시간만 기록 안내 없음)
    @Test fun countdownNoGpsCapture() = countdownCapture(located = false, name = "run-countdown-no-gps.png")

    private fun countdownCapture(located: Boolean, name: String) {
        compose.setContent {
            StepUpTheme(ThemeMode.DARK) { ExperienceProvider {
                RunCountdown(courseName = if (located) null else "Hangang 5K", locationAllowed = located, onGo = {}, onCancel = {})
            } }
        }
        compose.onNodeWithTag("run-countdown-cancel").assertIsDisplayed()
        if (located) {
            compose.onNodeWithText(compose.activity.getString(R.string.run_countdown_gps_on)).assertIsDisplayed()
        } else {
            compose.onNodeWithTag("run-countdown-caption", useUnmergedTree = true).assertDoesNotExist()
        }
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
     * 위치 장면(허용 · 대략적 위치 · 권한 없음)의 보조 버튼은 "취소"다 — 위치 없이 시작하는 길은 없다(2026-10-16).
     * 닫기(뒤로)는 장면마다 onDismiss 한 번.
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
            val location = next == RunPermissionSheet.LocationRationale || next == RunPermissionSheet.ApproximateLocation ||
                next == RunPermissionSheet.WithoutLocation
            if (location) {
                compose.onNodeWithTag("perm-secondary").assertTextEquals(compose.activity.getString(R.string.common_cancel))
            }
            val primaryBefore = primary
            val secondaryBefore = secondary
            compose.onNodeWithTag("perm-primary").assertIsDisplayed().performClick()
            compose.runOnIdle {
                // 보이는 앞 버튼은 언제나 주 행동(onPrimary)
                assertEquals("$next primary", primaryBefore + 1, primary)
                assertEquals("$next primary", secondaryBefore, secondary)
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
     * 자유러닝 시작(2026-10-17 전달본) — 3·2·1·0 → StepUP! → 지도 펼침. 러닝 기록은 펼침에 들어갈 때(3,760ms) 한 번만 시작하고,
     * 러닝이 실제로 켜진 것을 본 뒤에 펼친다. 프레임이 쌓이고 다시 그려져도 시작은 한 번이다.
     */
    @Test fun freeStartSequenceStartsOnceAtReveal() {
        var starts = 0
        var finished = 0
        var failed = 0
        var active by mutableStateOf(false)
        var shown by mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            StepUpTheme(ThemeMode.DARK) { ExperienceProvider {
                if (shown) {
                    FreeRunStartSequence(
                        sessionActive = active, startRefusals = 0,
                        onStartSession = { starts++; active = true; true },
                        onFinished = { finished++; shown = false },
                        onFailed = { failed++ },
                        onCancel = {},
                    )
                }
            } }
        }
        compose.mainClock.advanceTimeByFrame()
        // 숫자와 원호만 — 제목 · 안내 · 취소 버튼이 없다
        compose.onNodeWithTag("run-countdown-digit", useUnmergedTree = true).assertTextEquals("3")
        compose.onAllNodesWithTag("run-countdown-cancel").assertCountEquals(0)
        compose.onAllNodesWithTag("run-countdown-kicker", useUnmergedTree = true).assertCountEquals(0)
        compose.mainClock.advanceTimeBy(1_050)
        compose.onNodeWithTag("run-countdown-digit", useUnmergedTree = true).assertTextEquals("2")
        compose.mainClock.advanceTimeBy(2_000)
        compose.onNodeWithTag("run-countdown-digit", useUnmergedTree = true).assertTextEquals("0")
        compose.mainClock.advanceTimeBy(600)
        assertEquals("StepUP! 까지는 기록을 시작하지 않는다", 0, starts)
        compose.mainClock.advanceTimeBy(200)
        assertEquals("펼침에 들어가며 한 번 시작한다", 1, starts)
        compose.mainClock.advanceTimeBy(2_000)
        assertEquals("다시 그려져도 시작은 한 번", 1, starts)
        assertEquals(1, finished)
        assertEquals(0, failed)
    }

    /** 기록 전 뒤로 가기 — 시작 취소. 아무것도 시작하지 않는다 */
    @Test fun freeStartBackBeforeRevealNeverStarts() {
        var starts = 0
        var cancels = 0
        var shown by mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            StepUpTheme(ThemeMode.DARK) { ExperienceProvider {
                if (shown) {
                    FreeRunStartSequence(
                        sessionActive = false, startRefusals = 0,
                        onStartSession = { starts++; true }, onFinished = {}, onFailed = {},
                        onCancel = { cancels++; shown = false },
                    )
                }
            } }
        }
        compose.mainClock.advanceTimeBy(2_000)
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.mainClock.advanceTimeBy(5_000)
        compose.onNodeWithTag("run-start-sequence").assertDoesNotExist()
        assertEquals(1, cancels)
        assertEquals("취소한 카운트다운은 시작하지 않는다", 0, starts)
    }

    /** 시작이 거절됐다(끝나지 않은 러닝 · 저장 공간) — 지도를 펼치지 않고 거둔다. 실패한 러닝을 시작한 것처럼 보이지 않는다 */
    @Test fun freeStartRefusedStartDoesNotReveal() {
        var failed = 0
        var finished = 0
        var refusals by mutableIntStateOf(0)
        var shown by mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            StepUpTheme(ThemeMode.DARK) { ExperienceProvider {
                if (shown) {
                    FreeRunStartSequence(
                        sessionActive = false, startRefusals = refusals,
                        onStartSession = { refusals++; true },
                        onFinished = { finished++ }, onFailed = { failed++; shown = false }, onCancel = {},
                    )
                }
            } }
        }
        compose.mainClock.advanceTimeBy(4_500)
        assertEquals(1, failed)
        assertEquals(0, finished)
        compose.onNodeWithTag("run-start-sequence").assertDoesNotExist()
    }

    /** 이미 달리는 러닝으로 돌아왔다 — 카운트다운을 다시 보이지 않고 그 러닝으로 */
    @Test fun freeStartSkipsCountdownForActiveRun() {
        var starts = 0
        var finished = 0
        compose.mainClock.autoAdvance = false
        compose.setContent {
            StepUpTheme(ThemeMode.DARK) { ExperienceProvider {
                FreeRunStartSequence(
                    sessionActive = true, startRefusals = 0,
                    onStartSession = { starts++; true }, onFinished = { finished++ }, onFailed = {}, onCancel = {},
                )
            } }
        }
        compose.mainClock.advanceTimeBy(500)
        assertEquals(1, finished)
        assertEquals(0, starts)
    }

    /** 신발 점검 — 신고 있는 신발의 실제 등급 · 레벨 · 내구도. 출발은 연타해도 한 번, 돌아가기 · 닫기는 시작하지 않는다 */
    @Test fun shoeCheckShowsTheEquippedShoeAndStartsOnce() {
        var starts = 0
        var cancels = 0
        val shoe = checkShoe
        compose.setContent {
            StepUpTheme(ThemeMode.DARK) { ExperienceProvider {
                FreeRunShoeCheckContent(shoe = shoe, onStart = { starts++ }, onCancel = { cancels++ })
            } }
        }
        val grade = compose.activity.getString(
            R.string.run_free_check_grade_level, compose.activity.getString(tierLabel(shoe.tier)), shoe.level,
        )
        compose.onNodeWithTag("run-shoe-check-grade", useUnmergedTree = true).assertTextEquals(grade)
        compose.onNodeWithTag("run-shoe-check-durability-value", useUnmergedTree = true).assertTextEquals("68%")
        compose.onNodeWithTag("run-shoe-check-back").performClick()
        compose.onNodeWithTag("run-shoe-check-close").performClick()
        assertEquals(2, cancels)
        assertEquals("돌아가기 · 닫기는 시작하지 않는다", 0, starts)
        compose.onNodeWithTag("run-shoe-check-go").performClick()
        compose.onNodeWithTag("run-shoe-check-go").performClick()
        assertEquals("출발 연타는 한 번", 1, starts)
    }

    /** 신발을 신지 않았다 — 지금처럼 신발 없이도 달릴 수 있다. 그 사실을 알리고 "러닝 시작"으로 잇는다 */
    @Test fun shoeCheckWithoutShoeSaysSo() {
        var starts = 0
        compose.setContent {
            StepUpTheme(ThemeMode.DARK) { ExperienceProvider {
                FreeRunShoeCheckContent(shoe = null, onStart = { starts++ }, onCancel = {})
            } }
        }
        compose.onNodeWithTag("run-shoe-check-none").assertIsDisplayed()
        compose.onAllNodesWithTag("run-shoe-check-durability").assertCountEquals(0)
        compose.onNodeWithTag("run-shoe-check-go").performClick()
        assertEquals(1, starts)
    }

    private val checkShoe = Sneaker(
        id = 9_301, faction = Faction.WIND, rarity = Rarity.RARE, variant = 0, level = 3, mintNumber = 12,
        luck = 1.0, comfort = 1.0, durability = 68, equipped = true, acquiredAt = 1_790_000_000_000L,
        server = ServerStats(
            origin = "DRAW", efficiencyBps = 800, comfortBps = 580, durabilityPts = 68.0, maxLevel = 15,
            status = "OWNED", chainState = "APP", canWithdraw = true, upgradeCost = 625.0, repairCostPerPoint = 1.5625, genesisNo = 0,
        ),
        modelId = 1201,
    )

    private fun tierLabel(tier: ShoeTier): Int = when (tier) {
        ShoeTier.COMMON -> R.string.shoe_tier_common
        ShoeTier.RARE -> R.string.shoe_tier_rare
        ShoeTier.EPIC -> R.string.shoe_tier_epic
        ShoeTier.LEGENDARY -> R.string.shoe_tier_legendary
        ShoeTier.REDLINE -> R.string.shoe_tier_redline
        ShoeTier.FINISH -> R.string.shoe_tier_finish
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
