package com.stepup.android.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.ShoeTier
import kotlin.math.roundToInt

/*
 * 신발 이름 끝의 작은 둥근 등급 배지(2026-09-28 신발 화면 확정안 — design/shoes-ui-handoff-2026-09-28/badges).
 *
 * 전달받은 배지 PNG(2172 × 724, 한국어 글자가 들어간 원본)는 디자인 기준이다. 3:1 캔버스를 짧은 글자에 억지로 쓰면 이름 자리를
 * 너무 먹고, 언어를 바꿀 수 없다. 그래서 캡슐 · 이중 테두리 · 갈래마다 다른 장식(속도선 · 프리즘 · 금속광 · 열기 파동 · 체커)을
 * 그리고, 글자는 기기 글꼴로 얹는다 — 글자 확대 · 다국어가 그대로 된다. 누르는 곳이 아니다(표시일 뿐).
 */

/** 배지 · 막대 · 선택 테두리가 함께 쓰는 갈래 색 */
@Immutable
class TierColors(
    /** 테두리 금속 — 가로로 흐르는 광 */
    val rim: List<Color>,
    /** 캡슐 안 — 위에서 아래로 */
    val fill: List<Color>,
    val label: Color,
    /** 바깥 번짐(일반은 없음) */
    val glow: Color?,
    /** 장식 선 */
    val motif: Color,
    /** 막대 · 선택 표시에 쓰는 대표색 */
    val accent: Color,
)

fun tierColors(tier: ShoeTier): TierColors = when (tier) {
    // CARBON — 브러시드 실버, 번짐 없음
    ShoeTier.COMMON -> TierColors(
        rim = listOf(Color(0xFF6B7A90), Color(0xFFD9E1EE), Color(0xFF8C9AB0), Color(0xFFD9E1EE), Color(0xFF6B7A90)),
        fill = listOf(Color(0xFF1B2230), Color(0xFF10151F)),
        label = Color(0xFFDCE3EE), glow = null, motif = Color(0xFFA9B6CA), accent = Color(0xFF91A1B8),
    )
    // VELOCITY — 블루 · 아이스
    ShoeTier.RARE -> TierColors(
        rim = listOf(Color(0xFF2F7FE0), Color(0xFFA8DCFF), Color(0xFF4A9FF0), Color(0xFFA8DCFF), Color(0xFF2F7FE0)),
        fill = listOf(Color(0xFF0A1C44), Color(0xFF041030)),
        label = Color(0xFFABDCFF), glow = Color(0xFF4A9FF0), motif = Color(0xFF7CC4FF), accent = Color(0xFF4A9FF0),
    )
    // PRISM — 퍼플 · 라벤더
    ShoeTier.EPIC -> TierColors(
        rim = listOf(Color(0xFF7A4FD0), Color(0xFFE6D2FF), Color(0xFFA778E6), Color(0xFFE6D2FF), Color(0xFF7A4FD0)),
        fill = listOf(Color(0xFF251849), Color(0xFF150D33)),
        label = Color(0xFFE8DAFF), glow = Color(0xFFA778E6), motif = Color(0xFFCDB2FF), accent = Color(0xFFA778E6),
    )
    // PODIUM — 골드 · 샴페인
    ShoeTier.LEGENDARY -> TierColors(
        rim = listOf(Color(0xFFB8862E), Color(0xFFFFE6A8), Color(0xFFE2AE52), Color(0xFFFFE6A8), Color(0xFFB8862E)),
        fill = listOf(Color(0xFF231B12), Color(0xFF16110C)),
        label = Color(0xFFF4D48F), glow = Color(0xFFE2AE52), motif = Color(0xFFFFE0A0), accent = Color(0xFFE2AE52),
    )
    // REDLINE — 코럴 → 앰버 열기
    ShoeTier.REDLINE -> TierColors(
        rim = listOf(Color(0xFFE8432E), Color(0xFFFFC267), Color(0xFFFF654D), Color(0xFFFFC267), Color(0xFFE8432E)),
        fill = listOf(Color(0xFF300E13), Color(0xFF1C0709)),
        label = Color(0xFFFFD2C4), glow = Color(0xFFFF654D), motif = Color(0xFFFFB067), accent = Color(0xFFFF6F55),
    )
    // FINISH — 민트 · 보라 · 아이스화이트
    ShoeTier.FINISH -> TierColors(
        rim = listOf(Color(0xFF5DE9DE), Color(0xFFA99BFF), Color(0xFFF1FBFF), Color(0xFF8EF1E8), Color(0xFF5DE9DE)),
        fill = listOf(Color(0xFF08323B), Color(0xFF041C24)),
        label = Color(0xFFD0FCF7), glow = Color(0xFF5DE9DE), motif = Color(0xFFB9A8FF), accent = Color(0xFF5DE9DE),
    )
}

