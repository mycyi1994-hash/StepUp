package com.stepup.android.data.repo

import androidx.annotation.VisibleForTesting
import com.stepup.android.data.local.CrewDao
import com.stepup.android.data.local.CrewInfoDao
import com.stepup.android.data.local.NotificationType
import com.stepup.android.data.local.WalkSessionDao
import com.stepup.android.data.remote.CrewApi
import com.stepup.android.data.remote.CrewRequestRow
import com.stepup.android.data.remote.CrewRow
import com.stepup.android.data.remote.PartyApi
import com.stepup.android.data.remote.PartyStateRow
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.domain.CrewRank
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.haversineMeters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/** 크루 가입 방식 */
enum class CrewJoinPolicy {
    /** 누르면 바로 가입 */
    OPEN,

    /** 가입 신청 → 크루장이 승인하면 가입 */
    APPROVAL;

    companion object {
        fun of(value: String): CrewJoinPolicy = if (value == "APPROVAL") APPROVAL else OPEN
    }
}

/** 러닝 크루 */
data class Crew(
    val id: String,
    val monogram: String,
    val name: String,
    val tagline: String,
    val area: String,
    /** 보는 사람과의 거리(km). 서버가 아직 크루 위치를 받지 않아 0 이면 지역 이름을 보여 준다. */
    val kmAway: Double = 0.0,
    val memberCount: Int,
    /** 크루장이 먼저, 그다음은 먼저 들어온 순으로 최대 8명 */
    val roster: List<String>,
    /** 내가 만든 크루 */
    val owned: Boolean,
    val joinPolicy: CrewJoinPolicy = CrewJoinPolicy.OPEN,
    /** 내가 멤버인가 */
    val joined: Boolean = false,
    /** 내가 가입 신청을 넣고 기다리는 중인가 */
    val requested: Boolean = false,
    /** 기다리는 가입 신청 수. 크루장에게만 값이 있다. */
    val pendingCount: Int = 0,
)

/** 크루장이 보는 가입 신청 */
data class CrewJoinRequest(
    val userId: String,
    val name: String,
    val requestedAt: String,
)

/** 크루 목록을 서버에서 가져온 상태 */
sealed interface CrewSyncState {
    data object Idle : CrewSyncState
    data object Loading : CrewSyncState
    data object Ready : CrewSyncState

    /** 로그인하지 않았다. 크루는 계정이 있어야 보인다. */
    data object SignInRequired : CrewSyncState

    /** 서버에 닿지 못했거나 거절당했다. 다시 시도하면 될 수 있다. */
    data class Failed(val reason: String) : CrewSyncState
}

/** 크루에 무언가를 했을 때의 결말 */
sealed interface CrewActionResult {
    data object Joined : CrewActionResult
    data object Requested : CrewActionResult
    data class Created(val crewId: String) : CrewActionResult
    data object Done : CrewActionResult

    /** @param signIn 로그인해야 할 수 있는 일이었다 */
    data class Failed(val reason: String, val signIn: Boolean = false) : CrewActionResult
}

data class PartyMember(
    /** 서버의 사용자 id */
    val id: String,
    val name: String,
    val ready: Boolean,
    val isMe: Boolean,
    /** 방장 */
    val isHost: Boolean = false,
    /** 방장에게서의 거리(m). 달리기 전이거나 위치를 모르면 -1 */
    val distanceM: Int = -1,
    /** 마지막으로 서버에 소식을 보낸 지 몇 초 */
    val seenSec: Int = 0,
)

/** 로비가 멈춘 까닭 */
enum class PartyProblem {
    /** 로그인해야 방에 들어갈 수 있다 */
    SIGN_IN,

    /** 크루원이나 번개 참가자가 아니다 — 또는 방장이 내보냈다 */
    REMOVED,

    /** 서버에 닿지 못했다. 저절로 다시 묻는다. */
    NETWORK,

    /** 방이 닫혔다 */
    CLOSED,

    /** 방금 누른 것(준비·출발·내보내기)을 서버가 받아 주지 않았다 */
    FAILED,
}

enum class PartyPhase {
    /** 로비에 들어오지 않은 상태 */
    IDLE,

    /** 준비 대기 중 */
    LOBBY,

