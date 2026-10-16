package com.stepup.android.domain.coaching

import com.stepup.android.domain.averagePace
import com.stepup.android.domain.localDate
import com.stepup.android.domain.mondayOf
import com.stepup.android.domain.weekRange
import java.time.LocalDate
import java.time.ZoneId

/*
 * AI 코칭 — 오늘의 주간 띠(01) · 주간 변화(04)의 집계. AI 없이 저장된 러닝만으로 센다.
 *
 * 주는 사용자 시간대의 월요일 0시 이상 ~ 다음 월요일 0시 미만(내 러닝 기록과 같은 weekRange). 서버(0059 coaching.week_*)도
 * 같은 경계를 쓴다. 미래의 날 · 주는 0회 실적으로 그리지 않고 따로 표시한다. 계획(13)은 여기 섞지 않는다 — 체크는 실제 기록만.
 */

/**
 * 코칭이 세는 러닝 하나 — 지금 계정(account:…)의 저장된 러닝 중 거리와 활성 시간이 모두 있는 것.
 * 무효(VOID — 거리 0) · 걸음 없는 기록은 넣지 않는다(코칭의 "러닝"이 아니다).
 */
data class CoachRun(val id: Long, val startedAt: Long, val meters: Double, val seconds: Long)

/** 거리와 활성 시간이 모두 있는가 — 확장 속성으로 둔다(Room 이 결과를 이 클래스로 읽으므로 칸은 생성자 넷뿐) */
val CoachRun.measured: Boolean get() = meters.isFinite() && meters > 0.0 && seconds > 0L

/** 01 의 주간 띠 한 칸 — [runs] 는 그날 시작한 러닝 수(미래 날은 늘 0) */
data class WeekDay(val date: LocalDate, val runs: Int, val today: Boolean, val future: Boolean) {
    val done: Boolean get() = runs > 0
}

/** 지난(끝난) 주 · 진행 중인 주 · 아직 오지 않은 주 */
enum class WeekState { DONE, CURRENT, FUTURE }

/**
 * 한 주의 합계. 평균 페이스는 거리와 시간이 모두 있는 러닝들의 시간 합 ÷ 거리 합이다 — 러닝마다의 페이스를 평균하지 않는다.
 * 미래 주는 러닝을 세지 않는다(폰 시계가 틀려 미래에 시작한 기록이 있어도).
 */
data class WeekSummary(
    val monday: LocalDate,
    val state: WeekState,
    val runs: Int,
    val meters: Double,
    val seconds: Long,
) {
    val averagePaceSec: Double? get() = averagePace(seconds, meters)
}

/** 이번 주(오늘이 든 월~일)의 띠 */
fun weekStrip(today: LocalDate, runs: List<CoachRun>, zone: ZoneId): List<WeekDay> {
    val monday = mondayOf(today)
    val measured = runs.filter { it.measured }
    return (0L until 7L).map { offset ->
        val day = monday.plusDays(offset)
        val future = day.isAfter(today)
        WeekDay(day, if (future) 0 else measured.count { it.startedAt.localDate(zone) == day }, day == today, future)
    }
}

fun weekState(monday: LocalDate, today: LocalDate): WeekState = when {
    monday.isAfter(today) -> WeekState.FUTURE
    monday.plusDays(6).isBefore(today) -> WeekState.DONE
    else -> WeekState.CURRENT
}

fun weekSummary(monday: LocalDate, runs: List<CoachRun>, today: LocalDate, zone: ZoneId): WeekSummary {
    val state = weekState(monday, today)
    if (state == WeekState.FUTURE) return WeekSummary(monday, state, 0, 0.0, 0L)
    val range = weekRange(monday, zone)
    val inside = runs.filter { it.measured && it.startedAt >= range.from && it.startedAt < range.until }
    return WeekSummary(monday, state, inside.size, inside.sumOf { it.meters }, inside.sumOf { it.seconds })
}

/** 최근 [count]주의 합계 — [lastMonday] 주까지, 오래된 주부터(04 "최근 4주" 막대) */
fun recentWeeks(lastMonday: LocalDate, count: Int, runs: List<CoachRun>, today: LocalDate, zone: ZoneId): List<WeekSummary> =
    (count - 1 downTo 0).map { back -> weekSummary(lastMonday.minusWeeks(back.toLong()), runs, today, zone) }

/**
 * [monday] 주부터 거슬러 올라가며 러닝이 1회 이상인 주가 끊기지 않고 이어진 수(그 주 포함). 그 주가 0회면 0.
 * "4주째 러닝을 이어가고 있어요"의 숫자다. [limit] 주까지만 센다.
 */
fun weekStreak(monday: LocalDate, runs: List<CoachRun>, zone: ZoneId, limit: Int = 52): Int {
    val measured = runs.filter { it.measured }
    var streak = 0
    var week = monday
    while (streak < limit) {
        val range = weekRange(week, zone)
        if (measured.none { it.startedAt >= range.from && it.startedAt < range.until }) break
        streak++
        week = week.minusWeeks(1)
    }
    return streak
}

/** 04 가 처음 보이는 주 — 끝난 지난주(이번 주는 아직 진행 중이다) */
fun defaultChangeWeek(today: LocalDate): LocalDate = mondayOf(today).minusWeeks(1)

/** 막대의 위 끝(km) — 0 에서 시작하는 축의 깔끔한 눈금. 기록이 없으면 1km */
fun kmAxisTop(maxKm: Double): Double {
    if (!maxKm.isFinite() || maxKm <= 0.0) return 1.0
    val step = when {
        maxKm <= 2.0 -> 0.5
        maxKm <= 5.0 -> 1.0
        maxKm <= 10.0 -> 2.0
        maxKm <= 25.0 -> 5.0
        maxKm <= 50.0 -> 10.0
        else -> 20.0
    }
    return step * kotlin.math.ceil(maxKm / step)
}
