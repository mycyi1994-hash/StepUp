package com.stepup.android.ui.components

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.stepup.android.ui.experience.LocalMotion
import kotlin.math.cos
import kotlin.math.sin

object StepUpMotion {
    const val Quick = 160
    const val Standard = 260
    const val Emphasis = 440

    // ── UI 최종 통합안(2026-10-25 tokens.json motion · compose/Motion.kt) ──
    /** 누름 · 뗌 — 뗄 때 살짝 튕긴다 */
    val press = spring<Float>(dampingRatio = .55f, stiffness = 400f)

    /** 카드 → 상세(컨테이너 변환) 550ms */
    val emphasizedDecelerate = CubicBezierEasing(.05f, .7f, .1f, 1f)
    const val CONTAINER_MS = 550

    /** 아래에서 올라오는 시트 · 완료 화면 */
    val sheet = spring<Float>(dampingRatio = .75f, stiffness = 300f)
    val modal = spring<androidx.compose.ui.unit.IntOffset>(dampingRatio = .8f, stiffness = 300f)
    const val SCRIM_FADE_MS = 250

    /** 화면 바뀜(탭 전환 등) */
    const val FADE_MS = 300

    /** 알림 배너(위에서 떨어짐) · 토스트 */
    val bannerDrop = spring<Float>(dampingRatio = .6f, stiffness = 500f)
    const val BANNER_HOLD_MS = 2600L
    const val TOAST_MS = 2200L

    /** 카운트다운 3 · 2 · 1 — 숫자마다 750ms, 1.35배 → 1배 */
    const val COUNTDOWN_STEP_MS = 750L

    /** 길게 눌러 종료 */
    const val HOLD_TO_END_MS = 1000

    /** 숫자 올라가기 — easeInOutCubic */
    val countUp = CubicBezierEasing(.65f, 0f, .35f, 1f)
}

/** 완료 화면 · 모달: 아래에서 스프링으로 올라온다(뒤 화면은 0.96배 + 어둡게) */
val modalEnter: androidx.compose.animation.EnterTransition =
    androidx.compose.animation.slideInVertically(StepUpMotion.modal) { it } + androidx.compose.animation.fadeIn(tween(120))
val modalExit: androidx.compose.animation.ExitTransition =
    androidx.compose.animation.slideOutVertically(tween(280)) { it } + androidx.compose.animation.fadeOut(tween(200))

/** 숫자가 올라가는 표시(걸음 · SUP · 인원 · 거리). 값이 바뀌면 easeInOutCubic 으로 이어진다. 동작 줄이기면 바로 */
@Composable
fun countUpFloat(value: Float, durationMs: Int = 600): Float {
    val motion = LocalMotion.current
    val v by animateFloatAsState(value, tween(motion.duration(durationMs), easing = StepUpMotion.countUp), label = "countUp")
    return v
}

/** 값이 바뀔 때 살짝 튀기(1/6 → 2/6) — 1.25배에서 스프링으로 제자리 */
@Composable
fun Modifier.popOnChange(key: Any?): Modifier {
    val motion = LocalMotion.current
    val s = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(key) {
        if (first) { first = false; return@LaunchedEffect }
        if (motion.reduced) return@LaunchedEffect
        s.snapTo(1.25f)
        s.animateTo(1f, spring(dampingRatio = .5f, stiffness = 500f))
    }
    return graphicsLayer { scaleX = s.value; scaleY = s.value }
}

/** 달리는 중 경로: 지나온 비율(0..1)만큼 선을 그리고 끝점 좌표를 돌려준다 → 내 위치 점을 그 자리에 */
fun partialPath(full: androidx.compose.ui.graphics.Path, progress: Float): Pair<androidx.compose.ui.graphics.Path, Offset> {
    val pm = androidx.compose.ui.graphics.PathMeasure().apply { setPath(full, false) }
    val out = androidx.compose.ui.graphics.Path()
    val end = pm.length * progress.coerceIn(0f, 1f)
    pm.getSegment(0f, end, out, true)
    return out to pm.getPosition(end)
}

