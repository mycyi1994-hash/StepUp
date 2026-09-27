package com.stepup.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 러닝 패스 카드 — 짙은 남색 면, 오른쪽에 경로 장식, 왼쪽에 [content](거리 · 횟수 같은 실제 글).
 *
 * 장식은 디자이너 전달본(running-path-card-background.svg, 342 × 168)의 선을 그대로 옮긴 것이다.
 * **실제 위치나 러닝 경로가 아니다** — 지도 · 위치 권한과 무관하고 화면 낭독에서도 빠진다(그림에 의미가 없다).
 * 움직이지 않는다. 카드가 좁거나 글이 길면 장식을 오른쪽으로 밀어 잘라 내고, 글 자리를 먼저 둔다.
 *
 * @param textEnd 카드 왼쪽에서 글이 끝나는 곳 — 장식의 경로가 이보다 오른쪽에서 시작한다
 */
@Composable
fun RunningPathCard(
    modifier: Modifier = Modifier,
    textEnd: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(23.dp)
    val art = remember { RunningPathArt() }
    Box(modifier.fillMaxWidth()) {
        // 카드 아래로 5dp 비치는 그림자 면(시안의 #03060C)
        Box(Modifier.matchParentSize().offset(y = 5.dp).clip(shape).background(RunningPathColors.shadow))
        Box(
            Modifier.fillMaxWidth().heightIn(min = 168.dp).clip(shape)
                .background(Brush.linearGradient(listOf(RunningPathColors.cardFrom, RunningPathColors.cardTo),
                    start = Offset(0f, Float.POSITIVE_INFINITY), end = Offset(Float.POSITIVE_INFINITY, 0f))),
        ) {
            Canvas(Modifier.matchParentSize()) { art.draw(this, textEnd.toPx()) }
            content()
        }
    }
}

/** 카드 안팎의 색 — 카드는 밝은 테마에서도 남색이라 글자색도 고정이다 */
object RunningPathColors {
    val cardFrom = Color(0xFF0D182A)
    val cardTo = Color(0xFF182E4C)
    val shadow = Color(0xFF03060C)
    val title = Color(0xFFB0C3E0)
    val number = Color(0xFFF2F4FC)
    val unit = Color(0xFFA6BCDD)
    val runs = Color(0xFFA4B8D5)
    val link = Color(0xFFA3BFFE)
}

/**
 * 시안 SVG 의 선들 — 좌표는 390 × 844 화면 기준(카드는 x24 · y193 에서 시작)이다. 그릴 때 카드 기준으로
 * 옮긴다. 1 단위 = 1dp.
 */
private class RunningPathArt {
    private val contours: List<Path> = listOf(
        "M205 183C174 226 270 227 248 268S294 313 358 306S394 344 389 382",
        "M215 183C186 226 280 227 262 268S304 313 367 306S402 344 389 382",
        "M225 183C198 226 290 227 276 268S314 313 376 306S410 344 389 382",
        "M235 183C210 226 300 227 290 268S324 313 385 306S418 344 389 382",
        "M245 183C222 226 310 227 304 268S334 313 394 306S426 344 389 382",
        "M255 183C234 226 320 227 318 268S344 313 403 306S434 344 389 382",
        "M265 183C246 226 330 227 332 268S354 313 412 306S442 344 389 382",
        "M220 366c-2-55 33-65 66-52s59 30 105-5M240 193c-13 22-9 39 22 42s58-25 109-9",
    ).map(::path)
    private val route: Path = path(ROUTE)
    private val topLine: Path = path("M49 193.5H199")

    private val routeBrush = Brush.linearGradient(
        0f to Color(0xFF5475A7), 0.55f to Color(0xFF8CB6FF), 1f to Color(0xFFD9E7FF),
        // 경로의 왼쪽 아래 → 오른쪽 위(시안의 objectBoundingBox 0,1 → 1,0)
        start = Offset(256f, 329f), end = Offset(333f, 232f),
    )

    fun draw(scope: DrawScope, textEndPx: Float) = with(scope) {
        val unit = density
        // 오른쪽 끝을 카드 오른쪽에 맞춘다. 글이 경로 쪽으로 길면 그만큼 더 오른쪽으로 민다(잘린다).
        val alignRight = size.width - CARD_WIDTH * unit
        val routeLeft = alignRight + (ROUTE_LEFT - CARD_X) * unit
        val clearance = 12.dp.toPx()
        val shift = (textEndPx + clearance - routeLeft).coerceAtLeast(0f)
        withTransform({
            translate(alignRight + shift, 0f)
            scale(unit, unit, pivot = Offset.Zero)
            translate(-CARD_X, -CARD_Y)
        }) {
            contours.forEach { drawPath(it, Color(0xFF4C6787), alpha = 0.25f, style = Stroke(width = 0.8f)) }
            drawPath(route, Color(0xFF497AC4), alpha = 0.10f, style = Stroke(width = 9f))
            drawPath(route, routeBrush, style = Stroke(width = 2.8f, cap = StrokeCap.Round))
            // 시작점(아래)과 끝점(위)
            drawCircle(Color(0xFF304563), radius = 3.5f, center = Offset(283f, 329f))
            drawCircle(Color(0xFF839DCA), radius = 3.5f, center = Offset(283f, 329f), style = Stroke(width = 1.7f))
            drawCircle(Color(0xFF4A7FF8), radius = 10f, center = Offset(318f, 232f), alpha = 0.12f)
            drawCircle(Color(0xFFE1ECFF), radius = 4.5f, center = Offset(318f, 232f))
            drawCircle(Color(0xFF477AFF), radius = 2.1f, center = Offset(318f, 232f))
        }
        // 위 모서리의 가는 빛 — 카드 왼쪽 기준이라 밀지 않는다
        withTransform({
            scale(unit, unit, pivot = Offset.Zero)
            translate(-CARD_X, -CARD_Y)
        }) {
            drawPath(topLine, Color(0xFF3B557C), alpha = 0.65f, style = Stroke(width = 0.8f))
        }
    }

    companion object {
        const val CARD_X = 24f
        const val CARD_Y = 193f
        const val CARD_WIDTH = 342f

        /** 경로(굵은 선)의 왼쪽 끝 — 글이 이보다 앞에서 끝나야 겹치지 않는다 */
        const val ROUTE_LEFT = 252f

        const val ROUTE = "M283 329C267 317 295 311 292 295S256 284 257 267S283 252 295 261 326 273 333 257 329 237 318 232"

        fun path(data: String): Path = PathParser().parsePathString(data).toPath()
    }
}
