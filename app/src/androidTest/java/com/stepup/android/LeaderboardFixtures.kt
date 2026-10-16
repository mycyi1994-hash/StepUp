package com.stepup.android

import com.stepup.android.data.repo.LeaderboardSource
import com.stepup.android.domain.CrewBoard
import com.stepup.android.domain.LeaderEntry
import com.stepup.android.domain.LeaderLoad
import com.stepup.android.domain.LeaderPeriod
import com.stepup.android.domain.LeaderRun
import com.stepup.android.domain.PointScope
import com.stepup.android.domain.PointsBoard
import com.stepup.android.domain.RunnerBoard
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * 개발용 고정 값 — 리더보드 기기 검사에서만 [LeaderboardSource] 경계로 넣는다(앱 코드는 부르지 않는다). 실제 순위가 아니다.
 * 이름 · 수치는 전달본 시안(2026-10-17)의 예시를 따른다. 주간 포인트는 서버에 아직 없어 이 값으로만 그려 본다.
 */
class FixtureLeaderboards(
    var crews: LeaderLoad<CrewBoard> = LeaderLoad.Ready(LeaderboardSamples.crews()),
    var runners: LeaderLoad<RunnerBoard> = LeaderLoad.Ready(LeaderboardSamples.runners()),
    var points: (PointScope) -> LeaderLoad<PointsBoard> = { LeaderLoad.Ready(LeaderboardSamples.points(it)) },
) : LeaderboardSource {
    override suspend fun crews(top: Int): LeaderLoad<CrewBoard> = crews
    override suspend fun runners(top: Int): LeaderLoad<RunnerBoard> = runners
    override suspend fun points(scope: PointScope, top: Int): LeaderLoad<PointsBoard> = points(scope)
}

object LeaderboardSamples {
    val period = LeaderPeriod(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 19), "Asia/Seoul")

    private fun crew(rank: Int, name: String, mark: String, km: Double, runners: Int, previous: Int?, mine: Boolean = false) =
        LeaderEntry(id = "crew-$rank", name = name, mark = mark, rank = rank, previousRank = previous, metric = km, isMe = mine,
            participants = runners)

    fun crews(mine: Boolean = true): CrewBoard {
        val top = listOf(
            crew(1, "서울러닝클럽", "S", 412.6, 42, 3),
            crew(2, "새벽다섯시", "5", 298.4, 31, 1),
            crew(3, "러닝메이트", "M", 274.1, 28, 2),
            crew(4, "반포페이스", "B", 261.0, 24, 6),
            crew(5, "성수스트라이드", "S", 257.3, 36, 4),
            crew(6, "망원나이트런", "M", 233.8, 19, 5),
        )
        val near = listOf(
            crew(10, "서촌러너스", "S", 198.2, 22, 9),
            crew(11, "라이트페이스", "L", 189.5, 17, 13),
            crew(12, "한강러너스", "H", 184.0, 38, 15, mine = true),
            crew(13, "반포러닝", "B", 176.4, 15, 12),
            crew(14, "오후다섯시", "5", 167.9, 12, 11),
        )
        return CrewBoard(period, total = 86, top = top, mine = if (mine) near[2] else null, nearby = if (mine) near else emptyList())
    }

    private fun runner(rank: Int, name: String, km: Double, me: Boolean = false, previous: Int? = null, runs: Int? = null) =
        LeaderEntry(id = "runner-$rank", name = name, mark = name.take(1), rank = rank, previousRank = previous, metric = km, isMe = me, runs = runs)

    private fun at(text: String) = OffsetDateTime.parse(text).toInstant()

    fun runners(ranked: Boolean = true): RunnerBoard {
        val me = runner(127, "가나", 32.6, me = true, previous = 139, runs = 4)
        val near = listOf(runner(125, "예린", 34.8), runner(126, "서연", 33.4), me, runner(128, "민수", 31.9), runner(129, "은지", 31.4))
        val top = listOf(
            runner(1, "지훈", 128.4), runner(2, "서윤", 116.8), runner(3, "민재", 108.2), runner(4, "지수", 96.7), runner(5, "현우", 92.5),
            runner(6, "하린", 88.0), runner(7, "도윤", 84.3),
        )
        val runs = listOf(
            LeaderRun(at("2026-10-17T07:10:00+09:00"), 12.8),
            LeaderRun(at("2026-10-15T20:30:00+09:00"), 4.8),
            LeaderRun(at("2026-10-14T06:40:00+09:00"), 8.6),
            LeaderRun(at("2026-10-12T19:05:00+09:00"), 6.4),
        )
        return if (ranked) {
            RunnerBoard(period, total = 1240, me = me, ahead = near[1], nearby = near, top = top, myName = "가나",
                myKm = 32.6, myRuns = 4, myRunList = runs)
        } else {
            RunnerBoard(period, total = 1240, me = null, ahead = null, nearby = emptyList(), top = top, myName = "가나",
                myKm = 0.0, myRuns = 0, myRunList = emptyList())
        }
    }

    private fun point(rank: Int, name: String, p: Double, crew: String?, me: Boolean = false) =
        LeaderEntry(id = "p-$rank-$name", name = name, mark = name.take(1), rank = rank, metric = p, isMe = me, sub = crew)

    fun points(scope: PointScope): PointsBoard = when (scope) {
        PointScope.ALL -> {
            val near = listOf(point(82, "다은", 2860.0, null), point(83, "서연", 2760.0, null), point(84, "가나", 2680.0, null, me = true),
                point(85, "민수", 2630.0, null), point(86, "정우", 2560.0, null))
            PointsBoard(
                period, scope, crewName = null,
                top = listOf(point(1, "지훈", 9840.0, "서울러닝클럽"), point(2, "지수", 9120.0, "한강러너스"), point(3, "서윤", 8880.0, "새벽다섯시"),
                    point(4, "현우", 8450.0, "한강러너스"), point(5, "민재", 8210.0, "러닝메이트")),
                me = near[2], ahead = near[1], nearby = near, myName = "가나", myPoints = 2680.0, lastWeekPoints = 2370.0,
            )
        }
        PointScope.CREW -> {
            val near = listOf(point(6, "하린", 2860.0, null), point(7, "다은", 2760.0, null), point(8, "가나", 2680.0, null, me = true),
                point(9, "정우", 2630.0, null), point(10, "은지", 2560.0, null))
            PointsBoard(
                period, scope, crewName = "한강러너스",
                top = listOf(point(1, "지수", 9120.0, "한강러너스"), point(2, "현우", 8450.0, "한강러너스"), point(3, "서연", 5800.0, "한강러너스"),
                    point(4, "예린", 4210.0, "한강러너스"), point(5, "민수", 3120.0, "한강러너스")),
                me = near[2], ahead = near[1], nearby = near, myName = "가나", myPoints = 2680.0, lastWeekPoints = 2370.0,
            )
        }
    }
}
