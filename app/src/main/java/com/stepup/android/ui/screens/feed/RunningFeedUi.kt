package com.stepup.android.ui.screens.feed

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.IconToggleButton
import com.stepup.android.ui.components.FormField
import com.stepup.android.ui.components.DarkIconButton
import com.stepup.android.ui.components.GhostButton
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import com.stepup.android.ui.theme.VoltText
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.data.remote.EventRow
import com.stepup.android.data.remote.NewsRow
import com.stepup.android.data.remote.RunningFeedApi
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.quietClickable
import com.stepup.android.ui.theme.Alert
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 러닝 이벤트·뉴스 화면이 함께 쓰는 조각들.
 *
 * ── 이 파일이 지키는 것 ──
 *
 * **모르는 것을 아는 척하지 않는다.** 날짜가 없으면 "날짜 미정"이라고 적고,
 * 접수 상태를 확인한 지 오래됐으면 "접수 상태 확인 필요"로 바꾼다. 화면이
 * 자신 있게 "접수 중"이라고 말했는데 실제로는 마감이었다면, 그 사람은 헛걸음을
 * 한 것이다.
 */

private val DAY = DateTimeFormatter.ofPattern("M월 d일 (E)")
private val DAY_SHORT = DateTimeFormatter.ofPattern("M.d")

/** 접수 상태를 확인한 지 이만큼 지나면 확정으로 말하지 않는다. */
private const val STALE_DAYS = 14L

fun parseDay(iso: String?): LocalDate? =
    iso?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }

fun parseMoment(iso: String?): OffsetDateTime? =
    iso?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }

@Composable
fun dayLabel(iso: String?): String =
    parseDay(iso)?.format(DAY) ?: stringResource(R.string.feed_date_unknown)

/**
 * 접수 상태를 사람 말로.
 *
 * 확인한 지 오래됐으면 원래 상태 대신 "확인 필요"를 돌려준다. 캐시가 오래된
 * 접수 상태를 확정 정보처럼 보여 주지 않기 위해서다.
 */
@Composable
fun registrationLabel(status: String, lastVerifiedAt: String?, cancelled: String): Pair<String, Boolean> {
    if (cancelled == "CANCELLED") return stringResource(R.string.feed_cancelled) to true
    if (cancelled == "POSTPONED") return stringResource(R.string.feed_postponed) to true

    val verified = parseMoment(lastVerifiedAt)
    val stale = verified == null ||
        Duration.between(verified.toInstant(), java.time.Instant.now()).toDays() > STALE_DAYS
    if (stale && status in setOf("OPEN", "UPCOMING")) {
        return stringResource(R.string.feed_status_recheck) to true
    }
    val res = when (status) {
        "OPEN" -> R.string.feed_status_open
        "UPCOMING" -> R.string.feed_status_upcoming
        "CLOSED" -> R.string.feed_status_closed
        "SOLD_OUT" -> R.string.feed_status_sold_out
        else -> R.string.feed_status_unknown
    }
    return stringResource(res) to (status !in setOf("OPEN", "UPCOMING"))
}

@StringRes
fun distanceRes(key: String): Int = when (key) {
    "LTE_5K" -> R.string.feed_distance_5k
    "10K" -> R.string.feed_distance_10k
    "HALF" -> R.string.feed_distance_half
    "FULL" -> R.string.feed_distance_full
    "ULTRA" -> R.string.feed_distance_ultra
    "OTHER" -> R.string.feed_distance_other
    RunningFeedApi.ALL -> R.string.feed_filter_all
    else -> R.string.feed_distance_unknown
}

@StringRes
fun eventTypeRes(key: String): Int = when (key) {
    "ROAD" -> R.string.feed_type_road
    "TRAIL" -> R.string.feed_type_trail
    "WALK" -> R.string.feed_type_walk
    "FUNRUN" -> R.string.feed_type_funrun
    "CLASS" -> R.string.feed_type_class
    RunningFeedApi.ALL -> R.string.feed_filter_all
    else -> R.string.feed_type_other
}

