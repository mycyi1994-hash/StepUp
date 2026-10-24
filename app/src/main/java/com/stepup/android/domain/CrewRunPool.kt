package com.stepup.android.domain

import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.max

/*
 * 크루 러닝 — 크루 · 하루 하나의 공용 체력(서버 0071, 2026-10-22 크루 러닝 핸드오프 R01 ~ R13).
 *
 * - 오늘 크루 체력 = 그 크루 · 그 한국 날짜에 하나. 참가자가 실제로 출발할 때 고른 신발의 최대 내구도 × (1 + 레벨 체력 보너스)를 더한다.
 *   방이 달라도 같은 풀이다. 0/0 은 새 하루, 0/816 은 다 쓴 것 — 어느 쪽도 출발을 막지 않는다.
 * - 달리는 사람마다 1분에 5. 일시정지한 사람은 쓰지 않는다. 개인 신발 내구도 · 개인 에너지는 쓰지 않는다.
 * - 0 이 되면 적립만 멈추고 기록은 계속. 개인 신발로 넘어가지 않는다.
 * - 보상 = 기본 B(내가 고른 신발) + B × 인원 보너스(구간 하나) + B × 레벨 보너스. 확정 전에는 "확인 중".
 * - 인원 보너스 인원 = 같은 크루 · 출발 무렵과 2분 뒤 모두 500m 안 · 휴대폰 인증. 미인증자도 참가 · 기여 · 자기 보상은 그대로.
 * - 사용자 · 한국 날짜에 한 번(크루를 바꿔도). 다른 참가자가 모두 끝내면 마지막 한 사람도 서버가 끝낸다.
 *
 * 앱은 서버 값(crew_run_today · crew_run_room · crew_run_beat · crew_run_result)을 그대로 보인다. 아래 셈은 표시용 보간 · 미리보기 ·
 * 규칙 표(서버 economy.crew_headcount_bonus 등과 같은 값 — CrewRunPoolTest)뿐이다. 서버가 주지 않은 값은 null(0 이 아니다).
 */

/** 인원 보너스 한 줄 — n명이면 bonus(비율) */
data class CrewHeadcountStep(val n: Int, val bonus: Double)

/** 크루 레벨 한 줄 — 그 레벨이 되는 크루 러닝 누적 거리 · 공용 체력 보너스 · 보상 보너스(비율) */
data class CrewLevelStep(
    val level: Int,
    val km: Double,
    val stamina: Double = CrewRunMath.levelStamina(level),
    val reward: Double = CrewRunMath.levelReward(level),
)

/** 크루 러닝 규칙 — 서버가 준 표(없으면 앱의 같은 값) */
data class CrewRunRules(
    val roomMax: Int = CrewRunMath.ROOM_MAX,
    val nearM: Int = CrewRunMath.NEAR_M,
    val confirmSec: Int = CrewRunMath.CONFIRM_SEC,
    val drainPerMin: Double = CrewRunMath.DRAIN_PER_MIN,
    /** 인원 보너스에 휴대폰 인증이 필요한가 */
    val requiresPhone: Boolean = true,
    val headcount: List<CrewHeadcountStep> = CrewRunMath.headcountTable(),
    val levels: List<CrewLevelStep> = CrewRunMath.levelTable(),
) {
    fun headcountBonus(n: Int): Double =
        headcount.firstOrNull { it.n == n }?.bonus ?: CrewRunMath.headcountBonus(n)
}

/** 오늘 공용 체력 — 서버가 정산한 값. 남은 양 · 총량(분모 — 기여 합) · 기여한 사람 · 지금 쓰는 사람(크루 전체) */
data class CrewPoolSnap(
    val day: LocalDate,
    val total: Double,
    val remaining: Double,
    val consumed: Double,
    val contributors: Int,
    val activeRunners: Int,
    val exhausted: Boolean,
    val low: Boolean,
    val version: Long,
    val asOfMs: Long,
) {
    /** 아직 기여가 없는 새 하루(0/0) — 다 쓴 것(0/816)과 다르다 */
    val fresh: Boolean get() = total <= 0.0

    /** 표시용 — 서버 값에서 지금 쓰는 사람 수만큼 흘러간 시간을 빼 부드럽게(원장이 아니다) */
    fun remainingAt(nowMs: Long): Double =
        if (exhausted || activeRunners <= 0) remaining
        else max(remaining - activeRunners * max(nowMs - asOfMs, 0L) / 1000.0 / CrewRunMath.SEC_PER_POINT, 0.0)
}

