package com.stepup.android.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.LocalFeedback
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.StepUpNumbers
import com.stepup.android.ui.theme.StepUpSans
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * UI 최종 통합안(2026-10-25 개발 전달본 — StepUp_UI_최종_2026-10-25/04_개발_넘김) 공통 부품.
 *
 * 구조 · 묶음 목록 · 세그먼트 · 사진 포스터 카드 · 아주 큰 숫자 · 반투명 탭바는 G(나이키 × 애플),
 * 주 버튼 끝 원 화살표 · 이탤릭 LIVE 한 줄 · 가로로 넘기는 사진 카드 · 사진 시작 화면은 A(나이키 런클럽).
 * 두 벌(다크블루 / 화이트블루) — 값은 tokens.json, 팔레트에 이미 있는 자리(바닥 · 카드 · 글자 · 블루)는 [StepUpColors] 를 읽는다.
 *
 * 규칙(HANDOFF 1-3):
 *   - 화면에 주 버튼 하나. 원 화살표는 "다음으로 넘어가는" 주 버튼(Idle)에만 — 로딩 · 완료 · 마감 · 보조 · 작은 버튼에는 없다.
 *   - LIVE 는 배지가 아니다 — 빨간 점 7 + 이탤릭 "LIVE" + " · 23분 뒤 출발" 한 줄.
 *   - 번지는 빛 · 체크 달린 칩 · 같은 알약 칩 줄 · 쪼갠 버튼 · 아이콘 타일 · 이니셜 아바타 · 색 배지는 쓰지 않는다.
 */

// ── 색 ───────────────────────────────────────────────────────────────

/** 팔레트(Color.kt)에 없는 통합안 역할색 — tokens.json colors 의 나머지 */
@Immutable
class StepUpUiColors(
    val background: Color,
    val card: Color,
    val elevated: Color,
    /** 보조 버튼 · 작은 버튼 · 세그먼트 트랙 · 원 버튼(fill) */
    val fill: Color,
    /** 마감 · 비활성 바탕(fillSecondary) */
    val fillSecondary: Color,
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    /** 비활성 글자 */
    val onDisabled: Color,
    val separator: Color,
    val primary: Color,
    val primaryPressed: Color,
    val onPrimary: Color,
    val accentText: Color,
    val segmentThumb: Color,
    val live: Color,
    val success: Color,
    val tabBar: Color,
    val mapBackground: Color,
    val errorFill: Color,
    /** 사진 위 흰 알약 버튼 끝 원(다크 남색 · 라이트 검정) */
    val photoArrowCircle: Color,
    /** 시트 · 모달 뒤 막(상태바까지 같이 어두워진다) */
    val sheetScrim: Color,
    /** 시트 손잡이 */
    val handle: Color,
    /** 사진 위 유리 원 버튼 */
    val glass: Color,
    val dark: Boolean,
)

val DarkBlueUi = StepUpUiColors(
    background = Color(0xFF070B14), card = Color(0xFF0E1424), elevated = Color(0xFF151C2D),
    fill = Color(0x338296BE), fillSecondary = Color(0x1F8296BE),
    label = Color.White, secondaryLabel = Color(0xA3E2E8F5), tertiaryLabel = Color(0x61E2E8F5),
    onDisabled = Color(0x61E2E8F5), separator = Color(0x17FFFFFF),
    primary = Color(0xFF2F6BFF), primaryPressed = Color(0xFF2A5FE6), onPrimary = Color.White,
    accentText = Color(0xFF6E9BFF), segmentThumb = Color(0xFF26304A),
    live = Color(0xFFFF453A), success = Color(0xFF32D74B),
    tabBar = Color(0xD1070B14), mapBackground = Color(0xFF0A1020), errorFill = Color(0x29FF453A),
    photoArrowCircle = Color(0xFF070B14), sheetScrim = Color(0x8C000000), handle = Color(0x4DE2E8F5),
    glass = Color(0x2EFFFFFF), dark = true,
)

val WhiteBlueUi = StepUpUiColors(
    background = Color(0xFFF2F2F7), card = Color.White, elevated = Color.White,
    fill = Color(0x24787880), fillSecondary = Color(0x14787880),
    label = Color.Black, secondaryLabel = Color(0x9E3C3C43), tertiaryLabel = Color(0x523C3C43),
    onDisabled = Color(0x523C3C43), separator = Color(0x293C3C43),
    primary = Color(0xFF1F5EFF), primaryPressed = Color(0xFF1A4FE0), onPrimary = Color.White,
    accentText = Color(0xFF1F5EFF), segmentThumb = Color.White,
    live = Color(0xFFFF3B30), success = Color(0xFF28B44A),
    tabBar = Color(0xEBFAFAFC), mapBackground = Color(0xFFECEEF1), errorFill = Color(0x1AFF3B30),
    photoArrowCircle = Color.Black, sheetScrim = Color(0x40000000), handle = Color(0x4D3C3C43),
    glass = Color(0x2EFFFFFF), dark = false,
)

// ── 글자 단계(tokens.json type) ─────────────────────────────────────

@Immutable
class StepUpUiType(
    val largeTitle: TextStyle,
    val navTitle: TextStyle,
    val section: TextStyle,
    val rowTitle: TextStyle,
    val rowSubtitle: TextStyle,
    val body: TextStyle,
    val caption: TextStyle,
    val button: TextStyle,
    val smallButton: TextStyle,
    val segment: TextStyle,
    val tabLabel: TextStyle,
    val liveCaption: TextStyle,
    val unit: TextStyle,
    val numberHero: TextStyle,
    val numberPoster: TextStyle,
    val numberLarge: TextStyle,
    val numberStat: TextStyle,
    val numberRow: TextStyle,
    val numberCard: TextStyle,
)

private fun kr(size: Float, weight: FontWeight, tracking: Float, height: Float = 1.4f) = TextStyle(
    fontFamily = StepUpSans, fontWeight = weight, fontSize = size.sp, letterSpacing = tracking.em, lineHeight = (size * height).sp,
    // 한글은 낱말(띄어쓰기) 단위로 줄을 바꾼다 — "없어 / 요"처럼 낱말 가운데서 끊지 않게(시안 word-break: keep-all)
    lineBreak = KoreanLineBreak,
)

