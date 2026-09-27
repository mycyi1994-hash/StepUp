package com.stepup.android

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.data.repo.NameWork
import com.stepup.android.data.repo.PhotoWork
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Screen
import com.stepup.android.ui.components.CommerceBackdrop
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.profile.DefaultImageSheet
import com.stepup.android.ui.screens.profile.LeaveSheet
import com.stepup.android.ui.screens.profile.PhotoActionsSheet
import com.stepup.android.ui.screens.profile.ProfileEditContent
import com.stepup.android.ui.screens.profile.ProfileEditUi
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * 프로필 수정 v1(2026-09-28 전달본, docs/redesign/profile-edit-v1) — 장면 번호는 시안의 01~12 그대로.
 *
 * [profileEditInTheApp] 은 앱 셸 안에서 내 정보 → 프로필 수정으로 간다: 바꾸기 전엔 저장을 누를 수 없고, 이름을 바꾸면 누를 수 있다.
 * 기본 이미지는 고르면 바로 저장되고, 이름을 바꾼 채 나가려 하면 묻는다(이미 바꾼 사진은 유지). 저장하면 내 정보로 돌아와 새 이름과
 * "닉네임을 저장했어요". 16자를 넘는 입력은 받지 않는다. 이 기기의 닉네임 · 아바타는 끝에 되돌린다.
 * [profileEditStates] 는 저장 중 · 실패처럼 기기에서 만들 수 없는 장면을 화면에 바로 넣어 찍는다(사진 자리는 사진 파일이 없을 때의 모양).
 */
class ProfileEditDesignTest {
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

    private val directory get() = File(compose.activity.getExternalFilesDir(null), "profile-edit-v1").apply { mkdirs() }

    @Test fun profileEditInTheApp() {
        val prefs = ServiceLocator.userPrefs
        val nameBefore = runBlocking { prefs.nickname.first() }
        val avatarBefore = runBlocking { prefs.avatarId.first() }
        runBlocking {
            prefs.setNickname("민수")
            prefs.setAvatarId(0)
        }
        try {
            edgeToEdge()
            compose.setContent { StepUpTheme(ThemeMode.DARK) { ExperienceProvider { MainScaffold(initialTab = Screen.Profile) } } }
            tap("profile-edit")
            awaitTag("pe-nickname")
            // 01 — 바꾸기 전에는 저장할 것이 없다
            compose.onNodeWithTag("pe-save").assertIsNotEnabled()
            compose.onNodeWithTag("pe-count").assertTextEquals("2 / 16")
            shot("01-edit-profile")

            // 02 — 이름을 바꾸면 저장할 수 있다. 같은 이름으로 되돌리면 다시 막힌다
            compose.onNodeWithTag("pe-nickname").performTextReplacement("아침러너")
            compose.onNodeWithTag("pe-save").assertIsEnabled()
            compose.onNodeWithTag("pe-nickname").performTextReplacement(" 민수 ")
            compose.onNodeWithTag("pe-save").assertIsNotEnabled()
            compose.onNodeWithTag("pe-nickname").performTextReplacement("아침러너")
            compose.onNodeWithTag("pe-save").assertIsEnabled()
            closeKeyboard()
            shot("02-nickname-changed")

            // 06 — 16자까지. 더 넣어도 이미 쓴 글자를 지우지 않는다
            compose.onNodeWithTag("pe-nickname").performTextReplacement("한강아침러너_STEPUP_01")
            compose.onNodeWithTag("pe-count").assertTextEquals("16 / 16")
            compose.onNodeWithTag("pe-nickname").performTextInput("X")
            compose.onNodeWithTag("pe-nickname").assertTextEquals("한강아침러너_STEPUP_01")
            closeKeyboard()
            shot("06-name-limit")
            compose.onNodeWithTag("pe-nickname").performTextReplacement("아침러너")
            closeKeyboard()

            // 03 · 04 — 사진 변경 메뉴 → 기본 이미지: 누르면 바로 저장, 시트가 닫힌다
            tap("pe-change-photo")
            awaitTag("pe-photo-sheet")
            shot("03-photo-actions")
            tap("pe-default-images")
            awaitTag("pe-default-sheet")
            compose.onNodeWithTag("pe-avatar-0").assertIsSelected()
            shot("04-default-images")
            tap("pe-avatar-6")
            awaitGone("pe-default-sheet")
            compose.waitUntil(5_000) { runBlocking { prefs.avatarId.first() } == 6 }
            // 이름 초안은 그대로
            compose.onNodeWithTag("pe-nickname").assertTextEquals("아침러너")

            // 09 — 이름을 바꾼 채 나가려 하면 묻는다. 계속 수정하면 초안 그대로
            back()
            awaitTag("pe-leave-sheet")
            compose.onNodeWithText(korean(R.string.pe_leave_photo_kept)).assertExists()
            shot("09-leave-confirm")
            tap("pe-keep-editing")
            awaitGone("pe-leave-sheet")
            compose.onNodeWithTag("pe-nickname").assertTextEquals("아침러너")

            // 12 — 저장 → 내 정보에 새 이름과 안내(실제로 저장된 뒤에만)
            tap("pe-save")
            awaitTag("profile-saved-notice")
            // 내 정보의 이름은 저장소에서 다시 읽는다 — 돌아온 뒤 곧 새 이름
            compose.waitUntil(5_000) { runCatching { compose.onNodeWithTag("profile-name").assertTextEquals("아침러너") }.isSuccess }
            assertEquals("아침러너", runBlocking { prefs.nickname.first() })
            shot("12-saved-profile", settle = 300)

            // 다시 들어오면 저장된 이름으로 시작하고, 아무것도 바꾸지 않았으면 묻지 않고 나간다
            compose.waitUntil(6_000) { compose.onAllNodesWithTag("profile-saved-notice").fetchSemanticsNodes().isEmpty() }
            tap("profile-edit")
            awaitTag("pe-nickname")
            compose.onNodeWithTag("pe-nickname").assertTextEquals("아침러너")
            back()
            compose.onAllNodesWithTag("pe-leave-sheet").assertCountEquals(0)
            awaitTag("profile-edit")
        } finally {
            runBlocking {
                prefs.setNickname(nameBefore)
                prefs.setAvatarId(avatarBefore)
            }
        }
    }

