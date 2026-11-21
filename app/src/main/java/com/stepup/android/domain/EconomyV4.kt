package com.stepup.android.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * 경제 4판(서버 0116, 2026-11-18) — 신발 능력치 지수 · 등급 · 레벨업 포인트 · 수리 · 운 · 상자.
 *
 * 정본은 서버(`shoe_rules_v4` · `my_sneakers` · `box_status`)다. 앱은 받은 규칙으로 화면 값(지수 · 별 · 유형 · 포인트 미리보기)만 센다.
 * [ShoeRules.FALLBACK] 은 서버를 아직 못 읽었을 때의 같은 값 — 서버 0116 · 0118 과 맞대어 본다(`scripts/check-economy-twin.py`).
 *
 * 능력치는 지수로 보인다 — 100 이 기준이고 숫자가 2배면 2배:
 *  - 효율 지수 = 100 + 적립 +bps ÷ 100 (서버 `efficiency_bps` = 적립 +bps)
 *  - 착화감 지수 = 100 ÷ (1 − 에너지 절약 bps ÷ 10000) (서버 `comfort_bps` = 에너지 절약 bps)
 *  - 달리는 시간(분) = 내구도 ÷ 5 (1분마다 5 닳는다)
 */
data class GradeRule(
    val rarity: String,
    val efficiencyLo: Int,
    val efficiencyHi: Int,
    val comfortLo: Int,
    val comfortHi: Int,
    val durabilityLo: Int,
    val durabilityHi: Int,
    /** 레벨마다 받는 포인트 */
    val pointsPerLevel: Int,
    /** 5 · 10 · 15 …레벨에 더 받는 포인트 */
    val pointsMilestone: Int,
    /** 포인트 1 이 올리는 폭(만분율 — 100 = 뽑은 값의 1%) */
    val pointRateBps: Int,
    val maxLevel: Int,
)

/**
 * 강화 경험치(서버 0118 `shoe_rules_v4().forge` — docs/forge-xp-daily-spec-2026-11-19.md 2장). 재료 켤레값 · 통 크기를 미리 셀 때 쓰는 값이고,
 * 정본은 서버 견적(`forge_xp_quote`)이다. [fromServer] 가 아니면 [FALLBACK](서버 0118 의 기본값 — `scripts/check-economy-twin.py`).
 */
data class ForgeXpRule(
    /** 규칙 판 — 서버 `forge-xp-1` */
    val ruleVersion: String,
    /** 등급 한 칸마다 값 배수(3.1) — 재료 값 = 1,000 × step^등급 차례, 통은 레어부터 한 칸마다 × step */
    val step: Double,
    /** 일반 · 레어 Lv1 통 크기(켤레값, 2.88 — 1켤레값 = 1,000점) */
    val need: Double,
    /** 재료 스펙 배수 [아래, 위](0.7 ~ 1.3) — 0.7 + 0.6 × q */
    val spec: List<Double>,
    /** 재료마다 대성공 확률(0.10) · 그때 배수(2) */
    val critChance: Double,
    val critMult: Double,
    /** 강화한 재료가 물려주는 몫(0.5) */
    val inherit: Double,
    /** 닳은 몫 — 가득 수리비(SUP) ÷ 이 값 × 1,000점(24) */
    val supPerUnit: Double,
    /** 한 번에 넣는 최대 켤레(10) */
    val maxMaterials: Int,
    val fromServer: Boolean = false,
) {
    val specLo: Double get() = spec.getOrElse(0) { 0.7 }
    val specHi: Double get() = spec.getOrElse(1) { 1.3 }

    companion object {
        /** 서버 0118 의 기본값 */
        val FALLBACK = ForgeXpRule(
            ruleVersion = "forge-xp-1",
            step = 3.1,
            need = 2.88,
            spec = listOf(0.7, 1.3),
            critChance = 0.10,
            critMult = 2.0,
            inherit = 0.5,
            supPerUnit = 24.0,
            maxMaterials = 10,
        )
    }
}

/** 하루 러닝 목표의 범위(서버 0118 `shoe_rules_v4().goal` — 4,000 ~ 20,000 · 500 단위, 새 계정 6,000) */
data class GoalRule(
    val min: Int,
    val max: Int,
    val default: Int,
    val step: Int,
    val fromServer: Boolean = false,
) {
    /** 서버 `profile_set_daily_goal` 처럼 범위 안 · 단위에 맞춘다(가까운 단위로 반올림) — 서버가 돌려준 값이 정본이다 */
    fun fit(goal: Int): Int {
        val unit = step.coerceAtLeast(1)
        val rounded = Math.round(goal / unit.toDouble()).toInt() * unit
        return rounded.coerceIn(min, max.coerceAtLeast(min))
    }

    companion object {
        /** 서버 0118 의 기본값 */
        val FALLBACK = GoalRule(min = 4000, max = 20000, default = 6000, step = 500)
    }
}

