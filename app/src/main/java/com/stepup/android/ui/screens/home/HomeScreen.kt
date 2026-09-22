package com.stepup.android.ui.screens.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.core.AppTheme
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.local.DailyStepsEntity
import com.stepup.android.domain.RewardEconomy
import com.stepup.android.ui.StepPermissions
import com.stepup.android.ui.components.BarMeter
import com.stepup.android.ui.components.DarkIconButton
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.LevelAvatar
import com.stepup.android.ui.components.NeonRing
import com.stepup.android.domain.runnerTitle
import com.stepup.android.ui.components.SneakerFrame
import com.stepup.android.ui.components.fullLabel
import com.stepup.android.ui.components.StartRunButton
import com.stepup.android.ui.components.TokenCard
import com.stepup.android.ui.components.Wordmark
import com.stepup.android.ui.components.animatedInt
import com.stepup.android.ui.components.label
import com.stepup.android.ui.components.quietClickable
import com.stepup.android.ui.components.rememberCustomAvatar
import com.stepup.android.ui.guide.GuideTour
import com.stepup.android.ui.guide.guideTarget
import com.stepup.android.ui.components.tint
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.Volt
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * 홈 — 스크롤 없이 한 화면에 전부 담는다.
 *
 * 고정 높이 요소(상단바 · 인사 · CTA)를 먼저 잡고, 남는 공간을 카드들이
 * weight로 나눠 갖는다. 화면이 작아도 잘리지 않고 비율대로 줄어든다.
 */
