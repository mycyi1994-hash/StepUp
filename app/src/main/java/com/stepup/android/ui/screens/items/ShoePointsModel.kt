package com.stepup.android.ui.screens.items

import com.stepup.android.data.repo.EconomyOutcome
import com.stepup.android.domain.PointsPreview
import com.stepup.android.domain.ShoeIndex
import com.stepup.android.domain.ShoeRules
import com.stepup.android.domain.Sneaker

/*
 * 스탯 포인트 나누기(서버 sneaker_points_set — 경제 4판 0116)가 쓰는 순수 계산.
 *
 * 레벨이 오를 때 받는 포인트는 등급마다(레벨마다 2 · 3 · 4 · 5 · 6 · 8, 5레벨마다 보너스 3 · 5 · 6 · 8 · 9 · 12). 포인트 1 은
 * 뽑은 값의 +1.0 · 1.1 · 1.25 · 1.4 · 1.6 · 1.8%(등급마다) — 효율 · 착화감(에너지 70% 절약까지) · 최대 내구도(남은 내구도도 같이) 중 하나다.
 * 계산은 서버와 같은 식([ShoeRules.efficiencyWithPoints] · [ShoeRules.comfortWithPoints] · [ShoeRules.durabilityWithPoints]).
 * 나눈 포인트는 되돌리지 않는다. [PointsBasis.respec] 인 신발만 한 번 처음부터 다시 나눌 수 있다(서버 0119 부터 5레벨마다 한 번 받는다).
 * 값은 모두 서버가 준 신발 값 · 규칙에서 온다 — 이 계산은 미리보기이고, 서버가 같은 규칙으로 다시 확인한다.
 *
 * 서버 0119(docs/economy-roots-contract-2026-11-21.md): 효율 포인트는 더 빨리 벌게 하고 그만큼 신발 · 에너지를 빨리 쓰게 한다 —
 * 가득에서 달리는 분([PointsBasis.lifeMinutes])이 효율로 줄고 내구도로 는다. 서버가 내 지난 14일 러닝으로 셈한 미리보기
 * ([PointsPreview] — "1포인트 = 하루 약 +N SUP" · 추천)가 있으면 추천대로 채울 수 있다([recommended] · [recommendedAll]).
 */

enum class PointStat { EFFICIENCY, COMFORT, DURABILITY }

/** 나눈 뒤의 합계 — 서버에 그대로 보낸다(더할 값이 아니다. 같은 요청이 두 번 가도 두 번 더해지지 않는다) */
data class PointsDraft(val efficiency: Int = 0, val comfort: Int = 0, val durability: Int = 0) {
    val sum: Int get() = efficiency + comfort + durability

    operator fun get(stat: PointStat): Int = when (stat) {
        PointStat.EFFICIENCY -> efficiency
        PointStat.COMFORT -> comfort
        PointStat.DURABILITY -> durability
    }

    fun with(stat: PointStat, value: Int): PointsDraft = when (stat) {
        PointStat.EFFICIENCY -> copy(efficiency = value)
        PointStat.COMFORT -> copy(comfort = value)
        PointStat.DURABILITY -> copy(durability = value)
    }
}

