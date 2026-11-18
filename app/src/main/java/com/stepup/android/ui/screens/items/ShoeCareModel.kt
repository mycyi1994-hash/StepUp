package com.stepup.android.ui.screens.items

import com.stepup.android.domain.ForgeStats
import com.stepup.android.domain.Sneaker
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/*
 * 신발 상세 · 수리 · 강화(파란 톤 v4 전달본, docs/redesign/blue-v4-2026-10 — 상세·수리 30 · 강화 17)가 쓰는 순수 계산.
 *
 * 수리는 지금 서버 계약 그대로다 — sneaker_repair(p_id). 강화는 재료 신발 3개 · 확률 강화(0054 forge_*, 규칙 forge-v2)로
 * ShoeUpgradeViewModel · ForgeRepository 가 한다. 여기에는 강화 화면의 세 줄 계산만 둔다(성공 · 확률 · 소각을 폰에서 정하지 않는다).
 * 값은 모두 실제 신발 · 잔고에서 온다. 시안의 예시 숫자(83.07 · 50 · No. 0007)를 쓰지 않는다.
 */

// ── 금액 ───────────────────────────────────────────────────────────

/** 서버 원장의 소수 자리(numeric(20, 4)) */
private const val LEDGER_SCALE = 4

private fun Double.ledger(): BigDecimal = BigDecimal.valueOf(this).setScale(LEDGER_SCALE, RoundingMode.HALF_UP)

/**
 * 비용 · 차감액 — 서버 정밀도(소수 4자리)까지 그대로, 끝의 0 은 뺀다. 12.5 를 13 으로 올리지 않는다(확인한 금액과 실제 차감이 같아야 한다).
 * 50 → "50", 12.5 → "12.5", 1234.5678 → "1,234.5678"
 */
fun formatSupExact(amount: BigDecimal): String {
    val format = DecimalFormat("#,##0.####", DecimalFormatSymbols(Locale.ROOT))
    format.roundingMode = RoundingMode.HALF_UP
    return format.format(amount.setScale(LEDGER_SCALE, RoundingMode.HALF_UP))
}

fun formatSupExact(amount: Double): String = if (amount.isFinite()) formatSupExact(amount.ledger()) else "—"

// ── 수리 ───────────────────────────────────────────────────────────

/** 수리 1점(12초) 값의 소수 자리 — 서버 economy.repair_unit_of(round 8). 4자리로 자르면 차감액과 어긋난다 */
private const val UNIT_PRICE_SCALE = 8

/**
 * 수리 견적 — 서버 수리(0116 economy.repair_core)와 같은 계산: 고칠 양 = 최대 내구도 − 남은 내구도(1 = 12초),
 * 비용 = round(고칠 양 × 1점 값, 4). 1점 값은 서버가 준 그 신발의 값(my_sneakers.repair_cost_per_unit — 신발의 효율에 비례, 걸어서 버는
 * 돈의 45%)을 그대로 쓴다. 이틀에 한 번 무료 수리가 남아 있으면 서버가 1점 값을 0 으로 보내고 한 켤레를 가득 0 SUP 로 고친다([free]).
 * 서버가 아직 최대 내구도를 보내지 않으면(예전 서버) 예전 계산(0023): 고칠 양 = round(100 − 남은 비율, 2), 1점 값 = repair_cost_per_point.
 * 실제 차감은 서버가 다시 계산하고, 보인 값([cost])과 다르면 고치지 않는다(sneaker_repair_v2 의 p_expected) — 이 값은 확인용이다.
 */
