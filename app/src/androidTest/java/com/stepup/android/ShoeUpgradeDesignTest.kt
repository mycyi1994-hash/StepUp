package com.stepup.android

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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.Faction
import com.stepup.android.domain.ForgeMaterial
import com.stepup.android.domain.ForgeMaterialBlock
import com.stepup.android.domain.ForgePending
import com.stepup.android.domain.ForgeQuote
import com.stepup.android.domain.ForgeResult
import com.stepup.android.domain.ForgeStats
import com.stepup.android.domain.ForgeTarget
import com.stepup.android.domain.ForgeTargetBlock
import com.stepup.android.domain.MaterialSelection
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.ServerStats
import com.stepup.android.domain.ShoeForge
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.components.CommerceBackdrop
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.items.MaterialsLoad
import com.stepup.android.ui.screens.items.ShoeUpgradeContent
import com.stepup.android.ui.screens.items.ShoeUpgradeState
import com.stepup.android.ui.screens.items.UpgradeActions
import com.stepup.android.ui.screens.items.UpgradePhase
import com.stepup.android.ui.screens.items.UpgradeSheet
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

/**
 * 신발 강화 지시서 v6(2026-10-01, docs/redesign/shoe-upgrade-2026-10-01) — 장면 번호는 시안의 01 ~ 15 · 17 · 18 그대로(16 없음).
 * 상태를 화면에 바로 넣어 찍는다. 신발 번호 · 레벨 · 능력치 · 확률은 시안과 같은 예시이고 이 테스트 화면에만 있다 — 계정 데이터가 아니다.
 * 확률은 지시서 식(ShoeForge)으로 계산한 값이다 — 레전더리 Lv10 + 에픽 Lv10 · 레어 Lv10 · 에픽 Lv1 = 80.4%.
 */
class ShoeUpgradeDesignTest {
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

    private val directory get() = File(compose.activity.getExternalFilesDir(null), "shoe-upgrade").apply { mkdirs() }

    private var scene by mutableStateOf<@Composable () -> Unit>({})
    private var large by mutableStateOf(false)
    private var narrow by mutableStateOf(false)

