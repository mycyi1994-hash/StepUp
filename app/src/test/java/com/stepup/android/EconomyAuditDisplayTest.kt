package com.stepup.android

import com.stepup.android.domain.Faction
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.RewardEconomy
import com.stepup.android.domain.RunnerLevel
import com.stepup.android.domain.ServerStats
import com.stepup.android.domain.ShoeRules
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.StatPoints
import com.stepup.android.domain.StatRule
import com.stepup.android.domain.TogetherRule
import com.stepup.android.ui.screens.walk.ShoeCheckLine
import com.stepup.android.ui.screens.walk.runCheckLine
import com.stepup.android.ui.screens.walk.shoeCheckLine
import com.stepup.android.ui.screens.walk.todayEarnableSteps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 2026-11-22 경제 점검 — 앱이 보이는 숫자가 서버(0120 · 0121 · 0122)가 실제로 치르는 것과 맞는가.
 * 출발 전 점검의 적립 분(신발 수명 · 오늘 에너지 가운데 먼저 끝나는 쪽) · 번개 러닝의 올릴 때 인원 배수 · 크루 효율 섞기 ·
 * 러너 레벨 설명의 "하루 에너지 +2분"(늘지 않는 계정에는 말하지 않는다).
 */
class EconomyAuditDisplayTest {

    private val rules = ShoeRules.FALLBACK

    /** 일반 신발 — 최대 내구도 [max], 남은 [now](포인트 없음이면 내구도 × 12초) */
    private fun shoe(now: Int, max: Int, effPoints: Int = 0, comfortBps: Int = 0) = Sneaker(
        id = 1, faction = Faction.WIND, rarity = Rarity.COMMON, variant = 0, level = 1, mintNumber = 1,
        luck = 1.0, comfort = 1.0, durability = 100, equipped = true, acquiredAt = 0L, modelId = 1107,
        server = ServerStats(
            origin = "DRAW", efficiencyBps = 0, comfortBps = comfortBps,
            durabilityPts = if (max > 0) now * 100.0 / max else 100.0,
            maxLevel = 20, status = "OWNED", chainState = "APP", canWithdraw = false, upgradeCost = 0.0,
            repairCostPerPoint = 0.0, genesisNo = 0, durabilityNow = now, durabilityMax = max, durabilityBase = max,
            points = StatPoints(total = effPoints, efficiency = effPoints),
        ),
    )

    // ── 출발 전 점검: 신발 수명 · 오늘 에너지 ──

    @Test fun `새 Lv1 계정에 가득 찬 일반 신발 - 40분이 아니라 에너지로 약 20분, 그 뒤 30퍼센트를 말하지 않는다`() {
        val full = shoe(now = 200, max = 200)
        // 신발만 보면 200 × 12초 = 40분
        assertEquals(ShoeCheckLine.Earns(40), shoeCheckLine(full, rules))
        // 하루 에너지 5.5(러너 레벨 Lv1, 0121) = 3,300보 = 분당 165보로 20분
        val steps = todayEarnableSteps(5.5, full, rules)
        assertEquals(3_300, steps)
        assertEquals(ShoeCheckLine.EnergyEnds(20), runCheckLine(full, rules, steps))
    }

    @Test fun `신발이 에너지보다 먼저 닳을 때만 닳은 뒤 비율 줄 그대로`() {
        val full = shoe(now = 200, max = 200)
        // 에너지 16.5 = 9,900보 = 60분 > 신발 40분
        assertEquals(ShoeCheckLine.Earns(40), runCheckLine(full, rules, todayEarnableSteps(16.5, full, rules)))
        // 같이 끝나면(20분 = 20분) 닳은 뒤 적립은 없다 — 에너지 줄
        val half = shoe(now = 100, max = 200)
        assertEquals(ShoeCheckLine.Earns(20), shoeCheckLine(half, rules))
        assertEquals(ShoeCheckLine.EnergyEnds(20), runCheckLine(half, rules, todayEarnableSteps(5.5, half, rules)))
    }

    @Test fun `곧 닳는 신발 - 에너지가 먼저 끝나면 에너지 줄, 아니면 수리 안내 그대로`() {
        val low = shoe(now = 20, max = 155)
        assertEquals(ShoeCheckLine.Low(4), shoeCheckLine(low, rules))
        // 에너지 0.5 = 300보 = 1분
        assertEquals(ShoeCheckLine.EnergyEnds(1), runCheckLine(low, rules, todayEarnableSteps(0.5, low, rules)))
        // 에너지 10 = 6,000보 = 36분 — 신발이 먼저(4분)
        assertEquals(ShoeCheckLine.Low(4), runCheckLine(low, rules, todayEarnableSteps(10.0, low, rules)))
    }

