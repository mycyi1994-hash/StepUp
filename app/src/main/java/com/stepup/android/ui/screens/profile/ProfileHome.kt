package com.stepup.android.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.ui.components.LevelAvatar
import com.stepup.android.ui.components.RunningPathCard
import com.stepup.android.ui.components.RunningPathColors
import com.stepup.android.ui.components.formatSupDown
import com.stepup.android.ui.components.rememberCustomAvatar
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.guide.GuideTour
import com.stepup.android.ui.guide.guideTarget
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.StepUpSans
import com.stepup.android.ui.theme.VoltText
import java.util.Locale

/**
 * 내 정보 첫 화면 — 러닝 패스(2026-09-27 사용자 선택, docs/redesign/profile-running-path).
 *
 * 프로필 한 줄 → 누적 거리 카드(오른쪽 경로 장식) → 흰 "내 러닝 기록 보기" → 챌린지 · 지갑 · 설정.
 * 공통 머리(로고 · SUP)와 아래 탭은 앱 셸의 것을 쓴다 — 이 화면은 제목 줄을 따로 그리지 않는다.
 * 최근 기록 · 총 시간은 "내 러닝 기록 보기"(기록 화면)에 그대로 있다.
 */
@Composable
internal fun ProfileHome(
    state: ProfileViewModel.UiState,
    records: RunRecordState,
    onRetryRecords: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenChallenges: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val palette = profilePalette()
    // 목록의 기본 여백(20dp)에 4dp 를 더해 시안의 24dp 여백
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        ProfileRow(state, palette, onEditProfile)
        Spacer(Modifier.height(30.dp))
        RecordCard(records, onRetryRecords)
        Spacer(Modifier.height(25.dp))
        RecordsButton(onOpenRecords)
        Spacer(Modifier.height(33.dp))
        ProfileMenuRow(
            icon = ProfileIcons.Trophy, iconTint = palette.accentIcon,
            label = stringResource(R.string.challenge_title), onClick = onOpenChallenges, palette = palette,
            modifier = Modifier.testTag("profile-challenges"),
        )
        ProfileMenuRow(
            icon = ProfileIcons.Wallet, iconTint = palette.accentIcon,
            label = stringResource(R.string.me_path_wallet),
            // 읽기 전에 "0 SUP"을 보이면 잔액이 사라진 것처럼 읽힌다
            value = if (state.loaded) stringResource(R.string.me_path_balance, formatSupDown(state.balance)) else "—",
            onClick = onOpenWallet, palette = palette,
            modifier = Modifier.testTag("profile-wallet"),
        )
        ProfileMenuRow(
            icon = ProfileIcons.Gear, iconTint = palette.quietIcon,
            label = stringResource(R.string.profile_tab_settings), onClick = onOpenSettings, palette = palette,
            modifier = Modifier.testTag("profile-settings"),
        )
        Spacer(Modifier.height(8.dp))
    }
}

/** 누적 러닝 — 불러오는 중 · 값 · 실패. 실패를 0km 로, 불러오는 중을 0회로 보이지 않는다 */
sealed interface RunRecordState {
    data object Loading : RunRecordState
    data class Ready(val meters: Double, val runs: Int) : RunRecordState
    data object Failed : RunRecordState
}

/** 카드 밖의 색 — 어두운 테마는 시안 값, 밝은 테마는 앱의 색 토큰 */
private class ProfilePalette(
    val primary: Color,
    val secondary: Color,
    val link: Color,
    val plate: Color,
    val accentIcon: Color,
    val quietIcon: Color,
)

@Composable
private fun profilePalette(): ProfilePalette = if (StepUpColors.dark) {
    ProfilePalette(
        primary = Color(0xFFF2F4FC), secondary = Color(0xFF98A8C0), link = RunningPathColors.link,
        plate = Color(0xFF101B2C), accentIcon = Color(0xFFB4CAFF), quietIcon = Color(0xFFA4B2CA),
    )
} else {
    ProfilePalette(
        primary = Snow, secondary = Silver, link = VoltText,
        plate = CarbonHigh, accentIcon = VoltText, quietIcon = Silver,
    )
}

/** 사진 · 이름 · 프로필 수정 — 사진을 눌러도 수정 창이 열린다 */
@Composable
private fun ProfileRow(state: ProfileViewModel.UiState, palette: ProfilePalette, onEditProfile: () -> Unit) {
    val edit = stringResource(R.string.me_path_edit)
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        LevelAvatar(
            level = state.runner.level, size = 40.dp, contentDescription = edit, avatarId = state.avatarId,
            customBitmap = rememberCustomAvatar(state.avatarRev),
            modifier = Modifier.feedbackClickable(onClick = onEditProfile).guideTarget(GuideTour.Targets.PROFILE_AVATAR),
        )
        Spacer(Modifier.width(13.dp))
        Text(
            state.nickname.ifBlank { stringResource(R.string.me_default_name) },
            color = palette.primary, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.45).sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).testTag("profile-name"),
        )
        // 글자는 작아도 누르는 자리는 48dp — 오른쪽 끝은 본문 여백에 맞춘다
        Box(
            Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp))
                .feedbackClickable(role = Role.Button, onClick = onEditProfile)
                .padding(start = 12.dp).testTag("profile-edit"),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(edit, color = palette.link, fontSize = 13.sp, letterSpacing = (-0.25).sp, maxLines = 1)
        }
    }
}

