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
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.AvatarGender
import com.stepup.android.service.WalkSessionService
import com.stepup.android.service.WalkSessionState
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.community.CommunityScreen
import com.stepup.android.ui.screens.customize.CustomizeScreen
import com.stepup.android.ui.screens.customize.RunnerMarketScreen
import com.stepup.android.ui.screens.events.EventsScreen
import com.stepup.android.ui.screens.events.NewsScreen
import com.stepup.android.ui.screens.home.HomeScreen
import com.stepup.android.ui.screens.profile.ProfileScreen
import com.stepup.android.ui.screens.walk.RunScreen
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
 * 디자인 패키지(design/blue-black-2026-09)의 시안과 같은 뷰포트에서 찍는 화면.
 *
 * 390×844 가 시안의 기준이고, 좁은 폰(360)과 넓은 폰(430), 큰 글자(390·1.6배)를
 * 함께 찍는다. 러닝 중 · 러닝 완료는 실제 세션 없이 화면 상태만 넣어 그린다
 * ([WalkSessionService.showStateForTest]) — 적립 · 저장은 일어나지 않는다. 그 상태의
 * 숫자는 검사용 값이다.
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
        ServiceLocator.userPrefs.ensureRunnerUid()
        ServiceLocator.sneakerRepository.ensureStarter()
        ServiceLocator.crewRepository.ensureSeeded()
        ServiceLocator.communityRepository.ensureSeeded()
        ServiceLocator.courseRepository.ensureSeeded()
    }

    @After fun restore() {
        WalkSessionService.showStateForTest(WalkSessionState())
        runBlocking {
            ServiceLocator.avatarRepository.setGender(AvatarGender.MALE)
            ServiceLocator.userPrefs.setDemoMode(false)
        }
    }

    private companion object {
        const val CAPTURE_DENSITY = 1.8f
    }

    private enum class Scene {
        HOME, RUN_ACTIVE, RUN_FINISH, CUSTOMIZE_M, CUSTOMIZE_F, MARKET, NEWS, CHALLENGE, COMMUNITY, PROFILE,
        // 장비 — 데모 체험으로 새 의상(엠버 셸)을 입고 시작 신발(클라우드 러너)을 신은 혼합 조합
        HOME_TRIAL_OUTFIT, PROFILE_TRIAL_OUTFIT, CUSTOMIZE_TRIAL_OUTFIT,
    }

    @Test fun referenceViewports() {
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
            val localized = remember(width, large) { base.createConfigurationContext(config) }
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
                            key(scene, width, large) { Render(scene) }
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
        for ((w, h, enlarged) in viewports) {
            for (s in Scene.entries) {
                prepareScene(s)
                compose.runOnIdle { width = w; height = h; large = enlarged; scene = s }
                compose.waitForIdle()
                // 저장소(DataStore · Room)의 값이 화면에 닿을 틈 — 성별 전환 등
                Thread.sleep(400)
                compose.waitForIdle()
                if (s == Scene.HOME || s == Scene.HOME_TRIAL_OUTFIT) {
                    compose.waitUntil(5_000) { compose.onAllNodesWithText("12,840").fetchSemanticsNodes().isNotEmpty() }
                }
                val name = "ref-$w-${if (enlarged) "large" else "normal"}-${s.ordinal.toString().padStart(2, '0')}-${s.name.lowercase()}"
                audit(name)
                capture(name)
            }
        }
        val directory = File(compose.activity.getExternalFilesDir(null), "experience-qa").apply { mkdirs() }
        File(directory, "ref-audit.jsonl").writeText(findings.joinToString("\n") { it.toString() })
    }

    private fun prepareScene(s: Scene) {
        val now = System.currentTimeMillis()
        when (s) {
            // 24분 18초째 달리는 중 — GPS 잡힘
            Scene.RUN_ACTIVE -> WalkSessionService.showStateForTest(
                WalkSessionState(
                    isActive = true,
                    steps = 4_200,
                    elapsedSec = 1_458,
                    startedAt = now - 1_458_000,
                    gpsFix = true,
                ),
            )
            // 방금 끝난 러닝 — 서버 확인 전이라 "확인 중"으로 보여야 한다
            Scene.RUN_FINISH -> WalkSessionService.showStateForTest(
                WalkSessionState(
                    lastRewardPoints = 4.0,
                    lastSessionSteps = 4_200,
                    lastRewardedSteps = 4_200,
                    lastElapsedSec = 1_458,
                    lastGpsKm = 3.2,
                    lastStartedAt = now,
                ),
            )
            else -> WalkSessionService.showStateForTest(WalkSessionState())
        }
        runBlocking {
            ServiceLocator.avatarRepository.setGender(if (s == Scene.CUSTOMIZE_F) AvatarGender.FEMALE else AvatarGender.MALE)
            val trial = s == Scene.HOME_TRIAL_OUTFIT || s == Scene.PROFILE_TRIAL_OUTFIT || s == Scene.CUSTOMIZE_TRIAL_OUTFIT
            ServiceLocator.userPrefs.setDemoMode(trial)
            if (trial) ServiceLocator.userPrefs.setDemoOutfit("CLO-002")
        }
    }

    @Composable private fun Render(s: Scene) {
        when (s) {
            Scene.HOME, Scene.HOME_TRIAL_OUTFIT -> HomeScreen()
            Scene.RUN_ACTIVE, Scene.RUN_FINISH -> RunScreen()
            Scene.CUSTOMIZE_M, Scene.CUSTOMIZE_F, Scene.CUSTOMIZE_TRIAL_OUTFIT -> CustomizeScreen()
            Scene.MARKET -> RunnerMarketScreen()
            Scene.NEWS -> NewsScreen()
            Scene.CHALLENGE -> EventsScreen()
            Scene.COMMUNITY -> CommunityScreen()
            Scene.PROFILE, Scene.PROFILE_TRIAL_OUTFIT -> ProfileScreen()
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
