package com.stepup.android

import com.stepup.android.data.repo.EconomyOutcome
import com.stepup.android.domain.Faction
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.ServerStats
import com.stepup.android.domain.ShoeRules
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.StatPoints
import com.stepup.android.ui.screens.items.PointStat
import com.stepup.android.ui.screens.items.PointsDraft
import com.stepup.android.ui.screens.items.PointsEntry
import com.stepup.android.ui.screens.items.PointsError
import com.stepup.android.ui.screens.items.PointsPhase
import com.stepup.android.ui.screens.items.add
import com.stepup.android.ui.screens.items.backToEdit
import com.stepup.android.ui.screens.items.pointsAfterSend
import com.stepup.android.ui.screens.items.pointsAvailable
import com.stepup.android.ui.screens.items.pointsBasisOf
import com.stepup.android.ui.screens.items.pointsEntryOf
import com.stepup.android.ui.screens.items.remove
import com.stepup.android.ui.screens.items.review
import com.stepup.android.ui.screens.items.send
import com.stepup.android.ui.screens.items.startPoints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 스탯 포인트 나누기(경제 4판, 서버 0116) — 1 = 뽑은 값의 +1.0 ~ +1.8%(등급마다), 효율 · 착화감(에너지 70% 절약까지) · 최대 내구도
 * (남은 내구도도 함께). 나눈 포인트는 줄이지 않는다(respec 인 신발만 한 번 처음부터). 서버 sneaker_points_set 과 같은 규칙.
 */
class ShoePointsModelTest {

    @Test fun `포인트를 나눌 수 있는 신발 — 서버 신발 · 2레벨 이상 · 최대 내구도를 아는 서버`() {
        assertTrue(pointsBasisOf(shoe(level = 3, total = 8, free = 8)) != null)
        // 1레벨(받은 포인트 0) · 폰에서 올린 예전 신발 · 폰에만 있던 신발 · 예전 서버(최대 내구도 모름)
        assertNull(pointsBasisOf(shoe(level = 1, total = 0, free = 0)))
        assertNull(pointsBasisOf(shoe(level = 5, total = 16, free = 16, origin = "IMPORT")))
        assertNull(pointsBasisOf(shoe(level = 5, total = 16, free = 16).copy(server = null)))
        assertNull(pointsBasisOf(shoe(level = 5, total = 16, free = 16, max = 0)))
    }

    @Test fun `더하고 빼기 — 남은 포인트 안에서, 나눈 포인트 아래로는 못 뺀다`() {
        val basis = requireNotNull(pointsBasisOf(shoe(level = 3, total = 8, free = 4, efficiency = 2, durability = 2)))
        var phase: PointsPhase = startPoints(basis)
        assertEquals(4, basis.free(phase.draft))
        // 이미 나눈 효율 2 는 줄일 수 없다
        assertSame(phase, phase.remove(PointStat.EFFICIENCY))
        phase = phase.add(PointStat.EFFICIENCY).add(PointStat.COMFORT).add(PointStat.DURABILITY).add(PointStat.DURABILITY)
        assertEquals(PointsDraft(3, 1, 4), phase.draft)
        assertEquals(0, basis.free(phase.draft))
        // 남은 포인트가 없으면 더하지 않는다
        assertSame(phase, phase.add(PointStat.EFFICIENCY))
        // 이번에 더한 것은 뺄 수 있다(보내기 전)
        phase = phase.remove(PointStat.DURABILITY)
        assertEquals(PointsDraft(3, 1, 3), phase.draft)
        assertFalse(basis.lowers(phase.draft))
    }

    @Test fun `미리보기 — 포인트 1 = 뽑은 값의 +1퍼센트(일반), 효율 · 착화감은 지수, 최대 · 남은 내구도도 함께`() {
        // 가운데 일반 — 효율 140(+4,000bps) · 착화감 140(절약 2,857bps) · 내구도 155(31분)
        val basis = requireNotNull(pointsBasisOf(shoe(level = 2, total = 2, free = 2, effBase = 4_000, comfortBase = 2_857, base = 155, now = 120)))
        assertEquals(1.0, basis.pointRatePercent, 0.0)
        val draft = PointsDraft(efficiency = 1, comfort = 1)
        // 효율 140 × 1.01 = 141.4(+4,140bps) — 서버 economy.shoe_effective
        assertEquals(4_140, basis.efficiencyBps(draft))
        assertEquals(141.4, basis.efficiencyIndex(draft), 1e-9)
        // 착화감 지수도 × 1.01 — 절약 bps = 10000 − 7143 ÷ 1.01(반올림)
        assertEquals(10_000 - Math.round(7_143 / 1.01).toInt(), basis.comfortBps(draft))
        assertEquals(140.0 * 1.01, basis.comfortIndex(draft), 0.05)
        // 내구도 포인트 2 = 155 × 2% = 3.1 → 3(서버 economy.dur_max_of), 남은 내구도도 같이
        val dur = PointsDraft(durability = 2)
        assertEquals(158, basis.durabilityMax(dur))
        assertEquals(123, basis.durabilityNow(dur))
        assertEquals(2, basis.total)
    }