@Composable
fun HomeScreen(
    onStartRun: () -> Unit = {},
    onOpenWallet: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenLanguage: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {},
    onOpenItems: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val unread by viewModel.unreadCount.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    var hasPermission by remember {
        mutableStateOf(StepPermissions.hasActivityRecognition(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermission = StepPermissions.hasActivityRecognition(context)
        if (hasPermission) viewModel.onPermissionGranted()
    }

    LifecycleResumeEffect(Unit) {
        val granted = StepPermissions.hasActivityRecognition(context)
        if (granted != hasPermission) {
            hasPermission = granted
            if (granted) viewModel.onPermissionGranted()
        }
        onPauseOrDispose { }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        TopBar(unread, onOpenLanguage, onOpenNotifications, onOpenWallet)

        GreetingRow(
            level = state.level,
            nickname = state.nickname,
            avatarId = state.avatarId,
            avatarRev = state.avatarRev,
            balance = state.balance,
            onOpenWallet = onOpenWallet,
            onOpenProfile = onOpenProfile,
        )

        if (!hasPermission) {
            PermissionStrip(
                onClick = { permissionLauncher.launch(StepPermissions.missing(context)) },
            )
        }

        StepsHeroCard(
            state = state,
            onOpenProfile = onOpenAnalytics,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.30f)
                .guideTarget(GuideTour.Targets.HOME_STEPS),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.12f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            EnergyCard(
                energy = state.energy,
                maxEnergy = state.maxEnergy,
                percent = state.energyPercent,
                earnableSteps = state.earnableSteps,
                earnableSup = state.earnableSup,
                ready = state.loaded && state.maxEnergy > 0,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .guideTarget(GuideTour.Targets.HOME_ENERGY),
            )
            DistanceCard(
                week = state.week,
                onOpenProfile = onOpenAnalytics,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }

        SneakerStrip(
            state = state,
            onOpenItems = onOpenItems,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.74f),
        )

        StartRunButton(
            title = stringResource(R.string.start_run),
            subtitle = stringResource(R.string.start_run_sub),
            onClick = onStartRun,
            modifier = Modifier.guideTarget(GuideTour.Targets.HOME_START_RUN),
        )
    }
}

@Composable
private fun TopBar(
    unread: Int,
    onOpenLanguage: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenWallet: () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = StepUpColors.dark
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Wordmark(fontSize = 20.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // 해·달 — 지금 보이는 것의 반대를 그린다. 어두운 화면에서 해를
            // 보여 줘야 "누르면 밝아진다"로 읽힌다. 지금 상태를 그리면
            // 버튼이 표시등처럼 보여 누를 것으로 읽히지 않는다.
            DarkIconButton(
                icon = if (dark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                contentDescription = stringResource(
                    if (dark) R.string.cd_theme_to_light else R.string.cd_theme_to_dark,
                ),
                onClick = {
                    val next = AppTheme.toggle(systemDark)
                    // 저장은 화면 수명과 무관한 스코프에서 — 누르자마자
                    // 화면을 옮겨도 선택이 남아야 한다
                    CoroutineScope(Dispatchers.IO).launch {
                        ServiceLocator.userPrefs.setThemeMode(next.name)
                    }
                },
            )
            DarkIconButton(
                icon = Icons.Filled.Language,
                contentDescription = stringResource(R.string.cd_language),
                onClick = onOpenLanguage,
            )
            DarkIconButton(
                icon = Icons.Filled.Notifications,
                contentDescription = stringResource(R.string.cd_notifications),
                onClick = onOpenNotifications,
                badge = unread > 0,
            )
            DarkIconButton(
                icon = Icons.Filled.AccountBalanceWallet,
                contentDescription = stringResource(R.string.cd_wallet),
                onClick = onOpenWallet,
            )
        }
    }
}

@Composable
private fun GreetingRow(
    level: Int,
    nickname: String,
    avatarId: Int,
    avatarRev: Int,
    balance: Double,
    onOpenWallet: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val greetingRes = when (LocalTime.now().hour) {
        in 0..4 -> R.string.greeting_dawn
        in 5..10 -> R.string.greeting_morning
        in 11..16 -> R.string.greeting_afternoon
        else -> R.string.greeting_evening
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LevelAvatar(
            avatarId = avatarId,
            customBitmap = rememberCustomAvatar(avatarRev),
            level = level,
            size = 46.dp,
            modifier = Modifier.quietClickable(onOpenProfile),
            contentDescription = stringResource(R.string.cd_profile),
        )
        // 인사말 덩어리째 프로필로 가는 문이다. 아바타만 눌리게 두면
        // 이름을 누른 사람은 아무 일도 안 일어나는 화면을 보게 된다 —
        // 여기서 자기 이름을 부르고 있으니 누르면 자기 화면이 나와야 한다.
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .quietClickable(onOpenProfile)
                .padding(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = stringResource(greetingRes),
                fontSize = 11.sp,
                color = Silver,
            )
            Text(
                // 닉네임을 정했으면 그 이름으로 부른다. 방금 이름을 정하고
                // 홈으로 왔는데 여전히 "러너님"이면 설정이 안 먹은 줄 안다.
                text = if (nickname.isBlank()) {
                    stringResource(R.string.greeting_runner)
                } else {
                    stringResource(R.string.greeting_named, nickname)
                },
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
                color = Snow,
            )
            Text(
                text = stringResource(R.string.level_chip, level) + " · " + runnerTitle(level).label(),
                fontSize = 10.sp,
                color = Volt,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Box(Modifier.guideTarget(GuideTour.Targets.HOME_TOKEN)) {
            TokenCard(balance = balance, onClick = onOpenWallet)
        }
    }
}

@Composable
private fun PermissionStrip(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Volt.copy(alpha = 0.12f))
            .quietClickable(onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(
            Icons.AutoMirrored.Filled.DirectionsWalk,
            contentDescription = null,
            tint = Volt,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.perm_allow),
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Volt,
        )
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Volt,
            modifier = Modifier.size(15.dp),
        )
    }
}

/** 오늘 걸음 히어로 — 좌측 큰 숫자 + 우측 루트 맵 + 목표 진행 바 */
/**
 * 오늘 걸음을 재미있는 비교 문구로 풀어준다.
 * 1km 단위 100개 문구(랜드마크 비교)가 리소스에 있고, 오늘 거리(km)에 맞는 문구를 고른다.
 */
@Composable
private fun DistanceFactPanel(todaySteps: Int, modifier: Modifier = Modifier) {
    val facts = stringArrayResource(R.array.distance_facts)
    val km = todaySteps * RewardEconomy.STRIDE_METERS / 1000.0
    val index = km.toInt().coerceAtMost(facts.size)
    val text = if (index >= 1) facts[index - 1] else stringResource(R.string.distance_fact_none)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Volt.copy(alpha = 0.07f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = "%.2f km".format(km),
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.4).sp,
                color = Volt,
            )
            Text(
                text = text,
                fontSize = 10.5.sp,
                lineHeight = 14.sp,
                color = Silver,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 4,
            )
        }
    }
}

