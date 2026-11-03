package com.stepup.android.domain

import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime

/** 공용 DatePattern 이 JDK DateTimeFormatter(안드로이드 화면이 쓰던 것)와 같은 글자를 내는가 — 네 언어 · 앱이 쓰는 꼴 */
class DatePatternJvmTest {
    private val patterns = listOf(
        "HH:mm", "a hh:mm", "a h:mm", "M/d", "M/d E", "M/d HH:mm", "M.d HH:mm", "M.d (E)", "yyyy.MM.dd", "yyyy.MM.dd HH:mm",
        "yyyy-MM-dd", "MMM d", "MMMM d, EEEE", "M월 d일 EEEE", "yyyy'년' M'월' d'일'", "EEEE", "E", "yy.MM.dd", "h:mm a", "HH:mm:ss",
    )

    @Test fun matchesJdk() {
        val times = listOf(
            LocalDateTime(2026, 1, 5, 0, 7, 9), LocalDateTime(2026, 11, 3, 12, 0, 0), LocalDateTime(2025, 12, 31, 23, 59, 59),
            LocalDateTime(2026, 6, 14, 9, 30, 5), LocalDateTime(2026, 2, 28, 13, 5, 0), LocalDateTime(2024, 9, 1, 11, 59, 0),
        ) + (0 until 7).map { LocalDateTime(2026, 3, 2 + it, 7 + it, 3 * it, 0) }
        for (lang in listOf("ko", "en", "ja", "zh")) {
            val locale = Locale.forLanguageTag(lang)
            for (p in patterns) for (t in times) {
                val jdk = DateTimeFormatter.ofPattern(p, locale).format(t.toJavaLocalDateTime())
                assertEquals(jdk, DatePattern.format(t, p, lang), "$lang · '$p' · $t")
            }
        }
    }
}
