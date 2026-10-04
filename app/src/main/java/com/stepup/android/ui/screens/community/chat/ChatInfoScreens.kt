package com.stepup.android.ui.screens.community.chat

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.domain.ChatRoomMeta
import com.stepup.android.domain.CrewMember
import com.stepup.android.domain.CrewPerson
import com.stepup.android.domain.CrewPersonRole
import com.stepup.android.domain.CrewRules
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.community.crew.CrewAvatar
import com.stepup.android.ui.screens.community.crew.CrewImage

/** 채팅방 정보에서 나가는 곳 */
class ChatInfoActions(
    val onBack: () -> Unit,
    val onNotices: () -> Unit,
    val onMembers: () -> Unit,
    val onSearch: () -> Unit,
    val onCrew: () -> Unit,
    val onWriteNotice: () -> Unit,
    val onEnded: () -> Unit,
    /** 크루장의 크루원 보기 · 멤버 관리(기존 55) — 없으면 [onMembers] */
    val onManageMembers: (() -> Unit)? = null,
)

/**
 * 04 크루원 · 05 크루장 채팅방 정보 — 공지 · 크루원 · 내 채팅 알림 · 대화 검색 · 크루 상세. 크루원은 아래에 "크루 나가기"
 * (기존 61 확인), 크루장은 "크루장 전용"의 공지 등록 · 멤버 관리. 역할은 서버의 지금 값으로 다시 읽는다.
 */
@Composable
fun ChatInfoScreen(viewModel: ChatInfoViewModel, actions: ChatInfoActions) {
    val ink = blueInk()
    val context = LocalContext.current
    val meta by viewModel.meta.collectAsStateWithLifecycle()
    val notifyChoice by viewModel.notifyChoice.collectAsStateWithLifecycle()
    val notifyState by viewModel.notifyState.collectAsStateWithLifecycle()
    val leaveState by viewModel.leaveState.collectAsStateWithLifecycle()
    val ended by viewModel.ended.collectAsStateWithLifecycle()
    var osSheet by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }
    LaunchedEffect(ended) { if (ended) actions.onEnded() }

    val room = (meta as? ChatLoad.Ready)?.value
    BluePage(Modifier.testTag(if (room?.owner == true) "chat-info-owner" else "chat-info")) {
        BlueTopBar(stringResource(R.string.chat_info_bar), actions.onBack)
        when {
            room != null -> ChatInfoContent(room, actions, onNotify = viewModel::openNotify, onLeave = viewModel::askLeave)
            meta is ChatLoad.Failed -> ChatRetryState(
                title = stringResource(R.string.chat_error_title),
                body = stringResource(R.string.chat_error_body),
                button = stringResource(R.string.chat_error_retry),
                onRetry = viewModel::refresh,
                modifier = Modifier.padding(top = 120.dp),
                tag = "chat-info-error",
            )
            else -> Column(Modifier.padding(BlueGutter)) {
                BlueSkeleton(Modifier.size(78.dp), 17.dp)
                Spacer(Modifier.height(28.dp))
                BlueSkeleton(Modifier.fillMaxWidth().height(220.dp), 16.dp)
            }
        }
    }

    val choice = notifyChoice
    if (choice != null) {
        BlueSheet(stringResource(R.string.chat_notify_title), viewModel::closeNotify, Modifier.testTag("chat-notify"), dismissible = !notifyState.busy) {
            Spacer(Modifier.height(16.dp))
            val selectedLabel = stringResource(R.string.chat_selected)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BlueChoiceRow(
                    stringResource(R.string.chat_notify_on_row), selected = choice, onClick = { viewModel.notifyChoice.value = true },
                    tag = "chat-notify-on", enabled = !notifyState.busy, selectedLabel = selectedLabel,
                )
                BlueChoiceRow(
                    stringResource(R.string.chat_notify_off_row), selected = !choice, onClick = { viewModel.notifyChoice.value = false },
                    tag = "chat-notify-off", enabled = !notifyState.busy, selectedLabel = selectedLabel,
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.chat_notify_note), style = blueText(15.sp, ink.secondary, FontWeight.Medium, 1.5f))
            if (notifyState.error != null) {
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.chat_notify_error), style = blueText(14.sp, ink.warn, FontWeight.SemiBold), modifier = Modifier.testTag("chat-notify-error"))
            }
            Spacer(Modifier.height(22.dp))
            BlueButton(
                stringResource(R.string.chat_notify_apply),
                {
                    viewModel.applyNotify { on ->
                        // 방 알림을 켰는데 기기 알림이 꺼져 있으면 37 — 이 화면이 기기 권한을 바꾸지는 않는다
                        val device = ChatDeviceNotificationsForTest.enabled ?: NotificationManagerCompat.from(context).areNotificationsEnabled()
                        if (on && !device) osSheet = true
                    }
                },
                Modifier.testTag("chat-notify-apply"),
                busy = notifyState.busy,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
    if (osSheet) {
        BlueSheet(stringResource(R.string.chat_os_title), { osSheet = false }, Modifier.testTag("chat-os"), centered = true) {
            Spacer(Modifier.height(12.dp))
            BlueSheetBody(stringResource(R.string.chat_os_body))
            Spacer(Modifier.height(26.dp))
            BlueButton(
                stringResource(R.string.chat_os_settings),
                {
                    osSheet = false
                    openNotificationSettings(context)
                },
                Modifier.testTag("chat-os-settings"),
            )
            Spacer(Modifier.height(12.dp))
            BlueButton(stringResource(R.string.chat_os_later), { osSheet = false }, Modifier.testTag("chat-os-later"), BlueKind.SECONDARY)
            Spacer(Modifier.height(8.dp))
        }
    }
    val leave = leaveState
    if (leave != null && room != null) {
        BlueConfirmSheet(
            title = stringResource(R.string.crew_leave_title, room.name),
            body = stringResource(R.string.crew_leave_body),
            confirm = stringResource(R.string.crew_menu_leave),
            onConfirm = viewModel::confirmLeave,
            onDismiss = viewModel::closeLeave,
            busy = leave.busy,
            danger = true,
            tag = "chat-leave",
            error = leave.error?.let { chatConfirmErrorText(it) },
        )
    }
}

