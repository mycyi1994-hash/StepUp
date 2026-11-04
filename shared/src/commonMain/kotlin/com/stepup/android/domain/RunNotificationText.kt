package com.stepup.android.domain

import kotlin.concurrent.Volatile

/**
 * 러닝 중 알림의 글자(2026-10-18 — 잠금화면 · 알림 줄에 우리 모양으로). 화면과 같은 규칙:
 * 거리는 러닝 화면이 보이는 거리([km] — 자유 러닝은 GPS 거리, 다른 러닝은 [RunDistance.liveKm]),
 * 페이스는 달린 시간 ÷ 그 거리(50m 를 넘기 전에는 "—").
 */
object RunNotificationText {
    /**
     * 러닝 화면이 이 러닝을 자유 러닝 화면으로 그렸는지 — 화면이 [markShown] 으로 적는다. 잠금 화면 · 알림은 러닝 화면을 모르므로
     * 이것으로 같은 거리를 고른다(2026-11-03 — 예전엔 늘 GPS 거리라 챌린지 · 크루 러닝에서 화면 2.10km · 잠금 화면 0.00km 로 달랐다).
     */
    @Volatile private var shownAt: Long = 0L
    @Volatile private var shownFree: Boolean = true

    fun markShown(startedAt: Long, freeRun: Boolean) {
        shownFree = freeRun
        shownAt = startedAt
    }

    /**
     * 잠금 화면 · 알림의 거리(km) — 러닝 화면과 같은 값. 화면이 이 러닝을 아직 그리지 않았으면(되살린 러닝 등) 모임 러닝이 아니고
     * 계획이 자유 러닝일 때 자유 러닝으로 본다(러닝 화면이 고르는 순서와 같다).
     */
    fun km(startedAt: Long, gpsKm: Double, steps: Int, groupRun: Boolean): Double {
        val free = if (startedAt > 0 && shownAt == startedAt) shownFree
        else !groupRun && RunPlans.planFor(startedAt) == RunPlan.Free
        return if (free) gpsKm.coerceAtLeast(0.0) else RunDistance.liveKm(gpsKm, steps)
    }

    /** 페이스를 보이기 시작하는 거리(km) — 이보다 짧으면 페이스가 크게 튄다 */
    const val PACE_FROM_KM = 0.05

    fun distance(km: Double): String = formatUs("%.2f", km.coerceAtLeast(0.0))

    /** 1km 당 분'초" — 모르면 null */
    fun pace(elapsedSec: Long, km: Double): String? {
        if (km < PACE_FROM_KM || elapsedSec <= 0) return null
        val perKm = (elapsedSec / km).toLong()
        if (perKm >= 60 * 60) return null
        return formatUs("%d'%02d\"", perKm / 60, perKm % 60)
    }

    /** 남은 시간 "12:34" · "1:02:03" — 크루 체력 내 몫(0070) */
    fun clock(sec: Long): String {
        val s = sec.coerceAtLeast(0)
        return if (s >= 3600) formatUs("%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
        else formatUs("%d:%02d", s / 60, s % 60)
    }

    /** 알림 시간 칸(Chronometer)의 기준 — 화면이 꺼져도 시계가 스스로 흐르게 SystemClock.elapsedRealtime 기준으로 */
    fun chronometerBase(elapsedRealtimeMs: Long, elapsedSec: Long): Long = elapsedRealtimeMs - elapsedSec * 1000
}
