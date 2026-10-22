package com.stepup.android.domain

import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.max

/*
 * 크루 러닝 3판(서버 0070, 2026-10-21 사용자 결정) — 함께 쓰는 크루 체력 · 하루 한 번 · 곁 500m · 인원 보너스 · 크루 레벨 1 ~ 20.
 *
 * - 크루 체력(그 무리) = 무리의 크루원이 출발 전에 고른 신발의 최대 내구도 합 × (1 + 6% × (크루 레벨 − 1)).
 *   한 사람이 달린 1분마다 5 씩 쓴다. 몫은 똑같이 — 한 사람이 달릴 수 있는 시간 = 크루 체력 ÷ 인원 × 12초.
 *   다 쓰면 그 뒤 걸음은 적립하지 않는다(기록은 계속). 수리 없음 — 다음 날(한국 시각 00:00) 다시.
 * - 크루 러닝은 개인 에너지 · 개인 신발 내구도를 쓰지 않는다 — 하루에 자유 러닝 + 크루 러닝이 모두 된다.
 * - 보상 = 고른 신발의 효율(평균 내지 않는다) × (1 + 인원 보너스 + 레벨 보너스). 인원 · 레벨 보너스는 더한다(겹쳐 곱하지 않는다).
 * - 한 사람 하루 한 번. 출발 2분 뒤에도 서로 500m 안으로 이어진 크루원이 2명 이상이어야 크루 러닝이다. 혼자 남으면 60초 뒤 끝.
 *
 * 앱은 서버(crew_run_today · crew_run_room · crew_run_result)가 준 값을 보인다. 아래 셈은 달리는 중 예상 · 미리보기에만 쓴다 —
 * 서버 함수(economy.crew_headcount_bonus 등)와 같은 값이다(CrewRunPoolTest).
 */

/** 인원 보너스 한 줄 — n명이면 bonus(비율) */
data class CrewHeadcountStep(val n: Int, val bonus: Double)

/** 크루 레벨 한 줄 — 그 레벨이 되는 크루 러닝 누적 거리 · 크루 체력 보너스 · 보상 보너스(비율) */
data class CrewLevelStep(val level: Int, val km: Double, val stamina: Double = CrewRunMath.levelStamina(level), val reward: Double = CrewRunMath.levelReward(level))

/** 크루 러닝 규칙 — 서버가 준 표(없으면 앱의 같은 값) */
data class CrewRunRules(
    val roomMax: Int = CrewRunMath.ROOM_MAX,
    val nearM: Int = CrewRunMath.NEAR_M,
    val confirmSec: Int = CrewRunMath.CONFIRM_SEC,
    val aloneGraceSec: Int = CrewRunMath.ALONE_GRACE_SEC,
    val drainPerMin: Double = CrewRunMath.DRAIN_PER_MIN,
    val headcount: List<CrewHeadcountStep> = CrewRunMath.headcountTable(),
    val levels: List<CrewLevelStep> = CrewRunMath.levelTable(),
) {
    fun headcountBonus(n: Int): Double =
        headcount.lastOrNull { it.n <= n }?.bonus ?: CrewRunMath.headcountBonus(n)
}

/** 내 그날 크루 러닝 한 줄 — 서버가 기록한 값 */
data class CrewRunMine(
    val day: LocalDate,
    val crewId: String?,
    val sessionId: Long?,
    /** 함께 달린 크루원 수(나 포함) */
    val runners: Int,
    val crewLevel: Int,
    /** 무리의 크루 체력 · 한 사람 몫 · 몫으로 달릴 수 있는 초 */
    val pool: Double,
    val share: Double,
    val limitSec: Int,
    /** 내가 쓴 크루 체력 */
    val used: Double,
    /** 무리와 함께였던 초 */
    val togetherSec: Int,
    val headcountBonus: Double,
    val levelBonus: Double,
    val sneakerId: Long?,
)

/** 오늘 이 크루의 크루 러닝 — `crew_run_today` */
data class CrewRunToday(
    val crewId: String,
    val day: LocalDate,
    /** 오늘 내 크루 러닝(어느 크루에서든 하루 한 번) */
    val ranToday: Boolean,
    val mine: CrewRunMine?,
    /** 지금 크루 레벨 — 크루 러닝이 아직 없으면 null("새 크루") */
    val crewLevel: Int?,
    val runKm: Double,
    val nextLevelKm: Double?,
    val staminaBonus: Double,
    val rewardBonus: Double,
    /** 오늘 이 크루에서 크루 러닝을 올린 사람 · 그들의 몫 합 · 쓴 양 */
    val todayRunners: Int,
    val todayPool: Double,
    val todayUsed: Double,
    val rules: CrewRunRules = CrewRunRules(),
) {
    fun isOn(gameDay: LocalDate): Boolean = day == gameDay

    /** 오늘(한국 시각) 값인가 — 자정을 넘겨 들고 있던 어제 값을 오늘 것으로 보이지 않게 */
    fun isToday(): Boolean = isOn(LocalDate.now(CrewHomeRules.KST))
}