    @Test fun upgradeScenes() {
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
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (large) 1.3f else 1f)) {
                StepUpTheme(ThemeMode.DARK) {
                    ExperienceProvider {
                        Box(Modifier.fillMaxSize()) {
                            CommerceBackdrop(Modifier.fillMaxSize())
                            Box(
                                Modifier.then(if (narrow) Modifier.requiredWidth(360.dp).fillMaxHeight() else Modifier.fillMaxSize())
                                    .align(Alignment.TopCenter).statusBarsPadding(),
                            ) { scene() }
                        }
                    }
                }
            }
        }
        val all = listOf(M1, M2, M3)
        val ready = MaterialsLoad.Ready(all + listOf(M4_EQUIPPED, M5_CHAIN))
        fun editing(applied: List<Long>, draft: List<Long>? = null, sheet: UpgradeSheet = UpgradeSheet.None,
                    candidates: MaterialsLoad = ready, quote: ForgeQuote? = null) = BASE.copy(
            selection = MaterialSelection(applied, draft), sheet = sheet, candidates = candidates, quote = quote,
        )

        // 첫 장면은 창이 처음 그려질 때라 한 번 그려 두고 다시 찍는다(첫 캡처가 빈 화면으로 남지 않게)
        show("s01-ready-empty", "upgrade-screen", editing(emptyList()), settle = 2_000)

        // 01 — 재료 선택 전: 슬롯 셋 비어 있음 · 0 / 3 · 현재 예상 성공률 70.0% · 버튼은 누를 수 없음
        show("s01-ready-empty", "upgrade-screen", editing(emptyList()))
        compose.onNodeWithTag("upgrade-count", useUnmergedTree = true).assertTextEquals("0 / 3")
        compose.onNodeWithTag("upgrade-rate", useUnmergedTree = true).assertTextContains("70.0%", substring = true)
        compose.onNodeWithTag("upgrade-primary").assertIsNotEnabled()
        noFormulaText()

        // 02 — 재료 선택(하단 시트): 두 개 고른 임시 선택 · 재료별 보정 · 쓸 수 없는 줄은 까닭과 함께
        show("s02-picker", "upgrade-picker", editing(emptyList(), draft = listOf(M1.id, M2.id), sheet = UpgradeSheet.Picker))
        compose.onNodeWithTag("picker-count", useUnmergedTree = true).assertTextContains("2", substring = true)
        compose.onNodeWithTag("picker-apply").assertIsEnabled()
        noFormulaText()

        // 03 — 준비 완료: 3 / 3 · 서버 견적 80.4% · 성공 시 10 → 11
        show("s03-ready-full", "upgrade-screen", editing(all.map { it.id }, quote = QUOTE))
        compose.onNodeWithTag("upgrade-count", useUnmergedTree = true).assertTextEquals("3 / 3")
        compose.onNodeWithTag("upgrade-rate", useUnmergedTree = true).assertTextContains("80.4%", substring = true)
        compose.onNodeWithTag("upgrade-primary").assertIsEnabled()
        noFormulaText()

        // 04 — 최종 확인(하단 시트)
        show("s04-confirm", "upgrade-confirm", editing(all.map { it.id }, quote = QUOTE, sheet = UpgradeSheet.Confirm(QUOTE)))
        compose.onNodeWithTag("confirm-rate", useUnmergedTree = true).assertTextContains("80.4%", substring = true)

        // 05 — 진행 중: 버튼은 누를 수 없고 다시 보내지 않는다
        show("s05-running", "upgrade-running", BASE.copy(phase = UpgradePhase.Running, running = all, quote = QUOTE))
        compose.onNodeWithTag("upgrade-primary").assertIsNotEnabled()

        // 06 — 성공: 서버가 준 실제 값(Lv 11)
        val success = ForgeResult(true, TARGET.id, all.map { it.id }, 804, 10, 11, ForgeStats(11, 20, 1850, 820))
        show("s06-success", "upgrade-success", BASE.copy(phase = UpgradePhase.Succeeded(success, STATS), running = all))

        // 07 — 실패: 레벨 그대로 · 재료 소각
        val failure = ForgeResult(false, TARGET.id, all.map { it.id }, 804, 10, 10, STATS)
        show("s07-failed", "upgrade-failed", BASE.copy(phase = UpgradePhase.Failed(failure), running = all))

        // 08 — 두 개 넣음: 2 / 3 · 78.4%(미리보기) · 하나 더 고르기
        show("s08-two-selected", "upgrade-screen", editing(listOf(M1.id, M2.id)))
        compose.onNodeWithTag("upgrade-count", useUnmergedTree = true).assertTextEquals("2 / 3")
        compose.onNodeWithTag("upgrade-rate", useUnmergedTree = true).assertTextContains("78.4%", substring = true)
        compose.onNodeWithTag("upgrade-primary").assertIsNotEnabled()

        // 09 · 10 · 11 — 재료 시트: 없음 · 불러오는 중 · 실패(적용한 선택은 그대로)
        show("s09-picker-empty", "upgrade-picker", editing(emptyList(), draft = emptyList(), sheet = UpgradeSheet.Picker,
            candidates = MaterialsLoad.Ready(emptyList())))
        show("s10-picker-loading", "upgrade-picker", editing(emptyList(), draft = emptyList(), sheet = UpgradeSheet.Picker,
            candidates = MaterialsLoad.Loading))
        show("s11-picker-failed", "upgrade-picker", editing(listOf(M1.id, M2.id), draft = listOf(M1.id, M2.id),
            sheet = UpgradeSheet.Picker, candidates = MaterialsLoad.Failed))

        // 12 — 실행 전 확인에서 재료 하나가 쓸 수 없게 됨(선택에서 이미 빠짐)
        show("s12-materials-changed", "upgrade-changed", editing(listOf(M1.id, M2.id), sheet = UpgradeSheet.Changed(listOf(M3))))

        // 13 — 최대 레벨
        show("s13-max-level", "upgrade-max", BASE.copy(
            info = TARGET.copy(stats = ForgeStats(20, 20, 2300, 1000), block = ForgeTargetBlock.MAX_LEVEL),
            shoe = TARGET_SHOE.copy(level = 20), phase = UpgradePhase.Blocked(ForgeTargetBlock.MAX_LEVEL),
        ))
        compose.onAllNodesWithTag("upgrade-rate").assertCountEquals0()

        // 14 — 보내기 전 연결 끊김(하단 시트): 재료 그대로
        show("s14-offline", "upgrade-offline", editing(all.map { it.id }, quote = QUOTE, sheet = UpgradeSheet.Offline))

        // 15 — 결과 미확인: 같은 키로만 다시 묻는다
        val pending = ForgePending("demo-key", TARGET.id, all.map { it.id }, 804, System.currentTimeMillis() - 90_000)
        show("s15-result-unknown", "upgrade-unknown", BASE.copy(phase = UpgradePhase.Unknown(pending), running = all))
        compose.onNodeWithTag("upgrade-primary").assertIsEnabled()

        // 17 — 최하위 등급(일반)
        show("s17-lowest-grade", "upgrade-lowest", BASE.copy(
            shoe = COMMON_SHOE, info = TARGET.copy(id = COMMON_SHOE.id, rarity = Rarity.COMMON, block = ForgeTargetBlock.LOWEST_GRADE),
            phase = UpgradePhase.Blocked(ForgeTargetBlock.LOWEST_GRADE),
        ))

        // 18 — 대상 사용 불가(판매 중 · 보유 상태 바뀜)
        show("s18-target-unavailable", "upgrade-unavailable", BASE.copy(
            info = TARGET.copy(block = ForgeTargetBlock.TARGET_UNAVAILABLE),
            phase = UpgradePhase.Blocked(ForgeTargetBlock.TARGET_UNAVAILABLE),
        ))

        // 360dp · 큰 글씨 — 준비 완료 · 재료 시트 · 최종 확인 · 성공
        narrow = true
        show("w360-s03-ready-full", "upgrade-screen", editing(all.map { it.id }, quote = QUOTE))
        show("w360-s02-picker", "upgrade-picker", editing(emptyList(), draft = listOf(M1.id), sheet = UpgradeSheet.Picker))
        large = true
        show("w360-large-s03-ready-full", "upgrade-screen", editing(all.map { it.id }, quote = QUOTE))
        show("w360-large-s04-confirm", "upgrade-confirm", editing(all.map { it.id }, quote = QUOTE, sheet = UpgradeSheet.Confirm(QUOTE)))
        show("w360-large-s06-success", "upgrade-success", BASE.copy(phase = UpgradePhase.Succeeded(success, STATS), running = all))
        large = false
        narrow = false
    }

    private fun show(name: String, readyTag: String, state: ShoeUpgradeState, settle: Long = 700) {
        compose.runOnIdle { scene = { key(name) { ShoeUpgradeContent(state, UpgradeActions()) } } }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(readyTag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(settle)
        captureDisplay(File(directory, "$name.png"))
    }

    /** 성공률 계산 설명(16 · "기본 70% + 재료 보정")은 만들지 않는다 */
    private fun noFormulaText() {
        compose.onAllNodesWithText("재료 보정", substring = true).assertCountEquals0()
        compose.onAllNodesWithText("기본 70", substring = true).assertCountEquals0()
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteractionCollection.assertCountEquals0() {
        val nodes = fetchSemanticsNodes()
        check(nodes.isEmpty()) { "expected none, found ${nodes.size}" }
    }

    private companion object {
        fun server(efficiencyBps: Int, comfortBps: Int, maxLevel: Int = 20) = ServerStats(
            origin = "DRAW", efficiencyBps = efficiencyBps, comfortBps = comfortBps, durabilityPts = 100.0, maxLevel = maxLevel,
            status = "OWNED", chainState = "APP", canWithdraw = true, upgradeCost = 0.0, repairCostPerPoint = 0.0, genesisNo = 0,
        )

        fun shoe(id: Long, rarity: Rarity, model: Int?, level: Int, number: Int, equipped: Boolean = false) = Sneaker(
            id = id, faction = Faction.WIND, rarity = rarity, variant = 0, level = level, mintNumber = number,
            luck = 1.0, comfort = 1.0, durability = 100, equipped = equipped, acquiredAt = 0L,
            server = server(800, 500), modelId = model,
        )

        fun material(shoe: Sneaker, block: ForgeMaterialBlock? = null) =
            ForgeMaterial(shoe, ShoeForge.bonusPermille(Rarity.LEGENDARY, shoe.rarity, shoe.level), block)

        val TARGET_SHOE = shoe(9101, Rarity.LEGENDARY, 1301, level = 10, number = 7).copy(server = server(1800, 800))
        val STATS = ForgeStats(level = 10, maxLevel = 20, efficiencyBps = 1800, comfortBps = 800)
        val TARGET = ForgeTarget(TARGET_SHOE.id, Rarity.LEGENDARY, STATS, ShoeForge.basePermille(10), block = null)
        val COMMON_SHOE = shoe(9109, Rarity.COMMON, null, level = 3, number = 52)

        val M1 = material(shoe(9121, Rarity.EPIC, 1201, level = 10, number = 21))
        val M2 = material(shoe(9134, Rarity.RARE, 1107, level = 10, number = 34))
        val M3 = material(shoe(9148, Rarity.EPIC, 1202, level = 1, number = 48))
        val M4_EQUIPPED = material(shoe(9150, Rarity.EPIC, 1203, level = 4, number = 50, equipped = true), ForgeMaterialBlock.EQUIPPED)
        val M5_CHAIN = material(shoe(9151, Rarity.RARE, 1101, level = 2, number = 51), ForgeMaterialBlock.ON_CHAIN)

        val QUOTE = ForgeQuote(
            ratePermille = ShoeForge.ratePermille(TARGET.basePermille, listOf(M1, M2, M3).map { it.bonusPermille }),
            before = STATS, after = TARGET.previewNext(), quoteVersion = "test", materialIds = listOf(M1.id, M2.id, M3.id),
        )

        val BASE = ShoeUpgradeState(
            targetId = TARGET.id, shoe = TARGET_SHOE, info = TARGET,
            known = listOf(M1, M2, M3, M4_EQUIPPED, M5_CHAIN).associateBy { it.id },
            phase = UpgradePhase.Editing,
        )
    }
}
