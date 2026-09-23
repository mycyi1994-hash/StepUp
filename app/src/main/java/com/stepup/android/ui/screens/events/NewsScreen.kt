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
import androidx.compose.ui.unit.dp
import com.stepup.android.domain.AvatarArtCatalog
import com.stepup.android.domain.AvatarPose
import com.stepup.android.ui.components.AvatarImage
import com.stepup.android.ui.components.PageHero
import com.stepup.android.ui.components.SecondaryHeader
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.ui.theme.VoltText
import com.stepup.android.ui.screens.feed.isDemo
import com.stepup.android.ui.screens.feed.demoNews
import com.stepup.android.ui.screens.feed.demoEvents
import com.stepup.android.ui.components.TwoWaySwitch
import com.stepup.android.ui.components.SubHeader
import com.stepup.android.ui.components.SmallBadge
import com.stepup.android.ui.components.BadgeTone
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import com.stepup.android.core.ExternalIntents
import com.stepup.android.data.repo.EventFilter
import com.stepup.android.data.repo.NewsFilter
import com.stepup.android.ui.components.DarkIconButton
import com.stepup.android.ui.components.quietClickable
import com.stepup.android.ui.screens.feed.EventFilterSheet
import com.stepup.android.ui.screens.feed.EventSortSheet
import com.stepup.android.ui.screens.feed.NewsFilterSheet
import com.stepup.android.ui.screens.feed.NewsSortSheet
import com.stepup.android.ui.screens.feed.RunningFeedViewModel
import com.stepup.android.ui.screens.feed.runningEventsSection
import com.stepup.android.ui.screens.feed.runningNewsSection
import com.stepup.android.ui.theme.Silver

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
    onBack: () -> Unit = {},
    onOpenWallet: () -> Unit = {},
    viewModel: NewsViewModel = viewModel(factory = NewsViewModel.Factory),
    feedViewModel: RunningFeedViewModel = viewModel(factory = RunningFeedViewModel.Factory),
) {
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val serverEvents by feedViewModel.events.collectAsStateWithLifecycle()
    val serverNews by feedViewModel.news.collectAsStateWithLifecycle()
    // 데모 모드 — 서버 대신 "예시"라고 적힌 목록을 보여 준다. 운영 목록과 섞지 않는다.
    val demo by com.stepup.android.core.ServiceLocator.userPrefs.demoMode
        .collectAsStateWithLifecycle(initialValue = false)
    val events = if (demo) {
        serverEvents.copy(rows = demoEvents(serverEvents.filter), loading = false, problem = null, pickedDay = serverEvents.pickedDay)
    } else {
        serverEvents
    }
    val news = if (demo) {
        serverNews.copy(rows = demoNews(serverNews.filter), loading = false, problem = null)
    } else {
        serverNews
    }
    val sources by feedViewModel.sources.collectAsStateWithLifecycle()
    // 제목 옆 캐릭터 — 내 성별의 그림
    val look by com.stepup.android.core.ServiceLocator.avatarRepository.look
        .collectAsStateWithLifecycle(com.stepup.android.domain.AvatarLook())
    val message by feedViewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 마지막으로 본 탭을 기억한다. 화면을 껐다 켜도 돌아온다.
    // 대회 / 러닝·건강 둘이다. 예전의 특가 공지는 챌린지 아래로 옮겼다 —
    // 예전 판에서 세 번째 칸을 기억하고 있으면 첫 칸으로 돌린다.
    var section by rememberSaveable { mutableIntStateOf(0) }
    if (section > 1) section = 0

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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = if (section == 0) eventsScroll else newsScroll,
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        item {
            SecondaryHeader(onBack = onBack, balance = balance, onOpenWallet = onOpenWallet)
        }

        item {
            PageHero(
                title = stringResource(R.string.news_title),
                subtitle = stringResource(R.string.news_hero_sub),
            ) {
                AvatarImage(
                    art = AvatarArtCatalog.resolve(look, AvatarPose.RUN).art,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxSize(),
                )
            }
        }

        item {
            TwoWaySwitch(
                labels = listOf(
                    stringResource(R.string.news_tab_races),
                    stringResource(R.string.news_tab_health),
                ),
                selected = section,
                onSelect = { section = it },
            )
        }

        if (demo) {
            item { FeedDemoNote() }
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
                    if (isDemo(row.id)) {
                        feedViewModel.message.value = R.string.demo_no_link
                    } else {
                        row.targetUrl?.let { ExternalIntents.openUrl(context, it) }
                    }
                },
                onToggleSave = { row ->
                    if (isDemo(row.id)) {
                        feedViewModel.message.value = R.string.demo_no_save
                    } else {
                        feedViewModel.toggleSaveEvent(row)
                    }
                },
            )

            else -> runningNewsSection(
                ui = news,
                sources = sources,
                onQuery = { q -> feedViewModel.editNews { it.copy(query = q) } },
                onOpenFilters = { newsFilters = true },
                onOpenSort = { newsSort = true },
                onResetFilters = {
                    feedViewModel.editNews { NewsFilter(query = it.query) }
                },
                onRetry = { feedViewModel.loadNews(force = true) },
                onOpen = { row ->
                    if (isDemo(row.id) || row.originalUrl.isBlank()) {
                        feedViewModel.message.value = R.string.demo_no_link
                    } else {
                        ExternalIntents.openUrl(context, row.originalUrl)
                    }
                },
                onToggleSave = { row ->
                    if (isDemo(row.id)) {
                        feedViewModel.message.value = R.string.demo_no_save
                    } else {
                        feedViewModel.toggleSaveNews(row)
                    }
                },
            )
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

/** 데모 모드일 때 목록 위에 붙는 한 줄 — 아래가 예시라는 것을 먼저 말한다 */
@Composable
private fun FeedDemoNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(VoltText.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SmallBadge(stringResource(R.string.demo_badge), tone = BadgeTone.Glow)
        Text(
            text = stringResource(R.string.feed_demo_note),
            fontSize = 13.sp,
            color = Silver,
            lineHeight = 18.sp,
        )
    }
}
