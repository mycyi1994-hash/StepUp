package com.stepup.android.ui.screens.walk

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.ui.res.*
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.TrackPoint
import com.stepup.android.domain.haversineMeters
import com.stepup.android.domain.tier
import com.stepup.android.ui.components.formatSupDown
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.StepUpFonts

/*
 * 러닝 중 · 러닝 완료 — 2026-09-29 전달본(design/run-journey-2026-09-29 의 01 러닝 중 · 03 러닝 후).
 *
 * 러닝 중: 지도를 화면 가득 깔고, 위에 러닝 시간 | 달린 거리 판 하나, 아래에 현재 속도 | 내구도 판 하나와
 * 일시정지 · 종료 버튼. 평균 페이스 · 예상 보상은 러닝 중 화면에서 뺐다(예상 보상은 ⋯ 러닝 정보에 남는다).
 * 러닝 완료: 기록 카드 한 장(로고 · 날짜 · 거리 · 지도 · 시간 / 페이스 / 속도 · 신발 · 보상) + 이미지 저장 | 공유하기 + 완료.
 *
 * 수는 모두 이번 러닝의 실제 값이다. 보상은 서버가 확인하기 전에는 "예상 보상 · 정산 대기"로만 적는다.
 */

// ── 현재 속도 ──────────────────────────────────────────────────────

/** 최근 이만큼(ms)의 경로 점으로 지금 속도를 잰다 */
const val SPEED_WINDOW_MS = 20_000L

/** 새 점이 이만큼 안 오면 멈춰 선 것으로 본다 — GPS 는 8m 넘게 움직여야 경로에 점을 더한다 */
const val SPEED_STILL_MS = 20_000L

/** 이웃한 두 점이 이보다 멀면(신호가 끊겼다 이어짐) 속도를 모른다 */
const val SPEED_MAX_GAP_MS = 60_000L

/** 걸음이 이만큼(ms) 안에 늘었으면 새 경로 점이 없어도 아직 움직이는 중으로 본다 */
const val SPEED_MOVING_GRACE_MS = 10_000L

/**
 * 지금 속도(km/h) — 경로의 마지막 점과 그 앞 [SPEED_WINDOW_MS] 안의 점들로 잰다. 모르면 null.
 *
 * 마지막으로 버려진 구간([flaggedAt])이 마지막 점보다 늦으면 지금 읽은 속도가 사람 속도를 넘은 것이다 —
 * 그 뒤 사람 속도의 점이 들어올 때까지 모른다고 보인다. 새 점이 한동안 없고 걸음도 늘지 않으면 멈춰 선 것(0).
 * 걸음이 늘고 있으면([movingAt], 마지막으로 걸음이 는 때) 점이 드문 것뿐이라 마지막 속도를 그대로 보인다 — 구글 위치 서비스는
 * 오차의 두 배를 움직여야 점을 찍어, 천천히 걸으면 20~40초마다라 속도가 0 과 값을 오갔다(2026-10-17 점검).
 */
fun currentSpeedKmh(track: List<TrackPoint>, now: Long, flaggedAt: Long, movingAt: Long = 0L): Double? {
    val last = track.lastOrNull() ?: return null
    if (flaggedAt > last.at) return null
    if (now - last.at > SPEED_STILL_MS && now - movingAt > SPEED_MOVING_GRACE_MS) return 0.0
    if (now - last.at > SPEED_MAX_GAP_MS) return null
    if (track.size < 2) return null
    var first = track.lastIndex - 1
    while (first > 0 && track[first - 1].at >= last.at - SPEED_WINDOW_MS) first--
    if (last.at - track[first].at > SPEED_MAX_GAP_MS) return null
    var meters = 0.0
    for (i in first until track.lastIndex) meters += haversineMeters(track[i].toGeoPoint(), track[i + 1].toGeoPoint())
    val seconds = (last.at - track[first].at) / 1000.0
    return if (seconds > 0) meters / seconds * 3.6 else null
}

/** 평균 속도(km/h) — 50m 를 넘게 달렸을 때만 */
fun averageSpeedKmh(km: Double, elapsedSec: Long): Double? =
    if (km >= 0.05 && elapsedSec > 0) km / (elapsedSec / 3600.0) else null

