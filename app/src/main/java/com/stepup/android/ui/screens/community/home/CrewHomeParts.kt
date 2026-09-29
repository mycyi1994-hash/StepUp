package com.stepup.android.ui.screens.community.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.CrewHomeRules
import com.stepup.android.domain.CrewRules
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.MeetingFace
import com.stepup.android.ui.components.LiveRouteMap
import com.stepup.android.ui.components.StepUpMap
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.community.chat.ChatFace
import com.stepup.android.ui.screens.community.crew.CrewGutter
import com.stepup.android.ui.screens.community.crew.CrewSkeletonBox
import com.stepup.android.ui.screens.community.crew.crewInk
import com.stepup.android.ui.screens.community.stories.rangeSpan
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DayStyle
import java.util.Locale

/*
 * 내 크루 홈(확정 4번) — 화면들이 함께 쓰는 부품. 색 · 여백은 크루 명함형(crewInk · 좌우 24)과 같은 값을 쓴다.
 * 시안의 지도 도식은 실제 지도 타일(StepUpMap)과 서버 좌표로 바꿨다 — 좌표가 없으면 지도를 그리지 않는다.
 */

@Composable
internal fun homeLocale(): Locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()

/** "9월 29일" — 언어별 짧은 날짜 */
@Composable
internal fun homeDate(day: LocalDate): String {
    val locale = homeLocale()
    return remember(day, locale) {
        day.format(DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(locale, "MMMd"), locale))
    }
}

/** "9월 29일 화요일" */
@Composable
internal fun homeDateWeekday(day: LocalDate): String {
    val locale = homeLocale()
    return remember(day, locale) {
        day.format(DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(locale, "MMMMdEEEE"), locale))
    }
}

/** "9.28" — 주간 기록 제목 */
@Composable
internal fun homeShortDate(day: LocalDate): String {
    val locale = homeLocale()
    return remember(day, locale) {
        day.format(DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(locale, "Md"), locale))
            .let { if (locale.language == "ko") "${day.monthValue}.${day.dayOfMonth}" else it }
    }
}

internal fun localDay(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

internal fun homeClock(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(millis).atZone(zone).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))

/** 모임 날짜 — 오늘 · 내일 · "9월 30일" */
@Composable
internal fun meetingDayLabel(meetAt: Long): String = when (CrewHomeRules.whenOf(meetAt, System.currentTimeMillis(), ZoneId.systemDefault())) {
    CrewHomeRules.When.TODAY -> stringResource(R.string.crewhome_today)
    CrewHomeRules.When.TOMORROW -> stringResource(R.string.crewhome_tomorrow)
    CrewHomeRules.When.LATER -> homeDate(localDay(meetAt))
}

/** 기록 날짜 — 오늘 · 어제 · "9월 27일" */
@Composable
internal fun recordDayLabel(millis: Long): String {
    val day = localDay(millis)
    val today = LocalDate.now()
    return when (day) {
        today -> stringResource(R.string.crewhome_today)
        today.minusDays(1) -> stringResource(R.string.crewhome_yesterday)
        else -> homeDate(day)
    }
}

/** "33분 36초" · 한 시간 넘으면 "1시간 2분" */
@Composable
internal fun durationLong(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    return if (s >= 3600) stringResource(R.string.crewhome_duration_hour, s / 3600, s % 3600 / 60)
    else stringResource(R.string.crewhome_duration_min, s / 60, s % 60)
}

/** 요일 이름(월 … 일) — 앱 언어로 */
@Composable
internal fun weekdayLabels(): List<String> {
    val locale = homeLocale()
    return remember(locale) {
        java.time.DayOfWeek.entries.map { it.getDisplayName(if (locale.language == "en") DayStyle.SHORT else DayStyle.NARROW, locale) }
    }
}

