package com.stepup.android.ui.components

import com.stepup.android.ui.theme.Snow

import androidx.compose.ui.graphics.Color

import com.stepup.android.ui.theme.StepUpColors

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Volt
import com.stepup.android.ui.theme.StepUpDesign

/**
 * 거르기 한 벌 — 버튼 둘, 요약 한 줄, 아래에서 올라오는 패널.
 *
 * 러닝 이벤트 · 러닝·건강 뉴스 · 마켓 아이템이 모두 이 조각을 쓴다. 세
 * 화면이 같은 모양으로 걸러져야 한 번 배운 것이 다음 화면에서도 통한다.
 *
 * ── 왜 가로로 미는 칩을 버렸나 ──
 *
 * 칩을 한 줄에 늘어놓으면 화면에 안 들어오는 선택지가 생기고, 그것이 있는
 * 줄도 모른 채 지나간다. 또 목록의 첫 카드가 한참 아래로 밀린다. 그래서
 * 기본 화면에는 버튼 둘과 요약 한 줄만 두고, 선택지는 패널 안에서 줄을
 * 바꿔 가며 전부 보여 준다.
 *
 * ── 임시 선택 ──
 *
 * 패널 안에서 고른 것은 "결과 보기"를 눌러야 적용된다. X·뒤로가기·바깥을
 * 누르면 고르기 전으로 돌아간다. 고르는 족족 목록이 다시 불려 가면, 조건을
 * 다 맞추기 전에 서버를 네 번 부르게 된다.
 */

/** 기본 화면의 버튼 두 개 — 왼쪽 거르기, 오른쪽 정렬 */
@Composable
fun FilterToolbar(
    filterLabel: String,
    filterCount: Int,
    sortLabel: String,
    onOpenFilters: () -> Unit,
    onOpenSort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        ToolbarButton(
            text = if (filterCount > 0) "$filterLabel $filterCount" else filterLabel,
            leading = Icons.Filled.Tune,
            trailing = null,
            active = filterCount > 0,
            onClick = onOpenFilters,
            modifier = Modifier.weight(1f),
        )
        ToolbarButton(
            text = sortLabel,
            leading = null,
            trailing = Icons.Filled.ExpandMore,
            active = false,
            onClick = onOpenSort,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ToolbarButton(
    text: String,
    leading: androidx.compose.ui.graphics.vector.ImageVector?,
    trailing: androidx.compose.ui.graphics.vector.ImageVector?,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(StepUpDesign.ControlRadius)
    Row(
        modifier = modifier
            .heightIn(min = StepUpDesign.TouchTarget)
            .clip(shape)
            .background(if (active) Volt.copy(alpha = 0.12f) else CarbonHigh, shape)
            .border(1.dp, if (active) Volt.copy(alpha = 0.55f) else Edge, shape)
            .quietClickable(onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = StepUpDesign.SecondaryHorizontalPadding,
                vertical = StepUpDesign.SecondaryVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leading != null) {
            Icon(
                imageVector = leading,
                contentDescription = null,
                tint = if (active) Volt else Silver,
                modifier = Modifier.size(StepUpDesign.ControlIcon),
            )
            Spacer(Modifier.size(6.dp))
        }
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            fontSize = StepUpDesign.SecondaryLabel,
            fontWeight = FontWeight.Bold,
            color = if (active) Volt else Silver,
            textAlign = TextAlign.Center,
        )
        if (trailing != null) {
            Spacer(Modifier.size(4.dp))
            Icon(
                imageVector = trailing,
                contentDescription = null,
                tint = Silver,
                modifier = Modifier.size(StepUpDesign.ControlIcon),
            )
        }
    }
}

/**
 * 고른 조건을 짧은 글로.
 *
 * 세 개까지 적고 나머지는 "외 N개"로 줄인다. 요약과 초기화 동작을
 * 분리해서 글자 확대 때도 조건과 버튼이 서로를 밀어내지 않는다.
 */
@Composable
fun FilterSummaryRow(
    parts: List<String>,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    resetLabel: String? = null,
    extraAction: Pair<String, () -> Unit>? = null,
) {
    val shown = parts.take(3)
    val rest = parts.size - shown.size
    val text = when {
        parts.isEmpty() -> stringResource(R.string.filter_summary_none)
        rest > 0 -> shown.joinToString(" · ") + " " + stringResource(R.string.filter_summary_more, rest)
        else -> shown.joinToString(" · ")
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            fontSize = StepUpDesign.SecondaryLabel,
            color = if (parts.isEmpty()) Slate else Silver,
            lineHeight = 20.sp,
        )
        if (parts.isNotEmpty() || extraAction != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (parts.isNotEmpty()) GhostButton(
                    text = resetLabel ?: stringResource(R.string.filter_reset),
                    onClick = onReset,
                    modifier = Modifier.weight(1f),
                )
                if (extraAction != null) GhostButton(
                    text = extraAction.first,
                    onClick = extraAction.second,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * 아래에서 올라오는 거르기 패널.
 *
 * 공통 DialogPanel을 사용한다. 본문만 스크롤하며 적용·초기화는 아래에 유지된다.
 * 닫기·뒤로가기는 기존처럼 임시 선택을 버린다.
 */
@Composable
fun FilterBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
    resetLabel: String? = null,
    applyLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    DialogPanel(
        title = title, onDismiss = onDismiss,
        actions = {
            VoltButton(applyLabel ?: stringResource(R.string.filter_apply), onClick = onApply, modifier = Modifier.fillMaxWidth())
            GhostButton(resetLabel ?: stringResource(R.string.filter_reset), onClick = onReset, modifier = Modifier.fillMaxWidth())
        },
    ) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp), content = content)
    }
}

