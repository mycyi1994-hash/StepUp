package com.stepup.android

import com.stepup.android.data.local.SneakerEntity
import com.stepup.android.data.repo.toDomain
import com.stepup.android.data.repo.toEntity
import com.stepup.android.domain.Faction
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.Sneaker
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 0066 부터 서버 신발의 속성은 "NONE" 이다. 그림 · 색은 FIRE 로 기울어도, 거래소 모델(속성 · 등급 · 변형)을 찾는
 * 값은 적힌 글자 그대로여야 "판매" 에서 내 신발이 잡힌다.
 */
class SneakerFactionIdTest {

    private fun entity(faction: String) = SneakerEntity(
        id = 7, factionId = faction, rarity = "RARE", variant = 1, level = 3, mintNumber = 1,
        luck = 1.0, comfort = 1.0, durability = 100, equipped = false, acquiredAt = 0,
    )

    @Test
    fun `NONE 신발은 속성 글자를 그대로 들고 다닌다`() {
        val shoe = entity("NONE").toDomain()
        assertEquals(Faction.FIRE, shoe.faction)
        assertEquals("NONE", shoe.factionId)
        assertEquals("NONE", shoe.toEntity().factionId)
        assertEquals("NONE", shoe.copy(level = 4).factionId)
    }

    @Test
    fun `예전 속성 신발과 폰에서 만든 신발은 속성 id 와 같다`() {
        assertEquals("WATER", entity("WATER").toDomain().factionId)
        val made = Sneaker(
            id = 1, faction = Faction.WIND, rarity = Rarity.COMMON, variant = 0, level = 1, mintNumber = 1,
            luck = 1.0, comfort = 1.0, durability = 100, equipped = false, acquiredAt = 0,
        )
        assertEquals("WIND", made.factionId)
    }
}
