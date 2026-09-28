package com.stepup.android

import com.stepup.android.data.repo.EconomyOutcome
import com.stepup.android.domain.DrawKind
import com.stepup.android.domain.DrawStatus
import com.stepup.android.domain.Faction
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.screens.gacha.DrawCheck
import com.stepup.android.ui.screens.gacha.DrawStop
import com.stepup.android.ui.screens.gacha.DrawnShoe
import com.stepup.android.ui.screens.gacha.LinkWatch
import com.stepup.android.ui.screens.gacha.PendingDraw
import com.stepup.android.ui.screens.gacha.PremiumMode
import com.stepup.android.ui.screens.gacha.judgePending
import com.stepup.android.ui.screens.gacha.linkedGift
import com.stepup.android.ui.screens.gacha.origin
import com.stepup.android.ui.screens.gacha.premiumMode
import com.stepup.android.ui.screens.gacha.retryable
import com.stepup.android.ui.screens.gacha.runChancesAdded
import com.stepup.android.ui.screens.gacha.toDrawStop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 신발 뽑기 v2(두 칸) — 화면이 서버 값에서 장면을 고르는 규칙. 수를 만들지 않고, 모르는 것을 단정하지 않는다.
 * (서버 규칙 자체는 supabase/tests 의 0042 검사가 본다.)
 */
class ShoeDrawFlowTest {
    private val fresh = DrawStatus(
        dailyLeft = 3, dailyTotal = 3, signupLeft = 10, signupGranted = 10, walletLinked = false, giftOnLink = 10,
        giftLeft = 0, runLeft = 0, genesisLeft = 0, runProgressMeters = 0.0, runStepMeters = 1000,
        runToday = 0, runDailyCap = 10, chainPaused = false,
    )
    private val linked = fresh.copy(walletLinked = true, giftOnLink = 0, giftLeft = 10, genesisLeft = 1, runProgressMeters = 600.0)
    private val shoe = Sneaker(
        id = 42, faction = Faction.WIND, rarity = Rarity.RARE, variant = 1, level = 1, mintNumber = 7, luck = 1.0,
        comfort = 1.0, durability = 100, equipped = false, acquiredAt = 0,
    )

    @Test fun premiumCompartmentFollowsTheServerWalletState() {
        // 01 연결 전(첫 연결 선물이 남아 있다) · 02 연결 · 기회 있음 · 15 기회 없음
        assertEquals(PremiumMode.Connect, fresh.premiumMode())
        assertEquals(PremiumMode.Ready, linked.premiumMode())
        assertEquals(PremiumMode.Empty, linked.copy(giftLeft = 0, runLeft = 0).premiumMode())
        // 24 — 연결했다가 해제됐다(gift_on_link 0): 받은 기회는 그대로, 다시 연결
        val unlinked = linked.copy(walletLinked = false, giftLeft = 4)
        assertEquals(PremiumMode.Reconnect, unlinked.premiumMode())
        assertFalse("no premium draw without a wallet", unlinked.canDraw(DrawKind.PREMIUM))
        assertEquals(PremiumMode.Paused, linked.copy(chainPaused = true).premiumMode())
    }

    @Test fun anUnknownRequestIsJudgedOnlyWithServerValues() {
        val pending = PendingDraw(DrawKind.FREE, newestBefore = 40, leftBefore = 13, startedAt = 0, account = "u")
        // 새 신발이 보이면 그것이 결과다
        assertEquals(DrawCheck.Found(shoe), judgePending(pending, shoe, fresh.copy(dailyLeft = 2)))
        // 새 신발이 없고 수도 그대로 — 시작되지 않았다(19)
        assertEquals(DrawCheck.NotStarted, judgePending(pending, null, fresh))
        // 수는 줄었는데 신발이 보이지 않는다 — 단정하지 않는다(20 에 머문다)
        assertEquals(DrawCheck.Unknown, judgePending(pending, null, fresh.copy(dailyLeft = 2)))
        // 현황을 다시 읽지 못했다 — 모른다
        assertEquals(DrawCheck.Unknown, judgePending(pending, null, null))
        // 서버가 번호를 이미 돌려줬다 — 뽑기는 됐다. 목록에 없다고 "시작 안 됨"이라 하지 않는다
        assertEquals(DrawCheck.Unknown, judgePending(pending.copy(shoeId = 43), null, fresh))
        // 자정에 오늘 몫이 새로 생겨 수가 늘었어도 새 신발이 없으면 시작되지 않은 것이다
        assertEquals(DrawCheck.NotStarted, judgePending(pending, null, fresh.copy(dailyLeft = 3, signupLeft = 11)))
    }

    @Test fun drawnShoesAreFoundByTheirServerOrigin() {
        assertEquals("FREE_DRAW", DrawKind.FREE.origin())
        assertEquals("BONUS_DRAW", DrawKind.PREMIUM.origin())
    }

    @Test fun refusalsNeverOfferARetryThatWouldFailTheSameWay() {
        assertEquals(DrawStop.NoFree, EconomyOutcome.NoFreeDraws.toDrawStop())
        assertEquals(DrawStop.NoPremium, EconomyOutcome.NoPremiumDraws.toDrawStop())
        assertEquals(DrawStop.Wallet, EconomyOutcome.WalletRequired.toDrawStop())
        assertEquals(DrawStop.MintLimit, EconomyOutcome.MintLimitReached.toDrawStop())
        assertEquals(DrawStop.SignIn, EconomyOutcome.SignInRequired.toDrawStop())
        assertEquals(DrawStop.Other, EconomyOutcome.Rejected("x").toDrawStop())
        assertFalse(DrawStop.NoFree.retryable)
        assertFalse(DrawStop.Wallet.retryable)
        assertFalse(DrawStop.MintLimit.retryable)
        assertTrue(DrawStop.Network.retryable)
    }

    @Test fun runRewardIsAnnouncedOnlyWhenTheServerCountGrew() {
        assertEquals("first sight is not news", 0, runChancesAdded(null, linked.copy(runLeft = 3)))
        assertEquals(1, runChancesAdded(2, linked.copy(runLeft = 3)))
        assertEquals("used by a draw", 0, runChancesAdded(3, linked.copy(runLeft = 2)))
        assertEquals("no wallet, no running chances", 0, runChancesAdded(0, fresh.copy(runLeft = 1)))
    }

    @Test fun walletLinkIsShownOnlyAfterTheServerSaysSo() {
        val before = LinkWatch(linked = false, giftLeft = 0)
        assertNull("still not linked", linkedGift(before, fresh))
        assertEquals("first link gift", 10, linkedGift(before, linked))
        // 다시 연결 — 선물은 다시 주지 않는다
        assertEquals(0, linkedGift(LinkWatch(linked = false, giftLeft = 4), linked.copy(giftLeft = 4)))
        assertNull("nothing was watched", linkedGift(null, linked))
        assertNull("was already linked", linkedGift(LinkWatch(linked = true, giftLeft = 10), linked))
    }

    @Test fun theLastChanceResultNeedsAConfirmedZero() {
        assertTrue(DrawnShoe(DrawKind.FREE, shoe, left = 0).lastChance)
        assertFalse(DrawnShoe(DrawKind.FREE, shoe, left = 12).lastChance)
        assertFalse("unknown is not zero", DrawnShoe(DrawKind.FREE, shoe, left = null).lastChance)
    }
}
