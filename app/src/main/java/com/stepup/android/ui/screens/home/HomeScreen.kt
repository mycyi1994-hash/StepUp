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

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // 일반 폰 높이에서는 한 화면 — 신발 그림이 남는 높이를 쓴다. 아주 낮은 화면 · 큰 글씨면 그림 높이를 두고 넘긴다
        val scroll = largeText || maxHeight < 480.dp
        val short = maxHeight < 560.dp
        Column(
            Modifier.fillMaxSize()
                .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = StepUpDesign.Gutter)
                .padding(top = if (short) 4.dp else 14.dp, bottom = 12.dp)
                .testTag("home-content"),
        ) {
            StepsBlock(
                todaySteps = state.todaySteps, goal = state.goal, loaded = state.loaded,
                hasPermission = hasPermission, firstHome = firstHome,
                onRequestPermission = { requestActivity() },
                onOpenDetails = { showDetails = true },
            )
            Spacer(Modifier.height(if (short) 12.dp else 18.dp))
            HomeShoeCard(
                shoe = state.equipped, loaded = state.loaded, onOpen = onOpenCustomize,
                modifier = if (scroll) Modifier else Modifier.weight(1f),
                imageHeight = if (scroll) 150.dp else null,
            )
            Spacer(Modifier.height(if (short) 12.dp else 16.dp))
            // 이 화면의 주 행동 — 가로로 넓은 러닝 시작(시안: 파란 면 · 흰 글자 · 오른쪽 ▶)
            StartRunButton(
                label = stringResource(if (session.isActive) R.string.cd_resume else R.string.home_start_run),
                onClick = onStartRun,
                modifier = Modifier.guideTarget(GuideTour.Targets.HOME_START_RUN).testTag("home-start-run"),
            )
            Spacer(Modifier.height(10.dp))
            HomeShortcuts(onOpenRecords = onOpenRecords, onOpenCourses = onOpenCourses)
        }
    }

    if (showDetails) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showDetails = false },
            containerColor = com.stepup.android.ui.theme.Night,
        ) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = StepUpDesign.Gutter)
                    .padding(bottom = 24.dp)
                    .testTag("home-details"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!hasPermission) {
                    PermissionStrip(onClick = { requestActivity() })
                    if (permissionDenied) {
                        androidx.compose.material3.TextButton(onClick = {
                            com.stepup.android.core.ExternalIntents.openAppSettings(context)
                        }) { Text(stringResource(R.string.cd_open_settings)) }
                    }
                }
                // 상세 기록과 통계 — "오늘의 걸음"을 누르면 열린다. 챌린지는 내 정보 › 챌린지, 러닝 기록은 기록 보기
                Text(stringResource(R.string.home_k1_records_title), style = MaterialTheme.typography.titleLarge,
                    color = Snow, modifier = Modifier.testTag("home-records-title"))
                TodayEarned(earned)
                RecordWeek(state.week, state.todaySteps, state.loaded)
                GlowCard(contentPadding = HomeCardPadding, spacing = 12.dp) {
                    Text(stringResource(R.string.home_goal), style = MaterialTheme.typography.titleMedium, color = Snow)
                    AdaptiveNumber("%,d".format(state.todaySteps), 28.sp)
                    Text(stringResource(R.string.home_goal_suffix, "%,d".format(state.goal)), style = MaterialTheme.typography.bodyMedium, color = Silver)
                    BarMeter(fraction = if (state.goal > 0) (state.todaySteps.toFloat() / state.goal).coerceIn(0f, 1f) else 0f, height = 7.dp)
                }

                // ── 소식 — 작은 보조 진입점 ──
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
                    ShortcutButton(
                        icon = Icons.AutoMirrored.Filled.Article,
                        label = stringResource(R.string.home_shortcut_news),
                        subtitle = stringResource(R.string.home_shortcut_news_sub),
                        onClick = { showDetails = false; onOpenNews() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // ── 에너지 — 러닝을 누르기 직전에 알아야 하는 제한 한 줄 ──
                EnergyLine(earnableSteps = state.earnableSteps, ready = state.loaded && state.maxEnergy > 0)

                // ── 바탕 풍경 — 예전 홈 양옆 화살표를 여기로 옮겼다(시안의 홈에는 화살표가 없다) ──
                BackgroundRow(weatherScene, onPreviousBackground, onNextBackground)
            }
        }
    }
}

/** Home details share the same spacing as the other record panels. */
private val HomeCardPadding = PaddingValues(20.dp)

