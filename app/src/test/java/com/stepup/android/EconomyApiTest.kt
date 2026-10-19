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
                "draw_free" -> HttpResponse(400, """{"message":"무료 뽑기가 남아 있지 않습니다"}""")
                "premium_draw" -> HttpResponse(400, """{"message":"상급 뽑기가 남아 있지 않습니다"}""")
                "sneaker_upgrade" -> HttpResponse(400, """{"message":"최대 레벨입니다"}""")
                "boost_buy" -> HttpResponse(400, """{"message":"에너지가 이미 충분합니다"}""")
                "sneaker_repair" -> HttpResponse(0, "offline")
                else -> HttpResponse(400, """{"message":"다른 문제"}""")
            }
        }
        val api = api(http)
        assertEquals(EconomyOutcome.NoFreeDraws, api.drawFree().toEconomyOutcome())
        assertEquals(EconomyOutcome.NoPremiumDraws, api.drawPremium().toEconomyOutcome())
        assertEquals(EconomyOutcome.MaxLevel, api.upgrade(1).toEconomyOutcome())
        assertEquals(EconomyOutcome.EnergyFull, api.boostBuy("ENERGY_CELL").toEconomyOutcome())
        assertEquals(EconomyOutcome.Offline, api.repair(1).toEconomyOutcome())
        assertTrue(api.equip(1).toEconomyOutcome() is EconomyOutcome.Rejected)
    }

    @Test fun signedOutNeverCallsTheServer() = runBlocking {
        val http = Http { HttpResponse(200, "1") }
        assertEquals(EconomyOutcome.SignInRequired, api(http, signedIn = false).drawFree().toEconomyOutcome())
        assertEquals(EconomyOutcome.SignInRequired, api(http, signedIn = false).drawPremium().toEconomyOutcome())
        assertTrue(http.calls.isEmpty())
    }

    @Test fun dailyGoalGoesToTheServerSoTheGoalBonusUsesIt() = runBlocking {
        val http = Http { HttpResponse(200, "3000") }
        assertEquals(ServerResult.Ok(3000), api(http).setDailyGoal(3000))
        assertTrue(http.calls.any { it.first.endsWith("/rpc/profile_set_daily_goal") && it.second == """{"p_goal":3000}""" })
    }

    @Test fun drawStatusAndPremiumDrawGoThroughTheServer() = runBlocking {
        val http = Http { url ->
            when (url.substringAfterLast('/')) {
                "draw_status" -> HttpResponse(200, """[{"daily_left":2,"daily_total":3,"signup_left":10,"signup_granted":10,
                    "wallet_linked":true,"gift_on_link":0,"gift_left":9,"run_left":1,"genesis_left":0,
                    "run_progress_m":600.5,"run_step_m":1000,"run_today":1,"run_daily_cap":10,"chain_paused":false}]""")
                "premium_draw" -> HttpResponse(200, "91")
                else -> HttpResponse(404, "{}")
            }
        }
        val api = api(http)
        val status = (api.drawStatus() as ServerResult.Ok).value
        assertEquals(12, status.freeLeft)
        assertEquals(10, status.premiumLeft)
        assertEquals(600.5, status.runProgressMeters, 0.0)
        assertTrue(status.canDraw(com.stepup.android.domain.DrawKind.PREMIUM))
        assertEquals(ServerResult.Ok(91L), api.drawPremium())
        assertTrue(http.calls.any { it.first.endsWith("/rpc/premium_draw") && it.second == "{}" })
        assertTrue("no paid draw is ever called", http.calls.none { it.first.endsWith("/rpc/draw_paid") })
    }

    @Test fun premiumRefusalsBecomeTheirOwnMessages() = runBlocking {
        val replies = mapOf(
            "먼저 지갑을 연결해 주세요" to EconomyOutcome.WalletRequired,
            "오늘 발행 한도가 찼습니다. 내일 다시 해 주세요" to EconomyOutcome.MintLimitReached,
            "지금은 체인 작업을 잠시 멈췄습니다" to EconomyOutcome.ChainPaused,
        )
        for ((reason, expected) in replies) {
            val http = Http { HttpResponse(400, """{"message":"$reason"}""") }
            assertEquals(reason, expected, api(http).drawPremium().toEconomyOutcome())
        }
    }

    // ── 수리는 견적에 보인 만큼만(p_points) ─────────────────────────────

    @Test fun repairSendsTheQuotedPoints() = runBlocking {
        val http = Http { HttpResponse(200, "100") }
        val api = api(http)
        assertEquals(ServerResult.Ok(100.0), api.repair(7, points = 30, request = "0057a000-0000-0000-0000-000000000001"))
        assertEquals(ServerResult.Ok(100.0), api.repair(8))
        val bodies = http.calls.filter { it.first.endsWith("/rpc/sneaker_repair") }.map { it.second }
        assertEquals(
            listOf("""{"p_id":7,"p_points":30,"p_request":"0057a000-0000-0000-0000-000000000001"}""", """{"p_id":8}"""),
            bodies,
        )
    }

    // ── 목표 보너스는 러닝을 시작한 날로(0068) ───────────────────────────

    @Test fun goalClaimDaySendsTheDay() = runBlocking {
        val http = Http { url -> if (url.endsWith("/rpc/goal_claim_day")) HttpResponse(200, "2.75") else HttpResponse(404, "{}") }
        assertEquals(ServerResult.Ok(2.75), api(http).goalClaimDay(java.time.LocalDate.of(2026, 10, 18)))
        assertEquals(listOf("""{"p_day":"2026-10-18"}"""), http.calls.filter { it.first.endsWith("/rpc/goal_claim_day") }.map { it.second })
        assertTrue("새 함수가 있으면 오늘 보너스를 따로 부르지 않는다", http.calls.none { it.first.endsWith("/rpc/goal_claim") })
    }

    @Test fun goalClaimDayFallsBackToTodayOnAnOlderServer() = runBlocking {
        // 서버에 아직 0068 이 없다 — PostgREST 는 404 PGRST202 로 답한다
        val http = Http { url ->
            when (url.substringAfterLast('/')) {
                "goal_claim_day" -> HttpResponse(
                    404,
                    """{"code":"PGRST202","details":"Searched for the function public.goal_claim_day with parameter p_day",""" +
                        """"hint":"Perhaps you meant to call the function public.goal_claim",""" +
                        """"message":"Could not find the function public.goal_claim_day(p_day) in the schema cache"}""",
                )
                "goal_claim" -> HttpResponse(200, "2.5")
                else -> HttpResponse(404, "{}")
            }
        }
        assertEquals(ServerResult.Ok(2.5), api(http).goalClaimDay(java.time.LocalDate.of(2026, 10, 18)))
        assertTrue(http.calls.any { it.first.endsWith("/rpc/goal_claim") && it.second == "{}" })
    }

    @Test fun goalClaimDayKeepsOtherRefusals() = runBlocking {
        val http = Http { url ->
            when (url.substringAfterLast('/')) {
                "goal_claim_day" -> HttpResponse(400, """{"code":"23514","message":"아직 목표를 채우지 않았습니다"}""")
                else -> HttpResponse(200, "2.5")
            }
        }
        assertEquals(
            ServerResult.Rejected("아직 목표를 채우지 않았습니다"),
            api(http).goalClaimDay(java.time.LocalDate.of(2026, 10, 18)),
        )
        assertTrue("다른 거절은 오늘 보너스로 바꾸지 않는다", http.calls.none { it.first.endsWith("/rpc/goal_claim") })
    }

    @Test fun goalDaysAreTheKoreanStartDaysOldestFirst() {
        val seoul = java.time.ZoneId.of("Asia/Seoul")
        fun kst(at: String) = java.time.LocalDateTime.parse(at).atZone(seoul).toInstant().toEpochMilli()
        val days = EconomyApi.goalDays(
            listOf(
                kst("2026-10-18T10:00"),
                // 자정을 넘긴 러닝 — 시작한 날(10-17)의 목표에 든다
                kst("2026-10-17T23:30"),
                kst("2026-10-18T00:10"),
                kst("2026-10-16T08:00"),
            ),
        )
        assertEquals(
            listOf(java.time.LocalDate.of(2026, 10, 16), java.time.LocalDate.of(2026, 10, 17), java.time.LocalDate.of(2026, 10, 18)),
            days,
        )
        // 폰 시간대와 상관없이 한국 날짜로 — 2026-10-17T15:30Z 는 한국 10-18 00:30
        assertEquals(
            listOf(java.time.LocalDate.of(2026, 10, 18)),
            EconomyApi.goalDays(listOf(java.time.Instant.parse("2026-10-17T15:30:00Z").toEpochMilli())),
        )
        assertTrue(EconomyApi.goalDays(emptyList()).isEmpty())
    }
}
