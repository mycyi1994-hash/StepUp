package com.stepup.android.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import com.stepup.android.R
import com.stepup.android.domain.Outfit
import com.stepup.android.domain.Outfits

@StringRes
fun outfitNameRes(outfit: Outfit): Int = when (outfit.id) {
    Outfits.CORE_ZIP.id -> R.string.outfit_clo_001
    Outfits.EMBER_SHELL.id -> R.string.outfit_clo_002
    Outfits.TIDE_ANORAK.id -> R.string.outfit_clo_003
    Outfits.VOLT_JERSEY.id -> R.string.outfit_clo_004
    Outfits.AERO_WINDBREAKER.id -> R.string.outfit_clo_005
    else -> R.string.outfit_starter_hoodie
}

/**
 * 의상 상품 그림 — 의상 시트의 "ITEM" 줄에서 떼어 낸 앞면 그림이 있으면 그것을,
 * 없으면(기본 의상) 옷 아이콘을 그린다.
 */
@Composable
fun OutfitArt(outfit: Outfit, modifier: Modifier = Modifier) {
    val res = outfitProductRes(outfit.id)
    if (res != null) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(res),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            modifier = modifier,
        )
    } else {
        GarmentArt(outfit, modifier)
    }
}

/**
 * 의상 아이콘 — 옷만, 그 옷의 색으로.
 *
 * 상품 카드의 그림이다. 캐릭터가 입은 모습이 아니다 — 입은 모습은 완성
 * 그림이 있는 착장만 캐릭터 무대에서 보여 준다([CharacterStage]). 의상마다
 * 입체 상품 그림이 생기면 이 아이콘 자리에 그 그림을 넣는다.
 *
 * 긴소매 의상은 후드(모자 · 지퍼 · 끈), 반소매는 둥근 목 티셔츠로 그린다.
 */
@Composable
fun GarmentArt(outfit: Outfit, modifier: Modifier = Modifier) {
    val top = Color(outfit.top)
    val shade = Color(outfit.topShade)
    val trim = Color(outfit.trim)
    val hooded = outfit.longSleeve
    Canvas(modifier) {
        val s = size.minDimension / 100f
        val ox = (size.width - 100f * s) / 2f
        val oy = (size.height - 100f * s) / 2f
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        translate(ox, oy) {
            if (hooded) {
                // 등 뒤로 넘긴 모자
                val hood = Path().apply {
                    moveTo(33f * s, 27f * s)
                    quadraticBezierTo(34f * s, 8f * s, 50f * s, 8f * s)
                    quadraticBezierTo(66f * s, 8f * s, 67f * s, 27f * s)
                    close()
                }
                drawPath(hood, shade)
                drawPath(hood, trim.copy(alpha = 0.55f), style = Stroke(width = 1.2f * s))
            }
            val body = Path().apply {
                moveTo(38f * s, 22f * s)
                lineTo(30f * s, 26f * s)
                if (hooded) {
                    lineTo(18f * s, 40f * s)
                    lineTo(12f * s, 74f * s)
                    lineTo(22f * s, 76f * s)
                    lineTo(26f * s, 49f * s)
                } else {
                    lineTo(15f * s, 38f * s)
                    lineTo(21f * s, 51f * s)
                    lineTo(28f * s, 46f * s)
                }
                lineTo(28f * s, 86f * s)
                quadraticBezierTo(50f * s, 90f * s, 72f * s, 86f * s)
                if (hooded) {
                    lineTo(74f * s, 49f * s)
                    lineTo(78f * s, 76f * s)
                    lineTo(88f * s, 74f * s)
                    lineTo(82f * s, 40f * s)
                } else {
                    lineTo(72f * s, 46f * s)
                    lineTo(79f * s, 51f * s)
                    lineTo(85f * s, 38f * s)
                }
                lineTo(70f * s, 26f * s)
                lineTo(62f * s, 22f * s)
                quadraticBezierTo(50f * s, 30f * s, 38f * s, 22f * s)
                close()
            }
            drawPath(
                body,
                Brush.verticalGradient(0f to top, 1f to shade, startY = 20f * s, endY = 90f * s),
            )
            // 가장자리 파란 조명 — 캐릭터 그림과 같은 결
            drawPath(body, trim.copy(alpha = 0.45f), style = Stroke(width = 1.2f * s))

            val line = Stroke(width = 1.6f * s, cap = StrokeCap.Round)
            // 목둘레
            val neck = Path().apply {
                moveTo(38f * s, 22f * s)
                quadraticBezierTo(50f * s, 30f * s, 62f * s, 22f * s)
            }
            drawPath(neck, trim, style = Stroke(width = 2.2f * s, cap = StrokeCap.Round))
            // 밑단
            val hem = Path().apply {
                moveTo(28.5f * s, 83.5f * s)
                quadraticBezierTo(50f * s, 87.5f * s, 71.5f * s, 83.5f * s)
            }
            drawPath(hem, trim.copy(alpha = 0.9f), style = Stroke(width = 2.6f * s, cap = StrokeCap.Round))
            if (hooded) {
                // 지퍼 · 끈
                drawLine(trim.copy(alpha = 0.8f), p(50f, 27f), p(50f, 84f), strokeWidth = 1.2f * s)
                drawLine(trim, p(46f, 27f), p(45f, 39f), strokeWidth = 1.4f * s, cap = StrokeCap.Round)
                drawLine(trim, p(54f, 27f), p(55f, 39f), strokeWidth = 1.4f * s, cap = StrokeCap.Round)
                // 소매 줄 · 소맷부리
                drawLine(trim.copy(alpha = 0.85f), p(24f, 33f), p(16.5f, 69f), strokeWidth = line.width, cap = StrokeCap.Round)
                drawLine(trim.copy(alpha = 0.85f), p(76f, 33f), p(83.5f, 69f), strokeWidth = line.width, cap = StrokeCap.Round)
                drawLine(trim, p(12.8f, 71.5f), p(21.8f, 73.5f), strokeWidth = 3f * s, cap = StrokeCap.Round)
                drawLine(trim, p(78.2f, 73.5f), p(87.2f, 71.5f), strokeWidth = 3f * s, cap = StrokeCap.Round)
            } else {
                // 반소매 끝단 · 가슴의 작은 화살표
                drawLine(trim, p(16.5f, 41f), p(21.5f, 49f), strokeWidth = 2.4f * s, cap = StrokeCap.Round)
                drawLine(trim, p(83.5f, 41f), p(78.5f, 49f), strokeWidth = 2.4f * s, cap = StrokeCap.Round)
            }
            // 가슴의 위쪽 화살표 — UP 표시 대신
            val arrow = Path().apply {
                moveTo(58f * s, 44f * s)
                lineTo(62f * s, 39f * s)
                lineTo(66f * s, 44f * s)
            }
            drawPath(arrow, trim, style = Stroke(width = 1.8f * s, cap = StrokeCap.Round))
        }
    }
}
