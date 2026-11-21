package com.stepup.android.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/**
 * 예전 러너 레벨(폰 · 누적 거리) — 서버의 러너 레벨([RunnerLevel], 0121)을 받지 못할 때 내 정보 머리 줄이 쓴다.
 *
 * 러너 레벨 — 앱을 쓰며 실제로 걸은 거리(km)가 곧 경험치다.
 *
 * 신발 NFT의 강화 레벨과는 완전히 별개다. 신발은 사고 팔고 갈아 신지만,
 * 러너 레벨은 그 사람이 쌓아온 거리라서 되돌아가지 않는다.
 *
 * 곡선은 레벨이 오를수록 필요한 거리가 선형으로 늘어난다.
 * Lv.1→2 는 3km, Lv.30→31 은 49km, 만렙(60)까지는 약 2,900km —
 * 하루 5km 페이스로 1년 반쯤 걸리는 분량이다.
 */
data class RunnerProgress(
    val level: Int,
    /** 누적 거리(km) = 총 경험치 */
    val totalKm: Double,
    /** 현재 레벨에서 쌓은 거리(km) */
    val intoLevelKm: Double,
    /** 다음 레벨까지 필요한 거리(km). 만렙이면 0 */
    val levelSpanKm: Double,
) {
    val isMax: Boolean get() = levelSpanKm <= 0.0

    val progress: Float
        get() = if (isMax) 1f else (intoLevelKm / levelSpanKm).coerceIn(0.0, 1.0).toFloat()

    /** 다음 레벨까지 남은 거리(km) */
    val remainingKm: Double get() = if (isMax) 0.0 else (levelSpanKm - intoLevelKm).coerceAtLeast(0.0)
}

object RunnerLevels {

    const val MAX_LEVEL = 60

    /** 레벨 1에서 2로 오르는 데 필요한 거리 */
    private const val BASE_KM = 3.0

    /** 레벨이 하나 오를 때마다 늘어나는 요구 거리 */
    private const val STEP_KM = 1.6

    /** [level]에서 다음 레벨로 가는 데 필요한 거리(km). 만렙이면 0 */
    fun spanKm(level: Int): Double =
        if (level >= MAX_LEVEL) 0.0 else BASE_KM + (level - 1).coerceAtLeast(0) * STEP_KM

    /** [level]에 도달하기까지의 누적 요구 거리(km) */
    fun cumulativeKm(level: Int): Double {
        val n = (level - 1).coerceIn(0, MAX_LEVEL - 1)
        // Σ(BASE + i*STEP), i = 0 until n
        return n * BASE_KM + STEP_KM * n * (n - 1) / 2.0
    }

    /** 누적 거리로부터 현재 레벨과 진행도를 구한다 */
    fun of(totalKm: Double): RunnerProgress {
        val km = totalKm.coerceAtLeast(0.0)
        var level = 1
        while (level < MAX_LEVEL && km >= cumulativeKm(level + 1)) level++
        val floor = cumulativeKm(level)
        return RunnerProgress(
            level = level,
            totalKm = km,
            intoLevelKm = (km - floor).coerceAtLeast(0.0),
            levelSpanKm = spanKm(level),
        )
    }

    /** 걸음 수로부터 바로 구하는 편의 함수 */
    fun ofSteps(steps: Long): RunnerProgress =
        of(steps * RewardEconomy.STRIDE_METERS / 1000)
}

/** 레벨 구간별 러너 칭호 — 표시용 이름은 화면에서 현지화한다 */
enum class RunnerTitle { ROOKIE, STRIDER, TRAILBLAZER, PACESETTER, ELITE, MASTER, LEGEND }

fun runnerTitle(level: Int): RunnerTitle = when {
    level >= RunnerLevels.MAX_LEVEL -> RunnerTitle.LEGEND
    level >= 45 -> RunnerTitle.MASTER
    level >= 30 -> RunnerTitle.ELITE
    level >= 20 -> RunnerTitle.PACESETTER
    level >= 10 -> RunnerTitle.TRAILBLAZER
    level >= 5 -> RunnerTitle.STRIDER
    else -> RunnerTitle.ROOKIE
}

// ── 러너 레벨(계정 레벨, 서버 0121 — 2026-11-21) ─────────────────────

