package com.stepup.android.domain

import java.util.Locale

/**
 * 신발 강화 — 2026-11-19 부터(서버 0118, 앱 30) 경험치 강화가 기본이다: 재료 1 ~ 10켤레를 넣어 통을 채우고 실패가 없다([ForgeXp] ·
 * docs/forge-xp-daily-spec-2026-11-19.md 2장). 아래 [ShoeForge] 는 예전 확률 강화(재료 3켤레) — 서버에 0118 이 없을 때와,
 * 1.20.x 에서 보내 놓고 결과를 아직 모르는 요청을 읽을 때만 쓴다.
 *
 * 예전 확률 강화 — 내 신발 3개를 태워 확률로 레벨을 올린다(2026-10-01 지시서 v6, docs/redesign/shoe-upgrade-2026-10-01 ·
 * 파란 톤 v4 강화 전달본). 재료 규칙은 2026-10-04 사용자 결정(서버 규칙 forge-v2):
 * 재료는 대상보다 **한 등급 아래 · 같은 등급 · 더 높은 등급**이면 된다. 두 등급 이상 아래는 받지 않는다.
 *
 * 판정 · 소각은 서버가 한다(supabase/migrations/0054_shoe_forge.sql). 여기의 식은 서버와 같은 식이고, 화면은 서버가 준
 * 기본 확률 · 재료별 보정을 더해 0 ~ 2개 선택의 미리보기를 보여 줄 때만 쓴다. 3개를 고르면 서버 견적을 그대로 쓴다.
 * 확률은 천분율 정수다 — 80.4% 는 804. 화면에 보이는 값과 서버가 판정하는 값이 같다.
 */
object ShoeForge {
    /** 강화 상한 — 금고(v3) 신발은 서버가 더 낮은 상한을 줄 수 있다(그 값을 쓴다) */
    const val MAX_LEVEL = 20

    /** 재료 신발 수 */
    const val MATERIALS = 3

    /** 기본 성공률 = max(0, 1000 − 30 × 현재 레벨)(천분율) */
    fun basePermille(level: Int): Int = (1000 - 30 * level).coerceAtLeast(0)

    /** 등급 차이 = 재료 등급 − 대상 등급(COMMON < RARE < EPIC < LEGENDARY). −1 이면 한 등급 아래 */
    fun gradeDiff(target: Rarity, material: Rarity): Int = material.ordinal - target.ordinal

    /** 재료가 될 수 있는 등급인가 — 한 등급 아래 이상 */
    fun gradeAllowed(target: Rarity, material: Rarity): Boolean = gradeDiff(target, material) >= -1

    /**
     * 등급 차이별 계수 k(보정 = step × k / 2) — −1 → 2 · 0 → 3 · +1 → 4 · +2 → 5 · +3 이상 → 6.
     * 두 등급 이상 아래는 null(재료가 아니다).
     */
    fun gradeFactor(diff: Int): Int? = when {
        diff <= -2 -> null
        diff == -1 -> 2
        diff == 0 -> 3
        diff == 1 -> 4
        diff == 2 -> 5
        else -> 6
    }

    /** step = 20 + 4 × (min(L, 20) − 1). 레벨 20 을 넘은 재료는 20 으로 센다(막지 않는다). 레벨 1 미만이면 null */
    fun step(materialLevel: Int): Int? =
        if (materialLevel < 1) null else 20 + 4 * (minOf(materialLevel, MAX_LEVEL) - 1)

    /**
     * 재료 한 개의 보정(천분율) = step × k / 2 — step 이 늘 짝수라 정수로 나뉜다.
     * 두 등급 이상 아래 · 레벨 1 미만은 0 — 그런 재료는 서버가 받지 않는다.
     */
    fun bonusPermille(target: Rarity, material: Rarity, materialLevel: Int): Int {
        val s = step(materialLevel) ?: return 0
        val k = gradeFactor(gradeDiff(target, material)) ?: return 0
        return s * k / 2
    }

