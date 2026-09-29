package com.stepup.android

import com.stepup.android.domain.DrawKind
import com.stepup.android.domain.DrawStatus
import com.stepup.android.ui.screens.gacha.DrawCardAction
import com.stepup.android.ui.screens.gacha.DrawCardSub
import com.stepup.android.ui.screens.gacha.DrawCardTitle
import com.stepup.android.ui.screens.gacha.DrawChip
import com.stepup.android.ui.screens.gacha.DrawRow
import com.stepup.android.ui.screens.gacha.DrawRowLabel
import com.stepup.android.ui.screens.gacha.DrawSheet
import com.stepup.android.ui.screens.gacha.drawHomeCard
import com.stepup.android.ui.screens.gacha.drawInfoSheet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 신발 뽑기 디자인(26장) 메인 — 고른 글자 탭 하나의 무대 · 남은 횟수 줄 · 버튼 하나를 서버 값에서 고르는 규칙.
 * 모르는 수는 "—"(0 이 아니다), 결과를 모르는 요청이 있으면 두 탭 모두 새 뽑기 대신 결과 확인이다.
 */
class ShoeDrawHomeTest {
    private val fresh = DrawStatus(
        dailyLeft = 3, dailyTotal = 3, signupLeft = 10, signupGranted = 10, walletLinked = false, giftOnLink = 10,
        giftLeft = 0, runLeft = 0, genesisLeft = 0, runProgressMeters = 0.0, runStepMeters = 1000,
        runToday = 0, runDailyCap = 10, chainPaused = false,
    )
    private val linked = fresh.copy(walletLinked = true, giftOnLink = 0, giftLeft = 10, genesisLeft = 1, runProgressMeters = 600.0)

    private fun card(status: DrawStatus?, tab: DrawKind, pending: DrawKind? = null, loading: Boolean = false, signedOut: Boolean = false) =
        drawHomeCard(status, tab, pending, loading, signedOut)

    @Test fun freeTabShowsTheServerCountAndOneDrawButton() {
        val free = card(fresh, DrawKind.FREE)
        assertEquals(DrawKind.FREE, free.kind)
        assertEquals("매일 N회 무료 — dailyTotal 이 있을 때만", DrawCardTitle.DailyFree, free.title)
        assertNull(free.sub)
        assertEquals(DrawChip.Count(13), free.chip)
        assertEquals(DrawRow.Count(DrawRowLabel.FreeLeft, 13), free.row)
        assertEquals(DrawCardAction.Draw, free.action)
        assertFalse(free.dim)
        // 매일 무료가 없는 서버 값이면 "매일 0회 무료"라고 쓰지 않는다
        assertEquals(DrawCardTitle.Free, card(fresh.copy(dailyLeft = 0, dailyTotal = 0), DrawKind.FREE).title)
        // 무료를 다 썼다 — 뽑기 대신 안내(13)
        val used = card(fresh.copy(dailyLeft = 0, signupLeft = 0), DrawKind.FREE)
        assertEquals(DrawRow.Count(DrawRowLabel.FreeLeft, 0), used.row)
        assertEquals(DrawCardAction.FreeInfo, used.action)
    }

    @Test fun premiumTabFollowsTheWalletAndServiceState() {
        // 09 — 연결한 적이 없다: 수 대신 지갑 그림, 줄은 받을 수 있는 첫 연결 선물(가진 횟수가 아니다)
        val connect = card(fresh, DrawKind.PREMIUM)
        assertEquals(DrawCardSub.WalletNeeded, connect.sub)
        assertEquals(DrawChip.Wallet, connect.chip)
        assertEquals(DrawRow.Gift(10), connect.row)
        assertEquals(DrawCardAction.Connect, connect.action)
        // 02 — 연결됨 · 기회 있음
        val ready = card(linked, DrawKind.PREMIUM)
        assertEquals(DrawCardSub.Linked, ready.sub)
        assertEquals(DrawChip.Count(10), ready.chip)
        assertEquals(DrawRow.Count(DrawRowLabel.PremiumLeft, 10), ready.row)
        assertEquals(DrawCardAction.Draw, ready.action)
        // 14 — 연결됨 · 기회 없음: 러닝하고 기회 받기
        assertEquals(DrawCardAction.RunInfo, card(linked.copy(giftLeft = 0, runLeft = 0), DrawKind.PREMIUM).action)
        // 12 — 연결했다가 해제: 가진 기회는 그대로 보이고 다시 연결(선물 줄이 없다)
        val reconnect = card(linked.copy(walletLinked = false), DrawKind.PREMIUM)
        assertEquals(DrawCardSub.WalletNeeded, reconnect.sub)
        assertEquals(DrawRow.Count(DrawRowLabel.PremiumKept, 10), reconnect.row)
        assertEquals(DrawCardAction.Reconnect, reconnect.action)
        // 23 — 일시 중단: 수는 그대로, 버튼은 누를 수 없는 안내
        val paused = card(linked.copy(chainPaused = true), DrawKind.PREMIUM)
        assertEquals(DrawCardSub.Paused, paused.sub)
        assertEquals(DrawChip.Count(10), paused.chip)
        assertEquals(DrawCardAction.Paused, paused.action)
    }

