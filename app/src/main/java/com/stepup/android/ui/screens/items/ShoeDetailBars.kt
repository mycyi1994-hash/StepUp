package com.stepup.android.ui.screens.items

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.screens.customize.StatScale

/*
 * 신발 상세의 능력치 네 칸 — 카툰 입체형(2026-09-30 사용자 확정안 1번, docs/redesign/shoe-detail-cartoon-2026-09-30).
 * 스틸 블루 칸 하나에 "항목명 · 입체 막대 · 값" 한 줄. 칸 배경은 평평하고 입체감은 가운데 막대에만 둔다.
 * 막대는 그림을 늘이지 않고 경로로 그린다(길이가 바뀌어도 테두리가 찌그러지지 않게). 움직임 · 반짝임은 없다.
 */

/** 막대 채움 한 벌 — 정면 · 밝은 윗면 · 어두운 아랫면. 신발 등급과 상관없이 항목마다 같은 색이다 */
@Immutable
class CartoonFill(val face: Color, val top: Color, val bottom: Color)

object CartoonColors {
    /** 네 칸의 파란 바탕(파란 톤 v4 — 기본 파란 표면 #0B2B50 보다 한 단계 밝게, 남색 바닥과 갈리게) */
    val Cell = Color(0xFF0E2E57)
    val Text = Color(0xFFF5F8FF)
    val Number = Color(0xFFAAC3EA)
    val Ink = Color(0xFF020C1A)
    val Frame = Color(0xFF1B4FB0)
    val FrameTop = Color(0xFF4F8DF2)
    val FrameBottom = Color(0xFF0E2C6A)
    val Track = Color(0xFF061830)

    val Level = CartoonFill(Color(0xFFFFBD35), Color(0xFFFFE586), Color(0xFFC77A0E))
    val Efficiency = CartoonFill(Color(0xFF2E99FF), Color(0xFF80CBFF), Color(0xFF1254B0))
    val Comfort = CartoonFill(Color(0xFFA779F3), Color(0xFFD0B5FF), Color(0xFF6630AF))
    val Durability = CartoonFill(Color(0xFF27DCC5), Color(0xFF93FFEC), Color(0xFF0B927C))

    /** 강화 화면의 효율 — 강화 전달본의 청록 #48D9FA */
    val UpgradeEfficiency = CartoonFill(Color(0xFF48D9FA), Color(0xFFB0F1FF), Color(0xFF1287B0))
}

private fun fillOf(stat: DetailStat): CartoonFill = when (stat) {
    DetailStat.LEVEL -> CartoonColors.Level
    DetailStat.EFFICIENCY -> CartoonColors.Efficiency
    DetailStat.COMFORT -> CartoonColors.Comfort
    DetailStat.DURABILITY -> CartoonColors.Durability
}

private val CellShape = RoundedCornerShape(14.dp)
private val LabelMin = 52.dp
private val ValueMin = 64.dp
private val ColumnGap = 8.dp
private val CellPadding = 12.dp

/** 한 줄 배치를 지키는 막대의 가장 짧은 폭 — 이보다 좁아지면 막대를 글 아래로 내린다 */
private val BarMin = 56.dp
private val LabelStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold)

/** 값은 고정폭 숫자 — "5 / 15" 와 "92 / 100" 의 숫자 폭이 같게 */
private val ValueStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum")

/**
 * 레벨 · 효율 · 착화감 · 내구도 네 칸. 이름 열 · 값 열 폭은 네 줄 중 가장 긴 글에 맞춰 같게 둔다 —
 * 값이 긴 줄(내구도)만 막대가 짧아지지 않고 네 막대의 시작 · 끝이 같다. 글자를 키우면 열과 칸 높이가 함께 커진다.
 */
