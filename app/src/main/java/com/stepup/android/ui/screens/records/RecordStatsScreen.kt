package com.stepup.android.ui.screens.records

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.data.local.RecordTotals
import com.stepup.android.domain.RunBar
import com.stepup.android.domain.RunMark
import com.stepup.android.domain.averagePace
import com.stepup.android.domain.localDate
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunCard
import com.stepup.android.ui.components.RunLinkRow
import com.stepup.android.ui.components.RunNumber
import com.stepup.android.ui.components.RunSpec
import com.stepup.android.ui.components.RunTabs
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpSans
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle as DateTextStyle
import kotlin.math.ceil

/**
 * 러닝 통계(시안 H04 · H05 · H06 · H15) — 목록의 "통계". 고른 달의 월간으로 시작하고 주간 · 월간을 오간다.
 * 주간 막대는 날짜별 거리, 월간 막대는 날짜 구간별 합계(달력 주가 아니다). 아직 오지 않은 날은 막대를 그리지 않는다.
 * 막대 칸 전체가 누르는 곳이고(고르면 그 날짜의 거리 · 횟수 · 기록으로 가는 카드), 스크린리더는 이전 · 다음 날짜 동작으로도 고른다.
 * 기록이 하나도 없으면 빈 축과 "—"만(H15) — 통계를 지어내지 않는다.
 */
@Composable
fun RecordStatsScreen(
    start: YearMonth,
    onBack: () -> Unit = {},
    onOpenStepStats: () -> Unit = {},
    onStartRun: () -> Unit = {},
    onOpenRun: (Long) -> Unit = {},
    viewModel: RecordStatsViewModel = viewModel(key = "stats-$start", factory = RecordStatsViewModel.factory(start)),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    RecordStatsContent(
        ui = ui, onBack = onBack, onWeekly = viewModel::showWeekly, onMonthly = viewModel::showMonthly,
        onPrevious = viewModel::previous, onNext = viewModel::next, onReload = viewModel::reload, onOpenStepStats = onOpenStepStats,
        onStartRun = onStartRun, onOpenRun = onOpenRun,
    )
}

