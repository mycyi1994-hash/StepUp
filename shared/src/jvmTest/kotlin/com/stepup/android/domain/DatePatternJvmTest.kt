package com.stepup.android.domain

import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime

/** 공용 DatePattern 이 JDK DateTimeFormatter(안드로이드 화면이 쓰던 것)와 같은 글자를 내는가 — 네 언어 · 앱이 쓰는 꼴 */
class DatePatternJvmTest {
    private val patterns = listOf(
        "HH:mm", "a hh:mm", "a h:mm", "M/d", "M/d E", "M/d HH:mm", "M.d HH:mm", "M.d (E)", "yyyy.MM.dd", "yyyy.MM.dd HH:mm",
        "yyyy-MM-dd", "MMM d", "MMMM d, EEEE", "M월 d일 EEEE", "yyyy'년' M'월' d'일'", "EEEE", "E", "yy.MM.dd", "h:mm a", "HH:mm:ss",
    )

    @Test fun matchesJdk() {
        val times = listOf(
            LocalDateTime(2026, 1, 5, 0, 7, 9), LocalDateTime(2026, 11, 3, 12, 0, 0), LocalDateTime(2025, 12, 31, 23, 59, 59),
            LocalDateTime(2026, 6, 14, 9, 30, 5), LocalDateTime(2026, 2, 28, 13, 5, 0), LocalDateTime(2024, 9, 1, 11, 59, 0),
        ) + (0 until 7).map { LocalDateTime(2026, 3, 2 + it, 7 + it, 3 * it, 0) }
        for (lang in listOf("ko", "en", "ja", "zh")) {
            val locale = Locale.forLanguageTag(lang)
            for (p in patterns) for (t in times) {
                val jdk = DateTimeFormatter.ofPattern(p, locale).format(t.toJavaLocalDateTime())
                assertEquals(jdk, DatePattern.format(t, p, lang), "$lang · '$p' · $t")
            }
        }
    }
}

class DateNamesJvmTest {
    @Test fun mediumDateAndDayNamesMatchJdk() {
        val styles = mapOf(
            DateNameStyle.NARROW to java.time.format.TextStyle.NARROW, DateNameStyle.SHORT to java.time.format.TextStyle.SHORT,
            DateNameStyle.FULL to java.time.format.TextStyle.FULL,
        )
        for (lang in listOf("ko", "en", "ja", "zh")) {
            val locale = Locale.forLanguageTag(lang)
            var d = kotlinx.datetime.LocalDate(2026, 1, 1)
            repeat(40) {
                val j = java.time.LocalDate.of(d.year, d.month.ordinal + 1, d.day)
                assertEquals(
                    DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).withLocale(locale).format(j),
                    DatePattern.mediumDate(lang, d), "$lang medium $d",
                )
                for ((k, js) in styles) {
                    assertEquals(j.dayOfWeek.getDisplayName(js, locale), d.dayOfWeek.displayName(k, lang), "$lang $k $d")
                }
                d = d.plusDaysKx(37)
            }
        }
    }
}

/** DateCompat 의 날짜 셈이 java.time 과 같은가(달 끝 · 윤년) */
class DateCompatJvmTest {
    @Test fun plusMinusMatchJavaTime() {
        var d = kotlinx.datetime.LocalDate(2023, 1, 1)
        repeat(800) {
            val j = java.time.LocalDate.of(d.year, d.month.ordinal + 1, d.day)
            for (n in listOf(1, 3, 13, -2, 40)) {
                fun same(k: kotlinx.datetime.LocalDate, jv: java.time.LocalDate, what: String) =
                    assertEquals(jv.toString(), k.toString(), "$d $what $n")
                same(d.plusDaysKx(n), j.plusDays(n.toLong()), "plusDays")
                same(d.minusDaysKx(n), j.minusDays(n.toLong()), "minusDays")
                same(d.plusWeeksKx(n), j.plusWeeks(n.toLong()), "plusWeeks")
                same(d.plusMonthsKx(n), j.plusMonths(n.toLong()), "plusMonths")
                same(d.minusMonthsKx(n), j.minusMonths(n.toLong()), "minusMonths")
                same(d.plusYearsKx(n), j.plusYears(n.toLong()), "plusYears")
            }
            d = d.plusDaysKx(1)
        }
    }
}