/**
 * 러너 레벨 — 서버 `runner_me()`(docs/runner-level-contract-2026-11-21.md). 러닝으로 걸은 걸음(보상 걸음 — 에너지를 다 쓴 뒤 · 크루 러닝도,
 * 무효 · 과속 · 경로 없는 러닝은 빼고)이 1만 걸음마다 1레벨 오른다. 레벨마다 하루 에너지 +2분 · 일반 상자 1, 5레벨마다 상급 상자 1 더,
 * Lv10 · 20 · 30 배지. **값은 모두 서버가 셈한다** — 앱은 받은 값을 그릴 뿐 레벨 · 상자 · 에너지를 스스로 정하지 않는다.
 *
 * 빠진 칸은 계약의 규칙대로 채운다(만렙 30 · 1만 걸음). 분(에너지)은 모르면 null — 그 줄을 보이지 않는다.
 * [enabled] 가 false 면(서버가 러너 레벨을 껐다 — 예전 에너지) 화면은 러너 레벨 칸을 숨긴다.
 */
data class RunnerLevel(
    val enabled: Boolean,
    val level: Int,
    val maxLevel: Int,
    /** 지금까지 센 러너 걸음 */
    val steps: Long,
    /** 한 레벨에 드는 걸음 */
    val levelSteps: Long,
    /** 이 레벨 · 다음 레벨이 시작되는 누적 걸음 — 만렙이면 [nextAt] 은 null */
    val levelFrom: Long,
    val nextAt: Long?,
    /** 다음 레벨까지 남은 걸음 — 만렙이면 null */
    val toNext: Long?,
    /** 지금 하루 에너지(단위 — `my_economy().energy_max` 와 같은 값) */
    val energyMax: Double?,
    /** 지금 하루 에너지(분, 포인트 없이 달리기 기준) */
    val energyMinutes: Int?,
    /** 레벨만으로 정해지는 분 — [legacyFloor] 면 [energyMinutes] 가 더 크다 */
    val levelEnergyMinutes: Int?,
    /** 다음 레벨의 레벨 에너지(분) — 만렙이면 null */
    val nextEnergyMinutes: Int?,
    /** 하루 에너지가 더 늘지 않는 레벨(Lv21) */
    val energyCapLevel: Int?,
    /** 0121 전부터 있던 계정 — 예전 하루 에너지보다 줄지 않는다 */
    val legacyFloor: Boolean,
    /** 닿은 배지(10 · 20 · 30), 없으면 0 */
    val badge: Int,
    /** 러너 레벨로 지금까지 받은 상자 — 일반(무료 뽑기) · 상급 */
    val freeBoxes: Int,
    val giftBoxes: Int,
    val rewards: RunnerLevelRewards,
) {
    val isMax: Boolean get() = level >= maxLevel || nextAt == null

    /** 이 레벨 안에서 간 만큼(0 ~ 1, 만렙이면 1) */
    val progress: Float
        get() {
            if (isMax) return 1f
            val next = nextAt ?: return 1f
            val span = next - levelFrom
            if (span <= 0L) return 0f
            return ((steps - levelFrom).toDouble() / span).coerceIn(0.0, 1.0).toFloat()
        }

    /** 다음 레벨까지 남은 걸음 — 만렙이면 null */
    val stepsToNext: Long?
        get() = if (isMax) null else (toNext ?: nextAt?.let { it - steps })?.coerceAtLeast(0L)

    /**
     * 다음 레벨의 하루 에너지(분) — **지금보다 늘 때만**. 예전 보장([legacyFloor])이 더 크면 다음 레벨에도 늘지 않으니 null
     * ("+2분"을 말하지 않는다). 만렙 · 모르면 null
     */
    val nextEnergyIfMore: Int?
        get() {
            if (isMax) return null
            val next = nextEnergyMinutes ?: return null
            val now = energyMinutes ?: return null
            return next.takeIf { it > now }
        }

    companion object {
        const val DEFAULT_MAX_LEVEL = 30
        const val DEFAULT_LEVEL_STEPS = 10_000L
        val DEFAULT_BADGE_LEVELS: List<Int> = listOf(10, 20, 30)

        /** [level] 에서 닿은 가장 큰 배지 — 없으면 0 */
        fun badgeOf(level: Int, badgeLevels: List<Int> = DEFAULT_BADGE_LEVELS): Int =
            badgeLevels.filter { it in 1..level }.maxOrNull() ?: 0

        /** 응답 글 → 러너 레벨. JSON 객체가 아니거나 레벨 · 걸음이 둘 다 없으면 null(보일 것이 없다) */
        fun parse(body: String): RunnerLevel? = runCatching {
            val o = Json.parseToJsonElement(body) as? JsonObject ?: return@runCatching null
            val maxLevel = o.int("max_level")?.takeIf { it >= 1 } ?: DEFAULT_MAX_LEVEL
            val levelSteps = o.long("level_steps")?.takeIf { it > 0L } ?: DEFAULT_LEVEL_STEPS
            val stepsRaw = o.long("steps")?.coerceAtLeast(0L)
            val level = (o.int("level") ?: stepsRaw?.let { (1 + it / levelSteps).toInt() } ?: return@runCatching null)
                .coerceIn(1, maxLevel)
            val levelFrom = o.long("level_from")?.coerceAtLeast(0L) ?: ((level - 1) * levelSteps)
            val steps = stepsRaw ?: levelFrom
            // 만렙이면 서버가 null 을 보낸다 — 칸이 아예 없을 때만 규칙대로 채운다
            val nextAt = if ("next_at" in o) o.long("next_at") else if (level < maxLevel) level * levelSteps else null
            val toNext = if ("to_next" in o) o.long("to_next")?.coerceAtLeast(0L) else nextAt?.let { (it - steps).coerceAtLeast(0L) }
            val rewards = o.obj("rewards")
            val badgeLevels = (rewards?.get("badge_levels") as? JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p !is JsonNull }?.intOrNull?.takeIf { n -> n > 0 } }
                ?.takeIf { it.isNotEmpty() }
                ?: DEFAULT_BADGE_LEVELS
            val boxes = o.obj("boxes")
            RunnerLevel(
                enabled = o.bool("enabled") ?: true,
                level = level,
                maxLevel = maxLevel,
                steps = steps,
                levelSteps = levelSteps,
                levelFrom = levelFrom,
                nextAt = nextAt,
                toNext = toNext,
                energyMax = o.dbl("energy_max")?.takeIf { it >= 0.0 },
                energyMinutes = o.int("energy_minutes")?.takeIf { it >= 0 },
                levelEnergyMinutes = o.int("level_energy_minutes")?.takeIf { it >= 0 },
                nextEnergyMinutes = o.int("next_energy_minutes")?.takeIf { it >= 0 },
                energyCapLevel = o.int("energy_cap_level")?.takeIf { it >= 1 },
                legacyFloor = o.bool("legacy_floor") ?: false,
                badge = o.int("badge")?.takeIf { it >= 0 } ?: badgeOf(level, badgeLevels),
                freeBoxes = (boxes?.int("free") ?: 0).coerceAtLeast(0),
                giftBoxes = (boxes?.int("gift") ?: 0).coerceAtLeast(0),
                rewards = RunnerLevelRewards(
                    energyMinutesPerLevel = rewards?.int("energy_minutes_per_level")?.takeIf { it > 0 },
                    freeEveryLevel = rewards?.int("free_every_level")?.takeIf { it > 0 },
                    giftEvery = rewards?.int("gift_every")?.takeIf { it > 0 },
                    badgeLevels = badgeLevels,
                ),
            )
        }.getOrNull()
    }
}

