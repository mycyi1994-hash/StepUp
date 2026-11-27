package com.stepup.android.ui.screens.gacha

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
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
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.tier
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.ShareCardOption
import com.stepup.android.ui.components.ShareCards
import com.stepup.android.ui.components.StepUpShareSheet
import com.stepup.android.ui.components.V12ShoeCard
import com.stepup.android.ui.components.label
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.components.sneakerArtRes
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.LocalFeedback
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.items.shoeName
import com.stepup.android.ui.screens.walk.shareFace
import com.stepup.android.ui.theme.StepUpDesign
import kotlinx.coroutines.delay

/*
 * 13차 여러 번 뽑기 — 뽑는 중(S-408) · 결과(S-364) · 결과 공유(S-409), 한 번 뽑기 결과 공유(S-406 · S-407).
 * 묶어 보내는 서버 함수는 없다 — 한 번 뽑기를 차례로 보내고(DrawViewModel.drawMany) 서버가 준 신발만 뒤집는다.
 * 뒤집기는 서버가 답한 뒤에만(지어낸 결과를 먼저 보이지 않는다). 작은 카드는 0.12초 간격 · 하나 0.48초, 다 받으면 큰 카드를 마지막으로.
 * 동작 줄이기면 뒤집지 않고 바로 앞면.
 */

/** 작은 카드 뒤집기 시간 · 간격 · 큰 카드 · 다 뒤집은 뒤 결과로 넘어가기까지(밀리초) */
private const val FLIP_MS = 480
private const val FLIP_GAP_MS = 120L
private const val BIG_FLIP_MS = 900
private const val AFTER_MS = 700L

/** 결과에서 가장 높은 등급(같으면 먼저 뽑은 것) */
internal fun List<DrawnShoe>.best(): DrawnShoe? =
    fold(null as DrawnShoe?) { b, d -> if (b == null || d.shoe.rarity.ordinal > b.shoe.rarity.ordinal) d else b }

/** 등급별 수 — 높은 등급부터 */
internal fun List<DrawnShoe>.tierCounts(): List<Pair<com.stepup.android.domain.ShoeTier, Int>> =
    groupingBy { it.shoe.tier }.eachCount().toList().sortedByDescending { it.first.ordinal }

/** S-408 — 큰 카드 한 장(뒷면) · 아래 작은 카드 [DrawFlow.Batch.total] 장. 서버가 준 만큼 차례로 뒤집는다 */
@Composable
internal fun DrawBatchScreen(batch: DrawFlow.Batch, onBack: () -> Unit, onDone: () -> Unit) {
    val t = runTone()
    val feedback = LocalFeedback.current
    val reduced = LocalMotion.current.reduced
    LaunchedEffect(Unit) { feedback?.play(FeedbackCue.DrawCharge) }
    val best = batch.got.best()
    val bigTurn = remember { Animatable(0f) }
    LaunchedEffect(batch.done, best?.shoe?.id) {
        if (!batch.done || best == null) return@LaunchedEffect
        // 작은 카드가 다 뒤집힐 틈을 준 뒤 큰 카드
        if (!reduced) delay(FLIP_MS + FLIP_GAP_MS)
        feedback?.play(FeedbackCue.DrawBoxOpen)
        if (reduced) bigTurn.snapTo(1f) else bigTurn.animateTo(1f, tween(BIG_FLIP_MS, easing = FastOutSlowInEasing))
        feedback?.play(best.shoe.rarity.batchCue())
        delay(if (reduced) 200L else AFTER_MS)
        onDone()
    }
    val progress = stringResource(R.string.v13_draw_drawing_cd, batch.got.size.coerceAtMost(batch.total), batch.total)
    Column(Modifier.fillMaxSize().testTag("draw-batch")) {
        DrawFlowBar(
            stringResource(R.string.v13_draw_drawing), onBack,
            trailing = if (batch.done) {
                {
                    Box(
                        Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                            .feedbackClickable(role = Role.Button, onClick = onDone).padding(horizontal = 12.dp)
                            .testTag("draw-batch-skip"),
                        contentAlignment = Alignment.Center,
                    ) { Text(stringResource(R.string.dv2_skip), style = runTextStyle(15.sp, t.label, FontWeight.SemiBold)) }
                }
            } else null,
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = StepUpDesign.Gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.padding(top = 16.dp).fillMaxWidth(.5f).widthIn(max = 220.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite; contentDescription = progress }
                    .testTag("draw-batch-big"),
            ) {
                FlipCard(turn = bigTurn.value, back = { CardBack(batch.kind, big = true) }) {
                    if (best != null) V12ShoeCard(best.shoe, hero = false)
                }
            }
            Spacer(Modifier.height(36.dp))
            BatchGrid(batch.total) { i ->
                val drawn = batch.got.getOrNull(i)
                val turn = remember { Animatable(0f) }
                LaunchedEffect(drawn?.shoe?.id) {
                    if (drawn == null) return@LaunchedEffect
                    if (reduced) turn.snapTo(1f) else {
                        delay(FLIP_GAP_MS * (i % 5))
                        turn.animateTo(1f, tween(FLIP_MS, easing = FastOutSlowInEasing))
                    }
                }
                Box(Modifier.testTag(if (drawn != null) "draw-batch-cell-$i-open" else "draw-batch-cell-$i")) {
                    FlipCard(turn = turn.value, back = { CardBack(batch.kind, big = false) }) {
                        if (drawn != null) V12ShoeCard(drawn.shoe, hero = false, mini = true)
                    }
                }
            }
        }
    }
}

