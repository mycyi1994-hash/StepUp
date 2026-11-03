package com.stepup.android.service

import com.stepup.android.core.RunSessionControl
import com.stepup.android.domain.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.StateFlow

/**
 * 공용 러닝 화면이 부르는 러닝 한 번(안드로이드 — 러닝 서비스 [WalkSessionService] 를 그대로 켜고 끈다, 2026-11-03 C8).
 * 값 · 규칙은 모두 서비스의 것이다. 여기서는 컨텍스트만 붙인다.
 */
object AndroidRunSession : RunSessionControl {
    private val context get() = com.stepup.android.core.AndroidHost.app

    override val state: StateFlow<WalkSessionState> get() = WalkSessionService.state
    override val goalKm: MutableStateFlow<Double> get() = WalkSessionService.goalKm
    override val lastTrack: MutableStateFlow<List<GeoPoint>> get() = WalkSessionService.lastTrack
    override val recovery: StateFlow<RunCheckpoint?> get() = WalkSessionService.recovery
    override val discardFailures: StateFlow<Int> get() = WalkSessionService.discardFailures
    override val startRefusals: StateFlow<Int> get() = WalkSessionService.startRefusals

    override fun start(partySize: Int, group: StartGroup) = WalkSessionService.start(context, partySize, group)
    override fun pause() = WalkSessionService.pause(context)
    override fun resume() = WalkSessionService.resume(context)
    override fun stop() = WalkSessionService.stop(context)
    override fun discard() = WalkSessionService.discard(context)
    override fun canDiscard(state: WalkSessionState): Boolean = WalkSessionService.canDiscard(state)
    override fun recover(finish: Boolean) = WalkSessionService.recover(context, finish)
    override suspend fun checkRecovery() = WalkSessionService.checkRecovery()
    override fun clearLastTrack() = WalkSessionService.clearLastTrack()
    override fun recordManualLap() = WalkSessionService.recordManualLap()
    override fun markSimulated() = WalkSessionService.markSimulated()
    override fun clearLastReward() = WalkSessionService.clearLastReward()

    /** 러닝 중인가 — 공용 화면이 본다(서비스 상태의 isActive 만 따로) */
    val active: StateFlow<Boolean> by lazy {
        WalkSessionService.state
            .map { it.isActive }
            .stateIn(
                CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
                SharingStarted.Eagerly,
                WalkSessionService.state.value.isActive,
            )
    }
}
