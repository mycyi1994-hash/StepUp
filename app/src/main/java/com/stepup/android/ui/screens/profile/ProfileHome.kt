package com.stepup.android.ui.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.components.GroupedList
import com.stepup.android.ui.components.NavListRow
import com.stepup.android.ui.components.SecondaryButton
import com.stepup.android.ui.components.ShoeArtThumbnail
import com.stepup.android.ui.components.ShoeArtFull
import com.stepup.android.ui.components.StatItem
import com.stepup.android.ui.components.StatRow
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.components.formatSupDown
import com.stepup.android.ui.components.rememberCustomAvatar
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.guide.GuideTour
import com.stepup.android.ui.guide.guideTarget
import java.util.Locale

/**
 * 내 정보 첫 화면 — UI 최종 통합안(2026-10-25) `me`. 머리(로고 · 설정 원 버튼)와 아래 탭은 앱 셸의 것을 쓴다.
 *
 * 사진(내 사진이 없으면 지금 신은 신발 — 기본 사람 아이콘 · 이니셜을 쓰지 않는다) · 이름 · Lv → 지금까지 달린 거리(큰 숫자) →
 * 기록한 러닝 · 운동 시간 · 최장 거리 → 묶음 목록(지갑 · 프로필 편집 · 기록 보기 · 설정). 챌린지 줄은 2026-10-17 사용자 결정으로 뺐다.
 *
 * 러너 레벨은 서버의 러너 레벨(0121 `runner_me` — 러닝 걸음, [RunnerLevelCard])을 받았으면 그 값, 아니면 예전처럼 계정의 누적 거리
 * (서버 profiles.lifetime_km)로 센 `state.runner.level` 이다. 거리 · 횟수는 저장된 러닝 세션의 합 — 읽는 중엔 "—",
 * 못 읽으면 실패 안내와 다시 시도(0 으로 보이지 않는다).
 */
@Composable
internal fun ProfileHome(
    state: ProfileViewModel.UiState,
    records: RunRecordState,
    onRetryRecords: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenSettings: () -> Unit,
    longestMeters: Double? = null,
    /** 가장 최근 러닝 — 없으면 줄을 두지 않는다 */
    latest: com.stepup.android.data.local.WalkSessionEntity? = null,
    /** 러너 레벨(서버 0121) — 서버가 주지 않으면 null(카드를 두지 않고 머리 줄은 예전 레벨) */
    runnerLevel: com.stepup.android.domain.RunnerLevel? = null,
    onOpenRunnerLevel: () -> Unit = {},
    onOpenAchievements: () -> Unit = {},
    onOpenPup: (() -> Unit)? = null,
) {
    val c = StepUpUi.colors
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(8.dp))
        ProfileRow(state, onEditProfile, runnerLevel?.level)
        Spacer(Modifier.height(14.dp))
        if (runnerLevel != null) {
            RunnerLevelCard(runnerLevel, onOpenRunnerLevel)
            Spacer(Modifier.height(14.dp))
        }
        RecordBlock(records, state, longestMeters, onRetryRecords)
        if (latest != null) {
            Spacer(Modifier.height(18.dp))
            RecentRun(latest, onOpenRecords)
        }
        Spacer(Modifier.height(14.dp))
        val edit = stringResource(R.string.profile_row_edit)
        GroupedList(
            rows = listOfNotNull<@Composable () -> Unit>(
                onOpenPup?.let { open -> {
                    NavListRow(Icons.Outlined.Pets, stringResource(R.string.pup_title), open, modifier = Modifier.testTag("profile-pup"))
                } },
                {
                    NavListRow(
                        Icons.Outlined.AccountBalanceWallet, stringResource(R.string.me_path_wallet), onOpenWallet,
                        // 읽기 전에 "0 SUP"을 보이면 잔액이 사라진 것처럼 읽힌다
                        value = if (state.loaded) stringResource(R.string.me_path_balance, formatSupDown(state.balance)) else "—",
                        modifier = Modifier.testTag("profile-wallet"),
                    )
                },
                { NavListRow(Icons.Outlined.Edit, edit, onEditProfile, modifier = Modifier.testTag("profile-edit")) },
                {
                    NavListRow(Icons.Outlined.BarChart, stringResource(R.string.profile_row_records), onOpenRecords,
                        modifier = Modifier.testTag("profile-records"))
                },
                {
                    NavListRow(Icons.Outlined.EmojiEvents, stringResource(R.string.v12_achievements), onOpenAchievements,
                        modifier = Modifier.testTag("profile-achievements"))
                },
                {
                    NavListRow(Icons.Outlined.Settings, stringResource(R.string.profile_tab_settings), onOpenSettings,
                        modifier = Modifier.testTag("profile-settings"))
                },
            ),
        )
        Spacer(Modifier.height(16.dp))
    }
}

