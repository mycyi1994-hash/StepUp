package com.stepup.android

import com.stepup.android.data.remote.SneakerDateRow
import com.stepup.android.data.repo.EconomySync
import com.stepup.android.domain.AchievementBook
import com.stepup.android.domain.AchievementCategory
import com.stepup.android.domain.AchievementMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 2026-11-17 버그 점검 #7 · #16 — 강화 업적 수 · 신발 획득일(서버 0110) */
class ShoeFixesTest {

    @Test fun `강화 업적은 서버가 센 성공한 강화와 원장의 예전 강화 중 큰 쪽`() {
        assertEquals(3, AchievementBook.upgradeCount(ledgerUpgrades = 0, forgeSuccesses = 3))
        assertEquals(5, AchievementBook.upgradeCount(ledgerUpgrades = 5, forgeSuccesses = 3))
        // 서버에 0110 이 없으면 강화 수는 0 — 예전처럼 원장만
        assertEquals(2, AchievementBook.upgradeCount(ledgerUpgrades = 2, forgeSuccesses = 0))
        assertEquals(0, AchievementBook.upgradeCount(ledgerUpgrades = -1, forgeSuccesses = -4))
    }

    @Test fun `성공한 강화 셋이면 강화 업적 첫 칸이 열린다`() {
        val metrics = AchievementMetrics(upgrades = AchievementBook.upgradeCount(0, 3))
        val upgrade = AchievementBook.build(metrics).filter { it.category == AchievementCategory.UPGRADE }
        assertTrue(upgrade[0].unlocked && upgrade[1].unlocked)
        assertTrue(!upgrade[2].unlocked)
    }

    @Test fun `획득일은 서버 날이 먼저, 없으면 폰이 처음 본 때, 그것도 없으면 지금`() {
        assertEquals(100L, EconomySync.acquiredAt(server = 100L, previous = 500L, now = 900L))
        assertEquals(500L, EconomySync.acquiredAt(server = null, previous = 500L, now = 900L))
        assertEquals(900L, EconomySync.acquiredAt(server = null, previous = null, now = 900L))
        assertEquals(500L, EconomySync.acquiredAt(server = 0L, previous = 500L, now = 900L))
    }

    @Test fun `서버 시각을 읽는다, 못 읽으면 null`() {
        assertEquals(1_767_312_000_000L, SneakerDateRow.millis("2026-01-02T09:00:00+09:00"))
        assertEquals(1_767_312_000_123L, SneakerDateRow.millis("2026-01-02T00:00:00.123456+00:00"))
        assertNull(SneakerDateRow.millis(null))
        assertNull(SneakerDateRow.millis("어제"))
    }
}
