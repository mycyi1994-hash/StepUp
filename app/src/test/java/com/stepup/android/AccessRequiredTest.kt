package com.stepup.android

import com.stepup.android.data.remote.ACCESS_REQUIRED
import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.remote.isAccessRequired
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 접속 코드 문(0079 access_gate)이 막은 답 — 권한을 다시 묻는 갈고리와 러닝 올리기가 같은 판단을 쓴다(2026-11-04 점검).
 * 이 거절은 영구 거절이 아니다(코드를 넣으면 같은 요청이 받아진다).
 */
class AccessRequiredTest {

    private class LoggedIn : AuthSessionStore {
        override suspend fun load() = AuthSession(
            accessToken = "token", refreshToken = "refresh", expiresIn = 3600, expiresAt = 9_999_999_999L,
            user = AuthUser(id = "me", isAnonymous = false),
        )
        override suspend fun save(session: AuthSession) = Unit
        override suspend fun clear() = Unit
    }

    private class Http(private val reply: HttpResponse) : HttpPoster {
        override suspend fun post(url: String, body: String, headers: Map<String, String>) = reply
        override suspend fun get(url: String, headers: Map<String, String>) = reply
    }

    private fun server(reply: HttpResponse) = Http(reply).let { http ->
        StepUpServer(
            "https://test.supabase.co", "k",
            SessionHolder(SupabaseAuth("https://test.supabase.co", "k", http), LoggedIn(), now = { 1_000L }),
            http,
        )
    }

    @Test fun `문에 막힌 403 은 정한 까닭으로 돌려주고 권한을 다시 묻는다`() = runBlocking {
        // PostgREST 가 db_pre_request 의 raise(PT403)를 옮긴 모양 그대로
        val gate = HttpResponse(403, """{"code":"PT403","details":"접속 코드가 필요합니다","hint":"access_required","message":"access_required"}""")
        val server = server(gate)
        var asked = 0
        server.onAccessRequired = { asked++ }

        val result = server.balance()

        assertEquals(ServerResult.Rejected(ACCESS_REQUIRED), result)
        assertTrue(result.isAccessRequired())
        assertEquals(1, asked)
    }

    @Test fun `다른 거절은 접속 코드 문이 아니다`() = runBlocking {
        val server = server(HttpResponse(403, """{"code":"42501","message":"permission denied for table sup_balances"}"""))
        var asked = 0
        server.onAccessRequired = { asked++ }

        val result = server.balance()

        assertTrue(result is ServerResult.Rejected)
        assertFalse(result.isAccessRequired())
        assertEquals(0, asked)
        assertFalse(ServerResult.Rejected("러닝으로 확인되지 않았습니다").isAccessRequired())
        assertFalse(ServerResult.Retry(ACCESS_REQUIRED).isAccessRequired())
    }
}
