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
 * S2 기록 공유 카드: 푸른 검정 바탕에 도시 야경(S2 원본 그림)을 깔고, 달린 길과
 * 가는 큰 거리 · 시간 · 페이스를 적는다. 로고는 어두운 바탕용 공식 워드마크를
 * 그대로 쓴다 — 글꼴로 흉내 내지 않는다.
 *
 * SUP 는 적지 않는다. 서버가 확인하기 전의 적립을 밖으로 내보내면 그 숫자가
 * 나중에 달라질 수 있다.
 */
object RunShareCard {

    /** 인스타 세로 게시물 비율(4:5) */
    const val WIDTH = 1080
    const val HEIGHT = 1350

    private const val NIGHT = 0xFF05080E.toInt()
    private const val SCENE_BOTTOM = 900f
    private const val MARGIN = 72
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

        // S2 — 푸른 검정 바탕 위쪽에 도시 야경, 아래로 바탕색에 녹아든다
        canvas.drawColor(NIGHT)
        BitmapFactory.decodeResource(context.resources, R.drawable.s2_bg_login)?.let { scene ->
            val target = RectF(0f, 0f, WIDTH.toFloat(), SCENE_BOTTOM)
            val scale = max(target.width() / scene.width, target.height() / scene.height)
            val sw = target.width() / scale
            val sh = target.height() / scale
            val src = Rect(((scene.width - sw) / 2).toInt(), ((scene.height - sh) / 2).toInt(),
                ((scene.width + sw) / 2).toInt(), ((scene.height + sh) / 2).toInt())
            paint.alpha = 150
            canvas.drawBitmap(scene, src, target, paint)
            paint.alpha = 255
        }
        paint.shader = LinearGradient(0f, SCENE_BOTTOM * 0.45f, 0f, SCENE_BOTTOM, 0x0005080E, NIGHT, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), SCENE_BOTTOM, paint)
        paint.shader = null

        // 로고 — 어두운 바탕용 공식 워드마크 그대로
        BitmapFactory.decodeResource(context.resources, R.drawable.logo_wordmark_on_dark)?.let { logo ->
            val w = 300
            val h = w * logo.height / logo.width
            canvas.drawBitmap(logo, null, Rect(MARGIN, MARGIN, MARGIN + w, MARGIN + h), paint)
        }

        // 달린 길
        drawRoute(canvas, track, RectF(150f, 230f, WIDTH - 150f, 780f))

        val light = ResourcesCompat.getFont(context, R.font.pretendard_regular)
        val medium = ResourcesCompat.getFont(context, R.font.pretendard_medium)

        // 거리 — 가는 큰 숫자, 왼쪽 정렬
        paint.color = WHITE
        paint.typeface = light
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 210f
        val distance = "%.2f".format(km)
        canvas.drawText(distance, MARGIN.toFloat(), 990f, paint)
        val distanceWidth = paint.measureText(distance)
        paint.typeface = medium
        paint.textSize = 52f
        canvas.drawText("km", MARGIN + distanceWidth + 20f, 990f, paint)

        // 시간 · 페이스
        // 값의 아랫줄(1180)과 맨 아래 문구(1294) 사이를 띄운다
        stat(canvas, paint, labels.time, elapsed, MARGIN.toFloat(), 1100f, medium, light)
        stat(canvas, paint, labels.pace, pace, MARGIN + 360f, 1100f, medium, light)

        paint.typeface = medium
        paint.textSize = 30f
        paint.color = WHITE_DIM
        canvas.drawText(labels.footer, MARGIN.toFloat(), HEIGHT - 56f, paint)
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
        paint.color = WHITE_DIM
        paint.typeface = labelFace
        paint.textSize = 30f
        canvas.drawText(label, x, y, paint)
        paint.color = WHITE
        paint.typeface = valueFace
        paint.textSize = 72f
        canvas.drawText(value, x, y + 80f, paint)
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
