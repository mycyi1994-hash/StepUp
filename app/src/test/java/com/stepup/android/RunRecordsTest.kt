package com.stepup.android

import com.stepup.android.domain.RecordPeriod
import com.stepup.android.domain.RunMark
import com.stepup.android.domain.averagePace
import com.stepup.android.domain.bestDay
import com.stepup.android.domain.hasNextMonth
import com.stepup.android.domain.hasNextWeek
import com.stepup.android.domain.mondayOf
import com.stepup.android.domain.monthBars
import com.stepup.android.domain.monthForWeek
import com.stepup.android.domain.range
import com.stepup.android.domain.startMillis
import com.stepup.android.domain.weekBars
import com.stepup.android.domain.weekForMonth
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 내 러닝 기록의 기간 · 막대 · 페이스 — 사용자 시간대의 날짜로, 저장된 러닝의 거리 · 시간으로 센다 */
class RunRecordsTest {
    private val seoul = ZoneId.of("Asia/Seoul")

    private fun at(y: Int, m: Int, d: Int, h: Int = 7) = ZonedDateTime.of(y, m, d, h, 0, 0, 0, seoul).toInstant().toEpochMilli()

    @Test fun `달은 1일 0시 이상 다음 달 1일 0시 미만`() {
        val range = RecordPeriod.Month(YearMonth.of(2026, 9)).range(seoul)
        assertEquals(LocalDate.of(2026, 9, 1).startMillis(seoul), range.from)
        assertEquals(LocalDate.of(2026, 10, 1).startMillis(seoul), range.until)
        // 9월 30일 23시 59분은 9월, 10월 1일 0시는 10월
        assertTrue(ZonedDateTime.of(2026, 9, 30, 23, 59, 0, 0, seoul).toInstant().toEpochMilli() < range.until)
        assertEquals(range.until, at(2026, 10, 1, 0))
    }

