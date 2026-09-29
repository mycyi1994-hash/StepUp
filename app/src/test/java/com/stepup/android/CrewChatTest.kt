package com.stepup.android

import com.stepup.android.data.remote.ChatMessageRow
import com.stepup.android.data.remote.ChatRoomRow
import com.stepup.android.data.remote.ChatSyncRow
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.serverJson
import com.stepup.android.data.repo.ChatOutcome
import com.stepup.android.data.repo.chat
import com.stepup.android.domain.ChatDelivery
import com.stepup.android.domain.ChatEvent
import com.stepup.android.domain.ChatItem
import com.stepup.android.domain.ChatKind
import com.stepup.android.domain.ChatMessage
import com.stepup.android.domain.ChatPreview
import com.stepup.android.domain.ChatRead
import com.stepup.android.domain.ChatRole
import com.stepup.android.domain.ChatRoomMeta
import com.stepup.android.domain.ChatRules
import com.stepup.android.domain.ChatState
import com.stepup.android.ui.AppChromePolicy
import com.stepup.android.ui.screens.community.chat.ChatRoutes
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 크루 채팅(2026-09-29) — 서버 메시지와 이 폰의 내 메시지를 합치는 규칙, 미확인 수, 줄 묶음, 미리보기, 서버 답 읽기.
 */
class CrewChatTest {
    private val me = "u-me"
    private val zone = ZoneOffset.UTC

    private fun at(minute: Int, day: Int = 28): Long = LocalDate.of(2026, 9, day).atStartOfDay(zone).toInstant().toEpochMilli() + 19 * 3_600_000L + minute * 60_000L

    private fun server(id: Long, author: String, body: String = "m$id", minute: Int = id.toInt(), client: String? = null, rev: Long = id, day: Int = 28) = ChatMessage(
        id = id, seq = id, rev = rev, authorId = author, authorName = author.removePrefix("u-"), kind = ChatKind.TEXT, body = body,
        clientId = client, canDelete = author == me, createdAt = at(minute, day),
    )

    private fun local(client: String, body: String, delivery: ChatDelivery = ChatDelivery.SENDING) = ChatMessage(
        id = null, seq = null, authorId = me, authorName = null, kind = ChatKind.TEXT, body = body, clientId = client,
        createdAt = at(30), delivery = delivery,
    )

    private fun meta(owner: String = "u-owner", reads: List<ChatRead>? = null) = ChatRoomMeta(
        crewId = "c", name = "퇴근런", imageBg = 1, imageVer = 0, hasImage = false, ownerId = owner, role = if (owner == me) ChatRole.OWNER else ChatRole.MEMBER,
        memberCount = 25, lastRev = 0, notify = true, noticeCount = 0, pinned = null, myReadSeq = 0, reads = reads,
    )

    @Test
    fun `서버가 같은 요청 키의 메시지를 돌려주면 이 폰의 것은 빠진다 — 두 개로 보이지 않는다`() {
        val current = listOf(server(1, "u-a"), local("k1", "안녕"), local("k2", "또", ChatDelivery.FAILED))
        val merged = ChatRules.merge(current, listOf(server(2, me, "안녕", client = "k1")))
        assertEquals(listOf(1L, 2L, null), merged.map { it.id })
        assertEquals(1, merged.count { it.body == "안녕" })
        // 실패한 것은 이 폰에 남는다(맨 아래)
        assertEquals("k2", merged.last().clientId)
    }

    @Test
    fun `같은 메시지는 더 늦게 바뀐 것으로 · 방 순서대로`() {
        val current = listOf(server(3, "u-a"), server(1, "u-b"))
        val deleted = server(1, "u-b", rev = 9).copy(state = ChatState.DELETED, body = null)
        val merged = ChatRules.merge(current, listOf(deleted, server(2, "u-c")))
        assertEquals(listOf(1L, 2L, 3L), merged.map { it.seq })
        assertEquals(ChatState.DELETED, merged.first().state)
        // 늦게 온 옛 값이 새 값을 덮지 않는다
        val stale = ChatRules.merge(merged, listOf(server(1, "u-b", rev = 1)))
        assertEquals(ChatState.DELETED, stale.first().state)
    }

    @Test
    fun `처음부터 다시 받으면 서버 것은 새로, 이 폰의 내 메시지는 남긴다`() {
        val current = listOf(server(1, "u-a"), local("k1", "보내는 중"))
        val replaced = ChatRules.replace(current, listOf(server(5, "u-b")))
        assertEquals(listOf(5L, null), replaced.map { it.id })
    }

