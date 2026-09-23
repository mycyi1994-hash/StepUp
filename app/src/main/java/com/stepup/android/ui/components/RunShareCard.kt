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
 * 러닝 결과 공유 카드 — 인스타 스토리·카톡에 올릴 그림 한 장.
 *
 * 파란 면에 공식 로고(`design/brand/stepup-logo-white-on-blue`, 브랜드 규칙의
 * "공유 이미지" 자리)를 얹고, 달린 길과 거리·시간·페이스를 적는다. 로고는
 * 받은 그림을 그대로 쓴다 — 글꼴로 흉내 내지 않는다.
 *
 * SUP 는 적지 않는다. 서버가 확인하기 전의 적립을 밖으로 내보내면 그 숫자가
 * 나중에 달라질 수 있다.
 */
object RunShareCard {

    /** 인스타 세로 게시물 비율(4:5) */
    const val WIDTH = 1080
    const val HEIGHT = 1350

    private const val BLUE_TOP = 0xFF0A76FD.toInt()
    private const val BLUE_BOTTOM = 0xFF0450C8.toInt()
    private const val WHITE = 0xFFFDFDFD.toInt()
    private const val WHITE_DIM = 0xCCFDFDFD.toInt()

    data class Labels(val distance: String, val time: String, val pace: String, val footer: String)

    fun render(
        context: Context,
        km: Double,
        elapsed: String,
        pace: String,
        track: List<GeoPoint>,
        labels: Labels,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 바탕 — 브랜드 파랑에서 조금 깊은 파랑으로
        paint.shader = LinearGradient(0f, 0f, 0f, HEIGHT.toFloat(), BLUE_TOP, BLUE_BOTTOM, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), paint)
        paint.shader = null

        // 로고 — 받은 그림 그대로. 바탕이 같은 파랑이라 이음새가 보이지 않는다.
        BitmapFactory.decodeResource(context.resources, R.drawable.brand_logo_white_on_blue)?.let { logo ->
            val w = 560
            val h = w * logo.height / logo.width
            canvas.drawBitmap(logo, null, Rect((WIDTH - w) / 2, 40, (WIDTH + w) / 2, 40 + h), paint)
        }

        // 달린 길
        drawRoute(canvas, track, RectF(140f, 280f, WIDTH - 140f, 820f))

        val bold = ResourcesCompat.getFont(context, R.font.pretendard_extrabold)
        val regular = ResourcesCompat.getFont(context, R.font.pretendard_medium)
        val numbers = ResourcesCompat.getFont(context, R.font.barlow_bold)

        // 거리 — 가장 크게
        paint.color = WHITE
        paint.typeface = numbers
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 190f
        canvas.drawText("%.2f".format(km), WIDTH / 2f, 1040f, paint)
        paint.typeface = bold
        paint.textSize = 44f
        canvas.drawText("km", WIDTH / 2f, 1100f, paint)

        // 시간 · 페이스
        stat(canvas, paint, labels.time, elapsed, WIDTH * 0.3f, 1200f, regular, numbers)
        stat(canvas, paint, labels.pace, pace, WIDTH * 0.7f, 1200f, regular, numbers)

        paint.typeface = regular
        paint.textSize = 30f
        paint.color = WHITE_DIM
        canvas.drawText(labels.footer, WIDTH / 2f, HEIGHT - 44f, paint)
        return bitmap
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
        paint.color = WHITE
        paint.typeface = valueFace
        paint.textSize = 64f
        canvas.drawText(value, x, y, paint)
        paint.color = WHITE_DIM
        paint.typeface = labelFace
        paint.textSize = 30f
        canvas.drawText(label, x, y + 46f, paint)
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
        // 흐린 두꺼운 선 위에 선명한 선 — 빛나는 느낌
        stroke.color = 0x40FFFFFF
        stroke.strokeWidth = 34f
        canvas.drawPath(path, stroke)
        stroke.color = WHITE
        stroke.strokeWidth = 14f
        canvas.drawPath(path, stroke)

        // 출발점 · 도착점
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = WHITE }
        fun at(i: Int) = Pair(
            (offX + (xs[i] - minX) * scale).toFloat(),
            (offY + (ys[i] - minY) * scale).toFloat(),
        )
        val (sx, sy) = at(0)
        val (ex, ey) = at(xs.lastIndex)
        canvas.drawCircle(sx, sy, 16f, dot)
        dot.color = 0xFF24D8FF.toInt()
        canvas.drawCircle(ex, ey, 20f, dot)
    }

    /** 그림을 캐시에 저장하고 공유 창을 연다. */
    fun share(context: Context, bitmap: Bitmap, text: String, chooserTitle: String?) {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "stepup-run.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
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
