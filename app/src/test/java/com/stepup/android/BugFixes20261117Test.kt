package com.stepup.android

import com.stepup.android.data.local.UploadState
import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.remote.toResult
import com.stepup.android.data.repo.EconomyOutcome
import com.stepup.android.domain.Faction
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.ServerStats
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.screens.items.PointsDraft
import com.stepup.android.ui.screens.items.PointsError
import com.stepup.android.ui.screens.items.PointsPhase
import com.stepup.android.ui.screens.items.lowDurabilityMinutes
import com.stepup.android.ui.screens.items.pointsAfterSend
import com.stepup.android.ui.screens.items.pointsBasisOf
import com.stepup.android.ui.screens.items.repairUrgent
import com.stepup.android.ui.screens.walk.pendingShoeWearByShoe
import com.stepup.android.ui.screens.walk.pendingShoeWearOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 2026-11-17 버그 점검 #1 · #10 · #14 · #15(ShoeDrawFlowTest) · #17 */
class BugFixes20261117Test {

    private fun shoe(id: Long = 1, now: Int, max: Int, equipped: Boolean = true) = Sneaker(
        id = id, faction = Faction.WIND, rarity = Rarity.COMMON, variant = 0, level = 1, mintNumber = 1,
        luck = 1.0, comfort = 1.0, durability = 100, equipped = equipped, acquiredAt = 0L, modelId = 1107,
    ).copy(
        server = ServerStats(
            origin = "DRAW", efficiencyBps = 0, comfortBps = 0, durabilityPts = if (max > 0) now * 100.0 / max else 100.0,
            maxLevel = 20, status = "OWNED", chainState = "APP", canWithdraw = false, upgradeCost = 0.0,
            repairCostPerPoint = 0.0, genesisNo = 0, durabilityNow = now, durabilityMax = max, durabilityBase = max,
        ),
    )

    // ── #1 가득 찬 새 일반 신발에 "약 9분만 보상" ──

    @Test fun `가득 찬 신발은 알리지도 수리를 앞세우지도 않는다`() {
        // 새 일반 신발 45 · 50 · 55 — 9 · 10 · 11분, 가득
        for (max in listOf(45, 50, 55)) {
            assertNull("max $max", lowDurabilityMinutes(shoe(now = max, max = max)))
            assertFalse("max $max", repairUrgent(shoe(now = max, max = max)))
        }
    }

    @Test fun `짧은 신발은 제 수명의 절반보다 짧을 때 알린다`() {
        // 일반 50(10분) — 기준은 5분. 6분(30) 은 아직, 4분(20) 이면 알린다
        assertNull(lowDurabilityMinutes(shoe(now = 30, max = 50)))
        assertFalse(repairUrgent(shoe(now = 30, max = 50)))
        assertEquals(4, lowDurabilityMinutes(shoe(now = 20, max = 50)))
        assertTrue(repairUrgent(shoe(now = 20, max = 50)))
        // 긴 신발(피니시 90분 = 450) — 기준은 10분 그대로
        assertNull(lowDurabilityMinutes(shoe(now = 60, max = 450)))
        assertEquals(9, lowDurabilityMinutes(shoe(now = 45, max = 450)))
        // 다 닳음 — 분 안내 대신 다 닳음으로, 수리는 앞세운다
        assertTrue(repairUrgent(shoe(now = 0, max = 50)))
        // 최대 내구도를 모르는 신발은 알리지 않는다
        assertNull(lowDurabilityMinutes(shoe(now = 0, max = 0)))
    }

    // ── #10 접속 코드 문(403 access_required)은 "나중에 다시" ──

    private class Store(var value: AuthSession?) : AuthSessionStore {
        override suspend fun load() = value
        override suspend fun save(session: AuthSession) { value = session }
        override suspend fun clear() { value = null }
    }

    private class Http(private val answer: HttpResponse) : HttpPoster {
        override suspend fun post(url: String, body: String, headers: Map<String, String>) = answer
        override suspend fun get(url: String, headers: Map<String, String>) = answer
    }

    private fun server(answer: HttpResponse): StepUpServer {
        val http = Http(answer)
        val session = AuthSession(
            accessToken = "token", refreshToken = "refresh", expiresIn = 3600,
            expiresAt = 9_999_999_999L, user = AuthUser(id = "A", isAnonymous = false),
        )
        val holder = SessionHolder(SupabaseAuth("https://test.supabase.co", "test", http), Store(session), now = { 1000L })
        return StepUpServer("https://test.supabase.co", "test", holder, http)
    }

