package com.stepup.android.domain

/*
 * 글자 세기 — java.lang.String 의 코드 포인트 함수 대신(2026-11-03 KMP 2단계). 서버 char_length 처럼 글자(코드 포인트)로 센다.
 * 짝 없는 반쪽 글자(서로게이트)는 한 글자로 센다 — String.codePointCount 와 같다.
 */

/** String.codePointCount(0, length) */
fun String.codePointLength(): Int {
    var i = 0
    var n = 0
    while (i < length) { i += codePointWidthAt(i); n++ }
    return n
}

/** String.offsetByCodePoints(0, count) — 글자 [count] 개 뒤의 char 위치. 글자가 모자라면 IndexOutOfBoundsException */
fun String.offsetOfCodePoints(count: Int): Int {
    var i = 0
    repeat(count) {
        if (i >= length) throw IndexOutOfBoundsException("글자가 $count 개보다 적다")
        i += codePointWidthAt(i)
    }
    return i
}

/** 첫 글자(이모지 같은 두 칸 글자도 통째로) — String(Character.toChars(codePointAt(0))) */
fun String.firstCodePoint(): String = substring(0, codePointWidthAt(0))

private fun String.codePointWidthAt(i: Int): Int =
    if (this[i].isHighSurrogate() && i + 1 < length && this[i + 1].isLowSurrogate()) 2 else 1

/**
 * 한글 · 한자 · 히라가나 · 가타카나 글자인가 — Character.UnicodeScript 가 HANGUL · HAN · HIRAGANA · KATAKANA 인 BMP 글자.
 * JVM 검사(PlatformJvmTest)가 0 ~ FFFF 전부를 Character.UnicodeScript 와 맞대어 본다.
 */
fun isCjkNameLetter(c: Char): Boolean = CJK_RANGES.any { c.code in it }

private val CJK_RANGES = listOf(
    // HANGUL
    0x1100..0x11FF, 0x302E..0x302F, 0x3131..0x318E, 0x3200..0x321E, 0x3260..0x327E, 0xA960..0xA97C, 0xAC00..0xD7A3,
    0xD7B0..0xD7C6, 0xD7CB..0xD7FB, 0xFFA0..0xFFBE, 0xFFC2..0xFFC7, 0xFFCA..0xFFCF, 0xFFD2..0xFFD7, 0xFFDA..0xFFDC,
    // HAN
    0x2E80..0x2E99, 0x2E9B..0x2EF3, 0x2F00..0x2FD5, 0x3005..0x3005, 0x3007..0x3007, 0x3021..0x3029, 0x3038..0x303B,
    0x3400..0x4DBF, 0x4E00..0x9FFF, 0xF900..0xFA6D, 0xFA70..0xFAD9,
    // HIRAGANA
    0x3041..0x3096, 0x309D..0x309F,
    // KATAKANA
    0x30A1..0x30FA, 0x30FD..0x30FF, 0x31F0..0x31FF, 0x32D0..0x32FE, 0x3300..0x3357, 0xFF66..0xFF6F, 0xFF71..0xFF9D,
)