    /** 최종 = min(1000, 기본 + 보정 합) */
    fun ratePermille(basePermille: Int, bonuses: List<Int>): Int = (basePermille + bonuses.sum()).coerceIn(0, 1000)

    /** "80.4%" */
    fun formatRate(permille: Int): String = String.format(Locale.ROOT, "%d.%d%%", permille / 10, permille % 10)

    /** "+5.6%p" */
    fun formatBonus(permille: Int): String = String.format(Locale.ROOT, "+%d.%d%%p", permille / 10, permille % 10)
}

/** 강화 대상이 될 수 없는 까닭(서버 forge_target_block). 일반 등급도 강화할 수 있어 "아래 등급 없음"은 없다(forge-v2) */
enum class ForgeTargetBlock {
    /** 내 신발이 아니다 · 없어졌다(UP18) */
    TARGET_GONE,

    /** 판매 중 · 체인에 나가 있다(UP18) */
    TARGET_UNAVAILABLE,

    /** 폰에서 올린 예전 신발 — 서버가 레벨을 믿지 않는다(UP18) */
    LEGACY,

    /** 상한에 닿았다(UP13) */
    MAX_LEVEL,

    /** 내가 크루 러닝 중이다(0118 경험치 강화 — 대상도 재료도 쓸 수 없다) */
    RUNNING,
    ;

    companion object {
        fun of(code: String?): ForgeTargetBlock? = entries.firstOrNull { it.name == code }
    }
}

/**
 * 재료가 될 수 없는 까닭. 서버 값: 예전 forge_material_block(GRADE 는 두 등급 이상 아래, WORN 은 내구도가 설정
 * forge_material_min_durability 보다 낮다 — 0118 부터 예전 길은 가득만) · 0118 forge_xp_block(IN_ROOM 은 대기실에서 고른 · 끝나지 않은
 * 크루 러닝의 신발, NO_VALUE 는 닳아서 값이 0).
 * 앱만 아는 값 둘 — 서버는 모른다(spec 2.5): [PENDING_RUN] 아직 올리지 않은 러닝에 신은 신발, [UNREVEALED] 아직 열어 보지 않은 뽑기 신발.
 */
enum class ForgeMaterialBlock {
    GONE, TARGET, GRADE, LISTED, EQUIPPED, LOCKED, ON_CHAIN, WORN, LEVEL, IN_ROOM, NO_VALUE,
    PENDING_RUN, UNREVEALED, UNKNOWN;

    /** 서버가 보내지 않는 값(앱이 붙인다) */
    val appOnly: Boolean get() = this == PENDING_RUN || this == UNREVEALED

    companion object {
        /** 서버 글자 → 까닭. 앱만 아는 값은 서버 글자로 받지 않는다(UNKNOWN) */
        fun of(code: String?): ForgeMaterialBlock? =
            code?.let { c -> entries.firstOrNull { it.name == c && !it.appOnly } ?: UNKNOWN }
    }
}

/**
 * 대상의 능력치 한 벌 — 효율 · 착화감은 서버 실효 값(bps, 레벨 1 값 + 나눈 포인트). [pointsTotal] · [pointsFree] 는 이 레벨까지
 * 받은 · 남은 스탯 포인트(0066 — 등급 · 레벨마다 다르다). 예전 서버는 포인트를 보내지 않는다(0).
 */
data class ForgeStats(
    val level: Int,
    val maxLevel: Int,
    val efficiencyBps: Int,
    val comfortBps: Int,
    val pointsTotal: Int = 0,
    val pointsFree: Int = 0,
) {
    val efficiencyPercent: Double get() = efficiencyBps / 100.0
    val comfortPercent: Double get() = comfortBps / 100.0
}

