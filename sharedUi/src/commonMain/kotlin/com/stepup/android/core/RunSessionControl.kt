package com.stepup.android.core

import com.stepup.android.domain.GeoPoint
import com.stepup.android.service.RunCheckpoint
import com.stepup.android.service.StartGroup
import com.stepup.android.service.WalkSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 러닝 한 번을 켜고 끄는 쪽(안드로이드 WalkSessionService 의 companion 과 같은 이름 · 같은 뜻, 2026-11-03 iOS C).
 * 러닝 화면(공용)은 이것만 부른다 — 안드로이드는 러닝 서비스, iOS 는 IosRunSession 이 구현한다.
 */
interface RunSessionControl {
    val state: StateFlow<WalkSessionState>
    val goalKm: MutableStateFlow<Double>
    val lastTrack: MutableStateFlow<List<GeoPoint>>
    val recovery: StateFlow<RunCheckpoint?>
    val discardFailures: StateFlow<Int>
    val startRefusals: StateFlow<Int>

    fun start(partySize: Int = 1, group: StartGroup = StartGroup.NONE)
    fun pause()
    fun resume()
    fun stop()
    fun discard()
    fun canDiscard(state: WalkSessionState): Boolean
    fun recover(finish: Boolean)
    suspend fun checkRecovery()
    fun clearLastTrack()
    fun recordManualLap()
    fun markSimulated()
    fun clearLastReward()
}

/** 러닝이 없는 판(화면 미리 보기 · 검사) — 아무것도 켜지 않는다 */
object NoRunSession : RunSessionControl {
    override val state = MutableStateFlow(WalkSessionState())
    override val goalKm = MutableStateFlow(5.0)
    override val lastTrack = MutableStateFlow<List<GeoPoint>>(emptyList())
    override val recovery = MutableStateFlow<RunCheckpoint?>(null)
    override val discardFailures = MutableStateFlow(0)
    override val startRefusals = MutableStateFlow(0)
    override fun start(partySize: Int, group: StartGroup) = Unit
    override fun pause() = Unit
    override fun resume() = Unit
    override fun stop() = Unit
    override fun discard() = Unit
    override fun canDiscard(state: WalkSessionState): Boolean = false
    override fun recover(finish: Boolean) = Unit
    override suspend fun checkRecovery() = Unit
    override fun clearLastTrack() = Unit
    override fun recordManualLap() = Unit
    override fun markSimulated() = Unit
    override fun clearLastReward() = Unit
}
