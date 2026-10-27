package com.stepup.android.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.TransformOrigin
import kotlin.math.roundToInt
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.LocalFeedback
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.BrandLogoRole
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.StepUpSans
import com.stepup.android.ui.theme.StepUpNumbers

/*
 * 러닝 전체 리메이크(2026-10-02 전달본 "러닝 109장 · 신발 색감") 공통 부품.
 *
 * 시안 HOME.png 의 남색 · 전기 파랑 · 흰 버튼 — 바닥 #031427, 패널 #0B2B50, 주 파랑 #0754FF, 보조 시안 #48D9FA,
 * 주 버튼 면과 글자 #F5F8FF, 보조 글자 #AAC3EA. 주 버튼은 모서리를 깎은 흰 면 · 남색 굵은 글자 · 얇은 파란 아랫면,
 * 보조 버튼은 남색 면 · 파란 테두리. 큰 숫자만 기울인 굵은 글꼴(운동 숫자)이다.
 *
 * 러닝 화면들(러닝 홈 · 시작 · 달리는 중 · 결과 · 기록 · 코스 · 다이어트 · 크루 달리기)만 쓴다 — 다른 네 탭의 테마는
 * 건드리지 않는다. 밝은 테마는 같은 자리에 밝은 값을 둔다.
 */

/** 러닝 화면의 색 — 시안은 어두운 남색 한 벌이고, 밝은 테마 값은 거기서 끌어냈다 */
@Immutable
class RunTone(
    val dark: Boolean,
    /** 화면 바닥 — 위가 아주 조금 밝다 */
    val screenTop: Color,
    val screen: Color,
    val screenBottom: Color,
    /** 오른쪽 위에서 번지는 파란 빛 */
    val glow: Color,
    /** 패널(카드) 면 — 위 · 아래 */
    val panelTop: Color,
    val panel: Color,
    /** 패널 둘레 — 파란 선 */
    val panelEdge: Color,
    /** 패널 안 한 단계 더 깊은 칸(입력칸 · 지도 바탕) */
    val inset: Color,
    /** 제목 · 본문 */
    val text: Color,
    /** 보조 글자(#AAC3EA) */
    val label: Color,
    /** 더 흐린 글자 */
    val muted: Color,
    /** 주 파랑(#0754FF) — 채운 강조 */
    val cobalt: Color,
    /** 표면 위에 글자로 쓰는 파랑 */
    val cobaltText: Color,
    /** 보조 강조 시안(#48D9FA) — 빛나는 수 · 진행 막대 끝 */
    val cyan: Color,
    /** 주 버튼 면(위 → 아래)과 글자, 아랫면 */
    val primaryFace: List<Color>,
    val primaryInk: Color,
    val primaryBase: Color,
    /** 보조 버튼 면 · 테두리 · 글자 */
    val secondaryFace: Color,
    val secondaryEdge: Color,
    val secondaryInk: Color,
    /** 위험 — 삭제 · 기록 없이 끝내기 */
    val dangerFace: Color,
    val dangerBase: Color,
    val dangerInk: Color,
    val dangerText: Color,
    /** 누를 수 없는 버튼 */
    val disabledFace: Color,
    val disabledInk: Color,
    val disabledBase: Color,
    val divider: Color,
    val track: Color,
    val barFill: List<Color>,
    /** 시트 · 창 */
    val sheetTop: Color,
    val sheet: Color,
    val sheetEdge: Color,
    val handle: Color,
    val scrim: Color,
    /** 작은 알약(확인 중 · 예상) */
    val chipFace: Color,
    val chipEdge: Color,
    val chipInk: Color,
    /** 확인이 필요한 띠(노랑 대신 남색 위 시안 느낌표) · 무효(빨강) */
    val warnFace: Color,
    val warnEdge: Color,
    val warnIcon: Color,
    val errorFace: Color,
    val errorEdge: Color,
    val errorIcon: Color,
    /**
     * 지도 타일 색 — 실제 타일(MapTiler streets · OSM)의 색만 옮긴다(길 · 물 · 이름은 그대로). null 이면 앱 테마의 지도 색.
     * 러닝 화면은 두 테마 모두 밝은 거리 지도([BrightMapFilter], 2026-10-17 — 예전 남색 지도를 대신한다).
     */
    val mapFilter: ColorFilter? = null,
    /** 타일 위에 덮는 바탕색의 짙기 — 밝은 거리 지도는 덮지 않는다 */
    val mapShade: Float = 0.30f,
    /** 길만 밝히는 두 번째 겹(더하기, [mapFilter] 와 짝). null 이면 한 겹 */
    val mapRoads: ColorFilter? = null,
)

// UI 최종 통합안(2026-10-25) — 러닝 화면도 앱과 같은 다크블루 · 화이트블루 한 벌이다. 예전 남색 무대(바닥 그라데이션 · 파란 빛 ·
// 모서리 깎은 흰 버튼)는 없앴다. 주 버튼은 블루 알약 + 원 화살표([PrimaryButton]), 보조는 회색 채움 알약([SecondaryButton]).
val NavyRunTone = RunTone(
    dark = true,
    screenTop = Color(0xFF070B14), screen = Color(0xFF070B14), screenBottom = Color(0xFF070B14),
    glow = Color.Transparent,
    panelTop = Color(0xFF0E1424), panel = Color(0xFF0E1424), panelEdge = Color(0x17FFFFFF),
    inset = Color(0xFF151C2D),
    text = Color.White, label = Color(0xFF969CAA), muted = Color(0xFF6E7586),
    cobalt = Color(0xFF2F6BFF), cobaltText = Color(0xFF6E9BFF), cyan = Color(0xFF6E9BFF),
    primaryFace = listOf(Color(0xFF2F6BFF), Color(0xFF2F6BFF)), primaryInk = Color.White,
    primaryBase = Color.Transparent,
    secondaryFace = Color(0xFF202736), secondaryEdge = Color.Transparent, secondaryInk = Color.White,
    dangerFace = Color(0xFFFF453A), dangerBase = Color.Transparent, dangerInk = Color.White,
    dangerText = Color(0xFFFF453A),
    disabledFace = Color(0xFF141A27), disabledInk = Color(0xFF5D6371), disabledBase = Color.Transparent,
    divider = Color(0x17FFFFFF), track = Color(0xFF222A3D),
    barFill = listOf(Color(0xFF2F6BFF), Color(0xFF2F6BFF)),
    sheetTop = Color(0xFF0E1424), sheet = Color(0xFF0E1424), sheetEdge = Color.Transparent,
    handle = Color(0x4DE2E8F5), scrim = Color(0x8C000000),
    chipFace = Color(0xFF202736), chipEdge = Color.Transparent, chipInk = Color(0xFFE2E8F5),
    warnFace = Color(0xFF0E1424), warnEdge = Color.Transparent, warnIcon = Color(0xFF6E9BFF),
    errorFace = Color(0xFF2A1219), errorEdge = Color.Transparent, errorIcon = Color(0xFFFF453A),
    // 지도 — 다크블루는 앱 테마의 어두운 지도(시안 러닝 화면 다크 판), 화이트블루는 밝은 거리 지도
    mapFilter = null, mapShade = 0.30f, mapRoads = null,
)

