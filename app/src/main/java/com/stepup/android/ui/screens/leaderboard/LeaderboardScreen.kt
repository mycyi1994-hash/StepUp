package com.stepup.android.ui.screens.leaderboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.domain.CrewBoard
import com.stepup.android.domain.LeaderEntry
import com.stepup.android.domain.LeaderLoad
import com.stepup.android.domain.LeaderPeriod
import com.stepup.android.domain.LeaderboardMath
import com.stepup.android.domain.PointScope
import com.stepup.android.domain.PointsBoard
import com.stepup.android.domain.RunnerBoard
import com.stepup.android.ui.components.CircleIconButton
import com.stepup.android.ui.components.PlainDivider
import com.stepup.android.ui.components.PlainEmptyState
import com.stepup.android.ui.components.PlainListCard
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.RunTabs
import com.stepup.android.ui.components.RunTextAction
import com.stepup.android.ui.components.RunTopBar
import com.stepup.android.ui.components.runNumberStyle
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.feedbackClickable
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle

/*
 * 리더보드 — 크루 순위 · 개인 러닝 순위 · 주간 포인트 세 판(2026-11-24 재디자인: 앱 테마 · 흰 카드 목록).
 *
 * 머리(뒤로 · 이번 주 순위 · 순위 기준) → 판 고르기(세그먼트) → 기간 · 기준 한 줄 → 순위 카드 → 아래 고정 내 줄 → 시트.
 * 순위 · 앞뒤 · 기간은 서버 값 그대로 — 받은 목록 안에서 순위를 다시 세지 않는다([LeaderboardSource]).
 * 예전의 늘 남색 화면 · 움직이는 트랙 배경 · 1위 시상대 · 머리글자 칸은 없앴다.
 */

private enum class LbSheet { RULES, CREW_NEARBY, MY_RUNS, POINT_NEARBY }

/**
 * @param onFindCrew 내 크루가 없을 때 아래 줄의 "크루 찾기" — 없으면 그 자리에 크루 가입 안내 글.
 */
@Composable
fun LeaderboardScreen(
    onBack: () -> Unit,
    initial: LeaderBoardKind = LeaderBoardKind.CREW,
    viewModel: LeaderboardViewModel = viewModel(),
    onFindCrew: (() -> Unit)? = null,
) {
    // 주간 포인트 순위가 없으면 판은 크루 · 개인 둘(빈 "준비 중" 판을 두지 않는다)
    val boards = if (viewModel.offersPoints) LeaderBoardKind.entries else LeaderBoardKind.entries - LeaderBoardKind.POINTS
    var board by rememberSaveable { mutableStateOf(if (initial in boards) initial else LeaderBoardKind.CREW) }
    var sheet by rememberSaveable { mutableStateOf<LbSheet?>(null) }
    var runnerList by rememberSaveable { mutableStateOf(RunnerList.NEARBY) }
    LaunchedEffect(viewModel, board) { viewModel.open(board) }

    val crew by viewModel.crew.collectAsStateWithLifecycle()
    val runners by viewModel.runners.collectAsStateWithLifecycle()
    val points by viewModel.points.collectAsStateWithLifecycle()
    val scope by viewModel.scope.collectAsStateWithLifecycle()
    val more by viewModel.more.collectAsStateWithLifecycle()
    val t = runTone()

    Box(Modifier.fillMaxSize().background(t.screen).testTag("leaderboard")) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            RunTopBar(
                onBack = onBack,
                title = stringResource(R.string.lb_menu_entry),
                trailing = {
                    CircleIconButton(
                        LbIcons.Info, stringResource(R.string.lb_rules_open), { sheet = LbSheet.RULES },
                        modifier = Modifier.testTag("lb-info"), size = 40.dp,
                    )
                },
            )
            RunTabs(
                labels = boards.map {
                    stringResource(
                        when (it) {
                            LeaderBoardKind.CREW -> R.string.lb_board_crew
                            LeaderBoardKind.PERSONAL -> R.string.lb_board_personal
                            LeaderBoardKind.POINTS -> R.string.lb_board_points
                        },
                    )
                },
                selected = boards.indexOf(board).coerceAtLeast(0),
                onSelect = { board = boards[it] },
                modifier = Modifier.padding(horizontal = Lb.Gutter).padding(top = 4.dp),
                tagPrefix = "lb-board",
            )
            when (board) {
                LeaderBoardKind.CREW -> CrewPage(
                    state = crew,
                    loadingMore = LeaderBoardKind.CREW in more,
                    onRetry = { viewModel.retry(LeaderBoardKind.CREW) },
                    onMore = { viewModel.loadMore(LeaderBoardKind.CREW) },
                    onOpenNearby = { sheet = LbSheet.CREW_NEARBY },
                    onFindCrew = onFindCrew,
                )
                LeaderBoardKind.PERSONAL -> PersonalPage(
                    state = runners, list = runnerList,
                    onList = { runnerList = it },
                    loadingMore = LeaderBoardKind.PERSONAL in more,
                    onRetry = { viewModel.retry(LeaderBoardKind.PERSONAL) },
                    onMore = { viewModel.loadMore(LeaderBoardKind.PERSONAL) },
                    onOpenRuns = { sheet = LbSheet.MY_RUNS },
                )
                LeaderBoardKind.POINTS -> PointsPage(
                    state = points, scope = scope,
                    onScope = viewModel::selectScope,
                    onRetry = { viewModel.retry(LeaderBoardKind.POINTS) },
                    onOpenNearby = { sheet = LbSheet.POINT_NEARBY },
                )
            }
        }
    }

    // 시트는 그 내용이 있을 때만(되살린 화면에서 아직 불러오는 중이면 열지 않는다)
    val openSheet = sheet?.takeIf { open ->
        when (open) {
            LbSheet.RULES -> true
            LbSheet.CREW_NEARBY -> crew is LeaderLoad.Ready
            LbSheet.MY_RUNS -> runners is LeaderLoad.Ready
            LbSheet.POINT_NEARBY -> points is LeaderLoad.Ready
        }
    }
    openSheet?.let { open ->
        LbSheetHost(
            title = stringResource(
                when (open) {
                    LbSheet.RULES -> R.string.lb_rules_title
                    LbSheet.CREW_NEARBY -> R.string.lb_crew_nearby_title
                    LbSheet.MY_RUNS -> R.string.lb_records_title
                    LbSheet.POINT_NEARBY -> R.string.lb_points_nearby_title
                },
            ),
            tag = "lb-sheet-${open.name.lowercase()}",
            onDismiss = { sheet = null },
        ) {
            when (open) {
                LbSheet.RULES -> RulesSheet(
                    board, pointsReady = points is LeaderLoad.Ready,
                    period = when (board) {
                        LeaderBoardKind.CREW -> (crew as? LeaderLoad.Ready)?.value?.period
                        LeaderBoardKind.PERSONAL -> (runners as? LeaderLoad.Ready)?.value?.period
                        LeaderBoardKind.POINTS -> (points as? LeaderLoad.Ready)?.value?.period
                    },
                )
                LbSheet.CREW_NEARBY -> (crew as? LeaderLoad.Ready)?.value?.let { CrewNearbySheet(it) }
                LbSheet.MY_RUNS -> (runners as? LeaderLoad.Ready)?.value?.let { MyRunsSheet(it) }
                LbSheet.POINT_NEARBY -> (points as? LeaderLoad.Ready)?.value?.let { PointNearbySheet(it) }
            }
        }
    }
}

