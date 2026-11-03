package com.stepup.android.domain

/**
 * 돈 계산용 고정 소수(2026-11-03 iOS C3) — java.math.BigDecimal 중 수리 견적이 쓰는 만큼만: valueOf(Double) · setScale(HALF_UP) ·
 * 곱 · 차 · max · 비교 · 부호 · toDouble · toPlainString. 값 = [unscaled] × 10^−[scale]. 공용 코드라 기기마다 같은 자리 셈이다.
 * JVM 검사(DecimalJvmTest)가 BigDecimal 과 맞대어 본다.
 */
class Decimal private constructor(val unscaled: Long, val scale: Int) : Comparable<Decimal> {
    init { require(scale in 0..MAX_SCALE) }

    /** BigDecimal.setScale(newScale, HALF_UP) — 0.5 는 0 에서 먼 쪽 */
    fun setScale(newScale: Int): Decimal = when {
        newScale == scale -> this
        newScale > scale -> Decimal(unscaled * pow10(newScale - scale), newScale)
        else -> {
            val div = pow10(scale - newScale)
            val q = unscaled / div
            val r = unscaled % div
            val up = kotlin.math.abs(r) * 2 >= div
            Decimal(if (up) q + (if (unscaled < 0) -1 else 1) else q, newScale)
        }
    }

    fun multiply(other: Decimal): Decimal = Decimal(Math2.multiplyExact(unscaled, other.unscaled), scale + other.scale)

    fun subtract(other: Decimal): Decimal {
        val s = maxOf(scale, other.scale)
        return Decimal(setScale(s).unscaled - other.setScale(s).unscaled, s)
    }

    fun max(other: Decimal): Decimal = if (this >= other) this else other

    fun signum(): Int = unscaled.compareTo(0L)

    override fun compareTo(other: Decimal): Int {
        val s = maxOf(scale, other.scale)
        return setScale(s).unscaled.compareTo(other.setScale(s).unscaled)
    }

    /** BigDecimal.toPlainString — 자리 그대로(끝 0 도) */
    fun toPlainString(): String {
        val negative = unscaled < 0
        val digits = kotlin.math.abs(unscaled).toString().padStart(scale + 1, '0')
        val body = if (scale == 0) digits else digits.dropLast(scale) + "." + digits.takeLast(scale)
        return if (negative) "-$body" else body
    }

    /** BigDecimal.toDouble — 십진 표기를 가장 가까운 double 로 */
    fun toDouble(): Double = toPlainString().toDouble()

    override fun equals(other: Any?): Boolean = other is Decimal && other.unscaled == unscaled && other.scale == scale
    override fun hashCode(): Int = unscaled.hashCode() * 31 + scale
    override fun toString(): String = toPlainString()

    companion object {
        const val MAX_SCALE = 12
        val ZERO = Decimal(0, 0)

        /** BigDecimal.valueOf(Long) */
        fun of(value: Long): Decimal = Decimal(value, 0)

        /** BigDecimal.valueOf(Double).setScale([scale], HALF_UP) — Double.toString 의 십진 표기에서 자른다(formatFixed 와 같은 자리) */
        fun of(value: Double, scale: Int): Decimal {
            require(value.isFinite()) { "BigDecimal 은 NaN · 무한을 받지 않는다" }
            val text = formatFixed(value, scale)
            val negative = text.startsWith("-")
            val digits = text.removePrefix("-").replace(".", "")
            val n = digits.toLong()
            return Decimal(if (negative) -n else n, scale)
        }

        private fun pow10(n: Int): Long {
            var p = 1L
            repeat(n) { p *= 10 }
            return p
        }
    }
}

private object Math2 {
    fun multiplyExact(a: Long, b: Long): Long {
        val r = a * b
        if (a != 0L && (r / a != b || (a == -1L && b == Long.MIN_VALUE))) throw ArithmeticException("long overflow")
        return r
    }
}
