package com.stepup.android.ui.screens.home

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.tier
import com.stepup.android.ui.StepPermissions
import com.stepup.android.ui.components.AdaptiveNumber
import com.stepup.android.ui.components.BarMeter
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.ShoeArtThumbnail
import com.stepup.android.ui.components.ShoeNameWithBadge
import com.stepup.android.ui.components.ShortcutButton
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.guide.GuideTour
import com.stepup.android.ui.guide.guideTarget
import com.stepup.android.ui.screens.customize.BarStat
import com.stepup.android.ui.screens.customize.StatBar
import com.stepup.android.ui.screens.customize.statBars
import com.stepup.android.ui.screens.items.shoeName
import com.stepup.android.ui.theme.Alert
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.StepUpDesign
import com.stepup.android.ui.theme.Volt
import com.stepup.android.ui.theme.VoltText
import java.time.LocalTime
import kotlinx.coroutines.delay

/*
 * 러닝 탭 첫 화면(2026-09-29 전달본 "메인" 시안, design/run-journey-2026-09-29/02-home-v2).
 * 위부터 오늘의 걸음 · 목표 · 달성 막대 → 신고 있는 신발 카드(착용 중 · 신발 · 이름 끝 등급 배지 · Lv · 효율 / 착화감 / 내구도 막대) →
 * 러닝 시작(이 화면의 주 행동) → 기록 보기 · 코스 찾기. 거리 · 운동 시간은 홈에 두지 않는다(시안 지시).
 * 풍경 사진은 화면 전체 바탕(StepUpRoot)이 그린다. 예전 홈의 상세 기록(오늘 획득 · 7일 걸음 · 목표 · 소식 · 에너지)과
 * 풍경 넘기기는 지우지 않고 "오늘의 걸음"을 누르면 여는 상세 기록 안으로 옮겼다.
 * 모든 값은 이 기기의 실제 기록 · 신고 있는 신발에서 온다 — 시안의 6,840 · +4.8% 같은 예시 수를 쓰지 않는다.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartRun: () -> Unit = {},
    onOpenWallet: () -> Unit = {},
    onOpenNews: () -> Unit = {},
    onOpenCustomize: () -> Unit = {},
    /** 기록 보기 — 내 러닝 기록 */
    onOpenRecords: () -> Unit = {},
    /** 코스 찾기 — 코스 목록 */
    onOpenCourses: () -> Unit = {},
    onPreviousBackground: () -> Unit = {},
    onNextBackground: () -> Unit = {},
    /** 지금 보이는 풍경이 실제 날씨로 고른 것이면 그 날씨 — 상세 기록 안에서 한 줄로 밝힌다 */
    weatherScene: com.stepup.android.domain.WeatherScene? = null,
    /** 시안 검사(OnboardingDesignTest)만 쓴다 — 첫 러닝 홈(시안 11)을 그려 본다. null 이면 실제 권한 · 기록으로 판단한다 */
    firstRunPreview: Boolean? = null,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val earned by viewModel.todayEarned.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(StepPermissions.hasActivityRecognition(context))
    }
    var permissionDenied by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    // 누를 때만 묻는다(홈을 보는 것만으로는 묻지 않는다). 러닝 권한 안내와 같은 이력을 남겨 다음 안내가 맞게 고른다
    val requestActivity = com.stepup.android.ui.screens.walk.rememberActivityPermissionRequest {
        hasPermission = StepPermissions.hasActivityRecognition(context)
        permissionDenied = !hasPermission
        if (hasPermission) viewModel.onPermissionGranted()
    }
    LifecycleResumeEffect(Unit) {
        val granted = StepPermissions.hasActivityRecognition(context)
        if (granted) permissionDenied = false
        if (granted != hasPermission) {
            hasPermission = granted
            if (granted) viewModel.onPermissionGranted()
        }
        onPauseOrDispose { }
    }

    var showDetails by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val session by com.stepup.android.service.WalkSessionService.state.collectAsStateWithLifecycle()
    // 오늘 아직 아무것도 안 했는가 — 첫 러닝 홈 판단에 쓴다
    val noActivityYet = state.loaded && state.todaySteps == 0 && (earned ?: 0.0) <= 0.0
    // 첫 러닝 홈(시작·로그인·첫 사용 v1 시안 11) — 걸음 권한 전이고 이 계정의 기록이 하나도 없을 때만.
    // 권한은 여기서 묻지 않는다 — 러닝 시작 → 자유 러닝을 고르면 그때 안내한다. 기록이 있는 사람에게 "첫 러닝"이라고 쓰지 않는다
    val runCount by viewModel.runCount.collectAsStateWithLifecycle()
    val firstHome = firstRunPreview
        ?: com.stepup.android.domain.FirstHomeRules.applies(noActivityYet, hasPermission, runCount, state.lifetimeSteps)
    val largeText = LocalDensity.current.fontScale > 1.2f

    val pending by viewModel.pendingUploads.collectAsStateWithLifecycle()
    val runSec by viewModel.todayRunSec.collectAsStateWithLifecycle()
    HomeContent(
        shoe = state.equipped, loaded = state.loaded, todaySteps = state.todaySteps, goal = state.goal,
        hasPermission = hasPermission, firstHome = firstHome, running = session.isActive, largeText = largeText,
        onStartRun = onStartRun, onOpenCustomize = onOpenCustomize,
        onRequestPermission = { requestActivity() }, onOpenDetails = { showDetails = true },
    )

    if (showDetails) {
        // E01 — 오늘의 활동: 걸음 · 목표 · 남은 걸음 · 거리 · 러닝 시간 · 오늘의 SUP(서버 확인 값). 예전 상세 기록
        // (7일 걸음 · 소식 · 에너지 · 바탕 풍경)은 지우지 않고 아래 "더 보기"에 둔다
        com.stepup.android.ui.components.RunSheet(onDismiss = { showDetails = false }, modifier = Modifier.testTag("home-details-sheet")) {
            TodayActivity(
                steps = state.todaySteps, goal = state.goal, loaded = state.loaded, hasPermission = hasPermission,
                km = if (state.loaded) RewardEconomyKm(state.todaySteps) else null,
                runSec = runSec, earned = earned, pending = pending > 0,
                onRequestPermission = { requestActivity() },
            )
            Column(
                Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState()).testTag("home-details"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Spacer(Modifier.height(4.dp))
                com.stepup.android.ui.components.RunDivider()
                Text(stringResource(R.string.home_activity_more), color = Silver, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("home-records-title"))
                if (!hasPermission && permissionDenied) {
                    androidx.compose.material3.TextButton(onClick = {
                        com.stepup.android.core.ExternalIntents.openAppSettings(context)
                    }) { Text(stringResource(R.string.cd_open_settings)) }
                }
                RecordWeek(state.week, state.todaySteps, state.loaded)
                ShortcutButton(
                    icon = Icons.AutoMirrored.Filled.Article,
                    label = stringResource(R.string.home_shortcut_news),
                    subtitle = stringResource(R.string.home_shortcut_news_sub),
                    onClick = { showDetails = false; onOpenNews() },
                    modifier = Modifier.fillMaxWidth(),
                )
                EnergyLine(earnableSteps = state.earnableSteps, ready = state.loaded && state.maxEnergy > 0)
                BackgroundRow(weatherScene, onPreviousBackground, onNextBackground)
            }
            Spacer(Modifier.height(14.dp))
            com.stepup.android.ui.components.RunButton(
                stringResource(R.string.home_activity_ok), { showDetails = false },
                modifier = Modifier.testTag("home-details-ok"),
            )
        }
    }
}

