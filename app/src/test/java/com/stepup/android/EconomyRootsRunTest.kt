package com.stepup.android

import com.stepup.android.domain.Faction
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.RewardEconomy
import com.stepup.android.domain.RunVerdictReason
import com.stepup.android.domain.RunVerdictReason.Kind
import com.stepup.android.domain.ServerStats
import com.stepup.android.domain.ShoeRules
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.StatPoints
import com.stepup.android.domain.StatRule
import com.stepup.android.domain.energyMultOf
import com.stepup.android.domain.lifeSecOf
import com.stepup.android.domain.startLifeSec
import com.stepup.android.domain.wearMultOf
import com.stepup.android.domain.wearOf
import com.stepup.android.domain.wornPercent
import com.stepup.android.ui.screens.items.durabilityEarnMinutes
import com.stepup.android.ui.screens.items.durabilityPercentDuringRun
import com.stepup.android.ui.screens.items.durabilityWear
import com.stepup.android.ui.screens.items.lowDurabilityMinutes
import com.stepup.android.ui.screens.items.repairUrgent
import com.stepup.android.ui.screens.walk.PendingShoeWear
import com.stepup.android.ui.screens.walk.ShoeCheckLine
import com.stepup.android.ui.screens.walk.energyUsedUp
import com.stepup.android.ui.screens.walk.shoeCheckLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 경제의 뿌리 고치기(서버 0119, docs/economy-roots-contract-2026-11-21.md) — 러닝 전 · 중 · 뒤의 셈.
 * 효율 포인트만큼 빨리 닳는 신발 수명 · 달리는 중 닳는 양 · 닳은 구간의 예상 적립(30%) · 0119 전 규칙이면 예전 숫자 그대로 · 새 판정 까닭.
 */
class EconomyRootsRunTest {

    /** 서버 0119 의 기본 규칙(닳은 뒤 30% · 효율 포인트 배수가 닳음 · 에너지에 그대로) */
    private val roots = ShoeRules.FALLBACK

    /** 0119 전 서버 — `stats` 묶음이 없을 때 */
    private val legacy = ShoeRules.FALLBACK.copy(stats = StatRule.LEGACY)

    /** 일반 신발(포인트 1 = 1%, [ShoeRules.FALLBACK] 의 COMMON point_rate_bps 100) */
    private fun shoe(
        now: Int,
        max: Int,
        effPoints: Int = 0,
        origin: String = "DRAW",
        efficiencyBps: Int = 0,
        id: Long = 1,
        equipped: Boolean = true,
    ) = Sneaker(
        id = id, faction = Faction.WIND, rarity = Rarity.COMMON, variant = 0, level = 1, mintNumber = 1,
        luck = 1.0, comfort = 1.0, durability = 100, equipped = equipped, acquiredAt = 0L, modelId = 1107,
        server = ServerStats(
            origin = origin, efficiencyBps = efficiencyBps, comfortBps = 0,
            durabilityPts = if (max > 0) now * 100.0 / max else 100.0,
            maxLevel = 20, status = "OWNED", chainState = "APP", canWithdraw = false, upgradeCost = 0.0,
            repairCostPerPoint = 0.0, genesisNo = 0, durabilityNow = now, durabilityMax = max, durabilityBase = max,
            points = StatPoints(total = effPoints, efficiency = effPoints),
        ),
    )

    // ── 수명 ──

    @Test
    fun `신발 수명은 효율 포인트 배수만큼 짧다 - 내구도 150 · 포인트당 1퍼센트 · 50포인트면 1,200초`() {
        val tuned = shoe(now = 150, max = 150, effPoints = 50)
        // u = 1 + 100 × 50 ÷ 10000 = 1.5 → 150 × 12초 ÷ 1.5
        assertEquals(1.5, roots.wearMultOf(tuned), 1e-12)
        assertEquals(1_200L, roots.lifeSecOf(tuned, 150))
        assertEquals(1_200L, roots.startLifeSec(tuned))
        assertEquals(20, durabilityEarnMinutes(tuned, roots))
        // 포인트가 없는 신발은 예전처럼 내구도 × 12초
        val plain = shoe(now = 150, max = 150)
        assertEquals(1_800L, roots.startLifeSec(plain))
        assertEquals(30, durabilityEarnMinutes(plain, roots))
        // 폰에서 올린 예전 신발(IMPORT · MINT)은 포인트를 치지 않는다(서버 eff_point_mult 와 같다)
        assertEquals(1_800L, roots.startLifeSec(shoe(now = 150, max = 150, effPoints = 50, origin = "IMPORT")))
    }