val LightRunTone = RunTone(
    dark = false,
    screenTop = Color(0xFFF2F2F7), screen = Color(0xFFF2F2F7), screenBottom = Color(0xFFF2F2F7),
    glow = Color.Transparent,
    panelTop = Color.White, panel = Color.White, panelEdge = Color(0x293C3C43),
    inset = Color(0xFFEFEFF4),
    text = Color.Black, label = Color(0xFF848489), muted = Color(0xFF9C9CA2),
    cobalt = Color(0xFF1F5EFF), cobaltText = Color(0xFF1F5EFF), cyan = Color(0xFF1F5EFF),
    primaryFace = listOf(Color(0xFF1F5EFF), Color(0xFF1F5EFF)), primaryInk = Color.White,
    primaryBase = Color.Transparent,
    secondaryFace = Color(0xFFE3E3E8), secondaryEdge = Color.Transparent, secondaryInk = Color.Black,
    dangerFace = Color(0xFFFF3B30), dangerBase = Color.Transparent, dangerInk = Color.White,
    dangerText = Color(0xFFFF3B30),
    disabledFace = Color(0xFFE8E8ED), disabledInk = Color(0xFFA9A9AE), disabledBase = Color.Transparent,
    divider = Color(0x293C3C43), track = Color(0xFFE3E3E8),
    barFill = listOf(Color(0xFF1F5EFF), Color(0xFF1F5EFF)),
    sheetTop = Color.White, sheet = Color.White, sheetEdge = Color.Transparent,
    handle = Color(0x4D3C3C43), scrim = Color(0x40000000),
    chipFace = Color(0xFFE3E3E8), chipEdge = Color.Transparent, chipInk = Color(0xFF3C3C43),
    warnFace = Color.White, warnEdge = Color.Transparent, warnIcon = Color(0xFF1F5EFF),
    errorFace = Color(0xFFFFECEB), errorEdge = Color.Transparent, errorIcon = Color(0xFFFF3B30),
    mapFilter = BrightMapFilter, mapShade = 0f,
)

@Composable
fun runTone(): RunTone = if (StepUpColors.dark) NavyRunTone else LightRunTone

object RunSpec {
    val Gutter = 16.dp
    val PrimaryHeight = 54.dp
    val PrimaryBase = 0.dp
    val SecondaryHeight = 54.dp
    val HeroHeight = 54.dp
    val HeroBase = 0.dp
    val CardRadius = 16.dp
    val SheetRadius = 28.dp
    val Gap = 12.dp
}

// ── 글자 ───────────────────────────────────────────────────────────

/** 큰 운동 숫자 — 숫자 글꼴(콘덴스드, 기울이지 않음 · 자간 0). 본문에는 쓰지 않는다 */
fun runNumberStyle(size: TextUnit, color: Color, weight: FontWeight = FontWeight.ExtraBold): TextStyle = TextStyle(
    fontFamily = StepUpNumbers, fontWeight = weight, fontSize = size, color = color,
    letterSpacing = 0.em, fontFeatureSettings = "tnum", lineHeight = 1.0.em,
    // 숫자 글꼴의 깊은 아랫부분 — 줄 상자 가운데 맞춤(StepUpUi num() 과 같다)
    lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
        androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center, androidx.compose.ui.text.style.LineHeightStyle.Trim.None,
    ),
)

fun runTextStyle(size: TextUnit, color: Color, weight: FontWeight = FontWeight.Normal, height: Float = 1.4f): TextStyle =
    TextStyle(
        fontFamily = StepUpSans, fontWeight = weight, fontSize = size, color = color,
        letterSpacing = (if (size.value <= 13f) -0.02 else -0.03).em, lineHeight = height.em,
    )

// ── 바닥 ───────────────────────────────────────────────────────────

/** 러닝 화면 바닥 — 통합안 바닥색 한 장(번지는 빛 없음) */
@Composable
fun RunBackdrop(modifier: Modifier = Modifier) {
    val t = runTone()
    Canvas(modifier) { drawRect(t.screen) }
}

// ── 모양 ───────────────────────────────────────────────────────────

/** 네 모서리를 45°로 깎은 모양(시안의 주 · 보조 버튼) */
class ChamferShape(private val cut: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val c = with(density) { cut.toPx() }.coerceAtMost(minOf(size.width, size.height) / 2f)
        val w = size.width
        val h = size.height
        return Outline.Generic(
            Path().apply {
                moveTo(c, 0f); lineTo(w - c, 0f); lineTo(w, c); lineTo(w, h - c)
                lineTo(w - c, h); lineTo(c, h); lineTo(0f, h - c); lineTo(0f, c); close()
            },
        )
    }
}

private fun chamferPath(size: Size, cut: Float, top: Float = 0f, height: Float = size.height): Path {
    val c = cut.coerceAtMost(minOf(size.width, height) / 2f)
    val w = size.width
    val b = top + height
    return Path().apply {
        moveTo(c, top); lineTo(w - c, top); lineTo(w, top + c); lineTo(w, b - c)
        lineTo(w - c, b); lineTo(c, b); lineTo(0f, b - c); lineTo(0f, top + c); close()
    }
}

// ── 버튼 ───────────────────────────────────────────────────────────

enum class RunButtonKind { Primary, Secondary, Danger }

/**
 * 러닝 버튼 — 통합안 버튼으로 그린다. 주 = 블루 알약 54 + 끝 원 화살표([PrimaryButton]), 보조 = 회색 채움 알약,
 * 위험 = 회색 알약 + 빨간 글자(화살표 없음). [busy] 면 가운데 스피너만, 누를 수 없으면 회색. [hero] · [italic] 은 예전 모양 값이라 무시한다.
 */
