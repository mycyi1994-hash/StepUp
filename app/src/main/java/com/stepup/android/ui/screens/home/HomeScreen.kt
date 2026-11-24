package com.stepup.android.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.stepup.android.ui.components.PlainDivider
import com.stepup.android.ui.components.PlainListRow
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.ui.StepPermissions
import com.stepup.android.ui.components.AdaptiveNumber
import com.stepup.android.ui.components.PrimaryButton
import com.stepup.android.ui.components.ShortcutButton
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.components.countUpFloat
import com.stepup.android.ui.components.popOnChange
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.guide.GuideTour
import com.stepup.android.ui.guide.guideTarget
import com.stepup.android.ui.theme.Alert
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt
import com.stepup.android.ui.theme.VoltText
import java.time.LocalTime
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/*
 * 러닝 탭 첫 화면 — 2026-11-24 재디자인(docs/redesign/plain-2026-11-24 01 · 02). 아치 · 신발 전시장 없음(클라이언트 요청).
 * 위부터 오늘 러닝 걸음 카드(큰 숫자 / 목표 · 막대 · 오른쪽 캐릭터) → 회색 한 줄 안내 → 사진 카드(위에 GPS 글자) →
 * 오늘 거리 · 적립 · 이번 주 러닝 한 줄 → 러닝 시작(이 화면의 주 행동).
 * 걸음 칸을 누르면 오늘의 활동(E01) 시트 — 예전 상세 기록(7일 걸음 · 소식 · 에너지)은 그 안에 그대로 있다.
 * 신고 있는 신발 · 이름 바꾸기는 신발 탭으로 옮겼다(시안 `shoes`).
 * 모든 값은 이 기기의 실제 기록에서 온다 — 시안의 "2회" · "여의도 한강공원" 같은 예시를 쓰지 않는다.
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
    /** 시안 검사(OnboardingDesignTest)만 쓴다 — 첫 러닝 홈(시안 11)을 그려 본다. null 이면 실제 권한 · 기록으로 판단한다 */
    firstRunPreview: Boolean? = null,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val earned by viewModel.todayEarned.collectAsStateWithLifecycle()
    val week by viewModel.weekRuns.collectAsStateWithLifecycle()
    val starts by viewModel.runStarts.collectAsStateWithLifecycle()
    // 오늘의 보상(서버 0118) — 서버가 주지 않으면 null 이고 홈은 예전 모습(하루 전체 걸음)
    val bar by viewModel.dailyBar.collectAsStateWithLifecycle()
    val daily by viewModel.daily.collectAsStateWithLifecycle()
    val news by viewModel.rewardNews.collectAsStateWithLifecycle()
    val goalNotice by viewModel.goalRuleNotice.collectAsStateWithLifecycle()
    val overnightRun by viewModel.overnightRun.collectAsStateWithLifecycle()
    var goalSheet by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(StepPermissions.hasActivityRecognition(context))
    }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    // 누를 때만 묻는다(홈을 보는 것만으로는 묻지 않는다). 러닝 권한 안내와 같은 이력을 남겨 다음 안내가 맞게 고른다
    val requestActivity = com.stepup.android.ui.screens.walk.rememberActivityPermissionRequest {
        hasPermission = StepPermissions.hasActivityRecognition(context)
        permissionDenied = !hasPermission
        if (hasPermission) viewModel.onPermissionGranted()
    }
    LifecycleResumeEffect(Unit) {
        // 홈이 보일 때 오늘의 보상을 다시 묻는다(30초에 한 번까지) — 다른 폰의 러닝 · 자정이 지난 뒤도 맞게
        viewModel.refreshDaily()
        val granted = StepPermissions.hasActivityRecognition(context)
        if (granted) permissionDenied = false
        if (granted != hasPermission) {
            hasPermission = granted
            if (granted) viewModel.onPermissionGranted()
        }
        onPauseOrDispose { }
    }

    var showDetails by rememberSaveable { mutableStateOf(false) }
    val session by com.stepup.android.service.WalkSessionService.state.collectAsStateWithLifecycle()
    // 오늘 아직 아무것도 안 했는가 — 첫 러닝 홈 판단에 쓴다
    val noActivityYet = state.loaded && state.todaySteps == 0 && (earned ?: 0.0) <= 0.0
    // 첫 러닝 홈(시작·로그인·첫 사용 v1 시안 11) — 걸음 권한 전이고 이 계정의 기록이 하나도 없을 때만.
    // 권한은 여기서 묻지 않는다 — 러닝 시작 → 자유 러닝을 고르면 그때 안내한다. 기록이 있는 사람에게 "첫 러닝"이라고 쓰지 않는다
    val runCount by viewModel.runCount.collectAsStateWithLifecycle()
    val firstHome = firstRunPreview
        ?: com.stepup.android.domain.FirstHomeRules.applies(noActivityYet, hasPermission, runCount, state.lifetimeSteps)
    val largeText = LocalDensity.current.fontScale > 1.2f

    // 사람 없는 배경 15장 중 하나 — 러닝 탭에 들어올 때마다 고른다
    val photoIndex = remember { kotlin.random.Random.nextInt(RunPhotos.names.size) }
    val gps = rememberGpsReadiness()
    val pending by viewModel.pendingUploads.collectAsStateWithLifecycle()
    val runSec by viewModel.todayRunSec.collectAsStateWithLifecycle()
    HomeContent(
        loaded = state.loaded, todaySteps = state.todaySteps, goal = state.goal,
        hasPermission = hasPermission, firstHome = firstHome, running = session.isActive, largeText = largeText,
        earned = earned, km = if (state.loaded) RewardEconomyKm(state.todaySteps) else null, weekRuns = week?.runs,
        onStartRun = onStartRun,
        onRequestPermission = { requestActivity() }, onOpenDetails = { showDetails = true },
        photoIndex = photoIndex, gps = gps,
        bar = bar,
        newsText = news?.let { com.stepup.android.push.rewardNewsText(context, it) },
        onNewsSeen = { viewModel.consumeRewardNews() },
        goalNotice = goalNotice && bar != null,
        onGoalNotice = { viewModel.dismissGoalRuleNotice(); goalSheet = true },
        overnightRun = overnightRun,
    )

    if (goalSheet) {
        // 목표 바꾸기 — 설정과 같은 시트(바꾼 목표는 오늘 달렸으면 내일부터). 닫으면 서버 상태(내일 목표)를 다시 읽는다
        com.stepup.android.ui.screens.settings.DailyGoalSheet(onDismiss = { goalSheet = false; viewModel.refreshDaily(force = true) })
    }

    if (showDetails) {
        // E01 — 오늘의 활동: 걸음 · 목표 · 남은 걸음 · 거리 · 러닝 시간 · 오늘의 SUP(서버 확인 값). 예전 상세 기록
        // (하루 전체 걸음 · 7일 걸음 · 소식 · 에너지)은 지우지 않고 아래 "더 보기"에 둔다
        com.stepup.android.ui.components.RunSheet(onDismiss = { showDetails = false }, modifier = Modifier.testTag("home-details-sheet")) {
            val body: @Composable () -> Unit = {
                TodayActivity(
                    steps = state.todaySteps, goal = state.goal, loaded = state.loaded, hasPermission = hasPermission,
                    km = if (state.loaded) RewardEconomyKm(state.todaySteps) else null,
                    runSec = runSec, earned = earned, pending = pending > 0,
                    onRequestPermission = { requestActivity() },
                    bar = bar, daily = daily,
                    onChangeGoal = { showDetails = false; goalSheet = true },
                )
                Column(
                    Modifier.fillMaxWidth()
                        .then(if (bar == null) Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState()) else Modifier)
                        .testTag("home-details"),
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
                    // 하루 전체 걸음(폰 만보기) — 오늘의 보상이 있으면 보상은 러닝 걸음으로 센다는 설명과 함께 여기(더 보기)에
                    if (bar != null) {
                        AllDayRow(state.todaySteps, state.loaded, hasPermission, onRequestPermission = { requestActivity() })
                    }
                    RecordWeek(state.week, state.todaySteps, state.loaded)
                    ShortcutButton(
                        icon = Icons.AutoMirrored.Filled.Article,
                        label = stringResource(R.string.home_shortcut_news),
                        subtitle = stringResource(R.string.home_shortcut_news_sub),
                        onClick = { showDetails = false; onOpenNews() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    EnergyLine(earnableSteps = state.earnableSteps, ready = state.loaded && state.energyKnown && state.maxEnergy > 0)
                }
            }
            if (bar != null) {
                // 오늘의 보상(0118)이 있으면 시트가 길어진다 — 시트 안 전체를 넘긴다(작은 화면 · 큰 글씨)
                Column(Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState())) { body() }
            } else {
                body()
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
 * 러닝 탭의 몸통(2026-11-24 재디자인 01_러닝홈) — 상태 없이 그린다. 머리(로고 · 알림)와 하단 탭은 공통 틀이 그린다.
 * 위부터 오늘 러닝 걸음 카드(오른쪽 캐릭터) → 회색 한 줄 안내(목표 바뀜 · 새로 받은 보상) → 사진 카드(위에 GPS 글자) →
 * 오늘 거리 · 적립 · 이번 주 러닝 한 줄 → 러닝 시작(이 화면의 주 행동, 늘 아래에). 화면이 낮거나 글자가 크면 본문이 넘어간다.
 */
@Composable
internal fun HomeContent(
    loaded: Boolean,
    todaySteps: Int,
    goal: Int,
    hasPermission: Boolean,
    firstHome: Boolean,
    running: Boolean,
    largeText: Boolean,
    earned: Double?,
    km: Double?,
    weekRuns: Int?,
    onStartRun: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenDetails: () -> Unit,
    photoIndex: Int = 0,
    gps: GpsReadiness = GpsReadiness.WAITING,
    /** 오늘의 보상 막대(서버 0118) — null 이면 예전 모습(하루 전체 걸음 · 목표 막대) */
    bar: com.stepup.android.domain.DailyBar? = null,
    /** 러닝이 올라간 뒤 새로 받은 보상 한 줄 — 서버가 받았다고 한 것만 */
    newsText: String? = null,
    onNewsSeen: () -> Unit = {},
    /** 기존 사용자에게 한 번 — 목표가 러닝 걸음 기준으로 바뀌었다 */
    goalNotice: Boolean = false,
    onGoalNotice: () -> Unit = {},
    /** 가장 최근 러닝이 어젯밤 시작해 오늘 끝났다 — 막대 아래 "어제 걸음으로 셌어요"(오늘만) */
    overnightRun: Boolean = false,
) {
    // 본문은 넘기고, 이 화면의 주 행동(러닝 시작)은 늘 아래에 보인다(작은 화면 · 큰 글씨에서도)
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = StepUpUi.Gutter)
                .testTag("home-content"),
        ) {
            Spacer(Modifier.height(if (largeText) 6.dp else 10.dp))
            StepsHero(
                todaySteps = todaySteps, goal = goal, loaded = loaded,
                hasPermission = hasPermission, firstHome = firstHome,
                onRequestPermission = onRequestPermission, onOpenDetails = onOpenDetails,
                bar = bar, overnightRun = overnightRun,
            )
            if (newsText != null) {
                // 새로 받은 보상 — 한 번 보이고 잠시 뒤 걷힌다(누르면 바로)
                LaunchedEffect(newsText) {
                    delay(12_000)
                    onNewsSeen()
                }
                HomeNoteLine(newsText, stringResource(R.string.home_activity_ok), onNewsSeen, "home-reward-news")
            } else if (goalNotice) {
                HomeNoteLine(
                    stringResource(R.string.dr_goal_notice_title), stringResource(R.string.dr_goal_notice_action), onGoalNotice,
                    "home-goal-notice",
                )
            }
            Spacer(Modifier.height(if (newsText != null || goalNotice) 6.dp else 14.dp))
            RunPhotoCard(photoIndex, gps = gps)
            Spacer(Modifier.height(14.dp))
            HomeSummaryLine(km, earned, weekRuns)
            Spacer(Modifier.height(16.dp))
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = StepUpUi.Gutter).padding(top = 8.dp, bottom = 12.dp)) {
            // 이 화면의 주 행동 — 블루 알약(시안 "러닝 시작 →"). 누르면 러닝 시작 메뉴(U01), 달리는 중이면 그 러닝
            PrimaryButton(
                stringResource(if (running) R.string.home_continue_run else R.string.home_start_run), onStartRun,
                modifier = Modifier.fillMaxWidth().guideTarget(GuideTour.Targets.HOME_START_RUN).testTag("home-start-run"),
            )
        }
    }
}

