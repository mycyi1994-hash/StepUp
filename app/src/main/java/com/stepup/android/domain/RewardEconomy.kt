package com.stepup.android.domain

import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * StepUp의 M2E(Move-to-Earn) 경제 모델.
 *
 * - 러닝 세션 중 걸은 걸음에 대해서만 SUP 포인트가 적립된다.
 * - 에너지가 남아 있는 동안만 적립되며, 에너지는 매일 자정에 최대치로 리필된다.
 * - 착용한 스니커즈의 효율/착화감, 파티런 인원, 활성 부스트가 적립을 증폭한다.
 */
object RewardEconomy {

    /**
     * 1보당 기본 적립 포인트(SUP) — 효율 100(지수) 신발의 값. 서버 설정 points_per_step(0116)과 같은 값이고, 화면의 예상은
     * 서버에서 받은 값([ShoeRules.pointsPerStep])을 [sessionReward] 에 넘긴다. 신발의 효율 지수 ÷ 100 이 곧 적립 배율이다.
     */
    const val POINTS_PER_STEP = 0.0118

    /**
     * 에너지 1 로 적립하는 걸음의 가장 큰 배율의 역수 — 착화감 상한(에너지 70% 절약, 서버 economy.comfort_cap_bps 7000)이면
     * 같은 에너지로 1 ÷ 0.3 배를 걷는다. 예전(0066)에는 50% 까지였다.
     */
    const val MIN_ENERGY_FACTOR = 0.3

    /** 에너지 1칸으로 적립할 수 있는 걸음 수 */
    const val STEPS_PER_ENERGY = 600

    /**
     * 하루 에너지를 분으로 옮길 때의 걸음 빠르기(분당 걸음) — 서버 `runner_me()` 의 energy_minutes(에너지 × 600 ÷ 165, 0121)와 같은 기준
     * (Lv1 5.5 = 3,300보 ≈ 20분). 출발 전 점검의 "오늘은 약 N분 동안 적립돼요"도 이 값으로 남은 에너지를 분으로 바꾼다
     * (`scripts/check-economy-twin.py` 가 서버와 맞대어 본다)
     */
    const val RUN_STEPS_PER_MINUTE = 165

    /** 기본 최대 에너지 */
    const val BASE_MAX_ENERGY = 10.0

    /** 일일 목표 달성 기본 보너스(SUP) — 기본 목표(8,000보) 기준 */
    const val DAILY_GOAL_BONUS = 20.0

    /**
     * 일일 목표 1,000보당 달성 보너스(SUP).
     *
     * 목표를 높게 잡은 사람이 더 받는다. 8,000보 × 2.5 = 20 SUP 라서 기본
     * 목표에서는 예전과 같은 금액이 나온다 — 규칙을 바꾸면서 이미 쓰던
     * 사람의 보상이 줄어들면 그건 개선이 아니다.
     *
     * 목표에 비례해 곧게 올린다. 곱절로 걷는 일은 곱절로 힘들기 때문이다.
     * 높게 잡기만 하고 안 걸으면 한 푼도 못 받으므로, 무작정 올리는 것을
     * 막는 장치는 규칙 안에 이미 있다.
     */
    const val GOAL_BONUS_PER_1K = 2.5

    /** 연속 달성(스트릭) 1일당 추가 보너스 비율 */
    const val STREAK_BONUS_RATE = 0.1

    /** 평균 보폭(m) — 거리 추정용 */
    const val STRIDE_METERS = 0.762

    /** 걸음당 소모 칼로리(kcal) 추정치 */
    const val KCAL_PER_STEP = 0.04

    /** 파티런 1명 추가당 보너스 비율 (본인 제외, 최대 5명까지 가산) */
    const val PARTY_BONUS_RATE = 0.10
    const val PARTY_BONUS_CAP = 5

    /** XP 부스터 배율 */
    const val XP_BOOST_MULTIPLIER = 2.0

    /**
     * 내구도 1 로 달릴 수 있는 시간(초) — 달린 1분마다 5 닳는다(서버 0064 · economy.durability_sec_per_point). 효율 포인트를 넣은 신발은
     * 그 배수만큼 짧다(서버 0119) — 신발의 수명은 [startLifeSec] · [lifeSecOf] 로 센다.
     */
    const val DURABILITY_SEC_PER_POINT = 12

