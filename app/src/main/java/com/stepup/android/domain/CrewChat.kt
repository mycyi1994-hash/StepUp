package com.stepup.android.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 크루 채팅(2026-09-29 크루 채팅 패키지) — 크루마다 크루원 전용 방 하나(방 id = 크루 id).
 *
 * 서버(0048)가 기준이다: 가입 · 역할 · 삭제 가능 여부 · 인원 · 순서 · 받았는지. 이 폰은 보내는 중 · 전송 대기 ·
 * 실패한 내 메시지만 따로 들고, 서버가 같은 요청 키(clientId)의 메시지를 돌려주면 그것으로 바꾼다(두 개가 되지 않게).
 */
enum class ChatRole {
    OWNER, MEMBER;

    companion object {
        fun of(raw: String?): ChatRole = if (raw == "OWNER") OWNER else MEMBER
    }
}

enum class ChatKind {
    TEXT, IMAGE, SYSTEM;

    companion object {
        fun of(raw: String?): ChatKind = entries.firstOrNull { it.name == raw } ?: TEXT
    }
}

/** 보이는 메시지 · 내가 지운 메시지 · 크루장이 숨긴 메시지 — 지우거나 숨긴 것은 내용 · 사진을 싣지 않는다 */
enum class ChatState {
    VISIBLE, DELETED, HIDDEN;

    companion object {
        fun of(raw: String?): ChatState = entries.firstOrNull { it.name == raw } ?: VISIBLE
    }
}

/** 대화에 남는 알림 줄 — "○○ 님이 새 공지를 등록했어요." · "○○ 님이 크루에서 나갔어요." */
enum class ChatEvent {
    NOTICE_CREATED, MEMBER_LEFT;

    companion object {
        fun of(raw: String?): ChatEvent? = entries.firstOrNull { it.name == raw }
    }
}

/** 내 메시지가 서버에 닿았는가 — 보내는 중 · 전송 대기(연결이 끊겨 받았는지 모른다) · 실패 */
enum class ChatDelivery { SENT, SENDING, PENDING, FAILED }

/** 답장의 원문 — 지웠거나 숨겼으면 내용 없이 상태만 */
data class ChatReply(
    val id: Long,
    val seq: Long,
    val authorId: String?,
    val authorName: String,
    val kind: ChatKind,
    val state: ChatState,
    val body: String?,
)

data class ChatMessage(
    /** 서버 번호 — 서버가 받기 전에는 null */
    val id: Long?,
    /** 방 안의 순서 */
    val seq: Long?,
    /** 마지막으로 바뀐 때(삭제 · 숨김 · 인용 원문 변경) */
    val rev: Long = 0,
    val authorId: String?,
    val authorName: String?,
    val kind: ChatKind,
    val state: ChatState = ChatState.VISIBLE,
    val body: String?,
    val hasImage: Boolean = false,
    val event: ChatEvent? = null,
    val eventName: String? = null,
    val reply: ChatReply? = null,
    /** 내가 보낸 메시지의 요청 키(서버는 보낸 사람에게만 돌려준다) */
    val clientId: String? = null,
    val canDelete: Boolean = false,
    val createdAt: Long,
    val delivery: ChatDelivery = ChatDelivery.SENT,
    /** 보내는 중인 사진(앱 안 파일) — 서버가 받으면 서버 사진을 쓴다 */
    val localImage: String? = null,
) {
    /** 목록의 열쇠 — 내 메시지는 요청 키, 남의 메시지는 서버 번호(보낸 뒤에도 같은 줄로 남게) */
    val key: String get() = clientId?.let { "c:$it" } ?: "m:$id"

    val visible: Boolean get() = state == ChatState.VISIBLE
}

data class ChatNotice(
    val id: Long,
    val crewId: String,
    val title: String,
    val body: String,
    val pinned: Boolean,
    val authorId: String?,
    val authorName: String,
    val createdAt: Long,
    val updatedAt: Long,
) {
    /** 목록 · 모아보기에 보일 한 줄 — 내용의 첫 줄(비었으면 없음) */
    val preview: String get() = body.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()
}

