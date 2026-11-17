package com.stepup.android.domain

/**
 * 번개 · 크루 게시판 글쓰기의 글자 수 — 서버 `public.posts` 의 검사(0004: 제목 1 ~ 120 · 본문 4,000 · 장소 80, 글자 수는
 * Postgres `length` = 코드 포인트)와 같다. 넘게 치지 못하게 막아 서버의 영어 오류 문구가 보이지 않게 한다(점검 2026-11-17 #22).
 */
object PostLimits {
    const val TITLE = 120
    const val BODY = 4_000
    const val PLACE = 80

    /** [text] 를 [max] 글자(코드 포인트)까지 — 이모지 같은 두 칸 글자를 반으로 자르지 않는다 */
    fun cap(text: String, max: Int): String {
        if (text.length <= max) return text
        if (text.codePointCount(0, text.length) <= max) return text
        return text.substring(0, text.offsetByCodePoints(0, max))
    }
}
