package com.stepup.android

import com.stepup.android.domain.coaching.CoachRun
import com.stepup.android.domain.coaching.WeekState
import com.stepup.android.domain.coaching.defaultChangeWeek
import com.stepup.android.domain.coaching.kmAxisTop
import com.stepup.android.domain.coaching.recentWeeks
import com.stepup.android.domain.coaching.weekStreak
import com.stepup.android.domain.coaching.weekStrip
import com.stepup.android.domain.coaching.weekSummary
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** AI 코칭의 주간 집계 — 사용자 시간대의 월요일 시작 · 종료 미포함, 가중 평균 페이스, 미래 · 진행 중 구분 */
class CoachingWeeksTest {
    private val seoul = ZoneId.of("Asia/Seoul")
    private val today = LocalDate.of(2026, 10, 16) // 금요일

    private fun at(d: LocalDate, h: Int = 7, m: Int = 0, zone: ZoneId = seoul) =
        ZonedDateTime.of(d.year, d.monthValue, d.dayOfMonth, h, m, 0, 0, zone).toInstant().toEpochMilli()

    private fun run(id: Long, d: LocalDate, meters: Double, seconds: Long, h: Int = 7) = CoachRun(id, at(d, h), meters, seconds)

    @Test fun `이번 주 띠 — 월요일부터, 기록이 있는 날만 체크, 미래는 따로`() {
        val runs = listOf(run(1, LocalDate.of(2026, 10, 12), 3000.0, 1300), run(2, LocalDate.of(2026, 10, 14), 3240.0, 1458))
        val strip = weekStrip(today, runs, seoul)
        assertEquals(LocalDate.of(2026, 10, 12), strip.first().date)
        assertEquals(LocalDate.of(2026, 10, 18), strip.last().date)
        assertEquals(listOf(true, false, true, false, false, false, false), strip.map { it.done })
        assertEquals(listOf(false, false, false, false, true, false, false), strip.map { it.today })
        assertEquals(listOf(false, false, false, false, false, true, true), strip.map { it.future })
    }

    @Test fun `무효 러닝(거리 0)은 체크하지 않는다`() {
        val strip = weekStrip(today, listOf(run(1, LocalDate.of(2026, 10, 13), 0.0, 0)), seoul)
        assertFalse(strip[1].done)
    }

    @Test fun `미래 날짜에 찍힌 기록은 세지 않는다(폰 시계가 틀린 경우)`() {
        val strip = weekStrip(today, listOf(run(1, LocalDate.of(2026, 10, 17), 3000.0, 1300)), seoul)
        assertEquals(0, strip[5].runs)
    }

    @Test fun `검수 숫자 — 9km 1시간8분24초는 456초 per km, 러닝별 페이스의 평균이 아니다`() {
        val monday = LocalDate.of(2026, 10, 5)
        val runs = listOf(
            run(1, monday, 2000.0, 960), // 480 s/km
            run(2, monday.plusDays(2), 4000.0, 1680), // 420 s/km
            run(3, monday.plusDays(4), 3000.0, 1464), // 488 s/km
        )
        val week = weekSummary(monday, runs, today, seoul)
        assertEquals(WeekState.DONE, week.state)
        assertEquals(3, week.runs)
        assertEquals(9000.0, week.meters, 1e-9)
        assertEquals(4104L, week.seconds)
        assertEquals(456.0, week.averagePaceSec!!, 1e-9)
        val naive = listOf(480.0, 420.0, 488.0).average()
        assertTrue(kotlin.math.abs(naive - 456.0) > 5)
    }

    @Test fun `주 경계 — 일요일 23시59분은 그 주, 월요일 0시는 다음 주`() {
        val monday = LocalDate.of(2026, 10, 5)
        val sundayLate = CoachRun(1, at(LocalDate.of(2026, 10, 11), 23, 59), 1000.0, 400)
        val mondayZero = CoachRun(2, at(LocalDate.of(2026, 10, 12), 0, 0), 1000.0, 400)
        assertEquals(1, weekSummary(monday, listOf(sundayLate, mondayZero), today, seoul).runs)
        assertEquals(1, weekSummary(monday.plusWeeks(1), listOf(sundayLate, mondayZero), today, seoul).runs)
    }

