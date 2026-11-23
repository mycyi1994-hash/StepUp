package com.stepup.android

import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.RunnerBadgesResult
import com.stepup.android.data.remote.RunnerLevelApi
import com.stepup.android.data.remote.RunnerMeResult
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.repo.RunnerLevelRepository
import com.stepup.android.data.repo.RunnerSeenStore
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.RecordingOwner
import com.stepup.android.domain.RunnerBadges
import com.stepup.android.domain.RunnerLevel
import com.stepup.android.domain.RunnerLevelNotice
import com.stepup.android.domain.RunnerLevelNotices
import com.stepup.android.domain.RunnerSeen
import com.stepup.android.domain.ShoeRules
import com.stepup.android.domain.StatRule
import com.stepup.android.ui.screens.items.ForgeReward
import com.stepup.android.ui.screens.items.forgeRewards
import com.stepup.android.ui.screens.items.shoeLevelEnergy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 러너 레벨(서버 0121, docs/runner-level-contract-2026-11-21.md) — runner_me 읽기 · 막대 · 다음 레벨 에너지(예전 보장) · 배지 ·
 * 레벨이 올랐다는 알림 한 번 · 이름 옆 배지 묶어 묻기 · 신발 화면의 하루 에너지 줄. 값은 서버가 셈하고 여기서는 무엇을 보일지만 고른다.
 */
class RunnerLevelTest {

    // ── 읽기 ──

    @Test fun `runner_me 를 계약의 예 그대로 읽는다`() {
        val r = RunnerLevel.parse(SAMPLE)!!
        assertTrue(r.enabled)
        assertEquals(7, r.level)
        assertEquals(30, r.maxLevel)
        assertEquals(64_210L, r.steps)
        assertEquals(10_000L, r.levelSteps)
        assertEquals(60_000L, r.levelFrom)
        assertEquals(70_000L, r.nextAt)
        assertEquals(5_790L, r.stepsToNext)
        assertEquals(10.0, r.energyMax!!, 1e-9)
        assertEquals(36, r.energyMinutes)
        assertEquals(32, r.levelEnergyMinutes)
        assertEquals(34, r.nextEnergyMinutes)
        assertEquals(21, r.energyCapLevel)
        assertTrue(r.legacyFloor)
        assertEquals(0, r.badge)
        assertEquals(6, r.freeBoxes)
        assertEquals(1, r.giftBoxes)
        assertEquals(2, r.rewards.energyMinutesPerLevel)
        assertEquals(1, r.rewards.freeEveryLevel)
        assertEquals(5, r.rewards.giftEvery)
        assertEquals(listOf(10, 20, 30), r.rewards.badgeLevels)
        // 이 레벨 안에서 4,210 / 10,000
        assertEquals(0.421f, r.progress, 1e-4f)
        assertFalse(r.isMax)
    }

    @Test fun `예전 보장이 더 크면 다음 레벨에도 에너지가 늘지 않는다 — +2분을 말하지 않는다`() {
        // 보장 36분 > 다음 레벨 34분
        assertNull(RunnerLevel.parse(SAMPLE)!!.nextEnergyIfMore)
        // 보장 없이 레벨 에너지대로면 다음 레벨 분
        val plain = RunnerLevel.parse(
            """{"enabled":true,"level":12,"steps":115000,"energy_minutes":42,"level_energy_minutes":42,"next_energy_minutes":44,"legacy_floor":false}""",
        )!!
        assertEquals(44, plain.nextEnergyIfMore)
        // 하루 에너지 상한(Lv21) 뒤 — 다음 레벨도 같은 분이면 말하지 않는다
        val capped = RunnerLevel.parse("""{"level":23,"steps":225000,"energy_minutes":60,"next_energy_minutes":60}""")!!
        assertNull(capped.nextEnergyIfMore)
        // 분을 모르면 말하지 않는다
        assertNull(RunnerLevel.parse("""{"level":3,"steps":25000}""")!!.nextEnergyIfMore)
    }

