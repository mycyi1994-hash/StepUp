package com.stepup.android.core

/**
 * 웹 지갑 페이지(web/wallet.html) 주소.
 *
 * 로그인 토큰은 주소의 `#` 뒤에 붙인다. `#` 뒤는 브라우저가 서버로 보내지 않고,
 * 페이지는 읽자마자 주소창에서 지운다. 그래서 앱에서 누르면 다시 로그인하지 않고
 * 같은 계정으로 열린다. 토큰은 한 시간 뒤 저절로 만료된다.
 */
object WalletPage {
    fun url(base: String, accessToken: String): String? {
        val page = base.trim().substringBefore('#')
        if (page.isEmpty() || !page.startsWith("https://")) return null
        // JWT 는 base64url 세 조각이라 주소에 그대로 들어가지만, 다른 글자가 섞이면 싣지 않는다
        if (accessToken.isEmpty() || !accessToken.all { it.isLetterOrDigit() || it in "-_." }) return null
        return "$page#t=$accessToken"
    }
}
