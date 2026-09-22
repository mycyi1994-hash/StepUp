package com.stepup.android.ui.screens.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.data.remote.EventRow
import com.stepup.android.data.remote.NewsRow
import com.stepup.android.data.remote.RunningFeedApi
import com.stepup.android.data.remote.SourceRow
import com.stepup.android.ui.components.PillChip
import com.stepup.android.ui.theme.Slate

/**
 * 뉴스 탭 안의 두 자리.
 *
 * 화면을 따로 만들지 않고 같은 목록에 얹는 것은, 탭을 오갈 때 머리글이 하나로
 * 묶여 있어야 한 화면 안의 전환으로 읽히기 때문이다.
 */

/** 러닝 이벤트 — 대회를 찾는 자리 */
fun LazyListScope.runningEventsSection(
    ui: EventsUi,
    onQuery: (String) -> Unit,
    onType: (String) -> Unit,
    onDistance: (String) -> Unit,
    onRegion: (String) -> Unit,
    onStatus: (String) -> Unit,
    onSort: (String) -> Unit,
    onTogglePast: () -> Unit,
    onToggleCalendar: () -> Unit,
    onShiftMonth: (Long) -> Unit,
    onPickDay: (java.time.LocalDate) -> Unit,
    onOpen: (EventRow) -> Unit,
    onToggleSave: (EventRow) -> Unit,
) {
    item {
        Text(
            text = stringResource(R.string.feed_events_hint),
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = Slate,
            lineHeight = 18.sp,
        )
    }

    item {
        FeedSearchField(
            value = ui.filter.query,
            hint = stringResource(R.string.feed_events_search_hint),
            onValueChange = onQuery,
        )
    }

    // 유형과 거리는 따로 고른다. 트레일러닝도 10km 가 있고 걷기도 5km 가 있다.
    item {
        FeedChipRow(
            values = RunningFeedViewModel.TYPES,
            selected = ui.filter.eventType,
            label = { stringResource(eventTypeRes(it)) },
            onSelect = onType,
        )
    }
    item {
        FeedChipRow(
            values = RunningFeedViewModel.DISTANCES,
            selected = ui.filter.distance,
            label = { stringResource(distanceRes(it)) },
            onSelect = onDistance,
        )
    }
    item {
        FeedChipRow(
            values = RunningFeedViewModel.REGIONS,
            selected = ui.filter.region,
            label = { if (it == RunningFeedApi.ALL) stringResource(R.string.feed_region_all) else it },
            onSelect = onRegion,
        )
    }
    item {
        FeedChipRow(
            values = RunningFeedViewModel.STATUSES,
            selected = ui.filter.status,
            label = { stringResource(statusRes(it)) },
            onSelect = onStatus,
        )
    }

    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            PillChip(
                text = stringResource(R.string.feed_sort_date),
                selected = ui.filter.sort == "DATE",
                onClick = { onSort("DATE") },
            )
            PillChip(
                text = stringResource(R.string.feed_sort_closing),
                selected = ui.filter.sort == "CLOSING",
                onClick = { onSort("CLOSING") },
            )
            PillChip(
                text = stringResource(R.string.feed_sort_newest),
                selected = ui.filter.sort == "NEWEST",
                onClick = { onSort("NEWEST") },
            )
        }
    }
    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            PillChip(
                text = stringResource(R.string.feed_view_calendar),
                selected = ui.calendar,
                onClick = { onToggleCalendar() },
            )
            PillChip(
                text = stringResource(R.string.feed_include_past),
                selected = ui.filter.includePast,
                onClick = { onTogglePast() },
            )
        }
    }

    if (ui.problem != null) {
        item { FeedProblemNote(ui.problem, ui.loadedAtMillis) }
    }

    if (ui.calendar) {
        item {
            EventCalendar(
                month = ui.month,
                rows = ui.rows,
                picked = ui.pickedDay,
                onPick = onPickDay,
                onShift = onShiftMonth,
            )
        }
    }

    // 달력에서 날짜를 골랐으면 그 날의 대회만. 목록과 달력은 같은 데이터다.
    val shown = ui.pickedDay?.let { day ->
        ui.rows.filter { parseDay(it.eventDate) == day }
    } ?: ui.rows

    if (shown.isEmpty()) {
        item {
            if (ui.loading) {
                FeedEmptyNote(R.string.feed_loading)
            } else {
                FeedEmptyNote(R.string.feed_events_empty, R.string.feed_events_empty_hint)
            }
        }
        return
    }

    items(shown.size) { index ->
        val row = shown[index]
        EventCard(
            row = row,
            onOpen = { onOpen(row) },
            onToggleSave = { onToggleSave(row) },
        )
    }

    item {
        Text(
            text = stringResource(R.string.feed_events_notice),
            modifier = Modifier.padding(horizontal = 4.dp),
            fontSize = 10.sp,
            color = Slate,
            lineHeight = 16.sp,
        )
    }
}

