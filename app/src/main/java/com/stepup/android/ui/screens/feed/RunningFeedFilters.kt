package com.stepup.android.ui.screens.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.stepup.android.R
import com.stepup.android.data.remote.RunningFeedApi
import com.stepup.android.data.repo.EventFilter
import com.stepup.android.data.repo.NewsFilter
import com.stepup.android.ui.components.ChoiceGrid
import com.stepup.android.ui.components.FilterBottomSheet
import com.stepup.android.ui.components.FilterSection
import com.stepup.android.ui.components.SortBottomSheet

/**
 * 러닝 이벤트와 뉴스의 거르기 패널.
 *
 * 화면 쪽에는 버튼 둘과 요약 한 줄만 남기고, 고를 것은 전부 이 패널 안에
 * 있다. 패널은 고르는 동안 제 임시 상태를 들고 있다가 "결과 보기"를 누를 때
 * 한 번만 밖으로 넘긴다 — 칩을 누를 때마다 서버를 부르지 않기 위해서다.
 *
 * 선택지는 [RunningFeedViewModel] 의 목록을 그대로 쓴다. 여기서 따로
 * 추리면 서버가 아는 조건과 화면이 보여 주는 조건이 갈라진다. 전국 시·도도
 * 하나도 빼지 않고 격자에 담는다.
 */

/** 기본값을 벗어난 조건 묶음의 수. 정렬은 제 버튼이 따로 있어 세지 않는다. */
fun eventFilterCount(f: EventFilter): Int = listOf(
    f.eventType, f.distance, f.region, f.status,
).count { it != RunningFeedApi.ALL }

fun newsFilterCount(f: NewsFilter): Int = listOf(
    f.category, f.publisher,
).count { it != RunningFeedApi.ALL }

/** 고른 조건을 짧은 말로. 기본값은 적지 않는다 — 요약은 다른 점만 적는 것이다. */
@Composable
fun eventFilterParts(f: EventFilter): List<String> = buildList {
    if (f.eventType != RunningFeedApi.ALL) add(stringResource(eventTypeRes(f.eventType)))
    if (f.distance != RunningFeedApi.ALL) add(stringResource(distanceRes(f.distance)))
    if (f.region != RunningFeedApi.ALL) add(f.region)
    if (f.status != RunningFeedApi.ALL) add(stringResource(statusRes(f.status)))
}

@Composable
fun newsFilterParts(f: NewsFilter, publishers: List<Pair<String, String>>): List<String> =
    buildList {
        if (f.category != RunningFeedApi.ALL) add(stringResource(categoryRes(f.category)))
        if (f.publisher != RunningFeedApi.ALL) {
            // 언론사는 도메인이 아니라 이름으로 적는다. 목록에 없으면
            // 도메인이라도 적는다 — 무엇으로 걸렀는지는 보여야 한다.
            add(publishers.firstOrNull { it.first == f.publisher }?.second ?: f.publisher)
        }
    }

@Composable
fun eventSortLabel(sort: String): String = stringResource(
    when (sort) {
        "CLOSING" -> R.string.feed_sort_closing
        "NEWEST" -> R.string.feed_sort_newest
        else -> R.string.feed_sort_date
    },
)

@Composable
fun newsSortLabel(sort: String): String = stringResource(
    when (sort) {
        "RELEVANCE" -> R.string.feed_sort_relevance
        else -> R.string.feed_sort_recent
    },
)

private val EVENT_SORTS = listOf("DATE", "CLOSING", "NEWEST")
private val NEWS_SORTS = listOf("RECENT", "RELEVANCE")