@Composable
fun RecordStatsContent(
    ui: StatsUi?,
    onBack: () -> Unit = {},
    onWeekly: () -> Unit = {},
    onMonthly: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    onReload: () -> Unit = {},
    onOpenStepStats: () -> Unit = {},
    initialSelection: Int? = null,
    onStartRun: () -> Unit = {},
    onOpenRun: (Long) -> Unit = {},
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val t = runTone()
    // 고른 막대는 창(주 · 달)이 바뀌면 지운다
    var selected by rememberSaveable(ui?.window) { mutableStateOf(initialSelection) }
    RecordsFrame(onBack) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = RunSpec.Gutter).padding(top = 2.dp, bottom = 20.dp),
        ) {
            RecordTitle(stringResource(R.string.rec_stats_title))
            if (ui == null) return@Column
            val weekly = ui.window is StatWindow.Week
            RunTabs(
                listOf(stringResource(R.string.rec_weekly), stringResource(R.string.rec_monthly)),
                selected = if (weekly) 0 else 1,
                onSelect = { if (it == 0) onWeekly() else onMonthly() },
                modifier = Modifier.padding(top = 14.dp),
                tags = listOf("stats-weekly", "stats-monthly"),
            )
            WindowRow(ui, onPrevious, onNext)
            if (ui.failed) {
                RecordsState(
                    stringResource(R.string.run_rec_failed_title), stringResource(R.string.run_rec_failed_body),
                    Modifier.testTag("stats-failed"), actionLabel = stringResource(R.string.run_rec_reload), onAction = onReload,
                    actionTag = "stats-reload", refresh = true, scroll = false,
                )
                return@Column
            }
            val totals = ui.totals ?: return@Column
            val empty = totals.runs == 0
            val unknown = totals.runs > 0 && totals.measured == 0
            val chosen = selected?.takeIf { !empty && it in ui.bars.indices && !ui.bars[it].future }
            val emptyMessage: (@Composable () -> Unit)? = if (!empty) null else {
                { EmptyChartMessage(ui, weekly) }
            }
            if (weekly) {
                RunCard(Modifier.padding(top = 4.dp), padding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 10.dp)) {
                    TotalsRow(totals, empty, unknown)
                    Spacer(Modifier.height(18.dp))
                    BarChart(ui.bars, weekly = true, pick = chosen, onSelect = { selected = it }, empty = emptyMessage)
                }
            } else {
                RunCard(Modifier.padding(top = 4.dp), padding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)) {
                    TotalsStack(totals, empty, unknown)
                }
                RunCard(Modifier.padding(top = 12.dp), padding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.run_rec_week_chart), style = runTextStyle(16.sp, t.text, FontWeight.Bold),
                            modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.run_rec_km_axis), style = runTextStyle(12.sp, t.label, FontWeight.Medium))
                    }
                    Spacer(Modifier.height(6.dp))
                    BarChart(ui.bars, weekly = false, pick = chosen, onSelect = { selected = it }, empty = emptyMessage, axis = false)
                }
            }
            when {
                empty -> {
                    if (!ui.anyRecords) {
                        RunButton(
                            stringResource(R.string.run_rec_start), onStartRun,
                            Modifier.padding(top = 22.dp).testTag("stats-start-run"), italic = true,
                        )
                    }
                }
                chosen != null -> {
                    val bar = ui.bars[chosen]
                    val inside = ui.marks.filter { val day = it.startedAt.localDate(zone); !day.isBefore(bar.first) && !day.isAfter(bar.last) }
                    DayCard(bar, weekly, inside, onOpenRun, zone)
                }
                else -> ui.best?.let { BestDayCard(it) }
            }
            if (!empty) {
                TimeAndPace(totals)
                Text(stringResource(R.string.rec_stats_note), style = runTextStyle(13.sp, t.muted, FontWeight.Medium),
                    modifier = Modifier.padding(top = 14.dp))
            }
            // 예전 통계(걸음 · 기록 지도)는 지우지 않고 안쪽에 둔다
            RunLinkRow(stringResource(R.string.rec_steps_stats), onOpenStepStats, Modifier.padding(top = 14.dp).testTag("stats-steps"))
        }
    }
}

/** 이전 · 기간 · 다음 — 아직 오지 않은 기간으로는 가지 않는다. "9월 21일 – 27일" · "2026년 9월" */
@Composable
private fun WindowRow(ui: StatsUi, onPrevious: () -> Unit, onNext: () -> Unit) {
    val t = runTone()
    val weekly = ui.window is StatWindow.Week
    val day = rememberFormatter(R.string.date_month_day)
    val dayOnly = rememberFormatter(R.string.run_rec_day_only)
    val label = when (val w = ui.window) {
        is StatWindow.Week -> {
            val sunday = w.monday.plusDays(6)
            if (sunday.month == w.monday.month) "${day.format(w.monday)} – ${dayOnly.format(sunday)}"
            else "${day.format(w.monday)} – ${day.format(sunday)}"
        }
        is StatWindow.Month -> rememberFormatter(R.string.rec_month_pattern).format(w.month)
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        ArrowButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            stringResource(if (weekly) R.string.rec_prev_week else R.string.rec_prev_month), true, onPrevious, "stats-prev")
        Text(label, style = runTextStyle(20.sp, t.text, FontWeight.Bold), textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).testTag("stats-window"))
        ArrowButton(Icons.AutoMirrored.Filled.KeyboardArrowRight,
            stringResource(if (weekly) R.string.rec_next_week else R.string.rec_next_month), ui.canGoNext, onNext, "stats-next")
    }
}

@Composable
private fun ArrowButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tag: String,
) {
    val t = runTone()
    Box(
        Modifier.size(48.dp).clip(CircleShape)
            .feedbackClickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = label }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) t.text else t.muted.copy(alpha = 0.45f), modifier = Modifier.size(30.dp))
    }
}

