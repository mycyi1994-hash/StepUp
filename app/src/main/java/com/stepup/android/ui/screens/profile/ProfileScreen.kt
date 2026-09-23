package com.stepup.android.ui.screens.profile

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.filled.Tune
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.BuildConfig
import com.stepup.android.R
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.stepup.android.ui.theme.StepUpNumbers
import com.stepup.android.ui.theme.VoltText
import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.components.DarkIconButton
import com.stepup.android.ui.components.StepUpIcons
import com.stepup.android.ui.components.ShortcutButton
import com.stepup.android.ui.components.AvatarBadge
import com.stepup.android.data.local.DailyStepsEntity
import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.domain.RewardEconomy
import com.stepup.android.domain.runnerTitle
import com.stepup.android.ui.components.AvatarEmojis
import com.stepup.android.ui.components.BarMeter
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.HairlineDivider
import com.stepup.android.ui.components.HexEmblem
import com.stepup.android.ui.components.LevelAvatar
import com.stepup.android.ui.components.NeonRing
import com.stepup.android.ui.components.SectionHeader
import com.stepup.android.ui.components.TokenCard
import com.stepup.android.ui.components.VerticalHairline
import com.stepup.android.ui.components.VoltButton
import com.stepup.android.ui.components.label
import com.stepup.android.ui.components.quietClickable
import com.stepup.android.ui.components.rememberCustomAvatar
import com.stepup.android.ui.guide.GuideTour
import com.stepup.android.ui.guide.guideTarget
import com.stepup.android.ui.theme.Carbon
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.OnVolt
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ProfileScreen(
    onOpenGuide: () -> Unit = {},
    onOpenWallet: () -> Unit = {},
    onOpenAchievements: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {},
    onOpenNotificationSettings: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenSupport: () -> Unit = {},
    onOpenConnected: () -> Unit = {},
    onOpenLanguage: () -> Unit = {},
    onOpenTheme: () -> Unit = {},
    onOpenExperience: () -> Unit = {},
    onOpenItems: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val look by viewModel.look.collectAsStateWithLifecycle()
    val recentRuns by viewModel.recentRuns.collectAsStateWithLifecycle()
    val demo by viewModel.demoMode.collectAsStateWithLifecycle()
    // 사진과 이름을 한 창에서 고친다. 나눠 두면 "프로필 편집"을 눌렀는데
    // 이름은 못 바꾸는, 이름이 기능과 어긋나는 상태가 된다.
    // 탭 안의 탭 — 프로필(기록)과 설정.
    //
    // 설정은 원래 가로로 미는 알약 줄이었다. 열한 개가 한 줄에 들어가지 않아
    // 여섯째부터는 밀어야 보였고, 밀 수 있다는 표시도 없어서 거기 있는 줄
    // 모르는 항목이 생겼다. 세로 목록이면 한눈에 다 보인다.
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showProfileEdit by rememberSaveable { mutableStateOf(false) }
    var showGoalDialog by rememberSaveable { mutableStateOf(false) }

    // 갤러리 사진 선택 — 시스템 포토 피커 (권한 불필요)
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        // 사진을 고르면 창은 열어 둔다 — 이름도 여기서 마저 고칠 수 있어야 한다.
        if (uri != null) viewModel.setCustomAvatar(uri)
    }

    if (showProfileEdit) {
        ProfileEditDialog(
            nickname = state.nickname,
            selectedAvatar = state.avatarId,
            avatarRev = state.avatarRev,
            onPickAvatar = viewModel::setAvatar,
            onPickGallery = {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onSave = { name ->
                viewModel.setNickname(name)
                showProfileEdit = false
            },
            onDismiss = { showProfileEdit = false },
        )
    }

    if (showGoalDialog) {
        GoalDialog(
            goal = state.goal,
            onGoalChange = viewModel::setGoal,
            onDismiss = { showGoalDialog = false },
        )
    }

    val pills = listOf(
        SettingsPill(Icons.Filled.Edit, R.string.profile_edit_profile) { showProfileEdit = true },
        SettingsPill(Icons.Filled.Flag, R.string.profile_set_goal) { showGoalDialog = true },
        SettingsPill(Icons.Filled.Inbox, R.string.settings_inbox, onOpenNotifications),
        SettingsPill(Icons.Filled.Notifications, R.string.settings_notifications, onOpenNotificationSettings),
        SettingsPill(Icons.Filled.Link, R.string.settings_connected, onOpenConnected),
        SettingsPill(Icons.Filled.SupportAgent, R.string.settings_support, onOpenSupport),
        SettingsPill(Icons.Filled.Language, R.string.settings_language, onOpenLanguage),
        SettingsPill(Icons.Filled.Tune, R.string.settings_experience, onOpenExperience),
        SettingsPill(Icons.Filled.DarkMode, R.string.settings_theme, onOpenTheme),
        SettingsPill(Icons.Filled.AccountBalanceWallet, R.string.settings_wallet, onOpenWallet),
        SettingsPill(Icons.AutoMirrored.Filled.DirectionsWalk, R.string.profile_my_sneakers, onOpenItems),
        SettingsPill(Icons.AutoMirrored.Filled.MenuBook, R.string.settings_guide, onOpenGuide),
        SettingsPill(Icons.Filled.Security, R.string.settings_privacy, onOpenPrivacy),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ── 설정 — 안쪽 화면. 돌아가는 길을 맨 위에 둔다 ──
        if (tab == 1) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DarkIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        onClick = { tab = 0 },
                    )
                    Text(
                        text = stringResource(R.string.profile_tab_settings),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Snow,
                    )
                }
            }
            item { SectionHeader(title = stringResource(R.string.profile_account)) }
            items(pills) { pill ->
                SettingsRow(
                    icon = pill.icon,
                    label = stringResource(pill.label),
                    onClick = pill.onClick,
                )
            }
            // 데모 모드 — 서버 없이 화면을 둘러보는 모드. 운영 데이터와 섞이지 않는다.
            item { DemoModeRow(on = demo, onChange = viewModel::setDemoMode) }
            item {
                GlowCard(contentPadding = PaddingValues(16.dp), spacing = 9.dp) {
                    AboutRow(
                        label = stringResource(R.string.about_version),
                        value = "StepUp " + BuildConfig.VERSION_NAME,
                    )
                    AboutRow(
                        label = stringResource(R.string.about_network),
                        value = stringResource(R.string.about_network_value),
                    )
                }
            }
            return@LazyColumn
        }

        // ── 작은 캐릭터 · 닉네임 · 레벨 ──
        item {
            MeHeader(
                state = state,
                look = look,
                onEditProfile = { showProfileEdit = true },
                onOpenCustomize = onOpenItems,
                onOpenSettings = { tab = 1 },
            )
        }

        // ── 보유 포인트 · 적립 내역 ──
        item { PointsCard(balance = state.balance, onOpen = onOpenWallet) }

        // ── 이번 주 러닝 거리 + 주간 그래프 ──
        item { WeekCard(week = state.week, onOpen = onOpenAnalytics) }

        // ── 최근 러닝 기록 ──
        item { RecentRunsCard(runs = recentRuns, onOpenAll = onOpenAnalytics) }

        // ── 업적 — 기존 자리 그대로 ──
        item {
            Box(Modifier.guideTarget(GuideTour.Targets.PROFILE_ACHIEVEMENTS)) {
                BadgesCard(state, onOpenAchievements)
            }
        }

        // ── 내 아이템 · 설정 ──
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ShortcutButton(
                    icon = StepUpIcons.Shirt,
                    label = stringResource(R.string.me_items),
                    onClick = onOpenItems,
                    modifier = Modifier.weight(1f),
                )
                ShortcutButton(
                    icon = Icons.Filled.Settings,
                    label = stringResource(R.string.profile_tab_settings),
                    onClick = { tab = 1 },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** 설정 한 줄 — 아이콘 · 이름 · 들어가는 화살표 */
@Composable
private fun SettingsRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CarbonHigh)
            .border(1.dp, Edge, RoundedCornerShape(18.dp))
            .quietClickable(onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Volt, modifier = Modifier.size(19.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = Snow,
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Slate,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** 설정 필 칩 하나 — 아이콘 + 라벨 + 탭 액션 */
private data class SettingsPill(
    val icon: ImageVector,
    val label: Int,
    val onClick: () -> Unit,
)

/** 초 → H:MM:SS */
private fun formatDuration(totalSec: Long): String {
    val h = totalSec / 3600
    val m = totalSec % 3600 / 60
    val s = totalSec % 60
    return "%d:%02d:%02d".format(h, m, s)
}

/** 최근 7일 슬롯(오래된 날 → 오늘). DB에 없는 날은 null */
private fun weekSlots(week: List<DailyStepsEntity>): List<Pair<LocalDate, DailyStepsEntity?>> {
    val byDay = week.associateBy { it.epochDay }
    val today = LocalDate.now()
    return (6 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong())
        date to byDay[date.toEpochDay()]
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Silver)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Snow)
    }
}