@Composable
fun ShoeStatCells(shoe: Sneaker, modifier: Modifier = Modifier) {
    val rows = remember(shoe) { detailStatRows(shoe) }
    val labels = rows.map { statTitle(it.stat) }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val base = LocalTextStyle.current
    val labelStyle = remember(base) { base.merge(LabelStyle) }
    val valueStyle = remember(base) { base.merge(ValueStyle) }
    val (labelWidth, valueWidth) = remember(labels, rows, labelStyle, valueStyle, density) {
        fun widest(texts: List<String>, style: TextStyle): Dp = with(density) {
            (texts.maxOf { measurer.measure(it, style, softWrap = false).size.width } + 1).toDp()
        }
        maxOf(LabelMin, widest(labels, labelStyle)) to maxOf(ValueMin, widest(rows.map { it.value }, valueStyle))
    }
    val levelMax = shoe.maxLevel.toString()
    BoxWithConstraints(modifier.fillMaxWidth()) {
        // 아주 큰 글씨 · 좁은 화면에서 두 열이 칸을 다 먹으면(막대가 BarMin 보다 짧아지면) 그때만 막대를 글 아래 줄로 내린다
        val stacked = maxWidth - CellPadding * 2 - ColumnGap * 2 - labelWidth - valueWidth < BarMin
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEachIndexed { index, row ->
                val full = when (row.stat) {
                    DetailStat.LEVEL -> levelMax
                    DetailStat.EFFICIENCY -> formatPercent(StatScale.EFFICIENCY_MAX_PERCENT)
                    DetailStat.COMFORT -> formatPercent(StatScale.ENERGY_MAX_PERCENT)
                    DetailStat.DURABILITY -> StatScale.DURABILITY_MAX.toString()
                }
                StatCell(
                    label = labels[index], value = row.value, fraction = row.fraction, fill = fillOf(row.stat),
                    labelWidth = labelWidth, valueWidth = valueWidth, labelStyle = labelStyle, valueStyle = valueStyle,
                    stacked = stacked,
                    description = stringResource(R.string.sdv_bar_cd, labels[index], row.value, full),
                    tag = "shoe-cell-" + row.stat.name.lowercase(),
                )
            }
        }
    }
}

@Composable
private fun statTitle(stat: DetailStat): String = stringResource(
    when (stat) {
        DetailStat.LEVEL -> R.string.sdv_info_level
        DetailStat.EFFICIENCY -> R.string.my_shoes_stat_efficiency
        DetailStat.COMFORT -> R.string.sdv_stat_comfort
        DetailStat.DURABILITY -> R.string.sdv_stat_durability
    },
)

@Composable
private fun StatCell(
    label: String,
    value: String,
    fraction: Float,
    fill: CartoonFill,
    labelWidth: Dp,
    valueWidth: Dp,
    labelStyle: TextStyle,
    valueStyle: TextStyle,
    description: String,
    tag: String,
    stacked: Boolean,
) {
    val cell = Modifier.fillMaxWidth().heightIn(min = 54.dp).background(CartoonColors.Cell, CellShape)
        .semantics(mergeDescendants = true) { contentDescription = description }
        .testTag(tag)
        .padding(horizontal = CellPadding, vertical = 6.dp)
    if (stacked) {
        Column(cell, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = labelStyle, color = CartoonColors.Text, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(ColumnGap))
                Text(value, style = valueStyle, color = CartoonColors.Text, maxLines = 1, softWrap = false,
                    modifier = Modifier.testTag("$tag-value"))
            }
            CartoonStatBar(fraction, fill, Modifier.fillMaxWidth().height(30.dp).testTag("$tag-bar"))
        }
        return
    }
    Row(cell, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = labelStyle, color = CartoonColors.Text, maxLines = 1, softWrap = false,
            modifier = Modifier.width(labelWidth))
        Spacer(Modifier.width(ColumnGap))
        CartoonStatBar(fraction, fill, Modifier.weight(1f).height(30.dp).testTag("$tag-bar"))
        Spacer(Modifier.width(ColumnGap))
        Text(value, style = valueStyle, color = CartoonColors.Text, maxLines = 1, softWrap = false, textAlign = TextAlign.End,
            modifier = Modifier.width(valueWidth).testTag("$tag-value"))
    }
}

