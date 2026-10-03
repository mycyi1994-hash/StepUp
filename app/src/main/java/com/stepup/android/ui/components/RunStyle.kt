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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.ColorMatrix
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
import androidx.compose.ui.layout.layout
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
     * 남색 시안: 땅은 짙은 남색, 물은 파랑, 지명은 밝은 글자.
     */
    val mapFilter: ColorFilter? = null,
    /** 타일 위에 덮는 바탕색의 짙기 — 남색 지도는 이미 어두워 덮지 않는다 */
    val mapShade: Float = 0.30f,
)

/**
 * 밝은 길거리 타일(땅 #F1EEDC · 물 #85CAF9 · 길 흰색 · 글자 짙은 회색)을 시안 지도 색(땅 #08223E · 물 #03316F ·
 * 글자 #92B0D6)으로 옮기는 4x5 행렬 — 실제 타일 색과 시안 색을 짝지어 맞춘 값이다.
 */
private val NavyMapFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            0.1069f, -0.5333f, -0.4397f, 0f, 206.3f,
            -0.4935f, 0.4639f, -0.8512f, 0f, 233.1f,
            -1.0416f, 1.0823f, -0.9839f, 0f, 276.0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
)

val NavyRunTone = RunTone(
    dark = true,
    screenTop = Color(0xFF06204A), screen = Color(0xFF031427), screenBottom = Color(0xFF021023),
    glow = Color(0xFF0B3A86),
    panelTop = Color(0xFF0D2E57), panel = Color(0xFF0A2547), panelEdge = Color(0xFF1C4E95),
    inset = Color(0xFF06203F),
    text = Color(0xFFF5F8FF), label = Color(0xFFAAC3EA), muted = Color(0xFF7F99C2),
    cobalt = Color(0xFF0754FF), cobaltText = Color(0xFF5B95FF), cyan = Color(0xFF48D9FA),
    primaryFace = listOf(Color(0xFFFFFFFF), Color(0xFFDCEBFD)), primaryInk = Color(0xFF071B3D),
    primaryBase = Color(0xFF0754FF),
    secondaryFace = Color(0xFF061D3B), secondaryEdge = Color(0xFF1F63E6), secondaryInk = Color(0xFFF5F8FF),
    dangerFace = Color(0xFFE23B4B), dangerBase = Color(0xFF9E1C2B), dangerInk = Color.White,
    dangerText = Color(0xFFFF5C6B),
    disabledFace = Color(0xFF3A5070), disabledInk = Color(0xFFA9B8CF), disabledBase = Color(0xFF233752),
    divider = Color(0xFF1E3A63), track = Color(0xFF15305A),
    barFill = listOf(Color(0xFF0754FF), Color(0xFF48D9FA)),
    sheetTop = Color(0xFF0C2A50), sheet = Color(0xFF071C38), sheetEdge = Color(0xFF1C4E95),
    handle = Color(0xFF3A5A8A), scrim = Color(0xA8010A16),
    chipFace = Color(0xFF16305A), chipEdge = Color(0xFF2C4C7E), chipInk = Color(0xFFD6E2F8),
    warnFace = Color(0xFF0C2B55), warnEdge = Color(0xFF2B6BE0), warnIcon = Color(0xFF48D9FA),
    errorFace = Color(0xFF3A1323), errorEdge = Color(0xFFB83246), errorIcon = Color(0xFFFF5C6B),
    mapFilter = NavyMapFilter, mapShade = 0f,
)

val LightRunTone = RunTone(
    dark = false,
    screenTop = Color(0xFFEAF1FF), screen = Color(0xFFF5F8FF), screenBottom = Color(0xFFFFFFFF),
    glow = Color(0xFFD3E2FF),
    panelTop = Color(0xFFFFFFFF), panel = Color(0xFFF7FAFF), panelEdge = Color(0xFFC9D8F2),
    inset = Color(0xFFEDF2FC),
    text = Color(0xFF0B1E3F), label = Color(0xFF4A5F84), muted = Color(0xFF6A7C9C),
    cobalt = Color(0xFF0754FF), cobaltText = Color(0xFF0748D6), cyan = Color(0xFF0A8FB8),
    primaryFace = listOf(Color(0xFF1C63FF), Color(0xFF0754FF)), primaryInk = Color.White,
    primaryBase = Color(0xFF0335B0),
    secondaryFace = Color.White, secondaryEdge = Color(0xFF6E9BF5), secondaryInk = Color(0xFF0B1E3F),
    dangerFace = Color(0xFFD9364A), dangerBase = Color(0xFF8E1424), dangerInk = Color.White,
    dangerText = Color(0xFFC62337),
    disabledFace = Color(0xFFC9D3E3), disabledInk = Color(0xFF6E7D96), disabledBase = Color(0xFFAAB7CB),
    divider = Color(0xFFDCE4F2), track = Color(0xFFDCE5F5),
    barFill = listOf(Color(0xFF0754FF), Color(0xFF2BB8E0)),
    sheetTop = Color(0xFFFFFFFF), sheet = Color(0xFFF5F8FF), sheetEdge = Color(0xFFC9D8F2),
    handle = Color(0xFFB5C3DB), scrim = Color(0xB3091A33),
    chipFace = Color(0xFFE6EEFC), chipEdge = Color(0xFFC3D3EE), chipInk = Color(0xFF2A3E62),
    warnFace = Color(0xFFE8F1FF), warnEdge = Color(0xFF8DB2F5), warnIcon = Color(0xFF0748D6),
    errorFace = Color(0xFFFFECEE), errorEdge = Color(0xFFE59AA4), errorIcon = Color(0xFFC62337),
)

