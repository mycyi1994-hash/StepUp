package com.stepup.android

import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.ui.screens.profile.quarterRuns
import com.stepup.android.ui.screens.profile.shownDistanceKm
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileDistanceTest {
    @Test fun accountDistanceWinsWhenLargerThanPhoneTotal() {
        // 다른 폰에서 달린 기록 — 레벨이 쓰는 계정 거리로 보인다
        assertEquals(120.0, shownDistanceKm(localKm = 30.0, accountKm = 120.0)!!, 0.0)
    }

    @Test fun phoneTotalStaysWhenAccountUnknownOrSmaller() {
        assertEquals(30.0, shownDistanceKm(localKm = 30.0, accountKm = null)!!, 0.0)
        // 서버가 아직 올리지 못한 러닝까지 든 폰 합이 더 크면 폰 합
        assertEquals(30.0, shownDistanceKm(localKm = 30.0, accountKm = 25.0)!!, 0.0)
    }

    @Test fun accountDistanceShownWhileLocalLoading() {
        assertEquals(12.5, shownDistanceKm(localKm = null, accountKm = 12.5)!!, 0.0)
        assertNull(shownDistanceKm(localKm = null, accountKm = null))
    }

    private val zone = ZoneId.of("Asia/Seoul")
    private val today = LocalDate.of(2026, 11, 4)
    private fun at(day: LocalDate, verdict: String = "VERIFIED") = WalkSessionEntity(
        startedAt = day.atTime(7, 0).atZone(zone).toInstant().toEpochMilli(),
        endedAt = day.atTime(7, 30).atZone(zone).toInstant().toEpochMilli(),
        steps = 4_000, durationSec = 1_800, distanceMeters = 5_000.0, calories = 0.0, pointsEarned = 0.0,
        verdict = verdict,
    )

    @Test fun quarterRunsKeepsNinetyDayWindowAndDropsVoid() {
        val inside = at(today.minusDays(89))
        val outside = at(today.minusDays(90))
        val void = at(today, verdict = "VOID")
        val todayRun = at(today)
        assertEquals(listOf(inside, todayRun), quarterRuns(listOf(inside, outside, void, todayRun), today, zone))
    }
}
