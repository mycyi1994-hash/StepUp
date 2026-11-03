package com.stepup.android.data.repo

import com.stepup.android.data.remote.CrewRunApi
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.domain.CrewRunLive
import com.stepup.android.domain.CrewRunOutcome
import com.stepup.android.domain.CrewRunRoom
import com.stepup.android.domain.CrewRunState
import com.stepup.android.domain.CrewRunToday
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

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
    @kotlin.concurrent.Volatile var liveAtMs: Long = 0L
        private set

    @kotlin.concurrent.Volatile private var holder: String? = null

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
        liveAtMs = com.stepup.android.domain.epochMillis()
        _live.value = live
        return live
    }

    companion object {
        /** 상태를 알리는 간격 — 서버는 30초 넘게 소식이 없으면 쓰지 않는 것으로 본다 */
        const val BEAT_MS = 5_000L
    }
}
