package com.stepup.android.ui.screens.gacha

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.components.SneakerVisual
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpColors

/*
 * 신발 뽑기 v2 한 벌 — 색 · 버튼 · 글자 링크 · 받침과 상자(상자 열기 연출). 어두운 테마는 전달본 design-tokens.json 값,
 * 밝은 테마는 설정 v1 의 밝은 값에서 골랐다. 상급 칸에 금색 상자 · 등급 보석 · 폭죽을 더하지 않는다(전달서).
 */

@Immutable
internal class DrawPalette(
    val background: Color,
    val glow: Color,
    val text: Color,
    val secondary: Color,
    val accent: Color,
    val divider: Color,
    val freeSurface: Color,
    val premiumSurface: Color,
    val primaryFace: Brush,
    val primaryText: Color,
    /** 조용한 버튼 — "무료 기회 안내" · "러닝하고 기회 받기" · "결과 확인" */
    val quietFace: Color,
    val quietText: Color,
    /** 누를 수 없는 버튼 — "불러오는 중" */
    val idleFace: Color,
    val idleText: Color,
    val skeleton: Color,
    val track: Color,
    val barFrom: Color,
    val barTo: Color,
    val plinthTop: Color,
    val plinthBottom: Color,
    val plinthSide: Color,
    val plinthRim: Color,
    val stageGlow: Color,
)

private val DarkDraw = DrawPalette(
    background = Color(0xFF050912), glow = Color(0xFF0E1D35),
    text = Color(0xFFF2F4FC), secondary = Color(0xFF98A8C0), accent = Color(0xFFA3BFFE), divider = Color(0xFF253248),
    freeSurface = Color(0xFF0C1625), premiumSurface = Color(0xFF102036),
    primaryFace = SolidColor(Color(0xFFF2F4FC)), primaryText = Color(0xFF0B1220),
    quietFace = Color(0xFF23334B), quietText = Color(0xFFF2F4FC),
    idleFace = Color(0xFF172234), idleText = Color(0xFF7D8BA2),
    skeleton = Color(0xFF223249), track = Color(0xFF2A3B53), barFrom = Color(0xFF7FA2F2), barTo = Color(0xFF96B7FF),
    plinthTop = Color(0xFF1F2D40), plinthBottom = Color(0xFF152233), plinthSide = Color(0xFF0A111D),
    plinthRim = Color(0xFF2E3C52), stageGlow = Color(0xFF6E8297),
)

private val LightDraw = DrawPalette(
    background = Color(0xFFF6F8FC), glow = Color(0xFFE2EBFA),
    text = Color(0xFF10203B), secondary = Color(0xFF536580), accent = Color(0xFF335EAB), divider = Color(0xFFD8E0ED),
    freeSurface = Color(0xFFEAEFF7), premiumSurface = Color(0xFFDFE7F4),
    primaryFace = Brush.verticalGradient(listOf(Color(0xFF1B2D4E), Color(0xFF10203B))), primaryText = Color.White,
    quietFace = Color(0xFFCFD9E8), quietText = Color(0xFF10203B),
    idleFace = Color(0xFFE1E7F0), idleText = Color(0xFF7D8BA2),
    skeleton = Color(0xFFD3DCE9), track = Color(0xFFD5DEEB), barFrom = Color(0xFF5C82C8), barTo = Color(0xFF335EAB),
    plinthTop = Color(0xFFDCE4F0), plinthBottom = Color(0xFFC9D4E4), plinthSide = Color(0xFFAFBCD0),
    plinthRim = Color(0xFFEEF2F8), stageGlow = Color(0xFF8FA8D6),
)

@Composable
internal fun drawPalette(): DrawPalette = if (StepUpColors.dark) DarkDraw else LightDraw

/**
 * 뽑기 바탕 — 짙은 남색에 오른쪽 위가 조금 밝다(시안의 #050912). 앱 셸 안에서는 셸의 공통 바탕이 상태 막대 밑까지 깔므로
 * 쓰지 않고, 셸 밖에서 결과를 그릴 때(예전 이름 [DrawResultDialog])만 깐다.
 */
