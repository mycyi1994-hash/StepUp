package com.stepup.android.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.data.repo.RankingProblem
import com.stepup.android.data.repo.RankingState
import com.stepup.android.domain.CrewRank
import com.stepup.android.domain.RankBoard
import com.stepup.android.domain.RankEntry
import com.stepup.android.domain.RankPeriod
import com.stepup.android.ui.components.DetailPage
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.PillChip
import com.stepup.android.ui.components.tint
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt

/**
 * 랭킹.
 *
 * 줄 세우는 축은 다섯이다 — 쾌속·지구력·적립, 크루, 그리고 종족. 그리고
 * 어느 축이든 기간을 고를 수 있다: 오늘·이번 주·이번 달·전체기간.
 *
 * 기간이 있어야 하는 이유는 전체기간만 있으면 순위표가 일찍 시작한 사람의
 * 명단이 되기 때문이다. 어제 가입한 사람은 3년 치 누적을 따라잡을 수 없고,
 * 따라잡을 수 없는 순위표는 두 번 보지 않는다.
 *
 * **여기 보이는 사람은 전부 실제 사용자다.** 예전에는 상대 15명이 코드에
 * 박혀 있었다. 그러면 "당신은 3등입니다"는 거짓말이 된다. 지금은 서버가
 * 계산하고, 못 가져오면 지어내지 않고 못 가져왔다고 말한다.
 */
@Composable
fun RankingScreen(
    onBack: () -> Unit = {},
    viewModel: CommunityViewModel = viewModel(factory = CommunityViewModel.Factory),
) {
    var boardIndex by rememberSaveable { mutableIntStateOf(0) }

    val personalBoards = RankBoard.entries
    // 순서: 쾌속 · 지구력 · 적립 · 크루. 개인 셋이 먼저이고 그 뒤가 단체다.
    val crewIndex = personalBoards.size
    val board = personalBoards[boardIndex.coerceIn(0, personalBoards.lastIndex)]
    val meLabel = stringResource(R.string.rank_me)

    val ranking by viewModel.ranking.collectAsStateWithLifecycle()
    val crewRanking by viewModel.crewRanking.collectAsStateWithLifecycle()
    val period by viewModel.period.collectAsStateWithLifecycle()

    // 탭을 옮길 때마다 그 부문을 받아 온다. 이미 받아 둔 것은 다시 묻지
    // 않는다 — 같은 답을 받으려고 네트워크를 쓸 이유가 없다.
    LaunchedEffect(boardIndex, period) {
        when (boardIndex) {
            crewIndex -> viewModel.loadCrewRanking()
            else -> viewModel.loadRanking(board, meLabel)
        }
    }

    DetailPage(title = stringResource(R.string.community_ranking), onBack = onBack) {
        // 부문은 넷이라 한 줄에 균등 분할로는 글자가 뭉개진다 — 옆으로
        // 밀어서 고른다. 기간은 넷이고 이름이 짧아 한 줄에 들어간다.
        item {
            LazyRow(modifier = Modifier.testTag("ranking-board-tabs"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val labels = listOf(
                    R.string.rank_board_speed,
                    R.string.rank_board_time,
                    R.string.rank_board_sup,
                    R.string.rank_board_crew,
                )
                items(labels.size) { index ->
                    PillChip(
                        text = stringResource(labels[index]),
                        selected = boardIndex == index,
                        onClick = { boardIndex = index },
                    )
                }
            }
        }

        item {
            SegmentedTabs(
                labels = listOf(
                    stringResource(R.string.rank_period_day),
                    stringResource(R.string.rank_period_week),
                    stringResource(R.string.rank_period_month),
                    stringResource(R.string.rank_period_all_short),
                ),
                selected = RankPeriod.entries.indexOf(period),
                onSelect = { viewModel.selectPeriod(RankPeriod.entries[it]) },
            )
        }

        if (boardIndex == crewIndex) {
            item {
                GlowCard(contentPadding = PaddingValues(16.dp), spacing = 6.dp) {
                    Text(
                        text = stringResource(R.string.rank_crew_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = Snow,
                    )
                    Text(
                        text = stringResource(R.string.rank_crew_body),
                        fontSize = 14.sp,
                        color = Silver,
                        lineHeight = 22.sp,
                    )
                }
            }
            val crews = crewRanking
            when {
                crews == null -> item { RankingNotice(R.string.rank_loading) }
                // 명단은 있는데 아무도 같이 달리지 않았다. 0 km 만 늘어놓는
                // 것보다 왜 비었는지 말하는 편이 낫다.
                crews.none { it.runs > 0 } -> item { RankingNotice(R.string.rank_crew_empty) }
                else -> items(crews, key = { it.crewId }) { row -> CrewRow(row) }
            }
            return@DetailPage
        }

        val ready = ranking as? RankingState.Ready
        if (ready == null) {
            item {
                val state = ranking
                if (state is RankingState.Failed) {
                    RankingNotice(state.problem.message(),
                        signInRequired = state.problem == RankingProblem.SIGN_IN_REQUIRED,
                    ) {
                        viewModel.loadRanking(board, meLabel, force = true)
                    }
                } else {
                    RankingNotice(R.string.rank_loading)
                }
            }
            return@DetailPage
        }

        val entries = ready.entries
        val me = ready.me

        // 시상대는 3명이 모여야 성립한다. 아직 두 명뿐일 때 빈 자리를 세워
        // 두면 순위표가 아니라 공사장처럼 보인다.
        // Every runner appears once, in the ranked list below.

        if (me == null) {
            // 순위에 오르려면 이 기간에 한 번은 뛰어야 한다. 0으로 채운 줄을
            // 만들어 "전체 1명 중 1등"이라고 하는 것보다 이렇게 말하는 편이 낫다.
            item { RankingNotice(R.string.rank_no_record) }
        } else item {
            GlowCard(accent = true, contentPadding = PaddingValues(16.dp), spacing = 4.dp) {
                Text(
                    text = stringResource(R.string.rank_my_position),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Slate,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "#${me.rank}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.6).sp,
                        color = Volt,
                    )
                    Text(
                        text = valueLabel(me, board),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Snow,
                    )
                }
                Text(
                    text = stringResource(
                        R.string.rank_total_runners,
                        ready.totalRunners,
                    ),
                    fontSize = 14.sp,
                    color = Silver,
                )
            }
        }

        items(entries, key = { it.rank }) { entry ->
            RankRow(entry = entry, board = board)
        }
    }
}

/**
 * 순위 대신 보여 줄 한 줄.
 *
 * 비워 두면 사용자는 화면이 고장 난 줄 안다. 왜 비었는지 말하고, 다시
 * 해 볼 수 있는 것이면 누를 자리를 준다.
 */
@Composable
private fun RankingNotice(
    @StringRes message: Int,
    signInRequired: Boolean = false,
    onRetry: (() -> Unit)? = null,
) {
    com.stepup.android.ui.components.StatePanel(
        message = stringResource(message), icon = Icons.Filled.EmojiEvents,
        loading = message == R.string.rank_loading,
        action = if (signInRequired) {
            { com.stepup.android.ui.components.SignInAgainButton() }
        } else if (onRetry != null) {
            { com.stepup.android.ui.components.GhostButton(stringResource(R.string.rank_retry), onClick = onRetry, modifier = Modifier.fillMaxWidth()) }
        } else null,
    )
}

@StringRes
private fun RankingProblem.message(): Int = when (this) {
    RankingProblem.OFFLINE -> R.string.rank_offline
    RankingProblem.SIGN_IN_REQUIRED -> R.string.rank_sign_in
    RankingProblem.REJECTED -> R.string.rank_failed
}

@Composable
private fun RankRow(entry: RankEntry, board: RankBoard) {
    GlowCard(accent = entry.isMe, contentPadding = PaddingValues(18.dp), spacing = 0.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${entry.rank}", modifier = Modifier.width(30.dp),
                color = if (entry.rank <= 3) medalColor(entry.rank) else Silver,
                style = MaterialTheme.typography.titleLarge)
            Box(Modifier.size(44.dp).background(CarbonHigh, CircleShape), contentAlignment = Alignment.Center) {
                Text(entry.monogram, color = if (entry.isMe) Volt else Silver, style = MaterialTheme.typography.titleMedium)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(entry.name, color = Snow, style = MaterialTheme.typography.titleMedium)
                Text(valueLabel(entry, board), color = if (entry.isMe) Volt else Silver,
                    style = MaterialTheme.typography.titleLarge, fontFamily = com.stepup.android.ui.theme.StepUpNumbers)
            }
        }
    }
}