/** 한국어 받침 — "퇴근런이" / "공덕크루가" 를 고른다(다른 언어는 두 문구가 같다) */
internal fun endsWithConsonant(word: String): Boolean {
    val last = word.trim().lastOrNull() ?: return true
    return if (last in '가'..'힣') (last.code - 0xAC00) % 28 != 0 else true
}

internal fun km(value: Double): String = CrewRules.km(value)

/** 작은 푸른 소제목(시안의 "다음 러닝" · "퇴근런") */
@Composable
internal fun HomeLabel(text: String, modifier: Modifier = Modifier) {
    val ink = crewInk()
    Text(text, color = ink.info, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = modifier)
}

@Composable
internal fun HomeDivider(modifier: Modifier = Modifier) {
    val ink = crewInk()
    Box(modifier.fillMaxWidth().height(0.7.dp).background(ink.divider))
}

/** 얼굴 셋 겹치기 — 진행자(크루장이면 따뜻한 색) 먼저 */
@Composable
internal fun FaceStack(faces: List<MeetingFace>, hostId: String, hostOwner: Boolean, modifier: Modifier = Modifier) {
    val ink = crewInk()
    Row(modifier) {
        faces.take(3).forEachIndexed { index, face ->
            ChatFace(
                face.name, owner = hostOwner && face.userId == hostId, size = 30.dp,
                modifier = Modifier.offset(x = (-8 * index).dp).border(1.5.dp, ink.canvas, CircleShape),
            )
        }
    }
}

