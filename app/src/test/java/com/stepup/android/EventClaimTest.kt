package com.stepup.android

import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.DaySteps
import com.stepup.android.data.remote.EventApi
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.repo.Events
import com.stepup.android.data.repo.eventPeriodKey
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 도전 보상 — 앱이 서버에 보내는 몸통과, 한 번씩 받는 기간의 이름.
 *
 * 기간 이름이 서버(economy.event_period: ISO 주)와 어긋나면 앱은 "아직 안
 * 받음"으로 버튼을 켜 두고, 서버는 "이미 받음"으로 거절한다.
 */
class EventClaimTest {

    private class FakeHttp(private val answer: HttpResponse) : HttpPoster {
        var lastBody: String = ""
        var lastUrl: String = ""

        override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
            lastUrl = url
            lastBody = body
            return answer
        }

        override suspend fun get(url: String, headers: Map<String, String>) = post(url, "", headers)
    }

    private class LoggedIn : AuthSessionStore {
        override suspend fun load() = AuthSession(
            accessToken = "token",
            refreshToken = "refresh",
            expiresIn = 3600,
            expiresAt = 9_999_999_999L,
            user = AuthUser(id = "me", isAnonymous = false),
        )

        override suspend fun save(session: AuthSession) = Unit
        override suspend fun clear() = Unit
    }

    private fun api(http: FakeHttp) = EventApi(
        StepUpServer(
            baseUrl = "https://test.supabase.co",
            apiKey = "sb_publishable_test",
            sessions = SessionHolder(
                auth = SupabaseAuth("https://test.supabase.co", "sb_publishable_test", http),
                store = LoggedIn(),
                now = { 1_000L },
            ),
            http = http,
        ),
    )

    @Test
    fun `주간 도전은 ISO 주마다 이름이 바뀐다`() {
        // 2026-09-23 은 ISO 39주, 2027-01-01 은 2026년의 53주다(해가 바뀌어도 주가 이어진다)
        assertEquals("step_surge:2026-W39", eventPeriodKey(Events.STEP_SURGE, LocalDate.of(2026, 9, 23)))
        assertEquals("step_surge:2026-W53", eventPeriodKey(Events.STEP_SURGE, LocalDate.of(2027, 1, 1)))
        assertEquals("night_quest", eventPeriodKey(Events.NIGHT_QUEST, LocalDate.of(2026, 9, 23)))
    }

    @Test
    fun `받기는 도전 이름과 기기 시간대를 보낸다`() = runBlocking {
        val http = FakeHttp(HttpResponse(200, "250"))
        val result = api(http).claim("step_surge", "Asia/Seoul")

        assertEquals(ServerResult.Ok(250.0), result)
        assertTrue(http.lastUrl.endsWith("/rpc/event_claim"))
        assertEquals("""{"p_event":"step_surge","p_tz":"Asia/Seoul"}""", http.lastBody)
    }

    @Test
    fun `걸음은 날짜 배열로 올린다`() = runBlocking {
        val http = FakeHttp(HttpResponse(200, "2"))
        api(http).syncSteps(listOf(DaySteps(20719, 12000, 8000), DaySteps(20720, 30000, 8000)))

        assertTrue(http.lastUrl.endsWith("/rpc/steps_sync"))
        assertEquals(
            """{"p_days":[{"epoch_day":20719,"steps":12000,"goal":8000},{"epoch_day":20720,"steps":30000,"goal":8000}]}""",
            http.lastBody,
        )
    }

    @Test
    fun `서버가 거절한 이유를 그대로 읽는다`() = runBlocking {
        val http = FakeHttp(HttpResponse(409, """{"code":"23505","message":"이미 받은 보상입니다"}"""))
        assertEquals(ServerResult.Rejected("이미 받은 보상입니다"), api(http).claim("night_quest", "Asia/Seoul"))
    }
}