/** 내 오늘 크루 러닝 — 했는지(어느 크루에서든) · 상태 · 끝난 까닭 · 다음 참여 가능 시각 */
data class CrewRunMine(
    val usedToday: Boolean,
    val state: CrewRunState?,
    val crewId: String?,
    val crewName: String?,
    val partyId: Long?,
    val sessionId: Long?,
    val startedAtMs: Long?,
    val terminalReason: CrewTerminal?,
    val contribution: Double,
    val consumed: Double,
    val distanceM: Double?,
    val totalSup: Double?,
    val nextEligibleAtMs: Long?,
) {
    val active: Boolean get() = usedToday && state != null && state != CrewRunState.ENDED
}

/** 크루 레벨 업 — 서버가 적은 마지막 레벨 업 */
data class CrewLevelEvent(val level: Int, val reachedAtMs: Long, val runKm: Double?)

/** 크루 홈 · 입구 — `crew_run_today` */
data class CrewRunToday(
    val crewId: String,
    val day: LocalDate,
    val serverNowMs: Long,
    val resetsAtMs: Long,
    val pool: CrewPoolSnap,
    val mine: CrewRunMine,
    /** 내 휴대폰 인증 */
    val verified: Boolean,
    /** 지금 크루 레벨 — 크루 러닝이 아직 없으면 null("새 크루") */
    val crewLevel: Int?,
    val runKm: Double,
    val nextLevelKm: Double?,
    val staminaBonus: Double,
    val rewardBonus: Double,
    val levelEvent: CrewLevelEvent?,
    val rules: CrewRunRules = CrewRunRules(),
) {
    fun isOn(gameDay: LocalDate): Boolean = day == gameDay

    /** 오늘(한국 시각) 값인가 — 자정을 넘겨 들고 있던 어제 값을 오늘 것으로 보이지 않게 */
    fun isToday(): Boolean = isOn(LocalDate.now(CrewHomeRules.KST))

    /** 오늘 참여했고 다른 크루에서였다 — 크루를 바꿔도 하루 한 번 */
    val usedElsewhere: Boolean get() = mine.usedToday && mine.crewId != null && mine.crewId != crewId
}

/** 대기실 한 사람 — 준비 · 크루원 · 인증(참 · 거짓만) · 오늘 했는지 · 고른 신발(최대 · 지금 개인 내구도) */
data class CrewRoomMember(
    val userId: String,
    val name: String,
    val isMe: Boolean,
    val isHost: Boolean,
    val ready: Boolean,
    val crewMember: Boolean,
    val ranToday: Boolean,
    val verified: Boolean,
    val sneakerId: Long?,
    val rarity: String?,
    val shoeLevel: Int?,
    val modelId: Int?,
    val durMax: Int,
    val durNow: Int?,
)

/** 대기실 — `crew_run_room`. 출발 전 미리보기(준비한 크루원 중 인증된 사람 수로 센 인원 보너스 · 내 기여) · 오늘 공용 체력 */
data class CrewRunRoom(
    val partyId: Long,
    val crewId: String?,
    val status: String,
    val capacity: Int,
    val crewLevel: Int,
    val staminaBonus: Double,
    val rewardBonus: Double,
    val membersCount: Int,
    val readyCount: Int,
    val bonusCount: Int,
    val bonusPct: Double,
    val nextBonusPct: Double,
    val myContribution: Double,
    val myVerified: Boolean,
    val pool: CrewPoolSnap?,
    val members: List<CrewRoomMember>,
    val rules: CrewRunRules = CrewRunRules(),
) {
    val me: CrewRoomMember? get() = members.firstOrNull { it.isMe }
}

enum class CrewRunState { RUNNING, PAUSED, ENDED;
    companion object { fun of(v: String?): CrewRunState? = entries.firstOrNull { it.name == v } }
}

/** 러닝이 끝난 까닭 — 내가 끝냄 · 다른 사람이 모두 끝내 마지막으로 자동 종료 · 러닝을 올려 끝남 */
enum class CrewTerminal { USER_END, LAST_PARTICIPANT_END, RECORDED;
    companion object { fun of(v: String?): CrewTerminal? = entries.firstOrNull { it.name == v } }
}

