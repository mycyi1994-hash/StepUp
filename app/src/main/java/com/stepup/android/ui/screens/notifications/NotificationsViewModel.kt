package com.stepup.android.ui.screens.notifications

import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.local.NotificationEntity
import com.stepup.android.data.local.NotificationType
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.repo.AnnouncementRepository
import com.stepup.android.data.repo.CommentTarget
import com.stepup.android.data.repo.CommunityRepository
import com.stepup.android.data.repo.CrewActionResult
import com.stepup.android.data.repo.CrewRepository
import com.stepup.android.data.repo.CrewSyncState
import com.stepup.android.data.repo.NoticeBoard
import com.stepup.android.data.repo.NotificationRepository
import com.stepup.android.data.repo.SignInReturn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 내 알림 목록을 읽은 결과 — 읽기 전에는 빈 알림함으로 보이지 않는다(11 · 12) */
sealed interface InboxLoad {
    data object Loading : InboxLoad

    /** @param newestId 목록을 받은 때 가장 최근 알림 번호 — "모두 읽음"이 여기까지만 읽음으로 바꾼다 */
    data class Ready(val items: List<NotificationEntity>, val newestId: Long) : InboxLoad

    data object Failed : InboxLoad
}

/** 크루 · 함께 뛰기 초대 시트의 단계. 읽음(read) · 응답(actioned) · 실제 가입은 서로 다른 상태다 */
enum class InviteStep {
    /** 응답 전(05 · 08) */
    Idle,

    /** 서버에 보내거나 확인하는 중 — 수락 · 거절을 함께 막는다 */
    Busy,

    /** 서버가 가입을 끝냈다 */
    Joined,

    /** 가입 신청만 됐다(06) — 가입 완료가 아니다 */
    Requested,

    /** 이미 응답한 초대인데 지금 가입 상태를 서버 목록에서 찾지 못했다 */
    Answered,

    /** 서버가 받지 않았다(07). 초대는 그대로 */
    Failed,

    /** 응답을 받지 못했다 — 처리됐는지 모른다. 다시 수락하지 않고 가입 상태부터 확인한다 */
    Unknown,

    /** 가입 상태를 다시 물었지만 아직 모른다 */
    StillUnknown,

    /** 로그인해야 응답할 수 있다(22). 초대는 그대로 */
    SignIn,

    /** 거절 확인(21) — 초대 시트 안에서 내용만 바뀐다 */
    ConfirmDecline,
    Declining,
    DeclineFailed,

    /** 함께 뛰기 초대에 수락 표시를 했다 — 실제 참가는 대기실에서 확인한다 */
    Marked,

    /** 수락 표시를 저장하지 못했다 */
    MarkFailed,
}

/** 답글 알림이 가리키는 글을 열 수 있는가 */
enum class CommentCheck { Visible, Missing, SignIn, Offline }

/** 화면 아래 짧은 알림. 같은 말이 다시 떠도 새로 보이게 번호를 붙인다 */
data class InboxToast(@StringRes val message: Int, val success: Boolean, val seq: Long)

