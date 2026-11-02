package com.stepup.android.domain

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.round
import kotlin.math.roundToLong
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/** 안드로이드에서 옮기기 전 JVM 호출과 비트까지 같은가(2026-11-02 KMP 2단계) — 땅따먹기 칸 · 기록 좌표 문자열이 바뀌지 않게 */
class PlatformJvmTest {
    private val values = Random(20261102).let { r -> List(20_000) { r.nextDouble(-400.0, 400.0) } } +
        listOf(0.0, -0.0, 0.5, 1.5, 2.5, -2.5, 2.675, 180.0, 90.0, 37.5665, 126.978)

    @Test fun `Math 와 같은 값`() {
        for (v in values) {
            assertEquals(Math.toRadians(v), toRadians(v), "toRadians($v)")
            assertEquals(Math.toDegrees(v), toDegrees(v), "toDegrees($v)")
            assertEquals(Math.rint(v), round(v), "rint($v)")
            assertEquals(Math.round(v * 1_000_000.0), (v * 1_000_000.0).roundToLong(), "round($v)")
        }
    }

    @Test fun `BigDecimal 반올림과 같은 값`() {
        for (v in values) {
            assertEquals(BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).toDouble(), roundHalfUp2(v), "coarse($v)")
        }
    }
}