@Composable
fun runTone(): RunTone = if (StepUpColors.dark) NavyRunTone else LightRunTone

object RunSpec {
    val Gutter = 20.dp
    val PrimaryHeight = 56.dp
    val PrimaryBase = 6.dp
    val SecondaryHeight = 50.dp
    val HeroHeight = 72.dp
    val HeroBase = 8.dp
    val CardRadius = 16.dp
    val SheetRadius = 26.dp
    val Gap = 12.dp
}

// ── 글자 ───────────────────────────────────────────────────────────

/** 큰 운동 숫자 — 굵게 기울인다(본문에는 쓰지 않는다) */
fun runNumberStyle(size: TextUnit, color: Color, weight: FontWeight = FontWeight.ExtraBold): TextStyle = TextStyle(
    fontFamily = StepUpSans, fontWeight = weight, fontStyle = FontStyle.Italic, fontSize = size, color = color,
    letterSpacing = (-0.02).em, fontFeatureSettings = "tnum", lineHeight = 1.08.em,
)

fun runTextStyle(size: TextUnit, color: Color, weight: FontWeight = FontWeight.Normal, height: Float = 1.4f): TextStyle =
    TextStyle(
        fontFamily = StepUpSans, fontWeight = weight, fontSize = size, color = color,
        letterSpacing = (-0.01).em, lineHeight = height.em,
    )

// ── 바닥 ───────────────────────────────────────────────────────────

/** 러닝 화면 바닥 — 남색, 위가 조금 밝고 오른쪽 위에서 파란 빛이 번진다 */
@Composable
fun RunBackdrop(modifier: Modifier = Modifier) {
    val t = runTone()
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(0f to t.screenTop, 0.32f to t.screen, 1f to t.screenBottom))
        drawRect(
            Brush.radialGradient(
                0f to t.glow.copy(alpha = if (t.dark) 0.55f else 0.6f),
                1f to Color.Transparent,
                center = Offset(size.width * 0.9f, size.height * 0.02f),
                radius = size.width * 0.95f,
            ),
        )
    }
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
 * 러닝 버튼 — 주(흰 면 · 남색 글자 · 파란 아랫면), 보조(남색 면 · 파란 테두리), 위험(빨간 면).
 * 모서리를 깎은 모양이고 눌리면 아랫면 쪽으로 짧게 내려앉는다. [busy] 면 글자 앞에 도는 표시를 둔다.
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
    /** 오른쪽 끝 ">"(다음 화면으로 넘어가는 버튼 — "내 러닝 방법 보기 >") */
    chevron: Boolean = false,
    /** 기울인 굵은 글자(화면의 큰 행동 — 일시정지 · 처음 화면으로 · 공유하기). [hero] 는 늘 기울인다 */
    italic: Boolean = hero,
) {
    val t = runTone()
    val feedback = LocalFeedback.current
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val faceHeight = when {
        hero -> RunSpec.HeroHeight
        kind == RunButtonKind.Secondary -> RunSpec.SecondaryHeight
        else -> RunSpec.PrimaryHeight
    }
    val base = when {
        kind == RunButtonKind.Secondary -> 0.dp
        hero -> RunSpec.HeroBase
        else -> RunSpec.PrimaryBase
    }
    val cut = faceHeight * 0.3f
    val active = enabled && !busy
    val faceColors: List<Color>
    val ink: Color
    val baseColor: Color
    when {
        !enabled || busy -> {
            faceColors = listOf(t.disabledFace, t.disabledFace); ink = t.disabledInk; baseColor = t.disabledBase
        }
        kind == RunButtonKind.Primary -> {
            faceColors = t.primaryFace; ink = t.primaryInk; baseColor = t.primaryBase
        }
        kind == RunButtonKind.Danger -> {
            faceColors = listOf(t.dangerFace, t.dangerFace); ink = t.dangerInk; baseColor = t.dangerBase
        }
        else -> {
            faceColors = listOf(t.secondaryFace, t.secondaryFace); ink = t.secondaryInk; baseColor = Color.Transparent
        }
    }
    val sink = if (pressed && active && base > 0.dp) base * 0.6f else 0.dp
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = faceHeight + base)
            .clickable(interactionSource = interactions, indication = null, enabled = active, role = Role.Button) {
                feedback?.play(cue)
                onClick()
            }
            .semantics { if (busy) contentDescription = label },
    ) {
        Canvas(Modifier.matchParentSize()) {
            val faceH = size.height - base.toPx()
            val c = cut.toPx()
            if (base > 0.dp) {
                drawPath(chamferPath(size, c, top = base.toPx(), height = faceH), baseColor)
            }
            val top = sink.toPx()
            val face = chamferPath(size, c, top = top, height = faceH)
            drawPath(face, Brush.verticalGradient(faceColors, startY = top, endY = top + faceH))
            if (kind == RunButtonKind.Secondary && enabled) {
                drawPath(face, t.secondaryEdge, style = Stroke(width = 1.5.dp.toPx()))
            }
        }
        Row(
            Modifier.fillMaxWidth().height(faceHeight).offset(y = sink)
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (busy) {
                RunSpinner(Modifier.size(22.dp), color = ink, track = ink.copy(alpha = 0.25f))
                Spacer(Modifier.width(12.dp))
            } else if (icon != null) {
                Icon(
                    icon, contentDescription = null,
                    tint = if (kind == RunButtonKind.Primary && enabled && t.dark) t.cobalt else ink,
                    modifier = Modifier.size(if (hero) 34.dp else 24.dp),
                )
                Spacer(Modifier.width(if (hero) 16.dp else 10.dp))
            }
            val strong = kind != RunButtonKind.Secondary
            Text(
                label,
                style = if (strong) {
                    TextStyle(
                        fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold,
                        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
                        fontSize = when {
                            hero -> 30.sp
                            italic -> 25.sp
                            else -> 19.sp
                        },
                        letterSpacing = (-0.02).em, color = ink,
                    )
                } else {
                    runTextStyle(18.sp, ink, FontWeight.SemiBold)
                },
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            )
        }
        if (chevron) {
            Box(
                Modifier.align(Alignment.TopEnd).height(faceHeight).offset(y = sink).padding(end = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = ink, modifier = Modifier.size(26.dp))
            }
        }
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
            else -> t.label
        }
        Text(label, style = runTextStyle(16.sp, color, FontWeight.SemiBold), textAlign = TextAlign.Center)
        if (chevron) Icon(Icons.Filled.ChevronRight, null, tint = color, modifier = Modifier.size(20.dp))
    }
}