@Composable
private fun ProfileHeader(
    state: ProfileViewModel.UiState,
    onEditAvatar: () -> Unit,
    onOpenWallet: () -> Unit,
) {
    GlowCard(accent = true, contentPadding = PaddingValues(20.dp), spacing = 14.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box {
                LevelAvatar(
                    level = state.runner.level,
                    size = 62.dp,
                    contentDescription = stringResource(R.string.cd_profile),
                    avatarId = state.avatarId,
                    customBitmap = rememberCustomAvatar(state.avatarRev),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(CarbonHigh)
                        .border(1.dp, Volt.copy(alpha = 0.6f), CircleShape)
                        .quietClickable(onEditAvatar),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.profile_edit_avatar),
                        tint = Volt,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = stringResource(R.string.profile_hello),
                    style = MaterialTheme.typography.bodySmall,
                    color = Silver,
                )
                // 좁은 화면에서도 한 줄로 읽히게 — 줄바꿈을 막고 넘치면 줄임표
                Text(
                    text = state.nickname.ifBlank { stringResource(R.string.greeting_runner) },
                    style = MaterialTheme.typography.headlineSmall,
                    color = Snow,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    HexEmblem(size = 14.dp, glow = false)
                    Text(
                        text = stringResource(R.string.level_chip, state.runner.level) +
                            " · " + runnerTitle(state.runner.level).label(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Volt,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (state.runnerUid.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(CarbonHigh)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.profile_uid, state.runnerUid),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                            color = Slate,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }

        // 토큰 카드는 아래 한 줄을 통째로 쓴다 — 인사말 칸을 좁혀 글자가
        // 세로로 접히던 문제를 없앤다.
        TokenCard(
            balance = state.balance,
            onClick = onOpenWallet,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.level_chip, state.runner.level),
                style = MaterialTheme.typography.titleSmall,
                color = Volt,
            )
            Text(
                text = if (state.runner.isMax) {
                    stringResource(R.string.profile_level_max)
                } else {
                    stringResource(
                        R.string.profile_level_progress,
                        "%.1f".format(state.runner.intoLevelKm),
                        "%.0f".format(state.runner.levelSpanKm),
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = Silver,
            )
        }
        BarMeter(fraction = state.runner.progress)
        Text(
            text = if (state.runner.isMax) {
                stringResource(R.string.profile_level_hint_max)
            } else {
                stringResource(R.string.profile_level_hint, "%.1f".format(state.runner.remainingKm))
            },
            fontSize = 10.sp,
            color = Slate,
        )
    }
}

/** 실데이터 기반 업적 미리보기: 마라톤 / 스텝 킹 / 스트릭 / 컬렉터 */
private fun previewAchievements(state: ProfileViewModel.UiState): List<Boolean> = listOf(
    state.lifetimeKm >= 100.0,
    state.lifetimeSteps >= 1_000_000L,
    state.streak >= 30,
    state.ownedSneakers >= 3,
)

/** 나의 기록 — 누적 걸음/거리/칼로리/운동시간 4열 */
@Composable
private fun RecordsCard(state: ProfileViewModel.UiState, onOpenAnalytics: () -> Unit) {
    val labels = listOf(stringResource(R.string.profile_lifetime_steps), stringResource(R.string.profile_total_distance),
        stringResource(R.string.profile_total_calories), stringResource(R.string.profile_total_time))
    val values = listOf("%,d".format(state.lifetimeSteps), "%.2f km".format(state.lifetimeKm),
        "%,.0f kcal".format(state.lifetimeCalories), if (state.totalDurationSec > 0) formatDuration(state.totalDurationSec) else "—")
    val columns = if (LocalDensity.current.fontScale > 1.3f) 1 else 2
    GlowCard(modifier = Modifier.quietClickable(onOpenAnalytics)) {
        SectionHeader(stringResource(R.string.profile_my_records))
        labels.indices.chunked(columns).forEach { group ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                group.forEach { i ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(labels[i], style = MaterialTheme.typography.bodySmall, color = Silver)
                        Text(values[i], style = MaterialTheme.typography.titleLarge, color = Snow)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.RecordCell(
    label: String,
    value: String,
    unit: String? = null,
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            color = Slate,
            textAlign = TextAlign.Center,
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Snow,
            textAlign = TextAlign.Center,
        )
        if (unit != null) {
            Text(
                text = unit,
                fontSize = 8.5.sp,
                color = Slate,
            )
        }
    }
}

/** 누적 거리 카드 — 지구 한 바퀴 진행률 + 미니 행성 */
@Composable
private fun RowScope.DistanceCard(state: ProfileViewModel.UiState, onOpenAnalytics: () -> Unit) {
    val earthPercent = state.lifetimeKm / 40075.0 * 100
    GlowCard(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 250.dp)
            .quietClickable(onOpenAnalytics),
        contentPadding = PaddingValues(16.dp),
        spacing = 7.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.DirectionsRun,
                contentDescription = null,
                tint = Volt,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = stringResource(R.string.profile_total_distance),
                fontSize = 11.sp,
                color = Silver,
            )
        }
        Text(
            text = "%.2f km".format(state.lifetimeKm),
            fontSize = 21.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
            color = Volt,
        )
        Text(
            text = stringResource(R.string.profile_earth, "%.3f".format(earthPercent)),
            fontSize = 9.sp,
            lineHeight = 13.sp,
            color = Slate,
        )
        Spacer(Modifier.height(12.dp))
        // 미니 행성 — 방사형 볼트 그라데이션 원 + 점선 궤도 + 위성 점
        Canvas(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(70.dp),
        ) {
            val r = size.minDimension / 2f * 0.58f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Volt.copy(alpha = 0.95f),
                        Volt.copy(alpha = 0.30f),
                        Color.Transparent,
                    ),
                    center = Offset(center.x - r * 0.3f, center.y - r * 0.3f),
                    radius = r * 1.7f,
                ),
                radius = r,
                center = center,
            )
            drawOval(
                color = Volt.copy(alpha = 0.45f),
                topLeft = Offset(0f, center.y - r * 0.55f),
                size = Size(size.width, r * 1.1f),
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                ),
            )
            drawCircle(
                color = Snow,
                radius = 2.dp.toPx(),
                center = Offset(center.x + size.width * 0.37f, center.y + r * 0.33f),
            )
        }
    }
}

