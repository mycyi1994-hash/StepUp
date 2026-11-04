package com.stepup.android.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader as AndroidShader
import android.graphics.SweepGradient
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** 안드로이드 에뮬레이터(ranchu · goldfish) — 그리기가 러너의 SwiftShader 로 간다(StopBrush.kt). 시험 환경(JVM)에서는 false */
actual val gradientsNeedRaster: Boolean = runCatching {
    val hw = Build.HARDWARE.orEmpty()
    hw == "ranchu" || hw == "goldfish" || Build.FINGERPRINT.orEmpty().startsWith("generic")
}.getOrDefault(false)

actual fun rasterGradient(spec: GradientSpec, plain: Brush): Brush = RasterGradientBrush(spec)

/**
 * [spec] 을 CPU 로 미리 그린 띠 그림(비트맵)으로 칠한다 — GPU 는 그림을 늘려 붙이기만 한다(사진과 같은 길).
 * 선형은 축 방향 1줄 띠(한 픽셀에 한 칸), 원형은 원 하나 크기의 정사각형(최대 256), 각도형은 그릴 칸 크기(최대 256)를 그린다.
 * 위치는 그릴 크기에서 Compose 와 같은 규칙으로 푼다(무한 = 크기, 정하지 않은 가운데 = 크기의 가운데, 무한 반지름 = 짧은 변의 반).
 */
private class RasterGradientBrush(val spec: GradientSpec) : ShaderBrush() {
    override fun createShader(size: Size): Shader = RasterGradients.shader(spec, size)
    override fun equals(other: Any?) = other is RasterGradientBrush && other.spec == spec
    override fun hashCode() = spec.hashCode()
    override fun toString() = "RasterGradientBrush($spec)"
}

private object RasterGradients {
    private const val MAX_LINE = 2048
    private const val MAX_SQUARE = 256
    private data class Key(val spec: GradientSpec, val w: Int, val h: Int)
    private val cache = LruCache<Key, AndroidShader>(96)

    fun shader(spec: GradientSpec, size: Size): Shader {
        val key = Key(spec, (size.width * 2).roundToInt(), (size.height * 2).roundToInt())
        synchronized(cache) { cache.get(key) }?.let { return it }
        val made = make(spec, size)
        synchronized(cache) { cache.put(key, made) }
        return made
    }

    private fun colors(spec: GradientSpec) = IntArray(spec.colors.size) { spec.colors[it].toArgb() }
    private fun stops(spec: GradientSpec) = spec.stops?.toFloatArray()
    private val clamp = AndroidShader.TileMode.CLAMP

    private fun make(spec: GradientSpec, size: Size): AndroidShader = when (spec.kind) {
        GradientKind.Linear -> linear(spec, size)
        GradientKind.Radial -> radial(spec, size)
        GradientKind.Sweep -> sweep(spec, size)
    }

    private fun resolve(p: Offset, size: Size) = Offset(
        if (p.x == Float.POSITIVE_INFINITY) size.width else p.x,
        if (p.y == Float.POSITIVE_INFINITY) size.height else p.y,
    )

    private fun centerOf(spec: GradientSpec, size: Size) = if (spec.center.isUnspecified) size.center else resolve(spec.center, size)

    private fun linear(spec: GradientSpec, size: Size): AndroidShader {
        val s = resolve(spec.start, size)
        val e = resolve(spec.end, size)
        val dx = e.x - s.x
        val dy = e.y - s.y
        val length = hypot(dx, dy)
        if (!(length >= 1f) || !length.isFinite()) return LinearGradient(s.x, s.y, e.x, e.y, colors(spec), stops(spec), clamp)
        val n = ceil(length).toInt().coerceIn(2, MAX_LINE)
        val bitmap = Bitmap.createBitmap(n, 1, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawRect(0f, 0f, n.toFloat(), 1f, Paint().apply {
            isDither = true
            shader = LinearGradient(0f, 0f, n.toFloat(), 0f, colors(spec), stops(spec), clamp)
        })
        return BitmapShader(bitmap, clamp, clamp).apply {
            setLocalMatrix(Matrix().apply {
                setScale(length / n, 1f)
                postTranslate(0f, -0.5f)
                postRotate(Math.toDegrees(atan2(dy, dx).toDouble()).toFloat())
                postTranslate(s.x, s.y)
            })
        }
    }

    private fun radial(spec: GradientSpec, size: Size): AndroidShader {
        val c = centerOf(spec, size)
        val r = if (spec.radius == Float.POSITIVE_INFINITY) size.minDimension / 2 else spec.radius
        if (!(r >= 1f) || !r.isFinite()) return RadialGradient(c.x, c.y, max(r, 0.01f), colors(spec), stops(spec), clamp)
        val n = ceil(2 * r).toInt().coerceIn(2, MAX_SQUARE)
        val bitmap = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
        val half = n / 2f
        Canvas(bitmap).drawRect(0f, 0f, n.toFloat(), n.toFloat(), Paint().apply {
            isDither = true
            shader = RadialGradient(half, half, half, colors(spec), stops(spec), clamp)
        })
        return BitmapShader(bitmap, clamp, clamp).apply {
            setLocalMatrix(Matrix().apply {
                setScale(2 * r / n, 2 * r / n)
                postTranslate(c.x - r, c.y - r)
            })
        }
    }

    private fun sweep(spec: GradientSpec, size: Size): AndroidShader {
        val c = centerOf(spec, size)
        val w = max(size.width, 1f)
        val h = max(size.height, 1f)
        val k = min(1f, MAX_SQUARE / max(w, h))
        val bw = ceil(w * k).toInt().coerceAtLeast(2)
        val bh = ceil(h * k).toInt().coerceAtLeast(2)
        val sx = bw / w
        val sy = bh / h
        val bitmap = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            scale(sx, sy)
            drawRect(0f, 0f, w, h, Paint().apply { isDither = true; shader = SweepGradient(c.x, c.y, colors(spec), stops(spec)) })
        }
        return BitmapShader(bitmap, clamp, clamp).apply { setLocalMatrix(Matrix().apply { setScale(1f / sx, 1f / sy) }) }
    }
}