/** 방 한 사람 — 고른 신발(없으면 신은 신발) */
data class CrewRoomMember(
    val userId: String,
    val name: String,
    val isMe: Boolean,
    val isHost: Boolean,
    val crewMember: Boolean,
    val ranToday: Boolean,
    val sneakerId: Long?,
    val rarity: String?,
    val shoeLevel: Int?,
    val durMax: Int,
)

/** 방의 크루 체력 미리보기 — `crew_run_room`. 방의 크루원 모두가 함께 달린다고 친 값이다(실제는 서버가 출발 2분 뒤 무리로 센다) */
data class CrewRunRoom(
    val partyId: Long,
    val crewId: String?,
    val status: String,
    val crewLevel: Int,
    val staminaBonus: Double,
    val rewardBonus: Double,
    val runners: Int,
    val pool: Double,
    val share: Double,
    val shareSec: Int,
    val headcountBonus: Double,
    val members: List<CrewRoomMember>,
    val rules: CrewRunRules = CrewRunRules(),
) {
    val me: CrewRoomMember? get() = members.firstOrNull { it.isMe }

    /** 보상 배율(1 + 인원 + 레벨) */
    val multiplier: Double get() = 1 + headcountBonus + rewardBonus
}

/** 끝난 크루 러닝의 결과 — `crew_run_result`(크루 러닝이 아니었으면 없다) */
data class CrewRunOutcome(
    val mine: CrewRunMine,
    val points: Double,
    val reason: String,
    val crewKm: Double,
    val crewName: String,
    /** 지금(올린 뒤) 크루 레벨 — 출발 때보다 높으면 이 러닝으로 레벨이 올랐다 */
    val crewLevelNow: Int?,
    val crewRunKm: Double,
    val nextLevelKm: Double?,
) {
    val levelUp: Boolean get() = (crewLevelNow ?: 0) > mine.crewLevel

    /** 크루 체력을 다 썼다 · 혼자 남아 끝났다 — 서버가 적은 까닭 그대로 본다 */
    val poolEmpty: Boolean get() = reason.contains(CrewRunMath.REASON_POOL_EMPTY)
    val aloneEnded: Boolean get() = reason.contains(CrewRunMath.REASON_ALONE)
}

/** 달리는 중 크루 체력 */
enum class CrewPoolStage {
    /** 넉넉하다 */
    OK,

    /** 20% 아래 */
    LOW,

    /** 다 썼다 — 그 뒤 걸음은 적립하지 않는다(기록은 계속) */
    EMPTY,
}

object CrewRunMath {
    const val ROOM_MAX = 20
    const val NEAR_M = 500
    const val CONFIRM_SEC = 120
    const val ALONE_GRACE_SEC = 60
    const val LEVEL_MAX = 20

    /** 한 사람이 달린 1분마다 쓰는 크루 체력(신발 내구도와 같은 빠르기) */
    const val DRAIN_PER_MIN = 5.0

    /** 체력 1 로 달리는 초 */
    const val SEC_PER_POINT = 12.0

    /** 이만큼 남으면 "얼마 안 남았어요" */
    const val LOW_FRACTION = 0.2

    /** 서버가 적는 까닭(record_session) — 결과 화면이 갈래를 고른다 */
    const val REASON_POOL_EMPTY = "크루 체력을 다 썼습니다"
    const val REASON_ALONE = "혼자 남아 크루 러닝이 끝났습니다"
    const val REASON_DONE_TODAY = "오늘 크루 러닝을 이미 했습니다"

    /** 그 레벨이 되는 크루 러닝 누적 거리(km) — 레벨 1 은 0 */
    val LEVEL_KM = listOf(0, 20, 50, 100, 180, 300, 450, 650, 900, 1200, 1600, 2100, 2700, 3400, 4200, 5100, 6100, 7200, 8500, 10000)

    /** 인원 보너스(비율) — 2 ~ 5명 5%씩 · 6명 50% · 7 ~ 19명 명당 +2% · 20명 150% (economy.crew_headcount_bonus) */
    fun headcountBonus(n: Int): Double = when {
        n >= 20 -> 1.50
        n >= 7 -> 0.50 + 0.02 * (n - 6)
        n == 6 -> 0.50
        n >= 2 -> 0.05 * (n - 1)
        else -> 0.0
    }

    /** 크루 체력 보너스 — 레벨마다 +6%(레벨 20 이면 +114%) */
    fun levelStamina(level: Int?): Double = 0.06 * ((level ?: 1).coerceIn(1, LEVEL_MAX) - 1)

    /** 보상 보너스 — 레벨마다 +1%(레벨 20 이면 +19%) */
    fun levelReward(level: Int?): Double = 0.01 * ((level ?: 1).coerceIn(1, LEVEL_MAX) - 1)

    fun headcountTable(): List<CrewHeadcountStep> = (1..ROOM_MAX).map { CrewHeadcountStep(it, headcountBonus(it)) }

