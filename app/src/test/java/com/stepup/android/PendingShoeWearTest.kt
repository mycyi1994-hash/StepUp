package com.stepup.android

import com.stepup.android.data.local.UploadState
import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.data.repo.PartyMember
import com.stepup.android.data.repo.PartyPhase
import com.stepup.android.data.repo.PartyProblem
import com.stepup.android.data.repo.PartyState
import com.stepup.android.domain.RecordingOwner
import com.stepup.android.ui.screens.community.waitingForPartyStart
import com.stepup.android.ui.screens.walk.pendingShoeWearOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 2026-11-04 점검 — 아직 올라가지 않은 러닝의 신발 닳음은 그 러닝의 신발에만(크루 러닝은 빼고),
 * 크루 대기실의 화면 켜 두기는 방에 그대로 있을 때만.
 */
class PendingShoeWearTest {

    private val now = 1_800_000_000_000L

    /** 30분 혼자 러닝 — 확인 전이면 150 닳는다(1분마다 5) */
    private fun run(sneaker: Long = 0, crew: String = "", startedAt: Long = now - 3_600_000L) = WalkSessionEntity(
        startedAt = startedAt,
        endedAt = startedAt + 1_800_000L,
        steps = 4_000,
        durationSec = 1_800,
        distanceMeters = 5_000.0,
        calories = 300.0,
        pointsEarned = 0.0,
        recordingOwner = RecordingOwner.account("me"),
        crewId = crew,
        uploadState = UploadState.PENDING.name,
        sneakerServerId = sneaker,
    )

    @Test fun `신발을 적은 러닝은 그 신발에서만 뺀다`() {
        val rows = listOf(run(sneaker = 11), run(sneaker = 22, startedAt = now - 7_200_000L))
        assertEquals(150, pendingShoeWearOf(rows, syncedFrom = 0, now = now, shoeServerId = 11))
        assertEquals(150, pendingShoeWearOf(rows, syncedFrom = 0, now = now, shoeServerId = 22))
        // 지금 신은 신발이어도 다른 신발로 달린 러닝은 세지 않는다
        assertEquals(0, pendingShoeWearOf(rows, syncedFrom = 0, now = now, shoeServerId = 33))
        // 폰에만 있던 신발(서버 번호 0)도 다른 신발로 달린 러닝은 세지 않는다
        assertEquals(0, pendingShoeWearOf(rows, syncedFrom = 0, now = now, shoeServerId = 0))
    }

    @Test fun `신발을 적지 않은 러닝은 예전처럼 신은 신발에서 뺀다`() {
        val rows = listOf(run(sneaker = 0))
        assertEquals(150, pendingShoeWearOf(rows, syncedFrom = 0, now = now, shoeServerId = 33))
        assertEquals(150, pendingShoeWearOf(rows, syncedFrom = 0, now = now, shoeServerId = 33, shoeEquipped = true))
        // 신지 않은 신발(수리 시트에서 고른 다른 신발)에서는 빼지 않는다
        assertEquals(0, pendingShoeWearOf(rows, syncedFrom = 0, now = now, shoeServerId = 33, shoeEquipped = false))
    }

    @Test fun `크루 러닝은 개인 신발을 닳게 하지 않는다`() {
        val rows = listOf(run(sneaker = 11, crew = "crew-1"), run(sneaker = 0, crew = "crew-1"))
        assertEquals(0, pendingShoeWearOf(rows, syncedFrom = 0, now = now, shoeServerId = 11))
        assertEquals(0, pendingShoeWearOf(rows, syncedFrom = 0, now = now, shoeServerId = 0))
    }

    @Test fun `신은 신발이 적힌 신발이면 둘 다 센다`() {
        val rows = listOf(run(sneaker = 11), run(sneaker = 0, startedAt = now - 7_200_000L), run(sneaker = 22, startedAt = now - 9_000_000L))
        assertEquals(300, pendingShoeWearOf(rows, syncedFrom = 0, now = now, shoeServerId = 11, shoeEquipped = true))
        // 신지 않은 11 번 신발 — 11 로 달린 러닝만
        assertEquals(150, pendingShoeWearOf(rows, syncedFrom = 0, now = now, shoeServerId = 11, shoeEquipped = false))
    }

    // ── 크루 대기실 화면 켜 두기 ───────────────────────────────────────

    private fun room(problem: PartyProblem? = null, phase: PartyPhase = PartyPhase.LOBBY, ready: Boolean = true, closed: Boolean = false) =
        PartyState(
            phase = phase,
            partyId = 9L,
            crewId = "crew-1",
            members = listOf(PartyMember("me", "나", ready = ready, isMe = true), PartyMember("b", "B", ready = true, isMe = false)),
            problem = problem,
            roomClosed = closed,
        )

    @Test fun `준비하고 기다리는 동안만 화면을 켜 둔다`() {
        assertTrue(waitingForPartyStart(room()))
        assertTrue(waitingForPartyStart(room(phase = PartyPhase.COUNTDOWN)))
        assertFalse("준비하지 않았다", waitingForPartyStart(room(ready = false)))
        assertFalse("달리기 시작했다", waitingForPartyStart(room(phase = PartyPhase.RUNNING)))
        assertFalse("방에 없다", waitingForPartyStart(room().copy(partyId = null)))
    }

    @Test fun `내보내졌거나 로그인이 풀렸거나 방이 닫혔으면 화면을 켜 두지 않는다`() {
        // 묻기를 멈춰도 단계 · 명단은 그대로 남는다(CrewRepository) — 예전엔 화면이 계속 켜져 있었다
        for (problem in listOf(PartyProblem.REMOVED, PartyProblem.SIGN_IN, PartyProblem.CLOSED, PartyProblem.FULL, PartyProblem.STARTED, PartyProblem.MISSED_START)) {
            assertFalse("$problem", waitingForPartyStart(room(problem = problem)))
        }
        assertFalse("방장이 닫았다", waitingForPartyStart(room(closed = true)))
    }

    @Test fun `연결이 잠깐 끊기거나 누른 것이 막혔을 뿐이면 계속 기다린다`() {
        assertTrue(waitingForPartyStart(room(problem = PartyProblem.NETWORK)))
        assertTrue(waitingForPartyStart(room(problem = PartyProblem.FAILED)))
    }
}