data class RepairQuote(
    val shoeId: Long,
    val level: Int,
    /** 서버의 실제 남은 비율(%, 소수) — 수리가 끝났는지(100)를 이것으로 본다 */
    val durability: Double,
    /** 보이는 남은 · 최대 내구도(1 = 12초). 예전 서버면 내린 남은 비율 · 100 */
    val durabilityNow: Int,
    val durabilityMax: Int,
    /** 고칠 양 — 내구도 점(예전 서버는 남은 비율 점) */
    val points: BigDecimal,
    /** 1점 값(SUP) — 무료 수리면 0 */
    val perPoint: BigDecimal,
    val cost: BigDecimal,
    /** 남은 · 최대 내구도를 아는 서버(0059 ~)의 견적인가 — 고칠 양을 서버(p_points)에 보내고, 끝났는지를 남은 내구도로 본다 */
    val absolute: Boolean = true,
    /** 이틀에 한 번 무료 수리(0116) — 한 켤레를 가득, 0 SUP */
    val free: Boolean = false,
    /** 가진 만큼만 고치는 견적(잔액이 모자랄 때 — [partialRepairOf]) */
    val partial: Boolean = false,
) {
    /** 수리한 뒤의 남은 내구도 — 서버가 이만큼이 되면 끝났다 */
    val targetNow: Int get() = if (!absolute || free) durabilityMax else (durabilityNow + points.toInt()).coerceAtMost(durabilityMax)

    /** 서버에 보낼 고칠 양(p_points) — 예전 서버의 견적은 비운다(서버가 가득) */
    val sendPoints: Int? get() = if (absolute) points.toInt() else null

    /** 같은 견적인가 — 레벨 · 내구도 · 단가 · 비용 · 무료 · 부분이 그대로면 같다(RP10 은 다를 때만) */
    fun sameAs(other: RepairQuote): Boolean =
        shoeId == other.shoeId && level == other.level && points.compareTo(other.points) == 0 &&
            perPoint.compareTo(other.perPoint) == 0 && cost.compareTo(other.cost) == 0 &&
            free == other.free && partial == other.partial
}

/** 서버 신발이 아니면(폰에만 있는 예전 신발) null — 수리는 서버만 한다 */
fun repairQuoteOf(shoe: Sneaker): RepairQuote? {
    val server = shoe.server ?: return null
    if (server.hasDurabilityMax) {
        val now = server.durabilityNow.coerceIn(0, server.durabilityMax)
        val points = BigDecimal.valueOf((server.durabilityMax - now).toLong())
        // 0116 — 무료 수리가 남아 있으면 서버가 1점 값을 0 으로 보낸다(0059 ~ 0115 서버는 늘 0 보다 크다)
        val free = server.repairCostPerUnit <= 0.0
        val perPoint = BigDecimal.valueOf(server.repairCostPerUnit.coerceAtLeast(0.0)).setScale(UNIT_PRICE_SCALE, RoundingMode.HALF_UP)
        val cost = points.multiply(perPoint).setScale(LEDGER_SCALE, RoundingMode.HALF_UP)
        return RepairQuote(shoe.id, shoe.level, server.durabilityPts, now, server.durabilityMax, points, perPoint, cost, free = free)
    }
    val points = BigDecimal.valueOf(100.0).subtract(BigDecimal.valueOf(server.durabilityPts))
        .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
    val perPoint = BigDecimal.valueOf(server.repairCostPerPoint).setScale(LEDGER_SCALE, RoundingMode.HALF_UP)
    val cost = points.multiply(perPoint).setScale(LEDGER_SCALE, RoundingMode.HALF_UP)
    return RepairQuote(
        shoe.id, shoe.level, server.durabilityPts,
        kotlin.math.floor(server.durabilityPts).toInt().coerceIn(0, 100), 100, points, perPoint, cost, absolute = false,
    )
}

/**
 * 가진 만큼 수리(잔액이 모자랄 때) — 고칠 양 = 내림(잔고 ÷ 1점 값), 그 값이 1 이상이고 가득보다 적을 때만. 비용은 서버와 같은 반올림
 * (소수 4자리)이고 잔고를 넘지 않는다. 무료 · 예전 서버(남은 비율 점)의 견적에는 없다(null).
 */
fun partialRepairOf(quote: RepairQuote, balance: Double): RepairQuote? {
    if (quote.free || !quote.absolute || quote.partial || quote.perPoint.signum() <= 0 || !balance.isFinite()) return null
    val have = balance.ledger()
    if (have.signum() <= 0) return null
    var units = have.divide(quote.perPoint, 0, RoundingMode.DOWN).toLong()
        .coerceAtMost(quote.points.toLong() - 1)
    fun costOf(n: Long) = BigDecimal.valueOf(n).multiply(quote.perPoint).setScale(LEDGER_SCALE, RoundingMode.HALF_UP)
    // 반올림으로 잔고를 아주 조금 넘으면 하나 줄인다
    while (units > 0 && costOf(units) > have) units--
    if (units < 1) return null
    return quote.copy(points = BigDecimal.valueOf(units), cost = costOf(units), partial = true)
}

/** 부족한 금액 — max(비용 − 잔고, 0) */
fun repairShortfall(quote: RepairQuote, balance: Double): BigDecimal =
    quote.cost.subtract(balance.ledger()).max(BigDecimal.ZERO)

/** 수리 후 예상 잔고 — 아직 완료된 잔고가 아니다(처리 중 · 확인 화면에서만) */
fun balanceAfter(balance: Double, cost: BigDecimal): Double = balance.ledger().subtract(cost).toDouble()

