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
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.Faction
import com.stepup.android.domain.ForgeBar
import com.stepup.android.domain.ForgeKind
import com.stepup.android.domain.ForgeMaterial
import com.stepup.android.domain.ForgeMaterialBlock
import com.stepup.android.domain.ForgePrecious
import com.stepup.android.domain.ForgeXp
import com.stepup.android.domain.XpCatalog
import com.stepup.android.domain.XpCrit
import com.stepup.android.domain.XpMaterial
import com.stepup.android.domain.XpQuote
import com.stepup.android.domain.XpQuoteMaterial
import com.stepup.android.domain.XpResult
import com.stepup.android.domain.XpResultMaterial
import com.stepup.android.domain.XpSelection
import com.stepup.android.domain.XpTarget
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
 * 경험치 강화(서버 0118, 2026-11-19 — docs/forge-xp-daily-spec-2026-11-19.md 5.1)의 장면은 x01 ~ x12(`xpScenes`) — 통 · 재료 목록 · 남는 신발 넣기 ·
 * 경험치 넣기 / 레벨업 · 아까운 재료 시트 · 결과(레벨업 · 경험치만, 실패 없음) · 달리는 중 · 크루 러닝 중 · 거절 · 빠진 재료. 숫자는 계약 문서의 예시다.
 * 아래 확률 강화 장면(upgradeScenes)은 서버에 0118 이 없을 때 · 1.20.x 에서 보낸 요청의 결과를 볼 때의 예전 길이다.
 *
 * 신발 강화 — 파란 톤 v4 강화 전달본(docs/redesign/blue-v4-2026-10/01-packages/stepup-upgrade-blue-claude-v19) 장면 UP01 ~ UP15 · UP18
 * (UP16 성공률 설명은 없다). UP17(일반 등급 강화 불가)은 2026-10-04 재료 규칙(forge-v2)으로 없어져, 그 자리에서 일반 신발이
 * 같은 등급 재료로 강화할 수 있는 장면을 찍는다. 상태를 화면에 바로 넣어 찍는다. 신발 번호 · 레벨 · 능력치 · 확률은 시안과 같은
 * 예시이고 이 테스트 화면에만 있다 — 계정 데이터가 아니다. 확률은 서버와 같은 식(ShoeForge)으로 계산한 값이다 —
 * 레전더리 Lv10 + 에픽 Lv10(한 등급 아래 5.6) · 레전더리 Lv1(같은 등급 3.0) · 에픽 Lv1(2.0) = 80.6%.
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

        // UP01 — 재료 선택 전: 슬롯 셋 비어 있음 · 0 / 3 · 현재 예상 성공률 70.0% · 버튼은 누를 수 없음
        show("s01-ready-empty", "upgrade-screen", editing(emptyList()))
        compose.onNodeWithTag("upgrade-count", useUnmergedTree = true).assertTextEquals("0 / 3")
        compose.onNodeWithTag("upgrade-rate", useUnmergedTree = true).assertTextContains("70.0%", substring = true)
        compose.onNodeWithTag("upgrade-primary").assertIsNotEnabled()
        noFormulaText()

        // UP02 — 재료 선택(하단 시트): 두 개 고른 임시 선택 · 재료별 보정 · 등급 차이 · 쓸 수 없는 줄은 까닭과 함께
        show("s02-picker", "upgrade-picker", editing(emptyList(), draft = listOf(M1.id, M2.id), sheet = UpgradeSheet.Picker))
        compose.onNodeWithTag("picker-count", useUnmergedTree = true).assertTextContains("2", substring = true)
        compose.onNodeWithTag("picker-apply").assertIsEnabled()
        compose.onNodeWithTag("picker-bonus-${M1.id}", useUnmergedTree = true).assertTextEquals("+5.6%p")
        compose.onNodeWithTag("picker-relation-${M1.id}", useUnmergedTree = true).assertTextEquals("한 등급 아래")
        compose.onNodeWithTag("picker-relation-${M2.id}", useUnmergedTree = true).assertTextEquals("같은 등급")
        compose.onNodeWithTag("picker-block-${M4_EQUIPPED.id}", useUnmergedTree = true).assertTextEquals("착용 중")
        compose.onAllNodesWithText("하위 등급", substring = true).assertCountEquals0()
        noFormulaText()

        // UP03 — 준비 완료: 3 / 3 · 서버 견적 80.6% · 성공 시 10 → 11
        show("s03-ready-full", "upgrade-screen", editing(all.map { it.id }, quote = QUOTE))
        compose.onNodeWithTag("upgrade-count", useUnmergedTree = true).assertTextEquals("3 / 3")
        compose.onNodeWithTag("upgrade-rate", useUnmergedTree = true).assertTextContains("80.6%", substring = true)
        compose.onAllNodesWithText("하위 등급", substring = true).assertCountEquals0()
        compose.onNodeWithTag("upgrade-primary").assertIsEnabled()
        noFormulaText()

        // 04 — 최종 확인(하단 시트)
        show("s04-confirm", "upgrade-confirm", editing(all.map { it.id }, quote = QUOTE, sheet = UpgradeSheet.Confirm(QUOTE)))
        compose.onNodeWithTag("confirm-rate", useUnmergedTree = true).assertTextContains("80.6%", substring = true)

        // 05 — 진행 중: 버튼은 누를 수 없고 다시 보내지 않는다
        show("s05-running", "upgrade-running", BASE.copy(phase = UpgradePhase.Running, running = all, quote = QUOTE))
        compose.onNodeWithTag("upgrade-primary").assertIsNotEnabled()

        // 06 — 성공: 서버가 준 실제 값(Lv 11)
        val success = ForgeResult(true, TARGET.id, all.map { it.id }, 806, 10, 11, ForgeStats(11, 20, 1850, 820))
        show("s06-success", "upgrade-success", BASE.copy(phase = UpgradePhase.Succeeded(success, STATS), running = all))

        // 07 — 실패: 레벨 그대로 · 재료 소각
        val failure = ForgeResult(false, TARGET.id, all.map { it.id }, 806, 10, 10, STATS)
        show("s07-failed", "upgrade-failed", BASE.copy(phase = UpgradePhase.Failed(failure), running = all))

        // UP08 — 두 개 넣음: 2 / 3 · 78.6%(미리보기) · 하나 더 고르기
        show("s08-two-selected", "upgrade-screen", editing(listOf(M1.id, M2.id)))
        compose.onNodeWithTag("upgrade-count", useUnmergedTree = true).assertTextEquals("2 / 3")
        compose.onNodeWithTag("upgrade-rate", useUnmergedTree = true).assertTextContains("78.6%", substring = true)
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
        val pending = ForgePending("demo-key", TARGET.id, all.map { it.id }, 806, System.currentTimeMillis() - 90_000)
        show("s15-result-unknown", "upgrade-unknown", BASE.copy(phase = UpgradePhase.Unknown(pending), running = all))
        compose.onNodeWithTag("upgrade-primary").assertIsEnabled()

        // (UP17 자리) 일반 신발도 강화한다 — 같은 등급(일반) 재료 3개 · 일반 Lv3 기본 91% + 3.0 · 3.0 · 3.0 = 100%(상한)
        val commonStats = ForgeStats(level = 3, maxLevel = 20, efficiencyBps = 150, comfortBps = 300)
        val commonTarget = TARGET.copy(id = COMMON_SHOE.id, rarity = Rarity.COMMON, stats = commonStats,
            basePermille = ShoeForge.basePermille(3))
        val commons = listOf(C1, C2, C3)
        show("s17-common-target", "upgrade-screen", BASE.copy(
            shoe = COMMON_SHOE, info = commonTarget, targetId = COMMON_SHOE.id,
            known = commons.associateBy { it.id }, selection = MaterialSelection(commons.map { it.id }),
            candidates = MaterialsLoad.Ready(commons),
        ))
        compose.onNodeWithTag("upgrade-rate", useUnmergedTree = true).assertTextContains("100.0%", substring = true)
        compose.onNodeWithTag("upgrade-primary").assertIsEnabled()
        show("s17-common-picker", "upgrade-picker", BASE.copy(
            shoe = COMMON_SHOE, info = commonTarget, targetId = COMMON_SHOE.id,
            known = commons.associateBy { it.id }, selection = MaterialSelection(emptyList(), draft = listOf(C1.id)),
            candidates = MaterialsLoad.Ready(commons), sheet = UpgradeSheet.Picker,
        ))
        compose.onNodeWithTag("picker-relation-${C1.id}", useUnmergedTree = true).assertTextEquals("같은 등급")

        // UP18 — 대상 사용 불가(판매 중 · 보유 상태 바뀜)
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

    @Test fun xpScenes() {
        runBlocking {
            ServiceLocator.userPrefs.setReducedMotion(true)
            ServiceLocator.userPrefs.setSounds(false)
            ServiceLocator.userPrefs.setHaptics(false)
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
        fun xp(ids: List<Long> = emptyList(), quote: XpQuote? = null, sheet: UpgradeSheet = UpgradeSheet.None, catalog: XpCatalog = XP_CATALOG) =
            XP_BASE.copy(xp = catalog, xpSelection = XpSelection(ids, catalog.maxMaterials), xpQuote = quote, sheet = sheet)

        show("x01-xp-empty", "upgrade-xp-bar", xp(), settle = 2_000)
        // x01 — 고르기 전: 통 39.7% · 0 / 10 · 대성공 확률(서버 값) · 주 버튼은 누를 수 없다
        show("x01-xp-empty", "upgrade-xp-bar", xp())
        compose.onNodeWithTag("upgrade-xp-value", useUnmergedTree = true).assertTextEquals("39.7%")
        compose.onNodeWithTag("upgrade-count", useUnmergedTree = true).assertTextEquals("0 / 10")
        compose.onNodeWithTag("upgrade-crit-odds", useUnmergedTree = true).assertTextContains("10%", substring = true)
        compose.onNodeWithTag("upgrade-primary").assertIsNotEnabled()
        compose.onNodeWithTag("upgrade-primary").assertTextContains(string(R.string.fxp_need_pick))
        // 재료 줄은 목록(보이는 줄만 그린다)에 있다 — 그 줄까지 내려 본다
        val list = compose.onNode(androidx.compose.ui.test.hasScrollAction())
        list.performScrollToNode(androidx.compose.ui.test.hasTestTag("xp-row-${X1.id}"))
        compose.onNodeWithTag("xp-gain-${X1.id}", useUnmergedTree = true).assertTextEquals("+33.3%")
        list.performScrollToNode(androidx.compose.ui.test.hasTestTag("xp-row-${X_TOP.id}"))
        compose.onNodeWithTag("xp-precious-${X_TOP.id}", useUnmergedTree = true).assertTextEquals(string(R.string.fxp_precious_tag))
        list.performScrollToNode(androidx.compose.ui.test.hasTestTag("xp-row-${X_EQUIPPED.id}"))
        compose.onNodeWithTag("xp-block-${X_EQUIPPED.id}", useUnmergedTree = true).assertTextEquals("착용 중")
        list.performScrollToNode(androidx.compose.ui.test.hasTestTag("xp-row-${X_PENDING.id}"))
        compose.onNodeWithTag("xp-block-${X_PENDING.id}", useUnmergedTree = true).assertTextEquals(string(R.string.fxp_block_pending_run))

        // x02 — 한 켤레(견적 — 레벨업 아님): 39.7% → 73.0% · 경험치 넣기
        show("x02-xp-one", "upgrade-xp-bar", xp(listOf(X1.id), quote = QUOTE_ONE))
        compose.onNodeWithTag("upgrade-xp-value", useUnmergedTree = true).assertTextEquals("39.7% → 73.0%")
        compose.onNodeWithTag("upgrade-xp-caption", useUnmergedTree = true).assertTextContains("+33.3%", substring = true)
        compose.onNodeWithTag("upgrade-primary").assertIsEnabled()
        compose.onNodeWithTag("upgrade-primary").assertTextContains(string(R.string.fxp_action_add))

        // x03 — 레벨업 견적(최소 Lv. 4) — 주 버튼 "레벨업", 레벨 줄 "3 → 4+"
        show("x03-xp-level-up", "upgrade-xp-bar", xp(listOf(X1.id, X2.id), quote = QUOTE_LEVEL))
        compose.onNodeWithTag("upgrade-primary").assertTextContains(string(R.string.fxp_action_level_up))
        compose.onNodeWithTag("upgrade-value-level", useUnmergedTree = true).assertTextContains("4+", substring = true)
        compose.onNodeWithTag("upgrade-gain-energy", useUnmergedTree = true).assertTextContains("28 → 30", substring = true)

        // x04 — 아까운 재료: 가장 높은 레벨(에너지 30 → 10) · 가장 높은 효율(상한 1.36 → 1) · 이름 붙인 신발
        val precious = UpgradeSheet.Precious(
            QUOTE_PRECIOUS,
            listOf(X_TOP to listOf(ForgePrecious.LEVELED, ForgePrecious.TOP_LEVEL, ForgePrecious.TOP_EFFICIENCY),
                X2 to listOf(ForgePrecious.NICKNAMED)),
        )
        show("x04-xp-precious", "upgrade-precious", xp(listOf(X_TOP.id, X2.id), quote = QUOTE_PRECIOUS, sheet = precious))
        compose.onNodeWithTag("precious-why-${X_TOP.id}-top_level", useUnmergedTree = true).assertTextContains("28 → 10", substring = true)
        compose.onNodeWithTag("precious-why-${X_TOP.id}-top_efficiency", useUnmergedTree = true).assertTextContains("1.36", substring = true)
        compose.onNodeWithTag("precious-confirm").assertIsEnabled()

        // x05 — 결과 레벨업: Lv. 3 → Lv. 5 · 대성공 1켤레 ×2 · 상급 상자 1개
        show("x05-xp-done-level", "upgrade-xp-done", XP_BASE.copy(phase = UpgradePhase.XpDone(RESULT_LEVEL, XP_TARGET.bar, XP_STATS),
            xpRunning = listOf(X1, X2)))
        compose.onNodeWithTag("upgrade-xp-done-title", useUnmergedTree = true).assertTextEquals("Lv. 3 → Lv. 5")
        compose.onNodeWithTag("xp-crit-${X1.id}", useUnmergedTree = true).assertTextContains("×2", substring = true)
        compose.onNodeWithTag("upgrade-got-boxes", useUnmergedTree = true).assertExists()
        compose.onAllNodesWithText(string(R.string.upg_failed_title)).assertCountEquals0()

        // x06 — 결과 경험치만: 경험치 +33.3% · 지금 73.0%
        show("x06-xp-done-xp", "upgrade-xp-done", XP_BASE.copy(phase = UpgradePhase.XpDone(RESULT_XP, XP_TARGET.bar, XP_STATS), xpRunning = listOf(X1)))
        compose.onNodeWithTag("upgrade-xp-done-title", useUnmergedTree = true).assertTextContains("+33.3%", substring = true)
        compose.onNodeWithTag("upgrade-xp-done-now", useUnmergedTree = true).assertTextContains("73.0%", substring = true)

        // x07 — 달리는 중 · x08 — 크루 러닝 중(서버 RUNNING)
        show("x07-run-active", "upgrade-run-active", XP_BASE.copy(phase = UpgradePhase.RunActive))
        show("x08-crew-running", "upgrade-crew-running", XP_BASE.copy(xp = null, xpBlock = ForgeTargetBlock.RUNNING,
            phase = UpgradePhase.Blocked(ForgeTargetBlock.RUNNING)))

        // x09 — 거절(연결 끊김이 아니다) · x10 — 서버가 뺀 재료(닳아서 값 없음)
        show("x09-rejected", "upgrade-rejected", xp(sheet = UpgradeSheet.Rejected("MATERIALS_INVALID")))
        compose.onAllNodesWithText(string(R.string.upg_offline_title)).assertCountEquals0()
        show("x10-xp-changed", "upgrade-xp-changed", xp(sheet = UpgradeSheet.XpChanged(
            listOf(X1.copy(block = ForgeMaterialBlock.NO_VALUE)), com.stepup.android.ui.screens.items.XpChange.NO_VALUE)))

        // x11 — 넣는 중 · x12 — 결과 미확인(경험치 요청 — 같은 요청만 다시 확인)
        show("x11-xp-running", "upgrade-running", XP_BASE.copy(phase = UpgradePhase.Running, xpRunning = listOf(X1, X2)))
        compose.onNodeWithTag("upgrade-primary").assertIsNotEnabled()
        val pending = ForgePending("demo-key", XP_TARGET.id, listOf(X1.id), 0, System.currentTimeMillis() - 90_000, ForgeKind.XP, "md5")
        show("x12-xp-unknown", "upgrade-unknown", XP_BASE.copy(phase = UpgradePhase.Unknown(pending), xpRunning = listOf(X1)))
        compose.onNodeWithTag("upgrade-unknown-note", useUnmergedTree = true).assertTextContains(string(R.string.fxp_unknown_note), substring = true)

        // 360dp · 큰 글씨 — 레벨업 견적 · 결과
        narrow = true
        large = true
        show("w360-large-x03-xp-level-up", "upgrade-xp-bar", xp(listOf(X1.id, X2.id), quote = QUOTE_LEVEL))
        show("w360-large-x05-xp-done-level", "upgrade-xp-done", XP_BASE.copy(phase = UpgradePhase.XpDone(RESULT_LEVEL, XP_TARGET.bar, XP_STATS),
            xpRunning = listOf(X1, X2)))
        large = false
        narrow = false
    }

    private fun string(id: Int): String = compose.activity.getString(id)

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
        val M2 = material(shoe(9134, Rarity.LEGENDARY, 1302, level = 1, number = 34))
        val M3 = material(shoe(9148, Rarity.EPIC, 1202, level = 1, number = 48))
        val M4_EQUIPPED = material(shoe(9150, Rarity.EPIC, 1203, level = 4, number = 50, equipped = true), ForgeMaterialBlock.EQUIPPED)
        val M5_CHAIN = material(shoe(9151, Rarity.EPIC, 1201, level = 2, number = 51), ForgeMaterialBlock.ON_CHAIN)

        fun common(id: Long, number: Int) = ForgeMaterial(
            shoe(id, Rarity.COMMON, null, level = 1, number = number), ShoeForge.bonusPermille(Rarity.COMMON, Rarity.COMMON, 1), null,
        )
        val C1 = common(9161, 61)
        val C2 = common(9162, 62)
        val C3 = common(9163, 63)

        val QUOTE = ForgeQuote(
            ratePermille = ShoeForge.ratePermille(TARGET.basePermille, listOf(M1, M2, M3).map { it.bonusPermille }),
            before = STATS, after = TARGET.previewNext(), quoteVersion = "test", materialIds = listOf(M1.id, M2.id, M3.id),
        )

        val BASE = ShoeUpgradeState(
            targetId = TARGET.id, shoe = TARGET_SHOE, info = TARGET,
            known = listOf(M1, M2, M3, M4_EQUIPPED, M5_CHAIN).associateBy { it.id },
            phase = UpgradePhase.Editing,
        )

        // ── 경험치 강화(계약 문서의 예 — 레어 Lv3, 통 1,200 / 3,020) ──
        val XP_SHOE = shoe(9201, Rarity.RARE, 1101, level = 3, number = 12).copy(server = server(900, 600))
        val XP_TARGET = XpTarget(XP_SHOE.id, Rarity.RARE, 20, ForgeBar(3, 1200, 3020, 397))
        val XP_STATS = ForgeStats(level = 3, maxLevel = 20, efficiencyBps = 900, comfortBps = 600)

        fun xpItem(shoe: Sneaker, value: Int, stars: Int = 2, block: ForgeMaterialBlock? = null, precious: List<ForgePrecious> = emptyList(),
                   durability: Double = 100.0) = XpMaterial(
            shoe = shoe, stars = stars, durabilityPct = durability, own = value, inherit = 0, repairPart = 0, value = value,
            tenths = ForgeXp.tenths(value, 3020) ?: 0, block = block, precious = precious,
        )

        val X1 = xpItem(shoe(9211, Rarity.COMMON, null, level = 1, number = 211), 1007)
        val X2 = xpItem(shoe(9212, Rarity.COMMON, null, level = 1, number = 212), 1180, stars = 3)
        val X_TOP = xpItem(shoe(9213, Rarity.RARE, 1102, level = 9, number = 213), 9800,
            precious = listOf(ForgePrecious.LEVELED, ForgePrecious.TOP_LEVEL, ForgePrecious.TOP_EFFICIENCY))
        val X_EQUIPPED = xpItem(shoe(9214, Rarity.COMMON, null, level = 1, number = 214, equipped = true), 1000, block = ForgeMaterialBlock.EQUIPPED)
        val X_PENDING = xpItem(shoe(9215, Rarity.COMMON, null, level = 1, number = 215), 880, block = ForgeMaterialBlock.PENDING_RUN,
            durability = 64.0)

        val XP_CATALOG = XpCatalog(
            target = XP_TARGET, items = listOf(X1, X2, X_TOP, X_EQUIPPED, X_PENDING), crit = XpCrit(0.10, 2.0),
            maxMaterials = 10, ruleVersion = ForgeXp.RULE_VERSION,
        )

        fun quote(ids: List<Long>, after: ForgeBar, levels: Int, gain: Int, energy: Pair<Int, Int> = 28 to 28,
                  cap: Pair<Double, Double> = 1.36 to 1.36) = XpQuote(
            quoteVersion = "test", before = XP_TARGET.bar, after = after, levels = levels, overflow = 0, gain = gain,
            materials = ids.map { XpQuoteMaterial(it, 1000, 0, 1000, null) }, crit = XpCrit(0.10, 2.0),
            energyBefore = energy.first, energyAfter = energy.second, capBefore = cap.first, capAfter = cap.second,
            maxLevel = 20, overflowPossible = false, materialIds = ids,
        )

        val QUOTE_ONE = quote(listOf(X1.id), ForgeBar(3, 2207, 3020, 730), levels = 0, gain = 1007)
        val QUOTE_LEVEL = quote(listOf(X1.id, X2.id), ForgeBar(4, 367, 3140, 116), levels = 1, gain = 2187, energy = 28 to 30)
        val QUOTE_PRECIOUS = quote(listOf(X_TOP.id, X2.id), ForgeBar(6, 900, 3390, 265), levels = 3, gain = 10980,
            energy = 28 to 10, cap = 1.36 to 1.0)

        val RESULT_LEVEL = XpResult(
            targetId = XP_TARGET.id, levelBefore = 3, levelAfter = 5, levels = 2, gain = 6194,
            after = ForgeBar(5, 40, 3260, 12), overflow = 0,
            materials = listOf(XpResultMaterial(X1.id, 1007, 0, crit = true, value = 2014), XpResultMaterial(X2.id, 1180, 0, false, 1180)),
            crits = 1, levelBoxes = listOf(5),
        )
        val RESULT_XP = XpResult(
            targetId = XP_TARGET.id, levelBefore = 3, levelAfter = 3, levels = 0, gain = 1007,
            after = ForgeBar(3, 2207, 3020, 730), overflow = 0,
            materials = listOf(XpResultMaterial(X1.id, 1007, 0, crit = false, value = 1007)), crits = 0, levelBoxes = emptyList(),
        )

        val XP_BASE = ShoeUpgradeState(
            targetId = XP_TARGET.id, shoe = XP_SHOE, xp = XP_CATALOG, phase = UpgradePhase.Editing,
        )
    }
}
