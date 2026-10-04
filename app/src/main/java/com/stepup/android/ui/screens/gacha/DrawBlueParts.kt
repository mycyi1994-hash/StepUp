package com.stepup.android.ui.screens.gacha

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.stepup.android.R
import com.stepup.android.domain.DrawKind
import com.stepup.android.ui.components.RunSpec
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.LocalFeedback
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpSans

/*
 * 신발 뽑기 파란 톤(2026-10-03 통합 전달본 v4 · stepup-draw-blue-claude-v19) 공통 조각 — 남색 패널 · 상자 무대(조명 · 받침) ·
 * 안내창 틀 · 흐름 화면의 제목 줄. 색은 러닝 리메이크의 runTone()(밝은 테마 값 포함)만 쓴다. 상자 그림은 기존 앱 자산
 * (draw_box_closed_free · draw_box_closed_premium) — 시안의 흰색 · 코발트 새 상자는 아직 분리 자산이 없다(04-자산과-구현범위.md).
 */

/** 제목 글꼴 — 굵고 기울인 짧은 제목(신발 뽑기 · 무료 뽑기 · 안내창 제목). 긴 본문은 정자로 둔다 */
internal fun drawTitleStyle(size: TextUnit, color: Color): TextStyle = TextStyle(
    fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic,
    fontSize = size, lineHeight = 1.2.em, letterSpacing = (-0.02).em, color = color,
)

/** 남은 수 · "확인 중" — 시안의 시안색(#48D9FA) 큰 기울인 글자. 위가 밝다(어두운 테마) */
@Composable
internal fun drawCountBrush(): Brush {
    val t = runTone()
    return if (t.dark) Brush.verticalGradient(listOf(Color(0xFFB8F1FF), t.cyan)) else Brush.verticalGradient(listOf(t.cobalt, t.cyan))
}

/** 패널 모양 — 위 두 모서리를 깎고(시안의 조명 틀) 아래는 둥근 판 */
private class DrawPanelShape(private val cut: Dp, private val radius: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(panelPath(size, with(density) { cut.toPx() }, with(density) { radius.toPx() }))
}

private fun panelPath(size: Size, cut: Float, radius: Float): Path = Path().apply {
    val w = size.width
    val h = size.height
    val c = cut.coerceAtMost(minOf(w, h) / 4f)
    val r = radius.coerceAtMost(minOf(w, h) / 4f)
    moveTo(c, 0f); lineTo(w - c, 0f); lineTo(w, c); lineTo(w, h - r)
    quadraticTo(w, h, w - r, h); lineTo(r, h); quadraticTo(0f, h, 0f, h - r)
    lineTo(0f, c); close()
}

private val PanelCut = 14.dp
private val PanelRadius = 16.dp

/**
 * 뽑기 패널 — 남색 면 · 파란 둘레 · 위 모서리를 깎은 판, 위 왼쪽(상자 위)에 짧은 조명 막대. 안의 빛은 상자 자리에만 둔다
 * ([DrawBoxStage] 가 그린다). 누르는 곳이 아니다 — 행동은 안의 버튼 · 글자 링크만.
 */
@Composable
internal fun DrawPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val t = runTone()
    val shape = remember { DrawPanelShape(PanelCut, PanelRadius) }
    val light = if (t.dark) Color(0xFF7FD8FF) else t.cobalt
    Column(
        modifier.fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(t.panelTop, t.panel)))
            .drawWithContent {
                drawContent()
                drawPath(panelPath(size, PanelCut.toPx(), PanelRadius.toPx()), t.panelEdge, style = Stroke(width = 1.5.dp.toPx()))
                // 위 조명 막대 — 상자 위쪽에 얇게(패널 폭의 4할)
                drawRoundRect(
                    light.copy(alpha = 0.9f), topLeft = Offset(size.width * 0.08f, 3.dp.toPx()),
                    size = Size(size.width * 0.42f, 3.dp.toPx()), cornerRadius = CornerRadius(2.dp.toPx()),
                )
            }
            .padding(start = 12.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
        content = content,
    )
}

