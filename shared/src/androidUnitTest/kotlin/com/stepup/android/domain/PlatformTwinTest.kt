package com.stepup.android.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URLEncoder
import java.util.Locale
import kotlin.random.Random

/**
 * 아이폰 쪽 Platform(iosMain)이 쓰는 순수 Kotlin 이 안드로이드(Java)와 같은 글자를 내는지 맞대어 본다.
 * 안드로이드 앱은 Java 를 그대로 부르므로(Platform.android.kt) 이 테스트가 곧 "두 앱이 같은 글자"의 근거다.
 */
class PlatformTwinTest {

    private val random = Random(20261104)

    /** 규칙 코드가 쓰는 꼴(domain 의 formatRoot · formatUs · formatLocal 자리) + 깃발 · 너비 몇 개 */
    private val doublePatterns = listOf(
        "%.0f", "%.1f", "%.2f", "%.3f", "%.4f", "%,.1f", "%,.2f", "%.1fkm", "%f", "%8.2f", "%-8.2f|", "%08.2f", "%+.2f", "%,.0f",
    )
    private val intPatterns = listOf("%d", "%02d", "%03d", "%,d", "%5d", "%-5d|", "%+d", "%05d")

    private fun doubles(): List<Double> {
        val fixed = listOf(
            0.0, -0.0, 0.5, 1.5, 2.5, -2.5, 0.05, 0.15, 0.25, 0.35, 1.005, 2.675, 999.995, 9.995, 99.95, 0.0001, 0.00005,
            1e-7, 123.456, 1234567.891, 1e7, 12345678.125, -0.04, -1234.5678, 42.0, 100.0, 999.99, 0.1 + 0.2, 1.0 / 3.0,
            Double.MIN_VALUE, 37.56651234, 126.97801234, 4.35, 8.325, 1.45, 3.14159265358979,
        )
        val made = List(4000) {
            when (it % 4) {
                0 -> random.nextInt(0, 10_000_000) / 1000.0 // 짧은 소수(거리 · km)
                1 -> random.nextInt(0, 100_000) / 100.0 + random.nextDouble(-1e-9, 1e-9)
                2 -> random.nextDouble(-1000.0, 1000.0)
                else -> random.nextDouble(0.0, 200.0) * random.nextDouble(0.0, 1.0)
            }
        }
        return fixed + made
    }

    @Test
    fun `%f 와 %d 는 Java String format 과 같은 글자`() {
        for (v in doubles() + listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            for (p in doublePatterns) {
                assertEquals("$p of $v", String.format(Locale.ROOT, p, v), JavaFormat.format(p, v))
            }
        }
        val ints = listOf(0, 1, -1, 7, 42, 999, 1000, 12345, -98765, Int.MAX_VALUE, Int.MIN_VALUE) + List(500) { random.nextInt() }
        for (n in ints) for (p in intPatterns) {
            assertEquals("$p of $n", String.format(Locale.ROOT, p, n), JavaFormat.format(p, n))
        }
        val longs = listOf(0L, 1234567890123L, -5L, Long.MAX_VALUE, Long.MIN_VALUE) + List(300) { random.nextLong() }
        for (n in longs) for (p in intPatterns) {
            assertEquals("$p of $n", String.format(Locale.ROOT, p, n), JavaFormat.format(p, n))
        }
    }

    @Test
    fun `규칙 코드의 여러 칸 꼴도 같다`() {
        val cases = listOf<Pair<String, Array<Any>>>(
            "%d.%d%%" to arrayOf(12, 5),
            "+%d.%d%%p" to arrayOf(3, 0),
            "%d'%02d\"" to arrayOf(5, 7),
            "%d:%02d:%02d" to arrayOf(1, 2, 3),
            "%d:%02d" to arrayOf(59, 9),
            "%d′%02d″/km" to arrayOf(6L, 30L),
            "R-%03d" to arrayOf(7),
            "%s · %s" to arrayOf("가", 3),
            "100%%" to arrayOf(),
        )
        for ((p, args) in cases) {
            assertEquals(p, String.format(Locale.ROOT, p, *args), JavaFormat.format(p, *args))
        }
    }

    @Test
    fun `정확한 값 반올림은 BigDecimal(v) 와 같다`() {
        val values = doubles() + listOf(1e20, 1.7976931348623157e308, 4.9e-324, 2.2250738585072014e-308, -1e-300) +
            List(3000) { Double.fromBits(random.nextLong()) }.filter { it.isFinite() }
        for (v in values) for (scale in 0..6) {
            val java = BigDecimal(v).setScale(scale, RoundingMode.HALF_UP).toPlainString()
            assertEquals("BigDecimal($v).setScale($scale)", java, ExactDecimal.halfUp(v, scale))
        }
    }

    @Test
    fun `짧은 글자 반올림은 BigDecimal valueOf 와 같다`() {
        for (v in doubles()) for (scale in 0..6) {
            val java = BigDecimal.valueOf(v).setScale(scale, RoundingMode.HALF_UP)
            assertEquals("valueOf($v).setScale($scale)", java.toDouble(), JavaFormat.shortHalfUp(v, scale).toDouble(), 0.0)
        }
    }

    @Test
    fun `글자 정리는 stripTrailingZeros toPlainString 과 같다`() {
        for (v in doubles()) {
            val java = BigDecimal(v).setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
            assertEquals("strip $v", java, stripDecimalZeros(ExactDecimal.halfUp(v, 6)))
        }
    }

    @Test
    fun `URL 인코딩은 URLEncoder 와 같다`() {
        val samples = listOf(
            "", "abc", "a b", "https://stepupcrew.com/wallet.html?l=abc.123&x=가나", "*-._~!'()", "러닝 크루 🏃‍♀️", "%+/=?#",
        ) + List(300) { String(CharArray(random.nextInt(0, 20)) { (random.nextInt(32, 0xD7FF)).toChar() }) }
        for (s in samples) assertEquals(s, URLEncoder.encode(s, "UTF-8"), UrlEncoding.encode(s))
    }

    @Test
    fun `글자 수 세기는 codePointCount offsetByCodePoints 와 같다`() {
        val samples = listOf("", "abc", "가나다", "🏃‍♀️ 달리기", "a\uD83C", "\uDC00b", "😀😀😀😀😀", "x😀y")
        for (s in samples) {
            val count = s.codePointCount(0, s.length)
            assertEquals(s, count, codePointCount(s))
            for (n in 0..count) assertEquals("$s/$n", s.offsetByCodePoints(0, n), codePointOffset(s, n))
            for (n in 0..count + 2) {
                val cut = s.codePoints().limit(n.toLong()).toArray()
                assertEquals("$s take $n", String(cut, 0, cut.size), takeCodePoints(s, n))
            }
            if (s.isNotEmpty()) assertEquals(s, String(Character.toChars(s.codePointAt(0))), firstCodePoint(s))
        }
    }

    @Test
    fun `반올림은 Math round 와 같다`() {
        for (v in doubles() + listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -0.5, -1.5, 0.49999999999999994)) {
            assertEquals("round $v", Math.round(v), javaRound(v))
        }
    }
}