/** 보상 수 — 1 SUP 아래는 둘째 자리, 그 위는 첫째 자리까지. 버린다(받을 것보다 크게 보이지 않게) */
fun rewardAmount(value: Double): String =
    formatSupDown(value, if (kotlin.math.abs(value) < 1.0) 2 else 1)

// ── 색 ─────────────────────────────────────────────────────────────

@Immutable
class RunPalette(
    val screen: Color,
    val panel: Color,
    val panelEdge: Color,
    val divider: Color,
    val label: Color,
    val text: Color,
    val muted: Color,
    val unit: Color,
    val track: Color,
    val bar: List<Color>,
    val primary: List<Color>,
    val primaryOff: Color,
    val stopFace: Color,
    val stopEdge: Color,
    val stopMark: Color,
    val warnFace: Color,
    val warnInk: Color,
    val warnIcon: Color,
    val voidFace: Color,
    val voidInk: Color,
    val card: Color,
    val cardEdge: Color,
    val chipEdge: Color,
    val chipText: Color,
    val doneText: Color,
    val ghostFace: Color,
    val ghostEdge: Color,
    val emblemFace: Color,
    val emblemInk: Color,
)

// UI 최종 통합안(2026-10-25) — 다크블루 · 화이트블루, 평평한 블루(그라데이션 없음) · 회색 채움
val DarkRun = RunPalette(
    screen = Color(0xFF070B14), panel = Color(0xF20E1424), panelEdge = Color(0x17FFFFFF), divider = Color(0x17FFFFFF),
    label = Color(0xFF969CAA), text = Color.White, muted = Color(0xFF6E7586), unit = Color(0xFF969CAA),
    track = Color(0xFF222A3D), bar = listOf(Color(0xFF2F6BFF), Color(0xFF2F6BFF)),
    primary = listOf(Color(0xFF2F6BFF), Color(0xFF2F6BFF)), primaryOff = Color(0xFF141A27),
    stopFace = Color(0xFF202736), stopEdge = Color.Transparent, stopMark = Color(0xFFFF453A),
    warnFace = Color(0xFFFDB829), warnInk = Color(0xFF14171D), warnIcon = Color(0xFFF7B32B),
    voidFace = Color(0xFFFF453A), voidInk = Color.White,
    card = Color(0xFF0E1424), cardEdge = Color.Transparent,
    chipEdge = Color.Transparent, chipText = Color(0xFFE2E8F5), doneText = Color(0xFF6E9BFF),
    ghostFace = Color(0xFF202736), ghostEdge = Color.Transparent,
    emblemFace = Color(0xFF202736), emblemInk = Color(0xFFE2E8F5),
)

val LightRun = RunPalette(
    screen = Color(0xFFF2F2F7), panel = Color(0xF2FFFFFF), panelEdge = Color(0x293C3C43), divider = Color(0x293C3C43),
    label = Color(0xFF848489), text = Color.Black, muted = Color(0xFF9C9CA2), unit = Color(0xFF848489),
    track = Color(0xFFE3E3E8), bar = listOf(Color(0xFF1F5EFF), Color(0xFF1F5EFF)),
    primary = listOf(Color(0xFF1F5EFF), Color(0xFF1F5EFF)), primaryOff = Color(0xFFE8E8ED),
    stopFace = Color(0xFFE3E3E8), stopEdge = Color.Transparent, stopMark = Color(0xFFFF3B30),
    warnFace = Color(0xFFFDB829), warnInk = Color(0xFF14171D), warnIcon = Color(0xFFD08A00),
    voidFace = Color(0xFFFF3B30), voidInk = Color.White,
    card = Color.White, cardEdge = Color.Transparent,
    chipEdge = Color.Transparent, chipText = Color(0xFF3C3C43), doneText = Color(0xFF1F5EFF),
    ghostFace = Color(0xFFE3E3E8), ghostEdge = Color.Transparent,
    emblemFace = Color(0xFFE3E3E8), emblemInk = Color(0xFF3C3C43),
)

@Composable
fun runPalette(): RunPalette = if (StepUpColors.dark) DarkRun else LightRun

private fun numberStyle(size: TextUnit, color: Color, weight: FontWeight = FontWeight.ExtraBold) = TextStyle(
    fontFamily = StepUpFonts.sans, fontWeight = weight, fontSize = size, color = color,
    letterSpacing = (-0.02).em, fontFeatureSettings = "tnum", lineHeight = 1.1.em,
)

