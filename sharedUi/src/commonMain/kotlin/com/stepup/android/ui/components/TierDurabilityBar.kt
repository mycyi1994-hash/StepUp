package com.stepup.android.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stepup.android.domain.ShoeTier
import com.stepup.android.ui.experience.LocalMotion

/**
 * 등급 내구도 바(2026-10-17 사용자 "내구도 바도 너무 허접 — 등급에 따라 멋지게").
 *
 * 등급마다 채움 색이 다르고, 위로 갈수록 꾸밈이 하나씩 더해진다.
 * - 일반 · 레어: 등급 색 채움 + 윗면 광택 + 10칸 눈금
 * - 에픽부터: 바깥으로 번지는 빛과 채움 끝의 빛점
 * - 레전더리부터: 채움 위를 지나가는 반짝임(모션 줄이기면 멈춘다)
 * - 레드라인 · 피니시: 반짝임이 더 빠르고 테두리가 등급 색으로 빛난다
 *
 * 값은 남은 비율([fraction], 0 ~ 1) 그대로다 — 꾸밈이 값을 바꾸지 않는다.
 */
@Composable
fun TierDurabilityBar(
    fraction: Float,
    tier: ShoeTier,
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
) {
    val t = runTone()
    val style = remember(tier) { durabilityStyle(tier) }
    val f = fraction.coerceIn(0f, 1f)
    val shimmer = rememberDurabilityShimmer(style.shimmerMs)
    Box(
        modifier.fillMaxWidth().height(height + style.glowPad * 2)
            .drawBehind {
                val pad = style.glowPad.toPx()
                val top = pad
                val barH = size.height - pad * 2
                val r = CornerRadius(barH / 2f)
                // 바탕 홈 — 어두운 바닥 + 안쪽 테두리
                drawRoundRect(t.track, topLeft = Offset(0f, top), size = Size(size.width, barH), cornerRadius = r)
                drawRoundRect(
                    if (style.rim != null) style.rim.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.06f),
                    topLeft = Offset(0f, top), size = Size(size.width, barH), cornerRadius = r,
                    style = Stroke(width = 1.dp.toPx()),
                )
                if (f <= 0f) return@drawBehind
                val w = (size.width * f).coerceAtLeast(barH)
                // 번지는 빛(에픽부터) — 채움 둘레에 옅은 띠를 겹친다(흐림 필터 없이)
                if (style.glow != null) {
                    listOf(pad to 0.10f, pad * 0.6f to 0.16f, pad * 0.3f to 0.24f).forEach { (spread, alpha) ->
                        drawRoundRect(
                            style.glow.copy(alpha = alpha),
                            topLeft = Offset(-spread * 0.3f, top - spread), size = Size(w + spread * 0.6f, barH + spread * 2),
                            cornerRadius = CornerRadius((barH + spread * 2) / 2f),
                        )
                    }
                }
                // 채움
                drawRoundRect(
                    Brush.horizontalGradient(style.fill, startX = 0f, endX = w.coerceAtLeast(1f)),
                    topLeft = Offset(0f, top), size = Size(w, barH), cornerRadius = r,
                )
                // 윗면 광택
                drawRoundRect(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0.0f)),
                        startY = top, endY = top + barH * 0.6f,
                    ),
                    topLeft = Offset(barH * 0.25f, top + barH * 0.08f), size = Size((w - barH * 0.5f).coerceAtLeast(0f), barH * 0.42f),
                    cornerRadius = CornerRadius(barH * 0.21f),
                )
                // 10칸 눈금 — 채움 안에서만
                clipRect(0f, top, w, top + barH) {
                    for (i in 1 until 10) {
                        val x = size.width * i / 10f
                        if (x >= w - 1f) break
                        drawLine(Color.Black.copy(alpha = 0.22f), Offset(x, top + barH * 0.18f), Offset(x, top + barH * 0.82f),
                            strokeWidth = 1.dp.toPx())
                    }
                }
                // 지나가는 반짝임(레전더리부터)
                // 한 바퀴의 앞 60% 동안 지나가고 뒤 40% 는 쉰다
                val sweep = shimmer?.value?.let { it / 0.6f }?.takeIf { it < 1f }
                if (sweep != null && w > barH) {
                    val band = maxOf(barH * 3f, size.width * 0.18f)
                    val x = -band + (w + band * 2) * sweep
                    clipRect(0f, top, w, top + barH) {
                        drawRect(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent),
                                startX = x - band / 2f, endX = x + band / 2f,
                            ),
                            topLeft = Offset(x - band / 2f, top), size = Size(band, barH),
                        )
                    }
                }
                // 채움 끝의 빛점(에픽부터)
                if (style.glow != null) {
                    val end = Offset(w - barH / 2f, top + barH / 2f)
                    drawCircle(
                        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.95f), style.glow.copy(alpha = 0.6f), Color.Transparent),
                            center = end, radius = barH * 1.1f),
                        radius = barH * 1.1f, center = end,
                    )
                }
            },
    )
}

