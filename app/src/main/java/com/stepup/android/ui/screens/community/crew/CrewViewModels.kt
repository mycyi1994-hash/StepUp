package com.stepup.android.ui.screens.community.crew

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.prefs.CrewDraftCodec
import com.stepup.android.data.repo.CrewApplied
import com.stepup.android.data.repo.CrewCardRepository
import com.stepup.android.data.repo.CrewOutcome
import com.stepup.android.data.repo.CrewSyncState
import com.stepup.android.data.repo.PlaceSearch
import com.stepup.android.data.repo.PlaceSearchResult
import com.stepup.android.domain.CrewApplication
import com.stepup.android.domain.CrewApplicationStatus
import com.stepup.android.domain.CrewArea
import com.stepup.android.domain.CrewCard
import com.stepup.android.domain.CrewDecision
import com.stepup.android.domain.CrewDraft
import com.stepup.android.domain.CrewDraftMode
import com.stepup.android.domain.CrewImageChoice
import com.stepup.android.domain.CrewLevelInfo
import com.stepup.android.domain.CrewListQuery
import com.stepup.android.domain.CrewMember
import com.stepup.android.domain.CrewPerson
import com.stepup.android.domain.CrewPhrase
import com.stepup.android.domain.CrewProblem
import com.stepup.android.domain.CrewRules
import com.stepup.android.domain.CrewSort
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.haversineMeters
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.stepup.android.data.repo.CrewPhotos
import com.stepup.android.domain.CrewGoalChoice
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 한 번 읽는 것의 상태 */
sealed interface CrewLoad<out T> {
    data object Loading : CrewLoad<Nothing>
    data class Ready<T>(val value: T) : CrewLoad<T>
    data class Failed(val problem: CrewProblem) : CrewLoad<Nothing>
}

private fun <T> CrewOutcome<T>.asLoad(): CrewLoad<T> = when (this) {
    is CrewOutcome.Ok -> CrewLoad.Ready(value)
    is CrewOutcome.Failed -> CrewLoad.Failed(problem)
}

// ─────────────────────────────────────────────────────────────
// 목록(01 · 02 · 04 · 05 · 06 · 07)
// ─────────────────────────────────────────────────────────────

