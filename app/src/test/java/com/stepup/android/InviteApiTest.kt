package com.stepup.android

import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.InviteApi
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.ui.screens.invite.rewardLabel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InviteApiTest {
    private class Store(var value: AuthSession?) : AuthSessionStore {
        override suspend fun load() = value
        override suspend fun save(session: AuthSession) { value = session }
        override suspend fun clear() { value = null }
    }

    private class Http(private val reply: (String, String) -> HttpResponse) : HttpPoster {
        val bodies = mutableListOf<String>()
        override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
            bodies += body
            return reply(url.substringAfterLast('/'), body)
        }
        override suspend fun get(url: String, headers: Map<String, String>) = post(url, "", headers)
    }

    private fun api(http: Http): InviteApi {
        val session = AuthSession(
            accessToken = "token", refreshToken = "refresh", expiresIn = 3600,
            expiresAt = 9_999_999_999L, user = AuthUser(id = "A", isAnonymous = false),
        )
        val holder = SessionHolder(SupabaseAuth("https://test.supabase.co", "test", http), Store(session), now = { 1000L })
        return InviteApi(StepUpServer("https://test.supabase.co", "test", holder, http))
    }

    @Test fun readsStatusAndInvitees() = runBlocking {
        val http = Http { name, _ ->
            when (name) {
                "invite_status" -> HttpResponse(200, """[{"code":"STEP-AB23CD","reward_sup":0,"invited":2,"rewarded":1,
                    "can_redeem":false,"redeemed":false}]""")
                "invite_list" -> HttpResponse(200, """[{"display_name":"하윤","joined_at":"2026-09-26T01:00:00+00:00","rewarded_at":null}]""")
                else -> HttpResponse(404, "{}")
            }
        }
        val status = (api(http).status() as ServerResult.Ok).value
        assertEquals("STEP-AB23CD", status.code)
        assertEquals(2, status.invited)
        assertNull("no reward promise while the server amount is 0", rewardLabel(status.rewardSup))
        val list = (api(http).invitees() as ServerResult.Ok).value
        assertNull(list.single().rewardedAt)
    }

    @Test fun redeemSendsTheCodeAndSurfacesTheServerReason() = runBlocking {
        val ok = Http { _, _ -> HttpResponse(200, "\"하윤\"") }
        assertEquals("하윤", (api(ok).redeem("step-ab23cd") as ServerResult.Ok).value)
        assertTrue(ok.bodies.last().contains("\"p_code\":\"step-ab23cd\""))
        val rejected = Http { _, _ -> HttpResponse(400, """{"message":"이미 초대 코드를 입력했어요"}""") }
        assertEquals("이미 초대 코드를 입력했어요", (api(rejected).redeem("STEP-AB23CD") as ServerResult.Rejected).reason)
    }

    @Test fun rewardLabelOnlyForPositiveAmounts() {
        assertEquals("5", rewardLabel(5.0))
        assertEquals("2.50", rewardLabel(2.5))
        assertNull(rewardLabel(0.0))
    }
}
