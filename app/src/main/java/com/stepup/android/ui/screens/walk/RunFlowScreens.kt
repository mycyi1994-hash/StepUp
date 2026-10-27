package com.stepup.android.ui.screens.walk

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Route
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.GoalAttempt
import com.stepup.android.domain.RunGoal
import com.stepup.android.ui.components.KitGap
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunCard
import com.stepup.android.ui.components.RunDivider
import com.stepup.android.ui.components.RunLinkRow
import com.stepup.android.ui.components.RunModeCard
import com.stepup.android.ui.components.RunNumber
import com.stepup.android.ui.components.RunPage
import com.stepup.android.ui.components.RunPillKind
import com.stepup.android.ui.components.RunStat
import com.stepup.android.ui.components.RunStatRow
import com.stepup.android.ui.components.RunStatusPill
import com.stepup.android.ui.components.SupPill
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

/**
 * 러닝 시작(시안 U01) — 홈의 "러닝 시작"과 첫 안내의 "러닝 시작"에서 연다. 자유 러닝 · 크루 달리기 · 러닝 챌린지 ·
 * 추천 코스 · 다이어트 모드 다섯 가지를 같은 크기의 가로 버튼으로, 아래에 내 러닝 기록 · AI 코칭(2026-10-16 사용자 결정 — 코칭의 입구) ·
 * 이번 주 순위(리더보드, 2026-10-17).
 *
 * 자유 러닝을 누르면 그 뜻을 한 번 기억하고 필요한 권한 안내를 이 화면 위에 차례로 보인다 — 홈을 보는 것만으로는
 * 묻지 않는다. 안내를 닫으면 아무것도 시작하지 않고 이 메뉴에 남는다. 준비가 끝나야 한 번만 [onFreeRun](3-2-1)으로 간다.
 */
