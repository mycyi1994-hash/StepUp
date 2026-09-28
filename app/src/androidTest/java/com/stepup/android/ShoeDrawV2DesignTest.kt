package com.stepup.android

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.systemBarsPadding
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
import com.stepup.android.domain.DrawKind
import com.stepup.android.domain.Rarity
import com.stepup.android.ui.BOTTOM_NAV_TAG
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Screen
import com.stepup.android.ui.components.CommerceBackdrop
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.gacha.DrawFlow
import com.stepup.android.ui.screens.gacha.DrawNotice
import com.stepup.android.ui.screens.gacha.DrawPending
import com.stepup.android.ui.screens.gacha.DrawScreenState
import com.stepup.android.ui.screens.gacha.DrawSheet
import com.stepup.android.ui.screens.gacha.DrawStop
import com.stepup.android.ui.screens.gacha.DrawnShoe
import com.stepup.android.ui.screens.gacha.MysteryBoxScreen
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * 신발 뽑기 v2(두 칸) 시안 장면 — docs/redesign/shoe-draw-v2, 캡처는 `shoe-draw-v2/`.
 *
 * - [drawInTheApp] — 앱 셸 안의 실제 흐름: 신발 탭(00) → 뽑기(이 기기의 실제 서버 상태 그대로 — 로그인 전이면 로그인 안내) → 뒤로 내 신발.
 * - [drawFlowWithAnExampleServer] — 앱 셸 안, 흉내 낸 서버: 두 칸 → 결과 확인 중(10) → 결과(12) → 받은 신발이 골라진 내 신발(21).
 * - [drawScenes] — 시안 장면 번호대로 상태를 화면에 바로 넣어 찍는다. 시안의 13회 · 10회 · 0.6km 같은 예시 수는 이 검사 화면에만 있다
 *   (앱은 서버가 준 수만 보인다). 앱이 알 수 없는 07 · 09 · 25 · 27 은 만들지 않았다(README).
 */
class ShoeDrawV2DesignTest {
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

    private val directory get() = File(compose.activity.getExternalFilesDir(null), "shoe-draw-v2").apply { mkdirs() }