/** 시트가 시작하는 값 — 서버가 준 이 켤레의 받은 포인트 · 나눈 포인트 · 뽑은 값(포인트 전) · 내구도, 등급과 규칙 */
data class PointsBasis(
    val shoeId: Long,
    val total: Int,
    val saved: PointsDraft,
    val respec: Boolean,
    val efficiencyBaseBps: Int,
    val comfortBaseBps: Int,
    val durabilityBase: Int,
    val durabilityNow: Int,
    /** 서버 등급(COMMON …) — 포인트 1 의 폭이 등급마다 다르다 */
    val rarity: String = "COMMON",
    val rules: ShoeRules = ShoeRules.FALLBACK,
) {
    /** 포인트 1 이 올리는 폭(뽑은 값의 %) — 1.0 · 1.1 · 1.25 · 1.4 · 1.6 · 1.8 */
    val pointRatePercent: Double get() = rules.grade(rarity).pointRateBps / 100.0

    /** 착화감 상한(에너지 절약 bps) — 7000 = 에너지 70% 절약 */
    val comfortCapBps: Int get() = rules.comfortCapBps

    fun free(draft: PointsDraft): Int = total - draft.sum

    /** 줄일 수 있는 바닥 — 다시 나누기가 남았으면 0, 아니면 이미 나눈 값 */
    fun floor(stat: PointStat): Int = if (respec) 0 else saved[stat]

    fun canRemove(draft: PointsDraft, stat: PointStat): Boolean = draft[stat] > floor(stat)

    /** 남은 포인트가 있고, 착화감이면 상한(에너지 70% 절약)을 넘지 않을 때 — 서버 sneaker_points_set 과 같은 검사 */
    fun canAdd(draft: PointsDraft, stat: PointStat): Boolean =
        free(draft) > 0 && (stat != PointStat.COMFORT || comfortRaw(draft.comfort + 1) <= comfortCapBps)

    /** 착화감 포인트 [points] 를 서버가 받는가 — 이미 나눈 값까지는 그대로, 더 늘린 값은 상한(에너지 70% 절약)까지 */
    fun comfortFits(points: Int): Boolean = points <= saved.comfort || comfortRaw(points) <= comfortCapBps

    /** 착화감 포인트를 더한 에너지 절약 bps(상한 전) — 서버 economy.comfort_with_points */
    private fun comfortRaw(points: Int): Int = rules.comfortWithPoints(comfortBaseBps, rarity, points)

    /** 효율(적립 +bps) — 서버 economy.shoe_effective */
    fun efficiencyBps(draft: PointsDraft): Int = rules.efficiencyWithPoints(efficiencyBaseBps, rarity, draft.efficiency)
    fun comfortBps(draft: PointsDraft): Int = minOf(comfortRaw(draft.comfort), comfortCapBps)

    /** 지수로 — 화면이 보이는 값(효율 140 · 착화감 140) */
    fun efficiencyIndex(draft: PointsDraft): Double = ShoeIndex.efficiency(efficiencyBps(draft))
    fun comfortIndex(draft: PointsDraft): Double = ShoeIndex.comfort(comfortBps(draft))

    /** 최대 내구도 — 서버 economy.dur_max_of(뽑은 값 × (1 + 폭 × 포인트), 반올림) */
    fun durabilityMax(draft: PointsDraft): Int = rules.durabilityWithPoints(durabilityBase, rarity, draft.durability)

    /**
     * 내구도 포인트를 바꾼 뒤의 남은 내구도 — 서버 sneaker_points_set 과 같다. 0119 부터는 남은 비율을 지킨다(가득이면 가득 · 절반이면 절반 —
     * 다 닳은 신발의 포인트를 뺐다 넣어 공짜로 채우지 못하게). 0119 전 서버는 늘어난 만큼 더하고 줄어든 만큼 뺀다.
     */
    fun durabilityNow(draft: PointsDraft): Int {
        val before = durabilityMax(saved)
        val after = durabilityMax(draft)
        return if (rules.stats.fromServer) {
            Math.round(durabilityNow.toDouble() * after / before.coerceAtLeast(1)).toInt().coerceIn(0, after)
        } else {
            (durabilityNow + after - before).coerceIn(0, after)
        }
    }

    /**
     * 가득에서 달리는 분 — 최대 내구도 ÷ (분당 닳음 × 효율 포인트 배수)(서버 0119). 효율 포인트는 줄이고 내구도 포인트는 늘린다.
     * 0119 전 서버 규칙이면 효율은 건드리지 않는다(최대 내구도 ÷ 5).
     */
    fun lifeMinutes(draft: PointsDraft): Double = rules.lifeMinutes(durabilityMax(draft), rarity, draft.efficiency)

    fun changed(draft: PointsDraft): Boolean = draft != saved

    /** 이미 나눈 포인트를 줄이는가 — 다시 나누기를 쓰는 것이다 */
    fun lowers(draft: PointsDraft): Boolean = PointStat.entries.any { draft[it] < saved[it] }

    /** 서버가 받을 초안인가 — 받은 포인트 안, 바닥 위, 늘린 착화감은 상한(에너지 70% 절약)까지 */
    fun valid(draft: PointsDraft): Boolean =
        free(draft) >= 0 && PointStat.entries.all { draft[it] >= floor(it) } && comfortFits(draft.comfort)
}