class CrewListViewModel(
    private val repo: CrewCardRepository,
    private val places: PlaceSearch,
) : ViewModel() {
    val cards: StateFlow<List<CrewCard>> = repo.cards
    val sync: StateFlow<CrewSyncState> = repo.sync

    val region: StateFlow<CrewArea?> = repo.region.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val radius: StateFlow<Int> = repo.radius.stateIn(viewModelScope, SharingStarted.Eagerly, 3)
    val sort: StateFlow<CrewSort> = repo.sort.map { name -> CrewSort.entries.firstOrNull { it.name == name } ?: CrewSort.NEAR }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CrewSort.NEAR)

    private val here = MutableStateFlow<GeoPoint?>(null)
    private val _hereName = MutableStateFlow<String?>(null)

    /** 내 위치의 동네 이름("도화동") — 지역을 고르지 않았을 때 목록 위에 */
    val hereName: StateFlow<String?> = _hereName

    val query: StateFlow<CrewListQuery> = combine(region, radius, sort, here) { area, km, order, point ->
        CrewListQuery(area?.point ?: point, km, order)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CrewListQuery())

    val visible: StateFlow<List<CrewCard>> = combine(cards, query) { list, q -> CrewRules.visible(list, q) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** 아직 보지 않은 가입 결과 — 목록 위 알림 줄(푸시 없이 앱 안에서 다시 읽은 결과) */
    val results: StateFlow<List<CrewCard>> = cards.map { list -> list.filter { it.unseenResult != null } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** 만들다 둔 초안 — 모집 진입에서 이어 쓸지 묻는다(39) */
    val createDraft: StateFlow<CrewDraft?> = repo.draft(CrewDraft.KEY_CREATE)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var nameJob: Job? = null

    private var refreshedAt = 0L

    init {
        refresh()
    }

    fun refresh() {
        refreshedAt = android.os.SystemClock.elapsedRealtime()
        viewModelScope.launch { repo.refresh() }
    }

    /** 목록으로 돌아왔다 — 방금 읽었으면(30초 안) 다시 읽지 않는다 */
    fun refreshOnResume() {
        if (android.os.SystemClock.elapsedRealtime() - refreshedAt > RESUME_REFRESH_MS) refresh()
    }

    /** 화면이 받은 내 위치 — 조금 움직인 것으로는 목록을 다시 세지 않는다 */
    fun setHere(point: GeoPoint?) {
        val before = here.value
        if (point == null || before != null && haversineMeters(before, point) < 150) {
            if (point == null) here.value = null
            return
        }
        here.value = point
        nameJob?.cancel()
        nameJob = viewModelScope.launch { _hereName.value = runCatching { places.areaName(point) }.getOrNull() }
    }

    fun apply(radiusKm: Int) {
        viewModelScope.launch { repo.setRadius(radiusKm) }
    }

    fun applySort(next: CrewSort) {
        viewModelScope.launch { repo.setSort(next.name) }
    }

    /** 범위 넓히기(05) — 한 단계 넓힌다. 가장 넓으면 지역을 풀어 모든 크루를 본다 */
    fun widen() {
        viewModelScope.launch {
            val next = CrewRules.RADII.firstOrNull { it > radius.value }
            if (next != null) repo.setRadius(next) else repo.setRegion(null)
        }
    }

    companion object {
        private const val RESUME_REFRESH_MS = 30_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { CrewListViewModel(ServiceLocator.crewCards, ServiceLocator.placeSearch) }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 크루 한 곳 — 상세 · 멤버 · 사람 · 목표 · 신청 · 관리(같은 crewId 를 이어 쓴다)
// ─────────────────────────────────────────────────────────────

enum class CrewOp { APPLY, CANCEL, APPROVE, DECLINE, PAUSE, RESUME, GOAL, REMOVE, TRANSFER, LEAVE, DISSOLVE, REPORT, POLICY }

/** 누른 일의 진행 — 처리 중에는 다시 누를 수 없고, 서버가 받아 준 뒤에만 [done] */
data class CrewOpState(
    val op: CrewOp? = null,
    val running: Boolean = false,
    val problem: CrewProblem? = null,
    val done: Boolean = false,
    val payload: Any? = null,
)

class CrewScreenViewModel(
    private val repo: CrewCardRepository,
    private val saved: SavedStateHandle,
) : ViewModel() {
    val crewId: String = saved.get<String>("crewId").orEmpty()

    val card: StateFlow<CrewCard?> = repo.card(crewId).stateIn(viewModelScope, SharingStarted.Eagerly, repo.cardNow(crewId))

    private val _load = MutableStateFlow<CrewLoad<Unit>>(if (repo.cardNow(crewId) != null) CrewLoad.Ready(Unit) else CrewLoad.Loading)

    /** 상세 읽기 — 처음 읽는 동안은 67, 볼 수 없게 됐으면 [missing] */
    val load: StateFlow<CrewLoad<Unit>> = _load

    val missing: StateFlow<Boolean> = repo.missing.map { crewId in it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, crewId in repo.missing.value)

    val level = MutableStateFlow<CrewLoad<CrewLevelInfo>?>(null)
    val roster = MutableStateFlow<CrewLoad<List<CrewMember>>>(CrewLoad.Loading)
    val pending = MutableStateFlow<CrewLoad<List<CrewApplication>>>(CrewLoad.Loading)
    val person = MutableStateFlow<CrewLoad<CrewPerson>>(CrewLoad.Loading)
    val application = MutableStateFlow<CrewLoad<CrewApplication>>(CrewLoad.Loading)

    private val _op = MutableStateFlow(CrewOpState())
    val op: StateFlow<CrewOpState> = _op

    // ── 가입 신청서(14) — 고른 문구 · 한마디 · 요청 키(재시도해도 같은 신청) ──
    val phrases: StateFlow<List<String>> = saved.getStateFlow(KEY_PHRASES, ArrayList<String>())
    val message: StateFlow<String> = saved.getStateFlow(KEY_MESSAGE, "")

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            if (card.value == null) _load.value = CrewLoad.Loading
            _load.value = when (val result = repo.load(crewId)) {
                is CrewOutcome.Ok -> CrewLoad.Ready(Unit)
                is CrewOutcome.Failed -> if (card.value != null) CrewLoad.Ready(Unit) else CrewLoad.Failed(result.problem)
            }
        }
    }

    fun loadLevel() {
        level.value = CrewLoad.Loading
        viewModelScope.launch { level.value = repo.level(crewId).asLoad() }
    }

    fun closeLevel() {
        level.value = null
    }

    fun loadRoster() {
        if (roster.value !is CrewLoad.Ready) roster.value = CrewLoad.Loading
        viewModelScope.launch {
            val next = repo.roster(crewId).asLoad()
            if (next is CrewLoad.Ready || roster.value !is CrewLoad.Ready) roster.value = next
        }
    }

    fun loadPending() {
        if (pending.value !is CrewLoad.Ready) pending.value = CrewLoad.Loading
        viewModelScope.launch {
            val next = repo.pending(crewId).asLoad()
            if (next is CrewLoad.Ready || pending.value !is CrewLoad.Ready) pending.value = next
        }
    }

    fun loadPerson(userId: String) {
        person.value = CrewLoad.Loading
        viewModelScope.launch { person.value = repo.person(crewId, userId).asLoad() }
    }

    fun loadApplication(applicationId: Long) {
        if (application.value !is CrewLoad.Ready) application.value = CrewLoad.Loading
        viewModelScope.launch {
            val next = repo.application(applicationId).asLoad()
            if (next is CrewLoad.Ready || application.value !is CrewLoad.Ready) application.value = next
        }
    }

    // ── 신청서 ──

    fun togglePhrase(phrase: CrewPhrase) {
        val now = phrases.value
        saved[KEY_PHRASES] = ArrayList(if (phrase.name in now) now - phrase.name else now + phrase.name)
    }

    fun setMessage(text: String) {
        saved[KEY_MESSAGE] = if (text.length > CrewRules.MESSAGE_MAX) text.take(CrewRules.MESSAGE_MAX) else text
    }

    /** 신청 보내기 · 다시 신청 보내기 — 같은 입력과 같은 요청 키로 */
    fun apply() = run(CrewOp.APPLY) {
        var result = send()
        if ((result as? CrewOutcome.Ok)?.value == CrewApplied.Canceled) {
            // 이 요청 키의 신청은 그사이 취소됐다 — 지금 누른 신청은 새 요청 키로 보낸다
            saved[KEY_APPLY] = null
            result = send()
        }
        if (result is CrewOutcome.Ok) {
            // 보낸 신청서는 남기지 않는다 — 다음 신청은 새 요청 키
            saved[KEY_APPLY] = null
        }
        result
    }

    private suspend fun send(): CrewOutcome<CrewApplied> {
        val key = saved.get<String>(KEY_APPLY) ?: UUID.randomUUID().toString().also { saved[KEY_APPLY] = it }
        return repo.apply(crewId, phrases.value.mapNotNull(CrewPhrase::of), message.value, key)
    }

    fun cancel(applicationId: Long) = run(CrewOp.CANCEL) { repo.cancel(crewId, applicationId) }

    fun markSeen(applicationId: Long) {
        viewModelScope.launch { repo.markSeen(crewId, applicationId) }
    }

    // ── 크루장 ──

    fun decide(applicationId: Long, approve: Boolean) =
        run(if (approve) CrewOp.APPROVE else CrewOp.DECLINE) { repo.decide(crewId, applicationId, approve) }

    fun setRecruiting(open: Boolean) = run(if (open) CrewOp.RESUME else CrewOp.PAUSE) { repo.setRecruiting(crewId, open) }

    fun setGoal(km: Int?) = run(CrewOp.GOAL) { repo.setGoal(crewId, km) }

    fun removeMember(userId: String) = run(CrewOp.REMOVE) { repo.removeMember(crewId, userId) }

    fun transfer(userId: String) = run(CrewOp.TRANSFER) { repo.transfer(crewId, userId) }

    // 크루를 나가거나 해산하면 이 폰에 남은 그 크루의 채팅(대화 · 사진 · 입력 글)도 지운다 — 다시 보이지 않게
    fun leave() = run(CrewOp.LEAVE) { repo.leave(crewId).also { if (it is CrewOutcome.Ok) forgetLocal() } }

    fun dissolve() = run(CrewOp.DISSOLVE) { repo.dissolve(crewId).also { if (it is CrewOutcome.Ok) forgetLocal() } }

    /** 크루를 나갔다 · 해산했다 — 이 폰에 남은 그 크루의 채팅 · 홈을 지운다 */
    private suspend fun forgetLocal() {
        ServiceLocator.crewChat.forget(crewId)
        ServiceLocator.crewHome.forget(crewId)
    }

    fun report(reason: String, note: String) = run(CrewOp.REPORT) { repo.report(crewId, reason, note) }

    fun setOpenJoin(open: Boolean) = run(CrewOp.POLICY) { repo.setOpenJoin(crewId, open) }

    fun consumeOp() {
        _op.value = CrewOpState()
    }

    private fun run(op: CrewOp, call: suspend () -> CrewOutcome<*>) {
        if (_op.value.running) return
        _op.value = CrewOpState(op, running = true)
        viewModelScope.launch {
            _op.value = when (val result = call()) {
                is CrewOutcome.Ok -> CrewOpState(op, done = true, payload = result.value)
                is CrewOutcome.Failed -> CrewOpState(op, problem = result.problem)
            }
        }
    }

    companion object {
        private const val KEY_PHRASES = "crew_join_phrases"
        private const val KEY_MESSAGE = "crew_join_message"
        private const val KEY_APPLY = "crew_join_key"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { CrewScreenViewModel(ServiceLocator.crewCards, createSavedStateHandle()) }
        }
    }
}

