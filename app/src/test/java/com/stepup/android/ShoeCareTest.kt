package com.stepup.android

import com.stepup.android.data.local.SneakerEntity
import com.stepup.android.data.repo.EconomyOutcome
import com.stepup.android.data.repo.QUOTE_CHANGED
import com.stepup.android.data.repo.RepairReply
import com.stepup.android.data.repo.RequestKeys
import com.stepup.android.data.repo.repairAction
import com.stepup.android.domain.Faction
import com.stepup.android.domain.ForgePending
import com.stepup.android.domain.ForgeStats
import com.stepup.android.domain.ForgeTarget
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.RepairV2Result
import com.stepup.android.domain.ServerStats
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.screens.items.CareBlock
import com.stepup.android.ui.screens.items.DetailStat
import com.stepup.android.ui.screens.items.RepairPhase
import com.stepup.android.ui.screens.items.ShoeCareBackend
import com.stepup.android.ui.screens.items.ShoeCareStore
import com.stepup.android.ui.screens.items.balanceAfter
import com.stepup.android.ui.screens.items.careBlockOf
import com.stepup.android.ui.screens.items.ForgeReward
import com.stepup.android.ui.screens.items.forgePointsGain
import com.stepup.android.ui.screens.items.forgeRewards
import com.stepup.android.ui.screens.items.forgeRows
import com.stepup.android.ui.screens.items.freeRepairHoursLeft
import com.stepup.android.ui.screens.items.partialRepairOf
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

    // ── 경제 4판 수리(0116) — 무료 · 가진 만큼 · 바뀐 수리비 ─────────────

    @Test fun `무료 수리가 남아 있으면(서버 단가 0) 잔고와 상관없이 0 SUP 로 가득`() {
        val shoe = absolute(now = 100, max = 200, perUnit = 0.0)
        val quote = requireNotNull(repairQuoteOf(shoe))
        assertTrue(quote.free)
        assertEquals(0, BigDecimal.ZERO.compareTo(quote.cost))
        assertEquals(200, quote.targetNow)
        val gate = repairGate(shoe, 0.0, true) as RepairPhase.Confirm
        assertTrue(gate.quote.free)
        // 단가가 있으면 무료가 아니다
        assertFalse(requireNotNull(repairQuoteOf(absolute(now = 100, max = 200, perUnit = 0.12))).free)
    }

    @Test fun `잔고가 모자라면 가진 만큼 수리 — 내림(잔고 나누기 단가), 비용은 잔고를 넘지 않는다`() {
        val shoe = absolute(now = 100, max = 200, perUnit = 0.12)
        val gate = repairGate(shoe, 5.0, true) as RepairPhase.Insufficient
        // 가득은 100 × 0.12 = 12 SUP, 가진 5 SUP 로는 41점(4.92 SUP)
        assertEquals(0, BigDecimal("12").compareTo(gate.quote.cost))
        val partial = requireNotNull(gate.partial)
        assertTrue(partial.partial)
        assertEquals(0, BigDecimal("41").compareTo(partial.points))
        assertEquals(0, BigDecimal("4.92").compareTo(partial.cost))
        assertEquals(141, partial.targetNow)
        assertEquals(41, partial.sendPoints)
        // 1점도 못 고치면 없다(막힌 장면 그대로) · 무료 · 예전 서버 견적에도 없다
        assertNull((repairGate(shoe, 0.05, true) as RepairPhase.Insufficient).partial)
        assertNull(partialRepairOf(requireNotNull(repairQuoteOf(absolute(now = 100, max = 200, perUnit = 0.0))), 5.0))
        assertNull(partialRepairOf(requireNotNull(repairQuoteOf(server(durability = 68.0))), 20.0))
        assertNull(requireNotNull(repairQuoteOf(server(durability = 68.0))).sendPoints)
    }

    @Test fun `가진 만큼 수리는 그 양과 보인 금액으로 보내고, 고친 만큼 동기화되면 완료`() {
        val backend = FakeBackend(absolute(now = 100, max = 200, perUnit = 0.12), balance = 5.0)
        val store = ShoeCareStore(backend, CoroutineScope(Dispatchers.Unconfined))
        store.openRepair(ID)
        assertTrue((store.repair.value[ID] as RepairPhase.Insufficient).partial != null)
        backend.onRepair = {
            backend.shoe = absolute(now = 141, max = 200, perUnit = 0.12)
            backend.balance = 0.08
            EconomyOutcome.Ok
        }
        store.confirmRepair(ID)
        assertEquals(1, backend.repairs)
        assertEquals(41, backend.lastPoints)
        assertEquals(4.92, backend.lastExpected!!, 1e-9)
        val done = store.repair.value[ID] as RepairPhase.Done
        assertTrue(done.quote.partial)
        assertEquals(0.08, done.balance, 1e-9)
    }

    @Test fun `서버 수리비가 바뀌었으면(QUOTE_CHANGED) 치르지 않고 다시 받은 값으로 다시 확인받는다`() {
        // 화면은 무료로 봤지만 그새 다른 폰에서 무료를 썼다
        val backend = FakeBackend(absolute(now = 100, max = 200, perUnit = 0.0), balance = 50.0)
        val store = ShoeCareStore(backend, CoroutineScope(Dispatchers.Unconfined))
        store.openRepair(ID)
        assertTrue((store.repair.value[ID] as RepairPhase.Confirm).quote.free)
        backend.onReply = {
            // 거절 뒤 다시 받은 신발 목록 — 단가가 생겼다
            backend.shoe = absolute(now = 100, max = 200, perUnit = 0.12)
            RepairReply(
                EconomyOutcome.Rejected(QUOTE_CHANGED),
                quoteChanged = RepairV2Result(false, QUOTE_CHANGED, null, 100, 12.0, false, null, null),
            )
        }
        store.confirmRepair(ID)
        assertEquals(0.0, backend.lastExpected!!, 1e-9)
        val changed = store.repair.value[ID] as RepairPhase.Confirm
        assertTrue(changed.changed)
        assertFalse(changed.quote.free)
        assertEquals(0, BigDecimal("12").compareTo(changed.quote.cost))
        // 다시 누르면 새 금액으로 보낸다
        backend.onReply = null
        backend.onRepair = {
            backend.shoe = absolute(now = 200, max = 200, perUnit = 0.12)
            backend.balance = 38.0
            EconomyOutcome.Ok
        }
        store.confirmRepair(ID)
        assertEquals(2, backend.repairs)
        assertEquals(12.0, backend.lastExpected!!, 1e-9)
        assertTrue(store.repair.value[ID] is RepairPhase.Done)
    }

    @Test fun `다음 무료 수리까지 — 올려서 시간, 지금 쓸 수 있거나 모르면 없음`() {
        val now = 1_000_000_000L
        assertNull(freeRepairHoursLeft(null, now))
        assertNull(freeRepairHoursLeft(now - 1, now))
        assertEquals(2, freeRepairHoursLeft(now + 90 * 60_000L, now))
        assertEquals(1, freeRepairHoursLeft(now + 1_000L, now))
        assertEquals(48, freeRepairHoursLeft(now + 48 * 3_600_000L, now))
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

    @Test fun `아직 올라가지 않은 러닝이 있으면 알리고 올리며, 동기화 뒤 서버 값으로 견적을 다시 본다 — 수리는 막지 않는다`() {
        val backend = FakeBackend(server(durability = 92.0), balance = 83.07)
        backend.unsent = true
        val store = ShoeCareStore(backend, CoroutineScope(Dispatchers.Unconfined))
        store.openRepair(ID)
        assertTrue(store.unsentRuns.value)
        assertEquals(1, backend.sends)
        // 금액은 폰이 미리 빼서 꾸미지 않는다 — 지금 사본 그대로(92 → 12.5 SUP), 수리도 할 수 있다
        val first = store.repair.value[ID] as RepairPhase.Confirm
        assertEquals(0, BigDecimal("12.5").compareTo(first.quote.cost))
        assertFalse(first.changed)
        // 러닝이 올라가 서버가 닳게 하고 동기화가 사본을 맞췄다 — 바뀐 금액을 다시 확인받는다(RP10)
        backend.shoe = server(durability = 68.0)
        backend.unsent = false
        backend.syncs.value = 1L
        val requoted = store.repair.value[ID] as RepairPhase.Confirm
        assertTrue(requoted.changed)
        assertEquals(0, BigDecimal("50").compareTo(requoted.quote.cost))
        assertFalse(store.unsentRuns.value)
        // 그 뒤 동기화는 더 보지 않는다
        backend.shoe = server(durability = 50.0)
        backend.syncs.value = 2L
        assertEquals(0, BigDecimal("50").compareTo((store.repair.value[ID] as RepairPhase.Confirm).quote.cost))
    }

    @Test fun `올라가지 않은 러닝이 없으면 안내도 올리기도 없다`() {
        val backend = FakeBackend(server(durability = 68.0), balance = 83.07)
        val store = ShoeCareStore(backend, CoroutineScope(Dispatchers.Unconfined))
        store.openRepair(ID)
        assertFalse(store.unsentRuns.value)
        assertEquals(0, backend.sends)
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

    @Test fun `강화 미리보기는 레벨 +1 · 등급의 포인트 — 효율 · 착화감은 저절로 오르지 않고 내구도 줄은 없다`() {
        val stats = ForgeStats(level = 10, maxLevel = 27, efficiencyBps = 1_800, comfortBps = 800, pointsTotal = 36, pointsFree = 4)
        val target = ForgeTarget(ID, Rarity.LEGENDARY, stats, basePermille = 700, block = null)
        val next = target.previewNext()
        // 레전더리 Lv10 → 11: 레벨마다 5 (서버 0116)
        assertEquals(listOf(11, 41, 9), listOf(next.level, next.pointsTotal, next.pointsFree))
        assertEquals(1_800 to 800, next.efficiencyBps to next.comfortBps)
        val rows = forgeRows(stats, next, Rarity.LEGENDARY)
        assertEquals(listOf(DetailStat.LEVEL, DetailStat.EFFICIENCY, DetailStat.COMFORT), rows.map { it.stat })
        assertEquals("10" to "11", rows[0].now to rows[0].next)
        // 효율 · 착화감은 지수(경제 4판) — 그대로인 값은 화살표를 그리지 않는다
        assertEquals("118" to null, rows[1].now to rows[1].next)
        assertEquals("109" to null, rows[2].now to rows[2].next)
        assertNull(rows[1].nextFraction)
        // 막대 끝은 레전더리 꼭대기(효율 607.5)
        assertEquals((118.0 / 607.5).toFloat(), rows[1].nowFraction, 1e-4f)
        assertEquals(5, forgePointsGain(stats, next, Rarity.LEGENDARY))
        // 포인트를 보내지 않는 예전 서버 — 같은 표(등급 · 레벨)로 센다. 피니시 Lv30 은 8 + 5레벨 보너스 12
        assertEquals(5, forgePointsGain(stats.copy(pointsTotal = 0), next.copy(pointsTotal = 0), Rarity.LEGENDARY))
        assertEquals(20, forgePointsGain(stats.copy(level = 29, pointsTotal = 0), next.copy(level = 30, pointsTotal = 0), Rarity.FINISH))
        // 서버가 바뀐 효율을 주면 그대로 보인다
        assertEquals("118" to "119", forgeRows(stats, next.copy(efficiencyBps = 1_850))[1].let { it.now to it.next })
        assertEquals(0, forgePointsGain(stats, stats, Rarity.LEGENDARY))
        // 레벨 분모는 서버가 준 상한(27)
        assertEquals(10f / 27f, rows[0].nowFraction, 1e-6f)
        assertEquals(11f / 27f, rows[0].nextFraction!!, 1e-6f)
        // 실패 · 결과 미확인 · 최대 화면은 다음 값을 그리지 않는다
        assertNull(forgeRows(stats, null)[0].next)
        assertNull(forgeRows(stats, null)[0].nextFraction)
    }

    @Test fun `한 레벨이 주는 것 — 등급의 포인트 · 하루 에너지 +2 · 5레벨마다 내구도 가득과 상급 상자`() {
        // 일반 Lv5: 레벨 2 + 보너스 3
        assertEquals(
            listOf(ForgeReward.Points(5), ForgeReward.Energy(2), ForgeReward.Milestone(5, boxAlreadyGot = false)),
            forgeRewards(Rarity.COMMON, 5),
        )
        // 피니시 Lv11 — 레벨마다 8, 5의 배수가 아니면 상자 없음
        assertEquals(listOf(ForgeReward.Points(8), ForgeReward.Energy(2)), forgeRewards(Rarity.FINISH, 11))
        // 이 계정이 Lv10 상자를 이미 받았으면 그렇게 말한다
        assertEquals(ForgeReward.Milestone(10, boxAlreadyGot = true), forgeRewards(Rarity.RARE, 10, levelsGot = listOf(5, 10)).last())
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
        var lastPoints: Int? = null
        var lastExpected: Double? = null
        var onRepair: () -> EconomyOutcome = { EconomyOutcome.Ok }
        /** 있으면 대답 전체(QUOTE_CHANGED 등) */
        var onReply: (() -> RepairReply)? = null
        var unsent = false
        var sends = 0
        val syncs = kotlinx.coroutines.flow.MutableStateFlow(0L)
        override suspend fun unsentRuns(): Boolean = unsent
        override fun sendRuns() { sends++ }
        override val synced: kotlinx.coroutines.flow.Flow<Long> get() = syncs
        override val serverEconomy: Boolean = true
        override suspend fun refresh(): EconomyOutcome = refreshOutcome.also { refreshes++ }
        override suspend fun shoe(id: Long): Sneaker? = shoe?.takeIf { it.id == id }
        override suspend fun balance(): Double? = balance
        override suspend fun repair(id: Long, points: Int?, expected: Double?): RepairReply {
            repairs++
            lastPoints = points
            lastExpected = expected
            return onReply?.invoke() ?: RepairReply(onRepair())
        }
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