/**
 * 큰 수 + 작은 단위 한 줄 — 폭이 모자라면(큰 글씨 · 좁은 화면) 둘을 같은 비율로 줄인다. 두 줄로 넘기지 않는다.
 */
@Composable
fun FitNumber(
    value: String,
    unit: String?,
    valueSize: TextUnit,
    unitSize: TextUnit,
    valueColor: Color,
    unitColor: Color,
    modifier: Modifier = Modifier,
    valueTag: String? = null,
    unitWeight: FontWeight = FontWeight.Medium,
) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier) {
        val max = constraints.maxWidth
        val scale = remember(value, unit, max, valueSize, unitSize) {
            val v = measurer.measure(value, numberStyle(valueSize, valueColor), maxLines = 1).size.width
            val u = unit?.let { measurer.measure(" $it", numberStyle(unitSize, unitColor, unitWeight), maxLines = 1).size.width } ?: 0
            val total = v + u
            if (max == Constraints.Infinity || total <= max || total == 0) 1f else max.toFloat() / total
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value, style = numberStyle(valueSize * scale, valueColor), maxLines = 1, softWrap = false,
                modifier = Modifier.alignByBaseline().then(if (valueTag != null) Modifier.testTag(valueTag) else Modifier),
            )
            if (unit != null) {
                Text(
                    " $unit", style = numberStyle(unitSize * scale, unitColor, unitWeight), maxLines = 1, softWrap = false,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
    }
}

// ── 러닝 중 ────────────────────────────────────────────────────────

/** 판 — 남색 면, 은은한 테두리, 모서리 18dp(전달본) */
@Composable
fun RunPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val p = runPalette()
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(p.panel, shape).border(1.dp, p.panelEdge, shape)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        content = content,
    )
}

/** 판 안의 한 칸 — 작은 이름 + 큰 수(+ 단위) */
@Composable
private fun PanelValue(
    label: String,
    value: String,
    unit: String?,
    dim: Boolean,
    modifier: Modifier,
    valueTag: String?,
    lead: (@Composable () -> Unit)? = null,
) {
    val p = runPalette()
    Column(modifier) {
        Text(label, color = p.label, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            lead?.invoke()
            FitNumber(
                value, unit, valueSize = 46.sp, unitSize = 22.sp,
                valueColor = if (dim) p.muted else p.text, unitColor = p.unit,
                modifier = Modifier.weight(1f), valueTag = valueTag,
            )
        }
    }
}

/**
 * 같은 폭 칸 [columns] 개를 [gap] 을 두고 한 줄로 — 칸 사이 가운데에 가는 세로선을 긋는다(줄 높이만큼, 위아래 4dp 안쪽).
 * 선을 그림으로 긋는다 — 칸 안의 큰 수([FitNumber])는 폭을 재는 칸이라 "높이를 먼저 물어보는" 줄(IntrinsicSize)에 넣을 수 없다.
 */
