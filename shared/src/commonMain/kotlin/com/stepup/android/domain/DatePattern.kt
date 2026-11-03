package com.stepup.android.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * java.time.format.DateTimeFormatter.ofPattern(pattern, locale).format(…) 중 앱이 쓰는 글자만(2026-11-03 iOS C3) —
 * y · M(1 ~ 4) · d · E(1 ~ 4) · a · H · h · m · s 와 '따옴표 글자'. 앱이 담는 네 언어(ko · en · ja · zh)의 이름은
 * JDK(CLDR)와 같다 — JVM 검사(DatePatternJvmTest)가 DateTimeFormatter 와 맞대어 본다. 모르는 글자는 IllegalArgumentException.
 */
object DatePattern {
    fun format(at: LocalDateTime, pattern: String, language: String = "en"): String = render(at.date, at.time, pattern, language)

    fun format(date: LocalDate, pattern: String, language: String = "en"): String = render(date, null, pattern, language)

    fun format(time: LocalTime, pattern: String, language: String = "en"): String = render(null, time, pattern, language)

    fun format(epochMillis: Long, zone: TimeZone, pattern: String, language: String = "en"): String =
        format(Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone), pattern, language)

    /** DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) — 네 언어(JDK · CLDR 와 같은 꼴) */
    fun mediumDate(language: String, date: LocalDate): String = when (lang(language)) {
        "ko" -> "${date.year}. ${date.month.ordinal + 1}. ${date.day}."
        "ja" -> "${date.year}/${zeroPad(date.month.ordinal + 1, 2)}/${zeroPad(date.day, 2)}"
        "zh" -> "${date.year}年${date.month.ordinal + 1}月${date.day}日"
        else -> "${Names.of("en").monthShort[date.month.ordinal]} ${date.day}, ${date.year}"
    }

    fun mediumDate(language: String, at: LocalDateTime): String = mediumDate(language, at.date)

    /** DayOfWeek.getDisplayName(style, locale) */
    fun dayName(day: DayOfWeek, style: DateNameStyle, language: String): String {
        val names = Names.of(language)
        val i = day.ordinal
        return when (style) {
            DateNameStyle.FULL -> names.dayLong[i]
            DateNameStyle.SHORT -> names.dayShort[i]
            DateNameStyle.NARROW -> when (lang(language)) {
                "en" -> names.dayLong[i].take(1)
                "zh" -> "一二三四五六日"[i].toString()
                else -> names.dayShort[i]
            }
        }
    }

    internal fun lang(language: String): String = language.lowercase().substringBefore('-').substringBefore('_').let {
        if (it in setOf("ko", "ja", "zh")) it else "en"
    }

    private fun render(date: LocalDate?, time: LocalTime?, pattern: String, language: String): String {
        val names = Names.of(language)
        val out = StringBuilder()
        var i = 0
        while (i < pattern.length) {
            val c = pattern[i]
            if (c == '\'') {
                val end = pattern.indexOf('\'', i + 1)
                require(end > 0) { "닫는 따옴표가 없다: $pattern" }
                if (end == i + 1) out.append('\'') else out.append(pattern, i + 1, end)
                i = end + 1
                continue
            }
            if (c !in 'a'..'z' && c !in 'A'..'Z') { out.append(c); i++; continue }
            var n = 1
            while (i + n < pattern.length && pattern[i + n] == c) n++
            fun d() = requireNotNull(date) { "날짜가 없는 값에 '$c'" }
            fun t() = requireNotNull(time) { "시각이 없는 값에 '$c'" }
            when (c) {
                'y', 'u' -> {
                    val y = d().year
                    out.append(if (n == 2) zeroPad(y % 100, 2) else zeroPad(y, n))
                }
                'M', 'L' -> {
                    val m = d().month.ordinal + 1
                    out.append(when (n) { 1 -> m.toString(); 2 -> zeroPad(m, 2); 3 -> names.monthShort[m - 1]; else -> names.monthLong[m - 1] })
                }
                'd' -> out.append(zeroPad(d().day, n))
                'E' -> {
                    val w = d().dayOfWeek.ordinal // 월요일 0
                    out.append(if (n >= 4) names.dayLong[w] else names.dayShort[w])
                }
                'a' -> out.append(if (t().hour < 12) names.am else names.pm)
                'H' -> out.append(zeroPad(t().hour, n))
                'h' -> out.append(zeroPad((t().hour % 12).let { if (it == 0) 12 else it }, n))
                'm' -> out.append(zeroPad(t().minute, n))
                's' -> out.append(zeroPad(t().second, n))
                else -> throw IllegalArgumentException("모르는 날짜 글자 '$c': $pattern")
            }
            i += n
        }
        return out.toString()
    }

    private class Names(
        val monthShort: List<String>, val monthLong: List<String>,
        val dayShort: List<String>, val dayLong: List<String>,
        val am: String, val pm: String,
    ) {
        companion object {
            private val numbered = { suffix: String -> (1..12).map { "$it$suffix" } }
            private val KO = Names(numbered("월"), numbered("월"), "월화수목금토일".map { "$it" }, "월화수목금토일".map { "${it}요일" }, "오전", "오후")
            private val JA = Names(numbered("月"), numbered("月"), "月火水木金土日".map { "$it" }, "月火水木金土日".map { "${it}曜日" }, "午前", "午後")
            private val ZH = Names(
                numbered("月"), listOf("一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"),
                "一二三四五六日".map { "周$it" }, "一二三四五六日".map { "星期$it" }, "上午", "下午",
            )
            private val EN = Names(
                listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
                listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
                listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"),
                "AM", "PM",
            )

            fun of(language: String): Names = when (language.lowercase().substringBefore('-').substringBefore('_')) {
                "ko" -> KO
                "ja" -> JA
                "zh" -> ZH
                else -> EN
            }
        }
    }
}

enum class DateNameStyle { NARROW, SHORT, FULL }

/** DayOfWeek.getDisplayName(TextStyle, Locale) 자리 */
fun DayOfWeek.displayName(style: DateNameStyle, language: String): String = DatePattern.dayName(this, style, language)
