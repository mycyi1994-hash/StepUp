package com.stepup.android.ui.screens.community.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import com.stepup.android.ui.screens.community.chat.blueText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.ui.screens.community.chat.BlueBar
import com.stepup.android.ui.screens.community.chat.BlueConfirmSheet
import com.stepup.android.ui.screens.community.chat.BlueGutter
import com.stepup.android.ui.screens.community.chat.BlueLevelChip
import com.stepup.android.ui.screens.community.chat.BluePage
import com.stepup.android.ui.screens.community.chat.BlueRow
import com.stepup.android.ui.screens.community.chat.BlueSheet
import com.stepup.android.ui.screens.community.chat.BlueTopBar
import com.stepup.android.ui.screens.community.chat.blueInk
import com.stepup.android.domain.CrewCard
import com.stepup.android.domain.CrewHomeSummary
import com.stepup.android.domain.CrewMeeting
import com.stepup.android.domain.MeetingResponse
import com.stepup.android.ui.screens.community.chat.ChatFace
import com.stepup.android.ui.screens.community.chat.ChatRetryState
import com.stepup.android.ui.screens.community.chat.ChatUnreadBadge
import com.stepup.android.ui.screens.community.crew.CrewAvatar
import com.stepup.android.ui.screens.community.crew.CrewImage
import com.stepup.android.ui.screens.community.crew.CrewMembersLabel
import com.stepup.android.ui.screens.community.crew.CrewMissingPage
import com.stepup.android.ui.screens.community.crew.crewProblemText
import com.stepup.android.ui.screens.community.crew.leaderFace

/** 홈에서 나가는 곳 — 뒤로는 실제로 들어온 길로 */
class CrewHomeActions(
    val onBack: () -> Unit,
    val onIntro: () -> Unit,
    val onLevel: () -> Unit,
    val onMembers: () -> Unit,
    val onPerson: (userId: String) -> Unit,
    val onChat: () -> Unit,
    val onMeeting: (meetingId: Long) -> Unit,
    val onWeek: () -> Unit,
    val onNotice: (noticeId: Long) -> Unit,
    val onNotices: () -> Unit,
    val onBoard: () -> Unit,
    val onLobby: () -> Unit,
    val onManage: () -> Unit,
    /** 크루를 나갔다 · 볼 수 없게 됐다 — 크루 목록으로 */
    val onList: () -> Unit,
    val onLeft: () -> Unit,
)

/**
 * 00 내 크루 홈 — 큰 크루 이미지 영역 → 흰 크루 채팅 · 다음 러닝 → 참석 여부 → 이번 주 함께 → 크루장 공지.
 * 모임이 없으면 23(채팅은 그대로), 처음 읽기 실패는 26, 참석 뒤 28 · 29. 크루장 권한만 사라지면 크루원 화면으로 바뀐다.
 */
@Composable
fun CrewHomeScreen(viewModel: CrewHomeViewModel, actions: CrewHomeActions) {
    val card by viewModel.card.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val failed by viewModel.failed.collectAsStateWithLifecycle()
    val stale by viewModel.stale.collectAsStateWithLifecycle()
    val ended by viewModel.ended.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val leave by viewModel.leave.collectAsStateWithLifecycle()
    val left by viewModel.left.collectAsStateWithLifecycle()
    var menu by rememberSaveable { mutableStateOf(false) }

    // 보일 때마다 다시 읽는다 — 채팅을 읽고 돌아오면 미확인 수가 바뀌고, 모임에서 응답하면 인원이 바뀐다
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }
    LaunchedEffect(left) { if (left) actions.onLeft() }

    val crew = card
    val home = summary
    when {
        ended -> {
            CrewMissingPage(actions.onBack, actions.onList)
            return
        }
        crew == null || home == null -> BluePage(Modifier.testTag("crew-home-waiting")) {
            BlueTopBar(stringResource(R.string.crewhome_bar), actions.onBack)
            if (failed && !loading) {
                // 26 — 제목 · 다시 불러오기를 이 페이지에 맞춘다
                ChatRetryState(
                    title = stringResource(R.string.crewhome_error_title),
                    body = stringResource(R.string.crewhome_error_body),
                    button = stringResource(R.string.crewhome_error_retry),
                    onRetry = viewModel::refresh,
                    modifier = Modifier.padding(top = 170.dp),
                    tag = "home-error",
                )
            } else {
                HomeSkeleton()
            }
        }
        else -> HomeContent(crew, home, stale, actions, onMore = { menu = true }, onRetry = viewModel::refresh)
    }

    if (menu && crew != null && home != null) {
        HomeMenuSheet(
            home,
            onDismiss = { menu = false },
            onPick = { action -> menu = false; action() },
            actions = actions,
            onLeave = { menu = false; viewModel.askLeave() },
        )
    }
    val confirm = leave
    if (confirm != null && crew != null) {
        BlueConfirmSheet(
            title = stringResource(R.string.crew_leave_title, crew.name),
            body = stringResource(R.string.crew_leave_body),
            confirm = stringResource(R.string.crew_menu_leave),
            onConfirm = viewModel::confirmLeave,
            onDismiss = viewModel::closeLeave,
            busy = confirm.busy,
            danger = true,
            tag = "home-leave",
            error = confirm.error?.let { crewProblemText(it) },
        )
    }
}

