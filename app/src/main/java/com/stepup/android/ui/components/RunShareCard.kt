package com.stepup.android.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.stepup.android.R
import com.stepup.android.domain.GeoPoint
import java.io.File
import kotlin.math.cos
import kotlin.math.max

/**
 * 러닝 결과 공유 카드 — 인스타 · 카톡에 올릴 그림 한 장(러닝 전체 리메이크 E05).
 *
 * 남색 바탕에 공식 워드마크(어두운 바탕용 — 글꼴로 흉내 내지 않는다) · 날짜 · 아주 큰 거리 · 달린 시간 | 평균 페이스.
 * 경로를 넣으면 세로(4:5) 그림 가운데에 달린 길을 그린다. 경로를 빼면(기본) 가로로 긴 그림이고 길 · 지도 · 장소가 전혀 없다 —
 * 미리보기와 내보내는 그림이 같은 그림이다.
 *
 * SUP 는 적지 않는다. 서버가 확인하기 전의 적립을 밖으로 내보내면 그 숫자가 나중에 달라질 수 있다.
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
    private const val WHITE = 0xFFF5F8FF.toInt()
    private const val LABEL = 0xFFAAC3EA.toInt()
    private const val DIVIDER = 0xFF1E3A63.toInt()
    private const val CYAN = 0xFF48D9FA.toInt()

    data class Labels(val distance: String, val time: String, val pace: String, val footer: String)

    /**
     * @param track 넣을 경로. 비우면 경로 · 지도 없이 가로로 긴 그림으로 그린다.
     * @param date 그림 오른쪽 위 날짜(없으면 비운다)
     */
    fun render(
        context: Context,
        km: Double,
        elapsed: String,
        pace: String,
        track: List<GeoPoint>,
        labels: Labels,
        date: String? = null,
    ): Bitmap {
        val withRoute = track.size >= 2
        val height = if (withRoute) HEIGHT else HEIGHT_NO_ROUTE
        val bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 남색 바탕 — 위가 조금 밝고 오른쪽 위에서 파란 빛이 번진다
        paint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), TOP, BOTTOM, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), height.toFloat(), paint)
        paint.shader = android.graphics.RadialGradient(
            WIDTH * 0.92f, 0f, WIDTH * 0.9f, 0x660B3A86, 0x000B3A86, Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), height.toFloat(), paint)
        paint.shader = null

        // 로고 — 어두운 바탕용 공식 워드마크 그대로
        BitmapFactory.decodeResource(context.resources, R.drawable.logo_wordmark_on_dark)?.let { logo ->
            val w = 330
            val h = w * logo.height / logo.width
            canvas.drawBitmap(logo, null, Rect(MARGIN, MARGIN, MARGIN + w, MARGIN + h), paint)
        }
        val regular = ResourcesCompat.getFont(context, R.font.pretendard_medium)
        val heavy = ResourcesCompat.getFont(context, R.font.pretendard_extrabold)
        if (!date.isNullOrBlank()) {
            paint.color = LABEL
            paint.typeface = regular
            paint.textSize = 40f
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(date, WIDTH - MARGIN.toFloat(), MARGIN + 52f, paint)
            paint.textAlign = Paint.Align.LEFT
        }

        // 달린 길(넣을 때만)
        if (withRoute) drawRoute(canvas, track, RectF(150f, 230f, WIDTH - 150f, 700f))

        // 거리 — 아주 큰 기울인 굵은 수 + km, 가운데
        val distanceBase = if (withRoute) 905f else 455f
        val distance = "%.2f".format(km)
        paint.color = WHITE
        paint.typeface = heavy
        paint.textSkewX = -0.2f
        paint.textSize = 250f
        val numberWidth = paint.measureText(distance)
        paint.textSize = 96f
        val unitWidth = paint.measureText(" km")
        val left = (WIDTH - numberWidth - unitWidth) / 2f
        paint.textSize = 250f
        heavyText(canvas, paint, distance, left, distanceBase)
        paint.textSize = 96f
        heavyText(canvas, paint, " km", left + numberWidth, distanceBase)
        paint.textSkewX = 0f

        // 가는 가로선
        val lineY = distanceBase + 70f
        paint.color = DIVIDER
        canvas.drawRect(MARGIN.toFloat(), lineY, WIDTH - MARGIN.toFloat(), lineY + 3f, paint)

        // 달린 시간 | 평균 페이스
        val statTop = lineY + 90f
        stat(canvas, paint, labels.time, elapsed, MARGIN.toFloat(), statTop, regular, heavy)
        canvas.drawRect(WIDTH / 2f - 1.5f, statTop - 40f, WIDTH / 2f + 1.5f, statTop + 120f, Paint().apply { color = DIVIDER })
        stat(canvas, paint, labels.pace, "$pace/km", WIDTH / 2f + 50f, statTop, regular, heavy)

        paint.typeface = regular
        paint.textSize = 30f
        paint.color = LABEL
        canvas.drawText(labels.footer, MARGIN.toFloat(), height - 56f, paint)
        return bitmap
    }

    /** Pretendard 가장 굵은 굵기보다 더 굵게 — 같은 색 테두리를 한 겹 더 그린다(앱의 큰 운동 숫자와 같은 모양) */
    private fun heavyText(canvas: Canvas, paint: Paint, text: String, x: Float, y: Float) {
        val style = paint.style
        val stroke = paint.strokeWidth
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.strokeWidth = paint.textSize * 0.035f
        paint.strokeJoin = Paint.Join.ROUND
        canvas.drawText(text, x, y, paint)
        paint.style = style
        paint.strokeWidth = stroke
    }

    private fun stat(
        canvas: Canvas,
        paint: Paint,
        label: String,
        value: String,
        x: Float,
        y: Float,
        labelFace: android.graphics.Typeface?,
        valueFace: android.graphics.Typeface?,
    ) {
        paint.color = LABEL
        paint.typeface = labelFace
        paint.textSize = 38f
        canvas.drawText(label, x, y, paint)
        paint.color = WHITE
        paint.typeface = valueFace
        paint.textSkewX = -0.2f
        paint.textSize = 88f
        heavyText(canvas, paint, value, x, y + 100f)
        paint.textSkewX = 0f
    }

    /** 경로를 상자 안에 비율을 지켜 맞춰 그린다. 경로가 없으면 아무것도 그리지 않는다. */
    private fun drawRoute(canvas: Canvas, track: List<GeoPoint>, box: RectF) {
        if (track.size < 2) return
        // 위도에 따라 경도 1도의 길이가 줄어든다. 그대로 그리면 길이 옆으로 늘어난다.
        val midLat = track.map { it.lat }.average()
        val lngScale = cos(Math.toRadians(midLat))
        val xs = track.map { it.lng * lngScale }
        val ys = track.map { -it.lat }
        val minX = xs.min()
        val minY = ys.min()
        val spanX = max(xs.max() - minX, 1e-9)
        val spanY = max(ys.max() - minY, 1e-9)
        val scale = minOf(box.width() / spanX, box.height() / spanY)
        val offX = box.left + (box.width() - spanX * scale) / 2
        val offY = box.top + (box.height() - spanY * scale) / 2

        val path = Path()
        xs.indices.forEach { i ->
            val x = (offX + (xs[i] - minX) * scale).toFloat()
            val y = (offY + (ys[i] - minY) * scale).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        // 흐린 두꺼운 선 위에 선명한 선 — 빛나는 시안 경로
        stroke.color = 0x5548D9FA
        stroke.strokeWidth = 36f
        canvas.drawPath(path, stroke)
        stroke.color = CYAN
        stroke.strokeWidth = 14f
        canvas.drawPath(path, stroke)

        // 출발점(흰 점) · 도착점(흰 테 파란 점)
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = WHITE }
        fun at(i: Int) = Pair(
            (offX + (xs[i] - minX) * scale).toFloat(),
            (offY + (ys[i] - minY) * scale).toFloat(),
        )
        val (sx, sy) = at(0)
        val (ex, ey) = at(xs.lastIndex)
        canvas.drawCircle(sx, sy, 16f, dot)
        canvas.drawCircle(ex, ey, 24f, dot)
        dot.color = 0xFF0754FF.toInt()
        canvas.drawCircle(ex, ey, 15f, dot)
    }

    /** 그림을 캐시에 저장하고 공유 창을 연다. 큰 그림의 PNG 저장은 화면 스레드 밖에서 한다(누르는 순간 멈칫하지 않게). */
    suspend fun share(context: Context, bitmap: Bitmap, text: String, chooserTitle: String?) {
        val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val dir = File(context.cacheDir, "shared").apply { mkdirs() }
            File(dir, "stepup-run.png").also { out -> out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.share", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, chooserTitle))
    }
}
