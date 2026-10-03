package com.stepup.android.ui.screens.community.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.StepUpSans

/*
 * 크루 채팅 · 내 크루 홈 파란 톤(2026-10-03 통합 전달본 v4 — stepup-crew-chat/home-blue-claude-v19) 공통 부품.
 *
 * 시안 토큰(design-tokens.json): 바닥 #031427 · 면 #0B2B50 · 선·주 강조 #0754FF · 보조 강조 #48D9FA · 본문 #F5F8FF ·
 * 보조 글자 #ABC6ED · 위험 #FF8E86(면 #481F32). 주 버튼은 흰 면 + 얇은 파란 아랫면, 보조는 남색 면 + 파란 테두리, 삭제 · 숨김 ·
 * 내보내기는 코랄 의미색. 기준 좌우 20dp · 누르는 곳 48dp · 주 버튼 56dp · 면 모서리 16dp · 시트는 러닝 리메이크의 RunSheet.
 *
 * 크루 명함형 부품(crew/CrewUi.kt)은 다른 묶음이 바꾸므로 여기서는 이 두 영역이 쓰는 같은 모양을 따로 둔다 — 동작 · 꼬리표는 그대로.
 * 밝은 테마는 러닝 리메이크의 밝은 값([runTone])과 같은 관계로 둔다.
 */

/** 두 영역의 색 — 어두운 테마는 시안 토큰, 밝은 테마는 같은 관계의 밝은 값 */
@Immutable
internal class BlueInk(
    val canvas: Color,
    val canvasTop: Color,
    val card: Color,
    val cardTop: Color,
    val sheet: Color,
    val text: Color,
    val secondary: Color,
    /** 정보 · 수치 강조(시안 #48D9FA) */
    val info: Color,
    val divider: Color,
    /** 면 둘레 — 파란 선 */
    val edge: Color,
    /** 고른 면 · 입력 중 둘레 */
    val edgeStrong: Color,
    val tabMark: Color,
    /** 실패 · 주의 · 삭제 글자(코랄) */
    val warn: Color,
    val choice: Color,
    val choiceText: Color,
    val choiceOn: Color,
    val choiceOnText: Color,
    val level: Color,
    val levelText: Color,
    val track: Color,
    val bar: Color,
    val avatar: Color,
    val avatarText: Color,
    val secondaryButton: Color,
    val disabled: Color,
    val disabledText: Color,
    val danger: Color,
    val dangerText: Color,
    val primaryFace: Brush,
    val primaryText: Color,
    /** 주 버튼 아랫면 */
    val primaryBase: Color,
    val skeleton: Color,
    val handle: Color,
    val scrim: Color,
)

private val DarkBlueInk = BlueInk(
    canvas = Color(0xFF031427), canvasTop = Color(0xFF05193A), card = Color(0xFF0A2749), cardTop = Color(0xFF0D3160),
    sheet = Color(0xFF0A2547), text = Color(0xFFF5F8FF), secondary = Color(0xFFABC6ED), info = Color(0xFF48D9FA),
    divider = Color(0xFF173F7A), edge = Color(0xFF1F5BC8), edgeStrong = Color(0xFF2F72FF), tabMark = Color(0xFF0754FF),
    warn = Color(0xFFFF8E86), choice = Color(0xFF0A2547), choiceText = Color(0xFFABC6ED), choiceOn = Color(0xFF0754FF),
    choiceOnText = Color.White, level = Color(0xFF0E3570), levelText = Color(0xFF9EC2FF), track = Color(0xFF12305C),
    bar = Color(0xFF48D9FA), avatar = Color(0xFF0D3A75), avatarText = Color(0xFFBFD6FF), secondaryButton = Color(0xFF061D3B),
    disabled = Color(0xFF2A3F5E), disabledText = Color(0xFF8EA3C2), danger = Color(0xFF481F32), dangerText = Color(0xFFFF8E86),
    primaryFace = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE6F0FD))), primaryText = Color(0xFF071B3D),
    primaryBase = Color(0xFF0754FF), skeleton = Color(0xFF0E2A4E), handle = Color(0xFF8FB0E8), scrim = Color(0x8C000000),
)

