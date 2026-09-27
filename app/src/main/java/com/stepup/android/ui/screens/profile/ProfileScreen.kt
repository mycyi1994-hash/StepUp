package com.stepup.android.ui.screens.profile

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalanceWallet
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.clickable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.filled.Inbox
import com.stepup.android.ui.theme.StepUpNumbers
import com.stepup.android.ui.theme.VoltText
import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.components.DarkIconButton
import com.stepup.android.ui.components.QuietListRow
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
    onOpenCustomize: () -> Unit = {},
    onOpenChallenges: () -> Unit = {},
    onOpenWallet: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {},
    onOpenChallengeHistory: () -> Unit = {},
    /** 설정 첫 목록(설정 v1) — 알림 · 개인정보 · 언어 · 테마 · 계정 · 도움말과 예전 목록의 길은 모두 거기서 */
    onOpenSettings: () -> Unit = {},
    onChangeBackground: () -> Unit = {},
    /** 프로필 수정(v1) — 사진 · 닉네임 편집 화면. 예전의 작은 창을 바꿨다 */
    onOpenProfileEdit: () -> Unit = {},
    /** 편집 화면에서 닉네임을 실제로 저장하고 돌아왔다 — 짧은 안내를 한 번 보인다 */
    nicknameSaved: Boolean = false,
    onNicknameNoticeShown: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    var notice by remember { mutableStateOf<String?>(null) }
    val savedText = stringResource(R.string.pe_saved_toast)
    // 받은 표시는 바로 지운다(다시 들어와도 한 번만) — 그러면 이 효과가 다시 시작되므로, 안내를 닫는 기다림은 따로 둔다
    LaunchedEffect(nicknameSaved) {
        if (nicknameSaved) {
            notice = savedText
            onNicknameNoticeShown()
        }
    }
    LaunchedEffect(notice) {
        if (notice != null) {
            kotlinx.coroutines.delay(2_600)
            notice = null
        }
    }

    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = com.stepup.android.ui.theme.StepUpDesign.Gutter, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ── 내 정보 첫 화면 — 러닝 패스(2026-09-27 사용자 선택): 프로필 → 누적 거리 카드 → 기록 보기 → 챌린지 · 지갑 · 설정 ──
        item {
            ProfileHome(
                state = state,
                records = records,
                onRetryRecords = viewModel::retryRecords,
                onEditProfile = onOpenProfileEdit,
                onOpenRecords = onOpenAnalytics,
                // 내 정보의 챌린지는 기록 · 이력(사용 피드백 7) — 거기서 진행 중인 챌린지로 간다
                onOpenChallenges = onOpenChallengeHistory,
                onOpenWallet = onOpenWallet,
                onOpenSettings = onOpenSettings,
            )
        }
    }
    com.stepup.android.ui.components.SettingsToast(
        notice, Modifier.align(Alignment.BottomCenter).padding(horizontal = 24.dp, vertical = 16.dp).testTag("profile-saved-notice"),
    )
    }
}

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


// ── 내 정보 리뉴얼 조각 ─────────────────────────────────────────────



