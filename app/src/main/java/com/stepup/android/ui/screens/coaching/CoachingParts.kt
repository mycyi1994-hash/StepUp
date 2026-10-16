package com.stepup.android.ui.screens.coaching

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.coaching.CoachCandidate
import com.stepup.android.domain.coaching.CoachFact
import com.stepup.android.domain.coaching.CoachGoal
import com.stepup.android.domain.coaching.CoachIds
import com.stepup.android.domain.coaching.KmSplit
import com.stepup.android.domain.coaching.PaceAxis
import com.stepup.android.domain.coaching.StepRole
import com.stepup.android.domain.coaching.WeekSummary
import com.stepup.android.domain.coaching.WorkoutFamily
import com.stepup.android.domain.coaching.WorkoutStep
import com.stepup.android.domain.coaching.WorkoutTemplate
import com.stepup.android.domain.coaching.kmTextShort
import com.stepup.android.domain.coaching.paceText
import com.stepup.android.ui.components.RunBackdrop
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.RunSheetText
import com.stepup.android.ui.components.RunTopBar
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.records.rememberFormatter
import com.stepup.android.ui.theme.StepUpSans
import java.time.LocalDate

/*
 * AI 코칭 화면들의 공통 조각 — 러닝 화면과 같은 남색 바닥 · 공용 머리(RunTopBar: 뒤로 · 공식 로고) · RunStyle 버튼과 시트.
 * 화면마다 따로 그린 뒤로 화살표 · 로고 · 하단 탭은 두지 않는다(공용 틀이 그린다).
 */

/** 코칭 화면 틀 — 남색 바닥 · 머리(뒤로 · 로고 · 오른쪽 행동 하나). 상태 막대 · 하단 탭 자리는 앱 틀이 이미 비웠다 */
@Composable
internal fun CoachFrame(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        RunBackdrop(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize()) {
            RunTopBar(onBack = onBack, trailing = trailing, backTag = "coach-back")
            content()
        }
    }
}

/** 큰 굵은 제목 — "코칭" 옆에 작은 "AI"(01 · 03 · 04), 화면 제목("오늘의 훈련" · "AI 코칭 설정") */
@Composable
internal fun CoachTitle(text: String, modifier: Modifier = Modifier, badge: String? = null) {
    val t = runTone()
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            text,
            style = TextStyle(
                fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 1.2.em,
                letterSpacing = (-0.03).em, color = t.text,
            ),
            modifier = Modifier.semantics { heading() },
        )
        if (badge != null) {
            Spacer(Modifier.width(8.dp))
            Text(badge, style = runTextStyle(20.sp, t.label, FontWeight.SemiBold), modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}

/** 둥근 그림 버튼(설정 톱니) — 누르는 곳 48dp */
@Composable
internal fun CoachIconButton(icon: ImageVector, label: String, onClick: () -> Unit, tag: String) {
    val t = runTone()
    Box(
        Modifier.size(48.dp).clip(CircleShape).feedbackClickable(onClick = onClick)
            .semantics { contentDescription = label }.testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = t.text, modifier = Modifier.size(26.dp))
    }
}

/** 소제목 — "1km 구간 페이스" · "최근 4주" · "기록이 생기면" */
@Composable
internal fun CoachSection(text: String, modifier: Modifier = Modifier) {
    val t = runTone()
    Text(text, style = runTextStyle(19.sp, t.text, FontWeight.ExtraBold, 1.3f), modifier = modifier.semantics { heading() })
}

/**
 * 왼쪽 파란 세로줄 인용 — AI 칸과 안내 한 줄(시안의 "AI 코칭" · "AI 코치의 제안" · "코칭 안내"). [label] 아래에 내용.
 * [extra] 는 이름표 오른쪽(생성 시각 · 모의 응답 · 진행 표시).
 */
