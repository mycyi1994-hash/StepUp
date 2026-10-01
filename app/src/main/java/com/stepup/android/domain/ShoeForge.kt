package com.stepup.android.domain

import java.util.Locale

/**
 * 신발 강화 — 하위 등급 신발 3개를 태워 확률로 레벨을 올린다(2026-10-01 사용자 지시서 v6, docs/redesign/shoe-upgrade-2026-10-01).
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

    /** 기본 성공률 = 100 − 3 × 현재 레벨(%) */
    fun basePermille(level: Int): Int = (1000 - 30 * level).coerceAtLeast(0)

    /**
     * 재료 한 개의 보정 = (2 + 0.4 × (재료 레벨 − 1)) × 등급 차이 계수(1단계 1 · 2단계 ½ · 3단계 ¼) %p.
     * 같거나 높은 등급 · 레벨 1 ~ 20 밖은 0 — 그런 재료는 서버가 받지 않는다.
     */
    fun bonusPermille(target: Rarity, material: Rarity, materialLevel: Int): Int {
        if (materialLevel !in 1..MAX_LEVEL) return 0
        val step = 20 + 4 * (materialLevel - 1)
        return when (target.ordinal - material.ordinal) {
            1 -> step
            2 -> step / 2
            3 -> step / 4
            else -> 0
        }
    }

    /** 최종 = min(100%, 기본 + 보정 합) */
    fun ratePermille(basePermille: Int, bonuses: List<Int>): Int = (basePermille + bonuses.sum()).coerceIn(0, 1000)

    /** "80.4%" */
    fun formatRate(permille: Int): String = String.format(Locale.ROOT, "%d.%d%%", permille / 10, permille % 10)

    /** "+5.6%p" */
    fun formatBonus(permille: Int): String = String.format(Locale.ROOT, "+%d.%d%%p", permille / 10, permille % 10)
}

/** 강화 대상이 될 수 없는 까닭(서버 forge_target_block) */
enum class ForgeTargetBlock {
    /** 내 신발이 아니다 · 없어졌다(18) */
    TARGET_GONE,

    /** 판매 중 · 체인에 나가 있다(18) */
    TARGET_UNAVAILABLE,

    /** 폰에서 올린 예전 신발 — 서버가 레벨을 믿지 않는다(18) */
    LEGACY,

    /** 아래 등급이 없는 일반 신발(17) */
    LOWEST_GRADE,

    /** 상한에 닿았다(13) */
    MAX_LEVEL,
    ;

    companion object {
        fun of(code: String?): ForgeTargetBlock? = entries.firstOrNull { it.name == code }
    }
}

/** 재료가 될 수 없는 까닭(서버 forge_material_block) */
enum class ForgeMaterialBlock {
    GONE, TARGET, GRADE, LISTED, EQUIPPED, LOCKED, ON_CHAIN, LEVEL, UNKNOWN;

    companion object {
        fun of(code: String?): ForgeMaterialBlock? = code?.let { c -> entries.firstOrNull { it.name == c } ?: UNKNOWN }
    }
}

/** 대상의 능력치 한 벌 — 효율 · 착화감은 서버 실효 값(bps) */
data class ForgeStats(val level: Int, val maxLevel: Int, val efficiencyBps: Int, val comfortBps: Int) {
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
     * 성공 시 값의 미리보기 — 서버 실효 계산(economy.sneaker_effective)과 같은 규칙: 레벨 +1, 효율 +0.5%p,
     * 착화감 +0.2%p(20% 상한). 3개를 고르면 서버 견적의 값으로 바꿔 쓴다.
     */
    fun previewNext(): ForgeStats = stats.copy(
        level = stats.level + 1,
        efficiencyBps = stats.efficiencyBps + 50,
        comfortBps = (stats.comfortBps + 20).coerceAtMost(maxOf(2000, stats.comfortBps)),
    )
}

/** 재료 후보 한 켤레. [shoe] 의 id 가 소유 id(서버 신발 번호)다 — 도감 모델 번호 · 목록 순번으로 고르지 않는다 */
data class ForgeMaterial(val shoe: Sneaker, val bonusPermille: Int, val block: ForgeMaterialBlock?) {
    val id: Long get() = shoe.id
    val usable: Boolean get() = block == null
}

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

/** 서버에 보낸 강화 요청 — 보내기 전에 저장해 앱을 다시 켜도 같은 요청의 결과만 묻는다 */
data class ForgePending(
    val requestKey: String,
    val targetId: Long,
    val materialIds: List<Long>,
    val ratePermille: Int,
    val createdAt: Long,
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
