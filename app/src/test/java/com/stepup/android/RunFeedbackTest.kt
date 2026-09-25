package com.stepup.android

import com.stepup.android.domain.RunVerdict
import com.stepup.android.service.RunLap
import com.stepup.android.service.WalkSessionState
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.runFeedbackCue
import org.junit.Assert.*
import org.junit.Test

class RunFeedbackTest {
    private val idle = WalkSessionState()
    private val running = idle.copy(isActive = true, startedAt = 123L)

    @Test fun timeAndStepUpdatesStayQuiet() {
        assertNull(runFeedbackCue(running, running.copy(steps = 200, elapsedSec = 60)))
    }
    @Test fun committedStartPauseAndResumeHaveDistinctCues() {
        val paused = running.copy(isPaused = true)
        assertEquals(FeedbackCue.Start, runFeedbackCue(idle, running))
        assertEquals(FeedbackCue.Pause, runFeedbackCue(running, paused))
        assertEquals(FeedbackCue.Resume, runFeedbackCue(paused, running))
    }
    @Test fun invalidSettlementNeverCelebratesAndExistingReceiptNeverReplays() {
        val receipt = idle.copy(lastRewardPoints = 15.0)
        assertEquals(FeedbackCue.Finish, runFeedbackCue(running, receipt))
        assertNull(runFeedbackCue(receipt, receipt))
        assertEquals(FeedbackCue.Error, runFeedbackCue(running, receipt.copy(lastVerdict = RunVerdict.VOID)))
    }
    @Test fun newLapRingsOnceAndSessionResetDoesNotRing() {
        val lap = running.copy(laps = listOf(RunLap(1, 1.0, 360)))
        assertEquals(FeedbackCue.Lap, runFeedbackCue(running, lap))
        assertNull(runFeedbackCue(lap, lap.copy(elapsedSec = 361)))
        assertNull(runFeedbackCue(lap, idle))
    }
}
