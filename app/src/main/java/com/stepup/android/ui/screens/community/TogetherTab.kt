package com.stepup.android.ui.screens.community

import androidx.compose.material.icons.filled.Groups

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import com.stepup.android.ui.components.quietClickable
import com.stepup.android.ui.components.SectionHeader
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.data.repo.BoardSyncState
import com.stepup.android.domain.featuredMeetup
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.PrimaryCta
import com.stepup.android.ui.guide.GuideTour
import com.stepup.android.ui.guide.guideTarget
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.StepUpDesign
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Upcoming runs and joined crews come from the repositories; no sample cards. */
@Composable
internal fun TogetherTab(
    viewModel: CommunityViewModel,
    onOpenFlash: (Long) -> Unit,
    onWritePost: () -> Unit,
    onAllMeetups: () -> Unit,
    onOpenCrews: () -> Unit,
) {
    val posts by viewModel.boardPosts.collectAsStateWithLifecycle()
    val sync by viewModel.boardSync.collectAsStateWithLifecycle()
    val crews by viewModel.crews.collectAsStateWithLifecycle()
    val now by produceState(System.currentTimeMillis()) {
        while (true) { value = System.currentTimeMillis(); delay(30_000) }
    }
    val featured = featuredMeetup(posts, now)
    val upcoming = posts.filter { it.isFlash && it.crewId.isBlank() && it.meetAt > now && !it.isFull }
        .sortedWith(compareBy<com.stepup.android.domain.Post> { it.meetAt }.thenBy { it.id }).take(3)
    val locale = LocalConfiguration.current.locales[0]
    Column(Modifier.fillMaxSize().padding(horizontal = StepUpDesign.Gutter)) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (sync != BoardSyncState.Ready) {
                item { BoardSyncCard(sync, onRetry = viewModel::refreshBoard) }
            }
            item { SectionHeader(title = stringResource(R.string.community_upcoming)) }
            if (upcoming.isNotEmpty()) {
                items(upcoming, key = { "meetup-${it.id}" }) { meetup ->
                    GlowCard(modifier = Modifier.quietClickable { onOpenFlash(meetup.id) }, spacing = 8.dp) {
                        Text(meetup.title, color = Snow, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                            maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.testTag(if (meetup.id == featured?.id) "community-featured-title" else "community-meetup-${meetup.id}"))
                        Text(meetup.place, color = Silver, fontSize = 15.sp)
                        Text(Instant.ofEpochMilli(meetup.meetAt).atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale)),
                            color = Silver, fontSize = 15.sp)
                        Text(stringResource(R.string.flash_est_distance) + " · %.1f km".format(meetup.distanceKm),
                            color = Silver, fontSize = 15.sp)
                    }
                }
            } else if (sync == BoardSyncState.Ready) {
                item { com.stepup.android.ui.components.StatePanel(
                    stringResource(R.string.community_meetups_empty), androidx.compose.material.icons.Icons.Filled.Groups,
                ) }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.community_my_crews), color = Snow,
                        style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = onOpenCrews) { Text(stringResource(R.string.me_see_all)) }
                }
            }
            items(crews.filter { it.joined }.take(2), key = { "crew-${it.id}" }) { crew ->
                GlowCard(modifier = Modifier.quietClickable(onOpenCrews), spacing = 6.dp) {
                    Text(crew.name, color = Snow, style = MaterialTheme.typography.titleMedium)
                    Text(crew.area, color = Silver, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (featured != null || sync == BoardSyncState.Ready) {
            PrimaryCta(
                text = stringResource(if (featured != null) R.string.community_view_meetup else R.string.post_write),
                onClick = { if (featured != null) onOpenFlash(featured.id) else onWritePost() },
                modifier = Modifier.testTag("community-primary").guideTarget(GuideTour.Targets.COMMUNITY_WRITE),
            )
        }
        TextButton(onClick = onAllMeetups,
            modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp).testTag("community-all-meetups")) {
            Text(stringResource(R.string.community_other_meetups))
        }
    }
}