/** 낱말 단위 줄바꿈(Android 13+ 구 단위, 그 아래는 기본 줄바꿈) */
val KoreanLineBreak = androidx.compose.ui.text.style.LineBreak(
    strategy = androidx.compose.ui.text.style.LineBreak.Strategy.Balanced,
    strictness = androidx.compose.ui.text.style.LineBreak.Strictness.Strict,
    wordBreak = androidx.compose.ui.text.style.LineBreak.WordBreak.Phrase,
)

// 숫자 글꼴(Saira Condensed)은 아랫부분(0.44em)이 깊은데 숫자는 그 자리를 쓰지 않는다 — 줄 상자를 가운데 맞춤으로 두어
// 숫자가 상자 가운데 오게 한다(비례 맞춤이면 숫자 아래 빈칸이 생겨 시안보다 아래 줄이 멀어진다)
private fun num(size: Float) = TextStyle(
    fontFamily = StepUpNumbers, fontWeight = FontWeight.Bold, fontSize = size.sp, letterSpacing = 0.em,
    lineHeight = (size * .95f).sp, fontFeatureSettings = "tnum",
    lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
        androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center, androidx.compose.ui.text.style.LineHeightStyle.Trim.None,
    ),
)

val StepUpUiTypeScale = StepUpUiType(
    largeTitle = kr(34f, FontWeight.ExtraBold, -0.045f, 1.15f),
    navTitle = kr(17f, FontWeight.Bold, -0.04f),
    section = kr(20f, FontWeight.ExtraBold, -0.035f),
    rowTitle = kr(16.5f, FontWeight.Bold, -0.03f),
    rowSubtitle = kr(13.5f, FontWeight.Normal, -0.03f),
    body = kr(16f, FontWeight.Normal, -0.025f),
    caption = kr(13f, FontWeight.Normal, -0.03f),
    button = kr(17f, FontWeight.Bold, -0.04f),
    smallButton = kr(14f, FontWeight.Bold, -0.03f),
    segment = kr(14f, FontWeight.SemiBold, -0.04f),
    tabLabel = kr(10.5f, FontWeight.SemiBold, -0.02f, 1.2f),
    liveCaption = kr(13.5f, FontWeight.ExtraBold, 0.01f).copy(fontStyle = FontStyle.Italic),
    unit = kr(12f, FontWeight.SemiBold, -0.02f, 1.2f),
    numberHero = num(132f),
    numberPoster = num(104f),
    numberLarge = num(88f),
    numberStat = num(34f),
    numberRow = num(26f),
    numberCard = num(40f),
)

/** 통합안 색 · 글자 — 화면 코드는 `StepUpUi.colors.fill` · `StepUpUi.type.button` 처럼 읽는다 */
object StepUpUi {
    val colors: StepUpUiColors get() = if (StepUpColors.dark) DarkBlueUi else WhiteBlueUi
    val type: StepUpUiType get() = StepUpUiTypeScale

    /** 화면 양옆 여백 · 큰 제목 여백 · 줄 최소 높이 · 줄 안쪽 여백(tokens.json spacing) */
    val Gutter = 16.dp
    val TitleGutter = 20.dp
    val RowMinHeight = 64.dp
    val ButtonHeight = 54.dp
    val SmallButtonHeight = 32.dp
    val PosterRadius = 24.dp
    val GroupedRadius = 16.dp
    val FieldRadius = 14.dp
    val ThumbRadius = 12.dp
    val SheetRadius = 28.dp
}

// ── 누름 ─────────────────────────────────────────────────────────────

/**
 * 누름 피드백 공통 — 0.97배 + 어둡게 10% + 짧은 진동(설정에서 끈 사람은 없음). 뗄 때 스프링(감쇠 0.55 · 강성 400).
 * 물결(ripple)은 쓰지 않는다. 어둡게는 그린 픽셀에만 얹는다(모서리 밖이 어두워지지 않게).
 */
