package com.stepup.android.ui.screens.gacha

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.DrawKind
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.tier
import com.stepup.android.ui.components.RunDivider
import com.stepup.android.ui.components.RunNumber
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.ShoeNameWithBadge
import com.stepup.android.ui.components.SneakerGradeStage
import com.stepup.android.ui.components.label
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.components.variantLabel
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.LocalFeedback
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.items.formatShoeNumber
import com.stepup.android.ui.screens.items.shoeName
import com.stepup.android.ui.theme.StepUpDesign

/*
 * 뽑기를 누른 뒤의 장면(파란 톤 v4) — 결과 확인 중(DRAW10) → 상자 열기(DRAW11) → 결과(DRAW12 · 13 · 14 · 33), 답을 받지 못했을 때의
 * 결과 확인 지연(DRAW20). 로고 · 잔액 머리는 앱 셸이 그대로 두고(하단 탭만 걷는다), 그 아래에 뒤로 · 가운데 제목 · (건너뛰기) 줄을 둔다.
 * 처리 중 · 결과 불확실 상태에는 새 뽑기 버튼을 두지 않는다. 뒤로(와 기기 뒤로)는 DRAW10 · 20 에서 요청을 그대로 둔 채 메인으로(DRAW26),
 * DRAW11 에서는 같은 확정 결과로, 결과에서는 메인으로 간다. 상자를 기다리는 시간과 서버 확인 시간을 섞지 않는다 — 상자는 서버가
 * 신발을 확정한 뒤에만 연다.
 */

/** DRAW10 — 서버가 결과를 확인할 때까지. 버튼이 없다(다시 누를 수 없다). 뒤로 → 요청을 둔 채 메인(DRAW26) */
@Composable
internal fun DrawRequestScreen(kind: DrawKind, onBack: () -> Unit) {
    val t = runTone()
    val feedback = LocalFeedback.current
    LaunchedEffect(kind) { feedback?.play(FeedbackCue.DrawCharge) }
    Column(Modifier.fillMaxSize().testTag("draw-requesting")) {
        DrawFlowBar(stringResource(R.string.dv2_check_header), onBack)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = StepUpDesign.Gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DrawFlowHeadline(
                stringResource(R.string.draw_blue_request_head), stringResource(R.string.draw_blue_request_sub),
                Modifier.padding(top = 20.dp).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            )
            DrawBoxStage(kind, Modifier.widthIn(max = 420.dp).fillMaxWidth().height(260.dp).padding(top = 16.dp), maxBoxWidth = 280.dp)
            RunSpinner(Modifier.padding(top = 18.dp).size(40.dp))
            Text(
                stringResource(R.string.dv2_wait), style = runTextStyle(15.sp, t.label, FontWeight.Medium),
                modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
            )
        }
    }
}

/** DRAW11 — 결과를 받은 뒤에만. 약 2초(동작 줄이기면 0.2초), 건너뛰기 · 뒤로는 같은 확정 결과로 */
@Composable
internal fun DrawOpeningScreen(result: DrawnShoe, onFinish: () -> Unit, frozenAt: Float? = null) {
    val t = runTone()
    val feedback = LocalFeedback.current
    LaunchedEffect(result.shoe.id) { feedback?.play(FeedbackCue.DrawBoxOpen) }
    Column(Modifier.fillMaxSize().testTag("draw-opening")) {
        DrawFlowBar(
            stringResource(if (result.kind == DrawKind.FREE) R.string.dv2_free else R.string.dv2_premium), onBack = onFinish,
            trailing = {
                Box(
                    Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                        .feedbackClickable(role = Role.Button, onClick = onFinish).padding(horizontal = 12.dp)
                        .testTag("draw-skip"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.dv2_skip), style = runTextStyle(15.sp, t.label, FontWeight.SemiBold))
                }
            },
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = StepUpDesign.Gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DrawFlowHeadline(stringResource(R.string.draw_blue_opening_head), null, Modifier.padding(top = 16.dp))
            UnboxingStage(
                shoe = result.shoe, kind = result.kind, play = true, onFinished = onFinish, frozenAt = frozenAt,
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(top = 8.dp).testTag("draw-opening-stage"),
            )
            Text(
                stringResource(R.string.draw_blue_opening_note), style = runTextStyle(15.sp, t.label, FontWeight.Medium),
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
        }
    }
}

