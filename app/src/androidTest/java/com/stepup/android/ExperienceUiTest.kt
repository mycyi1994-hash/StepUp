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
import com.stepup.android.data.repo.Crew
import com.stepup.android.data.repo.CrewJoinPolicy
import com.stepup.android.data.repo.PartyMember
import com.stepup.android.data.repo.PartyPhase
import com.stepup.android.data.repo.PartyState
import com.stepup.android.domain.Post
import com.stepup.android.domain.PostCategory
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
        ServiceLocator.userPrefs.ensureRunnerUid()
        ServiceLocator.sneakerRepository.ensureStarter()
        // 크루는 서버에만 있다. 화면 검사는 서버 없이 도는 것이라, 실제 크루와 같은
        // 모양의 크루 하나를 채워 넣는다. 크루장이고 승인제라 관리 카드까지 그려진다.
        ServiceLocator.crewRepository.showForTest(
            listOf(
                Crew(
                    id = "00000000-0000-0000-0000-00000000c0de",
                    monogram = "HR",
                    name = "Hangang Runners",
                    tagline = "Saturday 7AM, 5K by the river",
                    area = "Mapo",
                    memberCount = 24,
                    roster = listOf("Ara Kim", "Bo Lee", "Cha Park", "Dan Choi"),
                    owned = true,
                    joinPolicy = CrewJoinPolicy.APPROVAL,
                    joined = true,
                    pendingCount = 2,
                ),
            ),
        )
        // 글도 서버에만 있다. 번개 하나, 자유 글 하나, 크루 글 하나로 게시판 화면을 채운다.
        val now = System.currentTimeMillis()
        ServiceLocator.communityRepository.showForTest(
            listOf(
                Post(
                    id = 101, category = PostCategory.FLASH, crewId = "", author = "Sora K.",
                    authorId = "u-sora", title = "Tonight 7PM · 5K by the river",
                    body = "Easy pace, everyone welcome.", createdAt = now - 3_600_000,
                    likes = 12, liked = false, commentCount = 4, mine = false,
                    place = "Yeouido Park Gate 3", distanceKm = 1.2, meetAt = now + 5_400_000,
                    capacity = 8, joinedCount = 5, joined = false,
                ),
                Post(
                    id = 102, category = PostCategory.TIP, crewId = "", author = "Ara Kim",
                    authorId = "u-ara", title = "Wide-toe running shoes that worked for me",
                    body = "Three picks after two months of testing.", createdAt = now - 7_200_000,
                    likes = 31, liked = true, commentCount = 9, mine = true,
                    place = "", distanceKm = 0.0, meetAt = 0L, capacity = 0, joinedCount = 0, joined = false,
                ),
                Post(
                    id = 103, category = PostCategory.FREE, crewId = "00000000-0000-0000-0000-00000000c0de",
                    author = "Bo Lee", authorId = "u-bo", title = "Saturday route is set",
                    body = "Meet at the bridge, 6:50.", createdAt = now - 10_800_000,
                    likes = 6, liked = false, commentCount = 2, mine = false,
                    place = "", distanceKm = 0.0, meetAt = 0L, capacity = 0, joinedCount = 0, joined = false,
                ),
            ),
        )
        // 파티 로비도 서버의 방이다. 방장인 나와 크루원 둘이 모인 로비를 채운다.
        ServiceLocator.crewRepository.showPartyForTest(
            PartyState(
                phase = PartyPhase.LOBBY,
                partyId = 1L,
                crewId = "00000000-0000-0000-0000-00000000c0de",
                crewName = "Hangang Runners",
                members = listOf(
                    PartyMember(id = "u-me", name = "", ready = true, isMe = true, isHost = true),
                    PartyMember(id = "u-ara", name = "Ara Kim", ready = true, isMe = false),
                    PartyMember(id = "u-bo", name = "Bo Lee", ready = false, isMe = false),
                ),
            ),
        )
        ServiceLocator.courseRepository.ensureSeeded()
        ServiceLocator.notificationRepository.seedWelcome()
        sneakerId = ServiceLocator.sneakerRepository.inventory.first().first().id
        crewId = ServiceLocator.crewRepository.crews.value.first().id
        postId = ServiceLocator.communityRepository.posts.value.first().id
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
            val localized = remember(language) { base.createConfigurationContext(config) }
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides compose.activity,
                LocalOnBackPressedDispatcherOwner provides compose.activity,
                LocalContext provides localized, LocalConfiguration provides config,
                LocalDensity provides Density(LocalDensity.current.density, config.fontScale)) {
                StepUpTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
                    ExperienceProvider {
                        Box(Modifier.requiredSize(config.screenWidthDp.dp, config.screenHeightDp.dp)
                            .background(Night).testTag("capture")) {
                            key(screen, language, large) { Scene(screen) }
                        }
                    }
                }
            }
        }
        for (locale in listOf("ko", "en", "ja", "zh")) {
            for (night in listOf(false, true)) {
                for (enlarged in listOf(false, true)) {
                    for (index in 0..27) {
                        compose.runOnIdle { language = locale; dark = night; large = enlarged; screen = index }
                        compose.waitForIdle()
                        if (index == 0) compose.waitUntil(5_000) {
                            compose.onAllNodesWithText("12,840").fetchSemanticsNodes().isNotEmpty()
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
            0 -> HomeScreen()
            1 -> RunScreen()
            2 -> CommunityScreen()
            3 -> ItemsScreen()
            4 -> EventsScreen()
            5 -> ProfileScreen()
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
            assertTrue("All nine cues should load", loaded.await(10, TimeUnit.SECONDS))
            assertTrue("Decoder failures: $failures", failures.isEmpty())
        } finally { compose.runOnUiThread { pool.release() } }
    }

    @Test fun mainNavigationAndSettingsAreReachable() {
        runBlocking { ServiceLocator.userPrefs.setReducedMotion(false) }
        compose.setContent { StepUpTheme { ExperienceProvider {
            Box(Modifier.background(Night).testTag("capture")) { MainScaffold() }
        } } }
        val tabs = listOf(R.string.tab_home, R.string.tab_news, R.string.tab_community, R.string.tab_market, R.string.tab_events, R.string.tab_profile)
        // 프로필 화면 안에도 "Profile" 탭이 있으므로 하단 탭 줄 안의 탭만 센다.
        val tabRole = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab) and
            hasAnyAncestor(hasTestTag(BOTTOM_NAV_TAG))
        compose.waitUntil(15_000) { compose.onAllNodes(tabRole).fetchSemanticsNodes().size == 6 }
        tabs.forEachIndexed { index, title ->
            val node = compose.onNode(hasText(compose.activity.getString(title)) and tabRole)
            node.performClick().assertIsSelected()
            capture("navigation-$index")
        }
        compose.onNodeWithText(compose.activity.getString(R.string.profile_tab_settings)).performClick()
        val settingsLabel = compose.activity.getString(R.string.settings_experience)
        compose.onAllNodes(hasScrollAction())[0].performScrollToNode(hasText(settingsLabel))
        compose.onNodeWithText(settingsLabel).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.experience_sound)).assertIsDisplayed()
        capture("navigation-experience")
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
