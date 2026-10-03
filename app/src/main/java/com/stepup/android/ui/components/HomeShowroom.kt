package com.stepup.android.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.stepup.android.domain.Sneaker

/*
 * 러닝 홈(시안 HOME.png) 전시장 — 창 너머 도시 야경 · 천장 조명 · 빛나는 받침 위 신고 있는 신발.
 *
 * 전달본에는 따로 뗀 조명 · 받침 그림이 없어서(asset-readiness.json) 모두 Compose 로 그린다. 창 너머 풍경은 기존 홈 바탕
 * 사진(HomePhotos — 넘기기 · 날씨 풍경 그대로)을 밤의 남색으로 눌러 쓴다. 신발은 앱에 든 실제 신발 그림이다.
 */

/** 사진을 밤 전시장 창 너머의 남색으로 — 밝기는 살리고 색을 파랑 쪽으로 모은다 */
private val ShowroomGrade = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            0.20f, 0.28f, 0.06f, 0f, -6f,
            0.18f, 0.40f, 0.10f, 0f, 4f,
            0.22f, 0.46f, 0.42f, 0f, 34f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
)

/**
 * 러닝 홈 화면 전체 바탕 — 남색 바닥 위 위쪽 절반에 창 너머 풍경(창틀 · 위 막대), 아래는 남색으로 녹는다.
 * 머리(로고 · SUP)와 전시장 위쪽이 이 위에 얹힌다. 밝은 테마는 밝은 막을 한 겹 더 덮는다.
 */
@Composable
fun HomeShowroomBackdrop(photo: HomePhoto, modifier: Modifier = Modifier) {
    val t = runTone()
    BoxWithConstraints(modifier.background(t.screen)) {
        val windowHeight = maxHeight * 0.52f
        Image(
            painter = painterResource(photo.res),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = if (t.dark) ShowroomGrade else null,
            modifier = Modifier.fillMaxWidth().height(windowHeight),
        )
        Canvas(Modifier.fillMaxSize()) {
            val wh = windowHeight.toPx()
            // 위는 머리 글자가 읽히게 어둡게, 창 아래쪽은 바닥 남색으로
            drawRect(
                Brush.verticalGradient(
                    0f to t.screen.copy(alpha = if (t.dark) 0.86f else 0.9f),
                    0.24f to t.screen.copy(alpha = if (t.dark) 0.30f else 0.55f),
                    0.70f to t.screen.copy(alpha = if (t.dark) 0.18f else 0.5f),
                    1f to t.screen,
                    startY = 0f, endY = wh,
                ),
                size = Size(size.width, wh),
            )
            drawWindowFrames(wh, t)
            // 양옆을 눌러 가운데(신발)로 눈이 가게
            drawRect(
                Brush.horizontalGradient(
                    0f to t.screen.copy(alpha = 0.55f), 0.18f to Color.Transparent,
                    0.82f to Color.Transparent, 1f to t.screen.copy(alpha = 0.55f),
                ),
                size = Size(size.width, wh),
            )
        }
    }
}

/** 창틀 — 위 막대와 세로 기둥 넷, 기둥 안쪽 면에 푸른 반사광 */
private fun DrawScope.drawWindowFrames(windowHeight: Float, t: RunTone) {
    val frame = if (t.dark) Color(0xFF020B19) else Color(0xFFD7E1F2)
    val shine = if (t.dark) Color(0xFF1C4C9C) else Color(0xFFB9CCEE)
    val w = size.width
    val beam = windowHeight * 0.13f
    drawRect(frame.copy(alpha = 0.92f), size = Size(w, beam))
    listOf(0.07f to 0.035f, 0.205f to 0.022f, 0.795f to 0.022f, 0.93f to 0.035f).forEach { (x, width) ->
        val left = w * x - w * width / 2f
        val pillar = Size(w * width, windowHeight * 0.92f)
        drawRect(frame.copy(alpha = 0.9f), topLeft = Offset(left, 0f), size = pillar)
        drawRect(
            Brush.horizontalGradient(listOf(shine.copy(alpha = 0.0f), shine.copy(alpha = 0.55f), shine.copy(alpha = 0f)),
                startX = left, endX = left + pillar.width),
            topLeft = Offset(left, beam), size = Size(pillar.width, pillar.height - beam),
        )
    }
}

