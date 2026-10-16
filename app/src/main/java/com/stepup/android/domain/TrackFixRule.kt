package com.stepup.android.domain

/*
 * 러닝 위치는 구글 지도와 같은 엔진(구글 위치 서비스 — GPS · 와이파이 · 기지국을 섞는다)으로 받는다(2026-10-16 사용자 결정
 * "그래 다 되도록 만들어"). 시작 전에 위치를 기다리지 않는다 — 러닝은 바로 시작하고, 위치는 3-2-1 · 러닝 동안 찾는다
 * (2026-10-16 "위치를 찾고 있어요 이거 안 뜨게"). 여기서는 잡힌 위치를 경로 · 거리에 넣을지만 정한다.
 */

/**
 * 달리는 중 위성 GPS 가 아닌 위치(구글 위치 서비스 · 와이파이 · 기지국)를 경로 · 거리에 넣을지 — 실내에서 제자리에 있어도
 * 와이파이 위치는 수십 m 씩 흔들린다. 그 떨림이 거리로 쌓이지 않게, 오차보다 확실히 크게 움직였을 때만 넣는다.
 * 위성 GPS(LocationManager 의 GPS)는 예전 규칙 그대로다(이 규칙을 거치지 않는다).
 */
object TrackFixRule {
    /** 오차가 이보다 크면(m) 경로 · 거리에 쓰지 않고 지도의 지금 자리만 옮긴다 — 기지국 위치 · 아주 흐린 와이파이 위치 */
    const val MAX_ACCURACY_M = 50f

    /** 오차를 알려 주지 않는 위치는 이만큼으로 본다(m) */
    const val UNKNOWN_ACCURACY_M = 30f

    /** 경로 점 사이 가장 짧은 거리(m) — 서비스가 경로에 점을 더하는 기준과 같다 */
    const val MIN_STEP_M = 8.0

    enum class Fix {
        /** 오차가 너무 크다 — 지도의 지금 자리만 */
        MAP_ONLY,

        /** 기준점에서 오차만큼도 안 움직였다 — 지도의 지금 자리만(기준점은 그대로) */
        HOLD,

        /** 경로 · 거리에 넣는다(기준점을 옮긴다) */
        COMMIT,
    }

    /**
     * @param anchorDistanceM 지금 기준점에서 이 위치까지(m). 기준점이 없으면(첫 점 · 끊긴 뒤 새 구간) null
     * @param anchorAccuracyM 기준점 위치의 오차(m)
     */
    fun decide(accuracyM: Float?, anchorDistanceM: Double?, anchorAccuracyM: Float?): Fix {
        val accuracy = accuracyM ?: UNKNOWN_ACCURACY_M
        if (accuracy > MAX_ACCURACY_M) return Fix.MAP_ONLY
        if (anchorDistanceM == null) return Fix.COMMIT
        // 두 점의 오차 중 큰 쪽의 두 배 — 흔들림만으로는 넘기 어렵고, 실제로 달리면 몇 초 안에 넘는다
        val threshold = maxOf(MIN_STEP_M, 2.0 * maxOf(accuracy, anchorAccuracyM ?: accuracy))
        return if (anchorDistanceM >= threshold) Fix.COMMIT else Fix.HOLD
    }
}
