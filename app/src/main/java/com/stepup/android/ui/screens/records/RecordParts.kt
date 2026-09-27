package com.stepup.android.ui.screens.records

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.ui.components.SettingsPrimaryButton
import com.stepup.android.ui.components.settingsPalette
import com.stepup.android.ui.experience.feedbackClickable
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

/*
 * 내 러닝 기록(2026-09-28 전달본, docs/redesign/running-records)의 공통 조각 — 머리 오른쪽 글자 버튼 · 거리 · 시간 · 페이스
 * 글자 · 빈 · 못 읽음 안내. 색은 설정 · 알림과 같은 전달본 토큰(settingsPalette).
 */

/** 머리 오른쪽 글자 버튼 — "통계". 누르는 곳은 48dp 이상 */
@Composable
internal fun HeaderLink(text: String, tag: String, onClick: () -> Unit) {
    val p = settingsPalette()
    Box(
        Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp).feedbackClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp).testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = p.accent, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

/** 거리(km) 한 자리 소수 — 1,284.5 */
internal fun km(meters: Double): String = String.format(Locale.ROOT, "%,.1f", meters / 1000.0)

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

/** 페이스(초/km) — 7'00" /km. 목록 줄처럼 좁은 곳은 [compact] 로 7'00"/km */
internal fun paceText(secondsPerKm: Double?, compact: Boolean = false): String? {
    val total = secondsPerKm?.takeIf { it.isFinite() && it > 0 }?.roundToLong() ?: return null
    return String.format(Locale.ROOT, "%d'%02d\"%s/km", total / 60, total % 60, if (compact) "" else " ")
}

/** 한 러닝의 페이스 — 거리와 시간이 모두 있을 때만 */
internal fun runPace(seconds: Long, meters: Double, compact: Boolean = false): String? =
    if (seconds > 0 && meters > 0) paceText(seconds / (meters / 1000.0), compact) else null

@Composable
internal fun rememberFormatter(@StringRes pattern: Int): DateTimeFormatter {
    val text = stringResource(pattern)
    val locale = LocalConfiguration.current.locales[0]
    return remember(text, locale) { DateTimeFormatter.ofPattern(text, locale) }
}

/** 빈 기록 · 못 읽음 — 가운데 그림 · 제목 · 한 줄 · 버튼 하나. 큰 글씨면 스크롤한다 */
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
) {
    val p = settingsPalette()
    Column(
        modifier.fillMaxWidth().then(if (scroll) Modifier.fillMaxSize().verticalScroll(rememberScrollState()) else Modifier)
            .padding(top = 88.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(66.dp).clip(RoundedCornerShape(20.dp)).background(p.surface), contentAlignment = Alignment.Center) {
            if (refresh) RefreshGlyph() else PathGlyph()
        }
        Text(title, color = p.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() })
        Text(body, color = p.secondary, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 1.5.em)
        if (actionLabel != null) {
            Spacer(Modifier.height(10.dp))
            SettingsPrimaryButton(actionLabel, onAction, Modifier.fillMaxWidth().testTag(actionTag))
        }
    }
}

/** 러닝 패스 모양의 작은 그림 — 출발점에서 한 바퀴 도는 길 */
@Composable
private fun PathGlyph() {
    val p = settingsPalette()
    Canvas(Modifier.size(30.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.12f, h * 0.88f)
            cubicTo(w * 0.30f, h * 0.80f, w * 0.72f, h * 0.84f, w * 0.86f, h * 0.60f)
            cubicTo(w * 1.02f, h * 0.30f, w * 0.70f, h * 0.04f, w * 0.44f, h * 0.18f)
            cubicTo(w * 0.24f, h * 0.30f, w * 0.26f, h * 0.60f, w * 0.40f, h * 0.60f)
        }
        drawPath(path, p.accent, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(p.accent, radius = 3.dp.toPx(), center = Offset(w * 0.12f, h * 0.88f))
        drawCircle(p.accent, radius = 3.4.dp.toPx(), center = Offset(w * 0.40f, h * 0.60f), style = Stroke(1.8.dp.toPx()))
    }
}

@Composable
private fun RefreshGlyph() {
    val p = settingsPalette()
    Icon(Icons.Filled.Refresh, contentDescription = null, tint = p.accent, modifier = Modifier.size(28.dp))
}
