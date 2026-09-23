package com.stepup.android.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * 땅따먹기 칸 — 지도를 약 200m 짜리 육각형으로 나눈다.
 *
 * 칠하는 것은 서버다(`supabase/migrations/0020_territory.sql`). 앱은 서버가 준
 * 칸 이름("q:r")을 지도 위 육각형으로 그리기만 한다. 그래서 이 식은 서버의
 * `economy.hex_cell` / `economy.hex_center` 와 **한 글자도 다르지 않아야** 한다 —
 * 어긋나면 칠한 곳과 보이는 곳이 한 칸씩 밀린다.
 *
 * 웹 메르카토르 평면 위의 뾰족한 쪽이 위인 육각형이다. 반지름 150(메르카토르
 * 미터)은 서울 위도에서 실제로 약 120m, 칸 너비는 약 200m 다.
 */
object Territory {

    /** 육각형 반지름(메르카토르 미터) — 서버의 economy.territory_hex_size() */
    const val HEX_SIZE = 150.0

    /** 칸 주인을 정하는 기간(일) — 서버의 economy.territory_window_days() */
    const val WINDOW_DAYS = 14

    private const val EARTH_R = 6378137.0
    private val SQRT3 = sqrt(3.0)

    fun mercatorX(lng: Double): Double = EARTH_R * Math.toRadians(lng)

    fun mercatorY(lat: Double): Double {
        val clamped = lat.coerceIn(-85.0, 85.0)
        return EARTH_R * ln(tan(PI / 4 + Math.toRadians(clamped) / 2))
    }

    private fun latOf(y: Double): Double = Math.toDegrees(2 * atan(exp(y / EARTH_R)) - PI / 2)

    private fun lngOf(x: Double): Double = Math.toDegrees(x / EARTH_R)

    /** 좌표가 속한 칸 이름 */
    fun cellOf(lat: Double, lng: Double): String {
        val x = mercatorX(lng)
        val y = mercatorY(lat)
        val q = (SQRT3 / 3 * x - y / 3) / HEX_SIZE
        val r = (2.0 / 3 * y) / HEX_SIZE
        // 큐브 좌표로 반올림. Postgres 의 round(double) 과 같게 짝수 쪽으로 반올림한다.
        val cx = q
        val cz = r
        val cy = -cx - cz
        var rx = Math.rint(cx)
        var ry = Math.rint(cy)
        var rz = Math.rint(cz)
        val dx = abs(rx - cx)
        val dy = abs(ry - cy)
        val dz = abs(rz - cz)
        if (dx > dy && dx > dz) {
            rx = -ry - rz
        } else if (dy > dz) {
            ry = -rx - rz
        } else {
            rz = -rx - ry
        }
        return "${rx.toLong()}:${rz.toLong()}"
    }

    private fun axial(cell: String): Pair<Double, Double>? {
        val parts = cell.split(':')
        if (parts.size != 2) return null
        val q = parts[0].toLongOrNull() ?: return null
        val r = parts[1].toLongOrNull() ?: return null
        return q.toDouble() to r.toDouble()
    }

    private fun centerXY(q: Double, r: Double): Pair<Double, Double> =
        HEX_SIZE * (SQRT3 * q + SQRT3 / 2 * r) to HEX_SIZE * (1.5 * r)

    /** 칸 가운데 좌표. 이름이 깨졌으면 null */
    fun center(cell: String): GeoPoint? {
        val (q, r) = axial(cell) ?: return null
        val (x, y) = centerXY(q, r)
        return GeoPoint(latOf(y), lngOf(x))
    }

    /** 육각형 꼭짓점 여섯 개 — 지도에 칸을 그릴 때 */
    fun corners(cell: String): List<GeoPoint> {
        val (q, r) = axial(cell) ?: return emptyList()
        val (cx, cy) = centerXY(q, r)
        return (0 until 6).map { i ->
            val angle = Math.toRadians(60.0 * i - 30.0)
            GeoPoint(latOf(cy + HEX_SIZE * sin(angle)), lngOf(cx + HEX_SIZE * cos(angle)))
        }
    }

    /**
     * 크루 색 — 크루 번호에서 늘 같은 색이 나온다. 서버에 색을 따로 두지 않아도
     * 같은 크루는 누구의 폰에서나 같은 색으로 보인다.
     */
    fun crewHue(crewId: String): Float {
        var h = 0
        for (c in crewId) h = h * 31 + c.code
        return ((h % 360) + 360) % 360f
    }
}
