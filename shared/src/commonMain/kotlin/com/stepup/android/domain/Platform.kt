package com.stepup.android.domain

import kotlin.math.roundToLong

/**
 * 공용 규칙이 쓰는 JVM 함수의 대신(2026-11-02 KMP 2단계). 안드로이드에서 예전과 비트까지 같은 값을 내야 한다 —
 * 땅따먹기 칸(Territory)은 서버(Postgres)와 같은 칸 이름을 만들어야 하고, 기록 좌표 문자열도 같아야 한다.
 */

/** java.lang.Math.toRadians 와 같다(곱하는 상수가 같다 — PI / 180) */
internal fun toRadians(degrees: Double): Double = degrees * DEGREES_TO_RADIANS

/** java.lang.Math.toDegrees 와 같다(180 / PI) */
internal fun toDegrees(radians: Double): Double = radians * RADIANS_TO_DEGREES

private const val DEGREES_TO_RADIANS = 0.017453292519943295
private const val RADIANS_TO_DEGREES = 57.29577951308232

/** 지금 시각(1970-01-01 UTC 부터 ms) — 안드로이드는 System.currentTimeMillis() */
expect fun epochMillis(): Long

/**
 * 소수 둘째 자리 반올림(0.5 는 0 에서 먼 쪽) — 십진수로 본 값에서 자른다(java.math.BigDecimal.valueOf(v).setScale(2, HALF_UP)).
 * 이진 부동소수로 곱하고 나누면 2.675 처럼 경계 값이 달라져 서버 좌표(crew_coarse)와 어긋날 수 있어 기기마다 십진 계산을 쓴다.
 */
expect fun roundHalfUp2(value: Double): Double

/** String.format(Locale.ROOT, "%.{decimals}f", value) 와 같다 — 안드로이드는 그대로 String.format, iOS 는 [formatFixedPortable] */
expect fun formatFixed(value: Double, decimals: Int): String

/** BigDecimal(value).setScale(scale, HALF_UP).toPlainString() 와 같다(이진 값 그대로의 십진) — iOS 는 [exactHalfUpPortable] */
expect fun exactHalfUp(value: Double, scale: Int): String

/** "%,.{decimals}f"(Locale.ROOT) — 천 단위 쉼표 */
fun formatGrouped(value: Double, decimals: Int): String = groupThousands(formatFixed(value, decimals))

/** "%,d"(Locale.ROOT) */
fun formatGrouped(value: Long): String = groupThousands(value.toString())

/** "%0{width}d" — 음수는 부호까지 넓이에 든다(java.util.Formatter 와 같다) */
fun zeroPad(value: Long, width: Int): String =
    if (value < 0) "-" + (-value).toString().padStart(width - 1, '0') else value.toString().padStart(width, '0')

fun zeroPad(value: Int, width: Int): String = zeroPad(value.toLong(), width)

/** java.lang.Math.round(Double) — 0.5 는 큰 쪽, NaN 은 0 */
fun javaRound(value: Double): Long = if (value.isNaN()) 0L else value.roundToLong()

/** java.lang.Math.floorMod(Int, Int) */
fun javaFloorMod(value: Int, mod: Int): Int = ((value % mod) + mod) % mod

/** java.lang.String.hashCode() — 플랫폼마다 hashCode 가 같다는 보장이 없어 직접 센다(크루 배경 고르기가 두 앱에서 같게) */
fun javaHash(text: String): Int {
    var h = 0
    for (c in text) h = 31 * h + c.code
    return h
}

/** java.net.URLEncoder.encode(text, "UTF-8") — 글자 · 숫자 · .-*_ 는 그대로, 공백은 +, 나머지는 UTF-8 바이트 %XX */
fun urlEncodeUtf8(text: String): String = buildString {
    for (b in text.encodeToByteArray()) {
        val c = (b.toInt() and 0xff)
        val ch = c.toChar()
        when {
            ch in 'a'..'z' || ch in 'A'..'Z' || ch in '0'..'9' || ch == '.' || ch == '-' || ch == '*' || ch == '_' -> append(ch)
            ch == ' ' -> append('+')
            else -> { append('%'); append(HEX[c shr 4]); append(HEX[c and 0xf]) }
        }
    }
}

private const val HEX = "0123456789ABCDEF"

private fun groupThousands(number: String): String {
    val negative = number.startsWith("-")
    val body = if (negative) number.substring(1) else number
    val dot = body.indexOf('.')
    val intPart = if (dot >= 0) body.substring(0, dot) else body
    val rest = if (dot >= 0) body.substring(dot) else ""
    if (intPart.any { !it.isDigit() }) return number
    val grouped = intPart.reversed().chunked(3).joinToString(",").reversed()
    return (if (negative) "-" else "") + grouped + rest
}

/**
 * java.util.Formatter 의 "%.nf" 를 따라 한다 — 가장 짧은 십진 표기(Double.toString)의 자리에서 0.5 를 올린다(HALF_UP).
 * 그래서 1.005 는 "1.01"(C 의 printf 는 이진 값으로 보아 "1.00"). JVM 검사(PlatformJvmTest)가 String.format 과 맞대어 본다.
 */