    @Test
    fun `내 메시지 옆 미확인 수 — 나를 빼고 그 메시지보다 앞까지만 읽은 사람, 뒤에 가입한 사람은 빼고`() {
        val message = server(10, me, minute = 10)
        val reads = listOf(
            ChatRead(me, 3, 0),
            ChatRead("u-a", 9, 0),
            ChatRead("u-b", 10, 0),
            ChatRead("u-c", 2, 0),
            ChatRead("u-late", 0, at(20)),
        )
        assertEquals(2, ChatRules.unread(message, meta(reads = reads), me))
        // 읽은 위치를 서버가 주지 않으면 숫자를 만들지 않는다 · 모두 읽었으면 보이지 않는다
        assertNull(ChatRules.unread(message, meta(reads = null), me))
        assertNull(ChatRules.unread(message, meta(reads = listOf(ChatRead("u-a", 10, 0))), me))
        // 남의 메시지 · 보내는 중에는 없다
        assertNull(ChatRules.unread(server(10, "u-a"), meta(reads = reads), me))
        assertNull(ChatRules.unread(message.copy(delivery = ChatDelivery.SENDING), meta(reads = reads), me))
    }

    @Test
    fun `좌우는 작성자와 지금 사용자로 — 크루장이라고 모두 오른쪽이 아니다`() {
        val messages = listOf(server(1, "u-owner"), server(2, "u-a"), server(3, me))
        val asMember = ChatRules.timeline(messages, meta(owner = "u-owner"), me, zone).filterIsInstance<ChatItem.Bubble>()
        assertEquals(listOf(false, false, true), asMember.map { it.mine })
        assertEquals(listOf(true, false, false), asMember.map { it.owner })
        // 크루장 시점 — 크루장 메시지만 오른쪽이고 "나 크루장"이 붙는다
        val asOwner = ChatRules.timeline(messages, meta(owner = "u-owner"), "u-owner", zone).filterIsInstance<ChatItem.Bubble>()
        assertEquals(listOf(true, false, false), asOwner.map { it.mine })
        assertTrue(asOwner.first().header)
        // 크루원인 내 메시지는 이름 줄이 없다
        assertFalse(asMember.last().header)
    }

    @Test
    fun `같은 사람이 이어 보내면 이름은 한 번, 같은 분의 시간은 마지막에만 · 날짜가 바뀌면 날짜 줄`() {
        val messages = listOf(
            server(1, "u-a", minute = 1), server(2, "u-a", minute = 1), server(3, "u-a", minute = 2),
            server(4, "u-a", minute = 1, day = 29),
        )
        val items = ChatRules.timeline(messages, meta(), me, zone)
        assertEquals(2, items.count { it is ChatItem.Day })
        val bubbles = items.filterIsInstance<ChatItem.Bubble>()
        assertEquals(listOf(true, false, false, true), bubbles.map { it.header })
        assertEquals(listOf(false, true, true, true), bubbles.map { it.time })
    }

    @Test
    fun `알림 줄은 가운데 한 줄이고 이름 묶음을 끊는다`() {
        val line = ChatMessage(
            id = 2, seq = 2, authorId = null, authorName = null, kind = ChatKind.SYSTEM, body = null,
            event = ChatEvent.NOTICE_CREATED, eventName = "준호", createdAt = at(1),
        )
        val items = ChatRules.timeline(listOf(server(1, "u-a"), line, server(3, "u-a")), meta(), me, zone)
        assertTrue(items.any { it is ChatItem.Line })
        assertEquals(listOf(true, true), items.filterIsInstance<ChatItem.Bubble>().map { it.header })
    }

    @Test
    fun `목록 미리보기 — 지웠거나 숨긴 메시지는 내용 없이 상태로`() {
        assertEquals(ChatPreview.Empty, ChatRules.preview(null))
        assertEquals(ChatPreview.Text("a", "줄 바꿈 있는 글"), ChatRules.preview(server(1, "u-a", "줄 바꿈\n있는   글")))
        assertEquals(ChatPreview.Deleted, ChatRules.preview(server(1, "u-a").copy(state = ChatState.DELETED, body = null)))
        assertEquals(ChatPreview.Hidden, ChatRules.preview(server(1, "u-a").copy(state = ChatState.HIDDEN, body = null)))
        assertEquals(ChatPreview.Photo("a"), ChatRules.preview(server(1, "u-a", "").copy(kind = ChatKind.IMAGE)))
        val left = server(1, "u-a").copy(kind = ChatKind.SYSTEM, event = ChatEvent.MEMBER_LEFT, eventName = "민수", body = null)
        assertEquals(ChatPreview.Left("민수"), ChatRules.preview(left))
    }

