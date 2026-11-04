package com.stepup.android.domain

/**
 * 코스 이름 · 지역 칸의 길이 — 서버 course_share(0012)가 `left(…, 60)` 으로 자르는 것과 같게, 입력 칸에서 먼저 멈춘다.
 * 서버의 60은 글자(코드 포인트) 수라 이모지 하나도 한 글자로 센다 — 반쪽 이모지가 남지 않게 코드 포인트로 자른다.
 */
object CourseText {
    const val MAX = 60

    fun clip(text: String, max: Int = MAX): String {
        if (text.codePointCount(0, text.length) <= max) return text
        return text.substring(0, text.offsetByCodePoints(0, max))
    }
}
