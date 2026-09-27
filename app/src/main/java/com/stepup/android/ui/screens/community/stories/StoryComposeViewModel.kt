package com.stepup.android.ui.screens.community.stories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.repo.BoardResult
import com.stepup.android.data.repo.CommunityRepository
import com.stepup.android.domain.StoryDraft
import com.stepup.android.domain.StoryPlace
import com.stepup.android.domain.StoryText
import com.stepup.android.domain.storyPlace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 글쓰기 결과 — 실패는 입력을 그대로 두고 알린다 */
enum class StoryPublishError { FAILED, SIGN_IN }

/** 목록이 "글쓰기"를 누를 때 미리 붙일 장소를 건넨다(고른 장소 · 가까운 글 장소). 한 번 읽으면 비운다 */
object StoryComposeSeed {
    @Volatile var place: StoryPlace? = null

    fun take(): StoryPlace? = place.also { place = null }
}

/**
 * 동네 이야기 쓰기 · 고치기 — 본문 한 칸과 장소만. 제목 · 분류를 먼저 고르게 하지 않는다.
 *
 * 본문 · 장소는 저장 상태에 둔다 — 장소를 고르러 다녀오거나 화면을 돌려도 그대로다.
 * 올리는 동안은 다시 누를 수 없다(같은 글이 두 번 올라가지 않게).
 */
class StoryComposeViewModel(
    private val repository: CommunityRepository,
    private val saved: SavedStateHandle,
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

    /** 처음 모습 — 이것과 달라졌을 때만 나갈 때 묻는다 */
    private val initialText = saved.getStateFlow(KEY_INITIAL_TEXT, "")
    private val initialPlaceKey = saved.getStateFlow(KEY_INITIAL_PLACE, "")

    private val _ready = MutableStateFlow(saved.get<Boolean>(KEY_READY) == true)

    /** 처음 내용을 채웠다(고칠 글 · 쓰다 만 글을 읽었다) */
    val ready: StateFlow<Boolean> = _ready

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting

    private val _error = MutableStateFlow<StoryPublishError?>(null)
    val error: StateFlow<StoryPublishError?> = _error

    init {
        if (!_ready.value) viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val resume = saved.get<Boolean>(ARG_RESUME) == true
        val edit = saved.get<Long>(ARG_EDIT) ?: 0L
        when {
            resume -> {
                val draft = repository.storyDraft.first()
                if (draft != null) {
                    saved[KEY_EDIT] = draft.editingId
                    setInitial(draft.text, draft.place)
                    // 이어 쓰는 글은 처음부터 "쓰다 만" 상태다 — 그대로 나가도 임시저장은 남는다
                    saved[KEY_INITIAL_TEXT] = draft.text
                    saved[KEY_INITIAL_PLACE] = draft.place?.key.orEmpty()
                } else {
                    setInitial("", StoryComposeSeed.take())
                }
            }
            edit > 0 -> {
                val post = repository.posts.value.firstOrNull { it.id == edit }
                    ?: run { repository.refresh(); repository.posts.value.firstOrNull { it.id == edit } }
                if (post != null) setInitial(StoryText.join(post.title, post.body), post.storyPlace)
            }
            else -> setInitial("", StoryComposeSeed.take())
        }
        saved[KEY_READY] = true
        _ready.value = true
    }

    private fun setInitial(body: String, place: StoryPlace?) {
        saved[KEY_TEXT] = body
        setPlace(place)
        saved[KEY_INITIAL_TEXT] = body
        saved[KEY_INITIAL_PLACE] = place?.key.orEmpty()
    }

    fun setText(next: String) {
        // 1,000자를 넘는 붙여넣기는 넘는 만큼 받지 않는다 — 코드포인트 경계에서 자른다
        saved[KEY_TEXT] = if (StoryText.length(next) <= StoryText.MAX_CHARS) next
        else next.substring(0, next.offsetByCodePoints(0, StoryText.MAX_CHARS))
        _error.value = null
    }

    fun setPlace(next: StoryPlace?) {
        saved[KEY_PLACE_NAME] = next?.name.orEmpty()
        saved[KEY_PLACE_ADDRESS] = next?.address.orEmpty()
        saved[KEY_PLACE_LAT] = next?.lat ?: Double.NaN
        saved[KEY_PLACE_LNG] = next?.lng ?: Double.NaN
        _error.value = null
    }

    /** 본문이나 장소가 처음과 달라졌는가 */
    fun changed(): Boolean =
        text.value != initialText.value || place.value?.key.orEmpty() != initialPlaceKey.value

    fun canSubmit(): Boolean = StoryText.canPost(text.value) && place.value != null && !_submitting.value

    /** 올리기 · 수정 완료. 성공해야 [onDone] — 목록이 "올라갔어요"를 띄운다 */
    fun submit(onDone: () -> Unit) {
        if (!canSubmit()) return
        val body = text.value
        val chosen = place.value ?: return
        val edit = editingId.value
        _submitting.value = true
        _error.value = null
        viewModelScope.launch {
            val result = if (edit > 0) repository.editStory(edit, body, chosen) else repository.writeStory(body, chosen)
            _submitting.value = false
            when (result) {
                is BoardResult.Ok -> {
                    // 올렸으니 이 글의 임시저장은 필요 없다
                    val draft = repository.storyDraft.first()
                    if (draft != null && draft.editingId == edit) repository.saveStoryDraft(null)
                    onDone()
                }
                is BoardResult.Failed -> _error.value = if (result.signIn) StoryPublishError.SIGN_IN else StoryPublishError.FAILED
            }
        }
    }

    /** 임시저장하고 나가기 — 본문 · 장소 · 고치던 글 번호를 함께 */
    fun saveDraft(onSaved: () -> Unit) {
        viewModelScope.launch {
            repository.saveStoryDraft(StoryDraft(text.value, place.value, editingId.value, System.currentTimeMillis()))
            repository.postStoryNotice(com.stepup.android.data.repo.StoryNotice.DraftSaved(editingId.value))
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

        private const val KEY_EDIT = "story_compose_edit"
        private const val KEY_TEXT = "story_compose_text"
        private const val KEY_PLACE_NAME = "story_compose_place_name"
        private const val KEY_PLACE_ADDRESS = "story_compose_place_address"
        private const val KEY_PLACE_LAT = "story_compose_place_lat"
        private const val KEY_PLACE_LNG = "story_compose_place_lng"
        private const val KEY_INITIAL_TEXT = "story_compose_initial_text"
        private const val KEY_INITIAL_PLACE = "story_compose_initial_place"
        private const val KEY_READY = "story_compose_ready"

        val Factory = viewModelFactory {
            initializer { StoryComposeViewModel(ServiceLocator.communityRepository, createSavedStateHandle()) }
        }
    }
}
