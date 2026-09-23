package com.stepup.android.domain

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

/**
 * 첫 실행 때 심는 데모 코스.
 *
 * ── 왜 공원 안 고리인가 ──
 *
 * 코스는 지도 위에 그려진다. 선이 강이나 건물을 가로지르면 그건 코스가
 * 아니라 낙서고, 받은 사람은 그대로 뛸 수 없다. 예전 데모 코스는 400m쯤
 * 띄엄띄엄 찍은 열두 점을 직선으로 이은 것이라 한강을 가로질렀다.
 *
 * 지금은 **큰 공원 안을 도는 고리**로 만든다. 공원은 안쪽이 통째로 걸을 수
 * 있는 땅이라, 고리가 상자를 벗어나지만 않으면 물 위나 건물 위로 나가지
 * 않는다. 점 간격은 [SPACING_M] 라 선이 각지지 않고 길처럼 보인다.
 *
 * ── 여기가 한계다 ──
 *
 * 이 좌표는 공원 **안**이라는 것만 보장한다. 실제 산책로 중심선과는 수십 m
 * 어긋날 수 있다. 진짜 길에 정확히 올리려면 그 길의 좌표가 있어야 하고,
 * 그 답은 두 가지다 — 사용자가 직접 뛰어서 만든 코스(코스 만들기), 또는
 * 검증된 GPX 를 받아 그대로 심는 것. 데모는 앱이 비어 보이지 않게 하는
 * 역할까지다.
 */
object DemoCourses {

    /**
     * 데모 코스의 판 번호. 좌표를 고치면 올린다.
     *
     * 올리면 이미 설치된 기기에서도 데모 코스가 갈아 끼워진다. 안 그러면
     * 처음 켠 사람만 새 코스를 보고, 쓰던 사람은 계속 예전 선을 본다.
     *
     *  1 — 손으로 찍은 좌표 열두 점. 직선으로 이어 한강을 가로질렀다.
     *  2 — 공원 상자 안을 도는 고리.
     *  3 — 지어낸 만든 사람·하트·완주 수를 뺐다. 기본 코스는 StepUp 이 깐 것이다.
     */
    const val VERSION = 3

    /** 기본 코스의 "만든 사람" */
    const val AUTHOR = "StepUp"

    /** 이웃한 두 점 사이 거리(m). 이보다 촘촘하면 저장만 커지고 보기엔 같다. */
    const val SPACING_M = 40.0

    /**
     * 상자 대비 고리의 크기.
     *
     * 1.0 으로 두면 고리가 상자 벽에 닿고, 곡선이 조금만 부풀어도 밖으로
     * 나간다. 여유를 남겨 둔다 — 공원 경계 바로 바깥은 대개 찻길이다.
     */
    const val FIT = 0.86

    val parks: List<DemoPark> = listOf(
        DemoPark(
            name = "서울숲 순환",
            area = "성수",
            center = GeoPoint(37.5444, 127.0374),
            halfNorthM = 380.0,
            halfEastM = 430.0,
            shape = listOf(1.0, 0.93, 0.97, 0.89, 1.0, 0.92, 0.96, 0.9, 0.98, 0.91, 0.95, 0.94),
        ),
        DemoPark(
            name = "올림픽공원 순환",
            area = "송파",
            center = GeoPoint(37.5202, 127.1216),
            halfNorthM = 420.0,
            halfEastM = 580.0,
            shape = listOf(0.98, 1.0, 0.92, 0.95, 0.9, 1.0, 0.94, 0.97, 0.89, 0.96, 0.93, 0.99),
        ),
        DemoPark(
            name = "여의도공원 순환",
            area = "여의도",
            center = GeoPoint(37.5265, 126.9240),
            // 남북으로 긴 띠 모양 공원이라 동서 폭이 좁다
            halfNorthM = 480.0,
            halfEastM = 130.0,
            shape = listOf(1.0, 0.96, 0.99, 0.94, 1.0, 0.97, 1.0, 0.95, 0.98, 0.93, 0.99, 0.96),
        ),
        DemoPark(
            name = "평화의공원 순환",
            area = "상암",
            center = GeoPoint(37.5700, 126.8850),
            halfNorthM = 330.0,
            halfEastM = 430.0,
            shape = listOf(0.95, 1.0, 0.91, 0.97, 0.93, 0.99, 0.9, 0.96, 1.0, 0.92, 0.98, 0.94),
        ),
        DemoPark(
            name = "보라매공원 순환",
            area = "동작",
            center = GeoPoint(37.4928, 126.9203),
            halfNorthM = 290.0,
            halfEastM = 340.0,
            shape = listOf(1.0, 0.92, 0.98, 0.9, 0.96, 0.94, 1.0, 0.91, 0.97, 0.93, 0.99, 0.95),
        ),
    )
}

