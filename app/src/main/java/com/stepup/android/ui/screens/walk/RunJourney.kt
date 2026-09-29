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
import androidx.compose.ui.res.stringResource
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
import com.stepup.android.R
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.TrackPoint
import com.stepup.android.domain.haversineMeters
import com.stepup.android.domain.tier
import com.stepup.android.ui.components.LiveRouteMap
import com.stepup.android.ui.components.ShoeArtThumbnail
import com.stepup.android.ui.components.ShoeNameWithBadge
import com.stepup.android.ui.components.formatSupDown
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.items.durabilityPoints
import com.stepup.android.ui.screens.items.shoeName
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.StepUpSans

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
internal const val SPEED_WINDOW_MS = 20_000L

/** 새 점이 이만큼 안 오면 멈춰 선 것으로 본다 — GPS 는 8m 넘게 움직여야 경로에 점을 더한다 */
internal const val SPEED_STILL_MS = 20_000L

/** 이웃한 두 점이 이보다 멀면(신호가 끊겼다 이어짐) 속도를 모른다 */
internal const val SPEED_MAX_GAP_MS = 60_000L

/**
 * 지금 속도(km/h) — 경로의 마지막 점과 그 앞 [SPEED_WINDOW_MS] 안의 점들로 잰다. 모르면 null.
 *
 * 마지막으로 버려진 구간([flaggedAt])이 마지막 점보다 늦으면 지금 읽은 속도가 사람 속도를 넘은 것이다 —
 * 그 뒤 사람 속도의 점이 들어올 때까지 모른다고 보인다. 새 점이 한동안 없으면 멈춰 선 것(0).
 */
