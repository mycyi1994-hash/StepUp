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

    @Test fun crewGroupSurvivesReopen() = runBlocking {
        // 크루 달리기는 앱을 죽였다 살려도 크루 달리기다 — 혼자 출발했어도(인원 1) 그 크루 · 이름으로 결과 · 공유가 이어진다
        val file = file()
        try {
            val crew = checkpoint().let {
                it.copy(state = it.state.copy(partySize = 1, group = RunGroup.CREW, groupCrewId = "crew-1", groupName = "여의도 퇴근런"))
            }
            RunCheckpointStore(file).save(crew)
            val restored = RunCheckpointStore(file).read()!!
            assertEquals(crew, restored)
            val paused = restored.pausedForRecovery()
            assertEquals(RunGroup.CREW, paused.group)
            assertEquals("crew-1", paused.groupCrewId)
            assertEquals("여의도 퇴근런", paused.groupName)
        } finally { file.delete(); File(file.path + ".new").delete(); File(file.path + ".bak").delete() }
    }

    @Test fun olderCheckpointsKeepTheirGroup() = runBlocking {
        // 판 2(모임 칸 없음)에서 — 크루가 적힌 여럿 러닝은 크루 달리기, 크루 없는 여럿 러닝은 번개, 혼자는 모임 없음
        fun v2(partySize: Int, crewId: String): ByteArray {
            val bytes = java.io.ByteArrayOutputStream()
            java.io.DataOutputStream(bytes).apply {
                writeInt(2); writeUTF(RunCheckpointPhase.RECORDING.name); writeLong(500_000); writeDouble(5.0)
                writeLong(1_000); writeUTF("account:runner-a"); writeBoolean(false); writeInt(partySize)
                writeInt(725); writeLong(490); writeDouble(0.51); writeDouble(9.7); writeInt(42); writeInt(1)
                writeInt(0); writeInt(0)
                writeBoolean(false); writeUTF(crewId)
                flush()
            }
            return bytes.toByteArray()
        }
        val cases = listOf(
            Triple(3, "crew-1", RunGroup.CREW),
            Triple(3, "", RunGroup.FLASH),
            Triple(1, "", RunGroup.NONE),
        )
        for ((size, crewId, group) in cases) {
            val file = file()
            try {
                file.writeBytes(v2(size, crewId))
                val restored = RunCheckpointStore(file).read()!!
                assertEquals(group, restored.state.group)
                assertEquals(crewId, restored.state.groupCrewId)
                assertEquals(crewId, restored.state.partyCrewId)
                assertEquals(size, restored.state.partySize)
            } finally { file.delete(); File(file.path + ".new").delete(); File(file.path + ".bak").delete() }
        }
    }

    @Test fun simulatedFlagSurvivesReopen() = runBlocking {
        // 테스트 도구의 걸음 흉내를 쓴 러닝은 앱을 죽였다 살려도 흉내 표시가 남는다(판 4) — 되살린 러닝이 적립 · 업로드되지 않게
        val file = file()
        try {
            val simulated = checkpoint().let { it.copy(state = it.state.copy(simulated = true)) }
            RunCheckpointStore(file).save(simulated)
            val restored = RunCheckpointStore(file).read()!!
            assertEquals(simulated, restored)
            assertTrue(restored.pausedForRecovery().simulated)
            assertEquals(com.stepup.android.domain.RunVerdict.VOID, restored.pausedForRecovery().liveVerdict)
        } finally { file.delete(); File(file.path + ".new").delete(); File(file.path + ".bak").delete() }
    }

    @Test fun version3CheckpointKeepsGroupWithoutSimulatedFlag() = runBlocking {
        // 판 3(흉내 표시 칸 없음)에서 — 모임은 그대로 읽고, 흉내 표시는 없었던 것으로 본다
        val file = file()
        try {
            val bytes = java.io.ByteArrayOutputStream()
            java.io.DataOutputStream(bytes).apply {
                writeInt(3); writeUTF(RunCheckpointPhase.RECORDING.name); writeLong(500_000); writeDouble(5.0)
                writeLong(1_000); writeUTF("account:runner-a"); writeBoolean(false); writeInt(1)
                writeInt(725); writeLong(490); writeDouble(0.51); writeDouble(9.7); writeInt(42); writeInt(1)
                writeInt(0); writeInt(0)
                writeBoolean(false); writeUTF("")
                writeUTF(RunGroup.CREW.name); writeUTF("crew-1"); writeUTF("여의도 퇴근런")
                flush()
            }
            file.writeBytes(bytes.toByteArray())
            val restored = RunCheckpointStore(file).read()!!
            assertEquals(RunGroup.CREW, restored.state.group)
            assertEquals("crew-1", restored.state.groupCrewId)
            assertEquals("여의도 퇴근런", restored.state.groupName)
            assertFalse(restored.state.simulated)
        } finally { file.delete(); File(file.path + ".new").delete(); File(file.path + ".bak").delete() }
    }

    @Test fun fakeLocationFlagSurvivesReopen() = runBlocking {
        // 가짜 위치를 본 러닝은 앱을 죽였다 살려도 무효 표시가 남아야 한다
        val file = file()
        try {
            val flagged = checkpoint().let { it.copy(state = it.state.copy(mockLocation = true)) }
            RunCheckpointStore(file).save(flagged)
            val restored = RunCheckpointStore(file).read()!!
            assertTrue(restored.state.mockLocation)
            assertTrue(restored.pausedForRecovery().mockLocation)
        } finally { file.delete(); File(file.path + ".new").delete(); File(file.path + ".bak").delete() }
    }
}
