package com.stepup.android

import com.stepup.android.data.local.RewardTotals
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
import com.stepup.android.data.remote.TokenResult
import com.stepup.android.data.repo.CoalescedRun
import com.stepup.android.data.repo.EconomyLoad
import com.stepup.android.data.repo.EconomySyncState
import com.stepup.android.data.repo.economyLoadOf
import com.stepup.android.data.repo.emptyCopyLoad
import com.stepup.android.ui.screens.access.BetaCodesView
import com.stepup.android.ui.screens.access.betaCodeDay
import com.stepup.android.ui.screens.access.betaCodesView
import com.stepup.android.ui.screens.items.OwnedLoad
import com.stepup.android.ui.screens.items.ownedLoadOf
import com.stepup.android.ui.screens.rewards.TotalsLoad
import com.stepup.android.ui.screens.rewards.totalsLoadOf
import com.stepup.android.ui.shouldReturnToSignIn
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 첫 동기화 전 · 끊김을 확정 값처럼 보이지 않게(2026-11-04) — 경제 사본 상태 · 동기화 함께 기다리기 ·
 * 죽은 refresh token 기억 · 401 한 번 다시 보내기 · 베타 코드 날짜.
 */
class SyncRecoveryTest {

    // ── 경제 사본 상태 ──────────────────────────────────────────────────

    @Test fun `서버 경제면 첫 동기화 전 빈 사본은 확정이 아니다`() {
        assertEquals(EconomyLoad.LOADING, economyLoadOf(true, 0L, EconomySyncState.UNKNOWN, running = false))
        assertEquals(EconomyLoad.LOADING, economyLoadOf(true, 0L, EconomySyncState.UNKNOWN, running = true))
        assertEquals(EconomyLoad.UNREACHABLE, economyLoadOf(true, 0L, EconomySyncState.OFFLINE, running = false))
        // 로그아웃 뒤 다시 로그인해 맞추는 중 — 지난 SIGNED_OUT 보다 기다림이 먼저
        assertEquals(EconomyLoad.LOADING, economyLoadOf(true, 0L, EconomySyncState.SIGNED_OUT, running = true))
        assertEquals(EconomyLoad.READY, economyLoadOf(true, 5L, EconomySyncState.OFFLINE, running = false))
        assertEquals(EconomyLoad.READY, economyLoadOf(false, 0L, EconomySyncState.OFFLINE, running = false))
    }

    @Test fun `비어 있지 않은 사본은 늘 보이고 빈 사본만 기다린다`() {
        assertEquals(EconomyLoad.READY, emptyCopyLoad(empty = false, load = EconomyLoad.UNREACHABLE))
        assertEquals(EconomyLoad.UNREACHABLE, emptyCopyLoad(empty = true, load = EconomyLoad.UNREACHABLE))

        assertEquals(OwnedLoad.Loading, ownedLoadOf(emptyList(), EconomyLoad.LOADING))
        assertEquals(OwnedLoad.Failed, ownedLoadOf(emptyList(), EconomyLoad.UNREACHABLE))
        assertEquals(OwnedLoad.Ready(emptyList()), ownedLoadOf(emptyList(), EconomyLoad.READY))

        val zero = RewardTotals(0.0, 0.0, 0.0)
        assertEquals(TotalsLoad.Loading, totalsLoadOf(zero, EconomyLoad.LOADING))
        assertEquals(TotalsLoad.Failed, totalsLoadOf(zero, EconomyLoad.UNREACHABLE))
        assertEquals(TotalsLoad.Ready(zero), totalsLoadOf(zero, EconomyLoad.READY))
        val some = RewardTotals(12.5, 20.0, 7.5)
        assertEquals(TotalsLoad.Ready(some), totalsLoadOf(some, EconomyLoad.LOADING))
    }

    // ── 동기화 함께 기다리기 ────────────────────────────────────────────

    @Test fun `도는 동안 부른 사람들은 다음 한 번을 함께 기다린다`() = runBlocking {
        val runs = CoalescedRun<Int>()
        var started = 0
        val gate = CompletableDeferred<Unit>()
        val first = async(start = CoroutineStart.UNDISPATCHED) { runs.run { started++; gate.await(); started } }
        assertTrue(runs.running.value)
        // 앞 실행이 도는 중 — 둘이 부르면 다음 실행 하나를 함께
        val second = async(start = CoroutineStart.UNDISPATCHED) { runs.run { started++; started } }
        val third = async(start = CoroutineStart.UNDISPATCHED) { runs.run { started++; started } }
        gate.complete(Unit)
        assertEquals(1, first.await())
        assertEquals(2, second.await())
        assertEquals("셋째는 둘째와 같은 실행을 받는다", 2, third.await())
        assertEquals(2, started)
        assertFalse(runs.running.value)
    }

    @Test fun `함께 기다리던 실행을 세운 쪽이 취소되면 다시 줄 선다`() = runBlocking {
        val runs = CoalescedRun<Int>()
        var started = 0
        val gate = CompletableDeferred<Unit>()
        val first = async(start = CoroutineStart.UNDISPATCHED) { runs.run { started++; gate.await(); started } }
        val owner = async(start = CoroutineStart.UNDISPATCHED) { runs.run { started++; started } }
        val joiner = async(start = CoroutineStart.UNDISPATCHED) { runs.run { started++; started } }
        owner.cancel()
        gate.complete(Unit)
        assertEquals(1, first.await())
        assertEquals("함께 기다리던 사람은 결과 없이 끝나지 않는다", 2, joiner.await())
        assertEquals(2, started)
    }

    // ── 죽은 refresh token · 401 ────────────────────────────────────────

