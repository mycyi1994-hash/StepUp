package com.stepup.android.ui.screens.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.MaterialTheme
import com.stepup.android.ui.components.AdaptiveNumber
import com.stepup.android.ui.components.BarMeter
import com.stepup.android.ui.components.HairlineDivider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.ui.StepPermissions
import com.stepup.android.domain.RewardEconomy
import com.stepup.android.domain.AvatarPose
import com.stepup.android.domain.AvatarArtCatalog
import com.stepup.android.ui.components.BadgeTone
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.PrimaryCta
import com.stepup.android.ui.components.CharacterStage
import com.stepup.android.ui.components.AvatarLookNote
import com.stepup.android.ui.components.ShortcutButton
import com.stepup.android.ui.components.SmallBadge
import com.stepup.android.ui.components.quietClickable
import com.stepup.android.ui.components.variantLabel
import androidx.compose.foundation.layout.heightIn
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.guide.GuideTour
import com.stepup.android.ui.guide.guideTarget
import com.stepup.android.ui.theme.Alert
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt
import com.stepup.android.ui.theme.VoltText
import java.time.LocalTime
import kotlinx.coroutines.delay

/** Native records and one pinned run action; scenic artwork is an independent layer. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartRun: () -> Unit = {},
    onOpenWallet: () -> Unit = {},
    onOpenChallenges: () -> Unit = {},
    onOpenNews: () -> Unit = {},
    onOpenCustomize: () -> Unit = {},
    backgroundSetting: com.stepup.android.ui.components.RunnerSetting = com.stepup.android.ui.components.RunnerSetting.HomeBlueNight,
    onPreviousBackground: () -> Unit = {},
    onNextBackground: () -> Unit = {},
    /** 지금 보이는 풍경이 실제 날씨로 고른 것이면 그 날씨 — 아치 아래에 한 줄로 밝힌다 */
    weatherScene: com.stepup.android.domain.WeatherScene? = null,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val earned by viewModel.todayEarned.collectAsStateWithLifecycle()
    val runSec by viewModel.todayRunSec.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(StepPermissions.hasActivityRecognition(context))
    }
    var permissionDenied by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
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

    val largeText = LocalDensity.current.fontScale > 1.2f
    var showDetails by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val session by com.stepup.android.service.WalkSessionService.state.collectAsStateWithLifecycle()
    val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
    val steps = if (state.loaded) "%,d".format(state.todaySteps) else "—"
    // S2 홈 — 오늘 걸음 문장 → 아치 풍경 → 오늘 SUP → 원형 시작 버튼. 자세한 기록은 "더보기" 안.
    Column(
        Modifier.fillMaxSize().padding(horizontal = com.stepup.android.ui.theme.StepUpDesign.Gutter)
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.height(if (screenHeight < 760) 4.dp else 20.dp))
            val runMode by com.stepup.android.core.ServiceLocator.userPrefs.runMode
                .collectAsStateWithLifecycle(initialValue = com.stepup.android.domain.RunMode.LITE)
            val runner = runMode == com.stepup.android.domain.RunMode.RUNNER
            com.stepup.android.ui.components.S2Kicker(
                stringResource(R.string.home_s2_goal, "%,d".format(state.goal)) +
                    if (runner) " · " + stringResource(R.string.setup_mode_runner) else "",
            )
            Box(Modifier.height(12.dp))
            com.stepup.android.ui.components.S2Headline(stringResource(R.string.home_s2_headline, steps))
            Box(Modifier.height(14.dp))
            com.stepup.android.ui.components.S2Subtitle(
                when {
                    !state.loaded || state.goal <= 0 -> " "
                    state.todaySteps >= state.goal -> stringResource(R.string.home_s2_done)
                    else -> stringResource(R.string.home_s2_left, "%,d".format(state.goal - state.todaySteps))
                },
            )
            Box(Modifier.height(if (screenHeight < 760) 16.dp else 26.dp))
            val archHeight = com.stepup.android.ui.components.s2ArchHeight(screenHeight, largeText)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                // 풍경 넘기기 — 아치 양옆의 작은 화살표. 장식이라 날씨·위치처럼 말하지 않는다.
                SceneArrow(Icons.Filled.ChevronLeft, stringResource(R.string.home_previous_background),
                    onPreviousBackground, Modifier.testTag("home-background-previous"))
                com.stepup.android.ui.components.S2Arch(
                    Modifier.height(archHeight).width(archHeight * (216f / 262f)),
                    image = com.stepup.android.ui.components.s2SceneryRes(backgroundSetting),
                )
                SceneArrow(Icons.Filled.ChevronRight, stringResource(R.string.home_next_background),
                    onNextBackground, Modifier.testTag("home-background-next"))
            }
            if (weatherScene != null) {
                Box(Modifier.height(8.dp))
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
            Box(Modifier.height(18.dp))
            if (!hasPermission) {
                PermissionStrip(onClick = { permissionLauncher.launch(StepPermissions.missingActivity(context)) })
                Box(Modifier.height(12.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.home_s2_today_sup), color = Silver, fontSize = 13.sp)
                Text(earned?.let { "+" + com.stepup.android.ui.components.formatSupDown(it, 2) } ?: "—",
                    color = Snow, fontSize = 20.sp, fontWeight = FontWeight.Normal)
            }
            Box(Modifier.height(4.dp))
            Text(stringResource(R.string.home_s2_sup_note), color = com.stepup.android.ui.theme.Slate,
                fontSize = 12.sp, textAlign = TextAlign.Center)
            // 러너 모드 — 착용 신발과 상태를 한 줄로(S2 시안 20). 누르면 신발 탭.
            val shoe = state.equipped
            if (runner && shoe != null) {
                Box(Modifier.height(12.dp))
                RunnerShoeLine(shoe, onOpenCustomize)
            }
        }
        com.stepup.android.ui.components.S2ActionRow(
            start = {
                // 보유 SUP 는 머리글에 있다 — 여기서는 오늘 거리
                com.stepup.android.ui.components.S2SideInfo(
                    label = stringResource(R.string.stat_distance),
                    value = if (state.loaded) "%.1f km".format(RewardEconomy.distanceMeters(state.todaySteps) / 1000) else "—",
                )
            },
            end = {
                com.stepup.android.ui.components.S2SideInfo(
                    label = stringResource(R.string.common_more),
                    end = true,
                    onClick = { showDetails = true },
                    modifier = Modifier.guideTarget(GuideTour.Targets.HOME_SHORTCUTS).testTag("home-details"),
                )
            },
        ) {
            com.stepup.android.ui.components.S2RoundAction(
                icon = Icons.Filled.PlayArrow,
                label = stringResource(if (session.isActive) R.string.cd_resume else R.string.home_start_run),
                onClick = onStartRun,
                modifier = Modifier.guideTarget(GuideTour.Targets.HOME_START_RUN).testTag("home-start-run"),
            )
        }
    }

    if (showDetails) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showDetails = false },
            containerColor = com.stepup.android.ui.theme.Night,
        ) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = com.stepup.android.ui.theme.StepUpDesign.Gutter)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!hasPermission) {
                    PermissionStrip(onClick = { permissionLauncher.launch(StepPermissions.missingActivity(context)) })
                    if (permissionDenied) {
                        androidx.compose.material3.TextButton(onClick = {
                            com.stepup.android.core.ExternalIntents.openAppSettings(context)
                        }) { Text(stringResource(R.string.cd_open_settings)) }
                    }
                }
                TodayEarned(earned)
                RecordWeek(state.week, state.todaySteps, state.loaded)
                GlowCard(contentPadding = HomeCardPadding, spacing = 16.dp) {
                    Text(stringResource(R.string.stat_distance), style = MaterialTheme.typography.bodyMedium, color = Silver)
                    AdaptiveNumber("%.1f km".format(RewardEconomy.distanceMeters(state.todaySteps) / 1000), 28.sp)
                    HairlineDivider()
                    Text(stringResource(R.string.home_run_time), style = MaterialTheme.typography.bodyMedium, color = Silver)
                    AdaptiveNumber(clock(runSec), 28.sp)
                }
                GlowCard(contentPadding = HomeCardPadding, spacing = 12.dp) {
                    Text(stringResource(R.string.home_goal), style = MaterialTheme.typography.titleMedium, color = Snow)
                    AdaptiveNumber("%,d".format(state.todaySteps), 28.sp)
                    Text(stringResource(R.string.home_goal_suffix, "%,d".format(state.goal)), style = MaterialTheme.typography.bodyMedium, color = Silver)
                    BarMeter(fraction = if (state.goal > 0) (state.todaySteps.toFloat() / state.goal).coerceIn(0f, 1f) else 0f, height = 7.dp)
                }

                // ── 챌린지 · 소식 — 작은 보조 진입점 두 개 ──
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_shortcuts_title),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Snow,
                    )
                    val challenge: @Composable (Modifier) -> Unit = { m ->
                        ShortcutButton(
                            icon = Icons.Filled.EmojiEvents,
                            label = stringResource(R.string.home_shortcut_challenges),
                            subtitle = stringResource(R.string.home_shortcut_challenges_sub),
                            onClick = { showDetails = false; onOpenChallenges() },
                            modifier = m,
                        )
                    }
                    val news: @Composable (Modifier) -> Unit = { m ->
                        ShortcutButton(
                            icon = Icons.AutoMirrored.Filled.Article,
                            label = stringResource(R.string.home_shortcut_news),
                            subtitle = stringResource(R.string.home_shortcut_news_sub),
                            onClick = { showDetails = false; onOpenNews() },
                            modifier = m,
                        )
                    }
                    // 큰 글자에서는 반 폭에 제목이 끊긴다 — 위아래로 쌓는다
                    if (largeText) {
                        challenge(Modifier.fillMaxWidth())
                        news(Modifier.fillMaxWidth())
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            challenge(Modifier.weight(1f))
                            news(Modifier.weight(1f))
                        }
                    }
                }

                // ── 에너지 — 러닝을 누르기 직전에 알아야 하는 제한 한 줄 ──
                EnergyLine(earnableSteps = state.earnableSteps, ready = state.loaded && state.maxEnergy > 0)


            }
        }
    }
}

