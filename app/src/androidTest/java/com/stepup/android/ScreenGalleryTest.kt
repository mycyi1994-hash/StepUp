package com.stepup.android

import android.content.res.Configuration
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.AvatarGender
import com.stepup.android.service.WalkSessionService
import com.stepup.android.service.WalkSessionState
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Routes
import com.stepup.android.ui.guide.GuideTour
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.home.HomeScreen
import com.stepup.android.ui.screens.walk.*
import com.stepup.android.ui.screens.community.*
import com.stepup.android.ui.screens.items.*
import com.stepup.android.ui.screens.events.*
import com.stepup.android.ui.screens.profile.*
import com.stepup.android.ui.screens.rewards.WalletScreen
import com.stepup.android.ui.screens.notifications.NotificationsScreen
import com.stepup.android.ui.screens.settings.*
import com.stepup.android.ui.screens.login.LoginScreen
import com.stepup.android.ui.screens.splash.SplashScreen
import com.stepup.android.ui.screens.map.MapScreen
import com.stepup.android.ui.screens.market.MarketModelScreen
import com.stepup.android.ui.screens.customize.*
import com.stepup.android.ui.theme.*
import java.io.File
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

/** Temporary read-only UI gallery. No production screen or backend code changes. */
class ScreenGalleryTest {
    @get:Rule(order = 0) val permissions = GrantPermissionRule.grant(
        android.Manifest.permission.ACTIVITY_RECOGNITION,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.POST_NOTIFICATIONS,
    )
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    private var sneakerId = 0L
    private var crewId = ""
    private lateinit var localized: android.content.Context
    private val directory get() = File(compose.activity.getExternalFilesDir(null), "screen-gallery").apply { mkdirs() }
    private val failures = mutableListOf<String>()

