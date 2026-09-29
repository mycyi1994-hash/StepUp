package com.stepup.android.ui.screens.community.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.repo.HomeProblem
import com.stepup.android.domain.CrewMeeting
import com.stepup.android.domain.MeetingAttendee
import com.stepup.android.domain.MeetingResponse
import com.stepup.android.ui.components.SettingsToast
import com.stepup.android.ui.screens.community.chat.ChatFace
import com.stepup.android.ui.screens.community.chat.ChatRetryState
import com.stepup.android.ui.screens.community.crew.CrewBottomBar
import com.stepup.android.ui.screens.community.crew.CrewButton
import com.stepup.android.ui.screens.community.crew.CrewButtonKind
import com.stepup.android.ui.screens.community.crew.CrewEmptyState
import com.stepup.android.ui.screens.community.crew.CrewGutter
import com.stepup.android.ui.screens.community.crew.CrewPage
import com.stepup.android.ui.screens.community.crew.CrewSheet
import com.stepup.android.ui.screens.community.crew.CrewSkeletonBox
import com.stepup.android.ui.screens.community.crew.CrewTopBar
import com.stepup.android.ui.screens.community.crew.crewInk
import kotlinx.coroutines.delay

/** "9월 29일 화요일 · 19:30" */
@Composable
internal fun meetingWhenLong(meeting: CrewMeeting): String {
    val at = meeting.meetAt ?: return ""
    return stringResource(R.string.crewhome_meeting_when_place, homeDateWeekday(localDay(at)), homeClock(at))
}

// ─────────────────────────────────────────────────────────────
// 09 다음 러닝 · 10 참석 여부 변경 · 11 참석 · 12 불참 · 25 저장 실패
// ─────────────────────────────────────────────────────────────

/**
 * 09 — 처음에는 참석 · 불참 두 버튼, 저장 중에는 둘 다 잠시 끄고 누른 버튼에 "저장 중…". 저장되면 11 · 12(서버가 센 인원),
 * 그 뒤로는 "참석 여부 변경"(10). 실패는 이전 응답 · 인원을 그대로 두고 같은 자리에(25). 지난 모임 · 취소는 서버 상태대로.
 */
@Composable
fun CrewMeetingScreen(
    viewModel: CrewMeetingViewModel,
    crewName: String,
    onBack: () -> Unit,
    onPlace: () -> Unit,
    onAttendees: () -> Unit,
    onChat: () -> Unit,
    onEnded: () -> Unit,
) {
    val ink = crewInk()
    val meeting by viewModel.meeting.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val sheet by viewModel.sheet.collectAsStateWithLifecycle()
    val ended by viewModel.ended.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.load()
        onPauseOrDispose {}
    }
    LaunchedEffect(ended) { if (ended) onEnded() }

    when (val state = meeting) {
        is HomeLoad.Ready -> {
            val current = state.value
            val tag = when (current.myResponse) {
                MeetingResponse.YES -> "home-meeting-yes"
                MeetingResponse.NO -> "home-meeting-no"
                null -> "home-meeting"
            }
            CrewPage(Modifier.testTag(tag)) {
                CrewTopBar(stringResource(if (current.open) R.string.crewhome_meeting_bar else R.string.crewhome_meeting_past_bar), onBack)
                MeetingBody(current, crewName, Modifier.weight(1f), onPlace, onAttendees)
                MeetingFooter(current, saving, if (sheet) null else error, viewModel::respond, viewModel::openSheet)
            }
            if (sheet) AttendanceSheet(current, saving, error, viewModel::respond, viewModel::closeSheet)
        }
        HomeLoad.Missing -> CrewPage(Modifier.testTag("home-meeting-cancelled")) {
            CrewTopBar(stringResource(R.string.crewhome_meeting_past_bar), onBack)
            CrewEmptyState(
                icon = { Icon(Icons.Outlined.Info, null, tint = ink.info, modifier = Modifier.size(44.dp)) },
                title = stringResource(R.string.crewhome_meeting_cancelled_title),
                body = stringResource(R.string.crewhome_meeting_cancelled_body),
                modifier = Modifier.padding(top = 150.dp),
            ) { CrewButton(stringResource(R.string.crewhome_meeting_to_chat), onChat, Modifier.testTag("home-meeting-chat")) }
        }
        HomeLoad.Failed -> CrewPage(Modifier.testTag("home-meeting-error")) {
            CrewTopBar(stringResource(R.string.crewhome_meeting_bar), onBack)
            ChatRetryState(
                title = stringResource(R.string.crewhome_meeting_load_error),
                body = stringResource(R.string.crewhome_error_body),
                button = stringResource(R.string.crewhome_error_retry),
                onRetry = viewModel::load,
                modifier = Modifier.padding(top = 150.dp),
                tag = "home-meeting-load",
            )
        }
        HomeLoad.Loading -> CrewPage(Modifier.testTag("home-meeting-loading")) {
            CrewTopBar(stringResource(R.string.crewhome_meeting_bar), onBack)
            Column(Modifier.padding(CrewGutter)) {
                CrewSkeletonBox(Modifier.width(220.dp).height(34.dp))
                Spacer(Modifier.height(24.dp))
                CrewSkeletonBox(Modifier.fillMaxWidth().height(180.dp), 18.dp)
            }
        }
    }
}

