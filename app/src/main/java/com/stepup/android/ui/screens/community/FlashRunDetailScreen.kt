package com.stepup.android.ui.screens.community

import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.core.ExternalIntents
import com.stepup.android.domain.FlashMember
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.Post
import com.stepup.android.domain.RewardEconomy
import com.stepup.android.ui.background.WeatherLayer
import com.stepup.android.ui.background.currentWeather
import com.stepup.android.ui.background.rememberPlacePhoto
import com.stepup.android.ui.components.BackButton
import com.stepup.android.ui.components.BottomActionBar
import com.stepup.android.ui.components.ButtonState
import com.stepup.android.ui.components.CircleIconButton
import com.stepup.android.ui.components.GroupedList
import com.stepup.android.ui.components.LiveCaption
import com.stepup.android.ui.components.NavListRow
import com.stepup.android.ui.components.PhotoCard
import com.stepup.android.ui.components.PrimaryButton
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.SecondaryButton
import com.stepup.android.ui.components.StatItem
import com.stepup.android.ui.components.StatRow
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.components.modalEnter
import com.stepup.android.ui.components.modalExit
import com.stepup.android.ui.components.popOnChange
import com.stepup.android.ui.components.rememberCurrentLocation
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 번개러닝 상세 — UI 최종 통합안(2026-10-25) `flash_detail` · `joined`. 카드 어디를 눌러도 여기로 들어온다.
 *
 * 위 사진 포스터(모이는 곳 사진 · 날씨 효과, 유리 원 버튼 ← · 공유 · ⋯) 아래쪽에 LIVE · 남은 시간 · 장소 → 큰 집결 시각 → 제목.
 * 그 아래 달릴 거리 · 나와의 거리 · 참여, "N자리 남았어요 · 호스트", 묶음 목록(파티 채팅 · 참여자), 맨 아래 주 버튼 하나.
 * 참여가 확인되면(서버) 아래에서 올라오는 참여 완료 화면 — 길찾기 · 파티 채팅 · 캘린더에 넣기 · 친구 부르기 | 확인.
 * 좋아요 · 참여 취소는 ⋯ 안에 둔다(지우지 않는다). 모든 값은 서버의 글에서 온다.
 */
