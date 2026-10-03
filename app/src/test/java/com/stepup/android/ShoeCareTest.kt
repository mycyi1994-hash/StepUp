package com.stepup.android

import com.stepup.android.data.repo.EconomyOutcome
import com.stepup.android.domain.Faction
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.ServerStats
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.screens.items.CareBlock
import com.stepup.android.ui.screens.items.DetailStat
import com.stepup.android.ui.screens.items.RepairPhase
import com.stepup.android.ui.screens.items.ShoeCareBackend
import com.stepup.android.ui.screens.items.ShoeCareStore
import com.stepup.android.ui.screens.items.UpgradePhase
import com.stepup.android.ui.screens.items.UpgradeRejection
import com.stepup.android.ui.screens.items.atMaxLevel
import com.stepup.android.ui.screens.items.balanceAfter
import com.stepup.android.ui.screens.items.careBlockOf
import com.stepup.android.ui.screens.items.formatSupExact
import com.stepup.android.ui.screens.items.pending
import com.stepup.android.ui.screens.items.repairGate
import com.stepup.android.ui.screens.items.repairQuoteOf
import com.stepup.android.ui.screens.items.repairShortfall
import com.stepup.android.ui.screens.items.upgradeBlockOf
import com.stepup.android.ui.screens.items.upgradePreview
import com.stepup.android.ui.screens.items.upgradeRows
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * 신발 상세 · 수리 · 강화(파란 톤 v4) — 수리 견적(서버 sneaker_repair 와 같은 계산) · 보내기 전 판정 · 한 번만 보내기 ·
 * 결과 미확인의 읽기 확인, 강화(지금 계약: SUP 를 내고 레벨 +1)의 미리보기 · 결말. 예시 수는 전달본 04-데이터와-금액기준.md 의 예시다.
 */
class ShoeCareTest {

    // ── 금액 ────────────────────────────────────────────────────

    @Test fun `비용은 소수 4자리까지 그대로 — 12_5 를 13 으로 올리지 않는다`() {
        assertEquals("12.5", formatSupExact(BigDecimal("12.5000")))
        assertEquals("50", formatSupExact(50.0))
        assertEquals("1,234.5678", formatSupExact(1234.5678))
        assertEquals("0.625", formatSupExact(0.625))
    }

    // ── 수리 견적 ──────────────────────────────────────────────

    @Test fun `레어 Lv5 68 에서 100 은 50 SUP, 92 에서 100 은 12_5 SUP`() {
        val q68 = requireNotNull(repairQuoteOf(server(durability = 68.0)))
        assertEquals(0, BigDecimal("32").compareTo(q68.points))
        assertEquals(0, BigDecimal("50").compareTo(q68.cost))
        val q92 = requireNotNull(repairQuoteOf(server(durability = 92.0)))
        assertEquals(0, BigDecimal("12.5").compareTo(q92.cost))
        assertEquals(70.57, balanceAfter(83.07, q92.cost), 1e-9)
        assertEquals(33.07, balanceAfter(83.07, q68.cost), 1e-9)
    }

    @Test fun `99_6 은 표시용 99 가 아니라 원래 값으로 계산한다`() {
        val quote = requireNotNull(repairQuoteOf(server(durability = 99.6)))
        assertEquals(0, BigDecimal("0.4").compareTo(quote.points))
        assertEquals(0, BigDecimal("0.625").compareTo(quote.cost))
    }

    @Test fun `잔액 20 에 비용 50 이면 30 SUP 부족, 잔액이 넉넉하면 0`() {
        val quote = requireNotNull(repairQuoteOf(server(durability = 68.0)))
        assertEquals(0, BigDecimal("30").compareTo(repairShortfall(quote, 20.0)))
        assertEquals(0, BigDecimal.ZERO.compareTo(repairShortfall(quote, 83.07)))
    }

