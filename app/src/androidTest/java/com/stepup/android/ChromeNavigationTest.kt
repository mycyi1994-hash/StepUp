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
                compose.onNodeWithTag("home-character-ready").isDisplayed()
            }
            val header = bounds("main-header")
            val logo = bounds("brand-wordmark")
            val bar = bounds(BOTTOM_NAV_TAG)
            val token = bounds("sup-balance")
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
                    compose.onNodeWithContentDescription(compose.activity.getString(R.string.profile_tab_settings)).performClick()
                    compose.waitForIdle()
                    assertEquals("profile settings keep header", header, bounds("main-header"))
                    assertEquals("profile settings keep navigation", bar, bounds(BOTTOM_NAV_TAG))
                    capture("${next.width}-${next.font}-${next.mode}-profile-settings")
                    pressBack()
                    compose.waitForIdle()
                    compose.onNodeWithTag("profile-settings").assertIsDisplayed()
                    assertEquals("settings Back returns to profile", logo, bounds("brand-wordmark"))
                    compose.onNode(hasText(compose.activity.getString(R.string.home_shortcut_challenges)) and hasClickAction())
                        .performScrollTo().performClick()
                    compose.waitForIdle()
                    compose.onNodeWithTag("challenge-primary-action").assertIsDisplayed().assertHasClickAction()
                    (0..2).forEach { choice ->
                        compose.onNodeWithTag("challenge-choice-$choice").performScrollTo().performClick()
                        compose.waitForIdle()
                        compose.onNodeWithTag("challenge-primary-action").assertIsDisplayed()
                        capture("${next.width}-${next.font}-${next.mode}-challenge-$choice")
                    }
                    pressBack()
                    compose.waitForIdle()
                    assertEquals("challenge returns to profile chrome", bar, bounds(BOTTOM_NAV_TAG))
                }
                if (tab == R.string.tab_community) {
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
            compose.onNodeWithText(compose.activity.getString(R.string.run_stop_confirm_title)).assertIsDisplayed()
            capture("${next.width}-${next.font}-${next.mode}-run-finish-confirm")
            compose.onNodeWithText(compose.activity.getString(R.string.run_stop_confirm_no)).performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("run-primary-action").assertIsDisplayed()
            compose.onNodeWithTag(BOTTOM_NAV_TAG).assertDoesNotExist()
            compose.onNodeWithTag("run-finish").performClick()
            compose.onNodeWithText(compose.activity.getString(R.string.run_stop_confirm_title)).assertIsDisplayed()
            pressBack()
            compose.waitForIdle()
            compose.onNodeWithText(compose.activity.getString(R.string.run_stop_confirm_title)).assertDoesNotExist()
            compose.onNodeWithTag("run-finish").assertIsDisplayed()
            // Presentation fixtures only: these do not simulate server settlement or GPS.
            com.stepup.android.service.WalkSessionService.showStateForTest(
                com.stepup.android.service.WalkSessionState(
                    lastRewardPoints = 4.0, lastSessionSteps = 1200, lastElapsedSec = 623,
                    lastStartedAt = 1L,
                ),
            )
            compose.waitForIdle()
            compose.onNodeWithTag("run-result-done").assertIsDisplayed().assertHasClickAction()
            compose.onNodeWithTag("run-result-reward").assertTextEquals("—")
            capture("${next.width}-${next.font}-${next.mode}-result-pending")
            com.stepup.android.service.WalkSessionService.showStateForTest(
                com.stepup.android.service.WalkSessionState(
                    lastRewardPoints = 4.0, lastSessionSteps = 1200, lastElapsedSec = 623,
                    lastStartedAt = 1L, lastVerdict = com.stepup.android.domain.RunVerdict.VOID,
                ),
            )
            compose.waitForIdle()
            compose.onNodeWithTag("run-result-reward").assertTextEquals("0")
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
            pressBack()
            compose.waitForIdle()
            assertEquals("run returns to the same navigation", bar, bounds(BOTTOM_NAV_TAG))
            com.stepup.android.service.WalkSessionService.showStateForTest(com.stepup.android.service.WalkSessionState())
            compose.onNodeWithTag("home-details").performClick()
            compose.waitForIdle()
            compose.onNodeWithText(compose.activity.getString(R.string.home_today_earned)).assertIsDisplayed()
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