    @Test fun `만렙은 다음 칸이 null — 막대 가득 · 남은 걸음 없음`() {
        val top = RunnerLevel.parse(
            """{"enabled":true,"level":30,"max_level":30,"steps":312000,"level_steps":10000,"level_from":290000,
               "next_at":null,"to_next":null,"energy_minutes":60,"next_energy_minutes":null,"badge":30,"boxes":{"free":29,"gift":6}}""",
        )!!
        assertTrue(top.isMax)
        assertEquals(1f, top.progress, 0f)
        assertNull(top.stepsToNext)
        assertNull(top.nextEnergyIfMore)
        assertEquals(30, top.badge)
    }

    @Test fun `빠진 칸은 계약의 규칙대로 — 레벨이 없으면 걸음으로, 배지는 레벨로`() {
        val r = RunnerLevel.parse("""{"steps":125500}""")!!
        assertTrue(r.enabled)
        assertEquals(13, r.level)
        assertEquals(120_000L, r.levelFrom)
        assertEquals(130_000L, r.nextAt)
        assertEquals(4_500L, r.stepsToNext)
        assertEquals(10, r.badge)
        assertEquals(0, r.freeBoxes)
        assertNull(r.energyMinutes)
        assertNull(r.rewards.energyMinutesPerLevel)
        assertEquals(RunnerLevel.DEFAULT_BADGE_LEVELS, r.rewards.badgeLevels)
        // 숫자가 글자 · 소수로 와도 읽는다
        val odd = RunnerLevel.parse("""{"level":"4","steps":"31000.0","boxes":{"free":3.0}}""")!!
        assertEquals(4, odd.level)
        assertEquals(31_000L, odd.steps)
        assertEquals(3, odd.freeBoxes)
        // 레벨은 1 ~ 만렙 안
        assertEquals(30, RunnerLevel.parse("""{"level":99}""")!!.level)
        assertEquals(1, RunnerLevel.parse("""{"level":0}""")!!.level)
    }

    @Test fun `읽지 못하는 글 · 레벨도 걸음도 없는 글은 보일 것이 없다`() {
        assertNull(RunnerLevel.parse(""))
        assertNull(RunnerLevel.parse("null"))
        assertNull(RunnerLevel.parse("[]"))
        assertNull(RunnerLevel.parse("not json"))
        assertNull(RunnerLevel.parse("""{"enabled":true}"""))
    }

    @Test fun `서버가 껐으면 enabled 가 false — 알림도 없다`() {
        val off = RunnerLevel.parse("""{"enabled":false,"level":7,"steps":64210}""")!!
        assertFalse(off.enabled)
        assertNull(RunnerLevelNotices.of(null, off))
    }

    @Test fun `배지 — 닿은 가장 큰 것`() {
        assertEquals(0, RunnerLevel.badgeOf(9))
        assertEquals(10, RunnerLevel.badgeOf(10))
        assertEquals(20, RunnerLevel.badgeOf(29))
        assertEquals(30, RunnerLevel.badgeOf(30))
        assertEquals(5, RunnerLevel.badgeOf(7, listOf(5, 15)))
    }

    // ── 이름 옆 배지 ──

    @Test fun `runner_badges 줄 배열을 읽는다 — 배지가 없는 줄은 뺀다`() {
        val map = RunnerBadges.parse(
            """[{"user_id":"$U1","level":12,"badge":10},{"user_id":"$U2","level":31,"badge":30},
                {"user_id":"$U3","level":5,"badge":0},{"user_id":"$U4","level":22}]""",
        )!!
        assertEquals(mapOf(U1 to 10, U2 to 30, U4 to 20), map)
        assertEquals(emptyMap<String, Int>(), RunnerBadges.parse("[]"))
        assertNull(RunnerBadges.parse("""{"user_id":"$U1"}"""))
        assertNull(RunnerBadges.parse("oops"))
    }

