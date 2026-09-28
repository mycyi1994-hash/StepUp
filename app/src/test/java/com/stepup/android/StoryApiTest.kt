package com.stepup.android

import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.CommunityApi
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.PlaceSearchApi
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.repo.PlaceSearch
import com.stepup.android.data.repo.PlaceSearchResult
import com.stepup.android.data.repo.toDomain
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.StoryPlace
import com.stepup.android.domain.coarse
import com.stepup.android.domain.storyPlace
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 동네 이야기가 서버 · 장소 검색과 주고받는 모양.
 *
 * story_create · story_update 의 인자 이름이 0041 과 어긋나면 서버가 "함수를 찾을 수 없다"로 거절하고,
 * MapTiler 응답을 잘못 읽으면 장소 이름 대신 엉뚱한 도로 이름이 글에 붙는다.
 */
class StoryApiTest {

    private class FakeHttp(private val answer: (String) -> HttpResponse) : HttpPoster {
        var lastBody = ""
        var lastUrl = ""
        override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
            lastUrl = url
            lastBody = body
            return answer(url)
        }
        override suspend fun get(url: String, headers: Map<String, String>) = post(url, "", headers)
    }

    private object LoggedIn : AuthSessionStore {
        override suspend fun load() = AuthSession("token", "refresh", 3600, 9_999_999_999L, AuthUser(id = "me"))
        override suspend fun save(session: AuthSession) = Unit
        override suspend fun clear() = Unit
    }

    private fun api(http: FakeHttp) = CommunityApi(
        StepUpServer(
            baseUrl = "https://test.supabase.co",
            apiKey = "sb_publishable_test",
            sessions = SessionHolder(SupabaseAuth("https://test.supabase.co", "sb_publishable_test", http), LoggedIn, now = { 1_000L }),
            http = http,
        ),
    )

    @Test fun `이야기 쓰기는 0041 의 인자 이름으로 보내고 새 글 번호를 읽는다`() = runBlocking {
        val http = FakeHttp { HttpResponse(200, "501") }
        val result = api(http).createStory("한강 러닝", "\n7시에 만나요", "여의도한강공원", "서울특별시 영등포구 여의동로", 37.5284, 126.934)

        assertEquals(ServerResult.Ok(501L), result)
        assertTrue(http.lastUrl.endsWith("/rpc/story_create"))
        assertEquals(
            """{"p_title":"한강 러닝","p_body":"\n7시에 만나요","p_place":"여의도한강공원",""" +
                """"p_place_address":"서울특별시 영등포구 여의동로","p_lat":37.5284,"p_lng":126.934}""",
            http.lastBody,
        )
    }

    @Test fun `이야기 고치기는 같은 글 번호로 보낸다`() = runBlocking {
        val http = FakeHttp { HttpResponse(204, "") }
        val result = api(http).updateStory(304, "제목", "", "여의도공원", "", 37.5261, 126.9225)

        assertTrue(result is ServerResult.Ok)
        assertTrue(http.lastUrl.endsWith("/rpc/story_update"))
        assertTrue(http.lastBody, http.lastBody.startsWith("""{"p_post":304,"p_title":"제목""""))
    }

    @Test fun `개인정보 노출 신고는 OTHER 와 원래 사유를 함께 보낸다`() = runBlocking {
        val http = FakeHttp { HttpResponse(204, "") }
        val reason = com.stepup.android.domain.StoryReportReason.PRIVACY
        api(http).report("POST", "7", reason.serverReason, reason.note)
        assertEquals("""{"p_type":"POST","p_target":"7","p_reason":"OTHER","p_note":"PRIVACY"}""", http.lastBody)
    }

    @Test fun `글 줄의 장소 주소를 읽고 예전 서버에는 빈 주소로 둔다`() = runBlocking {
        val feed = """
            [{"id":1,"category":"FREE","crew_id":null,"author":"민수","title":"여의도공원 한 바퀴","body":"",
              "place":"여의도공원","lat":37.5261,"lng":126.9225,"created_at":"2026-09-27T08:00:00Z",
              "place_address":"서울특별시 영등포구 여의공원로"},
             {"id":2,"category":"FREE","crew_id":null,"author":"도윤","title":"러닝 초보 질문","body":"",
              "created_at":"2026-09-27T07:00:00+00:00"}]
        """.trimIndent()
        val posts = (api(FakeHttp { HttpResponse(200, feed) }).posts() as ServerResult.Ok).value.map { it.toDomain() }

        assertEquals(StoryPlace("여의도공원", "서울특별시 영등포구 여의공원로", 37.5261, 126.9225), posts[0].storyPlace)
        assertEquals("", posts[1].placeAddress)
        assertNull(posts[1].storyPlace)
    }

    // ── MapTiler Geocoding ────────────────────────────────────────

    private val parkPoi = """{"features":[{"id":"poi.1","text":"여의도공원","place_type":["poi"],
        "center":[126.9225,37.5261],
        "context":[{"id":"address.1","text":"여의공원로"},{"id":"postal_code.1","text":"07243"},
                   {"id":"place.2","text":"여의도동"},{"id":"municipality.3","text":"여의동"},
                   {"id":"county.4","text":"영등포구"},{"id":"region.5","text":"서울특별시"},{"id":"country.6","text":"대한민국"}]}]}"""

    @Test fun `장소 검색은 시설까지 찾고 주소를 시 · 구 · 길로 적는다`() = runBlocking {
        var asked = ""
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.GERMANY) // 소수점이 쉼표인 로케일에서도 좌표가 깨지지 않아야 한다
        try {
            val api = PlaceSearchApi(key = "k", fetch = { url -> asked = url; parkPoi })
            val found = api.search("여의도 공원", GeoPoint(37.5267, 126.9238), "ko")!!

            assertEquals(listOf(StoryPlace("여의도공원", "서울특별시 영등포구 여의공원로", 37.5261, 126.9225)), found)
            assertTrue(asked, "/geocoding/%EC%97%AC%EC%9D%98%EB%8F%84%20%EA%B3%B5%EC%9B%90.json" in asked)
            // poi 를 types 에 적지 않으면 MapTiler 는 역 · 공원 이름을 돌려주지 않는다
            assertTrue(asked, "&types=poi," in asked)
            assertTrue(asked, "&proximity=126.923800,37.526700" in asked)
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test fun `연결이 안 되면 없는 장소와 섞지 않는다`() = runBlocking {
        val offline = PlaceSearch(PlaceSearchApi(key = "k", fetch = { null }), platform = null, language = { "ko" })
        assertEquals(PlaceSearchResult.Offline, offline.search("한강", null))

        val empty = PlaceSearch(PlaceSearchApi(key = "k", fetch = { """{"features":[]}""" }), platform = null, language = { "ko" })
        assertEquals(PlaceSearchResult.Found(emptyList()), empty.search("달빛러닝공원", null))
    }

    @Test fun `두 검색원의 같은 장소는 한 번만 보이고 순서는 관련도 그대로다`() = runBlocking {
        val device = object : com.stepup.android.data.repo.PlatformGeocoder {
            override suspend fun search(query: String, near: GeoPoint?, limit: Int) = listOf(
                StoryPlace("여의도공원", "", 37.52615, 126.92255), // 같은 공원, 좌표가 몇 m 다르다
                StoryPlace("여의도공원 문화의마당", "", 37.5255, 126.9236),
            )
        }
        val search = PlaceSearch(PlaceSearchApi(key = "k", fetch = { parkPoi }), platform = device, language = { "ko" })
        val found = (search.search("여의도공원", GeoPoint(37.5, 126.9)) as PlaceSearchResult.Found).places
        assertEquals(listOf("여의도공원", "여의도공원 문화의마당"), found.map { it.name })
    }

    @Test fun `지도에서 누른 자리에는 곁의 시설 이름만 붙이고 좌표는 누른 자리 그대로다`() = runBlocking {
        val tapped = GeoPoint(37.5263, 126.9228) // 여의도공원 POI 에서 약 30m
        val near = PlaceSearchApi(key = "k", fetch = { url -> if ("types=poi" in url) parkPoi else null })
        val named = near.nameAt(tapped, "ko")!!
        assertEquals("여의도공원", named.name)
        assertEquals(tapped, named.point)

        // 시설이 멀면(약 1.1km) 그 이름을 붙이지 않고 동네 이름을 쓴다. 국도 이름은 쓰지 않는다
        val far = GeoPoint(37.5360, 126.9300)
        val reverse = """{"features":[
            {"id":"road.1","text":"대한민국 국도 제46호선","place_type":["road"],"center":[126.93,37.536]},
            {"id":"place.2","text":"여의도동","place_type":["place"],"center":[126.9238,37.5267],
             "context":[{"id":"county.4","text":"영등포구"},{"id":"region.5","text":"서울특별시"}]}]}"""
        val api = PlaceSearchApi(key = "k", fetch = { url -> if ("types=poi" in url) parkPoi else reverse })
        val fallback = api.nameAt(far, "ko")!!
        assertEquals("여의도동", fallback.name)
        assertEquals("서울특별시 영등포구", fallback.address)
        assertEquals(far, fallback.point)

        // 이름을 못 찾으면 좌표를 이름처럼 적지 않는다
        assertNull(PlaceSearchApi(key = "k", fetch = { """{"features":[]}""" }).nameAt(far, "ko"))
    }

    @Test fun `목록 위 동네 이름은 좁은 단위부터 고른다`() = runBlocking {
        val reverse = """{"features":[
            {"id":"road.1","text":"여의대로","place_type":["road"],"center":[126.93,37.53]},
            {"id":"county.4","text":"영등포구","place_type":["county"],"center":[126.9,37.52]},
            {"id":"place.2","text":"여의도동","place_type":["place"],"center":[126.9238,37.5267]}]}"""
        assertEquals("여의도동", PlaceSearchApi(key = "k", fetch = { reverse }).areaName(GeoPoint(37.53, 126.93), "ko"))
    }

    @Test fun `크루 활동 지역의 좌표는 폰이 있던 자리가 아니라 동네 중심점이다`() = runBlocking {
        val reverse = """{"features":[
            {"id":"poi.9","text":"편의점","place_type":["poi"],"center":[126.930117,37.530341]},
            {"id":"place.2","text":"여의도동","place_type":["place"],"center":[126.9238,37.5267]}]}"""
        val here = GeoPoint(37.530341, 126.930117)
        assertEquals(StoryPlace("여의도동", "", 37.5267, 126.9238), PlaceSearchApi(key = "k", fetch = { reverse }).area(here, "ko"))
        // 동네 중심점을 모르면 약 1km 크기로 뭉갠다(서버 crew_coarse 와 같은 반올림)
        val noCenter = """{"features":[{"id":"place.2","text":"여의도동","place_type":["place"]}]}"""
        assertEquals(StoryPlace("여의도동", "", 37.53, 126.93), PlaceSearchApi(key = "k", fetch = { noCenter }).area(here, "ko"))
        assertEquals(GeoPoint(37.55, 126.96), GeoPoint(37.545, 126.955).coarse())
        assertNull(PlaceSearchApi(key = "k", fetch = { """{"features":[]}""" }).area(here, "ko"))
    }

    @Test fun `지도 키가 없으면 묻지 않는다`() = runBlocking {
        var asked = false
        val api = PlaceSearchApi(key = "", fetch = { asked = true; parkPoi })
        assertEquals(emptyList<StoryPlace>(), api.search("여의도", null, "ko"))
        assertNull(api.nameAt(GeoPoint(37.5, 126.9), "ko"))
        assertTrue(!asked)
    }
}