    @Test
    fun `공백만 있으면 보낼 수 없고, 공지는 제목이 있어야 올린다`() {
        assertFalse(ChatRules.sendable("   \n  "))
        assertTrue(ChatRules.sendable(" 안녕 "))
        assertFalse(ChatRules.sendable("가".repeat(ChatRules.BODY_MAX + 1)))
        assertFalse(ChatRules.noticeReady(" ", "내용"))
        assertTrue(ChatRules.noticeReady("제목", ""))
        assertEquals("c:new", ChatRules.noticeDraftKey("c", null))
        assertEquals("c:edit:7", ChatRules.noticeDraftKey("c", 7))
    }

    @Test
    fun `서버 답 읽기 — 따라오기 · 방 목록 · 답장 원문 · 보낸 사람에게만 요청 키`() {
        val sync = serverJson.decodeFromString<ChatSyncRow>(
            """
            {"room":{"crew_id":"c","name":"퇴근런","image_bg":1,"image_ver":2,"has_image":true,"owner_id":"u-o","role":"OWNER",
              "member_count":25,"last_rev":104,"notify":false,"notice_count":2,
              "pinned":{"id":61,"crew_id":"c","title":"오늘 19:30","body":"본문","pinned":true,"author_id":"u-o","author_name":"준호",
                "created_at":"2026-09-28T18:30:00+09:00","updated_at":"2026-09-28T18:30:00+09:00"},
              "my_read_seq":101,"reads":[{"user_id":"u-a","seq":100,"joined_at":"2026-08-01T01:00:00+00:00"}]},
             "messages":[{"id":104,"seq":104,"rev":110,"author_id":"u-me","author_name":"도윤","kind":"TEXT","state":"VISIBLE",
               "body":"네","has_image":false,"event":null,"event_name":null,
               "reply":{"id":101,"seq":101,"author_id":"u-o","author_name":"준호","kind":"TEXT","state":"HIDDEN","body":null},
               "client_id":"k1","can_delete":true,"created_at":"2026-09-28T19:12:00+09:00"}],
             "reset":false,"last_rev":110}
            """.trimIndent(),
        )
        val meta = sync.room.toDomain()
        assertTrue(meta.owner)
        assertFalse(meta.notify)
        assertEquals("오늘 19:30", meta.pinned?.title)
        assertEquals(1, meta.reads?.size)
        val message = sync.messages.single().toDomain()
        assertEquals("k1", message.clientId)
        assertEquals(ChatState.HIDDEN, message.reply?.state)
        assertNull(message.reply?.body)
        assertTrue(message.createdAt > 0)
        assertEquals(110L, sync.lastRev)

        val rooms = serverJson.decodeFromString<List<ChatRoomRow>>(
            """[{"crew_id":"c","name":"퇴근런","member_count":1,"role":"OWNER","last":null,"unread":0,"created_at":"2026-09-28T01:00:00+00:00"}]""",
        ).map { it.toDomain() }
        assertTrue(rooms.single().solo)
        val system = serverJson.decodeFromString<ChatMessageRow>(
            """{"id":5,"seq":5,"rev":5,"kind":"SYSTEM","state":"VISIBLE","event":"MEMBER_LEFT","event_name":"민수","created_at":"2026-09-28T19:00:00Z"}""",
        ).toDomain()
        assertEquals(ChatEvent.MEMBER_LEFT, system.event)
        assertNull(system.clientId)
    }

    @Test
    fun `서버 거절을 채팅의 결말로 — 멤버가 아니면 끝, 크루장 권한만 없으면 크루원으로`() {
        assertEquals(ChatOutcome.Ended, ServerResult.Rejected("chat_not_member").chat())
        assertEquals(ChatOutcome.NotOwner, ServerResult.Rejected("크루장만 할 수 있습니다").chat())
        assertTrue(ServerResult.Rejected("message_gone").chat() is ChatOutcome.Rejected)
        assertTrue(ServerResult.Retry("503").chat() is ChatOutcome.Offline)
        assertEquals(ChatOutcome.Ok(3L), ServerResult.Ok(3L).chat())
    }

    @Test
    fun `채팅 화면은 하단 탭 없이 커뮤니티에 속한다`() {
        // 크루원 길은 android.net.Uri 로 만들어(JVM 검사에서는 없다) 모양 그대로 적는다
        for (route in listOf(ChatRoutes.room("c1"), ChatRoutes.info("c1"), "chat/room/c1/member/u-a", ChatRoutes.noticeEdit("c1", 3, true), ChatRoutes.ENDED)) {
            val destination = AppChromePolicy.destination(route)
            assertEquals(route, com.stepup.android.ui.Screen.Community, destination?.parent)
            assertFalse(route, destination!!.showBottomBar)
        }
    }
}