/** 인원 보너스 판정 — 2분 전 · 확정 · 확인할 수 없음 */
enum class CrewBonusStatus { PENDING, CONFIRMED, UNAVAILABLE;
    companion object { fun of(v: String?): CrewBonusStatus = entries.firstOrNull { it.name == v } ?: PENDING }
}

/** 적립 상태 — 러닝 상태와 따로다(체력 0 은 끝이 아니다) */
enum class CrewEarning { PENDING, ACCRUING, POOL_EMPTY, PAUSED, ENDED;
    companion object { fun of(v: String?): CrewEarning = entries.firstOrNull { it.name == v } ?: PENDING }
}

data class CrewBonus(
    val status: CrewBonusStatus,
    val checkAtMs: Long?,
    val count: Int,
    val pct: Double,
    /** TOO_FEW · NO_LOCATION · ENDED_BEFORE_CHECK */
    val reason: String?,
    /** 나는 인원에 들 수 있나(인증) */
    val verified: Boolean,
)

/** 달리는 중 — `crew_run_beat` · `crew_run_live` */
data class CrewRunLive(
    val partyId: Long,
    val crewId: String?,
    val day: LocalDate?,
    val startedAtMs: Long?,
    val state: CrewRunState,
    val terminal: CrewTerminal?,
    val contribution: Double,
    val consumed: Double,
    val activeSec: Double,
    val pool: CrewPoolSnap?,
    val participants: Int,
    val notEnded: Int,
    val roomActiveRunners: Int,
    val bonus: CrewBonus,
    val crewLevel: Int,
    val levelPct: Double,
    val earning: CrewEarning,
    val serverNowMs: Long,
) {
    /** 다른 참가자가 모두 끝내 서버가 나를 끝냈다 */
    val endedAlone: Boolean get() = state == CrewRunState.ENDED && terminal == CrewTerminal.LAST_PARTICIPANT_END
}

/** 결과 — `crew_run_result`. 서버가 정산하기 전에는 settled=false */
data class CrewRunOutcome(
    val settled: Boolean,
    val crewId: String?,
    val crewName: String,
    val verdict: String,
    val reason: String,
    val terminal: CrewTerminal?,
    val participants: Int,
    val base: Double?,
    val headcountPct: Double,
    val headcount: Double?,
    val levelPct: Double,
    val level: Double?,
    val total: Double?,
    val bonus: CrewBonus,
    val contribution: Double,
    val consumed: Double,
    val activeSec: Double,
    val pool: CrewPoolSnap?,
    val xpM: Double?,
    val xpApplied: Boolean,
    val levelBefore: Int?,
    val levelAfter: Int?,
    val runKm: Double?,
    val nextLevelKm: Double?,
    val nextEligibleAtMs: Long?,
    /** 그 러닝의 대기실 이름 · 코스(0072 — 출발 때 적은 값, 지난 기록은 그때 이름으로). 예전 방이면 null */
    val partyName: String? = null,
    val courseName: String? = null,
    val courseKm: Double? = null,
) {
    val levelUp: Boolean get() = (levelAfter ?: 0) > (levelBefore ?: Int.MAX_VALUE)
    val invalid: Boolean get() = verdict == "VOID"

    /** 공용 체력이 0 이던 구간이 있어 그 뒤로는 적립하지 않았다 — 서버가 적은 까닭 그대로 본다 */
    val poolEmpty: Boolean get() = reason.contains(CrewRunMath.REASON_POOL_EMPTY)
}

object CrewRunMath {
    const val ROOM_MAX = 20
    const val NEAR_M = 500
    const val CONFIRM_SEC = 120
    const val LEVEL_MAX = 20

    /** 한 사람이 달린 1분마다 쓰는 공용 체력 */
    const val DRAIN_PER_MIN = 5.0

    /** 체력 1 로 달리는 초 */
    const val SEC_PER_POINT = 12.0

    /** 이만큼 남으면 "얼마 남지 않았어요" */
    const val LOW_FRACTION = 0.2

