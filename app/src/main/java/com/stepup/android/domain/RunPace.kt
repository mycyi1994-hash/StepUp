package com.stepup.android.domain

/**
 * 기록의 페이스 · 평균 속도(2026-10-17 점검).
 *
 * 러닝 중 · 결과 화면은 50m 를 넘어야 페이스를 보인다. 기록 상세 · 지난 도전은 거리만 있으면 나눠, 1분짜리 기록이
 * 65'37"/km 처럼 보였다. 이보다 짧거나 시간이 없으면 null 이고 화면은 "—" 를 적는다.
 */
object RunPace {
    /** 페이스 · 속도를 보이는 최소 거리(km) — 러닝 중(livePace) · 결과(finishPace)와 같다 */
    const val MIN_KM = 0.05
    const val MIN_METERS = MIN_KM * 1000

    /** 초/km — 시간이 있고 거리가 [MIN_METERS] 이상일 때만 */
    fun secPerKm(seconds: Long, meters: Double): Double? =
        if (seconds > 0 && meters.isFinite() && meters >= MIN_METERS) seconds / (meters / 1000.0) else null

    /** km/h — [secPerKm] 과 같은 조건 */
    fun kmh(seconds: Long, meters: Double): Double? =
        if (seconds > 0 && meters.isFinite() && meters >= MIN_METERS) (meters / 1000.0) / (seconds / 3600.0) else null

    /**
     * 여러 러닝의 평균 바탕 — 거리와 시간이 모두 있는 러닝만 더한 (시간 초, 거리 m). 시간만 잰 러닝(거리 0)의 시간이 섞이면
     * 평균 페이스가 느리게 나온다. 기록 통계(observeRecordTotals 의 pacedSeconds · pacedMeters)와 같은 바탕이다.
     */
    fun pacedTotals(runs: List<Pair<Long, Double>>): Pair<Long, Double> {
        val paced = runs.filter { (seconds, meters) -> seconds > 0 && meters > 0.0 }
        return paced.sumOf { it.first } to paced.sumOf { it.second }
    }
}
