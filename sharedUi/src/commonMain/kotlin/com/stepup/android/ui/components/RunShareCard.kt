package com.stepup.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.stepup.android.domain.GeoPoint
import com.stepup.ui.res.*
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.imageResource
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max

/**
 * 러닝 결과 공유 카드 — 인스타 · 카톡에 올릴 그림 한 장(러닝 전체 리메이크 E05).
 *
 * 남색 바탕에 공식 워드마크(어두운 바탕용 — 글꼴로 흉내 내지 않는다) · 날짜 · 아주 큰 거리 · 달린 시간 | 평균 페이스.
 * 경로를 넣으면 세로(4:5) 그림 가운데에 달린 길을 그린다. 경로를 빼면(기본) 가로로 긴 그림이고 길 · 지도 · 장소가 전혀 없다 —
 * 미리보기와 내보내는 그림이 같은 그림이다.
 *
 * SUP 는 적지 않는다. 서버가 확인하기 전의 적립을 밖으로 내보내면 그 숫자가 나중에 달라질 수 있다.
 *
 * 그림은 공용 Compose 그리기(CanvasDrawScope)로 만든다(2026-11-03 iOS C — 안드로이드 android.graphics 판과 같은 자리 · 크기 · 색).
 * 글자 자리는 안드로이드 drawText 처럼 기준선(baseline)으로 적는다.
 */
object RunShareCard {

    const val WIDTH = 1080
    /** 경로를 넣은 세로 그림(4:5) */
    const val HEIGHT = 1350
    /** 경로를 뺀 가로로 긴 그림 — 거리 · 시간 · 페이스만 */
    const val HEIGHT_NO_ROUTE = 880

    /** 바탕 — 위 #06204A → 아래 #031427 */
    const val TOP = 0xFF06204A.toInt()
    const val BOTTOM = 0xFF031427.toInt()
    private const val MARGIN = 80
    private val WHITE = Color(0xFFF5F8FF)
    private val LABEL = Color(0xFFAAC3EA)
    private val DIVIDER = Color(0xFF1E3A63)
    private val CYAN = Color(0xFF48D9FA)
    /** 자유 러닝 — 남색 바탕 · 아래 띠 · 형광 초록 경로 · 단위 파랑 */
    private val FREE_NAVY = Color(0xFF05213B)
    private val FREE_BAND = Color(0xFF0A2A4D)
    const val FREE_GREEN = 0xFF19F52E.toInt()
    private val FREE_UNIT = Color(0xFF7EAEFF)

    data class Labels(val distance: String, val time: String, val pace: String, val footer: String)

    /** 그림 모양 — 예전 모습(가운데 큰 거리) 또는 자유 러닝(위 지도 자리 · 형광 초록 경로 · 아래 거리 | 시간 | 평균 페이스) */
    enum class Style { Classic, Free }

    /** 그리는 재료 — 글꼴(Paperlogy 보통 · 굵게) · 글자 재기 · 워드마크. 화면(Composable)에서 [rememberKit] 로 받는다 */
    class Kit internal constructor(
        internal val measurer: TextMeasurer,
        internal val regular: FontFamily,
        internal val heavy: FontFamily,
        internal val logo: ImageBitmap?,
    )

    @Composable
    fun rememberKit(): Kit {
        val resolver = LocalFontFamilyResolver.current
        val regular = FontFamily(Font(Res.font.paperlogy_5medium))
        val heavy = FontFamily(Font(Res.font.paperlogy_7bold))
        val logo = imageResource(Res.drawable.logo_wordmark_on_dark)
        // 그림 좌표는 픽셀 그대로 — 1sp = 1px
        val measurer = remember(resolver) { TextMeasurer(resolver, Density(1f, 1f), LayoutDirection.Ltr, cacheSize = 16) }
        return remember(measurer, regular, heavy, logo) { Kit(measurer, regular, heavy, logo) }
    }