    @Test fun unknownCountsAreADashNeverZero() {
        for (tab in DrawKind.entries) {
            // 03 — 불러오는 중: 흐린 상자 · 돌아가는 표시, 버튼은 누를 수 없다
            val loading = card(null, tab, loading = true)
            assertEquals(DrawChip.Unknown, loading.chip)
            assertTrue(loading.row is DrawRow.Unknown)
            assertEquals(DrawCardAction.Loading, loading.action)
            assertTrue(loading.dim && loading.loading)
            // 21 — 불러오기 실패: 소진(0)과 다르다, 다시 불러오기
            val failed = card(null, tab)
            assertEquals(DrawChip.Unknown, failed.chip)
            assertTrue(failed.row is DrawRow.Unknown)
            assertEquals(DrawCardAction.Reload, failed.action)
            assertTrue(failed.dim)
            assertFalse(failed.loading)
            // 22 — 로그인 전: 수가 없고 로그인으로
            val signedOut = card(null, tab, signedOut = true)
            assertEquals(DrawCardTitle.SignedOut, signedOut.title)
            assertEquals(DrawChip.None, signedOut.chip)
            assertTrue(signedOut.row is DrawRow.Unknown)
            assertEquals(DrawCardAction.SignIn, signedOut.action)
            // 어느 경우에도 0 을 꾸며 보이지 않는다
            listOf(loading, failed, signedOut).forEach { c ->
                assertFalse(c.chip is DrawChip.Count)
                assertFalse(c.row is DrawRow.Count)
            }
        }
    }

    @Test fun anUnknownResultBlocksNewDrawsOnBothTabs() {
        // 20 — 무료 요청의 결과를 아직 모른다: 무료 탭은 "뽑은 신발 · 확인 중"
        val same = card(linked, DrawKind.FREE, pending = DrawKind.FREE)
        assertEquals(DrawCardTitle.Pending, same.title)
        assertEquals(DrawChip.Checking, same.chip)
        assertEquals(DrawRow.Pending, same.row)
        assertEquals(DrawCardAction.Check, same.action)
        // 상급 탭은 수를 보이되 새 뽑기 대신 결과 확인
        val other = card(linked, DrawKind.PREMIUM, pending = DrawKind.FREE)
        assertEquals(DrawRow.Count(DrawRowLabel.PremiumLeft, 10), other.row)
        assertEquals(DrawCardAction.Check, other.action)
        // 기회가 없거나 지갑 전이어도 막는다(안내 · 연결 버튼이 뽑기를 부르지 않게 결과부터)
        assertEquals(DrawCardAction.Check, card(fresh, DrawKind.PREMIUM, pending = DrawKind.FREE).action)
        assertEquals(DrawCardAction.Check, card(linked.copy(dailyLeft = 0, signupLeft = 0), DrawKind.FREE, pending = DrawKind.PREMIUM).action)
    }

    @Test fun theInfoButtonOpensTheSelectedTabsSheet() {
        assertEquals(DrawSheet.FreeChances, drawInfoSheet(fresh, DrawKind.FREE))
        // 지갑을 연결한 적이 없으면 상급 내역 대신 받는 방법(17)
        assertEquals(DrawSheet.Rules, drawInfoSheet(fresh, DrawKind.PREMIUM))
        assertEquals(DrawSheet.PremiumChances, drawInfoSheet(linked, DrawKind.PREMIUM))
        assertEquals(DrawSheet.PremiumChances, drawInfoSheet(linked.copy(walletLinked = false), DrawKind.PREMIUM))
        assertEquals(DrawSheet.PremiumChances, drawInfoSheet(linked.copy(chainPaused = true), DrawKind.PREMIUM))
    }
}
