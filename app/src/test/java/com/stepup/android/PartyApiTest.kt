package com.stepup.android

import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.CourseApi
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.PartyApi
import com.stepup.android.data.remote.PushApi
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.repo.CourseRepository
import com.stepup.android.data.repo.toDomain
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 파티 방과 코스 게시판 — 서버가 보낸 줄을 앱이 읽는 부분과, 앱이 서버 함수에 보내는 몸통.
 *
 * 파티 방은 이름 하나만 어긋나도 로비가 조용히 멈춘다. `server_now` 를 못 읽으면
 * 카운트다운이 폰 시계로 돌아 사람마다 다른 순간에 출발하고, `is_host` 를 못
 * 읽으면 아무도 출발 버튼을 못 본다.
 */
class PartyApiTest {

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

    private fun server(http: FakeHttp) = StepUpServer(
        baseUrl = "https://test.supabase.co",
        apiKey = "sb_publishable_test",
        sessions = SessionHolder(
            auth = SupabaseAuth("https://test.supabase.co", "sb_publishable_test", http),
            store = LoggedIn(),
            now = { 1_000L },
        ),
        http = http,
    )

    /** party_state 가 실제로 내려주는 모양 그대로 — 카운트다운 중인 방 */
    private val room = """
        {"id":9,"status":"COUNTDOWN","host_id":"u-host",
         "crew_id":"00000000-0000-0000-0000-00000000c0de","flash_post_id":null,
         "starts_at":"2026-09-23T10:00:04.5+00:00","server_now":"2026-09-23T10:00:00.5+00:00",
         "members":[
           {"user_id":"u-host","name":"Ara Kim","ready":true,"is_me":false,"is_host":true,
            "lat":37.5265,"lng":126.924,"seen_sec":1},
           {"user_id":"u-me","name":"Bo Lee","ready":true,"is_me":true,"is_host":false,
            "lat":null,"lng":null,"seen_sec":0}
         ]}
    """.trimIndent()

    @Test
    fun `방 상태의 이름과 시각이 서버와 맞는다`() = runBlocking {
        val result = PartyApi(server(FakeHttp(HttpResponse(200, room)))).state(9)
        assertTrue(result is ServerResult.Ok)
        val state = (result as ServerResult.Ok).value

        assertEquals("COUNTDOWN", state.status)
        assertEquals("00000000-0000-0000-0000-00000000c0de", state.crewId)
        assertNull(state.flashPostId)
        assertEquals("2026-09-23T10:00:04.5+00:00", state.startsAt)
        assertTrue(state.members[0].isHost)
        assertTrue(state.members[1].isMe)
        assertEquals(37.5265, state.members[0].lat!!, 1e-9)
        assertNull(state.members[1].lat)
    }

    @Test
    fun `크루 로비는 번개 번호를 null 로 보낸다`() = runBlocking {
        val http = FakeHttp(HttpResponse(200, "9"))
        val result = PartyApi(server(http)).open("crew-1", null)

        assertEquals(ServerResult.Ok(9L), result)
        assertTrue(http.lastUrl.endsWith("/rpc/party_open"))
        assertEquals("""{"p_crew":"crew-1","p_post":null}""", http.lastBody)
    }

    @Test
    fun `위치를 모르면 null 로 보고한다`() = runBlocking {
        val http = FakeHttp(HttpResponse(204, ""))
        PartyApi(server(http)).ping(9, null, null)
        assertEquals("""{"p_party":9,"p_lat":null,"p_lng":null}""", http.lastBody)
    }

    @Test
    fun `방장이 내보내면 거절로 읽는다`() = runBlocking {
        val http = FakeHttp(HttpResponse(403, """{"message":"이 파티에 들어와 있지 않습니다"}"""))
        assertEquals(
            ServerResult.Rejected("이 파티에 들어와 있지 않습니다"),
            PartyApi(server(http)).state(9),
        )
    }

    @Test
    fun `게시판 코스는 폰의 코스와 번호가 겹치지 않는다`() = runBlocking {
        val body = """
            [{"id":3,"owner_id":"u1","author":"Ara Kim","name":"Seoul Forest loop","area":"Seongsu",
              "distance_km":2.4,"elevation_m":19,"track":"37.5,127.0;37.51,127.01","shared":true,
              "run_count":7,"created_at":"2026-09-23T08:00:00+00:00","likes":5,"liked":true,"mine":false}]
        """.trimIndent()
        val result = CourseApi(server(FakeHttp(HttpResponse(200, body)))).board()
        val course = (result as ServerResult.Ok).value.single().toDomain()

        assertEquals(CourseRepository.REMOTE_BASE + 3, course.id)
        assertEquals(2, course.points.size)
        assertEquals(5, course.likes)
        assertTrue(course.liked)
        assertTrue(course.shared)
        assertEquals("37.5,127.0;37.51,127.01", course.encode())
    }

    @Test
    fun `코스 올리기는 경로를 서버 이름 그대로 보낸다`() = runBlocking {
        val http = FakeHttp(HttpResponse(200, "3"))
        val result = CourseApi(server(http)).share("Loop", "Mapo", 2.5, 20, "37.5,127.0;37.51,127.01")

        assertEquals(ServerResult.Ok(3L), result)
        assertEquals(
            """{"p_name":"Loop","p_area":"Mapo","p_distance_km":2.5,"p_elevation_m":20,""" +
                """"p_track":"37.5,127.0;37.51,127.01"}""",
            http.lastBody,
        )
    }

    @Test
    fun `푸시 토큰은 앱 언어와 함께 적는다`() = runBlocking {
        val http = FakeHttp(HttpResponse(204, ""))
        val result = PushApi(server(http)).register("fcm-token-aaaaaaaaaaaaaaaaaaaa", "ko")

        assertEquals(ServerResult.Ok(Unit), result)
        assertTrue(http.lastUrl.endsWith("/rpc/push_register"))
        assertEquals("""{"p_token":"fcm-token-aaaaaaaaaaaaaaaaaaaa","p_locale":"ko"}""", http.lastBody)
    }
}
