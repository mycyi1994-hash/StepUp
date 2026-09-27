package com.stepup.android.ui.screens.records

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.domain.RunBar
import com.stepup.android.domain.averagePace
import com.stepup.android.ui.components.SecondaryHeader
import com.stepup.android.ui.components.SettingsNavRow
import com.stepup.android.ui.components.settingsPalette
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.StepUpDesign
import java.time.YearMonth
import java.time.format.TextStyle
import kotlin.math.ceil

/**
 * 러닝 통계(04 · 05 · 06 · 15) — 목록의 "통계". 고른 달의 월간으로 시작하고 주간 · 월간을 오간다.
 * 주간 막대는 날짜별 거리, 월간 막대는 날짜 구간별 합계(달력 주가 아니다). 아직 오지 않은 날은 막대를 그리지 않는다.
 * 날짜 칸 전체가 누르는 곳이고, 스크린리더는 이전 · 다음 날짜 동작으로도 고른다.
 */
@Composable
fun RecordStatsScreen(
    start: YearMonth,
    onBack: () -> Unit = {},
    onOpenStepStats: () -> Unit = {},
    viewModel: RecordStatsViewModel = viewModel(key = "stats-$start", factory = RecordStatsViewModel.factory(start)),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    RecordStatsContent(
        ui = ui, onBack = onBack, onWeekly = viewModel::showWeekly, onMonthly = viewModel::showMonthly,
        onPrevious = viewModel::previous, onNext = viewModel::next, onReload = viewModel::reload, onOpenStepStats = onOpenStepStats,
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
) {
    val p = settingsPalette()
    // 고른 막대는 창(주 · 달)이 바뀌면 지운다
    var selected by rememberSaveable(ui?.window) { mutableStateOf(initialSelection) }
    Column(Modifier.fillMaxSize().padding(horizontal = StepUpDesign.Gutter)) {
        SecondaryHeader(onBack = onBack, balance = null, onOpenWallet = null, title = stringResource(R.string.rec_stats_title))
        if (ui == null) return@Column
        val weekly = ui.window is StatWindow.Week
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            WeekMonthSwitch(weekly, onWeekly, onMonthly)
            WindowRow(ui, onPrevious, onNext)
            if (ui.failed) {
                RecordsState(stringResource(R.string.rec_failed_title), stringResource(R.string.rec_failed_body),
                    Modifier.testTag("stats-failed"), actionLabel = stringResource(R.string.set_reload), onAction = onReload,
                    refresh = true, scroll = false)
                return@Column
            }
            val totals = ui.totals ?: return@Column
            val unknown = totals.runs > 0 && totals.measured == 0
            val month = (ui.window as? StatWindow.Month)?.month
            val monthName = rememberFormatter(R.string.rec_month_name_pattern)
            Text(
                when {
                    month == null -> stringResource(R.string.rec_week_distance)
                    month == ui.thisMonth -> stringResource(R.string.rec_card_this_month)
                    else -> stringResource(R.string.rec_card_month, monthName.format(month))
                },
                color = p.secondary, fontSize = 15.sp, modifier = Modifier.padding(top = 18.dp),
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(if (unknown) "—" else km(totals.meters), color = p.text, fontSize = 49.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.alignByBaseline().testTag("stats-distance"))
                if (!unknown) {
                    Spacer(Modifier.width(12.dp))
                    Text("km", color = p.secondary, fontSize = 20.sp, modifier = Modifier.alignByBaseline())
                }
            }
            if (totals.runs == 0) {
                RecordsState(
                    when {
                        month == null -> stringResource(R.string.rec_stats_empty_week)
                        month == ui.thisMonth -> stringResource(R.string.rec_stats_empty_month)
                        else -> stringResource(R.string.rec_stats_empty_named_month, monthName.format(month))
                    },
                    stringResource(if (weekly) R.string.rec_stats_empty_week_body else R.string.rec_stats_empty_month_body),
                    Modifier.testTag("stats-empty"), scroll = false,
                )
                return@Column
            }
            BarChart(ui.bars, weekly, selected, onSelect = { selected = it })
            Text(stringResource(if (weekly) R.string.rec_chart_hint_week else R.string.rec_chart_hint_month),
                color = p.secondary, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
            Box(Modifier.padding(top = 22.dp).fillMaxWidth().height(1.dp).background(p.divider))
            StatLine(stringResource(R.string.rec_runs_label), pluralStringResource(R.plurals.rec_runs_times, totals.runs, totals.runs), "stats-runs")
            StatLine(stringResource(R.string.rec_time_label), durationText(totals.seconds), "stats-time")
            StatLine(stringResource(R.string.rec_pace_label),
                paceText(averagePace(totals.pacedSeconds, totals.pacedMeters)) ?: "—", "stats-pace")
            Text(stringResource(R.string.rec_stats_note), color = p.secondary, fontSize = 13.sp, modifier = Modifier.padding(top = 18.dp))
            // 예전 통계(걸음 · 기록 지도)는 지우지 않고 안쪽에 둔다
            SettingsNavRow(stringResource(R.string.rec_steps_stats), onOpenStepStats,
                Modifier.padding(top = 18.dp).testTag("stats-steps"))
        }
    }
}

/** 주간 · 월간 — 고른 쪽은 한 단계 밝은 면. 누르는 곳은 48dp */
@Composable
private fun WeekMonthSwitch(weekly: Boolean, onWeekly: () -> Unit, onMonthly: () -> Unit) {
    val p = settingsPalette()
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(16.dp)).background(p.surface).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(true to R.string.rec_weekly, false to R.string.rec_monthly).forEach { (week, label) ->
            val on = week == weekly
            Box(
                Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(13.dp))
                    .background(if (on) selectedFace() else Color.Transparent)
                    .feedbackClickable(role = null) { if (week) onWeekly() else onMonthly() }
                    .semantics {
                        role = Role.Tab
                        selected = on
                    }
                    .testTag(if (week) "stats-weekly" else "stats-monthly"),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(label), color = if (on) p.text else p.secondary, fontSize = 16.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

/** 이전 · 기간 · 다음 — 아직 오지 않은 기간으로는 가지 않는다 */
@Composable
private fun WindowRow(ui: StatsUi, onPrevious: () -> Unit, onNext: () -> Unit) {
    val p = settingsPalette()
    val weekly = ui.window is StatWindow.Week
    val day = rememberFormatter(R.string.date_month_day)
    val label = when (val w = ui.window) {
        is StatWindow.Week -> "${day.format(w.monday)} – ${day.format(w.monday.plusDays(6))}"
        is StatWindow.Month -> rememberFormatter(R.string.rec_month_pattern).format(w.month)
    }
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp).testTag("stats-prev")) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                stringResource(if (weekly) R.string.rec_prev_week else R.string.rec_prev_month), tint = p.text)
        }
        Text(label, color = p.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).testTag("stats-window"))
        IconButton(onClick = onNext, enabled = ui.canGoNext, modifier = Modifier.size(48.dp).testTag("stats-next")) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight,
                stringResource(if (weekly) R.string.rec_next_week else R.string.rec_next_month),
                tint = if (ui.canGoNext) p.text else p.secondary.copy(alpha = 0.35f))
        }
    }
}