/**
 * 정렬만 고르는 작은 패널.
 *
 * 누르는 즉시 적용하고 닫는다. 정렬은 하나만 고르는 것이라 "결과 보기"를
 * 한 번 더 누르게 할 이유가 없다.
 */
@Composable
fun <T> SortBottomSheet(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    DialogPanel(
        title = title, onDismiss = onDismiss,
        actions = { GhostButton(stringResource(R.string.common_close), onClick = onDismiss, modifier = Modifier.fillMaxWidth()) },
    ) {
        options.forEach { option ->
            ChoiceChip(label(option), selected = option == selected, onClick = { onPick(option); onDismiss() }, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** 패널 안의 한 마당 — 제목 하나와 선택지 격자 하나 */
@Composable
fun FilterSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Slate,
        )
        content()
    }
}

/**
 * 선택지 격자.
 *
 * 화면이 좁으면 2열, 넉넉하면 3열로 접는다. 칸은 모두 같은 너비이고 긴
 * 이름은 두 줄까지 접힌다 — 화면 밖으로 잘려 나가는 칩이 없어야 한다.
 */
@Composable
fun <T> ChoiceGrid(
    values: List<T>,
    isSelected: (T) -> Boolean,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 3,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val fontScale = LocalDensity.current.fontScale
        val cols = when {
            fontScale >= 1.5f -> 1
            maxWidth < 340.dp || fontScale > 1f -> minOf(columns, 2)
            else -> columns
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            values.chunked(cols).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { value ->
                        ChoiceChip(
                            text = label(value),
                            selected = isSelected(value),
                            onClick = { onSelect(value) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // 마지막 줄이 모자라면 빈 자리로 채워 칸 너비를 맞춘다
                    repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** Shared choice: full label, growing height and a stable selected-state weight. */
@Composable
fun ChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(StepUpDesign.ControlRadius)
    Box(
        modifier = modifier
            .heightIn(min = StepUpDesign.TouchTarget)
            .clip(shape)
            .background(if (selected) (if (StepUpColors.dark) Color(0xFFF3F5FF) else Snow) else CarbonHigh, shape)
            .border(1.dp, if (selected) Color.Transparent else Edge.copy(alpha = 0.6f), shape)
            .quietClickable(onClick)
            .semantics { this.selected = selected; role = Role.Button }
            .padding(horizontal = StepUpDesign.SecondaryHorizontalPadding,
                vertical = StepUpDesign.SecondaryVerticalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = StepUpDesign.SecondaryLabel,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) (if (StepUpColors.dark) Color(0xFF070B12) else Color.White) else Silver,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )
    }
}
