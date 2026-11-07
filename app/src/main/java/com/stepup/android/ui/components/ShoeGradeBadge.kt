package com.stepup.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.ShoeTier
import com.stepup.android.ui.experience.feedbackClickable
import kotlin.math.roundToInt

/*
 * 신발 이름 끝의 등급 배지 — 색을 채운 둥근 알약 + 흰 글씨(2026-09-29 전달본 "채운 등급 배지 6", design/grade-badges-2026-09-29).
 * 일반 · 레어 · 에픽 · 레전더리 · 레드라인 · 피니시 여섯 갈래가 모두 같은 모양이고 색만 다르다. 테두리 · 광택 · 번짐 · 장식은 두지 않는다.
 * 전달 PNG 는 시안 기준이다 — 앱은 글자를 기기 글꼴로 얹어 그린다(글자 확대 · 다국어가 그대로 된다). 누르는 곳이 아니다(표시일 뿐).
 * 내 신발 · 신발 보관함 · 메인 · 러닝 결과 · 뽑기 결과 모두 이 한 부품을 이름 끝에 붙인다(배지 전용 줄을 따로 두지 않는다).
 */

/** 갈래 색 — 배지 바탕(흰 글씨가 올라가는 채움색) */
@Immutable
class TierColors(
    /** 배지 채움 */
    val fill: Color,
    /** 배지 글자 */
    val label: Color = Color.White,
)