/** 레벨이 주는 것(서버 `rewards`) — 모르는 칸은 null(설명 시트가 그 줄을 두지 않는다) */
data class RunnerLevelRewards(
    /** 레벨마다 늘어나는 하루 에너지(분) */
    val energyMinutesPerLevel: Int?,
    /** 레벨마다 일반 상자 수 */
    val freeEveryLevel: Int?,
    /** 몇 레벨마다 상급 상자 1개 더 */
    val giftEvery: Int?,
    val badgeLevels: List<Int>,
)

/** 크루 명단 · 대기실 이름 옆 배지 — 서버 `runner_badges(p_users)` */
object RunnerBadges {
    /** 한 번에 묻는 사람 수(서버 상한) */
    const val MAX_PER_CALL = 200

    private val UUID = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    /** 서버 사용자 id 모양(uuid)인가 — 아니면 묻지 않는다(하나라도 틀리면 서버가 묶음째 거절한다) */
    fun isUserId(id: String): Boolean = UUID.matches(id)

    /** 응답(줄 배열) → 사용자 id → 배지. 배지가 0 인 줄은 뺀다. 배열이 아니면 null */
    fun parse(body: String): Map<String, Int>? = runCatching {
        val rows = Json.parseToJsonElement(body) as? JsonArray ?: return@runCatching null
        rows.mapNotNull { row ->
            val o = row as? JsonObject ?: return@mapNotNull null
            val id = o.str("user_id")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val badge = o.int("badge")?.takeIf { it >= 0 } ?: o.int("level")?.let { RunnerLevel.badgeOf(it) } ?: 0
            if (badge > 0) id to badge else null
        }.toMap()
    }.getOrNull()
}

/** 이 폰이 이 계정에 마지막으로 알린 러너 레벨 — 레벨 · 받은 상자 · 하루 에너지(분) */
data class RunnerSeen(val level: Int, val freeBoxes: Int, val giftBoxes: Int, val energyMinutes: Int?) {
    fun encode(): String = "$level,$freeBoxes,$giftBoxes,${energyMinutes ?: ""}"

