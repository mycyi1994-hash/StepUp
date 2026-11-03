package com.stepup.android.data.weather

import com.stepup.android.data.remote.defaultHttpPoster
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.WeatherScene
import com.stepup.android.domain.epochMillis
import com.stepup.android.ui.StepPermissions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * "날씨에 맞춰 홈 풍경 바꾸기"(설정 › 경험)가 켜져 있을 때만 쓰는 지금 날씨.
 *
 * 위치 권한이 있을 때 기기에 남아 있는 마지막 위치만 읽고(새로 GPS 를 켜지 않는다 — [lastKnownLocation]),
 * 30분에 한 번까지만 묻는다. 실패하면 이전 값을 둔다.
 */
object WeatherBackground {
    private const val REFRESH_MS = 30 * 60 * 1000L

    private val client by lazy { WeatherClient(defaultHttpPoster(8_000)) }
    private val _scene = MutableStateFlow<WeatherScene?>(null)
    val scene: StateFlow<WeatherScene?> = _scene.asStateFlow()
    private val _code = MutableStateFlow<Int?>(null)

    /** 같은 응답의 WMO 날씨 코드 — 사진 위 날씨 효과(비 · 눈, ui/background/WeatherEffect.kt)를 고른다. 모르면 null(효과 없음) */
    val weatherCode: StateFlow<Int?> = _code.asStateFlow()
    private var fetchedAt = 0L

    suspend fun refresh(now: Long = epochMillis()) {
        if (_scene.value != null && now - fetchedAt < REFRESH_MS) return
        if (!StepPermissions.hasLocation()) return
        val location = lastKnownLocation() ?: return
        val report = client.report(location.lat, location.lng) ?: return
        fetchedAt = now
        _scene.value = report.scene
        _code.value = report.weatherCode
    }

    /** 설정을 끄면 받아 둔 날씨를 버린다 */
    fun clear() {
        _scene.value = null
        _code.value = null
        fetchedAt = 0L
    }
}

/** 기기에 남아 있는 마지막 위치(새로 켜지 않는다) — 안드로이드 getLastKnownLocation · iOS CLLocationManager.location */
expect fun lastKnownLocation(): GeoPoint?