/**
 * 상자 무대 — 위에서 내려오는 조명, 둥근 받침, 그 위 닫힌 상자(기존 앱 자산). [dim] 이면(불러오기 실패 · 안내창 뒤) 흐리게.
 * 빛은 이 칸 안에만 그린다 — 글자 위로 광원이 겹치지 않는다.
 */
@Composable
internal fun DrawBoxStage(kind: DrawKind, modifier: Modifier = Modifier, dim: Boolean = false, maxBoxWidth: Dp = 260.dp) {
    val t = runTone()
    BoxWithConstraints(modifier, contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            // 위에서 내려오는 조명 — 받침 쪽으로 넓어지는 옅은 빛(가운데)
            val beam = Path().apply {
                moveTo(w * 0.32f, 0f); lineTo(w * 0.68f, 0f); lineTo(w * 0.94f, h * 0.86f); lineTo(w * 0.06f, h * 0.86f); close()
            }
            drawPath(
                beam,
                Brush.verticalGradient(
                    listOf(t.cobalt.copy(alpha = if (dim) 0.10f else if (t.dark) 0.34f else 0.16f), Color.Transparent),
                    startY = 0f, endY = h * 0.86f,
                ),
            )
            // 받침 — 납작한 원판 둘(아래 그림자 · 위 면)과 시안 테두리
            val cy = h * 0.86f
            val rw = w * 0.46f
            val rh = h * 0.08f
            drawOval(Color.Black.copy(alpha = if (t.dark) 0.35f else 0.12f), Offset(w / 2 - rw, cy - rh + h * 0.03f), Size(rw * 2, rh * 2))
            drawOval(
                Brush.radialGradient(
                    listOf((if (t.dark) Color(0xFF0B3A86) else Color(0xFFD3E2FF)), (if (t.dark) Color(0xFF06204A) else Color(0xFFEAF1FF))),
                    center = Offset(w / 2, cy), radius = rw,
                ),
                Offset(w / 2 - rw, cy - rh), Size(rw * 2, rh * 2),
            )
            drawOval(
                (if (dim) t.panelEdge else t.cyan).copy(alpha = if (dim) 0.5f else 0.85f), Offset(w / 2 - rw, cy - rh), Size(rw * 2, rh * 2),
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }
        val boxWidth = minOf(maxBoxWidth, maxWidth * 0.86f)
        val matrix = remember(dim) {
            if (dim) {
                ColorFilter.colorMatrix(
                    ColorMatrix().apply {
                        setToSaturation(0.55f)
                        timesAssign(ColorMatrix().apply { setToScale(0.55f, 0.55f, 0.6f, 1f) })
                    },
                )
            } else {
                null
            }
        }
        Image(
            com.stepup.android.ui.components.cachedPainterResource(if (kind == DrawKind.PREMIUM) R.drawable.draw_box_closed_premium else R.drawable.draw_box_closed_free),
            contentDescription = stringResource(R.string.draw_box_description),
            contentScale = ContentScale.Fit, colorFilter = matrix,
            modifier = Modifier.padding(bottom = maxHeight * 0.1f).width(boxWidth).heightIn(max = maxHeight * 0.86f),
        )
    }
}

/** 모르는 수 — 짧은 줄 하나("—"). 0 으로 보이지 않는다 */
@Composable
internal fun DrawUnknownValue(tag: String, modifier: Modifier = Modifier) {
    val t = runTone()
    Box(
        modifier.heightIn(min = 40.dp).widthIn(min = 80.dp).clip(RoundedCornerShape(12.dp)).background(t.chipFace)
            .padding(horizontal = 18.dp).testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(DASH, style = runTextStyle(22.sp, t.cyan, FontWeight.Bold, 1.1f))
    }
}

/** 모르는 수 — 0 으로 보이지 않는다 */
internal const val DASH = "—"

// ── 안내창 ───────────────────────────────────────────────────────

/**
 * 뽑기 안내창 — 아래에서 올라오는 남색 판(불투명) · 손잡이 · 오른쪽 위 ✕ · 기울인 굵은 제목 · 한 줄 설명. 뒤는 헤더까지 검정 약 60% 로
 * 덮고(따로 뜬 창이라 뒤 화면 버튼 · 하단 탭은 누를 수도, 읽기 초점을 받을 수도 없다), 높이가 모자라면 본문만 넘기고 실행 버튼은
 * 늘 보인다. ✕ · 바깥 · 뒤로는 [onDismiss](창만 닫는다 — 기회를 쓰지 않는다).
 */
