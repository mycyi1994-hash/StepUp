package com.stepup.android

import com.stepup.android.sensor.DayReading
import com.stepup.android.sensor.StepBaseline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StepBaselineTest {
    private val today = 20_000L

    @Test fun carriesStepsWalkedSinceLastReadingYesterday() {
        // 어제 기준점 10,000 · 저장 3,000 → 마지막 누적 13,000. 지금 15,500 이면 2,500 을 넘긴다
        assertEquals(2_500L, StepBaseline.carryOver(today - 1, 10_000, today, 15_500, 3_000, rebooted = false))
    }

    @Test fun rebootYesterdayKeepsCarryOver() {
        // 어제 재부팅해 기준점이 −4,000(부팅 전 걸음). 그 뒤 누적 2,000 → 저장 6,000.
        // 오늘 아침 누적 3,500 이면 마지막 측정(2,000) 뒤 1,500 보를 넘긴다 — 예전엔 음수 기준점이라 0 이었다
        assertEquals(1_500L, StepBaseline.carryOver(today - 1, -4_000, today, 3_500, 6_000, rebooted = false))
    }

    @Test fun overnightRebootCarriesStepsSinceBoot() {
        // 어제 누적 50,000 에서 본 뒤 밤에 재부팅. 지금 누적 800 은 모두 부팅 뒤 걸음이다
        assertEquals(800L, StepBaseline.carryOver(today - 1, 40_000, today, 800, 10_000, rebooted = true))
    }

    @Test fun overnightRebootIsCapped() {
        assertEquals(StepBaseline.CARRY_OVER_CAP, StepBaseline.carryOver(today - 1, 0, today, 30_000, 0, rebooted = true))
    }

    @Test fun counterBelowLastSeenWithoutBootInfoCarriesNothing() {
        assertEquals(0L, StepBaseline.carryOver(today - 1, 40_000, today, 800, 10_000, rebooted = false))
    }

    @Test fun staleOrMissingBaselineCarriesNothing() {
        assertEquals(0L, StepBaseline.carryOver(today - 3, 10_000, today, 15_000, 3_000, rebooted = false))
        assertEquals(0L, StepBaseline.carryOver(-1, -1, today, 15_000, 0, rebooted = true))
    }

    @Test fun carryIsCapped() {
        assertEquals(StepBaseline.CARRY_OVER_CAP, StepBaseline.carryOver(today - 1, 0, today, 50_000, 1_000, rebooted = false))
    }

    @Test fun rebootNeedsBothBootCounts() {
        assertTrue(StepBaseline.rebooted(5, 4))
        assertFalse(StepBaseline.rebooted(5, 5))
        assertFalse(StepBaseline.rebooted(-1, 4))
        assertFalse(StepBaseline.rebooted(5, -1))
    }

    @Test fun readingFromYesterdayShowsZeroToday() {
        val reading = DayReading(today - 1, 9_000)
        assertEquals(0, reading.stepsOn(today))
        assertEquals(9_000, reading.stepsOn(today - 1))
        assertEquals(1_200, DayReading(today, 1_200).stepsOn(today))
    }
}