/**
 * 능력치 규칙(서버 0119 `shoe_rules_v4().stats` — docs/economy-roots-2026-11-21.md). 효율 = 버는 속도(그만큼 신발 · 에너지를 빨리 쓴다) ·
 * 내구도 = 한 켤레로 버는 양(가득 수리비는 늘지 않는다) · 착화감 = 하루에 버는 양. 미리 보일 때만 쓰고 정산은 늘 서버다.
 * 묶음이 없는 서버(0119 전)는 [LEGACY] — 닳은 뒤 적립 없음 · 효율이 수명 / 에너지를 건드리지 않음 · 레벨마다 에너지 +2.
 */
data class StatRule(
    /** 신발이 다 닳은 뒤 걸음의 적립 비율(0.30). 0 이면 예전처럼 적립 없음 */
    val wornRate: Double,
    /** 효율 포인트 배수가 닳는 속도를 올리는 세기(0 ~ 1) — 분당 닳음 = [wearPerMinute] × 배수^세기 */
    val effWearExp: Double,
    /** 효율 포인트 배수가 걸음당 에너지를 올리는 세기(0 ~ 1) */
    val effEnergyExp: Double,
    /** 내구도 포인트가 가득 수리비를 올리지 않는 세기(1 = 가득 수리비는 뽑은 내구도가 정한다) */
    val durRepairExp: Double,
    /** 포인트 없는 신발이 1분에 닳는 양(5) */
    val wearPerMinute: Int,
    /** 하루 에너지 = [energyBase] + (가진 신발 중 최고 레벨 − 1) × [energyPerLevel] */
    val energyBase: Double,
    val energyPerLevel: Double,
    /** 에너지 1 로 적립하는 걸음(600 — 착화감 · 효율 포인트 전) */
    val stepsPerEnergy: Int,
    /** 이 레벨마다 포인트를 한 번 다시 나눌 수 있다(5, 0 이면 없음) */
    val respecEvery: Int,
    val fromServer: Boolean = false,
) {
    /** 다 닳은 신발로도 적립하고 걸음을 세는가 */
    val earnsWhenWorn: Boolean get() = wornRate > 0

    /** 포인트 배수 — 1 + 등급의 포인트당 비율 × 포인트(효율 · 내구도 같은 식) */
    fun pointMult(pointRateBps: Int, points: Int): Double = 1 + pointRateBps * points.coerceAtLeast(0) / 10000.0

    /** 효율 포인트를 넣은 신발이 닳는 속도의 배수(서버 `economy.wear_mult`) */
    fun wearMult(pointRateBps: Int, effPoints: Int): Double = pointMult(pointRateBps, effPoints).pow(effWearExp.coerceIn(0.0, 1.0))

    /** 효율 포인트를 넣은 신발이 에너지를 쓰는 속도의 배수(서버 `economy.energy_mult`) */
    fun energyMult(pointRateBps: Int, effPoints: Int): Double = pointMult(pointRateBps, effPoints).pow(effEnergyExp.coerceIn(0.0, 1.0))

    /** 내구도 [durability] 로 달릴 수 있는 초 — 서버 정산과 같은 식(내구도 × 12초 ÷ 닳는 배수) */
    fun lifeSeconds(durability: Int, pointRateBps: Int, effPoints: Int): Double =
        durability.coerceAtLeast(0) * 60.0 / wearPerMinute.coerceAtLeast(1) / wearMult(pointRateBps, effPoints)

    fun lifeMinutes(durability: Int, pointRateBps: Int, effPoints: Int): Double = lifeSeconds(durability, pointRateBps, effPoints) / 60.0

    /** 달린 [seconds] 초 동안 닳는 내구도(운 전 · 버림) — 서버 `durability_wear_time` × 닳는 배수 */
    fun wearFor(seconds: Long, pointRateBps: Int, effPoints: Int): Int =
        (wearPerMinute * (seconds.coerceAtLeast(0) / 60) * wearMult(pointRateBps, effPoints)).roundToInt()

    /** 하루 에너지 최대치 — 서버 `economy.energy_max_for_level` */
    fun energyMax(topLevel: Int): Double = energyBase + (topLevel.coerceAtLeast(1) - 1) * energyPerLevel.coerceAtLeast(0.0)

    companion object {
        /** 서버 0119 의 기본값 */
        val FALLBACK = StatRule(
            wornRate = 0.30, effWearExp = 1.0, effEnergyExp = 1.0, durRepairExp = 1.0,
            wearPerMinute = 5, energyBase = 10.0, energyPerLevel = 0.0, stepsPerEnergy = 600, respecEvery = 5,
        )

        /** 0119 전 서버의 규칙 — `stats` 묶음이 없을 때 */
        val LEGACY = StatRule(
            wornRate = 0.0, effWearExp = 0.0, effEnergyExp = 0.0, durRepairExp = 0.0,
            wearPerMinute = 5, energyBase = 10.0, energyPerLevel = 2.0, stepsPerEnergy = 600, respecEvery = 0,
        )
    }
}

