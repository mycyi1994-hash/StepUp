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
}
