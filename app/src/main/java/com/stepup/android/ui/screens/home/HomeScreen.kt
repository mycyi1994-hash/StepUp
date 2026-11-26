package com.stepup.android.ui.screens.home

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.ui.StepPermissions
import com.stepup.android.ui.background.RunBackground
import com.stepup.android.ui.background.Spot
import com.stepup.android.ui.background.Spots
import com.stepup.android.ui.background.TimeOfDay
import com.stepup.android.ui.background.WeatherLayer
import com.stepup.android.ui.background.currentWeather
import com.stepup.android.ui.background.rememberSpotPhoto
import com.stepup.android.ui.components.AdaptiveNumber
import com.stepup.android.ui.components.CircleIconButton
import com.stepup.android.ui.components.PhotoCard
import com.stepup.android.ui.components.PrimaryButton
import com.stepup.android.ui.components.RoundControl
import com.stepup.android.ui.components.ShortcutButton
import com.stepup.android.ui.components.StatItem
import com.stepup.android.ui.components.StatRow
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
import kotlinx.coroutines.launch

/*
 * 러닝 탭 첫 화면 — UI 최종 통합안(2026-10-25) `home`. 아치 · 신발 전시장 없음(클라이언트 요청).
 * 위부터 오늘 걸음(큰 숫자 / 목표) → 둥근 사진 카드(자주 달리는 곳 · 시간대, 좌우 유리 버튼으로 장소 넘기기) →
 * 오늘 SUP · 거리 · 이번 주 러닝 수 → 둥근 러닝 시작(이 화면의 주 행동, 빛번짐 없음).
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
    /** 지금 신은 신발 카드(v12 시제품) — 이름 › 신발 상세 · 러닝 아이템 › 아이템 탭 · 신발이 없으면 신발 탭 · 스댕이 줄 */
    onOpenShoe: (Long) -> Unit = {},
    onOpenItems: () -> Unit = {},
    onOpenPup: (() -> Unit)? = null,
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
    var repairShoeId by rememberSaveable { mutableStateOf<Long?>(null) }
    val repairScope = androidx.compose.runtime.rememberCoroutineScope()
    val bag by com.stepup.android.core.ServiceLocator.itemsRepository.bag.collectAsStateWithLifecycle()
    val picks by com.stepup.android.core.ServiceLocator.itemsRepository.picks.collectAsStateWithLifecycle()
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
        shoe = state.equipped,
        runItem = HomeRunItem.of(bag, picks),
        onOpenShoe = { state.equipped?.let { onOpenShoe(it.id) } },
        onRepair = { repairShoeId = state.equipped?.id },
        onOpenItems = onOpenItems,
        onOpenShoes = onOpenCustomize,
        onOpenPup = onOpenPup,
    )

    // 수리 — 신발 탭 · 러닝 점검과 같은 시트(ShoeCareStore). 여는 것만으로 수리하지 않는다
    repairShoeId?.let { id ->
        val careStore = com.stepup.android.ui.screens.items.ShoeCareStore.shared
        val repairs by careStore.repair.collectAsStateWithLifecycle()
        val repairShoe = state.equipped?.takeIf { it.id == id }
        LaunchedEffect(id) { if (careStore.repair.value[id] == null) careStore.openRepair(id) }
        val closeRepair = {
            repairShoeId = null
            careStore.closeRepair(id)
        }
        if (repairShoe == null || session.isActive) {
            LaunchedEffect(Unit) { closeRepair() }
        } else {
            com.stepup.android.ui.screens.items.ShoeRepairSheet(
                shoe = repairShoe,
                phase = repairs[id] ?: com.stepup.android.ui.screens.items.RepairPhase.Loading,
                onConfirm = { careStore.confirmRepair(id) },
                onRecheck = { careStore.recheckRepair(id) },
                onReload = { careStore.reloadRepair(id) },
                onSignIn = if (!com.stepup.android.core.ServiceLocator.serverEconomyOn) null else ({
                    repairScope.launch { runCatching { com.stepup.android.ui.components.returnToSignIn(context) } }
                }),
                onClose = closeRepair,
            )
        }
    }

    if (goalSheet) {
        // 목표 바꾸기 — 설정과 같은 시트(바꾼 목표는 오늘 달렸으면 내일부터). 닫으면 서버 상태(내일 목표)를 다시 읽는다
        com.stepup.android.ui.screens.settings.DailyGoalSheet(onDismiss = { goalSheet = false; viewModel.refreshDaily(force = true) })
    }

    if (showDetails) {
        // E01 — 오늘의 활동: 걸음 · 목표 · 남은 걸음 · 거리 · 러닝 시간 · 오늘의 SUP(서버 확인 값). 예전 상세 기록
        // (7일 걸음 · 소식 · 에너지)은 지우지 않고 아래 "더 보기"에 둔다
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
 * 러닝 탭의 몸통(2026-10-26 시안 `run`) — 상태 없이 그린다. 머리(로고 · 알림)와 하단 탭은 공통 틀이 그린다.
 * 혼자 달리기만 둔다(크루 · 번개러닝은 함께 뛰기 탭). 화면이 낮거나 글자가 크면 본문이 넘어간다.
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
    /** 오늘의 보상 막대(서버 0118) — null 이면 예전 모습(하루 전체 걸음 · 목표 고리) */
    bar: com.stepup.android.domain.DailyBar? = null,
    /** 러닝이 올라간 뒤 새로 받은 보상 한 줄 — 서버가 받았다고 한 것만 */
    newsText: String? = null,
    onNewsSeen: () -> Unit = {},
    /** 기존 사용자에게 한 번 — 목표가 러닝 걸음 기준으로 바뀌었다 */
    goalNotice: Boolean = false,
    onGoalNotice: () -> Unit = {},
    /** 가장 최근 러닝이 어젯밤 시작해 오늘 끝났다 — 막대 아래 "어제 걸음으로 셌어요"(오늘만) */
    overnightRun: Boolean = false,
    /** 지금 신은 신발(v12 시제품 2026-11-26) — 사진 카드 · 숫자 줄 자리. null 이면 신발 탭으로 가는 한 줄 */
    shoe: com.stepup.android.domain.Sneaker? = null,
    runItem: HomeRunItem? = null,
    onOpenShoe: () -> Unit = {},
    onRepair: () -> Unit = {},
    onOpenItems: () -> Unit = {},
    onOpenShoes: () -> Unit = {},
    /** 스댕이 키우기 줄(기획 전 — 테스트 판에서만) */
    onOpenPup: (() -> Unit)? = null,
) {
    // 본문은 넘기고, 이 화면의 주 행동(러닝 시작)은 늘 아래에 보인다(작은 화면 · 큰 글씨에서도)
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = StepUpUi.Gutter)
                .testTag("home-content"),
        ) {
            Spacer(Modifier.height(if (largeText) 8.dp else 14.dp))
            // 제목 오른쪽에 GPS 준비(v12 시제품 — 사진 카드 아래에서 올렸다)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RunTabTitle(stringResource(R.string.d26_title_run), Modifier.weight(1f))
                GpsLine(gps)
            }
            Spacer(Modifier.height(if (largeText) 14.dp else 22.dp))
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
                Spacer(Modifier.height(10.dp))
                com.stepup.android.ui.components.RunNotice(
                    newsText, kind = com.stepup.android.ui.components.RunNoticeKind.Info,
                    action = stringResource(R.string.home_activity_ok), onAction = onNewsSeen, tag = "home-reward-news",
                )
            } else if (goalNotice) {
                Spacer(Modifier.height(10.dp))
                com.stepup.android.ui.components.RunNotice(
                    stringResource(R.string.dr_goal_notice_title), kind = com.stepup.android.ui.components.RunNoticeKind.Info,
                    action = stringResource(R.string.dr_goal_notice_action), onAction = onGoalNotice, tag = "home-goal-notice",
                )
            }
            // 지금 신은 신발 · 스댕이 키우기(v12 시제품 2026-11-26) — 예전 사진 카드 · 오늘 거리 / 적립 / 이번 주 러닝 줄을 대신한다.
            // 오늘 거리 · 적립은 걸음 칸을 누르면 여는 오늘의 활동 시트에 그대로 있다
            Spacer(Modifier.height(28.dp))
            HomeShoeSection(
                shoe = shoe, runItem = runItem, onOpenShoe = onOpenShoe, onRepair = onRepair,
                onOpenItems = onOpenItems, onOpenShoes = onOpenShoes,
            )
            if (onOpenPup != null) {
                Spacer(Modifier.height(10.dp))
                com.stepup.android.ui.screens.pup.PupEntryRow(onOpenPup)
            }
            Spacer(Modifier.height(16.dp))
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = StepUpUi.Gutter).padding(top = 8.dp, bottom = 12.dp)) {
            // 이 화면의 주 행동 — 블루 알약(시안 "러닝 시작 ▷"). 누르면 러닝 시작 메뉴(U01), 달리는 중이면 그 러닝
            PrimaryButton(
                stringResource(if (running) R.string.home_continue_run else R.string.home_start_run), onStartRun,
                modifier = Modifier.fillMaxWidth().guideTarget(GuideTour.Targets.HOME_START_RUN).testTag("home-start-run"),
            )
        }
    }
}

