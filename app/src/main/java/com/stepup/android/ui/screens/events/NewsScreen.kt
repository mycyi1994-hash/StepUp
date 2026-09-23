package com.stepup.android.ui.screens.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.core.ExternalIntents
import com.stepup.android.data.repo.EventFilter
import com.stepup.android.data.repo.NewsFilter
import com.stepup.android.ui.components.DarkIconButton
import com.stepup.android.ui.components.Wordmark
import com.stepup.android.ui.components.quietClickable
import com.stepup.android.ui.screens.community.SegmentedTabs
import com.stepup.android.ui.screens.feed.EventFilterSheet
import com.stepup.android.ui.screens.feed.EventSortSheet
import com.stepup.android.ui.screens.feed.NewsFilterSheet
import com.stepup.android.ui.screens.feed.NewsSortSheet
import com.stepup.android.ui.screens.feed.RunningFeedViewModel
import com.stepup.android.ui.screens.feed.runningEventsSection
import com.stepup.android.ui.screens.feed.runningNewsSection
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Snow

/**
 * 뉴스 — 읽고 찾는 자리.
 *
 * 세 갈래다.
 *
 *   * **러닝 이벤트** — 바깥에서 열리는 대회. 신청은 주최 측 사이트에서 한다.
 *   * **러닝·건강 뉴스** — 언론사 기사. 읽는 것은 원문에서 한다.
 *   * **특가 공지** — StepUp 이 직접 쓰는 공지.
 *
 * ── 이벤트 탭과 무엇이 다른가 ──
 *
 * 하단의 "이벤트" 탭은 챌린지·미션처럼 **보상을 받는** 자리다. 여기 러닝
 * 이벤트는 **바깥 대회를 찾는** 자리이고, 눌러서 나가는 것으로 SUP 가 생기지
 * 않는다. 표도 화면도 갈라 두었다 — 섞이면 언젠가 바깥 대회를 눌렀다고
 * 보상을 주는 실수가 난다.
 *
 * ── 돌아왔을 때 ──
 *
 * 탭마다 스크롤 자리를 따로 기억한다. 바깥 사이트를 커스텀 탭으로 열기
 * 때문에 닫으면 이 화면이 그대로 남고, 거르기와 스크롤을 다시 맞출 필요가 없다.
 */
