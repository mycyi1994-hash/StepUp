package com.stepup.android.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** 글쓰기 글자 수 — 서버 검사(제목 120 · 장소 80 · 본문 4,000, 코드 포인트)와 같게 막는다(점검 2026-11-17 #22) */
class PostLimitsTest {
    @Test
    fun `한도 안이면 그대로`() {
        assertEquals("한강 번개", PostLimits.cap("한강 번개", PostLimits.TITLE))
    }

    @Test
    fun `한도를 넘으면 한도까지 자른다`() {
        val long = "가".repeat(130)
        assertEquals(PostLimits.TITLE, PostLimits.cap(long, PostLimits.TITLE).length)
        assertEquals(PostLimits.PLACE, PostLimits.cap("a".repeat(81), PostLimits.PLACE).length)
    }

    @Test
    fun `이모지는 한 글자로 세고 반으로 자르지 않는다`() {
        val emoji = "🏃" // 달리는 사람 — 두 칸(서로게이트 쌍)이 한 글자
        val text = emoji.repeat(121)
        val capped = PostLimits.cap(text, PostLimits.TITLE)
        assertEquals(PostLimits.TITLE, capped.codePointCount(0, capped.length))
        assertEquals(emoji.repeat(PostLimits.TITLE), capped)
        // 120 글자는 240 칸이지만 한도 안이다
        assertEquals(emoji.repeat(120), PostLimits.cap(emoji.repeat(120), PostLimits.TITLE))
    }
}