/**
 * 전시장 무대 — 위 천장 조명 막대와 내려오는 빛, 아래 반들한 바닥과 빛나는 받침, 받침 위 신발.
 * [shoe] 가 없으면 빈 받침만(이름 · 안내는 부르는 쪽이 적는다). 신발 그림을 못 읽으면 그림 없음 표시.
 */
@Composable
fun ShowroomStage(shoe: Sneaker?, modifier: Modifier = Modifier) {
    val t = runTone()
    BoxWithConstraints(modifier) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val pedestalWidth = w * 0.92f
        val pedestalTopH = (pedestalWidth * 0.13f).coerceAtMost(h * 0.18f)
        val pedestalDepth = pedestalTopH * 0.30f
        val pedestalTop = h - pedestalTopH - pedestalDepth - h * 0.03f
        Canvas(Modifier.fillMaxSize().testTag("home-stage")) {
            drawFloor(pedestalTop, t)
            drawCeilingLight(t)
            drawPedestal(Offset(size.width / 2f, pedestalTop), pedestalWidth, pedestalTopH, pedestalDepth, t)
        }
        if (shoe != null) {
            val art by rememberShoeArt(shoe, ShoeArtFull)
            val shoeMaxW = w * 0.80f
            val shoeMaxH = (pedestalTop + pedestalTopH * 0.52f) - h * 0.13f
            when (val load = art) {
                is ShoeArtLoad.Ready -> Canvas(Modifier.fillMaxSize().testTag("home-shoe-image")) {
                    val visible = load.art.visible
                    val scale = minOf(shoeMaxW / visible.width, shoeMaxH / visible.height)
                    val dw = visible.width * scale
                    val dh = visible.height * scale
                    val left = (size.width - dw) / 2f
                    // 신발 바닥이 받침 윗면 가운데쯤에 닿게
                    val bottom = pedestalTop + pedestalTopH * 0.52f
                    val top = bottom - dh
                    // 받침 위 접촉 그림자
                    drawOval(
                        Brush.radialGradient(
                            listOf(Color.Black.copy(alpha = if (t.dark) 0.55f else 0.22f), Color.Transparent),
                            center = Offset(size.width / 2f, bottom - dh * 0.02f), radius = dw * 0.5f,
                        ),
                        topLeft = Offset(left + dw * 0.04f, bottom - dh * 0.10f), size = Size(dw * 0.92f, dh * 0.16f),
                    )
                    drawImage(
                        load.art.image,
                        srcOffset = IntOffset(visible.left, visible.top),
                        srcSize = IntSize(visible.width, visible.height),
                        dstOffset = IntOffset(left.toInt(), top.toInt()),
                        dstSize = IntSize(dw.toInt(), dh.toInt()),
                    )
                }
                ShoeArtLoad.Failed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.ImageNotSupported, contentDescription = null, tint = t.muted,
                        modifier = Modifier.testTag("home-shoe-art-missing"))
                }
                ShoeArtLoad.Loading -> Unit
            }
        }
    }
}

