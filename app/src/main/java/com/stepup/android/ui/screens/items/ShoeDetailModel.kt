package com.stepup.android.ui.screens.items

import com.stepup.android.data.repo.EconomyOutcome
import com.stepup.android.domain.Sneaker
import java.util.Locale

/*
 * 보유 신발 상세 v1(2026-09-28 전달본, docs/redesign/shoe-detail-v1)이 쓰는 순수 계산 — 신발 탭 목록 · 상세 · 착용 결말.
 *
 * 값은 모두 실제 신발([Sneaker])에서 온다. 시안의 예시 이름 · 번호(#0002) · 능력치를 만들지 않는다.
 * 고르기는 늘 소유 id 로 한다 — 목록 순서(인덱스)로 고르지 않는다.
 */

/** 보유 신발 읽기 — 읽는 중 · 읽지 못함 · 목록. 읽지 못한 것을 빈 목록으로 바꾸지 않는다(시안 13 · 14 · 18을 가른다) */
sealed interface OwnedLoad {
    data object Loading : OwnedLoad
    data object Failed : OwnedLoad
    data class Ready(val shoes: List<Sneaker>) : OwnedLoad
}

/** 상세 한 화면 — 13 조회 중 · 14 조회 실패 · 15 없는 신발 · 신발(02 · 03 · 08 · 17) */
sealed interface ShoeDetailState {
    data object Loading : ShoeDetailState
    data object Failed : ShoeDetailState
    data object NotFound : ShoeDetailState

    /** [wearing] 은 지금 실제로 신고 있는 켤레(없으면 null — 17) */
    data class Ready(val shoe: Sneaker, val wearing: Sneaker?) : ShoeDetailState
}

/** 읽기 결과에서 [id] 한 켤레의 상세 상태. 목록을 다 읽은 뒤에 없을 때만 "없는 신발"이다 */
fun detailStateOf(load: OwnedLoad, id: Long): ShoeDetailState = when (load) {
    OwnedLoad.Loading -> ShoeDetailState.Loading
    OwnedLoad.Failed -> ShoeDetailState.Failed
    is OwnedLoad.Ready -> load.shoes.firstOrNull { it.id == id }
        ?.let { ShoeDetailState.Ready(it, load.shoes.firstOrNull { shoe -> shoe.equipped }) }
        ?: ShoeDetailState.NotFound
}

// ── 능력치 ─────────────────────────────────────────────────────

enum class ShoeStat { BONUS, ENERGY, LUCK, COMFORT, DURABILITY }

data class ShoeStatValue(val stat: ShoeStat, val text: String)

/**
 * SUP 적립 보너스(%) — 기존 상세와 같은 출처. 서버 신발은 효율(efficiency_bps ÷ 100, 서버 적립 계산의 1 + 효율),
 * 폰에만 있던 예전 신발은 도감 값 + 레벨 한 칸에 0.5([Sneaker.boostPercent]). 화면에서 새로 계산하지 않는다.
 */
fun bonusPercent(shoe: Sneaker): Double = shoe.server?.let { it.efficiencyBps / 100.0 } ?: shoe.boostPercent

/** 에너지 절감(%) — 기존 계산 그대로 (1 − [Sneaker.energyEfficiency]) × 100. 예전 신발은 착화감 기준 최대 15% */
fun energySavingPercent(shoe: Sneaker): Double = (1.0 - shoe.energyEfficiency) * 100.0

/**
 * 능력치 시트(04)의 줄 — 이 신발에 실제로 있는 값만.
 * 서버 신발은 행운을 서버가 갖지 않고(폰의 1.00 은 자리 값) 착화감이 곧 에너지 절감 비율이라 두 줄을 뺀다 — 없는 값을 꾸미지 않는다.
 */
fun shoeStats(shoe: Sneaker): List<ShoeStatValue> = buildList {
    add(ShoeStatValue(ShoeStat.BONUS, formatBonus(bonusPercent(shoe))))
    add(ShoeStatValue(ShoeStat.ENERGY, formatPercent(energySavingPercent(shoe))))
    if (shoe.server == null) {
        add(ShoeStatValue(ShoeStat.LUCK, formatStat(shoe.luck)))
        add(ShoeStatValue(ShoeStat.COMFORT, formatStat(shoe.comfort)))
    }
    add(ShoeStatValue(ShoeStat.DURABILITY, formatDurability(shoe)))
}

/** 소수 둘째 자리까지, 끝의 0 은 한 자리만 남긴다 — 0.45 → "0.45", 7.5 → "7.5", 6 → "6.0" */
fun trimmedDecimal(value: Double): String {
    val two = String.format(Locale.ROOT, "%.2f", value + 0.0) // + 0.0 — "-0.00" 이 되지 않게
    return if (two.endsWith("0")) two.dropLast(1) else two
}

/** "+0.45%" — 배율(1.0045)이나 SUP 수량으로 읽히지 않게 늘 + 와 % 를 붙인다 */
fun formatBonus(percent: Double): String = "+" + trimmedDecimal(percent) + "%"