/**
 * 포인트를 나눌 수 있는 신발이면 그 값. 폰에만 있던 신발 · 폰에서 올린 예전 신발(IMPORT · MINT) · 아직 1레벨인 신발 ·
 * 포인트를 모르는 예전 서버면 null.
 */
fun pointsBasisOf(shoe: Sneaker, rules: ShoeRules = ShoeRules.FALLBACK): PointsBasis? {
    val server = shoe.server ?: return null
    if (!server.hasDurabilityMax || !server.upgradable || server.points.total <= 0) return null
    val p = server.points
    return PointsBasis(
        shoeId = shoe.id,
        total = p.total,
        saved = PointsDraft(p.efficiency, p.comfort, p.durability),
        respec = p.respec,
        efficiencyBaseBps = server.efficiencyBaseBps,
        comfortBaseBps = server.comfortBaseBps,
        durabilityBase = server.durabilityBase,
        durabilityNow = server.durabilityNow,
        rarity = shoe.rarity.id,
        rules = rules,
    )
}

// ── ⓘ — 포인트 1 이 이 신발(등급)에 주는 것(2026-11-19 사용 후기 "포인트 화면에 ⓘ") ─────────────

/** 포인트 1 의 폭 "1.1" · "1.25" · "1" — 등급의 point_rate_bps(서버 규칙). 끝의 0 은 뺀다 */
fun PointsBasis.rateText(): String = trimmedDecimal(pointRatePercent).removeSuffix(".0")

/**
 * 내구도 포인트 1 이 늘리는 달리는 시간(분) "0.3" · "1.3" — 뽑은 최대 내구도 × 폭(서버 dur_max_of 는 합계를 반올림하므로 한 개씩은
 * 1 단위쯤 오르내린다 — 그 평균). 소수 한 자리로 내려서(부풀리지 않게), 끝의 .0 은 뺀다. 뽑은 값을 모르면 그 등급의 가장 낮은 값으로
 */
fun PointsBasis.minutesPerPointText(): String {
    val grade = rules.grade(rarity)
    val base = durabilityBase.takeIf { it > 0 } ?: grade.durabilityLo
    val units = base * grade.pointRateBps / 10000.0
    // 1분에 닳는 내구도 — 5 × 이미 나눈 효율 포인트의 배수(서버 0119. 효율 포인트가 없거나 예전 서버면 5)
    val perMinute = rules.stats.wearPerMinute * rules.stats.wearMult(grade.pointRateBps, saved.efficiency)
    val tenths = kotlin.math.floor(units / perMinute * 10 + 1e-9).toInt().coerceAtLeast(0)
    return if (tenths % 10 == 0) "${tenths / 10}" else "${tenths / 10}.${tenths % 10}"
}

// ── 추천대로 나누기(서버 0119 sneaker_points_preview) ─────────────

/**
 * 이 미리보기가 이 시트의 값으로 셈한 것인가 — 서버가 본 받은 포인트 · 나눈 포인트가 폰 사본과 다르면(낡은 사본 · 그사이 나눴다)
 * 값 · 추천을 쓰지 않는다
 */
fun PointsBasis.accepts(preview: PointsPreview): Boolean =
    preview.matches(total, saved.efficiency, saved.comfort, saved.durability)