/** 걸음 카드 밑 회색 한 줄 + 오른쪽 글자 버튼(목표 바뀜 "다시 고르기" · 새로 받은 보상 "확인"). 버튼 태그는 "$tag-action" */
@Composable
private fun HomeNoteLine(text: String, action: String, onAction: () -> Unit, tag: String) {
    val t = runTone()
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp).testTag(tag), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text, style = runTextStyle(13.5.sp, t.label, FontWeight.Medium, 1.35f), maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(10.dp)).feedbackClickable(onClick = onAction)
                .padding(horizontal = 6.dp).testTag("$tag-action"),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(action, style = runTextStyle(14.5.sp, t.cobaltText, FontWeight.Bold), maxLines = 1, softWrap = false)
        }
    }
}

/** 사진 카드 밑 한 줄 — 오늘 거리 · 적립 · 이번 주 러닝(숫자만 굵게). 모르는 값은 "—" */
@Composable
private fun HomeSummaryLine(km: Double?, earned: Double?, weekRuns: Int?) {
    val t = runTone()
    val kmText = km?.let { "%.2f".format(it) } ?: "—"
    val supText = earned?.let { com.stepup.android.ui.components.formatSupDown(it, 2) } ?: "—"
    val runsText = weekRuns?.toString() ?: "—"
    Text(
        boldArgs(stringResource(R.string.rd_a_home_summary, "\u0001", "\u0002", "\u0003"), listOf(kmText, supText, runsText), t.text),
        style = runTextStyle(15.sp, t.label, FontWeight.Medium, 1.35f),
        maxLines = 2, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp).testTag("home-stats"),
    )
}

