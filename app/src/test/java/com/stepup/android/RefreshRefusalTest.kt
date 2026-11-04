package com.stepup.android

import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.remote.TokenResult
import com.stepup.android.data.remote.refreshTokenDead
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 출입증 갱신이 거절됐을 때 — 죽은 refresh token 만 로그아웃이고, 나머지는 나중에 다시(2026-11-04 점검).
 *
 * 예전엔 429 를 뺀 모든 4xx 를 죽은 토큰으로 보아, 공유기 · 프록시 · 와이파이 로그인 쪽지(HTML)나 403 · 404 · 407 · 408 에도
 * 다시 로그인하러 보냈다.
 */
class RefreshRefusalTest {

    // ── 죽은 토큰 — Supabase Auth 의 JSON 오류 ─────────────────────────

    @Test fun `Supabase 가 죽은 토큰이라고 말하면 다시 로그인한다`() {
        // 지금 판(기본) — 상태 숫자 code · 글자 error_code · msg
        assertTrue(refreshTokenDead(400, """{"code":400,"error_code":"refresh_token_not_found","msg":"Invalid Refresh Token: Refresh Token Not Found"}"""))
        assertTrue(refreshTokenDead(400, """{"code":400,"error_code":"refresh_token_already_used","msg":"Invalid Refresh Token: Already Used"}"""))
        assertTrue(refreshTokenDead(401, """{"error_code":"session_not_found","msg":"Session not found"}"""))
        assertTrue(refreshTokenDead(400, """{"error_code":"session_expired","msg":"Session Expired"}"""))
        // 2024-01-01 판 — 글자 code · message
        assertTrue(refreshTokenDead(400, """{"code":"refresh_token_not_found","message":"Invalid Refresh Token: Refresh Token Not Found"}"""))
        // 예전 OAuth 판 — error · error_description
        assertTrue(refreshTokenDead(400, """{"error":"invalid_grant","error_description":"Invalid Refresh Token: Refresh Token Not Found"}"""))
        // 코드 없이 글만
        assertTrue(refreshTokenDead(400, """{"msg":"Invalid Refresh Token"}"""))
    }

    // ── 잠깐의 문제 — 나중에 다시 ─────────────────────────────────────

    @Test fun `JSON 이 아닌 쪽지는 죽은 토큰이 아니다`() {
        val portal = "<html><head><title>Wi-Fi 로그인</title></head><body>Invalid Refresh Token</body></html>"
        assertFalse(refreshTokenDead(400, portal))
        assertFalse(refreshTokenDead(401, ""))
        assertFalse(refreshTokenDead(400, "Bad Request"))
        assertFalse(refreshTokenDead(400, """["refresh_token_not_found"]"""))
    }

    @Test fun `400 · 401 이 아니면 본문이 같아도 죽은 토큰이 아니다`() {
        val dead = """{"error_code":"refresh_token_not_found","msg":"Invalid Refresh Token: Refresh Token Not Found"}"""
        for (status in listOf(403, 404, 407, 408, 409, 429, 500, 502, 503, 0, 200)) {
            assertFalse("$status", refreshTokenDead(status, dead))
        }
    }

    @Test fun `모르는 오류 코드 · 다른 invalid_grant 는 죽은 토큰이 아니다`() {
        assertFalse(refreshTokenDead(400, """{"error_code":"validation_failed","msg":"Invalid Refresh Token"}"""))
        assertFalse(refreshTokenDead(400, """{"error_code":"bad_json","msg":"Could not parse request body as JSON"}"""))
        assertFalse(refreshTokenDead(401, """{"message":"Invalid API key","hint":"Double check your Supabase `anon` or `service_role` API key."}"""))
        assertFalse(refreshTokenDead(400, """{"error":"invalid_grant","error_description":"Invalid login credentials"}"""))
        assertFalse(refreshTokenDead(400, """{"code":400,"msg":"Something else"}"""))
    }

    // ── 로그인 상태까지 ───────────────────────────────────────────────

