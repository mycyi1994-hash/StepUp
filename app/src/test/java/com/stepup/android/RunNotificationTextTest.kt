package com.stepup.android

import com.stepup.android.domain.RunNotificationText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 러닝 중 알림(잠금화면, 2026-10-18)의 거리 · 페이스 · 시계 기준 */
class RunNotificationTextTest {
    @Test fun `거리는 소수 둘째 자리 · 음수는 0`() {
        assertEquals("2.31", RunNotificationText.distance(2.3149))
        assertEquals("0.00", RunNotificationText.distance(-1.0))
    }

    @Test fun `페이스 — 50m 전 · 시간 0 · 한 시간 넘는 페이스는 보이지 않는다`() {
        assertEquals("5'42\"", RunNotificationText.pace(342, 1.0))
        assertEquals("6'00\"", RunNotificationText.pace(900, 2.5))
        assertNull(RunNotificationText.pace(60, 0.04))
        assertNull(RunNotificationText.pace(0, 1.0))
        assertNull(RunNotificationText.pace(4_000, 1.0))
    }

    @Test fun `시계 기준은 지금에서 달린 시간을 뺀 값`() {
        assertEquals(100_000L - 65_000L, RunNotificationText.chronometerBase(100_000L, 65))
    }
}
