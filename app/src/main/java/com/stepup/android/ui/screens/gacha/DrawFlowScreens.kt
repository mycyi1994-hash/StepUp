package com.stepup.android.ui.screens.gacha

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.DrawKind
import com.stepup.android.domain.Rarity
import com.stepup.android.ui.components.SecondaryHeader
import com.stepup.android.ui.components.SneakerGradeStage
import com.stepup.android.ui.components.label
import com.stepup.android.ui.components.variantLabel
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.LocalFeedback
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpDesign

/*
 * 뽑기를 누른 뒤의 장면 — 결과 확인 중(10) → 상자 열기(11) → 결과(12 · 13 · 14), 답을 받지 못했을 때의 결과 확인 지연(20).
 * 머리의 뒤로(와 기기 뒤로)는 10 · 20 에서 요청을 그대로 둔 채 두 칸으로(26), 11 에서는 확정된 결과로, 결과에서는 두 칸으로 간다.
 */

@Composable
private fun kindTitle(kind: DrawKind): String = stringResource(if (kind == DrawKind.FREE) R.string.dv2_free else R.string.dv2_premium)

/** 한 장면의 큰 제목 · 한 줄 — 화면 읽기가 바뀐 상태를 알린다 */
@Composable
private fun FlowHeadline(title: String, body: String, modifier: Modifier = Modifier) {
    val p = drawPalette()
    Column(modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
        Text(
            title, color = p.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { heading() },
        )
        Text(
            body, color = p.secondary, fontSize = 14.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}

/** 10 — 서버가 결과를 확인할 때까지. 버튼이 없다(다시 누를 수 없다) */
@Composable
internal fun DrawRequestScreen(kind: DrawKind, onBack: () -> Unit) {
    val p = drawPalette()
    val feedback = LocalFeedback.current
    LaunchedEffect(kind) { feedback?.play(FeedbackCue.DrawCharge) }
    Column(Modifier.fillMaxSize().testTag("draw-requesting")) {
        SecondaryHeader(
            onBack = onBack, balance = null, onOpenWallet = null, title = kindTitle(kind),
            modifier = Modifier.padding(horizontal = StepUpDesign.Gutter),
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = StepUpDesign.Gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FlowHeadline(stringResource(R.string.dv2_request_head), stringResource(R.string.dv2_request_sub), Modifier.padding(top = 20.dp))
            UnboxingStage(shoe = null, modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(top = 12.dp))
            CircularProgressIndicator(Modifier.padding(top = 8.dp).size(30.dp), color = p.accent, strokeWidth = 2.5.dp, trackColor = p.track)
            Text(
                stringResource(R.string.dv2_wait), color = p.secondary, fontSize = 14.sp,
                modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
            )
        }
    }
}

/** 11 — 결과를 받은 뒤에만. 약 2초(동작 줄이기면 0.2초), 건너뛰면 같은 결과로 */
@Composable
internal fun DrawOpeningScreen(result: DrawnShoe, onFinish: () -> Unit, frozenAt: Float? = null) {
    val p = drawPalette()
    val feedback = LocalFeedback.current
    LaunchedEffect(result.shoe.id) { feedback?.play(FeedbackCue.DrawBoxOpen) }
    Column(Modifier.fillMaxSize().testTag("draw-opening")) {
        SecondaryHeader(
            onBack = onFinish, balance = null, onOpenWallet = null, title = kindTitle(result.kind),
            modifier = Modifier.padding(horizontal = StepUpDesign.Gutter),
            trailing = {
                Box(
                    Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                        .feedbackClickable(role = Role.Button, onClick = onFinish).padding(horizontal = 10.dp)
                        .testTag("draw-skip"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.dv2_skip), color = p.secondary, fontSize = 15.sp)
                }
            },
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = StepUpDesign.Gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FlowHeadline(stringResource(R.string.dv2_opening_head), stringResource(R.string.dv2_opening_sub), Modifier.padding(top = 20.dp))
            UnboxingStage(
                shoe = result.shoe, play = true, onFinished = onFinish, frozenAt = frozenAt,
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(top = 12.dp).testTag("draw-opening-stage"),
            )
            Text(
                stringResource(R.string.dv2_opening_note), color = p.secondary, fontSize = 14.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
        }
    }
}

/**
 * 12 무료 결과 · 13 상급 결과 · 14 마지막 기회 — 서버가 준 신발(등급 프레임 v8 무대)과 뽑은 뒤 서버가 준 남은 수.
 * "내 신발 보기"는 받은 신발을 고른 채로 신발 탭을 연다(착용은 그대로). "한 번 더 뽑기"는 남은 수를 다시 확인한 새 요청이다.
 */
@Composable
internal fun DrawResultScreen(
    result: DrawnShoe,
    canDrawAgain: Boolean,
    onOpenShoes: () -> Unit,
    onDrawAgain: () -> Unit,
    onClose: () -> Unit,
) {
    val p = drawPalette()
    val feedback = LocalFeedback.current
    LaunchedEffect(result.shoe.id) { feedback?.play(result.shoe.rarity.revealCue()) }
    val name = result.shoe.variantLabel()
    val grade = result.shoe.rarity.label()
    Column(Modifier.fillMaxSize().testTag("draw-result")) {
        SecondaryHeader(
            onBack = onClose, balance = null, onOpenWallet = null,
            title = stringResource(if (result.kind == DrawKind.FREE) R.string.dv2_result_free else R.string.dv2_result_premium),
            modifier = Modifier.padding(horizontal = StepUpDesign.Gutter),
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = StepUpDesign.Gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.fillMaxWidth().padding(top = 16.dp).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(grade, color = p.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("draw-result-grade"))
                Text(
                    name, color = p.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    lineHeight = 34.sp, modifier = Modifier.padding(top = 6.dp).semantics { heading() }.testTag("draw-result-name"),
                )
                Text(
                    stringResource(R.string.dv2_result_added), color = p.secondary, fontSize = 14.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            val described = stringResource(R.string.dv2_result_cd, name, grade)
            // 무대에는 등급 태그(grade-stage-…)가 따로 붙는다 — 이 화면의 이름표는 감싸는 칸에
            Box(
                Modifier.padding(top = 18.dp, bottom = 12.dp).widthIn(max = 340.dp).fillMaxWidth()
                    .semantics { contentDescription = described }.testTag("draw-result-stage"),
            ) {
                SneakerGradeStage(result.shoe, Modifier.fillMaxWidth(), animate = true)
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = StepUpDesign.Gutter).padding(bottom = 12.dp)) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).semantics(mergeDescendants = true) {}.testTag("draw-result-left"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(if (result.kind == DrawKind.FREE) R.string.dv2_result_free_left else R.string.dv2_result_premium_left),
                    color = p.secondary, fontSize = 15.sp, modifier = Modifier.weight(1f),
                )
                // 뽑은 뒤 서버에서 다시 읽은 수만 — 아직 못 읽었으면 "확인 중"(0 으로 보이지 않는다)
                Text(
                    result.left?.let { stringResource(R.string.dv2_count, it) } ?: stringResource(R.string.dv2_pending_value),
                    color = if (result.left == null) p.secondary else p.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(10.dp))
            DrawButton(stringResource(R.string.dv2_open_shoes), onOpenShoes, Modifier.testTag("draw-result-shoes"))
            if (canDrawAgain) {
                DrawLink(
                    stringResource(R.string.dv2_draw_again), onClick = onDrawAgain, fontSize = 15f,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp).testTag("draw-result-again"),
                )
            } else {
                DrawLink(
                    stringResource(R.string.dv2_to_draw), onClick = onClose, fontSize = 15f,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp).testTag("draw-result-home"),
                )
            }
        }
    }
}

