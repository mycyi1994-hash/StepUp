package com.stepup.android.ui.experience

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.stepup.android.domain.RunVerdict
import com.stepup.android.service.WalkSessionService
import com.stepup.android.service.WalkSessionState
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

/** No replay: a saved operation must never make a sound on a later visit. */
object ExperienceEvents {
    val cues = MutableSharedFlow<FeedbackCue>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    fun emit(cue: FeedbackCue) { cues.tryEmit(cue) }
}

fun runFeedbackCue(previous: WalkSessionState, current: WalkSessionState): FeedbackCue? = when {
    current.isActive && !previous.isActive -> FeedbackCue.Start
    current.isActive && current.isPaused != previous.isPaused ->
        if (current.isPaused) FeedbackCue.Pause else FeedbackCue.Resume
    current.lastRewardPoints != null && previous.lastRewardPoints == null ->
        if (current.lastVerdict == RunVerdict.VOID) FeedbackCue.Error else FeedbackCue.Finish
    current.isActive && current.laps.size > previous.laps.size -> FeedbackCue.Lap
    else -> null
}

/** Compare authoritative service transitions, never the user's intention to start/stop. */
@Composable
fun RunFeedback() {
    val feedback = LocalFeedback.current
    LaunchedEffect(feedback) {
        var previous = WalkSessionService.state.value
        WalkSessionService.state.collect { current ->
            val cue = runFeedbackCue(previous, current)
            previous = current
            if (cue != null) feedback?.play(cue)
        }
    }
}
