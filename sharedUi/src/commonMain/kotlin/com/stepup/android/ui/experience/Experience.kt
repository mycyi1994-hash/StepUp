package com.stepup.android.ui.experience

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role

/*
 * 누름 · 효과음 · 진동 · 모션 줄이기 — 공용 화면용(2026-11-03 iOS C0). 안드로이드 app/.../ui/experience 와 같은 이름 · 같은 누름 모양.
 * 소리 · 진동을 실제로 내는 쪽은 기기마다([ExperiencePlayer] — 안드로이드 SoundPool · iOS AVAudio/UIImpactFeedback).
 */

/** 효과음 이름(소리 파일은 기기 쪽이 고른다 — 안드로이드 FeedbackCue 와 같은 차례) */
enum class FeedbackCue {
    Tap, Select, Back, Toggle, SheetOpen, SheetClose, BackgroundSwitch, Equip, Success, Error, Reward,
    Countdown, Start, Resume, Pause, Lap, Finish, DrawEnter, DrawCharge, DrawBoxOpen, DrawCommon,
    DrawRare, DrawEpic, DrawLegendary, DrawCancel, DrawFail,
}

/** 화면 뒤에 은은히 까는 소리(안드로이드 AmbientScene 과 같은 이름) — 고르면 기기 쪽이 그 소리를 돌린다 */
enum class AmbientScene { Dawn, Night, Wardrobe }

/** 기기의 소리 · 진동 */
fun interface ExperiencePlayer {
    fun play(cue: FeedbackCue)

    /** 배경 소리 — null 이면 끈다. 배경 소리가 없는 기기는 아무것도 하지 않는다 */
    fun setAmbientScene(scene: AmbientScene?) {}
}

data class MotionPreferences(val reduced: Boolean = false, val active: Boolean = true) {
    val decorative: Boolean get() = !reduced && active
    fun duration(millis: Int): Int = if (reduced) 0 else millis
}

val LocalMotion = staticCompositionLocalOf { MotionPreferences() }
val LocalFeedback = staticCompositionLocalOf<ExperiencePlayer?> { null }

/**
 * 누름 하나 — 0.97배 + 어둡게 10% + 효과음(설정 따름), 뗄 때 스프링(감쇠 0.55 · 강성 400). 물결은 끈다(안드로이드와 같다).
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
