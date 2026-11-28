package com.stepup.android

import com.stepup.android.domain.FixClock
import org.junit.Assert.assertEquals
import org.junit.Test

class FixClockTest {

    @Test
    fun batchedFixesKeepTheirOwnTimes() {
        // 화면이 꺼져 60초 동안 잰 점들이 한꺼번에 왔다 — 받은 시각은 모두 같아도 잰 시각은 2.5초씩 다르다
        val wallNow = 1_800_000_000_000L
        val elapsedNow = 500_000L
        val first = FixClock.wallTime(wallNow, elapsedNow, (elapsedNow - 60_000) * 1_000_000)
        val second = FixClock.wallTime(wallNow, elapsedNow, (elapsedNow - 57_500) * 1_000_000)
        assertEquals(wallNow - 60_000, first)
        assertEquals(2_500, second - first)
    }

    @Test
    fun unknownFutureOrVeryOldFallsBackToNow() {
        val wallNow = 1_800_000_000_000L
        assertEquals(wallNow, FixClock.wallTime(wallNow, 500_000L, 0L))
        assertEquals(wallNow, FixClock.wallTime(wallNow, 500_000L, 600_000L * 1_000_000))
        assertEquals(wallNow, FixClock.wallTime(wallNow, 5_000_000L, 1_000L * 1_000_000))
    }
}