@Composable
fun RunButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: RunButtonKind = RunButtonKind.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    busy: Boolean = false,
    hero: Boolean = false,
    cue: FeedbackCue = FeedbackCue.Tap,
    /** 오른쪽 끝 화살표 — 통합안은 주 버튼에 늘 원 화살표를 단다(보조 · 위험 버튼에는 없다) */
    chevron: Boolean = false,
    italic: Boolean = hero,
) {
    when (kind) {
        RunButtonKind.Primary -> PrimaryButton(
            label, onClick, modifier.fillMaxWidth(),
            state = when {
                busy -> ButtonState.Loading
                !enabled -> ButtonState.Disabled
                else -> ButtonState.Idle
            },
            icon = icon, cue = cue,
        )
        RunButtonKind.Secondary -> SecondaryBusyButton(label, onClick, modifier.fillMaxWidth(), enabled = enabled, busy = busy, icon = icon, cue = cue)
        RunButtonKind.Danger -> DangerButton(label, onClick, modifier.fillMaxWidth(), enabled = enabled, busy = busy, icon = icon, cue = cue)

    }
}

/** 글자만 있는 작은 행동 — "기록 없이 끝내기"(빨강) · "자유 러닝으로 시작 >" */
@Composable
fun RunTextAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    chevron: Boolean = false,
    enabled: Boolean = true,
) {
    val t = runTone()
    Row(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
            .feedbackClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val color = when {
            !enabled -> t.muted
            danger -> t.dangerText
            else -> t.cobaltText
        }
        Text(label, style = runTextStyle(15.sp, color, FontWeight.SemiBold), textAlign = TextAlign.Center)
        if (chevron) Icon(Icons.Filled.ChevronRight, null, tint = color, modifier = Modifier.size(20.dp))
    }
}

/** 작은 보조 버튼 — 아이콘 + 글자(공유하기 · 내 러닝 기록 보기). 회색 채움 알약, 둘씩 나란히 둔다 */
@Composable
fun RunTileButton(
    label: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    SecondaryButton(label, onClick, modifier, icon = icon, enabled = enabled)
}

// ── 머리 · 제목 ─────────────────────────────────────────────────────

/**
 * 러닝 화면 머리 — 왼쪽 뒤로(회색 원 36), 오른쪽은 그 화면의 행동 하나(삭제 · SUP 잔액 등)만. 통합안 머리에는 로고를 두지 않는다
 * (첫 화면 머리만 로고). [crumb] 가 있으면(크루 화면) 가운데 작은 이름.
 */
@Composable
fun RunTopBar(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    crumb: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    /** 뒤로 버튼 꼬리표 — 권한 안내처럼 닫기와 같은 뜻이면 그 이름으로 */
    backTag: String = "kit-back",
    /** 테마와 상관없이 늘 짙은 화면(리더보드) — 흰 글자 · 유리 원 */
    onNavy: Boolean = false,
    /** 가운데 제목(17 · 700) — 시안 머리 줄 */
    title: String? = null,
) {
    val t = runTone()
    Box(
        modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        val center = title ?: crumb
        if (center != null) {
            Text(
                center, style = StepUpUi.type.navTitle, color = if (onNavy) Color.White else t.text, maxLines = 1,
                overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 64.dp).semantics { heading() }
                    .then(if (crumb != null && title == null) Modifier.testTag("run-crumb") else Modifier),
            )
        }
        if (onBack != null) {
            CircleIconButton(
                Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back), onBack,
                modifier = Modifier.align(Alignment.CenterStart).testTag(backTag), onPhoto = onNavy, cue = FeedbackCue.Back,
            )
        }
        if (trailing != null) {
            Box(Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)) { trailing() }
        }
    }
}

/** 머리 오른쪽 글자 행동 — "삭제"(빨강, 휴지통) */
@Composable
fun RunHeaderAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    danger: Boolean = false,
    enabled: Boolean = true,
) {
    val t = runTone()
    val color = when {
        !enabled -> t.muted
        danger -> t.dangerText
        else -> t.label
    }
    Row(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
            .feedbackClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(label, style = runTextStyle(16.sp, color, FontWeight.SemiBold))
    }
}

/** 화면 제목 — 굵은 큰 제목(30 · 800, 메뉴 화면 [display] 는 34) · 한 줄 설명. 기울이지 않는다 */
@Composable
fun RunHeadline(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    display: Boolean = false,
    align: TextAlign = TextAlign.Start,
    titleTag: String? = null,
) {
    val t = runTone()
    Column(modifier.fillMaxWidth(), horizontalAlignment = if (align == TextAlign.Center) Alignment.CenterHorizontally else Alignment.Start) {
        val style = TextStyle(
            fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = if (display) 34.sp else 30.sp,
            lineHeight = 1.25.em, letterSpacing = (-0.045).em, color = t.text,
        )
        Text(
            title, style = style, textAlign = align,
            modifier = Modifier.semantics { heading() }.then(if (titleTag != null) Modifier.testTag(titleTag) else Modifier),
        )
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = runTextStyle(15.sp, t.label, FontWeight.Normal), textAlign = align)
        }
    }
}

/**
 * 러닝 화면 틀 — 바닥 · 머리(뒤로 · 로고) · 제목 · 내용(넘김) · 아래 버튼. 버튼은 키보드 · 시스템 아래 영역 위에 선다.
 */
@Composable
fun RunPage(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    display: Boolean = false,
    crumb: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    scroll: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = RunSpec.Gutter),
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    backTag: String = "kit-back",
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        RunBackdrop(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            RunTopBar(onBack = onBack, crumb = crumb, trailing = trailing, backTag = backTag)
            Column(
                Modifier.weight(1f).fillMaxWidth()
                    .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(contentPadding),
            ) {
                if (title != null) {
                    Spacer(Modifier.height(4.dp))
                    RunHeadline(title, subtitle = subtitle, display = display, titleTag = "run-page-title")
                    Spacer(Modifier.height(20.dp))
                }
                content()
                Spacer(Modifier.height(16.dp))
            }
            if (bottom != null) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = RunSpec.Gutter).padding(top = 8.dp, bottom = 16.dp).imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = bottom,
                )
            }
            footer?.invoke()
        }
    }
}

// ── 상태 알약 ───────────────────────────────────────────────────────

enum class RunPillKind { Live, Paused, Saved, Pending, Neutral, Alert }

