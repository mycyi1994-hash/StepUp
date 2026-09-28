package com.stepup.android.ui.screens.walk

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.GoalAttempt
import com.stepup.android.domain.RunGoal
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
import com.stepup.android.ui.theme.VoltText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * 러닝 방법 고르기(시안 U01 · 시작·로그인·첫 사용 v1 시안 12) — 홈의 "러닝 시작"과 첫 안내의 "러닝 시작"에서 연다.
 * 자유 러닝 · 러닝 챌린지 · 추천 코스(준비 중) · 다이어트 모드. 10분 러닝 · 가입 입력 · 크루 · 지갑 단계는 없다.
 *
 * 자유 러닝을 누르면 그 뜻을 한 번 기억하고 필요한 권한 안내(13~19)를 이 화면 위에 차례로 보인다 — 홈을 보는 것만으로는
 * 묻지 않는다. 안내를 닫으면 아무것도 시작하지 않고 이 메뉴에 남는다. 준비가 끝나야 한 번만 [onFreeRun](3-2-1)으로 간다.
 */
@Composable
fun RunStartMenuScreen(
    onBack: () -> Unit,
    onFreeRun: () -> Unit,
    onGoals: () -> Unit,
    onDiet: (() -> Unit)?,
    /** 권한 안내의 "홈으로 돌아가기"(시안 14 · 15) */
    onHome: () -> Unit = onBack,
) {
    // 한 번 고른 자유 러닝 — 권한 안내를 지나는 동안(회전 · 설정 다녀오기 포함) 이 값 하나로 잇는다
    var freeRunPending by rememberSaveable { mutableStateOf(false) }
    RunStartMenuContent(
        onBack = onBack,
        onFreeRun = { if (!freeRunPending) freeRunPending = true },
        onGoals = onGoals,
        onDiet = onDiet,
    )
    if (freeRunPending) {
        RunPermissionFlow(
            onReady = {
                freeRunPending = false
                onFreeRun()
            },
            onCancel = { freeRunPending = false },
            onHome = {
                freeRunPending = false
                onHome()
            },
        )
    }
}

/** 러닝 방법 고르기 화면 — 상태 없이 그린다(시안 검사가 권한 안내 시트를 이 위에 얹어 찍는다) */
@Composable
internal fun RunStartMenuContent(
    onBack: () -> Unit,
    onFreeRun: () -> Unit,
    onGoals: () -> Unit,
    onDiet: (() -> Unit)?,
) {
    val context = LocalContext.current
    val soon = stringResource(R.string.runflow_soon_toast)
    val p = com.stepup.android.ui.components.settingsPalette()
    Box(Modifier.fillMaxSize().testTag("run-menu")) {
        com.stepup.android.ui.components.S2Stage(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            com.stepup.android.ui.components.SecondaryHeader(
                onBack = onBack, balance = null, onOpenWallet = null,
                title = stringResource(R.string.runflow_menu_title),
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = RunKit.Gutter).padding(bottom = 24.dp),
            ) {
                Spacer(Modifier.height(28.dp))
                Text(
                    stringResource(R.string.onb_menu_headline), color = p.text, fontSize = 29.sp,
                    fontWeight = FontWeight.SemiBold, lineHeight = 38.sp, letterSpacing = (-0.6).sp,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.onb_menu_subtitle), color = p.secondary, fontSize = 15.sp, lineHeight = 22.sp)
                Spacer(Modifier.height(36.dp))
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    RunMenuCard(
                        stringResource(R.string.runflow_free), stringResource(R.string.runflow_free_desc),
                        Icons.AutoMirrored.Outlined.DirectionsRun, onFreeRun, primary = true,
                        modifier = Modifier.testTag("run-menu-free"),
                    )
                    RunMenuCard(
                        stringResource(R.string.runflow_goal), stringResource(R.string.onb_menu_goal_desc),
                        Icons.Outlined.BarChart, onGoals, modifier = Modifier.testTag("run-menu-goals"),
                    )
                    // 추천 코스는 걸을 수 있는 경로 데이터가 정해지면 연다(2026-09-27 사용자 결정)
                    RunMenuCard(
                        stringResource(R.string.runflow_course), stringResource(R.string.onb_menu_course_desc),
                        Icons.Outlined.LocationOn, { Toast.makeText(context, soon, Toast.LENGTH_SHORT).show() },
                        badge = stringResource(R.string.runflow_soon), enabled = false,
                        modifier = Modifier.testTag("run-menu-course"),
                    )
                    RunMenuCard(
                        stringResource(R.string.runflow_diet), stringResource(R.string.runflow_diet_desc),
                        Icons.Outlined.AccountCircle,
                        onDiet ?: { Toast.makeText(context, soon, Toast.LENGTH_SHORT).show() },
                        badge = if (onDiet == null) stringResource(R.string.runflow_soon) else null,
                        enabled = onDiet != null,
                        modifier = Modifier.testTag("run-menu-diet"),
                    )
                }
            }
        }
    }
}

/** 러닝 방법 카드(시안 12) — 이름 · 한 줄 · 오른쪽 그림. 자유 러닝 한 장만 흰 면(어두운 테마) */
@Composable
private fun RunMenuCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    badge: String? = null,
) {
    val p = com.stepup.android.ui.components.settingsPalette()
    val shape = RoundedCornerShape(18.dp)
    val ink = if (primary) p.primaryText else p.text
    val muted = if (primary) p.primaryText.copy(alpha = 0.72f) else p.secondary
    Row(
        modifier.fillMaxWidth().heightIn(min = 93.dp).clip(shape)
            .then(if (primary) Modifier.background(p.primaryFace, shape) else Modifier.background(p.surface, shape))
            .feedbackClickable(enabled = enabled, onClick = onClick)
            .padding(start = 20.dp, end = 22.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, color = if (enabled) ink else ink.copy(alpha = 0.5f), fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold, lineHeight = 28.sp)
                if (badge != null) {
                    Text(
                        badge, color = p.secondary, fontSize = 12.sp,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(p.divider)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Text(description, color = if (enabled) muted else muted.copy(alpha = 0.5f), fontSize = 14.sp, lineHeight = 20.sp)
        }
        Icon(
            icon, contentDescription = null,
            tint = if (primary) p.primaryText else p.accent.copy(alpha = if (enabled) 1f else 0.4f),
            modifier = Modifier.size(28.dp),
        )
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


