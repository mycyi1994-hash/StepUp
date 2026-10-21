package com.stepup.android.domain

import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.max

/*
 * 크루 체력(서버 0069, 2026-10-21 사용자 결정) — 크루 러닝(그 크루의 함께 달리기 방 · 함께 달린 2명 이상)은 개인 에너지 대신
 * 크루가 함께 쓰는 체력으로 적립한다.
 *
 * - 크루 체력(그 크루의 하루, 한국 시각) = 나눠 받는 크루원의 신발 에너지 최대치 합 + 크루 레벨 보너스(레벨 하나마다 10).
 *   나눠 받는 크루원 = 그날 전에 가입했고 그 전 7일 안에 달린 크루원. 그날 처음 읽거나 쓸 때 서버가 한 번 정한다.
 * - 한 사람 몫 = 크루 체력 ÷ 나눠 받는 크루원 수. 몫을 다 쓰면 그 뒤 크루 러닝은 적립이 없다 — 개인 에너지로 넘어가지 않는다.
 *   나눠 받지 않는 크루원(갓 가입 · 오래 쉼)의 크루 러닝은 개인 에너지로 적립한다.
 * - 크루 레벨은 크루 러닝 누적 거리(함께 달린 사람마다 자기 거리)로 오른다.
 *
 * 앱은 서버가 정한 값을 보이기만 한다 — 몫 · 남은 양을 여기서 셈하지 않는다(달리는 중 예상만 [RewardEconomy] 로).
 */

/** 그날의 크루 체력 — `crew_stamina` 가 돌려준 값 */
data class CrewStamina(
    val crewId: String,
    val day: LocalDate,
    /** 그날 크루 체력 — 나눠 받는 크루원이 없으면 0 */
    val max: Double,
    val used: Double,
    val left: Double,
    /** 나눠 받는 크루원 수 */
    val contributors: Int,
    /** 한 사람 몫 */
    val share: Double,
    /** 그날 크루 체력을 정한 때의 레벨과 그 보너스 — 오늘 오른 레벨은 내일부터 */
    val level: Int,
    val levelBonus: Double,
    val me: CrewStaminaMe,
    /** 나눠 받는 크루원 — 보탠 양이 큰 순 */
    val members: List<CrewStaminaMember>,
    /** 지금 크루 레벨 — 크루 러닝이 아직 없으면 null("새 크루") */
    val crewLevel: Int?,
    val runKm: Double,
    /** 다음 레벨이 되는 누적 거리 — 마지막 레벨이면 null */
    val nextLevelKm: Double?,
) {
    /** 그 하루(한국 시각)의 값인가 — 러닝이 시작한 날의 체력만 그 러닝에 쓴다 */
    fun isOn(gameDay: LocalDate): Boolean = day == gameDay

    /** 오늘(한국 시각) 값인가 — 자정을 넘겨 들고 있던 어제 체력을 오늘 것으로 보이지 않게 */
    fun isToday(): Boolean = isOn(LocalDate.now(CrewHomeRules.KST))

    /** 크루 러닝이 받쳐 줄 체력 — 이 사람에게 */
    val pool: CrewPool
        get() = when {
            !me.contributor -> CrewPool.PERSONAL
            me.left < CrewStaminaRules.ONE_STEP -> CrewPool.CREW_EMPTY
            else -> CrewPool.CREW
        }
}

/** 나 — 나눠 받는지 · 보탠 양(신은 신발의 에너지 최대치) · 쓴 양 · 남은 몫 · 내 id(명단의 "나") */
data class CrewStaminaMe(val contributor: Boolean, val contribution: Double, val used: Double, val left: Double, val userId: String = "")

/** 나눠 받는 크루원 한 사람 — 보탠 양만(사람마다 쓴 양은 서버가 주지 않는다) */
data class CrewStaminaMember(val userId: String, val name: String, val contribution: Double)

/** 크루 러닝이 받쳐 줄 체력 */
enum class CrewPool {
    /** 내 몫이 남았다 — 크루 체력으로 적립 */
    CREW,

    /** 내 몫을 다 썼다 — 크루 러닝은 적립이 없다(개인 에너지로 넘어가지 않는다) */
    CREW_EMPTY,

    /** 오늘 나눠 받지 않는다(갓 가입 · 7일 넘게 쉼) — 크루 러닝도 개인 에너지로 */
    PERSONAL,
}

/** 크루 레벨 기준 한 줄 — `crew_level` 의 levels */
data class CrewLevelStep(val level: Int, val km: Double, val bonus: Double)

object CrewStaminaRules {
    /** 걸음 하나에 드는 체력(착화감 0) — 이보다 적게 남으면 다 쓴 것으로 본다 */
    const val ONE_STEP = 1.0 / RewardEconomy.STEPS_PER_ENERGY

    /** 크루 레벨 하나마다 더하는 크루 체력(서버 economy.crew_level_bonus) */
    const val LEVEL_BONUS = 10.0

    /**
     * 체력 숫자 — 소수 한 자리, 0 으로 끝나면 떼고(80 · 12.4), 소수 한 자리에서 버린다(남은 몫을 올려 보이지 않게).
     * 0 보다 작으면 0.
     */
    fun amount(value: Double): String {
        val tenths = floor(max(value, 0.0) * 10 + 1e-6).toLong()
        return if (tenths % 10 == 0L) "${tenths / 10}" else "${tenths / 10}.${tenths % 10}"
    }

    /** 게이지 칸 — 남은 양 ÷ 전체(0 ~ 1) */
    fun fraction(left: Double, max: Double): Float = if (max <= 0.0) 0f else (left / max).coerceIn(0.0, 1.0).toFloat()

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

    /** 그 시각의 게임 하루(한국 시각) — 크루 체력은 러닝을 시작한 날의 것을 쓴다 */
    fun gameDay(epochMs: Long): LocalDate = java.time.Instant.ofEpochMilli(epochMs).atZone(CrewHomeRules.KST).toLocalDate()

    /** 레벨 보너스 — 레벨 1(과 새 크루)은 0 */
    fun levelBonus(level: Int?): Double = LEVEL_BONUS * ((level ?: 1).coerceAtLeast(1) - 1)
}
