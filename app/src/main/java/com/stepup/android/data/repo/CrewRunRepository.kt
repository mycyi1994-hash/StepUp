package com.stepup.android.data.repo

import com.stepup.android.data.remote.CrewRunApi
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.domain.CrewRunOutcome
import com.stepup.android.domain.CrewRunRoom
import com.stepup.android.domain.CrewRunToday
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * 크루 러닝 3판(서버 0070) — 오늘 크루 러닝 · 방의 크루 체력 미리보기 · 끝난 크루 러닝의 결과를 들고 있다.
 * 크루 홈 · 대기실 · 달리는 중 · 결과가 함께 읽는다. 읽기가 실패하면 들고 있던 값을 그대로 둔다(서버가 0070 전이면 화면은 칸을 두지 않는다).
 * 계정이 바뀌면 앞사람 값을 지운다.
 */
class CrewRunRepository(
    private val api: CrewRunApi,
    private val owner: suspend () -> String,
) {
    private val todays = MutableStateFlow<Map<String, CrewRunToday>>(emptyMap())
    private val rooms = MutableStateFlow<Map<Long, CrewRunRoom>>(emptyMap())

    /** 시작 시각 → 결과(크루 러닝이 아니었으면 [NOT_CREW]) */
    private val outcomes = MutableStateFlow<Map<Long, OutcomeSlot>>(emptyMap())

    @Volatile private var holder: String? = null

    private suspend fun ownerNow() {
        val now = owner()
        val before = holder
        if (before != null && before != now) {
            todays.value = emptyMap()
            rooms.value = emptyMap()
            outcomes.value = emptyMap()
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

    /** 대기실에서 신발 고르기 — 서버가 받으면 방 미리보기를 다시 읽는다 */
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
        // 크루 러닝을 올렸으면 그 크루의 오늘 값도 바뀌었다
        slot.value?.mine?.crewId?.let { loadToday(it) }
        return slot
    }

    /** 읽은 결과 — 크루 러닝이 아니었으면 [value] 가 null */
    data class OutcomeSlot(val value: CrewRunOutcome?)
}
