package com.stepup.android

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
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
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.gacha.DrawActions
import com.stepup.android.ui.screens.gacha.DrawBackdrop
import com.stepup.android.ui.screens.gacha.DrawFlow
import com.stepup.android.ui.screens.gacha.DrawNotice
import com.stepup.android.ui.screens.gacha.DrawReply
import com.stepup.android.ui.screens.gacha.DrawScreenState
import com.stepup.android.ui.screens.gacha.DrawSheet
import com.stepup.android.ui.screens.gacha.DrawStop
import com.stepup.android.ui.screens.gacha.DrawnShoe
import com.stepup.android.ui.screens.gacha.MysteryBoxScreen
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 신발 뽑기 장면 — 2026-10-03 파란 톤 통합 전달본 v4(docs/redesign/blue-v4-2026-10/01-packages/stepup-draw-blue-claude-v19)부터
 * 무료 · 상급 두 패널이 한 화면에 함께 있다(예전 글자 탭 없음). 캡처는 `shoe-draw-v3/`(파일 이름 앞 번호는 예전 v3 26장 번호 그대로).
 *
 * - [inTheApp] — 앱 셸 안(공통 머리 · 하단 탭), 흉내 낸 서버: 불러오는 중(03) → 무료 탭(01) → ⓘ 무료 내역(15) → 받는 방법(17) →
 *   상급 탭 · 지갑 전(09) → 연결 혜택(10) → 연결 뒤(02) → ⓘ 상급 내역(16) → 결과 확인 중(04, 하단 탭만 걷힘) → 결과(06) → 내 신발
 * - [inTheAppStates] — 앱 셸 안: 다시 연결(12) → 일시 중단(23) → 무료 소진(13) → 상급 보기 → 러닝으로 받기(14) → 러닝 반영 알림(26) →
 *   결과를 모르는 요청(20, 두 탭 모두 "결과 확인")
 * - [signedOutThenLoadFailedInTheApp] — 로그인 전(22) → 첫 읽기 실패(21) → 닫은 뒤(21b) → 다시 불러오기
 * - [fitsAtDeviceSizes] — 앱 셸 그대로 360×800 · 390×844 · 412×915(dp) · 큰 글씨 · 밝은 테마: 메인은 스크롤 없이 실행 버튼이 하단 탭 위,
 *   결과는 신발 · 이름 · 남은 수 · 두 버튼이 한 화면에
 * - [scenes] — 서버 흐름으로 만들기 어려운 장면(05 상자 열기 · 07 · 08 · 11 · 18 · 19 · 22 · 24 · 25 · 26)을 상태로 바로 넣어 찍고,
 *   밝은 테마 · 큰 글씨 · 320dp 폭. 시안의 13회 · 10회 · 0.6km 같은 예시 수는 이 검사 화면에만 있다(앱은 서버가 준 수만 보인다).
 */
class ShoeDrawTabsDesignTest {
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

    private val directory get() = File(compose.activity.getExternalFilesDir(null), "shoe-draw-v3").apply { mkdirs() }

    private fun label(id: Int, vararg args: Any): String = compose.activity.getString(id, *args)

