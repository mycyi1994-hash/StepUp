package com.stepup.android

import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.domain.TrackPoint
import com.stepup.android.service.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RunCheckpointPersistenceTest {
    private fun file() = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
        "run-checkpoint-${UUID.randomUUID()}.bin")

    private fun checkpoint() = RunCheckpoint(
        state = WalkSessionState(
            isActive = true, startedAt = 1_000, recordingOwner = "account:runner-a",
            steps = 725, elapsedSec = 490, partySize = 2,
            gpsKm = 0.51, topSpeedKmh = 9.7, validSegments = 42, flaggedSegments = 1,
            laps = listOf(RunLap(1, 0.5, 480)),
            track = listOf(TrackPoint(37.512345678, 127.023456789, 1_000),
                TrackPoint(37.512456789, 127.023456789, 4_000)),
        ), goalKm = 5.0, savedAt = 500_000,
    )

    @Test fun reopenPreservesOwnerRouteLapsAndOnlyRecordedTime() = runBlocking {
        val file = file()
        val expected = checkpoint()
        try {
            RunCheckpointStore(file).save(expected)
            val restored = RunCheckpointStore(file).read()!!
            assertEquals(expected, restored)
            val paused = restored.pausedForRecovery()
            assertTrue(paused.isPaused)
            assertFalse(paused.gpsFix)
            assertEquals(expected.state.elapsedSec, paused.elapsedSec)
            assertEquals(expected.state.recordingOwner, paused.recordingOwner)
            // A settings/account change cannot replace or clear another runner's unfinished data.
            val reopened = RunCheckpointStore(file)
            assertFalse(reopened.clear(expected.state.startedAt, "account:runner-b"))
            var rejected = false
            try {
                reopened.save(expected.copy(state = expected.state.copy(recordingOwner = "account:runner-b")))
            } catch (_: IllegalArgumentException) { rejected = true }
            assertTrue(rejected)
            assertEquals(expected, reopened.read())
            assertTrue(reopened.clear(expected.state.startedAt, expected.state.recordingOwner))
            assertNull(RunCheckpointStore(file).read())
        } finally { file.delete(); File(file.path + ".new").delete(); File(file.path + ".bak").delete() }
    }

    @Test fun interruptedWriteAndCorruptReadDoNotDiscardCommittedData() = runBlocking {
        val file = file()
        try {
            val expected = checkpoint()
            RunCheckpointStore(file).save(expected)
            // Android AtomicFile writes .new before replacing the committed base file.
            File(file.path + ".new").writeBytes(byteArrayOf(1, 2, 3))
            assertEquals(expected, RunCheckpointStore(file).read())
            assertFalse(File(file.path + ".new").exists())
            val broken = byteArrayOf(0, 0, 0, 99)
            file.writeBytes(broken)
            var readFailed = false
            try { RunCheckpointStore(file).read() } catch (_: java.io.IOException) { readFailed = true }
            assertTrue(readFailed)
            var saveFailed = false
            try { RunCheckpointStore(file).save(expected) } catch (_: java.io.IOException) { saveFailed = true }
            assertTrue(saveFailed)
            assertArrayEquals(broken, file.readBytes())
        } finally { file.delete(); File(file.path + ".new").delete(); File(file.path + ".bak").delete() }
    }

    @Test fun settlementBoundarySurvivesReopenAndCannotBecomeRunningAgain() = runBlocking {
        val file = file()
        try {
            val recording = checkpoint()
            val settling = recording.copy(phase = RunCheckpointPhase.SETTLING, savedAt = 501_000)
            RunCheckpointStore(file).save(recording)
            RunCheckpointStore(file).save(settling)
            val restored = RunCheckpointStore(file).read()!!
            assertEquals(settling, restored)
            var resumeRejected = false
            try { restored.pausedForRecovery() } catch (_: IllegalStateException) { resumeRejected = true }
            assertTrue(resumeRejected)
            var rollbackRejected = false
            try {
                RunCheckpointStore(file).save(recording.copy(savedAt = 502_000))
            } catch (_: IllegalArgumentException) { rollbackRejected = true }
            assertTrue(rollbackRejected)
            assertEquals(settling, RunCheckpointStore(file).read())
        } finally { file.delete(); File(file.path + ".new").delete(); File(file.path + ".bak").delete() }
    }
}