/** 작은 테두리 버튼 — 아이콘 + 글자(공유하기 · 내 러닝 기록 보기). 둘씩 나란히 둔다 */
@Composable
fun RunTileButton(
    label: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val t = runTone()
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.heightIn(min = 52.dp).clip(shape).background(t.secondaryFace, shape)
            .border(1.5.dp, if (enabled) t.secondaryEdge else t.divider, shape)
            .feedbackClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val ink = if (enabled) t.text else t.muted
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (enabled) t.label else t.muted, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(label, style = runTextStyle(15.sp, ink, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ── 머리 · 제목 ─────────────────────────────────────────────────────

/**
 * 러닝 화면 머리 — 왼쪽 뒤로(가는 화살표) · 공식 로고, 오른쪽은 그 화면의 행동 하나(삭제 · SUP 잔액 등)만.
 * [crumb] 가 있으면(크루 화면) 왼쪽에 작은 이름, 로고는 오른쪽에 작게.
 */
@Composable
fun RunTopBar(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    crumb: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    /** 뒤로 버튼 꼬리표 — 권한 안내처럼 닫기와 같은 뜻이면 그 이름으로 */
    backTag: String = "kit-back",
) {
    val t = runTone()
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 4.dp, end = RunSpec.Gutter - 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                Modifier.size(48.dp).clip(CircleShape)
                    .feedbackClickable(cue = FeedbackCue.Back, onClick = onBack).testTag(backTag),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBackIos, stringResource(R.string.cd_back), tint = t.text,
                    modifier = Modifier.size(20.dp).offset(x = 3.dp),
                )
            }
        } else {
            Spacer(Modifier.width(RunSpec.Gutter - 4.dp))
        }
        if (crumb != null) {
            Text(
                crumb, style = runTextStyle(16.sp, t.label, FontWeight.Medium), maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).testTag("run-crumb"),
            )
            Wordmark(BrandLogoRole.Card)
        } else {
            Wordmark(BrandLogoRole.Header, Modifier.weight(1f, fill = false))
            Spacer(Modifier.weight(1f))
            trailing?.invoke()
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

/** 화면 제목 — 굵은 제목(메뉴 화면은 [display] 로 기울인 큰 제목) · 한 줄 설명 */
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
        val style = if (display) {
            TextStyle(
                fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic,
                fontSize = 42.sp, lineHeight = 1.15.em, letterSpacing = (-0.03).em, color = t.text,
            )
        } else {
            TextStyle(
                fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp,
                lineHeight = 1.24.em, letterSpacing = (-0.03).em, color = t.text,
            )
        }
        Box {
            Text(
                title, style = style, textAlign = align,
                modifier = Modifier.semantics { heading() }.then(if (titleTag != null) Modifier.testTag(titleTag) else Modifier),
            )
            if (display) {
                // 시안의 큰 제목은 가장 굵은 글꼴보다 굵다 — 같은 색 테두리를 얇게 한 겹 더 그린다(읽히는 글자는 위의 것 하나)
                Text(
                    title, textAlign = align,
                    style = style.copy(drawStyle = Stroke(width = with(LocalDensity.current) { 1.4.dp.toPx() }, join = StrokeJoin.Round)),
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
        }
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = runTextStyle(16.sp, t.label, FontWeight.Medium), textAlign = align)
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
                    Modifier.fillMaxWidth().padding(horizontal = RunSpec.Gutter).padding(top = 8.dp, bottom = 14.dp).imePadding(),
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

/** 상태 알약 — 기록 중(파란 면 · 시안 점) · 일시정지 · 저장 완료(시안 테두리 · 체크) · 저장 전 · 확인 중 */
@Composable
fun RunStatusPill(text: String, kind: RunPillKind, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val t = runTone()
    val shape = RoundedCornerShape(50)
    val (face, edge, ink) = when (kind) {
        RunPillKind.Live -> Triple(t.cobalt, t.cobalt.copy(alpha = 0.0f), Color.White)
        RunPillKind.Paused -> Triple(t.secondaryFace, t.secondaryEdge, t.text)
        RunPillKind.Saved -> Triple(if (t.dark) Color(0xFF07254A) else Color(0xFFE6F6FD), t.cyan, if (t.dark) t.text else t.text)
        RunPillKind.Pending -> Triple(t.secondaryFace, t.chipEdge, t.text)
        RunPillKind.Neutral -> Triple(t.chipFace, t.chipEdge, t.chipInk)
        RunPillKind.Alert -> Triple(t.errorFace, t.errorEdge, t.text)
    }
    Row(
        modifier.heightIn(min = 32.dp).clip(shape)
            .background(
                if (kind == RunPillKind.Live) Brush.horizontalGradient(listOf(Color(0xFF0754FF), Color(0xFF2A73FF)))
                else Brush.horizontalGradient(listOf(face, face)),
                shape,
            )
            .border(1.dp, edge, shape)
            .padding(horizontal = 14.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            icon != null -> Icon(icon, null, tint = if (kind == RunPillKind.Saved) t.cyan else ink, modifier = Modifier.size(18.dp))
            kind == RunPillKind.Live -> Box(
                Modifier.size(12.dp).drawBehind {
                    drawCircle(t.cyan.copy(alpha = 0.35f), radius = size.minDimension / 2f)
                    drawCircle(t.cyan, radius = size.minDimension / 3.2f)
                },
            )
            kind == RunPillKind.Paused -> Icon(Icons.Filled.Pause, null, tint = ink, modifier = Modifier.size(18.dp))
            kind == RunPillKind.Saved -> Icon(Icons.Filled.CheckCircle, null, tint = t.cyan, modifier = Modifier.size(18.dp))
            kind == RunPillKind.Pending -> Box(Modifier.size(10.dp).clip(CircleShape).background(t.cobaltText))
            else -> Unit
        }
        if (kind != RunPillKind.Neutral || icon != null) Spacer(Modifier.width(8.dp))
        Text(text, style = runTextStyle(15.sp, ink, FontWeight.SemiBold), maxLines = 1)
    }
}

// ── 숫자 ───────────────────────────────────────────────────────────

/**
 * 큰 수 + 작은 단위 한 줄 — 폭이 모자라면(큰 글씨 · 좁은 화면) 둘을 같은 비율로 줄인다. 두 줄로 넘기지 않는다.
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
    val unitStyle = if (italicUnit) runNumberStyle(unitSize, unitColor, FontWeight.Bold)
    else runTextStyle(unitSize, unitColor, FontWeight.SemiBold, 1.1f)
    // 폭이 모자라면 한 번 잰 줄을 그대로 줄여 그린다 — 예전처럼 BoxWithConstraints(그리는 중에 한 번 더 짜기)와 글자 따로 재기를
    // 하지 않는다. 이 수는 한 화면에 여러 개(목록 줄마다)라 그 비용이 쌓이면 넘김이 버벅인다.
    val horizontal = align
    Row(
        modifier.layout { measurable, constraints ->
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
            layout(width, height) {
                if (scale == 1f) placeable.place(x, y)
                else placeable.placeWithLayer(x, y) {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0f)
                }
            }
        },
        verticalAlignment = Alignment.Bottom,
    ) {
        // 시안의 큰 운동 숫자는 Pretendard 가장 굵은 굵기보다 더 굵다 — 같은 색 테두리를 한 겹 더 그려 굵힌다(큰 수만).
        // 위가 밝고 아래가 살짝 푸른 빛(남색 테마)
        val heavy = size >= 28.sp
        val shine = heavy && runTone().dark && color == runTone().text
        Box(Modifier.alignByBaseline()) {
            // 읽히는 글자(꼬리표 · 화면 낭독)가 먼저, 굵히는 테두리는 같은 색이라 위에 겹쳐도 같다
            Text(
                value,
                style = if (shine) valueStyle.copy(brush = NumberShine) else valueStyle,
                maxLines = 1, softWrap = false,
                modifier = if (valueTag != null) Modifier.testTag(valueTag) else Modifier,
            )
            if (heavy) {
                Text(
                    value,
                    style = valueStyle.copy(
                        drawStyle = Stroke(width = with(LocalDensity.current) { (size.value * 0.035f).dp.toPx() }, join = StrokeJoin.Round),
                        brush = if (shine) NumberShine else null,
                    ),
                    maxLines = 1, softWrap = false, modifier = Modifier.clearAndSetSemantics { },
                )
            }
        }
        if (unit != null) {
            Text(" $unit", style = unitStyle, maxLines = 1, softWrap = false, modifier = Modifier.alignByBaseline())
        }
    }
}

/** 큰 수의 빛 — 위가 밝고 아래가 살짝 푸르다 */
private val NumberShine = Brush.verticalGradient(listOf(Color.White, Color(0xFFC7D7F2)))

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
 * 수치 묶음 — 같은 폭 칸들을 한 줄로, 칸 사이에 가는 세로선. [framed] 면 패널 안에 둔다.
 */
@Composable
fun RunStatRow(
    stats: List<RunStat>,
    modifier: Modifier = Modifier,
    framed: Boolean = false,
    valueSize: TextUnit = 40.sp,
    dim: Boolean = false,
) {
    val t = runTone()
    val gap = 20.dp
    val inner = @Composable {
        Row(
            Modifier.fillMaxWidth().drawBehind {
                val g = gap.toPx()
                val columns = stats.size
                val column = (size.width - g * (columns - 1)) / columns
                for (i in 1 until columns) {
                    val x = column * i + g * (i - 1) + g / 2f
                    drawLine(t.divider, Offset(x, 6.dp.toPx()), Offset(x, size.height - 6.dp.toPx()), strokeWidth = 1.dp.toPx())
                }
            },
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.Top,
        ) {
            stats.forEach { stat ->
                // 칸은 한 덩어리로 읽힌다(이름 · 값 · 단위). 꼬리표는 값 글자에 — 검사는 펼친 트리에서 값만 읽는다
                Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                    Text(stat.label, style = runTextStyle(14.sp, t.label, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    RunNumber(
                        stat.value, unit = stat.unit, size = valueSize,
                        color = when {
                            dim -> t.muted
                            stat.accent -> t.cyan
                            else -> t.text
                        },
                        modifier = Modifier.fillMaxWidth(), valueTag = stat.tag,
                    )
                    if (stat.caption != null) {
                        Text(stat.caption, style = runTextStyle(13.sp, t.muted, FontWeight.Medium), maxLines = 1)
                    }
                }
            }
        }
    }
    if (framed) {
        RunCard(modifier, padding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)) { inner() }
    } else {
        Box(modifier) { inner() }
    }
}

// ── 막대 ───────────────────────────────────────────────────────────

/** 진행 막대 — 어두운 바닥에 파랑 → 시안 채움. [inside] 가 있으면 채운 끝에 작은 글자 */
@Composable
fun RunMeter(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
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
                        Brush.horizontalGradient(colors, endX = w.coerceAtLeast(1f)), size = Size(w, size.height), cornerRadius = r,
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

/** 패널 — 남색 면, 파란 둘레, 모서리 16dp. [selected] 면 파란 둘레가 굵어진다 */
@Composable
fun RunCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    tag: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val t = runTone()
    val shape = RoundedCornerShape(RunSpec.CardRadius)
    Column(
        modifier.fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(t.panelTop, t.panel)), shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) t.cobalt.copy(alpha = 0.95f) else t.panelEdge, shape)
            .then(if (onClick != null) Modifier.feedbackClickable(onClick = onClick) else Modifier)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** 메뉴 카드 둘레 빛 */
private val CardGlow = Color(0xFF1E64FF)

/**
 * 러닝 방법 카드(시안 U01 · U02) — 같은 크기의 가로 버튼. 왼쪽 큰 이름 · 작은 설명, 오른쪽 그림 · 화살표.
 * 카드 전체가 하나의 버튼이다(카드 안에 버튼을 또 두지 않는다).
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
    minHeight: Dp = 88.dp,
) {
    val t = runTone()
    val shape = RoundedCornerShape(RunSpec.CardRadius)
    val edge = if (t.dark) Color(0xFF2B6DFF) else t.secondaryEdge
    Row(
        modifier.fillMaxWidth().heightIn(min = minHeight)
            .drawBehind {
                // 바깥 빛 — 파란 둘레가 은은하게 번진다. 흐림 필터(BlurMaskFilter)는 다시 그릴 때마다 CPU 로 흐림을 계산해
                // 메뉴 카드가 여럿이면 화면 넘김이 버벅인다 — 넓고 옅은 테 두 겹으로 같은 느낌만 낸다
                if (t.dark && enabled) {
                    val r = CornerRadius(RunSpec.CardRadius.toPx())
                    drawRoundRect(CardGlow.copy(alpha = 0.14f), cornerRadius = r, style = Stroke(10.dp.toPx()))
                    drawRoundRect(CardGlow.copy(alpha = 0.24f), cornerRadius = r, style = Stroke(5.dp.toPx()))
                }
            }
            .clip(shape)
            .background(Brush.verticalGradient(listOf(t.panelTop, t.panel)), shape)
            .border(1.5.dp, if (enabled) edge else t.divider, shape)
            .feedbackClickable(enabled = enabled, onClick = onClick)
            .padding(start = 20.dp, end = 10.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    title,
                    style = titleStyle ?: runTextStyle(27.sp, if (enabled) t.text else t.muted, FontWeight.ExtraBold, 1.2f),
                    maxLines = 2,
                )
                if (badge != null) {
                    Text(
                        badge, style = runTextStyle(12.sp, t.chipInk, FontWeight.SemiBold),
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(t.chipFace)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Text(subtitle, style = runTextStyle(15.sp, if (enabled) t.label else t.muted, FontWeight.Medium))
        }
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (enabled) Color(0xFFCFE0FF).takeIf { t.dark } ?: t.cobalt else t.muted,
                modifier = Modifier.size(48.dp))
            Spacer(Modifier.width(12.dp))
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = if (enabled) t.label else t.muted, modifier = Modifier.size(30.dp))
    }
}

/** 한 줄 이동 칸 — "내 러닝 기록 >" · "지난 도전 보기 >" */
@Composable
fun RunLinkRow(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, trailing: String? = null) {
    val t = runTone()
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape).background(t.secondaryFace, shape)
            .border(1.dp, t.secondaryEdge.copy(alpha = 0.75f), shape)
            .feedbackClickable(onClick = onClick)
            .padding(start = 20.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = runTextStyle(17.sp, t.text, FontWeight.SemiBold), modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, style = runTextStyle(14.sp, t.label, FontWeight.Medium))
            Spacer(Modifier.width(4.dp))
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = t.label, modifier = Modifier.size(24.dp))
    }
}