    @Test fun inTheApp() {
        prepare()
        val shoe = DrawSamples.addShoe()
        val equipped = DrawSamples.equippedId()
        val server = FakeDrawSource(shoe)
        val reading = CompletableDeferred<Unit>()
        server.statusGate = reading
        ServiceLocator.useDrawForTest(server)
        try {
            edgeToEdge()
            compose.setContent { StepUpTheme(ThemeMode.DARK) { ExperienceProvider { MainScaffold(initialTab = Screen.Customize) } } }
            openDrawTab()
            // 03 — 첫 읽기 중: 흐린 상자 · "—" · 누를 수 없는 버튼. 0 을 임시로 보이지 않고, 모르는 동안은 ⓘ 도 두지 않는다
            awaitTag("draw-free-unknown")
            compose.onNodeWithTag("draw-premium-unknown", useUnmergedTree = true).assertExists()
            compose.onNodeWithTag("draw-free-action").assertIsNotEnabled()
            compose.onAllNodesWithText("0회").assertCountEquals(0)
            compose.onAllNodesWithTag("draw-free-history").assertCountEquals(0)
            compose.onAllNodesWithTag("draw-rules").assertCountEquals(0)
            shot("d03-loading")
            server.statusGate = null
            reading.complete(Unit)

            // DRAW01 — 두 패널: 무료(가입 선물 10 + 오늘 3 = 13회 · 무료로 1회 뽑기)와 상급(지갑 전)이 함께. 무료 버튼은 스크롤 없이 하단 탭 위
            awaitText("draw-free-left", "13회")
            compose.onAllNodesWithTag("draw-tab-free").assertCountEquals(0)
            compose.onNodeWithTag("draw-free-detail", useUnmergedTree = true).assert(hasText(label(R.string.draw_blue_free_breakdown, 10, 3)))
            compose.onNodeWithTag("draw-free-action").assertIsEnabled().assertTextContains(label(R.string.dv2_action_free))
            compose.onNodeWithTag("draw-premium-action").assertExists()
            compose.onNodeWithTag("main-header").assertIsDisplayed()
            assertMainFits("draw-free-action", "pixel_2 free")
            shot("d01-free-main")

            // DRAW03 — 무료 패널의 "기회 내역 보기": 무료 기회 내역 → 안의 "기회 받는 방법 ›"(DRAW05). 여닫아도 기회를 쓰지 않는다
            tap("draw-free-history")
            awaitTag("draw-sheet-free")
            compose.onNodeWithTag("draw-sheet-free-total").assert(hasText("13회", substring = true))
            shot("d15-free-history")
            tap("draw-sheet-rules-link")
            awaitTag("draw-sheet-rules")
            awaitGone("draw-sheet-free")
            shot("d17-rules")
            tap("draw-sheet-ok")
            awaitGone("draw-sheet-rules")
            assertEquals("sheets never draw", 0, server.draws.get())

            // DRAW01 상급 패널 · 지갑 전: "지갑 연결 전", 처음 연결하면 받을 수 있는 10회(보유 수가 아니다), "지갑 연결하고 10회 받기"
            awaitTag("draw-premium-gift")
            compose.onNodeWithTag("draw-premium-gift").assert(hasText("10회", substring = true))
            compose.onNodeWithTag("draw-premium-sub", useUnmergedTree = true).assert(hasText(label(R.string.draw_blue_before_link)))
            compose.onAllNodesWithTag("draw-premium-history").assertCountEquals(0)
            compose.onNodeWithTag("draw-premium-action").assertTextContains(label(R.string.dv2_action_connect, 10))
            assertMainFits("draw-premium-action", "pixel_2 wallet")
            shot("d09-wallet-required")
            // 10 — 연결 혜택. "나중에"는 창만 닫는다(연결 · 뽑기 없음)
            tap("draw-premium-action")
            awaitTag("draw-sheet-benefit")
            shot("d10-wallet-benefit")
            tap("draw-sheet-later")
            awaitGone("draw-sheet-benefit")

            // DRAW02 — 지갑을 연결하고 돌아왔다(서버 값): 남은 상급 10회 · 다음 1회까지 0.4km(서버 거리) · 상급으로 1회 뽑기
            server.linked = true
            leaveAndReturn()
            awaitText("draw-premium-left", "10회")
            compose.onNodeWithTag("draw-premium-detail", useUnmergedTree = true).assert(hasText(label(R.string.dv2_next_premium, "0.4")))
            compose.onNodeWithTag("draw-premium-action").assertIsEnabled().assertTextContains(label(R.string.dv2_action_premium))
            assertMainFits("draw-premium-action", "pixel_2 premium", scroll = true)
            shot("d02-premium-main")
            // DRAW04 — 상급 패널의 "기회 내역 보기": 상급 기회 내역 · 다음 1회까지 0.4km(0.6 / 1km)
            tap("draw-premium-history")
            awaitTag("draw-sheet-premium")
            compose.onNodeWithText(label(R.string.dv2_progress, "0.6", "1")).assertExists()
            shot("d16-premium-history")
            tap("draw-sheet-close")
            awaitGone("draw-sheet-premium")

            // DRAW10 — 결과 확인 중: 로고 · 잔액 머리는 그대로, 하단 탭만 걷는다. 다시 누를 뽑기 버튼이 없다
            awaitText("draw-free-left", "13회")
            val gate = CompletableDeferred<Unit>()
            server.gate = gate
            tap("draw-free-action")
            awaitTag("draw-requesting")
            compose.waitUntil(5_000) { server.draws.get() == 1 }
            awaitGone(BOTTOM_NAV_TAG)
            compose.onNodeWithTag("main-header").assertIsDisplayed()
            compose.onAllNodesWithTag("draw-free-action").assertCountEquals(0)
            shot("d04-processing")
            gate.complete(Unit)

            // 06 — 무료 뽑기 결과: 서버가 준 신발 · 이름 끝의 작은 등급 배지 · 뽑은 뒤 다시 읽은 남은 무료 뽑기 12회
            awaitTag("draw-result")
            awaitText("draw-result-left", "12회", unmerged = false)
            compose.onNodeWithTag("draw-result-name", useUnmergedTree = true).assertIsDisplayed()
            compose.onNodeWithTag("draw-result-again").assertExists()
            compose.onAllNodesWithTag(BOTTOM_NAV_TAG).assertCountEquals(0)
            assertResultFits("pixel_2")
            shot("d06-free-result", settle = 1_200)
            assertEquals("one tap, one draw", 1, server.draws.get())
            assertEquals(listOf(DrawKind.FREE), server.kinds)
            // 내 신발 보기 → 신발 탭(받은 신발이 골라져 있고, 착용은 그대로)
            tap("draw-result-shoes")
            awaitTag("my-shoes")
            compose.onNodeWithTag("shoe-owned-row").performScrollToNode(hasTestTag("shoe-choice-${shoe.id}"))
            compose.waitUntil(5_000) { runCatching { compose.onNodeWithTag("shoe-choice-${shoe.id}").assertIsSelected() }.isSuccess }
            assertEquals("seeing the new shoe does not equip it", equipped, DrawSamples.equippedId())
            shot("d06b-my-shoes-with-the-new-shoe")
        } finally {
            ServiceLocator.useDrawForTest(null)
            DrawSamples.removeShoe(shoe)
        }
    }

