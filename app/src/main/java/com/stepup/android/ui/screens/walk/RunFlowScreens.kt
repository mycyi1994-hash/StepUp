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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.ui.semantics.text
import androidx.compose.ui.draw.alpha
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
import com.stepup.android.ui.experience.feedbackClickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ColumnScope
import com.stepup.android.ui.components.stepUpRim
import com.stepup.android.ui.components.StepUpUi
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
    // 13차 — 내 러닝 기록 줄에 마지막 러닝(날짜 · 거리), 이번 주 순위 줄에 우리 크루 순위(서버 — 못 읽으면 비움)
    val latest by remember { ServiceLocator.stepRepository.observeLatestSession() }.collectAsState(initial = null)
    val datePattern = stringResource(R.string.share_run_date_pattern)
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val lastRun = latest?.takeIf { it.distanceMeters > 0 }?.let { run ->
        val date = runCatching { java.text.SimpleDateFormat(datePattern, locale).format(java.util.Date(run.startedAt)) }.getOrDefault("")
        stringResource(R.string.v13_run_last, date, "%.2f".format(java.util.Locale.ROOT, run.distanceMeters / 1000.0))
    }
    var crewRank by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(onLeaderboard != null) {
        if (onLeaderboard == null) return@LaunchedEffect
        val load = runCatching { ServiceLocator.leaderboards.crews(3) }.getOrNull()
        crewRank = (load as? com.stepup.android.domain.LeaderLoad.Ready)?.value?.mine?.rank
    }
    RunStartMenuContent(
        lastRun = lastRun,
        crewRank = crewRank?.let { stringResource(R.string.v13_run_crew_rank, it) },
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
    /** 내 러닝 기록 줄의 값 — 마지막 러닝("11월 24일 5.02km"). 없으면 비움 */
    lastRun: String? = null,
    /** 이번 주 순위 줄의 값 — "우리 크루 7위". 크루가 없거나 순위가 없으면 비움 */
    crewRank: String? = null,
) {
    val context = LocalContext.current
    val soon = stringResource(R.string.runflow_soon_toast)
    val locked = { Toast.makeText(context, soon, Toast.LENGTH_SHORT).show() }
    // 13차(2026-11-26 S-361) — 처음 빌드처럼 아이콘 카드: 위 두 칸 큰 카드(자유 러닝 블루 면 · 크루 달리기), 가운데 두 줄(아이콘 + 값 — 내 러닝 기록 ·
    // 이번 주 순위), 아래 "준비 중" 2 × 2 작은 칸(자물쇠 · 흐리게, 누르면 곧 열린다는 안내 — RunMenuLocks 2026-10-27 결정 그대로).
    // 부제 · 둘째 줄 설명 없음, 아래 탭바는 보인다(AppChromePolicy — RUN_MENU 는 Header.Detail)
    RunPage(
        onBack = onBack,
        modifier = Modifier.testTag("run-menu"),
        crumb = stringResource(R.string.runflow_menu_title),
        trailing = { SupPill(balance, onOpenWallet) },
    ) {
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MenuBigCard(
                stringResource(R.string.runflow_free), stringResource(R.string.v13_run_free_sub), Icons.AutoMirrored.Filled.DirectionsRun,
                primary = true, onClick = onFreeRun, tag = "run-menu-free", modifier = Modifier.weight(1f),
            )
            MenuBigCard(
                stringResource(R.string.run_mode_crew), stringResource(R.string.v13_run_crew_sub), Icons.Outlined.Groups,
                primary = false, onClick = onCrew ?: locked, tag = "run-menu-crew", modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        MenuGroup {
            MenuRow(stringResource(R.string.run_menu_records), onRecords, "run-menu-records", icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
                value = lastRun)
            if (onLeaderboard != null) {
                MenuRow(stringResource(R.string.lb_menu_entry), onLeaderboard, "run-menu-leaderboard", icon = Icons.Outlined.EmojiEvents,
                    value = crewRank)
            }
        }
        Spacer(Modifier.height(26.dp))
        Text(stringResource(R.string.runflow_soon), style = StepUpUi.type.rowTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
            color = StepUpUi.colors.label, modifier = Modifier.padding(start = 2.dp, bottom = 10.dp))
        val courseOpen = onCourse != null && !RunMenuLocks.COURSE
        val dietOpen = onDiet != null && !RunMenuLocks.DIET
        val soonCells = buildList {
            add(SoonCell(stringResource(R.string.runflow_goal), Icons.Outlined.Flag, "run-menu-goals", if (RunMenuLocks.CHALLENGE) null else onGoals))
            add(SoonCell(stringResource(R.string.runflow_course), Icons.Outlined.Route, "run-menu-course", onCourse.takeIf { courseOpen }))
            add(SoonCell(stringResource(R.string.runflow_diet), Icons.Outlined.TrackChanges, "run-menu-diet", onDiet.takeIf { dietOpen }))
            if (onCoaching != null) {
                add(SoonCell(stringResource(R.string.coach_menu_entry), Icons.Outlined.BarChart, "run-menu-coaching",
                    if (RunMenuLocks.COACHING) null else onCoaching))
            }
        }
        soonCells.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { cell -> MenuSmallCell(cell, locked, Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(12.dp))
    }
}

private data class SoonCell(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val tag: String, val open: (() -> Unit)?)

/** 위 큰 카드(S-361) — 아이콘 칸 · 제목 · 한 줄. 자유 러닝은 블루 면, 크루는 카드 면 */
@Composable
private fun MenuBigCard(
    title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector, primary: Boolean,
    onClick: () -> Unit, tag: String, modifier: Modifier = Modifier,
) {
    val c = StepUpUi.colors
    val shape = RoundedCornerShape(20.dp)
    val ink = if (primary) androidx.compose.ui.graphics.Color.White else c.label
    Column(
        modifier.heightIn(min = 156.dp).clip(shape).background(if (primary) c.primary else c.card)
            .stepUpRim(shape, if (primary) com.stepup.android.ui.components.RimKind.Primary else com.stepup.android.ui.components.RimKind.Surface)
            .feedbackClickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
            .padding(18.dp).testTag(tag),
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                .background(if (primary) androidx.compose.ui.graphics.Color.White.copy(alpha = .22f) else c.primary.copy(alpha = .16f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = if (primary) androidx.compose.ui.graphics.Color.White else c.accentText, modifier = Modifier.size(26.dp)) }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(18.dp))
        Text(title, color = ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        Text(sub, color = ink.copy(alpha = .8f), fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/** "준비 중" 작은 칸 — 아이콘 · 이름 · 자물쇠, 흐리게. 열린 것은 꺾쇠 없이 그대로 누른다 */
@Composable
private fun MenuSmallCell(cell: SoonCell, locked: () -> Unit, modifier: Modifier = Modifier) {
    val c = StepUpUi.colors
    val shape = RoundedCornerShape(16.dp)
    val open = cell.open != null
    Row(
        modifier.heightIn(min = 64.dp).clip(shape).background(c.card)
            .stepUpRim(shape, com.stepup.android.ui.components.RimKind.Surface)
            .feedbackClickable(role = androidx.compose.ui.semantics.Role.Button, onClick = cell.open ?: locked)
            .alpha(if (open) 1f else .62f).padding(horizontal = 16.dp).testTag(cell.tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(cell.icon, null, tint = c.label, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(cell.title, color = c.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (!open) {
            // 화면 읽기 · 검사가 "준비 중"을 읽는다 — 눈에는 자물쇠
            val soonLabel = stringResource(R.string.runflow_soon)
            Icon(
                Icons.Outlined.Lock, contentDescription = null, tint = c.secondaryLabel,
                modifier = Modifier.size(18.dp).semantics { text = androidx.compose.ui.text.AnnotatedString(soonLabel) },
            )
        }
    }
}

/** 흰 묶음(v12) — 반지름 18, 줄 사이 가는 선 */
@Composable
private fun MenuGroup(content: @Composable ColumnScope.() -> Unit) {
    val c = StepUpUi.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.card)
            .stepUpRim(RoundedCornerShape(18.dp), com.stepup.android.ui.components.RimKind.Surface),
        content = content,
    )
}

/** 목록 한 줄 — 16 · 600 글 · 오른쪽 꺾쇠(잠긴 줄은 꺾쇠 없이) */
@Composable
private fun MenuRow(
    title: String, onClick: () -> Unit, tag: String, chevron: Boolean = true,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null, value: String? = null,
) {
    val c = StepUpUi.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 58.dp).feedbackClickable(onClick = onClick).padding(horizontal = 18.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = c.accentText, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(title, style = StepUpUi.type.rowTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
            color = if (chevron) c.label else c.label.copy(alpha = .75f), modifier = Modifier.weight(1f))
        if (value != null) {
            Text(value, color = c.secondaryLabel, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1,
                modifier = Modifier.padding(start = 8.dp).testTag("$tag-value"))
        }
        if (chevron) {
            Icon(Icons.Filled.ChevronRight, null, tint = c.tertiaryLabel, modifier = Modifier.size(22.dp))
        } else {
            // 잠긴 줄 — 줄마다 "준비 중"(화면 읽기가 줄만 읽어도 알게, 2026-10-27 자물쇠 결정)
            Text(stringResource(R.string.runflow_soon), style = StepUpUi.type.caption, color = c.tertiaryLabel)
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
