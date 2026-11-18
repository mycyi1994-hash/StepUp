package com.stepup.android.domain

import java.util.Locale
import kotlin.math.roundToInt

/**
 * 러닝 보상의 운(경제 4판, 서버 0116 `record_session` 의 `reward_luck` — 0.7 ~ 1.3, 평균 1). 상한 전 적립에 곱한 값이다.
 *
 * 서버가 이 러닝을 확인하고 적립이 있을 때만 보인다([shown]) — 무효 · 예전 서버는 값이 없고(null), 적립이 0 이면 운이 뜻이 없다.
 * 폰은 운을 짓지 않는다 — 받은 값을 소수 둘째 자리로 보여 줄 뿐이다.
 */
object RunLuck {
    enum class Tone { GOOD, EVEN, LOW }

    /** ×0.98 ~ ×1.02(소수 둘째 자리로 맞춘 값)는 평소만큼으로 본다 */
    private const val EVEN_LOW_CENTS = 98
    private const val EVEN_HIGH_CENTS = 102

    /** 소수 둘째 자리로 맞춘 운(× 100) */
    private fun cents(luck: Double): Int = (luck * 100).roundToInt()

    fun tone(luck: Double): Tone = when {
        cents(luck) > EVEN_HIGH_CENTS -> Tone.GOOD
        cents(luck) < EVEN_LOW_CENTS -> Tone.LOW
        else -> Tone.EVEN
    }

    /** "×1.12" — 소수 둘째 자리, 앱 언어와 상관없이 점 */
    fun factor(luck: Double): String = "×" + String.format(Locale.ROOT, "%.2f", cents(luck) / 100.0)

    /**
     * 화면에 보일 운 — 서버가 확인했고([signed]) 적립이 있는([points] > 0) 러닝의 서버 값만. 아니면 null(줄을 두지 않는다).
     */
    fun shown(luck: Double?, points: Double?, signed: Boolean): Double? =
        luck?.takeIf { signed && (points ?: 0.0) > 0.0 && it.isFinite() && it > 0.0 }
}
