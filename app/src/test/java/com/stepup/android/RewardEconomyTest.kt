package com.stepup.android

import com.stepup.android.domain.RewardEconomy
import org.junit.Assert.assertEquals
import org.junit.Test

class RewardEconomyTest {

    /** 앱 기본 일일 목표 (UserPrefs.DEFAULT_GOAL 과 같은 값) */
    private val DEFAULT_GOAL = 8_000

    @Test
    fun `세션 적립은 에너지 한도 내 걸음만 인정한다`() {
        // 에너지 1.0 = 600보 적립 가능
        val reward = RewardEconomy.sessionReward(walkedSteps = 1000, energyRemaining = 1.0, sneakerLevel = 1)
        assertEquals(600, reward.rewardedSteps)
        assertEquals(600 * RewardEconomy.POINTS_PER_STEP, reward.points, 1e-9)
        assertEquals(1.0, reward.energyUsed, 1e-9)
    }

    @Test
    fun `에너지가 충분하면 걸은 만큼 전부 적립된다`() {
        val reward = RewardEconomy.sessionReward(walkedSteps = 500, energyRemaining = 10.0, sneakerLevel = 1)
        assertEquals(500, reward.rewardedSteps)
        assertEquals(500 * RewardEconomy.POINTS_PER_STEP, reward.points, 1e-9)
        assertEquals(500.0 / 600, reward.energyUsed, 1e-9)
    }

    @Test
    fun `걸음당 SUP 는 서버 규칙 값 — 효율 지수 ÷ 100 이 배율(경제 4판, 0116)`() {
        // 서버 기본 0.0118 — 가운데 일반 신발(효율 140)이 걸음당 0.01652
        assertEquals(0.0118, RewardEconomy.POINTS_PER_STEP, 0.0)
        val mid = RewardEconomy.sessionReward(walkedSteps = 1_000, energyRemaining = 10.0, earningMultiplier = 1.4, pointsPerStep = 0.0118)
        assertEquals(16.52, mid.points, 1e-9)
        // 운영자가 값을 바꾸면 예상도 그 값으로
        val tuned = RewardEconomy.sessionReward(walkedSteps = 1_000, energyRemaining = 10.0, earningMultiplier = 1.0, pointsPerStep = 0.02)
        assertEquals(20.0, tuned.points, 1e-9)
    }

    @Test
    fun `착화감은 에너지 70퍼센트 절약까지 — 같은 에너지로 1 ÷ 0_3 배를 걷는다`() {
        // 에너지 1 · 절약 70%(에너지 배율 0.3) → 2,000보
        assertEquals(2_000, RewardEconomy.earnableSteps(1.0, 0.3))
        // 그보다 크게 아끼는 값은 70% 로 자른다
        assertEquals(2_000, RewardEconomy.earnableSteps(1.0, 0.1))
        val reward = RewardEconomy.sessionReward(walkedSteps = 3_000, energyRemaining = 1.0, earningMultiplier = 1.0, energyEfficiency = 0.3)
        assertEquals(2_000, reward.rewardedSteps)
        assertEquals(1.0, reward.energyUsed, 1e-9)
    }

    @Test
    fun `스니커즈 레벨이 오르면 적립 배율이 커진다`() {
        val lv1 = RewardEconomy.sessionReward(600, 10.0, sneakerLevel = 1)
        val lv3 = RewardEconomy.sessionReward(600, 10.0, sneakerLevel = 3)
        assertEquals(lv1.points * 1.3, lv3.points, 1e-9)
    }

    @Test
    fun `에너지가 없으면 적립되지 않는다`() {
        val reward = RewardEconomy.sessionReward(walkedSteps = 1000, energyRemaining = 0.0, sneakerLevel = 1)
        assertEquals(0, reward.rewardedSteps)
        assertEquals(0.0, reward.points, 1e-9)
        assertEquals(0.0, reward.energyUsed, 1e-9)
    }