fun tierColors(tier: ShoeTier): TierColors = when (tier) {
    ShoeTier.COMMON -> TierColors(Color(0xFF596777))
    ShoeTier.RARE -> TierColors(Color(0xFF165DDF))
    ShoeTier.EPIC -> TierColors(Color(0xFF7941C6))
    ShoeTier.LEGENDARY -> TierColors(Color(0xFFA96710))
    ShoeTier.REDLINE -> TierColors(Color(0xFFC74143))
    ShoeTier.FINISH -> TierColors(Color(0xFF128071))
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
 * 배지 크기 — 보이는 높이 24dp(글자가 커지면 글자 + 위아래 3dp), 글자 12sp, 양옆 10dp(전달 지침: 높이 24–28dp · 글자 11–12sp ·
 * 좌우 8–10dp). 양옆은 높이에 비례해 넓힌다 — 큰 글씨에서도 글자가 둥근 끝에 붙지 않게.
 */
private val BadgeHeight = 24.dp
private val BadgeSidePadding = 10.dp
private val BadgeLabelInset = 3.dp
private val BadgeLabelSize = 12.sp

/** 글자 높이 [labelHeight] 에 맞는 배지 높이(px) */
private fun Density.badgeHeightPx(labelHeight: Int): Int =
    maxOf(BadgeHeight.roundToPx(), labelHeight + BadgeLabelInset.roundToPx() * 2)

/** 배지 높이 [height] 에 맞는 양옆(px) — 24dp 일 때 10dp */
private fun Density.badgeSidePx(height: Int): Int = (BadgeSidePadding.toPx() * height / BadgeHeight.toPx()).roundToInt()

/** 이름과 배지 사이 */
private val BadgeGap = 8.dp

/** 배지와 뒤에 붙는 글(번호) 사이 */
private val SuffixGap = 10.dp

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
            .drawBehind { drawRoundRect(colors.fill, cornerRadius = CornerRadius(size.height / 2f)) }
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
 * 이름 줄 끝의 작은 행동 하나 — 배지 · 번호 바로 오른쪽에 채운 둥근 알약(신발 상세의 "🔨 수리하기", 2026-11-07 사용자 선택 1안).
 * [icon] 은 글자 앞 장식(그림 글자)이라 읽기 도구는 [label] 만 한 번 읽는다. [enabled] 가 거짓이면 흐리게, 눌러도 아무 일이 없다.
 */
class ShoeNameAction(
    val label: String,
    val onClick: () -> Unit,
    /** 알약 채움 */
    val fill: Color,
    val tag: String,
    val enabled: Boolean = true,
    val icon: String? = null,
    /** 알약 글자 */
    val ink: Color = Color.White,
)

/**
 * 알약 크기 — 보이는 높이 28dp(글자가 커지면 글자 + 위아래 5dp), 글자 13sp, 양옆 10dp, 장식과 글자 사이 4dp.
 * 눌리는 곳은 보이는 알약보다 넓다 — 작은 누름 칸은 Compose 가 최소 누름 크기(48dp)까지 넓혀 받는다(이름 · 번호는 누르는 곳이 아니다).
 */
private val ActionHeight = 28.dp
private val ActionInset = 5.dp
private val ActionSidePadding = 10.dp
private val ActionIconGap = 4.dp
private val ActionLabelSize = 13.sp

/** 번호(번호가 없으면 배지)와 알약 사이 */
private val ActionGap = 8.dp

/** 한 줄에 다 안 들어가 이름 아래 줄로 내려갈 때 이름과 알약 사이 */
private val ActionDropGap = 6.dp

/** 잰 폭과 그린 폭이 조금 다를 때(그림 글자 · 글꼴 대체) 끝 글자가 잘리지 않게 */
private val ActionSlack = 2.dp

private fun actionLabelStyle(action: ShoeNameAction) =
    TextStyle(color = action.ink, fontSize = ActionLabelSize, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)

/** 알약 하나 — 크기는 부르는 쪽이 같은 계산으로 잰 값([ShoeNameWithBadge]) */
@Composable
private fun ShoeNameActionPill(action: ShoeNameAction, style: TextStyle, modifier: Modifier) {
    Box(
        modifier
            .alpha(if (action.enabled) 1f else .5f)
            // 누름(작아짐 · 어둡게)이 붉은 면까지 함께 — 면은 누름 안쪽에서 그린다
            .feedbackClickable(enabled = action.enabled, role = Role.Button, onClick = action.onClick)
            .drawBehind { drawRoundRect(action.fill, cornerRadius = CornerRadius(size.height / 2f)) }
            .testTag(action.tag),
        contentAlignment = Alignment.Center,
    ) {
        Row(Modifier.wrapContentSize(unbounded = true), verticalAlignment = Alignment.CenterVertically) {
            if (action.icon != null) {
                Text(action.icon, style = style, maxLines = 1, softWrap = false, modifier = Modifier.clearAndSetSemantics { })
                Spacer(Modifier.width(ActionIconGap))
            }
            Text(action.label, style = style, maxLines = 1, softWrap = false)
        }
    }
}

/**
 * 이름 + 끝의 배지 — 배지는 이름의 **마지막 줄 끝**에 같은 줄로 붙는다(따로 한 줄을 차지하지 않는다).
 * 이름은 [maxLines] 줄까지. 넘치면 이름을 줄여 "…"를 붙이고 배지는 그대로 둔다 — 배지가 잘려 사라지지 않게.
 * [suffix] 가 있으면(신발 상세의 "No. 0007") 배지 바로 오른쪽에 붙여 배지와 한 덩어리로 둔다 — 번호만 다음 줄로 떨어지지 않는다.
 * [action] 이 있으면(신발 상세의 수리하기) 번호 바로 오른쪽에 알약을 붙여 같은 덩어리로 둔다. 덩어리가 한 줄 폭에 다 들어가지 않으면
 * (아주 큰 글씨 · 좁은 화면) 알약만 이름 바로 아래 줄로 내린다 — 줄여 숨기거나 화면 밖으로 밀지 않는다. 없으면 예전 그대로다.
 */
@Composable
fun ShoeNameWithBadge(
    name: String,
    tier: ShoeTier,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = 2,
    textAlign: TextAlign = TextAlign.Start,
    suffix: String? = null,
    suffixStyle: TextStyle = TextStyle.Default,
    action: ShoeNameAction? = null,
) {
    val density = LocalDensity.current
    val colors = remember(tier) { tierColors(tier) }
    val label = tier.label()
    val measurer = rememberTextMeasurer()
    // 재는 글과 그리는 글이 같은 글꼴이어야 배지 자리가 맞는다 — 테마 글꼴(LocalTextStyle)을 함께 쓴다
    val base = LocalTextStyle.current
    val nameStyle = remember(base, style, textAlign) { base.merge(style).copy(textAlign = textAlign) }
    val labelStyle = remember(base, colors) { base.merge(badgeLabelStyle(colors)) }
    val actionStyle = action?.let { base.merge(actionLabelStyle(it)) }
    BoxWithConstraints(modifier) {
        val labelSize = remember(label, labelStyle, density) { measurer.measure(label, labelStyle).size }
        val tailStyle = remember(base, suffixStyle) { base.merge(suffixStyle) }
        val suffixSize = remember(suffix, tailStyle, density) {
            suffix?.let { measurer.measure(it, tailStyle, softWrap = false).size }
        }
        val suffixWidth = with(density) { suffixSize?.let { (it.width + 1).toDp() } ?: 0.dp }
        val suffixHeight = with(density) { suffixSize?.height?.toDp() ?: 0.dp }
        // 배지 칸(badgeLayout)과 같은 계산 — 글자가 커지면 높이 · 양옆이 함께 커진다
        val (badgeWidth, badgeHeight) = remember(labelSize, density) {
            with(density) {
                val height = badgeHeightPx(labelSize.height)
                (labelSize.width + badgeSidePx(height) * 2).toDp() to height.toDp()
            }
        }
        // 알약 — 글자 · 장식을 재서 높이 · 폭을 정한다(글자가 커지면 함께 커진다)
        val actionSize = remember(action?.label, action?.icon, actionStyle, density) {
            if (action == null || actionStyle == null) null else with(density) {
                val text = measurer.measure(action.label, actionStyle, softWrap = false).size
                val icon = action.icon?.let { measurer.measure(it, actionStyle, softWrap = false).size }
                val height = maxOf(ActionHeight.roundToPx(), maxOf(text.height, icon?.height ?: 0) + ActionInset.roundToPx() * 2)
                val width = text.width + (icon?.let { it.width + ActionIconGap.roundToPx() } ?: 0) +
                    ActionSidePadding.roundToPx() * 2 + ActionSlack.roundToPx()
                DpSize(width.toDp(), height.toDp())
            }
        }
        val widthPx = constraints.maxWidth
        val blockWidth = badgeWidth + BadgeGap + if (suffix != null) SuffixGap + suffixWidth else 0.dp
        // 배지 · 번호 · 알약 한 덩어리가 한 줄 폭에 들어가면 번호 바로 오른쪽, 아니면 이름 아래 줄로
        val actionInline = actionSize != null &&
            (widthPx == Constraints.Infinity || with(density) { (blockWidth + ActionGap + actionSize.width).roundToPx() } <= widthPx)
        val placeholder = with(density) {
            Placeholder(
                width = (blockWidth + if (actionInline && actionSize != null) ActionGap + actionSize.width else 0.dp).toSp(),
                height = maxOf(badgeHeight, suffixHeight, if (actionInline && actionSize != null) actionSize.height else 0.dp).toSp(),
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
            )
        }
        // 대체 글 — 배지 자리가 읽히는 말(" 레어", 번호가 있으면 " 레어 No. 0007"). 알약은 따로 읽히는 버튼이라 넣지 않는다
        val alt = if (suffix != null) "$label $suffix" else label
        val text = remember(name, alt, nameStyle, widthPx, placeholder, maxLines) {
            fittedName(name, alt, widthPx) { candidate ->
                measurer.measure(candidate, nameStyle, maxLines = maxLines, constraints = Constraints(maxWidth = widthPx),
                    placeholders = listOf(AnnotatedString.Range(placeholder, candidate.length - badgeAltLength(alt), candidate.length)))
                    .hasVisualOverflow
            }
        }
        val nameText = @Composable {
            Text(
                text = text,
                style = nameStyle,
                maxLines = maxLines,
                overflow = TextOverflow.Clip,
                inlineContent = mapOf(
                    BadgeInlineId to InlineTextContent(placeholder) {
                        Row(Modifier.padding(start = BadgeGap), verticalAlignment = Alignment.CenterVertically) {
                            // 글의 대체 글(" 레어")이 등급을 이미 읽어 준다
                            ShoeGradeBadge(tier, Modifier.size(width = badgeWidth, height = badgeHeight), decorative = true)
                            if (suffix != null) {
                                Text(suffix, style = tailStyle, maxLines = 1, softWrap = false,
                                    modifier = Modifier.padding(start = SuffixGap).clearAndSetSemantics { testTag = "shoe-name-suffix" })
                            }
                            if (actionInline && action != null && actionStyle != null && actionSize != null) {
                                ShoeNameActionPill(action, actionStyle, Modifier.padding(start = ActionGap).size(actionSize))
                            }
                        }
                    },
                ),
            )
        }
        if (!actionInline && action != null && actionStyle != null && actionSize != null) {
            Column(
                horizontalAlignment = if (textAlign == TextAlign.Center) Alignment.CenterHorizontally else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(ActionDropGap),
            ) {
                nameText()
                ShoeNameActionPill(action, actionStyle, Modifier.size(actionSize))
            }
        } else {
            nameText()
        }
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

/** 배지 높이 — 이름 줄 높이를 맞출 때 */
val ShoeGradeBadgeHeight: Dp = BadgeHeight
