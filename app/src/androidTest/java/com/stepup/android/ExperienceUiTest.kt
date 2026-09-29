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
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import com.stepup.android.ui.BOTTOM_NAV_TAG
import com.stepup.android.ui.MainScaffold
import com.stepup.android.core.ServiceLocator
import com.stepup.android.ui.components.VoltButton
import com.stepup.android.ui.components.animatedInt
import com.stepup.android.ui.experience.*
import com.stepup.android.ui.screens.home.HomeScreen
import com.stepup.android.ui.screens.walk.*
import com.stepup.android.ui.screens.community.*
import com.stepup.android.ui.screens.items.*
import com.stepup.android.ui.screens.events.EventsScreen
import com.stepup.android.ui.screens.profile.*
import com.stepup.android.ui.screens.rewards.WalletScreen
import com.stepup.android.ui.screens.notifications.NotificationsScreen
import com.stepup.android.ui.screens.settings.*
import com.stepup.android.ui.screens.login.LoginScreen
import com.stepup.android.ui.theme.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.After
import com.stepup.android.ui.Routes
import com.stepup.android.ui.Screen
import com.stepup.android.ui.guide.GuideTour
import com.stepup.android.service.WalkSessionService
import com.stepup.android.service.WalkSessionState
import com.stepup.android.ui.screens.customize.CustomizeScreen
import com.stepup.android.ui.screens.gacha.MysteryBoxScreen
import com.stepup.android.domain.DrawKind
import com.stepup.android.domain.DrawStatus
import com.stepup.android.ui.screens.gacha.DrawActions
import com.stepup.android.ui.screens.gacha.DrawScreenState
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real Compose/Android rendering. Captures are review artifacts, not mock HTML screens. */
@RunWith(AndroidJUnit4::class)
class ExperienceUiTest {
    @get:Rule(order = 0) val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.ACTIVITY_RECOGNITION,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.POST_NOTIFICATIONS,
    )
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    private var sneakerId = 0L
    private var crewId = ""
    private var postId = 0L

    @Before fun prepare() = runBlocking {
        ServiceLocator.userPrefs.setReducedMotion(true)
        ServiceLocator.userPrefs.setSounds(false)
        ServiceLocator.userPrefs.setHaptics(false)
        ServiceLocator.userPrefs.setLoginMethod("guest")
        ServiceLocator.userPrefs.setGuideSeen()
        GuideTour.stop()
        WalkSessionService.showStateForTest(WalkSessionState())
        ServiceLocator.userPrefs.ensureRunnerUid()
        ServiceLocator.sneakerRepository.ensureStarter()
        TestData.seedCommunity()
        ServiceLocator.courseRepository.ensureSeeded()
        TestData.seedWelcomeNotification()
        sneakerId = ServiceLocator.sneakerRepository.inventory.first().first().id
        crewId = ServiceLocator.crewRepository.crews.value.first().id
        postId = ServiceLocator.communityRepository.posts.value.first().id
    }

    @After fun restorePresentation() {
        GuideTour.stop()
        WalkSessionService.showStateForTest(WalkSessionState())
    }

    @Test fun allModulesRenderInFourLanguagesAndLargeText() {
        // Exercise realistic five-digit figures; a dashboard containing only zeros can hide clipping.
        ServiceLocator.stepRepository.startTracking()
        ServiceLocator.stepRepository.simulateSteps((12840 - ServiceLocator.stepRepository.todaySteps.value).coerceAtLeast(0))
        var screen by mutableIntStateOf(0)
        var language by mutableStateOf("ko")
        var large by mutableStateOf(false)
        var dark by mutableStateOf(false)
        compose.setContent {
            val base = LocalContext.current
            val config = Configuration(LocalConfiguration.current).apply {
                setLocales(LocaleList(Locale.forLanguageTag(language)))
                fontScale = if (large) 1.6f else 1f
                screenWidthDp = if (large) 320 else 360
                screenHeightDp = if (large) 568 else 680
            }
            val localized = remember(language, large) { base.createConfigurationContext(config) }
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides compose.activity,
                LocalOnBackPressedDispatcherOwner provides compose.activity,
                LocalContext provides localized, LocalConfiguration provides config,
                LocalDensity provides Density(LocalDensity.current.density, config.fontScale)) {
                StepUpTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
                    ExperienceProvider {
                        Box(Modifier.requiredSize(config.screenWidthDp.dp, config.screenHeightDp.dp)
                            .background(Night).testTag("capture")) {
                            key(screen, language, large, dark) { Scene(screen) }
                        }
                    }
                }
            }
        }
        for (locale in listOf("ko", "en", "ja", "zh")) {
            for (night in listOf(false, true)) {
                for (enlarged in listOf(false, true)) {
                    for (index in 0..30) {
                        compose.runOnIdle { language = locale; dark = night; large = enlarged; screen = index }
                        compose.waitForIdle()
                        if (index == 0) compose.waitUntil(5_000) {
                            compose.onAllNodesWithText("12,840", substring = true).fetchSemanticsNodes().isNotEmpty()
                        }
                        val name = "$locale-${if (night) "dark" else "light"}-${if (enlarged) "large" else "normal"}-${index.toString().padStart(2, '0')}"
                        capture(name)
                        // Exercise content below the initial viewport, including profile
                        // statistics, settings, run metrics and form submit controls.
                        if (locale == "ko" && enlarged) {
                            repeat(5) { page ->
                                val scrolls = compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes()
                                    .filter { it.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange) != null }
                                if (scrolls.isNotEmpty()) {
                                    val node = scrolls.first()
                                    val range = node.config[SemanticsProperties.VerticalScrollAxisRange]
                                    if (range.value() < range.maxValue()) {
                                        compose.onNode(SemanticsMatcher("scroll container") { it.id == node.id })
                                            .performTouchInput { swipeUp(durationMillis = 350) }
                                        compose.waitForIdle()
                                        capture("$name-page${page + 1}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable private fun Scene(index: Int) {
        when (index) {
            0 -> MainScaffold()
            1 -> MainScaffold(initialRoute = Routes.RUN)
            2 -> MainScaffold(initialTab = Screen.Community)
            3 -> ItemsScreen()
            4 -> MainScaffold(initialRoute = Routes.EVENTS)
            5 -> MainScaffold(initialTab = Screen.Profile)
            6 -> WalletScreen()
            7 -> CourseHubScreen()
            8 -> AchievementsScreen()
            9 -> AnalyticsScreen()
            10 -> NotificationsScreen()
            11 -> ExperienceSettingsScreen {}
            12 -> NotificationSettingsScreen()
            13 -> PrivacyScreen()
            14 -> SupportScreen()
            15 -> ConnectedAccountsScreen()
            16 -> LanguageScreen()
            17 -> RankingScreen()
            18 -> MainScaffold(initialRoute = Routes.CREW_CREATE)
            19 -> PostComposeScreen()
            20 -> SneakerDetailScreen(sneakerId)
            21 -> CrewBoardScreen(crewId)
            22 -> FlashRunDetailScreen(postId)
            23 -> PartyLobbyScreen(crewId, onBack = {}, onRunStarted = {})
            24 -> LoginScreen {}
            25 -> ThemeScreen()
            26 -> SneakerDexScreen()
            27 -> com.stepup.android.ui.screens.events.NewsScreen()
            28 -> MainScaffold(initialTab = Screen.Customize)
            29 -> MainScaffold(initialRoute = Routes.RUNNER_MARKET)
            30 -> MainScaffold(initialRoute = Routes.MYSTERY_BOX)
        }
    }

    @Test fun experienceSwitchesPersistAndSurviveRecomposition() {
        var visible by mutableStateOf(true)
        compose.setContent { StepUpTheme { ExperienceProvider { if (visible) ExperienceSettingsScreen {} } } }
        compose.onNodeWithText(compose.activity.getString(R.string.experience_sound)).performClick()
        // 설정은 DataStore 파일에 쓰인 뒤에야 읽힌다. 에뮬레이터에서는 첫 쓰기가 1초를
        // 넘길 때가 있어(기본 대기 1초), 다른 대기와 같이 5초를 준다.
        compose.waitUntil(5_000) { runBlocking { ServiceLocator.userPrefs.experience.first().sounds } }
        compose.onNodeWithText(compose.activity.getString(R.string.experience_motion)).performClick()
        compose.waitUntil(5_000) { runBlocking { !ServiceLocator.userPrefs.experience.first().reducedMotion } }
        compose.runOnIdle { visible = false }
        compose.runOnIdle { visible = true }
        compose.onNode(hasText(compose.activity.getString(R.string.experience_sound)) and isToggleable()).assertIsOn()
        compose.onNode(hasText(compose.activity.getString(R.string.experience_motion)) and isToggleable()).assertIsOff()
    }

    @Test fun disabledControlsCannotFireAndPressAnimationSettles() {
        var clicks = 0
        var enabled by mutableStateOf(false)
        compose.setContent {
            StepUpTheme { CompositionLocalProvider(LocalMotion provides MotionPreferences()) {
                VoltButton("Confirm", { clicks++ }, enabled = enabled)
            } }
        }
        compose.onNodeWithText("Confirm").assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle { assertEquals(0, clicks); enabled = true }
        compose.onNodeWithText("Confirm").performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test fun reducedMotionShowsFinalMetricWithoutInterpolation() {
        var target by mutableIntStateOf(0)
        compose.setContent { StepUpTheme {
            CompositionLocalProvider(LocalMotion provides MotionPreferences(reduced = true)) {
                androidx.compose.material3.Text("${animatedInt(target)}")
            }
        } }
        compose.onNodeWithText("0").assertExists()
        compose.runOnIdle { target = 8000 }
        compose.onNodeWithText("8000").assertExists()
    }

    @Test fun everyBundledCueDecodesWithAndroidSoundPool() {
        val loaded = CountDownLatch(FeedbackCue.entries.size)
        val failures = java.util.concurrent.CopyOnWriteArrayList<Int>()
        lateinit var pool: android.media.SoundPool
        compose.runOnUiThread {
            pool = android.media.SoundPool.Builder().setMaxStreams(2).build()
            pool.setOnLoadCompleteListener { _, id, status ->
                if (status != 0) failures.add(id)
                loaded.countDown()
            }
            FeedbackCue.entries.forEach { pool.load(compose.activity, it.sound, 1) }
        }
        try {
            assertTrue("All bundled cues should load", loaded.await(15, TimeUnit.SECONDS))
            assertTrue("Decoder failures: $failures", failures.isEmpty())
        } finally { compose.runOnUiThread { pool.release() } }
    }

    @Test fun mainNavigationAndSettingsAreReachable() {
        runBlocking { ServiceLocator.userPrefs.setReducedMotion(false) }
        compose.setContent { StepUpTheme { ExperienceProvider {
            Box(Modifier.background(Night).testTag("capture")) { MainScaffold() }
        } } }
        // Five tab roles — 러닝 / 신발 / 뽑기 / 커뮤니티 / 내 정보(신발 화면 확정안 2026-09-28). 뽑기는 가운데 독립 탭.
        val tabs = listOf(R.string.tab_run, R.string.tab_customize, R.string.tab_draw, R.string.tab_community, R.string.tab_me)
        // 프로필 화면 안에도 "Profile" 탭이 있으므로 하단 탭 줄 안의 탭만 센다.
        val tabRole = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab) and
            hasAnyAncestor(hasTestTag(BOTTOM_NAV_TAG))
        compose.waitUntil(15_000) { compose.onAllNodes(tabRole).fetchSemanticsNodes().size == 5 }
        tabs.forEachIndexed { index, title ->
            val node = compose.onNode(hasText(compose.activity.getString(title)) and tabRole)
            node.performClick().assertIsSelected()
            capture("navigation-$index")
        }
        // 뽑기 탭 — 공통 머리 · 하단 탭 아래의 무료 · 상급 글자 탭(뒤로 버튼 없음). 신발 탭 위에는 뽑기가 없다(내 신발 · 신발 보관함)
        compose.onNode(hasText(compose.activity.getString(R.string.tab_draw)) and tabRole).performClick().assertIsSelected()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("draw-home").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("draw-home").assertIsDisplayed()
        compose.onNodeWithTag(BOTTOM_NAV_TAG).assertIsDisplayed()
        compose.onNodeWithTag("main-header").assertIsDisplayed()
        capture("navigation-draw")
        compose.onNode(hasText(compose.activity.getString(R.string.tab_customize)) and tabRole).performClick().assertIsSelected()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("shoes-section-mine").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("draw-home").assertDoesNotExist()
        compose.onNodeWithTag("shoes-section-mine").assertIsSelected()
        compose.onNodeWithTag("shoes-section-vault").assertIsNotSelected()
        compose.onNodeWithTag("shoes-section-draw").assertDoesNotExist()
        compose.onNode(hasText(compose.activity.getString(R.string.tab_me)) and tabRole).performClick().assertIsSelected()
        compose.onNodeWithTag("draw-home").assertDoesNotExist()
        compose.onNodeWithTag("profile-settings").performClick()
        val settingsLabel = compose.activity.getString(R.string.set_experience)
        compose.onAllNodes(hasScrollAction())[0].performScrollToNode(hasText(settingsLabel))
        compose.onNodeWithText(settingsLabel).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.experience_sound)).assertIsDisplayed()
        capture("navigation-experience")
    }

    /**
     * 내 신발(신발 화면 확정안 2026-09-28) — 보유 목록의 칸 고르기와 관리(⋯ → 신발 상세)를 여는 것은 미리 보기이고,
     * 착용은 상세의 "이 신발 신기"를 눌러 저장이 끝난 뒤에만 바뀐다. 같은 모델의 다른 켤레도 자기 칸(소유 id)으로 고른다.
     */
    @Test fun shoePreviewOnlyEquipsAfterConfirmation() {
        val dao = ServiceLocator.database.sneakerDao()
        val original = runBlocking { requireNotNull(dao.equippedNow()) }
        // 신고 있는 켤레와 같은 모델의 다른 켤레 — 목록에서 따로 한 칸(소유 id)
        val candidateId = runBlocking {
            dao.insert(original.copy(id = 0, mintNumber = dao.maxMintNumber() + 1,
                equipped = false, acquiredAt = System.currentTimeMillis(), serverId = 0))
        }
        var detailId by mutableStateOf<Long?>(null)
        try {
            compose.setContent { StepUpTheme { ExperienceProvider {
                val opened = detailId
                if (opened == null) CustomizeScreen(onOpenSneaker = { detailId = it })
                else SneakerDetailScreen(opened, onBack = { detailId = null })
            } } }
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("shoe-owned-row").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("shoe-owned-row").performScrollToNode(hasTestTag("shoe-choice-$candidateId"))
            compose.onNodeWithTag("shoe-choice-$candidateId").performClick().assertIsSelected()
            assertEquals("Picking a pair must not change the stored equipment", original.id,
                runBlocking { dao.equippedNow()?.id })
            compose.onNodeWithTag("shoe-choice-${original.id}").assertIsNotSelected()
            compose.onNodeWithTag("shoe-manage").performClick()
            compose.runOnIdle { assertEquals(candidateId, detailId) }
            // 상세 — 아직 신지 않은 켤레라 주 버튼이 살아 있다(읽는 동안은 "확인 중…"으로 막혀 있다). 여는 것만으로 착용은 그대로
            compose.waitUntil(5_000) {
                runCatching { compose.onNodeWithTag("detail-primary-action").assertIsEnabled() }.isSuccess
            }
            assertEquals("Preview must not change the stored equipment", original.id,
                runBlocking { dao.equippedNow()?.id })
            compose.onNodeWithTag("detail-primary-action").performClick()
            compose.waitUntil(5_000) { runBlocking { dao.equippedNow()?.id == candidateId } }
            compose.waitUntil(5_000) {
                runCatching { compose.onNodeWithTag("detail-primary-action").assertIsNotEnabled() }.isSuccess
            }
            // 뒤로 — 착용 체크가 같은 id 로 옮겨 가 있다(보는 켤레도 그대로 — "착용 중")
            compose.runOnIdle { detailId = null }
            compose.waitUntil(5_000) {
                compose.onAllNodes(hasTestTag("shoe-worn-badge") and hasAnyAncestor(hasTestTag("shoe-choice-$candidateId")),
                    useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("shoe-hero-meta", useUnmergedTree = true)
                .assertTextContains(compose.activity.getString(R.string.my_shoes_wearing), substring = true)
        } finally {
            // Remove only this test's new copy and restore the previous pair.
            runBlocking {
                ServiceLocator.sneakerRepository.equip(original.id)
                dao.byId(candidateId)?.let { dao.delete(it) }
            }
        }
    }

    @Test fun shoeDrawRespectsReadinessAndTabs() {
        // 뽑기 디자인 26장(2026-09-28) — 가격 · 결제 확인이 없고, 위 글자 탭(무료 · 상급)으로 고른 탭의 버튼 하나만 있다
        var state by mutableStateOf<DrawScreenState>(DrawScreenState.SignedOut)
        var canSignIn by mutableStateOf(false)
        val draws = mutableListOf<DrawKind>()
        var connects = 0
        var signIns = 0
        compose.setContent { StepUpTheme { ExperienceProvider {
            MysteryBoxScreen(
                state = state,
                actions = DrawActions(
                    onDraw = { draws += it }, onConnectWallet = { connects++ },
                    onSignIn = if (canSignIn) ({ signIns++ }) else null,
                ),
            )
        } } }
        // 로그인 전 — 수는 "—"(0 이 아니다). 로그인할 수 없는 빌드면 버튼을 누를 수 없다
        compose.onNodeWithTag("draw-tab-free").assertIsSelected()
        compose.onNodeWithTag("draw-free-action").assertIsNotEnabled().performTouchInput { click() }
        compose.onNodeWithTag("draw-left-unknown", useUnmergedTree = true).assertExists()
        compose.onAllNodesWithText("0회").assertCountEquals(0)
        compose.onNodeWithTag("draw-info").assertDoesNotExist()
        compose.runOnIdle { assertTrue(draws.isEmpty()) }
        compose.onNodeWithText(compose.activity.getString(R.string.mystery_draw_outfit)).assertDoesNotExist()
        compose.onNodeWithTag("draw-tab-premium").performClick().assertIsSelected()
        compose.onNodeWithTag("draw-free-action").assertDoesNotExist()
        compose.onNodeWithTag("draw-premium-action").assertIsNotEnabled()
        // 로그인할 수 있으면 로그인 화면으로 — 뽑지 않는다
        compose.runOnIdle { canSignIn = true }
        compose.onNodeWithTag("draw-premium-action").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, signIns); assertTrue(draws.isEmpty()) }
        val fresh = DrawStatus(dailyLeft = 3, dailyTotal = 3, signupLeft = 10, signupGranted = 10, walletLinked = false,
            giftOnLink = 10, giftLeft = 0, runLeft = 0, genesisLeft = 0, runProgressMeters = 0.0, runStepMeters = 1000,
            runToday = 0, runDailyCap = 10, chainPaused = false)
        compose.runOnIdle { state = DrawScreenState.Ready(fresh) }
        compose.onNodeWithTag("draw-tab-free").performClick().assertIsSelected()
        compose.onNodeWithTag("draw-free-action").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(listOf(DrawKind.FREE), draws) }
        // 상급 탭 — 지갑 전이면 버튼은 연결 혜택(10), 그 안의 "WEB3 지갑 연결하기"가 지갑 페이지다. 연결만으로 뽑지 않는다
        compose.onNodeWithTag("draw-tab-premium").performClick().assertIsSelected()
        compose.onNodeWithTag("draw-chip-wallet", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("draw-premium-action").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("draw-sheet-connect").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("draw-sheet-connect").performClick()
        compose.runOnIdle { assertEquals(1, connects); assertEquals(listOf(DrawKind.FREE), draws) }
        compose.runOnIdle { state = DrawScreenState.Ready(fresh.copy(walletLinked = true, giftOnLink = 0, giftLeft = 10, genesisLeft = 1)) }
        compose.onNodeWithTag("draw-premium-action").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(listOf(DrawKind.FREE, DrawKind.PREMIUM), draws) }
        // 상자 무대는 누르는 곳이 아니다
        compose.onNodeWithTag("draw-premium").assertHasNoClickAction()
        compose.onNodeWithTag("draw-tab-free").performClick()
        compose.onNodeWithTag("draw-free").assertHasNoClickAction()
    }

    /**
     * 시작·로그인·첫 사용 v1 — 첫 안내는 한 장(시안 02). 탭을 옮겨 다니지 않고 러닝 · 신발 · 내 정보를 한 번에 설명한다.
     * "러닝 시작"은 러닝 방법 고르기로 갈 뿐 러닝을 시작하지 않고, 홈에 돌아와도 안내가 다시 뜨지 않는다.
     */
    @Test fun firstGuideIsOneSheetAndOpensRunMenu() {
        // 멈춘 러닝이 남아 있으면 그것부터 묻고 첫 안내는 미룬다 — 앞 테스트의 저장본을 비운다
        clearAnyRunCheckpointForTest()
        compose.setContent { StepUpTheme { ExperienceProvider { MainScaffold(startTour = true) } } }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("first-guide-sheet").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(compose.activity.getString(R.string.onb_guide_title)).assertIsDisplayed()
        for (route in listOf("home", "customize", "profile")) compose.onNodeWithTag("guide-row-$route").assertExists()
        // 안내 줄은 설명이다 — 누를 곳이 아니다
        compose.onNodeWithTag("guide-row-customize").assertHasNoClickAction()
        assertFalse(GuideTour.active)
        compose.onNodeWithTag("first-guide-start").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("run-menu-free", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("first-guide-sheet").assertDoesNotExist()
        assertFalse("the guide opens the run menu without starting a run", WalkSessionService.state.value.isActive)
        // 메뉴에서 뒤로 — 러닝 홈. 이미 고른 안내는 다시 뜨지 않는다
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("home-start-run").fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
        compose.onNodeWithTag("first-guide-sheet").assertDoesNotExist()
    }

    /** 첫 안내를 X 로 닫아도 "먼저 둘러보기"와 같다 — 홈에 머물고 다시 열리지 않는다 */
    @Test fun firstGuideCloseStaysHome() {
        clearAnyRunCheckpointForTest()
        compose.setContent { StepUpTheme { ExperienceProvider { MainScaffold(startTour = true) } } }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("first-guide-sheet").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("onboarding-sheet-close").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("first-guide-sheet").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("home-start-run").assertIsDisplayed()
        compose.onNodeWithTag("run-menu").assertDoesNotExist()
    }

    private fun capture(name: String) {
        auditText(name)
        val bitmap = compose.onNodeWithTag("capture").captureToImage().asAndroidBitmap()
        assertTrue(bitmap.width > 300 && bitmap.height > 500)
        val directory = File(compose.activity.getExternalFilesDir(null), "experience-qa").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    /** Log measured overflow and intersecting text bounds, not just screenshots. */
    private fun auditText(scene: String) {
        val nodes = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
            .fetchSemanticsNodes().filter { it.boundsInRoot.width > 1f && it.boundsInRoot.height > 1f }
        val findings = mutableListOf<JSONObject>()
        compose.runOnIdle {
            nodes.forEach { node ->
                val layouts = mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
                layouts.forEach { result ->
                    if (result.hasVisualOverflow) {
                        findings += JSONObject().put("scene", scene)
                            .put("kind", if (result.layoutInput.overflow == TextOverflow.Ellipsis) "ellipsis" else "overflow")
                            .put("text", result.layoutInput.text.text)
                            .put("fontSp", result.layoutInput.style.fontSize.value)
                            .put("width", result.size.width).put("height", result.size.height)
                    }
                }
            }
            nodes.forEachIndexed { i, a ->
                nodes.drop(i + 1).forEach { b ->
                    val x = a.boundsInRoot.intersect(b.boundsInRoot)
                    if (x.width > 2f && x.height > 2f) {
                        findings += JSONObject().put("scene", scene).put("kind", "overlap")
                            .put("text", a.config.getOrNull(SemanticsProperties.Text).toString())
                            .put("other", b.config.getOrNull(SemanticsProperties.Text).toString())
                            .put("intersection", x.toString())
                    }
                }
            }
        }
        val directory = File(compose.activity.getExternalFilesDir(null), "experience-qa").apply { mkdirs() }
        File(directory, "layout-audit.jsonl").appendText(findings.joinToString("") { it.toString() + "\n" })
        File(directory, "layout-scenes.txt").appendText(scene + "\n")
    }

}
