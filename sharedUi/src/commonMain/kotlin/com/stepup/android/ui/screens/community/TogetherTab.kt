package com.stepup.android.ui.screens.community

import kotlinx.datetime.toLocalDateTime
import com.stepup.android.domain.formatPattern
import com.stepup.android.ui.theme.StepUpFonts
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
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.ui.res.*
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
import kotlin.time.Instant

/** Upcoming runs and joined crews come from the repositories; no sample cards. */
@Composable
fun TogetherTab(
    viewModel: CommunityViewModel,
    onOpenFlash: (Long) -> Unit,
    onWritePost: () -> Unit,
    onAllMeetups: () -> Unit,
    onOpenCrews: () -> Unit,
) {
    val posts by viewModel.boardPosts.collectAsStateWithLifecycle()
    val sync by viewModel.boardSync.collectAsStateWithLifecycle()
    val crews by viewModel.crews.collectAsStateWithLifecycle()
    val now by produceState(com.stepup.android.domain.epochMillis()) {
        while (true) { value = com.stepup.android.domain.epochMillis(); delay(30_000) }
    }
    val featured = featuredMeetup(posts, now)
    val upcoming = posts.filter { it.isFlash && it.crewId.isBlank() && it.meetAt > now && !it.isFull }
        .sortedWith(compareBy<com.stepup.android.domain.Post> { it.meetAt }.thenBy { it.id }).take(3)
    val locale = androidx.compose.ui.text.intl.Locale.current
    val zone = kotlinx.datetime.TimeZone.currentSystemDefault()
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
                        S2Kicker(stringResource(Res.string.community_upcoming) + " · ${com.stepup.android.domain.formatFixed((featured.distanceKm).toDouble(), 1)} km")
                        Spacer(Modifier.height(10.dp))
                        Text(
                            featured.title, color = Snow, textAlign = TextAlign.Center,
                            maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            style = androidx.compose.ui.text.TextStyle(
                                fontFamily = com.stepup.android.ui.theme.StepUpFonts.sans, fontWeight = FontWeight.SemiBold,
                                fontSize = 27.sp, lineHeight = 34.sp,
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("community-featured-title"),
                        )
                        Spacer(Modifier.height(10.dp))
                        S2Subtitle(featured.place)
                        Spacer(Modifier.height(22.dp))
                        val at = kotlin.time.Instant.fromEpochMilliseconds(featured.meetAt).toLocalDateTime(zone)
                        Text(
                            stringResource(Res.string.community_s2_meet_time) + " · " +
                                com.stepup.android.domain.DatePattern.mediumDate(locale.language, at),
                            color = Silver, fontSize = 12.sp,
                        )
                        S2Number(at.formatPattern("HH:mm", androidx.compose.ui.text.intl.Locale.current.language), 68.sp, Modifier.padding(vertical = 6.dp))
                        Text(
                            stringResource(Res.string.community_s2_people) + " " +
                                stringResource(Res.string.map_flash_people, featured.joinedCount, featured.capacity) +
                                " · " + stringResource(Res.string.flash_est_distance) + " ${com.stepup.android.domain.formatFixed((featured.distanceKm).toDouble(), 1)} km",
                            color = Silver, fontSize = 13.sp, textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            } else if (sync == BoardSyncState.Ready) {
                item { com.stepup.android.ui.components.StatePanel(
                    stringResource(Res.string.community_meetups_empty), androidx.compose.material.icons.Icons.Filled.Groups,
                ) }
            }
            if (others.isNotEmpty()) {
                item { Text(stringResource(Res.string.community_s2_next), color = Silver, fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp)) }
                items(others, key = { "meetup-${it.id}" }) { meetup ->
                    S2MeetupRow(meetup, locale, onClick = { onOpenFlash(meetup.id) })
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.community_my_crews), color = Silver, fontSize = 13.sp,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = onOpenCrews) { Text(stringResource(Res.string.me_see_all), color = Silver) }
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
                S2SideInfo(stringResource(Res.string.community_other_meetups), onClick = onAllMeetups,
                    modifier = Modifier.testTag("community-all-meetups"))
            },
            end = {
                if (featured != null) S2SideInfo(stringResource(Res.string.post_write), end = true, onClick = onWritePost)
            },
        ) {
            if (featured != null || sync == BoardSyncState.Ready) {
                S2RoundAction(
                    icon = if (featured != null) Icons.Filled.Groups else Icons.Filled.Edit,
                    label = stringResource(if (featured != null) Res.string.community_view_meetup else Res.string.post_write),
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
private fun S2MeetupRow(meetup: com.stepup.android.domain.Post, locale: androidx.compose.ui.text.intl.Locale, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().quietClickable(onClick).padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(meetup.title, color = Snow, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("community-meetup-${meetup.id}"))
                Text(kotlin.time.Instant.fromEpochMilliseconds(meetup.meetAt).toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                    .let { com.stepup.android.domain.DatePattern.mediumDateShortTime(locale.language, it) } +
                    " · " + meetup.place,
                    color = Silver, fontSize = 13.sp, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            Text(stringResource(Res.string.map_flash_people, meetup.joinedCount, meetup.capacity),
                color = Silver, fontSize = 13.sp)
        }
        com.stepup.android.ui.components.HairlineDivider()
    }
}
