package com.stepup.android.core

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

/**
 * 러닝 위치를 받는 엔진(2026-10-16 사용자 결정 "그래 다 되도록 만들어") — 구글 지도와 같은 구글 위치 서비스(fused)를 먼저 쓴다.
 * GPS · 와이파이 · 기지국을 섞어 실내에서도 위치가 잡히고, 밖에서는 GPS 정확도로 받는다. 구글 Play 서비스가 없는 휴대폰은
 * 휴대폰 위치(LocationManager — GPS · 와이파이 · 기지국)로 받는다.
 */
object LocationEngine {
    /** 구글 위치 서비스를 쓸 수 있는가 — 구글 Play 서비스가 깔려 있고 쓸 수 있는 상태 */
    fun fusedAvailable(context: Context): Boolean = runCatching {
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    }.getOrDefault(false)
}
