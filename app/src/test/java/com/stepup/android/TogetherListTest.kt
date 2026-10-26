package com.stepup.android

import com.stepup.android.domain.CrewRoom
import com.stepup.android.domain.CrewRooms
import com.stepup.android.domain.Post
import com.stepup.android.domain.PostCategory
import com.stepup.android.domain.RoomMode
import com.stepup.android.ui.screens.community.TogetherList
import org.junit.Assert.assertEquals
import org.junit.Test

/** 함께 뛰기 "모집 중" — 여러 크루의 대기실 · 번개러닝을 한 목록에(2026-10-26) */
class TogetherListTest {
    private fun room(id: Long, created: Long, mine: Boolean = false, status: String = "LOBBY", kicked: Boolean = false) = CrewRoom(
        id = id, name = "방 $id", status = status, hostId = "h", hostName = "진행자", createdAtMs = created,
        members = 2, ready = 0, capacity = 8, course = null, mine = mine, kicked = kicked, mode = RoomMode.TOGETHER,
    )

    private fun flash(id: Long, meetAt: Long, crewId: String = "", capacity: Int = 4, joined: Int = 1, mine: Boolean = false, remote: Boolean = false) = Post(
        id = id, category = PostCategory.FLASH, crewId = crewId, author = "러너", title = "번개 $id", body = "", createdAt = 0, likes = 0,
        liked = false, commentCount = 0, mine = false, place = if (remote) "" else "한강", distanceKm = 5.0, meetAt = meetAt,
        capacity = capacity, joinedCount = joined, joined = mine, remote = remote,
    )

    @Test fun `rooms from every joined crew in one list, mine first then newest`() {
        val rooms = mapOf(
            "a" to CrewRooms(listOf(room(1, 100), room(2, 300, status = "RUNNING")), capacity = 20, doneToday = false),
            "b" to CrewRooms(listOf(room(3, 200), room(4, 50, mine = true), room(5, 400, kicked = true)), capacity = 20, doneToday = false),
        )
        val list = TogetherList.crewRuns(listOf("a" to "망원", "b" to "반포"), rooms)
        assertEquals(listOf(4L, 3L, 1L), list.map { it.room.id })
        assertEquals(listOf("b", "b", "a"), list.map { it.crewId })
        assertEquals("반포", list.first().crewName)
    }

    @Test fun `a crew whose rooms have not loaded yet adds nothing`() {
        val rooms = mapOf("a" to CrewRooms(listOf(room(1, 100)), capacity = 20, doneToday = false))
        assertEquals(listOf(1L), TogetherList.crewRuns(listOf("a" to "망원", "b" to "반포"), rooms).map { it.room.id })
    }

    @Test fun `upcoming public flash runs, soonest first, full ones only when joined`() {
        val now = 1_000L
        val posts = listOf(
            flash(1, meetAt = 5_000),
            flash(2, meetAt = 2_000, remote = true),
            flash(3, meetAt = 500), // 지났다
            flash(4, meetAt = 3_000, crewId = "crew"), // 크루 게시판 모임은 크루 홈에
            flash(5, meetAt = 4_000, capacity = 2, joined = 2), // 찼다
            flash(6, meetAt = 4_500, capacity = 2, joined = 2, mine = true), // 찼지만 내가 참가했다
        )
        assertEquals(listOf(2L, 6L, 1L), TogetherList.flashes(posts, now).map { it.id })
    }
}
