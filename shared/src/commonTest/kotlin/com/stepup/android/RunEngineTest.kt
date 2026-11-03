package com.stepup.android

import com.stepup.android.domain.RunEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** iOS 러닝 셈 — 안드로이드와 같은 공용 규칙(위치 거르기 · 거리 · 시간) */
class RunEngineTest {
    private val t0 = 1_800_000_000_000L

    @Test fun countsOnlyRunningTimeAndFilteredTrack() {
        val e = RunEngine(t0)
        // 정확한 위치 세 점 — 위도 0.0005 ≈ 55.6m 씩 북쪽으로
        e.fix(37.5000, 126.9500, 5f, t0)
        e.tick(t0 + 10_000)
        e.fix(37.5005, 126.9500, 5f, t0 + 10_000)
        // 흐린 위치(오차 80m)는 지도만 — 경로 · 거리에 넣지 않는다
        e.fix(37.5100, 126.9500, 80f, t0 + 15_000)
        e.fix(37.5010, 126.9500, 5f, t0 + 20_000)
        e.tick(t0 + 20_000)
        // 일시정지 동안은 시간 · 거리를 세지 않는다
        e.pause(t0 + 20_000)
        e.fix(37.5100, 126.9500, 5f, t0 + 60_000)
        e.tick(t0 + 60_000)
        e.resume(t0 + 60_000)
        e.tick(t0 + 61_000)
        val s = e.state(t0 + 61_000)
        assertEquals(21, s.elapsedSec)
        assertEquals(3, s.track.size)
        assertTrue(s.gpsKm in 0.110..0.112, "gps ${s.gpsKm}")
        // 경로 100m 를 넘었으니 화면 거리는 GPS
        assertEquals(s.gpsKm, s.liveKm)
    }

    @Test fun stepsBeforeGpsAndCreditedCap() {
        val e = RunEngine(t0)
        e.steps(1000, t0 + 1_000)
        e.tick(t0 + 60_000)
        val s = e.state(t0 + 60_000)
        // 경로가 없으면 걸음 거리(안드로이드 RewardEconomy.distanceMeters)
        assertEquals(com.stepup.android.domain.RewardEconomy.distanceMeters(1000) / 1000, s.liveKm)
        assertEquals(com.stepup.android.domain.RewardEconomy.distanceMeters(1000), e.creditedMeters())
        // 걸음이 멈춘 지 30초가 넘으면 서 있음
        assertTrue(e.state(t0 + 40_000).still)
    }

    @Test fun blurryFixKeepsHereFor30sAfterTrack() {
        val e = RunEngine(t0)
        e.fix(37.5000, 126.9500, 5f, t0)
        // 경로가 생긴 뒤 30초 안의 흐린 위치는 지금 자리를 옮기지 않는다
        e.fix(37.5100, 126.9500, 80f, t0 + 10_000)
        assertEquals(37.5000, e.state(t0 + 10_000).here!!.lat)
        assertTrue(e.state(t0 + 10_000).hereAccurate)
        // 30초가 지나면 흐린 위치도 지도만 옮긴다
        e.fix(37.5100, 126.9500, 80f, t0 + 31_000)
        val s = e.state(t0 + 31_000)
        assertEquals(37.5100, s.here!!.lat)
        assertEquals(false, s.hereAccurate)
        assertEquals(1, s.track.size)
    }
}
