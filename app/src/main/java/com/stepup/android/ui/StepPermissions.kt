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

    fun missingActivity(context: Context): Array<String> =
        if (hasActivityRecognition(context)) emptyArray() else arrayOf(Manifest.permission.ACTIVITY_RECOGNITION)

    fun hasLocation(context: Context): Boolean =
        granted(context, Manifest.permission.ACCESS_FINE_LOCATION) || granted(context, Manifest.permission.ACCESS_COARSE_LOCATION)

    /** 걸음 센서 사용 권한(ACTIVITY_RECOGNITION)이 허용됐는지. API 29 미만은 권한 불필요. */
    fun hasActivityRecognition(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            granted(context, Manifest.permission.ACTIVITY_RECOGNITION)

    private fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