/**
 * 함께 달린 사람의 문턱(서버 0120 `shoe_rules_v4().together` — docs/together-bonus-fix-spec-2026-11-21.md 2 · 3장).
 * [ACTIVE] 면 크루 · 번개 방의 인원 보너스는 그 방에서 [minMinutes] 분 · 경로로 확인된 걸음 [minSteps] 이상 달리고 방의 목표 거리
 * (크루는 고른 코스 · 번개는 글의 거리)의 [goalShare] 이상을 채운 사람만 세고, 함께 달린 사람이 러닝을 올릴 때마다 출발 뒤 [topupHours]
 * 시간 안에 이미 올린 사람의 보너스를 더한다. 화면은 이 값으로 안내 한 줄만 쓰고, 인원 · 금액은 늘 서버 값이다.
 * 묶음이 없는 서버(0120 전)는 [LEGACY] — 함께 출발한 사람 모두를 출발 2분 뒤에 센다.
 */
data class TogetherRule(
    /** ACTIVE(함께 달린 사람만 · 올릴 때마다 더함) · LEGACY(함께 출발한 모두 · 2분 판정) */
    val rule: String,
    val minMinutes: Int,
    val minSteps: Int,
    /** 방의 목표 거리 가운데 채워야 하는 몫(0.5 = 절반). 목표가 없는 방은 분 · 걸음만 본다 */
    val goalShare: Double,
    val topupHours: Int,
    val fromServer: Boolean = false,
) {
    val active: Boolean get() = rule == ACTIVE

    /** 목표 거리 [goalKm] 에서 채워야 하는 거리(km) — 목표가 없으면(0 이하) null */
    fun goalKmOf(goalKm: Double?): Double? =
        goalKm?.takeIf { it > 0.0 && goalShare > 0.0 }?.let { it * goalShare }

    /** 출발 [startedAtMs] 뒤 아직 보너스가 더해질 수 있는가 — [nowMs] 가 더하는 시간 안 */
    fun topupOpen(startedAtMs: Long, nowMs: Long): Boolean =
        active && startedAtMs > 0 && nowMs - startedAtMs < topupHours.coerceAtLeast(0) * 3_600_000L

    companion object {
        const val ACTIVE = "ACTIVE"

        /** 서버 0120 의 기본값 */
        val FALLBACK = TogetherRule(
            rule = "ACTIVE", minMinutes = 10, minSteps = 1500, goalShare = 0.5, topupHours = 24,
        )

        /** 0120 전 서버 — `together` 묶음이 없을 때 */
        val LEGACY = TogetherRule(
            rule = "LEGACY", minMinutes = 0, minSteps = 0, goalShare = 0.0, topupHours = 0,
        )
    }
}

/**
 * 하루 최대 SUP(서버 0120 `shoe_rules_v4().daily_max` — docs/together-bonus-fix-spec-2026-11-21.md 14장). [HOUR] 면 하루에 받는 SUP
 * (혼자 · 번개 · 크루 · 함께 달린 보너스 · 목표 보너스 모두)의 합은 그 러닝 신발로 1시간([hourSteps] 걸음) 달린 값 × [hourMult] × 적립 배수
 * (이벤트 · 2배권)까지 — 가입한 지 얼마 안 된 계정은 절반. 신발 화면은 "하루 최대 약 N SUP" 한 줄만 쓰고, 정산은 늘 서버다.
 * 묶음이 없는 서버(0120 전)는 [LEGACY] — 예전 하루 금액 상한(개인 러닝만).
 */
data class DailyMaxRule(
    /** HOUR(신발 1시간 값 × 배) · CAP(예전 하루 금액 상한) */
    val rule: String,
    val hourSteps: Int,
    val hourMult: Double,
    /** 목표 보너스도 하루 최대 안에 드는가 */
    val goalIncluded: Boolean,
    val fromServer: Boolean = false,
) {
    val hour: Boolean get() = rule == HOUR

    companion object {
        const val HOUR = "HOUR"
        const val CAP = "CAP"

        /** 서버 0120 의 기본값 */
        val FALLBACK = DailyMaxRule(
            rule = "HOUR", hourSteps = 9900, hourMult = 4.0, goalIncluded = true,
        )

        /** 0120 전 서버 — `daily_max` 묶음이 없을 때 */
        val LEGACY = DailyMaxRule(
            rule = "CAP", hourSteps = 0, hourMult = 0.0, goalIncluded = false,
        )
    }
}

