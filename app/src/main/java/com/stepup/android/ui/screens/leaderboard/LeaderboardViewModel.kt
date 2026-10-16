package com.stepup.android.ui.screens.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.repo.LeaderboardSource
import com.stepup.android.domain.CrewBoard
import com.stepup.android.domain.LeaderLoad
import com.stepup.android.domain.PointScope
import com.stepup.android.domain.PointsBoard
import com.stepup.android.domain.RunnerBoard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 리더보드의 세 판 */
enum class LeaderBoardKind { CREW, PERSONAL, POINTS }

/** 개인 순위 목록 — 내 주변 · 상위 러너(큰 내 순위 · 집계는 그대로) */
enum class RunnerList { NEARBY, TOP }

/**
 * 리더보드 세 판의 상태. 판마다 요청 번호를 두어, 늦게 온 옛 답이 새 답(특히 포인트 범위를 바꾼 뒤)을 덮지 않게 한다.
 * 범위를 바꾸면 목록 · 내 순위 · 앞뒤 · 차이를 한꺼번에 비우고 같은 범위의 한 답으로 다시 채운다.
 */
class LeaderboardViewModel(
    private val source: LeaderboardSource = ServiceLocator.leaderboards,
) : ViewModel() {

    private val _crew = MutableStateFlow<LeaderLoad<CrewBoard>>(LeaderLoad.Loading)
    val crew: StateFlow<LeaderLoad<CrewBoard>> = _crew.asStateFlow()

    private val _runners = MutableStateFlow<LeaderLoad<RunnerBoard>>(LeaderLoad.Loading)
    val runners: StateFlow<LeaderLoad<RunnerBoard>> = _runners.asStateFlow()

    private val _points = MutableStateFlow<LeaderLoad<PointsBoard>>(LeaderLoad.Loading)
    val points: StateFlow<LeaderLoad<PointsBoard>> = _points.asStateFlow()

    private val _scope = MutableStateFlow(PointScope.ALL)
    val scope: StateFlow<PointScope> = _scope.asStateFlow()

    /** 더 보기를 가져오는 중인 판 — 지금 목록은 그대로 둔다 */
    private val _more = MutableStateFlow<Set<LeaderBoardKind>>(emptySet())
    val more: StateFlow<Set<LeaderBoardKind>> = _more.asStateFlow()

    private var crewTop = CREW_PAGE
    private var runnerTop = RUNNER_PAGE
    private val asked = mutableSetOf<LeaderBoardKind>()
    private val tickets = IntArray(LeaderBoardKind.entries.size)

    /** 처음 보이는 판만 가져온다(이미 가져왔으면 그대로) */
    fun open(kind: LeaderBoardKind) {
        if (asked.add(kind)) load(kind)
    }

    fun retry(kind: LeaderBoardKind) = load(kind)

    fun selectScope(scope: PointScope) {
        if (_scope.value == scope) return
        _scope.value = scope
        load(LeaderBoardKind.POINTS)
    }

    /** 상위 목록을 더 — 서버가 한 번에 같은 집계로 다시 센다(최대 100) */
    fun loadMore(kind: LeaderBoardKind) {
        if (kind in _more.value) return
        when (kind) {
            LeaderBoardKind.CREW -> load(kind, keep = true, top = (crewTop + CREW_PAGE).coerceAtMost(MAX_TOP))
            LeaderBoardKind.PERSONAL -> load(kind, keep = true, top = (runnerTop + RUNNER_PAGE).coerceAtMost(MAX_TOP))
            LeaderBoardKind.POINTS -> Unit
        }
    }

    fun topLimit(kind: LeaderBoardKind): Int = when (kind) {
        LeaderBoardKind.CREW -> crewTop
        LeaderBoardKind.PERSONAL -> runnerTop
        LeaderBoardKind.POINTS -> POINT_PAGE
    }

    private fun load(kind: LeaderBoardKind, keep: Boolean = false, top: Int = topLimit(kind)) {
        val ticket = ++tickets[kind.ordinal]
        if (keep) _more.value = _more.value + kind else when (kind) {
            LeaderBoardKind.CREW -> _crew.value = LeaderLoad.Loading
            LeaderBoardKind.PERSONAL -> _runners.value = LeaderLoad.Loading
            LeaderBoardKind.POINTS -> _points.value = LeaderLoad.Loading
        }
        viewModelScope.launch {
            try {
                when (kind) {
                    LeaderBoardKind.CREW -> {
                        val result = source.crews(top)
                        if (ticket == tickets[kind.ordinal]) {
                            if (result is LeaderLoad.Ready) crewTop = top
                            _crew.value = result.keepOnFailure(_crew.value, keep)
                        }
                    }
                    LeaderBoardKind.PERSONAL -> {
                        val result = source.runners(top)
                        if (ticket == tickets[kind.ordinal]) {
                            if (result is LeaderLoad.Ready) runnerTop = top
                            _runners.value = result.keepOnFailure(_runners.value, keep)
                        }
                    }
                    LeaderBoardKind.POINTS -> {
                        val scope = _scope.value
                        val result = source.points(scope, POINT_PAGE)
                        // 범위를 바꾼 사이에 온 옛 범위의 답은 버린다
                        if (ticket == tickets[kind.ordinal] && scope == _scope.value) _points.value = result
                    }
                }
            } finally {
                if (ticket == tickets[kind.ordinal]) _more.value = _more.value - kind
            }
        }
    }

    /** 더 보기에 실패하면 지금 목록을 지우지 않는다 */
    private fun <T> LeaderLoad<T>.keepOnFailure(current: LeaderLoad<T>, keep: Boolean): LeaderLoad<T> =
        if (keep && this !is LeaderLoad.Ready && current is LeaderLoad.Ready) current else this

    companion object {
        const val CREW_PAGE = 30
        const val RUNNER_PAGE = 20
        const val POINT_PAGE = 20
        const val MAX_TOP = 100
    }
}
