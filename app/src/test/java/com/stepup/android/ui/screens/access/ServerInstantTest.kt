package com.stepup.android.ui.screens.access

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 베타 초대 코드 목록의 날짜 — 서버가 보내는 "+00:00" 모양도 읽는다(점검 2026-11-17 #21) */
class ServerInstantTest {
    private val at = Instant.parse("2026-11-17T10:00:00Z")

    @Test
    fun `시차가 붙은 서버 시각을 읽는다`() {
        assertEquals(at, serverInstant("2026-11-17T10:00:00+00:00"))
        assertEquals(at, serverInstant("2026-11-17T19:00:00+09:00"))
        assertEquals(at.plusMillis(123), serverInstant("2026-11-17T10:00:00.123456+00:00")?.let { Instant.ofEpochMilli(it.toEpochMilli()) })
    }

    @Test
    fun `Z 모양도 읽고 빈 값 · 이상한 값은 비운다`() {
        assertEquals(at, serverInstant("2026-11-17T10:00:00Z"))
        assertNull(serverInstant(null))
        assertNull(serverInstant(""))
        assertNull(serverInstant("어제"))
    }
}