/** 기기 검사 — 기기 알림 권한(검사가 바꿀 수 없다) 대신 쓸 값. 앱에서는 늘 null */
object ChatDeviceNotificationsForTest {
    @Volatile var enabled: Boolean? = null
}

/** 기기의 StepUp 알림 설정 — 없으면 앱 정보 */
private fun openNotificationSettings(context: android.content.Context) {
    val notify = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(notify) }.onFailure {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

@Composable
private fun ChatInfoContent(room: ChatRoomMeta, actions: ChatInfoActions, onNotify: () -> Unit, onLeave: () -> Unit) {
    val ink = blueInk()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = BlueGutter)) {
        Spacer(Modifier.height(12.dp))
        BlueSurface(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                CrewImage(room.card, 84.dp, 18.dp)
                Column(Modifier.weight(1f).padding(start = 18.dp)) {
                    Text(room.name, style = blueText(26.sp, ink.text, FontWeight.ExtraBold, 1.25f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.chat_info_sub, room.memberCount, stringResource(if (room.owner) R.string.chat_role_owner else R.string.chat_role_member)),
                        style = blueText(15.sp, ink.secondary, FontWeight.Bold), modifier = Modifier.testTag("chat-info-sub"),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        BlueRow(stringResource(R.string.chat_info_notices), actions.onNotices, Modifier.testTag("chat-info-notices"), value = stringResource(R.string.chat_info_notice_count, room.noticeCount))
        val manage = actions.onManageMembers ?: actions.onMembers
        BlueRow(
            stringResource(R.string.chat_info_members), if (room.owner) manage else actions.onMembers, Modifier.testTag("chat-info-members"),
            value = stringResource(R.string.chat_info_member_count, room.memberCount),
        )
        BlueRow(
            stringResource(R.string.chat_info_notify), onNotify, Modifier.testTag("chat-info-notify"),
            value = stringResource(if (room.notify) R.string.chat_notify_on else R.string.chat_notify_off),
        )
        BlueRow(stringResource(R.string.chat_info_search), actions.onSearch, Modifier.testTag("chat-info-search"))
        BlueRow(stringResource(R.string.chat_info_crew), actions.onCrew, Modifier.testTag("chat-info-crew"))
        if (room.owner) {
            Spacer(Modifier.height(40.dp))
            Text(stringResource(R.string.chat_info_owner_section), style = blueText(14.sp, ink.info, FontWeight.Bold))
            Spacer(Modifier.height(4.dp))
            BlueRow(stringResource(R.string.chat_info_write_notice), actions.onWriteNotice, Modifier.testTag("chat-info-write"))
            BlueRow(stringResource(R.string.chat_info_manage_members), manage, Modifier.testTag("chat-info-manage"))
        } else {
            Spacer(Modifier.height(48.dp))
            BlueRow(
                stringResource(R.string.crew_menu_leave), onLeave, Modifier.testTag("chat-info-leave"),
                titleColor = ink.warn, sub = stringResource(R.string.chat_info_leave_sub),
            )
        }
        Spacer(Modifier.height(40.dp))
    }
}

// ─────────────────────────────────────────────────────────────
// 06 크루원
// ─────────────────────────────────────────────────────────────

/** 06 크루원 — 크루장 먼저, 먼저 들어온 순. 나를 누르면 내 정보, 다른 사람은 07(크루장이면 33) */
@Composable
fun ChatMembersScreen(viewModel: ChatPeopleViewModel, onBack: () -> Unit, onOpen: (String) -> Unit, onMe: () -> Unit, onEnded: () -> Unit) {
    val ink = blueInk()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val meta by viewModel.meta.collectAsStateWithLifecycle()
    val me by viewModel.me.collectAsStateWithLifecycle()
    val ended by viewModel.ended.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.loadMembers()
        onPauseOrDispose {}
    }
    LaunchedEffect(ended) { if (ended) onEnded() }
    BluePage(Modifier.testTag("chat-members")) {
        BlueTopBar(stringResource(R.string.chat_members_bar), onBack)
        when (val state = members) {
            is ChatLoad.Ready -> LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = BlueGutter, end = BlueGutter, bottom = 40.dp)) {
                item(key = "heading") {
                    Column(Modifier.padding(top = 18.dp, bottom = 18.dp)) {
                        Text(
                            stringResource(R.string.chat_members_heading, state.value.size), style = blueText(30.sp, ink.text, FontWeight.ExtraBold, 1.25f),
                            modifier = Modifier.testTag("chat-members-heading"),
                        )
                        meta?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.chat_members_sub, it.name), style = blueText(16.sp, ink.secondary, FontWeight.Medium))
                        }
                    }
                }
                items(state.value, key = { it.userId }) { member ->
                    ChatMemberRow(member, mine = member.userId == me) { if (member.userId == me) onMe() else onOpen(member.userId) }
                }
            }
            ChatLoad.Failed -> ChatRetryState(
                title = stringResource(R.string.chat_members_error),
                body = stringResource(R.string.chat_error_body),
                button = stringResource(R.string.chat_error_retry),
                onRetry = viewModel::loadMembers,
                modifier = Modifier.padding(top = 120.dp),
                tag = "chat-members-error",
            )
            else -> Column(Modifier.padding(BlueGutter)) {
                repeat(4) {
                    BlueSkeleton(Modifier.fillMaxWidth().height(56.dp), 12.dp)
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ChatMemberRow(member: CrewMember, mine: Boolean, onClick: () -> Unit) {
    val ink = blueInk()
    val labels = buildList {
        if (member.owner) add(stringResource(R.string.chat_member_owner))
        if (mine) add(stringResource(R.string.chat_member_me))
    }
    BluePersonRow(
        name = member.name,
        sub = labels.joinToString(" · ").ifEmpty { stringResource(R.string.chat_member_member) },
        onClick = onClick,
        modifier = Modifier.testTag("chat-member-${member.userId}"),
        subColor = if (labels.isEmpty()) ink.secondary else ink.info,
        owner = member.owner,
    )
}

// ─────────────────────────────────────────────────────────────
// 07 · 33 크루원 프로필 · 34 내보내기
// ─────────────────────────────────────────────────────────────

/**
 * 07 크루원 프로필(공개 정보만) — 크루장이 다른 크루원을 보면 33(아래 "크루에서 내보내기"). 나와 지금의 크루장에게는
 * 내보내기를 두지 않는다. 크루장 여부는 서버의 지금 값으로 다시 읽는다(주소만 알아서는 내보낼 수 없다 — 서버가 다시 본다).
 */
@Composable
fun ChatMemberScreen(
    viewModel: ChatPeopleViewModel,
    onBack: () -> Unit,
    onPublicProfile: (String) -> Unit,
    onRemoved: () -> Unit,
    onEnded: () -> Unit,
) {
    val ink = blueInk()
    val person by viewModel.person.collectAsStateWithLifecycle()
    val meta by viewModel.meta.collectAsStateWithLifecycle()
    val me by viewModel.me.collectAsStateWithLifecycle()
    val removeState by viewModel.removeState.collectAsStateWithLifecycle()
    val removed by viewModel.removed.collectAsStateWithLifecycle()
    val ended by viewModel.ended.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.loadPerson()
        onPauseOrDispose {}
    }
    LaunchedEffect(ended) { if (ended) onEnded() }
    LaunchedEffect(removed) { if (removed) onRemoved() }

    val target = (person as? ChatLoad.Ready)?.value
    val canRemove = target != null && meta?.owner == true && target.userId != me && target.role == CrewPersonRole.MEMBER
    BluePage(Modifier.testTag(if (canRemove) "chat-member-owner" else "chat-member")) {
        BlueTopBar(stringResource(R.string.chat_profile_bar), onBack)
        when {
            target != null -> ChatMemberContent(
                target, meta?.name.orEmpty(), Modifier.weight(1f), onPublicProfile,
                onRemove = if (canRemove) viewModel::askRemove else null,
            )
            person is ChatLoad.Failed -> ChatRetryState(
                title = stringResource(R.string.chat_error_title),
                body = stringResource(R.string.chat_error_body),
                button = stringResource(R.string.chat_error_retry),
                onRetry = viewModel::loadPerson,
                modifier = Modifier.padding(top = 120.dp),
                tag = "chat-member-error",
            )
            else -> Column(Modifier.fillMaxWidth().padding(top = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                BlueSkeleton(Modifier.size(76.dp), 38.dp)
                Spacer(Modifier.height(24.dp))
                BlueSkeleton(Modifier.size(width = 120.dp, height = 30.dp))
            }
        }
    }
    val remove = removeState
    if (remove != null && target != null) {
        BlueConfirmSheet(
            title = stringResource(R.string.chat_remove_title, target.name),
            body = stringResource(R.string.chat_remove_body, meta?.name.orEmpty()),
            confirm = stringResource(R.string.chat_remove_confirm),
            onConfirm = viewModel::confirmRemove,
            onDismiss = viewModel::closeRemove,
            busy = remove.busy,
            danger = true,
            tag = "chat-remove",
            error = remove.error?.let { chatConfirmErrorText(it) },
        )
    }
}

@Composable
private fun ChatMemberContent(
    person: CrewPerson,
    crewName: String,
    modifier: Modifier,
    onPublicProfile: (String) -> Unit,
    /** 33 — 크루장이 일반 크루원을 볼 때만(나 · 지금의 크루장에게는 없다) */
    onRemove: (() -> Unit)?,
) {
    val ink = blueInk()
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = BlueGutter)) {
        Spacer(Modifier.height(26.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            ChatFace(person.name, person.role == CrewPersonRole.OWNER, 84.dp)
            Spacer(Modifier.height(18.dp))
            Text(person.name, style = blueText(30.sp, ink.text, FontWeight.ExtraBold, 1.25f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            val role = when (person.role) {
                CrewPersonRole.OWNER -> stringResource(R.string.chat_role_owner)
                CrewPersonRole.MEMBER -> stringResource(R.string.chat_role_member)
                else -> null
            }
            Text(
                if (role != null) stringResource(R.string.chat_profile_role, crewName, role) else stringResource(R.string.chat_profile_gone),
                style = blueText(16.sp, if (role != null) ink.info else ink.secondary, FontWeight.Bold), textAlign = TextAlign.Center,
                modifier = Modifier.testTag("chat-member-role"),
            )
        }
        if (person.role == CrewPersonRole.OWNER || person.role == CrewPersonRole.MEMBER) {
            Spacer(Modifier.height(30.dp))
            BlueSurface(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                    person.joinedAt?.let { joined ->
                        Text(
                            stringResource(R.string.chat_profile_since, chatProfileDate(joined)),
                            style = blueText(20.sp, ink.text, FontWeight.ExtraBold, 1.35f),
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        if (person.weekKm > 0.0) stringResource(R.string.chat_profile_week, CrewRules.km(person.weekKm)) else stringResource(R.string.chat_profile_week_none),
                        style = blueText(15.5.sp, ink.secondary, FontWeight.Medium, 1.45f),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            BlueRow(stringResource(R.string.chat_profile_public), { onPublicProfile(person.userId) }, Modifier.testTag("chat-member-public"))
            if (onRemove != null) {
                BlueRow(stringResource(R.string.chat_profile_remove), onRemove, Modifier.testTag("chat-member-remove"), titleColor = ink.warn)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun chatProfileDate(millis: Long): String {
    val pattern = stringResource(R.string.chat_profile_since_pattern)
    return runCatching {
        java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern(pattern, java.util.Locale.getDefault()))
    }.getOrDefault("")
}