/**
 * 카툰 입체 막대 하나 — 아래 순서로 겹친다.
 * 1 아래로 내려온 어두운 받침 → 2 짙은 잉크 외곽선 → 3 파란 회색 프레임(밝은 윗면 · 어두운 아랫면) →
 * 4 안으로 들어간 짙은 남색 트랙 → 5 진행률만큼의 채움 → 6 채움의 밝은 윗면 · 정면 · 어두운 아랫면 → 7 왼쪽 위 하이라이트 한 줄.
 * [fraction] 은 프레임을 뺀 안쪽 트랙 폭에 적용한다. 0 이면 채움 · 하이라이트를 그리지 않는다. 읽기 도구는 칸이 읽는다.
 */
@Composable
fun CartoonStatBar(fraction: Float, fill: CartoonFill, modifier: Modifier = Modifier, preview: Float? = null) {
    Canvas(modifier) {
        val now = fraction.coerceIn(0f, 1f).takeUnless { it.isNaN() } ?: 0f
        drawCartoonBar(now, fill, preview?.coerceIn(0f, 1f)?.takeUnless { it.isNaN() || it <= now })
    }
}

/** 모서리를 짧게 잘라낸 사각형 — [cut] 은 모서리 사선의 깊이 */
private fun chamfered(rect: Rect, cut: Float): Path {
    val c = cut.coerceAtMost(minOf(rect.width, rect.height) / 2f).coerceAtLeast(0f)
    return Path().apply {
        moveTo(rect.left + c, rect.top)
        lineTo(rect.right - c, rect.top)
        lineTo(rect.right, rect.top + c)
        lineTo(rect.right, rect.bottom - c)
        lineTo(rect.right - c, rect.bottom)
        lineTo(rect.left + c, rect.bottom)
        lineTo(rect.left, rect.bottom - c)
        lineTo(rect.left, rect.top + c)
        close()
    }
}

private fun Rect.inset(by: Float) = Rect(left + by, top + by, right - by, bottom - by)

