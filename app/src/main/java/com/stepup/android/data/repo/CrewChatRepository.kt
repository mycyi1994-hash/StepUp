package com.stepup.android.data.repo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.LruCache
import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.data.remote.ChatMessageRow
import com.stepup.android.data.remote.CrewApi
import com.stepup.android.data.remote.CrewChatApi
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.domain.ChatDelivery
import com.stepup.android.domain.ChatKind
import com.stepup.android.domain.ChatMessage
import com.stepup.android.domain.ChatNotice
import com.stepup.android.domain.ChatNoticeDraft
import com.stepup.android.domain.ChatReportReason
import com.stepup.android.domain.ChatRoomMeta
import com.stepup.android.domain.ChatRoomSummary
import com.stepup.android.domain.CrewMember
import com.stepup.android.domain.CrewPerson
import com.stepup.android.domain.CrewPersonRole
import com.stepup.android.domain.RecordingOwner
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** 크루 채팅 한 동작의 결말 — 화면은 이것으로 다음 화면(32 · 크루원으로 돌아가기 · 짧은 오류)을 고른다 */
sealed interface ChatOutcome<out T> {
    data class Ok<T>(val value: T) : ChatOutcome<T>

    /** 지금 이 크루의 멤버가 아니다(탈퇴 · 내보내기 · 해산) — 32 채팅 참여 종료 */
    data object Ended : ChatOutcome<Nothing>

    /** 크루장 권한만 없다(넘겼거나 바뀌었다) — 크루원으로 돌아간다 */
    data object NotOwner : ChatOutcome<Nothing>

    /** 연결 · 서버 사정 — 받았는지 모른다. 다시 하면 될 수 있다 */
    data class Offline(val reason: String) : ChatOutcome<Nothing>

    /** 서버가 거절했다(이미 처리됨 · 잘못된 값) — 다시 보내도 같다 */
    data class Rejected(val reason: String) : ChatOutcome<Nothing>
}

/** 방을 따라온 결과 */
data class ChatSync(val meta: ChatRoomMeta, val messages: List<ChatMessage>, val reset: Boolean, val lastRev: Long)

/** 보낼 사진 — 올릴 모양(JPEG base64)과 화면에 보일 앱 안 파일 */
data class ChatPhoto(val data: String, val file: File, val width: Int, val height: Int)

/** 01 내 크루 대화의 상태 */
sealed interface ChatRoomsState {
    data object Loading : ChatRoomsState
    data class Ready(val rooms: List<ChatRoomSummary>) : ChatRoomsState
    data class Failed(val reason: String) : ChatRoomsState

    /** 로그인하지 않았다 — 가입한 크루의 대화는 로그인한 뒤에 */
    data object SignIn : ChatRoomsState
}

/**
 * 크루 채팅 — 서버(0048)가 기준이다. 이 폰은 아직 서버가 받지 않은 내 메시지(보내는 중 · 전송 대기 · 실패)와
 * 입력창에 쓰던 글만 들고 있다(앱이 살아 있는 동안). 이 방에 참여할 수 없게 되면 그 방의 대화 · 사진 · 쓰다 만 공지를
 * 지운다([forget]) — 다시 보이지 않게.
 */
