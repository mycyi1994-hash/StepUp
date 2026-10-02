package com.stepup.android

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.rule.GrantPermissionRule
import com.stepup.android.core.ServiceLocator
import com.stepup.android.ui.BOTTOM_NAV_TAG
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Uses actual navigation and production screens, not replicas of the shared components. */
class ChromeNavigationTest {
    @get:Rule(order = 0) val permissions = GrantPermissionRule.grant(
        Manifest.permission.ACTIVITY_RECOGNITION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS,
    )
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()

    private data class Viewport(val width: Int, val height: Int, val font: Float, val mode: ThemeMode)

    @Test fun chromeSurvivesTabChangesWalletAndBack() {
        runBlocking {
            ServiceLocator.userPrefs.setReducedMotion(true)
            ServiceLocator.userPrefs.setSounds(false)
            ServiceLocator.userPrefs.setHaptics(false)
            ServiceLocator.userPrefs.setGuideSeen()
            ServiceLocator.sneakerRepository.ensureStarter()
        }
        var viewport by mutableStateOf(Viewport(360, 780, 1f, ThemeMode.DARK))
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1.8f, viewport.font)) {
                StepUpTheme(viewport.mode) {
                    ExperienceProvider {
                        Box(Modifier.requiredSize(viewport.width.dp, viewport.height.dp).testTag("chrome-viewport")) {
                            com.stepup.android.ui.components.NightCanvas(Modifier.fillMaxSize())
                            key(viewport) { MainScaffold() }
                        }
                    }
                }
            }
        }
        val scenarios = listOf(
            Viewport(360, 780, 1f, ThemeMode.DARK),
            Viewport(430, 900, 1f, ThemeMode.DARK),
            Viewport(360, 780, 1.3f, ThemeMode.DARK),
            Viewport(360, 780, 1.3f, ThemeMode.LIGHT),
        )
        scenarios.forEach { next ->
            compose.runOnIdle { viewport = next }
            compose.waitForIdle()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onNodeWithTag("home-start-run").isDisplayed()
            }
            val header = bounds("main-header")
            val logo = bounds("brand-wordmark")
            val bar = bounds(BOTTOM_NAV_TAG)
            val token = bounds("sup-balance")
            // 하단 탭 다섯 — 러닝 / 신발 / 뽑기 / 커뮤니티 / 내 정보(신발 화면 확정안 2026-09-28)
            val tabRoutes = listOf("home", "customize", "mystery-box", "community", "profile")
            assertEquals("icons stay on one baseline $next", 1,
                tabRoutes.map { bounds("nav-icon-$it").top }.distinct().size)
            assertEquals("labels stay on one baseline $next", 1,
                tabRoutes.map { bounds("nav-label-$it").top }.distinct().size)
            tabRoutes.forEach { route ->
                val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                compose.onNodeWithTag("nav-label-$route", useUnmergedTree = true)
                    .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) {
                        it(layouts)
                    }
                org.junit.Assert.assertTrue("tab has measured text $next/$route", layouts.isNotEmpty())
                org.junit.Assert.assertFalse("tab label must not truncate $next/$route: " +
                    layouts.joinToString { "text=${it.layoutInput.text}, size=${it.size}, paragraph=${it.multiParagraph.width}x${it.multiParagraph.height}, constraints=${it.layoutInput.constraints}" },
                    layouts.any { it.hasVisualOverflow })
            }
            capture("${next.width}-${next.font}-${next.mode}-home")
            compose.onNodeWithTag("home-start-run").assertIsDisplayed().assertHasClickAction()
            listOf(R.string.tab_customize, R.string.tab_draw, R.string.tab_community, R.string.tab_me, R.string.tab_run).forEach { tab ->
                compose.onNode(hasText(compose.activity.getString(tab)) and hasAnyAncestor(hasTestTag(BOTTOM_NAV_TAG)))
                    .performClick()
                compose.waitForIdle()
                assertEquals("header $next/$tab", header, bounds("main-header"))
                assertEquals("logo $next/$tab", logo, bounds("brand-wordmark"))
                assertEquals("navigation $next/$tab", bar, bounds(BOTTOM_NAV_TAG))
                assertEquals("balance $next/$tab", token, bounds("sup-balance"))
                capture("${next.width}-${next.font}-${next.mode}-$tab")
                if (tab == R.string.tab_me) {
                    if (next.font == 1f) {
                        val wallet = bounds("profile-wallet")
                        org.junit.Assert.assertTrue("profile destinations fit above navigation $next", wallet.bottom <= bar.top)
                    } else {
                        compose.onNodeWithTag("profile-wallet").performScrollTo().assertIsDisplayed()
                        compose.onNodeWithTag("profile-settings").performScrollTo()
                    }
                    // 러닝 패스(2026-09-27): 설정은 메뉴의 "설정" 줄이다. 설정 v1(2026-09-28)부터 설정 첫 목록은
                    // 다른 설정 화면처럼 상세 화면(뒤로 · 제목 머리)이고, 아래 탭은 내 정보 그대로다
                    compose.onNodeWithTag("profile-settings").performScrollTo().performClick()
                    compose.waitForIdle()
                    compose.onNodeWithTag("settings-goal").assertIsDisplayed()
                    assertEquals("profile settings keep navigation", bar, bounds(BOTTOM_NAV_TAG))
                    capture("${next.width}-${next.font}-${next.mode}-profile-settings")
                    pressBack()
                    compose.waitForIdle()
                    compose.onNodeWithTag("profile-settings").assertIsDisplayed()
                    assertEquals("settings Back returns to profile", logo, bounds("brand-wordmark"))
                    // 내 정보의 챌린지는 기록(K2) — 거기서 진행 중인 챌린지로 간다
                    compose.onNodeWithTag("profile-challenges").performScrollTo().performClick()
                    compose.waitForIdle()
                    compose.onNodeWithTag("challenge-history-summary").assertIsDisplayed()
                    capture("${next.width}-${next.font}-${next.mode}-challenge-history")
                    compose.onNodeWithTag("detail-primary-action").performClick()
                    compose.waitForIdle()
                    compose.onNodeWithTag("challenge-hero").assertIsDisplayed()
                    compose.onNodeWithTag("challenge-primary-action").assertIsDisplayed().assertHasClickAction()
                    (0..2).forEach { choice ->
                        compose.onNodeWithTag("challenge-choice-$choice").performScrollTo().performClick()
                        compose.waitForIdle()
                        compose.onNodeWithTag("challenge-primary-action").assertIsDisplayed()
                        capture("${next.width}-${next.font}-${next.mode}-challenge-$choice")
                    }
                    pressBack()
                    compose.waitForIdle()
                    pressBack()
                    compose.waitForIdle()
                    assertEquals("challenge returns to profile chrome", bar, bounds(BOTTOM_NAV_TAG))
                }
                if (tab == R.string.tab_community) {
                    // 첫 화면은 러닝 이야기 — 번개 모임 · 내 크루(예전 "함께 뛰기")는 크루 모집 목록 끝 안쪽에 있다
                    compose.openCommunityMeetups()
                    compose.onNodeWithContentDescription(compose.activity.getString(R.string.community_tab_my_crew)).performClick()
                    compose.waitForIdle()
                    assertEquals("crews keep navigation", bar, bounds(BOTTOM_NAV_TAG))
                    pressBack()
                    awaitVisible("community-all-meetups")
                    compose.onNodeWithTag("community-all-meetups").assertIsDisplayed().performClick()
                    compose.waitForIdle()
                    pressBack()
                    compose.waitForIdle()
                    compose.onNodeWithTag("community-all-meetups").assertIsDisplayed()
                    assertEquals("meetups Back restores chrome", header, bounds("main-header"))
                }
            }
            compose.onNodeWithTag("sup-balance").performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("main-header").assertDoesNotExist()
            assertEquals("wallet keeps navigation geometry", bar, bounds(BOTTOM_NAV_TAG))
            pressBack()
            compose.waitForIdle()
            assertEquals("restored header", header, bounds("main-header"))
            assertEquals("restored logo", logo, bounds("brand-wordmark"))
            assertEquals("restored navigation", bar, bounds(BOTTOM_NAV_TAG))
            compose.onNodeWithTag("home-start-run").assertIsDisplayed()
            com.stepup.android.service.WalkSessionService.showStateForTest(
                com.stepup.android.service.WalkSessionState(isActive = true, isPaused = true, elapsedSec = 623, steps = 1200),
            )
            compose.onNodeWithTag("home-start-run").performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("main-header").assertDoesNotExist()
            compose.onNodeWithTag(BOTTOM_NAV_TAG).assertDoesNotExist()
            compose.onNodeWithTag("run-primary-action").assertIsDisplayed().assertHasClickAction()
            compose.onNodeWithTag("run-finish").assertIsDisplayed().assertHasClickAction()
            capture("${next.width}-${next.font}-${next.mode}-run-paused")
            compose.onNodeWithTag("run-finish").performClick()
            // R04 — 러닝을 마칠까요?(아래 시트)
            compose.onNodeWithTag("run-end-dialog").assertIsDisplayed()
            capture("${next.width}-${next.font}-${next.mode}-run-finish-confirm")
            compose.onNodeWithTag("run-end-continue").performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("run-primary-action").assertIsDisplayed()
            compose.onNodeWithTag(BOTTOM_NAV_TAG).assertDoesNotExist()
            compose.onNodeWithTag("run-finish").performClick()
            compose.onNodeWithTag("run-end-dialog").assertIsDisplayed()
            pressBack()
            compose.waitForIdle()
            compose.onNodeWithTag("run-end-dialog").assertDoesNotExist()
            compose.onNodeWithTag("run-finish").assertIsDisplayed()
            // Presentation fixtures only: these do not simulate server settlement or GPS.
            for (saveStatus in listOf(com.stepup.android.service.RunSaveStatus.SAVING,
                com.stepup.android.service.RunSaveStatus.FAILED)) {
                com.stepup.android.service.WalkSessionService.showStateForTest(
                    com.stepup.android.service.WalkSessionState(isActive = true, isPaused = true,
                        elapsedSec = 623, steps = 1200, saveStatus = saveStatus),
                )
                compose.waitForIdle()
                compose.onNodeWithTag("run-finish").assertDoesNotExist()
                val action = compose.onNodeWithTag("run-primary-action").assertIsDisplayed()
                if (saveStatus == com.stepup.android.service.RunSaveStatus.SAVING) {
                    action.assertIsNotEnabled()
                    compose.onNodeWithTag("run-save-error").assertDoesNotExist()
                } else {
                    action.assertIsEnabled()
                    compose.onNodeWithTag("run-save-error").assertIsDisplayed()
                }
                capture("${next.width}-${next.font}-${next.mode}-save-$saveStatus")
            }
            com.stepup.android.service.WalkSessionService.showStateForTest(
                com.stepup.android.service.WalkSessionState(
                    lastRewardPoints = 4.0, lastSessionSteps = 1200, lastElapsedSec = 623,
                    lastStartedAt = 1L,
                ),
            )
            compose.waitForIdle()
            compose.onNodeWithTag("run-result-done").assertIsDisplayed().assertHasClickAction()
            // 러닝 결과(E10) — 서버 확인 전에는 이 폰이 셈한 값을 "이번 러닝 SUP · 예상 · 확인 중"으로만 보인다
            compose.onNodeWithTag("run-result-reward").onChildren().onFirst().assertTextEquals("+4.0")
            compose.onNodeWithTag("run-result-reward-label", useUnmergedTree = true)
                .assertTextEquals(compose.activity.getString(R.string.run_reward_this))
            compose.onNodeWithText(compose.activity.getString(R.string.run_reward_estimate), useUnmergedTree = true).assertExists()
            compose.onNodeWithTag("run-result-settle-pending", useUnmergedTree = true).assertExists()
            compose.onAllNodesWithTag("run-result-settle-done", useUnmergedTree = true).assertCountEquals(0)
            capture("${next.width}-${next.font}-${next.mode}-result-pending")
            com.stepup.android.service.WalkSessionService.showStateForTest(
                com.stepup.android.service.WalkSessionState(
                    lastRewardPoints = 4.0, lastSessionSteps = 1200, lastElapsedSec = 623,
                    lastStartedAt = 1L, lastVerdict = com.stepup.android.domain.RunVerdict.VOID,
                ),
            )
            compose.waitForIdle()
            // 지급 제외(E08) — 금액을 적지 않고 "지급 제외"와 까닭(자세히 보기)만
            compose.onAllNodesWithTag("run-result-reward").assertCountEquals(0)
            compose.onNodeWithTag("run-result-settle-void", useUnmergedTree = true).assertExists()
            compose.onNodeWithTag("run-result-void-reason", useUnmergedTree = true).assertExists()
            capture("${next.width}-${next.font}-${next.mode}-result-void")
            compose.onNodeWithTag("run-result-done").assertIsDisplayed().performClick()
            compose.waitForIdle()
            assertEquals("result returns to the same navigation", bar, bounds(BOTTOM_NAV_TAG))
            // Re-enter the paused fixture to check the independent system Back route as well.
            com.stepup.android.service.WalkSessionService.showStateForTest(
                com.stepup.android.service.WalkSessionState(isActive = true, isPaused = true),
            )
            compose.onNodeWithTag("home-start-run").performClick()
            compose.waitForIdle()
            // 멈춘 러닝에서 뒤로 가기는 화면을 닫지 않고 마칠지 묻는다(R04 — 달리는 중이면 먼저 일시정지)
            pressBack()
            compose.waitForIdle()
            compose.onNodeWithTag("run-end-dialog").assertIsDisplayed()
            capture("${next.width}-${next.font}-${next.mode}-run-back-asks-end")
            pressBack()
            compose.waitForIdle()
            compose.onNodeWithTag("run-end-dialog").assertDoesNotExist()
            // 러닝이 끝난 뒤의 뒤로 가기는 원래대로 화면을 닫는다
            com.stepup.android.service.WalkSessionService.showStateForTest(com.stepup.android.service.WalkSessionState())
            compose.waitForIdle()
            pressBack()
            compose.waitForIdle()
            assertEquals("run returns to the same navigation", bar, bounds(BOTTOM_NAV_TAG))
            com.stepup.android.service.WalkSessionService.showStateForTest(com.stepup.android.service.WalkSessionState())
            // 러닝 홈(시안 HOME · E01) — 오늘의 활동은 걸음 칸을 누르면 여는 시트 안에
            compose.onNodeWithTag("home-steps").performClick()
            compose.waitForIdle()
            compose.onNodeWithText(compose.activity.getString(R.string.home_activity_title)).assertIsDisplayed()
            pressBack()
            compose.waitForIdle()
            compose.onNodeWithTag("home-start-run").assertIsDisplayed()
        }
    }

    private fun bounds(tag: String): Rect = compose.onNodeWithTag(tag, useUnmergedTree = true)
        .assertIsDisplayed().fetchSemanticsNode().boundsInRoot

    private fun awaitVisible(tag: String) {
        try {
            // A system Back key and lifecycle-backed state can settle after Compose first becomes idle.
            compose.waitUntil(timeoutMillis = 5_000) {
                runCatching { compose.onNodeWithTag(tag).assertIsDisplayed(); true }.getOrDefault(false)
            }
        } catch (failure: Throwable) {
            capture("failed-wait-$tag")
            throw failure
        }
    }

    // Dispatch an actual system Back key so dialog windows receive it before the activity.
    private fun pressBack() = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)

    private fun capture(name: String) {
        val directory = java.io.File(compose.activity.getExternalFilesDir(null), "chrome-checks").apply { mkdirs() }
        captureDisplay(java.io.File(directory, "$name.png"))
    }
}
