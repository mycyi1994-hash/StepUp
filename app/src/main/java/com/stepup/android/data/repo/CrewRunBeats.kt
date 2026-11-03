package com.stepup.android.data.repo

import android.content.Context
import com.stepup.android.domain.CrewRunLive
import com.stepup.android.domain.CrewRunState
import com.stepup.android.service.RunGroup
import com.stepup.android.service.WalkSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// 크루 러닝 상태 알림(안드로이드 러닝 서비스를 보는 쪽 — 예전 CrewRunRepository.startBeats 그대로)
private const val PREFS = "crew_run_beats"
private const val KEY_STARTED = "started_at"
private const val KEY_PARTY = "party_id"
private const val TICK_MS = 1_000L
private const val END_TRIES = 6
private const val END_RETRY_MS = 3_000L

/**
 * 달리는 동안 상태 알림 — 앱이 살아 있는 동안 한 번 건다. 크루 러닝(모임이 크루)이 켜져 있으면 5초마다 RUNNING · PAUSED,
 * 일시정지 · 다시 달리기는 바로. 러닝이 끝나면 ENDED 를 몇 번 다시 보낸다. 서버가 나를 끝냈으면(마지막 한 사람) 러닝을 멈춘다.
 * 방 번호는 출발한 방(크루 방 · 같은 크루)으로 적어 두어 앱을 다시 켜도 같은 러닝으로 이어 간다.
 */
fun CrewRunRepository.startBeats(
    scope: CoroutineScope,
    context: Context,
    party: StateFlow<PartyState>,
    run: StateFlow<WalkSessionState>,
    onCrewPool: (startedAt: Long, live: CrewRunLive) -> Unit,
    stopRun: () -> Unit,
) {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun remembered(startedAt: Long): Long? =
        prefs.getLong(KEY_PARTY, 0L).takeIf { it != 0L && prefs.getLong(KEY_STARTED, 0L) == startedAt }
    fun remember(startedAt: Long, partyId: Long) {
        if (remembered(startedAt) != partyId) prefs.edit().putLong(KEY_STARTED, startedAt).putLong(KEY_PARTY, partyId).apply()
    }
    scope.launch {
        var active: WalkSessionState? = null
        var lastBeatMs = 0L
        var lastPaused: Boolean? = null
        while (true) {
            val r = run.value
            val p = party.value
            if (r.isActive && r.group == RunGroup.CREW) {
                val fromRoom = p.partyId?.takeIf {
                    p.crewId == r.groupCrewId && (p.phase == PartyPhase.RUNNING || p.phase == PartyPhase.FINISHED)
                }
                val partyId = fromRoom ?: remembered(r.startedAt)
                if (partyId != null) {
                    remember(r.startedAt, partyId)
                    val now = System.currentTimeMillis()
                    if (now - lastBeatMs >= CrewRunRepository.BEAT_MS || lastPaused != r.isPaused) {
                        lastBeatMs = now
                        lastPaused = r.isPaused
                        val live = runCatching { beat(partyId, if (r.isPaused) CrewRunState.PAUSED else CrewRunState.RUNNING) }.getOrNull()
                        if (live != null) {
                            onCrewPool(r.startedAt, live)
                            // 다른 참가자가 모두 끝내 서버가 나를 끝냈다 — 이 폰의 러닝도 멈춘다(기록은 그대로 저장)
                            if (live.endedAlone) stopRun()
                        }
                    }
                }
                active = r
            } else {
                val ended = active
                if (ended != null) {
                    active = null
                    lastPaused = null
                    val partyId = remembered(ended.startedAt)
                    if (partyId != null) {
                        for (attempt in 0 until END_TRIES) {
                            if (runCatching { beat(partyId, CrewRunState.ENDED) }.getOrNull() != null) break
                            delay(END_RETRY_MS * (attempt + 1))
                        }
                    }
                }
            }
            delay(TICK_MS)
        }
    }
}