/** 틀 글([template] 안 \u0001 · \u0002 · \u0003 자리)에 값을 넣고 그 값만 굵게 · [color] 로 */
private fun boldArgs(template: String, values: List<String>, color: androidx.compose.ui.graphics.Color): AnnotatedString =
    buildAnnotatedString {
        var rest = template
        while (rest.isNotEmpty()) {
            val at = rest.indexOfFirst { it in '\u0001'..'\u0003' }
            if (at < 0) {
                append(rest)
                break
            }
            append(rest.substring(0, at))
            withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) { append(values[rest[at] - '\u0001']) }
            rest = rest.substring(at + 1)
        }
    }

/** 오늘 걸음으로 잰 거리(km) — 예전 홈과 같은 걸음 기준 */
private fun RewardEconomyKm(steps: Int): Double = com.stepup.android.domain.RewardEconomy.distanceMeters(steps) / 1000.0

// ── 오늘 걸음 ──────────────────────────────────────────────────

/** 홈 걸음 카드 — 흰 카드(어두운 테마 panel) · 모서리 22. 누르면 오늘의 활동(E01) */
@Composable
private fun HomeCard(onTap: (() -> Unit)?, modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val t = runTone()
    val shape = RoundedCornerShape(22.dp)
    val detailsLabel = stringResource(R.string.home_open_details)
    Column(
        modifier.fillMaxWidth().clip(shape).background(t.panel, shape).border(1.dp, t.panelEdge, shape)
            .then(
                if (onTap != null) {
                    Modifier.feedbackClickable(role = Role.Button, onClick = onTap)
                        .semantics { onClick(label = detailsLabel) { onTap(); true } }
                } else {
                    Modifier
                },
            )
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 16.dp),
        content = content,
    )
}

/** 걸음 카드 오른쪽 캐릭터 — 목표를 채운 날은 mascot_finish, 아니면 mascot_go. 높이만 정하고 비율 그대로 */
@Composable
private fun HomeMascot(done: Boolean) {
    androidx.compose.foundation.Image(
        painterResource(if (done) R.drawable.mascot_finish else R.drawable.mascot_go), contentDescription = null,
        modifier = Modifier.height(54.dp).testTag(if (done) "home-mascot-finish" else "home-mascot-go"),
        contentScale = ContentScale.Fit,
    )
}

/** 큰 걸음 숫자(올라가며 바뀔 때 살짝 튄다) + "/ 목표 걸음" */
@Composable
private fun HeroNumber(value: String, popKey: Int, goalText: String) {
    val t = runTone()
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            value,
            style = StepUpUi.type.numberHero.copy(fontSize = 56.sp, lineHeight = 58.sp, letterSpacing = .005.em),
            color = t.text, maxLines = 1, softWrap = false,
            modifier = Modifier.alignByBaseline().popOnChange(popKey).testTag("home-steps-value"),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            goalText,
            style = runTextStyle(19.sp, t.label, FontWeight.Medium, 1.2f),
            maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.alignByBaseline().testTag("home-goal"),
        )
    }
}

/**
 * 오늘 걸음 카드 — 작은 "오늘 러닝 걸음" · 큰 숫자 / 목표 · 오른쪽 캐릭터 · 막대. 누르면 오늘의 활동(E01).
 * 걸음 권한이 없으면 숫자 대신 권한 줄, 첫 러닝 홈이면 첫 안내 두 줄(시작·로그인·첫 사용 v1 시안 11).
 */