@Composable
private fun MeetingBody(meeting: CrewMeeting, crewName: String, modifier: Modifier, onPlace: () -> Unit, onAttendees: () -> Unit) {
    val ink = crewInk()
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = CrewGutter)) {
        Spacer(Modifier.height(20.dp))
        if (crewName.isNotBlank()) {
            HomeLabel(crewName)
            Spacer(Modifier.height(12.dp))
        }
        Text(
            meeting.title, color = ink.text, fontSize = 27.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag("home-meeting-title"),
        )
        meeting.meetAt?.let { at ->
            Spacer(Modifier.height(18.dp))
            Text(homeDateWeekday(localDay(at)), color = ink.secondary, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    homeClock(at), color = ink.text, fontSize = 54.sp, lineHeight = 60.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).testTag("home-meeting-time"),
                )
                val state = when (meeting.myResponse) {
                    MeetingResponse.YES -> stringResource(R.string.crewhome_meeting_state_yes)
                    MeetingResponse.NO -> stringResource(R.string.crewhome_meeting_state_no)
                    null -> null
                }
                if (state != null) {
                    Box(
                        Modifier.clip(RoundedCornerShape(10.dp)).background(ink.secondaryButton).padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("home-meeting-state"),
                    ) { Text(state, color = ink.levelText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        meeting.point?.let { point ->
            Box(Modifier.fillMaxWidth().height(180.dp).homeClickable(onClick = onPlace)) {
                MeetingMap(point, meeting.place, Modifier.fillMaxWidth().height(180.dp))
            }
            Spacer(Modifier.height(10.dp))
        }
        if (meeting.place.isNotBlank()) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 60.dp).homeClickable(onClick = onPlace).padding(vertical = 10.dp).testTag("home-meeting-place"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(meeting.place, color = ink.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, null, tint = ink.secondary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        HomeDivider()
        Spacer(Modifier.height(20.dp))
        Row {
            if (meeting.distanceKm > 0) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.crewhome_meeting_distance), color = ink.secondary, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.crewhome_week_km, km(meeting.distanceKm)), color = ink.text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            // 모임 글의 정원 — 크루 정원과 다르다
            if (meeting.capacity > 0) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.crewhome_meeting_capacity), color = ink.secondary, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.crewhome_meeting_capacity_value, meeting.capacity), color = ink.text, fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).homeClickable(onClick = onAttendees).padding(vertical = 8.dp).testTag("home-meeting-attendees"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FaceStack(meeting.faces, meeting.hostId, meeting.hostOwner)
            Spacer(Modifier.width(if (meeting.faces.isEmpty()) 0.dp else 8.dp))
            Text(
                stringResource(R.string.crewhome_meeting_attendees, meeting.attendees), color = ink.text, fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).testTag("home-meeting-count"),
            )
            Icon(Icons.Filled.ChevronRight, null, tint = ink.secondary, modifier = Modifier.size(20.dp))
        }
        if (meeting.body.isNotBlank()) {
            Spacer(Modifier.height(18.dp))
            Text(meeting.body, color = ink.secondary, fontSize = 14.sp, lineHeight = 24.sp, modifier = Modifier.testTag("home-meeting-body"))
        }
        val note = when {
            !meeting.open -> stringResource(R.string.crewhome_meeting_closed)
            meeting.full && meeting.myResponse != MeetingResponse.YES -> stringResource(R.string.crewhome_meeting_full)
            else -> null
        }
        if (note != null) {
            Spacer(Modifier.height(18.dp))
            Text(note, color = ink.warn, fontSize = 13.5.sp, modifier = Modifier.testTag("home-meeting-note"))
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** 09 첫 응답은 두 버튼, 응답한 뒤에는 "참석 여부 변경" 하나. 지난 모임은 버튼을 두지 않는다 */
@Composable
private fun MeetingFooter(
    meeting: CrewMeeting,
    saving: MeetingResponse?,
    error: HomeProblem?,
    onRespond: (Boolean) -> Unit,
    onChange: () -> Unit,
) {
    if (!meeting.open) return
    val ink = crewInk()
    CrewBottomBar {
        if (meeting.myResponse == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val yesBlocked = meeting.full
                CrewButton(
                    stringResource(if (saving == MeetingResponse.YES) R.string.crewhome_meeting_saving else R.string.crewhome_meeting_yes),
                    { onRespond(true) }, Modifier.weight(1f).testTag("home-meeting-yes-button"),
                    kind = if (yesBlocked) CrewButtonKind.DISABLED else CrewButtonKind.PRIMARY,
                    enabled = saving == null && !yesBlocked,
                )
                CrewButton(
                    stringResource(if (saving == MeetingResponse.NO) R.string.crewhome_meeting_saving else R.string.crewhome_meeting_no),
                    { onRespond(false) }, Modifier.weight(1f).testTag("home-meeting-no-button"),
                    kind = CrewButtonKind.SECONDARY, enabled = saving == null,
                )
            }
            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    meetingErrorText(error), color = ink.warn, fontSize = 13.sp, lineHeight = 20.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().testTag("home-meeting-error-inline"),
                )
            }
        } else {
            CrewButton(stringResource(R.string.crewhome_meeting_change), onChange, Modifier.testTag("home-meeting-change"))
        }
    }
}

