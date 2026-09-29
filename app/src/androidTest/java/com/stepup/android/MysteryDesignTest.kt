package com.stepup.android

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.core.ServiceLocator
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Screen
import com.stepup.android.ui.components.HomePhotos
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A small capture of the new draw entry and the home scenery controls. */
@RunWith(AndroidJUnit4::class)
class MysteryDesignTest {
    @get:Rule(order = 0) val appLanguage = object : org.junit.rules.ExternalResource() {
        private var previous = ""
        override fun before() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val context = instrumentation.targetContext
            com.stepup.android.core.AppLocale.syncFromSystem(context)
            previous = com.stepup.android.core.AppLocale.tag
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
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun drawScreenAndBackgroundSwitch() {
        runBlocking {
            ServiceLocator.userPrefs.setReducedMotion(true)
            ServiceLocator.userPrefs.setSounds(false)
            ServiceLocator.userPrefs.setHaptics(false)
            ServiceLocator.userPrefs.setGuideSeen()
            ServiceLocator.sneakerRepository.ensureStarter()
        }
        compose.activityRule.scenario.onActivity {
            it.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            )
        }
        var largeType by mutableStateOf(false)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (largeType) 1.3f else 1f)) {
                StepUpTheme(ThemeMode.DARK) { ExperienceProvider { MainScaffold() } }
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("home-start-run").fetchSemanticsNodes().isNotEmpty() }
        val out = File(compose.activity.getExternalFilesDir(null), "screen-gallery").apply { mkdirs() }
        // 하단 가운데 뽑기 탭(신발 화면 확정안 2026-09-28)
        compose.onNodeWithTag("nav-label-${Screen.Draw.route}", useUnmergedTree = true).performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("draw-home").fetchSemanticsNodes().isNotEmpty()
        }
        // 뽑기 디자인 26장(2026-09-28) — 위 무료 · 상급 글자 탭, 고른 탭의 상자 무대와 실행 버튼 하나. 가격 · SUP 안내는 없다
        compose.onNodeWithTag("draw-tab-free").assertIsDisplayed()
        compose.onNodeWithTag("draw-tab-premium").assertIsDisplayed()
        compose.onNodeWithTag("draw-free").assertIsDisplayed()
        compose.onNodeWithTag("draw-free-action").assertExists()
        compose.onNodeWithTag("draw-premium-action").assertDoesNotExist()
        compose.onAllNodesWithText("500 SUP", substring = true).assertCountEquals(0)
        compose.onNodeWithText(compose.activity.getString(R.string.mystery_draw_outfit)).assertDoesNotExist()
        captureDisplay(File(out, "mystery-01-normal.png"))

        compose.runOnIdle { largeType = true }
        compose.waitForIdle()
        captureDisplay(File(out, "mystery-02-large-type.png"))

        // 하단 탭 — 뽑기 탭에서 러닝 탭으로(뒤로 쌓이지 않는다)
        compose.runOnIdle { largeType = false }
        // 서버를 읽지 못한 기기면 불러오기 실패 시트가 떠 있다 — 먼저 시트만 닫는다
        if (compose.onAllNodesWithTag("draw-sheet-load-failed", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) {
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            compose.waitForIdle()
        }
        compose.onNodeWithTag("nav-label-${Screen.Run.route}", useUnmergedTree = true).performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("draw-home").fetchSemanticsNodes().isEmpty()
        }
        compose.waitForIdle()
        captureDisplay(File(out, "mystery-03-after-run.png"))
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("home-start-run").fetchSemanticsNodes().isNotEmpty() }
        val before = HomePhotos.all.first { photo ->
            compose.onAllNodesWithTag("home-scene-${photo.key}").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("home-background-next").assertIsDisplayed().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("home-scene-${before.key}").fetchSemanticsNodes().isEmpty()
        }
        captureDisplay(File(out, "home-01-background-next.png"))
    }
}
