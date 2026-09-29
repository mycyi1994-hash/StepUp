package com.stepup.android

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.stepup.android.domain.Faction
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.ServerStats
import com.stepup.android.domain.ShoeCatalog
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Screen
import com.stepup.android.ui.components.CommerceBackdrop
import com.stepup.android.ui.components.S2ShoesSections
import com.stepup.android.ui.components.ShoeSection
import com.stepup.android.ui.components.fullSneakerLabel
import com.stepup.android.ui.components.shoeModelNameRes
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.customize.MyShoesContent
import com.stepup.android.ui.screens.items.EquipFailure
import com.stepup.android.ui.screens.items.EquipResult
import com.stepup.android.ui.screens.items.OwnedLoad
import com.stepup.android.ui.screens.items.ShoeDetailContent
import com.stepup.android.ui.screens.items.ShoeDetailState
import com.stepup.android.ui.screens.items.withObjectParticle
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 보유 신발 상세 v1(2026-09-28 전달본, docs/redesign/shoe-detail-v1) — 장면 번호는 시안의 01~18 그대로.
 * 신발 탭 첫 화면은 신발 화면 확정안(2026-09-28)의 내 신발이다 — 켤레마다 한 칸, 관리(⋯)로 상세에 들어간다(상세 보기 글 링크 없음).
 *
 * [wearFlowInTheApp] 은 앱 셸 안에서 실제로 걷는다: 신발 탭 → 칸 고르기(착용 그대로) → 같은 모델 두 켤레(각자 한 칸) → 관리(⋯) →
 * 능력치 자세히 · 능력치 설명(시스템 뒤로 가기는 능력치로) → 신발 정보(체인 줄) → ⋯ 이 신발 관리(옮긴 강화 · 수리 · 판매) →
 * 이 신발 신기 → 저장된 착용이 바뀐 뒤 완료 알림 → 뒤로(목록의 체크가 같은 id 로). 새 도감(70종) 신발 세 켤레를 넣고,
 * 끝나면 지우고 원래 신던 신발을 다시 신긴다.
 * [detailScenes] 는 시안 장면의 상태를 화면에 바로 넣어 찍는다(조회 중 · 실패 · 없는 신발 · 그림 실패 · 착용 없음 · 빈 목록 · 실패 시트,
 * 밝은 테마 · 큰 글씨 · 320dp). 그 장면의 번호(#0001 ~ #0004) · 능력치는 이 테스트 화면에만 있는 예시다 — 앱 데이터가 아니다.
 */
class ShoeDetailDesignTest {
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

    private val directory get() = File(compose.activity.getExternalFilesDir(null), "shoe-detail-v1").apply { mkdirs() }
    private var originalEquipped: Long? = null

    /** 앱 셸 안 — 신발 탭에서 고르고, 상세에서 시트를 열어 보고, 신고, 돌아온다 */
    @Test fun wearFlowInTheApp() {
        val seeded = seed()
        val dao = ServiceLocator.database.sneakerDao()
        val original = requireNotNull(originalEquipped)
        try {
            edgeToEdge()
            compose.setContent { StepUpTheme(ThemeMode.DARK) { ExperienceProvider { MainScaffold(initialTab = Screen.Customize) } } }
            awaitTag("my-shoes")
            awaitTag("shoe-hero")
            // 01 — 처음 보는 신발은 신고 있는 켤레("착용 중")
            assertMetaWearing(true)
            shot("01a-in-app-wearing")

            // 01 — 칸을 누르면 보는 신발만 바뀐다. 착용은 그대로
            pick(seeded.single)
            compose.waitUntil(10_000) { heroNameShows(modelName(1107)) }
            assertMetaWearing(false)
            assertEquals("Picking a tile must not change the stored equipment", original, runBlocking { dao.equippedNow()?.id })
            shot("01b-in-app-picked")

            // 07 — 같은 모델 두 켤레는 각자 한 칸(소유 id) — 번호로 구분. 고르는 것만으로 착용은 그대로
            pick(seeded.pairA)
            val numberA = metaText()
            pick(seeded.pairB)
            val numberB = metaText()
            assertTrue("the two pairs of 1201 show their own numbers: $numberA / $numberB", numberA != numberB)
            compose.onNodeWithTag("shoe-choice-${seeded.pairA}").assertIsNotSelected()
            shot("07-in-app-pairs")
            assertEquals(original, runBlocking { dao.equippedNow()?.id })

            // 02 — 관리(⋯): 고른 켤레(소유 id)의 상세
            pick(seeded.single)
            tap("shoe-manage")
            awaitWearEnabled()
            awaitTag("shoe-art")
            // 이름 끝에 공통 등급 배지(2026-09-29) — 이름 글 뒤에 배지 자리(" 레어")가 붙는다
            compose.onNode(hasText(modelName(1107), substring = true) and hasAnyAncestor(hasTestTag("shoe-name")), useUnmergedTree = true)
                .assertExists()
            compose.onNode(hasTestTag("tier-badge-rare") and hasAnyAncestor(hasTestTag("shoe-name")), useUnmergedTree = true)
                .assertExists()
            assertUnmergedText("shoe-status", string(R.string.sdv_status_owned))
            compose.onNodeWithTag("detail-primary-action").assertTextEquals(string(R.string.sdv_wear))
            shot("02-in-app-detail")

            // 04 → 05 → 시스템 뒤로(04) → 확인
            tap("shoe-row-stats")
            awaitTag("shoe-stats-sheet")
            shot("04-in-app-stats")
            tap("shoe-stats-help")
            awaitTag("shoe-explain-sheet")
            shot("05-in-app-explain")
            back()
            awaitTag("shoe-stats-sheet")
            tap("shoe-stats-ok")
            awaitGone("shoe-stats-sheet")

            // 06 — 신발 정보: 신발 번호 · 받은 날짜 · 체인 줄(이 기기에서 넣은 신발이라 아직 체인에 없음)
            tap("shoe-row-info")
            awaitTag("shoe-info-sheet")
            compose.onNodeWithTag("shoe-info-chain").assertTextContains(string(R.string.sdv_chain_none))
            shot("06-in-app-info")
            tap("shoe-info-ok")
            awaitGone("shoe-info-sheet")

            // ⋯ — 이전 상세의 강화 · 수리 · 판매(지우지 않고 옮김). 보기만 하고 닫는다
            tap("shoe-more")
            awaitTag("shoe-manage-sheet")
            awaitTag("shoe-manage-enhance")
            compose.onNodeWithTag("shoe-manage-sell").assertIsEnabled()
            shot("19-in-app-manage")
            back()
            awaitGone("shoe-manage-sheet")
            assertEquals(original, runBlocking { dao.equippedNow()?.id })

            // 09 → 10 — 이 신발 신기: 기존 착용 길(equipExclusively)로 저장되고, 저장된 착용이 바뀐 뒤에만 완료 알림
            tap("detail-primary-action")
            compose.waitUntil(10_000) { runBlocking { dao.equippedNow()?.id } == seeded.single }
            awaitTag("shoe-worn-toast")
            compose.onNodeWithTag("shoe-worn-toast").assertTextContains(modelName(1107), substring = true)
            shot("10-in-app-equipped", settle = 250)
            compose.waitUntil(5_000) {
                runCatching { compose.onNodeWithTag("detail-primary-action").assertIsNotEnabled() }.isSuccess
            }
            compose.onNodeWithTag("detail-primary-action").assertTextEquals(string(R.string.sdv_status_wearing))
            assertUnmergedText("shoe-status", string(R.string.sdv_status_wearing))
            assertEquals(1, runBlocking { dao.allNow().count { it.equipped } })

            // 12 — 뒤로: 목록의 체크가 같은 id 로 옮겨 가고, 보던 켤레는 그대로("착용 중")
            back()
            awaitTag("my-shoes")
            compose.waitUntil(10_000) { runCatching { assertMetaWearing(true) }.isSuccess }
            assertTrue("the stage still shows ${modelName(1107)}", heroNameShows(modelName(1107)))
            showTile(seeded.single)
            compose.onNode(hasTestTag("shoe-worn-badge") and hasAnyAncestor(hasTestTag("shoe-choice-${seeded.single}")),
                useUnmergedTree = true).assertExists()
            compose.onNodeWithTag("shoe-choice-${seeded.single}").assertIsSelected()
            shot("12-in-app-after-equip")
        } finally {
            restore(seeded)
        }
    }

    /** 시안 장면 — 상태를 화면에 바로 넣는다 */
    @Test fun detailScenes() {
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
                                    .align(Alignment.TopCenter).statusBarsPadding(),
                            ) { scene() }
                        }
                    }
                }
            }
        }
        val all = listOf(WORN, PAIR_A, SPIKE, PAIR_B)
        fun ready(shoe: Sneaker, list: List<Sneaker> = all) = ShoeDetailState.Ready(shoe, list.firstOrNull { it.equipped })
        fun detail(
            state: ShoeDetailState,
            equipping: Boolean = false,
            result: EquipResult? = null,
            sheet: String? = null,
        ): @Composable () -> Unit = {
            ShoeDetailContent(state = state, equipping = equipping, result = result, zone = SEOUL, initialSheet = sheet)
        }
        fun tab(load: OwnedLoad, selected: Long?): @Composable () -> Unit = {
            Column(Modifier.fillMaxSize()) {
                S2ShoesSections(ShoeSection.MINE, onSelect = {})
                MyShoesContent(
                    load = load, selectedId = selected, onSelect = {}, onOpenSneaker = {}, onOpenDraw = {}, onReload = {},
                    modifier = Modifier.weight(1f),
                )
            }
        }
        val worn = legacyName()

        // 01 — 보는 신발(칸 테두리) · 신고 있는 신발(작은 체크)을 가른다. 켤레 수는 4 — 같은 모델 두 켤레도 각자 한 칸
        show("s01-owned-entry", "shoe-hero", tab(OwnedLoad.Ready(all), PAIR_A.id))
        assertMetaWearing(false)
        compose.onNodeWithTag("shoe-owned-count", useUnmergedTree = true).assertTextEquals("4")
        compose.onNode(hasTestTag("shoe-worn-badge") and hasAnyAncestor(hasTestTag("shoe-choice-${WORN.id}")), useUnmergedTree = true)
            .assertExists()
        compose.onNodeWithTag("shoe-choice-${PAIR_A.id}").assertIsSelected()
        compose.onNodeWithTag("shoe-choice-${PAIR_B.id}").assertIsNotSelected()
        compose.onAllNodesWithTag("shoe-detail").assertCountEquals(0)
        // 02 — 다른 신발 구경: 보유 중 · 이 신발 신기 · +0.45% · 7.5%
        show("s02-shoe-detail", "shoe-art", detail(ready(PAIR_A)))
        assertUnmergedText("shoe-status", string(R.string.sdv_status_owned))
        compose.onNodeWithTag("detail-primary-action").assertIsEnabled().assertTextEquals(string(R.string.sdv_wear))
        compose.onNodeWithTag("shoe-key-bonus").assertTextContains("+0.45%")
        compose.onNodeWithTag("shoe-key-energy").assertTextContains("7.5%")
        // 03 — 신고 있는 신발: 버튼은 강화가 아니라 "지금 신고 있어요"(누를 수 없음)
        show("s03-equipped-detail", "shoe-art", detail(ready(WORN)))
        assertUnmergedText("shoe-status", string(R.string.sdv_status_wearing))
        compose.onNodeWithTag("detail-primary-action").assertIsNotEnabled().assertTextEquals(string(R.string.sdv_status_wearing))
        // 04 — 서버 신발(새 도감)은 있는 값 세 줄 · 예전 신발은 다섯 줄
        show("s04-all-stats", "shoe-stats-sheet", detail(ready(PAIR_A), sheet = "Stats"))
        compose.onNodeWithTag("shoe-stat-durability").assertTextContains("100 / 100")
        compose.onAllNodesWithTag("shoe-stat-luck").assertCountEquals(0)
        show("s04b-all-stats-legacy", "shoe-stats-sheet", detail(ready(WORN), sheet = "Stats"))
        compose.onNodeWithTag("shoe-stat-luck").assertTextContains("1.05")
        compose.onNodeWithTag("shoe-stat-comfort").assertTextContains("1.12")
        show("s05-stat-explanation", "shoe-explain-sheet", detail(ready(WORN), sheet = "Explain"))
        // 06 — 신발 정보 + 체인 줄(시안에 없는 줄): 없음 · v3 금고
        show("s06-shoe-information", "shoe-info-sheet", detail(ready(PAIR_A), sheet = "Info"))
        compose.onNodeWithTag("shoe-info-number").assertTextContains("#0002")
        compose.onNodeWithTag("shoe-info-received").assertTextContains("2026.09.27")
        compose.onNodeWithTag("shoe-info-chain").assertTextContains(string(R.string.sdv_chain_none))
        compose.onAllNodesWithTag("shoe-info-faction").assertCountEquals(0)
        show("s06b-shoe-information-vault", "shoe-info-sheet", detail(ready(SPIKE), sheet = "Info"))
        compose.onNodeWithTag("shoe-info-chain").assertTextContains(string(R.string.sdv_chain_vault, 1_000_003L))
        show("s06c-shoe-information-legacy", "shoe-info-sheet", detail(ready(WORN), sheet = "Info"))
        compose.onNodeWithTag("shoe-info-faction").assertExists()
        // 07 — 같은 모델 여러 켤레: 칸마다 한 켤레, 보는 켤레의 번호가 이름 아래에
        show("s07-owned-pairs", "shoe-hero", tab(OwnedLoad.Ready(all), PAIR_B.id))
        compose.onNodeWithTag("shoe-hero-meta", useUnmergedTree = true).assertTextContains("#0004", substring = true)
        compose.onNodeWithTag("shoe-choice-${PAIR_B.id}").assertIsSelected()
        compose.onNodeWithTag("shoe-choice-${PAIR_A.id}").assertIsNotSelected()
        // 08 — 같은 모델의 다른 켤레: 자기 번호 · 능력치 · 날짜
        show("s08-another-copy", "shoe-art", detail(ready(PAIR_B)))
        compose.onNodeWithTag("shoe-key-energy").assertTextContains("6.0%")
        // 09 — 바꾸는 중: 다시 누를 수 없다
        show("s09-equipping", "shoe-art", detail(ready(PAIR_A), equipping = true))
        compose.onNodeWithTag("detail-primary-action").assertIsNotEnabled().assertTextEquals(string(R.string.sdv_wear_busy))
        // 10 — 저장된 착용이 바뀐 뒤: 지금 신고 있어요 + 완료 알림(한 번)
        val after = all.map { it.copy(equipped = it.id == PAIR_A.id) }
        show("s10-equipped-success", "shoe-worn-toast", detail(ready(after.first { it.id == PAIR_A.id }, after), result = EquipResult.Worn(PAIR_A.id)), settle = 250)
        compose.onNodeWithTag("shoe-worn-toast").assertTextContains(string(R.string.sdv_worn_toast, withObjectParticle(modelName(1201))))
        // 11 — 실패: 기존 착용을 말하고 다시 신기 · 닫기. 결과를 모르면 단정하지 않는다
        show("s11-equip-error", "shoe-equip-error",
            detail(ready(PAIR_A), result = EquipResult.NotWorn(PAIR_A.id, EquipFailure.REJECTED, WORN.id, confirmed = true)))
        compose.onNodeWithTag("shoe-error-kept").assertTextEquals(string(R.string.sdv_error_kept, withObjectParticle(worn)))
        compose.onNodeWithTag("shoe-error-retry").assertHasClickAction()
        show("s11b-equip-unconfirmed", "shoe-equip-error",
            detail(ready(PAIR_A), result = EquipResult.NotWorn(PAIR_A.id, EquipFailure.OFFLINE, WORN.id, confirmed = false)))
        compose.onNodeWithTag("shoe-error-kept").assertTextEquals(string(R.string.sdv_error_unknown_body))
        show("s11c-equip-error-none", "shoe-equip-error",
            detail(ready(PAIR_A, all.map { it.copy(equipped = false) }), result = EquipResult.NotWorn(PAIR_A.id, EquipFailure.REJECTED, null, confirmed = true)))
        compose.onNodeWithTag("shoe-error-kept").assertTextEquals(string(R.string.sdv_error_none))
        // 12 — 돌아온 목록: 체크가 같은 id 로
        show("s12-inventory-after-equip", "shoe-hero", tab(OwnedLoad.Ready(after), PAIR_A.id))
        assertMetaWearing(true)
        compose.onNode(hasTestTag("shoe-worn-badge") and hasAnyAncestor(hasTestTag("shoe-choice-${PAIR_A.id}")), useUnmergedTree = true)
            .assertExists()
        // 13 · 14 · 15 — 조회 중(임시 값 없음) · 조회 실패 · 없는 신발
        show("s13-detail-loading", "shoe-detail-loading", detail(ShoeDetailState.Loading))
        compose.onNodeWithTag("detail-primary-action").assertIsNotEnabled().assertTextEquals(string(R.string.sdv_checking))
        compose.onAllNodesWithTag("shoe-key-bonus").assertCountEquals(0)
        show("s14-detail-load-error", "shoe-detail-failed", detail(ShoeDetailState.Failed))
        compose.onNodeWithTag("shoe-state-primary").assertTextEquals(string(R.string.sdv_reload))
        compose.onNodeWithTag("shoe-state-secondary").assertTextEquals(string(R.string.sdv_back_to_owned))
        show("s15-shoe-not-found", "shoe-detail-missing", detail(ShoeDetailState.NotFound))
        compose.onNodeWithTag("shoe-state-primary").assertTextEquals(string(R.string.sdv_back_to_owned))
        compose.onAllNodesWithTag("detail-primary-action").assertCountEquals(0)
        // 16 — 그림만 실패(이 앱에 그림이 없는 새 모델 번호): 다른 신발 그림을 넣지 않고, 능력치 · 신기는 그대로
        val unknown = PAIR_A.copy(id = 99, modelId = 1999)
        show("s16-art-load-error", "shoe-art-failed", detail(ready(unknown, all + unknown)))
        compose.onAllNodesWithTag("shoe-art").assertCountEquals(0)
        compose.onNodeWithTag("shoe-art-retry").assertHasClickAction()
        compose.onNodeWithTag("detail-primary-action").assertIsEnabled()
        // 17 — 아직 착용 없음: 같은 버튼으로 처음 신는다
        val nobody = all.map { it.copy(equipped = false) }
        show("s17-first-equipment", "shoe-art", detail(ready(nobody.first { it.id == PAIR_A.id }, nobody)))
        assertUnmergedText("shoe-status", string(R.string.sdv_status_none))
        compose.onNodeWithTag("detail-primary-action").assertIsEnabled()
        // 18 — 읽기가 끝났고 정말 비었을 때: 무료 뽑기로
        show("s18-empty-inventory", "shoe-tab-empty", tab(OwnedLoad.Ready(emptyList()), null))
        compose.onNodeWithTag("shoe-state-primary").assertTextEquals(string(R.string.sdv_empty_action))
        // 신발 탭을 읽는 중 · 읽지 못함(빈 목록으로 보이지 않는다)
        show("s18b-tab-loading", "shoe-tab-loading", tab(OwnedLoad.Loading, null))
        compose.onAllNodesWithTag("shoe-tab-empty").assertCountEquals(0)
        show("s18c-tab-failed", "shoe-tab-failed", tab(OwnedLoad.Failed, null))
        // ⋯ — 옮긴 기존 행동. 신고 있으면 판매는 까닭과 함께 누를 수 없다
        show("s19-manage", "shoe-manage-sheet", detail(ready(WORN), sheet = "Manage"))
        compose.onNodeWithTag("shoe-manage-sell").assertIsNotEnabled()
        compose.onNodeWithTag("shoe-manage-enhance").assertIsEnabled()

        // 밝은 테마 · 큰 글씨 · 320dp
        compose.runOnIdle { light = true }
        show("s30-light-entry", "shoe-hero", tab(OwnedLoad.Ready(all), PAIR_A.id))
        show("s30b-light-detail", "shoe-art", detail(ready(PAIR_A)))
        show("s30c-light-stats", "shoe-stats-sheet", detail(ready(WORN), sheet = "Stats"))
        compose.runOnIdle { light = false; large = true }
        show("s31-large-font-detail", "shoe-art", detail(ready(SPIKE)))
        show("s31b-large-font-entry", "shoe-hero", tab(OwnedLoad.Ready(all), SPIKE.id))
        show("s31c-large-font-info", "shoe-info-sheet", detail(ready(SPIKE), sheet = "Info"))
        compose.runOnIdle { large = false; narrow = true }
        show("s32-narrow-detail", "shoe-art", detail(ready(SPIKE)))
        show("s32b-narrow-entry", "shoe-hero", tab(OwnedLoad.Ready(all), SPIKE.id))
        show("s32c-narrow-error", "shoe-equip-error",
            detail(ready(SPIKE), result = EquipResult.NotWorn(SPIKE.id, EquipFailure.SIGN_IN, WORN.id, confirmed = true)))
    }

    // ── 도우미 ─────────────────────────────────────────────────────

    private data class Seeded(val single: Long, val pairA: Long, val pairB: Long) {
        val all get() = listOf(single, pairA, pairB)
    }

    private var scene by mutableStateOf<@Composable () -> Unit>({})

    /** 장면마다 새로 그린다(앞 장면의 기억 — 열린 시트 · 고른 줄 — 을 이어받지 않게) */
    private fun show(name: String, readyTag: String, content: @Composable () -> Unit, settle: Long = 700) {
        compose.runOnIdle { scene = { key(name) { content() } } }
        awaitTag(readyTag)
        shot(name, settle)
    }

    private fun string(id: Int, vararg args: Any): String = compose.activity.getString(id, *args)

    private fun modelName(model: Int): String = compose.activity.getString(requireNotNull(shoeModelNameRes(model)))

    /** 예시 착용 신발(예전 52종 — 바람 일반 0)의 현지화 이름 */
    private fun legacyName(): String = fullSneakerLabel(compose.activity, Faction.WIND, Rarity.COMMON, 0)

    private fun assertUnmergedText(tag: String, text: String) {
        compose.onNodeWithTag(tag, useUnmergedTree = true).assertTextEquals(text)
    }

    /** 가로 목록 안의 칸을 보이게 — 내 신발은 세로로 끌지 않는다 */
    private fun showTile(id: Long) {
        compose.onNodeWithTag("shoe-owned-row").performScrollToNode(hasTestTag("shoe-choice-$id"))
    }

    /** 보유 칸을 눌러 미리 보기로 고른다(무대 · 이름 · 능력치가 그 켤레로) */
    private fun pick(id: Long) {
        showTile(id)
        compose.onNodeWithTag("shoe-choice-$id").performClick().assertIsSelected()
        compose.waitForIdle()
    }

    /**
     * 무대 위 이름에 [name] 이 있다 — 이름 칸(제목 한 덩어리)은 글 · 배지를 감싸므로 그 안의 글을 본다
     * (글 끝에는 배지 자리의 대체 글 " 레어"가 붙는다)
     */
    private fun heroNameShows(name: String): Boolean = compose.onAllNodes(
        hasText(name, substring = true) and hasAnyAncestor(hasTestTag("shoe-hero-name")), useUnmergedTree = true,
    ).fetchSemanticsNodes().isNotEmpty()

    /** 이름 아래 줄 — "Lv. 1 · #0002 · 착용 중" */
    private fun metaText(): String =
        compose.onNodeWithTag("shoe-hero-meta", useUnmergedTree = true).fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString { it.text }

    private fun assertMetaWearing(wearing: Boolean) {
        val text = metaText()
        assertEquals("meta line \"$text\" wearing=$wearing", wearing, text.contains(string(R.string.my_shoes_wearing)))
    }

    /** 상세의 주 버튼이 읽기(확인 중…)를 마치고 눌릴 수 있게 될 때까지 */
    private fun awaitWearEnabled() {
        compose.waitUntil(10_000) {
            runCatching { compose.onNodeWithTag("detail-primary-action").assertIsEnabled() }.isSuccess
        }
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

    private fun prepare() = runBlocking {
        ServiceLocator.userPrefs.setReducedMotion(true)
        ServiceLocator.userPrefs.setSounds(false)
        ServiceLocator.userPrefs.setHaptics(false)
        ServiceLocator.userPrefs.setGuideSeen()
    }

    /**
     * 앱 신발 창고에 새 도감 세 켤레 — 한 켤레(레어 1107)와 같은 모델 두 켤레(에픽 1201). 신고 있는 켤레(없으면 첫 켤레를 신긴다)를
     * 본떠 폰에만 있는 신발로 넣는다(서버를 거치지 않는 기존 착용 길 — equipExclusively).
     */
    private fun seed(): Seeded = runBlocking {
        prepare()
        ServiceLocator.sneakerRepository.ensureStarter()
        val dao = ServiceLocator.database.sneakerDao()
        val base = requireNotNull(dao.equippedNow() ?: dao.allNow().firstOrNull())
        if (!base.equipped) ServiceLocator.sneakerRepository.equip(base.id)
        originalEquipped = base.id
        val now = System.currentTimeMillis()
        suspend fun add(model: Int, level: Int) = dao.insert(
            base.copy(
                id = 0, rarity = requireNotNull(ShoeCatalog.of(model)).rarity.id, variant = 0, level = level,
                mintNumber = dao.maxMintNumber() + 1, equipped = false, acquiredAt = now, serverId = 0, origin = "",
                modelId = model, tokenId = 0,
            ),
        )
        Seeded(single = add(1107, 1), pairA = add(1201, 1), pairB = add(1201, 2))
    }

    private fun restore(seeded: Seeded) = runBlocking {
        val dao = ServiceLocator.database.sneakerDao()
        originalEquipped?.let { ServiceLocator.sneakerRepository.equip(it) }
        seeded.all.filter { it != originalEquipped }.forEach { id -> dao.byId(id)?.let { dao.delete(it) } }
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

    private fun awaitGone(tag: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }
    }

    private fun shot(name: String, settle: Long = 700) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(settle)
        captureDisplay(File(directory, "$name.png"))
    }

    private companion object {
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")

        fun at(y: Int, m: Int, d: Int): Long = LocalDateTime.of(y, m, d, 9, 30).atZone(SEOUL).toInstant().toEpochMilli()

        fun server(efficiencyBps: Int, comfortBps: Int) = ServerStats(
            origin = "DRAW", efficiencyBps = efficiencyBps, comfortBps = comfortBps, durabilityPts = 100.0, maxLevel = 15,
            status = "OWNED", chainState = "APP", canWithdraw = true, upgradeCost = 120.0, repairCostPerPoint = 0.0, genesisNo = 0,
        )

        /** 시안 예시(세 모델 · 네 켤레)의 모양을 실제 신발 그림으로 — 번호 · 능력치 · 날짜는 이 테스트 화면에만 있는 예시 */
        val WORN = Sneaker(
            id = 9001, faction = Faction.WIND, rarity = Rarity.COMMON, variant = 0, level = 1, mintNumber = 1,
            luck = 1.05, comfort = 1.12, durability = 100, equipped = true, acquiredAt = at(2026, 9, 26),
        )
        val PAIR_A = Sneaker(
            id = 9002, faction = Faction.WIND, rarity = Rarity.EPIC, variant = 0, level = 1, mintNumber = 2,
            luck = 1.0, comfort = 1.075, durability = 100, equipped = false, acquiredAt = at(2026, 9, 27),
            server = server(45, 750), modelId = 1201,
        )
        val SPIKE = Sneaker(
            id = 9003, faction = Faction.WIND, rarity = Rarity.LEGENDARY, variant = 0, level = 1, mintNumber = 3,
            luck = 1.0, comfort = 1.06, durability = 100, equipped = false, acquiredAt = at(2026, 9, 27),
            server = server(55, 600), modelId = 1311, tokenId = 1_000_003,
        )
        val PAIR_B = Sneaker(
            id = 9004, faction = Faction.WIND, rarity = Rarity.EPIC, variant = 0, level = 1, mintNumber = 4,
            luck = 1.0, comfort = 1.06, durability = 100, equipped = false, acquiredAt = at(2026, 9, 28),
            server = server(45, 600), modelId = 1201,
        )
    }
}