    @Test fun inTheAppStates() {
        prepare()
        val shoe = DrawSamples.addShoe()
        val server = FakeDrawSource(shoe)
        server.linked = true
        // 12 — 연결했다가 해제된 회원(서버 값): 받은 상급 기회는 그대로, 지갑 다시 연결하기(선물은 다시 주지 않는다)
        server.statusOverride = DrawSamples.LINKED.copy(walletLinked = false)
        ServiceLocator.useDrawForTest(server)
        try {
            edgeToEdge()
            compose.setContent { StepUpTheme(ThemeMode.DARK) { ExperienceProvider { MainScaffold(initialTab = Screen.Customize) } } }
            openDrawTab()
            awaitText("draw-free-left", "13회")
            awaitText("draw-premium-left", "10회")
            compose.onNodeWithTag("draw-premium-sub", useUnmergedTree = true).assert(hasText(label(R.string.dv3_state_wallet_needed)))
            compose.onNodeWithText(label(R.string.dv2_kept_premium), substring = true).assertExists()
            compose.onNodeWithTag("draw-premium-action").assertTextContains(label(R.string.dv2_action_reconnect))
            compose.onAllNodesWithTag("draw-premium-gift").assertCountEquals(0)
            shot("d12-wallet-reconnect")

            // 23 — 상급 뽑기를 잠시 멈췄다(서버 값): 남은 수는 그대로, 버튼은 누를 수 없는 안내
            server.statusOverride = DrawSamples.LINKED.copy(chainPaused = true)
            leaveAndReturn()
            awaitTag("draw-premium-paused")
            awaitText("draw-premium-left", "10회")
            compose.onNodeWithTag("draw-premium-action").assertIsNotEnabled().assertTextContains(label(R.string.dv2_action_paused))
            shot("d23-service-paused")

            // DRAW15 · 16 — 무료 기회를 모두 썼다: 버튼은 뽑기 대신 안내 → "상급 뽑기 보기"는 같은 화면의 상급 패널로
            server.statusOverride = DrawSamples.EMPTY
            leaveAndReturn()
            awaitText("draw-free-left", "0회")
            compose.onNodeWithTag("draw-free-action").assertTextContains(label(R.string.dv2_action_free_info))
            shot("d13a-free-used-up")
            tap("draw-free-action")
            awaitTag("draw-sheet-free-empty")
            shot("d13-free-empty")
            tap("draw-sheet-see-premium")
            awaitGone("draw-sheet-free-empty")
            compose.waitForIdle()
            compose.onNodeWithTag("draw-premium-action").assertIsDisplayed()
            // 14 — 연결됨 · 상급 기회 없음: 러닝하고 기회 받기 → 러닝으로 기회 받기(다음 1회까지 0.4km)
            compose.onNodeWithTag("draw-premium-action").assertTextContains(label(R.string.dv2_action_run))
            tap("draw-premium-action")
            awaitTag("draw-sheet-run")
            compose.onNodeWithText(label(R.string.dv2_km, "0.4"), substring = true).assertExists()
            shot("d14-premium-empty")
            tap("draw-sheet-close")
            awaitGone("draw-sheet-run")
            assertEquals("info sheets never draw", 0, server.draws.get())

            // 26 — 러닝이 반영돼 상급 기회가 늘었다(서버 값이 늘었을 때만 잠깐, 하단 탭 위). 테스트 시계는 멈춰 있어 찍은 뒤 그만큼 보낸다
            server.statusOverride = DrawSamples.EMPTY.copy(runLeft = 1, runToday = 1, runProgressMeters = 0.0)
            leaveAndReturn()
            awaitTag("draw-toast")
            compose.onNodeWithText(label(R.string.dv2_run_reward, 1)).assertExists()
            // 알림은 실행 버튼을 가리지 않는다 — 버튼이 알림 위로 올라간다
            compose.waitForIdle()
            assertTrue("the notice sits below the main content", bounds("draw-toast").top >= bounds("draw-home").bottom - 1)
            shot("d26-running-notice", settle = 300)
            compose.mainClock.advanceTimeBy(4_000)
            awaitGone("draw-toast")

            // 20 — 결과를 모르는 요청이 남았다: 새 뽑기는 막고 두 탭 모두 "결과 확인". 확인은 새로 뽑지 않는다
            server.statusOverride = null
            server.nextReply = DrawReply.Unknown
            val hold = CompletableDeferred<Unit>()
            server.gate = hold
            leaveAndReturn()
            awaitText("draw-free-left", "13회")
            tap("draw-free-action")
            // 04 가 그려진 뒤에 뒤로 — 그리기 전(같은 프레임)에 누르면 뽑기 화면의 뒤로가 아직 켜지지 않는다
            awaitTag("draw-requesting")
            compose.waitUntil(5_000) { server.draws.get() == 1 }
            back()
            awaitTag("draw-pending")
            compose.onNodeWithTag("draw-free-value").assert(hasText(label(R.string.dv2_pending_label), substring = true))
            compose.onNodeWithTag("draw-free-action").assertTextContains(label(R.string.dv2_action_check))
            // DRAW26 — 다른 종류의 새 뽑기도 막는다: 상급 패널도 "결과 확인"
            compose.onNodeWithTag("draw-premium-action").assertTextContains(label(R.string.dv2_action_check))
            compose.onAllNodesWithTag("draw-free-history").assertCountEquals(0)
            shot("d20-resume-result")
            hold.complete(Unit)
            compose.waitUntil(15_000) { server.checks.get() >= 1 }
            tap("draw-premium-action")
            awaitTag("draw-result", 15_000)
            assertEquals("the lost answer is checked, not drawn again", 1, server.draws.get())
            shot("d20c-checked-result", settle = 1_200)
            tap("draw-result-close")
            awaitTag("draw-home")
        } finally {
            ServiceLocator.useDrawForTest(null)
            DrawSamples.removeShoe(shoe)
        }
    }