/** 크루원의 읽은 위치 — 가입한 때보다 먼저 온 메시지는 그 사람의 미확인으로 세지 않는다 */
data class ChatRead(val userId: String, val seq: Long, val joinedAt: Long)

/** 방 정보 — 제목 · 대표 이미지 · 지금 크루원 수(접속자 수 아님) · 내 역할 · 고정 공지 · 내 알림 */
data class ChatRoomMeta(
    val crewId: String,
    val name: String,
    val imageBg: Int?,
    val imageVer: Int,
    val hasImage: Boolean,
    val ownerId: String?,
    val role: ChatRole,
    val memberCount: Int,
    val lastRev: Long,
    val notify: Boolean,
    val noticeCount: Int,
    val pinned: ChatNotice?,
    val myReadSeq: Long,
    /** 크루원의 읽은 위치 — 크루가 너무 커 서버가 싣지 않으면 null(내 메시지 옆 숫자를 만들지 않는다) */
    val reads: List<ChatRead>?,
) {
    val owner: Boolean get() = role == ChatRole.OWNER

    /** 크루 명함형의 대표 이미지 부품에 넘길 모양 */
    val card: CrewCard get() = CrewCard(id = crewId, name = name, imageBg = imageBg, imageVer = imageVer, hasImage = hasImage, memberCount = memberCount)
}

/** 01 내 크루 대화의 한 줄 */
data class ChatRoomSummary(
    val crewId: String,
    val name: String,
    val imageBg: Int?,
    val imageVer: Int,
    val hasImage: Boolean,
    val ownerId: String?,
    val role: ChatRole,
    val memberCount: Int,
    val last: ChatMessage?,
    val unread: Int,
    val createdAt: Long,
) {
    val card: CrewCard get() = CrewCard(id = crewId, name = name, imageBg = imageBg, imageVer = imageVer, hasImage = hasImage, memberCount = memberCount)

    /** 혼자 만든 새 크루 — 아직 대화가 없고 나 혼자다(30) */
    val solo: Boolean get() = memberCount <= 1 && last == null
}

/** 목록 · 알림에 보일 마지막 메시지 한 줄 — 글은 화면이 언어에 맞게 짓는다 */
sealed interface ChatPreview {
    data object Empty : ChatPreview
    data class Text(val author: String, val body: String) : ChatPreview
    data class Photo(val author: String) : ChatPreview
    data object Deleted : ChatPreview
    data object Hidden : ChatPreview
    data class Notice(val name: String) : ChatPreview
    data class Left(val name: String) : ChatPreview
}

/** 화면에 늘어놓을 한 줄 — 날짜 줄 · 알림 줄 · 말풍선 */
sealed interface ChatItem {
    val key: String

    data class Day(val day: LocalDate) : ChatItem {
        override val key: String get() = "d:$day"
    }

    data class Line(val message: ChatMessage) : ChatItem {
        override val key: String get() = message.key
    }

    data class Bubble(
        val message: ChatMessage,
        /** 내가 보낸 메시지(작성자와 지금 사용자가 같다) — 크루장이라고 모두 오른쪽이 아니다 */
        val mine: Boolean,
        /** 이름(남의 메시지) · "나 크루장"(내가 크루장일 때)을 보인다 — 같은 사람이 이어 보낸 둘째부터는 뺀다 */
        val header: Boolean,
        /** 보낸 시간 — 같은 사람이 같은 분에 이어 보낸 것은 마지막에만 */
        val time: Boolean,
        /** 작성자가 지금의 크루장 */
        val owner: Boolean,
        /** 내 메시지를 아직 읽지 않은 크루원 수(없거나 셀 수 없으면 null) */
        val unread: Int?,
    ) : ChatItem {
        override val key: String get() = message.key
    }
}

/** 신고 사유 — 서버 칸(content_reports.reason) 그대로 */
enum class ChatReportReason(val server: String) { ABUSE("ABUSE"), SPAM("SPAM"), CONTENT("OTHER") }

