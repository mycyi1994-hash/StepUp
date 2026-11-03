package com.stepup.android.core

/**
 * 빌드 판 값(안드로이드 BuildConfig 의 화면 쪽 자리, 2026-11-03 iOS C3). 공용 화면은 BuildConfig 대신 이것을 본다.
 * iOS 는 앱 시작 때 IosApp 이 채운다. 기본값은 스토어 판과 같다 — 블록체인 기능 없음(WEB3 = false), 시험 도구 없음.
 * iOS 의 지갑 주소 연결(WALLET_LINK)은 사용자 결정 전이라 꺼 둔다(CLAUDE.md "사용자 결정 전").
 */
object AppBuild {
    var WEB3: Boolean = false
    var WALLET_LINK: Boolean = false
    var WALLET_LINK_URL: String = ""
    var WALLET_URL: String = ""
    var TEST_TOOLS: Boolean = false
    var VERSION_NAME: String = ""
    val MAPTILER_KEY: String get() = com.stepup.android.ui.components.MapTiles.mapTilerKey
}
