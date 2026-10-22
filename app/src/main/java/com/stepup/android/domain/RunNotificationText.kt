package com.stepup.android.domain

import java.util.Locale

/**
 * 러닝 중 알림의 글자(2026-10-18 — 잠금화면 · 알림 줄에 우리 모양으로). 화면과 같은 규칙:
 * 거리는 GPS 로 잰 거리(자유 러닝과 같다), 페이스는 달린 시간 ÷ 거리(50m 를 넘기 전에는 "—").
 */
object RunNotificationText {
    /** 페이스를 보이기 시작하는 거리(km) — 이보다 짧으면 페이스가 크게 튄다 */
    const val PACE_FROM_KM = 0.05

    fun distance(km: Double): String = String.format(Locale.US, "%.2f", km.coerceAtLeast(0.0))

    /** 1km 당 분'초" — 모르면 null */
    fun pace(elapsedSec: Long, km: Double): String? {
        if (km < PACE_FROM_KM || elapsedSec <= 0) return null
        val perKm = (elapsedSec / km).toLong()
        if (perKm >= 60 * 60) return null
        return String.format(Locale.US, "%d'%02d\"", perKm / 60, perKm % 60)
    }

    /** 남은 시간 "12:34" · "1:02:03" — 크루 체력 내 몫(0070) */
    fun clock(sec: Long): String {
        val s = sec.coerceAtLeast(0)
        return if (s >= 3600) String.format(Locale.US, "%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
        else String.format(Locale.US, "%d:%02d", s / 60, s % 60)
    }

    /** 알림 시간 칸(Chronometer)의 기준 — 화면이 꺼져도 시계가 스스로 흐르게 SystemClock.elapsedRealtime 기준으로 */
    fun chronometerBase(elapsedRealtimeMs: Long, elapsedSec: Long): Long = elapsedRealtimeMs - elapsedSec * 1000
}