@Composable
internal fun DrawBackdrop(modifier: Modifier = Modifier) {
    val p = drawPalette()
    Canvas(modifier.background(p.background)) {
        val center = Offset(size.width * 0.92f, size.height * 0.02f)
        val radius = size.width * 0.95f
        drawCircle(Brush.radialGradient(listOf(p.glow, Color.Transparent), center, radius), radius, center)
    }
}

internal enum class DrawButtonStyle { Primary, Quiet, Idle }

/** 칸 · 결과의 버튼 — 높이 50 이상, 모서리 17(시안). 일하는 중이면 돌고 누를 수 없다 */
@Composable
internal fun DrawButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: DrawButtonStyle = DrawButtonStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val p = drawPalette()
    val shape = RoundedCornerShape(17.dp)
    val face: Brush = when (style) {
        DrawButtonStyle.Primary -> p.primaryFace
        DrawButtonStyle.Quiet -> SolidColor(p.quietFace)
        DrawButtonStyle.Idle -> SolidColor(p.idleFace)
    }
    val ink = when (style) {
        DrawButtonStyle.Primary -> p.primaryText
        DrawButtonStyle.Quiet -> p.quietText
        DrawButtonStyle.Idle -> p.idleText
    }
    val clickable = enabled && !loading && style != DrawButtonStyle.Idle
    Row(
        modifier.fillMaxWidth().heightIn(min = 50.dp).clip(shape).background(face, shape)
            .feedbackClickable(enabled = clickable, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), color = ink, strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, color = ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

/** 글자 링크 — "기회 내역 보기 ›" · "다음 1회까지 0.4km ›" · "닫기". 누르는 곳은 44dp 이상 */
@Composable
internal fun DrawLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    chevron: Boolean = false,
    fontSize: Float = 14f,
    enabled: Boolean = true,
) {
    val p = drawPalette()
    Row(
        modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(10.dp))
            .feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(end = if (chevron) 0.dp else 6.dp, start = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = if (enabled) p.accent else p.secondary, fontSize = fontSize.sp, fontWeight = FontWeight.Medium)
        if (chevron) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = p.accent, modifier = Modifier.size(20.dp))
        }
    }
}

/** 러닝 진행 막대 — 모은 거리의 몫(서버 값) */
@Composable
internal fun DrawProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    val p = drawPalette()
    Box(modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(p.track)) {
        Box(
            Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(3.dp))
                .background(Brush.horizontalGradient(listOf(p.barFrom, p.barTo))),
        )
    }
}

// ── 받침과 상자 · 상자 열기(11) ─────────────────────────────────────
//
// 전달본 04-motion/unboxing.json(390 × 420, 60fps, 약 2초)의 값을 그대로 옮겼다 — 뚜껑이 들려 오른쪽 위로 비켜나고(0.14~0.8초),
// 신발이 상자에서 올라오고(0.45~1.0초), 상자가 오른쪽으로 빠진 뒤(0.92~1.5초) 신발이 받침에 내려앉는다(1.2~1.75초).
// 받침 · 그림자는 선명하도록 직접 그리고, 상자 몸통 · 뚜껑은 전달본 그림(draw_box_body · draw_box_lid)을 쓴다.
// 신발은 서버가 정한 실제 신발 그림(SneakerVisual)이다. 동작 줄이기면 0.2초 동안 상자가 사라지며 신발이 나타난다.

private const val ArtW = 390f
private const val ArtH = 420f

/** 전달본 이미지 한 변(1254) — 앵커 · 가림 선은 이 좌표로 적혀 있다 */
private const val Img = 1254f
private const val BoxScale = 0.266f
private const val ShoeScale = 0.249f

internal const val OpeningMillis = 2_000f
private const val ReducedMillis = 200f

private val Move: Easing = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)
private val Enter: Easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

private fun phase(t: Float, from: Float, to: Float, easing: Easing): Float =
    easing.transform(((t - from) / (to - from)).coerceIn(0f, 1f))

