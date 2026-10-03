package com.stepup.android

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.RunPermissionSheet
import com.stepup.android.domain.WeatherScene
import com.stepup.android.service.WalkSessionService
import com.stepup.android.service.WalkSessionState
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Routes
import com.stepup.android.ui.components.HomePhotos
import com.stepup.android.ui.components.S2Scenery
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.home.HomeScreen
import com.stepup.android.ui.screens.login.LoginContent
import com.stepup.android.ui.screens.login.LoginNotice
import com.stepup.android.ui.screens.login.LoginPhase
import com.stepup.android.ui.screens.onboarding.FirstGuideSheet
import com.stepup.android.ui.screens.onboarding.GuideReplayScreen
import com.stepup.android.ui.screens.splash.LaunchScene
import com.stepup.android.ui.screens.splash.LaunchStage
import com.stepup.android.ui.screens.walk.RunPermissionSheetView
import com.stepup.android.ui.screens.walk.RunStartMenuContent
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 시작·로그인·첫 사용 v1(2026-09-28 전달본, docs/redesign/onboarding-v1) — 장면 번호는 시안의 01~20 그대로.
 *
 * [onboardingScenes] 는 상태를 화면에 바로 넣어 찍는다 — 로그인 결과 · 앱 준비 실패 · 약관 링크 실패 · 권한 안내처럼
 * 기기에서 만들기 어렵거나 시스템 창(계정 선택 · 권한)을 띄워야 하는 장면. 시스템 창은 띄우지 않는다.
 * [firstGuideInTheApp] · [guideReplayFromHelp] 는 앱 셸 안에서 실제로 누르며 간다(첫 안내 → 러닝 방법, 도움말 → 다시 보기 → 뒤로 · 러닝 홈).
 */
