package com.stepup.android.ui.screens.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.stepup.android.ui.components.DarkIconButton
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.Wordmark
import com.stepup.android.ui.screens.community.SegmentedTabs
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Snow

/**
 * 뉴스 — 읽는 자리.
 *
 * 이벤트 탭과 나눈 이유는 하는 일이 다르기 때문이다. 이벤트 탭은 진행률을
 * 보고 보상을 **받는** 자리라 누를 것이 있고, 여기는 무슨 일이 열리는지
 * **읽는** 자리라 누를 것이 없다. 한 탭에 섞어 두면 "받기" 버튼을 찾으러
 * 읽을거리를 헤치고 지나가야 한다.
 *
 * 세 갈래 — 러닝 이벤트 소식 · 특가 공지 · 건강 뉴스.
 */
@Composable
fun NewsScreen(
    onOpenNotifications: () -> Unit = {},
    viewModel: NewsViewModel = viewModel(factory = NewsViewModel.Factory),
) {
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val runEvents by viewModel.runEvents.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var section by rememberSaveable { mutableIntStateOf(0) }

    // 러닝 이벤트만 서버에서 받아 온다. 특가 공지와 건강 뉴스는 StepUp 이
    // 쓴 글이라 앱 안에 있고, 남의 기사를 옮겨 오지 않는다.
    val feed = if (section == 1) DEAL_FEED else HEALTH_FEED
    val footnote = when {
        // 아직 못 받았을 때는 StepUp 이 쓴 안내가 대신 서 있다.
        // 그 글을 남의 기사인 것처럼 설명하면 안 된다.
        section == 0 && runEvents.isEmpty() -> R.string.feed_note_runs_pending
        section == 0 -> R.string.feed_note_runs
        section == 1 -> R.string.feed_note_deals
        else -> R.string.feed_note_health
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
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
                if (section == 0) {
                    DarkIconButton(
                        icon = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.news_refresh),
                        onClick = { viewModel.refresh() },
                    )
                }
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
                    text = stringResource(R.string.tab_news),
                    fontSize = 32.sp,
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
                    stringResource(R.string.news_section_deals),
                    stringResource(R.string.news_section_health),
                ),
                selected = section,
                onSelect = { section = it },
            )
        }

        if (section == 0) {
            // 서버에서 받아 온 소식. 하루 한 번 새 글이 들어온다.
            if (runEvents.isEmpty()) {
                item {
                    GlowCard(contentPadding = PaddingValues(16.dp), spacing = 8.dp) {
                        Text(
                            text = stringResource(
                                if (refreshing) R.string.news_loading else R.string.news_empty
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = Silver,
                            lineHeight = 19.sp,
                        )
                    }
                }
                // 아직 못 받았을 때는 StepUp 이 쓴 안내라도 보여 준다.
                // 빈 화면은 앱이 고장 난 것처럼 보인다.
                items(RUN_EVENT_FEED.size) { index -> FeedCard(RUN_EVENT_FEED[index]) }
            } else {
                items(runEvents.size) { index ->
                    val item = runEvents[index]
                    NewsCard(item) { ExternalIntents.openUrl(context, item.url) }
                }
            }
        } else {
            items(feed.size) { index -> FeedCard(feed[index]) }
        }
        item { FeedFootnote(footnote) }
    }
}
