package com.stepup.android

import com.stepup.android.ui.screens.records.paceValue
import com.stepup.android.ui.screens.records.runPace
import com.stepup.android.ui.screens.records.speedText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 기록의 페이스 · 평균 속도(2026-10-17 점검) — 러닝 중 · 결과처럼 50m 를 넘어야 보인다. 예전 기록 상세는 1분 동안 15m 를
 * 65'37"/km 로 보였다. 규칙(RunPace)은 공용 모듈로 옮겼고 그 검사도 함께 갔다(shared 의 RunPaceTest) — 여기서는 화면 글자만 본다.
 */
class RunPaceTest {

    @Test
    fun `50m 가 안 되면 페이스와 속도를 보이지 않는다`() {
        // 1분 · 15m — 예전엔 65'37"/km
        assertNull(paceValue(60, 15.24))
        assertNull(runPace(60, 15.24))
        assertNull(speedText(60, 15.24))
    }

    @Test
    fun `50m 부터는 시간을 거리로 나눈다`() {
        assertEquals("5'00\"", paceValue(1_500, 5_000.0))
        assertEquals("5'00\" /km", runPace(1_500, 5_000.0))
        assertEquals("5'00\"/km", runPace(1_500, 5_000.0, compact = true))
        assertEquals("12.0", speedText(1_500, 5_000.0))
    }
}