    @Test fun `사용자 id 는 uuid 모양만 묻는다`() {
        assertTrue(RunnerBadges.isUserId(U1))
        assertTrue(RunnerBadges.isUserId(U1.uppercase()))
        assertFalse(RunnerBadges.isUserId(""))
        assertFalse(RunnerBadges.isUserId("me"))
        assertFalse(RunnerBadges.isUserId("$U1\"]}"))
        assertEquals("""{"p_users":["$U1","$U2"]}""", RunnerLevelApi.badgesBody(listOf(U1, U2)))
    }

    // ── 알림 ──

    @Test fun `처음 보는 계정 — 지난 러닝으로 채운 레벨과 상자를 한 번에`() {
        val n = RunnerLevelNotices.of(null, RunnerLevel.parse(SAMPLE)!!) as RunnerLevelNotice.Welcome
        assertEquals(7, n.level)
        assertEquals(6, n.freeBoxes)
        assertEquals(1, n.giftBoxes)
        assertEquals(RunnerSeen(7, 6, 1, 36), n.snapshot)
        // 갓 시작한 사람(Lv1 · 상자 0)에게는 알리지 않는다
        assertNull(RunnerLevelNotices.of(null, RunnerLevel.parse("""{"level":1,"steps":800,"boxes":{"free":0,"gift":0}}""")!!))
    }

    @Test fun `레벨이 오르면 — 서버 상자 수가 늘어난 만큼 · 에너지는 실제로 늘었을 때만 · 새 배지`() {
        val now = RunnerLevel.parse(
            """{"level":10,"steps":91000,"energy_minutes":38,"badge":10,"boxes":{"free":9,"gift":2}}""",
        )!!
        val n = RunnerLevelNotices.of(RunnerSeen(9, 8, 1, 36), now) as RunnerLevelNotice.LevelUp
        assertEquals(9, n.from)
        assertEquals(10, n.to)
        assertEquals(36, n.energyFrom)
        assertEquals(38, n.energyTo)
        assertEquals(1, n.freeBoxes)
        assertEquals(1, n.giftBoxes)
        assertEquals(10, n.badge)
        assertEquals(RunnerSeen(10, 9, 2, 38), n.snapshot)

        // 예전 보장 때문에 에너지가 그대로면 에너지 줄이 없다
        val floor = RunnerLevel.parse("""{"level":8,"steps":71000,"energy_minutes":36,"legacy_floor":true,"boxes":{"free":7,"gift":1}}""")!!
        val f = RunnerLevelNotices.of(RunnerSeen(7, 6, 1, 36), floor) as RunnerLevelNotice.LevelUp
        assertNull(f.energyFrom)
        assertNull(f.energyTo)
        assertNull(f.badge)
        assertEquals(1, f.freeBoxes)
        assertEquals(0, f.giftBoxes)

        // 상자가 아직 안 늘었으면 0 — 지어내지 않는다
        val noBox = RunnerLevelNotices.of(RunnerSeen(7, 6, 1, 36), floor.copy(freeBoxes = 6)) as RunnerLevelNotice.LevelUp
        assertEquals(0, noBox.freeBoxes)
    }

    @Test fun `오르지 않았으면 알리지 않는다 — 같거나 내려갔을 때`() {
        val now = RunnerLevel.parse(SAMPLE)!!
        assertNull(RunnerLevelNotices.of(RunnerSeen(7, 6, 1, 36), now))
        assertNull(RunnerLevelNotices.of(RunnerSeen(8, 7, 1, 36), now))
    }

    @Test fun `본 값은 글 한 줄로 남기고 되살린다`() {
        val seen = RunnerSeen(12, 11, 2, 42)
        assertEquals(seen, RunnerSeen.decode(seen.encode()))
        assertEquals(RunnerSeen(3, 2, 0, null), RunnerSeen.decode(RunnerSeen(3, 2, 0, null).encode()))
        assertNull(RunnerSeen.decode(null))
        assertNull(RunnerSeen.decode(""))
        assertNull(RunnerSeen.decode("x,1,2,3"))
        assertNull(RunnerSeen.decode("1,2,3"))
    }