    @Test fun signedOutThenLoadFailedInTheApp() {
        prepare()
        val server = FakeDrawSource(DrawSamples.shoe())
        server.signedIn = false
        ServiceLocator.useDrawForTest(server)
        try {
            edgeToEdge()
            compose.setContent { StepUpTheme(ThemeMode.DARK) { ExperienceProvider { MainScaffold(initialTab = Screen.Customize) } } }
            openDrawTab()
            // 22 — 로그인 전: "로그인하면 볼 수 있어요." · 수는 "—" · 버튼 하나(로그인 화면으로 — 이 기기의 로그인을 바꾸니 여기서는 누르지 않는다)
            awaitTag("draw-free-unknown")
            compose.onNodeWithTag("draw-free-detail", useUnmergedTree = true).assert(hasText(label(R.string.dv3_signed_out_title)))
            compose.onAllNodesWithText("0회").assertCountEquals(0)
            compose.onAllNodesWithTag("draw-free-history").assertCountEquals(0)
            compose.onAllNodesWithTag("draw-rules").assertCountEquals(0)
            shot("d22-signed-out")

            // 21 — 로그인 뒤 첫 읽기 실패: 소진(0)과 다르다. 안내창, 닫으면 흐린 상자 · "—" · 다시 불러오기
            server.signedIn = true
            server.statusFails = true
            leaveAndReturn()
            awaitTag("draw-sheet-load-failed")
            compose.onAllNodesWithText("0회").assertCountEquals(0)
            shot("d21-load-failed")
            back()
            awaitGone("draw-sheet-load-failed")
            awaitTag("draw-free-unknown")
            compose.onNodeWithTag("draw-free-action").assertIsEnabled().assertTextContains(label(R.string.dv2_action_reload))
            shot("d21b-load-failed-closed")
            server.statusFails = false
            tap("draw-free-action")
            awaitText("draw-free-left", "13회")
            compose.onAllNodesWithTag("draw-sheet-load-failed").assertCountEquals(0)
        } finally {
            ServiceLocator.useDrawForTest(null)
        }
    }

    private data class Viewport(val width: Int, val height: Int, val font: Float = 1f, val mode: ThemeMode = ThemeMode.DARK) {
        val label get() = "${width}x$height" + (if (font != 1f) "-font${(font * 100).roundToInt()}" else "") +
            (if (mode == ThemeMode.LIGHT) "-light" else "")
    }

