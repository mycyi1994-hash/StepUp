package com.stepup.android.ui.screens.items

import com.stepup.android.data.repo.EconomyOutcome
import com.stepup.android.domain.Sneaker
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/*
 * 신발 상세 · 수리 · 강화(파란 톤 v4 전달본, docs/redesign/blue-v4-2026-10 — 상세·수리 30 · 강화 17)가 쓰는 순수 계산.
 *
 * 경제는 지금 서버 계약 그대로다 — 수리는 sneaker_repair(p_id), 강화는 sneaker_upgrade(p_id)(SUP 를 내고 레벨 +1).
 * 전달본의 "하위 등급 재료 3개 · 최대 Lv20 · 기본 확률 100 − 3L · 실패해도 재료 소각"은 서버에 없는 계약이라 화면이 흉내 내지 않는다
 * (재료 고르기 · 성공률 · 소각을 폰에서 정하지 않는다). 차이는 tracker/upgrade.csv 에 적었다.
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

/**
 * 수리 견적 — 서버 sneaker_repair 와 같은 계산(0023): 수리 포인트 = round(100 − 실제 내구도, 2),
 * 비용 = round(포인트 × 단가, 4). 단가는 서버가 준 값(my_sneakers.repair_cost_per_point)을 그대로 쓴다.
 * 표시용으로 내린 내구도(99)가 아니라 원래 값(99.6)으로 계산한다. 실제 차감은 서버가 다시 계산한다 — 이 값은 확인용이다.
 */
data class RepairQuote(
    val shoeId: Long,
    val level: Int,
    /** 서버의 실제 내구도(소수) */
    val durability: Double,
    val points: BigDecimal,
    val perPoint: BigDecimal,
    val cost: BigDecimal,
) {
    /** 같은 견적인가 — 레벨 · 내구도 · 단가 · 비용이 그대로면 같다(RP10 은 다를 때만) */
    fun sameAs(other: RepairQuote): Boolean =
        shoeId == other.shoeId && level == other.level && points.compareTo(other.points) == 0 &&
            perPoint.compareTo(other.perPoint) == 0 && cost.compareTo(other.cost) == 0
}

/** 서버 신발이 아니면(폰에만 있는 예전 신발) null — 수리는 서버만 한다 */
fun repairQuoteOf(shoe: Sneaker): RepairQuote? {
    val server = shoe.server ?: return null
    val points = BigDecimal.valueOf(100.0).subtract(BigDecimal.valueOf(server.durabilityPts))
        .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
    val perPoint = BigDecimal.valueOf(server.repairCostPerPoint).setScale(LEDGER_SCALE, RoundingMode.HALF_UP)
    val cost = points.multiply(perPoint).setScale(LEDGER_SCALE, RoundingMode.HALF_UP)
    return RepairQuote(shoe.id, shoe.level, server.durabilityPts, points, perPoint, cost)
}

/** 부족한 금액 — max(비용 − 잔고, 0) */
fun repairShortfall(quote: RepairQuote, balance: Double): BigDecimal =
    quote.cost.subtract(balance.ledger()).max(BigDecimal.ZERO)

/** 수리 후 예상 잔고 — 아직 완료된 잔고가 아니다(처리 중 · 확인 화면에서만) */
fun balanceAfter(balance: Double, cost: BigDecimal): Double = balance.ledger().subtract(cost).toDouble()

/** 수리할 수 없는 확인된 까닭(RP09) — 네트워크 오류는 여기에 넣지 않는다(그건 RP07 · RP08) */
enum class CareBlock {
    /** 판매 중(서버 status ≠ OWNED) */
    LISTED,

    /** 체인에 나가 있다(chain_state ≠ APP) */
    ON_CHAIN,

    /** 폰에서 올린 예전 신발 — 서버가 강화를 받지 않는다(수리는 받는다) */
    LEGACY,

    /** 성공한 조회에 이 소유 id 가 없다 */
    MISSING,

    /** 서버가 거절했는데 까닭을 모른다 */
    OTHER,
}

/** 서버 거절 문구 → 확인된 까닭(0023 economy.my_app_sneaker · sneaker_upgrade 의 문구) */
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

    /** RP06 — 잔고 부족. 수리 버튼은 누를 수 없다 */
    data class Insufficient(val quote: RepairQuote, val balance: Double) : RepairPhase

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
 * 강화 가능(upgradable)으로 수리 가능을 정하지 않는다 — 서버(my_app_sneaker)가 보는 소유 · 판매 · 체인 상태만 본다.
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
    if (repairShortfall(quote, balance).signum() > 0) return RepairPhase.Insufficient(quote, balance)
    return RepairPhase.Confirm(quote, balance)
}

// ── 강화(지금 서버 계약: SUP 를 내고 레벨 +1) ─────────────────────────

/** 레벨 한 칸에 오르는 효율 · 착화감 — 서버 economy.efficiency_per_level_bps · comfort_per_level_bps · comfort_cap_bps(0022) */
private const val EFFICIENCY_PER_LEVEL_BPS = 50
private const val COMFORT_PER_LEVEL_BPS = 20
private const val COMFORT_CAP_BPS = 2_000

/** 강화할 수 없는 까닭(UP13 · UP18). null 이면 지금 계약으로 강화할 수 있다 */
fun upgradeBlockOf(shoe: Sneaker?): CareBlock? {
    if (shoe == null) return CareBlock.MISSING
    val server = shoe.server
    return when {
        server != null && server.status != "OWNED" -> CareBlock.LISTED
        server != null && server.chainState.isNotBlank() && server.chainState != "APP" -> CareBlock.ON_CHAIN
        server != null && !server.upgradable -> CareBlock.LEGACY
        else -> null
    }
}

