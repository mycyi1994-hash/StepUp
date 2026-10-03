package com.stepup.android.ui.screens.records

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.ui.components.RunBackdrop
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunButtonKind
import com.stepup.android.ui.components.RunSpec
import com.stepup.android.ui.components.RunStateArt
import com.stepup.android.ui.components.RunTopBar
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpSans
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

/*
 * 내 러닝 기록(러닝 전체 리메이크 2026-10-02 · 시안 H01–H16)의 공통 조각 — 남색 러닝 바닥 · 머리(뒤로 · 로고) ·
 * 거리 · 시간 · 페이스 글자 · 빈 · 못 읽음 안내. 색과 버튼은 러닝 화면과 같은 RunStyle 이다.
 */

/**
 * 기록 화면 틀 — 남색 바닥 · 머리(뒤로 · 공식 로고 · 오른쪽 행동 하나). 상태 막대 · 하단 탭 자리는 앱 틀이 이미 비웠다.
 */
@Composable
internal fun RecordsFrame(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        RunBackdrop(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize()) {
            RunTopBar(onBack = onBack, trailing = trailing)
            content()
        }
    }
}

/** 제목 줄 오른쪽 글자 이동 — "통계 >". 누르는 곳은 48dp 이상 */
@Composable
internal fun HeaderLink(text: String, tag: String, onClick: () -> Unit) {
    val t = runTone()
    Row(
        Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
            .feedbackClickable(role = Role.Button, onClick = onClick)
            .padding(start = 8.dp, end = 2.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = runTextStyle(16.sp, t.label, FontWeight.SemiBold))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = t.label, modifier = Modifier.size(22.dp))
    }
}

/** 큰 굵은 제목(기울이지 않음) — "내 러닝 기록" · "러닝 통계" · "지난 러닝" */
@Composable
internal fun RecordTitle(text: String, modifier: Modifier = Modifier) {
    val t = runTone()
    Text(
        text,
        style = TextStyle(
            fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 35.sp, lineHeight = 1.2.em,
            letterSpacing = (-0.03).em, color = t.text,
        ),
        modifier = modifier.semantics { heading() },
    )
}

/** 거리(km) 한 자리 소수 — 1,284.5(합계 · 막대) */
internal fun km(meters: Double): String = String.format(Locale.ROOT, "%,.1f", meters / 1000.0)

/** 러닝 한 번의 거리(km) 두 자리 소수 — 2.40(목록 줄 · 상세 · 경로, 러닝 결과와 같은 자리수) */
internal fun km2(meters: Double): String = String.format(Locale.ROOT, "%,.2f", meters / 1000.0)

/** 운동 시간 — 1시간 43분 · 22분 24초 · 45초 */
@Composable
internal fun durationText(seconds: Long): String {
    val s = seconds.coerceAtLeast(0)
    return when {
        s >= 3600 -> stringResource(R.string.rec_hours_minutes, (s / 3600).toInt(), ((s % 3600) / 60).toInt())
        s >= 60 -> stringResource(R.string.rec_minutes_seconds, (s / 60).toInt(), (s % 60).toInt())
        else -> stringResource(R.string.rec_seconds, s.toInt())
    }
}

/** 시계 모양 시간 — 22:24(분:초) · 1:05:12(시:분:초) */
internal fun clockText(seconds: Long): String {
    val s = seconds.coerceAtLeast(0)
    return if (s >= 3600) String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
    else String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60)
}

/** 페이스(초/km) — 7'00" /km. 목록 줄처럼 좁은 곳은 [compact] 로 7'00"/km */
internal fun paceText(secondsPerKm: Double?, compact: Boolean = false): String? {
    val total = secondsPerKm?.takeIf { it.isFinite() && it > 0 }?.roundToLong() ?: return null
    return String.format(Locale.ROOT, "%d'%02d\"%s/km", total / 60, total % 60, if (compact) "" else " ")
}

/** 한 러닝의 페이스 — 거리와 시간이 모두 있을 때만 */
internal fun runPace(seconds: Long, meters: Double, compact: Boolean = false): String? =
    if (seconds > 0 && meters > 0) paceText(seconds / (meters / 1000.0), compact) else null