@Composable
private fun StepsHero(
    todaySteps: Int,
    goal: Int,
    loaded: Boolean,
    hasPermission: Boolean,
    firstHome: Boolean,
    onRequestPermission: () -> Unit,
    onOpenDetails: () -> Unit,
    bar: com.stepup.android.domain.DailyBar? = null,
    overnightRun: Boolean = false,
) {
    val t = runTone()
    if (firstHome) {
        // 첫 러닝 홈의 두 줄은 설명이라 누를 곳이 없다 — 걸음이 생기면 걸음 칸이 오늘의 활동을 연다
        HomeCard(onTap = null, modifier = Modifier.testTag("home-steps")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.onb_home_first_title), style = runTextStyle(24.sp, t.text, FontWeight.Bold, 1.3f),
                        modifier = Modifier.testTag("home-headline"),
                    )
                    Text(
                        stringResource(R.string.onb_home_first_body), style = runTextStyle(14.sp, t.label, FontWeight.Medium, 1.45f),
                        modifier = Modifier.padding(top = 6.dp).testTag("home-first-run"),
                    )
                }
                Spacer(Modifier.width(10.dp))
                HomeMascot(done = false)
            }
        }
        return
    }
    if (bar != null) {
        RewardsHero(
            bar = bar, allDaySteps = todaySteps, loaded = loaded, hasPermission = hasPermission,
            onRequestPermission = onRequestPermission, onOpenDetails = onOpenDetails,
            overnightRun = overnightRun,
        )
        return
    }
    // 예전 서버(오늘의 보상 없음) — 이 숫자는 폰이 센 하루 전체 걸음이다
    HomeCard(onTap = onOpenDetails, modifier = Modifier.testTag("home-steps")) {
        Text(
            stringResource(R.string.dr_all_day_label), style = runTextStyle(14.5.sp, t.label, FontWeight.Medium, 1.3f),
            modifier = Modifier.semantics { heading() },
        )
        if (!hasPermission) {
            PermissionStrip(onClick = onRequestPermission)
            return@HomeCard
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                val shown = countUpFloat(if (loaded) todaySteps.toFloat() else 0f).roundToInt()
                HeroNumber(
                    if (loaded) "%,d".format(shown) else "—", todaySteps / 1000,
                    stringResource(R.string.d26_steps_goal, "%,d".format(goal)),
                )
            }
            Spacer(Modifier.width(8.dp))
            HomeMascot(done = loaded && goal > 0 && todaySteps >= goal)
        }
        Spacer(Modifier.height(12.dp))
        StepsBar(if (loaded && goal > 0) todaySteps.toFloat() / goal else 0f, 0f, height = 8.dp)
    }
}

// ── 오늘의 활동(E01) ────────────────────────────────────────────