/**
 * 러닝 홈의 몸통(시안 HOME) — 상태 없이 그린다. 전시장 · 신발 줄 · 러닝 시작 · 오늘 걸음.
 * 바탕 창 풍경은 StepUpRoot 가, 머리(로고 · SUP)와 하단 탭은 공통 틀이 그린다.
 */
@Composable
internal fun HomeContent(
    shoe: Sneaker?,
    loaded: Boolean,
    todaySteps: Int,
    goal: Int,
    hasPermission: Boolean,
    firstHome: Boolean,
    running: Boolean,
    largeText: Boolean,
    onStartRun: () -> Unit,
    onOpenCustomize: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenDetails: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // 시안 HOME — 위 전시장(신고 있는 신발) · 이름 · 등급 · Lv · 내구도 → 러닝 시작(주 행동) → 오늘 걸음 · 목표 막대.
        // 큰 글씨 · 낮은 화면이면 전시장 높이를 두고 위쪽만 넘긴다. 러닝 시작 · 오늘 걸음은 넘기지 않고 늘 아래에 보인다
        val scroll = largeText || maxHeight < 520.dp
        val short = maxHeight < 600.dp
        Column(Modifier.fillMaxSize().padding(bottom = 10.dp).testTag("home-content")) {
            Column(
                Modifier.fillMaxWidth().weight(1f)
                    .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            ) {
                val stageOpen = stringResource(R.string.home_stage_open)
                Box(
                    Modifier.fillMaxWidth()
                        .then(if (scroll) Modifier.height(if (short) 210.dp else 250.dp) else Modifier.weight(1f).heightIn(min = 180.dp))
                        .feedbackClickable(role = Role.Button, onClick = onOpenCustomize)
                        .semantics { onClick(label = stageOpen) { onOpenCustomize(); true } }
                        .testTag("home-shoe-card"),
                ) {
                    com.stepup.android.ui.components.ShowroomStage(shoe, Modifier.fillMaxSize())
                    if (shoe == null && loaded) {
                        Column(Modifier.align(Alignment.Center).padding(horizontal = 32.dp).padding(bottom = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.home_no_shoe_title), color = Snow, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center, modifier = Modifier.testTag("home-no-shoe"))
                            Text(stringResource(R.string.home_no_shoe_body), color = Silver, fontSize = 14.sp, textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
                HomeShoeInfo(shoe, Modifier.padding(horizontal = StepUpDesign.Gutter).padding(top = if (short) 6.dp else 10.dp))
            }
            Spacer(Modifier.height(if (short) 10.dp else 16.dp))
            // 이 화면의 주 행동 — 흰 면 · 남색 굵은 글자 · 파란 아랫면 · 왼쪽 ▶(시안)
            com.stepup.android.ui.components.RunButton(
                label = stringResource(if (running) R.string.home_continue_run else R.string.home_start_run),
                onClick = onStartRun,
                hero = true,
                icon = Icons.Filled.PlayArrow,
                modifier = Modifier.padding(horizontal = StepUpDesign.Gutter)
                    .guideTarget(GuideTour.Targets.HOME_START_RUN).testTag("home-start-run"),
            )
            Spacer(Modifier.height(if (short) 10.dp else 16.dp))
            StepsBlock(
                todaySteps = todaySteps, goal = goal, loaded = loaded,
                hasPermission = hasPermission, firstHome = firstHome,
                onRequestPermission = onRequestPermission,
                onOpenDetails = onOpenDetails,
                modifier = Modifier.padding(horizontal = StepUpDesign.Gutter),
            )
        }
    }

}

/** 오늘 걸음으로 잰 거리(km) — 예전 홈과 같은 걸음 기준 */
private fun RewardEconomyKm(steps: Int): Double = com.stepup.android.domain.RewardEconomy.distanceMeters(steps) / 1000.0

// ── 오늘의 걸음 ──────────────────────────────────────────────────

/**
 * 오늘 걸음(시안 HOME 아래) — "오늘" · 큰 걸음 수 · 오른쪽 목표, 아래 파랑 → 시안 막대. 누르면 오늘의 활동(E01).
 * 걸음 권한이 없으면 수 대신 권한 줄, 첫 러닝 홈이면 첫 안내 두 줄(시작·로그인·첫 사용 v1 시안 11).
 */
@Composable
private fun StepsBlock(
    todaySteps: Int,
    goal: Int,
    loaded: Boolean,
    hasPermission: Boolean,
    firstHome: Boolean,
    onRequestPermission: () -> Unit,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = com.stepup.android.ui.components.runTone()
    val detailsLabel = stringResource(R.string.home_open_details)
    if (firstHome) {
        // 첫 러닝 홈의 두 줄은 설명이라 누를 곳이 없다 — 걸음이 생기면 걸음 칸이 오늘의 활동을 연다
        Column(modifier.fillMaxWidth().padding(vertical = 2.dp).testTag("home-steps")) {
            Text(
                stringResource(R.string.onb_home_first_title), color = t.text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                lineHeight = 31.sp, modifier = Modifier.testTag("home-headline"),
            )
            Text(
                stringResource(R.string.onb_home_first_body), color = t.label, fontSize = 15.sp,
                modifier = Modifier.padding(top = 6.dp).testTag("home-first-run"),
            )
        }
        return
    }
    val fraction = if (goal > 0) (todaySteps.toFloat() / goal).coerceIn(0f, 1f) else 0f
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .feedbackClickable(role = Role.Button, onClick = onOpenDetails)
            .semantics { onClick(label = detailsLabel) { onOpenDetails(); true } }
            .padding(vertical = 2.dp)
            .testTag("home-steps"),
    ) {
        Text(stringResource(R.string.home_today_label), color = t.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        if (!hasPermission) {
            Spacer(Modifier.height(8.dp))
            PermissionStrip(onClick = onRequestPermission)
            Spacer(Modifier.height(4.dp))
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                com.stepup.android.ui.components.RunNumber(
                    if (loaded) "%,d".format(todaySteps) else "—", unit = stringResource(R.string.home_steps_unit),
                    size = 46.sp, unitSize = 24.sp, unitColor = t.text, valueTag = "home-steps-value",
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.home_goal_steps, "%,d".format(goal)), color = t.label, fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 1,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp).testTag("home-goal"),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        com.stepup.android.ui.components.RunMeter(
            if (loaded && hasPermission) fraction else 0f, height = 12.dp,
            modifier = Modifier.testTag("home-goal-bar"),
        )
    }
}

// ── 신고 있는 신발 ────────────────────────────────────────────────

/**
 * 전시장 아래 한 줄(시안 HOME) — 왼쪽 신발 이름(굵게 기울임) · 등급 배지 · Lv, 가는 세로선, 오른쪽 내구도 수 · 막대.
 * 배지는 앱 공통 등급 배지(채운 색)다. 신고 있는 신발이 없으면 아무것도 두지 않는다(전시장이 안내한다).
 */
@Composable
private fun HomeShoeInfo(shoe: Sneaker?, modifier: Modifier = Modifier) {
    if (shoe == null) return
    val t = com.stepup.android.ui.components.runTone()
    val points = com.stepup.android.ui.screens.items.durabilityPoints(shoe)
    Row(modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("home-shoe-info"), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1.1f)) {
            Text(
                shoe.shoeName(),
                style = TextStyle(
                    color = t.text, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, letterSpacing = (-0.5).sp, lineHeight = 30.sp,
                ),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("home-shoe-name"),
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.stepup.android.ui.components.ShoeGradeBadge(shoe.tier)
                Text(
                    stringResource(R.string.level_chip, shoe.level), color = t.text, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(50)).border(1.5.dp, t.cyan.copy(alpha = 0.85f), RoundedCornerShape(50))
                        .background(if (t.dark) Color(0xFF0A2A52) else Color(0xFFE6F6FD), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 3.dp).testTag("home-shoe-level"),
                )
            }
        }
        Box(Modifier.padding(horizontal = 14.dp).width(1.dp).height(56.dp).background(t.divider))
        Column(
            Modifier.weight(1f).semantics(mergeDescendants = true) {}.testTag("home-stat-durability"),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(stringResource(R.string.sdv_stat_durability), color = t.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.alignByBaseline())
                Spacer(Modifier.width(10.dp))
                Text(
                    "$points", color = t.cyan, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.alignByBaseline().testTag("home-stat-durability-value"),
                )
                Text(" / 100", color = t.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.alignByBaseline())
            }
            Spacer(Modifier.height(8.dp))
            com.stepup.android.ui.components.RunMeter(
                points / 100f, height = 10.dp,
                fill = listOf(Color(0xFF2FB5F0), t.cyan),
            )
        }
    }
}