    companion object {
        fun of(now: RunnerLevel) = RunnerSeen(now.level, now.freeBoxes, now.giftBoxes, now.energyMinutes)

        /** 폰에 남긴 글 → 값. 없거나 틀린 글이면 null(처음 보는 계정으로) */
        fun decode(raw: String?): RunnerSeen? {
            val parts = raw?.split(',') ?: return null
            if (parts.size != 4) return null
            val level = parts[0].toIntOrNull()?.takeIf { it >= 1 } ?: return null
            val free = parts[1].toIntOrNull()?.takeIf { it >= 0 } ?: return null
            val gift = parts[2].toIntOrNull()?.takeIf { it >= 0 } ?: return null
            return RunnerSeen(level, free, gift, parts[3].toIntOrNull())
        }
    }
}

/** 러너 레벨이 올랐다는 알림 한 번 — 사람이 닫으면 [snapshot] 을 본 것으로 적는다 */
sealed interface RunnerLevelNotice {
    val snapshot: RunnerSeen

    /**
     * 이 폰에서 이 계정의 러너 레벨을 처음 봤다(새 판 · 0121 을 올린 뒤) — 지난 러닝으로 채운 레벨과 그 상자를 한 번에 알린다.
     * 상자 수는 서버의 `boxes`(러너 레벨로 지금까지 받은 것)
     */
    data class Welcome(val level: Int, val freeBoxes: Int, val giftBoxes: Int, override val snapshot: RunnerSeen) : RunnerLevelNotice

    /**
     * 지난번 본 레벨보다 올랐다. 상자는 서버 `boxes` 가 늘어난 만큼(받은 것만 — 지어내지 않는다).
     * 하루 에너지는 실제로 늘었을 때만 [energyFrom] · [energyTo](예전 보장이 더 크면 늘지 않는다), [badge] 는 이번에 새로 닿은 배지
     */
    data class LevelUp(
        val from: Int,
        val to: Int,
        val energyFrom: Int?,
        val energyTo: Int?,
        val freeBoxes: Int,
        val giftBoxes: Int,
        val badge: Int?,
        override val snapshot: RunnerSeen,
    ) : RunnerLevelNotice
}

object RunnerLevelNotices {
    /**
     * 지난번 본 값([seen] — 이 폰에서 처음이면 null)과 지금 값으로 알릴 것을 고른다. 알릴 것이 없으면 null.
     * 처음 보는 계정은 Lv1 · 상자 0 이면(갓 시작한 사람) 알리지 않는다.
     */
    fun of(seen: RunnerSeen?, now: RunnerLevel): RunnerLevelNotice? {
        if (!now.enabled) return null
        val snapshot = RunnerSeen.of(now)
        if (seen == null) {
            if (now.level <= 1 && now.freeBoxes == 0 && now.giftBoxes == 0) return null
            return RunnerLevelNotice.Welcome(now.level, now.freeBoxes, now.giftBoxes, snapshot)
        }
        if (now.level <= seen.level) return null
        val energyUp = seen.energyMinutes != null && now.energyMinutes != null && now.energyMinutes > seen.energyMinutes
        val badgeBefore = RunnerLevel.badgeOf(seen.level, now.rewards.badgeLevels)
        return RunnerLevelNotice.LevelUp(
            from = seen.level,
            to = now.level,
            energyFrom = seen.energyMinutes.takeIf { energyUp },
            energyTo = now.energyMinutes.takeIf { energyUp },
            freeBoxes = (now.freeBoxes - seen.freeBoxes).coerceAtLeast(0),
            giftBoxes = (now.giftBoxes - seen.giftBoxes).coerceAtLeast(0),
            badge = now.badge.takeIf { it > 0 && it > badgeBefore },
            snapshot = snapshot,
        )
    }
}

private fun JsonObject.prim(k: String): JsonPrimitive? = (this[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }
private fun JsonObject.obj(k: String): JsonObject? = this[k] as? JsonObject
private fun JsonObject.str(k: String): String? = prim(k)?.takeIf { it.isString }?.content
private fun JsonObject.bool(k: String): Boolean? = prim(k)?.booleanOrNull
private fun JsonObject.dbl(k: String): Double? = prim(k)?.let { it.doubleOrNull ?: it.content.toDoubleOrNull() }?.takeIf { it.isFinite() }
private fun JsonObject.int(k: String): Int? =
    prim(k)?.let { it.intOrNull ?: it.doubleOrNull?.takeIf { d -> d.isFinite() }?.toInt() }
private fun JsonObject.long(k: String): Long? =
    prim(k)?.let { it.longOrNull ?: it.doubleOrNull?.takeIf { d -> d.isFinite() }?.toLong() }
