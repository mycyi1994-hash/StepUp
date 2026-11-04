package com.stepup.android.domain

import kotlin.math.abs

/**
 * 아이폰용 `String.format(Locale.ROOT, …)` — 규칙 코드가 쓰는 꼴만: `%d` · `%f` · `%s` · `%%`, 깃발 `,` `0` `+` `-`, 너비, `.자리`.
 * 숫자는 Java 와 같이 "짧은 글자(toString)의 숫자를 반올림(HALF_UP)"한다 — 그래서 2.675 의 `%.2f` 는 "2.68" 이다(C printf 는 "2.67").
 * Java 와 같은 글자인지는 PlatformTwinTest 가 맞대어 본다. 모르는 꼴은 조용히 틀리지 않고 예외를 던진다.
 */
internal object JavaFormat {

    fun format(pattern: String, vararg args: Any): String {
        val out = StringBuilder()
        var argIndex = 0
        var i = 0
        while (i < pattern.length) {
            val c = pattern[i]
            if (c != '%') {
                out.append(c); i++; continue
            }
            i++
            require(i < pattern.length) { "format: '%' at end of \"$pattern\"" }
            var comma = false; var zero = false; var plus = false; var left = false
            while (i < pattern.length && pattern[i] in ",0+-") {
                when (pattern[i]) {
                    ',' -> comma = true
                    '0' -> zero = true
                    '+' -> plus = true
                    '-' -> left = true
                }
                i++
            }
            var width = -1
            while (i < pattern.length && pattern[i].isDigit()) {
                width = (if (width < 0) 0 else width) * 10 + (pattern[i] - '0'); i++
            }
            var precision = -1
            if (i < pattern.length && pattern[i] == '.') {
                i++
                precision = 0
                while (i < pattern.length && pattern[i].isDigit()) {
                    precision = precision * 10 + (pattern[i] - '0'); i++
                }
            }
            require(i < pattern.length) { "format: unfinished conversion in \"$pattern\"" }
            val conv = pattern[i]; i++
            if (conv == '%') {
                out.append(pad("%", width, left, zero = false)); continue
            }
            require(argIndex < args.size) { "format: missing argument for %$conv in \"$pattern\"" }
            val arg = args[argIndex++]
            val body = when (conv) {
                'd' -> {
                    val n = when (arg) {
                        is Int -> arg.toLong()
                        is Long -> arg
                        is Short -> arg.toLong()
                        is Byte -> arg.toLong()
                        else -> throw IllegalArgumentException("format: %d needs an integer, got ${arg::class.simpleName}")
                    }
                    val neg = n < 0
                    val digits = if (n == Long.MIN_VALUE) "9223372036854775808" else abs(n).toString()
                    signed(group(digits, comma), neg, plus, width, left, zero)
                }
                'f' -> {
                    val v = when (arg) {
                        is Double -> arg
                        is Float -> arg.toDouble()
                        else -> throw IllegalArgumentException("format: %f needs a floating number, got ${arg::class.simpleName}")
                    }
                    if (v.isNaN()) pad("NaN", width, left, zero = false)
                    else {
                        val neg = v < 0.0 || (v == 0.0 && 1.0 / v < 0.0)
                        if (v.isInfinite()) pad(if (neg) "-Infinity" else if (plus) "+Infinity" else "Infinity", width, left, zero = false)
                        else {
                            val p = if (precision < 0) 6 else precision
                            val (intPart, frac) = roundShort(abs(v), p)
                            val text = group(intPart, comma) + if (p > 0) ".$frac" else ""
                            signed(text, neg, plus, width, left, zero)
                        }
                    }
                }
                's' -> {
                    val s = arg.toString()
                    pad(if (precision >= 0 && precision < s.length) s.substring(0, precision) else s, width, left, zero = false)
                }
                else -> throw IllegalArgumentException("format: unsupported conversion %$conv in \"$pattern\"")
            }
            out.append(body)
        }
        return out.toString()
    }

