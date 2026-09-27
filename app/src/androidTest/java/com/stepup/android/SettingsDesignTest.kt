package com.stepup.android

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.core.AppTheme
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.prefs.ExperiencePreferences
import com.stepup.android.data.prefs.NotifyPrefs
import com.stepup.android.push.NotificationSyncState
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Screen
import com.stepup.android.ui.components.S2Stage
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.settings.ClearStep
import com.stepup.android.ui.screens.settings.ConnectedAccountsContent
import com.stepup.android.ui.screens.settings.DeleteStep
import com.stepup.android.ui.screens.settings.ExperienceContent
import com.stepup.android.ui.screens.settings.LanguageContent
import com.stepup.android.ui.screens.settings.NotificationSettingsContent
import com.stepup.android.ui.screens.settings.NotificationSettingsUi
import com.stepup.android.ui.screens.settings.PermissionSnapshot
import com.stepup.android.ui.screens.settings.PrivacyContent
import com.stepup.android.ui.screens.settings.SettingsLoad
import com.stepup.android.ui.screens.settings.SupportContent
import com.stepup.android.ui.screens.settings.WalletLink
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 설정 v1(2026-09-28 전달본, docs/redesign/settings-v1) — 장면 번호는 시안의 01~28 그대로.
 *
 * [settingsInTheApp] 은 앱 셸 안에서 실제 값으로 간다(첫 목록 → 목표 시트 → 알림 → 개인정보 → 연결된 계정 → 테마 → 언어 →
 * 소리 → 도움말). 바꾼 값(목표 · 이벤트 소식 · 테마)은 끝나면 되돌린다. [settingsStates] 는 휴대폰 알림 차단 · 서버 반영
 * 대기 · 저장 실패 · 계정 삭제 단계처럼 기기에서 만들 수 없는 상태를 화면에 바로 넣어 찍는다.
 */
class SettingsDesignTest {
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

    private val directory get() = File(compose.activity.getExternalFilesDir(null), "settings-v1").apply { mkdirs() }

    @Test fun settingsInTheApp() {
        val prefs = ServiceLocator.userPrefs
        val goalBefore = runBlocking { prefs.dailyGoal.first() }
        val notifyBefore = runBlocking { prefs.notifyPrefs.first() }
        val themeBefore = AppTheme.mode
        try {
            edgeToEdge()
            // 앱(MainActivity)처럼 지금 고른 테마로 그린다 — 17 에서 "밝게"를 고르면 화면이 실제로 밝아져야 한다
            AppTheme.change(ThemeMode.DARK)
            compose.setContent { StepUpTheme(AppTheme.mode) { ExperienceProvider { MainScaffold(initialTab = Screen.Profile) } } }
            awaitTag("profile-settings")
            compose.onNodeWithTag("profile-settings").performScrollTo().performClick()
            awaitTag("settings-goal")
            compose.onNodeWithTag("settings-goal").assertIsDisplayed()
            shot("01-settings-home")

            // 24 — 하루 걸음 목표: ±500 은 바로 저장된다(목표를 바꾸는 것만으로 보상을 주지 않는다)
            compose.onNodeWithTag("settings-goal").performClick()
            awaitTag("goal-sheet")
            val up = goalBefore < com.stepup.android.data.prefs.UserPrefs.MAX_GOAL
            compose.onNodeWithTag(if (up) "goal-plus" else "goal-minus").performClick()
            val changed = goalBefore + if (up) 500 else -500
            compose.waitUntil(5_000) { runBlocking { prefs.dailyGoal.first() } == changed }
            compose.onNodeWithTag("goal-value", useUnmergedTree = true).assertTextEquals("%,d".format(changed))
            shot("24-daily-goal")
            compose.onNodeWithTag(if (up) "goal-minus" else "goal-plus").performClick()
            compose.waitUntil(5_000) { runBlocking { prefs.dailyGoal.first() } == goalBefore }
            compose.onNodeWithTag("goal-close").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("goal-sheet").fetchSemanticsNodes().isEmpty() }

            // 02 — 알림: 실제 저장값을 읽은 뒤 스위치. 바꾸면 "이 휴대폰에 저장했어요"
            tap("settings-notifications")
            awaitTag("notif-push")
            shot("02-notifications")
            compose.onNodeWithTag("notif-event").performClick()
            compose.waitUntil(5_000) { runBlocking { prefs.notifyPrefs.first() }.eventNews != notifyBefore.eventNews }
            awaitTag("settings-toast")
            shot("02b-notifications-saved")
            compose.onNodeWithTag("notif-event").performClick()
            compose.waitUntil(5_000) { runBlocking { prefs.notifyPrefs.first() } == notifyBefore }
            back()

            // 06 · 08 — 개인정보 · 앱 권한: 권한은 글자로만(앱 안 스위치가 아니다)
            tap("settings-privacy")
            awaitTag("perm-location")
            shot("06-privacy-permissions")
            tap("privacy-stored")
            awaitTag("stored-sheet")
            shot("08-stored-data-sheet")
            back()
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("stored-sheet").fetchSemanticsNodes().isEmpty() }
            back()

