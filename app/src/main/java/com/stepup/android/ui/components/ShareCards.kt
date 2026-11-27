package com.stepup.android.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.stepup.android.R
import com.stepup.android.domain.GeoPoint
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/**
 * 13차 공유(자랑하기) 카드 — 세로 9:16(1080 × 1920) 그림 한 장(2026-11-26 13차 S-403 ~ S-420).
 *
 * 인스타 스토리 배경으로 넘겨도 잘리지 않는 크기. 모든 글은 부르는 쪽이 현지화해 넘긴다(그림 안에서 글을 지어내지 않는다).
 * SUP 는 적지 않는다(서버가 확인하기 전 적립을 밖으로 내보내지 않는다 — RunShareCard 와 같은 원칙).
 * 지도 카드의 바탕은 실제 지도 타일이 아니라 남색 바탕 + 옅은 격자다(공유 그림을 만들 때 지도 타일을 다시 받지 않는다 — 13차 문서 "시안과 다른 점").
 */
object ShareCards {
    const val W = 1080
    const val H = 1920
    private const val M = 84f

    private const val NAVY_TOP = 0xFF16244A.toInt()
    private const val NAVY_BOTTOM = 0xFF0A1530.toInt()
    private const val BLUE_TOP = 0xFF3C6CF2.toInt()
    private const val BLUE_BOTTOM = 0xFF2148C9.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val SOFT = 0xFFC9D5F2.toInt()
    private const val ROUTE = 0xFF5B8CFF.toInt()

    /** 줄 하나 — 작은 이름표 위 큰 값 */
    data class Stat(val label: String, val value: String)

    // ── 러닝 ───────────────────────────────────────────────────

    /** 지도 카드(S-403) — 위에 달린 길, 아래 큰 거리 · 시간 · 페이스 · 날짜(· 동네) */
    fun runMap(context: Context, track: List<GeoPoint>, km: String, time: String, pace: String, footer: String): Bitmap =
        frame(context, NAVY_TOP, NAVY_BOTTOM) { c, p ->
            // 옅은 격자 — 지도 자리
            p.color = 0x14FFFFFF
            p.strokeWidth = 2f
            val top = 230f
            val bottom = 1230f
            var x = 0f
            while (x <= W) { c.drawLine(x, top, x, bottom, p); x += 120f }
            var y = top
            while (y <= bottom) { c.drawLine(0f, y, W.toFloat(), y, p); y += 120f }
            drawRoute(c, track, RectF(150f, top + 80f, W - 150f, bottom - 80f))
            big(context, c, p, km, "km", 1500f)
            p.typeface = font(context, R.font.paperlogy_6semibold)
            p.color = WHITE
            p.textSize = 52f
            c.drawText("$time    $pace", M, 1600f, p)
            p.typeface = font(context, R.font.paperlogy_5medium)
            p.color = SOFT
            p.textSize = 44f
            c.drawText(footer, M, 1690f, p)
        }

    /** 기록 카드(S-404) — 파란 면 · 제목 · 큰 거리 · 두 줄 두 칸(시간 · 페이스 / 걸음 · 신발) */
    fun runRecord(context: Context, title: String, km: String, stats: List<Stat>): Bitmap =
        frame(context, BLUE_TOP, BLUE_BOTTOM) { c, p ->
            p.typeface = font(context, R.font.paperlogy_6semibold)
            p.color = WHITE
            p.textSize = 54f
            c.drawText(title, M, 560f, p)
            big(context, c, p, km, null, 900f, size = 300f)
            p.typeface = font(context, R.font.paperlogy_6semibold)
            p.textSize = 64f
            c.drawText("km", M, 1010f, p)
            grid(context, c, p, stats, 1230f)
        }

