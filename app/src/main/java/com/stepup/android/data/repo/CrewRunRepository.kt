package com.stepup.android.data.repo

import android.content.Context
import android.content.SharedPreferences
import com.stepup.android.data.remote.CrewRunApi
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.domain.CrewAlone
import com.stepup.android.domain.CrewAloneGrace
import com.stepup.android.domain.CrewEmptySteps
import com.stepup.android.domain.CrewEndRetry
import com.stepup.android.domain.CrewRunLive
import com.stepup.android.domain.CrewRunOutcome
import com.stepup.android.domain.CrewRunRoom
import com.stepup.android.domain.CrewRunState
import com.stepup.android.domain.CrewRunToday
import com.stepup.android.service.RunGroup
import com.stepup.android.service.WalkSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 크루 러닝 상태 알림이 적어 두는 작은 값들(출발한 방 번호 · 공용 체력이 0 이던 동안의 걸음 · 아직 닿지 않은 "끝냄") —
 * 앱이 꺼졌다 켜져도 잇는다. 앱은 SharedPreferences 에, 검사는 메모리 사본에 둔다.
 */
interface CrewBeatStore {
    /** 적힌 값 — 없으면 null */
    fun get(key: String): Long?

    /** 한꺼번에 적는다 — 값이 null 이면 지운다 */
    fun put(vararg values: Pair<String, Long?>)
}

private class PrefsBeatStore(private val prefs: SharedPreferences) : CrewBeatStore {
    override fun get(key: String): Long? = if (prefs.contains(key)) runCatching { prefs.getLong(key, 0L) }.getOrNull() else null

    override fun put(vararg values: Pair<String, Long?>) {
        val edit = prefs.edit()
        values.forEach { (key, value) -> if (value == null) edit.remove(key) else edit.putLong(key, value) }
        edit.apply()
    }
}

/**
 * 크루 러닝(서버 0071) — 오늘 크루 러닝 · 대기실 · 달리는 중(공용 체력 · 인원 판정) · 결과를 들고 있다.
 *
 * 달리는 동안 5초마다(일시정지 · 다시 달리기는 바로) 서버에 내 상태를 알린다([startBeats] — `crew_run_beat`). 서버가 그때 공용 체력을
 * 정산하고, 다른 참가자가 모두 끝냈으면 나를 끝낸다 — 그러면 크게 알리고 1분 뒤(서버가 적립하는 데까지) 이 폰의 러닝도 멈춘다([alone]).
 * 러닝이 끝나면 "끝냄"을 닿을 때까지 보낸다([noteEnded]). 공용 체력이 0 이던 동안의 걸음도 여기서 센다([emptySteps]).
 * 읽기가 실패하면 들고 있던 값을 그대로 둔다(0 이나 참여 완료로 바꾸지 않는다). 계정이 바뀌면 앞사람 값을 지운다.
 */