            // 11/12 — 연결된 계정: 이 기기의 실제 로그인 상태 그대로
            tap("settings-connected")
            awaitTag("connected-health")
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText(korean(R.string.set_checking)).fetchSemanticsNodes().isEmpty()
            }
            val signedIn = runBlocking { ServiceLocator.sessionHolder.isSignedIn() }
            if (!signedIn) compose.onAllNodesWithTag("connected-delete").assertCountEquals(0)
            shot(if (signedIn) "11-connected-accounts" else "12-account-not-signed-in")
            back()

            // 16 · 17 — 테마: 고르면 바로 바뀐다(되돌려 둔다)
            tap("settings-theme")
            awaitTag("theme-dark")
            tap("theme-dark")
            compose.waitUntil(5_000) { AppTheme.mode == ThemeMode.DARK }
            shot("16-theme-dark")
            tap("theme-light")
            compose.waitUntil(5_000) { AppTheme.mode == ThemeMode.LIGHT }
            compose.onNodeWithTag("theme-light").assertIsSelected()
            shot("17-theme-light")
            // 나머지 장면은 어두운 테마로 찍고, 원래 테마는 끝에 되돌린다
            tap("theme-dark")
            compose.waitUntil(5_000) { AppTheme.mode == ThemeMode.DARK }
            back()

            // 18 — 언어: 지금 고른 것에 표시(다른 언어를 고르면 화면을 다시 만든다 — 19 는 아래 장면에서)
            tap("settings-language")
            awaitTag("language-ko")
            compose.onNodeWithTag("language-ko").assertIsSelected()
            shot("18-language-korean")
            back()

            // 20 — 소리 · 진동 · 동작
            tap("settings-experience")
            awaitTag("experience-sound")
            shot("20-experience")
            back()

            // 21 · 22 · 23 — 도움말 · 문의
            tap("settings-support")
            awaitTag("faq-0")
            shot("21-support")
            tap("faq-0")
            compose.onNodeWithText(korean(R.string.set_faq_a_earn)).assertIsDisplayed()
            shot("22-faq-expanded")
            tap("support-contact")
            awaitTag("contact-sheet")
            compose.onNodeWithTag("contact-address", useUnmergedTree = true).assertTextEquals("support@stepupcrew.com")
            shot("23-contact-sheet")
            back()
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("contact-sheet").fetchSemanticsNodes().isEmpty() }
            back()

            // 설정 첫 목록의 "더 보기" — 예전 목록에만 있던 길이 남아 있다
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("settings-inbox"))
            compose.onNodeWithTag("settings-inbox").assertIsDisplayed()
            shot("01b-settings-more")
        } finally {
            runBlocking {
                prefs.setDailyGoal(goalBefore)
                prefs.setNotifyPrefs(notifyBefore)
                prefs.setThemeMode(themeBefore.name)
            }
            compose.runOnIdle { AppTheme.change(themeBefore) }
        }
    }

    /** 기기에서 만들 수 없는 상태 — 휴대폰 알림 차단 · 반영 대기 · 저장 실패 · 조회 중/실패 · 삭제 단계 · 영어 · 밝은 테마 · 큰 글씨 */
    @Test fun settingsStates() {
        var light by mutableStateOf(false)
        var large by mutableStateOf(false)
        var narrow by mutableStateOf(false)
        var english by mutableStateOf(false)
        edgeToEdge()
        compose.setContent {
            val density = LocalDensity.current
            val base = LocalContext.current
            val baseConfig = LocalConfiguration.current
            // 영어 장면(19) — 화면을 다시 만들지 않고 이 장면만 영어 자원으로 그린다
            val config = if (english) Configuration(baseConfig).apply { setLocale(Locale.ENGLISH) } else baseConfig
            val context = if (english) base.createConfigurationContext(config) else base
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, if (large) 1.3f else 1f),
                LocalContext provides context,
                LocalConfiguration provides config,
            ) {
                StepUpTheme(if (light) ThemeMode.LIGHT else ThemeMode.DARK) {
                    ExperienceProvider {
                        Box(Modifier.fillMaxSize()) {
                            S2Stage(Modifier.fillMaxSize())
                            Box(
                                Modifier.then(if (narrow) Modifier.requiredWidth(320.dp).fillMaxHeight() else Modifier.fillMaxSize())
                                    .align(Alignment.TopCenter).statusBarsPadding(),
                            ) { scene() }
                        }
                    }
                }
            }
        }
        val prefs = NotifyPrefs(push = true, goalReminder = true, partyInvite = true, eventNews = false)
        val granted = PermissionSnapshot(activity = true, preciseLocation = true, approximateLocation = true, notifications = true)
        fun notifications(ui: NotificationSettingsUi): @Composable () -> Unit = { NotificationSettingsContent(ui) }

        show("03-notifications-blocked", "notif-blocked", notifications(NotificationSettingsUi(SettingsLoad.Ready(prefs), osBlocked = true)))
        // 차단 안내는 앱 스위치를 대신 끄지 않는다
        compose.onNodeWithTag("notif-push").assertIsOn()
        show("04-notification-sync-pending", "notif-sync-pending",
            notifications(NotificationSettingsUi(SettingsLoad.Ready(prefs), sync = NotificationSyncState.Pending)))
        show("05-setting-save-error", "notif-save-failed", notifications(NotificationSettingsUi(SettingsLoad.Ready(prefs), saveFailed = true)))
        compose.onNodeWithTag("notif-event").assertIsOff()
        show("25-settings-loading", "settings-loading", notifications(NotificationSettingsUi(SettingsLoad.Loading)))
        compose.onAllNodesWithTag("notif-push").assertCountEquals(0)
        show("26-settings-error", "settings-load-failed", notifications(NotificationSettingsUi(SettingsLoad.Failed)))

        val limited = PermissionSnapshot(activity = true, preciseLocation = false, approximateLocation = true, notifications = false)
        show("07-permissions-limited", "perm-location") { PrivacyContent(limited) }
        compose.onNodeWithTag("perm-location").assert(hasText(korean(R.string.set_perm_approximate), substring = true))
        show("09-clear-notifications-confirm", "clear-sheet") { PrivacyContent(granted, clear = ClearStep.Confirm) }
        compose.onNodeWithText(korean(R.string.set_clear_keeps_pending)).assertExists()
        show("09b-clearing", "clear-confirm") { PrivacyContent(granted, clear = ClearStep.Clearing) }
        compose.onNodeWithTag("clear-confirm").assertIsNotEnabled()
        show("10-clear-notifications-done", "settings-toast") { PrivacyContent(granted, toast = korean(R.string.set_cleared)) }
        show("27-clear-notifications-error", "clear-failed-sheet") { PrivacyContent(granted, clear = ClearStep.Failed) }

        show("11-connected-accounts", "connected-delete") { ConnectedAccountsContent(signedIn = true, wallet = WalletLink.NotLinked) }
        show("12-account-not-signed-in", "connected-social") { ConnectedAccountsContent(signedIn = false, wallet = WalletLink.SignedOut) }
        compose.onAllNodesWithTag("connected-delete").assertCountEquals(0)
        show("13-account-delete-confirm", "delete-sheet") {
            ConnectedAccountsContent(signedIn = true, wallet = WalletLink.Linked, step = DeleteStep.Confirm)
        }
        show("14-account-deleting", "delete-busy") {
            ConnectedAccountsContent(signedIn = true, wallet = WalletLink.Linked, step = DeleteStep.Deleting)
        }
        compose.onAllNodesWithTag("delete-confirm").assertCountEquals(0)
        show("15-account-delete-unknown", "delete-unknown") {
            ConnectedAccountsContent(signedIn = true, wallet = WalletLink.Linked, step = DeleteStep.Unknown)
        }
        // 결과를 모르면 다시 지우기 버튼이 없다 — 문의 · 닫기만
        compose.onAllNodesWithTag("delete-confirm").assertCountEquals(0)
        show("15b-account-delete-failed", "delete-failed") {
            ConnectedAccountsContent(signedIn = true, wallet = WalletLink.Linked, step = DeleteStep.Failed)
        }

        compose.runOnIdle { english = true }
        show("19-language-english", "language-en") { LanguageContent("en") }
        compose.onNodeWithText("Changes apply immediately.").assertExists()
        compose.runOnIdle { english = false }

        show("28-contact-open-error", "contact-failed-sheet") { SupportContent(contactOpen = true, contactFailed = true) }

        // 밝은 테마 · 큰 글씨 · 좁은 폭
        compose.runOnIdle { light = true }
        show("30-light-notifications", "notif-push", notifications(NotificationSettingsUi(SettingsLoad.Ready(prefs))))
        show("31-light-privacy", "perm-location") { PrivacyContent(limited) }
        compose.runOnIdle { light = false; large = true }
        show("32-large-font-experience", "experience-sound") {
            ExperienceContent(SettingsLoad.Ready(ExperiencePreferences(sounds = true, haptics = true, ambience = false, reducedMotion = false)))
        }
        compose.runOnIdle { large = false; narrow = true }
        show("33-narrow-connected", "connected-delete") { ConnectedAccountsContent(signedIn = true, wallet = WalletLink.NotLinked) }
        compose.runOnIdle { narrow = false }
    }

    // ── 도우미 ─────────────────────────────────────────────────────

    /** 지금 찍는 장면 — [settingsStates] 의 화면 틀 안에 들어간다 */
    private var scene by mutableStateOf<@Composable () -> Unit>({})

    private fun show(name: String, readyTag: String, content: @Composable () -> Unit) {
        compose.runOnIdle { scene = content }
        awaitTag(readyTag)
        shot(name)
    }

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

    private fun korean(id: Int): String {
        val config = Configuration(compose.activity.resources.configuration).apply { setLocale(Locale.KOREAN) }
        return compose.activity.createConfigurationContext(config).getString(id)
    }

    private fun edgeToEdge() {
        runBlocking {
            ServiceLocator.userPrefs.setReducedMotion(true)
            ServiceLocator.userPrefs.setSounds(false)
            ServiceLocator.userPrefs.setHaptics(false)
            ServiceLocator.userPrefs.setGuideSeen()
        }
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

    private fun shot(name: String, settle: Long = 700) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(settle)
        captureDisplay(File(directory, "$name.png"))
    }
}
