package com.stepup.android.ui.screens.coaching

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.data.local.RunRecordRow
import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.averagePace
import com.stepup.android.domain.coaching.CoachAi
import com.stepup.android.domain.coaching.CoachResult
import com.stepup.android.domain.coaching.RunSplits
import com.stepup.android.domain.coaching.SplitGap
import com.stepup.android.domain.coaching.WeekDay
import com.stepup.android.domain.coaching.WeekState
import com.stepup.android.domain.coaching.clockText
import com.stepup.android.domain.coaching.kmAxisTop
import com.stepup.android.domain.coaching.kmText
import com.stepup.android.domain.coaching.kmTextShort
import com.stepup.android.domain.coaching.paceAxis
import com.stepup.android.domain.coaching.paceText
import com.stepup.android.domain.coaching.runSplits
import com.stepup.android.domain.localDate
import com.stepup.android.ui.components.LiveRouteMap
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunButtonKind
import com.stepup.android.ui.components.RunCard
import com.stepup.android.ui.components.RunChoiceRow
import com.stepup.android.ui.components.RunDivider
import com.stepup.android.ui.components.RunMapFrame
import com.stepup.android.ui.components.RunNotice
import com.stepup.android.ui.components.RunNoticeKind
import com.stepup.android.ui.components.RunNumber
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.RunSheetText
import com.stepup.android.ui.components.RunSpec
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.RunTabs
import com.stepup.android.ui.components.RunTextAction
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.records.rememberFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** 코칭의 세 탭 — 오늘(01 · 08) · 분석(03 · 07 · 09–12) · 변화(04) */
enum class CoachTab(val key: String) {
    TODAY("today"),
    ANALYSIS("analysis"),
    CHANGE("change"),
    ;

    companion object {
        fun of(key: String?): CoachTab = entries.firstOrNull { it.key == key } ?: TODAY
    }
}

/**
 * 코칭 화면에서 나가는 길. [onGoRun] 은 기존 러닝 시작 흐름(시작 메뉴 — 달리던 러닝이 있으면 그 러닝)이고, 코칭은 러닝을
 * 스스로 시작하지 않는다. [onOpenRecord] 는 기존 지난 러닝 상세(기본 기록).
 */
class CoachingActions(
    val onBack: () -> Unit,
    val onSettings: () -> Unit,
    val onConsent: () -> Unit,
    val onGoals: () -> Unit,
    val onWorkout: (String) -> Unit,
    val onPlan: () -> Unit,
    val onGoRun: () -> Unit,
    val onOpenRecord: (Long) -> Unit,
)

/**
 * AI 코칭(2026-10-16 인계) — 머리 "코칭 AI" · 설정 톱니 · 오늘 / 분석 / 변화 탭. 숫자 · 그래프 · 주간 띠는 이 휴대폰의
 * 기록으로 먼저 그리고, AI 칸만 서버 답을 기다린다. 모달(기록 고르기 · 목표 페이스 적용 16)은 하단 탭까지 덮는다.
 */
@Composable
fun CoachingHomeScreen(viewModel: CoachingHubViewModel, startTab: CoachTab, actions: CoachingActions) {
    val locale = stringResource(R.string.coach_locale)
    LaunchedEffect(locale) { viewModel.setLocale(locale) }
    // 동의 · 목표 · 설정에서 돌아오면 서버 상태를 다시 읽는다(처음 들어올 때는 이미 읽는 중이다)
    var resumedOnce by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        if (resumedOnce) viewModel.refreshAccount() else resumedOnce = true
        onPauseOrDispose { }
    }
    var tab by rememberSaveable { mutableIntStateOf(startTab.ordinal) }
    val account by viewModel.account.collectAsStateWithLifecycle()
    CoachFrame(
        onBack = actions.onBack,
        trailing = {
            CoachIconButton(Icons.Outlined.Settings, stringResource(R.string.coach_settings_cd), actions.onSettings, "coach-settings")
        },
    ) {
        CoachTitle(
            stringResource(R.string.coach_title), Modifier.padding(horizontal = RunSpec.Gutter),
            badge = stringResource(R.string.coach_ai_badge),
        )
        Spacer(Modifier.height(10.dp))
        RunTabs(
            labels = listOf(
                stringResource(R.string.coach_tab_today), stringResource(R.string.coach_tab_analysis),
                stringResource(R.string.coach_tab_change),
            ),
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier.padding(horizontal = RunSpec.Gutter),
            tagPrefix = "coach-tab",
        )
        when (CoachTab.entries[tab]) {
            CoachTab.TODAY -> TodayTab(viewModel, account, actions, onOpenRun = { id ->
                viewModel.pick(id)
                tab = CoachTab.ANALYSIS.ordinal
            })
            CoachTab.ANALYSIS -> AnalysisTab(viewModel, account, actions)
            CoachTab.CHANGE -> ChangeTab(viewModel, account, actions)
        }
    }
}

// ── 오늘(01 · 08) ────────────────────────────────────────────────────

@Composable
private fun TodayTab(viewModel: CoachingHubViewModel, account: CoachAccount, actions: CoachingActions, onOpenRun: (Long) -> Unit) {
    val today by viewModel.today.collectAsStateWithLifecycle()
    val rec by viewModel.recommendation.collectAsStateWithLifecycle()
    when (val load = today) {
        Loaded.Loading -> CoachLoading()
        Loaded.Failed -> CoachLoadFailed()
        is Loaded.Ready -> {
            val latest = load.value.latest
            if (latest == null) {
                NoRecords(actions)
            } else {
                TodayContent(viewModel, load.value.days, latest, account, shownAi(rec, account.snapshot), actions, onOpenRun)
            }
        }
    }
}

