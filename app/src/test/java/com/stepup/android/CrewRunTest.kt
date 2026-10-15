package com.stepup.android

import com.stepup.android.data.repo.PartyMember
import com.stepup.android.data.repo.PartyPhase
import com.stepup.android.data.repo.PartyProblem
import com.stepup.android.data.repo.PartyState
import com.stepup.android.domain.GeoPoint
import com.stepup.android.service.RunGroup
import com.stepup.android.service.StartGroup
import com.stepup.android.ui.screens.walk.CrewEnd
import com.stepup.android.ui.screens.walk.CrewLink
import com.stepup.android.ui.screens.walk.crewEnd
import com.stepup.android.ui.screens.walk.crewLink
import com.stepup.android.ui.screens.walk.crewOrder
import com.stepup.android.ui.screens.walk.crewPins
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 크루 달리기 v2(2026-10-16) — 러닝의 모임(크루 · 번개)과 방과의 연결, 지도에 올릴 사람, 마칠 때의 안내.
 * 화면이 보이는 것은 실제로 확인한 상태만이어야 한다 — 끊긴 방의 옛 위치, 위치를 공유하지 않은 사람의 점, 진행자에게 맞지 않는 안내가 없게.
 */
class CrewRunTest {
    private val here = GeoPoint(37.5181, 126.9518)

    private fun member(id: String, me: Boolean = false, host: Boolean = false, sharing: Boolean = false, point: GeoPoint? = null) =
        PartyMember(id, "이름$id", ready = true, isMe = me, isHost = host, point = point, sharing = sharing)

    private val running = PartyState(
        phase = PartyPhase.RUNNING, partyId = 7L, crewId = "c1", crewName = "여의도 퇴근런",
        members = listOf(
            member("me", me = true, sharing = true, point = here),
            member("h", host = true, sharing = true, point = GeoPoint(37.5196, 126.9490)),
            member("a"),
            member("b", sharing = true, point = GeoPoint(37.5222, 126.9431)),
        ),
    )

    @Test fun startGroupComesFromTheRoomNotFromBeingActive() {
        // 크루 방은 크루 달리기, 번개 글의 방은 번개러닝 — 혼자 출발해도(인원 1) 모임은 같다
        assertEquals(StartGroup(RunGroup.CREW, "c1", "여의도 퇴근런"), StartGroup.of(running, " 여의도 퇴근런 "))
        assertEquals(StartGroup(RunGroup.CREW, "c1", "여의도 퇴근런"),
            StartGroup.of(running.copy(members = running.members.take(1)), "여의도 퇴근런"))
        val flash = running.copy(crewId = null, flashPostId = 101L, crewName = "한강 번개")
        assertEquals(StartGroup(RunGroup.FLASH, "", "한강 번개"), StartGroup.of(flash, "한강 번개"))
        // 크루도 번개 글도 없는 방은 모임이 아니다
        assertEquals(StartGroup.NONE, StartGroup.of(running.copy(crewId = null), ""))
        // 저장본 칸을 넘기지 않게 이름은 잘라 둔다
        assertEquals(StartGroup.NAME_MAX, StartGroup.of(flash, "가".repeat(500)).name.length)
    }

    @Test fun linkFollowsTheRunsOwnRoom() {
        assertEquals(CrewLink.Linked, crewLink(running, "c1"))
        // 서버에 잠시 닿지 못함(CR18) — 다시 물으면 풀린다
        assertEquals(CrewLink.Offline, crewLink(running.copy(liveOffline = true), "c1"))
        // 진행자가 마쳐 방이 닫힘
        assertEquals(CrewLink.Closed, crewLink(running.copy(roomClosed = true), "c1"))
        // 방에서 빠졌거나 로그인이 풀림 — 더 묻지 않는다
        assertEquals(CrewLink.Detached, crewLink(running.copy(problem = PartyProblem.REMOVED), "c1"))
        assertEquals(CrewLink.Detached, crewLink(running.copy(problem = PartyProblem.SIGN_IN), "c1"))
        // 앱이 다시 켜져 방 상태가 비었다(되살린 러닝) · 다른 크루의 방 · 번개 방은 이 러닝의 방이 아니다
        assertEquals(CrewLink.Detached, crewLink(PartyState(), "c1"))
        assertEquals(CrewLink.Detached, crewLink(running.copy(crewId = "c2"), "c1"))
        assertEquals(CrewLink.Detached, crewLink(running.copy(crewId = null, flashPostId = 101L), "c1"))
        assertEquals(CrewLink.Detached, crewLink(running.copy(phase = PartyPhase.FINISHED), "c1"))
    }

    @Test fun onlySharedFreshPointsAreDrawn() {
        // 위치를 공유한 다른 사람만(나는 내 경로 끝점) — 공유하지 않는 사람은 줄에만 있다
        assertEquals(listOf("h", "b"), crewPins(running, CrewLink.Linked).map { it.id })
        val sneaky = running.copy(members = running.members + member("x", point = GeoPoint(37.51, 126.95)))
        assertEquals(listOf("h", "b"), crewPins(sneaky, CrewLink.Linked).map { it.id })
        // 끊김 · 방 닫힘 · 되살린 러닝에서는 옛 위치를 지금 것처럼 보이지 않는다
        for (link in listOf(CrewLink.Offline, CrewLink.Closed, CrewLink.Detached)) {
            assertTrue(link.name, crewPins(running, link).isEmpty())
        }
    }

    @Test fun endCopyMatchesWhatTheServerDoes() {
        // 참가자가 마치면 방에서 나오고(party_leave), 진행자가 마치면 방이 닫힌다(party_finish)
        assertEquals(CrewEnd.Member, crewEnd(running, CrewLink.Linked))
        val hosting = running.copy(members = running.members.map { it.copy(isHost = it.isMe) })
        assertEquals(CrewEnd.Host, crewEnd(hosting, CrewLink.Linked))
        assertEquals(CrewEnd.Host, crewEnd(hosting, CrewLink.Offline))
        // 혼자 남았거나 · 방이 닫혔거나 · 끊겼으면 내 기록만 저장하고 마친다
        assertEquals(CrewEnd.Alone, crewEnd(running.copy(members = running.members.take(1)), CrewLink.Linked))
        assertEquals(CrewEnd.Alone, crewEnd(hosting, CrewLink.Closed))
        assertEquals(CrewEnd.Alone, crewEnd(running, CrewLink.Detached))
    }

    @Test fun rosterPutsHostThenMe() {
        assertEquals(listOf("h", "me", "a", "b"), crewOrder(running.members).map { it.id })
        val hosting = running.members.map { it.copy(isHost = it.isMe) }
        assertEquals(listOf("me", "h", "a", "b"), crewOrder(hosting).map { it.id })
    }
}