@Composable
internal fun DrawSheetFrame(
    title: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val t = runTone()
    val feedback = LocalFeedback.current
    LaunchedEffect(feedback) { feedback?.play(FeedbackCue.SheetOpen) }
    val dismiss = {
        feedback?.play(FeedbackCue.SheetClose)
        onDismiss()
    }
    Dialog(
        onDismissRequest = dismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            // 뒤 화면 딤은 창이 깐다(설정 안내창과 같다) — 여기서는 바깥 누르기만 받는다
            Box(Modifier.matchParentSize().pointerInput(Unit) { detectTapGestures { dismiss() } })
            val shape = RoundedCornerShape(topStart = RunSpec.SheetRadius, topEnd = RunSpec.SheetRadius)
            Column(
                modifier.widthIn(max = 600.dp).fillMaxWidth().statusBarsPadding().heightIn(max = maxHeight - 24.dp)
                    .clip(shape).background(Brush.verticalGradient(listOf(t.sheetTop, t.sheet)))
                    .border(1.dp, t.sheetEdge, shape)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .navigationBarsPadding(),
            ) {
                Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(width = 44.dp, height = 5.dp).clip(RoundedCornerShape(3.dp)).background(t.handle))
                }
                Row(
                    Modifier.fillMaxWidth().padding(start = RunSpec.Gutter, end = 8.dp, top = 6.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(Modifier.weight(1f).padding(top = 12.dp)) {
                        if (title != null) {
                            Text(title, style = drawTitleStyle(26.sp, t.text), modifier = Modifier.semantics { heading() })
                        }
                        if (subtitle != null) {
                            Text(
                                subtitle, style = runTextStyle(15.sp, t.label, FontWeight.Medium, 1.45f),
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                    val closeLabel = stringResource(R.string.common_close)
                    Box(
                        Modifier.size(48.dp).clip(CircleShape)
                            .feedbackClickable(cue = FeedbackCue.Back, onClick = dismiss)
                            .semantics { contentDescription = closeLabel }
                            .testTag("draw-sheet-x"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, tint = t.text, modifier = Modifier.size(26.dp))
                    }
                }
                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                        .padding(horizontal = RunSpec.Gutter).padding(top = 12.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = content,
                )
                if (actions != null) {
                    Column(
                        Modifier.fillMaxWidth().padding(start = RunSpec.Gutter, end = RunSpec.Gutter, top = 14.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        content = actions,
                    )
                } else {
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

// ── 흐름 화면의 제목 줄 ───────────────────────────────────────────

/**
 * 결과 확인 중 · 상자 열기 · 결과 · 결과 확인 지연의 제목 줄 — 로고 · 잔액 머리(앱 셸) 아래. 왼쪽 뒤로, 가운데 제목,
 * 오른쪽 한 행동(건너뛰기) 또는 같은 폭의 빈자리. 시안에서 화살표가 빠진 장면도 기존 복귀 동작은 그대로 둔다.
 */
@Composable
internal fun DrawFlowBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backTag: String = "draw-flow-back",
    trailing: (@Composable () -> Unit)? = null,
) {
    val t = runTone()
    Box(modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 4.dp)) {
        com.stepup.android.ui.components.RunBackButton(onBack, Modifier.align(Alignment.CenterStart), tag = backTag, tint = t.text)
        Text(
            title, style = runTextStyle(18.sp, t.text, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 60.dp).semantics { heading() },
        )
        if (trailing != null) {
            Box(Modifier.align(Alignment.CenterEnd)) { trailing() }
        }
    }
}

/** 흐름 화면의 큰 제목(가운데, 기울인 굵은 글자) · 한 줄 설명 */
@Composable
internal fun DrawFlowHeadline(title: String, body: String?, modifier: Modifier = Modifier) {
    val t = runTone()
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            title, style = drawTitleStyle(28.sp, t.text), textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { heading() },
        )
        if (body != null) {
            Text(
                body, style = runTextStyle(15.sp, t.label, FontWeight.Medium, 1.45f), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}