    // ── 서버 부르기 ──

    @Test fun `서버의 대답 — 함수가 없으면 Missing, 끊기면 Failed`() {
        assertTrue(RunnerLevelApi.meResultOf(ServerResult.Ok(SAMPLE)) is RunnerMeResult.Ok)
        assertEquals(RunnerMeResult.Failed, RunnerLevelApi.meResultOf(ServerResult.Ok("oops")))
        assertEquals(
            RunnerMeResult.Missing,
            RunnerLevelApi.meResultOf(ServerResult.Rejected("Could not find the function public.runner_me without parameters in the schema cache")),
        )
        assertEquals(RunnerMeResult.Failed, RunnerLevelApi.meResultOf(ServerResult.Rejected("permission denied")))
        assertEquals(RunnerMeResult.Failed, RunnerLevelApi.meResultOf(ServerResult.Retry("offline")))
        assertEquals(RunnerMeResult.Failed, RunnerLevelApi.meResultOf(ServerResult.SignInRequired("sign in")))
        assertEquals(RunnerBadgesResult.Missing, RunnerLevelApi.badgesResultOf(ServerResult.Rejected("PGRST202")))
        assertEquals(RunnerBadgesResult.Ok(mapOf(U1 to 10)), RunnerLevelApi.badgesResultOf(ServerResult.Ok("""[{"user_id":"$U1","badge":10}]""")))
    }

    @Test fun `runner_me · runner_badges 요청 — 0121 전 서버의 404 는 Missing`() = runBlocking {
        val ok = Http { url -> HttpResponse(200, if (url.endsWith("runner_me")) SAMPLE else """[{"user_id":"$U2","level":20,"badge":20}]""") }
        val me = api(ok).me()
        assertEquals(7, (me as RunnerMeResult.Ok).level.level)
        assertEquals("https://test.supabase.co/rest/v1/rpc/runner_me" to "{}", ok.calls.single())
        val badges = api(ok).badges(listOf(U1, "not-a-user", U2, U1))
        assertEquals(RunnerBadgesResult.Ok(mapOf(U2 to 20)), badges)
        assertEquals("https://test.supabase.co/rest/v1/rpc/runner_badges" to """{"p_users":["$U1","$U2"]}""", ok.calls.last())

        val missing = Http {
            HttpResponse(404, """{"code":"PGRST202","message":"Could not find the function public.runner_me without parameters in the schema cache"}""")
        }
        assertEquals(RunnerMeResult.Missing, api(missing).me())
        assertEquals(RunnerMeResult.Failed, api(Http { HttpResponse(503, "") }).me())
        assertEquals(RunnerMeResult.Failed, api(Http { HttpResponse(200, SAMPLE) }, signedIn = false).me())
        // 물을 사람이 없으면 부르지 않는다
        val quiet = Http { HttpResponse(200, "[]") }
        assertEquals(RunnerBadgesResult.Ok(emptyMap()), api(quiet).badges(listOf("x", "")))
        assertTrue(quiet.calls.isEmpty())
    }

    // ── 저장소 ──

