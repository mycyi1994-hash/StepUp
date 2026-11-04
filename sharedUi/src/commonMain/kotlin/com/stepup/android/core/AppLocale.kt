package com.stepup.android.core

/**
 * 고른 화면 언어(안드로이드 core/AppLocale.kt 와 같은 값 · 같은 규칙, 2026-11-03 iOS C4). 저장 값: "" = 시스템 기본 · en · ko · zh · ja.
 * 기기에 적용하는 일만 기기별 — 안드로이드는 앱별 언어(LocaleManager), iOS 는 다음 실행부터(AppleLanguages).
 */
object AppLocale {
    const val SYSTEM = ""
    val SUPPORTED: List<String> = listOf("en", "ko", "zh", "ja")

    /** 현재 선택된 언어 태그. 빈 문자열이면 시스템 기본 */
    var tag: String = SYSTEM
        private set

    fun bootstrap(saved: String) {
        tag = normalize(saved)
    }

    /** 언어를 바꾼다. @return 앱을 다시 켜야 바뀌면 true(iOS) */
    fun change(next: String): Boolean {
        tag = normalize(next)
        return applyAppLanguage(tag)
    }

    private fun normalize(raw: String): String {
        val language = raw.substringBefore('-').lowercase()
        return if (language in SUPPORTED) language else SYSTEM
    }
}

/** 기기에 언어를 적용한다 — 다시 켜야 바뀌면 true */
expect fun applyAppLanguage(tag: String): Boolean
