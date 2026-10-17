package com.stepup.android

import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.PartyApi
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.remote.UrlConnectionPoster
import com.stepup.android.data.repo.CrewRepository
import com.stepup.android.data.repo.PartyMember
import com.stepup.android.data.repo.PartyPhase
import com.stepup.android.data.repo.PartyProblem
import com.stepup.android.data.repo.PartyState
import com.stepup.android.data.repo.leaveLateRoom
import com.stepup.android.data.repo.leaveRetryDelayMs
import com.stepup.android.data.repo.lobbyProblem
import com.stepup.android.data.repo.partyDepartures
import com.stepup.android.data.repo.partyOffline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 크루 · 번개 같이 달리기가 연결이 불안정할 때(2026-10-17 인터넷 끊김 점검).
 * 끊긴 것을 바로 알고, 굴다리에서 잠깐 끊긴 사람을 나갔다고 하지 않고, 떠난 대기실에 유령 진행자를 남기지 않고,
 * 답만 잃은 준비를 실패라 하지 않는다. 내 러닝 기록은 이것과 상관없이 이어진다.
 */
class PartyLinkTest {

    private fun member(id: String, me: Boolean = false, seen: Int = 0, ready: Boolean = true) =
        PartyMember(id, "이름$id", ready = ready, isMe = me, seenSec = seen)

    private val me = member("me", me = true)
    private val a = member("a")
    private val b = member("b")

    @Test fun offlineAsSoonAsACallMissesOrTheRoomGoesQuiet() {
        // 이번 차례의 보내기 · 물음 하나라도 서버에 닿지 못하면 바로
        assertTrue(partyOffline(missed = true, heardAtMs = 10_000, nowMs = 10_001))
        // 물음이 시간 제한까지 걸려 있어도 방 상태를 LINK_LOST_MS 받지 못하면
        assertFalse(partyOffline(missed = false, heardAtMs = 10_000, nowMs = 10_000 + CrewRepository.LINK_LOST_MS - 1))
        assertTrue(partyOffline(missed = false, heardAtMs = 10_000, nowMs = 10_000 + CrewRepository.LINK_LOST_MS))
        // 예전 물음 하나의 시간 제한(연결 20초 + 읽기 20초)보다 훨씬 빨리 안다
        assertTrue(CrewRepository.LINK_LOST_MS < 40_000L)
    }

    @Test fun partyCallsGiveUpSoonerThanTheAppDefault() {
        val server = StepUpServer(
            baseUrl = "https://test.supabase.co",
            apiKey = "sb_publishable_test",
            sessions = SessionHolder(SupabaseAuth("https://test.supabase.co", "sb_publishable_test"), SignedOut),
        )
        val poster = PartyApi(server).http
        // 앱 기본 연결(20초)을 함께 쓰지 않고 파티만의 짧은 연결을 쓴다 — ServiceLocator 는 그대로
        assertNotSame(server.http, poster)
        assertTrue(poster is UrlConnectionPoster)
        val timeout = UrlConnectionPoster::class.java.getDeclaredField("timeoutMillis")
            .apply { isAccessible = true }
            .getInt(poster)
        assertEquals(PartyApi.TIMEOUT_MS, timeout)
        assertTrue(PartyApi.TIMEOUT_MS < 20_000)
    }

    @Test fun onlyRowsThatDisappearAfterTheStartAreDepartures() {
        val running = listOf(me, a, b)
        // 굴다리에서 1분 넘게 소식이 없어도 서버 명단에 줄이 남아 있으면 아직 방에 있다 — 알리지 않는다
        assertTrue(partyDepartures(running, PartyPhase.RUNNING, listOf(me, a, b.copy(seenSec = 90)), PartyPhase.RUNNING).isEmpty())
        // 줄이 서버 명단에서 없어졌다(러닝을 마치고 나갔다)
        assertEquals(listOf("b"), partyDepartures(running, PartyPhase.RUNNING, listOf(me, a), PartyPhase.RUNNING).map { it.id })
        // 소식이 끊겨 화면에서 잠시 뺐던 사람이 그 뒤에 나가도 알린다(앞 명단은 서버 명단 그대로)
        val quiet = listOf(me, a, b.copy(seenSec = 90))
        assertEquals(listOf("b"), partyDepartures(quiet, PartyPhase.RUNNING, listOf(me, a), PartyPhase.RUNNING).map { it.id })
        // 카운트다운 중에 나간 사람도
        assertEquals(listOf("a"), partyDepartures(running, PartyPhase.COUNTDOWN, listOf(me, b), PartyPhase.RUNNING).map { it.id })
        // 출발하며 서버가 뺀 준비 전 인원 · 대기실에서 오가는 사람은 "나갔어요"가 아니다
        val lobby = listOf(me, a, b.copy(ready = false))
        assertTrue(partyDepartures(lobby, PartyPhase.LOBBY, listOf(me, a), PartyPhase.COUNTDOWN).isEmpty())
        assertTrue(partyDepartures(lobby, PartyPhase.LOBBY, listOf(me, a), PartyPhase.RUNNING).isEmpty())
        assertTrue(partyDepartures(lobby, PartyPhase.LOBBY, listOf(me), PartyPhase.LOBBY).isEmpty())
        // 내 러닝을 마친 뒤(결과)에는 알리지 않는다 · 앞 명단이 없으면(방에 막 들어왔다) 아무도 나가지 않았다
        assertTrue(partyDepartures(running, PartyPhase.RUNNING, listOf(me), PartyPhase.FINISHED).isEmpty())
        assertTrue(partyDepartures(emptyList(), PartyPhase.RUNNING, listOf(me, a), PartyPhase.RUNNING).isEmpty())
        // 나는 알리지 않는다
        assertTrue(partyDepartures(running, PartyPhase.RUNNING, listOf(a, b), PartyPhase.RUNNING).isEmpty())
    }

