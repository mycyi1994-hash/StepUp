package com.stepup.android

import com.stepup.android.data.local.SneakerEntity
import com.stepup.android.data.repo.EconomyOutcome
import com.stepup.android.data.repo.RequestKeys
import com.stepup.android.data.repo.repairAction
import com.stepup.android.domain.Faction
import com.stepup.android.domain.ForgePending
import com.stepup.android.domain.ForgeStats
import com.stepup.android.domain.ForgeTarget
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.ServerStats
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.screens.items.CareBlock
import com.stepup.android.ui.screens.items.DetailStat
import com.stepup.android.ui.screens.items.RepairPhase
import com.stepup.android.ui.screens.items.ShoeCareBackend
import com.stepup.android.ui.screens.items.ShoeCareStore
import com.stepup.android.ui.screens.items.balanceAfter
import com.stepup.android.ui.screens.items.careBlockOf
import com.stepup.android.ui.screens.items.forgePointsGain
import com.stepup.android.ui.screens.items.forgeRows
import com.stepup.android.ui.screens.items.formatSupExact
import com.stepup.android.ui.screens.items.holds
import com.stepup.android.ui.screens.items.pending
import com.stepup.android.ui.screens.items.repairGate
import com.stepup.android.ui.screens.items.repairQuoteOf
import com.stepup.android.ui.screens.items.repairShortfall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * 신발 상세 · 수리 · 강화(파란 톤 v4) — 수리 견적(서버 sneaker_repair 와 같은 계산) · 보내기 전 판정 · 한 번만 보내기 ·
 * 결과 미확인의 읽기 확인, 강화 화면의 세 줄(서버 forge 값). 강화 요청 · 확률은 ShoeForgeTest. 예시 수는 전달본 04-데이터와-금액기준.md 의 예시다.
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

    @Test fun `내구도 1점은 10m — 일반 Lv1 의 1km(100점) 수리는 예전처럼 1_05 SUP, 1점 값은 소수 8자리까지 쓴다`() {
        val q = requireNotNull(repairQuoteOf(absolute(now = 730, max = 1_230, perUnit = 0.0105)))
        assertEquals(0, BigDecimal("500").compareTo(q.points))
        assertEquals(0, BigDecimal("5.25").compareTo(q.cost))
        assertEquals(730 to 1_230, q.durabilityNow to q.durabilityMax)
        assertEquals(0, BigDecimal("1.05").compareTo(BigDecimal("100").multiply(q.perPoint)))
        // 레전더리 Lv7 — 1점 0.0165375. 4자리(0.0165)로 자르면 서버 차감액(14.3876)과 어긋난다
        val legendary = requireNotNull(repairQuoteOf(absolute(now = 2_000, max = 2_870, perUnit = 0.0165375)))
        assertEquals(0, BigDecimal("14.3876").compareTo(legendary.cost))
        // 가득이면 고칠 것이 없다
        assertTrue(repairGate(absolute(now = 1_230, max = 1_230, perUnit = 0.0105), 50.0, true) is RepairPhase.NotNeeded)
        assertTrue(repairGate(absolute(now = 1_220, max = 1_230, perUnit = 0.0105), 50.0, true) is RepairPhase.Confirm)
    }

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

    @Test fun `서버가 처리했다고 답한 수리는 그새 달려 100 아래로 닳았어도 처음 맞춘 값으로 끝낸다`() {
        val backend = FakeBackend(server(durability = 68.0), balance = 83.07)
        val store = ShoeCareStore(backend, CoroutineScope(Dispatchers.Unconfined))
        store.openRepair(ID)
        // 서버는 고쳤다고 답했지만 뒤이은 동기화를 못 해 폰 사본은 옛 값이다
        backend.onRepair = { EconomyOutcome.Ok }
        store.confirmRepair(ID)
        assertTrue((store.repair.value[ID] as RepairPhase.Unknown).accepted)
        // 아직 맞추지 못하면 확인 중 그대로 — 신발을 붙잡는다
        backend.refreshOutcome = EconomyOutcome.Offline
        store.recheckRepair(ID)
        assertTrue(store.busy(ID))
        // 그사이 달려 서버 값이 96 으로 닳았다 — 100 을 기다리지 않고 한 번 맞추면 끝나고 신발이 풀린다
        backend.refreshOutcome = EconomyOutcome.Ok
        backend.shoe = server(durability = 96.0)
        backend.balance = 33.07
        store.recheckRepair(ID)
        val done = store.repair.value[ID] as RepairPhase.Done
        assertEquals(96.0, done.durability, 0.0)
        assertEquals(33.07, done.balance, 1e-9)
        assertFalse(store.busy(ID))
        assertEquals(1, backend.repairs)
    }

    @Test fun `수리 요청 번호는 고치기 전 내구도로 나뉜다 — 응답을 잃은 수리 뒤 다시 닳았으면 새 번호`() {
        val keys = RequestKeys()
        val worn = shoeRow(now = 900, max = 1_000, pts = 90.0)
        // 응답을 잃은 수리 — 같은 상태에서 다시 누르면 같은 번호라 서버가 한 번만 치른다
        val lost = keys.keyFor(repairAction(worn))
        assertEquals(lost, keys.keyFor(repairAction(worn.copy())))
        // 그 수리가 됐고 그 뒤 달려 다시 닳았다 — 새 번호여야 옛 영수증("됐어요", 고치지 않음) 대신 실제로 고친다
        assertNotEquals(lost, keys.keyFor(repairAction(worn.copy(durabilityNow = 950, durabilityPts = 95.0))))
        // 최대 내구도를 보내지 않는 예전 서버(남은 · 최대가 0)도 남은 비율로 나뉜다
        val legacy = worn.copy(durabilityNow = 0, durabilityMax = 0, durabilityPts = 68.0)
        assertNotEquals(repairAction(legacy), repairAction(legacy.copy(durabilityPts = 92.0)))
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

    // ── 강화 화면의 세 줄(서버 forge 값) ────────────────────────

    @Test fun `강화 미리보기는 레벨 +1 · 스탯 포인트 +4 — 효율 · 착화감은 저절로 오르지 않고 내구도 줄은 없다`() {
        val stats = ForgeStats(level = 10, maxLevel = 20, efficiencyBps = 1_800, comfortBps = 800, pointsTotal = 36, pointsFree = 4)
        val target = ForgeTarget(ID, Rarity.LEGENDARY, stats, basePermille = 700, block = null)
        val next = target.previewNext()
        assertEquals(listOf(11, 40, 8), listOf(next.level, next.pointsTotal, next.pointsFree))
        assertEquals(1_800 to 800, next.efficiencyBps to next.comfortBps)
        val rows = forgeRows(stats, next)
        assertEquals(listOf(DetailStat.LEVEL, DetailStat.EFFICIENCY, DetailStat.COMFORT), rows.map { it.stat })
        assertEquals("10" to "11", rows[0].now to rows[0].next)
        // 그대로인 효율 · 착화감은 화살표를 그리지 않는다
        assertEquals("+18.0%" to null, rows[1].now to rows[1].next)
        assertEquals("8.0%" to null, rows[2].now to rows[2].next)
        assertNull(rows[1].nextFraction)
        assertEquals(4, forgePointsGain(stats, next))
        // 포인트를 보내지 않는 예전 서버 — 오른 레벨 × 4 로 센다. 서버가 바뀐 효율을 주면 그대로 보인다
        assertEquals(4, forgePointsGain(stats.copy(pointsTotal = 0), next.copy(pointsTotal = 0)))
        assertEquals("+18.0%" to "+18.5%", forgeRows(stats, next.copy(efficiencyBps = 1_850))[1].let { it.now to it.next })
        assertEquals(0, forgePointsGain(stats, stats))
        // 레벨 분모는 서버가 준 상한(20)
        assertEquals(10f / 20f, rows[0].nowFraction, 1e-6f)
        assertEquals(11f / 20f, rows[0].nextFraction!!, 1e-6f)
        // 실패 · 결과 미확인 · 최대 화면은 다음 값을 그리지 않는다
        assertNull(forgeRows(stats, null)[0].next)
        assertNull(forgeRows(stats, null)[0].nextFraction)
    }

    @Test fun `결과를 모르는 강화 요청은 대상과 재료 신발을 붙잡는다`() {
        val pending = ForgePending("k", targetId = ID, materialIds = listOf(11, 12, 13), ratePermille = 806, createdAt = 0)
        assertTrue(pending.holds(ID))
        assertTrue(pending.holds(12))
        assertFalse(pending.holds(99))
        assertFalse((null as ForgePending?).holds(ID))
    }

    // ── 도우미 ─────────────────────────────────────────────────

    private class FakeBackend(var shoe: Sneaker?, var balance: Double?) : ShoeCareBackend {
        var refreshOutcome: EconomyOutcome = EconomyOutcome.Ok
        var refreshes = 0
        var repairs = 0
        var onRepair: () -> EconomyOutcome = { EconomyOutcome.Ok }
        override val serverEconomy: Boolean = true
        override suspend fun refresh(): EconomyOutcome = refreshOutcome.also { refreshes++ }
        override suspend fun shoe(id: Long): Sneaker? = shoe?.takeIf { it.id == id }
        override suspend fun balance(): Double? = balance
        override suspend fun repair(id: Long): EconomyOutcome = onRepair().also { repairs++ }
    }

    private companion object {
        const val ID = 7L

        /** 최대 내구도를 보내는 서버(0059) — 남은 · 최대 내구도(1 = 10m)와 10m 수리 값 */
        fun absolute(now: Int, max: Int, perUnit: Double) = server(durability = Math.round(10_000.0 * now / max) / 100.0).let {
            it.copy(
                server = it.server!!.copy(
                    durabilityNow = now, durabilityMax = max, durabilityBase = max, repairCostPerUnit = perUnit,
                ),
            )
        }

        /** 폰 사본(sneakers 표)의 서버 신발 — 수리 요청 번호가 보는 남은 · 최대 내구도(1 = 10m)와 남은 비율 */
        fun shoeRow(now: Int, max: Int, pts: Double) = SneakerEntity(
            id = ID, factionId = "WIND", rarity = "RARE", variant = 0, level = 5, mintNumber = 7,
            luck = 1.0, comfort = 1.0, durability = pts.toInt(), equipped = false, acquiredAt = 0L,
            serverId = ID, origin = "DRAW", durabilityPts = pts, durabilityNow = now, durabilityMax = max,
        )

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