/** Home details share the same spacing as the other record panels. */
private val HomeCardPadding = PaddingValues(20.dp)

/** 오늘 받은 포인트 — 이 화면에서 가장 큰 숫자 */
@Composable
private fun TodayEarned(earned: Double?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(R.string.home_today_earned),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Silver,
        )
        AdaptiveNumber(earned?.let { "+%,.0f".format(it) } ?: "—", 44.sp, color = VoltText, textAlign = TextAlign.Center)
        Text("SUP", style = MaterialTheme.typography.bodyMedium, color = Silver)
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
            .padding(horizontal = 14.dp, vertical = 12.dp),
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

/** 아치 옆 풍경 넘기기 — 48dp 터치, 아이콘은 가볍게 */
@Composable
private fun SceneArrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.size(com.stepup.android.ui.theme.StepUpDesign.TouchTarget)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .feedbackClickable(onClick = onClick)
            .semantics { contentDescription = description; role = androidx.compose.ui.semantics.Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Silver, modifier = Modifier.size(22.dp))
    }
}

/** 초 → "24:18" 또는 "1:04:18" */
private fun clock(totalSec: Long): String {
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
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

@Composable
private fun RunnerShoeLine(shoe: com.stepup.android.domain.Sneaker, onOpen: () -> Unit) {
    val durability = shoe.server?.durabilityPts?.let { "%.0f".format(it) } ?: shoe.durability.toString()
    Row(
        Modifier.heightIn(min = 48.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .feedbackClickable(onClick = onOpen)
            .testTag("home-runner-shoe")
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        com.stepup.android.ui.components.SneakerFrame(shoe, Modifier.width(44.dp).height(30.dp))
        Text(shoe.variantLabel(), color = Snow, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(stringResource(R.string.home_runner_shoe_state, durability), color = Silver, fontSize = 12.sp, maxLines = 1)
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Silver, modifier = Modifier.size(16.dp))
    }
}