/** 연속 활동 카드 — 스트릭 일수 + 최근 7일 목표 달성 도트 */
@Composable
private fun RowScope.StreakCard(state: ProfileViewModel.UiState) {
    GlowCard(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 250.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
        spacing = 7.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = Volt,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = stringResource(R.string.profile_streak_title),
                fontSize = 11.sp,
                color = Silver,
            )
        }
        Text(
            text = stringResource(R.string.days_count, state.streak),
            fontSize = 21.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
            color = Snow,
        )
        Text(
            text = stringResource(R.string.profile_streak_great),
            fontSize = 9.sp,
            lineHeight = 13.sp,
            color = Slate,
        )
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            weekSlots(state.week).forEachIndexed { index, (date, day) ->
                val met = day != null && day.steps >= day.goal
                val isToday = index == 6
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (met) Volt else CarbonHigh)
                            .then(
                                // 오늘 도트는 볼트 링으로 강조
                                if (isToday) Modifier.border(1.5.dp, Volt, CircleShape) else Modifier
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (met) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = OnVolt,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }
                    Text(
                        text = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        fontSize = 8.5.sp,
                        color = if (isToday) Volt else Slate,
                    )
                }
            }
        }
    }
}

/** 최근 활동 요약 — 주간 링 + 최고 기록 + 미니 바 그래프 */
@Composable
private fun RecentSummaryCard(state: ProfileViewModel.UiState, onOpenAnalytics: () -> Unit) {
    val activeDays = state.week.count { it.steps > 0 }
    val bestDaySteps = state.week.maxOfOrNull { it.steps } ?: 0
    val bestDayKm = bestDaySteps * RewardEconomy.STRIDE_METERS / 1000
    val pace = if (state.avgPaceSecPerKm > 0) {
        "%d'%02d\"".format(state.avgPaceSecPerKm / 60, state.avgPaceSecPerKm % 60)
    } else {
        "—"
    }
    GlowCard(
        modifier = Modifier.quietClickable(onOpenAnalytics),
        contentPadding = PaddingValues(18.dp),
        spacing = 14.dp,
    ) {
        SectionHeader(
            title = stringResource(R.string.profile_recent_summary),
            actionText = stringResource(R.string.common_view_all),
            onAction = onOpenAnalytics,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NeonRing(
                progress = activeDays / 7f,
                modifier = Modifier.size(116.dp),
                ringWidth = 10.dp,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.profile_week_runs),
                        fontSize = 9.sp,
                        color = Slate,
                    )
                    Text(
                        text = stringResource(R.string.profile_times_unit, activeDays),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Snow,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                SummaryStat(
                    label = stringResource(R.string.profile_longest_distance),
                    value = "%.2f km".format(bestDayKm),
                )
                SummaryStat(
                    label = stringResource(R.string.profile_longest_time),
                    value = if (state.longestSessionSec > 0) formatDuration(state.longestSessionSec) else "—",
                )
                SummaryStat(
                    label = stringResource(R.string.profile_avg_pace),
                    value = pace,
                )
            }
            // 주간 미니 바 — 걸음 수 비례, 오늘만 볼트
            val maxSteps = bestDaySteps.coerceAtLeast(1)
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                weekSlots(state.week).forEachIndexed { index, (_, day) ->
                    val frac = (day?.steps ?: 0).toFloat() / maxSteps
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .height(6.dp + 40.dp * frac)
                            .clip(RoundedCornerShape(50))
                            .background(if (index == 6) Volt else Slate.copy(alpha = 0.55f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(label, fontSize = 9.sp, color = Slate)
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = Snow,
        )
    }
}

/** 러너 배지 — 육각 엠블럼 6칸 (해금 4 + 잠금 2), 탭하면 업적 전체 */
@Composable
private fun BadgesCard(state: ProfileViewModel.UiState, onOpenAchievements: () -> Unit) {
    val states = previewAchievements(state)
    val unlocked = states.count { it }
    GlowCard(
        modifier = Modifier.quietClickable(onOpenAchievements),
        contentPadding = PaddingValues(18.dp),
        spacing = 14.dp,
    ) {
        SectionHeader(
            title = stringResource(R.string.profile_badges),
            actionText = "$unlocked / 100",
            onAction = onOpenAchievements,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BadgeCell(
                name = stringResource(R.string.ach_marathon),
                unlocked = states[0],
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
            )
            BadgeCell(
                name = stringResource(R.string.ach_step_king),
                unlocked = states[1],
                icon = Icons.Filled.MilitaryTech,
            )
            BadgeCell(
                name = stringResource(R.string.ach_streak_master),
                unlocked = states[2],
                icon = Icons.Filled.Whatshot,
            )
            BadgeCell(
                name = stringResource(R.string.ach_collector),
                unlocked = states[3],
                icon = Icons.Filled.TrendingUp,
            )
            BadgeCell(
                name = stringResource(R.string.ach_locked),
                unlocked = false,
                icon = Icons.Filled.Lock,
            )
            BadgeCell(
                name = stringResource(R.string.ach_locked),
                unlocked = false,
                icon = Icons.Filled.Lock,
            )
        }
    }
}

@Composable
private fun RowScope.BadgeCell(
    name: String,
    unlocked: Boolean,
    icon: ImageVector,
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (unlocked) {
                HexEmblem(size = 42.dp)
                Icon(icon, contentDescription = null, tint = Snow, modifier = Modifier.size(16.dp))
            } else {
                HexEmblem(modifier = Modifier.alpha(0.25f), size = 42.dp, glow = false)
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Slate,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Text(
            text = name,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (unlocked) Snow else Slate,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 목표 설정 다이얼로그 — 기존 목표 카드의 슬라이더 UI를 그대로 품는다 */
@Composable
private fun GoalDialog(
    goal: Int,
    onGoalChange: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var sliderValue by remember(goal) { mutableFloatStateOf(goal.toFloat()) }
    val steps = sliderValue.toInt()
    val distanceKm = RewardEconomy.distanceMeters(steps) / 1000

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Carbon,
        titleContentColor = Snow,
        textContentColor = Silver,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.common_close),
                    color = Volt,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.goal_title),
                fontWeight = FontWeight.Black,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "%,d".format(steps),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1).sp,
                            color = Snow,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.goal_steps_suffix),
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate,
                            modifier = Modifier.padding(bottom = 5.dp),
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(bottom = 5.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.goal_about_km, "%.1f".format(distanceKm)),
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate,
                        )
                        // 목표를 올리면 보너스도 오른다는 것을 여기서 보여준다.
                        // 규칙만 바꾸고 알리지 않으면 아무도 목표를 올리지 않는다.
                        Text(
                            text = stringResource(
                                R.string.goal_bonus_preview,
                                "%,.1f".format(RewardEconomy.goalBaseBonus(steps)),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Volt,
                        )
                    }
                }
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { onGoalChange(sliderValue.toInt()) },
                    valueRange = UserPrefs.MIN_GOAL.toFloat()..UserPrefs.MAX_GOAL.toFloat(),
                    steps = (UserPrefs.MAX_GOAL - UserPrefs.MIN_GOAL) / 500 - 1,
                    colors = SliderDefaults.colors(
                        thumbColor = Volt,
                        activeTrackColor = Volt,
                        inactiveTrackColor = Snow.copy(alpha = 0.10f),
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent,
                    ),
                )
                Text(
                    text = stringResource(R.string.goal_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate,
                )
            }
        },
    )
}