/** "7.5%" */
fun formatPercent(percent: Double): String = trimmedDecimal(percent) + "%"

/** 행운 · 착화감 — 도감 표처럼 두 자리("1.08") */
fun formatStat(value: Double): String = String.format(Locale.ROOT, "%.2f", value)

/** 내구도 점수(100 기준). 서버 값(소수)은 내려서 쓴다: 99.6 을 100 으로 올려 보이면 수리할 곳이 가려진다 */
fun durabilityPoints(shoe: Sneaker): Int =
    (shoe.server?.let { kotlin.math.floor(it.durabilityPts).toInt() } ?: shoe.durability).coerceAtLeast(0)

/** 내구도 "100 / 100" */
fun formatDurability(shoe: Sneaker): String = "${durabilityPoints(shoe)} / 100"

// ── 상세 본문의 네 줄(카툰 입체형, 2026-09-30 확정) ──────────────

enum class DetailStat { LEVEL, EFFICIENCY, COMFORT, DURABILITY }

/** 상세 본문 한 줄 — 보이는 값("5 / 15")과 막대 길이(0 ~ 1) */
data class DetailStatRow(val stat: DetailStat, val value: String, val fraction: Float)

/**
 * 레벨 · 효율 · 착화감 · 내구도 — 레벨은 [Sneaker.level] ÷ [Sneaker.maxLevel](서버의 양수 상한 우선, 없으면 등급 기본값),
 * 나머지 셋은 내 신발 막대와 같은 값 · 기준(statBars — 효율 27.5% · 착화감 20% · 내구도 100). 행운 · 원시 착화감은 넣지 않는다.
 */
fun detailStatRows(shoe: Sneaker): List<DetailStatRow> {
    val level = DetailStatRow(
        DetailStat.LEVEL, "${shoe.level} / ${shoe.maxLevel}",
        com.stepup.android.ui.screens.customize.barFraction(shoe.level.toDouble(), shoe.maxLevel.toDouble()),
    )
    return listOf(level) + com.stepup.android.ui.screens.customize.statBars(shoe).map { bar ->
        val stat = when (bar.stat) {
            com.stepup.android.ui.screens.customize.BarStat.EFFICIENCY -> DetailStat.EFFICIENCY
            com.stepup.android.ui.screens.customize.BarStat.COMFORT -> DetailStat.COMFORT
            com.stepup.android.ui.screens.customize.BarStat.DURABILITY -> DetailStat.DURABILITY
        }
        DetailStatRow(stat, bar.value, bar.fraction)
    }
}

/** 신발 번호 "No. 0007" — 소유 켤레의 민팅 번호. 네 자리보다 짧으면 앞을 0 으로, 길면 자르지 않는다 */
fun formatShoeNumber(mintNumber: Int): String = "No. " + String.format(Locale.ROOT, "%04d", mintNumber)

// ── 체인 ───────────────────────────────────────────────────────

/** 신발 정보 시트(06)의 체인 줄 — 서버가 준 토큰 번호만. 번호를 꾸미지 않는다 */
sealed interface ChainMark {
    /** 아직 체인에 없음(발행 대기일 수 있다 — 내 정보 › 지갑 › 체인 기록) */
    data object None : ChainMark

    /** v2 토큰(1~) */
    data class Token(val tokenId: Long) : ChainMark

    /** v3 금고 발행(1,000,001~ — contracts/StepUpSneakersV3.sol FIRST_TOKEN_ID) */
    data class Vault(val tokenId: Long) : ChainMark
}

const val VAULT_FIRST_TOKEN_ID = 1_000_001L

fun chainMarkOf(tokenId: Long?): ChainMark = when {
    tokenId == null || tokenId <= 0 -> ChainMark.None
    tokenId >= VAULT_FIRST_TOKEN_ID -> ChainMark.Vault(tokenId)
    else -> ChainMark.Token(tokenId)
}

// ── 보유 목록(같은 모델 묶음) ──────────────────────────────────

/**
 * 같은 모델(slotKey — 새 도감 "M:1101", 예전 "FIRE:EPIC:1") 켤레 묶음 — 보유 목록 한 칸.
 * [copies] 는 신발 번호 순. 대표는 신고 있는 켤레, 없으면 기존 보관함 규칙(레벨 · 번호가 가장 큰 켤레).
 */
data class OwnedGroup(val key: String, val representative: Sneaker, val copies: List<Sneaker>) {
    val count: Int get() = copies.size
    val worn: Sneaker? get() = copies.firstOrNull { it.equipped }
}

/** 기존 보관함(ItemsViewModel.groups)과 같은 대표 규칙 */
fun representativeOf(copies: List<Sneaker>): Sneaker =
    copies.firstOrNull { it.equipped } ?: copies.maxBy { it.level * 1000L + it.mintNumber }

/**
 * 보유 목록 칸 순서 — 등급 높은 순 → 최근에 받은 순 → 모델 키. 착용을 바꿔도 칸 순서가 바뀌지 않는다
 * (돌아왔을 때 가로 목록 자리가 그대로 맞게 — 시안 12).
 */