    private class Store(var value: AuthSession?) : AuthSessionStore {
        override suspend fun load() = value
        override suspend fun save(session: AuthSession) { value = session }
        override suspend fun clear() { value = null }
    }

    private class Http(private val reply: (String, Int) -> HttpResponse) : HttpPoster {
        val calls = mutableListOf<String>()
        override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
            calls += url
            return reply(url, calls.count { it == url })
        }
        override suspend fun get(url: String, headers: Map<String, String>) = post(url, "", headers)
    }

    private fun session(access: String, refresh: String, expiresAt: Long) = AuthSession(
        accessToken = access, refreshToken = refresh, expiresIn = 3600, expiresAt = expiresAt,
        user = AuthUser(id = "me", isAnonymous = false),
    )

    private fun sessionJson(access: String, refresh: String) = """
        {"access_token":"$access","refresh_token":"$refresh","expires_in":3600,
         "user":{"id":"me","is_anonymous":false}}
    """.trimIndent()

    @Test fun `거절된 refresh token 은 다시 보내지 않고 다시 로그인하면 풀린다`() = runBlocking {
        val store = Store(session("old", "dead", expiresAt = 0L))
        val http = Http { url, _ ->
            if (url.contains("grant_type=refresh_token")) HttpResponse(400, """{"msg":"Invalid Refresh Token"}""")
            else HttpResponse(200, sessionJson("new", "fresh"))
        }
        val sessions = SessionHolder(SupabaseAuth("https://test.supabase.co", "k", http, now = { 1000L }), store, now = { 1000L })

        assertTrue(sessions.accessToken() is TokenResult.SignInRequired)
        assertTrue(sessions.signInRequired.value)
        assertTrue(sessions.accessToken() is TokenResult.SignInRequired)
        assertTrue(sessions.accessToken() is TokenResult.SignInRequired)
        assertEquals("죽은 토큰은 한 번만 보낸다", 1, http.calls.count { it.contains("grant_type=refresh_token") })

        assertTrue(sessions.signInWithGoogle("id-token") is TokenResult.Ok)
        assertFalse(sessions.signInRequired.value)
        assertEquals(TokenResult.Ok("new", "me"), sessions.accessToken())
    }

    @Test fun `401 이면 갱신한 출입증으로 한 번만 바로 다시 보낸다`() = runBlocking {
        val store = Store(session("stale", "refresh", expiresAt = 9_999_999_999L))
        val http = Http { url, n ->
            when {
                url.contains("grant_type=refresh_token") -> HttpResponse(200, sessionJson("fresh", "refresh2"))
                url.endsWith("/rpc/chain_check_in") -> if (n == 1) HttpResponse(401, "") else HttpResponse(200, "true")
                else -> HttpResponse(404, "{}")
            }
        }
        val sessions = SessionHolder(SupabaseAuth("https://test.supabase.co", "k", http, now = { 1000L }), store, now = { 1000L })
        val api = EconomyApi(StepUpServer("https://test.supabase.co", "k", sessions, http))

        assertEquals(ServerResult.Ok(Unit), api.checkIn())
        assertEquals(2, http.calls.count { it.endsWith("/rpc/chain_check_in") })
        assertEquals(1, http.calls.count { it.contains("grant_type=refresh_token") })
        assertEquals("fresh", store.value?.accessToken)
    }

    @Test fun `갱신한 출입증도 401 이면 다음 차례로 미룬다`() = runBlocking {
        val store = Store(session("stale", "refresh", expiresAt = 9_999_999_999L))
        val http = Http { url, _ ->
            if (url.contains("grant_type=refresh_token")) HttpResponse(200, sessionJson("fresh", "refresh2"))
            else HttpResponse(401, "")
        }
        val sessions = SessionHolder(SupabaseAuth("https://test.supabase.co", "k", http, now = { 1000L }), store, now = { 1000L })
        val api = EconomyApi(StepUpServer("https://test.supabase.co", "k", sessions, http))

        assertTrue(api.checkIn() is ServerResult.Retry)
        assertEquals("두 번까지만", 2, http.calls.count { it.endsWith("/rpc/chain_check_in") })
    }

    @Test fun `달리는 중에는 로그인 화면으로 보내지 않는다`() {
        assertTrue(shouldReturnToSignIn(refreshRejected = true, running = false))
        assertFalse(shouldReturnToSignIn(refreshRejected = true, running = true))
        assertFalse(shouldReturnToSignIn(refreshRejected = false, running = false))
    }

    // ── 베타 코드 화면 ──────────────────────────────────────────────────

    @Test fun `PostgREST 시각의 시차 표기를 읽는다`() {
        val day = DateTimeFormatter.ofPattern("M/d")
        val seoul = ZoneId.of("Asia/Seoul")
        assertEquals("11/4", betaCodeDay("2026-11-03T16:30:06.12+00:00", seoul, day))
        assertEquals("11/3", betaCodeDay("2026-11-03T07:30:06Z", seoul, day))
        assertEquals(null, betaCodeDay("not a time", seoul, day))
        assertEquals(null, betaCodeDay(null, seoul, day))
    }

    @Test fun `내 상태를 받지 못하면 만들 수 없다고 하지 않는다`() {
        assertEquals(BetaCodesView.LOADING, betaCodesView(loading = true, statusKnown = false, canIssue = false))
        assertEquals(BetaCodesView.OFFLINE, betaCodesView(loading = false, statusKnown = false, canIssue = false))
        assertEquals(BetaCodesView.NOT_ALLOWED, betaCodesView(loading = false, statusKnown = true, canIssue = false))
        assertEquals(BetaCodesView.ALLOWED, betaCodesView(loading = true, statusKnown = true, canIssue = true))
    }
}