    @Test fun lostReadyReplyIsSettledByTheServerValue() {
        // 답만 잃었다 — 서버가 이미 받았으면 실패 안내가 없다(연결 안내도 받았으니 푼다)
        assertNull(lobbyProblem(closed = false, phase = PartyPhase.LOBBY, before = null, readyTap = true, serverReady = true))
        assertNull(lobbyProblem(false, PartyPhase.LOBBY, PartyProblem.NETWORK, readyTap = false, serverReady = false))
        // 닿지 않았다 — 화면은 서버 값(누르기 전)을 보이고 실패를 알린다
        assertEquals(PartyProblem.FAILED, lobbyProblem(false, PartyPhase.LOBBY, null, readyTap = true, serverReady = false))
        assertEquals(PartyProblem.FAILED, lobbyProblem(false, PartyPhase.LOBBY, PartyProblem.NETWORK, readyTap = false, serverReady = true))
        // 먼저 읽은 상태가 와서 실패로 보였어도, 나중 상태가 누른 값과 같으면 지운다
        assertNull(lobbyProblem(false, PartyPhase.LOBBY, PartyProblem.FAILED, readyTap = true, serverReady = true))
        // 기다리는 것이 없으면 예전 그대로 — 막힌 누름 · 새 대기실 안내는 대기실에서 다음에 누를 때까지, 연결 · 로그인은 푼다
        assertEquals(PartyProblem.FAILED, lobbyProblem(false, PartyPhase.LOBBY, PartyProblem.FAILED, null, false))
        assertEquals(PartyProblem.MISSED_START, lobbyProblem(false, PartyPhase.LOBBY, PartyProblem.MISSED_START, null, false))
        assertNull(lobbyProblem(false, PartyPhase.LOBBY, PartyProblem.NETWORK, null, false))
        assertNull(lobbyProblem(false, PartyPhase.LOBBY, PartyProblem.SIGN_IN, null, true))
        // 출발한 뒤에는 대기실 안내를 두지 않는다 · 닫힌 대기실은 "방이 닫혔어요"
        assertNull(lobbyProblem(false, PartyPhase.RUNNING, PartyProblem.FAILED, readyTap = true, serverReady = false))
        assertEquals(PartyProblem.CLOSED, lobbyProblem(true, PartyPhase.LOBBY, null, readyTap = true, serverReady = true))
    }

    @Test fun leaveRetriesBackOffForAFewMinutes() {
        assertEquals(listOf(2_000L, 4_000L, 8_000L, 16_000L, 30_000L, 30_000L), (1..6).map { leaveRetryDelayMs(it) })
        assertEquals(CrewRepository.LEAVE_RETRY_MAX_MS, leaveRetryDelayMs(100))
        // 모두 몇 분 — 서버가 대기실 진행자를 넘기는 30초보다 오래 다시 보내고, 끝없이 두드리지는 않는다
        val total = (1 until CrewRepository.LEAVE_TRIES).sumOf { leaveRetryDelayMs(it) }
        assertTrue(total in 120_000L..600_000L)
    }

    @Test fun lateRoomIsLeftOnlyWhenTheLobbyMovedOn() {
        val lobby = PartyState(phase = PartyPhase.LOBBY, crewId = "c1")
        // 기다리는 사이 대기실을 떠났다 — 늦게 온 방에서 나온다(처음 연 사람이면 진행자로 남았다)
        assertTrue(leaveLateRoom(PartyState(), "c1", null, room = 9))
        // 다른 크루 · 다른 번개 대기실로 옮겼다
        assertTrue(leaveLateRoom(lobby.copy(crewId = "c2"), "c1", null, room = 9))
        assertTrue(leaveLateRoom(PartyState(phase = PartyPhase.LOBBY, flashPostId = 5L), null, 6L, room = 9))
        // 같은 대기실에 아직 있다 — 방을 받기 전이거나 같은 방을 받았다(같은 대기실은 같은 방을 받는다)
        assertFalse(leaveLateRoom(lobby, "c1", null, room = 9))
        assertFalse(leaveLateRoom(lobby.copy(partyId = 9), "c1", null, room = 9))
        assertFalse(leaveLateRoom(PartyState(phase = PartyPhase.LOBBY, flashPostId = 5L), null, 5L, room = 9))
        // 같은 대기실이 다른 방을 받았다 — 늦게 온 방은 쓰지 않는다
        assertTrue(leaveLateRoom(lobby.copy(partyId = 10), "c1", null, room = 9))
    }

    private object SignedOut : AuthSessionStore {
        override suspend fun load(): AuthSession? = null
        override suspend fun save(session: AuthSession) = Unit
        override suspend fun clear() = Unit
    }
}
