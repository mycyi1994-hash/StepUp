package com.stepup.android

import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.CourseApi
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.remote.TerritoryApi
import com.stepup.android.domain.Territory
import com.stepup.android.domain.haversineMeters
import com.stepup.android.domain.GeoPoint
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 땅따먹기 칸과 코스 기록 — 앱이 서버와 같은 칸을 그리는지, 서버에 무엇을 보내는지.
 *
 * 칸 이름은 서버(economy.hex_cell)가 정하고 앱은 그 이름으로 육각형을 그린다.
 * 둘의 식이 어긋나면 칠한 곳과 보이는 곳이 한 칸씩 밀린다. 아래 값은
 * supabase/tests/schema_test.sql 의 "칸 이름이 앱과 같은 식으로 나온다"와 같다.
 */
class TerritoryTest {

    @Test
    fun `칸 이름이 서버와 같다`() {
        assertEquals("44405:20068", Territory.cellOf(37.5443, 127.0557))
        assertEquals("44474:20003", Territory.cellOf(37.440309273233716, 127.13897349477489))
        assertEquals("44313:20121", Territory.cellOf(37.629132385692984, 126.90202761029576))
        assertEquals("44337:19995", Territory.cellOf(37.428157876032266, 126.8113389906088))
    }

    @Test
    fun `칸 가운데와 꼭짓점은 그 칸 안에 있다`() {
        val cell = Territory.cellOf(37.5443, 127.0557)
        val center = Territory.center(cell)!!
        assertEquals(cell, Territory.cellOf(center.lat, center.lng))
        assertTrue(haversineMeters(center, GeoPoint(37.5443, 127.0557)) < 200)

        val corners = Territory.corners(cell)
        assertEquals(6, corners.size)
        // 꼭짓점을 가운데 쪽으로 조금 당기면 여전히 같은 칸이다
        for (c in corners) {
            val inside = GeoPoint(center.lat + (c.lat - center.lat) * 0.9, center.lng + (c.lng - center.lng) * 0.9)
            assertEquals(cell, Territory.cellOf(inside.lat, inside.lng))
        }
    }

    @Test
    fun `1km 떨어지면 다른 칸이고 깨진 이름은 그리지 않는다`() {
        assertNotEquals(Territory.cellOf(37.5443, 127.0557), Territory.cellOf(37.5543, 127.0557))
        assertNull(Territory.center("엉망"))
        assertTrue(Territory.corners("1:x").isEmpty())
    }

    @Test
    fun `같은 크루는 늘 같은 색이다`() {
        val id = "0b1c2d3e-0000-4000-8000-000000000001"
        assertEquals(Territory.crewHue(id), Territory.crewHue(id))
        assertTrue(Territory.crewHue(id) in 0f..360f)
    }

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

    @Test
    fun `보이는 범위의 칸을 묻고 주인 크루를 읽는다`() = runBlocking {
        val http = FakeHttp(
            HttpResponse(
                200,
                """[{"cell":"44405:20068","lat":37.54,"lng":127.05,"crew_id":"c1","crew_name":"뚝섬","score":3,"mine":true}]""",
            ),
        )
        val result = TerritoryApi(server(http)).view(37.5, 127.0, 37.6, 127.1)

        assertTrue(http.lastUrl.endsWith("/rpc/territory_view"))
        assertEquals(
            """{"p_min_lat":37.5,"p_min_lng":127.0,"p_max_lat":37.6,"p_max_lng":127.1}""",
            http.lastBody,
        )
        val cells = (result as ServerResult.Ok).value
        assertEquals("뚝섬", cells.single().crewName)
        assertTrue(cells.single().mine)
    }

    @Test
    fun `코스 기록은 길과 러닝 시작 시각으로 낸다`() = runBlocking {
        val http = FakeHttp(HttpResponse(200, """[{"course_id":7,"duration_sec":601,"rank":2,"runners":5}]"""))
        val result = CourseApi(server(http)).submitRun("37.5,127.0;37.51,127.0", 1_700_000_000_000L, "me")

        assertTrue(http.lastUrl.endsWith("/rpc/course_run_submit"))
        assertEquals(
            """{"p_course_track":"37.5,127.0;37.51,127.0","p_started_at":"2023-11-14T22:13:20Z"}""",
            http.lastBody,
        )
        assertEquals(2, (result as ServerResult.Ok).value?.rank)
    }

    @Test
    fun `서버에 없는 코스면 기록 없이 넘어간다`() = runBlocking {
        val http = FakeHttp(HttpResponse(200, "[]"))
        val result = CourseApi(server(http)).submitRun("37.5,127.0;37.51,127.0", 1_700_000_000_000L, "me")
        assertEquals(ServerResult.Ok(null), result)
    }
}