/**
 * DRAW12 무료 결과 · DRAW13 상급 결과 · DRAW14 마지막 기회 · DRAW33 남은 수 확인 중 — 서버가 준 신발(등급 프레임 무대)과 이름 끝의
 * 등급 배지(서버 등급 그대로 — 특정 등급 보장 색 · 문구 없음), Lv · 번호(그 소유 신발의 값), 뽑은 뒤 서버가 준 남은 수.
 * "내 신발 보기"는 받은 소유 신발을 고른 채로 신발 탭을 연다(착용은 그대로). "한 번 더 뽑기"는 남은 수를 다시 확인한 새 요청이다
 * — 마지막 기회였거나 수를 아직 모르면 "뽑기 화면으로".
 */
@Composable
internal fun DrawResultScreen(
    result: DrawnShoe,
    canDrawAgain: Boolean,
    onOpenShoes: () -> Unit,
    onDrawAgain: () -> Unit,
    onClose: () -> Unit,
) {
    val t = runTone()
    val feedback = LocalFeedback.current
    LaunchedEffect(result.shoe.id) { feedback?.play(result.shoe.rarity.revealCue()) }
    val name = result.shoe.variantLabel()
    // 읽기 도구가 읽는 등급 — 이름 끝 배지와 같은 이름(레드라인 · 피니시 포함)
    val grade = result.shoe.tier.label()
    Column(Modifier.fillMaxSize().testTag("draw-result")) {
        DrawFlowBar(
            stringResource(if (result.kind == DrawKind.FREE) R.string.dv2_result_free else R.string.dv2_result_premium),
            onBack = onClose, backTag = "draw-result-close",
        )
        // 신발 무대는 남은 높이만큼(150–286dp) — 작은 폰에서도 이름 · 신발 · 남은 수 · 버튼이 한 화면에. 모자라면 가운데만 넘긴다
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val viewport = constraints.maxHeight
            DrawFitLayout(
                viewport = viewport,
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag("draw-result-scroll")
                    .padding(horizontal = StepUpDesign.Gutter),
                topGap = 4.dp, stageGap = 14.dp, endGap = 8.dp, minStage = 150.dp, maxStage = ResultStageMax,
                top = {
                    Column(
                        Modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ShoeNameWithBadge(
                            name = result.shoe.shoeName(), tier = result.shoe.tier,
                            style = drawTitleStyle(27.sp, t.text),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().testTag("draw-result-name"),
                        )
                        Text(
                            stringResource(R.string.draw_blue_level_number, result.shoe.level, formatShoeNumber(result.shoe.mintNumber)),
                            style = runTextStyle(16.sp, t.label, FontWeight.SemiBold), textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp).testTag("draw-result-number"),
                        )
                        Text(
                            stringResource(R.string.dv2_result_added), style = runTextStyle(15.sp, t.label, FontWeight.Medium),
                            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                },
                stage = {
                    val described = stringResource(R.string.dv2_result_cd, name, grade)
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        // 무대에는 등급 태그(grade-stage-…)가 따로 붙는다 — 이 화면의 이름표는 감싸는 칸에. 폭 300dp 까지, 칸 높이에 맞춘다
                        Box(Modifier.widthIn(max = 300.dp).semantics { contentDescription = described }.testTag("draw-result-stage")) {
                            SneakerGradeStage(result.shoe, animate = true, pedestal = true)
                        }
                    }
                },
                bottom = { Spacer(Modifier.height(0.dp)) },
            )
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = StepUpDesign.Gutter).padding(bottom = 10.dp)) {
            RunDivider()
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).semantics(mergeDescendants = true) {}.testTag("draw-result-left"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(if (result.kind == DrawKind.FREE) R.string.dv2_result_free_left else R.string.dv2_result_premium_left),
                    style = runTextStyle(16.sp, t.label, FontWeight.SemiBold), modifier = Modifier.weight(1f),
                )
                // 뽑은 뒤 서버에서 다시 읽은 수만 — 아직 못 읽었으면 "확인 중"(DRAW33, 0 으로 보이지 않는다)
                val left = result.left
                if (left != null) {
                    RunNumber(stringResource(R.string.dv2_count, left), size = 24.sp)
                } else {
                    Text(stringResource(R.string.dv2_pending_value), style = runTextStyle(20.sp, t.cyan, FontWeight.ExtraBold))
                }
            }
            Spacer(Modifier.height(8.dp))
            DrawButton(stringResource(R.string.dv2_open_shoes), onOpenShoes, Modifier.testTag("draw-result-shoes"))
            if (canDrawAgain) {
                DrawButton(stringResource(R.string.dv2_draw_again), onDrawAgain, Modifier.testTag("draw-result-again"), DrawButtonStyle.Ghost)
            } else {
                DrawButton(stringResource(R.string.dv2_to_draw), onClose, Modifier.testTag("draw-result-home"), DrawButtonStyle.Ghost)
            }
        }
    }
}

