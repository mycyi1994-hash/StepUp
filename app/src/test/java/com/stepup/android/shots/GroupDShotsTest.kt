package com.stepup.android.shots

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.stepup.android.data.local.RecordTotals
import com.stepup.android.data.repo.LeaderboardSource
import com.stepup.android.domain.CrewBoard
import com.stepup.android.domain.LeaderEntry
import com.stepup.android.domain.LeaderLoad
import com.stepup.android.domain.LeaderPeriod
import com.stepup.android.domain.PointScope
import com.stepup.android.domain.PointsBoard
import com.stepup.android.domain.RecordPeriod
import com.stepup.android.domain.RunnerBoard
import com.stepup.android.domain.monthBars
import com.stepup.android.ui.screens.leaderboard.LeaderboardScreen
import com.stepup.android.ui.screens.leaderboard.LeaderboardViewModel
import com.stepup.android.ui.screens.records.RecordStatsContent
import com.stepup.android.ui.screens.records.RecordsContent
import com.stepup.android.ui.screens.records.RecordsLoad
import com.stepup.android.ui.screens.records.StatWindow
import com.stepup.android.ui.screens.records.StatsUi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** 재디자인 D — 09 · 09b 이번 주 크루 순위, 10 내 러닝 기록, 11 러닝 통계(시안의 값으로) */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class GroupDShotsTest(private val dark: Boolean, private val sizeIndex: Int) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0} size={1}")
        fun params(): List<Array<Any>> = listOf(false, true).flatMap { d -> ShotSizes.indices.map { arrayOf<Any>(d, it) } }
    }

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Before fun only() = Shots.require()

    private val seoul = ZoneId.of("Asia/Seoul")
    private val today = LocalDate.of(2026, 11, 24)
    private val period = LeaderPeriod(LocalDate.of(2026, 11, 23), LocalDate.of(2026, 11, 30), "Asia/Seoul")

    private class Fixed(val crew: LeaderLoad<CrewBoard>) : LeaderboardSource {
        override suspend fun crews(top: Int) = crew
        override suspend fun runners(top: Int): LeaderLoad<RunnerBoard> = LeaderLoad.Loading
        override suspend fun points(scope: PointScope, top: Int): LeaderLoad<PointsBoard> = LeaderLoad.Unavailable
        override val offersPoints: Boolean get() = false
    }

    private fun crews(): CrewBoard {
        fun c(rank: Int, name: String, km: Double, runners: Int) =
            LeaderEntry(id = "c$rank", name = name, mark = name.take(1), rank = rank, metric = km, participants = runners, previousRank = rank)
        val top = listOf(
            c(1, "오늘도 카보로딩 RC", 128.4, 14), c(2, "달려라안양고고", 96.2, 11), c(3, "1공수", 81.0, 9),
            c(4, "달토끼", 54.7, 6), c(5, "원숭이", 33.1, 4), c(6, "남한강러너", 20.5, 3),
        )
        return CrewBoard(period, total = 6, top = top, mine = null, nearby = emptyList())
    }

    private fun all(name: String, content: @androidx.compose.runtime.Composable () -> Unit) =
        compose.shot(name, dark, ShotSizes[sizeIndex], content)

    private fun board(name: String, load: LeaderLoad<CrewBoard>) {
        val model = LeaderboardViewModel(Fixed(load))
        compose.shot(name, dark, ShotSizes[sizeIndex]) { LeaderboardScreen(onBack = {}, viewModel = model, onFindCrew = {}) }
    }

    @Test fun s09_crew() = board("09_crew", LeaderLoad.Ready(crews()))

    @Test fun s09b_failed() = board("09b_crew_failed", LeaderLoad.Failed(offline = true))

    private val zero = RecordTotals(0, 0.0, 0, 0.0, 0, 0)

    @Test fun s10_records() = all("10_records") {
        RecordsContent(
            RecordsLoad.Ready(RecordPeriod.Month(YearMonth.of(2026, 11)), zero, emptyList(), anyRecords = true, more = false),
            RecordPeriod.Month(YearMonth.of(2026, 11)), zone = seoul, today = today,
        )
    }

    @Test fun s11_stats() = all("11_stats") {
        val month = YearMonth.of(2026, 11)
        RecordStatsContent(
            StatsUi(StatWindow.Month(month), zero, monthBars(month, emptyList(), today, seoul), canGoNext = false, thisMonth = month),
            zone = seoul,
        )
    }
}