/**
 * 상태 한 줄 — 통합안은 상태를 알약(칩)으로 쓰지 않는다. 앞에 점(기록 중 · 확인 중) 또는 아이콘, 뒤에 글자 한 줄.
 * 기록 중 = 초록 점, 일시정지 = 일시정지 아이콘, 저장 완료 = 체크, 경고 = 빨간 점.
 */
@Composable
fun RunStatusPill(text: String, kind: RunPillKind, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val t = runTone()
    val ui = StepUpUi.colors
    val ink = when (kind) {
        RunPillKind.Alert -> t.dangerText
        RunPillKind.Live, RunPillKind.Saved -> t.text
        else -> t.label
    }
    Row(
        modifier.heightIn(min = 28.dp).padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            icon != null -> Icon(icon, null, tint = if (kind == RunPillKind.Saved) ui.success else ink, modifier = Modifier.size(16.dp))
            kind == RunPillKind.Live -> Box(Modifier.size(7.dp).clip(CircleShape).background(ui.success))
            kind == RunPillKind.Paused -> Icon(Icons.Filled.Pause, null, tint = ink, modifier = Modifier.size(16.dp))
            kind == RunPillKind.Saved -> Icon(Icons.Filled.CheckCircle, null, tint = ui.success, modifier = Modifier.size(16.dp))
            kind == RunPillKind.Pending -> Box(Modifier.size(7.dp).clip(CircleShape).background(t.cobaltText))
            kind == RunPillKind.Alert -> Box(Modifier.size(7.dp).clip(CircleShape).background(ui.live))
            else -> Unit
        }
        if (kind != RunPillKind.Neutral || icon != null) Spacer(Modifier.width(7.dp))
        Text(text, style = runTextStyle(14.sp, ink, FontWeight.Medium), maxLines = 1)
    }
}

// ── 숫자 ───────────────────────────────────────────────────────────

/**
 * 큰 수 + 작은 단위 한 줄 — 폭이 모자라면(큰 글씨 · 좁은 화면) 둘을 같은 비율로 줄인다. 두 줄로 넘기지 않는다.
 * 수는 숫자 글꼴(콘덴스드), 단위는 한글 글꼴 — 단위는 12sp 보다 작아지지 않는다.
 */
@Composable
fun RunNumber(
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    size: TextUnit = 40.sp,
    unitSize: TextUnit = size * 0.45f,
    color: Color = runTone().text,
    unitColor: Color = runTone().label,
    align: Alignment.Horizontal = Alignment.Start,
    valueTag: String? = null,
    italicUnit: Boolean = true,
) {
    val valueStyle = runNumberStyle(size, color)
    val unitStyle = runTextStyle(if (unitSize.value < 12f) 12.sp else unitSize, unitColor, FontWeight.Medium, 1.1f)
    // 폭이 모자라면 한 번 잰 줄을 그대로 줄여 그린다 — 예전처럼 BoxWithConstraints(그리는 중에 한 번 더 짜기)와 글자 따로 재기를
    // 하지 않는다. 이 수는 한 화면에 여러 개(목록 줄마다)라 그 비용이 쌓이면 넘김이 버벅인다.
    Row(
        modifier.then(FitScale(align)),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            value, style = valueStyle, maxLines = 1, softWrap = false,
            modifier = Modifier.alignByBaseline().then(if (valueTag != null) Modifier.testTag(valueTag) else Modifier),
        )
        if (unit != null) {
            Text(" $unit", style = unitStyle, maxLines = 1, softWrap = false, modifier = Modifier.alignByBaseline())
        }
    }
}


/** 가운데 큰 시간 — 작은 이름("달린 시간") · 아주 큰 기울인 수 · 아래 한 줄 */
@Composable
fun RunHero(
    label: String?,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    caption: String? = null,
    dim: Boolean = false,
    size: TextUnit = 84.sp,
    labelColor: Color? = null,
    valueTag: String = "run-hero-value",
) {
    val t = runTone()
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (label != null) {
            Text(label, style = runTextStyle(17.sp, labelColor ?: t.label, FontWeight.SemiBold), textAlign = TextAlign.Center)
        }
        RunNumber(
            value, unit = unit, size = size, unitSize = size * 0.4f,
            color = if (dim) t.muted else t.text, align = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(), valueTag = valueTag,
        )
        if (caption != null) {
            Text(caption, style = runTextStyle(15.sp, t.label, FontWeight.Medium), textAlign = TextAlign.Center)
        }
    }
}

/** 수치 한 칸 */
@Immutable
data class RunStat(
    val label: String,
    val value: String,
    val unit: String? = null,
    val caption: String? = null,
    val tag: String? = null,
    val accent: Boolean = false,
)

/**
 * 수치 묶음 — 상자 없이 같은 폭 칸들을 한 줄로. 칸마다 큰 수(+단위) · 아래 이름(시안 숫자 줄). [framed] 면 카드 안에 둔다.
 */
@Composable
fun RunStatRow(
    stats: List<RunStat>,
    modifier: Modifier = Modifier,
    framed: Boolean = false,
    valueSize: TextUnit = 34.sp,
    dim: Boolean = false,
) {
    val t = runTone()
    val inner = @Composable {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            stats.forEach { stat ->
                // 칸은 한 덩어리로 읽힌다(이름 · 값 · 단위). 꼬리표는 값 글자에 — 검사는 펼친 트리에서 값만 읽는다
                Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                    RunNumber(
                        stat.value, unit = stat.unit, size = valueSize, unitSize = 12.sp,
                        color = when {
                            dim -> t.muted
                            stat.accent -> t.cobaltText
                            else -> t.text
                        },
                        modifier = Modifier.fillMaxWidth(), valueTag = stat.tag,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(stat.label, style = runTextStyle(13.sp, t.label, FontWeight.Normal), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (stat.caption != null) {
                        Text(stat.caption, style = runTextStyle(12.sp, t.muted, FontWeight.Normal), maxLines = 1)
                    }
                }
            }
        }
    }
    if (framed) {
        RunCard(modifier, padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) { inner() }
    } else {
        Box(modifier) { inner() }
    }
}

// ── 막대 ───────────────────────────────────────────────────────────

