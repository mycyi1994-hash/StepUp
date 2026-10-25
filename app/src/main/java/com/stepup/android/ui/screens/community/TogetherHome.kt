package com.stepup.android.ui.screens.community

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.data.repo.BoardSyncState
import com.stepup.android.data.repo.Crew
import com.stepup.android.data.repo.CrewJoinPolicy
import com.stepup.android.domain.Post
import com.stepup.android.domain.featuredMeetup
import com.stepup.android.ui.background.WeatherLayer
import com.stepup.android.ui.background.currentWeather
import com.stepup.android.ui.background.rememberPlacePhoto
import com.stepup.android.ui.components.ButtonState
import com.stepup.android.ui.components.CircleIconButton
import com.stepup.android.ui.components.GroupHeader
import com.stepup.android.ui.components.GroupRow
import com.stepup.android.ui.components.GroupedList
import com.stepup.android.ui.components.LargeTitle
import com.stepup.android.ui.components.LiveCaption
import com.stepup.android.ui.components.PhotoCard
import com.stepup.android.ui.components.PlaceThumb
import com.stepup.android.ui.components.PrimaryButton
import com.stepup.android.ui.components.SmallButton
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.components.TitleSubtitle
import com.stepup.android.ui.components.WeekCard
import com.stepup.android.ui.guide.GuideTour
import com.stepup.android.ui.guide.guideTarget
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/*
 * 함께 뛰기(UI 최종 통합안 2026-10-25 — 02_함께뛰기 · 03_번개러닝_목록 · 06_이야기) — 큰 제목 + 세그먼트 셋(번개러닝 · 크루 · 이야기).
 * 번개러닝: 가장 가까운 모임 하나를 사진 포스터(LIVE 한 줄 · 아주 큰 시간 · 제목 · 같이 달리기 + 공유), 이번 주 = 가로 사진 카드,
 * 아래 추천 크루(묶음 목록 + 작은 버튼). 모임이 없으면 같은 포스터에 "아직 예정된 번개러닝이 없어요" + 번개러닝 올리기.
 * 데이터 · 서버 호출은 예전 함께 뛰기(번개 모임 · 내 크루)와 같다 — 같은 뷰모델 값을 다르게 그릴 뿐이다.
 */

/** 머리 줄 — 오른쪽 원 버튼 셋(지도 · 크루 채팅 · 글쓰기). 글쓰기는 파란 연필 */
@Composable
internal fun TogetherTopBar(onMap: () -> Unit, onChat: () -> Unit, onWrite: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(Icons.Outlined.Map, stringResource(R.string.community_map_title), onMap, modifier = Modifier.testTag("community-map"))
        CircleIconButton(Icons.Outlined.ChatBubbleOutline, stringResource(R.string.ui_open_chat), onChat, modifier = Modifier.testTag("community-tab-chat"))
        CircleIconButton(
            Icons.Outlined.Edit, stringResource(R.string.post_write), onWrite,
            tint = StepUpUi.colors.accentText,
            modifier = Modifier.testTag("community-write").guideTarget(GuideTour.Targets.COMMUNITY_WRITE),
        )
    }
}

/** 큰 제목 "함께 뛰기" */
@Composable
internal fun TogetherTitle() {
    LargeTitle(stringResource(R.string.community_together), Modifier.padding(bottom = 2.dp))
}

/** 번개러닝 세그먼트 — 포스터 · 이번 주 · 추천 크루 */
@Composable
internal fun MeetupsSegment(
    viewModel: CommunityViewModel,
    onOpenFlash: (Long) -> Unit,
    onWritePost: () -> Unit,
    onAllMeetups: () -> Unit,
    onOpenMyCrews: () -> Unit,
) {
    val posts by viewModel.boardPosts.collectAsStateWithLifecycle()
    val sync by viewModel.boardSync.collectAsStateWithLifecycle()
    val crews by viewModel.crews.collectAsStateWithLifecycle()
    val now by produceState(System.currentTimeMillis()) {
        while (true) { value = System.currentTimeMillis(); delay(30_000) }
    }
    MeetupsContent(
        posts, crews, sync, now,
        onOpenFlash = onOpenFlash, onWritePost = onWritePost, onAllMeetups = onAllMeetups, onOpenMyCrews = onOpenMyCrews,
        onRetry = viewModel::refreshBoard, onToggleCrew = viewModel::toggleJoin,
    )
}