    private var viewport by mutableStateOf(Viewport(360, 800))

    @Test fun fitsAtDeviceSizes() {
        prepare()
        val shoe = DrawSamples.addShoe()
        val server = FakeDrawSource(shoe)
        server.linked = true
        ServiceLocator.useDrawForTest(server)
        try {
            edgeToEdge()
            compose.setContent { DeviceFrame { MainScaffold(initialTab = Screen.Customize) } }
            for (next in listOf(
                Viewport(360, 800), Viewport(390, 844), Viewport(412, 915),
                Viewport(360, 800, font = 1.3f), Viewport(390, 844, mode = ThemeMode.LIGHT),
            )) {
                show(next)
                openDrawTab()
                awaitText("draw-free-left", "${server.free}회")
                val dpPx = frameDpPx(next)
                assertMainFits("draw-free-action", "${next.label} free", dpPx)
                shot("f-${next.label}-free")
                awaitText("draw-premium-left", "${server.premium}회")
                assertMainFits("draw-premium-action", "${next.label} premium", dpPx, scroll = true)
                shot("f-${next.label}-premium")
            }
            // 결과 — 작은 폭 · 큰 글씨에서도 신발 · 이름 · 남은 수 · 두 버튼이 한 화면에(가운데를 넘기지 않는다)
            for (next in listOf(Viewport(360, 800), Viewport(360, 800, font = 1.3f))) {
                show(next)
                openDrawTab()
                awaitText("draw-free-left", "${server.free}회")
                tap("draw-free-action")
                awaitTag("draw-result")
                awaitText("draw-result-left", "${server.free}회", unmerged = false)
                assertResultFits(next.label, frameDpPx(next))
                shot("f-${next.label}-result", settle = 1_200)
                tap("draw-result-close")
                awaitTag("draw-home")
            }
        } finally {
            ServiceLocator.useDrawForTest(null)
            DrawSamples.removeShoe(shoe)
        }
    }

