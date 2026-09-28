package com.stepup.android.ui.screens.community.stories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.prefs.StoryRunJson
import com.stepup.android.data.repo.BoardResult
import com.stepup.android.data.repo.CommunityRepository
import com.stepup.android.domain.StoryAttachment
import com.stepup.android.domain.StoryComposeRules
import com.stepup.android.domain.StoryDraft
import com.stepup.android.domain.StoryPhrase
import com.stepup.android.domain.StoryPlace
import com.stepup.android.domain.StoryPlaceSource
import com.stepup.android.domain.StoryRecordCard
import com.stepup.android.domain.StoryRecords
import com.stepup.android.domain.StoryRun
import com.stepup.android.domain.StoryRunProblem
import com.stepup.android.domain.StoryRunRules
import com.stepup.android.domain.StoryText
import com.stepup.android.domain.storyPlace
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 글쓰기 결과 — 실패는 입력을 그대로 두고 알린다 */
enum class StoryPublishError { FAILED, SIGN_IN }

/** 목록이 "글쓰기"를 누를 때 미리 붙일 장소를 건넨다(목록에서 고른 장소). 한 번 읽으면 비운다 */
object StoryComposeSeed {
    @Volatile var place: StoryPlace? = null

    fun take(): StoryPlace? = place.also { place = null }
}

/**
 * 동네 이야기 쓰기 · 고치기 — 기록 칸(최근 러닝 첨부) · 장소 · 한 줄 이야기 버튼 · 본문.
 *
 * 코스 첨부의 가능 여부와 일반 글쓰기를 나눈다(쉬운 글쓰기 상황별 지시서 2026-09-28). 기록을 불러오지 못해도,
 * 최근 러닝이 없어도 장소와 본문이 있으면 올릴 수 있다. 본문 · 장소 · 첨부는 저장 상태에 둔다 — 장소를 고르러
 * 다녀오거나 화면을 돌려도 그대로다. 올리는 동안은 다시 누를 수 없고, 글쓰기마다 요청 키를 보내 결과를 모른 채
 * 다시 올려도 글이 두 편 생기지 않는다.
 */