    @Test
    fun `수명을 모르는 신발 - 폰에만 있던 신발은 모르고 다 닳은 예전 서버 신발은 0`() {
        val phoneOnly = Sneaker(
            id = 2, faction = Faction.WIND, rarity = Rarity.COMMON, variant = 0, level = 1, mintNumber = 1,
            luck = 1.0, comfort = 1.0, durability = 100, equipped = true, acquiredAt = 0L,
        )
        assertNull(roots.startLifeSec(phoneOnly))
        // 최대 내구도를 보내지 않는 예전 서버 — 닳지 않았으면 모르고, 다 닳았으면 0
        assertNull(roots.startLifeSec(shoe(now = 0, max = 0)))
        val wornOld = shoe(now = 0, max = 0).let { it.copy(server = it.server!!.copy(durabilityPts = 0.0)) }
        assertEquals(0L, roots.startLifeSec(wornOld))
        assertEquals(0L, roots.startLifeSec(shoe(now = 0, max = 150)))
    }

    @Test
    fun `출발 전 점검의 분도 효율 포인트 배수만큼 짧다`() {
        // 155 · 50포인트 — 155 × 12초 ÷ 1.5 = 1,240초 → 20분(가득이라 경고 없이)
        assertEquals(ShoeCheckLine.Earns(20), shoeCheckLine(shoe(now = 155, max = 155, effPoints = 50), roots))
        assertEquals(ShoeCheckLine.Earns(31), shoeCheckLine(shoe(now = 155, max = 155), roots))
        // 40 남음 · 50포인트 — 5분(가득 20분의 절반 10분보다 짧다)
        assertEquals(5, lowDurabilityMinutes(shoe(now = 40, max = 155, effPoints = 50), roots))
        assertEquals(ShoeCheckLine.Low(5), shoeCheckLine(shoe(now = 40, max = 155, effPoints = 50), roots))
        assertTrue(repairUrgent(shoe(now = 40, max = 155, effPoints = 50), roots))
        // 다 닳은 신발은 그대로 "다 닳음" 줄 — 출발은 막지 않고 글만 다르다(N%만 적립)
        assertEquals(ShoeCheckLine.WornOut, shoeCheckLine(shoe(now = 0, max = 155, effPoints = 50), roots))
    }

    // ── 달리는 중 닳음 ──

    @Test
    fun `달리는 중 닳는 양은 1분마다 5 곱하기 효율 포인트 배수`() {
        val tuned = shoe(now = 150, max = 150, effPoints = 50)
        // 20분 — 5 × 20 × 1.5
        assertEquals(150, durabilityWear(tuned, 1_200, roots))
        assertEquals(150, roots.wearOf(tuned, 1_200))
        // 다 채운 분만 — 59초는 0
        assertEquals(0, durabilityWear(tuned, 59, roots))
        // 10분 달린 화면의 남은 비율 — 75 닳아 50%
        assertEquals(50, durabilityPercentDuringRun(tuned, 600, roots))
        // 수명(20분)을 넘기면 0 에서 멈춘다(더 닳지 않는다)
        assertEquals(0, durabilityPercentDuringRun(tuned, 3_600, roots))
        // 포인트가 없는 신발은 예전과 같다
        val plain = shoe(now = 150, max = 150)
        assertEquals(durabilityWear(1_200), durabilityWear(plain, 1_200, roots))
        assertEquals(66, durabilityPercentDuringRun(plain, 600, roots))
    }

    @Test
    fun `아직 올라가지 않은 러닝의 닳음도 그 신발의 배수로`() {
        val pending = PendingShoeWear(byShoe = mapOf(7L to 30), unknown = 15)
        val tuned = shoe(now = 150, max = 150, effPoints = 50, id = 7, equipped = false)
        val wearing = shoe(now = 150, max = 150, id = 8, equipped = true)
        assertEquals(45, pending.of(tuned, roots))
        assertEquals(15, pending.of(wearing, roots))
        assertEquals(30, pending.of(tuned, legacy))
        // 뺄 것이 없는 신발은 0
        assertEquals(0, pending.of(shoe(now = 150, max = 150, effPoints = 50, id = 9, equipped = false), roots))
    }

