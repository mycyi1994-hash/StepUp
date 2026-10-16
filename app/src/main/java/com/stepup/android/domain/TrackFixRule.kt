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

    /** 기준점보다 이만큼(배) 이상 정확해야 [Fix.REFINE] — 와이파이(수십 m) → GPS(몇 m)처럼 확실히 나아졌을 때만 */
    const val REFINE_RATIO = 0.5f

    /** 경로가 생긴 뒤 흐린 위치([Fix.MAP_ONLY])가 와도 지금 자리를 이만큼(ms) 그대로 둔다 — 그보다 오래 좋은 위치가 없으면 흐린 위치라도 쓴다 */
    const val KEEP_HERE_MS = 30_000L

    enum class Fix {
        /** 오차가 너무 크다 — 지도의 지금 자리만 */
        MAP_ONLY,

        /** 기준점에서 오차만큼도 안 움직였다 — 지도의 지금 자리만(기준점은 그대로) */
        HOLD,

        /**
         * 기준점보다 훨씬 정확한 위치가 기준점의 오차 안에 왔다 — 같은 자리를 더 정확히 알게 된 것이다. 기준점만 이 위치로 옮기고
         * 거리 · 경로 점은 더하지 않는다. 시작할 때 와이파이 위치(오차 수십 m)로 잡힌 기준점에 묶여, GPS 가 잡힌 뒤에도 그 오차의
         * 두 배를 움직일 때까지 거리가 0 에 멈춰 있던 것(걸으면 40~60초)을 막는다.
         */
        REFINE,

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
        if (anchorDistanceM >= threshold) return Fix.COMMIT
        // 오차를 알려 준 위치만 기준점을 다시 잡는다(모르는 위치는 30m 로 볼 뿐 더 정확하다고 할 수 없다)
        val refines = accuracyM != null && anchorAccuracyM != null &&
            accuracyM <= anchorAccuracyM * REFINE_RATIO && anchorDistanceM <= anchorAccuracyM + accuracyM
        return if (refines) Fix.REFINE else Fix.HOLD
    }

    /**
     * 흐린 위치([Fix.MAP_ONLY])가 왔을 때 지금 자리를 그대로 둘까 — 경로가 생긴 뒤, 좋은 위치를 받은 지 [KEEP_HERE_MS] 안이면.
     * 지도의 내 점 · 크루에게 보내는 위치가 기지국 위치로 수백 m 튀지 않게.
     *
     * @param hereAgeMs 지금 자리를 받은 지(ms). 모르면(받은 적 없음) 아주 큰 값
     */
    fun keepHere(hasTrack: Boolean, hereAgeMs: Long): Boolean = hasTrack && hereAgeMs in 0 until KEEP_HERE_MS
}
