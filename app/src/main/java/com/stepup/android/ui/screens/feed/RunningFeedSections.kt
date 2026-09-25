package com.stepup.android.ui.screens.feed

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
import com.stepup.android.data.remote.SourceRow
import com.stepup.android.ui.theme.Silver

/**
 * 뉴스 탭 안의 두 자리.
 *
 * 화면을 따로 만들지 않고 같은 목록에 얹는 것은, 탭을 오갈 때 머리글이 하나로
 * 묶여 있어야 한 화면 안의 전환으로 읽히기 때문이다.
 *
 * ── 거르기 ──
 *
 * 기본 화면에는 검색칸, 버튼 둘, 요약 한 줄만 둔다. 고를 것은 [EventFilterSheet]
 * 와 [NewsFilterSheet] 안에 전부 있고, 패널은 화면(NewsScreen)이 띄운다 —
 * LazyColumn 의 item 안에서 창을 띄우면 그 줄이 화면 밖으로 밀릴 때 창까지
 * 같이 사라진다.
 */

/** 러닝 이벤트 — 대회를 찾는 자리 */
fun LazyListScope.runningEventsSection(
    ui: EventsUi,
    onQuery: (String) -> Unit,
    onOpenFilters: () -> Unit,
    onOpenSort: () -> Unit,
    onResetFilters: () -> Unit,
    onTogglePast: () -> Unit,
    onToggleCalendar: () -> Unit,
    onShiftMonth: (Long) -> Unit,
    onPickDay: (java.time.LocalDate) -> Unit,
    onRetry: () -> Unit,
    onOpen: (EventRow) -> Unit,
    onToggleSave: (EventRow) -> Unit,
) {
    // 시안대로 검색칸 하나. 거르기는 칸 끝의 버튼이 연다.
    item {
        FeedSearchField(
            value = ui.filter.query,
            hint = stringResource(R.string.feed_events_search_hint),
            onValueChange = onQuery,
            onFilter = onOpenFilters,
            filterCount = eventFilterCount(ui.filter),
        )
    }

    if (ui.problem != null) {
        item { FeedProblemNote(ui.problem, ui.loadedAtMillis, onRetry = onRetry) }
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
        // 비어 있는 까닭을 가른다. 조건을 좁혀서 빈 것과 아직 등록된 대회가
        // 없는 것은 다음에 할 일이 다르다.
        val narrowed = ui.filter.query.isNotBlank() ||
            eventFilterCount(ui.filter) > 0 ||
            ui.pickedDay != null
        item {
            when {
                ui.loading -> FeedEmptyNote(R.string.feed_loading)
                narrowed -> FeedEmptyNote(
                    text = R.string.feed_events_no_match,
                    hint = R.string.feed_events_no_match_hint,
                    action = R.string.filter_reset to onResetFilters,
                )
                else -> FeedEmptyNote(R.string.feed_events_empty, R.string.feed_events_empty_hint)
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
            fontSize = 14.sp,
            color = Silver,
            lineHeight = 21.sp,
        )
    }
}

/** 러닝·건강 뉴스 — 읽을 것을 찾는 자리 */
fun LazyListScope.runningNewsSection(
    ui: NewsUi,
    sources: List<SourceRow>,
    onQuery: (String) -> Unit,
    onOpenFilters: () -> Unit,
    onOpenSort: () -> Unit,
    onResetFilters: () -> Unit,
    onRetry: () -> Unit,
    onOpen: (NewsRow) -> Unit,
    onToggleSave: (NewsRow) -> Unit,
) {
    item {
        FeedSearchField(
            value = ui.filter.query,
            hint = stringResource(R.string.feed_news_search_hint),
            onValueChange = onQuery,
            onFilter = onOpenFilters,
            filterCount = newsFilterCount(ui.filter),
        )
    }

    if (ui.problem != null) {
        item { FeedProblemNote(ui.problem, ui.loadedAtMillis, onRetry = onRetry) }
    }

    if (ui.rows.isEmpty()) {
        // 세 가지를 가른다.
        //
        //   * 아직 연결된 출처가 없다 — 우리가 준비 중이다.
        //   * 조건에 맞는 것이 없다 — 조건을 줄이면 된다.
        //   * 서버에 못 닿았다 — 위의 안내가 이미 다시 시도를 내놓았다.
        val narrowed = ui.filter.query.isNotBlank() || newsFilterCount(ui.filter) > 0
        val connected = sources.any { it.enabled && it.canDiscover }
        item {
            when {
                ui.loading -> FeedEmptyNote(R.string.feed_loading)
                ui.problem != null -> Unit
                narrowed -> FeedEmptyNote(
                    text = R.string.feed_news_no_match,
                    hint = R.string.feed_news_no_match_hint,
                    action = R.string.filter_reset to onResetFilters,
                )
                !connected -> FeedEmptyNote(
                    R.string.feed_news_empty,
                    R.string.feed_news_empty_hint,
                )
                else -> FeedEmptyNote(R.string.feed_news_empty)
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
            fontSize = 14.sp,
            color = Silver,
            lineHeight = 21.sp,
        )
    }
}