@Composable
private fun StepsHeroCard(
    state: HomeViewModel.UiState,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fraction = if (state.goal > 0) state.todaySteps.toFloat() / state.goal else 0f
    val steps = animatedInt(state.todaySteps)

    GlowCard(
        modifier = modifier,
        accent = true,
        contentPadding = PaddingValues(15.dp),
        spacing = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_total_steps),
                    fontSize = 11.sp,
                    color = Silver,
                )
                Text(
                    text = "%,d".format(steps),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1.8).sp,
                    color = Snow,
                )
                Text(
                    text = stringResource(R.string.home_daily_goal, "%,d".format(state.goal)),
                    fontSize = 10.sp,
                    color = Slate,
                )
            }
            DistanceFactPanel(
                todaySteps = state.todaySteps,
                modifier = Modifier
                    .weight(0.82f)
                    .fillMaxHeight()
                    .padding(vertical = 2.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            BarMeter(fraction = fraction, modifier = Modifier.weight(1f), height = 7.dp)
            Text(
                text = "${state.goalPercent.coerceAtMost(999)}%",
                color = Volt,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = stringResource(R.string.home_view_analytics),
                tint = Slate,
                modifier = Modifier
                    .size(16.dp)
                    .quietClickable(onOpenProfile),
            )
        }
    }
}

