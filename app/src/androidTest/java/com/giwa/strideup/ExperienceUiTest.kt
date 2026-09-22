package com.giwa.strideup

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
import com.giwa.strideup.ui.StrideUpRoot
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.ui.components.VoltButton
import com.giwa.strideup.ui.components.animatedInt
import com.giwa.strideup.ui.experience.*
import com.giwa.strideup.ui.screens.home.HomeScreen
import com.giwa.strideup.ui.screens.walk.*
import com.giwa.strideup.ui.screens.community.*
import com.giwa.strideup.ui.screens.items.*
import com.giwa.strideup.ui.screens.events.EventsScreen
import com.giwa.strideup.ui.screens.profile.*
import com.giwa.strideup.ui.screens.rewards.WalletScreen
import com.giwa.strideup.ui.screens.notifications.NotificationsScreen
import com.giwa.strideup.ui.screens.settings.*
import com.giwa.strideup.ui.screens.login.LoginScreen
import com.giwa.strideup.ui.theme.*
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
        ServiceLocator.crewRepository.ensureSeeded()
        ServiceLocator.communityRepository.ensureSeeded()
        ServiceLocator.courseRepository.ensureSeeded()
        ServiceLocator.notificationRepository.seedWelcome()
        sneakerId = ServiceLocator.sneakerRepository.inventory.first().first().id
        crewId = ServiceLocator.crewRepository.crews.first { it.isNotEmpty() }.first().id
        postId = ServiceLocator.communityRepository.posts.first { it.isNotEmpty() }.first().id
    }

    @Test fun allModulesRenderInFourLanguagesAndLargeText() {
        var screen by mutableIntStateOf(0)
        var language by mutableStateOf("ko")
        var large by mutableStateOf(false)
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
                StrideUpTheme {
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
            for (index in 0..24) {
                compose.runOnIdle { language = locale; screen = index; large = false }
                compose.waitForIdle()
                capture("$locale-${index.toString().padStart(2, '0')}")
            }
        }
        // The dashboard, run controls, settings and forms must remain usable on a compact phone.
        for (index in listOf(0, 1, 5, 11, 18, 19)) {
            compose.runOnIdle { language = "ko"; screen = index; large = true }
            compose.waitForIdle()
            capture("large-$index")
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
        }
    }

    @Test fun experienceSwitchesPersistAndSurviveRecomposition() {
        var visible by mutableStateOf(true)
        compose.setContent { StrideUpTheme { ExperienceProvider { if (visible) ExperienceSettingsScreen {} } } }
        compose.onNodeWithText(compose.activity.getString(R.string.experience_sound)).performClick()
        compose.waitUntil { runBlocking { ServiceLocator.userPrefs.experience.first().sounds } }
        compose.onNodeWithText(compose.activity.getString(R.string.experience_motion)).performClick()
        compose.waitUntil { runBlocking { !ServiceLocator.userPrefs.experience.first().reducedMotion } }
        compose.runOnIdle { visible = false }
        compose.runOnIdle { visible = true }
        compose.onNode(hasText(compose.activity.getString(R.string.experience_sound)) and isToggleable()).assertIsOn()
        compose.onNode(hasText(compose.activity.getString(R.string.experience_motion)) and isToggleable()).assertIsOff()
    }

    @Test fun disabledControlsCannotFireAndPressAnimationSettles() {
        var clicks = 0
        var enabled by mutableStateOf(false)
        compose.setContent {
            StrideUpTheme { CompositionLocalProvider(LocalMotion provides MotionPreferences()) {
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
        compose.setContent { StrideUpTheme {
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
        compose.setContent { StrideUpTheme { ExperienceProvider {
            Box(Modifier.background(Night).testTag("capture")) { StrideUpRoot() }
        } } }
        val tabs = listOf(R.string.tab_home, R.string.tab_community, R.string.tab_items, R.string.tab_events, R.string.tab_profile)
        val tabRole = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        compose.waitUntil(15_000) { compose.onAllNodes(tabRole).fetchSemanticsNodes().size == 5 }
        tabs.forEachIndexed { index, title ->
            val node = compose.onNode(hasText(compose.activity.getString(title)) and tabRole)
            node.performClick().assertIsSelected()
            capture("navigation-$index")
        }
        val settingsLabel = compose.activity.getString(R.string.settings_experience)
        compose.onAllNodes(hasScrollAction())[0].performScrollToNode(hasText(settingsLabel))
        compose.onNodeWithText(settingsLabel).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.experience_sound)).assertIsDisplayed()
        capture("navigation-experience")
    }

    private fun capture(name: String) {
        val bitmap = compose.onNodeWithTag("capture").captureToImage().asAndroidBitmap()
        assertTrue(bitmap.width > 300 && bitmap.height > 500)
        val directory = File(compose.activity.getExternalFilesDir(null), "experience-qa").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