/** 강화 화면을 열 때 서버가 준 대상 상태 */
data class ForgeTarget(
    val id: Long,
    val rarity: Rarity,
    val stats: ForgeStats,
    val basePermille: Int,
    val block: ForgeTargetBlock?,
) {
    /**
     * 성공 시 값의 미리보기 — 서버 규칙(0116)과 같다: 레벨 +1, 스탯 포인트는 그 레벨 · 등급만큼([ShoeRules.pointsAt] — 5의 배수 레벨이면
     * 보너스까지). 효율 · 착화감은 저절로 오르지 않는다(받은 포인트를 사용자가 나눈다). 3개를 고르면 서버 견적의 값으로 바꿔 쓴다.
     */
    fun previewNext(rules: ShoeRules = ShoeRules.FALLBACK): ForgeStats = stats.copy(
        level = stats.level + 1,
        pointsTotal = stats.pointsTotal + StatPoints.gainAt(rarity, stats.level + 1, rules),
        pointsFree = stats.pointsFree + StatPoints.gainAt(rarity, stats.level + 1, rules),
    )
}

/** 재료 후보 한 켤레. [shoe] 의 id 가 소유 id(서버 신발 번호)다 — 도감 모델 번호 · 목록 순번으로 고르지 않는다 */
data class ForgeMaterial(val shoe: Sneaker, val bonusPermille: Int, val block: ForgeMaterialBlock?) {
    val id: Long get() = shoe.id
    val usable: Boolean get() = block == null
}

/** 선택창 목록 순서 — 쓸 수 있는 것 먼저, 그 안에서는 보정이 큰 것부터(서버도 같은 순서로 준다) */
fun List<ForgeMaterial>.forPicker(): List<ForgeMaterial> =
    sortedWith(compareBy<ForgeMaterial> { !it.usable }.thenByDescending { it.bonusPermille })

/** 서버 견적 — 이 확률로 판정한다 */
data class ForgeQuote(
    val ratePermille: Int,
    val before: ForgeStats,
    val after: ForgeStats,
    val quoteVersion: String,
    val materialIds: List<Long>,
)

/** 확정된 한 번 */
data class ForgeResult(
    val success: Boolean,
    val targetId: Long,
    val materialIds: List<Long>,
    val ratePermille: Int,
    val levelBefore: Int,
    val levelAfter: Int,
    /** 결과 뒤 대상의 실제 값. 대상을 읽지 못하면 null */
    val target: ForgeStats?,
)

/** 강화 요청의 종류 — 예전 확률 강화(0054 forge_start)와 경험치 강화(0118 forge_xp_add) */
enum class ForgeKind { CHANCE, XP }

/**
 * 서버에 보낸 강화 요청 — 보내기 전에 저장해 앱을 다시 켜도 같은 요청의 결과만 묻는다. [kind] 로 어느 함수에 물을지 가른다 —
 * 1.20.x 에서 보낸 예전 요청(종류 없는 줄)은 [ForgeKind.CHANCE] 로 읽어 예전 결과(성공 · 실패)를 그대로 보인다.
 * [quoteVersion] 은 경험치 강화의 견적 서명 — 응답을 잃고 서버에 결과가 없으면 같은 요청 번호로 한 번 더 보낼 때 쓴다(같은 번호는 한 번만 된다).
 */
data class ForgePending(
    val requestKey: String,
    val targetId: Long,
    val materialIds: List<Long>,
    val ratePermille: Int,
    val createdAt: Long,
    val kind: ForgeKind = ForgeKind.CHANCE,
    val quoteVersion: String = "",
)

/**
 * 재료 선택 — 메인 슬롯에 적용된 소유 id 와, 선택창에서 고치는 중인 임시 선택.
 * 선택창의 변경은 [apply] 전에는 메인에 닿지 않는다. 3개를 넘게 담지 않는다 — 넷째를 누르면 그대로다(몰래 바꾸지 않는다).
 */
data class MaterialSelection(val applied: List<Long> = emptyList(), val draft: List<Long>? = null) {
    val open: Boolean get() = draft != null

    /** 선택창을 연다 — 지금 적용된 선택에서 시작 */
    fun openPicker(): MaterialSelection = copy(draft = applied)

    /** 선택창의 한 줄을 누른다. 이미 고른 줄이면 빼고, 3개 미만이면 더한다. 3개면 아무것도 바꾸지 않는다 */
    fun toggle(id: Long): MaterialSelection {
        val current = draft ?: return this
        return when {
            id in current -> copy(draft = current - id)
            current.size >= ShoeForge.MATERIALS -> this
            else -> copy(draft = current + id)
        }
    }