/** 주간 합계(H04) — 총 거리 | 러닝 횟수. 기록이 없으면 "—" */
@Composable
private fun TotalsRow(totals: RecordTotals, empty: Boolean, unknown: Boolean) {
    val t = runTone()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1.15f)) {
            Text(stringResource(R.string.run_rec_total), style = runTextStyle(15.sp, t.label, FontWeight.SemiBold))
            RunNumber(
                if (empty || unknown) "—" else km(totals.meters), unit = "km", size = 58.sp, unitSize = 22.sp,
                italicUnit = false, valueTag = "stats-distance",
            )
        }
        Box(Modifier.padding(horizontal = 14.dp).width(1.5.dp).height(64.dp).background(t.divider))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.rec_runs_label), style = runTextStyle(15.sp, t.label, FontWeight.SemiBold))
            RunNumber(
                if (empty) "—" else "${totals.runs}", unit = pluralStringResource(R.plurals.run_rec_runs_unit, totals.runs),
                size = 52.sp, unitSize = 17.sp, italicUnit = false, valueTag = "stats-runs",
            )
        }
    }
}

/** 월간 합계(H06) — 총 거리 위, 총 러닝 횟수 아래 */
@Composable
private fun TotalsStack(totals: RecordTotals, empty: Boolean, unknown: Boolean) {
    val t = runTone()
    Text(stringResource(R.string.run_rec_total), style = runTextStyle(15.sp, t.label, FontWeight.SemiBold))
    RunNumber(
        if (empty || unknown) "—" else km(totals.meters), unit = "km", size = 72.sp, unitSize = 26.sp,
        italicUnit = false, valueTag = "stats-distance",
    )
    Box(Modifier.padding(vertical = 10.dp).fillMaxWidth().height(1.dp).background(t.divider))
    Text(stringResource(R.string.run_rec_total_runs), style = runTextStyle(15.sp, t.label, FontWeight.SemiBold))
    RunNumber(
        if (empty) "—" else "${totals.runs}", unit = pluralStringResource(R.plurals.run_rec_runs_unit, totals.runs),
        size = 58.sp, unitSize = 19.sp, italicUnit = false, valueTag = "stats-runs",
    )
}

/** 빈 축 가운데(H15) — 기록이 하나도 없으면 첫 러닝 안내, 이 기간만 비면 다른 기간 안내 */
@Composable
private fun EmptyChartMessage(ui: StatsUi, weekly: Boolean) {
    val t = runTone()
    val month = (ui.window as? StatWindow.Month)?.month
    val monthName = rememberFormatter(R.string.rec_month_name_pattern)
    Column(Modifier.testTag("stats-empty"), horizontalAlignment = Alignment.CenterHorizontally) {
        BarsGlyph()
        Spacer(Modifier.height(10.dp))
        if (!ui.anyRecords) {
            Text(stringResource(R.string.run_rec_stats_empty), style = runTextStyle(15.sp, t.text, FontWeight.SemiBold, 1.45f),
                textAlign = TextAlign.Center)
        } else {
            Text(
                when {
                    month == null -> stringResource(R.string.rec_stats_empty_week)
                    month == ui.thisMonth -> stringResource(R.string.rec_stats_empty_month)
                    else -> stringResource(R.string.rec_stats_empty_named_month, monthName.format(month))
                },
                style = runTextStyle(15.sp, t.text, FontWeight.SemiBold, 1.45f), textAlign = TextAlign.Center,
            )
            Text(stringResource(if (weekly) R.string.rec_stats_empty_week_body else R.string.rec_stats_empty_month_body),
                style = runTextStyle(13.sp, t.label, FontWeight.Medium), textAlign = TextAlign.Center)
        }
    }
}

/**
 * 막대 — 칸 전체가 누르는 곳. 고르면 그 막대만 시안 색, 위에 값 말풍선. 고르지 않았으면 막대마다 값.
 * 0km 인 지난 날은 바닥에 얇은 막대와 0, 아직 오지 않은 칸은 흐린 글자만. [empty] 면 막대 없이 축과 안내만.
 */
