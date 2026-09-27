package com.stepup.android.ui.screens.community.stories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.repo.BoardResult
import com.stepup.android.data.repo.BoardSyncState
import com.stepup.android.data.repo.CommunityRepository
import com.stepup.android.data.repo.StoryNotice
import com.stepup.android.domain.Comment
import com.stepup.android.domain.Post
import com.stepup.android.domain.StoryReportReason
import com.stepup.android.domain.StoryText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 상세 화면이 여는 창 — 하나만 열린다 */
enum class StoryDetailSheet { NONE, OWN_MENU, OTHER_MENU, DELETE, REPORT, REPORTED }

/** 상세 화면이 잠깐 띄울 실패 */
enum class StoryDetailError { COMMENT, LIKE, DELETE, REPORT, SIGN_IN }

/**
 * 글 상세 — 글쓴이 → 제목 · 본문 → 장소 → 반응 → 댓글. 댓글 입력은 아래에 고정.
 * 내 글이면 수정 · 삭제, 남의 글이면 숨기기 · 신고.
 */
class StoryDetailViewModel(
    private val repository: CommunityRepository,
    saved: SavedStateHandle,
) : ViewModel() {

    val postId: Long = saved.get<Long>(ARG_ID) ?: 0L

    /** 목록에서 빠져도(신고 · 삭제 직후) 창을 닫기 전까지 보이게 마지막 모습을 들고 있다 */
    private var lastSeen: Post? = null

    val post: StateFlow<Post?> = repository.posts
        .map { list -> list.firstOrNull { it.id == postId }?.also { lastSeen = it } ?: lastSeen }
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.posts.value.firstOrNull { it.id == postId })

    /** 목록을 받았는데 이 글이 없다(지워졌거나 볼 수 없다) */
    private val _missing = MutableStateFlow(false)
    val missing: StateFlow<Boolean> = _missing

    /** 목록을 받지 못했다 — "없는 글"과 섞지 않고 다시 시도를 준다 */
    private val _offline = MutableStateFlow(false)
    val offline: StateFlow<Boolean> = _offline

    init {
        loadIfNeeded()
    }

    /** 목록을 아직 못 받았으면(알림 · 다시 뜬 프로세스) 받는다 */
    fun loadIfNeeded() {
        if (repository.posts.value.any { it.id == postId }) return
        _offline.value = false
        viewModelScope.launch {
            val state = repository.refresh()
            val found = repository.posts.value.any { it.id == postId }
            _missing.value = !found && state == BoardSyncState.Ready
            _offline.value = !found && state != BoardSyncState.Ready
        }
    }

    /** 이 글을 고치고 돌아왔다 — "글을 수정했어요" · "임시저장했어요" */
    val notice: StateFlow<StoryNotice?> = repository.storyNotice
        .map { it.takeIf(::isMine) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun consumeNotice() {
        repository.storyNotice.value?.takeIf(::isMine)?.let(repository::consumeStoryNotice)
    }

    private fun isMine(notice: StoryNotice?): Boolean =
        (notice is StoryNotice.Edited || notice is StoryNotice.DraftSaved) && notice.postId == postId

    /** 댓글 — 부모 뒤에 답글이 이어진다(예전 앱이 단 답글도 보인다) */
    val comments: StateFlow<List<Comment>> = repository.commentThreads(postId)
        .map { threads -> threads.flatMap { listOf(it.comment) + it.replies } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _sheet = MutableStateFlow(StoryDetailSheet.NONE)
    val sheet: StateFlow<StoryDetailSheet> = _sheet

    fun openMenu() {
        val mine = post.value?.mine ?: return
        _sheet.value = if (mine) StoryDetailSheet.OWN_MENU else StoryDetailSheet.OTHER_MENU
    }

    fun openSheet(next: StoryDetailSheet) {
        _sheet.value = next
    }

    fun closeSheet() {
        if (!_busy.value) _sheet.value = StoryDetailSheet.NONE
    }

    private val _busy = MutableStateFlow(false)

    /** 삭제 · 신고 · 댓글을 보내는 중 — 두 번 누르지 않게 */
    val busy: StateFlow<Boolean> = _busy

    private val _error = MutableStateFlow<StoryDetailError?>(null)
    val error: StateFlow<StoryDetailError?> = _error

    fun consumeError() {
        _error.value = null
    }

    private fun fail(kind: StoryDetailError, result: BoardResult) {
        _error.value = if (result is BoardResult.Failed && result.signIn) StoryDetailError.SIGN_IN else kind
    }

    fun toggleLike() {
        viewModelScope.launch {
            val result = repository.toggleStoryLike(postId)
            if (result is BoardResult.Failed) fail(StoryDetailError.LIKE, result)
        }
    }

    /**
     * 댓글 보내기. 성공해야 [onSent] 로 입력을 비운다 — 실패하면 쓴 말이 그대로 남아 다시 보낼 수 있다.
     */
    fun sendComment(text: String, onSent: () -> Unit) {
        val clean = text.trim()
        if (clean.isEmpty() || StoryText.length(clean) > StoryText.COMMENT_MAX_CHARS || _busy.value) return
        _busy.value = true
        viewModelScope.launch {
            val result = repository.addStoryComment(postId, clean)
            _busy.value = false
            if (result is BoardResult.Ok) onSent() else fail(StoryDetailError.COMMENT, result)
        }
    }

    /** 삭제 — 서버가 지운 뒤에만 나간다. 실패하면 글이 그대로 있다 */
    fun delete(onDeleted: () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            val result = repository.deleteStory(postId)
            _busy.value = false
            if (result is BoardResult.Ok) {
                _sheet.value = StoryDetailSheet.NONE
                onDeleted()
            } else {
                fail(StoryDetailError.DELETE, result)
            }
        }
    }

    /** 이 글 숨기기 — 차단이 아니다. 목록에서 되돌리기를 준다 */
    fun hide(onHidden: () -> Unit) {
        _sheet.value = StoryDetailSheet.NONE
        viewModelScope.launch {
            repository.hideStory(postId)
            onHidden()
        }
    }

    /** 신고 — 서버가 받아 준 뒤에만 "접수됐어요" */
    fun report(reason: StoryReportReason) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            val result = repository.reportStory(postId, reason)
            _busy.value = false
            if (result is BoardResult.Ok) _sheet.value = StoryDetailSheet.REPORTED
            else fail(StoryDetailError.REPORT, result)
        }
    }

    companion object {
        const val ARG_ID = "postId"

        val Factory = viewModelFactory {
            initializer {
                StoryDetailViewModel(ServiceLocator.communityRepository, createSavedStateHandle())
            }
        }
    }
}
