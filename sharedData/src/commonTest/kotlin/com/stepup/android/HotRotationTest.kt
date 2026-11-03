package com.stepup.android

import com.stepup.android.data.repo.CommunityRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/** 이번 주 핫글 갱신 시각 — 한국 시간 월요일 09:00(예전 java.time with(MONDAY) 와 같은 답) */
class HotRotationTest {
    private fun kst(iso: String) = Instant.parse("$iso+09:00").toEpochMilliseconds()

    @Test fun lastMondayNineKst() {
        assertEquals(kst("2026-11-02T09:00:00"), CommunityRepository.lastHotRotation(kst("2026-11-03T10:00:00")))
        assertEquals(kst("2026-11-02T09:00:00"), CommunityRepository.lastHotRotation(kst("2026-11-02T09:00:00")))
        assertEquals(kst("2026-10-26T09:00:00"), CommunityRepository.lastHotRotation(kst("2026-11-02T08:59:59")))
        assertEquals(kst("2026-11-02T09:00:00"), CommunityRepository.lastHotRotation(kst("2026-11-08T23:30:00")))
        // 해를 넘어가는 주
        assertEquals(kst("2025-12-29T09:00:00"), CommunityRepository.lastHotRotation(kst("2026-01-01T12:00:00")))
    }
}
