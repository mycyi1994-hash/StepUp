package com.stepup.android

import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.CrewHomeApi
import com.stepup.android.data.remote.CrewLevelRow
import com.stepup.android.data.remote.CrewStaminaRow
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.remote.serverJson
import com.stepup.android.data.repo.CrewHomeRepository
import com.stepup.android.data.repo.HomeOutcome
import com.stepup.android.domain.CrewLevelStep
import com.stepup.android.domain.CrewPool
import com.stepup.android.domain.CrewStaminaRules
import com.stepup.android.domain.RewardEconomy
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 크루 체력(서버 0069) — 서버 답 읽기(crew_stamina · crew_level 모양 그대로), 내 몫의 세 갈래, 숫자 · 막대 · 레벨 진행,
 * 읽기가 실패해도 들고 있던 값을 지키는 것, 크루 러닝의 예상 적립이 내 몫에서 멈추는 것.
 */
class CrewStaminaTest {

    private class Router : HttpPoster {
        val answers = mutableMapOf<String, HttpResponse>()

        override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse =
            answers[url.substringAfterLast('/')] ?: HttpResponse(500, "{}")

        override suspend fun get(url: String, headers: Map<String, String>) = post(url, "", headers)
    }

    private class LoggedIn : AuthSessionStore {
        override suspend fun load() = AuthSession(
            accessToken = "token", refreshToken = "refresh", expiresIn = 3600, expiresAt = 9_999_999_999L,
            user = AuthUser(id = "u-a", isAnonymous = false),
        )

        override suspend fun save(session: AuthSession) = Unit
        override suspend fun clear() = Unit
    }

    private fun server(http: HttpPoster) = StepUpServer(
        baseUrl = "https://test.supabase.co",
        apiKey = "sb_publishable_test",
        sessions = SessionHolder(auth = SupabaseAuth("https://test.supabase.co", "sb_publishable_test", http), store = LoggedIn(), now = { 1_000L }),
        http = http,
    )

    /** 서버 검사(schema_test 0069)가 찍은 답 그대로 — 넷이 나눠 받는 레벨 3 크루의 오늘 */
    private val staminaJson = """
        {"me": {"left": 20.0000, "used": 0.0000, "user_id": "u-a", "contributor": true, "contribution": 30.0000},
         "day": "2026-10-21", "max": 80.0000, "left": 80.0000, "used": 0.0000, "level": 3, "share": 20.0000, "run_km": 116.178,
         "members": [{"name": "체력장", "user_id": "u-a", "contribution": 30.0000},
                     {"name": "새내기", "user_id": "u-d", "contribution": 10.0000}],
         "crew_level": 3, "level_bonus": 20, "contributors": 4, "next_level_km": 250}
    """.trimIndent()

    private val levels = listOf(0.0, 30.0, 100.0, 250.0, 500.0, 900.0, 1500.0, 2300.0, 3300.0, 4500.0)
        .mapIndexed { i, km -> CrewLevelStep(i + 1, km, 10.0 * i) }

    @Test
    fun `서버 답을 그대로 읽는다 — 오늘 체력 · 내 몫 · 나눠 받는 크루원 · 레벨`() {
        val s = serverJson.decodeFromString<CrewStaminaRow>(staminaJson).toDomain("c1")!!
        assertEquals(LocalDate.of(2026, 10, 21), s.day)
        assertEquals(80.0, s.max, 0.0)
        assertEquals(4, s.contributors)
        assertEquals(20.0, s.share, 0.0)
        assertEquals(20.0, s.levelBonus, 0.0)
        assertTrue(s.me.contributor)
        assertEquals("u-a", s.me.userId)
        assertEquals(listOf("체력장", "새내기"), s.members.map { it.name })
        assertEquals(3, s.crewLevel)
        assertEquals(250.0, s.nextLevelKm!!, 0.0)
        assertEquals(CrewPool.CREW, s.pool)
        assertTrue(s.isOn(LocalDate.of(2026, 10, 21)))
    }

    @Test
    fun `레벨이 없는 새 크루 · 날짜를 읽지 못한 답`() {
        val fresh = serverJson.decodeFromString<CrewStaminaRow>(
            """{"day":"2026-10-21","max":0,"used":0,"left":0,"contributors":0,"share":0,"level":1,"level_bonus":0,
               "me":{"contributor":false,"contribution":0,"used":0,"left":0},"members":[],"crew_level":null,"run_km":0,"next_level_km":30}""",
        ).toDomain("c2")!!
        assertNull(fresh.crewLevel)
        assertEquals(CrewPool.PERSONAL, fresh.pool)
        assertNull(serverJson.decodeFromString<CrewStaminaRow>("""{"day":"","max":10}""").toDomain("c3"))
    }

    @Test
    fun `내 몫은 셋 — 남음 · 다 씀(걸음 하나 값보다 적게 남음) · 오늘 나눠 받지 않음`() {
        val base = serverJson.decodeFromString<CrewStaminaRow>(staminaJson).toDomain("c1")!!
        assertEquals(CrewPool.CREW, base.copy(me = base.me.copy(left = 0.002)).pool)
        // 서버 검사: 몫 16.6666 에서 9,999걸음(16.665)을 쓰고 0.0016 이 남았다 — 걸음 하나(1/600)도 못 받는다
        assertEquals(CrewPool.CREW_EMPTY, base.copy(me = base.me.copy(left = 0.0016)).pool)
        assertEquals(CrewPool.CREW_EMPTY, base.copy(me = base.me.copy(left = 0.0)).pool)
        assertEquals(CrewPool.PERSONAL, base.copy(me = base.me.copy(contributor = false, left = 0.0)).pool)
    }

