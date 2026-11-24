package com.stepup.android.ui.screens.records

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
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
import com.stepup.android.ui.components.PlainEmptyState
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunButtonKind
import com.stepup.android.ui.components.RunCard
import com.stepup.android.ui.components.RunMapFrame
import com.stepup.android.ui.components.RunNotice
import com.stepup.android.ui.components.RunNoticeKind
import com.stepup.android.ui.components.RunNumber
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.RunSpec
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.RunTextAction
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpSans
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * 내 러닝 기록(시안 H01 · H02 · H03 · H07 · H08 · H09 · H10) — 내 정보의 "내 러닝 기록 보기" · 러닝 결과의 "기록".
 *
 * 처음엔 이번 달. 기간 알약을 누르면 기간 시트(전체 기간 · 달 — 적용해야 바뀐다). 위 합계는 기간 전체(불러온 줄만의 합이
 * 아니다)이고, 목록은 경로 썸네일 · 날짜 · 거리 · 시간 카드로 끝까지 내리면 더 읽는다. 아래 큰 버튼은 러닝 시작(U01).
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
        route = viewModel::route, cachedRoute = viewModel::cachedRoute, zone = zone,
    )
    if (sheetOpen) {
        PeriodSheet(
            current = period, thisMonth = thisMonth,
            firstMonth = minOf(first?.let { YearMonth.from(it.localDate(zone)) } ?: thisMonth, thisMonth),
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
    cachedRoute: (Long) -> List<GeoPoint>? = { null },
    listState: LazyListState = rememberLazyListState(),
    zone: ZoneId = ZoneId.systemDefault(),
    today: LocalDate = LocalDate.now(zone),
) {
    val thisMonth = YearMonth.from(today)
    // 첫 기록 전(H07) · 못 읽음(H10)은 제목만 — 기간 · 통계로 갈 것이 없다
    val firstEmpty = load is RecordsLoad.Ready && load.rows.isEmpty() && (!load.anyRecords || period == RecordPeriod.All)
    val bare = firstEmpty || load == RecordsLoad.Failed
    RecordsFrame(onBack, Modifier.testTag("records-screen")) {
        Row(
            Modifier.fillMaxWidth().padding(start = RecSpec.TitleGutter, end = RecSpec.TitleGutter - 10.dp, top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RecScreenTitle(
                stringResource(if (period == RecordPeriod.All && !bare) R.string.run_rec_title_all else R.string.rec_title),
                Modifier.weight(1f),
            )
            if (!bare) RunTextAction(stringResource(R.string.rec_stats), onOpenStats, Modifier.testTag("records-stats"), chevron = true)
        }
        if (!bare) PeriodChips(period, onOpenPeriod, onShowAll, Modifier.padding(horizontal = RecSpec.TitleGutter).padding(top = 8.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (load) {
                RecordsLoad.Loading -> RecordsSkeleton()
                RecordsLoad.Failed -> CenteredState(Modifier.testTag("records-failed")) {
                    PlainEmptyState(
                        R.drawable.mascot_oops, stringResource(R.string.run_rec_failed_title),
                        body = stringResource(R.string.run_rec_failed_body),
                        action = stringResource(R.string.run_rec_reload), onAction = onReload, actionTag = "records-reload",
                    )
                }
                is RecordsLoad.Ready -> when {
                    firstEmpty -> CenteredState(Modifier.testTag("records-empty")) {
                        PlainEmptyState(
                            R.drawable.scene_blanket, stringResource(R.string.run_rec_empty_title),
                            body = stringResource(R.string.run_rec_empty_body), imageHeight = 120.dp,
                        )
                    }
                    load.rows.isEmpty() -> CenteredState(
                        Modifier.testTag("records-period-empty"),
                        header = { Summary(load.totals, tagged = false) },
                    ) {
                        // "11월엔 아직 기록이 없어요" · "지난 기록 보기"(전체 기간으로 — 예전 "전체 기록 보기"와 같은 일)
                        val month = (period as? RecordPeriod.Month)?.month
                        PlainEmptyState(
                            R.drawable.scene_blanket,
                            if (month != null) stringResource(R.string.rd_d_month_empty_title, shortMonth(month))
                            else stringResource(R.string.run_rec_period_empty_title),
                            body = stringResource(R.string.rd_d_month_empty_body), imageHeight = 120.dp,
                            action = stringResource(R.string.rd_d_past_records), onAction = onShowAll, actionTag = "records-show-all",
                        )
                    }
                    else -> LazyColumn(
                        state = listState, modifier = Modifier.fillMaxSize().testTag("records-list"),
                        contentPadding = PaddingValues(start = RecSpec.Gutter, end = RecSpec.Gutter, top = 0.dp, bottom = 12.dp),
                    ) {
                        item(key = "summary") { Summary(load.totals) }
                        if (load.stale) item(key = "stale") { StaleNotice(onReload) }
                        items(load.rows, key = { it.id }) { row -> RecordRow(row, route, cachedRoute, zone, today) { onOpenRun(row.id) } }
                        if (load.more) {
                            item(key = "more") {
                                LaunchedEffect(load.rows.size) { onLoadMore() }
                                Box(Modifier.fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
                                    RunSpinner(Modifier.size(26.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        // 주 버튼은 하나 — 러닝 시작(U01). 못 읽음은 다시 불러오기(글자 버튼)만
        if (load != RecordsLoad.Failed) {
            RunButton(
                stringResource(R.string.run_rec_start), onStartRun,
                Modifier.padding(horizontal = RecSpec.Gutter).padding(top = 6.dp, bottom = 14.dp)
                    .testTag(if (firstEmpty) "records-start-run" else "records-hero-start"),
                hero = true,
            )
        }
    }
}

/** 빈 · 못 읽음 — 남은 자리 가운데(큰 글씨면 넘긴다). [header] 는 맨 위(기간 합계 한 줄) */
@Composable
private fun CenteredState(
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight)
                .padding(horizontal = RecSpec.Gutter).padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (header != null) header()
            Spacer(Modifier.weight(1f))
            content()
            Spacer(Modifier.weight(1.3f))
        }
    }
}

/** 재디자인(2026-11-24) 여백 — 제목 · 기간 칩 · 합계는 20dp, 목록 · 카드 · 주 버튼은 18dp(시안 10 · 11) */
internal object RecSpec {
    val TitleGutter = 20.dp
    val Gutter = 18.dp
}

/** 화면 제목 — 30sp Bold, 자간 −2.5%(재디자인 2-5) */
@Composable
internal fun RecScreenTitle(text: String, modifier: Modifier = Modifier) {
    val t = runTone()
    Text(
        text,
        style = runTextStyle(30.sp, t.text, FontWeight.Bold, 1.25f).copy(letterSpacing = (-0.025).em),
        maxLines = 2, overflow = TextOverflow.Ellipsis,
        modifier = modifier.semantics { heading() },
    )
}

/** "11월" · "Nov" — 이 기기 언어의 짧은 달 이름 */
@Composable
internal fun shortMonth(month: YearMonth): String =
    month.month.getDisplayName(java.time.format.TextStyle.SHORT, androidx.compose.ui.platform.LocalConfiguration.current.locales[0])

@Composable
private fun periodLabel(period: RecordPeriod): String = when (period) {
    RecordPeriod.All -> stringResource(R.string.rec_period_all)
    is RecordPeriod.Month -> rememberFormatter(R.string.rec_month_pattern).format(period.month)
}

/**
 * 기간 칩 — 고른 기간(검정 · 어둡게는 흰색)과 "전체 기간". 고른 칩을 누르면 기간 시트(달 고르기),
 * 달을 보고 있을 때의 "전체 기간" 칩은 바로 전체로. 누르는 곳은 48dp(칩은 그 안에)
 */
@Composable
private fun PeriodChips(period: RecordPeriod, onOpenSheet: () -> Unit, onShowAll: () -> Unit, modifier: Modifier = Modifier) {
    val hint = stringResource(R.string.rec_period_pick)
    val label = periodLabel(period)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        PeriodChip(
            label, selected = true, onClick = onOpenSheet,
            modifier = Modifier.weight(1f, fill = false).semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "$label, $hint"
            }.testTag("records-period"),
        )
        if (period is RecordPeriod.Month) {
            PeriodChip(
                stringResource(R.string.rd_d_all), selected = false, onClick = onShowAll,
                modifier = Modifier.testTag("records-period-all"),
            )
        }
    }
}

@Composable
private fun PeriodChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val t = runTone()
    val shape = RoundedCornerShape(50)
    Box(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp))
            .feedbackClickable(role = null, cue = FeedbackCue.Select, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = runTextStyle(15.sp, if (selected) (if (t.dark) Color.Black else Color.White) else t.label, FontWeight.Bold, 1.2f),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clip(shape).background(if (selected) t.text else t.secondaryFace, shape)
                .padding(horizontal = 18.dp, vertical = 9.dp),
        )
    }
}

/**
 * 기간 합계 한 줄 — "8회 · 34.2km · 3:12:00". 러닝은 있는데 거리를 모르면 0km 로 단정하지 않는다("—").
 * [tagged] 면 횟수 · 거리에 꼬리표(기록 목록의 합계).
 */
@Composable
private fun Summary(totals: RecordTotals, tagged: Boolean = true) {
    val t = runTone()
    val unknown = totals.runs > 0 && totals.measured == 0
    val style = runTextStyle(16.sp, t.label, FontWeight.SemiBold, 1.3f).copy(fontFeatureSettings = "tnum")
    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 10.dp).then(if (tagged) Modifier.testTag("records-summary") else Modifier)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${totals.runs}", style = style, maxLines = 1, softWrap = false,
                modifier = if (tagged) Modifier.testTag("records-runs") else Modifier)
            Text(pluralStringResource(R.plurals.rd_d_runs_unit, totals.runs) + " · ", style = style, maxLines = 1, softWrap = false)
            Text(if (unknown) "—" else km(totals.meters), style = style, maxLines = 1, softWrap = false,
                modifier = if (tagged) Modifier.testTag("records-distance") else Modifier)
            Text((if (unknown) "" else "km") + " · " + clockText(totals.seconds), style = style, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        }
        if (unknown) {
            Text(stringResource(R.string.rec_distance_unknown), style = runTextStyle(13.sp, t.muted, FontWeight.Medium))
        }
    }
}

/** 기록 한 장 — 경로 썸네일 · 날짜 · 거리 · 시간. 카드 전체가 누르는 곳 */
@Composable
private fun RecordRow(
    row: RunRecordRow,
    route: suspend (Long) -> List<GeoPoint>,
    cachedRoute: (Long) -> List<GeoPoint>?,
    zone: ZoneId,
    today: LocalDate,
    onClick: () -> Unit,
) {
    val t = runTone()
    val date = row.startedAt.localDate(zone)
    val dateText = rememberFormatter(if (date.year == today.year) R.string.run_rec_row_date else R.string.run_rec_row_date_year).format(date)
    val hasDistance = row.distanceMeters > 0
    RunCard(Modifier.padding(vertical = 5.dp), padding = PaddingValues(8.dp), onClick = onClick, tag = "record-row-${row.id}") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RouteThumb(row, route, cachedRoute, Modifier.weight(0.47f).height(98.dp))
            Column(Modifier.weight(0.53f).padding(start = 14.dp)) {
                Text(dateText, style = runTextStyle(14.sp, t.label, FontWeight.SemiBold), maxLines = 1)
                if (hasDistance) {
                    RunNumber(km2(row.distanceMeters), unit = "km", size = 38.sp, unitSize = 18.sp, italicUnit = false)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = t.label, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(clockText(row.durationSec), style = runTextStyle(15.sp, t.label, FontWeight.SemiBold))
                    }
                } else {
                    // 시간만 남은 러닝 — 시간이 큰 수, 거리는 재지 않았다
                    RunNumber(clockText(row.durationSec), size = 38.sp)
                    Text(stringResource(R.string.run_rec_kind_time), style = runTextStyle(14.sp, t.label, FontWeight.SemiBold))
                }
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = t.label, modifier = Modifier.size(24.dp))
        }
    }
}

