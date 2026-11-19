package com.stepup.android

import com.stepup.android.data.remote.PartyRoomRow
import com.stepup.android.data.repo.CrewRepository
import com.stepup.android.data.repo.LatestOnly
import com.stepup.android.data.repo.PartyPhase
import com.stepup.android.data.repo.PartyProblem
import com.stepup.android.data.repo.PartyPush
import com.stepup.android.data.repo.PartyReturns
import com.stepup.android.data.repo.PartyState
import com.stepup.android.data.repo.joinProblem
import com.stepup.android.data.repo.keepsLobby
import com.stepup.android.data.repo.lobbyProblem
import com.stepup.android.data.repo.openRefusal
import com.stepup.android.data.repo.partyPush
import com.stepup.android.data.repo.pollRefusal
import com.stepup.android.data.repo.readyRefusal
import com.stepup.android.data.repo.releaseOnExit
import com.stepup.android.data.repo.startRefusal
import com.stepup.android.data.repo.toDomain
import com.stepup.android.data.repo.unrunRoom
import com.stepup.android.data.repo.unrunToday
import com.stepup.android.domain.CrewProblem
import com.stepup.android.ui.screens.walk.LobbyTerminal
import com.stepup.android.ui.screens.walk.lobbyTerminal
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * 함께 뛰기 대기실 · 출발 점검 수정(2026-11-18, 서버 0117) — 서버가 가른 까닭 읽기 · 방에 그대로 둘지 · 나갈 때 돌려받기 ·
 * 달리지 않은 방 알아보기 · 출발 푸시 가리기 · 정해 둔 출발 시각 · 준비를 차례대로 보내기.
 */
class TogetherLobbyFixTest {

    // ── 서버 거절의 까닭 ──

    @Test fun `방 상태 거절 - 나를 빼고 출발 · 닫힘 · 내보냄을 가른다`() {
        assertEquals(PartyProblem.LEFT_BEHIND, pollRefusal("room_started"))
        assertEquals(PartyProblem.CLOSED, pollRefusal("room_closed"))
        assertEquals(PartyProblem.REMOVED, pollRefusal("진행자가 내보낸 방입니다"))
        // 예전 서버는 까닭을 가르지 않는다 — 예전처럼
        assertEquals(PartyProblem.REMOVED, pollRefusal("이 파티에 들어와 있지 않습니다"))
    }

    @Test fun `들어가다 막힌 까닭 - 크루원이 아님 · 닫힌 번개 대기실은 내보냄이 아니다`() {
        assertEquals(PartyProblem.NOT_MEMBER, joinProblem("크루원만 들어갈 수 있습니다"))
        assertEquals(PartyProblem.REMOVED, joinProblem("진행자가 내보낸 방입니다"))
        assertEquals(PartyProblem.FLASH_CLOSED, openRefusal("flash_lobby_closed"))
        assertEquals(PartyProblem.NOT_MEMBER, openRefusal("크루원이나 번개 참가자만 들어갈 수 있습니다"))
        assertEquals(PartyProblem.REMOVED, openRefusal("진행자가 내보낸 방입니다"))
    }

    @Test fun `준비 · 출발 거절 - 오늘 이미 함 · 코스 바뀜은 제 까닭으로`() {
        assertEquals(PartyProblem.DONE_TODAY, readyRefusal("crew_run_done_today"))
        assertNull(readyRefusal("이미 출발한 파티입니다"))
        assertEquals(PartyProblem.COURSE_CHANGED, startRefusal("course_changed"))
        assertEquals(PartyProblem.DONE_TODAY, startRefusal("crew_run_done_today"))
        assertEquals(PartyProblem.NEED_TWO, startRefusal("crew_run_need_two"))
        // 까닭을 적은 거절은 다음에 누를 때까지 대기실 위에 남는다 — 출발한 뒤에는 두지 않는다
        assertEquals(PartyProblem.DONE_TODAY, lobbyProblem(false, PartyPhase.LOBBY, PartyProblem.DONE_TODAY, null, false))
        assertEquals(PartyProblem.COURSE_CHANGED, lobbyProblem(false, PartyPhase.LOBBY, PartyProblem.COURSE_CHANGED, null, true))
        assertNull(lobbyProblem(false, PartyPhase.COUNTDOWN, PartyProblem.COURSE_CHANGED, null, true))
        // 방 위 안내일 뿐 — 방을 대신하는 화면이 아니다
        assertTrue(PartyProblem.DONE_TODAY.keepsRoom)
        assertTrue(PartyProblem.COURSE_CHANGED.keepsRoom)
        assertFalse(PartyProblem.LEFT_BEHIND.keepsRoom)
        assertFalse(PartyProblem.REMOVED.keepsRoom)
    }