    /** 서버가 적는 까닭(record_session) — 결과 화면이 갈래를 고른다 */
    const val REASON_POOL_EMPTY = "크루 체력을 다 썼습니다"
    const val REASON_DONE_TODAY = "오늘 크루 러닝을 이미 했습니다"

    /** 그 레벨이 되는 크루 러닝 누적 거리(km) — 레벨 1 은 0 */
    val LEVEL_KM = listOf(0, 20, 50, 100, 180, 300, 450, 650, 900, 1200, 1600, 2100, 2700, 3400, 4200, 5100, 6100, 7200, 8500, 10000)

    /** 인원 보너스(비율) — 구간 하나: 2 ~ 5명 5%씩 · 6명 50% · 7 ~ 19명 명당 +2% · 20명 150% (economy.crew_headcount_bonus) */
    fun headcountBonus(n: Int): Double = when {
        n >= 20 -> 1.50
        n >= 7 -> 0.50 + 0.02 * (n - 6)
        n == 6 -> 0.50
        n >= 2 -> 0.05 * (n - 1)
        else -> 0.0
    }

    /** 공용 체력 보너스 — 레벨마다 +6%(Lv20 +114%) */
    fun levelStamina(level: Int?): Double = 0.06 * ((level ?: 1).coerceIn(1, LEVEL_MAX) - 1)

    /** 보상 보너스 — 레벨마다 +1%(Lv20 +19%) */
    fun levelReward(level: Int?): Double = 0.01 * ((level ?: 1).coerceIn(1, LEVEL_MAX) - 1)

    fun headcountTable(): List<CrewHeadcountStep> = (1..ROOM_MAX).map { CrewHeadcountStep(it, headcountBonus(it)) }

    fun levelTable(): List<CrewLevelStep> = LEVEL_KM.mapIndexed { i, km -> CrewLevelStep(i + 1, km.toDouble()) }

    /** 크루 레벨 — 누적 거리로 */
    fun levelFor(km: Double): Int = LEVEL_KM.indexOfLast { km + 1e-9 >= it }.coerceAtLeast(0) + 1

    /** 내 기여 미리보기 = 최대 내구도 × (1 + 레벨 체력 보너스) — 남은 개인 내구도가 아니다 */
    fun contribution(durMax: Int, level: Int?): Double = durMax * (1 + levelStamina(level))

    /** 보상 나눔 — 기본 B · 인원 · 레벨(각각 4자리, 합은 그 셋의 합 — 서버와 같다) */
    data class Split(val base: Double, val headcount: Double, val level: Double) { val total: Double get() = base + headcount + level }

    fun split(base: Double, headcountPct: Double, levelPct: Double): Split {
        val b = round4(base)
        return Split(b, round4(b * headcountPct), round4(b * levelPct))
    }

    private fun round4(v: Double): Double = Math.round(v * 10_000.0) / 10_000.0

    /** 공용 체력 비율(0 ~ 1) — 총량이 0 이면 0 */
    fun fraction(remaining: Double, total: Double): Float = if (total <= 0.0) 0f else (remaining / total).coerceIn(0.0, 1.0).toFloat()

    /** 20% 아래인가(0 은 아님) */
    fun low(remaining: Double, total: Double): Boolean = total > 0 && remaining > 0 && remaining <= total * LOW_FRACTION

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

    /** SUP — 소수 둘째 자리 */
    fun sup(value: Double): String = String.format(java.util.Locale.US, "%.2f", value)

    /**
     * 레벨 진행 — 지금 레벨이 된 거리에서 다음 레벨 거리까지 몇 % 왔나(0 ~ 1). 마지막 레벨이면 1.
     * Lv7 · 520km 면 450 → 650 구간의 70/200 = 35%(520/650 이 아니다).
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

    /** 그 시각의 게임 하루(한국 시각) */
    fun gameDay(epochMs: Long): LocalDate = java.time.Instant.ofEpochMilli(epochMs).atZone(CrewHomeRules.KST).toLocalDate()

    /** 2분 확인까지 남은 초 — 서버 시각 기준(폰 시계가 아니다) */
    fun checkLeftSec(checkAtMs: Long?, serverNowMs: Long, receivedAtMs: Long, nowMs: Long): Long? =
        checkAtMs?.let { max((it - (serverNowMs + (nowMs - receivedAtMs))) / 1000, 0) }
}
