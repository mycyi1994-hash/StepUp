package com.stepup.android.ui.screens.records

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.data.local.RecordTotals
import com.stepup.android.data.local.RunRecordRow
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.RecordPeriod
import com.stepup.android.domain.localDate
import com.stepup.android.ui.components.LiveRouteMap
import com.stepup.android.ui.components.RunningPathCard
import com.stepup.android.ui.components.RunningPathColors
import com.stepup.android.ui.components.SecondaryHeader
import com.stepup.android.ui.components.SettingsChoiceRow
import com.stepup.android.ui.components.SettingsPrimaryButton
import com.stepup.android.ui.components.SettingsSheet
import com.stepup.android.ui.components.settingsPalette
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.StepUpDesign
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * 내 러닝 기록(01 · 02 · 03 · 07 · 08 · 09 · 10) — 내 정보의 "내 러닝 기록 보기".
 *
 * 처음엔 이번 달. 기간 줄을 누르면 기간 시트(월 · 전체 기간 — 적용해야 바뀐다). 요약 카드는 러닝 패스 카드를 작게,
 * 목록은 경로 썸네일 · 날짜 · 거리 · 시간 · 페이스. 합계는 기간 전체이고 목록은 끝까지 내리면 더 읽는다.
 * 상세 · 통계에 다녀와도 기간과 스크롤 위치는 그대로다.
 */
@Composable
fun RecordsScreen(
    onBack: () -> Unit = {},
    onOpenStats: (YearMonth) -> Unit = {},
    onOpenRun: (Long) -> Unit = {},
    onStartRun: () -> Unit = {},
    viewModel: RecordsViewModel = viewModel(factory = RecordsViewModel.Factory),
) {
    val load by viewModel.load.collectAsStateWithLifecycle()
    val period by viewModel.period.collectAsStateWithLifecycle()
    val first by viewModel.firstRecordAt.collectAsStateWithLifecycle()
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    val zone = remember { ZoneId.systemDefault() }
    val thisMonth = YearMonth.now(zone)
    RecordsContent(
        load = load, period = period, onBack = onBack,
        onOpenStats = { onOpenStats((period as? RecordPeriod.Month)?.month ?: thisMonth) },
        onOpenPeriod = { sheetOpen = true }, onOpenRun = onOpenRun, onStartRun = onStartRun,
        onShowAll = { viewModel.setPeriod(RecordPeriod.All) }, onReload = viewModel::reload, onLoadMore = viewModel::loadMore,
        route = viewModel::route, zone = zone,
    )
    if (sheetOpen) {
        PeriodSheet(
            current = period, thisMonth = thisMonth,
            firstYear = minOf(first?.localDate(zone)?.year ?: thisMonth.year, thisMonth.year),
            onApply = {
                viewModel.setPeriod(it)
                sheetOpen = false
            },
            onDismiss = { sheetOpen = false },
        )
    }
}

