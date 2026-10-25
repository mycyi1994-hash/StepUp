package com.stepup.android

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick

/**
 * 함께 뛰기 › 번개러닝 — UI 최종 통합안(2026-10-25)에서 함께 뛰기의 첫 세그먼트다(번개러닝 · 크루 · 이야기). 기존 검사가 그 화면을 여는 길.
 */
fun ComposeTestRule.openCommunityMeetups() {
    waitUntil(10_000) { onAllNodes(hasTestTag("community-tab-meetups")).fetchSemanticsNodes().isNotEmpty() }
    onNodeWithTag("community-tab-meetups").performClick()
    waitForIdle()
}
