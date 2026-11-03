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