/**
 * 다음 무료 수리까지 남은 시간(시간, 올려서 — 1 이상) — 무료가 지금 남아 있거나 모르면 null. [nextMs] 는 서버 shoe_rules_v4 의 free_repair_next.
 */
fun freeRepairHoursLeft(nextMs: Long?, nowMs: Long): Int? {
    if (nextMs == null || nextMs == Long.MAX_VALUE) return null
    val left = nextMs - nowMs
    if (left <= 0) return null
    return ((left + 3_599_999L) / 3_600_000L).toInt().coerceAtLeast(1)
}

/** 수리할 수 없는 확인된 까닭(RP09) — 네트워크 오류는 여기에 넣지 않는다(그건 RP07 · RP08) */
enum class CareBlock {
    /** 판매 중(서버 status ≠ OWNED) */
    LISTED,

    /** 체인에 나가 있다(chain_state ≠ APP) */
    ON_CHAIN,

    /** 폰에서 올린 예전 신발(서버 거절 문구 "예전 신발") */
    LEGACY,

    /** 성공한 조회에 이 소유 id 가 없다 */
    MISSING,

    /** 서버가 거절했는데 까닭을 모른다 */
    OTHER,
}

/** 서버 거절 문구 → 확인된 까닭(0023 economy.my_app_sneaker 의 문구) */
fun careBlockOf(reason: String): CareBlock = when {
    "판매 중" in reason -> CareBlock.LISTED
    "체인에 있는" in reason -> CareBlock.ON_CHAIN
    "예전 신발" in reason -> CareBlock.LEGACY
    "내 신발이 아닙니다" in reason || "없는 신발" in reason -> CareBlock.MISSING
    else -> CareBlock.OTHER
}

/** 수리 시트 한 장 — RP01 ~ RP11. 한 번에 시트는 하나다 */
sealed interface RepairPhase {
    /** RP02 — 최신 내구도 · 비용 · 잔고를 읽는 중(읽기만 한다) */
    data object Loading : RepairPhase

    /** RP01 · RP10 — 확인. [changed] 면 확인 중에 값이 바뀌어 새 견적을 다시 확인받는다(RP10) */
    data class Confirm(val quote: RepairQuote, val balance: Double, val changed: Boolean = false) : RepairPhase

    /**
     * RP06 — 잔고 부족. 가득 고치는 버튼은 누를 수 없다. [partial] 이 있으면 "가진 만큼 수리"가 주 행동이다(없으면 고칠 수 없다).
     * [changed] 면 수리비가 바뀌어 다시 확인했다(서버 QUOTE_CHANGED · 동기화)
     */
    data class Insufficient(
        val quote: RepairQuote,
        val balance: Double,
        val partial: RepairQuote? = null,
        val changed: Boolean = false,
    ) : RepairPhase

    /** RP05 — 이미 가득(청구 없음) */
    data class NotNeeded(val durability: Double) : RepairPhase

    /** RP09 — 확인된 불가 사유 */
    data class Blocked(val reason: CareBlock) : RepairPhase

    /** RP07 — 보내기 전 조회 실패. "다시 불러오기"는 조회만 한다 */
    data object LoadFailed : RepairPhase

    /** RP11 — 로그인 필요. 로그인 뒤 자동으로 수리하지 않는다 */
    data object SignIn : RepairPhase

    /** RP03 — 한 번 보냈다. 닫아도 서버 요청이 취소되지 않는다 */
    data class Sending(val quote: RepairQuote, val balance: Double) : RepairPhase

    /**
     * RP08 — 결과를 확인하지 못했다. [accepted] 면 서버는 처리했다고 답했지만 최신 값을 아직 못 받았다.
     * [observed] 는 마지막으로 다시 확인한 실제 내구도(확인 전이면 null), [checking] 은 결과 확인(읽기) 중.
     */
    data class Unknown(
        val quote: RepairQuote,
        val balance: Double,
        val accepted: Boolean,
        val observed: Double? = null,
        val checking: Boolean = false,
    ) : RepairPhase

    /** RP04 — 서버의 성공과 내구도 · 잔고 동기화를 확인한 뒤에만 */
    data class Done(val quote: RepairQuote, val balance: Double, val durability: Double) : RepairPhase
}

/** 결과가 아직 정해지지 않은 수리 — 상세가 SD19(결과 확인 중)를 보이고 중복 수리 · 착용 · 강화를 막는다 */
val RepairPhase.pending: Boolean
    get() = this is RepairPhase.Sending || (this is RepairPhase.Unknown && (observed == null || accepted))

