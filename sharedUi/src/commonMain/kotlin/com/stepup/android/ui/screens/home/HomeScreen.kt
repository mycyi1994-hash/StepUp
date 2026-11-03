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
import com.stepup.android.ui.theme.Alert
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt
import com.stepup.android.ui.theme.VoltText
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/**
 * 러닝 탭의 몸통(2026-10-26 시안 `run`) — 상태 없이 그린다. 머리(로고 · 알림)와 하단 탭은 공통 틀이 그린다.
 * 혼자 달리기만 둔다(크루 · 번개러닝은 함께 뛰기 탭). 화면이 낮거나 글자가 크면 본문이 넘어간다.
 */
@Composable
fun HomeContent(
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
) {
    // 본문은 넘기고, 이 화면의 주 행동(러닝 시작)은 늘 아래에 보인다(작은 화면 · 큰 글씨에서도)
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = StepUpUi.Gutter)
                .testTag("home-content"),
        ) {
            Spacer(Modifier.height(if (largeText) 8.dp else 14.dp))
            RunTabTitle(stringResource(Res.string.d26_title_run))
            Spacer(Modifier.height(if (largeText) 14.dp else 18.dp))
            StepsHero(
                todaySteps = todaySteps, goal = goal, loaded = loaded,
                hasPermission = hasPermission, firstHome = firstHome,
                onRequestPermission = onRequestPermission, onOpenDetails = onOpenDetails,
            )
            Spacer(Modifier.height(14.dp))
            RunPhotoCard(photoIndex)
            Spacer(Modifier.height(12.dp))
            GpsLine(gps)
            Spacer(Modifier.height(14.dp))
            StatRow(
                listOf(
                    StatItem(stringResource(Res.string.d26_stat_km), km?.let { com.stepup.android.domain.formatFixed((it).toDouble(), 2) } ?: "—", "km", tag = "home-stat-km"),
                    StatItem(stringResource(Res.string.d26_stat_sup),
                        earned?.let { "+" + com.stepup.android.ui.components.formatSupDown(it, 2) } ?: "—", "SUP", tag = "home-stat-sup"),
                    StatItem(stringResource(Res.string.d26_stat_week), weekRuns?.toString() ?: "—",
                        stringResource(Res.string.pf_runs_unit), tag = "home-stat-runs"),
                ),
                modifier = Modifier.testTag("home-stats"),
            )
            Spacer(Modifier.height(16.dp))
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = StepUpUi.Gutter).padding(top = 8.dp, bottom = 12.dp)) {
            // 이 화면의 주 행동 — 블루 알약(시안 "러닝 시작 ▷"). 누르면 러닝 시작 메뉴(U01), 달리는 중이면 그 러닝
            PrimaryButton(
                stringResource(if (running) Res.string.home_continue_run else Res.string.home_start_run), onStartRun,
                modifier = Modifier.fillMaxWidth().testTag("home-start-run"),
            )
        }
    }
}

/** 오늘 걸음으로 잰 거리(km) — 예전 홈과 같은 걸음 기준 */
fun RewardEconomyKm(steps: Int): Double = com.stepup.android.domain.RewardEconomy.distanceMeters(steps) / 1000.0


/**
 * 오늘 걸음(시안 `home` 위) — 작은 "오늘 걸음" · 큰 숫자(96) · "/ 목표". 누르면 오늘의 활동(E01).
 * 걸음 권한이 없으면 숫자 대신 권한 줄, 첫 러닝 홈이면 첫 안내 두 줄(시작·로그인·첫 사용 v1 시안 11).
 */
@Composable
fun StepsHero(
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
                stringResource(Res.string.onb_home_first_title), style = StepUpUi.type.largeTitle.copy(fontSize = 28.sp),
                color = c.label, modifier = Modifier.testTag("home-headline"),
            )
            Text(
                stringResource(Res.string.onb_home_first_body), style = StepUpUi.type.body, color = c.secondaryLabel,
                modifier = Modifier.padding(top = 8.dp).testTag("home-first-run"),
            )
        }
        return
    }
    val detailsLabel = stringResource(Res.string.home_open_details)
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
            Text(
                stringResource(Res.string.home_steps_today), style = StepUpUi.type.segment, color = c.secondaryLabel,
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
                    if (loaded) com.stepup.android.domain.formatGrouped((shown).toLong()) else "—",
                    style = StepUpUi.type.numberHero.copy(fontSize = 58.sp, lineHeight = 58.sp, letterSpacing = .005.em),
                    color = c.label, maxLines = 1, softWrap = false,
                    modifier = Modifier.alignByBaseline().popOnChange(todaySteps / 1000).testTag("home-steps-value"),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(Res.string.d26_steps_goal, com.stepup.android.domain.formatGrouped((goal).toLong())),
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

@Composable
fun PermissionStrip(onClick: () -> Unit) {
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
            text = stringResource(Res.string.perm_allow),
            modifier = Modifier.weight(1f),
            style = StepUpUi.type.rowTitle,
            color = c.label,
        )
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = c.tertiaryLabel, modifier = Modifier.size(18.dp))
    }
}
