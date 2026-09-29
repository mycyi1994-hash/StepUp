package com.stepup.android.ui.screens.gacha

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.DrawKind
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.components.SneakerVisual
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpColors

/*
 * 신발 뽑기 한 벌(2026-09-28 전달본 "신발 뽑기 디자인" 26장, docs/redesign/shoe-draw-v3) — 색 · 버튼 · 탭 · 상자 무대 · 상자 열기.
 * 어두운 테마는 확정 메인 시안(01 · 02)에서 잰 값(편집용 디자인의 design.css), 밝은 테마는 같은 관계로 옮긴 값이다.
 * 상자는 매트한 실제 신발 상자 — 네온 · 발광 링 · 폭죽 · 금빛 상자를 더하지 않는다(전달서).
 */

@Immutable
internal class DrawPalette(
    val background: Color,
    val glow: Color,
    val text: Color,
    /** 보조 글 — "남은 무료 뽑기" 같은 이름 */
    val secondary: Color,
    /** 더 흐린 보조 글 — 안내 한 줄 */
    val note: Color,
    /** 강조 숫자 · 글자 링크(+1회 · 지금 사용할 수 있는 기회) */
    val accent: Color,
    val divider: Color,
    val tabFace: Color,
    val tabEdge: Color,
    val tabOn: Color,
    val tabOnEdge: Color,
    val tabOffText: Color,
    val stageFrom: Color,
    val stageTo: Color,
    val stageGlow: Color,
    val stageText: Color,
    val stageSub: Color,
    val chipFace: Color,
    val chipText: Color,
    val chipQuietFace: Color,
    val chipQuietText: Color,
    val primaryFace: Color,
    val primaryEdge: Color,
    val primaryText: Color,
    /** 누를 수 없는 버튼 — "불러오는 중" · "상급 뽑기를 잠시 멈췄어요" */
    val offFace: Color,
    val offEdge: Color,
    val offText: Color,
    /** 테두리만 있는 보조 버튼 — "한 번 더 뽑기" · "닫기" · "무료 기회 안내" */
    val ghostEdge: Color,
    val ghostText: Color,
    val row: Color,
    val rowEdge: Color,
    val skeleton: Color,
    val track: Color,
    val barFrom: Color,
    val barTo: Color,
    val toastFace: Color,
    val toastEdge: Color,
)

private val DarkDraw = DrawPalette(
    background = Color(0xFF0A111F), glow = Color(0xFF0C1428),
    text = Color(0xFFF2F4FC), secondary = Color(0xFF93A1DC), note = Color(0xFF7482B8), accent = Color(0xFF6D8BFF),
    divider = Color(0xFF25304F),
    tabFace = Color(0xFF0E1528), tabEdge = Color(0xFF252E8B), tabOn = Color(0xFF3B51D3), tabOnEdge = Color(0x598CA0FF),
    tabOffText = Color(0xFF9AA6DA),
    stageFrom = Color(0xFF2F4696), stageTo = Color(0xFF2B3F87), stageGlow = Color(0xFF3A4E9E),
    stageText = Color(0xFFFFFFFF), stageSub = Color(0xFFAAB7F2),
    chipFace = Color(0xFFBBCBFD), chipText = Color(0xFF1F2B7A), chipQuietFace = Color(0x38BBCBFD), chipQuietText = Color(0xFFDDE4FF),
    primaryFace = Color(0xFF3358FA), primaryEdge = Color(0xFF829EF6), primaryText = Color(0xFFFFFFFF),
    offFace = Color(0xFF353F68), offEdge = Color(0xFF4A5584), offText = Color(0xFFA3ADD6),
    ghostEdge = Color(0xFF2F3D70), ghostText = Color(0xFFC9D2F5),
    row = Color(0xE616213E), rowEdge = Color(0xFF23305A),
    skeleton = Color(0xFF223249), track = Color(0xFF26304F), barFrom = Color(0xFF2F58F0), barTo = Color(0xFF6A8BFF),
    toastFace = Color(0xFF16213E), toastEdge = Color(0xFF2A3866),
)

