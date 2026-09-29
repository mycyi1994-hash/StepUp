package com.stepup.android.ui.screens.community.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.domain.CrewRun
import com.stepup.android.domain.CrewRunsScope
import com.stepup.android.domain.CrewWeek
import com.stepup.android.domain.CrewWeekMember
import com.stepup.android.domain.CrewHomeRules
import com.stepup.android.ui.screens.community.chat.ChatFace
import com.stepup.android.ui.screens.community.chat.ChatRetryState
import com.stepup.android.ui.screens.community.crew.CrewBar
import com.stepup.android.ui.screens.community.crew.CrewBottomBar
import com.stepup.android.ui.screens.community.crew.CrewButton
import com.stepup.android.ui.screens.community.crew.CrewButtonKind
import com.stepup.android.ui.screens.community.crew.CrewEmptyState
import com.stepup.android.ui.screens.community.crew.CrewGutter
import com.stepup.android.ui.screens.community.crew.CrewPage
import com.stepup.android.ui.screens.community.crew.CrewRow
import com.stepup.android.ui.screens.community.crew.CrewSheet
import com.stepup.android.ui.screens.community.crew.CrewSkeletonBox
import com.stepup.android.ui.screens.community.crew.CrewTopBar
import com.stepup.android.ui.screens.community.crew.crewInk
import java.time.LocalDate

/** "9.28 – 10.4" */
@Composable
internal fun weekRangeShort(start: LocalDate): String = stringResource(R.string.crewhome_week_range, homeShortDate(start), homeShortDate(start.plusDays(6)))

/** "9월 28일 – 10월 4일" */
@Composable
internal fun weekRangeLong(start: LocalDate): String = stringResource(R.string.crewhome_week_range, homeDate(start), homeDate(start.plusDays(6)))

/** 이번 주 · 지난주 · "9월 14일 주" */
@Composable
internal fun weekName(start: LocalDate, weeks: List<LocalDate>): String = when (weeks.indexOf(start)) {
    0 -> stringResource(R.string.crewhome_week_this)
    1 -> stringResource(R.string.crewhome_week_last)
    else -> stringResource(R.string.crewhome_week_of_date, homeDate(start))
}

/** 16 주간 선택 — 이번 주 · 지난주 · 2주 전. 고른 주를 기록 목록 · 개인 기록까지 넘긴다 */
@Composable
internal fun WeekSelectSheet(weeks: List<LocalDate>, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    CrewSheet(stringResource(R.string.crewhome_week_select), onDismiss, Modifier.testTag("home-week-select")) {
        Spacer(Modifier.height(16.dp))
        weeks.forEachIndexed { index, start ->
            CrewRow(
                weekRangeLong(start),
                { onPick(start) },
                Modifier.testTag("home-week-pick-$index"),
                value = when (index) {
                    0 -> stringResource(R.string.crewhome_week_this)
                    1 -> stringResource(R.string.crewhome_week_last)
                    else -> null
                },
            )
        }
        Spacer(Modifier.height(40.dp))
    }
}

// ─────────────────────────────────────────────────────────────
// 15 주간 기록 · 17 지난주 · 24 기록 없음
// ─────────────────────────────────────────────────────────────

class CrewWeekActions(
    val onBack: () -> Unit,
    /** 참여 기록 — 고른 주 전체(day · user 없음), 한 날, 한 사람 */
    val onRuns: (week: LocalDate, day: LocalDate?, member: CrewWeekMember?) -> Unit,
    val onHome: () -> Unit,
    val onEnded: () -> Unit,
)

/**
 * 15 · 17 · 24 — 한국 시간 월요일부터의 한 주. 오지 않은 요일은 "—"(0km 와 다르다), 목표를 넘어도 막대는 100% 까지,
 * 목표가 없으면 나누지 않고 거리만. 읽기 실패를 0km 로 보이지 않는다(실패 화면). 고른 주는 목록 · 개인 기록까지 이어진다.
 */
