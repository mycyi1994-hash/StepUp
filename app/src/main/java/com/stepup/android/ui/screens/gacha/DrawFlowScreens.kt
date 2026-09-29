package com.stepup.android.ui.screens.gacha

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.DrawKind
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.tier
import com.stepup.android.ui.components.SecondaryHeader
import com.stepup.android.ui.components.ShoeNameWithBadge
import com.stepup.android.ui.components.SneakerGradeStage
import com.stepup.android.ui.components.label
import com.stepup.android.ui.components.variantLabel
import com.stepup.android.ui.screens.items.shoeName
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.LocalFeedback
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpDesign

/*
 * 뽑기를 누른 뒤의 장면 — 결과 확인 중(04) → 상자 열기(05) → 결과(06 · 07 · 08), 답을 받지 못했을 때의 결과 확인 지연(19).
 * 로고 · 잔액 머리는 앱 셸이 그대로 두고(하단 탭만 걷는다), 그 아래에 이 장면의 제목 줄을 둔다. 처리 중 · 결과 불확실 상태에는
 * 새 뽑기 버튼을 두지 않는다. 뒤로(와 기기 뒤로)는 04 · 19 에서 요청을 그대로 둔 채 메인으로(20), 05 에서는 확정된 결과로,
 * 결과에서는 메인으로 간다.
 */

/** 장면의 큰 제목(가운데) · 한 줄 — 화면 읽기가 바뀐 상태를 알린다 */
@Composable
private fun FlowHeadline(title: String, body: String?, modifier: Modifier = Modifier) {
    val p = drawPalette()
    Column(modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
        Text(
            title, color = p.text, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 34.sp,
            modifier = Modifier.fillMaxWidth().semantics { heading() },
        )
        if (body != null) {
            Text(
                body, color = p.secondary, fontSize = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}

/** 결과 · 상자 열기의 제목 줄 — 왼쪽 제목(24sp), 오른쪽 한 행동(✕ 또는 건너뛰기). 뒤로 가기 머리는 셸 공통 부품을 쓴다 */
@Composable
private fun FlowTitle(title: String, trailing: @Composable () -> Unit) {
    val p = drawPalette()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = StepUpDesign.Gutter, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title, color = p.text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        trailing()
    }
}

/** 04 · 19 의 머리 — 공통 뒤로 · 왼쪽 제목 "뽑기 결과 확인" */
@Composable
private fun CheckTitle(onBack: () -> Unit) {
    val p = drawPalette()
    SecondaryHeader(
        onBack = onBack, balance = null, onOpenWallet = null,
        modifier = Modifier.padding(horizontal = 8.dp),
        titleContent = {
            Text(stringResource(R.string.dv2_check_header), color = p.text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp)
        },
    )
}

/** 닫힌 상자 — 뒤의 푸른 빛. [clock] 이면 오른쪽 아래 시계(결과 확인 지연) */
@Composable
private fun ClosedBox(kind: DrawKind, clock: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.widthIn(max = 320.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width * 0.55f
            drawCircle(Brush.radialGradient(listOf(Color(0x733750BE), Color.Transparent), center, radius), radius, center)
        }
        Image(
            painterResource(if (kind == DrawKind.PREMIUM) R.drawable.draw_box_closed_premium else R.drawable.draw_box_closed_free),
            contentDescription = stringResource(R.string.draw_box_description),
            contentScale = ContentScale.Fit,
            modifier = Modifier.padding(vertical = 36.dp).widthIn(max = 260.dp).fillMaxWidth().heightIn(min = 60.dp),
        )
        if (clock) {
            Box(
                Modifier.align(Alignment.BottomEnd).padding(end = 34.dp, bottom = 34.dp).size(54.dp).clip(CircleShape)
                    .background(Color(0xFF1B2750)).border(2.dp, Color(0xFF8FA3F5), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Color(0xFFDDE4FF), modifier = Modifier.size(30.dp))
            }
        }
    }
}

/** 막대 여덟 개가 도는 표시 — 동작 줄이기면 멈춘 채 */
@Composable
private fun DrawBarSpinner(modifier: Modifier = Modifier) {
    val reduced = LocalMotion.current.reduced
    val turn = if (reduced) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "drawSpinner")
        val value by transition.animateFloat(0f, 8f, infiniteRepeatable(tween(960, easing = LinearEasing), RepeatMode.Restart), label = "turn")
        value
    }
    Canvas(modifier.size(34.dp)) {
        val lead = turn.toInt()
        for (i in 0 until 8) {
            val alpha = 0.25f + 0.75f * (((i - lead + 8) % 8) / 7f)
            rotate(i * 45f) {
                drawLine(
                    Color(0xFF9FB3FF).copy(alpha = alpha),
                    Offset(size.width / 2f, size.height * 0.08f), Offset(size.width / 2f, size.height * 0.36f),
                    strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round,
                )
            }
        }
    }
}

