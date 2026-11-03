package com.stepup.android.ui.experience

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

/** 화면 밖(뷰모델)에서 낸 소리 · 진동 신호 — 앱 뿌리가 받아 LocalFeedback 으로 낸다(안드로이드 ExperienceEvents 와 같다) */
object ExperienceEvents {
    val cues = MutableSharedFlow<FeedbackCue>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    fun emit(cue: FeedbackCue) { cues.tryEmit(cue) }
}

fun runFeedbackCue(
    previous: com.stepup.android.service.WalkSessionState,
    current: com.stepup.android.service.WalkSessionState,
): FeedbackCue? = when {
    current.isActive && !previous.isActive -> FeedbackCue.Start
    current.isActive && current.isPaused != previous.isPaused ->
        if (current.isPaused) FeedbackCue.Pause else FeedbackCue.Resume
    current.lastRewardPoints != null && previous.lastRewardPoints == null ->
        if (current.lastVerdict == com.stepup.android.domain.RunVerdict.VOID) FeedbackCue.Error else FeedbackCue.Finish
    current.isActive && current.laps.size > previous.laps.size -> FeedbackCue.Lap
    else -> null
}

/** 러닝 상태가 실제로 바뀐 것을 보고 소리를 낸다(누른 뜻이 아니라 — 안드로이드와 같다) */
@androidx.compose.runtime.Composable
fun RunFeedback() {
    val feedback = LocalFeedback.current
    androidx.compose.runtime.LaunchedEffect(feedback) {
        val run = com.stepup.android.core.AppServices.run.state
        var previous = run.value
        run.collect { current ->
            val cue = runFeedbackCue(previous, current)
            previous = current
            if (cue != null) feedback?.play(cue)
        }
    }
}