private val LightBlueInk = BlueInk(
    canvas = Color(0xFFF5F8FF), canvasTop = Color(0xFFEAF1FF), card = Color(0xFFFFFFFF), cardTop = Color(0xFFFFFFFF),
    sheet = Color(0xFFFFFFFF), text = Color(0xFF0B1E3F), secondary = Color(0xFF4A5F84), info = Color(0xFF0748D6),
    divider = Color(0xFFDCE4F2), edge = Color(0xFFC9D8F2), edgeStrong = Color(0xFF0754FF), tabMark = Color(0xFF0754FF),
    warn = Color(0xFFC62337), choice = Color(0xFFEDF2FC), choiceText = Color(0xFF4A5F84), choiceOn = Color(0xFF0754FF),
    choiceOnText = Color.White, level = Color(0xFFE1EBFF), levelText = Color(0xFF0748D6), track = Color(0xFFDCE5F5),
    bar = Color(0xFF0754FF), avatar = Color(0xFFE1EBFF), avatarText = Color(0xFF0748D6), secondaryButton = Color(0xFFFFFFFF),
    disabled = Color(0xFFC9D3E3), disabledText = Color(0xFF6E7D96), danger = Color(0xFFFFECEE), dangerText = Color(0xFFC62337),
    primaryFace = Brush.verticalGradient(listOf(Color(0xFF1C63FF), Color(0xFF0754FF))), primaryText = Color.White,
    primaryBase = Color(0xFF0335B0), skeleton = Color(0xFFE3EAF6), handle = Color(0xFFB5C3DB), scrim = Color(0x8C091A33),
)

@Composable
internal fun blueInk(): BlueInk = if (StepUpColors.dark) DarkBlueInk else LightBlueInk

/** 화면 좌우(시안 20dp) */
internal val BlueGutter = 20.dp

private val SurfaceShape = RoundedCornerShape(16.dp)

internal fun blueText(size: TextUnit, color: Color, weight: FontWeight = FontWeight.Normal, height: Float = 1.4f): TextStyle =
    TextStyle(fontFamily = StepUpSans, fontSize = size, color = color, fontWeight = weight, lineHeight = size * height)

// ── 바닥 · 머리 ────────────────────────────────────────────────

/** 화면 바닥 — 남색, 위가 아주 조금 밝다 */
@Composable
internal fun BluePage(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val ink = blueInk()
    Column(
        modifier.fillMaxSize().background(Brush.verticalGradient(0f to ink.canvasTop, 0.35f to ink.canvas, 1f to ink.canvas)),
        content = content,
    )
}

/**
 * 중첩 화면 머리 — 왼쪽 뒤로(가는 꺾쇠), 가운데 제목, 오른쪽 보조 행동 하나(•••). 로고 · SUP · 전역 탭은 두지 않는다.
 * [subtitle] 은 제목 아래 작은 줄(혼자 있는 새 크루의 "크루원 1명").
 */
@Composable
internal fun BlueTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onMore: (() -> Unit)? = null,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val ink = blueInk()
    Box(modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = 6.dp)) {
        BlueBackButton(onBack, Modifier.align(Alignment.CenterStart))
        Column(
            Modifier.align(Alignment.Center).padding(horizontal = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                title, style = blueText(19.sp, ink.text, FontWeight.Bold, 1.25f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Text(subtitle, style = blueText(13.sp, ink.secondary, FontWeight.Medium, 1.3f), maxLines = 1, textAlign = TextAlign.Center)
            }
        }
        Box(Modifier.align(Alignment.CenterEnd)) {
            when {
                trailing != null -> trailing()
                onMore != null -> BlueMoreButton(onMore, Modifier.testTag("crew-more"))
            }
        }
    }
}

/** 뒤로 — 가는 꺾쇠 하나(48dp 누르는 곳) */
@Composable
internal fun BlueBackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    com.stepup.android.ui.components.RunBackButton(onBack, modifier, tint = blueInk().text)
}

/** ••• (더보기) */
@Composable
internal fun BlueMoreButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ink = blueInk()
    val label = stringResource(R.string.chat_more)
    Box(
        modifier.size(width = 52.dp, height = 48.dp).clip(RoundedCornerShape(12.dp)).feedbackClickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(width = 24.dp, height = 6.dp)) {
            val r = 2.2.dp.toPx()
            listOf(0.1f, 0.5f, 0.9f).forEach { drawCircle(ink.text, r, Offset(size.width * it, size.height / 2)) }
        }
    }
}