    @Test fun `해산 거절 - 달리는 중인 크루 러닝이 있으면 제 까닭으로`() {
        assertEquals(CrewProblem.CREW_RUNNING, CrewProblem.of("crew_running"))
        assertEquals(CrewProblem.OTHER, CrewProblem.of("crew_runner"))
    }

    // ── 방에 그대로 있는가 ──

    private val room = PartyState(phase = PartyPhase.LOBBY, partyId = 7L, crewId = "c", pickedRoom = 7L)

    @Test fun `묻기가 멈춘 방은 들어가 있는 방이 아니다`() {
        assertTrue(room.inRoom)
        assertTrue(room.copy(problem = PartyProblem.FAILED).inRoom)
        assertTrue(room.copy(problem = PartyProblem.NEED_TWO).inRoom)
        assertTrue(room.copy(problem = PartyProblem.NETWORK).inRoom)
        // 내보냄 · 닫힘 · 로그인 필요 — 굳은 "준비 완료"로 화면을 켜 두거나 그 방을 다시 열지 않는다
        assertFalse(room.copy(problem = PartyProblem.REMOVED).inRoom)
        assertFalse(room.copy(problem = PartyProblem.CLOSED).inRoom)
        assertFalse(room.copy(problem = PartyProblem.SIGN_IN).inRoom)
        assertFalse(room.copy(partyId = null).inRoom)
        assertTrue(room.isCrewRoom)
        assertFalse(PartyState(phase = PartyPhase.LOBBY, partyId = 3L, flashPostId = 9L).isCrewRoom)
    }

    @Test fun `같은 대기실로 돌아왔을 때 - 방 위 안내 · 잠깐 끊김으로는 다시 들어가지 않는다`() {
        assertTrue(keepsLobby(PartyPhase.LOBBY, null, polling = true))
        // 채팅에 다녀온 사이 "출발하지 못했어요" · 연결 안내가 떠 있어도 방에 그대로다(다시 들어가면 준비 · 진행자를 잃을 수 있다)
        assertTrue(keepsLobby(PartyPhase.LOBBY, PartyProblem.NEED_TWO, polling = true))
        assertTrue(keepsLobby(PartyPhase.LOBBY, PartyProblem.FAILED, polling = true))
        assertTrue(keepsLobby(PartyPhase.LOBBY, PartyProblem.NETWORK, polling = true))
        // 묻는 일이 끝났으면 다시 연다
        assertFalse(keepsLobby(PartyPhase.LOBBY, PartyProblem.NETWORK, polling = false))
        // 놓친 출발 · 켜지 못한 출발 · 나를 빼고 출발 — 그 결말을 보인다(말없이 새 대기실에 넣지 않는다)
        assertTrue(keepsLobby(PartyPhase.LOBBY, PartyProblem.MISSED_START, polling = false))
        assertTrue(keepsLobby(PartyPhase.LOBBY, PartyProblem.START_BLOCKED, polling = false))
        assertTrue(keepsLobby(PartyPhase.LOBBY, PartyProblem.LEFT_BEHIND, polling = false))
        // 내보냄 · 닫힘 · 결과 · 빈 상태는 다시 연다
        assertFalse(keepsLobby(PartyPhase.LOBBY, PartyProblem.REMOVED, polling = false))
        assertFalse(keepsLobby(PartyPhase.LOBBY, PartyProblem.CLOSED, polling = true))
        assertFalse(keepsLobby(PartyPhase.FINISHED, null, polling = false))
        assertFalse(keepsLobby(PartyPhase.IDLE, null, polling = false))
    }

    @Test fun `대기실 대신 한 화면 - 나를 빼고 출발 · 크루원 아님 · 켜지 못한 출발`() {
        val gone = room.copy(partyId = null)
        assertEquals(LobbyTerminal.LEFT_BEHIND, lobbyTerminal(gone.copy(problem = PartyProblem.LEFT_BEHIND)))
        assertEquals(LobbyTerminal.NOT_MEMBER, lobbyTerminal(gone.copy(problem = PartyProblem.NOT_MEMBER)))
        assertEquals(LobbyTerminal.BLOCKED, lobbyTerminal(gone.copy(problem = PartyProblem.START_BLOCKED)))
        assertEquals(LobbyTerminal.MISSED, lobbyTerminal(gone.copy(problem = PartyProblem.MISSED_START)))
        // 방 위 안내는 방 화면 그대로
        assertNull(lobbyTerminal(room.copy(problem = PartyProblem.DONE_TODAY)))
        assertNull(lobbyTerminal(room.copy(problem = PartyProblem.COURSE_CHANGED)))
    }