/**
 * S-364 — 가장 높은 등급 한 장 크게 · 등급별 수 · 받은 신발 격자(누르면 그 신발을 고른 신발 탭). 아래 "한 번 더 N회" · "확인".
 * 중간에 멈췄으면 그 까닭 한 줄(나머지는 기회를 쓰지 않았다), 마지막 한 번을 모르면 확인 중 한 줄. 공유는 오른쪽 위.
 */
@Composable
internal fun DrawBatchResultScreen(
    result: DrawFlow.BatchResult,
    againCount: Int,
    onAgain: () -> Unit,
    onOpenShoe: (Long) -> Unit,
    onClose: () -> Unit,
) {
    val t = runTone()
    val best = result.got.best() ?: return
    var share by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().testTag("draw-batch-result")) {
        DrawFlowBar(
            stringResource(R.string.v13_draw_batch_title, result.got.size), onClose, backTag = "draw-result-close",
            trailing = { ShareIconButton({ share = true }, "draw-batch-share") },
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = StepUpDesign.Gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.padding(top = 8.dp).fillMaxWidth(.5f).widthIn(max = 220.dp)
                    .semantics(mergeDescendants = true) {}.testTag("draw-batch-best"),
            ) { V12ShoeCard(best.shoe, hero = false, onClick = { onOpenShoe(best.shoe.id) }) }
            Text(
                stringResource(R.string.v13_draw_best), style = runTextStyle(14.sp, t.label, FontWeight.SemiBold),
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 18.dp).testTag("draw-batch-counts"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                result.got.tierCounts().forEach { (tier, n) ->
                    Text(
                        stringResource(R.string.v13_draw_tier_count, tier.label(), n),
                        style = runTextStyle(13.sp, t.text, FontWeight.Bold),
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(t.panel).border(1.dp, t.panelEdge, RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            BatchGrid(result.got.size) { i ->
                val shoe = result.got[i].shoe
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.testTag("draw-batch-shoe-$i")) {
                    V12ShoeCard(
                        shoe, hero = false, mini = true, onClick = { onOpenShoe(shoe.id) },
                        clickLabel = stringResource(R.string.v13_draw_cell_cd, shoe.shoeName(), shoe.tier.label()),
                    )
                    Text(
                        shoe.shoeName(), style = runTextStyle(11.sp, t.text, FontWeight.SemiBold), maxLines = 1,
                        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            val note = when {
                result.unsure -> stringResource(R.string.v13_draw_unsure)
                result.stop != null -> stringResource(R.string.v13_draw_stopped, result.asked, result.got.size)
                else -> null
            }
            if (note != null) {
                Text(
                    note, style = runTextStyle(14.sp, t.label, FontWeight.Medium, 1.4f), textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp).testTag("draw-batch-note"),
                )
            }
            result.left?.let {
                Text(
                    stringResource(R.string.v13_draw_left, it), style = runTextStyle(14.sp, t.cyan, FontWeight.SemiBold),
                    modifier = Modifier.padding(top = 10.dp).testTag("draw-batch-left"),
                )
            }
            Spacer(Modifier.height(16.dp))
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = StepUpDesign.Gutter).padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (againCount >= 2) {
                Box(
                    Modifier.heightIn(min = 56.dp).clip(RoundedCornerShape(50)).background(t.panel)
                        .border(1.dp, t.panelEdge, RoundedCornerShape(50))
                        .feedbackClickable(role = Role.Button, onClick = onAgain).padding(horizontal = 20.dp)
                        .testTag("draw-batch-again"),
                    contentAlignment = Alignment.Center,
                ) { Text(stringResource(R.string.v13_draw_again_n, againCount), style = runTextStyle(16.sp, t.text, FontWeight.Bold)) }
            }
            RunButton(stringResource(R.string.v13_draw_ok), onClose, Modifier.weight(1f).testTag("draw-batch-ok"))
        }
    }
    if (share) {
        DrawBatchShareSheet(result, onDismiss = { share = false })
    }
}

/** 공유 아이콘(제목 줄 오른쪽) — 누르는 곳 48dp */
@Composable
internal fun ShareIconButton(onClick: () -> Unit, tag: String) {
    val t = runTone()
    val cd = stringResource(R.string.v13_draw_share)
    Box(
        Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).feedbackClickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = cd }.testTag(tag),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Filled.Share, contentDescription = null, tint = t.text, modifier = Modifier.size(22.dp)) }
}