    /**
     * 남은 내구도로 달린 걸음(가득 구간) — 포인트 없는 신발의 수명(내구도 × 12초)으로. [durabilityNow] 는 달리기 전 남은 내구도,
     * [activeSec] 는 멈춘 시간을 뺀 달린 시간(움직였든 서 있었든). 효율 포인트까지 보려면 [stepsWithinLife] 에 그 신발의 수명을 넘긴다.
     */
    fun stepsWithinDurability(steps: Int, durabilityNow: Int, activeSec: Long): Int =
        stepsWithinLife(steps, durabilityNow.coerceAtLeast(0).toLong() * DURABILITY_SEC_PER_POINT, activeSec)

    /**
     * 신발 수명 [lifeSec](남은 내구도로 달릴 수 있는 초) 안에 걸은 걸음 — 걸음은 달린 시간에 고르게 걸었다고 본다(서버 record_session 과
     * 같은 셈: 걸음 × 수명 ÷ 달린 시간, 버림). 이 뒤의 걸음은 닳은 구간이다 — 서버 0119 는 일부([StatRule.wornRate])만 적립하고,
     * 그 전 서버는 적립하지 않는다(0066).
     */
    fun stepsWithinLife(steps: Int, lifeSec: Long, activeSec: Long): Int {
        if (steps <= 0 || activeSec <= 0L) return steps
        val life = lifeSec.coerceAtLeast(0L)
        if (life >= activeSec) return steps
        return (steps.toLong() * life / activeSec).toInt()
    }

    /** 스니커즈 민팅 비용 */
    const val MINT_COST = 500.0

    // ── 스니커즈 ─────────────────────────────────────────────

    /**
     * 하루 에너지 최대치의 어림(서버가 준 값을 아직 모를 때) — 서버 `economy.energy_max_for_level`.
     * 0119 부터 레벨과 상관없이 10(레벨마다 늘어나는 양은 규칙 `stats.energyPerLevel`, 0119 전 서버는 2)
     */
    fun maxEnergy(sneakerLevel: Int, stats: StatRule = StatRule.FALLBACK): Double = stats.energyMax(sneakerLevel)

    fun sneakerMultiplier(sneakerLevel: Int): Double =
        1.0 + 0.15 * (sneakerLevel - 1).coerceAtLeast(0)

    fun upgradeCost(currentLevel: Int): Double = currentLevel * 100.0

    /** 희귀도가 높을수록 강화 비용이 가파르다 (그만큼 성능도 높다) */
    fun sneakerUpgradeCost(currentLevel: Int, rarity: Rarity): Double =
        (currentLevel * 100.0 * (1.0 + rarity.ordinal * 0.25)).roundToInt().toDouble()

    // ── 파티런 ───────────────────────────────────────────────

    /** partySize = 본인 포함 인원 */
    fun partyMultiplier(partySize: Int): Double =
        1.0 + PARTY_BONUS_RATE * (partySize - 1).coerceIn(0, PARTY_BONUS_CAP)

    fun partyBonusPercent(partySize: Int): Int =
        ((partyMultiplier(partySize) - 1.0) * 100).roundToInt()

    // ── 세션 정산 ────────────────────────────────────────────