/**
 * 오늘의 활동 시트(2026-11-24 재디자인 02) — 가운데 정렬 걸음 카드(큰 숫자 · 막대 · 눈금 세 칸 · 다음 보상 · 거리 · 러닝 시간 · 오늘 SUP),
 * 아래 이번 주 · 스텝 서지 · 연속 목록 카드, "보상은 확인 후 반영돼요" · 목표 바꾸기.
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
    /** 오늘의 보상(서버 0118) — 있으면 걸음 칸이 러닝 걸음 막대 · 이번 주 · 연속 · 목표 바꾸기가 된다 */
    bar: com.stepup.android.domain.DailyBar? = null,
    daily: com.stepup.android.domain.DailyRewards? = null,
    onChangeGoal: () -> Unit = {},
) {
    val t = runTone()
    Column(Modifier.fillMaxWidth().testTag("home-activity")) {
        Text(
            stringResource(R.string.home_activity_title), style = runTextStyle(22.sp, t.text, FontWeight.Bold, 1.25f),
            modifier = Modifier.padding(end = 48.dp).semantics { heading() },
        )
        Spacer(Modifier.height(14.dp))
        val kmText = km?.let { "%.1f".format(it) } ?: "—"
        val supText = earned?.let { com.stepup.android.ui.components.formatSupDown(it, 2) } ?: "—"
        val summary = stringResource(R.string.rd_a_activity_line, kmText, "${runSec / 60}", supText)
        if (bar != null) {
            TodayRewards(bar = bar, daily = daily, summary = summary, pending = pending, onChangeGoal = onChangeGoal)
            return@Column
        }
        SheetCard {
            Text(
                stringResource(R.string.dr_all_day_label), style = runTextStyle(14.5.sp, t.label, FontWeight.Medium),
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            if (!hasPermission) {
                PermissionStrip(onClick = onRequestPermission)
            } else {
                Spacer(Modifier.height(2.dp))
                CenterNumber(if (loaded) "%,d".format(steps) else "—", "/ " + "%,d".format(goal), "home-activity-steps")
                Spacer(Modifier.height(14.dp))
                val fraction = if (goal > 0) (steps.toFloat() / goal).coerceIn(0f, 1f) else 0f
                StepsBar(if (loaded) fraction else 0f, 0f, height = 6.dp, modifier = Modifier.testTag("home-goal-percent"))
                Spacer(Modifier.height(12.dp))
                Text(
                    when {
                        !loaded -> ""
                        steps >= goal -> stringResource(R.string.home_activity_done)
                        else -> stringResource(R.string.home_activity_left, "%,d".format(goal - steps))
                    },
                    style = runTextStyle(15.sp, t.cobaltText, FontWeight.Bold), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
            }
            SummaryLine(summary, pending)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.home_activity_sup_note), style = runTextStyle(13.sp, t.label, FontWeight.Medium),
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

/** 시트 안 카드 — 시트 면 위에 둘레 선 하나(시안: 흰 시트 위 흰 카드 · 어두운 테마 남색) */
@Composable
private fun SheetCard(modifier: Modifier = Modifier, padded: Boolean = true, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val t = runTone()
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(t.panel, shape).border(1.dp, t.panelEdge, shape)
            .padding(horizontal = 18.dp, vertical = if (padded) 18.dp else 0.dp),
        content = content,
    )
}

/** 가운데 큰 숫자 + 작은 "/ 목표" */
@Composable
private fun CenterNumber(value: String, goalText: String, tag: String) {
    val t = runTone()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
        Text(
            value, style = StepUpUi.type.numberHero.copy(fontSize = 56.sp, lineHeight = 58.sp), color = t.text,
            maxLines = 1, softWrap = false, modifier = Modifier.alignByBaseline().testTag(tag),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            goalText, style = runTextStyle(20.sp, t.label, FontWeight.Medium, 1.2f), maxLines = 1, softWrap = false,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

/** 카드 맨 아래 — 구분선 뒤 "0.0km · 0분 · 오늘 +0.00 SUP"(서버 값), 올리는 중이면 옆에 "확인 중" */
@Composable
private fun SummaryLine(summary: String, pending: Boolean, head: (@Composable () -> Unit)? = null) {
    val t = runTone()
    Spacer(Modifier.height(16.dp))
    PlainDivider()
    Spacer(Modifier.height(14.dp))
    head?.invoke()
    Row(
        Modifier.fillMaxWidth().testTag("home-activity-sup"),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            summary, style = runTextStyle(14.5.sp, t.label, FontWeight.Medium).copy(fontFeatureSettings = "tnum"),
            textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.weight(1f, fill = false).testTag("home-activity-sup-value"),
        )
        if (pending) {
            Spacer(Modifier.width(8.dp))
            com.stepup.android.ui.components.RunChip(stringResource(R.string.home_activity_checking),
                modifier = Modifier.testTag("home-activity-pending"))
        }
    }
}

// ── 오늘의 보상(서버 0118) ───────────────────────────────────────

/**
 * 홈 위 — 오늘의 보상 카드(docs/forge-xp-daily-spec-2026-11-19.md 5.3). 큰 숫자는 서버가 확인한 오늘 러닝 걸음, 오른쪽 위에 작게 하루 전체 걸음
 * (걸음 권한이 있을 때만 — 막대는 권한이 없어도 보인다). 눈금(상자 · 목표)은 서버가 주고 받은 것만 ✓, 아래 파란 한 줄은 다음 보상까지 · 연속.
 * 올리는 중 · 달리는 중 걸음은 옅은 구간 + "확인 중"(눈금을 채우지 않는다). 오늘(한국 날짜) 상태를 모르면 숫자 대신 "—". 누르면 오늘의 활동.
 */
@Composable
private fun RewardsHero(
    bar: com.stepup.android.domain.DailyBar,
    allDaySteps: Int,
    loaded: Boolean,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpenDetails: () -> Unit,
    overnightRun: Boolean = false,
) {
    val t = runTone()
    HomeCard(onTap = onOpenDetails, modifier = Modifier.testTag("home-steps")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.home_steps_today), style = runTextStyle(14.5.sp, t.label, FontWeight.Medium, 1.3f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).semantics { heading() },
                    )
                }
                val shown = countUpFloat(if (bar.known) bar.rewardSteps.toFloat() else 0f).roundToInt()
                HeroNumber(
                    if (bar.known) "%,d".format(shown) else "—", bar.rewardSteps / 1000,
                    stringResource(R.string.d26_steps_goal, if (bar.goal > 0) "%,d".format(bar.goal) else "—"),
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                AllDaySteps(allDaySteps, loaded, hasPermission, onRequestPermission)
                Spacer(Modifier.height(4.dp))
                HomeMascot(done = bar.known && bar.goal > 0 && bar.rewardSteps >= bar.goal)
            }
        }
        Spacer(Modifier.height(12.dp))
        RewardTrack(bar)
        Spacer(Modifier.height(8.dp))
        RewardLine(bar)
        // 보상 걸음에 들지 않은 걸음 · 어젯밤 러닝 — 그럴 때만 작은 한 줄씩(오늘 상태를 알 때만)
        if (com.stepup.android.domain.DailyRewardsBar.offRunNote(bar, allDaySteps, allDayKnown = loaded && hasPermission)) {
            // 서버 0119(닳은 뒤에도 적립하는 규칙)부터 보상 걸음은 밖에서 경로가 확인된 걸음 모두다 — 신발이 닳았어도 센다.
            // 그 전 규칙이면 예전 글("닳지 않은 신발로 걸은 걸음만")
            val anyShoeCounts = com.stepup.android.ui.screens.items.rememberShoeRules().stats.earnsWhenWorn
            RewardNote(
                stringResource(if (anyShoeCounts) R.string.roots_off_run_note else R.string.dr_off_run_note),
                "home-reward-note-off-run",
            )
        }
        if (overnightRun && bar.known) RewardNote(stringResource(R.string.dr_overnight_note), "home-reward-note-overnight")
    }
}

/** 막대 아래 작은 설명 한 줄 — 누를 곳 없이 */
@Composable
private fun RewardNote(text: String, tag: String) {
    val t = runTone()
    Spacer(Modifier.height(4.dp))
    Text(text, style = runTextStyle(12.5.sp, t.label, FontWeight.Medium, 1.4f), modifier = Modifier.fillMaxWidth().testTag(tag))
}

/** 하루 전체 걸음(폰 만보기) — 작게 "하루 전체 N ›". 걸음 권한이 없으면 그 자리에 "하루 전체 걸음 보기"(누르면 권한을 묻는다) */
@Composable
private fun AllDaySteps(steps: Int, loaded: Boolean, hasPermission: Boolean, onRequestPermission: () -> Unit) {
    val t = runTone()
    if (hasPermission) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.dr_all_day, if (loaded) "%,d".format(steps) else "—"),
                style = runTextStyle(13.5.sp, t.label, FontWeight.Medium), maxLines = 1, softWrap = false,
                modifier = Modifier.testTag("home-all-day"),
            )
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = t.muted, modifier = Modifier.size(16.dp))
        }
    } else {
        Text(
            stringResource(R.string.dr_all_day_allow), style = runTextStyle(13.5.sp, t.cobaltText, FontWeight.SemiBold), maxLines = 1,
            softWrap = false,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).feedbackClickable(onClick = onRequestPermission)
                .padding(horizontal = 6.dp, vertical = 4.dp).testTag("home-permission"),
        )
    }
}

