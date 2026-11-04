package com.stepup.android.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 안드로이드 · 아이폰 둘 다에서 도는 검사 — 기댓값은 Java(String.format · BigDecimal · URLEncoder)가 낸 글자 그대로다.
 * 안드로이드에서는 Platform 이 Java 를 부르고, 아이폰에서는 순수 Kotlin(JavaFormat · ExactDecimal)을 부른다 — 둘 다 이 답이어야 한다.
 */
class PlatformCommonTest {

    @Test
    fun formatRoot_matches_java() {
        assertEquals("2.68", formatRoot("%.2f", 2.675))
        assertEquals("0.2", formatRoot("%.1f", 0.15))
        assertEquals("1.01", formatRoot("%.2f", 1.005))
        assertEquals("3", formatRoot("%.0f", 2.5))
        assertEquals("1", formatRoot("%.0f", 0.5))
        assertEquals("1,234,567.89", formatRoot("%,.2f", 1234567.891))
        assertEquals("1,000.0", formatRoot("%,.1f", 999.95))
        assertEquals("0.000", formatRoot("%.3f", 1e-7))
        assertEquals("10000000.00", formatRoot("%.2f", 1e7))
        assertEquals("37.5665", formatRoot("%.4f", 37.56651234))
        assertEquals("-0.00", formatRoot("%.2f", -0.0))
        assertEquals("-0.0", formatRoot("%.1f", -0.04))
        assertEquals("007", formatRoot("%03d", 7))
        assertEquals("1,234,567", formatRoot("%,d", 1234567))
        assertEquals("59:09", formatRoot("%d:%02d", 59, 9))
        assertEquals("0.30", formatRoot("%.2f", 0.1 + 0.2))
        assertEquals("12.3km", formatRoot("%.1fkm", 12.345))
        assertEquals("12.5%", formatRoot("%d.%d%%", 12, 5))
        assertEquals("5'07\"", formatUs("%d'%02d\"", 5, 7))
    }

    @Test
    fun exactHalfUp_matches_bigdecimal() {
        assertEquals("3", exactHalfUp(2.675, 0))
        assertEquals("2.67", exactHalfUp(2.675, 2))
        assertEquals("2.675000", exactHalfUp(2.675, 6))
        assertEquals("0.13", exactHalfUp(0.125, 2))
        assertEquals("1.00", exactHalfUp(1.005, 2))
        assertEquals("37.566512", exactHalfUp(37.566512345678, 6))
        assertEquals("0.000000", exactHalfUp(-1e-7, 6))
        assertEquals("0.10", exactHalfUp(0.1, 2))
        assertEquals("37.566512", stripDecimalZeros(exactHalfUp(37.566512, 6)))
        assertEquals("120", stripDecimalZeros(exactHalfUp(120.0, 6)))
    }

    @Test
    fun shortHalfUp_matches_bigdecimal_valueOf() {
        assertEquals(2.68, shortHalfUp(2.675, 2))
        assertEquals(1.01, shortHalfUp(1.005, 2))
        assertEquals(-37.57, shortHalfUp(-37.565, 2))
    }

    @Test
    fun urlEncode_matches_urlencoder() {
        assertEquals("%EB%9F%AC%EB%8B%9D+%ED%81%AC%EB%A3%A8+*a.b-c_d%7E%21", urlEncodeUtf8("러닝 크루 *a.b-c_d~!"))
    }

    @Test
    fun codePoints_count_emoji_as_one() {
        assertEquals(3, codePointCount("가😀나"))
        assertEquals(3, codePointOffset("😀a", 2))
        assertEquals("😀😀", takeCodePoints("😀😀😀", 2))
        assertEquals("😀", firstCodePoint("😀x"))
    }

    @Test
    fun javaRound_half_up_and_nan_zero() {
        assertEquals(3L, javaRound(2.5))
        assertEquals(-2L, javaRound(-2.5))
        assertEquals(0L, javaRound(Double.NaN))
    }
}