// ── 오늘의 활동(E01) ────────────────────────────────────────────

/**
 * 오늘의 활동 시트 — 큰 걸음 수 · 목표 · 막대와 달성 % · 남은 걸음, 거리 | 러닝 시간, 오늘의 SUP.
 * SUP 은 서버가 원장에 적은 값만(앱이 셈한 예상은 쓰지 않는다). 올리는 중인 러닝이 있으면 "확인 중".
 */
@Composable
internal fun TodayActivity(
    steps: Int,
    goal: Int,
    loaded: Boolean,
    hasPermission: Boolean,
    km: Double?,
    runSec: Long,
    earned: Double?,
    pending: Boolean,
    onRequestPermission: () -> Unit,
) {
    val t = com.stepup.android.ui.components.runTone()
    Column(Modifier.fillMaxWidth().testTag("home-activity")) {
        Text(stringResource(R.string.home_activity_title), color = t.text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(12.dp))
        if (!hasPermission) {
            PermissionStrip(onClick = onRequestPermission)
        } else {
            com.stepup.android.ui.components.RunNumber(
                if (loaded) "%,d".format(steps) else "—", unit = stringResource(R.string.home_steps_unit),
                size = 56.sp, unitSize = 22.sp, align = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(), valueTag = "home-activity-steps",
            )
            Text(
                stringResource(R.string.home_activity_goal, "%,d".format(goal)), color = t.label, fontSize = 15.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            val fraction = if (goal > 0) (steps.toFloat() / goal).coerceIn(0f, 1f) else 0f
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.stepup.android.ui.components.RunMeter(if (loaded) fraction else 0f, height = 12.dp, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                Text(
                    if (loaded && goal > 0) "${(steps * 100L / goal)}%" else "—", color = t.cyan, fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold, modifier = Modifier.testTag("home-goal-percent"),
                )
            }
            Text(
                when {
                    !loaded -> ""
                    steps >= goal -> stringResource(R.string.home_activity_done)
                    else -> stringResource(R.string.home_activity_left, "%,d".format(goal - steps))
                },
                color = t.label, fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        com.stepup.android.ui.components.RunStatRow(
            listOf(
                com.stepup.android.ui.components.RunStat(stringResource(R.string.stat_distance), km?.let { "%.1f".format(it) } ?: "—", "km",
                    tag = "home-activity-distance"),
                com.stepup.android.ui.components.RunStat(stringResource(R.string.home_activity_run_time), "${runSec / 60}",
                    stringResource(R.string.home_activity_minutes), tag = "home-activity-time"),
            ),
            framed = true, valueSize = 30.sp,
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().testTag("home-activity-sup"), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_activity_sup), color = t.text, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f))
            com.stepup.android.ui.components.RunNumber(
                earned?.let { "+" + com.stepup.android.ui.components.formatSupDown(it, 2) } ?: "—",
                size = 28.sp, color = t.cyan, valueTag = "home-activity-sup-value",
            )
            if (pending) {
                Spacer(Modifier.width(8.dp))
                com.stepup.android.ui.components.RunChip(stringResource(R.string.home_activity_checking),
                    modifier = Modifier.testTag("home-activity-pending"))
            }
        }
        Text(stringResource(R.string.home_activity_sup_note), color = t.label, fontSize = 13.sp, textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
    }
}

/**
 * 오늘 더 적립할 수 있는 걸음 — 에너지를 한 줄로.
 *
 * 다 썼으면 그 사실과 충전까지 남은 시간을 적는다. 러닝을 막지는 않는다 —
 * 에너지가 없어도 뛰는 것 자체는 기록된다.
 */
@Composable
private fun EnergyLine(earnableSteps: Int, ready: Boolean) {
    var secondsLeft by remember { mutableIntStateOf(86_400 - LocalTime.now().toSecondOfDay()) }
    LaunchedEffect(Unit) {
        while (true) {
            secondsLeft = 86_400 - LocalTime.now().toSecondOfDay()
            delay(1_000)
        }
    }
    val countdown = "%02d:%02d".format(secondsLeft / 3600, (secondsLeft % 3600) / 60)
    val empty = ready && earnableSteps <= 0
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.Bolt,
            contentDescription = null,
            tint = if (empty) Alert else Volt,
            modifier = Modifier.size(16.dp),
        )
        Box(Modifier.width(6.dp))
        Text(
            text = when {
                !ready -> stringResource(R.string.home_energy_loading)
                empty -> stringResource(R.string.home_energy_empty) + " · " +
                    stringResource(R.string.home_recharge_in, countdown)
                else -> stringResource(R.string.home_energy_can, "%,d".format(earnableSteps))
            },
            fontSize = 14.sp,
            color = if (empty) Alert else Silver,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PermissionStrip(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Volt.copy(alpha = 0.14f))
            .feedbackClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("home-permission"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, tint = VoltText, modifier = Modifier.size(18.dp))
        Text(
            text = stringResource(R.string.perm_allow),
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Snow,
        )
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = VoltText, modifier = Modifier.size(18.dp))
    }
}

