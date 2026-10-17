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

    @Test fun `a much sharper fix inside the anchor's error moves the anchor instead of stalling`() {
        // 와이파이(오차 35m)로 잡힌 출발점 근처에 GPS(오차 5m)가 왔다 — 같은 자리를 더 정확히 안 것: 기준점만 옮긴다(거리는 더하지 않는다).
        // 예전엔 70m(35m 의 두 배)를 움직일 때까지 거리가 0 에 멈춰 있었다(2026-10-16 "인식도 잘 안 되고 느려")
        assertEquals(TrackFixRule.Fix.REFINE, TrackFixRule.decide(accuracyM = 5f, anchorDistanceM = 28.0, anchorAccuracyM = 35f))
        // 두 오차의 합(40m)까지가 같은 자리다
        assertEquals(TrackFixRule.Fix.REFINE, TrackFixRule.decide(accuracyM = 5f, anchorDistanceM = 40.0, anchorAccuracyM = 35f))
        assertEquals(TrackFixRule.Fix.HOLD, TrackFixRule.decide(accuracyM = 5f, anchorDistanceM = 41.0, anchorAccuracyM = 35f))
        // 기준점 오차의 절반보다 흐리면 더 정확하다고 볼 수 없다 — 그대로 기다린다
        assertEquals(TrackFixRule.Fix.REFINE, TrackFixRule.decide(accuracyM = 17.5f, anchorDistanceM = 10.0, anchorAccuracyM = 35f))
        assertEquals(TrackFixRule.Fix.HOLD, TrackFixRule.decide(accuracyM = 20f, anchorDistanceM = 10.0, anchorAccuracyM = 35f))
        // 오차를 알려 주지 않는 위치로는 다시 잡지 않는다
        assertEquals(TrackFixRule.Fix.HOLD, TrackFixRule.decide(accuracyM = null, anchorDistanceM = 10.0, anchorAccuracyM = 50f))
        // 기준점 오차의 두 배를 넘게 움직였으면 예전처럼 경로 · 거리에 넣는다
        assertEquals(TrackFixRule.Fix.COMMIT, TrackFixRule.decide(accuracyM = 5f, anchorDistanceM = 70.0, anchorAccuracyM = 35f))
    }

    @Test fun `a blurry fix does not move the map dot once the route has begun`() {
        // 경로가 생긴 뒤 30초 안에 좋은 위치가 있었으면 기지국 위치로 내 점 · 크루에게 보내는 위치를 옮기지 않는다
        assertEquals(true, TrackFixRule.keepHere(hasTrack = true, hereAgeMs = 2_500))
        assertEquals(true, TrackFixRule.keepHere(hasTrack = true, hereAgeMs = 29_999))
        // 30초 넘게 좋은 위치가 없으면 흐린 위치라도 쓴다
        assertEquals(false, TrackFixRule.keepHere(hasTrack = true, hereAgeMs = 30_000))
        // 경로 전(대략적인 자리를 먼저 보인다) · 받은 적 없음 · 시계가 뒤로 감
        assertEquals(false, TrackFixRule.keepHere(hasTrack = false, hereAgeMs = 1_000))
        assertEquals(false, TrackFixRule.keepHere(hasTrack = true, hereAgeMs = Long.MAX_VALUE))
        assertEquals(false, TrackFixRule.keepHere(hasTrack = true, hereAgeMs = -5))
    }

    @Test fun `a fresh sharp fix caught on the run tab counts as located at the start`() {
        // 러닝 탭에서 미리 잡은 위치(2026-10-17 "키자마자 잡아두면 안되냐") — 10초 안 · 오차 50m 안이면 시작하자마자 잡힌 것으로 본다
        assertEquals(true, TrackFixRule.warmFix(accuracyM = 6f, ageMs = 1_200))
        assertEquals(true, TrackFixRule.warmFix(accuracyM = 50f, ageMs = 10_000))
        assertEquals(true, TrackFixRule.warmFix(accuracyM = null, ageMs = 0))
        // 오래됐거나 · 흐리거나 · 시각이 거꾸로면 쓰지 않는다(러닝이 직접 찾는다)
        assertEquals(false, TrackFixRule.warmFix(accuracyM = 6f, ageMs = 10_001))
        assertEquals(false, TrackFixRule.warmFix(accuracyM = 51f, ageMs = 1_000))
        assertEquals(false, TrackFixRule.warmFix(accuracyM = 6f, ageMs = -1))
    }

    @Test fun `fixes without accuracy are treated as 30m`() {
        assertEquals(TrackFixRule.Fix.HOLD, TrackFixRule.decide(accuracyM = null, anchorDistanceM = 59.0, anchorAccuracyM = null))
        assertEquals(TrackFixRule.Fix.COMMIT, TrackFixRule.decide(accuracyM = null, anchorDistanceM = 60.0, anchorAccuracyM = null))
    }
}