/** 대회 거르기 패널 */
@Composable
fun EventFilterSheet(
    initial: EventFilter,
    onDismiss: () -> Unit,
    onApply: (EventFilter) -> Unit,
) {
    // 패널 안에서만 사는 임시 선택. 닫으면 그대로 사라진다.
    var draft by remember(initial) { mutableStateOf(initial) }

    FilterBottomSheet(
        title = stringResource(R.string.filter_events_title),
        onDismiss = onDismiss,
        // 패널 안 초기화는 임시 상태만 되돌린다. 검색어와 지난 대회
        // 포함처럼 패널 밖에서 고른 것은 건드리지 않는다.
        onReset = {
            draft = draft.copy(
                eventType = RunningFeedApi.ALL,
                distance = RunningFeedApi.ALL,
                region = RunningFeedApi.ALL,
                status = RunningFeedApi.ALL,
                sort = EventFilter().sort,
            )
        },
        onApply = { onApply(draft) },
    ) {
        FilterSection(stringResource(R.string.filter_group_type)) {
            ChoiceGrid(
                values = RunningFeedViewModel.TYPES,
                isSelected = { it == draft.eventType },
                label = { stringResource(eventTypeRes(it)) },
                onSelect = { draft = draft.copy(eventType = it) },
            )
        }
        FilterSection(stringResource(R.string.filter_group_distance)) {
            ChoiceGrid(
                values = RunningFeedViewModel.DISTANCES,
                isSelected = { it == draft.distance },
                label = { stringResource(distanceRes(it)) },
                onSelect = { draft = draft.copy(distance = it) },
            )
        }
        FilterSection(stringResource(R.string.filter_group_region)) {
            ChoiceGrid(
                values = RunningFeedViewModel.REGIONS,
                isSelected = { it == draft.region },
                label = {
                    if (it == RunningFeedApi.ALL) stringResource(R.string.feed_region_all) else it
                },
                onSelect = { draft = draft.copy(region = it) },
            )
        }
        FilterSection(stringResource(R.string.filter_group_status)) {
            ChoiceGrid(
                values = RunningFeedViewModel.STATUSES,
                isSelected = { it == draft.status },
                label = { stringResource(statusRes(it)) },
                onSelect = { draft = draft.copy(status = it) },
            )
        }
        FilterSection(stringResource(R.string.filter_group_sort)) {
            ChoiceGrid(
                values = EVENT_SORTS,
                isSelected = { it == draft.sort },
                label = { eventSortLabel(it) },
                onSelect = { draft = draft.copy(sort = it) },
                columns = 2,
            )
        }
    }
}

/** 뉴스 거르기 패널 — 대회와 같은 조각, 같은 규칙 */
@Composable
fun NewsFilterSheet(
    initial: NewsFilter,
    publishers: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onApply: (NewsFilter) -> Unit,
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    val sources = remember(publishers) {
        listOf(RunningFeedApi.ALL to "") + publishers
    }

    FilterBottomSheet(
        title = stringResource(R.string.filter_news_title),
        onDismiss = onDismiss,
        onReset = {
            draft = draft.copy(
                category = RunningFeedApi.ALL,
                publisher = RunningFeedApi.ALL,
                sort = NewsFilter().sort,
            )
        },
        onApply = { onApply(draft) },
    ) {
        FilterSection(stringResource(R.string.filter_group_topic)) {
            ChoiceGrid(
                values = RunningFeedViewModel.CATEGORIES,
                isSelected = { it == draft.category },
                label = { stringResource(categoryRes(it)) },
                onSelect = { draft = draft.copy(category = it) },
            )
        }
        // 실린 기사에서 뽑은 언론사만 내놓는다. 기사가 하나도 없는 곳을
        // 골라 두면 빈 목록이 나오고, 그것이 거르기 탓인지 수집 탓인지
        // 알 수 없게 된다.
        if (publishers.isNotEmpty()) {
            FilterSection(stringResource(R.string.filter_group_publisher)) {
                ChoiceGrid(
                    values = sources,
                    isSelected = { it.first == draft.publisher },
                    label = {
                        if (it.first == RunningFeedApi.ALL) {
                            stringResource(R.string.feed_publisher_all)
                        } else {
                            it.second.ifBlank { it.first }
                        }
                    },
                    onSelect = { draft = draft.copy(publisher = it.first) },
                )
            }
        }
        FilterSection(stringResource(R.string.filter_group_sort)) {
            ChoiceGrid(
                values = NEWS_SORTS,
                isSelected = { it == draft.sort },
                label = { newsSortLabel(it) },
                onSelect = { draft = draft.copy(sort = it) },
                columns = 2,
            )
        }
    }
}

/** 정렬만 고르는 작은 패널 — 누르면 바로 적용된다 */
@Composable
fun EventSortSheet(selected: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    SortBottomSheet(
        title = stringResource(R.string.filter_sort_title),
        options = EVENT_SORTS,
        selected = selected,
        label = { eventSortLabel(it) },
        onPick = onPick,
        onDismiss = onDismiss,
    )
}

@Composable
fun NewsSortSheet(selected: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    SortBottomSheet(
        title = stringResource(R.string.filter_sort_title),
        options = NEWS_SORTS,
        selected = selected,
        label = { newsSortLabel(it) },
        onPick = onPick,
        onDismiss = onDismiss,
    )
}