/** 천장 조명 — 어두운 틀 안의 밝은 막대, 아래로 번지는 빛과 내려오는 빛기둥 */
private fun DrawScope.drawCeilingLight(t: RunTone) {
    val w = size.width
    val barLeft = w * 0.06f
    val barRight = w * 0.94f
    val barTop = size.height * 0.035f
    val housing = 14.dp.toPx()
    // 내려오는 빛기둥
    val cone = Path().apply {
        moveTo(barLeft + w * 0.04f, barTop + housing)
        lineTo(barRight - w * 0.04f, barTop + housing)
        lineTo(w * 0.98f, size.height * 0.86f)
        lineTo(w * 0.02f, size.height * 0.86f)
        close()
    }
    drawPath(
        cone,
        Brush.verticalGradient(
            listOf(Color(0xFF9CC8FF).copy(alpha = if (t.dark) 0.20f else 0.18f), Color.Transparent),
            startY = barTop, endY = size.height * 0.86f,
        ),
    )
    // 틀
    drawRoundRect(
        if (t.dark) Color(0xFF0A1930) else Color(0xFF26334A), topLeft = Offset(barLeft, barTop),
        size = Size(barRight - barLeft, housing), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
    )
    // 빛 번짐
    glowLine(Offset(barLeft + 8.dp.toPx(), barTop + housing * 0.78f), Offset(barRight - 8.dp.toPx(), barTop + housing * 0.78f),
        Color(0xFF8EC5FF), 14.dp.toPx(), 18.dp.toPx(), 0.9f)
    drawLine(
        Color(0xFFF2FAFF), Offset(barLeft + 6.dp.toPx(), barTop + housing * 0.8f), Offset(barRight - 6.dp.toPx(), barTop + housing * 0.8f),
        strokeWidth = 3.5.dp.toPx(),
    )
}

/** 반들한 바닥 — 받침보다 조금 위에서 시작해 아래로 어두워진다. 받침 아래 파란 반사 */
private fun DrawScope.drawFloor(pedestalTop: Float, t: RunTone) {
    val floorTop = pedestalTop - size.height * 0.10f
    drawRect(
        Brush.verticalGradient(
            listOf(
                (if (t.dark) Color(0xFF0B2850) else Color(0xFFDCE7FA)).copy(alpha = 0.92f),
                if (t.dark) Color(0xFF041631) else Color(0xFFEFF4FD),
            ),
            startY = floorTop, endY = size.height,
        ),
        topLeft = Offset(0f, floorTop), size = Size(size.width, size.height - floorTop),
    )
    // 바닥과 창이 만나는 선의 푸른 빛
    glowLine(Offset(0f, floorTop), Offset(size.width, floorTop), t.cobalt, 2.dp.toPx(), 10.dp.toPx(), 0.55f)
    drawOval(
        Brush.radialGradient(
            listOf(t.cobalt.copy(alpha = if (t.dark) 0.45f else 0.2f), Color.Transparent),
            center = Offset(size.width / 2f, size.height * 0.98f), radius = size.width * 0.6f,
        ),
        topLeft = Offset(-size.width * 0.1f, pedestalTop), size = Size(size.width * 1.2f, size.height - pedestalTop),
    )
}