    /** 전원 준비 완료 → 카운트다운 */
    COUNTDOWN,

    /** 같이 측정 중 */
    RUNNING,

    /** 정산 결과 표시 */
    FINISHED,
}

data class PartyState(
    val phase: PartyPhase = PartyPhase.IDLE,
    /** 서버의 방 번호. 방에 들어가기 전에는 null */
    val partyId: Long? = null,
    val crewId: String? = null,
    /** 번개러닝에서 연 로비면 그 글의 id. 크루 로비면 null. */
    val flashPostId: Long? = null,
    val crewName: String = "",
    val members: List<PartyMember> = emptyList(),
    val countdown: Int = 0,
    val resultPoints: Double = 0.0,
    val resultSteps: Int = 0,
    /** 모두 같이 출발하는 시각 — 이 폰의 시계로 고친 값 */
    val startsAtLocal: Long = 0L,
    /** 방장이 방을 닫았다 */
    val roomClosed: Boolean = false,
    val problem: PartyProblem? = null,
) {
    val readyCount: Int get() = members.count { it.ready }
    val partySize: Int get() = members.size
    val allReady: Boolean get() = members.isNotEmpty() && members.all { it.ready }
    val myReady: Boolean get() = members.firstOrNull { it.isMe }?.ready == true
    val isHost: Boolean get() = members.firstOrNull { it.isMe }?.isHost == true

    /**
     * 파티장이 지금 출발할 수 있는가.
     *
     * 전원이 준비하기를 기다리지 않는다. 한 사람이 신발 끈을 못 찾으면
     * 나머지 넷이 길에 서 있게 되고, 그 다섯 명은 다음부터 파티런을 안 쓴다.
     * 방장 본인만 준비돼 있으면 출발할 수 있고, 준비를 마친 사람들끼리 뛴다.
     */
    val canStart: Boolean get() = isHost && myReady
    val isActive: Boolean get() = phase == PartyPhase.RUNNING
}

/**
 * 크루 목록·가입 상태와 파티런 로비를 관리한다.
 *
 * 크루는 서버(Supabase)에만 있다. 예전에는 폰 안에 가짜 크루 네 개를 심어
 * 두었는데, 그러면 내가 만든 크루를 친구가 볼 수 없어 초대도 가입도 성립하지
 * 않는다. 목록은 서버에서 받아 메모리에 들고 있고, 가입·탈퇴·만들기는 서버
 * 함수를 부른 뒤 목록을 다시 받는다.
 *
 * 파티런 로비도 서버에 있다. 같은 크루의 로비를 연 사람들이 같은 방에 모인다.
 */
