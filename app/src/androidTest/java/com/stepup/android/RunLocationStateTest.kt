package com.stepup.android

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.rule.GrantPermissionRule
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.GeoPoint
import com.stepup.android.service.WalkSessionService
import com.stepup.android.service.WalkSessionState
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Routes
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test

/**
 * 러닝 중 위치 상태 — GPS 전에 대략적인 위치를 지도에 먼저 보이는지, 위치가 안 잡히는 까닭
 * (휴대폰 위치 꺼짐 · 대략적인 위치만 허용)을 알려 주는지. 화면용 상태만 넣는다(실제 GPS 아님).
 */
class RunLocationStateTest {
    @get:Rule(order = 0) val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACTIVITY_RECOGNITION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()

    @After fun reset() = WalkSessionService.showStateForTest(WalkSessionState())

    private fun show(state: WalkSessionState) {
        runBlocking {
            ServiceLocator.userPrefs.setReducedMotion(true)
            ServiceLocator.userPrefs.setGuideSeen()
        }
        WalkSessionService.showStateForTest(state)
        compose.setContent { StepUpTheme(ThemeMode.DARK) { ExperienceProvider { MainScaffold(initialRoute = Routes.RUN) } } }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("run-live-map").fetchSemanticsNodes().isNotEmpty() }
    }

    private val running = WalkSessionState(isActive = true, startedAt = System.currentTimeMillis() - 60_000, elapsedSec = 60)

    @Test fun roughPositionShowsBeforeGps() {
        show(running.copy(here = GeoPoint(37.5283, 126.9326)))
        compose.onNodeWithTag("run-rough-location", useUnmergedTree = true).assertExists()
        compose.onNodeWithText(compose.activity.getString(R.string.run_gps_search_rough)).assertIsDisplayed()
        capture("run-rough-location.png")
    }

    @Test fun phoneLocationOffIsExplained() {
        show(running.copy(locationOn = false))
        compose.onNodeWithTag("run-location-off").assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.run_location_off_action)).assertIsDisplayed()
        capture("run-location-off.png")
    }

    @Test fun approximateOnlyIsExplained() {
        show(running.copy(precise = false))
        compose.onNodeWithTag("run-location-approx").assertIsDisplayed()
        capture("run-location-approx.png")
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val directory = File(compose.activity.getExternalFilesDir(null), "form-checks").apply { mkdirs() }
        captureDisplay(File(directory, name))
    }
}
