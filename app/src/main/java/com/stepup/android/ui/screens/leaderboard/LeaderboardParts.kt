package com.stepup.android.ui.screens.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
import com.stepup.android.domain.LeaderEntry
import com.stepup.android.domain.LeaderboardMath
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpNumbers
import com.stepup.android.ui.theme.StepUpSans

/*
 * 리더보드 공통 부품(2026-10-17 전달본 source/preview.html · shared-style.html) — 색 · 글자 · 줄 · 아래 내 줄 · 글자 탭.
 * 세 화면 모두 딥 네이비 한 벌이다(앱 테마와 상관없이 시안 색).
 */

internal object Lb {
    val Navy = Color(0xFF061B30)
    val Blue = Color(0xFF1450FF)
    val White = Color.White
    val Ice = Color(0xFFB4C9E2)
    val Muted = Color(0xFF9DB0C8)
    val Line = Color(0xFF29435F)
    val RowLine = Color(0x8C29435F)
    val Surface = Color(0xFF102B46)
    val Avatar = Color(0xFF23435F)
    val AvatarInk = Color(0xFFE4EFFF)
    val CrewAvatar = Color(0xFF174776)
    val MyBar = Color(0xFF173B63)
    val Rise = Color(0xFF78A7FF)
    val Underline = Color(0xFF6599FF)
    val Gutter = 20.dp
}

internal fun lbText(size: TextUnit, color: Color = Lb.White, weight: FontWeight = FontWeight.Normal, height: Float = 1.4f, spacing: TextUnit = (-0.01).em) =
    TextStyle(fontFamily = StepUpSans, fontWeight = weight, fontSize = size, color = color, lineHeight = height.em, letterSpacing = spacing)

/** 숫자 — 앱 숫자 서체(Pretendard)를 굵고 조밀하게. [italic] 이면 기울인다(시안의 큰 순위 숫자) */
internal fun lbNumber(size: TextUnit, color: Color = Lb.White, weight: FontWeight = FontWeight.Bold, italic: Boolean = false, spacing: TextUnit = (-0.02).em) =
    TextStyle(
        fontFamily = StepUpNumbers, fontWeight = weight, fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        fontSize = size, color = color, lineHeight = 1.em, letterSpacing = spacing, fontFeatureSettings = "tnum",
    )

/** 동그란(사람) · 둥근 네모(크루) 머리글자 칸 */
@Composable
internal fun LbAvatar(mark: String, modifier: Modifier = Modifier, size: Dp = 36.dp, crew: Boolean = false, face: Color? = null) {
    val shape: Shape = if (crew) RoundedCornerShape(size * 0.3f) else CircleShape
    Box(
        modifier.size(size).clip(shape).background(face ?: if (crew) Lb.CrewAvatar else Lb.Avatar, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            mark.take(2),
            style = TextStyle(
                fontFamily = StepUpSans, fontWeight = if (crew) FontWeight.ExtraBold else FontWeight.Bold,
                fontStyle = if (crew) FontStyle.Italic else FontStyle.Normal, fontSize = (size.value * 0.4f).sp, color = Lb.AvatarInk,
            ),
            maxLines = 1,
        )
    }
}

