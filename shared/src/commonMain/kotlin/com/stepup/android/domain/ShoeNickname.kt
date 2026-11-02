package com.stepup.android.domain

/**
 * 내가 붙인 신발 이름(2026-10-17 사용자 "이름 바꿀 수 있게"). 이 폰에만 적는다(설정 저장소) — 서버 · 거래소 · 다른 사람에게는
 * 원래 모델 이름이 그대로 보인다. 신발 번호는 서버 신발 번호라 서버와 다시 맞춰도 이름이 남는다.
 */
object ShoeNickname {
    /** 이름 길이 상한(글자) — 러닝 탭 두 줄 안에 들어가는 길이 */
    const val MAX_LENGTH = 20

    /** 앞뒤 공백 · 줄바꿈 · 탭을 지우고 상한까지 자른다. 비면 null(원래 이름) */
    fun clean(name: String?): String? {
        val one = name.orEmpty().replace(Regex("[\\r\\n\\t]+"), " ").trim().replace(Regex(" {2,}"), " ")
        if (one.isEmpty()) return null
        return one.takeCodePoints(MAX_LENGTH).trim().ifEmpty { null }
    }

    /** 앞에서 [n] 글자(코드 포인트 — 이모지 같은 두 칸 글자도 하나)까지. 짝 없는 반쪽 글자는 한 글자로 센다(String.codePoints 와 같다) */
    private fun String.takeCodePoints(n: Int): String {
        var end = 0
        var count = 0
        while (end < length && count < n) {
            end += if (this[end].isHighSurrogate() && end + 1 < length && this[end + 1].isLowSurrogate()) 2 else 1
            count++
        }
        return substring(0, end)
    }

    fun encode(rows: Map<Long, String>): String =
        rows.entries.sortedBy { it.key }.joinToString("\n") { "${it.key}\t${it.value}" }

    fun decode(raw: String?): Map<Long, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.lineSequence().mapNotNull { line ->
            val tab = line.indexOf('\t')
            if (tab <= 0) return@mapNotNull null
            val id = line.substring(0, tab).toLongOrNull() ?: return@mapNotNull null
            val name = clean(line.substring(tab + 1)) ?: return@mapNotNull null
            id to name
        }.toMap()
    }
}
