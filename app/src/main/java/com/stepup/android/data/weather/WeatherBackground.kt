package com.stepup.android.data.weather

import android.content.Context
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.stepup.android.data.remote.UrlConnectionPoster
import com.stepup.android.domain.WeatherScene
import com.stepup.android.ui.StepPermissions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * "날씨에 맞춰 홈 풍경 바꾸기"(설정 › 경험)가 켜져 있을 때만 쓰는 지금 날씨.
 *
 * 위치 권한이 있을 때 기기에 남아 있는 마지막 위치만 읽고(새로 GPS 를 켜지 않는다),
 * 30분에 한 번까지만 묻는다. 실패하면 이전 값을 둔다.
 */
object WeatherBackground {
    private const val REFRESH_MS = 30 * 60 * 1000L

    private val client = WeatherClient(UrlConnectionPoster(timeoutMillis = 8_000))
    private val _scene = MutableStateFlow<WeatherScene?>(null)
    val scene: StateFlow<WeatherScene?> = _scene.asStateFlow()
    private var fetchedAt = 0L

    suspend fun refresh(context: Context, now: Long = System.currentTimeMillis()) {
        if (_scene.value != null && now - fetchedAt < REFRESH_MS) return
        if (!StepPermissions.hasLocation(context)) return
        val manager = ContextCompat.getSystemService(context, LocationManager::class.java) ?: return
        val location = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)
            .mapNotNull { provider ->
                try { manager.getLastKnownLocation(provider) } catch (_: SecurityException) { null } catch (_: IllegalArgumentException) { null }
            }
            .maxByOrNull { it.time } ?: return
        val scene = client.current(location.latitude, location.longitude) ?: return
        fetchedAt = now
        _scene.value = scene
    }

    /** 설정을 끄면 받아 둔 날씨를 버린다 */
    fun clear() {
        _scene.value = null
        fetchedAt = 0L
    }
}
