package com.stepup.android.data.repo

import com.stepup.android.data.remote.CrewBoardEntryRow
import com.stepup.android.data.remote.CrewBoardRow
import com.stepup.android.data.remote.LeaderboardApi
import com.stepup.android.data.remote.RunnerBoardRow
import com.stepup.android.data.remote.RunnerRow
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.domain.CrewBoard
import com.stepup.android.domain.LeaderEntry
import com.stepup.android.domain.LeaderLoad
import com.stepup.android.domain.LeaderPeriod
import com.stepup.android.domain.LeaderRun
import com.stepup.android.domain.LeaderboardMath
import com.stepup.android.domain.PointScope
import com.stepup.android.domain.PointsBoard
import com.stepup.android.domain.RunnerBoard
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * 리더보드 세 화면이 기대는 곳(어댑터 경계). 화면은 이 모양만 안다 — 서버 함수 이름 · 응답 칸은 구현이 감춘다.
 *
 * - 크루 · 개인: 서버 주간 순위(0061 weekly_crew_board · weekly_runner_board)
 * - 포인트: 서버에 주간 포인트 순위가 없다 — [LeaderLoad.Unavailable]. 개발용 고정 값은 기기 검사에서만 이 경계로 넣는다.
 */
interface LeaderboardSource {
    suspend fun crews(top: Int): LeaderLoad<CrewBoard>
    suspend fun runners(top: Int): LeaderLoad<RunnerBoard>
    suspend fun points(scope: PointScope, top: Int): LeaderLoad<PointsBoard>
}

class ServerLeaderboardSource(private val api: LeaderboardApi) : LeaderboardSource {

    override suspend fun crews(top: Int): LeaderLoad<CrewBoard> = api.crews(top).toLoad { it.toBoard() }

    override suspend fun runners(top: Int): LeaderLoad<RunnerBoard> = api.runners(top).toLoad { it.toBoard() }

    /** 주간 포인트(P) 순위는 서버에 없다 — P 가 무엇인지(적립 SUP 인지 따로 쌓는 점수인지) 정해지지 않았다 */
    override suspend fun points(scope: PointScope, top: Int): LeaderLoad<PointsBoard> = LeaderLoad.Unavailable
}

private inline fun <R, T> ServerResult<R>.toLoad(map: (R) -> T): LeaderLoad<T> = when (this) {
    is ServerResult.Ok -> runCatching { map(value) }.fold({ LeaderLoad.Ready(it) }, { LeaderLoad.Failed(offline = false) })
    is ServerResult.Retry -> LeaderLoad.Failed(offline = true)
    is ServerResult.SignInRequired -> LeaderLoad.SignedOut
    // 서버에 아직 함수가 올라가지 않았으면(PostgREST 의 "Could not find the function") 준비 중으로 — 앱이 먼저 깔린 경우
    is ServerResult.Rejected -> if (reason.contains("Could not find the function", ignoreCase = true)) {
        LeaderLoad.Unavailable
    } else {
        LeaderLoad.Failed(offline = false)
    }
}

private fun period(start: String, end: String, timezone: String) =
    LeaderPeriod(LocalDate.parse(start.take(10)), LocalDate.parse(end.take(10)), timezone)

internal fun RunnerBoardRow.toBoard(): RunnerBoard = RunnerBoard(
    period = period(weekStart, weekEnd, timezone),
    total = total,
    me = me?.toEntry(),
    ahead = ahead?.toEntry(),
    nearby = nearby.map { it.toEntry() },
    top = top.map { it.toEntry() },
    myName = myName?.takeIf { it.isNotBlank() },
    myKm = myMeters / 1000.0,
    myRuns = myRuns,
    myRunList = myRunList.mapNotNull { run ->
        runCatching { LeaderRun(run.startedAt.toInstant(), run.meters / 1000.0) }.getOrNull()
    },
)

private fun RunnerRow.toEntry() = LeaderEntry(
    id = userId,
    name = name,
    mark = LeaderboardMath.personMark(name, monogram),
    rank = rank,
    previousRank = previousRank,
    metric = meters / 1000.0,
    isMe = isMe,
    runs = runs,
)

internal fun CrewBoardRow.toBoard(): CrewBoard = CrewBoard(
    period = period(weekStart, weekEnd, timezone),
    total = total,
    top = top.map { it.toEntry() },
    mine = myCrew?.toEntry(),
    nearby = nearby.map { it.toEntry() },
)

private fun CrewBoardEntryRow.toEntry() = LeaderEntry(
    id = crewId,
    name = name,
    mark = monogram?.takeIf { it.isNotBlank() } ?: name.trim().take(1),
    rank = rank,
    previousRank = previousRank,
    metric = meters / 1000.0,
    isMe = isMine,
    participants = runners,
    photoVersion = imageVer.takeIf { hasImage },
    tint = imageBg,
)

/** Postgres 시각("2026-10-16T09:12:00.123+00:00" · "2026-10-16 09:12:00+00") → Instant */
private fun String.toInstant(): Instant {
    val t = trim().replace(' ', 'T').let { if (Regex("[+-]\\d\\d$").containsMatchIn(it)) "$it:00" else it }
    return runCatching { OffsetDateTime.parse(t).toInstant() }.getOrElse { Instant.parse(t) }
}
