package com.stepup.android.core

import com.stepup.android.domain.GeoPoint

/**
 * 앱 밖으로 여는 것(안드로이드 core/ExternalIntents.kt 와 같은 이름, 2026-11-03 iOS C4) — 설정 · 지도 · 웹 주소.
 * 여는 데 실패하면 false(부르는 쪽이 안내한다).
 */
expect object ExternalIntents {
    fun openAppSettings(): Boolean
    fun openLocationSettings(): Boolean
    fun openPlaceInMaps(place: String)
    fun openRouteInMaps(points: List<GeoPoint>)
    fun openCoordinateInMaps(point: GeoPoint)
    fun openUrl(url: String)
    fun tryOpenUrl(url: String): Boolean
    /** 메일 앱의 작성 창(받는 사람 · 제목). 열 곳이 없으면 false */
    fun composeEmail(address: String, subject: String): Boolean
    /** 글 보내기(앱 밖 공유 시트) — [title] 은 안드로이드 고르기 창 제목 */
    /** 글을 클립보드에 넣는다(안드로이드 ClipboardManager · iOS UIPasteboard) */
    fun copyText(text: String)

    fun shareText(text: String, title: String? = null): Boolean
    /** 기기 캘린더의 새 일정 화면(저장은 사용자가 그 화면에서) */
    fun addToCalendar(title: String, place: String, beginMs: Long, endMs: Long): Boolean
}