@Composable
internal fun CoachQuote(
    label: String?,
    modifier: Modifier = Modifier,
    extra: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val t = runTone()
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(Modifier.width(4.dp).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(t.cobalt))
        Column(Modifier.weight(1f).padding(start = 16.dp, top = 2.dp, bottom = 2.dp)) {
            if (label != null || extra != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (label != null) Text(label, style = runTextStyle(14.sp, t.cobaltText, FontWeight.SemiBold))
                    if (extra != null) {
                        Spacer(Modifier.width(10.dp))
                        extra()
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            content()
        }
    }
}

/** 인용 안의 큰 줄(근거 · 상태 제목) */
@Composable
internal fun CoachQuoteTitle(text: String, modifier: Modifier = Modifier, dim: Boolean = false) {
    val t = runTone()
    Text(text, style = runTextStyle(21.sp, if (dim) t.label else t.text, FontWeight.ExtraBold, 1.35f), modifier = modifier)
}

/** 인용 안의 작은 줄(AI 해석 · 이유 · 안내) */
@Composable
internal fun CoachQuoteBody(text: String, modifier: Modifier = Modifier) {
    val t = runTone()
    Text(text, style = runTextStyle(15.sp, t.label, FontWeight.Medium, 1.45f), modifier = modifier.padding(top = 4.dp))
}

/** 작은 이름표 알약 — "모의 응답"(실제 AI 처럼 보이지 않게) · "제안 · 아직 적용 전" */
@Composable
internal fun CoachPill(text: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    val t = runTone()
    val shape = RoundedCornerShape(50)
    Text(
        text,
        style = runTextStyle(13.sp, if (accent) t.text else t.chipInk, FontWeight.SemiBold, 1.2f),
        modifier = modifier.clip(shape).background(if (accent) t.cobalt else t.chipFace, shape)
            .border(1.dp, if (accent) t.cobalt else t.chipEdge, shape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/** 글자 이동 줄 — "다음 주 계획 보기 →" · "저장된 분석 보기 →" · "기본 기록 보기 →". 위아래 얇은 줄 */
@Composable
internal fun CoachLinkRow(text: String, onClick: () -> Unit, tag: String, modifier: Modifier = Modifier, sub: String? = null) {
    val t = runTone()
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).feedbackClickable(onClick = onClick).testTag(tag).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text, style = runTextStyle(19.sp, t.text, FontWeight.Bold, 1.3f))
            if (sub != null) Text(sub, style = runTextStyle(14.sp, t.cobaltText, FontWeight.Medium))
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = t.text, modifier = Modifier.size(26.dp))
    }
}

/** 고르는 칸 셋(2회 · 3회 · 4회 / 15분 · 25분 · 40분) — 하나만. 고른 칸은 파란 면 */
@Composable
internal fun CoachOptions(options: List<String>, selected: Int?, onSelect: (Int) -> Unit, tagPrefix: String, modifier: Modifier = Modifier) {
    val t = runTone()
    val shape = RoundedCornerShape(14.dp)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEachIndexed { index, label ->
            val on = index == selected
            Box(
                Modifier.weight(1f).heightIn(min = 56.dp).clip(shape)
                    .background(if (on) t.cobalt else t.panel, shape)
                    .border(1.dp, if (on) t.cobalt else t.panelEdge, shape)
                    .feedbackClickable(role = Role.RadioButton, cue = FeedbackCue.Select) { onSelect(index) }
                    .semantics { this.selected = on }
                    .testTag("$tagPrefix-$index"),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = runTextStyle(17.sp, if (on) Color.White else t.text, if (on) FontWeight.Bold else FontWeight.Medium))
            }
        }
    }
}

/** 체크 칸 한 줄(동의 05 · 분석 삭제 15) — 줄 전체가 누르는 곳 */
@Composable
internal fun CoachCheckRow(
    checked: Boolean,
    onToggle: () -> Unit,
    title: String,
    tag: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    enabled: Boolean = true,
) {
    val t = runTone()
    Row(
        modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(12.dp))
            .feedbackClickable(enabled = enabled, role = Role.Checkbox, cue = FeedbackCue.Toggle, onClick = onToggle)
            .semantics { toggleableState = if (checked) ToggleableState.On else ToggleableState.Off }
            .testTag(tag).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (checked) Icons.Filled.CheckBox else Icons.Outlined.CheckBoxOutlineBlank, contentDescription = null,
            tint = if (checked) t.cobalt else t.text, modifier = Modifier.size(30.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = runTextStyle(16.sp, if (enabled) t.text else t.muted, FontWeight.SemiBold, 1.35f))
            if (sub != null) Text(sub, style = runTextStyle(14.sp, t.label, FontWeight.Medium, 1.35f))
        }
    }
}

