package com.stepup.android.ui.screens.community.home

import androidx.compose.foundation.background
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
import com.stepup.android.domain.CrewCard
import com.stepup.android.domain.CrewHomeSummary
import com.stepup.android.domain.CrewMeeting
import com.stepup.android.domain.MeetingResponse
import com.stepup.android.ui.screens.community.chat.ChatFace
import com.stepup.android.ui.screens.community.chat.ChatRetryState
import com.stepup.android.ui.screens.community.chat.ChatUnreadBadge
import com.stepup.android.ui.screens.community.crew.CrewAvatar
import com.stepup.android.ui.screens.community.crew.CrewBar
import com.stepup.android.ui.screens.community.crew.CrewConfirmSheet
import com.stepup.android.ui.screens.community.crew.CrewGutter
import com.stepup.android.ui.screens.community.crew.CrewImage
import com.stepup.android.ui.screens.community.crew.CrewLevelChip
import com.stepup.android.ui.screens.community.crew.CrewMembersLabel
import com.stepup.android.ui.screens.community.crew.CrewMissingPage
import com.stepup.android.ui.screens.community.crew.CrewPage
import com.stepup.android.ui.screens.community.crew.CrewRow
import com.stepup.android.ui.screens.community.crew.CrewSheet
import com.stepup.android.ui.screens.community.crew.CrewTopBar
import com.stepup.android.ui.screens.community.crew.crewInk
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
        crew == null || home == null -> CrewPage(Modifier.testTag("crew-home-waiting")) {
            CrewTopBar(stringResource(R.string.crewhome_bar), actions.onBack)
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
        CrewConfirmSheet(
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
    val ink = crewInk()
    CrewPage(Modifier.testTag(if (home.owner) "crew-home-owner" else "crew-home")) {
        CrewTopBar(stringResource(R.string.crewhome_bar), actions.onBack, onMore = onMore)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = CrewGutter)) {
            if (stale) {
                Text(
                    stringResource(R.string.crewhome_stale), color = ink.warn, fontSize = 12.5.sp,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).homeClickable(onClick = onRetry)
                        .padding(vertical = 12.dp).testTag("home-stale"),
                )
            }
            Spacer(Modifier.height(14.dp))
            Masthead(crew, home, actions)
            Spacer(Modifier.height(26.dp))
            val meeting = home.meeting
            if (meeting != null) {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ChatCard(home.unread, Modifier.weight(1.45f).fillMaxHeight(), actions.onChat)
                    NextRunCard(meeting, Modifier.weight(1f).fillMaxHeight()) { actions.onMeeting(meeting.id) }
                }
                Spacer(Modifier.height(22.dp))
                MeetingLine(meeting) { actions.onMeeting(meeting.id) }
            } else {
                // 23 — 모임만 비었다. 채팅은 그대로, 주간 기록과는 따로
                ChatButton(home.unread, actions.onChat)
                Spacer(Modifier.height(34.dp))
                HomeLabel(stringResource(R.string.crewhome_next))
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.crewhome_no_meeting_title), color = ink.text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("home-no-meeting"),
                )
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.crewhome_no_meeting_body), color = ink.secondary, fontSize = 13.5.sp, lineHeight = 22.sp)
            }
            Spacer(Modifier.height(28.dp))
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
    val ink = crewInk()
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(RoundedCornerShape(24.dp)).background(ink.card)
            .homeClickable(onClick = actions.onIntro)
            .padding(start = 24.dp, end = 20.dp, top = 22.dp, bottom = 14.dp).testTag("home-masthead"),
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                crew.name, color = ink.text, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("home-name"),
            )
            if (crew.tagline.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(crew.tagline, color = ink.text, fontSize = 16.5.sp, lineHeight = 30.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.heightIn(min = 44.dp).homeClickable(onClick = actions.onLevel).testTag("home-level"), contentAlignment = Alignment.Center) {
                    CrewLevelChip(crew.level)
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
                CrewAvatar(crew.leaderName, 24.dp, leaderFace(crew), ink.text)
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.crew_leader_named, crew.leaderName), color = ink.text, fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.End) {
            CrewImage(crew, 150.dp, 20.dp)
            Spacer(Modifier.weight(1f))
            if (crew.area.isNotBlank()) {
                Text(
                    crew.area, color = ink.secondary, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 150.dp).padding(bottom = 12.dp).testTag("home-area"),
                )
            }
        }
    }
}

/** 흰 크루 채팅 카드 — 읽지 않은 수(서버의 읽은 위치로 센 값, 읽고 돌아오면 바뀐다) */
@Composable
private fun ChatCard(unread: Int, modifier: Modifier, onClick: () -> Unit) {
    val ink = crewInk()
    Box(
        modifier.heightIn(min = 148.dp).clip(RoundedCornerShape(24.dp)).background(ink.primaryFace).homeClickable(onClick = onClick)
            .padding(start = 22.dp, end = 18.dp, top = 22.dp, bottom = 18.dp).testTag("home-chat"),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.crewhome_chat), color = ink.primaryText, fontSize = 23.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                if (unread > 0) ChatUnreadBadge(unread, Modifier.padding(start = 6.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(if (unread > 0) R.string.crewhome_chat_new else R.string.crewhome_chat_quiet),
                color = ink.primaryText.copy(alpha = 0.62f), fontSize = 13.sp, maxLines = 2,
            )
        }
        ChatBubbleIcon(ink.primaryText, Modifier.align(Alignment.BottomEnd))
    }
}