    fun levelTable(): List<CrewLevelStep> = LEVEL_KM.mapIndexed { i, km -> CrewLevelStep(i + 1, km.toDouble()) }

    /** 크루 레벨 — 누적 거리로 */
    fun levelFor(km: Double): Int = LEVEL_KM.indexOfLast { km + 1e-9 >= it }.coerceAtLeast(0) + 1

    /** 크루 체력 = 고른 신발 최대 내구도 합 × (1 + 레벨 보너스) */
    fun pool(durMaxSum: Double, level: Int?): Double = durMaxSum * (1 + levelStamina(level))

    /** 한 사람이 몫으로 달릴 수 있는 초 */
    fun shareSec(pool: Double, runners: Int): Int =
        if (runners <= 0) 0 else floor(pool / runners * SEC_PER_POINT).toInt()

    /** 보상 배율 — 1 + 인원 보너스 + 레벨 보너스(더하기) */
    fun multiplier(runners: Int, level: Int?): Double = 1 + headcountBonus(runners) + levelReward(level)

    /** 달리는 중 남은 몫(초) — 멈춘 시간은 쓰지 않는다 */
    fun secLeft(shareSec: Int, elapsedSec: Long): Long = max(shareSec.toLong() - elapsedSec, 0L)

    /** 남은 몫 비율(0 ~ 1) */
    fun leftFraction(shareSec: Int, elapsedSec: Long): Float =
        if (shareSec <= 0) 0f else (secLeft(shareSec, elapsedSec).toDouble() / shareSec).coerceIn(0.0, 1.0).toFloat()

    fun stage(shareSec: Int, elapsedSec: Long): CrewPoolStage {
        val left = leftFraction(shareSec, elapsedSec)
        return when {
            left <= 0f -> CrewPoolStage.EMPTY
            left <= LOW_FRACTION -> CrewPoolStage.LOW
            else -> CrewPoolStage.OK
        }
    }

    /** 몫 안에서 적립하는 걸음 — 몫을 넘긴 시간의 걸음은 빼고(서버와 같이 걸음을 시간 비율로 나눈다) */
    fun stepsWithinShare(steps: Int, shareSec: Int, elapsedSec: Long): Int =
        if (elapsedSec <= shareSec || elapsedSec <= 0) steps
        else floor(steps.toDouble() * max(shareSec, 0) / elapsedSec).toInt()

    /** 달린 초만큼 쓴 크루 체력 */
    fun usedFor(elapsedSec: Long): Double = elapsedSec / SEC_PER_POINT

    /**
     * 체력 숫자 — 소수 한 자리, 0 으로 끝나면 떼고(80 · 12.4), 소수 한 자리에서 버린다(남은 양을 올려 보이지 않게).
     * 0 보다 작으면 0.
     */
    fun amount(value: Double): String {
        val tenths = floor(max(value, 0.0) * 10 + 1e-6).toLong()
        return if (tenths % 10 == 0L) "${tenths / 10}" else "${tenths / 10}.${tenths % 10}"
    }

    /** 비율 → "15" · "2" · "114" (퍼센트 숫자, 소수는 반올림) */
    fun percent(ratio: Double): String = Math.round(ratio * 100).toString()

    /** 분 — 몫 시간 표시("약 23분"). 1분보다 적으면 0 */
    fun minutes(sec: Int): Int = sec / 60

    /**
     * 레벨 진행 — 지금 레벨이 된 거리에서 다음 레벨 거리까지 몇 % 왔나(0 ~ 1). 마지막 레벨이면 1.
     * 레벨이 없는(새 크루) 크루는 0km 에서 첫 기준까지.
     */
    fun levelProgress(runKm: Double, level: Int?, nextKm: Double?, steps: List<CrewLevelStep>): Float {
        if (nextKm == null) return 1f
        val from = steps.firstOrNull { it.level == (level ?: 1) }?.km ?: 0.0
        val span = nextKm - from
        if (span <= 0.0) return 0f
        return ((runKm - from) / span).coerceIn(0.0, 1.0).toFloat()
    }

    /** 다음 레벨까지 남은 거리(km) — 마지막 레벨이면 null */
    fun kmToNext(runKm: Double, nextKm: Double?): Double? = nextKm?.let { max(it - runKm, 0.0) }

    /**
     * 곁(크루 방 500m)에 크루원이 보이지 않는다 — 위치를 보이는 다른 사람이 하나라도 있고 모두 [nearM] 보다 멀 때만.
     * 위치를 보이지 않는 사람은 모르니 혼자라고 하지 않는다(화면 안내용 — 실제로는 서버가 곁 기록으로 정한다).
     */
    fun aloneNow(me: GeoPoint?, others: List<GeoPoint>, nearM: Int): Boolean =
        me != null && others.isNotEmpty() && others.all { haversineMeters(me, it) > nearM }

    /** 그 시각의 게임 하루(한국 시각) */
    fun gameDay(epochMs: Long): LocalDate = java.time.Instant.ofEpochMilli(epochMs).atZone(CrewHomeRules.KST).toLocalDate()
}