@Composable
fun FlashRunDetailScreen(
    postId: Long,
    onBack: () -> Unit = {},
    onOpenLobby: () -> Unit = {},
    viewModel: CommunityViewModel = viewModel(factory = CommunityViewModel.Factory),
) {
    val posts by viewModel.allPosts.collectAsStateWithLifecycle()
    val post = posts.firstOrNull { it.id == postId }
    var showMembers by rememberSaveable { mutableStateOf(false) }
    var more by rememberSaveable { mutableStateOf(false) }
    // 참여를 눌렀고 서버가 받아 주면(joined 가 true 로 바뀌면) 참여 완료 화면
    var joining by rememberSaveable { mutableStateOf(false) }
    var joinedShown by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(post?.joined, joining) {
        if (joining && post?.joined == true) { joining = false; joinedShown = true }
    }
    // 실패하면(정원이 찼다 등) 로딩을 거둔다 — 같은 글이 다시 오면 joined 가 그대로 false
    LaunchedEffect(joining) { if (joining) { kotlinx.coroutines.delay(8_000); joining = false } }
    val here = rememberCurrentLocation()
    val context = LocalContext.current
    val c = StepUpUi.colors

    // "파티 채팅"이 여는 댓글 창은 이 화면 위에 그대로 뜬다
    CommentSheetHost(viewModel)

    Box(Modifier.fillMaxSize().background(c.background).testTag("flash-detail")) {
        if (post == null) {
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                BackButton(onBack)
                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.community_board_empty), style = StepUpUi.type.body, color = c.secondaryLabel)
            }
            return@Box
        }
        val share = { shareFlash(context, post) }
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                FlashPoster(post, onBack = onBack, onShare = share, onMore = { more = true })
                Spacer(Modifier.height(20.dp))
                FlashStats(post, here)
                Text(
                    if (post.capacity > 0 && !post.isFull) stringResource(R.string.flash_seats_left, post.capacity - post.joinedCount, post.author)
                    else stringResource(R.string.flash_seats_none, post.author),
                    style = StepUpUi.type.rowSubtitle, color = c.secondaryLabel,
                    modifier = Modifier.padding(horizontal = 20.dp).padding(top = 16.dp).testTag("flash-seats"),
                )
                if (post.body.isNotBlank()) {
                    Text(post.body, style = StepUpUi.type.body, color = c.label, modifier = Modifier.padding(horizontal = 20.dp).padding(top = 12.dp))
                }
                Spacer(Modifier.height(28.dp))
                GroupedList(
                    rows = listOf(
                        {
                            NavListRow(Icons.Outlined.ChatBubbleOutline, stringResource(R.string.flash_chat), { viewModel.openComments(post.id) },
                                value = if (post.commentCount > 0) post.commentCount.toString() else null,
                                modifier = Modifier.testTag("flash-chat"))
                        },
                        {
                            NavListRow(Icons.Outlined.Group, stringResource(R.string.flash_members), { showMembers = true },
                                value = stringResource(R.string.post_slots, post.joinedCount, post.capacity),
                                modifier = Modifier.testTag("flash-members"))
                        },
                    ),
                )
                // 참가만 눌러 놓고 끝나면 이 글은 게시판 글일 뿐이다 — 실제로 같이 뛰려면 대기실(준비하고 모이면 출발)로 간다
                if (post.joined && !post.isClosed) {
                    Text(
                        stringResource(R.string.flash_lobby_hint, RewardEconomy.partyBonusPercent(post.joinedCount.coerceAtLeast(1))),
                        style = StepUpUi.type.caption, color = c.secondaryLabel,
                        modifier = Modifier.padding(horizontal = 20.dp).padding(top = 12.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
            BottomActionBar {
                PrimaryButton(
                    stringResource(when {
                        post.isClosed -> R.string.flash_closed_badge
                        post.joined -> R.string.flash_lobby_cta
                        post.isFull -> R.string.flash_full_badge
                        else -> R.string.flash_join_go
                    }),
                    onClick = {
                        if (post.joined) onOpenLobby() else { joining = true; viewModel.toggleJoinFlash(post.id) }
                    },
                    state = when {
                        joining -> ButtonState.Loading
                        post.isClosed || (!post.joined && post.isFull) -> ButtonState.Disabled
                        else -> ButtonState.Idle
                    },
                    modifier = Modifier.weight(1f).testTag("flash-primary"),
                )
            }
        }
        AnimatedVisibility(joinedShown && post.joined, enter = modalEnter, exit = modalExit) {
            FlashJoined(post, onClose = { joinedShown = false }, onInvite = share, onChat = { viewModel.openComments(post.id) })
        }
    }

    if (more && post != null) {
        RunSheet(onDismiss = { more = false }, modifier = Modifier.testTag("flash-more-sheet")) {
            SecondaryButton(
                stringResource(if (post.liked) R.string.post_unlike_action else R.string.post_like_action),
                onClick = { more = false; viewModel.toggleLike(post.id) }, modifier = Modifier.fillMaxWidth(),
            )
            if (post.joined && !post.isClosed) {
                Spacer(Modifier.height(10.dp))
                com.stepup.android.ui.components.DangerButton(
                    stringResource(R.string.flash_cancel_join), onClick = { more = false; viewModel.toggleJoinFlash(post.id) },
                    modifier = Modifier.fillMaxWidth().testTag("flash-cancel-join"),
                )
            }
        }
    }
    if (showMembers && post != null) {
        val rosterFlow = remember(post.id, post.joinedCount) { viewModel.flashRoster(post.id) }
        val roster by rosterFlow.collectAsStateWithLifecycle(null)
        FlashMembersDialog(roster = roster, onDismiss = { showMembers = false })
    }
}

private fun meetClock(meetAt: Long): String =
    Instant.ofEpochMilli(meetAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))