    @Test
    fun `음수 입력은 0으로 처리한다`() {
        val reward = RewardEconomy.sessionReward(walkedSteps = -10, energyRemaining = -1.0, sneakerLevel = 1)
        assertEquals(0, reward.rewardedSteps)
        assertEquals(0.0, reward.points, 1e-9)
    }

    @Test
    fun `스트릭 보너스는 7일까지만 가산된다`() {
        val goal = DEFAULT_GOAL
        assertEquals(RewardEconomy.DAILY_GOAL_BONUS, RewardEconomy.goalBonus(1, goal), 1e-9)
        assertEquals(RewardEconomy.DAILY_GOAL_BONUS * 1.7, RewardEconomy.goalBonus(8, goal), 1e-9)
        assertEquals(RewardEconomy.goalBonus(8, goal), RewardEconomy.goalBonus(30, goal), 1e-9)
    }

    @Test
    fun `목표를 높게 잡으면 보너스도 오른다`() {
        // 1,000보당 2.5 SUP — 목표에 곧게 비례한다.
        assertEquals(7.5, RewardEconomy.goalBaseBonus(3_000), 1e-9)
        assertEquals(20.0, RewardEconomy.goalBaseBonus(8_000), 1e-9)
        assertEquals(50.0, RewardEconomy.goalBaseBonus(20_000), 1e-9)
    }

    @Test
    fun `기본 목표에서는 규칙을 바꾸기 전과 같은 금액이 나온다`() {
        // 규칙을 바꾸면서 이미 쓰던 사람의 보상이 줄면 그건 개선이 아니다.
        // 8,000 × 2.5 / 1000 = 20 = DAILY_GOAL_BONUS
        assertEquals(
            RewardEconomy.DAILY_GOAL_BONUS,
            RewardEconomy.goalBaseBonus(DEFAULT_GOAL),
            1e-9,
        )
    }

    @Test
    fun `목표 보너스에 스트릭이 곱해진다`() {
        // 두 축은 서로 곱한다 — 높은 목표를 오래 지킨 사람이 가장 많이 받는다.
        assertEquals(
            RewardEconomy.goalBaseBonus(20_000) * 1.7,
            RewardEconomy.goalBonus(8, 20_000),
            1e-9,
        )
    }

    @Test
    fun `레벨별 최대 에너지와 업그레이드 비용`() {
        // 0119 전 서버(LEGACY): 레벨마다 +2 · 0119 부터: 레벨과 상관없이 10
        assertEquals(10.0, RewardEconomy.maxEnergy(1, com.stepup.android.domain.StatRule.LEGACY), 1e-9)
        assertEquals(14.0, RewardEconomy.maxEnergy(3, com.stepup.android.domain.StatRule.LEGACY), 1e-9)
        assertEquals(10.0, RewardEconomy.maxEnergy(1), 1e-9)
        assertEquals(10.0, RewardEconomy.maxEnergy(20), 1e-9)
        assertEquals(100.0, RewardEconomy.upgradeCost(1), 1e-9)
        assertEquals(300.0, RewardEconomy.upgradeCost(3), 1e-9)
    }

    @Test
    fun `내구도가 다 닳은 뒤의 걸음은 적립하지 않는다 - 서버 0066 과 같은 규칙`() {
        // 남은 내구도 50 = 10분(600초). 20분(1201초) 달린 2000걸음 중 앞의 10분 몫만
        assertEquals(999, RewardEconomy.stepsWithinDurability(2000, 50, 1201))
        // 다 닳지 않았으면 그대로
        assertEquals(2000, RewardEconomy.stepsWithinDurability(2000, 50, 600))
        assertEquals(2000, RewardEconomy.stepsWithinDurability(2000, 450, 1201))
        // 다 닳은 신발 · 달린 시간을 모르면
        assertEquals(0, RewardEconomy.stepsWithinDurability(2000, 0, 1201))
        assertEquals(2000, RewardEconomy.stepsWithinDurability(2000, 0, 0))
    }
}