/** 진행 막대 — 회색 길 위 블루 채움(그라데이션 없음). [inside] 가 있으면 채운 끝에 작은 글자 */
@Composable
fun RunMeter(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    inside: String? = null,
    fill: List<Color>? = null,
) {
    val t = runTone()
    val colors = fill ?: t.barFill
    val f = fraction.coerceIn(0f, 1f)
    Box(
        modifier.fillMaxWidth().height(if (inside != null) maxOf(height, 18.dp) else height)
            .drawBehind {
                val r = CornerRadius(size.height / 2f)
                drawRoundRect(t.track, cornerRadius = r)
                if (f > 0f) {
                    val w = (size.width * f).coerceAtLeast(size.height)
                    drawRoundRect(
                        if (colors.size > 1) Brush.horizontalGradient(colors, endX = w.coerceAtLeast(1f)) else Brush.horizontalGradient(listOf(colors[0], colors[0])),
                        size = Size(w, size.height), cornerRadius = r,
                    )
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        if (inside != null && f > 0.12f) {
            Box(Modifier.fillMaxWidth(f), contentAlignment = Alignment.CenterEnd) {
                Text(inside, style = runTextStyle(11.sp, Color.White, FontWeight.Bold, 1.1f), modifier = Modifier.padding(end = 8.dp))
            }
        }
    }
}

// ── 패널 ───────────────────────────────────────────────────────────

/** 카드 — 묶음 면(다크 #0E1424 · 라이트 흰색), 모서리 16, 테두리 없음. [selected] 면 2dp 블루 테두리 */
@Composable
fun RunCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    tag: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val t = runTone()
    val shape = RoundedCornerShape(RunSpec.CardRadius)
    Column(
        modifier.fillMaxWidth().clip(shape)
            .background(t.panel, shape)
            .then(if (selected) Modifier.border(2.dp, t.cobalt, shape) else Modifier)
            .then(if (onClick != null) Modifier.feedbackClickable(onClick = onClick) else Modifier)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .padding(padding),
        content = content,
    )
}


/**
 * 러닝 방법 카드(시안 U01 · U02) — 같은 크기의 가로 줄. 왼쪽 이름 · 작은 설명, 오른쪽 그림 · ">".
 * 카드 전체가 하나의 버튼이다(카드 안에 버튼을 또 두지 않는다). 번지는 빛 · 테두리 없이 묶음 면만.
 */
@Composable
fun RunModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    badge: String? = null,
    titleStyle: TextStyle? = null,
    minHeight: Dp = 76.dp,
    /** 아직 열지 않은 방법(2026-10-27) — 흐리게 · 오른쪽 자물쇠. 누르면 [onClick](곧 열린다는 안내) */
    locked: Boolean = false,
) {
    val t = runTone()
    val shape = RoundedCornerShape(RunSpec.CardRadius)
    Row(
        modifier.fillMaxWidth().heightIn(min = minHeight)
            .clip(shape)
            .background(t.panel, shape)
            .feedbackClickable(enabled = enabled || locked, onClick = onClick)
            .padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (enabled) t.cobaltText else t.muted, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    title,
                    style = titleStyle ?: runTextStyle(18.sp, if (enabled) t.text else t.muted, FontWeight.ExtraBold, 1.25f),
                    maxLines = 2,
                )
                if (badge != null) {
                    Text(badge, style = runTextStyle(12.sp, t.label, FontWeight.SemiBold))
                }
            }
            Text(subtitle, style = runTextStyle(13.5.sp, if (enabled) t.label else t.muted, FontWeight.Normal))
        }
        Icon(
            if (locked) Icons.Filled.Lock else Icons.Filled.ChevronRight, contentDescription = null, tint = t.muted,
            modifier = Modifier.size(if (locked) 20.dp else 24.dp),
        )
        if (locked) Spacer(Modifier.width(4.dp))
    }
}

/** 한 줄 이동 칸 — "내 러닝 기록 >" · "지난 도전 보기 >"(묶음 면 한 줄) */
@Composable
fun RunLinkRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    /** 아직 열지 않은 곳(2026-10-27) — 흐린 이름 · 오른쪽 자물쇠 */
    locked: Boolean = false,
) {
    val t = runTone()
    val shape = RoundedCornerShape(RunSpec.CardRadius)
    Row(
        modifier.fillMaxWidth().heightIn(min = 54.dp).clip(shape).background(t.panel, shape)
            .feedbackClickable(onClick = onClick)
            .padding(start = 16.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = runTextStyle(16.sp, if (locked) t.muted else t.text, FontWeight.Bold), modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, style = runTextStyle(13.5.sp, t.label, FontWeight.Normal))
            Spacer(Modifier.width(4.dp))
        }
        Icon(
            if (locked) Icons.Filled.Lock else Icons.Filled.ChevronRight, contentDescription = null, tint = t.muted,
            modifier = Modifier.size(if (locked) 18.dp else 22.dp),
        )
    }
}

// ── 지도 틀 ─────────────────────────────────────────────────────────

/**
 * 지도 틀 — 모서리 22, 테두리 없음, 오른쪽 위 확대 원 버튼. 안은 실제 지도([LiveRouteMap])나 안내를 넣는다.
 * 지도 출처 표기는 지도 부품이 왼쪽 아래에 두므로 가리지 않는다.
 */