@Composable
private fun HomeContent(
    crew: CrewCard,
    home: CrewHomeSummary,
    stale: Boolean,
    actions: CrewHomeActions,
    onMore: () -> Unit,
    onRetry: () -> Unit,
) {
    val ink = blueInk()
    BluePage(Modifier.testTag(if (home.owner) "crew-home-owner" else "crew-home")) {
        BlueTopBar(stringResource(R.string.crewhome_bar), actions.onBack, onMore = onMore)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = BlueGutter)) {
            if (stale) {
                Text(
                    stringResource(R.string.crewhome_stale), style = blueText(14.sp, ink.warn, FontWeight.SemiBold),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).homeClickable(onClick = onRetry)
                        .padding(vertical = 12.dp).testTag("home-stale"),
                )
            }
            Spacer(Modifier.height(8.dp))
            Masthead(crew, home, actions)
            Spacer(Modifier.height(20.dp))
            val meeting = home.meeting
            if (meeting != null) {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChatCard(home.unread, Modifier.weight(1.45f).fillMaxHeight(), actions.onChat)
                    NextRunCard(meeting, Modifier.weight(1f).fillMaxHeight()) { actions.onMeeting(meeting.id) }
                }
                Spacer(Modifier.height(20.dp))
                MeetingLine(meeting) { actions.onMeeting(meeting.id) }
            } else {
                // 23 — 모임만 비었다. 채팅은 그대로, 주간 기록과는 따로. 가짜 모임 만들기 행동은 두지 않는다
                ChatButton(home.unread, actions.onChat)
                Spacer(Modifier.height(30.dp))
                HomeLabel(stringResource(R.string.crewhome_next))
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.crewhome_no_meeting_title), style = blueText(24.sp, ink.text, FontWeight.ExtraBold, 1.3f),
                    modifier = Modifier.testTag("home-no-meeting"),
                )
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.crewhome_no_meeting_body), style = blueText(15.5.sp, ink.secondary, FontWeight.Medium, 1.5f))
            }
            Spacer(Modifier.height(24.dp))
            HomeDivider()
            WeekBlock(home, actions.onWeek)
            home.notice?.let { notice ->
                HomeDivider()
                NoticeLine(
                    title = notice.title,
                    author = notice.authorName,
                    authorOwner = notice.authorId != null && notice.authorId == home.ownerId,
                    onClick = { actions.onNotice(notice.id) },
                )
            }
            Spacer(Modifier.navigationBarsPadding().height(28.dp))
        }
    }
}