    @Test
    fun `체력 숫자는 소수 한 자리에서 버리고 0 으로 끝나면 뗀다`() {
        assertEquals("80", CrewStaminaRules.amount(80.0))
        assertEquals("16.6", CrewStaminaRules.amount(16.6666))
        assertEquals("12.3", CrewStaminaRules.amount(12.35))
        assertEquals("0", CrewStaminaRules.amount(0.0016))
        assertEquals("0", CrewStaminaRules.amount(-3.0))
        assertEquals("0.7", CrewStaminaRules.amount(0.7))
        assertEquals("20", CrewStaminaRules.amount(20.0000))
    }

    @Test
    fun `막대 · 레벨 진행 · 다음 레벨까지`() {
        assertEquals(0.8f, CrewStaminaRules.fraction(64.0, 80.0), 0.0001f)
        assertEquals(0f, CrewStaminaRules.fraction(5.0, 0.0), 0f)
        assertEquals(1f, CrewStaminaRules.fraction(90.0, 80.0), 0f)
        // 레벨 3(100km) → 4(250km) 사이 116.178km
        assertEquals(((116.178 - 100) / 150).toFloat(), CrewStaminaRules.levelProgress(116.178, 3, 250.0, levels), 0.0001f)
        // 새 크루는 0km 에서 첫 기준(30km)까지
        assertEquals(0.5f, CrewStaminaRules.levelProgress(15.0, null, 30.0, levels), 0.0001f)
        assertEquals(1f, CrewStaminaRules.levelProgress(5000.0, 10, null, levels), 0f)
        assertEquals(133.822, CrewStaminaRules.kmToNext(116.178, 250.0)!!, 0.0001)
        assertNull(CrewStaminaRules.kmToNext(5000.0, null))
        assertEquals(0.0, CrewStaminaRules.levelBonus(null), 0.0)
        assertEquals(0.0, CrewStaminaRules.levelBonus(1), 0.0)
        assertEquals(20.0, CrewStaminaRules.levelBonus(3), 0.0)
    }

    @Test
    fun `크루 체력은 러닝을 시작한 한국 날짜의 것 — 자정 직후 시작은 그날`() {
        // 2026-10-20 15:30 UTC = 2026-10-21 00:30 한국
        assertEquals(LocalDate.of(2026, 10, 21), CrewStaminaRules.gameDay(1_792_510_200_000L))
        assertEquals(LocalDate.of(2026, 10, 20), CrewStaminaRules.gameDay(1_792_510_200_000L - 3_600_000L))
    }

    @Test
    fun `크루 러닝의 예상 적립은 내 몫에서 멈춘다 — 몫 1 이면 600걸음까지`() {
        val reward = RewardEconomy.sessionReward(
            walkedSteps = 2000, energyRemaining = 1.0, earningMultiplier = 1.0, energyEfficiency = 1.0,
            partyMultiplier = RewardEconomy.partyMultiplier(2), boostMultiplier = 1.0,
        )
        assertEquals(600, reward.rewardedSteps)
        val none = RewardEconomy.sessionReward(
            walkedSteps = 2000, energyRemaining = 0.0016, earningMultiplier = 1.0, energyEfficiency = 1.0,
            partyMultiplier = RewardEconomy.partyMultiplier(2), boostMultiplier = 1.0,
        )
        assertEquals(0, none.rewardedSteps)
    }

    @Test
    fun `레벨 안내 — 0069 서버는 기준을 주고, 예전 서버면 비어 있다`() {
        val now = serverJson.decodeFromString<CrewLevelRow>(
            """{"level":3,"week_km":12.5,"week_runners":2,"member_count":5,"run_km":116.178,"next_level_km":250,"level_bonus":20,
               "levels":[{"level":1,"km":0,"bonus":0},{"level":2,"km":30,"bonus":10}]}""",
        )
        assertEquals(116.178, now.runKm!!, 0.0)
        assertEquals(250.0, now.nextLevelKm!!, 0.0)
        assertEquals(2, now.levels.size)
        val old = serverJson.decodeFromString<CrewLevelRow>("""{"level":null,"week_km":0,"week_runners":0,"member_count":1}""")
        assertNull(old.runKm)
        assertTrue(old.levels.isEmpty())
    }

    @Test
    fun `읽기가 실패하면 들고 있던 체력을 그대로 둔다 · 다른 계정으로 바뀌면 지운다`() = runBlocking {
        val http = Router()
        var owner = "account:u-a"
        val repo = CrewHomeRepository(CrewHomeApi(server(http)), owner = { owner })
        http.answers["crew_stamina"] = HttpResponse(200, staminaJson)
        assertTrue(repo.loadStamina("c1") is HomeOutcome.Ok)
        assertEquals(80.0, repo.staminaNow("c1")!!.max, 0.0)

        // 0069 전 서버(함수 없음) · 끊김 — 값을 지우지 않는다
        http.answers["crew_stamina"] = HttpResponse(404, """{"code":"PGRST202","message":"Could not find the function public.crew_stamina"}""")
        assertTrue(repo.loadStamina("c1") is HomeOutcome.Failed)
        assertEquals(80.0, repo.staminaNow("c1")!!.max, 0.0)

        // 크루를 나갔다 — 그 크루의 값은 지운다
        http.answers["crew_stamina"] = HttpResponse(403, """{"message":"crew_not_member"}""")
        assertTrue(repo.loadStamina("c1") is HomeOutcome.Ended)
        assertNull(repo.staminaNow("c1"))

        // 다른 계정 — 앞사람의 체력을 보이지 않는다
        http.answers["crew_stamina"] = HttpResponse(200, staminaJson)
        repo.loadStamina("c1")
        owner = "account:u-b"
        http.answers["crew_stamina"] = HttpResponse(500, "{}")
        repo.loadStamina("c9")
        assertNull(repo.staminaNow("c1"))
    }
}
