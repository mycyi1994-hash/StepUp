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
import com.stepup.android.core.Analytics
import com.stepup.android.domain.RankPeriod
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

    /**
     * @param signIn 로그인해야 할 수 있는 일이었다
     * @param retryable 연결·서버 문제라 나중에 다시 하면 될 수 있다
     */
    data class Failed(
        val reason: String,
        val signIn: Boolean = false,
        val retryable: Boolean = false,
    ) : CrewActionResult
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
    /** 같이 뛰는 중 보이는 위치 — 본인이거나 공유를 켠 사람만 */
    val point: GeoPoint? = null,
    /** 화면용 거리(km) — 본인이거나 공유를 켠 사람만 */
    val km: Double? = null,
    val sharing: Boolean = false,
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

    /** 함께 출발한 지 오래돼(대기실을 떠나 있었다) 그 방에서 나오고 새 대기실을 열었다 */
    MISSED_START,
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
    /**
     * 끝난 러닝의 시작 시각 — 결과 화면이 이 러닝의 서버 확인 금액을 찾는 열쇠.
     * 폰이 계산한 예상 금액은 두지 않는다(서버가 무효 · 상한 처리할 수 있다).
     */
    val resultStartedAt: Long = 0L,
    val resultSteps: Int = 0,
    /** 모두 같이 출발하는 시각 — 이 폰의 시계로 고친 값 */
    val startsAtLocal: Long = 0L,
    /** 방장이 방을 닫았다 */
    val roomClosed: Boolean = false,
    val problem: PartyProblem? = null,
    /**
     * 같이 뛰는 중 서버에 닿지 못했다(CR18) — 화면 안내용. 다른 사람 위치는 오래된 값이라 지도에서 뺀다.
     * 내 러닝은 그대로 이어지고, 다음 물음이 닿으면 저절로 풀린다.
     */
    val liveOffline: Boolean = false,
    /**
     * 내 위치 공유 선택이 아직 서버에 닿지 않았다 — 닿을 때까지 물을 때마다 다시 보낸다(null 이면 기다리는 것이 없다).
     * 끄기가 한 번 실패하면 다음 물음이 서버의 "켬"으로 되돌려 위치가 계속 보였다.
     */
    val pendingShare: Boolean? = null,
) {
    /** 로비 · 달리는 중 어디서든 서버에 닿지 못하고 있다 */
    val networkProblem: Boolean get() = problem == PartyProblem.NETWORK || liveOffline
    val readyCount: Int get() = members.count { it.ready }
    val partySize: Int get() = members.size
    val allReady: Boolean get() = members.isNotEmpty() && members.all { it.ready }
    val myReady: Boolean get() = members.firstOrNull { it.isMe }?.ready == true
    val isHost: Boolean get() = members.firstOrNull { it.isMe }?.isHost == true
    /** 내 위치 · 거리를 같이 뛰는 사람에게 보이는 중 */
    val myShare: Boolean get() = members.firstOrNull { it.isMe }?.sharing == true

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
    /** 달리는 동안 이 폰이 잰 거리(km) — 공유를 켰을 때만 방에 보낸다(화면용) */
    private val currentKm: () -> Double? = { null },
    /** 방에 들어갈 때 "내 위치 보이기"를 켤지 — 사용자가 마지막으로 고른 값(기본 끔) */
    private val shareByDefault: suspend () -> Boolean = { false },
    /** 이 폰이 러닝을 마치고 나온 방(최근 것) — 나가기가 서버에 닿지 않았어도 그 방에 다시 붙지 않게 */
    private val exitedParties: suspend () -> Set<Long> = { emptySet() },
    private val rememberExit: suspend (Long) -> Unit = {},
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _crews = MutableStateFlow<List<Crew>>(emptyList())
    val crews: StateFlow<List<Crew>> = _crews

    /** 크루 명함(목록 · 상세) — 같은 crew_feed 한 번으로 [crews] 와 함께 채운다 */
    private val _cards = MutableStateFlow<List<com.stepup.android.domain.CrewCard>>(emptyList())
    val cards: StateFlow<List<com.stepup.android.domain.CrewCard>> = _cards

    val joinedCrewIds: StateFlow<Set<String>> = _crews
        .map { list -> list.filter { it.joined }.map { it.id }.toSet() }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    private val _sync = MutableStateFlow<CrewSyncState>(CrewSyncState.Idle)
    val sync: StateFlow<CrewSyncState> = _sync

    private val refreshLock = Mutex()

    /** 계정이 바뀔 때마다 늘어난다 — 바뀌기 전에 보낸 목록 요청의 답(앞 계정 것)을 버린다 */
    @Volatile private var accountEpoch = 0

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
        val epoch = accountEpoch
        val before = _sync.value
        if (_crews.value.isEmpty()) _sync.value = CrewSyncState.Loading
        // 받는 중에 부른 화면이 닫혀 취소되면 "불러오는 중"으로 남기지 않는다(같은 상태를 보는 다른 화면이 멈춰 보이지 않게)
        val result = try {
            api.crews()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            if (_sync.value == CrewSyncState.Loading) _sync.value = before
            throw cancelled
        }
        // 받는 사이 계정이 바뀌었다 — 앞 계정의 목록을 새 계정에 채우지 않는다(다음 물음이 새 계정 것을 받는다)
        if (epoch != accountEpoch) return@withLock _sync.value
        val next = when (result) {
            is ServerResult.Ok -> {
                val list = result.value.map { it.toDomain() }
                announceApprovals(list)
                _crews.value = list
                _cards.value = result.value.map { it.toCard() }
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
        if (outcome == CrewActionResult.Joined || outcome == CrewActionResult.Requested) {
            Analytics.crewJoined(requested = outcome == CrewActionResult.Requested)
        }
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
            is ServerResult.Ok -> CrewActionResult.Created(result.value).also { Analytics.crewCreated() }
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

    /** 한 크루를 다시 읽었다 — 목록의 같은 크루를 바꾼다(목록에 없으면 그대로) */
    fun patchCard(card: com.stepup.android.domain.CrewCard) {
        if (_cards.value.any { it.id == card.id }) _cards.value = _cards.value.map { if (it.id == card.id) card else it }
    }

    /** 해산 · 접근 불가 — 목록에서 뺀다 */
    fun dropCrew(crewId: String) {
        _cards.value = _cards.value.filterNot { it.id == crewId }
        _crews.value = _crews.value.filterNot { it.id == crewId }
    }

    /** 로그아웃 · 계정 바꿈 — 앞 계정의 가입 상태가 남지 않게 */
    fun clearCards() {
        _cards.value = emptyList()
    }

    // ── 파티런 로비 ──────────────────────────────────────────
    //
    // 방은 서버에 있다. 같은 크루(또는 같은 번개 글)의 로비를 연 사람들이 같은
    // 방에 모이고, 처음 연 사람이 방장이다. 앱은 몇 초마다 방의 상태를 물어
    // 준비·출발을 맞춘다 — 로비에서는 2초, 카운트다운은 서버 시각으로 폰이 세고,
    // 달리는 동안은 5초마다 내 위치를 보내며 묻는다.
    //
    // 달리는 동안 소식이 STALE_SEC 넘게 끊긴 사람은 이 폰의 명단에서 잠시 빠진다(다시 소식이 오면 돌아온다).
    // 적립 보너스 인원은 서버가 센다 — 출발 2분 뒤에도 서로 NEAR_DISTANCE_M 안에서 같이 뛴 사람들(0056).
    // 이 폰의 인원은 화면 예상일 뿐이다.

    private val _party = MutableStateFlow(PartyState())
    val party: StateFlow<PartyState> = _party

    private var pollJob: Job? = null

    /** 방에 무언가를 한 뒤 기다리지 말고 바로 다시 물으라는 신호 */
    private val wake = Channel<Unit>(Channel.CONFLATED)

    /** 달리는 동안 "나갔어요"를 이미 알린 사람 — 다시 끊겨도 한 번만 알린다 */
    private val droppedIds = mutableSetOf<String>()

    /** 출발을 보낸 방 — 두 번 눌러도 한 번만 보낸다(두 번째는 서버가 거절해 실패 안내가 남았다) */
    private var startingParty: Long? = null

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

    private fun open(crewId: String?, flashPostId: Long?, title: String, notice: PartyProblem? = null) {
        val current = _party.value
        val sameRoom = current.crewId == crewId && current.flashPostId == flashPostId
        if (sameRoom && current.phase != PartyPhase.IDLE && current.phase != PartyPhase.FINISHED &&
            (current.problem == null || current.problem == PartyProblem.MISSED_START)
        ) {
            return // 이미 이 방에 있음
        }
        pollJob?.cancel()
        current.partyId?.let { old -> if (!sameRoom) scope.launch { partyApi.leave(old) } }
        droppedIds.clear()
        startingParty = null
        _party.value = PartyState(
            phase = PartyPhase.LOBBY,
            crewId = crewId,
            flashPostId = flashPostId,
            crewName = title,
            problem = notice,
        )
        pollJob = scope.launch {
            var wait = OPEN_RETRY_MS
            var rejoined = false
            while (true) {
                when (val result = partyApi.open(crewId, flashPostId)) {
                    is ServerResult.Ok -> {
                        val id = result.value
                        // 러닝을 마치고 나온 방을 서버가 다시 내줬다 — 나가기가 서버에 닿지 않았다.
                        // 그 방에 다시 붙으면 새 러닝이 옛 명단 · 크루로 켜진다. 다시 나오고 새 대기실을 연다.
                        if (!rejoined && id in exitedParties()) {
                            rejoined = true
                            partyApi.leave(id)
                            continue
                        }
                        _party.value = _party.value.copy(
                            partyId = id,
                            // 새 대기실 안내(놓친 출발)는 그사이 연결이 잠깐 끊겨 "다시 시도"가 대신 보였어도 되살린다
                            problem = notice.takeIf { _party.value.problem == notice || _party.value.problem == PartyProblem.NETWORK },
                        )
                        if (shareByDefault()) partyApi.share(id, true)
                        poll(id)
                        return@launch
                    }
                    // 서버에 닿지 못했다 — "다시 시도하는 중"이라고 말한 대로 조금씩 늦추며 다시 연다
                    is ServerResult.Retry -> {
                        _party.value = _party.value.copy(problem = PartyProblem.NETWORK)
                        delay(wait)
                        wait = (wait * 2).coerceAtMost(OPEN_RETRY_MAX_MS)
                    }
                    else -> {
                        _party.value = _party.value.copy(problem = result.asPartyProblem())
                        return@launch
                    }
                }
            }
        }
    }

    private suspend fun poll(partyId: Long) {
        while (true) {
            val before = _party.value
            if (before.partyId != partyId) return
            // 이 폰이 이 모임의 러닝을 실제로 뛰는 중일 때만 위치를 보낸다 — 서버는 출발 2분 뒤에도 서로 곁에서
            // 위치를 보낸 사람을 인원으로 센다(0056). 준비만 누르고 뛰지 않는 폰이나, 대기실을 떠나 따로 켠 자유
            // 러닝이 방에 위치를 보내면 남의 보너스가 된다.
            val running = com.stepup.android.service.WalkSessionService.state.value
                .let { it.isActive && !it.isPaused && it.group != com.stepup.android.service.RunGroup.NONE }
            if (before.phase == PartyPhase.RUNNING && running) {
                // GPS 를 잃었으면(마지막 점이 오래됐다) 위치 없이 보낸다 — 서버가 옛 점을 지워 다른 사람에게 지금
                // 위치처럼 보이지 않게
                val here = currentLocation()
                partyApi.ping(partyId, here?.lat, here?.lng)
                // 공유를 켠 사람만 화면용 거리를 보낸다 — 적립 계산에는 쓰이지 않는다
                if (before.myShare) currentKm()?.let { km -> partyApi.live(partyId, km.coerceIn(0.0, 300.0)) }
            }
            // 위치 공유 선택이 아직 서버에 닿지 않았으면 다시 보낸다(서버 값이 같아질 때까지)
            before.pendingShare?.let { partyApi.share(partyId, it) }
            when (val result = partyApi.state(partyId)) {
                is ServerResult.Ok -> applyRoom(result.value)
                // 방장이 내보냈거나 방이 없어졌다. 더 물어도 같다.
                // 서버에 닿기는 했다 — 같이 뛰는 중 "연결 끊김"(CR18)은 풀어 둔다(더 묻지 않으니 다시 켜질 일도 없다).
                // 같이 뛰는 중에도 방과 끊겼다고 적는다 — 더 묻지 않아 굳은 명단 · 위치를 화면이 지금 것처럼 보이지 않게(내 러닝은 이어진다)
                is ServerResult.Rejected -> {
                    _party.value = _party.value.copy(problem = PartyProblem.REMOVED, liveOffline = false)
                    return
                }
                is ServerResult.SignInRequired -> {
                    _party.value = _party.value.copy(problem = PartyProblem.SIGN_IN, liveOffline = false)
                    return
                }
                is ServerResult.Retry -> {
                    if (_party.value.phase != PartyPhase.RUNNING) {
                        _party.value = _party.value.copy(problem = PartyProblem.NETWORK)
                    } else {
                        _party.value = _party.value.copy(liveOffline = true)
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
            // 방장이 방을 닫았으면 방은 더 묻지 않는다. 내 러닝은 내가 멈출 때까지 간다 — 그동안 위치만 계속 보낸다.
            // 서버는 출발 2분 뒤에도 곁에서 뛴 사람을 함께 달린 인원으로 센다(0056). 진행자가 2분 전에 마치면
            // 남은 사람들이 끝까지 같이 달려도 보너스를 잃었다. 방 · 명단은 다시 열지 않는다(닫힌 방은 아무에게도 보이지 않는다)
            if (_party.value.roomClosed) {
                pingWhileRunning(partyId)
                return
            }
            val interval = if (_party.value.phase == PartyPhase.RUNNING) RUN_POLL_MS else LOBBY_POLL_MS
            withTimeoutOrNull(interval) { wake.receive() }
        }
    }

    /** 닫힌 방에 내 러닝이 끝날 때까지 위치만 보낸다(곁 판정용) — 마치면([finishParty]) · 다른 방으로 가면 멈춘다 */
    private suspend fun pingWhileRunning(partyId: Long) {
        while (_party.value.partyId == partyId && _party.value.phase == PartyPhase.RUNNING) {
            val run = com.stepup.android.service.WalkSessionService.state.value
            if (!run.isActive || run.group == com.stepup.android.service.RunGroup.NONE) return
            if (!run.isPaused) {
                val here = currentLocation()
                partyApi.ping(partyId, here?.lat, here?.lng)
            }
            delay(RUN_POLL_MS)
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
        // 서버가 내 공유 선택을 받았으면 기다림을 푼다. 아직이면 화면은 내가 고른 값을 보인다
        val serverShare = row.members.firstOrNull { it.isMe }?.share
        val pendingShare = before.pendingShare?.takeIf { it != serverShare }

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
                point = point,
                km = m.km,
                sharing = if (m.isMe && pendingShare != null) pendingShare else m.share,
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
            // 소식이 끊긴 사람은 명단에서 잠시 뺀다. 다시 소식이 오면 돌아온다 — 진행자가 1~2분 끊겼다고
            // 남은 사람을 끝까지 빼던 것(진행자의 굳은 점 기준 거리)은 없앴다. 보너스 인원은 서버가 곁으로 센다(0056).
            visible = members.filter { m ->
                if (m.isMe) return@filter true
                val gone = m.seenSec > STALE_SEC
                if (gone && droppedIds.add(m.id)) newlyDropped += m
                !gone
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
            liveOffline = false,
            pendingShare = pendingShare,
            problem = when {
                closed && phase == PartyPhase.LOBBY -> PartyProblem.CLOSED
                // 누른 것이 막혔다는 알림 · 새 대기실 안내는 대기실에서만, 다음에 무언가를 누를 때까지 둔다
                // (출발 뒤까지 남으면 달리는 내내 실패 안내가 남았다)
                phase == PartyPhase.LOBBY && before.problem == PartyProblem.FAILED -> PartyProblem.FAILED
                phase == PartyPhase.LOBBY && before.problem == PartyProblem.MISSED_START -> PartyProblem.MISSED_START
                else -> null
            },
        )
        newlyDropped.forEach { rewardRepository.notify(NotificationType.PARTY_MEMBER_LEFT, it.name) }
    }

    /** 방에 무언가를 하고, 된 뒤에 바로 방을 다시 묻는다. */
    private fun act(call: suspend (Long) -> ServerResult<Unit>) {
        val id = _party.value.partyId ?: return
        val problem = _party.value.problem
        if (problem == PartyProblem.FAILED || problem == PartyProblem.MISSED_START) {
            _party.value = _party.value.copy(problem = null)
        }
        scope.launch {
            val result = call(id)
            // 그새 다른 방으로 옮겼으면 이 방의 실패를 지금 방에 적지 않는다
            if (result !is ServerResult.Ok && _party.value.partyId == id) {
                val failed = if (result is ServerResult.SignInRequired) PartyProblem.SIGN_IN else PartyProblem.FAILED
                _party.value = _party.value.copy(problem = failed)
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

    /**
     * 달리는 동안 내 위치 · 거리를 같이 뛰는 사람에게 보일지. 로비와 달리는 중 모두 바꿀 수 있다.
     * 끄면 서버가 바로 다른 사람에게서 가린다(0037).
     */
    fun setMyShare(share: Boolean) {
        val state = _party.value
        val id = state.partyId ?: return
        // 서버가 받을 때까지 물을 때마다 다시 보낸다(poll) — 한 번 실패한 끄기가 서버의 "켬"으로 되돌아가지 않게
        _party.value = state.copy(
            pendingShare = share,
            members = state.members.map { if (it.isMe) it.copy(sharing = share) else it },
        )
        scope.launch {
            partyApi.share(id, share)
            wake.trySend(Unit)
        }
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
        val room = state.partyId ?: return
        if (startingParty == room) return
        startingParty = room
        act { id ->
            partyApi.start(id).also {
                if (it is ServerResult.Ok) Analytics.partyStarted(state.readyCount) else startingParty = null
            }
        }
    }

    /**
     * 출발 시각에 맞춰 이 폰의 러닝을 켜도 되는가 — 출발한 지 LATE_START_MS 안이어야 한다.
     * 대기실을 떠나 있던(채팅 · 화면 꺼짐) 사람이 한참 뒤 돌아와 옛 방의 러닝이 저절로 켜지던 것을 막는다.
     */
    fun startIsFresh(): Boolean {
        val state = _party.value
        return state.phase == PartyPhase.RUNNING &&
            System.currentTimeMillis() - state.startsAtLocal <= LATE_START_MS
    }

    /** 함께 출발을 놓쳤다 — 그 방에서 나오고(다시 붙지 않게 적어 둔다) 같은 크루(번개)의 새 대기실을 연다. */
    fun missedStart() {
        val state = _party.value
        val id = state.partyId ?: return
        if (state.phase != PartyPhase.RUNNING) return
        pollJob?.cancel()
        _party.value = state.copy(phase = PartyPhase.IDLE)
        // pollJob 에 둔다 — 나오는 사이 대기실을 떠나거나(leaveLobby) 계정이 바뀌면(resetForAccount) 이 일도 멈춘다.
        // 멈추지 않으면 사용자가 떠난 뒤에 새 대기실이 열려 보이지 않는 방을 앱이 꺼질 때까지 물었다
        pollJob = scope.launch {
            rememberExit(id)
            partyApi.leave(id)
            val now = _party.value
            if (now.partyId != id || now.phase != PartyPhase.IDLE) return@launch
            open(state.crewId, state.flashPostId, state.crewName, notice = PartyProblem.MISSED_START)
        }
    }

    /** 계정이 바뀌었다 — 앞 계정의 대기실 · 크루 목록 · 가입 표시를 남기지 않는다(옛 방을 새 계정으로 묻지 않게) */
    fun resetForAccount() {
        pollJob?.cancel()
        droppedIds.clear()
        startingParty = null
        _party.value = PartyState()
        // 앞 계정의 가입 · 신청 표시가 남으면 다음 목록에서 "신청 → 가입"으로 읽혀 새 계정에 가입 알림이 갔다(announceApprovals)
        accountEpoch++
        _crews.value = emptyList()
        _sync.value = CrewSyncState.Idle
        clearCards()
    }

    /** 지금 방을 다시 묻는다 — 같이 뛰는 중 연결이 끊겼을 때 "다시 연결"(CR18). 기다리던 물음을 앞당길 뿐이다. */
    fun wakeParty() {
        // 묻기가 이미 끝났으면(서버가 거절 · 로그인 필요로 멈춘 뒤) 깨울 것이 없다 — 같이 뛰는 중이면 다시 묻기 시작한다
        val now = _party.value
        val id = now.partyId
        if (pollJob?.isActive != true && id != null && now.phase == PartyPhase.RUNNING && !now.roomClosed) {
            pollJob = scope.launch { poll(id) }
            return
        }
        wake.trySend(Unit)
    }

    /** 이상이 있어 멈춘 로비를 다시 연다. */
    fun retryLobby() {
        val state = _party.value
        _party.value = state.copy(phase = PartyPhase.IDLE)
        open(state.crewId, state.flashPostId, state.crewName)
    }

    /** 세션 정산 후 결과 화면으로 전환. 방장이면 방을 닫고, 아니면 방에서 나온다. */
    fun finishParty(startedAt: Long, steps: Int) {
        val state = _party.value
        if (state.phase != PartyPhase.RUNNING) return
        pollJob?.cancel()
        state.partyId?.let { id ->
            scope.launch {
                // 먼저 적어 둔다 — 아래 나가기가 끝내 닿지 않아도 다음 대기실이 이 방에 다시 붙지 않는다
                rememberExit(id)
                // 연결이 잠깐 끊겼으면 몇 번 더 보낸다(진행자의 마치기가 닿지 않으면 남은 사람의 방이 닫히지 않는다)
                for (attempt in 1..EXIT_TRIES) {
                    val result = if (state.isHost) partyApi.finish(id) else partyApi.leave(id)
                    if (result !is ServerResult.Retry || attempt == EXIT_TRIES) break
                    delay(EXIT_RETRY_MS * attempt)
                }
            }
        }
        _party.value = state.copy(
            phase = PartyPhase.FINISHED,
            resultStartedAt = startedAt,
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
     * 거리는 서버가 크루원 모두의 크루 러닝으로 센다(`crew_leaderboard`). 서버에
     * 닿지 못하면 이 폰에서 달린 크루 러닝으로 대신 보여 준다 — 빈 표보다 낫다.
     *
     * @param period 기간. 서버에 물을 때 쓴다.
     * @param since 이 시각 이후에 시작한 러닝만 센다(서버에 못 닿았을 때). 전체기간이면 0.
     */
    suspend fun ranking(period: RankPeriod, since: Long): List<CrewRank> {
        // 화면이 열리자마자 순위를 물으면 목록을 아직 못 받았을 수 있다.
        if (_crews.value.isEmpty()) refresh()
        val server = (api.leaderboard(period.name) as? ServerResult.Ok)?.value
            ?.associateBy { it.crewId }
        val local = if (server == null) walkSessionDao.crewDistances(since).associateBy { it.crewId } else null
        return _crews.value
            .map { crew ->
                val km = server?.get(crew.id)?.km ?: ((local?.get(crew.id)?.meters ?: 0.0) / 1000.0)
                val runs = server?.get(crew.id)?.runs ?: (local?.get(crew.id)?.runs ?: 0)
                CrewRank(
                    // 번호는 정렬한 뒤에 붙인다
                    rank = 0,
                    crewId = crew.id,
                    name = crew.name,
                    monogram = crew.monogram,
                    km = km,
                    runs = runs,
                    joined = crew.joined,
                )
            }
            // 같은 거리면 이름순. 순서가 매번 흔들리면 순위표를 믿지 않게 된다.
            .sortedWith(compareByDescending<CrewRank> { it.km }.thenBy { it.name })
            .mapIndexed { index, row -> row.copy(rank = index + 1) }
    }

    companion object {
        /** 서버가 함께 달린 사람으로 세는 거리(m) — 출발 2분 뒤에도 서로 이만큼 안에서 위치를 보냈다(0056 party_ping) */
        const val NEAR_DISTANCE_M = 200

        /** 달리는 동안 이만큼(초) 소식이 없으면 이 폰의 명단에서 잠시 빠진다 */
        const val STALE_SEC = 60

        /** 출발한 지 이만큼 안이면 늦게 본 폰도 러닝을 켠다(화면이 꺼져 카운트다운을 놓친 경우). 넘으면 새 대기실 */
        const val LATE_START_MS = 120_000L

        private const val LOBBY_POLL_MS = 2_000L
        private const val RUN_POLL_MS = 5_000L
        private const val OPEN_RETRY_MS = 2_000L
        private const val OPEN_RETRY_MAX_MS = 30_000L
        private const val EXIT_TRIES = 3
        private const val EXIT_RETRY_MS = 2_000L
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

/** 서버 줄 → 크루 명함 */
fun CrewRow.toCard(): com.stepup.android.domain.CrewCard = com.stepup.android.domain.CrewCard(
    id = id,
    name = name,
    tagline = tagline,
    leaderNote = leaderNote,
    leaderId = ownerId,
    leaderName = leaderName.ifBlank { roster.firstOrNull().orEmpty() },
    imageBg = imageBg,
    imageVer = imageVer,
    hasImage = hasImage,
    area = area,
    lat = lat,
    lng = lng,
    schedule = com.stepup.android.domain.CrewSchedule(meetDays, meetTime).normalized,
    distance = com.stepup.android.domain.CrewDistance.of(runDistance),
    moods = moods.mapNotNull(com.stepup.android.domain.CrewMood::of),
    memberCount = memberCount,
    capacity = capacity,
    recruiting = recruiting,
    recruitChangedAt = recruitChangedAt.ifBlank { createdAt }.isoToMillis(),
    createdAt = createdAt.isoToMillis(),
    goalKm = weeklyGoalKm,
    level = level,
    weekKm = weekKm,
    weekRunners = weekRunners,
    joined = joined,
    requested = requested,
    owned = owned,
    pendingCount = pendingCount,
    openJoin = joinPolicy == "OPEN",
    myApplicationId = myApplicationId,
    myApplicationStatus = com.stepup.android.domain.CrewApplicationStatus.of(myApplicationStatus),
    myApplicationSeen = myApplicationSeen,
)

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
    is ServerResult.Retry -> CrewActionResult.Failed(reason, retryable = true)
    is ServerResult.Ok -> CrewActionResult.Failed("")
}