    @Test fun `보내기 전 판정 — 가득 · 부족 · 판매 중 · 체인 · 로그인 · 없는 신발 · 확인`() {
        assertTrue(repairGate(server(durability = 100.0), 10.0, true) is RepairPhase.NotNeeded)
        assertTrue(repairGate(server(durability = 68.0), 20.0, true) is RepairPhase.Insufficient)
        assertEquals(RepairPhase.Blocked(CareBlock.LISTED), repairGate(server(status = "LISTED"), 999.0, true))
        assertEquals(RepairPhase.Blocked(CareBlock.ON_CHAIN), repairGate(server(chain = "CHAIN"), 999.0, true))
        assertEquals(RepairPhase.SignIn, repairGate(server(), 999.0, serverEconomy = false))
        assertEquals(RepairPhase.SignIn, repairGate(server().copy(server = null), 999.0, true))
        assertEquals(RepairPhase.Blocked(CareBlock.MISSING), repairGate(null, 999.0, true))
        // 폰에서 올린 예전 신발도 서버는 수리를 받는다 — 강화 가능(upgradable)으로 수리 가능을 정하지 않는다
        assertTrue(repairGate(server(origin = "IMPORT", durability = 68.0), 999.0, true) is RepairPhase.Confirm)
    }

    @Test fun `서버 거절 문구를 확인된 까닭으로 옮긴다`() {
        assertEquals(CareBlock.LISTED, careBlockOf("판매 중인 신발입니다"))
        assertEquals(CareBlock.ON_CHAIN, careBlockOf("체인에 있는 신발입니다. 먼저 앱으로 넣어 주세요"))
        assertEquals(CareBlock.MISSING, careBlockOf("내 신발이 아닙니다"))
        assertEquals(CareBlock.OTHER, careBlockOf("알 수 없는 오류"))
    }

    // ── 수리 진행(한 번만 보내기 · 결과 확인은 읽기만) ─────────────

    @Test fun `수리 확인을 두 번 눌러도 한 번만 보내고, 동기화된 가득 내구도가 있어야 완료`() {
        val backend = FakeBackend(server(durability = 68.0), balance = 83.07)
        val store = ShoeCareStore(backend, CoroutineScope(Dispatchers.Unconfined))
        store.openRepair(ID)
        assertTrue(store.repair.value[ID] is RepairPhase.Confirm)
        backend.onRepair = {
            backend.shoe = server(durability = 100.0)
            backend.balance = 33.07
            EconomyOutcome.Ok
        }
        store.confirmRepair(ID)
        store.confirmRepair(ID)
        assertEquals(1, backend.repairs)
        val done = store.repair.value[ID] as RepairPhase.Done
        assertEquals(33.07, done.balance, 1e-9)
        assertEquals(0, BigDecimal("50").compareTo(done.quote.cost))
    }

    @Test fun `견적이 바뀌었으면 보내지 않고 새 견적을 다시 확인받는다(RP10)`() {
        val backend = FakeBackend(server(durability = 68.0), balance = 83.07)
        val store = ShoeCareStore(backend, CoroutineScope(Dispatchers.Unconfined))
        store.openRepair(ID)
        backend.shoe = server(durability = 92.0)
        store.confirmRepair(ID)
        assertEquals(0, backend.repairs)
        val changed = store.repair.value[ID] as RepairPhase.Confirm
        assertTrue(changed.changed)
        assertEquals(0, BigDecimal("12.5").compareTo(changed.quote.cost))
    }

    @Test fun `응답을 못 받으면 결과 미확인 — 결과 확인은 읽기만 하고 수리를 다시 보내지 않는다`() {
        val backend = FakeBackend(server(durability = 68.0), balance = 83.07)
        val store = ShoeCareStore(backend, CoroutineScope(Dispatchers.Unconfined))
        store.openRepair(ID)
        backend.onRepair = { EconomyOutcome.Offline }
        store.confirmRepair(ID)
        val unknown = store.repair.value[ID] as RepairPhase.Unknown
        assertTrue(unknown.pending)
        // 닫아도 남는다(SD19), 다시 열어도 새로 조회하지 않는다
        store.closeRepair(ID)
        store.openRepair(ID)
        assertTrue(store.repair.value[ID] is RepairPhase.Unknown)
        val refreshes = backend.refreshes
        backend.shoe = server(durability = 100.0)
        store.recheckRepair(ID)
        assertEquals(1, backend.repairs)
        assertEquals(refreshes + 1, backend.refreshes)
        // 요청별 결과 조회가 없어 이 수리가 반영됐다고 단정하지 않는다 — 확인한 값만
        val after = store.repair.value[ID] as RepairPhase.Unknown
        assertEquals(100.0, after.observed!!, 0.0)
        assertFalse(after.pending)
    }