// ── 공통 — 기간 · 상태 ─────────────────────────────────────────────

/** 기간 · 기준 한 줄 — 왼쪽 "이번 주 11.17–11.23 · 크루 전체 거리", 오른쪽 초기화 시각 */
@Composable
private fun LbMeta(period: LeaderPeriod?, criterion: String) {
    val t = runTone()
    val style = runTextStyle(13.sp, t.label, FontWeight.Medium, 1.3f)
    Row(
        Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 10.dp).heightIn(min = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                (if (period == null) stringResource(R.string.lb_period_pending) else weekLabel(period)) + " · ",
                style = style, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false).testTag("lb-period"),
            )
            Text(criterion, style = style, maxLines = 1, softWrap = false, modifier = Modifier.testTag("lb-criterion"))
        }
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.rd_d_resets), style = style, maxLines = 1, softWrap = false)
    }
}

/** "11월 4주" — 서버가 준 주의 첫날(월요일)이 그 달의 몇째 월요일인가. 날짜 범위는 순위 기준 시트에 있다 */
@Composable
private fun weekLabel(period: LeaderPeriod): String {
    val locale = LocalConfiguration.current.locales[0]
    val month = period.startDate.month.getDisplayName(JavaTextStyle.SHORT, locale)
    return stringResource(R.string.rd_d_week_of_month, month, (period.startDate.dayOfMonth - 1) / 7 + 1)
}

/** "11.23–11.29" — 서버가 준 주의 첫날 · 마지막 날 */
private fun periodRange(period: LeaderPeriod): String =
    "${period.startDate.monthValue}.${period.startDate.dayOfMonth}–${period.lastDate.monthValue}.${period.lastDate.dayOfMonth}"