@Composable
fun ShoeTier.label(): String = stringResource(
    when (this) {
        ShoeTier.COMMON -> R.string.shoe_tier_common
        ShoeTier.RARE -> R.string.shoe_tier_rare
        ShoeTier.EPIC -> R.string.shoe_tier_epic
        ShoeTier.LEGENDARY -> R.string.shoe_tier_legendary
        ShoeTier.REDLINE -> R.string.shoe_tier_redline
        ShoeTier.FINISH -> R.string.shoe_tier_finish
    },
)

/**
 * 배지 크기 — 보이는 높이 24dp(글자가 커지면 글자 + 위아래 3dp), 글자 11.5sp. 양옆 16dp 는 끝 둥근 곳의 장식이 글자와 2dp 넘게
 * 떨어져 들어갈 자리다 — 11dp 였을 때는 광점 · 파동 · 체커가 글자 끝을 덮어 "레전더리"가 "레전더라"처럼 보였다(2026-09-28 기기 캡처).
 * 장식은 높이에 비례해 커지므로 양옆도 높이에 비례해 넓힌다(큰 글씨에서도 겹치지 않게).
 */
private val BadgeHeight = 24.dp
private val BadgeSidePadding = 16.dp
private val BadgeLabelInset = 3.dp
private val BadgeLabelSize = 11.5.sp

/** 글자 높이 [labelHeight] 에 맞는 배지 높이(px) */
private fun Density.badgeHeightPx(labelHeight: Int): Int =
    maxOf(BadgeHeight.roundToPx(), labelHeight + BadgeLabelInset.roundToPx() * 2)

/** 배지 높이 [height] 에 맞는 양옆(px) — 24dp 일 때 16dp */
private fun Density.badgeSidePx(height: Int): Int = (BadgeSidePadding.toPx() * height / BadgeHeight.toPx()).roundToInt()

/** 이름과 배지 사이 */
private val BadgeGap = 6.dp

private fun badgeLabelStyle(colors: TierColors) =
    TextStyle(color = colors.label, fontSize = BadgeLabelSize, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)

/**
 * 둥근 등급 배지 하나 — 이름 옆 표시. 눌러도 아무 일이 없다.
 * [decorative] 면 읽기 도구가 읽지 않는다 — 이름 글에 등급 이름이 이미 들어 있을 때(두 번 읽지 않게).
 */
@Composable
fun ShoeGradeBadge(tier: ShoeTier, modifier: Modifier = Modifier, decorative: Boolean = false) {
    val colors = remember(tier) { tierColors(tier) }
    val tag = "tier-badge-${tier.key}"
    Box(
        modifier
            .drawBehind { drawTierBadge(tier, colors) }
            .then(if (decorative) Modifier.clearAndSetSemantics { testTag = tag } else Modifier.testTag(tag))
            .badgeLayout(),
        contentAlignment = Alignment.Center,
    ) {
        Text(tier.label(), style = LocalTextStyle.current.merge(badgeLabelStyle(colors)), maxLines = 1, softWrap = false)
    }
}

/**
 * 배지 칸 — 글자를 재서 높이([badgeHeightPx]) · 양옆([badgeSidePx])을 정하고 글자를 가운데에 둔다.
 * 부르는 쪽이 크기를 정해 두면(이름 끝 자리 — 같은 계산으로 잰 크기) 그 안 가운데에 둔다.
 */
