package com.stepup.android.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** 걸음 추적에 필요한 런타임 권한 도우미 */
object StepPermissions {

    /** 아직 허용되지 않아 요청해야 하는 권한 목록 */
    fun missing(context: Context): Array<String> = requiredPermissions(Build.VERSION.SDK_INT) { granted(context, it) }

    internal fun requiredPermissions(api: Int, granted: (String) -> Boolean): Array<String> {
        val needed = mutableListOf<String>()
        if (api >= Build.VERSION_CODES.Q &&
            !granted(Manifest.permission.ACTIVITY_RECOGNITION)
        ) {
            needed += Manifest.permission.ACTIVITY_RECOGNITION
        }
        if (api >= Build.VERSION_CODES.TIRAMISU &&
            !granted(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            needed += Manifest.permission.POST_NOTIFICATIONS
        }
        // GPS 코스 기록 — 거부해도 러닝 자체는 걸음 센서로 계속 된다
        if (!granted(Manifest.permission.ACCESS_FINE_LOCATION) && !granted(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            // Android 12+ may ignore FINE by itself. Respect an existing approximate grant.
            needed += Manifest.permission.ACCESS_FINE_LOCATION
            needed += Manifest.permission.ACCESS_COARSE_LOCATION
        }
        return needed.toTypedArray()
    }

    /**
     * 함께 뛰기 출발 순간에 꼭 있어야 하는 권한 — 걸음 · 위치. 알림 권한은 뺀다: 출발 순간에 권한 창이 뜨면 답할 때까지 러닝이
     * 늦게 켜진다(2026-11-18 점검 — 알림은 대기실에서 준비할 때 묻는다).
     */
    fun missingForStart(context: Context): Array<String> = startPermissions(Build.VERSION.SDK_INT) { granted(context, it) }

    internal fun startPermissions(api: Int, granted: (String) -> Boolean): Array<String> =
        requiredPermissions(api, granted).filter { it != Manifest.permission.POST_NOTIFICATIONS }.toTypedArray()

    fun missingActivity(context: Context): Array<String> =
        if (hasActivityRecognition(context)) emptyArray() else arrayOf(Manifest.permission.ACTIVITY_RECOGNITION)

    fun hasLocation(context: Context): Boolean =
        granted(context, Manifest.permission.ACCESS_FINE_LOCATION) || granted(context, Manifest.permission.ACCESS_COARSE_LOCATION)

    /** 정확한 위치(FINE)까지 허용했는지 — 대략적인 위치만이면 false */
    fun hasPreciseLocation(context: Context): Boolean = granted(context, Manifest.permission.ACCESS_FINE_LOCATION)

    /** 알림 권한 — Android 13 미만은 실행 중에 묻지 않으므로 늘 true */
    fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || granted(context, Manifest.permission.POST_NOTIFICATIONS)

    /** 걸음 센서 사용 권한(ACTIVITY_RECOGNITION)이 허용됐는지. API 29 미만은 권한 불필요. */
    fun hasActivityRecognition(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            granted(context, Manifest.permission.ACTIVITY_RECOGNITION)

    private fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