/** 막대 — 바탕 track, 진한 구간 = 서버가 확인한 걸음, 옅은 구간 = 확인 중(한 색, 그라데이션 없음) */
@Composable
private fun StepsBar(confirmed: Float, pending: Float, height: Dp, modifier: Modifier = Modifier) {
    val t = runTone()
    androidx.compose.foundation.Canvas(modifier.fillMaxWidth().height(height)) {
        val h = size.height
        val corner = androidx.compose.ui.geometry.CornerRadius(h / 2, h / 2)
        drawRoundRect(t.track, cornerRadius = corner)
        val p = pending.coerceIn(0f, 1f)
        val c = confirmed.coerceIn(0f, 1f)
        if (p > c) drawRoundRect(t.cobalt.copy(alpha = 0.3f), size = androidx.compose.ui.geometry.Size(size.width * p, h), cornerRadius = corner)
        if (c > 0f) drawRoundRect(t.cobalt, size = androidx.compose.ui.geometry.Size((size.width * c).coerceAtLeast(h), h), cornerRadius = corner)
    }
}

/**
 * 홈 막대 — 구간(0 → 상자 → 상자 → 목표)을 같은 너비로, 같은 걸음의 눈금은 하나(목표 4,000 = 둘째 상자). 진한 구간 = 서버가 확인한 걸음,
 * 옅은 구간 = 확인 중. 눈금은 흰 동그라미(상자 그림 · 목표는 SUP 동그라미), 서버가 받았다고 한 것만 파랗게 채우고 ✓.
 */
@Composable
private fun RewardTrack(bar: com.stepup.android.domain.DailyBar, modifier: Modifier = Modifier) {
    val t = runTone()
    val dot = 30.dp
    Column(modifier.fillMaxWidth().testTag("home-reward-bar")) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(dot)) {
            val r = dot / 2
            val w = (maxWidth - dot).coerceAtLeast(1.dp)
            Box(Modifier.padding(horizontal = r).align(Alignment.CenterStart)) {
                StepsBar(bar.confirmedFraction, bar.pendingFraction, height = 8.dp)
            }
            bar.slots.forEach { slot ->
                val got = slot.got && bar.known
                Box(
                    Modifier.offset(x = w * slot.position).size(dot).clip(CircleShape)
                        .background(if (got) t.cobalt else t.panel, CircleShape)
                        .then(if (got) Modifier else Modifier.border(2.dp, t.track, CircleShape)),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        got -> Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        slot.box -> Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = t.label, modifier = Modifier.size(14.dp))
                        else -> Box(Modifier.size(16.dp).clip(CircleShape).background(t.gold, CircleShape))
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        TickLabels(bar, dot)
    }
}

/**
 * 눈금 아래 글 — "걸음 · 보상"(상자 / +N SUP) 한 줄. 구간마다 같은 너비의 칸에 오른쪽으로 붙여 그 눈금에서 끝난다(눈금이 서로 가까워도
 * 글이 겹치지 않는다). 금액은 서버 값(받았으면 받은 SUP, 아니면 오늘 채우면 받을 SUP)
 */
@Composable
private fun TickLabels(bar: com.stepup.android.domain.DailyBar, dot: Dp) {
    val t = runTone()
    Row(Modifier.fillMaxWidth().padding(start = dot / 2)) {
        bar.slots.forEach { slot ->
            val reward = tickReward(slot, bar)
            val got = slot.got && bar.known
            val description = stringResource(if (got) R.string.dr_tick_got_cd else R.string.dr_tick_open_cd, "%,d".format(slot.steps), reward)
            Text(
                "%,d".format(slot.steps) + " · " + if (got) "$reward ✓" else reward,
                style = runTextStyle(12.5.sp, if (got) t.cobaltText else t.label, FontWeight.Medium, 1.3f),
                maxLines = 2, textAlign = TextAlign.End,
                modifier = Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = description }
                    .testTag("home-reward-tick-${slot.steps}"),
            )
        }
    }
}

/** 눈금의 보상 글 — 상자 · +N SUP · 둘 다(목표 4,000 = 둘째 상자). 금액을 모르면 "목표" */
@Composable
private fun tickReward(slot: com.stepup.android.domain.TickSlot, bar: com.stepup.android.domain.DailyBar): String {
    val amount = (if (slot.got) bar.goalPaid else null) ?: bar.goalBonus
    val sup = amount?.let { com.stepup.android.domain.DailyRewardsBar.supText(it) }
    return when {
        slot.box && slot.goal -> if (sup != null) stringResource(R.string.dr_tick_box_goal, sup) else stringResource(R.string.dr_tick_box_goal_plain)
        slot.goal -> if (sup != null) stringResource(R.string.dr_tick_goal, sup) else stringResource(R.string.dr_tick_goal_plain)
        else -> stringResource(R.string.dr_tick_box)
    }
}

/** 막대 아래 파란 한 줄 — 다음 보상까지 남은 걸음 · 연속, 옆에 확인 중 걸음 */
@Composable
private fun RewardLine(bar: com.stepup.android.domain.DailyBar) {
    val t = runTone()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            rewardLineText(bar), style = runTextStyle(15.sp, t.cobaltText, FontWeight.Bold, 1.35f),
            modifier = Modifier.weight(1f).semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite }
                .testTag("home-reward-line"),
        )
        if (bar.pendingSteps > 0) {
            Spacer(Modifier.width(8.dp))
            com.stepup.android.ui.components.RunChip(
                stringResource(R.string.dr_pending, "%,d".format(bar.pendingSteps)), modifier = Modifier.testTag("home-reward-pending"),
            )
        }
    }
}

