package com.stepup.android.ui.screens.walk

import com.stepup.android.ui.components.ButtonState
import com.stepup.android.ui.components.DangerButton
import com.stepup.android.ui.components.SecondaryBusyButton
import com.stepup.android.ui.components.PrimaryButton
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpSans

/*
 * 크루 러닝 흐름의 남색 부품(2026-10-22 크루 러닝 핸드오프 05_ANDROID_DESIGN · design-tokens.json).
 * 전역 앱 테마(밝게 · 어둡게)와 상관없이 크루 대기실 · 달리는 중 · 결과는 이 남색으로 그린다 — 핸드오프 "전역 테마를 바꾸는 작업이 아니라
 * 이 크루 흐름을 승인 시안에 맞춘다". 큰 주 버튼은 흰 면 + 파란 아랫면 하나, 보조 버튼은 남색 면 + 파란 테두리.
 */
internal object CrewNavy {
    val Bg = Color(0xFF070B14)
    val BgTop = Color(0xFF070B14)
    val Card = Color(0xFF0E1424)
    val CardTop = Color(0xFF0E1424)
    val Text = Color(0xFFFFFFFF)
    val Sub = Color(0xFF969CAA)
    val Cyan = Color(0xFF6E9BFF)
    val Line = Color(0x17FFFFFF)
    val Edge = Color(0x00000000)
    val Blue = Color(0xFF2F6BFF)
    val LevelFace = Color(0xFF202736)
    val LevelInk = Color(0xFF6E9BFF)
    val Track = Color(0xFF222A3D)
    val WhiteTop = Color(0xFF2F6BFF)
    val WhiteBottom = Color(0xFF2F6BFF)
    val Ink = Color(0xFFFFFFFF)
    val Secondary = Color(0xFF202736)
    val Warn = Color(0xFFFF453A)
    val WarnFace = Color(0xFF2A1219)
    val Mint = Color(0xFF32D74B)
    val Note = Color(0xFF0E1424)
    val Sheet = Color(0xFF0E1424)
    val Scrim = Color(0x8C000000)
    val Gutter = 20.dp
}

internal fun navyText(size: TextUnit, color: Color = CrewNavy.Text, weight: FontWeight = FontWeight.Normal, line: Float = 1.35f) =
    TextStyle(fontFamily = StepUpSans, fontSize = size, fontWeight = weight, color = color, lineHeight = line.em, letterSpacing = (-0.01).em)

/** 큰 주 버튼 — 통합안 블루 알약 + 원 화살표. 요청 중이면 돌고 눌리지 않는다 */
@Composable
internal fun NavyPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
) {
    PrimaryButton(text, onClick, modifier.fillMaxWidth(), state = when {
                busy -> ButtonState.Loading
                !enabled -> ButtonState.Disabled
                else -> ButtonState.Idle
            })
}

/** 보조 버튼 — 남색 면 + 파란 테두리 */
@Composable
internal fun NavySecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape).background(CrewNavy.Secondary, shape)
            .border(1.5.dp, if (enabled) CrewNavy.Edge else CrewNavy.Line, shape)
            .then(if (enabled) Modifier.feedbackClickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = navyText(17.sp, if (enabled) CrewNavy.Text else CrewNavy.Sub, FontWeight.Bold), textAlign = TextAlign.Center)
    }
}

/** 카드 — 위가 조금 밝은 남색 면 · 테두리(강조면 파랑) */
@Composable
internal fun NavyCard(
    modifier: Modifier = Modifier,
    strong: Boolean = false,
    selected: Boolean = false,
    padding: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(CrewNavy.CardTop, CrewNavy.Card)), shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) CrewNavy.Cyan else if (strong) CrewNavy.Edge else CrewNavy.Line, shape)
            .then(if (onClick != null) Modifier.feedbackClickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** 진행 막대 — 하늘색(주의면 붉은빛) 위에 어두운 길 */
@Composable
internal fun NavyBar(fraction: Float, modifier: Modifier = Modifier, warn: Boolean = false) {
    val shape = RoundedCornerShape(6.dp)
    Box(modifier.fillMaxWidth().height(10.dp).clip(shape).background(CrewNavy.Track, shape)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(10.dp).clip(shape).background(if (warn) CrewNavy.Warn else CrewNavy.Cyan, shape))
    }
}

