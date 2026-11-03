package com.stepup.android.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/*
 * java.time 의 날짜 셈 이름(plusDays · minusMonths …)을 kotlinx-datetime 으로(2026-11-03 iOS C — 화면 코드를 옮길 때 tools/port_ui_to_cmp.py 가 이 이름으로 바꾼다).
 * java.time 과 같은 결과다 — 달을 더해 그 달에 없는 날이 되면 그 달 마지막 날(1월 31일 + 1달 = 2월 28일).
 */

fun LocalDate.plusDaysKx(n: Number): LocalDate = plus(n.toLong(), DateTimeUnit.DAY)
fun LocalDate.minusDaysKx(n: Number): LocalDate = minus(n.toLong(), DateTimeUnit.DAY)
fun LocalDate.plusWeeksKx(n: Number): LocalDate = plus(n.toLong() * 7, DateTimeUnit.DAY)
fun LocalDate.minusWeeksKx(n: Number): LocalDate = minus(n.toLong() * 7, DateTimeUnit.DAY)
fun LocalDate.plusMonthsKx(n: Number): LocalDate = plus(n.toLong(), DateTimeUnit.MONTH)
fun LocalDate.minusMonthsKx(n: Number): LocalDate = minus(n.toLong(), DateTimeUnit.MONTH)
fun LocalDate.plusYearsKx(n: Number): LocalDate = plus(n.toLong(), DateTimeUnit.YEAR)
fun LocalDate.minusYearsKx(n: Number): LocalDate = minus(n.toLong(), DateTimeUnit.YEAR)

fun YearMonth.plusMonthsKx(n: Number): YearMonth = plus(n.toLong(), DateTimeUnit.MONTH)
fun YearMonth.minusMonthsKx(n: Number): YearMonth = minus(n.toLong(), DateTimeUnit.MONTH)
fun YearMonth.plusYearsKx(n: Number): YearMonth = plus(n.toLong(), DateTimeUnit.YEAR)
fun YearMonth.minusYearsKx(n: Number): YearMonth = minus(n.toLong(), DateTimeUnit.YEAR)

/** DateTimeFormatter.ofPattern(pattern).format(this) — [DatePattern] */
fun LocalDate.formatPattern(pattern: String, language: String): String = DatePattern.format(this, pattern, language)
fun LocalDateTime.formatPattern(pattern: String, language: String): String = DatePattern.format(this, pattern, language)
fun LocalTime.formatPattern(pattern: String, language: String): String = DatePattern.format(this, pattern, language)

/** IsoFields.WEEK_BASED_YEAR · WEEK_OF_WEEK_BASED_YEAR — 그 주의 목요일이 든 해 · 그 해의 몇째 주(ISO 8601) */
fun LocalDate.isoWeekBasedYear(): Int = plusDaysKx(4 - (dayOfWeek.ordinal + 1)).year

fun LocalDate.isoWeekOfWeekBasedYear(): Int {
    val thursday = plusDaysKx(4 - (dayOfWeek.ordinal + 1))
    return (thursday.dayOfYear - 1) / 7 + 1
}

/**
 * DateTimeFormatter.ofPattern(pattern, locale) 자리 — 같은 꼴 · 같은 글자([DatePattern]). [zone] 을 주면 시각(Instant)도 받는다(withZone).
 */
class PatternFormatter(val pattern: String, val language: String, val zone: kotlinx.datetime.TimeZone? = null) {
    fun format(date: LocalDate): String = DatePattern.format(date, pattern, language)
    fun format(at: LocalDateTime): String = DatePattern.format(at, pattern, language)
    fun format(time: LocalTime): String = DatePattern.format(time, pattern, language)
    fun format(month: YearMonth): String = DatePattern.format(LocalDate(month.year, month.month, 1), pattern, language)
    fun format(instant: kotlin.time.Instant): String =
        DatePattern.format(instant.toEpochMilliseconds(), requireNotNull(zone) { "withZone 없이 시각을 꼴로" }, pattern, language)

    fun withZone(zone: kotlinx.datetime.TimeZone): PatternFormatter = PatternFormatter(pattern, language, zone)
}

/** java.time 의 date.format(formatter) 자리 */
fun LocalDate.format(f: PatternFormatter): String = f.format(this)
fun LocalDateTime.format(f: PatternFormatter): String = f.format(this)
fun LocalTime.format(f: PatternFormatter): String = f.format(this)
fun YearMonth.format(f: PatternFormatter): String = f.format(this)