class StoryComposeViewModel(
    private val repository: CommunityRepository,
    private val saved: SavedStateHandle,
    activeRun: Flow<Boolean> = flowOf(false),
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    /** 고치는 글 번호. 새 글이면 0 */
    val editingId: StateFlow<Long> = saved.getStateFlow(KEY_EDIT, saved.get<Long>(ARG_EDIT) ?: 0L)

    val text: StateFlow<String> = saved.getStateFlow(KEY_TEXT, "")

    private val placeName = saved.getStateFlow(KEY_PLACE_NAME, "")
    private val placeAddress = saved.getStateFlow(KEY_PLACE_ADDRESS, "")
    private val placeLat = saved.getStateFlow(KEY_PLACE_LAT, Double.NaN)
    private val placeLng = saved.getStateFlow(KEY_PLACE_LNG, Double.NaN)

    val place: StateFlow<StoryPlace?> = combine(placeName, placeAddress, placeLat, placeLng) { name, address, lat, lng ->
        if (name.isBlank() || lat.isNaN() || lng.isNaN()) null else StoryPlace(name, address, lat, lng)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** 장소를 어느 버튼으로 골랐는가 — 그 버튼을 고른 모습으로 보인다 */
    val placeSource: StateFlow<StoryPlaceSource?> = saved.getStateFlow(KEY_PLACE_SOURCE, "")
        .map { name -> StoryPlaceSource.entries.firstOrNull { it.name == name } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val runRaw = saved.getStateFlow(KEY_RUN, "")

    /** 붙인 러닝 */
    val attachment: StateFlow<StoryAttachment?> = runRaw.map(StoryRunJson::decode)
        .stateIn(viewModelScope, SharingStarted.Eagerly, StoryRunJson.decode(runRaw.value))

    /** 게시 때 서버가 첨부를 거절한 이유(기간 초과 · 붙일 수 없음) — 첨부를 바꾸거나 빼면 사라진다 */
    private val problem = saved.getStateFlow(KEY_PROBLEM, "")
        .map { name -> StoryRunProblem.entries.firstOrNull { it.name == name } }

    /** 고치는 글에서 러닝을 바꾸거나 뺐다 */
    private val runChanged = saved.getStateFlow(KEY_RUN_CHANGED, false)

    /** 앱이 본문에 넣은 글 — 사용자가 손대기 전까지만 장소 · 기록에 맞춰 바꾼다. 비었으면 사용자의 글이다 */
    private val autoText = saved.getStateFlow(KEY_AUTO_TEXT, "")

    /** 러닝을 붙였을 때 본문이 비어 있었다 — 기록으로 만든 기본 문장을 한 번 넣는다 */
    val autoFill: StateFlow<Boolean> = saved.getStateFlow(KEY_AUTO_FILL, false)

    /** 마지막으로 누른 한 줄 이야기 버튼 */
    val phrase: StateFlow<StoryPhrase?> = saved.getStateFlow(KEY_PHRASE, "").map { StoryPhrase.of(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, StoryPhrase.of(saved.get<String>(KEY_PHRASE)))

    private val _records = MutableStateFlow<StoryRecords>(StoryRecords.Loading)

    /** 기록 조회 — 불러오는 중 · 실패 · 결과 */
    val records: StateFlow<StoryRecords> = _records

    /** 러닝이 진행 중인가 — 진행 중인 코스는 아직 붙일 수 없다 */
    val activeRun: StateFlow<Boolean> = activeRun.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** 최근 공개 장소 — 없으면 "최근 장소" 버튼을 뺀다 */
    val recentPlaces: StateFlow<List<StoryPlace>> =
        repository.recentStoryPlaces.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** 기록 칸이 보일 모습 */
    val card: StateFlow<StoryRecordCard> = combine(attachment, problem, _records, this.activeRun) { att, trouble, records, active ->
        StoryComposeRules.card(att, trouble, records, active, StoryRunRules.today(now()))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StoryRecordCard.Loading)

    /** 처음 모습 — 이것과 달라졌을 때만 나갈 때 묻는다 */
    private val initialText = saved.getStateFlow(KEY_INITIAL_TEXT, "")
    private val initialPlaceKey = saved.getStateFlow(KEY_INITIAL_PLACE, "")
    private val initialRun = saved.getStateFlow(KEY_INITIAL_RUN, "")

    private val _ready = MutableStateFlow(saved.get<Boolean>(KEY_READY) == true)

    /** 처음 내용을 채웠다(고칠 글 · 쓰다 만 글을 읽었다) */
    val ready: StateFlow<Boolean> = _ready

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting

    private val _error = MutableStateFlow<StoryPublishError?>(null)
    val error: StateFlow<StoryPublishError?> = _error

    private var recordsJob: Job? = null
    private var recordsLoadedAt = 0L

    init {
        if (!_ready.value) viewModelScope.launch { load() }
        loadRecords()
    }

    private suspend fun load() {
        val resume = saved.get<Boolean>(ARG_RESUME) == true
        val edit = saved.get<Long>(ARG_EDIT) ?: 0L
        when {
            resume -> {
                val draft = repository.storyDraft.first()
                if (draft != null) {
                    saved[KEY_EDIT] = draft.editingId
                    setInitial(draft.text, draft.place, draft.run)
                    saved[KEY_RUN_CHANGED] = draft.runChanged
                    saved[KEY_CLIENT_KEY] = draft.clientKey.ifBlank { newKey() }
                    // 쓰다 둔 글에는 러닝을 저절로 붙이지 않는다 — 뺐던 첨부가 되살아나지 않게
                    saved[KEY_AUTO_ATTACHED] = true
                } else {
                    setInitial("", StoryComposeSeed.take(), null)
                    saved[KEY_CLIENT_KEY] = newKey()
                }
            }
            edit > 0 -> {
                val post = repository.posts.value.firstOrNull { it.id == edit }
                    ?: run { repository.refresh(); repository.posts.value.firstOrNull { it.id == edit } }
                // 이미 올린 글의 러닝은 기간이 지나도 그대로 둔다(번호 없이 — 서버가 그대로 둔다)
                if (post != null) setInitial(StoryText.join(post.title, post.body), post.storyPlace, post.run?.let { StoryAttachment(it, kept = true) })
                saved[KEY_AUTO_ATTACHED] = true
            }
            else -> {
                setInitial("", StoryComposeSeed.take(), null)
                saved[KEY_CLIENT_KEY] = newKey()
            }
        }
        saved[KEY_READY] = true
        _ready.value = true
        autoAttach()
    }

    private fun setInitial(body: String, place: StoryPlace?, run: StoryAttachment?) {
        saved[KEY_TEXT] = body
        writePlace(place)
        saved[KEY_RUN] = StoryRunJson.encode(run)
        saved[KEY_INITIAL_TEXT] = body
        saved[KEY_INITIAL_PLACE] = place?.key.orEmpty()
        saved[KEY_INITIAL_RUN] = StoryRunJson.encode(run)
    }

    private fun newKey(): String = UUID.randomUUID().toString()

    /**
     * 기록 칸을 불러온다. 앞서 받은 결과가 있으면 다시 받는 동안에도 그대로 보인다(깜빡이지 않게).
     * 실패하면 실패로 둔다 — 기록 없음으로 바꾸지 않고, 붙여 둔 러닝도 지우지 않는다.
     */
    fun loadRecords() {
        if (recordsJob?.isActive == true) return
        recordsJob = viewModelScope.launch {
            if (_records.value !is StoryRecords.Ready) _records.value = StoryRecords.Loading
            val next = repository.storyRecords()
            // 다시 받기에 실패해도 앞서 받은 결과가 있으면 그대로 둔다
            if (next is StoryRecords.Ready || _records.value !is StoryRecords.Ready) _records.value = next
            recordsLoadedAt = now()
            autoAttach()
        }
    }

    /** 다른 화면(러닝 · 지난 기록)에 다녀왔다 — 새 기록이 생겼을 수 있다 */
    fun onReturn() {
        if (now() - recordsLoadedAt > RELOAD_AFTER_MS || _records.value !is StoryRecords.Ready) loadRecords()
    }

    /** 새 글이면 붙일 수 있는 가장 최근 러닝을 한 번 붙인다(카드의 "첨부 삭제"로 뺄 수 있다) */
    private fun autoAttach() {
        if (!_ready.value || saved.get<Boolean>(KEY_AUTO_ATTACHED) == true) return
        val ready = _records.value as? StoryRecords.Ready ?: return
        saved[KEY_AUTO_ATTACHED] = true
        if (attachment.value != null || editingId.value > 0) return
        ready.options.runs.firstOrNull()?.let(::attach)
    }

    fun setText(next: String) {
        // 1,000자를 넘는 붙여넣기는 넘는 만큼 받지 않는다 — 코드포인트 경계에서 자른다
        saved[KEY_TEXT] = clamp(next)
        _error.value = null
    }

    private fun clamp(next: String): String =
        if (StoryText.length(next) <= StoryText.MAX_CHARS) next
        else next.substring(0, next.offsetByCodePoints(0, StoryText.MAX_CHARS))

    fun setPlace(next: StoryPlace?, source: StoryPlaceSource? = null) {
        writePlace(next)
        saved[KEY_PLACE_SOURCE] = source?.name.orEmpty()
        _error.value = null
    }

    private fun writePlace(next: StoryPlace?) {
        saved[KEY_PLACE_NAME] = next?.name.orEmpty()
        saved[KEY_PLACE_ADDRESS] = next?.address.orEmpty()
        saved[KEY_PLACE_LAT] = next?.lat ?: Double.NaN
        saved[KEY_PLACE_LNG] = next?.lng ?: Double.NaN
    }

    /** 러닝 붙이기(기록 변경 · 다른 기록 고르기) — 본문이 비었으면 기록으로 만든 기본 문장을 넣게 한다 */
    fun attach(run: StoryRun) {
        saved[KEY_RUN] = StoryRunJson.encode(StoryAttachment(run))
        saved[KEY_PROBLEM] = ""
        if (editingId.value > 0) saved[KEY_RUN_CHANGED] = true
        if (text.value.isBlank()) saved[KEY_AUTO_FILL] = true
        _error.value = null
    }

    /**
     * 첨부 삭제 · 코스만 빼기 — 붙인 연결만 뺀다. 원본 러닝 기록 · 장소 · 작성 문장은 지우지 않고, 그 뒤로는
     * 문장을 자동으로 바꾸지도 않는다.
     */
    fun removeRun() {
        saved[KEY_RUN] = ""
        saved[KEY_PROBLEM] = ""
        if (editingId.value > 0) saved[KEY_RUN_CHANGED] = true
        saved[KEY_AUTO_TEXT] = ""
        saved[KEY_AUTO_FILL] = false
        if (placeSource.value == StoryPlaceSource.COURSE) saved[KEY_PLACE_SOURCE] = ""
        _error.value = null
    }

    /** 한 줄 이야기 버튼 — [starter] 는 화면이 지금 기록 · 장소로 만든 문장 */
    fun applyStarter(phrase: StoryPhrase, starter: String) {
        StoryComposeRules.insertStarter(text.value, autoText.value, starter)?.let { result ->
            saved[KEY_TEXT] = clamp(result.text)
            saved[KEY_AUTO_TEXT] = result.autoText
        }
        saved[KEY_PHRASE] = phrase.name
        saved[KEY_AUTO_FILL] = false
        _error.value = null
    }

    /**
     * 화면이 지금 기록 · 장소 · 고른 버튼으로 만든 문장. 러닝을 막 붙였고 본문이 비었으면 넣고, 앱이 넣은 글을
     * 사용자가 아직 고치지 않았으면 새 문장으로 바꾼다. 사용자가 쓴 글은 건드리지 않는다.
     */
    fun syncAuto(candidate: String) {
        if (candidate.isBlank()) return
        val body = text.value
        when {
            autoFill.value && body.isBlank() -> {
                saved[KEY_TEXT] = clamp(candidate)
                saved[KEY_AUTO_TEXT] = candidate
                saved[KEY_AUTO_FILL] = false
            }
            autoText.value.isNotEmpty() && body == autoText.value && body != candidate -> {
                saved[KEY_TEXT] = clamp(candidate)
                saved[KEY_AUTO_TEXT] = candidate
            }
        }
    }

    /** 본문 · 장소 · 첨부가 처음과 달라졌는가 */
    fun changed(): Boolean =
        text.value != initialText.value || place.value?.key.orEmpty() != initialPlaceKey.value ||
            StoryRunJson.encode(attachment.value) != initialRun.value

    /**
     * 올리기 · 수정 완료 · 다시 올리기. 성공해야 [onDone] — 목록이 "올라갔어요"를 띄운다.
     * [fallback] 은 본문이 비었을 때 쓸 기록 문장(유효한 러닝만 붙였을 때).
     */
    fun submit(fallback: String, onDone: () -> Unit) {
        val shown = card.value
        val chosen = place.value ?: return
        if (_submitting.value || !_ready.value) return
        if (!StoryComposeRules.canPost(shown, true, text.value, fallback)) return
        val attached = StoryComposeRules.postableRun(shown)
        val body = text.value.ifBlank { fallback }
        val edit = editingId.value
        _submitting.value = true
        _error.value = null
        viewModelScope.launch {
            val result = if (edit > 0) {
                repository.editStory(edit, body, chosen, runChange = runChanged.value, run = attached?.takeUnless { it.kept }?.run)
            } else {
                repository.writeStory(body, chosen, run = attached?.run, clientKey = saved.get<String>(KEY_CLIENT_KEY))
            }
            _submitting.value = false
            when (result) {
                is BoardResult.Ok -> {
                    // 올렸으니 이 글의 임시저장은 필요 없다
                    val draft = repository.storyDraft.first()
                    if (draft != null && draft.editingId == edit) repository.saveStoryDraft(null)
                    onDone()
                }
                is BoardResult.Failed -> when {
                    // 쓰는 사이 기간이 지났다(자정을 넘겼다) · 붙일 수 없는 기록 — 코스만 정리하게 한다
                    result.reason.contains(RUN_EXPIRED) -> saved[KEY_PROBLEM] = StoryRunProblem.EXPIRED.name
                    result.reason.contains(RUN_INVALID) -> saved[KEY_PROBLEM] = StoryRunProblem.INVALID.name
                    else -> _error.value = if (result.signIn) StoryPublishError.SIGN_IN else StoryPublishError.FAILED
                }
            }
        }
    }

    /** 임시저장하고 나가기 — 본문 · 장소 · 첨부 · 고치던 글 번호 · 요청 키를 함께 */
    fun saveDraft(notify: Boolean = true, onSaved: () -> Unit) {
        viewModelScope.launch {
            repository.saveStoryDraft(
                StoryDraft(
                    text = text.value,
                    place = place.value,
                    editingId = editingId.value,
                    savedAt = now(),
                    run = attachment.value,
                    runChanged = runChanged.value,
                    clientKey = saved.get<String>(KEY_CLIENT_KEY).orEmpty(),
                ),
            )
            if (notify) repository.postStoryNotice(com.stepup.android.data.repo.StoryNotice.DraftSaved(editingId.value))
            onSaved()
        }
    }

    /** 삭제하고 나가기 — 이어 쓰던 임시저장도 지운다 */
    fun discard(onDone: () -> Unit) {
        viewModelScope.launch {
            val draft = repository.storyDraft.first()
            if (draft != null && draft.editingId == editingId.value) repository.saveStoryDraft(null)
            onDone()
        }
    }

    companion object {
        const val ARG_EDIT = "edit"
        const val ARG_RESUME = "resume"

        /** 서버(0046)가 첨부를 거절할 때의 고정 낱말 */
        private const val RUN_EXPIRED = "run_expired"
        private const val RUN_INVALID = "run_invalid"

        /** 다른 화면에서 돌아왔을 때 기록 칸을 다시 받는 간격 */
        private const val RELOAD_AFTER_MS = 20_000L

        private const val KEY_EDIT = "story_compose_edit"
        private const val KEY_TEXT = "story_compose_text"
        private const val KEY_PLACE_NAME = "story_compose_place_name"
        private const val KEY_PLACE_ADDRESS = "story_compose_place_address"
        private const val KEY_PLACE_LAT = "story_compose_place_lat"
        private const val KEY_PLACE_LNG = "story_compose_place_lng"
        private const val KEY_PLACE_SOURCE = "story_compose_place_source"
        private const val KEY_RUN = "story_compose_run"
        private const val KEY_PROBLEM = "story_compose_run_problem"
        private const val KEY_RUN_CHANGED = "story_compose_run_changed"
        private const val KEY_AUTO_TEXT = "story_compose_auto_text"
        private const val KEY_AUTO_FILL = "story_compose_auto_fill"
        private const val KEY_AUTO_ATTACHED = "story_compose_auto_attached"
        private const val KEY_PHRASE = "story_compose_phrase"
        private const val KEY_CLIENT_KEY = "story_compose_client_key"
        private const val KEY_INITIAL_TEXT = "story_compose_initial_text"
        private const val KEY_INITIAL_PLACE = "story_compose_initial_place"
        private const val KEY_INITIAL_RUN = "story_compose_initial_run"
        private const val KEY_READY = "story_compose_ready"

        val Factory = viewModelFactory {
            initializer {
                StoryComposeViewModel(
                    repository = ServiceLocator.communityRepository,
                    saved = createSavedStateHandle(),
                    activeRun = com.stepup.android.service.WalkSessionService.state.map { it.isActive },
                )
            }
        }
    }
}