class OnboardingDesignTest {
    @get:Rule(order = 0) val appLanguage = object : org.junit.rules.ExternalResource() {
        private var previous = ""
        override fun before() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val context = instrumentation.targetContext
            com.stepup.android.core.AppLocale.syncFromSystem(context)
            previous = com.stepup.android.core.AppLocale.tag
            instrumentation.runOnMainSync { com.stepup.android.core.AppLocale.change(context, "ko") }
        }
        override fun after() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            instrumentation.runOnMainSync { com.stepup.android.core.AppLocale.change(instrumentation.targetContext, previous) }
        }
    }
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()

    private val directory get() = File(compose.activity.getExternalFilesDir(null), "onboarding-v1").apply { mkdirs() }

    @Test fun onboardingScenes() {
        var light by mutableStateOf(false)
        var large by mutableStateOf(false)
        var narrow by mutableStateOf(false)
        prepare()
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (large) 1.3f else 1f)) {
                StepUpTheme(if (light) ThemeMode.LIGHT else ThemeMode.DARK) {
                    ExperienceProvider {
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier.then(if (narrow) Modifier.requiredWidth(320.dp).fillMaxHeight() else Modifier.fillMaxSize())
                                    .align(Alignment.TopCenter),
                            ) { scene() }
                        }
                    }
                }
            }
        }
        val login: (LoginPhase, LoginNotice?, Boolean) -> @Composable () -> Unit = { phase, notice, legal ->
            { LoginContent(phase = phase, notice = notice, onSignIn = {}, legalFailedAtStart = legal) }
        }
        val menuWith: (RunPermissionSheet?) -> @Composable () -> Unit = { sheet ->
            {
                RunStartMenuContent(onBack = {}, onFreeRun = {}, onGoals = {}, onDiet = {})
                if (sheet != null) RunPermissionSheetView(sheet, onPrimary = {}, onSecondary = {}, onDismiss = {})
            }
        }
        val firstHome: @Composable () -> Unit = {
            Box(Modifier.fillMaxSize()) {
                S2Scenery(HomePhotos.all.first { it.mood == WeatherScene.NIGHT }, Modifier.fillMaxSize())
                HomeScreen(firstRunPreview = true)
            }
        }

        // 01 · 05~10 — 로그인. Google 버튼 위 한 줄만 결과에 따라 바뀐다
        show("s01-login", "login-caption", login(LoginPhase.Idle, null, false))
        compose.onNodeWithTag("login-google").assertIsEnabled()
        compose.onNodeWithTag("login-terms").assertHasClickAction()
        compose.onNodeWithTag("login-privacy").assertHasClickAction()
        show("s03-launch-loading", "launch-loading") { LaunchScene(LaunchStage.Loading) }
        show("s04-launch-error", "launch-retry") { LaunchScene(LaunchStage.Error) }
        compose.onNodeWithTag("launch-retry").assertHasClickAction()
        show("s05-signing-in", "login-verifying", login(LoginPhase.Verifying, null, false))
        compose.onNodeWithTag("login-google").assertIsNotEnabled()
        show("s06-offline", "login-notice-offline", login(LoginPhase.Idle, LoginNotice.Offline, false))
        compose.onNodeWithTag("login-google").assertIsEnabled()
        show("s07-login-failed", "login-notice-failed", login(LoginPhase.Idle, LoginNotice.Failed, false))
        show("s08-no-account", "login-notice-noaccount", login(LoginPhase.Idle, LoginNotice.NoAccount, false))
        show("s09-sign-in-again", "login-notice-signinagain", login(LoginPhase.Idle, LoginNotice.SignInAgain, false))
        show("s10-legal-link-error", "login-legal-failed", login(LoginPhase.Idle, null, true))

        // 11 · 02 — 첫 러닝 홈과 그 위의 첫 안내 한 장. 안내 줄은 설명이라 누를 곳이 없다
        show("s11-first-home", "home-first-run", firstHome)
        // 기록을 읽은 뒤의 제목까지 기다린다 — 읽기 전에는 빈 제목이다(0 을 임시로 넣지 않는다)
        compose.waitUntil(10_000) {
            runCatching { compose.onNodeWithTag("home-headline").assertTextEquals(text(R.string.onb_home_first_title)) }.isSuccess
        }
        shot("s11-first-home")
        compose.onNodeWithTag("home-start-run").assertHasClickAction()
        compose.onNodeWithTag("home-first-run").assertTextEquals(text(R.string.onb_home_first_body))
        show("s02-first-guide", "first-guide-sheet") {
            firstHome()
            FirstGuideSheet(onAction = {})
        }
        for (route in listOf("home", "customize", "profile")) compose.onNodeWithTag("guide-row-$route").assertHasNoClickAction()
        compose.onNodeWithTag("first-guide-start").assertHasClickAction()
        compose.onNodeWithTag("first-guide-browse").assertHasClickAction()

        // 12~19 — 러닝 방법과 그 위에 차례로 뜨는 권한 안내
        show("s12-running-menu", "run-menu-free", menuWith(null))
        compose.onNodeWithTag("run-menu-course", useUnmergedTree = true).assertIsNotEnabled()
        show("s13-activity-rationale", "perm-sheet-activity", menuWith(RunPermissionSheet.ActivityRationale))
        compose.onNodeWithTag("perm-primary").assertTextEquals(text(R.string.run_pm_allow))
        compose.onNodeWithTag("perm-secondary").assertTextEquals(text(R.string.run_perm_later))
        show("s14-activity-denied", "perm-sheet-activity-denied", menuWith(RunPermissionSheet.ActivityDenied))
        compose.onNodeWithTag("perm-secondary").assertTextEquals(text(R.string.run_go_home))
        show("s15-activity-settings", "perm-sheet-activity-settings", menuWith(RunPermissionSheet.ActivitySettings))
        compose.onNodeWithTag("perm-primary").assertTextEquals(text(R.string.run_pm_open_settings))
        show("s16-location-rationale", "perm-sheet-location", menuWith(RunPermissionSheet.LocationRationale))
        show("s17-approximate-location", "perm-sheet-location-approximate", menuWith(RunPermissionSheet.ApproximateLocation))
        show("s18-without-location", "perm-sheet-location-off", menuWith(RunPermissionSheet.WithoutLocation))
        show("s19-notification-rationale", "perm-sheet-notification", menuWith(RunPermissionSheet.NotificationRationale))

        // 20 — 사용 안내 다시 보기
        show("s20-guide-replay", "guide-replay-home") { GuideReplayScreen(onBack = {}, onRunHome = {}) }

        // 밝은 테마 · 큰 글씨 · 좁은 폭
        compose.runOnIdle { light = true }
        show("s31-light-login", "login-caption", login(LoginPhase.Idle, null, false))
        show("s32-light-first-guide", "first-guide-sheet") {
            firstHome()
            FirstGuideSheet(onAction = {})
        }
        show("s33-light-activity", "perm-sheet-activity", menuWith(RunPermissionSheet.ActivityRationale))
        show("s34-light-replay", "guide-replay-home") { GuideReplayScreen(onBack = {}, onRunHome = {}) }
        compose.runOnIdle { light = false; large = true }
        show("s35-large-login-error", "login-notice-failed", login(LoginPhase.Idle, LoginNotice.Failed, false))
        compose.onNodeWithTag("login-google").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("login-privacy").performScrollTo().assertIsDisplayed()
        show("s36-large-activity-settings", "perm-sheet-activity-settings", menuWith(RunPermissionSheet.ActivitySettings))
        show("s37-large-first-guide", "first-guide-sheet") {
            firstHome()
            FirstGuideSheet(onAction = {})
        }
        // 버튼 줄은 시트 아래에 붙어 있어(본문만 스크롤) 큰 글씨에서도 넘기지 않고 보인다
        compose.onNodeWithTag("first-guide-browse").assertIsDisplayed()
        compose.runOnIdle { large = false; narrow = true }
        show("s38-narrow-login", "login-caption", login(LoginPhase.Idle, null, false))
        show("s39-narrow-menu", "run-menu-free", menuWith(null))
        show("s40-narrow-replay", "guide-replay-home") { GuideReplayScreen(onBack = {}, onRunHome = {}) }
    }

    /**
     * 앱 셸 안의 첫 사용 — 첫 안내 한 장 → "러닝 시작" → 러닝 방법(12). 러닝은 시작하지 않고,
     * 뒤로 가면 러닝 홈이고 안내는 다시 뜨지 않는다.
     */
    @Test fun firstGuideInTheApp() {
        prepare()
        compose.setContent { StepUpTheme(ThemeMode.DARK) { ExperienceProvider { MainScaffold(startTour = true) } } }
        awaitTag("first-guide-sheet")
        shot("a02-first-guide-in-app")
        tap("first-guide-start")
        awaitTag("run-menu-free")
        awaitGone("first-guide-sheet")
        assertFalse("the guide opens the run menu without starting a run", WalkSessionService.state.value.isActive)
        shot("a12-running-menu-in-app")
        back()
        awaitTag("home-start-run")
        compose.waitForIdle()
        compose.onNodeWithTag("first-guide-sheet").assertDoesNotExist()
        shot("a11-home-after-guide")
    }

    /** 도움말 · 문의 › 앱 사용 안내(시안 20) — 뒤로 가면 도움말, "러닝 홈으로"는 러닝 탭. 가이드를 "안 봄"으로 되돌리지 않는다 */
    @Test fun guideReplayFromHelp() {
        prepare()
        compose.setContent {
            StepUpTheme(ThemeMode.DARK) { ExperienceProvider { MainScaffold(initialRoute = Routes.SETTINGS_SUPPORT) } }
        }
        tap("support-guide")
        awaitTag("guide-replay-home")
        compose.onNodeWithTag("guide-row-home").assertHasNoClickAction()
        shot("a20-guide-replay-in-app")
        back()
        awaitTag("support-guide")
        awaitGone("guide-replay")
        shot("a20b-back-to-help")
        tap("support-guide")
        tap("guide-replay-home")
        awaitTag("home-start-run")
        // 다시 보기는 첫 안내를 되살리지 않는다
        compose.onNodeWithTag("first-guide-sheet").assertDoesNotExist()
        assertTrue(runBlocking { ServiceLocator.userPrefs.guideSeen.first() })
        shot("a20c-run-home")
    }

    // ── 도우미 ─────────────────────────────────────────────────────

    private var scene by mutableStateOf<@Composable () -> Unit>({})

    /** 장면마다 새로 그린다(앞 장면의 시트 · 안내 상태를 이어받지 않게) */
    private fun show(name: String, readyTag: String, content: @Composable () -> Unit) {
        compose.runOnIdle { scene = { key(name) { content() } } }
        awaitTag(readyTag)
        shot(name)
    }

    /** 화면이 쓰는 것과 같은 리소스로 읽는다(앱 언어는 위의 규칙이 ko 로 맞춘다) */
    private fun text(id: Int): String = compose.activity.getString(id)

    private fun tap(tag: String) {
        awaitTag(tag)
        val node = compose.onNodeWithTag(tag)
        runCatching { node.performScrollTo() }
        node.performClick()
        compose.waitForIdle()
    }

    private fun back() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
    }

    private fun prepare() {
        runBlocking {
            ServiceLocator.userPrefs.setReducedMotion(true)
            ServiceLocator.userPrefs.setSounds(false)
            ServiceLocator.userPrefs.setHaptics(false)
            ServiceLocator.userPrefs.setGuideSeen()
        }
        WalkSessionService.showStateForTest(WalkSessionState())
        // 앞 테스트가 남긴 멈춘 러닝이 있으면 첫 안내가 뒤로 미뤄진다(멈춘 러닝을 먼저 묻는다) — 비우고 시작한다
        clearAnyRunCheckpointForTest()
        compose.activityRule.scenario.onActivity {
            it.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            )
        }
    }

    private fun awaitTag(tag: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun awaitGone(tag: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }
    }

    private fun shot(name: String, settle: Long = 700) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(settle)
        captureDisplay(File(directory, "$name.png"))
    }
}
