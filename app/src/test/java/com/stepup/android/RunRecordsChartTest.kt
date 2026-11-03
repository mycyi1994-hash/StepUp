package com.stepup.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 기록 막대 그림의 눈금 · 경로 줄이기 — 화면 코드라 앱에 남는다(규칙은 공용 RunRecordsTest) */
class RunRecordsChartTest {
    @Test
    fun `눈금은 가장 큰 막대가 두세 칸 안에 들어오게, 경로 줄이기는 모양과 끝점을 남긴다`() {
        // 시안: 주간(가장 큰 5.0km)은 0 · 3 · 6, 월간(12.2km)은 0 · 5 · 10 · 15
        assertEquals(3.0, com.stepup.android.ui.screens.records.niceStep(5.0), 0.0)
        assertEquals(6.0, com.stepup.android.ui.screens.records.chartTop(5.0, 3.0), 0.0)
        assertEquals(5.0, com.stepup.android.ui.screens.records.niceStep(12.2), 0.0)
        assertEquals(15.0, com.stepup.android.ui.screens.records.chartTop(12.2, 5.0), 0.0)
        assertEquals(2.0, com.stepup.android.ui.screens.records.niceStep(3.2), 0.0)
        assertEquals(0.5, com.stepup.android.ui.screens.records.niceStep(0.0), 0.0)
        val points = (0 until 1000).map { com.stepup.android.domain.GeoPoint(37.0 + it * 1e-5, 127.0) }
        val thinned = com.stepup.android.ui.screens.records.thin(points, 160)
        assertEquals(points.first(), thinned.first())
        assertEquals(points.last(), thinned.last())
        assertTrue(thinned.size <= 161)
        assertEquals(points.take(100), com.stepup.android.ui.screens.records.thin(points.take(100), 160))
    }
}