/** 페이스 숫자만 — 8'20"(단위 "/km" 는 작은 글자로 따로) */
internal fun paceValue(seconds: Long, meters: Double): String? {
    if (seconds <= 0 || meters <= 0) return null
    val total = (seconds / (meters / 1000.0)).takeIf { it.isFinite() && it > 0 }?.roundToLong() ?: return null
    return String.format(Locale.ROOT, "%d'%02d\"", total / 60, total % 60)
}

/** 평균 속도(km/h) 한 자리 — 거리와 시간이 모두 있을 때만 */
internal fun speedText(seconds: Long, meters: Double): String? =
    if (seconds > 0 && meters > 0) String.format(Locale.ROOT, "%.1f", (meters / 1000.0) / (seconds / 3600.0)) else null

@Composable
internal fun rememberFormatter(@StringRes pattern: Int): DateTimeFormatter {
    val text = stringResource(pattern)
    val locale = LocalConfiguration.current.locales[0]
    return remember(text, locale) { DateTimeFormatter.ofPattern(text, locale) }
}

/** 빈 · 못 읽음 그림 — 첫 기록 전(문서 위 러너) · 기간 빔(위치 핀) · 못 읽음(신호 + 느낌표) · 찾을 수 없음 */
internal enum class RecordArt { Empty, Period, Offline, Missing }

/**
 * 빈 기록 · 못 읽음(H07 · H08 · H10) — 그림 · 큰 제목 · 한두 줄 · 아래 버튼(주 하나 + 돌아가기).
 * [scroll] 이면 남은 자리를 채워 글은 가운데, 버튼은 아래에 두고 큰 글씨면 넘긴다. 아니면(통계 안) 그 자리에 차례로.
 */
@Composable
internal fun RecordsState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    actionTag: String = "records-state-action",
    refresh: Boolean = false,
    scroll: Boolean = true,
    art: RecordArt = if (refresh) RecordArt.Offline else RecordArt.Empty,
    actionIcon: ImageVector? = null,
    backLabel: String? = null,
    onBackAction: () -> Unit = {},
    backTag: String = "records-state-back",
    big: Boolean = false,
    /** 맨 위에 둘 것(H08 의 빈 합계 카드) — 글은 그 아래 남은 자리 가운데 */
    header: (@Composable () -> Unit)? = null,
) {
    val t = runTone()
    val message = @Composable {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            StateArt(art)
            Spacer(Modifier.height(if (big) 34.dp else 22.dp))
            Text(
                title,
                style = TextStyle(
                    fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = if (big) 32.sp else 24.sp,
                    lineHeight = 1.28.em, letterSpacing = (-0.03).em, color = t.text,
                ),
                textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(10.dp))
            Text(body, style = runTextStyle(16.sp, t.label, FontWeight.Medium, 1.5f), textAlign = TextAlign.Center)
        }
    }
    val buttons = @Composable {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (actionLabel != null) {
                RunButton(
                    actionLabel, onAction, Modifier.testTag(actionTag), icon = actionIcon,
                    hero = actionIcon != null, italic = true,
                )
            }
            if (backLabel != null) {
                RunButton(backLabel, onBackAction, Modifier.testTag(backTag), kind = RunButtonKind.Secondary)
            }
        }
    }
    if (scroll) {
        BoxWithConstraints(modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight)
                    .padding(horizontal = RunSpec.Gutter).padding(top = 12.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (header != null) header() else Spacer(Modifier.height(1.dp))
                Box(Modifier.padding(vertical = 24.dp)) { message() }
                buttons()
            }
        }
    } else {
        Column(
            modifier.fillMaxWidth().padding(top = 28.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            header?.invoke()
            message()
            buttons()
        }
    }
}

@Composable
private fun StateArt(art: RecordArt) {
    when (art) {
        RecordArt.Empty -> RecordSheetArt()
        RecordArt.Period -> PinArt()
        RecordArt.Offline -> RunStateArt(Icons.Filled.Wifi, size = 150.dp, badge = Icons.Filled.PriorityHigh)
        RecordArt.Missing -> RunStateArt(Icons.Outlined.SearchOff, size = 132.dp)
    }
}

