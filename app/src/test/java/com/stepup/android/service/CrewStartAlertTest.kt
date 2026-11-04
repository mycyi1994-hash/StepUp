package com.stepup.android.service

import com.stepup.android.data.repo.CrewRepository
import com.stepup.android.data.repo.PartyPhase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 앱이 뒤에 있을 때 크루 러닝 출발 알림을 띄울지 — 준비한 방 · 출발 2분 안 · 앱이 뒤 · 러닝이 아직 꺼짐일 때만 */
class CrewStartAlertTest {

    private val start = 1_000_000L

    private fun due(
        phase: PartyPhase = PartyPhase.RUNNING,
        partyId: Long? = 7,
        readyParty: Long? = 7,
        now: Long = start + 10_000,
        foreground: Boolean = false,
        running: Boolean = false,
    ) = CrewStartAlert.shouldAlert(phase, partyId, readyParty, start, now, foreground, running)

    @Test
    fun `준비한 방이 출발했고 앱이 뒤에 있으면 알린다`() {
        assertTrue(due())
        assertTrue(due(phase = PartyPhase.COUNTDOWN, now = start - 3_000))
        assertTrue(due(now = start + CrewRepository.LATE_START_MS))
    }

    @Test
    fun `앱이 앞 · 이미 달림 · 준비 안 함 · 2분 지남 · 대기실이면 알리지 않는다`() {
        assertFalse(due(foreground = true))
        assertFalse(due(running = true))
        assertFalse(due(readyParty = null))
        assertFalse(due(readyParty = 8))
        assertFalse(due(partyId = null))
        assertFalse(due(now = start + CrewRepository.LATE_START_MS + 1))
        assertFalse(due(phase = PartyPhase.LOBBY))
        assertFalse(due(phase = PartyPhase.FINISHED))
        assertFalse(due(phase = PartyPhase.IDLE))
    }
}
