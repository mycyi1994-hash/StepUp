package com.stepup.android

import com.stepup.android.data.repo.MarketOutcome
import com.stepup.android.data.repo.MarketRefusal
import com.stepup.android.data.repo.marketRefusalKm
import com.stepup.android.ui.screens.market.MarketMessage
import com.stepup.android.ui.screens.market.say
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 거래소 거절 문장(서버 market_* 의 raise · 앱 쪽 문장)을 번역된 문구 갈래로 — 영어 · 일본어 · 중국어 화면에 한국어가 뜨지 않게 */
class MarketRefusalTest {

    @Test
    fun `서버가 보낸 거래소 거절 문장을 갈래로 옮긴다`() {
        assertEquals(MarketRefusal.NOT_ENOUGH_SUP, MarketRefusal.of("SUP 가 모자랍니다"))
        assertEquals(MarketRefusal.PRICE_TOO_LOW, MarketRefusal.of("값이 너무 낮습니다"))
        assertEquals(MarketRefusal.PRICE_INVALID, MarketRefusal.of("값이 올바르지 않습니다"))
        assertEquals(MarketRefusal.ALREADY_LISTED, MarketRefusal.of("이미 판매 중입니다"))
        assertEquals(MarketRefusal.LISTING_CLOSED, MarketRefusal.of("이미 끝난 매물입니다"))
        assertEquals(MarketRefusal.OWN_LISTING, MarketRefusal.of("내 매물은 살 수 없습니다"))
        assertEquals(MarketRefusal.BID_CLOSED, MarketRefusal.of("이미 끝난 입찰입니다"))
        assertEquals(MarketRefusal.OWN_BID, MarketRefusal.of("내 입찰에는 팔 수 없습니다"))
        assertEquals(MarketRefusal.MODEL_MISMATCH, MarketRefusal.of("모델이 다릅니다"))
        assertEquals(MarketRefusal.LEVEL_TOO_LOW, MarketRefusal.of("레벨이 모자랍니다"))
        assertEquals(MarketRefusal.NOT_MINE, MarketRefusal.of("내 스니커즈가 아닙니다"))
        assertEquals(MarketRefusal.IMPORT_LIMIT, MarketRefusal.of("올릴 수 있는 수를 넘었습니다 (최대 50)"))
        assertEquals(MarketRefusal.LOCKED_SHOE, MarketRefusal.of("착용 중이거나 체인에 있는 신발은 팔 수 없습니다"))
        assertEquals(MarketRefusal.NOT_TRANSFERABLE, MarketRefusal.of("넘길 수 없는 신발입니다"))
        assertEquals(MarketRefusal.RUN_MORE, MarketRefusal.of("이 신발로 3.5km 를 더 달려야 넘길 수 있습니다"))
        assertEquals(MarketRefusal.SIGN_IN, MarketRefusal.of("로그인이 필요합니다"))
    }

    @Test
    fun `앱 쪽 문장과 모르는 문장은 일반 안내로`() {
        assertEquals(MarketRefusal.NO_SNEAKER, MarketRefusal.of("없는 스니커즈입니다"))
        assertEquals(MarketRefusal.OTHER, MarketRefusal.of("알 수 없는 문제"))
        assertEquals(MarketRefusal.OTHER, MarketRefusal.of("요청이 거절되었습니다 (403)"))
        assertEquals(MarketRefusal.OTHER, MarketRefusal.of(""))
    }

    @Test
    fun `남은 km 를 꺼낸다`() {
        assertEquals("3.5", marketRefusalKm("이 신발로 3.5km 를 더 달려야 넘길 수 있습니다"))
        assertEquals("12", marketRefusalKm("이 신발로 12 km 를 더 달려야 넘길 수 있습니다"))
        assertNull(marketRefusalKm("넘길 수 없는 신발입니다"))
    }

    @Test
    fun `걸어 두기만 한 내놓기 · 입찰도 알리고, 거두기는 말없이`() {
        assertEquals(MarketMessage.Res(R.string.market_msg_listed), MarketOutcome.Ok(traded = false).say(R.string.market_msg_listed))
        assertEquals(MarketMessage.Res(R.string.market_msg_bid_placed), MarketOutcome.Ok(traded = false).say(R.string.market_msg_bid_placed))
        assertNull(MarketOutcome.Ok(traded = false).say())
        assertEquals(MarketMessage.Res(R.string.market_msg_traded), MarketOutcome.Ok(traded = true).say(R.string.market_msg_listed))
        assertEquals(MarketMessage.Res(R.string.market_err_not_enough_sup), MarketOutcome.Failed("SUP 가 모자랍니다").say())
        assertEquals(MarketMessage.Res(R.string.market_err_run_more, "3.5"), MarketOutcome.Failed("이 신발로 3.5km 를 더 달려야 넘길 수 있습니다").say())
        assertEquals(MarketMessage.Res(R.string.market_err_other), MarketOutcome.Failed("요청이 거절되었습니다 (403)").say())
    }
}