/** 첫 기록 전(H07) — 빛나는 기록지 위 러너와 세 줄. 기록이 쌓일 자리라는 뜻이다(숫자 · 지도는 그리지 않는다) */
@Composable
private fun RecordSheetArt() {
    val t = runTone()
    val ink = if (t.dark) Color(0xFF6FA2FF) else t.cobalt
    Box(
        Modifier.size(width = 168.dp, height = 210.dp).drawBehind {
            drawCircle(
                Brush.radialGradient(
                    0f to t.cobalt.copy(alpha = if (t.dark) 0.30f else 0.14f), 1f to Color.Transparent,
                    center = center, radius = size.minDimension * 0.75f,
                ),
                radius = size.minDimension * 0.75f,
            )
            val w = size.width * 0.82f
            val h = size.height * 0.92f
            val left = (size.width - w) / 2
            val top = (size.height - h) / 2
            val fold = w * 0.2f
            val stroke = 4.dp.toPx()
            val outline = androidx.compose.ui.graphics.Path().apply {
                moveTo(left + 14.dp.toPx(), top)
                lineTo(left + w - fold, top)
                lineTo(left + w, top + fold)
                lineTo(left + w, top + h - 14.dp.toPx())
                quadraticTo(left + w, top + h, left + w - 14.dp.toPx(), top + h)
                lineTo(left + 14.dp.toPx(), top + h)
                quadraticTo(left, top + h, left, top + h - 14.dp.toPx())
                lineTo(left, top + 14.dp.toPx())
                quadraticTo(left, top, left + 14.dp.toPx(), top)
                close()
            }
            drawPath(outline, (if (t.dark) Color(0xFF0B2C57) else Color(0xFFE5EEFF)).copy(alpha = 0.85f))
            drawPath(outline, ink, style = Stroke(width = stroke, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            val corner = androidx.compose.ui.graphics.Path().apply {
                moveTo(left + w - fold, top)
                lineTo(left + w - fold, top + fold)
                lineTo(left + w, top + fold)
            }
            drawPath(corner, ink, style = Stroke(width = stroke, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            // 세 줄 — 점과 선
            repeat(3) { i ->
                val y = top + h * (0.60f + i * 0.13f)
                drawCircle(ink, radius = 6.dp.toPx(), center = Offset(left + w * 0.2f, y))
                drawRoundRect(
                    ink, topLeft = Offset(left + w * 0.32f, y - 2.5.dp.toPx()),
                    size = Size(w * 0.5f, 5.dp.toPx()), cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
        },
    ) {
        Icon(
            Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, tint = ink,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 34.dp).size(64.dp),
        )
    }
}

/** 기간 빔(H08) — 위치 핀과 바닥 그림자 */
@Composable
private fun PinArt() {
    val t = runTone()
    Box(Modifier.size(width = 96.dp, height = 104.dp), contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.matchParentSize()) {
            drawOval(
                t.cobalt.copy(alpha = 0.35f), topLeft = Offset(size.width * 0.22f, size.height * 0.82f),
                size = Size(size.width * 0.56f, size.height * 0.14f),
            )
        }
        Icon(Icons.Outlined.Place, contentDescription = null, tint = if (t.dark) Color(0xFF4D8BFF) else t.cobalt, modifier = Modifier.size(88.dp))
    }
}

/** 작은 막대 그림 — 통계가 없는 축 가운데(H15) */
@Composable
internal fun BarsGlyph(modifier: Modifier = Modifier) {
    val t = runTone()
    Canvas(modifier.size(34.dp)) {
        val w = size.width
        val h = size.height
        val bar = w * 0.18f
        listOf(0.45f, 0.7f, 1f).forEachIndexed { i, f ->
            val x = w * 0.14f + i * (bar + w * 0.12f)
            drawLine(
                t.label.copy(alpha = 0.85f), Offset(x + bar / 2, h), Offset(x + bar / 2, h * (1f - f)),
                strokeWidth = bar, cap = StrokeCap.Round,
            )
        }
    }
}
