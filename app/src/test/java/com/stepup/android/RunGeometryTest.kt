package com.stepup.android

import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.TrackPoint
import com.stepup.android.domain.distanceToPathMeters
import com.stepup.android.domain.segmentBreaks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 러닝 화면의 경로 계산 — 신호가 끊긴 자리를 잇지 않기(L04) · 코스에서 벗어난 거리(K05) */
class RunGeometryTest {
    // 위도 1e-5 도 ≈ 1.11m
    private val origin = GeoPoint(37.5200, 126.9400)

    private fun north(meters: Double) = GeoPoint(origin.lat + meters / 111_195.0, origin.lng)
    private fun east(meters: Double) = GeoPoint(origin.lat, origin.lng + meters / (111_195.0 * Math.cos(Math.toRadians(origin.lat))))

    @Test fun `끊겼다 멀리서 다시 잡힌 점은 앞 점과 잇지 않는다`() {
        val points = listOf(
            TrackPoint(origin.lat, origin.lng, 0L),
            TrackPoint(east(10.0).lat, east(10.0).lng, 5_000L),
            // 45초 동안 신호가 없다가 200m 떨어진 곳에서 다시 잡혔다
            TrackPoint(east(210.0).lat, east(210.0).lng, 50_000L),
            TrackPoint(east(220.0).lat, east(220.0).lng, 55_000L),
        )
        assertEquals(setOf(2), segmentBreaks(points))
    }

    @Test fun `오래 멈췄다 같은 자리에서 다시 달리면 그대로 잇는다`() {
        val points = listOf(
            TrackPoint(origin.lat, origin.lng, 0L),
            // 2분 쉬었지만 5m 안 — 신호 끊김이 아니라 제자리에 선 것
            TrackPoint(east(5.0).lat, east(5.0).lng, 120_000L),
            TrackPoint(east(15.0).lat, east(15.0).lng, 125_000L),
        )
        assertTrue(segmentBreaks(points).isEmpty())
    }

    @Test fun `빠르게 이어진 점은 멀어도 끊김이 아니다`() {
        val points = listOf(
            TrackPoint(origin.lat, origin.lng, 0L),
            TrackPoint(east(60.0).lat, east(60.0).lng, 10_000L),
        )
        assertTrue(segmentBreaks(points).isEmpty())
        assertTrue(segmentBreaks(points.take(1)).isEmpty())
    }

    @Test fun `코스 선 위의 점은 0m 이고 옆으로 떨어진 만큼 멀다`() {
        val path = listOf(origin, east(500.0))
        assertEquals(0.0, distanceToPathMeters(east(250.0), path), 0.5)
        val beside = GeoPoint(north(100.0).lat, east(250.0).lng)
        assertEquals(100.0, distanceToPathMeters(beside, path), 1.0)
    }

    @Test fun `코스 끝을 지나면 끝점까지의 거리`() {
        val path = listOf(origin, east(500.0))
        assertEquals(80.0, distanceToPathMeters(east(580.0), path), 1.0)
        assertEquals(30.0, distanceToPathMeters(east(-30.0), path), 1.0)
    }

    @Test fun `꺾인 코스는 가장 가까운 선분으로 잰다`() {
        val path = listOf(origin, east(300.0), GeoPoint(north(300.0).lat, east(300.0).lng))
        // 두 번째 선분(북쪽으로 꺾인 길)에서 동쪽으로 40m
        val point = GeoPoint(north(150.0).lat, east(340.0).lng)
        assertEquals(40.0, distanceToPathMeters(point, path), 1.0)
    }

    @Test fun `빈 코스 · 한 점 코스`() {
        assertEquals(Double.POSITIVE_INFINITY, distanceToPathMeters(origin, emptyList()), 0.0)
        assertEquals(100.0, distanceToPathMeters(north(100.0), listOf(origin)), 1.0)
    }
}
