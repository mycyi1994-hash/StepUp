package com.stepup.android

import com.stepup.android.data.remote.AccessApi
import com.stepup.android.data.remote.AccessIssueRow
import com.stepup.android.data.remote.AccessStatusRow
import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.remote.serverJson
import com.stepup.android.ui.screens.access.betaCodeNextAt
import com.stepup.android.ui.screens.access.issueErrorText
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 베타 초대 코드 한도(서버 0113) — 하루 1개 · 지난 7일 3개, 새 함수의 거절, 예전 서버로 돌아가기 */
class AccessLimitsTest {
    private class Store(var value: AuthSession?) : AuthSessionStore {
        override suspend fun load() = value
        override suspend fun save(session: AuthSession) { value = session }
        override suspend fun clear() { value = null }
    }

    private class Http(private val reply: (String) -> HttpResponse) : HttpPoster {
        val calls = mutableListOf<String>()
        override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
            calls += url.substringAfterLast('/')
            return reply(url.substringAfterLast('/'))
        }
        override suspend fun get(url: String, headers: Map<String, String>) = post(url, "", headers)
    }

    private fun api(http: Http): AccessApi {
        val session = AuthSession(
            accessToken = "token", refreshToken = "refresh", expiresIn = 3600,
            expiresAt = 9_999_999_999L, user = AuthUser(id = "A", isAnonymous = false),
        )
        val holder = SessionHolder(SupabaseAuth("https://test.supabase.co", "test", http), Store(session), now = { 1000L })
        return AccessApi(StepUpServer("https://test.supabase.co", "test", holder, http))
    }

    @Test fun readsBothLimitsFromStatus() {
        val s = serverJson.decodeFromString(
            AccessStatusRow.serializer(),
            """{"mode":"CLOSED_BETA","granted":true,"source":"CODE","revoked":false,"can_issue":true,
               "daily_limit":1,"issued_today":1,"weekly_limit":3,"issued_week":1,"left_today":0,"left_week":2,"left":0,
               "next_at":"2026-11-18T15:00:00+00:00","limited_by":"DAY"}""",
        )
        assertEquals(0, s.codesLeftToday)
        assertEquals(2, s.codesLeftWeek)
        assertEquals(0, s.codesLeft)
        assertEquals(Instant.parse("2026-11-18T15:00:00Z"), betaCodeNextAt(s))
    }

    @Test fun weekLimitBlocksEvenWithTodayFree() {
        val s = AccessStatusRow(
            mode = "CLOSED_BETA", canIssue = true, dailyLimit = 1, issuedToday = 0, weeklyLimit = 3, issuedWeek = 3,
            leftToday = 1, leftWeek = 0, nextAt = "2026-11-21T02:04:55.315614+00:00", limitedBy = "WEEK",
        )
        assertEquals(1, s.codesLeftToday)
        assertEquals(0, s.codesLeft)
        assertEquals(Instant.parse("2026-11-21T02:04:55.315614Z"), betaCodeNextAt(s))
        // 남은 자리가 있거나 만들 수 없는 사람이면 다음 시각을 보이지 않는다
        assertNull(betaCodeNextAt(s.copy(leftWeek = 1)))
        assertNull(betaCodeNextAt(s.copy(canIssue = false)))
    }

    @Test fun oldServerHasOnlyTheDailyLimit() {
        val s = AccessStatusRow(mode = "CLOSED_BETA", canIssue = true, dailyLimit = 3, issuedToday = 1)
        assertNull(s.codesLeftWeek)
        assertEquals(2, s.codesLeft)
        assertNull(betaCodeNextAt(s.copy(issuedToday = 3)))
        // 7일 칸만 오고 남은 수가 없으면 한도에서 만든 수를 뺀다
        assertEquals(1, s.copy(weeklyLimit = 3, issuedWeek = 2).codesLeftWeek)
    }

    @Test fun issuesWithTheNewFunction() = runBlocking {
        val http = Http { name ->
            when (name) {
                "access_code_issue_v2" -> HttpResponse(200, """{"code":"ABCD-EFGH","expires_at":"2026-12-02T00:00:00+00:00",
                    "left_today":0,"left_week":2}""")
                else -> HttpResponse(500, "{}")
            }
        }
        assertEquals("ABCD-EFGH", (api(http).issue() as ServerResult.Ok).value.code)
        assertEquals(listOf("access_code_issue_v2"), http.calls)
    }

    @Test fun limitRefusalsBecomeReasons() = runBlocking {
        val weekly = Http {
            HttpResponse(400, """{"code":"23514","details":"2026-11-21T02:04:55+00:00","hint":"weekly_limit",
                "message":"초대 코드는 7일 동안 3개까지 만들 수 있어요. 11월 21일 11:04부터 다시 만들 수 있어요"}""")
        }
        val w = (api(weekly).issue() as ServerResult.Ok).value
        assertEquals(AccessIssueRow(error = "weekly_limit", nextAt = "2026-11-21T02:04:55+00:00"), w)

        val daily = Http { HttpResponse(400, """{"code":"23514","details":"","hint":"daily_limit","message":"하루 1개"}""") }
        assertEquals(AccessIssueRow(error = "daily_limit"), (api(daily).issue() as ServerResult.Ok).value)

        // 42501 은 PostgREST 가 403 으로 보낸다 — 접속 코드 문(access_required)과 헷갈리지 않는다
        val notAllowed = Http { HttpResponse(403, """{"code":"42501","details":null,"hint":"not_allowed","message":"지금은 초대 코드를 만들 수 없어요"}""") }
        assertEquals(AccessIssueRow(error = "not_allowed"), (api(notAllowed).issue() as ServerResult.Ok).value)

        // 모르는 거절은 그대로 거절
        val other = Http { HttpResponse(400, """{"code":"22023","hint":null,"message":"모르는 거절"}""") }
        assertTrue(api(other).issue() is ServerResult.Rejected)
    }

    @Test fun oldServerFallsBackToTheOldFunction() = runBlocking {
        val http = Http { name ->
            when (name) {
                "access_code_issue_v2" -> HttpResponse(
                    404, """{"code":"PGRST202","message":"Could not find the function public.access_code_issue_v2 without parameters"}""",
                )
                "access_code_issue" -> HttpResponse(200, """{"error":"daily_limit"}""")
                else -> HttpResponse(500, "{}")
            }
        }
        assertEquals("daily_limit", (api(http).issue() as ServerResult.Ok).value.error)
        assertEquals(listOf("access_code_issue_v2", "access_code_issue"), http.calls)
    }

    @Test fun everyRefusalHasItsOwnText() {
        val texts = listOf("daily_limit", "weekly_limit", "not_allowed").map(::issueErrorText)
        assertEquals(texts.size, texts.toSet().size)
        assertEquals(R.string.beta_codes_not_allowed, issueErrorText("something_new"))
    }
}