@Composable
fun DividedRow(
    columns: Int,
    gap: Dp,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val color = runPalette().divider
    Row(
        modifier.fillMaxWidth().drawBehind {
            val g = gap.toPx()
            val column = (size.width - g * (columns - 1)) / columns
            val inset = 4.dp.toPx()
            for (i in 1 until columns) {
                val x = column * i + g * (i - 1) + g / 2f
                drawLine(color, Offset(x, inset), Offset(x, size.height - inset), strokeWidth = 1.dp.toPx())
            }
        },
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * 러닝 중 위 판 — 러닝 시간 | 달린 거리(같은 무게의 두 칸). 목표 · 다이어트는 아래에 진행 막대와 한두 줄.
 * 왼쪽 큰 수가 이 화면의 주인공이다(run-hero-value).
 */
@Composable
fun RunTopPanel(
    left: Pair<String, String>,
    right: Pair<String, String>,
    rightUnit: String?,
    dim: Boolean,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    captions: List<String> = emptyList(),
) {
    val p = runPalette()
    RunPanel(modifier.testTag("run-top-panel")) {
        DividedRow(columns = 2, gap = 28.dp) {
            PanelValue(left.first, left.second, null, dim, Modifier.weight(1f), valueTag = "run-hero-value")
            PanelValue(right.first, right.second, rightUnit, dim, Modifier.weight(1f), valueTag = "run-distance-value")
        }
        if (progress != null) {
            Spacer(Modifier.height(10.dp))
            RunBar(progress, Modifier.fillMaxWidth().height(6.dp))
        }
        captions.forEachIndexed { index, caption ->
            Spacer(Modifier.height(if (index == 0) 8.dp else 2.dp))
            Text(caption, color = p.label, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.testTag("run-panel-caption"))
        }
    }
}

/** 채움 막대 — 어두운 바탕에 파란 채움(내구도 · 진행) */
@Composable
fun RunBar(fraction: Float, modifier: Modifier) {
    val p = runPalette()
    val shape = RoundedCornerShape(50)
    Box(modifier.clip(shape).background(p.track, shape)) {
        Box(
            Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(shape)
                .background(Brush.horizontalGradient(p.bar), shape),
        )
    }
}

/**
 * 위 알림 띠 — 속도 측정 오류(노랑) · 세션 무효(빨강). 누르면 까닭(러닝 정보)을 연다.
 */
@Composable
fun RunAlertBanner(text: String, void: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = runPalette()
    val shape = RoundedCornerShape(16.dp)
    val face = if (void) p.voidFace else p.warnFace
    val ink = if (void) p.voidInk else p.warnInk
    Row(
        modifier.fillMaxWidth().heightIn(min = 46.dp).clip(shape).background(face, shape)
            .feedbackClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(if (void) "run-void-banner" else "run-speed-banner"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = ink, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f, fill = false))
    }
}

/** 지도 위 작은 판 — 위치 안내 · 같이 뛰는 순위 · 챌린지 진행 · GPS 상태처럼 가끔만 보이는 것 */
@Composable
fun RunOverlayCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val p = runPalette()
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(p.panel, shape).border(1.dp, p.panelEdge, shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        content = content,
    )
}

/** 주 버튼 — 파란 면 · 흰 글자(일시정지 · 다시 달리기 · 러닝 시작 · 완료) */
@Composable
fun RunPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 60.dp,
) {
    val p = runPalette()
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier.heightIn(min = height).clip(shape)
            .background(if (enabled) Brush.horizontalGradient(p.primary) else Brush.horizontalGradient(listOf(p.primaryOff, p.primaryOff)), shape)
            .feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val ink = if (enabled) Color.White else Color.White.copy(alpha = 0.6f)
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(label, color = ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 종료 — 어두운 면 · 빨간 네모 · 흰 글자 */
@Composable
fun RunStopButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = runPalette()
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier.heightIn(min = 60.dp).clip(shape).background(p.stopFace, shape).border(1.dp, p.stopEdge, shape)
            .feedbackClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(18.dp).clip(RoundedCornerShape(3.dp)).background(p.stopMark))
        Spacer(Modifier.width(12.dp))
        Text(label, color = p.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * 위 판들은 위에, 아래 판은 아래에 붙인다. 둘 사이(지도가 보이는 자리)가 [minGap] 보다 좁아지면
 * 창 높이([viewport])를 넘겨 쌓는다 — 부모가 스크롤한다(작은 화면 · 큰 글씨).
 */
@Composable
fun RunOverlayLayout(
    viewport: Int,
    minGap: Dp,
    modifier: Modifier = Modifier,
    top: @Composable () -> Unit,
    bottom: @Composable () -> Unit,
) {
    Layout(contents = listOf(top, bottom), modifier = modifier) { (t, b), constraints ->
        val width = constraints.maxWidth
        val loose = Constraints(maxWidth = width)
        val topP = t.map { it.measure(loose) }
        val bottomP = b.map { it.measure(loose) }
        val topH = topP.sumOf { it.height }
        val bottomH = bottomP.sumOf { it.height }
        val content = topH + minGap.roundToPx() + bottomH
        val height = if (viewport == Constraints.Infinity) content else maxOf(content, viewport)
        layout(width, height) {
            var y = 0
            topP.forEach { it.place(0, y); y += it.height }
            var by = height - bottomH
            bottomP.forEach { it.place(0, by); by += it.height }
        }
    }
}

// ── 러닝 완료 ──────────────────────────────────────────────────────