/** 오늘 걸음으로 잰 거리(km) — 예전 홈과 같은 걸음 기준 */
private fun RewardEconomyKm(steps: Int): Double = com.stepup.android.domain.RewardEconomy.distanceMeters(steps) / 1000.0

// ── 오늘 걸음 ──────────────────────────────────────────────────

/**
 * 오늘 걸음(시안 `home` 위) — 작은 "오늘 걸음" · 큰 숫자(96) · "/ 목표". 누르면 오늘의 활동(E01).
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
    val c = StepUpUi.colors
    if (firstHome) {
        // 첫 러닝 홈의 두 줄은 설명이라 누를 곳이 없다 — 걸음이 생기면 걸음 칸이 오늘의 활동을 연다
        Column(
            Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("home-steps"),
        ) {
            Text(
                stringResource(R.string.onb_home_first_title), style = StepUpUi.type.largeTitle.copy(fontSize = 28.sp),
                color = c.label, modifier = Modifier.testTag("home-headline"),
            )
            Text(
                stringResource(R.string.onb_home_first_body), style = StepUpUi.type.body, color = c.secondaryLabel,
                modifier = Modifier.padding(top = 8.dp).testTag("home-first-run"),
            )
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
    val detailsLabel = stringResource(R.string.home_open_details)
    // 둥근 모서리로 자르지 않는다 — 안쪽 여백이 없어 왼쪽 위 "오늘 걸음" 첫 글자가 모서리에 잘렸다(2026-10-27).
    // 누름 효과(살짝 작아짐 · 그린 픽셀만 어둡게)는 자르지 않아도 그대로다
    Row(
        Modifier.fillMaxWidth()
            .feedbackClickable(role = Role.Button, onClick = onOpenDetails)
            .semantics { onClick(label = detailsLabel) { onOpenDetails(); true } }
            .testTag("home-steps"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            // 예전 서버(오늘의 보상 없음) — 이 숫자는 폰이 센 하루 전체 걸음이다
            Text(
                stringResource(R.string.dr_all_day_label), style = StepUpUi.type.segment, color = c.secondaryLabel,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(4.dp))
            if (!hasPermission) {
                PermissionStrip(onClick = onRequestPermission)
                return@Column
            }
            // 숫자 올라가기 + 바뀔 때 살짝 튀기(1-5 움직임)
            val shown = countUpFloat(if (loaded) todaySteps.toFloat() else 0f).roundToInt()
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    if (loaded) "%,d".format(shown) else "—",
                    style = StepUpUi.type.numberHero.copy(fontSize = 58.sp, lineHeight = 58.sp, letterSpacing = .005.em),
                    color = c.label, maxLines = 1, softWrap = false,
                    modifier = Modifier.alignByBaseline().popOnChange(todaySteps / 1000).testTag("home-steps-value"),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.d26_steps_goal, "%,d".format(goal)),
                    style = StepUpUi.type.navTitle.copy(fontSize = 17.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.02).em),
                    color = c.secondaryLabel, maxLines = 1,
                    modifier = Modifier.alignByBaseline().testTag("home-goal"),
                )
            }
        }
        if (hasPermission) {
            Spacer(Modifier.width(12.dp))
            StepsRing(if (loaded && goal > 0) todaySteps.toFloat() / goal else 0f)
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
    /** 오늘의 보상(서버 0118) — 있으면 걸음 칸이 러닝 걸음 막대 · 이번 주 · 연속 · 목표 바꾸기가 된다 */
    bar: com.stepup.android.domain.DailyBar? = null,
    daily: com.stepup.android.domain.DailyRewards? = null,
    onChangeGoal: () -> Unit = {},
) {
    val t = com.stepup.android.ui.components.runTone()
    Column(Modifier.fillMaxWidth().testTag("home-activity")) {
        Text(stringResource(R.string.home_activity_title), color = t.text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(12.dp))
        if (bar != null) {
            TodayRewards(
                bar = bar, daily = daily, allDaySteps = steps, loaded = loaded, hasPermission = hasPermission,
                onRequestPermission = onRequestPermission, onChangeGoal = onChangeGoal,
            )
        } else if (!hasPermission) {
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

// ── 오늘의 보상(서버 0118) ───────────────────────────────────────

/**
 * 홈 위 — 오늘의 보상 막대(docs/forge-xp-daily-spec-2026-11-19.md 5.3). 큰 숫자는 서버가 확인한 오늘 러닝 걸음, 옆에 작게 하루 전체 걸음
 * (걸음 권한이 있을 때만 — 막대는 권한이 없어도 보인다). 눈금(상자 · 목표)은 서버가 주고 받은 것만 ✓, 아래 한 줄은 다음 보상까지 · 연속.
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
    val c = StepUpUi.colors
    val detailsLabel = stringResource(R.string.home_open_details)
    Column(
        Modifier.fillMaxWidth()
            .feedbackClickable(role = Role.Button, onClick = onOpenDetails)
            .semantics { onClick(label = detailsLabel) { onOpenDetails(); true } }
            .testTag("home-steps"),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.home_steps_today), style = StepUpUi.type.segment, color = c.secondaryLabel,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            AllDaySteps(allDaySteps, loaded, hasPermission, onRequestPermission)
        }
        Spacer(Modifier.height(4.dp))
        val shown = countUpFloat(if (bar.known) bar.rewardSteps.toFloat() else 0f).roundToInt()
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                if (bar.known) "%,d".format(shown) else "—",
                style = StepUpUi.type.numberHero.copy(fontSize = 58.sp, lineHeight = 58.sp, letterSpacing = .005.em),
                color = c.label, maxLines = 1, softWrap = false,
                modifier = Modifier.alignByBaseline().popOnChange(bar.rewardSteps / 1000).testTag("home-steps-value"),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.d26_steps_goal, if (bar.goal > 0) "%,d".format(bar.goal) else "—"),
                style = StepUpUi.type.navTitle.copy(fontSize = 17.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.02).em),
                color = c.secondaryLabel, maxLines = 1,
                modifier = Modifier.alignByBaseline().testTag("home-goal"),
            )
        }
        Spacer(Modifier.height(12.dp))
        RewardTrack(bar, hole = c.background)
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
    Spacer(Modifier.height(4.dp))
    Text(
        text, style = StepUpUi.type.caption, color = StepUpUi.colors.secondaryLabel,
        modifier = Modifier.fillMaxWidth().testTag(tag),
    )
}

/** 하루 전체 걸음(폰 만보기) — 작게. 걸음 권한이 없으면 그 자리에 "하루 전체 걸음 보기"(누르면 권한을 묻는다) */
@Composable
private fun AllDaySteps(steps: Int, loaded: Boolean, hasPermission: Boolean, onRequestPermission: () -> Unit) {
    val c = StepUpUi.colors
    if (hasPermission) {
        Text(
            stringResource(R.string.dr_all_day, if (loaded) "%,d".format(steps) else "—"),
            style = StepUpUi.type.caption, color = c.tertiaryLabel, maxLines = 1,
            modifier = Modifier.testTag("home-all-day"),
        )
    } else {
        Text(
            stringResource(R.string.dr_all_day_allow), style = StepUpUi.type.caption, color = c.accentText, maxLines = 1,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).feedbackClickable(onClick = onRequestPermission)
                .padding(horizontal = 6.dp, vertical = 4.dp).testTag("home-permission"),
        )
    }
}

/**
 * 막대 — 구간(0 → 상자 → 상자 → 목표)을 같은 너비로, 같은 걸음의 눈금은 하나(목표 4,000 = 둘째 상자). 진한 구간 = 서버가 확인한 걸음,
 * 옅은 구간 = 확인 중. 눈금은 서버가 받았다고 한 것만 채우고 ✓. [hole] 은 아직 받지 않은 눈금 안쪽(바탕색)
 */
@Composable
internal fun RewardTrack(bar: com.stepup.android.domain.DailyBar, hole: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    val c = StepUpUi.colors
    val dot = 18.dp
    Column(modifier.fillMaxWidth().testTag("home-reward-bar")) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(dot)) {
            val r = dot.toPx() / 2
            val h = 8.dp.toPx()
            val left = r
            val w = (size.width - 2 * r).coerceAtLeast(1f)
            val top = size.height / 2 - h / 2
            val corner = androidx.compose.ui.geometry.CornerRadius(h / 2, h / 2)
            drawRoundRect(c.separator, androidx.compose.ui.geometry.Offset(left, top), androidx.compose.ui.geometry.Size(w, h), corner)
            if (bar.pendingFraction > bar.confirmedFraction) {
                drawRoundRect(
                    c.primary.copy(alpha = 0.3f), androidx.compose.ui.geometry.Offset(left, top),
                    androidx.compose.ui.geometry.Size(w * bar.pendingFraction, h), corner,
                )
            }
            if (bar.confirmedFraction > 0f) {
                drawRoundRect(
                    c.primary, androidx.compose.ui.geometry.Offset(left, top),
                    androidx.compose.ui.geometry.Size(w * bar.confirmedFraction, h), corner,
                )
            }
            val cy = size.height / 2
            val stroke = 2.dp.toPx()
            bar.slots.forEach { slot ->
                val center = androidx.compose.ui.geometry.Offset(left + w * slot.position, cy)
                if (slot.got) {
                    drawCircle(c.primary, r, center)
                    val check = androidx.compose.ui.graphics.Path().apply {
                        moveTo(center.x - r * 0.42f, cy + r * 0.02f)
                        lineTo(center.x - r * 0.1f, cy + r * 0.34f)
                        lineTo(center.x + r * 0.46f, cy - r * 0.32f)
                    }
                    drawPath(
                        check, c.onPrimary,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round,
                        ),
                    )
                } else {
                    drawCircle(hole, r, center)
                    drawCircle(c.tertiaryLabel, r - stroke / 2, center, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        TickLabels(bar, dot)
    }
}

/**
 * 눈금 아래 글 — 걸음 · 보상(상자 / +N SUP). 구간마다 같은 너비의 칸에 오른쪽으로 붙여 그 눈금에서 끝난다(눈금이 서로 가까워도 글이 겹치지
 * 않는다). 금액은 서버 값(받았으면 받은 SUP, 아니면 오늘 채우면 받을 SUP)
 */
@Composable
private fun TickLabels(bar: com.stepup.android.domain.DailyBar, dot: Dp) {
    val c = StepUpUi.colors
    Row(Modifier.fillMaxWidth().padding(start = dot)) {
        bar.slots.forEach { slot ->
            val reward = tickReward(slot, bar)
            val got = slot.got && bar.known
            val description = stringResource(if (got) R.string.dr_tick_got_cd else R.string.dr_tick_open_cd, "%,d".format(slot.steps), reward)
            Column(
                Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = description }
                    .testTag("home-reward-tick-${slot.steps}"),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    "%,d".format(slot.steps), style = StepUpUi.type.caption, color = c.secondaryLabel, maxLines = 1,
                    textAlign = TextAlign.End,
                )
                Text(
                    if (got) "$reward ✓" else reward,
                    style = StepUpUi.type.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = if (got) c.accentText else c.label, maxLines = 2, textAlign = TextAlign.End,
                )
            }
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

/** 막대 아래 한 줄 — 다음 보상까지 남은 걸음 · 연속, 옆에 확인 중 걸음 */
@Composable
private fun RewardLine(bar: com.stepup.android.domain.DailyBar) {
    val c = StepUpUi.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            rewardLineText(bar), style = StepUpUi.type.rowSubtitle, color = c.secondaryLabel,
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
internal fun rewardLineText(bar: com.stepup.android.domain.DailyBar): String {
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
 * 오늘의 활동 시트의 오늘의 보상 — 러닝 걸음 · 목표(내일부터 바뀔 목표) · 막대 · 하루 전체 걸음, 이번 주(주간 상급 상자 · 스텝 서지),
 * 연속(7일 중 며칠 · 다음 연속 상자까지), 목표 바꾸기. 모든 수는 서버 상태 그대로.
 */
@Composable
private fun TodayRewards(
    bar: com.stepup.android.domain.DailyBar,
    daily: com.stepup.android.domain.DailyRewards?,
    allDaySteps: Int,
    loaded: Boolean,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onChangeGoal: () -> Unit,
) {
    val t = com.stepup.android.ui.components.runTone()
    com.stepup.android.ui.components.RunNumber(
        if (bar.known) "%,d".format(bar.rewardSteps) else "—", unit = stringResource(R.string.home_steps_unit),
        size = 56.sp, unitSize = 22.sp, align = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(), valueTag = "home-activity-steps",
    )
    Text(
        stringResource(R.string.dr_sheet_today, if (bar.goal > 0) "%,d".format(bar.goal) else "—"), color = t.label, fontSize = 15.sp,
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
    )
    bar.goalNext?.let { next ->
        Text(
            stringResource(R.string.dr_goal_next, "%,d".format(next)), color = t.cyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 2.dp).testTag("home-activity-goal-next"),
        )
    }
    Spacer(Modifier.height(12.dp))
    RewardTrack(bar, hole = t.sheet)
    Spacer(Modifier.height(6.dp))
    Text(rewardLineText(bar), color = t.label, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth().testTag("home-activity-all-day"), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.dr_all_day_label), color = t.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f))
        if (hasPermission) {
            Text(if (loaded) "%,d".format(allDaySteps) else "—", color = t.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        } else {
            AllDaySteps(allDaySteps, loaded, hasPermission = false, onRequestPermission = onRequestPermission)
        }
    }
    Text(stringResource(R.string.dr_all_day_note), color = t.muted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())

    // 이번 주 — 주간 상급 상자 · 스텝 서지(서버 값)
    if (daily != null && bar.known) {
        Spacer(Modifier.height(12.dp))
        com.stepup.android.ui.components.RunDivider()
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.dr_week_title), color = t.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        if (daily.weekTarget > 0) {
            WeekRow(
                label = stringResource(R.string.dr_week_box, "%,d".format(daily.weekTarget)),
                value = if (daily.weekGot) stringResource(R.string.boxes_got)
                else stringResource(R.string.boxes_steps_progress, "%,d".format(daily.stepsWeek.coerceAtMost(daily.weekTarget)), "%,d".format(daily.weekTarget)),
                fraction = if (daily.weekGot) 1f else (daily.stepsWeek.toFloat() / daily.weekTarget).coerceIn(0f, 1f),
                tag = "home-activity-week",
            )
        }
        daily.surge?.takeIf { it.target > 0 }?.let { surge ->
            WeekRow(
                label = stringResource(R.string.dr_surge, "%,d".format(surge.target), com.stepup.android.domain.DailyRewardsBar.supText(surge.reward)),
                value = if (surge.claimed) stringResource(R.string.boxes_got)
                else stringResource(R.string.boxes_steps_progress, "%,d".format(surge.steps.coerceAtMost(surge.target)), "%,d".format(surge.target)),
                fraction = surge.fraction,
                tag = "home-activity-surge",
            )
        }
        // 연속 — 7일 중 며칠 · 다음 연속 상급 상자까지(서버 값)
        val streak = daily.streak
        if (streak != null) {
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.dr_streak_title), color = t.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            val inWeek = com.stepup.android.domain.DailyRewardsBar.streakInWeek(streak, paidToday = daily.goalPaid != null)
            WeekRow(
                label = stringResource(R.string.dr_streak_week, inWeek),
                value = daily.streakBoxIn?.let {
                    if (it <= 0) stringResource(R.string.dr_streak_box_today) else stringResource(R.string.dr_streak_box_in, it)
                } ?: "",
                fraction = inWeek / 7f,
                tag = "home-activity-streak",
            )
            daily.streakBest?.takeIf { it > 0 }?.let { best ->
                Text(stringResource(R.string.dr_streak_best, best), color = t.label, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).feedbackClickable(onClick = onChangeGoal)
            .padding(vertical = 10.dp).testTag("home-activity-change-goal"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.dr_goal_change), color = t.cobaltText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = t.label, modifier = Modifier.size(18.dp))
    }
}

/** 이번 주 · 연속 한 줄 — 이름 · 값(오른쪽) · 가는 막대 */
@Composable
private fun WeekRow(label: String, value: String, fraction: Float, tag: String) {
    val t = com.stepup.android.ui.components.runTone()
    Column(Modifier.fillMaxWidth().padding(top = 8.dp).semantics(mergeDescendants = true) {}.testTag(tag)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = t.text, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            if (value.isNotEmpty()) Text(value, color = t.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
        }
        Spacer(Modifier.height(4.dp))
        com.stepup.android.ui.components.RunMeter(fraction, height = 6.dp)
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