private val LightDraw = DrawPalette(
    background = Color(0xFFF4F6FC), glow = Color(0xFFE3E9FA),
    text = Color(0xFF121A33), secondary = Color(0xFF56628E), note = Color(0xFF6B769C), accent = Color(0xFF3358FA),
    divider = Color(0xFFD5DCEE),
    tabFace = Color(0xFFFFFFFF), tabEdge = Color(0xFFC9D2F5), tabOn = Color(0xFF3B51D3), tabOnEdge = Color(0x598CA0FF),
    tabOffText = Color(0xFF56628E),
    stageFrom = Color(0xFF3E57B2), stageTo = Color(0xFF34499C), stageGlow = Color(0xFF5A70C4),
    stageText = Color(0xFFFFFFFF), stageSub = Color(0xFFDCE3FF),
    chipFace = Color(0xFFDCE4FF), chipText = Color(0xFF1F2B7A), chipQuietFace = Color(0x40FFFFFF), chipQuietText = Color(0xFFFFFFFF),
    primaryFace = Color(0xFF3358FA), primaryEdge = Color(0xFF829EF6), primaryText = Color(0xFFFFFFFF),
    offFace = Color(0xFFDCE1F0), offEdge = Color(0xFFC3CAE0), offText = Color(0xFF6B769C),
    ghostEdge = Color(0xFFB9C3E6), ghostText = Color(0xFF2B3A73),
    row = Color(0xFFFFFFFF), rowEdge = Color(0xFFD5DCEE),
    skeleton = Color(0xFFD3DCE9), track = Color(0xFFDCE2F2), barFrom = Color(0xFF2F58F0), barTo = Color(0xFF6A8BFF),
    toastFace = Color(0xFF16213E), toastEdge = Color(0xFF2A3866),
)

@Composable
internal fun drawPalette(): DrawPalette = if (StepUpColors.dark) DarkDraw else LightDraw

/** 셸 밖에서 결과를 그릴 때(예전 이름 [DrawResultDialog])만 까는 바탕 — 짙은 남색, 위가 조금 밝다 */
@Composable
internal fun DrawBackdrop(modifier: Modifier = Modifier) {
    val p = drawPalette()
    Canvas(modifier.background(p.background)) {
        val center = Offset(size.width * 0.5f, 0f)
        val radius = size.width * 1.1f
        drawCircle(Brush.radialGradient(listOf(p.glow, Color.Transparent), center, radius), radius, center)
    }
}

internal enum class DrawButtonStyle { Primary, Ghost, Off }

/**
 * 실행 버튼 — 일반 크기(높이 52, 모서리 14, 19sp). 화면마다 크기를 바꾸지 않는다(전달서: 과하게 높은 버튼 금지).
 * 누를 수 없으면(또는 [DrawButtonStyle.Off]) 흐린 면. 일하는 중이면 돌고 누를 수 없다.
 */
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
    val shape = RoundedCornerShape(14.dp)
    val off = style == DrawButtonStyle.Off || (!enabled && style == DrawButtonStyle.Primary)
    val face = when {
        off -> p.offFace
        style == DrawButtonStyle.Ghost -> Color.Transparent
        else -> p.primaryFace
    }
    val edge = when {
        off -> p.offEdge
        style == DrawButtonStyle.Ghost -> p.ghostEdge
        else -> p.primaryEdge
    }
    val ink = when {
        off -> p.offText
        style == DrawButtonStyle.Ghost -> if (enabled) p.ghostText else p.offText
        else -> p.primaryText
    }
    val clickable = enabled && !loading && style != DrawButtonStyle.Off
    Row(
        modifier.fillMaxWidth().heightIn(min = 52.dp).clip(shape).background(face, shape).border(1.5.dp, edge, shape)
            .feedbackClickable(enabled = clickable, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), color = ink, strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text, color = ink, fontSize = 19.sp, letterSpacing = (-0.2).sp, textAlign = TextAlign.Center,
            fontWeight = if (style == DrawButtonStyle.Ghost) FontWeight.SemiBold else FontWeight.Bold,
        )
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
/**
 * 위 · 무대 · 아래를 쌓는다 — 위 · 아래 글자 칸을 먼저 재고, 무대에는 남은 높이를 준다([minStage]–[maxStage]).
 * 창 높이 [viewport] 안에 다 들면 스크롤 거리가 0 이다(메인: 내용 · 실행 버튼 · 하단 탭이 함께, 결과: 신발 · 이름이 함께 보인다).
 * 모자라면(작은 화면 · 큰 글씨) 무대는 [minStage] 로 두고 넘친다 — 부모가 스크롤한다. 무대 칸은 폭 × 높이가 정해져 들어온다.
 */
