package com.stepup.android

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.CoachingApi
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.repo.CoachingCacheStore
import com.stepup.android.data.repo.CoachingRepository
import com.stepup.android.domain.RecordingOwner
import com.stepup.android.domain.RunTrack
import com.stepup.android.domain.TrackPoint
import com.stepup.android.domain.coaching.LapCodec
import com.stepup.android.domain.coaching.SavedLap
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.coaching.CoachTab
import com.stepup.android.ui.screens.coaching.CoachingActions
import com.stepup.android.ui.screens.coaching.CoachingConsentScreen
import com.stepup.android.ui.screens.coaching.CoachingGoalsScreen
import com.stepup.android.ui.screens.coaching.CoachingHomeScreen
import com.stepup.android.ui.screens.coaching.CoachingHubViewModel
import com.stepup.android.ui.screens.coaching.CoachingPlanScreen
import com.stepup.android.ui.screens.coaching.CoachingSettingsScreen
import com.stepup.android.ui.screens.coaching.CoachingSetupViewModel
import com.stepup.android.ui.screens.coaching.CoachingWorkoutScreen
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

/**
 * AI 코칭 16장(2026-10-16) — 화면을 실제로 그려 본다(런타임 오류 없이 그려지는지 · 주요 칸이 있는지).
 *
 * 서버는 0060 이 내는 JSON 모양 그대로 흉내 낸다(가짜 HttpPoster — 네트워크에 닿지 않는다). 기록은 이 휴대폰 DB 에 넣은
 * 시험 계정(account:qa-coach)의 러닝 다섯이고 끝나면 지운다 — 지금 로그인한 계정 · 실제 서버와 상관없다.
 * AI 글은 모의 공급자의 시험 글이다(화면에 "모의 응답"이 붙는다). 장면은 coaching/ 에 남는다.
 */
class CoachingDesignTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val directory get() = File(compose.activity.getExternalFilesDir(null), "coaching").apply { mkdirs() }
    private val user = "qa-coach"
    private val owner = RecordingOwner.account(user)

    @Test fun screens() {
        seedRuns()
        try {
            edgeToEdge()
            var scene by mutableStateOf<(@Composable () -> Unit)?>(null)
            compose.setContent { Frame { scene?.invoke() } }
            fun show(name: String, tag: String, settle: Long = 1_500, content: @Composable () -> Unit) {
                compose.runOnIdle { scene = content }
                awaitTag(tag)
                shot(name, settle)
            }
            val actions = CoachingActions(
                onBack = {}, onSettings = {}, onConsent = {}, onGoals = {}, onWorkout = {}, onPlan = {}, onGoRun = {}, onOpenRecord = {},
            )
            lateinit var hub: CoachingHubViewModel
            lateinit var setup: CoachingSetupViewModel
            lateinit var fresh: CoachingSetupViewModel
            lateinit var off: CoachingSetupViewModel
            lateinit var offHub: CoachingHubViewModel
            compose.runOnIdle {
                hub = CoachingHubViewModel(repo(enabled = true, consented = true), null)
                setup = CoachingSetupViewModel(repo(enabled = true, consented = true))
                fresh = CoachingSetupViewModel(repo(enabled = true, consented = false))
                off = CoachingSetupViewModel(repo(enabled = false, consented = false))
                offHub = CoachingHubViewModel(repo(enabled = false, consented = false), null)
            }

            // 01 오늘 — 이번 주 띠 · 오늘의 훈련(모의 추천) · 최근 러닝
            show("C01-today", "coach-today-card", settle = 2_500) { CoachingHomeScreen(hub, CoachTab.TODAY, actions) }
            // 03 분석 — 가장 최근 기록(서버 확인 · 랩 다섯) · 1km 구간 그래프 · AI 칸
            tap("coach-tab-1")
            awaitTag("coach-pace-chart")
            awaitTag("coach-ai-ready")
            shot("C03-analysis")
            // 16 목표 페이스 적용 확인
            tap("coach-next")
            awaitTag("coach-pace-sheet")
            shot("C16-pace-sheet")
            tap("coach-pace-cancel")
            awaitGone("coach-pace-sheet")
            // 04 변화 — 처음은 끝난 지난주(AI 정리), 다음 주(진행 중)는 끝나면 정리한다고만
            tap("coach-tab-2")
            awaitTag("coach-week-ready")
            shot("C04-change")
            tap("coach-week-next")
            awaitTag("coach-week-progress-note")
            shot("C04-change-this-week")

            // 14 설정 · 15 해제 확인 · 06 목표 · 13 다음 주 계획 · 02 훈련 상세
            show("C14-settings", "coach-settings-off") { CoachingSettingsScreen(setup, {}, {}, {}) }
            tap("coach-settings-off")
            awaitTag("coach-off-sheet")
            shot("C15-withdraw")
            show("C06-goals", "coach-goals-save") { CoachingGoalsScreen(setup, {}, {}) }
            show("C13-plan", "coach-plan-head") { CoachingPlanScreen(setup, {}, {}) }
            show("C02-workout", "coach-workout-start") { CoachingWorkoutScreen("walk_run_25", setup, {}, {}) }

            // 05 첫 사용 동의 — 서버가 켜져 있고 공급자가 정해졌을 때만 고를 수 있다
            compose.runOnIdle { scene = { CoachingConsentScreen(fresh, {}, {}) } }
            compose.waitUntil(15_000) { runCatching { compose.onNodeWithTag("coach-consent-check").assertIsEnabled() }.isSuccess }
            shot("C05-consent")
            // 코칭이 꺼진 서버(운영 기본값) — 동의를 받지 않고 "준비 중", 분석은 기록 숫자 · 구간만
            show("C05-unavailable", "coach-consent-unavailable") { CoachingConsentScreen(off, {}, {}) }
            show("C03-off", "coach-pace-chart", settle = 2_500) { CoachingHomeScreen(offHub, CoachTab.ANALYSIS, actions) }
            // 꺼진 동안에는 동의할 수 없다 — "동의 필요" 칸을 보이지 않는다(AI 칸 없이 기록 숫자 · 구간만)
            compose.onAllNodesWithTag("coach-ai-consent", useUnmergedTree = true).assertCountEquals(0)
            tap("coach-tab-0")
            awaitTag("coach-today-card")
            compose.onAllNodesWithTag("coach-rec-request", useUnmergedTree = true).assertCountEquals(0)
            shot("C01-off")
        } finally {
            dropRuns()
        }
    }

    // ── 가짜 서버 ────────────────────────────────────────────────

    private class Store(var value: AuthSession?) : AuthSessionStore {
        override suspend fun load() = value
        override suspend fun save(session: AuthSession) {
            value = session
        }
        override suspend fun clear() {
            value = null
        }
    }

    /** 0060 의 RPC 이름마다 서버가 내는 답 — 켜짐 · 동의에 따라 AI 상태가 다르다(동의 전 no_consent · 꺼짐 disabled) */
    private class FakeServer(private val enabled: Boolean, private val consented: Boolean) : HttpPoster {
        override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse =
            HttpResponse(200, reply(url.substringAfterLast('/'), body))

        override suspend fun get(url: String, headers: Map<String, String>): HttpResponse = post(url, "", headers)

        private fun reply(name: String, body: String): String {
            val locale = Regex("\"p_locale\":\"([A-Za-z-]+)\"").find(body)?.groupValues?.get(1) ?: "ko"
            // 서버(0060)는 동의를 먼저 본다 — 꺼져 있어도 동의 전이면 no_consent
            val ai = when {
                !consented -> status("no_consent")
                !enabled -> status("disabled")
                else -> null
            }
            return when (name) {
                "coaching_state" -> state()
                "coaching_splits_put" -> "\"stored\""
                "coaching_run", "coaching_job_retry" -> ai ?: runReady(locale)
                "coaching_recommend" -> ai ?: recReady(locale)
                "coaching_week" -> ai ?: run {
                    val week = Regex("\"p_week_start\":\"([0-9-]+)\"").find(body)?.groupValues?.get(1).orEmpty()
                    val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()
                    if (week >= monday) status("skipped", "\"week_in_progress\"") else weekReady(locale)
                }
                "coaching_consent_withdraw" -> """{"deleted_results": 0}"""
                "coaching_prefs_save", "coaching_apply_pace" -> """{"ok": true, "conflict": false, "prefs": $PREFS}"""
                else -> "null"
            }
        }

        private fun state(): String {
            val consent = if (consented) {
                """{"version": "draft-2026-10-16", "granted_at": "2026-10-16T08:07:09+00:00", "current": true}"""
            } else {
                "null"
            }
            val provider = if (enabled) "mock" else "none"
            return """{"enabled": $enabled, "provider": "$provider", "consent_version": "draft-2026-10-16", "consent": $consent,
                "prefs": $PREFS, "plans": [], "daily_limit": null, "reset_at": null}"""
        }

        private fun status(state: String, reason: String = "null") =
            """{"state": "$state", "job_id": null, "reason": $reason, "retryable": false, "reset_at": null, "result": null, "stale": false}"""

        private fun ready(result: String) =
            """{"state": "ready", "job_id": 7, "reason": null, "retryable": false, "reset_at": null, "stale": false, "result": $result}"""

        // 시험 기록(가장 최근 · 5.2km · 1km 랩 다섯 418 · 418 · 416 · 424 · 436초)에서 서버가 낼 근거 값 — 평균 423초 · 마지막이 첫 구간보다 18초
        private fun runReady(locale: String) = ready(
            """{"id": 3, "kind": "run", "generated_at": "2026-10-16T08:07:09+00:00", "locale": "$locale",
                "facts": [{"id": "run.avg_pace", "values": {"pace_sec": 423}}, {"id": "run.split_last_vs_first", "values": {"last_km": 5, "delta_sec": 18}}],
                "fact_id": "run.split_last_vs_first", "explanation": "(시험 응답) 후반으로 갈수록 조금 느려졌어요.",
                "candidates": [{"id": "pace.start_easier", "values": {"pace_sec": 430}}, {"id": "pace.keep", "values": {"pace_sec": 423}}],
                "candidate_id": "pace.start_easier", "action_reason": "(시험 응답) 처음을 조금 천천히 시작하면 끝까지 편해요.",
                "limitation": null, "provider": "mock"}""",
        )

        private fun recReady(locale: String) = ready(
            """{"id": 4, "kind": "recommendation", "generated_at": "2026-10-16T08:07:09+00:00", "locale": "$locale",
                "facts": [{"id": "week.runs", "values": {"runs": 3, "goal": 3}}], "fact_id": "week.runs", "explanation": null,
                "candidates": [{"id": "template.walk_run_25", "values": {}}, {"id": "template.easy_25", "values": {}}],
                "candidate_id": "template.walk_run_25", "action_reason": "(시험 응답) 이번 주 목표를 채웠으니 가볍게 이어가요.",
                "limitation": null, "provider": "mock"}""",
        )

        // 지난주 — 시험 기록 둘(6.0km · 3.5km)
        private fun weekReady(locale: String) = ready(
            """{"id": 5, "kind": "weekly", "generated_at": "2026-10-16T08:07:09+00:00", "locale": "$locale",
                "facts": [{"id": "week.runs", "values": {"runs": 2, "goal": 3}}, {"id": "week.distance", "values": {"meters": 9500}}],
                "fact_id": "week.runs", "explanation": "(시험 응답) 목표보다 한 번 적게 달렸어요.",
                "candidates": [{"id": "plan.next_week", "values": {}}], "candidate_id": "plan.next_week",
                "action_reason": "(시험 응답) 다음 주는 쉬는 날을 사이에 두고 세 번 달려 봐요.", "limitation": null, "provider": "mock"}""",
        )

        companion object {
            const val PREFS = """{"goal": "steady", "runs_per_week": 3, "minutes": 25, "auto_analysis": false,
                "weekly_report": false, "target_pace_sec": null, "revision": 4}"""
        }
    }

    /** 시험 계정의 코칭 저장소 — 가짜 서버 · 이 휴대폰 DB · 시험용 사본 폴더 */
    private fun repo(enabled: Boolean, consented: Boolean): CoachingRepository {
        val http = FakeServer(enabled, consented)
        val session = AuthSession(
            accessToken = "qa", refreshToken = "qa", expiresIn = 3600, expiresAt = 9_999_999_999L,
            user = AuthUser(id = user, isAnonymous = false),
        )
        val holder = SessionHolder(SupabaseAuth("https://qa.invalid", "qa", http), Store(session), now = { 1000L })
        return CoachingRepository(
            dao = ServiceLocator.database.walkSessionDao(),
            api = CoachingApi(StepUpServer("https://qa.invalid", "qa", holder, http)),
            cache = CoachingCacheStore(File(compose.activity.cacheDir, "coaching-qa-${System.nanoTime()}").apply { mkdirs() }),
            owner = flowOf(owner),
            online = { true },
        )
    }

    // ── 시험 기록 ────────────────────────────────────────────────

    /** 시험 계정의 러닝 다섯 — 오늘 · 2일 전 · 4일 전 · 8일 전 · 11일 전. 가장 최근 것만 1km 랩 · 경로가 있다 */
    private fun seedRuns() = runBlocking(Dispatchers.IO) {
        ServiceLocator.userPrefs.setReducedMotion(true)
        dropRuns()
        val dao = ServiceLocator.database.walkSessionDao()
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        listOf(0L to 5_200.0, 2L to 3_100.0, 4L to 4_000.0, 8L to 6_000.0, 11L to 3_500.0).forEachIndexed { index, (ago, meters) ->
            val started = now - ago * day - 3_600_000L
            val latest = index == 0
            val seconds = if (latest) 2_200L else (meters / 1000.0 * 400).toLong()
            dao.insert(
                WalkSessionEntity(
                    startedAt = started, endedAt = started + seconds * 1000, steps = (meters / 0.762).toInt(), durationSec = seconds,
                    distanceMeters = meters, calories = 0.0, pointsEarned = 0.0, recordingOwner = owner, uploadState = "SIGNED",
                    track = if (latest) RunTrack.encode(route(started, seconds)) else "",
                    laps = if (latest) {
                        LapCodec.encode(listOf(418L, 836L, 1_252L, 1_676L, 2_112L).mapIndexed { i, at -> SavedLap(i + 1, (i + 1).toDouble(), at) })
                    } else {
                        ""
                    },
                ),
            )
        }
    }

    /** 한강 따라 남동쪽으로 5.2km 쯤 — 시각은 고르게 */
    private fun route(started: Long, seconds: Long): List<TrackPoint> = (0 until 40).map { i ->
        TrackPoint(37.5283 - 0.00055 * i, 126.9326 + 0.0011 * i, started + seconds * 1000 * i / 39)
    }

    private fun dropRuns() = runBlocking(Dispatchers.IO) {
        // Room 거래 안에서 지워야 그 줄을 지켜보는 화면도 지운 것을 안다
        ServiceLocator.database.runInTransaction {
            ServiceLocator.database.openHelper.writableDatabase.execSQL(
                "DELETE FROM walk_sessions WHERE recordingOwner = ?", arrayOf<Any?>(owner),
            )
        }
    }

    // ── 화면 틀 · 찍기 ───────────────────────────────────────────

    /** 390 × 844dp · 글자 100% · 어두운 테마 — 에뮬레이터 창 안에 그 크기의 화면을 만든다 */
    @Composable private fun Frame(content: @Composable () -> Unit) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.TopCenter) {
            val px = minOf(constraints.maxHeight / 844f, constraints.maxWidth / 390f)
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides Density(px, 1f)) {
                StepUpTheme(ThemeMode.DARK) {
                    ExperienceProvider {
                        Box(Modifier.requiredSize(390.dp, 844.dp).testTag("coach-frame")) { content() }
                    }
                }
            }
        }
    }

    private fun exists(tag: String): Boolean = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun awaitTag(tag: String, timeout: Long = 15_000) = compose.waitUntil(timeout) { exists(tag) }

    private fun awaitGone(tag: String, timeout: Long = 15_000) = compose.waitUntil(timeout) { !exists(tag) }

    private fun tap(tag: String) {
        awaitTag(tag)
        compose.onNodeWithTag(tag).performClick()
        compose.waitForIdle()
    }

    private fun edgeToEdge() {
        compose.activityRule.scenario.onActivity {
            it.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            )
        }
    }

    private fun shot(name: String, settle: Long = 1_500) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(settle)
        captureDisplay(File(directory, "$name.png"))
    }
}