// ── 버튼 ────────────────────────────────────────────────────────

internal enum class BlueKind { PRIMARY, SECONDARY, DANGER, DISABLED }

/**
 * 버튼(56dp · 모서리 16) — 주는 흰 면과 얇은 파란 아랫면, 보조는 남색 면 · 파란 테두리, 위험은 코랄 테두리.
 * 처리 중이면 도는 표시와 함께 다시 누를 수 없다.
 */
@Composable
internal fun BlueButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: BlueKind = BlueKind.PRIMARY,
    busy: Boolean = false,
    enabled: Boolean = true,
) {
    val ink = blueInk()
    val active = enabled && !busy && kind != BlueKind.DISABLED
    val off = kind == BlueKind.DISABLED || !enabled
    val base = if (kind == BlueKind.PRIMARY && !off) 4.dp else 0.dp
    val shape = SurfaceShape
    val color = when {
        off -> ink.disabledText
        kind == BlueKind.PRIMARY -> ink.primaryText
        kind == BlueKind.DANGER -> ink.dangerText
        kind == BlueKind.SECONDARY && !StepUpColors.dark -> ink.text
        else -> ink.text
    }
    Box(
        modifier.fillMaxWidth().heightIn(min = 56.dp + base)
            .drawBehind {
                val r = CornerRadius(16.dp.toPx())
                if (base > 0.dp) drawRoundRect(ink.primaryBase, cornerRadius = r)
            }
            .padding(bottom = base)
            .clip(shape)
            .then(
                when {
                    off -> Modifier.background(ink.disabled, shape)
                    kind == BlueKind.PRIMARY -> Modifier.background(ink.primaryFace, shape)
                    kind == BlueKind.DANGER -> Modifier.background(ink.danger, shape).border(1.5.dp, ink.warn, shape)
                    else -> Modifier.background(ink.secondaryButton, shape).border(1.5.dp, ink.edgeStrong, shape)
                },
            )
            .feedbackClickable(enabled = active, onClick = onClick)
            .semantics { if (busy) contentDescription = text },
        contentAlignment = Alignment.Center,
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (busy) {
                RunSpinner(Modifier.size(18.dp), color = color, track = color.copy(alpha = 0.25f))
                Spacer(Modifier.width(10.dp))
            }
            Text(text, style = blueText(17.sp, color, FontWeight.Bold, 1.25f), textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** 아래 고정 버튼 자리 — 시스템 아래 영역 위에 선다 */
@Composable
internal fun BlueBottomBar(caption: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val ink = blueInk()
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = BlueGutter).padding(top = 8.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (caption != null) {
            Text(caption, style = blueText(12.5.sp, ink.secondary), textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 12.dp).testTag("crew-footer-caption"))
        }
        content()
    }
}

// ── 면 · 줄 ─────────────────────────────────────────────────────

/** 테두리 면(16dp) — 남색 면 · 파란 둘레. [selected] 면 둘레가 밝다 */
@Composable
internal fun BlueSurface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    radius: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val ink = blueInk()
    val shape = RoundedCornerShape(radius)
    Column(
        modifier.clip(shape)
            .background(Brush.verticalGradient(listOf(ink.cardTop, ink.card)), shape)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) ink.edgeStrong else ink.edge, shape)
            .then(if (onClick != null) Modifier.feedbackClickable(onClick = onClick) else Modifier),
        content = content,
    )
}

@Composable
internal fun BlueDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(blueInk().divider))
}