@Composable
fun CrewWeekScreen(viewModel: CrewWeekViewModel, actions: CrewWeekActions) {
    val ink = crewInk()
    val week by viewModel.week.collectAsStateWithLifecycle()
    val card by viewModel.card.collectAsStateWithLifecycle()
    val sheet by viewModel.sheet.collectAsStateWithLifecycle()
    val ended by viewModel.ended.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.load()
        onPauseOrDispose {}
    }
    LaunchedEffect(ended) { if (ended) actions.onEnded() }
    val current = (week as? HomeLoad.Ready)?.value
    CrewPage(Modifier.testTag(if (current?.empty == true) "home-week-empty" else "home-week-page")) {
        CrewTopBar(stringResource(R.string.crewhome_week_bar), actions.onBack)
        when {
            current != null -> {
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = CrewGutter)) {
                    WeekHead(current, card?.name.orEmpty()) { viewModel.sheet.value = true }
                    Spacer(Modifier.height(32.dp))
                    Text(stringResource(R.string.crewhome_week_days), color = ink.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(20.dp))
                    val today = if (current.thisWeek) (LocalDate.now(CrewHomeRules.KST).toEpochDay() - current.start.toEpochDay()).toInt() else null
                    WeekBars(
                        current.days, today, Modifier.testTag("home-week-bars"),
                        onDay = if (current.empty) null else { index -> actions.onRuns(current.start, current.start.plusDays(index.toLong()), null) },
                    )
                    Spacer(Modifier.height(26.dp))
                    HomeDivider()
                    if (current.empty) {
                        Spacer(Modifier.height(36.dp))
                        Text(
                            stringResource(if (current.thisWeek) R.string.crewhome_week_empty_body else R.string.crewhome_week_empty_past),
                            color = ink.text, fontSize = 17.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("home-week-empty-body"),
                        )
                    } else {
                        Spacer(Modifier.height(26.dp))
                        Text(stringResource(R.string.crewhome_week_members), color = ink.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.crewhome_week_members_note), color = ink.secondary, fontSize = 12.5.sp)
                        Spacer(Modifier.height(10.dp))
                        current.members.forEach { member ->
                            WeekMemberRow(member) { actions.onRuns(current.start, null, member) }
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                }
                CrewBottomBar {
                    if (current.empty) CrewButton(stringResource(R.string.crewhome_week_to_home), actions.onHome, Modifier.testTag("home-week-home"))
                    else CrewButton(stringResource(R.string.crewhome_week_all), { actions.onRuns(current.start, null, null) }, Modifier.testTag("home-week-all"))
                }
                if (sheet) WeekSelectSheet(current.weeks, viewModel::select) { viewModel.sheet.value = false }
            }
            week is HomeLoad.Loading -> Column(Modifier.padding(CrewGutter)) {
                CrewSkeletonBox(Modifier.width(180.dp).height(32.dp))
                Spacer(Modifier.height(30.dp))
                CrewSkeletonBox(Modifier.fillMaxWidth().height(120.dp))
                Spacer(Modifier.height(30.dp))
                CrewSkeletonBox(Modifier.fillMaxWidth().height(132.dp))
            }
            else -> ChatRetryState(
                title = stringResource(R.string.crewhome_week_error),
                body = stringResource(R.string.crewhome_error_body),
                button = stringResource(R.string.crewhome_error_retry),
                onRetry = viewModel::load,
                modifier = Modifier.padding(top = 150.dp),
                tag = "home-week-error",
            )
        }
    }
}

