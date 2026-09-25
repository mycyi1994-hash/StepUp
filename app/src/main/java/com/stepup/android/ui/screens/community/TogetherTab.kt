package com.stepup.android.ui.screens.community

import androidx.compose.material.icons.filled.Groups

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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

/** Illustration is decorative; all meetup details come from the real board repository. */
@Composable
internal fun TogetherTab(
    viewModel: CommunityViewModel,
    onOpenFlash: (Long) -> Unit,
    onWritePost: () -> Unit,
    onAllMeetups: () -> Unit,
) {
    val posts by viewModel.boardPosts.collectAsStateWithLifecycle()
    val sync by viewModel.boardSync.collectAsStateWithLifecycle()
    val now by produceState(System.currentTimeMillis()) {
        while (true) { value = System.currentTimeMillis(); delay(30_000) }
    }
    val featured = featuredMeetup(posts, now)
    val locale = LocalConfiguration.current.locales[0]
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val artworkHeight = (maxHeight * 0.42f / androidx.compose.ui.platform.LocalDensity.current.fontScale)
        .coerceIn(170.dp, 300.dp)
    Column(Modifier.fillMaxSize().padding(horizontal = StepUpDesign.Gutter)) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (sync != BoardSyncState.Ready) {
                item { BoardSyncCard(sync, onRetry = viewModel::refreshBoard) }
            }
            if (featured != null) {
                item {
                    GlowCard(spacing = 8.dp) {
                        Text(featured.title, color = Snow, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                            maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("community-featured-title"))
                        Text(featured.place, color = Silver, fontSize = 15.sp)
                        Text(Instant.ofEpochMilli(featured.meetAt).atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale)),
                            color = Silver, fontSize = 15.sp)
                        Text(stringResource(R.string.flash_est_distance) + " · %.1f km".format(featured.distanceKm),
                            color = Silver, fontSize = 15.sp)
                    }
                }
            } else if (sync == BoardSyncState.Ready) {
                item { com.stepup.android.ui.components.StatePanel(
                    stringResource(R.string.community_meetups_empty), androidx.compose.material.icons.Icons.Filled.Groups,
                ) }
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
}
