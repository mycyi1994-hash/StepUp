package com.stepup.android

import android.Manifest.permission.*
import com.stepup.android.ui.StepPermissions
import org.junit.Assert.*
import org.junit.Test

class StepPermissionsTest {
    @Test fun freshModernInstallRequestsBothLocationPermissions() {
        assertEquals(setOf(ACTIVITY_RECOGNITION, POST_NOTIFICATIONS, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
            StepPermissions.requiredPermissions(35) { false }.toSet())
    }

    @Test fun approximateChoiceDoesNotTriggerRepeatedPrecisionRequests() {
        val granted = setOf(ACTIVITY_RECOGNITION, POST_NOTIFICATIONS, ACCESS_COARSE_LOCATION)
        assertTrue(StepPermissions.requiredPermissions(35) { it in granted }.isEmpty())
    }

    @Test fun olderAndroidDoesNotAskForUnsupportedRuntimePermissions() {
        assertEquals(setOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
            StepPermissions.requiredPermissions(28) { false }.toSet())
        assertEquals(setOf(ACTIVITY_RECOGNITION, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
            StepPermissions.requiredPermissions(31) { false }.toSet())
    }
}
