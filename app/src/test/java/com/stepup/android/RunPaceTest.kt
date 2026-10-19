package com.stepup.android

import com.stepup.android.domain.RunPace
import com.stepup.android.ui.screens.records.paceValue
import com.stepup.android.ui.screens.records.runPace
import com.stepup.android.ui.screens.records.speedText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 기록의 페이스 · 평균 속도(2026-10-17 점검) — 러닝 중 · 결과처럼 50m 를 넘어야 보인다. 예전 기록 상세는 1분 동안 15m 를
 * 65'37"/km 로 보였다. 여러 러닝의 평균은 거리와 시간이 모두 있는 러닝만으로 낸다(시간만 잰 러닝이 평균을 늦추지 않게).
 */
class RunPaceTest {

    @Test
    fun `50m 가 안 되면 페이스와 속도를 보이지 않는다`() {
        // 1분 · 15m — 예전엔 65'37"/km
        assertNull(RunPace.secPerKm(60, 15.24))
        assertNull(RunPace.kmh(60, 15.24))
        assertNull(paceValue(60, 15.24))
        assertNull(runPace(60, 15.24))
        assertNull(speedText(60, 15.24))
        assertNull(RunPace.secPerKm(60, 49.9))
    }

    @Test
    fun `50m 부터는 시간을 거리로 나눈다`() {
        assertEquals(600.0, RunPace.secPerKm(30, 50.0)!!, 1e-9)
        assertEquals(300.0, RunPace.secPerKm(1_500, 5_000.0)!!, 1e-9)
        assertEquals(9.0, RunPace.kmh(600, 1_500.0)!!, 1e-9)
        assertEquals("5'00\"", paceValue(1_500, 5_000.0))
        assertEquals("5'00\" /km", runPace(1_500, 5_000.0))
        assertEquals("5'00\"/km", runPace(1_500, 5_000.0, compact = true))
        assertEquals("12.0", speedText(1_500, 5_000.0))
    }

    @Test
    fun `시간이 없거나 거리가 이상하면 보이지 않는다`() {
        assertNull(RunPace.secPerKm(0, 5_000.0))
        assertNull(RunPace.secPerKm(-5, 5_000.0))
        assertNull(RunPace.secPerKm(600, Double.NaN))
        assertNull(RunPace.kmh(600, Double.POSITIVE_INFINITY))
    }

    @Test
    fun `평균은 거리와 시간이 모두 있는 러닝만 더한다`() {
        val runs = listOf(
            600L to 1_500.0,
            // 시간만 잰 러닝(거리 0) — 예전엔 이 20분까지 나눠 평균 페이스가 느려졌다
            1_200L to 0.0,
            // 거리만 있고 시간이 없는 러닝
            0L to 300.0,
            300L to 500.0,
        )
        val (seconds, meters) = RunPace.pacedTotals(runs)
        assertEquals(900L, seconds)
        assertEquals(2_000.0, meters, 1e-9)
        assertEquals(450.0, RunPace.secPerKm(seconds, meters)!!, 1e-9)
        assertEquals(8.0, RunPace.kmh(seconds, meters)!!, 1e-9)
    }

    @Test
    fun `러닝이 없으면 바탕도 비어 있다`() {
        assertEquals(0L to 0.0, RunPace.pacedTotals(emptyList()))
        assertNull(RunPace.secPerKm(0, 0.0))
    }
}