    @Test fun `에너지를 모르면 적립 분을 약속하지 않는다 - 닳음 안내는 그대로`() {
        assertEquals(ShoeCheckLine.Ready, runCheckLine(shoe(now = 200, max = 200), rules, null))
        assertEquals(ShoeCheckLine.Low(4), runCheckLine(shoe(now = 20, max = 155), rules, null))
        assertEquals(ShoeCheckLine.WornOut, runCheckLine(shoe(now = 0, max = 155), rules, null))
        assertNull(todayEarnableSteps(null, shoe(now = 200, max = 200), rules))
    }

    @Test fun `오늘 에너지를 다 썼으면 - 준비 줄, 닳은 신발은 N퍼센트를 말하지 않는 수리 안내`() {
        assertEquals(ShoeCheckLine.Ready, runCheckLine(shoe(now = 200, max = 200), rules, 0))
        assertEquals(ShoeCheckLine.Ready, runCheckLine(shoe(now = 20, max = 155), rules, 0))
        assertEquals(ShoeCheckLine.WornNoEnergy, runCheckLine(shoe(now = 0, max = 155), rules, 0))
        // 에너지가 남아 있으면 닳은 신발도 그 동안 N% — "닳은 신발이에요 · 그대로 달리면 N%"
        assertEquals(ShoeCheckLine.WornOut, runCheckLine(shoe(now = 0, max = 155), rules, 3_300))
        // 1분이 안 되게 남았어도 0분이라 하지 않는다
        assertEquals(ShoeCheckLine.EnergyEnds(1), runCheckLine(shoe(now = 200, max = 200), rules, 100))
    }

    @Test fun `수명을 모르는 신발은 에너지가 있어도 준비 줄`() {
        val phoneOnly = Sneaker(
            id = 2, faction = Faction.WIND, rarity = Rarity.COMMON, variant = 0, level = 1, mintNumber = 1,
            luck = 1.0, comfort = 1.0, durability = 100, equipped = true, acquiredAt = 0L,
        )
        assertEquals(ShoeCheckLine.Ready, runCheckLine(phoneOnly, rules, 3_300))
    }

    @Test fun `오늘 적립할 걸음 - 효율 포인트 배수 · 착화감 · 에너지 드링크까지`() {
        val plain = shoe(now = 200, max = 200)
        assertEquals(3_300, todayEarnableSteps(5.5, plain, rules))
        // 효율 포인트 50(포인트당 1%) — 에너지를 1.5배 쓴다
        assertEquals(2_200, todayEarnableSteps(5.5, shoe(now = 200, max = 200, effPoints = 50), rules))
        // 착화감 50% — 같은 에너지로 2배
        assertEquals(6_600, todayEarnableSteps(5.5, shoe(now = 200, max = 200, comfortBps = 5000), rules))
        // 에너지 드링크(절반만 쓴다) — 2배. 0 이하 · 1 이상은 없는 것으로
        assertEquals(6_600, todayEarnableSteps(5.5, plain, rules, energyFactor = 0.5))
        assertEquals(3_300, todayEarnableSteps(5.5, plain, rules, energyFactor = 0.0))
        assertEquals(3_300, todayEarnableSteps(5.5, plain, rules, energyFactor = 1.5))
        // 신발 없이도 센다(배수 1)
        assertEquals(3_300, todayEarnableSteps(5.5, null, rules))
        assertEquals(0, todayEarnableSteps(0.0, plain, rules))
    }

    @Test fun `분당 걸음은 서버 runner_me 와 같은 165`() {
        assertEquals(165, RewardEconomy.RUN_STEPS_PER_MINUTE)
        // Lv1 5.5 × 600 ÷ 165 = 20분 · 상한 10 × 600 ÷ 165 ≈ 36분
        assertEquals(20, (5.5 * RewardEconomy.STEPS_PER_ENERGY / RewardEconomy.RUN_STEPS_PER_MINUTE).toInt())
        assertEquals(36, (10.0 * RewardEconomy.STEPS_PER_ENERGY / RewardEconomy.RUN_STEPS_PER_MINUTE).toInt())
    }

    @Test fun `예전 규칙의 신발 줄도 에너지가 먼저면 에너지 줄`() {
        val legacy = rules.copy(stats = StatRule.LEGACY)
        val full = shoe(now = 200, max = 200)
        assertEquals(ShoeCheckLine.EnergyEnds(20), runCheckLine(full, legacy, todayEarnableSteps(5.5, full, legacy)))
    }

    // ── 번개 · 크루: 올릴 때 서버가 치르는 인원 배수 · 효율 섞기 ──

