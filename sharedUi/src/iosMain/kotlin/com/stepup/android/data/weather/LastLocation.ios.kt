@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.stepup.android.data.weather

import com.stepup.android.domain.GeoPoint
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocationManager

/** iOS 가 마지막으로 알던 위치(새로 GPS 를 켜지 않는다) */
actual fun lastKnownLocation(): GeoPoint? =
    CLLocationManager().location?.coordinate?.useContents { GeoPoint(latitude, longitude) }