@Composable
private fun BarChart(
    bars: List<RunBar>,
    weekly: Boolean,
    pick: Int?,
    onSelect: (Int?) -> Unit,
    empty: (@Composable () -> Unit)?,
    axis: Boolean = true,
) {
    val t = runTone()
    val maxKm = bars.maxOfOrNull { it.meters / 1000.0 } ?: 0.0
    // 빈 축은 0–8 눈금만(값이 아니라 칸) — 막대 · 값은 그리지 않는다
    val step = if (empty != null) 2.0 else niceStep(maxKm)
    val top = if (empty != null) 8.0 else chartTop(maxKm, step)
    val ticks = (0..((top / step).toInt())).map { it * step }
    val dayFormat = rememberFormatter(R.string.date_month_day)
    val rangeLabel = @Composable { bar: RunBar -> stringResource(R.string.rec_bar_range, bar.first.dayOfMonth, bar.last.dayOfMonth) }
    val locale = LocalConfiguration.current.locales[0]
    val labels = bars.map { bar ->
        if (weekly) bar.first.dayOfWeek.getDisplayName(DateTextStyle.SHORT, locale) else rangeLabel(bar)
    }
    val spoken = bars.map { bar -> if (weekly) dayFormat.format(bar.first) else rangeLabel(bar) }
    val cds = bars.mapIndexed { i, bar ->
        if (bar.future) stringResource(R.string.rec_bar_future_cd, spoken[i])
        else stringResource(R.string.rec_bar_cd, spoken[i], km(bar.meters))
    }
    val previousDay = stringResource(R.string.rec_prev_day)
    val nextDay = stringResource(R.string.rec_next_day)
    val past = bars.indices.filter { !bars[it].future }
    val chartHeight = 196.dp
    val labelHeight = 34.dp
    val axisWidth = if (axis) 26.dp else 0.dp
    Column(
        Modifier.fillMaxWidth().testTag("stats-chart")
            .semantics {
                if (empty == null) {
                    customActions = listOf(
                        CustomAccessibilityAction(previousDay) {
                            val at = past.indexOf(pick ?: past.lastOrNull() ?: return@CustomAccessibilityAction false)
                            past.getOrNull(at - 1)?.let { onSelect(it); true } ?: false
                        },
                        CustomAccessibilityAction(nextDay) {
                            val at = pick?.let { past.indexOf(it) } ?: -1
                            past.getOrNull(at + 1)?.let { onSelect(it); true } ?: false
                        },
                    )
                }
            },
    ) {
        if (axis) {
            Text(stringResource(R.string.run_rec_km_axis), style = runTextStyle(12.sp, t.label, FontWeight.Medium),
                modifier = Modifier.padding(bottom = 14.dp))
        }
        Row(Modifier.fillMaxWidth().height(chartHeight + labelHeight)) {
            if (axis) {
                // 눈금 글자
                Box(Modifier.width(axisWidth).fillMaxHeight()) {
                    ticks.forEach { value ->
                        val fraction = (value / top).toFloat()
                        Text(
                            if (value % 1.0 == 0.0) value.toInt().toString() else String.format(java.util.Locale.ROOT, "%.1f", value),
                            style = runTextStyle(12.sp, t.label, FontWeight.Medium, 1.1f),
                            modifier = Modifier.align(Alignment.TopStart).offset(y = chartHeight * (1f - fraction) - 8.dp),
                        )
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                Canvas(Modifier.fillMaxWidth().height(chartHeight)) {
                    val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
                    ticks.forEach { value ->
                        val y = size.height * (1f - (value / top).toFloat())
                        drawLine(
                            t.divider, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx(),
                            pathEffect = if (value == 0.0) null else dash,
                        )
                    }
                    if (axis) drawLine(t.divider, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 1.dp.toPx())
                }
                Row(Modifier.fillMaxSize()) {
                    bars.forEachIndexed { index, bar ->
                        val on = pick == index
                        Column(
                            Modifier.weight(1f).fillMaxHeight()
                                .feedbackClickable(enabled = empty == null && !bar.future, role = null) { onSelect(if (on) null else index) }
                                .semantics(mergeDescendants = true) {
                                    role = Role.Button
                                    selected = on
                                    contentDescription = cds[index]
                                }
                                .testTag("stats-bar-$index"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(Modifier.fillMaxWidth().height(chartHeight)) {
                                if (empty == null && !bar.future) Bar(bar, top, chartHeight, weekly, on)
                            }
                            Box(Modifier.fillMaxWidth().height(labelHeight), contentAlignment = Alignment.Center) {
                                Text(
                                    labels[index], maxLines = 1,
                                    style = runTextStyle(
                                        13.sp,
                                        when {
                                            bar.future -> t.muted.copy(alpha = 0.55f)
                                            on -> t.text
                                            else -> t.label
                                        },
                                        if (on) FontWeight.Bold else FontWeight.Medium, 1.1f,
                                    ),
                                )
                            }
                        }
                    }
                }
                if (empty != null) {
                    Box(Modifier.fillMaxWidth().height(chartHeight).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                        empty()
                    }
                }
            }
        }
    }
}

/** 막대 하나 — 막대는 눈금과 같은 높이로, 값(고른 막대는 시안 말풍선)은 막대 위에. 0km 는 바닥의 얇은 막대와 0 */
@Composable
private fun BoxScope.Bar(bar: RunBar, top: Double, chartHeight: androidx.compose.ui.unit.Dp, weekly: Boolean, on: Boolean) {
    val t = runTone()
    val km = bar.meters / 1000.0
    val value = if (km <= 0.0) "0" else String.format(java.util.Locale.ROOT, "%.1f", km)
    val barHeight = if (km <= 0.0) 3.dp else chartHeight * (km / top).toFloat().coerceIn(0.03f, 1f)
    val shape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 1.dp, bottomEnd = 1.dp)
    Box(
        Modifier.align(Alignment.BottomCenter).width(if (weekly) 26.dp else 34.dp).height(barHeight).clip(shape)
            .background(
                if (on) Brush.verticalGradient(listOf(Color(0xFF7EE8FF), Color(0xFF29BFF0)))
                else Brush.verticalGradient(listOf(Color(0xFF2E7BFF), Color(0xFF0754FF))),
            ),
    )
    Column(
        Modifier.align(Alignment.BottomCenter).offset(y = -(barHeight + 4.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (on) {
            // 고른 막대 — 시안 말풍선 + 아래 꼭지
            Box(Modifier.clip(RoundedCornerShape(7.dp)).background(t.cyan).padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text(value, style = runTextStyle(14.sp, Color(0xFF041A33), FontWeight.ExtraBold, 1.1f), modifier = Modifier.testTag("stats-tip"))
            }
            Box(
                Modifier.size(width = 12.dp, height = 6.dp).background(
                    t.cyan,
                    GenericShape { size, _ ->
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width / 2f, size.height)
                        close()
                    },
                ),
            )
        } else {
            Text(value, style = runTextStyle(13.sp, t.text, FontWeight.Bold, 1.1f))
        }
    }
}

/** 고른 날짜(H05) — "9월 25일 금요일" · 거리 | 횟수. 그 날 러닝이 하나면 카드를 누르면 그 기록(H11 · H13), 여럿이면 줄마다 */
@Composable
private fun DayCard(bar: RunBar, weekly: Boolean, runs: List<RunMark>, onOpenRun: (Long) -> Unit, zone: ZoneId) {
    val t = runTone()
    val single = runs.singleOrNull()?.id?.takeIf { it > 0 }
    val label = if (weekly) rememberFormatter(R.string.run_rec_day_pattern).format(bar.first)
    else stringResource(R.string.rec_bar_range, bar.first.dayOfMonth, bar.last.dayOfMonth)
    RunCard(
        Modifier.padding(top = 12.dp), padding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        onClick = single?.let { id -> { onOpenRun(id) } }, tag = "stats-day-card",
    ) {
        Row(
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.testTag("stats-day"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1.2f)) {
                Text(label, style = runTextStyle(15.sp, t.text, FontWeight.Bold))
                RunNumber(km(bar.meters), unit = "km", size = 46.sp, unitSize = 20.sp, color = t.cyan, italicUnit = false)
            }
            Box(Modifier.padding(horizontal = 12.dp).width(1.5.dp).height(52.dp).background(t.divider))
            RunNumber(
                "${bar.runs}", unit = pluralStringResource(R.plurals.run_rec_runs_unit, bar.runs), size = 40.sp, unitSize = 16.sp,
                italicUnit = false, modifier = Modifier.weight(1f),
            )
            if (single != null) Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = t.label, modifier = Modifier.size(24.dp))
        }
        if (runs.size > 1) {
            val time = rememberFormatter(R.string.rec_time_pattern)
            Spacer(Modifier.height(8.dp))
            runs.sortedBy { it.startedAt }.forEach { mark ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp))
                        .feedbackClickable { onOpenRun(mark.id) }
                        .testTag("stats-run-${mark.id}")
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        time.format(Instant.ofEpochMilli(mark.startedAt).atZone(zone)),
                        style = runTextStyle(15.sp, t.label, FontWeight.SemiBold), modifier = Modifier.weight(1f),
                    )
                    Text(if (mark.meters > 0) "${km2(mark.meters)} km" else "—", style = runTextStyle(16.sp, t.text, FontWeight.Bold))
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = t.label, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

/** 가장 많이 달린 날(H04 · H06) — 날짜 | 거리(시안 색) */
@Composable
private fun BestDayCard(best: RunBar) {
    val t = runTone()
    RunCard(Modifier.padding(top = 12.dp), padding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)) {
        Row(
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.testTag("stats-best"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1.2f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = t.cyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.run_rec_best_day), style = runTextStyle(15.sp, t.label, FontWeight.SemiBold))
                }
                Text(
                    rememberFormatter(R.string.date_month_day).format(best.first),
                    style = TextStyle(
                        fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic,
                        fontSize = 32.sp, letterSpacing = (-0.02).em, color = t.text,
                    ),
                    maxLines = 1,
                )
            }
            Box(Modifier.padding(horizontal = 12.dp).width(1.5.dp).height(52.dp).background(t.divider))
            RunNumber(km(best.meters), unit = "km", size = 44.sp, unitSize = 20.sp, color = t.cyan, italicUnit = false,
                modifier = Modifier.weight(1f))
        }
    }
}

