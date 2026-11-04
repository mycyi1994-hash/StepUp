package com.stepup.android.core

/**
 * 앱이 지금 실제로 보이는 언어 — "ko" · "en" · "ja" · "zh" 중 하나.
 *
 * 서버에 알리는 푸시 글 언어와 바깥 문서(약관 · 개인정보처리방침의 영어 본문)를 화면 글자와 맞추는 데 쓴다.
 * 고른 언어(앱 안에서 고름 · 안드로이드 13 이상은 시스템의 앱별 언어 — [AppLocale.tag])가 있으면 그것, 없으면 기기 언어다.
 * 앱에 없는 언어(프랑스어 등)는 기본 글자(values/ — 영어)로 보이므로 "en" 이다. 서버는 모르는 언어를 한국어로 보내기 때문에
 * 기기 언어를 그대로 넘기면 영어 화면을 쓰는 사람에게 한국어 푸시가 간다.
 */
object ShownLanguage {

    /** 앱에 담긴 언어(build.gradle 의 localeFilters · xml/locales_config 와 같다) */
    private val SHIPPED = setOf("ko", "en", "ja", "zh")

    /** 앱에 없는 언어일 때 화면이 쓰는 기본 글자(values/)의 언어 */
    private const val FALLBACK = "en"

    /**
     * @param picked 고른 언어 태그. 비었으면 기기 설정을 따른다
     * @param device 기기 언어(`Locale.getDefault().language`) — "ko", "ko-KR", "zh_TW" 처럼 지역이 붙어도 된다
     */
    fun of(picked: String, device: String): String {
        val chosen = base(picked)
        if (chosen in SHIPPED) return chosen
        val fromDevice = base(device)
        return if (fromDevice in SHIPPED) fromDevice else FALLBACK
    }

    private fun base(tag: String): String = tag.trim().substringBefore('-').substringBefore('_').lowercase()
}
