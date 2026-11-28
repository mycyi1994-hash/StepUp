package com.stepup.android.ui.screens.customize

import com.stepup.android.domain.ShoeRules
import com.stepup.android.domain.ShoeTier
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.tier
import com.stepup.android.ui.screens.items.ItemSort
import com.stepup.android.ui.screens.items.comfortBarEnd
import com.stepup.android.ui.screens.items.comfortIndex
import com.stepup.android.ui.screens.items.durabilityFraction
import com.stepup.android.ui.screens.items.durabilityLeft
import com.stepup.android.ui.screens.items.durabilityMax
import com.stepup.android.ui.screens.items.efficiencyBarEnd
import com.stepup.android.ui.screens.items.efficiencyIndex
import com.stepup.android.ui.screens.items.formatCount
import com.stepup.android.ui.screens.items.formatDurability
import com.stepup.android.ui.screens.items.formatIndex
import com.stepup.android.ui.screens.items.wholeMinutes

/*
 * 신발 화면 확정안(2026-09-28)이 쓰는 순수 계산 — 내 신발의 능력치 막대 · 보유 목록 순서, 신발 보관함의 정렬 · 거르기.
 *
 * 값은 모두 실제 신발([Sneaker])에서 온다(기존 상세와 같은 출처 — ShoeDetailModel). 시안의 예시 값(+4.8% · 7.5% · 92 / 100)을
 * 쓰지 않는다. 능력치는 경제 4판(0116)의 지수 · 분으로 보인다(ShoeStatsV4). 고르기는 늘 소유 id 로 한다.
 */

/**
 * 막대 끝(경제 4판, 서버 0116) — 효율 · 착화감은 지수(100 = 기준, 숫자가 2배면 2배)이고 막대 끝은 **그 등급에서 가장 잘 뽑힌 새 신발**
 * (서버 shoe_rules_v4 의 등급 범위 꼭대기)이다. 같은 등급 안에서 얼마나 잘 뽑혔는지가 막대로 보이고, 포인트로 넘으면 가득 찬다.
 * 내구도는 그 신발의 최대 내구도가 끝(달릴 수 있는 분으로 보인다). 막대 길이 = 실제 값 ÷ 막대 끝 — 값을 100점으로 바꾸지 않는다.
 */
object StatScale {
    /** 지수의 기준 — 효율 100 = 걸음당 기본 SUP(서버 points_per_step), 착화감 100 = 에너지 1 에 600걸음 */
    const val INDEX_BASE = 100.0
}

enum class BarStat { EFFICIENCY, COMFORT, DURABILITY }

/** 값의 단위 — 지수는 단위가 없고, 내구도는 분(화면이 "28 / 31분"처럼 단위를 붙인다). 최대 내구도를 모르는 예전 신발은 그대로 */
enum class StatUnit { NONE, MINUTES }

/** 막대 한 줄 — 보이는 값("140" · "28 / 31"), 막대 길이(0 ~ 1), 막대 끝("180" · "31"), 단위 */
data class StatBar(val stat: BarStat, val value: String, val fraction: Float, val end: String, val unit: StatUnit = StatUnit.NONE)

/** 막대 길이 — 값 ÷ 막대 끝, 0 ~ 1 로 자른다 */
fun barFraction(value: Double, max: Double): Float =
    if (max <= 0.0 || value.isNaN()) 0f else (value / max).coerceIn(0.0, 1.0).toFloat()

/**
 * 내 신발의 막대 셋 — 효율 지수 · 착화감 지수 · 내구도(분). 서버 신발에 없는 행운은 만들지 않는다.
 * 막대 끝은 [rules] 의 그 등급 꼭대기(서버를 아직 못 읽었으면 서버 0116 과 같은 값).
 */
fun statBars(shoe: Sneaker, rules: ShoeRules = ShoeRules.FALLBACK): List<StatBar> {
    val efficiency = efficiencyIndex(shoe)
    val comfort = comfortIndex(shoe)
    val efficiencyEnd = efficiencyBarEnd(shoe, rules)
    val comfortEnd = comfortBarEnd(shoe, rules)
    val minutes = shoe.server?.hasDurabilityMax == true
    val durability = if (minutes) {
        // 분 = 내구도 ÷ (5 × 효율 포인트 배수)(서버 0119 — 효율에 포인트를 넣은 신발은 그만큼 빨리 닳는다). 막대 길이는 남은 ÷ 최대 그대로
        val full = wholeMinutes(shoe, durabilityMax(shoe), rules)
        StatBar(
            BarStat.DURABILITY, "${wholeMinutes(shoe, durabilityLeft(shoe), rules)} / $full",
            durabilityFraction(shoe), "$full", StatUnit.MINUTES,
        )
    } else {
        StatBar(BarStat.DURABILITY, formatDurability(shoe), durabilityFraction(shoe), formatCount(durabilityMax(shoe)))
    }
    return listOf(
        StatBar(BarStat.EFFICIENCY, formatIndex(efficiency), barFraction(efficiency, efficiencyEnd), formatIndex(efficiencyEnd)),
        StatBar(BarStat.COMFORT, formatIndex(comfort), barFraction(comfort, comfortEnd), formatIndex(comfortEnd)),
        durability,
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
val VaultSorts: List<String> = listOf(ItemSort.RECENT, ItemSort.RARITY, ItemSort.LEVEL, ItemSort.DURABILITY)

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
        ItemSort.DURABILITY -> shoes.sortedWith(
            compareBy<Sneaker> { com.stepup.android.ui.screens.items.durabilityPercent(it) }.then(recent),
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
