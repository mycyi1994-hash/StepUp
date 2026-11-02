package com.stepup.android.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 공용 규칙이 JVM 함수 대신 쓰는 것들(Platform.kt) — 안드로이드 · iOS 둘 다 같은 기대값으로 돈다.
 * 좌표 반올림은 서버(crew_coarse)와 같은 십진 반올림이어야 한다 — 이진 부동소수로 곱해 자르면 2.675 가 2.67 이 된다.
 */
class PlatformTest {

    @Test fun `좌표 반올림은 십진수로 본 값에서 0_5 를 올린다`() {
        assertEquals(2.68, roundHalfUp2(2.675))
        assertEquals(127.04, roundHalfUp2(127.035))
        assertEquals(37.57, roundHalfUp2(37.5651))
        assertEquals(37.56, roundHalfUp2(37.5649))
        assertEquals(-1.23, roundHalfUp2(-1.225))
        assertEquals(126.98, roundHalfUp2(126.98))
        assertEquals(0.0, roundHalfUp2(0.004))
    }

    @Test fun `도 · 라디안 바꾸기`() {
        assertEquals(PI, toRadians(180.0))
        assertEquals(180.0, toDegrees(PI))
        assertTrue(abs(toRadians(37.5665) - 0.6556591134504498) < 1e-15)
    }

    @Test fun `지금 시각은 2026 년 뒤 ms`() {
        assertTrue(epochMillis() > 1_767_225_600_000L) // 2026-01-01
    }

    @Test fun `신발 이름은 이모지도 한 글자로 세어 20 글자까지`() {
        assertEquals("😀".repeat(20), ShoeNickname.clean("😀".repeat(25)))
        assertEquals("a".repeat(20), ShoeNickname.clean("  " + "a".repeat(30) + "\n"))
        assertEquals("빠른 신발", ShoeNickname.clean("빠른\t\t신발"))
        assertNull(ShoeNickname.clean(" \n "))
    }
}