data class ShoeRules(
    val pointsPerStep: Double,
    val repairShare: Double,
    val rewardLuck: List<Double>?,
    val wearLuck: List<Double>?,
    val comfortCapBps: Int,
    val freeRepairHours: Double,
    /** 지금 무료 수리를 쓸 수 있는가(로그인했을 때만 — 모르면 null) */
    val freeRepairReady: Boolean?,
    /** 다음 무료 수리 시각(벽시계 ms, 모르면 null) */
    val freeRepairNextMs: Long?,
    val forgeMaterialMinDurability: Double,
    val boostShopEnabled: Boolean,
    val grades: List<GradeRule>,
    /** 강화 경험치(서버 0118). 서버에 0118 이 없으면 [ForgeXpRule.FALLBACK](fromServer = false) */
    val forge: ForgeXpRule = ForgeXpRule.FALLBACK,
    /** 하루 러닝 목표의 범위(서버 0118). 서버에 0118 이 없으면 [GoalRule.FALLBACK](fromServer = false) */
    val goal: GoalRule = GoalRule.FALLBACK,
    /** 능력치 규칙(서버 0119). 서버에 0119 가 없으면 [StatRule.LEGACY] */
    val stats: StatRule = StatRule.FALLBACK,
    /** 함께 달린 사람의 문턱(서버 0120). 서버에 0120 이 없으면 [TogetherRule.LEGACY] */
    val together: TogetherRule = TogetherRule.FALLBACK,
    /** 하루 최대 SUP(서버 0120). 서버에 0120 이 없으면 [DailyMaxRule.LEGACY] */
    val dailyMax: DailyMaxRule = DailyMaxRule.FALLBACK,
    /** 서버에서 받은 값인가(아니면 [FALLBACK]) */
    val fromServer: Boolean = false,
) {
    fun grade(rarity: String?): GradeRule = grades.firstOrNull { it.rarity == rarity } ?: grades.first()
    fun grade(rarity: Rarity): GradeRule = grade(rarity.id)

    /** 그 등급 · 레벨까지 받은 포인트 — 서버 `economy.points_total_of` 와 같다 */
    fun pointsTotal(rarity: String?, level: Int): Int {
        val g = grade(rarity)
        val lv = level.coerceAtLeast(1)
        return g.pointsPerLevel * (lv - 1) + g.pointsMilestone * (lv / 5)
    }

    /** 한 레벨 올리면 받는 포인트(5의 배수 레벨이면 보너스까지) */
    fun pointsAt(rarity: String?, level: Int): Int {
        val g = grade(rarity)
        if (level <= 1) return 0
        return g.pointsPerLevel + if (level % 5 == 0) g.pointsMilestone else 0
    }

    /** 포인트를 더한 효율(적립 +bps) — 서버 `shoe_effective` */
    fun efficiencyWithPoints(baseBps: Int, rarity: String?, points: Int): Int =
        ((10000 + baseBps) * (1 + grade(rarity).pointRateBps * points / 10000.0)).roundToInt() - 10000

    /** 포인트를 더한 착화감(에너지 절약 bps, 상한 전) — 서버 `comfort_with_points` */
    fun comfortWithPoints(baseBps: Int, rarity: String?, points: Int): Int =
        10000 - ((10000 - baseBps) / (1 + grade(rarity).pointRateBps * points / 10000.0)).roundToInt()

    /** 포인트를 더한 최대 내구도 — 서버 `dur_max_of` */
    fun durabilityWithPoints(base: Int, rarity: String?, points: Int): Int =
        base + (base * grade(rarity).pointRateBps * points / 10000.0).roundToInt()

    /** 내구도 1(12초)을 고치는 값 — 그 신발로 걸을 때(분당 110걸음) 버는 돈 × [repairShare] ÷ 5 */
    fun repairUnit(efficiencyBps: Int): Double = repairShare * 110 * pointsPerStep / 5 * (10000 + efficiencyBps) / 10000.0

    /**
     * 포인트를 넣은 신발의 내구도 1점 수리비 — 서버 `economy.repair_unit_of`(0119). [efficiencyBps] 는 포인트까지 든 효율.
     * 효율 포인트는 닳는 속도만큼, 내구도 포인트는 늘어난 내구도만큼 1점 값을 낮춘다 — 가득 수리비는 포인트와 상관없다.
     */
    fun repairUnit(efficiencyBps: Int, rarity: String?, effPoints: Int, durPoints: Int): Double {
        val rate = grade(rarity).pointRateBps
        return repairUnit(efficiencyBps) / stats.wearMult(rate, effPoints) /
            stats.pointMult(rate, durPoints).pow(stats.durRepairExp.coerceIn(0.0, 1.0))
    }

    /** 그 신발이 가득에서 달릴 수 있는 분 — 최대 내구도 ÷ (분당 닳음 × 효율 포인트 배수) */
    fun lifeMinutes(durability: Int, rarity: String?, effPoints: Int): Double =
        stats.lifeMinutes(durability, grade(rarity).pointRateBps, effPoints)

    /**
     * 그 신발로 하루에 받을 수 있는 최대 SUP(적립 배수 1 · 가입한 지 오래된 계정) — 서버 `economy.daily_max_of` 와 같은 식:
     * [DailyMaxRule.hourMult] × [DailyMaxRule.hourSteps] × [pointsPerStep] × (1 + [efficiencyBps] ÷ 10000). [efficiencyBps] 는 포인트까지 든 효율.
     * 하루 최대 규칙이 아니면(예전 하루 금액 상한 — CAP) null
     */
    fun dailyMaxSup(efficiencyBps: Int): Double? {
        if (!dailyMax.hour) return null
        val v = dailyMax.hourMult.coerceAtLeast(0.0) * dailyMax.hourSteps.coerceAtLeast(0) * pointsPerStep *
            (10000 + efficiencyBps) / 10000.0
        // 서버도 소수 넷째 자리에서 반올림한다
        return Math.round(v * 10000) / 10000.0
    }

    companion object {
        /** 서버 0116 · 0118 과 같은 값 — 서버를 아직 못 읽었을 때 */
        val FALLBACK = ShoeRules(
            pointsPerStep = 0.0118,
            repairShare = 0.45,
            rewardLuck = listOf(0.7, 1.0, 1.3),
            wearLuck = listOf(0.667, 0.889, 1.444),
            comfortCapBps = 7000,
            freeRepairHours = 48.0,
            freeRepairReady = null,
            freeRepairNextMs = null,
            // 예전 강화 길의 재료는 내구도 가득만(0118 — 50 → 100)
            forgeMaterialMinDurability = 100.0,
            boostShopEnabled = false,
            grades = listOf(
                GradeRule("COMMON", 0, 8000, 0, 4444, 110, 200, 2, 3, 100, 20),
                GradeRule("RARE", 5000, 17000, 667, 4815, 150, 265, 3, 5, 110, 20),
                GradeRule("EPIC", 12500, 30500, 1250, 5139, 200, 360, 4, 6, 125, 26),
                GradeRule("LEGENDARY", 23750, 50750, 1765, 5425, 270, 485, 5, 8, 140, 27),
                GradeRule("REDLINE", 40625, 81125, 2222, 5679, 365, 660, 6, 9, 160, 28),
                GradeRule("FINISH", 65938, 126688, 2632, 5906, 495, 890, 8, 12, 180, 30),
            ),
            forge = ForgeXpRule.FALLBACK,
            goal = GoalRule.FALLBACK,
            stats = StatRule.FALLBACK,
        )

        fun parse(body: String): ShoeRules? = runCatching {
            val o = Json.parseToJsonElement(body) as JsonObject
            val grades = (o["grades"] as? JsonArray)?.mapNotNull { e ->
                val g = e as? JsonObject ?: return@mapNotNull null
                val eff = g.ints("efficiency"); val cmf = g.ints("comfort"); val dur = g.ints("durability")
                GradeRule(
                    rarity = g.str("rarity") ?: return@mapNotNull null,
                    efficiencyLo = eff.getOrNull(0) ?: return@mapNotNull null, efficiencyHi = eff.getOrNull(1) ?: return@mapNotNull null,
                    comfortLo = cmf.getOrNull(0) ?: 0, comfortHi = cmf.getOrNull(1) ?: 0,
                    durabilityLo = dur.getOrNull(0) ?: 0, durabilityHi = dur.getOrNull(1) ?: 0,
                    pointsPerLevel = g.int("points_per_level") ?: 0,
                    pointsMilestone = g.int("points_milestone") ?: 0,
                    pointRateBps = g.int("point_rate_bps") ?: 0,
                    maxLevel = g.int("max_level") ?: 20,
                )
            }.orEmpty()
            if (grades.size < 6) return@runCatching null
            ShoeRules(
                pointsPerStep = o.dbl("points_per_step") ?: FALLBACK.pointsPerStep,
                repairShare = o.dbl("repair_share") ?: FALLBACK.repairShare,
                rewardLuck = o.dbls("reward_luck"),
                wearLuck = o.dbls("wear_luck"),
                comfortCapBps = o.int("comfort_cap_bps") ?: FALLBACK.comfortCapBps,
                freeRepairHours = o.dbl("free_repair_hours") ?: 0.0,
                freeRepairReady = (o["free_repair_ready"] as? JsonPrimitive)?.booleanOrNull,
                freeRepairNextMs = o.str("free_repair_next")?.let(::isoMillis),
                forgeMaterialMinDurability = o.dbl("forge_material_min_durability") ?: 0.0,
                boostShopEnabled = (o["boost_shop_enabled"] as? JsonPrimitive)?.booleanOrNull ?: false,
                grades = grades,
                forge = (o["forge"] as? JsonObject)?.let(::parseForge) ?: FALLBACK.forge,
                goal = (o["goal"] as? JsonObject)?.let(::parseGoal) ?: FALLBACK.goal,
                // 묶음이 없으면 0119 전 서버다 — 예전 규칙으로 보인다(닳은 뒤 적립 없음 · 레벨마다 에너지 +2)
                stats = (o["stats"] as? JsonObject)?.let(::parseStats) ?: StatRule.LEGACY,
                // 묶음이 없으면 0120 전 서버다 — 함께 출발한 모두 · 2분 판정, 예전 하루 금액 상한
                together = (o["together"] as? JsonObject)?.let(::parseTogether) ?: TogetherRule.LEGACY,
                dailyMax = (o["daily_max"] as? JsonObject)?.let(::parseDailyMax) ?: DailyMaxRule.LEGACY,
                fromServer = true,
            )
        }.getOrNull()

        /** `together` 묶음(0120) — 빠진 칸은 기본값. 규칙 글자는 대문자로(ACTIVE 가 아니면 예전 규칙으로 본다) */
        private fun parseTogether(t: JsonObject): TogetherRule {
            val d = TogetherRule.FALLBACK
            return TogetherRule(
                rule = t.str("rule")?.trim()?.uppercase()?.takeIf { it.isNotEmpty() } ?: d.rule,
                minMinutes = (t.int("min_minutes") ?: d.minMinutes).coerceAtLeast(0),
                minSteps = (t.int("min_steps") ?: d.minSteps).coerceAtLeast(0),
                goalShare = (t.dbl("goal_share") ?: d.goalShare).coerceIn(0.0, 1.0),
                topupHours = (t.int("topup_hours") ?: d.topupHours).coerceAtLeast(0),
                fromServer = true,
            )
        }

        /** `daily_max` 묶음(0120) — 빠진 칸은 기본값. 규칙 글자는 대문자로(HOUR 가 아니면 예전 하루 금액 상한으로 본다) */
        private fun parseDailyMax(m: JsonObject): DailyMaxRule {
            val d = DailyMaxRule.FALLBACK
            return DailyMaxRule(
                rule = m.str("rule")?.trim()?.uppercase()?.takeIf { it.isNotEmpty() } ?: d.rule,
                hourSteps = (m.int("hour_steps") ?: d.hourSteps).coerceAtLeast(0),
                hourMult = (m.dbl("hour_mult") ?: d.hourMult).coerceAtLeast(0.0),
                goalIncluded = (m["goal_included"] as? JsonPrimitive)?.booleanOrNull ?: d.goalIncluded,
                fromServer = true,
            )
        }

        /** `forge` 묶음(0118) — 빠진 칸은 기본값. 묶음이 없는 서버(0118 전)는 부르지 않는다([FALLBACK] 그대로 · fromServer = false) */
        private fun parseForge(f: JsonObject): ForgeXpRule {
            val d = ForgeXpRule.FALLBACK
            val spec = (f["spec"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.doubleOrNull }?.takeIf { it.size == 2 }
            return ForgeXpRule(
                ruleVersion = f.str("rule_version") ?: d.ruleVersion,
                step = f.dbl("step") ?: d.step,
                need = f.dbl("need") ?: d.need,
                spec = spec ?: d.spec,
                critChance = f.dbl("crit_chance") ?: d.critChance,
                critMult = f.dbl("crit_mult") ?: d.critMult,
                inherit = f.dbl("inherit") ?: d.inherit,
                supPerUnit = f.dbl("sup_per_unit") ?: d.supPerUnit,
                maxMaterials = f.int("max_materials") ?: d.maxMaterials,
                fromServer = true,
            )
        }

        /** `stats` 묶음(0119) — 빠진 칸은 기본값 */
        private fun parseStats(t: JsonObject): StatRule {
            val d = StatRule.FALLBACK
            return StatRule(
                wornRate = (t.dbl("worn_rate") ?: d.wornRate).coerceIn(0.0, 0.9),
                effWearExp = (t.dbl("eff_wear_exp") ?: d.effWearExp).coerceIn(0.0, 1.0),
                effEnergyExp = (t.dbl("eff_energy_exp") ?: d.effEnergyExp).coerceIn(0.0, 1.0),
                durRepairExp = (t.dbl("dur_repair_exp") ?: d.durRepairExp).coerceIn(0.0, 1.0),
                wearPerMinute = (t.int("wear_per_minute") ?: d.wearPerMinute).coerceAtLeast(1),
                energyBase = t.dbl("energy_base") ?: d.energyBase,
                energyPerLevel = (t.dbl("energy_per_level") ?: d.energyPerLevel).coerceAtLeast(0.0),
                stepsPerEnergy = (t.int("steps_per_energy") ?: d.stepsPerEnergy).coerceAtLeast(1),
                respecEvery = (t.int("respec_every") ?: d.respecEvery).coerceAtLeast(0),
                fromServer = true,
            )
        }

        /** `goal` 묶음(0118) — 범위가 뒤집혔으면 버린다(기본값으로) */
        private fun parseGoal(g: JsonObject): GoalRule? {
            val d = GoalRule.FALLBACK
            val min = g.int("min") ?: d.min
            val max = g.int("max") ?: d.max
            if (min <= 0 || max < min) return null
            return GoalRule(
                min = min,
                max = max,
                default = (g.int("default") ?: d.default).coerceIn(min, max),
                step = (g.int("step") ?: d.step).coerceAtLeast(1),
                fromServer = true,
            )
        }
    }
}

/** 신발 능력치를 지수 · 분으로 — 화면에 보이는 숫자 */
object ShoeIndex {
    fun efficiency(bps: Int): Double = 100 + bps / 100.0
    fun comfort(bps: Int): Double = 100.0 / (1 - bps.coerceIn(0, 9999) / 10000.0)
    fun minutes(durability: Int): Double = durability / 5.0

    /** 범위 안의 자리(0 = 바닥, 1 = 꼭대기) — 효율은 bps 그대로, 착화감은 지수로, 내구도는 그대로 */
    fun efficiencyPosition(bps: Int, g: GradeRule): Double =
        ((bps - g.efficiencyLo).toDouble() / (g.efficiencyHi - g.efficiencyLo).coerceAtLeast(1)).coerceIn(0.0, 1.0)

    fun comfortPosition(bps: Int, g: GradeRule): Double {
        val lo = comfort(g.comfortLo)
        return ((comfort(bps) / lo - 1) / 0.8).coerceIn(0.0, 1.0)
    }

    fun durabilityPosition(base: Int, g: GradeRule): Double =
        ((base - g.durabilityLo).toDouble() / (g.durabilityHi - g.durabilityLo).coerceAtLeast(1)).coerceIn(0.0, 1.0)
}

/** 뽑힌 값이 얼마나 좋은가 — 보여 주기만(값은 서버 능력치 그대로) */
enum class ShoeType { EFFICIENCY, COMFORT, ENDURANCE, BALANCED }

data class ShoeQuality(val stars: Int, val type: ShoeType, val position: Double) {
    companion object {
        /**
         * 뽑은 값(포인트 전)의 세 자리 평균으로 별(★ 0.5 아래 · ★★ 0.65 아래 · ★★★) — 뽑기가 가운데를 자주 내므로 ★ 50% · ★★ 39% · ★★★ 10% 쯤.
         * 가장 높게 뽑힌 능력치가 다른 둘보다 0.1 넘게 높으면 그 유형, 아니면 균형형.
         */
        fun of(rules: ShoeRules, rarity: String?, efficiencyBaseBps: Int, comfortBaseBps: Int, durabilityBase: Int): ShoeQuality {
            val g = rules.grade(rarity)
            val pe = ShoeIndex.efficiencyPosition(efficiencyBaseBps, g)
            val pc = ShoeIndex.comfortPosition(comfortBaseBps, g)
            val pd = ShoeIndex.durabilityPosition(durabilityBase, g)
            // 천분의 일로 반올림 — 저장 값(정수 bps)의 반올림 때문에 가운데 신발이 0.49999 로 ★ 이 되지 않게
            val avg = Math.round((pe + pc + pd) / 3 * 1000) / 1000.0
            val stars = when {
                avg < 0.5 -> 1
                avg < 0.65 -> 2
                else -> 3
            }
            val ranked = listOf(ShoeType.EFFICIENCY to pe, ShoeType.COMFORT to pc, ShoeType.ENDURANCE to pd).sortedByDescending { it.second }
            val type = if (ranked[0].second - ranked[1].second > 0.1) ranked[0].first else ShoeType.BALANCED
            return ShoeQuality(stars, type, avg)
        }
    }
}

/** 새 수리(0116) 결과 */
data class RepairV2Result(
    val ok: Boolean,
    /** QUOTE_CHANGED — 화면이 보인 값과 서버 값이 달라 고치지 않았다 */
    val code: String?,
    val durability: Double?,
    val units: Int?,
    val cost: Double?,
    val free: Boolean,
    val balance: Double?,
    val freeNextMs: Long?,
) {
    companion object {
        fun parse(body: String): RepairV2Result? = runCatching {
            val o = Json.parseToJsonElement(body) as JsonObject
            RepairV2Result(
                ok = (o["ok"] as? JsonPrimitive)?.booleanOrNull ?: false,
                code = o.str("code"),
                durability = o.dbl("durability"),
                units = o.int("units"),
                cost = o.dbl("cost"),
                free = (o["free"] as? JsonPrimitive)?.booleanOrNull ?: false,
                balance = o.dbl("balance"),
                freeNextMs = o.str("free_next")?.let(::isoMillis),
            )
        }.getOrNull()
    }
}

/** 상자 현황(0116) — 일반 상자 = 출석 · 러닝 · 사기, 상급 상자 = 주간 · 연속 · 신발 레벨 · 사기 */
data class BoxStatus(
    val dailyFree: Int,
    val stepsToday: Int,
    /** 러닝 상자 — 오늘 적립 걸음 문턱과 받았는지 */
    val runBoxes: List<Pair<Int, Boolean>>,
    val stepsWeek: Int,
    val weekTarget: Int,
    val weekGot: Boolean,
    val streakDays: Int,
    val levelsGot: List<Int>,
    val normalPrice: Double,
    val normalLimit: Int,
    val normalBought: Int,
    val premiumPrice: Double,
    val premiumLimit: Int,
    val premiumBought: Int,
    val freeLeft: Int,
    val giftLeft: Int,
    val balance: Double,
) {
    val normalCanBuy: Boolean get() = normalPrice > 0 && normalBought < normalLimit && balance >= normalPrice
    val premiumCanBuy: Boolean get() = premiumPrice > 0 && premiumBought < premiumLimit && balance >= premiumPrice

    companion object {
        fun parse(body: String): BoxStatus? = runCatching {
            val o = Json.parseToJsonElement(body) as JsonObject
            BoxStatus(
                dailyFree = o.int("daily_free") ?: 0,
                stepsToday = o.int("steps_today") ?: 0,
                runBoxes = (o["run_boxes"] as? JsonArray)?.mapNotNull { e ->
                    val b = e as? JsonObject ?: return@mapNotNull null
                    (b.int("steps") ?: return@mapNotNull null) to ((b["got"] as? JsonPrimitive)?.booleanOrNull ?: false)
                }.orEmpty(),
                stepsWeek = o.int("steps_week") ?: 0,
                weekTarget = o.int("week_target") ?: 0,
                weekGot = (o["week_got"] as? JsonPrimitive)?.booleanOrNull ?: false,
                streakDays = o.int("streak_days") ?: 0,
                levelsGot = o.ints("levels_got"),
                normalPrice = o.dbl("normal_price") ?: 0.0,
                normalLimit = o.int("normal_limit") ?: 0,
                normalBought = o.int("normal_bought") ?: 0,
                premiumPrice = o.dbl("premium_price") ?: 0.0,
                premiumLimit = o.int("premium_limit") ?: 0,
                premiumBought = o.int("premium_bought") ?: 0,
                freeLeft = o.int("free_left") ?: 0,
                giftLeft = o.int("gift_left") ?: 0,
                balance = o.dbl("balance") ?: 0.0,
            )
        }.getOrNull()
    }
}

private fun JsonObject.prim(k: String): JsonPrimitive? = (this[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }
private fun JsonObject.str(k: String): String? = prim(k)?.takeIf { it.isString }?.content
private fun JsonObject.int(k: String): Int? = prim(k)?.let { it.intOrNull ?: it.doubleOrNull?.toInt() }
private fun JsonObject.dbl(k: String): Double? = prim(k)?.let { it.doubleOrNull ?: it.content.toDoubleOrNull() }
private fun JsonObject.ints(k: String): List<Int> = (this[k] as? JsonArray)?.mapNotNull { e: JsonElement ->
    (e as? JsonPrimitive)?.let { it.intOrNull ?: it.doubleOrNull?.toInt() ?: it.longOrNull?.toInt() }
}.orEmpty()
private fun JsonObject.dbls(k: String): List<Double>? = (this[k] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.doubleOrNull }
    ?.takeIf { it.size == 3 }

private fun isoMillis(iso: String): Long? = runCatching { java.time.OffsetDateTime.parse(iso.replace(' ', 'T')).toInstant().toEpochMilli() }
    .recoverCatching { java.time.OffsetDateTime.parse(iso.replace(' ', 'T') + ":00").toInstant().toEpochMilli() }
    .getOrNull()