/** 번개러닝 본문 — 상태를 받아 그리기만 한다(기기 검사 · 시안 비교가 장면을 바로 넣어 그린다) */
@Composable
internal fun MeetupsContent(
    posts: List<Post>,
    crews: List<Crew>,
    sync: BoardSyncState,
    now: Long,
    onOpenFlash: (Long) -> Unit,
    onWritePost: () -> Unit,
    onAllMeetups: () -> Unit,
    onOpenMyCrews: () -> Unit,
    onRetry: () -> Unit,
    onToggleCrew: (String) -> Unit,
) {
    val featured = featuredMeetup(posts, now)
    // 이번 주 — 앞으로 7일 안에 열리는 다른 번개(포스터에 올린 것은 뺀다)
    val week = posts.filter { it.isFlash && it.crewId.isBlank() && it.meetAt > now && it.id != featured?.id && it.meetAt - now < 7L * 24 * 3600_000 }
        .sortedWith(compareBy<Post> { it.meetAt }.thenBy { it.id })
    val recommended = crews.filterNot { it.joined }.sortedWith(compareByDescending<Crew> { it.requested }.thenBy { it.kmAway }).take(3)
    val weather = currentWeather()
    LazyColumn(Modifier.fillMaxSize().testTag("community-meetups"), contentPadding = PaddingValues(bottom = 24.dp)) {
        if (sync != BoardSyncState.Ready) {
            item { Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { BoardSyncCard(sync, onRetry = onRetry, compact = true) } }
        }
        item {
            Box(Modifier.padding(horizontal = StepUpUi.Gutter).padding(top = 16.dp)) {
                if (featured != null) {
                    MeetupPoster(featured, now, weather, onOpen = { onOpenFlash(featured.id) })
                } else {
                    EmptyMeetupPoster(weather, onWritePost)
                }
            }
        }
        if (week.isNotEmpty()) {
            item {
                GroupHeader(
                    stringResource(R.string.ui_this_week), action = stringResource(R.string.ui_see_all),
                    actionTag = "community-all-meetups", onAction = onAllMeetups,
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = StepUpUi.Gutter),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.testTag("community-week"),
                ) {
                    items(week, key = { "week-${it.id}" }) { meetup -> MeetupWeekCard(meetup, now, onClick = { onOpenFlash(meetup.id) }) }
                }
            }
        } else if (featured != null) {
            // 이번 주 다른 모임이 없어도 지난 · 다른 모임 목록으로 가는 길은 둔다
            item {
                GroupHeader(
                    stringResource(R.string.ui_this_week), action = stringResource(R.string.ui_see_all),
                    actionTag = "community-all-meetups", onAction = onAllMeetups,
                )
            }
        }
        if (recommended.isNotEmpty()) {
            item {
                GroupHeader(
                    stringResource(R.string.ui_recommended_crews), action = stringResource(R.string.ui_see_all),
                    actionTag = "community-crews-all", onAction = onOpenMyCrews,
                )
            }
            item {
                GroupedList(
                    rows = recommended.map { crew ->
                        @Composable { CrewSuggestionRow(crew, onToggle = { onToggleCrew(crew.id) }, onOpen = onOpenMyCrews) }
                    },
                )
            }
        }
    }
}

