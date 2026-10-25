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
    /** 크루 홈(내 소속 크루 카드) */
    onOpenCrew: (String) -> Unit = {},
    /** 크루가 없을 때 — 커뮤니티의 크루 찾기 */
    onFindCrew: () -> Unit = {},
    /** 크루 러닝 — 고른 대기실(crewId, 방 번호) */
    onOpenRoom: (String, Long) -> Unit = { _, _ -> },
    /** 크루 러닝 — 새 대기실 만들기 */
    onCreateRoom: (String) -> Unit = {},
    /** 시안 검사(OnboardingDesignTest)만 쓴다 — 첫 러닝 홈(시안 11)을 그려 본다. null 이면 실제 권한 · 기록으로 판단한다 */
    firstRunPreview: Boolean? = null,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val earned by viewModel.todayEarned.collectAsStateWithLifecycle()
    val week by viewModel.weekRuns.collectAsStateWithLifecycle()
    val starts by viewModel.runStarts.collectAsStateWithLifecycle()
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

    // 크루 — 들어가 있는 대기실의 크루가 먼저, 아니면 가입한 첫 크루(2026-10-26 시안 "내 소속 크루")
    val crewRepo = com.stepup.android.core.ServiceLocator.crewRepository
    val crews by crewRepo.crews.collectAsStateWithLifecycle()
    val party by crewRepo.party.collectAsStateWithLifecycle()
    val crew = remember(crews, party.crewId) {
        val joined = crews.filter { it.joined }
        joined.firstOrNull { it.id == party.crewId && party.flashPostId == null } ?: joined.firstOrNull()
    }
    LaunchedEffect(Unit) {
        if (crewRepo.crews.value.isEmpty()) runCatching { crewRepo.refresh() }
    }
    var segment by rememberSaveable { mutableStateOf(RunSegment.FREE) }
    // 사람 없는 배경 15장 중 하나 — 러닝 탭에 들어올 때마다 고른다
    val photoIndex = remember { kotlin.random.Random.nextInt(RunPhotos.names.size) }
    val gps = rememberGpsReadiness()
    // 크루 러닝 칸이 보이는 동안만 대기실 목록을 10초마다 다시 묻는다(보기만 — 들어가지 않는다)
    var rooms by remember(crew?.id) { mutableStateOf<com.stepup.android.ui.screens.walk.RoomsUi?>(null) }
    var roomsReload by remember { mutableIntStateOf(0) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(crew?.id, segment, roomsReload) {
        val id = crew?.id ?: return@LaunchedEffect
        if (segment != RunSegment.CREW) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            while (true) {
                rooms = when (val result = crewRepo.rooms(id)) {
                    is com.stepup.android.data.remote.ServerResult.Ok -> com.stepup.android.ui.screens.walk.RoomsUi.Ready(result.value)
                    is com.stepup.android.data.remote.ServerResult.Retry ->
                        (rooms as? com.stepup.android.ui.screens.walk.RoomsUi.Ready) ?: com.stepup.android.ui.screens.walk.RoomsUi.Failed(signIn = false)
                    is com.stepup.android.data.remote.ServerResult.SignInRequired -> com.stepup.android.ui.screens.walk.RoomsUi.Failed(signIn = true)
                    is com.stepup.android.data.remote.ServerResult.Rejected -> com.stepup.android.ui.screens.walk.RoomsUi.Failed(signIn = false)
                }
                delay(10_000)
            }
        }
    }

    val pending by viewModel.pendingUploads.collectAsStateWithLifecycle()
    val runSec by viewModel.todayRunSec.collectAsStateWithLifecycle()
    HomeContent(
        loaded = state.loaded, todaySteps = state.todaySteps, goal = state.goal,
        hasPermission = hasPermission, firstHome = firstHome, running = session.isActive, largeText = largeText,
        earned = earned, km = if (state.loaded) RewardEconomyKm(state.todaySteps) else null, weekRuns = week?.runs,
        onStartRun = onStartRun,
        onRequestPermission = { requestActivity() }, onOpenDetails = { showDetails = true },
        segment = segment, onSegment = { segment = it },
        crew = crew, onOpenCrew = { crew?.let { onOpenCrew(it.id) } }, onFindCrew = onFindCrew,
        photoIndex = photoIndex, gps = gps,
        rooms = rooms,
        onOpenRoom = { id -> crew?.let { onOpenRoom(it.id, id) } },
        onCreateRoom = { crew?.let { onCreateRoom(it.id) } },
        onRetryRooms = {
            rooms = com.stepup.android.ui.screens.walk.RoomsUi.Loading
            roomsReload++
        },
    )

    if (showDetails) {
        // E01 — 오늘의 활동: 걸음 · 목표 · 남은 걸음 · 거리 · 러닝 시간 · 오늘의 SUP(서버 확인 값). 예전 상세 기록
        // (7일 걸음 · 소식 · 에너지)은 지우지 않고 아래 "더 보기"에 둔다
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
 * 러닝 탭의 몸통(2026-10-26 시안 `run` · `lobby`) — 상태 없이 그린다. 머리(로고 · 알림)와 하단 탭은 공통 틀이 그린다.
 * 위의 세그먼트가 자유 러닝 · 크루 러닝을 가른다. 화면이 낮거나 글자가 크면 본문이 넘어간다.
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
    segment: RunSegment = RunSegment.FREE,
    onSegment: (RunSegment) -> Unit = {},
    crew: com.stepup.android.data.repo.Crew? = null,
    onOpenCrew: () -> Unit = {},
    onFindCrew: () -> Unit = {},
    photoIndex: Int = 0,
    gps: GpsReadiness = GpsReadiness.WAITING,
    rooms: com.stepup.android.ui.screens.walk.RoomsUi? = null,
    onOpenRoom: (Long) -> Unit = {},
    onCreateRoom: () -> Unit = {},
    onRetryRooms: () -> Unit = {},
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = StepUpUi.Gutter)
            .testTag(if (segment == RunSegment.FREE) "home-content" else "run-crew-content"),
    ) {
        Spacer(Modifier.height(8.dp))
        RunSegments(segment, onSegment)
        Spacer(Modifier.height(20.dp))
        RunTabTitle(stringResource(if (segment == RunSegment.FREE) R.string.d26_title_run else R.string.d26_seg_crew))
        Spacer(Modifier.height(14.dp))
        MyCrewCard(crew, onOpen = onOpenCrew, onFind = onFindCrew)
        if (segment == RunSegment.FREE) {
            Spacer(Modifier.height(if (largeText) 14.dp else 22.dp))
            StepsHero(
                todaySteps = todaySteps, goal = goal, loaded = loaded,
                hasPermission = hasPermission, firstHome = firstHome,
                onRequestPermission = onRequestPermission, onOpenDetails = onOpenDetails,
            )
            Spacer(Modifier.height(16.dp))
            RunPhotoCard(photoIndex)
            Spacer(Modifier.height(14.dp))
            GpsLine(gps)
            Spacer(Modifier.height(16.dp))
            StatRow(
                listOf(
                    StatItem(stringResource(R.string.d26_stat_km), km?.let { "%.2f".format(it) } ?: "—", "km", tag = "home-stat-km"),
                    StatItem(stringResource(R.string.d26_stat_sup),
                        earned?.let { "+" + com.stepup.android.ui.components.formatSupDown(it, 2) } ?: "—", "SUP", tag = "home-stat-sup"),
                    StatItem(stringResource(R.string.d26_stat_week), weekRuns?.toString() ?: "—",
                        stringResource(R.string.pf_runs_unit), tag = "home-stat-runs"),
                ),
                modifier = Modifier.testTag("home-stats"),
            )
            Spacer(Modifier.height(20.dp))
            // 이 화면의 주 행동 — 블루 알약(시안 "러닝 시작 ▷"). 누르면 러닝 시작 메뉴(U01), 달리는 중이면 그 러닝
            PrimaryButton(
                stringResource(if (running) R.string.home_continue_run else R.string.home_start_run), onStartRun,
                modifier = Modifier.fillMaxWidth().guideTarget(GuideTour.Targets.HOME_START_RUN).testTag("home-start-run"),
            )
        } else {
            Spacer(Modifier.height(22.dp))
            if (crew != null) {
                CrewRoomsSection(rooms, crew.name, onOpenRoom = onOpenRoom, onRetry = onRetryRooms)
                Spacer(Modifier.height(16.dp))
                val done = (rooms as? com.stepup.android.ui.screens.walk.RoomsUi.Ready)?.rooms?.doneToday == true
                NewRoomButton(enabled = rooms is com.stepup.android.ui.screens.walk.RoomsUi.Ready && !done, onClick = onCreateRoom)
            }
        }
        Spacer(Modifier.height(24.dp))
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
    val detailsLabel = stringResource(R.string.home_open_details)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .feedbackClickable(role = Role.Button, onClick = onOpenDetails)
            .semantics { onClick(label = detailsLabel) { onOpenDetails(); true } }
            .testTag("home-steps"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.home_steps_today), style = StepUpUi.type.segment, color = c.secondaryLabel,
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

