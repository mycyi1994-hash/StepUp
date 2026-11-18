package com.stepup.android

import com.stepup.android.core.InviteLinks
import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.EconomyApi
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.domain.DrawKind
import com.stepup.android.domain.DrawStatus
import com.stepup.android.ui.screens.gacha.DrawCardAction
import com.stepup.android.ui.screens.gacha.DrawCardTitle
import com.stepup.android.ui.screens.gacha.DrawCheck
import com.stepup.android.ui.screens.gacha.DrawChip
import com.stepup.android.ui.screens.gacha.DrawRow
import com.stepup.android.ui.screens.gacha.DrawRowLabel
import com.stepup.android.ui.screens.gacha.DrawSheet
import com.stepup.android.ui.screens.gacha.DrawStop
import com.stepup.android.ui.screens.gacha.PendingDraw
import com.stepup.android.ui.screens.gacha.PremiumMode
import com.stepup.android.ui.screens.gacha.drawHomeCard
import com.stepup.android.ui.screens.gacha.drawInfoSheet
import com.stepup.android.ui.screens.gacha.judgePending
import com.stepup.android.ui.screens.gacha.premiumMode
import com.stepup.android.ui.screens.gacha.premiumPanelShown
import com.stepup.android.ui.screens.gacha.retryable
import com.stepup.android.ui.screens.gacha.shownIn
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 앱 업데이트 보상(서버 0112) — 선물로 받은 상급 뽑기(GIFT, draw_status_v2 의 reward_left)는 지갑 없이 쓴다.
 * 스토어 판(WEB3 = false)은 상급 칸에 선물만 보이고(지갑 · 러닝 · 발행 안내 없이), 선물이 없으면 칸을 두지 않는다.
 * 거래소 거두기 · 업데이트 보상 알림은 신발 탭 · 신발 뽑기로 연다.
 */
class UpdateRewardDrawTest {
    private val fresh = DrawStatus(
        dailyLeft = 3, dailyTotal = 3, signupLeft = 15, signupGranted = 15, walletLinked = false, giftOnLink = 10,
        giftLeft = 0, runLeft = 0, genesisLeft = 0, runProgressMeters = 0.0, runStepMeters = 1000,
        runToday = 0, runDailyCap = 10, chainPaused = false,
    )
    private val gifted = fresh.copy(rewardLeft = 1)
    private val linked = fresh.copy(walletLinked = true, giftOnLink = 0, giftLeft = 10, genesisLeft = 1)

    @Test fun `gifted premium draws need no wallet and count toward what is left`() {
        assertEquals("예전 서버(draw_status)는 선물 칸이 없다 — 0", 0, fresh.rewardLeft)
        assertFalse(fresh.canDraw(DrawKind.PREMIUM))
        assertTrue("지갑 없이 선물로 뽑는다", gifted.canDraw(DrawKind.PREMIUM))
        assertTrue("체인이 멈춰도 선물로 뽑는다", gifted.copy(walletLinked = true, chainPaused = true).canDraw(DrawKind.PREMIUM))
        assertEquals(1, gifted.premiumLeft)
        assertEquals(12, linked.copy(runLeft = 1, rewardLeft = 1).premiumLeft)
        assertEquals(11, linked.copy(runLeft = 1, rewardLeft = 1).walletPremiumLeft)
    }

    @Test fun `the store build only sees and uses gifted draws`() {
        // 스토어 판에는 지갑 혜택이 없다 — 예전에 지갑을 연결한 계정의 선물 · 러닝 기회는 보이지도 쓰이지도 않는다(서버는 선물부터 쓴다)
        assertFalse(linked.canDraw(DrawKind.PREMIUM, web3 = false))
        assertTrue(linked.canDraw(DrawKind.PREMIUM, web3 = true))
        assertTrue(linked.copy(rewardLeft = 1).canDraw(DrawKind.PREMIUM, web3 = false))
        assertEquals(1, linked.copy(rewardLeft = 1).shownLeft(DrawKind.PREMIUM, web3 = false))
        assertEquals(11, linked.copy(rewardLeft = 1).shownLeft(DrawKind.PREMIUM, web3 = true))
        assertEquals(18, gifted.shownLeft(DrawKind.FREE, web3 = false))
    }

