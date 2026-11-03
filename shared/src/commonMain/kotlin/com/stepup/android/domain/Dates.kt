package com.stepup.android.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/*
 * 공용 규칙의 날짜(2026-11-03 KMP 2단계) — java.time 대신 kotlinx-datetime. 예전 java.time 코드와 같은 이름으로 둬서
 * 규칙 코드가 그대로 읽히게 한다(plusDays · isBefore …). 하루 · 주는 사용자 시간대의 0시 기준, 크루 · 이야기는 한국 시간(KST).
 */

/** 한국 시간 — 크루 하루 · 이야기 14일 · 순위 주가 이 날짜로 끊긴다(서버와 같다) */
val KST: TimeZone = TimeZone.of("Asia/Seoul")

/** epoch ms → 그 시간대의 날짜 */
fun Long.localDate(zone: TimeZone): LocalDate = Instant.fromEpochMilliseconds(this).toLocalDateTime(zone).date

/** 그 날짜 0시(그 시간대)의 epoch ms */
fun LocalDate.startMillis(zone: TimeZone): Long = atStartOfDayIn(zone).toEpochMilliseconds()

/** 지금 그 시간대의 날짜(기본은 폰 시간대 — java.time 의 LocalDate.now()) */
fun todayIn(zone: TimeZone = TimeZone.currentSystemDefault()): LocalDate = epochMillis().localDate(zone)

fun LocalDate.plusDays(days: Long): LocalDate = plus(days, DateTimeUnit.DAY)
fun LocalDate.plusDays(days: Int): LocalDate = plus(days, DateTimeUnit.DAY)
fun LocalDate.minusDays(days: Long): LocalDate = minus(days, DateTimeUnit.DAY)
fun LocalDate.minusDays(days: Int): LocalDate = minus(days, DateTimeUnit.DAY)
fun LocalDate.plusWeeks(weeks: Long): LocalDate = plus(weeks, DateTimeUnit.WEEK)
fun LocalDate.minusWeeks(weeks: Long): LocalDate = minus(weeks, DateTimeUnit.WEEK)
fun LocalDate.isBefore(other: LocalDate): Boolean = this < other
fun LocalDate.isAfter(other: LocalDate): Boolean = this > other

/** java.time 의 ChronoUnit.DAYS.between(from, to) */
fun daysBetween(from: LocalDate, to: LocalDate): Long = from.daysUntil(to).toLong()
