package com.stepup.android

import com.stepup.android.domain.DietRoutine
import com.stepup.android.domain.DietSegmentKind
import com.stepup.android.domain.RunExperience
import com.stepup.android.ui.screens.walk.BodyInputError
import com.stepup.android.ui.screens.walk.DietInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DietRoutineTest {
    @Test
    fun beginnerRoutineIsTheHandoffExample() {
        // 준비 걷기 3분 + (러닝 1분 + 걷기 2분) × 3 + 마무리 걷기 3분 = 15분, 러닝 3분 · 걷기 12분, 8구간
        val r = DietRoutine.forExperience(RunExperience.FIRST)
        assertEquals(15 * 60L, r.totalSec)
        assertEquals(3 * 60L, r.totalRunSec)
        assertEquals(12 * 60L, r.totalWalkSec)
        assertEquals(8, r.segments.size)
        assertEquals(DietSegmentKind.WARMUP, r.segments.first().kind)
        assertEquals(DietSegmentKind.COOLDOWN, r.segments.last().kind)
    }

    @Test
    fun otherLevelsAreFixedToo() {
        val sometimes = DietRoutine.forExperience(RunExperience.SOMETIMES)
        assertEquals(18 * 60L, sometimes.totalSec)
        assertEquals(10, sometimes.segments.size)
        val steady = DietRoutine.forExperience(RunExperience.STEADY)
        assertEquals(21 * 60L, steady.totalSec)
        assertEquals(10 * 60L, steady.totalRunSec)
        assertEquals(11 * 60L, steady.totalWalkSec)
        assertEquals(12, steady.segments.size)
    }

    @Test
    fun segmentsFollowElapsedTimeAndSwitchByThemselves() {
        val r = DietRoutine.forExperience(RunExperience.FIRST)
        val warmup = r.at(44)!!
        assertEquals(0, warmup.index)
        assertEquals(136, warmup.remainingSec)
        // 3분이 지나면 저절로 첫 러닝 구간
        val run = r.at(180)!!
        assertEquals(DietSegmentKind.RUN, run.segment.kind)
        assertEquals(1, run.segment.round)
        assertEquals(60, run.remainingSec)
        val walk = r.at(240 + 32)!!
        assertEquals(DietSegmentKind.WALK, walk.segment.kind)
        assertEquals(88, walk.remainingSec)
        assertEquals(DietSegmentKind.COOLDOWN, r.at(15 * 60L - 1)!!.segment.kind)
        assertNull(r.at(15 * 60L))
        assertTrue(r.finished(15 * 60L))
        assertFalse(r.finished(15 * 60L - 1))
    }

    @Test
    fun completedSegmentsCountOnlyFinishedParts() {
        val r = DietRoutine.forExperience(RunExperience.FIRST)
        assertEquals(0, r.completedSegments(179))
        assertEquals(1, r.completedSegments(180))
        assertEquals(2, r.completedSegments(6 * 60L + 32 - 150))
        assertEquals(8, r.completedSegments(10_000))
    }

    @Test
    fun bodyInputIsCheckedInPlace() {
        assertEquals(BodyInputError.EMPTY, DietInput.heightError(""))
        assertEquals(BodyInputError.RANGE, DietInput.heightError("99"))
        assertNull(DietInput.heightError("170"))
        assertEquals(BodyInputError.EMPTY, DietInput.weightError(""))
        assertEquals(BodyInputError.RANGE, DietInput.weightError("300"))
        assertNull(DietInput.weightError("70.5"))
    }

    @Test
    fun keypadKeepsNumbersInShape() {
        assertEquals("170", DietInput.type("17", '0', decimal = false))
        assertEquals("170", DietInput.type("170", '5', decimal = false))
        assertEquals("170", DietInput.type("170", '.', decimal = false))
        assertEquals("70.", DietInput.type("70", '.', decimal = true))
        assertEquals("70.5", DietInput.type("70.", '5', decimal = true))
        assertEquals("70.5", DietInput.type("70.5", '1', decimal = true))
        assertEquals("0.", DietInput.type("", '.', decimal = true))
        assertEquals("7", DietInput.type("0", '7', decimal = true))
    }
}