/**
 * [target] 을 서버가 받을 초안으로 맞춘다 — 바닥(이미 나눈 값, 다시 나누기가 남았으면 0) 위 · 착화감은 상한(에너지 70% 절약)까지 ·
 * 합은 받은 포인트 안. 넘치면 착화감 → 내구도 → 효율 차례로 덜어 낸다(서버 추천이 같은 값일 때 고르는 차례 효율 → 내구도 → 착화감의 반대).
 */
fun PointsBasis.fitted(target: PointsDraft): PointsDraft {
    var efficiency = maxOf(target.efficiency, floor(PointStat.EFFICIENCY))
    var comfort = maxOf(target.comfort, floor(PointStat.COMFORT))
    var durability = maxOf(target.durability, floor(PointStat.DURABILITY))
    while (comfort > 0 && !comfortFits(comfort)) comfort--
    var over = efficiency + comfort + durability - total
    fun trimmed(value: Int, stat: PointStat): Int {
        val cut = minOf(over, value - floor(stat)).coerceAtLeast(0)
        over -= cut
        return value - cut
    }
    comfort = trimmed(comfort, PointStat.COMFORT)
    durability = trimmed(durability, PointStat.DURABILITY)
    efficiency = trimmed(efficiency, PointStat.EFFICIENCY)
    return PointsDraft(efficiency, comfort, durability)
}

/**
 * "추천대로 나누기" — 이미 나눈 값에 서버 추천(남은 포인트를 나누는 **더할 양**)을 더한 초안. 그사이 ± 로 고친 것은 버린다(추천은 서버가
 * 본 값에서 셈한 것이다). 추천이 없거나 · 미리보기가 이 시트의 값과 맞지 않거나 · 더할 것이 없으면 null.
 */
fun PointsBasis.recommended(preview: PointsPreview?): PointsDraft? {
    val add = preview?.takeIf { accepts(it) }?.recommend?.takeIf { it.sum > 0 } ?: return null
    val draft = fitted(PointsDraft(saved.efficiency + add.efficiency, saved.comfort + add.comfort, saved.durability + add.durability))
    return draft.takeIf { valid(it) && it != saved }
}

/**
 * "처음부터 추천대로" — 다시 나눌 수 있는 신발만: 모든 포인트를 처음부터 나눈 서버 추천(**절대값**). 이미 나눈 값을 줄이면 다시 나누기를
 * 쓴다([PointsBasis.lowers] — 확인 장면이 그렇게 말한다). 다시 나눌 수 없거나 추천이 없으면 null.
 */
fun PointsBasis.recommendedAll(preview: PointsPreview?): PointsDraft? {
    if (!respec) return null
    val all = preview?.takeIf { accepts(it) }?.recommendAll ?: return null
    val draft = fitted(PointsDraft(all.efficiency, all.comfort, all.durability))
    return draft.takeIf { valid(it) && it != saved }
}

/**
 * 처음 포인트가 생긴 사람인가 — 나눌 포인트가 있는 신발이 있고 아직 어느 신발에도 포인트를 나눈 적이 없다.
 * 이때만 ⓘ 에 "나눈 포인트는 되돌릴 수 없어요"를 함께 보인다(한 번 나누면 더는 보이지 않는다 — 폰에 따로 적지 않는다)
 */
fun firstPointsOf(shoes: List<Sneaker>): Boolean {
    val bases = shoes.mapNotNull { pointsBasisOf(it) }
    return bases.isNotEmpty() && bases.all { it.saved.sum == 0 }
}

/** 상세에서 포인트로 가는 길 — 남은 포인트가 있으면 알림, 다시 나누기만 남았으면 관리 시트의 한 줄만 */
sealed interface PointsEntry {
    /** 포인트가 없는 신발 */
    data object None : PointsEntry

    /** 나눌 포인트 [free] 개가 남았다 */
    data class Free(val free: Int) : PointsEntry

    /** 다 나눴지만 한 번 처음부터 다시 나눌 수 있다 */
    data object Respec : PointsEntry

    /** 다 나눴다 */
    data object Done : PointsEntry
}