// ── 지도 틀 ─────────────────────────────────────────────────────────

/**
 * 지도 틀 — 모서리 16dp · 파란 둘레, 오른쪽 위 확대 버튼. 안은 실제 지도([LiveRouteMap])나 안내를 넣는다.
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
    val shape = RoundedCornerShape(RunSpec.CardRadius)
    Box(
        modifier.fillMaxWidth().clip(shape).background(t.inset, shape).border(1.dp, t.panelEdge, shape),
    ) {
        androidx.compose.runtime.CompositionLocalProvider(LocalMapTone provides MapTone(t.mapFilter, t.mapShade)) {
            content()
        }
        if (onExpand != null) {
            val label = expandLabel ?: stringResource(R.string.run_map_expand)
            Box(
                Modifier.align(Alignment.TopEnd).padding(8.dp).size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(t.screen.copy(alpha = 0.78f)).border(1.dp, t.panelEdge, RoundedCornerShape(12.dp))
                    .feedbackClickable(onClick = onExpand)
                    .semantics { contentDescription = label }
                    .testTag("run-map-expand"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.OpenInFull, contentDescription = null, tint = t.text, modifier = Modifier.size(20.dp))
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

/** 빛나는 원 안의 아이콘 — 빈 화면 · 실패 · 권한 안내 */
@Composable
fun RunStateArt(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 112.dp, alert: Boolean = false, badge: ImageVector? = null) {
    val t = runTone()
    Box(
        modifier.size(size).drawBehind {
            drawCircle(
                Brush.radialGradient(
                    0f to t.cobalt.copy(alpha = if (t.dark) 0.35f else 0.18f), 1f to Color.Transparent,
                    center = center, radius = this.size.minDimension * 0.62f,
                ),
                radius = this.size.minDimension * 0.62f,
            )
            drawCircle(if (t.dark) Color(0xFF0B2C57) else Color(0xFFE5EEFF), radius = this.size.minDimension / 2.25f)
            drawCircle(
                (if (alert) t.dangerText else t.cobalt).copy(alpha = 0.85f), radius = this.size.minDimension / 2.25f,
                style = Stroke(width = 2.dp.toPx()),
            )
        },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (t.dark) Color(0xFFBFD6FF) else t.cobalt, modifier = Modifier.size(size * 0.46f))
        if (badge != null) {
            Box(
                Modifier.align(Alignment.BottomEnd).offset(x = (-size * 0.06f), y = (-size * 0.06f)).size(size * 0.32f)
                    .clip(CircleShape).background(t.dangerFace),
                contentAlignment = Alignment.Center,
            ) {
                Icon(badge, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.2f))
            }
        }
    }
}