    /**
     * @param track 넣을 경로. 비우면 경로 · 지도 없이 가로로 긴 그림으로 그린다.
     * @param date 그림 오른쪽 위 날짜(없으면 비운다)
     */
    fun render(
        kit: Kit,
        km: Double,
        elapsed: String,
        pace: String,
        track: List<GeoPoint>,
        labels: Labels,
        date: String? = null,
        style: Style = Style.Classic,
        /** [Style.Free] 의 거리 글 — 모르면 "—"(걸음으로 셈한 거리를 적지 않는다) */
        distance: String? = null,
    ): ImageBitmap {
        if (style == Style.Free) {
            return renderFree(kit, distance ?: com.stepup.android.domain.formatFixed(km, 2), elapsed, pace, track, labels, date)
        }
        val withRoute = track.size >= 2
        val height = if (withRoute) HEIGHT else HEIGHT_NO_ROUTE
        return paint(WIDTH, height) {
            val pen = Pen(kit, this)
            // 남색 바탕 — 위가 조금 밝고 오른쪽 위에서 파란 빛이 번진다
            drawRect(Brush.verticalGradient(listOf(Color(TOP), Color(BOTTOM)), 0f, height.toFloat()))
            drawRect(
                Brush.radialGradient(
                    listOf(Color(0x660B3A86), Color(0x000B3A86)), center = Offset(WIDTH * 0.92f, 0f), radius = WIDTH * 0.9f,
                ),
            )
            // 로고 — 어두운 바탕용 공식 워드마크 그대로
            kit.logo?.let { logo -> drawLogo(logo, MARGIN, MARGIN, 330) }
            if (!date.isNullOrBlank()) {
                pen.text(date, WIDTH - MARGIN.toFloat(), MARGIN + 52f, 40f, LABEL, kit.regular, alignRight = true)
            }

            // 달린 길(넣을 때만)
            if (withRoute) drawRoute(track, Rect(150f, 230f, WIDTH - 150f, 700f))

            // 거리 — 아주 큰 기울인 굵은 수 + km, 가운데
            val distanceBase = if (withRoute) 905f else 455f
            val number = com.stepup.android.domain.formatFixed(km, 2)
            val numberWidth = pen.width(number, 250f, kit.heavy, SKEW)
            val unitWidth = pen.width(" km", 96f, kit.heavy, SKEW)
            val left = (WIDTH - numberWidth - unitWidth) / 2f
            pen.heavy(number, left, distanceBase, 250f, WHITE, kit.heavy)
            pen.heavy(" km", left + numberWidth, distanceBase, 96f, WHITE, kit.heavy)

            // 가는 가로선
            val lineY = distanceBase + 70f
            drawRect(DIVIDER, Offset(MARGIN.toFloat(), lineY), Size(WIDTH - 2f * MARGIN, 3f))

            // 달린 시간 | 평균 페이스
            val statTop = lineY + 90f
            pen.stat(labels.time, elapsed, MARGIN.toFloat(), statTop)
            drawRect(DIVIDER, Offset(WIDTH / 2f - 1.5f, statTop - 40f), Size(3f, 160f))
            pen.stat(labels.pace, "$pace/km", WIDTH / 2f + 50f, statTop)

            pen.text(labels.footer, MARGIN.toFloat(), height - 56f, 30f, LABEL, kit.regular)
        }
    }

    /** 기울인 큰 숫자의 기울기(안드로이드 Paint.textSkewX 와 같은 값) */
    private const val SKEW = -0.2f

    /** 글자를 기준선 자리에 쓰는 붓 — 안드로이드 Canvas.drawText(text, x, baseline, paint) 와 같은 자리 */
    private class Pen(private val kit: Kit, private val scope: DrawScope) {
        private fun style(size: Float, color: Color, family: FontFamily, skew: Float, stroke: Float?) = TextStyle(
            color = color, fontSize = size.sp, fontFamily = family,
            textGeometricTransform = if (skew != 0f) TextGeometricTransform(skewX = skew) else null,
            drawStyle = stroke?.let { Stroke(width = it, join = StrokeJoin.Round) } ?: Fill,
        )

        fun width(text: String, size: Float, family: FontFamily, skew: Float = 0f): Float =
            kit.measurer.measure(text, style(size, Color.White, family, skew, null), softWrap = false).size.width.toFloat()

        fun text(
            text: String, x: Float, baseline: Float, size: Float, color: Color, family: FontFamily,
            alignRight: Boolean = false, skew: Float = 0f, stroke: Float? = null,
        ) {
            val layout = kit.measurer.measure(text, style(size, color, family, skew, stroke), softWrap = false)
            val left = if (alignRight) x - layout.size.width else x
            scope.drawText(layout, topLeft = Offset(left, baseline - layout.firstBaseline))
        }

