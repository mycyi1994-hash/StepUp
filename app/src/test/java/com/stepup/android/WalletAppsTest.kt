package com.stepup.android

import com.stepup.android.domain.WalletApp
import com.stepup.android.domain.WalletLinkRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** WEB3 지갑 연결(2026-10-18) — 지갑 넷의 순서 · 여는 주소 · 서버 문장 읽기 */
class WalletAppsTest {
    private val user = "f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1"
    private val nonce = "ab".repeat(16)
    // 서버 economy.wallet_link_message 와 같은 꼴
    private val message = "StepUp 지갑 연결\n계정: $user\n확인 번호: $nonce\n\n이 서명은 거래가 아니며 수수료가 들지 않습니다."

    @Test fun `GIWA 가 맨 위 · 준비 중, 그다음 MetaMask · OKX · Rabby`() {
        assertEquals(listOf(WalletApp.GIWA, WalletApp.METAMASK, WalletApp.OKX, WalletApp.RABBY), WalletApp.entries)
        assertFalse(WalletApp.GIWA.ready)
        assertTrue(WalletApp.entries.drop(1).all { it.ready })
        assertNull(WalletApp.GIWA.storeUrl())
    }

    @Test fun `서버 문장에서 계정 · 확인 번호를 읽고 연결 페이지 주소를 만든다`() {
        val request = requireNotNull(WalletLinkRequest.parse(message))
        assertEquals(WalletLinkRequest(user, nonce), request)
        assertEquals("https://stepupcrew.com/wallet.html?l=$user.$nonce", request.pageUrl("https://stepupcrew.com/wallet.html#old"))
        assertNull(request.pageUrl("http://stepupcrew.com/wallet.html"))
        assertNull(WalletLinkRequest.parse("StepUp 지갑 연결\n계정: x\n확인 번호: $nonce"))
        assertNull(WalletLinkRequest.parse("StepUp 지갑 연결\n계정: $user\n확인 번호: 123"))
    }

    @Test fun `지갑 앱 브라우저로 여는 주소 — MetaMask 는 공식 dapp 링크, OKX 는 인코딩, Rabby 는 복사`() {
        val page = "https://stepupcrew.com/wallet.html?l=$user.$nonce"
        assertEquals("https://link.metamask.io/dapp/stepupcrew.com/wallet.html?l=$user.$nonce", WalletApp.METAMASK.openUrl(page))
        assertEquals(
            "okx://wallet/dapp/url?dappUrl=https%3A%2F%2Fstepupcrew.com%2Fwallet.html%3Fl%3D$user.$nonce",
            WalletApp.OKX.openUrl(page),
        )
        assertNull(WalletApp.RABBY.openUrl(page))
        assertNull(WalletApp.GIWA.openUrl(page))
        assertEquals("https://play.google.com/store/apps/details?id=com.debank.rabbymobile", WalletApp.RABBY.storeUrl())
    }
}
