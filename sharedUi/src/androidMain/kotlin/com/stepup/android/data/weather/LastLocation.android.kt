package com.stepup.android.data.weather

import android.location.LocationManager
import com.stepup.android.core.AndroidHost
import com.stepup.android.domain.GeoPoint
import com.stepup.android.ui.components.lastKnown

/** 기기에 남아 있는 마지막 위치(네트워크 · 패시브 · GPS 중 가장 최근) — 새로 켜지 않는다(예전 WeatherBackground 와 같다) */
actual fun lastKnownLocation(): GeoPoint? {
    val manager = AndroidHost.app.getSystemService(LocationManager::class.java) ?: return null
    return manager.lastKnown()?.let { GeoPoint(it.latitude, it.longitude) }
}