    /** 신발 카드(S-405) — 신은 신발 카드 + "○○와 3.21km" · 시간 · 페이스 */
    fun runShoe(context: Context, shoe: ShoeFace, line: String, km: String, time: String, pace: String): Bitmap =
        frame(context, NAVY_TOP, NAVY_BOTTOM) { c, p ->
            shoeCard(context, c, p, shoe, RectF(200f, 260f, W - 200f, 1160f))
            p.typeface = font(context, R.font.paperlogy_6semibold)
            p.color = SOFT
            p.textSize = 54f
            c.drawText(line, M, 1340f, p)
            big(context, c, p, km, "km", 1560f, size = 220f)
            p.typeface = font(context, R.font.paperlogy_6semibold)
            p.color = WHITE
            p.textSize = 52f
            c.drawText("$time    $pace", M, 1660f, p)
        }

    // ── 순위 · 신발 · 뽑기 ─────────────────────────────────────

    /** 순위 카드(S-418) — 제목 · 큰 순위 · 바뀐 칸 · 크루 이름 · 두 칸 */
    fun ranking(context: Context, title: String, rank: String, change: String?, name: String, stats: List<Stat>): Bitmap =
        frame(context, BLUE_TOP, BLUE_BOTTOM) { c, p ->
            p.typeface = font(context, R.font.paperlogy_6semibold)
            p.color = WHITE
            p.textSize = 56f
            c.drawText(title, M, 560f, p)
            big(context, c, p, rank, null, 900f, size = 320f)
            if (change != null) {
                p.typeface = font(context, R.font.paperlogy_6semibold)
                p.color = SOFT
                p.textSize = 52f
                c.drawText(change, M, 1000f, p)
            }
            p.typeface = font(context, R.font.paperlogy_7bold)
            p.color = WHITE
            p.textSize = 72f
            c.drawText(ellipsize(p, name, W - 2 * M), M, 1180f, p)
            grid(context, c, p, stats, 1330f)
        }

    /** 새 신발(S-407) — 큰 신발 카드 + 아래 한 줄 */
    fun newShoe(context: Context, shoe: ShoeFace, line: String): Bitmap =
        frame(context, NAVY_TOP, NAVY_BOTTOM) { c, p ->
            shoeCard(context, c, p, shoe, RectF(170f, 300f, W - 170f, 1330f))
            p.typeface = font(context, R.font.paperlogy_7bold)
            p.color = WHITE
            p.textSize = 64f
            p.textAlign = Paint.Align.CENTER
            c.drawText(ellipsize(p, line, W - 2 * M), W / 2f, 1520f, p)
            p.textAlign = Paint.Align.LEFT
        }

    /** 10회 결과(S-409) — 가장 높은 등급 한 장 크게 · 아래 등급별 개수 한 줄 */
    fun drawTen(context: Context, best: ShoeFace, summary: String, title: String): Bitmap =
        frame(context, NAVY_TOP, NAVY_BOTTOM) { c, p ->
            p.typeface = font(context, R.font.paperlogy_7bold)
            p.color = WHITE
            p.textSize = 64f
            c.drawText(title, M, 360f, p)
            shoeCard(context, c, p, best, RectF(220f, 440f, W - 220f, 1380f))
            p.typeface = font(context, R.font.paperlogy_6semibold)
            p.color = SOFT
            p.textSize = 56f
            p.textAlign = Paint.Align.CENTER
            c.drawText(ellipsize(p, summary, W - 2 * M), W / 2f, 1560f, p)
            p.textAlign = Paint.Align.LEFT
        }

    // ── 초대(크루 · 번개 · 베타 코드) ───────────────────────────

