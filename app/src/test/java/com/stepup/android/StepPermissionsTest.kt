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

    @Test fun groupStartNeverWaitsOnTheNotificationPermission() {
        // 함께 뛰기 출발 순간에는 걸음 · 위치만 본다 — 알림 권한 창이 출발을 늦추지 않게(알림은 준비할 때 묻는다)
        assertEquals(setOf(ACTIVITY_RECOGNITION, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
            StepPermissions.startPermissions(35) { false }.toSet())
        assertTrue(StepPermissions.startPermissions(35) { it != POST_NOTIFICATIONS }.isEmpty())
        assertEquals(setOf(ACTIVITY_RECOGNITION), StepPermissions.startPermissions(35) { it != ACTIVITY_RECOGNITION }.toSet())
    }

    @Test fun olderAndroidDoesNotAskForUnsupportedRuntimePermissions() {
        assertEquals(setOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
            StepPermissions.requiredPermissions(28) { false }.toSet())
        assertEquals(setOf(ACTIVITY_RECOGNITION, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
            StepPermissions.requiredPermissions(31) { false }.toSet())
    }
}
