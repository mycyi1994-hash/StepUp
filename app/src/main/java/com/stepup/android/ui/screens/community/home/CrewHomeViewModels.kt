package com.stepup.android.ui.screens.community.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.repo.CrewCardRepository
import com.stepup.android.data.repo.CrewHomeRepository
import com.stepup.android.data.repo.CrewOutcome
import com.stepup.android.data.repo.HomeOutcome
import com.stepup.android.data.repo.HomeProblem
import com.stepup.android.domain.CrewCard
import com.stepup.android.domain.CrewHomeSummary
import com.stepup.android.domain.CrewMeeting
import com.stepup.android.domain.CrewMember
import com.stepup.android.domain.CrewPerson
import com.stepup.android.domain.CrewProblem
import com.stepup.android.domain.CrewRun
import com.stepup.android.domain.CrewRunDetail
import com.stepup.android.domain.CrewRunsScope
import com.stepup.android.domain.CrewWeek
import com.stepup.android.domain.MeetingAttendee
import com.stepup.android.domain.MeetingResponse
import java.time.LocalDate
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 한 화면의 읽기 — 처음 읽는 동안은 자리만(이전 크루 · 다른 모임의 값을 섞지 않는다) */
sealed interface HomeLoad<out T> {
    data object Loading : HomeLoad<Nothing>
    data class Ready<T>(val value: T) : HomeLoad<T>
    data object Failed : HomeLoad<Nothing>

    /** 모임이 취소(삭제)됐다 · 기록을 볼 수 없다 */
    data object Missing : HomeLoad<Nothing>
}

/** 확인 시트(크루 나가기) */
data class HomeConfirm(val busy: Boolean = false, val error: CrewProblem? = null)

private fun SavedStateHandle.crewId(): String = get<String>("crewId").orEmpty()

private fun String?.day(): LocalDate? = this?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

// ─────────────────────────────────────────────────────────────
// 00 홈 · 22 더보기(크루 나가기) · 23 모임 없음 · 26 불러오기 실패 · 28 · 29
// ─────────────────────────────────────────────────────────────