        /** 글꼴 가장 굵은 굵기보다 더 굵게 — 같은 색 테두리를 한 겹 더 그린다(앱의 큰 운동 숫자와 같은 모양) */
        fun heavy(text: String, x: Float, baseline: Float, size: Float, color: Color, family: FontFamily, weight: Float = 0.035f, skew: Float = SKEW) {
            text(text, x, baseline, size, color, family, skew = skew)
            text(text, x, baseline, size, color, family, skew = skew, stroke = size * weight)
        }

        fun stat(label: String, value: String, x: Float, y: Float) {
            text(label, x, y, 38f, LABEL, kit.regular)
            heavy(value, x, y + 100f, 88f, WHITE, kit.heavy)
        }
    }

    private fun paint(width: Int, height: Int, block: DrawScope.() -> Unit): ImageBitmap {
        val image = ImageBitmap(width, height)
        CanvasDrawScope().draw(Density(1f, 1f), LayoutDirection.Ltr, Canvas(image), Size(width.toFloat(), height.toFloat()), block)
        return image
    }

    private fun DrawScope.drawLogo(logo: ImageBitmap, x: Int, y: Int, w: Int) {
        val h = w * logo.height / logo.width
        drawImage(logo, srcOffset = IntOffset.Zero, srcSize = IntSize(logo.width, logo.height), dstOffset = IntOffset(x, y), dstSize = IntSize(w, h))
    }

