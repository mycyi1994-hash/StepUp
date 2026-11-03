package com.stepup.android.ui.screens.events

import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
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
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.ui.res.*
import com.stepup.android.core.AppServices
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
import kotlin.time.Instant

/**
 * 내 정보 › 챌린지 기록(사용 피드백 7) — 지금 하는 챌린지는 러닝 쪽에 있고, 여기는 끝낸 이력이다.
 * 원장에 적힌 적립(일일 목표 보너스 · 도전 보상)만 보인다. 받기 전 · 확인 전 진행은 여기 없다.
 */
@Composable
fun ChallengeHistoryScreen(onBack: () -> Unit, onOpenChallenges: () -> Unit) {
    val flow = androidx.compose.runtime.remember { AppServices.rewardRepository.challengeRewards() }
    val rows by flow.collectAsStateWithLifecycle(initialValue = null)
    // 요약은 목록 한도(200줄)와 무관하게 전부로 센다
    val totalsFlow = androidx.compose.runtime.remember { AppServices.rewardRepository.challengeTotals() }
    val totals by totalsFlow.collectAsStateWithLifecycle(initialValue = null)
    val goalDaysFlow = androidx.compose.runtime.remember { AppServices.rewardRepository.goalDays() }
    val goalDays by goalDaysFlow.collectAsStateWithLifecycle(initialValue = null)
    DetailPage(
        title = stringResource(Res.string.challenge_history_title),
        onBack = onBack,
        primaryActionLabel = stringResource(Res.string.challenge_history_current),
        onPrimaryAction = onOpenChallenges,
        primaryActionIcon = Icons.Filled.EmojiEvents,
    ) {
        val list = rows
        item {
            S2Stats(
                listOf(
                    stringResource(Res.string.challenge_history_done) to (totals?.count?.let { "${com.stepup.android.domain.formatGrouped((it).toLong())}" } ?: "—"),
                    stringResource(Res.string.challenge_history_goal_days) to (goalDays?.let { "${com.stepup.android.domain.formatGrouped((it).toLong())}" } ?: "—"),
                    stringResource(Res.string.challenge_history_total) to
                        (totals?.let { "+" + formatSupDown(it.total, 2) } ?: "—"),
                ),
                valueSize = 22.sp,
                modifier = Modifier.padding(vertical = 12.dp).testTag("challenge-history-summary"),
            )
        }
        when {
            list == null -> item { StatePanel(stringResource(Res.string.feed_loading), Icons.Filled.EmojiEvents, loading = true) }
            list.isEmpty() -> item {
                StatePanel(stringResource(Res.string.challenge_history_empty), Icons.Filled.EmojiEvents,
                    modifier = Modifier.testTag("challenge-history-empty"))
            }
            else -> items(list, key = { it.id }) { HistoryRow(it) }
        }
    }
}

@Composable
private fun HistoryRow(row: RewardEntity) {
    val date = kotlin.time.Instant.fromEpochMilliseconds(row.timestamp).toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("challenge-history-row"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (row.type == RewardType.BONUS_GOAL) Res.string.challenge_daily_title else Res.string.challenge_history_event),
                    color = Snow, style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "${date.year}.${date.month.number}.${date.day}" + if (row.description.isNotBlank()) " · ${row.description}" else "",
                    color = Slate, style = MaterialTheme.typography.bodySmall, maxLines = 1,
                )
            }
            Text("+" + formatSupDown(row.amount, 2) + " SUP", color = VoltText, style = MaterialTheme.typography.titleSmall)
        }
        HairlineDivider()
    }
}