class CrewRepository(
    private val api: CrewApi,
    private val crewDao: CrewDao,
    private val crewInfoDao: CrewInfoDao,
    private val walkSessionDao: WalkSessionDao,
    private val rewardRepository: RewardRepository,
    private val partyApi: PartyApi,
    /** 달리는 동안 방에 보낼 내 위치. 모르면 null */
    private val currentLocation: () -> GeoPoint? = { null },
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _crews = MutableStateFlow<List<Crew>>(emptyList())
    val crews: StateFlow<List<Crew>> = _crews

    val joinedCrewIds: StateFlow<Set<String>> = _crews
        .map { list -> list.filter { it.joined }.map { it.id }.toSet() }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    private val _sync = MutableStateFlow<CrewSyncState>(CrewSyncState.Idle)
    val sync: StateFlow<CrewSyncState> = _sync

    private val refreshLock = Mutex()

    fun crewOf(id: String): Crew? = crews.value.firstOrNull { it.id == id }

    /**
     * 예전 버전이 폰 안에 만들어 둔 크루와 가입 기록을 지운다.
     *
     * 크루는 이제 서버에만 있다. 폰에 남은 것은 시드로 넣은 가짜 크루이거나
     * 나 혼자만 보던 크루라, 남겨 두면 실제 크루와 섞여 헷갈린다.
     */
    suspend fun clearLegacy() {
        crewInfoDao.clear()
        crewDao.clear()
    }

    /** 서버에서 크루 목록을 다시 받는다. 동시에 여러 번 불려도 한 번씩 차례로 한다. */
    suspend fun refresh(): CrewSyncState = refreshLock.withLock {
        if (_crews.value.isEmpty()) _sync.value = CrewSyncState.Loading
        val next = when (val result = api.crews()) {
            is ServerResult.Ok -> {
                val list = result.value.map { it.toDomain() }
                announceApprovals(list)
                _crews.value = list
                CrewSyncState.Ready
            }
            is ServerResult.SignInRequired -> CrewSyncState.SignInRequired
            is ServerResult.Rejected -> CrewSyncState.Failed(result.reason)
            is ServerResult.Retry -> CrewSyncState.Failed(result.reason)
        }
        _sync.value = next
        next
    }

    /**
     * 기다리던 가입 신청이 승인됐으면 알림을 남긴다.
     *
     * 아직 푸시가 없어서 크루장이 승인해도 신청한 사람은 모른다. 목록을 다시
     * 받았을 때 "신청함"이던 크루가 "가입함"이 되어 있으면 그게 승인이다.
     */
    private suspend fun announceApprovals(next: List<Crew>) {
        val waiting = _crews.value.filter { it.requested }.map { it.id }.toSet()
        if (waiting.isEmpty()) return
        next.filter { it.joined && it.id in waiting }.forEach {
            rewardRepository.notify(NotificationType.CREW_JOINED, it.name, argExtra = it.id)
        }
    }

    /**
     * 가입. 자유 가입 크루는 바로 들어가고([CrewActionResult.Joined]),
     * 승인제 크루는 신청만 남는다([CrewActionResult.Requested]).
     */
    suspend fun join(crewId: String): CrewActionResult {
        val outcome = when (val result = api.join(crewId)) {
            is ServerResult.Ok ->
                if (result.value == "REQUESTED") CrewActionResult.Requested else CrewActionResult.Joined
            else -> result.asFailure()
        }
        refresh()
        if (outcome == CrewActionResult.Joined) {
            crewOf(crewId)?.let {
                // 크루 id 를 함께 담는다. 알림을 눌렀을 때 그 크루로 갈 수 있어야 한다.
                rewardRepository.notify(NotificationType.CREW_JOINED, it.name, argExtra = it.id)
            }
        }
        return outcome
    }

    /** 탈퇴, 또는 기다리던 가입 신청 취소. 서버 함수 하나가 둘 다 한다. */
    suspend fun leave(crewId: String): CrewActionResult {
        val outcome = when (val result = api.leave(crewId)) {
            is ServerResult.Ok -> CrewActionResult.Done
            else -> result.asFailure()
        }
        refresh()
        return outcome
    }

    /** 크루 만들기. 만든 사람은 서버가 주인 멤버로 넣는다. */
    suspend fun create(
        name: String,
        tagline: String,
        area: String,
        joinPolicy: CrewJoinPolicy,
    ): CrewActionResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return CrewActionResult.Failed("")
        val monogram = trimmed.split(" ", "-", "_")
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
            .ifBlank { trimmed.take(2).uppercase() }
        val outcome = when (
            val result = api.create(trimmed, monogram, tagline.trim(), area.trim(), joinPolicy.name)
        ) {
            is ServerResult.Ok -> CrewActionResult.Created(result.value)
            else -> result.asFailure()
        }
        refresh()
        return outcome
    }

    /** 크루장 — 가입 방식 바꾸기. 자유 가입으로 열면 기다리던 신청은 모두 받아 준다. */
    suspend fun setJoinPolicy(crewId: String, joinPolicy: CrewJoinPolicy): CrewActionResult {
        val outcome = when (val result = api.setJoinPolicy(crewId, joinPolicy.name)) {
            is ServerResult.Ok -> CrewActionResult.Done
            else -> result.asFailure()
        }
        refresh()
        return outcome
    }

    /** 크루장 — 기다리는 가입 신청 목록 */
    suspend fun requests(crewId: String): ServerResult<List<CrewJoinRequest>> =
        when (val result = api.requests(crewId)) {
            is ServerResult.Ok -> ServerResult.Ok(result.value.map { it.toDomain() })
            is ServerResult.Rejected -> result
            is ServerResult.Retry -> result
            is ServerResult.SignInRequired -> result
        }

    /** 크루장 — 가입 신청 승인·거절 */
    suspend fun decide(crewId: String, userId: String, approve: Boolean): CrewActionResult {
        val outcome = when (val result = api.decide(crewId, userId, approve)) {
            is ServerResult.Ok -> CrewActionResult.Done
            else -> result.asFailure()
        }
        refresh()
        return outcome
    }

    /** 화면 검사용 — 서버 없이 크루 목록을 채운다. */
    @VisibleForTesting
    fun showForTest(list: List<Crew>) {
        _crews.value = list
        _sync.value = CrewSyncState.Ready
    }

    // ── 파티런 로비 ──────────────────────────────────────────
    //
    // 방은 서버에 있다. 같은 크루(또는 같은 번개 글)의 로비를 연 사람들이 같은
    // 방에 모이고, 처음 연 사람이 방장이다. 앱은 몇 초마다 방의 상태를 물어
    // 준비·출발을 맞춘다 — 로비에서는 2초, 카운트다운은 서버 시각으로 폰이 세고,
    // 달리는 동안은 5초마다 내 위치를 보내며 묻는다.
    //
    // 달리는 동안 방장에게서 MAX_PARTY_DISTANCE_M 넘게 떨어졌거나 소식이
    // STALE_SEC 넘게 끊긴 사람은 이 폰의 파티에서 빠진다. 적립 보너스(인원수)는
    // 실제로 같이 뛴 사람만 센다.

    private val _party = MutableStateFlow(PartyState())
    val party: StateFlow<PartyState> = _party

    private var pollJob: Job? = null

    /** 방에 무언가를 한 뒤 기다리지 말고 바로 다시 물으라는 신호 */
    private val wake = Channel<Unit>(Channel.CONFLATED)

    /** 달리는 동안 이 폰의 파티에서 뺀 사람. 서버가 다시 보내도 다시 넣지 않는다. */
    private val droppedIds = mutableSetOf<String>()

    /** 크루 로비. 크루원만 들어갈 수 있다. */
    fun openLobby(crewId: String) {
        val crew = crewOf(crewId)
        open(crewId = crewId, flashPostId = null, title = crew?.name.orEmpty())
    }

    /**
     * 번개러닝 로비. 그 번개에 참가한 사람만 들어갈 수 있다.
     *
     * 크루 로비와 같은 판을 쓴다 — 준비를 누르고, 방장이 출발하고, 인원수만큼
     * 적립 부스트가 붙는다.
     */
    fun openFlashLobby(postId: Long, title: String) {
        open(crewId = null, flashPostId = postId, title = title)
    }

    private fun open(crewId: String?, flashPostId: Long?, title: String) {
        val current = _party.value
        val sameRoom = current.crewId == crewId && current.flashPostId == flashPostId
        if (sameRoom && current.phase != PartyPhase.IDLE && current.phase != PartyPhase.FINISHED &&
            current.problem == null
        ) {
            return // 이미 이 방에 있음
        }
        pollJob?.cancel()
        current.partyId?.let { old -> if (!sameRoom) scope.launch { partyApi.leave(old) } }
        droppedIds.clear()
        _party.value = PartyState(
            phase = PartyPhase.LOBBY,
            crewId = crewId,
            flashPostId = flashPostId,
            crewName = title,
        )
        pollJob = scope.launch {
            when (val result = partyApi.open(crewId, flashPostId)) {
                is ServerResult.Ok -> {
                    _party.value = _party.value.copy(partyId = result.value)
                    poll(result.value)
                }
                else -> _party.value = _party.value.copy(problem = result.asPartyProblem())
            }
        }
    }

    private suspend fun poll(partyId: Long) {
        while (true) {
            val before = _party.value
            if (before.partyId != partyId) return
            if (before.phase == PartyPhase.RUNNING) {
                val here = currentLocation()
                partyApi.ping(partyId, here?.lat, here?.lng)
            }
            when (val result = partyApi.state(partyId)) {
                is ServerResult.Ok -> applyRoom(result.value)
                // 방장이 내보냈거나 방이 없어졌다. 더 물어도 같다.
                is ServerResult.Rejected -> {
                    if (_party.value.phase != PartyPhase.RUNNING) {
                        _party.value = _party.value.copy(problem = PartyProblem.REMOVED)
                    }
                    return
                }
                is ServerResult.SignInRequired -> {
                    _party.value = _party.value.copy(problem = PartyProblem.SIGN_IN)
                    return
                }
                is ServerResult.Retry -> {
                    if (_party.value.phase != PartyPhase.RUNNING) {
                        _party.value = _party.value.copy(problem = PartyProblem.NETWORK)
                    }
                }
            }

            val now = _party.value
            if (now.partyId != partyId) return
            when (now.phase) {
                PartyPhase.COUNTDOWN -> countDown(partyId)
                PartyPhase.FINISHED, PartyPhase.IDLE -> return
                else -> Unit
            }
            // 방장이 방을 닫았으면 더 물을 것이 없다. 내 러닝은 내가 멈출 때까지 간다.
            if (_party.value.roomClosed) return
            val interval = if (_party.value.phase == PartyPhase.RUNNING) RUN_POLL_MS else LOBBY_POLL_MS
            withTimeoutOrNull(interval) { wake.receive() }
        }
    }

    /** 서버가 정한 출발 시각까지 센다. 폰 시계가 틀려도 서버 시각과의 차이로 맞춘다. */
    private suspend fun countDown(partyId: Long) {
        while (true) {
            val state = _party.value
            if (state.partyId != partyId || state.phase != PartyPhase.COUNTDOWN) return
            val left = state.startsAtLocal - System.currentTimeMillis()
            if (left <= 0) {
                _party.value = state.copy(phase = PartyPhase.RUNNING, countdown = 0)
                return
            }
            val seconds = ((left + 999) / 1000).toInt()
            if (seconds != state.countdown) _party.value = state.copy(countdown = seconds)
            delay(minOf(left, 200L))
        }
    }

    private suspend fun applyRoom(row: PartyStateRow) {
        val before = _party.value
        val offset = row.serverNow.isoToMillis() - System.currentTimeMillis()
        val host = row.members.firstOrNull { it.isHost }
        val hostPoint = host?.let { h -> h.lat?.let { lat -> h.lng?.let { lng -> GeoPoint(lat, lng) } } }

        val members = row.members.map { m ->
            val point = m.lat?.let { lat -> m.lng?.let { lng -> GeoPoint(lat, lng) } }
            PartyMember(
                id = m.userId,
                name = m.name,
                ready = m.ready,
                isMe = m.isMe,
                isHost = m.isHost,
                distanceM = when {
                    m.isHost -> 0
                    point != null && hostPoint != null -> haversineMeters(point, hostPoint).toInt()
                    else -> -1
                },
                seenSec = m.seenSec,
            )
        }

        var phase = when (row.status) {
            "COUNTDOWN" -> PartyPhase.COUNTDOWN
            "RUNNING" -> PartyPhase.RUNNING
            "FINISHED" -> before.phase
            else -> PartyPhase.LOBBY
        }
        // 폰이 먼저 카운트다운을 끝냈으면 서버가 아직 COUNTDOWN 이어도 뛰는 중이다.
        if (before.phase == PartyPhase.RUNNING && phase == PartyPhase.COUNTDOWN) phase = PartyPhase.RUNNING
        if (before.phase == PartyPhase.FINISHED) phase = PartyPhase.FINISHED

        val closed = row.status == "FINISHED"
        var visible = members
        val newlyDropped = mutableListOf<PartyMember>()
        if (phase == PartyPhase.RUNNING) {
            visible = members.filter { m ->
                if (m.isMe || m.id in droppedIds) return@filter m.isMe
                val far = m.distanceM > MAX_PARTY_DISTANCE_M
                val gone = m.seenSec > STALE_SEC
                if (far || gone) {
                    droppedIds += m.id
                    newlyDropped += m
                }
                !(far || gone)
            }
        }
        // 출발 때 준비 안 한 사람은 서버가 방에서 뺀다. 조용히 사라지면 본인은
        // 앱이 고장 난 줄 알고, 남은 사람은 왜 줄었는지 모른다.
        if (before.phase == PartyPhase.LOBBY && phase != PartyPhase.LOBBY) {
            val stayed = members.mapTo(HashSet()) { it.id }
            newlyDropped += before.members.filter { !it.isMe && it.id !in stayed }
        }

        _party.value = before.copy(
            phase = phase,
            // 크루 목록을 받기 전에 로비를 열었으면 이름이 비어 있다
            crewName = before.crewName.ifBlank { row.crewId?.let { crewOf(it)?.name }.orEmpty() },
            crewId = row.crewId ?: before.crewId,
            flashPostId = row.flashPostId ?: before.flashPostId,
            members = if (phase == PartyPhase.FINISHED) before.members else visible,
            startsAtLocal = row.startsAt?.isoToMillis()?.minus(offset) ?: 0L,
            roomClosed = closed,
            problem = when {
                closed && phase == PartyPhase.LOBBY -> PartyProblem.CLOSED
                // 누른 것이 막혔다는 알림은 다음에 무언가를 누를 때까지 둔다
                before.problem == PartyProblem.FAILED -> PartyProblem.FAILED
                else -> null
            },
        )
        newlyDropped.forEach { rewardRepository.notify(NotificationType.PARTY_MEMBER_LEFT, it.name) }
    }

    /** 방에 무언가를 하고, 된 뒤에 바로 방을 다시 묻는다. */
    private fun act(call: suspend (Long) -> ServerResult<Unit>) {
        val id = _party.value.partyId ?: return
        if (_party.value.problem == PartyProblem.FAILED) _party.value = _party.value.copy(problem = null)
        scope.launch {
            val result = call(id)
            if (result !is ServerResult.Ok) {
                val problem = if (result is ServerResult.SignInRequired) PartyProblem.SIGN_IN else PartyProblem.FAILED
                _party.value = _party.value.copy(problem = problem)
            }
            wake.trySend(Unit)
        }
    }

    fun leaveLobby() {
        pollJob?.cancel()
        _party.value.partyId?.let { id -> scope.launch { partyApi.leave(id) } }
        _party.value = PartyState()
    }

    /** 방장 권한 — 로비에서 한 사람을 내보낸다. */
    fun kick(memberId: String) {
        val state = _party.value
        if (state.phase != PartyPhase.LOBBY || !state.isHost) return
        _party.value = state.copy(members = state.members.filterNot { it.id == memberId })
        act { partyApi.kick(it, memberId) }
    }

    /** 내 준비 상태. 화면은 바로 바꾸고, 서버가 거절하면 다음 물음에서 되돌아간다. */
    fun setMyReady(ready: Boolean) {
        val state = _party.value
        if (state.phase != PartyPhase.LOBBY) return
        _party.value = state.copy(
            members = state.members.map { if (it.isMe) it.copy(ready = ready) else it },
        )
        act { partyApi.ready(it, ready) }
    }

    /**
     * 방장 권한 — 준비된 사람들끼리 출발. 서버가 출발 시각을 정하고, 모든
     * 폰이 그 시각에 맞춰 카운트다운을 끝낸다.
     */
    fun startParty() {
        val state = _party.value
        if (state.phase != PartyPhase.LOBBY || !state.canStart) return
        act { partyApi.start(it) }
    }

    /** 이상이 있어 멈춘 로비를 다시 연다. */
    fun retryLobby() {
        val state = _party.value
        _party.value = state.copy(phase = PartyPhase.IDLE)
        open(state.crewId, state.flashPostId, state.crewName)
    }

    /** 세션 정산 후 결과 화면으로 전환. 방장이면 방을 닫고, 아니면 방에서 나온다. */
    fun finishParty(points: Double, steps: Int) {
        val state = _party.value
        if (state.phase != PartyPhase.RUNNING) return
        pollJob?.cancel()
        state.partyId?.let { id ->
            scope.launch { if (state.isHost) partyApi.finish(id) else partyApi.leave(id) }
        }
        _party.value = state.copy(
            phase = PartyPhase.FINISHED,
            resultPoints = points,
            resultSteps = steps,
        )
    }

    fun dismissResult() {
        if (_party.value.phase == PartyPhase.FINISHED) _party.value = PartyState()
    }

    /** 화면 검사용 — 서버 없이 로비를 채운다. */
    @VisibleForTesting
    fun showPartyForTest(state: PartyState) {
        pollJob?.cancel()
        _party.value = state
    }

    /** 현재 파티 인원 (파티런이 아니면 1) */
    fun currentPartySize(): Int =
        if (_party.value.isActive) _party.value.partySize.coerceAtLeast(1) else 1

    /**
     * 지금 달리고 있는 크루 러닝의 크루 id. 크루 러닝이 아니면 빈 문자열이다.
     *
     * 번개러닝 로비도 파티지만 크루가 없다(crewId 가 null 이다). 그 거리를
     * 아무 크루에나 얹을 수는 없으므로 여기서도 빈 문자열이 나간다.
     */
    fun currentPartyCrewId(): String =
        if (_party.value.isActive) _party.value.crewId.orEmpty() else ""

    /**
     * 크루 순위 — 크루 러닝으로 많이 달린 순.
     *
     * 아직 한 번도 같이 달리지 않은 크루는 0 km 로 명단에 남는다. 목록에서
     * 빼 버리면 "우리 크루가 순위에 없다"가 되고, 사용자는 기능이 고장 난
     * 줄 안다. 0 km 는 고장이 아니라 아직 안 달렸다는 뜻이다.
     *
     * 거리는 아직 이 폰에서 달린 크루 러닝만 센다. 크루원 전체의 거리를 모으는
     * 일은 파티런을 서버에 붙일 때 함께 한다.
     *
     * @param since 이 시각 이후에 시작한 러닝만 센다. 전체기간이면 0.
     */
    suspend fun ranking(since: Long): List<CrewRank> {
        val totals = walkSessionDao.crewDistances(since).associateBy { it.crewId }
        // 화면이 열리자마자 순위를 물으면 목록을 아직 못 받았을 수 있다.
        if (_crews.value.isEmpty()) refresh()
        return _crews.value
            .map { crew ->
                val total = totals[crew.id]
                CrewRank(
                    // 번호는 정렬한 뒤에 붙인다
                    rank = 0,
                    crewId = crew.id,
                    name = crew.name,
                    monogram = crew.monogram,
                    km = (total?.meters ?: 0.0) / 1000.0,
                    runs = total?.runs ?: 0,
                    joined = crew.joined,
                )
            }
            // 같은 거리면 이름순. 순서가 매번 흔들리면 순위표를 믿지 않게 된다.
            .sortedWith(compareByDescending<CrewRank> { it.km }.thenBy { it.name })
            .mapIndexed { index, row -> row.copy(rank = index + 1) }
    }

    companion object {
        /** 방장에게서 이만큼 떨어지면 자동으로 파티에서 빠진다 */
        const val MAX_PARTY_DISTANCE_M = 100

        /** 달리는 동안 이만큼(초) 소식이 없으면 파티에서 빠진다 */
        const val STALE_SEC = 60

        private const val LOBBY_POLL_MS = 2_000L
        private const val RUN_POLL_MS = 5_000L
    }
}

