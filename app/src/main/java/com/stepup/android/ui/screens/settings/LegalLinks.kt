package com.stepup.android.ui.screens.settings

/**
 * 이용약관 · 개인정보처리방침 주소 — 홈페이지(web/terms.html · web/privacy.html) 그대로. 문서 내용은 앱이 새로 쓰거나 요약하지 않는다.
 *
 * 한국어 화면이면 한국어 본문, 그 밖(영어 · 일본어 · 중국어)은 같은 페이지의 영어 본문(`#en`)으로 연다 — web/legal.js 가 `#en` 이면
 * 머리 · 메뉴 · 문서 언어까지 영어로 바꾼다. 일본어 · 중국어 문서는 따로 없다.
 * 앱 안 길은 설정 › 앱 정보 › 약관 및 정책(로그인 화면의 링크는 2026-10-29 사용자 결정으로 뺐다).
 */
object LegalLinks {
    const val TERMS = "https://stepupcrew.com/terms.html"
    const val PRIVACY = "https://stepupcrew.com/privacy.html"

    /** @param shown 화면 언어 — [com.stepup.android.core.ShownLanguage.of] */
    fun terms(shown: String): String = TERMS + anchor(shown)

    /** @param shown 화면 언어 — [com.stepup.android.core.ShownLanguage.of] */
    fun privacy(shown: String): String = PRIVACY + anchor(shown)

    private fun anchor(shown: String): String = if (shown == "ko") "" else "#en"
}