    /** 초대 카드(S-410 · S-419 · S-420) — 제목 · 큰 이름 · 줄 몇 개 · 아래 코드나 주소 */
    fun invite(context: Context, title: String, name: String, lines: List<String>, code: String?, photo: Bitmap? = null): Bitmap =
        frame(context, NAVY_TOP, NAVY_BOTTOM) { c, p ->
            var y = 520f
            if (photo != null) {
                val box = RectF(M, 300f, W - M, 900f)
                c.save()
                val clip = Path().apply { addRoundRect(box, 48f, 48f, Path.Direction.CW) }
                c.clipPath(clip)
                val scale = max(box.width() / photo.width, box.height() / photo.height)
                val w = photo.width * scale
                val h = photo.height * scale
                c.drawBitmap(photo, null, RectF(box.centerX() - w / 2, box.centerY() - h / 2, box.centerX() + w / 2, box.centerY() + h / 2), p)
                c.restore()
                y = 1040f
            }
            p.typeface = font(context, R.font.paperlogy_6semibold)
            p.color = SOFT
            p.textSize = 52f
            c.drawText(title, M, y, p)
            p.typeface = font(context, R.font.paperlogy_7bold)
            p.color = WHITE
            p.textSize = 96f
            c.drawText(ellipsize(p, name, W - 2 * M), M, y + 130f, p)
            p.typeface = font(context, R.font.paperlogy_5medium)
            p.color = SOFT
            p.textSize = 48f
            lines.forEachIndexed { i, line -> c.drawText(ellipsize(p, line, W - 2 * M), M, y + 230f + i * 72f, p) }
            if (code != null) {
                val box = RectF(M, 1560f, W - M, 1740f)
                p.color = 0x22FFFFFF
                c.drawRoundRect(box, 40f, 40f, p)
                p.typeface = font(context, R.font.paperlogy_7bold)
                p.color = WHITE
                p.textSize = 64f
                p.textAlign = Paint.Align.CENTER
                c.drawText(ellipsize(p, code, box.width() - 80f), box.centerX(), box.centerY() + 22f, p)
                p.textAlign = Paint.Align.LEFT
            }
        }

    /** 공유 카드 안의 신발 한 켤레 — 그림 · 등급 · 번호 · 이름 · 등급 색 */
    data class ShoeFace(val art: Bitmap?, val tier: String, val number: String, val name: String, val rim: Int)

    /** 그림 자원을 공유 카드용으로 읽는다(없으면 null — 카드는 그림 없이) */
    fun shoeArt(context: Context, res: Int?): Bitmap? = res?.let {
        runCatching {
            BitmapFactory.decodeResource(context.resources, it, BitmapFactory.Options().apply { inSampleSize = 1 })
        }.getOrNull()
    }

    // ── 그리기 ─────────────────────────────────────────────────