@Composable
private fun TodayContent(
    viewModel: CoachingHubViewModel,
    days: List<WeekDay>,
    latest: RunRecordRow,
    account: CoachAccount,
    rec: CoachAi,
    actions: CoachingActions,
    onOpenRun: (Long) -> Unit,
) {
    val t = runTone()
    val snapshot = account.snapshot
    val goal = snapshot?.chosenPrefs()?.runsPerWeek
    val runs = days.sumOf { it.runs }
    val workout = todayWorkout(snapshot, viewModel.todayDate(), rec)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = RunSpec.Gutter)) {
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(weekRangeText(days.first().date), style = runTextStyle(16.sp, t.label, FontWeight.Medium), modifier = Modifier.weight(1f))
            Text(
                if (goal != null) stringResource(R.string.coach_week_progress, runs, goal)
                else stringResource(R.string.coach_week_progress_nogoal, runs),
                style = runTextStyle(16.sp, t.label, FontWeight.SemiBold),
                modifier = Modifier.testTag("coach-week-progress"),
            )
        }
        Spacer(Modifier.height(12.dp))
        WeekStrip(days)
        Spacer(Modifier.height(20.dp))
        if (workout != null) {
            TodayCard(workout, onOpen = { actions.onWorkout(workout.template.id) })
        } else {
            NeedGoal(actions.onGoals)
        }
        Spacer(Modifier.height(18.dp))
        RecLine(rec, workout, snapshot?.consented == true, onRequest = viewModel::requestRecommendation)
        Spacer(Modifier.height(24.dp))
        CoachSection(stringResource(R.string.coach_recent_title))
        Spacer(Modifier.height(10.dp))
        RecentCard(latest, viewModel, onClick = { onOpenRun(latest.id) })
        Spacer(Modifier.height(24.dp))
    }
}

/** 이번 주 띠 — 날마다 요일 · 날짜 · 실제 러닝 체크(계획은 섞지 않는다). 미래 날은 흐리게, 체크 없이 */
@Composable
private fun WeekStrip(days: List<WeekDay>) {
    val t = runTone()
    val locale = LocalConfiguration.current.locales[0]
    val weekday = remember(locale) { DateTimeFormatter.ofPattern("E", locale) }
    val spoken = rememberFormatter(R.string.coach_date_wd)
    Row(Modifier.fillMaxWidth()) {
        days.forEach { day ->
            val dayText = spoken.format(day.date)
            val cd = when {
                day.done -> stringResource(R.string.coach_day_done_cd, dayText)
                day.today -> stringResource(R.string.coach_day_today_cd, dayText)
                else -> dayText
            }
            Column(
                Modifier.weight(1f).clearAndSetSemantics { contentDescription = cd }.testTag("coach-day-${day.date}"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(weekday.format(day.date), style = runTextStyle(13.sp, t.label, FontWeight.Medium, 1.2f))
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier.size(38.dp).clip(CircleShape).background(if (day.today) t.cobalt else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${day.date.dayOfMonth}",
                        style = runTextStyle(
                            19.sp,
                            when {
                                day.today -> Color.White
                                day.future -> t.muted
                                else -> t.text
                            },
                            FontWeight.Bold, 1.1f,
                        ),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                    if (day.done) Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = t.cyan, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/** 오늘 카드 — 이름표(오늘의 계획 · 오늘의 추천 · 기본 훈련) · 훈련 이름 · 총 시간 · 단계 막대. 누르면 훈련 상세(02) */
@Composable
private fun TodayCard(workout: TodayWorkout, onOpen: () -> Unit) {
    val t = runTone()
    val shape = RoundedCornerShape(20.dp)
    val label = stringResource(
        when (workout.source) {
            TodaySource.PLAN -> R.string.coach_today_plan
            TodaySource.AI -> R.string.coach_today_pick
            TodaySource.BASIC -> R.string.coach_today_basic
        },
    )
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(t.glow, t.panel)), shape)
            .border(1.dp, t.panelEdge, shape)
            .feedbackClickable(onClick = onOpen)
            .testTag("coach-today-card"),
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = runTextStyle(14.sp, t.label, FontWeight.SemiBold), modifier = Modifier.testTag("coach-today-source"))
                if (workout.source == TodaySource.AI && workout.mock) {
                    Spacer(Modifier.width(8.dp))
                    CoachPill(stringResource(R.string.coach_ai_mock))
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = t.text, modifier = Modifier.size(26.dp))
            }
            Text(stringResource(familyName(workout.template.family)), style = runTextStyle(26.sp, t.text, FontWeight.ExtraBold, 1.3f))
            RunNumber(
                "${workout.template.minutes}", unit = stringResource(R.string.coach_unit_minutes), size = 68.sp, unitSize = 26.sp,
                italicUnit = false, valueTag = "coach-today-minutes",
            )
            Spacer(Modifier.height(12.dp))
            WorkoutBar(workout.template, height = 24.dp)
            Spacer(Modifier.height(10.dp))
            WorkoutBarLegend(workout.template)
        }
        RunDivider()
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.coach_workout_open), style = runTextStyle(17.sp, t.text, FontWeight.Bold))
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = t.text, modifier = Modifier.size(22.dp))
        }
    }
}

/** 목표가 없다 — 없는 훈련을 만들어 보이지 않고 목표 설정(06)으로 */
@Composable
private fun NeedGoal(onGoals: () -> Unit) {
    RunCard(tag = "coach-need-goal") {
        CoachQuoteTitle(stringResource(R.string.coach_need_goal_line))
        Spacer(Modifier.height(12.dp))
        RunButton(stringResource(R.string.coach_set_goal), onGoals, kind = RunButtonKind.Secondary, chevron = true)
    }
}

