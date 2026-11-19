package com.stepup.android

import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.CrewRunApi
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.repo.CrewBeatStore
import com.stepup.android.data.repo.CrewRunRepository
import com.stepup.android.data.repo.PartyPhase
import com.stepup.android.data.repo.PartyState
import com.stepup.android.domain.CrewAloneGrace
import com.stepup.android.domain.CrewRunLive
import com.stepup.android.service.RunGroup
import com.stepup.android.service.WalkSessionState
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 크루 러닝 상태 알림(CrewRunRepository.startBeats)의 차례 — 2026-11-18 함께 뛰기 점검.
 *
 * 마지막 한 사람이 되면 바로 멈추지 않고 알린 뒤 서버가 적립하는 데까지 기록하고 멈춘다 · "끝냄"은 달리는 모습을 보지 못한 러닝도,
 * 닿지 못했으면 인터넷이 다시 될 때 다시 보낸다 · 공용 체력이 0 이던 동안의 걸음은 화면이 아니라 여기서 센다.
 * 시계는 검사가 쥔다(부팅 기준 시계 자리). 일은 실제로 돌므로 일어나길 기다렸다 본다([await]).
 */
class CrewRunBeatsTest {

    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun stop() {
        scopes.forEach { it.cancel() }
    }

    /** 서버 흉내 — 상태 알림(crew_run_beat)마다 보낸 상태를 적고 [answer] 를 돌려준다 */
    private class Http(private val answer: (state: String) -> HttpResponse) : HttpPoster {
        val states = CopyOnWriteArrayList<String>()

        override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
            if (!url.endsWith("/rpc/crew_run_beat")) return HttpResponse(404, "{}")
            val state = Regex("\"p_state\":\"(\\w+)\"").find(body)?.groupValues?.get(1).orEmpty()
            states += state
            return answer(state)
        }

        override suspend fun get(url: String, headers: Map<String, String>) = HttpResponse(404, "{}")

