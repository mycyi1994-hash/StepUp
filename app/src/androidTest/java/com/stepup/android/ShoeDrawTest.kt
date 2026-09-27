package com.stepup.android

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
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
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.repo.EconomyOutcome
import com.stepup.android.domain.DrawKind
import com.stepup.android.domain.DrawStatus
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Screen
import com.stepup.android.ui.components.S2Stage
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.gacha.DrawScreenState
import com.stepup.android.ui.screens.gacha.DrawSource
import com.stepup.android.ui.screens.gacha.MysteryBoxScreen
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 신발 뽑기 — 무료 정책(2026-09-27, docs/redesign/shoe-draw). 시안 세 장면(무료 · 상급 지갑 전 · 상급 지갑 뒤)과
 * 상태를 찍고, 앱 셸 안에서 한 번 뽑기 → 결과 → 내 신발까지 탄다. 서버는 흉내 낸 DrawSource 다
 * (서버 규칙 자체는 supabase/tests 의 0042 검사가 본다).
 */
class ShoeDrawTest {
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

    private val directory get() = File(compose.activity.getExternalFilesDir(null), "shoe-draw").apply { mkdirs() }

    @Test fun drawStates() {
        var state by mutableStateOf<DrawScreenState>(DrawScreenState.Ready(FRESH))
        var tab by mutableStateOf(DrawKind.FREE)
        var large by mutableStateOf(false)
        var light by mutableStateOf(false)
        var narrow by mutableStateOf(false)
        var retries = 0
        edgeToEdge()
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (large) 1.3f else 1f)) {
                StepUpTheme(if (light) ThemeMode.LIGHT else ThemeMode.DARK) {
                    ExperienceProvider {
                        Box(Modifier.fillMaxSize()) {
                            S2Stage(Modifier.fillMaxSize())
                            Box(
                                Modifier.then(if (narrow) Modifier.requiredWidth(320.dp).fillMaxHeight() else Modifier.fillMaxSize())
                                    .align(Alignment.TopCenter).statusBarsPadding(),
                            ) {
                                MysteryBoxScreen(state = state, tab = tab, onTab = { tab = it }, onRetry = { retries++ })
                            }
                        }
                    }
                }
            }
        }
        val context = compose.activity
        fun label(id: Int, vararg args: Any) = context.getString(id, *args)

        // 01 — 처음 가입한 날: 가입 선물 10 + 오늘 3 = 13
        state("01-free-draw") {
            compose.onNodeWithTag("draw-tab-free").assertIsSelected()
            compose.onNodeWithTag("draw-headline").assertTextEquals(label(R.string.draw_free_headline, 10, 3))
            text("draw-free-left", "13회")
            compose.onNodeWithText(label(R.string.draw_signup_gift)).assertExists()
            compose.onNodeWithTag("draw-shoe").assertIsEnabled().assertTextContains(label(R.string.draw_action_free))
            compose.onNodeWithTag("draw-caption").assertTextEquals(label(R.string.draw_caption_added))
            noPrice()
        }
        // 02 — 상급 칸, 지갑 전: 주 버튼은 지갑 연결
        compose.onNodeWithTag("draw-wallet-benefit").performScrollTo().performClick()
        state("02-premium-connect-wallet") {
            compose.onNodeWithTag("draw-tab-premium").assertIsSelected()
            compose.onNodeWithTag("draw-headline").assertTextEquals(label(R.string.draw_premium_headline_connect))
            compose.onNodeWithTag("draw-shoe").assertDoesNotExist()
            compose.onNodeWithTag("draw-connect-wallet").assertIsEnabled()
            compose.onNodeWithText(label(R.string.draw_premium_on_link)).assertExists()
            compose.onNodeWithTag("draw-caption").assertTextEquals(label(R.string.draw_caption_no_wallet))
            noPrice()
        }
        // 03 — 상급 칸, 지갑 연결 뒤: 선물 10 · 러닝 0 · 0.6 / 1km
        compose.runOnIdle { state = DrawScreenState.Ready(LINKED) }
        state("03-premium-running") {
            text("draw-premium-left", "10회")
            compose.onNodeWithTag("draw-run-progress", useUnmergedTree = true).performScrollTo()
            compose.onNodeWithText(label(R.string.draw_km, "0.4")).assertExists()
            compose.onNodeWithText(label(R.string.draw_progress, "0.6", "1")).assertExists()
            compose.onNodeWithTag("draw-shoe").assertIsEnabled().assertTextContains(label(R.string.draw_action_premium))
        }
        // 04 — 오늘 러닝 한도를 채웠다
        compose.runOnIdle { state = DrawScreenState.Ready(LINKED.copy(runLeft = 10, runToday = 10, runProgressMeters = 300.0)) }
        state("04-premium-run-cap") {
            compose.onNodeWithTag("draw-run-progress").performScrollTo()
            compose.onNodeWithText(label(R.string.draw_run_cap_reached)).assertExists()
            text("draw-premium-left", "20회")
        }
        // 05 — 무료를 다 썼다: 버튼은 누를 수 없고, 내일 생긴다고 알린다
        compose.runOnIdle { tab = DrawKind.FREE; state = DrawScreenState.Ready(FRESH.copy(dailyLeft = 0, signupLeft = 0)) }
        state("05-free-used-up") {
            text("draw-free-left", "0회")
            compose.onNodeWithTag("draw-shoe").assertIsNotEnabled().assertTextContains(label(R.string.draw_action_free_empty))
            compose.onNodeWithTag("draw-caption").assertTextEquals(label(R.string.draw_caption_free_tomorrow, 3))
        }
        // 06 — 로그인 전
        compose.runOnIdle { state = DrawScreenState.SignedOut }
        state("06-signed-out") {
            compose.onNodeWithTag("draw-shoe").assertIsNotEnabled().assertTextContains(label(R.string.draw_action_sign_in))
        }
        // 07 — 읽는 중: 0회가 아니라 "—"
        compose.runOnIdle { state = DrawScreenState.Loading }
        state("07-loading") {
            text("draw-free-left", "—")
            compose.onNodeWithTag("draw-shoe").assertIsNotEnabled()
        }
        // 08 — 못 읽었다: 다시 시도
        compose.runOnIdle { state = DrawScreenState.Failed }
        state("08-failed") {
            compose.onNodeWithTag("draw-retry").performScrollTo().performClick()
            compose.runOnIdle { assertEquals(1, retries) }
        }
        // 09 · 10 · 11 — 큰 글씨 · 좁은 폭 · 밝은 테마: 주 버튼이 화면 안에 온전히 있다
        compose.runOnIdle { state = DrawScreenState.Ready(FRESH); large = true }
        state("09-large-font") { actionInside() }
        compose.runOnIdle { large = false; narrow = true; tab = DrawKind.PREMIUM; state = DrawScreenState.Ready(LINKED) }
        state("10-narrow-320") { actionInside() }
        compose.runOnIdle { narrow = false; light = true; tab = DrawKind.FREE; state = DrawScreenState.Ready(FRESH) }
        state("11-light-theme") { actionInside() }
    }

    /** 앱 셸 안에서: 신발 탭 → 뽑기 → 한 번 뽑기(두 번 눌러도 한 번) → 결과 → 내 신발 */
    @Test fun drawFlowUsesTheServerResult() {
        val shoe = runBlocking {
            ServiceLocator.userPrefs.setReducedMotion(true)
            ServiceLocator.userPrefs.setSounds(false)
            ServiceLocator.userPrefs.setHaptics(false)
            ServiceLocator.userPrefs.setGuideSeen()
            ServiceLocator.sneakerRepository.ensureStarter()
            ServiceLocator.sneakerRepository.inventory.first().first()
        }
        val server = FakeDrawServer(shoe)
        ServiceLocator.useDrawForTest(server)
        try {
            edgeToEdge()
            compose.setContent {
                StepUpTheme(ThemeMode.DARK) { ExperienceProvider { MainScaffold(initialTab = Screen.Customize) } }
            }
            awaitTag("shoes-section-draw")
            compose.onNodeWithTag("shoes-section-draw").performClick()
            awaitTag("draw-free-left")
            compose.waitUntil(10_000) { runCatching { text("draw-free-left", "13회") }.isSuccess }
            shot("20-in-app-free")

            // 서버가 답을 붙잡고 있는 동안: 돌고, 다시 눌러도 두 번 뽑지 않는다
            val gate = CompletableDeferred<Unit>()
            server.gate = gate
            compose.onNodeWithTag("draw-shoe").performClick()
            compose.waitUntil(5_000) { server.draws.get() == 1 }
            compose.onNodeWithTag("draw-shoe").assertIsNotEnabled()
            shot("21-drawing")
            compose.onNodeWithTag("draw-shoe").performClick()
            gate.complete(Unit)
            awaitTag("draw-result")
            assertEquals("one tap, one draw", 1, server.draws.get())
            assertEquals(listOf(DrawKind.FREE), server.kinds)
            // 결과를 받은 뒤 수를 다시 읽는다 — 버튼은 새 수로 풀린다
            compose.waitUntil(5_000) { runCatching { text("draw-free-left", "12회") }.isSuccess }
            shot("22-result", settle = 1_500)
            compose.onNodeWithTag("draw-result-shoes").performClick()
            awaitTag("shoes-section-mine")
            compose.onNodeWithTag("shoes-section-mine").assertIsSelected()
            compose.onAllNodesWithTag("draw-result").assertCountEquals(0)

            // 상급 칸(지갑 연결 뒤)으로 한 번 더
            server.linked = true
            compose.onNodeWithTag("shoes-section-draw").performClick()
            awaitTag("draw-tab-premium")
            compose.onNodeWithTag("draw-tab-premium").performClick()
            awaitTag("draw-premium-left")
            compose.onNodeWithTag("draw-shoe").performClick()
            awaitTag("draw-result")
            assertEquals(listOf(DrawKind.FREE, DrawKind.PREMIUM), server.kinds)
            compose.onNodeWithTag("draw-result-close").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("draw-result").fetchSemanticsNodes().isEmpty() }
            assertTrue("status is read again after each draw", server.statusReads.get() >= 4)
        } finally {
            ServiceLocator.useDrawForTest(null)
        }
    }

    // ── 도우미 ─────────────────────────────────────────────────────

    private fun state(name: String, check: () -> Unit) {
        compose.waitForIdle()
        check()
        shot(name)
    }

    private fun text(tag: String, value: String) {
        compose.onNodeWithTag(tag, useUnmergedTree = true).assert(hasText(value, substring = true))
    }

    /** 가격 · 결제 문구가 없다 — 모든 뽑기는 무료 */
    private fun noPrice() {
        compose.onAllNodesWithText("SUP", substring = true).assertCountEquals(0)
    }

    /** 주 버튼이 화면 안에 온전히 있다(큰 글씨 · 좁은 폭에서 잘리지 않는다) */
    private fun actionInside() {
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val tag = if (compose.onAllNodesWithTag("draw-shoe").fetchSemanticsNodes().isNotEmpty()) "draw-shoe" else "draw-connect-wallet"
        val action = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        val minHeight = with(compose.density) { 55.dp.toPx() }
        assertTrue("action inside the screen: $action in $root",
            action.left >= root.left - 1 && action.right <= root.right + 1 && action.bottom <= root.bottom + 1 &&
                action.height >= minHeight)
    }

    private fun edgeToEdge() {
        runBlocking {
            ServiceLocator.userPrefs.setReducedMotion(true)
            ServiceLocator.userPrefs.setSounds(false)
            ServiceLocator.userPrefs.setHaptics(false)
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

    /** 뽑기 서버 흉내 — 수는 서버처럼 뽑을 때마다 줄고, [gate] 가 있으면 뽑기 답을 붙잡는다 */
    private class FakeDrawServer(private val shoe: Sneaker) : DrawSource {
        val draws = AtomicInteger(0)
        val statusReads = AtomicInteger(0)
        val kinds = java.util.concurrent.CopyOnWriteArrayList<DrawKind>()
        @Volatile var gate: CompletableDeferred<Unit>? = null
        @Volatile var linked = false
        @Volatile private var free = 13
        @Volatile private var premium = 10

        override fun ready() = true

        override suspend fun status(): ServerResult<DrawStatus> {
            statusReads.incrementAndGet()
            val daily = (free - 10).coerceAtLeast(0)
            return ServerResult.Ok(
                (if (linked) LINKED else FRESH).copy(
                    dailyLeft = daily, signupLeft = free - daily, giftLeft = if (linked) premium else 0,
                ),
            )
        }

        override suspend fun draw(kind: DrawKind): Pair<EconomyOutcome, Sneaker?> {
            draws.incrementAndGet()
            kinds += kind
            gate?.await()
            gate = null
            if (kind == DrawKind.FREE) free-- else premium--
            return EconomyOutcome.Ok to shoe
        }
    }

    private companion object {
        val FRESH = DrawStatus(
            dailyLeft = 3, dailyTotal = 3, signupLeft = 10, signupGranted = 10, walletLinked = false, giftOnLink = 10,
            giftLeft = 0, runLeft = 0, genesisLeft = 0, runProgressMeters = 0.0, runStepMeters = 1000,
            runToday = 0, runDailyCap = 10, chainPaused = false,
        )
        val LINKED = FRESH.copy(walletLinked = true, giftOnLink = 0, giftLeft = 10, genesisLeft = 1, runProgressMeters = 600.0)
    }
}
