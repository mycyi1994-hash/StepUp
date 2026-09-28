package com.stepup.android

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode

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
    onNodeWithTag("crew-list-meetups").performClick()
    waitForIdle()
}