// ── 색 ──────────────────────────────────────────────────────────

/** 러닝 홈의 색 — 풍경 사진 위에 얹는다. 시안은 어두운 테마만이라 밝은 테마 값은 거기서 끌어냈다 */
@Immutable
private class HomePalette(
    val label: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val track: Color,
    val barFill: List<Color>,
    val card: Color,
    val cardEdge: Color,
    val divider: Color,
    val chipFace: Color,
    val chipEdge: Color,
    val chipText: Color,
    val startFrom: Color,
    val startTo: Color,
    val row: Color,
    val rowEdge: Color,
)

private val DarkHome = HomePalette(
    label = Color(0xFFD5DDF5), text = Color(0xFFFFFFFF), muted = Color(0xFFA9B6DC), accent = Color(0xFF3F8CFF),
    track = Color(0xFF1B2744), barFill = listOf(Color(0xFF2C77F4), Color(0xFF3A8BFF)),
    card = Color(0xE00B1631), cardEdge = Color(0xFF223561), divider = Color(0xFF223257),
    chipFace = Color(0xFF0E2350), chipEdge = Color(0xFF3968D8), chipText = Color(0xFFDDE7FF),
    startFrom = Color(0xFF1556F2), startTo = Color(0xFF2B7BFF),
    row = Color(0xCC0B1631), rowEdge = Color(0xFF223561),
)

private val LightHome = HomePalette(
    label = Color(0xFF34405E), text = Color(0xFF0E1A33), muted = Color(0xFF5B6886), accent = Color(0xFF1558D6),
    track = Color(0xFFDCE4F2), barFill = listOf(Color(0xFF2167E8), Color(0xFF3A82F5)),
    card = Color(0xEBFFFFFF), cardEdge = Color(0xFFD5DEEE), divider = Color(0xFFE1E7F2),
    chipFace = Color(0xFFE6EEFF), chipEdge = Color(0xFF7FA2EC), chipText = Color(0xFF1B3F9A),
    startFrom = Color(0xFF1556F2), startTo = Color(0xFF2B7BFF),
    row = Color(0xE6FFFFFF), rowEdge = Color(0xFFD5DEEE),
)

@Composable
private fun homePalette(): HomePalette = if (StepUpColors.dark) DarkHome else LightHome

/** 능력치 막대 색 — 효율 파랑 · 착화감 보라 · 내구도 민트(시안 지시 #3988FF · #A18AF5 · #46C5AC) */
private fun BarStat.color(): Color = when (this) {
    BarStat.EFFICIENCY -> Color(0xFF3988FF)
    BarStat.COMFORT -> Color(0xFFA18AF5)
    BarStat.DURABILITY -> Color(0xFF46C5AC)
}

// ── 오늘의 걸음 ──────────────────────────────────────────────────

