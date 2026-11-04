package com.stepup.android

import com.stepup.android.data.local.SneakerEntity
import com.stepup.android.data.repo.toDomain
import com.stepup.android.domain.Faction
import com.stepup.android.domain.MarketShoeLook
import com.stepup.android.domain.Rarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 거래소 줄의 신발은 신발 탭과 같은 규칙으로 그린다(2026-11-04) — 속성 "NONE"(0066 뒤 신발)의 레어 이상은 매물마다 도감 모델이 달라
 * 줄 값만으로는 모델을 정하지 않는다(예전엔 불 모델 이름 · 그림을 붙였다).
 */
class MarketShoeLookTest {

    @Test fun `0066 뒤 신발의 레어 이상은 모델이 정해지지 않고, 일반과 예전 속성 신발은 정해진다`() {
        for (r in listOf("RARE", "EPIC", "LEGENDARY", "REDLINE", "FINISH")) {
            assertFalse(r, MarketShoeLook.pinsDesign("NONE", r))
        }
        // 일반은 도감 모델이 없어(서버 draw_model 이 비운다) 신발 탭도 예전 그림 — 줄 값으로 정해진다
        assertTrue(MarketShoeLook.pinsDesign("NONE", "COMMON"))
        assertTrue(MarketShoeLook.pinsDesign("WATER", "EPIC"))
        assertTrue(MarketShoeLook.pinsDesign("FIRE", "LEGENDARY"))
    }

    @Test fun `그리기용 신발은 폰 DB 의 신발을 읽을 때와 같은 속성 · 변형 · 등급`() {
        val preview = MarketShoeLook.preview("NONE", "COMMON", 2, level = 3)
        val owned = SneakerEntity(
            factionId = "NONE", rarity = "COMMON", variant = 2, level = 3, mintNumber = 1,
            luck = 1.0, comfort = 1.0, durability = 100, equipped = false, acquiredAt = 0,
        ).toDomain()
        assertEquals(owned.faction, preview.faction)
        assertEquals(Faction.FIRE, preview.faction)
        assertEquals("NONE", preview.factionId)
        assertEquals(owned.rarity, preview.rarity)
        assertEquals(owned.variant, preview.variant)
        assertEquals(owned.slotKey, preview.slotKey)
        assertEquals(3, preview.level)
        assertNull(preview.modelId)
    }

    @Test fun `등급에 없는 변형 번호는 신발 탭처럼 끝 번호로 맞춘다`() {
        assertEquals(Rarity.LEGENDARY.variantCount - 1, MarketShoeLook.preview("FIRE", "LEGENDARY", 9).variant)
        assertEquals(0, MarketShoeLook.preview("FIRE", "RARE", -1).variant)
    }
}
