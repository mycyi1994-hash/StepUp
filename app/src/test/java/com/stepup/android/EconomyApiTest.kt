package com.stepup.android

import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.EconomyApi
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.repo.EconomyOutcome
import com.stepup.android.data.repo.toEconomyOutcome
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EconomyApiTest {
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

    private fun api(http: Http, signedIn: Boolean = true): EconomyApi {
        val session = AuthSession(
            accessToken = "token", refreshToken = "refresh", expiresIn = 3600,
            expiresAt = 9_999_999_999L, user = AuthUser(id = "A", isAnonymous = false),
        )
        val holder = SessionHolder(SupabaseAuth("https://test.supabase.co", "test", http),
            Store(if (signedIn) session else null), now = { 1000L })
        return EconomyApi(StepUpServer("https://test.supabase.co", "test", holder, http))
    }

    @Test fun readsTheServerEconomyAndSneakers() = runBlocking {
        val http = Http { url ->
            when (url.substringAfterLast('/')) {
                "my_economy" -> HttpResponse(200, """[{"balance":1234.5,"energy_max":6,"energy_left":2.5,
                    "xp_booster_until":null,"streak_shield_until":null,"equipped_id":7,"game_day":"2026-09-25"}]""")
                "my_sneakers" -> HttpResponse(200, """[{"id":7,"faction":"WIND","rarity":"RARE","variant":1,"level":3,
                    "max_level":15,"efficiency_bps":520,"comfort_bps":410,"durability":72.5,"equipped":true,
                    "origin":"FREE_DRAW","chain_state":"APP","status":"OWNED","mint_number":42,"genesis_no":null,
                    "token_id":null,"km_run":3.2,"lock_km":50,"can_withdraw":false,"upgrade_cost":375,
                    "repair_cost_per_point":1.44}]""")
                else -> HttpResponse(404, "{}")
            }
        }
        val economy = (api(http).economy() as ServerResult.Ok).value
        assertEquals(1234.5, economy.balance, 0.0)
        assertEquals(2.5, economy.energyLeft, 0.0)
        val shoe = (api(http).sneakers() as ServerResult.Ok).value.single()
        assertEquals(520, shoe.efficiencyBps)
        assertEquals(72.5, shoe.durability, 0.0)
        assertEquals("FREE_DRAW", shoe.origin)
    }

    @Test fun writesGoThroughServerFunctionsWithTheShoeNumber() = runBlocking {
        val http = Http { url ->
            when (url.substringAfterLast('/')) {
                "sneaker_upgrade" -> HttpResponse(200, "4")
                "sneaker_repair" -> HttpResponse(200, "100")
                "draw_free" -> HttpResponse(200, "88")
                else -> HttpResponse(200, "null")
            }
        }
        val api = api(http)
        assertEquals(ServerResult.Ok(4), api.upgrade(7))
        assertEquals(ServerResult.Ok(100.0), api.repair(7))
        assertEquals(ServerResult.Ok(88L), api.drawFree())
        assertTrue(http.calls.any { it.first.endsWith("/rpc/sneaker_upgrade") && it.second == """{"p_id":7}""" })
    }

    @Test fun serverRefusalsBecomeScreenMessages() = runBlocking {
        val http = Http { url ->
            when (url.substringAfterLast('/')) {
                "draw_paid" -> HttpResponse(400, """{"message":"SUP가 부족합니다 (보유 10, 필요 500)"}""")
                "draw_free" -> HttpResponse(400, """{"message":"무료 뽑기가 남아 있지 않습니다"}""")
                "sneaker_upgrade" -> HttpResponse(400, """{"message":"최대 레벨입니다"}""")
                "boost_buy" -> HttpResponse(400, """{"message":"에너지가 이미 충분합니다"}""")
                "sneaker_repair" -> HttpResponse(0, "offline")
                else -> HttpResponse(400, """{"message":"다른 문제"}""")
            }
        }
        val api = api(http)
        assertEquals(EconomyOutcome.NotEnoughBalance, api.drawPaid().toEconomyOutcome())
        assertEquals(EconomyOutcome.NoFreeDraws, api.drawFree().toEconomyOutcome())
        assertEquals(EconomyOutcome.MaxLevel, api.upgrade(1).toEconomyOutcome())
        assertEquals(EconomyOutcome.EnergyFull, api.boostBuy("ENERGY_CELL").toEconomyOutcome())
        assertEquals(EconomyOutcome.Offline, api.repair(1).toEconomyOutcome())
        assertTrue(api.equip(1).toEconomyOutcome() is EconomyOutcome.Rejected)
    }

    @Test fun signedOutNeverCallsTheServer() = runBlocking {
        val http = Http { HttpResponse(200, "1") }
        assertEquals(EconomyOutcome.SignInRequired, api(http, signedIn = false).drawPaid().toEconomyOutcome())
        assertTrue(http.calls.isEmpty())
    }
}
