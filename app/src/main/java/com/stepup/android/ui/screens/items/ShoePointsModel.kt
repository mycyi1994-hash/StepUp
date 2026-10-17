package com.stepup.android.ui.screens.items

import com.stepup.android.data.repo.EconomyOutcome
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.StatPoints

/*
 * 스탯 포인트 나누기(서버 0059 sneaker_points_set, 2026-10-16 사용자 결정)가 쓰는 순수 계산.
 *
 * 레벨이 하나 오를 때마다 포인트 4 — 포인트 1 은 효율 +0.25%p · 착화감 +0.1%p(착화감은 20% 까지) · 최대 내구도 + 기본의 10%
 * (남은 내구도도 같이, 서버 0066) 중 하나다. 나눈 포인트는 되돌리지 않는다. 0059 전에 강화한 신발은 지금 스탯이 그대로이도록
 * 효율 2 · 착화감 2 씩 나눠 둔 채로 받았고, 한 번은 처음부터 다시 나눌 수 있다([PointsBasis.respec]).
 * 값은 모두 서버가 준 신발 값에서 온다 — 이 계산은 미리보기이고, 서버가 같은 규칙으로 다시 확인한다.
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

/** 시트가 시작하는 값 — 서버가 준 이 켤레의 받은 포인트 · 나눈 포인트 · 레벨 1 값 · 내구도 */
data class PointsBasis(
    val shoeId: Long,
    val total: Int,
    val saved: PointsDraft,
    val respec: Boolean,
    val efficiencyBaseBps: Int,
    val comfortBaseBps: Int,
    val durabilityBase: Int,
    val durabilityNow: Int,
) {
    fun free(draft: PointsDraft): Int = total - draft.sum

    /** 줄일 수 있는 바닥 — 다시 나누기가 남았으면 0, 아니면 이미 나눈 값 */
    fun floor(stat: PointStat): Int = if (respec) 0 else saved[stat]

    fun canRemove(draft: PointsDraft, stat: PointStat): Boolean = draft[stat] > floor(stat)

    /** 남은 포인트가 있고, 착화감이면 20% 를 넘지 않을 때 */
    fun canAdd(draft: PointsDraft, stat: PointStat): Boolean =
        free(draft) > 0 && (stat != PointStat.COMFORT || comfortRaw(draft.comfort + 1) <= StatPoints.COMFORT_CAP_BPS)

    private fun comfortRaw(points: Int): Int = comfortBaseBps + StatPoints.COMFORT_BPS * points

    fun efficiencyBps(draft: PointsDraft): Int = efficiencyBaseBps + StatPoints.EFFICIENCY_BPS * draft.efficiency
    fun comfortBps(draft: PointsDraft): Int = minOf(comfortRaw(draft.comfort), StatPoints.COMFORT_CAP_BPS)
    /** 내구도 포인트 1 이 올리는 값 — 이 신발 기본 최대 내구도의 10% */
    val durabilityPerPoint: Int get() = StatPoints.durabilityPerPoint(durabilityBase)

    fun durabilityMax(draft: PointsDraft): Int = durabilityBase + durabilityPerPoint * draft.durability

    /** 내구도 포인트는 최대 · 남은 내구도를 함께 올린다(다시 나누기로 줄이면 함께 내린다) */
    fun durabilityNow(draft: PointsDraft): Int =
        (durabilityNow + durabilityPerPoint * (draft.durability - saved.durability)).coerceIn(0, durabilityMax(draft))

    fun changed(draft: PointsDraft): Boolean = draft != saved

    /** 이미 나눈 포인트를 줄이는가 — 다시 나누기를 쓰는 것이다 */
    fun lowers(draft: PointsDraft): Boolean = PointStat.entries.any { draft[it] < saved[it] }

    /** 서버가 받을 초안인가 — 받은 포인트 안, 바닥 위, 늘린 착화감은 20% 까지 */
    fun valid(draft: PointsDraft): Boolean =
        free(draft) >= 0 && PointStat.entries.all { draft[it] >= floor(it) } &&
            (draft.comfort <= saved.comfort || comfortRaw(draft.comfort) <= StatPoints.COMFORT_CAP_BPS)
}

/**
 * 포인트를 나눌 수 있는 신발이면 그 값. 폰에만 있던 신발 · 폰에서 올린 예전 신발(IMPORT · MINT) · 아직 1레벨인 신발 ·
 * 포인트를 모르는 예전 서버면 null.
 */
fun pointsBasisOf(shoe: Sneaker): PointsBasis? {
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
    )
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
fun pointsAfterSend(sent: PointsPhase.Sending, outcome: EconomyOutcome, latest: PointsBasis?): PointsPhase = when (outcome) {
    EconomyOutcome.Ok -> PointsPhase.Done(sent.basis, sent.draft)
    EconomyOutcome.Offline -> PointsPhase.Editing(sent.basis, sent.draft, PointsError.OFFLINE)
    EconomyOutcome.SignInRequired -> PointsPhase.Editing(sent.basis, sent.draft, PointsError.SIGN_IN)
    else -> (latest ?: sent.basis).let { PointsPhase.Editing(it, it.saved, PointsError.REJECTED) }
}

/** 서버가 포인트를 받는 상태인가 — 내 신발 · 판매 중 아님 · 앱 안(economy.my_app_sneaker 와 같은 조건) */
fun pointsAvailable(shoe: Sneaker): Boolean {
    val server = shoe.server ?: return false
    return server.status == "OWNED" && (server.chainState.isBlank() || server.chainState == "APP")
}
