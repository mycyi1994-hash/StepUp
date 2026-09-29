package com.stepup.android.data.remote

import com.stepup.android.domain.ChatEvent
import com.stepup.android.domain.ChatKind
import com.stepup.android.domain.ChatMessage
import com.stepup.android.domain.ChatNotice
import com.stepup.android.domain.ChatRead
import com.stepup.android.domain.ChatReply
import com.stepup.android.domain.ChatRole
import com.stepup.android.domain.ChatRoomMeta
import com.stepup.android.domain.ChatRoomSummary
import com.stepup.android.domain.ChatState
import java.time.OffsetDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * 크루 채팅 — 서버 함수(`supabase/migrations/0048_crew_chat.sql`)만 부른다. 표에는 직접 읽고 쓰지 않는다.
 * 함수마다 지금의 가입 · 크루장 여부를 서버가 다시 본다(멤버가 아니면 chat_not_member).
 */
class CrewChatApi(private val server: StepUpServer) {

    /** 01 내 크루 대화 */
    suspend fun rooms(): ServerResult<List<ChatRoomRow>> =
        rpc("crew_chat_rooms", "{}") { serverJson.decodeFromString<List<ChatRoomRow>>(it) }

    /** 방을 따라온다 — [sinceRev] 뒤에 바뀐 메시지와 지금의 방 정보. null 이면 최근 50개(reset) */
    suspend fun sync(crewId: String, sinceRev: Long?): ServerResult<ChatSyncRow> =
        rpc(
            "crew_chat_sync",
            jsonBody {
                put("p_crew", crewId)
                put("p_since_rev", sinceRev ?: JsonNull)
            },
        ) { serverJson.decodeFromString<ChatSyncRow>(it) }

    /** 이전 대화 — [beforeSeq] 앞의 메시지(오래된 순) */
    suspend fun history(crewId: String, beforeSeq: Long, limit: Int): ServerResult<List<ChatMessageRow>> =
        rpc(
            "crew_chat_history",
            jsonBody {
                put("p_crew", crewId)
                put("p_before_seq", beforeSeq)
                put("p_limit", limit)
            },
        ) { serverJson.decodeFromString<List<ChatMessageRow>>(it) }

    /** 보내기 — 같은 [clientId] 로 다시 보내면 서버는 처음 저장한 메시지를 돌려준다 */
    suspend fun send(crewId: String, clientId: String, body: String, replyTo: Long?, image: String?): ServerResult<ChatMessageRow> =
        rpc(
            "crew_chat_send",
            jsonBody {
                put("p_crew", crewId)
                put("p_client_id", clientId)
                put("p_body", body)
                put("p_reply_to", replyTo ?: JsonNull)
                put("p_image", image ?: JsonNull)
            },
        ) { serverJson.decodeFromString<ChatMessageRow>(it) }

    /** 연결이 끊겼을 때 보내던 메시지 — 서버가 받은 것만 돌려준다 */
    suspend fun confirm(crewId: String, clientIds: List<String>): ServerResult<List<ChatMessageRow>> =
        rpc(
            "crew_chat_confirm",
            buildJsonObject {
                put("p_crew", crewId)
                putJsonArray("p_client_ids") { clientIds.forEach { add(JsonPrimitive(it)) } }
            }.toString(),
        ) { serverJson.decodeFromString<List<ChatMessageRow>>(it) }

    suspend fun read(crewId: String, seq: Long): ServerResult<Long> =
        rpc(
            "crew_chat_read",
            jsonBody {
                put("p_crew", crewId)
                put("p_seq", seq)
            },
        ) { it.trim().toLongOrNull() }

    suspend fun delete(messageId: Long): ServerResult<ChatMessageRow> =
        rpc("crew_chat_delete", jsonBody { put("p_message", messageId) }) { serverJson.decodeFromString<ChatMessageRow>(it) }

    suspend fun hide(messageId: Long): ServerResult<ChatMessageRow> =
        rpc("crew_chat_hide", jsonBody { put("p_message", messageId) }) { serverJson.decodeFromString<ChatMessageRow>(it) }

    /** 사진 원본(base64) */
    suspend fun image(messageId: Long): ServerResult<String> =
        rpc("crew_chat_image", jsonBody { put("p_message", messageId) }) { serverJson.decodeFromString<String?>(it) }