    // ── 나갈 때 · 달리지 않은 방 ──

    @Test fun `나갈 때 돌려받기 - 출발했을 수 있는 크루 방이고 이 폰이 달리지 않았을 때만`() {
        assertTrue(releaseOnExit(crewRoom = true, phase = PartyPhase.COUNTDOWN, myReady = true, ran = false))
        assertTrue(releaseOnExit(crewRoom = true, phase = PartyPhase.RUNNING, myReady = true, ran = false))
        // 진행자가 방금 출발을 눌렀는데 이 폰이 아직 모를 수 있다 — 준비한 사람은 대기실에서도
        assertTrue(releaseOnExit(crewRoom = true, phase = PartyPhase.LOBBY, myReady = true, ran = false))
        // 준비 전이면 출발 명단에 들 수 없다
        assertFalse(releaseOnExit(crewRoom = true, phase = PartyPhase.LOBBY, myReady = false, ran = false))
        // 이 폰이 달린(달리는) 러닝의 기회는 돌려받지 않는다 · 번개러닝에는 하루 한 번이 없다 · 끝난 방
        assertFalse(releaseOnExit(crewRoom = true, phase = PartyPhase.RUNNING, myReady = true, ran = true))
        assertFalse(releaseOnExit(crewRoom = false, phase = PartyPhase.COUNTDOWN, myReady = true, ran = false))
        assertFalse(releaseOnExit(crewRoom = true, phase = PartyPhase.FINISHED, myReady = true, ran = false))
    }

    @Test fun `출발했는데 달리지 않은 내 방 - 늦게 켤 수 있는 시간이 지난 뒤에만`() {
        val start = 1_000_000L
        val late = CrewRepository.LATE_START_MS + CrewRepository.UNRUN_GRACE_MS
        assertTrue(unrunRoom(mine = true, status = "RUNNING", startsAtMs = start, serverNowMs = start + late + 1))
        assertTrue(unrunRoom(mine = true, status = "COUNTDOWN", startsAtMs = start, serverNowMs = start + late + 1))
        // 아직 출발 무렵 — 이 폰(다른 기기)이 러닝을 켤 수 있다
        assertFalse(unrunRoom(mine = true, status = "RUNNING", startsAtMs = start, serverNowMs = start + late))
        assertFalse(unrunRoom(mine = false, status = "RUNNING", startsAtMs = start, serverNowMs = start + late + 1))
        assertFalse(unrunRoom(mine = true, status = "LOBBY", startsAtMs = 0L, serverNowMs = start + late + 1))
        // 오늘 크루 러닝이 "했음"인데 올린 러닝이 없다
        assertTrue(unrunToday(sessionId = null, startedAtMs = start, serverNowMs = start + late + 1))
        assertFalse(unrunToday(sessionId = 55L, startedAtMs = start, serverNowMs = start + late + 1))
        assertFalse(unrunToday(sessionId = null, startedAtMs = start, serverNowMs = start + 30_000L))
        assertFalse(unrunToday(sessionId = null, startedAtMs = 0L, serverNowMs = start + late + 1))
    }

    // ── 출발 푸시 ──

    @Test fun `출발 푸시 - 이 폰이 들어가 있는 방의 것만 가린다`() {
        val crew = "0a1b2c3d-0000-4000-8000-00000000abcd"
        assertEquals(PartyPush.MINE, partyPush("room/$crew/7", room))
        assertEquals(PartyPush.MINE, partyPush("room/$crew/7", room.copy(phase = PartyPhase.RUNNING)))
        // 다른 방 · 이미 나온 방 · 다른 종류의 링크는 평소처럼 띄운다
        assertEquals(PartyPush.OTHER, partyPush("room/$crew/8", room))
        assertEquals(PartyPush.OTHER, partyPush("room/$crew/7", room.copy(partyId = null)))
        assertEquals(PartyPush.OTHER, partyPush("crew-chat/$crew", room))
        assertEquals(PartyPush.OTHER, partyPush("", room))
        // 번개 글 링크는 댓글 알림도 같이 쓴다 — 대기실이면 방을 다시 물어 본 뒤에 정한다
        val flash = PartyState(phase = PartyPhase.LOBBY, partyId = 3L, flashPostId = 42L)
        assertEquals(PartyPush.CHECK, partyPush("flash/42", flash))
        assertEquals(PartyPush.MINE, partyPush("flash/42", flash.copy(phase = PartyPhase.COUNTDOWN)))
        assertEquals(PartyPush.MINE, partyPush("flash/42", flash.copy(phase = PartyPhase.RUNNING)))
        assertEquals(PartyPush.OTHER, partyPush("flash/42", flash.copy(phase = PartyPhase.FINISHED)))
        assertEquals(PartyPush.OTHER, partyPush("flash/41", flash))
        assertEquals(PartyPush.OTHER, partyPush("flash/42", flash.copy(partyId = null)))
        assertEquals(PartyPush.OTHER, partyPush("flash/42", room))
    }