@Composable
internal fun rewardLineText(bar: com.stepup.android.domain.DailyBar, firstBox: Boolean = false): String {
    if (!bar.known) return stringResource(R.string.dr_loading)
    val next = bar.next
    val head = when {
        next != null -> {
            val left = "%,d".format(next.stepsLeft)
            val sup = next.bonus?.let { com.stepup.android.domain.DailyRewardsBar.supText(it) }
            when {
                next.box && next.goal && sup != null -> stringResource(R.string.dr_next_box_goal, left, sup)
                next.goal && sup != null -> stringResource(R.string.dr_next_goal, left, sup)
                next.goal -> stringResource(R.string.dr_next_goal_plain, left)
                // 시트 — 오늘 아직 받은 상자가 없으면 "첫 상자"
                firstBox && bar.slots.none { it.got } -> stringResource(R.string.rd_a_first_box, left)
                else -> stringResource(R.string.dr_next_box, left)
            }
        }
        bar.waiting -> stringResource(R.string.dr_waiting)
        bar.allGot -> stringResource(R.string.dr_all_got)
        else -> ""
    }
    val streak = bar.streak
    val tail = when {
        streak == null -> null
        bar.goalPaid != null && streak >= 2 -> stringResource(R.string.dr_streak_today, streak)
        bar.goalPaid == null && streak >= 2 -> stringResource(R.string.dr_streak_if, streak)
        else -> null
    }
    return listOfNotNull(head.takeIf { it.isNotEmpty() }, tail).joinToString(" · ")
}

/**
 * 오늘의 활동 시트의 오늘의 보상 — 가운데 정렬 카드(오늘 러닝 걸음 · 큰 숫자 / 목표 · 막대 · 눈금 세 칸 · 다음 보상 · 거리 · 시간 · SUP),
 * 목록 카드(이번 주 상급 상자 · 스텝 서지 · 연속 러닝), "보상은 확인 후 반영돼요" · 목표 바꾸기. 모든 수는 서버 상태 그대로.
 * 하루 전체 걸음은 아래 "더 보기"에 있다([AllDayRow]).
 */
@Composable
private fun TodayRewards(
    bar: com.stepup.android.domain.DailyBar,
    daily: com.stepup.android.domain.DailyRewards?,
    summary: String,
    pending: Boolean,
    onChangeGoal: () -> Unit,
) {
    val t = runTone()
    SheetCard {
        Text(
            stringResource(R.string.home_steps_today), style = runTextStyle(14.5.sp, t.label, FontWeight.Medium),
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        CenterNumber(
            if (bar.known) "%,d".format(bar.rewardSteps) else "—", "/ " + if (bar.goal > 0) "%,d".format(bar.goal) else "—",
            "home-activity-steps",
        )
        bar.goalNext?.let { next ->
            Text(
                stringResource(R.string.dr_goal_next, "%,d".format(next)), style = runTextStyle(13.sp, t.cobaltText, FontWeight.SemiBold),
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 2.dp).testTag("home-activity-goal-next"),
            )
        }
        Spacer(Modifier.height(16.dp))
        // 막대 위에는 아무것도 올리지 않는다 — 눈금은 아래 세 칸
        StepsBar(bar.confirmedFraction, bar.pendingFraction, height = 6.dp)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth()) {
            bar.slots.forEach { slot -> SheetTick(slot, bar, Modifier.weight(1f)) }
        }
        SummaryLine(summary, pending) {
            Text(
                rewardLineText(bar, firstBox = true), style = runTextStyle(16.sp, t.cobaltText, FontWeight.Bold),
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().testTag("home-activity-next"),
            )
        }
    }

    // 이번 주 — 주간 상급 상자 · 스텝 서지, 연속(서버 값)
    if (daily != null && bar.known) {
        val rows = buildList<@Composable () -> Unit> {
            if (daily.weekTarget > 0) add {
                PlainListRow(
                    stringResource(R.string.rd_a_week_box),
                    value = if (daily.weekGot) stringResource(R.string.boxes_got)
                    else stringResource(R.string.boxes_steps_progress, "%,d".format(daily.stepsWeek.coerceAtMost(daily.weekTarget)), "%,d".format(daily.weekTarget)),
                    progress = if (daily.weekGot) 1f else (daily.stepsWeek.toFloat() / daily.weekTarget).coerceIn(0f, 1f),
                    barColor = t.cobalt,
                    modifier = Modifier.semantics(mergeDescendants = true) {}.testTag("home-activity-week"),
                )
            }
            daily.surge?.takeIf { it.target > 0 }?.let { surge ->
                add {
                    PlainListRow(
                        stringResource(R.string.rd_a_surge, com.stepup.android.domain.DailyRewardsBar.supText(surge.reward)),
                        value = if (surge.claimed) stringResource(R.string.boxes_got)
                        else stringResource(R.string.boxes_steps_progress, "%,d".format(surge.steps.coerceAtMost(surge.target)), "%,d".format(surge.target)),
                        progress = surge.fraction, barColor = t.gold,
                        modifier = Modifier.semantics(mergeDescendants = true) {}.testTag("home-activity-surge"),
                    )
                }
            }
            daily.streak?.let { streak -> add { StreakRow(daily, streak) } }
        }
        if (rows.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            SheetCard(padded = false) {
                rows.forEachIndexed { i, row ->
                    if (i > 0) PlainDivider()
                    row()
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.home_activity_sup_note), style = runTextStyle(13.5.sp, t.label, FontWeight.Medium),
            maxLines = 2, modifier = Modifier.weight(1f).padding(start = 4.dp),
        )
        Box(
            Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(10.dp)).feedbackClickable(onClick = onChangeGoal)
                .padding(horizontal = 6.dp).testTag("home-activity-change-goal"),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(stringResource(R.string.dr_goal_change), style = runTextStyle(15.sp, t.cobaltText, FontWeight.Bold), maxLines = 1, softWrap = false)
        }
    }
}