/**
 * 알림함(알림·공지 v1) — 내 알림과 공지.
 *
 * 알림함에 들어온 것만으로 읽음 처리하지 않는다. 알림을 열어 내용 시트나 갈 곳이 준비되면 그 알림만 읽음으로,
 * "모두 읽음"은 사람이 누를 때만 — 누른 때 목록에 있던 알림까지만 바꾼다. 읽음은 행 삭제 · 초대 응답 · 보상 지급과
 * 따로 움직인다. 본문은 저장된 type + 인자를 표시 시점에 현지화한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModel(
    private val repo: NotificationRepository,
    private val crewRepository: CrewRepository,
    private val communityRepository: CommunityRepository,
    private val announcements: AnnouncementRepository,
) : ViewModel() {

    private val attempt = MutableStateFlow(0)

    val inbox: StateFlow<InboxLoad> = attempt.flatMapLatest {
        combine(repo.notifications(), repo.newestId) { items, newest ->
            InboxLoad.Ready(items, newest ?: 0L) as InboxLoad
        }
            .onStart { emit(InboxLoad.Loading) }
            .catch { emit(InboxLoad.Failed) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InboxLoad.Loading)

    /** 새 알림 수 — 목록 한도(100) 밖까지 저장소 전체에서 센다 */
    val unread: StateFlow<Int> = repo.unreadCount
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val notices: StateFlow<NoticeBoard> = announcements.board

    /** 로그인하고 돌아오면 다시 열 초대 */
    val reopenInvite: StateFlow<SignInReturn?> = repo.inviteAfterSignIn

    private val _readingAll = MutableStateFlow(false)
    val readingAll = _readingAll.asStateFlow()

    private val _steps = MutableStateFlow<Map<Long, InviteStep>>(emptyMap())
    val steps = _steps.asStateFlow()

    private val _toast = MutableStateFlow<InboxToast?>(null)
    val toast = _toast.asStateFlow()
    private var toastSeq = 0L
    private var noticesAskedAt = 0L

    fun reload() {
        attempt.value++
    }

    fun toastShown(seq: Long) {
        if (_toast.value?.seq == seq) _toast.value = null
    }

    private fun toast(@StringRes message: Int, success: Boolean) {
        _toast.value = InboxToast(message, success, ++toastSeq)
    }

    // ── 읽음 ──────────────────────────────────────────────────────

    /** "모두 읽음" — 확인 창 없이. 누르는 동안 다시 누를 수 없고, 실패하면 저장된 상태를 그대로 둔다(13) */
    fun markAllRead() {
        val ready = inbox.value as? InboxLoad.Ready ?: return
        if (_readingAll.value) return
        _readingAll.value = true
        viewModelScope.launch {
            try {
                repo.markReadUpTo(ready.newestId)
                toast(R.string.inbox_marked_all, success = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                toast(R.string.inbox_mark_all_failed, success = false)
            } finally {
                _readingAll.value = false
            }
        }
    }

    /** 알림 하나를 열었다 — 그 알림만 읽음으로. 못 남겨도 내용은 그대로 보인다 */
    fun markRead(entity: NotificationEntity) {
        if (entity.read) return
        viewModelScope.launch {
            try {
                repo.markRead(entity.id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                toast(R.string.inbox_mark_one_failed, success = false)
            }
        }
    }

    fun clearReopen() {
        repo.reopenAfterSignIn(null)
    }

    // ── 공지 ──────────────────────────────────────────────────────

    /** 공지 메뉴를 열 때 — 방금 받았으면(1분 안) 다시 묻지 않는다. 상세에서 돌아와도 목록이 그대로다 */
    fun refreshNoticesIfStale(now: Long = System.currentTimeMillis()) {
        if (now - noticesAskedAt < 60_000 && notices.value.items != null) return
        refreshNotices(now)
    }

    fun refreshNotices(now: Long = System.currentTimeMillis()) {
        noticesAskedAt = now
        viewModelScope.launch { announcements.refresh() }
    }

    // ── 답글 알림 ─────────────────────────────────────────────────

    /** 답글 알림이 가리키는 글 — 서버가 없다고 답한 것만 "열 수 없는 글"(14). 닿지 못한 것은 다시 누르게 한다 */
    suspend fun checkComment(target: CommentTarget): CommentCheck {
        if (communityRepository.posts.value.any { it.id == target.postId }) return CommentCheck.Visible
        return when (val result = communityRepository.postVisible(target.postId)) {
            is ServerResult.Ok -> if (result.value) {
                // 게시판 목록을 받아 두어야 댓글 창이 그 글을 찾는다
                communityRepository.refresh()
                CommentCheck.Visible
            } else {
                CommentCheck.Missing
            }
            is ServerResult.SignInRequired -> CommentCheck.SignIn
            is ServerResult.Rejected, is ServerResult.Retry -> CommentCheck.Offline
        }
    }

    fun commentUnavailable(check: CommentCheck) {
        when (check) {
            CommentCheck.SignIn -> toast(R.string.comment_sign_in, success = false)
            CommentCheck.Offline -> toast(R.string.comment_check_failed, success = false)
            else -> Unit
        }
    }

    // ── 초대 ──────────────────────────────────────────────────────

    private fun step(id: Long, next: InviteStep?) {
        _steps.value = if (next == null) _steps.value - id else _steps.value + (id to next)
    }

    private fun busy(id: Long) = _steps.value[id] == InviteStep.Busy || _steps.value[id] == InviteStep.Declining

    /** 초대 시트를 열 때 — 이미 응답한 크루 초대는 서버의 지금 가입 상태로 보인다 */
    fun prepareInvite(entity: NotificationEntity) {
        if (_steps.value[entity.id] != null || !entity.actioned) return
        when (entity.type) {
            NotificationType.CREW_INVITE -> {
                step(entity.id, crewStatus(entity.argExtra) ?: InviteStep.Answered)
                viewModelScope.launch {
                    val synced = try {
                        crewRepository.refresh()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        return@launch
                    }
                    if (synced == CrewSyncState.Ready && !busy(entity.id)) {
                        step(entity.id, crewStatus(entity.argExtra) ?: InviteStep.Answered)
                    }
                }
            }
            NotificationType.PARTY_INVITE -> step(entity.id, InviteStep.Marked)
        }
    }

    /** 시트를 닫으면 일시적인 단계(실패 · 거절 확인 · 로그인 안내)는 잊는다. 결과와 "모름"은 남긴다 */
    fun closeInvite(id: Long) {
        when (_steps.value[id]) {
            InviteStep.Failed, InviteStep.SignIn, InviteStep.ConfirmDecline,
            InviteStep.DeclineFailed, InviteStep.MarkFailed -> step(id, null)
            else -> Unit
        }
    }

    private fun crewStatus(crewId: String): InviteStep? = crewRepository.crewOf(crewId)?.let {
        when {
            it.joined -> InviteStep.Joined
            it.requested -> InviteStep.Requested
            else -> null
        }
    }

    /**
     * 크루 초대 수락 — 기존 acceptCrewInvitation(crewRepository.join) 결과를 따른다.
     * 가입 완료(Joined)와 가입 신청(Requested)을 나누고, 서버가 받기 전에는 성공으로 보이지 않는다.
     */
    fun acceptCrewInvite(entity: NotificationEntity) {
        if (busy(entity.id)) return
        step(entity.id, InviteStep.Busy)
        viewModelScope.launch {
            val next = try {
                when (val result = repo.acceptCrewInvite(entity, crewRepository)) {
                    CrewActionResult.Joined -> InviteStep.Joined
                    CrewActionResult.Requested -> InviteStep.Requested
                    CrewActionResult.Done -> crewStatus(entity.argExtra) ?: InviteStep.Answered
                    is CrewActionResult.Failed -> when {
                        result.signIn -> InviteStep.SignIn
                        // 응답을 못 받았다 — join 이 끝나며 받은 크루 목록에서 먼저 찾는다
                        result.retryable -> settleFromCrewList(entity) ?: InviteStep.Unknown
                        else -> InviteStep.Failed
                    }
                    is CrewActionResult.Created -> InviteStep.Failed
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                InviteStep.Unknown
            }
            step(entity.id, next)
        }
    }

    /** 결과를 모를 때 — 가입 상태를 서버에 다시 묻는다. 수락을 다시 보내지 않는다 */
    fun checkCrewStatus(entity: NotificationEntity) {
        if (busy(entity.id)) return
        step(entity.id, InviteStep.Busy)
        viewModelScope.launch {
            val next = try {
                crewRepository.refresh()
                settleFromCrewList(entity) ?: InviteStep.StillUnknown
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                InviteStep.StillUnknown
            }
            step(entity.id, next)
        }
    }

    /** 막 받은 크루 목록으로 판단한다. 목록을 받지 못했으면 null(모름) */
    private suspend fun settleFromCrewList(entity: NotificationEntity): InviteStep? {
        if (crewRepository.sync.value != CrewSyncState.Ready) return null
        val status = crewStatus(entity.argExtra)
        // 서버에 가입 · 신청이 있으면 초대는 응답한 것이다. 없으면 처리되지 않았다(07 — 다시 수락할 수 있다)
        if (status != null) repo.markInviteAnswered(entity)
        return status ?: InviteStep.Failed
    }

    fun askDecline(entity: NotificationEntity) {
        if (!busy(entity.id)) step(entity.id, InviteStep.ConfirmDecline)
    }

    fun cancelDecline(entity: NotificationEntity) {
        if (_steps.value[entity.id] != InviteStep.Declining) step(entity.id, null)
    }

    /** 초대 거절 — 그 초대 알림만 지운다(크루 탈퇴 · 대기실 변화 없음). 지운 뒤에만 "거절했어요" */
    fun decline(entity: NotificationEntity) {
        if (_steps.value[entity.id] == InviteStep.Declining) return
        step(entity.id, InviteStep.Declining)
        viewModelScope.launch {
            try {
                repo.decline(entity)
                step(entity.id, null)
                toast(R.string.decline_done, success = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                step(entity.id, InviteStep.DeclineFailed)
            }
        }
    }

    /**
     * 함께 뛰기 "수락하고 보기" — 기존처럼 이 폰의 알림에 수락 표시만 하고 대기실로 간다.
     * 실제 참가 · 출발은 대기실이 서버와 확인한다. "대기실 보기"만 누르면 수락 표시를 하지 않는다.
     */
    fun acceptPartyInvite(entity: NotificationEntity, onOpenLobby: (String) -> Unit) {
        if (busy(entity.id)) return
        step(entity.id, InviteStep.Busy)
        viewModelScope.launch {
            try {
                repo.acceptPartyInvite(entity)
                step(entity.id, InviteStep.Marked)
                if (entity.argExtra.isNotBlank()) onOpenLobby(entity.argExtra)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                step(entity.id, InviteStep.MarkFailed)
            }
        }
    }

    /** 로그인이 필요한 초대 — 로그인하고 돌아오면 이 초대를 다시 연다(자동 수락 없음) */
    fun rememberForSignIn(entity: NotificationEntity) {
        step(entity.id, null)
        repo.reopenAfterSignIn(SignInReturn(entity.id, SystemClock.elapsedRealtime()))
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                NotificationsViewModel(
                    ServiceLocator.notificationRepository,
                    ServiceLocator.crewRepository,
                    ServiceLocator.communityRepository,
                    ServiceLocator.announcementRepository,
                )
            }
        }
    }
}
