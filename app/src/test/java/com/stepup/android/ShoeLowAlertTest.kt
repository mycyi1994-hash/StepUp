package com.stepup.android

import com.stepup.android.domain.ShoeLowAlert
import com.stepup.android.domain.ShoeWearSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShoeLowAlertTest {

    @Test
    fun percentDropsEachFullMinute() {
        // 최대 1,000 · 남은 300(30%) · 1분마다 5 → 1분 지나면 295(29%)
        val s = ShoeWearSnapshot(300, 1000, 5.0)
        assertEquals(30, s.percentAt(0))
        assertEquals(30, s.percentAt(59))
        assertEquals(29, s.percentAt(60))
        assertEquals(20, s.percentAt(20 * 60))
        assertEquals(0, s.percentAt(10_000))
    }

    @Test
    fun startFloorSkipsPassedThresholds() {
        assertEquals(ShoeLowAlert.NONE, ShoeLowAlert.startFloor(80))
        assertEquals(20, ShoeLowAlert.startFloor(20))
        assertEquals(20, ShoeLowAlert.startFloor(15))
        assertEquals(10, ShoeLowAlert.startFloor(8))
    }

    @Test
    fun alertsTwentyThenTenOnce() {
        var floor = ShoeLowAlert.startFloor(40)
        assertNull(ShoeLowAlert.crossed(floor, 21))
        assertEquals(20, ShoeLowAlert.crossed(floor, 20))
        floor = 20
        assertNull(ShoeLowAlert.crossed(floor, 19))
        assertNull(ShoeLowAlert.crossed(floor, 11))
        assertEquals(10, ShoeLowAlert.crossed(floor, 10))
        floor = 10
        assertNull(ShoeLowAlert.crossed(floor, 5))
    }

    @Test
    fun jumpingBothAlertsOnlyLowerAndZeroIsSeparate() {
        assertEquals(10, ShoeLowAlert.crossed(ShoeLowAlert.NONE, 9))
        assertNull(ShoeLowAlert.crossed(ShoeLowAlert.NONE, 0))
        // 15%에서 출발하면 20% 는 건너뛰고 10% 만
        assertEquals(10, ShoeLowAlert.crossed(ShoeLowAlert.startFloor(15), 10))
    }
}
