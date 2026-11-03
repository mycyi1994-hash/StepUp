package com.stepup.android.core

/** 이 앱이 받은 권한(설정 › 개인정보가 보인다) — 안드로이드 PrivacyScreen.readPermissions 와 같은 넷 */
data class PermissionState(
    val activity: Boolean,
    val preciseLocation: Boolean,
    val approximateLocation: Boolean,
    val notifications: Boolean,
)

/** 지금 권한 — iOS: 걸음(Core Motion) · 위치(정확 · 대략) · 알림(마지막으로 읽은 값) */
expect fun readPermissionSnapshot(): PermissionState

/** 인터넷이 있는가 — 모르면 true(보내 보고 답으로 안다) */
expect fun hasNetwork(): Boolean

/** 기기 위치 기능이 켜져 있는가(안드로이드 LocationManagerCompat.isLocationEnabled) */
expect fun locationServicesOn(): Boolean