@Composable
private fun WeekHead(week: CrewWeek, crewName: String, onSelect: () -> Unit) {
    val ink = crewInk()
    Spacer(Modifier.height(18.dp))
    Row(
        Modifier.heightIn(min = 48.dp).homeClickable(onClick = onSelect).padding(end = 12.dp).testTag("home-week-range"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(weekRangeShort(week.start), color = ink.text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(18.dp))
        Icon(Icons.Filled.KeyboardArrowDown, null, tint = ink.info, modifier = Modifier.size(24.dp))
    }
    Spacer(Modifier.height(26.dp))
    val progress = week.progress
    Row(verticalAlignment = Alignment.Top) {
        Text(
            stringResource(if (endsWithConsonant(crewName)) R.string.crewhome_week_heading_c else R.string.crewhome_week_heading_v, crewName),
            color = ink.secondary, fontSize = 14.sp, modifier = Modifier.weight(1f),
        )
        if (progress != null) Text(stringResource(R.string.crewhome_week_goal, progress.goalKm), color = ink.secondary, fontSize = 13.sp)
    }
    Row(verticalAlignment = Alignment.Bottom) {
        Text(km(week.km), color = ink.text, fontSize = 72.sp, lineHeight = 80.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("home-week-total"))
        Spacer(Modifier.width(14.dp))
        Text("km", color = ink.secondary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp).weight(1f))
        if (progress != null) {
            Text(
                stringResource(R.string.crew_percent, progress.percent), color = ink.info, fontSize = 30.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 10.dp).testTag("home-week-percent"),
            )
        }
    }
    if (progress != null) {
        Spacer(Modifier.height(12.dp))
        CrewBar(progress.fraction, Modifier.fillMaxWidth().height(8.dp))
    }
    Spacer(Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        val line = when {
            week.empty -> stringResource(R.string.crewhome_week_first)
            progress == null -> null
            progress.reached -> stringResource(R.string.crewhome_week_reached)
            else -> stringResource(R.string.crewhome_week_left, km(progress.remainingKm))
        }
        Text(line.orEmpty(), color = ink.info, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(stringResource(R.string.crewhome_week_runners, week.runners), color = ink.secondary, fontSize = 12.5.sp, modifier = Modifier.testTag("home-week-runners"))
    }
}

@Composable
private fun WeekMemberRow(member: CrewWeekMember, onClick: () -> Unit) {
    val ink = crewInk()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).homeClickable(onClick = onClick).padding(vertical = 10.dp).testTag("home-week-member-${member.userId}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChatFace(member.name, owner = false, size = 40.dp)
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(member.name, color = ink.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val lastAt = member.lastAt
            val lastKm = member.lastKm
            if (lastAt != null && lastKm != null) {
                Spacer(Modifier.height(6.dp))
                val day = recordDayLabel(lastAt)
                Text(
                    if (localDay(lastAt) == LocalDate.now()) stringResource(R.string.crewhome_week_member_last, day, km(lastKm), homeClock(lastAt))
                    else stringResource(R.string.crewhome_week_member_last_day, day, km(lastKm)),
                    color = ink.secondary, fontSize = 12.5.sp,
                )
            }
        }
        Text(stringResource(R.string.crewhome_week_km, km(member.km)), color = ink.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────────────────────
// 18 참여 기록
// ─────────────────────────────────────────────────────────────

/** 18 — 고른 주 전체 · 한 날 · 한 사람의 기록, 늦게 끝난 순으로 이어 보인다. 위 칸을 누르면 16(그 주 전체로) */
@Composable
fun CrewRunsScreen(viewModel: CrewRunsViewModel, onBack: () -> Unit, onRun: (CrewRun) -> Unit, onEnded: () -> Unit) {
    val ink = crewInk()
    val scope by viewModel.scope.collectAsStateWithLifecycle()
    val week by viewModel.week.collectAsStateWithLifecycle()
    val runs by viewModel.runs.collectAsStateWithLifecycle()
    val more by viewModel.more.collectAsStateWithLifecycle()
    val loadingMore by viewModel.loadingMore.collectAsStateWithLifecycle()
    val sheet by viewModel.sheet.collectAsStateWithLifecycle()
    val ended by viewModel.ended.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.load()
        onPauseOrDispose {}
    }
    LaunchedEffect(ended) { if (ended) onEnded() }
    val listState = rememberLazyListState()
    val nearEnd by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index?.let { it >= listState.layoutInfo.totalItemsCount - 3 } == true } }
    LaunchedEffect(nearEnd, more) { if (nearEnd && more) viewModel.loadMore() }

    CrewPage(Modifier.testTag("home-runs")) {
        CrewTopBar(stringResource(R.string.crewhome_runs_bar), onBack)
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().testTag("home-runs-list"), state = listState,
            contentPadding = PaddingValues(start = CrewGutter, end = CrewGutter, bottom = 40.dp),
        ) {
            item(key = "head") { RunsHead(scope, week, viewModel.name) { viewModel.sheet.value = true } }
            when (val state = runs) {
                is HomeLoad.Ready -> {
                    if (state.value.isEmpty()) {
                        item(key = "empty") {
                            Text(stringResource(R.string.crewhome_runs_empty), color = ink.secondary, fontSize = 14.sp, modifier = Modifier.padding(top = 36.dp).testTag("home-runs-empty"))
                        }
                    }
                    // 날짜별 소제목 — 한 날만 고른 경우에는 제목이 그 날이라 두지 않는다
                    var lastDay: LocalDate? = null
                    state.value.forEach { run ->
                        val day = localDay(run.endedAt)
                        if (scope.day == null && day != lastDay) {
                            val header = day
                            item(key = "day-${header}") {
                                Text(
                                    homeDateWeekday(header), color = ink.info, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 22.dp, bottom = 4.dp),
                                )
                            }
                        }
                        lastDay = day
                        item(key = "run-${run.id}") { RunRow(run) { onRun(run) } }
                    }
                    if (loadingMore) {
                        item(key = "more-loading") {
                            Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(22.dp), color = ink.info, strokeWidth = 2.dp)
                            }
                        }
                    } else if (more) {
                        item(key = "more") {
                            Text(stringResource(R.string.crewhome_runs_more), color = ink.secondary, fontSize = 12.5.sp, modifier = Modifier.padding(top = 28.dp).testTag("home-runs-more"))
                        }
                    }
                }
                HomeLoad.Loading -> items(4, key = { "skeleton-$it" }) {
                    CrewSkeletonBox(Modifier.fillMaxWidth().padding(vertical = 8.dp).height(56.dp))
                }
                else -> item(key = "error") {
                    ChatRetryState(
                        title = stringResource(R.string.crewhome_runs_error),
                        body = stringResource(R.string.crewhome_error_body),
                        button = stringResource(R.string.crewhome_error_retry),
                        onRetry = viewModel::load,
                        modifier = Modifier.padding(top = 80.dp),
                        tag = "home-runs-error",
                    )
                }
            }
        }
    }
    val weeks = week?.weeks.orEmpty()
    if (sheet && weeks.isNotEmpty()) WeekSelectSheet(weeks, viewModel::selectWeek) { viewModel.sheet.value = false }
}