    /**
     * 세션 정산.
     *
     * @param earningMultiplier 스니커즈 적립 배율
     * @param energyEfficiency  1.0 미만이면 같은 에너지로 더 많이 걷는다
     * @param partyMultiplier   파티런 보너스
     * @param boostMultiplier   XP 부스터 등 활성 부스트
     * @param pointsPerStep     효율 100 신발의 걸음당 SUP — 서버 규칙(ShoeRules.pointsPerStep)
     * @param fullSteps         남은 내구도로 달린 걸음(가득 구간, [stepsWithinLife]) — 이 뒤의 걸음(닳은 구간)은 [wornRate] 만큼만
     *                          적립한다(서버 0119). 기본은 모든 걸음이 가득 구간
     * @param wornRate          닳은 구간의 적립 비율(0 ~ 1) — 걸음은 그대로 세고 에너지도 그대로 쓴다
     * @param energyMultiplier  효율 포인트를 넣은 신발이 에너지를 더 쓰는 배수(1 이상, 서버 0119 economy.energy_mult)
     */
    fun sessionReward(
        walkedSteps: Int,
        energyRemaining: Double,
        earningMultiplier: Double,
        energyEfficiency: Double = 1.0,
        partyMultiplier: Double = 1.0,
        boostMultiplier: Double = 1.0,
        pointsPerStep: Double = POINTS_PER_STEP,
        fullSteps: Int = Int.MAX_VALUE,
        wornRate: Double = 0.0,
        energyMultiplier: Double = 1.0,
    ): SessionReward {
        val eff = energyEfficiency.coerceIn(MIN_ENERGY_FACTOR, 1.0) * energyMultiplier.coerceAtLeast(1.0)
        val energy = energyRemaining.coerceAtLeast(0.0)
        val earnableSteps = floor(energy * STEPS_PER_ENERGY / eff).toInt()
        val rewardedSteps = walkedSteps.coerceAtLeast(0).coerceAtMost(earnableSteps)
        // 에너지로 자른 뒤에 가득 구간 · 닳은 구간으로 가른다(서버 0119 와 같은 차례)
        val full = rewardedSteps.coerceAtMost(fullSteps.coerceAtLeast(0))
        val worn = rewardedSteps - full
        val points = (full + worn * wornRate.coerceIn(0.0, 1.0)) * pointsPerStep *
            earningMultiplier * partyMultiplier * boostMultiplier
        val energyUsed = rewardedSteps.toDouble() * eff / STEPS_PER_ENERGY
        return SessionReward(rewardedSteps, points, energyUsed)
    }

    /**
     * 신은 신발 · 서버 규칙으로 본 혼자 러닝 한 번의 예상 적립 — 달리는 중 화면과 러닝을 마칠 때의 예상이 같은 셈을 쓴다.
     * 예상일 뿐이고 정산은 서버가 한다(record_session).
     *
     * - 신발 수명(남은 내구도 × 12초 ÷ 효율 포인트 배수, [startLifeSec])까지의 걸음은 다 적립한다.
     * - 그 뒤의 걸음은 규칙이 닳은 뒤에도 적립하면([StatRule.earnsWhenWorn], 서버 0119) [StatRule.wornRate] 만큼, 아니면 적립하지
     *   않는다(0066 — 그때는 다 닳은 신발의 적립 배율도 0 이다).
     * - 효율 포인트를 넣은 신발은 그 배수만큼 에너지를 더 쓴다(0119).
     *
     * @param steps 러닝에서 걸은 걸음
     * @param activeSec 멈춘 시간을 뺀 달린 시간(초) — 0 이면 수명으로 가르지 않는다
     * @param shoe 신은 신발 — 없으면 [fallbackMultiplier](레벨 기반 배율)로
     */
    fun shoeSessionReward(
        steps: Int,
        activeSec: Long,
        energyRemaining: Double,
        shoe: Sneaker?,
        rules: ShoeRules,
        fallbackMultiplier: Double = 1.0,
        partyMultiplier: Double = 1.0,
        boostMultiplier: Double = 1.0,
    ): SessionReward {
        val server = shoe?.server
        val stats = rules.stats
        val life = if (shoe != null) rules.startLifeSec(shoe) else null
        // 수명을 아는 신발만 시간으로 가른다. 최대 내구도를 모르는데 다 닳은 신발(예전 서버)은 닳은 뒤에도 적립하는 규칙이면
        // 처음부터 닳은 구간, 아니면 예전처럼 배율(0)로 치른다
        val fullSteps = when {
            life == null -> steps
            server?.hasDurabilityMax == true -> stepsWithinLife(steps, life, activeSec)
            stats.earnsWhenWorn -> 0
            else -> steps
        }
        val earning = when {
            shoe == null -> fallbackMultiplier
            // 닳은 뒤에도 적립하는 규칙이면 내구도는 배율이 아니라 구간으로 치른다 — 0% 신발도 효율 그대로
            server != null && stats.earnsWhenWorn -> 1.0 + server.efficiencyBps / 10_000.0
            else -> shoe.earningMultiplier
        }
        return sessionReward(
            walkedSteps = if (stats.earnsWhenWorn) steps else fullSteps,
            energyRemaining = energyRemaining,
            earningMultiplier = earning,
            energyEfficiency = shoe?.energyEfficiency ?: 1.0,
            partyMultiplier = partyMultiplier,
            boostMultiplier = boostMultiplier,
            pointsPerStep = rules.pointsPerStep,
            fullSteps = if (stats.earnsWhenWorn) fullSteps else Int.MAX_VALUE,
            wornRate = stats.wornRate,
            energyMultiplier = if (shoe != null) rules.energyMultOf(shoe) else 1.0,
        )
    }