class CrewRunRepository(
    private val api: CrewRunApi,
    private val owner: suspend () -> String,
    /**
     * 부팅 기준 시계(ms) — 상태를 알리는 간격 · 마지막 한 사람 대기 · 표시 보간을 잰다. 폰 시계(벽시계)로 재면 시계를 뒤로 돌린 동안
     * 상태 알림이 멈췄다(2026-11-18).
     */
    private val elapsedMs: () -> Long = { android.os.SystemClock.elapsedRealtime() },
) {
    private val todays = MutableStateFlow<Map<String, CrewRunToday>>(emptyMap())
    private val rooms = MutableStateFlow<Map<Long, CrewRunRoom>>(emptyMap())
    private val outcomes = MutableStateFlow<Map<Long, OutcomeSlot>>(emptyMap())
    private val _live = MutableStateFlow<CrewRunLive?>(null)

    /** 지금(또는 방금) 달린 크루 러닝의 서버 상태 — 크루 러닝이 아니면 null */
    val live: StateFlow<CrewRunLive?> = _live

    /** 마지막으로 서버 상태를 받은 때(부팅 기준 시계) */
    @Volatile private var liveAtElapsed: Long = 0L

    /** 마지막으로 서버 상태를 받은 뒤 흐른 시간(ms) — 화면이 공용 체력을 부드럽게 보간하고, 2분 확인까지 남은 시간을 센다 */
    fun sinceLiveMs(): Long = (elapsedMs() - liveAtElapsed).coerceAtLeast(0L)

    private val _alone = MutableStateFlow<CrewAloneGrace?>(null)

    /** 마지막 한 사람이 되어 곧 멈출 러닝 — 화면이 "N초 뒤 크루 러닝이 끝나요"를 센다. 그런 러닝이 없으면 null */
    val alone: StateFlow<CrewAloneGrace?> = _alone

    private val _emptySteps = MutableStateFlow<CrewEmptySteps?>(null)

    /** 공용 체력이 0 이던 동안의 걸음(러닝마다) — 예상 보상에서 뺀다. 아직 모르면 null */
    val emptySteps: StateFlow<CrewEmptySteps?> = _emptySteps

    private val _notCrew = MutableStateFlow(0L)

    /** 서버가 "이 방에 내 크루 러닝 참여가 없다"고 답한 러닝(시작 시각) — 없으면 0. 답을 받기 전에는 크루 러닝으로 본다 */
    val notCrewRun: StateFlow<Long> = _notCrew

    @Volatile private var holder: String? = null

    private suspend fun ownerNow() {
        val now = owner()
        val before = holder
        if (before != null && before != now) {
            todays.value = emptyMap()
            rooms.value = emptyMap()
            outcomes.value = emptyMap()
            _live.value = null
            _alone.value = null
            _emptySteps.value = null
            _notCrew.value = 0L
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

    /** 한 번 알린 답 — 닿지 못한 것과 "서버가 이 참여를 모른다"를 가른다(끝냄을 다시 보낼지가 여기서 갈린다) */
    sealed interface BeatReply {
        data class Live(val live: CrewRunLive) : BeatReply

        /** 서버에 닿았고, 이 방에 내 크루 러닝 참여가 없다고 했다 */
        data object NotCrew : BeatReply

        /** 닿지 못했다 — 다시 보낸다 */
        data object Failed : BeatReply
    }

    /**
     * 한 번 상태를 알린다 — 받은 값을 [live] 에 둔다. 닿지 못하면 들고 있던 값을 둔다.
     * [publish] 가 false 면(한참 뒤에 다시 보내는 "끝냄") 같은 방의 값을 들고 있을 때만 바꾼다 — 그사이 시작한 다른 러닝의 값을 덮지 않게.
     */
    internal suspend fun beatReply(partyId: Long, state: CrewRunState, publish: Boolean = true): BeatReply {
        ownerNow()
        val result = api.beat(partyId, state) as? ServerResult.Ok ?: return BeatReply.Failed
        val live = result.value.toDomain()
        val current = _live.value
        if (publish || current == null || current.partyId == partyId) {
            liveAtElapsed = elapsedMs()
            _live.value = live
        }
        return if (live == null) BeatReply.NotCrew else BeatReply.Live(live)
    }

    /** 한 번 상태를 알린다 — 받은 값. 닿지 못했거나 크루 러닝이 아니면 null */
    suspend fun beat(partyId: Long, state: CrewRunState): CrewRunLive? =
        (beatReply(partyId, state) as? BeatReply.Live)?.live

    @Volatile private var store: CrewBeatStore? = null
    @Volatile private var beatScope: CoroutineScope? = null
    @Volatile private var runNow: (() -> WalkSessionState)? = null
    private val endLock = Any()
    private var endJob: Job? = null

    /**
     * 달리는 동안 상태 알림 — 앱이 살아 있는 동안 한 번 건다. 크루 러닝(모임이 크루)이 켜져 있으면 5초마다 RUNNING · PAUSED,
     * 일시정지 · 다시 달리기는 바로. 방 번호는 출발한 방(크루 방 · 같은 크루)으로 적어 두어 앱을 다시 켜도 같은 러닝으로 이어 간다.
     *
     * 서버가 나를 끝냈으면(마지막 한 사람) 바로 멈추지 않는다 — [onAlone] 으로 크게 알리고(소리 · 진동 · 알림) 서버가 적립하는 데까지
     * (끝난 때 + 60초) 기록한 뒤 [stopRun]. 그렇게 멈춘 러닝이 저장되면 [onAloneSaved], 그 전에 사용자가 마쳤으면 [onAloneCleared].
     * 공용 체력이 바뀌거나 비면 [onCrewPool](잠금화면 알림). 러닝이 끝나면 "끝냄"을 닿을 때까지 보낸다([noteEnded]) —
     * [networkBack] 이 오면(인터넷이 다시 됐다) 바로 다시.
     */
    fun startBeats(
        scope: CoroutineScope,
        context: Context,
        party: StateFlow<PartyState>,
        run: StateFlow<WalkSessionState>,
        onCrewPool: (startedAt: Long, live: CrewRunLive, empty: Boolean) -> Unit,
        stopRun: () -> Unit,
        onAlone: (leftSec: Int) -> Unit = {},
        onAloneSaved: () -> Unit = {},
        onAloneCleared: () -> Unit = {},
        networkBack: Flow<Int> = emptyFlow(),
    ) = startBeats(
        scope, PrefsBeatStore(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)), party, run, onCrewPool, stopRun,
        onAlone, onAloneSaved, onAloneCleared, networkBack,
    )

    /** [startBeats] — 적어 두는 곳([store])과 보는 간격([tickMs])을 받는다(검사는 메모리 사본 · 짧은 간격을 쓴다) */
    internal fun startBeats(
        scope: CoroutineScope,
        store: CrewBeatStore,
        party: StateFlow<PartyState>,
        run: StateFlow<WalkSessionState>,
        onCrewPool: (startedAt: Long, live: CrewRunLive, empty: Boolean) -> Unit,
        stopRun: () -> Unit,
        onAlone: (leftSec: Int) -> Unit = {},
        onAloneSaved: () -> Unit = {},
        onAloneCleared: () -> Unit = {},
        networkBack: Flow<Int> = emptyFlow(),
        tickMs: Long = TICK_MS,
    ) {
        this.store = store
        this.beatScope = scope
        this.runNow = { run.value }
        fun remembered(startedAt: Long): Long? = store.partyOf(startedAt)
        fun remember(startedAt: Long, partyId: Long) {
            if (remembered(startedAt) != partyId) store.put(KEY_STARTED to startedAt, KEY_PARTY to partyId)
        }
        // 앱을 다시 켰다 — 적어 둔 빈 구간 걸음(되살린 러닝이 이어 쓴다)과 못 보낸 "끝냄"을 잇는다
        store.get(KEY_EMPTY_STARTED)?.takeIf { it != 0L }?.let { startedAt ->
            _emptySteps.value = CrewEmptySteps(
                startedAt, (store.get(KEY_EMPTY_EXCLUDED) ?: 0L).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
                store.get(KEY_EMPTY_FROM)?.takeIf { it in 0L..Int.MAX_VALUE.toLong() }?.toInt(),
            )
        }
        resumeEnd()
        scope.launch { networkBack.drop(1).collect { resumeEnd() } }
        scope.launch {
            var active: WalkSessionState? = null
            var lastBeatAt: Long? = null
            var lastPaused: Boolean? = null
            var lastEmpty: Boolean? = null
            // 마지막 한 사람 대기가 끝나 이 일이 멈춘 러닝 · 마지막으로 멈추라고 한 때(저장이 실패하면 몇 초마다 다시)
            var forcedStop = 0L
            var lastStopAt: Long? = null
            while (true) {
                val r = run.value
                val p = party.value
                if (r.isActive && r.group == RunGroup.CREW) {
                    if (active?.startedAt != r.startedAt) {
                        lastBeatAt = null
                        lastPaused = null
                        lastEmpty = null
                        lastStopAt = null
                    }
                    val fromRoom = p.partyId?.takeIf {
                        p.crewId == r.groupCrewId && (p.phase == PartyPhase.RUNNING || p.phase == PartyPhase.FINISHED)
                    }
                    val partyId = fromRoom ?: remembered(r.startedAt)
                    if (partyId != null) {
                        remember(r.startedAt, partyId)
                        val now = elapsedMs()
                        val last = lastBeatAt
                        var heard = false
                        if (last == null || now - last >= BEAT_MS || lastPaused != r.isPaused) {
                            lastBeatAt = now
                            lastPaused = r.isPaused
                            val reply = runCatching {
                                beatReply(partyId, if (r.isPaused) CrewRunState.PAUSED else CrewRunState.RUNNING)
                            }.getOrDefault(BeatReply.Failed)
                            when (reply) {
                                is BeatReply.Live -> {
                                    heard = true
                                    if (_notCrew.value == r.startedAt) _notCrew.value = 0L
                                    // 다른 참가자가 모두 끝내 서버가 나를 끝냈다 — 처음 안 때 한 번 알리고, 적립하는 데까지 기록한다
                                    if (reply.live.endedAlone && _alone.value?.startedAt != r.startedAt) {
                                        val left = CrewAlone.leftMs(reply.live.rewardUntilMs, reply.live.serverNowMs)
                                        _alone.value = CrewAloneGrace(r.startedAt, elapsedMs() + left)
                                        if (left > 0L) runCatching { onAlone(CrewAlone.seconds(left)) }
                                    }
                                }
                                BeatReply.NotCrew -> _notCrew.value = r.startedAt
                                BeatReply.Failed -> Unit
                            }
                        }
                        // 공용 체력이 0 인 동안의 걸음을 센다 — 1초마다, 화면과 상관없이. 받은 뒤 흐른 시간만큼 보간해 0 을 지났는지도 본다
                        _live.value?.takeIf { it.partyId == partyId }?.let { live ->
                            val empty = live.poolEmptyAfter(sinceLiveMs())
                            val before = _emptySteps.value?.takeIf { it.startedAt == r.startedAt } ?: CrewEmptySteps(r.startedAt)
                            val next = before.next(r.steps, empty)
                            if (next != _emptySteps.value) {
                                _emptySteps.value = next
                                store.put(
                                    KEY_EMPTY_STARTED to next.startedAt, KEY_EMPTY_EXCLUDED to next.excluded.toLong(),
                                    KEY_EMPTY_FROM to next.emptyFrom?.toLong(),
                                )
                            }
                            if (heard || lastEmpty != empty) {
                                lastEmpty = empty
                                runCatching { onCrewPool(r.startedAt, live, empty) }
                            }
                        }
                    }
                    // 마지막 한 사람 대기가 끝났다 — 러닝을 멈춘다(기록은 그대로 저장). 저장이 실패해 러닝이 남아 있으면 몇 초마다 다시
                    val grace = _alone.value
                    if (grace != null && grace.startedAt == r.startedAt && elapsedMs() >= grace.stopAtElapsedMs) {
                        val now = elapsedMs()
                        val last = lastStopAt
                        if (last == null || now - last >= BEAT_MS) {
                            lastStopAt = now
                            forcedStop = r.startedAt
                            runCatching { stopRun() }
                        }
                    }
                    active = r
                } else {
                    val ended = active
                    if (ended != null) {
                        active = null
                        lastPaused = null
                        lastBeatAt = null
                        noteEnded(ended.startedAt)
                        // 마지막 한 사람으로 알린 러닝 — 그 대기가 끝나 멈춘 러닝이 저장됐으면 그렇다고 알리고, 사용자가 먼저 마쳤으면 알림만 거둔다
                        if (_alone.value?.startedAt == ended.startedAt) {
                            val saved = !r.isActive && r.lastStartedAt == ended.startedAt && r.lastRewardPoints != null
                            runCatching { if (saved && forcedStop == ended.startedAt) onAloneSaved() else onAloneCleared() }
                        }
                    }
                    // 달리는 모습을 보지 못한 러닝(되살리자마자 끝낸 러닝)도 "끝냄"을 보낸다 — 방금 끝난 러닝의 결과가 남아 있는 동안
                    if (!r.isActive && r.lastGroup == RunGroup.CREW && r.lastStartedAt > 0L && r.lastRewardPoints != null) {
                        noteEnded(r.lastStartedAt)
                    }
                }
                delay(tickMs)
            }
        }
    }

    /**
     * 이 크루 러닝(시작 시각)이 끝났다 — 서버에 "끝냄"을 닿을 때까지 보낸다. 러닝 서비스(저장을 마친 때)와 상태 알림이 함께 부른다 —
     * 같은 러닝으로 여러 번 불러도 한 번만 시작한다. 이 폰이 상태를 알린 적 없는 러닝(방 번호를 모른다)은 보낼 곳이 없다.
     * 적어 두고 보내므로 앱이 꺼졌다 켜져도 이어 보낸다.
     */
    fun noteEnded(startedAt: Long) {
        val store = store ?: return
        // 러닝 서비스와 상태 알림이 동시에 불러도 한 번만 적고 한 번만 시작한다
        synchronized(endLock) {
            if (startedAt <= 0L || store.get(KEY_END_DONE) == startedAt) return
            if (store.get(KEY_END_STARTED) == startedAt) return
            val partyId = store.partyOf(startedAt) ?: return
            store.put(KEY_END_STARTED to startedAt, KEY_END_PARTY to partyId, KEY_END_SINCE to System.currentTimeMillis())
            resumeEnd()
        }
    }

    /** 못 보낸 "끝냄"이 있으면 지금부터 다시 보낸다 — 기다리던 중이었으면 기다림을 버리고 바로(인터넷이 다시 됐다) */
    private fun resumeEnd() {
        val scope = beatScope ?: return
        val store = store ?: return
        if ((store.get(KEY_END_STARTED) ?: 0L) == 0L) return
        synchronized(endLock) {
            endJob?.cancel()
            endJob = scope.launch { sendEnd(store) }
        }
    }

    private suspend fun sendEnd(store: CrewBeatStore) {
        val began = elapsedMs()
        var attempt = 0
        while (true) {
            val startedAt = store.get(KEY_END_STARTED) ?: 0L
            val partyId = store.get(KEY_END_PARTY) ?: 0L
            if (startedAt == 0L || partyId == 0L) return
            // 그 러닝이 아직 켜져 있으면(저장 실패로 남았다) 끝냄을 보내지 않는다
            if (runNow?.invoke()?.let { it.isActive && it.startedAt == startedAt } == true) return
            // 오래된 것은 버린다 — 러닝을 올리면 서버가 그 참여를 닫는다
            if (System.currentTimeMillis() - (store.get(KEY_END_SINCE) ?: 0L) > CrewEndRetry.KEEP_MS) {
                clearEnd(store, startedAt)
                return
            }
            val reply = runCatching { beatReply(partyId, CrewRunState.ENDED, publish = false) }.getOrDefault(BeatReply.Failed)
            if (reply !is BeatReply.Failed) {
                clearEnd(store, startedAt)
                return
            }
            // 이번에는 여기까지 — 인터넷이 다시 되거나 앱을 다시 켜면 이어 보낸다
            if (CrewEndRetry.over(elapsedMs() - began, attempt)) return
            delay(CrewEndRetry.delayMs(attempt))
            attempt++
        }
    }

    private fun clearEnd(store: CrewBeatStore, startedAt: Long) {
        // 그사이 다른 러닝의 끝냄이 적혔으면 그것은 두고 간다
        if (store.get(KEY_END_STARTED) != startedAt) return
        // 닿았든 버렸든(24시간) 이 러닝은 끝냈다고 적는다 — 버린 끝냄을 상태 알림이 다시 걸어 하루마다 10분씩 보내지 않게(2026-11-18 검토)
        store.put(KEY_END_STARTED to null, KEY_END_PARTY to null, KEY_END_SINCE to null, KEY_END_DONE to startedAt)
    }

    /** 이 러닝(시작 시각)이 상태를 알리던 방 — 적어 둔 것이 그 러닝의 것일 때만 */
    private fun CrewBeatStore.partyOf(startedAt: Long): Long? =
        get(KEY_PARTY)?.takeIf { it != 0L && get(KEY_STARTED) == startedAt }

    companion object {
        private const val PREFS = "crew_run_beats"
        private const val KEY_STARTED = "started_at"
        private const val KEY_PARTY = "party_id"

        /** 공용 체력이 0 이던 동안의 걸음 — 러닝(시작 시각) · 지난 빈 구간의 합 · 지금 빈 구간이 시작한 걸음(없으면 -1) */
        private const val KEY_EMPTY_STARTED = "empty_started_at"
        private const val KEY_EMPTY_EXCLUDED = "empty_excluded"
        private const val KEY_EMPTY_FROM = "empty_from"

        /** 아직 닿지 않은 "끝냄" — 러닝(시작 시각) · 방 · 끝난 때(폰 시계), 그리고 마지막으로 닿은 러닝 */
        private const val KEY_END_STARTED = "end_started_at"
        private const val KEY_END_PARTY = "end_party_id"
        private const val KEY_END_SINCE = "end_since"
        private const val KEY_END_DONE = "end_done_started_at"

        /** 상태를 알리는 간격 — 서버는 30초 넘게 소식이 없으면 쓰지 않는 것으로 본다 */
        const val BEAT_MS = 5_000L
        private const val TICK_MS = 1_000L
    }
}