/** 사진 포스터(시안 393 × 420) — 모이는 곳 사진 · 날씨 효과 · 아래로 어둡게, 위 유리 원 버튼, 아래 LIVE · 시각 · 제목 */
@Composable
internal fun FlashPoster(post: Post, onBack: () -> Unit, onShare: () -> Unit, onMore: () -> Unit) {
    val photo = rememberPlacePhoto(post.place, post.lat, post.lng, seed = "flash-${post.id}")
    val status = when {
        post.isClosed -> stringResource(R.string.flash_closed_badge)
        post.isFull -> stringResource(R.string.flash_full_badge)
        else -> startsInLabel(post.meetAt, System.currentTimeMillis())
    }
    PhotoCard(photo?.painter, colorFilter = photo?.colorFilter, height = 390.dp, radius = 0.dp, modifier = Modifier.testTag("flash-poster")) {
        WeatherLayer(currentWeather())
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack, onPhoto = true, modifier = Modifier.testTag("flash-back"))
            Spacer(Modifier.weight(1f))
            CircleIconButton(Icons.Outlined.IosShare, stringResource(R.string.flash_invite), onShare, onPhoto = true,
                modifier = Modifier.testTag("flash-share"))
            CircleIconButton(Icons.Rounded.MoreHoriz, stringResource(R.string.run_free_more), onMore, onPhoto = true,
                modifier = Modifier.testTag("flash-more"))
        }
        Column(Modifier.align(Alignment.BottomStart).padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
            LiveCaption(
                listOf(status, post.place.ifBlank { null }).filterNotNull().joinToString(" · "),
                live = !post.isClosed,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                meetClock(post.meetAt), style = StepUpUi.type.numberPoster.copy(fontSize = 112.sp, letterSpacing = .005.em),
                color = Color.White, maxLines = 1, modifier = Modifier.testTag("flash-time"),
            )
            Text(
                post.title, style = StepUpUi.type.section.copy(fontSize = 24.sp, letterSpacing = (-0.035).em), color = Color.White,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("flash-title"),
            )
        }
    }
}

/** 달릴 거리 · 나와의 거리 · 참여(바뀌면 살짝 튄다) */
@Composable
internal fun FlashStats(post: Post, here: GeoPoint?) {
    val away = post.awayKmFrom(here)
    Box(Modifier.popOnChange(post.joinedCount)) {
        StatRow(
            listOf(
                StatItem(stringResource(R.string.flash_est_distance), if (post.distanceKm > 0.0) "%.1f".format(post.distanceKm) else "—",
                    if (post.distanceKm > 0.0) "km" else null, tag = "flash-distance"),
                StatItem(stringResource(R.string.flash_from_me), away?.let { "%.1f".format(it) } ?: "—", if (away != null) "km" else null,
                    tag = "flash-away"),
                StatItem(stringResource(R.string.flash_count_label), "${post.joinedCount}/${post.capacity}", tag = "flash-count"),
            ),
            numberStyle = StepUpUi.type.numberStat.copy(fontSize = 40.sp),
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}

/**
 * 참여 완료(시안 joined) — 아래에서 스프링으로 올라온다. 사진 카드(✓ 참여 완료 · 시각 · 참여 수 · 제목) →
 * 대기실 안내 → 길찾기 · 파티 채팅 · 캘린더에 넣기 → 친구 부르기 | 확인.
 */
@Composable
internal fun FlashJoined(post: Post, onClose: () -> Unit, onInvite: () -> Unit, onChat: () -> Unit) {
    val c = StepUpUi.colors
    val context = LocalContext.current
    val photo = rememberPlacePhoto(post.place, post.lat, post.lng, seed = "flash-${post.id}")
    androidx.activity.compose.BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().background(c.background).testTag("flash-joined")) {
        Box(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 12.dp)) {
            CircleIconButton(Icons.Rounded.Close, stringResource(R.string.common_close), onClose, modifier = Modifier.align(Alignment.CenterEnd))
        }
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
            PhotoCard(photo?.painter, colorFilter = photo?.colorFilter, height = 360.dp, modifier = Modifier.padding(horizontal = 16.dp).padding(top = 4.dp)) {
                Row(Modifier.padding(start = 16.dp, top = 17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.flash_joined_done), style = StepUpUi.type.liveCaption, color = Color.White)
                }
                Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 18.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(meetClock(post.meetAt), style = StepUpUi.type.numberLarge, color = Color.White, maxLines = 1,
                            modifier = Modifier.weight(1f))
                        Text("${post.joinedCount}/${post.capacity}", style = StepUpUi.type.numberStat.copy(fontSize = 44.sp), color = Color.White,
                            modifier = Modifier.popOnChange(post.joinedCount).testTag("flash-joined-count"))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(post.title, style = StepUpUi.type.section.copy(fontSize = 22.sp), color = Color.White, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.flash_count_label), style = StepUpUi.type.caption, color = Color.White.copy(alpha = .75f))
                    }
                }
            }
            Text(
                stringResource(R.string.flash_lobby_hint, RewardEconomy.partyBonusPercent(post.joinedCount.coerceAtLeast(1))),
                style = StepUpUi.type.rowTitle.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold), color = c.label,
                modifier = Modifier.padding(horizontal = 20.dp).padding(top = 18.dp, bottom = 16.dp),
            )
            GroupedList(
                rows = listOfNotNull(
                    if (post.place.isNotBlank()) ({
                        NavListRow(Icons.Outlined.NearMe, stringResource(R.string.flash_route), { ExternalIntents.openPlaceInMaps(context, post.place) },
                            value = post.place, modifier = Modifier.testTag("flash-joined-route"))
                    }) else null,
                    {
                        NavListRow(Icons.Outlined.ChatBubbleOutline, stringResource(R.string.flash_chat), onChat,
                            value = stringResource(R.string.flash_chat_people, post.joinedCount))
                    },
                    {
                        NavListRow(Icons.Outlined.CalendarMonth, stringResource(R.string.flash_calendar), { addToCalendar(context, post) },
                            value = meetClock(post.meetAt), modifier = Modifier.testTag("flash-joined-calendar"))
                    },
                ),
            )
        }
        BottomActionBar {
            SecondaryButton(stringResource(R.string.flash_invite), onClick = onInvite, icon = Icons.Outlined.IosShare,
                modifier = Modifier.weight(.45f).testTag("flash-joined-invite"))
            PrimaryButton(stringResource(R.string.common_ok), onClick = onClose, arrow = false,
                modifier = Modifier.weight(.55f).testTag("flash-joined-ok"))
        }
    }
}