/** 큰 크루 이미지 영역 — 이름 · 한 줄 소개 · Lv · 인원/정원 · 크루장 · 지역, 오른쪽 대표 이미지. 전체를 누르면 01 */
@Composable
private fun Masthead(crew: CrewCard, home: CrewHomeSummary, actions: CrewHomeActions) {
    val ink = blueInk()
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(shape)
            .background(Brush.linearGradient(listOf(ink.cardTop, ink.card)), shape).border(1.5.dp, ink.edgeStrong, shape)
            .homeClickable(onClick = actions.onIntro)
            .padding(start = 22.dp, end = 16.dp, top = 20.dp, bottom = 12.dp).testTag("home-masthead"),
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                crew.name, style = blueText(32.sp, ink.text, FontWeight.ExtraBold, 1.2f),
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("home-name"),
            )
            if (crew.tagline.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(crew.tagline, style = blueText(18.sp, ink.text, FontWeight.SemiBold, 1.5f), maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.heightIn(min = 48.dp).homeClickable(onClick = actions.onLevel).testTag("home-level"), contentAlignment = Alignment.Center) {
                    BlueLevelChip(crew.level, large = true)
                }
                Spacer(Modifier.width(14.dp))
                Box(
                    Modifier.heightIn(min = 44.dp).homeClickable(onClick = actions.onMembers).padding(horizontal = 4.dp).testTag("home-members"),
                    contentAlignment = Alignment.Center,
                ) {
                    CrewMembersLabel(home.memberCount, crew.capacity, ink.secondary)
                }
            }
            Row(
                Modifier.heightIn(min = 44.dp).homeClickable { actions.onPerson(home.ownerId) }.testTag("home-leader"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChatFace(crew.leaderName, owner = true, size = 30.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.crew_leader_named, crew.leaderName), style = blueText(15.sp, ink.text, FontWeight.Bold),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.End) {
            CrewImage(crew, 140.dp, 20.dp)
            Spacer(Modifier.weight(1f))
            if (crew.area.isNotBlank()) {
                Text(
                    crew.area, style = blueText(14.sp, ink.secondary, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 150.dp).padding(bottom = 12.dp).testTag("home-area"),
                )
            }
        }
    }
}

/** 흰 크루 채팅 카드 — 읽지 않은 수(서버의 읽은 위치로 센 값, 읽고 돌아오면 바뀐다) */
@Composable
private fun ChatCard(unread: Int, modifier: Modifier, onClick: () -> Unit) {
    val ink = blueInk()
    Box(
        modifier.heightIn(min = 152.dp).primaryFace(20.dp).homeClickable(onClick = onClick)
            .padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 18.dp).testTag("home-chat"),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.crewhome_chat), style = blueText(24.sp, ink.primaryText, FontWeight.ExtraBold, 1.25f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                if (unread > 0) ChatUnreadBadge(unread, Modifier.padding(start = 6.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(if (unread > 0) R.string.crewhome_chat_new else R.string.crewhome_chat_quiet),
                style = blueText(15.sp, ink.primaryText.copy(alpha = 0.7f), FontWeight.SemiBold), maxLines = 2,
            )
        }
        ChatBubbleIcon(ink.primaryText, Modifier.align(Alignment.BottomEnd).size(30.dp))
    }
}

/** 흰 면 + 얇은 파란 아랫면(주 버튼과 같은 결) */
@Composable
private fun Modifier.primaryFace(radius: androidx.compose.ui.unit.Dp): Modifier {
    val ink = blueInk()
    val shape = RoundedCornerShape(radius)
    return this
        .drawBehind { drawRoundRect(ink.primaryBase, cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius.toPx())) }
        .padding(bottom = 5.dp)
        .clip(shape)
        .background(ink.primaryFace, shape)
}

/** 23 — 모임이 없을 때의 넓은 크루 채팅 버튼 */
@Composable
private fun ChatButton(unread: Int, onClick: () -> Unit) {
    val ink = blueInk()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 84.dp).primaryFace(18.dp)
            .homeClickable(onClick = onClick).padding(start = 20.dp, end = 14.dp, top = 14.dp, bottom = 14.dp).testTag("home-chat"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.crewhome_chat), style = blueText(22.sp, ink.primaryText, FontWeight.ExtraBold, 1.25f))
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(if (unread > 0) R.string.crewhome_chat_new else R.string.crewhome_chat_quiet),
                style = blueText(15.sp, ink.primaryText.copy(alpha = 0.7f), FontWeight.SemiBold), maxLines = 2,
            )
        }
        if (unread > 0) ChatUnreadBadge(unread, Modifier.padding(start = 10.dp))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = ink.primaryText, modifier = Modifier.padding(start = 6.dp).size(24.dp))
    }
}

