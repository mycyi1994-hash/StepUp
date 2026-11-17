package com.stepup.android

import com.stepup.android.data.local.SneakerEntity
import com.stepup.android.data.remote.ModelKey
import com.stepup.android.data.remote.QuoteRow
import com.stepup.android.data.repo.toDomain
import com.stepup.android.data.repo.MarketRefusal
import com.stepup.android.domain.MarketFeature
import com.stepup.android.domain.MarketShoeLook
import com.stepup.android.domain.SellBlock
import com.stepup.android.domain.sellBlock
import com.stepup.android.ui.Routes
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 테스터 "신발 상세에서 판매하기를 누르면 딴 신발로 연결된다"(2026-11-17) — 타비 스플릿 토 조거(레어 · 도감 1119)의 판매하기가
 * 예전 불 모델 "플레어 시프트"(FIRE · 레어 · 변형) 장을 열었다. 0107 부터 도감 모델이 있는 신발은 그 모델 장으로 간다.
 */
class MarketByModelTest {

    private fun shoe(
        modelId: Int = 1119,
        variant: Int = 3,
        origin: String = "FREE_DRAW",
        canWithdraw: Boolean = false,
        equipped: Boolean = false,
        status: String = "OWNED",
        kmRun: Double = 0.0,
        lockKm: Double = 50.0,
    ) = SneakerEntity(
        id = 7, factionId = "NONE", rarity = "RARE", variant = variant, level = 1, mintNumber = 365,
        luck = 1.0, comfort = 1.0, durability = 100, equipped = equipped, acquiredAt = 0, serverId = 70,
        origin = origin, status = status, canWithdraw = canWithdraw, modelId = modelId, kmRun = kmRun, lockKm = lockKm,
    ).toDomain()

    @Test fun `도감 모델 신발의 판매하기는 그 모델 장 — 변형 번호는 쓰지 않는다`() {
        val tabi = shoe()
        val key = ModelKey.of(tabi)
        assertEquals(ModelKey("NONE", "RARE", 0, 1119), key)
        assertEquals("market/NONE/RARE/0?sell=7&model=1119", Routes.marketModel(key, tabi.id))
        // 같은 줄(NONE · 레어 · 3)의 다른 모델은 다른 장
        assertTrue(ModelKey.of(shoe(modelId = 1120)) != key)
        // 같은 모델이면 변형이 달라도 같은 장
        assertEquals(key, ModelKey.of(shoe(variant = 5)))
        // 장의 이름 · 그림은 그 모델 — 예전 불 모델(플레어 시프트)을 붙이지 않는다
        assertTrue(MarketShoeLook.pinsDesign(key.faction, key.rarity, key.modelId))
        assertEquals(1119, MarketShoeLook.preview(key.faction, key.rarity, key.variant, modelId = key.modelId).modelId)
    }

    @Test fun `모델 없는 신발(일반 · 예전 52종)은 예전처럼 속성 × 등급 × 변형`() {
        val common = SneakerEntity(
            id = 8, factionId = "NONE", rarity = "COMMON", variant = 2, level = 1, mintNumber = 1,
            luck = 1.0, comfort = 1.0, durability = 100, equipped = false, acquiredAt = 0,
        ).toDomain()
        assertEquals(ModelKey("NONE", "COMMON", 2), ModelKey.of(common))
        assertEquals("market/NONE/COMMON/2?sell=0&model=0", Routes.marketModel(ModelKey.of(common)))
    }

    @Test fun `시세판 2판 줄의 장 — model_id 가 있으면 모델 장`() {
        val json = Json { ignoreUnknownKeys = true }
        val rows = json.decodeFromString<List<QuoteRow>>(
            """[{"faction":"NONE","rarity":"RARE","variant":0,"model_id":1119,"supply":3,"listed":1,"ask":120},
                {"faction":"NONE","rarity":"COMMON","variant":2,"model_id":null,"supply":9,"listed":0}]""",
        )
        assertEquals(ModelKey.ofModel("RARE", 1119), rows[0].key)
        assertEquals(ModelKey("NONE", "COMMON", 2), rows[1].key)
    }

    @Test fun `팔 수 없는 까닭 — 서버 값으로 가른다`() {
        // 테스터의 신발: 무료 뽑기 · 아직 잠금 거리를 못 달림
        assertEquals(SellBlock.Locked(50.0), shoe().sellBlock)
        assertEquals(SellBlock.Locked(17.5), shoe(kmRun = 32.5).sellBlock)
        assertNull(shoe(canWithdraw = true, kmRun = 50.0).sellBlock)
        assertEquals(SellBlock.Equipped, shoe(canWithdraw = true, equipped = true).sellBlock)
        assertEquals(SellBlock.Listed, shoe(status = "LISTED").sellBlock)
        assertEquals(SellBlock.Starter, shoe(origin = "STARTER", lockKm = 0.0).sellBlock)
        // 동기화 전(잠금 거리를 아직 모름) — 숫자 없이 말한다
        assertEquals(SellBlock.Locked(0.0), shoe(lockKm = 0.0).sellBlock)
        // 폰에만 있던 옛 신발
        val legacy = SneakerEntity(
            factionId = "WIND", rarity = "RARE", variant = 1, level = 1, mintNumber = 1,
            luck = 1.0, comfort = 1.0, durability = 100, equipped = false, acquiredAt = 0,
        ).toDomain()
        assertEquals(SellBlock.Other, legacy.sellBlock)
    }

    @Test fun `거래를 멈춘 동안 앱은 거래 버튼을 막고, 서버 거절은 멈춤 안내로 읽는다(0108)`() {
        // 사용자 "일단 지금 판매하기 버튼도 비활성화 — 거래 하면 안 돼". 다시 열 때는 서버 market_trading_enabled 와 함께 켠다
        assertEquals(false, MarketFeature.TRADING)
        assertEquals(MarketRefusal.PAUSED, MarketRefusal.of("지금은 거래소를 쉬고 있습니다"))
    }
}
