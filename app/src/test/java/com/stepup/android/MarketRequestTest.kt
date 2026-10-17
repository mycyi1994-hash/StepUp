package com.stepup.android

import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.MarketApi
import com.stepup.android.data.remote.ModelKey
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.repo.RequestKeys
import com.stepup.android.data.repo.sendOnce
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 거래소 — 값이 오가는 넷(내놓기 · 즉시 구매 · 구매 입찰 · 즉시 판매)의 요청 번호(서버 0062).
 * 응답을 잃은 거래를 다시 보내면 같은 번호라 서버가 처음 결과를 돌려준다(구매 입찰이 두 번 묶이지 않는다).
 * 답을 받으면 다음 거래는 새 번호다.
 */
class MarketRequestTest {
    private class Store(var value: AuthSession?) : AuthSessionStore {
        override suspend fun load() = value
        override suspend fun save(session: AuthSession) { value = session }
        override suspend fun clear() { value = null }
    }

    private class Http(private val reply: (String) -> HttpResponse) : HttpPoster {
        val calls = mutableListOf<Pair<String, String>>()
        override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
            calls += url to body
            return reply(url)
        }
        override suspend fun get(url: String, headers: Map<String, String>) = post(url, "", headers)
    }

    private fun api(http: Http): MarketApi {
        val session = AuthSession(
            accessToken = "token", refreshToken = "refresh", expiresIn = 3600,
            expiresAt = 9_999_999_999L, user = AuthUser(id = "A", isAnonymous = false),
        )
        val holder = SessionHolder(SupabaseAuth("https://test.supabase.co", "test", http), Store(session), now = { 1000L })
        return MarketApi(StepUpServer("https://test.supabase.co", "test", holder, http))
    }

    /** 몸통에 실린 요청 번호 — 없으면 null */
    private fun requestOf(body: String): String? = Regex("\"p_request\":\"([^\"]+)\"").find(body)?.groupValues?.get(1)

    private val model = ModelKey("WIND", "RARE", 1)

    @Test fun `값이 오가는 넷은 요청 번호를 p_request 로 보낸다`() = runBlocking {
        val http = Http { HttpResponse(200, "5") }
        val api = api(http)
        api.list(7, 12.5, "k1")
        api.buyNow(8, "k2")
        api.bid(model, 3, 20.0, "k3")
        api.sellNow(7, 9, "k4")
        assertEquals(
            listOf(
                """{"p_sneaker_id":7,"p_price":12.5,"p_request":"k1"}""",
                """{"p_listing_id":8,"p_request":"k2"}""",
                """{"p_faction":"WIND","p_rarity":"RARE","p_variant":1,"p_min_level":3,"p_price":20.0,"p_request":"k3"}""",
                """{"p_sneaker_id":7,"p_bid_id":9,"p_request":"k4"}""",
            ),
            http.calls.map { it.second },
        )
        assertEquals(
            listOf("market_list", "market_buy_now", "market_bid", "market_sell_now"),
            http.calls.map { it.first.substringAfterLast('/') },
        )
    }

    @Test fun `번호 없이 부르면 예전 몸통 그대로 — 바로 체결된 null 도 성공이다`() = runBlocking {
        val http = Http { HttpResponse(200, "null") }
        val api = api(http)
        assertEquals(ServerResult.Ok(null), api.buyNow(8))
        assertEquals(ServerResult.Ok(null), api.bid(model, 3, 20.0))
        assertEquals(
            listOf(
                """{"p_listing_id":8}""",
                """{"p_faction":"WIND","p_rarity":"RARE","p_variant":1,"p_min_level":3,"p_price":20.0}""",
            ),
            http.calls.map { it.second },
        )
    }

    @Test fun `응답을 잃은 구매 입찰을 다시 보내면 같은 번호 — 답을 받은 뒤의 입찰은 새 번호`() = runBlocking {
        var online = false
        val http = Http { if (online) HttpResponse(200, "41") else HttpResponse(0, "통신 실패: Read timed out") }
        val api = api(http)
        val keys = RequestKeys()
        val action = "BID:WIND:RARE:1:3:20.0"

        val lost = keys.sendOnce(action) { key -> api.bid(model, 3, 20.0, key) }
        assertTrue(lost is ServerResult.Retry)
        online = true
        // 서버는 같은 번호에 처음 결과(입찰 번호 41)를 돌려준다 — 같은 값을 두 번 묶지 않는다
        assertEquals(ServerResult.Ok(41L), keys.sendOnce(action) { key -> api.bid(model, 3, 20.0, key) })
        keys.sendOnce(action) { key -> api.bid(model, 3, 20.0, key) }

        val requests = http.calls.map { requestOf(it.second) }
        assertEquals(3, requests.size)
        assertTrue(requests.all { it != null })
        assertEquals(requests[0], requests[1])
        assertNotEquals(requests[1], requests[2])
    }

    @Test fun `거절도 답이다 — 다음 거래는 새 번호로 보낸다`() = runBlocking {
        val http = Http { HttpResponse(400, """{"message":"이미 끝난 매물입니다"}""") }
        val api = api(http)
        val keys = RequestKeys()

        assertEquals(ServerResult.Rejected("이미 끝난 매물입니다"), keys.sendOnce("BUY:8") { key -> api.buyNow(8, key) })
        keys.sendOnce("BUY:8") { key -> api.buyNow(8, key) }

        val requests = http.calls.map { requestOf(it.second) }
        assertNotEquals(requests[0], requests[1])
    }
}
