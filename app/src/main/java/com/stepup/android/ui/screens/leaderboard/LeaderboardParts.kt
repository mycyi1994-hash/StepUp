package com.stepup.android.ui.screens.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.LeaderEntry
import com.stepup.android.domain.LeaderboardMath
import com.stepup.android.ui.components.PlainDivider
import com.stepup.android.ui.components.RunTone
import com.stepup.android.ui.components.runNumberStyle
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.feedbackClickable

/*
 * 리더보드 공통 부품(2026-11-24 재디자인 — 앱 테마). 예전의 늘 남색 한 벌 · 머리글자 칸 · 파란 면 줄은 없앴다.
 * 줄은 흰 카드(panel) 안 구분선으로 나누고, 색은 숫자(1 ~ 3위 순위 · 내 줄)와 글자 버튼에만 쓴다.
 */

internal object Lb {
    val Gutter = 16.dp
}

/** 값 + 단위 — km 는 소수 한 자리, 포인트는 정수. 단위는 숫자 뒤 작은 보조 글자 */
@Composable
internal fun LbValue(
    entry: LeaderEntry,
    points: Boolean,
    modifier: Modifier = Modifier,
    size: TextUnit = 18.sp,
    color: Color? = null,
    unitColor: Color? = null,
    valueTag: String? = null,
) {
    val value = if (points) LeaderboardMath.points(entry.metric) else LeaderboardMath.km(entry.metric)
    LbAmount(value, stringResource(if (points) R.string.lb_unit_points else R.string.lb_unit_km), modifier, size, color, unitColor, valueTag)
}

/** 숫자 + 단위(숫자 폭 고정 · 줄바꿈 없음) */
@Composable
internal fun LbAmount(
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 18.sp,
    color: Color? = null,
    unitColor: Color? = null,
    valueTag: String? = null,
) {
    val t = runTone()
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            value,
            style = runTextStyle(size, color ?: t.text, FontWeight.Bold, 1.15f).copy(fontFeatureSettings = "tnum"),
            maxLines = 1, softWrap = false,
            modifier = if (valueTag != null) Modifier.testTag(valueTag) else Modifier,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            unit, style = runTextStyle(13.sp, unitColor ?: t.label, FontWeight.Medium, 1.2f),
            maxLines = 1, softWrap = false, modifier = Modifier.padding(bottom = 1.dp),
        )
    }
}

/** 순위 숫자의 색 — 1 ~ 3위 · 내 줄은 파랑, 나머지는 흐린 글자 */
internal fun lbRankColor(t: RunTone, rank: Int?, me: Boolean): Color =
    if (me || (rank != null && rank <= 3)) t.cobaltText else t.muted

/**
 * 순위 한 줄 — 순위 숫자 · 이름(아래 작은 글) · 값. 1 ~ 3위는 62dp · 파란 굵은 순위, 나머지 54dp.
 * 내 줄은 순위 · 이름을 파랑으로(면은 칠하지 않는다). [tag] 가 없으면 lb-row-me / lb-row-순위.
 */