/** 친구 부르기 · 공유 — 제목 · 시각 · 장소를 글로(앱 밖 공유 창) */
private fun shareFlash(context: android.content.Context, post: Post) {
    val text = context.getString(R.string.flash_share_text, post.title, meetClock(post.meetAt), post.place.ifBlank { "—" })
    runCatching {
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** 캘린더에 넣기 — 기기 캘린더 앱의 새 일정 화면(집결 시각부터 1시간 · 장소 · 제목). 저장은 사용자가 그 앱에서 한다 */
private fun addToCalendar(context: android.content.Context, post: Post) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, post.meetAt)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, post.meetAt + 60 * 60_000L)
                .putExtra(CalendarContract.Events.TITLE, post.title)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, post.place)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

/** 참여자 — 서버의 명단 그대로(주최자가 첫 줄). 이니셜 동그라미 없이 이름 줄만(UI 최종 통합안) */
@Composable
private fun FlashMembersDialog(roster: List<FlashMember>?, onDismiss: () -> Unit) {
    val c = StepUpUi.colors
    RunSheet(onDismiss = onDismiss, modifier = Modifier.testTag("flash-members-sheet")) {
        Text(stringResource(R.string.flash_members), style = StepUpUi.type.section, color = c.label)
        Spacer(Modifier.height(12.dp))
        if (roster == null) {
            Text(stringResource(R.string.board_loading), style = StepUpUi.type.rowSubtitle, color = c.secondaryLabel)
        }
        Column(Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            roster.orEmpty().forEachIndexed { i, member ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(.5.dp).background(c.separator))
                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(member.name, style = StepUpUi.type.rowTitle, color = c.label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f))
                    if (member.isHost) Text(stringResource(R.string.flash_host), style = StepUpUi.type.caption, color = c.accentText)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SecondaryButton(stringResource(R.string.common_ok), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
    }
}