@StringRes
fun statusRes(key: String): Int = when (key) {
    "UPCOMING" -> R.string.feed_status_upcoming
    "OPEN" -> R.string.feed_status_open
    "CLOSED" -> R.string.feed_status_closed
    else -> R.string.feed_filter_all
}

@StringRes
fun categoryRes(key: String): Int = when (key) {
    "RUNNING" -> R.string.feed_topic_running
    "WALK_JOG" -> R.string.feed_topic_walk
    "TRAINING" -> R.string.feed_topic_training
    "INJURY" -> R.string.feed_topic_injury
    "HEALTH" -> R.string.feed_topic_health
    "RACE_NEWS" -> R.string.feed_topic_race
    "PUBLIC_HEALTH" -> R.string.feed_topic_public_health
    else -> R.string.feed_filter_all
}

/** 어느 버튼을 보여 줄지 — 링크가 정한다. 없는 접수처를 있다고 하지 않는다. */
@StringRes
fun destinationRes(key: String): Int = when (key) {
    "REGISTRATION" -> R.string.feed_go_register
    "OFFICIAL_INFO" -> R.string.feed_go_official
    else -> R.string.feed_go_source
}

// ── 조각 ────────────────────────────────────────────────────────────

/** 검색칸. Material 의 TextField 를 쓰지 않는 것은 앱의 다른 입력과 같은 모양을 지키려는 것이다. */
@Composable
fun FeedSearchField(
    value: String,
    hint: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** 칸 오른쪽 끝의 거르기 버튼 — 따로 줄을 차지하지 않는다 */
    onFilter: (() -> Unit)? = null,
    filterCount: Int = 0,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FormField(label = hint, value = value, onValueChange = onValueChange, modifier = Modifier.weight(1f))
        if (onFilter != null) {
            val label = stringResource(R.string.filter_button)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                DarkIconButton(Icons.Filled.Tune, if (filterCount > 0) "$label $filterCount" else label, onClick = onFilter)
                if (filterCount > 0) Text("$filterCount", style = MaterialTheme.typography.bodyMedium, color = VoltText)
            }
        }
    }
}

/**
 * 못 가져왔을 때.
 *
 * 무엇을 해야 하는지까지 적는다. "오류"만 적으면 할 수 있는 일이 없다.
 */
@Composable
fun FeedProblemNote(
    problem: FeedProblem,
    loadedAtMillis: Long,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Alert.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(
                if (problem == FeedProblem.REJECTED) R.string.feed_problem_rejected
                else R.string.feed_problem_offline
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = Silver,
            lineHeight = 21.sp,
        )
        if (loadedAtMillis > 0) {
            Text(
                text = stringResource(R.string.feed_showing_cached, timeLabel(loadedAtMillis)),
                fontSize = 14.sp,
                color = Slate,
            )
        }
        // 서버가 거절한 것은 다시 해도 같은 답이 온다. 그때는 다시 시도를
        // 내놓지 않는다 — 눌러도 달라지지 않는 버튼을 두는 것은 거짓말이다.
        if (onRetry != null && problem == FeedProblem.OFFLINE) {
            GhostButton(stringResource(R.string.feed_retry), onClick = onRetry, modifier = Modifier.fillMaxWidth())
        }
    }
}

/**
 * 아무것도 없을 때. 진짜 상태를 적는다 — 가짜 대회로 채우지 않는다.
 *
 * [action] 은 할 수 있는 일이 있을 때만 붙인다. 조건을 좁혀서 빈 것이라면
 * 조건을 푸는 것이 답이고, 아직 등록된 것이 없다면 누를 것이 없다.
 */