/**
 * 막대 — 칸 전체가 누르는 곳. 고르면 그 막대만 밝게, 위에 "9월 25일 · 5.0km". 고르지 않았으면 막대마다 값.
 * 0km 인 지난 날은 작은 점, 아직 오지 않은 칸은 흐린 글자만.
 */
@Composable
private fun BarChart(bars: List<RunBar>, weekly: Boolean, pick: Int?, onSelect: (Int?) -> Unit) {
    val p = settingsPalette()
    val maxKm = bars.maxOfOrNull { it.meters / 1000.0 } ?: 0.0
    val step = niceStep(maxKm)
    val top = chartTop(maxKm, step)
    val ticks = (0..((top / step).toInt())).map { it * step }
    val dayFormat = rememberFormatter(R.string.date_month_day)
    val rangeLabel = @Composable { bar: RunBar -> stringResource(R.string.rec_bar_range, bar.first.dayOfMonth, bar.last.dayOfMonth) }
    val locale = LocalConfiguration.current.locales[0]
    val labels = bars.map { bar ->
        if (weekly) bar.first.dayOfWeek.getDisplayName(TextStyle.SHORT, locale) else rangeLabel(bar)
    }
    val spoken = bars.map { bar -> if (weekly) dayFormat.format(bar.first) else rangeLabel(bar) }
    val cds = bars.mapIndexed { i, bar ->
        if (bar.future) stringResource(R.string.rec_bar_future_cd, spoken[i])
        else stringResource(R.string.rec_bar_cd, spoken[i], km(bar.meters))
    }
    val chosen = pick?.takeIf { it in bars.indices && !bars[it].future }
    val previousDay = stringResource(R.string.rec_prev_day)
    val nextDay = stringResource(R.string.rec_next_day)
    val past = bars.indices.filter { !bars[it].future }
    val tipHeight = 40.dp
    val chartHeight = 196.dp
    val labelHeight = 34.dp
    Column(
        Modifier.fillMaxWidth().padding(top = 12.dp).testTag("stats-chart")
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(previousDay) {
                        val at = past.indexOf(chosen ?: past.lastOrNull() ?: return@CustomAccessibilityAction false)
                        past.getOrNull(at - 1)?.let { onSelect(it); true } ?: false
                    },
                    CustomAccessibilityAction(nextDay) {
                        val at = chosen?.let { past.indexOf(it) } ?: -1
                        past.getOrNull(at + 1)?.let { onSelect(it); true } ?: false
                    },
                )
            },
    ) {
        // 고른 칸의 거리 — 그 칸 위에
        Box(Modifier.fillMaxWidth().height(tipHeight).padding(start = 30.dp)) {
            if (chosen != null) {
                val bias = if (bars.size <= 1) 0f else (chosen.toFloat() / (bars.size - 1)) * 2f - 1f
                Box(
                    Modifier.align(BiasAlignment(bias.coerceIn(-1f, 1f), 0f)).clip(RoundedCornerShape(12.dp))
                        .background(if (StepUpColors.dark) Color(0xFF273B58) else p.surface)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(stringResource(R.string.rec_selected_day, spoken[chosen], km(bars[chosen].meters)), color = p.text,
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("stats-tip"))
                }
            } else {
                Text("km", color = p.secondary, fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomEnd))
            }
        }
        Row(Modifier.fillMaxWidth().height(chartHeight + labelHeight)) {
            // 눈금 글자
            Box(Modifier.width(30.dp).fillMaxHeight()) {
                ticks.forEach { value ->
                    val fraction = (value / top).toFloat()
                    Text(
                        if (value % 1.0 == 0.0) value.toInt().toString() else String.format(java.util.Locale.ROOT, "%.1f", value),
                        color = p.secondary, fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.TopStart).offset(y = chartHeight * (1f - fraction) - 7.dp),
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                Canvas(Modifier.fillMaxWidth().height(chartHeight)) {
                    ticks.forEach { value ->
                        val y = size.height * (1f - (value / top).toFloat())
                        drawLine(p.divider, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y),
                            strokeWidth = 1.dp.toPx())
                    }
                }
                Row(Modifier.fillMaxSize()) {
                    bars.forEachIndexed { index, bar ->
                        val on = chosen == index
                        Column(
                            Modifier.weight(1f).fillMaxHeight()
                                .feedbackClickable(enabled = !bar.future, role = null) { onSelect(if (on) null else index) }
                                .semantics(mergeDescendants = true) {
                                    role = Role.Button
                                    selected = on
                                    contentDescription = cds[index]
                                }
                                .testTag("stats-bar-$index"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(Modifier.fillMaxWidth().height(chartHeight), contentAlignment = Alignment.BottomCenter) {
                                val km = bar.meters / 1000.0
                                when {
                                    bar.future -> Unit
                                    km <= 0.0 -> Box(Modifier.padding(bottom = 0.dp).size(4.dp).clip(CircleShape).background(p.secondary.copy(alpha = 0.6f)))
                                    else -> {
                                        val fraction = (km / top).toFloat().coerceIn(0.02f, 1f)
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            if (chosen == null) {
                                                Text(String.format(java.util.Locale.ROOT, "%.1f", km), color = p.accent, fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
                                            }
                                            Box(
                                                Modifier.width(if (weekly) 26.dp else 34.dp).height(chartHeight * fraction)
                                                    .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                                                    .background(
                                                        if (chosen == null || on) Brush.verticalGradient(listOf(Color(0xFFACCAFF), Color(0xFF527EBD)))
                                                        else Brush.verticalGradient(listOf(Color(0xFF304765), Color(0xFF304765))),
                                                    ),
                                            )
                                        }
                                    }
                                }
                            }
                            Box(Modifier.fillMaxWidth().height(labelHeight), contentAlignment = Alignment.Center) {
                                Text(labels[index], fontSize = 12.sp, maxLines = 1,
                                    color = if (bar.future) p.secondary.copy(alpha = 0.45f) else if (on) p.text else p.secondary,
                                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 눈금 간격 — 칸은 셋 이하, 맨 위는 가장 큰 막대보다 한 뼘(10%) 이상 높게, 그중 맨 위가 가장 낮은 것(같으면 칸이 적은 것).
 * 시안: 주간 5.0km → 3 간격(0 · 3 · 6), 월간 12.2km → 5 간격(0 · 5 · 10 · 15)
 */
internal fun niceStep(maxKm: Double): Double {
    if (maxKm <= 0.0) return 0.5
    val steps = listOf(0.5, 1.0, 2.0, 3.0, 5.0, 10.0, 20.0, 25.0, 50.0, 100.0, 200.0, 250.0, 500.0, 1000.0)
    return steps.filter { ceil(maxKm / it) <= 3 && chartTop(maxKm, it) >= maxKm * 1.1 }
        .minWithOrNull(compareBy<Double> { chartTop(maxKm, it) }.thenByDescending { it }) ?: 1000.0
}

/** 막대 그림의 맨 위 값 */
internal fun chartTop(maxKm: Double, step: Double): Double = step * ceil(maxKm / step).coerceAtLeast(1.0)

@Composable
private fun StatLine(label: String, value: String, tag: String) {
    val p = settingsPalette()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(top = 10.dp).semantics(mergeDescendants = true) {}.testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = p.secondary, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(value, color = p.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}
