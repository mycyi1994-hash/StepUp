package com.stepup.android.ui.screens.leaderboard

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.CrewBoard
import com.stepup.android.domain.LeaderEntry
import com.stepup.android.domain.LeaderLoad
import com.stepup.android.domain.LeaderPeriod
import com.stepup.android.domain.LeaderboardMath
import com.stepup.android.domain.PointScope
import com.stepup.android.domain.PointsBoard
import com.stepup.android.domain.RunnerBoard
import com.stepup.android.ui.components.RunTopBar
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.ui.experience.feedbackClickable
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle

/*
 * 리더보드(2026-10-17 전달본) — 크루 순위 · 개인 러닝 순위 · 주간 포인트 세 판. 늘 딥 네이비 한 벌이다.
 *
 * 맨 아래 → 위: 네이비 · 움직이는 트랙 배경(판이 바뀌어도 같은 시계로 잇는다) → 머리 · 판 고르기 · 내용 → 아래 내 줄 → 시트.
 * 시트가 열려 있거나 · 앱이 뒤에 있거나 · 화면을 떠났거나 · 모션을 줄였으면 배경이 멈춘다(모션 감소면 처음부터 정지 화면).
 * 순위 · 앞뒤 · 기간은 서버 값 그대로 — 받은 목록 안에서 순위를 다시 세지 않는다([LeaderboardSource]).
 */

private enum class LbSheet { RULES, CREW_NEARBY, MY_RUNS, POINT_NEARBY }

/** 판마다 배경 트랙의 자리 · 세기 */
private val LeaderBoardKind.scene: LeaderboardMotion.Scene
    get() = when (this) {
        LeaderBoardKind.CREW -> LeaderboardMotion.Scene.CREW
        LeaderBoardKind.PERSONAL -> LeaderboardMotion.Scene.PERSONAL
        LeaderBoardKind.POINTS -> LeaderboardMotion.Scene.POINTS
    }

@Composable
fun LeaderboardScreen(
    onBack: () -> Unit,
    initial: LeaderBoardKind = LeaderBoardKind.CREW,
    viewModel: LeaderboardViewModel = viewModel(),
) {
    var board by rememberSaveable { mutableStateOf(initial) }
    var sheet by rememberSaveable { mutableStateOf<LbSheet?>(null) }
    var runnerList by rememberSaveable { mutableStateOf(RunnerList.NEARBY) }
    LaunchedEffect(viewModel, board) { viewModel.open(board) }

    val crew by viewModel.crew.collectAsStateWithLifecycle()
    val runners by viewModel.runners.collectAsStateWithLifecycle()
    val points by viewModel.points.collectAsStateWithLifecycle()
    val scope by viewModel.scope.collectAsStateWithLifecycle()
    val more by viewModel.more.collectAsStateWithLifecycle()

    val reduced = LocalMotion.current.reduced
    // 시계 하나 — 판을 바꾸거나 목록이 새로 와도 처음부터 다시 돌지 않는다. 시트를 읽는 동안 · 모션 감소면 멈춘다
    val clock = rememberLeaderboardClock(running = !reduced && sheet == null)
    val now = { clock.longValue }
    LightSystemBarIcons()

    BoxWithConstraints(Modifier.fillMaxSize().background(Lb.Navy).testTag("leaderboard")) {
        val compact = maxWidth < 380.dp
        val gutter = if (compact) 17.dp else Lb.Gutter
        LeaderboardBackdrop(board.scene, clock = now, reduced = reduced, modifier = Modifier.fillMaxSize().testTag("lb-backdrop"))
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            RunTopBar(
                onBack = onBack,
                onNavy = true,
                trailing = { LbIconButton(LbIcons.Info, stringResource(R.string.lb_rules_open), "lb-info") { sheet = LbSheet.RULES } },
            )
            LbUnderlineTabs(
                labels = listOf(
                    stringResource(R.string.lb_board_crew),
                    stringResource(R.string.lb_board_personal),
                    stringResource(R.string.lb_board_points),
                ),
                selected = board.ordinal,
                onSelect = { board = LeaderBoardKind.entries[it] },
                modifier = Modifier.padding(horizontal = gutter),
                tagPrefix = "lb-board",
            )
            when (board) {
                LeaderBoardKind.CREW -> CrewPage(
                    state = crew, compact = compact, gutter = gutter, clock = now, reduced = reduced,
                    loadingMore = LeaderBoardKind.CREW in more,
                    onRetry = { viewModel.retry(LeaderBoardKind.CREW) },
                    onMore = { viewModel.loadMore(LeaderBoardKind.CREW) },
                    onOpenNearby = { sheet = LbSheet.CREW_NEARBY },
                )
                LeaderBoardKind.PERSONAL -> PersonalPage(
                    state = runners, compact = compact, gutter = gutter, list = runnerList,
                    onList = { runnerList = it },
                    loadingMore = LeaderBoardKind.PERSONAL in more,
                    onRetry = { viewModel.retry(LeaderBoardKind.PERSONAL) },
                    onMore = { viewModel.loadMore(LeaderBoardKind.PERSONAL) },
                    onOpenRuns = { sheet = LbSheet.MY_RUNS },
                )
                LeaderBoardKind.POINTS -> PointsPage(
                    state = points, scope = scope, compact = compact, gutter = gutter,
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
                LbSheet.RULES -> RulesSheet(board)
                LbSheet.CREW_NEARBY -> (crew as? LeaderLoad.Ready)?.value?.let { CrewNearbySheet(it) }
                LbSheet.MY_RUNS -> (runners as? LeaderLoad.Ready)?.value?.let { MyRunsSheet(it) }
                LbSheet.POINT_NEARBY -> (points as? LeaderLoad.Ready)?.value?.let { PointNearbySheet(it) }
            }
        }
    }
}

