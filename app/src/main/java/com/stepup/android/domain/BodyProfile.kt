package com.stepup.android.domain

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * S2 첫 설정의 신체 정보와 목표 — 이 기기에만 저장한다(서버로 보내지 않는다).
 * 값이 없으면 사용자가 건너뛴 것이다. 앱은 이 값으로 적립이나 기록을 바꾸지 않는다.
 */
data class BodyProfile(
    val heightCm: Int? = null,
    val weightKg: Double? = null,
    val goalWeightKg: Double? = null,
    val goalWeeks: Int? = null,
) {
    val bmi: Double? get() = BodyMath.bmi(heightCm, weightKg)
    val goalBmi: Double? get() = BodyMath.bmi(heightCm, goalWeightKg)
}

/**
 * 홈 구성 모드(S2 시안 19). 둘 다 탭은 넷이고 SUP 적립 규칙은 같다 — 서버가 계산한다.
 *  - LITE: 홈에 걸음 · 풍경 · 시작만.
 *  - RUNNER: 홈에 착용 신발과 상태를 함께 보인다.
 */
enum class RunMode { LITE, RUNNER }

/** 대한비만학회 성인 BMI 구간(kg/m²) */
enum class BmiBand(val upperExclusive: Double) {
    UNDER(18.5), NORMAL(23.0), PRE_OBESE(25.0), OBESE(Double.MAX_VALUE),
}

object BodyMath {
    const val MIN_HEIGHT = 100
    const val MAX_HEIGHT = 230
    const val MIN_WEIGHT = 25.0
    const val MAX_WEIGHT = 250.0
    val GOAL_WEEKS = listOf(8, 12, 16)

    fun bmi(heightCm: Int?, weightKg: Double?): Double? {
        if (heightCm == null || weightKg == null || heightCm <= 0 || weightKg <= 0) return null
        val m = heightCm / 100.0
        return weightKg / (m * m)
    }

    fun band(bmi: Double): BmiBand = BmiBand.entries.first { bmi < it.upperExclusive }

    /** 0.1 단위로 반올림 — 0.5 kg 단위 조절에서 부동소수 오차가 쌓이지 않게 */
    fun round1(value: Double): Double = (value * 10).roundToInt() / 10.0

    fun clampHeight(cm: Int): Int = cm.coerceIn(MIN_HEIGHT, MAX_HEIGHT)
    fun clampWeight(kg: Double): Double = round1(kg.coerceIn(MIN_WEIGHT, MAX_WEIGHT))

    /** 한 주에 바꿔야 하는 몸무게(kg, 절댓값). 기간이 없으면 null */
    fun weeklyChange(fromKg: Double, toKg: Double, weeks: Int?): Double? =
        if (weeks == null || weeks <= 0) null else abs(toKg - fromKg) / weeks

    /** 주당 1kg 넘게 줄이거나 목표 BMI 가 저체중 구간이면 한 번 더 살펴보라고 알린다 */
    fun needsCaution(profile: BodyProfile): Boolean {
        val from = profile.weightKg ?: return false
        val to = profile.goalWeightKg ?: return false
        val weekly = weeklyChange(from, to, profile.goalWeeks)
        val tooFast = to < from && weekly != null && weekly > 1.0
        val underweight = profile.goalBmi?.let { band(it) == BmiBand.UNDER } == true
        return tooFast || underweight
    }
}