private fun DrawScope.drawCartoonBar(fraction: Float, fill: CartoonFill, preview: Float? = null) {
    val drop = 2.5.dp.toPx()
    val ink = 1.75.dp.toPx()
    val body = Rect(0f, 0f, size.width, size.height - drop)
    if (body.width <= ink * 8 || body.height <= ink * 6) return
    val cut = 6.dp.toPx()

    // 1 받침 — 같은 모양을 아래로 내려 어둡게
    drawPath(chamfered(body.translate(Offset(0f, drop)), cut), CartoonColors.Ink.copy(alpha = 0.85f))
    // 2 잉크 외곽선
    drawPath(chamfered(body, cut), CartoonColors.Ink)
    // 3 프레임 — 밝은 윗면, 그 위에 조금 내린 어두운 아랫면, 가운데 프레임 면
    val frame = body.inset(ink)
    val framePath = chamfered(frame, cut - ink * 0.6f)
    val rim = 1.5.dp.toPx()
    drawPath(framePath, CartoonColors.FrameTop)
    clipPath(framePath) {
        drawPath(chamfered(frame.translate(Offset(0f, rim)), cut - ink * 0.6f), CartoonColors.FrameBottom)
    }
    val face = Rect(frame.left + rim, frame.top + rim, frame.right - rim, frame.bottom - rim)
    drawPath(chamfered(face, cut - ink - rim * 0.6f), CartoonColors.Frame)
    // 4 안쪽 트랙 — 잉크 테두리 안의 짙은 남색
    val trackOuter = face.inset(1.5.dp.toPx())
    val trackCut = (cut - ink - rim).coerceAtLeast(2.dp.toPx())
    drawPath(chamfered(trackOuter, trackCut), CartoonColors.Ink)
    val track = trackOuter.inset(1.25.dp.toPx())
    drawPath(chamfered(track, trackCut - 0.5.dp.toPx()), CartoonColors.Track)
    // 5 ~ 7 채움 — 트랙 안쪽 전체 폭이 100%
    val lane = track.inset(1.5.dp.toPx())
    // 강화 미리보기 — 지금 채움 뒤에 다음 값까지의 구간을 옅게(실제 값으로 계산한 길이 그대로, 과장하지 않는다)
    if (preview != null) {
        val previewWidth = lane.width * preview
        if (previewWidth >= 0.5f) {
            val ahead = Rect(lane.left, lane.top, lane.left + previewWidth, lane.bottom)
            val aheadCut = minOf(3.dp.toPx(), previewWidth / 2f, ahead.height / 2f)
            drawPath(chamfered(ahead, aheadCut), fill.top.copy(alpha = 0.55f))
        }
    }
    val width = lane.width * fraction
    if (fraction <= 0f || width < 0.5f) return
    val bar = Rect(lane.left, lane.top, lane.left + width, lane.bottom)
    // 사선 깊이는 채움 길이의 절반 이하 — 아주 짧아도 모양이 뒤집히지 않게
    val barCut = minOf(3.dp.toPx(), width / 2f, bar.height / 2f)
    val barPath = chamfered(bar, barCut)
    clipPath(barPath) {
        drawRect(fill.face, topLeft = bar.topLeft, size = bar.size)
        val topBand = bar.height * 0.30f
        clipRect(bar.left, bar.top, bar.right, bar.top + topBand) { drawRect(fill.top, bar.topLeft, bar.size) }
        val bottomBand = bar.height * 0.28f
        clipRect(bar.left, bar.bottom - bottomBand, bar.right, bar.bottom) { drawRect(fill.bottom, bar.topLeft, bar.size) }
    }
    drawPath(barPath, CartoonColors.Ink, style = Stroke(width = 1.dp.toPx()))
    // 7 하이라이트 — 채움 왼쪽 위의 짧고 둥근 한 줄
    val line = 2.dp.toPx()
    val startX = bar.left + barCut + 3.dp.toPx()
    val endX = minOf(bar.left + width * 0.42f, startX + 22.dp.toPx(), bar.right - barCut - 2.dp.toPx())
    if (endX - startX >= line) {
        val y = bar.top + bar.height * 0.17f + line / 2f
        drawLine(Color.White.copy(alpha = 0.9f), Offset(startX, y), Offset(endX, y), strokeWidth = line, cap = StrokeCap.Round)
    }
}

/**
 * SD08 조회 중 — 네 칸의 이름만 두고 막대는 빈 트랙, 값은 "—". 읽지 못한 값을 0 으로 보이지 않는다.
 */
@Composable
fun ShoeStatCellsPlaceholder(modifier: Modifier = Modifier) {
    val base = LocalTextStyle.current
    val labelStyle = remember(base) { base.merge(LabelStyle) }
    val valueStyle = remember(base) { base.merge(ValueStyle) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailStat.entries.forEach { stat ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 54.dp).background(CartoonColors.Cell, CellShape)
                    .padding(horizontal = CellPadding, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(statTitle(stat), style = labelStyle, color = CartoonColors.Text.copy(alpha = 0.7f), maxLines = 1,
                    softWrap = false, modifier = Modifier.width(LabelMin + 16.dp))
                Spacer(Modifier.width(ColumnGap))
                CartoonStatBar(0f, fillOf(stat), Modifier.weight(1f).height(30.dp))
                Spacer(Modifier.width(ColumnGap))
                Text("—", style = valueStyle, color = CartoonColors.Number, textAlign = TextAlign.End,
                    modifier = Modifier.width(ValueMin))
            }
        }
    }
}
