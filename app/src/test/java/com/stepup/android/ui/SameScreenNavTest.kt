package com.stepup.android.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SameScreenNavTest {
    private val room = "chat/room/{crewId}"

    @Test
    fun `다른 화면이 위에 있으면 그냥 연다`() {
        assertEquals(
            SameScreenNav.Step.OPEN,
            SameScreenNav.step("community", emptyMap(), room, mapOf("crewId" to "b")),
        )
    }

    @Test
    fun `같은 방이 위에 있으면 겹쳐 쌓지 않는다`() {
        assertEquals(
            SameScreenNav.Step.STAY,
            SameScreenNav.step(room, mapOf("crewId" to "a"), room, mapOf("crewId" to "a")),
        )
    }

    @Test
    fun `같은 화면의 다른 방이면 걷고 새로 연다`() {
        assertEquals(
            SameScreenNav.Step.REPLACE,
            SameScreenNav.step(room, mapOf("crewId" to "a"), room, mapOf("crewId" to "b")),
        )
    }

    @Test
    fun `대기실은 크루와 방 번호 둘 다 같아야 그대로 둔다`() {
        val lobby = "lobby/{crewId}?party={party}"
        assertEquals(
            SameScreenNav.Step.REPLACE,
            SameScreenNav.step(lobby, mapOf("crewId" to "a", "party" to "1"), lobby, mapOf("crewId" to "a", "party" to "2")),
        )
        assertEquals(
            SameScreenNav.Step.STAY,
            SameScreenNav.step(lobby, mapOf("crewId" to "a", "party" to "2"), lobby, mapOf("crewId" to "a", "party" to "2")),
        )
    }
}
