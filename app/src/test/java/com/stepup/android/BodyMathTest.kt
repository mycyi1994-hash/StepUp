package com.stepup.android

import com.stepup.android.domain.BmiBand
import com.stepup.android.domain.BodyMath
import com.stepup.android.domain.BodyProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyMathTest {
    @Test fun bmiUsesMetres() {
        assertEquals(22.5, BodyMath.bmi(162, 59.0)!!, 0.05)
        assertNull(BodyMath.bmi(null, 59.0))
        assertNull(BodyMath.bmi(162, null))
    }

    @Test fun koreanObesityBandsHaveInclusiveLowerEdges() {
        assertEquals(BmiBand.UNDER, BodyMath.band(18.49))
        assertEquals(BmiBand.NORMAL, BodyMath.band(18.5))
        assertEquals(BmiBand.NORMAL, BodyMath.band(22.99))
        assertEquals(BmiBand.PRE_OBESE, BodyMath.band(23.0))
        assertEquals(BmiBand.OBESE, BodyMath.band(25.0))
    }

    @Test fun halfKiloStepsDoNotDrift() {
        var w = 60.0
        repeat(7) { w = BodyMath.clampWeight(w - 0.5) }
        assertEquals(56.5, w, 0.0)
        assertEquals(BodyMath.MAX_WEIGHT, BodyMath.clampWeight(999.0), 0.0)
        assertEquals(BodyMath.MIN_HEIGHT, BodyMath.clampHeight(10))
    }

    @Test fun cautionOnFastLossOrUnderweightGoalOnly() {
        val base = BodyProfile(heightCm = 162, weightKg = 59.0, goalWeeks = 12)
        assertFalse(BodyMath.needsCaution(base.copy(goalWeightKg = 52.5)))  // 0.54 kg/week, BMI 20.0
        assertTrue(BodyMath.needsCaution(base.copy(goalWeightKg = 45.0)))   // underweight goal
        assertTrue(BodyMath.needsCaution(base.copy(goalWeightKg = 50.0, goalWeeks = 8))) // 1.1 kg/week
        assertFalse(BodyMath.needsCaution(base.copy(goalWeightKg = 65.0)))  // gaining is not flagged as fast loss
        assertFalse(BodyMath.needsCaution(BodyProfile()))
    }
}
