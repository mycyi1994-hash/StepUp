package com.giwa.strideup.ui.experience

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.giwa.strideup.domain.RunVerdict
import com.giwa.strideup.service.WalkSessionService
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

/** No replay: a saved operation must never make a sound on a later visit. */
object ExperienceEvents {
    val cues = MutableSharedFlow<FeedbackCue>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    fun emit(cue: FeedbackCue) { cues.tryEmit(cue) }
}

/** Compare authoritative service transitions, never the user's intention to start/stop. */
@Composable
fun RunFeedback() {
    val feedback = LocalFeedback.current
    LaunchedEffect(feedback) {
        var previous = WalkSessionService.state.value
        WalkSessionService.state.collect { current ->
            val cue = when {
                current.isActive && !previous.isActive -> FeedbackCue.Start
                current.isActive && current.isPaused != previous.isPaused ->
                    if (current.isPaused) FeedbackCue.Pause else FeedbackCue.Start
                current.lastRewardPoints != null && previous.lastRewardPoints == null ->
                    if (current.lastVerdict == RunVerdict.VOID) FeedbackCue.Error else FeedbackCue.Reward
                current.isActive && current.laps.size > previous.laps.size -> FeedbackCue.Lap
                else -> null
            }
            previous = current
            if (cue != null) feedback?.play(cue)
        }
    }
}