/**
 * 보내기 전 판정 — 최신으로 읽은 신발 · 잔고로 RP01 · RP05 · RP06 · RP09 · RP11 을 고른다.
 * 강화 가능 여부로 수리 가능을 정하지 않는다 — 서버(my_app_sneaker)가 보는 소유 · 판매 · 체인 상태만 본다.
 */
fun repairGate(shoe: Sneaker?, balance: Double?, serverEconomy: Boolean): RepairPhase {
    if (shoe == null) return RepairPhase.Blocked(CareBlock.MISSING)
    val server = shoe.server
    if (!serverEconomy || server == null) return RepairPhase.SignIn
    if (server.status != "OWNED") return RepairPhase.Blocked(CareBlock.LISTED)
    if (server.chainState.isNotBlank() && server.chainState != "APP") return RepairPhase.Blocked(CareBlock.ON_CHAIN)
    val quote = repairQuoteOf(shoe) ?: return RepairPhase.SignIn
    if (quote.points.signum() <= 0) return RepairPhase.NotNeeded(server.durabilityPts)
    if (balance == null) return RepairPhase.LoadFailed
    // 무료 수리(0116)는 잔고와 상관없이 가득 — 0 SUP
    if (quote.free) return RepairPhase.Confirm(quote, balance)
    if (repairShortfall(quote, balance).signum() > 0) return RepairPhase.Insufficient(quote, balance, partialRepairOf(quote, balance))
    return RepairPhase.Confirm(quote, balance)
}

/** 보낼 견적 — 확인(RP01 · RP10)이면 그 견적, 잔고 부족(RP06)이면 가진 만큼 수리. 그 밖에는 보내지 않는다(null) */
fun RepairPhase.sendable(): RepairQuote? = when (this) {
    is RepairPhase.Confirm -> quote
    is RepairPhase.Insufficient -> partial
    else -> null
}

/** 같은 장면 · 같은 견적인가 — 보내기 직전 다시 본 값이 사용자가 본 값과 같을 때만 보낸다 */
fun RepairPhase.sameQuoteAs(shown: RepairPhase): Boolean {
    val a = sendable() ?: return false
    val b = shown.sendable() ?: return false
    return this::class == shown::class && a.sameAs(b)
}

/** 바뀐 값으로 다시 확인받는 장면 — 확인 · 잔고 부족이면 "비용을 다시 확인했어요"를 붙인다 */
fun RepairPhase.markChanged(): RepairPhase = when (this) {
    is RepairPhase.Confirm -> copy(changed = true)
    is RepairPhase.Insufficient -> copy(changed = true)
    else -> this
}

// ── 강화 화면의 세 줄(레벨 · 효율 · 착화감) — 강화 자체는 ShoeUpgradeViewModel · ForgeRepository(0054) ────────

/** 강화 화면의 한 줄 — 레벨 · 효율 · 착화감(내구도는 강화로 바뀌지 않아 넣지 않는다) */
data class UpgradeRow(
    val stat: DetailStat,
    val now: String,
    /** 미리보기 · 결과의 다음 값. 실패 · 최대 · 결과 미확인 화면에서는 null(증가 구간을 그리지 않는다) */
    val next: String?,
    val nowFraction: Float,
    val nextFraction: Float?,
)

/**
 * 서버 값 [before] → [after] 의 세 줄. [after] 가 null 이면 지금 값만. 막대는 실제 값 ÷ 막대 끝 — 레벨은 서버가 준 상한,
 * 효율 · 착화감은 지수(경제 4판)이고 막대 끝은 그 등급에서 가장 잘 뽑힌 새 신발(내 신발 막대와 같다). 값 아래에 정규화 비율을 쓰지 않는다.
 * 레벨이 올라도 효율 · 착화감은 그대로라(레벨마다 포인트) 값이 같으면 화살표를 그리지 않는다. 받는 포인트는 [forgeRewards] 로 따로 보인다.
 */
