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
import androidx.compose.runtime.setValue
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
 * 함께 뛰기(UI 최종 통합안 2026-10-25 — 02_함께뛰기 · 06_이야기) — 큰 제목 + 세그먼트 셋(모집 중 · 크루 · 이야기).
 * 첫 칸 "모집 중"은 크루 러닝 대기실 · 번개러닝을 한 목록에 둔다(2026-10-26, `TogetherRooms.kt` — 예전 번개 사진 포스터를 대신한다).
 * 여기는 머리 · 제목과 크루 칸 · 시트가 함께 쓰는 부품이다.
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

/**
 * 승인제 크루 가입 신청(시안 apply_sheet) — 사진 · 동네 · 인원 · 승인제 · 이름 → 안내 → 취소 | 가입 신청.
 * 신청은 기존 crew_join(서버가 승인제면 신청으로 받는다) 그대로다 — 이 길은 한 줄 소개를 서버에 보내지 않아 입력 칸을 두지 않는다.
 */
@Composable
internal fun CrewApplySheet(crew: Crew, onApply: () -> Unit, onDismiss: () -> Unit) {
    val c = StepUpUi.colors
    val photo = rememberPlacePhoto(crew.area, seed = "crew-${crew.id}", widthDp = 60)
    val area = crew.area.ifBlank { stringResource(R.string.crew_area_here) }
    com.stepup.android.ui.components.RunSheet(onDismiss = onDismiss, modifier = Modifier.testTag("crew-apply-sheet")) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PlaceThumb(photo?.painter, size = 60.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.ui_crew_meta_approval, area, crew.memberCount), style = StepUpUi.type.caption, color = c.secondaryLabel)
                Text(crew.name, style = StepUpUi.type.section.copy(fontSize = 24.sp), color = c.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(stringResource(R.string.crew_apply_body), style = StepUpUi.type.body, color = c.label)
        Text(stringResource(R.string.crew_apply_note), style = StepUpUi.type.rowSubtitle, color = c.secondaryLabel, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            com.stepup.android.ui.components.SecondaryButton(stringResource(R.string.run_cancel), onClick = onDismiss, modifier = Modifier.weight(.36f))
            PrimaryButton(stringResource(R.string.crew_apply_send), onClick = onApply, modifier = Modifier.weight(.64f).testTag("crew-apply-send"))
        }
    }
}

/** 가입한 크루 포스터(시안 mycrew) — 활동 지역 사진 · 이름 · "동네 · 인원 · 승인제" · 흰 "크루 보기" */
@Composable
internal fun CrewPoster(crew: Crew, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val photo = rememberPlacePhoto(crew.area, seed = "crew-${crew.id}", widthDp = 361)
    val approval = crew.joinPolicy == CrewJoinPolicy.APPROVAL
    val area = crew.area.ifBlank { stringResource(R.string.crew_area_here) }
    PhotoCard(photo?.painter, colorFilter = photo?.colorFilter, height = 220.dp, modifier = modifier.padding(horizontal = 16.dp).testTag("crew-poster-${crew.id}")) {
        Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            Text(crew.name, style = StepUpUi.type.section.copy(fontSize = 24.sp), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(
                    if (approval) stringResource(R.string.ui_crew_meta_approval, area, crew.memberCount)
                    else stringResource(R.string.ui_crew_meta_people, area, crew.memberCount),
                    // 크루장에게는 기다리는 신청을 카드에서 바로 알린다
                    if (crew.owned && crew.pendingCount > 0) stringResource(R.string.crew_pending_badge, crew.pendingCount) else null,
                ).joinToString(" · "),
                style = StepUpUi.type.rowSubtitle.copy(fontSize = 14.sp), color = Color.White.copy(alpha = .8f), maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp),
            )
            Spacer(Modifier.height(14.dp))
            PrimaryButton(stringResource(R.string.crew_poster_open), onClick = onOpen, onPhoto = true,
                modifier = Modifier.fillMaxWidth().testTag("crew-poster-open"))
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