    @Test fun scenes() {
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
                            DrawBackdrop(Modifier.fillMaxSize())
                            Box(
                                Modifier.then(if (narrow) Modifier.requiredWidth(320.dp).fillMaxHeight() else Modifier.fillMaxSize())
                                    .align(Alignment.TopCenter).systemBarsPadding().testTag(FRAME),
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
        val epic = DrawSamples.shoe(id = 9_002, rarity = Rarity.EPIC, variant = 0)
        fun draw(
            state: DrawScreenState,
            flow: DrawFlow = DrawFlow.Home,
            notice: DrawNotice? = null,
            sheet: DrawSheet? = null,
            tab: DrawKind? = null,
            openingAt: Float? = null,
            actions: DrawActions = DrawActions(),
        ): @Composable () -> Unit = {
            MysteryBoxScreen(
                state = state, flow = flow, notice = notice, actions = actions,
                initialSheet = sheet, initialTab = tab, openingAt = openingAt,
            )
        }
        fun opening(kind: DrawKind = DrawKind.FREE) = DrawFlow.Opening(DrawnShoe(kind, if (kind == DrawKind.FREE) shoe else epic))

        // 05 — 상자 열기(결과를 받은 뒤에만): 뚜껑이 열리는 중 · 신발이 나오는 중 · 내려앉음. 상급은 오프화이트 뚜껑
        show("d05-unboxing", "draw-opening", draw(fresh, flow = opening(), openingAt = 880f))
        compose.onNodeWithTag("draw-skip").assertHasClickAction()
        compose.onAllNodesWithTag("draw-free-action").assertCountEquals(0)
        show("d05b-unboxing-lid", "draw-opening", draw(fresh, flow = opening(), openingAt = 380f))
        show("d05c-unboxing-landing", "draw-opening", draw(fresh, flow = opening(), openingAt = 1_450f))
        show("d05d-unboxing-premium", "draw-opening", draw(linked, flow = opening(DrawKind.PREMIUM), openingAt = 880f))
        // 07 — 상급 뽑기 결과(에픽) · 남은 상급 9회 · 한 번 더 뽑기
        val afterPremium = DrawScreenState.Ready(DrawSamples.LINKED.copy(giftLeft = 9))
        show("d07-premium-result", "draw-result", draw(afterPremium, flow = DrawFlow.Result(DrawnShoe(DrawKind.PREMIUM, epic, left = 9))))
        compose.onNodeWithTag("draw-result-left").assert(hasText("9회", substring = true))
        compose.onNodeWithTag("draw-result-again").assertExists()
        // 08 — 마지막 기회였다: 한 번 더 뽑기 대신 뽑기 화면으로
        val lastFree = DrawScreenState.Ready(DrawSamples.FRESH.copy(dailyLeft = 0, signupLeft = 0))
        show("d08-last-result", "draw-result", draw(lastFree, flow = DrawFlow.Result(DrawnShoe(DrawKind.FREE, shoe, left = 0))))
        compose.onNodeWithTag("draw-result-home").assertExists()
        compose.onAllNodesWithTag("draw-result-again").assertCountEquals(0)
        // 결과 뒤의 수를 아직 못 읽었다 — "확인 중"(0 으로 보이지 않는다), 한 번 더 뽑기 없음
        show("d08b-result-count-unknown", "draw-result", draw(fresh, flow = DrawFlow.Result(DrawnShoe(DrawKind.FREE, shoe, left = null))))
        compose.onNodeWithTag("draw-result-left").assert(hasText(label(R.string.dv2_pending_value), substring = true))
        compose.onAllNodesWithTag("draw-result-again").assertCountEquals(0)
        // 11 — 지갑 연결을 서버 값으로 확인했을 때만: +10회 · 상급으로 1회 뽑기(뒤는 상급 탭)
        show("d11-wallet-success", "draw-sheet-linked", draw(linked, notice = DrawNotice.Linked(10)))
        compose.onNodeWithTag("draw-sheet-linked-gift", useUnmergedTree = true).assert(hasText("+10회"))
        // 18 — 시작 실패(기회를 쓰지 않았다): 연결 문제면 다시 뽑기, 무료가 없으면 확인만
        show("d18-not-started", "draw-sheet-not-started", draw(fresh, notice = DrawNotice.NotStarted(DrawKind.FREE, DrawStop.Network)))
        compose.onNodeWithText(label(R.string.dv2_stop_unused)).assertExists()
        compose.onNodeWithTag("draw-sheet-retry").assertExists()
        show("d18b-not-started-no-free", "draw-sheet-not-started", draw(empty, notice = DrawNotice.NotStarted(DrawKind.FREE, DrawStop.NoFree)))
        compose.onAllNodesWithTag("draw-sheet-retry").assertCountEquals(0)
        // 19 — 결과 확인 지연(새로 뽑지 않는다): 한 번 확인했는데 아직 모름 · 다시 확인하는 중
        show("d19-delayed-result", "draw-checking", draw(fresh, flow = DrawFlow.Checking(DrawKind.FREE, busy = false, tried = true)))
        compose.onNodeWithTag("draw-check-again").assertIsEnabled()
        compose.onAllNodesWithTag("draw-free-action").assertCountEquals(0)
        show("d19b-delayed-result-busy", "draw-checking", draw(fresh, flow = DrawFlow.Checking(DrawKind.FREE, busy = true)))
        compose.onNodeWithTag("draw-check-again").assertIsNotEnabled()
        // 22 — 로그인 전: 로그인 화면으로 가는 버튼. 서버 설정이 없는 빌드면 누를 수 없는 안내
        show("d22b-signed-out-sign-in", "draw-free-action", draw(DrawScreenState.SignedOut, actions = DrawActions(onSignIn = {})))
        compose.onNodeWithTag("draw-free-action").assertIsEnabled().assertTextContains(label(R.string.dv3_action_sign_in))
        compose.onAllNodesWithText("0회").assertCountEquals(0)
        show("d22c-signed-out-no-server", "draw-free-action", draw(DrawScreenState.SignedOut))
        compose.onNodeWithTag("draw-free-action").assertIsNotEnabled()
        // 24 — 오늘 러닝 지급 한도: 상급 내역에 한도 알림(남은 기회는 그대로) · 기회가 없을 때의 러닝 안내에도
        val capped = DrawScreenState.Ready(DrawSamples.LINKED.copy(runLeft = 10, runToday = 10, runProgressMeters = 300.0))
        show("d24-running-cap", "draw-sheet-run-cap", draw(capped, sheet = DrawSheet.PremiumChances, tab = DrawKind.PREMIUM))
        compose.onNodeWithTag("draw-sheet-premium-total").assert(hasText("20회", substring = true))
        val cappedEmpty = DrawScreenState.Ready(DrawSamples.EMPTY.copy(runToday = 10))
        show("d24b-running-cap-empty", "draw-sheet-run-cap", draw(cappedEmpty, sheet = DrawSheet.RunChances, tab = DrawKind.PREMIUM))
        // 25 — 발행 한도: "지금은 뽑을 수 없어요" · 확인 하나
        show("d25-mint-cap", "draw-sheet-not-started", draw(linked, notice = DrawNotice.NotStarted(DrawKind.PREMIUM, DrawStop.MintLimit)))
        compose.onNodeWithText(label(R.string.dv3_stop_title_blocked)).assertExists()
        compose.onAllNodesWithTag("draw-sheet-retry").assertCountEquals(0)
        // 26 — 러닝 반영 알림(서버 값이 늘었을 때만, 상급 탭 위)
        val runOne = DrawScreenState.Ready(DrawSamples.EMPTY.copy(runLeft = 1, runProgressMeters = 0.0))
        show("d26b-running-notice", "draw-toast", draw(runOne, notice = DrawNotice.RunReward(1), tab = DrawKind.PREMIUM))
        // 가격 · SUP 결제 문구가 없다 — 모든 뽑기는 무료
        show("d01b-free-main", "draw-free-left", draw(fresh))
        compose.onAllNodesWithText("SUP", substring = true).assertCountEquals(0)

        // 밝은 테마 · 큰 글씨(1.3) · 320dp 폭
        val afterFree = DrawScreenState.Ready(DrawSamples.FRESH.copy(dailyLeft = 2))
        val freeResult = DrawFlow.Result(DrawnShoe(DrawKind.FREE, shoe, left = 12))
        compose.runOnIdle { light = true }
        show("v01-light-free-main", "draw-free-left", draw(fresh))
        show("v02-light-premium-main", "draw-premium-left", draw(linked, tab = DrawKind.PREMIUM))
        show("v03-light-wallet-required", "draw-premium-gift", draw(fresh, tab = DrawKind.PREMIUM))
        show("v04-light-free-sheet", "draw-sheet-free", draw(fresh, sheet = DrawSheet.FreeChances))
        show("v05-light-result", "draw-result", draw(afterFree, flow = freeResult))
        show("v06-light-opening", "draw-opening", draw(fresh, flow = opening(), openingAt = 880f))
        show("v07-light-checking", "draw-checking", draw(fresh, flow = DrawFlow.Checking(DrawKind.FREE, busy = false, tried = true)))
        compose.runOnIdle { light = false; large = true }
        show("v08-large-free-main", "draw-free-left", draw(fresh))
        actionInside("draw-free-action")
        show("v09-large-result", "draw-result", draw(afterFree, flow = freeResult))
        actionInside("draw-result-shoes")
        show("v10-large-premium-sheet", "draw-sheet-premium", draw(linked, sheet = DrawSheet.PremiumChances, tab = DrawKind.PREMIUM))
        compose.runOnIdle { large = false; narrow = true }
        show("v11-narrow-premium-main", "draw-premium-left", draw(linked, tab = DrawKind.PREMIUM))
        actionInside("draw-premium-action")
        show("v12-narrow-wallet-required", "draw-premium-gift", draw(fresh, tab = DrawKind.PREMIUM))
        actionInside("draw-premium-action")
        show("v13-narrow-result", "draw-result", draw(afterFree, flow = freeResult))
        actionInside("draw-result-shoes")
    }

    // ── 도우미 ─────────────────────────────────────────────────────

    private var scene by mutableStateOf<@Composable () -> Unit>({})

    private fun show(name: String, readyTag: String, content: @Composable () -> Unit) {
        compose.runOnIdle { scene = { key(name) { content() } } }
        awaitTag(readyTag)
        // 결과 무대는 등급 틀 · 신발 그림이 커서 처음 그릴 때 화면에 늦게 올라온다(QA 36524498482 의 d07 은 앞 장면이 찍혔다)
        shot(name, settle = if (readyTag == "draw-result") 1_800 else 700)
    }

    private fun show(next: Viewport) {
        compose.runOnIdle { viewport = next }
        compose.waitForIdle()
    }

    /** 앱 셸을 [viewport] 크기(dp) · 글자 배율 · 테마로 — 에뮬레이터 창 안에 그 크기의 화면을 만든다 */
    @Composable private fun DeviceFrame(content: @Composable () -> Unit) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.TopCenter) {
            val v = viewport
            val px = minOf(constraints.maxHeight / v.height.toFloat(), constraints.maxWidth / v.width.toFloat())
            CompositionLocalProvider(LocalDensity provides Density(px, v.font)) {
                StepUpTheme(v.mode) {
                    ExperienceProvider {
                        Box(Modifier.requiredSize(v.width.dp, v.height.dp).testTag(FRAME)) {
                            key(v) { content() }
                        }
                    }
                }
            }
        }
    }

    private fun prepare() = runBlocking {
        ServiceLocator.userPrefs.setReducedMotion(true)
        ServiceLocator.userPrefs.setSounds(false)
        ServiceLocator.userPrefs.setHaptics(false)
        ServiceLocator.userPrefs.setGuideSeen()
        ServiceLocator.sneakerRepository.ensureStarter()
    }

    /** 하단 가운데 뽑기 탭 */
    private fun openDrawTab() {
        awaitTag("nav-label-${Screen.Draw.route}")
        compose.onNodeWithTag("nav-label-${Screen.Draw.route}", useUnmergedTree = true).performClick()
        compose.waitForIdle()
        awaitTag("draw-home")
    }

    /** 신발 탭에 갔다가 뽑기 탭으로 — 돌아올 때마다 서버 현황을 다시 읽는다 */
    private fun leaveAndReturn() {
        compose.onNodeWithTag("nav-label-${Screen.Customize.route}", useUnmergedTree = true).performClick()
        awaitTag("shoes-section-mine")
        openDrawTab()
    }

    /**
     * 메인의 실행 버튼이 화면 안에 온전히(하단 탭이 있으면 그 위) — 무료 패널 버튼은 넘기지 않아도 보이고, 상급 패널 버튼은
     * [scroll] 이면 넘겨서 닿는다(작은 화면 · 큰 글씨는 넘김을 허용한다 — 두 번째 버튼이 하단 탭 뒤로 사라지지 않을 것).
     */
    private fun assertMainFits(action: String, where: String, dpPx: Float = compose.density.density, scroll: Boolean = false) {
        compose.waitForIdle()
        if (scroll) {
            compose.onNodeWithTag(action).performScrollTo()
            compose.waitForIdle()
        } else {
            val range = compose.onNodeWithTag("draw-scroll").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
            assertEquals("the draw main starts at the top at $where", 0f, range.value(), 1f)
        }
        val button = bounds(action)
        val limit = if (exists(BOTTOM_NAV_TAG)) bounds(BOTTOM_NAV_TAG).top else frameBottom()
        assertTrue("$action above the bottom tabs at $where: $button / $limit", button.bottom <= limit + 1)
        assertTrue("$action keeps its height at $where: $button", button.height >= 49f * dpPx)
    }

    /** 결과가 한 화면 — 가운데를 넘기지 않고, 신발 이름이 고정된 남은 수 칸 위에 보인다 */
    private fun assertResultFits(where: String, dpPx: Float = compose.density.density) {
        compose.waitForIdle()
        val scroll = compose.onNodeWithTag("draw-result-scroll").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
        assertEquals("the draw result must not scroll at $where", 0f, scroll.maxValue(), 1f)
        val name = bounds("draw-result-name")
        val left = bounds("draw-result-left")
        assertTrue("the shoe name sits above the count at $where: $name / $left", name.bottom <= left.top + 1)
        val stage = bounds("draw-result-stage")
        assertTrue("the shoe stays large enough at $where: $stage", stage.height >= 140f * dpPx)
    }

    /** 버튼이 화면 안에 온전히 있다(큰 글씨 · 좁은 폭에서 잘리지 않는다) — 넘치면 스크롤해서 */
    private fun actionInside(tag: String) {
        runCatching { compose.onNodeWithTag(tag).performScrollTo() }
        compose.waitForIdle()
        val frame = bounds(FRAME)
        val action = bounds(tag)
        val minHeight = with(compose.density) { 49.dp.toPx() }
        assertTrue(
            "$tag inside the screen: $action in $frame",
            action.left >= frame.left - 1 && action.right <= frame.right + 1 && action.bottom <= frame.bottom + 1 && action.height >= minHeight,
        )
    }

    /** 기기 크기 틀은 화면 밀도를 바꾼다 — 그 틀의 1dp 가 몇 px 인가 */
    private fun frameDpPx(v: Viewport): Float = bounds(FRAME).height / v.height

    private fun frameBottom(): Float = if (exists(FRAME)) bounds(FRAME).bottom else compose.onRoot().fetchSemanticsNode().boundsInRoot.bottom

    private fun bounds(tag: String): Rect = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    private fun exists(tag: String): Boolean = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

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

    private fun awaitTag(tag: String, timeout: Long = 10_000) {
        compose.waitUntil(timeout) { exists(tag) }
    }

    private fun awaitGone(tag: String, timeout: Long = 10_000) {
        compose.waitUntil(timeout) { !exists(tag) }
    }

    /** [tag] 칸의 글자에 [value] 가 들 때까지 — 수가 들어 있는 글자 칸은 묶인 줄 안에 있어 기본은 풀린 나무에서 찾는다 */
    private fun awaitText(tag: String, value: String, unmerged: Boolean = true, timeout: Long = 10_000) = eventually(timeout) {
        compose.onNodeWithTag(tag, useUnmergedTree = unmerged).assert(hasText(value, substring = true))
    }

    private fun eventually(timeout: Long = 10_000, check: () -> Unit) {
        compose.waitUntil(timeout) { runCatching(check).isSuccess }
        check()
    }

    private fun shot(name: String, settle: Long = 700) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(settle)
        captureDisplay(File(directory, "$name.png"))
    }

    private companion object {
        const val FRAME = "draw-frame"
    }
}