/** 상자 몸통의 앞면만(뚜껑 자리 아래) — 올라오는 신발의 아래쪽을 가린다(전달본 마스크 그대로) */
private val FrontRim = GenericShape { size, _ ->
    moveTo(0f, size.height * 578f / Img)
    lineTo(size.width * 976f / Img, size.height * 710f / Img)
    lineTo(size.width, size.height * 504f / Img)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

private class Pose(val x: Float, val y: Float, val scale: Float, val rotation: Float, val alpha: Float = 1f)

private fun boxPose(t: Float, reduced: Boolean, fade: Float): Pose {
    if (reduced) return Pose(195f, 330f, BoxScale, 0f, 1f - fade)
    val slide = phase(t, 920f, 1500f, Move)
    return Pose(195f + 440f * slide, 330f + 25f * slide, BoxScale - 0.02f * slide, 5f * slide)
}

private fun lidPose(t: Float, reduced: Boolean, fade: Float): Pose {
    if (reduced) return Pose(195f, 222f, BoxScale, 0f, 1f - fade)
    val lift = phase(t, 140f, 500f, Move)
    val away = phase(t, 500f, 800f, Move)
    return Pose(195f + 50f * lift + 424f * away, 222f - 87f * lift + 18f * away, BoxScale, 8f * lift + 5f * away)
}

private fun shoePose(t: Float, reduced: Boolean, fade: Float): Pose {
    if (reduced) return Pose(195f, 255f, ShoeScale, 0f, fade)
    val rise = phase(t, 450f, 1030f, Move)
    val land = phase(t, 1180f, 1700f, Move)
    return Pose(195f, 362f - 219.3f * rise + 112.3f * land, 0.209f + 0.038f * rise + 0.002f * land, -4f * rise + 4f * land)
}

/** 신발이 보이는 아래 선(그림 좌표) — 상자가 빠지기 전에는 상자 안이라 가려진다 */
private fun shoeMask(t: Float, reduced: Boolean): Float =
    if (reduced) ArtH else 310f + 110f * phase(t, 917f, 1170f, Enter)

/**
 * 받침 위의 신발 상자. [shoe] 가 없으면 닫힌 상자(10 · 20), 있으면 열어 그 신발을 보인다(11).
 * [play] 면 처음부터 끝까지 한 번 돌고 [onFinished]. [frozenAt] 은 기기 검사가 한 순간(밀리초)을 찍을 때만 쓴다.
 */
@Composable
internal fun UnboxingStage(
    shoe: Sneaker?,
    modifier: Modifier = Modifier,
    play: Boolean = false,
    onFinished: () -> Unit = {},
    frozenAt: Float? = null,
) {
    val p = drawPalette()
    val reduced = LocalMotion.current.reduced && frozenAt == null
    val time = remember(shoe?.id) { Animatable(frozenAt ?: 0f) }
    val finished by rememberUpdatedState(onFinished)
    LaunchedEffect(shoe?.id, play, frozenAt, reduced) {
        if (frozenAt != null) {
            time.snapTo(frozenAt)
            return@LaunchedEffect
        }
        if (shoe == null || !play) return@LaunchedEffect
        val end = if (reduced) ReducedMillis else OpeningMillis
        if (time.value < end) time.animateTo(end, tween((end - time.value).toInt(), easing = LinearEasing))
        finished()
    }
    // 몸통 그림은 뒤 · 앞 두 겹이 함께 쓴다 — 한 번만 푼다
    val body = painterResource(R.drawable.draw_box_body)
    val lid = painterResource(R.drawable.draw_box_lid)
    BoxWithConstraints(modifier.aspectRatio(ArtW / ArtH)) {
        val unit = constraints.maxWidth / ArtW
        val unitDp = maxWidth / ArtW
        fun fade() = if (reduced) (time.value / ReducedMillis).coerceIn(0f, 1f) else 0f
        // 받침 · 조명 · 그림자
        Canvas(Modifier.fillMaxSize()) {
            drawPlinth(unit, p)
            val t = time.value
            val box = boxPose(t, reduced, fade())
            softEllipse(Offset(box.x * unit, 329f * unit), 290f * unit, 62f * unit, 0.40f * box.alpha)
            if (shoe != null) {
                val land = if (reduced) fade() else phase(t, 1170f, 1750f, Move)
                val width = (0.968f - 0.062f * (if (reduced) 1f else land)) * 320f
                val height = (0.561f - 0.155f * (if (reduced) 1f else land)) * 64f
                softEllipse(Offset(195f * unit, 351f * unit), width * unit, height * unit, 0.13f + 0.62f * land)
            }
        }
        // 상자 안쪽(뒤)
        ArtLayer(body, unitDp, BoxScale, anchorY = 957.4f) { boxPose(time.value, reduced, fade()) }
        // 신발 — 상자 안에 있는 동안은 가림 선 아래가 보이지 않는다
        if (shoe != null) {
            Box(
                Modifier.fillMaxSize().drawWithContent {
                    clipRect(left = -size.width, top = -size.height, right = size.width * 2, bottom = shoeMask(time.value, reduced) * unit) {
                        this@drawWithContent.drawContent()
                    }
                },
            ) {
                ArtLayer(null, unitDp, ShoeScale, anchorY = Img / 2, content = { SneakerVisual(shoe, Modifier.fillMaxSize()) }) {
                    shoePose(time.value, reduced, fade())
                }
            }
        }
        // 상자 앞면 · 뚜껑
        ArtLayer(body, unitDp, BoxScale, anchorY = 957.4f, clip = true) { boxPose(time.value, reduced, fade()) }
        ArtLayer(lid, unitDp, BoxScale, anchorY = 551.9f) { lidPose(time.value, reduced, fade()) }
    }
}

/** 전달본 좌표의 한 겹 — 정사각 그림을 [base] 배율로 두고, 앵커를 [pose] 위치에 맞춰 옮기고 · 돌리고 · 키운다 */
@Composable
private fun ArtLayer(
    painter: Painter?,
    unitDp: Dp,
    base: Float,
    anchorY: Float,
    clip: Boolean = false,
    content: (@Composable () -> Unit)? = null,
    pose: () -> Pose,
) {
    val side = unitDp * (Img * base)
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.size(side).graphicsLayer {
                val now = pose()
                val sidePx = side.toPx()
                val unitPx = unitDp.toPx()
                transformOrigin = TransformOrigin(0.5f, anchorY / Img)
                scaleX = now.scale / base
                scaleY = now.scale / base
                rotationZ = now.rotation
                alpha = now.alpha
                translationX = now.x * unitPx - 0.5f * sidePx
                translationY = now.y * unitPx - anchorY / Img * sidePx
            }.then(if (clip) Modifier.clip(FrontRim) else Modifier),
        ) {
            if (painter != null) {
                Image(painter, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            }
            content?.invoke()
        }
    }
}

