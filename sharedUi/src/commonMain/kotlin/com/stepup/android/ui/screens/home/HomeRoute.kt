package com.stepup.android.ui.screens.home

import kotlinx.datetime.toLocalDateTime
import com.stepup.android.domain.displayName
import com.stepup.android.domain.minusYearsKx
import com.stepup.android.domain.plusYearsKx
import com.stepup.android.domain.minusMonthsKx
import com.stepup.android.domain.plusMonthsKx
import com.stepup.android.domain.minusWeeksKx
import com.stepup.android.domain.plusWeeksKx
import com.stepup.android.domain.minusDaysKx
import com.stepup.android.domain.plusDaysKx
import com.stepup.android.core.AppServices
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
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
import com.stepup.ui.res.*
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
import kotlinx.datetime.LocalTime
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

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

    var hasPermission by remember {
        mutableStateOf(com.stepup.android.ui.StepPermissions.hasActivityRecognition())
    }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    // 누를 때만 묻는다(홈을 보는 것만으로는 묻지 않는다). 러닝 권한 안내와 같은 이력을 남겨 다음 안내가 맞게 고른다
    val requestActivity = com.stepup.android.ui.screens.walk.rememberActivityPermissionRequest {
        hasPermission = com.stepup.android.ui.StepPermissions.hasActivityRecognition()
        permissionDenied = !hasPermission
        if (hasPermission) viewModel.onPermissionGranted()
    }
    LifecycleResumeEffect(Unit) {
        val granted = com.stepup.android.ui.StepPermissions.hasActivityRecognition()
        if (granted) permissionDenied = false
        if (granted != hasPermission) {
            hasPermission = granted
            if (granted) viewModel.onPermissionGranted()
        }
        onPauseOrDispose { }
    }

    var showDetails by rememberSaveable { mutableStateOf(false) }
    val session by com.stepup.android.core.AppServices.run.state.collectAsStateWithLifecycle()
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
                Text(stringResource(Res.string.home_activity_more), color = Silver, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("home-records-title"))
                if (!hasPermission && permissionDenied) {
                    androidx.compose.material3.TextButton(onClick = {
                        com.stepup.android.core.ExternalIntents.openAppSettings()
                    }) { Text(stringResource(Res.string.cd_open_settings)) }
                }
                RecordWeek(state.week, state.todaySteps, state.loaded)
                ShortcutButton(
                    icon = Icons.AutoMirrored.Filled.Article,
                    label = stringResource(Res.string.home_shortcut_news),
                    subtitle = stringResource(Res.string.home_shortcut_news_sub),
                    onClick = { showDetails = false; onOpenNews() },
                    modifier = Modifier.fillMaxWidth(),
                )
                EnergyLine(earnableSteps = state.earnableSteps, ready = state.loaded && state.maxEnergy > 0)
            }
            Spacer(Modifier.height(14.dp))
            com.stepup.android.ui.components.RunButton(
                stringResource(Res.string.home_activity_ok), { showDetails = false },
                modifier = Modifier.testTag("home-details-ok"),
            )
        }
    }
}

/**
 * 오늘의 활동 시트 — 큰 걸음 수 · 목표 · 막대와 달성 % · 남은 걸음, 거리 | 러닝 시간, 오늘의 SUP.
 * SUP 은 서버가 원장에 적은 값만(앱이 셈한 예상은 쓰지 않는다). 올리는 중인 러닝이 있으면 "확인 중".
 */