/** 가장 가까운 번개 — 사진 포스터(모서리 24) · LIVE 한 줄 · 큰 시간 · 제목 · 장소 · 인원 · 같이 달리기 + 공유 */
@Composable
private fun MeetupPoster(post: Post, now: Long, weather: com.stepup.android.ui.background.Weather, onOpen: () -> Unit) {
    val context = LocalContext.current
    val photo = rememberPlacePhoto(post.place, post.lat, post.lng, seed = "flash-${post.id}")
    val zone = ZoneId.systemDefault()
    val at = Instant.ofEpochMilli(post.meetAt).atZone(zone)
    PhotoCard(photo?.painter, height = 344.dp, colorFilter = photo?.colorFilter, modifier = Modifier.testTag("community-poster")) {
        WeatherLayer(weather, Modifier.matchParentSize())
        Column(Modifier.matchParentSize().padding(20.dp)) {
            LiveCaption(startsInLabel(post.meetAt, now), live = post.meetAt - now <= 3 * 3600_000L)
            Spacer(Modifier.weight(1f))
            Text(at.format(DateTimeFormatter.ofPattern("HH:mm")), style = StepUpUi.type.numberPoster, color = Color.White, maxLines = 1)
            Text(
                post.title, style = StepUpUi.type.section.copy(fontSize = 24.sp), color = Color.White,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp).testTag("community-featured-title"),
            )
            Text(
                stringResource(R.string.ui_place_people, post.place.ifBlank { "—" }, post.joinedCount, post.capacity),
                style = StepUpUi.type.caption.copy(fontSize = 14.sp), color = Color.White.copy(alpha = .8f),
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PrimaryButton(
                    stringResource(R.string.ui_run_together), onOpen, Modifier.weight(1f).testTag("community-primary"), onPhoto = true,
                )
                Spacer(Modifier.width(10.dp))
                CircleIconButton(
                    Icons.Outlined.IosShare, stringResource(R.string.ui_share), { shareMeetup(context, post) },
                    size = 54.dp, onPhoto = true,
                )
            }
        }
    }
}

/** 모임이 없을 때 — 같은 포스터에 "아직 예정된 번개러닝이 없어요" + ⚡ 번개러닝 올리기 */
@Composable
private fun EmptyMeetupPoster(weather: com.stepup.android.ui.background.Weather, onWrite: () -> Unit) {
    val photo = rememberPlacePhoto("망원 한강공원", seed = "empty")
    PhotoCard(photo?.painter, height = 300.dp, colorFilter = photo?.colorFilter, modifier = Modifier.testTag("community-poster")) {
        WeatherLayer(weather, Modifier.matchParentSize())
        Column(Modifier.matchParentSize().padding(20.dp)) {
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.ui_meetups_empty_title), style = StepUpUi.type.largeTitle.copy(fontSize = 30.sp),
                color = Color.White, modifier = Modifier.testTag("community-featured-title"),
            )
            Text(
                stringResource(R.string.ui_meetups_empty_body), style = StepUpUi.type.caption.copy(fontSize = 14.sp),
                color = Color.White.copy(alpha = .8f), modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                stringResource(R.string.ui_meetups_post), onWrite, Modifier.fillMaxWidth().testTag("community-primary"),
                onPhoto = true, icon = Icons.Rounded.Bolt,
            )
        }
    }
}

/** 이번 주 가로 카드 — 위 D-n(이탤릭), 아래 시간(숫자 글꼴) · 제목 · "내일 · 3/6 · 마감" */
@Composable
private fun MeetupWeekCard(post: Post, now: Long, onClick: () -> Unit) {
    val zone = ZoneId.systemDefault()
    val at = Instant.ofEpochMilli(post.meetAt).atZone(zone)
    val days = ChronoUnit.DAYS.between(LocalDate.now(zone), at.toLocalDate()).toInt()
    val dayLabel = when (days) {
        0 -> stringResource(R.string.ui_today)
        1 -> stringResource(R.string.ui_tomorrow)
        else -> at.format(DateTimeFormatter.ofPattern("E", LocalConfiguration.current.locales[0]))
    }
    val photo = rememberPlacePhoto(post.place, post.lat, post.lng, seed = "flash-${post.id}", widthDp = 168)
    val sub = buildString {
        append(dayLabel)
        append(" · ")
        append("${post.joinedCount}/${post.capacity}")
        if (post.isFull) append(" · ").append(stringResource(R.string.post_closed))
    }
    WeekCard(
        photo = photo?.painter, badge = if (days <= 0) "D-DAY" else "D-$days", time = at.format(DateTimeFormatter.ofPattern("HH:mm")),
        title = post.title, sub = sub, onClick = onClick, modifier = Modifier.testTag("community-week-${post.id}"),
    )
}