    @Test fun `일광절약시간이 끝나는 주도 사용자 시간대의 월요일 0시로 자른다`() {
        val berlin = ZoneId.of("Europe/Berlin") // 2026-10-25 03:00 → 02:00
        val monday = LocalDate.of(2026, 10, 19)
        val lastHour = CoachRun(1, at(LocalDate.of(2026, 10, 25), 23, 30, berlin), 1000.0, 400)
        val nextMonday = CoachRun(2, at(LocalDate.of(2026, 10, 26), 0, 0, berlin), 1000.0, 400)
        val later = LocalDate.of(2026, 11, 2)
        assertEquals(1, weekSummary(monday, listOf(lastHour, nextMonday), later, berlin).runs)
        assertEquals(1, weekSummary(monday.plusWeeks(1), listOf(lastHour, nextMonday), later, berlin).runs)
    }

    @Test fun `시간대가 바뀌면 같은 기록이 다른 날 · 다른 주가 될 수 있다`() {
        // 서울 월요일 07시 = UTC 일요일 22시 — UTC 사용자의 주에서는 앞 주다
        val runs = listOf(run(1, LocalDate.of(2026, 10, 12), 1000.0, 400))
        assertEquals(1, weekSummary(LocalDate.of(2026, 10, 12), runs, today, seoul).runs)
        assertEquals(1, weekSummary(LocalDate.of(2026, 10, 5), runs, today, ZoneId.of("UTC")).runs)
    }

    @Test fun `진행 중인 주와 미래 주를 끝난 주와 가른다`() {
        assertEquals(WeekState.CURRENT, weekSummary(LocalDate.of(2026, 10, 12), emptyList(), today, seoul).state)
        val future = weekSummary(LocalDate.of(2026, 10, 19), listOf(run(1, LocalDate.of(2026, 10, 20), 1000.0, 400)), today, seoul)
        assertEquals(WeekState.FUTURE, future.state)
        assertEquals(0, future.runs)
        assertNull(future.averagePaceSec)
    }

    @Test fun `최근 4주는 오래된 주부터, 선택한 주까지`() {
        val weeks = recentWeeks(LocalDate.of(2026, 10, 5), 4, emptyList(), today, seoul)
        assertEquals(listOf(14, 21, 28, 5), weeks.map { it.monday.dayOfMonth })
        assertTrue(weeks.all { it.state == WeekState.DONE && it.runs == 0 })
    }

    @Test fun `이어진 주 — 끊기면 멈추고, 그 주가 0회면 0`() {
        val runs = (0L until 4L).map { back -> run(back, LocalDate.of(2026, 10, 5).minusWeeks(back), 3000.0, 1300) } +
            run(9, LocalDate.of(2026, 9, 1), 3000.0, 1300) // 끊긴 뒤의 옛 주
        assertEquals(4, weekStreak(LocalDate.of(2026, 10, 5), runs, seoul))
        assertEquals(0, weekStreak(LocalDate.of(2026, 10, 12), runs, seoul))
    }

    @Test fun `변화 탭은 끝난 지난주에서 시작한다`() {
        assertEquals(LocalDate.of(2026, 10, 5), defaultChangeWeek(today))
        assertEquals(LocalDate.of(2026, 10, 5), defaultChangeWeek(LocalDate.of(2026, 10, 12)))
    }

    @Test fun `막대 축은 0 에서 시작하고 깔끔한 눈금에서 끝난다`() {
        assertEquals(10.0, kmAxisTop(9.0), 1e-9)
        assertEquals(1.0, kmAxisTop(0.0), 1e-9)
        assertEquals(2.0, kmAxisTop(1.6), 1e-9)
        assertEquals(1.0, kmAxisTop(Double.NaN), 1e-9)
    }
}
