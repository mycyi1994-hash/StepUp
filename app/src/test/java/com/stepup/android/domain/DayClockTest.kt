package com.stepup.android.domain

import com.stepup.android.sensor.StepReading
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** 자정이 지나면 홈의 오늘 걸음 · 연속 일수를 다시 본다(점검 2026-11-17 #24) */
class DayClockTest {
    @Test
    fun `날짜가 바뀔 때만 새 값을 낸다`() = runBlocking {
        val days = ArrayDeque(listOf(10L, 10L, 10L, 11L, 11L, 12L))
        val seen = DayClock.ticker(today = { days.removeFirstOrNull() ?: 12L }, pollMs = 1).take(3).toList()
        assertEquals(listOf(10L, 11L, 12L), seen)
    }

    @Test
    fun `어제 잰 걸음은 오늘 0 으로 보인다`() {
        val reading = StepReading(day = 10L, steps = 8_000)
        assertEquals(8_000, reading.shownOn(10L))
        assertEquals(0, reading.shownOn(11L))
    }
}
