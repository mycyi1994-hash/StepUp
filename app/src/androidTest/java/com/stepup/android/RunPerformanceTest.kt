package com.stepup.android

import android.Manifest
import android.os.Handler
import android.os.HandlerThread
import android.view.FrameMetrics
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.RunPlan
import com.stepup.android.domain.RunPlans
import com.stepup.android.domain.RunTrack
import com.stepup.android.domain.TrackPoint
import com.stepup.android.service.WalkSessionService
import com.stepup.android.service.WalkSessionState
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Routes
import com.stepup.android.ui.Screen
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import java.util.Collections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

/**
 * 러닝 리메이크 화면의 프레임 시간 — 실제 앱(MainScaffold)에서 자주 쓰는 동작(화면 넘기기 · 목록 넘기기 · 러닝 중 갱신)을 하는 동안
 * 창이 그린 프레임마다 걸린 시간(FrameMetrics)을 모아 `run-journey/perf.txt` 에 적는다. 통과/실패를 가르지 않는다 — 에뮬레이터는
 * 소프트웨어 GPU 라 절대값보다 화면끼리 · 고치기 전후의 차이를 본다.
 */
class RunPerformanceTest {
    @get:Rule(order = 0) val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACTIVITY_RECOGNITION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS,
    )
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()

    private val report = StringBuilder()

    @Test fun performance() {
        runBlocking {
            ServiceLocator.userPrefs.setGuideSeen()
            ServiceLocator.userPrefs.setReducedMotion(false)
        }
        seedRecords()
        try {
            // 1. 러닝 홈 ↔ 러닝 시작(U01) 오가기 — 화면 넘김 애니메이션
            show { MainScaffold() }
            await("home-start-run")
            measure("home-to-menu") {
                repeat(3) {
                    compose.onNodeWithTag("home-start-run").performClick()
                    await("run-menu-free")
                    compose.waitForIdle()
                    back()
                    await("home-start-run")
                    compose.waitForIdle()
                }
            }

            // 2. 달리는 중 — 시간 · 걸음 · 위치가 1초에 여러 번 바뀐다
            RunPlans.set(RunPlan.Free)
            val now = System.currentTimeMillis()
            WalkSessionService.showStateForTest(running(now, 750))
            show { MainScaffold(initialRoute = Routes.RUN) }
            await("run-live")
            compose.waitForIdle()
            measure("run-live-updates") {
                for (i in 1..24) {
                    WalkSessionService.showStateForTest(running(now, 750L + i / 3, steps = 2_100 + i * 3))
                    compose.waitForIdle()
                    Thread.sleep(250)
                }
            }
            WalkSessionService.showStateForTest(WalkSessionState())
            RunPlans.clear()

            // 3. 내 러닝 기록 목록 넘기기(줄마다 실제 지도)
            show { MainScaffold(initialRoute = Routes.RECORDS) }
            await("records-list")
            compose.waitForIdle()
            measure("records-scroll") { swipeAround("records-list") }

            // 4. 코스 허브(내 코스 · 줄마다 지도)
            runBlocking { ServiceLocator.courseRepository.ensureSeeded() }
            show { MainScaffold(initialRoute = Routes.COURSES) }
            await("courses-list")
            compose.waitForIdle()
            measure("courses-scroll") { swipeAround("courses-list") }

            // 5. 신발 탭(내 신발 · 무대 조명)
            show { MainScaffold(initialTab = Screen.Customize) }
            await("shoes-backdrop")
            compose.waitForIdle()
            measure("shoes-tab-scroll") { swipeAround(null) }
        } finally {
            WalkSessionService.showStateForTest(WalkSessionState())
            RunPlans.clear()
            dropRecords()
            val dir = File(compose.activity.getExternalFilesDir(null), "run-journey").apply { mkdirs() }
            File(dir, "perf.txt").writeText(report.toString())
            println("RunPerformance\n$report")
        }
    }

    // ── 재기 ──────────────────────────────────────────────────────

    private fun measure(name: String, block: () -> Unit) {
        val total = Collections.synchronizedList(mutableListOf<Long>())
        val ui = Collections.synchronizedList(mutableListOf<Long>())
        val thread = HandlerThread("frames-$name").apply { start() }
        val listener = Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
            if (metrics.getMetric(FrameMetrics.FIRST_DRAW_FRAME) == 1L) return@OnFrameMetricsAvailableListener
            total += metrics.getMetric(FrameMetrics.TOTAL_DURATION)
            ui += metrics.getMetric(FrameMetrics.ANIMATION_DURATION) + metrics.getMetric(FrameMetrics.INPUT_HANDLING_DURATION) +
                metrics.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION) + metrics.getMetric(FrameMetrics.DRAW_DURATION)
        }
        val window = compose.activity.window
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            window.addOnFrameMetricsAvailableListener(listener, Handler(thread.looper))
        }
        try {
            block()
            compose.waitForIdle()
            Thread.sleep(300)
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { window.removeOnFrameMetricsAvailableListener(listener) }
            thread.quitSafely()
        }
        fun ms(list: List<Long>, p: Double): String {
            if (list.isEmpty()) return "-"
            val sorted = list.sorted()
            val v = sorted[((sorted.size - 1) * p).toInt()]
            return "%.1f".format(v / 1_000_000.0)
        }
        val frames = synchronized(total) { total.toList() }
        val uiFrames = synchronized(ui) { ui.toList() }
        val over16 = frames.count { it > 16_700_000 } * 100 / frames.size.coerceAtLeast(1)
        val over32 = frames.count { it > 33_400_000 } * 100 / frames.size.coerceAtLeast(1)
        report.appendLine(
            "$name frames=${frames.size} total_p50=${ms(frames, 0.5)} p90=${ms(frames, 0.9)} p99=${ms(frames, 0.99)} " +
                "max=${ms(frames, 1.0)} over16ms=$over16% over33ms=$over32% ui_p50=${ms(uiFrames, 0.5)} ui_p90=${ms(uiFrames, 0.9)}",
        )
    }

    private fun swipeAround(tag: String?) {
        val target = if (tag != null) compose.onNodeWithTag(tag) else compose.onAllNodesWithTag("shoes-backdrop")[0]
        repeat(3) {
            target.performTouchInput { swipeUp(durationMillis = 300) }
            compose.waitForIdle()
        }
        repeat(3) {
            target.performTouchInput { swipeDown(durationMillis = 300) }
            compose.waitForIdle()
        }
    }

    // ── 준비 ──────────────────────────────────────────────────────

    private var contentSet = false
    private var scene by mutableStateOf<(@Composable () -> Unit)?>(null)

    /** 앱 화면 하나를 띄운다 — 처음 한 번만 setContent, 그다음은 장면만 바꾼다 */
    private fun show(content: @Composable () -> Unit) {
        if (!contentSet) {
            contentSet = true
            scene = content
            compose.setContent {
                StepUpTheme(ThemeMode.DARK) {
                    ExperienceProvider { key(scene) { scene?.invoke() } }
                }
            }
        } else {
            compose.runOnIdle { scene = content }
        }
    }

    private fun await(tag: String) = compose.waitUntil(15_000) {
        compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun back() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
    }

    private fun running(now: Long, elapsed: Long, steps: Int = 2_100) = WalkSessionState(
        isActive = true, steps = steps, elapsedSec = elapsed, startedAt = now - 750_000,
        gpsFix = true, gpsKm = 1.56, validSegments = 120, track = track(now - 750_000, now - 2_000),
    )

    /** 한강 길을 따라 — 실제 좌표 */
    private fun track(from: Long, to: Long): List<TrackPoint> {
        val path = listOf(
            GeoPoint(37.5283, 126.9326), GeoPoint(37.5269, 126.9349), GeoPoint(37.5251, 126.9371),
            GeoPoint(37.5236, 126.9398), GeoPoint(37.5222, 126.9431), GeoPoint(37.5210, 126.9462),
            GeoPoint(37.5196, 126.9490), GeoPoint(37.5181, 126.9518),
        )
        return path.mapIndexed { i, p -> TrackPoint(p.lat, p.lng, from + (to - from) * i / (path.size - 1)) }
    }

    /** 이번 달 기록 24개(경로 있음) — 끝나면 지운다 */
    private fun seedRecords() = runBlocking(Dispatchers.IO) {
        val dao = ServiceLocator.database.walkSessionDao()
        val now = System.currentTimeMillis()
        for (i in 1..24) {
            val start = now - i * 3_600_000L
            dao.insert(
                WalkSessionEntity(
                    startedAt = start, endedAt = start + 1_500_000, steps = 3_000, durationSec = 1_500,
                    distanceMeters = 3_000.0 + i * 50, calories = 0.0, pointsEarned = 0.0,
                    track = RunTrack.encode(track(start, start + 1_500_000)), faction = MARK,
                    uploadState = "REJECTED", verdict = "VOID",
                ),
            )
        }
    }

    private fun dropRecords() = runBlocking(Dispatchers.IO) {
        ServiceLocator.database.runInTransaction {
            ServiceLocator.database.openHelper.writableDatabase.execSQL("DELETE FROM walk_sessions WHERE faction = '$MARK'")
        }
    }

    private companion object {
        const val MARK = "perf-test"
    }
}