/** 불러오는 중 · 실패 · 로그인 · 준비 중 — 지어낸 숫자 대신 이 칸을 보인다. 그 밖의 상태(Ready)면 false */
@Composable
private fun <T> LbLoadState(state: LeaderLoad<T>, onRetry: () -> Unit, unavailableTitle: String? = null, unavailableBody: String? = null): Boolean {
    when (state) {
        LeaderLoad.Loading -> Box(Modifier.fillMaxWidth().padding(vertical = 64.dp).testTag("lb-loading"), contentAlignment = Alignment.Center) {
            val label = stringResource(R.string.lb_loading)
            RunSpinner(Modifier.size(32.dp).semantics { contentDescription = label })
        }
        is LeaderLoad.Failed -> Column(
            Modifier.fillMaxWidth().padding(top = 72.dp).testTag("lb-failed"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PlainEmptyState(
                R.drawable.mascot_oops, stringResource(R.string.lb_failed_title),
                body = if (state.offline) stringResource(R.string.lb_failed_offline) else null,
                action = stringResource(R.string.lb_retry), onAction = onRetry, actionTag = "lb-failed-action",
            )
        }
        LeaderLoad.SignedOut -> PlainEmptyState(
            null, stringResource(R.string.lb_signed_out_title), Modifier.padding(top = 48.dp).testTag("lb-signed-out"),
            body = stringResource(R.string.lb_signed_out_body),
        )
        LeaderLoad.Unavailable -> PlainEmptyState(
            null, unavailableTitle ?: stringResource(R.string.lb_unavailable_title), Modifier.padding(top = 48.dp).testTag("lb-unavailable"),
            body = unavailableBody ?: stringResource(R.string.lb_unavailable_body),
        )
        is LeaderLoad.Ready -> return false
    }
    return true
}

/** 판 하나의 틀 — 넘기는 내용 + 아래 고정 내 줄(목록을 덮지 않게 내용 밖에 둔다) */
@Composable
private fun LbPage(
    tag: String,
    footer: (@Composable () -> Unit)?,
    content: LazyListScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().testTag(tag)) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().testTag("$tag-list"),
            contentPadding = PaddingValues(start = Lb.Gutter, end = Lb.Gutter, bottom = 20.dp),
            content = content,
        )
        if (footer != null) {
            Box(Modifier.fillMaxWidth().padding(start = Lb.Gutter, end = Lb.Gutter, top = 8.dp, bottom = 16.dp)) { footer() }
        }
    }
}

@Composable
private fun LbMoreButton(loading: Boolean, onMore: () -> Unit, tag: String) {
    Box(Modifier.fillMaxWidth().padding(top = 6.dp).heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
        if (loading) {
            RunSpinner(Modifier.size(22.dp))
        } else {
            RunTextAction(stringResource(R.string.lb_more), onMore, Modifier.testTag(tag))
        }
    }
}

/** 순위 카드 — 흰 카드 하나에 줄을 구분선으로 */
@Composable
private fun LbCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    PlainListCard(modifier, content = content)
}

// ── 크루 순위 ─────────────────────────────────────────────────────

@Composable
private fun CrewPage(
    state: LeaderLoad<CrewBoard>,
    loadingMore: Boolean,
    onRetry: () -> Unit,
    onMore: () -> Unit,
    onOpenNearby: () -> Unit,
    onFindCrew: (() -> Unit)?,
) {
    val ready = (state as? LeaderLoad.Ready)?.value
    LbPage(
        tag = "lb-crew",
        footer = ready?.let { b -> { CrewFooter(b, onOpenNearby, onFindCrew) } },
    ) {
        // 불러오지 못했으면 기간 줄 없이 빈 화면만(09b)
        if (state !is LeaderLoad.Failed) item(key = "head") { LbMeta(ready?.period, stringResource(R.string.lb_criterion_crew)) }
        item(key = "state") { LbLoadState(state, onRetry) }
        if (ready == null) return@LbPage
        if (ready.top.isEmpty()) {
            item(key = "empty") {
                PlainEmptyState(
                    null, stringResource(R.string.lb_crew_empty_title), Modifier.padding(top = 32.dp).testTag("lb-crew-empty"),
                    body = stringResource(R.string.lb_crew_empty_body),
                )
            }
            return@LbPage
        }
        item(key = "list") {
            LbCard {
                LbRows(ready.top) { entry -> CrewRow(entry) }
            }
        }
        if (ready.total > ready.top.size && ready.top.size < LeaderboardViewModel.MAX_TOP) {
            item(key = "more") { LbMoreButton(loadingMore, onMore, "lb-crew-more") }
        }
    }
}

/** 크루 한 줄 — 1위 줄은 지난주 대비도 함께(예전 1위 칸의 글) */
@Composable
private fun CrewRow(entry: LeaderEntry) {
    val runners = stringResource(R.string.lb_crew_runners, entry.participants ?: 0)
    when (entry.rank) {
        1 -> {
            val move = moveText(LeaderboardMath.rankRise(entry.rank, entry.previousRank), newWhenUnknown = entry.previousRank == null)
            LbRow(
                entry, points = false, sub = if (move != null) "$runners · $move" else runners,
                tag = "lb-crew-winner", nameTag = "lb-crew-winner-name", valueTag = "lb-crew-winner-value",
            )
        }
        2, 3 -> LbRow(entry, points = false, sub = runners, tag = "lb-crew-podium-${entry.rank}")
        else -> LbRow(entry, points = false, sub = runners)
    }
}