/** 값 + 단위 — km 는 소수 한 자리, 포인트는 정수 */
@Composable
internal fun LbValue(
    entry: LeaderEntry,
    points: Boolean,
    modifier: Modifier = Modifier,
    size: TextUnit = 20.sp,
    inlineUnit: Boolean = true,
    color: Color = Lb.White,
    unitColor: Color = Lb.Muted,
) {
    val value = if (points) LeaderboardMath.points(entry.metric) else LeaderboardMath.km(entry.metric)
    val unit = stringResource(if (points) R.string.lb_unit_points else R.string.lb_unit_km)
    if (inlineUnit) {
        Row(modifier, verticalAlignment = Alignment.Bottom) {
            Text(value, style = lbNumber(size, color, FontWeight.SemiBold), maxLines = 1)
            Spacer(Modifier.width(4.dp))
            Text(unit, style = lbText(11.sp, unitColor), maxLines = 1, modifier = Modifier.padding(bottom = 1.dp))
        }
    } else {
        Column(modifier, horizontalAlignment = Alignment.End) {
            Text(value, style = lbNumber(size, color, FontWeight.SemiBold), maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text(unit, style = lbText(11.sp, unitColor), maxLines = 1)
        }
    }
}

/**
 * 순위 한 줄 — 순위 · 머리글자 · 이름(아래 작은 글) · 값. 내 줄은 파란 면.
 * [ladder] 는 개인 순위 사다리(줄 사이 선 없이, 모든 줄이 좌우로 8 넓어지고 안쪽 8 — 내 줄의 파란 면만 바깥으로 보인다),
 * [sheet] 는 시트 안(내 줄만 안쪽 9).
 */
@Composable
internal fun LbRow(
    entry: LeaderEntry,
    points: Boolean,
    modifier: Modifier = Modifier,
    crew: Boolean = false,
    minHeight: Dp = 58.dp,
    rankWidth: Dp = 22.dp,
    rankSize: TextUnit = 18.sp,
    rankColor: Color = Lb.Ice,
    avatarSize: Dp = 36.dp,
    nameSize: TextUnit = 14.sp,
    valueSize: TextUnit = 20.sp,
    sub: String? = entry.sub,
    stackedUnit: Boolean = false,
    divider: Boolean = true,
    ladder: Boolean = false,
    sheet: Boolean = false,
    avatar: (@Composable () -> Unit)? = null,
) {
    val me = entry.isMe
    val meLabel = stringResource(R.string.lb_me)
    val shape = RoundedCornerShape(12.dp)
    val inset = when {
        ladder -> 8.dp
        sheet && me -> 9.dp
        me -> 8.dp
        else -> 0.dp
    }
    Row(
        modifier.then(if (ladder) Modifier.lbBleed(8.dp) else Modifier).fillMaxWidth().heightIn(min = minHeight)
            .then(if (me) Modifier.clip(shape).background(Lb.Blue, shape) else Modifier)
            .then(if (divider && !me) Modifier.lbDivider(Lb.RowLine) else Modifier)
            .padding(horizontal = inset)
            .semantics(mergeDescendants = true) {
                if (me) contentDescription = "$meLabel · ${entry.rank ?: "—"} · ${entry.name}"
            }
            .testTag(if (me) "lb-row-me" else "lb-row-${entry.rank ?: "x"}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            entry.rank?.toString() ?: "—",
            style = lbNumber(rankSize, if (me) Lb.White else rankColor, if (me) FontWeight.Bold else FontWeight.SemiBold),
            textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.widthIn(min = rankWidth),
        )
        if (avatar != null) {
            avatar()
        } else {
            LbAvatar(entry.mark, size = avatarSize, crew = crew, face = if (me) Color(0x29FFFFFF) else null)
        }
        Column(Modifier.weight(1f)) {
            Text(entry.name, style = lbText(nameSize, Lb.White, FontWeight.SemiBold, 1.25f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub != null) {
                Spacer(Modifier.height(3.dp))
                Text(sub, style = lbText(11.sp, if (me) Color(0xFFE1EBFF) else Lb.Muted, height = 1.2f), maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }
        LbValue(entry, points, size = valueSize, inlineUnit = !stackedUnit, unitColor = if (me) Color(0xFFE1EBFF) else Lb.Muted)
    }
}

/** 줄 아래 가는 선 */
internal fun Modifier.lbDivider(color: Color): Modifier = drawBehind {
    val y = size.height - 0.5.dp.toPx()
    drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
}

/** 좌우로 [x] 씩 넓힌다(시안의 margin: 0 -x) — 면만 바깥으로 나가고 안의 글은 같은 줄에 선다 */
internal fun Modifier.lbBleed(x: Dp): Modifier = layout { measurable, constraints ->
    val extra = x.roundToPx() * 2
    if (!constraints.hasBoundedWidth) {
        val placeable = measurable.measure(constraints)
        return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
    val wide = constraints.copy(minWidth = constraints.minWidth + extra, maxWidth = constraints.maxWidth + extra)
    val placeable = measurable.measure(wide)
    layout(placeable.width - extra, placeable.height) { placeable.place(-x.roundToPx(), 0) }
}

/** 목록 머리 — 왼쪽 굵은 이름 · 오른쪽 기준 */
@Composable
internal fun LbListHead(title: String, criterion: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().heightIn(min = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = lbText(13.sp, Lb.White, FontWeight.SemiBold), modifier = Modifier.weight(1f), maxLines = 1,
            overflow = TextOverflow.Ellipsis)
        Text(criterion, style = lbText(11.sp, Lb.Muted), maxLines = 1)
    }
}

/**
 * 순위 변화 — 위 화살표 + "n계단 상승" / 아래 + "n계단 하락" / "순위 그대로". 지난 기간 순위가 없으면 [newWhenUnknown] 일 때만
 * "이번 주 첫 순위", 아니면 아무것도 그리지 않는다(숫자를 만들지 않는다).
 */
@Composable
internal fun LbMove(rise: Int?, modifier: Modifier = Modifier, color: Color = Lb.Rise, size: TextUnit = 11.sp, newWhenUnknown: Boolean = false) {
    val text = when {
        rise == null && newWhenUnknown -> stringResource(R.string.lb_move_new)
        rise == null -> return
        rise > 0 -> stringResource(R.string.lb_move_up, rise)
        rise < 0 -> stringResource(R.string.lb_move_down, -rise)
        else -> stringResource(R.string.lb_move_same)
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (rise != null && rise != 0) {
            Icon(if (rise > 0) LbIcons.ChevronUp else LbIcons.ChevronDown, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(3.dp))
        }
        Text(text, style = lbText(size, color, FontWeight.SemiBold), maxLines = 1)
    }
}

/** 글자 탭(밑줄) — 시안의 내 주변 순위 | 상위 러너 · 전체 러너 | 우리 크루 */
@Composable
internal fun LbUnderlineTabs(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    tagPrefix: String = "lb-tab",
) {
    Row(modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
        labels.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.heightIn(min = 44.dp)
                    .feedbackClickable(role = Role.Tab, cue = FeedbackCue.Select) { onSelect(i) }
                    .semantics { this.selected = on; role = Role.Tab }
                    .testTag("$tagPrefix-$i"),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, style = lbText(13.sp, if (on) Lb.White else Lb.Muted, FontWeight.SemiBold), maxLines = 1,
                        modifier = Modifier.padding(horizontal = 1.dp, vertical = 10.dp))
                    Box(Modifier.height(2.dp).widthIn(min = 24.dp).background(if (on) Lb.Underline else Color.Transparent))
                }
            }
            if (i < labels.lastIndex) Spacer(Modifier.width(18.dp))
        }
        if (trailing != null) {
            Spacer(Modifier.weight(1f))
            Text(trailing, style = lbText(11.sp, Lb.Muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** 아래 고정 내 줄의 면(파란 남색 · 둥근 16) — 누르면 상세 */
@Composable
internal fun LbFooterSurface(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    face: Color = Lb.MyBar,
    minHeight: Dp = 73.dp,
    label: String? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier.fillMaxWidth().heightIn(min = minHeight).clip(shape).background(face, shape)
            .then(if (onClick != null) Modifier.feedbackClickable(onClick = onClick) else Modifier)
            .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) { content() }
}

/** 상세 시트 안의 작은 안내 */
@Composable
internal fun LbSheetNote(text: String, modifier: Modifier = Modifier) {
    Text(text, style = lbText(12.sp, Lb.Muted), modifier = modifier.padding(top = 4.dp, bottom = 14.dp))
}

/** 상태 칸 — 비었음 · 순위 없음 · 못 가져옴 · 준비 중 */
@Composable
internal fun LbStateBox(
    title: String,
    body: String?,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    tag: String = "lb-state",
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(Lb.Surface, shape).border(1.dp, Lb.Line, shape)
            .padding(horizontal = 18.dp, vertical = 20.dp).testTag(tag),
    ) {
        Text(title, style = lbText(15.sp, Lb.White, FontWeight.SemiBold))
        if (body != null) {
            Spacer(Modifier.height(6.dp))
            Text(body, style = lbText(13.sp, Lb.Ice, height = 1.6f))
        }
        if (action != null && onAction != null) {
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp)).background(Lb.Blue)
                    .feedbackClickable(onClick = onAction).padding(horizontal = 16.dp).testTag("$tag-action"),
                contentAlignment = Alignment.Center,
            ) { Text(action, style = lbText(14.sp, Lb.White, FontWeight.SemiBold)) }
        }
    }
}
