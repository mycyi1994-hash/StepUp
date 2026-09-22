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