/** 다음 러닝 카드 — 오늘 · 내일 · 날짜, 큰 시각 */
@Composable
private fun NextRunCard(meeting: CrewMeeting, modifier: Modifier, onClick: () -> Unit) {
    val ink = blueInk()
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.heightIn(min = 152.dp).clip(shape).background(Brush.verticalGradient(listOf(ink.cardTop, ink.card)), shape)
            .border(1.5.dp, ink.edgeStrong, shape).homeClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 18.dp).testTag("home-next"),
    ) {
        Text(stringResource(R.string.crewhome_next), style = blueText(15.sp, ink.secondary, FontWeight.SemiBold))
        Spacer(Modifier.height(8.dp))
        val at = meeting.meetAt
        if (at != null) {
            Text(meetingDayLabel(at), style = blueText(20.sp, ink.text, FontWeight.Bold), maxLines = 1)
            Spacer(Modifier.weight(1f))
            Text(
                homeClock(at), style = blueText(38.sp, ink.text, FontWeight.ExtraBold, 1.1f), maxLines = 1, softWrap = false,
                modifier = Modifier.testTag("home-next-time"),
            )
        } else {
            Text(meeting.title, style = blueText(18.sp, ink.text, FontWeight.Bold, 1.35f), maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** 모임 줄 — 장소 · 거리, 서버가 센 참석 인원, 참석 여부(내 응답) */
@Composable
private fun MeetingLine(meeting: CrewMeeting, onClick: () -> Unit) {
    val ink = blueInk()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).homeClickable(onClick = onClick).padding(vertical = 6.dp).testTag("home-meeting")) {
            val place = meeting.place.ifBlank { meeting.title }
            Text(
                if (meeting.distanceKm > 0) stringResource(R.string.crewhome_place_km, place, km(meeting.distanceKm)) else place,
                style = blueText(18.sp, ink.text, FontWeight.Bold, 1.3f), maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.crewhome_attendees, meeting.attendees), style = blueText(15.sp, ink.secondary, FontWeight.SemiBold),
                modifier = Modifier.testTag("home-attendees"),
            )
        }
        Spacer(Modifier.width(12.dp))
        val label = when {
            meeting.myResponse == MeetingResponse.YES -> stringResource(R.string.crewhome_meeting_state_yes)
            meeting.myResponse == MeetingResponse.NO -> stringResource(R.string.crewhome_rsvp_no)
            !meeting.open -> stringResource(R.string.crewhome_rsvp_closed)
            else -> stringResource(R.string.crewhome_rsvp)
        }
        val shape = RoundedCornerShape(14.dp)
        Box(
            Modifier.widthIn(min = 112.dp).heightIn(min = 52.dp).clip(shape).background(ink.secondaryButton, shape)
                .border(1.5.dp, ink.edgeStrong, shape)
                .homeClickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp).testTag("home-rsvp"),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = blueText(16.sp, ink.text, FontWeight.Bold), maxLines = 1)
        }
    }
}