    @Test fun profileEditStates() {
        var light by mutableStateOf(false)
        var large by mutableStateOf(false)
        var narrow by mutableStateOf(false)
        edgeToEdge()
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (large) 1.3f else 1f)) {
                StepUpTheme(if (light) ThemeMode.LIGHT else ThemeMode.DARK) {
                    ExperienceProvider {
                        Box(Modifier.fillMaxSize()) {
                            CommerceBackdrop(Modifier.fillMaxSize())
                            Box(
                                Modifier.then(if (narrow) Modifier.requiredWidth(320.dp).fillMaxHeight() else Modifier.fillMaxSize())
                                    .align(Alignment.TopCenter).statusBarsPadding(),
                            ) { scene() }
                        }
                    }
                }
            }
        }
        // 사진 자리 — 전달본처럼 사진 파일이 없을 때의 모양(갤러리 사진을 쓰는데 파일이 없으면 사람 모양)
        val base = ProfileEditUi(stored = "아침러너", avatarId = UserPrefs.AVATAR_CUSTOM)
        fun edit(ui: ProfileEditUi, text: String): @Composable () -> Unit = {
            val field = remember(text) { TextFieldState(text) }
            ProfileEditContent(ui, field)
        }
        show("s01-edit-profile", "pe-nickname", edit(base.copy(stored = "민수"), "민수"))
        compose.onNodeWithTag("pe-save").assertIsNotEnabled()
        show("s02-nickname-changed", "pe-nickname", edit(base.copy(stored = "민수"), "아침러너"))
        compose.onNodeWithTag("pe-save").assertIsEnabled()
        show("s03-photo-actions", "pe-photo-sheet") {
            ProfileEditContent(base, remember { TextFieldState("아침러너") })
            PhotoActionsSheet(onAlbum = {}, onDefaults = {}, onDismiss = {})
        }
        show("s04-default-images", "pe-default-sheet") {
            ProfileEditContent(base, remember { TextFieldState("아침러너") })
            DefaultImageSheet(current = 0, saving = false, failed = false, onPick = {}, onDismiss = {})
        }
        compose.onNodeWithTag("pe-avatar-0").assertIsSelected()
        compose.onNodeWithTag("pe-avatar-1").assertIsNotSelected()
        show("s04b-default-image-failed", "pe-default-error") {
            ProfileEditContent(base, remember { TextFieldState("아침러너") })
            DefaultImageSheet(current = 0, saving = false, failed = true, onPick = {}, onDismiss = {})
        }
        show("s05-empty-nickname", "pe-nickname", edit(base, ""))
        compose.onNodeWithTag("pe-save").assertIsEnabled()
        compose.onNodeWithTag("pe-count").assertTextEquals("0 / 16")
        show("s06-name-limit", "pe-nickname", edit(base, "한강아침러너_STEPUP_01"))
        compose.onNodeWithTag("pe-count").assertTextEquals("16 / 16")
        show("s07-photo-saving", "pe-avatar", edit(base.copy(photo = PhotoWork.Saving), "아침러너"))
        compose.onNodeWithTag("pe-save").assertIsNotEnabled()
        compose.onNodeWithTag("pe-change-photo").assertIsNotEnabled()
        show("s08-photo-error", "pe-photo-error", edit(base.copy(stored = "민수", photo = PhotoWork.Failed), "아침러너"))
        compose.onNodeWithTag("pe-photo-error-action").assertHasClickAction()
        show("s09-leave-confirm", "pe-leave-sheet") {
            ProfileEditContent(base, remember { TextFieldState("민수") })
            LeaveSheet(photoChanged = true, onKeep = {}, onDiscard = {})
        }
        show("s10-name-saving", "pe-nickname", edit(base.copy(stored = "민수", name = NameWork.Saving("아침러너")), "아침러너"))
        compose.onNodeWithTag("pe-save").assertIsNotEnabled()
        compose.onNodeWithText(korean(R.string.pe_saving)).assertExists()
        show("s11-name-error", "pe-name-error", edit(base.copy(stored = "민수", name = NameWork.Failed("아침러너")), "아침러너"))
        compose.onNodeWithText(korean(R.string.pe_save_again)).assertExists()
        compose.onNodeWithTag("pe-save").assertIsEnabled()
        // 기본 이미지를 쓰는 사람의 모습
        show("s13-emoji-avatar", "pe-avatar", edit(base.copy(avatarId = 6), "아침러너"))

        compose.runOnIdle { light = true }
        show("s30-light", "pe-nickname", edit(base.copy(stored = "민수"), "아침러너"))
        compose.runOnIdle { light = false; large = true }
        show("s31-large-font", "pe-nickname", edit(base.copy(stored = "민수", photo = PhotoWork.Failed), "한강아침러너_STEPUP_01"))
        compose.runOnIdle { large = false; narrow = true }
        show("s32-narrow", "pe-nickname", edit(base.copy(stored = "민수"), "한강아침러너_STEPUP_01"))
        show("s33-narrow-default-images", "pe-default-sheet") {
            ProfileEditContent(base, remember { TextFieldState("아침러너") })
            DefaultImageSheet(current = 6, saving = false, failed = false, onPick = {}, onDismiss = {})
        }
    }

    // ── 도우미 ─────────────────────────────────────────────────────

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

    /** 글자판이 떠 있으면 닫는다(뒤로 가기가 글자판을 먼저 닫으므로, 입력칸의 초점만 푼다) */
    private fun closeKeyboard() {
        compose.onNodeWithTag("pe-nickname").performImeAction()
        compose.waitForIdle()
    }

    private fun korean(id: Int): String {
        val config = android.content.res.Configuration(compose.activity.resources.configuration)
            .apply { setLocale(java.util.Locale.KOREAN) }
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