/** 쓰다 만 공지 — 쓴 사람 · 방 · 새 공지/수정(공지 번호)마다 따로 남는다 */
data class ChatNoticeDraft(
    val crewId: String,
    /** 수정하던 공지. 새 공지면 null */
    val noticeId: Long?,
    val title: String,
    val body: String,
    val pinned: Boolean,
    val savedAt: Long = 0L,
) {
    val key: String get() = ChatRules.noticeDraftKey(crewId, noticeId)

    val empty: Boolean get() = title.isBlank() && body.isBlank()
}

object ChatRules {
    /** 서버 칸 한도(0048) — 기술적 한도다(새 정책이 아니다) */
    const val BODY_MAX = 2000
    const val NOTICE_TITLE_MAX = 100
    const val NOTICE_BODY_MAX = 2000
    const val SEARCH_MAX = 60

    /** 처음 열 때 · 이전 대화 한 번에 받는 수 */
    const val PAGE = 50

    /** 방을 보는 동안 다시 읽는 간격 · 연결이 끊겼을 때 다시 해 보는 간격 */
    const val POLL_MS = 3_000L
    const val RETRY_MS = 4_000L

    /** 목록(01)을 보는 동안 다시 읽는 간격 */
    const val LIST_POLL_MS = 15_000L

    /** 이름 · 시간 묶음 — 같은 사람이 이 안에 이어 보내면 이름을 한 번만 */
    const val GROUP_MS = 5 * 60_000L

    /** 공백만 있으면 보낼 수 없다 */
    fun sendable(text: String): Boolean = text.isNotBlank() && text.trim().length <= BODY_MAX

    fun noticeDraftKey(crewId: String, noticeId: Long?): String = if (noticeId == null) "$crewId:new" else "$crewId:edit:$noticeId"

    /** 공지 제목이 있어야 올린다(내용은 선택) */
    fun noticeReady(title: String, body: String): Boolean =
        title.trim().length in 1..NOTICE_TITLE_MAX && body.trim().length <= NOTICE_BODY_MAX

    /**
     * 서버에서 온 메시지를 지금 목록에 합친다. 같은 서버 번호는 더 늦게 바뀐 것(rev)으로, 이 폰에만 있는 내 메시지
     * (보내는 중 · 전송 대기 · 실패)는 서버가 같은 요청 키의 메시지를 돌려주면 빠진다 — 두 개로 보이지 않는다.
     * 서버 메시지는 방 순서대로, 이 폰에만 있는 것은 맨 아래(보낸 차례대로).
     */
    fun merge(current: List<ChatMessage>, incoming: List<ChatMessage>): List<ChatMessage> {
        val byId = LinkedHashMap<Long, ChatMessage>()
        for (message in current) {
            val id = message.id ?: continue
            byId[id] = message
        }
        for (message in incoming) {
            val id = message.id ?: continue
            val before = byId[id]
            if (before == null || message.rev >= before.rev) byId[id] = message
        }
        val acknowledged = byId.values.mapNotNullTo(HashSet()) { it.clientId }
        val local = current.filter { it.id == null && it.clientId !in acknowledged }
        return byId.values.sortedWith(compareBy<ChatMessage> { it.seq ?: Long.MAX_VALUE }.thenBy { it.id }) + local
    }

    /** 방이 처음부터 다시 왔을 때(reset) — 이 폰에만 있는 내 메시지는 남기고 서버 것은 새로 받은 것으로 */
    fun replace(current: List<ChatMessage>, incoming: List<ChatMessage>): List<ChatMessage> =
        merge(current.filter { it.id == null }, incoming)

