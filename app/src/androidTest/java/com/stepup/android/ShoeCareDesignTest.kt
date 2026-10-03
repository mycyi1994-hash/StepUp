package com.stepup.android

import androidx.activity.ComponentActivity
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
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.domain.Faction
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.ServerStats
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.components.CommerceBackdrop
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.items.CareBlock
import com.stepup.android.ui.screens.items.EquipFailure
import com.stepup.android.ui.screens.items.EquipResult
import com.stepup.android.ui.screens.items.RepairPhase
import com.stepup.android.ui.screens.items.ShoeCareStatus
import com.stepup.android.ui.screens.items.ShoeDetailContent
import com.stepup.android.ui.screens.items.ShoeDetailState
import com.stepup.android.ui.screens.items.ShoeUpgradeContent
import com.stepup.android.ui.screens.items.UpgradePhase
import com.stepup.android.ui.screens.items.UpgradeRejection
import com.stepup.android.ui.screens.items.repairQuoteOf
import com.stepup.android.ui.screens.items.sceneTag
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test

/**
 * 파란 톤 v4 신발 상세 · 수리 · 강화(docs/redesign/blue-v4-2026-10 — SD01 ~ SD19 · RP01 ~ RP11 · UP01 ~ UP18 중 지금 계약으로 그릴 수 있는 장면).
 * 상태를 화면에 바로 넣어 찍는다(서버 · 저장소를 건드리지 않는다). 번호 · 능력치 · 잔액은 이 테스트 화면의 예시다 — 앱 데이터가 아니다.
 * 강화의 재료 선택 · 성공률 · 소각(UP02 · 07 · 08 ~ 12 · 17)은 서버 계약이 없어 장면이 없다.
 */
class ShoeCareDesignTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

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
        fun upgrade(shoe: Sneaker = TARGET, phase: UpgradePhase? = null, balance: Double? = 5_000.0, sheet: String? = null):
            @Composable () -> Unit = {
                ShoeUpgradeContent(
                    state = ShoeDetailState.Ready(shoe, WORN), balance = balance, phase = phase,
                    onBack = {}, onOpenOwned = {}, onStart = {}, onRecheck = {}, onReset = {}, onSignIn = {}, initialSheet = sheet,
                )
            }

        // SD01 · SD03 · SD12 · SD18 · SD19
        show("SD01-detail", "shoe-art", detail())
        compose.onNodeWithTag("shoe-upgrade-open").assertIsEnabled()
        compose.onNodeWithTag("detail-primary-action").assertIsEnabled()
        show("SD03-equipping", "shoe-art", detail(equipping = true))
        compose.onNodeWithTag("shoe-upgrade-open").assertIsNotEnabled()
        show("SD12-first", "shoe-first-hint", detail(state = ShoeDetailState.Ready(RARE, null)))
        show("SD18-upgrade-pending", "shoe-upgrade-pending", detail(care = ShoeCareStatus(upgradePending = true)))
        compose.onNodeWithTag("detail-primary-action").assertIsNotEnabled()
        show("SD19-repair-pending", "shoe-repair-pending", detail(care = ShoeCareStatus(repairPending = true)))
        compose.onNodeWithTag("shoe-upgrade-open").assertIsNotEnabled()
        // SD05 · SD06 · SD07
        show("SD05-rejected", "shoe-equip-error",
            detail(result = EquipResult.NotWorn(RARE.id, EquipFailure.REJECTED, WORN.id, confirmed = true)))
        show("SD06-sign-in", "shoe-equip-error",
            detail(result = EquipResult.NotWorn(RARE.id, EquipFailure.SIGN_IN, WORN.id, confirmed = true)))
        show("SD07-unconfirmed", "shoe-equip-error",
            detail(result = EquipResult.NotWorn(RARE.id, EquipFailure.OFFLINE, WORN.id, confirmed = false)))
        compose.onNodeWithTag("shoe-error-retry").assertTextEquals(string(R.string.care_wear_check))
        // SD08 · SD13 · SD14 · SD15 · SD16
        show("SD08-loading", "shoe-stat-cells-loading", detail(state = ShoeDetailState.Loading))
        show("SD13-manage", "shoe-manage-sheet", detail(sheet = "Manage"))
        compose.onNodeWithTag("shoe-manage-sell").assertIsEnabled()
        show("SD14-manage-worn", "shoe-manage-sheet", detail(state = ShoeDetailState.Ready(WORN, WORN), sheet = "Manage"))
        compose.onNodeWithTag("shoe-manage-sell").assertIsNotEnabled()
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

        // UP01(지금 계약) · UP04 · UP05 · UP06 · UP13 · UP14 · UP15 · UP18
        show("UP01-ready", "upgrade-ready", upgrade())
        compose.onNodeWithTag("upgrade-primary").assertIsEnabled()
        compose.onAllNodesWithTag("upgrade-rejected").assertCountEquals(0)
        show("UP01b-short", "upgrade-short", upgrade(balance = 100.0))
        compose.onNodeWithTag("upgrade-primary").assertIsNotEnabled()
        show("UP04-confirm", "upgrade-confirm-sheet", upgrade(sheet = "Confirm"))
        show("UP05-sending", "upgrade-sending", upgrade(phase = UpgradePhase.Sending(TARGET)))
        show("UP06-success", "upgrade-success", upgrade(shoe = TARGET.copy(level = 11),
            phase = UpgradePhase.Success(TARGET, upgraded(TARGET))))
        show("UP13-max", "upgrade-max", upgrade(shoe = TARGET.copy(level = 30)))
        show("UP14-offline", "upgrade-offline-sheet", upgrade(sheet = "Offline"))
        show("UP15-unknown", "upgrade-unknown", upgrade(phase = UpgradePhase.Unknown(TARGET, accepted = false)))
        show("UP18-blocked", "upgrade-blocked", upgrade(shoe = TARGET.copy(server = TARGET.server!!.copy(status = "LISTED"))))
        show("UP18b-rejected", "upgrade-no-charge", upgrade(phase = UpgradePhase.Rejected(UpgradeRejection.OTHER)))

        // 밝은 테마 · 큰 글씨 · 320dp
        compose.runOnIdle { light = true }
        show("L-SD01", "shoe-art", detail())
        show("L-RP01", "repair-confirm", detail(state = worn, repair = RepairPhase.Confirm(quote, 83.07)))
        show("L-UP01", "upgrade-ready", upgrade())
        compose.runOnIdle { light = false; large = true; narrow = true }
        show("N-SD01", "shoe-art", detail())
        show("N-RP03", "repair-sending", detail(state = worn, repair = RepairPhase.Sending(quote, 83.07)))
        show("N-UP01", "upgrade-ready", upgrade())
        show("N-UP15", "upgrade-unknown", upgrade(phase = UpgradePhase.Unknown(TARGET, accepted = true)))
    }

    private fun upgraded(s: Sneaker) = s.copy(
        level = s.level + 1, server = s.server!!.copy(efficiencyBps = s.server!!.efficiencyBps + 50, comfortBps = s.server!!.comfortBps + 20),
    )

    private fun string(id: Int, vararg args: Any): String = compose.activity.getString(id, *args)

    private fun show(name: String, readyTag: String, content: @Composable () -> Unit) {
        compose.runOnIdle { scene = { key(name) { content() } } }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(readyTag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(500)
        captureDisplay(File(directory, "$name.png"))
    }

    private companion object {
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")

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
        val TARGET = Sneaker(
            id = 9302, faction = Faction.WIND, rarity = Rarity.LEGENDARY, variant = 0, level = 10, mintNumber = 7,
            luck = 1.0, comfort = 1.0, durability = 100, equipped = false, acquiredAt = 1_790_000_000_000L,
            server = server(1_800, 800, maxLevel = 30, cost = 3_000.0), modelId = 1302,
        )
    }
}