@Composable
private fun meetingErrorText(problem: HomeProblem): String = stringResource(
    when (problem) {
        HomeProblem.FULL -> R.string.crewhome_meeting_error_full
        HomeProblem.CLOSED -> R.string.crewhome_meeting_error_closed
        else -> R.string.crewhome_meeting_error
    },
)

/** 10 참석 여부 변경 · 25 저장 실패 — 닫기 · 뒤로는 저장하지 않는다. 저장 중에는 닫히지 않는다 */
@Composable
private fun AttendanceSheet(
    meeting: CrewMeeting,
    saving: MeetingResponse?,
    error: HomeProblem?,
    onRespond: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val ink = crewInk()
    BackHandler(enabled = saving != null) {}
    CrewSheet(
        stringResource(R.string.crewhome_meeting_change), onDismiss,
        Modifier.testTag(if (error != null) "home-attendance-error" else "home-attendance"), dismissible = saving == null,
    ) {
        Spacer(Modifier.height(14.dp))
        val at = meeting.meetAt
        Text(
            listOfNotNull(
                at?.let { stringResource(R.string.crewhome_meeting_when, meetingDayLabel(it), homeClock(it)) },
                meeting.place.takeIf { it.isNotBlank() },
            ).joinToString(" · "),
            color = ink.secondary, fontSize = 14.sp,
        )
        Spacer(Modifier.height(30.dp))
        CrewButton(
            stringResource(if (saving == MeetingResponse.YES) R.string.crewhome_meeting_saving else R.string.crewhome_meeting_yes),
            { onRespond(true) }, Modifier.testTag("home-attendance-yes"),
            kind = CrewButtonKind.SECONDARY,
            enabled = saving == null && (!meeting.full || meeting.myResponse == MeetingResponse.YES),
        )
        Spacer(Modifier.height(14.dp))
        CrewButton(
            stringResource(if (saving == MeetingResponse.NO) R.string.crewhome_meeting_saving else R.string.crewhome_meeting_no),
            { onRespond(false) }, Modifier.testTag("home-attendance-no"),
            kind = CrewButtonKind.SECONDARY, enabled = saving == null,
        )
        Spacer(Modifier.height(28.dp))
        if (error != null) {
            Text(meetingErrorText(error), color = ink.warn, fontSize = 13.5.sp, lineHeight = 22.sp, modifier = Modifier.testTag("home-attendance-error-text"))
        } else {
            Text(stringResource(R.string.crewhome_meeting_sheet_note), color = ink.secondary, fontSize = 13.5.sp)
        }
        Spacer(Modifier.height(36.dp))
    }
}

// ─────────────────────────────────────────────────────────────
// 13 참석자
// ─────────────────────────────────────────────────────────────

