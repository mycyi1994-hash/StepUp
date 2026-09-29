package com.stepup.android

import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.TrackPoint
import com.stepup.android.domain.haversineMeters
import com.stepup.android.ui.screens.walk.SPEED_STILL_MS
import com.stepup.android.ui.screens.walk.averageSpeedKmh
import com.stepup.android.ui.screens.walk.currentSpeedKmh
import com.stepup.android.ui.screens.walk.rewardAmount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 러닝 중 현재 속도 · 러닝 완료 평균 속도 · 보상 수(2026-09-29 전달본) */
class RunJourneyTest {
    private val start = 1_700_000_000_000L

    /** 북쪽으로 [step] 도씩, [gapMs] 간격 */
    private fun track(count: Int, gapMs: Long, step: Double = 0.000225): List<TrackPoint> =
        (0 until count).map { i -> TrackPoint(37.5 + step * i, 127.0, start + gapMs * i) }

    private fun meters(a: TrackPoint, b: TrackPoint) = haversineMeters(GeoPoint(a.lat, a.lng), GeoPoint(b.lat, b.lng))

    @Test
    fun `경로가 없거나 한 점뿐이면 현재 속도를 모른다`() {
        assertNull(currentSpeedKmh(emptyList(), start, 0L))
        assertNull(currentSpeedKmh(track(1, 10_000), start, 0L))
    }

    @Test
    fun `최근 20초 안의 점들로 잰다`() {
        val points = track(6, 10_000)
        val last = points.last()
        // 마지막 점과 그 앞 20초 안의 점(두 칸 앞까지) — 40m 남짓을 20초에
        val expected = (meters(points[3], points[4]) + meters(points[4], points[5])) / 20.0 * 3.6
        assertEquals(expected, currentSpeedKmh(points, last.at + 1_000, 0L)!!, 0.01)
    }

    @Test
    fun `앞 점이 창보다 오래됐어도 1분 안이면 두 점으로 잰다`() {
        val points = track(3, 30_000)
        val expected = meters(points[1], points[2]) / 30.0 * 3.6
        assertEquals(expected, currentSpeedKmh(points, points.last().at, 0L)!!, 0.01)
    }

    @Test
    fun `두 점 사이가 1분을 넘으면 모른다`() {
        val points = track(2, 90_000)
        assertNull(currentSpeedKmh(points, points.last().at, 0L))
    }

    @Test
    fun `방금 읽은 속도가 버려졌으면 모른다 — 그 뒤 사람 속도의 점이 오면 다시 보인다`() {
        val points = track(4, 10_000)
        assertNull(currentSpeedKmh(points, points.last().at + 2_000, flaggedAt = points.last().at + 1_000))
        assertEquals(
            meters(points[1], points[2]) / 20.0 * 3.6 + meters(points[2], points[3]) / 20.0 * 3.6,
            currentSpeedKmh(points, points.last().at + 2_000, flaggedAt = points[2].at)!!,
            0.01,
        )
    }

    @Test
    fun `새 점이 한동안 없으면 멈춰 선 것이다`() {
        val points = track(4, 10_000)
        assertEquals(0.0, currentSpeedKmh(points, points.last().at + SPEED_STILL_MS + 1, 0L)!!, 0.0)
    }

    @Test
    fun `평균 속도는 50m 를 넘게 달렸을 때만`() {
        assertEquals(8.0, averageSpeedKmh(3.24, 1_458)!!, 0.01)
        assertNull(averageSpeedKmh(0.04, 600))
        assertNull(averageSpeedKmh(3.0, 0))
    }

    @Test
    fun `보상 수는 버린다 — 1 아래는 둘째 자리, 그 위는 첫째 자리`() {
        assertEquals("2.4", rewardAmount(2.49))
        assertEquals("0.85", rewardAmount(0.859))
        assertEquals("4.0", rewardAmount(4.0))
    }
}