@Composable
fun Modifier.pressable(
    enabled: Boolean = true,
    cue: FeedbackCue = FeedbackCue.Tap,
    role: Role? = Role.Button,
    onClick: () -> Unit,
): Modifier {
    val feedback = LocalFeedback.current
    val motion = LocalMotion.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val down = pressed && enabled
    val scale by animateFloatAsState(
        if (down && !motion.reduced) .97f else 1f,
        if (down) tween(90) else spring(dampingRatio = .55f, stiffness = 400f), label = "press",
    )
    val dim by animateFloatAsState(if (down) .10f else 0f, tween(if (down) 90 else 160), label = "pressDim")
    return this
        .graphicsLayer {
            scaleX = scale; scaleY = scale
            compositingStrategy = if (dim > 0f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
        }
        .drawWithContent {
            drawContent()
            if (dim > 0f) drawRect(Color.Black.copy(alpha = dim), blendMode = BlendMode.SrcAtop)
        }
        .clickable(source, indication = null, enabled = enabled, role = role) {
            feedback?.play(cue)
            onClick()
        }
}

// ── 버튼 ─────────────────────────────────────────────────────────────

/** 버튼 상태 다섯 — 기본 → 로딩(못 누름) → 완료(보조 모양) · 신청됨(대기) · 마감 */
enum class ButtonState { Idle, Loading, Done, Waiting, Disabled }

/** 주 버튼 끝 원 화살표 — 원 40, 가운데 화살표(가로 14 · 머리 6 · 선 2.4 둥근 끝). 시안 SVG 와 같은 좌표 */
@Composable
fun ArrowCircle(circle: Color, arrow: Color, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Canvas(modifier.size(size)) {
        val u = this.size.width / 40f
        drawCircle(circle)
        val w = 2.4f * u
        drawLine(arrow, Offset(12.5f * u, 20f * u), Offset(26.5f * u, 20f * u), w, StrokeCap.Round)
        drawLine(arrow, Offset(21f * u, 14f * u), Offset(27f * u, 20f * u), w, StrokeCap.Round)
        drawLine(arrow, Offset(21f * u, 26f * u), Offset(27f * u, 20f * u), w, StrokeCap.Round)
    }
}

/**
 * 로딩을 너무 짧게 보이지 않게(최소 400ms, tokens motion.loading.minimumMs) — 서버가 바로 답해도 스피너가 깜빡이고 사라지지 않는다.
 */
@Composable
private fun rememberMinLoading(state: ButtonState): ButtonState {
    var shown by remember { mutableStateOf(state) }
    var loadingSince by remember { mutableStateOf(0L) }
    LaunchedEffect(state) {
        if (state == ButtonState.Loading) {
            loadingSince = System.currentTimeMillis()
            shown = state
        } else {
            if (shown == ButtonState.Loading) {
                val left = 400L - (System.currentTimeMillis() - loadingSince)
                if (left > 0) delay(left)
            }
            shown = state
        }
    }
    return shown
}

/**
 * 주 버튼 — 화면에 하나. 블루 알약 54, 글자 왼쪽 24, 오른쪽 끝(안쪽 7)에 흰 원 40 + 블루 화살표.
 *
 * [onPhoto] = 사진 위: 흰 알약 + 남색(라이트는 검정) 원 + 흰 화살표.
 * [arrow] = false 면 "다음으로 넘어가지 않는" 주 버튼(확인 · 닫기) — 가운데 글자만.
 * 화살표는 Idle 일 때만. Loading = 가운데 스피너만, Done/Waiting = 보조 모양으로 내려감, Disabled = 회색 + 가운데 글자.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: ButtonState = ButtonState.Idle,
    onPhoto: Boolean = false,
    arrow: Boolean = true,
    icon: ImageVector? = null,
    doneText: String? = null,
    waitingText: String? = null,
    cue: FeedbackCue = FeedbackCue.Tap,
    /** 완료 · 대기 상태에서도 누를 수 있다(다시 누르면 취소 시트 등) */
    tappableWhenDone: Boolean = false,
) {
    val c = StepUpUi.colors
    val type = StepUpUi.type
    val shown = rememberMinLoading(state)
    val asPrimary = shown == ButtonState.Idle || shown == ButtonState.Loading
    val bg by animateColorAsState(
        when {
            shown == ButtonState.Disabled -> c.fillSecondary
            onPhoto && asPrimary -> Color.White
            asPrimary -> c.primary
            else -> c.fill
        },
        tween(200), label = "primaryBg",
    )
    val fg = when {
        shown == ButtonState.Disabled -> c.onDisabled
        onPhoto && asPrimary -> Color.Black
        asPrimary -> c.onPrimary
        onPhoto -> Color.White
        else -> c.label
    }
    val withArrow = arrow && shown == ButtonState.Idle
    val clickable = shown == ButtonState.Idle ||
        (tappableWhenDone && (shown == ButtonState.Done || shown == ButtonState.Waiting))
    Box(
        modifier
            .heightIn(min = StepUpUi.ButtonHeight)
            .pressable(enabled = clickable, cue = cue, onClick = onClick)
            .clip(CircleShape)
            .background(bg)
            // 로딩은 스피너만 보이지만 글("저장 중…")은 읽기 도구 · 검사에 그대로 남긴다
            .semantics { if (shown == ButtonState.Loading) { contentDescription = text; this.text = androidx.compose.ui.text.AnnotatedString(text) } }
            .padding(start = if (withArrow) 24.dp else 20.dp, end = if (withArrow) 7.dp else 20.dp),
        contentAlignment = if (withArrow) Alignment.CenterStart else Alignment.Center,
    ) {
        AnimatedContent(shown, transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(120)) }, label = "primaryState") { s ->
            when (s) {
                ButtonState.Loading -> StepUpSpinner(Modifier.size(22.dp), color = fg)
                ButtonState.Done -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Check, null, tint = fg, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(doneText ?: text, style = type.button, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                ButtonState.Waiting -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Schedule, null, tint = fg, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(waitingText ?: text, style = type.button, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                ButtonState.Idle -> if (withArrow) {
                    Row(Modifier.fillMaxWidth().heightIn(min = StepUpUi.ButtonHeight), verticalAlignment = Alignment.CenterVertically) {
                        if (icon != null) {
                            Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text, style = type.button, color = fg, modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(8.dp))
                        ArrowCircle(
                            circle = if (onPhoto) c.photoArrowCircle else Color.White,
                            arrow = if (onPhoto) Color.White else c.primary,
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (icon != null) {
                            Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(text, style = type.button, color = fg, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                ButtonState.Disabled -> Text(text, style = type.button, color = fg, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** 보조 버튼 — 회색 채움 알약 54, 화살표 없음. [onPhoto] 면 사진 위 유리 알약 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onPhoto: Boolean = false,
    color: Color? = null,
    cue: FeedbackCue = FeedbackCue.Tap,
) {
    val c = StepUpUi.colors
    val fg = when {
        !enabled -> c.onDisabled
        color != null -> color
        onPhoto -> Color.White
        else -> c.label
    }
    Box(
        modifier
            .heightIn(min = StepUpUi.ButtonHeight)
            .pressable(enabled = enabled, cue = cue, onClick = onClick)
            .clip(CircleShape)
            .background(if (!enabled) c.fillSecondary else if (onPhoto) c.glass else c.fill)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = fg, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(7.dp))
            }
            Text(text, style = StepUpUi.type.button, color = fg, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** 위험 버튼(나가기 · 삭제 · 해산 · 기록 없이 끝내기) — 회색 채움 알약 + 빨간 글자, 화살표 없음. [busy] 면 가운데 스피너 */
@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
    icon: ImageVector? = null,
    cue: FeedbackCue = FeedbackCue.Tap,
) {
    val c = StepUpUi.colors
    val fg = if (enabled) c.live else c.onDisabled
    Box(
        modifier
            .heightIn(min = StepUpUi.ButtonHeight)
            .pressable(enabled = enabled && !busy, cue = cue, onClick = onClick)
            .clip(CircleShape)
            .background(if (enabled) c.fill else c.fillSecondary)
            .semantics { if (busy) contentDescription = text }
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (busy) {
            StepUpSpinner(Modifier.size(22.dp), color = fg)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, null, tint = fg, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(7.dp))
                }
                Text(text, style = StepUpUi.type.button, color = fg, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** 보조 버튼 + 처리 중 — [busy] 면 같은 회색 알약 가운데 스피너(다시 누를 수 없다) */
@Composable
fun SecondaryBusyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
    icon: ImageVector? = null,
    cue: FeedbackCue = FeedbackCue.Tap,
) {
    if (busy) {
        Box(
            modifier.heightIn(min = StepUpUi.ButtonHeight).clip(CircleShape).background(StepUpUi.colors.fill)
                .semantics { contentDescription = text },
            contentAlignment = Alignment.Center,
        ) { StepUpSpinner(Modifier.size(22.dp), color = StepUpUi.colors.label) }
    } else {
        SecondaryButton(text, onClick, modifier, icon = icon, enabled = enabled, cue = cue)
    }
}

/**
 * 목록 작은 버튼(가입 · 신청 · 신청됨 · 마감 · 참여). 보이는 높이 32, 터치 영역은 48.
 * 모두 회색 바탕 + 파란 글자 — 사진 위 주 버튼보다 약하게. 신청됨은 시계 + 회색 글자, 마감은 흐리게.
 */
@Composable
fun SmallButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonState = ButtonState.Idle,
) {
    val c = StepUpUi.colors
    val bg = if (kind == ButtonState.Disabled) c.fillSecondary else c.fill
    val fg = when (kind) {
        ButtonState.Disabled -> c.onDisabled
        ButtonState.Waiting, ButtonState.Done -> c.secondaryLabel
        else -> c.accentText
    }
    Box(modifier.minimumInteractiveComponentSize(), contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .height(StepUpUi.SmallButtonHeight)
                .pressable(enabled = kind != ButtonState.Disabled && kind != ButtonState.Loading, onClick = onClick)
                .clip(CircleShape)
                .background(bg)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (kind) {
                ButtonState.Loading -> StepUpSpinner(Modifier.size(14.dp), color = fg)
                ButtonState.Waiting -> {
                    Icon(Icons.Rounded.Schedule, null, tint = fg, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                }
                ButtonState.Done -> {
                    Icon(Icons.Rounded.Check, null, tint = fg, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                }
                else -> Unit
            }
            if (kind != ButtonState.Loading) Text(text, style = StepUpUi.type.smallButton, color = fg, maxLines = 1)
        }
    }
}

/**
 * 뒤로(원 버튼 · 뒤로 소리) — 화면 머리의 뒤로는 모두 이것이다([NavHeader] 도). 화면이 화살표를 따로 그리지 않는다
 * (tools/check_design_contract.py 가 화면 파일의 ArrowBack 을 막는다). 기본 낭독 이름은 "뒤로".
 */
@Composable
fun BackButton(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onPhoto: Boolean = false,
    size: Dp = 36.dp,
    contentDescription: String? = null,
) {
    CircleIconButton(
        Icons.AutoMirrored.Rounded.ArrowBack, contentDescription ?: stringResource(R.string.cd_back), onBack,
        modifier = modifier, size = size, onPhoto = onPhoto, cue = FeedbackCue.Back,
    )
}

/**
 * 원 버튼 — 머리의 뒤로 · 지도 · 글쓰기(36), 옆 공유(54). 회색 채움 원, [onPhoto] 면 유리 원(흰 15% + 위 빛 한 줄).
 * 보이는 크기가 작아도 터치 영역은 48.
 */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    onPhoto: Boolean = false,
    tint: Color? = null,
    enabled: Boolean = true,
    cue: FeedbackCue = FeedbackCue.Tap,
) {
    val c = StepUpUi.colors
    // 눌리는 곳 · 이름 · 꼬리표는 바깥 48 칸 하나에 — 보이는 원이 작아도 48 전체가 눌리고, 눌리는 카드(사진 포스터) 안에 있어도
    // 꼬리표가 카드에 합쳐져 사라지지 않는다
    Box(
        modifier.minimumInteractiveComponentSize()
            .pressable(enabled = enabled, cue = cue, onClick = onClick)
            .semantics { if (contentDescription != null) this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(if (onPhoto) c.glass else c.fill)
                .then(
                    if (onPhoto) Modifier.drawBehind {
                        drawArc(Color.White.copy(alpha = .5f), 200f, 140f, false, style = Stroke(.5.dp.toPx()))
                    } else Modifier,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon, null,
                tint = when {
                    !enabled -> c.onDisabled
                    tint != null -> tint
                    onPhoto -> Color.White
                    else -> c.label
                },
                modifier = Modifier.size(if (size >= 48.dp) 24.dp else 20.dp),
            )
        }
    }
}

/** 22dp 링 0.7초 한 바퀴(tokens motion.loading.spinner) */
@Composable
fun StepUpSpinner(modifier: Modifier = Modifier, color: Color = StepUpUi.colors.primary) {
    val motion = LocalMotion.current
    val angle = remember { Animatable(0f) }
    LaunchedEffect(motion.reduced) {
        if (motion.reduced) return@LaunchedEffect
        while (true) {
            angle.animateTo(360f, tween(700, easing = LinearEasing))
            angle.snapTo(0f)
        }
    }
    Canvas(modifier.semantics { role = Role.Image }) {
        val w = size.minDimension * .12f
        drawArc(color.copy(alpha = .25f), 0f, 360f, false, style = Stroke(w))
        drawArc(color, angle.value - 90f, 100f, false, style = Stroke(w, cap = StrokeCap.Round))
    }
}

// ── 전환 ─────────────────────────────────────────────────────────────

/**
 * 세그먼트 — 회색 트랙(높이 34 · 모서리 11) + 떠 있는 선택 칸(모서리 9 · 그림자). 선택 칸이 스프링으로 미끄러진다.
 * [tags] 를 주면 칸마다 testTag 를 단다(검사가 칸을 집는다).
 */
@Composable
fun StepUpTabs(
    items: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tags: List<String>? = null,
    height: Dp = 34.dp,
) {
    val c = StepUpUi.colors
    val type = StepUpUi.type
    val feedback = LocalFeedback.current
    BoxWithConstraints(
        modifier.heightIn(min = height).clip(RoundedCornerShape(11.dp)).background(c.fill).padding(2.dp),
    ) {
        val count = items.size.coerceAtLeast(1)
        val w = maxWidth / count
        val x by animateDpAsState(
            w * selected.coerceIn(0, count - 1),
            spring(dampingRatio = .8f, stiffness = Spring.StiffnessMediumLow), label = "segThumb",
        )
        Box(
            Modifier.offset(x = x).width(w).height(height - 4.dp)
                .shadow(if (c.dark) 6.dp else 3.dp, RoundedCornerShape(9.dp), ambientColor = Color.Black.copy(alpha = .35f), spotColor = Color.Black.copy(alpha = .35f))
                .clip(RoundedCornerShape(9.dp)).background(c.segmentThumb),
        )
        Row(Modifier.fillMaxWidth()) {
            items.forEachIndexed { i, t ->
                val on = i == selected
                Box(
                    Modifier.weight(1f).heightIn(min = height - 4.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .clickable(role = Role.Tab) { if (!on) { feedback?.play(FeedbackCue.Select); onSelect(i) } }
                        .semantics { this.selected = on }
                        .then(if (tags != null && i < tags.size) Modifier.testTag(tags[i]) else Modifier)
                        .padding(horizontal = 4.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        t, style = type.segment.copy(fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold),
                        color = c.label, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/**
 * 글자 탭 — 이야기 분류(전체 · 자유 · 꿀팁) · 정렬(좋아요 순 · 가까운 순). 고른 글자는 굵고 아래 2dp 파란 줄, 나머지는 회색.
 * 알약 칩 줄을 쓰지 않는다.
 */
@Composable
fun TextTabs(
    items: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tags: List<String>? = null,
) {
    val c = StepUpUi.colors
    val feedback = LocalFeedback.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
        items.forEachIndexed { i, t ->
            val on = i == selected
            Box(
                Modifier
                    .heightIn(min = 40.dp)
                    .clickable(role = Role.Tab) { if (!on) { feedback?.play(FeedbackCue.Select); onSelect(i) } }
                    .semantics { this.selected = on }
                    .then(if (tags != null && i < tags.size) Modifier.testTag(tags[i]) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    t,
                    style = StepUpUi.type.segment.copy(fontSize = 15.sp, fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold, letterSpacing = (-0.03).em),
                    color = if (on) c.label else c.secondaryLabel,
                    maxLines = 1,
                    modifier = Modifier.drawBehind {
                        if (on) drawRect(c.accentText, topLeft = Offset(0f, size.height + 2.dp.toPx()), size = androidx.compose.ui.geometry.Size(size.width, 2.dp.toPx()))
                    },
                )
            }
        }
    }
}

// ── 머리 ─────────────────────────────────────────────────────────────

/** 큰 제목(34 · 800) — 왼쪽 20. 아래 [subtitle] 한 줄(13.5 회색) */
@Composable
fun LargeTitle(title: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(modifier.padding(horizontal = StepUpUi.TitleGutter)) {
        Text(title, style = StepUpUi.type.largeTitle, color = StepUpUi.colors.label, modifier = Modifier.semantics { heading() })
        if (subtitle != null) {
            Text(subtitle, style = StepUpUi.type.rowSubtitle, color = StepUpUi.colors.secondaryLabel, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/**
 * 머리 줄(높이 44) — 왼쪽 뒤로(원 36) · 가운데 제목(17 · 700) · 오른쪽 원 버튼들. [onPhoto] 면 유리 원.
 * 상태바 안전 영역은 부르는 쪽(화면 바닥)이 정한다 — [statusBar] 가 true 면 여기서 더한다.
 */
@Composable
fun NavHeader(
    title: String?,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backDescription: String? = null,
    onPhoto: Boolean = false,
    statusBar: Boolean = false,
    backTag: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(
        modifier.fillMaxWidth().then(if (statusBar) Modifier.statusBarsPadding() else Modifier)
            .heightIn(min = 52.dp).padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (title != null) {
            Text(
                title, style = StepUpUi.type.navTitle, color = if (onPhoto) Color.White else StepUpUi.colors.label,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 64.dp).semantics { heading() },
            )
        }
        if (onBack != null) {
            BackButton(
                onBack, contentDescription = backDescription, onPhoto = onPhoto,
                modifier = Modifier.align(Alignment.CenterStart).then(if (backTag != null) Modifier.testTag(backTag) else Modifier),
            )
        }
        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** 구역 머리(20 · 800) + 오른쪽 "모두 보기"(15 · 600 파랑) */
@Composable
fun GroupHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    actionTag: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().padding(start = StepUpUi.TitleGutter, end = 12.dp, top = 22.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = StepUpUi.type.section, color = StepUpUi.colors.label, modifier = Modifier.weight(1f).semantics { heading() })
        if (action != null && onAction != null) {
            Box(
                Modifier.minimumInteractiveComponentSize().clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction)
                    .then(if (actionTag != null) Modifier.testTag(actionTag) else Modifier).padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(action, style = StepUpUi.type.segment.copy(fontSize = 15.sp), color = StepUpUi.colors.accentText)
            }
        }
    }
}

// ── 사진 카드 ────────────────────────────────────────────────────────

/**
 * 사진 포스터 카드 — 모서리 24, 사진 + 위 · 아래 그라데이션 + 아주 큰 숫자. 사진은 장소 · 시간대별(RunBackground).
 * 사진이 없으면(null) 남색 바탕에 같은 그라데이션.
 */
@Composable
fun PhotoCard(
    photo: Painter?,
    modifier: Modifier = Modifier,
    height: Dp = 388.dp,
    radius: Dp = StepUpUi.PosterRadius,
    colorFilter: ColorFilter? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.fillMaxWidth().heightIn(min = height).clip(RoundedCornerShape(radius)).background(Color(0xFF111111))) {
        if (photo != null) {
            Image(photo, null, contentScale = ContentScale.Crop, colorFilter = colorFilter, modifier = Modifier.matchParentSize())
        }
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = .28f), .3f to Color.Transparent, .5f to Color.Black.copy(alpha = .05f),
                    1f to Color.Black.copy(alpha = .78f),
                ),
            ),
        )
        content()
    }
}

/** LIVE — 배지 아님. 빨간 점 7 + "LIVE" 이탤릭 + " · 남은 시간" 한 줄(사진 위 흰 글자) */
@Composable
fun LiveCaption(detail: String?, modifier: Modifier = Modifier, color: Color = Color.White, live: Boolean = true) {
    val type = StepUpUi.type
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (live) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(StepUpUi.colors.live))
            Spacer(Modifier.width(7.dp))
            Text("LIVE", style = type.liveCaption, color = color)
        }
        if (detail != null) {
            Text(
                if (live) "  ·  $detail" else detail, style = type.liveCaption, color = color,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 이번 주 모임 — 가로로 넘기는 사진 카드 168×196, 모서리 18(LazyRow, 카드 사이 10, 양옆 16) */
@Composable
fun WeekCard(
    photo: Painter?,
    badge: String,
    time: String,
    title: String,
    sub: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.size(168.dp, 196.dp).pressable(onClick = onClick).clip(RoundedCornerShape(18.dp)).background(Color(0xFF111111)),
    ) {
        if (photo != null) Image(photo, null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(.35f to Color.Transparent, 1f to Color.Black.copy(alpha = .78f))))
        Text(
            badge,
            style = StepUpUi.type.smallButton.copy(fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic),
            color = Color.White, modifier = Modifier.padding(14.dp),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
            Text(time, style = StepUpUi.type.numberCard, color = Color.White, maxLines = 1)
            Text(
                title, style = StepUpUi.type.rowTitle.copy(fontSize = 15.sp, letterSpacing = (-0.04).em), color = Color.White,
                modifier = Modifier.padding(top = 7.dp), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                sub, style = StepUpUi.type.caption.copy(fontSize = 12.sp), color = Color.White.copy(alpha = .78f),
                modifier = Modifier.padding(top = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 장소 사진 썸네일 52 · 모서리 12(이니셜 육각형 · 아이콘 타일 대신) */
@Composable
fun PlaceThumb(photo: Painter?, modifier: Modifier = Modifier, size: Dp = 52.dp) {
    Box(modifier.size(size).clip(RoundedCornerShape(StepUpUi.ThumbRadius)).background(StepUpUi.colors.fill)) {
        if (photo != null) Image(photo, null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
    }
}

// ── 목록 ─────────────────────────────────────────────────────────────

/** 묶음 목록 — 모서리 16 + 왼쪽 16 들인 0.5dp 구분선. [rows] 는 줄마다의 내용 */
@Composable
fun GroupedList(modifier: Modifier = Modifier, rows: List<@Composable () -> Unit>) {
    val c = StepUpUi.colors
    Column(modifier.fillMaxWidth().padding(horizontal = StepUpUi.Gutter).clip(RoundedCornerShape(StepUpUi.GroupedRadius)).background(c.card)) {
        rows.forEachIndexed { i, row ->
            if (i > 0) Box(Modifier.padding(start = 16.dp).fillMaxWidth().height(.5.dp).background(c.separator))
            row()
        }
    }
}

/** 묶음 목록 한 줄 — 최소 높이 64(아이콘 줄은 52), 안쪽 12 · 16 */
@Composable
fun GroupRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    minHeight: Dp = StepUpUi.RowMinHeight,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.feedbackClickable(onClick = onClick) else Modifier)
            .heightIn(min = minHeight).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

/** 아이콘 · 제목 · 오른쪽 값 · ">" 줄(길찾기 · 파티 채팅 · 지갑 · 프로필 편집) */
@Composable
fun NavListRow(
    icon: ImageVector?,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    iconTint: Color = StepUpUi.colors.accentText,
) {
    val c = StepUpUi.colors
    GroupRow(modifier, onClick = onClick, minHeight = 54.dp) {
        if (icon != null) Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
        Text(title, style = StepUpUi.type.rowTitle.copy(fontSize = 16.sp), color = c.label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (value != null) {
            Text(value, style = StepUpUi.type.rowSubtitle, color = c.secondaryLabel, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 180.dp))
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.tertiaryLabel, modifier = Modifier.size(20.dp))
    }
}

/** 큰 숫자 + 단위(12sp 이상) — 상자 없이 */
@Composable
fun NumberWithUnit(
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    style: TextStyle = StepUpUi.type.numberStat,
    color: Color = StepUpUi.colors.label,
    unitColor: Color = StepUpUi.colors.secondaryLabel,
    unitSize: TextUnit = 12.sp,
    valueTag: String? = null,
) {
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            value, style = style, color = color, maxLines = 1, softWrap = false,
            modifier = (if (valueTag != null) Modifier.testTag(valueTag) else Modifier).alignByBaseline(),
        )
        if (unit != null) {
            Text(
                " $unit", style = StepUpUi.type.unit.copy(fontSize = maxOf(unitSize.value, 12f).sp), color = unitColor, maxLines = 1,
                modifier = Modifier.alignByBaseline(),
            )
        }
    }
}

/** 숫자 줄 한 칸 — 값(+단위) · 아래 라벨 */
data class StatItem(val label: String, val value: String, val unit: String? = null, val tag: String? = null)

/** 숫자 줄 — 상자 없이 숫자(콘덴스드) + 아래 라벨, 칸을 같은 폭으로 */
@Composable
fun StatRow(
    items: List<StatItem>,
    modifier: Modifier = Modifier,
    numberStyle: TextStyle = StepUpUi.type.numberStat,
    color: Color = StepUpUi.colors.label,
    labelColor: Color = StepUpUi.colors.secondaryLabel,
) {
    Row(modifier.fillMaxWidth()) {
        items.forEach { item ->
            Column(Modifier.weight(1f)) {
                NumberWithUnit(item.value, unit = item.unit, style = numberStyle, color = color, unitColor = labelColor, valueTag = item.tag)
                Text(item.label, style = StepUpUi.type.caption, color = labelColor, modifier = Modifier.padding(top = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ── 입력 ─────────────────────────────────────────────────────────────

/** 입력칸 틀 — 모서리 14, 위에 작은 이름(12.5), 고르면 2dp 파란 테두리 · 이름도 파랑 */
@Composable
fun FieldFrame(
    label: String,
    focused: Boolean,
    modifier: Modifier = Modifier,
    error: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = StepUpUi.colors
    val ring = when {
        error -> c.live
        focused -> c.primary
        else -> Color.Transparent
    }
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(StepUpUi.FieldRadius)).background(c.card)
            .border(2.dp, ring, RoundedCornerShape(StepUpUi.FieldRadius))
            .padding(horizontal = 16.dp, vertical = 11.dp),
    ) {
        Text(
            label, style = StepUpUi.type.caption.copy(fontSize = 12.5.sp),
            color = if (error) c.live else if (focused) c.accentText else c.secondaryLabel,
        )
        Spacer(Modifier.height(3.dp))
        content()
    }
}

/** 숫자 조절 — [−][값 단위][+] (회색 칸 190×44 · 모서리 12). 숫자 입력칸 셋 대신 */
@Composable
fun NumberStepper(
    value: String,
    unit: String?,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    minusEnabled: Boolean = true,
    plusEnabled: Boolean = true,
    minusDescription: String? = null,
    plusDescription: String? = null,
    tag: String? = null,
) {
    val c = StepUpUi.colors
    Row(
        modifier.widthIn(min = 170.dp).height(44.dp).clip(RoundedCornerShape(12.dp)).background(c.fill),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperKey("−", minusEnabled, onMinus, minusDescription, tag?.let { "$it-minus" })
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            NumberWithUnit(value, unit = unit, style = StepUpUi.type.numberRow, valueTag = tag?.let { "$it-value" })
        }
        StepperKey("+", plusEnabled, onPlus, plusDescription, tag?.let { "$it-plus" })
    }
}

@Composable
private fun StepperKey(symbol: String, enabled: Boolean, onClick: () -> Unit, description: String?, tag: String?) {
    val c = StepUpUi.colors
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
            .pressable(enabled = enabled, cue = FeedbackCue.Select, onClick = onClick)
            .semantics { if (description != null) contentDescription = description }
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = StepUpUi.type.button.copy(fontSize = 22.sp, fontWeight = FontWeight.Normal), color = if (enabled) c.accentText else c.onDisabled)
    }
}

// ── 시트 ─────────────────────────────────────────────────────────────

/** 시트 손잡이 36×5 */
@Composable
fun SheetHandle(modifier: Modifier = Modifier) {
    Box(modifier.padding(top = 8.dp, bottom = 6.dp).size(36.dp, 5.dp).clip(CircleShape).background(StepUpUi.colors.handle))
}

// ── 길게 눌러 종료 ───────────────────────────────────────────────────

/**
 * 길게 눌러 러닝 종료 — 1초 동안 링(4dp)이 차면 [onEnd]. 짧게 누르면 [onHint]("길게 눌러 종료해요").
 * 누르는 동안 가운데가 0.94배로 들어가고 [onHolding] 이 true(말풍선 "손을 떼면 계속 달려요 · 1초면 종료"를 띄우는 자리).
 * "러닝을 끝낼까요?" 확인창은 두지 않는다 — 두 번 막지 않는다.
 */
@Composable
fun HoldToEndButton(
    onEnd: () -> Unit,
    onHint: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 68.dp,
    holdMillis: Int = 1000,
    enabled: Boolean = true,
    contentDescription: String? = null,
    onHolding: (Boolean) -> Unit = {},
    /** 가운데 그림 — 없으면 정지 네모(종료). 잠금 풀기는 자물쇠 */
    icon: ImageVector? = null,
    /** 짧게 눌렀을 때 읽기 도구가 읽는 이름("길게 눌러 종료해요") */
    hintLabel: String? = null,
) {
    val c = StepUpUi.colors
    val haptic = LocalHapticFeedback.current
    val feedback = LocalFeedback.current
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    var done by remember { mutableStateOf(false) }
    val end by rememberUpdatedState(onEnd)
    val hint by rememberUpdatedState(onHint)
    val holding by rememberUpdatedState(onHolding)
    val inner = if (c.dark) Color.White else Color.Black
    val glyph = if (c.dark) Color.Black else Color.White
    Box(
        modifier
            .size(size)
            .semantics {
                role = Role.Button
                if (contentDescription != null) this.contentDescription = contentDescription
                // 읽기 도구 · 기기 검사 — 두 번 눌러 길게(종료), 두 번 눌러(안내)
                if (enabled) {
                    onClick(label = hintLabel) { hint(); true }
                    onLongClick(label = contentDescription) {
                        if (!done) { done = true; feedback?.play(FeedbackCue.Finish); end() }
                        true
                    }
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(onPress = {
                    holding(true)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val job = scope.launch {
                        progress.animateTo(1f, tween(holdMillis, easing = LinearEasing))
                        done = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        feedback?.play(FeedbackCue.Finish)
                        end()
                    }
                    tryAwaitRelease()
                    holding(false)
                    if (progress.value < 1f) {
                        job.cancel()
                        scope.launch { progress.animateTo(0f, tween(200)) }
                        hint()
                    }
                })
            }
            .drawBehind {
                val w = 4.dp.toPx()
                val inset = w / 2f
                drawArc(
                    c.fill, -90f, 360f, false, style = Stroke(w),
                    topLeft = Offset(inset, inset), size = androidx.compose.ui.geometry.Size(this.size.width - w, this.size.height - w),
                )
                drawArc(
                    c.primary, -90f, 360f * progress.value, false, style = Stroke(w, cap = StrokeCap.Round),
                    topLeft = Offset(inset, inset), size = androidx.compose.ui.geometry.Size(this.size.width - w, this.size.height - w),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(size - 16.dp)
                .graphicsLayer { val s = 1f - .06f * progress.value; scaleX = s; scaleY = s }
                .clip(CircleShape)
                .background(inner),
            contentAlignment = Alignment.Center,
        ) {
            when {
                done -> Icon(Icons.Rounded.Check, null, tint = glyph, modifier = Modifier.size(24.dp))
                icon != null -> Icon(icon, null, tint = glyph, modifier = Modifier.size(22.dp))
                else -> Box(Modifier.size(16.dp).clip(RoundedCornerShape(2.dp)).background(glyph))
            }
        }
    }
}

/** 길게 누르는 동안 버튼 위 말풍선(검은 알약 · 흰 글자, 라이트는 반대) */
@Composable
fun HoldBubble(text: String, modifier: Modifier = Modifier) {
    val c = StepUpUi.colors
    Box(
        modifier.clip(RoundedCornerShape(10.dp)).background(if (c.dark) Color.White else Color(0xFF111111))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text, style = StepUpUi.type.caption.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp), color = if (c.dark) Color.Black else Color.White)
    }
}

/** 원 컨트롤(러닝 일시정지 · 재개) — 블루 원 96 + 흰 아이콘, 아래 굵은 이름 */
@Composable
fun RoundControl(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    size: Dp = if (primary) 96.dp else 60.dp,
    tag: String? = null,
    cue: FeedbackCue = FeedbackCue.Tap,
    enabled: Boolean = true,
) {
    val c = StepUpUi.colors
    // 원과 아래 이름이 한 덩어리 — 이름까지 누를 수 있고, 읽기 도구는 이름을 한 번 읽는다
    Column(
        modifier.pressable(enabled = enabled, cue = cue, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(size).clip(CircleShape).background(if (primary) c.primary else c.fill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = if (!enabled) c.onDisabled else if (primary) Color.White else c.label,
                modifier = Modifier.size(if (primary) 44.dp else 24.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            label, style = StepUpUi.type.caption.copy(fontWeight = if (primary) FontWeight.Bold else FontWeight.Normal),
            color = if (!enabled) c.onDisabled else if (primary) c.label else c.secondaryLabel, maxLines = 1,
        )
    }
}

/** 화면 바닥 — 통합안 바닥색 한 장(번지는 빛 없음) */
@Composable
fun StepUpBackground(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(StepUpUi.colors.background))
}

/** 이미지 비트맵을 Painter 로(장소 사진 등) */
@Composable
fun rememberBitmapPainter(bitmap: ImageBitmap?): Painter? =
    remember(bitmap) { bitmap?.let { androidx.compose.ui.graphics.painter.BitmapPainter(it) } }

/** 화면 맨 아래 주 버튼 자리(좌우 16 · 아래 16) */
@Composable
fun BottomActionBar(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = StepUpUi.Gutter).padding(top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** 줄 오른쪽 위 작은 글자 + 큰 글자 묶음(크루 이름 · 동네 줄) */
@Composable
fun TitleSubtitle(title: String, subtitle: String?, modifier: Modifier = Modifier, titleColor: Color = StepUpUi.colors.label, subtitleColor: Color = StepUpUi.colors.secondaryLabel) {
    Column(modifier) {
        Text(title, style = StepUpUi.type.rowTitle, color = titleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (!subtitle.isNullOrBlank()) {
            Text(subtitle, style = StepUpUi.type.rowSubtitle, color = subtitleColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

/** 상태바까지 덮는 시트 뒤 막 — [visible] 동안 250ms 로 어두워진다 */
@Composable
fun SheetScrim(visible: Boolean, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(250), label = "scrim")
    if (alpha > 0f) {
        Box(
            modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }.background(StepUpUi.colors.sheetScrim)
                .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        )
    }
}

/** 둥근 위 모서리 시트 면(28) + 손잡이 */
@Composable
fun SheetSurface(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(topStart = StepUpUi.SheetRadius, topEnd = StepUpUi.SheetRadius))
            .background(if (StepUpUi.colors.dark) StepUpUi.colors.card else Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SheetHandle()
        Column(Modifier.fillMaxWidth(), content = content)
    }
}

/** 오류 · 무효 한 줄 카드(빨간 옅은 바탕 + 느낌표) — "걸음이 잡히지 않아 적립 없음 · 기록은 저장했어요" */
@Composable
fun ErrorNote(title: String, body: String?, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val c = StepUpUi.colors
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(StepUpUi.GroupedRadius)).background(c.errorFill).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(20.dp).clip(CircleShape).background(c.live), contentAlignment = Alignment.Center) {
            if (icon != null) Icon(icon, null, tint = Color.White, modifier = Modifier.size(14.dp))
            else Text("!", style = StepUpUi.type.smallButton.copy(fontSize = 13.sp), color = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = StepUpUi.type.rowTitle.copy(fontSize = 15.sp), color = c.live)
            if (body != null) Text(body, style = StepUpUi.type.caption, color = c.live.copy(alpha = .8f), modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/** 화면 가운데 빈 상태 — 아이콘 타일 없이 문장 한 줄 + (있으면) 주 버튼 */
@Composable
fun EmptyLine(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(horizontal = StepUpUi.TitleGutter, vertical = 18.dp), horizontalAlignment = Alignment.Start) {
        Text(text, style = StepUpUi.type.body, color = StepUpUi.colors.secondaryLabel)
        if (action != null) {
            Spacer(Modifier.height(14.dp))
            action()
        }
    }
}

/** 화면 맨 위 상태바 높이만큼 비우는 자리 */
@Composable
fun StatusBarSpacer() {
    Spacer(Modifier.fillMaxWidth().statusBarsPadding())
}

/** 세로 꽉 찬 칸 안 가운데 정렬 — 미리보기용 보조 */
@Composable
internal fun FillCenter(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxHeight().fillMaxWidth(), contentAlignment = Alignment.Center, content = content)
}