/** 서버 줄 → 도메인 모델 */
fun CrewRow.toDomain(): Crew = Crew(
    id = id,
    monogram = monogram.ifBlank { name.take(2).uppercase() },
    name = name,
    tagline = tagline,
    area = area,
    memberCount = memberCount,
    roster = roster,
    owned = owned,
    joinPolicy = CrewJoinPolicy.of(joinPolicy),
    joined = joined,
    requested = requested,
    pendingCount = pendingCount,
)

private fun CrewRequestRow.toDomain() = CrewJoinRequest(userId, name, requestedAt)

/** 방에 들어가거나 무언가를 하다 막힌 까닭 */
private fun ServerResult<*>.asPartyProblem(): PartyProblem = when (this) {
    is ServerResult.SignInRequired -> PartyProblem.SIGN_IN
    is ServerResult.Rejected -> PartyProblem.REMOVED
    is ServerResult.Retry, is ServerResult.Ok -> PartyProblem.NETWORK
}

/** 서버가 받아 주지 않은 결말을 화면이 쓰는 실패로 */
private fun ServerResult<*>.asFailure(): CrewActionResult.Failed = when (this) {
    is ServerResult.SignInRequired -> CrewActionResult.Failed(reason, signIn = true)
    is ServerResult.Rejected -> CrewActionResult.Failed(reason)
    is ServerResult.Retry -> CrewActionResult.Failed(reason)
    is ServerResult.Ok -> CrewActionResult.Failed("")
}
