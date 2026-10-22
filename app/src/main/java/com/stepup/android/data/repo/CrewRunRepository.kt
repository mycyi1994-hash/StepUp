package com.stepup.android.data.repo

import android.content.Context
import com.stepup.android.data.remote.CrewRunApi
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.domain.CrewRunLive
import com.stepup.android.domain.CrewRunOutcome
import com.stepup.android.domain.CrewRunRoom
import com.stepup.android.domain.CrewRunState
import com.stepup.android.domain.CrewRunToday
import com.stepup.android.service.RunGroup
import com.stepup.android.service.WalkSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 크루 러닝(서버 0071) — 오늘 크루 러닝 · 대기실 · 달리는 중(공용 체력 · 인원 판정) · 결과를 들고 있다.
 *
 * 달리는 동안 5초마다(일시정지 · 다시 달리기는 바로) 서버에 내 상태를 알린다([startBeats] — `crew_run_beat`). 서버가 그때 공용 체력을
 * 정산하고, 다른 참가자가 모두 끝냈으면 나를 끝낸다 — 그러면 이 폰의 러닝도 멈춘다. 러닝이 끝나면 "끝냄"을 보낸다.
 * 읽기가 실패하면 들고 있던 값을 그대로 둔다(0 이나 참여 완료로 바꾸지 않는다). 계정이 바뀌면 앞사람 값을 지운다.
 */
class CrewRunRepository(
    private val api: CrewRunApi,
    private val owner: suspend () -> String,
) {
    private val todays = MutableStateFlow<Map<String, CrewRunToday>>(emptyMap())
    private val rooms = MutableStateFlow<Map<Long, CrewRunRoom>>(emptyMap())
    private val outcomes = MutableStateFlow<Map<Long, OutcomeSlot>>(emptyMap())
    private val _live = MutableStateFlow<CrewRunLive?>(null)

    /** 지금(또는 방금) 달린 크루 러닝의 서버 상태 — 크루 러닝이 아니면 null */
    val live: StateFlow<CrewRunLive?> = _live

    /** 마지막으로 서버 상태를 받은 때(벽시계) — 화면이 공용 체력을 부드럽게 보간한다 */
    @Volatile var liveAtMs: Long = 0L
        private set

    @Volatile private var holder: String? = null

    private suspend fun ownerNow() {
        val now = owner()
        val before = holder
        if (before != null && before != now) {
            todays.value = emptyMap()
            rooms.value = emptyMap()
            outcomes.value = emptyMap()
            _live.value = null
        }
        holder = now
    }

    fun todayNow(crewId: String): CrewRunToday? = todays.value[crewId]

    fun today(crewId: String): Flow<CrewRunToday?> = todays.map { it[crewId] }.distinctUntilChanged()

    suspend fun loadToday(crewId: String): CrewRunToday? {
        ownerNow()
        val value = (api.today(crewId) as? ServerResult.Ok)?.value?.toDomain(crewId) ?: return null
        todays.update { it + (crewId to value) }
        return value
    }

    fun roomNow(partyId: Long): CrewRunRoom? = rooms.value[partyId]

    fun room(partyId: Long): Flow<CrewRunRoom?> = rooms.map { it[partyId] }.distinctUntilChanged()

    suspend fun loadRoom(partyId: Long): CrewRunRoom? {
        ownerNow()
        val value = (api.room(partyId) as? ServerResult.Ok)?.value?.toDomain() ?: return null
        rooms.update { it + (partyId to value) }
        return value
    }

    /** 대기실에서 신발 고르기 — 서버가 받으면 대기실을 다시 읽는다 */
    suspend fun pickShoe(partyId: Long, sneakerId: Long?): Boolean {
        ownerNow()
        val ok = api.pickShoe(partyId, sneakerId) is ServerResult.Ok
        if (ok) loadRoom(partyId)
        return ok
    }

    /** 결과 — 아직 읽지 않았으면 null, 크루 러닝이 아니었으면 [OutcomeSlot.value] 가 null */
    fun outcome(startedAt: Long): Flow<OutcomeSlot?> = outcomes.map { it[startedAt] }.distinctUntilChanged()

    suspend fun loadOutcome(startedAt: Long): OutcomeSlot? {
        ownerNow()
        val result = api.result(startedAt) as? ServerResult.Ok ?: return null
        val slot = OutcomeSlot(result.value.value?.toDomain())
        outcomes.update { it + (startedAt to slot) }
        slot.value?.crewId?.let { loadToday(it) }
        return slot
    }

    /** 읽은 결과 — 크루 러닝이 아니었으면 [value] 가 null */
    data class OutcomeSlot(val value: CrewRunOutcome?)

    /** 한 번 상태를 알린다 — 받은 값을 [live] 에 둔다. 실패하면 들고 있던 값을 둔다 */
    suspend fun beat(partyId: Long, state: CrewRunState): CrewRunLive? {
        ownerNow()
        val result = api.beat(partyId, state) as? ServerResult.Ok ?: return null
        val live = result.value.toDomain()
        liveAtMs = System.currentTimeMillis()
        _live.value = live
        return live
    }

    /**
     * 달리는 동안 상태 알림 — 앱이 살아 있는 동안 한 번 건다. 크루 러닝(모임이 크루)이 켜져 있으면 5초마다 RUNNING · PAUSED,
     * 일시정지 · 다시 달리기는 바로. 러닝이 끝나면 ENDED 를 몇 번 다시 보낸다. 서버가 나를 끝냈으면(마지막 한 사람) 러닝을 멈춘다.
     * 방 번호는 출발한 방(크루 방 · 같은 크루)으로 적어 두어 앱을 다시 켜도 같은 러닝으로 이어 간다.
     */
    fun startBeats(
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
                        if (now - lastBeatMs >= BEAT_MS || lastPaused != r.isPaused) {
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

    companion object {
        private const val PREFS = "crew_run_beats"
        private const val KEY_STARTED = "started_at"
        private const val KEY_PARTY = "party_id"

        /** 상태를 알리는 간격 — 서버는 30초 넘게 소식이 없으면 쓰지 않는 것으로 본다 */
        const val BEAT_MS = 5_000L
        private const val TICK_MS = 1_000L
        private const val END_TRIES = 6
        private const val END_RETRY_MS = 3_000L
    }
}