@Composable
fun RunMapFrame(
    modifier: Modifier = Modifier,
    onExpand: (() -> Unit)? = null,
    expandLabel: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val t = runTone()
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier.fillMaxWidth().clip(shape).background(StepUpUi.colors.mapBackground, shape),
    ) {
        androidx.compose.runtime.CompositionLocalProvider(LocalMapTone provides MapTone(t.mapFilter, t.mapShade, t.mapRoads)) {
            content()
        }
        if (onExpand != null) {
            val label = expandLabel ?: stringResource(R.string.run_map_expand)
            Box(
                Modifier.align(Alignment.TopEnd).padding(8.dp).size(40.dp).clip(CircleShape)
                    .background(t.panel.copy(alpha = 0.9f))
                    .feedbackClickable(onClick = onExpand)
                    .semantics { contentDescription = label }
                    .testTag("run-map-expand"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.OpenInFull, contentDescription = null, tint = t.text, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** 지도 자리에 두는 안내 — 위치 없이 기록 · 경로 없음. 가짜 지도를 그리지 않는다 */
@Composable
fun RunMapPlaceholder(
    icon: ImageVector,
    title: String,
    body: String?,
    modifier: Modifier = Modifier,
) {
    val t = runTone()
    Column(
        modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        RunStateArt(icon, size = 72.dp)
        Spacer(Modifier.height(14.dp))
        Text(title, style = runTextStyle(17.sp, t.text, FontWeight.Bold), textAlign = TextAlign.Center)
        if (body != null) {
            Spacer(Modifier.height(4.dp))
            Text(body, style = runTextStyle(14.sp, t.label, FontWeight.Medium), textAlign = TextAlign.Center)
        }
    }
}

// ── 상태 그림 · 빈 화면 ─────────────────────────────────────────────

/** 빈 화면 · 실패 · 권한 안내의 그림 — 아이콘 하나(빛 · 타일 없이). [alert] 면 빨강 */
@Composable
fun RunStateArt(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 112.dp, alert: Boolean = false, badge: ImageVector? = null) {
    val t = runTone()
    Box(modifier.size(size * 0.6f), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = if (alert) t.dangerText else t.cobaltText, modifier = Modifier.size(size * 0.46f))
        if (badge != null) {
            Box(
                Modifier.align(Alignment.BottomEnd).size(size * 0.24f).clip(CircleShape).background(t.dangerFace),
                contentAlignment = Alignment.Center,
            ) {
                Icon(badge, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.15f))
            }
        }
    }
}

/** 빈 · 실패 화면의 가운데 — (작은 그림) · 큰 한 줄 · 설명 */
@Composable
fun RunEmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    alert: Boolean = false,
    badge: ImageVector? = null,
    artSize: Dp = 132.dp,
) {
    val t = runTone()
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        RunStateArt(icon, size = artSize * 0.7f, alert = alert, badge = badge)
        Spacer(Modifier.height(16.dp))
        Text(
            title, style = TextStyle(
                fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 1.3.em,
                letterSpacing = (-0.04).em, color = t.text,
            ),
            textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() },
        )
        if (body != null) {
            Spacer(Modifier.height(8.dp))
            Text(body, style = runTextStyle(15.sp, t.label, FontWeight.Normal, 1.5f), textAlign = TextAlign.Center)
        }
    }
}

/** 도는 표시 — 블루 호 하나(0.7초 한 바퀴). 동작 줄이기에서는 멈춘 호 */
@Composable
fun RunSpinner(modifier: Modifier = Modifier, color: Color? = null, track: Color? = null) {
    val t = runTone()
    val motion = LocalMotion.current
    val spin = rememberInfiniteTransition(label = "runSpinner")
    val angle by spin.animateFloat(
        0f, 360f, infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Restart), label = "runSpinnerAngle",
    )
    Canvas(modifier.semantics { contentDescription = "…" }) {
        val stroke = size.minDimension * 0.11f
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val ink = color ?: t.cobalt
        drawArc(track ?: ink.copy(alpha = 0.22f), 0f, 360f, false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke))
        drawArc(
            ink, if (motion.reduced) -90f else angle - 90f, 100f, false,
            topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
        )
    }
}

// ── 안내 띠 ─────────────────────────────────────────────────────────

enum class RunNoticeKind { Info, Warn, Error }

/** 안내 띠 — 아이콘 · 제목 · 설명 · (오른쪽 작은 행동). 정보 · 확인 필요는 묶음 면, 오류는 옅은 빨강 면. 테두리 없음 */
@Composable
fun RunNotice(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    kind: RunNoticeKind = RunNoticeKind.Info,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    tag: String? = null,
    /** 정보 띠의 그림을 바꾼다(크루원 계속 달리는 중 — 사람들) */
    icon: ImageVector? = null,
) {
    val t = runTone()
    val shape = RoundedCornerShape(RunSpec.CardRadius)
    val (face, iconTint) = when (kind) {
        RunNoticeKind.Error -> t.errorFace to t.errorIcon
        RunNoticeKind.Warn -> t.warnFace to t.warnIcon
        RunNoticeKind.Info -> t.panel to t.cobaltText
    }
    Row(
        modifier.fillMaxWidth().clip(shape).background(face, shape)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon ?: if (kind == RunNoticeKind.Info) Icons.Outlined.Info else Icons.Outlined.ErrorOutline, null,
            tint = iconTint, modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = runTextStyle(15.sp, if (kind == RunNoticeKind.Error) t.errorIcon else t.text, FontWeight.Bold))
            if (body != null) {
                Text(body, style = runTextStyle(13.sp, if (kind == RunNoticeKind.Error) t.errorIcon.copy(alpha = 0.85f) else t.label, FontWeight.Normal))
            }
        }
        if (action != null && onAction != null) {
            Spacer(Modifier.width(8.dp))
            SmallButton(action, onAction, Modifier.then(if (tag != null) Modifier.testTag("$tag-action") else Modifier))
        }
    }
}

/** 작은 표시 — "확인 중"(시계) · "예상" · "반영 완료". 통합안은 알약 칩을 쓰지 않는다 — 아이콘 + 회색 글자 한 줄 */
@Composable
fun RunChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    outlined: Boolean = false,
    accent: Color? = null,
) {
    val t = runTone()
    val ink = accent ?: t.label
    Row(
        modifier.heightIn(min = 24.dp).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = ink, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = runTextStyle(13.sp, ink, FontWeight.SemiBold), maxLines = 1)
    }
}

// ── 시트 · 창 ───────────────────────────────────────────────────────