internal fun formatFixedPortable(value: Double, decimals: Int): String {
    if (value.isNaN()) return "NaN"
    if (value.isInfinite()) return if (value > 0) "Infinity" else "-Infinity"
    val negative = value < 0 || (value == 0.0 && 1.0 / value < 0)
    val (digits, pointPos) = shortestDigits(kotlin.math.abs(value))
    return (if (negative) "-" else "") + roundDigits(digits, pointPos, decimals)
}

/** BigDecimal(value) — 이진 값 그대로의 십진 자리 — 를 scale 자리에서 HALF_UP 으로 */
internal fun exactHalfUpPortable(value: Double, scale: Int): String {
    require(!value.isNaN() && !value.isInfinite()) { "BigDecimal 은 NaN · 무한을 받지 않는다" }
    val (digits, pointPos) = exactDigits(kotlin.math.abs(value))
    val body = roundDigits(digits, pointPos, scale)
    val zero = body.all { it == '0' || it == '.' }
    return if (value < 0 && !zero) "-$body" else body
}

/** 자리 문자열 [digits](앞 0 없음 · 소수점 없음)와 소수점 위치(앞에서 몇 자리 뒤) — 자리 [decimals] 에서 HALF_UP, 정확히 그 자릿수로 */
private fun roundDigits(digits: String, pointPos: Int, decimals: Int): String {
    // 정수부 · 소수부로 펼친다
    val intDigits = if (pointPos <= 0) "0" else digits.take(pointPos).padEnd(pointPos, '0')
    val fracAll = if (pointPos >= 0) digits.drop(pointPos) else "0".repeat(-pointPos) + digits
    val keep = fracAll.take(decimals).padEnd(decimals, '0')
    val roundUp = fracAll.length > decimals && fracAll[decimals] >= '5'
    var number = (intDigits + keep).toCharArray()
    if (roundUp) {
        var i = number.size - 1
        while (i >= 0) {
            if (number[i] == '9') { number[i] = '0'; i-- } else { number[i] = number[i] + 1; break }
        }
        if (i < 0) number = charArrayOf('1') + number
    }
    val s = number.concatToString()
    val intLen = s.length - decimals
    val intPart = s.take(intLen).trimStart('0').ifEmpty { "0" }
    return if (decimals > 0) intPart + "." + s.drop(intLen) else intPart
}

/** 가장 짧은 십진 표기의 자리와 소수점 위치(Double.toString 이 고르는 자리) */
private fun shortestDigits(value: Double): Pair<String, Int> {
    val text = value.toString().lowercase()
    val ePos = text.indexOf('e')
    val mantissa = if (ePos >= 0) text.substring(0, ePos) else text
    val exp = if (ePos >= 0) text.substring(ePos + 1).toInt() else 0
    val dot = mantissa.indexOf('.')
    val intPart = if (dot >= 0) mantissa.substring(0, dot) else mantissa
    val frac = if (dot >= 0) mantissa.substring(dot + 1) else ""
    var digits = intPart + frac
    var pointPos = intPart.length + exp
    val lead = digits.indexOfFirst { it != '0' }
    if (lead < 0) return "0" to 1
    digits = digits.substring(lead)
    pointPos -= lead
    return digits.trimEnd('0').ifEmpty { "0" } to pointPos
}

/** 이진 값 그대로의 십진 자리 — value = m × 2^e 를 m × 5^-e / 10^-e 로 펼친다 */
private fun exactDigits(value: Double): Pair<String, Int> {
    if (value == 0.0) return "0" to 1
    val bits = value.toRawBits()
    val rawExp = ((bits ushr 52) and 0x7ff).toInt()
    var mantissa = bits and 0xfffffffffffffL
    var exp: Int
    if (rawExp == 0) exp = -1074 else { mantissa = mantissa or (1L shl 52); exp = rawExp - 1075 }
    val big = BigNat.of(mantissa)
    return if (exp >= 0) {
        repeat(exp) { big.times(2) }
        val d = big.toDecimal()
        d to d.length
    } else {
        repeat(-exp) { big.times(5) }
        val d = big.toDecimal()
        // 소수점은 끝에서 -exp 자리 앞
        d to (d.length + exp)
    }
}

/** 작은 큰 정수(10^9 진법) — exactDigits 만 쓴다 */
private class BigNat private constructor(private val limbs: MutableList<Long>) {
    fun times(k: Int) {
        var carry = 0L
        for (i in limbs.indices) {
            val v = limbs[i] * k + carry
            limbs[i] = v % BASE
            carry = v / BASE
        }
        while (carry > 0) { limbs.add(carry % BASE); carry /= BASE }
    }

    fun toDecimal(): String = buildString {
        append(limbs.last())
        for (i in limbs.size - 2 downTo 0) append(limbs[i].toString().padStart(9, '0'))
    }

    companion object {
        private const val BASE = 1_000_000_000L
        fun of(value: Long): BigNat {
            val l = mutableListOf<Long>()
            var v = value
            if (v == 0L) l.add(0)
            while (v > 0) { l.add(v % BASE); v /= BASE }
            return BigNat(l)
        }
    }
}

private val monotonicOrigin = kotlin.time.TimeSource.Monotonic.markNow()

/** 멈추지 않고 되돌아가지 않는 시계(ms) — 차이만 뜻이 있다(System.nanoTime() / 1_000_000 대신) */
fun monotonicMillis(): Long = monotonicOrigin.elapsedNow().inWholeMilliseconds
