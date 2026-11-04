package com.stepup.android.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

/**
 * 세 색 이상 세로 그라디언트를 두 색 띠로 나눠 그린다 — 보이는 색은 `background(Brush.verticalGradient(*stops))` 와 같다
 * (구간마다 두 색 사이를 곧게 잇는 것이 여러 색 그라디언트 그 자체다. 첫 점 위 · 끝 점 아래는 그 색으로 채운다 — Clamp).
 *
 * 까닭(2026-11-04): 기기 검사 에뮬레이터의 소프트웨어 GPU(SwiftShader)가 사진 카드 · 신발 카드의 여러 색 그라디언트를 그리다
 * 에뮬레이터째 죽었다 — core dump 는 래스터라이저의 범위 밖 읽기, 같은 자리를 두 색으로 바꾼 판은 검사 끝까지 살았다
 * (Emulator crash diag run 41779362780 · 41781375163). 실제 폰은 그대로 그렸다. 띠 경계는 픽셀에 맞춰 이음매가 없다.
 */
fun Modifier.verticalStops(vararg stops: Pair<Float, Color>): Modifier = drawWithCache {
    val bands = verticalBands(size, stops.toList())
    onDrawBehind {
        for (band in bands) {
            val brush = band.brush
            if (brush != null) drawRect(brush, Offset(0f, band.top), Size(size.width, band.bottom - band.top))
            else drawRect(band.color, Offset(0f, band.top), Size(size.width, band.bottom - band.top))
        }
    }
}

/** 한 띠 — 두 색이면 [brush], 한 색이면 [color] */
class VerticalBand(val top: Float, val bottom: Float, val color: Color, val brush: Brush?)

/** 높이 [size] 를 [stops](0 ~ 1, 오름차순)로 나눈 띠들. 경계는 정수 픽셀 */
fun verticalBands(size: Size, stops: List<Pair<Float, Color>>): List<VerticalBand> {
    if (stops.isEmpty() || size.height <= 0f) return emptyList()
    val h = size.height
    fun y(p: Float) = (p.coerceIn(0f, 1f) * h).roundToInt().toFloat().coerceAtMost(h)
    val out = ArrayList<VerticalBand>(stops.size + 1)
    val firstY = y(stops.first().first)
    if (firstY > 0f) out += VerticalBand(0f, firstY, stops.first().second, null)
    for (i in 0 until stops.size - 1) {
        val (p0, c0) = stops[i]
        val (p1, c1) = stops[i + 1]
        val y0 = y(p0)
        val y1 = y(p1)
        if (y1 <= y0) continue
        val brush = if (c0 == c1) null else Brush.verticalGradient(listOf(c0, c1), startY = y0, endY = y1)
        out += VerticalBand(y0, y1, c0, brush)
    }
    val lastY = y(stops.last().first)
    if (lastY < h) out += VerticalBand(lastY, h, stops.last().second, null)
    return out
}