    @Test fun `premium mode picks the gift compartment when the wallet cannot draw`() {
        assertEquals(PremiumMode.Gift, gifted.premiumMode(web3 = false))
        assertEquals("스토어 판은 선물이 없어도 지갑 칸이 아니다", PremiumMode.Gift, fresh.premiumMode(web3 = false))
        assertEquals("연결 전 · 선물 있음", PremiumMode.Gift, gifted.premiumMode())
        assertEquals("연결 전 · 선물 없음은 예전처럼", PremiumMode.Connect, fresh.premiumMode())
        assertEquals("일시 중단 · 선물 있음", PremiumMode.Gift, linked.copy(chainPaused = true, rewardLeft = 1).premiumMode())
        assertEquals(PremiumMode.Paused, linked.copy(chainPaused = true).premiumMode())
        assertEquals("연결됨 · 선물 있음 — 모두 한 칸에", PremiumMode.Ready, linked.copy(rewardLeft = 1).premiumMode())
        assertEquals("해제 · 선물 있음", PremiumMode.Gift, linked.copy(walletLinked = false, rewardLeft = 1).premiumMode())
    }

    @Test fun `store premium card shows only the gift count and one draw button`() {
        val card = drawHomeCard(gifted, DrawKind.PREMIUM, null, loading = false, signedOut = false, web3 = false)
        assertEquals(DrawCardTitle.Premium, card.title)
        assertNull("지갑 상태 줄이 없다", card.sub)
        assertEquals(DrawChip.Count(1), card.chip)
        assertEquals(DrawRow.Count(DrawRowLabel.PremiumLeft, 1), card.row)
        assertEquals(DrawCardAction.Draw, card.action)
        // 지갑을 연결했던 계정도 스토어 판은 선물 수만
        val staff = drawHomeCard(linked.copy(rewardLeft = 1), DrawKind.PREMIUM, null, loading = false, signedOut = false, web3 = false)
        assertEquals(DrawChip.Count(1), staff.chip)
        assertEquals(DrawCardAction.Draw, staff.action)
        assertEquals(
            DrawCardAction.Spent,
            drawHomeCard(fresh, DrawKind.PREMIUM, null, loading = false, signedOut = false, web3 = false).action,
        )
        // 결과를 기다리는 동안은 예전처럼 확인 중
        val pending = drawHomeCard(fresh, DrawKind.PREMIUM, DrawKind.PREMIUM, loading = false, signedOut = false, web3 = false)
        assertEquals(DrawRow.Pending, pending.row)
        assertEquals(DrawCardAction.Check, pending.action)
        // 블록체인 판 — 연결 전이어도 선물이 있으면 뽑기 버튼(지갑 연결은 선물을 쓴 뒤 다시 보인다)
        assertEquals(DrawCardAction.Draw, drawHomeCard(gifted, DrawKind.PREMIUM, null, loading = false, signedOut = false).action)
        assertEquals(DrawCardAction.Connect, drawHomeCard(fresh, DrawKind.PREMIUM, null, loading = false, signedOut = false).action)
        assertEquals(DrawSheet.PremiumChances, drawInfoSheet(gifted, DrawKind.PREMIUM, web3 = false))
    }

    @Test fun `the store build shows the premium panel only while a gift is left or a premium result is pending`() {
        assertTrue(premiumPanelShown(web3 = true, status = fresh, pendingKind = null))
        assertTrue(premiumPanelShown(web3 = true, status = null, pendingKind = null))
        assertFalse("선물이 없으면 칸을 두지 않는다", premiumPanelShown(web3 = false, status = fresh, pendingKind = null))
        assertFalse("수를 모르면 두지 않는다", premiumPanelShown(web3 = false, status = null, pendingKind = null))
        assertTrue(premiumPanelShown(web3 = false, status = gifted, pendingKind = null))
        assertTrue("상급 결과를 기다리는 동안은 둔다", premiumPanelShown(web3 = false, status = fresh, pendingKind = DrawKind.PREMIUM))
        assertFalse(premiumPanelShown(web3 = false, status = fresh, pendingKind = DrawKind.FREE))
    }

    @Test fun `store refusals never mention wallets or minting`() {
        assertEquals(DrawStop.NoPremium, DrawStop.Wallet.shownIn(web3 = false))
        assertEquals(DrawStop.NoPremium, DrawStop.ChainPaused.shownIn(web3 = false))
        assertEquals(DrawStop.NoPremium, DrawStop.MintLimit.shownIn(web3 = false))
        assertFalse(DrawStop.ChainPaused.shownIn(web3 = false).retryable)
        assertEquals(DrawStop.Network, DrawStop.Network.shownIn(web3 = false))
        assertEquals(DrawStop.Wallet, DrawStop.Wallet.shownIn(web3 = true))
    }