    private class Store(var value: AuthSession?) : AuthSessionStore {
        var cleared = 0
        override suspend fun load() = value
        override suspend fun save(session: AuthSession) { value = session }
        override suspend fun clear() { value = null; cleared++ }
    }

    private class Http(private val reply: HttpResponse) : HttpPoster {
        var calls = 0
        override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
            calls++
            return reply
        }
        override suspend fun get(url: String, headers: Map<String, String>) = post(url, "", headers)
    }

    private fun holder(reply: HttpResponse): Triple<SessionHolder, Store, Http> {
        val store = Store(
            AuthSession(
                accessToken = "old", refreshToken = "refresh", expiresIn = 3600, expiresAt = 0L,
                user = AuthUser(id = "me", isAnonymous = false),
            ),
        )
        val http = Http(reply)
        val sessions = SessionHolder(
            SupabaseAuth("https://test.supabase.co", "k", http, now = { 1_000L }),
            store,
            now = { 1_000L },
            monotonicMs = { 0L },
        )
        return Triple(sessions, store, http)
    }

    @Test fun `프록시 쪽지 · 403 · 404 · 407 · 408 은 로그아웃시키지 않는다`() = runBlocking {
        val replies = listOf(
            HttpResponse(400, "<html><body>Your session with the hotspot has expired</body></html>"),
            HttpResponse(403, """{"error_code":"refresh_token_not_found","msg":"Invalid Refresh Token"}"""),
            HttpResponse(404, "<html>Not Found</html>"),
            HttpResponse(407, "Proxy Authentication Required"),
            HttpResponse(408, ""),
            HttpResponse(401, """{"message":"Invalid API key"}"""),
        )
        for (reply in replies) {
            val (sessions, store, _) = holder(reply)
            val result = sessions.accessToken()
            assertTrue("${reply.status} ${reply.body}", result is TokenResult.Unavailable)
            assertFalse("${reply.status} — 다시 로그인하러 보내지 않는다", sessions.signInRequired.value)
            assertEquals("old", store.value?.accessToken)
            assertEquals(0, store.cleared)
        }
    }

    @Test fun `잠깐의 문제였으면 쉬는 시간 뒤 같은 토큰으로 다시 갱신해 본다`() = runBlocking {
        var clock = 0L
        val store = Store(
            AuthSession(
                accessToken = "old", refreshToken = "refresh", expiresIn = 3600, expiresAt = 0L,
                user = AuthUser(id = "me", isAnonymous = false),
            ),
        )
        val http = Http(HttpResponse(407, "Proxy Authentication Required"))
        val sessions = SessionHolder(
            SupabaseAuth("https://test.supabase.co", "k", http, now = { 1_000L }),
            store,
            now = { 1_000L },
            monotonicMs = { clock },
        )

        assertTrue(sessions.accessToken() is TokenResult.Unavailable)
        clock = SessionHolder.REFRESH_PAUSE_MS
        assertTrue(sessions.accessToken() is TokenResult.Unavailable)
        assertEquals("죽은 토큰으로 기억하지 않는다", 2, http.calls)
    }

    @Test fun `죽은 토큰이면 다시 로그인하게 하고 스스로 나가면 그 표시를 지운다`() = runBlocking {
        val (sessions, _, http) = holder(
            HttpResponse(400, """{"code":400,"error_code":"refresh_token_not_found","msg":"Invalid Refresh Token: Refresh Token Not Found"}"""),
        )

        assertTrue(sessions.accessToken() is TokenResult.SignInRequired)
        assertTrue(sessions.signInRequired.value)
        assertTrue(sessions.accessToken() is TokenResult.SignInRequired)
        assertEquals("죽은 토큰은 다시 보내지 않는다", 1, http.calls)

        // 계정 삭제 · 로그아웃의 정리 — 로그인 화면이 "다시 로그인해 주세요"를 보이지 않게
        sessions.forgetRefusal()
        assertFalse(sessions.signInRequired.value)
    }
}
