package com.stepup.android.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.stepup.android.core.LocationEngine
import com.stepup.android.core.LocationWarmup
import com.stepup.android.domain.GeoPoint

/**
 * 지금 내가 있는 자리. 아직 모르면 null 이다.
 *
 * 지도가 떠 있는 화면이 쓴다 — "코스를 안 골랐을 때 내가 선 자리를 지도로 보여 준다"에는 좌표 하나면 된다.
 * (러닝 탭에 있는 동안은 [RunLocationWarmup] 이 따로 위치를 잡아 두어, 여기서도 마지막 위치가 바로 맞게 나온다.)
 *
 * 그래서 두 단계로 받는다.
 *  1. 마지막으로 알려진 위치 — 즉시 나온다. 대개 몇 분 전 것이고, 지도를
 *     내 동네에 맞추는 데는 충분하다.
 *  2. 그다음 갱신 — 화면이 떠 있는 동안만 받는다. [enabled] 가 꺼지거나
 *     화면을 떠나면 바로 끊는다.
 *
 * 갱신은 구글 지도와 같은 엔진(구글 위치 서비스 — GPS · 와이파이 · 기지국을 섞는다)으로 받는다. 실내에서도 와이파이로
 * 바로 잡히고, 3-2-1 동안 켜 두면 러닝이 시작될 때 위치가 이미 잡혀 있다. 구글 Play 서비스가 없거나 받지 않으면
 * 휴대폰 위치(LocationManager)로 받는다.
 *
 * 권한이 없거나 위치가 꺼져 있으면 조용히 null 을 낸다. 여기서 권한을 다시 조르지 않는다.
 *
 * @param enabled 끄면 갱신을 받지 않고 마지막 값도 지운다. 보여 줄 다른 것(코스·
 *   실시간 경로)이 생기면 꺼서, 안 보이는 지도를 위해 GPS 를 켜 두지 않는다.
 */
@Composable
fun rememberCurrentLocation(enabled: Boolean = true): GeoPoint? {
    val context = LocalContext.current
    var here by remember { mutableStateOf<GeoPoint?>(null) }

    DisposableEffect(enabled) {
        if (!enabled || !context.canReadLocation()) {
            here = null
            return@DisposableEffect onDispose { }
        }
        val manager = ContextCompat.getSystemService(context, LocationManager::class.java)
        // 끊은 뒤에 늦게 온 위치(구글 위치 서비스의 마지막 위치 등)는 버린다
        var listening = true
        // 지금 보이는 "마지막으로 알려진 위치"의 시각 — 더 오래된 마지막 위치로 되돌리지 않는다
        var seedTime = 0L
        // 새 위치를 한 번이라도 받았다 — 늦게 온 마지막 위치로 되돌리지 않는다
        var live = false

        val seed = manager?.lastKnown()
        if (seed != null) {
            here = GeoPoint(seed.latitude, seed.longitude)
            seedTime = seed.time
        }

        fun take(location: Location) {
            if (!listening) return
            live = true
            here = GeoPoint(location.latitude, location.longitude)
        }

        // API 29 이하에서는 넷 모두 추상 메서드다 — 하나라도 빠지면 이 화면에서 위치를 켜고 끌 때 앱이 죽는다(AbstractMethodError)
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) = take(location)

            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
        }
        var managerOn = false
        fun startManager() {
            if (!listening || managerOn || manager == null) return
            val provider = manager.bestProvider() ?: return
            try {
                // 3초·5m — 지도를 내 자리에 맞추는 데 필요한 만큼이다
                manager.requestLocationUpdates(provider, 3_000L, 5f, listener, Looper.getMainLooper())
                managerOn = true
            } catch (_: SecurityException) {
                // Permission can be revoked after the initial check.
                here = null
            } catch (_: IllegalArgumentException) {
                // The provider may disappear while this screen is opening.
            }
        }

        // 1) 구글 위치 서비스 — 마지막 위치부터 바로, 그다음 3초마다(처음 위치를 정확한 것이 올 때까지 미루지 않는다)
        val fused = if (LocationEngine.fusedAvailable(context)) LocationServices.getFusedLocationProviderClient(context) else null
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let(::take)
            }
        }
        val fusedOn = fused != null && try {
            fused.lastLocation.addOnSuccessListener { last ->
                if (listening && !live && last != null && last.time >= seedTime) here = GeoPoint(last.latitude, last.longitude)
            }
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000L)
                .setMinUpdateIntervalMillis(1_000L)
                .setMinUpdateDistanceMeters(5f)
                .setWaitForAccurateLocation(false)
                .build()
            // Play 서비스가 받지 않았다 — 휴대폰 위치로 바꿔 받는다
            fused.requestLocationUpdates(request, callback, Looper.getMainLooper()).addOnFailureListener { startManager() }
            true
        } catch (_: SecurityException) {
            false
        }
        // 2) Play 서비스가 없으면 휴대폰 위치
        if (!fusedOn) startManager()

        onDispose {
            listening = false
            if (fusedOn) runCatching { fused?.removeLocationUpdates(callback) }
            if (managerOn) runCatching { manager?.removeUpdates(listener) }
        }
    }

    return here
}

/**
 * [enabled] 인 동안, 앱이 화면에 있을 때만 위치를 미리 잡아 둔다([LocationWarmup] — 러닝 탭 · 시작 흐름 · 크루 대기실).
 * 앱이 뒤로 가면 끄고 돌아오면 다시 켠다. 위치 권한 · 위치 기능을 허용하고 돌아온 때(ON_RESUME)에도 다시 켜 본다.
 */
@Composable
fun RunLocationWarmup(enabled: Boolean) {
    val context = LocalContext.current
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(enabled, lifecycle) {
        if (!enabled) {
            LocationWarmup.stop()
            return@DisposableEffect onDispose { }
        }
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_START, androidx.lifecycle.Lifecycle.Event.ON_RESUME -> LocationWarmup.start(context)
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> LocationWarmup.stop()
                else -> Unit
            }
        }
        // 붙이는 순간 지금 상태까지의 일(ON_START · ON_RESUME)을 다시 받는다 — 화면에 있으면 바로 켜진다
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            LocationWarmup.stop()
        }
    }
}

private fun Context.canReadLocation(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/**
 * 마지막으로 알려진 위치 중 가장 최근 것.
 *
 * 공급자마다 따로 들고 있어서, GPS 만 물으면 실내에서 몇 시간 전 것이 나오고
 * 네트워크 쪽에 있는 1분 전 위치를 놓친다. 셋 다 물어 보고 시각으로 고른다.
 */
private fun LocationManager.lastKnown(): Location? = runCatching {
    listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER,
    )
        .mapNotNull { provider ->
            try {
                getLastKnownLocation(provider)
            } catch (_: SecurityException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }
        }
        .maxByOrNull { it.time }
}.getOrNull()

/** 지금 쓸 수 있는 공급자. GPS 가 꺼져 있으면 네트워크로 내려간다. */
private fun LocationManager.bestProvider(): String? = runCatching {
    when {
        isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> null
    }
}.getOrNull()