    /** "선택한 n개 넣기" — 0개면 넣지 않는다(메인 슬롯의 X 로 비운다) */
    fun apply(): MaterialSelection {
        val current = draft ?: return this
        if (current.isEmpty()) return this
        return MaterialSelection(applied = current, draft = null)
    }

    /** X · 뒤로 · 아래로 닫기 · 바깥 — 임시 변경을 버린다 */
    fun cancel(): MaterialSelection = copy(draft = null)

    /** 메인 슬롯의 X */
    fun remove(id: Long): MaterialSelection = copy(applied = applied - id)

    /** 실제로 쓸 수 없다고 확인된 것만 뺀다(네트워크 오류로는 부르지 않는다) */
    fun drop(ids: Collection<Long>): MaterialSelection =
        MaterialSelection(applied = applied.filterNot { it in ids }, draft = draft?.filterNot { it in ids })

    fun clear(): MaterialSelection = MaterialSelection()
}

// ─────────────────────────────────────────────────────────────
// 경험치 강화(서버 0118 forge_xp_* · 규칙 forge-xp-1, docs/forge-xp-daily-spec-2026-11-19.md 2장)
// ─────────────────────────────────────────────────────────────

/**
 * 경험치 강화 — 재료 1 ~ [DEFAULT_MAX_MATERIALS] 켤레를 태워 대상의 통을 채운다. 실패가 없고, 통이 차면 레벨 + 1 · 남은 점수는 다음 통으로.
 * 재료 값 · 통 크기 · 대성공은 모두 서버가 정한다 — 앱은 서버가 준 점수(1켤레값 = 1,000점)와 %만 그린다.
 * %는 서버와 같은 식 `floor(xp × 1000 ÷ need) ÷ 10`(소수 한 자리, 버림). 통이 차면 서버가 바로 레벨을 올리므로 레벨이 오르지 않은 막대는
 * 99.9% 를 넘기지 않는다(100.0% 인데 레벨업이 아닌 일은 없다).
 */
object ForgeXp {
    const val RULE_VERSION = "forge-xp-1"

    /** 한 번에 넣는 재료 상한 — 서버가 `max_materials` 를 주면 그 값 */
    const val DEFAULT_MAX_MATERIALS = 10

    /** 0.1% 단위 정수(397 = 39.7%) — `floor(points × 1000 ÷ need)`. 통이 없으면(만렙) null */
    fun tenths(points: Int, need: Int?): Int? {
        if (need == null || need <= 0) return null
        return (points.coerceAtLeast(0).toLong() * 1000 / need).toInt()
    }

    /** 서버가 준 % 값(39.7) → 0.1% 단위 정수. 서버 값은 이미 버린 값이라 반올림은 소수 표기 오차만 지운다 */
    fun tenthsOf(pct: Double?): Int? = pct?.takeIf { it.isFinite() && it >= 0 }?.let { Math.round(it * 10).toInt() }

    /** 막대에 보일 값 — 레벨이 오르지 않았으면 99.9% 까지 */
    fun barTenths(tenths: Int, leveledUp: Boolean = false): Int =
        if (leveledUp) tenths.coerceIn(0, 1000) else tenths.coerceIn(0, 999)

    /** "39.7%" */
    fun formatPct(tenths: Int): String = String.format(Locale.ROOT, "%d.%d%%", tenths / 10, tenths % 10)

    /** "+34.7%" */
    fun formatGain(tenths: Int): String = "+" + formatPct(tenths)

    /** 대성공 확률(0.10) → "10%" — 서버 값 그대로(소수 한 자리까지, 끝의 .0 은 뺀다) */
    fun formatChance(chance: Double): String {
        val tenths = Math.round(chance.coerceIn(0.0, 1.0) * 1000).toInt()
        return if (tenths % 10 == 0) "${tenths / 10}%" else String.format(Locale.ROOT, "%d.%d%%", tenths / 10, tenths % 10)
    }

