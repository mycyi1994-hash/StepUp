package com.stepup.android.data.remote

import kotlin.test.Test
import kotlin.test.assertEquals

/** 서버 시각 글자 — 공용 함수(kotlin.time)가 예전 안드로이드 java.time 과 같은 값을 내는지 */
class IsoJvmTest {
    private fun old(text: String): Long? =
        runCatching { java.time.OffsetDateTime.parse(text).toInstant().toEpochMilli() }.getOrNull()

    @Test fun parsesLikeOffsetDateTime() {
        listOf(
            "2026-10-16T08:07:09+00:00", "2026-10-16T08:07:09.123+00:00", "2026-10-16T08:07:09.123456+00:00",
            "2026-10-16T08:07:09.9999999+09:00", "2026-10-16T17:07:09Z", "2026-10-16T17:07:09.5Z",
            "2026-12-31T23:59:59-05:30", "1999-01-01T00:00:00.001+14:00", "2026-02-29T00:00:00+00:00",
            "", "not a time", "2026-10-16", "2026-10-16 08:07:09+00:00",
        ).forEach { assertEquals(old(it), isoOffsetMillis(it), it) }
    }

    @Test fun writesLikeJavaInstant() {
        listOf(0L, 1L, 999L, 1_000L, 1_793_628_440_896L, 1_793_628_440_000L, 1_793_628_440_100L, 4_102_444_800_000L)
            .forEach { assertEquals(java.time.Instant.ofEpochMilli(it).toString(), it.toIsoInstant(), it.toString()) }
    }
}