@Composable
fun RunStartMenuScreen(
    onBack: () -> Unit,
    onFreeRun: () -> Unit,
    onGoals: () -> Unit,
    onDiet: (() -> Unit)?,
    /** 권한 안내의 "홈으로 돌아가기" */
    onHome: () -> Unit = onBack,
    onCrew: (() -> Unit)? = null,
    onCourse: (() -> Unit)? = null,
    onRecords: () -> Unit = {},
    onOpenWallet: (() -> Unit)? = null,
    /** AI 코칭(오늘 · 분석 · 변화) — 없으면 줄을 두지 않는다 */
    onCoaching: (() -> Unit)? = null,
    /** 리더보드(크루 · 개인 · 주간 포인트) — 없으면 줄을 두지 않는다 */
    onLeaderboard: (() -> Unit)? = null,
) {
    // 한 번 고른 자유 러닝 — 권한 안내를 지나는 동안(회전 · 설정 다녀오기 포함) 이 값 하나로 잇는다
    var freeRunPending by rememberSaveable { mutableStateOf(false) }
    val balance by ServiceLocator.rewardRepository.balance.collectAsState(initial = null)
    RunStartMenuContent(
        onBack = onBack,
        onFreeRun = { if (!freeRunPending) freeRunPending = true },
        onGoals = onGoals,
        onDiet = onDiet,
        onCrew = onCrew,
        onCourse = onCourse,
        onRecords = onRecords,
        balance = balance,
        onOpenWallet = onOpenWallet,
        onCoaching = onCoaching,
        onLeaderboard = onLeaderboard,
    )
    if (freeRunPending) {
        // 자유 러닝 카드에서만 오는 권한 안내 — 자유 러닝의 짧은 카드(2026-10-14 v2)
        RunPermissionFlow(
            minimal = true,
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

/** 러닝 시작 화면 — 상태 없이 그린다(시안 검사가 권한 안내를 이 위에 얹어 찍는다) */
@Composable
internal fun RunStartMenuContent(
    onBack: () -> Unit,
    onFreeRun: () -> Unit,
    onGoals: () -> Unit,
    onDiet: (() -> Unit)?,
    /** 크루 달리기(CR) · 추천 코스(U04) — 없으면(연결 전) 누를 수 없게 "곧" 표시 */
    onCrew: (() -> Unit)? = null,
    onCourse: (() -> Unit)? = null,
    onRecords: () -> Unit = {},
    balance: Double? = null,
    onOpenWallet: (() -> Unit)? = null,
    onCoaching: (() -> Unit)? = null,
    onLeaderboard: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val soon = stringResource(R.string.runflow_soon_toast)
    RunPage(
        onBack = onBack,
        modifier = Modifier.testTag("run-menu"),
        title = stringResource(R.string.runflow_menu_title),
        subtitle = stringResource(R.string.run_menu_sub),
        display = true,
        // SUP 잔액은 홈과 이 화면에만(시안)
        trailing = { SupPill(balance, onOpenWallet) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RunModeCard(
                stringResource(R.string.runflow_free), stringResource(R.string.run_mode_free_sub),
                Icons.AutoMirrored.Filled.DirectionsRun, onFreeRun, modifier = Modifier.testTag("run-menu-free"),
            )
            RunModeCard(
                stringResource(R.string.run_mode_crew), stringResource(R.string.run_mode_crew_sub),
                Icons.Filled.Groups, onCrew ?: { Toast.makeText(context, soon, Toast.LENGTH_SHORT).show() },
                enabled = onCrew != null, badge = if (onCrew == null) stringResource(R.string.runflow_soon) else null,
                modifier = Modifier.testTag("run-menu-crew"),
            )
            // 러닝 챌린지 · 추천 코스 · 다이어트 · 코칭은 아직 열지 않는다(2026-10-27 사용자 "크루 · 혼자 러닝 빼면 비활성화 ·
            // 준비 중 · 자물쇠") — 누르면 곧 열린다는 안내만. 다시 열려면 RunMenuLocks 의 값을 끈다
            val locked = { Toast.makeText(context, soon, Toast.LENGTH_SHORT).show() }
            val soonBadge = stringResource(R.string.runflow_soon)
            RunModeCard(
                stringResource(R.string.runflow_goal), stringResource(R.string.run_mode_challenge_sub),
                Icons.Filled.EmojiEvents, if (RunMenuLocks.CHALLENGE) locked else onGoals,
                enabled = !RunMenuLocks.CHALLENGE, locked = RunMenuLocks.CHALLENGE,
                badge = soonBadge.takeIf { RunMenuLocks.CHALLENGE }, modifier = Modifier.testTag("run-menu-goals"),
            )
            val courseOpen = onCourse != null && !RunMenuLocks.COURSE
            RunModeCard(
                stringResource(R.string.runflow_course), stringResource(R.string.run_mode_course_sub),
                Icons.Filled.Route, onCourse.takeIf { courseOpen } ?: locked,
                enabled = courseOpen, locked = !courseOpen, badge = soonBadge.takeIf { !courseOpen },
                modifier = Modifier.testTag("run-menu-course"),
            )
            val dietOpen = onDiet != null && !RunMenuLocks.DIET
            RunModeCard(
                stringResource(R.string.runflow_diet), stringResource(R.string.run_mode_diet_sub),
                ImageVector.vectorResource(R.drawable.ic_tab_shoes),
                onDiet.takeIf { dietOpen } ?: locked,
                enabled = dietOpen, locked = !dietOpen, badge = soonBadge.takeIf { !dietOpen },
                modifier = Modifier.testTag("run-menu-diet"),
            )
            Spacer(Modifier.height(4.dp))
            RunDivider()
            Spacer(Modifier.height(4.dp))
            RunLinkRow(stringResource(R.string.run_menu_records), onRecords, Modifier.testTag("run-menu-records"))
            if (onCoaching != null) {
                RunLinkRow(
                    stringResource(R.string.coach_menu_entry), if (RunMenuLocks.COACHING) locked else onCoaching,
                    Modifier.testTag("run-menu-coaching"), trailing = soonBadge.takeIf { RunMenuLocks.COACHING },
                    locked = RunMenuLocks.COACHING,
                )
            }
            if (onLeaderboard != null) {
                RunLinkRow(stringResource(R.string.lb_menu_entry), onLeaderboard, Modifier.testTag("run-menu-leaderboard"))
            }
        }
    }
}

/** 러닝 시작 메뉴에서 아직 열지 않은 곳(2026-10-27) — 혼자 러닝 · 크루만 연다. false 로 바꾸면 다시 열린다 */
internal object RunMenuLocks {
    const val CHALLENGE = true
    const val COURSE = true
    const val DIET = true
    const val COACHING = true
}

/** 러닝 챌린지(시안 U02) — 10분 · 1km · 3km 같은 크기의 카드. 카드를 누르면 그 도전이 바로 시작된다(안에 버튼을 또 두지 않는다) */
@Composable
fun RunGoalsScreen(
    onBack: () -> Unit,
    onStart: (RunGoal) -> Unit,
    onHistory: () -> Unit,
) {
    val t = runTone()
    RunPage(
        onBack = onBack,
        modifier = Modifier.testTag("run-goals"),
        title = stringResource(R.string.runflow_goals_title),
        subtitle = stringResource(R.string.run_challenge_sub),
        display = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RunGoal.entries.forEach { goal ->
                val (big, title, sub) = when (goal) {
                    RunGoal.TEN_MIN -> Triple(R.string.run_challenge_10_big, R.string.run_challenge_10_title, R.string.run_challenge_10_sub)
                    RunGoal.ONE_KM -> Triple(R.string.run_challenge_1k_big, R.string.run_challenge_1k_title, R.string.run_challenge_1k_sub)
                    RunGoal.THREE_KM -> Triple(R.string.run_challenge_3k_big, R.string.run_challenge_3k_title, R.string.run_challenge_3k_sub)
                }
                val name = goalName(goal)
                RunCard(
                    onClick = { onStart(goal) },
                    padding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
                    tag = "run-goal-${goal.key}",
                    modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = name },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            RunNumber(stringResource(big), size = 54.sp)
                            Text(stringResource(title), style = runTextStyle(21.sp, t.text, FontWeight.ExtraBold))
                            Text(stringResource(sub), style = runTextStyle(14.sp, t.label, FontWeight.Medium))
                        }
                        Icon(Icons.AutoMirrored.Filled.DirectionsRun, null, tint = if (t.dark) androidx.compose.ui.graphics.Color(0xFFCFE0FF) else t.cobalt, modifier = Modifier.size(60.dp))
                        Icon(Icons.Filled.ChevronRight, null, tint = t.label, modifier = Modifier.size(28.dp))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            RunDivider()
            Spacer(Modifier.height(4.dp))
            RunLinkRow(stringResource(R.string.runflow_goals_history), onHistory, Modifier.testTag("run-goals-history"))
        }
    }
}

@Composable
fun goalName(goal: RunGoal): String = stringResource(when (goal) {
    RunGoal.TEN_MIN -> R.string.goal_10min
    RunGoal.ONE_KM -> R.string.goal_1km
    RunGoal.THREE_KM -> R.string.goal_3km
})

/** 지난 도전(시안 C03) — 저장한 챌린지 러닝. 줄을 누르면 그 러닝의 기록 상세(없으면 펼쳐서 페이스 · 목표) */
@Composable
fun RunGoalHistoryScreen(
    onBack: () -> Unit,
    onNewChallenge: () -> Unit = onBack,
    onOpenRecord: (Long) -> Unit = {},
) {
    val attempts by ServiceLocator.userPrefs.goalAttempts.collectAsState(initial = null)
    var open by rememberSaveable { mutableStateOf<Long?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val t = runTone()
    RunPage(
        onBack = onBack,
        modifier = Modifier.testTag("run-goal-history"),
        title = stringResource(R.string.runflow_history_title),
        bottom = {
            RunButton(stringResource(R.string.run_challenge_new), onNewChallenge, modifier = Modifier.testTag("run-goal-history-new"))
        },
    ) {
        val rows = attempts
        if (rows != null) {
            Text(
                stringResource(R.string.run_challenge_history_count, rows.count { it.achieved }),
                style = runTextStyle(17.sp, t.label, FontWeight.SemiBold),
                modifier = Modifier.testTag("run-goal-history-count"),
            )
            Spacer(Modifier.height(16.dp))
        }
        if (rows != null && rows.isEmpty()) {
            Text(stringResource(R.string.runflow_history_empty), style = runTextStyle(16.sp, t.label),
                modifier = Modifier.testTag("run-goal-history-empty"))
        }
        // 기록 찾는 중에 한 번 더 눌러도 기록 화면을 두 번 쌓지 않는다
        var finding by remember { mutableStateOf(false) }
        Column(verticalArrangement = Arrangement.spacedBy(KitGap)) {
            rows.orEmpty().forEach { attempt ->
                GoalAttemptRow(attempt, expanded = open == attempt.startedAt, onOpen = {
                    if (finding) return@GoalAttemptRow
                    finding = true
                    scope.launch {
                        val id = ServiceLocator.runRecordsRepository.idForStart(attempt.startedAt)
                        // 찾기는 다른 스레드에서 끝날 수 있다 — 화면 이동은 메인에서
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main.immediate) {
                            finding = false
                            if (id != null) onOpenRecord(id)
                            else open = if (open == attempt.startedAt) null else attempt.startedAt
                        }
                    }
                })
            }
        }
    }
}

@Composable
private fun GoalAttemptRow(attempt: GoalAttempt, expanded: Boolean, onOpen: () -> Unit) {
    val t = runTone()
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val day = Instant.ofEpochMilli(attempt.startedAt).atZone(ZoneId.systemDefault()).toLocalDate()
    val dayLabel = day.format(DateTimeFormatter.ofPattern(stringResource(R.string.date_month_day), locale))
    RunCard(onClick = onOpen, padding = androidx.compose.foundation.layout.PaddingValues(16.dp), tag = "run-goal-row-${attempt.startedAt}") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(t.inset).border(1.dp, t.panelEdge, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.DirectionsRun, null, tint = t.cyan, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(goalName(attempt.goal), style = runTextStyle(19.sp, t.text, FontWeight.ExtraBold))
                Text(
                    dayLabel + "  |  " + "%.2f km".format(attempt.distanceKm),
                    style = runTextStyle(14.sp, t.label, FontWeight.Medium),
                )
            }
            RunStatusPill(
                stringResource(if (attempt.achieved) R.string.run_challenge_done else R.string.run_challenge_tried),
                if (attempt.achieved) RunPillKind.Live else RunPillKind.Neutral,
                icon = if (attempt.achieved) Icons.Filled.CheckCircle else null,
            )
            Icon(Icons.Filled.ChevronRight, null, tint = t.label, modifier = Modifier.size(24.dp))
        }
        if (expanded) {
            Spacer(Modifier.height(12.dp))
            // 50m 이상일 때만 — 러닝 중 · 결과와 같은 기준(RunPace). 몇 m 로 나누면 1분 도전이 수십 분/km 로 보였다
            val pace = com.stepup.android.domain.RunPace.secPerKm(attempt.elapsedSec, attempt.distanceKm * 1000)
                ?.let { formatPace(it.toLong()) } ?: "—"
            RunStatRow(
                listOf(
                    RunStat(stringResource(R.string.runflow_time_label), formatDuration(attempt.elapsedSec)),
                    RunStat(stringResource(R.string.runflow_pace_label), pace),
                ),
                valueSize = 24.sp,
            )
        }
    }
}