/** 낮은 받침 — 위 면(위에서 아래로 어두워진다) · 옆면 · 아래 가장자리의 밝은 선 · 위쪽 조명 */
private fun DrawScope.drawPlinth(unit: Float, p: DrawPalette) {
    val glowCenter = Offset(195f * unit, 232f * unit)
    drawCircle(Brush.radialGradient(listOf(p.stageGlow.copy(alpha = 0.16f), Color.Transparent), glowCenter, 200f * unit), 200f * unit, glowCenter)
    softEllipse(Offset(195f * unit, 398f * unit), 380f * unit, 34f * unit, 0.45f)
    val left = 12f * unit
    val width = 366f * unit
    val height = 108f * unit
    drawOval(p.plinthSide, topLeft = Offset(left, (344f - 54f) * unit), size = Size(width, height))
    drawOval(
        Brush.verticalGradient(listOf(p.plinthTop, p.plinthBottom), startY = 274f * unit, endY = 382f * unit),
        topLeft = Offset(left, 274f * unit), size = Size(width, height),
    )
    drawOval(
        Brush.verticalGradient(listOf(Color.Transparent, p.plinthRim), startY = 330f * unit, endY = 382f * unit),
        topLeft = Offset(left, 274f * unit), size = Size(width, height), style = Stroke(width = 1.4f * unit),
    )
}

/** 가장자리가 흐린 타원 그림자 */
private fun DrawScope.softEllipse(center: Offset, width: Float, height: Float, alpha: Float) {
    if (alpha <= 0f || width <= 0f || height <= 0f) return
    withTransform({ scale(1f, height / width, pivot = center) }) {
        drawCircle(
            Brush.radialGradient(listOf(Color.Black.copy(alpha = alpha.coerceIn(0f, 1f)), Color.Transparent), center, width / 2f),
            width / 2f, center,
        )
    }
}