/** 경로 썸네일 — 상세 · 확대 지도와 같은 저장 좌표(실제 지도 타일). 좌표가 없으면 위치 없음 표시 */
@Composable
private fun RouteThumb(
    row: RunRecordRow,
    route: suspend (Long) -> List<GeoPoint>,
    cachedRoute: (Long) -> List<GeoPoint>?,
    modifier: Modifier,
) {
    val t = runTone()
    // 이미 읽은 경로면 줄이 다시 보일 때 빈 칸 없이 바로
    var points by remember(row.id) { mutableStateOf(if (row.hasTrack) cachedRoute(row.id) else emptyList()) }
    LaunchedEffect(row.id, row.hasTrack) {
        points = if (row.hasTrack) route(row.id) else emptyList()
    }
    RunMapFrame(modifier) {
        val shown = points
        when {
            shown == null -> Unit
            shown.size >= 2 -> LiveRouteMap(shown, Modifier.fillMaxSize(), seed = row.id.toInt(), routeColor = t.cyan)
            else -> Icon(
                Icons.Outlined.LocationOff, contentDescription = null, tint = t.label,
                modifier = Modifier.align(Alignment.Center).size(26.dp),
            )
        }
    }
}

/** 다시 읽기 실패 — 앞서 읽은 기록은 그대로 두고 알린다 */
@Composable
private fun StaleNotice(onRetry: () -> Unit) {
    RunNotice(
        stringResource(R.string.rec_stale), Modifier.padding(vertical = 6.dp), kind = RunNoticeKind.Warn,
        action = stringResource(R.string.rec_retry), onAction = onRetry, tag = "records-stale",
    )
}

