package com.stepup.android.service

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
        require(phase == RunCheckpointPhase.SETTLING || state.saveStatus == RunSaveStatus.IDLE)
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
        return state.copy(isPaused = true, gpsFix = false, gpsLost = false)
    }
}
