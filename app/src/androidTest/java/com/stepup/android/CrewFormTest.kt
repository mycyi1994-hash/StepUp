package com.stepup.android

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.stepup.android.core.ServiceLocator
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Routes
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test

/** Exercises the production form without submitting a real crew to the server. */
class CrewFormTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @After fun closeKeyboardBeforeActivityIsDestroyed() {
        compose.runOnUiThread {
            androidx.core.view.WindowInsetsControllerCompat(
                compose.activity.window, compose.activity.window.decorView,
            ).hide(androidx.core.view.WindowInsetsCompat.Type.ime())
        }
        compose.waitUntil(5_000) {
            androidx.core.view.ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) != true
        }
    }

    @Test fun meetupFormNeedsTitleAndPlaceAndKeepsNumbersInRange() {
        runBlocking {
            ServiceLocator.userPrefs.setReducedMotion(true)
            ServiceLocator.userPrefs.setGuideSeen()
        }
        compose.setContent {
            StepUpTheme(ThemeMode.DARK) {
                ExperienceProvider { MainScaffold(initialRoute = Routes.postCompose("")) }
            }
        }
        fun fill(label: Int, value: String) {
            val description = compose.activity.getString(label)
            // 앞 칸의 키보드가 열리고 닫히는 동안 목록 높이가 바뀌어, 스크롤해 둔 칸이 잠깐 목록에서 빠질 수 있다
            // (PR #34 Experience QA 36254343201). 칸이 실제로 있을 때까지 스크롤을 다시 하고 누른다.
            compose.waitUntil(5_000) {
                runCatching {
                    compose.onNodeWithTag("form-content").performScrollToNode(hasContentDescription(description))
                }.isSuccess && compose.onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty()
            }
            // 키보드 창이 바뀌는 순간에는 터치 주입이 거절될 수 있다("Failed to inject touch input",
            // 2026-09-28 PR #59 Experience QA). 화면이 한가해진 뒤 두 번까지 다시 누른다.
            for (attempt in 1..3) {
                val clicked = runCatching { compose.onNodeWithContentDescription(description).performClick() }
                if (clicked.isSuccess) break
                if (attempt == 3) throw clicked.exceptionOrNull()!!
                compose.waitForIdle()
                Thread.sleep(500)
            }
            compose.waitUntil(5_000) {
                runCatching { compose.onNodeWithContentDescription(description).assertIsFocused() }.isSuccess
            }
            compose.onNodeWithContentDescription(description).assertIsFocused()
            compose.onNodeWithContentDescription(description).performTextReplacement(value)
        }
        // 번개러닝(UI 최종 통합안 compose_flash) — 제목 · 만나는 곳을 채워야 올릴 수 있고, 숫자는 [−][값][+] 로만 바뀌어 범위를 벗어나지 않는다
        compose.onNodeWithTag("post-cat-flash").performClick()
        compose.onNodeWithTag("post-submit").assertIsNotEnabled()
        fill(R.string.post_field_title, "Riverside run")
        compose.onNodeWithTag("post-submit").assertIsNotEnabled()
        fill(R.string.post_field_place, "Bridge entrance")
        compose.onNodeWithTag("post-submit").assertIsEnabled()
        // 숫자 칸은 키보드를 닫은 뒤에 누른다 — 키보드가 닫히며 창이 늘어나는 동안 누르면 빗나간다(2026-10-25 PR #123 Experience QA)
        hideKeyboard()
        // 정원 2명 아래로는 줄지 않는다
        repeat(4) { step("post-capacity-minus", "post-capacity-value", "${5 - it}") }
        compose.onNodeWithTag("post-capacity-minus").assertIsNotEnabled()
        // 달릴 거리는 0.5km 씩 · 출발 시각은 시각 칸(2026-10-26 — "오늘 … · N분 뒤 출발")
        step("post-distance-plus", "post-distance-value", "1.5")
        compose.onNodeWithTag("post-starts-line").assertExists()
        compose.onNodeWithTag("post-submit").assertIsEnabled()
        // Deliberately do not publish a real meetup from instrumentation.
    }

    @Test fun postBodyAndKeyboardLeaveSubmitReachable() {
        runBlocking {
            ServiceLocator.userPrefs.setReducedMotion(true)
            ServiceLocator.userPrefs.setGuideSeen()
        }
        compose.setContent {
            StepUpTheme(ThemeMode.DARK) {
                ExperienceProvider { MainScaffold(initialRoute = Routes.postCompose("")) }
            }
        }
        val title = compose.onNodeWithContentDescription(compose.activity.getString(R.string.post_field_title))
        title.performScrollTo().performTextInput("Evening riverside run")
        val body = compose.onNodeWithContentDescription(compose.activity.getString(R.string.post_field_body))
        body.performScrollTo().performClick().performTextInput("Meet at the park entrance.")
        try {
            compose.waitUntil(5_000) {
                androidx.core.view.ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                    ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == true
            }
            body.assertIsDisplayed()
            compose.onNodeWithTag("post-submit").assertIsDisplayed().assertIsEnabled()
            compose.onNodeWithTag("bottom-nav").assertDoesNotExist()
        } finally {
            capture("post-body-keyboard")
        }
        // Never submit or create a real post from this design test.
    }

    @Test fun nameInputAndKeyboardLeaveSubmitReachable() = exerciseCrewForm(1f)

    @Test fun nameInputAndKeyboardLeaveSubmitReachableAtLargeFont() = exerciseCrewForm(1.3f)

    private fun exerciseCrewForm(font: Float) {
        runBlocking {
            ServiceLocator.userPrefs.setReducedMotion(true)
            ServiceLocator.userPrefs.setGuideSeen()
        }
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1.8f, font)) {
                StepUpTheme(ThemeMode.DARK) {
                    ExperienceProvider {
                        Box(Modifier.requiredSize(360.dp, 780.dp).testTag("crew-form-viewport")) {
                            com.stepup.android.ui.components.NightCanvas(Modifier.fillMaxSize())
                            MainScaffold(initialRoute = Routes.CREW_CREATE)
                        }
                    }
                }
            }
        }
        // Each font size gets a fresh rule-owned Activity. Replacing a focused
        // editor in-place races its pending IME hide against the next editor's show.
        // 크루 명함형 만들기 1 / 3 · 크루 소개(예전 "모임 만들기" 길) — 이름이 있어야 "다음"을 누를 수 있다.
        run {
            compose.onNodeWithTag("crew-draft-primary").assertIsDisplayed().assertIsNotEnabled()
            val name = compose.onNodeWithTag("crew-draft-name")
            name.performScrollTo().performClick()
            try {
                compose.waitUntil(timeoutMillis = 5_000) {
                    androidx.core.view.ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                        ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == true
                }
                name.performTextInput("River runners")
                name.assert(SemanticsMatcher("crew name text is retained") {
                    it.config[SemanticsProperties.EditableText].text == "River runners"
                })
            } finally {
                // Preserve the focused form before JUnit tears down its activity,
                // including a missing keyboard; a post-test adb screenshot is too late.
                capture("$font-keyboard-wait")
            }
            name.assertIsDisplayed()
            compose.onNodeWithTag("crew-draft-primary").assertIsDisplayed().assertIsEnabled()
            val tagline = compose.onNodeWithTag("crew-draft-tagline")
            tagline.performScrollTo().performClick().performTextInput("Easy laps after work")
            compose.onNodeWithTag("crew-draft-primary").assertIsDisplayed()
            compose.onNodeWithTag("main-header").assertDoesNotExist()
            capture("$font-input")
            name.performScrollTo().performTextClearance()
            compose.onNodeWithTag("crew-draft-primary").assertIsNotEnabled()
        }
    }

    private fun capture(name: String) {
        val directory = java.io.File(compose.activity.getExternalFilesDir(null), "form-checks").apply { mkdirs() }
        captureDisplay(java.io.File(directory, "$name.png"))
    }

    /**
     * 조절 단추를 보이게 한 뒤 한 번 누르고, 값 칸이 [expected] 가 될 때까지 기다린다.
     * 좌표 터치 대신 단추의 누르기 동작을 부른다 — 키보드가 닫히는 애니메이션 동안 창이 움직여 터치가 빗나갔다(PR #123 · #124 Experience QA)
     */
    private fun step(key: String, valueTag: String, expected: String) {
        compose.onNodeWithTag("form-content").performScrollToNode(hasTestTag(key))
        compose.waitForIdle()
        compose.onNodeWithTag(key).assertIsEnabled()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        compose.waitUntil(5_000) {
            runCatching { compose.onNodeWithTag(valueTag, useUnmergedTree = true).assertTextEquals(expected) }.isSuccess
        }
    }

    /** 키보드를 닫고 다 닫힐 때까지 기다린다 */
    private fun hideKeyboard() {
        compose.runOnUiThread {
            androidx.core.view.WindowInsetsControllerCompat(compose.activity.window, compose.activity.window.decorView)
                .hide(androidx.core.view.WindowInsetsCompat.Type.ime())
        }
        compose.waitUntil(5_000) {
            androidx.core.view.ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) != true
        }
        compose.waitForIdle()
    }
}
