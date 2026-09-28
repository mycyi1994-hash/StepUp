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
            18 -> CrewCreateScreen()
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
        // Four tab roles; draw is a section inside the shoes tab.
        val tabs = listOf(R.string.tab_run, R.string.tab_customize, R.string.tab_community, R.string.tab_me)
        // 프로필 화면 안에도 "Profile" 탭이 있으므로 하단 탭 줄 안의 탭만 센다.
        val tabRole = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab) and
            hasAnyAncestor(hasTestTag(BOTTOM_NAV_TAG))
        compose.waitUntil(15_000) { compose.onAllNodes(tabRole).fetchSemanticsNodes().size == 4 }
        tabs.forEachIndexed { index, title ->
            val node = compose.onNode(hasText(compose.activity.getString(title)) and tabRole)
            node.performClick().assertIsSelected()
            capture("navigation-$index")
        }
        // S2 — no draw slot in the bar; the draw screen is a section inside the shoes tab.
        compose.onNodeWithTag("nav-draw-action").assertDoesNotExist()
        compose.onNode(hasText(compose.activity.getString(R.string.tab_customize)) and tabRole).performClick()
        compose.onNodeWithTag("shoes-section-draw").performClick().assertIsSelected()
        compose.onNodeWithTag("draw-shoe").assertIsDisplayed()
        compose.onNode(hasText(compose.activity.getString(R.string.tab_customize)) and tabRole).assertIsSelected()
        capture("navigation-draw")
        compose.onNodeWithTag("shoes-section-mine").performClick()
        compose.onNodeWithTag("draw-shoe").assertDoesNotExist()
        compose.onNodeWithTag("shoes-section-mine").assertIsSelected()
        compose.onNode(hasText(compose.activity.getString(R.string.tab_me)) and tabRole).performClick().assertIsSelected()
        compose.onNodeWithTag("draw-shoe").assertDoesNotExist()
        compose.onNodeWithTag("profile-settings").performClick()
        val settingsLabel = compose.activity.getString(R.string.set_experience)
        compose.onAllNodes(hasScrollAction())[0].performScrollToNode(hasText(settingsLabel))
        compose.onNodeWithText(settingsLabel).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.experience_sound)).assertIsDisplayed()
        capture("navigation-experience")
    }

    /**
     * 보유 신발 상세 v1 — 신발 탭에서 고르기(칸 · 같은 모델 켤레 시트)와 상세를 여는 것은 미리 보기이고,
     * 착용은 상세의 "이 신발 신기"를 눌러 저장이 끝난 뒤에만 바뀐다. 같은 모델의 다른 켤레는 소유 id 로 고른다.
     */
    @Test fun shoePreviewOnlyEquipsAfterConfirmation() {
        val dao = ServiceLocator.database.sneakerDao()
        val original = runBlocking { requireNotNull(dao.equippedNow()) }
        // 신고 있는 켤레와 같은 모델의 다른 켤레 — 한 칸에 묶이고 "2켤레 보기" 시트에서 번호로 고른다
        val candidateId = runBlocking {
            dao.insert(original.copy(id = 0, mintNumber = dao.maxMintNumber() + 1,
                equipped = false, acquiredAt = System.currentTimeMillis(), serverId = 0))
        }
        val slot = runBlocking { ServiceLocator.sneakerRepository.inventory.first().first { it.id == original.id }.slotKey }
        var detailId by mutableStateOf<Long?>(null)
        try {
            compose.setContent { StepUpTheme { ExperienceProvider {
                val opened = detailId
                if (opened == null) CustomizeScreen(onOpenSneaker = { detailId = it })
                else SneakerDetailScreen(opened, onBack = { detailId = null })
            } } }
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("shoe-owned-row").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("shoe-list").performScrollToNode(hasTestTag("shoe-owned-row"))
            compose.onNodeWithTag("shoe-owned-row").performScrollToNode(hasTestTag("shoe-copies-$slot"))
            compose.onNodeWithTag("shoe-copies-$slot").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("shoe-copy-$candidateId").fetchSemanticsNodes().isNotEmpty() }
            assertEquals("Opening the copies sheet must not change the stored equipment", original.id,
                runBlocking { dao.equippedNow()?.id })
            compose.onNodeWithTag("shoe-copy-$candidateId").performClick()
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
            // 뒤로 — 신발 탭의 체크가 같은 id 로 옮겨 가 있다(보는 켤레도 그대로)
            compose.runOnIdle { detailId = null }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithTag("shoe-kicker", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("shoe-kicker", useUnmergedTree = true)
                .assertTextEquals(compose.activity.getString(R.string.sdv_kicker_wearing))
        } finally {
            // Remove only this test's new copy and restore the previous pair.
            runBlocking {
                ServiceLocator.sneakerRepository.equip(original.id)
                dao.byId(candidateId)?.let { dao.delete(it) }
            }
        }
    }

    @Test fun shoeDrawRespectsReadinessAndTabs() {
        // 2026-09-27 무료 정책 — 가격 · 결제 확인이 없고, 무료 · 상급 두 칸에 주 버튼이 하나다
        var state by mutableStateOf<DrawScreenState>(DrawScreenState.SignedOut)
        var tab by mutableStateOf(DrawKind.FREE)
        var draws = 0
        var connects = 0
        compose.setContent { StepUpTheme { ExperienceProvider {
            MysteryBoxScreen(state = state, tab = tab, onTab = { tab = it }, onDraw = { draws++ },
                onConnectWallet = { connects++ })
        } } }
        compose.onNodeWithTag("draw-shoe").assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle { assertEquals(0, draws) }
        compose.onNodeWithText(compose.activity.getString(R.string.mystery_draw_outfit)).assertDoesNotExist()
        val fresh = DrawStatus(dailyLeft = 3, dailyTotal = 3, signupLeft = 10, signupGranted = 10, walletLinked = false,
            giftOnLink = 10, giftLeft = 0, runLeft = 0, genesisLeft = 0, runProgressMeters = 0.0, runStepMeters = 1000,
            runToday = 0, runDailyCap = 10, chainPaused = false)
        compose.runOnIdle { state = DrawScreenState.Ready(fresh) }
        compose.onNodeWithTag("draw-shoe").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, draws) }
        // 혜택 카드는 상급 칸으로 — 지갑 전이면 주 버튼이 지갑 연결이 된다
        compose.onNodeWithTag("draw-wallet-benefit").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(DrawKind.PREMIUM, tab) }
        compose.onNodeWithTag("draw-shoe").assertDoesNotExist()
        compose.onNodeWithTag("draw-connect-wallet").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, connects); assertEquals(1, draws) }
        compose.runOnIdle { state = DrawScreenState.Ready(fresh.copy(walletLinked = true, giftOnLink = 0, giftLeft = 10, genesisLeft = 1)) }
        compose.onNodeWithTag("draw-shoe").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(2, draws) }
        compose.onNodeWithTag("draw-tab-free").performClick()
        compose.runOnIdle { assertEquals(DrawKind.FREE, tab) }
    }

    @Test fun firstGuideVisitsRunningShoesAndProfile() {
        compose.setContent { StepUpTheme { ExperienceProvider { MainScaffold(startTour = true) } } }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("guide-step-title").fetchSemanticsNodes().isNotEmpty() }
        val expected = listOf(R.string.tour3_title, R.string.tour_customize_title, R.string.tour11_title)
        expected.forEachIndexed { index, title ->
            compose.onNodeWithTag("guide-step-title").assertTextEquals(compose.activity.getString(title))
            assertEquals(index, GuideTour.stepIndex)
            // 신발 탭의 주 행동은 "신발 자세히 보기"다(보유 신발 상세 v1 — 신기는 상세 안으로 옮겼다)
            val target = when (index) { 0 -> "home-start-run"; 1 -> "shoe-detail"; else -> "profile-settings" }
            compose.waitUntil(5_000) { compose.onAllNodesWithTag(target).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText(compose.activity.getString(
                if (index == expected.lastIndex) R.string.guide_start else R.string.guide_next)).performClick()
        }
        compose.waitUntil(5_000) { !GuideTour.active }
        compose.onNodeWithTag("guide-step-title").assertDoesNotExist()
        compose.onNodeWithTag("home-start-run").assertIsDisplayed()
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
