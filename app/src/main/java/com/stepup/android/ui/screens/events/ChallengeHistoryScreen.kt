package com.stepup.android.ui.screens.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.local.RewardEntity
import com.stepup.android.data.local.RewardType
import com.stepup.android.ui.components.DetailPage
import com.stepup.android.ui.components.HairlineDivider
import com.stepup.android.ui.components.S2Stats
import com.stepup.android.ui.components.StatePanel
import com.stepup.android.ui.components.formatSupDown
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.VoltText
import java.time.Instant
import java.time.ZoneId

/**
 * 내 정보 › 챌린지 기록(사용 피드백 7) — 지금 하는 챌린지는 러닝 쪽에 있고, 여기는 끝낸 이력이다.
 * 원장에 적힌 적립(일일 목표 보너스 · 도전 보상)만 보인다. 받기 전 · 확인 전 진행은 여기 없다.
 */
@Composable
fun ChallengeHistoryScreen(onBack: () -> Unit, onOpenChallenges: () -> Unit) {
    val flow = androidx.compose.runtime.remember { ServiceLocator.rewardRepository.challengeRewards() }
    val rows by flow.collectAsStateWithLifecycle(initialValue = null)
    DetailPage(
        title = stringResource(R.string.challenge_history_title),
        onBack = onBack,
        primaryActionLabel = stringResource(R.string.challenge_history_current),
        onPrimaryAction = onOpenChallenges,
        primaryActionIcon = Icons.Filled.EmojiEvents,
    ) {
        val list = rows
        item {
            S2Stats(
                listOf(
                    stringResource(R.string.challenge_history_done) to (list?.size?.toString() ?: "—"),
                    stringResource(R.string.challenge_history_goal_days) to
                        (list?.count { it.type == RewardType.BONUS_GOAL }?.toString() ?: "—"),
                    stringResource(R.string.challenge_history_total) to
                        (list?.let { "+" + formatSupDown(it.sumOf { r -> r.amount }, 2) } ?: "—"),
                ),
                valueSize = 22.sp,
                modifier = Modifier.padding(vertical = 12.dp).testTag("challenge-history-summary"),
            )
        }
        when {
            list == null -> item { StatePanel(stringResource(R.string.feed_loading), Icons.Filled.EmojiEvents, loading = true) }
            list.isEmpty() -> item {
                StatePanel(stringResource(R.string.challenge_history_empty), Icons.Filled.EmojiEvents,
                    modifier = Modifier.testTag("challenge-history-empty"))
            }
            else -> items(list, key = { it.id }) { HistoryRow(it) }
        }
    }
}

@Composable
private fun HistoryRow(row: RewardEntity) {
    val date = Instant.ofEpochMilli(row.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("challenge-history-row"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (row.type == RewardType.BONUS_GOAL) R.string.challenge_daily_title else R.string.challenge_history_event),
                    color = Snow, style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "${date.year}.${date.monthValue}.${date.dayOfMonth}" + if (row.description.isNotBlank()) " · ${row.description}" else "",
                    color = Slate, style = MaterialTheme.typography.bodySmall, maxLines = 1,
                )
            }
            Text("+" + formatSupDown(row.amount, 2) + " SUP", color = VoltText, style = MaterialTheme.typography.titleSmall)
        }
        HairlineDivider()
    }
}
