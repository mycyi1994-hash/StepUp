package com.stepup.android

import com.stepup.android.data.remote.PartyRoomsRow
import com.stepup.android.domain.RoomMode
import com.stepup.android.ui.screens.community.minutesUntil
import com.stepup.android.ui.screens.profile.nextMilestoneKm
import com.stepup.android.ui.screens.walk.defaultPlanAt
import com.stepup.android.ui.screens.walk.planAtFor
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 2026-10-26 전체 시안 — 대기실 방식 · 출발 시각 · 번개 출발 · 다음 기록 */
class Design1026Test {
    @Test fun `room mode reads the server value and falls back to together`() {
        assertEquals(RoomMode.REMOTE, RoomMode.of("REMOTE"))
        assertEquals(RoomMode.TOGETHER, RoomMode.of("TOGETHER"))
        assertEquals(RoomMode.TOGETHER, RoomMode.of(null))
        assertEquals(RoomMode.TOGETHER, RoomMode.of("ODD"))
    }

    @Test fun `room list row carries mode, place, plan and names`() {
        val json = """{"server_now":"2026-10-25T10:00:00Z","rooms":[{"id":7,"run_mode":"REMOTE","meet_place":null,
            "plan_at":"2026-10-25T11:00:00Z","member_names":["민수","지훈"],"capacity":8}]}"""
        val row = Json { ignoreUnknownKeys = true }.decodeFromString(PartyRoomsRow.serializer(), json).rooms.single()
        assertEquals("REMOTE", row.runMode)
        assertEquals(listOf("민수", "지훈"), row.memberNames)
        assertEquals(8, row.capacity)
        // 예전 서버(0073)의 줄은 같이 뛰기 · 이름 없음
        val old = Json { ignoreUnknownKeys = true }.decodeFromString(PartyRoomsRow.serializer(), """{"server_now":"x","rooms":[{"id":1}]}""").rooms.single()
        assertEquals("TOGETHER", old.runMode)
        assertTrue(old.memberNames.isEmpty())
    }

    @Test fun `default plan time is the next half hour at least 15 minutes away`() {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.of(2026, 10, 25, 18, 20, 0, 0, zone).toInstant().toEpochMilli()
        val at = Instant.ofEpochMilli(defaultPlanAt(now)).atZone(zone)
        assertEquals(19, at.hour)
        assertEquals(0, at.minute)
        val early = ZonedDateTime.of(2026, 10, 25, 18, 5, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(30, Instant.ofEpochMilli(defaultPlanAt(early)).atZone(zone).minute)
    }

    @Test fun `a passed plan time moves to tomorrow`() {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.of(2026, 10, 25, 20, 0, 0, 0, zone)
        val later = Instant.ofEpochMilli(planAtFor(21, 0, now.toInstant().toEpochMilli())).atZone(zone)
        assertEquals(25, later.dayOfMonth)
        val passed = Instant.ofEpochMilli(planAtFor(7, 0, now.toInstant().toEpochMilli())).atZone(zone)
        assertEquals(26, passed.dayOfMonth)
        // 10분 안으로 지난 시각은 오늘 그대로(서버가 받는다)
        val justNow = Instant.ofEpochMilli(planAtFor(19, 55, now.toInstant().toEpochMilli())).atZone(zone)
        assertEquals(25, justNow.dayOfMonth)
    }

    @Test fun `flash start minutes stay within 5 minutes and 24 hours`() {
        val now = ZonedDateTime.of(2026, 10, 25, 18, 37, 0, 0, ZoneId.systemDefault())
        assertEquals(23, minutesUntil(19, 0, now))
        // 이미 지났으면 다음 날
        assertEquals(24 * 60 - 37, minutesUntil(18, 0, now))
        assertTrue(minutesUntil(18, 39, now) >= 5)
        assertTrue(minutesUntil(18, 37, now) <= 24 * 60)
    }

    @Test fun `next record is the next milestone above the total`() {
        assertEquals(10, nextMilestoneKm(0.0))
        assertEquals(150, nextMilestoneKm(128.4))
        assertEquals(200, nextMilestoneKm(150.0))
        assertEquals(6000, nextMilestoneKm(5_200.0))
    }
}
