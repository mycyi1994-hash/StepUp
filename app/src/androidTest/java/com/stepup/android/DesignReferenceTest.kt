package com.stepup.android

import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.TrackPoint
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Routes
import com.stepup.android.ui.Screen
import com.stepup.android.ui.BOTTOM_NAV_TAG
import com.stepup.android.ui.guide.GuideTour
import com.stepup.android.ui.screens.login.LoginContent
import androidx.compose.ui.test.*
import com.stepup.android.service.WalkSessionService
import com.stepup.android.service.WalkSessionState
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.theme.Night
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale

/**
 * Character-free native captures with the real shared header/navigation.
 * Ten approved destinations plus paused/no-GPS running, market and news.
 * Route coordinates and running numbers below are display fixtures only;
 * they never start GPS, write a run or confirm a reward. Not pixel-golden tests:
 * home scenery intentionally varies and map tiles depend on availability.
 */
@RunWith(AndroidJUnit4::class)
class DesignReferenceTest {
    @get:Rule(order = 0) val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.ACTIVITY_RECOGNITION,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.POST_NOTIFICATIONS,
    )
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()

    private val findings = mutableListOf<JSONObject>()

    @Before fun prepare() {
        runBlocking { seed() }
    }

    private suspend fun seed() {
        ServiceLocator.userPrefs.setReducedMotion(true)
        ServiceLocator.userPrefs.setSounds(false)
        ServiceLocator.userPrefs.setHaptics(false)
        ServiceLocator.userPrefs.setLoginMethod("guest")
        ServiceLocator.userPrefs.setGuideSeen()
        GuideTour.stop()
        WalkSessionService.showStateForTest(WalkSessionState())
        ServiceLocator.userPrefs.ensureRunnerUid()
        ServiceLocator.sneakerRepository.ensureStarter()
        // 크루·글은 서버에만 있다. 서버 없이 도는 검사라 같은 모양의 자료를 채운다.
        TestData.seedCommunity()
        ServiceLocator.courseRepository.ensureSeeded()
    }

    @After fun restore() {
        WalkSessionService.showStateForTest(WalkSessionState())
        GuideTour.stop()
    }

    private companion object {
        const val CAPTURE_DENSITY = 1.8f
    }

    private enum class Scene {
        HOME, SHOES, DRAW, RUN_ACTIVE, RUN_FINISH, COMMUNITY, PROFILE, CHALLENGE, LOGIN, FIRST_GUIDE,
        RUN_PAUSED, RUN_NO_GPS, MARKET, NEWS,
        POST_COMPOSE, CREW_CREATE, FLASH_DETAIL, EXPERIENCE, NOTIFICATIONS, PRIVACY, SUPPORT,
        CONNECTED, THEME, LANGUAGE,
        ANALYTICS, HISTORY_MAP, WALLET, ACHIEVEMENTS, INBOX,
        COURSES, EXPLORE_MAP, RANKING,
    }

    private enum class Group { PRIMARY, SECONDARY, RECORDS, EXPLORE }

    @Test fun referenceViewports() = captureViewports(Group.PRIMARY)

    @Test fun secondaryViewports() = captureViewports(Group.SECONDARY)

    @Test fun recordViewports() = captureViewports(Group.RECORDS)

    @Test fun exploreViewports() = captureViewports(Group.EXPLORE)

    private fun captureViewports(group: Group) {
        ServiceLocator.stepRepository.startTracking()
        ServiceLocator.stepRepository.simulateSteps((12840 - ServiceLocator.stepRepository.todaySteps.value).coerceAtLeast(0))

        var width by mutableIntStateOf(390)
        var height by mutableIntStateOf(844)
        var large by mutableStateOf(false)
        var scene by mutableStateOf(Scene.HOME)
        compose.setContent {
            val base = LocalContext.current
            val config = Configuration(LocalConfiguration.current).apply {
                setLocales(LocaleList(Locale.forLanguageTag("ko")))
                fontScale = if (large) 1.6f else 1f
                screenWidthDp = width
                screenHeightDp = height
            }
            val localized = remember(width, height, large) { base.createConfigurationContext(config) }
            CompositionLocalProvider(
                LocalActivityResultRegistryOwner provides compose.activity,
                LocalOnBackPressedDispatcherOwner provides compose.activity,
                LocalContext provides localized,
                LocalConfiguration provides config,
                // 430×932dp 도 에뮬레이터 창(pixel_2) 안에 온전히 들어가도록 밀도를 1.8 로
                // 고정한다. 창보다 크면 캡처가 잘린다. 레이아웃은 dp 기준이라 같다.
                LocalDensity provides Density(CAPTURE_DENSITY, config.fontScale),
            ) {
                StepUpTheme(ThemeMode.DARK) {
                    ExperienceProvider {
                        Box(
                            Modifier
                                .requiredSize(width.dp, height.dp)
                                .background(Night)
                                .testTag("capture"),
                        ) {
                            key(scene, width, height, large) { Render(scene) }
                        }
                    }
                }
            }
        }

        val viewports = listOf(
            Triple(360, 760, false),
            Triple(390, 844, false),
            Triple(430, 932, false),
            Triple(390, 844, true),
        )
        val layoutFailures = mutableListOf<String>()
        val selected = Scene.entries.filter {
            when (group) {
                Group.PRIMARY -> it.ordinal <= Scene.NEWS.ordinal
                Group.SECONDARY -> it.ordinal in Scene.POST_COMPOSE.ordinal..Scene.LANGUAGE.ordinal
                Group.RECORDS -> it.ordinal in Scene.ANALYTICS.ordinal..Scene.INBOX.ordinal
                Group.EXPLORE -> it.ordinal >= Scene.COURSES.ordinal
            }
        }
        val prefix = when (group) {
            Group.PRIMARY -> "ref"
            Group.SECONDARY -> "secondary"
            Group.RECORDS -> "record"
            Group.EXPLORE -> "explore"
        }
        for ((w, h, enlarged) in viewports.filter { group == Group.PRIMARY || it.first != 430 }) {
            for (s in selected) {
                prepareScene(s)
                compose.runOnIdle { width = w; height = h; large = enlarged; scene = s }
                compose.waitForIdle()
                val name = "$prefix-$w-${if (enlarged) "large" else "normal"}-${s.ordinal.toString().padStart(2, '0')}-${s.name.lowercase()}"
                try {
                    awaitScene(s)
                } catch (failure: Throwable) {
                    capture("$name-failed")
                    throw failure
                }
                try {
                    assertReviewedLayout(s, enlarged)
                } catch (failure: AssertionError) {
                    // Retain every scene for diagnosis; report all layout failures at the end.
                    layoutFailures += "$name: ${failure.message}"
                }
                audit(name)
                capture(name)
                if (s == Scene.POST_COMPOSE) {
                    compose.onNodeWithText(korean(R.string.post_cat_flash)).performClick()
                    compose.onNodeWithTag("form-content").performScrollToNode(hasContentDescription(korean(R.string.post_field_capacity)))
                    compose.onNodeWithContentDescription(korean(R.string.post_field_capacity)).assertIsDisplayed()
                    compose.onNodeWithTag("post-submit").assertIsDisplayed().assertIsNotEnabled()
                    capture("$name-meetup-fields")
                }
                if (s == Scene.FLASH_DETAIL) {
                    compose.onNode(hasScrollAction()).performScrollToNode(hasText(korean(R.string.flash_enter_chat)))
                    compose.onNodeWithText(korean(R.string.flash_enter_chat)).assertIsDisplayed()
                    compose.onNodeWithTag("detail-primary-action").assertIsDisplayed()
                    capture("$name-chat")
                }
                if (s == Scene.ANALYTICS) {
                    compose.onNodeWithText(korean(R.string.analytics_tab_quarter)).performClick()
                    compose.onNode(hasText(korean(R.string.analytics_tab_quarter)) and isSelected()).assertExists()
                    capture("$name-quarter")
                }
                if (s == Scene.HISTORY_MAP) {
                    compose.onNodeWithText(korean(R.string.history_period_all)).performClick()
                    capture("$name-all")
                }
                if (s == Scene.COURSES) {
                    compose.onNodeWithText(korean(R.string.courses_make)).performClick()
                    capture("$name-make")
                    compose.onNodeWithText(korean(R.string.courses_board)).performClick()
                    capture("$name-board")
                }
                if (s == Scene.EXPLORE_MAP) {
                    compose.onNodeWithText(korean(R.string.map_seg_territory)).performClick()
                    capture("$name-territory")
                }
                if (s == Scene.RANKING) {
                    compose.onNodeWithText(korean(R.string.rank_period_all)).performClick()
                    capture("$name-all")
                }
            }
        }
        val directory = File(compose.activity.getExternalFilesDir(null), "experience-qa").apply { mkdirs() }
        File(directory, "$prefix-audit.jsonl").writeText(findings.joinToString("\n") { it.toString() })
        File(directory, "$prefix-layout-checks.txt").writeText(
            if (layoutFailures.isEmpty()) "PASS: reviewed layout checks" else layoutFailures.joinToString("\n"),
        )
        assertTrue(layoutFailures.joinToString("\n"), layoutFailures.isEmpty())
    }

    private fun prepareScene(s: Scene) {
        GuideTour.stop()
        val now = System.currentTimeMillis()
        val fixtureTrack = listOf(
            GeoPoint(37.5280, 126.9320), GeoPoint(37.5290, 126.9340),
            GeoPoint(37.5295, 126.9370), GeoPoint(37.5302, 126.9390),
        ).mapIndexed { index, point -> TrackPoint(point.lat, point.lng, now - 1_458_000 + index * 120_000L) }
        WalkSessionService.showStateForTest(when (s) {
            Scene.RUN_ACTIVE, Scene.RUN_PAUSED, Scene.RUN_NO_GPS -> WalkSessionState(
                isActive = true, isPaused = s == Scene.RUN_PAUSED,
                steps = 4_200, elapsedSec = 1_458, startedAt = now - 1_458_000,
                gpsFix = s != Scene.RUN_NO_GPS,
                track = if (s == Scene.RUN_NO_GPS) emptyList() else fixtureTrack,
            )
            Scene.RUN_FINISH -> WalkSessionState(
                lastRewardPoints = 4.0, lastSessionSteps = 4_200, lastRewardedSteps = 4_200,
                lastElapsedSec = 1_458, lastGpsKm = 3.2, lastStartedAt = now,
                track = fixtureTrack,
            )
            else -> WalkSessionState()
        })
    }

    private fun awaitScene(s: Scene) {
        val readyTag = when (s) {
            Scene.HOME -> "home-start-run"
            Scene.SHOES -> "shoe-equip"
            Scene.DRAW -> "draw-shoe"
            Scene.RUN_ACTIVE, Scene.RUN_PAUSED, Scene.RUN_NO_GPS -> "run-live-map"
            Scene.RUN_FINISH -> "run-result-map"
            Scene.COMMUNITY -> "community-featured-title"
            Scene.PROFILE -> "profile-settings"
            Scene.CHALLENGE -> "challenge-primary-action"
            Scene.LOGIN -> "login-google"
            Scene.FIRST_GUIDE -> "guide-step-title"
            Scene.POST_COMPOSE -> "post-submit"
            Scene.CREW_CREATE -> "crew-create-submit"
            Scene.FLASH_DETAIL -> "detail-primary-action"
            Scene.ANALYTICS -> "bottom-nav"
            Scene.HISTORY_MAP -> "bottom-nav"
            Scene.WALLET -> "bottom-nav"
            Scene.ACHIEVEMENTS -> "bottom-nav"
            Scene.INBOX -> "bottom-nav"
            Scene.COURSES, Scene.EXPLORE_MAP, Scene.RANKING -> "bottom-nav"
            else -> "bottom-nav"
        }
        // Clickable cards merge child text for accessibility; readiness may target that child.
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(readyTag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        if (s == Scene.HOME) {
            compose.waitUntil(5_000) { compose.onAllNodesWithText("12,840").fetchSemanticsNodes().isNotEmpty() }
        }
        when (s) {
            Scene.RUN_ACTIVE, Scene.RUN_PAUSED, Scene.RUN_NO_GPS, Scene.RUN_FINISH, Scene.LOGIN, Scene.POST_COMPOSE, Scene.CREW_CREATE ->
                compose.onNodeWithTag(BOTTOM_NAV_TAG).assertDoesNotExist()
            else -> compose.onNodeWithTag(BOTTOM_NAV_TAG).assertExists()
        }
        if (s == Scene.SHOES) compose.onNodeWithTag("shoe-equip").assertIsNotEnabled()
        if (s == Scene.RUN_NO_GPS) compose.onNodeWithText(korean(R.string.map_waiting_title)).assertExists()
        if (s == Scene.RUN_FINISH) {
            compose.onNode(hasText("—") and hasAnyAncestor(hasTestTag("run-result-reward"))).assertExists()
        }
    }

    private fun korean(id: Int): String {
        val config = Configuration(compose.activity.resources.configuration).apply {
            setLocales(LocaleList(Locale.KOREAN))
        }
        return compose.activity.createConfigurationContext(config).getString(id)
    }

    /** Guard the visible regressions from the independent visual review. */
    private fun assertReviewedLayout(scene: Scene, enlarged: Boolean) {
        if (scene == Scene.HOME) {
            val nav = compose.onNodeWithTag(BOTTOM_NAV_TAG).getUnclippedBoundsInRoot()
            val draw = compose.onNodeWithTag("nav-draw-action").getUnclippedBoundsInRoot()
            assertTrue("Draw action must stay centered at every font size",
                kotlin.math.abs((draw.left + draw.right - nav.left - nav.right).value) <= 2f)
            val nodes = listOf(Screen.Run, Screen.Customize, Screen.Community, Screen.Profile).map {
                compose.onNodeWithTag("nav-label-${it.route}", useUnmergedTree = true).fetchSemanticsNode()
            }
            compose.runOnIdle {
                nodes.forEach { node ->
                    val layouts = mutableListOf<TextLayoutResult>()
                    node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
                    assertTrue("Navigation label must fit without shrinking or orphan syllables",
                        layouts.isNotEmpty() && layouts.all { it.lineCount == 1 && !it.didOverflowWidth })
                }
            }
        }
        if (scene == Scene.RUN_ACTIVE && !enlarged) {
            val map = compose.onNodeWithTag("run-live-map").getUnclippedBoundsInRoot()
            val action = compose.onNodeWithTag("run-primary-action").getUnclippedBoundsInRoot()
            assertTrue("Full live map must be above the pinned action: map=${map.bottom}, action=${action.top}", map.bottom <= action.top)
        }
        if (scene == Scene.PROFILE) {
            compose.onNodeWithTag("profile-records").assertIsDisplayed()
        }
        if (scene == Scene.COMMUNITY) {
            val title = compose.onNodeWithTag("community-featured-title", useUnmergedTree = true)
                .getUnclippedBoundsInRoot()
            val action = compose.onNodeWithTag("community-primary").getUnclippedBoundsInRoot()
            assertTrue("Meetup title must remain above its action", title.bottom <= action.top)
        }
    }

    @Composable private fun Render(s: Scene) {
        when (s) {
            Scene.HOME -> MainScaffold()
            Scene.SHOES -> MainScaffold(initialTab = Screen.Customize)
            Scene.DRAW -> MainScaffold(initialRoute = Routes.MYSTERY_BOX)
            Scene.RUN_ACTIVE, Scene.RUN_PAUSED, Scene.RUN_NO_GPS, Scene.RUN_FINISH -> MainScaffold(initialRoute = Routes.RUN)
            Scene.COMMUNITY -> MainScaffold(initialTab = Screen.Community)
            Scene.PROFILE -> MainScaffold(initialTab = Screen.Profile)
            Scene.CHALLENGE -> MainScaffold(initialRoute = Routes.EVENTS)
            Scene.LOGIN -> LoginContent(signingIn = false, error = null, onSignIn = {})
            Scene.FIRST_GUIDE -> MainScaffold(startTour = true)
            Scene.MARKET -> MainScaffold(initialRoute = Routes.RUNNER_MARKET)
            Scene.NEWS -> MainScaffold(initialRoute = Routes.NEWS)
            Scene.POST_COMPOSE -> MainScaffold(initialRoute = Routes.postCompose(""))
            Scene.CREW_CREATE -> MainScaffold(initialRoute = Routes.CREW_CREATE)
            Scene.FLASH_DETAIL -> MainScaffold(initialRoute = Routes.flashDetail(101L))
            Scene.EXPERIENCE -> MainScaffold(initialRoute = Routes.SETTINGS_EXPERIENCE)
            Scene.NOTIFICATIONS -> MainScaffold(initialRoute = Routes.SETTINGS_NOTIFICATIONS)
            Scene.PRIVACY -> MainScaffold(initialRoute = Routes.SETTINGS_PRIVACY)
            Scene.SUPPORT -> MainScaffold(initialRoute = Routes.SETTINGS_SUPPORT)
            Scene.CONNECTED -> MainScaffold(initialRoute = Routes.SETTINGS_CONNECTED)
            Scene.THEME -> MainScaffold(initialRoute = Routes.SETTINGS_THEME)
            Scene.LANGUAGE -> MainScaffold(initialRoute = Routes.SETTINGS_LANGUAGE)
            Scene.ANALYTICS -> MainScaffold(initialRoute = Routes.ANALYTICS)
            Scene.HISTORY_MAP -> MainScaffold(initialRoute = Routes.HISTORY_MAP)
            Scene.WALLET -> MainScaffold(initialRoute = Routes.WALLET)
            Scene.ACHIEVEMENTS -> MainScaffold(initialRoute = Routes.ACHIEVEMENTS)
            Scene.INBOX -> MainScaffold(initialRoute = Routes.NOTIFICATIONS)
            Scene.COURSES -> MainScaffold(initialRoute = Routes.COURSES)
            Scene.EXPLORE_MAP -> MainScaffold(initialRoute = Routes.MAP)
            Scene.RANKING -> MainScaffold(initialRoute = Routes.RANKING)
        }
    }

    private fun capture(name: String) {
        val bitmap = compose.onNodeWithTag("capture").captureToImage().asAndroidBitmap()
        assertTrue(bitmap.width > 300 && bitmap.height > 500)
        val directory = File(compose.activity.getExternalFilesDir(null), "experience-qa").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** 잘리거나 넘친 글자를 기록한다 — 캡처만으로는 놓치는 것 */
    private fun audit(scene: String) {
        val nodes = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
            .fetchSemanticsNodes().filter { it.boundsInRoot.width > 1f && it.boundsInRoot.height > 1f }
        compose.runOnIdle {
            nodes.forEach { node ->
                val layouts = mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
                layouts.forEach { result ->
                    if (result.hasVisualOverflow) {
                        findings += JSONObject().put("scene", scene)
                            .put("kind", if (result.layoutInput.overflow == TextOverflow.Ellipsis) "ellipsis" else "overflow")
                            .put("text", result.layoutInput.text.text)
                    }
                }
            }
        }
    }
}
