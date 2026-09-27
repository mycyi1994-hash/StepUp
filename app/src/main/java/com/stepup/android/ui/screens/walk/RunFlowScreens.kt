package com.stepup.android.ui.screens.walk

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.GoalAttempt
import com.stepup.android.domain.RunGoal
import com.stepup.android.ui.components.KitActionCard
import com.stepup.android.ui.components.KitButton
import com.stepup.android.ui.components.KitGap
import com.stepup.android.ui.components.KitGoalCard
import com.stepup.android.ui.components.KitMetricRow
import com.stepup.android.ui.components.KitScreen
import com.stepup.android.ui.components.KitTone
import com.stepup.android.ui.components.RunKit
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.Carbon
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.VoltText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * 시작 메뉴(시안 U01) — 홈의 "러닝 시작"에서 연다.
 * 자유 러닝 · 러닝 챌린지 · 추천 코스(준비 중) · 다이어트 모드.
 */
@Composable
fun RunStartMenuScreen(
    onBack: () -> Unit,
    onFreeRun: () -> Unit,
    onGoals: () -> Unit,
    onDiet: (() -> Unit)?,
) {
    val context = LocalContext.current
    val soon = stringResource(R.string.runflow_soon_toast)
    KitScreen(
        title = stringResource(R.string.runflow_menu_title),
        onBack = onBack,
        modifier = Modifier.testTag("run-menu"),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.runflow_menu_kicker), color = VoltText, fontSize = 14.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.runflow_menu_headline), color = Snow, fontSize = 28.sp, lineHeight = 38.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(32.dp))
        Column(verticalArrangement = Arrangement.spacedBy(KitGap)) {
            KitActionCard(
                stringResource(R.string.runflow_free), stringResource(R.string.runflow_free_desc),
                Icons.AutoMirrored.Filled.DirectionsRun, onFreeRun, primary = true,
                modifier = Modifier.testTag("run-menu-free"),
            )
            KitActionCard(
                stringResource(R.string.runflow_goal), stringResource(R.string.runflow_goal_desc),
                Icons.Filled.Flag, onGoals, modifier = Modifier.testTag("run-menu-goals"),
            )
            // 추천 코스는 걸을 수 있는 경로 데이터가 정해지면 연다(2026-09-27 사용자 결정)
            KitActionCard(
                stringResource(R.string.runflow_course), stringResource(R.string.runflow_course_desc),
                Icons.Filled.LocationOn, { Toast.makeText(context, soon, Toast.LENGTH_SHORT).show() },
                badge = stringResource(R.string.runflow_soon), enabled = false,
                modifier = Modifier.testTag("run-menu-course"),
            )
            KitActionCard(
                stringResource(R.string.runflow_diet), stringResource(R.string.runflow_diet_desc),
                Icons.AutoMirrored.Filled.DirectionsWalk,
                onDiet ?: { Toast.makeText(context, soon, Toast.LENGTH_SHORT).show() },
                badge = if (onDiet == null) stringResource(R.string.runflow_soon) else null,
                enabled = onDiet != null,
                modifier = Modifier.testTag("run-menu-diet"),
            )
        }
    }
}

/** 러닝 챌린지 목록(시안 U02) — 10분 · 1km · 3km, 아래에 지난 도전 */
@Composable
fun RunGoalsScreen(
    onBack: () -> Unit,
    onStart: (RunGoal) -> Unit,
    onHistory: () -> Unit,
) {
    KitScreen(
        title = stringResource(R.string.runflow_goals_title),
        onBack = onBack,
        headline = stringResource(R.string.runflow_goals_headline),
        subtitle = stringResource(R.string.runflow_goals_sub),
        modifier = Modifier.testTag("run-goals"),
        bottom = {
            KitButton(stringResource(R.string.runflow_goals_history), onHistory, tone = KitTone.Ghost,
                modifier = Modifier.testTag("run-goals-history"))
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(KitGap)) {
            RunGoal.entries.forEach { goal ->
                KitGoalCard(
                    title = goalName(goal),
                    description = stringResource(when (goal) {
                        RunGoal.TEN_MIN -> R.string.goal_10min_desc
                        RunGoal.ONE_KM -> R.string.goal_1km_desc
                        RunGoal.THREE_KM -> R.string.goal_3km_desc
                    }),
                    actionLabel = stringResource(R.string.goal_start, goalName(goal)),
                    onAction = { onStart(goal) },
                    modifier = Modifier.testTag("run-goal-${goal.key}"),
                )
            }
        }
    }
}

@Composable
fun goalName(goal: RunGoal): String = stringResource(when (goal) {
    RunGoal.TEN_MIN -> R.string.goal_10min
    RunGoal.ONE_KM -> R.string.goal_1km
    RunGoal.THREE_KM -> R.string.goal_3km
})

/** 지난 도전(시안 C03) — 저장한 챌린지 러닝. 누르면 평균 페이스 · 목표를 펼친다 */
@Composable
fun RunGoalHistoryScreen(onBack: () -> Unit) {
    val attempts by ServiceLocator.userPrefs.goalAttempts.collectAsState(initial = null)
    var open by rememberSaveable { mutableStateOf<Long?>(null) }
    KitScreen(
        title = stringResource(R.string.runflow_history_title),
        onBack = onBack,
        headline = stringResource(R.string.runflow_history_headline),
        subtitle = stringResource(R.string.runflow_history_sub),
        modifier = Modifier.testTag("run-goal-history"),
        bottom = {
            KitButton(stringResource(R.string.runflow_history_back), onBack)
        },
    ) {
        val rows = attempts
        if (rows != null && rows.isEmpty()) {
            Text(stringResource(R.string.runflow_history_empty), color = Silver, fontSize = 16.sp,
                modifier = Modifier.testTag("run-goal-history-empty"))
        }
        Column(verticalArrangement = Arrangement.spacedBy(KitGap)) {
            rows.orEmpty().forEach { attempt ->
                GoalAttemptRow(attempt, expanded = open == attempt.startedAt, onToggle = {
                    open = if (open == attempt.startedAt) null else attempt.startedAt
                })
            }
        }
    }
}

@Composable
private fun GoalAttemptRow(attempt: GoalAttempt, expanded: Boolean, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(RunKit.CardRadius)
    val today = remember { LocalDate.now() }
    val day = Instant.ofEpochMilli(attempt.startedAt).atZone(ZoneId.systemDefault()).toLocalDate()
    val dayLabel = when (day) {
        today -> stringResource(R.string.runflow_today)
        today.minusDays(1) -> stringResource(R.string.runflow_yesterday)
        else -> day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Carbon, shape)
            .feedbackClickable(onClick = onToggle).animateContentSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            stringResource(if (attempt.achieved) R.string.runflow_history_row_done else R.string.runflow_history_row_tried,
                dayLabel, goalName(attempt.goal)),
            color = VoltText, fontSize = 16.sp,
        )
        Text(
            formatDuration(attempt.elapsedSec) + " · " + "%.2f km".format(attempt.distanceKm),
            color = Silver, fontSize = 14.sp,
        )
        if (expanded) {
            Spacer(Modifier.height(12.dp))
            val pace = if (attempt.distanceKm >= 0.01 && attempt.elapsedSec > 0) {
                formatPace((attempt.elapsedSec / attempt.distanceKm).toLong())
            } else "—"
            KitMetricRow(
                stringResource(R.string.runflow_pace_label) to pace,
                stringResource(R.string.runflow_history_goal) to goalName(attempt.goal),
            )
        }
    }
}