/** 에너지 카드 — 네온 링 + 자정까지 리필 카운트다운 */
@Composable
private fun EnergyCard(
    energy: Double,
    maxEnergy: Double,
    percent: Int,
    /** 남은 에너지로 아직 적립할 수 있는 걸음 */
    earnableSteps: Int,
    /** 그 걸음을 다 걸었을 때 받는 SUP */
    earnableSup: Double,
    /** 실제 값이 도착했는가. 아직이면 비율을 짓지 않고 트랙만 그린다. */
    ready: Boolean,
    modifier: Modifier = Modifier,
) {
    var secondsLeft by remember { mutableIntStateOf(86_400 - LocalTime.now().toSecondOfDay()) }
    LaunchedEffect(Unit) {
        while (true) {
            secondsLeft = 86_400 - LocalTime.now().toSecondOfDay()
            delay(1_000)
        }
    }
    val countdown = "%02d:%02d:%02d".format(
        secondsLeft / 3600,
        (secondsLeft % 3600) / 60,
        secondsLeft % 60,
    )

    GlowCard(modifier = modifier, contentPadding = PaddingValues(13.dp), spacing = 6.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(Icons.Filled.Bolt, contentDescription = null, tint = Volt, modifier = Modifier.size(13.dp))
            Text(
                text = stringResource(R.string.home_energy),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Snow,
            )
        }
        // 잔량 게이지. 12시에서 출발해 시계방향으로 남은 만큼 채운다.
        //
        // 숫자를 고리 한가운데 둔다. 글자가 테두리에 닿지 않도록 크기를
        // 고리의 안지름에서 계산한다 — 카드 폭은 화면마다 다르고, 글자를
        // 한 값으로 못 박으면 좁은 기기에서 링에 걸친다.
        val gaugeCd = if (ready) {
            stringResource(R.string.cd_home_energy_gauge, percent)
        } else {
            stringResource(R.string.cd_home_energy_gauge_unknown)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .semantics { contentDescription = gaugeCd },
            contentAlignment = Alignment.Center,
        ) {
            // aspectRatio 로 정사각형을 못 박는 것은 여전히 필요하다 —
            // 높이만 채우게 두면 폭이 0이 되어 원이 사라진다.
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center,
            ) {
                val ringWidth = 7.dp
                // 고리 안쪽에 실제로 비는 지름. NeonRing 이 선 굵기의 절반과
                // 6dp 를 안쪽으로 물리므로 그만큼을 빼고 남는 자리다.
                val inner = maxWidth - ringWidth * 3 - 12.dp
                val size = (inner.value * 0.34f).coerceIn(11f, 22f).sp
                NeonRing(
                    progress = percent / 100f,
                    modifier = Modifier.fillMaxSize(),
                    ringWidth = ringWidth,
                    // 값이 바뀔 때 250~400ms 로 부드럽게. 기기에서 애니메이션을
                    // 꺼 두었으면 NeonRing 이 바로 그린다.
                    durationMillis = 320,
                    inactive = !ready,
                ) {
                    Text(
                        text = if (ready) "$percent%" else "—",
                        fontSize = size,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        color = if (ready) Snow else Slate,
                        maxLines = 1,
                    )
                }
            }
        }
        // %가 무엇의 %인지 — 오늘 남은 적립 여력이다. 고리 아래 별도 줄로
        // 둔다. "N 보"와 "N SUP"를 한 줄에 붙여 카드 높이를 늘리지 않는다.
        Text(
            text = if (ready) {
                stringResource(
                    R.string.home_energy_left,
                    "%,d".format(earnableSteps),
                    "%,.0f".format(earnableSup),
                )
            } else {
                stringResource(R.string.home_energy_loading)
            },
            fontSize = 10.sp,
            color = if (ready) Snow else Slate,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Text(
            text = stringResource(R.string.home_recharge_in, countdown),
            fontSize = 10.sp,
            color = Volt,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** 거리 카드 — 주간 미니 바 차트 */
@Composable
private fun DistanceCard(
    week: List<DailyStepsEntity>,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now().toEpochDay()
    val days = (0..6).map { offset -> today - 6 + offset }
    val byDay = week.associateBy { it.epochDay }
    val weekSteps = days.sumOf { (byDay[it]?.steps ?: 0).toLong() }
    val weekKm = RewardEconomy.distanceMeters(weekSteps.toInt()) / 1000
    val maxSteps = maxOf(week.maxOfOrNull { it.steps } ?: 0, 1)

    GlowCard(
        modifier = modifier.quietClickable(onOpenProfile),
        contentPadding = PaddingValues(13.dp),
        spacing = 6.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Volt, modifier = Modifier.size(13.dp))
            Text(
                text = stringResource(R.string.home_distance),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Snow,
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "%.2f".format(weekKm),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp,
                color = Snow,
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = "km",
                fontSize = 11.sp,
                color = Slate,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
        // 막대와 요일을 **한 칸 안에 같이** 둔다.
        //
        // 예전에는 막대 줄과 요일 줄이 따로 있었다. 둘 다 7등분이라 맞아떨어질
        // 것 같지만, 요일 동그라미는 16dp 고정이라 칸 너비와 어긋나 조금씩
        // 밀렸다 — 화면에서는 동그라미와 요일이 안 맞는 것으로 보인다.
        // 한 칸(Column)에 막대와 요일을 세로로 쌓으면 어긋날 자리가 없다.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            days.forEach { day ->
                val steps = byDay[day]?.steps ?: 0
                val fraction = (steps.toFloat() / maxSteps).coerceIn(0.08f, 1f)
                val isToday = day == today
                val label = LocalDate.ofEpochDay(day).dayOfWeek
                    .getDisplayName(TextStyle.NARROW, Locale.getDefault())

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // 막대가 설 자리는 남은 높이를 통째로 갖고, 막대는 그 안에서
                    // 비율만큼만 채운다.
                    //
                    // 예전에는 막대 자체에 weight(비율)를 줬는데, 한 칸에 무게를
                    // 가진 자식이 하나뿐이면 무게 값과 상관없이 남은 공간을 전부
                    // 가져간다 — 7일이 전부 꽉 찬 막대로 나왔다.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isToday) Volt else Volt.copy(alpha = 0.30f)),
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    // 동그라미 없이 글자만. 오늘은 색과 굵기로 구분한다 —
                    // 칸 하나가 15dp 남짓이라 동그라미를 넣으면 요일 일곱 개가
                    // 서로 밀려 막대와 어긋나 보인다.
                    Text(
                        text = label,
                        color = if (isToday) Volt else Slate,
                        fontSize = 9.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** 착용 스니커즈 요약 — 실제 NFT 아트를 보여준다 */
@Composable
private fun SneakerStrip(
    state: HomeViewModel.UiState,
    onOpenItems: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sneaker = state.equipped
    GlowCard(
        modifier = modifier.quietClickable(onOpenItems),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        spacing = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (sneaker != null) {
                SneakerFrame(
                    sneaker = sneaker,
                    modifier = Modifier
                        .weight(0.62f)
                        .fillMaxHeight(),
                    corner = 12.dp,
                )
            } else {
                Spacer(Modifier.weight(0.62f))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = if (sneaker != null) {
                        sneaker.fullLabel()
                    } else {
                        stringResource(R.string.common_none)
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Snow,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // 신발 칸에는 **신발의** 레벨을 쓴다.
                    //
                    // 여기에 러너 레벨(state.level)이 들어가 있었다. 강화로
                    // Lv.8 이 된 신발을 신고도 홈에서는 Lv.1 로 보였고,
                    // 그러면 강화에 쓴 SUP 가 어디로 갔는지 알 수 없다.
                    if (sneaker != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(CarbonHigh)
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.level_chip, sneaker.level),
                                color = Volt,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Text(
                        text = "+%.1f%%".format(sneaker?.boostPercent ?: 0.0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = sneaker?.faction?.tint() ?: Silver,
                    )
                }
                BarMeter(
                    fraction = (sneaker?.durability ?: 85) / 100f,
                    height = 5.dp,
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Slate,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