    @Test fun `주는 월요일부터 — 주간에서 월간 · 월간에서 주간`() {
        val today = LocalDate.of(2026, 9, 27) // 일요일
        assertEquals(LocalDate.of(2026, 9, 21), mondayOf(today))
        assertEquals(YearMonth.of(2026, 9), monthForWeek(LocalDate.of(2026, 9, 21), today))
        // 주가 두 달에 걸치면 그 주의 마지막 날이 속한 달(아직 안 온 날이면 오늘)
        assertEquals(YearMonth.of(2026, 10), monthForWeek(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 3)))
        assertEquals(YearMonth.of(2026, 9), monthForWeek(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 9, 29)))
        // 이번 달이면 이번 주, 지난 달이면 그 달의 마지막 날이 들어 있는 주
        assertEquals(LocalDate.of(2026, 9, 21), weekForMonth(YearMonth.of(2026, 9), today))
        assertEquals(LocalDate.of(2026, 8, 31), weekForMonth(YearMonth.of(2026, 8), today))
    }

    @Test fun `아직 오지 않은 주 · 달로는 가지 않는다`() {
        val today = LocalDate.of(2026, 9, 27)
        assertFalse(hasNextWeek(LocalDate.of(2026, 9, 21), today))
        assertTrue(hasNextWeek(LocalDate.of(2026, 9, 14), today))
        assertFalse(hasNextMonth(YearMonth.of(2026, 9), today))
        assertTrue(hasNextMonth(YearMonth.of(2026, 8), today))
    }

    @Test fun `주간 막대는 날짜별 거리, 아직 오지 않은 날은 비운다`() {
        val runs = listOf(
            RunMark(at(2026, 9, 21), 3000.0), RunMark(at(2026, 9, 23), 4000.0),
            RunMark(at(2026, 9, 25), 5000.0), RunMark(at(2026, 9, 25, 20), 1000.0),
            RunMark(at(2026, 9, 20, 23), 9000.0), // 전 주 일요일 — 들어가지 않는다
        )
        val bars = weekBars(LocalDate.of(2026, 9, 21), runs, LocalDate.of(2026, 9, 25), seoul)
        assertEquals(7, bars.size)
        assertEquals(listOf(3000.0, 0.0, 4000.0, 0.0, 6000.0, 0.0, 0.0), bars.map { it.meters })
        assertEquals(2, bars[4].runs)
        assertEquals(listOf(false, false, false, false, false, true, true), bars.map { it.future })
    }

    @Test fun `월간 막대는 날짜 구간(1–7 · 8–14 · 15–21 · 22–28 · 29–말일)`() {
        val runs = listOf(RunMark(at(2026, 9, 7), 3500.0), RunMark(at(2026, 9, 8), 9500.0), RunMark(at(2026, 9, 30), 1200.0))
        val bars = monthBars(YearMonth.of(2026, 9), runs, LocalDate.of(2026, 9, 27), seoul)
        assertEquals(5, bars.size)
        assertEquals(LocalDate.of(2026, 9, 29), bars[4].first)
        assertEquals(LocalDate.of(2026, 9, 30), bars[4].last)
        assertEquals(listOf(3500.0, 9500.0, 0.0, 0.0, 1200.0), bars.map { it.meters })
        assertTrue("29–30일은 아직 오지 않았다", bars[4].future)
        // 2월(28일)은 네 칸
        assertEquals(4, monthBars(YearMonth.of(2026, 2), emptyList(), LocalDate.of(2026, 9, 27), seoul).size)
    }

    @Test fun `가장 많이 달린 날은 사용자 시간대의 날짜로 묶은 거리 합이 가장 큰 날`() {
        val runs = listOf(
            RunMark(at(2026, 9, 21), 3000.0, id = 1), RunMark(at(2026, 9, 25), 2500.0, id = 2),
            RunMark(at(2026, 9, 25, 20), 2000.0, id = 3), RunMark(at(2026, 9, 23), 4000.0, id = 4),
        )
        val best = bestDay(runs, seoul)!!
        assertEquals(LocalDate.of(2026, 9, 25), best.first)
        assertEquals(4500.0, best.meters, 0.001)
        assertEquals(2, best.runs)
        // 같으면 앞선 날 · 거리가 있는 날이 없으면 없다(0km 를 가장 많이 달린 날로 보이지 않는다)
        assertEquals(LocalDate.of(2026, 9, 21), bestDay(listOf(RunMark(at(2026, 9, 22), 3000.0), RunMark(at(2026, 9, 21), 3000.0)), seoul)!!.first)
        assertNull(bestDay(listOf(RunMark(at(2026, 9, 22), 0.0)), seoul))
        assertNull(bestDay(emptyList(), seoul))
    }

    @Test fun `평균 페이스는 시간 합 ÷ 거리 합 — 러닝마다 페이스의 평균이 아니다`() {
        // 1km 를 10분, 3km 를 15분 — 페이스 평균이면 7분 30초, 합으로는 25분 ÷ 4km = 6분 15초
        assertEquals(375.0, averagePace(25 * 60L, 4000.0)!!, 0.001)
        assertNull(averagePace(0, 4000.0))
        assertNull(averagePace(600, 0.0))
    }

    @Test
    fun `눈금은 가장 큰 막대가 두세 칸 안에 들어오게, 경로 줄이기는 모양과 끝점을 남긴다`() {
        // 시안: 주간(가장 큰 5.0km)은 0 · 3 · 6, 월간(12.2km)은 0 · 5 · 10 · 15
        assertEquals(3.0, com.stepup.android.ui.screens.records.niceStep(5.0), 0.0)
        assertEquals(6.0, com.stepup.android.ui.screens.records.chartTop(5.0, 3.0), 0.0)
        assertEquals(5.0, com.stepup.android.ui.screens.records.niceStep(12.2), 0.0)
        assertEquals(15.0, com.stepup.android.ui.screens.records.chartTop(12.2, 5.0), 0.0)
        assertEquals(2.0, com.stepup.android.ui.screens.records.niceStep(3.2), 0.0)
        assertEquals(0.5, com.stepup.android.ui.screens.records.niceStep(0.0), 0.0)
        val points = (0 until 1000).map { com.stepup.android.domain.GeoPoint(37.0 + it * 1e-5, 127.0) }
        val thinned = com.stepup.android.ui.screens.records.thin(points, 160)
        assertEquals(points.first(), thinned.first())
        assertEquals(points.last(), thinned.last())
        assertTrue(thinned.size <= 161)
        assertEquals(points.take(100), com.stepup.android.ui.screens.records.thin(points.take(100), 160))
    }
}