/**
 * 아래에서 올라오는 시트 — 남색 면 · 손잡이 · (오른쪽 위 닫기). 뒤 화면은 어둡게 덮여 누를 수 없다.
 * 바깥을 누르거나 뒤로 가면 [onDismiss](취소)만 한다 — 위험한 행동은 버튼으로만.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    showClose: Boolean = true,
    dismissible: Boolean = true,
    closeTag: String = "run-sheet-close",
    content: @Composable ColumnScope.() -> Unit,
) {
    val t = runTone()
    // confirmValueChange 는 시트 상태(rememberSaveable)의 키다 — 닫을 수 있음(dismissible)이 바뀔 때마다 새 람다를 넘기면
    // 새 시트 상태가 만들어져 시트가 다시 올라온다(지우는 중 · 보내는 중). 한 번 만든 람다가 지금 값을 읽게 한다
    val canDismiss by androidx.compose.runtime.rememberUpdatedState(dismissible)
    val confirm = remember { { value: androidx.compose.material3.SheetValue -> canDismiss || value != androidx.compose.material3.SheetValue.Hidden } }
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = confirm)
    ModalBottomSheet(
        onDismissRequest = { if (dismissible) onDismiss() },
        sheetState = state,
        containerColor = Color.Transparent,
        scrimColor = StepUpUi.colors.sheetScrim,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = RunSpec.SheetRadius, topEnd = RunSpec.SheetRadius),
        properties = androidx.compose.material3.ModalBottomSheetProperties(shouldDismissOnBackPress = dismissible),
    ) {
        RunSheetPanel(
            onClose = if (showClose && dismissible) onDismiss else null,
            modifier = modifier,
            closeTag = closeTag,
            content = content,
        )
    }
}

/** 시트의 면 — 위 모서리 28 + 손잡이 36×5. [RunSheet] 안과 시안 검사(창 없이 그리기)가 함께 쓴다 */
@Composable
fun RunSheetPanel(
    onClose: (() -> Unit)?,
    modifier: Modifier = Modifier,
    closeTag: String = "run-sheet-close",
    content: @Composable ColumnScope.() -> Unit,
) {
    val t = runTone()
    val shape = RoundedCornerShape(topStart = RunSpec.SheetRadius, topEnd = RunSpec.SheetRadius)
    Box(modifier.fillMaxWidth().clip(shape).background(t.sheet, shape)) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().imePadding()
                .padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(width = 36.dp, height = 5.dp).clip(RoundedCornerShape(3.dp)).background(t.handle))
            Spacer(Modifier.height(18.dp))
            content()
        }
        if (onClose != null) {
            val label = stringResource(R.string.common_close)
            Box(
                Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 10.dp).size(48.dp).clip(CircleShape)
                    .feedbackClickable(cue = FeedbackCue.Back, onClick = onClose)
                    .semantics { contentDescription = label }
                    .testTag(closeTag),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(30.dp).clip(CircleShape).background(StepUpUi.colors.fill), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Close, contentDescription = null, tint = t.label, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/** 시트 · 창의 글 — 가운데 큰 제목(22 · 800) · 본문 · 보조 한 줄 */
@Composable
fun RunSheetText(title: String, modifier: Modifier = Modifier, body: String? = null, note: String? = null) {
    val t = runTone()
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            title, style = TextStyle(
                fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 1.3.em,
                letterSpacing = (-0.04).em, color = t.text,
            ),
            textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() }.padding(horizontal = 28.dp),
        )
        if (body != null) {
            Spacer(Modifier.height(8.dp))
            Text(body, style = runTextStyle(15.sp, t.text, FontWeight.Normal, 1.45f), textAlign = TextAlign.Center)
        }
        if (note != null) {
            Spacer(Modifier.height(4.dp))
            Text(note, style = runTextStyle(13.5.sp, t.label, FontWeight.Normal, 1.45f), textAlign = TextAlign.Center)
        }
    }
}

/**
 * 가운데 창 — 남색 면 카드(권한 안내 P01 · 목표 달성 C01 같은 짧은 확인). 바깥 · 뒤로는 [onDismiss](취소)만.
 */
@Composable
fun RunDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false, dismissOnBackPress = dismissible, dismissOnClickOutside = dismissible,
        ),
    ) {
        RunDialogPanel(modifier, content)
    }
}

/** 가운데 창의 면 — 묶음 면 · 모서리 24 · 테두리 없음. [RunDialog] 안과 시안 검사가 함께 쓴다 */
@Composable
fun RunDialogPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val t = runTone()
    val shape = RoundedCornerShape(24.dp)
    Box(Modifier.fillMaxWidth().padding(horizontal = 28.dp), contentAlignment = Alignment.Center) {
        Column(
            modifier.widthIn(max = 460.dp).fillMaxWidth().clip(shape)
                .background(t.sheet, shape)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

// ── 목록 줄 ─────────────────────────────────────────────────────────

/** 고르는 줄 — 하나만 고른다. 고르면 2dp 블루 테두리 + 블루 원 체크(시안 U05 · CR19 · H03) */
@Composable
fun RunChoiceRow(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    selected: Boolean,
    onSelect: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
    compact: Boolean = false,
) {
    val t = runTone()
    val shape = RoundedCornerShape(RunSpec.CardRadius)
    Row(
        modifier.fillMaxWidth().heightIn(min = if (compact) 52.dp else 68.dp).clip(shape)
            .background(t.panel, shape)
            .border(2.dp, if (selected) t.cobalt else Color.Transparent, shape)
            .feedbackClickable(role = Role.RadioButton, cue = FeedbackCue.Select, onClick = onSelect)
            .semantics { this.selected = selected }
            .padding(horizontal = 16.dp, vertical = if (compact) 8.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = runTextStyle(if (compact) 16.sp else 16.5.sp, t.text, FontWeight.Bold))
            if (description != null) {
                Text(description, style = runTextStyle(13.sp, t.label, FontWeight.Normal))
            }
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier.size(24.dp).clip(CircleShape)
                .then(
                    if (selected) Modifier.background(t.cobalt)
                    else Modifier.border(2.dp, t.muted.copy(alpha = 0.7f), CircleShape),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

/** 이름 머리글자 원 — 크루원. 통합안은 이니셜 아바타를 줄인다 — 회색 원 · 회색 글자로 눈에 덜 띄게 */
@Composable
fun RunInitials(text: String, modifier: Modifier = Modifier, size: Dp = 40.dp, filled: Boolean = false) {
    val t = runTone()
    Box(
        modifier.size(size).clip(CircleShape).background(if (filled) t.cobalt else StepUpUi.colors.fill),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = runTextStyle(if (text.length > 1) 13.sp else 15.sp, if (filled) Color.White else t.label, FontWeight.Bold, 1.1f))
    }
}

/** 칸 사이 기본 간격 */
val RunGap: Dp = RunSpec.Gap

/** 세로로 남는 칸을 채운다 */
@Composable
fun ColumnScope.RunFill() = Spacer(Modifier.weight(1f))

/** 가로 둘 — 같은 폭 두 버튼 */
@Composable
fun RunPair(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

/** 가는 가로선 */
@Composable
fun RunDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(runTone().divider))
}

/** 세로선 — 수치 사이 */
@Composable
fun RunVerticalDivider(modifier: Modifier = Modifier) {
    Box(modifier.width(1.dp).fillMaxHeight().background(runTone().divider))
}

// ── 글자 탭 · 입력칸 ────────────────────────────────────────────────

/** 켜고 끄기 — 켜면 블루 길, 끄면 회색 길(위치 공유 · 경로 포함) */
@Composable
fun RunSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val t = runTone()
    androidx.compose.material3.Switch(
        checked = checked, onCheckedChange = onCheckedChange, enabled = enabled, modifier = modifier,
        colors = androidx.compose.material3.SwitchDefaults.colors(
            checkedThumbColor = Color.White, checkedTrackColor = t.cobalt, checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color.White, uncheckedTrackColor = t.track, uncheckedBorderColor = Color.Transparent,
            disabledUncheckedTrackColor = t.track.copy(alpha = 0.5f), disabledUncheckedBorderColor = Color.Transparent,
        ),
    )
}

/** 같은 폭 전환(코스 선택 · 코스 만들기 · 코스 게시판) — 통합안 세그먼트([StepUpTabs]) */
@Composable
fun RunTabs(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tagPrefix: String = "run-tab",
    /** 칸마다 꼬리표를 따로 줄 때(통계의 주간 · 월간) — 없으면 "[tagPrefix]-번호" */
    tags: List<String>? = null,
) {
    StepUpTabs(
        labels, selected, onSelect, modifier.fillMaxWidth(),
        tags = labels.indices.map { tags?.getOrNull(it) ?: "$tagPrefix-$it" },
    )
}

/**
 * 한 줄 입력칸 — 통합안 입력칸(모서리 14 · 묶음 면, 고르면 2dp 블루 테두리). [label] 은 칸 안 위에 작게.
 * [leading] 이 있으면 앞에 아이콘(검색), 글이 있으면 오른쪽 지우기.
 */
@Composable
fun RunTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leading: ImageVector? = null,
    imeAction: androidx.compose.ui.text.input.ImeAction = androidx.compose.ui.text.input.ImeAction.Done,
    onImeAction: () -> Unit = {},
    clearLabel: String? = null,
    fieldTag: String? = null,
) {
    val t = runTone()
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(StepUpUi.FieldRadius)
    Row(
        modifier.fillMaxWidth().heightIn(min = 52.dp).clip(shape).background(t.panel, shape)
            .border(2.dp, if (focused) t.cobalt else Color.Transparent, shape)
            .padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Icon(leading, contentDescription = null, tint = t.label, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f).padding(vertical = if (label != null) 10.dp else 0.dp)) {
            if (label != null) {
                Text(label, style = runTextStyle(12.5.sp, if (focused) t.cobaltText else t.label, FontWeight.Normal, 1.2f))
                Spacer(Modifier.height(2.dp))
            }
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty() && placeholder != null) {
                    Text(placeholder, style = runTextStyle(16.sp, t.muted, FontWeight.Normal), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = runTextStyle(16.sp, t.text, FontWeight.Normal),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(t.cobalt),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = imeAction),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onAny = { onImeAction() }),
                    modifier = Modifier.fillMaxWidth().padding(vertical = if (label != null) 2.dp else 14.dp)
                        .onFocusChanged { focused = it.isFocused }
                        .semantics { if (label != null) contentDescription = label else if (placeholder != null) contentDescription = placeholder }
                        .then(if (fieldTag != null) Modifier.testTag(fieldTag) else Modifier),
                )
            }
        }
        if (value.isNotEmpty() && clearLabel != null) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).feedbackClickable { onValueChange("") }
                    .semantics { contentDescription = clearLabel },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(20.dp).clip(CircleShape).background(t.muted), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Close, contentDescription = null, tint = t.panel, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