/** 오늘 카드 아래 AI 한 줄 — 추천의 이유(AI) · 추천 받기 · 진행 중. 계획 · 기본 훈련에는 AI 이유를 붙이지 않는다 */
@Composable
private fun RecLine(rec: CoachAi, workout: TodayWorkout?, consented: Boolean, onRequest: () -> Unit) {
    val t = runTone()
    if (workout != null && workout.source == TodaySource.AI) {
        val reason = workout.reason ?: return
        CoachQuote(
            stringResource(R.string.coach_ai_coach_label),
            extra = if (workout.mock) {
                { CoachPill(stringResource(R.string.coach_ai_mock)) }
            } else {
                null
            },
        ) { CoachQuoteTitle(reason) }
        return
    }
    // 추천을 받을 자리 — 기본 훈련을 보이는 중이고 동의했을 때만(계획 · 목표 없음에는 붙이지 않는다)
    if (workout == null || workout.source != TodaySource.BASIC || !consented) return
    when (rec) {
        CoachAi.Requestable -> RunTextAction(stringResource(R.string.coach_rec_request), onRequest, Modifier.testTag("coach-rec-request"), chevron = true)
        is CoachAi.Working -> Row(verticalAlignment = Alignment.CenterVertically) {
            RunSpinner(Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(if (rec.running) R.string.coach_ai_working_label else R.string.coach_ai_queued_label),
                style = runTextStyle(15.sp, t.label, FontWeight.Medium),
            )
        }
        else -> Unit
    }
}

/** 최근 러닝 한 장 — 경로 썸네일 · 거리 · 날짜 · 시간. 누르면 분석 탭의 그 기록 */
@Composable
private fun RecentCard(row: RunRecordRow, viewModel: CoachingHubViewModel, onClick: () -> Unit) {
    val t = runTone()
    val zone = remember { ZoneId.systemDefault() }
    val short = rememberFormatter(R.string.coach_date_short)
    RunCard(padding = PaddingValues(10.dp), onClick = onClick, tag = "coach-recent") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RouteThumb(row.id, row.hasTrack, viewModel, Modifier.width(118.dp).height(92.dp))
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                RunNumber(kmText(row.distanceMeters), unit = "km", size = 34.sp, unitSize = 17.sp, italicUnit = false)
                Text(
                    stringResource(R.string.coach_recent_line, short.format(row.startedAt.localDate(zone)), clockText(row.durationSec)),
                    style = runTextStyle(15.sp, t.label, FontWeight.Medium),
                )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = t.label, modifier = Modifier.size(26.dp))
        }
    }
}

/** 경로 썸네일 — 내 러닝 기록과 같은 저장 좌표(이 휴대폰에서만 그린다 — 서버 · AI 로 보내지 않는다) */
@Composable
private fun RouteThumb(id: Long, hasTrack: Boolean, viewModel: CoachingHubViewModel, modifier: Modifier) {
    val t = runTone()
    var points by remember(id) { mutableStateOf(if (hasTrack) viewModel.cachedRoute(id) else emptyList()) }
    LaunchedEffect(id, hasTrack) {
        points = if (hasTrack) viewModel.route(id) else emptyList()
    }
    RunMapFrame(modifier) {
        val shown: List<GeoPoint>? = points
        when {
            shown == null -> Unit
            shown.size >= 2 -> LiveRouteMap(shown, Modifier.fillMaxSize(), seed = id.toInt(), routeColor = t.cyan)
            else -> Icon(
                Icons.Outlined.LocationOff, contentDescription = null, tint = t.label,
                modifier = Modifier.align(Alignment.Center).size(26.dp),
            )
        }
    }
}

/** 기록 없음(08) — 장식 지도는 두지 않는다. 기존 러닝으로 · 목표 설정 */
@Composable
private fun NoRecords(actions: CoachingActions) {
    val t = runTone()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = RunSpec.Gutter).testTag("coach-no-records")) {
        Spacer(Modifier.height(20.dp))
        CoachPill(stringResource(R.string.coach_no_records_pill))
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.coach_no_records_title), style = runTextStyle(32.sp, t.text, FontWeight.ExtraBold, 1.25f))
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.coach_no_records_body), style = runTextStyle(16.sp, t.label, FontWeight.Medium, 1.5f))
        Spacer(Modifier.height(26.dp))
        CoachSection(stringResource(R.string.coach_no_records_later))
        Spacer(Modifier.height(6.dp))
        InfoRow(Icons.Filled.BarChart, stringResource(R.string.coach_no_records_analysis), stringResource(R.string.coach_no_records_analysis_sub))
        RunDivider()
        InfoRow(Icons.Outlined.CalendarMonth, stringResource(R.string.coach_no_records_change), stringResource(R.string.coach_no_records_change_sub))
        RunDivider()
        Spacer(Modifier.height(18.dp))
        CoachQuote(stringResource(R.string.coach_guide_label)) { CoachQuoteTitle(stringResource(R.string.coach_no_records_guide)) }
        Spacer(Modifier.height(22.dp))
        RunButton(stringResource(R.string.coach_go_run), actions.onGoRun, Modifier.testTag("coach-go-run"), chevron = true)
        Spacer(Modifier.height(6.dp))
        RunTextAction(stringResource(R.string.coach_set_goal), actions.onGoals, Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(20.dp))
    }
}

/** 그림 · 제목 · 한 줄(누르지 않는 안내) */
@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String) {
    val t = runTone()
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(t.panel),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = t.cobaltText, modifier = Modifier.size(24.dp)) }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = runTextStyle(17.sp, t.text, FontWeight.Bold, 1.3f))
            Text(sub, style = runTextStyle(14.sp, t.label, FontWeight.Medium, 1.35f))
        }
    }
}

// ── 분석(03 · 07 · 09 · 10 · 11 · 12 · 16) ───────────────────────────

@Composable
private fun AnalysisTab(viewModel: CoachingHubViewModel, account: CoachAccount, actions: CoachingActions) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val record by viewModel.record.collectAsStateWithLifecycle()
    val ai by viewModel.runAi.collectAsStateWithLifecycle()
    val pace by viewModel.pace.collectAsStateWithLifecycle()
    var picking by rememberSaveable { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<CoachResult?>(null) }
    val list = (rows as? Loaded.Ready)?.value
    val loadedRecord = record
    val shown = (loadedRecord as? Loaded.Ready)?.value
    // 고른 기록이 사라졌다(지움) — 가장 최근 기록으로
    LaunchedEffect(loadedRecord, list) {
        if (loadedRecord is Loaded.Ready && loadedRecord.value == null && !list.isNullOrEmpty()) viewModel.pickLatest()
    }
    when {
        rows == Loaded.Failed || loadedRecord == Loaded.Failed -> CoachLoadFailed()
        list == null -> CoachLoading()
        list.isEmpty() -> NoAnalysis(actions.onGoRun)
        shown == null -> CoachLoading()
        else -> AnalysisContent(
            viewModel, shown, shownAi(ai, account.snapshot), actions,
            onPick = { picking = true },
            onPace = { result ->
                viewModel.closePace()
                confirm = result
            },
        )
    }
    if (picking && !list.isNullOrEmpty()) {
        RecordPicker(list, shown?.id, onPick = { id ->
            viewModel.pick(id)
            picking = false
        }, onDismiss = { picking = false })
    }
    val target = confirm
    if (target != null) {
        PaceSheet(target, account, pace, onApply = { viewModel.applyPace(target.id) }, onDismiss = {
            viewModel.closePace()
            confirm = null
        })
    }
}

