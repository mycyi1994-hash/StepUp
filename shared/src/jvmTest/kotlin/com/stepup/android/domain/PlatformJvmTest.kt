package com.stepup.android.domain

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.round
import kotlin.math.roundToLong
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/** 안드로이드에서 옮기기 전 JVM 호출과 비트까지 같은가(2026-11-02 KMP 2단계) — 땅따먹기 칸 · 기록 좌표 문자열이 바뀌지 않게 */
class PlatformJvmTest {
    private val values = Random(20261102).let { r -> List(20_000) { r.nextDouble(-400.0, 400.0) } } +
        listOf(0.0, -0.0, 0.5, 1.5, 2.5, -2.5, 2.675, 180.0, 90.0, 37.5665, 126.978)

    @Test fun `Math 와 같은 값`() {
        for (v in values) {
            assertEquals(Math.toRadians(v), toRadians(v), "toRadians($v)")
            assertEquals(Math.toDegrees(v), toDegrees(v), "toDegrees($v)")
            assertEquals(Math.rint(v), round(v), "rint($v)")
            assertEquals(Math.round(v * 1_000_000.0), (v * 1_000_000.0).roundToLong(), "round($v)")
        }
    }

    @Test fun `BigDecimal 반올림과 같은 값`() {
        for (v in values) {
            assertEquals(BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).toDouble(), roundHalfUp2(v), "coarse($v)")
        }
    }

    @Test fun `숫자 형식은 String_format 과 같은 글자`() {
        val extra = listOf(1.005, 0.125, 2.675, 0.05, 999.995, 1e-7, 123456.789, 1e21, 5e-324, 0.0, -0.0, -1.005, -0.001)
        for (v in values + extra) {
            for (d in 0..4) {
                val expected = String.format(java.util.Locale.ROOT, "%.${d}f", v)
                assertEquals(expected, formatFixed(v, d), "formatFixed($v, $d)")
                assertEquals(expected, formatFixedPortable(v, d), "formatFixedPortable($v, $d)")
            }
            assertEquals(String.format(java.util.Locale.ROOT, "%,.2f", v), formatGrouped(v, 2), "grouped($v)")
        }
        for (n in listOf(0L, 7L, 999L, 1000L, 1234567L, -1234567L, Long.MAX_VALUE)) {
            assertEquals(String.format(java.util.Locale.ROOT, "%,d", n), formatGrouped(n))
        }
        for (n in listOf(0L, 5L, 59L, 123L, -5L)) assertEquals(String.format("%02d", n), zeroPad(n, 2))
    }

    @Test fun `BigDecimal 반올림 글자도 같다`() {
        for (v in values + listOf(0.5, 1.5, 2.5, 0.125, 0.15, 126.0 / 160 * 100, 1e-7, -0.0, 123456789.0123456)) {
            for (s in 0..6) {
                val expected = BigDecimal(v).setScale(s, RoundingMode.HALF_UP).toPlainString()
                assertEquals(expected, exactHalfUp(v, s), "exactHalfUp($v, $s)")
                assertEquals(expected, exactHalfUpPortable(v, s), "exactHalfUpPortable($v, $s)")
            }
        }
    }

    @Test fun `반올림 · 나머지 · 해시 · URL 인코딩`() {
        for (v in values + listOf(0.49999999999999994, 2.5, -2.5, Double.NaN, 1e300, -1e300)) {
            assertEquals(Math.round(v), javaRound(v), "javaRound($v)")
        }
        for (a in listOf(-7, -1, 0, 3, Int.MIN_VALUE, Int.MAX_VALUE)) assertEquals(Math.floorMod(a, 6), javaFloorMod(a, 6))
        for (t in listOf("", "a", "강남 러너스", "😀crew", "River runners 123")) {
            assertEquals(t.hashCode(), javaHash(t))
            assertEquals(java.net.URLEncoder.encode(t, "UTF-8"), urlEncodeUtf8(t))
        }
        val page = "https://stepupcrew.com/wallet.html?l=abc.123&x=a b*~"
        assertEquals(java.net.URLEncoder.encode(page, "UTF-8"), urlEncodeUtf8(page))
    }

    @Test fun `글자 세기 · 한중일 글자 판별`() {
        for (t in listOf("", "abc", "강남", "😀😀a", "a\uD83D", "\uDE00b")) {
            assertEquals(t.codePointCount(0, t.length), t.codePointLength(), "count($t)")
            if (t.isNotEmpty()) assertEquals(String(Character.toChars(t.codePointAt(0))), t.firstCodePoint())
        }
        assertEquals("😀😀a".offsetByCodePoints(0, 2), "😀😀a".offsetOfCodePoints(2))
        val cjk = setOf(Character.UnicodeScript.HANGUL, Character.UnicodeScript.HAN, Character.UnicodeScript.HIRAGANA, Character.UnicodeScript.KATAKANA)
        // 이 JDK 가 아직 모르는 글자(새 유니코드 판에서 생긴 글자 — CI 의 JDK 17 은 유니코드 13)는 맞대어 볼 수 없어 뺀다.
        // 표(isCjkNameLetter)는 안드로이드 15 의 유니코드 15 에 맞춘 것이다.
        val wrong = (0..0xFFFF).filter { c -> Character.getType(c) != Character.UNASSIGNED.toInt() }
            .filter { c -> (Character.UnicodeScript.of(c) in cjk) != isCjkNameLetter(c.toChar()) }
        assertEquals(emptyList(), wrong.map { "%04X".format(it) }.take(40), "한중일 판별이 다른 글자")
    }
}