// ── 공통 — 머리 · 기간 · 상태 ─────────────────────────────────────

@Composable
private fun LbHeading(title: String, subtitle: String?, period: LeaderPeriod?, criterion: String, compact: Boolean) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Text(
            title,
            style = lbText(if (compact) 28.sp else 30.sp, Lb.White, FontWeight.Bold, height = 1.18f, spacing = (-0.037).em),
            modifier = Modifier.semantics { heading() }.testTag("lb-title"),
        )
        if (subtitle != null) {
            Spacer(Modifier.height(7.dp))
            Text(subtitle, style = lbText(12.sp, Lb.Muted))
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp).heightIn(min = 23.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                if (period == null) stringResource(R.string.lb_period_pending) else stringResource(R.string.lb_period, periodLabel(period)),
                style = lbText(12.sp, Lb.Ice, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).testTag("lb-period"),
            )
            Text(criterion, style = lbText(12.sp, Lb.Muted), maxLines = 1, modifier = Modifier.testTag("lb-criterion"))
        }
    }
}

/** "10.12–10.18" — 서버가 준 주의 첫날 · 마지막 날 */
private fun periodLabel(period: LeaderPeriod): String =
    "${period.startDate.monthValue}.${period.startDate.dayOfMonth}–${period.lastDate.monthValue}.${period.lastDate.dayOfMonth}"

/** 불러오는 중 · 실패 · 로그인 · 준비 중 — 지어낸 숫자 대신 이 칸을 보인다. 그 밖의 상태(Ready)면 false */
@Composable
private fun <T> LbLoadState(state: LeaderLoad<T>, onRetry: () -> Unit, unavailableTitle: String? = null, unavailableBody: String? = null): Boolean {
    when (state) {
        LeaderLoad.Loading -> Box(Modifier.fillMaxWidth().padding(vertical = 48.dp).testTag("lb-loading"), contentAlignment = Alignment.Center) {
            val label = stringResource(R.string.lb_loading)
            CircularProgressIndicator(
                color = Lb.Underline, trackColor = Lb.Line, strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp).semantics { contentDescription = label },
            )
        }
        is LeaderLoad.Failed -> LbStateBox(
            stringResource(R.string.lb_failed_title),
            if (state.offline) stringResource(R.string.lb_failed_offline) else null,
            modifier = Modifier.padding(top = 18.dp),
            action = stringResource(R.string.lb_retry), onAction = onRetry, tag = "lb-failed",
        )
        LeaderLoad.SignedOut -> LbStateBox(
            stringResource(R.string.lb_signed_out_title), stringResource(R.string.lb_signed_out_body),
            modifier = Modifier.padding(top = 18.dp), tag = "lb-signed-out",
        )
        LeaderLoad.Unavailable -> LbStateBox(
            unavailableTitle ?: stringResource(R.string.lb_unavailable_title),
            unavailableBody ?: stringResource(R.string.lb_unavailable_body),
            modifier = Modifier.padding(top = 18.dp), tag = "lb-unavailable",
        )
        is LeaderLoad.Ready -> return false
    }
    return true
}

/** 판 하나의 틀 — 넘기는 내용 + 아래 고정 내 줄(목록을 덮지 않게 내용 밖에 둔다) */
@Composable
private fun LbPage(
    gutter: Dp,
    tag: String,
    footer: (@Composable () -> Unit)?,
    content: LazyListScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().testTag(tag)) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().testTag("$tag-list"),
            contentPadding = PaddingValues(start = gutter, end = gutter, bottom = 20.dp),
            content = content,
        )
        if (footer != null) {
            Box(Modifier.fillMaxWidth().padding(start = gutter, end = gutter, top = 8.dp, bottom = 16.dp)) { footer() }
        }
    }
}

@Composable
private fun LbMoreButton(loading: Boolean, onMore: () -> Unit, tag: String) {
    Box(
        Modifier.fillMaxWidth().padding(top = 10.dp).heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
            .border(1.dp, Lb.Line, RoundedCornerShape(12.dp))
            .feedbackClickable(enabled = !loading, onClick = onMore).testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = Lb.Underline, trackColor = Lb.Line, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Text(stringResource(R.string.lb_more), style = lbText(13.sp, Lb.Ice, FontWeight.SemiBold))
        }
    }
}

// ── 크루 순위 ─────────────────────────────────────────────────────