    /**
     * 내 메시지 옆 미확인 수 — 나를 뺀 지금 크루원 중 그 메시지보다 앞까지만 읽은 사람(메시지 뒤에 가입한 사람은 빼고).
     * 방 정보에 읽은 위치가 없으면 null(숫자를 만들어 보이지 않는다). 0 이면 보이지 않는다.
     */
    fun unread(message: ChatMessage, meta: ChatRoomMeta, me: String): Int? {
        if (message.authorId != me || message.delivery != ChatDelivery.SENT || message.kind == ChatKind.SYSTEM) return null
        val seq = message.seq ?: return null
        val reads = meta.reads ?: return null
        val count = reads.count { it.userId != me && it.seq < seq && it.joinedAt <= message.createdAt }
        return count.takeIf { it > 0 }
    }

    fun preview(message: ChatMessage?): ChatPreview {
        message ?: return ChatPreview.Empty
        return when {
            message.kind == ChatKind.SYSTEM -> when (message.event) {
                ChatEvent.NOTICE_CREATED -> ChatPreview.Notice(message.eventName.orEmpty())
                ChatEvent.MEMBER_LEFT -> ChatPreview.Left(message.eventName.orEmpty())
                null -> ChatPreview.Empty
            }
            message.state == ChatState.DELETED -> ChatPreview.Deleted
            message.state == ChatState.HIDDEN -> ChatPreview.Hidden
            message.kind == ChatKind.IMAGE && message.body.isNullOrBlank() -> ChatPreview.Photo(message.authorName.orEmpty())
            else -> ChatPreview.Text(message.authorName.orEmpty(), oneLine(message.body.orEmpty()))
        }
    }

    /** 여러 줄을 한 줄로(목록 미리보기 · 답장 원문) */
    fun oneLine(text: String): String = text.trim().replace(Regex("\\s+"), " ")

    /** 방 순서의 마지막(서버가 받은 것 중) */
    fun newestSeq(messages: List<ChatMessage>): Long = messages.maxOfOrNull { it.seq ?: 0L } ?: 0L

    /**
     * 늘어놓을 줄 — 날짜가 바뀌면 날짜 줄, 알림 줄은 가운데 한 줄. 같은 사람이 [GROUP_MS] 안에 이어 보내면 이름은 첫
     * 줄에만, 시간은 같은 분의 마지막 줄에만. 크루장 표시는 지금의 크루장(방 정보) 기준이다.
     */
    fun timeline(messages: List<ChatMessage>, meta: ChatRoomMeta, me: String, zone: ZoneId): List<ChatItem> {
        val items = ArrayList<ChatItem>(messages.size + 4)
        var lastDay: LocalDate? = null
        for ((index, message) in messages.withIndex()) {
            val day = Instant.ofEpochMilli(message.createdAt).atZone(zone).toLocalDate()
            if (day != lastDay) {
                items += ChatItem.Day(day)
                lastDay = day
            }
            if (message.kind == ChatKind.SYSTEM) {
                items += ChatItem.Line(message)
                continue
            }
            val previous = messages.getOrNull(index - 1)
            val next = messages.getOrNull(index + 1)
            val continues = previous != null && previous.kind != ChatKind.SYSTEM && previous.authorId == message.authorId &&
                sameDay(previous, message, zone) && message.createdAt - previous.createdAt <= GROUP_MS
            val sameMinuteNext = next != null && next.kind != ChatKind.SYSTEM && next.authorId == message.authorId &&
                next.createdAt / 60_000 == message.createdAt / 60_000 && next.delivery == message.delivery
            val mine = message.authorId == me
            val owner = message.authorId != null && message.authorId == meta.ownerId
            items += ChatItem.Bubble(
                message = message,
                mine = mine,
                header = !continues && (!mine || owner),
                time = !sameMinuteNext,
                owner = owner,
                unread = unread(message, meta, me),
            )
        }
        return items
    }

    private fun sameDay(a: ChatMessage, b: ChatMessage, zone: ZoneId): Boolean =
        Instant.ofEpochMilli(a.createdAt).atZone(zone).toLocalDate() == Instant.ofEpochMilli(b.createdAt).atZone(zone).toLocalDate()

    /** 이름 첫 글자(말풍선 옆 동그라미) */
    fun initial(name: String?): String = name?.trim()?.firstOrNull()?.toString() ?: "?"
}