@Composable
private fun AnalysisContent(
    viewModel: CoachingHubViewModel,
    shown: WalkSessionEntity,
    ai: CoachAi,
    actions: CoachingActions,
    onPick: () -> Unit,
    onPace: (CoachResult) -> Unit,
) {
    val t = runTone()
    val zone = remember { ZoneId.systemDefault() }
    val md = rememberFormatter(R.string.coach_date_md)
    val dateText = md.format(shown.startedAt.localDate(zone))
    val average = averagePace(shown.durationSec, shown.distanceMeters)
    val splits = remember(shown.distanceMeters, shown.durationSec, shown.laps) {
        runSplits(shown.distanceMeters, shown.durationSec, shown.laps)
    }
    val offline = ai is CoachAi.Ready && ai.offline
    var showSaved by rememberSaveable(shown.id) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = RunSpec.Gutter)) {
        if (offline) OfflineBanner()
        Spacer(Modifier.height(14.dp))
        val pickCd = stringResource(R.string.coach_pick_record_cd)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RouteThumb(shown.id, shown.track.isNotEmpty(), viewModel, Modifier.size(96.dp))
            Row(
                Modifier.weight(1f).padding(start = 14.dp).heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                    .feedbackClickable(onClick = onPick).semantics { contentDescription = pickCd }.testTag("coach-pick-record")
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.coach_run_title_date, dateText),
                    style = runTextStyle(17.sp, t.label, FontWeight.SemiBold), modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = t.label, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            RunNumber(kmText(shown.distanceMeters), unit = "km", size = 60.sp, unitSize = 24.sp, italicUnit = false, valueTag = "coach-run-km")
            Box(Modifier.padding(horizontal = 14.dp).width(1.dp).fillMaxHeight().background(t.divider))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LabeledValue(clockText(shown.durationSec), stringResource(R.string.coach_time), "coach-run-time")
                LabeledValue(
                    paceText(average)?.let { it + stringResource(R.string.coach_per_km) } ?: "—",
                    stringResource(R.string.coach_avg_pace), "coach-run-pace",
                )
            }
        }
        Spacer(Modifier.height(26.dp))
        when (splits) {
            is RunSplits.Ready -> {
                CoachSection(stringResource(R.string.coach_splits_title))
                Spacer(Modifier.height(10.dp))
                val axis = remember(splits, average) { paceAxis(splits.full.map { it.seconds }, average) }
                PaceChart(splits.full, average, axis)
                Text(
                    stringResource(R.string.coach_splits_basis), style = runTextStyle(13.sp, t.muted, FontWeight.Medium),
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
                val partial = splits.partial
                if (partial != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.coach_partial, kmText(partial.meters), paceText(partial.paceSecPerKm).orEmpty()),
                        style = runTextStyle(13.sp, t.muted, FontWeight.Medium), textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().testTag("coach-partial"),
                    )
                }
            }
            is RunSplits.Unavailable -> SplitsUnavailable(splits.gap)
        }
        Spacer(Modifier.height(24.dp))
        RunAiBlock(
            ai = ai,
            showSaved = showSaved,
            onShowSaved = { showSaved = true },
            onRequest = viewModel::requestRun,
            onRetry = viewModel::retryRun,
            onConsent = actions.onConsent,
            onOpenRecord = { actions.onOpenRecord(shown.id) },
            onPace = onPace,
        )
        if (offline) {
            Spacer(Modifier.height(14.dp))
            RunDivider()
            OfflineLine(stringResource(R.string.coach_offline_foot))
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LabeledValue(value: String, label: String, tag: String) {
    val t = runTone()
    Row(verticalAlignment = Alignment.Bottom) {
        Text(value, style = runTextStyle(26.sp, t.text, FontWeight.ExtraBold, 1.15f), modifier = Modifier.testTag(tag))
        Spacer(Modifier.width(10.dp))
        Text(label, style = runTextStyle(14.sp, t.label, FontWeight.Medium, 1.2f), modifier = Modifier.padding(bottom = 3.dp))
    }
}

/** 구간 분석 불가(09) — 없는 그래프 · 숫자를 채우지 않는다. 이유는 코드가 판정한 것 */
@Composable
private fun SplitsUnavailable(gap: SplitGap) {
    val t = runTone()
    Column(
        Modifier.fillMaxWidth().padding(vertical = 26.dp).testTag("coach-splits-unavailable"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.Block, contentDescription = null, tint = t.label, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.coach_splits_none_title), style = runTextStyle(19.sp, t.text, FontWeight.Bold))
        Spacer(Modifier.height(6.dp))
        Text(stringResource(gapText(gap)), style = runTextStyle(15.sp, t.label, FontWeight.Medium, 1.45f), textAlign = TextAlign.Center)
    }
}

@StringRes
private fun gapText(gap: SplitGap): Int = when (gap) {
    SplitGap.NO_DISTANCE -> R.string.coach_gap_no_distance
    SplitGap.TOO_SHORT -> R.string.coach_gap_too_short
    SplitGap.NOT_SAVED -> R.string.coach_gap_not_saved
    SplitGap.MANUAL_LAPS -> R.string.coach_gap_manual
    SplitGap.MISMATCH -> R.string.coach_gap_mismatch
    SplitGap.OUT_OF_RANGE -> R.string.coach_gap_range
}