/** 빈 · 실패 화면의 가운데 — 그림 · 큰 한 줄 · 설명 */
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
        RunStateArt(icon, size = artSize, alert = alert, badge = badge)
        Spacer(Modifier.height(24.dp))
        Text(
            title, style = TextStyle(
                fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp, lineHeight = 1.3.em,
                letterSpacing = (-0.02).em, color = t.text,
            ),
            textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() },
        )
        if (body != null) {
            Spacer(Modifier.height(10.dp))
            Text(body, style = runTextStyle(16.sp, t.label, FontWeight.Medium, 1.5f), textAlign = TextAlign.Center)
        }
    }
}

/** 도는 표시 — 파랑에서 시안으로 이어지는 호. 동작 줄이기에서는 멈춘 호 */
@Composable
fun RunSpinner(modifier: Modifier = Modifier, color: Color? = null, track: Color? = null) {
    val t = runTone()
    val motion = LocalMotion.current
    val spin = rememberInfiniteTransition(label = "runSpinner")
    val angle by spin.animateFloat(
        0f, 360f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "runSpinnerAngle",
    )
    Canvas(modifier.semantics { contentDescription = "…" }) {
        val stroke = size.minDimension * 0.09f
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(track ?: t.track, 0f, 360f, false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke))
        drawArc(
            if (color != null) Brush.linearGradient(listOf(color, color)) else Brush.sweepGradient(listOf(t.cobalt, t.cyan, t.cobalt)),
            if (motion.reduced) -90f else angle - 90f, 110f, false,
            topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
        )
    }
}