@Composable
private fun CrewPage(
    state: LeaderLoad<CrewBoard>,
    compact: Boolean,
    gutter: Dp,
    clock: () -> Long,
    reduced: Boolean,
    loadingMore: Boolean,
    onRetry: () -> Unit,
    onMore: () -> Unit,
    onOpenNearby: () -> Unit,
) {
    val ready = (state as? LeaderLoad.Ready)?.value
    LbPage(
        gutter = gutter, tag = "lb-crew",
        footer = ready?.let { b -> { CrewFooter(b, onOpenNearby) } },
    ) {
        item(key = "head") {
            LbHeading(
                stringResource(R.string.lb_title_crew), stringResource(R.string.lb_sub_crew), ready?.period,
                stringResource(R.string.lb_criterion_crew), compact,
            )
        }
        item(key = "state") { LbLoadState(state, onRetry) }
        if (ready == null) return@LbPage
        if (ready.top.isEmpty()) {
            item(key = "empty") {
                LbStateBox(
                    stringResource(R.string.lb_crew_empty_title), stringResource(R.string.lb_crew_empty_body),
                    modifier = Modifier.padding(top = 18.dp), tag = "lb-crew-empty",
                )
            }
            return@LbPage
        }
        item(key = "winner") { CrewWinner(ready.top.first(), compact, clock, reduced) }
        if (ready.top.size > 1) {
            item(key = "runners-up") { CrewRunnersUp(ready.top.getOrNull(1), ready.top.getOrNull(2), compact) }
        }
        val rest = ready.top.drop(3)
        if (rest.isNotEmpty()) {
            item(key = "list-head") {
                LbListHead(
                    stringResource(R.string.lb_crew_list_head), stringResource(R.string.lb_crew_list_criterion),
                    Modifier.padding(top = 18.dp, bottom = 5.dp),
                )
            }
            items(rest, key = { "crew-${it.id}" }) { entry ->
                LbRow(
                    entry, points = false, crew = true, minHeight = 58.dp, stackedUnit = true,
                    sub = stringResource(R.string.lb_crew_runners, entry.participants ?: 0),
                    avatar = { LbCrewAvatar(entry, 36.dp, 11.dp, if (entry.isMe) Color(0x29FFFFFF) else Lb.CrewAvatar, 14.sp) },
                )
            }
        }
        if (ready.total > ready.top.size && ready.top.size < LeaderboardViewModel.MAX_TOP) {
            item(key = "more") { LbMoreButton(loadingMore, onMore, "lb-crew-more") }
        }
    }
}

/** 1위 — 전체 너비 블루 면 · 왕관 · 크루 그림 · 이름 · 기록 · 뒤의 큰 기울인 1 · 지나가는 빛 띠(모션 감소면 없음) */
@Composable
private fun CrewWinner(entry: LeaderEntry, compact: Boolean, clock: () -> Long, reduced: Boolean) {
    val shape = RoundedCornerShape(22.dp)
    val km = LeaderboardMath.km(entry.metric)
    val unit = stringResource(R.string.lb_unit_km)
    val first = stringResource(R.string.lb_crew_first)
    Box(
        Modifier.fillMaxWidth().padding(top = 18.dp).heightIn(min = 190.dp).clip(shape).background(Lb.Blue, shape)
            .drawWithCache {
                val band = championSheenBrush(90.dp.toPx())
                onDrawWithContent {
                    if (!reduced) drawChampionSheen(clock(), band)
                    drawContent()
                }
            }
            .semantics(mergeDescendants = true) { contentDescription = "$first · ${entry.name} · $km$unit" }
            .testTag("lb-crew-winner"),
    ) {
        // 배경 숫자 1 — 이름 · 기록보다 뒤(먼저 그린다), 오른쪽 아래. 카드 높이를 정하지 않는다(넘치는 곳은 카드가 자른다)
        Box(Modifier.matchParentSize()) {
            Text(
                "1",
                style = lbNumber(188.sp, Color(0x38FFFFFF), FontWeight.Black, italic = true, spacing = (-0.053).em),
                modifier = Modifier.align(Alignment.BottomEnd).wrapContentSize(Alignment.BottomEnd, unbounded = true)
                    .offset(x = (-10).dp, y = 15.dp).clearAndSetSemantics { },
                maxLines = 1, softWrap = false,
            )
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = if (compact) 16.dp else 20.dp, vertical = 18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(LbIcons.Crown, contentDescription = null, tint = Lb.White, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text(first, style = lbText(12.sp, Lb.White, FontWeight.SemiBold), modifier = Modifier.weight(1f), maxLines = 1)
                LbMove(LeaderboardMath.rankRise(entry.rank, entry.previousRank), color = Lb.White, newWhenUnknown = entry.previousRank == null)
            }
            Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                LbCrewAvatar(entry, 52.dp, 14.dp, Color(0x23FFFFFF), 27.sp)
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.name, style = lbText(if (compact) 21.sp else 23.sp, Lb.White, FontWeight.Bold, 1.2f, (-0.035).em),
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("lb-crew-winner-name"),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.lb_crew_winner_sub, entry.participants ?: 0),
                        style = lbText(12.sp, Lb.White), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(Modifier.padding(top = 13.dp), verticalAlignment = Alignment.Bottom) {
                Text(km, style = lbNumber(44.sp, Lb.White, FontWeight.Bold, spacing = (-0.023).em), maxLines = 1,
                    modifier = Modifier.testTag("lb-crew-winner-value"))
                Spacer(Modifier.width(5.dp))
                Text(unit, style = lbText(14.sp, Lb.White, FontWeight.SemiBold), modifier = Modifier.padding(bottom = 4.dp))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.lb_crew_score_caption), style = lbText(11.sp, Lb.White),
                    modifier = Modifier.padding(bottom = 5.dp), maxLines = 1)
            }
        }
    }
}

/** 2 · 3위 — 같은 크기로 나란히(2위 숫자는 밝은 아이스, 3위는 한 단계 낮게) */
@Composable
private fun CrewRunnersUp(second: LeaderEntry?, third: LeaderEntry?, compact: Boolean) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(second, third).forEachIndexed { i, entry ->
            if (entry == null) {
                Spacer(Modifier.weight(1f))
                return@forEachIndexed
            }
            val shape = RoundedCornerShape(16.dp)
            Row(
                Modifier.weight(1f).heightIn(min = 87.dp).clip(shape)
                    .background(if (i == 0) Color(0xFF153752) else Color(0xFF102C46), shape)
                    .padding(horizontal = if (compact) 10.dp else 12.dp, vertical = 13.dp)
                    .semantics(mergeDescendants = true) { }
                    .testTag("lb-crew-podium-${entry.rank}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
            ) {
                Text(
                    entry.rank?.toString() ?: "—",
                    style = lbNumber(37.sp, if (i == 0) Color(0xFFC9E0FF) else Color(0xFF99B6D3), FontWeight.Bold, italic = true,
                        spacing = (-0.054).em),
                    modifier = Modifier.widthIn(min = 25.dp), maxLines = 1,
                )
                Column(Modifier.weight(1f)) {
                    Text(entry.name, style = lbText(12.sp, Lb.White, FontWeight.SemiBold, 1.25f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(7.dp))
                    LbValue(entry, points = false, size = if (compact) 22.sp else 24.sp)
                }
            }
        }
    }
}

