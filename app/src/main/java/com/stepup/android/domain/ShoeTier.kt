package com.stepup.android.domain

/**
 * 신발 등급의 **보이는** 갈래 여섯 — 일반 · 레어 · 에픽 · 레전더리 · 레드라인 · 피니시(2026-09-28 신발 화면 확정안).
 *
 * 서버 · 보상 · 확률 · NFT 가 쓰는 등급은 여전히 [Rarity] 넷이다. 레드라인 · 피니시는 새 도감(0045)의 레전더리 30종 안에서
 * 시리즈([ShoeCatalog.Model.series])로 나뉜 것이라, 프레임 · 효과 · 배지만 따로 그린다 — 드롭률 · 적립 · 등급을 늘리지 않는다.
 */
enum class ShoeTier(val rarity: Rarity) {
    COMMON(Rarity.COMMON),
    RARE(Rarity.RARE),
    EPIC(Rarity.EPIC),
    LEGENDARY(Rarity.LEGENDARY),
    REDLINE(Rarity.LEGENDARY),
    FINISH(Rarity.LEGENDARY),
    ;

    /** 그림 · 검사 표식에 쓰는 이름 — "redline" */
    val key: String get() = name.lowercase(java.util.Locale.ROOT)
}

/** 알려진 시리즈(레드라인 · 피니시)를 먼저 보고, 나머지는 실제 등급 그대로 */
val Sneaker.tier: ShoeTier
    get() = when (catalogModel?.series) {
        "REDLINE" -> ShoeTier.REDLINE
        "FINISH" -> ShoeTier.FINISH
        else -> when (rarity) {
            Rarity.COMMON -> ShoeTier.COMMON
            Rarity.RARE -> ShoeTier.RARE
            Rarity.EPIC -> ShoeTier.EPIC
            Rarity.LEGENDARY -> ShoeTier.LEGENDARY
        }
    }
