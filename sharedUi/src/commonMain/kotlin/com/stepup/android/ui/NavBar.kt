package com.stepup.android.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.guide.GuideTour
import com.stepup.android.ui.guide.guideTarget
import com.stepup.android.ui.theme.StepUpDesign
import org.jetbrains.compose.resources.stringResource

/*
 * 하단 탭(안드로이드 StepUpRoot.kt 의 VoltNavBar · NavTab · tabIcon 과 같은 모양) — 화면 옮기기는 부르는 쪽이 한다(onSelect).
 * 같은 탭을 다시 누르면 그 탭의 첫 화면으로 돌아가는 규칙도 부르는 쪽이 정한다.
 */

const val BOTTOM_NAV_TAG = "bottom-nav"

@Composable
fun StepUpTabBar(
    /** 지금 고른 탭(하위 화면이면 그 부모 탭) */
    selectedTab: Screen?,
    onSelect: (Screen) -> Unit,
    bottomTabs: List<Screen> = listOf(Screen.Run, Screen.Community, Screen.Customize, Screen.Profile),
) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val labelStyle = com.stepup.android.ui.components.StepUpUi.type.tabLabel.copy(textAlign = TextAlign.Center)
    val labels = bottomTabs.map { stringResource(it.labelRes) }
    val measureStyle = labelStyle
    // Allocate spare space to long localized labels without defeating system font scaling.
    val preferredWidths = labels.map { label ->
        val measured = measurer.measure(text = label, style = measureStyle, softWrap = false)
        (with(density) { measured.size.width.toDp().value } + 10f)
            .coerceAtLeast(StepUpDesign.TouchTarget.value)
    }
    val equalWidth = maxWidth.value / labels.size
    val minimumWidth = StepUpDesign.TouchTarget.value
    val widths = if (preferredWidths.all { it <= equalWidth }) labels.map { equalWidth } else {
        val spare = maxWidth.value - preferredWidths.sum()
        if (spare >= 0f) preferredWidths.map { it + spare / labels.size } else {
            val needs = preferredWidths.map { (it - minimumWidth).coerceAtLeast(0f) }
            val available = (maxWidth.value - minimumWidth * labels.size).coerceAtLeast(0f)
            val totalNeed = needs.sum().coerceAtLeast(1f)
            needs.map { minimumWidth + available * it / totalNeed }
        }
    }
    val labelHeightPx = bottomTabs.mapIndexed { index, screen ->
        val labelWidth = with(density) { (widths[index].dp - 8.dp).roundToPx().coerceAtLeast(1) }
        measurer.measure(
            text = stringResource(screen.labelRes), style = measureStyle,
            constraints = androidx.compose.ui.unit.Constraints(maxWidth = labelWidth),
        ).size.height
    }.maxOrNull() ?: 0
    val labelHeight = with(density) { labelHeightPx.toDp() }
    // UI 최종 통합안(2026-10-25) — 반투명 탭바(다크 #070B14 82% · 라이트 #FAFAFC 92%) + 위 0.5dp 선, 선택만 블루. 파란 타일 없음
    val ui = com.stepup.android.ui.components.StepUpUi.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(ui.tabBar),
    ) {
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(ui.separator))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // 프로필 화면 안에도 "프로필" 탭이 있다. 검사가 하단 탭만 집도록 이름을 단다.
                .testTag(BOTTOM_NAV_TAG)
                .navigationBarsPadding()
                // 탭 줄(아이콘 26 · 글자 10.5) — 그 아래로 시스템 안전 영역만큼 더 내려간다
                .heightIn(min = 58.dp)
                .padding(top = 6.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val parent = selectedTab
            bottomTabs.forEachIndexed { index, screen ->
                NavTab(
                    screen = screen,
                    slotWeight = widths[index],
                    labelHeight = labelHeight,
                    labelStyle = labelStyle,
                    selected = parent == screen,
                    onClick = { onSelect(screen) },
                )
            }
        }
    }
    }
}

@Composable
private fun RowScope.NavTab(
    screen: Screen, labelHeight: androidx.compose.ui.unit.Dp,
    slotWeight: Float,
    labelStyle: androidx.compose.ui.text.TextStyle,
    selected: Boolean, onClick: () -> Unit,
) {
    // UI 최종 통합안 — 고른 탭만 블루(표면 위 파란 글자), 나머지는 보조 글자색. 아이콘은 가는 선, 커뮤니티는 고르면 채운 모양
    val ui = com.stepup.android.ui.components.StepUpUi.colors
    val tint by animateColorAsState(
        targetValue = if (selected) ui.accentText else ui.secondaryLabel,
        label = "navTabTint",
    )
    Column(
        modifier = Modifier.weight(slotWeight)
            // 기능을 설명하기 전에 "그게 이 버튼 안에 있다"부터 보여준다.
            .guideTarget(GuideTour.Targets.tab(screen.route))
            .semantics { this.selected = selected }
            .feedbackClickable(cue = FeedbackCue.Select, role = Role.Tab) { onClick() }
            .heightIn(min = 52.dp)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = tabIcon(screen, selected),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(26.dp).testTag("nav-icon-${screen.route}"),
        )
        Text(
            text = stringResource(screen.labelRes),
            modifier = Modifier.fillMaxWidth().heightIn(min = labelHeight).testTag("nav-label-${screen.route}"),
            color = tint,
            style = labelStyle,
            softWrap = true,
        )
    }
}

/** 하단 탭 아이콘 — 가는 선 아이콘(통합안). 커뮤니티만 고르면 채운 모양(시안) */
private fun tabIcon(screen: Screen, selected: Boolean): ImageVector = when (screen) {
    Screen.Run -> com.stepup.android.ui.components.StepUpIcons.TabRun
    Screen.Customize -> com.stepup.android.ui.components.StepUpIcons.TabShoe
    Screen.Draw -> com.stepup.android.ui.components.StepUpIcons.TabDraw
    Screen.Community -> if (selected) Icons.Filled.Groups else com.stepup.android.ui.components.StepUpIcons.TabCommunity
    Screen.Profile -> com.stepup.android.ui.components.StepUpIcons.TabProfile
}