    // ── 닳은 구간의 예상 적립 ──

    @Test
    fun `닳은 구간의 걸음은 세고 30퍼센트만 적립한다`() {
        // 30분짜리 신발(150)로 60분 · 2,000걸음 — 앞의 1,000걸음은 다, 뒤의 1,000걸음은 30%
        val reward = RewardEconomy.shoeSessionReward(
            steps = 2_000, activeSec = 3_600, energyRemaining = 100.0, shoe = shoe(now = 150, max = 150), rules = roots,
        )
        assertEquals(2_000, reward.rewardedSteps)
        assertEquals((1_000 + 1_000 * 0.30) * 0.0118, reward.points, 1e-9)
        // 닳은 구간의 걸음도 에너지는 그대로 쓴다
        assertEquals(2_000 / 600.0, reward.energyUsed, 1e-9)
        // 수명 안에 끝난 러닝은 모두 가득 구간
        val short = RewardEconomy.shoeSessionReward(
            steps = 2_000, activeSec = 1_500, energyRemaining = 100.0, shoe = shoe(now = 150, max = 150), rules = roots,
        )
        assertEquals(2_000 * 0.0118, short.points, 1e-9)
    }

    @Test
    fun `다 닳은 신발로 출발해도 모든 걸음이 30퍼센트로 적립된다`() {
        // 효율 +50%(5,000bps) 신발이 0% — 배율은 효율 그대로, 구간이 처음부터 닳은 구간
        val worn = shoe(now = 0, max = 150, efficiencyBps = 5_000)
        val reward = RewardEconomy.shoeSessionReward(
            steps = 2_000, activeSec = 1_200, energyRemaining = 100.0, shoe = worn, rules = roots,
        )
        assertEquals(2_000, reward.rewardedSteps)
        assertEquals(2_000 * 0.30 * 0.0118 * 1.5, reward.points, 1e-9)
    }

    @Test
    fun `효율 포인트를 넣은 신발은 수명도 에너지도 그 배수만큼 빨리 쓴다`() {
        val tuned = shoe(now = 150, max = 150, effPoints = 50)
        assertEquals(1.5, roots.energyMultOf(tuned), 1e-12)
        // 에너지 1 = 600걸음 ÷ 1.5 = 400걸음
        assertEquals(400, RewardEconomy.earnableSteps(1.0, 1.0, roots.energyMultOf(tuned)))
        assertTrue(energyUsedUp(0.0, 1.0, 1.5))
        assertFalse(energyUsedUp(1.0, 1.0, 1.5))
        // 40분(수명 20분) · 2,000걸음 · 에너지 2 → 800걸음까지만 적립하고, 그 가운데 수명 안 1,000걸음이 먼저라 모두 가득 구간
        val reward = RewardEconomy.shoeSessionReward(
            steps = 2_000, activeSec = 2_400, energyRemaining = 2.0, shoe = tuned, rules = roots,
        )
        assertEquals(800, reward.rewardedSteps)
        assertEquals(800 * 0.0118, reward.points, 1e-9)
        assertEquals(2.0, reward.energyUsed, 1e-9)
        // 에너지가 넉넉하면 1,000걸음은 다, 1,000걸음은 30%
        val rich = RewardEconomy.shoeSessionReward(
            steps = 2_000, activeSec = 2_400, energyRemaining = 100.0, shoe = tuned, rules = roots,
        )
        assertEquals((1_000 + 1_000 * 0.30) * 0.0118, rich.points, 1e-9)
    }

    @Test
    fun `에너지로 자른 뒤에 가득 구간과 닳은 구간으로 가른다`() {
        // 2,000걸음 가운데 가득 구간 1,000 · 에너지 2(1,200걸음) → 가득 1,000 + 닳은 200
        val reward = RewardEconomy.sessionReward(
            walkedSteps = 2_000, energyRemaining = 2.0, earningMultiplier = 1.0, fullSteps = 1_000, wornRate = 0.30,
        )
        assertEquals(1_200, reward.rewardedSteps)
        assertEquals((1_000 + 200 * 0.30) * RewardEconomy.POINTS_PER_STEP, reward.points, 1e-9)
        assertEquals(2.0, reward.energyUsed, 1e-9)
    }