/** 누적 러닝 — 불러오는 중 · 값 · 실패. 실패를 0km 로, 불러오는 중을 0회로 보이지 않는다 */
sealed interface RunRecordState {
    data object Loading : RunRecordState
    data class Ready(val meters: Double, val runs: Int) : RunRecordState
    data object Failed : RunRecordState
}

/**
 * 사진(56) · 이름(21 · 아주 굵게) · "Lv.N · 오늘도 좋은 하루예요". 사진을 누르면 프로필 편집.
 * [serverLevel] 은 서버의 러너 레벨(0121 — 걸음) — 받았으면 아래 카드와 같은 레벨을, 아니면 예전 레벨(누적 거리)을 보인다
 */
@Composable
private fun ProfileRow(state: ProfileViewModel.UiState, onEditProfile: () -> Unit, serverLevel: Int? = null) {
    val c = StepUpUi.colors
    val edit = stringResource(R.string.profile_row_edit)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileAvatar(
            state, size = 68.dp,
            modifier = Modifier.clip(CircleShape).feedbackClickable(onClick = onEditProfile)
                .semantics { contentDescription = edit }.guideTarget(GuideTour.Targets.PROFILE_AVATAR),
        )
        Spacer(Modifier.width(14.dp))
        // v12 시제품(2026-11-26) — 이름 옆 회색 "Lv.N"(읽기 전엔 비운다 — 1 로 보이지 않게)
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                state.nickname.ifBlank { stringResource(R.string.me_default_name) },
                style = StepUpUi.type.navTitle.copy(fontSize = 22.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, letterSpacing = (-0.035).em),
                color = c.label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false).semantics { heading() }.testTag("profile-name"),
            )
            if (state.loaded) {
                Spacer(Modifier.width(8.dp))
                Text(
                    "Lv.${serverLevel ?: state.runner.level}",
                    style = StepUpUi.type.rowSubtitle.copy(fontSize = 17.sp), color = c.secondaryLabel, maxLines = 1,
                    modifier = Modifier.testTag("profile-level"),
                )
            }
        }
    }
}

/**
 * 프로필 사진 — 내 사진을 올렸으면 그 사진, 아니면 지금 신은 신발(남색 원 위). 신은 신발이 없으면 빈 원
 * (HANDOFF me: 기본 사람 아이콘 · 이니셜 금지).
 */
@Composable
internal fun ProfileAvatar(state: ProfileViewModel.UiState, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val c = StepUpUi.colors
    val photo = rememberCustomAvatar(state.avatarRev)
    Box(
        modifier.size(size).clip(CircleShape).background(if (c.dark) Color(0xFF18213A) else Color(0xFFE6ECF6))
            .border(1.dp, Color.White.copy(alpha = if (c.dark) .08f else 0f), CircleShape)
            .testTag("profile-avatar"),
        contentAlignment = Alignment.Center,
    ) {
        val shoe = state.equipped.takeIf { state.loaded }
        when {
            state.avatarId == UserPrefs.AVATAR_CUSTOM && photo != null ->
                Image(photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            shoe != null -> ShoeArtThumbnail(shoe, sample = ShoeArtFull, modifier = Modifier.fillMaxSize().padding(size * .06f))
        }
    }
}

/** 다음 기록 — 누적 거리보다 큰 다음 이정표(km). 실제 누적 거리에서만 정한다 */
internal fun nextMilestoneKm(km: Double): Int {
    val steps = listOf(10, 25, 50, 100, 150, 200, 300, 500, 750, 1000, 1500, 2000, 3000, 5000)
    return steps.firstOrNull { it > km } ?: ((km / 1000).toInt() + 1) * 1000
}

/**
 * 지금까지 달린 거리 카드(2026-10-26 시안 profile) — 사람 없는 배경 사진 위 안쪽 틀 · 큰 숫자 · 다음 기록까지 막대,
 * 그 아래 기록한 러닝 · 운동 시간 · 최장 거리. 읽는 중엔 "—", 못 읽으면 실패 안내와 다시 시도.
 */
@Composable
private fun RecordBlock(records: RunRecordState, state: ProfileViewModel.UiState, longestMeters: Double?, onRetry: () -> Unit) {
    val c = StepUpUi.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).testTag("profile-record-card")) {
        if (records is RunRecordState.Failed) {
            Text(stringResource(R.string.profile_distance_hero), style = StepUpUi.type.rowSubtitle.copy(fontSize = 14.sp), color = c.secondaryLabel)
            Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.Start) {
                Text(
                    stringResource(R.string.me_path_failed), style = StepUpUi.type.section, color = c.label,
                    modifier = Modifier.testTag("profile-record-failed"),
                )
                Spacer(Modifier.height(12.dp))
                SecondaryButton(stringResource(R.string.me_path_retry), onClick = onRetry, modifier = Modifier.testTag("profile-record-retry"))
            }
            return@Column
        }
        val ready = records as? RunRecordState.Ready
        DistanceFrame(ready?.meters?.div(1000))
        // 기록한 러닝 · 운동 시간 · 최장 거리 줄은 뺐다(v12 시제품 — 내 러닝 기록 · 통계에 그대로 있다)
        when {
            records is RunRecordState.Loading -> Row(
                Modifier.fillMaxWidth().padding(top = 14.dp).testTag("profile-record-loading"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RunSpinner(Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.me_path_loading), style = StepUpUi.type.rowSubtitle, color = c.secondaryLabel)
            }
            ready != null && ready.runs == 0 -> Text(
                stringResource(R.string.pf_first_run), style = StepUpUi.type.rowSubtitle, color = c.secondaryLabel,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("profile-first-run"),
            )
        }
    }
}

