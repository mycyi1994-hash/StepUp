package com.stepup.android

import com.stepup.android.data.repo.EconomyOutcome
import com.stepup.android.domain.Faction
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.ServerStats
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
 * 스탯 포인트 나누기(서버 0059) — 레벨마다 4, 1 = 효율 +0.25%p · 착화감 +0.1%p(20% 까지) · 최대 내구도 +100(남은 내구도도 함께).
 * 나눈 포인트는 줄이지 않는다(0059 전에 강화한 신발만 한 번 처음부터). 서버 sneaker_points_set 과 같은 규칙.
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

    @Test fun `미리보기 — 효율 +0_25퍼센트p · 착화감 +0_1퍼센트p · 최대 · 남은 내구도 +100`() {
        val basis = requireNotNull(pointsBasisOf(shoe(level = 2, total = 4, free = 4, effBase = 300, comfortBase = 100, base = 1_230, now = 1_000)))
        val draft = PointsDraft(efficiency = 1, comfort = 1, durability = 2)
        assertEquals(325, basis.efficiencyBps(draft))
        assertEquals(110, basis.comfortBps(draft))
        assertEquals(1_430, basis.durabilityMax(draft))
        assertEquals(1_200, basis.durabilityNow(draft))
        assertEquals(StatPoints.PER_LEVEL, basis.total)
    }

    @Test fun `착화감은 20퍼센트까지 — 1200 + 포인트 80 은 되고 81 은 안 된다`() {
        val basis = requireNotNull(pointsBasisOf(shoe(level = 30, total = 116, free = 116, comfortBase = 1_200)))
        val at79 = PointsPhase.Editing(basis, PointsDraft(comfort = 79))
        val at80 = at79.add(PointStat.COMFORT)
        assertEquals(80, at80.draft.comfort)
        assertEquals(2_000, basis.comfortBps(at80.draft))
        assertSame(at80, at80.add(PointStat.COMFORT))
        assertTrue(basis.valid(at80.draft))
        assertFalse(basis.valid(PointsDraft(comfort = 81)))
    }

    @Test fun `다시 나누기 — 0059 전에 강화한 신발은 한 번 처음부터, 줄이면 다시 나누기를 쓴다`() {
        // Lv5 — 포인트 16 을 효율 8 · 착화감 8 로 나눠 둔 채로 받았다
        val basis = requireNotNull(pointsBasisOf(shoe(level = 5, total = 16, free = 0, efficiency = 8, comfort = 8, respec = true, base = 1_100, now = 900)))
        var phase: PointsPhase = startPoints(basis)
        repeat(8) { phase = phase.remove(PointStat.COMFORT) }
        repeat(8) { phase = phase.add(PointStat.DURABILITY) }
        assertEquals(PointsDraft(8, 0, 8), phase.draft)
        assertTrue(basis.lowers(phase.draft))
        assertTrue(basis.valid(phase.draft))
        assertEquals(1_900, basis.durabilityMax(phase.draft))
        assertEquals(1_700, basis.durabilityNow(phase.draft))
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
        effBase: Int = 400,
        comfortBase: Int = 300,
        base: Int = 1_200,
        now: Int = 1_200,
        max: Int = base + 100 * durability,
        status: String = "OWNED",
        chain: String = "APP",
    ) = Sneaker(
        id = 7, faction = Faction.WIND, rarity = Rarity.COMMON, variant = 0, level = level, mintNumber = 7,
        luck = 1.0, comfort = 1.0, durability = 100, equipped = false, acquiredAt = 0L,
        server = ServerStats(
            origin = origin, efficiencyBps = effBase + 25 * efficiency, comfortBps = comfortBase + 10 * comfort,
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
