package com.stepup.android.service

import android.util.AtomicFile
import com.stepup.android.domain.TrackPoint
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** A settling checkpoint must be reconciled with durable receipts, never replayed as a new run. */
enum class RunCheckpointPhase { RECORDING, SETTLING }

data class RunCheckpoint(
    val state: WalkSessionState,
    val goalKm: Double,
    val savedAt: Long,
    val phase: RunCheckpointPhase = RunCheckpointPhase.RECORDING,
) {
    init {
        require(state.isActive && state.startedAt > 0 && state.recordingOwner.isNotBlank())
        require(state.steps >= 0 && state.elapsedSec >= 0 && state.partySize >= 1)
        require(savedAt >= state.startedAt && goalKm.isFinite() && goalKm > 0)
        require(state.gpsKm.isFinite() && state.gpsKm >= 0)
        require(state.topSpeedKmh.isFinite() && state.topSpeedKmh >= 0)
        require(state.validSegments >= 0 && state.flaggedSegments >= 0)
        require(state.track.size <= 1_000_000 && state.laps.size <= 1_000_000)
        require(state.track.all { it.lat.isFinite() && it.lat in -90.0..90.0 &&
            it.lng.isFinite() && it.lng in -180.0..180.0 && it.at >= state.startedAt })
        require(state.laps.all { it.index > 0 && it.km.isFinite() && it.km >= 0 &&
            it.splitSec in 0..state.elapsedSec })
    }

    /** Downtime is neither exercise time nor distance. Recovery needs an explicit resume. */
    fun pausedForRecovery(): WalkSessionState {
        check(phase == RunCheckpointPhase.RECORDING) { "Settlement needs receipt reconciliation" }
        return state.copy(isPaused = true, gpsFix = false)
    }
}

/**
 * One application-owned instance per file. AtomicFile retains the previous committed
 * checkpoint on interrupted writes. Read errors propagate; damaged data is never
 * silently treated as 'no run' or overwritten by an automatic reset.
 * Service integration is gated on idempotent settlement/recovery, not enabled here.
 */
class RunCheckpointStore(file: File) {
    private val storage = AtomicFile(file)
    private val mutex = Mutex()

    suspend fun read(): RunCheckpoint? = withContext(Dispatchers.IO) {
        mutex.withLock { readLocked() }
    }

    suspend fun save(checkpoint: RunCheckpoint) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val existing = readLocked()
            require(existing == null || (existing.state.startedAt == checkpoint.state.startedAt &&
                existing.state.recordingOwner == checkpoint.state.recordingOwner)) {
                "An unresolved run cannot be replaced by another run or account"
            }
            require(existing == null || checkpoint.savedAt >= existing.savedAt) { "Stale checkpoint" }
            require(existing == null || (checkpoint.state.steps >= existing.state.steps &&
                checkpoint.state.elapsedSec >= existing.state.elapsedSec &&
                checkpoint.state.track.size >= existing.state.track.size)) { "Progress cannot move backwards" }
            require(existing?.phase != RunCheckpointPhase.SETTLING ||
                checkpoint.phase == RunCheckpointPhase.SETTLING) { "Cannot undo settlement boundary" }
            val output = storage.startWrite()
            try {
                val data = DataOutputStream(output)
                data.writeInt(VERSION)
                data.writeUTF(checkpoint.phase.name)
                data.writeLong(checkpoint.savedAt)
                data.writeDouble(checkpoint.goalKm)
                with(checkpoint.state) {
                    data.writeLong(startedAt)
                    data.writeUTF(recordingOwner)
                    data.writeBoolean(isPaused)
                    data.writeInt(partySize)
                    data.writeInt(steps)
                    data.writeLong(elapsedSec)
                    data.writeDouble(gpsKm)
                    data.writeDouble(topSpeedKmh)
                    data.writeInt(validSegments)
                    data.writeInt(flaggedSegments)
                    data.writeInt(track.size)
                    track.forEach { data.writeDouble(it.lat); data.writeDouble(it.lng); data.writeLong(it.at) }
                    data.writeInt(laps.size)
                    laps.forEach { data.writeInt(it.index); data.writeDouble(it.km); data.writeLong(it.splitSec) }
                }
                data.flush()
                storage.finishWrite(output)
            } catch (error: Throwable) {
                storage.failWrite(output)
                throw error
            }
        }
    }

    /** Call only after the same run's durable completion/discard has been acknowledged. */
    suspend fun clear(startedAt: Long, recordingOwner: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = readLocked() ?: return@withLock false
            if (current.state.startedAt != startedAt || current.state.recordingOwner != recordingOwner) {
                return@withLock false
            }
            storage.delete()
            if (storage.baseFile.exists() || File(storage.baseFile.path + ".bak").exists()) {
                throw IOException("Checkpoint could not be cleared")
            }
            true
        }
    }

    private fun readLocked(): RunCheckpoint? {
        if (!storage.baseFile.exists() && !File(storage.baseFile.path + ".bak").exists()) return null
        return DataInputStream(storage.openRead()).use { input ->
            if (input.readInt() != VERSION) throw IOException("Unsupported run checkpoint version")
            val phase = RunCheckpointPhase.valueOf(input.readUTF())
            val savedAt = input.readLong()
            val goal = input.readDouble()
            val state = WalkSessionState(
                isActive = true,
                startedAt = input.readLong(), recordingOwner = input.readUTF(),
                isPaused = input.readBoolean(), partySize = input.readInt(),
                steps = input.readInt(), elapsedSec = input.readLong(),
                gpsKm = input.readDouble(), topSpeedKmh = input.readDouble(),
                validSegments = input.readInt(), flaggedSegments = input.readInt(),
                track = List(input.count()) { TrackPoint(input.readDouble(), input.readDouble(), input.readLong()) },
                laps = List(input.count()) { RunLap(input.readInt(), input.readDouble(), input.readLong()) },
            )
            if (input.read() != -1) throw IOException("Unexpected checkpoint trailing data")
            RunCheckpoint(state, goal, savedAt, phase)
        }
    }

    private fun DataInputStream.count(): Int = readInt().also {
        if (it !in 0..1_000_000) throw IOException("Invalid checkpoint collection length")
    }

    private companion object { const val VERSION = 1 }
}