    @Test fun allScreens() {
        runBlocking {
            ServiceLocator.userPrefs.setReducedMotion(true)
            ServiceLocator.userPrefs.setSounds(false)
            ServiceLocator.userPrefs.setHaptics(false)
            ServiceLocator.userPrefs.setLoginMethod("guest")
            ServiceLocator.userPrefs.setGuideSeen()
            ServiceLocator.userPrefs.ensureRunnerUid()
            ServiceLocator.sneakerRepository.ensureStarter()
            TestData.seedCommunity()
            ServiceLocator.courseRepository.ensureSeeded()
            TestData.seedWelcomeNotification()
            sneakerId = ServiceLocator.sneakerRepository.inventory.first().first().id
            crewId = ServiceLocator.crewRepository.crews.value.first().id
        }
        ServiceLocator.stepRepository.startTracking()
        ServiceLocator.stepRepository.simulateSteps((12840 - ServiceLocator.stepRepository.todaySteps.value).coerceAtLeast(0))
        var screen by mutableIntStateOf(0)
        var revision by mutableIntStateOf(0)
        compose.setContent {
            val base = LocalContext.current
            val config = Configuration(LocalConfiguration.current).apply {
                setLocales(LocaleList(Locale.KOREAN))
                fontScale = 1f
                screenWidthDp = 390
                screenHeightDp = 844
            }
            localized = remember { base.createConfigurationContext(config) }
            CompositionLocalProvider(
                LocalActivityResultRegistryOwner provides compose.activity,
                LocalOnBackPressedDispatcherOwner provides compose.activity,
                LocalContext provides localized, LocalConfiguration provides config,
                LocalDensity provides Density(1.8f, 1f),
            ) {
                StepUpTheme(ThemeMode.DARK) {
                    ExperienceProvider {
                        Box(Modifier.requiredSize(390.dp, 844.dp).background(Night).testTag("gallery")) {
                            key(screen, revision) { Scene(screen) }
                        }
                    }
                }
            }
        }
        fun reset(index: Int) {
            android.util.Log.i("ScreenGallery", "Opening scene $index")
            runBlocking { ServiceLocator.avatarRepository.setGender(AvatarGender.MALE) }
            WalkSessionService.showStateForTest(when (index) {
                34 -> WalkSessionState(isActive = true, steps = 4200, elapsedSec = 1458,
                    startedAt = System.currentTimeMillis() - 1458000, gpsFix = true)
                35 -> WalkSessionState(lastRewardPoints = 4.0, lastSessionSteps = 4200,
                    lastRewardedSteps = 4200, lastElapsedSec = 1458, lastGpsKm = 3.2,
                    lastStartedAt = System.currentTimeMillis())
                else -> WalkSessionState()
            })
            compose.runOnIdle { screen = index; revision++ }
            compose.waitForIdle()
            Thread.sleep(650)
        }
        for (index in 0..36) {
            reset(index)
            if (index == 22) {
                // Joining must be reachable without scrolling through the roster/chat.
                compose.onNodeWithText(localized.getString(R.string.flash_join_cta))
                    .assertIsDisplayed().assertIsEnabled().assertHasClickAction()
            }
            capture("screen-${index.toString().padStart(2, '0')}")
            if (index == 9) {
                val dayTag = "analytics-day-${java.time.LocalDate.now().toEpochDay()}"
                compose.onNodeWithTag(dayTag).performScrollTo().performClick().assertIsSelected()
                compose.onNodeWithTag("analytics-day-details").performScrollTo().assertIsDisplayed()
                capture("extra-analytics-day-details")
                compose.onNodeWithTag(dayTag).performScrollTo().performClick().assertIsNotSelected()
                compose.onNodeWithTag("analytics-day-details").assertDoesNotExist()
                compose.onNodeWithText(localized.getString(R.string.analytics_tab_quarter))
                    .performScrollTo().performClick()
                compose.onNodeWithTag("analytics-week-0").performScrollTo().performClick().assertIsSelected()
                compose.onNodeWithTag("analytics-week-details").performScrollTo().assertIsDisplayed()
                capture("extra-analytics-week-details")
                compose.onNodeWithTag("analytics-week-0").performScrollTo().performClick().assertIsNotSelected()
                compose.onNodeWithTag("analytics-week-details").assertDoesNotExist()
            }
            if (index == 20) {
                compose.onNodeWithTag("detail-primary-action").assertIsDisplayed()
                val before = runBlocking { ServiceLocator.sneakerRepository.inventory.first() }
                val shoe = before.first { it.id == sneakerId }
                if (shoe.canUpgrade) {
                    compose.onNodeWithText(localized.getString(R.string.sneaker_action_enhance))
                        .performClick()
                    compose.onNodeWithText(localized.getString(
                        R.string.items_upgrade_cost, "%,.0f".format(shoe.upgradeCost),
                    )).assertIsDisplayed()
                    capture("extra-sneaker-upgrade-confirm")
                    compose.onNodeWithText(localized.getString(R.string.common_cancel)).performClick()
                    org.junit.Assert.assertEquals(before,
                        runBlocking { ServiceLocator.sneakerRepository.inventory.first() })
                    compose.onNodeWithTag("detail-primary-action").assertIsDisplayed()
                }
            }
        }
        val variations = listOf(
            Triple(0, "home-details", listOf(R.string.common_more)),
            Triple(2, "community-crews", listOf(R.string.community_tab_my_crew)),
            Triple(2, "community-stories", listOf(R.string.community_stories)),
            Triple(2, "community-meetups", listOf(R.string.community_other_meetups)),
            Triple(4, "challenge-weekly", listOf(R.string.challenge_tag_weekly)),
            Triple(4, "challenge-night", listOf(R.string.event_night_quest)),
            Triple(3, "items-store", listOf(R.string.market_tab_store)),
            Triple(3, "items-exchange", listOf(R.string.market_tab_nft)),
            Triple(3, "items-my-trades", listOf(R.string.market_tab_nft, R.string.market_section_mine)),
            Triple(3, "items-filter", listOf(R.string.filter_button_detail)),
            Triple(5, "profile-settings", listOf(R.string.profile_tab_settings)),
            Triple(5, "profile-edit", listOf(R.string.profile_tab_settings, R.string.profile_edit_profile)),
            Triple(5, "profile-goal", listOf(R.string.profile_tab_settings, R.string.profile_set_goal)),
            Triple(7, "courses-create", listOf(R.string.courses_make)),
            Triple(7, "courses-board", listOf(R.string.courses_board)),
            Triple(9, "analytics-quarter", listOf(R.string.analytics_tab_quarter)),
            Triple(17, "ranking-time", listOf(R.string.rank_board_time)),
            Triple(17, "ranking-speed", listOf(R.string.rank_board_speed)),
            Triple(17, "ranking-factions", listOf(R.string.rank_board_faction)),
            Triple(17, "ranking-sup", listOf(R.string.rank_board_sup)),
            Triple(17, "ranking-crews", listOf(R.string.rank_board_crew)),
            Triple(19, "compose-free", listOf(R.string.post_cat_free)),
            Triple(19, "compose-tip", listOf(R.string.post_cat_tip)),
            Triple(27, "news-health", listOf(R.string.news_tab_health)),
            Triple(28, "customize-shoes", listOf(R.string.customize_tab_shoes)),
            Triple(28, "customize-options", listOf(R.string.common_more)),
            Triple(29, "market-outfits", listOf(R.string.customize_tab_outfit)),
            Triple(29, "market-shoes", listOf(R.string.customize_tab_shoes)),
            Triple(30, "map-territory", listOf(R.string.map_seg_territory)),
            Triple(31, "history-month", listOf(R.string.history_period_month)),
            Triple(31, "history-all", listOf(R.string.history_period_all)),
        )
        for ((index, name, actions) in variations) {
            try {
                reset(index)
                actions.forEach { tap(it) }
                capture("extra-$name")
                // The next keyed scene disposes any open dialog without saving it.
            } catch (error: Throwable) {
                failures.add("$name: ${error.message}")
                File(directory, "capture-notes.txt").writeText(failures.joinToString("\n"))
            }
        }
        reset(28)
        runBlocking { ServiceLocator.avatarRepository.setGender(AvatarGender.FEMALE) }
        Thread.sleep(650)
        capture("extra-customize-female")
        reset(37)
        capture("navigation-home")
        for ((id, name) in listOf(R.string.tab_customize to "customize", R.string.tab_community to "community", R.string.tab_me to "profile")) {
            try { tap(id); capture("navigation-$name") } catch (error: Throwable) { failures.add("navigation-$name: ${error.message}") }
        }
        reset(39)
        capture("launch-logo-reveal")
        reset(40)
        capture("launch-preparation-error")
        reset(38)
        for (index in GuideTour.steps.indices) {
            try {
                capture("guide-${index.toString().padStart(2, '0')}")
                if (index < GuideTour.steps.lastIndex) tap(R.string.guide_next)
            } catch (error: Throwable) { failures.add("guide-$index: ${error.message}"); break }
        }
        File(directory, "capture-notes.txt").writeText(failures.joinToString("\n"))
        org.junit.Assert.assertTrue("Missing gallery states:\n${failures.joinToString("\n")}", failures.isEmpty())
    }