    @Test fun `서버 0119 — 내구도 포인트를 바꿔도 남은 내구도는 같은 비율이다(뺐다 넣어 공짜로 채우지 못한다)`() {
        // 서버가 능력치 규칙(stats 묶음)을 준 뒤 — sneaker_points_set 이 비율을 지킨다
        val rules = ShoeRules.FALLBACK.copy(stats = com.stepup.android.domain.StatRule.FALLBACK.copy(fromServer = true))
        // 일반 · 기본 200 · 내구도 포인트 50(최대 300) · 절반 남음
        val half = requireNotNull(pointsBasisOf(shoe(level = 20, total = 50, free = 0, durability = 50, respec = true, base = 200, now = 150), rules))
        assertEquals(300, half.durabilityMax(half.saved))
        assertEquals(100, half.durabilityNow(PointsDraft(efficiency = 50)))          // 최대 200 · 절반 그대로
        // 다 닳은 신발은 포인트를 넣어도 0, 가득 찬 신발은 가득
        val empty = requireNotNull(pointsBasisOf(shoe(level = 20, total = 50, free = 50, base = 200, now = 0), rules))
        assertEquals(0, empty.durabilityNow(PointsDraft(durability = 50)))
        val full = requireNotNull(pointsBasisOf(shoe(level = 20, total = 50, free = 50, base = 200, now = 200), rules))
        assertEquals(300, full.durabilityNow(PointsDraft(durability = 50)))
        // 0119 전 서버(규칙을 못 받았을 때 포함)는 늘어난 만큼 더한다
        val old = requireNotNull(pointsBasisOf(shoe(level = 20, total = 50, free = 50, base = 200, now = 0)))
        assertEquals(100, old.durabilityNow(PointsDraft(durability = 50)))
    }

    @Test fun `등급이 높을수록 포인트 1 의 폭이 크다 — 피니시는 뽑은 값의 +1_8퍼센트`() {
        val rules = ShoeRules.FALLBACK
        val finish = requireNotNull(pointsBasisOf(shoe(level = 2, total = 8, free = 8, effBase = 96_000, rarity = Rarity.FINISH), rules))
        assertEquals(1.8, finish.pointRatePercent, 1e-9)
        // 지수 1,060 × 1.018 = 1,079.08
        assertEquals(1_079.08, finish.efficiencyIndex(PointsDraft(efficiency = 1)), 1e-6)
        // 레벨마다 받는 포인트도 등급마다(서버 0116)
        assertEquals(2, StatPoints.gainAt(Rarity.COMMON, 2))
        assertEquals(5, StatPoints.gainAt(Rarity.COMMON, 5))
        assertEquals(8, StatPoints.gainAt(Rarity.FINISH, 2))
        assertEquals(2 * 19 + 3 * 4, StatPoints.totalAt(Rarity.COMMON, 20))
    }

    @Test fun `착화감은 에너지 70퍼센트 절약까지 — 상한을 넘는 포인트는 더하지 않는다`() {
        // 일반 착화감 꼭대기(절약 4,444bps, 지수 180) — 지수 333 까지 약 85포인트
        val basis = requireNotNull(pointsBasisOf(shoe(level = 30, total = 200, free = 200, comfortBase = 4_444)))
        assertEquals(7_000, basis.comfortCapBps)
        var phase: PointsPhase = PointsPhase.Editing(basis, PointsDraft())
        repeat(200) { phase = phase.add(PointStat.COMFORT) }
        val atCap = phase.draft.comfort
        assertTrue(basis.comfortBps(phase.draft) <= 7_000)
        assertTrue(basis.valid(phase.draft))
        // 하나 더하면 상한을 넘는다 — 더해지지 않고, 서버가 받지 않을 초안이다
        assertSame(phase, phase.add(PointStat.COMFORT))
        assertFalse(basis.valid(PointsDraft(comfort = atCap + 1)))
        assertTrue(atCap in 80..90)
    }

    @Test fun `다시 나누기 — 0059 전에 강화한 신발은 한 번 처음부터, 줄이면 다시 나누기를 쓴다`() {
        // Lv5 — 포인트 16 을 효율 8 · 착화감 8 로 나눠 둔 채로 받았다
        // 기본 450 — 포인트 8 = 450 × 8% = 36(반올림, 일반 폭 1%)
        val basis = requireNotNull(pointsBasisOf(shoe(level = 5, total = 16, free = 0, efficiency = 8, comfort = 8, respec = true, base = 450, now = 400)))
        var phase: PointsPhase = startPoints(basis)
        repeat(8) { phase = phase.remove(PointStat.COMFORT) }
        repeat(8) { phase = phase.add(PointStat.DURABILITY) }
        assertEquals(PointsDraft(8, 0, 8), phase.draft)
        assertTrue(basis.lowers(phase.draft))
        assertTrue(basis.valid(phase.draft))
        assertEquals(486, basis.durabilityMax(phase.draft))
        assertEquals(436, basis.durabilityNow(phase.draft))
        assertEquals(PointsEntry.Respec, pointsEntryOf(shoe(level = 5, total = 16, free = 0, efficiency = 8, comfort = 8, respec = true)))
    }