@Composable
fun RecordsContent(
    load: RecordsLoad,
    period: RecordPeriod,
    onBack: () -> Unit = {},
    onOpenStats: () -> Unit = {},
    onOpenPeriod: () -> Unit = {},
    onOpenRun: (Long) -> Unit = {},
    onStartRun: () -> Unit = {},
    onShowAll: () -> Unit = {},
    onReload: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    route: suspend (Long) -> List<GeoPoint> = { emptyList() },
    listState: LazyListState = rememberLazyListState(),
    zone: ZoneId = ZoneId.systemDefault(),
    today: LocalDate = LocalDate.now(zone),
) {
    val thisMonth = YearMonth.from(today)
    Column(Modifier.fillMaxSize().padding(horizontal = StepUpDesign.Gutter)) {
        SecondaryHeader(
            onBack = onBack, balance = null, onOpenWallet = null, title = stringResource(R.string.rec_title),
            trailing = { HeaderLink(stringResource(R.string.rec_stats), "records-stats", onOpenStats) },
        )
        PeriodRow(periodLabel(period), onOpenPeriod)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (load) {
                RecordsLoad.Loading -> RecordsSkeleton()
                RecordsLoad.Failed -> RecordsState(
                    stringResource(R.string.rec_failed_title), stringResource(R.string.rec_failed_body),
                    Modifier.testTag("records-failed"), actionLabel = stringResource(R.string.set_reload), onAction = onReload,
                    refresh = true,
                )
                is RecordsLoad.Ready -> when {
                    load.rows.isEmpty() && (!load.anyRecords || period == RecordPeriod.All) -> RecordsState(
                        stringResource(R.string.rec_empty_title), stringResource(R.string.rec_empty_body),
                        Modifier.testTag("records-empty"), actionLabel = stringResource(R.string.rec_start_run),
                        onAction = onStartRun, actionTag = "records-start-run",
                    )
                    load.rows.isEmpty() -> RecordsState(
                        stringResource(R.string.rec_period_empty_title), stringResource(R.string.rec_period_empty_body),
                        Modifier.testTag("records-period-empty"), actionLabel = stringResource(R.string.rec_show_all),
                        onAction = onShowAll, actionTag = "records-show-all",
                    )
                    else -> LazyColumn(
                        state = listState, modifier = Modifier.fillMaxSize().testTag("records-list"),
                        contentPadding = PaddingValues(top = 6.dp, bottom = 28.dp),
                    ) {
                        if (load.stale) item(key = "stale") { StaleNotice(onReload) }
                        item(key = "summary") { SummaryCard(load.period, load.totals, thisMonth) }
                        item(key = "heading") { ListHeading(load.period, load.totals.runs) }
                        items(load.rows, key = { it.id }) { row -> RecordRow(row, route, zone, today) { onOpenRun(row.id) } }
                        if (load.more) {
                            item(key = "more") {
                                LaunchedEffect(load.rows.size) { onLoadMore() }
                                Box(Modifier.fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(Modifier.size(22.dp), color = settingsPalette().accent, strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun periodLabel(period: RecordPeriod): String = when (period) {
    RecordPeriod.All -> stringResource(R.string.rec_period_all)
    is RecordPeriod.Month -> rememberFormatter(R.string.rec_month_pattern).format(period.month)
}

/** 기간 줄 — 아이콘 · 기간 · 화살표 · "기간 선택"이 하나의 버튼 */
@Composable
private fun PeriodRow(label: String, onClick: () -> Unit) {
    val p = settingsPalette()
    val hint = stringResource(R.string.rec_period_pick)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .feedbackClickable(role = null, onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "$label, $hint"
            }
            .testTag("records-period"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.DateRange, contentDescription = null, tint = p.accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = p.text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = p.secondary,
            modifier = Modifier.padding(start = 10.dp).size(22.dp))
        Spacer(Modifier.weight(1f))
        Text(hint, color = p.secondary, fontSize = 13.sp)
    }
}

/** 기간 요약 — 러닝 패스 카드를 작게. 러닝은 있는데 거리를 모르면 0km 로 단정하지 않는다 */
@Composable
private fun SummaryCard(period: RecordPeriod, totals: RecordTotals, thisMonth: YearMonth) {
    val unknown = totals.runs > 0 && totals.measured == 0
    val number = if (unknown) "—" else km(totals.meters)
    val size = when {
        number.length > 7 -> 36.sp
        number.length > 5 -> 44.sp
        else -> 51.sp
    }
    RunningPathCard(Modifier.padding(top = 8.dp).testTag("records-summary"), textEnd = 170.dp, minHeight = 134.dp) {
        Column(Modifier.padding(start = 20.dp, end = 16.dp)) {
            Text(
                when (period) {
                    RecordPeriod.All -> stringResource(R.string.rec_card_all)
                    is RecordPeriod.Month -> if (period.month == thisMonth) stringResource(R.string.rec_card_this_month)
                    else stringResource(R.string.rec_card_month, rememberFormatter(R.string.rec_month_name_pattern).format(period.month))
                },
                color = RunningPathColors.title, fontSize = 14.sp, modifier = Modifier.paddingFromBaseline(top = 32.dp),
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(number, color = RunningPathColors.number, fontSize = size, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, modifier = Modifier.alignByBaseline().paddingFromBaseline(top = 58.dp).testTag("records-distance"))
                if (!unknown) {
                    Spacer(Modifier.width(10.dp))
                    Text("km", color = RunningPathColors.unit, fontSize = 20.sp, modifier = Modifier.alignByBaseline())
                }
            }
            Text(
                if (unknown) stringResource(R.string.rec_distance_unknown)
                else pluralStringResource(R.plurals.rec_runs_count, totals.runs, totals.runs),
                color = RunningPathColors.runs, fontSize = 13.sp,
                modifier = Modifier.paddingFromBaseline(top = 22.dp, bottom = 22.dp).testTag("records-runs"),
            )
        }
    }
}

@Composable
private fun ListHeading(period: RecordPeriod, runs: Int) {
    val p = settingsPalette()
    Row(Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            when (period) {
                RecordPeriod.All -> stringResource(R.string.rec_list_heading_all, runs)
                is RecordPeriod.Month ->
                    stringResource(R.string.rec_list_heading_month, rememberFormatter(R.string.rec_month_name_pattern).format(period.month), runs)
            },
            color = p.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
        )
        Text(stringResource(R.string.rec_recent_first), color = p.secondary, fontSize = 13.sp)
    }
}

/** 기록 한 줄 — 경로 썸네일 · 날짜 · 거리 · 시간 · 페이스. 줄 전체가 누르는 곳 */
@Composable
private fun RecordRow(row: RunRecordRow, route: suspend (Long) -> List<GeoPoint>, zone: ZoneId, today: LocalDate, onClick: () -> Unit) {
    val p = settingsPalette()
    val date = row.startedAt.localDate(zone)
    val sameYear = date.year == today.year
    val dateText = rememberFormatter(if (sameYear) R.string.rec_row_date_pattern else R.string.rec_row_date_year_pattern).format(date)
    val detail = listOfNotNull(durationText(row.durationSec), runPace(row.durationSec, row.distanceMeters, compact = true)).joinToString(" · ")
    Column(Modifier.fillMaxWidth().feedbackClickable(role = Role.Button, onClick = onClick).testTag("record-row-${row.id}")) {
        Row(Modifier.fillMaxWidth().heightIn(min = 116.dp).padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            RouteThumb(row, route)
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(dateText, color = p.text.copy(alpha = 0.86f), fontSize = 14.sp)
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                    Text(if (row.distanceMeters > 0) km(row.distanceMeters) else "—", color = p.text, fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.alignByBaseline())
                    if (row.distanceMeters > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text("km", color = p.secondary, fontSize = 16.sp, modifier = Modifier.alignByBaseline())
                    }
                }
                Text(detail, color = p.secondary, fontSize = 12.5.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = p.secondary,
                modifier = Modifier.padding(start = 8.dp).size(22.dp))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
    }
}

/** 경로 썸네일 — 상세 · 확대 지도와 같은 저장 좌표. 좌표가 없으면 위치 없음 표시 */
@Composable
private fun RouteThumb(row: RunRecordRow, route: suspend (Long) -> List<GeoPoint>) {
    val p = settingsPalette()
    val shape = RoundedCornerShape(15.dp)
    var points by remember(row.id) { mutableStateOf<List<GeoPoint>?>(null) }
    LaunchedEffect(row.id, row.hasTrack) {
        points = if (row.hasTrack) route(row.id) else emptyList()
    }
    Box(Modifier.size(80.dp).clip(shape).background(p.surface), contentAlignment = Alignment.Center) {
        val shown = points
        when {
            shown == null -> Unit
            shown.size >= 2 -> LiveRouteMap(shown, Modifier.size(80.dp), seed = row.id.toInt())
            else -> Icon(Icons.Outlined.LocationOff, contentDescription = null, tint = p.secondary, modifier = Modifier.size(24.dp))
        }
    }
}

/** 다시 읽기 실패 — 앞서 읽은 기록은 그대로 두고 알린다 */
@Composable
private fun StaleNotice(onRetry: () -> Unit) {
    val p = settingsPalette()
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(18.dp)).background(p.surface)
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp).testTag("records-stale"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.rec_stale), color = p.secondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Box(
            Modifier.heightIn(min = 48.dp).padding(horizontal = 12.dp).feedbackClickable(role = Role.Button, onClick = onRetry),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.rec_retry), color = p.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** 읽는 중 — 카드와 줄 자리만(숫자를 0으로 보이지 않는다) */
@Composable
private fun RecordsSkeleton() {
    val p = settingsPalette()
    Column(Modifier.fillMaxWidth().padding(top = 8.dp).testTag("records-loading")) {
        Box(Modifier.fillMaxWidth().height(134.dp).clip(RoundedCornerShape(23.dp)).background(p.skeleton.copy(alpha = 0.6f)))
        Box(Modifier.padding(top = 30.dp).fillMaxWidth(0.4f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(p.skeleton))
        repeat(3) {
            Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(80.dp).clip(RoundedCornerShape(15.dp)).background(p.skeleton))
                Spacer(Modifier.width(18.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.width(110.dp).height(10.dp).clip(RoundedCornerShape(5.dp)).background(p.skeleton))
                    Box(Modifier.width(80.dp).height(20.dp).clip(RoundedCornerShape(6.dp)).background(p.skeleton))
                    Box(Modifier.width(140.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(p.skeleton))
                }
            }
        }
    }
}

/**
 * 기간 선택(03) — 전체 기간 또는 한 달. 시트 안에서 고른 것은 "적용"을 눌러야 목록에 들어간다(닫기 · 바깥 · 뒤로는 그대로).
 * 아직 오지 않은 달은 고를 수 없다.
 */
@Composable
fun PeriodSheet(
    current: RecordPeriod,
    thisMonth: YearMonth,
    firstYear: Int,
    onApply: (RecordPeriod) -> Unit,
    onDismiss: () -> Unit,
) {
    val p = settingsPalette()
    var pending by remember { mutableStateOf(current) }
    var year by remember { mutableIntStateOf((current as? RecordPeriod.Month)?.month?.year ?: thisMonth.year) }
    val yearText = rememberFormatter(R.string.rec_year_pattern).format(YearMonth.of(year, 1))
    val monthFormat = rememberFormatter(R.string.rec_short_month_pattern)
    SettingsSheet(
        title = stringResource(R.string.rec_period_pick), onDismiss = onDismiss, modifier = Modifier.testTag("period-sheet"),
        actions = {
            SettingsPrimaryButton(stringResource(R.string.rec_apply), { onApply(pending) },
                Modifier.fillMaxWidth().testTag("period-apply"))
        },
    ) {
        SettingsChoiceRow(stringResource(R.string.rec_period_all), selected = pending == RecordPeriod.All,
            onClick = { pending = RecordPeriod.All }, modifier = Modifier.testTag("period-all"))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { year-- }, enabled = year > firstYear, modifier = Modifier.size(48.dp).testTag("period-prev-year")) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.rec_prev_year),
                    tint = if (year > firstYear) p.text else p.secondary.copy(alpha = 0.4f))
            }
            Text(yearText, color = p.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(1f).testTag("period-year"))
            IconButton(onClick = { year++ }, enabled = year < thisMonth.year, modifier = Modifier.size(48.dp).testTag("period-next-year")) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.rec_next_year),
                    tint = if (year < thisMonth.year) p.text else p.secondary.copy(alpha = 0.4f))
            }
        }
        (0 until 3).forEach { line ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..4).forEach { column ->
                    val month = YearMonth.of(year, line * 4 + column)
                    val future = month.isAfter(thisMonth)
                    val on = pending == RecordPeriod.Month(month)
                    Box(
                        Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp))
                            .background(if (on) selectedFace() else Color.Transparent)
                            .feedbackClickable(enabled = !future, role = null) { pending = RecordPeriod.Month(month) }
                            .semantics {
                                role = Role.RadioButton
                                selected = on
                            }
                            .testTag("period-month-${month.monthValue}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(monthFormat.format(month), fontSize = 17.sp,
                            color = when {
                                future -> p.secondary.copy(alpha = 0.45f)
                                on -> p.text
                                else -> p.text.copy(alpha = 0.9f)
                            },
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

/** 고른 칸 · 통계 선택 버튼의 면 — 어두운 테마 #273B58 */
@Composable
internal fun selectedFace(): Color = if (StepUpColors.dark) Color(0xFF273B58) else settingsPalette().surface
