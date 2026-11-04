package com.stepup.android.domain

import java.net.URLEncoder

/**
 * WEB3 지갑 연결(2026-10-18 사용자 "WalletConnect 빼고 OKX · MetaMask · Rabby · GIWA(맨 위, 준비 중) 4개").
 *
 * 앱이 서버에서 일회용 확인 번호(10분)를 받아, 그 번호가 든 연결 페이지(web/wallet.html?l=계정.번호)를 지갑 앱 **안의 브라우저**로 연다.
 * 지갑 브라우저에는 지갑이 들어 있어 서명 한 번이면 어테스터가 확인해 붙인다. 로그인 토큰은 지갑 앱에 넘기지 않는다.
 */
enum class WalletApp(val key: String, val packageName: String?, val ready: Boolean) {
    /** GIWA Wallet — 맨 위, 아직 준비 중(누를 수 없다) */
    GIWA("giwa", null, ready = false),
    METAMASK("metamask", "io.metamask", ready = true),
    OKX("okx", "com.okinc.okex.gp", ready = true),
    /** Rabby 는 앱 브라우저로 바로 여는 공개 링크가 없다 — 링크를 복사하고 앱을 연다(브라우저에 붙여 넣기) */
    RABBY("rabby", "com.debank.rabbymobile", ready = true);

    /** 연결 페이지를 이 지갑의 앱 안 브라우저로 여는 주소. null 이면 링크를 복사해 붙여 넣는 길 */
    fun openUrl(page: String): String? = when (this) {
        // MetaMask 공식 — https://link.metamask.io/dapp/{주소(https:// 뺀)} (docs.metamask.io "Use deeplinks")
        METAMASK -> "https://link.metamask.io/dapp/" + page.removePrefix("https://")
        // OKX — okx://wallet/dapp/url?dappUrl={주소, 인코딩}
        OKX -> "okx://wallet/dapp/url?dappUrl=" + URLEncoder.encode(page, "UTF-8")
        GIWA, RABBY -> null
    }

    /** 앱이 없을 때 — 플레이 스토어 */
    fun storeUrl(): String? = packageName?.let { "https://play.google.com/store/apps/details?id=$it" }

    companion object {
        /**
         * 지갑 고르기 창에 보일 지갑. 블록체인 판(WEB3)은 넷 다(GIWA 는 "준비 중"으로), 스토어 판은 준비된 지갑만 —
         * 쓸 수 없는 "준비 중" 줄을 스토어 판에 두지 않는다.
         */
        fun shown(web3: Boolean): List<WalletApp> = if (web3) entries else entries.filter { it.ready }
    }
}

/** 서버가 준 지갑 연결 문장에서 꺼낸 계정 번호 · 확인 번호 */
data class WalletLinkRequest(val user: String, val nonce: String) {
    /** 연결 페이지 주소 — [base] 는 웹 지갑 페이지(https). 꼴이 맞지 않으면 null */
    fun pageUrl(base: String): String? {
        val page = base.trim().substringBefore('#').substringBefore('?')
        if (!page.startsWith("https://")) return null
        return "$page?l=$user.$nonce"
    }

    companion object {
        private val ACCOUNT = Regex("계정: ([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})")
        private val NONCE = Regex("확인 번호: ([0-9a-f]{32})")

        /** 서버 economy.wallet_link_message 문장에서 읽는다 */
        fun parse(message: String): WalletLinkRequest? {
            val user = ACCOUNT.find(message)?.groupValues?.get(1) ?: return null
            val nonce = NONCE.find(message)?.groupValues?.get(1) ?: return null
            return WalletLinkRequest(user, nonce)
        }
    }
}

/** 연결된 지갑 주소(서버 my_wallet — 0x 로 시작하는 40자리 16진수) */
object WalletAddress {
    /**
     * 화면에 보일 짧은 꼴 — "0x1234…abcd"(앞 6자 · 뒤 4자). 이미 짧거나 비었으면 그대로.
     * 복사는 늘 원래 주소 전체로 한다.
     */
    fun short(address: String): String {
        val raw = address.trim()
        return if (raw.length <= 12) raw else raw.take(6) + "…" + raw.takeLast(4)
    }

    /** 서버가 준 값이 주소 꼴인가 — 아니면 화면에 두지 않는다 */
    fun looksValid(address: String?): Boolean = address != null && ADDRESS.matches(address.trim())

    private val ADDRESS = Regex("0x[0-9a-fA-F]{40}")
}