private fun Modifier.badgeLayout(): Modifier = layout { measurable, constraints ->
    val label = measurable.measure(Constraints())
    val height = if (constraints.hasFixedHeight) constraints.maxHeight else badgeHeightPx(label.height)
    val width = if (constraints.hasFixedWidth) constraints.maxWidth else label.width + badgeSidePx(height) * 2
    layout(width, height) { label.place((width - label.width) / 2, (height - label.height) / 2) }
}

/**
 * 이름 + 끝의 배지 — 배지는 이름의 **마지막 줄 끝**에 같은 줄로 붙는다(따로 한 줄을 차지하지 않는다).
 * 이름은 [maxLines] 줄까지. 넘치면 이름을 줄여 "…"를 붙이고 배지는 그대로 둔다 — 배지가 잘려 사라지지 않게.
 */
@Composable
fun ShoeNameWithBadge(
    name: String,
    tier: ShoeTier,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = 2,
    textAlign: TextAlign = TextAlign.Start,
) {
    val density = LocalDensity.current
    val colors = remember(tier) { tierColors(tier) }
    val label = tier.label()
    val measurer = rememberTextMeasurer()
    // 재는 글과 그리는 글이 같은 글꼴이어야 배지 자리가 맞는다 — 테마 글꼴(LocalTextStyle)을 함께 쓴다
    val base = LocalTextStyle.current
    val nameStyle = remember(base, style, textAlign) { base.merge(style).copy(textAlign = textAlign) }
    val labelStyle = remember(base, colors) { base.merge(badgeLabelStyle(colors)) }
    BoxWithConstraints(modifier) {
        val labelSize = remember(label, labelStyle, density) { measurer.measure(label, labelStyle).size }
        // 배지 칸(badgeLayout)과 같은 계산 — 글자가 커지면 높이 · 양옆이 함께 커진다
        val (badgeWidth, badgeHeight) = remember(labelSize, density) {
            with(density) {
                val height = badgeHeightPx(labelSize.height)
                (labelSize.width + badgeSidePx(height) * 2).toDp() to height.toDp()
            }
        }
        val placeholder = with(density) {
            Placeholder(
                width = (badgeWidth + BadgeGap).toSp(), height = badgeHeight.toSp(),
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
            )
        }
        val widthPx = constraints.maxWidth
        val text = remember(name, label, nameStyle, widthPx, placeholder, maxLines) {
            fittedName(name, label, widthPx) { candidate ->
                measurer.measure(candidate, nameStyle, maxLines = maxLines, constraints = Constraints(maxWidth = widthPx),
                    placeholders = listOf(AnnotatedString.Range(placeholder, candidate.length - badgeAltLength(label), candidate.length)))
                    .hasVisualOverflow
            }
        }
        Text(
            text = text,
            style = nameStyle,
            maxLines = maxLines,
            overflow = TextOverflow.Clip,
            inlineContent = mapOf(
                BadgeInlineId to InlineTextContent(placeholder) {
                    Box(Modifier.padding(start = BadgeGap), contentAlignment = Alignment.CenterStart) {
                        // 글의 대체 글(" 레어")이 등급을 이미 읽어 준다
                        ShoeGradeBadge(tier, Modifier.size(width = badgeWidth, height = badgeHeight), decorative = true)
                    }
                },
            ),
        )
    }
}

private const val BadgeInlineId = "tier-badge"

/** 배지 자리의 대체 글(" 레어") 길이 — 글 안에서 배지 자리가 차지하는 글자 수 */
private fun badgeAltLength(label: String) = label.length + 1

/**
 * 이름 뒤에 배지 자리를 붙인 글. [overflows] 가 참이면(줄 수를 넘으면) 이름을 한 글자씩 줄여 "…"를 붙인다 — 배지는 늘 남는다.
 * 폭을 모르면(0) 그대로 둔다.
 */
private fun fittedName(
    name: String,
    label: String,
    widthPx: Int,
    overflows: (AnnotatedString) -> Boolean,
): AnnotatedString {
    fun build(body: String) = buildAnnotatedString {
        append(body)
        appendInlineContent(BadgeInlineId, " $label")
    }
    val full = build(name)
    if (widthPx <= 0 || widthPx == Constraints.Infinity || !overflows(full)) return full
    var lo = 0
    var hi = name.length
    while (lo < hi) {
        val mid = (lo + hi + 1) / 2
        if (overflows(build(name.take(mid).trimEnd() + "…"))) hi = mid - 1 else lo = mid
    }
    return build(name.take(lo).trimEnd() + "…")
}

