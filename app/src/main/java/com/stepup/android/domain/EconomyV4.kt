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
                fromServer = true,
            )
        }.getOrNull()

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