@Composable
fun FeedEmptyNote(
    @StringRes text: Int,
    @StringRes hint: Int? = null,
    action: Pair<Int, () -> Unit>? = null,
) {
    GlowCard(contentPadding = PaddingValues(18.dp), spacing = 6.dp) {
        Text(
            text = stringResource(text),
            style = MaterialTheme.typography.titleSmall,
            color = Snow,
        )
        if (hint != null) {
            Text(
                text = stringResource(hint),
                fontSize = 14.sp,
                color = Silver,
                lineHeight = 21.sp,
            )
        }
        if (action != null) {
            GhostButton(stringResource(action.first), onClick = action.second, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun timeLabel(millis: Long): String {
    val moment = java.time.Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    return moment.format(DateTimeFormatter.ofPattern("M.d HH:mm"))
}

/** 작은 표시 */
@Composable
fun FeedTag(text: String, accent: Boolean = false, warn: Boolean = false) {
    val tint = if (warn) Alert else if (accent) Volt else Slate
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (warn || accent) tint.copy(alpha = 0.14f) else CarbonHigh)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = tint)
    }
}

// ── 대회 카드 ───────────────────────────────────────────────────────

/**
 * 대회 한 장.
 *
 * 카드 본문을 누르면 그 대회의 바깥 페이지로 바로 간다. 관심 저장 버튼은
 * 따로 동작한다 — 저장하려다 브라우저가 열리면 놀란다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EventCard(row: EventRow, onOpen: () -> Unit, onToggleSave: () -> Unit, modifier: Modifier = Modifier) {
    val (statusText, statusMuted) = registrationLabel(row.registrationStatus, row.lastVerifiedAt, row.cancelled)
    GlowCard(modifier, contentPadding = PaddingValues(20.dp), spacing = 14.dp) {
        Text(dayLabel(row.eventDate), style = MaterialTheme.typography.bodyMedium, color = Silver)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(row.title, style = MaterialTheme.typography.titleLarge, color = Snow, modifier = Modifier.weight(1f).quietClickable(onOpen))
            FeedSaveButton(row.saved, onToggleSave)
        }
        Text(listOf(row.region, row.venue).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { stringResource(R.string.feed_place_unknown) }, style = MaterialTheme.typography.bodyMedium, color = Silver)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            row.disciplineNames.forEach { FeedTag(it) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isDemo(row.id)) FeedTag(stringResource(R.string.demo_example_event), accent = true)
            FeedTag(statusText, accent = !statusMuted, warn = statusMuted)
            if (row.feeMin != null) FeedTag(stringResource(R.string.feed_fee_from, "%,d".format(row.feeMin.toLong())))
        }
        if (row.organizer.isNotBlank()) Text(row.organizer, style = MaterialTheme.typography.bodyMedium, color = Silver)
        GhostButton(stringResource(destinationRes(row.destinationType)), onClick = onOpen, modifier = Modifier.fillMaxWidth())
        Text(
            if (row.lastVerifiedAt != null) stringResource(R.string.feed_last_checked, dayLabel(row.lastVerifiedAt))
            else stringResource(R.string.feed_last_checked_unknown),
            style = MaterialTheme.typography.bodyMedium, color = Silver,
        )
    }
}

@Composable
private fun FeedSaveButton(saved: Boolean, onToggle: () -> Unit) {
    IconToggleButton(saved, onCheckedChange = { onToggle() }, modifier = Modifier.size(48.dp)) {
        Icon(if (saved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            stringResource(if (saved) R.string.feed_unsave else R.string.feed_save), tint = if (saved) VoltText else Silver)
    }
}

// ── 뉴스 카드 ───────────────────────────────────────────────────────

/**
 * 기사 한 장.
 *
 * 제목은 원문 그대로 둔다. 눈에 띄게 고쳐 쓰면 그 기사가 하지 않은 말을
 * 우리가 하는 셈이 된다.
 *
 * 사진은 이용 권한이 확인된 것만 온다(서버가 걸러 보낸다). 그래서 사진 없이도
 * 카드가 완성되게 만들었다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewsCard(row: NewsRow, onOpen: () -> Unit, onToggleSave: () -> Unit, modifier: Modifier = Modifier) {
    GlowCard(modifier, contentPadding = PaddingValues(20.dp), spacing = 14.dp) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isDemo(row.id)) FeedTag(stringResource(R.string.demo_example_news), accent = true)
            FeedTag(stringResource(categoryRes(row.category)), accent = true)
        }
        Text(listOfNotNull(row.publisherName.ifBlank { row.publisherDomain }, parseDay(row.publishedAt)?.format(DAY_SHORT)).joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = Silver)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(row.title, style = MaterialTheme.typography.titleLarge, color = Snow, modifier = Modifier.weight(1f).quietClickable(onOpen))
            FeedSaveButton(row.saved, onToggleSave)
        }
        val body = row.summary ?: row.description.takeIf { it.isNotBlank() }
        if (body != null) {
            Text(body, style = MaterialTheme.typography.bodyLarge, color = Silver)
            Text(stringResource(if (row.summaryType == "AI_SUMMARY") R.string.feed_ai_summary else R.string.feed_search_description), style = MaterialTheme.typography.bodyMedium, color = Silver)
        }
        GhostButton(stringResource(R.string.feed_read_original), onClick = onOpen, modifier = Modifier.fillMaxWidth())
    }
}

// ── 달력 ────────────────────────────────────────────────────────────

/**
 * 월별 달력.
 *
 * 목록과 **같은 대회 데이터**를 쓴다. 따로 불러오면 거르기를 바꿨을 때 두
 * 화면이 다른 말을 한다.
 */
@Composable
fun EventCalendar(
    month: java.time.YearMonth,
    rows: List<EventRow>,
    picked: LocalDate?,
    onPick: (LocalDate) -> Unit,
    onShift: (Long) -> Unit,
) {
    val byDay = rows.mapNotNull { parseDay(it.eventDate) }.groupingBy { it }.eachCount()
    GlowCard(contentPadding = PaddingValues(14.dp), spacing = 10.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "‹",
                fontSize = 20.sp,
                color = Silver,
                modifier = Modifier
                    .size(44.dp)
                    .quietClickable { onShift(-1) }
                    .padding(top = 8.dp),
            )
            Text(
                text = "${month.year}. ${month.monthValue}",
                style = MaterialTheme.typography.titleSmall,
                color = Snow,
            )
            Text(
                text = "›",
                fontSize = 20.sp,
                color = Silver,
                modifier = Modifier
                    .size(44.dp)
                    .quietClickable { onShift(1) }
                    .padding(top = 8.dp),
            )
        }

        val first = month.atDay(1)
        // 월요일 시작. 한국 달력의 관례다.
        val lead = (first.dayOfWeek.value + 6) % 7
        val cells = lead + month.lengthOfMonth()
        val weeks = (cells + 6) / 7

        Row(Modifier.fillMaxWidth()) {
            for (name in listOf("월", "화", "수", "목", "금", "토", "일")) {
                Text(
                    text = name,
                    modifier = Modifier.weight(1f),
                    fontSize = 9.sp,
                    color = Slate,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        for (w in 0 until weeks) {
            Row(Modifier.fillMaxWidth()) {
                for (d in 0 until 7) {
                    val index = w * 7 + d - lead + 1
                    val day = if (index in 1..month.lengthOfMonth()) month.atDay(index) else null
                    val count = day?.let { byDay[it] } ?: 0
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .then(if (day != null) Modifier.quietClickable { onPick(day) } else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (day != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$index",
                                    fontSize = 12.sp,
                                    fontWeight = if (picked == day) FontWeight.Black else FontWeight.Normal,
                                    color = when {
                                        picked == day -> Volt
                                        count > 0 -> Snow
                                        else -> Slate
                                    },
                                )
                                if (count > 0) {
                                    Box(
                                        Modifier
                                            .padding(top = 2.dp)
                                            .width(14.dp)
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(Volt),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