    @Test fun `서버가 처리했다고 답했지만 아직 옛 값이면 확인 중으로 남고, 다시 읽어 가득이면 완료`() {
        val backend = FakeBackend(server(durability = 68.0), balance = 83.07)
        val store = ShoeCareStore(backend, CoroutineScope(Dispatchers.Unconfined))
        store.openRepair(ID)
        backend.onRepair = { EconomyOutcome.Ok }
        store.confirmRepair(ID)
        val unknown = store.repair.value[ID] as RepairPhase.Unknown
        assertTrue(unknown.accepted)
        backend.shoe = server(durability = 100.0)
        backend.balance = 33.07
        store.recheckRepair(ID)
        assertTrue(store.repair.value[ID] is RepairPhase.Done)
    }

    @Test fun `조회 실패는 RP07, 로그인 필요는 RP11 — 수리를 보내지 않는다`() {
        val backend = FakeBackend(server(durability = 68.0), balance = 83.07)
        val store = ShoeCareStore(backend, CoroutineScope(Dispatchers.Unconfined))
        backend.refreshOutcome = EconomyOutcome.Offline
        store.openRepair(ID)
        assertEquals(RepairPhase.LoadFailed, store.repair.value[ID])
        backend.refreshOutcome = EconomyOutcome.SignInRequired
        store.reloadRepair(ID)
        assertEquals(RepairPhase.SignIn, store.repair.value[ID])
        assertEquals(0, backend.repairs)
    }

    // ── 강화(지금 계약) ────────────────────────────────────────

    @Test fun `미리보기는 서버 레벨 한 칸 몫(효율 +0_5 · 착화감 +0_2, 20 상한) — 내구도 줄은 없다`() {
        val shoe = server(efficiencyBps = 1_800, comfortBps = 800, level = 10, maxLevel = 30)
        val next = upgradePreview(shoe)
        assertEquals(11, next.level)
        val rows = upgradeRows(shoe, next)
        assertEquals(listOf(DetailStat.LEVEL, DetailStat.EFFICIENCY, DetailStat.COMFORT), rows.map { it.stat })
        assertEquals("10" to "11", rows[0].now to rows[0].next)
        assertEquals("+18.0%" to "+18.5%", rows[1].now to rows[1].next)
        assertEquals("8.0%" to "8.2%", rows[2].now to rows[2].next)
        // 레벨 분모는 서버가 준 상한(30) — 화면만 20 으로 바꾸지 않는다
        assertEquals(10f / 30f, rows[0].nowFraction, 1e-6f)
        val capped = upgradePreview(server(comfortBps = 1_990))
        assertEquals(2_000, capped.server!!.comfortBps)
        // 결과 미확인 · 최대 화면은 다음 값을 그리지 않는다
        assertNull(upgradeRows(shoe, null)[0].next)
    }

    @Test fun `강화할 수 없는 까닭 — 판매 중 · 체인 · 예전 신발, 최대 레벨은 서버 상한으로`() {
        assertEquals(CareBlock.LISTED, upgradeBlockOf(server(status = "LISTED")))
        assertEquals(CareBlock.ON_CHAIN, upgradeBlockOf(server(chain = "CHAIN")))
        assertEquals(CareBlock.LEGACY, upgradeBlockOf(server(origin = "IMPORT")))
        assertNull(upgradeBlockOf(server()))
        assertTrue(atMaxLevel(server(level = 30, maxLevel = 30)))
        assertFalse(atMaxLevel(server(level = 20, maxLevel = 30)))
    }

    @Test fun `강화는 한 번만 보내고 서버가 돌려준 새 레벨로만 성공`() {
        val before = server(level = 10, maxLevel = 30)
        val backend = FakeBackend(before, balance = 5_000.0)
        val store = ShoeCareStore(backend, CoroutineScope(Dispatchers.Unconfined))
        backend.onUpgrade = {
            // 보내는 중에 다시 눌러도(연타) 두 번째 요청은 나가지 않는다
            store.startUpgrade(before)
            EconomyOutcome.Ok to before.copy(level = 11)
        }
        store.startUpgrade(before)
        assertEquals(1, backend.upgrades)
        val success = store.upgrade.value[ID] as UpgradePhase.Success
        assertEquals(11, success.after.level)
    }