/**
 * [RunNumber] 의 줄 맞춤 — 줄을 한 번만 재고, 폭이 모자랄 때만 그 줄을 그대로 줄여 그린다.
 * 고유 크기(IntrinsicSize 줄 · 나란한 칸 높이 맞추기)도 같은 규칙으로 답한다 — 기본 layout { } 은 폭을 무한으로 잰
 * 자식의 고유 높이를 제대로 넘기지 못해 IntrinsicSize.Min 줄 안에서 높이 0 이 되어 숫자가 잘렸다(내 정보 러닝 패스).
 */
private class FitScale(private val horizontal: Alignment.Horizontal) : androidx.compose.ui.layout.LayoutModifier {
    override fun androidx.compose.ui.layout.MeasureScope.measure(
        measurable: androidx.compose.ui.layout.Measurable,
        constraints: Constraints,
    ): androidx.compose.ui.layout.MeasureResult {
        val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity, minHeight = 0))
        val max = constraints.maxWidth
        val scale = if (constraints.hasBoundedWidth && placeable.width > max && placeable.width > 0) {
            max.toFloat() / placeable.width
        } else 1f
        val shownW = (placeable.width * scale).roundToInt()
        val shownH = (placeable.height * scale).roundToInt()
        val width = shownW.coerceIn(constraints.minWidth, if (constraints.hasBoundedWidth) max else Int.MAX_VALUE)
        val height = shownH.coerceIn(constraints.minHeight, constraints.maxHeight)
        val x = horizontal.align(shownW, width, layoutDirection)
        val y = (height - shownH) / 2
        return layout(width, height) {
            if (scale == 1f) placeable.place(x, y)
            else placeable.placeWithLayer(x, y) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }

    /** 줄 하나의 자연 폭 — 줄여도 되므로 최소 · 최대 모두 이 값 */
    private fun natural(measurable: androidx.compose.ui.layout.IntrinsicMeasurable) = measurable.maxIntrinsicWidth(Constraints.Infinity)

    /** 폭 [width] 에 맞춰 줄였을 때의 높이 */
    private fun heightAt(measurable: androidx.compose.ui.layout.IntrinsicMeasurable, width: Int): Int {
        val w = natural(measurable)
        val h = measurable.maxIntrinsicHeight(w.coerceAtLeast(0))
        return if (width in 1 until w && width != Constraints.Infinity) (h.toLong() * width / w).toInt() else h
    }

    override fun androidx.compose.ui.layout.IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: androidx.compose.ui.layout.IntrinsicMeasurable, height: Int,
    ) = natural(measurable)

    override fun androidx.compose.ui.layout.IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: androidx.compose.ui.layout.IntrinsicMeasurable, height: Int,
    ) = natural(measurable)

    override fun androidx.compose.ui.layout.IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: androidx.compose.ui.layout.IntrinsicMeasurable, width: Int,
    ) = heightAt(measurable, width)

    override fun androidx.compose.ui.layout.IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: androidx.compose.ui.layout.IntrinsicMeasurable, width: Int,
    ) = heightAt(measurable, width)

    override fun equals(other: Any?) = other is FitScale && other.horizontal == horizontal
    override fun hashCode() = horizontal.hashCode()
}