fun ownedGroups(shoes: List<Sneaker>): List<OwnedGroup> =
    shoes.groupBy { it.slotKey }
        .map { (key, copies) -> OwnedGroup(key, representativeOf(copies), copies.sortedWith(compareBy({ it.mintNumber }, { it.id }))) }
        .sortedWith(
            compareByDescending<OwnedGroup> { it.representative.rarity.ordinal }
                .thenByDescending { group -> group.copies.maxOf { it.acquiredAt } }
                .thenBy { it.key },
        )

/** 지금 보는 켤레 — 고른 id 가 목록에 있으면 그 켤레, 없으면 신고 있는 켤레, 그것도 없으면 첫 칸의 대표 */
fun resolveSelection(groups: List<OwnedGroup>, selectedId: Long?): Sneaker? {
    val all = groups.asSequence().flatMap { it.copies.asSequence() }
    return all.firstOrNull { it.id == selectedId } ?: all.firstOrNull { it.equipped } ?: groups.firstOrNull()?.representative
}

/** 칸을 누르면 — 그 모델의 다른 켤레를 이미 보고 있으면 그대로, 아니면 대표 켤레. 착용은 바꾸지 않는다 */
fun pickInGroup(group: OwnedGroup, currentId: Long?): Long =
    group.copies.firstOrNull { it.id == currentId }?.id ?: group.representative.id

// ── 착용 결말 ─────────────────────────────────────────────────

/** 착용이 바뀌지 않은(또는 확인하지 못한) 까닭 — 시트 문구만 다르다 */
enum class EquipFailure {
    /** 서버 · 폰이 거절했다 */
    REJECTED,

    /** 로그인이 필요하다 */
    SIGN_IN,

    /** 서버에 닿지 못했다 — 보냈는지 모른다 */
    OFFLINE,

    /** 서버는 받았다는데 이 휴대폰의 목록이 아직 바뀌지 않았다 */
    UNCONFIRMED,

    /** 보내기 전에 다시 보니 내 신발 목록에 없다(15) — 착용을 건드리지 않았다 */
    MISSING,
}

/** 착용 변경 한 번의 결말. 상세가 한 번만 보여 주고 지운다(다시 들어와도 되풀이하지 않는다) */
sealed interface EquipResult {
    val targetId: Long

    /** 저장된 실제 착용이 고른 켤레와 같다 — 이때만 "신었어요"(10) */
    data class Worn(override val targetId: Long) : EquipResult

    /**
     * 바뀌지 않았다(11). [keptId] 는 저장된 지금 착용(없으면 null). [confirmed] 면 "계속 신고 있어요"라고 말해도 된다 —
     * 아니면(연결 끊김 · 확인 안 됨) 단정하지 않고 다시 확인하게 한다.
     */
    data class NotWorn(
        override val targetId: Long,
        val reason: EquipFailure,
        val keptId: Long?,
        val confirmed: Boolean,
    ) : EquipResult
}

/**
 * 서버(또는 폰)의 대답 [outcome] 과 그 뒤 저장된 실제 착용 [storedWornId] 로 결말을 정한다.
 * 성공 신호만으로 "신었어요"라고 하지 않는다 — 저장된 착용이 [targetId] 일 때만.
 */
fun classifyEquip(targetId: Long, outcome: EconomyOutcome, storedWornId: Long?): EquipResult = when {
    storedWornId == targetId -> EquipResult.Worn(targetId)
    outcome == EconomyOutcome.Ok -> EquipResult.NotWorn(targetId, EquipFailure.UNCONFIRMED, storedWornId, confirmed = false)
    outcome == EconomyOutcome.Offline -> EquipResult.NotWorn(targetId, EquipFailure.OFFLINE, storedWornId, confirmed = false)
    outcome == EconomyOutcome.SignInRequired -> EquipResult.NotWorn(targetId, EquipFailure.SIGN_IN, storedWornId, confirmed = true)
    else -> EquipResult.NotWorn(targetId, EquipFailure.REJECTED, storedWornId, confirmed = true)
}

// ── 문구 도우미 ───────────────────────────────────────────────

/**
 * 한국어 목적격 조사를 붙인다 — "새벽 강변을", "클라우드 러너를". 마지막 글자에 받침이 있으면 "을", 없으면 "를".
 * 숫자는 읽는 소리로(0 · 1 · 3 · 6 · 7 · 8 은 "을"), 그 밖의 글자는 "을(를)". 한국어 화면에서만 쓴다.
 */
fun withObjectParticle(word: String): String {
    val trimmed = word.trimEnd()
    val last = trimmed.lastOrNull() ?: return word
    val particle = when {
        last in '가'..'힣' -> if ((last - '가') % 28 != 0) "을" else "를"
        last.isDigit() -> if (last in "013678") "을" else "를"
        else -> "을(를)"
    }
    return trimmed + particle
}