    /** 대성공 배수(2.0) → "2" · 1.5 → "1.5" */
    fun formatMult(mult: Double): String =
        if (mult == Math.floor(mult)) mult.toLong().toString() else String.format(Locale.ROOT, "%.1f", mult)
}

/**
 * 한 신발의 통 — 지금 레벨 · 통에 든 점수 · 이 레벨의 통 크기(만렙이면 null) · %(0.1% 단위, 서버 값 — 없으면 같은 식으로 센다).
 * [shownTenths] 는 막대 · 글자에 쓰는 값이다(레벨이 오르지 않았으면 99.9% 까지).
 */
data class ForgeBar(val level: Int, val xp: Int, val need: Int?, val tenths: Int?) {
    val maxed: Boolean get() = need == null

    val shownTenths: Int? get() = (tenths ?: ForgeXp.tenths(xp, need))?.let { ForgeXp.barTenths(it) }

    /** 막대 채움 0 ~ 1 — 만렙이면 가득 */
    val fraction: Float get() = shownTenths?.let { it / 1000f } ?: 1f
}

/** 아까운 재료의 까닭(서버 `precious` + 이 폰만 아는 이름 붙인 신발) — 넣기 전에 한 번 더 묻는다(spec 2.6) */
enum class ForgePrecious {
    HIGHER_GRADE, LEVELED, STARS3, LAST_MODEL, SELL_UNLOCKED, TOP_LEVEL, TOP_EFFICIENCY,

    /** 이 폰에서 이름을 붙인 신발([ShoeNickname]) — 서버는 모른다 */
    NICKNAMED,

    /** 이 앱이 모르는 서버 까닭 — 그래도 묻는다 */
    OTHER;

    companion object {
        fun of(code: String): ForgePrecious =
            entries.firstOrNull { it.name == code && it != NICKNAMED && it != OTHER } ?: OTHER
    }
}

/** 경험치 강화의 대상 */
data class XpTarget(val id: Long, val rarity: Rarity, val maxLevel: Int, val bar: ForgeBar) {
    val level: Int get() = bar.level
}

/**
 * 재료 후보 한 켤레(forge_xp_materials 의 items 한 줄). [shoe] 의 id 는 서버 신발 번호다.
 * [value] 는 넣는 값(제 값 + 물려줌, 대성공 없이 — 점수), [tenths] 는 그 값이 대상 지금 통의 몇 %인지(서버 값).
 * [block] 은 서버 까닭, 앱이 아는 까닭(달린 뒤 안 올린 러닝 · 열어 보지 않은 뽑기)은 [withAppBlock] 으로 덧붙인다.
 */
data class XpMaterial(
    val shoe: Sneaker,
    val stars: Int?,
    val durabilityPct: Double?,
    val own: Int,
    val inherit: Int,
    val repairPart: Int,
    val value: Int,
    val tenths: Int,
    val block: ForgeMaterialBlock?,
    val precious: List<ForgePrecious>,
) {
    val id: Long get() = shoe.id
    val usable: Boolean get() = block == null && value > 0

    /** 아까운 까닭 — 서버 까닭 + 이 폰에서 이름을 붙였으면 [ForgePrecious.NICKNAMED] */
    fun preciousWith(nicknames: Map<Long, String>): List<ForgePrecious> =
        if (nicknames[id] != null && ForgePrecious.NICKNAMED !in precious) precious + ForgePrecious.NICKNAMED else precious

    /** 서버가 막지 않은 신발에만 앱의 까닭을 붙인다 — 서버 까닭이 먼저 */
    fun withAppBlock(app: ForgeMaterialBlock?): XpMaterial = if (block != null || app == null) this else copy(block = app)
}

/** 대성공 — 재료마다 [chance] 확률로 그 재료의 제 값 × [mult](서버 값) */
data class XpCrit(val chance: Double, val mult: Double)

