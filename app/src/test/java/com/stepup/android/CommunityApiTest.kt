package com.stepup.android

import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.CommunityApi
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.repo.toDomain
import com.stepup.android.domain.PostCategory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 서버가 보낸 글·댓글 줄을 앱이 읽는 부분과, 앱이 서버 함수에 보내는 몸통.
 *
 * 여기서 어긋나면 오류 없이 틀린 값이 들어온다. `created_at` 을 못 읽으면
 * 모든 글이 1970년에 쓴 글이 되고, 크루 id 가 없는 글(null)을 못 읽으면 전체
 * 게시판이 통째로 빈다.
 */
class CommunityApiTest {

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

    private fun api(http: FakeHttp) = CommunityApi(
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

    /** post_feed 가 실제로 내려주는 모양 그대로 — 전체 게시판 번개 하나 */
    private val feed = """
        [
          {"id":7,"category":"FLASH","crew_id":null,"author_id":"u-sora","author":"Sora K.",
           "title":"오늘 저녁 7시 번개","body":"천천히","place":"여의도 3문",
           "distance_km":5,"meet_at":"2026-09-23T10:00:00.123456+00:00","capacity":8,
           "created_at":"2026-09-23T08:00:00+00:00",
           "likes":3,"comment_count":2,"joined_count":5,
           "liked":true,"joined":false,"mine":false}
        ]
    """.trimIndent()

    @Test
    fun `글 줄의 이름과 시각이 서버와 맞는다`() = runBlocking {
        val result = api(FakeHttp(HttpResponse(200, feed))).posts()
        assertTrue(result is ServerResult.Ok)
        val post = (result as ServerResult.Ok).value.single().toDomain()

        assertEquals(PostCategory.FLASH, post.category)
        assertEquals("", post.crewId)
        assertEquals("u-sora", post.authorId)
        assertEquals(1_790_150_400_000L, post.createdAt)
        assertEquals(1_790_157_600_123L, post.meetAt)
        assertEquals(5, post.joinedCount)
        assertEquals(2, post.commentCount)
        assertTrue(post.liked)
    }

    @Test
    fun `전체 게시판 글은 크루와 모임 시각을 null 로 보낸다`() = runBlocking {
        val http = FakeHttp(HttpResponse(200, "42"))
        val result = api(http).createPost("FREE", "", "제목", "본문", "", 0.0, "", 0)

        assertEquals(ServerResult.Ok(42L), result)
        assertTrue(http.lastUrl.endsWith("/rpc/post_create"))
        // 문자열 "null" 이 아니라 JSON null 이어야 서버가 "크루 없음"으로 읽는다.
        assertTrue(http.lastBody, http.lastBody.contains("\"p_crew\":null"))
        assertTrue(http.lastBody, http.lastBody.contains("\"p_meet_at\":null"))
    }

    @Test
    fun `좋아요는 누른 뒤의 상태를 읽는다`() = runBlocking {
        val result = api(FakeHttp(HttpResponse(200, "false"))).toggleLike(7)
        assertEquals(ServerResult.Ok(false), result)
    }

    @Test
    fun `댓글 줄의 부모 번호를 읽는다`() = runBlocking {
        val body = """
            [{"id":11,"post_id":7,"parent_id":0,"author_id":"u1","author":"Ara Kim",
              "body":"저도 갈래요","created_at":"2026-09-23T08:05:00+00:00","mine":true},
             {"id":12,"post_id":7,"parent_id":11,"author_id":"u2","author":"Bo Lee",
              "body":"같이 가요","created_at":"2026-09-23T08:06:00+00:00","mine":false}]
        """.trimIndent()
        val result = api(FakeHttp(HttpResponse(200, body))).comments(7)
        val comments = (result as ServerResult.Ok).value.map { it.toDomain() }

        assertEquals(0L, comments[0].parentId)
        assertEquals(11L, comments[1].parentId)
        assertTrue(comments[1].isReply)
    }

    @Test
    fun `신고는 대상 종류와 사유를 서버 이름 그대로 보낸다`() = runBlocking {
        val http = FakeHttp(HttpResponse(204, ""))
        api(http).report("POST", "7", "SPAM")
        assertEquals(
            """{"p_type":"POST","p_target":"7","p_reason":"SPAM","p_note":""}""",
            http.lastBody,
        )
    }

    @Test
    fun `번개 참가자 명단은 주최자와 나를 가려 읽는다`() = runBlocking {
        val body = """
            [{"user_id":"u-sora","name":"Sora K.","is_host":true,"is_me":false},
             {"user_id":"me","name":"Ara Kim","is_host":false,"is_me":true}]
        """.trimIndent()
        val http = FakeHttp(HttpResponse(200, body))
        val roster = (api(http).roster(7) as ServerResult.Ok).value

        assertTrue(http.lastUrl, http.lastUrl.contains("/flash_roster?") && http.lastUrl.contains("post_id=eq.7"))
        assertTrue(roster[0].isHost)
        assertTrue(roster[1].isMe)
        assertEquals("Ara Kim", roster[1].name)
    }

    @Test
    fun `번개 모임 장소 좌표를 보내고 읽는다`() = runBlocking {
        val http = FakeHttp(HttpResponse(200, "8"))
        api(http).createPost("FLASH", "", "번개", "", "여의도", 5.0, "2026-09-23T10:00:00Z", 6, 37.5265, 126.924)
        assertTrue(http.lastBody, http.lastBody.contains("\"p_lat\":37.5265,\"p_lng\":126.924"))

        val withPlace = feed.replace("\"place\":\"여의도 3문\",", "\"place\":\"여의도 3문\",\"lat\":37.5265,\"lng\":126.924,")
        val post = (api(FakeHttp(HttpResponse(200, withPlace))).posts() as ServerResult.Ok).value.single().toDomain()
        assertEquals(0.0, post.awayKmFrom(com.stepup.android.domain.GeoPoint(37.5265, 126.924))!!, 1e-6)
        assertEquals(null, post.copy(lat = null).awayKmFrom(com.stepup.android.domain.GeoPoint(37.5, 127.0)))
    }

    @Test
    fun `정원이 찬 번개는 서버가 적어 보낸 이유로 거절된다`() = runBlocking {
        val http = FakeHttp(HttpResponse(400, """{"message":"정원이 찼습니다 (8명)"}"""))
        assertEquals(ServerResult.Rejected("정원이 찼습니다 (8명)"), api(http).joinFlash(7))
    }
}