    @Test fun drawInTheApp() {
        prepare()
        edgeToEdge()
        compose.setContent { StepUpTheme(ThemeMode.DARK) { ExperienceProvider { MainScaffold(initialTab = Screen.Customize) } } }
        awaitTag("shoes-section-draw")
        shot("00-owned-shoes-entry")
        tap("shoes-section-draw")
        awaitTag("draw-home")
        // 이 기기의 실제 상태 — 읽는 동안은 자리만, 로그인 전이면 로그인 안내(0 을 임시로 보이지 않는다)
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("draw-skeleton", useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag(BOTTOM_NAV_TAG).assertDoesNotExist()
        compose.onNodeWithTag("draw-free-action").assertExists()
        compose.onNodeWithTag("draw-premium-action").assertExists()
        shot("a01-in-app-real-state")
        // 서버를 읽지 못한 기기면 23 시트가 떠 있다 — 뒤로는 먼저 시트만 닫는다
        if (compose.onAllNodesWithTag("draw-sheet-load-failed", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) back()
        back()
        awaitTag("shoes-section-draw")
        shot("a02-back-to-my-shoes")
    }

    @Test fun drawFlowWithAnExampleServer() {
        prepare()
        val shoe = DrawSamples.addShoe()
        val server = FakeDrawSource(shoe)
        ServiceLocator.useDrawForTest(server)
        try {
            edgeToEdge()
            compose.setContent { StepUpTheme(ThemeMode.DARK) { ExperienceProvider { MainScaffold(initialTab = Screen.Customize) } } }
            tap("shoes-section-draw")
            awaitTag("draw-free-left")
            compose.waitUntil(10_000) {
                runCatching { compose.onNodeWithTag("draw-free-left", useUnmergedTree = true).assert(hasText("13회")) }.isSuccess
            }
            shot("b01-in-app-two-compartments")
            val gate = CompletableDeferred<Unit>()
            server.gate = gate
            tap("draw-free-action")
            awaitTag("draw-requesting")
            shot("b10-in-app-request")
            gate.complete(Unit)
            awaitTag("draw-result")
            compose.waitUntil(5_000) {
                runCatching { compose.onNodeWithTag("draw-result-left").assert(hasText("12회", substring = true)) }.isSuccess
            }
            shot("b12-in-app-result", settle = 1_200)
            tap("draw-result-shoes")
            awaitTag("shoe-list")
            compose.onNodeWithTag("shoe-list").performScrollToNode(hasTestTag("shoe-choice-${shoe.id}"))
            compose.waitUntil(5_000) { runCatching { compose.onNodeWithTag("shoe-choice-${shoe.id}").assertIsSelected() }.isSuccess }
            compose.onNodeWithTag("shoe-list").performScrollToIndex(0)
            shot("b21-in-app-owned-shoes")
            assertEquals(1, server.draws.get())
        } finally {
            ServiceLocator.useDrawForTest(null)
            DrawSamples.removeShoe(shoe)
        }
    }

    @Test fun drawScenes() {
        var light by mutableStateOf(false)
        var large by mutableStateOf(false)
        var narrow by mutableStateOf(false)
        prepare()
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
                                    .align(Alignment.TopCenter).systemBarsPadding(),
                            ) { scene() }
                        }
                    }
                }
            }
        }
        val fresh = DrawScreenState.Ready(DrawSamples.FRESH)
        val linked = DrawScreenState.Ready(DrawSamples.LINKED)
        val empty = DrawScreenState.Ready(DrawSamples.EMPTY)
        val shoe = DrawSamples.shoe()
        fun label(id: Int, vararg args: Any) = compose.activity.getString(id, *args)
        fun draw(
            state: DrawScreenState,
            flow: DrawFlow = DrawFlow.Home,
            pending: DrawPending? = null,
            notice: DrawNotice? = null,
            sheet: DrawSheet? = null,
            openingAt: Float? = null,
        ): @Composable () -> Unit = {
            MysteryBoxScreen(state = state, flow = flow, pending = pending, notice = notice, initialSheet = sheet, openingAt = openingAt)
        }

        // 01 · 02 — 두 칸(연결 전 · 연결 뒤)
        show("01-home-unconnected", "draw-free-left", draw(fresh))
        compose.onNodeWithTag("draw-free-left", useUnmergedTree = true).assert(hasText("13회"))
        compose.onNodeWithTag("draw-premium-gift", useUnmergedTree = true).assert(hasText("+10회"))
        compose.onAllNodesWithText("SUP", substring = true).assertCountEquals(0)
        show("02-home-connected", "draw-premium-left", draw(linked))
        compose.onNodeWithText(label(R.string.dv2_next_premium, "0.4")).assertExists()
        // 03 · 04 · 05 · 06 — 내역 · 러닝 진행 · 받는 방법 · 연결 혜택(여닫아도 기회를 쓰지 않는다)
        show("03-free-chances-sheet", "draw-sheet-free", draw(fresh, sheet = DrawSheet.FreeChances))
        compose.onNodeWithTag("draw-sheet-free-total").assert(hasText("13회", substring = true))
        show("04-premium-chances-sheet", "draw-sheet-premium", draw(linked, sheet = DrawSheet.PremiumChances))
        compose.onNodeWithText(label(R.string.dv2_progress, "0.6", "1")).assertExists()
        show("05-reward-rules-sheet", "draw-sheet-rules", draw(fresh, sheet = DrawSheet.Rules))
        show("06-wallet-benefit-sheet", "draw-sheet-benefit", draw(fresh, sheet = DrawSheet.WalletBenefit))
        // 08 — 돌아와 읽은 서버 값에서 연결 · 첫 연결 선물을 확인했을 때만
        show("08-wallet-connected", "draw-sheet-linked", draw(linked, notice = DrawNotice.Linked(10)))
        // 10 · 11 · 12 · 13 · 14 — 뽑기 흐름(결과는 서버가 준 신발)
        show("10-draw-request", "draw-requesting", draw(fresh, flow = DrawFlow.Requesting(DrawKind.FREE)))
        compose.onAllNodesWithTag("draw-free-action").assertCountEquals(0)
        show("11-draw-opening", "draw-opening", draw(fresh, flow = DrawFlow.Opening(DrawnShoe(DrawKind.FREE, shoe)), openingAt = 1_000f))
        show("11b-draw-opening-lid", "draw-opening", draw(fresh, flow = DrawFlow.Opening(DrawnShoe(DrawKind.FREE, shoe)), openingAt = 380f))
        show("11c-draw-opening-landing", "draw-opening", draw(fresh, flow = DrawFlow.Opening(DrawnShoe(DrawKind.FREE, shoe)), openingAt = 1_450f))
        val afterFree = DrawScreenState.Ready(DrawSamples.FRESH.copy(dailyLeft = 2))
        show("12-free-draw-result", "draw-result", draw(afterFree, flow = DrawFlow.Result(DrawnShoe(DrawKind.FREE, shoe, left = 12))))
        compose.onNodeWithTag("draw-result-left").assert(hasText("12회", substring = true))
        compose.onNodeWithTag("draw-result-again").assertExists()
        val epic = DrawSamples.shoe(id = 9_002, rarity = Rarity.EPIC, variant = 0)
        val afterPremium = DrawScreenState.Ready(DrawSamples.LINKED.copy(giftLeft = 9))
        show("13-premium-draw-result", "draw-result", draw(afterPremium, flow = DrawFlow.Result(DrawnShoe(DrawKind.PREMIUM, epic, left = 9))))
        val lastFree = DrawScreenState.Ready(DrawSamples.FRESH.copy(dailyLeft = 0, signupLeft = 0))
        show("14-last-chance-result", "draw-result", draw(lastFree, flow = DrawFlow.Result(DrawnShoe(DrawKind.FREE, shoe, left = 0))))
        compose.onNodeWithTag("draw-result-home").assertExists()
        compose.onAllNodesWithTag("draw-result-again").assertCountEquals(0)
        // 15 · 16 · 17 — 기회 소진 · 안내
        show("15-home-no-chances", "draw-free-left", draw(empty))
        show("16-free-no-chances", "draw-sheet-free-empty", draw(empty, sheet = DrawSheet.FreeEmpty))
        show("17-premium-no-chances", "draw-sheet-run", draw(empty, sheet = DrawSheet.RunChances))
        // 18 — 러닝 반영으로 상급 +1(서버 값이 늘었을 때 짧게)
        val runOne = DrawScreenState.Ready(DrawSamples.EMPTY.copy(runLeft = 1, runProgressMeters = 0.0))
        show("18-running-reward", "settings-toast", draw(runOne, notice = DrawNotice.RunReward(1)))
        // 19 · 20 — 시작 실패(기회를 쓰지 않았다) · 결과 확인 지연(새로 뽑지 않는다)
        show("19-draw-not-started", "draw-sheet-not-started", draw(fresh, notice = DrawNotice.NotStarted(DrawKind.FREE, DrawStop.Network)))
        compose.onNodeWithText(label(R.string.dv2_stop_unused)).assertExists()
        show("20-draw-reconcile", "draw-checking", draw(fresh, flow = DrawFlow.Checking(DrawKind.FREE, busy = false)))
        show("20b-draw-reconcile-unknown", "draw-checking", draw(fresh, flow = DrawFlow.Checking(DrawKind.FREE, busy = false, tried = true)))
        // 22 · 23 — 불러오는 중(0 을 보이지 않는다) · 불러오기 실패(소진과 다르다, 닫아도 설명과 다시 불러오기가 남는다)
        show("22-home-loading", "draw-skeleton", draw(DrawScreenState.Loading))
        compose.onAllNodesWithText("0회").assertCountEquals(0)
        show("23-home-load-error", "draw-sheet-load-failed", draw(DrawScreenState.Failed, notice = DrawNotice.LoadFailed))
        show("23b-home-load-error-closed", "draw-free-failed", draw(DrawScreenState.Failed))
        compose.onAllNodesWithText("0회").assertCountEquals(0)
        // 24 — 연결했다가 해제된 기존 회원: 받은 기회는 그대로, 다시 연결(선물은 다시 주지 않는다)
        val unlinked = DrawScreenState.Ready(DrawSamples.LINKED.copy(walletLinked = false))
        show("24-wallet-reconnect", "draw-premium-left", draw(unlinked))
        compose.onNodeWithTag("draw-premium-action").assertTextContains(label(R.string.dv2_action_reconnect))
        // 26 — 결과를 아직 보지 않은 요청: 새 뽑기는 막고 "결과 확인"만
        show("26-home-pending-result", "draw-pending", draw(linked, pending = DrawPending(DrawKind.FREE, ready = false)))
        compose.onNodeWithTag("draw-free-action").assertTextContains(label(R.string.dv2_action_check))
        // 서버 값으로만 생기는 상태 — 로그인 전 · 체인 멈춤
        show("s01-signed-out", "draw-free-action", draw(DrawScreenState.SignedOut))
        show("s02-chain-paused", "draw-premium-left", draw(DrawScreenState.Ready(DrawSamples.LINKED.copy(chainPaused = true))))
        // 밝은 테마 · 큰 글씨 · 320dp
        compose.runOnIdle { light = true }
        show("v01-light-home", "draw-free-left", draw(fresh))
        show("v02-light-connected", "draw-premium-left", draw(linked))
        show("v03-light-free-sheet", "draw-sheet-free", draw(fresh, sheet = DrawSheet.FreeChances))
        show("v04-light-result", "draw-result", draw(afterFree, flow = DrawFlow.Result(DrawnShoe(DrawKind.FREE, shoe, left = 12))))
        show("v05-light-opening", "draw-opening", draw(fresh, flow = DrawFlow.Opening(DrawnShoe(DrawKind.FREE, shoe)), openingAt = 1_000f))
        compose.runOnIdle { light = false; large = true }
        show("v06-large-font-home", "draw-free-left", draw(fresh))
        show("v07-large-font-result", "draw-result", draw(afterFree, flow = DrawFlow.Result(DrawnShoe(DrawKind.FREE, shoe, left = 12))))
        show("v08-large-font-premium-sheet", "draw-sheet-premium", draw(linked, sheet = DrawSheet.PremiumChances))
        compose.runOnIdle { large = false; narrow = true }
        show("v09-narrow-home", "draw-free-left", draw(linked))
        show("v10-narrow-result", "draw-result", draw(afterFree, flow = DrawFlow.Result(DrawnShoe(DrawKind.FREE, shoe, left = 12))))
    }

    // ── 도우미 ─────────────────────────────────────────────────────

    private var scene by mutableStateOf<@Composable () -> Unit>({})

    private fun show(name: String, readyTag: String, content: @Composable () -> Unit) {
        compose.runOnIdle { scene = { key(name) { content() } } }
        awaitTag(readyTag)
        shot(name)
    }

    private fun prepare() = runBlocking {
        ServiceLocator.userPrefs.setReducedMotion(true)
        ServiceLocator.userPrefs.setSounds(false)
        ServiceLocator.userPrefs.setHaptics(false)
        ServiceLocator.userPrefs.setGuideSeen()
        ServiceLocator.sneakerRepository.ensureStarter()
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

    private fun edgeToEdge() {
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
