package com.stepup.android

import com.stepup.android.data.local.SneakerEntity
import com.stepup.android.data.remote.ServerSneakerRow
import com.stepup.android.data.remote.serverJson
import com.stepup.android.data.repo.toDomain
import com.stepup.android.domain.Faction
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.ShoeCatalog
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.parseModelSlotKey
import com.stepup.android.domain.parseSlotKey
import com.stepup.android.ui.components.shoeModelImageRes
import com.stepup.android.ui.components.shoeModelNameRes
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 새 신발 도감(0045) — 앱 표가 원본(design/shoes-2026-09/catalog.json) · 서버 · 체인과 같은 번호 · 등급인지,
 * 서버가 준 모델 번호로 그림 · 이름을 고르는지 본다.
 */
class ShoeCatalogTest {

    @Test
    fun `70종 — 레어 20 · 에픽 20 · 레전더리 30, 번호는 예전 52종과 겹치지 않는다`() {
        assertEquals(70, ShoeCatalog.models.size)
        assertEquals(70, ShoeCatalog.models.map { it.id }.toSet().size)
        assertEquals(20, ShoeCatalog.models.count { it.rarity == Rarity.RARE })
        assertEquals(20, ShoeCatalog.models.count { it.rarity == Rarity.EPIC })
        assertEquals(30, ShoeCatalog.models.count { it.rarity == Rarity.LEGENDARY })
        assertTrue(ShoeCatalog.models.all { it.id >= 1000 })
    }

    @Test
    fun `원본 catalog json 과 번호 · 등급 · 이름이 같다`() {
        val file = listOf(File("../design/shoes-2026-09/catalog.json"), File("design/shoes-2026-09/catalog.json")).first { it.exists() }
        val models = serverJson.parseToJsonElement(file.readText())
            .let { it as kotlinx.serialization.json.JsonObject }["models"] as kotlinx.serialization.json.JsonArray
        assertEquals(ShoeCatalog.models.size, models.size)
        models.forEachIndexed { i, element ->
            val m = element as kotlinx.serialization.json.JsonObject
            val app = ShoeCatalog.models[i]
            assertEquals(m["id"].toString().toInt(), app.id)
            assertEquals(m["rarity"].toString().trim('"'), app.rarity.id)
            assertEquals(m["en"].toString().trim('"'), app.englishName)
        }
    }

    @Test
    fun `모든 모델에 그림 · 이름 리소스가 있다`() {
        for (m in ShoeCatalog.models) {
            assertNotNull("그림 ${m.id}", shoeModelImageRes(m.id))
            assertNotNull("이름 ${m.id}", shoeModelNameRes(m.id))
        }
        assertNull(shoeModelImageRes(21))
    }

    @Test
    fun `서버가 준 모델 번호 · 토큰 번호가 신발까지 온다`() {
        val row = serverJson.decodeFromString<List<ServerSneakerRow>>(
            """[{"id":7,"faction":"WATER","rarity":"LEGENDARY","variant":1,"level":2,"max_level":30,
                "efficiency_bps":1100,"comfort_bps":950,"durability":100,"equipped":false,"origin":"FREE_DRAW",
                "model_id":1317,"token_id":1000002}]""",
        ).single()
        assertEquals(1317, row.modelId)
        assertEquals(1_000_002L, row.tokenId)
        // 예전 서버(0045 전)는 두 칸이 없다
        val old = serverJson.decodeFromString<List<ServerSneakerRow>>(
            """[{"id":8,"faction":"FIRE","rarity":"RARE","variant":0,"level":1,"max_level":15,
                "efficiency_bps":500,"comfort_bps":400,"durability":100,"equipped":true,"origin":"STARTER"}]""",
        ).single()
        assertNull(old.modelId)
        assertNull(old.tokenId)
    }

    @Test
    fun `새 도감 신발은 모델 번호로 묶고 이름을 짓는다 — 예전 신발은 그대로`() {
        val entity = SneakerEntity(
            id = 3, factionId = "WATER", rarity = "LEGENDARY", variant = 1, level = 1, mintNumber = 1, luck = 1.0,
            comfort = 1.0, durability = 100, equipped = false, acquiredAt = 0, origin = "FREE_DRAW", modelId = 1317,
        )
        val shoe = entity.toDomain()
        assertEquals(1317, shoe.modelId)
        assertEquals("M:1317", shoe.slotKey)
        assertEquals(1317, parseModelSlotKey(shoe.slotKey))
        assertNull(parseSlotKey(shoe.slotKey))
        assertEquals(ShoeCatalog.of(1317)!!.englishName, shoe.displayName)
        assertNull(entity.copy(modelId = 0).toDomain().modelId)

        val legacy = Sneaker(
            id = 1, faction = Faction.FIRE, rarity = Rarity.EPIC, variant = 1, level = 1, mintNumber = 1,
            luck = 1.0, comfort = 1.0, durability = 100, equipped = false, acquiredAt = 0,
        )
        assertEquals("FIRE:EPIC:1", legacy.slotKey)
        assertNull(parseModelSlotKey(legacy.slotKey))
        assertNull(parseModelSlotKey("M:21"))
    }
}