    @Test fun `접속 코드 문에 막히면 거절이 아니라 나중에 다시 - 문을 다시 묻는다`() = runBlocking {
        val gate = HttpResponse(403, """{"code":"42501","message":"access_required"}""")
        val server = server(gate)
        var asked = 0
        server.onAccessRequired = { asked++ }
        val result = server.authed { gate }
        assertTrue("$result", result is ServerResult.Retry)
        assertEquals(1, asked)
        // 로그인 없이 · 로그인 자격으로 여는 길도 같다
        assertTrue(gate.toResult() is ServerResult.Retry)
        assertTrue(server.openPost("https://test.supabase.co/rest/v1/rpc/x", "{}") is ServerResult.Retry)
        assertEquals(2, asked)
    }

    @Test fun `다른 403 은 그대로 거절`() = runBlocking {
        val other = HttpResponse(403, """{"message":"크루장만 할 수 있습니다"}""")
        val server = server(other)
        var asked = 0
        server.onAccessRequired = { asked++ }
        assertEquals(ServerResult.Rejected("크루장만 할 수 있습니다"), server.authed { other })
        assertTrue(other.toResult() is ServerResult.Rejected)
        assertEquals(0, asked)
    }

    // ── #14 아직 안 올라간 러닝의 닳음은 그 러닝의 신발에서 ──

    private val now = 1_800_000_000_000L

    private fun row(startedAt: Long, minutes: Long, sneaker: Long = 0, crew: String = "", state: UploadState = UploadState.PENDING) =
        WalkSessionEntity(
            startedAt = startedAt, endedAt = startedAt + minutes * 60_000, steps = 1_000, durationSec = minutes * 60,
            distanceMeters = 1_000.0, calories = 50.0, pointsEarned = 0.0, recordingOwner = "user-A",
            crewId = crew, uploadState = state.name, sneakerServerId = sneaker,
        )

    @Test fun `닳음은 러닝 줄의 신발마다 - 모르는 줄만 신은 신발에서, 크루 러닝은 닳지 않는다`() {
        val rows = listOf(
            row(now - 60_000, minutes = 4, sneaker = 7), // A(7) 로 4분 — 20
            row(now - 120_000, minutes = 2, sneaker = 7), // A 로 2분 — 10
            row(now - 180_000, minutes = 3), // 신발을 모르는 예전 줄 — 15, 지금 신은 신발 몫
            row(now - 240_000, minutes = 10, sneaker = 9, crew = "crew-1"), // 크루 러닝 — 공용 체력, 신발은 안 닳는다
            row(now - 300_000, minutes = 5, sneaker = 9, state = UploadState.REJECTED), // 거절 — 닳지 않는다
        )
        val wear = pendingShoeWearByShoe(rows, syncedFrom = 0, now = now)
        assertEquals(mapOf(7L to 30), wear.byShoe)
        assertEquals(15, wear.unknown)
        assertEquals(45, pendingShoeWearOf(rows, 0, now))

        val a = shoe(id = 7, now = 50, max = 50, equipped = false)
        val b = shoe(id = 8, now = 50, max = 50, equipped = true)
        val crewShoe = shoe(id = 9, now = 50, max = 50, equipped = false)
        // A 로 달리고 B 로 바꿨다 — B 에서는 모르는 줄 몫만, A 에서는 A 의 러닝만
        assertEquals(30, wear.of(a))
        assertEquals(15, wear.of(b))
        assertEquals(0, wear.of(crewShoe))
    }

    // ── #17 거절 뒤 "최신 값으로 다시 불러왔어요"는 다시 받았을 때만 ──

    @Test fun `거절 뒤 다시 받지 못했으면 다시 불러왔다고 하지 않는다`() {
        val server = shoe(now = 50, max = 50).server!!.copy(
            points = com.stepup.android.domain.StatPoints(total = 4, free = 4),
        )
        val basis = requireNotNull(pointsBasisOf(shoe(now = 50, max = 50).copy(level = 2, server = server)))
        val sending = PointsPhase.Sending(basis, PointsDraft(efficiency = 1))
        val refused = EconomyOutcome.Rejected("나눈 포인트는 되돌릴 수 없습니다")
        assertEquals(PointsError.REJECTED, (pointsAfterSend(sending, refused, basis, reloaded = true) as PointsPhase.Editing).error)
        assertEquals(
            PointsError.REJECTED_NOT_RELOADED,
            (pointsAfterSend(sending, refused, basis, reloaded = false) as PointsPhase.Editing).error,
        )
        assertEquals(PointsError.REJECTED_NOT_RELOADED, (pointsAfterSend(sending, refused, null) as PointsPhase.Editing).error)
    }
}