/** 요일별 거리 막대(월 → 일) — 값이 있는 날은 숫자와 막대, 오지 않은 날은 "—"(0km 와 다르다) */
@Composable
internal fun WeekBars(days: List<Double?>, today: Int?, modifier: Modifier = Modifier, height: Dp = 132.dp, onDay: ((Int) -> Unit)? = null) {
    val ink = crewInk()
    val labels = weekdayLabels()
    val bars = CrewHomeRules.bars(days)
    Row(modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        days.forEachIndexed { index, value ->
            val fraction = bars.getOrNull(index)
            val clickable = onDay != null && value != null
            Column(
                Modifier.weight(1f).fillMaxHeight()
                    .then(if (clickable) Modifier.homeClickable { onDay?.invoke(index) } else Modifier)
                    .testTag("home-week-day-$index"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(if (value == null) "—" else km(value), color = ink.secondary, fontSize = 11.sp, maxLines = 1)
                Spacer(Modifier.height(6.dp))
                val barHeight = (height.value - 48f) * (fraction ?: 0f)
                val filled = value != null && value > 0.0
                Box(
                    Modifier.width(28.dp).height(if (filled) barHeight.coerceAtLeast(6f).dp else 3.dp)
                        .clip(RoundedCornerShape(if (filled) 7.dp else 2.dp))
                        .background(
                            when {
                                !filled && value == null -> ink.track
                                !filled -> ink.bar.copy(alpha = 0.55f)
                                index == today -> ink.text
                                else -> ink.bar
                            },
                        ),
                )
                Spacer(Modifier.height(10.dp))
                Text(labels.getOrElse(index) { "" }, color = ink.secondary, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

/** 누르는 곳 — 앱의 공통 누름(소리 · 떨림 설정을 따른다) */
@Composable
internal fun Modifier.homeClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier =
    this.clip(RoundedCornerShape(10.dp)).feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick)

/** 모임 장소 지도 — 실제 지도 타일 위에 모이는 곳 표시와 이름. 좌표가 없으면 그리지 않는다 */
@Composable
internal fun MeetingMap(point: GeoPoint, label: String, modifier: Modifier = Modifier, spanMeters: Int = 260) {
    val ink = crewInk()
    val measurer = rememberTextMeasurer()
    val style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    val dot = Color(0xFF4F86FF)
    Box(modifier.clip(RoundedCornerShape(18.dp)).background(ink.card).testTag("home-meeting-map")) {
        StepUpMap(focus = remember(point, spanMeters) { rangeSpan(point, spanMeters) }, seed = 4) { plan ->
            val at = plan.toScreen(point)
            drawCircle(dot.copy(alpha = 0.25f), radius = 16.dp.toPx(), center = at)
            drawCircle(Color.White, radius = 8.dp.toPx(), center = at)
            drawCircle(dot, radius = 5.5f.dp.toPx(), center = at)
            if (label.isNotBlank()) {
                val text = measurer.measure(label.take(24), style)
                val padX = 8.dp.toPx()
                val padY = 4.dp.toPx()
                val size = Size(text.size.width + padX * 2, text.size.height + padY * 2)
                val topLeft = Offset(at.x + 14.dp.toPx(), at.y - size.height / 2f)
                drawRoundRect(Color(0xCC0C1729), topLeft = topLeft, size = size, cornerRadius = CornerRadius(size.height / 2f))
                drawText(text, topLeft = Offset(topLeft.x + padX, topLeft.y + padY))
            }
        }
    }
}

/** 러닝 코스 지도 — 서버가 처음과 끝 300m 를 뗀 코스 */
@Composable
internal fun RunRouteMap(route: List<GeoPoint>, modifier: Modifier = Modifier) {
    val ink = crewInk()
    Box(modifier.clip(RoundedCornerShape(18.dp)).background(ink.card).testTag("home-run-map")) {
        LiveRouteMap(points = route, seed = 5)
    }
}

/** 처음 읽는 동안의 자리 — 이전 크루 · 다른 모임의 값을 먼저 보이지 않는다 */
@Composable
internal fun HomeSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = CrewGutter).testTag("home-loading")) {
        Spacer(Modifier.height(20.dp))
        CrewSkeletonBox(Modifier.fillMaxWidth().height(258.dp), 24.dp)
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CrewSkeletonBox(Modifier.weight(1.45f).height(148.dp), 24.dp)
            CrewSkeletonBox(Modifier.weight(1f).height(148.dp), 24.dp)
        }
        Spacer(Modifier.height(28.dp))
        CrewSkeletonBox(Modifier.fillMaxWidth().height(96.dp), 16.dp)
    }
}

/** 한 줄 값 — 이름 왼쪽, 값 오른쪽, 아래 선(01 · 05 · 06) */
@Composable
internal fun HomeValueRow(label: String, value: String, modifier: Modifier = Modifier) {
    val ink = crewInk()
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 66.dp).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = ink.secondary, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(value, color = ink.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.padding(start = 16.dp))
        }
        HomeDivider()
    }
}

/** 동그라미 점(작은 원) — 요일 막대가 비었을 때 등 */
@Composable
internal fun HomeDot(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(6.dp)) { drawCircle(color) }
}

/** 말풍선 선 그림(크루 채팅 카드의 오른쪽 아래) */
@Composable
internal fun ChatBubbleIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val s = size.width / 24f
        val stroke = Stroke(width = 1.7f * s, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(4f * s, 5f * s)
            lineTo(20f * s, 5f * s)
            lineTo(20f * s, 16f * s)
            lineTo(10f * s, 16f * s)
            lineTo(6f * s, 19.5f * s)
            lineTo(6f * s, 16f * s)
            lineTo(4f * s, 16f * s)
            close()
        }
        drawPath(path, color, style = stroke)
    }
}

/** 이름 찾기 칸 — 돋보기 · 입력(모서리 14 · 52dp) */
@Composable
internal fun HomeSearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val ink = crewInk()
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(fontFamily = com.stepup.android.ui.theme.StepUpSans, color = ink.text, fontSize = 15.sp),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(ink.info),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(14.dp)).background(ink.card)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.Icon(
                    androidx.compose.material.icons.Icons.Filled.Search, null, tint = ink.secondary, modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(14.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, color = ink.secondary, fontSize = 15.sp, maxLines = 1)
                    inner()
                }
            }
        },
    )
}