    /** `BigDecimal.valueOf(v).setScale(scale, HALF_UP).toPlainString()` */
    fun shortHalfUp(v: Double, scale: Int): String {
        require(!v.isNaN() && !v.isInfinite()) { "shortHalfUp: $v" }
        val (intPart, frac) = roundShort(abs(v), scale)
        val text = if (scale > 0) "$intPart.$frac" else intPart
        val isZero = text.all { it == '0' || it == '.' }
        return if (v < 0.0 && !isZero) "-$text" else text
    }

    /** 0 이상인 v 의 짧은 글자 숫자를 소수 [scale] 자리로 HALF_UP — (정수 부분, 소수 부분) */
    internal fun roundShort(v: Double, scale: Int): Pair<String, String> {
        val (digits, point) = shortDigits(v)
        return DecimalRounding.halfUp(digits, point, scale)
    }

    /**
     * v(0 이상)의 toString 숫자 — (숫자들, 소수점 위치). 값 = 0.숫자들 × 10^위치 가 아니라
     * "숫자들의 앞 [point] 자리가 정수 부분"이다(point 는 음수일 수 있다).
     */
    internal fun shortDigits(v: Double): Pair<String, Int> {
        val s = v.toString()
        val ePos = s.indexOfFirst { it == 'E' || it == 'e' }
        val mantissa = if (ePos >= 0) s.substring(0, ePos) else s
        val exp = if (ePos >= 0) s.substring(ePos + 1).toInt() else 0
        val dot = mantissa.indexOf('.')
        val intDigits = if (dot >= 0) mantissa.substring(0, dot) else mantissa
        val fracDigits = if (dot >= 0) mantissa.substring(dot + 1) else ""
        return Pair(intDigits + fracDigits, intDigits.length + exp)
    }

    private fun group(intDigits: String, comma: Boolean): String {
        if (!comma || intDigits.length <= 3) return intDigits
        val sb = StringBuilder()
        val first = intDigits.length % 3
        if (first > 0) sb.append(intDigits, 0, first)
        var k = first
        while (k < intDigits.length) {
            if (sb.isNotEmpty()) sb.append(',')
            sb.append(intDigits, k, k + 3)
            k += 3
        }
        return sb.toString()
    }

    private fun signed(text: String, neg: Boolean, plus: Boolean, width: Int, left: Boolean, zero: Boolean): String {
        val sign = if (neg) "-" else if (plus) "+" else ""
        if (zero && !left && width > sign.length + text.length) {
            return sign + "0".repeat(width - sign.length - text.length) + text
        }
        return pad(sign + text, width, left, zero = false)
    }

    private fun pad(s: String, width: Int, left: Boolean, zero: Boolean): String {
        if (width <= s.length) return s
        val fill = (if (zero) "0" else " ").repeat(width - s.length)
        return if (left) s + fill else fill + s
    }
}

/** 10진 숫자 글자를 HALF_UP 으로 자르는 계산 — JavaFormat(짧은 글자) · ExactDecimal(정확한 값)이 함께 쓴다 */
internal object DecimalRounding {

    /**
     * [digits] 의 앞 [point] 자리가 정수 부분인 0 이상의 수를 소수 [scale] 자리로 HALF_UP.
     * 돌려주는 정수 부분은 앞 0 이 없고(0 이면 "0"), 소수 부분은 꼭 [scale] 자리다.
     */
    fun halfUp(digits: String, point: Int, scale: Int): Pair<String, String> {
        // 정수 부분 · 소수 부분으로 펼친다(point 가 음수면 앞에 0, digits 보다 크면 뒤에 0)
        val intRaw: String
        val fracRaw: String
        when {
            point <= 0 -> { intRaw = "0"; fracRaw = "0".repeat(-point) + digits }
            point >= digits.length -> { intRaw = digits + "0".repeat(point - digits.length); fracRaw = "" }
            else -> { intRaw = digits.substring(0, point); fracRaw = digits.substring(point) }
        }
        val kept = (intRaw + fracRaw.padEnd(scale, '0').substring(0, scale)).toCharArray()
        val roundUp = fracRaw.length > scale && fracRaw[scale] >= '5'
        var result = kept.concatToString()
        if (roundUp) {
            var j = kept.size - 1
            while (j >= 0) {
                if (kept[j] == '9') { kept[j] = '0'; j-- } else { kept[j] = kept[j] + 1; break }
            }
            result = if (j < 0) "1" + kept.concatToString() else kept.concatToString()
        }
        val intLen = result.length - scale
        val intPart = result.substring(0, intLen).trimStart('0').ifEmpty { "0" }
        return Pair(intPart, result.substring(intLen))
    }
}