@Composable
fun animatedInt(target: Int, durationMillis: Int = 650): Int {
    val motion = LocalMotion.current
    val value by animateIntAsState(target, tween(motion.duration(durationMillis), easing = FastOutSlowInEasing), label = "metricInt")
    return value
}

@Composable
fun animatedFloat(target: Float, durationMillis: Int = 650): Float {
    val motion = LocalMotion.current
    val value by animateFloatAsState(target, tween(motion.duration(durationMillis), easing = FastOutSlowInEasing), label = "metricFloat")
    return value
}

/** No infinite transition is composed while motion is reduced or the activity is paused. */
@Composable
fun ambientPhase(durationMillis: Int = 3600, reverse: Boolean = false): State<Float> {
    if (!LocalMotion.current.decorative) return rememberUpdatedState(.5f)
    val transition = rememberInfiniteTransition(label = "ambient")
    return transition.animateFloat(0f, 1f, infiniteRepeatable(
        tween(durationMillis, easing = if (reverse) FastOutSlowInEasing else LinearEasing),
        if (reverse) RepeatMode.Reverse else RepeatMode.Restart), label = "ambientPhase")
}

/** Draw-phase reads avoid recomposing the button content on every frame. */
@Composable
fun Modifier.sheen(
    color: Color = Color.White,
    alpha: Float = .12f,
    bandFraction: Float = .24f,
    durationMillis: Int = 4200,
    delayMillis: Int = 2000,
): Modifier {
    if (!LocalMotion.current.decorative) return this
    val phase = ambientPhase(durationMillis + delayMillis)
    return drawWithContent {
        drawContent()
        val active = ((phase.value * (durationMillis + delayMillis) - delayMillis) / durationMillis).coerceIn(0f, 1f)
        if (active > 0f && active < 1f) {
            val band = size.width * bandFraction
            val x = size.width * (-.5f + 2f * active)
            drawRect(Brush.linearGradient(listOf(Color.Transparent, color.copy(alpha = alpha), Color.Transparent),
                Offset(x - band, 0f), Offset(x + band, size.height)))
        }
    }
}

@Composable
fun breathing(durationMillis: Int = 3000): Float = ambientPhase(durationMillis, reverse = true).value

/** A small, finite entrance; never blocks input or delays data availability. */
@Composable
fun Modifier.reveal(key: Any? = Unit): Modifier {
    val motion = LocalMotion.current
    val amount = remember(key) { Animatable(if (motion.reduced) 1f else 0f) }
    LaunchedEffect(key, motion.reduced) {
        if (motion.reduced) amount.snapTo(1f)
        else amount.animateTo(1f, tween(StepUpMotion.Emphasis, easing = FastOutSlowInEasing))
    }
    return graphicsLayer {
        alpha = amount.value
        translationY = 10.dp.toPx() * (1f - amount.value)
    }
}

/** Bounded celebration inside a result card: no flashing, overlay, or input interception. */
@Composable
fun Modifier.celebrate(event: Any?, color: Color = com.stepup.android.ui.theme.Volt): Modifier {
    val motion = LocalMotion.current
    val progress = remember { Animatable(1f) }
    LaunchedEffect(event, motion.reduced) {
        if (event != null && motion.decorative) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(850, easing = LinearOutSlowInEasing))
        } else progress.snapTo(1f)
    }
    return drawWithContent {
        drawContent()
        val t = progress.value
        if (t < 1f) {
            val center = Offset(size.width / 2, size.height * .36f)
            repeat(14) { index ->
                val angle = index * Math.PI * 2 / 14
                val distance = size.minDimension * (.12f + .48f * t)
                drawCircle(color.copy(alpha = (1 - t) * .65f), radius = (2.5f - t).dp.toPx(),
                    center = center + Offset(cos(angle).toFloat() * distance, sin(angle).toFloat() * distance))
            }
        }
    }
}
