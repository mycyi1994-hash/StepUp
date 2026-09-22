package com.stepup.android

import com.stepup.android.domain.Faction
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.SneakerDesigns
import com.stepup.android.domain.TOTAL_COLLECTION
import com.stepup.android.domain.VARIANTS_PER_FACTION
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 도감 표가 시트와 어긋나지 않는지 본다.
 *
 * 값을 손으로 옮겨 적은 표라 오타 하나가 조용히 들어가기 쉽다. 그리고 그
 * 오타는 화면에서 "적립 +1.5%" 대신 "+1.05%"처럼 보일 뿐이라 눈으로는
 * 알아채기 어렵다. 번호·개수·차례만 확인해도 대부분 잡힌다.
 */
class SneakerDesignsTest {

    @Test
    fun `속성마다 13종, 모두 52종이다`() {
        assertEquals(13, VARIANTS_PER_FACTION)
        assertEquals(52, TOTAL_COLLECTION)
        assertEquals(52, SneakerDesigns.all.size)
        for (faction in Faction.entries) {
            assertEquals(13, SneakerDesigns.all.count { it.faction == faction })
        }
    }

    @Test
    fun `등급별 칸 수는 레전더리 2 희귀 3 레어 4 일반 4 다`() {
        assertEquals(2, Rarity.LEGENDARY.variantCount)
        assertEquals(3, Rarity.EPIC.variantCount)
        assertEquals(4, Rarity.RARE.variantCount)
        assertEquals(4, Rarity.COMMON.variantCount)
    }

    @Test
    fun `번호와 등급·변형은 서로 되돌릴 수 있다`() {
        for (index in 1..13) {
            val (rarity, variant) = SneakerDesigns.slotOf(index)
            assertEquals(index, SneakerDesigns.indexOf(rarity, variant))
            assertTrue("변형 $variant 는 ${rarity.id} 범위 밖", variant < rarity.variantCount)
        }
    }

    @Test
    fun `도감 번호는 52개가 전부 다르다`() {
        assertEquals(52, SneakerDesigns.all.map { it.code }.toSet().size)
        assertEquals("FIR-001", SneakerDesigns.of(Faction.FIRE, Rarity.LEGENDARY, 0).code)
        assertEquals("WND-013", SneakerDesigns.of(Faction.WIND, Rarity.COMMON, 3).code)
    }

    @Test
    fun `시트에 적힌 값이 그대로 들어 있다`() {
        val inferno = SneakerDesigns.of(Faction.FIRE, Rarity.LEGENDARY, 0)
        assertEquals("INFERNO CROWN", inferno.englishName)
        assertEquals(2.55, inferno.boostPercent, 1e-9)
        assertEquals(1.36, inferno.luck, 1e-9)

        val tideLite = SneakerDesigns.of(Faction.WATER, Rarity.COMMON, 3)
        assertEquals("TIDE LITE", tideLite.englishName)
        assertEquals(0.20, tideLite.boostPercent, 1e-9)
        assertEquals(1.03, tideLite.luck, 1e-9)

        val zenith = SneakerDesigns.of(Faction.LIGHTNING, Rarity.LEGENDARY, 0)
        assertEquals(2.50, zenith.boostPercent, 1e-9)

        val windRunner = SneakerDesigns.of(Faction.WIND, Rarity.RARE, 0)
        assertEquals("WIND RUNNER", windRunner.englishName)
        assertEquals(0.90, windRunner.boostPercent, 1e-9)
    }

    @Test
    fun `번호가 커질수록 적립과 행운은 낮아진다`() {
        // 1번이 제일 좋고 13번이 제일 약하다. 이 차례가 깨지면 도감을 보고
        // 값을 가늠할 수 없게 된다.
        for (faction in Faction.entries) {
            val ordered = SneakerDesigns.all.filter { it.faction == faction }.sortedBy { it.index }
            ordered.zipWithNext { a, b ->
                assertTrue(
                    "${a.code} +${a.boostPercent}% 뒤에 ${b.code} +${b.boostPercent}% 가 왔다",
                    a.boostPercent > b.boostPercent,
                )
                assertTrue("${a.code} 행운 ${a.luck} 뒤에 ${b.code} ${b.luck}", a.luck > b.luck)
            }
        }
    }

    @Test
    fun `등급이 높은 칸이 더 앞 번호를 받는다`() {
        for (faction in Faction.entries) {
            val worstLegendary = SneakerDesigns.of(faction, Rarity.LEGENDARY, 1)
            val bestEpic = SneakerDesigns.of(faction, Rarity.EPIC, 0)
            val bestRare = SneakerDesigns.of(faction, Rarity.RARE, 0)
            val bestCommon = SneakerDesigns.of(faction, Rarity.COMMON, 0)
            assertTrue(worstLegendary.boostPercent > bestEpic.boostPercent)
            assertTrue(bestEpic.boostPercent > bestRare.boostPercent)
            assertTrue(bestRare.boostPercent > bestCommon.boostPercent)
        }
    }

    @Test
    fun `범위를 벗어난 변형도 빈 칸을 주지 않는다`() {
        // 옛 저장값이 범위 밖일 수 있다. 그때 화면이 비면 신발이 사라진 것처럼 보인다.
        val design = SneakerDesigns.of(Faction.FIRE, Rarity.LEGENDARY, 9)
        assertEquals(Rarity.LEGENDARY, design.rarity)
        assertEquals("FIR-002", design.code)
    }
}
