package com.stepup.android

import android.util.Log
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeTestRule

/**
 * 화면에 보이는지 묻기 — 실패하면 그 줄과 모든 창(뿌리)의 자리를 기록에 남기고 다시 던진다(2026-11-03 C8 — 공용 화면으로 바꾼 뒤
 * "있는데 보이지 않음 · 누를 수 없음"이 무엇 때문인지 기기 없이 보려고). logcat 꼬리표 StepUpDisplay.
 */
fun ComposeTestRule.assertDisplayedOrExplain(matcher: SemanticsMatcher, what: String, timeoutMs: Long = 5_000) {
    val shown = runCatching {
        waitUntil(timeoutMs) { runCatching { onNode(matcher).assertIsDisplayed() }.isSuccess }
    }.isSuccess
    if (shown) return
    explain(matcher, what)
    onNode(matcher).assertIsDisplayed()
}

/** 눌러 보기 — 누를 수 없으면 자리를 남기고 다시 던진다 */
fun ComposeTestRule.clickOrExplain(matcher: SemanticsMatcher, what: String, click: (SemanticsNodeInteraction) -> Unit) {
    try {
        click(onNode(matcher))
    } catch (failure: Throwable) {
        explain(matcher, what)
        throw failure
    }
}

private fun ComposeTestRule.explain(matcher: SemanticsMatcher, what: String) {
    val lines = mutableListOf("=== $what ===")
    for (unmerged in listOf(false, true)) {
        runCatching {
            onAllNodes(matcher, useUnmergedTree = unmerged).fetchSemanticsNodes().forEach {
                lines += (if (unmerged) "unmerged " else "merged ") + describe(it) + " parentChain=" +
                    generateSequence(it.parent) { p -> p.parent }.take(6).joinToString(">") { p -> "${p.id}:${p.size}@${p.positionInWindow}" }
            }
        }.onFailure { lines += "lookup(unmerged=$unmerged) failed: ${it.message}" }
    }
    runCatching {
        onAllNodes(isRoot()).fetchSemanticsNodes().forEachIndexed { i, root -> lines += "root[$i] " + describe(root) }
    }.onFailure { lines += "root lookup failed: ${it.message}" }
    lines.forEach { Log.e("StepUpDisplay", it) }
}

private fun describe(node: SemanticsNode): String =
    "id=${node.id} boundsInRoot=${node.boundsInRoot} boundsInWindow=${node.boundsInWindow} size=${node.size} pos=${node.positionInWindow} " +
        "placed=${node.layoutInfo.isPlaced} attached=${node.layoutInfo.isAttached} config=${node.config.toString().take(160)}"