/** 아래 우리 크루 — 누르면 우리 크루 앞뒤. 크루가 없거나 이번 주 기록이 없으면 순위 없이 말한다 */
@Composable
private fun CrewFooter(board: CrewBoard, onOpenNearby: () -> Unit) {
    val mine = board.mine
    if (mine == null) {
        LbFooterSurface(onClick = null, modifier = Modifier.testTag("lb-crew-footer")) {
            Column {
                Text(stringResource(R.string.lb_crew_none_title), style = lbText(13.sp, Lb.White, FontWeight.SemiBold))
                Spacer(Modifier.height(3.dp))
                Text(stringResource(R.string.lb_crew_none_body), style = lbText(11.sp, Lb.Muted))
            }
        }
        return
    }
    val ranked = mine.rank != null && board.nearby.isNotEmpty()
    val rise = LeaderboardMath.rankRise(mine.rank, mine.previousRank)
    val sub = when {
        mine.rank == null -> stringResource(R.string.lb_crew_mine_unranked)
        rise != null -> stringResource(R.string.lb_crew_mine_move, moveText(rise))
        else -> stringResource(R.string.lb_crew_mine)
    }
    LbFooterSurface(
        onClick = if (ranked) onOpenNearby else null,
        label = if (ranked) stringResource(R.string.lb_crew_nearby_open) else null,
        modifier = Modifier.testTag("lb-crew-footer"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                mine.rank?.toString() ?: "—", style = lbNumber(22.sp, Lb.White, FontWeight.SemiBold),
                textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 30.dp).testTag("lb-crew-footer-rank"), maxLines = 1,
            )
            Column(Modifier.weight(1f)) {
                Text(mine.name, style = lbText(13.sp, Lb.White, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(sub, style = lbText(11.sp, Lb.Muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (mine.rank != null) LbValue(mine, points = false, size = 23.sp, inlineUnit = false)
            if (ranked) Icon(LbIcons.ChevronRight, contentDescription = null, tint = Lb.Ice, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun CrewNearbySheet(board: CrewBoard) {
    LbSheetNote(stringResource(R.string.lb_crew_nearby_note))
    board.nearby.forEach { entry ->
        LbRow(
            entry, points = false, crew = true, minHeight = 61.dp, stackedUnit = true, sheet = true,
            sub = if (entry.isMe) {
                "${stringResource(R.string.lb_crew_mine)} · ${stringResource(R.string.lb_crew_runners, entry.participants ?: 0)}"
            } else {
                stringResource(R.string.lb_crew_runners, entry.participants ?: 0)
            },
            avatar = { LbCrewAvatar(entry, 36.dp, 11.dp, if (entry.isMe) Color(0x29FFFFFF) else Lb.CrewAvatar, 14.sp) },
        )
    }
}

/** 크루 그림 — 대표 사진이 있으면 사진, 없으면 기울인 모노그램 */
@Composable
private fun LbCrewAvatar(entry: LeaderEntry, size: Dp, radius: Dp, face: Color, textSize: TextUnit) {
    var photo by remember(entry.id, entry.photoVersion) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(entry.id, entry.photoVersion) {
        val version = entry.photoVersion ?: return@LaunchedEffect
        photo = runCatching { ServiceLocator.crewCards.images.bitmap(entry.id, version)?.asImageBitmap() }.getOrNull()
    }
    val shape = RoundedCornerShape(radius)
    Box(Modifier.size(size).clip(shape).background(face, shape), contentAlignment = Alignment.Center) {
        val image = photo
        if (image != null) {
            androidx.compose.foundation.Image(image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(
                entry.mark.take(2),
                style = lbText(textSize, Lb.AvatarInk, FontWeight.ExtraBold, 1f).copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                maxLines = 1,
            )
        }
    }
}

// ── 개인 러닝 순위 ────────────────────────────────────────────────

@Composable
private fun PersonalPage(
    state: LeaderLoad<RunnerBoard>,
    compact: Boolean,
    gutter: Dp,
    list: RunnerList,
    onList: (RunnerList) -> Unit,
    loadingMore: Boolean,
    onRetry: () -> Unit,
    onMore: () -> Unit,
    onOpenRuns: () -> Unit,
) {
    val ready = (state as? LeaderLoad.Ready)?.value
    LbPage(
        gutter = gutter, tag = "lb-personal",
        footer = ready?.let { b -> { PersonalFooter(b, onOpenRuns) } },
    ) {
        item(key = "head") {
            LbHeading(stringResource(R.string.lb_title_personal), null, ready?.period, stringResource(R.string.lb_criterion_personal), compact)
        }
        item(key = "state") { LbLoadState(state, onRetry) }
        if (ready == null) return@LbPage
        val me = ready.me
        if (me == null) {
            item(key = "unranked") {
                LbStateBox(
                    stringResource(R.string.lb_personal_unranked_title), stringResource(R.string.lb_personal_unranked_body),
                    modifier = Modifier.padding(top = 18.dp), tag = "lb-personal-unranked",
                )
            }
        } else {
            item(key = "hero") { PersonalHero(ready, me, compact) }
            item(key = "next") { NextRunnerCard(ready, me) }
        }
        if (ready.top.isEmpty()) {
            item(key = "empty") {
                LbStateBox(
                    stringResource(R.string.lb_personal_empty_title), stringResource(R.string.lb_personal_empty_body),
                    modifier = Modifier.padding(top = 18.dp), tag = "lb-personal-empty",
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
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    tagPrefix = "lb-runner-list",
                )
            }
        } else {
            item(key = "top-head") {
                LbListHead(stringResource(R.string.lb_list_top), stringResource(R.string.lb_criterion_personal), Modifier.padding(top = 18.dp, bottom = 5.dp))
            }
        }
        val rows = if (shown == RunnerList.NEARBY) ready.nearby else ready.top
        items(rows, key = { "runner-${shown.name}-${it.id}" }) { entry ->
            LbRow(
                entry, points = false, minHeight = 46.dp, rankWidth = 34.dp, rankSize = 16.sp, rankColor = Color(0xFFA9BFD8),
                avatarSize = 30.dp, nameSize = 14.sp, valueSize = 21.sp, divider = false, ladder = true,
                modifier = Modifier.background(Lb.Navy),
            )
        }
        if (shown == RunnerList.TOP && ready.total > ready.top.size && ready.top.size < LeaderboardViewModel.MAX_TOP) {
            item(key = "more") { LbMoreButton(loadingMore, onMore, "lb-personal-more") }
        }
    }
}

/** 큰 내 순위 + 지난주 대비 */
@Composable
private fun PersonalHero(board: RunnerBoard, me: LeaderEntry, compact: Boolean) {
    val rank = me.rank ?: return
    val name = board.myName
    val label = if (name != null) stringResource(R.string.lb_personal_label, name) else stringResource(R.string.lb_personal_label_anon)
    val rankText = stringResource(R.string.lb_rank_text, rank)
    Row(
        Modifier.fillMaxWidth().padding(top = 14.dp).heightIn(min = 152.dp).padding(top = 8.dp, bottom = 10.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$label · $rankText" }
            .testTag("lb-personal-hero"),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = lbText(12.sp, Lb.Muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Row {
                Text(
                    rank.toString(),
                    style = lbNumber(if (compact) 104.sp else 112.sp, Lb.White, FontWeight.Bold, italic = true, spacing = (-0.054).em),
                    maxLines = 1, softWrap = false, modifier = Modifier.alignByBaseline().testTag("lb-personal-rank"),
                )
                val suffix = stringResource(R.string.lb_rank_suffix)
                if (suffix.isNotEmpty()) {
                    Spacer(Modifier.width(7.dp))
                    Text(suffix, style = lbText(17.sp, Lb.White, FontWeight.SemiBold, 1f), modifier = Modifier.alignByBaseline())
                }
            }
        }
        RiseBlock(LeaderboardMath.rankRise(me.rank, me.previousRank), isNew = me.previousRank == null, compact = compact)
    }
}

/** 지난주 대비 — 상승 · 하락 · 그대로 · 이번 주 처음(지난주 순위가 없으면 숫자를 만들지 않는다) */
@Composable
private fun RiseBlock(rise: Int?, isNew: Boolean, compact: Boolean) {
    val accent = Color(0xFF8BB3FF)
    Column(Modifier.padding(bottom = 9.dp).testTag("lb-personal-rise"), horizontalAlignment = Alignment.End) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            when {
                rise == null && isNew -> Text(stringResource(R.string.lb_rise_new_mark), style = lbNumber(if (compact) 22.sp else 24.sp, accent, FontWeight.Bold))
                rise == null -> Unit
                rise > 0 -> {
                    Icon(LbIcons.ArrowUpRight, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                    Text(rise.toString(), style = lbNumber(if (compact) 30.sp else 34.sp, accent, FontWeight.SemiBold))
                }
                rise < 0 -> {
                    Icon(LbIcons.ArrowDownRight, contentDescription = null, tint = Lb.Muted, modifier = Modifier.size(22.dp))
                    Text((-rise).toString(), style = lbNumber(if (compact) 30.sp else 34.sp, Lb.Muted, FontWeight.SemiBold))
                }
                else -> Icon(LbIcons.Minus, contentDescription = null, tint = Lb.Muted, modifier = Modifier.size(22.dp))
            }
        }
        val text = when {
            rise == null && isNew -> stringResource(R.string.lb_rise_new)
            rise == null -> null
            rise > 0 -> stringResource(R.string.lb_rise_up, rise)
            rise < 0 -> stringResource(R.string.lb_rise_down, -rise)
            else -> stringResource(R.string.lb_rise_same)
        }
        if (text != null) {
            Spacer(Modifier.height(6.dp))
            Text(text, style = lbText(11.sp, Lb.Muted, height = 1.5f), textAlign = TextAlign.End)
        }
    }
}

/** 바로 앞 러너 — 같은 집계의 두 값으로 뺀 차이. 1위면 선두, 0 이면 동점(추월을 약속하는 말은 하지 않는다) */
@Composable
private fun NextRunnerCard(board: RunnerBoard, me: LeaderEntry) {
    val shape = RoundedCornerShape(14.dp)
    val ahead = board.ahead
    Row(
        Modifier.fillMaxWidth().padding(top = 9.dp).heightIn(min = 66.dp).clip(shape).background(Lb.Blue, shape)
            .padding(horizontal = 15.dp, vertical = 12.dp).semantics(mergeDescendants = true) { }.testTag("lb-personal-next"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (ahead == null) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.lb_next_leader_title), style = lbText(13.sp, Lb.White, FontWeight.SemiBold))
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.lb_next_leader_sub, board.total), style = lbText(11.sp, Color(0xFFDCE8FF)))
            }
            return@Row
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.lb_next_title, ahead.name), style = lbText(13.sp, Lb.White, FontWeight.SemiBold),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.lb_next_sub, ahead.rank ?: 0, LeaderboardMath.km(ahead.metric)),
                style = lbText(11.sp, Color(0xFFDCE8FF)), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        val gap = LeaderboardMath.gapToNext(me.metric, ahead.metric) ?: 0.0
        Column(horizontalAlignment = Alignment.End) {
            if (gap <= 0.0) {
                Text(stringResource(R.string.lb_next_tie), style = lbNumber(28.sp, Lb.White, FontWeight.SemiBold), modifier = Modifier.testTag("lb-personal-gap"))
                Spacer(Modifier.height(3.dp))
                Text(stringResource(R.string.lb_next_tie_caption), style = lbText(11.sp, Color(0xFFDCE8FF)))
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(LeaderboardMath.gapKm(gap), style = lbNumber(35.sp, Lb.White, FontWeight.SemiBold, spacing = (-0.023).em),
                        modifier = Modifier.testTag("lb-personal-gap"), maxLines = 1)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.lb_unit_km), style = lbText(12.sp, Lb.White, FontWeight.Medium), modifier = Modifier.padding(bottom = 3.dp))
                }
                Spacer(Modifier.height(3.dp))
                Text(stringResource(R.string.lb_next_gap_caption), style = lbText(11.sp, Color(0xFFDCE8FF)))
            }
        }
    }
}

