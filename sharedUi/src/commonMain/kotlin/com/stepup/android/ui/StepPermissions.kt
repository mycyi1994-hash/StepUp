package com.stepup.android.ui

import com.stepup.android.core.readPermissionSnapshot

/** 러닝 권한(안드로이드 ui/StepPermissions.kt 와 같은 이름) — 지금 받은 것만 본다. 묻기는 rememberLocationPermissionRequest */
object StepPermissions {
    fun hasLocation(): Boolean = readPermissionSnapshot().approximateLocation
    fun hasPreciseLocation(): Boolean = readPermissionSnapshot().preciseLocation
    fun hasActivityRecognition(): Boolean = readPermissionSnapshot().activity
    fun hasNotificationPermission(): Boolean = readPermissionSnapshot().notifications
}
