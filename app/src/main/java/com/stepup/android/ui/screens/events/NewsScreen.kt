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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.ui.components.DarkIconButton
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
    viewModel: EventsViewModel = viewModel(factory = EventsViewModel.Factory),
) {
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableIntStateOf(0) }

    val feed = when (section) {
        0 -> RUN_EVENT_FEED
        1 -> DEAL_FEED
        else -> HEALTH_FEED
    }
    val footnote = when (section) {
        0 -> R.string.feed_note_runs
        1 -> R.string.feed_note_deals
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

        items(feed.size) { index -> FeedCard(feed[index]) }
        item { FeedFootnote(footnote) }
    }
}