// ── 안내 띠 ─────────────────────────────────────────────────────────

enum class RunNoticeKind { Info, Warn, Error }

/** 안내 띠 — 아이콘 · 제목 · 설명 · (오른쪽 작은 행동). 확인 필요(시안 느낌표) · 오류(빨강) */
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
    val shape = RoundedCornerShape(14.dp)
    val (face, edge, iconTint) = when (kind) {
        RunNoticeKind.Error -> Triple(t.errorFace, t.errorEdge, t.errorIcon)
        RunNoticeKind.Warn -> Triple(t.warnFace, t.warnEdge, t.warnIcon)
        RunNoticeKind.Info -> Triple(t.panel, t.panelEdge, t.cyan)
    }
    Row(
        modifier.fillMaxWidth().clip(shape).background(face, shape).border(1.dp, edge, shape)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(30.dp).clip(CircleShape)
                .background(if (kind == RunNoticeKind.Info) iconTint.copy(alpha = 0.16f) else iconTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon ?: if (kind == RunNoticeKind.Info) Icons.Outlined.Info else Icons.Outlined.ErrorOutline, null,
                tint = if (kind == RunNoticeKind.Info) iconTint else if (t.dark) t.screen else Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = runTextStyle(15.sp, t.text, FontWeight.Bold))
            if (body != null) Text(body, style = runTextStyle(13.sp, t.label, FontWeight.Medium))
        }
        if (action != null && onAction != null) {
            Spacer(Modifier.width(8.dp))
            val chip = RoundedCornerShape(10.dp)
            Text(
                action, style = runTextStyle(13.sp, t.text, FontWeight.SemiBold),
                modifier = Modifier.clip(chip).background(t.secondaryFace).border(1.dp, t.secondaryEdge, chip)
                    .feedbackClickable(onClick = onAction).padding(horizontal = 12.dp, vertical = 8.dp)
                    .then(if (tag != null) Modifier.testTag("$tag-action") else Modifier),
            )
        }
    }
}