@StringRes
private fun skipText(reason: String?): Int = when (reason) {
    "no_splits" -> R.string.coach_skip_no_splits
    "too_short" -> R.string.coach_skip_too_short
    "not_a_run" -> R.string.coach_skip_not_run
    else -> R.string.coach_skip_other
}

/** 기록 하나의 AI 칸 — 상태마다 다른 말(서버 확인 대기는 "분석 중"이 아니다). 기본 기록은 이 칸과 상관없이 위에 있다 */
@Composable
private fun RunAiBlock(
    ai: CoachAi,
    showSaved: Boolean,
    onShowSaved: () -> Unit,
    onRequest: () -> Unit,
    onRetry: (Long?) -> Unit,
    onConsent: () -> Unit,
    onOpenRecord: () -> Unit,
    onPace: (CoachResult) -> Unit,
) {
    val t = runTone()
    val aiLabel = stringResource(R.string.coach_ai_label)
    when (ai) {
        CoachAi.Loading -> LoadingLine(stringResource(R.string.coach_ai_loading))
        CoachAi.Hidden -> Unit
        CoachAi.NeedsConsent -> CoachQuote(aiLabel, Modifier.testTag("coach-ai-consent")) {
            CoachQuoteBody(stringResource(R.string.coach_ai_consent))
            RunTextAction(stringResource(R.string.coach_ai_consent_action), onConsent, chevron = true)
        }
        CoachAi.NeedsSignIn -> CoachQuote(aiLabel, Modifier.testTag("coach-ai-signin")) {
            CoachQuoteBody(stringResource(R.string.coach_signin))
        }
        CoachAi.AwaitingServer -> CoachQuote(aiLabel, Modifier.testTag("coach-ai-awaiting")) {
            CoachQuoteTitle(stringResource(R.string.coach_ai_waiting_server))
            CoachQuoteBody(stringResource(R.string.coach_ai_waiting_server_sub))
        }
        CoachAi.NotVerified -> CoachQuote(aiLabel, Modifier.testTag("coach-ai-not-verified")) {
            CoachQuoteBody(stringResource(R.string.coach_ai_not_verified))
        }
        is CoachAi.Skipped -> {
            CoachQuote(stringResource(R.string.coach_ai_skipped_label), Modifier.testTag("coach-ai-skipped")) {
                CoachQuoteTitle(stringResource(R.string.coach_ai_skipped))
                CoachQuoteBody(stringResource(skipText(ai.reason)))
            }
            Spacer(Modifier.height(16.dp))
            RunDivider()
            CoachLinkRow(stringResource(R.string.coach_ai_basic_only), onOpenRecord, "coach-open-record")
        }
        CoachAi.Requestable -> CoachQuote(aiLabel, Modifier.testTag("coach-ai-requestable")) {
            CoachQuoteTitle(stringResource(R.string.coach_ai_request_title))
            Spacer(Modifier.height(12.dp))
            RunButton(stringResource(R.string.coach_ai_request), onRequest, Modifier.testTag("coach-ai-request"), kind = RunButtonKind.Secondary)
            CoachQuoteBody(stringResource(R.string.coach_ai_request_note))
        }
        is CoachAi.Working -> WorkingBlock(ai.running)
        is CoachAi.Ready -> ReadyBlock(ai.result, ai.offline, ai.stale, onRequest, onPace)
        is CoachAi.Failed -> {
            CoachQuote(aiLabel, Modifier.testTag("coach-ai-failed")) {
                CoachQuoteTitle(stringResource(R.string.coach_ai_failed), dim = true)
                CoachQuoteTitle(stringResource(R.string.coach_ai_failed_safe))
                CoachQuoteBody(stringResource(if (ai.retryable) R.string.coach_ai_failed_hint else R.string.coach_ai_failed_final))
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (ai.retryable) {
                    RunButton(
                        stringResource(R.string.coach_ai_retry), { onRetry(ai.jobId) },
                        Modifier.weight(1f).testTag("coach-ai-retry"),
                    )
                    Spacer(Modifier.width(12.dp))
                }
                RunTextAction(stringResource(R.string.coach_ai_basic_only), onOpenRecord)
            }
        }
        is CoachAi.Limited -> {
            Row(Modifier.fillMaxWidth().testTag("coach-ai-limit")) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = t.label, modifier = Modifier.size(34.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.coach_limit_label), style = runTextStyle(14.sp, t.label, FontWeight.SemiBold))
                    Text(stringResource(R.string.coach_limit_title), style = runTextStyle(20.sp, t.text, FontWeight.ExtraBold, 1.35f))
                    Text(stringResource(R.string.coach_limit_sub), style = runTextStyle(15.sp, t.label, FontWeight.Medium, 1.45f))
                    val reset = ai.resetAt
                    if (reset != null) {
                        Text(
                            stringResource(R.string.coach_limit_reset, dateTimeText(reset)),
                            style = runTextStyle(14.sp, t.muted, FontWeight.Medium), modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
            val saved = ai.saved
            if (saved != null) {
                Spacer(Modifier.height(16.dp))
                if (showSaved) {
                    ReadyBlock(saved, offline = false, stale = false, onRequest = onRequest, onPace = onPace, allowApply = false)
                } else {
                    RunDivider()
                    CoachLinkRow(stringResource(R.string.coach_limit_saved), onShowSaved, "coach-show-saved")
                }
            }
        }
        CoachAi.Offline -> OfflineLine(stringResource(R.string.coach_offline_none))
    }
}

/** 분석 중(07) — 기본 기록은 이미 위에 있다. 화면을 떠나도 된다 */
@Composable
private fun WorkingBlock(running: Boolean) {
    val t = runTone()
    CoachQuote(
        stringResource(if (running) R.string.coach_ai_working_label else R.string.coach_ai_queued_label),
        Modifier.testTag("coach-ai-working"),
        extra = { RunSpinner(Modifier.size(18.dp)) },
    ) {
        CoachQuoteTitle(stringResource(R.string.coach_ai_working))
        CoachQuoteBody(stringResource(R.string.coach_ai_working_sub))
    }
    Spacer(Modifier.height(14.dp))
    repeat(2) {
        Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(44.dp).clip(RoundedCornerShape(10.dp)).background(t.panel))
    }
    Spacer(Modifier.height(10.dp))
    Text(stringResource(R.string.coach_ai_leave_ok), style = runTextStyle(14.sp, t.label, FontWeight.Medium))
}