@Composable
internal fun DrawFitLayout(
    viewport: Int,
    modifier: Modifier,
    topGap: Dp,
    stageGap: Dp,
    endGap: Dp,
    minStage: Dp,
    maxStage: Dp,
    top: @Composable () -> Unit,
    stage: @Composable () -> Unit,
    bottom: @Composable () -> Unit,
) {
    Layout(contents = listOf(top, stage, bottom), modifier = modifier) { (t, s, b), constraints ->
        val width = constraints.maxWidth
        val loose = Constraints(maxWidth = width)
        val topP = t.first().measure(loose)
        val bottomP = b.first().measure(loose)
        val fixed = topGap.roundToPx() + topP.height + stageGap.roundToPx() + bottomP.height + endGap.roundToPx()
        val stageH = if (viewport == Constraints.Infinity) maxStage.roundToPx()
        else (viewport - fixed).coerceIn(minStage.roundToPx(), maxStage.roundToPx())
        val stageP = s.first().measure(Constraints.fixed(width, stageH))
        val content = fixed + stageH
        val height = if (viewport == Constraints.Infinity) content else maxOf(content, viewport)
        layout(width, height) {
            var y = topGap.roundToPx()
            topP.place(0, y)
            y += topP.height + stageGap.roundToPx()
            stageP.place(0, y)
            y += stageH
            bottomP.place(0, y)
        }
    }
}

@Composable
internal fun DrawProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    val p = drawPalette()
    Box(modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(p.track)) {
        Box(
            Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(4.dp))
                .background(Brush.horizontalGradient(listOf(p.barFrom, p.barTo))),
        )
    }
}

// ── 받침과 상자 · 상자 열기(11) ─────────────────────────────────────
//
// 전달본 04-motion/unboxing.json(390 × 420, 60fps, 약 2초)의 값을 그대로 옮겼다 — 뚜껑이 들려 오른쪽 위로 비켜나고(0.14~0.8초),
// 신발이 상자에서 올라오고(0.45~1.0초), 상자가 오른쪽으로 빠진 뒤(0.92~1.5초) 신발이 받침에 내려앉는다(1.2~1.75초).
// 뒤의 푸른 빛 · 그림자는 직접 그리고, 상자 몸통 · 뚜껑은 뽑기 디자인의 상자 그림(draw_box_body · 무료 draw_box_lid ·
// 상급 draw_box_lid_premium — 원본 상자에서 뚜껑 색만 시안에 맞춘 것)을 쓴다. 받침은 두지 않는다(26장 시안).
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
 * 신발 상자. [shoe] 가 없으면 닫힌 상자, 있으면 열어 그 신발을 보인다(05). 뚜껑 색은 [kind](무료 라벤더 · 상급 오프화이트).
 * [play] 면 처음부터 끝까지 한 번 돌고 [onFinished]. [frozenAt] 은 기기 검사가 한 순간(밀리초)을 찍을 때만 쓴다.
 */
@Composable
internal fun UnboxingStage(
    shoe: Sneaker?,
    modifier: Modifier = Modifier,
    kind: DrawKind = DrawKind.FREE,
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
    val lid = painterResource(if (kind == DrawKind.PREMIUM) R.drawable.draw_box_lid_premium else R.drawable.draw_box_lid)
    BoxWithConstraints(modifier.aspectRatio(ArtW / ArtH)) {
        val unit = constraints.maxWidth / ArtW
        val unitDp = maxWidth / ArtW
        fun fade() = if (reduced) (time.value / ReducedMillis).coerceIn(0f, 1f) else 0f
        // 뒤의 푸른 빛 · 바닥 그림자
        Canvas(Modifier.fillMaxSize()) {
            drawStageGlow(unit, p)
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

/** 상자 뒤의 푸른 빛(시안 rgba(55, 80, 190, .45))과 바닥의 넓은 그림자 */
private fun DrawScope.drawStageGlow(unit: Float, p: DrawPalette) {
    val glowCenter = Offset(195f * unit, 250f * unit)
    drawCircle(
        Brush.radialGradient(listOf(Color(0x733750BE), Color.Transparent), glowCenter, 170f * unit),
        170f * unit, glowCenter,
    )
    softEllipse(Offset(195f * unit, 372f * unit), 300f * unit, 44f * unit, if (p === DarkDraw) 0.5f else 0.25f)
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
