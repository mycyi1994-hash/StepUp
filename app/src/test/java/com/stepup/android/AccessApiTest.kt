package com.stepup.android

import com.stepup.android.core.blocked
import com.stepup.android.data.remote.AccessApi
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
import com.stepup.android.ui.screens.access.errorText
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 접속 코드(서버 0079) — 서버 답을 그대로 읽고, 문이 닫혔을 때 권한 없는 사람만 막힌다 */
class AccessApiTest {
    private class Store(var value: AuthSession?) : AuthSessionStore {
        override suspend fun load() = value
        override suspend fun save(session: AuthSession) { value = session }
        override suspend fun clear() { value = null }
    }

    private class Http(private val reply: (String) -> HttpResponse) : HttpPoster {
        val calls = mutableListOf<Pair<String, String>>()
        override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
            calls += url to body
            return reply(url)
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

    @Test fun readsStatusRedeemAndCodes() = runBlocking {
        val http = Http { url ->
            when (url.substringAfterLast('/')) {
                "access_mode" -> HttpResponse(200, "\"CLOSED_BETA\"")
                "access_status" -> HttpResponse(200, """{"mode":"CLOSED_BETA","granted":true,"source":"CODE","revoked":false,
                    "can_issue":true,"daily_limit":3,"issued_today":1}""")
                "access_redeem" -> HttpResponse(200, """{"ok":false,"error":"expired"}""")
                "access_code_issue" -> HttpResponse(200, """{"code":"ABCD-EFGH","expires_at":"2026-11-11T00:00:00Z"}""")
                "my_access_codes" -> HttpResponse(200, """[{"code":"ABCD-EFGH","status":"USED","created_at":"2026-10-28T00:00:00Z",
                    "expires_at":"2026-11-11T00:00:00Z","used_at":"2026-10-28T01:00:00Z"}]""")
                else -> HttpResponse(404, "{}")
            }
        }
        val api = api(http)
        assertEquals(ServerResult.Ok("CLOSED_BETA"), api.mode())
        val status = (api.status() as ServerResult.Ok).value
        assertTrue(status.canIssue)
        assertEquals(2, status.dailyLimit - status.issuedToday)
        assertEquals("expired", (api.redeem("abcd efgh") as ServerResult.Ok).value.error)
        assertEquals("{\"p_code\":\"abcd efgh\"}", http.calls.first { it.first.endsWith("access_redeem") }.second)
        assertEquals("ABCD-EFGH", (api.issue() as ServerResult.Ok).value.code)
        assertEquals("USED", (api.myCodes() as ServerResult.Ok).value.single().status)
    }

    @Test fun onlyClosedDoorsWithoutGrantBlock() {
        assertFalse(AccessStatusRow(mode = "OPEN", granted = false).blocked)
        assertFalse(AccessStatusRow(mode = "CLOSED_BETA", granted = true).blocked)
        assertTrue(AccessStatusRow(mode = "CLOSED_BETA", granted = false).blocked)
        assertTrue(AccessStatusRow(mode = "OPEN_BETA", granted = false).blocked)
    }

    @Test fun everyServerErrorHasAMessage() {
        val known = listOf("not_found", "revoked", "expired", "used_up", "too_many", "blocked").map(::errorText)
        assertEquals(known.size, known.toSet().size)
        assertEquals(R.string.access_error_network, errorText("something_new"))
    }
}
