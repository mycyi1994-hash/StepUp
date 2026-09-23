package com.stepup.android

import com.stepup.android.core.SafeUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 바깥으로 나가는 주소 검사.
 *
 * 이 검사가 무너지면 카드 하나를 누른 사용자가 위험해진다. 서버에서도 같은
 * 규칙을 걸지만(0009_running_feed.sql), 주소가 들어오는 길은 하나가 아니다.
 */
class SafeUrlTest {

    @Test
    fun `http와 https만 연다`() {
        assertTrue(SafeUrl.looksSafe("https://seoul-marathon.com/main"))
        assertTrue(SafeUrl.looksSafe("http://example.co.kr/a?b=1#c"))
        assertFalse(SafeUrl.looksSafe("ftp://example.com/a"))
    }

    @Test
    fun `위험한 스킴은 막는다`() {
        for (bad in listOf(
            "javascript:alert(1)",
            "JavaScript:alert(1)",
            "data:text/html,<script>x</script>",
            "file:///etc/passwd",
            "intent://scan#Intent;scheme=zxing;end",
            "market://details?id=x",
            "content://com.example/x",
        )) {
            assertFalse("막혔어야 한다: $bad", SafeUrl.looksSafe(bad))
        }
    }

    @Test
    fun `빈 값과 공백이 섞인 주소는 막는다`() {
        assertFalse(SafeUrl.looksSafe(null))
        assertFalse(SafeUrl.looksSafe(""))
        assertFalse(SafeUrl.looksSafe("   "))
        // 스킴 앞에 공백·개행을 끼워 검사를 피하는 수법
        assertFalse(SafeUrl.looksSafe(" javascript:alert(1)"))
        assertFalse(SafeUrl.looksSafe("java\nscript:alert(1)"))
        assertFalse(SafeUrl.looksSafe("https://exa mple.com"))
    }

    @Test
    fun `호스트가 없으면 막는다`() {
        assertFalse(SafeUrl.looksSafe("https://"))
        assertFalse(SafeUrl.looksSafe("https:///path"))
        // 점이 없는 이름은 바깥 주소가 아니다
        assertFalse(SafeUrl.looksSafe("https://localhost/a"))
    }

    @Test
    fun `https를 앞세운다`() {
        assertEquals("https://example.com/a", SafeUrl.preferHttps("http://example.com/a"))
        assertEquals("https://example.com/a", SafeUrl.preferHttps("https://example.com/a"))
    }
}