/** 아래 우리 크루 — 누르면 우리 크루 앞뒤. 크루가 없으면 캐릭터 · "크루 찾기", 이번 주 기록이 없으면 순위 없이 말한다 */
@Composable
private fun CrewFooter(board: CrewBoard, onOpenNearby: () -> Unit, onFindCrew: (() -> Unit)?) {
    val mine = board.mine
    if (mine == null) {
        LbFooterSurface(onClick = null, modifier = Modifier.testTag("lb-crew-footer")) { ink ->
            Image(
                painterResource(R.drawable.mascot_run_with_me), contentDescription = null,
                modifier = Modifier.height(46.dp), contentScale = ContentScale.Fit,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.rd_d_my_crew), style = runTextStyle(13.sp, ink.label, FontWeight.Medium, 1.25f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    // "내 크루 · 아직 없어요"(시안) — 크루 찾기 길이 없으면 예전 글 "아직 크루가 없어요" + 설명
                    stringResource(if (onFindCrew != null) R.string.rd_d_none else R.string.lb_crew_none_title), style = runTextStyle(17.sp, ink.text, FontWeight.Bold, 1.25f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (onFindCrew == null) {
                    Text(
                        stringResource(R.string.lb_crew_none_body), style = runTextStyle(12.sp, ink.label, FontWeight.Medium, 1.3f),
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (onFindCrew != null) {
                Row(
                    Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp))
                        .feedbackClickable(onClick = onFindCrew)
                        .padding(start = 6.dp, end = 2.dp)
                        .testTag("lb-crew-find"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.rd_d_find_crew), style = runTextStyle(15.sp, ink.link, FontWeight.Bold),
                        maxLines = 1, softWrap = false,
                    )
                    Icon(LbIcons.ChevronRight, contentDescription = null, tint = ink.link, modifier = Modifier.size(18.dp))
                }
            }
        }
        return
    }
    val ranked = mine.rank != null && board.nearby.isNotEmpty()
    val rise = LeaderboardMath.rankRise(mine.rank, mine.previousRank)
    val sub = when {
        mine.rank == null -> stringResource(R.string.lb_crew_mine_unranked)
        rise != null -> stringResource(R.string.lb_crew_mine_move, moveText(rise) ?: "")
        else -> stringResource(R.string.lb_crew_mine)
    }
    LbFooterSurface(
        onClick = if (ranked) onOpenNearby else null,
        label = if (ranked) stringResource(R.string.lb_crew_nearby_open) else null,
        modifier = Modifier.testTag("lb-crew-footer"),
    ) { ink ->
        Text(
            mine.rank?.toString() ?: "—", style = runNumberStyle(22.sp, ink.link, FontWeight.Bold),
            textAlign = TextAlign.Center, modifier = Modifier.width(34.dp).testTag("lb-crew-footer-rank"), maxLines = 1, softWrap = false,
        )
        Column(Modifier.weight(1f)) {
            Text(sub, style = runTextStyle(12.5.sp, ink.label, FontWeight.Medium, 1.25f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(mine.name, style = runTextStyle(16.sp, ink.text, FontWeight.Bold, 1.25f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (mine.rank != null) LbValue(mine, points = false, color = ink.text, unitColor = ink.label)
        if (ranked) LbFooterChevron(ink)
    }
}

@Composable
private fun CrewNearbySheet(board: CrewBoard) {
    LbSheetNote(stringResource(R.string.lb_crew_nearby_note))
    val mine = stringResource(R.string.lb_crew_mine)
    LbRows(board.nearby) { entry ->
        val runners = stringResource(R.string.lb_crew_runners, entry.participants ?: 0)
        LbRow(entry, points = false, sub = if (entry.isMe) "$mine · $runners" else runners)
    }
}

// ── 개인 러닝 순위 ────────────────────────────────────────────────

@Composable
private fun PersonalPage(
    state: LeaderLoad<RunnerBoard>,
    list: RunnerList,
    onList: (RunnerList) -> Unit,
    loadingMore: Boolean,
    onRetry: () -> Unit,
    onMore: () -> Unit,
    onOpenRuns: () -> Unit,
) {
    val ready = (state as? LeaderLoad.Ready)?.value
    LbPage(
        tag = "lb-personal",
        footer = ready?.let { b -> { PersonalFooter(b, onOpenRuns) } },
    ) {
        // 불러오지 못했으면 기간 줄 없이 빈 화면만(09b)
        if (state !is LeaderLoad.Failed) item(key = "head") { LbMeta(ready?.period, stringResource(R.string.lb_criterion_personal)) }
        item(key = "state") { LbLoadState(state, onRetry) }
        if (ready == null) return@LbPage
        val me = ready.me
        if (me == null) {
            item(key = "unranked") {
                PlainEmptyState(
                    null, stringResource(R.string.lb_personal_unranked_title), Modifier.padding(top = 8.dp).testTag("lb-personal-unranked"),
                    body = stringResource(R.string.lb_personal_unranked_body),
                )
            }
        } else {
            item(key = "hero") { PersonalHero(ready, me) }
            item(key = "next") { NextRunnerCard(ready, me) }
        }
        if (ready.top.isEmpty()) {
            item(key = "empty") {
                PlainEmptyState(
                    null, stringResource(R.string.lb_personal_empty_title), Modifier.padding(top = 8.dp).testTag("lb-personal-empty"),
                    body = stringResource(R.string.lb_personal_empty_body),
                )
            }
            return@LbPage
        }
        // 순위가 없으면 내 주변이 없다 — 상위 러너만(집계 범위는 바꾸지 않는다)
        val shown = if (me == null) RunnerList.TOP else list
        if (me != null) {
            item(key = "toggle") {
                LbUnderlineTabs(
                    labels = listOf(stringResource(R.string.lb_list_nearby), stringResource(R.string.lb_list_top)),
                    selected = shown.ordinal,
                    onSelect = { onList(RunnerList.entries[it]) },
                    modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
                    tagPrefix = "lb-runner-list",
                )
            }
        } else {
            item(key = "top-head") {
                LbListHead(stringResource(R.string.lb_list_top), stringResource(R.string.lb_criterion_personal), Modifier.padding(top = 18.dp, bottom = 8.dp))
            }
        }
        val rows = if (shown == RunnerList.NEARBY) ready.nearby else ready.top
        item(key = "runners-${shown.name}") {
            LbCard { LbRows(rows) { entry -> LbRow(entry, points = false) } }
        }
        if (shown == RunnerList.TOP && ready.total > ready.top.size && ready.top.size < LeaderboardViewModel.MAX_TOP) {
            item(key = "more") { LbMoreButton(loadingMore, onMore, "lb-personal-more") }
        }
    }
}

/** 큰 내 순위 + 지난주 대비 */
@Composable
private fun PersonalHero(board: RunnerBoard, me: LeaderEntry) {
    val t = runTone()
    val rank = me.rank ?: return
    val name = board.myName
    val label = if (name != null) stringResource(R.string.lb_personal_label, name) else stringResource(R.string.lb_personal_label_anon)
    val rankText = stringResource(R.string.lb_rank_text, rank)
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$label · $rankText" }
            .testTag("lb-personal-hero"),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = runTextStyle(14.sp, t.label, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    rank.toString(), style = runNumberStyle(54.sp, t.text),
                    maxLines = 1, softWrap = false, modifier = Modifier.testTag("lb-personal-rank"),
                )
                val suffix = stringResource(R.string.lb_rank_suffix)
                if (suffix.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Text(suffix, style = runTextStyle(20.sp, t.label, FontWeight.SemiBold, 1.2f), modifier = Modifier.padding(bottom = 6.dp))
                }
            }
        }
        RiseBlock(LeaderboardMath.rankRise(me.rank, me.previousRank), isNew = me.previousRank == null)
    }
}

/** 지난주 대비 — 상승 · 하락 · 그대로 · 이번 주 처음(지난주 순위가 없으면 숫자를 만들지 않는다) */
@Composable
private fun RiseBlock(rise: Int?, isNew: Boolean) {
    val t = runTone()
    val text = when {
        rise == null && isNew -> stringResource(R.string.lb_rise_new)
        rise == null -> null
        rise > 0 -> stringResource(R.string.lb_rise_up, rise)
        rise < 0 -> stringResource(R.string.lb_rise_down, -rise)
        else -> stringResource(R.string.lb_rise_same)
    } ?: return
    Text(
        text,
        style = runTextStyle(13.sp, if (rise != null && rise > 0) t.cobaltText else t.label, FontWeight.SemiBold, 1.4f),
        textAlign = TextAlign.End, modifier = Modifier.padding(bottom = 6.dp).testTag("lb-personal-rise"),
    )
}

/** 바로 앞 러너 — 같은 집계의 두 값으로 뺀 차이. 1위면 선두, 0 이면 동점(추월을 약속하는 말은 하지 않는다) */
@Composable
private fun NextRunnerCard(board: RunnerBoard, me: LeaderEntry) {
    val t = runTone()
    val ahead = board.ahead
    LbCard(Modifier.testTag("lb-personal-next")) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 12.dp).semantics(mergeDescendants = true) { },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                if (ahead == null) {
                    Text(stringResource(R.string.lb_next_leader_title), style = runTextStyle(15.5.sp, t.text, FontWeight.SemiBold, 1.3f))
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(R.string.lb_next_leader_sub, board.total), style = runTextStyle(13.sp, t.label, FontWeight.Medium, 1.35f))
                } else {
                    Text(stringResource(R.string.lb_next_title, ahead.name), style = runTextStyle(15.5.sp, t.text, FontWeight.SemiBold, 1.3f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.lb_next_sub, ahead.rank ?: 0, LeaderboardMath.km(ahead.metric)),
                        style = runTextStyle(13.sp, t.label, FontWeight.Medium, 1.35f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (ahead != null) {
                val gap = LeaderboardMath.gapToNext(me.metric, ahead.metric) ?: 0.0
                Column(horizontalAlignment = Alignment.End) {
                    if (gap <= 0.0) {
                        Text(stringResource(R.string.lb_next_tie), style = runTextStyle(18.sp, t.cobaltText, FontWeight.Bold, 1.2f),
                            maxLines = 1, softWrap = false, modifier = Modifier.testTag("lb-personal-gap"))
                        Text(stringResource(R.string.lb_next_tie_caption), style = runTextStyle(12.sp, t.label, FontWeight.Medium), maxLines = 1)
                    } else {
                        LbAmount(LeaderboardMath.gapKm(gap), stringResource(R.string.lb_unit_km), color = t.cobaltText, valueTag = "lb-personal-gap")
                        Text(stringResource(R.string.lb_next_gap_caption), style = runTextStyle(12.sp, t.label, FontWeight.Medium), maxLines = 1)
                    }
                }
            }
        }
    }
}

/** 아래 — 내 이번 주 누적 거리 · 러닝 횟수. 누르면 이번 주 기록 */
@Composable
private fun PersonalFooter(board: RunnerBoard, onOpenRuns: () -> Unit) {
    val name = board.myName ?: stringResource(R.string.lb_me_fallback)
    LbFooterSurface(
        onClick = onOpenRuns, label = stringResource(R.string.lb_personal_records_open),
        modifier = Modifier.testTag("lb-personal-footer"),
    ) { ink ->
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.lb_personal_runs, board.myRuns), style = runTextStyle(12.5.sp, ink.label, FontWeight.Medium, 1.25f),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(name, style = runTextStyle(16.sp, ink.text, FontWeight.Bold, 1.25f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        LbAmount(LeaderboardMath.km(board.myKm), stringResource(R.string.lb_unit_km), color = ink.text, unitColor = ink.label,
            valueTag = "lb-personal-footer-km")
        LbFooterChevron(ink)
    }
}

@Composable
private fun MyRunsSheet(board: RunnerBoard) {
    val t = runTone()
    LbSheetNote(stringResource(R.string.lb_records_note, board.myRuns, LeaderboardMath.km(board.myKm)))
    if (board.myRunList.isEmpty()) {
        Text(stringResource(R.string.lb_records_empty), style = runTextStyle(14.sp, t.label, FontWeight.Medium), modifier = Modifier.padding(vertical = 12.dp))
        return
    }
    val locale = LocalConfiguration.current.locales[0]
    val zone = remember(board.period.timezone) { runCatching { ZoneId.of(board.period.timezone) }.getOrDefault(ZoneId.systemDefault()) }
    val dates = DateTimeFormatter.ofPattern(stringResource(R.string.lb_run_date_pattern), locale)
    board.myRunList.forEachIndexed { i, run ->
        if (i > 0) PlainDivider()
        val day = run.startedAt.atZone(zone)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 58.dp).semantics(mergeDescendants = true) { }.testTag("lb-run-$i"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(dates.format(day), style = runTextStyle(15.5.sp, t.text, FontWeight.SemiBold, 1.3f))
                Text(day.dayOfWeek.getDisplayName(JavaTextStyle.FULL, locale), style = runTextStyle(13.sp, t.label, FontWeight.Medium, 1.3f))
            }
            LbAmount(LeaderboardMath.km(run.km), stringResource(R.string.lb_unit_km))
        }
    }
}

// ── 주간 포인트 ───────────────────────────────────────────────────

@Composable
private fun PointsPage(
    state: LeaderLoad<PointsBoard>,
    scope: PointScope,
    onScope: (PointScope) -> Unit,
    onRetry: () -> Unit,
    onOpenNearby: () -> Unit,
) {
    val ready = (state as? LeaderLoad.Ready)?.value?.takeIf { it.scope == scope }
    LbPage(
        tag = "lb-points",
        footer = ready?.let { b -> { PointsFooter(b, onOpenNearby) } },
    ) {
        // 불러오지 못했으면 기간 줄 없이 빈 화면만(09b)
        if (state !is LeaderLoad.Failed) item(key = "head") { LbMeta(ready?.period, stringResource(R.string.lb_criterion_points)) }
        item(key = "state") {
            LbLoadState(
                state, onRetry,
                unavailableTitle = stringResource(R.string.lb_points_unavailable_title),
                unavailableBody = stringResource(R.string.lb_points_unavailable_body),
            )
        }
        if (ready == null) return@LbPage
        item(key = "hero") { PointsHero(ready) }
        item(key = "scope") {
            LbUnderlineTabs(
                labels = listOf(stringResource(R.string.lb_scope_all), stringResource(R.string.lb_scope_crew)),
                selected = scope.ordinal,
                onSelect = { onScope(PointScope.entries[it]) },
                modifier = Modifier.padding(top = 9.dp),
                trailing = if (scope == PointScope.CREW) ready.crewName else stringResource(R.string.lb_scope_all_label),
                tagPrefix = "lb-scope",
            )
        }
        item(key = "list-head") {
            LbListHead(
                stringResource(if (scope == PointScope.CREW) R.string.lb_points_list_crew else R.string.lb_points_list_all),
                stringResource(R.string.lb_points_list_criterion),
                Modifier.padding(top = 12.dp, bottom = 8.dp),
            )
        }
        item(key = "points-${scope.name}") {
            LbCard {
                LbRows(ready.top) { entry ->
                    if (entry.rank == 1) {
                        LbRow(entry, points = true, sub = stringResource(R.string.lb_points_first), tag = "lb-points-first")
                    } else {
                        LbRow(entry, points = true)
                    }
                }
            }
        }
    }
}

/** 이번 주 적립 · 내 순위 · 지난주 대비(지난주 값이 없으면 말하지 않는다 — 0 으로 쳐서 부풀리지 않는다) */
@Composable
private fun PointsHero(board: PointsBoard) {
    val t = runTone()
    val name = board.myName
    Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp).testTag("lb-points-hero")) {
        Text(
            if (name != null) stringResource(R.string.lb_points_label, name) else stringResource(R.string.lb_points_label_anon),
            style = runTextStyle(14.sp, t.label, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                Text(LeaderboardMath.points(board.myPoints), style = runNumberStyle(54.sp, t.text),
                    maxLines = 1, softWrap = false, modifier = Modifier.testTag("lb-points-mine"))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.lb_unit_points), style = runTextStyle(20.sp, t.label, FontWeight.SemiBold, 1.2f),
                    modifier = Modifier.padding(bottom = 6.dp))
            }
            val rank = board.me?.rank
            if (rank != null) {
                Row(Modifier.padding(bottom = 6.dp).testTag("lb-points-rank"), verticalAlignment = Alignment.Bottom) {
                    Text(rank.toString(), style = runNumberStyle(28.sp, t.cobaltText), maxLines = 1, softWrap = false,
                        modifier = Modifier.testTag("lb-points-rank-value"))
                    val suffix = stringResource(R.string.lb_rank_suffix)
                    if (suffix.isNotEmpty()) {
                        Spacer(Modifier.width(4.dp))
                        Text(suffix, style = runTextStyle(14.sp, t.cobaltText, FontWeight.SemiBold, 1.2f), modifier = Modifier.padding(bottom = 2.dp))
                    }
                }
            }
        }
        val delta = LeaderboardMath.delta(board.myPoints, board.lastWeekPoints)
        if (delta != null) {
            Text(
                when {
                    delta > 0 -> stringResource(R.string.lb_points_more, LeaderboardMath.points(delta))
                    delta < 0 -> stringResource(R.string.lb_points_less, LeaderboardMath.points(-delta))
                    else -> stringResource(R.string.lb_points_same)
                },
                style = runTextStyle(13.sp, if (delta > 0) t.cobaltText else t.label, FontWeight.SemiBold),
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** 아래 — 바로 앞 순위와의 차이(1위면 선두) · 내 주변 순위 */
@Composable
private fun PointsFooter(board: PointsBoard, onOpenNearby: () -> Unit) {
    val me = board.me
    if (me?.rank == null) {
        LbFooterSurface(onClick = null, modifier = Modifier.testTag("lb-points-footer")) { ink ->
            Text(stringResource(R.string.lb_points_unranked), style = runTextStyle(15.sp, ink.text, FontWeight.SemiBold), modifier = Modifier.weight(1f))
        }
        return
    }
    val ahead = board.ahead
    val gap = LeaderboardMath.gapToNext(me.metric, ahead?.metric)
    LbFooterSurface(onClick = onOpenNearby, modifier = Modifier.testTag("lb-points-footer")) { ink ->
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.lb_points_gap_open), style = runTextStyle(12.5.sp, ink.label, FontWeight.Medium, 1.25f), maxLines = 1)
            Text(
                if (ahead == null) stringResource(R.string.lb_points_gap_leader)
                else stringResource(R.string.lb_points_gap_title, ahead.rank ?: 0),
                style = runTextStyle(15.sp, ink.text, FontWeight.Bold, 1.25f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (gap != null) {
            if (gap <= 0.0) {
                Text(stringResource(R.string.lb_next_tie), style = runTextStyle(17.sp, ink.text, FontWeight.Bold), maxLines = 1, softWrap = false)
            } else {
                LbAmount(LeaderboardMath.points(gap), stringResource(R.string.lb_unit_points), color = ink.text, unitColor = ink.label,
                    valueTag = "lb-points-gap")
            }
        }
        LbFooterChevron(ink)
    }
}

@Composable
private fun PointNearbySheet(board: PointsBoard) {
    val scopeLabel = if (board.scope == PointScope.CREW) board.crewName ?: stringResource(R.string.lb_scope_crew) else stringResource(R.string.lb_scope_all)
    LbSheetNote(stringResource(R.string.lb_points_nearby_note, scopeLabel))
    LbRows(board.nearby) { entry ->
        LbRow(entry, points = true, sub = stringResource(if (entry.isMe) R.string.lb_points_mine_sub else R.string.lb_points_other_sub))
    }
}

// ── 순위 기준 시트 ────────────────────────────────────────────────

@Composable
private fun RulesSheet(board: LeaderBoardKind, pointsReady: Boolean, period: LeaderPeriod?) {
    val t = runTone()
    // 목록 위 "11월 4주" 의 날짜 범위(예전 머리 줄의 "이번 주 11.23–11.29")
    if (period != null) LbSheetNote(stringResource(R.string.lb_period, periodRange(period)), Modifier.testTag("lb-rules-period"))
    val rules = when (board) {
        LeaderBoardKind.CREW -> listOf(
            R.string.lb_rules_crew_title to R.string.lb_rules_crew_body,
            R.string.lb_rules_week_title to R.string.lb_rules_week_body,
        )
        LeaderBoardKind.PERSONAL -> listOf(
            R.string.lb_rules_personal_title to R.string.lb_rules_personal_body,
            R.string.lb_rules_week_title to R.string.lb_rules_week_body,
            R.string.lb_rules_tie_title to R.string.lb_rules_tie_body,
        )
        // 앱의 주간 포인트는 준비 중이다(2026-10-17 사용자 결정) — 순위가 없으면 정하지 않은 P 기준을 설명하지 않는다
        LeaderBoardKind.POINTS -> if (pointsReady) {
            listOf(R.string.lb_rules_points_title to R.string.lb_rules_points_body)
        } else {
            listOf(R.string.lb_points_unavailable_title to R.string.lb_points_unavailable_body)
        }
    }
    rules.forEachIndexed { i, (title, body) ->
        if (i > 0) PlainDivider()
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp).testTag("lb-rule-$i")) {
            Text(stringResource(title), style = runTextStyle(15.5.sp, t.text, FontWeight.SemiBold, 1.3f))
            Spacer(Modifier.height(4.dp))
            Text(stringResource(body), style = runTextStyle(14.sp, t.label, FontWeight.Medium, 1.55f))
        }
    }
}

