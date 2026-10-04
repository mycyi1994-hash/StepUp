package com.stepup.android.ui.screens.gacha

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stepup.android.R
import com.stepup.android.domain.DrawKind
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.components.RunBackdrop
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunMeter
import com.stepup.android.ui.components.RunTextAction
import com.stepup.android.ui.components.SneakerVisual
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.LocalMotion

/*
 * 신발 뽑기 한 벌 — 2026-10-03 파란 톤 통합 전달본 v4(stepup-draw-blue-claude-v19)부터 색 · 버튼 · 바닥은 러닝 리메이크의 남색 부품
 * (ui/components/RunStyle.kt)을 그대로 쓴다. 여기에는 뽑기만의 것 — 화면 칸 나누기([DrawFitLayout]) · 상자 열기([UnboxingStage]) —
 * 와 예전 이름(DrawButton · DrawBackdrop)을 남색 부품으로 잇는 얇은 이음만 둔다.
 * 상자는 매트한 실제 신발 상자다 — 네온 · 폭죽 · 금빛 상자를 더하지 않는다. 빛은 상자 · 신발 무대에만 둔다(화면 전체를 깜빡이지 않는다).
 */

/** 뽑기 탭 바닥 — 러닝 · 신발 탭과 같은 남색 바닥(앱 셸이 상태 막대 밑까지 깐다) */
@Composable
internal fun DrawBackdrop(modifier: Modifier = Modifier) {
    RunBackdrop(modifier)
}

internal enum class DrawButtonStyle { Primary, Ghost, Off }

/**
 * 뽑기의 버튼 — 주 행동은 공통 주 버튼(흰 면 · 남색 글자 · 얇은 파란 아랫면, 눌리면 1~2dp 내려앉는다), 보조 행동은 글자 버튼
 * (닫기 · 나중에 · 뽑기 화면으로). 누를 수 없으면([DrawButtonStyle.Off] · [enabled] = false) 흐린 면, 일하는 중이면 도는 표시.
 * 화면마다 모양 · 크기를 바꾸지 않는다(전달서: 시안마다 다른 모서리는 공통 버튼 하나로).
 */
@Composable
internal fun DrawButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: DrawButtonStyle = DrawButtonStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    when (style) {
        DrawButtonStyle.Ghost -> RunTextAction(text, onClick, modifier.fillMaxWidth(), enabled = enabled && !loading)
        else -> RunButton(
            text, onClick, modifier, enabled = enabled && style != DrawButtonStyle.Off, busy = loading, icon = icon,
        )
    }
}

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

/** 러닝 진행 막대 — 모은 거리의 몫(서버 값). 공통 막대(파랑 → 시안) */
@Composable
internal fun DrawProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    RunMeter(fraction, modifier, height = 8.dp)
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
    val tone = runTone()
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
    val body = com.stepup.android.ui.components.cachedPainterResource(R.drawable.draw_box_body)
    val lid = com.stepup.android.ui.components.cachedPainterResource(if (kind == DrawKind.PREMIUM) R.drawable.draw_box_lid_premium else R.drawable.draw_box_lid)
    BoxWithConstraints(modifier.aspectRatio(ArtW / ArtH)) {
        val unit = constraints.maxWidth / ArtW
        val unitDp = maxWidth / ArtW
        fun fade() = if (reduced) (time.value / ReducedMillis).coerceIn(0f, 1f) else 0f
        // 뒤의 푸른 빛 · 바닥 그림자
        Canvas(Modifier.fillMaxSize()) {
            drawStageGlow(unit, tone.dark)
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

/** 상자 뒤의 푸른 빛(전기 파랑 #0754FF)과 바닥의 넓은 그림자 — 빛은 상자 자리에만 */
private fun DrawScope.drawStageGlow(unit: Float, dark: Boolean) {
    val glowCenter = Offset(195f * unit, 250f * unit)
    drawCircle(
        Brush.radialGradient(listOf(Color(0x800754FF), Color(0x2048D9FA), Color.Transparent), glowCenter, 170f * unit),
        170f * unit, glowCenter,
    )
    softEllipse(Offset(195f * unit, 372f * unit), 300f * unit, 44f * unit, if (dark) 0.5f else 0.25f)
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