/** 아이폰용 `BigDecimal(v).setScale(scale, HALF_UP).toPlainString()` — double 의 정확한 이진 값을 10진으로 펼쳐 반올림한다 */
internal object ExactDecimal {

    fun halfUp(v: Double, scale: Int): String {
        if (v.isNaN() || v.isInfinite()) throw NumberFormatException("Infinite or NaN")
        val (digits, point) = exactDigits(abs(v))
        val (intPart, frac) = DecimalRounding.halfUp(digits, point, scale)
        val text = if (scale > 0) "$intPart.$frac" else intPart
        val isZero = text.all { it == '0' || it == '.' }
        return if (v < 0.0 && !isZero) "-$text" else text
    }

    /** 0 이상 v 의 정확한 10진 숫자 — (숫자들, 정수 자리 수) */
    internal fun exactDigits(v: Double): Pair<String, Int> {
        if (v == 0.0) return Pair("0", 1)
        val bits = v.toRawBits()
        val expBits = ((bits ushr 52) and 0x7FF).toInt()
        val fraction = bits and 0xFFFFFFFFFFFFFL
        val mantissa = if (expBits == 0) fraction else fraction or (1L shl 52)
        val exp2 = if (expBits == 0) -1074 else expBits - 1075
        // v = mantissa × 2^exp2
        val big = BigNat.of(mantissa)
        return if (exp2 >= 0) {
            repeat(exp2) { big.times(2) }
            val s = big.toDecimal()
            Pair(s, s.length)
        } else {
            // mantissa × 2^-k = mantissa × 5^k / 10^k
            val k = -exp2
            var left = k
            while (left >= 13) { big.times(1_220_703_125); left -= 13 } // 5^13
            var rest = 1
            repeat(left) { rest *= 5 }
            if (rest > 1) big.times(rest)
            val s = big.toDecimal()
            Pair(s, s.length - k)
        }
    }

    /** 아주 작은 자연수 셈(10^9 진 칸, 낮은 칸부터) — 곱하기 · 10진 글자만 */
    private class BigNat private constructor(private var limbs: LongArray, private var size: Int) {
        fun times(m: Int) {
            var carry = 0L
            for (j in 0 until size) {
                val x = limbs[j] * m + carry
                limbs[j] = x % BASE
                carry = x / BASE
            }
            while (carry > 0) {
                if (size == limbs.size) limbs = limbs.copyOf(size * 2)
                limbs[size++] = carry % BASE
                carry /= BASE
            }
        }

        fun toDecimal(): String {
            val sb = StringBuilder(limbs[size - 1].toString())
            for (j in size - 2 downTo 0) sb.append(limbs[j].toString().padStart(9, '0'))
            return sb.toString()
        }

        companion object {
            const val BASE = 1_000_000_000L
            fun of(n: Long): BigNat {
                val arr = LongArray(8)
                var size = 0
                var x = n
                do { arr[size++] = x % BASE; x /= BASE } while (x > 0)
                return BigNat(arr, size)
            }
        }
    }
}

/** 아이폰용 `URLEncoder.encode(s, "UTF-8")` — 영문 · 숫자 · `.-*_` 는 그대로, 빈칸은 `+`, 나머지는 UTF-8 `%XX`(대문자) */
internal object UrlEncoding {
    private const val HEX = "0123456789ABCDEF"

    fun encode(s: String): String {
        val sb = StringBuilder()
        for (b in s.encodeToByteArray()) {
            val c = (b.toInt() and 0xFF)
            val ch = c.toChar()
            when {
                ch in 'a'..'z' || ch in 'A'..'Z' || ch in '0'..'9' || ch == '.' || ch == '-' || ch == '*' || ch == '_' -> sb.append(ch)
                ch == ' ' -> sb.append('+')
                else -> sb.append('%').append(HEX[c shr 4]).append(HEX[c and 0xF])
            }
        }
        return sb.toString()
    }
}