    @Test fun `저장소 — 처음 본 계정은 알림 한 번, 닫으면 본 것으로 적고 다시 알리지 않는다`() = runBlocking {
        val store = MemoryStore()
        var reply: RunnerMeResult = RunnerMeResult.Ok(RunnerLevel.parse(SAMPLE)!!)
        val repo = RunnerLevelRepository(fetchMe = { reply }, fetchBadges = { RunnerBadgesResult.Ok(emptyMap()) }, store = store, owner = { OWNER })

        assertEquals(7, repo.refresh()!!.level)
        assertTrue(repo.notice.value is RunnerLevelNotice.Welcome)
        // 닫기 전에는 본 것으로 적지 않는다(앱이 꺼져도 다음에 다시)
        assertNull(store.seen[OWNER])
        repo.refresh()
        assertTrue(repo.notice.value is RunnerLevelNotice.Welcome)
        repo.consumeNotice()
        assertNull(repo.notice.value)
        assertEquals(RunnerSeen(7, 6, 1, 36), store.seen[OWNER])
        repo.refresh()
        assertNull(repo.notice.value)

        // 레벨이 오르면 한 번
        reply = RunnerMeResult.Ok(RunnerLevel.parse("""{"level":8,"steps":70500,"energy_minutes":36,"legacy_floor":true,"boxes":{"free":7,"gift":1}}""")!!)
        repo.refresh()
        val up = repo.notice.value as RunnerLevelNotice.LevelUp
        assertEquals(7 to 8, up.from to up.to)
        assertEquals(1, up.freeBoxes)

        // 끊기면 아는 값 그대로 · 서버가 껐으면 숨긴다 · 함수가 없으면 다시 묻지 않는다
        reply = RunnerMeResult.Failed
        assertEquals(8, repo.refresh()!!.level)
        reply = RunnerMeResult.Ok(RunnerLevel.parse("""{"enabled":false,"level":8,"steps":70500}""")!!)
        assertNull(repo.refresh())
        assertNull(repo.level.value)
        assertNull(repo.notice.value)
        reply = RunnerMeResult.Missing
        assertNull(repo.refresh())
        reply = RunnerMeResult.Ok(RunnerLevel.parse(SAMPLE)!!)
        assertNull(repo.refresh())
    }

    @Test fun `저장소 — 갓 시작한 계정은 알리지 않고 조용히 적는다 · 로그인 전에는 묻지 않는다`() = runBlocking {
        val store = MemoryStore()
        var asked = 0
        val fresh = RunnerLevelRepository(
            fetchMe = { asked++; RunnerMeResult.Ok(RunnerLevel.parse("""{"level":1,"steps":300,"energy_minutes":20}""")!!) },
            fetchBadges = { RunnerBadgesResult.Ok(emptyMap()) }, store = store, owner = { OWNER },
        )
        fresh.refresh()
        assertNull(fresh.notice.value)
        assertEquals(RunnerSeen(1, 0, 0, 20), store.seen[OWNER])

        val guest = RunnerLevelRepository(
            fetchMe = { asked++; RunnerMeResult.Ok(RunnerLevel.parse(SAMPLE)!!) },
            fetchBadges = { RunnerBadgesResult.Ok(emptyMap()) }, store = store, owner = { RecordingOwner.GUEST },
        )
        assertNull(guest.refresh())
        assertEquals(1, asked)
    }

    @Test fun `저장소 — 배지는 명단마다 한 번에 묻고 잠깐 기억한다`() = runBlocking {
        var clock = 0L
        val asked = mutableListOf<List<String>>()
        val repo = RunnerLevelRepository(
            fetchMe = { RunnerMeResult.Failed },
            fetchBadges = { ids -> asked += ids; RunnerBadgesResult.Ok(mapOf(U1 to 10).filterKeys { it in ids }) },
            owner = { OWNER }, clock = { clock },
        )
        assertEquals(mapOf(U1 to 10), repo.badges(listOf(U1, U2, "bad", U1)))
        assertEquals(listOf(listOf(U1, U2)), asked)
        // 기억한 동안은 다시 묻지 않는다 — 배지가 없는 사람도
        assertEquals(mapOf(U1 to 10), repo.badges(listOf(U2, U1)))
        assertEquals(1, asked.size)
        assertEquals(mapOf(U1 to 10), repo.cachedBadges(listOf(U1, U2, U3)))
        // 새 사람만 묻는다
        repo.badges(listOf(U1, U3))
        assertEquals(listOf(U3), asked.last())
        // 시간이 지나면 다시
        clock += RunnerLevelRepository.BADGE_TTL_MS
        repo.badges(listOf(U1))
        assertEquals(listOf(U1), asked.last())

        // 서버에 함수가 없으면 다시 묻지 않는다
        var calls = 0
        val old = RunnerLevelRepository(fetchMe = { RunnerMeResult.Failed }, fetchBadges = { calls++; RunnerBadgesResult.Missing }, owner = { OWNER })
        assertEquals(emptyMap<String, Int>(), old.badges(listOf(U1)))
        assertEquals(emptyMap<String, Int>(), old.badges(listOf(U2)))
        assertEquals(1, calls)
    }