    @Test fun `함께 달린 사람만 세는 규칙이면 번개 러닝을 올릴 때 인원 배수는 1`() {
        val active = TogetherRule.FALLBACK.copy(fromServer = true)
        assertEquals(1.0, active.forFlash().uploadPartyMultiplier(4), 1e-12)
        // 규칙을 아직 못 받았어도(기본값 ACTIVE) 1 — 치르지 않을 수 있는 배수를 예상에 넣지 않는다
        assertEquals(1.0, TogetherRule.FALLBACK.forFlash().uploadPartyMultiplier(4), 1e-12)
        // 예전 서버(0120 전) · 번개만 예전 규칙으로 되돌린 서버 — 함께 출발한 인원으로 +10%/명
        assertEquals(1.3, TogetherRule.LEGACY.forFlash().uploadPartyMultiplier(4), 1e-12)
        val flashOld = active.copy(flashRule = "LEGACY")
        assertEquals(1.3, flashOld.forFlash().uploadPartyMultiplier(4), 1e-12)
        // 크루 규칙은 그대로 ACTIVE
        assertEquals(1.0, flashOld.uploadPartyMultiplier(4), 1e-12)
        // 혼자면 배수 없음
        assertEquals(1.0, TogetherRule.LEGACY.uploadPartyMultiplier(1), 1e-12)
    }

    @Test fun `크루 효율을 섞을 수 있으면 달리는 중 금액을 보이지 않는다 - 받기 전 기본값도`() {
        assertTrue(TogetherRule.FALLBACK.mayBlend)
        assertFalse("받은 규칙만 섞는다고 보는 blends 는 기본값에서 false", TogetherRule.FALLBACK.blends)
        assertTrue(TogetherRule.FALLBACK.copy(fromServer = true).mayBlend)
        assertFalse(TogetherRule.FALLBACK.copy(blendShare = 0.0).mayBlend)
        assertFalse(TogetherRule.LEGACY.mayBlend)
    }

    // ── 러너 레벨: 하루 에너지가 더 늘 수 있는가 ──

    @Test fun `예전 보장 36분인 계정은 상한 10(Lv10 · 36분)까지 가도 늘지 않는다 - +2분 줄을 두지 않는다`() {
        val lv1 = RunnerLevel.parse(
            """{"enabled":true,"level":1,"steps":0,"energy_minutes":36,"level_energy_minutes":20,"next_energy_minutes":22,
               "energy_cap_level":10,"legacy_floor":true,"rewards":{"energy_minutes_per_level":2}}""",
        )!!
        assertFalse(lv1.energyRisesWithLevel)
        // Lv9 — 레벨 에너지 9.9 도 36분
        val lv9 = RunnerLevel.parse(
            """{"enabled":true,"level":9,"steps":80000,"energy_minutes":36,"level_energy_minutes":36,"next_energy_minutes":36,
               "energy_cap_level":10,"legacy_floor":true,"rewards":{"energy_minutes_per_level":2}}""",
        )!!
        assertFalse(lv9.energyRisesWithLevel)
    }

    @Test fun `새 계정은 상한 레벨 전까지 늘고, 상한 레벨부터는 늘지 않는다`() {
        val fresh = RunnerLevel.parse(
            """{"enabled":true,"level":1,"steps":0,"energy_minutes":20,"level_energy_minutes":20,"next_energy_minutes":22,
               "energy_cap_level":10,"legacy_floor":false,"rewards":{"energy_minutes_per_level":2}}""",
        )!!
        assertTrue(fresh.energyRisesWithLevel)
        val capped = RunnerLevel.parse(
            """{"enabled":true,"level":10,"steps":90000,"energy_minutes":36,"level_energy_minutes":36,"next_energy_minutes":36,
               "energy_cap_level":10,"legacy_floor":false,"rewards":{"energy_minutes_per_level":2}}""",
        )!!
        assertFalse(capped.energyRisesWithLevel)
    }

    @Test fun `상한을 60분(Lv21)으로 올리면 예전 보장 계정도 Lv10 부터 늘어 줄을 둔다`() {
        // 계약의 예(0121 — 상한 16.5, Lv7 · 보장 36분 · 레벨 32분): 상한 바로 아래 Lv20 은 58분
        val sample = RunnerLevel.parse(
            """{"enabled":true,"level":7,"steps":64210,"energy_minutes":36,"level_energy_minutes":32,"next_energy_minutes":34,
               "energy_cap_level":21,"legacy_floor":true,"rewards":{"energy_minutes_per_level":2}}""",
        )!!
        assertTrue(sample.energyRisesWithLevel)
    }

    @Test fun `셈에 쓸 칸을 모르면 예전처럼 줄을 둔다`() {
        val unknownCap = RunnerLevel.parse(
            """{"enabled":true,"level":3,"steps":25000,"energy_minutes":36,"level_energy_minutes":24,"legacy_floor":true,
               "rewards":{"energy_minutes_per_level":2}}""",
        )!!
        assertTrue(unknownCap.energyRisesWithLevel)
    }
}