@Composable
internal fun LbRow(
    entry: LeaderEntry,
    points: Boolean,
    modifier: Modifier = Modifier,
    sub: String? = entry.sub,
    tag: String? = null,
    nameTag: String? = null,
    valueTag: String? = null,
) {
    val t = runTone()
    val me = entry.isMe
    val podium = entry.rank != null && entry.rank <= 3
    val meLabel = stringResource(R.string.lb_me)
    Row(
        modifier.fillMaxWidth().heightIn(min = if (podium) 62.dp else 54.dp).padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {
                if (me) contentDescription = "$meLabel · ${entry.rank ?: "—"} · ${entry.name}"
            }
            .testTag(tag ?: if (me) "lb-row-me" else "lb-row-${entry.rank ?: "x"}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            entry.rank?.toString() ?: "—",
            style = runNumberStyle(if (podium) 22.sp else 19.sp, lbRankColor(t, entry.rank, me), if (podium || me) FontWeight.Bold else FontWeight.SemiBold),
            textAlign = TextAlign.Start, maxLines = 1, softWrap = false, modifier = Modifier.widthIn(min = 34.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.name, style = runTextStyle(16.sp, if (me) t.cobaltText else t.text, FontWeight.SemiBold, 1.25f),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = if (nameTag != null) Modifier.testTag(nameTag) else Modifier,
            )
            if (sub != null) {
                Spacer(Modifier.height(2.dp))
                Text(sub, style = runTextStyle(13.sp, t.label, FontWeight.Medium, 1.25f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(12.dp))
        LbValue(entry, points, size = if (podium) 19.sp else 17.sp, valueTag = valueTag)
    }
}

/** 줄 여럿 — 사이에 1dp 구분선 */
@Composable
internal fun <T> LbRows(items: List<T>, row: @Composable (T) -> Unit) {
    items.forEachIndexed { i, item ->
        if (i > 0) PlainDivider()
        row(item)
    }
}

/** 목록 머리 — 왼쪽 이름 · 오른쪽 기준(작은 보조 글자) */
@Composable
internal fun LbListHead(title: String, criterion: String, modifier: Modifier = Modifier) {
    val t = runTone()
    Row(modifier.fillMaxWidth().heightIn(min = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = runTextStyle(15.5.sp, t.text, FontWeight.SemiBold), modifier = Modifier.weight(1f), maxLines = 1,
            overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(8.dp))
        Text(criterion, style = runTextStyle(13.sp, t.label, FontWeight.Medium), maxLines = 1, softWrap = false)
    }
}

/** 글자 탭(밑줄) — 내 주변 순위 | 상위 러너 · 전체 러너 | 우리 크루 */
@Composable
internal fun LbUnderlineTabs(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    tagPrefix: String = "lb-tab",
) {
    val t = runTone()
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
                    Text(label, style = runTextStyle(15.sp, if (on) t.text else t.label, FontWeight.SemiBold), maxLines = 1,
                        modifier = Modifier.padding(horizontal = 1.dp, vertical = 10.dp))
                    Box(Modifier.height(2.dp).widthIn(min = 24.dp).background(if (on) t.cobalt else Color.Transparent))
                }
            }
            if (i < labels.lastIndex) Spacer(Modifier.width(18.dp))
        }
        if (trailing != null) {
            Spacer(Modifier.weight(1f))
            Text(trailing, style = runTextStyle(13.sp, t.label, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * 아래 고정 내 줄 — 화면과 반대 색 알약(밝게: 검정 · 어둡게: 흰색, 모서리 22). 누를 수 있으면 줄 전체가 누르는 곳.
 * [content] 는 [LbFooterInk] 색으로 글을 쓴다.
 */
@Composable
internal fun LbFooterSurface(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    label: String? = null,
    content: @Composable RowScope.(LbFooterInk) -> Unit,
) {
    val t = runTone()
    val shape = RoundedCornerShape(22.dp)
    val ink = LbFooterInk(
        text = if (t.dark) Color(0xFF0A0D14) else Color.White,
        label = if (t.dark) Color(0xFF6B7080) else Color(0xFFA4A7B0),
        link = if (t.dark) t.cobalt else Color(0xFF8FB2FF),
    )
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp).clip(shape).background(t.text, shape)
            .then(if (onClick != null) Modifier.feedbackClickable(onClick = onClick) else Modifier)
            // 누를 수 없는 줄(크루 없음 · 순위 없음)도 한 덩어리로 읽힌다
            .semantics(mergeDescendants = true) { if (label != null) contentDescription = label }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) { content(ink) }
}

/** 아래 고정 줄 안의 글자 색 */
internal class LbFooterInk(val text: Color, val label: Color, val link: Color)

/** 아래 고정 줄의 › */
@Composable
internal fun LbFooterChevron(ink: LbFooterInk, size: Dp = 18.dp) {
    Icon(LbIcons.ChevronRight, contentDescription = null, tint = ink.label, modifier = Modifier.size(size))
}

/** 상세 시트 안의 작은 안내 */
@Composable
internal fun LbSheetNote(text: String, modifier: Modifier = Modifier) {
    val t = runTone()
    Text(text, style = runTextStyle(13.sp, t.label, FontWeight.Medium), modifier = modifier.padding(top = 2.dp, bottom = 10.dp))
}