/** 읽는 중(H09) — 도는 표시 · 안내 · 합계와 카드 자리. 숫자를 0으로 보이지 않는다 */
@Composable
private fun RecordsSkeleton() {
    val t = runTone()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = RecSpec.Gutter).testTag("records-loading"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RunSpinner(Modifier.padding(top = 8.dp).size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.run_rec_loading_title), style = runTextStyle(17.sp, t.text, FontWeight.Bold))
        Text(stringResource(R.string.run_rec_loading_body), style = runTextStyle(14.sp, t.label, FontWeight.Medium))
        Spacer(Modifier.height(18.dp))
        Bone(180.dp, 14.dp)
        Spacer(Modifier.height(16.dp))
        repeat(3) {
            RunCard(Modifier.padding(vertical = 5.dp), padding = PaddingValues(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MapBone(Modifier.weight(0.47f).height(98.dp))
                    Column(Modifier.weight(0.53f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Bone(70.dp, 12.dp)
                        Bone(124.dp, 26.dp)
                        Bone(84.dp, 12.dp)
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = t.muted, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun Bone(width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp) {
    val t = runTone()
    Box(Modifier.size(width, height).clip(RoundedCornerShape(height / 2)).background(t.track))
}

/** 썸네일 자리 — 지도를 흉내 내지 않는 옅은 결 */
@Composable
private fun MapBone(modifier: Modifier) {
    val t = runTone()
    val shape = RoundedCornerShape(RunSpec.CardRadius)
    Canvas(modifier.clip(shape).background(t.inset).border(1.dp, t.panelEdge, shape)) {
        val line = t.track
        listOf(0.2f to 0.9f, 0.55f to 1.2f, -0.1f to 0.5f).forEach { (from, to) ->
            drawLine(line, Offset(size.width * from, 0f), Offset(size.width * to, size.height), strokeWidth = 5.dp.toPx())
        }
        drawLine(line, Offset(0f, size.height * 0.62f), Offset(size.width, size.height * 0.38f), strokeWidth = 9.dp.toPx())
    }
}

/**
 * 기간 선택(H03) — 전체 기간 또는 한 달(이번 달부터 첫 기록의 달까지). 시트 안에서 고른 것은 "적용하기"를 눌러야 목록에
 * 들어간다(취소 · 바깥 · 뒤로는 그대로). 아직 오지 않은 달은 목록에 없다.
 */
@Composable
fun PeriodSheet(
    current: RecordPeriod,
    thisMonth: YearMonth,
    firstMonth: YearMonth,
    onApply: (RecordPeriod) -> Unit,
    onDismiss: () -> Unit,
) {
    val t = runTone()
    var pending by remember { mutableStateOf(current) }
    val monthFormat = rememberFormatter(R.string.rec_month_pattern)
    val months = remember(thisMonth, firstMonth, current) {
        val listed = generateSequence(thisMonth) { it.minusMonths(1) }.takeWhile { !it.isBefore(firstMonth) }.toList()
        val chosen = (current as? RecordPeriod.Month)?.month?.takeIf { !it.isAfter(thisMonth) }
        (listed + listOfNotNull(chosen)).distinct().sortedDescending()
    }
    RunSheet(onDismiss = onDismiss, modifier = Modifier.testTag("period-sheet"), showClose = false) {
        Text(
            stringResource(R.string.rec_period_pick),
            style = TextStyle(
                fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp, lineHeight = 1.3.em,
                letterSpacing = (-0.02).em, color = t.text,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        Column(
            Modifier.fillMaxWidth().heightIn(max = 336.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PeriodChoice(stringResource(R.string.rec_period_all), pending == RecordPeriod.All, "period-all") {
                pending = RecordPeriod.All
            }
            months.forEach { month ->
                PeriodChoice(monthFormat.format(month), pending == RecordPeriod.Month(month), "period-month-$month") {
                    pending = RecordPeriod.Month(month)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        RunButton(stringResource(R.string.run_rec_apply), { onApply(pending) }, Modifier.testTag("period-apply"), italic = true)
        Spacer(Modifier.height(10.dp))
        RunButton(stringResource(R.string.common_cancel), onDismiss, Modifier.testTag("period-cancel"), kind = RunButtonKind.Secondary)
    }
}

/** 기간 한 줄 — 왼쪽 동그라미(고르면 흰 바탕 체크) · 이름. 고른 줄은 파란 면 */
@Composable
private fun PeriodChoice(label: String, selected: Boolean, tag: String, onSelect: () -> Unit) {
    val t = runTone()
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(shape)
            .background(
                if (selected) t.cobalt else t.panel,
                shape,
            )
                        .feedbackClickable(role = Role.RadioButton, cue = FeedbackCue.Select, onClick = onSelect)
            .semantics { this.selected = selected }
            .testTag(tag)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(24.dp).clip(CircleShape)
                .then(if (selected) Modifier.background(Color.White) else Modifier.border(2.dp, t.label.copy(alpha = 0.8f), CircleShape)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = t.cobalt, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            label, style = runTextStyle(17.sp, if (selected) Color.White else t.text, FontWeight.Bold),
            textAlign = TextAlign.Start, modifier = Modifier.weight(1f),
        )
    }
}