/** 13 — 서버가 센 참석자(진행자 먼저). 크루 인원과 다르다 */
@Composable
fun CrewAttendeesScreen(viewModel: CrewAttendeesViewModel, onBack: () -> Unit, onOpen: (MeetingAttendee) -> Unit, onEnded: () -> Unit) {
    val ink = crewInk()
    val attendees by viewModel.attendees.collectAsStateWithLifecycle()
    val meeting by viewModel.meeting.collectAsStateWithLifecycle()
    val ended by viewModel.ended.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.load()
        onPauseOrDispose {}
    }
    LaunchedEffect(ended) { if (ended) onEnded() }
    CrewPage(Modifier.testTag("home-attendees")) {
        CrewTopBar(stringResource(R.string.crewhome_attendees_bar), onBack)
        when (val state = attendees) {
            is HomeLoad.Ready -> LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = CrewGutter, end = CrewGutter, bottom = 40.dp),
            ) {
                item(key = "head") {
                    Column {
                        Spacer(Modifier.height(20.dp))
                        Text(
                            stringResource(R.string.crewhome_attendees_title, state.value.size), color = ink.text, fontSize = 27.sp,
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("home-attendees-title"),
                        )
                        meeting?.let { m ->
                            Spacer(Modifier.height(10.dp))
                            Text(meetingWhenLong(m), color = ink.secondary, fontSize = 13.5.sp)
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }
                if (state.value.isEmpty()) {
                    item(key = "empty") {
                        Text(stringResource(R.string.crewhome_attendees_empty), color = ink.secondary, fontSize = 14.sp, modifier = Modifier.padding(top = 24.dp))
                    }
                }
                items(state.value, key = { it.userId }) { person ->
                    val sub = when {
                        person.host && person.owner -> stringResource(R.string.crewhome_attendees_host_owner)
                        person.host -> stringResource(R.string.crewhome_attendees_host)
                        else -> stringResource(R.string.crewhome_attendees_going)
                    }
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 76.dp).homeClickable { onOpen(person) }.testTag("home-attendee-${person.userId}"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ChatFace(person.name, person.owner, 44.dp)
                        Column(Modifier.weight(1f).padding(start = 18.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f).padding(vertical = 14.dp)) {
                                    Text(person.name, color = ink.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.height(6.dp))
                                    Text(sub, color = if (person.host) ink.info else ink.secondary, fontSize = 13.sp)
                                }
                                Icon(Icons.Filled.ChevronRight, null, tint = ink.secondary, modifier = Modifier.size(20.dp))
                            }
                            HomeDivider()
                        }
                    }
                }
            }
            HomeLoad.Loading -> Column(Modifier.padding(CrewGutter)) {
                repeat(4) {
                    CrewSkeletonBox(Modifier.fillMaxWidth().height(56.dp))
                    Spacer(Modifier.height(14.dp))
                }
            }
            // 모임이 취소(삭제)됐다 — 다시 불러와도 같으니 취소 안내
            HomeLoad.Missing -> CrewEmptyState(
                icon = { Icon(Icons.Outlined.Info, null, tint = ink.info, modifier = Modifier.size(44.dp)) },
                title = stringResource(R.string.crewhome_meeting_cancelled_title),
                body = stringResource(R.string.crewhome_meeting_cancelled_body),
                modifier = Modifier.padding(top = 150.dp).testTag("home-attendees-cancelled"),
            ) { CrewButton(stringResource(R.string.crewhome_place_back), onBack, Modifier.testTag("home-attendees-back")) }
            else -> ChatRetryState(
                title = stringResource(R.string.crewhome_attendees_error),
                body = stringResource(R.string.crewhome_error_body),
                button = stringResource(R.string.crewhome_error_retry),
                onRetry = viewModel::load,
                modifier = Modifier.padding(top = 150.dp),
                tag = "home-attendees-error",
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 14 모이는 장소
// ─────────────────────────────────────────────────────────────

/** 14 — 실제 지도와 집결 좌표. 지도 앱은 그 장소를 연다(현재 위치 권한을 묻지 않는다) */
@Composable
fun CrewPlaceScreen(viewModel: CrewMeetingViewModel, onBack: () -> Unit, onEnded: () -> Unit) {
    val ink = crewInk()
    val context = LocalContext.current
    val meeting by viewModel.meeting.collectAsStateWithLifecycle()
    val ended by viewModel.ended.collectAsStateWithLifecycle()
    var toast by remember { mutableStateOf<String?>(null) }
    LifecycleResumeEffect(viewModel) {
        viewModel.load()
        onPauseOrDispose {}
    }
    LaunchedEffect(ended) { if (ended) onEnded() }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2400)
            toast = null
        }
    }
    // 공지에서 바로 온 모임처럼 홈이 들고 있지 않은 모임은 읽기 결과를 그대로 — 취소됐으면 취소, 못 읽었으면 다시 불러오기
    when (meeting) {
        HomeLoad.Missing -> {
            CrewPage(Modifier.testTag("home-place-cancelled")) {
                CrewTopBar(stringResource(R.string.crewhome_place_bar), onBack)
                CrewEmptyState(
                    icon = { Icon(Icons.Outlined.Info, null, tint = ink.info, modifier = Modifier.size(44.dp)) },
                    title = stringResource(R.string.crewhome_meeting_cancelled_title),
                    body = stringResource(R.string.crewhome_meeting_cancelled_body),
                    modifier = Modifier.padding(top = 150.dp),
                ) { CrewButton(stringResource(R.string.crewhome_place_back), onBack, Modifier.testTag("home-place-back")) }
            }
            return
        }
        HomeLoad.Failed -> {
            CrewPage(Modifier.testTag("home-place-error")) {
                CrewTopBar(stringResource(R.string.crewhome_place_bar), onBack)
                ChatRetryState(
                    title = stringResource(R.string.crewhome_meeting_load_error),
                    body = stringResource(R.string.crewhome_error_body),
                    button = stringResource(R.string.crewhome_error_retry),
                    onRetry = viewModel::load,
                    modifier = Modifier.padding(top = 150.dp),
                    tag = "home-place-load",
                )
            }
            return
        }
        else -> Unit
    }
    val noApp = stringResource(R.string.crewhome_place_no_app)
    Box {
        CrewPage(Modifier.testTag("home-place")) {
            CrewTopBar(stringResource(R.string.crewhome_place_bar), onBack)
            val current = (meeting as? HomeLoad.Ready)?.value
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = CrewGutter)) {
                Spacer(Modifier.height(20.dp))
                if (current != null) {
                    Text(
                        current.place.ifBlank { current.title }, color = ink.text, fontSize = 28.sp, lineHeight = 35.sp,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("home-place-name"),
                    )
                    if (current.meetAt != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(meetingWhenLong(current), color = ink.secondary, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(24.dp))
                    val point = current.point
                    if (point != null) {
                        MeetingMap(point, current.place, Modifier.fillMaxWidth().height(360.dp), spanMeters = 380)
                    } else {
                        Text(
                            stringResource(R.string.crewhome_place_no_point), color = ink.secondary, fontSize = 14.sp, lineHeight = 22.sp,
                            modifier = Modifier.testTag("home-place-no-point"),
                        )
                    }
                } else {
                    CrewSkeletonBox(Modifier.fillMaxWidth().height(360.dp), 18.dp)
                }
                Spacer(Modifier.height(24.dp))
            }
            CrewBottomBar {
                CrewButton(
                    stringResource(R.string.crewhome_place_open),
                    { current?.let { if (!openMaps(context, it)) toast = noApp } },
                    Modifier.testTag("home-place-open"), kind = CrewButtonKind.SECONDARY, enabled = current != null,
                )
                Spacer(Modifier.height(12.dp))
                CrewButton(stringResource(R.string.crewhome_place_back), onBack, Modifier.testTag("home-place-back"))
            }
        }
        SettingsToast(toast, Modifier.align(Alignment.BottomCenter).padding(start = CrewGutter, end = CrewGutter, bottom = 150.dp))
    }
}

/** 지도 앱으로 — 좌표가 있으면 그 자리에 이름을 붙여, 없으면 장소 이름으로 찾는다 */
internal fun openMaps(context: Context, meeting: CrewMeeting): Boolean {
    val label = meeting.place.ifBlank { meeting.title }
    val uri = meeting.point?.let { p -> Uri.parse("geo:${p.lat},${p.lng}?q=${p.lat},${p.lng}(${Uri.encode(label)})") }
        ?: Uri.parse("geo:0,0?q=${Uri.encode(label)}")
    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

/** 크루 이름(모임 화면 위 작은 글자) — 명함에서 */
internal fun crewNameOf(crewId: String): String = ServiceLocator.crewCards.cardNow(crewId)?.name.orEmpty()