/** 등급의 밝은 글자 색 — 내구도 숫자처럼 바 옆에 쓰는 값 */
fun tierAccent(tier: ShoeTier): Color = durabilityStyle(tier).fill.last()

private class DurabilityStyle(
    val fill: List<Color>,
    /** 번지는 빛 · 끝 빛점 색 — null 이면 없다 */
    val glow: Color?,
    /** 테두리 빛 — null 이면 옅은 흰 테두리 */
    val rim: Color?,
    /** 반짝임 한 바퀴(ms) — null 이면 없다 */
    val shimmerMs: Int?,
    val glowPad: Dp,
)

private fun durabilityStyle(tier: ShoeTier): DurabilityStyle = when (tier) {
    ShoeTier.COMMON -> DurabilityStyle(listOf(Color(0xFF6F8299), Color(0xFFC3D0E0)), null, null, null, 0.dp)
    ShoeTier.RARE -> DurabilityStyle(listOf(Color(0xFF1C5FE8), Color(0xFF62B4FF)), null, null, null, 0.dp)
    ShoeTier.EPIC -> DurabilityStyle(listOf(Color(0xFF6A2FD6), Color(0xFFC690FF)), Color(0xFFA463FF), null, null, 4.dp)
    ShoeTier.LEGENDARY -> DurabilityStyle(
        listOf(Color(0xFFB46A0C), Color(0xFFF2B33A), Color(0xFFFFE08A)), Color(0xFFFFB938), null, 2_600, 5.dp,
    )
    ShoeTier.REDLINE -> DurabilityStyle(
        listOf(Color(0xFFA3141F), Color(0xFFE8423D), Color(0xFFFF8A70)), Color(0xFFFF4A3D), Color(0xFFFF6A5A), 1_800, 5.dp,
    )
    ShoeTier.FINISH -> DurabilityStyle(
        listOf(Color(0xFF0B7D6E), Color(0xFF22C9A8), Color(0xFF9CFFE6)), Color(0xFF3CF0C8), Color(0xFF6FF5D8), 1_500, 6.dp,
    )
}

/** 반짝임 진행(0 ~ 1) — 그리기 단계에서만 읽는다. 반짝임이 없는 등급 · 모션 줄이기면 null */
@Composable
private fun rememberDurabilityShimmer(periodMs: Int?): State<Float>? {
    val decorative = LocalMotion.current.decorative
    if (periodMs == null || !decorative) return null
    val transition = rememberInfiniteTransition(label = "durabilityShimmer")
    return transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        // 한 번 지나가고(앞 60%) 쉬었다가(뒤 40%) 다시
        animationSpec = infiniteRepeatable(tween(periodMs, easing = LinearEasing), RepeatMode.Restart),
        label = "durabilityShimmerProgress",
    )
}