/**
 * 오늘의 걸음 · 목표 · 달성 비율과 막대. 누르면 상세 기록(오늘 획득 · 7일 걸음 · 소식 · 에너지 · 풍경).
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
) {
    val p = homePalette()
    val detailsLabel = stringResource(R.string.home_open_details)
    val clickable = Modifier.clip(RoundedCornerShape(16.dp))
        .feedbackClickable(role = Role.Button, onClick = onOpenDetails)
        .semantics { onClick(label = detailsLabel) { onOpenDetails(); true } }
    if (firstHome) {
        Column(Modifier.fillMaxWidth().then(clickable).padding(vertical = 4.dp).testTag("home-steps")) {
            Text(
                stringResource(R.string.onb_home_first_title), color = p.text, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                lineHeight = 36.sp, modifier = Modifier.testTag("home-headline"),
            )
            Text(
                stringResource(R.string.onb_home_first_body), color = p.label, fontSize = 15.sp,
                modifier = Modifier.padding(top = 8.dp).testTag("home-first-run"),
            )
        }
        return
    }
    val percent = if (goal > 0) todaySteps * 100.0 / goal else 0.0
    val fraction = if (goal > 0) (todaySteps.toFloat() / goal).coerceIn(0f, 1f) else 0f
    Row(Modifier.fillMaxWidth().then(clickable).padding(vertical = 2.dp).testTag("home-steps"), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1.15f)) {
            Text(stringResource(R.string.home_today_steps), color = p.label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (!hasPermission) {
                Spacer(Modifier.height(8.dp))
                PermissionStrip(onClick = onRequestPermission)
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        if (loaded) "%,d".format(todaySteps) else "—", color = p.text, fontSize = 52.sp,
                        fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp, lineHeight = 60.sp, maxLines = 1,
                        modifier = Modifier.testTag("home-steps-value"),
                    )
                    Text(
                        stringResource(R.string.home_steps_unit), color = p.label, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 6.dp, bottom = 9.dp),
                    )
                }
            }
            Text(
                stringResource(R.string.home_goal_short, "%,d".format(goal)), color = p.label, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, modifier = Modifier.padding(top = 2.dp).testTag("home-goal"),
            )
        }
        Column(Modifier.weight(1f).padding(start = 12.dp, bottom = 2.dp), horizontalAlignment = Alignment.End) {
            Text(
                if (loaded && hasPermission) "%.1f%%".format(percent) else "—", color = p.accent, fontSize = 17.sp,
                fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.testTag("home-goal-percent"),
            )
            Spacer(Modifier.height(8.dp))
            HomeBar(if (loaded && hasPermission) fraction else 0f, p.barFill, Modifier.fillMaxWidth().height(10.dp))
        }
    }
}

/** 둥근 막대 — 같은 트랙, 값이 조금이라도 있으면 둥근 끝이 보일 만큼은 채운다 */
@Composable
private fun HomeBar(fraction: Float, fill: List<Color>, modifier: Modifier) {
    val track = homePalette().track
    Box(
        modifier.clearAndSetSemantics { }.drawBehind {
            val radius = CornerRadius(size.height / 2)
            drawRoundRect(track, cornerRadius = radius)
            if (fraction > 0f) {
                val w = (size.width * fraction).coerceAtLeast(size.height)
                val brush = if (fill.size > 1) Brush.horizontalGradient(fill, endX = w) else Brush.horizontalGradient(listOf(fill[0], fill[0]))
                drawRoundRect(brush, size = Size(w, size.height), cornerRadius = radius)
            }
        },
    )
}

// ── 신고 있는 신발 ────────────────────────────────────────────────

/**
 * 신고 있는 신발 한 장 — 착용 중 · 신발(틀 없이) · 이름 끝 등급 배지 · Lv · 효율 / 착화감 / 내구도 막대. 누르면 신발 탭(내 신발).
 * 신발 그림은 카드 안 남는 높이를 쓴다([imageHeight] 가 있으면 그 높이). 신고 있는 신발이 없으면 신발 탭으로 가는 안내.
 */
@Composable
private fun HomeShoeCard(shoe: Sneaker?, loaded: Boolean, onOpen: () -> Unit, modifier: Modifier, imageHeight: Dp?) {
    val p = homePalette()
    val shape = RoundedCornerShape(24.dp)
    val open = stringResource(R.string.home_shoe_open)
    Column(
        modifier.fillMaxWidth().clip(shape).background(p.card, shape).border(1.dp, p.cardEdge, shape)
            .feedbackClickable(role = Role.Button, onClick = onOpen)
            .semantics { onClick(label = open) { onOpen(); true } }
            .padding(horizontal = 18.dp, vertical = 16.dp)
            .testTag("home-shoe-card"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (shoe == null) {
            Box(Modifier.fillMaxWidth().then(if (imageHeight != null) Modifier.height(imageHeight) else Modifier.weight(1f)))
            if (loaded) {
                Text(stringResource(R.string.home_no_shoe_title), color = p.text, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.testTag("home-no-shoe"))
                Text(stringResource(R.string.home_no_shoe_body), color = p.muted, fontSize = 14.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp))
            }
            Box(Modifier.fillMaxWidth().then(if (imageHeight != null) Modifier.height(24.dp) else Modifier.weight(1f)))
            return@Column
        }
        // 착용 중 · 신발 그림 — 표시는 그림 위 왼쪽 위에 얹는다(따로 한 줄을 차지하지 않게)
        Box(
            Modifier.fillMaxWidth().then(if (imageHeight != null) Modifier.height(imageHeight) else Modifier.weight(1f)),
        ) {
            ShoeArtThumbnail(shoe, Modifier.fillMaxSize().padding(top = 26.dp, bottom = 4.dp).testTag("home-shoe-image"))
            WearingChip(Modifier.align(Alignment.TopStart))
        }
        ShoeNameWithBadge(
            name = shoe.shoeName(), tier = shoe.tier,
            style = TextStyle(color = p.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp),
            maxLines = 2, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("home-shoe-name"),
        )
        Text(
            stringResource(R.string.level_chip, shoe.level), color = p.muted, fontSize = 14.sp,
            modifier = Modifier.padding(top = 2.dp).testTag("home-shoe-level"),
        )
        Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp).height(1.dp).background(p.divider))
        ShoeStats(shoe)
    }
}

