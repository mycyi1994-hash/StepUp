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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * 러너 레벨은 걸음으로 쌓은 기존 `state.runner.level` 이다. 거리 · 횟수는 저장된 러닝 세션의 합 — 읽는 중엔 "—",
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
) {
    val c = StepUpUi.colors
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(8.dp))
        ProfileRow(state, onEditProfile)
        Spacer(Modifier.height(10.dp))
        RecordBlock(records, state, longestMeters, onRetryRecords)
        Spacer(Modifier.height(21.dp))
        val edit = stringResource(R.string.profile_row_edit)
        GroupedList(
            rows = listOf(
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

/** 사진(56) · 이름(21 · 아주 굵게) · "Lv.N · 오늘도 좋은 하루예요". 사진을 누르면 프로필 편집 */
@Composable
private fun ProfileRow(state: ProfileViewModel.UiState, onEditProfile: () -> Unit) {
    val c = StepUpUi.colors
    val edit = stringResource(R.string.profile_row_edit)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileAvatar(
            state, size = 56.dp,
            modifier = Modifier.clip(CircleShape).feedbackClickable(onClick = onEditProfile)
                .semantics { contentDescription = edit }.guideTarget(GuideTour.Targets.PROFILE_AVATAR),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                state.nickname.ifBlank { stringResource(R.string.me_default_name) },
                style = StepUpUi.type.navTitle.copy(fontSize = 21.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, letterSpacing = (-0.035).em),
                color = c.label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }.testTag("profile-name"),
            )
            // 러너 레벨 — 읽기 전엔 비운다(1 로 보이지 않게)
            if (state.loaded) {
                Text(
                    stringResource(R.string.profile_level_line, state.runner.level),
                    style = StepUpUi.type.rowSubtitle, color = c.secondaryLabel, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp).testTag("profile-level"),
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

/** 지금까지 달린 거리(104) + km → 기록한 러닝 · 운동 시간 · 최장 거리 */
@Composable
private fun RecordBlock(records: RunRecordState, state: ProfileViewModel.UiState, longestMeters: Double?, onRetry: () -> Unit) {
    val c = StepUpUi.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).testTag("profile-record-card")) {
        Text(stringResource(R.string.profile_distance_hero), style = StepUpUi.type.rowSubtitle.copy(fontSize = 14.sp), color = c.secondaryLabel)
        if (records is RunRecordState.Failed) {
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
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                // 천 단위 쉼표(랭킹과 같은 모양) — 1,284.5
                ready?.let { String.format(Locale.ROOT, "%,.1f", it.meters / 1000) } ?: "—",
                style = StepUpUi.type.numberPoster.copy(lineHeight = 96.sp, letterSpacing = .005.em), color = c.label,
                maxLines = 1, softWrap = false,
                modifier = Modifier.alignByBaseline().testTag("profile-distance"),
            )
            if (ready != null) {
                Spacer(Modifier.width(8.dp))
                Text("km", style = StepUpUi.type.navTitle.copy(fontSize = 24.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                    color = c.secondaryLabel, modifier = Modifier.alignByBaseline())
            }
        }
        Spacer(Modifier.height(15.dp))
        StatRow(
            listOf(
                StatItem(stringResource(R.string.profile_stat_runs), ready?.let { String.format(Locale.ROOT, "%,d", it.runs) } ?: "—",
                    if (ready != null) stringResource(R.string.pf_runs_unit) else null, tag = "profile-runs"),
                StatItem(stringResource(R.string.profile_stat_time), if (state.loaded) hoursMinutes(state.totalDurationSec) else "—",
                    tag = "profile-time"),
                StatItem(stringResource(R.string.profile_stat_longest), longestMeters?.let { String.format(Locale.ROOT, "%.1f", it / 1000) } ?: "—",
                    if (longestMeters != null) "km" else null, tag = "profile-longest"),
            ),
            numberStyle = StepUpUi.type.numberStat.copy(fontSize = 32.sp),
        )
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

/** 운동 시간 "HH:MM"(시안 00:33) */
private fun hoursMinutes(totalSec: Long): String = String.format(Locale.ROOT, "%02d:%02d", totalSec / 3600, (totalSec % 3600) / 60)