/** 강화 화면을 열 때 서버가 준 것(forge_xp_materials) */
data class XpCatalog(
    val target: XpTarget,
    /** 막히지 않은 것(값 낮은 것부터) → 막힌 것 — 서버 순서 그대로 */
    val items: List<XpMaterial>,
    val crit: XpCrit?,
    val maxMaterials: Int,
    val ruleVersion: String,
) {
    fun item(id: Long): XpMaterial? = items.firstOrNull { it.id == id }

    /** 앱의 까닭(신발 번호 → 까닭)을 붙이고 목록 순서를 다시 맞춘다: 쓸 수 있는 것(값 낮은 것부터) → 막힌 것 */
    fun withAppBlocks(app: Map<Long, ForgeMaterialBlock>): XpCatalog {
        if (app.isEmpty()) return this
        val marked = items.map { it.withAppBlock(app[it.id]) }
        return copy(items = marked.filter { it.usable } + marked.filterNot { it.usable })
    }
}

/** 견적의 재료 한 줄 */
data class XpQuoteMaterial(val id: Long, val own: Int, val inherit: Int, val value: Int, val tenths: Int?)

/**
 * 서버 견적(forge_xp_quote) — [after] 는 대성공 없이 받는 **최소**다. 결과는 그 이상이다.
 * [energyBefore] · [energyAfter] 는 재료를 태운 뒤의 하루 에너지(가진 신발 중 가장 높은 레벨이 정한다), [capBefore] · [capAfter] 는 하루 적립 상한 배수.
 */
data class XpQuote(
    val quoteVersion: String,
    val before: ForgeBar,
    val after: ForgeBar,
    /** 오르는 레벨(최소) */
    val levels: Int,
    /** 만렙에 닿아 버려지는 점수(최소) */
    val overflow: Int,
    val gain: Int,
    val materials: List<XpQuoteMaterial>,
    val crit: XpCrit?,
    val energyBefore: Int?,
    val energyAfter: Int?,
    val capBefore: Double?,
    val capAfter: Double?,
    val maxLevel: Int,
    /** 만렙에 닿아 남는 경험치가 사라질 수 있다(대성공 때문에 정확한 양은 미리 모른다) */
    val overflowPossible: Boolean,
    val materialIds: List<Long>,
) {
    /** 이 견적이 지금 고른 재료의 것인가(순서는 상관없다) */
    fun covers(ids: Collection<Long>): Boolean = materialIds.size == ids.size && materialIds.toSet() == ids.toSet()
}

/** 넣은 재료 한 켤레의 결과 */
data class XpResultMaterial(val id: Long, val own: Int, val inherit: Int, val crit: Boolean, val value: Int)

/** 확정된 한 번(forge_xp_add · forge_xp_result) — 실패가 없다 */
data class XpResult(
    val targetId: Long,
    val levelBefore: Int,
    val levelAfter: Int,
    val levels: Int,
    val gain: Int,
    /** 넣은 뒤의 통(새 레벨) */
    val after: ForgeBar,
    val overflow: Int,
    val materials: List<XpResultMaterial>,
    val crits: Int,
    /** 이 강화로 받은 레벨 상자(5 · 10 …) */
    val levelBoxes: List<Int>,
) {
    val leveledUp: Boolean get() = levels > 0 || levelAfter > levelBefore
    val materialIds: List<Long> get() = materials.map { it.id }
}

/** 경험치 강화의 재료 선택 — 1 ~ [max] 켤레. 순서는 고른 차례 그대로. 상한이면 더 담지 않는다(몰래 바꾸지 않는다) */
data class XpSelection(val ids: List<Long> = emptyList(), val max: Int = ForgeXp.DEFAULT_MAX_MATERIALS) {
    val full: Boolean get() = ids.size >= max
    val isEmpty: Boolean get() = ids.isEmpty()

    fun toggle(id: Long): XpSelection = when {
        id in ids -> copy(ids = ids - id)
        full -> this
        else -> copy(ids = ids + id)
    }

    fun drop(gone: Collection<Long>): XpSelection = copy(ids = ids.filterNot { it in gone })

    fun clear(): XpSelection = copy(ids = emptyList())