/** 달린 시간 · 평균 페이스 — 기간 전체(평균 페이스는 거리 있는 러닝의 시간 합 ÷ 거리 합) */
@Composable
private fun TimeAndPace(totals: RecordTotals) {
    val t = runTone()
    RunCard(Modifier.padding(top = 12.dp), padding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}.testTag("stats-time")) {
                Text(stringResource(R.string.run_label_time), style = runTextStyle(14.sp, t.label, FontWeight.SemiBold))
                Text(durationText(totals.seconds), style = runTextStyle(19.sp, t.text, FontWeight.Bold), maxLines = 1)
            }
            Box(Modifier.padding(horizontal = 12.dp).width(1.5.dp).height(40.dp).background(t.divider))
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}.testTag("stats-pace")) {
                Text(stringResource(R.string.run_label_pace), style = runTextStyle(14.sp, t.label, FontWeight.SemiBold))
                Text(paceText(averagePace(totals.pacedSeconds, totals.pacedMeters)) ?: "—",
                    style = runTextStyle(19.sp, t.text, FontWeight.Bold), maxLines = 1)
            }
        }
    }
}

/**
 * 눈금 간격 — 칸은 셋 이하, 맨 위는 가장 큰 막대보다 한 뼘(10%) 이상 높게, 그중 맨 위가 가장 낮은 것(같으면 칸이 적은 것).
 * 예: 주간 5.0km → 3 간격(0 · 3 · 6), 월간 12.2km → 5 간격(0 · 5 · 10 · 15)
 */
internal fun niceStep(maxKm: Double): Double {
    if (maxKm <= 0.0) return 0.5
    val steps = listOf(0.5, 1.0, 2.0, 3.0, 5.0, 10.0, 20.0, 25.0, 50.0, 100.0, 200.0, 250.0, 500.0, 1000.0)
    return steps.filter { ceil(maxKm / it) <= 3 && chartTop(maxKm, it) >= maxKm * 1.1 }
        .minWithOrNull(compareBy<Double> { chartTop(maxKm, it) }.thenByDescending { it }) ?: 1000.0
}

/** 막대 그림의 맨 위 값 */
internal fun chartTop(maxKm: Double, step: Double): Double = step * ceil(maxKm / step).coerceAtLeast(1.0)