internal fun currentSpeedKmh(track: List<TrackPoint>, now: Long, flaggedAt: Long): Double? {
    val last = track.lastOrNull() ?: return null
    if (flaggedAt > last.at) return null
    if (now - last.at > SPEED_STILL_MS) return 0.0
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
internal fun averageSpeedKmh(km: Double, elapsedSec: Long): Double? =
    if (km >= 0.05 && elapsedSec > 0) km / (elapsedSec / 3600.0) else null

/** 보상 수 — 1 SUP 아래는 둘째 자리, 그 위는 첫째 자리까지. 버린다(받을 것보다 크게 보이지 않게) */
internal fun rewardAmount(value: Double): String =
    formatSupDown(value, if (kotlin.math.abs(value) < 1.0) 2 else 1)

// ── 색 ─────────────────────────────────────────────────────────────

@Immutable
internal class RunPalette(
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

private val DarkRun = RunPalette(
    screen = Color(0xFF041C33), panel = Color(0xEB0A1A33), panelEdge = Color(0xFF26395A), divider = Color(0xFF26395A),
    label = Color(0xFFB6C4DD), text = Color.White, muted = Color(0xFF8D9DBB), unit = Color(0xFF7EAEFF),
    track = Color(0xFF213453), bar = listOf(Color(0xFF1A61F0), Color(0xFF3A8BFF)),
    primary = listOf(Color(0xFF1159F0), Color(0xFF2C7DFF)), primaryOff = Color(0xFF1D355F),
    stopFace = Color(0xFF13274A), stopEdge = Color(0xFF2B4168), stopMark = Color(0xFFF26D6D),
    warnFace = Color(0xFFFDB829), warnInk = Color(0xFF14171D), warnIcon = Color(0xFFF7B32B),
    voidFace = Color(0xFFD64545), voidInk = Color.White,
    card = Color(0xFF0A1A32), cardEdge = Color(0xFF223759),
    chipEdge = Color(0xFF34496D), chipText = Color(0xFFC9D5EA), doneText = Color(0xFF7EAEFF),
    ghostFace = Color(0xFF0E2242), ghostEdge = Color(0xFF2A3F63),
    emblemFace = Color(0xFF12284B), emblemInk = Color(0xFFDCE6FF),
)

private val LightRun = RunPalette(
    screen = Color(0xFFF2F5FA), panel = Color(0xF2FFFFFF), panelEdge = Color(0xFFD5DEEE), divider = Color(0xFFE0E6F1),
    label = Color(0xFF4A5775), text = Color(0xFF0E1A33), muted = Color(0xFF5B6886), unit = Color(0xFF1F63D9),
    track = Color(0xFFDCE4F2), bar = listOf(Color(0xFF1A61F0), Color(0xFF3A8BFF)),
    primary = listOf(Color(0xFF1159F0), Color(0xFF2C7DFF)), primaryOff = Color(0xFFB9C8E6),
    stopFace = Color.White, stopEdge = Color(0xFFD5DEEE), stopMark = Color(0xFFE05252),
    warnFace = Color(0xFFFDB829), warnInk = Color(0xFF14171D), warnIcon = Color(0xFFD08A00),
    voidFace = Color(0xFFD64545), voidInk = Color.White,
    card = Color.White, cardEdge = Color(0xFFD5DEEE),
    chipEdge = Color(0xFFC5D1E6), chipText = Color(0xFF34405E), doneText = Color(0xFF1F63D9),
    ghostFace = Color.White, ghostEdge = Color(0xFFD5DEEE),
    emblemFace = Color(0xFFE8EEF9), emblemInk = Color(0xFF1F3F86),
)

@Composable
internal fun runPalette(): RunPalette = if (StepUpColors.dark) DarkRun else LightRun

private fun numberStyle(size: TextUnit, color: Color, weight: FontWeight = FontWeight.ExtraBold) = TextStyle(
    fontFamily = StepUpSans, fontWeight = weight, fontSize = size, color = color,
    letterSpacing = (-0.02).em, fontFeatureSettings = "tnum", lineHeight = 1.1.em,
)

/**
 * 큰 수 + 작은 단위 한 줄 — 폭이 모자라면(큰 글씨 · 좁은 화면) 둘을 같은 비율로 줄인다. 두 줄로 넘기지 않는다.
 */
@Composable
internal fun FitNumber(
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
internal fun RunPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
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
private fun DividedRow(
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
internal fun RunTopPanel(
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
internal fun RunBar(fraction: Float, modifier: Modifier) {
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
 * 러닝 중 아래 판 — 현재 속도 | 내구도 한 줄(같은 무게의 두 칸). 평균 페이스 · 예상 보상 · 신발 그림은 두지 않는다.
 * 속도를 모르면 "—", 방금 읽은 속도가 사람 속도를 넘었으면 앞에 노란 경고.
 */
@Composable
internal fun RunBottomPanel(
    speedKmh: Double?,
    speedError: Boolean,
    shoe: Sneaker?,
    dim: Boolean,
    modifier: Modifier = Modifier,
) {
    val p = runPalette()
    val speedLabel = stringResource(R.string.run_current_speed)
    val unit = stringResource(R.string.run_speed_unit)
    val unknown = stringResource(R.string.run_speed_unknown)
    val durabilityLabel = stringResource(R.string.sdv_stat_durability)
    val points = shoe?.let { durabilityPoints(it) }
    RunPanel(modifier.testTag("run-bottom-panel")) {
        DividedRow(columns = 2, gap = 28.dp) {
            Column(
                Modifier.weight(1f).semantics(mergeDescendants = true) {
                    contentDescription = if (speedKmh == null) "$speedLabel · $unknown" else "$speedLabel ${"%.1f".format(speedKmh)} $unit"
                }.testTag("run-speed"),
            ) {
                Text(speedLabel, color = p.label, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (speedError) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = p.warnIcon,
                            modifier = Modifier.size(24.dp).testTag("run-speed-warning"))
                        Spacer(Modifier.width(12.dp))
                    }
                    FitNumber(
                        speedKmh?.let { "%.1f".format(it) } ?: "—", unit, valueSize = 32.sp, unitSize = 20.sp,
                        valueColor = if (dim) p.muted else p.text, unitColor = p.label, modifier = Modifier.weight(1f),
                        valueTag = "run-speed-value", unitWeight = FontWeight.Normal,
                    )
                }
            }
            Column(
                Modifier.weight(1f).semantics(mergeDescendants = true) {
                    contentDescription = "$durabilityLabel ${points ?: "—"} / 100"
                }.testTag("run-durability"),
            ) {
                Text(durabilityLabel, color = p.label, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                FitNumber(
                    points?.toString() ?: "—", "/ 100", valueSize = 32.sp, unitSize = 18.sp,
                    valueColor = p.text, unitColor = p.label, modifier = Modifier.fillMaxWidth(),
                    valueTag = "run-durability-value", unitWeight = FontWeight.Normal,
                )
                Spacer(Modifier.height(8.dp))
                RunBar((points ?: 0) / 100f, Modifier.fillMaxWidth().height(8.dp))
            }
        }
    }
}

/**
 * 위 알림 띠 — 속도 측정 오류(노랑) · 세션 무효(빨강). 누르면 까닭(러닝 정보)을 연다.
 */
@Composable
internal fun RunAlertBanner(text: String, void: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
internal fun RunOverlayCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
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
internal fun RunPrimaryButton(
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
internal fun RunStopButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
internal fun RunOverlayLayout(
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

internal enum class Settle { PENDING, DONE, VOID, NOT_PAID }

/** 결과 카드의 보상 줄 — 이름 · 수 · 정산 상태, 아래에 덧붙일 설명 */
@Immutable
internal data class ResultReward(
    val label: String,
    val amount: String,
    val settle: Settle,
    val notes: List<String> = emptyList(),
    val signIn: Boolean = false,
)

/**
 * 위 · 무대(지도) · 아래를 쌓는다 — 위 · 아래를 먼저 재고 지도에 남은 높이를 준다([minStage]–[maxStage]).
 * [viewport] 안에 다 들면 스크롤이 없다. 모자라면 지도는 [minStage] 로 두고 넘친다 — 부모가 스크롤한다.
 */
@Composable
private fun ResultFitColumn(
    viewport: Int,
    minStage: Dp,
    maxStage: Dp,
    gap: Dp,
    modifier: Modifier,
    top: @Composable () -> Unit,
    stage: @Composable () -> Unit,
    bottom: @Composable () -> Unit,
) {
    Layout(contents = listOf(top, stage, bottom), modifier = modifier) { (t, s, b), constraints ->
        val width = constraints.maxWidth
        val loose = Constraints(maxWidth = width)
        val topP = t.first().measure(loose)
        val bottomP = b.first().measure(loose)
        val fixed = topP.height + gap.roundToPx() * 2 + bottomP.height
        val stageH = if (viewport == Constraints.Infinity) maxStage.roundToPx()
        else (viewport - fixed).coerceIn(minStage.roundToPx(), maxStage.roundToPx())
        val stageP = s.first().measure(Constraints.fixed(width, stageH))
        layout(width, fixed + stageH) {
            topP.place(0, 0)
            stageP.place(0, topP.height + gap.roundToPx())
            bottomP.place(0, topP.height + gap.roundToPx() * 2 + stageH)
        }
    }
}

/** 결과 카드의 바깥 여백 · 안쪽 위아래 여백 — 지도 높이를 셈할 때 뺀다 */
internal val ResultCardMargin = 12.dp
internal val ResultCardPadding = 16.dp

/** 결과 카드 지도의 가장 낮은 높이 — 이보다 좁으면 카드가 넘기기로 바뀐다 */
internal val ResultMapMin = 88.dp

/**
 * 러닝 완료 카드(전달본 03) — 로고 · 날짜 / 큰 거리 / 지도 / 러닝 시간 · 평균 페이스 · 평균 속도 / 신발(이름 끝 등급 배지 · 내구도) / 보상.
 * [viewport] 는 카드가 쓸 수 있는 높이(px) — 지도가 남는 높이를 채운다.
 */
@Composable
internal fun RunResultCard(
    viewport: Int,
    date: String?,
    km: Double,
    note: String?,
    track: List<GeoPoint>,
    time: String,
    paceSec: Long?,
    speedKmh: Double?,
    shoe: Sneaker?,
    reward: ResultReward,
    modifier: Modifier = Modifier,
    brand: @Composable () -> Unit,
) {
    val p = runPalette()
    val shape = RoundedCornerShape(20.dp)
    val hasRoute = track.isNotEmpty()
    ResultFitColumn(
        viewport = viewport,
        // 작은 화면(360×780 · 360×800)에서는 지도가 88dp 까지 줄어 보상 줄까지 카드가 한 화면에 든다
        minStage = if (hasRoute) ResultMapMin else 56.dp,
        maxStage = if (hasRoute) 260.dp else 56.dp,
        gap = 12.dp,
        modifier = modifier.fillMaxWidth().clip(shape).background(p.card, shape).border(1.dp, p.cardEdge, shape)
            .padding(horizontal = 12.dp, vertical = ResultCardPadding).testTag("run-result-card"),
        top = {
            Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    brand()
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    if (date != null) {
                        Text(date, color = p.label, fontSize = 13.sp, maxLines = 1, modifier = Modifier.testTag("run-result-date"))
                    }
                }
                Spacer(Modifier.height(10.dp))
                FitNumber(
                    "%.2f".format(km), "km", valueSize = 76.sp, unitSize = 38.sp,
                    valueColor = p.text, unitColor = p.unit, modifier = Modifier.fillMaxWidth(),
                    valueTag = "run-result-distance", unitWeight = FontWeight.Normal,
                )
                if (note != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(note, color = p.label, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.testTag("run-result-note"))
                }
            }
        },
        stage = {
            val mapShape = RoundedCornerShape(16.dp)
            if (hasRoute) {
                Box(Modifier.fillMaxSize().clip(mapShape).border(1.dp, p.cardEdge, mapShape).testTag("run-result-map")) {
                    LiveRouteMap(points = track, modifier = Modifier.fillMaxSize())
                }
            } else {
                Box(
                    Modifier.fillMaxSize().clip(mapShape).background(p.panel, mapShape).border(1.dp, p.cardEdge, mapShape)
                        .testTag("run-result-no-route"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.run_route_unavailable), color = p.muted, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
        },
        bottom = {
            Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                ResultStats(time, paceSec, speedKmh)
                if (shoe != null) {
                    ResultDivider()
                    ResultShoe(shoe)
                }
                ResultDivider()
                ResultRewardRow(reward)
            }
        },
    )
}

@Composable
private fun ResultDivider() {
    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp).height(1.dp).background(runPalette().divider))
}

/** 러닝 시간 · 평균 페이스 · 평균 속도 — 같은 폭 세 칸, 사이에 가는 세로선 */
@Composable
private fun ResultStats(time: String, paceSec: Long?, speedKmh: Double?) {
    DividedRow(columns = 3, gap = 20.dp, modifier = Modifier.testTag("run-result-stats")) {
        ResultStat(stringResource(R.string.runflow_time_label), time, null, Modifier.weight(1f), "run-result-time")
        ResultStat(stringResource(R.string.run_avg_pace), paceSec?.let { formatPace(it) } ?: "—", "/km", Modifier.weight(1f), "run-result-pace")
        ResultStat(stringResource(R.string.analytics_avg_speed), speedKmh?.let { "%.1f".format(it) } ?: "—",
            stringResource(R.string.run_speed_unit), Modifier.weight(1f), "run-result-speed")
    }
}

@Composable
private fun ResultStat(label: String, value: String, unit: String?, modifier: Modifier, tag: String) {
    val p = runPalette()
    Column(modifier.semantics(mergeDescendants = true) {}.testTag(tag)) {
        Text(label, color = p.label, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        FitNumber(
            value, unit, valueSize = 28.sp, unitSize = 14.sp, valueColor = p.text, unitColor = p.label,
            modifier = Modifier.fillMaxWidth(), unitWeight = FontWeight.Normal,
        )
    }
}

/** 신고 달린 신발 — 그림(틀 없이) · 이름 끝 등급 배지 · 내구도 수 · 막대 */
@Composable
private fun ResultShoe(shoe: Sneaker) {
    val p = runPalette()
    val points = durabilityPoints(shoe)
    Row(Modifier.fillMaxWidth().testTag("run-result-shoe"), verticalAlignment = Alignment.CenterVertically) {
        ShoeArtThumbnail(shoe, Modifier.width(120.dp).height(68.dp).testTag("run-result-shoe-image"))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            ShoeNameWithBadge(
                name = shoe.shoeName(), tier = shoe.tier,
                style = TextStyle(color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp),
                maxLines = 2, modifier = Modifier.fillMaxWidth().testTag("run-result-shoe-name"),
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(stringResource(R.string.sdv_stat_durability), color = p.label, fontSize = 13.sp,
                    modifier = Modifier.alignByBaseline())
                Spacer(Modifier.width(8.dp))
                Text("$points", color = p.text, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.alignByBaseline().testTag("run-result-durability"))
                Text(" / 100", color = p.label, fontSize = 13.sp, modifier = Modifier.alignByBaseline())
            }
            Spacer(Modifier.height(6.dp))
            RunBar(points / 100f, Modifier.fillMaxWidth().height(8.dp))
        }
    }
}

/** 보상 — 상자 표 · 이름("예상 보상" / "보상") · 수 SUP · 오른쪽 정산 상태 알약. 아래에 까닭 한두 줄 */
@Composable
private fun ResultRewardRow(reward: ResultReward) {
    val p = runPalette()
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(p.emblemFace), contentAlignment = Alignment.Center) {
                CubeGlyph(p.emblemInk, Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(reward.label, color = p.label, fontSize = 13.sp, maxLines = 1, modifier = Modifier.testTag("run-result-reward-label"))
                FitNumber(
                    reward.amount, "SUP", valueSize = 30.sp, unitSize = 24.sp, valueColor = p.text, unitColor = p.label,
                    modifier = Modifier.fillMaxWidth().testTag("run-result-reward"), unitWeight = FontWeight.Normal,
                )
            }
            Spacer(Modifier.width(8.dp))
            SettleChip(reward.settle)
        }
        reward.notes.forEach { line ->
            Spacer(Modifier.height(6.dp))
            Text(line, color = p.muted, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.testTag("run-result-reward-note"))
        }
        // 로그인이 풀려 올리지 못했다 — 카드 안에 둔다(지도 높이를 셈할 때 함께 잰다)
        if (reward.signIn) {
            Spacer(Modifier.height(8.dp))
            com.stepup.android.ui.components.SignInAgainButton()
        }
    }
}

/** 정산 상태 알약 — 정산 대기(시계) · 적립 완료 · 무효 · 지급 안 됨. 누르는 곳이 아니다 */
@Composable
private fun SettleChip(settle: Settle) {
    val p = runPalette()
    val shape = RoundedCornerShape(50)
    val (text, color) = when (settle) {
        Settle.PENDING -> stringResource(R.string.result_settle_pending) to p.chipText
        Settle.DONE -> stringResource(R.string.result_settle_done) to p.doneText
        Settle.VOID -> stringResource(R.string.result_settle_void) to p.stopMark
        Settle.NOT_PAID -> stringResource(R.string.result_settle_rejected) to p.chipText
    }
    Row(
        Modifier.heightIn(min = 34.dp).clip(shape).border(1.dp, p.chipEdge, shape)
            .padding(horizontal = 12.dp, vertical = 6.dp).testTag("run-result-settle-${settle.name.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (settle == Settle.PENDING) {
            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = color, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/** 보상 상자 표 — 육각형 윤곽과 가운데에서 갈라지는 세 선(정육면체) */
@Composable
private fun CubeGlyph(color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2f * 0.92f
        val c = Offset(size.width / 2f, size.height / 2f)
        val pts = (0 until 6).map { i ->
            val a = Math.toRadians((-90.0 + i * 60.0))
            Offset(c.x + r * kotlin.math.cos(a).toFloat(), c.y + r * kotlin.math.sin(a).toFloat())
        }
        val stroke = Stroke(width = size.minDimension * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val outline = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until 6) lineTo(pts[i].x, pts[i].y)
            close()
        }
        drawPath(outline, color, style = stroke)
        // 윗면 · 두 옆면이 만나는 선 — 가운데에서 위 왼쪽 · 위 오른쪽 · 아래로
        val inner = Path().apply {
            moveTo(pts[5].x, pts[5].y); lineTo(c.x, c.y); lineTo(pts[1].x, pts[1].y)
            moveTo(c.x, c.y); lineTo(pts[3].x, pts[3].y)
        }
        drawPath(inner, color, style = stroke)
    }
}

/** 결과 아래 보조 버튼 — 어두운 면 · 테두리 · 아이콘 + 글자(이미지 저장 · 공유하기) */
@Composable
internal fun RunOutlineButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val p = runPalette()
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier.heightIn(min = 52.dp).clip(shape).background(p.ghostFace, shape).border(1.dp, p.ghostEdge, shape)
            .feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val ink = if (enabled) p.text else p.muted
        Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = ink, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 머리 오른쪽 닫기(✕) — 어두운 원 */
@Composable
internal fun RunCloseButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = runPalette()
    Box(
        modifier.size(48.dp).clip(CircleShape).feedbackClickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(p.ghostFace), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Close, contentDescription = null, tint = p.text, modifier = Modifier.size(22.dp))
        }
    }
}
