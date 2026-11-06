package com.stepup.android.domain

/**
 * 거래소 줄(속성 · 등급 · 변형 · 레벨)로 그리는 신발 — 신발 탭이 같은 신발을 그리는 규칙 그대로(2026-11-04).
 *
 * 신발 탭은 도감 모델 번호(modelId)가 있으면 그 모델 그림 · 이름, 없으면 속성 × 등급 × 변형 그림을 쓴다. 속성 글자 "NONE"(0066 뒤 신발)은
 * 그림 · 색 때문에 불(FIRE)로 기운다(SneakerEntity.toDomain 과 같다).
 *
 * 거래소 줄에는 모델 번호가 없다. 0066 뒤 신발 가운데 도감 모델이 있는 등급(레어부터)은 뽑을 때 모델을 따로 고르므로, 같은 줄이라도
 * 매물마다 모델이 다르다 — 그 줄에 불 그림 · 이름을 붙이면 사는 사람이 받을 신발과 다르다. 그런 줄은 [pinsDesign] 이 false 이고,
 * 화면은 특정 모델 대신 등급만 보인다. 일반 등급은 도감 모델이 없어(서버 draw_model 이 비운다) 신발 탭도 불 그림이라 그대로 맞다.
 */
object MarketShoeLook {

    /** 0066 부터 새 신발의 속성 글자 */
    const val NO_FACTION = "NONE"

    /** 이 줄의 신발이 줄 값만으로 그림 · 이름이 정해지는가 — false 면 매물마다 모델이 다를 수 있다 */
    fun pinsDesign(factionId: String, rarity: String): Boolean {
        if (factionId != NO_FACTION) return true
        val r = Rarity.of(rarity)
        return ShoeCatalog.models.none { it.rarity == r }
    }

    /**
     * 그리기용 신발 한 켤레 — 남의 신발이라 폰에 없다. 속성 · 등급 · 변형 · 레벨만 쓰고 나머지는 화면에 나오지 않는 기본값이다.
     * 속성 · 변형은 폰 DB 의 신발을 읽을 때(SneakerEntity.toDomain)와 같은 규칙으로 맞춘다.
     */
    fun preview(factionId: String, rarity: String, variant: Int, level: Int = 1): Sneaker {
        val r = Rarity.of(rarity)
        return Sneaker(
            id = 0,
            faction = Faction.of(factionId),
            rarity = r,
            variant = variant.coerceIn(0, r.variantCount - 1),
            level = level,
            mintNumber = 0,
            luck = 1.0,
            comfort = 1.0,
            durability = 100,
            equipped = false,
            acquiredAt = 0,
            factionId = factionId,
        )
    }
}
