package com.stepup.android.domain

/**
 * 신발 등급의 **보이는** 갈래 여섯 — 일반 · 레어 · 에픽 · 레전더리 · 레드라인 · 피니시(2026-09-28 신발 화면 확정안).
 *
 * 2026-10-17 부터 레드라인 · 피니시도 [Rarity] 의 등급이다(서버 0066 — 확률 · 스탯 · 내구도 · 최대 레벨이 따로). 보이는 갈래와
 * 등급이 하나씩 맞는다. 체인(v3) 도감에서는 둘 다 레전더리로 올라가 있다.
 */
enum class ShoeTier(val rarity: Rarity) {
    COMMON(Rarity.COMMON),
    RARE(Rarity.RARE),
    EPIC(Rarity.EPIC),
    LEGENDARY(Rarity.LEGENDARY),
    REDLINE(Rarity.REDLINE),
    FINISH(Rarity.FINISH),
    ;

    /** 그림 · 검사 표식에 쓰는 이름 — "redline" */
    val key: String get() = name.lowercase(java.util.Locale.ROOT)
}

/** 등급 그대로 — 레드라인 · 피니시도 등급이다(서버 0066) */
val Sneaker.tier: ShoeTier
    get() = rarity.tier

val Rarity.tier: ShoeTier
    get() = when (this) {
        Rarity.COMMON -> ShoeTier.COMMON
        Rarity.RARE -> ShoeTier.RARE
        Rarity.EPIC -> ShoeTier.EPIC
        Rarity.LEGENDARY -> ShoeTier.LEGENDARY
        Rarity.REDLINE -> ShoeTier.REDLINE
        Rarity.FINISH -> ShoeTier.FINISH
    }