/** 아래 — 내 이번 주 누적 거리 · 러닝 횟수. 누르면 이번 주 기록 */
@Composable
private fun PersonalFooter(board: RunnerBoard, onOpenRuns: () -> Unit) {
    val name = board.myName ?: stringResource(R.string.lb_me_fallback)
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Lb.Line))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 60.dp).feedbackClickable(onClick = onOpenRuns)
                .semantics(mergeDescendants = true) { }.padding(top = 14.dp).testTag("lb-personal-footer"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LbAvatar(LeaderboardMath.personMark(name, null), size = 36.dp, face = Color(0xFF1C4779))
            Column(Modifier.weight(1f)) {
                Text(name, style = lbText(14.sp, Lb.White, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(stringResource(R.string.lb_personal_runs, board.myRuns), style = lbText(11.sp, Lb.Muted), maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(LeaderboardMath.km(board.myKm), style = lbNumber(32.sp, Lb.White, FontWeight.SemiBold), maxLines = 1,
                    modifier = Modifier.testTag("lb-personal-footer-km"))
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.lb_unit_km), style = lbText(11.sp, Lb.Muted))
            }
        }
    }
}

@Composable
private fun MyRunsSheet(board: RunnerBoard) {
    LbSheetNote(stringResource(R.string.lb_records_note, board.myRuns, LeaderboardMath.km(board.myKm)))
    if (board.myRunList.isEmpty()) {
        Text(stringResource(R.string.lb_records_empty), style = lbText(13.sp, Lb.Ice), modifier = Modifier.padding(vertical = 12.dp))
        return
    }
    val locale = LocalConfiguration.current.locales[0]
    val zone = remember(board.period.timezone) { runCatching { ZoneId.of(board.period.timezone) }.getOrDefault(ZoneId.systemDefault()) }
    val dates = DateTimeFormatter.ofPattern(stringResource(R.string.lb_run_date_pattern), locale)
    board.myRunList.forEachIndexed { i, run ->
        val day = run.startedAt.atZone(zone)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 61.dp)
                .then(if (i < board.myRunList.lastIndex) Modifier.lbDivider(Color(0xFF34516B)) else Modifier)
                .semantics(mergeDescendants = true) { }.testTag("lb-run-$i"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(dates.format(day), style = lbText(14.sp, Lb.White, FontWeight.SemiBold))
                Spacer(Modifier.height(4.dp))
                Text(day.dayOfWeek.getDisplayName(JavaTextStyle.FULL, locale), style = lbText(11.sp, Lb.Muted))
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(LeaderboardMath.km(run.km), style = lbNumber(22.sp, Lb.White, FontWeight.SemiBold))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.lb_unit_km), style = lbText(11.sp, Lb.Muted), modifier = Modifier.padding(bottom = 1.dp))
            }
        }
    }
}