    @Test fun `나누기는 확인을 거쳐 보낸다 — 바뀐 것이 없으면 확인으로 가지 않는다`() {
        val basis = requireNotNull(pointsBasisOf(shoe(level = 2, total = 4, free = 4)))
        val start = startPoints(basis)
        assertSame(start, start.review())
        assertNull(start.send())
        val edited = start.add(PointStat.EFFICIENCY)
        val confirm = edited.review()
        assertTrue(confirm is PointsPhase.Confirm)
        assertEquals(edited.draft, confirm.backToEdit().draft)
        val sending = requireNotNull(confirm.send())
        assertEquals(PointsDraft(efficiency = 1), sending.draft)
    }

    @Test fun `서버의 대답 — 받았으면 완료, 끊기면 같은 초안으로 다시 보내기, 거절이면 최신 값에서 다시`() {
        val basis = requireNotNull(pointsBasisOf(shoe(level = 2, total = 4, free = 4)))
        val sending = PointsPhase.Sending(basis, PointsDraft(durability = 4))
        assertTrue(pointsAfterSend(sending, EconomyOutcome.Ok, null) is PointsPhase.Done)

        val offline = pointsAfterSend(sending, EconomyOutcome.Offline, null) as PointsPhase.Editing
        assertEquals(PointsError.OFFLINE, offline.error)
        // 같은 초안을 그대로 다시 보낸다(같은 요청 번호 · 합계라 두 번 나뉘지 않는다)
        assertEquals(sending.draft, offline.send()?.draft)

        val latest = requireNotNull(pointsBasisOf(shoe(level = 2, total = 4, free = 0, efficiency = 4)))
        val rejected = pointsAfterSend(sending, EconomyOutcome.Rejected("나눈 포인트는 되돌릴 수 없습니다"), latest) as PointsPhase.Editing
        assertEquals(PointsError.REJECTED, rejected.error)
        assertEquals(latest.saved, rejected.draft)
        assertNull(rejected.send())

        val signIn = pointsAfterSend(sending, EconomyOutcome.SignInRequired, null) as PointsPhase.Editing
        assertEquals(PointsError.SIGN_IN, signIn.error)
    }

    @Test fun `상세의 길 — 남은 포인트 · 다 나눔 · 포인트 없음, 판매 중 · 체인에서는 나누지 않는다`() {
        assertEquals(PointsEntry.Free(4), pointsEntryOf(shoe(level = 2, total = 4, free = 4)))
        assertEquals(PointsEntry.Done, pointsEntryOf(shoe(level = 2, total = 4, free = 0, efficiency = 4)))
        assertEquals(PointsEntry.None, pointsEntryOf(shoe(level = 1, total = 0, free = 0)))
        assertTrue(pointsAvailable(shoe(level = 2, total = 4, free = 4)))
        assertFalse(pointsAvailable(shoe(level = 2, total = 4, free = 4, status = "LISTED")))
        assertFalse(pointsAvailable(shoe(level = 2, total = 4, free = 4, chain = "ON_CHAIN")))
    }

    private fun shoe(
        level: Int,
        total: Int,
        free: Int,
        efficiency: Int = 0,
        comfort: Int = 0,
        durability: Int = 0,
        respec: Boolean = false,
        origin: String = "PAID_DRAW",
        effBase: Int = 4_000,
        comfortBase: Int = 2_857,
        base: Int = 155,
        now: Int = 155,
        rarity: Rarity = Rarity.COMMON,
        max: Int = ShoeRules.FALLBACK.durabilityWithPoints(base, rarity.id, durability),
        status: String = "OWNED",
        chain: String = "APP",
    ) = Sneaker(
        id = 7, faction = Faction.WIND, rarity = rarity, variant = 0, level = level, mintNumber = 7,
        luck = 1.0, comfort = 1.0, durability = 100, equipped = false, acquiredAt = 0L,
        server = ServerStats(
            origin = origin,
            efficiencyBps = ShoeRules.FALLBACK.efficiencyWithPoints(effBase, rarity.id, efficiency),
            comfortBps = ShoeRules.FALLBACK.comfortWithPoints(comfortBase, rarity.id, comfort),
            durabilityPts = if (max > 0) Math.round(10_000.0 * now / max) / 100.0 else 100.0,
            maxLevel = 20, status = status, chainState = chain, canWithdraw = false, upgradeCost = 0.0,
            repairCostPerPoint = 0.0, genesisNo = 0,
            durabilityNow = if (max > 0) now else 0, durabilityMax = max, durabilityBase = if (max > 0) base else 0,
            efficiencyBaseBps = effBase, comfortBaseBps = comfortBase,
            points = StatPoints(total, free, efficiency, comfort, durability, respec),
            repairCostPerUnit = 0.0105,
        ),
    )
}