/** 빛나는 받침 — 길쭉한 육각 윗면(반들한 남색), 앞면, 둘레의 푸른 빛줄과 앞 모서리 등 */
private fun DrawScope.drawPedestal(center: Offset, width: Float, topH: Float, depth: Float, t: RunTone) {
    val left = center.x - width / 2f
    val right = center.x + width / 2f
    val cut = width * 0.075f
    val top = center.y
    val mid = top + topH / 2f
    val bottom = top + topH
    val face = Path().apply {
        moveTo(left + cut, top); lineTo(right - cut, top); lineTo(right, mid)
        lineTo(right - cut, bottom); lineTo(left + cut, bottom); lineTo(left, mid); close()
    }
    val front = Path().apply {
        moveTo(left, mid); lineTo(left + cut, bottom); lineTo(right - cut, bottom); lineTo(right, mid)
        lineTo(right, mid + depth); lineTo(right - cut, bottom + depth); lineTo(left + cut, bottom + depth)
        lineTo(left, mid + depth); close()
    }
    // 받침 아래 바닥으로 퍼지는 빛
    drawOval(
        Brush.radialGradient(
            listOf(t.cobalt.copy(alpha = if (t.dark) 0.6f else 0.25f), Color.Transparent),
            center = Offset(center.x, bottom + depth), radius = width * 0.6f,
        ),
        topLeft = Offset(left - width * 0.08f, bottom - topH * 0.1f), size = Size(width * 1.16f, topH * 2.2f),
    )
    // 앞면 — 어두운 남색, 위 모서리에 파란 빛줄
    drawPath(
        front,
        Brush.verticalGradient(
            listOf(if (t.dark) Color(0xFF0A2C5E) else Color(0xFFB7CBEF), if (t.dark) Color(0xFF041431) else Color(0xFF9DB6E4)),
            startY = mid, endY = bottom + depth,
        ),
    )
    // 윗면 — 뒤가 밝고 앞이 깊은 반들한 남색, 앞쪽에 빛 띠
    drawPath(
        face,
        Brush.verticalGradient(
            listOf(
                if (t.dark) Color(0xFF223F6E) else Color(0xFFFFFFFF),
                if (t.dark) Color(0xFF10284D) else Color(0xFFE9F0FC),
                if (t.dark) Color(0xFF0B1F3E) else Color(0xFFDCE6F8),
            ),
            startY = top, endY = bottom,
        ),
    )
    drawPath(
        face,
        Brush.radialGradient(
            listOf(Color(0xFF9CCBFF).copy(alpha = if (t.dark) 0.22f else 0.3f), Color.Transparent),
            center = Offset(center.x, top + topH * 0.35f), radius = width * 0.45f,
        ),
    )
    // 윗면 둘레 — 번지는 빛 + 밝은 선
    drawIntoCanvas { canvas ->
        val glow = Paint().apply {
            color = Color(0xFF3FA9FF)
            style = androidx.compose.ui.graphics.PaintingStyle.Stroke
            strokeWidth = 5.dp.toPx()
            alpha = 0.75f
        }
        glow.asFrameworkPaint().maskFilter = BlurMaskFilter(7.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
        canvas.drawPath(face, glow)
    }
    drawPath(face, Color(0xFF9AD4FF), style = Stroke(width = 1.8.dp.toPx()))
    // 앞면 위 · 아래 빛줄
    glowLine(Offset(left + cut, bottom), Offset(right - cut, bottom), Color(0xFFBFE3FF), 1.6.dp.toPx(), 6.dp.toPx(), 0.95f)
    glowLine(Offset(left + cut, bottom + depth), Offset(right - cut, bottom + depth), Color(0xFF2F8BFF), 3.dp.toPx(), 12.dp.toPx(), 0.95f)
    // 윗면 모서리 작은 등
    listOf(
        Offset(left + cut + width * 0.02f, top + topH * 0.28f), Offset(right - cut - width * 0.02f, top + topH * 0.28f),
        Offset(left + cut * 0.9f + width * 0.03f, bottom - topH * 0.22f), Offset(right - cut * 0.9f - width * 0.03f, bottom - topH * 0.22f),
    ).forEach { at ->
        drawCircle(Color(0xFF8CC8FF).copy(alpha = 0.45f), radius = 5.dp.toPx(), center = at)
        drawCircle(Color(0xFFEAF6FF), radius = 1.8.dp.toPx(), center = at)
    }
}

/** 번지는 빛줄 — 흐린 굵은 선 위에 가는 선 */
private fun DrawScope.glowLine(start: Offset, end: Offset, color: Color, width: Float, blur: Float, alpha: Float) {
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            this.color = color
            style = androidx.compose.ui.graphics.PaintingStyle.Stroke
            strokeWidth = width * 2.2f
            this.alpha = alpha * 0.7f
        }
        paint.asFrameworkPaint().maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
        canvas.drawLine(start, end, paint)
    }
    drawLine(color.copy(alpha = alpha), start, end, strokeWidth = width)
}