class CrewChatRepository(
    private val api: CrewChatApi,
    private val crewApi: CrewApi,
    private val prefs: UserPrefs,
    private val owner: suspend () -> String,
    /** 보낼 사진을 잠깐 두는 곳(cacheDir/chat_photos) */
    private val photoDir: File,
) {
    private val _rooms = MutableStateFlow<ChatRoomsState>(ChatRoomsState.Loading)
    val rooms: StateFlow<ChatRoomsState> = _rooms

    /** 방마다 — 이 폰에만 있는 내 메시지 · 입력창 글 */
    private val locals = ConcurrentHashMap<String, List<ChatMessage>>()
    private val drafts = ConcurrentHashMap<String, String>()

    /** 방마다 마지막으로 본 대화(다시 열 때 먼저 보인다 — 서버가 확인하면 바뀐다) */
    private val snapshots = ConcurrentHashMap<String, ChatSync>()

    private val images = object : LruCache<Long, Bitmap>(IMAGE_BYTES) {
        override fun sizeOf(key: Long, value: Bitmap): Int = value.byteCount
    }
    private val imageRooms = ConcurrentHashMap<Long, String>()
    private val imageLocks = ConcurrentHashMap<Long, Mutex>()

    /** 이 폰에 들고 있는 대화의 주인 — 계정이 바뀌면 앞사람의 대화 · 사진 · 입력 글을 모두 지운다 */
    @Volatile private var holder: String? = null

    private suspend fun ownerNow(): String {
        val now = owner()
        val before = holder
        if (before != null && before != now) clearAll()
        holder = now
        return now
    }

    /** 지금 로그인한 사람의 서버 id — 작성자와 같으면 오른쪽 말풍선 */
    suspend fun me(): String = RecordingOwner.userId(ownerNow()).orEmpty()

    suspend fun ownerKey(): String = ownerNow()

    // ── 목록 ────────────────────────────────────────────────────

    suspend fun refreshRooms(): ChatOutcome<List<ChatRoomSummary>> {
        if (RecordingOwner.userId(ownerNow()) == null) {
            _rooms.value = ChatRoomsState.SignIn
            return ChatOutcome.Offline("sign in")
        }
        val result = api.rooms()
        if (result is ServerResult.SignInRequired) {
            _rooms.value = ChatRoomsState.SignIn
            return ChatOutcome.Offline(result.reason)
        }
        val outcome = result.chat().map { rows -> rows.map { it.toDomain() } }
        when (outcome) {
            is ChatOutcome.Ok -> {
                // 목록에 없는 방(나갔다 · 내보내졌다 · 해산됐다)의 대화 · 사진 · 입력 글은 지운다
                val ids = outcome.value.mapTo(HashSet()) { it.crewId }
                (snapshots.keys + locals.keys + drafts.keys + imageRooms.values).filter { it !in ids }.toSet().forEach { forget(it) }
                _rooms.value = ChatRoomsState.Ready(outcome.value)
            }
            // 목록이 이미 있으면 그대로 두고(갱신 실패), 없을 때만 실패 화면
            is ChatOutcome.Offline -> if (_rooms.value !is ChatRoomsState.Ready) _rooms.value = ChatRoomsState.Failed(outcome.reason)
            is ChatOutcome.Rejected -> if (_rooms.value !is ChatRoomsState.Ready) _rooms.value = ChatRoomsState.Failed(outcome.reason)
            else -> Unit
        }
        return outcome
    }

    fun roomNow(crewId: String): ChatRoomSummary? = (_rooms.value as? ChatRoomsState.Ready)?.rooms?.firstOrNull { it.crewId == crewId }

    // ── 방 ─────────────────────────────────────────────────────

    suspend fun sync(crewId: String, sinceRev: Long?): ChatOutcome<ChatSync> =
        api.sync(crewId, sinceRev).chat().map { row ->
            ChatSync(row.room.toDomain(), row.messages.map { it.toDomain() }, row.reset, row.lastRev)
        }.also { if (it is ChatOutcome.Ended) forget(crewId) }

    fun snapshot(crewId: String): ChatSync? = snapshots[crewId]

    fun keepSnapshot(crewId: String, sync: ChatSync) {
        snapshots[crewId] = sync
    }

    suspend fun history(crewId: String, beforeSeq: Long): ChatOutcome<List<ChatMessage>> =
        api.history(crewId, beforeSeq, com.stepup.android.domain.ChatRules.PAGE).chat().map { rows -> rows.map { it.toDomain() } }
            .also { if (it is ChatOutcome.Ended) forget(crewId) }

    fun local(crewId: String): List<ChatMessage> = locals[crewId].orEmpty()

    fun setLocal(crewId: String, messages: List<ChatMessage>) {
        if (messages.isEmpty()) locals.remove(crewId) else locals[crewId] = messages
    }

    fun draft(crewId: String): String = drafts[crewId].orEmpty()

    fun setDraft(crewId: String, text: String) {
        if (text.isEmpty()) drafts.remove(crewId) else drafts[crewId] = text
    }

    /** 보낼 메시지 — 새 요청 키로. 서버가 받기 전까지 이 폰에만 있다 */
    suspend fun outgoing(crewId: String, body: String, replyTo: ChatMessage?, photo: ChatPhoto?): ChatMessage = ChatMessage(
        id = null,
        seq = null,
        authorId = me(),
        authorName = null,
        kind = if (photo == null) ChatKind.TEXT else ChatKind.IMAGE,
        body = body,
        hasImage = photo != null,
        reply = replyTo?.let {
            com.stepup.android.domain.ChatReply(
                id = it.id ?: 0L, seq = it.seq ?: 0L, authorId = it.authorId, authorName = it.authorName.orEmpty(),
                kind = it.kind, state = it.state, body = it.body,
            )
        },
        clientId = UUID.randomUUID().toString(),
        canDelete = false,
        createdAt = System.currentTimeMillis(),
        delivery = ChatDelivery.SENDING,
        localImage = photo?.file?.path,
    )

    /** 보내기 — 같은 요청 키로(다시 보내도 서버에는 하나) */
    suspend fun send(crewId: String, message: ChatMessage, photo: ChatPhoto?): ChatOutcome<ChatMessage> {
        val clientId = message.clientId ?: return ChatOutcome.Rejected("no client id")
        val outcome = api.send(crewId, clientId, message.body.orEmpty(), message.reply?.id?.takeIf { it > 0 }, photo?.data)
            .chat().map { it.toDomain() }
        if (outcome is ChatOutcome.Ok && photo != null) {
            val id = outcome.value.id
            if (id != null) decodeFile(photo.file)?.let { rememberImage(crewId, id, it) }
        }
        if (outcome is ChatOutcome.Ended) forget(crewId)
        return outcome
    }

    /** 연결이 돌아왔다 — 보내던 메시지 중 서버가 받은 것 */
    suspend fun confirm(crewId: String, clientIds: List<String>): ChatOutcome<List<ChatMessage>> =
        if (clientIds.isEmpty()) ChatOutcome.Ok(emptyList())
        else api.confirm(crewId, clientIds).chat().map { rows -> rows.map { it.toDomain() } }

    suspend fun read(crewId: String, seq: Long): ChatOutcome<Long> = api.read(crewId, seq).chat()

    suspend fun delete(crewId: String, message: ChatMessage): ChatOutcome<ChatMessage> {
        val id = message.id ?: return ChatOutcome.Rejected("not sent")
        return api.delete(id).chat().map(ChatMessageRow::toDomain).also {
            if (it is ChatOutcome.Ok) forgetImage(id)
            if (it is ChatOutcome.Ended) forget(crewId)
        }
    }

    suspend fun hide(crewId: String, message: ChatMessage): ChatOutcome<ChatMessage> {
        val id = message.id ?: return ChatOutcome.Rejected("not sent")
        return api.hide(id).chat().map(ChatMessageRow::toDomain).also {
            if (it is ChatOutcome.Ok) forgetImage(id)
            if (it is ChatOutcome.Ended) forget(crewId)
        }
    }

    suspend fun report(crewId: String, message: ChatMessage, reason: ChatReportReason): ChatOutcome<Unit> {
        val id = message.id ?: return ChatOutcome.Rejected("not sent")
        return api.report(id, reason.server).chat().also { if (it is ChatOutcome.Ended) forget(crewId) }
    }

    suspend fun search(crewId: String, query: String): ChatOutcome<List<ChatMessage>> =
        api.search(crewId, query.trim().take(com.stepup.android.domain.ChatRules.SEARCH_MAX)).chat()
            .map { rows -> rows.map { it.toDomain() } }
            .also { if (it is ChatOutcome.Ended) forget(crewId) }

    suspend fun setNotify(crewId: String, on: Boolean): ChatOutcome<Boolean> =
        api.notifySet(crewId, on).chat().also { if (it is ChatOutcome.Ended) forget(crewId) }

    // ── 사진 ────────────────────────────────────────────────────

    /** 받은 사진 — 방에 들어갈 수 있을 때만 서버가 준다. 앱이 살아 있는 동안 메모리에만 둔다 */
    suspend fun image(crewId: String, messageId: Long): ChatOutcome<Bitmap> {
        images.get(messageId)?.let { return ChatOutcome.Ok(it) }
        val lock = imageLocks.getOrPut(messageId) { Mutex() }
        return lock.withLock {
            images.get(messageId)?.let { return@withLock ChatOutcome.Ok(it) }
            when (val outcome = api.image(messageId).chat()) {
                is ChatOutcome.Ok -> {
                    val bitmap = withContext(Dispatchers.Default) { decodeBase64(outcome.value) }
                    if (bitmap == null) ChatOutcome.Rejected("broken image") else {
                        rememberImage(crewId, messageId, bitmap)
                        ChatOutcome.Ok(bitmap)
                    }
                }
                is ChatOutcome.Ended -> {
                    forget(crewId)
                    ChatOutcome.Ended
                }
                is ChatOutcome.NotOwner -> ChatOutcome.NotOwner
                is ChatOutcome.Offline -> outcome
                is ChatOutcome.Rejected -> outcome
            }
        }
    }

    fun imageNow(messageId: Long): Bitmap? = images.get(messageId)

    private fun rememberImage(crewId: String, messageId: Long, bitmap: Bitmap) {
        images.put(messageId, bitmap)
        imageRooms[messageId] = crewId
    }

    private fun forgetImage(messageId: Long) {
        images.remove(messageId)
        imageRooms.remove(messageId)
    }

    /**
     * 고른 사진을 보낼 모양으로 — 긴 변 [PHOTO_SIDE] 까지 줄이고 크루 대표 사진과 같은 기술 한도(base64 200,000자 —
     * 넉넉히 180,000자) 안의 JPEG 로. 읽을 수 없으면 null, 접근이 막혔으면 [CrewPhotos.Loaded.Denied].
     */
    suspend fun preparePhoto(context: Context, uri: Uri): Pair<ChatPhoto?, CrewPhotos.Loaded> {
        val loaded = CrewPhotos.load(context, uri)
        val source = (loaded as? CrewPhotos.Loaded.Ok)?.bitmap ?: return null to loaded
        val photo = withContext(Dispatchers.Default) { encodePhoto(source) }
        return photo to loaded
    }

    private fun encodePhoto(source: Bitmap): ChatPhoto? {
        var side = PHOTO_SIDE
        while (side >= 480) {
            val scale = side.toFloat() / maxOf(source.width, source.height)
            val bitmap = if (scale >= 1f) source else Bitmap.createScaledBitmap(
                source, (source.width * scale).toInt().coerceAtLeast(1), (source.height * scale).toInt().coerceAtLeast(1), true,
            )
            var quality = 84
            while (quality >= 46) {
                val bytes = ByteArrayOutputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                    out.toByteArray()
                }
                val text = Base64.encodeToString(bytes, Base64.NO_WRAP)
                if (text.length <= PHOTO_MAX_BASE64) {
                    photoDir.mkdirs()
                    val file = File(photoDir, "photo-${UUID.randomUUID()}.jpg")
                    file.writeBytes(bytes)
                    return ChatPhoto(text, file, bitmap.width, bitmap.height)
                }
                quality -= 12
            }
            side -= 240
        }
        return null
    }

    fun decodeFile(file: File): Bitmap? = runCatching { BitmapFactory.decodeFile(file.path) }.getOrNull()

    private fun decodeBase64(data: String): Bitmap? = runCatching {
        val bytes = Base64.decode(data, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()

    // ── 공지 ────────────────────────────────────────────────────

    suspend fun notices(crewId: String): ChatOutcome<List<ChatNotice>> =
        api.notices(crewId).chat().map { rows -> rows.map { it.toDomain() } }.also { if (it is ChatOutcome.Ended) forget(crewId) }

    suspend fun saveNotice(
        crewId: String,
        noticeId: Long?,
        title: String,
        body: String,
        pinned: Boolean,
        clientKey: String?,
        /** 이을 모임(4번) — [meetingChange] 가 참일 때만 서버가 바꾼다 */
        meetingId: Long? = null,
        meetingChange: Boolean = false,
    ): ChatOutcome<ChatNotice> =
        api.noticeSave(crewId, noticeId, title.trim(), body.trim(), pinned, clientKey, meetingId, meetingChange).chat().map { it.toDomain() }
            .also { if (it is ChatOutcome.Ended) forget(crewId) }


    suspend fun deleteNotice(crewId: String, noticeId: Long): ChatOutcome<Unit> =
        api.noticeDelete(noticeId).chat().also { if (it is ChatOutcome.Ended) forget(crewId) }

    suspend fun noticeDraft(crewId: String, noticeId: Long?): ChatNoticeDraft? =
        prefs.chatNoticeDraft(owner(), com.stepup.android.domain.ChatRules.noticeDraftKey(crewId, noticeId))

    suspend fun saveNoticeDraft(draft: ChatNoticeDraft?, crewId: String, noticeId: Long?) {
        prefs.setChatNoticeDraft(
            owner(), com.stepup.android.domain.ChatRules.noticeDraftKey(crewId, noticeId),
            draft?.copy(savedAt = System.currentTimeMillis()),
        )
    }

    // ── 크루원 ──────────────────────────────────────────────────

    /** 06 크루원 — 크루장 먼저, 먼저 들어온 순(기존 crew_roster) */
    suspend fun members(crewId: String): ChatOutcome<List<CrewMember>> =
        crewApi.roster(crewId).chat().map { rows ->
            rows.map { CrewMember(it.userId, it.name, it.role == "OWNER", it.joinedAt.isoToMillis(), it.weekKm, it.weekRuns) }
        }

    /** 07 · 33 크루원 한 사람(기존 crew_person) — 지금 멤버가 아니면 NONE */
    suspend fun person(crewId: String, userId: String): ChatOutcome<CrewPerson> =
        crewApi.person(crewId, userId).chat().map { row ->
            CrewPerson(
                userId = row.userId,
                name = row.name,
                role = CrewPersonRole.entries.firstOrNull { it.name == row.role } ?: CrewPersonRole.NONE,
                joinedAt = row.joinedAt?.isoToMillis(),
                weekKm = row.weekKm,
            )
        }

    /** 34 내보내기(기존 crew_member_remove) — 크루 소속과 채팅 참여가 함께 끝난다 */
    suspend fun removeMember(crewId: String, userId: String): ChatOutcome<Unit> =
        crewApi.removeMember(crewId, userId).chat()

    /** 크루 나가기(기존 crew_leave) — 채팅 참여도 함께 끝난다 */
    suspend fun leave(crewId: String): ChatOutcome<Unit> =
        crewApi.leave(crewId).chat().also { if (it is ChatOutcome.Ok) forget(crewId) }

    // ── 지우기 ──────────────────────────────────────────────────

    /**
     * 이 방에 참여할 수 없게 됐다 — 이 폰에 남은 그 방의 대화 · 보내던 메시지 · 입력 글 · 사진 · 쓰다 만 공지 ·
     * 목록 미리보기를 지운다.
     */
    suspend fun forget(crewId: String) {
        locals.remove(crewId)
        drafts.remove(crewId)
        snapshots.remove(crewId)
        imageRooms.entries.filter { it.value == crewId }.forEach { (id, _) -> forgetImage(id) }
        (_rooms.value as? ChatRoomsState.Ready)?.let { ready ->
            _rooms.value = ChatRoomsState.Ready(ready.rooms.filterNot { it.crewId == crewId })
        }
        runCatching { prefs.clearChatNoticeDrafts(crewId) }
    }

    /** 계정을 바꿨다 · 지웠다 — 다른 사람의 대화가 남지 않게 모두 지운다 */
    fun clearAll() {
        locals.clear()
        drafts.clear()
        snapshots.clear()
        images.evictAll()
        imageRooms.clear()
        _rooms.value = ChatRoomsState.Loading
        photoDir.listFiles()?.forEach { it.delete() }
    }

    companion object {
        private const val IMAGE_BYTES = 24 * 1024 * 1024

        /** 보낼 사진의 긴 변 */
        const val PHOTO_SIDE = 1200

        /** 서버 칸(200,000자)보다 넉넉히 작게 — 크루 대표 사진과 같은 한도 */
        private const val PHOTO_MAX_BASE64 = 180_000
    }
}

private fun <T, R> ChatOutcome<T>.map(transform: (T) -> R): ChatOutcome<R> = when (this) {
    is ChatOutcome.Ok -> ChatOutcome.Ok(transform(value))
    is ChatOutcome.Ended -> ChatOutcome.Ended
    is ChatOutcome.NotOwner -> ChatOutcome.NotOwner
    is ChatOutcome.Offline -> this
    is ChatOutcome.Rejected -> this
}

/** 서버의 답을 채팅의 결말로 — 멤버가 아니면 Ended, 크루장 권한이 없으면 NotOwner */
internal fun <T> ServerResult<T>.chat(): ChatOutcome<T> = when (this) {
    is ServerResult.Ok -> ChatOutcome.Ok(value)
    is ServerResult.Rejected -> when {
        reason.contains("chat_not_member") -> ChatOutcome.Ended
        reason.contains("크루장만") -> ChatOutcome.NotOwner
        else -> ChatOutcome.Rejected(reason)
    }
    is ServerResult.Retry -> ChatOutcome.Offline(reason)
    is ServerResult.SignInRequired -> ChatOutcome.Offline(reason)
}
