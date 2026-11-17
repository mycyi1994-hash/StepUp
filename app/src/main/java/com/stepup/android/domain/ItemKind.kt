package com.stepup.android.domain

/**
 * 아이템(서버 0106 `economy.item_kinds`). 효과 · 값은 서버가 정하고(`items_me` 의 value) 앱은 그 값을 보여 줄 뿐이다.
 * 이름 · 그림은 앱이 들고 있다 — 종류를 더하면 서버 표 · 운영자 페이지(web/ops/ops.js) · 푸시 문구(push-send) · 여기를 함께 고친다.
 * 모르는 종류(새 서버 · 옛 앱)는 화면에 보이지 않는다([of] 가 null).
 */
enum class ItemKind(val id: String, val group: String, val runPick: Boolean) {
    /** 러닝 부스터 — 고른 러닝 1회 적립 × value */
    RUN_BOOSTER("RUN_BOOSTER", "REWARD", true),

    /** 보상 2배권 — 고른 러닝 1회 적립 × value */
    REWARD_DOUBLE("REWARD_DOUBLE", "REWARD", true),

    /** 에너지 드링크 — 고른 러닝 1회 에너지를 value 만큼만 */
    ENERGY_DRINK("ENERGY_DRINK", "ENERGY", true),

    /** 내구도 초기화권 — 신은 신발 내구도를 가득(바로 쓴다) */
    DURABILITY_RESET("DURABILITY_RESET", "DURABILITY", false),

    /** 하루 할당량 초기화권 — 오늘 쓴 에너지를 되돌린다(바로 쓴다) */
    ENERGY_RESET("ENERGY_RESET", "DAILY", false);

    companion object {
        fun of(id: String?): ItemKind? = entries.firstOrNull { it.id == id }
    }
}

/** 가방 한 줄 — 종류 · 서버 값 · 개수 */
data class BagItem(val kind: ItemKind, val value: Double?, val qty: Int)

/** 지금 켜진 보상 이벤트(운영자가 연다 — 그 시간에 시작한 러닝 적립 × mult) */
data class RewardEvent(val title: String, val mult: Double, val endsAtMillis: Long)

/** 내 가방(`items_me`) */
data class Bag(val items: List<BagItem>, val event: RewardEvent?, val boostUntilMillis: Long?) {
    val total: Int get() = items.sumOf { it.qty }
    fun qty(kind: ItemKind): Int = items.firstOrNull { it.kind == kind }?.qty ?: 0
    fun value(kind: ItemKind): Double? = items.firstOrNull { it.kind == kind }?.value
    /** 출발 전에 고를 수 있는 것(가진 것만) */
    val runPicks: List<BagItem> get() = items.filter { it.kind.runPick && it.qty > 0 }

    /**
     * 아이템 없이도 지금 켜져 있는 적립 배수 — 보상 이벤트 · 24시간 부스터 중 큰 것(없으면 1).
     * 서버는 이것과 러닝 아이템 가운데 가장 큰 것 하나만 쓴다(`economy.run_reward_mult`).
     */
    fun activeRewardMult(now: Long): Double = maxOf(
        1.0,
        event?.takeIf { it.endsAtMillis > now }?.mult ?: 1.0,
        if ((boostUntilMillis ?: 0L) > now) RunItemPicks.BOOSTER_MULT else 1.0,
    )
}

/**
 * 출발 전 고르기 규칙 — 같은 묶음(적립 배수 · 에너지)은 하나만(서버 `run_items_use` 도 거절한다).
 * 이미 고른 것을 다시 누르면 뺀다, 같은 묶음의 다른 것을 누르면 바꾼다.
 */
object RunItemPicks {
    fun toggle(picked: Set<ItemKind>, kind: ItemKind): Set<ItemKind> {
        if (!kind.runPick) return picked
        if (kind in picked) return picked - kind
        return picked.filterNot { it.group == kind.group }.toSet() + kind
    }

    /** 24시간 부스터의 적립 배수(서버 `economy.run_reward_mult` 와 같은 값) */
    const val BOOSTER_MULT = 2.0

    /** 가진 개수가 0 이 된 것은 뺀다(다른 폰에서 썼거나 되돌려진 지급) */
    fun keepOwned(picked: Set<ItemKind>, bag: Bag?): Set<ItemKind> =
        if (bag == null) picked else picked.filter { bag.qty(it) > 0 }.toSet()

    /**
     * 지금 써도 적립이 늘지 않는 아이템 — 이미 켜진 배수(보상 이벤트 · 24시간 부스터)가 이 아이템과 같거나 크다.
     * 서버는 큰 것 하나만 쓰므로 고르면 아이템만 사라진다 — 고르지 못하게 한다(2026-11-17 점검).
     */
    fun covered(kind: ItemKind, bag: Bag?, now: Long): Boolean =
        bag != null && kind.group == "REWARD" && (bag.value(kind) ?: 1.0) <= bag.activeRewardMult(now)

    /** 가진 것 · 지금 쓸모 있는 것만 남긴다 */
    fun keepUseful(picked: Set<ItemKind>, bag: Bag?, now: Long): Set<ItemKind> =
        keepOwned(picked, bag).filterNot { covered(it, bag, now) }.toSet()

    /** 이번 러닝의 적립 배수(아이템만 — 부스터 · 이벤트와 겹치면 서버가 큰 것 하나만 쓴다) */
    fun rewardMult(picked: Set<ItemKind>, bag: Bag?): Double =
        picked.filter { it.group == "REWARD" }.maxOfOrNull { bag?.value(it) ?: 1.0 } ?: 1.0

    /** 이번 러닝의 에너지 비율 */
    fun energyFactor(picked: Set<ItemKind>, bag: Bag?): Double =
        picked.filter { it.group == "ENERGY" }.minOfOrNull { bag?.value(it) ?: 1.0 } ?: 1.0
}