    @Test fun `a gifted draw that lost its reply is judged by the total premium count`() {
        val marker = PendingDraw(DrawKind.PREMIUM, newestBefore = 10, leftBefore = gifted.left(DrawKind.PREMIUM), startedAt = 0, account = "A")
        // 선물을 썼다(수가 줄었다) — 신발이 아직 안 보이면 단정하지 않는다
        assertEquals(DrawCheck.Unknown, judgePending(marker, null, gifted.copy(rewardLeft = 0)))
        assertEquals(DrawCheck.NotStarted, judgePending(marker, null, gifted))
    }

    @Test fun `notice links open the shoe tab and the draw screen`() {
        assertEquals(InviteLinks.PushTarget.Shoes, InviteLinks.pushTargetOf("shoes"))
        assertEquals(InviteLinks.PushTarget.Draw, InviteLinks.pushTargetOf("draw"))
        assertEquals(InviteLinks.PushTarget.Draw, InviteLinks.pushTargetOf("/draw/"))
        assertNull(InviteLinks.pushTargetOf("draw/1"))
        assertNull(InviteLinks.pushTargetOf("shoe"))
    }

    // ── 서버 읽기: draw_status_v2, 없으면 예전 draw_status ─────────────────

    private class Store(var value: AuthSession?) : AuthSessionStore {
        override suspend fun load() = value
        override suspend fun save(session: AuthSession) { value = session }
        override suspend fun clear() { value = null }
    }

    private class Http(private val reply: (String) -> HttpResponse) : HttpPoster {
        val calls = mutableListOf<String>()
        override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
            calls += url.substringAfterLast('/')
            return reply(url.substringAfterLast('/'))
        }
        override suspend fun get(url: String, headers: Map<String, String>) = post(url, "", headers)
    }

    private fun api(http: Http): EconomyApi {
        val session = AuthSession(
            accessToken = "token", refreshToken = "refresh", expiresIn = 3600,
            expiresAt = 9_999_999_999L, user = AuthUser(id = "A", isAnonymous = false),
        )
        val holder = SessionHolder(SupabaseAuth("https://test.supabase.co", "test", http), Store(session), now = { 1000L })
        return EconomyApi(StepUpServer("https://test.supabase.co", "test", holder, http))
    }

    private val row = """"daily_left":3,"daily_total":3,"signup_left":15,"signup_granted":15,"wallet_linked":false,
        "gift_on_link":10,"gift_left":0,"run_left":0,"genesis_left":0,"run_progress_m":0,"run_step_m":1000,
        "run_today":0,"run_daily_cap":10,"chain_paused":false"""

    @Test fun `draw status reads the gifted count from the second version`() = runBlocking {
        val http = Http { name ->
            when (name) {
                "draw_status_v2" -> HttpResponse(200, "[{$row,\"reward_left\":1}]")
                else -> HttpResponse(404, "{}")
            }
        }
        val status = (api(http).drawStatus() as ServerResult.Ok).value
        assertEquals(1, status.rewardLeft)
        assertEquals(15, status.signupLeft)
        assertEquals(listOf("draw_status_v2"), http.calls)
    }

    @Test fun `draw status falls back to the old function on an older server`() = runBlocking {
        val http = Http { name ->
            when (name) {
                "draw_status_v2" -> HttpResponse(
                    404,
                    """{"code":"PGRST202","message":"Could not find the function public.draw_status_v2 without parameters in the schema cache"}""",
                )
                "draw_status" -> HttpResponse(200, "[{$row}]")
                else -> HttpResponse(404, "{}")
            }
        }
        val status = (api(http).drawStatus() as ServerResult.Ok).value
        assertEquals(0, status.rewardLeft)
        assertEquals(3, status.dailyLeft)
        assertEquals(listOf("draw_status_v2", "draw_status"), http.calls)
    }

    @Test fun `other refusals are not hidden behind the old function`() = runBlocking {
        val http = Http { name ->
            when (name) {
                "draw_status_v2" -> HttpResponse(400, """{"code":"42501","message":"권한이 없습니다"}""")
                else -> HttpResponse(200, "[{$row}]")
            }
        }
        val result = api(http).drawStatus()
        assertFalse(result is ServerResult.Ok)
        assertEquals(listOf("draw_status_v2"), http.calls.filter { it.startsWith("draw_status") })
    }
}