    private fun frame(context: Context, top: Int, bottom: Int, draw: (Canvas, Paint) -> Unit): Bitmap {
        val bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bitmap)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, 0f, H.toFloat(), top, bottom, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), p)
        p.shader = RadialGradient(W * 0.85f, H * 0.1f, W * 0.9f, 0x334B83FE, 0x004B83FE, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), p)
        p.shader = null
        BitmapFactory.decodeResource(context.resources, R.drawable.logo_wordmark_on_dark)?.let { logo ->
            val w = 300
            val h = w * logo.height / logo.width
            c.drawBitmap(logo, null, Rect(M.toInt(), 110, M.toInt() + w, 110 + h), p)
        }
        draw(c, p)
        return bitmap
    }

    private fun font(context: Context, res: Int): Typeface? = runCatching { ResourcesCompat.getFont(context, res) }.getOrNull()

    /** 큰 수(+ 작은 단위) — 왼쪽 맞춤 */
    private fun big(context: Context, c: Canvas, p: Paint, value: String, unit: String?, baseline: Float, size: Float = 260f) {
        p.typeface = font(context, R.font.paperlogy_8extrabold) ?: font(context, R.font.paperlogy_7bold)
        p.color = WHITE
        p.textSize = size
        c.drawText(value, M, baseline, p)
        if (unit != null) {
            val x = M + p.measureText(value) + 18f
            p.typeface = font(context, R.font.paperlogy_6semibold)
            p.textSize = size * 0.3f
            c.drawText(unit, x, baseline, p)
        }
    }

    /** 두 칸씩 줄 — 이름표 · 값 */
    private fun grid(context: Context, c: Canvas, p: Paint, stats: List<Stat>, top: Float) {
        val colW = (W - 2 * M) / 2f
        stats.forEachIndexed { i, s ->
            val x = M + (i % 2) * colW
            val y = top + (i / 2) * 210f
            p.typeface = font(context, R.font.paperlogy_5medium)
            p.color = SOFT
            p.textSize = 42f
            c.drawText(s.label, x, y, p)
            p.typeface = font(context, R.font.paperlogy_7bold)
            p.color = WHITE
            p.textSize = 76f
            c.drawText(ellipsize(p, s.value, colW - 30f), x, y + 92f, p)
        }
    }

    /** 신발 카드 — 등급 색 테두리 · 왼쪽 위 등급 칩 · 오른쪽 위 번호 · 가운데 그림 · 아래 이름 */
    private fun shoeCard(context: Context, c: Canvas, p: Paint, shoe: ShoeFace, box: RectF) {
        p.shader = LinearGradient(0f, box.top, 0f, box.bottom, 0xFF13285A.toInt(), 0xFF0A1633.toInt(), Shader.TileMode.CLAMP)
        c.drawRoundRect(box, 56f, 56f, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = 10f
        p.color = shoe.rim
        c.drawRoundRect(box, 56f, 56f, p)
        p.style = Paint.Style.FILL
        // 등급 칩
        p.typeface = font(context, R.font.paperlogy_7bold)
        p.textSize = 40f
        val chipW = p.measureText(shoe.tier) + 36f
        val chip = RectF(box.left + 40f, box.top + 40f, box.left + 40f + chipW, box.top + 100f)
        p.color = 0xD9FFFFFF.toInt()
        c.drawRoundRect(chip, 14f, 14f, p)
        p.color = 0xFF1B2433.toInt()
        c.drawText(shoe.tier, chip.left + 18f, chip.bottom - 16f, p)
        p.color = 0xC8FFFFFF.toInt()
        p.typeface = font(context, R.font.paperlogy_6semibold)
        p.textAlign = Paint.Align.RIGHT
        c.drawText(shoe.number, box.right - 40f, box.top + 86f, p)
        p.textAlign = Paint.Align.LEFT
        shoe.art?.let { art ->
            val area = RectF(box.left + 40f, box.top + 140f, box.right - 40f, box.bottom - 200f)
            val scale = min(area.width() / art.width, area.height() / art.height)
            val w = art.width * scale
            val h = art.height * scale
            c.drawBitmap(art, null, RectF(area.centerX() - w / 2, area.centerY() - h / 2, area.centerX() + w / 2, area.centerY() + h / 2), p)
        }
        p.typeface = font(context, R.font.paperlogy_7bold)
        p.color = WHITE
        p.textSize = 64f
        c.drawText(ellipsize(p, shoe.name, box.width() - 80f), box.left + 44f, box.bottom - 70f, p)
    }

    private fun ellipsize(p: Paint, text: String, width: Float): String {
        if (p.measureText(text) <= width) return text
        var end = text.length
        while (end > 1 && p.measureText(text.substring(0, end) + "…") > width) end--
        return text.substring(0, end) + "…"
    }

    private fun drawRoute(c: Canvas, track: List<GeoPoint>, box: RectF) {
        if (track.size < 2) return
        val lat0 = track.sumOf { it.lat } / track.size
        val k = cos(Math.toRadians(lat0))
        val xs = track.map { it.lng * k }
        val ys = track.map { it.lat }
        val minX = xs.min(); val maxX = xs.max(); val minY = ys.min(); val maxY = ys.max()
        val spanX = max(maxX - minX, 1e-9)
        val spanY = max(maxY - minY, 1e-9)
        val scale = min(box.width() / spanX, box.height() / spanY)
        val offX = box.left + (box.width() - spanX * scale) / 2
        val offY = box.top + (box.height() - spanY * scale) / 2
        val path = Path()
        track.indices.forEach { i ->
            val x = (offX + (xs[i] - minX) * scale).toFloat()
            val y = (offY + (maxY - ys[i]) * scale).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        p.color = 0x554B83FE
        p.strokeWidth = 34f
        c.drawPath(path, p)
        p.color = ROUTE
        p.strokeWidth = 16f
        c.drawPath(path, p)
    }
}