// ── 시트 · 아이콘 ─────────────────────────────────────────────────

/** 상세 시트 — 앱 시트(손잡이 · 닫기 ×) · 왼쪽 제목. 바깥 · 뒤로 · 닫기로 닫힌다 */
@Composable
private fun LbSheetHost(title: String, tag: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    // 잠금 화면 덮개가 덮고 있으면 창을 띄우지 않는다(LockCover — RunSheet 안에서도 다시 본다)
    if (com.stepup.android.ui.components.LockCover.hidesWindows()) return
    val t = runTone()
    RunSheet(onDismiss = onDismiss, closeTag = "lb-sheet-close") {
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()).testTag(tag)) {
            Text(
                title, style = runTextStyle(22.sp, t.text, FontWeight.Bold, 1.25f),
                modifier = Modifier.padding(end = 44.dp, bottom = 10.dp).semantics { heading() },
            )
            content()
        }
    }
}

/** 지난 기간 대비 짧은 글 — "n계단 상승" 등. 지난 순위가 없으면 [newWhenUnknown] 일 때만 "이번 주 첫 순위" */
@Composable
private fun moveText(rise: Int?, newWhenUnknown: Boolean = false): String? = when {
    rise == null && newWhenUnknown -> stringResource(R.string.lb_move_new)
    rise == null -> null
    rise > 0 -> stringResource(R.string.lb_move_up, rise)
    rise < 0 -> stringResource(R.string.lb_move_down, -rise)
    else -> stringResource(R.string.lb_move_same)
}

/** 선 아이콘(lucide, 24 격자 · 두께 2 · 둥근 끝) */
internal object LbIcons {
    val ChevronRight = icon("chevron-right", "m9 18 6-6-6-6")
    val Info = icon("info", "M22 12A10 10 0 1 1 2 12A10 10 0 1 1 22 12z", "M12 16v-4", "M12 8h.01")

    private fun icon(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name = "lb-$name", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .apply {
                paths.forEach { d ->
                    addPath(
                        pathData = addPathNodes(d), fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
                    )
                }
            }
            .build()
}