@Composable
fun TodayActivity(
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
        Text(stringResource(Res.string.home_activity_title), color = t.text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(12.dp))
        if (!hasPermission) {
            PermissionStrip(onClick = onRequestPermission)
        } else {
            com.stepup.android.ui.components.RunNumber(
                if (loaded) "${com.stepup.android.domain.formatGrouped((steps).toLong())}" else "—", unit = stringResource(Res.string.home_steps_unit),
                size = 56.sp, unitSize = 22.sp, align = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(), valueTag = "home-activity-steps",
            )
            Text(
                stringResource(Res.string.home_activity_goal, "${com.stepup.android.domain.formatGrouped((goal).toLong())}"), color = t.label, fontSize = 15.sp,
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
                    steps >= goal -> stringResource(Res.string.home_activity_done)
                    else -> stringResource(Res.string.home_activity_left, "${com.stepup.android.domain.formatGrouped((goal - steps).toLong())}")
                },
                color = t.label, fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        com.stepup.android.ui.components.RunStatRow(
            listOf(
                com.stepup.android.ui.components.RunStat(stringResource(Res.string.stat_distance), km?.let { "${com.stepup.android.domain.formatFixed((it).toDouble(), 1)}" } ?: "—", "km",
                    tag = "home-activity-distance"),
                com.stepup.android.ui.components.RunStat(stringResource(Res.string.home_activity_run_time), "${runSec / 60}",
                    stringResource(Res.string.home_activity_minutes), tag = "home-activity-time"),
            ),
            framed = true, valueSize = 30.sp,
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().testTag("home-activity-sup"), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.home_activity_sup), color = t.text, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f))
            com.stepup.android.ui.components.RunNumber(
                earned?.let { "+" + com.stepup.android.ui.components.formatSupDown(it, 2) } ?: "—",
                size = 28.sp, color = t.cyan, valueTag = "home-activity-sup-value",
            )
            if (pending) {
                Spacer(Modifier.width(8.dp))
                com.stepup.android.ui.components.RunChip(stringResource(Res.string.home_activity_checking),
                    modifier = Modifier.testTag("home-activity-pending"))
            }
        }
        Text(stringResource(Res.string.home_activity_sup_note), color = t.label, fontSize = 13.sp, textAlign = TextAlign.End,
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
fun EnergyLine(earnableSteps: Int, ready: Boolean) {
    var secondsLeft by remember { mutableIntStateOf(86_400 - kotlin.time.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).time.toSecondOfDay()) }
    LaunchedEffect(Unit) {
        while (true) {
            secondsLeft = 86_400 - kotlin.time.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).time.toSecondOfDay()
            delay(1_000)
        }
    }
    val countdown = "${com.stepup.android.domain.zeroPad((secondsLeft / 3600).toLong(), 2)}:${com.stepup.android.domain.zeroPad(((secondsLeft % 3600) / 60).toLong(), 2)}"
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
                !ready -> stringResource(Res.string.home_energy_loading)
                empty -> stringResource(Res.string.home_energy_empty) + " · " +
                    stringResource(Res.string.home_recharge_in, countdown)
                else -> stringResource(Res.string.home_energy_can, "${com.stepup.android.domain.formatGrouped((earnableSteps).toLong())}")
            },
            fontSize = 14.sp,
            color = if (empty) Alert else Silver,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun RecordWeek(week: List<com.stepup.android.data.local.DailyStepsEntity>, todaySteps: Int, loaded: Boolean) {
    val today = com.stepup.android.domain.todayIn()
    val days = (6 downTo 0).map { today.minusDaysKx(it.toLong()) }
    val recorded = week.associateBy { it.epochDay }
    val values = days.map { if (it == today) todaySteps else recorded[it.toEpochDays()]?.steps ?: 0 }
    val maximum = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val locale = androidx.compose.ui.text.intl.Locale.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(stringResource(Res.string.home_recent_week), style = MaterialTheme.typography.titleMedium, color = Snow)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        days.forEachIndexed { index, day ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                AdaptiveNumber(if (loaded) "${com.stepup.android.domain.formatGrouped((values[index]).toLong())}" else "—", 12.sp,
                    color = if (day == today) VoltText else Silver, textAlign = TextAlign.Center)
                Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.fillMaxWidth(0.72f)
                        .height(if (loaded) (40f * values[index].toFloat() / maximum).coerceAtLeast(2f).dp else 2.dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(if (day == today) Volt else Volt.copy(alpha = 0.4f)))
                }
                Text(day.dayOfWeek.displayName(com.stepup.android.domain.DateNameStyle.SHORT, androidx.compose.ui.text.intl.Locale.current.language),
                    style = MaterialTheme.typography.labelSmall, color = Silver)
            }
        }
    }
    }
}
