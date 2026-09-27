package com.stepup.android

import com.stepup.android.ui.components.formatSupDown
import org.junit.Assert.assertEquals
import org.junit.Test

/** SUP 는 내려 보이되, 폰이 더한 합의 부동소수 찌꺼기 때문에 한 단위 덜 보이지 않는다 */
class SupFormatTest {
    // 소수점 기호는 기기 언어를 따른다 — 기대값도 같은 방식으로 만든다
    private fun two(value: Double) = "%.2f".format(value)

    @Test fun floatingPointSumsAreNotCutOneUnitShort() {
        var tenDimes = 0.0
        repeat(10) { tenDimes += 0.1 } // 0.9999999999999999
        assertEquals("1", formatSupDown(tenDimes))
        assertEquals(two(0.8), formatSupDown(0.7 + 0.1, 2)) // 0.7999999999999999
        assertEquals(two(3.6), formatSupDown(1.2 + 1.2 + 1.2, 2)) // 3.5999999999999996
    }

    @Test fun stillRoundsTowardZero() {
        assertEquals("499", formatSupDown(499.6))
        assertEquals("499", formatSupDown(499.9999))
        assertEquals(two(3.45), formatSupDown(3.4567, 2))
        assertEquals(two(-1.23), formatSupDown(-1.2345, 2))
        assertEquals("—", formatSupDown(Double.NaN))
    }
}