fun pointsEntryOf(shoe: Sneaker): PointsEntry {
    val basis = pointsBasisOf(shoe) ?: return PointsEntry.None
    val free = basis.free(basis.saved)
    return when {
        free > 0 -> PointsEntry.Free(free)
        basis.respec -> PointsEntry.Respec
        else -> PointsEntry.Done
    }
}

/** 나누지 못한 까닭 */
enum class PointsError {
    /** 연결이 끊겼다 — 같은 초안을 다시 보내도 두 번 나뉘지 않는다(같은 요청 번호 · 합계를 보낸다) */
    OFFLINE,

    /** 서버가 받지 않았다(이미 바뀐 값 · 판매 중 · 체인 등) — 최신 값으로 다시 시작한다 */
    REJECTED,

    /** 서버가 받지 않았고 최신 값도 아직 다시 받지 못했다 — 보이는 값이 예전 값일 수 있다("다시 불러왔어요"라고 쓰지 않는다) */
    REJECTED_NOT_RELOADED,

    /** 로그인이 필요하다 */
    SIGN_IN,
}

/** 포인트 시트 한 장 */
sealed interface PointsPhase {
    val basis: PointsBasis
    val draft: PointsDraft

    /** ± 로 나누는 중. [error] 는 바로 앞의 보내기가 실패한 까닭 */
    data class Editing(override val basis: PointsBasis, override val draft: PointsDraft, val error: PointsError? = null) : PointsPhase

    /** 되돌릴 수 없으니 한 번 더 확인 */
    data class Confirm(override val basis: PointsBasis, override val draft: PointsDraft) : PointsPhase

    /** 보냈다 — 답을 기다린다 */
    data class Sending(override val basis: PointsBasis, override val draft: PointsDraft) : PointsPhase

    /** 서버가 받았다 */
    data class Done(override val basis: PointsBasis, override val draft: PointsDraft) : PointsPhase
}

fun startPoints(basis: PointsBasis): PointsPhase = PointsPhase.Editing(basis, basis.saved)

fun PointsPhase.add(stat: PointStat): PointsPhase =
    if (this is PointsPhase.Editing && basis.canAdd(draft, stat)) copy(draft = draft.with(stat, draft[stat] + 1), error = null) else this

fun PointsPhase.remove(stat: PointStat): PointsPhase =
    if (this is PointsPhase.Editing && basis.canRemove(draft, stat)) copy(draft = draft.with(stat, draft[stat] - 1), error = null) else this

/** 미리보기가 말하는 포인트 1 의 값 — 시트의 능력치 줄 아래 한 줄 */
sealed interface PointWorth {
    /** "1포인트 = 하루 약 +[sup] SUP" — 어림값이다(늘 "약") */
    data class Gain(val sup: String) : PointWorth

    /** 지금 내 러닝으로는 더 늘지 않는다(0) */
    data object Flat : PointWorth

    /** 착화감이 상한(에너지 70% 절약)에 닿았다 */
    data object ComfortCapped : PointWorth
}

/** 하루에 늘어나는 SUP "0.42" · "1.3" · "12" — 소수 둘째 자리까지, 끝의 0 은 뺀다(어림값이라 더 잘게 쓰지 않는다) */
fun formatApproxSup(value: Double): String =
    if (!value.isFinite()) "0"
    else java.math.BigDecimal.valueOf(value.coerceAtLeast(0.0)).setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

/**
 * 그 능력치에 포인트 1 을 더 넣으면 — 서버가 내 지난 14일 러닝으로 셈한 값. 서버가 그 칸을 보내지 않았으면 null(줄을 보이지 않는다).
 * 0 으로 보일 만큼 작으면 "더 늘지 않는다"고 말한다("+0 SUP"라고 쓰지 않는다).
 */
fun pointWorthOf(preview: PointsPreview, stat: PointStat): PointWorth? {
    if (stat == PointStat.COMFORT && preview.comfortCapped) return PointWorth.ComfortCapped
    val value = when (stat) {
        PointStat.EFFICIENCY -> preview.perPoint.efficiency
        PointStat.COMFORT -> preview.perPoint.comfort
        PointStat.DURABILITY -> preview.perPoint.durability
    } ?: return null
    // 둘째 자리에서 반올림해 0 이면(0.005 아래) 늘지 않는 것으로
    return if (!value.isFinite() || value < 0.005) PointWorth.Flat else PointWorth.Gain(formatApproxSup(value))
}

