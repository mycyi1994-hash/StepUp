package com.stepup.android

import com.stepup.android.domain.ShoeNickname
import com.stepup.android.ui.components.ceilingFlashOf
import com.stepup.android.ui.components.ceilingFlicker
import com.stepup.android.ui.components.ceilingSweep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 신발 이름 바꾸기(2026-10-17) — 이 폰의 설정 저장소에 적는 형식 · 다듬기 */
class ShoeNicknameTest {

    @Test fun `빈 이름 · 공백만은 원래 이름(null), 줄바꿈 · 탭 · 겹친 공백은 한 칸으로`() {
        assertNull(ShoeNickname.clean(null))
        assertNull(ShoeNickname.clean("   \n\t "))
        assertEquals("새벽 러너", ShoeNickname.clean("  새벽\n\t  러너  "))
    }

    @Test fun `20자까지 — 이모지는 한 글자로 센다`() {
        assertEquals("가".repeat(20), ShoeNickname.clean("가".repeat(25)))
        val emoji = "🏃".repeat(21)
        val cut = requireNotNull(ShoeNickname.clean(emoji))
        assertEquals(20, cut.codePointCount(0, cut.length))
    }

    @Test fun `저장 형식 — 번호 탭 이름 줄, 깨진 줄은 버린다`() {
        val rows = mapOf(7L to "내 레이서", 1_000_003L to "Night Run")
        assertEquals(rows, ShoeNickname.decode(ShoeNickname.encode(rows)))
        assertEquals(mapOf(5L to "좋은 신발"), ShoeNickname.decode("x\tbad\n5\t좋은 신발\n\t빈 번호\n9\t   "))
        assertEquals(emptyMap<Long, String>(), ShoeNickname.decode(null))
    }

    @Test fun `천장 조명 — 쉼(0)에는 그대로, 앞쪽 두 번 깜빡이고 뒤에 막대를 훑는다`() {
        assertEquals(0f, ceilingFlicker(0f), 0f)
        assertEquals(0f, ceilingFlicker(1f), 0f)
        assertEquals(1f, ceilingFlicker(0.05f), 0.0001f)
        assertEquals(0f, ceilingFlicker(0.13f), 0f)
        assertTrue(ceilingFlicker(0.23f) > 0.5f)
        assertEquals(0f to 0f, ceilingSweep(0.2f))
        val (at, power) = ceilingSweep(0.65f)
        assertEquals(0.5f, at, 0.0001f)
        assertEquals(1f, power, 0.0001f)
        assertEquals(0f to 0f, ceilingSweep(1f))
        // 6초 바퀴의 끝 1.4초만 번쩍인다 — 처음 들어왔을 때는 쉼
        assertEquals(0f, ceilingFlashOf(0f), 0f)
        assertEquals(0f, ceilingFlashOf(4_500f), 0f)
        assertEquals(0.5f, ceilingFlashOf(5_300f), 0.0001f)
        assertEquals(1f, ceilingFlashOf(6_000f), 0f)
    }
}