/**
 * 프로필 편집 — 사진과 이름을 한 창에서.
 *
 * 예전에는 "프로필 편집"이 사진만 바꾸고 이름은 옆 칩에 따로 있었다.
 * 이름을 바꾸러 프로필 편집을 누른 사람은 거기서 멈춘다.
 *
 * 사진은 고르는 즉시 저장한다(되돌릴 것이 없다). 이름은 저장을 눌러야
 * 반영한다 — 글자를 지우는 중간 상태가 그대로 저장되면 곤란하다.
 */
@Composable
private fun ProfileEditDialog(
    nickname: String,
    selectedAvatar: Int,
    avatarRev: Int,
    onPickAvatar: (Int) -> Unit,
    onPickGallery: () -> Unit,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable(nickname) { mutableStateOf(nickname) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Carbon,
        titleContentColor = Snow,
        textContentColor = Silver,
        confirmButton = {
            TextButton(onClick = { onSave(text) }) {
                Text(
                    text = stringResource(R.string.common_confirm),
                    color = Volt,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_cancel), color = Slate)
            }
        },
        title = {
            Text(
                text = stringResource(R.string.profile_edit_profile),
                fontWeight = FontWeight.Black,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // ── 이름 ──
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.profile_set_nickname),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Slate,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CarbonHigh)
                            .border(1.dp, Edge, RoundedCornerShape(14.dp))
                            .padding(horizontal = 13.dp, vertical = 12.dp),
                    ) {
                        if (text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.profile_nickname_hint),
                                fontSize = 13.sp,
                                color = Slate,
                            )
                        }
                        BasicTextField(
                            // 길이 제한은 저장할 때가 아니라 입력할 때 건다. 17자를
                            // 쳐 놓고 저장 뒤에 잘려 있으면 고장으로 읽힌다.
                            value = text,
                            onValueChange = { if (it.length <= UserPrefs.NICKNAME_MAX) text = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = Snow, fontSize = 14.sp),
                            cursorBrush = SolidColor(Volt),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.profile_nickname_note),
                            fontSize = 11.sp,
                            color = Slate,
                        )
                        Text(
                            text = "${text.length} / ${UserPrefs.NICKNAME_MAX}",
                            fontSize = 11.sp,
                            color = Slate,
                        )
                    }
                }

                HairlineDivider()

                // ── 사진 ──
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(
                        text = stringResource(R.string.profile_edit_avatar),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Slate,
                    )
                    VoltButton(
                        text = stringResource(R.string.profile_avatar_gallery),
                        onClick = onPickGallery,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    // 갤러리 사진을 쓰고 있으면 지금 무엇이 걸려 있는지 보여준다.
                    if (selectedAvatar == UserPrefs.AVATAR_CUSTOM) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            LevelAvatar(
                                level = 1,
                                size = 42.dp,
                                avatarId = selectedAvatar,
                                customBitmap = rememberCustomAvatar(avatarRev),
                            )
                            Text(
                                text = stringResource(R.string.profile_avatar_current),
                                fontSize = 11.sp,
                                color = Silver,
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.profile_avatar_or_emoji),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate,
                    )
                    AvatarEmojis.chunked(4).forEachIndexed { rowIndex, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEachIndexed { colIndex, emoji ->
                                val id = rowIndex * 4 + colIndex
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .background(CarbonHigh)
                                        .border(
                                            width = if (selectedAvatar == id) 2.dp else 1.dp,
                                            color = if (selectedAvatar == id) Volt else Edge,
                                            shape = CircleShape,
                                        )
                                        .quietClickable { onPickAvatar(id) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(text = emoji, fontSize = 24.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}

// ── 내 정보 리뉴얼 조각 ─────────────────────────────────────────────

/**
 * 머리 — 작은 캐릭터 · 닉네임 · 레벨.
 *
 * 캐릭터를 누르면 꾸미기로 간다. 여기서 크게 보여 줄 것은 "나"이지 잔액이
 * 아니다 — 잔액은 아래 카드 한 군데에만 있다.
 */
@Composable
private fun MeHeader(
    state: ProfileViewModel.UiState,
    look: com.stepup.android.domain.AvatarLook,
    onEditProfile: () -> Unit,
    onOpenCustomize: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val runner = state.runner
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AvatarBadge(
            look = look,
            modifier = Modifier
                .size(84.dp)
                .guideTarget(GuideTour.Targets.PROFILE_AVATAR)
                .feedbackClickable(onClick = onOpenCustomize),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.feedbackClickable(onClick = onEditProfile),
            ) {
                Text(
                    text = state.nickname.ifBlank { stringResource(R.string.me_default_name) },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Snow,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.profile_edit_profile),
                    tint = Silver,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = stringResource(R.string.level_chip, runner.level),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = VoltText,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BarMeter(fraction = runner.progress, height = 6.dp, modifier = Modifier.weight(1f))
                Text(
                    text = if (runner.isMax) "MAX" else "%.1f / %.0f km".format(runner.intoLevelKm, runner.levelSpanKm),
                    fontSize = 11.sp,
                    color = Silver,
                    maxLines = 1,
                )
            }
        }
        DarkIconButton(
            icon = Icons.Filled.Settings,
            contentDescription = stringResource(R.string.cd_open_settings),
            onClick = onOpenSettings,
        )
    }
}

/** 보유 포인트 — 누르면 지갑(적립 내역 · 출금) */
@Composable
private fun PointsCard(balance: Double, onOpen: () -> Unit) {
    GlowCard(
        modifier = Modifier.feedbackClickable(onClick = onOpen),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        spacing = 6.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.me_points),
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Silver,
            )
            Text(
                text = stringResource(R.string.me_points_history),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = VoltText,
            )
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = VoltText, modifier = Modifier.size(18.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HexEmblem(size = 30.dp)
            Text(
                text = "%,.0f".format(balance),
                fontFamily = StepUpNumbers,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Snow,
                maxLines = 1,
            )
            Text(text = "SUP", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VoltText)
        }
    }
}