/** 러닝·건강 뉴스 — 읽을 것을 찾는 자리 */
fun LazyListScope.runningNewsSection(
    ui: NewsUi,
    sources: List<SourceRow>,
    onQuery: (String) -> Unit,
    onCategory: (String) -> Unit,
    onPublisher: (String) -> Unit,
    onSort: (String) -> Unit,
    onOpen: (NewsRow) -> Unit,
    onToggleSave: (NewsRow) -> Unit,
) {
    item {
        Text(
            text = stringResource(R.string.feed_news_hint),
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = Slate,
            lineHeight = 18.sp,
        )
    }

    item {
        FeedSearchField(
            value = ui.filter.query,
            hint = stringResource(R.string.feed_news_search_hint),
            onValueChange = onQuery,
        )
    }

    item {
        FeedChipRow(
            values = RunningFeedViewModel.CATEGORIES,
            selected = ui.filter.category,
            label = { stringResource(categoryRes(it)) },
            onSelect = onCategory,
        )
    }

    if (ui.publishers.isNotEmpty()) {
        item {
            FeedChipRow(
                values = listOf(RunningFeedApi.ALL to "") + ui.publishers,
                selected = ui.publishers.firstOrNull { it.first == ui.filter.publisher }
                    ?: (RunningFeedApi.ALL to ""),
                label = {
                    if (it.first == RunningFeedApi.ALL) stringResource(R.string.feed_publisher_all)
                    else it.second.ifBlank { it.first }
                },
                onSelect = { onPublisher(it.first) },
            )
        }
    }

    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            PillChip(
                text = stringResource(R.string.feed_sort_recent),
                selected = ui.filter.sort == "RECENT",
                onClick = { onSort("RECENT") },
            )
            PillChip(
                text = stringResource(R.string.feed_sort_relevance),
                selected = ui.filter.sort == "RELEVANCE",
                onClick = { onSort("RELEVANCE") },
            )
        }
    }

    if (ui.problem != null) {
        item { FeedProblemNote(ui.problem, ui.loadedAtMillis) }
    }

    if (ui.rows.isEmpty()) {
        item {
            if (ui.loading) {
                FeedEmptyNote(R.string.feed_loading)
            } else {
                FeedEmptyNote(R.string.feed_news_empty, R.string.feed_news_empty_hint)
            }
        }
    } else {
        items(ui.rows.size) { index ->
            val row = ui.rows[index]
            NewsCard(
                row = row,
                onOpen = { onOpen(row) },
                onToggleSave = { onToggleSave(row) },
            )
        }
    }

    // 어디서 온 정보인지 밝힌다. 켜진 출처가 하나도 없으면 그 사실도 적는다.
    item {
        val connected = sources.filter { it.enabled && it.canDiscover }
        Text(
            text = if (connected.isEmpty()) {
                stringResource(R.string.feed_sources_none)
            } else {
                stringResource(
                    R.string.feed_sources_note,
                    connected.joinToString(" · ") { it.name },
                )
            },
            modifier = Modifier.padding(horizontal = 4.dp),
            fontSize = 10.sp,
            color = Slate,
            lineHeight = 16.sp,
        )
    }
}
