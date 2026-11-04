package com.stepup.android.domain

import kotlin.math.roundToLong

// 규칙 코드가 쓰던 Java 함수 자리(아이폰에는 Java 가 없다).
// 안드로이드 구현(androidMain)은 예전 Java 함수를 그대로 부른다 — 안드로이드 동작은 한 글자도 바뀌지 않는다.
// 아이폰 구현(iosMain)은 같은 결과를 내는 순수 Kotlin(JavaFormat · ExactDecimal)을 쓰고,
// 그 순수 Kotlin 이 Java 와 같은지는 안드로이드 단위 테스트가 맞대어 본다(PlatformTwinTest).

/** 지금 시각(1970 부터 ms) — 예전 `System.currentTimeMillis()` */
expect fun nowMillis(): Long

/** 예전 `String.format(Locale.ROOT, pattern, …)` · `pattern.format(Locale.ROOT, …)` */
expect fun formatRoot(pattern: String, vararg args: Any): String

/** 예전 `String.format(Locale.US, pattern, …)` */
expect fun formatUs(pattern: String, vararg args: Any): String

/** 예전 `pattern.format(…)` — 기기 언어를 따르던 자리(아이폰은 ROOT 와 같다) */
expect fun formatLocal(pattern: String, vararg args: Any): String

/** 예전 `Math.toRadians` */
expect fun toRadians(degrees: Double): Double

/** 예전 `Math.toDegrees` */
expect fun toDegrees(radians: Double): Double

/** 예전 `URLEncoder.encode(s, "UTF-8")` */
expect fun urlEncodeUtf8(s: String): String

/** 예전 `BigDecimal(v).setScale(scale, RoundingMode.HALF_UP).toPlainString()` — v 의 정확한 이진 값을 반올림한다 */
expect fun exactHalfUp(v: Double, scale: Int): String

/** 예전 `BigDecimal.valueOf(v).setScale(scale, RoundingMode.HALF_UP).toDouble()` — v 의 짧은 글자(toString)를 반올림한다 */
expect fun shortHalfUp(v: Double, scale: Int): Double

/** 예전 `BigDecimal(…).stripTrailingZeros().toPlainString()` 의 글자 정리 — "1.500" → "1.5", "2.000" → "2" */
fun stripDecimalZeros(plain: String): String {
    if ('.' !in plain) return plain
    val s = plain.trimEnd('0').trimEnd('.')
    return if (s == "-0" || s.isEmpty()) "0" else s
}

/** 예전 `Math.round(x)` — 가장 가까운 정수(반은 올림), NaN 은 0(Kotlin `roundToLong` 은 NaN 에 예외라 따로 둔다) */
fun javaRound(x: Double): Long = if (x.isNaN()) 0L else x.roundToLong()

/** 예전 `String(Character.toChars(s.codePointAt(0)))` — 첫 글자 하나(이모지 같은 두 칸 글자는 두 칸 그대로) */
fun firstCodePoint(s: String): String =
    if (s.length >= 2 && s[0].isHighSurrogate() && s[1].isLowSurrogate()) s.substring(0, 2) else s.substring(0, 1)

/** 예전 `s.codePointCount(0, s.length)` — 글자 수(이모지 같은 두 칸 글자는 하나로, 짝 없는 칸은 하나씩) */
fun codePointCount(s: String): Int {
    var i = 0
    var n = 0
    while (i < s.length) {
        i += codePointWidth(s, i)
        n++
    }
    return n
}

/** 예전 `s.offsetByCodePoints(0, count)` — 앞에서 [count] 글자 뒤의 칸 번호. 글자가 모자라면 예외(Java 와 같다) */
fun codePointOffset(s: String, count: Int): Int {
    var i = 0
    repeat(count) {
        if (i >= s.length) throw IndexOutOfBoundsException("codePointOffset: $count > ${codePointCount(s)}")
        i += codePointWidth(s, i)
    }
    return i
}

/** 앞에서 [count] 글자까지(모자라면 전부) — 예전 `codePoints().limit(count)` 로 다시 만든 글자 */
fun takeCodePoints(s: String, count: Int): String {
    var i = 0
    var n = 0
    while (i < s.length && n < count) {
        i += codePointWidth(s, i)
        n++
    }
    return s.substring(0, i)
}

private fun codePointWidth(s: String, i: Int): Int =
    if (s[i].isHighSurrogate() && i + 1 < s.length && s[i + 1].isLowSurrogate()) 2 else 1
