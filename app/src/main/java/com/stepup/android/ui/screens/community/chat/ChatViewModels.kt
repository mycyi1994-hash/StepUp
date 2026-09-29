package com.stepup.android.ui.screens.community.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.repo.ChatOutcome
import com.stepup.android.data.repo.ChatPhoto
import com.stepup.android.data.repo.ChatRoomsState
import com.stepup.android.data.repo.ChatSync
import com.stepup.android.data.repo.CrewChatRepository
import com.stepup.android.data.repo.CrewPhotos
import com.stepup.android.domain.ChatDelivery
import com.stepup.android.domain.ChatMessage
import com.stepup.android.domain.ChatNotice
import com.stepup.android.domain.ChatNoticeDraft
import com.stepup.android.domain.ChatReportReason
import com.stepup.android.domain.ChatRoomMeta
import com.stepup.android.domain.ChatRules
import com.stepup.android.domain.CrewMember
import com.stepup.android.domain.CrewPerson
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** 한 번 읽는 것의 상태 — 참여가 끝났으면 [Ended](32) */
sealed interface ChatLoad<out T> {
    data object Loading : ChatLoad<Nothing>
    data class Ready<T>(val value: T) : ChatLoad<T>
    data object Failed : ChatLoad<Nothing>
    data object Ended : ChatLoad<Nothing>
}

/** 확인창(삭제 · 숨김 · 신고 · 내보내기 · 나가기 · 공지 삭제)의 상태 — 실패해도 창을 닫지 않고 짧은 오류와 다시 하기 */
data class ChatConfirm(val busy: Boolean = false, val error: ChatConfirmError? = null)

enum class ChatConfirmError {
    /** 연결 · 서버 사정 — 원래 상태 그대로, 다시 해 볼 수 있다 */
    FAILED,

    /** 이미 처리됐다 — 지금 상태를 다시 읽어 보인다 */
    GONE,
}

private fun <T> ChatOutcome<T>.confirmError(): ChatConfirmError =
    if (this is ChatOutcome.Rejected && (reason.contains("message_missing") || reason.contains("message_gone") ||
            reason.contains("notice_missing") || reason.contains("target_not_member"))
    ) ChatConfirmError.GONE else ChatConfirmError.FAILED

// ─────────────────────────────────────────────────────────────
// 01 내 크루 대화
// ─────────────────────────────────────────────────────────────

