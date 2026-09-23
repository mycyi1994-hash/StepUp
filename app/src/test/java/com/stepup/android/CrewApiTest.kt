package com.stepup.android

import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.CrewApi
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.repo.CrewJoinPolicy
import com.stepup.android.data.repo.toDomain
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 서버가 보낸 크루 줄을 앱이 읽는 부분.
 *
 * 이름이 어긋나면 오류 없이 기본값이 들어온다. `join_policy` 를 `joinPolicy` 로
 * 읽으면 모든 크루가 자유 가입으로 보이고, 승인제 크루에 "가입하기" 버튼이
 * 뜬다 — 눌러도 신청만 들어가니 사용자는 버튼이 고장 난 줄 안다.
 */
class CrewApiTest {

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

    private fun api(http: FakeHttp) = CrewApi(
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

    /** crew_feed 가 실제로 내려주는 모양 그대로 */
    private val feed = """
        [
          {"id":"6a1f0c9e-0000-4000-8000-000000000001","owner_id":"u1","name":"새벽 6시 크루",
           "monogram":"D6","tagline":"출근 전 5km","area":"서울 성수","lat":null,"lng":null,
           "join_policy":"APPROVAL","created_at":"2026-09-23T01:00:00+00:00",
           "member_count":12,"roster":["Ara Kim","Bo Lee"],
           "joined":false,"requested":true,"pending_count":0,"owned":false}
        ]
    """.trimIndent()

    @Test
    fun `크루 줄의 이름이 서버와 맞는다`() = runBlocking {
        val result = api(FakeHttp(HttpResponse(200, feed))).crews()
        assertTrue(result is ServerResult.Ok)
        val crew = (result as ServerResult.Ok).value.single().toDomain()

        assertEquals(CrewJoinPolicy.APPROVAL, crew.joinPolicy)
        assertEquals(12, crew.memberCount)
        assertEquals(listOf("Ara Kim", "Bo Lee"), crew.roster)
        assertTrue(crew.requested)
        assertFalse(crew.joined)
        assertFalse(crew.owned)
    }

    @Test
    fun `모르는 가입 방식은 자유 가입으로 읽는다`() {
        assertEquals(CrewJoinPolicy.OPEN, CrewJoinPolicy.of("SOMETHING_NEW"))
        assertEquals(CrewJoinPolicy.APPROVAL, CrewJoinPolicy.of("APPROVAL"))
    }

    @Test
    fun `승인제 크루에 가입하면 신청이 되었다고 읽는다`() = runBlocking {
        val http = FakeHttp(HttpResponse(200, "\"REQUESTED\""))
        val result = api(http).join("6a1f0c9e-0000-4000-8000-000000000001")

        assertEquals(ServerResult.Ok("REQUESTED"), result)
        assertTrue(http.lastUrl.endsWith("/rpc/crew_join"))
        assertEquals("""{"p_crew":"6a1f0c9e-0000-4000-8000-000000000001"}""", http.lastBody)
    }

    @Test
    fun `크루를 만들면 새 id 를 받는다`() = runBlocking {
        val http = FakeHttp(HttpResponse(200, "\"6a1f0c9e-0000-4000-8000-000000000002\""))
        val result = api(http).create("한강 러너스", "HR", "토요일 7시", "마포", "APPROVAL")

        assertEquals(ServerResult.Ok("6a1f0c9e-0000-4000-8000-000000000002"), result)
        assertTrue(http.lastBody.contains("\"p_join_policy\":\"APPROVAL\""))
    }

    @Test
    fun `돌려주는 값이 없는 함수도 성공으로 읽는다`() = runBlocking {
        // 반환형이 void 인 함수는 빈 몸통으로 온다. 그것을 "못 읽었다"로 치면
        // 탈퇴는 됐는데 화면은 실패라고 말한다.
        val result = api(FakeHttp(HttpResponse(204, ""))).leave("c")
        assertEquals(ServerResult.Ok(Unit), result)
    }

    @Test
    fun `승인 여부는 참거짓으로 보낸다`() = runBlocking {
        val http = FakeHttp(HttpResponse(204, ""))
        api(http).decide("c", "u", approve = true)
        assertEquals("""{"p_crew":"c","p_user":"u","p_approve":true}""", http.lastBody)
    }

    @Test
    fun `크루장만 할 수 있는 일은 서버가 적어 보낸 이유로 거절된다`() = runBlocking {
        val http = FakeHttp(HttpResponse(403, """{"message":"크루장만 할 수 있습니다"}"""))
        val result = api(http).requests("c")
        assertEquals(ServerResult.Rejected("크루장만 할 수 있습니다"), result)
    }
}
