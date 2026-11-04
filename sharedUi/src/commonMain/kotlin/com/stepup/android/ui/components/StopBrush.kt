package com.stepup.android.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode

/**
 * 그라디언트 붓 — `Brush.verticalGradient` · `horizontalGradient` · `linearGradient` · `radialGradient` · `sweepGradient` 과
 * 이름 · 인자가 같다. 화면 코드는 `Brush.` 대신 이것을 쓴다(`tools/check_gradients.py` 가 막는다).
 *
 * 실제 폰 · iOS 에서는 그 `Brush` 를 그대로 돌려준다 — 보이는 것 · 성능이 같다.
 * 안드로이드 에뮬레이터에서만, 세 색 이상(또는 0 · 1 이 아닌 자리의 색) 그라디언트를 CPU 로 미리 그린 띠 그림으로 대신한다
 * ([rasterGradient] — androidMain). 까닭(2026-11-04): 기기 검사 에뮬레이터의 소프트웨어 GPU(SwiftShader)가 여러 색 그라디언트를
 * 그리다 에뮬레이터째 죽었다 — core dump 는 래스터라이저의 범위 밖 읽기, 같은 자리를 두 색으로 바꾼 판 · 그림(사진)은 끝까지 살았다
 * (Emulator crash diag run 41779362780 · 41781375163, docs/ios/README.md "기기 검사 에뮬레이터").
 */
object StopBrush {
    fun verticalGradient(colors: List<Color>, startY: Float = 0f, endY: Float = Float.POSITIVE_INFINITY, tileMode: TileMode = TileMode.Clamp): Brush =
        pick(GradientSpec.linear(colors, null, Offset(0f, startY), Offset(0f, endY), tileMode)) { Brush.verticalGradient(colors, startY, endY, tileMode) }

    fun verticalGradient(vararg colorStops: Pair<Float, Color>, startY: Float = 0f, endY: Float = Float.POSITIVE_INFINITY, tileMode: TileMode = TileMode.Clamp): Brush =
        pick(GradientSpec.linear(colorStops.map { it.second }, colorStops.map { it.first }, Offset(0f, startY), Offset(0f, endY), tileMode)) {
            Brush.verticalGradient(*colorStops, startY = startY, endY = endY, tileMode = tileMode)
        }

    fun horizontalGradient(colors: List<Color>, startX: Float = 0f, endX: Float = Float.POSITIVE_INFINITY, tileMode: TileMode = TileMode.Clamp): Brush =
        pick(GradientSpec.linear(colors, null, Offset(startX, 0f), Offset(endX, 0f), tileMode)) { Brush.horizontalGradient(colors, startX, endX, tileMode) }

    fun horizontalGradient(vararg colorStops: Pair<Float, Color>, startX: Float = 0f, endX: Float = Float.POSITIVE_INFINITY, tileMode: TileMode = TileMode.Clamp): Brush =
        pick(GradientSpec.linear(colorStops.map { it.second }, colorStops.map { it.first }, Offset(startX, 0f), Offset(endX, 0f), tileMode)) {
            Brush.horizontalGradient(*colorStops, startX = startX, endX = endX, tileMode = tileMode)
        }

    fun linearGradient(colors: List<Color>, start: Offset = Offset.Zero, end: Offset = Offset.Infinite, tileMode: TileMode = TileMode.Clamp): Brush =
        pick(GradientSpec.linear(colors, null, start, end, tileMode)) { Brush.linearGradient(colors, start, end, tileMode) }

    fun linearGradient(vararg colorStops: Pair<Float, Color>, start: Offset = Offset.Zero, end: Offset = Offset.Infinite, tileMode: TileMode = TileMode.Clamp): Brush =
        pick(GradientSpec.linear(colorStops.map { it.second }, colorStops.map { it.first }, start, end, tileMode)) {
            Brush.linearGradient(*colorStops, start = start, end = end, tileMode = tileMode)
        }

    fun radialGradient(colors: List<Color>, center: Offset = Offset.Unspecified, radius: Float = Float.POSITIVE_INFINITY, tileMode: TileMode = TileMode.Clamp): Brush =
        pick(GradientSpec(GradientKind.Radial, colors, null, center = center, radius = radius, tileMode = tileMode)) {
            Brush.radialGradient(colors, center, radius, tileMode)
        }

    fun radialGradient(vararg colorStops: Pair<Float, Color>, center: Offset = Offset.Unspecified, radius: Float = Float.POSITIVE_INFINITY, tileMode: TileMode = TileMode.Clamp): Brush =
        pick(GradientSpec(GradientKind.Radial, colorStops.map { it.second }, colorStops.map { it.first }, center = center, radius = radius, tileMode = tileMode)) {
            Brush.radialGradient(*colorStops, center = center, radius = radius, tileMode = tileMode)
        }

    fun sweepGradient(colors: List<Color>, center: Offset = Offset.Unspecified): Brush =
        pick(GradientSpec(GradientKind.Sweep, colors, null, center = center)) { Brush.sweepGradient(colors, center) }

    fun sweepGradient(vararg colorStops: Pair<Float, Color>, center: Offset = Offset.Unspecified): Brush =
        pick(GradientSpec(GradientKind.Sweep, colorStops.map { it.second }, colorStops.map { it.first }, center = center)) {
            Brush.sweepGradient(*colorStops, center = center)
        }

    private inline fun pick(spec: GradientSpec, plain: () -> Brush): Brush =
        if (gradientsNeedRaster && spec.needsRaster()) rasterGradient(spec, plain()) else plain()
}

enum class GradientKind { Linear, Radial, Sweep }

/** 그라디언트 한 벌의 값 — 위치(무한 · 정하지 않음)는 그릴 크기에서 Compose 와 같은 규칙으로 푼다 */
data class GradientSpec(
    val kind: GradientKind,
    val colors: List<Color>,
    val stops: List<Float>?,
    val start: Offset = Offset.Zero,
    val end: Offset = Offset.Infinite,
    val center: Offset = Offset.Unspecified,
    val radius: Float = Float.POSITIVE_INFINITY,
    val tileMode: TileMode = TileMode.Clamp,
) {
    /** 두 색(처음 0 · 끝 1)이면 GPU 가 한 구간으로 그려 괜찮았다. 늘어놓기(Clamp)가 아니면 그대로 둔다 */
    fun needsRaster(): Boolean {
        if (tileMode != TileMode.Clamp) return false
        if (colors.size >= 3) return true
        val s = stops ?: return false
        return s.size == 2 && (s[0] != 0f || s[1] != 1f)
    }

    companion object {
        fun linear(colors: List<Color>, stops: List<Float>?, start: Offset, end: Offset, tileMode: TileMode) =
            GradientSpec(GradientKind.Linear, colors, stops, start = start, end = end, tileMode = tileMode)
    }
}

/** 이 기기에서 여러 색 그라디언트를 미리 그려야 하는가 — 안드로이드 에뮬레이터만 true */
expect val gradientsNeedRaster: Boolean

/** [spec] 을 미리 그린 그림으로 칠하는 붓. 못 만들면 [plain] */
expect fun rasterGradient(spec: GradientSpec, plain: Brush): Brush