/** 데이터 이용 안내(05 · 14 · 15의 링크) — 보내는 것 · 보내지 않는 것 · 계정 연결 · 해제 · 보관. 확정 전 초안 문구다 */
@Composable
internal fun CoachDataSheet(onDismiss: () -> Unit) {
    val t = runTone()
    RunSheet(onDismiss = onDismiss, modifier = Modifier.testTag("coach-data-sheet")) {
        RunSheetText(stringResource(R.string.coach_data_title))
        Spacer(Modifier.height(16.dp))
        listOf(
            R.string.coach_data_send, R.string.coach_data_not_send, R.string.coach_data_account,
            R.string.coach_data_withdraw, R.string.coach_data_retention,
        ).forEach { line ->
            Text(
                stringResource(line), style = runTextStyle(15.sp, t.text, FontWeight.Medium, 1.5f),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ── 글자(코드가 만든 숫자) ─────────────────────────────────────────

/** 근거 id → 앱의 번역 글자. 숫자는 서버가 저장된 기록으로 만든 값이다(AI 가 쓴 숫자가 아니다). 모르는 id 는 null */
@Composable
internal fun factText(fact: CoachFact): String? {
    val v = fact.values
    return when (fact.id) {
        CoachIds.FACT_SPLIT_LAST_VS_FIRST -> {
            val km = v["last_km"]?.toInt()
            val delta = v["delta_sec"]
            when {
                km == null || delta == null -> null
                delta > 0 -> stringResource(R.string.coach_fact_last_slower, km, delta.toInt())
                delta < 0 -> stringResource(R.string.coach_fact_last_faster, km, (-delta).toInt())
                else -> stringResource(R.string.coach_fact_even)
            }
        }
        CoachIds.FACT_SPLIT_EVEN -> stringResource(R.string.coach_fact_even)
        CoachIds.FACT_AVG_PACE -> paceText(v["pace_sec"]?.toDouble())?.let { stringResource(R.string.coach_fact_avg_pace, it) }
        CoachIds.FACT_WEEK_STREAK -> v["weeks"]?.toInt()?.takeIf { it > 0 }?.let { stringResource(R.string.coach_fact_streak, it) }
        CoachIds.FACT_WEEK_RUNS -> {
            val runs = v["runs"]?.toInt()
            val goal = v["goal"]?.toInt() ?: 0
            when {
                runs == null -> null
                goal > 0 -> stringResource(R.string.coach_fact_week_runs_goal, runs, goal)
                else -> stringResource(R.string.coach_fact_week_runs, runs)
            }
        }
        CoachIds.FACT_WEEK_DISTANCE -> v["meters"]?.let { stringResource(R.string.coach_fact_week_distance, kmTextShort(it.toDouble())) }
        else -> null
    }
}

/** 페이스 후보의 제목 — [long] 은 확인 시트(16)의 두 줄 제목 */
@Composable
internal fun candidateTitle(candidate: CoachCandidate, long: Boolean): String? = when (candidate.id) {
    CoachIds.CAND_PACE_START_EASIER ->
        stringResource(if (long) R.string.coach_cand_start_easier_long else R.string.coach_cand_start_easier)
    CoachIds.CAND_PACE_KEEP -> stringResource(if (long) R.string.coach_cand_keep_long else R.string.coach_cand_keep)
    else -> null
}

/** 페이스 후보의 작은 줄 — "7'30\"/km로 시작해 보세요" */
@Composable
internal fun candidateSub(candidate: CoachCandidate): String? {
    val pace = paceText(candidate.values["pace_sec"]?.toDouble()) ?: return null
    return when (candidate.id) {
        CoachIds.CAND_PACE_START_EASIER -> stringResource(R.string.coach_cand_pace_sub, pace)
        CoachIds.CAND_PACE_KEEP -> stringResource(R.string.coach_cand_keep_sub, pace)
        else -> null
    }
}

@StringRes
internal fun goalName(goal: CoachGoal): Int = when (goal) {
    CoachGoal.STEADY -> R.string.coach_goal_steady
    CoachGoal.FINISH_5K -> R.string.coach_goal_5k
    CoachGoal.IMPROVE -> R.string.coach_goal_improve
}

@StringRes
internal fun familyName(family: WorkoutFamily): Int = when (family) {
    WorkoutFamily.EASY -> R.string.coach_family_easy
    WorkoutFamily.WALK_RUN -> R.string.coach_family_walk_run
}

@StringRes
internal fun familySub(family: WorkoutFamily): Int = when (family) {
    WorkoutFamily.EASY -> R.string.coach_family_easy_sub
    WorkoutFamily.WALK_RUN -> R.string.coach_family_walk_run_sub
}

/** 단계의 짧은 이름(오늘 카드의 막대 아래) — 걷기 · 달리기 · 달리기 · 걷기 */
@StringRes
internal fun stepShort(role: StepRole): Int = when (role) {
    StepRole.WARMUP -> R.string.coach_step_warmup
    StepRole.EASY_RUN -> R.string.coach_step_run
    StepRole.RUN_WALK -> R.string.coach_step_runwalk
    StepRole.COOLDOWN -> R.string.coach_step_cooldown
}

@StringRes
internal fun stepTitle(role: StepRole): Int = when (role) {
    StepRole.WARMUP -> R.string.coach_step_warmup_title
    StepRole.EASY_RUN -> R.string.coach_step_easy_title
    StepRole.RUN_WALK -> R.string.coach_step_runwalk_title
    StepRole.COOLDOWN -> R.string.coach_step_cooldown_title
}

@Composable
internal fun stepSub(step: WorkoutStep): String = when (step.role) {
    StepRole.WARMUP -> stringResource(R.string.coach_step_warmup_sub)
    StepRole.EASY_RUN -> stringResource(R.string.coach_step_easy_sub)
    StepRole.RUN_WALK -> stringResource(R.string.coach_step_runwalk_sub, step.runMinutes, step.walkMinutes, step.rounds)
    StepRole.COOLDOWN -> stringResource(R.string.coach_step_cooldown_sub)
}

internal fun stepIcon(step: WorkoutStep): ImageVector =
    if (step.walking) Icons.AutoMirrored.Outlined.DirectionsWalk else Icons.AutoMirrored.Outlined.DirectionsRun

/** "10월 12일 – 18일" — 같은 달이면 끝은 날만 */
@Composable
internal fun weekRangeText(monday: LocalDate): String {
    val md = rememberFormatter(R.string.coach_date_md)
    val d = rememberFormatter(R.string.coach_date_d)
    val sunday = monday.plusDays(6)
    val end = if (sunday.month == monday.month) d.format(sunday) else md.format(sunday)
    return stringResource(R.string.coach_week_range, md.format(monday), end)
}

// ── 그림 ─────────────────────────────────────────────────────────

/** 훈련 막대 — 단계 시간에 비례한 칸(걷기는 옅은 파랑, 달리기는 진한 파랑). [labels] 면 칸 안에 "5분" */
@Composable
internal fun WorkoutBar(template: WorkoutTemplate, modifier: Modifier = Modifier, height: Dp = 22.dp, labels: Boolean = false) {
    val t = runTone()
    val walk = t.cobaltText.copy(alpha = 0.55f)
    Row(
        modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(10.dp)).clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        template.steps.forEach { step ->
            Box(
                Modifier.weight(step.minutes.toFloat()).fillMaxHeight().background(if (step.walking) walk else t.cobalt),
                contentAlignment = Alignment.Center,
            ) {
                if (labels) {
                    Text(
                        stringResource(R.string.coach_minutes, step.minutes),
                        style = runTextStyle(15.sp, Color.White, FontWeight.Bold, 1.1f), maxLines = 1,
                    )
                }
            }
        }
    }
}

/** 막대 아래 단계 이름 · 시간(오늘 카드) — 막대 칸과 같은 비율 */
@Composable
internal fun WorkoutBarLegend(template: WorkoutTemplate, modifier: Modifier = Modifier) {
    val t = runTone()
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        template.steps.forEach { step ->
            Column(Modifier.weight(step.minutes.toFloat()), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(stepShort(step.role)), style = runTextStyle(13.sp, t.label, FontWeight.Medium, 1.2f), maxLines = 1)
                Text(
                    stringResource(R.string.coach_minutes, step.minutes),
                    style = runTextStyle(16.sp, t.text, FontWeight.Bold, 1.2f), maxLines = 1,
                )
            }
        }
    }
}

/**
 * 1km 구간 페이스 그래프(03) — 빠른 쪽이 위. 점마다 그 구간 페이스, 점선은 이 러닝의 평균. 마지막 남은 거리는 넣지 않는다.
 * 점이 많으면(긴 러닝) 값 글자는 생략하고 눈금 · km 글자는 건너뛴다. 화면 낭독은 구간을 차례로 읽는다.
 */
@Composable
internal fun PaceChart(splits: List<KmSplit>, average: Double?, axis: PaceAxis, modifier: Modifier = Modifier) {
    val t = runTone()
    val measurer = rememberTextMeasurer()
    val averageLabel = stringResource(R.string.coach_avg_line)
    val description = splits.map { split ->
        stringResource(R.string.coach_split_cd, split.km, paceText(split.seconds.toDouble()).orEmpty())
    }.joinToString(", ")
    val tickStyle = runTextStyle(12.sp, t.label, FontWeight.Medium, 1.1f)
    val valueStyle = runTextStyle(13.sp, t.text, FontWeight.Bold, 1.1f)
    Canvas(
        modifier.fillMaxWidth().height(230.dp).semantics { contentDescription = description }.testTag("coach-pace-chart"),
    ) {
        val left = 48.dp.toPx()
        val right = size.width - 12.dp.toPx()
        val top = 22.dp.toPx()
        val bottom = size.height - 30.dp.toPx()
        fun y(sec: Double): Float = top + (bottom - top) * axis.fraction(sec)
        val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
        // 눈금(분'초") — 위가 빠르다
        axis.ticks.forEach { tick ->
            val at = y(tick.toDouble())
            drawLine(t.divider, Offset(left, at), Offset(right, at), strokeWidth = 1.dp.toPx(), pathEffect = dash)
            val label = measurer.measure(paceText(tick.toDouble()).orEmpty(), tickStyle)
            drawText(label, topLeft = Offset(left - label.size.width - 8.dp.toPx(), at - label.size.height / 2f))
        }
        drawLine(t.label.copy(alpha = 0.6f), Offset(left, top), Offset(left, bottom), strokeWidth = 1.dp.toPx())
        drawLine(t.label.copy(alpha = 0.6f), Offset(left, bottom), Offset(right, bottom), strokeWidth = 1.dp.toPx())
        if (splits.isEmpty()) return@Canvas
        val step = (right - left) / splits.size
        val xs = splits.indices.map { left + step * (it + 0.5f) }
        // 평균 — 굵은 점선과 오른쪽 끝 "평균"
        if (average != null && average.isFinite() && average > 0) {
            val at = y(average)
            drawLine(t.label, Offset(left, at), Offset(right, at), strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx())))
            val label = measurer.measure(averageLabel, tickStyle)
            drawText(label, topLeft = Offset(right - label.size.width, at - label.size.height - 3.dp.toPx()))
        }
        val points = splits.mapIndexed { index, split -> Offset(xs[index], y(split.seconds.toDouble())) }
        for (i in 1 until points.size) {
            drawLine(t.cobalt, points[i - 1], points[i], strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        }
        val showValues = splits.size <= 6
        val labelEvery = ((splits.size + 7) / 8).coerceAtLeast(1)
        points.forEachIndexed { index, point ->
            drawCircle(t.cobalt, radius = 6.dp.toPx(), center = point)
            drawCircle(t.text, radius = 2.5.dp.toPx(), center = point)
            if (showValues) {
                val value = measurer.measure(paceText(splits[index].seconds.toDouble()).orEmpty(), valueStyle)
                drawText(value, topLeft = Offset(point.x - value.size.width / 2f, point.y - value.size.height - 9.dp.toPx()))
            }
            if (index % labelEvery == 0 || index == points.lastIndex) {
                val km = measurer.measure("${splits[index].km}km", tickStyle)
                drawText(km, topLeft = Offset(point.x - km.size.width / 2f, bottom + 8.dp.toPx()))
            }
        }
    }
}

