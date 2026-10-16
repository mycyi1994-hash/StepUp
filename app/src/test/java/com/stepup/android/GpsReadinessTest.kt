package com.stepup.android

import com.stepup.android.domain.GpsFixRule
import com.stepup.android.domain.GpsHint
import com.stepup.android.domain.GpsWait
import com.stepup.android.domain.gpsHintOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 시작 전 GPS 대기(2026-10-16 — GPS 가 없으면 러닝을 시작하지 않는다) — 무엇을 "잡혔다"로 보고, 오래 걸리면 무엇을 먼저 알리는가 */
class GpsReadinessTest {

    @Test fun `a fresh real GPS fix within 50m starts the run`() {
        assertTrue(GpsFixRule.accepts(fromGps = true, mock = false, accuracyM = 8f, ageMs = 0))
        assertTrue(GpsFixRule.accepts(fromGps = true, mock = false, accuracyM = 50f, ageMs = 30_000))
        // 정확도를 알려 주지 않는 기기 — 그 때문에 영영 시작하지 못하게 하지 않는다
        assertTrue(GpsFixRule.accepts(fromGps = true, mock = false, accuracyM = null, ageMs = 1_000))
    }

    @Test fun `network fixes, mock fixes, stale fixes and loose fixes do not count`() {
        // 기지국 · 와이파이 위치 — GPS 가 아니다
        assertFalse(GpsFixRule.accepts(fromGps = false, mock = false, accuracyM = 20f, ageMs = 0))
        // 모의 위치 앱이 넣은 좌표
        assertFalse(GpsFixRule.accepts(fromGps = true, mock = true, accuracyM = 3f, ageMs = 0))
        // 휴대폰이 30초보다 전에 알던 위치
        assertFalse(GpsFixRule.accepts(fromGps = true, mock = false, accuracyM = 5f, ageMs = 30_001))
        // 아직 50m 보다 부정확하다
        assertFalse(GpsFixRule.accepts(fromGps = true, mock = false, accuracyM = 51f, ageMs = 0))
    }

    @Test fun `blocking causes are told at once, waiting causes after 20 seconds`() {
        // 막는 까닭 — 기다려도 소용없으니 바로
        assertEquals(GpsHint.NO_GPS, gpsHintOf(GpsWait(hasGps = false)))
        assertEquals(GpsHint.LOCATION_OFF, gpsHintOf(GpsWait(locationOn = false)))
        assertEquals(GpsHint.GPS_OFF, gpsHintOf(GpsWait(gpsOn = false)))
        assertEquals(GpsHint.MOCK, gpsHintOf(GpsWait(mock = true)))
        // 기다리는 중 — 20초 전에는 아무것도 알리지 않는다
        assertEquals(GpsHint.NONE, gpsHintOf(GpsWait(elapsedSec = 19, satellitesSeen = 0, satellitesUsed = 0)))
    }

    @Test fun `after 20 seconds the hint says why — no satellites, weak signal or a slow first fix`() {
        val waited = GpsWait(elapsedSec = GpsFixRule.HINT_AFTER_SEC)
        // 받는 위성이 하나도 없다 — 실내 · 지하
        assertEquals(GpsHint.NO_SATELLITES, gpsHintOf(waited.copy(satellitesSeen = 0, satellitesUsed = 0)))
        // 위치는 오는데 아직 50m 밖이다
        assertEquals(GpsHint.WEAK, gpsHintOf(waited.copy(satellitesSeen = 6, satellitesUsed = 3, accuracyM = 120f)))
        // 위성은 보이는데 첫 위치가 늦다 — 유심 없는 휴대폰 · 오래 안 쓴 GPS
        assertEquals(GpsHint.SLOW, gpsHintOf(waited.copy(satellitesSeen = 7, satellitesUsed = 0)))
        // 위성 수를 알려 주지 않는 휴대폰도 늦은 첫 위치로 안내한다
        assertEquals(GpsHint.SLOW, gpsHintOf(waited))
    }

    @Test fun `wifi location alone explains why maps works indoors but the run waits for satellites`() {
        val waited = GpsWait(elapsedSec = GpsFixRule.HINT_AFTER_SEC)
        // 실내 — 와이파이 위치는 있는데 위성은 하나도 쓰지 못한다(구글 지도는 이걸로 위치를 보인다)
        assertEquals(GpsHint.WIFI_ONLY, gpsHintOf(waited.copy(roughFix = true, roughAccuracyM = 22f, satellitesSeen = 0, satellitesUsed = 0)))
        assertEquals(GpsHint.WIFI_ONLY, gpsHintOf(waited.copy(roughFix = true)))
        // 와이파이 위치만으로는 잡힘이 아니다 — 20초 전에는 알리지도 않는다
        assertEquals(GpsHint.NONE, gpsHintOf(GpsWait(elapsedSec = 5, roughFix = true, roughAccuracyM = 15f)))
        // GPS 위치가 오기 시작했으면 신호 약함이 더 정확한 까닭이다
        assertEquals(GpsHint.WEAK, gpsHintOf(waited.copy(roughFix = true, accuracyM = 80f, satellitesUsed = 3)))
        // 위성을 쓰기 시작했으면 곧 잡힌다 — 첫 위치 늦음
        assertEquals(GpsHint.SLOW, gpsHintOf(waited.copy(roughFix = true, satellitesSeen = 9, satellitesUsed = 4)))
    }

    @Test fun `once fixed nothing is hinted`() {
        assertEquals(GpsHint.NONE, gpsHintOf(GpsWait(fixed = true, elapsedSec = 300, satellitesSeen = 0, accuracyM = 30f)))
    }
}
