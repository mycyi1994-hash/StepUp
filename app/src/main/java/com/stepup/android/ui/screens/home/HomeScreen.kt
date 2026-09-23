package com.stepup.android.ui.screens.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Shadow
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
import com.stepup.android.ui.components.GoalBar
import com.stepup.android.ui.components.PrimaryCta
import com.stepup.android.ui.components.CharacterStage
import com.stepup.android.ui.components.AvatarLookNote
import com.stepup.android.ui.components.ShortcutButton
import com.stepup.android.ui.components.SmallBadge
import com.stepup.android.ui.components.StatCell
import com.stepup.android.ui.components.SupPill
import com.stepup.android.ui.components.VerticalHairline
import com.stepup.android.ui.components.Wordmark
import com.stepup.android.ui.components.quietClickable
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.guide.GuideTour
import com.stepup.android.ui.guide.guideTarget
import com.stepup.android.ui.theme.Alert
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.StepUpNumbers
import com.stepup.android.ui.theme.Volt
import com.stepup.android.ui.theme.VoltInk
import com.stepup.android.ui.theme.VoltText
import java.time.LocalTime
import kotlinx.coroutines.delay

/**
 * 러닝 — 앱을 열면 가장 먼저 보이는 화면.
 *
 * 세 가지가 바로 보여야 한다.
 *
 *   1. 오늘 받은 포인트
 *   2. 내 캐릭터
 *   3. 러닝 시작
 *
 * 나머지(거리 · 운동 시간 · 목표)는 그 셋을 받쳐 주는 줄이다. 예전 홈의 큰
 * 에너지 원형 차트, 겹치던 거리 카드, 통계 여러 장은 이 화면에서 뺐다.
 * 에너지는 러닝 시작 바로 위에 한 줄로만 남긴다 — 러닝을 누르기 직전에
 * 알아야 하는 제한이기 때문이다.
 */
