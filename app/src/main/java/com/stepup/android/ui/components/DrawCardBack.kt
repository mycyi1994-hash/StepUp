package com.stepup.android.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.stepup.android.R

/** 뽑기 카드 뒷면(13차 S-408) — 남색 사선 무늬 · 가운데 STEPUP. 상급은 금색 테, 무료는 파란 테 */
@Composable
fun DrawCardBack(premium: Boolean, big: Boolean) {
    val rim = if (premium) Color(0xFFE7B53C) else Color(0xFF5B8BFF)
    val shape = RoundedCornerShape(if (big) 18.dp else 10.dp)
    Box(
        Modifier.fillMaxWidth().aspectRatio(V12ShoeCardRatio).clip(shape)
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF1B2C5C), Color(0xFF101A3A))))
                val step = size.width / 6f
                var x = -size.height
                while (x < size.width) {
                    drawLine(Color.White.copy(alpha = .05f), Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = step / 2.4f)
                    x += step
                }
            }
            .border(if (big) 3.dp else 1.5.dp, rim, shape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            cachedPainterResource(R.drawable.logo_wordmark_white), contentDescription = null,
            contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth(.66f).aspectRatio(5f),
        )
    }
}