/** 착용 중 — 테두리가 있는 작은 파란 알약(누르는 곳이 아니다) */
@Composable
private fun WearingChip(modifier: Modifier) {
    val p = homePalette()
    val shape = RoundedCornerShape(50)
    Text(
        stringResource(R.string.my_shoes_wearing), color = p.chipText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        modifier = modifier.clip(shape).background(p.chipFace, shape).border(1.dp, p.chipEdge, shape)
            .padding(horizontal = 12.dp, vertical = 4.dp).testTag("home-shoe-wearing"),
    )
}

/** 효율 · 착화감 · 내구도 — 같은 폭 세 칸, 사이에 가는 세로선. 값 · 막대는 신발 탭과 같은 계산(MyShoesModel) */
@Composable
private fun ShoeStats(shoe: Sneaker) {
    val p = homePalette()
    val bars = remember(shoe) { statBars(shoe) }
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("home-shoe-stats"), verticalAlignment = Alignment.CenterVertically) {
        bars.forEachIndexed { index, bar ->
            if (index > 0) Box(Modifier.padding(horizontal = 10.dp).width(1.dp).height(48.dp).background(p.divider))
            StatColumn(bar, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatColumn(bar: StatBar, modifier: Modifier) {
    val p = homePalette()
    val (title, tag) = when (bar.stat) {
        BarStat.EFFICIENCY -> stringResource(R.string.my_shoes_stat_efficiency) to "efficiency"
        BarStat.COMFORT -> stringResource(R.string.sdv_stat_comfort) to "comfort"
        BarStat.DURABILITY -> stringResource(R.string.sdv_stat_durability) to "durability"
    }
    Column(modifier.semantics(mergeDescendants = true) { contentDescription = "$title ${bar.value}" }.testTag("home-stat-$tag")) {
        Text(title, color = p.muted, fontSize = 13.sp, maxLines = 1)
        Text(
            bar.value, color = p.text, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp).testTag("home-stat-$tag-value"),
        )
        Spacer(Modifier.height(7.dp))
        HomeBar(bar.fraction, listOf(bar.stat.color()), Modifier.fillMaxWidth().height(7.dp))
    }
}

// ── 러닝 시작 · 기록 보기 · 코스 찾기 ─────────────────────────────

/** 러닝 홈의 주 행동 — 가로로 넓은 파란 알약, 가운데 흰 글자 · 오른쪽 ▶(시안) */
@Composable
private fun StartRunButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = homePalette()
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(p.startFrom, p.startTo)), shape)
            .feedbackClickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1,
            modifier = Modifier.padding(horizontal = 56.dp, vertical = 12.dp))
        Icon(
            Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 22.dp).size(30.dp),
        )
    }
}

/** 기록 보기 · 코스 찾기 — 한 판에 반씩, 가운데 가는 세로선 */
@Composable
private fun HomeShortcuts(onOpenRecords: () -> Unit, onOpenCourses: () -> Unit) {
    val p = homePalette()
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(shape).background(p.row, shape).border(1.dp, p.rowEdge, shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShortcutHalf(Icons.AutoMirrored.Outlined.ReceiptLong, stringResource(R.string.home_open_records), onOpenRecords,
            Modifier.weight(1f).testTag("home-open-records"))
        Box(Modifier.width(1.dp).height(28.dp).background(p.divider))
        ShortcutHalf(Icons.Outlined.Place, stringResource(R.string.home_find_courses), onOpenCourses,
            Modifier.weight(1f).testTag("home-open-courses"))
    }
}

@Composable
private fun ShortcutHalf(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val p = homePalette()
    Row(
        // 줄 높이는 칸이 정한다 — fillMaxHeight 를 쓰면 한 화면 홈(Column)에서 이 줄이 남은 높이를 다 먹어 신발 카드가 사라진다
        modifier.heightIn(min = 52.dp).feedbackClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = p.label, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false))
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = p.muted, modifier = Modifier.size(20.dp))
    }
}

// ── 상세 기록 안 ─────────────────────────────────────────────────

/** 오늘 받은 포인트 — 이 화면에서 가장 큰 숫자. 오늘 카드와 같이 내림(받은 것보다 크게 보이지 않게) */
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
        AdaptiveNumber(earned?.let { "+" + com.stepup.android.ui.components.formatSupDown(it, 2) } ?: "—", 44.sp, color = VoltText, textAlign = TextAlign.Center)
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