/** 04 — 서버가 결과를 확인할 때까지. 버튼이 없다(다시 누를 수 없다) */
@Composable
internal fun DrawRequestScreen(kind: DrawKind, onBack: () -> Unit) {
    val feedback = LocalFeedback.current
    LaunchedEffect(kind) { feedback?.play(FeedbackCue.DrawCharge) }
    Column(Modifier.fillMaxSize().testTag("draw-requesting")) {
        CheckTitle(onBack)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = StepUpDesign.Gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            ClosedBox(kind, clock = false)
            DrawBarSpinner(Modifier.padding(top = 4.dp))
            FlowHeadline(stringResource(R.string.dv2_check_head), stringResource(R.string.dv2_wait), Modifier.padding(top = 20.dp, bottom = 24.dp))
        }
    }
}

/** 05 — 결과를 받은 뒤에만. 약 2초(동작 줄이기면 0.2초), 건너뛰면 같은 결과로 */
@Composable
internal fun DrawOpeningScreen(result: DrawnShoe, onFinish: () -> Unit, frozenAt: Float? = null) {
    val p = drawPalette()
    val feedback = LocalFeedback.current
    LaunchedEffect(result.shoe.id) { feedback?.play(FeedbackCue.DrawBoxOpen) }
    Column(Modifier.fillMaxSize().testTag("draw-opening")) {
        FlowTitle(stringResource(R.string.dv2_title)) {
            Box(
                Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                    .feedbackClickable(role = Role.Button, onClick = onFinish).padding(horizontal = 12.dp)
                    .testTag("draw-skip"),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.dv2_skip), color = p.secondary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = StepUpDesign.Gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FlowHeadline(stringResource(R.string.dv3_opening_met), null, Modifier.padding(top = 16.dp))
            UnboxingStage(
                shoe = result.shoe, kind = result.kind, play = true, onFinished = onFinish, frozenAt = frozenAt,
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(top = 8.dp, bottom = 24.dp).testTag("draw-opening-stage"),
            )
        }
    }
}

