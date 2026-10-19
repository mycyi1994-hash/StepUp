package com.stepup.android.domain

/**
 * 러닝 하나의 거리 — 달리는 중 · 결과 · 기록 · 1km 랩 · 목표가 모두 이 규칙으로 센다(2026-10-17 메카닉 점검). 예전에는 기록 ·
 * 랩 · 코칭이 걸음 거리(걸음 × 0.762m)를, 결과 화면과 서버는 GPS 거리를 써서 같은 러닝이 화면마다 달랐다
 * (결과 5.00km · 기록 3.77km). 챌린지 · 코스 화면은 GPS 가 조금이라도 잡히면 걸음 거리에서 GPS 거리로 바뀌어 거리가 거꾸로 갔다.
 *
 * 서버(record_session)가 쳐 주는 거리와 같은 규칙이다: 경로(GPS)로 잰 거리가 [GPS_MIN_M] 이상이면 경로 거리, 아니면 걸음 거리.
 */
object RunDistance {
    /** 이만큼(m) 경로로 재기 전에는 걸음 거리를 쓴다 — 서버와 같다. 실내 떨림 몇십 m 로 걸음 거리를 버리지 않게 */
    const val GPS_MIN_M = 100.0

    /**
     * 달리는 동안 · 결과 · 1km 랩의 거리(km) — 경로가 [GPS_MIN_M] 를 넘으면 GPS 거리, 그 전엔 걸음 거리. GPS 거리는 줄지 않으므로
     * 한 번 GPS 로 넘어가면 다시 걸음 거리로 돌아오지 않는다.
     */
    fun liveKm(gpsKm: Double, steps: Int): Double =
        if (gpsKm * 1000 >= GPS_MIN_M) gpsKm else RewardEconomy.distanceMeters(steps) / 1000

    /**
     * 저장 · 기록에 남기는 거리(m) — 서버가 쳐 주는 거리와 같다. [liveKm] 에 더해 경로 거리는 걸음 거리의 3배 + 100m 를 넘지 않는다
     * (걸음 없이 탈것으로 움직인 거리를 치지 않는다, record_session 의 v_distance_m).
     */
    fun creditedMeters(gpsKm: Double, steps: Int): Double {
        val stepMeters = RewardEconomy.distanceMeters(steps)
        val gpsMeters = gpsKm * 1000
        if (gpsMeters < GPS_MIN_M) return stepMeters
        return minOf(gpsMeters, stepMeters * 3 + if (steps > 0) 100.0 else 0.0)
    }
}