/** 설정 줄 — 이름, 오른쪽 값(청록), 꺾쇠, 아래 선 */
@Composable
internal fun BlueRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    titleColor: Color? = null,
    titleSize: TextUnit = 17.sp,
    divider: Boolean = true,
    sub: String? = null,
    leading: ImageVector? = null,
    leadingTint: Color? = null,
) {
    val ink = blueInk()
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 60.dp).feedbackClickable(onClick = onClick).padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background((leadingTint ?: ink.info).copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(leading, null, tint = leadingTint ?: ink.secondary, modifier = Modifier.size(21.dp)) }
                Spacer(Modifier.width(16.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title, style = blueText(titleSize, titleColor ?: ink.text, FontWeight.Bold, 1.3f),
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                if (sub != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(sub, style = blueText(13.5.sp, ink.secondary, FontWeight.Medium), maxLines = 2)
                }
            }
            if (value != null) {
                Text(value, style = blueText(15.sp, ink.info, FontWeight.Bold), maxLines = 1, modifier = Modifier.padding(start = 8.dp))
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = ink.secondary, modifier = Modifier.padding(start = 6.dp).size(22.dp))
        }
        if (divider) BlueDivider()
    }
}

/** 사람 한 줄 — 동그라미 · 이름 · 역할 줄 · 꺾쇠 */
@Composable
internal fun BluePersonRow(name: String, sub: String, onClick: () -> Unit, modifier: Modifier = Modifier, subColor: Color? = null, owner: Boolean = false) {
    val ink = blueInk()
    Row(
        modifier.fillMaxWidth().heightIn(min = 76.dp).feedbackClickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChatFace(name, owner, 44.dp)
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(vertical = 13.dp)) {
                    Text(name, style = blueText(17.sp, ink.text, FontWeight.Bold, 1.3f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text(sub, style = blueText(13.5.sp, subColor ?: ink.secondary, FontWeight.Medium), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = ink.secondary, modifier = Modifier.size(22.dp))
            }
            BlueDivider()
        }
    }
}

/** 불러오는 중의 빈 모양 — 가짜 크루 정보를 먼저 보이지 않는다 */
@Composable
internal fun BlueSkeleton(modifier: Modifier, radius: Dp = 10.dp) {
    Box(modifier.clip(RoundedCornerShape(radius)).background(blueInk().skeleton))
}

/** 레벨 칩 — 저장된 레벨이 없으면 "새 크루" */
@Composable
internal fun BlueLevelChip(level: Int?, modifier: Modifier = Modifier, large: Boolean = false) {
    val ink = blueInk()
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier.clip(shape).background(ink.level, shape).border(1.dp, ink.edge.copy(alpha = 0.6f), shape)
            .padding(horizontal = if (large) 18.dp else 14.dp, vertical = if (large) 8.dp else 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (level != null) stringResource(R.string.crew_level_value, level) else stringResource(R.string.crew_level_new),
            style = blueText(if (large) 17.sp else 14.sp, ink.levelText, FontWeight.Bold, 1.2f), maxLines = 1,
        )
    }
}

/** 진행 막대 — 어두운 길에 청록 채움(100% 까지만 채운다. 숫자는 자르지 않는다) */
@Composable
internal fun BlueBar(fraction: Float, modifier: Modifier) {
    val ink = blueInk()
    Canvas(modifier) {
        val r = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(ink.track, cornerRadius = r)
        val w = size.width * fraction.coerceIn(0f, 1f)
        if (w > 0f) drawRoundRect(ink.bar, size = Size(w.coerceAtLeast(size.height), size.height), cornerRadius = r)
    }
}

// ── 입력 ────────────────────────────────────────────────────────

@Composable
internal fun BlueFieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = blueText(15.sp, blueInk().text, FontWeight.Bold), modifier = modifier)
}

/** 입력칸 — 남색 면 · 파란 둘레(모서리 14 · 52dp 이상). [maxChars] 를 넘는 글은 받지 않는다 */
@Composable
internal fun BlueTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    minHeight: Dp = 52.dp,
    keyboard: KeyboardOptions = KeyboardOptions.Default,
    maxChars: Int = Int.MAX_VALUE,
) {
    val ink = blueInk()
    val shape = RoundedCornerShape(14.dp)
    BasicTextField(
        value = value,
        onValueChange = { next -> onValueChange(if (next.length > maxChars) next.take(maxChars) else next) },
        singleLine = singleLine,
        keyboardOptions = keyboard,
        textStyle = blueText(16.sp, ink.text, FontWeight.Medium, 1.55f),
        cursorBrush = SolidColor(ink.info),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxWidth().heightIn(min = minHeight).clip(shape)
                    .background(Brush.verticalGradient(listOf(ink.cardTop, ink.card)), shape).border(1.dp, ink.edge, shape)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
            ) {
                if (value.isEmpty() && placeholder.isNotEmpty()) Text(placeholder, style = blueText(16.sp, ink.secondary, FontWeight.Medium, 1.55f))
                inner()
            }
        },
    )
}