/**
 * 06 무료 결과 · 07 상급 결과 · 08 마지막 기회 — 서버가 준 신발(등급 프레임 무대)과 이름 끝의 작은 둥근 등급 배지, 뽑은 뒤 서버가 준
 * 남은 수. "내 신발 보기"는 받은 신발을 고른 채로 신발 탭을 연다(착용은 그대로). "한 번 더 뽑기"는 남은 수를 다시 확인한 새 요청이다
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
    val p = drawPalette()
    val feedback = LocalFeedback.current
    LaunchedEffect(result.shoe.id) { feedback?.play(result.shoe.rarity.revealCue()) }
    val name = result.shoe.variantLabel()
    val grade = result.shoe.rarity.label()
    Column(Modifier.fillMaxSize().testTag("draw-result")) {
        FlowTitle(stringResource(if (result.kind == DrawKind.FREE) R.string.dv2_result_free else R.string.dv2_result_premium)) {
            val closeLabel = stringResource(R.string.dv2_close)
            Box(
                Modifier.size(48.dp).clip(CircleShape).feedbackClickable(role = Role.Button, onClick = onClose)
                    .semantics { contentDescription = closeLabel }.testTag("draw-result-close"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = p.text, modifier = Modifier.size(26.dp))
            }
        }
        // 신발 무대는 남은 높이만큼(150–286dp) — 작은 폰에서도 신발 · 이름 · 남은 수 · 버튼이 한 화면에. 모자라면 가운데만 넘긴다
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val viewport = constraints.maxHeight
            DrawFitLayout(
                viewport = viewport,
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag("draw-result-scroll")
                    .padding(horizontal = StepUpDesign.Gutter),
                topGap = 6.dp, stageGap = 14.dp, endGap = 0.dp, minStage = 150.dp, maxStage = ResultStageMax,
                top = { FlowHeadline(stringResource(R.string.dv3_result_head), null) },
                stage = {
                    val described = stringResource(R.string.dv2_result_cd, name, grade)
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        // 무대에는 등급 태그(grade-stage-…)가 따로 붙는다 — 이 화면의 이름표는 감싸는 칸에. 폭 300dp 까지, 칸 높이에 맞춘다
                        Box(Modifier.widthIn(max = 300.dp).semantics { contentDescription = described }.testTag("draw-result-stage")) {
                            SneakerGradeStage(result.shoe, animate = true)
                        }
                    }
                },
                bottom = {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 12.dp)
                            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ShoeNameWithBadge(
                            name = result.shoe.shoeName(), tier = result.shoe.tier,
                            style = TextStyle(color = p.text, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().testTag("draw-result-name"),
                        )
                        Text(
                            stringResource(R.string.dv2_result_added), color = p.secondary, fontSize = 15.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                },
            )
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = StepUpDesign.Gutter).padding(bottom = 14.dp)) {
            val shape = RoundedCornerShape(16.dp)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 58.dp).clip(shape).background(p.row).border(1.dp, p.rowEdge, shape)
                    .padding(horizontal = 18.dp).semantics(mergeDescendants = true) {}.testTag("draw-result-left"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(if (result.kind == DrawKind.FREE) R.string.dv2_result_free_left else R.string.dv2_result_premium_left),
                    color = p.secondary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
                )
                // 뽑은 뒤 서버에서 다시 읽은 수만 — 아직 못 읽었으면 "확인 중"(0 으로 보이지 않는다)
                Text(
                    result.left?.let { stringResource(R.string.dv2_count, it) } ?: stringResource(R.string.dv2_pending_value),
                    color = if (result.left == null) p.secondary else p.text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                )
            }
            Spacer(Modifier.height(14.dp))
            DrawButton(stringResource(R.string.dv2_open_shoes), onOpenShoes, Modifier.testTag("draw-result-shoes"))
            Spacer(Modifier.height(12.dp))
            if (canDrawAgain) {
                DrawButton(stringResource(R.string.dv2_draw_again), onDrawAgain, Modifier.testTag("draw-result-again"), DrawButtonStyle.Ghost)
            } else {
                DrawButton(stringResource(R.string.dv2_to_draw), onClose, Modifier.testTag("draw-result-home"), DrawButtonStyle.Ghost)
            }
        }
    }
}

/** 19 — 답을 받지 못해 처리 여부를 모른다. 새로 뽑지 않고 서버 목록 · 현황만 다시 읽는다 */
@Composable
internal fun DrawCheckScreen(checking: DrawFlow.Checking, onCheck: () -> Unit, onBack: () -> Unit) {
    val unknown = checking.tried && !checking.busy
    Column(Modifier.fillMaxSize().testTag("draw-checking")) {
        CheckTitle(onBack)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = StepUpDesign.Gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            ClosedBox(checking.kind, clock = true)
            FlowHeadline(
                stringResource(if (unknown) R.string.dv3_check_unknown_head else R.string.dv2_check_head),
                stringResource(R.string.dv2_check_note),
                Modifier.padding(top = 12.dp, bottom = 16.dp).testTag("draw-check-note"),
            )
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = StepUpDesign.Gutter).padding(bottom = 14.dp)) {
            DrawButton(
                stringResource(R.string.dv2_check_again), onCheck, Modifier.testTag("draw-check-again"),
                enabled = !checking.busy, loading = checking.busy,
            )
            Spacer(Modifier.height(12.dp))
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
