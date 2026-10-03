package com.stepup.android.domain

import kotlin.math.roundToInt

/**
 * 내 위치에서 출발할 수 있는 코스(시안 U04 · K02) — 저장한 코스 · 게시판 코스 중 지금 자리에서 가까운 것부터.
 *
 * 추천 서버가 없어 거리만 본다(남은 연동: 추천 API). 앱이 심어 둔 체험 코스는 넣지 않는다 — 진짜 추천처럼 보이게
 * 꾸미지 않는다. 같은 길이 내 코스와 게시판에 둘 다 있으면 한 번만 넣는다.
 */
object CourseRecommendations {
    /** 지금 자리에서 코스 길까지 이 거리 안이면 "내 위치에서 출발" */
    const val NEAR_METERS = 1_500.0

    /** 예상 소요 시간에 쓰는 기준 페이스(초/km) — 걷기가 섞인 가벼운 러닝, 화면에 기준을 함께 적는다 */
    const val ETA_PACE_SEC_PER_KM = 480

    data class Pick(val course: RunCourse, val toCourseMeters: Double)

    fun near(
        here: GeoPoint,
        courses: List<RunCourse>,
        exclude: (RunCourse) -> Boolean = { false },
        radius: Double = NEAR_METERS,
    ): List<Pick> = courses.asSequence()
        .filter { it.hasTrack && !exclude(it) }
        .distinctBy { it.encode() }
        .map { Pick(it, distanceToPathMeters(here, it.points)) }
        .filter { it.toCourseMeters <= radius }
        .sortedWith(compareBy<Pick> { it.toCourseMeters }.thenBy { it.course.distanceKm })
        .toList()

    /** 예상 소요 시간(분) — [ETA_PACE_SEC_PER_KM] 기준, 1분보다 짧게 적지 않는다 */
    fun minutes(km: Double, paceSecPerKm: Int = ETA_PACE_SEC_PER_KM): Int =
        (km * paceSecPerKm / 60.0).roundToInt().coerceAtLeast(1)

    /** "350m" · "1.2km" — 코스까지의 거리 */
    fun distanceLabel(meters: Double): String =
        if (meters < 950) "${(meters / 50).roundToInt() * 50}m" else "%.1fkm".format(java.util.Locale.ROOT, meters / 1000)
}