/** 추천 크루 한 줄 — 동네 사진 52 · 이름 · "동네 · N명 · 승인제" · 작은 버튼(가입 · 신청 · 신청됨) */
@Composable
internal fun CrewSuggestionRow(crew: Crew, onToggle: () -> Unit, onOpen: () -> Unit) {
    val photo = rememberPlacePhoto(crew.area, seed = "crew-${crew.id}", widthDp = 52)
    val approval = crew.joinPolicy == CrewJoinPolicy.APPROVAL
    val area = crew.area.ifBlank { stringResource(R.string.crew_area_here) }
    GroupRow(onClick = onOpen, minHeight = 76.dp) {
        PlaceThumb(photo?.painter)
        TitleSubtitle(
            crew.name,
            if (approval) stringResource(R.string.ui_crew_meta_approval, area, crew.memberCount)
            else stringResource(R.string.ui_crew_meta_people, area, crew.memberCount),
            Modifier.weight(1f),
        )
        when {
            crew.joined -> SmallButton(stringResource(R.string.ui_crew_open), onOpen)
            crew.requested -> SmallButton(stringResource(R.string.ui_crew_requested), onToggle, kind = ButtonState.Waiting)
            approval -> SmallButton(stringResource(R.string.ui_crew_apply), onToggle)
            else -> SmallButton(stringResource(R.string.ui_crew_join), onToggle)
        }
    }
}

/** "23분 뒤 출발" · "3시간 뒤 출발" · 날짜(멀면) */
@Composable
internal fun startsInLabel(meetAt: Long, now: Long): String {
    val minutes = ((meetAt - now) / 60_000L).coerceAtLeast(0L)
    return when {
        minutes < 60 -> stringResource(R.string.ui_starts_in_min, minutes.toInt())
        minutes < 24 * 60 -> stringResource(R.string.ui_starts_in_hour, (minutes / 60).toInt())
        else -> Instant.ofEpochMilli(meetAt).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("M/d E", LocalConfiguration.current.locales[0]))
    }
}

/** 모임 공유 — 제목 · 시간 · 장소를 글로 보낸다(앱 밖 공유 시트) */
internal fun shareMeetup(context: Context, post: Post) {
    val at = Instant.ofEpochMilli(post.meetAt).atZone(ZoneId.systemDefault())
    val text = listOf(post.title, at.format(DateTimeFormatter.ofPattern("M/d HH:mm")) + " · " + post.place).filter { it.isNotBlank() }.joinToString("\n")
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching { context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** 사진 썸네일만(크루 화면 등에서 다시 쓴다) */
@Composable
internal fun CrewPhotoThumb(area: String, seed: String) {
    val photo = rememberPlacePhoto(area, seed = seed, widthDp = 52)
    PlaceThumb(photo?.painter)
}

/** 포스터에 쓰는 사진만 — Image 로 직접 그리는 곳 */
@Composable
internal fun PlacePhotoImage(place: String, seed: String, modifier: Modifier = Modifier) {
    val photo = rememberPlacePhoto(place, seed = seed)
    if (photo != null) {
        Image(photo.painter, null, modifier = modifier, contentScale = androidx.compose.ui.layout.ContentScale.Crop, colorFilter = photo.colorFilter)
    }
}