    private fun tap(resource: Int) {
        val label = localized.getString(resource)
        var matches = compose.onAllNodes(hasText(label) and hasClickAction())
        if (matches.fetchSemanticsNodes().isEmpty()) matches = compose.onAllNodes(hasContentDescription(label) and hasClickAction())
        if (matches.fetchSemanticsNodes().isEmpty()) matches = compose.onAllNodesWithText(label)
        if (matches.fetchSemanticsNodes().isEmpty()) {
            compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText(label))
            matches = compose.onAllNodesWithText(label)
        }
        val target = matches.onFirst()
        try { target.performScrollTo() } catch (_: Throwable) { }
        target.performClick()
        compose.waitForIdle()
        Thread.sleep(500)
    }

    private fun capture(name: String) {
        android.util.Log.i("ScreenGallery", "Capturing $name")
        compose.waitForIdle()
        captureDisplay(File(directory, "$name.png"))
    }

    @Composable private fun Scene(index: Int) {
        when (index) {
            0 -> MainScaffold()
            1, 34, 35 -> MainScaffold(initialRoute = com.stepup.android.ui.Routes.RUN)
            2 -> MainScaffold(initialTab = com.stepup.android.ui.Screen.Community)
            3 -> MainScaffold(initialRoute = com.stepup.android.ui.Routes.ITEMS)
            4 -> MainScaffold(initialRoute = Routes.EVENTS)
            5 -> MainScaffold(initialTab = com.stepup.android.ui.Screen.Profile)
            6 -> MainScaffold(initialRoute = Routes.WALLET)
            7 -> MainScaffold(initialRoute = com.stepup.android.ui.Routes.COURSES)
            8 -> MainScaffold(initialRoute = com.stepup.android.ui.Routes.ACHIEVEMENTS)
            9 -> MainScaffold(initialRoute = com.stepup.android.ui.Routes.ANALYTICS)
            10 -> MainScaffold(initialRoute = com.stepup.android.ui.Routes.NOTIFICATIONS)
            11 -> ExperienceSettingsScreen {}
            12 -> NotificationSettingsScreen()
            13 -> PrivacyScreen()
            14 -> SupportScreen()
            15 -> ConnectedAccountsScreen()
            16 -> LanguageScreen()
            17 -> MainScaffold(initialRoute = com.stepup.android.ui.Routes.RANKING)
            18 -> MainScaffold(initialRoute = Routes.CREW_CREATE)
            19 -> MainScaffold(initialRoute = Routes.postCompose(""))
            20 -> MainScaffold(initialRoute = com.stepup.android.ui.Routes.sneaker(sneakerId))
            21 -> MainScaffold(initialRoute = com.stepup.android.ui.Routes.crewBoard(crewId))
            22 -> MainScaffold(initialRoute = com.stepup.android.ui.Routes.flashDetail(101L))
            23 -> PartyLobbyScreen(crewId, onBack = {}, onRunStarted = {})
            24 -> LoginScreen {}
            25 -> ThemeScreen()
            26 -> MainScaffold(initialRoute = com.stepup.android.ui.Routes.SNEAKER_DEX)
            27 -> NewsScreen()
            28 -> MainScaffold(initialTab = com.stepup.android.ui.Screen.Customize)
            29 -> RunnerMarketScreen()
            30 -> MainScaffold(initialRoute = com.stepup.android.ui.Routes.MAP)
            31 -> MainScaffold(initialRoute = com.stepup.android.ui.Routes.HISTORY_MAP)
            32 -> MainScaffold(initialRoute = com.stepup.android.ui.Routes.marketModel("FIRE", "COMMON", 1))
            33 -> com.stepup.android.ui.screens.splash.LaunchScene(com.stepup.android.ui.screens.splash.LaunchStage.Loading)
            36 -> PartyLobbyScreen(flashPostId = 101L, onBack = {}, onRunStarted = {})
            37 -> MainScaffold()
            38 -> MainScaffold(startTour = true)
            39 -> com.stepup.android.ui.screens.splash.LaunchScene(com.stepup.android.ui.screens.splash.LaunchStage.Reveal)
            40 -> com.stepup.android.ui.screens.splash.LaunchScene(com.stepup.android.ui.screens.splash.LaunchStage.Error)
        }
    }
}
