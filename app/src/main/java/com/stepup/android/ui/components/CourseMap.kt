package com.stepup.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt

/**
 * Recorded route geometry on a schematic surface. It never invents local streets.
 * The full map screen uses real tiles when available.
 */
@Composable
fun CourseTrackMap(
    points: List<Pair<Float, Float>>,
    modifier: Modifier = Modifier,
    seed: Int = 0,
    /** 0..1 — 경로 위 진행 지점에 러너 점을 찍는다. null이면 표시하지 않음 */
    progress: Float? = null,
) {
    Canvas(modifier) {
        drawStreets(seed)
        if (points.size < 2) return@Canvas

        // 경로를 8% 안쪽 여백에 펼친다
        val inset = 0.08f
        fun px(p: Pair<Float, Float>): Offset = Offset(
            (inset + p.first * (1f - inset * 2)) * size.width,
            (inset + p.second * (1f - inset * 2)) * size.height,
        )

        val path = Path()
        val first = px(points.first())
        path.moveTo(first.x, first.y)
        for (i in 1 until points.size) {
            val p = px(points[i])
            path.lineTo(p.x, p.y)
        }

        // 글로우(넓고 옅게) → 본선(가늘고 진하게)
        drawPath(
            path,
            color = Volt.copy(alpha = 0.18f),
            style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawPath(
            path,
            brush = Brush.linearGradient(listOf(Volt.copy(alpha = 0.85f), Volt)),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        // 시작점 — 흰 심의 파란 링
        drawCircle(Volt.copy(alpha = 0.28f), radius = 8.dp.toPx(), center = first)
        drawCircle(Volt, radius = 4.5f.dp.toPx(), center = first)
        drawCircle(Color.White, radius = 2.dp.toPx(), center = first)

        // 도착 깃발
        val last = px(points.last())
        drawFlag(last)

        // 진행 지점 러너 점
        progress?.let { f ->
            val at = pointAlong(points.map(::px), f.coerceIn(0f, 1f))
            drawCircle(Volt.copy(alpha = 0.30f), radius = 9.dp.toPx(), center = at)
            drawCircle(Volt, radius = 4.5f.dp.toPx(), center = at)
            drawCircle(Color.White, radius = 2.dp.toPx(), center = at)
        }
    }
}

/**
 * Abstract coordinate grid behind a route preview or unloaded tiles. This cannot
 * be mistaken for street data from the real map provider.
 */
@Suppress("UNUSED_PARAMETER")
internal fun DrawScope.drawStreets(seed: Int) {
    drawRect(Color(0xFF0C1930))
    val line = Snow.copy(alpha = 0.055f)
    for (step in 1..5) {
        val x = size.width * step / 6f
        val y = size.height * step / 6f
        drawLine(line, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
        drawLine(line, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
    }
}

/** 폴리라인 전체 길이 기준 f(0..1) 지점의 좌표 */
private fun pointAlong(pts: List<Offset>, f: Float): Offset {
    if (pts.size < 2) return pts.firstOrNull() ?: Offset.Zero
    val segs = FloatArray(pts.size - 1)
    var total = 0f
    for (i in 0 until pts.size - 1) {
        val d = (pts[i + 1] - pts[i]).getDistance()
        segs[i] = d
        total += d
    }
    if (total <= 0f) return pts.first()
    var remain = total * f
    for (i in segs.indices) {
        if (remain <= segs[i]) {
            val t = if (segs[i] > 0f) remain / segs[i] else 0f
            return pts[i] + (pts[i + 1] - pts[i]) * t
        }
        remain -= segs[i]
    }
    return pts.last()
}

/** 도착 깃발 — 막대 + 삼각 깃발 */
private fun DrawScope.drawFlag(at: Offset) {
    val h = 13.dp.toPx()
    val pole = 2.dp.toPx()
    drawCircle(Volt.copy(alpha = 0.30f), radius = 8.dp.toPx(), center = at)
    drawLine(
        color = Snow,
        start = at,
        end = Offset(at.x, at.y - h),
        strokeWidth = pole,
        cap = StrokeCap.Round,
    )
    val flag = Path().apply {
        moveTo(at.x, at.y - h)
        lineTo(at.x + h * 0.62f, at.y - h * 0.78f)
        lineTo(at.x, at.y - h * 0.56f)
        close()
    }
    drawPath(flag, Volt)
}
