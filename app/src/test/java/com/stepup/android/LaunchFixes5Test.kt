package com.stepup.android

import com.stepup.android.core.ShownLanguage
import com.stepup.android.domain.CourseText
import com.stepup.android.domain.WalletAddress
import com.stepup.android.ui.screens.events.KstDay
import com.stepup.android.ui.screens.settings.LegalLinks
import com.stepup.android.ui.screens.settings.faqEntries
import com.stepup.android.ui.screens.setup.BodySetup
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 출시 전 점검 5차(2026-11-04) — 설정 · 알림 언어 · 지갑 주소 · 코스 이름 · 이벤트 하루의 순수 규칙 */
class LaunchFixes5Test {

    // ── 화면 언어(푸시 글 · 약관 #en) ─────────────────────────────

    @Test
    fun `고른 언어가 있으면 그 언어`() {
        assertEquals("ja", ShownLanguage.of("ja", "ko"))
        assertEquals("zh", ShownLanguage.of("zh-TW", "en"))
        assertEquals("ko", ShownLanguage.of("ko-KR", "fr"))
    }

    @Test
    fun `고르지 않았으면 기기 언어`() {
        assertEquals("ko", ShownLanguage.of("", "ko"))
        assertEquals("zh", ShownLanguage.of("", "zh_CN"))
        assertEquals("en", ShownLanguage.of("", "en-US"))
    }

    @Test
    fun `앱에 없는 언어는 영어 — 서버가 한국어로 보내지 않게`() {
        assertEquals("en", ShownLanguage.of("", "fr"))
        assertEquals("en", ShownLanguage.of("", ""))
        // 고른 값이 앱에 없는 언어면 기기 언어로
        assertEquals("ko", ShownLanguage.of("de", "ko"))
    }

    // ── 약관 및 정책 ────────────────────────────────────────────

    @Test
    fun `한국어 화면은 한국어 본문, 그 밖은 영어 본문`() {
        assertEquals("https://stepupcrew.com/terms.html", LegalLinks.terms("ko"))
        assertEquals("https://stepupcrew.com/privacy.html", LegalLinks.privacy("ko"))
        for (lang in listOf("en", "ja", "zh")) {
            assertEquals("https://stepupcrew.com/terms.html#en", LegalLinks.terms(lang))
            assertEquals("https://stepupcrew.com/privacy.html#en", LegalLinks.privacy(lang))
        }
    }

    // ── 첫 설정(몸 정보 · 모드) ─────────────────────────────────

    @Test
    fun `스토어 판에서 다이어트가 잠겨 있으면 몸 정보를 묻지 않는다`() {
        assertFalse(BodySetup.asked(store = true, dietLocked = true))
        assertTrue(BodySetup.asked(store = true, dietLocked = false))
        assertTrue(BodySetup.asked(store = false, dietLocked = true))
        assertTrue(BodySetup.asked(store = false, dietLocked = false))
    }

    // ── 도움말의 뽑기 답 ─────────────────────────────────────────

    @Test
    fun `스토어 판 뽑기 답에는 지갑 상급 뽑기가 없다`() {
        val store = faqEntries(web3 = false).toMap()
        val web3 = faqEntries(web3 = true).toMap()
        assertEquals(R.string.set_faq_a_draw_store, store[R.string.set_faq_q_draw])
        assertEquals(R.string.set_faq_a_draw, web3[R.string.set_faq_q_draw])
        assertFalse(store.containsValue(R.string.set_faq_a_draw))
    }

    // ── 지갑 주소 ───────────────────────────────────────────────

    @Test
    fun `지갑 주소는 앞 6자 뒤 4자로 짧게`() {
        val address = "0x1234567890abcdef1234567890abcdef12345678"
        assertEquals("0x1234…5678", WalletAddress.short(address))
        assertEquals("0x1234…5678", WalletAddress.short("  $address "))
        // 이미 짧으면 그대로
        assertEquals("0x12", WalletAddress.short("0x12"))
        assertEquals("", WalletAddress.short(""))
    }

    @Test
    fun `주소 꼴이 아니면 보이지 않는다`() {
        assertTrue(WalletAddress.looksValid("0x1234567890abcdef1234567890ABCDEF12345678"))
        assertFalse(WalletAddress.looksValid(null))
        assertFalse(WalletAddress.looksValid(""))
        assertFalse(WalletAddress.looksValid("0x1234"))
        assertFalse(WalletAddress.looksValid("1234567890abcdef1234567890abcdef1234567890"))
        assertFalse(WalletAddress.looksValid("0xZZ34567890abcdef1234567890abcdef12345678"))
    }

    // ── 코스 이름 길이(서버 60자) ────────────────────────────────

    @Test
    fun `코스 이름은 60자에서 멈춘다`() {
        val long = "가".repeat(70)
        assertEquals(60, CourseText.clip(long).length)
        assertEquals("한강 코스", CourseText.clip("한강 코스"))
        assertEquals("a".repeat(60), CourseText.clip("a".repeat(60)))
    }

    @Test
    fun `이모지를 반쪽으로 자르지 않는다`() {
        val runner = "🏃" // 🏃 — 두 UTF-16 칸, 한 글자
        val text = "a".repeat(59) + runner + "b"
        val clipped = CourseText.clip(text)
        assertEquals("a".repeat(59) + runner, clipped)
        assertEquals(60, clipped.codePointCount(0, clipped.length))
    }

    // ── 이벤트의 오늘(한국 시각) ─────────────────────────────────

    @Test
    fun `한국 자정에 하루가 바뀐다`() {
        val kst = ZoneId.of("Asia/Seoul")
        val nov4 = ZonedDateTime.of(2026, 11, 4, 0, 0, 0, 0, kst).toInstant().toEpochMilli()
        val nov5 = ZonedDateTime.of(2026, 11, 5, 0, 0, 0, 0, kst).toInstant().toEpochMilli()
        // 한국 11월 4일 23:59:59 = UTC 14:59:59
        assertEquals(nov4, KstDay.start(Instant.parse("2026-11-04T14:59:59Z")))
        // 한국 11월 5일 0:00 = UTC 15:00
        assertEquals(nov5, KstDay.start(Instant.parse("2026-11-04T15:00:00Z")))
        // 기기 시간대와 상관없다 — UTC 로 같은 날 아침도 한국은 이미 그날
        assertEquals(nov4, KstDay.start(Instant.parse("2026-11-03T15:00:00Z")))
    }
}