/**
 * 지금까지 달린 거리 — 저장된 러닝의 누적 거리(km, 소수 첫째 자리)와 횟수. 걸음으로 어림한 거리는 쓰지 않는다.
 * 숫자와 km 는 한 줄로 재어 넘치면 숫자를 줄이고, 장식은 그 뒤로 민다.
 */
@Composable
private fun RecordCard(records: RunRecordState, onRetry: () -> Unit) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("profile-record-card")) {
        val number = when (records) {
            is RunRecordState.Ready -> String.format(Locale.ROOT, "%.1f", records.meters / 1000)
            else -> "—"
        }
        val showUnit = records is RunRecordState.Ready
        val unitStyle = TextStyle(fontFamily = StepUpSans, fontSize = 23.sp, fontWeight = FontWeight.Normal,
            letterSpacing = (-0.5).sp)
        val inner = with(density) { (maxWidth - 36.dp).toPx() }
        // 72sp 부터 한 칸씩 줄여 숫자 · 간격 · km 가 한 줄에 들어가는 크기
        val fit = remember(number, showUnit, inner, density) {
            val unitWidth = if (showUnit) measurer.measure("km", unitStyle).size.width + with(density) { 8.dp.toPx() } else 0f
            val size = listOf(72, 64, 56, 48, 40).firstOrNull { size ->
                measurer.measure(number, numberStyle(size)).size.width + unitWidth <= inner
            } ?: 40
            val width = measurer.measure(number, numberStyle(size)).size.width + unitWidth
            size to with(density) { (20.dp - 4.dp).toPx() + width }
        }
        val textEnd: Dp = with(density) { fit.second.toDp() }
        RunningPathCard(textEnd = if (records is RunRecordState.Failed) 0.dp else textEnd) {
            Column(Modifier.padding(start = 20.dp, end = 16.dp)) {
                Text(
                    stringResource(R.string.me_path_title),
                    color = RunningPathColors.title, fontSize = 14.sp, letterSpacing = (-0.25).sp,
                    modifier = Modifier.paddingFromBaseline(top = 35.dp),
                )
                if (records is RunRecordState.Failed) {
                    Text(
                        stringResource(R.string.me_path_failed),
                        color = RunningPathColors.number, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.paddingFromBaseline(top = 36.dp).testTag("profile-record-failed"),
                    )
                    Box(
                        Modifier.padding(top = 8.dp, bottom = 12.dp).heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                            .feedbackClickable(role = Role.Button, onClick = onRetry)
                            .padding(horizontal = 4.dp).testTag("profile-record-retry"),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(stringResource(R.string.me_path_retry), color = RunningPathColors.link, fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            number, style = numberStyle(fit.first), color = RunningPathColors.number, maxLines = 1,
                            // 큰 숫자의 왼쪽 여백이 커 보여 4dp 당긴다(시안)
                            modifier = Modifier.alignByBaseline().paddingFromBaseline(top = 72.dp).offset(x = (-4).dp)
                                .testTag("profile-distance"),
                        )
                        if (showUnit) {
                            Spacer(Modifier.width(4.dp))
                            Text("km", style = unitStyle, color = RunningPathColors.unit, maxLines = 1,
                                modifier = Modifier.alignByBaseline())
                        }
                    }
                    Text(
                        when (records) {
                            is RunRecordState.Ready ->
                                if (records.runs == 0) stringResource(R.string.me_path_none)
                                else pluralStringResource(R.plurals.me_path_runs, records.runs, records.runs)
                            else -> stringResource(R.string.me_path_loading)
                        },
                        color = RunningPathColors.runs, fontSize = 13.5.sp, letterSpacing = (-0.25).sp,
                        modifier = Modifier.paddingFromBaseline(top = 15.dp, bottom = 24.dp).testTag("profile-runs"),
                    )
                }
            }
        }
    }
}

private fun numberStyle(size: Int) = TextStyle(
    fontFamily = StepUpSans, fontSize = size.sp, fontWeight = FontWeight.SemiBold,
    // 시안 72sp 에서 -2.7 — 크기를 줄여도 같은 비율
    letterSpacing = (-2.7f * size / 72f).sp,
)