/**
 * 분석 결과(03 · 10) — 근거(코드 숫자를 앱 글자로) · AI 해석 · 다음 러닝 제안. 모의 공급자면 "모의 응답", 오프라인이면 생성 시각,
 * 기록이 바뀌었으면 오래된 분석이라고 알리고 제안을 적용하지 않는다.
 */
@Composable
private fun ReadyBlock(
    result: CoachResult,
    offline: Boolean,
    stale: Boolean,
    onRequest: () -> Unit,
    onPace: (CoachResult) -> Unit,
    allowApply: Boolean = true,
) {
    val t = runTone()
    val fact = result.fact?.let { factText(it) }
    val generated = rememberFormatter(R.string.coach_date_short).format(result.generatedAt.localDate(ZoneId.systemDefault()))
    CoachQuote(
        stringResource(R.string.coach_ai_label),
        Modifier.testTag("coach-ai-ready"),
        extra = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (offline) {
                    Text(stringResource(R.string.coach_ai_generated_at, generated), style = runTextStyle(13.sp, t.label, FontWeight.Medium))
                }
                if (result.mock) {
                    if (offline) Spacer(Modifier.width(8.dp))
                    CoachPill(stringResource(R.string.coach_ai_mock), Modifier.testTag("coach-ai-mock"))
                }
            }
        },
    ) {
        if (fact != null) CoachQuoteTitle(fact, Modifier.testTag("coach-ai-fact"), dim = stale)
        val explanation = result.explanation
        if (explanation != null) CoachQuoteBody(explanation)
        if (stale) {
            CoachQuoteBody(stringResource(R.string.coach_ai_stale))
            RunTextAction(stringResource(R.string.coach_ai_reanalyze), onRequest, chevron = true)
        }
    }
    val candidate = result.candidate
    val title = candidate?.let { candidateTitle(it, long = false) }
    if (candidate != null && title != null) {
        Spacer(Modifier.height(16.dp))
        RunDivider()
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.coach_next_label), style = runTextStyle(14.sp, t.label, FontWeight.Medium))
        if (allowApply && !offline && !stale) {
            CoachLinkRow(title, { onPace(result) }, "coach-next", sub = candidateSub(candidate))
        } else {
            Text(title, style = runTextStyle(19.sp, t.text, FontWeight.Bold, 1.3f), modifier = Modifier.padding(vertical = 8.dp))
            candidateSub(candidate)?.let { Text(it, style = runTextStyle(14.sp, t.cobaltText, FontWeight.Medium)) }
        }
    }
}

/** 목표 페이스 적용 확인(16) — 하단 탭까지 덮는 시트. 적용해도 러닝을 시작하지 않는다. 성공은 서버 답을 받은 뒤에만 */
@Composable
private fun PaceSheet(result: CoachResult, account: CoachAccount, state: SaveState, onApply: () -> Unit, onDismiss: () -> Unit) {
    val t = runTone()
    val candidate = result.candidate ?: return
    val suggested = paceText(candidate.values["pace_sec"]?.toDouble()) ?: return
    val saving = state == SaveState.Saving
    val current = account.snapshot?.prefs?.targetPaceSec?.let { paceText(it.toDouble()) }
    RunSheet(onDismiss = onDismiss, modifier = Modifier.testTag("coach-pace-sheet"), showClose = false, dismissible = !saving) {
        Column(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.coach_next_label), style = runTextStyle(14.sp, t.label, FontWeight.Medium))
            Spacer(Modifier.height(6.dp))
            Text(candidateTitle(candidate, long = true).orEmpty(), style = runTextStyle(26.sp, t.text, FontWeight.ExtraBold, 1.3f))
            val reason = result.actionReason
            if (reason != null) {
                Spacer(Modifier.height(6.dp))
                Text(reason, style = runTextStyle(15.sp, t.label, FontWeight.Medium, 1.45f))
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.coach_pace_current), style = runTextStyle(14.sp, t.label, FontWeight.Medium))
                    Text(
                        current?.let { it + stringResource(R.string.coach_per_km) } ?: stringResource(R.string.coach_pace_none),
                        style = runTextStyle(24.sp, t.text, FontWeight.ExtraBold, 1.25f), modifier = Modifier.testTag("coach-pace-current"),
                    )
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = t.text, modifier = Modifier.size(24.dp))
                Box(Modifier.padding(horizontal = 14.dp).width(1.dp).fillMaxHeight().background(t.divider))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.coach_pace_suggested), style = runTextStyle(14.sp, t.label, FontWeight.Medium))
                    RunNumber(
                        suggested, unit = stringResource(R.string.coach_per_km), size = 34.sp, unitSize = 16.sp,
                        italicUnit = false, valueTag = "coach-pace-suggested",
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            CoachQuote(null) { CoachQuoteBody(stringResource(R.string.coach_pace_note)) }
            when (state) {
                SaveState.Saved -> SheetNotice(stringResource(R.string.coach_pace_applied), RunNoticeKind.Info, "coach-pace-applied")
                SaveState.Conflict -> SheetNotice(stringResource(R.string.coach_conflict), RunNoticeKind.Warn, "coach-pace-conflict")
                SaveState.Failed, SaveState.Offline -> SheetNotice(stringResource(R.string.coach_save_failed), RunNoticeKind.Error, "coach-pace-failed")
                SaveState.Idle, SaveState.Saving -> Unit
            }
            Spacer(Modifier.height(20.dp))
            if (state == SaveState.Saved) {
                RunButton(stringResource(R.string.common_close), onDismiss, Modifier.testTag("coach-pace-close"))
            } else {
                RunButton(
                    stringResource(R.string.coach_pace_apply), onApply, Modifier.testTag("coach-pace-apply"),
                    enabled = account is CoachAccount.Ready && !saving, busy = saving,
                )
                Spacer(Modifier.height(10.dp))
                RunButton(
                    stringResource(R.string.coach_cancel), onDismiss, Modifier.testTag("coach-pace-cancel"),
                    kind = RunButtonKind.Secondary, enabled = !saving,
                )
            }
        }
    }
}