/** 시트의 눈금 한 칸 — 상자 그림(또는 SUP 동그라미) · "N보" · 보상. 받은 칸은 파란 글자 + ✓ */
@Composable
private fun SheetTick(slot: com.stepup.android.domain.TickSlot, bar: com.stepup.android.domain.DailyBar, modifier: Modifier) {
    val t = runTone()
    val reward = tickReward(slot, bar)
    val got = slot.got && bar.known
    val description = stringResource(if (got) R.string.dr_tick_got_cd else R.string.dr_tick_open_cd, "%,d".format(slot.steps), reward)
    Column(
        modifier.semantics(mergeDescendants = true) { contentDescription = description }.testTag("home-activity-tick-${slot.steps}"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.height(30.dp), contentAlignment = Alignment.Center) {
            if (slot.box) {
                androidx.compose.foundation.Image(
                    painterResource(R.drawable.draw_box_closed_free), contentDescription = null,
                    modifier = Modifier.height(28.dp), contentScale = ContentScale.Fit,
                )
            } else {
                Box(Modifier.size(26.dp).clip(CircleShape).background(t.gold, CircleShape), contentAlignment = Alignment.Center) {
                    Text("S", style = runTextStyle(13.sp, Color.White, FontWeight.Bold, 1f))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.rd_a_steps, "%,d".format(slot.steps)),
            style = runTextStyle(15.sp, if (got) t.cobaltText else t.text, FontWeight.Bold, 1.25f).copy(fontFeatureSettings = "tnum"),
            maxLines = 1, softWrap = false, textAlign = TextAlign.Center,
        )
        Text(
            if (got) "$reward ✓" else reward, style = runTextStyle(13.sp, if (got) t.cobaltText else t.label, FontWeight.Medium, 1.3f),
            maxLines = 2, textAlign = TextAlign.Center,
        )
    }
}

/** 연속 러닝 — "7일 중 N일" · 최근 7일 점(오늘은 테) · 연속 상자까지 · 가장 긴 연속(서버 값) */
@Composable
private fun StreakRow(daily: com.stepup.android.domain.DailyRewards, streak: Int) {
    val t = runTone()
    val paidToday = daily.goalPaid != null
    val inWeek = com.stepup.android.domain.DailyRewardsBar.streakInWeek(streak, paidToday = paidToday)
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val today = daily.day
    val todayLabel = stringResource(R.string.rd_a_today)
    Column(
        Modifier.fillMaxWidth().padding(vertical = 14.dp).semantics(mergeDescendants = true) {}.testTag("home-activity-streak"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.rd_a_streak_title), style = runTextStyle(15.5.sp, t.text, FontWeight.SemiBold, 1.3f),
                modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.dr_streak_week, inWeek), style = runTextStyle(16.sp, t.text, FontWeight.Bold, 1.2f),
                maxLines = 1, softWrap = false)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            // 지난 6일 + 오늘. 채운 날 = 연속으로 이어 온 날(오늘 채웠으면 오늘부터, 아니면 어제부터 거꾸로)
            for (back in 6 downTo 0) {
                val day = today.minusDays(back.toLong())
                val filled = if (paidToday) back < inWeek else back in 1..inWeek
                val isToday = back == 0
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(12.dp).clip(CircleShape).then(
                            when {
                                filled -> Modifier.background(t.cobalt, CircleShape)
                                isToday -> Modifier.border(2.dp, t.cobaltText, CircleShape)
                                else -> Modifier.background(t.track, CircleShape)
                            },
                        ),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (isToday) todayLabel else day.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, locale),
                        style = runTextStyle(12.5.sp, if (isToday) t.cobaltText else t.label, FontWeight.SemiBold, 1.2f),
                        maxLines = 1, softWrap = false,
                    )
                }
            }
        }
        daily.streakBoxIn?.let {
            Spacer(Modifier.height(10.dp))
            Text(
                if (it <= 0) stringResource(R.string.dr_streak_box_today) else stringResource(R.string.dr_streak_box_in, it),
                style = runTextStyle(13.5.sp, t.label, FontWeight.Medium),
            )
        }
        daily.streakBest?.takeIf { it > 0 }?.let { best ->
            Text(stringResource(R.string.dr_streak_best, best), style = runTextStyle(13.sp, t.muted, FontWeight.Medium))
        }
    }
}

/** 더 보기의 하루 전체 걸음 — 폰이 센 걸음 · 설명(보상은 러닝 걸음으로 센다) */
@Composable
private fun AllDayRow(steps: Int, loaded: Boolean, hasPermission: Boolean, onRequestPermission: () -> Unit) {
    val t = runTone()
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().testTag("home-activity-all-day"), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.dr_all_day_label), style = runTextStyle(15.sp, t.text, FontWeight.SemiBold),
                modifier = Modifier.weight(1f))
            if (hasPermission) {
                Text(if (loaded) "%,d".format(steps) else "—", style = runTextStyle(15.sp, t.label, FontWeight.SemiBold), maxLines = 1, softWrap = false)
            } else {
                AllDaySteps(steps, loaded, hasPermission = false, onRequestPermission = onRequestPermission)
            }
        }
        Text(stringResource(R.string.dr_all_day_note), style = runTextStyle(12.sp, t.muted), modifier = Modifier.fillMaxWidth())
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
    val c = StepUpUi.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(StepUpUi.FieldRadius))
            .background(c.fill)
            .feedbackClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("home-permission"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, tint = c.accentText, modifier = Modifier.size(20.dp))
        Text(
            text = stringResource(R.string.perm_allow),
            modifier = Modifier.weight(1f),
            style = StepUpUi.type.rowTitle,
            color = c.label,
        )
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = c.tertiaryLabel, modifier = Modifier.size(18.dp))
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