// ── 주간 포인트 ───────────────────────────────────────────────────

@Composable
private fun PointsPage(
    state: LeaderLoad<PointsBoard>,
    scope: PointScope,
    compact: Boolean,
    gutter: Dp,
    onScope: (PointScope) -> Unit,
    onRetry: () -> Unit,
    onOpenNearby: () -> Unit,
) {
    val ready = (state as? LeaderLoad.Ready)?.value?.takeIf { it.scope == scope }
    LbPage(
        gutter = gutter, tag = "lb-points",
        footer = ready?.let { b -> { PointsFooter(b, onOpenNearby) } },
    ) {
        item(key = "head") {
            LbHeading(
                stringResource(R.string.lb_title_points), stringResource(R.string.lb_sub_points), ready?.period,
                stringResource(R.string.lb_criterion_points), compact,
            )
        }
        item(key = "state") {
            LbLoadState(
                state, onRetry,
                unavailableTitle = stringResource(R.string.lb_points_unavailable_title),
                unavailableBody = stringResource(R.string.lb_points_unavailable_body),
            )
        }
        if (ready == null) return@LbPage
        item(key = "hero") { PointsHero(ready, compact) }
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
                Modifier.padding(top = 12.dp, bottom = 3.dp),
            )
        }
        items(ready.top, key = { "points-${scope.name}-${it.id}" }) { entry ->
            if (entry.rank == 1) {
                PointsFirstRow(entry)
            } else {
                LbRow(entry, points = true, minHeight = 48.dp, valueSize = 22.sp, modifier = Modifier.background(Lb.Navy))
            }
        }
    }
}