    suspend fun search(crewId: String, query: String): ServerResult<List<ChatMessageRow>> =
        rpc(
            "crew_chat_search",
            jsonBody {
                put("p_crew", crewId)
                put("p_query", query)
            },
        ) { serverJson.decodeFromString<List<ChatMessageRow>>(it) }

    suspend fun report(messageId: Long, reason: String): ServerResult<Unit> =
        rpc(
            "crew_chat_report",
            jsonBody {
                put("p_message", messageId)
                put("p_reason", reason)
            },
        ) { }

    suspend fun notifySet(crewId: String, on: Boolean): ServerResult<Boolean> =
        rpc(
            "crew_chat_notify_set",
            jsonBody {
                put("p_crew", crewId)
                put("p_on", on)
            },
        ) { it.trim().toBooleanStrictOrNull() }

    suspend fun notices(crewId: String): ServerResult<List<ChatNoticeRow>> =
        rpc("crew_chat_notices", jsonBody { put("p_crew", crewId) }) { serverJson.decodeFromString<List<ChatNoticeRow>>(it) }

    suspend fun noticeSave(
        crewId: String,
        noticeId: Long?,
        title: String,
        body: String,
        pinned: Boolean,
        clientKey: String?,
        /** 이을 모임(4번) — [meetingChange] 가 참일 때만 서버가 바꾼다(모르는 예전 앱이 고쳐도 풀리지 않게) */
        meetingId: Long? = null,
        meetingChange: Boolean = false,
    ): ServerResult<ChatNoticeRow> =
        rpc(
            "crew_chat_notice_save",
            jsonBody {
                put("p_crew", crewId)
                put("p_notice", noticeId ?: JsonNull)
                put("p_title", title)
                put("p_body", body)
                put("p_pinned", pinned)
                put("p_client_key", clientKey ?: JsonNull)
                if (meetingChange || meetingId != null) {
                    put("p_meeting", meetingId ?: JsonNull)
                    put("p_meeting_change", meetingChange)
                }
            },
        ) { serverJson.decodeFromString<ChatNoticeRow>(it) }

    /** 공지 하나(20) — 크루 홈 · 공지 목록에서 id 로 연다 */
    suspend fun notice(noticeId: Long): ServerResult<ChatNoticeRow> =
        rpc("crew_chat_notice", jsonBody { put("p_notice", noticeId) }) { serverJson.decodeFromString<ChatNoticeRow>(it) }

    suspend fun noticeDelete(noticeId: Long): ServerResult<Unit> =
        rpc("crew_chat_notice_delete", jsonBody { put("p_notice", noticeId) }) { }

    private suspend fun <T> rpc(name: String, body: String, parse: (String) -> T?): ServerResult<T> =
        server.authed { token ->
            server.http.post("${server.restUrl}/rpc/$name", body, server.headers(token))
        }.mapBody(parse)
}

@Serializable
data class ChatReplyRow(
    val id: Long,
    val seq: Long = 0,
    @SerialName("author_id") val authorId: String? = null,
    @SerialName("author_name") val authorName: String? = null,
    val kind: String = "TEXT",
    val state: String = "VISIBLE",
    val body: String? = null,
) {
    fun toDomain() = ChatReply(
        id = id, seq = seq, authorId = authorId, authorName = authorName.orEmpty(),
        kind = ChatKind.of(kind), state = ChatState.of(state), body = body,
    )
}

@Serializable
data class ChatMessageRow(
    val id: Long,
    val seq: Long,
    val rev: Long = 0,
    @SerialName("author_id") val authorId: String? = null,
    @SerialName("author_name") val authorName: String? = null,
    val kind: String = "TEXT",
    val state: String = "VISIBLE",
    val body: String? = null,
    @SerialName("has_image") val hasImage: Boolean = false,
    val event: String? = null,
    @SerialName("event_name") val eventName: String? = null,
    val reply: ChatReplyRow? = null,
    @SerialName("client_id") val clientId: String? = null,
    @SerialName("can_delete") val canDelete: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
) {
    fun toDomain() = ChatMessage(
        id = id,
        seq = seq,
        rev = rev,
        authorId = authorId,
        authorName = authorName,
        kind = ChatKind.of(kind),
        state = ChatState.of(state),
        body = body,
        hasImage = hasImage,
        event = ChatEvent.of(event),
        eventName = eventName,
        reply = reply?.toDomain(),
        clientId = clientId,
        canDelete = canDelete,
        createdAt = createdAt.chatMillis(),
    )
}

