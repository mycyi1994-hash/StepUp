package com.stepup.android

import com.stepup.android.data.remote.AccountDeletion
import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 계정 삭제(설정 v1 13~15) — 서버 답을 셋으로 나눈다. 모르는 것(끊김 · 서버 오류)을 성공이나 실패로 단정하지 않고,
 * 보내지 못했거나 서버가 거절한 것만 "지우지 않았다"로 본다. 삭제는 account_delete 한 번만 부른다.
 */
class AccountDeletionTest {
    @Test fun `서버가 지웠다고 답하면 지웠다`() = runBlocking {
        val http = FakeHttp(HttpResponse(204, ""))
        assertEquals(AccountDeletion.Deleted, server(http).deleteAccount())
        assertTrue(http.urls.single().endsWith("/rpc/account_delete"))
    }

    @Test fun `응답을 못 받거나 서버 오류면 모른다`() = runBlocking {
        assertEquals(AccountDeletion.Unknown, server(FakeHttp(HttpResponse(0, "통신 실패: timeout"))).deleteAccount())
        assertEquals(AccountDeletion.Unknown, server(FakeHttp(HttpResponse(503, "busy"))).deleteAccount())
    }

    @Test fun `거절 · 인증 만료는 지우지 않았다`() = runBlocking {
        assertEquals(AccountDeletion.NotDeleted, server(FakeHttp(HttpResponse(400, """{"message":"no"}"""))).deleteAccount())
        assertEquals(AccountDeletion.NotDeleted, server(FakeHttp(HttpResponse(401, ""))).deleteAccount())
        assertEquals(AccountDeletion.NotDeleted, server(FakeHttp(HttpResponse(429, ""))).deleteAccount())
    }

    @Test fun `서버 주소가 없으면 보내지 않는다`() = runBlocking {
        val http = FakeHttp(HttpResponse(204, ""))
        val offline = StepUpServer(baseUrl = "", apiKey = "", sessions = sessions(http), http = http)
        assertEquals(AccountDeletion.NotDeleted, offline.deleteAccount())
        assertTrue(http.urls.isEmpty())
    }

    private class FakeHttp(private val answer: HttpResponse) : HttpPoster {
        val urls = mutableListOf<String>()
        override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
            urls += url
            return answer
        }
        override suspend fun get(url: String, headers: Map<String, String>) = post(url, "", headers)
    }

    private class LoggedIn : AuthSessionStore {
        override suspend fun load() = AuthSession(
            accessToken = "token", refreshToken = "refresh", expiresIn = 3600, expiresAt = 9_999_999_999L,
            user = AuthUser(id = "me", isAnonymous = false),
        )
        override suspend fun save(session: AuthSession) = Unit
        override suspend fun clear() = Unit
    }

    private fun sessions(http: FakeHttp) = SessionHolder(
        auth = SupabaseAuth("https://test.supabase.co", "sb_publishable_test", http),
        store = LoggedIn(),
        now = { 1_000L },
    )

    private fun server(http: FakeHttp) = StepUpServer(
        baseUrl = "https://test.supabase.co", apiKey = "sb_publishable_test", sessions = sessions(http), http = http,
    )
}
