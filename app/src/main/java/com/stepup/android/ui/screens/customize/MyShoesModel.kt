package com.stepup.android.ui.screens.customize

import com.stepup.android.domain.ShoeTier
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.tier
import com.stepup.android.ui.screens.items.ItemSort
import com.stepup.android.ui.screens.items.bonusPercent
import com.stepup.android.ui.screens.items.durabilityFraction
import com.stepup.android.ui.screens.items.durabilityMax
import com.stepup.android.ui.screens.items.energySavingPercent
import com.stepup.android.ui.screens.items.formatBonus
import com.stepup.android.ui.screens.items.formatCount
import com.stepup.android.ui.screens.items.formatDurability
import com.stepup.android.ui.screens.items.formatPercent

/*
 * 신발 화면 확정안(2026-09-28)이 쓰는 순수 계산 — 내 신발의 능력치 막대 · 보유 목록 순서, 신발 보관함의 정렬 · 거르기.
 *
 * 값은 모두 실제 신발([Sneaker])에서 온다(기존 상세와 같은 출처 — ShoeDetailModel). 시안의 예시 값(+4.8% · 7.5% · 92 / 100)을
 * 쓰지 않는다. 고르기는 늘 소유 id 로 한다.
 */

/**
 * 막대 끝 — 효율 · 착화감은 모든 신발(서버 신발 · 폰에만 있던 예전 신발)에 같은 기준을 쓴다. 내구도는 신발마다 최대
 * 내구도가 달라(서버 0059) 그 신발의 최대 내구도가 끝이다. 값을 100점으로 바꾸지 않는다: 막대 길이 = 실제 값 ÷ 막대 끝.
 */
object StatScale {
    /**
     * 효율(SUP 적립 보너스) — 레전더리 최고 효율 1,300bps + 레벨마다 0.5%p(포인트를 효율 2 · 착화감 2 로 고르게 나눈 값,
     * 0059 전의 자동 증가와 같다) × (최고 30레벨 − 1) = 2,750bps = 27.5%. 포인트를 효율에 더 몰면 이보다 높아질 수 있고,
     * 그때는 막대가 가득 찬다. supabase/migrations/0022 · 0059 의 economy.efficiency_range · point_efficiency_bps · max_level.
     */
    const val EFFICIENCY_MAX_PERCENT = 27.5

    /** 효율 막대 끝이 되는 레벨 — 레전더리 최고 레벨(economy.max_level) */
    const val EFFICIENCY_MAX_LEVEL = 30

    /** 착화감(에너지 절감) — economy.comfort_cap_bps() 2,000bps = 20%. 예전 신발은 최대 15%(Sneaker.energyEfficiency) */
    const val ENERGY_MAX_PERCENT = 20.0
}

enum class BarStat { EFFICIENCY, COMFORT, DURABILITY }

/** 막대 한 줄 — 보이는 값("+4.8%"), 막대 길이(0 ~ 1), 막대 끝("27.5%" · "20%" · 그 신발의 최대 내구도 "1,230") */
data class StatBar(val stat: BarStat, val value: String, val fraction: Float, val end: String)

/** 막대 길이 — 값 ÷ 막대 끝, 0 ~ 1 로 자른다 */
fun barFraction(value: Double, max: Double): Float =
    if (max <= 0.0 || value.isNaN()) 0f else (value / max).coerceIn(0.0, 1.0).toFloat()

/** 내 신발의 막대 셋 — 효율 · 착화감(에너지 절감) · 내구도. 서버 신발에 없는 행운은 만들지 않는다 */
fun statBars(shoe: Sneaker): List<StatBar> {
    val bonus = bonusPercent(shoe)
    val energy = energySavingPercent(shoe)
    return listOf(
        StatBar(
            BarStat.EFFICIENCY, formatBonus(bonus), barFraction(bonus, StatScale.EFFICIENCY_MAX_PERCENT),
            formatPercent(StatScale.EFFICIENCY_MAX_PERCENT),
        ),
        StatBar(
            BarStat.COMFORT, formatPercent(energy), barFraction(energy, StatScale.ENERGY_MAX_PERCENT),
            formatPercent(StatScale.ENERGY_MAX_PERCENT),
        ),
        StatBar(BarStat.DURABILITY, formatDurability(shoe), durabilityFraction(shoe), formatCount(durabilityMax(shoe))),
    )
}

