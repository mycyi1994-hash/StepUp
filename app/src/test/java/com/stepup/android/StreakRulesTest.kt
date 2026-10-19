package com.stepup.android

import com.stepup.android.domain.StreakRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "연속 활동" — 저장된 값은 목표를 채운 날에만 바뀌어, 하루를 놓쳐도 다음에 채울 때까지 옛 값이 보였다(2026-10-17 점검).
 * 화면은 마지막으로 채운 날을 보고 끊겼으면 0 을 보인다. 보호막 규칙은 StepRepository.onSteps 가 이어 셀 때와 같다.
 */
class StreakRulesTest {

    private val today = 20_000L

    @Test
    fun `오늘이나 어제 채웠으면 저장된 값 그대로다`() {
        assertEquals(5, StreakRules.shown(stored = 5, lastMetDay = today, today = today, shieldCoveredYesterday = false))
        // 오늘은 아직 못 채웠어도 어제 채웠으면 이어진다
        assertEquals(5, StreakRules.shown(stored = 5, lastMetDay = today - 1, today = today, shieldCoveredYesterday = false))
    }

    @Test
    fun `어제를 놓쳤으면 0 이다`() {
        assertEquals(0, StreakRules.shown(stored = 12, lastMetDay = today - 2, today = today, shieldCoveredYesterday = false))
        assertEquals(0, StreakRules.shown(stored = 12, lastMetDay = today - 30, today = today, shieldCoveredYesterday = false))
    }

    @Test
    fun `그제 채우고 어제를 보호막이 덮었으면 아직 이어진다`() {
        assertEquals(12, StreakRules.shown(stored = 12, lastMetDay = today - 2, today = today, shieldCoveredYesterday = true))
        // 보호막은 하루만 잇는다 — 사흘 전이면 끊겼다
        assertEquals(0, StreakRules.shown(stored = 12, lastMetDay = today - 3, today = today, shieldCoveredYesterday = true))
    }

    @Test
    fun `보호막은 그제 채웠을 때만 물어본다`() {
        assertTrue(StreakRules.needsShield(lastMetDay = today - 2, today = today))
        assertFalse(StreakRules.needsShield(lastMetDay = today - 1, today = today))
        assertFalse(StreakRules.needsShield(lastMetDay = today - 3, today = today))
        assertFalse(StreakRules.needsShield(lastMetDay = -1, today = 1))
    }

    @Test
    fun `채운 적이 없으면 0 이다`() {
        assertEquals(0, StreakRules.shown(stored = 0, lastMetDay = today, today = today, shieldCoveredYesterday = false))
        assertEquals(0, StreakRules.shown(stored = 3, lastMetDay = -1, today = today, shieldCoveredYesterday = false))
    }

    @Test
    fun `폰 날짜를 되돌려 마지막 날이 오늘보다 뒤여도 끊겼다고 하지 않는다`() {
        assertEquals(4, StreakRules.shown(stored = 4, lastMetDay = today + 2, today = today, shieldCoveredYesterday = false))
    }
}