/** "추천대로 나누기" — 초안을 서버 추천(이미 나눈 값 + 더할 양)으로 채운다. 추천이 없으면 그대로 */
fun PointsPhase.recommend(preview: PointsPreview?): PointsPhase =
    if (this is PointsPhase.Editing) basis.recommended(preview)?.takeIf { it != draft }?.let { copy(draft = it, error = null) } ?: this else this

/** "처음부터 추천대로" — 다시 나눌 수 있는 신발의 초안을 서버 추천(절대값)으로 바꾼다. 추천이 없으면 그대로 */
fun PointsPhase.recommendAll(preview: PointsPreview?): PointsPhase =
    if (this is PointsPhase.Editing) basis.recommendedAll(preview)?.takeIf { it != draft }?.let { copy(draft = it, error = null) } ?: this else this

/** "포인트 나누기" — 바뀐 것이 있고 서버가 받을 초안일 때만 확인으로 */
fun PointsPhase.review(): PointsPhase =
    if (this is PointsPhase.Editing && basis.changed(draft) && basis.valid(draft)) PointsPhase.Confirm(basis, draft) else this

/**
 * 보내기 — 확인 장면에서, 또는 연결이 끊겨 확인하지 못한 같은 초안을 다시. 그 밖에는 null(보내지 않는다).
 * 같은 초안은 같은 요청 번호 · 같은 합계라 다시 보내도 서버에 한 번만 적힌다.
 */
fun PointsPhase.send(): PointsPhase.Sending? = when {
    this is PointsPhase.Confirm -> PointsPhase.Sending(basis, draft)
    this is PointsPhase.Editing && error == PointsError.OFFLINE && basis.changed(draft) && basis.valid(draft) ->
        PointsPhase.Sending(basis, draft)
    else -> null
}

/** 확인에서 "취소" — 고친 초안은 그대로 둔다 */
fun PointsPhase.backToEdit(): PointsPhase = if (this is PointsPhase.Confirm) PointsPhase.Editing(basis, draft) else this

/**
 * 서버의 대답 → 다음 장면. 받았으면 완료, 연결이 끊겼으면 같은 초안으로 다시 보낼 수 있게, 거절이면 최신 값([latest])으로
 * 다시 시작한다(최신 값을 모르면 보낸 초안에서 — 다음 동기화가 맞춘다).
 */
fun pointsAfterSend(
    sent: PointsPhase.Sending,
    outcome: EconomyOutcome,
    latest: PointsBasis?,
    reloaded: Boolean = latest != null,
): PointsPhase = when (outcome) {
    EconomyOutcome.Ok -> PointsPhase.Done(sent.basis, sent.draft)
    EconomyOutcome.Offline -> PointsPhase.Editing(sent.basis, sent.draft, PointsError.OFFLINE)
    EconomyOutcome.SignInRequired -> PointsPhase.Editing(sent.basis, sent.draft, PointsError.SIGN_IN)
    // "최신 값으로 다시 불러왔어요"는 서버 값을 실제로 다시 받았을 때만(2026-11-17 점검 #17)
    else -> (latest ?: sent.basis).let {
        PointsPhase.Editing(it, it.saved, if (reloaded && latest != null) PointsError.REJECTED else PointsError.REJECTED_NOT_RELOADED)
    }
}

/** 서버가 포인트를 받는 상태인가 — 내 신발 · 판매 중 아님 · 앱 안(economy.my_app_sneaker 와 같은 조건) */
fun pointsAvailable(shoe: Sneaker): Boolean {
    val server = shoe.server ?: return false
    return server.status == "OWNED" && (server.chainState.isBlank() || server.chainState == "APP")
}