@Composable
fun NewsScreen(
    onOpenNotifications: () -> Unit = {},
    viewModel: NewsViewModel = viewModel(factory = NewsViewModel.Factory),
    feedViewModel: RunningFeedViewModel = viewModel(factory = RunningFeedViewModel.Factory),
) {
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val events by feedViewModel.events.collectAsStateWithLifecycle()
    val news by feedViewModel.news.collectAsStateWithLifecycle()
    val sources by feedViewModel.sources.collectAsStateWithLifecycle()
    val message by feedViewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 마지막으로 본 탭을 기억한다. 화면을 껐다 켜도 돌아온다.
    var section by rememberSaveable { mutableIntStateOf(0) }

    // 거르기 패널은 목록 **밖**에서 띄운다. LazyColumn 의 item 안에서
    // 띄우면 그 줄이 화면 밖으로 밀릴 때 창까지 같이 사라진다.
    var eventFilters by rememberSaveable { mutableStateOf(false) }
    var eventSort by rememberSaveable { mutableStateOf(false) }
    var newsFilters by rememberSaveable { mutableStateOf(false) }
    var newsSort by rememberSaveable { mutableStateOf(false) }

    // 탭마다 제 스크롤 자리를 갖는다. 하나로 쓰면 대회를 한참 내려보다
    // 뉴스로 옮겼을 때 엉뚱한 곳에서 시작한다.
    val eventsScroll = rememberLazyListState()
    val newsScroll = rememberLazyListState()
    val dealsScroll = rememberLazyListState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = when (section) {
            0 -> eventsScroll
            1 -> newsScroll
            else -> dealsScroll
        },
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Wordmark(fontSize = 22.sp, modifier = Modifier.weight(1f))
                TotalRewardsCard(balance)
                DarkIconButton(
                    icon = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.news_refresh),
                    onClick = {
                        when (section) {
                            0 -> feedViewModel.loadEvents(force = true)
                            1 -> feedViewModel.loadNews(force = true)
                            else -> Unit
                        }
                    },
                )
                DarkIconButton(
                    icon = Icons.Filled.Notifications,
                    contentDescription = stringResource(R.string.cd_notifications),
                    onClick = onOpenNotifications,
                    badge = true,
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(
                        when (section) {
                            0 -> R.string.feed_events_title
                            1 -> R.string.feed_news_title
                            else -> R.string.news_section_deals
                        }
                    ),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    color = Snow,
                )
                Text(
                    text = stringResource(R.string.news_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = Silver,
                )
            }
        }

        item {
            SegmentedTabs(
                labels = listOf(
                    stringResource(R.string.news_section_runs),
                    stringResource(R.string.news_section_health),
                    stringResource(R.string.news_section_deals),
                ),
                selected = section,
                onSelect = { section = it },
            )
        }

        message?.let { note ->
            item {
                Box(Modifier.quietClickable { feedViewModel.consumeMessage() }) {
                    Text(
                        text = stringResource(note),
                        style = MaterialTheme.typography.bodySmall,
                        color = Silver,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }

        when (section) {
            0 -> runningEventsSection(
                ui = events,
                onQuery = { q -> feedViewModel.editEvents { it.copy(query = q) } },
                onOpenFilters = { eventFilters = true },
                onOpenSort = { eventSort = true },
                // 기본 화면의 초기화 — 누르는 즉시 기본 조건으로 돌아간다.
                // 검색어와 지난 대회 포함은 그대로 둔다. 그 둘은 패널이
                // 아니라 화면에서 고른 것이라, 함께 지우면 방금 친 검색어가
                // 말없이 사라진다.
                onResetFilters = {
                    feedViewModel.editEvents {
                        EventFilter(query = it.query, includePast = it.includePast)
                    }
                    feedViewModel.pickDay(null)
                },
                onTogglePast = {
                    feedViewModel.editEvents { it.copy(includePast = !it.includePast) }
                },
                onToggleCalendar = feedViewModel::toggleCalendar,
                onShiftMonth = feedViewModel::shiftMonth,
                onPickDay = feedViewModel::pickDay,
                onRetry = { feedViewModel.loadEvents(force = true) },
                onOpen = { row ->
                    // 카드를 누르면 그 대회의 바깥 페이지로 바로 간다.
                    // 주소를 모르면 아무 데도 보내지 않는다 — 홈페이지로
                    // 대충 보내면 사용자가 자기 대회를 다시 찾아야 한다.
                    row.targetUrl?.let { ExternalIntents.openUrl(context, it) }
                },
                onToggleSave = feedViewModel::toggleSaveEvent,
            )

            1 -> runningNewsSection(
                ui = news,
                sources = sources,
                onQuery = { q -> feedViewModel.editNews { it.copy(query = q) } },
                onOpenFilters = { newsFilters = true },
                onOpenSort = { newsSort = true },
                onResetFilters = {
                    feedViewModel.editNews { NewsFilter(query = it.query) }
                },
                onRetry = { feedViewModel.loadNews(force = true) },
                onOpen = { row -> ExternalIntents.openUrl(context, row.originalUrl) },
                onToggleSave = feedViewModel::toggleSaveNews,
            )

            else -> {
                items(DEAL_FEED.size) { index -> FeedCard(DEAL_FEED[index]) }
                item { FeedFootnote(R.string.feed_note_deals) }
            }
        }
    }

    // ── 거르기 패널 ────────────────────────────────────────────
    //
    // 패널이 열린 동안에는 고른 것이 패널 안에만 있다. 닫기(X·뒤로가기·
    // 바깥)는 그 임시 선택을 버리고, "결과 보기"만 밖으로 넘긴다.
    if (eventFilters) {
        EventFilterSheet(
            initial = events.filter,
            onDismiss = { eventFilters = false },
            onApply = { applied ->
                eventFilters = false
                feedViewModel.editEvents { applied }
            },
        )
    }
    if (eventSort) {
        EventSortSheet(
            selected = events.filter.sort,
            onPick = { v -> feedViewModel.editEvents { it.copy(sort = v) } },
            onDismiss = { eventSort = false },
        )
    }
    if (newsFilters) {
        NewsFilterSheet(
            initial = news.filter,
            publishers = news.publishers,
            onDismiss = { newsFilters = false },
            onApply = { applied ->
                newsFilters = false
                feedViewModel.editNews { applied }
            },
        )
    }
    if (newsSort) {
        NewsSortSheet(
            selected = news.filter.sort,
            onPick = { v -> feedViewModel.editNews { it.copy(sort = v) } },
            onDismiss = { newsSort = false },
        )
    }
}
