package com.stepup.android

import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.CoachingApi
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.remote.parseInstantMillis
import com.stepup.android.domain.coaching.CoachIds
import com.stepup.android.domain.coaching.CoachKind
import com.stepup.android.domain.coaching.CoachServerState
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** AI 코칭 서버 호출(0059) — 보내는 것(시작 시각 · 랩만), 계정 대조, 답 읽기(서버가 실제로 내는 JSON 모양) */
class CoachingApiTest {
    private class Store(var value: AuthSession?) : AuthSessionStore {
        override suspend fun load() = value
        override suspend fun save(session: AuthSession) { value = session }
        override suspend fun clear() { value = null }
    }

    private class Http(private val reply: (String, String) -> HttpResponse) : HttpPoster {
        val calls = mutableListOf<Pair<String, String>>()
        override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
            calls += url.substringAfterLast('/') to body
            return reply(url.substringAfterLast('/'), body)
        }
        override suspend fun get(url: String, headers: Map<String, String>) = post(url, "", headers)
    }

    private fun api(http: Http, user: String = "A"): CoachingApi {
        val session = AuthSession(
            accessToken = "token", refreshToken = "refresh", expiresIn = 3600,
            expiresAt = 9_999_999_999L, user = AuthUser(id = user, isAnonymous = false),
        )
        val holder = SessionHolder(SupabaseAuth("https://test.supabase.co", "test", http), Store(session), now = { 1000L })
        return CoachingApi(StepUpServer("https://test.supabase.co", "test", holder, http))
    }

    // 서버(coaching.status · coaching.result_json)가 내는 모양 그대로 — 로컬 Postgres 16 의 출력에서 옮겼다
    private val readyRun = """{"state": "ready", "job_id": 7, "reason": null, "retryable": false, "reset_at": null, "stale": false,
        "result": {"id": 3, "kind": "run", "generated_at": "2026-10-16T08:07:09.159009+00:00", "locale": "ko",
          "facts": [{"id": "run.avg_pace", "values": {"pace_sec": 450}}, {"id": "run.split_last_vs_first", "values": {"last_km": 3, "delta_sec": 40}}],
          "fact_id": "run.split_last_vs_first", "explanation": "후반으로 갈수록 페이스가 느려졌어요.",
          "candidates": [{"id": "pace.start_easier", "values": {"pace_sec": 450}}, {"id": "pace.keep", "values": {"pace_sec": 450}}],
          "candidate_id": "pace.start_easier", "action_reason": "처음을 조금 천천히 시작하면 끝까지 편해요.", "limitation": null, "provider": "mock"}}"""

    @Test fun `결과를 읽는다 — 근거 · 후보 값은 서버의 정수 그대로`() = runBlocking {
        val http = Http { _, _ -> HttpResponse(200, readyRun) }
        val status = (api(http).run("A", 1_760_400_000_000, "ko", request = false) as ServerResult.Ok).value
        assertEquals(CoachServerState.READY, status.state)
        assertEquals(7L, status.jobId)
        val result = status.result!!
        assertEquals(CoachKind.RUN, result.kind)
        assertEquals(40L, result.fact!!.values["delta_sec"])
        assertEquals(450L, result.candidate!!.values["pace_sec"])
        assertEquals(java.time.Instant.parse("2026-10-16T08:07:09.159Z").toEpochMilli(), result.generatedAt)
        assertTrue(result.mock)
    }

    @Test fun `보내는 것은 시작 시각 · 언어 · 요청 여부뿐 — 로컬 번호 · 사용자 번호 · 숫자를 보내지 않는다`() = runBlocking {
        val http = Http { _, _ -> HttpResponse(200, """{"state": "queued", "job_id": 9, "reason": null, "retryable": false, "reset_at": null, "result": null, "stale": false}""") }
        val status = (api(http).run("A", 1_760_400_000_000, "ko", request = true) as ServerResult.Ok).value
        assertEquals(CoachServerState.QUEUED, status.state)
        val (name, body) = http.calls.single()
        assertEquals("coaching_run", name)
        assertEquals("""{"p_started_at":"2025-10-14T00:00:00Z","p_locale":"ko","p_request":true}""", body)
    }

    @Test fun `랩은 JSON 정수 배열로 보낸다`() = runBlocking {
        val http = Http { _, _ -> HttpResponse(200, "\"stored\"") }
        assertEquals("stored", (api(http).putSplits("A", 1_760_400_000_000, listOf(432, 878, 1350)) as ServerResult.Ok).value)
        assertEquals("""{"p_started_at":"2025-10-14T00:00:00Z","p_laps":[432,878,1350]}""", http.calls.single().second)
    }

    @Test fun `계정이 바뀌었으면 보내지 않는다 — 이전 계정의 요청이 새 계정으로 가지 않게`() = runBlocking {
        val http = Http { _, _ -> HttpResponse(200, readyRun) }
        val result = api(http, user = "B").run("A", 1_760_400_000_000, "ko", request = true)
        assertTrue(result is ServerResult.SignInRequired)
        assertTrue(http.calls.isEmpty())
    }

    @Test fun `모르는 상태 · 깨진 답은 성공이 아니다`() = runBlocking {
        val odd = Http { _, _ -> HttpResponse(200, """{"state": "done"}""") }
        assertTrue(api(odd).run("A", 1, "ko", false) is ServerResult.Retry)
        val broken = Http { _, _ -> HttpResponse(200, "<html>") }
        assertTrue(api(broken).run("A", 1, "ko", false) is ServerResult.Retry)
        // 결과 칸이 깨졌으면(필수 값 없음) 결과 없이 상태만 — 화면은 성공으로 보이지 않는다
        val noResult = Http { _, _ -> HttpResponse(200, """{"state": "ready", "job_id": 1, "result": {"id": 1}}""") }
        assertNull((api(noResult).run("A", 1, "ko", false) as ServerResult.Ok).value.result)
    }

    @Test fun `상태 — 동의 · 선호 · 적용한 계획 · 켜짐`() = runBlocking {
        val http = Http { _, _ ->
            HttpResponse(200, """{"enabled": false, "provider": "none", "consent_version": "draft-2026-10-16",
                "consent": {"version": "draft-2026-10-16", "granted_at": "2026-10-16T08:07:09+00:00", "current": true},
                "prefs": {"goal": "steady", "runs_per_week": 3, "minutes": 25, "auto_analysis": false, "weekly_report": false,
                          "target_pace_sec": 450, "revision": 4},
                "plans": [{"week_start": "2026-10-19", "days": [{"date": "2026-10-19", "template": "easy_25"}], "prefs_revision": 4,
                           "applied_at": "2026-10-16T08:07:09+00:00"}],
                "daily_limit": null, "reset_at": "2026-10-16T15:00:00+00:00"}""")
        }
        val state = (api(http).state("A", "Asia/Seoul") as ServerResult.Ok).value
        assertFalse(state.enabled)
        assertTrue(state.consented)
        assertEquals(450, state.prefs!!.targetPaceSec)
        assertEquals(4L, state.prefs!!.revision)
        assertEquals(LocalDate.of(2026, 10, 19) to "easy_25", state.plans.single().days.single())
    }

    @Test fun `선호 저장 — 판이 다르면 저장하지 않았다고 읽는다`() = runBlocking {
        val http = Http { _, _ -> HttpResponse(200, """{"ok": false, "conflict": true, "prefs": {"goal": "improve", "revision": 9}}""") }
        val save = (api(http).savePrefs("A", "steady", 3, 25, false, false, 4) as ServerResult.Ok).value
        assertTrue(save.conflict)
        assertEquals(9L, save.prefs!!.revision)
        assertTrue(http.calls.single().second.contains("\"p_expected_revision\":4"))
        assertTrue(CoachIds.knownCandidate(CoachIds.CAND_PACE_START_EASIER))
    }

    @Test fun `서버 시각 글자`() {
        assertEquals(java.time.Instant.parse("2026-10-16T07:12:03.123Z").toEpochMilli(), parseInstantMillis("2026-10-16T07:12:03.123456+00:00"))
        assertEquals(java.time.Instant.parse("2026-10-16T07:12:03Z").toEpochMilli(), parseInstantMillis("2026-10-16 16:12:03+09"))
        assertNull(parseInstantMillis("어제"))
    }
}