    /** 레벨만 아는 경우의 간편 정산 (스니커즈 인벤토리가 없을 때의 폴백) */
    fun sessionReward(
        walkedSteps: Int,
        energyRemaining: Double,
        sneakerLevel: Int,
    ): SessionReward = sessionReward(
        walkedSteps = walkedSteps,
        energyRemaining = energyRemaining,
        earningMultiplier = sneakerMultiplier(sneakerLevel),
    )

    /**
     * 현재 에너지로 더 걸을 수 있는 걸음 수. [energyMultiplier] 는 효율 포인트를 넣은 신발이 에너지를 더 쓰는 배수(1 이상,
     * 서버 0119 economy.energy_mult — [energyMultOf])
     */
    fun earnableSteps(energyRemaining: Double, energyEfficiency: Double = 1.0, energyMultiplier: Double = 1.0): Int =
        floor(energyRemaining.coerceAtLeast(0.0) * STEPS_PER_ENERGY /
            (energyEfficiency.coerceIn(MIN_ENERGY_FACTOR, 1.0) * energyMultiplier.coerceAtLeast(1.0))).toInt()

    /** streak일 연속 달성 시 일일 목표 보너스 (7일 초과분은 가산하지 않음) */
    /**
     * 목표 달성 보너스.
     *
     * @param streak 오늘까지의 연속 달성 일수 (1 이상)
     * @param dailyGoal 그 사람이 정한 목표 걸음
     */
    fun goalBonus(streak: Int, dailyGoal: Int): Double =
        goalBaseBonus(dailyGoal) * (1.0 + STREAK_BONUS_RATE * (streak - 1).coerceIn(0, 7))

    /** 연속 달성을 빼고, 목표만으로 정해지는 몫 */
    fun goalBaseBonus(dailyGoal: Int): Double =
        ((dailyGoal / 1000.0) * GOAL_BONUS_PER_1K * 100).roundToInt() / 100.0

    fun distanceMeters(steps: Int): Double = steps * STRIDE_METERS

    fun calories(steps: Int): Double = steps * KCAL_PER_STEP
}

// ── 신발 수명 · 닳은 구간(서버 0119 — docs/economy-roots-contract-2026-11-21.md) ──────────
//
// 효율 포인트를 넣은 신발은 그 배수만큼 빨리 닳고 에너지를 더 쓴다. 다 닳은 뒤의 걸음은 세고, 적립은 일부만 한다.
// 셈은 서버 규칙([ShoeRules.stats])이 한다 — 0119 전 서버([StatRule.LEGACY])면 배수는 늘 1 이고 닳은 뒤 적립은 없다(예전 그대로).

/** 다 닳은 신발의 적립 비율(%) — 화면 글의 "N%만 적립돼요"(서버 worn_rate 0.30 → 30). 닳은 뒤 적립이 없는 규칙이면 0 */
val StatRule.wornPercent: Int get() = (wornRate * 100).roundToInt()

/**
 * 닳는 속도 · 에너지 배수를 정하는 효율 포인트 — 서버 `economy.eff_point_mult` 와 같이 폰에서 올린 예전 신발(IMPORT · MINT) ·
 * 폰에만 있던 신발은 0(배수 1).
 */