@Composable
private fun SheetNotice(text: String, kind: RunNoticeKind, tag: String) {
    Spacer(Modifier.height(12.dp))
    RunNotice(text, kind = kind, tag = tag)
}

/** 분석할 기록 고르기 — 최근 먼저(이 계정의 기록만) */
@Composable
private fun RecordPicker(rows: List<RunRecordRow>, selectedId: Long?, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val zone = remember { ZoneId.systemDefault() }
    val md = rememberFormatter(R.string.coach_date_md)
    RunSheet(onDismiss = onDismiss, modifier = Modifier.testTag("coach-record-picker")) {
        RunSheetText(stringResource(R.string.coach_pick_record_title))
        Spacer(Modifier.height(14.dp))
        Column(Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
            rows.forEach { row ->
                RunChoiceRow(
                    title = stringResource(R.string.coach_recent_line, md.format(row.startedAt.localDate(zone)), kmText(row.distanceMeters) + "km"),
                    description = clockText(row.durationSec),
                    selected = row.id == selectedId,
                    onSelect = { onPick(row.id) },
                    compact = true,
                    modifier = Modifier.padding(vertical = 4.dp).testTag("coach-pick-${row.id}"),
                )
            }
        }
    }
}

/** 분석할 기록이 없다 — 기존 러닝으로 */
@Composable
private fun NoAnalysis(onGoRun: () -> Unit) {
    val t = runTone()
    Column(
        Modifier.fillMaxSize().padding(horizontal = RunSpec.Gutter, vertical = 40.dp).testTag("coach-no-analysis"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.coach_no_analysis_records), style = runTextStyle(18.sp, t.text, FontWeight.Bold),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        RunButton(stringResource(R.string.coach_go_run), onGoRun, chevron = true)
    }
}

@Composable
private fun OfflineBanner() {
    val t = runTone()
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp).testTag("coach-offline-banner"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.WifiOff, contentDescription = null, tint = t.label, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(stringResource(R.string.coach_offline_banner), style = runTextStyle(15.sp, t.label, FontWeight.Medium))
    }
    RunDivider()
}

@Composable
private fun OfflineLine(text: String) {
    val t = runTone()
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp).testTag("coach-offline-line"), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.WifiOff, contentDescription = null, tint = t.label, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Text(text, style = runTextStyle(15.sp, t.label, FontWeight.Medium, 1.4f))
    }
}

// ── 변화(04) ───────────────────────────────────────────────────────