/** 작은 칩 — "Lv.7" · "크루 러닝" · "인원 확인 중" */
@Composable
internal fun NavyChip(text: String, modifier: Modifier = Modifier, warn: Boolean = false) {
    Text(
        text, style = navyText(15.sp, if (warn) CrewNavy.Warn else CrewNavy.LevelInk, FontWeight.Bold),
        modifier = modifier.clip(RoundedCornerShape(10.dp)).background(if (warn) CrewNavy.WarnFace else CrewNavy.LevelFace)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/** 안내 띠 — 방패 아이콘 + 한 줄(주의면 붉은 면) */
@Composable
internal fun NavyNote(text: String, modifier: Modifier = Modifier, icon: ImageVector = Icons.Outlined.VerifiedUser, warn: Boolean = false) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier.fillMaxWidth().clip(shape).background(if (warn) CrewNavy.WarnFace else CrewNavy.Note, shape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (warn) CrewNavy.Warn else CrewNavy.Cyan, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = navyText(15.sp, if (warn) CrewNavy.Warn else CrewNavy.Cyan, FontWeight.Medium, 1.4f))
    }
}

/** 이름 · 값 한 줄 */
@Composable
internal fun NavyRow(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = CrewNavy.Text, big: Boolean = false) {
    Row(modifier.fillMaxWidth().heightIn(min = 40.dp).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = navyText(16.sp, CrewNavy.Sub, FontWeight.Medium), modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(value, style = navyText(if (big) 24.sp else 18.sp, valueColor, FontWeight.ExtraBold), textAlign = TextAlign.End)
    }
}

/** 큰 수 + 단위("3.20 km") */
@Composable
internal fun RowScope.NavyBigNumber(value: String, unit: String?, size: TextUnit = 54.sp, color: Color = CrewNavy.Text, tag: String? = null) {
    Text(
        value, style = navyText(size, color, FontWeight.ExtraBold, 1.05f).copy(letterSpacing = (-0.03).em),
        modifier = if (tag != null) Modifier.testTag(tag) else Modifier, maxLines = 1,
    )
    if (unit != null) {
        Spacer(Modifier.width(6.dp))
        Text(unit, style = navyText(size * 0.5f, CrewNavy.Sub, FontWeight.Bold), modifier = Modifier.padding(bottom = (size.value * 0.08f).dp))
    }
}

/** 시트 — 남색 면 · 제목 · 닫기. 바깥 탭 · 뒤로 가기 · 닫기는 부모의 선택 · 상태를 바꾸지 않는다 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NavySheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { if (dismissible) onDismiss() },
        sheetState = state,
        containerColor = Color.Transparent,
        scrimColor = CrewNavy.Scrim,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        Column(
            modifier.fillMaxWidth().clip(shape).background(CrewNavy.Sheet, shape).border(1.dp, CrewNavy.Line, shape)
                .navigationBarsPadding().imePadding().padding(horizontal = CrewNavy.Gutter).padding(top = 10.dp, bottom = 14.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(CrewNavy.Sub.copy(alpha = 0.5f)))
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = navyText(26.sp, weight = FontWeight.ExtraBold, line = 1.25f), modifier = Modifier.weight(1f).semantics { heading() })
                if (dismissible) {
                    val close = stringResource(R.string.common_close)
                    Icon(
                        Icons.Filled.Close, contentDescription = close, tint = CrewNavy.Text,
                        modifier = Modifier.size(48.dp).clip(CircleShape).feedbackClickable(onClick = onDismiss).padding(12.dp)
                            .semantics { contentDescription = close }.testTag("navy-sheet-close"),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()), content = content)
        }
    }
}

/** 시트 안 큰 제목(아이콘 · 두 줄) */
@Composable
internal fun NavyHeadline(text: String, icon: ImageVector? = null, color: Color = CrewNavy.Text) {
    if (icon != null) {
        Icon(icon, contentDescription = null, tint = CrewNavy.Cyan, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(6.dp))
    }
    Text(text, style = navyText(30.sp, color, FontWeight.ExtraBold, 1.25f))
}