/**
 * 최근 4주 막대(04) — 0km 에서 시작하는 축. 고른 주는 진한 파랑, 그 앞은 옅은 파랑. 미래 주는 그리지 않는다(0 으로 보이지 않게).
 */
@Composable
internal fun WeekBars(weeks: List<WeekSummary>, top: Double, selected: LocalDate, modifier: Modifier = Modifier) {
    val t = runTone()
    val short = rememberFormatter(R.string.coach_date_short)
    val chart = 160.dp
    Row(modifier.fillMaxWidth().height(chart + 56.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        weeks.forEach { week ->
            val km = kmTextShort(week.meters)
            val label = short.format(week.monday)
            val on = week.monday == selected
            val cd = stringResource(R.string.coach_bar_cd, label, km)
            Column(
                Modifier.weight(1f).fillMaxHeight().clearAndSetSemantics { contentDescription = cd }.testTag("coach-week-bar-${week.monday}"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                val fraction = if (top > 0) (week.meters / 1000.0 / top).toFloat().coerceIn(0f, 1f) else 0f
                Text(km, style = runTextStyle(15.sp, t.text, FontWeight.Bold, 1.2f))
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier.fillMaxWidth().height(chart * fraction)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (on) t.cobalt else t.cobaltText.copy(alpha = 0.55f)),
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(t.label.copy(alpha = 0.5f)))
                Spacer(Modifier.height(8.dp))
                Text(label, style = runTextStyle(13.sp, t.label, FontWeight.Medium, 1.2f), textAlign = TextAlign.Center)
            }
        }
    }
}