/** 23 — 모임이 없을 때의 넓은 크루 채팅 버튼 */
@Composable
private fun ChatButton(unread: Int, onClick: () -> Unit) {
    val ink = crewInk()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(RoundedCornerShape(16.dp)).background(ink.primaryFace)
            .homeClickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp).testTag("home-chat"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.crewhome_chat), color = ink.primaryText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        if (unread > 0) ChatUnreadBadge(unread, Modifier.padding(start = 10.dp))
    }
}

/** 다음 러닝 카드 — 오늘 · 내일 · 날짜, 큰 시각 */
@Composable
private fun NextRunCard(meeting: CrewMeeting, modifier: Modifier, onClick: () -> Unit) {
    val ink = crewInk()
    Column(
        modifier.heightIn(min = 148.dp).clip(RoundedCornerShape(24.dp)).background(ink.card).homeClickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 20.dp).testTag("home-next"),
    ) {
        HomeLabel(stringResource(R.string.crewhome_next))
        Spacer(Modifier.height(10.dp))
        val at = meeting.meetAt
        if (at != null) {
            Text(meetingDayLabel(at), color = ink.text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(Modifier.weight(1f))
            Text(homeClock(at), color = ink.text, fontSize = 32.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.testTag("home-next-time"))
        } else {
            Text(meeting.title, color = ink.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** 모임 줄 — 장소 · 거리, 서버가 센 참석 인원, 참석 여부(내 응답) */
@Composable
private fun MeetingLine(meeting: CrewMeeting, onClick: () -> Unit) {
    val ink = crewInk()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).homeClickable(onClick = onClick).padding(vertical = 6.dp).testTag("home-meeting")) {
            val place = meeting.place.ifBlank { meeting.title }
            Text(
                if (meeting.distanceKm > 0) stringResource(R.string.crewhome_place_km, place, km(meeting.distanceKm)) else place,
                color = ink.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.crewhome_attendees, meeting.attendees), color = ink.secondary, fontSize = 12.5.sp,
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
        Box(
            Modifier.widthIn(min = 110.dp).heightIn(min = 52.dp).clip(RoundedCornerShape(15.dp)).background(ink.secondaryButton)
                .homeClickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp).testTag("home-rsvp"),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, color = ink.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

/** 이번 주 함께 — 목표가 있으면 "126 / 160km · 79%" 와 진행 바(100% 까지만), 없으면 거리만 */
@Composable
private fun WeekBlock(home: CrewHomeSummary, onClick: () -> Unit) {
    val ink = crewInk()
    val progress = home.progress
    Column(Modifier.fillMaxWidth().homeClickable(onClick = onClick).padding(vertical = 24.dp).testTag("home-week")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.crewhome_week), color = ink.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (home.weekRunners > 0) Text(stringResource(R.string.crewhome_week_runners, home.weekRunners), color = ink.secondary, fontSize = 12.5.sp)
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                if (progress != null) stringResource(R.string.crewhome_week_of, km(home.weekKm), progress.goalKm)
                else stringResource(R.string.crewhome_week_km, km(home.weekKm)),
                color = ink.text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).testTag("home-week-km"),
            )
            if (progress != null && home.weekKm > 0) {
                Text(stringResource(R.string.crew_percent, progress.percent), color = ink.info, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (progress != null) {
            Spacer(Modifier.height(16.dp))
            CrewBar(progress.fraction, Modifier.fillMaxWidth().height(7.dp))
        }
        if (home.weekKm <= 0.0) {
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.crewhome_week_waiting), color = ink.secondary, fontSize = 13.sp, modifier = Modifier.testTag("home-week-waiting"))
        }
    }
}

/** 하단 크루장 공지 — 고정 공지(없으면 최근) 한 줄 */
@Composable
private fun NoticeLine(title: String, author: String, authorOwner: Boolean, onClick: () -> Unit) {
    val ink = crewInk()
    Row(
        Modifier.fillMaxWidth().homeClickable(onClick = onClick).padding(vertical = 22.dp).testTag("home-notice"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChatFace(author, authorOwner, 36.dp)
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(
                stringResource(if (authorOwner) R.string.crewhome_notice_owner else R.string.crewhome_notice_member, author),
                color = ink.info, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(title, color = ink.text, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = ink.secondary, modifier = Modifier.size(20.dp))
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
    val ink = crewInk()
    CrewSheet(stringResource(R.string.crewhome_bar), onDismiss, Modifier.testTag("home-menu")) {
        Spacer(Modifier.height(16.dp))
        CrewRow(stringResource(R.string.crewhome_menu_intro), { onPick(actions.onIntro) }, Modifier.testTag("home-menu-intro"))
        CrewRow(
            stringResource(R.string.crewhome_menu_members), { onPick(actions.onMembers) }, Modifier.testTag("home-menu-members"),
            value = stringResource(R.string.crewhome_menu_count, home.memberCount),
        )
        CrewRow(stringResource(R.string.crewhome_menu_notices), { onPick(actions.onNotices) }, Modifier.testTag("home-menu-notices"))
        CrewRow(stringResource(R.string.crewhome_menu_board), { onPick(actions.onBoard) }, Modifier.testTag("home-menu-board"))
        CrewRow(stringResource(R.string.crewhome_menu_lobby), { onPick(actions.onLobby) }, Modifier.testTag("home-menu-lobby"))
        if (home.owner) {
            CrewRow(stringResource(R.string.crewhome_menu_manage), { onPick(actions.onManage) }, Modifier.testTag("home-menu-manage"))
        } else {
            CrewRow(stringResource(R.string.crew_menu_leave), onLeave, Modifier.testTag("home-menu-leave"), titleColor = ink.warn)
        }
        Spacer(Modifier.height(36.dp))
    }
}
