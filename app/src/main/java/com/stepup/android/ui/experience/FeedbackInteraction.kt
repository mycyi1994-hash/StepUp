package com.stepup.android.ui.experience

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role

/**
 * One interaction source for touch, keyboard, TalkBack and press motion.
 *
 * UI 최종 통합안(2026-10-25) 누름 — 0.97배 + 어둡게 10% + 짧은 진동(설정 따름), 뗄 때 스프링(감쇠 0.55 · 강성 400).
 * 물결(ripple)은 끈다. 어둡게는 그린 픽셀에만 얹는다(둥근 모서리 밖이 어두워지지 않게).
 */
@Composable
fun Modifier.feedbackClickable(
    enabled: Boolean = true,
    cue: FeedbackCue = FeedbackCue.Tap,
    role: Role? = Role.Button,
    onClick: () -> Unit,
): Modifier {
    val feedback = LocalFeedback.current
    val motion = LocalMotion.current
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val down = pressed && enabled
    val scale = animateFloatAsState(
        if (down && !motion.reduced) .97f else 1f,
        if (down) tween(motion.duration(90)) else spring(dampingRatio = .55f, stiffness = 400f), label = "pressScale")
    val dim = animateFloatAsState(if (down) .10f else 0f, tween(if (down) 90 else 160), label = "pressDim")
    return graphicsLayer {
        scaleX = scale.value; scaleY = scale.value
        compositingStrategy = if (dim.value > 0f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
    }
        .drawWithContent {
            drawContent()
            if (dim.value > 0f) drawRect(Color.Black.copy(alpha = dim.value), blendMode = BlendMode.SrcAtop)
        }
        .clickable(interactionSource = interactions, indication = null, enabled = enabled, role = role) {
            feedback?.play(cue)
            onClick()
        }
}