/** 지금까지 달린 거리 — v12 시제품(2026-11-26) 남색 카드 · 큰 숫자 · 다음 기록까지 막대와 오른쪽 "N km" */
@Composable
private fun DistanceFrame(km: Double?) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF13285A), Color(0xFF0B193C))))
            .padding(horizontal = 24.dp, vertical = 22.dp),
    ) {
        Text(stringResource(R.string.profile_distance_hero), style = StepUpUi.type.rowTitle.copy(fontSize = 15.sp), color = Color(0xFFB9C6E4))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                km?.let { String.format(Locale.ROOT, "%,.1f", it) } ?: "—",
                style = StepUpUi.type.numberPoster.copy(fontSize = 64.sp, lineHeight = 68.sp, letterSpacing = .005.em), color = Color.White,
                maxLines = 1, softWrap = false,
                modifier = Modifier.alignByBaseline().testTag("profile-distance"),
            )
            if (km != null) {
                Spacer(Modifier.width(6.dp))
                Text("km", style = StepUpUi.type.navTitle.copy(fontSize = 24.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                    color = Color(0xFFB9C6E4), modifier = Modifier.alignByBaseline())
            }
        }
        if (km != null) {
            val next = nextMilestoneKm(km)
            val prev = listOf(0, 10, 25, 50, 100, 150, 200, 300, 500, 750, 1000, 1500, 2000, 3000, 5000).lastOrNull { it < next && it <= km } ?: 0
            val fraction = ((km - prev) / (next - prev)).toFloat().coerceIn(0f, 1f)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().testTag("profile-next"), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = .22f))) {
                    Box(Modifier.fillMaxWidth(fraction).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFE6EBF5)))
                }
                Spacer(Modifier.width(14.dp))
                Text("${next}km", style = StepUpUi.type.rowSubtitle.copy(fontSize = 15.sp), color = Color(0xFFB9C6E4))
            }
        }
    }
}

/** 최근 러닝 한 줄 — 거리 · 날짜, 오른쪽 시간 · 페이스. "전체 보기"는 내 러닝 기록 */
@Composable
private fun RecentRun(run: com.stepup.android.data.local.WalkSessionEntity, onOpenRecords: () -> Unit) {
    val c = StepUpUi.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).testTag("profile-recent")) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.d26_pf_recent), style = StepUpUi.type.section.copy(fontSize = 19.sp), color = c.label,
                modifier = Modifier.weight(1f).semantics { heading() })
            Text(
                stringResource(R.string.d26_pf_all), style = StepUpUi.type.rowSubtitle, color = c.secondaryLabel,
                modifier = Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(8.dp)).feedbackClickable(onClick = onOpenRecords)
                    .padding(horizontal = 6.dp, vertical = 12.dp).testTag("profile-recent-all"),
            )
        }
        val km = run.distanceMeters / 1000
        val date = java.time.Instant.ofEpochMilli(run.startedAt).atZone(java.time.ZoneId.systemDefault())
        // v12 시제품 — "3.2 km  26분 ·········· 11월 25일 (수) ›"
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(12.dp)).feedbackClickable(onClick = onOpenRecords)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(String.format(Locale.ROOT, "%.1f", km), style = StepUpUi.type.numberStat.copy(fontSize = 26.sp), color = c.label,
                modifier = Modifier.alignByBaseline())
            Spacer(Modifier.width(3.dp))
            Text("km", style = StepUpUi.type.unit.copy(fontSize = 15.sp), color = c.secondaryLabel, modifier = Modifier.alignByBaseline())
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.v12_minutes, (run.durationSec / 60).toInt()), style = StepUpUi.type.rowSubtitle.copy(fontSize = 15.sp),
                color = c.secondaryLabel, modifier = Modifier.alignByBaseline())
            Spacer(Modifier.weight(1f))
            Text(java.time.format.DateTimeFormatter.ofPattern(stringResource(R.string.v12_recent_date_pattern)).format(date),
                style = StepUpUi.type.rowSubtitle.copy(fontSize = 15.sp), color = c.secondaryLabel)
            androidx.compose.material3.Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = c.tertiaryLabel, modifier = Modifier.size(20.dp))
        }
    }
}

/** 운동 시간 "HH:MM"(시안 00:33) */
private fun hoursMinutes(totalSec: Long): String = String.format(Locale.ROOT, "%02d:%02d", totalSec / 3600, (totalSec % 3600) / 60)