/**
 * 내 신발의 보유 가로 목록 — 켤레마다 한 칸(같은 모델도 소유 id 로 따로). 신고 있는 켤레 먼저, 그다음 최근에 받은 순.
 * 칸을 눌러 보기만 바꾸면 순서가 그대로다(착용이 바뀔 때만 앞자리가 바뀐다).
 */
fun ownedRow(shoes: List<Sneaker>): List<Sneaker> =
    shoes.sortedWith(
        compareByDescending<Sneaker> { it.equipped }
            .thenByDescending { it.acquiredAt }
            .thenByDescending { it.id },
    )

/** 지금 보는 켤레 — 고른 id 가 있으면 그 켤레, 없으면 신고 있는 켤레, 그것도 없으면 목록 첫 켤레 */
fun resolvePair(shoes: List<Sneaker>, selectedId: Long?): Sneaker? =
    shoes.firstOrNull { it.id == selectedId } ?: shoes.firstOrNull { it.equipped } ?: ownedRow(shoes).firstOrNull()

/** 보관함 정렬 — 기본은 최근 획득순(시안 02). 착용은 순서를 바꾸지 않는다(체크로만 보인다) */
val VaultSorts: List<String> = listOf(ItemSort.RECENT, ItemSort.RARITY, ItemSort.LEVEL)

/**
 * 보관함 순서. 등급 높은순은 서버 등급(넷)으로 매기고 같은 등급 안에서는 보이는 갈래(레전더리 · 레드라인 · 피니시)끼리 모은다 —
 * 레드라인 · 피니시가 레전더리보다 "높은" 등급인 것처럼 매기지 않는다.
 */
fun vaultSorted(shoes: List<Sneaker>, sort: String): List<Sneaker> {
    val recent = compareByDescending<Sneaker> { it.acquiredAt }.thenByDescending { it.id }
    return when (sort) {
        ItemSort.RARITY -> shoes.sortedWith(
            compareByDescending<Sneaker> { it.rarity.ordinal }.thenBy { it.tier.ordinal }.then(recent),
        )
        ItemSort.LEVEL -> shoes.sortedWith(
            compareByDescending<Sneaker> { it.level }.thenByDescending { it.rarity.ordinal }.then(recent),
        )
        else -> shoes.sortedWith(recent)
    }
}

/** 거르기 칸 하나 — [tier] 가 null 이면 전체. 수는 실제 켤레 수 */
data class TierCount(val tier: ShoeTier?, val count: Int)

/** 전체 + 가진 갈래만(0켤레인 갈래는 칸을 만들지 않는다) — 갈래 순서는 일반 → 피니시 */
fun tierCounts(shoes: List<Sneaker>): List<TierCount> {
    val byTier = shoes.groupingBy { it.tier }.eachCount()
    return listOf(TierCount(null, shoes.size)) +
        ShoeTier.entries.mapNotNull { tier -> byTier[tier]?.let { TierCount(tier, it) } }
}

/** 고른 거르기가 아직 유효한가 — 그 갈래 신발이 없어졌으면(판매 등) 전체로 돌아간다 */
fun activeFilter(shoes: List<Sneaker>, filterKey: String?): ShoeTier? =
    ShoeTier.entries.firstOrNull { it.key == filterKey }?.takeIf { tier -> shoes.any { it.tier == tier } }

/** 보관함에 보이는 켤레 — 거르고 정렬한다 */
fun vaultShown(shoes: List<Sneaker>, filter: ShoeTier?, sort: String): List<Sneaker> =
    vaultSorted(if (filter == null) shoes else shoes.filter { it.tier == filter }, sort)