class ChatListViewModel(
    private val repo: CrewChatRepository,
    crewCards: com.stepup.android.data.repo.CrewCardRepository,
) : ViewModel() {
    val rooms: StateFlow<ChatRoomsState> = repo.rooms

    /** 31 "내 크루 만들기" — 남겨 둔 만들기 초안이 있으면 이어 쓸지 먼저 묻는다(크루 모집과 같은 시트) */
    val createDraft: StateFlow<com.stepup.android.domain.CrewDraft?> =
        crewCards.draft(com.stepup.android.domain.CrewDraft.KEY_CREATE)
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, null)

    private var poll: Job? = null

    /** 목록이 보이는 동안만 다시 읽는다(미확인 수 · 마지막 메시지) */
    fun setActive(active: Boolean) {
        poll?.cancel()
        if (!active) return
        poll = viewModelScope.launch {
            while (isActive) {
                repo.refreshRooms()
                delay(ChatRules.LIST_POLL_MS)
            }
        }
    }

    fun retry() {
        viewModelScope.launch { repo.refreshRooms() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ChatListViewModel(ServiceLocator.crewChat, ServiceLocator.crewCards) }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 02 · 03 대화 — 보내기 · 전송 실패 · 재접속 · 이전 대화 · 읽음 · 메시지 메뉴
// ─────────────────────────────────────────────────────────────

enum class ChatRoomLoad { LOADING, READY, ERROR, ENDED }

data class ChatRoomUi(
    val load: ChatRoomLoad = ChatRoomLoad.LOADING,
    val meta: ChatRoomMeta? = null,
    /** 서버 메시지(방 순서) + 이 폰에만 있는 내 메시지(맨 아래) */
    val messages: List<ChatMessage> = emptyList(),
    val me: String = "",
    /** 연결이 끊겨 다시 잇는 중(20) — 보내기 · 사진은 막고 입력은 그대로 */
    val online: Boolean = true,
    val olderLoading: Boolean = false,
    val olderDone: Boolean = false,
    /** 검색에서 온 메시지 — 잠깐 강조 */
    val highlight: Long? = null,
)

/** 메시지를 두고 연 창 */
sealed interface ChatSheet {
    data class Menu(val message: ChatMessage) : ChatSheet
    data class Delete(val message: ChatMessage, val state: ChatConfirm = ChatConfirm()) : ChatSheet
    data class Hide(val message: ChatMessage, val state: ChatConfirm = ChatConfirm()) : ChatSheet
    data class Report(val message: ChatMessage, val reason: ChatReportReason? = null, val state: ChatConfirm = ChatConfirm()) : ChatSheet
    data object Attach : ChatSheet
}

/** 한 번만 알리는 것 */
sealed interface ChatEventOnce {
    data object Copied : ChatEventOnce
    data object Reported : ChatEventOnce
    data object OwnerLost : ChatEventOnce
    data object Gone : ChatEventOnce
    data object SearchGone : ChatEventOnce
}

class ChatRoomViewModel(
    private val repo: CrewChatRepository,
    saved: SavedStateHandle,
) : ViewModel() {
    val crewId: String = saved.get<String>("crewId").orEmpty()

    private val _ui = MutableStateFlow(ChatRoomUi())
    val ui: StateFlow<ChatRoomUi> = _ui

    /** 입력창 — 방을 나갔다 와도 남는다(앱이 살아 있는 동안) */
    private val _draft = MutableStateFlow(repo.draft(crewId))
    val draft: StateFlow<String> = _draft

    private val _replyTo = MutableStateFlow<ChatMessage?>(null)
    val replyTo: StateFlow<ChatMessage?> = _replyTo

    private val _sheet = MutableStateFlow<ChatSheet?>(null)
    val sheet: StateFlow<ChatSheet?> = _sheet

    private val _events = MutableSharedFlow<ChatEventOnce>(extraBufferCapacity = 4)
    val events: SharedFlow<ChatEventOnce> = _events

    private var lastRev: Long? = null
    private var poll: Job? = null
    private var failures = 0
    private var readSent = 0L
    private var readJob: Job? = null

    /** 목록에서 연 방 — 서버가 방 정보를 주기 전까지 머리의 제목 · 대표 이미지 · 인원은 목록 값으로 */
    val preview: com.stepup.android.domain.ChatRoomSummary? = repo.roomNow(crewId)

    init {
        viewModelScope.launch {
            val me = repo.me()
            // 지난번에 보던 대화는 가장 최근 목록에 이 방이 있을 때만 먼저 보인다(내보내졌거나 나간 방을 다시 보이지 않게)
            val snapshot = repo.snapshot(crewId)?.takeIf { repo.roomNow(crewId) != null }
            // 앱이 보내던 중에 방을 떠났다 — 받았는지 모르는 메시지는 연결되면 요청 키로 확인한다(다시 보내지 않는다)
            val locals = repo.local(crewId).map { if (it.delivery == ChatDelivery.SENDING) it.copy(delivery = ChatDelivery.PENDING) else it }
            _ui.update { it.copy(me = me, messages = locals) }
            if (snapshot != null) apply(snapshot.copy(reset = true))
        }
    }

    /** 방이 화면에 보이는 동안만 따라온다(배경에서는 읽지 않는다) */
    fun setActive(active: Boolean) {
        poll?.cancel()
        if (!active || _ui.value.load == ChatRoomLoad.ENDED) return
        poll = viewModelScope.launch {
            while (isActive) {
                val ok = syncOnce()
                if (_ui.value.load == ChatRoomLoad.ENDED) break
                delay(if (ok) ChatRules.POLL_MS else ChatRules.RETRY_MS)
            }
        }
    }

    /** 36 다시 불러오기 */
    fun retryLoad() {
        _ui.update { it.copy(load = ChatRoomLoad.LOADING) }
        viewModelScope.launch { syncOnce() }
    }

    private suspend fun syncOnce(): Boolean {
        return when (val outcome = repo.sync(crewId, lastRev)) {
            is ChatOutcome.Ok -> {
                val wasOffline = !_ui.value.online
                failures = 0
                apply(outcome.value)
                if (wasOffline || _ui.value.messages.any { it.delivery == ChatDelivery.PENDING }) confirmPending()
                true
            }
            is ChatOutcome.Ended -> {
                end()
                false
            }
            else -> {
                failures++
                _ui.update { ui ->
                    when {
                        ui.meta == null -> ui.copy(load = ChatRoomLoad.ERROR)
                        failures >= 2 -> ui.copy(online = false)
                        else -> ui
                    }
                }
                false
            }
        }
    }

    private fun apply(sync: ChatSync) {
        val current = _ui.value.messages
        val merged = if (sync.reset) ChatRules.replace(current, sync.messages) else ChatRules.merge(current, sync.messages)
        lastRev = sync.lastRev
        readSent = maxOf(readSent, sync.meta.myReadSeq)
        // 크루장 권한만 사라졌다 — 크루원 화면으로 돌아가고 한 번 알린다(소속은 그대로)
        if (_ui.value.meta?.owner == true && !sync.meta.owner) {
            (_sheet.value as? ChatSheet.Hide)?.let { _sheet.value = null }
            _events.tryEmit(ChatEventOnce.OwnerLost)
        }
        repo.setLocal(crewId, merged.filter { it.id == null })
        _ui.update {
            it.copy(
                load = ChatRoomLoad.READY,
                meta = sync.meta,
                messages = merged,
                online = true,
                olderDone = it.olderDone || (sync.reset && sync.messages.size < ChatRules.PAGE),
            )
        }
        repo.keepSnapshot(crewId, ChatSync(sync.meta, merged.filter { it.id != null }, true, sync.lastRev))
    }

    private suspend fun end() {
        poll?.cancel()
        repo.forget(crewId)
        _sheet.value = null
        _ui.update { ChatRoomUi(load = ChatRoomLoad.ENDED, me = it.me) }
    }

    // ── 입력 · 보내기 ────────────────────────────────────────

    fun setDraft(text: String) {
        val clipped = text.take(ChatRules.BODY_MAX)
        _draft.value = clipped
        repo.setDraft(crewId, clipped)
    }

    fun reply(message: ChatMessage) {
        _replyTo.value = message
        _sheet.value = null
    }

    /** 답장 연결만 취소 — 쓴 글은 그대로 */
    fun cancelReply() {
        _replyTo.value = null
    }

    /** 보내기 버튼을 눌렀을 때만(Enter 로는 보내지 않는다). 공백만 있거나 연결이 끊겼으면 보내지 않는다 */
    fun send() {
        val text = _draft.value
        if (!ChatRules.sendable(text) || !_ui.value.online || _ui.value.load != ChatRoomLoad.READY) return
        // 입력창은 바로 비운다 — 빠르게 두 번 눌러도 메시지는 하나
        val reply = _replyTo.value
        setDraft("")
        _replyTo.value = null
        viewModelScope.launch {
            val message = repo.outgoing(crewId, text.trim(), reply, null)
            addLocal(message)
            deliver(message)
        }
    }

    /** 19 재전송 — 같은 요청 키로(서버에는 하나) */
    fun retry(message: ChatMessage) {
        if (!_ui.value.online) return
        val current = _ui.value.messages.firstOrNull { it.id == null && it.clientId == message.clientId } ?: return
        if (current.delivery == ChatDelivery.SENDING) return
        val sending = current.copy(delivery = ChatDelivery.SENDING)
        updateLocal(message.clientId) { sending }
        viewModelScope.launch { deliver(sending) }
    }

    /** 19 미전송 메시지 지우기 — 이 폰의 실패한 것만(서버가 받은 메시지는 건드리지 않는다) */
    fun discard(message: ChatMessage) {
        if (message.id != null) return
        _ui.update { ui -> ui.copy(messages = ui.messages.filterNot { it.id == null && it.clientId == message.clientId }) }
        repo.setLocal(crewId, _ui.value.messages.filter { it.id == null })
    }

    private suspend fun deliver(message: ChatMessage) {
        when (val outcome = repo.send(crewId, message, null)) {
            is ChatOutcome.Ok -> reconcile(listOf(outcome.value))
            is ChatOutcome.Offline -> {
                // 받았는지 모른다 — 전송 대기. 연결이 돌아오면 요청 키로 확인한다(다시 보내지 않는다)
                updateLocal(message.clientId) { it.copy(delivery = ChatDelivery.PENDING) }
                _ui.update { it.copy(online = false) }
            }
            is ChatOutcome.Ended -> end()
            is ChatOutcome.NotOwner, is ChatOutcome.Rejected ->
                updateLocal(message.clientId) { it.copy(delivery = ChatDelivery.FAILED) }
        }
    }

    /** 연결이 돌아왔다 — 전송 대기였던 것 중 서버가 받은 것은 확정, 못 받은 것은 실패(19)로 */
    private suspend fun confirmPending() {
        val pending = _ui.value.messages.filter { it.id == null && it.delivery == ChatDelivery.PENDING }.mapNotNull { it.clientId }
        if (pending.isEmpty()) return
        when (val outcome = repo.confirm(crewId, pending)) {
            is ChatOutcome.Ok -> {
                reconcile(outcome.value)
                val got = outcome.value.mapNotNull { it.clientId }.toSet()
                pending.filterNot { it in got }.forEach { id -> updateLocal(id) { m -> m.copy(delivery = ChatDelivery.FAILED) } }
            }
            is ChatOutcome.Ended -> end()
            else -> Unit
        }
    }

    private fun reconcile(messages: List<ChatMessage>) {
        val merged = ChatRules.merge(_ui.value.messages, messages)
        _ui.update { it.copy(messages = merged) }
        repo.setLocal(crewId, merged.filter { it.id == null })
    }

    private fun addLocal(message: ChatMessage) {
        _ui.update { it.copy(messages = it.messages + message) }
        repo.setLocal(crewId, _ui.value.messages.filter { it.id == null })
    }

    private fun updateLocal(clientId: String?, change: (ChatMessage) -> ChatMessage) {
        clientId ?: return
        _ui.update { ui -> ui.copy(messages = ui.messages.map { if (it.id == null && it.clientId == clientId) change(it) else it }) }
        repo.setLocal(crewId, _ui.value.messages.filter { it.id == null })
    }

    // ── 이전 대화 · 검색에서 온 메시지 · 읽음 ────────────────

    fun loadOlder() {
        val ui = _ui.value
        if (ui.olderLoading || ui.olderDone || ui.load != ChatRoomLoad.READY) return
        viewModelScope.launch { older() }
    }

    private suspend fun older(): Boolean {
        val oldest = _ui.value.messages.mapNotNull { it.seq }.minOrNull() ?: run {
            _ui.update { it.copy(olderDone = true) }
            return false
        }
        _ui.update { it.copy(olderLoading = true) }
        return when (val outcome = repo.history(crewId, oldest)) {
            is ChatOutcome.Ok -> {
                val merged = ChatRules.merge(_ui.value.messages, outcome.value)
                _ui.update { it.copy(messages = merged, olderLoading = false, olderDone = outcome.value.size < ChatRules.PAGE) }
                outcome.value.isNotEmpty()
            }
            is ChatOutcome.Ended -> {
                end()
                false
            }
            else -> {
                _ui.update { it.copy(olderLoading = false) }
                false
            }
        }
    }

    /** 검색 결과로 돌아왔다 — 그 메시지가 들어올 때까지 이전 대화를 읽고 잠깐 강조한다 */
    fun reveal(messageId: Long, seq: Long) {
        viewModelScope.launch {
            var pages = 0
            while (_ui.value.messages.none { it.id == messageId } && pages < 20) {
                val oldest = _ui.value.messages.mapNotNull { it.seq }.minOrNull() ?: break
                if (oldest <= seq || _ui.value.olderDone) break
                if (!older()) break
                pages++
            }
            val found = _ui.value.messages.firstOrNull { it.id == messageId }
            if (found == null || !found.visible) {
                _events.tryEmit(ChatEventOnce.SearchGone)
                return@launch
            }
            _ui.update { it.copy(highlight = messageId) }
            delay(1_600)
            _ui.update { if (it.highlight == messageId) it.copy(highlight = null) else it }
        }
    }

    /** 대화가 실제로 보인다(앱이 켜져 있고 이 방 화면이 보이는 동안) — 읽은 위치를 앞으로 */
    fun seen(seq: Long) {
        if (seq <= readSent || _ui.value.load != ChatRoomLoad.READY) return
        val before = readSent
        readSent = seq
        readJob?.cancel()
        readJob = viewModelScope.launch {
            delay(600)
            // 못 보냈으면 다음에 다시(목록의 미확인 수가 남지 않게)
            if (repo.read(crewId, seq) !is ChatOutcome.Ok && readSent == seq) readSent = before
        }
    }

    // ── 메시지 메뉴 · 확인창 ─────────────────────────────────

    fun openMenu(message: ChatMessage) {
        if (message.id == null || !message.visible) return
        _sheet.value = ChatSheet.Menu(message)
    }

    fun openAttach() {
        if (!_ui.value.online) return
        _sheet.value = ChatSheet.Attach
    }

    fun closeSheet() {
        _sheet.value = null
    }

    fun copied() {
        _sheet.value = null
        _events.tryEmit(ChatEventOnce.Copied)
    }

    fun askDelete(message: ChatMessage) {
        _sheet.value = ChatSheet.Delete(message)
    }

    fun askHide(message: ChatMessage) {
        _sheet.value = ChatSheet.Hide(message)
    }

    fun askReport(message: ChatMessage) {
        _sheet.value = ChatSheet.Report(message)
    }

    fun pickReason(reason: ChatReportReason) {
        (_sheet.value as? ChatSheet.Report)?.let { _sheet.value = it.copy(reason = reason, state = it.state.copy(error = null)) }
    }

    fun confirmDelete() {
        val sheet = _sheet.value as? ChatSheet.Delete ?: return
        _sheet.value = sheet.copy(state = ChatConfirm(busy = true))
        viewModelScope.launch {
            when (val outcome = repo.delete(crewId, sheet.message)) {
                is ChatOutcome.Ok -> {
                    reconcile(listOf(outcome.value))
                    _sheet.value = null
                    syncOnce()
                }
                is ChatOutcome.Ended -> end()
                else -> {
                    _sheet.value = sheet.copy(state = ChatConfirm(error = outcome.confirmError()))
                    if (outcome.confirmError() == ChatConfirmError.GONE) syncOnce()
                }
            }
        }
    }

    fun confirmHide() {
        val sheet = _sheet.value as? ChatSheet.Hide ?: return
        _sheet.value = sheet.copy(state = ChatConfirm(busy = true))
        viewModelScope.launch {
            when (val outcome = repo.hide(crewId, sheet.message)) {
                is ChatOutcome.Ok -> {
                    reconcile(listOf(outcome.value))
                    _sheet.value = null
                    syncOnce()
                }
                is ChatOutcome.Ended -> end()
                is ChatOutcome.NotOwner -> {
                    // 크루장 권한만 사라졌다 — 크루원으로 돌아간다(소속은 그대로). 방 정보가 바뀌면 apply() 가 알리고,
                    // 방 정보를 다시 읽지 못했으면 여기서 알린다
                    _sheet.value = null
                    val before = _ui.value.meta?.owner == true
                    syncOnce()
                    if (before && _ui.value.meta?.owner == true) _events.tryEmit(ChatEventOnce.OwnerLost)
                }
                else -> {
                    _sheet.value = sheet.copy(state = ChatConfirm(error = outcome.confirmError()))
                    if (outcome.confirmError() == ChatConfirmError.GONE) syncOnce()
                }
            }
        }
    }

    fun confirmReport() {
        val sheet = _sheet.value as? ChatSheet.Report ?: return
        val reason = sheet.reason ?: return
        _sheet.value = sheet.copy(state = ChatConfirm(busy = true))
        viewModelScope.launch {
            when (val outcome = repo.report(crewId, sheet.message, reason)) {
                is ChatOutcome.Ok -> {
                    _sheet.value = null
                    _events.tryEmit(ChatEventOnce.Reported)
                }
                is ChatOutcome.Ended -> end()
                else -> {
                    // 고른 사유는 그대로 — 같은 창에서 다시
                    _sheet.value = sheet.copy(state = ChatConfirm(error = outcome.confirmError()))
                    if (outcome.confirmError() == ChatConfirmError.GONE) syncOnce()
                }
            }
        }
    }

    /** 38 공지로 등록 — 작성 화면에 원문을 미리 채운다(바로 올리지 않는다) */
    fun seedNotice(message: ChatMessage) {
        ChatNoticeSeed.put(crewId, message.body.orEmpty())
        _sheet.value = null
    }

    override fun onCleared() {
        poll?.cancel()
        super.onCleared()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ChatRoomViewModel(ServiceLocator.crewChat, createSavedStateHandle()) }
        }
    }
}

