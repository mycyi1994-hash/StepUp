package com.stepup.android

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stepup.android.ui.components.GradientKind
import com.stepup.android.ui.components.GradientSpec
import com.stepup.android.ui.components.rasterGradient
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.hypot

/**
 * 에뮬레이터에서 여러 색 그라디언트를 대신 그리는 띠 그림(StopBrush · rasterGradient)이 원래 그라디언트와 같은 색인지 —
 * 둘 다 CPU 캔버스(ImageBitmap)에 그려 픽셀을 맞댄다(GPU 를 쓰지 않으니 에뮬레이터 SwiftShader 와 상관없다).
 */
@RunWith(AndroidJUnit4::class)
class StopBrushTest {
    private val w = 320
    private val h = 480
    private val a = Color.Black.copy(alpha = .28f)
    private val b = Color.Transparent
    private val c = Color(0xFF3D74FA)
    private val d = Color.White.copy(alpha = .78f)

    @Test fun 세로_네_색_사진_카드() = same(
        GradientSpec.linear(listOf(a, b, c, d), listOf(0f, .3f, .5f, 1f), Offset(0f, 0f), Offset(0f, Float.POSITIVE_INFINITY), androidx.compose.ui.graphics.TileMode.Clamp),
        Brush.verticalGradient(0f to a, .3f to b, .5f to c, 1f to d),
    )

    @Test fun 가로_세_색_가운데_띠_어긋난_칸() = same(
        GradientSpec.linear(listOf(b, d, b), null, Offset(60f, 0f), Offset(220f, 0f), androidx.compose.ui.graphics.TileMode.Clamp),
        Brush.horizontalGradient(listOf(b, d, b), startX = 60f, endX = 220f),
        topLeft = Offset(30f, 40f), drawSize = Size(260f, 300f),
    )

    @Test fun 비스듬한_세_색() = same(
        GradientSpec.linear(listOf(a, c, d), listOf(0f, .47f, 1f), Offset(12f, 300f), Offset(280f, 20f), androidx.compose.ui.graphics.TileMode.Clamp),
        Brush.linearGradient(0f to a, .47f to c, 1f to d, start = Offset(12f, 300f), end = Offset(280f, 20f)),
    )

    @Test fun 원형_세_색() = same(
        GradientSpec(GradientKind.Radial, listOf(c, a, b), listOf(0f, .45f, 1f), center = Offset(140f, 200f), radius = 110f),
        Brush.radialGradient(0f to c, .45f to a, 1f to b, center = Offset(140f, 200f), radius = 110f),
    )

    @Test fun 원형_가운데_반지름_정하지_않음() = same(
        GradientSpec(GradientKind.Radial, listOf(d, c, b), null),
        Brush.radialGradient(listOf(d, c, b)),
    )

    @Test fun 각도형_다섯_색() = same(
        GradientSpec(GradientKind.Sweep, listOf(b, b, c, d, b), listOf(0f, .66f, .8f, .95f, 1f), center = Offset(160f, 240f)),
        Brush.sweepGradient(0f to b, .66f to b, .8f to c, .95f to d, 1f to b, center = Offset(160f, 240f)),
        skipNear = Offset(160f, 240f) to 24f, maxDiff = 40,
    )

    private fun same(
        spec: GradientSpec,
        plain: Brush,
        topLeft: Offset = Offset.Zero,
        drawSize: Size = Size(w.toFloat(), h.toFloat()),
        skipNear: Pair<Offset, Float>? = null,
        maxDiff: Int = 12,
    ) {
        val want = paint(plain, topLeft, drawSize)
        val got = paint(rasterGradient(spec, plain), topLeft, drawSize)
        var worst = 0
        var sum = 0L
        var count = 0
        for (y in 0 until h) for (x in 0 until w) {
            if (skipNear != null && hypot(x - skipNear.first.x, y - skipNear.first.y) < skipNear.second) continue
            val p = want[x, y]
            val q = got[x, y]
            val diff = maxOf(abs(p.red - q.red), abs(p.green - q.green), abs(p.blue - q.blue), abs(p.alpha - q.alpha)).times(255).toInt()
            worst = maxOf(worst, diff); sum += diff; count++
        }
        val mean = sum.toDouble() / count
        assertTrue("$spec: 가장 큰 차이 $worst/255 · 평균 $mean", worst <= maxDiff && mean <= 1.5)
    }

    private fun paint(brush: Brush, topLeft: Offset, drawSize: Size) = ImageBitmap(w, h).also { image ->
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(w.toFloat(), h.toFloat())) {
            drawRect(brush, topLeft, drawSize)
        }
    }.toPixelMap()
}