@Composable
private fun RunsHead(scope: CrewRunsScope, week: CrewWeek?, name: String, onSelect: () -> Unit) {
    val ink = crewInk()
    Column {
        Spacer(Modifier.height(20.dp))
        val day = scope.day
        val start = week?.start ?: scope.week
        Text(
            when {
                day != null -> homeDateWeekday(day)
                scope.userId != null && name.isNotBlank() -> stringResource(R.string.crewhome_runs_scope_user, name)
                start != null -> weekRangeShort(start)
                else -> ""
            },
            color = ink.text, fontSize = 27.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("home-runs-title"),
        )
        if (week != null) {
            Spacer(Modifier.height(10.dp))
            val weekLabel = weekName(week.start, week.weeks)
            val sub = when {
                day != null -> {
                    val index = (day.toEpochDay() - week.start.toEpochDay()).toInt()
                    val dayKm = week.days.getOrNull(index) ?: 0.0
                    val dayLabel = if (day == LocalDate.now()) stringResource(R.string.crewhome_today) else homeDate(day)
                    stringResource(R.string.crewhome_runs_sub, dayLabel, km(dayKm), weekLabel, km(week.km))
                }
                scope.userId != null -> {
                    val member = week.members.firstOrNull { it.userId == scope.userId }
                    stringResource(R.string.crewhome_runs_sub, weekLabel, km(member?.km ?: 0.0), stringResource(R.string.crewhome_week), km(week.km))
                }
                else -> stringResource(R.string.crewhome_runs_week_sub, weekLabel, km(week.km), week.runners)
            }
            Text(sub, color = ink.secondary, fontSize = 13.sp)
        }
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(14.dp)).background(ink.card)
                .homeClickable(onClick = onSelect).padding(horizontal = 20.dp, vertical = 14.dp).testTag("home-runs-scope"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val label = when {
                day != null -> stringResource(R.string.crewhome_runs_scope_day, homeDate(day))
                scope.userId != null && name.isNotBlank() -> stringResource(R.string.crewhome_runs_scope_user, name)
                week != null -> stringResource(R.string.crewhome_runs_scope_week, weekName(week.start, week.weeks))
                else -> ""
            }
            Text(label, color = ink.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = ink.info, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun RunRow(run: CrewRun, onClick: () -> Unit) {
    val ink = crewInk()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 84.dp).homeClickable(onClick = onClick).testTag("home-run-row-${run.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChatFace(run.name, owner = false, size = 42.dp)
        Column(Modifier.weight(1f).padding(start = 18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(vertical = 16.dp)) {
                    Text(run.name, color = ink.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.crewhome_runs_row_sub, recordDayLabel(run.endedAt), homeClock(run.endedAt)), color = ink.secondary, fontSize = 12.5.sp)
                }
                Text(stringResource(R.string.crewhome_week_km, km(run.km)), color = ink.text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            }
            HomeDivider()
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 19 러닝 기록
// ─────────────────────────────────────────────────────────────

/** 19 — 고른 기록(runId)의 러너 · 코스(처음과 끝 300m 를 뗀) · 거리 · 시간 · 평균 페이스 */
@Composable
fun CrewRunScreen(viewModel: CrewRunViewModel, onBack: () -> Unit, onRuns: () -> Unit, onEnded: () -> Unit) {
    val ink = crewInk()
    val run by viewModel.run.collectAsStateWithLifecycle()
    val ended by viewModel.ended.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.load()
        onPauseOrDispose {}
    }
    LaunchedEffect(ended) { if (ended) onEnded() }
    CrewPage(Modifier.testTag("home-run")) {
        CrewTopBar(stringResource(R.string.crewhome_run_bar), onBack)
        when (val state = run) {
            is HomeLoad.Ready -> {
                val detail = state.value
                val r = detail.run
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = CrewGutter)) {
                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChatFace(r.name, owner = false, size = 40.dp)
                        Column(Modifier.padding(start = 16.dp)) {
                            Text(r.name, color = ink.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("home-run-name"))
                            Spacer(Modifier.height(6.dp))
                            Text(stringResource(R.string.crewhome_run_done, homeDate(localDay(r.endedAt)), homeClock(r.endedAt)), color = ink.secondary, fontSize = 12.5.sp)
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    if (detail.route.size >= 2) {
                        RunRouteMap(detail.route, Modifier.fillMaxWidth().height(290.dp))
                        Spacer(Modifier.height(26.dp))
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(km(r.km), color = ink.text, fontSize = 64.sp, lineHeight = 70.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("home-run-km"))
                        Spacer(Modifier.width(14.dp))
                        Text("km", color = ink.secondary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 10.dp))
                    }
                    Spacer(Modifier.height(24.dp))
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text(CrewHomeRules.clock(r.durationS), color = ink.text, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("home-run-time"))
                            Spacer(Modifier.height(6.dp))
                            Text(stringResource(R.string.crewhome_run_time), color = ink.secondary, fontSize = 12.5.sp)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                r.paceSecPerKm?.let(CrewHomeRules::paceText) ?: "—", color = ink.text, fontSize = 28.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag("home-run-pace"),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(stringResource(R.string.crewhome_run_pace), color = ink.secondary, fontSize = 12.5.sp)
                        }
                    }
                    Spacer(Modifier.height(26.dp))
                    HomeDivider()
                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(R.string.crewhome_run_shared), color = ink.secondary, fontSize = 12.5.sp)
                    Spacer(Modifier.height(24.dp))
                }
                CrewBottomBar { CrewButton(stringResource(R.string.crewhome_run_back), onRuns, Modifier.testTag("home-run-back"), kind = CrewButtonKind.SECONDARY) }
            }
            HomeLoad.Missing -> CrewEmptyState(
                icon = { Icon(Icons.Outlined.Info, null, tint = ink.info, modifier = Modifier.size(44.dp)) },
                title = stringResource(R.string.crewhome_run_missing_title),
                body = stringResource(R.string.crewhome_run_missing_body),
                modifier = Modifier.padding(top = 150.dp).testTag("home-run-missing"),
            ) { CrewButton(stringResource(R.string.crewhome_run_back), onRuns) }
            HomeLoad.Loading -> Column(Modifier.padding(CrewGutter)) {
                CrewSkeletonBox(Modifier.fillMaxWidth().height(290.dp), 18.dp)
            }
            HomeLoad.Failed -> ChatRetryState(
                title = stringResource(R.string.crewhome_run_error),
                body = stringResource(R.string.crewhome_error_body),
                button = stringResource(R.string.crewhome_error_retry),
                onRetry = viewModel::load,
                modifier = Modifier.padding(top = 150.dp),
                tag = "home-run-error",
            )
        }
    }
}
