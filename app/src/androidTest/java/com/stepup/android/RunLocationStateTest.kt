package com.stepup.android

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.rule.GrantPermissionRule
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.RunPlans
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
 * 자유 러닝(2026-10-14 v2) 화면 — 까닭은 지도 위 띠 하나로 알리고, 고칠 수 있으면 띠 안 "설정" 알약으로 간다.
 */
class RunLocationStateTest {
    @get:Rule(order = 0) val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACTIVITY_RECOGNITION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()

    private var previousCourse: Long? = null

    @After fun reset() {
        WalkSessionService.showStateForTest(WalkSessionState())
        previousCourse?.let { runBlocking { ServiceLocator.userPrefs.setSelectedCourse(it) } }
    }

    private fun show(state: WalkSessionState) {
        runBlocking {
            ServiceLocator.userPrefs.setReducedMotion(true)
            ServiceLocator.userPrefs.setGuideSeen()
            // 자유 러닝 화면을 본다 — 앞 테스트가 고른 코스 · 계획이 남아 있으면 코스 · 목표 러닝 화면이 된다
            previousCourse = ServiceLocator.userPrefs.selectedCourseNow()
            ServiceLocator.userPrefs.setSelectedCourse(-1)
            ServiceLocator.userPrefs.setCourseRecording(false)
        }
        RunPlans.clear()
        com.stepup.android.ui.screens.events.ChallengeRunFocus.clear()
        WalkSessionService.showStateForTest(state)
        compose.setContent { StepUpTheme(ThemeMode.DARK) { ExperienceProvider { MainScaffold(initialRoute = Routes.RUN) } } }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("run-live-map").fetchSemanticsNodes().isNotEmpty() }
    }

    private val running = WalkSessionState(isActive = true, startedAt = System.currentTimeMillis() - 60_000, elapsedSec = 60)

    @Test fun roughPositionShowsBeforeGps() {
        show(running.copy(here = GeoPoint(37.5283, 126.9326)))
        compose.onNodeWithTag("run-rough-location", useUnmergedTree = true).assertExists()
        // 대략적인 위치 · GPS 찾는 중 — 지도 위 띠 안의 글
        compose.onNodeWithTag("run-rough-band").assertIsDisplayed()
        compose.onNode(hasText(compose.activity.getString(R.string.run_gps_search_rough)) and hasAnyAncestor(hasTestTag("run-rough-band")))
            .assertIsDisplayed()
        // 띠는 한 번에 하나 — 대략적인 자리가 있으면 "GPS 찾는 중" 띠는 없다
        compose.onNodeWithTag("run-gps-searching").assertDoesNotExist()
        capture("run-rough-location.png")
    }

    @Test fun phoneLocationOffIsExplained() {
        show(running.copy(locationOn = false))
        compose.onNodeWithTag("run-location-off").assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.run_free_location_off)).assertIsDisplayed()
        // 고치는 곳은 띠 안 "설정" 알약(휴대폰 위치 설정) — 예전 알림의 "위치 켜기" 글은 없다
        compose.onNodeWithTag("run-location-off-action").assertIsDisplayed().assertHasClickAction()
            .assertTextEquals(compose.activity.getString(R.string.run_free_settings))
        compose.onNodeWithText(compose.activity.getString(R.string.run_location_off_action)).assertDoesNotExist()
        compose.onNodeWithTag("run-gps-searching").assertDoesNotExist()
        capture("run-location-off.png")
    }

    @Test fun approximateOnlyIsExplained() {
        show(running.copy(precise = false))
        compose.onNodeWithTag("run-location-approx").assertIsDisplayed()
        compose.onNode(hasText(compose.activity.getString(R.string.run_gps_approx)) and hasAnyAncestor(hasTestTag("run-location-approx")))
            .assertIsDisplayed()
        // 정확한 위치 허용은 띠 안 "설정" 알약(앱 설정)
        compose.onNodeWithTag("run-location-approx-action").assertIsDisplayed().assertHasClickAction()
            .assertTextEquals(compose.activity.getString(R.string.run_free_settings))
        compose.onNodeWithTag("run-gps-searching").assertDoesNotExist()
        capture("run-location-approx.png")
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val directory = File(compose.activity.getExternalFilesDir(null), "form-checks").apply { mkdirs() }
        captureDisplay(File(directory, name))
    }
}
