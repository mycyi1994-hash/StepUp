package com.stepup.android.core

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.stepup.android.service.isMockLocation

/**
 * 러닝 탭에 있는 동안 위치를 미리 잡아 둔다(2026-10-17 사용자 결정 "키자마자 잡아두면 안되냐 · 시작하고도 10초를 찾는다").
 * 러닝을 시작하는 순간 GPS 를 처음 켜면 위성을 찾는 데 10초쯤 걸린다 — 앱은 러닝 탭으로 열리므로, 켜자마자 찾기 시작해
 * 러닝을 누를 때는 이미 잡혀 있게 한다.
 *
 * 러닝 탭 · 시작 흐름 · 크루 대기실이 화면에 있는 동안만 켠다(`RunLocationWarmup`). 다른 탭으로 가거나 앱이 뒤로 가면 끈다 —
 * 안 보이는 러닝을 위해 GPS 를 켜 두지 않는다. 한 번 잡힌 GPS 는 잠깐 꺼도 다시 켜면 금방 잡힌다.
 *
 * 러닝 서비스는 시작할 때 여기서 잡은 위치로 지도의 내 점 · GPS 표시를 바로 채운다([fresh]). 경로 · 거리에는 넣지 않는다
 * (러닝 전 위치다). 엔진은 러닝과 같다 — 구글 위치 서비스, 없으면 휴대폰 위치. 메인 스레드에서만 부른다.
 */
object LocationWarmup {
    /** 마지막으로 받은 위치(가짜 GPS 앱의 위치는 담지 않는다) */
    @Volatile private var last: Location? = null

    private var fused: FusedLocationProviderClient? = null
    private var manager: LocationManager? = null
    private var managerOn = false

    private fun take(location: Location) {
        if (isMockLocation(location)) return
        val previous = last
        // 늦게 온 옛 위치로 되돌리지 않는다
        if (previous == null || location.elapsedRealtimeNanos >= previous.elapsedRealtimeNanos) last = location
    }

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach(::take)
        }
    }

    // API 29 이하에서는 넷 모두 추상 메서드다 — 하나라도 빠지면 위치를 켜고 끌 때 앱이 죽는다(AbstractMethodError)
    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) = take(location)
        override fun onProviderEnabled(provider: String) = Unit
        override fun onProviderDisabled(provider: String) = Unit

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
    }

    /** 켠다 — 이미 켜져 있거나 위치 권한이 없으면 아무것도 하지 않는다(권한을 조르지 않는다) */
    fun start(context: Context) {
        if (fused != null || managerOn) return
        val app = context.applicationContext
        val fine = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return
        manager = ContextCompat.getSystemService(app, LocationManager::class.java)
        if (!startFused(app)) startManager(fine)
    }

    /** 끈다. 받아 둔 위치는 남긴다 — [fresh] 가 시간으로 거른다 */
    fun stop() {
        fused?.let { client -> runCatching { client.removeLocationUpdates(callback) } }
        fused = null
        if (managerOn) runCatching { manager?.removeUpdates(listener) }
        managerOn = false
        manager = null
    }

    /** [maxAgeMs] 안에 받은 위치 — 없으면 null */
    fun fresh(maxAgeMs: Long): Location? = last?.takeIf { ageMs(it) in 0..maxAgeMs }

    /** 이 위치를 받은 지(ms) — 폰 시계를 바꿔도 틀리지 않게 부팅 기준 시각으로 잰다 */
    fun ageMs(location: Location): Long = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000

    @SuppressLint("MissingPermission")
    private fun startFused(context: Context): Boolean {
        if (!LocationEngine.fusedAvailable(context)) return false
        val client = LocationServices.getFusedLocationProviderClient(context)
        // 러닝과 같은 높은 정확도 — 위성까지 켜 둬야 시작할 때 GPS 가 이미 잡혀 있다. 첫 위치는 정확한 것을 기다리지 않는다
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
            .setMinUpdateIntervalMillis(1_000L)
            .setWaitForAccurateLocation(false)
            .build()
        return try {
            client.lastLocation.addOnSuccessListener { location -> if (location != null) take(location) }
            client.requestLocationUpdates(request, callback, Looper.getMainLooper()).addOnFailureListener {
                // Play 서비스가 받지 않았다 — 그사이 끄지 않았으면 휴대폰 위치로 받는다
                if (fused === client) {
                    fused = null
                    startManager(
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                            PackageManager.PERMISSION_GRANTED,
                    )
                }
            }
            fused = client
            true
        } catch (_: SecurityException) {
            false
        }
    }

    /** 휴대폰 위치 — 위성 GPS(정확한 위치를 허용했을 때)와 와이파이 · 기지국 위치를 함께 받는다 */
    @SuppressLint("MissingPermission")
    private fun startManager(fine: Boolean) {
        val lm = manager ?: return
        if (managerOn) return
        if (fine && LocationManager.GPS_PROVIDER in lm.allProviders) {
            managerOn = runCatching { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2_000L, 0f, listener, Looper.getMainLooper()) }
                .isSuccess
        }
        if (LocationManager.NETWORK_PROVIDER in lm.allProviders) {
            val network = runCatching {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3_000L, 0f, listener, Looper.getMainLooper())
            }.isSuccess
            managerOn = managerOn || network
        }
    }
}
