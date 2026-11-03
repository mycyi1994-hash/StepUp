package com.stepup.android.ui.experience

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

/** 화면 밖(뷰모델)에서 낸 소리 · 진동 신호 — 앱 뿌리가 받아 LocalFeedback 으로 낸다(안드로이드 ExperienceEvents 와 같다) */
object ExperienceEvents {
    val cues = MutableSharedFlow<FeedbackCue>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    fun emit(cue: FeedbackCue) { cues.tryEmit(cue) }
}