/** 이번 주 함께 — 목표가 있으면 "126 / 160km · 79%" 와 진행 바(100% 까지만), 없으면 거리만 */
@Composable
private fun WeekBlock(home: CrewHomeSummary, onClick: () -> Unit) {
    val ink = blueInk()
    val progress = home.progress
    Column(Modifier.fillMaxWidth().homeClickable(onClick = onClick).padding(vertical = 22.dp).testTag("home-week")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.crewhome_week), style = blueText(18.sp, ink.text, FontWeight.Bold), modifier = Modifier.weight(1f))
            if (home.weekRunners > 0) Text(stringResource(R.string.crewhome_week_runners, home.weekRunners), style = blueText(15.sp, ink.secondary, FontWeight.SemiBold))
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                if (progress != null) stringResource(R.string.crewhome_week_of, km(home.weekKm), progress.goalKm)
                else stringResource(R.string.crewhome_week_km, km(home.weekKm)),
                style = blueText(36.sp, ink.text, FontWeight.ExtraBold, 1.15f), modifier = Modifier.weight(1f).testTag("home-week-km"),
            )
            if (progress != null && home.weekKm > 0) {
                Text(stringResource(R.string.crew_percent, progress.percent), style = blueText(32.sp, ink.info, FontWeight.ExtraBold, 1.15f))
            }
        }
        if (progress != null) {
            Spacer(Modifier.height(14.dp))
            BlueBar(progress.fraction, Modifier.fillMaxWidth().height(10.dp))
        }
        if (home.weekKm <= 0.0) {
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.crewhome_week_waiting), style = blueText(15.sp, ink.secondary, FontWeight.Medium), modifier = Modifier.testTag("home-week-waiting"))
        }
    }
}

/** 하단 크루장 공지 — 고정 공지(없으면 최근) 한 줄 */
@Composable
private fun NoticeLine(title: String, author: String, authorOwner: Boolean, onClick: () -> Unit) {
    val ink = blueInk()
    Row(
        Modifier.fillMaxWidth().homeClickable(onClick = onClick).padding(vertical = 22.dp).testTag("home-notice"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChatFace(author, authorOwner, 44.dp)
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(
                stringResource(if (authorOwner) R.string.crewhome_notice_owner else R.string.crewhome_notice_member, author),
                style = blueText(14.sp, ink.info, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(title, style = blueText(17.sp, ink.text, FontWeight.SemiBold, 1.4f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = ink.secondary, modifier = Modifier.size(24.dp))
    }
}

/** 22 내 크루 더보기 — 소개 · 크루원 · 공지, 기존 게시판 · 같이 달리기, 크루원은 크루 나가기(61) · 크루장은 크루 관리 */
@Composable
private fun HomeMenuSheet(
    home: CrewHomeSummary,
    onDismiss: () -> Unit,
    onPick: (() -> Unit) -> Unit,
    actions: CrewHomeActions,
    onLeave: () -> Unit,
) {
    val ink = blueInk()
    BlueSheet(stringResource(R.string.crewhome_bar), onDismiss, Modifier.testTag("home-menu")) {
        Spacer(Modifier.height(8.dp))
        BlueRow(
            stringResource(R.string.crewhome_menu_intro), { onPick(actions.onIntro) }, Modifier.testTag("home-menu-intro"),
            leading = Icons.Outlined.Description,
        )
        BlueRow(
            stringResource(R.string.crewhome_menu_members), { onPick(actions.onMembers) }, Modifier.testTag("home-menu-members"),
            value = stringResource(R.string.crewhome_menu_count, home.memberCount), leading = Icons.Outlined.Group,
        )
        BlueRow(
            stringResource(R.string.crewhome_menu_notices), { onPick(actions.onNotices) }, Modifier.testTag("home-menu-notices"),
            leading = Icons.Outlined.Campaign,
        )
        BlueRow(
            stringResource(R.string.crewhome_menu_board), { onPick(actions.onBoard) }, Modifier.testTag("home-menu-board"),
            leading = Icons.Outlined.ChatBubbleOutline,
        )
        BlueRow(
            stringResource(R.string.crewhome_menu_lobby), { onPick(actions.onLobby) }, Modifier.testTag("home-menu-lobby"),
            leading = Icons.AutoMirrored.Filled.DirectionsRun,
        )
        if (home.owner) {
            BlueRow(
                stringResource(R.string.crewhome_menu_manage), { onPick(actions.onManage) }, Modifier.testTag("home-menu-manage"),
                leading = Icons.Outlined.Settings,
            )
        } else {
            BlueRow(
                stringResource(R.string.crew_menu_leave), onLeave, Modifier.testTag("home-menu-leave"), titleColor = ink.warn,
                leading = Icons.AutoMirrored.Outlined.Logout, leadingTint = ink.warn, divider = false,
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}
