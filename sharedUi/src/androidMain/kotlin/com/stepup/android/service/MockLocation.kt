package com.stepup.android.service

import android.location.Location
import android.os.Build

/**
 * 이 좌표가 모의 위치(개발자 옵션의 "모의 위치 앱", 가짜 GPS 앱)에서 왔는가.
 * API 31 부터는 `isMock`, 그 전에는 `isFromMockProvider` 가 같은 표시다.
 */
fun isMockLocation(location: Location): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        location.isMock
    } else {
        @Suppress("DEPRECATION")
        location.isFromMockProvider
    }