/** 작은 알약 — "확인 중"(시계) · "예상" · "반영 완료" */
@Composable
fun RunChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    outlined: Boolean = false,
    accent: Color? = null,
) {
    val t = runTone()
    val shape = RoundedCornerShape(50)
    val ink = accent ?: t.chipInk
    Row(
        modifier.heightIn(min = 28.dp).clip(shape)
            .background(if (outlined) Color.Transparent else t.chipFace, shape)
            .border(1.dp, if (outlined) (accent ?: t.chipEdge) else t.chipEdge, shape)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = ink, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
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
        scrimColor = t.scrim,
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

/** 시트의 면 — [RunSheet] 안과 시안 검사(창 없이 그리기)가 함께 쓴다 */
@Composable
fun RunSheetPanel(
    onClose: (() -> Unit)?,
    modifier: Modifier = Modifier,
    closeTag: String = "run-sheet-close",
    content: @Composable ColumnScope.() -> Unit,
) {
    val t = runTone()
    val shape = RoundedCornerShape(topStart = RunSpec.SheetRadius, topEnd = RunSpec.SheetRadius)
    Box(
        modifier.fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(t.sheetTop, t.sheet)), shape)
            .border(1.dp, t.sheetEdge, shape),
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().imePadding()
                .padding(horizontal = RunSpec.Gutter).padding(top = 10.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(width = 44.dp, height = 5.dp).clip(RoundedCornerShape(3.dp)).background(t.handle))
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
                Icon(Icons.Filled.Close, contentDescription = null, tint = t.text, modifier = Modifier.size(26.dp))
            }
        }
    }
}

/** 시트 · 창의 글 — 가운데 큰 제목 · 본문 · 보조 한 줄 */
@Composable
fun RunSheetText(title: String, modifier: Modifier = Modifier, body: String? = null, note: String? = null) {
    val t = runTone()
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            title, style = TextStyle(
                fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp, lineHeight = 1.3.em,
                letterSpacing = (-0.02).em, color = t.text,
            ),
            textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() }.padding(horizontal = 28.dp),
        )
        if (body != null) {
            Spacer(Modifier.height(10.dp))
            Text(body, style = runTextStyle(16.sp, t.text, FontWeight.Medium, 1.45f), textAlign = TextAlign.Center)
        }
        if (note != null) {
            Spacer(Modifier.height(4.dp))
            Text(note, style = runTextStyle(14.sp, t.label, FontWeight.Medium, 1.45f), textAlign = TextAlign.Center)
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

/** 가운데 창의 면 — [RunDialog] 안과 시안 검사가 함께 쓴다 */
@Composable
fun RunDialogPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val t = runTone()
    val shape = RoundedCornerShape(24.dp)
    Box(Modifier.fillMaxWidth().padding(horizontal = 28.dp), contentAlignment = Alignment.Center) {
        Column(
            modifier.widthIn(max = 460.dp).fillMaxWidth().clip(shape)
                .background(Brush.verticalGradient(listOf(t.sheetTop, t.sheet)), shape)
                .border(1.5.dp, t.secondaryEdge.copy(alpha = 0.8f), shape)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

// ── 목록 줄 ─────────────────────────────────────────────────────────

/** 고르는 줄 — 하나만 고른다. 고르면 파란 면 · 흰 체크(시안 U05 · CR19 · H03) */
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
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = if (compact) 52.dp else 72.dp).clip(shape)
            .background(
                if (selected) Brush.horizontalGradient(listOf(Color(0xFF0754FF), Color(0xFF1F66FF)))
                else Brush.verticalGradient(listOf(t.panelTop, t.panel)),
                shape,
            )
            .border(1.dp, if (selected) Color(0xFF4D8BFF) else t.panelEdge, shape)
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
            Text(title, style = runTextStyle(if (compact) 16.sp else 18.sp, if (selected) Color.White else t.text, FontWeight.Bold))
            if (description != null) {
                Text(description, style = runTextStyle(13.sp, if (selected) Color(0xFFD9E6FF) else t.label, FontWeight.Medium))
            }
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier.size(26.dp).clip(CircleShape)
                .then(
                    if (selected) Modifier.background(Color.White)
                    else Modifier.border(2.dp, t.label.copy(alpha = 0.8f), CircleShape),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(Icons.Filled.Check, null, tint = Color(0xFF0754FF), modifier = Modifier.size(18.dp))
        }
    }
}

