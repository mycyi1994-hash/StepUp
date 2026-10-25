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
import org.junit.Assert.assertTrue
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
            // 러닝 화면은 자유 러닝(보상 UI 없는 지도 화면)으로 본다 — 앞 테스트가 남긴 코스 · 코스 만들기를 비운다
            ServiceLocator.courseRepository.clearSelection()
            ServiceLocator.courseRepository.cancelRecording()
        }
        com.stepup.android.domain.RunPlans.clear()
        com.stepup.android.ui.screens.events.ChallengeRunFocus.clear()
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
            // 하단 탭 넷 — 러닝 / 신발 / 커뮤니티 / 내 정보(2026-10-26 시안)
            val tabRoutes = listOf("home", "customize", "community", "profile")
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
            listOf(R.string.tab_customize, R.string.tab_community, R.string.tab_me, R.string.tab_run).forEach { tab ->
                compose.onNode(hasText(compose.activity.getString(tab)) and hasAnyAncestor(hasTestTag(BOTTOM_NAV_TAG)))
                    .performClick()
                compose.waitForIdle()
                // UI 최종 통합안: 함께 뛰기는 앱 머리 없이 큰 제목 · 세그먼트, 내 정보는 머리 오른쪽이 SUP 대신 설정 원 버튼
                if (tab == R.string.tab_community) {
                    compose.onNodeWithTag("main-header").assertDoesNotExist()
                    compose.onNodeWithTag("community-tab-meetups").assertIsDisplayed()
                } else {
                    assertEquals("header $next/$tab", header, bounds("main-header"))
                    assertEquals("logo $next/$tab", logo, bounds("brand-wordmark"))
                }
                assertEquals("navigation $next/$tab", bar, bounds(BOTTOM_NAV_TAG))
                if (tab == R.string.tab_me) compose.onNodeWithTag("header-settings").assertIsDisplayed()
                else if (tab == R.string.tab_customize) {
                    // 신발 탭 머리는 로고 · SUP 만(시안 shoes — 알림 종 없음) — 같은 줄 · 같은 크기로 오른쪽 끝에 붙는다
                    compose.onNodeWithTag("header-notifications").assertDoesNotExist()
                    val pill = bounds("sup-balance")
                    assertEquals("balance row $next/$tab", token.top, pill.top, 1f)
                    assertEquals("balance size $next/$tab", token.width, pill.width, 1f)
                    assertTrue("balance moves to the end without the bell $next/$tab: $pill / $token", pill.right > token.right)
                } else if (tab != R.string.tab_community) assertEquals("balance $next/$tab", token, bounds("sup-balance"))
                capture("${next.width}-${next.font}-${next.mode}-$tab")
                if (tab == R.string.tab_me) {
                    // 파란 톤 v4 내 정보(PF01): 러닝 패스가 커서 짧은 화면에서는 본문이 넘어간다(지시서: 짧은 화면 · 큰 글자는 스크롤).
                    // 넘겨서 닿고, 닿은 자리는 하단 탭 위에 온전히 보여야 한다
                    compose.onNodeWithTag("profile-wallet").performScrollTo().assertIsDisplayed()
                    compose.waitForIdle()
                    org.junit.Assert.assertTrue("profile destinations scroll above navigation $next", bounds("profile-wallet").bottom <= bar.top)
                    compose.onNodeWithTag("profile-settings").performScrollTo()
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
                    // 내 정보의 챌린지 줄은 뺐다(2026-10-17)
                    compose.onAllNodesWithTag("profile-challenges").assertCountEquals(0)
                }
                if (tab == R.string.tab_community) {
                    // 첫 화면은 번개러닝 세그먼트 — 이번 주 "모두 보기"는 번개만 모은 목록(뒤로 오면 그대로)
                    compose.openCommunityMeetups()
                    // "이번 주 · 모두 보기"는 사진 포스터 아래 — 낮은 화면에서는 목록을 내려야 보인다
                    compose.onNodeWithTag("community-meetups").performScrollToNode(hasTestTag("community-all-meetups"))
                    awaitVisible("community-all-meetups")
                    compose.onNodeWithTag("community-all-meetups").assertIsDisplayed().performClick()
                    compose.waitForIdle()
                    assertEquals("meetups keep navigation", bar, bounds(BOTTOM_NAV_TAG))
                    pressBack()
                    compose.waitForIdle()
                    awaitVisible("community-all-meetups")
                    compose.onNodeWithTag("community-all-meetups").assertIsDisplayed()
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
            // 자유 러닝(UI 최종 통합안 paused) — 위 제목 "일시정지" · 큰 거리 · 지도 카드 · 숫자 줄 · 길게 눌러 종료 | 재개
            compose.onNodeWithTag("run-live").assertExists()
            compose.onNodeWithTag("run-paused-band", useUnmergedTree = true).assertExists()
            compose.onNodeWithTag("run-primary-action").assertIsDisplayed().assertHasClickAction()
                .assertTextContains(compose.activity.getString(R.string.run_resume_short))
            compose.onNodeWithTag("run-finish").assertIsDisplayed().assertHasClickAction()
            capture("${next.width}-${next.font}-${next.mode}-run-paused")
            // 짧게 누르면 마치지 않고 "길게 눌러 종료해요" — 확인창은 없다
            compose.onNodeWithTag("run-finish").performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("run-hold-tip", useUnmergedTree = true).assertIsDisplayed()
            compose.onAllNodesWithTag("run-end-dialog").assertCountEquals(0)
            capture("${next.width}-${next.font}-${next.mode}-run-finish-hint")
            compose.onNodeWithTag("run-primary-action").assertIsDisplayed()
            compose.onNodeWithTag(BOTTOM_NAV_TAG).assertDoesNotExist()
            // Presentation fixtures only: these do not simulate server settlement or GPS.
            // 자유 러닝은 저장 중(06) · 저장 실패(07)도 같은 지도 · 기록 판이다. 실패는 시트 없이 위 띠 · 다시 저장 · 화면에 머물기
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
                    compose.onNodeWithTag("run-saving").assertExists()
                    action.assertIsNotEnabled()
                    compose.onNodeWithTag("run-save-error").assertDoesNotExist()
                } else {
                    action.assertIsEnabled()
                    compose.onNodeWithTag("run-save-error").assertIsDisplayed()
                    compose.onNodeWithTag("run-save-stay").assertIsDisplayed()
                    compose.onNodeWithTag("run-save-failed-dialog").assertDoesNotExist()
                    compose.onNodeWithTag("run-details-open").assertDoesNotExist()
                }
                capture("${next.width}-${next.font}-${next.mode}-save-$saveStatus")
                if (saveStatus == com.stepup.android.service.RunSaveStatus.FAILED) {
                    // "화면에 머물기"는 한 번 누르면 숨는다 — 위 띠와 다시 저장은 남는다
                    compose.onNodeWithTag("run-save-stay").performClick()
                    compose.waitForIdle()
                    compose.onNodeWithTag("run-save-stay").assertDoesNotExist()
                    compose.onNodeWithTag("run-save-error").assertIsDisplayed()
                    compose.onNodeWithTag("run-primary-action").assertIsDisplayed().assertIsEnabled()
                }
            }
            com.stepup.android.service.WalkSessionService.showStateForTest(
                com.stepup.android.service.WalkSessionState(
                    lastRewardPoints = 4.0, lastSessionSteps = 1200, lastElapsedSec = 623,
                    lastStartedAt = 1L,
                ),
            )
            compose.waitForIdle()
            // 자유 러닝 결과(08) — 기록 저장만 알린다. 이 폰이 셈한 보상 · 서버 확인 상태(SUP · 예상 · 확인 중)는 두지 않는다
            compose.onNodeWithTag("run-result").assertExists()
            compose.onNodeWithTag("run-result-done").assertIsDisplayed().assertHasClickAction()
            compose.onNodeWithTag("run-result-saved", useUnmergedTree = true)
                .assertTextEquals(compose.activity.getString(R.string.run_free_record_saved))
            // 거리는 GPS 로 잰 값만 — GPS 거리가 없으면 걸음으로 셈하지 않고 "—"
            compose.onNodeWithTag("run-result-distance", useUnmergedTree = true).assertTextEquals("—")
            assertNoRewardOnFreeResult("result")
            capture("${next.width}-${next.font}-${next.mode}-result")
            com.stepup.android.service.WalkSessionService.showStateForTest(
                com.stepup.android.service.WalkSessionState(
                    lastRewardPoints = 4.0, lastSessionSteps = 1200, lastElapsedSec = 623,
                    lastStartedAt = 1L, lastVerdict = com.stepup.android.domain.RunVerdict.VOID,
                ),
            )
            compose.waitForIdle()
            // 지급 제외(VOID)여도 자유 러닝 결과는 같은 모습이다 — "지급 제외" · 까닭 줄을 따로 두지 않는다
            compose.onNodeWithTag("run-result").assertExists()
            compose.onNodeWithTag("run-result-done").assertIsDisplayed().assertHasClickAction()
            compose.onNodeWithTag("run-result-distance", useUnmergedTree = true).assertTextEquals("—")
            assertNoRewardOnFreeResult("void result")
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
            // 멈춘 자유 러닝에서 뒤로 가기는 마칠지 묻지 않고 화면만 닫는다(러닝은 멈춘 채 남는다 — 홈의 "러닝 이어가기")
            pressBack()
            compose.waitForIdle()
            compose.onAllNodesWithTag("run-end-dialog").assertCountEquals(0)
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

    /** 자유 러닝 결과에는 보상 칸 · 정산 상태 · 무효 까닭 · SUP 글이 없다(보상 상태와 상관없이 같은 모습) */
    private fun assertNoRewardOnFreeResult(state: String) {
        listOf(
            "run-result-reward", "run-result-reward-card", "run-result-reward-label", "run-result-reward-note",
            "run-result-settle-pending", "run-result-settle-done", "run-result-settle-void", "run-result-settle-rejected",
            "run-result-void-reason", "run-result-shoe", "run-result-note", "run-result-basis", "run-result-message",
            "run-result-scroll",
        ).forEach { tag ->
            assertEquals("free run $state must not show $tag", 0,
                compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().size)
        }
        listOf(R.string.run_reward_this, R.string.run_reward_estimate).forEach { id ->
            val text = compose.activity.getString(id)
            assertEquals("free run $state must not say \"$text\"", 0,
                compose.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().size)
        }
        assertEquals("free run $state must not mention SUP", 0,
            compose.onAllNodesWithText("SUP", substring = true, useUnmergedTree = true).fetchSemanticsNodes().size)
        assertEquals("free run $state must not read SUP aloud", 0,
            compose.onAllNodesWithContentDescription("SUP", substring = true, useUnmergedTree = true).fetchSemanticsNodes().size)
    }

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
