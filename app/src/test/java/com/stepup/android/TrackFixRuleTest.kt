package com.stepup.android

import com.stepup.android.domain.TrackFixRule
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 달리는 중 위치 거르기(2026-10-16 사용자 결정 "그래 다 되도록 만들어") — 구글 지도처럼 GPS · 와이파이 · 기지국 어느 위치든
 * 받되, 위성 GPS 가 아닌 위치의 떨림은 거리로 쌓지 않는다.
 */
class TrackFixRuleTest {

    @Test fun `first usable non-gps fix starts the track, cell-sized fixes only move the map dot`() {
        // 첫 점 · 끊긴 뒤 새 구간 — 오차가 50m 안이면 경로를 시작한다
        assertEquals(TrackFixRule.Fix.COMMIT, TrackFixRule.decide(accuracyM = 18f, anchorDistanceM = null, anchorAccuracyM = null))
        // 기지국 위치처럼 오차가 크면 지도의 지금 자리만
        assertEquals(TrackFixRule.Fix.MAP_ONLY, TrackFixRule.decide(accuracyM = 900f, anchorDistanceM = null, anchorAccuracyM = null))
        assertEquals(TrackFixRule.Fix.MAP_ONLY, TrackFixRule.decide(accuracyM = 51f, anchorDistanceM = 400.0, anchorAccuracyM = 10f))
    }

    @Test fun `indoor wifi jitter does not add distance but real running does`() {
        // 실내 제자리 — 오차 20m 짜리 위치가 25m 튀었다: 두 배(40m)를 못 넘으니 거리로 넣지 않는다
        assertEquals(TrackFixRule.Fix.HOLD, TrackFixRule.decide(accuracyM = 20f, anchorDistanceM = 25.0, anchorAccuracyM = 20f))
        // 기준점 오차가 더 크면 그 오차의 두 배까지 기다린다(와이파이로 시작해 GPS 로 넘어갈 때 한 번에 튀지 않게)
        assertEquals(TrackFixRule.Fix.HOLD, TrackFixRule.decide(accuracyM = 5f, anchorDistanceM = 45.0, anchorAccuracyM = 30f))
        assertEquals(TrackFixRule.Fix.COMMIT, TrackFixRule.decide(accuracyM = 5f, anchorDistanceM = 61.0, anchorAccuracyM = 30f))
        // 밖에서 GPS 급 위치(오차 5m) — 10m 움직이면 넣는다(달리면 몇 초 안)
        assertEquals(TrackFixRule.Fix.HOLD, TrackFixRule.decide(accuracyM = 5f, anchorDistanceM = 9.0, anchorAccuracyM = 5f))
        assertEquals(TrackFixRule.Fix.COMMIT, TrackFixRule.decide(accuracyM = 5f, anchorDistanceM = 10.0, anchorAccuracyM = 5f))
        // 아주 정확해도 경로 점 사이는 8m 이상
        assertEquals(TrackFixRule.Fix.HOLD, TrackFixRule.decide(accuracyM = 2f, anchorDistanceM = 7.5, anchorAccuracyM = 2f))
        assertEquals(TrackFixRule.Fix.COMMIT, TrackFixRule.decide(accuracyM = 2f, anchorDistanceM = 8.0, anchorAccuracyM = 2f))
    }

    @Test fun `fixes without accuracy are treated as 30m`() {
        assertEquals(TrackFixRule.Fix.HOLD, TrackFixRule.decide(accuracyM = null, anchorDistanceM = 59.0, anchorAccuracyM = null))
        assertEquals(TrackFixRule.Fix.COMMIT, TrackFixRule.decide(accuracyM = null, anchorDistanceM = 60.0, anchorAccuracyM = null))
    }
}
