package com.stepup.android.ui.screens.walk

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.stepup.android.R
import com.stepup.android.domain.CrewBonusStatus
import com.stepup.android.domain.CrewRunOutcome
import com.stepup.android.domain.TogetherRule

/*
 * 함께 달린 사람(서버 0120 — docs/together-bonus-fix-spec-2026-11-21.md 2 · 3장)의 화면 글 — 대기실 한 줄 · 크루 결과 한 줄.
 * 판단은 화면 밖에서 한다(TogetherBonusTest). 인원 · 금액은 늘 서버 값이고, 여기서는 규칙의 문턱(분 · 목표의 몫)만 글로 옮긴다.
 */

/** 대기실 한 줄의 갈래 */
internal sealed interface TogetherLobbyLine {
    /** 목표(크루 코스 · 번개 글의 거리)의 절반 — [km] 는 채워야 하는 거리 */
    data class Half(val km: Double, val minutes: Int, val course: Boolean) : TogetherLobbyLine

    /** 목표의 다른 몫(설정이 0.5 가 아닐 때) — [km] 는 채워야 하는 거리 */
    data class Km(val km: Double, val minutes: Int) : TogetherLobbyLine

    /** 목표가 없는 방(자유 코스 · 거리 없는 번개) — 분만 */
    data class Minutes(val minutes: Int) : TogetherLobbyLine
}

/**
 * 대기실에 둘 한 줄 — 서버에서 받은 규칙이 함께 달린 사람만 세는 규칙(ACTIVE)일 때만. 예전 규칙 · 받지 못한 규칙이면 null.
 * [goalKm] 은 방의 목표 거리 — null 이면 아직 모른다(줄을 두지 않는다), 0 이하면 목표가 없는 방. [course] 는 크루 방의 코스인가(번개는 글의 거리).
 */
internal fun togetherLobbyLine(rule: TogetherRule?, goalKm: Double?, course: Boolean): TogetherLobbyLine? {
    if (rule == null || !rule.fromServer || !rule.active || goalKm == null) return null
    val minutes = rule.minMinutes.coerceAtLeast(0)
    val need = rule.goalKmOf(goalKm)
    return when {
        need == null -> if (minutes > 0) TogetherLobbyLine.Minutes(minutes) else null
        kotlin.math.abs(rule.goalShare - 0.5) < 1e-9 -> TogetherLobbyLine.Half(need, minutes, course)
        else -> TogetherLobbyLine.Km(need, minutes)
    }
}

@Composable
internal fun togetherLobbyText(line: TogetherLobbyLine?): String? = when (line) {
    null -> null
    is TogetherLobbyLine.Half -> stringResource(
        if (line.course) R.string.tb_lobby_course_half else R.string.tb_lobby_goal_half, courseKm(line.km), line.minutes,
    )
    is TogetherLobbyLine.Km -> stringResource(R.string.tb_lobby_km, courseKm(line.km), line.minutes)
    is TogetherLobbyLine.Minutes -> stringResource(R.string.tb_lobby_minutes, line.minutes)
}

/** 크루 러닝 결과의 함께 달린 사람 한 줄 */
internal sealed interface TogetherResultLine {
    /** 내 러닝이 함께 달린 사람에 들지 못했다(서버 bonus.reason NOT_TOGETHER) — [course] 면 코스의 절반도 말한다 */
    data class NotTogether(val minutes: Int, val course: Boolean) : TogetherResultLine

    /** 지금까지 함께 달린 사람 [count] 명 — 출발 뒤 더하는 시간 안이라 친구가 올리면 더해질 수 있다 */
    data class Counting(val count: Int) : TogetherResultLine
}

/** 서버가 함께 달린 사람에 들지 못했다고 적는 까닭(crew_runs.bonus_reason, 0120) */
internal const val BONUS_NOT_TOGETHER = "NOT_TOGETHER"

/**
 * 크루 결과에 둘 함께 달린 사람 한 줄 — 서버에서 받은 규칙이 ACTIVE 일 때만, 서버가 이 러닝을 정산한 뒤에만.
 * 못 들었으면(NOT_TOGETHER) 차분한 한 줄, 아니면 출발 [startedAtMs] 뒤 더하는 시간 안일 때만 "N명 · 더해질 수 있어요".
 * 무효 러닝 · 적립 0(금액 줄이 없을 때)은 null — 더해질 것이 없다.
 */
internal fun togetherResultLine(rule: TogetherRule?, outcome: CrewRunOutcome?, startedAtMs: Long, nowMs: Long): TogetherResultLine? {
    if (rule == null || !rule.fromServer || !rule.active || outcome == null) return null
    if (!outcome.settled || outcome.invalid || !crewMoneyLines(outcome.total)) return null
    if (outcome.bonus.reason == BONUS_NOT_TOGETHER) {
        return TogetherResultLine.NotTogether(rule.minMinutes.coerceAtLeast(0), course = (outcome.courseKm ?: 0.0) > 0.0)
    }
    if (outcome.bonus.status != CrewBonusStatus.CONFIRMED || outcome.bonus.count < 1) return null
    return if (rule.topupOpen(startedAtMs, nowMs)) TogetherResultLine.Counting(outcome.bonus.count) else null
}

@Composable
internal fun togetherResultText(line: TogetherResultLine?): String? = when (line) {
    null -> null
    is TogetherResultLine.NotTogether -> stringResource(
        if (line.course) R.string.tb_result_not_together_goal else R.string.tb_result_not_together, line.minutes,
    )
    is TogetherResultLine.Counting -> stringResource(R.string.tb_result_together_n, line.count)
}