/** 이번 주 — 합계 거리와 요일별 막대. 같은 걸음 기록에서 둘 다 계산한다. */
@Composable
private fun WeekCard(week: List<DailyStepsEntity>, onOpen: () -> Unit) {
    val slots = weekSlots(week)
    val kms = slots.map { (_, day) -> RewardEconomy.distanceMeters(day?.steps ?: 0) / 1000 }
    val total = kms.sum()
    val peak = kms.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    val today = LocalDate.now()
    GlowCard(
        modifier = Modifier.feedbackClickable(onClick = onOpen),
        contentPadding = PaddingValues(16.dp),
        spacing = 10.dp,
    ) {
        Text(text = stringResource(R.string.me_this_week), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Silver)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "%.1f".format(total),
                    fontFamily = StepUpNumbers,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Snow,
                )
                Text(
                    text = " km",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Silver,
                    modifier = Modifier.padding(bottom = 5.dp),
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                slots.forEachIndexed { i, (date, _) ->
                    val isToday = date == today
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((40 * (kms[i] / peak)).coerceAtLeast(3.0).dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(if (isToday) Volt else Volt.copy(alpha = 0.55f)),
                        )
                        Text(
                            text = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, Locale.getDefault()),
                            fontSize = 11.sp,
                            color = if (isToday) Snow else Silver,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

/** 최근 러닝 — 거리 · 날짜 · 그 러닝으로 번 SUP */
@Composable
private fun RecentRunsCard(runs: List<WalkSessionEntity>?, onOpenAll: () -> Unit) {
    GlowCard(contentPadding = PaddingValues(16.dp), spacing = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.me_recent_runs),
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Snow,
            )
            Text(
                text = stringResource(R.string.me_see_all),
                modifier = Modifier
                    .feedbackClickable(onClick = onOpenAll)
                    .padding(6.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = VoltText,
            )
        }
        when {
            runs == null -> Unit
            runs.isEmpty() -> Text(
                text = stringResource(R.string.me_no_runs),
                fontSize = 13.sp,
                color = Silver,
                lineHeight = 18.sp,
            )
            else -> runs.forEach { run ->
                val date = java.time.Instant.ofEpochMilli(run.startedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CarbonHigh)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, tint = VoltText, modifier = Modifier.size(22.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "%.1f km".format(run.distanceMeters / 1000),
                            fontFamily = StepUpNumbers,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Snow,
                        )
                        Text(
                            text = date.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)),
                            fontSize = 12.sp,
                            color = Silver,
                        )
                    }
                    Text(
                        text = "+%,.0f SUP".format(run.pointsEarned),
                        fontFamily = StepUpNumbers,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = VoltText,
                    )
                }
            }
        }
    }
}

/** 데모 모드 스위치 — 켜면 소식 · 러너 마켓에 "예시"가 뜬다 */
@Composable
private fun DemoModeRow(on: Boolean, onChange: (Boolean) -> Unit) {
    GlowCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), spacing = 4.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.settings_demo),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Snow,
                )
                Text(
                    text = stringResource(R.string.settings_demo_note),
                    fontSize = 12.sp,
                    color = Silver,
                    lineHeight = 17.sp,
                )
            }
            androidx.compose.material3.Switch(
                checked = on,
                onCheckedChange = onChange,
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = com.stepup.android.ui.theme.OnVolt,
                    checkedTrackColor = Volt,
                ),
            )
        }
    }
}
