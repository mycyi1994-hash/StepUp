package com.stepup.android

import com.stepup.android.core.InviteLinks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 크루 초대 링크 읽기.
 *
 * 우리 주소의 `/c/<크루 id>` 만 받아야 한다. 아무 주소나 받으면 남의 사이트
 * 링크로 앱 화면을 열 수 있고, id 모양을 안 보면 엉뚱한 글자로 서버를 두드린다.
 */
class InviteLinksTest {

    private val id = "00000000-0000-0000-0000-00000000c0de"

    @Test
    fun `만든 링크를 다시 읽으면 같은 크루다`() {
        assertEquals("https://stepupcrew.com/c/$id", InviteLinks.crewLink(id))
        assertEquals(id, InviteLinks.crewIdOf(InviteLinks.crewLink(id)))
    }

    @Test
    fun `www 와 끝의 빗금은 받아 준다`() {
        assertEquals(id, InviteLinks.crewIdOf("https://www.stepupcrew.com/c/$id/"))
    }

    @Test
    fun `다른 주소 · http · 다른 경로 · 이상한 id 는 받지 않는다`() {
        assertNull(InviteLinks.crewIdOf("https://evil.example/c/$id"))
        assertNull(InviteLinks.crewIdOf("https://stepupcrew.com.evil.example/c/$id"))
        assertNull(InviteLinks.crewIdOf("http://stepupcrew.com/c/$id"))
        assertNull(InviteLinks.crewIdOf("https://stepupcrew.com/p/$id"))
        assertNull(InviteLinks.crewIdOf("https://stepupcrew.com/c/not-a-crew"))
        assertNull(InviteLinks.crewIdOf("https://stepupcrew.com/c/$id/extra"))
        assertNull(InviteLinks.crewIdOf(null))
    }

    @Test
    fun `크루 알림의 앱 안 링크도 읽는다`() {
        assertEquals(id, InviteLinks.crewIdOfPush("crew/$id"))
        assertNull(InviteLinks.crewIdOfPush("post/$id"))
    }

    @Test
    fun `가입 신청 알림은 신청 목록 링크로 읽고 예전 크루 링크도 그대로 읽는다`() {
        assertEquals(id, InviteLinks.crewRequestsIdOfPush("crew/$id/requests"))
        assertNull(InviteLinks.crewRequestsIdOfPush("crew/$id"))
        assertNull(InviteLinks.crewRequestsIdOfPush("crew/$id/other"))
        assertNull(InviteLinks.crewRequestsIdOfPush("crew/not-a-crew/requests"))
        // 신청 목록 링크는 크루 상세 링크로 새지 않고, 예전 링크(crew/<id>)는 그대로 상세
        assertNull(InviteLinks.crewIdOfPush("crew/$id/requests"))
        assertEquals(id, InviteLinks.crewIdOfPush("crew/$id"))
    }

    @Test
    fun `크루 채팅 알림은 방 링크로만 읽는다`() {
        assertEquals(id, InviteLinks.chatIdOfPush("crew-chat/$id"))
        assertNull(InviteLinks.chatIdOfPush("crew/$id"))
        assertNull(InviteLinks.chatIdOfPush("crew-chat/not-a-crew"))
        assertNull(InviteLinks.chatIdOfPush("crew-chat/$id/extra"))
        // 채팅 링크는 크루 상세로 새지 않는다
        assertNull(InviteLinks.crewIdOfPush("crew-chat/$id"))
    }

    @Test
    fun `글 · 번개 · 대기실 알림은 그 자리로 읽는다`() {
        assertEquals(InviteLinks.PushTarget.Post(42L), InviteLinks.pushTargetOf("post/42"))
        assertEquals(InviteLinks.PushTarget.Flash(7L), InviteLinks.pushTargetOf("flash/7"))
        assertEquals(
            InviteLinks.PushTarget.Room(id, 123L),
            InviteLinks.pushTargetOf("room/${id.uppercase()}/123"),
        )
        // 모양이 틀리면 받지 않는다
        assertNull(InviteLinks.pushTargetOf("post/"))
        assertNull(InviteLinks.pushTargetOf("post/abc"))
        assertNull(InviteLinks.pushTargetOf("post/-1"))
        assertNull(InviteLinks.pushTargetOf("post/0"))
        assertNull(InviteLinks.pushTargetOf("post/42/x"))
        assertNull(InviteLinks.pushTargetOf("flash/99999999999999999999"))
        assertNull(InviteLinks.pushTargetOf("room/not-a-crew/1"))
        assertNull(InviteLinks.pushTargetOf("room/$id"))
        assertNull(InviteLinks.pushTargetOf("room/$id/x"))
        assertNull(InviteLinks.pushTargetOf(null))
        // 예전 링크는 예전 길 그대로 — 새 읽기가 가로채지 않고, 새 링크는 크루 상세로 새지 않는다
        assertNull(InviteLinks.pushTargetOf("crew/$id"))
        assertNull(InviteLinks.pushTargetOf("crew-chat/$id"))
        assertNull(InviteLinks.pushTargetOf("crew/$id/requests"))
        assertNull(InviteLinks.crewIdOfPush("room/$id/123"))
        assertNull(InviteLinks.crewIdOfPush("post/42"))
    }