/**
 * 데모 코스 한 개 — 공원 하나와 그 안을 도는 고리.
 *
 * @param halfNorthM 공원 상자의 남북 반높이(m). 고리는 이 안에 머문다.
 * @param halfEastM 공원 상자의 동서 반너비(m)
 * @param shape 방향별 반지름 배수(0..1). 전부 1.0 이면 타원이 되고, 값을
 *   조금씩 흔들면 사람이 다닌 길처럼 굽는다.
 */
data class DemoPark(
    val name: String,
    val area: String,
    val center: GeoPoint,
    val halfNorthM: Double,
    val halfEastM: Double,
    val shape: List<Double>,
) {
    fun track(): List<GeoPoint> = parkLoop(
        center = center,
        radiusNorthM = halfNorthM * DemoCourses.FIT,
        radiusEastM = halfEastM * DemoCourses.FIT,
        shape = shape,
        spacingM = DemoCourses.SPACING_M,
    )

    /** 이 공원 상자 안에 있는 좌표인가 — 데모 코스가 물이나 찻길로 나가지 않았는지 */
    fun contains(point: GeoPoint): Boolean {
        val north = (point.lat - center.lat) * METERS_PER_DEGREE_LAT
        val east = (point.lng - center.lng) * metersPerDegreeLng(center.lat)
        return kotlin.math.abs(north) <= halfNorthM && kotlin.math.abs(east) <= halfEastM
    }
}

/** 위도 1도의 거리(m). 어디서나 거의 같다. */
internal const val METERS_PER_DEGREE_LAT = 111_320.0

/** 경도 1도의 거리(m) — 위도가 높을수록 좁아진다 */
internal fun metersPerDegreeLng(lat: Double): Double =
    METERS_PER_DEGREE_LAT * cos(Math.toRadians(lat)).coerceAtLeast(0.01)

/**
 * 한 점을 중심으로 도는 닫힌 고리를 만든다.
 *
 * [shape] 의 방향마다 기준점을 하나 찍고 그 사이를 캣멀롬 곡선으로 잇는다.
 * 기준점만 이으면 12각형이 되는데, 지도 위의 12각형은 길로 보이지 않는다.
 *
 * 마지막 점은 첫 점과 같다 — 고리는 닫혀 있어야 출발지로 돌아온다.
 */
internal fun parkLoop(
    center: GeoPoint,
    radiusNorthM: Double,
    radiusEastM: Double,
    shape: List<Double>,
    spacingM: Double = DemoCourses.SPACING_M,
): List<GeoPoint> {
    if (shape.size < 3) return emptyList()
    val perLng = metersPerDegreeLng(center.lat)

    // 방향마다 기준점 하나. 0도가 북쪽이고 시계 방향으로 돈다.
    val anchors = shape.mapIndexed { index, scale ->
        val angle = 2 * PI * index / shape.size
        GeoPoint(
            lat = center.lat + radiusNorthM * scale * cos(angle) / METERS_PER_DEGREE_LAT,
            lng = center.lng + radiusEastM * scale * sin(angle) / perLng,
        )
    }

    val out = mutableListOf<GeoPoint>()
    val n = anchors.size
    for (i in 0 until n) {
        val p0 = anchors[(i - 1 + n) % n]
        val p1 = anchors[i]
        val p2 = anchors[(i + 1) % n]
        val p3 = anchors[(i + 2) % n]
        // 이 구간을 몇 조각으로 나눌지 — 직선 거리를 간격으로 나눈다
        val steps = ceil(haversineMeters(p1, p2) / spacingM).toInt().coerceAtLeast(1)
        for (s in 0 until steps) {
            out += catmullRom(p0, p1, p2, p3, s.toDouble() / steps)
        }
    }
    out += out.first()
    return out
}

/** 네 점을 지나는 캣멀롬 곡선 위의 t(0..1) 지점. p1 → p2 구간을 그린다. */
private fun catmullRom(p0: GeoPoint, p1: GeoPoint, p2: GeoPoint, p3: GeoPoint, t: Double): GeoPoint {
    val t2 = t * t
    val t3 = t2 * t
    fun axis(a0: Double, a1: Double, a2: Double, a3: Double): Double = 0.5 * (
        2 * a1 +
            (-a0 + a2) * t +
            (2 * a0 - 5 * a1 + 4 * a2 - a3) * t2 +
            (-a0 + 3 * a1 - 3 * a2 + a3) * t3
        )
    return GeoPoint(
        lat = axis(p0.lat, p1.lat, p2.lat, p3.lat),
        lng = axis(p0.lng, p1.lng, p2.lng, p3.lng),
    )
}
