package com.stepup.android

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.asAndroidBitmap
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
            val header = bounds("main-header")
            val logo = bounds("brand-wordmark")
            val bar = bounds(BOTTOM_NAV_TAG)
            val token = bounds("sup-balance")
            val tabRoutes = listOf("home", "customize", "community", "profile")
            assertEquals("icons stay on one baseline $next", 1,
                tabRoutes.map { bounds("nav-icon-$it").top }.distinct().size)
            assertEquals("labels stay on one baseline $next", 1,
                tabRoutes.map { bounds("nav-label-$it").top }.distinct().size)
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
                    compose.onNodeWithContentDescription(compose.activity.getString(R.string.profile_tab_settings)).performClick()
                    compose.waitForIdle()
                    assertEquals("profile settings keep header", header, bounds("main-header"))
                    assertEquals("profile settings keep navigation", bar, bounds(BOTTOM_NAV_TAG))
                    capture("${next.width}-${next.font}-${next.mode}-profile-settings")
                    pressBack()
                    compose.waitForIdle()
                    compose.onNodeWithTag("profile-settings").assertIsDisplayed()
                    assertEquals("settings Back returns to profile", logo, bounds("brand-wordmark"))
                }
                if (tab == R.string.tab_community) {
                    compose.onNodeWithContentDescription(compose.activity.getString(R.string.community_tab_my_crew)).performClick()
                    compose.waitForIdle()
                    assertEquals("crews keep navigation", bar, bounds(BOTTOM_NAV_TAG))
                    pressBack()
                    compose.waitForIdle()
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

    // Dispatch an actual system Back key so dialog windows receive it before the activity.
    private fun pressBack() = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)

    private fun capture(name: String) {
        val directory = java.io.File(compose.activity.getExternalFilesDir(null), "chrome-checks").apply { mkdirs() }
        val bitmap = compose.onNodeWithTag("chrome-viewport").captureToImage().asAndroidBitmap()
        java.io.File(directory, "$name.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
