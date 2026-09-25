package com.stepup.android

import android.content.res.Configuration
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import com.stepup.android.domain.Comment
import com.stepup.android.domain.CommentThread
import com.stepup.android.data.repo.BoardSyncState
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
import com.stepup.android.ui.screens.map.MapViewModel
import com.stepup.android.ui.screens.map.TerritoryState
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
    @get:Rule(order = 0) val appLanguage = object : org.junit.rules.ExternalResource() {
        private var previous = ""
        override fun before() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val context = instrumentation.targetContext
            com.stepup.android.core.AppLocale.syncFromSystem(context)
            previous = com.stepup.android.core.AppLocale.tag
            // Apply before the Activity exists: Dialog windows must receive the
            // same application locale as their parent, not a Compose-only override.
            instrumentation.runOnMainSync {
                com.stepup.android.core.AppLocale.change(context, "ko")
            }
        }
        override fun after() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            instrumentation.runOnMainSync {
                com.stepup.android.core.AppLocale.change(instrumentation.targetContext, previous)
            }
        }
    }
    @get:Rule(order = 1) val permissions = GrantPermissionRule.grant(
        android.Manifest.permission.ACTIVITY_RECOGNITION,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.POST_NOTIFICATIONS,
    )
    @get:Rule(order = 2) val compose = createAndroidComposeRule<ComponentActivity>()
    private var sneakerId = 0L
    private var crewId = ""
    private lateinit var localized: android.content.Context
    private val directory get() = File(compose.activity.getExternalFilesDir(null), "screen-gallery").apply { mkdirs() }
    private val failures = mutableListOf<String>()

    /** Production shell at the device's actual size; no white fixed-size gallery frame. */
    @Test fun wardrobeDesign() {
        runBlocking {
            ServiceLocator.userPrefs.setReducedMotion(true)
            ServiceLocator.userPrefs.setSounds(false)
            ServiceLocator.userPrefs.setHaptics(false)
            ServiceLocator.userPrefs.setGuideSeen()
            ServiceLocator.sneakerRepository.ensureStarter()
            ServiceLocator.avatarRepository.setDemoMode(false)
            ServiceLocator.avatarRepository.equipOutfit(com.stepup.android.domain.Outfits.DEFAULT)
            ServiceLocator.avatarRepository.setGender(AvatarGender.FEMALE)
        }
        compose.activityRule.scenario.onActivity {
            it.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            )
        }
        var largeType by mutableStateOf(false)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (largeType) 1.3f else 1f)) {
                StepUpTheme(ThemeMode.DARK) {
                    ExperienceProvider { MainScaffold() }
                }
            }
        }
        compose.waitUntil(10_000) { compose.onNodeWithTag("home-character-ready").isDisplayed() }
        compose.onNodeWithText(compose.activity.getString(R.string.tab_customize)).performClick()
        fun ready(gender: AvatarGender, outfitId: String = com.stepup.android.domain.Outfits.BASE_ID) {
            compose.waitUntil(10_000) {
                compose.onNodeWithTag("wardrobe-look-${gender.id}-$outfitId").isDisplayed()
            }
            compose.waitForIdle()
        }
        fun shot(name: String) {
            compose.mainClock.advanceTimeBy(400)
            compose.waitForIdle()
            // Semantics can be ready before SurfaceFlinger presents that frame.
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            // Let the native short equip toast finish before recording the inventory.
            Thread.sleep(2400)
            captureDisplay(File(directory, "$name.png"))
        }
        ready(AvatarGender.FEMALE)
        shot("wardrobe-01-owned")
        val initialBounds = compose.onNodeWithTag("wardrobe-look-${AvatarGender.FEMALE.id}-${com.stepup.android.domain.Outfits.BASE_ID}")
            .fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("wardrobe-background").performClick()
        compose.onNodeWithTag("wardrobe-scene-Wardrobe").assertDoesNotExist()
        org.junit.Assert.assertEquals(initialBounds, compose.onNodeWithTag("wardrobe-look-${AvatarGender.FEMALE.id}-${com.stepup.android.domain.Outfits.BASE_ID}")
            .fetchSemanticsNode().boundsInRoot)
        shot("wardrobe-02-background")
        compose.onNodeWithText(compose.activity.getString(R.string.customize_tab_shoes)).performClick().assertIsSelected()
        shot("wardrobe-03-shoes")
        compose.onNodeWithText(compose.activity.getString(R.string.customize_tab_outfit)).performClick()
        runBlocking { ServiceLocator.avatarRepository.setDemoMode(true) }
        compose.waitUntil(10_000) { compose.onAllNodesWithText(compose.activity.getString(R.string.customize_trial_badge)).fetchSemanticsNodes().isNotEmpty() }
        shot("wardrobe-04-demo-grid")
        compose.onNodeWithTag("wardrobe-options").performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.customize_open_market)).assertIsDisplayed()
        shot("wardrobe-05-options")
        InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        compose.waitForIdle()
        runBlocking { ServiceLocator.avatarRepository.setGender(AvatarGender.MALE) }
        ready(AvatarGender.MALE)
        shot("wardrobe-06-runo")
        compose.runOnIdle { largeType = true }
        compose.onNodeWithTag("wardrobe-background").assertIsDisplayed()
        shot("wardrobe-07-large-type")
        compose.runOnIdle { largeType = false }
        runBlocking { ServiceLocator.avatarRepository.setGender(AvatarGender.FEMALE) }
        ready(AvatarGender.FEMALE)
        for (outfit in com.stepup.android.domain.Outfits.STUDIO) {
            val label = compose.activity.getString(com.stepup.android.ui.components.outfitNameRes(outfit))
            compose.onNodeWithContentDescription(label, substring = true).performScrollTo().performClick()
            compose.onNodeWithTag("wardrobe-equip").performClick()
            ready(AvatarGender.FEMALE, outfit.id)
            val baseLabel = compose.activity.getString(R.string.outfit_starter_hoodie)
            compose.onNodeWithContentDescription(baseLabel, substring = true).performScrollTo()
            shot("stage2-lumi-${outfit.id.lowercase()}")
            if (outfit == com.stepup.android.domain.Outfits.SOFT_PINK) {
                compose.onNodeWithTag("wardrobe-background").performClick()
                shot("stage2-lumi-pink-background")
            }
            runBlocking { ServiceLocator.avatarRepository.setGender(AvatarGender.MALE) }
            ready(AvatarGender.MALE, outfit.id)
            shot("stage2-runo-${outfit.id.lowercase()}")
            runBlocking { ServiceLocator.avatarRepository.setGender(AvatarGender.FEMALE) }
            ready(AvatarGender.FEMALE, outfit.id)
        }
        runBlocking { ServiceLocator.avatarRepository.setDemoMode(false) }
    }

    @Test fun allScreens() {
        // The full gallery decodes many large scenes. CI runs it in independent
        // parts so the emulator can release image memory between processes.
        val galleryPart = InstrumentationRegistry.getArguments().getString("galleryPart") ?: "all"
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
                LocalDensity provides Density(LocalDensity.current.density, 1f),
            ) {
                StepUpTheme(ThemeMode.DARK) {
                    ExperienceProvider {
                        Box(Modifier.fillMaxSize().background(Night).testTag("gallery")) {
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
        val screenIndices = when (galleryPart) {
            "a" -> (0..12).toList()
            "b" -> (13..24).toList()
            "c" -> (25..36).toList()
            "d", "e", "f" -> emptyList()
            else -> (0..36).toList()
        }
        for (index in screenIndices) {
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
                    val costNode = compose.onNodeWithText(localized.getString(
                        R.string.items_upgrade_cost, "%,.0f".format(shoe.upgradeCost),
                    ))
                    // A dialog owns a separate Android window. Compose idleness alone
                    // does not establish that WindowManager has laid out that window.
                    try {
                        compose.waitUntil(timeoutMillis = 5_000) { costNode.isDisplayed() }
                    } finally {
                        capture("extra-sneaker-upgrade-confirm")
                        File(directory, "sneaker-upgrade-semantics.txt").writeText(
                            compose.onAllNodes(isRoot()).printToString(),
                        )
                    }
                    costNode.assertIsDisplayed()
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
        val selectedVariations = when (galleryPart) {
            "d" -> variations.take(16)
            "e" -> variations.drop(16)
            "a", "b", "c", "f" -> emptyList()
            else -> variations
        }
        for ((index, name, actions) in selectedVariations) {
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
        if (galleryPart == "f" || galleryPart == "all") {
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
            // The launch delay uses Compose's test clock; wall-clock sleep alone does not advance it.
            compose.mainClock.advanceTimeBy(500)
            compose.waitForIdle()
            compose.onNodeWithText(localized.getString(R.string.guide_next)).assertIsDisplayed()
            for (index in GuideTour.steps.indices) {
                try {
                    compose.onNodeWithTag("guide-step-title")
                        .assertTextEquals(localized.getString(GuideTour.steps[index].titleRes)).assertIsDisplayed()
                    compose.mainClock.advanceTimeByFrame()
                    capture("guide-${index.toString().padStart(2, '0')}")
                    if (index < GuideTour.steps.lastIndex) tap(R.string.guide_next)
                } catch (error: Throwable) { failures.add("guide-$index: ${error.message}"); break }
            }
            if (GuideTour.stepIndex == GuideTour.steps.lastIndex) {
                tap(R.string.guide_start)
                compose.onNodeWithTag("home-start-run").assertIsDisplayed()
                org.junit.Assert.assertFalse("Guide completion returns to usable home", GuideTour.active)
                capture("guide-finished-home")
            }
        }
        File(directory, "capture-notes.txt").writeText(failures.joinToString("\n"))
        org.junit.Assert.assertTrue("Missing gallery states:\n${failures.joinToString("\n")}", failures.isEmpty())
    }

    /** Nine previously unrecorded comment, report, board and territory states. */
    @Test fun edgeStates() {
        runBlocking {
            ServiceLocator.userPrefs.setReducedMotion(true)
            ServiceLocator.userPrefs.setSounds(false)
            ServiceLocator.userPrefs.setHaptics(false)
            ServiceLocator.userPrefs.setLoginMethod("guest")
            ServiceLocator.userPrefs.setGuideSeen()
            TestData.seedCommunity()
        }
        val post = ServiceLocator.communityRepository.posts.value.first { it.id == 102L }
        val parent = Comment(2001L, post.id, 0L, "Ara Kim", "u-ara", "오늘 코스 정말 좋았어요.", System.currentTimeMillis(), false)
        val reply = Comment(2002L, post.id, parent.id, "Bo Lee", "u-bo", "저도 다음에 함께 달릴게요!", System.currentTimeMillis(), false)
        val mapViewModel = MapViewModel(
            ServiceLocator.communityRepository,
            ServiceLocator.courseRepository,
            ServiceLocator.territoryApi,
        )
        var edge by mutableStateOf("comments-empty")
        compose.setContent {
            StepUpTheme(ThemeMode.DARK) {
                ExperienceProvider {
                    Box(Modifier.fillMaxSize().background(Night)) {
                        when (edge) {
                            "comments-empty", "comments-replies" -> {
                                MainScaffold(initialTab = com.stepup.android.ui.Screen.Community)
                                CommentSheet(
                                    post = post,
                                    threads = if (edge == "comments-empty") emptyList() else listOf(CommentThread(parent, listOf(reply))),
                                    focusCommentId = if (edge == "comments-replies") reply.id else 0L,
                                    onSend = { _, _ -> }, onDeleteComment = {}, onDismiss = {},
                                )
                            }
                            "report" -> {
                                MainScaffold(initialTab = com.stepup.android.ui.Screen.Community)
                                ReportDialog(
                                    authorName = post.author, canBlock = true,
                                    onDismiss = {}, onReason = {}, onBlock = {},
                                )
                            }
                            "board" -> MainScaffold(initialTab = com.stepup.android.ui.Screen.Community)
                            "map" -> MapScreen(viewModel = mapViewModel)
                        }
                    }
                }
            }
        }
        compose.onNodeWithText(compose.activity.getString(R.string.comments_empty)).assertIsDisplayed()
        capture("extra-community-comments-empty")
        compose.runOnIdle { edge = "comments-replies" }
        compose.onNodeWithText(reply.body).assertIsDisplayed()
        capture("extra-community-comments-replies")
        compose.runOnIdle { edge = "report" }
        compose.onNodeWithText(compose.activity.getString(R.string.report_title)).assertIsDisplayed()
        capture("extra-community-report-dialog")

        compose.runOnIdle { edge = "board" }
        compose.onNodeWithText(compose.activity.getString(R.string.community_stories)).performClick()
        ServiceLocator.communityRepository.showBoardStateForTest(BoardSyncState.Failed("gallery"))
        compose.onNodeWithText(compose.activity.getString(R.string.board_load_failed)).assertIsDisplayed()
        capture("extra-community-board-load-failed")
        ServiceLocator.communityRepository.showBoardStateForTest(BoardSyncState.SignInRequired)
        compose.onNodeWithText(compose.activity.getString(R.string.board_sign_in_needed)).assertIsDisplayed()
        capture("extra-community-board-sign-in")

        compose.runOnIdle { edge = "map" }
        mapViewModel.select(com.stepup.android.ui.screens.map.MapMode.TERRITORY)
        compose.waitForIdle()
        for ((name, state, message) in listOf(
            Triple("sign-in", TerritoryState.SignIn, R.string.map_territory_sign_in),
            Triple("zoom-in", TerritoryState.ZoomIn, R.string.map_territory_zoom),
            Triple("empty", TerritoryState.Ready(emptyList()), R.string.map_territory_empty),
            Triple("load-failed", TerritoryState.Failed, R.string.map_territory_failed),
        )) {
            mapViewModel.showTerritoryForTest(state)
            compose.onNodeWithText(compose.activity.getString(message)).assertIsDisplayed()
            capture("extra-map-territory-$name")
        }
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