    fun withMax(newMax: Int): XpSelection = copy(max = newMax.coerceAtLeast(1), ids = ids.take(newMax.coerceAtLeast(1)))
}

/**
 * "남는 신발 넣기"(spec 2.6) — 지금 고른 것은 두고, 아까운 신발 · 막힌 신발은 고르지 않고, 값이 낮은 것부터 통이 찰 때까지
 * [XpCatalog.maxMaterials] 켤레까지 더한다. 이미 통이 차면 그대로. 만렙(통 없음)이면 그대로.
 */
fun XpCatalog.bulkPick(current: List<Long>, nicknames: Map<Long, String> = emptyMap()): List<Long> {
    val need = target.bar.need ?: return current
    val chosen = current.toMutableList()
    var sum = target.bar.xp.toLong() + chosen.sumOf { id -> (item(id)?.value ?: 0).toLong() }
    if (sum >= need) return current
    val spare = items
        .filter { it.usable && it.id !in chosen && it.preciousWith(nicknames).isEmpty() }
        .sortedWith(compareBy<XpMaterial> { it.value }.thenBy { it.id })
    for (m in spare) {
        if (chosen.size >= maxMaterials) break
        chosen += m.id
        sum += m.value
        if (sum >= need) break
    }
    return chosen
}

/**
 * 견적이 오기 전의 미리보기 — 서버가 준 점수(대상 통 · 재료 값)만 더한다. 지금 통 안에서 끝나면 그대로 최소값이고([fills] = false),
 * 통이 차면 몇 레벨 오를지는 견적이 알려 준다([fills] = true — 레벨업 표시만).
 */
data class XpPreview(val gainTenths: Int, val afterTenths: Int, val fills: Boolean)

fun XpCatalog.preview(ids: Collection<Long>): XpPreview? {
    if (ids.isEmpty()) return null
    val need = target.bar.need ?: return null
    val gain = ids.sumOf { (item(it)?.value ?: 0).toLong() }.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    val total = target.bar.xp.toLong() + gain
    val fills = total >= need
    val after = ForgeXp.barTenths((total * 1000 / need).coerceAtMost(1000).toInt(), leveledUp = fills)
    return XpPreview(ForgeXp.tenths(gain, need) ?: 0, after, fills)
}

/** 고른 재료 가운데 아까운 것과 그 까닭 — 넣기 전 확인 창에 적는다. 없으면 빈 목록(바로 넣는다) */
fun XpCatalog.preciousPicked(ids: List<Long>, nicknames: Map<Long, String> = emptyMap()): List<Pair<XpMaterial, List<ForgePrecious>>> =
    ids.mapNotNull { id -> item(id)?.let { m -> m.preciousWith(nicknames).takeIf { it.isNotEmpty() }?.let { m to it } } }

/**
 * 뽑기 결과의 "재료로 쓰면 ○○ +34.7%" 한 줄(spec 5.1) — 서버가 준 그 재료의 % (대상 = 신은 신발). 띄우지 않는 경우는 null:
 * 신은 신발이 없거나 첫 신발(STARTER) · 만렙, 걸려 있는 강화가 있다, 뽑은 신발이 막혔거나 아까운 신발, 서버에 경험치 강화가 없다([catalog] null).
 */
fun drawForgeHintTenths(
    equipped: Sneaker?,
    drawnId: Long,
    catalog: XpCatalog?,
    forgePending: Boolean,
    nicknames: Map<Long, String> = emptyMap(),
): Int? {
    if (equipped == null || forgePending || catalog == null) return null
    val stats = equipped.server ?: return null
    if (stats.origin == "STARTER" || equipped.id == drawnId) return null
    if (catalog.target.id != equipped.id || catalog.target.bar.maxed) return null
    if (equipped.level >= stats.maxLevel && stats.maxLevel > 0) return null
    val item = catalog.item(drawnId) ?: return null
    if (!item.usable || item.preciousWith(nicknames).isNotEmpty()) return null
    return item.tenths.takeIf { it > 0 }
}