    @Test fun `저장소 — 200명이 넘으면 나눠 묻는다`() = runBlocking {
        val sizes = mutableListOf<Int>()
        val repo = RunnerLevelRepository(
            fetchMe = { RunnerMeResult.Failed },
            fetchBadges = { ids -> sizes += ids.size; RunnerBadgesResult.Ok(emptyMap()) },
            owner = { OWNER },
        )
        val many = (0 until 250).map { i -> "00000000-0000-0000-0000-%012d".format(i) }
        repo.badges(many)
        assertEquals(listOf(200, 50), sizes)
    }

    // ── 신발 화면의 하루 에너지 ──

    @Test fun `러너 레벨이 하루 에너지를 정하면 신발 레벨로 에너지가 는다고 말하지 않는다`() {
        val legacy = ShoeRules.FALLBACK.copy(stats = StatRule.LEGACY)
        // 러너 레벨을 못 받았으면 예전 그대로(레벨마다 +2)
        assertEquals(2.0, shoeLevelEnergy(legacy, runnerEnergy = false), 0.0)
        assertTrue(forgeRewards(Rarity.RARE, 7, legacy).any { it is ForgeReward.Energy })
        // 러너 레벨이 켜졌으면 에너지 줄이 없다
        assertEquals(0.0, shoeLevelEnergy(legacy, runnerEnergy = true), 0.0)
        assertTrue(forgeRewards(Rarity.RARE, 7, legacy, runnerEnergy = true).none { it is ForgeReward.Energy })
        assertNotNull(forgeRewards(Rarity.RARE, 7, legacy, runnerEnergy = true).firstOrNull { it is ForgeReward.Points })
    }

    // ── 돕는 것 ──

    private class MemoryStore : RunnerSeenStore {
        val seen = mutableMapOf<String, RunnerSeen>()
        override suspend fun read(owner: String) = seen[owner]
        override suspend fun write(owner: String, seen: RunnerSeen) { this.seen[owner] = seen }
    }

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

    private fun api(http: Http, signedIn: Boolean = true): RunnerLevelApi {
        val session = AuthSession(
            accessToken = "token", refreshToken = "refresh", expiresIn = 3600,
            expiresAt = 9_999_999_999L, user = AuthUser(id = "A", isAnonymous = false),
        )
        val holder = SessionHolder(SupabaseAuth("https://test.supabase.co", "test", http),
            Store(if (signedIn) session else null), now = { 1000L })
        return RunnerLevelApi(StepUpServer("https://test.supabase.co", "test", holder, http))
    }

    private companion object {
        const val U1 = "11111111-1111-4111-8111-111111111111"
        const val U2 = "22222222-2222-4222-8222-222222222222"
        const val U3 = "33333333-3333-4333-8333-333333333333"
        const val U4 = "44444444-4444-4444-8444-444444444444"
        val OWNER = RecordingOwner.account("A")

        /** docs/runner-level-contract-2026-11-21.md 의 예 */
        const val SAMPLE = """{
  "enabled": true, "level": 7, "max_level": 30, "steps": 64210, "level_steps": 10000,
  "level_from": 60000, "next_at": 70000, "to_next": 5790,
  "energy_max": 10.0, "energy_minutes": 36, "level_energy_minutes": 32, "next_energy_minutes": 34,
  "energy_cap_level": 21, "legacy_floor": true, "badge": 0,
  "boxes": { "free": 6, "gift": 1 },
  "rewards": { "energy_minutes_per_level": 2, "free_every_level": 1, "gift_every": 5, "badge_levels": [10, 20, 30] }
}"""
    }
}