// ── 그리기 ──────────────────────────────────────────────────────

private fun DrawScope.capsule(inset: Float) = RoundRect(
    Rect(inset, inset, size.width - inset, size.height - inset),
    CornerRadius((size.height - inset * 2) / 2),
)

private fun Path.addCapsule(rect: RoundRect) = apply { addRoundRect(rect) }

/**
 * 바깥 번짐 → 캡슐 안 → 금속 테두리 → 안쪽 가는 선 → 갈래 장식. 장식은 양 끝 둥근 곳(끝에서 4 ~ 14u, 글자는 16u 부터)에만 둔다.
 * 단위 u 는 높이 24dp 기준 1dp — 글자가 커져 배지가 높아지면 장식도 같이 커진다.
 */
private fun DrawScope.drawTierBadge(tier: ShoeTier, c: TierColors) {
    val u = size.height / 24f
    val outer = capsule(0.9f * u)
    colorsGlow(c, outer, u)
    drawPath(Path().addCapsule(outer), Brush.verticalGradient(c.fill))
    drawPath(Path().addCapsule(outer), Brush.horizontalGradient(c.rim), style = Stroke(width = 1.35f * u))
    drawPath(Path().addCapsule(capsule(2.9f * u)), c.rim[1].copy(alpha = 0.35f), style = Stroke(width = 0.6f * u))
    val h = size.height
    val w = size.width
    val m = h / 2
    when (tier) {
        ShoeTier.COMMON -> {
            // 짧은 각인 두 줄(=) — 양 끝, 가운데 높이
            listOf(-1.5f to 5.4f, 1.5f to 6.2f).forEach { (dy, from) ->
                val y = m + dy * u
                drawLine(c.motif.copy(alpha = 0.8f), Offset(from * u, y), Offset(10.2f * u, y), strokeWidth = 1f * u, cap = StrokeCap.Round)
                drawLine(c.motif.copy(alpha = 0.8f), Offset(w - from * u, y), Offset(w - 10.2f * u, y), strokeWidth = 1f * u, cap = StrokeCap.Round)
            }
        }
        ShoeTier.RARE -> {
            // 왼쪽 비스듬한 속도선 둘(//) · 오른쪽 끝으로 갈수록 짧아지는 가속선 셋
            for (k in 0..1) {
                val dx = k * 2.7f * u
                drawLine(c.motif.copy(alpha = 0.95f - k * 0.3f), Offset(5.2f * u + dx, m + 4.2f * u), Offset(10.2f * u + dx, m - 3.8f * u),
                    strokeWidth = 1.15f * u, cap = StrokeCap.Round)
            }
            listOf(-2.9f to 5.6f, 0f to 4.4f, 2.9f to 3.0f).forEach { (dy, len) ->
                val y = m + dy * u
                drawLine(c.motif.copy(alpha = 0.8f), Offset(w - (5.4f + len) * u, y), Offset(w - 5.4f * u, y),
                    strokeWidth = 1f * u, cap = StrokeCap.Round)
            }
        }
        ShoeTier.EPIC -> {
            // 양 끝의 프리즘 면 · 오른쪽 아래 빗금 셋
            for (left in listOf(true, false)) {
                val tip = if (left) 4.3f * u else w - 4.3f * u
                val base = if (left) 10.6f * u else w - 10.6f * u
                val facet = Path().apply {
                    moveTo(tip, m); lineTo(base, m - 5.4f * u); lineTo(base, m + 5.4f * u); close()
                }
                drawPath(facet, Brush.linearGradient(listOf(c.motif.copy(alpha = 0.6f), c.rim[0].copy(alpha = 0.15f)),
                    start = Offset(tip, m - 4f * u), end = Offset(base, m + 4f * u)))
                drawLine(c.label.copy(alpha = 0.7f), Offset(tip, m), Offset(base, m - 5.4f * u), strokeWidth = 0.7f * u)
            }
            for (k in 0..2) {
                val x = w - 14.2f * u + k * 1.5f * u
                drawLine(c.motif.copy(alpha = 0.75f), Offset(x, h - 3.8f * u), Offset(x + 1.1f * u, h - 5.4f * u),
                    strokeWidth = 0.8f * u, cap = StrokeCap.Round)
            }
        }
        ShoeTier.LEGENDARY -> {
            // 위 테두리 가운데 꺾쇠 · 오른쪽 끝 안의 광점
            val cx = w / 2
            drawLine(c.motif, Offset(cx - 2.6f * u, 1.3f * u), Offset(cx, 3.5f * u), strokeWidth = 1f * u, cap = StrokeCap.Round)
            drawLine(c.motif, Offset(cx + 2.6f * u, 1.3f * u), Offset(cx, 3.5f * u), strokeWidth = 1f * u, cap = StrokeCap.Round)
            sparkle(Offset(w - 9.2f * u, m), 2.6f * u, c.label)
        }
        ShoeTier.REDLINE -> {
            // 왼쪽 가속선 둘(끝 쪽으로 옅게) · 오른쪽 끝 둥근 곳을 따라 도는 열기 파동 둘
            for (dy in listOf(-1.9f, 1.9f)) {
                drawLine(
                    Brush.horizontalGradient(listOf(c.motif.copy(alpha = 0.15f), c.motif), startX = 4.6f * u, endX = 12.6f * u),
                    Offset(4.6f * u, m + dy * u), Offset(12.6f * u, m + dy * u), strokeWidth = 1.1f * u, cap = StrokeCap.Round,
                )
            }
            val center = Offset(w - 12f * u, m)
            for ((k, radius) in listOf(4.6f, 6.8f).withIndex()) {
                drawArc(
                    color = c.motif.copy(alpha = 0.95f - k * 0.3f),
                    startAngle = -50f, sweepAngle = 100f, useCenter = false,
                    topLeft = Offset(center.x - radius * u, center.y - radius * u),
                    size = Size(radius * 2 * u, radius * 2 * u),
                    style = Stroke(width = 1.1f * u, cap = StrokeCap.Round),
                )
            }
        }
        ShoeTier.FINISH -> {
            // 안쪽 궤도선 · 오른쪽 끝의 체커 결승 테이프(2줄 × 4칸)
            drawPath(Path().addCapsule(capsule(4.2f * u)),
                Brush.horizontalGradient(listOf(c.accent.copy(alpha = 0.0f), c.accent.copy(alpha = 0.45f), c.motif.copy(alpha = 0.0f))),
                style = Stroke(width = 0.6f * u))
            val cell = 1.5f * u
            val left = w - 12.6f * u
            val top = m - cell
            for (row in 0..1) for (col in 0..3) {
                if ((row + col) % 2 == 0) {
                    drawRect(if (col % 2 == 0) c.accent else c.motif, Offset(left + col * cell, top + row * cell), Size(cell, cell))
                } else {
                    drawRect(c.label.copy(alpha = 0.25f), Offset(left + col * cell, top + row * cell), Size(cell, cell))
                }
            }
        }
    }
}

/** 바깥 번짐 — 흐림 대신 옅은 굵은 선 두 겹(작은 크기에서도 번지지 않고 또렷하게) */
private fun DrawScope.colorsGlow(c: TierColors, outer: RoundRect, u: Float) {
    val glow = c.glow ?: return
    drawPath(Path().addCapsule(outer), glow.copy(alpha = 0.14f), style = Stroke(width = 3.2f * u))
    drawPath(Path().addCapsule(outer), glow.copy(alpha = 0.22f), style = Stroke(width = 2.0f * u))
}

/** 네 갈래 광점 */
private fun DrawScope.sparkle(center: Offset, arm: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - arm)
        quadraticTo(center.x, center.y, center.x + arm, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + arm)
        quadraticTo(center.x, center.y, center.x - arm, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - arm)
        close()
    }
    drawPath(path, color)
}

/** 갈래 대표색 — 선택 테두리 · 능력치 막대 */
fun ShoeTier.accent(): Color = tierColors(this).accent

/** 배지 높이 — 이름 줄 높이를 맞출 때 */
val ShoeGradeBadgeHeight: Dp = BadgeHeight