/** S-407 — 새 신발 한 켤레 공유(카드 하나) */
@Composable
internal fun DrawShoeShareSheet(shoe: Sneaker, onDismiss: () -> Unit) {
    val face = shareFace(shoe)
    val line = stringResource(R.string.v13_draw_share_line, shoe.tier.label(), shoe.shoeName())
    val chip = stringResource(R.string.v13_draw_new_shoe)
    val options = listOf(
        ShareCardOption("shoe", chip) { ctx -> ShareCards.newShoe(ctx, face.copy(art = ShareCards.shoeArt(ctx, sneakerArtRes(shoe))), line) },
    )
    StepUpShareSheet(
        options = options, text = stringResource(R.string.v13_draw_share_new, shoe.shoeName()),
        fileName = "StepUp-shoe-${shoe.id}", onDismiss = onDismiss,
    )
}

/** S-409 — 여러 번 뽑기 결과 공유(가장 높은 등급 · 등급별 수) */
@Composable
private fun DrawBatchShareSheet(result: DrawFlow.BatchResult, onDismiss: () -> Unit) {
    val best = result.got.best() ?: return
    val face = shareFace(best.shoe)
    val parts = result.got.tierCounts().map { (tier, n) -> stringResource(R.string.v13_draw_tier_count, tier.label(), n) }
    val summary = parts.joinToString(" · ")
    val title = stringResource(R.string.v13_draw_batch_title, result.got.size)
    val chip = stringResource(R.string.v13_draw_best)
    val options = listOf(
        ShareCardOption("ten", chip) { ctx ->
            ShareCards.drawTen(ctx, face.copy(art = ShareCards.shoeArt(ctx, sneakerArtRes(best.shoe))), summary, title)
        },
    )
    StepUpShareSheet(
        options = options, text = stringResource(R.string.v13_draw_share_ten, result.got.size, best.shoe.tier.label()),
        fileName = "StepUp-draw-${best.shoe.id}", onDismiss = onDismiss,
    )
}

// ── 그림 ─────────────────────────────────────────────────────────

/** 다섯 칸씩 줄 */
@Composable
private fun BatchGrid(count: Int, cell: @Composable (Int) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        (0 until count).chunked(5).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { i -> Box(Modifier.weight(1f)) { cell(i) } }
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** 0 → 1 로 돌며 반을 넘으면 앞면. 앞면이 아직 없으면(서버가 답하기 전) 뒷면 그대로 */
@Composable
private fun FlipCard(turn: Float, back: @Composable () -> Unit, front: @Composable () -> Unit) {
    val showFront = turn >= 0.5f
    val angle = if (showFront) (1f - turn) * -180f else turn * 180f
    Box(
        Modifier.graphicsLayer {
            rotationY = angle
            cameraDistance = 12f * density
        },
    ) { if (showFront) front() else back() }
}

/** 카드 뒷면 — 남색 사선 무늬 · 가운데 STEPUP. 상급은 금색 테, 무료는 파란 테 */
@Composable
private fun CardBack(kind: DrawKind, big: Boolean) {
    val rim = if (kind == DrawKind.PREMIUM) Color(0xFFE7B53C) else Color(0xFF5B8BFF)
    val shape = RoundedCornerShape(if (big) 18.dp else 10.dp)
    Box(
        Modifier.fillMaxWidth().aspectRatio(com.stepup.android.ui.components.V12ShoeCardRatio).clip(shape)
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF1B2C5C), Color(0xFF101A3A))))
                val step = size.width / 6f
                var x = -size.height
                while (x < size.width) {
                    drawLine(Color.White.copy(alpha = .05f), Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = step / 2.4f)
                    x += step
                }
            }
            .border(if (big) 3.dp else 1.5.dp, rim, shape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            com.stepup.android.ui.components.cachedPainterResource(R.drawable.logo_wordmark_white), contentDescription = null,
            contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth(.66f).aspectRatio(5f),
        )
    }
}

private fun com.stepup.android.domain.Rarity.batchCue(): FeedbackCue = when (this) {
    com.stepup.android.domain.Rarity.COMMON -> FeedbackCue.DrawCommon
    com.stepup.android.domain.Rarity.RARE -> FeedbackCue.DrawRare
    com.stepup.android.domain.Rarity.EPIC -> FeedbackCue.DrawEpic
    else -> FeedbackCue.DrawLegendary
}