    @Test
    fun `신발이 없으면 레벨 배율로 - 수명으로 가르지 않는다`() {
        val reward = RewardEconomy.shoeSessionReward(
            steps = 1_000, activeSec = 3_600, energyRemaining = 100.0, shoe = null, rules = roots, fallbackMultiplier = 1.15,
        )
        assertEquals(1_000, reward.rewardedSteps)
        assertEquals(1_000 * 0.0118 * 1.15, reward.points, 1e-9)
    }

    // ── 0119 전 규칙이면 예전 숫자 그대로 ──

    @Test
    fun `0119 전 규칙이면 수명 · 닳음 · 예상 적립이 예전 숫자와 같다`() {
        assertFalse(legacy.stats.earnsWhenWorn)
        assertEquals(0, legacy.stats.wornPercent)
        val tuned = shoe(now = 150, max = 150, effPoints = 50)
        // 효율 포인트가 수명 · 닳음 · 에너지를 건드리지 않는다
        assertEquals(1.0, legacy.wearMultOf(tuned), 1e-12)
        assertEquals(1.0, legacy.energyMultOf(tuned), 1e-12)
        assertEquals(1_800L, legacy.startLifeSec(tuned))
        assertEquals(30, durabilityEarnMinutes(tuned, legacy))
        assertEquals(durabilityWear(1_200), durabilityWear(tuned, 1_200, legacy))
        assertEquals(66, durabilityPercentDuringRun(tuned, 600, legacy))
        assertEquals(ShoeCheckLine.Earns(31), shoeCheckLine(shoe(now = 155, max = 155, effPoints = 50), legacy))
        // 닳은 뒤의 걸음은 적립하지 않는다(0066) — 30분짜리 신발로 60분이면 절반만
        val reward = RewardEconomy.shoeSessionReward(
            steps = 2_000, activeSec = 3_600, energyRemaining = 100.0, shoe = tuned, rules = legacy,
        )
        assertEquals(RewardEconomy.stepsWithinDurability(2_000, 150, 3_600), reward.rewardedSteps)
        assertEquals(1_000 * 0.0118, reward.points, 1e-9)
        assertEquals(1_000 / 600.0, reward.energyUsed, 1e-9)
        // 다 닳은 신발은 적립이 없다
        val worn = RewardEconomy.shoeSessionReward(
            steps = 2_000, activeSec = 1_200, energyRemaining = 100.0, shoe = shoe(now = 0, max = 150, efficiencyBps = 5_000), rules = legacy,
        )
        assertEquals(0, worn.rewardedSteps)
        assertEquals(0.0, worn.points, 0.0)
    }

    @Test
    fun `닳은 뒤 적립 비율은 규칙에서 - 30퍼센트`() {
        assertTrue(roots.stats.earnsWhenWorn)
        assertEquals(30, roots.stats.wornPercent)
        assertEquals(25, roots.stats.copy(wornRate = 0.25).wornPercent)
    }

    // ── 판정 까닭 ──

    @Test
    fun `새 판정 까닭 - 신발이 다 닳은 뒤의 걸음은 일부만 적립`() {
        val note = RunVerdictReason.note("SIGNED", 3.5, "신발이 다 닳은 뒤의 걸음은 일부만 적립했습니다", "")!!
        assertFalse(note.zero)
        assertEquals(listOf(Kind.WORN_PARTIAL), note.kinds)
        // 다른 까닭과 함께 와도 읽는다
        assertEquals(
            listOf(Kind.ENERGY, Kind.WORN_PARTIAL),
            RunVerdictReason.kinds("에너지를 다 썼습니다 · 신발이 다 닳은 뒤의 걸음은 일부만 적립했습니다"),
        )
        // 닳은 뒤 적립이 없는 서버(0119 전 · worn_rate 0)의 예전 글도 그대로 안다
        assertEquals(listOf(Kind.WORN_AFTER), RunVerdictReason.kinds("내구도가 다 닳은 뒤의 걸음은 적립하지 않습니다"))
        assertEquals(listOf(Kind.WORN), RunVerdictReason.kinds("내구도가 다 닳아 적립이 없습니다"))
    }
}
