package com.stepup.android

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.domain.Faction
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.ServerStats
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.components.CommerceBackdrop
import com.stepup.android.ui.components.LocalShoeNicknames
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.items.CareBlock
import com.stepup.android.ui.screens.items.EquipFailure
import com.stepup.android.ui.screens.items.EquipResult
import com.stepup.android.ui.screens.items.RepairPhase
import com.stepup.android.ui.screens.items.ShoeCareStatus
import com.stepup.android.ui.screens.items.ShoeDetailContent
import com.stepup.android.ui.screens.items.ShoeDetailState
import com.stepup.android.ui.screens.items.repairQuoteOf
import com.stepup.android.ui.screens.items.sceneTag
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 파란 톤 v4 신발 상세 · 수리(docs/redesign/blue-v4-2026-10 — SD01 ~ SD19 · RP01 ~ RP11).
 * 상태를 화면에 바로 넣어 찍는다(서버 · 저장소를 건드리지 않는다). 번호 · 능력치 · 잔액은 이 테스트 화면의 예시다 — 앱 데이터가 아니다.
 * 강화 화면(UP01 ~ UP15 · UP18 — 재료 신발 3개 · 확률, 0054 forge)은 ShoeUpgradeDesignTest 가 찍는다.
 * [repairButton] 은 이름 줄 번호 옆 붉은 수리하기(2026-11-07 사용자 선택 1안)를 기기 폭 · 360 · 390 · 430dp · 큰 글씨 · 긴 이름 · 밝은 테마로 찍고,
 * 누르면 기존 수리 시트가 열리기만 하는지(확정 0번) 본다.
 */
class ShoeCareDesignTest {
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

    private val directory get() = File(compose.activity.getExternalFilesDir(null), "shoe-care-blue").apply { mkdirs() }
    private var scene by mutableStateOf<@Composable () -> Unit>({})