/** 메시지로 공지를 쓸 때 원문 — 작성 화면이 한 번 꺼내 쓴다 */
object ChatNoticeSeed {
    private val seeds = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun put(crewId: String, text: String) {
        seeds[crewId] = text
    }

    fun take(crewId: String): String? = seeds.remove(crewId)
}

// ─────────────────────────────────────────────────────────────
// 04 · 05 채팅방 정보 · 24 채팅 알림 · 크루 나가기
// ─────────────────────────────────────────────────────────────

class ChatInfoViewModel(
    private val repo: CrewChatRepository,
    saved: SavedStateHandle,
) : ViewModel() {
    val crewId: String = saved.get<String>("crewId").orEmpty()

    private val _meta = MutableStateFlow<ChatLoad<ChatRoomMeta>>(
        repo.snapshot(crewId)?.meta?.let { ChatLoad.Ready(it) } ?: ChatLoad.Loading,
    )
    val meta: StateFlow<ChatLoad<ChatRoomMeta>> = _meta

    /** 24 채팅 알림 — 고른 값(적용 전) */
    val notifyChoice = MutableStateFlow<Boolean?>(null)
    val notifyState = MutableStateFlow(ChatConfirm())
    val leaveState = MutableStateFlow<ChatConfirm?>(null)

    private val _ended = MutableStateFlow(false)
    val ended: StateFlow<Boolean> = _ended

    fun refresh() {
        viewModelScope.launch {
            when (val outcome = repo.sync(crewId, repo.snapshot(crewId)?.lastRev)) {
                is ChatOutcome.Ok -> {
                    _meta.value = ChatLoad.Ready(outcome.value.meta)
                    val before = repo.snapshot(crewId)
                    if (before == null || outcome.value.reset) {
                        repo.keepSnapshot(crewId, outcome.value.copy(messages = outcome.value.messages, reset = true))
                    } else {
                        repo.keepSnapshot(crewId, before.copy(meta = outcome.value.meta, lastRev = before.lastRev))
                    }
                }
                is ChatOutcome.Ended -> _ended.value = true
                else -> if (_meta.value !is ChatLoad.Ready) _meta.value = ChatLoad.Failed
            }
        }
    }

    fun openNotify() {
        notifyChoice.value = (meta.value as? ChatLoad.Ready)?.value?.notify ?: true
        notifyState.value = ChatConfirm()
    }

    fun closeNotify() {
        notifyChoice.value = null
    }

    /** 적용 — 나에게만. 저장되면 [onSaved](켰으면 기기 알림 권한을 화면이 다시 본다) */
    fun applyNotify(onSaved: (Boolean) -> Unit) {
        val choice = notifyChoice.value ?: return
        notifyState.value = ChatConfirm(busy = true)
        viewModelScope.launch {
            when (val outcome = repo.setNotify(crewId, choice)) {
                is ChatOutcome.Ok -> {
                    notifyChoice.value = null
                    notifyState.value = ChatConfirm()
                    (meta.value as? ChatLoad.Ready)?.value?.let { _meta.value = ChatLoad.Ready(it.copy(notify = outcome.value)) }
                    onSaved(outcome.value)
                }
                is ChatOutcome.Ended -> _ended.value = true
                else -> notifyState.value = ChatConfirm(error = ChatConfirmError.FAILED)
            }
        }
    }

    fun askLeave() {
        leaveState.value = ChatConfirm()
    }

    fun closeLeave() {
        leaveState.value = null
    }

    fun confirmLeave() {
        leaveState.value = ChatConfirm(busy = true)
        viewModelScope.launch {
            when (val outcome = repo.leave(crewId)) {
                is ChatOutcome.Ok, is ChatOutcome.Ended -> {
                    leaveState.value = null
                    ServiceLocator.crewCards.refresh()
                    _ended.value = true
                }
                else -> leaveState.value = ChatConfirm(error = outcome.confirmError())
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ChatInfoViewModel(ServiceLocator.crewChat, createSavedStateHandle()) }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 06 크루원 · 07 · 33 크루원 프로필 · 34 내보내기
// ─────────────────────────────────────────────────────────────

class ChatPeopleViewModel(
    private val repo: CrewChatRepository,
    saved: SavedStateHandle,
) : ViewModel() {
    val crewId: String = saved.get<String>("crewId").orEmpty()
    val userId: String = saved.get<String>("userId").orEmpty()

    val members = MutableStateFlow<ChatLoad<List<CrewMember>>>(ChatLoad.Loading)
    val person = MutableStateFlow<ChatLoad<CrewPerson>>(ChatLoad.Loading)
    val meta = MutableStateFlow(repo.snapshot(crewId)?.meta)
    val me = MutableStateFlow("")
    val removeState = MutableStateFlow<ChatConfirm?>(null)

    private val _ended = MutableStateFlow(false)
    val ended: StateFlow<Boolean> = _ended

    private val _removed = MutableStateFlow(false)
    val removed: StateFlow<Boolean> = _removed

    init {
        viewModelScope.launch { me.value = repo.me() }
    }

    fun loadMembers() {
        viewModelScope.launch {
            refreshMeta()
            members.value = when (val outcome = repo.members(crewId)) {
                is ChatOutcome.Ok -> ChatLoad.Ready(outcome.value)
                is ChatOutcome.Ended -> ChatLoad.Ended.also { _ended.value = true }
                else -> ChatLoad.Failed
            }
        }
    }

    fun loadPerson() {
        viewModelScope.launch {
            refreshMeta()
            person.value = when (val outcome = repo.person(crewId, userId)) {
                is ChatOutcome.Ok -> ChatLoad.Ready(outcome.value)
                is ChatOutcome.Ended -> ChatLoad.Ended.also { _ended.value = true }
                else -> ChatLoad.Failed
            }
        }
    }

    /** 크루장 여부는 서버의 지금 값으로 — 크루장이 바뀌면 내보내기 버튼도 바로 사라진다 */
    private suspend fun refreshMeta() {
        when (val outcome = repo.sync(crewId, repo.snapshot(crewId)?.lastRev)) {
            is ChatOutcome.Ok -> meta.value = outcome.value.meta
            is ChatOutcome.Ended -> _ended.value = true
            else -> Unit
        }
    }

    fun askRemove() {
        removeState.value = ChatConfirm()
    }

    fun closeRemove() {
        removeState.value = null
    }

    fun confirmRemove() {
        removeState.value = ChatConfirm(busy = true)
        viewModelScope.launch {
            when (val outcome = repo.removeMember(crewId, userId)) {
                is ChatOutcome.Ok -> {
                    removeState.value = null
                    ServiceLocator.crewCards.refresh()
                    _removed.value = true
                }
                is ChatOutcome.Ended -> _ended.value = true
                is ChatOutcome.NotOwner -> {
                    removeState.value = null
                    refreshMeta()
                }
                else -> removeState.value = ChatConfirm(error = outcome.confirmError())
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ChatPeopleViewModel(ServiceLocator.crewChat, createSavedStateHandle()) }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 08 공지 · 09 공지 모아보기
// ─────────────────────────────────────────────────────────────

class ChatNoticesViewModel(
    private val repo: CrewChatRepository,
    saved: SavedStateHandle,
) : ViewModel() {
    val crewId: String = saved.get<String>("crewId").orEmpty()
    val noticeId: Long = saved.get<Long>("noticeId") ?: 0L

    val notices = MutableStateFlow<ChatLoad<List<ChatNotice>>>(ChatLoad.Loading)
    val meta = MutableStateFlow(repo.snapshot(crewId)?.meta)

    private val _ended = MutableStateFlow(false)
    val ended: StateFlow<Boolean> = _ended

    fun load() {
        viewModelScope.launch {
            when (val sync = repo.sync(crewId, repo.snapshot(crewId)?.lastRev)) {
                is ChatOutcome.Ok -> meta.value = sync.value.meta
                is ChatOutcome.Ended -> {
                    _ended.value = true
                    return@launch
                }
                else -> Unit
            }
            notices.value = when (val outcome = repo.notices(crewId)) {
                is ChatOutcome.Ok -> ChatLoad.Ready(outcome.value)
                is ChatOutcome.Ended -> ChatLoad.Ended.also { _ended.value = true }
                else -> ChatLoad.Failed
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ChatNoticesViewModel(ServiceLocator.crewChat, createSavedStateHandle()) }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 10 등록 · 11 수정 · 12 삭제 · 43 저장 실패 · 44 초안
// ─────────────────────────────────────────────────────────────

data class ChatNoticeForm(
    val title: String = "",
    val body: String = "",
    val pinned: Boolean = true,
    /** 처음 연 값(바뀌었는지 — 나갈 때 44) */
    val original: Triple<String, String, Boolean> = Triple("", "", true),
) {
    val changed: Boolean get() = Triple(title, body, pinned) != original
    val ready: Boolean get() = ChatRules.noticeReady(title, body)
}

sealed interface ChatNoticeSheet {
    data class SaveFailed(val busy: Boolean = false) : ChatNoticeSheet
    data object KeepDraft : ChatNoticeSheet
    data class Delete(val state: ChatConfirm = ChatConfirm()) : ChatNoticeSheet
}

class ChatNoticeEditViewModel(
    private val repo: CrewChatRepository,
    saved: SavedStateHandle,
) : ViewModel() {
    val crewId: String = saved.get<String>("crewId").orEmpty()

    /** 수정하는 공지. 새 공지면 null */
    val noticeId: Long? = saved.get<Long>("noticeId")?.takeIf { it > 0 }
    private val fromMessage: Boolean = saved.get<Boolean>("fromMessage") == true

    val form = MutableStateFlow(ChatNoticeForm())
    val load = MutableStateFlow<ChatLoad<Unit>>(ChatLoad.Loading)
    val saving = MutableStateFlow(false)
    val sheet = MutableStateFlow<ChatNoticeSheet?>(null)

    private val _done = MutableStateFlow(false)

    /** 올렸다 · 고쳤다 · 지웠다 — 대화로 돌아간다 */
    val done: StateFlow<Boolean> = _done

    private val _ended = MutableStateFlow(false)
    val ended: StateFlow<Boolean> = _ended

    private val _ownerLost = MutableStateFlow(false)
    val ownerLost: StateFlow<Boolean> = _ownerLost

    /** 새 공지의 요청 키 — 다시 저장해도 공지 하나 */
    private val clientKey = saved.get<String>(KEY_CLIENT) ?: UUID.randomUUID().toString().also { saved[KEY_CLIENT] = it }

    init {
        viewModelScope.launch {
            val seed = if (fromMessage) ChatNoticeSeed.take(crewId) else null
            val draft = if (seed == null) repo.noticeDraft(crewId, noticeId) else null
            when {
                seed != null -> {
                    form.value = ChatNoticeForm(body = seed.take(ChatRules.NOTICE_BODY_MAX), original = Triple("", "", true))
                    load.value = ChatLoad.Ready(Unit)
                }
                noticeId == null -> {
                    form.value = draft?.let { ChatNoticeForm(it.title, it.body, it.pinned, Triple("", "", true)) } ?: ChatNoticeForm()
                    load.value = ChatLoad.Ready(Unit)
                }
                else -> {
                    when (val outcome = repo.notices(crewId)) {
                        is ChatOutcome.Ok -> {
                            val notice = outcome.value.firstOrNull { it.id == noticeId }
                            if (notice == null) {
                                load.value = ChatLoad.Failed
                            } else {
                                val original = Triple(notice.title, notice.body, notice.pinned)
                                // 이 공지를 고치다 남긴 초안이 있으면 그것으로(공지 내용을 생성 예시로 덮지 않는다)
                                form.value = draft?.let { ChatNoticeForm(it.title, it.body, it.pinned, original) }
                                    ?: ChatNoticeForm(notice.title, notice.body, notice.pinned, original)
                                load.value = ChatLoad.Ready(Unit)
                            }
                        }
                        is ChatOutcome.Ended -> _ended.value = true
                        else -> load.value = ChatLoad.Failed
                    }
                }
            }
        }
    }

    fun setTitle(text: String) = form.update { it.copy(title = text.replace('\n', ' ').take(ChatRules.NOTICE_TITLE_MAX)) }
    fun setBody(text: String) = form.update { it.copy(body = text.take(ChatRules.NOTICE_BODY_MAX)) }
    fun setPinned(on: Boolean) = form.update { it.copy(pinned = on) }

    /** 공지 올리기 · 수정 저장 — 서버가 받은 뒤에만 돌아간다. 실패하면 쓴 내용 · 기존 공지 그대로(43) */
    fun save() {
        val current = form.value
        if (!current.ready || saving.value) return
        saving.value = true
        if (sheet.value is ChatNoticeSheet.SaveFailed) sheet.value = ChatNoticeSheet.SaveFailed(busy = true)
        viewModelScope.launch {
            val outcome = repo.saveNotice(crewId, noticeId, current.title, current.body, current.pinned, clientKey.takeIf { noticeId == null })
            saving.value = false
            when (outcome) {
                is ChatOutcome.Ok -> {
                    repo.saveNoticeDraft(null, crewId, noticeId)
                    sheet.value = null
                    _done.value = true
                }
                is ChatOutcome.Ended -> _ended.value = true
                is ChatOutcome.NotOwner -> {
                    repo.saveNoticeDraft(null, crewId, noticeId)
                    _ownerLost.value = true
                }
                else -> sheet.value = ChatNoticeSheet.SaveFailed()
            }
        }
    }

    /** 뒤로 — 바뀐 것이 있으면 44(남길까요?) */
    fun back(onLeave: () -> Unit) {
        if (form.value.changed && !form.value.let { it.title.isBlank() && it.body.isBlank() }) sheet.value = ChatNoticeSheet.KeepDraft
        else onLeave()
    }

    /** 44 저장 후 나가기 — 올리지 않고 이 폰에 초안으로(쓴 사람 · 방 · 새 공지/수정마다) */
    fun keepDraft(onLeave: () -> Unit) {
        val current = form.value
        viewModelScope.launch {
            repo.saveNoticeDraft(ChatNoticeDraft(crewId, noticeId, current.title, current.body, current.pinned), crewId, noticeId)
            sheet.value = null
            onLeave()
        }
    }

    fun closeSheet() {
        sheet.value = null
    }

    fun askDelete() {
        sheet.value = ChatNoticeSheet.Delete()
    }

    fun confirmDelete() {
        val id = noticeId ?: return
        sheet.value = ChatNoticeSheet.Delete(ChatConfirm(busy = true))
        viewModelScope.launch {
            when (val outcome = repo.deleteNotice(crewId, id)) {
                is ChatOutcome.Ok -> {
                    repo.saveNoticeDraft(null, crewId, id)
                    sheet.value = null
                    _done.value = true
                }
                is ChatOutcome.Ended -> _ended.value = true
                is ChatOutcome.NotOwner -> _ownerLost.value = true
                else -> sheet.value = ChatNoticeSheet.Delete(ChatConfirm(error = outcome.confirmError()))
            }
        }
    }

    companion object {
        private const val KEY_CLIENT = "chat_notice_client"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ChatNoticeEditViewModel(ServiceLocator.crewChat, createSavedStateHandle()) }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 22 · 23 대화 검색
// ─────────────────────────────────────────────────────────────

sealed interface ChatSearchState {
    data object Idle : ChatSearchState
    data object Searching : ChatSearchState
    data class Found(val results: List<ChatMessage>) : ChatSearchState
    data object Empty : ChatSearchState
    data object Failed : ChatSearchState
}

class ChatSearchViewModel(
    private val repo: CrewChatRepository,
    saved: SavedStateHandle,
) : ViewModel() {
    val crewId: String = saved.get<String>("crewId").orEmpty()
    val query = MutableStateFlow("")
    val state = MutableStateFlow<ChatSearchState>(ChatSearchState.Idle)
    val meta = MutableStateFlow(repo.snapshot(crewId)?.meta)

    private val _ended = MutableStateFlow(false)
    val ended: StateFlow<Boolean> = _ended
    private var job: Job? = null

    fun setQuery(text: String) {
        query.value = text.replace('\n', ' ').take(ChatRules.SEARCH_MAX)
    }

    fun search() {
        val text = query.value.trim()
        job?.cancel()
        if (text.isEmpty()) {
            state.value = ChatSearchState.Idle
            return
        }
        state.value = ChatSearchState.Searching
        job = viewModelScope.launch {
            state.value = when (val outcome = repo.search(crewId, text)) {
                is ChatOutcome.Ok -> if (outcome.value.isEmpty()) ChatSearchState.Empty else ChatSearchState.Found(outcome.value)
                is ChatOutcome.Ended -> ChatSearchState.Idle.also { _ended.value = true }
                else -> ChatSearchState.Failed
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ChatSearchViewModel(ServiceLocator.crewChat, createSavedStateHandle()) }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 26 사진 미리보기 · 42 사진 전송 실패 · 41 받은 사진
// ─────────────────────────────────────────────────────────────

/** 보낼 사진 — 방마다 하나(미리보기 화면이 꺼내 쓴다) */
object ChatPhotoDraft {
    private val photos = java.util.concurrent.ConcurrentHashMap<String, ChatPhoto>()

    fun put(crewId: String, photo: ChatPhoto) {
        photos[crewId] = photo
    }

    fun get(crewId: String): ChatPhoto? = photos[crewId]

    fun clear(crewId: String) {
        photos.remove(crewId)?.file?.delete()
    }
}

class ChatPhotoViewModel(
    private val repo: CrewChatRepository,
    saved: SavedStateHandle,
) : ViewModel() {
    val crewId: String = saved.get<String>("crewId").orEmpty()
    val photo: ChatPhoto? = ChatPhotoDraft.get(crewId)
    val caption = MutableStateFlow("")
    val sending = MutableStateFlow(false)

    /** 42 사진을 보내지 못했어요 */
    val failed = MutableStateFlow(false)

    private val _sent = MutableStateFlow(false)
    val sent: StateFlow<Boolean> = _sent
    private val _ended = MutableStateFlow(false)
    val ended: StateFlow<Boolean> = _ended

    /** 같은 사진 · 설명은 같은 요청 키로(다시 보내도 서버에는 하나) */
    private var outgoing: ChatMessage? = null

    fun setCaption(text: String) {
        caption.value = text.take(ChatRules.BODY_MAX)
    }

    fun send() {
        val photo = photo ?: return
        if (sending.value) return
        sending.value = true
        viewModelScope.launch {
            val message = outgoing?.copy(body = caption.value.trim())
                ?: repo.outgoing(crewId, caption.value.trim(), null, photo).also { outgoing = it }
            outgoing = message
            when (repo.send(crewId, message, photo)) {
                is ChatOutcome.Ok -> {
                    failed.value = false
                    ChatPhotoDraft.clear(crewId)
                    _sent.value = true
                }
                is ChatOutcome.Ended -> _ended.value = true
                else -> failed.value = true
            }
            sending.value = false
        }
    }

    fun closeFailed() {
        failed.value = false
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ChatPhotoViewModel(ServiceLocator.crewChat, createSavedStateHandle()) }
        }
    }
}

class ChatViewerViewModel(
    private val repo: CrewChatRepository,
    saved: SavedStateHandle,
) : ViewModel() {
    val crewId: String = saved.get<String>("crewId").orEmpty()
    val messageId: Long = saved.get<Long>("messageId") ?: 0L
    val message: ChatMessage? = repo.snapshot(crewId)?.messages?.firstOrNull { it.id == messageId }
    val image = MutableStateFlow<ChatLoad<android.graphics.Bitmap>>(
        repo.imageNow(messageId)?.let { ChatLoad.Ready(it) } ?: ChatLoad.Loading,
    )

    private val _ended = MutableStateFlow(false)
    val ended: StateFlow<Boolean> = _ended

    init {
        if (image.value !is ChatLoad.Ready) load()
    }

    fun load() {
        image.value = ChatLoad.Loading
        viewModelScope.launch {
            image.value = when (val outcome = repo.image(crewId, messageId)) {
                is ChatOutcome.Ok -> ChatLoad.Ready(outcome.value)
                is ChatOutcome.Ended -> ChatLoad.Ended.also { _ended.value = true }
                else -> ChatLoad.Failed
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ChatViewerViewModel(ServiceLocator.crewChat, createSavedStateHandle()) }
        }
    }
}

/** 고른 사진을 보낼 모양으로(25 → 26) */
internal suspend fun prepareChatPhoto(context: Context, crewId: String, uri: Uri): CrewPhotos.Loaded {
    val (photo, loaded) = ServiceLocator.crewChat.preparePhoto(context, uri)
    if (photo != null) ChatPhotoDraft.put(crewId, photo)
    return if (photo == null && loaded is CrewPhotos.Loaded.Ok) CrewPhotos.Loaded.Broken else loaded
}
