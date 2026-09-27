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
import com.stepup.android.core.ServiceLocator
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Routes
import com.stepup.android.ui.experience.ExperienceProvider
import kotlinx.coroutines.runBlocking
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
        runBlocking { ServiceLocator.userPrefs.setGuideSeen(); ServiceLocator.userPrefs.setReducedMotion(true) }
        compose.setContent {
            StepUpTheme(ThemeMode.DARK) {
                ExperienceProvider { MainScaffold(initialRoute = Routes.SETTINGS_PRIVACY) }
            }
        }

        // 설정 v1 개인정보 · 앱 권한 — 권한 줄은 글자로 상태를 보인다(앱 안 스위치가 아니다)
        fun status(tag: String, value: Int) {
            compose.onNodeWithTag(tag).performScrollTo()
            compose.onNodeWithTag(tag).assert(hasText(context.getString(value))).assertIsDisplayed()
        }
        fun capture(name: String) {
            compose.waitForIdle()
            captureDisplay(File(context.getExternalFilesDir(null), "form-checks/privacy-$name.png"))
        }
        fun shell(command: String): String =
            android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command))
                .bufferedReader().use { it.readText() }
        fun settingsRoundTrip(grants: List<String>) {
            compose.onNodeWithTag("privacy-open-settings").performScrollTo().performClick()
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

        status("perm-activity", R.string.set_perm_denied)
        status("perm-location", R.string.set_perm_denied)
        status("perm-notifications", R.string.set_perm_denied)
        capture("denied")

        settingsRoundTrip(listOf(Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.POST_NOTIFICATIONS))
        status("perm-activity", R.string.set_perm_allowed)
        status("perm-location", R.string.set_perm_approximate)
        status("perm-notifications", R.string.set_perm_allowed)
        capture("approximate")

        settingsRoundTrip(listOf(Manifest.permission.ACCESS_FINE_LOCATION))
        status("perm-location", R.string.set_perm_precise)
        capture("precise")
    }
}
