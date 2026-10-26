package com.stepup.android

import com.stepup.android.domain.DrawOdds
import com.stepup.android.domain.Rarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 뽑기 확률 표(서버 0077 draw_odds) — 서버 표 그대로 읽고, 못 읽으면 표를 두지 않는다 */
class DrawOddsTest {
    @Test fun `permille to percent text`() {
        assertEquals("79", DrawOdds.percent(790))
        assertEquals("2.5", DrawOdds.percent(25))
        assertEquals("0.5", DrawOdds.percent(5))
        assertEquals("1", DrawOdds.percent(10))
    }

    @Test fun `reads both pools in server order`() {
        val odds = DrawOdds.parse(
            """{"pools":[{"pool":"FREE","odds":[{"rarity":"COMMON","permille":790},{"rarity":"RARE","permille":200},{"rarity":"EPIC","permille":10}]},""" +
                """{"pool":"PREMIUM","odds":[{"rarity":"RARE","permille":600},{"rarity":"FINISH","permille":5}]}]}""",
        )!!
        assertEquals(listOf(Rarity.COMMON to 790, Rarity.RARE to 200, Rarity.EPIC to 10), odds.free)
        assertEquals(listOf(Rarity.RARE to 600, Rarity.FINISH to 5), odds.premium)
    }

    @Test fun `nothing readable means no table`() {
        assertNull(DrawOdds.parse("""{"pools":[]}"""))
        assertNull(DrawOdds.parse("not json"))
    }
}