/** 최대 레벨(서버가 준 상한 우선, 없으면 등급 기본값) — 화면 분모만 20 으로 바꾸지 않는다 */
fun atMaxLevel(shoe: Sneaker): Boolean = shoe.level >= shoe.maxLevel

/**
 * 강화 뒤의 값을 미리 본다(화면 미리보기 전용 — 저장하지 않는다). 서버 신발은 실효 스탯에 레벨 한 칸 몫을 더하고(서버 sneaker_effective),
 * 예전 신발은 폰 강화(SneakerRepository.upgrade)와 같게. 실제 결과는 서버가 돌려준 값으로 다시 그린다.
 */
fun upgradePreview(shoe: Sneaker): Sneaker {
    val server = shoe.server
    return if (server != null) {
        shoe.copy(
            level = shoe.level + 1,
            server = server.copy(
                efficiencyBps = server.efficiencyBps + EFFICIENCY_PER_LEVEL_BPS,
                comfortBps = minOf(server.comfortBps + COMFORT_PER_LEVEL_BPS, maxOf(COMFORT_CAP_BPS, server.comfortBps)),
            ),
        )
    } else {
        shoe.copy(level = shoe.level + 1, comfort = shoe.comfort + 0.02, luck = shoe.luck + 0.02)
    }
}

/** 강화 화면의 한 줄 — 레벨 · 효율 · 착화감(내구도는 강화로 바뀌지 않아 넣지 않는다) */
data class UpgradeRow(
    val stat: DetailStat,
    val now: String,
    /** 미리보기 · 결과의 다음 값. 실패 · 최대 · 결과 미확인 화면에서는 null(증가 구간을 그리지 않는다) */
    val next: String?,
    val nowFraction: Float,
    val nextFraction: Float?,
)

/** [before] → [after] 의 세 줄. [after] 가 null 이면 지금 값만 */
fun upgradeRows(before: Sneaker, after: Sneaker?): List<UpgradeRow> {
    val max = before.maxLevel.toDouble()
    fun level(s: Sneaker) = com.stepup.android.ui.screens.customize.barFraction(s.level.toDouble(), max)
    fun eff(s: Sneaker) = com.stepup.android.ui.screens.customize.barFraction(
        bonusPercent(s), com.stepup.android.ui.screens.customize.StatScale.EFFICIENCY_MAX_PERCENT,
    )
    fun comfort(s: Sneaker) = com.stepup.android.ui.screens.customize.barFraction(
        energySavingPercent(s), com.stepup.android.ui.screens.customize.StatScale.ENERGY_MAX_PERCENT,
    )
    return listOf(
        UpgradeRow(DetailStat.LEVEL, before.level.toString(), after?.level?.toString(), level(before), after?.let(::level)),
        UpgradeRow(
            DetailStat.EFFICIENCY, formatBonus(bonusPercent(before)), after?.let { formatBonus(bonusPercent(it)) },
            eff(before), after?.let(::eff),
        ),
        UpgradeRow(
            DetailStat.COMFORT, formatPercent(energySavingPercent(before)), after?.let { formatPercent(energySavingPercent(it)) },
            comfort(before), after?.let(::comfort),
        ),
    )
}

/** 강화 요청의 진행 · 결말 — UP05 · UP06 · UP13 · UP15 · UP18 과 사전 거절 */
sealed interface UpgradePhase {
    /** UP05 — 보냈다. 타이머로 결과를 만들지 않는다 */
    data class Sending(val before: Sneaker) : UpgradePhase

    /** UP06 — 서버가 확정하고 새 레벨을 받은 뒤. [observed] 면 결과 다시 확인(읽기)으로 바뀐 레벨을 본 것 */
    data class Success(val before: Sneaker, val after: Sneaker, val observed: Boolean = false) : UpgradePhase

    /**
     * UP15 — 보냈는지 · 처리됐는지 모른다. 새 강화를 보내지 않고 같은 결과만 읽는다.
     * [accepted] 면 서버는 처리했다고 답했지만 최신 레벨을 아직 못 받았다. [synced] 면 다시 읽었는데 레벨이 그대로였다.
     */
    data class Unknown(
        val before: Sneaker,
        val accepted: Boolean,
        val checking: Boolean = false,
        val synced: Boolean = false,
    ) : UpgradePhase

    /** 서버가 받지 않았다(차감 없음) */
    data class Rejected(val reason: UpgradeRejection) : UpgradePhase
}

enum class UpgradeRejection { NOT_ENOUGH_BALANCE, MAX_LEVEL, SIGN_IN, LISTED, ON_CHAIN, LEGACY, MISSING, OTHER }

val UpgradePhase.pending: Boolean
    get() = this is UpgradePhase.Sending || (this is UpgradePhase.Unknown && (!synced || accepted))

/** 서버의 거절 결말 → 화면 까닭 */
fun upgradeRejectionOf(outcome: EconomyOutcome): UpgradeRejection = when (outcome) {
    EconomyOutcome.NotEnoughBalance -> UpgradeRejection.NOT_ENOUGH_BALANCE
    EconomyOutcome.MaxLevel -> UpgradeRejection.MAX_LEVEL
    EconomyOutcome.SignInRequired -> UpgradeRejection.SIGN_IN
    is EconomyOutcome.Rejected -> when (careBlockOf(outcome.reason)) {
        CareBlock.LISTED -> UpgradeRejection.LISTED
        CareBlock.ON_CHAIN -> UpgradeRejection.ON_CHAIN
        CareBlock.LEGACY -> UpgradeRejection.LEGACY
        CareBlock.MISSING -> UpgradeRejection.MISSING
        CareBlock.OTHER -> UpgradeRejection.OTHER
    }
    else -> UpgradeRejection.OTHER
}
