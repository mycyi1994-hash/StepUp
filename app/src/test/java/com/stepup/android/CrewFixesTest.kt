package com.stepup.android

import com.stepup.android.data.repo.PartyMember
import com.stepup.android.data.repo.PartyPhase
import com.stepup.android.data.repo.PartyProblem
import com.stepup.android.data.repo.PartyState
import com.stepup.android.data.repo.lobbyProblem
import com.stepup.android.data.repo.startRefusal
import com.stepup.android.ui.screens.community.FlashMeetTime
import com.stepup.android.ui.screens.community.flashCancelable
import com.stepup.android.ui.screens.walk.lobbyTerminal
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 2026-11-17 버그 점검 — 크루 · 번개 러닝(#2 · #4 · #5 · #9, 서버 0109) */
class CrewFixesTest {

    private fun member(id: String, ready: Boolean, seen: Int, me: Boolean = false, host: Boolean = false) =
        PartyMember(id = id, name = id, ready = ready, isMe = me, isHost = host, seenSec = seen)

    private val lobby = PartyState(
        phase = PartyPhase.LOBBY,
        partyId = 7L,
        crewId = "crew",
        members = listOf(
            member("me", ready = true, seen = 40, me = true, host = true),
            member("asleep", ready = true, seen = 20),
            member("here", ready = false, seen = 1),
        ),
    )

    @Test fun `ready members not looking at the lobby do not count toward the start`() {
        // 나는 오래 안 물었어도(지금 누르는 사람) 센다 — 잠든 폰은 서버처럼 뺀다
        assertEquals(1, lobby.readyCount)
        assertEquals(listOf("asleep"), lobby.awayReady.map { it.id })
        assertFalse(lobby.members.first { it.isMe }.away)
        // 15초까지는 자리에 있다(서버 party_start 와 같다)
        val back = lobby.copy(members = lobby.members.map { if (it.id == "asleep") it.copy(seenSec = PartyMember.AWAY_SEC) else it })
        assertEquals(2, back.readyCount)
        assertTrue(back.awayReady.isEmpty())
        // 달리는 중에는 소식이 끊겨도 준비 인원을 그대로 센다
        assertEquals(2, lobby.copy(phase = PartyPhase.RUNNING).readyCount)
        assertTrue(lobby.copy(phase = PartyPhase.RUNNING).awayReady.isEmpty())
    }

    @Test fun `refused start for too few people keeps a specific notice while someone is away`() {
        assertEquals(PartyProblem.NEED_TWO, startRefusal("crew_run_need_two"))
        assertNull(startRefusal("course_changed"))
        assertEquals(PartyProblem.NEED_TWO,
            lobbyProblem(false, PartyPhase.LOBBY, PartyProblem.NEED_TWO, null, true, awayReady = true))
        // 돌아왔으면 거둔다 — 다시 누르면 출발한다
        assertNull(lobbyProblem(false, PartyPhase.LOBBY, PartyProblem.NEED_TWO, null, true, awayReady = false))
        assertNull(lobbyProblem(false, PartyPhase.COUNTDOWN, PartyProblem.NEED_TWO, null, true, awayReady = true))
        // 대기실 위 안내일 뿐 — 방을 대신하는 한 화면이 아니다
        assertNull(lobbyTerminal(lobby.copy(problem = PartyProblem.NEED_TWO)))
    }

    private val zone: ZoneId = ZoneId.of("Asia/Seoul")
    private val now: ZonedDateTime = ZonedDateTime.of(2026, 11, 17, 18, 37, 20, 0, zone)
    private val nowMs = now.toInstant().toEpochMilli()
    private fun at(day: Int, h: Int, m: Int) = ZonedDateTime.of(2026, 11, day, h, m, 0, 0, zone).toInstant().toEpochMilli()

    @Test fun `flash start keeps the picked clock time itself`() {
        val pick = FlashMeetTime.pick(19, 0, now)
        assertEquals(at(17, 19, 0), pick.atMillis)
        assertFalse(pick.clamped)
        // "N분 뒤"는 보여 주기만 — 19:00 까지 22분 40초 → 23분
        assertEquals(23, FlashMeetTime.minutesLeft(pick.atMillis, nowMs))
        // 앞서 지난 시각은 내일 그 시각
        assertEquals(at(18, 7, 0), FlashMeetTime.pick(7, 0, now).atMillis)
    }

    @Test fun `flash start too soon becomes the earliest time instead of tomorrow`() {
        // 5분 안 · 막 지난 시각 — 말없이 내일로 넘기지 않고 가장 이른 시각(5분 뒤, 분 단위 올림)으로 맞추고 알린다
        val soon = FlashMeetTime.pick(18, 39, now)
        assertTrue(soon.clamped)
        assertEquals(at(17, 18, 43), soon.atMillis)
        val justPassed = FlashMeetTime.pick(18, 20, now)
        assertTrue(justPassed.clamped)
        assertEquals(at(17, 18, 43), justPassed.atMillis)
        // 화면을 오래 열어 두어 5분 안으로 들어왔다 — 올리기 전에 맞춘다
        val late = FlashMeetTime.beforeSubmit(at(17, 18, 40), nowMs)
        assertTrue(late.clamped)
        assertEquals(at(17, 18, 43), late.atMillis)
        assertFalse(FlashMeetTime.beforeSubmit(at(17, 19, 0), nowMs).clamped)
        assertEquals(at(17, 19, 38), FlashMeetTime.initial(nowMs))
    }

    @Test fun `flash host cannot cancel once the lobby started`() {
        val flash = PartyState(phase = PartyPhase.LOBBY, partyId = 3L, flashPostId = 42L)
        assertTrue(flashCancelable(42L, flash))
        assertFalse(flashCancelable(42L, flash.copy(phase = PartyPhase.COUNTDOWN)))
        assertFalse(flashCancelable(42L, flash.copy(phase = PartyPhase.RUNNING)))
        // 다른 번개의 방이 출발한 것은 상관없다
        assertTrue(flashCancelable(41L, flash.copy(phase = PartyPhase.RUNNING)))
    }
}