        fun count(state: String) = states.count { it == state }
    }

    private class LoggedIn : AuthSessionStore {
        override suspend fun load() = AuthSession(
            accessToken = "token", refreshToken = "refresh", expiresIn = 3600, expiresAt = 9_999_999_999L,
            user = AuthUser(id = "me", isAnonymous = false),
        )

        override suspend fun save(session: AuthSession) = Unit
        override suspend fun clear() = Unit
    }

    /** 앱이 꺼졌다 켜져도 남는 자리 — 검사에서는 메모리에 */
    private class MemoryStore : CrewBeatStore {
        private val values = ConcurrentHashMap<String, Long>()
        override fun get(key: String): Long? = values[key]
        override fun put(vararg values: Pair<String, Long?>) {
            values.forEach { (key, value) -> if (value == null) this.values.remove(key) else this.values[key] = value }
        }
    }

    private val clock = AtomicLong(1_000_000L)

    private fun repo(http: Http) = CrewRunRepository(
        api = CrewRunApi(
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
        ),
        owner = { "account:me" },
        elapsedMs = { clock.get() },
    )

    private fun ok(body: String) = HttpResponse(200, body)

    /** crew_run_beat 의 답 — 서버 시각 10:30:00 */
    private fun live(
        state: String = "RUNNING",
        terminal: String? = null,
        earning: String = "ACCRUING",
        rewardUntil: String? = null,
    ) = """{"crew_run":true,"server_now":"2026-11-18T10:30:00+00:00","party_id":5,"crew_id":"c1","day":"2026-11-18",
        "started_at":"2026-11-18T10:00:06+00:00","state":"$state","terminal_reason":${terminal?.let { "\"$it\"" } ?: "null"},
        "contribution":136,"consumed":11.25,"active_sec":135,"participants":2,"not_ended":${if (state == "ENDED") 0 else 2},
        "room_active_runners":1,
        "pool":{"day":"2026-11-18","total":816,"remaining":612,"consumed":204,"contributors":2,"active_runners":2,
                "exhausted":${earning == "POOL_EMPTY"},"low":false,"version":4},
        "bonus":{"status":"CONFIRMED","count":2,"pct":0.05},"level":{"level":2,"pct":0.01},"earning_state":"$earning"
        ${rewardUntil?.let { ",\"ended_at\":\"2026-11-18T10:29:42+00:00\",\"reward_until\":\"$it\"" }.orEmpty()}}"""

    private val startedAt = 1_795_000_000_000L
    private fun running(steps: Int = 100) = WalkSessionState(
        isActive = true, startedAt = startedAt, steps = steps, partySize = 2, group = RunGroup.CREW, groupCrewId = "c1",
    )
    private fun finished() = WalkSessionState(lastRewardPoints = 0.0, lastStartedAt = startedAt, lastGroup = RunGroup.CREW, lastGroupSize = 2)
    private val room = PartyState(phase = PartyPhase.RUNNING, partyId = 5L, crewId = "c1")

    private class Seen {
        val alone = CopyOnWriteArrayList<Int>()
        val stops = AtomicInteger()
        val saved = AtomicInteger()
        val cleared = AtomicInteger()
        val pools = CopyOnWriteArrayList<Boolean>()
    }

    private fun start(
        repo: CrewRunRepository,
        run: MutableStateFlow<WalkSessionState>,
        store: CrewBeatStore = MemoryStore(),
        party: PartyState = room,
        back: MutableStateFlow<Int> = MutableStateFlow(0),
    ): Seen {
        val seen = Seen()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default).also { scopes += it }
        repo.startBeats(
            scope = scope, store = store, party = MutableStateFlow(party), run = run,
            onCrewPool = { _: Long, _: CrewRunLive, empty: Boolean -> seen.pools += empty },
            stopRun = { seen.stops.incrementAndGet() },
            onAlone = { seen.alone += it },
            onAloneSaved = { seen.saved.incrementAndGet() },
            onAloneCleared = { seen.cleared.incrementAndGet() },
            networkBack = back,
            tickMs = 5L,
        )
        return seen
    }

    private fun await(what: String, check: () -> Boolean) {
        val until = System.nanoTime() + 10_000_000_000L
        while (!check()) {
            if (System.nanoTime() > until) throw AssertionError("기다렸지만 일어나지 않았다: $what")
            Thread.sleep(5)
        }
    }

    /** 그사이 일어나면 안 되는 일을 보기 전에 일이 몇 바퀴 돌 시간을 준다 */
    private fun settle() = Thread.sleep(120)

    // ── 마지막 한 사람 ────────────────────────────────────────────────────

    @Test
    fun `마지막 한 사람이 되면 바로 멈추지 않고 알린 뒤 적립하는 데까지 기록하고 멈춘다`() {
        val http = Http { ok(live(state = "ENDED", terminal = "LAST_PARTICIPANT_END", earning = "ENDED", rewardUntil = "2026-11-18T10:30:42+00:00")) }
        val repo = repo(http)
        val run = MutableStateFlow(running())
        val seen = start(repo, run)

        await("마지막 한 사람 알림") { seen.alone.size == 1 }
        assertEquals("남은 초를 알린다", 42, seen.alone.single())
        assertEquals(CrewAloneGrace(startedAt, 1_000_000L + 42_000L), repo.alone.value)
        settle()
        assertEquals("알리자마자 멈추지 않는다", 0, seen.stops.get())

        clock.addAndGet(41_000L)
        settle()
        assertEquals("적립하는 데까지는 기록한다", 0, seen.stops.get())

        clock.addAndGet(1_000L)
        await("러닝을 멈춘다") { seen.stops.get() == 1 }
        settle()
        assertEquals("한 번만 알린다", 1, seen.alone.size)
        assertEquals("시계가 그대로면 다시 멈추라고 하지 않는다", 1, seen.stops.get())

        // 저장이 실패해 러닝이 남아 있으면 몇 초 뒤 다시 멈추라고 한다
        clock.addAndGet(CrewRunRepository.BEAT_MS)
        await("다시 멈춘다") { seen.stops.get() == 2 }

        // 러닝 서비스가 저장을 마쳤다 — 끝났다고 알리고 "끝냄"을 보낸다
        run.value = finished()
        await("끝났다는 알림") { seen.saved.get() == 1 }
        await("끝냄") { http.count("ENDED") == 1 }
        assertEquals(0, seen.cleared.get())
    }

    @Test
    fun `적립 끝이 이미 지났으면 남은 초를 알리지 않고 바로 멈춘다`() {
        // 한참 끊겼다 돌아왔다 — 서버 시각 10:30:00, 적립은 10:20:00 까지였다
        val http = Http { ok(live(state = "ENDED", terminal = "LAST_PARTICIPANT_END", earning = "ENDED", rewardUntil = "2026-11-18T10:20:00+00:00")) }
        val run = MutableStateFlow(running())
        val seen = start(repo(http), run)

        await("러닝을 멈춘다") { seen.stops.get() == 1 }
        assertTrue("지난 시간을 세어 알리지 않는다", seen.alone.isEmpty())
        run.value = finished()
        await("끝났다는 알림") { seen.saved.get() == 1 }
    }

    @Test
    fun `알린 뒤 사용자가 먼저 마치면 알림만 거두고 멈추라고 하지 않는다`() {
        val http = Http { ok(live(state = "ENDED", terminal = "LAST_PARTICIPANT_END", earning = "ENDED", rewardUntil = "2026-11-18T10:30:42+00:00")) }
        val run = MutableStateFlow(running())
        val seen = start(repo(http), run)

        await("마지막 한 사람 알림") { seen.alone.size == 1 }
        run.value = finished()
        await("알림을 거둔다") { seen.cleared.get() == 1 }
        settle()
        assertEquals(0, seen.stops.get())
        assertEquals(0, seen.saved.get())
    }

    @Test
    fun `함께 달리는 사람이 남아 있으면 알리지도 멈추지도 않는다`() {
        val http = Http { ok(live()) }
        val repo = repo(http)
        val seen = start(repo, MutableStateFlow(running()))

        await("상태 알림") { http.count("RUNNING") >= 1 }
        clock.addAndGet(120_000L)
        await("다음 상태 알림") { http.count("RUNNING") >= 2 }
        settle()
        assertTrue(seen.alone.isEmpty())
        assertEquals(0, seen.stops.get())
        assertEquals(null, repo.alone.value)
    }

    // ── 끝냄 ────────────────────────────────────────────────────────────

    @Test
    fun `달리는 모습을 보지 못한 러닝도 끝냄을 한 번 보낸다`() {
        // 앞 실행 — 달리는 동안 상태를 알렸다(방 번호가 적혔다). 그러다 앱이 죽었다
        val store = MemoryStore()
        val before = Http { ok(live()) }
        start(repo(before), MutableStateFlow(running()), store)
        await("앞 실행의 상태 알림") { before.count("RUNNING") >= 1 }
        scopes.forEach { it.cancel() }

        // 새 실행 — 되살리자마자 끝낸 러닝. 이 일은 달리는 모습을 본 적이 없고, 방 상태도 비어 있다
        val http = Http { ok(live(state = "ENDED", terminal = "USER_END", earning = "ENDED")) }
        start(repo(http), MutableStateFlow(finished()), store, party = PartyState())
        await("끝냄") { http.count("ENDED") == 1 }
        settle()
        assertEquals("닿은 끝냄은 다시 보내지 않는다", listOf("ENDED"), http.states.toList())
    }

    @Test
    fun `러닝 서비스가 끝났다고 알리면 끝냄을 보낸다 - 여러 번 알려도 한 번`() {
        val store = MemoryStore()
        val before = Http { ok(live()) }
        start(repo(before), MutableStateFlow(running()), store)
        await("앞 실행의 상태 알림") { before.count("RUNNING") >= 1 }
        scopes.forEach { it.cancel() }

        // 결과를 이미 닫아 방금 끝난 러닝이 화면 상태에 남아 있지 않다 — 서비스가 직접 알린다
        val http = Http { ok(live(state = "ENDED", terminal = "USER_END", earning = "ENDED")) }
        val repo = repo(http)
        start(repo, MutableStateFlow(WalkSessionState()), store, party = PartyState())
        repo.noteEnded(startedAt)
        repo.noteEnded(startedAt)
        await("끝냄") { http.count("ENDED") == 1 }
        settle()
        repo.noteEnded(startedAt)
        settle()
        assertEquals(1, http.count("ENDED"))
        // 상태를 알린 적 없는 러닝은 보낼 방이 없다
        repo.noteEnded(startedAt + 1)
        settle()
        assertEquals(1, http.count("ENDED"))
    }

    @Test
    fun `끝냄이 닿지 못했으면 인터넷이 다시 될 때 바로 다시 보낸다`() {
        var online = false
        val http = Http { state ->
            when {
                state != "ENDED" -> ok(live())
                online -> ok(live(state = "ENDED", terminal = "USER_END", earning = "ENDED"))
                else -> HttpResponse(0, "통신 실패")
            }
        }
        val run = MutableStateFlow(running())
        val back = MutableStateFlow(0)
        start(repo(http), run, back = back)
        await("상태 알림") { http.count("RUNNING") >= 1 }

        run.value = finished()
        await("닿지 못한 끝냄") { http.count("ENDED") == 1 }
        settle()
        assertEquals("다시 보내기는 몇 초 뒤부터", 1, http.count("ENDED"))

        online = true
        back.value = 1
        await("다시 보낸 끝냄") { http.count("ENDED") == 2 }
        back.value = 2
        settle()
        assertEquals("닿은 뒤에는 더 보내지 않는다", 2, http.count("ENDED"))
    }

    // ── 공용 체력이 0 이던 동안의 걸음 ────────────────────────────────────────

    @Test
    fun `공용 체력이 0 이던 동안의 걸음은 화면과 상관없이 여기서 센다`() {
        var earning = "ACCRUING"
        val http = Http { ok(live(earning = earning)) }
        val repo = repo(http)
        val run = MutableStateFlow(running(steps = 1_000))
        val seen = start(repo, run)

        await("처음 값") { repo.emptySteps.value?.startedAt == startedAt }
        assertFalse(repo.emptySteps.value!!.emptyNow)
        assertEquals(listOf(false), seen.pools.distinct())

        // 다 썼다 — 다음 상태 알림에서 안다
        earning = "POOL_EMPTY"
        clock.addAndGet(CrewRunRepository.BEAT_MS)
        await("비었다") { repo.emptySteps.value?.emptyNow == true }
        assertEquals(1_000, repo.emptySteps.value!!.emptyFrom)
        run.value = running(steps = 1_600)
        settle()
        assertEquals("빈 동안 걸은 걸음은 예상에 들지 않는다", 1_000, repo.emptySteps.value!!.earningSteps(run.value.steps))
        assertTrue("잠금화면 알림에도 알린다", seen.pools.contains(true))

        // 다른 방이 출발해 다시 채워졌다 — 그 뒤 걸음은 다시 센다
        earning = "ACCRUING"
        clock.addAndGet(CrewRunRepository.BEAT_MS)
        await("다시 채워졌다") { repo.emptySteps.value?.emptyNow == false }
        assertEquals(600, repo.emptySteps.value!!.excluded)
        assertEquals(1_400, repo.emptySteps.value!!.earningSteps(2_000))
    }

    @Test
    fun `앱이 꺼졌다 켜져도 그동안의 걸음을 잇는다`() {
        val store = MemoryStore()
        val before = Http { ok(live(earning = "POOL_EMPTY")) }
        val first = repo(before)
        start(first, MutableStateFlow(running(steps = 700)), store)
        await("비었다") { first.emptySteps.value?.emptyNow == true }
        scopes.forEach { it.cancel() }

        val again = repo(Http { ok(live(earning = "POOL_EMPTY")) })
        start(again, MutableStateFlow(WalkSessionState()), store, party = PartyState())
        assertEquals(700, again.emptySteps.value?.emptyFrom)
        assertEquals(startedAt, again.emptySteps.value?.startedAt)
    }

    @Test
    fun `서버가 이 방에 내 참여가 없다고 하면 그 러닝만 크루 러닝이 아니다`() {
        val http = Http { ok("""{"crew_run":false,"server_now":"2026-11-18T10:30:00+00:00"}""") }
        val repo = repo(http)
        assertEquals(0L, repo.notCrewRun.value)
        start(repo, MutableStateFlow(running()))
        await("서버의 답") { repo.notCrewRun.value == startedAt }
        assertEquals(null, repo.live.value)
    }
}
