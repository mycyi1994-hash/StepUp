package com.stepup.android.core

/*
 * 공용 규칙(shared)은 kotlinx-datetime 을 쓴다(2026-11-03 KMP 2단계). 안드로이드 화면이 아직 java.time 으로 글자를 만드는 곳에서
 * 바꿔 쓰는 도우미 — 화면을 공용으로 옮기면(C 단계) 사라진다.
 */
fun kotlinx.datetime.YearMonth.toJavaYearMonth(): java.time.YearMonth = java.time.YearMonth.of(year, month.ordinal + 1)

fun java.time.YearMonth.toKotlinYearMonth(): kotlinx.datetime.YearMonth = kotlinx.datetime.YearMonth(year, monthValue)

/** java.time 형식으로 날짜 글자 — 화면 글자는 아직 기기 언어의 DateTimeFormatter 로 만든다 */
fun kotlinx.datetime.LocalDate.format(formatter: java.time.format.DateTimeFormatter): String =
    java.time.LocalDate.of(year, month.ordinal + 1, day).format(formatter)

fun kotlinx.datetime.YearMonth.format(formatter: java.time.format.DateTimeFormatter): String = toJavaYearMonth().format(formatter)

/** java.time 의 LocalDate.toEpochDay() 와 같은 값 */
fun kotlinx.datetime.LocalDate.toEpochDay(): Long = toEpochDays().toLong()