private fun medalColor(rank: Int): Color = when (rank) {
    1 -> Color(0xFFD9A400)
    2 -> Color(0xFF8C9BAD)
    3 -> Color(0xFFB4703C)
    else -> Color(0xFF93A1BE)
}

@Composable
private fun valueLabel(entry: RankEntry, board: RankBoard): String = when (board) {
    RankBoard.TOP_SPEED -> "%.1f km/h".format(entry.topSpeedKmh)
    RankBoard.LONGEST_TIME -> durationLabel(entry.activeSec)
    RankBoard.TOTAL_SUP -> "%,.0f SUP".format(entry.sup)
}

/** 누적 시간을 "12h 30m" / "45m" 로 — 랭킹 줄에 들어갈 만큼 짧게 */
private fun durationLabel(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

/**
 * 크루 한 줄 — 순위 · 이름 · 크루 러닝 누적 거리.
 *
 * 아직 같이 달리지 않은 크루도 0 km 로 남는다. 목록에서 빼 버리면
 * "우리 크루가 순위에 없다"가 되고, 사용자는 기능이 고장 난 줄 안다.
 */
@Composable
private fun CrewRow(row: CrewRank) {
    GlowCard(accent = row.joined, contentPadding = PaddingValues(18.dp), spacing = 0.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${row.rank}", modifier = Modifier.width(30.dp),
                color = if (row.rank <= 3) medalColor(row.rank) else Silver, style = MaterialTheme.typography.titleLarge)
            Box(Modifier.size(44.dp).background(CarbonHigh, CircleShape), contentAlignment = Alignment.Center) {
                Text(row.monogram, color = if (row.joined) Volt else Silver, style = MaterialTheme.typography.titleMedium)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(row.name, style = MaterialTheme.typography.titleMedium, color = Snow)
                Text(stringResource(R.string.rank_crew_runs, row.runs), style = MaterialTheme.typography.bodyMedium, color = Silver)
                Text("%,.1f km".format(row.km), style = MaterialTheme.typography.titleLarge,
                    fontFamily = com.stepup.android.ui.theme.StepUpNumbers, color = if (row.joined) Volt else Silver)
            }
        }
    }
}