/**
 * DRAW20 — 답을 받지 못해 처리 여부를 모른다. "결과 다시 확인"은 새로 뽑지 않고 같은 요청의 서버 목록 · 현황만 다시 읽는다.
 * 읽는 동안은 버튼을 누를 수 없고, 다 읽었는데도 모르면 다시 누를 수 있다. "뽑기 화면으로" · 뒤로 → 요청을 둔 채 메인(DRAW26).
 */
@Composable
internal fun DrawCheckScreen(checking: DrawFlow.Checking, onCheck: () -> Unit, onBack: () -> Unit) {
    val t = runTone()
    val unknown = checking.tried && !checking.busy
    Column(Modifier.fillMaxSize().testTag("draw-checking")) {
        DrawFlowBar(stringResource(R.string.dv2_check_header), onBack)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = StepUpDesign.Gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DrawFlowHeadline(
                stringResource(if (unknown) R.string.dv3_check_unknown_head else R.string.dv2_check_head),
                stringResource(R.string.draw_blue_check_sub),
                Modifier.padding(top = 20.dp).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            )
            Box(Modifier.heightIn(min = 52.dp).padding(top = 12.dp), contentAlignment = Alignment.Center) {
                if (checking.busy) RunSpinner(Modifier.size(40.dp))
            }
            DrawBoxStage(checking.kind, Modifier.widthIn(max = 420.dp).fillMaxWidth().height(230.dp), maxBoxWidth = 270.dp)
            Text(
                stringResource(R.string.dv2_check_note), style = runTextStyle(15.sp, t.label, FontWeight.Medium),
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp, bottom = 16.dp).testTag("draw-check-note"),
            )
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = StepUpDesign.Gutter).padding(bottom = 10.dp)) {
            DrawButton(
                stringResource(R.string.dv2_check_again), onCheck, Modifier.testTag("draw-check-again"),
                enabled = !checking.busy, loading = checking.busy,
            )
            DrawButton(stringResource(R.string.dv2_to_draw), onBack, Modifier.testTag("draw-check-home"), DrawButtonStyle.Ghost)
        }
    }
}

/** 결과 무대의 가장 큰 높이 — 폭 300dp 무대(440:418) */
private val ResultStageMax = 286.dp

private fun Rarity.revealCue(): FeedbackCue = when (this) {
    Rarity.COMMON -> FeedbackCue.DrawCommon
    Rarity.RARE -> FeedbackCue.DrawRare
    Rarity.EPIC -> FeedbackCue.DrawEpic
    Rarity.LEGENDARY -> FeedbackCue.DrawLegendary
}