@Serializable
data class ChatNoticeRow(
    val id: Long,
    @SerialName("crew_id") val crewId: String = "",
    val title: String = "",
    val body: String = "",
    val pinned: Boolean = false,
    @SerialName("author_id") val authorId: String? = null,
    @SerialName("author_name") val authorName: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    val meeting: ChatNoticeMeetingRow? = null,
) {
    fun toDomain() = ChatNotice(
        id = id, crewId = crewId, title = title, body = body, pinned = pinned, authorId = authorId,
        authorName = authorName.orEmpty(), createdAt = createdAt.chatMillis(), updatedAt = updatedAt.chatMillis(),
        meeting = meeting?.toDomain(),
    )
}

/** 공지에 이은 모임(0049) */
@Serializable
data class ChatNoticeMeetingRow(
    val id: Long,
    val title: String? = null,
    val place: String? = null,
    @SerialName("meet_at") val meetAt: String? = null,
) {
    fun toDomain() = com.stepup.android.domain.ChatNoticeMeeting(
        id = id, title = title.orEmpty(), place = place.orEmpty(), meetAt = meetAt?.chatMillis()?.takeIf { it > 0 },
    )
}

@Serializable
data class ChatReadRow(
    @SerialName("user_id") val userId: String,
    val seq: Long = 0,
    @SerialName("joined_at") val joinedAt: String = "",
)

@Serializable
data class ChatMetaRow(
    @SerialName("crew_id") val crewId: String,
    val name: String = "",
    @SerialName("image_bg") val imageBg: Int? = null,
    @SerialName("image_ver") val imageVer: Int = 0,
    @SerialName("has_image") val hasImage: Boolean = false,
    @SerialName("owner_id") val ownerId: String? = null,
    val role: String? = null,
    @SerialName("member_count") val memberCount: Int = 0,
    @SerialName("last_rev") val lastRev: Long = 0,
    val notify: Boolean = true,
    @SerialName("notice_count") val noticeCount: Int = 0,
    val pinned: ChatNoticeRow? = null,
    @SerialName("my_read_seq") val myReadSeq: Long = 0,
    val reads: List<ChatReadRow>? = null,
) {
    fun toDomain() = ChatRoomMeta(
        crewId = crewId,
        name = name,
        imageBg = imageBg,
        imageVer = imageVer,
        hasImage = hasImage,
        ownerId = ownerId,
        role = ChatRole.of(role),
        memberCount = memberCount,
        lastRev = lastRev,
        notify = notify,
        noticeCount = noticeCount,
        pinned = pinned?.toDomain(),
        myReadSeq = myReadSeq,
        reads = reads?.map { ChatRead(it.userId, it.seq, it.joinedAt.chatMillis()) },
    )
}

@Serializable
data class ChatSyncRow(
    val room: ChatMetaRow,
    val messages: List<ChatMessageRow> = emptyList(),
    val reset: Boolean = false,
    @SerialName("last_rev") val lastRev: Long = 0,
)

@Serializable
data class ChatRoomRow(
    @SerialName("crew_id") val crewId: String,
    val name: String = "",
    @SerialName("image_bg") val imageBg: Int? = null,
    @SerialName("image_ver") val imageVer: Int = 0,
    @SerialName("has_image") val hasImage: Boolean = false,
    @SerialName("owner_id") val ownerId: String? = null,
    val role: String? = null,
    @SerialName("member_count") val memberCount: Int = 0,
    val last: ChatMessageRow? = null,
    val unread: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
) {
    fun toDomain() = ChatRoomSummary(
        crewId = crewId, name = name, imageBg = imageBg, imageVer = imageVer, hasImage = hasImage, ownerId = ownerId,
        role = ChatRole.of(role), memberCount = memberCount, last = last?.toDomain(), unread = unread,
        createdAt = createdAt.chatMillis(),
    )
}

/** Postgres 시각(끝이 +00:00) — Instant.parse 는 Z 로 끝나는 것만 읽는다 */
internal fun String.chatMillis(): Long =
    runCatching { OffsetDateTime.parse(this).toInstant().toEpochMilli() }.getOrDefault(0L)