fun forgeRows(
    before: ForgeStats,
    after: ForgeStats?,
    rarity: com.stepup.android.domain.Rarity = com.stepup.android.domain.Rarity.COMMON,
    rules: com.stepup.android.domain.ShoeRules = com.stepup.android.domain.ShoeRules.FALLBACK,
): List<UpgradeRow> {
    val max = (after ?: before).maxLevel.toDouble()
    val grade = rules.grade(rarity)
    val effEnd = com.stepup.android.domain.ShoeIndex.efficiency(grade.efficiencyHi)
    val comfortEnd = com.stepup.android.domain.ShoeIndex.comfort(grade.comfortHi)
    fun effIndex(s: ForgeStats) = com.stepup.android.domain.ShoeIndex.efficiency(s.efficiencyBps)
    fun comfortIndex(s: ForgeStats) = com.stepup.android.domain.ShoeIndex.comfort(s.comfortBps)
    fun level(s: ForgeStats) = com.stepup.android.ui.screens.customize.barFraction(s.level.toDouble(), max)
    fun eff(s: ForgeStats) = com.stepup.android.ui.screens.customize.barFraction(effIndex(s), effEnd)
    fun comfort(s: ForgeStats) = com.stepup.android.ui.screens.customize.barFraction(comfortIndex(s), comfortEnd)
    val effAfter = after?.takeIf { it.efficiencyBps != before.efficiencyBps }
    val comfortAfter = after?.takeIf { it.comfortBps != before.comfortBps }
    return listOf(
        UpgradeRow(DetailStat.LEVEL, before.level.toString(), after?.level?.toString(), level(before), after?.let(::level)),
        UpgradeRow(
            DetailStat.EFFICIENCY, formatIndex(effIndex(before)), effAfter?.let { formatIndex(effIndex(it)) },
            eff(before), effAfter?.let(::eff),
        ),
        UpgradeRow(
            DetailStat.COMFORT, formatIndex(comfortIndex(before)), comfortAfter?.let { formatIndex(comfortIndex(it)) },
            comfort(before), comfortAfter?.let(::comfort),
        ),
    )
}

/**
 * 강화 성공으로 받는 스탯 포인트 — 오른 레벨 · 등급만큼(0116 — 레벨마다 2 ~ 8, 5레벨마다 보너스). 서버가 포인트를 보내지 않으면(예전 서버)
 * 같은 표([rules])로 센다. 레벨이 오르지 않으면 0.
 */
fun forgePointsGain(
    before: ForgeStats,
    after: ForgeStats,
    rarity: com.stepup.android.domain.Rarity,
    rules: com.stepup.android.domain.ShoeRules = com.stepup.android.domain.ShoeRules.FALLBACK,
): Int {
    val reported = after.pointsTotal - before.pointsTotal
    if (reported > 0) return reported
    return ((before.level + 1)..after.level).sumOf { com.stepup.android.domain.StatPoints.gainAt(rarity, it, rules) }
}

/** 한 레벨이 주는 것(경제 4판, 0116) — 강화 화면이 "성공하면"에 한 줄씩 */
sealed interface ForgeReward {
    /** 스탯 포인트 — 등급마다(레벨마다 2 · 3 · 4 · 5 · 6 · 8, 5레벨마다 보너스까지) */
    data class Points(val count: Int) : ForgeReward

    /** 하루 에너지 +[amount] — 가진 신발 가운데 가장 높은 레벨이 에너지 최대치를 정한다 */
    data class Energy(val amount: Int) : ForgeReward

    /**
     * 5 · 10 · 15 …레벨 — 그 신발 내구도 가득 · 상급 상자 1개(계정마다 그 레벨에 처음 닿을 때).
     * [boxAlreadyGot] 이면 이 계정은 그 레벨의 상자를 이미 받았다(상자 현황에서 — 모르면 false)
     */
    data class Milestone(val level: Int, val boxAlreadyGot: Boolean) : ForgeReward
}

/** 레벨마다 하루 에너지 — 서버 economy.energy_max_for_level(레벨 1 에 10, 레벨마다 +2) */
const val ENERGY_PER_LEVEL = 2

/**
 * 레벨 [nextLevel] 에 닿으면 받는 것 — 포인트(등급 표) · 하루 에너지 +2 · (5의 배수면) 내구도 가득 · 상급 상자.
 * [levelsGot] 은 상자 현황(box_status)의 이미 상자를 받은 레벨들 — 모르면 null.
 */
fun forgeRewards(
    rarity: com.stepup.android.domain.Rarity,
    nextLevel: Int,
    rules: com.stepup.android.domain.ShoeRules = com.stepup.android.domain.ShoeRules.FALLBACK,
    levelsGot: List<Int>? = null,
): List<ForgeReward> = buildList {
    val points = rules.pointsAt(rarity.id, nextLevel)
    if (points > 0) add(ForgeReward.Points(points))
    if (nextLevel > 1) add(ForgeReward.Energy(ENERGY_PER_LEVEL))
    if (nextLevel % 5 == 0) add(ForgeReward.Milestone(nextLevel, boxAlreadyGot = levelsGot?.contains(nextLevel) == true))
}