/** 입력처럼 보이는 고르기 줄(모임 연결) — 앞 아이콘(있으면) · 글 · 아래 꺾쇠 */
@Composable
internal fun BluePickerField(
    text: String,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: ImageVector? = null,
) {
    val ink = blueInk()
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape)
            .background(Brush.verticalGradient(listOf(ink.cardTop, ink.card)), shape).border(1.dp, ink.edgeStrong.copy(alpha = 0.8f), shape)
            .feedbackClickable(enabled = enabled, onClick = onClick).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null && text.isNotEmpty()) {
            Icon(leading, null, tint = ink.info, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
        }
        Text(
            text.ifEmpty { placeholder }, style = blueText(16.sp, if (!enabled) ink.secondary else ink.text, if (text.isEmpty()) FontWeight.Medium else FontWeight.Bold),
            modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = ink.info, modifier = Modifier.size(24.dp))
    }
}

/** 지우기 X(입력값만 지운다) — 검색칸 오른쪽 */
@Composable
internal fun BlueClearButton(label: String, onClear: () -> Unit, modifier: Modifier = Modifier) {
    val ink = blueInk()
    Box(
        modifier.size(44.dp).clip(CircleShape).feedbackClickable(onClick = onClear).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(26.dp).clip(CircleShape).background(ink.secondary.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Close, contentDescription = null, tint = ink.text, modifier = Modifier.size(16.dp))
        }
    }
}

// ── 상태 · 시트 ─────────────────────────────────────────────────

