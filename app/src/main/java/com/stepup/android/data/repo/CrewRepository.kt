package com.stepup.android.data.repo

import androidx.annotation.VisibleForTesting
import com.stepup.android.data.local.CrewDao
import com.stepup.android.data.local.CrewInfoDao
import com.stepup.android.data.local.NotificationType
import com.stepup.android.data.local.WalkSessionDao
import com.stepup.android.data.remote.CrewApi
import com.stepup.android.data.remote.CrewRequestRow
import com.stepup.android.data.remote.CrewRow
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.domain.CrewRank
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    val id: String,
    val name: String,
    /** 러너 고유 ID — "SU-XXXXXX" */
    val uid: String,
    val level: Int,
    val ready: Boolean,
    val isMe: Boolean,
    /** 파티장(나)로부터의 거리 (m, 시뮬레이션) */
    val distanceM: Int,
)

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
    val crewId: String? = null,
    /** 번개러닝에서 연 로비면 그 글의 id. 크루 로비면 null. */
    val flashPostId: Long? = null,
    val crewName: String = "",
    val members: List<PartyMember> = emptyList(),
    val countdown: Int = 0,
    val resultPoints: Double = 0.0,
    val resultSteps: Int = 0,
) {
    val readyCount: Int get() = members.count { it.ready }
    val partySize: Int get() = members.size
    val allReady: Boolean get() = members.isNotEmpty() && members.all { it.ready }
    val myReady: Boolean get() = members.firstOrNull { it.isMe }?.ready == true

    /**
     * 파티장이 지금 출발할 수 있는가.
     *
     * 전원이 준비하기를 기다리지 않는다. 한 사람이 신발 끈을 못 찾으면
     * 나머지 넷이 길에 서 있게 되고, 그 다섯 명은 다음부터 파티런을 안 쓴다.
     * 파티장 본인만 준비돼 있으면 출발할 수 있고, 준비를 마친 사람들끼리 뛴다.
     */
    val canStart: Boolean get() = myReady && readyCount >= 1
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
 * 파티런 로비는 아직 서버에 붙지 않았다. 크루원의 준비·거리는 시뮬레이션이고,
 * 실시간 연결은 다음 단계에서 붙인다.
 */
class CrewRepository(
    private val api: CrewApi,
    private val crewDao: CrewDao,
    private val crewInfoDao: CrewInfoDao,
    private val walkSessionDao: WalkSessionDao,
    private val rewardRepository: RewardRepository,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var simulationJob: Job? = null

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
    // 로비를 연 사람(나)이 파티장이다. 파티장은 크루원을 초대·강퇴할 수 있고,
    // 전원 준비되면 "시작"을 눌러 러닝을 개시한다.
    // 러닝 중 파티장에게서 MAX_PARTY_DISTANCE_M 이상 떨어진 크루원은
    // 자동으로 파티에서 빠진다(백엔드가 없으므로 거리는 시뮬레이션).

    private val _party = MutableStateFlow(PartyState())
    val party: StateFlow<PartyState> = _party

    private var driftJob: Job? = null

    /** 시뮬레이션 러너의 고유 ID — 이름에서 결정적으로 만든다 */
    private fun uidOf(name: String): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        var h = name.hashCode()
        val body = buildString {
            repeat(6) {
                append(alphabet[((h % alphabet.length) + alphabet.length) % alphabet.length])
                h = h shr 3
            }
        }
        return "SU-$body"
    }

    private fun member(index: Int, name: String, random: Random, ready: Boolean = false) =
        PartyMember(
            id = "m$index",
            name = name,
            uid = uidOf(name),
            level = 8 + random.nextInt(14),
            ready = ready,
            isMe = false,
            distanceM = 4 + random.nextInt(38),
        )

    /** 로비 입장. 크루원 일부는 이미 준비를 마친 상태로 시작한다. */
    fun openLobby(crewId: String) {
        val crew = crewOf(crewId) ?: return
        if (_party.value.crewId == crewId &&
            _party.value.phase !in listOf(PartyPhase.IDLE, PartyPhase.FINISHED)
        ) {
            return // 이미 이 크루 로비에 있음
        }
        simulationJob?.cancel()
        driftJob?.cancel()
        val random = Random(System.nanoTime())
        val squad = crew.roster.take(3 + random.nextInt(2))
        val members = buildList {
            add(PartyMember("me", "", "", 0, ready = false, isMe = true, distanceM = 0))
            squad.forEachIndexed { index, name ->
                add(member(index, name, random, ready = index == 0 && random.nextBoolean()))
            }
        }
        _party.value = PartyState(
            phase = PartyPhase.LOBBY,
            crewId = crew.id,
            crewName = crew.name,
            members = members,
        )
    }

    /**
     * 번개러닝 로비.
     *
     * 크루 로비와 같은 판을 쓴다 — 준비를 누르고, 파티장이 시작하고, 인원수만큼
     * 적립 부스트가 붙는다. 번개러닝만 이 흐름이 없으면 "모집은 되는데 같이
     * 뛸 수는 없는 글"이 되고, 그건 게시판이지 러닝 기능이 아니다.
     *
     * 크루 로비와 마찬가지로 **로비를 연 사람이 파티장**이다. 주최자만 시작할
     * 수 있게 하면, 주최자가 늦는 날 모인 사람들이 아무것도 못 한다.
     *
     * @param others 나를 뺀 참가자 수. 글의 참가 인원에서 온다.
     */
    fun openFlashLobby(postId: Long, title: String, others: Int) {
        if (_party.value.flashPostId == postId &&
            _party.value.phase !in listOf(PartyPhase.IDLE, PartyPhase.FINISHED)
        ) {
            return // 이미 이 번개 로비에 있음
        }
        simulationJob?.cancel()
        driftJob?.cancel()
        val random = Random(System.nanoTime())
        val squad = EXTRA_RUNNERS.shuffled(random).take(others.coerceIn(0, 9))
        val members = buildList {
            add(PartyMember("me", "", "", 0, ready = false, isMe = true, distanceM = 0))
            squad.forEachIndexed { index, name ->
                add(member(index, name, random, ready = index == 0 && random.nextBoolean()))
            }
        }
        _party.value = PartyState(
            phase = PartyPhase.LOBBY,
            flashPostId = postId,
            crewName = title,
            members = members,
        )
    }

    fun leaveLobby() {
        simulationJob?.cancel()
        driftJob?.cancel()
        _party.value = PartyState()
    }

    /** 파티에 없는 크루원 — 초대 후보 */
    fun inviteCandidates(): List<String> {
        val state = _party.value
        val inParty = state.members.map { it.name }.toSet()
        // 번개러닝 로비에는 크루 명단이 없다. 그때는 일반 러너 중에서 부른다.
        val roster = state.crewId?.let { crewOf(it) }?.roster.orEmpty()
        return (roster + EXTRA_RUNNERS).distinct().filterNot { it in inParty }
    }

    /** 파티장 권한 — 크루원 초대. 초대된 크루원은 잠시 후 준비를 누른다. */
    fun invite(name: String) {
        val state = _party.value
        if (state.phase != PartyPhase.LOBBY) return
        if (state.members.any { it.name == name }) return
        val random = Random(System.nanoTime())
        val newMember = member(state.members.size + 100, name, random)
        _party.value = state.copy(members = state.members + newMember)
        scope.launch {
            delay(900L + random.nextLong(1800))
            val snapshot = _party.value
            if (snapshot.phase != PartyPhase.LOBBY) return@launch
            _party.value = snapshot.copy(
                members = snapshot.members.map {
                    if (it.id == newMember.id) it.copy(ready = true) else it
                },
            )
        }
    }

    /** 파티장 권한 — 크루원 강퇴 */
    fun kick(memberId: String) {
        val state = _party.value
        if (state.phase != PartyPhase.LOBBY) return
        if (memberId == "me") return
        _party.value = state.copy(members = state.members.filterNot { it.id == memberId })
    }

    /** 내 준비 상태 토글. 준비를 누르면 남은 크루원들이 차례로 따라온다. */
    fun setMyReady(ready: Boolean) {
        val state = _party.value
        if (state.phase != PartyPhase.LOBBY) return
        _party.value = state.copy(
            members = state.members.map { if (it.isMe) it.copy(ready = ready) else it },
        )
        if (ready) startSimulation() else simulationJob?.cancel()
    }

    private fun startSimulation() {
        simulationJob?.cancel()
        simulationJob = scope.launch {
            val random = Random(System.nanoTime())
            while (true) {
                val current = _party.value
                if (current.phase != PartyPhase.LOBBY) return@launch
                val pending = current.members.filter { !it.ready && !it.isMe }
                if (pending.isEmpty()) return@launch
                delay(700L + random.nextLong(1600))
                val snapshot = _party.value
                if (snapshot.phase != PartyPhase.LOBBY || !snapshot.myReady) return@launch
                val next = snapshot.members.firstOrNull { !it.ready && !it.isMe } ?: return@launch
                _party.value = snapshot.copy(
                    members = snapshot.members.map {
                        if (it.id == next.id) it.copy(ready = true) else it
                    },
                )
            }
        }
    }

    /**
     * 파티장 권한 — 준비된 사람들끼리 시작. 카운트다운을 거쳐 동시 측정에 들어간다.
     *
     * 아직 준비하지 않은 사람은 여기서 파티에서 빠진다. 데리고 들어가면
     * 적립 부스트(인원수 배율)에는 들어가면서 실제로는 안 뛰는 사람이 생긴다 —
     * 같이 뛴 사람들 몫을 안 뛴 사람이 나눠 갖는 셈이다.
     */
    fun startParty() {
        val state = _party.value
        if (state.phase != PartyPhase.LOBBY || !state.canStart) return

        val ready = state.members.filter { it.ready }
        val leftBehind = state.members.filterNot { it.ready }
        if (leftBehind.isNotEmpty()) _party.value = state.copy(members = ready)

        simulationJob?.cancel()
        simulationJob = scope.launch {
            // 두고 간 사람은 알림으로 남긴다. 로비에 있다가 조용히 사라지면
            // 본인은 앱이 고장 난 줄 안다.
            //
            // 알림 쓰기는 suspend 라 여기 코루틴 안에서 한다. 화면이 보는
            // 파티 상태는 위에서 이미 바꿔 뒀으므로, 카운트다운은 알림을
            // 기다리지 않고 바로 시작한다.
            leftBehind.forEach {
                rewardRepository.notify(NotificationType.PARTY_MEMBER_LEFT, it.name)
            }
            for (n in 3 downTo 1) {
                val snapshot = _party.value
                if (snapshot.phase !in listOf(PartyPhase.LOBBY, PartyPhase.COUNTDOWN)) return@launch
                _party.value = snapshot.copy(phase = PartyPhase.COUNTDOWN, countdown = n)
                delay(1000)
            }
            val snapshot = _party.value
            if (snapshot.phase != PartyPhase.COUNTDOWN) return@launch
            _party.value = snapshot.copy(phase = PartyPhase.RUNNING, countdown = 0)
            startDistanceDrift()
        }
    }

    /**
     * 러닝 중 거리 시뮬레이션. 크루원 거리가 조금씩 출렁이다
     * [MAX_PARTY_DISTANCE_M]을 넘으면 자동으로 파티에서 빠지고 알림을 남긴다.
     */
    private fun startDistanceDrift() {
        driftJob?.cancel()
        driftJob = scope.launch {
            val random = Random(System.nanoTime())
            while (true) {
                delay(2500)
                val state = _party.value
                if (state.phase != PartyPhase.RUNNING) return@launch
                var dropped: PartyMember? = null
                val updated = state.members.map { m ->
                    if (m.isMe) return@map m
                    val next = (m.distanceM + random.nextInt(-9, 15)).coerceAtLeast(2)
                    m.copy(distanceM = next)
                }
                val survivors = updated.filter { m ->
                    val ok = m.isMe || m.distanceM <= MAX_PARTY_DISTANCE_M
                    if (!ok && dropped == null) dropped = m
                    ok
                }
                _party.value = state.copy(members = survivors)
                dropped?.let {
                    rewardRepository.notify(NotificationType.PARTY_MEMBER_LEFT, it.name)
                }
            }
        }
    }

    /** 세션 정산 후 결과 화면으로 전환 */
    fun finishParty(points: Double, steps: Int) {
        val state = _party.value
        if (state.phase != PartyPhase.RUNNING) return
        simulationJob?.cancel()
        driftJob?.cancel()
        _party.value = state.copy(
            phase = PartyPhase.FINISHED,
            resultPoints = points,
            resultSteps = steps,
        )
    }

    fun dismissResult() {
        if (_party.value.phase == PartyPhase.FINISHED) _party.value = PartyState()
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
        /** 파티장에게서 이만큼 떨어지면 자동으로 파티에서 빠진다 */
        const val MAX_PARTY_DISTANCE_M = 100

        /** 초대 후보를 채우는 여분 러너들 */
        private val EXTRA_RUNNERS = listOf("Riley P.", "Sena K.", "Théo M.", "Ines V.", "Milo J.")
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

/** 서버가 받아 주지 않은 결말을 화면이 쓰는 실패로 */
private fun ServerResult<*>.asFailure(): CrewActionResult.Failed = when (this) {
    is ServerResult.SignInRequired -> CrewActionResult.Failed(reason, signIn = true)
    is ServerResult.Rejected -> CrewActionResult.Failed(reason)
    is ServerResult.Retry -> CrewActionResult.Failed(reason)
    is ServerResult.Ok -> CrewActionResult.Failed("")
}
