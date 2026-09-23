package com.stepup.android

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.ui.screens.settings.PrivacyScreen
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** The permissions suite revokes permissions before starting this process, never during it. */
class PrivacyPermissionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun deniedApproximateAndPrecisePermissionsRefreshAfterSettingsReturn() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        val context = instrumentation.targetContext
        val permissions = listOf(
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS,
        )
        permissions.forEach {
            assertEquals("Suite must begin denied: $it", PackageManager.PERMISSION_DENIED,
                ContextCompat.checkSelfPermission(context, it))
        }
        assertFalse(NotificationManagerCompat.from(context).areNotificationsEnabled())
        compose.setContent { StepUpTheme(ThemeMode.DARK) { PrivacyScreen() } }

        fun status(label: Int, value: Int) {
            val labelText = context.getString(label)
            compose.onNodeWithText(labelText).performScrollTo()
            compose.onNode(hasText(context.getString(value)) and hasText(labelText))
                .assertIsDisplayed()
        }
        fun capture(name: String) {
            compose.waitForIdle()
            captureDisplay(File(context.getExternalFilesDir(null), "form-checks/privacy-$name.png"))
        }
        fun shell(command: String): String =
            android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command))
                .bufferedReader().use { it.readText() }
        fun settingsRoundTrip(grants: List<String>) {
            compose.onNodeWithText(context.getString(R.string.cd_open_settings))
                .performScrollTo().performClick()
            // Inspect the actual external window, not a mocked Intent callback.
            val deadline = android.os.SystemClock.elapsedRealtime() + 10_000
            var settingsVisible = false
            while (!settingsVisible && android.os.SystemClock.elapsedRealtime() < deadline) {
                settingsVisible = shell("dumpsys activity activities").lineSequence().any {
                    (it.contains("mResumedActivity") || it.contains("topResumedActivity")) &&
                        it.contains("com.android.settings/")
                }
                if (!settingsVisible) android.os.SystemClock.sleep(100)
            }
            assertTrue("The app-settings button must open Android Settings", settingsVisible)
            grants.forEach { automation.grantRuntimePermission(context.packageName, it) }
            shell("input keyevent KEYCODE_BACK")
            compose.waitUntil(10_000) {
                compose.activity.lifecycle.currentState == Lifecycle.State.RESUMED
            }
            compose.waitForIdle()
        }

        status(R.string.privacy_perm_activity, R.string.privacy_permission_not_allowed)
        status(R.string.privacy_perm_location, R.string.privacy_permission_not_allowed)
        status(R.string.privacy_perm_notification, R.string.privacy_permission_not_allowed)
        capture("denied")

        settingsRoundTrip(listOf(Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.POST_NOTIFICATIONS))
        status(R.string.privacy_perm_activity, R.string.privacy_permission_allowed)
        status(R.string.privacy_perm_location, R.string.privacy_location_approximate)
        status(R.string.privacy_perm_notification, R.string.privacy_permission_allowed)
        capture("approximate")

        settingsRoundTrip(listOf(Manifest.permission.ACCESS_FINE_LOCATION))
        status(R.string.privacy_perm_location, R.string.privacy_location_precise)
        capture("precise")
    }
}