/** 이번 주 적립 · 내 순위 · 지난주 대비(지난주 값이 없으면 말하지 않는다 — 0 으로 쳐서 부풀리지 않는다) */
@Composable
private fun PointsHero(board: PointsBoard, compact: Boolean) {
    val name = board.myName
    Column(
        Modifier.fillMaxWidth().padding(top = 11.dp).lbDivider(Lb.Line).padding(top = 16.dp, bottom = 18.dp).testTag("lb-points-hero"),
    ) {
        Text(
            if (name != null) stringResource(R.string.lb_points_label, name) else stringResource(R.string.lb_points_label_anon),
            style = lbText(12.sp, Lb.Muted), maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Row(Modifier.fillMaxWidth().padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.weight(1f)) {
                Text(LeaderboardMath.points(board.myPoints), style = lbNumber(if (compact) 58.sp else 62.sp, Lb.White, FontWeight.Bold, spacing = (-0.032).em),
                    maxLines = 1, softWrap = false, modifier = Modifier.alignByBaseline().testTag("lb-points-mine"))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.lb_unit_points), style = lbText(23.sp, Color(0xFF82ABFF), FontWeight.SemiBold, 1f), modifier = Modifier.alignByBaseline())
            }
            val rank = board.me?.rank
            if (rank != null) {
                val shape = RoundedCornerShape(16.dp)
                Row(
                    Modifier.widthIn(min = if (compact) 62.dp else 65.dp).clip(shape).background(Lb.Blue, shape)
                        .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = 16.dp).testTag("lb-points-rank"),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(rank.toString(), style = lbNumber(29.sp, Lb.White, FontWeight.SemiBold), maxLines = 1,
                        modifier = Modifier.alignByBaseline().testTag("lb-points-rank-value"))
                    val suffix = stringResource(R.string.lb_rank_suffix)
                    if (suffix.isNotEmpty()) {
                        Spacer(Modifier.width(3.dp))
                        Text(suffix, style = lbText(12.sp, Lb.White, FontWeight.Medium, 1f), modifier = Modifier.alignByBaseline())
                    }
                }
            }
        }
        val delta = LeaderboardMath.delta(board.myPoints, board.lastWeekPoints)
        if (delta != null) {
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (delta < 0) LbIcons.TrendingDown else LbIcons.TrendingUp, contentDescription = null,
                    tint = Color(0xFF82ABFF), modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    when {
                        delta > 0 -> stringResource(R.string.lb_points_more, LeaderboardMath.points(delta))
                        delta < 0 -> stringResource(R.string.lb_points_less, LeaderboardMath.points(-delta))
                        else -> stringResource(R.string.lb_points_same)
                    },
                    style = lbText(12.sp, Lb.Ice),
                )
            }
        }
    }
}