@Composable
fun HomeScreen(
    onStartRun: () -> Unit = {},
    onOpenWallet: () -> Unit = {},
    onOpenChallenges: () -> Unit = {},
    onOpenNews: () -> Unit = {},
    onOpenCustomize: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val earned by viewModel.todayEarned.collectAsStateWithLifecycle()
    val runSec by viewModel.todayRunSec.collectAsStateWithLifecycle()
    val look by viewModel.look.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(StepPermissions.hasActivityRecognition(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
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

    val largeText = LocalDensity.current.fontScale > 1.2f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── 머리글 48dp — 로고와 작은 보유 포인트 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Wordmark(fontSize = 24.sp, modifier = Modifier.weight(1f, fill = false))
            Box(Modifier.weight(1f))
            SupPill(
                balance = state.balance,
                onClick = onOpenWallet,
                modifier = Modifier.guideTarget(GuideTour.Targets.HOME_TOKEN),
            )
        }

        if (!hasPermission) {
            PermissionStrip(onClick = { permissionLauncher.launch(StepPermissions.missing(context)) })
        }

        // ── 1. 오늘 받은 포인트 + 2. 내 캐릭터(누르면 꾸미기) ──
        //
        // 숫자와 캐릭터를 한 덩어리로 둔다 — 캐릭터 무대의 조명이 숫자 뒤까지
        // 번져 두 요소가 따로 떠 보이지 않는다.
        val characterCd = stringResource(R.string.cd_home_character)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            TodayEarned(earned)
            CharacterStage(
                look = look,
                pose = AvatarPose.RUN,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (largeText) 220.dp else 280.dp)
                    .quietClickable(onOpenCustomize),
                characterFraction = 0.9f,
                contentDescription = characterCd,
            ) { _ ->
                if (look.trial) {
                    SmallBadge(
                        text = stringResource(R.string.avatar_trial),
                        tone = BadgeTone.Glow,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                    )
                }
            }
            // 그림 속 착장이 실제 착장과 다르면 여기서 밝힌다
            AvatarLookNote(
                look = look,
                render = AvatarArtCatalog.resolve(look, AvatarPose.RUN),
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        // ── 오늘 거리 · 운동 시간 (≈60dp) ──
        GlowCard(contentPadding = HomeCardPadding) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatCell(
                    icon = Icons.Filled.LocationOn,
                    value = "%.1f".format(RewardEconomy.distanceMeters(state.todaySteps) / 1000),
                    unit = "km",
                    label = stringResource(R.string.stat_distance),
                    modifier = Modifier.weight(1f),
                )
                VerticalHairline(height = 44.dp)
                StatCell(
                    icon = Icons.Filled.Timer,
                    value = clock(runSec),
                    unit = "",
                    label = stringResource(R.string.home_run_time),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                )
            }
        }

        // ── 오늘의 목표 — 걸음 목표. 단위를 바꾸지 않는다 (≈56dp) ──
        GlowCard(contentPadding = HomeCardPadding) {
            GoalBar(
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                title = stringResource(R.string.home_goal),
                value = "%,d".format(state.todaySteps),
                suffix = stringResource(R.string.home_goal_suffix, "%,d".format(state.goal)),
                fraction = if (state.goal > 0) state.todaySteps.toFloat() / state.goal else 0f,
            )
        }

        // ── 챌린지 · 소식 ──
        // 큰 글자에서는 반 폭에 "챌린지"가 "챌린 / 지"로 끊긴다 — 위아래로 쌓는다.
        val shortcutsModifier = Modifier
            .fillMaxWidth()
            .guideTarget(GuideTour.Targets.HOME_SHORTCUTS)
        if (largeText) {
            Column(modifier = shortcutsModifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ShortcutButton(
                    icon = Icons.Filled.EmojiEvents,
                    label = stringResource(R.string.home_shortcut_challenges),
                    onClick = onOpenChallenges,
                    modifier = Modifier.fillMaxWidth(),
                )
                ShortcutButton(
                    icon = Icons.AutoMirrored.Filled.Article,
                    label = stringResource(R.string.home_shortcut_news),
                    onClick = onOpenNews,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Row(modifier = shortcutsModifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ShortcutButton(
                    icon = Icons.Filled.EmojiEvents,
                    label = stringResource(R.string.home_shortcut_challenges),
                    onClick = onOpenChallenges,
                    modifier = Modifier.weight(1f),
                )
                ShortcutButton(
                    icon = Icons.AutoMirrored.Filled.Article,
                    label = stringResource(R.string.home_shortcut_news),
                    onClick = onOpenNews,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // ── 에너지 — 러닝을 누르기 직전에 알아야 하는 제한 한 줄 ──
        EnergyLine(earnableSteps = state.earnableSteps, ready = state.loaded && state.maxEnergy > 0)

        // ── 3. 러닝 시작 ──
        PrimaryCta(
            text = stringResource(R.string.home_start_run),
            icon = Icons.AutoMirrored.Filled.DirectionsRun,
            onClick = onStartRun,
            modifier = Modifier.guideTarget(GuideTour.Targets.HOME_START_RUN),
        )
    }
}

/** 홈의 얇은 카드 — 지표 60dp · 목표 56dp 안팎 */
private val HomeCardPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)

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
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                // 아직 못 읽었으면 대시. "0"은 정말 못 번 날에만 보인다.
                text = earned?.let { "+%,.0f".format(it) } ?: "—",
                // 포인트는 빛 번짐을 쓰는 몇 안 되는 자리다
                style = TextStyle(
                    brush = VoltInk,
                    shadow = Shadow(color = Volt.copy(alpha = 0.55f), blurRadius = 28f),
                ),
                fontFamily = StepUpNumbers,
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
                maxLines = 1,
            )
            Text(
                text = " SUP",
                fontFamily = StepUpNumbers,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = VoltText,
                modifier = Modifier.padding(bottom = 7.dp),
            )
        }
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
            fontSize = 13.sp,
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

/** 초 → "24:18" 또는 "1:04:18" */
private fun clock(totalSec: Long): String {
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