    @Test fun `강화 응답을 못 받으면 결과 미확인 — 다시 확인은 읽기만, 새 강화를 막는다`() {
        val before = server(level = 10, maxLevel = 30)
        val backend = FakeBackend(before, balance = 5_000.0)
        val store = ShoeCareStore(backend, CoroutineScope(Dispatchers.Unconfined))
        backend.onUpgrade = { EconomyOutcome.Offline to null }
        store.startUpgrade(before)
        assertTrue(store.upgrade.value[ID] is UpgradePhase.Unknown)
        assertTrue(store.busy(ID))
        store.startUpgrade(before)
        store.clearUpgrade(ID)
        assertEquals(1, backend.upgrades)
        assertTrue(store.upgrade.value[ID] is UpgradePhase.Unknown)
        // 다시 읽었는데 그대로면 미확인(값만 확인), 레벨이 올라 있으면 그 값으로 보인다
        store.recheckUpgrade(ID)
        assertTrue((store.upgrade.value[ID] as UpgradePhase.Unknown).synced)
        backend.shoe = before.copy(level = 11)
        store.recheckUpgrade(ID)
        val seen = store.upgrade.value[ID] as UpgradePhase.Success
        assertTrue(seen.observed)
        assertEquals(1, backend.upgrades)
    }

    @Test fun `서버 거절은 차감 없는 거절로 — 확률 실패로 말하지 않는다`() {
        val before = server(level = 10, maxLevel = 30)
        val backend = FakeBackend(before, balance = 0.0)
        val store = ShoeCareStore(backend, CoroutineScope(Dispatchers.Unconfined))
        backend.onUpgrade = { EconomyOutcome.NotEnoughBalance to null }
        store.startUpgrade(before)
        assertEquals(UpgradePhase.Rejected(UpgradeRejection.NOT_ENOUGH_BALANCE), store.upgrade.value[ID])
        assertFalse(store.busy(ID))
        store.clearUpgrade(ID)
        assertNull(store.upgrade.value[ID])
    }

    // ── 도우미 ─────────────────────────────────────────────────

    private class FakeBackend(var shoe: Sneaker?, var balance: Double?) : ShoeCareBackend {
        var refreshOutcome: EconomyOutcome = EconomyOutcome.Ok
        var refreshes = 0
        var repairs = 0
        var upgrades = 0
        var onRepair: () -> EconomyOutcome = { EconomyOutcome.Ok }
        var onUpgrade: () -> Pair<EconomyOutcome, Sneaker?> = { EconomyOutcome.Ok to null }
        override val serverEconomy: Boolean = true
        override suspend fun refresh(): EconomyOutcome = refreshOutcome.also { refreshes++ }
        override suspend fun shoe(id: Long): Sneaker? = shoe?.takeIf { it.id == id }
        override suspend fun balance(): Double? = balance
        override suspend fun repair(id: Long): EconomyOutcome = onRepair().also { repairs++ }
        override suspend fun upgrade(id: Long): Pair<EconomyOutcome, Sneaker?> = onUpgrade().also {
            upgrades++
            it.second?.let { s -> shoe = s }
        }
    }

    private companion object {
        const val ID = 7L

        /** 레어 Lv5 — 서버 단가 (1 + 0.25 × 1) × (1 + 0.05 × 5) = 1.5625 */
        fun server(
            durability: Double = 92.0,
            status: String = "OWNED",
            chain: String = "APP",
            origin: String = "DRAW",
            level: Int = 5,
            maxLevel: Int = 15,
            efficiencyBps: Int = 800,
            comfortBps: Int = 580,
        ) = Sneaker(
            id = ID, faction = Faction.WIND, rarity = Rarity.RARE, variant = 0, level = level, mintNumber = 7,
            luck = 1.0, comfort = 1.0, durability = durability.toInt(), equipped = false, acquiredAt = 0L,
            server = ServerStats(
                origin = origin, efficiencyBps = efficiencyBps, comfortBps = comfortBps, durabilityPts = durability,
                maxLevel = maxLevel, status = status, chainState = chain, canWithdraw = false, upgradeCost = 625.0,
                repairCostPerPoint = 1.5625, genesisNo = 0,
            ),
        )
    }
}