/** 1위 줄 — 블루 표면 · 큰 숫자(크루 화면의 큰 시상대를 그대로 옮기지 않는다) */
@Composable
private fun PointsFirstRow(entry: LeaderEntry) {
    val shape = RoundedCornerShape(13.dp)
    Row(
        Modifier.padding(bottom = 4.dp).lbBleed(9.dp).fillMaxWidth().heightIn(min = 63.dp).clip(shape).background(Color(0xFF113454), shape)
            .padding(horizontal = 9.dp).semantics(mergeDescendants = true) { }.testTag("lb-points-first"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("1", style = lbNumber(29.sp, Lb.White, FontWeight.Bold, italic = true), modifier = Modifier.widthIn(min = 22.dp), maxLines = 1)
        LbAvatar(entry.mark, size = 36.dp, face = Lb.Blue)
        Column(Modifier.weight(1f)) {
            Text(entry.name, style = lbText(14.sp, Lb.White, FontWeight.SemiBold, 1.25f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(stringResource(R.string.lb_points_first), style = lbText(11.sp, Color(0xFF8EB7FF)), maxLines = 1)
        }
        LbValue(entry, points = true, size = 25.sp)
    }
}

/** 아래 — 바로 앞 순위와의 차이(1위면 선두) · 내 주변 순위 */
@Composable
private fun PointsFooter(board: PointsBoard, onOpenNearby: () -> Unit) {
    val me = board.me
    val shape = RoundedCornerShape(16.dp)
    if (me?.rank == null) {
        LbFooterSurface(onClick = null, face = Color(0xFF153B62), minHeight = 70.dp, modifier = Modifier.testTag("lb-points-footer")) {
            Text(stringResource(R.string.lb_points_unranked), style = lbText(12.sp, Color(0xFFE2ECFF)))
        }
        return
    }
    val ahead = board.ahead
    val gap = LeaderboardMath.gapToNext(me.metric, ahead?.metric)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 70.dp).clip(shape).background(Color(0xFF153B62), shape)
            .feedbackClickable(onClick = onOpenNearby).semantics(mergeDescendants = true) { }
            .padding(horizontal = 15.dp, vertical = 14.dp).testTag("lb-points-footer"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (ahead == null) stringResource(R.string.lb_points_gap_leader)
                else stringResource(R.string.lb_points_gap_title, ahead.rank ?: 0),
                style = lbText(12.sp, Color(0xFFE2ECFF)), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.lb_points_gap_open), style = lbText(11.sp, Lb.Muted), maxLines = 1)
        }
        if (gap != null) {
            Row(verticalAlignment = Alignment.Bottom) {
                if (gap <= 0.0) {
                    Text(stringResource(R.string.lb_next_tie), style = lbNumber(24.sp, Lb.White, FontWeight.SemiBold))
                } else {
                    Text(LeaderboardMath.points(gap), style = lbNumber(29.sp, Lb.White, FontWeight.SemiBold), modifier = Modifier.testTag("lb-points-gap"))
                    Spacer(Modifier.width(3.dp))
                    Text(stringResource(R.string.lb_unit_points), style = lbText(12.sp, Lb.White, FontWeight.Medium), modifier = Modifier.padding(bottom = 3.dp))
                }
            }
        }
        Icon(LbIcons.ChevronRight, contentDescription = null, tint = Lb.Ice, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun PointNearbySheet(board: PointsBoard) {
    val scopeLabel = if (board.scope == PointScope.CREW) board.crewName ?: stringResource(R.string.lb_scope_crew) else stringResource(R.string.lb_scope_all)
    LbSheetNote(stringResource(R.string.lb_points_nearby_note, scopeLabel))
    board.nearby.forEach { entry ->
        LbRow(
            entry, points = true, minHeight = 61.dp, sheet = true,
            sub = stringResource(if (entry.isMe) R.string.lb_points_mine_sub else R.string.lb_points_other_sub),
        )
    }
}

// ── 순위 기준 시트 ────────────────────────────────────────────────

@Composable
private fun RulesSheet(board: LeaderBoardKind) {
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
        LeaderBoardKind.POINTS -> listOf(R.string.lb_rules_points_title to R.string.lb_rules_points_body)
    }
    rules.forEachIndexed { i, (title, body) ->
        Column(
            Modifier.fillMaxWidth().then(if (i < rules.lastIndex) Modifier.lbDivider(Color(0xFF34516B)) else Modifier)
                .padding(vertical = 15.dp).testTag("lb-rule-$i"),
        ) {
            Text(stringResource(title), style = lbText(15.sp, Color(0xFFD4E5FF), FontWeight.SemiBold))
            Spacer(Modifier.height(6.dp))
            Text(stringResource(body), style = lbText(13.sp, Lb.Ice, height = 1.65f))
        }
    }
}

// ── 시트 · 아이콘 · 시스템 바 ─────────────────────────────────────

/** 상세 시트 — 남색 면 · 위 둥근 25 · 제목 · 닫기. 바깥 · 뒤로 · 닫기로 닫힌다(그 동안 배경은 멈춘다) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LbSheetHost(title: String, tag: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = Lb.Surface,
        scrimColor = Color(0xBA020B16),
        shape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp),
        dragHandle = null,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 35.dp).testTag(tag),
        ) {
            Row(Modifier.fillMaxWidth().padding(bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title, style = lbText(22.sp, Lb.White, FontWeight.Bold, 1.25f, (-0.027).em),
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                LbIconButton(LbIcons.Close, stringResource(R.string.lb_close), "lb-sheet-close", onClick = onDismiss)
            }
            content()
        }
    }
}

@Composable
private fun LbIconButton(icon: ImageVector, label: String, tag: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape)
            .feedbackClickable(role = Role.Button, cue = FeedbackCue.Select, onClick = onClick)
            .semantics { contentDescription = label }.testTag(tag),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = Lb.Ice, modifier = Modifier.size(20.dp)) }
}

/** 지난 기간 대비 짧은 글 — "n계단 상승" 등 */
@Composable
private fun moveText(rise: Int): String = when {
    rise > 0 -> stringResource(R.string.lb_move_up, rise)
    rise < 0 -> stringResource(R.string.lb_move_down, -rise)
    else -> stringResource(R.string.lb_move_same)
}

/** 화면이 늘 남색이라 상태바 · 내비바 아이콘을 밝게 — 떠나면 앞의 값으로 되돌린다 */
@Composable
private fun LightSystemBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val status = controller?.isAppearanceLightStatusBars
        val navigation = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (controller != null) {
                status?.let { controller.isAppearanceLightStatusBars = it }
                navigation?.let { controller.isAppearanceLightNavigationBars = it }
            }
        }
    }
}

/** 시안의 선 아이콘(lucide, 24 격자 · 두께 2 · 둥근 끝) */
internal object LbIcons {
    val Crown = icon("crown", "M11.562 3.266a.5.5 0 0 1 .876 0L15.39 8.87a1 1 0 0 0 1.516.294L21.183 5.5a.5.5 0 0 1 .798.519l-2.834 10.246a1 1 0 0 1-.956.734H5.81a1 1 0 0 1-.957-.734L2.02 6.02a.5.5 0 0 1 .798-.519l4.276 3.664a1 1 0 0 0 1.516-.294z", "M5 21h14")
    val ChevronUp = icon("chevron-up", "m18 15-6-6-6 6")
    val ChevronDown = icon("chevron-down", "m6 9 6 6 6-6")
    val ChevronRight = icon("chevron-right", "m9 18 6-6-6-6")
    val ArrowUpRight = icon("arrow-up-right", "M7 7h10v10", "M7 17 17 7")
    val ArrowDownRight = icon("arrow-down-right", "m7 7 10 10", "M17 7v10H7")
    val TrendingUp = icon("trending-up", "M16 7h6v6", "m22 7-8.5 8.5-5-5L2 17")
    val TrendingDown = icon("trending-down", "M16 17h6v-6", "m22 17-8.5-8.5-5 5L2 7")
    val Minus = icon("minus", "M5 12h14")
    val Info = icon("info", "M22 12A10 10 0 1 1 2 12A10 10 0 1 1 22 12z", "M12 16v-4", "M12 8h.01")
    val Close = icon("x", "M18 6 6 18", "m6 6 12 12")

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