/** 이름 머리글자 원 — 크루원(이니셜) */
@Composable
fun RunInitials(text: String, modifier: Modifier = Modifier, size: Dp = 40.dp, filled: Boolean = false) {
    val t = runTone()
    Box(
        modifier.size(size).clip(CircleShape)
            .background(if (filled) t.cobalt else t.inset)
            .border(1.5.dp, if (filled) t.cobalt else t.cyan.copy(alpha = 0.8f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = runTextStyle(if (text.length > 1) 13.sp else 15.sp, if (filled) Color.White else t.cyan, FontWeight.Bold, 1.1f))
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

/** 켜고 끄기 — 켜면 파란 길, 끄면 남색 길(위치 공유 · 경로 포함) */
@Composable
fun RunSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val t = runTone()
    androidx.compose.material3.Switch(
        checked = checked, onCheckedChange = onCheckedChange, enabled = enabled, modifier = modifier,
        colors = androidx.compose.material3.SwitchDefaults.colors(
            checkedThumbColor = Color.White, checkedTrackColor = t.cobalt,
            uncheckedThumbColor = Color.White, uncheckedTrackColor = t.track, uncheckedBorderColor = t.panelEdge,
            disabledUncheckedTrackColor = t.track.copy(alpha = 0.5f), disabledUncheckedBorderColor = t.divider,
        ),
    )
}

/** 같은 폭 글자 탭(코스 선택 · 코스 만들기 · 코스 게시판) — 고른 칸은 파란 면 · 흰 글자 */
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
    val t = runTone()
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp).clip(shape).background(t.inset, shape).border(1.dp, t.panelEdge, shape)
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEachIndexed { i, label ->
            val on = i == selected
            val cell = RoundedCornerShape(11.dp)
            Box(
                Modifier.weight(1f).heightIn(min = 42.dp).clip(cell)
                    .then(
                        if (on) Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF0754FF), Color(0xFF2A73FF))), cell)
                            .border(1.dp, Color(0xFF4D8BFF), cell)
                        else Modifier,
                    )
                    .feedbackClickable(role = Role.Tab, cue = FeedbackCue.Select) { onSelect(i) }
                    .semantics { this.selected = on }
                    .testTag(tags?.getOrNull(i) ?: "$tagPrefix-$i"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label, style = runTextStyle(15.sp, if (on) Color.White else t.label, if (on) FontWeight.Bold else FontWeight.SemiBold),
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
        }
    }
}

/**
 * 한 줄 입력칸 — 위에 작은 이름, 남색 칸, 오른쪽 지우기(글이 있을 때). [leading] 이 있으면 앞에 아이콘(검색).
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
    val shape = RoundedCornerShape(12.dp)
    Column(modifier.fillMaxWidth()) {
        if (label != null) {
            Text(label, style = runTextStyle(14.sp, t.label, FontWeight.SemiBold), modifier = Modifier.padding(bottom = 6.dp))
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(shape).background(t.inset, shape)
                .border(if (focused) 1.5.dp else 1.dp, if (focused) Color(0xFF4D8BFF) else t.panelEdge, shape)
                .padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                Icon(leading, contentDescription = null, tint = t.label, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty() && placeholder != null) {
                    Text(placeholder, style = runTextStyle(16.sp, t.muted, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = runTextStyle(16.sp, t.text, FontWeight.SemiBold),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(t.cyan),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = imeAction),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onAny = { onImeAction() }),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)
                        .onFocusChanged { focused = it.isFocused }
                        .semantics { if (label != null) contentDescription = label else if (placeholder != null) contentDescription = placeholder }
                        .then(if (fieldTag != null) Modifier.testTag(fieldTag) else Modifier),
                )
            }
            if (value.isNotEmpty() && clearLabel != null) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).feedbackClickable { onValueChange("") }
                        .semantics { contentDescription = clearLabel },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(20.dp).clip(CircleShape).background(t.label), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Close, contentDescription = null, tint = t.inset, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