/** 바탕 풍경 — 이전 · 다음(예전 홈 양옆 화살표). 실제 날씨로 고른 풍경이면 그 날씨를 한 줄로 */
@Composable
private fun BackgroundRow(
    weatherScene: com.stepup.android.domain.WeatherScene?,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_background), color = Snow, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f))
            SceneArrow(Icons.Filled.ChevronLeft, stringResource(R.string.home_previous_background), onPrevious,
                Modifier.testTag("home-background-previous"))
            SceneArrow(Icons.Filled.ChevronRight, stringResource(R.string.home_next_background), onNext,
                Modifier.testTag("home-background-next"))
        }
        if (weatherScene != null) {
            Text(
                stringResource(R.string.home_weather_caption, stringResource(when (weatherScene) {
                    com.stepup.android.domain.WeatherScene.DAY -> R.string.weather_day
                    com.stepup.android.domain.WeatherScene.DUSK -> R.string.weather_dusk
                    com.stepup.android.domain.WeatherScene.NIGHT -> R.string.weather_night
                    com.stepup.android.domain.WeatherScene.RAIN -> R.string.weather_rain
                })),
                color = com.stepup.android.ui.theme.Slate, fontSize = 12.sp,
                modifier = Modifier.testTag("home-weather-caption"),
            )
        }
    }
}