/** 가는 청록 고리 안의 아이콘(접수 · 종료 · 다시 불러오기) */
@Composable
internal fun BlueStateIcon(icon: ImageVector, modifier: Modifier = Modifier, ring: Boolean = true) {
    val ink = blueInk()
    Box(
        modifier.size(68.dp).then(if (ring) Modifier.border(2.dp, ink.info, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = ink.info, modifier = Modifier.size(if (ring) 34.dp else 52.dp)) }
}

/** 가운데 안내(빈 목록 · 실패 · 볼 수 없음) — 그림 · 제목 · 설명 · 버튼들 */
@Composable
internal fun BlueEmptyState(
    icon: @Composable () -> Unit,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    buttons: @Composable ColumnScope.() -> Unit = {},
) {
    val ink = blueInk()
    Column(modifier.fillMaxWidth().padding(horizontal = BlueGutter), horizontalAlignment = Alignment.CenterHorizontally) {
        icon()
        Spacer(Modifier.height(22.dp))
        Text(title, style = blueText(24.sp, ink.text, FontWeight.Bold, 1.3f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(body, style = blueText(15.sp, ink.secondary, FontWeight.Medium, 1.5f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(30.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp), content = buttons)
    }
}

/**
 * 시트 — 러닝 리메이크의 RunSheet(남색 면 · 손잡이 · 닫기 X · 약 55% 덮개). 메뉴 시트는 제목이 왼쪽([centered] = false),
 * 확인 · 안내 시트는 가운데. 바깥 · 뒤로 · X 는 바꾸기 전으로 돌아간다(처리 중에는 닫히지 않는다).
 */
@Composable
internal fun BlueSheet(
    title: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissible: Boolean = true,
    centered: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val ink = blueInk()
    RunSheet(onDismiss = onDismiss, modifier = modifier, dismissible = dismissible, closeTag = "crew-sheet-close") {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
            if (title != null) {
                Text(
                    title, style = blueText(if (centered) 25.sp else 27.sp, ink.text, FontWeight.ExtraBold, 1.3f),
                    textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                    modifier = Modifier.fillMaxWidth().padding(start = if (centered) 36.dp else 4.dp, end = 36.dp).semantics { heading() },
                )
            }
            content()
        }
    }
}

/** 시트 본문 — 가운데 시트는 가운데로 */
@Composable
internal fun BlueSheetBody(text: String, modifier: Modifier = Modifier, centered: Boolean = true, color: Color? = null) {
    val ink = blueInk()
    Text(
        text, style = blueText(16.sp, color ?: ink.text.copy(alpha = 0.92f), FontWeight.Medium, 1.55f),
        textAlign = if (centered) TextAlign.Center else TextAlign.Start, modifier = modifier.fillMaxWidth(),
    )
}

/** 확인 시트 — 가운데 제목 · 설명 · [취소 · 행동]. 실패하면 같은 시트에 오류 */
@Composable
internal fun BlueConfirmSheet(
    title: String,
    body: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    busy: Boolean = false,
    danger: Boolean = false,
    tag: String = "crew-confirm",
    error: String? = null,
) {
    val ink = blueInk()
    BlueSheet(title, onDismiss, Modifier.testTag(tag), dismissible = !busy, centered = true) {
        Spacer(Modifier.height(12.dp))
        BlueSheetBody(body)
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                error, style = blueText(14.sp, ink.warn, FontWeight.SemiBold, 1.5f), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().testTag("crew-confirm-error"),
            )
        }
        Spacer(Modifier.height(26.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
            BlueButton(stringResource(R.string.common_cancel), onDismiss, Modifier.weight(1f), BlueKind.SECONDARY, enabled = !busy)
            BlueButton(
                confirm, onConfirm, Modifier.weight(1f).testTag("$tag-yes"),
                if (danger) BlueKind.DANGER else BlueKind.PRIMARY, busy = busy,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** 하나를 고르는 줄(테두리 면) — 고르면 파란 둘레 · 청록 체크(알림 받기 · 신고 사유) */
@Composable
internal fun BlueChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
    enabled: Boolean = true,
    selectedLabel: String? = null,
) {
    val ink = blueInk()
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 62.dp).clip(shape)
            .background(if (selected) ink.cardTop else ink.card.copy(alpha = 0.6f), shape)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) ink.edgeStrong else ink.edge.copy(alpha = 0.55f), shape)
            .feedbackClickable(enabled = enabled, cue = FeedbackCue.Select, role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = blueText(17.sp, ink.text, FontWeight.Bold, 1.3f), modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        Box(
            Modifier.size(28.dp).clip(CircleShape)
                .then(if (selected) Modifier.background(ink.info) else Modifier.border(2.dp, ink.secondary.copy(alpha = 0.85f), CircleShape)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(Icons.Filled.Check, null, tint = if (StepUpColors.dark) Color(0xFF06274D) else Color.White, modifier = Modifier.size(18.dp))
        }
        if (selected && selectedLabel != null) {
            Spacer(Modifier.width(10.dp))
            Text(selectedLabel, style = blueText(14.sp, ink.info, FontWeight.Bold), maxLines = 1)
        }
    }
}

/** 결과 화면(접수 · 알림) — 고리 아이콘 · 제목 · 설명 · 바로 아래 버튼 하나 */
@Composable
internal fun BlueResultPage(
    title: String,
    body: String,
    button: String,
    onButton: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Check,
    barTitle: String = stringResource(R.string.crew_notice_title),
) {
    val ink = blueInk()
    BluePage(modifier) {
        BlueTopBar(barTitle, onBack)
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = BlueGutter).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.6f))
            BlueStateIcon(icon)
            Spacer(Modifier.height(26.dp))
            Text(title, style = blueText(26.sp, ink.text, FontWeight.ExtraBold, 1.3f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(body, style = blueText(15.5.sp, ink.secondary, FontWeight.Medium, 1.5f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            BlueButton(button, onButton, Modifier.widthIn(max = 520.dp).testTag("crew-result-button"))
            Spacer(Modifier.weight(1.4f))
        }
    }
}

/** 세로 선 — 수치 사이 */
@Composable
internal fun BlueVerticalLine(modifier: Modifier = Modifier) {
    Box(modifier.width(1.dp).fillMaxHeight().background(blueInk().divider))
}

/** 원 안의 체크(테두리만) — 고리 결과용 선 그림 */
@Composable
internal fun BlueRing(modifier: Modifier = Modifier) {
    val ink = blueInk()
    Canvas(modifier) { drawCircle(ink.info, style = Stroke(2.dp.toPx())) }
}