    // ── 나온 뒤 다시 들어간 방 ──

    @Test fun `다시 들어간 방 목록 - 최근 스무 개, 같은 방은 한 번만`() {
        assertEquals(listOf(3L, 5L, 9L), PartyReturns.parse("3, 5,x,,9"))
        assertEquals(emptyList<Long>(), PartyReturns.parse(""))
        assertEquals(listOf(5L, 9L, 3L), PartyReturns.added(listOf(3L, 5L, 9L), 3L))
        val many = (1L..25L).fold(emptyList<Long>()) { ids, id -> PartyReturns.added(ids, id) }
        assertEquals((6L..25L).toList(), many)
    }

    // ── 정해 둔 출발 시각 ──

    @Test fun `정해 둔 출발 시각 · 방을 연 시각은 폰 시계 차이로 밀리지 않는다`() {
        val row = PartyRoomRow(id = 1L, createdAt = "2026-11-18T09:41:07+00:00", planAt = "2026-11-18T10:00:00+00:00")
        val planned = OffsetDateTime.parse("2026-11-18T10:00:00+00:00").toInstant().toEpochMilli()
        val created = OffsetDateTime.parse("2026-11-18T09:41:07+00:00").toInstant().toEpochMilli()
        // 폰 시계가 서버보다 2초 늦거나 3분 빨라도 — 예전에는 그만큼 빼서 "19:00" 방이 "18:59" · "19:03"으로 보였다
        for (offset in listOf(0L, 2_000L, -180_000L)) {
            val listed = row.toDomain(offsetMs = offset)
            assertEquals(planned, listed.planAtMs)
            assertEquals(created, listed.createdAtMs)
            assertEquals("19:00", DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochMilli(listed.planAtMs).atZone(ZoneId.of("Asia/Seoul"))))
        }
        // 출발 시각을 정하지 않은 방
        assertEquals(0L, PartyRoomRow(id = 2L).toDomain().planAtMs)
    }

    // ── 준비를 차례대로 ──

    private data class Ask(val n: Int)

    @Test fun `준비 값은 한 번에 하나씩 - 보내는 사이 다시 누르면 나중 값을 이어서 보내고 답은 나중 것만`() = runBlocking {
        val queue = LatestOnly<Ask>()
        val second = Ask(2)
        val third = Ask(3)
        val sent = mutableListOf<Int>()
        val settled = mutableListOf<Pair<Int, String>>()
        queue.offer(Ask(1))
        assertTrue(queue.busy)
        queue.drain(
            send = { ask ->
                sent.add(ask.n)
                // 첫 값을 보내는 사이 두 번 더 눌렀다 — 가운데 값은 보내지 않고 가장 나중 값만 이어서 보낸다
                if (ask.n == 1) {
                    queue.offer(second)
                    queue.offer(third)
                }
                "answer${ask.n}"
            },
        ) { ask, answer -> settled.add(ask.n to answer) }
        assertEquals(listOf(1, 3), sent)
        assertEquals(listOf(3 to "answer3"), settled)
        assertFalse(queue.busy)
    }

    @Test fun `떠난 방의 준비는 보내지 않고 버린다`() = runBlocking {
        val queue = LatestOnly<Ask>()
        queue.offer(Ask(1))
        var sent = 0
        queue.drain(skip = { true }, send = { sent++ }) { _, _ -> fail("보내지 않은 값의 답은 없다") }
        assertEquals(0, sent)
        assertFalse(queue.busy)
        // 비운 뒤에는 보낼 것이 없다
        queue.offer(Ask(2))
        queue.clear()
        queue.drain(send = { sent++ }) { _, _ -> fail("비운 값의 답은 없다") }
        assertEquals(0, sent)
    }
}