/**
 * 신은 신발의 적립 배수(예상) — (1 + 효율) × 내구도 몫. 다 닳은 신발은 0119 부터 [StatRule.wornRate](30%)만큼 받는다
 * (예전 규칙 · 0119 전 서버는 0 — [Sneaker.earningMultiplier] 그대로). 실제 적립은 서버가 정한다
 */
fun Sneaker.earningMultiplierWith(stats: StatRule): Double {
    val s = server ?: return earningMultiplier
    val base = 1.0 + s.efficiencyBps / 10_000.0
    return if (s.durabilityFactor <= 0.0 && stats.earnsWhenWorn) base * stats.wornRate else base * s.durabilityFactor
}

val Sneaker.wearPoints: Int get() = server?.takeIf { it.upgradable }?.points?.efficiency?.coerceAtLeast(0) ?: 0

/** 이 신발이 닳는 속도의 배수(서버 `economy.wear_mult`) — 효율 포인트가 없으면 1 */
fun ShoeRules.wearMultOf(shoe: Sneaker): Double = stats.wearMult(grade(shoe.rarity).pointRateBps, shoe.wearPoints)

/** 이 신발이 에너지를 쓰는 속도의 배수(서버 `economy.energy_mult`) — 효율 포인트가 없으면 1 */
fun ShoeRules.energyMultOf(shoe: Sneaker): Double = stats.energyMult(grade(shoe.rarity).pointRateBps, shoe.wearPoints)

/**
 * 내구도 [durability] 로 이 신발이 달릴 수 있는 초(내려서) — 서버 정산과 같은 식: 내구도 × 12초 ÷ 닳는 배수.
 * 150 · 포인트당 1% · 효율 포인트 50 이면 배수 1.5 → 1,200초.
 */
fun ShoeRules.lifeSecOf(shoe: Sneaker, durability: Int): Long =
    floor(stats.lifeSeconds(durability, grade(shoe.rarity).pointRateBps, shoe.wearPoints) + LIFE_ROUNDING).toLong()

/** 나눗셈의 끝자리 때문에 1,200초가 1,199.999… 로 내려가지 않게 */
private const val LIFE_ROUNDING = 1e-6

/**
 * 이 신발이 달린 [activeSec] 초 동안 닳는 내구도(운 전) — 다 채운 1분마다 5 × 닳는 배수(서버 `durability_wear_time` × `wear_mult`).
 * 포인트가 없으면 예전과 같이 1분마다 5.
 */
fun ShoeRules.wearOf(shoe: Sneaker, activeSec: Long): Int =
    stats.wearFor(activeSec, grade(shoe.rarity).pointRateBps, shoe.wearPoints)

/**
 * 러닝을 시작할 때 이 신발로 가득 적립하며 달릴 수 있는 시간(초) — 남은 내구도의 [lifeSecOf]. 다 닳았으면 0.
 * 알 수 없으면 null: 폰에만 있던 신발이거나, 최대 내구도를 보내지 않는 예전 서버의 아직 닳지 않은 신발.
 */
fun ShoeRules.startLifeSec(shoe: Sneaker): Long? {
    val server = shoe.server ?: return null
    return when {
        server.hasDurabilityMax -> lifeSecOf(shoe, server.durabilityNow)
        server.durabilityPts <= 0.0 -> 0L
        else -> null
    }
}

/** 세션 정산 결과 */
data class SessionReward(
    val rewardedSteps: Int,
    val points: Double,
    val energyUsed: Double,
)

/** 부스트 종류와 가격·지속시간 */
enum class BoostType(
    val id: String,
    val cost: Double,
    val durationMillis: Long,
) {
    /** 즉시 에너지 2칸 회복 */
    ENERGY_CELL("ENERGY_CELL", 50.0, 0L),

    /** 24시간 동안 스트릭 보호 */
    STREAK_SHIELD("STREAK_SHIELD", 120.0, 24 * 60 * 60 * 1000L),

    /** 24시간 동안 세션 적립 2배 */
    XP_BOOSTER("XP_BOOSTER", 200.0, 24 * 60 * 60 * 1000L);

    val isInstant: Boolean get() = durationMillis == 0L

    companion object {
        fun of(id: String): BoostType = entries.firstOrNull { it.id == id } ?: ENERGY_CELL
    }
}
