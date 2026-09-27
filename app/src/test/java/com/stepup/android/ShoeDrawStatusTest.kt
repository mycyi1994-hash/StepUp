package com.stepup.android

import com.stepup.android.domain.DrawDistance
import com.stepup.android.domain.DrawKind
import com.stepup.android.domain.DrawStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 뽑기 현황(0042)을 화면 수로 옮기는 규칙 — 출처마다 남은 수, 상급은 지갑 · 체인 상태까지 본다 */
class ShoeDrawStatusTest {
    private val fresh = DrawStatus(
        dailyLeft = 3, dailyTotal = 3, signupLeft = 10, signupGranted = 10, walletLinked = false, giftOnLink = 10,
        giftLeft = 0, runLeft = 0, genesisLeft = 0, runProgressMeters = 0.0, runStepMeters = 1000,
        runToday = 0, runDailyCap = 10, chainPaused = false,
    )

    @Test fun freeCountsTodayAndTheSignUpGift() {
        assertEquals(13, fresh.freeLeft)
        assertTrue(fresh.canDraw(DrawKind.FREE))
        val usedUp = fresh.copy(dailyLeft = 0, signupLeft = 0)
        assertEquals(0, usedUp.freeLeft)
        assertFalse(usedUp.canDraw(DrawKind.FREE))
    }

    @Test fun premiumNeedsALinkedWalletAndARunningChain() {
        assertFalse("no wallet, no premium draw", fresh.copy(giftLeft = 10).canDraw(DrawKind.PREMIUM))
        val linked = fresh.copy(walletLinked = true, giftOnLink = 0, giftLeft = 9, runLeft = 2)
        assertEquals(11, linked.premiumLeft)
        assertTrue(linked.canDraw(DrawKind.PREMIUM))
        assertFalse("paused chain", linked.copy(chainPaused = true).canDraw(DrawKind.PREMIUM))
        assertFalse("nothing left", linked.copy(giftLeft = 0, runLeft = 0).canDraw(DrawKind.PREMIUM))
    }

    @Test fun runProgressShowsWhatIsCollectedAndWhatIsLeft() {
        val linked = fresh.copy(walletLinked = true, runProgressMeters = 600.0)
        assertEquals(0.6f, linked.progressFraction, 0.0001f)
        assertEquals(400.0, linked.metersToNextPremium, 0.0)
        assertEquals("0.6", DrawDistance.progressKm(linked.runProgressMeters))
        assertEquals("0.4", DrawDistance.remainingKm(linked.metersToNextPremium))
        assertEquals("1", DrawDistance.stepKm(linked.runStepMeters))
        // 999m 를 "1.0"이라 하지 않고, 남은 1m 를 "0"이라 하지 않는다
        assertEquals("0.9", DrawDistance.progressKm(999.0))
        assertEquals("0.1", DrawDistance.remainingKm(1.0))
        assertEquals("0", DrawDistance.progressKm(0.0))
        assertEquals("1.5", DrawDistance.stepKm(1500))
    }

    @Test fun dailyRunCapIsReachedOnlyWithACap() {
        assertTrue(fresh.copy(runToday = 10).runCapReached)
        assertFalse(fresh.copy(runToday = 9).runCapReached)
        assertFalse("no cap configured", fresh.copy(runDailyCap = 0, runToday = 5).runCapReached)
    }
}