/** 풍경 넘기기 — 48dp 터치 */
@Composable
private fun SceneArrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.size(StepUpDesign.TouchTarget)
            .clip(CircleShape)
            .feedbackClickable(onClick = onClick)
            .semantics { contentDescription = description; role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Snow.copy(alpha = 0.85f), modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun RecordWeek(week: List<com.stepup.android.data.local.DailyStepsEntity>, todaySteps: Int, loaded: Boolean) {
    val today = java.time.LocalDate.now()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val recorded = week.associateBy { it.epochDay }
    val values = days.map { if (it == today) todaySteps else recorded[it.toEpochDay()]?.steps ?: 0 }
    val maximum = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(stringResource(R.string.home_recent_week), style = MaterialTheme.typography.titleMedium, color = Snow)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        days.forEachIndexed { index, day ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                AdaptiveNumber(if (loaded) "%,d".format(values[index]) else "—", 12.sp,
                    color = if (day == today) VoltText else Silver, textAlign = TextAlign.Center)
                Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.fillMaxWidth(0.72f)
                        .height(if (loaded) (40f * values[index].toFloat() / maximum).coerceAtLeast(2f).dp else 2.dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(if (day == today) Volt else Volt.copy(alpha = 0.4f)))
                }
                Text(day.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, locale),
                    style = MaterialTheme.typography.labelSmall, color = Silver)
            }
        }
    }
    }
}

