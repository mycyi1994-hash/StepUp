package com.stepup.android.service

import com.stepup.android.data.repo.CrewRepository
import com.stepup.android.data.repo.PartyPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun `크루 방 알림은 출발 푸시와 같은 꼬리표 - 번개러닝은 꼬리표 없이`() {
        // 서버 출발 푸시(0117)의 링크 room/<크루>/<방> 과 같아야 둘이 겹치지 않고 하나가 다른 하나를 바꾼다
        assertEquals("room/0a1b2c3d-0000-4000-8000-00000000abcd/7", CrewStartAlert.roomTag("0A1B2C3D-0000-4000-8000-00000000ABCD", null, 7))
        assertNull(CrewStartAlert.roomTag(null, 42L, 7))
        assertNull(CrewStartAlert.roomTag("", null, 7))
        // 번개 글의 방은 크루 id 가 있어도 번개다
        assertNull(CrewStartAlert.roomTag("crew", 42L, 7))
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
