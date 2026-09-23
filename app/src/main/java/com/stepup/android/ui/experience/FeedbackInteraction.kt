package com.stepup.android.ui.experience

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role

/** One interaction source for touch, keyboard, TalkBack, ripple and press motion. */
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
    val scale = animateFloatAsState(
        if (pressed && enabled && !motion.reduced) .975f else 1f,
        tween(motion.duration(if (pressed) 90 else 180)), label = "pressScale")
    return graphicsLayer { scaleX = scale.value; scaleY = scale.value }
        .clickable(interactionSource = interactions, indication = ripple(), enabled = enabled, role = role) {
            feedback?.play(cue)
            onClick()
        }
}
