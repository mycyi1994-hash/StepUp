package com.stepup.android.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/*
 * 내 러닝 기록(2026-09-28 전달본, docs/redesign/running-records) — 기간 · 통계 계산.
 *
 * 모두 저장된 러닝 세션(walk_sessions)의 거리 · 시간 · 횟수로 센다. 만보기 하루 걸음으로 대신하지 않는다.
 * 날짜는 사용자 시간대로: 달은 1일 0시 이상 ~ 다음 달 1일 0시 미만, 주는 월요일 ~ 일요일.
 */

/** 기록 목록의 기간 — 한 달 또는 전체 */
sealed interface RecordPeriod {
    data class Month(val month: YearMonth) : RecordPeriod
    data object All : RecordPeriod
}

/** [from] 이상 [until] 미만(epoch ms) */
data class TimeRange(val from: Long, val until: Long)

fun RecordPeriod.range(zone: ZoneId): TimeRange = when (this) {
    RecordPeriod.All -> TimeRange(Long.MIN_VALUE, Long.MAX_VALUE)
    is RecordPeriod.Month -> month.range(zone)
}

fun YearMonth.range(zone: ZoneId): TimeRange =
    TimeRange(atDay(1).startMillis(zone), plusMonths(1).atDay(1).startMillis(zone))

/** 월요일에 시작하는 한 주 */
fun weekRange(monday: LocalDate, zone: ZoneId): TimeRange = TimeRange(monday.startMillis(zone), monday.plusDays(7).startMillis(zone))

fun LocalDate.startMillis(zone: ZoneId): Long = atStartOfDay(zone).toInstant().toEpochMilli()

fun Long.localDate(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

/** 그 날짜가 들어 있는 주의 월요일 */
fun mondayOf(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/** 주간 → 월간: 선택한 주의 마지막 날짜(아직 오지 않았으면 오늘)가 속한 달 */
fun monthForWeek(monday: LocalDate, today: LocalDate): YearMonth = YearMonth.from(minOf(monday.plusDays(6), today))

/** 월간 → 주간: 이번 달이면 이번 주, 지난 달이면 그 달의 마지막 날이 들어 있는 주 */
fun weekForMonth(month: YearMonth, today: LocalDate): LocalDate =
    if (month == YearMonth.from(today)) mondayOf(today) else mondayOf(minOf(month.atEndOfMonth(), today))

/** 다음 주 · 다음 달로 갈 수 있는가 — 아직 오지 않은 기간으로는 가지 않는다 */
fun hasNextWeek(monday: LocalDate, today: LocalDate): Boolean = !monday.plusWeeks(1).isAfter(today)

fun hasNextMonth(month: YearMonth, today: LocalDate): Boolean = !month.plusMonths(1).isAfter(YearMonth.from(today))

/** 러닝 한 번의 날짜 · 거리 — 막대 계산에 쓰는 것만. [id] 는 그 날짜의 기록(상세)으로 가는 세션 id */
data class RunMark(val startedAt: Long, val meters: Double, val id: Long = 0)

/**
 * 막대 하나 — 주간은 하루, 월간은 날짜 구간(1–7 · 8–14 · 15–21 · 22–28 · 29–말일, 달력 주가 아니다).
 * [future] 는 아직 오지 않은 칸 — 0km 로 그리지 않는다.
 */
data class RunBar(val first: LocalDate, val last: LocalDate, val meters: Double, val runs: Int, val future: Boolean)

fun weekBars(monday: LocalDate, runs: List<RunMark>, today: LocalDate, zone: ZoneId): List<RunBar> =
    (0L until 7L).map { offset ->
        val day = monday.plusDays(offset)
        bar(day, day, runs, today, zone)
    }

fun monthBars(month: YearMonth, runs: List<RunMark>, today: LocalDate, zone: ZoneId): List<RunBar> {
    val last = month.lengthOfMonth()
    return listOf(1 to 7, 8 to 14, 15 to 21, 22 to 28, 29 to last)
        .filter { (start, _) -> start <= last }
        .map { (start, end) -> bar(month.atDay(start), month.atDay(end), runs, today, zone) }
}

private fun bar(first: LocalDate, last: LocalDate, runs: List<RunMark>, today: LocalDate, zone: ZoneId): RunBar {
    val inside = runs.filter { val day = it.startedAt.localDate(zone); !day.isBefore(first) && !day.isAfter(last) }
    return RunBar(first, last, inside.sumOf { it.meters.coerceAtLeast(0.0) }, inside.size, future = first.isAfter(today))
}

/**
 * 가장 많이 달린 날(통계 H04 · H06) — 그 기간 러닝을 사용자 시간대의 날짜로 묶어 거리 합이 가장 큰 날.
 * 거리가 있는 날이 없으면 null(0km 인 날을 "가장 많이 달린 날"로 보이지 않는다). 같으면 앞선 날.
 */
fun bestDay(runs: List<RunMark>, zone: ZoneId): RunBar? =
    runs.groupBy { it.startedAt.localDate(zone) }
        .map { (day, inside) -> RunBar(day, day, inside.sumOf { it.meters.coerceAtLeast(0.0) }, inside.size, future = false) }
        .filter { it.meters > 0.0 }
        .sortedBy { it.first }
        .maxByOrNull { it.meters }

/**
 * 평균 페이스(초/km) — 거리와 시간이 모두 있는 러닝들의 시간 합 ÷ 거리 합. 러닝마다의 페이스를 평균하지 않는다.
 * 계산할 수 없으면 null.
 */
fun averagePace(seconds: Long, meters: Double): Double? =
    if (seconds > 0 && meters > 0.0) seconds / (meters / 1000.0) else null
