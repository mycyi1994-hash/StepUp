package com.stepup.android.ui.screens.community

import androidx.compose.material.icons.filled.Groups

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import com.stepup.android.ui.components.quietClickable
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.text.style.TextAlign
import com.stepup.android.ui.components.S2ActionRow
import com.stepup.android.ui.components.S2Kicker
import com.stepup.android.ui.components.S2Number
import com.stepup.android.ui.components.S2RoundAction
import com.stepup.android.ui.components.S2SideInfo
import com.stepup.android.ui.components.S2Subtitle
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
    val zone = ZoneId.systemDefault()
    val others = upcoming.filter { it.id != featured?.id }
    // S2 같이 뛰기 — 가장 가까운 모임 하나를 크게(제목 · 장소 · 큰 시간 · 인원), 아래로 이어지는 모임과 내 크루.
    Column(Modifier.fillMaxSize().padding(horizontal = StepUpDesign.Gutter)) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (sync != BoardSyncState.Ready) {
                item { BoardSyncCard(sync, onRetry = viewModel::refreshBoard, compact = true) }
            }
            if (featured != null) {
                item {
                    Column(
                        Modifier.fillMaxWidth().quietClickable { onOpenFlash(featured.id) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(Modifier.height(8.dp))
                        S2Kicker(stringResource(R.string.community_upcoming) + " · %.1f km".format(featured.distanceKm))
                        Spacer(Modifier.height(10.dp))
                        Text(
                            featured.title, color = Snow, textAlign = TextAlign.Center,
                            maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            style = androidx.compose.ui.text.TextStyle(
                                fontFamily = com.stepup.android.ui.theme.StepUpSans, fontWeight = FontWeight.SemiBold,
                                fontSize = 27.sp, lineHeight = 34.sp,
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("community-featured-title"),
                        )
                        Spacer(Modifier.height(10.dp))
                        S2Subtitle(featured.place)
                        Spacer(Modifier.height(22.dp))
                        val at = Instant.ofEpochMilli(featured.meetAt).atZone(zone)
                        Text(
                            stringResource(R.string.community_s2_meet_time) + " · " +
                                at.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)),
                            color = Silver, fontSize = 12.sp,
                        )
                        S2Number(at.format(DateTimeFormatter.ofPattern("HH:mm")), 68.sp, Modifier.padding(vertical = 6.dp))
                        Text(
                            stringResource(R.string.community_s2_people) + " " +
                                stringResource(R.string.map_flash_people, featured.joinedCount, featured.capacity) +
                                " · " + stringResource(R.string.flash_est_distance) + " %.1f km".format(featured.distanceKm),
                            color = Silver, fontSize = 13.sp, textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            } else if (sync == BoardSyncState.Ready) {
                item { com.stepup.android.ui.components.StatePanel(
                    stringResource(R.string.community_meetups_empty), androidx.compose.material.icons.Icons.Filled.Groups,
                ) }
            }
            if (others.isNotEmpty()) {
                item { Text(stringResource(R.string.community_s2_next), color = Silver, fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp)) }
                items(others, key = { "meetup-${it.id}" }) { meetup ->
                    S2MeetupRow(meetup, locale, onClick = { onOpenFlash(meetup.id) })
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.community_my_crews), color = Silver, fontSize = 13.sp,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = onOpenCrews) { Text(stringResource(R.string.me_see_all), color = Silver) }
                }
            }
            items(crews.filter { it.joined }.take(2), key = { "crew-${it.id}" }) { crew ->
                Column(Modifier.fillMaxWidth().quietClickable(onOpenCrews).padding(vertical = 6.dp)) {
                    Text(crew.name, color = Snow, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(crew.area, color = Silver, fontSize = 13.sp)
                }
                com.stepup.android.ui.components.HairlineDivider()
            }
        }
        S2ActionRow(
            Modifier.padding(vertical = 8.dp),
            start = {
                S2SideInfo(stringResource(R.string.community_other_meetups), onClick = onAllMeetups,
                    modifier = Modifier.testTag("community-all-meetups"))
            },
            end = {
                if (featured != null) S2SideInfo(stringResource(R.string.post_write), end = true, onClick = onWritePost)
            },
        ) {
            if (featured != null || sync == BoardSyncState.Ready) {
                S2RoundAction(
                    icon = if (featured != null) Icons.Filled.Groups else Icons.Filled.Edit,
                    label = stringResource(if (featured != null) R.string.community_view_meetup else R.string.post_write),
                    onClick = { if (featured != null) onOpenFlash(featured.id) else onWritePost() },
                    modifier = Modifier.testTag("community-primary").guideTarget(GuideTour.Targets.COMMUNITY_WRITE),
                )
            } else {
                Spacer(Modifier.width(88.dp))
            }
        }
    }
}

/** 이어서 열리는 모임 한 줄 — 제목, 시간 · 장소, 인원 */
@Composable
private fun S2MeetupRow(meetup: com.stepup.android.domain.Post, locale: java.util.Locale, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().quietClickable(onClick).padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(meetup.title, color = Snow, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("community-meetup-${meetup.id}"))
                Text(Instant.ofEpochMilli(meetup.meetAt).atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale)) +
                    " · " + meetup.place,
                    color = Silver, fontSize = 13.sp, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            Text(stringResource(R.string.map_flash_people, meetup.joinedCount, meetup.capacity),
                color = Silver, fontSize = 13.sp)
        }
        com.stepup.android.ui.components.HairlineDivider()
    }
}