    /** 경로를 상자 안에 비율을 지켜 맞춰 그린다. 경로가 없으면 아무것도 그리지 않는다. */
    private fun DrawScope.drawRoute(track: List<GeoPoint>, box: Rect) {
        if (track.size < 2) return
        val points = fit(track, box)
        val path = Path()
        points.forEachIndexed { i, (x, y) -> if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }
        // 흐린 두꺼운 선 위에 선명한 선 — 빛나는 시안 경로
        drawPath(path, Color(0x5548D9FA), style = Stroke(36f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path, CYAN, style = Stroke(14f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        // 출발점(흰 점) · 도착점(흰 테 파란 점)
        val (sx, sy) = points.first()
        val (ex, ey) = points.last()
        drawCircle(WHITE, 16f, Offset(sx, sy))
        drawCircle(WHITE, 24f, Offset(ex, ey))
        drawCircle(Color(0xFF0754FF), 15f, Offset(ex, ey))
    }

    /**
     * 자유 러닝 공유 그림(전달본 10) — 늘 세로(4:5) 남색. 위 대부분은 경로 자리(넣을 때만 형광 초록 경로 · 고리 출발점 · 화살표 끝점,
     * 빼면 길 · 지도 · 장소가 전혀 없다), 왼쪽 위 공식 워드마크 · 오른쪽 위 날짜, 아래 띠에 거리 | 시간 | 평균 페이스.
     * 보상(SUP) · 아래 안내 글은 두지 않는다.
     */
    private fun renderFree(
        kit: Kit,
        distance: String,
        elapsed: String,
        pace: String,
        track: List<GeoPoint>,
        labels: Labels,
        date: String?,
    ): ImageBitmap = paint(WIDTH, HEIGHT) {
        val pen = Pen(kit, this)
        val band = 290f
        val bandTop = HEIGHT - band
        drawRect(Brush.verticalGradient(listOf(Color(0xFF0A2C52), FREE_NAVY), 0f, bandTop), size = Size(WIDTH.toFloat(), bandTop))

        if (track.size >= 2) drawFreeRoute(track, Rect(140f, 240f, WIDTH - 140f, bandTop - 110f))

        kit.logo?.let { logo -> drawLogo(logo, 64, 64, 300) }
        if (!date.isNullOrBlank()) pen.text(date, WIDTH - 64f, 112f, 40f, WHITE, kit.regular, alignRight = true)

        // 아래 띠 — 거리 | 시간 | 평균 페이스
        drawRect(FREE_BAND, Offset(0f, bandTop), Size(WIDTH.toFloat(), band))
        drawRect(DIVIDER, Offset(0f, bandTop), Size(WIDTH.toFloat(), 3f))
        val column = WIDTH / 3f
        val cells = listOf(
            Triple(labels.distance, distance, " km"),
            Triple(labels.time, elapsed, null),
            Triple(labels.pace, pace, " /km"),
        )
        cells.forEachIndexed { i, (label, value, unit) ->
            val left = column * i + 56f
            val room = column - 84f
            pen.text(label, left, bandTop + 92f, 38f, LABEL, kit.regular)
            // 한 칸에 들어가게 — 넘치면 수 · 단위를 같은 비율로 줄인다
            val valueWidth = pen.width(value, 92f, kit.heavy)
            val unitWidth = unit?.let { pen.width(it, 44f, kit.heavy) } ?: 0f
            val scale = minOf(1f, room / (valueWidth + unitWidth).coerceAtLeast(1f))
            pen.heavy(value, left, bandTop + 210f, 92f * scale, WHITE, kit.heavy, weight = 0.02f, skew = 0f)
            if (unit != null) pen.text(unit, left + valueWidth * scale, bandTop + 210f, 44f * scale, FREE_UNIT, kit.regular)
            if (i > 0) drawRect(DIVIDER, Offset(column * i - 1.5f, bandTop + 56f), Size(3f, 174f))
        }
    }

    /** 자유 러닝 경로 — 남색 테 위 형광 초록 선, 고리 출발점 · 마지막 방향 화살표 끝점 */
    private fun DrawScope.drawFreeRoute(track: List<GeoPoint>, box: Rect) {
        val points = fit(track, box)
        val path = Path()
        points.forEachIndexed { i, (x, y) -> if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }
        val green = Color(FREE_GREEN)
        drawPath(path, FREE_NAVY, style = Stroke(28f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path, green, style = Stroke(16f, cap = StrokeCap.Round, join = StrokeJoin.Round))

        val (sx, sy) = points.first()
        val start = Offset(sx, sy)
        drawCircle(FREE_NAVY, 30f, start); drawCircle(green, 25f, start)
        drawCircle(FREE_NAVY, 16f, start); drawCircle(green, 8f, start)

        val (ex, ey) = points.last()
        var from = points.lastIndex - 1
        while (from > 0 && hypot(ex - points[from].first, ey - points[from].second) < 1f) from--
        val dx = ex - points[from].first
        val dy = ey - points[from].second
        val degrees = if (hypot(dx, dy) < 1f) -90f else (atan2(dy, dx) * 180.0 / PI).toFloat()
        val end = Offset(ex, ey)
        drawCircle(FREE_NAVY, 38f, end); drawCircle(green, 32f, end)
        val r = 18f
        val arrow = Path().apply {
            moveTo(r, 0f)
            lineTo(-r * 0.75f, -r * 0.7f)
            lineTo(-r * 0.35f, 0f)
            lineTo(-r * 0.75f, r * 0.7f)
            close()
        }
        translate(ex, ey) {
            rotate(degrees, pivot = Offset.Zero) { drawPath(arrow, WHITE) }
        }
    }

    /** 경로 점을 상자 안에 비율을 지켜 맞춘 화면 좌표로(위도에 따라 줄어드는 경도 길이를 바로잡는다) */
    private fun fit(track: List<GeoPoint>, box: Rect): List<Pair<Float, Float>> {
        val midLat = track.map { it.lat }.average()
        val lngScale = cos(midLat * PI / 180.0)
        val xs = track.map { it.lng * lngScale }
        val ys = track.map { -it.lat }
        val minX = xs.min()
        val minY = ys.min()
        val spanX = max(xs.max() - minX, 1e-9)
        val spanY = max(ys.max() - minY, 1e-9)
        val scale = minOf(box.width / spanX, box.height / spanY)
        val offX = box.left + (box.width - spanX * scale) / 2
        val offY = box.top + (box.height - spanY * scale) / 2
        return xs.indices.map { i -> Pair((offX + (xs[i] - minX) * scale).toFloat(), (offY + (ys[i] - minY) * scale).toFloat()) }
    }

    /** 글만 공유한다 — 그림을 만들지 못했거나 그림 공유가 실패했을 때. 받을 앱이 없으면 false. */
    fun shareText(text: String): Boolean = com.stepup.android.core.ExternalIntents.shareText(text)

    /** 그림(PNG)과 글을 공유 창으로 — 안드로이드 FileProvider 공유 · iOS 공유 시트 */
    suspend fun share(image: ImageBitmap, text: String, chooserTitle: String?) = shareImage(image, text, chooserTitle)
}

/** 그림 공유(기기마다) — 그림은 PNG 로 */
expect suspend fun shareImage(image: ImageBitmap, text: String, chooserTitle: String?)

/**
 * 러닝 그림을 사진첩에 남기는 함수를 돌려준다(안드로이드 MediaStore — 옛 판은 저장 권한을 먼저 · iOS 사진 보관함).
 * 결과는 [onResult](남겼나)로 온다.
 */
@Composable
expect fun rememberRunImageSaver(onResult: (Boolean) -> Unit): (image: ImageBitmap, name: String) -> Unit