    @Test fun careScenes() {
        var light by mutableStateOf(false)
        var large by mutableStateOf(false)
        var narrow by mutableStateOf(false)
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
        val ready = ShoeDetailState.Ready(RARE, WORN)
        fun detail(
            state: ShoeDetailState = ready,
            sheet: String? = null,
            repair: RepairPhase? = null,
            care: ShoeCareStatus = ShoeCareStatus(),
            equipping: Boolean = false,
            result: EquipResult? = null,
        ): @Composable () -> Unit = {
            ShoeDetailContent(state = state, equipping = equipping, result = result, zone = SEOUL, initialSheet = sheet,
                care = care, repair = repair, onSignIn = {})
        }

        // SD01 · SD03 · SD12 · SD18 · SD19
        show("SD01-detail", "shoe-art", detail())
        compose.onNodeWithTag("shoe-upgrade-open").assertIsEnabled()
        compose.onNodeWithTag("detail-primary-action").assertIsEnabled()
        // 번호 옆 수리하기(2026-11-07) — 머리 ⋯ · 본문 수리 줄은 없다
        compose.onNodeWithTag("shoe-repair-open").assertIsEnabled()
        compose.onAllNodesWithTag("shoe-more", useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithTag("shoe-care-repair", useUnmergedTree = true).assertCountEquals(0)
        show("SD03-equipping", "shoe-art", detail(equipping = true))
        compose.onNodeWithTag("shoe-upgrade-open").assertIsNotEnabled()
        show("SD12-first", "shoe-first-hint", detail(state = ShoeDetailState.Ready(RARE, null)))
        // 결과를 모르는 강화 요청 — 강화하기 자리가 "강화 결과 확인"(같은 요청의 결과만 묻는다), 신기는 막는다
        show("SD18-upgrade-pending", "shoe-upgrade-pending", detail(care = ShoeCareStatus(upgradePending = true)))
        compose.onNodeWithTag("detail-primary-action").assertIsNotEnabled()
        compose.onNodeWithTag("shoe-upgrade-open").assertIsEnabled()
        compose.onNodeWithTag("shoe-upgrade-open").assertTextContains(string(R.string.upg_action_check))
        compose.onNodeWithTag("shoe-repair-open").assertIsNotEnabled()
        // 결과를 모르는 수리 — 번호 옆 수리하기는 막고, 위 "결과 확인" 안내가 같은 시트를 연다
        show("SD19-repair-pending", "shoe-repair-pending", detail(care = ShoeCareStatus(repairPending = true)))
        compose.onNodeWithTag("shoe-upgrade-open").assertIsNotEnabled()
        compose.onNodeWithTag("shoe-repair-open").assertIsNotEnabled()
        // SD05 · SD06 · SD07
        show("SD05-rejected", "shoe-equip-error",
            detail(result = EquipResult.NotWorn(RARE.id, EquipFailure.REJECTED, WORN.id, confirmed = true)))
        show("SD06-sign-in", "shoe-equip-error",
            detail(result = EquipResult.NotWorn(RARE.id, EquipFailure.SIGN_IN, WORN.id, confirmed = true)))
        show("SD07-unconfirmed", "shoe-equip-error",
            detail(result = EquipResult.NotWorn(RARE.id, EquipFailure.OFFLINE, WORN.id, confirmed = false)))
        compose.onNodeWithTag("shoe-error-retry").assertTextEquals(string(R.string.care_wear_check))
        // SD08 · SD13 · SD14 · SD15 · SD16 — 신발 관리는 ⋯ 시트가 아니라 본문 아래 줄(판매 · 기록)
        show("SD08-loading", "shoe-stat-cells-loading", detail(state = ShoeDetailState.Loading))
        compose.onAllNodesWithTag("shoe-repair-open", useUnmergedTree = true).assertCountEquals(0)
        show("SD13-care-actions", "shoe-care-actions", detail())
        compose.onNodeWithTag("shoe-care-actions", useUnmergedTree = true).performScrollTo()
        shot("SD13b-care-actions-scrolled")
        compose.onNodeWithTag("shoe-care-sell").assertIsEnabled()
        compose.onNodeWithTag("shoe-care-record").assertHasClickAction()
        show("SD14-care-actions-worn", "shoe-care-actions", detail(state = ShoeDetailState.Ready(WORN, WORN)))
        compose.onNodeWithTag("shoe-care-actions", useUnmergedTree = true).performScrollTo()
        shot("SD14b-care-actions-worn-scrolled")
        compose.onNodeWithTag("shoe-care-sell").assertIsNotEnabled()
        show("SD15-record-none", "shoe-info-sheet", detail(sheet = "Record"))
        show("SD16-record-token", "shoe-info-sheet", detail(state = ShoeDetailState.Ready(RARE.copy(tokenId = 142), WORN), sheet = "Record"))
        compose.onNodeWithTag("shoe-info-chain").assertTextContains(string(R.string.care_chain_token, 142L))

        // RP01 ~ RP11
        val quote = requireNotNull(repairQuoteOf(WORN_DOWN))
        val worn = ShoeDetailState.Ready(WORN_DOWN, WORN)
        val phases = listOf(
            "RP01" to RepairPhase.Confirm(quote, 83.07),
            "RP02" to RepairPhase.Loading,
            "RP03" to RepairPhase.Sending(quote, 83.07),
            "RP04" to RepairPhase.Done(quote, 33.07, 100.0),
            "RP05" to RepairPhase.NotNeeded(100.0),
            "RP06" to RepairPhase.Insufficient(quote, 20.0),
            "RP07" to RepairPhase.LoadFailed,
            "RP08" to RepairPhase.Unknown(quote, 83.07, accepted = false),
            "RP09" to RepairPhase.Blocked(CareBlock.LISTED),
            "RP10" to RepairPhase.Confirm(quote, 83.07, changed = true),
            "RP11" to RepairPhase.SignIn,
        )
        phases.forEach { (id, phase) -> show("$id-${phase.sceneTag()}", phase.sceneTag(), detail(state = worn, repair = phase)) }
        show("RP06b-short", "repair-insufficient", detail(state = worn, repair = RepairPhase.Insufficient(quote, 20.0)))
        compose.onNodeWithTag("repair-short", useUnmergedTree = true).assertTextEquals(string(R.string.care_short, "30"))
        compose.onNodeWithTag("repair-confirm").assertIsNotEnabled()

        // 밝은 테마 · 큰 글씨 · 320dp
        compose.runOnIdle { light = true }
        show("L-SD01", "shoe-art", detail())
        show("L-RP01", "repair-confirm", detail(state = worn, repair = RepairPhase.Confirm(quote, 83.07)))
        compose.runOnIdle { light = false; large = true; narrow = true }
        show("N-SD01", "shoe-art", detail())
        show("N-RP03", "repair-sending", detail(state = worn, repair = RepairPhase.Sending(quote, 83.07)))
    }

    /**
     * 번호 옆 수리하기(2026-11-07 사용자 선택 1안) — 앱 화면([SneakerDetailScreen])처럼 누르면 수리 시트 상태를 여는 틀에 넣는다.
     * 시트 장면 · 잔액(83.07 SUP)은 이 테스트 화면의 예시다. 수리 확정(onRepairConfirm)은 한 번도 불리지 않아야 한다.
     */
    @Test fun repairButton() {
        var viewport by mutableStateOf(Viewport())
        var nicknames by mutableStateOf<Map<Long, String>>(emptyMap())
        var care by mutableStateOf(ShoeCareStatus())
        var repair by mutableStateOf<RepairPhase?>(null)
        var opened by mutableIntStateOf(0)
        var confirmed by mutableIntStateOf(0)
        val quote = requireNotNull(repairQuoteOf(WORN_DOWN))
        val state = ShoeDetailState.Ready(WORN_DOWN, WORN)
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.TopCenter) {
                val v = viewport
                val base = LocalDensity.current
                // 폭을 그 dp 로 채우는 밀도 — 기기 폭이면 기기 그대로. 무대 높이 계산(screenHeightDp)도 그 화면에 맞춘다
                val px = v.width?.let { constraints.maxWidth / it.toFloat() } ?: base.density
                val fontScale = v.font ?: base.fontScale
                val config = Configuration(LocalConfiguration.current).apply {
                    screenWidthDp = (constraints.maxWidth / px).toInt()
                    screenHeightDp = (constraints.maxHeight / px).toInt()
                }
                CompositionLocalProvider(LocalDensity provides Density(px, fontScale), LocalConfiguration provides config) {
                    StepUpTheme(v.mode) {
                        ExperienceProvider {
                            CompositionLocalProvider(LocalShoeNicknames provides nicknames) {
                                Box(Modifier.fillMaxSize()) {
                                    CommerceBackdrop(Modifier.fillMaxSize())
                                    Box(Modifier.fillMaxSize().statusBarsPadding().testTag(VIEWPORT)) {
                                        key(v) {
                                            ShoeDetailContent(
                                                state = state, zone = SEOUL, care = care, repair = repair, onSignIn = {},
                                                onOpenRepair = {
                                                    opened += 1
                                                    repair = RepairPhase.Confirm(quote, 83.07)
                                                },
                                                onRepairConfirm = { confirmed += 1 },
                                                onRepairClose = { repair = null },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        fun at(next: Viewport, name: String) {
            compose.runOnIdle { viewport = next }
            awaitTag("shoe-repair-open")
            shot(name)
        }

        // R01 — 기기 폭 첫 화면: 이름 · 등급 · No. 번호 바로 오른쪽 붉은 수리하기, 머리 ⋯ 없음, 아래는 강화하기 · 이 신발 신기 둘
        at(Viewport(), "R01-detail-number-repair")
        compose.onNode(hasTestTag("shoe-repair-open") and hasAnyAncestor(hasTestTag("shoe-name")), useUnmergedTree = true)
            .assertExists()
        compose.onNodeWithTag("shoe-repair-open").assertIsEnabled().assertHasClickAction()
            .assertTextEquals(string(R.string.care_repair))
        compose.onAllNodesWithTag("shoe-repair-open").assertCountEquals(1)
        compose.onAllNodesWithTag("shoe-more", useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithTag("shoe-care-repair", useUnmergedTree = true).assertCountEquals(0)
        compose.onNodeWithTag("shoe-upgrade-open").assertIsEnabled()
        compose.onNodeWithTag("detail-primary-action").assertIsEnabled()
        assertTitleCentered()
        assertRightOfNumber()
        assertFits()

        // R02 — 누르면 이 켤레의 기존 수리 시트(견적 · 잔액 · 확정 버튼). 여는 것만으로 확정하지 않는다
        compose.onNodeWithTag("shoe-repair-open").performClick()
        awaitTag("repair-sheet")
        awaitTag("repair-confirm")
        shot("R02-repair-sheet-from-button")
        assertEquals("the button opens the sheet once", 1, opened)
        assertEquals("opening the sheet must not confirm a repair", 0, confirmed)
        // R03 — 닫으면 상세 그대로
        compose.onNodeWithTag("repair-close").performClick()
        awaitGone("repair-sheet")
        awaitTag("shoe-repair-open")
        shot("R03-after-close")
        assertEquals(0, confirmed)

        // R04 — 결과를 모르는 수리: 번호 옆 버튼은 흐리게 막히고(눌러도 다시 열지 않는다) 위 안내의 "결과 확인"이 같은 시트를 연다
        compose.runOnIdle { care = ShoeCareStatus(repairPending = true) }
        awaitTag("shoe-repair-pending")
        shot("R04-repair-pending")
        compose.onNodeWithTag("shoe-repair-open").assertIsNotEnabled().performClick()
        compose.waitForIdle()
        assertEquals("a blocked button does not reopen the sheet", 1, opened)
        compose.onAllNodesWithTag("repair-sheet", useUnmergedTree = true).assertCountEquals(0)
        compose.runOnIdle { care = ShoeCareStatus() }

        // R05 — 360 · 390 · 430dp: 번호 바로 오른쪽, 잘리지 않고 능력치 줄과 겹치지 않는다
        for (width in listOf(360, 390, 430)) {
            at(Viewport(width), "R05-${width}dp")
            assertRightOfNumber()
            assertFits()
        }
        // R06 — 큰 글씨(1.3) · 긴 이름(20자 — 이름 바꾸기 최대): 번호 · 수리하기는 붙어 남고 화면 안에
        compose.runOnIdle { nicknames = mapOf(WORN_DOWN.id to LONG_NAME) }
        at(Viewport(), "R06a-long-name")
        assertGrouped()
        assertFits()
        at(Viewport(360, font = 1.3f), "R06b-360dp-font1.3-long-name")
        assertGrouped()
        assertFits()
        at(Viewport(430, font = 1.3f), "R06c-430dp-font1.3-long-name")
        assertGrouped()
        assertFits()
        compose.runOnIdle { nicknames = emptyMap() }
        at(Viewport(390, font = 1.3f), "R06d-390dp-font1.3")
        assertGrouped()
        assertFits()
        // 아주 큰 글씨(2.0) · 360dp — 한 줄에 다 안 들어가면 수리하기만 이름 바로 아래로(숨기거나 밀어내지 않는다)
        at(Viewport(360, font = 2f), "R06e-360dp-font2.0")
        assertGrouped()
        assertFits()

        // R07 — 밝은 테마: 같은 자리 · 붉은 채움 · 흰 글자, 누르면 같은 시트
        at(Viewport(390, mode = ThemeMode.LIGHT), "R07-light-390dp")
        assertRightOfNumber()
        assertFits()
        compose.onNodeWithTag("shoe-repair-open").performClick()
        awaitTag("repair-confirm")
        shot("R08-light-repair-sheet")
        assertEquals(2, opened)
        assertEquals(0, confirmed)
        compose.onNodeWithTag("repair-close").performClick()
        awaitGone("repair-sheet")
    }

    /** 머리 제목은 가운데 그대로(⋯ 자리를 비워도 한쪽으로 쏠리지 않는다) */
    private fun assertTitleCentered() {
        val title = compose.onNodeWithText(string(R.string.sdv_title), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val frame = bounds(VIEWPORT)
        assertEquals("title centred", frame.center.x, title.center.x, dp(2f))
    }

    /** 수리하기는 번호 바로 오른쪽, 같은 줄 */
    private fun assertRightOfNumber() {
        val number = bounds("shoe-name-suffix")
        val pill = bounds("shoe-repair-open")
        assertTrue("pill $pill starts right of number $number", pill.left >= number.right - dp(1f))
        assertTrue("pill $pill sits close to number $number", pill.left - number.right <= dp(16f))
        assertEquals("pill and number share a line", number.center.y, pill.center.y, dp(4f))
    }

    /** 번호 바로 오른쪽(같은 줄)이거나, 한 줄에 다 안 들어가면 이름 바로 아래 — 어느 쪽이든 번호와 붙어 있다 */
    private fun assertGrouped() {
        val number = bounds("shoe-name-suffix")
        val pill = bounds("shoe-repair-open")
        val sameLine = kotlin.math.abs(number.center.y - pill.center.y) <= dp(4f) && pill.left >= number.right - dp(1f)
        val below = pill.top >= number.bottom - dp(2f) && pill.top - number.bottom <= dp(32f)
        assertTrue("pill $pill stays with number $number", sameLine || below)
    }

    /** 수리하기가 화면 폭 안 · 이름 칸 안에 다 보이고, 아래 능력치 줄과 겹치지 않는다 */
    private fun assertFits() {
        val frame = bounds(VIEWPORT)
        val pill = bounds("shoe-repair-open")
        val name = bounds("shoe-name")
        val stats = bounds("shoe-stat-cells")
        assertTrue("pill $pill inside screen $frame", pill.left >= frame.left - 1f && pill.right <= frame.right + 1f)
        assertTrue("pill $pill inside name block $name", pill.right <= name.right + dp(1f) && pill.bottom <= name.bottom + dp(1f))
        assertTrue("pill $pill above stats $stats", pill.bottom <= stats.top + 1f)
        assertTrue("pill keeps a visible size: $pill", pill.width >= dp(60f) && pill.height >= dp(24f))
    }

    private fun bounds(tag: String): Rect =
        compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    /** 지금 화면의 dp → px(밀도를 바꾼 화면이면 그 밀도) */
    private fun dp(value: Float): Float {
        val node = compose.onNodeWithTag(VIEWPORT, useUnmergedTree = true).fetchSemanticsNode()
        return value * node.layoutInfo.density.density
    }

    private fun awaitTag(tag: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun awaitGone(tag: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }
    }

    /** 기기 폭([width] 없음) 또는 그 dp 폭 · 글자 배율 · 테마 */
    private data class Viewport(val width: Int? = null, val font: Float? = null, val mode: ThemeMode = ThemeMode.DARK)

    private fun string(id: Int, vararg args: Any): String = compose.activity.getString(id, *args)

    private fun show(name: String, readyTag: String, content: @Composable () -> Unit) {
        compose.runOnIdle { scene = { key(name) { content() } } }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(readyTag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        shot(name)
    }

    private fun shot(name: String) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(500)
        captureDisplay(File(directory, "$name.png"))
    }

    private companion object {
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
        const val VIEWPORT = "care-viewport"

        /** 이름 바꾸기 최대 길이(20자)의 예시 이름 — 이 테스트 화면에만 있다 */
        const val LONG_NAME = "새벽 강변 장거리 러너 스페셜 에디션"

        fun server(eff: Int, comfort: Int, durability: Double = 100.0, maxLevel: Int = 15, perPoint: Double = 1.5625, cost: Double = 625.0) =
            ServerStats(
                origin = "DRAW", efficiencyBps = eff, comfortBps = comfort, durabilityPts = durability, maxLevel = maxLevel,
                status = "OWNED", chainState = "APP", canWithdraw = true, upgradeCost = cost, repairCostPerPoint = perPoint, genesisNo = 0,
            )

        val RARE = Sneaker(
            id = 9107, faction = Faction.WIND, rarity = Rarity.RARE, variant = 0, level = 5, mintNumber = 7,
            luck = 1.0, comfort = 1.0, durability = 92, equipped = false, acquiredAt = 1_790_000_000_000L,
            server = server(800, 580, durability = 92.0), modelId = 1107,
        )
        val WORN_DOWN = RARE.copy(durability = 68, server = server(800, 580, durability = 68.0))
        val WORN = Sneaker(
            id = 9201, faction = Faction.WIND, rarity = Rarity.EPIC, variant = 0, level = 3, mintNumber = 21,
            luck = 1.0, comfort = 1.0, durability = 100, equipped = true, acquiredAt = 1_790_000_000_000L,
            server = server(300, 400, maxLevel = 20), modelId = 1201,
        )
    }
}