/** 20 — 답을 받지 못해 처리 여부를 모른다. 새로 뽑지 않고 서버 목록 · 현황만 다시 읽는다 */
@Composable
internal fun DrawCheckScreen(checking: DrawFlow.Checking, onCheck: () -> Unit, onBack: () -> Unit) {
    val p = drawPalette()
    Column(Modifier.fillMaxSize().testTag("draw-checking")) {
        SecondaryHeader(
            onBack = onBack, balance = null, onOpenWallet = null, title = stringResource(R.string.dv2_check_header),
            modifier = Modifier.padding(horizontal = StepUpDesign.Gutter),
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = StepUpDesign.Gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FlowHeadline(stringResource(R.string.dv2_check_head), stringResource(R.string.dv2_check_sub), Modifier.padding(top = 20.dp))
            UnboxingStage(shoe = null, modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(top = 12.dp))
            Text(
                stringResource(if (checking.tried && !checking.busy) R.string.dv2_check_unknown else R.string.dv2_check_note),
                color = p.secondary, fontSize = 14.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp).semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag("draw-check-note"),
            )
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = StepUpDesign.Gutter).padding(bottom = 12.dp)) {
            DrawButton(
                stringResource(R.string.dv2_check_again), onCheck, Modifier.testTag("draw-check-again"),
                enabled = !checking.busy, loading = checking.busy,
            )
            DrawLink(
                stringResource(R.string.dv2_to_draw), onClick = onBack, fontSize = 15f,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp).testTag("draw-check-home"),
            )
        }
    }
}

private fun Rarity.revealCue(): FeedbackCue = when (this) {
    Rarity.COMMON -> FeedbackCue.DrawCommon
    Rarity.RARE -> FeedbackCue.DrawRare
    Rarity.EPIC -> FeedbackCue.DrawEpic
    Rarity.LEGENDARY -> FeedbackCue.DrawLegendary
}
