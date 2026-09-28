package com.stepup.android

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp

/**
 * 커뮤니티 › 크루 모집 › 목록 끝 "번개 모임 · 같이 달리기" — 예전 "함께 뛰기" 글자 탭(번개 모임 · 내 크루)은
 * 크루 명함형(2026-09-28)에서 크루 모집 목록 안쪽으로 옮겼다. 기존 검사가 그 화면을 여는 길.
 */
fun ComposeTestRule.openCommunityMeetups() {
    waitUntil(10_000) { onAllNodes(hasTestTag("community-tab-crews")).fetchSemanticsNodes().isNotEmpty() }
    onNodeWithTag("community-tab-crews").performClick()
    waitForIdle()
    waitUntil(10_000) { onAllNodes(hasTestTag("crew-list-scroll")).fetchSemanticsNodes().isNotEmpty() }
    onNodeWithTag("crew-list-scroll").performScrollToNode(hasTestTag("crew-list-meetups"))
    // 오른쪽 아래에 떠 있는 "크루 모집하기"가 큰 글씨에서는 넓어져 이 줄의 가운데를 덮을 수 있다(목록이 조금만 넘쳐
    // 끝까지 올리지 않은 채 보일 때) — 글자가 있는 왼쪽을 누른다
    onNodeWithTag("crew-list-meetups").performTouchInput { click(centerLeft + Offset(24.dp.toPx(), 0f)) }
    waitForIdle()
}