/** 흰 "내 러닝 기록 보기" — 기록 · 분석 화면으로. 밝은 테마는 앱 규칙대로 남색 면에 흰 글자 */
@Composable
private fun RecordsButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(17.dp)
    val dark = StepUpColors.dark
    Box(Modifier.fillMaxWidth()) {
        // 버튼 아래 2dp 비치는 두께
        if (dark) Box(Modifier.matchParentSize().offset(y = 2.dp).clip(shape).background(Color(0xFF26334B)))
        Box(
            Modifier.fillMaxWidth().heightIn(min = 54.dp).clip(shape)
                .background(if (dark) Brush.verticalGradient(listOf(Color(0xFFF7F8FF), Color(0xFFE8EDFA))) else SolidColor(Snow))
                .feedbackClickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .testTag("profile-records"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.me_path_records),
                color = if (dark) Color(0xFF081223) else Color.White,
                fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.25).sp, textAlign = TextAlign.Center,
            )
        }
    }
}

/** 메뉴 한 줄 — 줄 전체를 누른다. 아이콘 받침 · 이름 · (값) · 작은 화살표 */
@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    onClick: () -> Unit,
    palette: ProfilePalette,
    modifier: Modifier = Modifier,
    value: String? = null,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 74.dp).clip(RoundedCornerShape(16.dp))
            .feedbackClickable(role = Role.Button, onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(palette.plate), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(15.dp))
        LabelAndValue(label, value, palette, Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Icon(ProfileIcons.Chevron, contentDescription = null, tint = palette.secondary, modifier = Modifier.size(width = 8.dp, height = 12.dp))
    }
}

/** 이름과 값을 한 줄에 — 값이 길거나 글자가 커서 겹치면 값을 이름 아래 줄로 내린다 */
@Composable
private fun LabelAndValue(label: String, value: String?, palette: ProfilePalette, modifier: Modifier) {
    Layout(
        content = {
            Text(label, color = palette.primary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.25).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (value != null) {
                Text(value, color = palette.secondary, fontSize = 14.sp, letterSpacing = (-0.25).sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val labelPlaceable = measurables[0].measure(loose)
        val valuePlaceable = measurables.getOrNull(1)?.measure(loose)
        val gap = 12.dp.roundToPx()
        val width = constraints.maxWidth
        if (valuePlaceable == null || labelPlaceable.width + gap + valuePlaceable.width <= width) {
            val height = maxOf(labelPlaceable.height, valuePlaceable?.height ?: 0)
            layout(width, height) {
                labelPlaceable.placeRelative(0, (height - labelPlaceable.height) / 2)
                valuePlaceable?.placeRelative(width - valuePlaceable.width, (height - valuePlaceable.height) / 2)
            }
        } else {
            layout(width, labelPlaceable.height + valuePlaceable.height) {
                labelPlaceable.placeRelative(0, 0)
                valuePlaceable.placeRelative(0, labelPlaceable.height)
            }
        }
    }
}

/** 시안의 얇은 선 아이콘(24 격자, 선 1.6) — 트로피 · 지갑 · 톱니, 작은 화살표 */
private object ProfileIcons {
    val Trophy: ImageVector = strokeIcon(
        "trophy",
        "M7 3h10v6a5 5 0 0 1-10 0V3ZM7 5H3v3a4 4 0 0 0 4 4m10-7h4v3a4 4 0 0 1-4 4M12 14v5m-4 2h8m-4-2H8m4 0h4",
    )
    val Wallet: ImageVector = strokeIcon(
        "wallet",
        "M20 7V4H6a3 3 0 0 0 0 6h15v11H5a2 2 0 0 1-2-2V7m18 6h-6v5h6",
        "M17.7 15.5a.7 .7 0 1 1-1.4 0a.7 .7 0 1 1 1.4 0",
    )
    val Gear: ImageVector = strokeIcon(
        "gear",
        "m9.5 3 .6-1h3.8l.6 1 .4 2 1.8 1 1.9-.5 1.1.2 1.9 3.3-.4 1-1.5 1.5v2l1.5 1.5.4 1-1.9 3.3-1.1.2-1.9-.5-1.8 1-.4 2" +
            "-.6 1h-3.8l-.6-1-.4-2-1.8-1-1.9.5-1.1-.2-1.9-3.3.4-1 1.5-1.5v-2L3.2 10l-.4-1 1.9-3.3 1.1-.2 1.9.5 1.8-1 .4-2Z",
        "M15.1 12a3.1 3.1 0 1 1-6.2 0a3.1 3.1 0 1 1 6.2 0",
    )
    val Chevron: ImageVector = ImageVector.Builder("thinChevron", 8.dp, 12.dp, 8f, 12f).apply {
        addPath(
            PathParser().parsePathString("M2 2l4 4-4 4").toNodes(),
            stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
        )
    }.build()

    private fun strokeIcon(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { data ->
                addPath(
                    PathParser().parsePathString(data).toNodes(),
                    stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f,
                    strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()
}