/** 신청 결과 — 화면이 다음으로 갈 곳 */
internal fun CrewOpState.applied(): CrewApplied? = payload as? CrewApplied

internal fun CrewOpState.decision(): CrewDecision? = payload as? CrewDecision

internal fun CrewOpState.cancelStatus(): CrewApplicationStatus? = payload as? CrewApplicationStatus

// ─────────────────────────────────────────────────────────────
// 만들기 · 수정 초안(25 · 26 · 31 · 33 · 34 · 41 · 42 · 43)
// ─────────────────────────────────────────────────────────────

/** 고른 사진 — 읽는 중 · 자르기(28) · 읽지 못함(29) · 접근 불가(69) */
sealed interface CrewPhotoStep {
    data object Idle : CrewPhotoStep
    data object Loading : CrewPhotoStep
    data class Crop(val source: Bitmap) : CrewPhotoStep
    data object Saving : CrewPhotoStep
    data object Broken : CrewPhotoStep
    data object Denied : CrewPhotoStep
}

sealed interface CrewSave {
    data object Idle : CrewSave
    data object Saving : CrewSave
    data class Created(val crewId: String) : CrewSave
    data object Saved : CrewSave
    data class Failed(val problem: CrewProblem) : CrewSave
}

class CrewDraftViewModel(
    private val repo: CrewCardRepository,
    private val saved: SavedStateHandle,
) : ViewModel() {
    val mode: CrewDraftMode = CrewDraftMode.entries.firstOrNull { it.name == saved.get<String>("mode") } ?: CrewDraftMode.CREATE
    private val crewId: String = saved.get<String>("crewId").orEmpty()

    private val _draft = MutableStateFlow(saved.get<String>(KEY_DRAFT)?.let(CrewDraftCodec::decode) ?: CrewDraft(mode = mode, crewId = crewId))
    val draft: StateFlow<CrewDraft> = _draft

    private val initial = MutableStateFlow(saved.get<String>(KEY_INITIAL)?.let(CrewDraftCodec::decode))

    private val _ready = MutableStateFlow(saved.get<Boolean>(KEY_READY) == true)
    val ready: StateFlow<Boolean> = _ready

    /** 이 폰에 남겨 둔 수정 초안을 불러왔다 — "처음 값으로 되돌리기"를 보인다 */
    private val _restored = MutableStateFlow(saved.get<Boolean>(KEY_RESTORED) == true)
    val restored: StateFlow<Boolean> = _restored

    private val _save = MutableStateFlow<CrewSave>(CrewSave.Idle)
    val save: StateFlow<CrewSave> = _save

    /** 수정할 크루를 읽지 못했다 — 빈 칸으로 저장하지 않게, 다시 읽을 때까지 칸을 열지 않는다 */
    private val _startFailed = MutableStateFlow(false)
    val startFailed: StateFlow<Boolean> = _startFailed

    /** 크루장이 보는 크루(수정 · 정원 검사) */
    val card: StateFlow<CrewCard?> = repo.card(crewId).stateIn(viewModelScope, SharingStarted.Eagerly, repo.cardNow(crewId))

    init {
        if (!_ready.value) viewModelScope.launch { start() }
        // 모집 설정(43)의 "직접 입력"은 주간 목표 수정(46)에서 바로 바뀐다 — 돌아오면 이 화면의 목표도 서버 값으로
        if (mode == CrewDraftMode.EDIT_RECRUIT) {
            viewModelScope.launch {
                card.map { it?.goalKm }.distinctUntilChanged().collect { km -> if (_ready.value) syncGoal(km) }
            }
        }
    }

    /**
     * 서버의 목표가 바뀌었다 — 되돌릴 기준을 서버 값으로 옮긴다. 이 화면에서 목표를 건드리지 않았거나 "직접 입력"(46)에서
     * 바꾸고 돌아왔으면 이 화면의 목표도 서버 값이 된다(저장할 때 46 의 값을 이 화면에서 먼저 고른 값으로 덮지 않는다).
     */
    private fun syncGoal(km: Int?) {
        val base = initial.value ?: return
        if (base.goal.km == km) return
        val fresh: CrewGoalChoice = km?.let { CrewGoalChoice.Km(it, custom = it !in CrewRules.CREATE_GOALS) } ?: CrewGoalChoice.None
        val adopt = _draft.value.goal.km == base.goal.km || saved.get<Boolean>(KEY_GOAL_EDIT) == true
        val nextBase = base.copy(goal = fresh)
        initial.value = nextBase
        saved[KEY_INITIAL] = CrewDraftCodec.encode(nextBase)
        if (adopt) {
            saved[KEY_GOAL_EDIT] = false
            write(_draft.value.copy(goal = fresh))
        }
    }

    /** 43 "직접 입력" — 주간 목표 수정(46)으로 간다. 거기서 바뀐 목표가 돌아오면 이 화면의 목표로 받는다 */
    fun beginGoalEdit() {
        saved[KEY_GOAL_EDIT] = true
    }

    // ── 대표 사진(27 → OS 사진 선택 → 28) ──

    private val _photo = MutableStateFlow<CrewPhotoStep>(CrewPhotoStep.Idle)
    val photo: StateFlow<CrewPhotoStep> = _photo

    /** OS 사진 선택기가 준 사진을 읽는다 — 읽히면 자르기(28), 아니면 29 · 69 */
    fun loadPhoto(context: Context, uri: Uri) {
        _photo.value = CrewPhotoStep.Loading
        val app = context.applicationContext
        viewModelScope.launch {
            _photo.value = when (val loaded = CrewPhotos.load(app, uri)) {
                is CrewPhotos.Loaded.Ok -> CrewPhotoStep.Crop(loaded.bitmap)
                CrewPhotos.Loaded.Broken -> CrewPhotoStep.Broken
                CrewPhotos.Loaded.Denied -> CrewPhotoStep.Denied
            }
        }
    }

    /** 사진 선택기를 열 수 없다(기기에 없음) — 69 */
    fun photoUnavailable() {
        _photo.value = CrewPhotoStep.Denied
    }

    /** 28 "이 이미지 사용" — 틀 안의 정사각형을 512px 로 잘라 앱 안에 둔다 */
    fun applyCrop(left: Float, top: Float, side: Float) {
        val source = (_photo.value as? CrewPhotoStep.Crop)?.source ?: return
        _photo.value = CrewPhotoStep.Saving
        viewModelScope.launch {
            val file = repo.newDraftPhotoFile()
            val ok = withContext(Dispatchers.Default) {
                runCatching { CrewPhotos.save(CrewPhotos.crop(source, left, top, side), file) }.getOrDefault(false)
            }
            if (ok) {
                replaceImage(CrewImageChoice.Photo(file.path))
                _photo.value = CrewPhotoStep.Idle
            } else {
                _photo.value = CrewPhotoStep.Broken
            }
        }
    }

    fun closePhoto() {
        if (_photo.value !is CrewPhotoStep.Saving) _photo.value = CrewPhotoStep.Idle
    }

    /** 대표 이미지를 바꾼다 — 앞서 고른 사진 파일은 남겨 둔 초안이 쓰지 않으면 지운다 */
    fun replaceImage(next: CrewImageChoice) {
        val before = (_draft.value.image as? CrewImageChoice.Photo)?.path
        update { it.copy(image = next) }
        if (before != null && before != (next as? CrewImageChoice.Photo)?.path) {
            viewModelScope.launch {
                val kept = (repo.storedDraft(_draft.value.key)?.image as? CrewImageChoice.Photo)?.path
                if (kept != before) withContext(Dispatchers.IO) { runCatching { File(before).delete() } }
            }
        }
    }

    private suspend fun start() {
        val resume = saved.get<Boolean>("resume") == true
        val start: CrewDraft = when (mode) {
            CrewDraftMode.CREATE -> {
                val stored = if (resume) repo.storedDraft(CrewDraft.KEY_CREATE) else null
                stored ?: run {
                    val key = UUID.randomUUID().toString()
                    CrewDraft(mode = mode, clientKey = key, image = CrewImageChoice.Named(CrewRules.bgFor(key)))
                }
            }
            else -> {
                val card = repo.cardNow(crewId) ?: (repo.load(crewId) as? CrewOutcome.Ok)?.value
                if (card == null) {
                    _startFailed.value = true
                    return
                }
                val base = CrewDraft.edit(mode, card)
                saved[KEY_INITIAL] = CrewDraftCodec.encode(base)
                initial.value = base
                val stored = repo.storedDraft(base.key)
                if (stored != null) {
                    _restored.value = true
                    saved[KEY_RESTORED] = true
                }
                stored ?: base
            }
        }
        if (mode == CrewDraftMode.CREATE) {
            val base = CrewDraft(mode = mode, clientKey = start.clientKey, image = CrewImageChoice.Named(CrewRules.bgFor(start.clientKey)))
            saved[KEY_INITIAL] = CrewDraftCodec.encode(base)
            initial.value = base
        }
        write(start)
        saved[KEY_READY] = true
        _ready.value = true
    }

    /** 수정할 크루를 다시 읽는다 */
    fun retryStart() {
        if (_ready.value) return
        _startFailed.value = false
        viewModelScope.launch { start() }
    }

    /** 지역 검색(03)에서 고른 활동 지역 */
    fun pickArea(raw: String) {
        CrewDraftCodec.decodeArea(raw)?.let { area -> update { it.copy(area = area) } }
    }

    fun update(change: (CrewDraft) -> CrewDraft) {
        write(change(_draft.value))
        if (_save.value is CrewSave.Failed) _save.value = CrewSave.Idle
    }

    private fun write(next: CrewDraft) {
        _draft.value = next
        saved[KEY_DRAFT] = CrewDraftCodec.encode(next)
    }

    /** 단계만 옮긴다 — 저장 실패 표시는 칸을 고칠 때까지 남긴다(그 칸이 있는 단계로 옮겨 보여 준다) */
    fun step(next: Int) = write(_draft.value.copy(step = next.coerceIn(0, 3)))

    /** 처음과 달라졌는가 — 나갈 때 초안을 남길지 묻는다(38) */
    val changed: Boolean
        get() = initial.value?.let { base -> strip(base) != strip(_draft.value) } ?: (_draft.value.name.isNotBlank())

    private fun strip(d: CrewDraft) = d.copy(step = 0, savedAt = 0L)

    /** 저장 후 나가기(38) */
    fun keepDraft(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.saveDraft(_draft.value)
            onDone()
        }
    }

    /** 처음 값으로 되돌리기 — 남겨 둔 수정 초안을 지운다 */
    fun revert() {
        val base = initial.value ?: return
        viewModelScope.launch { repo.deleteDraft(_draft.value) }
        write(base)
        _restored.value = false
        saved[KEY_RESTORED] = false
    }

    /** 만들기 · 저장 — 실패하면 입력과 이미지를 그대로 두고 같은 저장을 다시 한다(37) */
    fun submit() {
        if (_save.value is CrewSave.Saving) return
        _save.value = CrewSave.Saving
        viewModelScope.launch {
            val current = _draft.value
            _save.value = if (mode == CrewDraftMode.CREATE) {
                when (val result = repo.create(current)) {
                    is CrewOutcome.Ok -> CrewSave.Created(result.value)
                    is CrewOutcome.Failed -> CrewSave.Failed(result.problem)
                }
            } else {
                when (val result = repo.saveEdit(current, goalChanged = current.goal.km != initial.value?.goal?.km)) {
                    is CrewOutcome.Ok -> CrewSave.Saved
                    is CrewOutcome.Failed -> CrewSave.Failed(result.problem)
                }
            }
        }
    }

    fun consumeSave() {
        if (_save.value !is CrewSave.Saving) _save.value = CrewSave.Idle
    }

    /** 새 사진 파일 자리 */
    fun newPhotoFile(): java.io.File = repo.newDraftPhotoFile()

    companion object {
        private const val KEY_DRAFT = "crew_draft"
        private const val KEY_INITIAL = "crew_draft_initial"
        private const val KEY_READY = "crew_draft_ready"
        private const val KEY_RESTORED = "crew_draft_restored"
        private const val KEY_GOAL_EDIT = "crew_draft_goal_edit"

        /**
         * 지역 검색이 돌려주는 값(인코딩한 CrewArea). 이 화면이 올라간 길(NavBackStackEntry)의 savedStateHandle 에 온다 —
         * 뷰모델의 SavedStateHandle 과는 다른 곳이라 화면([CrewDraftRoute])이 받아 [pickArea] 로 넘긴다.
         */
        const val PICK_REGION = "crew_pick_region"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { CrewDraftViewModel(ServiceLocator.crewCards, createSavedStateHandle()) }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 지역 검색(03 · 71 · 72)
// ─────────────────────────────────────────────────────────────

sealed interface CrewRegionState {
    data object Idle : CrewRegionState
    data object Searching : CrewRegionState
    data class Found(val areas: List<CrewArea>) : CrewRegionState
    data object Empty : CrewRegionState
    data object Offline : CrewRegionState
}

class CrewRegionViewModel(private val places: PlaceSearch) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _state = MutableStateFlow<CrewRegionState>(CrewRegionState.Idle)
    val state: StateFlow<CrewRegionState> = _state

    private var job: Job? = null

    fun setQuery(text: String) {
        _query.value = text
        search(delayMs = 350)
    }

    fun search(delayMs: Long = 0) {
        job?.cancel()
        val text = _query.value.trim()
        if (text.isEmpty()) {
            _state.value = CrewRegionState.Idle
            return
        }
        job = viewModelScope.launch {
            if (delayMs > 0) delay(delayMs)
            _state.value = CrewRegionState.Searching
            _state.value = when (val result = places.regions(text, null)) {
                is PlaceSearchResult.Found ->
                    if (result.places.isEmpty()) CrewRegionState.Empty
                    else CrewRegionState.Found(result.places.map { CrewArea(it.name.take(CrewRules.AREA_MAX), it.address, it.lat, it.lng) })
                PlaceSearchResult.Offline -> CrewRegionState.Offline
            }
        }
    }

    /**
     * 현재 위치의 동네 — 이름을 못 찾으면 null(좌표를 이름처럼 적지 않는다). 좌표는 폰이 있던 자리가 아니라 그 동네의
     * 중심점이다: 크루 목록은 누구나 보므로, 크루장이 서 있던 자리(대개 집)를 크루 위치로 남기지 않는다.
     */
    suspend fun areaAt(point: GeoPoint): CrewArea? =
        runCatching { places.area(point) }.getOrNull()?.let { CrewArea(it.name.take(CrewRules.AREA_MAX), "", it.lat, it.lng) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { CrewRegionViewModel(ServiceLocator.placeSearch) }
        }
    }
}