    @Test
    fun `번개 링크는 우리 주소의 f 와 글 번호만 받는다`() {
        assertEquals("https://stepupcrew.com/f/42", InviteLinks.flashLink(42))
        assertEquals(42L, InviteLinks.flashIdOf(InviteLinks.flashLink(42)))
        assertEquals(42L, InviteLinks.flashIdOf("https://www.stepupcrew.com/f/42/"))
        assertNull(InviteLinks.flashIdOf("http://stepupcrew.com/f/42"))
        assertNull(InviteLinks.flashIdOf("https://evil.example/f/42"))
        assertNull(InviteLinks.flashIdOf("https://stepupcrew.com/f/0"))
        assertNull(InviteLinks.flashIdOf("https://stepupcrew.com/f/abc"))
        assertNull(InviteLinks.flashIdOf("https://stepupcrew.com/f/42/x"))
        assertNull(InviteLinks.flashIdOf("https://stepupcrew.com/c/42"))
        assertNull(InviteLinks.flashIdOf(null))
        // 크루 링크 읽기는 번개 링크를 크루로 읽지 않는다
        assertNull(InviteLinks.crewIdOf(InviteLinks.flashLink(42)))
    }

    @Test
    fun `최근 앱 목록에서 다시 연 실행은 알림 링크를 다시 읽지 않는다`() {
        val fromHistory = android.content.Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY or android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        org.junit.Assert.assertTrue(InviteLinks.launchedFromHistory(fromHistory))
        org.junit.Assert.assertFalse(InviteLinks.launchedFromHistory(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    @Test
    fun `알림의 링크 칸은 이 설치의 표가 맞는 우리 인텐트에서만 읽는다`() {
        val token = "ab".repeat(32)
        // 우리 알림(PendingIntent — 동작 없음) · 앱이 뒤에 있을 때 FCM 이 띄운 알림(런처 인텐트)
        org.junit.Assert.assertTrue(InviteLinks.trustsExtras(null, null, token, token))
        org.junit.Assert.assertTrue(
            InviteLinks.trustsExtras(android.content.Intent.ACTION_MAIN, setOf(android.content.Intent.CATEGORY_LAUNCHER), token, token),
        )
        // 표가 없거나 틀리다 — 다른 앱이 실어 보낸 링크
        org.junit.Assert.assertFalse(InviteLinks.trustsExtras(android.content.Intent.ACTION_MAIN, setOf(android.content.Intent.CATEGORY_LAUNCHER), null, token))
        org.junit.Assert.assertFalse(InviteLinks.trustsExtras(null, null, "", token))
        org.junit.Assert.assertFalse(InviteLinks.trustsExtras(null, null, "cd".repeat(32), token))
        org.junit.Assert.assertFalse(InviteLinks.trustsExtras(null, null, token.dropLast(1), token))
        // 이 폰의 표를 못 읽었다 — 아무것도 믿지 않는다
        org.junit.Assert.assertFalse(InviteLinks.trustsExtras(null, null, token, null))
        org.junit.Assert.assertFalse(InviteLinks.trustsExtras(null, null, "", ""))
        // 웹 페이지 · 다른 앱의 링크(intent://… · 보기) — 표가 있어도 링크 칸은 읽지 않는다
        org.junit.Assert.assertFalse(InviteLinks.trustsExtras(android.content.Intent.ACTION_VIEW, null, token, token))
        org.junit.Assert.assertFalse(InviteLinks.trustsExtras(null, setOf(android.content.Intent.CATEGORY_BROWSABLE), token, token))
        org.junit.Assert.assertFalse(
            InviteLinks.trustsExtras(
                android.content.Intent.ACTION_MAIN,
                setOf(android.content.Intent.CATEGORY_LAUNCHER, android.content.Intent.CATEGORY_BROWSABLE),
                token, token,
            ),
        )
    }

    @Test
    fun `대기실 링크는 이 폰이 이미 그 방에 있을 때만 바로 연다 — 아니면 그 크루의 대기실 목록`() {
        val room = InviteLinks.PushTarget.Room(id, 123L)
        org.junit.Assert.assertTrue(InviteLinks.opensRoomDirectly(room, id, 123L))
        org.junit.Assert.assertTrue(InviteLinks.opensRoomDirectly(room, id.uppercase(), 123L))
        // 방에 없다 · 다른 방에 있다 · 같은 방 번호를 다른 크루로 실어 왔다
        org.junit.Assert.assertFalse(InviteLinks.opensRoomDirectly(room, null, null))
        org.junit.Assert.assertFalse(InviteLinks.opensRoomDirectly(room, id, 124L))
        org.junit.Assert.assertFalse(InviteLinks.opensRoomDirectly(room, "00000000-0000-0000-0000-00000000beef", 123L))
        org.junit.Assert.assertFalse(InviteLinks.opensRoomDirectly(room, null, 123L))
    }
}