@Composable
private fun ChangeTab(viewModel: CoachingHubViewModel, account: CoachAccount, actions: CoachingActions) {
    val t = runTone()
    val week by viewModel.week.collectAsStateWithLifecycle()
    val change by viewModel.change.collectAsStateWithLifecycle()
    val ai by viewModel.weekAi.collectAsStateWithLifecycle()
    val thisMonday = viewModel.thisMonday()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = RunSpec.Gutter)) {
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoachIconButton(Icons.Filled.ChevronLeft, stringResource(R.string.coach_prev_week_cd), viewModel::previousWeek, "coach-week-prev")
            Text(
                weekRangeText(week), style = runTextStyle(17.sp, t.label, FontWeight.SemiBold), textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).testTag("coach-week-range"),
            )
            if (week.isBefore(thisMonday)) {
                CoachIconButton(Icons.Filled.ChevronRight, stringResource(R.string.coach_next_week_cd), viewModel::nextWeek, "coach-week-next")
            } else {
                Spacer(Modifier.size(48.dp))
            }
        }
        when (val load = change) {
            Loaded.Loading -> CoachLoading()
            Loaded.Failed -> CoachLoadFailed()
            is Loaded.Ready -> ChangeContent(load.value, week, thisMonday, account, shownAi(ai, account.snapshot), viewModel, actions)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ChangeContent(
    data: ChangeData,
    week: LocalDate,
    thisMonday: LocalDate,
    account: CoachAccount,
    ai: CoachAi,
    viewModel: CoachingHubViewModel,
    actions: CoachingActions,
) {
    val t = runTone()
    val s = data.summary
    val goal = account.snapshot?.chosenPrefs()?.runsPerWeek
    Spacer(Modifier.height(10.dp))
    Text(
        stringResource(
            when {
                s.state == WeekState.CURRENT -> R.string.coach_week_this_label
                week == thisMonday.minusWeeks(1) -> R.string.coach_week_last_label
                else -> R.string.coach_week_past_label
            },
        ),
        style = runTextStyle(15.sp, t.label, FontWeight.Medium),
    )
    Spacer(Modifier.height(6.dp))
    Text(
        when {
            s.runs == 0 -> stringResource(R.string.coach_week_head_none)
            s.state == WeekState.CURRENT -> stringResource(R.string.coach_week_head_current, s.runs)
            goal != null && s.runs >= goal -> stringResource(R.string.coach_week_head_steady, s.runs)
            else -> stringResource(R.string.coach_week_head_runs, s.runs)
        },
        style = runTextStyle(30.sp, t.text, FontWeight.ExtraBold, 1.25f),
        modifier = Modifier.testTag("coach-week-head"),
    )
    Spacer(Modifier.height(18.dp))
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        WeekStat("${s.runs}", stringResource(R.string.coach_unit_times), stringResource(R.string.coach_stat_runs), Modifier.weight(0.8f), "coach-week-runs")
        Box(Modifier.width(1.dp).fillMaxHeight().background(t.divider))
        WeekStat(kmTextShort(s.meters), "km", stringResource(R.string.coach_stat_distance), Modifier.weight(1.1f).padding(start = 14.dp), "coach-week-km")
        Box(Modifier.width(1.dp).fillMaxHeight().background(t.divider))
        WeekStat(clockText(s.seconds), null, stringResource(R.string.coach_stat_time), Modifier.weight(1.2f).padding(start = 14.dp), "coach-week-time")
    }
    Spacer(Modifier.height(26.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        CoachSection(stringResource(R.string.coach_recent4), Modifier.weight(1f))
        Text("km", style = runTextStyle(13.sp, t.label, FontWeight.Medium))
    }
    Spacer(Modifier.height(12.dp))
    WeekBars(data.recent, kmAxisTop(data.recent.maxOf { it.meters } / 1000.0), week)
    Spacer(Modifier.height(18.dp))
    RunDivider()
    Spacer(Modifier.height(16.dp))
    WeekAiBlock(ai, onRequest = viewModel::requestWeek, onRetry = viewModel::retryWeek, onConsent = actions.onConsent)
    Spacer(Modifier.height(16.dp))
    RunDivider()
    CoachLinkRow(stringResource(R.string.coach_next_plan), actions.onPlan, "coach-next-plan")
}

@Composable
private fun WeekStat(value: String, unit: String?, label: String, modifier: Modifier, tag: String) {
    val t = runTone()
    Column(modifier) {
        RunNumber(value, unit = unit, size = 34.sp, unitSize = 18.sp, italicUnit = false, valueTag = tag)
        Text(label, style = runTextStyle(14.sp, t.label, FontWeight.Medium))
    }
}

/** 한 주의 AI 정리 — 진행 중인 주는 끝나면 정리한다고만. 근거 숫자는 코드가 만든 것 */
@Composable
private fun WeekAiBlock(ai: CoachAi, onRequest: () -> Unit, onRetry: (Long?) -> Unit, onConsent: () -> Unit) {
    val aiLabel = stringResource(R.string.coach_ai_label)
    when (ai) {
        CoachAi.Loading -> LoadingLine(stringResource(R.string.coach_ai_loading))
        CoachAi.Hidden, CoachAi.AwaitingServer, CoachAi.NotVerified -> Unit
        CoachAi.NeedsSignIn -> CoachQuote(aiLabel) { CoachQuoteBody(stringResource(R.string.coach_signin)) }
        CoachAi.NeedsConsent -> CoachQuote(aiLabel, Modifier.testTag("coach-week-consent")) {
            CoachQuoteBody(stringResource(R.string.coach_ai_consent))
            RunTextAction(stringResource(R.string.coach_ai_consent_action), onConsent, chevron = true)
        }
        is CoachAi.Skipped -> when (ai.reason) {
            "week_in_progress" -> CoachQuote(aiLabel, Modifier.testTag("coach-week-progress-note")) {
                CoachQuoteBody(stringResource(R.string.coach_week_ai_progress))
            }
            // 기록이 없는 주 · 목표 없음 — 머리글이 이미 말한다
            else -> Unit
        }
        CoachAi.Requestable -> CoachQuote(aiLabel, Modifier.testTag("coach-week-requestable")) {
            Spacer(Modifier.height(4.dp))
            RunButton(stringResource(R.string.coach_ai_request), onRequest, Modifier.testTag("coach-week-request"), kind = RunButtonKind.Secondary)
            CoachQuoteBody(stringResource(R.string.coach_ai_request_note))
        }
        is CoachAi.Working -> CoachQuote(
            stringResource(if (ai.running) R.string.coach_ai_working_label else R.string.coach_ai_queued_label),
            extra = { RunSpinner(Modifier.size(18.dp)) },
        ) { CoachQuoteBody(stringResource(R.string.coach_ai_working)) }
        is CoachAi.Ready -> {
            val result = ai.result
            val fact = result.fact?.let { factText(it) }
            CoachQuote(
                aiLabel, Modifier.testTag("coach-week-ready"),
                extra = if (result.mock) {
                    { CoachPill(stringResource(R.string.coach_ai_mock)) }
                } else {
                    null
                },
            ) {
                if (fact != null) CoachQuoteTitle(fact, dim = ai.stale)
                val explanation = result.explanation
                if (explanation != null) CoachQuoteBody(explanation)
                if (ai.stale) {
                    CoachQuoteBody(stringResource(R.string.coach_ai_stale))
                    RunTextAction(stringResource(R.string.coach_ai_reanalyze), onRequest, chevron = true)
                }
            }
        }
        is CoachAi.Failed -> CoachQuote(aiLabel, Modifier.testTag("coach-week-failed")) {
            CoachQuoteBody(stringResource(R.string.coach_ai_failed))
            if (ai.retryable) RunTextAction(stringResource(R.string.coach_ai_retry), { onRetry(ai.jobId) }, chevron = true)
        }
        is CoachAi.Limited -> CoachQuote(stringResource(R.string.coach_limit_label), Modifier.testTag("coach-week-limit")) {
            CoachQuoteBody(stringResource(R.string.coach_limit_title))
        }
        CoachAi.Offline -> OfflineLine(stringResource(R.string.coach_offline_none))
    }
}

// ── 공통 ──────────────────────────────────────────────────────────

@Composable
private fun LoadingLine(text: String) {
    val t = runTone()
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("coach-loading-line"), verticalAlignment = Alignment.CenterVertically) {
        RunSpinner(Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = runTextStyle(15.sp, t.label, FontWeight.Medium))
    }
}

@Composable
internal fun CoachLoading() {
    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp).testTag("coach-loading"), contentAlignment = Alignment.Center) {
        RunSpinner(Modifier.size(32.dp))
    }
}

@Composable
internal fun CoachLoadFailed() {
    val t = runTone()
    Text(
        stringResource(R.string.coach_load_failed), style = runTextStyle(16.sp, t.label, FontWeight.Medium), textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = RunSpec.Gutter, vertical = 40.dp).testTag("coach-load-failed"),
    )
}

/** 한도가 풀리는 때 — 그 휴대폰의 시간대 · 언어로 */
@Composable
private fun dateTimeText(epochMillis: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(locale) }
    return formatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
}
