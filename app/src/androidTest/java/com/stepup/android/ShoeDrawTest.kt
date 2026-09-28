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
import com.stepup.android.data.repo.EconomyOutcome
import com.stepup.android.domain.DrawKind
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Screen
import com.stepup.android.ui.components.CommerceBackdrop
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.gacha.DrawActions
import com.stepup.android.ui.screens.gacha.DrawReply
import com.stepup.android.ui.screens.gacha.DrawScreenState
import com.stepup.android.ui.screens.gacha.MysteryBoxScreen
import com.stepup.android.ui.screens.gacha.PendingDraw
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 신발 뽑기 v2(두 칸, 2026-09-28) — 뽑기 규칙. 서버는 흉내 낸 DrawSource 다(서버 규칙 자체는 supabase/tests 의 0042 검사).
 *
 * - 두 칸의 상태(값 · 소진 · 로그인 전 · 불러오는 중 · 실패 · 큰 글씨 · 좁은 폭 · 밝은 테마) — 0 을 임시로 보이지 않고 가격이 없다
 * - 앱 셸 안: 한 번 누르면 한 번만 뽑고(10 에는 버튼이 없다), 서버 결과로만 결과 화면, 뒤에 수를 다시 읽는다, 내 신발에서 그 신발이 골라져 있다
 * - 답을 잃은 요청은 새로 뽑지 않고 확인한다(20 → 결과), 뒤로 가도(26) 새 뽑기는 막힌다
 * - 서버가 거절하면 기회를 쓰지 않았다고 말한다(19), 다시 열면 남은 요청부터 확인한다
 *
 * 시안 장면 캡처는 [ShoeDrawV2DesignTest].
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
        var state by mutableStateOf<DrawScreenState>(DrawScreenState.Ready(DrawSamples.FRESH))
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
                            CommerceBackdrop(Modifier.fillMaxSize())
                            Box(
                                Modifier.then(if (narrow) Modifier.requiredWidth(320.dp).fillMaxHeight() else Modifier.fillMaxSize())
                                    .align(Alignment.TopCenter).statusBarsPadding(),
                            ) {
                                MysteryBoxScreen(state = state, actions = DrawActions(onRetry = { retries++ }))
                            }
                        }
                    }
                }
            }
        }
        val context = compose.activity
        fun label(id: Int, vararg args: Any) = context.getString(id, *args)

        // 01 — 처음 가입한 날: 가입 선물 10 + 오늘 3 = 13, 상급은 지갑 연결 안내
        state("01-two-compartments") {
            text("draw-free-left", "13회")
            compose.onNodeWithText(label(R.string.dv2_free_rule, 10, 3)).assertExists()
            compose.onNodeWithTag("draw-free-action").assertIsEnabled().assertTextContains(label(R.string.dv2_action_free))
            compose.onNodeWithTag("draw-premium-action").performScrollTo().assertTextContains(label(R.string.dv2_action_connect, 10))
            text("draw-premium-gift", "+10회")
            noPrice()
        }
        // 02 — 지갑 연결 뒤: 선물 10 · 0.6 / 1km
        compose.runOnIdle { state = DrawScreenState.Ready(DrawSamples.LINKED) }
        state("02-connected") {
            compose.onNodeWithTag("draw-premium-left", useUnmergedTree = true).performScrollTo()
            text("draw-premium-left", "10회")
            compose.onNodeWithText(label(R.string.dv2_next_premium, "0.4")).assertExists()
            compose.onNodeWithTag("draw-premium-action").assertIsEnabled().assertTextContains(label(R.string.dv2_action_premium))
        }
        // 03 — 오늘 러닝 한도를 채웠다: 다음 1회까지 거리 대신 한도 안내
        compose.runOnIdle { state = DrawScreenState.Ready(DrawSamples.LINKED.copy(runLeft = 10, runToday = 10, runProgressMeters = 300.0)) }
        state("03-run-cap") {
            compose.onNodeWithText(label(R.string.dv2_run_capped)).performScrollTo().assertExists()
            text("draw-premium-left", "20회")
        }
        // 04 — 둘 다 소진: 누를 수 있지만 뽑지 않는 안내 버튼(무료 기회 안내 · 러닝하고 기회 받기)
        compose.runOnIdle { state = DrawScreenState.Ready(DrawSamples.EMPTY) }
        state("04-used-up") {
            text("draw-free-left", "0회")
            compose.onNodeWithTag("draw-free-action").assertTextContains(label(R.string.dv2_action_free_info))
            compose.onNodeWithTag("draw-premium-action").performScrollTo().assertTextContains(label(R.string.dv2_action_run))
        }
        // 05 — 로그인 전: 누를 수 없고 수가 없다
        compose.runOnIdle { state = DrawScreenState.SignedOut }
        state("05-signed-out") {
            compose.onNodeWithTag("draw-free-action").assertIsNotEnabled()
            compose.onAllNodesWithText("0회").assertCountEquals(0)
        }
        // 06 — 읽는 중: 0회가 아니라 자리만
        compose.runOnIdle { state = DrawScreenState.Loading }
        state("06-loading") {
            compose.onAllNodesWithTag("draw-skeleton").assertCountEquals(2)
            compose.onAllNodesWithText("0회").assertCountEquals(0)
            compose.onNodeWithTag("draw-free-action").assertIsNotEnabled()
        }
        // 07 — 못 읽었다: 소진과 다르다. 칸에 설명과 다시 불러오기
        compose.runOnIdle { state = DrawScreenState.Failed }
        state("07-failed") {
            compose.onNodeWithTag("draw-free-failed", useUnmergedTree = true).assertExists()
            compose.onAllNodesWithText("0회").assertCountEquals(0)
            compose.onNodeWithTag("draw-free-action").performClick()
            compose.runOnIdle { assertEquals(1, retries) }
        }
        // 08 · 09 · 10 — 큰 글씨 · 좁은 폭 · 밝은 테마: 칸의 버튼이 화면 안에 온전히 있다
        compose.runOnIdle { state = DrawScreenState.Ready(DrawSamples.FRESH); large = true }
        state("08-large-font") { actionInside("draw-free-action") }
        compose.runOnIdle { large = false; narrow = true; state = DrawScreenState.Ready(DrawSamples.LINKED) }
        state("09-narrow-320") { actionInside("draw-free-action") }
        compose.runOnIdle { narrow = false; light = true; state = DrawScreenState.Ready(DrawSamples.FRESH) }
        state("10-light-theme") { actionInside("draw-free-action") }
    }

    /** 앱 셸 안에서: 신발 탭 → 뽑기 → 한 번 뽑기(두 번 눌러도 한 번) → 결과 → 내 신발(그 신발이 골라져 있다) */
    @Test fun drawFlowUsesTheServerResult() {
        prepare()
        val shoe = DrawSamples.addShoe()
        val equipped = DrawSamples.equippedId()
        val server = FakeDrawSource(shoe)
        ServiceLocator.useDrawForTest(server)
        try {
            edgeToEdge()
            compose.setContent { StepUpTheme(ThemeMode.DARK) { ExperienceProvider { MainScaffold(initialTab = Screen.Customize) } } }
            awaitTag("shoes-section-draw")
            compose.onNodeWithTag("shoes-section-draw").performClick()
            awaitTag("draw-free-left")
            compose.waitUntil(10_000) { runCatching { text("draw-free-left", "13회") }.isSuccess }
            shot("20-in-app-free")

            // 서버가 답을 붙잡고 있는 동안(10): 뽑기 버튼이 없다 — 두 번 뽑을 수 없다
            val gate = CompletableDeferred<Unit>()
            server.gate = gate
            compose.onNodeWithTag("draw-free-action").performClick()
            compose.waitUntil(5_000) { server.draws.get() == 1 }
            awaitTag("draw-requesting")
            compose.onAllNodesWithTag("draw-free-action").assertCountEquals(0)
            shot("21-requesting")
            gate.complete(Unit)
            awaitTag("draw-result")
            assertEquals("one tap, one draw", 1, server.draws.get())
            assertEquals(listOf(DrawKind.FREE), server.kinds)
            // 결과를 받은 뒤 수를 다시 읽는다 — 결과의 남은 수는 서버가 준 새 수
            compose.waitUntil(5_000) {
                runCatching { compose.onNodeWithTag("draw-result-left").assert(hasText("12회", substring = true)) }.isSuccess
            }
            compose.onNodeWithTag("draw-result-name", useUnmergedTree = true).assertExists()
            shot("22-result", settle = 1_500)
            compose.onNodeWithTag("draw-result-shoes").performClick()
            awaitTag("shoe-list")
            compose.onNodeWithTag("shoe-list").performScrollToNode(hasTestTag("shoe-choice-${shoe.id}"))
            compose.waitUntil(5_000) { runCatching { compose.onNodeWithTag("shoe-choice-${shoe.id}").assertIsSelected() }.isSuccess }
            compose.onNodeWithTag("shoe-list").performScrollToIndex(0)
            assertEquals("seeing the new shoe does not equip it", equipped, DrawSamples.equippedId())
            shot("23-my-shoes-with-the-new-shoe")

            // 상급 칸(지갑 연결 뒤)으로 한 번 더 — 결과에서 뒤로 가면 두 칸
            server.linked = true
            compose.onNodeWithTag("shoes-section-draw").performClick()
            awaitTag("draw-premium-left")
            compose.onNodeWithTag("draw-premium-action").performScrollTo().performClick()
            awaitTag("draw-result")
            assertEquals(listOf(DrawKind.FREE, DrawKind.PREMIUM), server.kinds)
            back()
            awaitTag("draw-home")
            assertTrue("status is read again after each draw", server.statusReads.get() >= 4)
        } finally {
            ServiceLocator.useDrawForTest(null)
            DrawSamples.removeShoe(shoe)
        }
    }

    /** 답을 잃었다(20) — 새로 뽑지 않고 확인해서 결과로. 뒤로 가도(26) 새 뽑기는 막힌다 */
    @Test fun unknownAnswerNeverDrawsTwice() {
        prepare()
        val shoe = DrawSamples.shoe()
        val server = FakeDrawSource(shoe)
        ServiceLocator.useDrawForTest(server)
        try {
            edgeToEdge()
            compose.setContent { StepUpTheme(ThemeMode.DARK) { ExperienceProvider { MainScaffold(initialTab = Screen.Customize) } } }
            awaitTag("shoes-section-draw")
            compose.onNodeWithTag("shoes-section-draw").performClick()
            awaitTag("draw-free-left")
            server.nextReply = DrawReply.Unknown
            val gate = CompletableDeferred<Unit>()
            server.gate = gate
            compose.onNodeWithTag("draw-free-action").performClick()
            compose.waitUntil(5_000) { server.draws.get() == 1 }
            // 10 이 그려진 뒤에 뒤로 — 그리기 전(같은 프레임)에 누르면 뽑기 화면의 뒤로가 아직 켜지지 않아 신발 탭으로 나간다
            awaitTag("draw-requesting")
            // 10 에서 뒤로 — 요청은 그대로, 두 칸의 버튼은 "결과 확인"(26)
            back()
            awaitTag("draw-pending")
            compose.onNodeWithTag("draw-free-action").assertTextContains(compose.activity.getString(R.string.dv2_action_check))
            compose.onNodeWithTag("draw-premium-action").performScrollTo()
                .assertTextContains(compose.activity.getString(R.string.dv2_action_check))
            shot("30-pending-home")
            gate.complete(Unit)
            // 답을 잃었다 — 잠시 뒤 서버 목록을 다시 읽어 확인한다(새로 뽑지 않는다)
            compose.waitUntil(15_000) { server.checks.get() >= 1 }
            compose.onNodeWithTag("draw-free-action").performScrollTo().performClick()
            awaitTag("draw-result", 15_000)
            assertEquals("the lost answer is checked, not drawn again", 1, server.draws.get())
            shot("31-checked-result", settle = 1_200)
        } finally {
            ServiceLocator.useDrawForTest(null)
        }
    }

    /** 서버가 거절했다(19) — 기회를 쓰지 않았다고 말한다. 다시 눌러도 같은 까닭이면 "다시 뽑기"를 두지 않는다 */
    @Test fun refusedDrawSaysNoChanceWasUsed() {
        prepare()
        val server = FakeDrawSource(DrawSamples.shoe())
        ServiceLocator.useDrawForTest(server)
        try {
            edgeToEdge()
            compose.setContent { StepUpTheme(ThemeMode.DARK) { ExperienceProvider { MainScaffold(initialTab = Screen.Customize) } } }
            awaitTag("shoes-section-draw")
            compose.onNodeWithTag("shoes-section-draw").performClick()
            awaitTag("draw-free-left")
            server.nextReply = DrawReply.Refused(EconomyOutcome.NoFreeDraws)
            compose.onNodeWithTag("draw-free-action").performClick()
            awaitTag("draw-sheet-not-started")
            compose.onNodeWithText(compose.activity.getString(R.string.dv2_stop_unused)).assertExists()
            compose.onAllNodesWithTag("draw-sheet-retry").assertCountEquals(0)
            shot("40-not-started")
            compose.onNodeWithTag("draw-sheet-ok").performClick()
            awaitGone("draw-sheet-not-started")
            awaitTag("draw-home")
        } finally {
            ServiceLocator.useDrawForTest(null)
        }
    }

    /** 앱을 다시 열었다 — 남은 요청부터 확인한다(새로 뽑지 않는다) */
    @Test fun reopenedAppChecksTheLeftoverRequest() {
        prepare()
        val shoe = DrawSamples.shoe()
        val server = FakeDrawSource(shoe)
        // 앞 실행이 보내 놓고 답을 받지 못한 요청 — 서버에서는 처리됐다
        runBlocking {
            server.draw(DrawKind.FREE)
            server.draws.set(0)
            server.kinds.clear()
        }
        server.memory.keep(
            PendingDraw(DrawKind.FREE, newestBefore = shoe.id - 1, leftBefore = 13, startedAt = System.currentTimeMillis() - 120_000, account = "draw-test"),
        )
        ServiceLocator.useDrawForTest(server)
        try {
            edgeToEdge()
            compose.setContent { StepUpTheme(ThemeMode.DARK) { ExperienceProvider { MainScaffold(initialTab = Screen.Customize) } } }
            awaitTag("shoes-section-draw")
            compose.onNodeWithTag("shoes-section-draw").performClick()
            awaitTag("draw-pending")
            compose.waitUntil(10_000) { server.checks.get() >= 1 }
            compose.onNodeWithTag("draw-free-action").performClick()
            awaitTag("draw-result", 10_000)
            assertEquals("a leftover request is checked, not drawn again", 0, server.draws.get())
            assertTrue("the leftover request is cleared", server.memory.pending() == null)
        } finally {
            ServiceLocator.useDrawForTest(null)
        }
    }

    // ── 도우미 ─────────────────────────────────────────────────────

    private fun prepare() = runBlocking {
        ServiceLocator.userPrefs.setReducedMotion(true)
        ServiceLocator.userPrefs.setSounds(false)
        ServiceLocator.userPrefs.setHaptics(false)
        ServiceLocator.userPrefs.setGuideSeen()
        ServiceLocator.sneakerRepository.ensureStarter()
    }

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

    /** 칸의 버튼이 화면 안에 온전히 있다(큰 글씨 · 좁은 폭에서 잘리지 않는다) */
    private fun actionInside(tag: String) {
        compose.onNodeWithTag(tag).performScrollTo()
        compose.waitForIdle()
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val action = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        val minHeight = with(compose.density) { 49.dp.toPx() }
        assertTrue("action inside the screen: $action in $root",
            action.left >= root.left - 1 && action.right <= root.right + 1 && action.bottom <= root.bottom + 1 &&
                action.height >= minHeight)
    }

    private fun back() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
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

    private fun awaitTag(tag: String, timeout: Long = 10_000) {
        compose.waitUntil(timeout) { compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
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