class CrewHomeViewModel(
    private val repo: CrewHomeRepository,
    private val cards: CrewCardRepository,
    saved: SavedStateHandle,
) : ViewModel() {
    val crewId: String = saved.crewId()

    val card: StateFlow<CrewCard?> = cards.card(crewId).stateIn(viewModelScope, SharingStarted.Eagerly, cards.cardNow(crewId))

    /** 크루마다 마지막으로 받은 홈 — 참석 응답이 저장되면 서버가 준 모임으로 바뀐다(28 · 29) */
    val summary: StateFlow<CrewHomeSummary?> = repo.home(crewId).stateIn(viewModelScope, SharingStarted.Eagerly, repo.homeNow(crewId))

    private val _failed = MutableStateFlow(false)

    /** 처음 읽기가 실패했다(보일 값이 없다) — 26 */
    val failed: StateFlow<Boolean> = _failed

    private val _stale = MutableStateFlow(false)

    /** 다시 읽기가 실패했다 — 있던 값은 그대로 두고 다시 읽기를 안내한다 */
    val stale: StateFlow<Boolean> = _stale

    private val _ended = MutableStateFlow(false)

    /** 이 크루의 멤버가 아니게 됐다(탈퇴 · 내보내기 · 해산) — 접근 종료 */
    val ended: StateFlow<Boolean> = _ended

    val loading = MutableStateFlow(false)

    val leave = MutableStateFlow<HomeConfirm?>(null)

    private val _left = MutableStateFlow(false)

    /** 22 → 61 크루 나가기가 끝났다 */
    val left: StateFlow<Boolean> = _left

    fun refresh() {
        if (loading.value) return
        loading.value = true
        _failed.value = false
        viewModelScope.launch {
            val crew = async { cards.load(crewId) }
            val home = async { repo.loadHome(crewId) }
            val crewResult = crew.await()
            val homeResult = home.await()
            loading.value = false
            when {
                crewResult is CrewOutcome.Ok && crewResult.value == null -> {
                    repo.forget(crewId)
                    ServiceLocator.crewChat.forget(crewId)
                    _ended.value = true
                }
                homeResult is HomeOutcome.Ended -> _ended.value = true
                homeResult is HomeOutcome.Ok && crewResult is CrewOutcome.Ok -> _stale.value = false
                summary.value != null && card.value != null -> _stale.value = true
                else -> _failed.value = true
            }
        }
    }

    fun askLeave() {
        leave.value = HomeConfirm()
    }

    fun closeLeave() {
        if (leave.value?.busy != true) leave.value = null
    }

    fun confirmLeave() {
        if (leave.value?.busy == true) return
        leave.value = HomeConfirm(busy = true)
        viewModelScope.launch {
            when (val outcome = cards.leave(crewId)) {
                is CrewOutcome.Ok -> {
                    // 이 폰에 남은 그 크루의 채팅 · 홈을 지운다 — 다시 보이지 않게
                    ServiceLocator.crewChat.forget(crewId)
                    repo.forget(crewId)
                    leave.value = null
                    _left.value = true
                }
                is CrewOutcome.Failed -> leave.value = HomeConfirm(error = outcome.problem)
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { CrewHomeViewModel(ServiceLocator.crewHome, ServiceLocator.crewCards, createSavedStateHandle()) }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 01 소개 · 03 레벨 · 04 크루원 — 공개 값(명함 · 명단)만 읽는 화면도 보일 때마다 지금의 가입 여부를 본다
// ─────────────────────────────────────────────────────────────

class CrewHomeAccessViewModel(
    private val repo: CrewHomeRepository,
    saved: SavedStateHandle,
) : ViewModel() {
    val crewId: String = saved.crewId()

    private val _ended = MutableStateFlow(false)

    /** 탈퇴 · 내보내기 · 해산 — 접근 종료로 */
    val ended: StateFlow<Boolean> = _ended

    /** 홈 한 번 읽기(crew_home)로 확인한다 — 받은 홈은 돌아갈 홈의 값도 새로 한다. 연결 실패는 그대로 둔다 */
    fun check() {
        viewModelScope.launch {
            if (repo.loadHome(crewId) == HomeOutcome.Ended) _ended.value = true
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { CrewHomeAccessViewModel(ServiceLocator.crewHome, createSavedStateHandle()) }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 04 크루원 · 27 이름 검색
// ─────────────────────────────────────────────────────────────

class CrewHomeMembersViewModel(
    private val repo: CrewHomeRepository,
    private val cards: CrewCardRepository,
    private val saved: SavedStateHandle,
) : ViewModel() {
    val crewId: String = saved.crewId()
    val card: StateFlow<CrewCard?> = cards.card(crewId).stateIn(viewModelScope, SharingStarted.Eagerly, cards.cardNow(crewId))
    val roster = MutableStateFlow<HomeLoad<List<CrewMember>>>(HomeLoad.Loading)
    val me = MutableStateFlow("")

    /** 검색어 — 화면을 돌려도 남는다 */
    val query: StateFlow<String> = saved.getStateFlow(KEY_QUERY, "")

    fun setQuery(text: String) {
        saved[KEY_QUERY] = text.replace('\n', ' ').take(QUERY_MAX)
    }

    fun load() {
        viewModelScope.launch {
            me.value = repo.me()
            val next = when (val outcome = cards.roster(crewId)) {
                is CrewOutcome.Ok -> HomeLoad.Ready(outcome.value)
                is CrewOutcome.Failed -> HomeLoad.Failed
            }
            // 다시 읽기가 실패하면 있던 목록을 그대로 둔다
            if (next is HomeLoad.Ready || roster.value !is HomeLoad.Ready) roster.value = next
        }
    }

    companion object {
        private const val KEY_QUERY = "home_members_query"
        private const val QUERY_MAX = 40

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { CrewHomeMembersViewModel(ServiceLocator.crewHome, ServiceLocator.crewCards, createSavedStateHandle()) }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 05 크루장 · 06 크루원 프로필 — 고른 사람(userId)의 공개 정보만
// ─────────────────────────────────────────────────────────────

class CrewHomePersonViewModel(
    private val repo: CrewHomeRepository,
    private val cards: CrewCardRepository,
    saved: SavedStateHandle,
) : ViewModel() {
    val crewId: String = saved.crewId()
    val userId: String = saved.get<String>("userId").orEmpty()
    val card: StateFlow<CrewCard?> = cards.card(crewId).stateIn(viewModelScope, SharingStarted.Eagerly, cards.cardNow(crewId))
    val person = MutableStateFlow<HomeLoad<CrewPerson>>(HomeLoad.Loading)
    val lastRun = MutableStateFlow<HomeLoad<CrewRun?>>(HomeLoad.Loading)

    private val _ended = MutableStateFlow(false)
    val ended: StateFlow<Boolean> = _ended

    fun load() {
        viewModelScope.launch {
            val next = when (val outcome = cards.person(crewId, userId)) {
                is CrewOutcome.Ok -> HomeLoad.Ready(outcome.value)
                is CrewOutcome.Failed -> HomeLoad.Failed
            }
            if (next is HomeLoad.Ready || person.value !is HomeLoad.Ready) person.value = next
        }
        viewModelScope.launch {
            when (val outcome = repo.lastRun(crewId, userId)) {
                is HomeOutcome.Ok -> lastRun.value = HomeLoad.Ready(outcome.value)
                HomeOutcome.Ended -> _ended.value = true
                else -> if (lastRun.value !is HomeLoad.Ready) lastRun.value = HomeLoad.Failed
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { CrewHomePersonViewModel(ServiceLocator.crewHome, ServiceLocator.crewCards, createSavedStateHandle()) }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 09 다음 러닝 · 10 참석 여부 변경 · 11 · 12 · 25 저장 실패 · 14 모이는 장소
// ─────────────────────────────────────────────────────────────

class CrewMeetingViewModel(
    private val repo: CrewHomeRepository,
    saved: SavedStateHandle,
) : ViewModel() {
    val crewId: String = saved.crewId()
    val meetingId: Long = saved.get<Long>("meetingId") ?: 0L

    /** 처음에는 홈이 들고 있던 같은 모임을 먼저 보인다(다른 모임을 섞지 않는다) */
    val meeting = MutableStateFlow<HomeLoad<CrewMeeting>>(
        repo.homeNow(crewId)?.meeting?.takeIf { it.id == meetingId }?.let { HomeLoad.Ready(it) } ?: HomeLoad.Loading,
    )

    /** 저장 중인 응답 — 그동안 두 버튼을 잠시 끄고 누른 버튼에 "저장 중…" */
    val saving = MutableStateFlow<MeetingResponse?>(null)

    /** 저장 실패(25) — 이전 응답 · 인원은 그대로 */
    val error = MutableStateFlow<HomeProblem?>(null)

    /** 10 참석 여부 변경 시트 */
    val sheet = MutableStateFlow(false)

    private val _ended = MutableStateFlow(false)
    val ended: StateFlow<Boolean> = _ended

    /** 응답을 보낸 횟수 — 그보다 먼저 시작한 읽기가 늦게 와도 저장된 응답을 덮지 않는다 */
    private var responses = 0

    fun load() {
        val asked = responses
        viewModelScope.launch {
            val outcome = repo.meeting(crewId, meetingId)
            if (asked != responses) return@launch
            when (outcome) {
                is HomeOutcome.Ok -> meeting.value = HomeLoad.Ready(outcome.value)
                HomeOutcome.Missing -> meeting.value = HomeLoad.Missing
                HomeOutcome.Ended -> _ended.value = true
                is HomeOutcome.Failed -> if (meeting.value !is HomeLoad.Ready) meeting.value = HomeLoad.Failed
            }
        }
    }

    /** 참석 · 불참 — 서버가 받은 뒤에만 바뀐다. 같은 응답을 다시 골라도 서버가 그대로 둔다 */
    fun respond(attend: Boolean) {
        if (saving.value != null || meeting.value !is HomeLoad.Ready) return
        responses++
        saving.value = if (attend) MeetingResponse.YES else MeetingResponse.NO
        error.value = null
        viewModelScope.launch {
            when (val outcome = repo.respond(crewId, meetingId, attend)) {
                is HomeOutcome.Ok -> {
                    meeting.value = HomeLoad.Ready(outcome.value)
                    sheet.value = false
                }
                HomeOutcome.Missing -> {
                    meeting.value = HomeLoad.Missing
                    sheet.value = false
                }
                HomeOutcome.Ended -> _ended.value = true
                is HomeOutcome.Failed -> {
                    error.value = outcome.problem
                    // 이미 시작했다 · 정원이 찼다 — 서버의 지금 상태(접수 마감 · 인원)를 다시 받아 보인다
                    if (outcome.problem == HomeProblem.CLOSED || outcome.problem == HomeProblem.FULL) {
                        (repo.meeting(crewId, meetingId) as? HomeOutcome.Ok)?.let { meeting.value = HomeLoad.Ready(it.value) }
                    }
                }
            }
            saving.value = null
        }
    }

    fun openSheet() {
        error.value = null
        sheet.value = true
    }

    /** 닫기 · 시스템 뒤로 — 저장하지 않고 원래 상태로 */
    fun closeSheet() {
        if (saving.value != null) return
        sheet.value = false
        error.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { CrewMeetingViewModel(ServiceLocator.crewHome, createSavedStateHandle()) }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 13 참석자
// ─────────────────────────────────────────────────────────────

class CrewAttendeesViewModel(
    private val repo: CrewHomeRepository,
    saved: SavedStateHandle,
) : ViewModel() {
    val crewId: String = saved.crewId()
    val meetingId: Long = saved.get<Long>("meetingId") ?: 0L
    val attendees = MutableStateFlow<HomeLoad<List<MeetingAttendee>>>(HomeLoad.Loading)
    val meeting = MutableStateFlow<CrewMeeting?>(repo.homeNow(crewId)?.meeting?.takeIf { it.id == meetingId })

    private val _ended = MutableStateFlow(false)
    val ended: StateFlow<Boolean> = _ended

    fun load() {
        viewModelScope.launch {
            (repo.meeting(crewId, meetingId) as? HomeOutcome.Ok)?.let { meeting.value = it.value }
            when (val outcome = repo.attendees(crewId, meetingId)) {
                is HomeOutcome.Ok -> attendees.value = HomeLoad.Ready(outcome.value)
                HomeOutcome.Missing -> attendees.value = HomeLoad.Missing
                HomeOutcome.Ended -> _ended.value = true
                is HomeOutcome.Failed -> if (attendees.value !is HomeLoad.Ready) attendees.value = HomeLoad.Failed
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { CrewAttendeesViewModel(ServiceLocator.crewHome, createSavedStateHandle()) }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 15 주간 기록 · 16 주간 선택 · 17 지난주 · 24 기록 없음
// ─────────────────────────────────────────────────────────────

class CrewWeekViewModel(
    private val repo: CrewHomeRepository,
    private val cards: CrewCardRepository,
    private val saved: SavedStateHandle,
) : ViewModel() {
    val crewId: String = saved.crewId()
    val card: StateFlow<CrewCard?> = cards.card(crewId).stateIn(viewModelScope, SharingStarted.Eagerly, cards.cardNow(crewId))
    val week = MutableStateFlow<HomeLoad<CrewWeek>>(HomeLoad.Loading)

    /** 고른 주 — 비어 있으면 이번 주. 다른 화면에 다녀와도 고른 주가 남는다 */
    val selected: StateFlow<String> = saved.getStateFlow(KEY_WEEK, saved.get<String>("week").orEmpty())

    val sheet = MutableStateFlow(false)

    private val _ended = MutableStateFlow(false)
    val ended: StateFlow<Boolean> = _ended

    fun load() {
        val start = selected.value.day()
        viewModelScope.launch {
            when (val outcome = repo.week(crewId, start)) {
                is HomeOutcome.Ok -> if (selected.value.day() == start) week.value = HomeLoad.Ready(outcome.value)
                HomeOutcome.Ended -> _ended.value = true
                else -> if (selected.value.day() == start) {
                    val shown = (week.value as? HomeLoad.Ready)?.value
                    // 읽기 오류를 0km 로 보이지 않는다 — 고른 주의 값이 없으면 실패 화면(다른 주를 읽던 늦은 실패는 버린다)
                    if (shown == null || (start != null && shown.start != start)) week.value = HomeLoad.Failed
                }
            }
        }
    }

    /** 16 에서 고른 주 — 제목 · 수치 · 목록이 함께 바뀐다 */
    fun select(start: LocalDate) {
        sheet.value = false
        val current = (week.value as? HomeLoad.Ready)?.value
        if (current?.start == start) return
        saved[KEY_WEEK] = start.toString()
        week.value = HomeLoad.Loading
        load()
    }

    companion object {
        private const val KEY_WEEK = "home_week"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { CrewWeekViewModel(ServiceLocator.crewHome, ServiceLocator.crewCards, createSavedStateHandle()) }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 18 참여 기록 — 고른 주 전체 · 한 날 · 한 사람
// ─────────────────────────────────────────────────────────────

class CrewRunsViewModel(
    private val repo: CrewHomeRepository,
    private val saved: SavedStateHandle,
) : ViewModel() {
    val crewId: String = saved.crewId()

    /** 사람으로 고른 경우의 이름(목록 제목) */
    val name: String = saved.get<String>("name").orEmpty()

    private val _scope = MutableStateFlow(
        CrewRunsScope(
            week = (saved.get<String>(KEY_WEEK) ?: saved.get<String>("week")).day(),
            day = (saved.get<String>(KEY_DAY) ?: saved.get<String>("day")).day(),
            userId = (saved.get<String>(KEY_USER) ?: saved.get<String>("userId"))?.takeIf { it.isNotBlank() },
        ),
    )
    val scope: StateFlow<CrewRunsScope> = _scope

    val week = MutableStateFlow<CrewWeek?>(null)
    val runs = MutableStateFlow<HomeLoad<List<CrewRun>>>(HomeLoad.Loading)

    /** 뒤에 더 있을 수 있다(한 장이 가득 찼다) */
    val more = MutableStateFlow(false)
    val loadingMore = MutableStateFlow(false)
    val sheet = MutableStateFlow(false)

    private val _ended = MutableStateFlow(false)
    val ended: StateFlow<Boolean> = _ended

    fun load() {
        val scope = _scope.value
        viewModelScope.launch {
            when (val outcome = repo.week(crewId, scope.week)) {
                is HomeOutcome.Ok -> if (_scope.value == scope) week.value = outcome.value
                HomeOutcome.Ended -> _ended.value = true
                else -> Unit
            }
        }
        // 다시 보일 때(러닝 기록에서 돌아옴)는 보이던 만큼 한 번에 다시 읽는다 — 첫 장으로 줄어 보던 자리를 잃지 않게
        val shown = (runs.value as? HomeLoad.Ready)?.value?.size ?: 0
        val limit = shown.coerceIn(CrewHomeRepository.PAGE, CrewHomeRepository.MAX_PAGE)
        viewModelScope.launch {
            when (val outcome = repo.runs(crewId, scope, after = null, limit = limit)) {
                is HomeOutcome.Ok -> if (_scope.value == scope) {
                    runs.value = HomeLoad.Ready(outcome.value)
                    more.value = outcome.value.size >= limit
                }
                HomeOutcome.Ended -> _ended.value = true
                else -> if (_scope.value == scope && runs.value !is HomeLoad.Ready) runs.value = HomeLoad.Failed
            }
        }
    }

    /** 끝에 닿으면 다음 장 — 마지막 줄 다음부터(겹치거나 빠지지 않는다) */
    fun loadMore() {
        val shown = (runs.value as? HomeLoad.Ready)?.value ?: return
        if (!more.value || loadingMore.value || shown.isEmpty()) return
        val scope = _scope.value
        loadingMore.value = true
        viewModelScope.launch {
            when (val outcome = repo.runs(crewId, scope, after = shown.last())) {
                is HomeOutcome.Ok -> if (_scope.value == scope) {
                    val known = shown.mapTo(HashSet()) { it.id }
                    runs.value = HomeLoad.Ready(shown + outcome.value.filter { it.id !in known })
                    more.value = outcome.value.size >= CrewHomeRepository.PAGE
                }
                HomeOutcome.Ended -> _ended.value = true
                else -> Unit
            }
            loadingMore.value = false
        }
    }

    /** 16 기간 선택 — 그 주의 전체 기록으로(고른 날 · 사람은 풀린다) */
    fun selectWeek(start: LocalDate) {
        sheet.value = false
        val next = CrewRunsScope(week = start)
        if (next == _scope.value) return
        _scope.value = next
        saved[KEY_WEEK] = start.toString()
        saved[KEY_DAY] = ""
        saved[KEY_USER] = ""
        week.value = null
        runs.value = HomeLoad.Loading
        more.value = false
        load()
    }

    companion object {
        private const val KEY_WEEK = "home_runs_week"
        private const val KEY_DAY = "home_runs_day"
        private const val KEY_USER = "home_runs_user"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { CrewRunsViewModel(ServiceLocator.crewHome, createSavedStateHandle()) }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 19 러닝 기록
// ─────────────────────────────────────────────────────────────

class CrewRunViewModel(
    private val repo: CrewHomeRepository,
    saved: SavedStateHandle,
) : ViewModel() {
    val crewId: String = saved.crewId()
    val runId: Long = saved.get<Long>("runId") ?: 0L
    val run = MutableStateFlow<HomeLoad<CrewRunDetail>>(HomeLoad.Loading)

    private val _ended = MutableStateFlow(false)
    val ended: StateFlow<Boolean> = _ended

    fun load() {
        viewModelScope.launch {
            when (val outcome = repo.run(crewId, runId)) {
                is HomeOutcome.Ok -> run.value = HomeLoad.Ready(outcome.value)
                HomeOutcome.Missing -> run.value = HomeLoad.Missing
                HomeOutcome.Ended -> _ended.value = true
                is HomeOutcome.Failed -> if (run.value !is HomeLoad.Ready) run.value = HomeLoad.Failed
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { CrewRunViewModel(ServiceLocator.crewHome, createSavedStateHandle()) }
        }
    }
}
