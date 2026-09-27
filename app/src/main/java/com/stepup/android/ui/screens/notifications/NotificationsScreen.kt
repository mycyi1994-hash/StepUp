package com.stepup.android.ui.screens.notifications

import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.local.NotificationEntity
import com.stepup.android.data.local.NotificationType
import com.stepup.android.data.repo.Announcement
import com.stepup.android.data.repo.CommentTarget
import com.stepup.android.data.repo.NoticeBoard
import com.stepup.android.domain.parseSlotKey
import com.stepup.android.ui.components.SecondaryHeader
import com.stepup.android.ui.components.SettingsPrimaryButton
import com.stepup.android.ui.components.SettingsToast
import com.stepup.android.ui.components.StepUpIcons
import com.stepup.android.ui.components.settingsPalette
import com.stepup.android.ui.components.variantLabel
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.StepUpDesign
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 알림 화면의 두 메뉴 */
enum class NotifTab { Inbox, Notices }

/** 알림 화면이 보일 것 — 기기 테스트는 이것을 바로 넣어 장면을 찍는다 */
data class NotificationsUi(
    val tab: NotifTab = NotifTab.Inbox,
    val inbox: InboxLoad = InboxLoad.Loading,
    /** 저장소 전체의 새 알림 수 */
    val unread: Int = 0,
    val readingAll: Boolean = false,
    val notices: NoticeBoard = NoticeBoard(),
    val now: Long = System.currentTimeMillis(),
    val toast: InboxToast? = null,
)

/**
 * 알림(알림·공지 v1, 2026-09-28 전달본 — docs/redesign/notifications-v1).
 *
 * "내 알림"은 오늘 · 어제 · 이전으로 묶은 단순 목록(작은 아이콘 · 제목 · 시간 · 읽지 않은 점), "공지"는 제목과
 * 게시일의 짧은 목록이다. 들어온 것만으로 읽음 처리하지 않는다 — 알림을 열면 그 알림만, "모두 읽음"은 누를 때만.
 * 알림을 누르면 종류에 맞는 내용 시트(적립 · 신발 · 초대 · 이전 보상)나 원래 자리(댓글 · 크루 게시판)로 간다.
 * 메뉴와 두 목록의 스크롤 위치는 공지 상세에 다녀와도 그대로다.
 */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenLobby: (String) -> Unit = {},
    /** 알림이 가리키는 댓글로 이동 */
    onOpenComment: (CommentTarget) -> Unit = {},
    /** 알림이 가리키는 크루 게시판으로 이동 */
    onOpenCrew: (String) -> Unit = {},
    onOpenChallenges: () -> Unit = {},
    onOpenWallet: () -> Unit = {},
    onOpenShoes: () -> Unit = {},
    onOpenCommunity: () -> Unit = {},
    onOpenNotice: (Long) -> Unit = {},
    /** 로그인 화면으로 — 기록 · 장비 · 알림은 지우지 않는다 */
    onSignIn: () -> Unit = {},
    viewModel: NotificationsViewModel = viewModel(factory = NotificationsViewModel.Factory),
) {
    val inbox by viewModel.inbox.collectAsStateWithLifecycle()
    val unread by viewModel.unread.collectAsStateWithLifecycle()
    val readingAll by viewModel.readingAll.collectAsStateWithLifecycle()
    val notices by viewModel.notices.collectAsStateWithLifecycle()
    val steps by viewModel.steps.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val reopen by viewModel.reopenInvite.collectAsStateWithLifecycle()
    val now = rememberNow()
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableStateOf(NotifTab.Inbox) }
    var openId by rememberSaveable { mutableStateOf<Long?>(null) }
    var missingOpen by rememberSaveable { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    // 이 화면이 열린 때 — 로그인하고 돌아와 새로 열린 화면만 멈춰 둔 초대를 다시 연다
    val openedAt = rememberSaveable { SystemClock.elapsedRealtime() }
    val inboxState = rememberLazyListState()
    val noticeState = rememberLazyListState()

    LaunchedEffect(tab) { if (tab == NotifTab.Notices) viewModel.refreshNoticesIfStale() }
    LaunchedEffect(toast) {
        val shown = toast ?: return@LaunchedEffect
        delay(2_000)
        viewModel.toastShown(shown.seq)
    }
    LaunchedEffect(reopen, inbox) {
        val request = reopen ?: return@LaunchedEffect
        val ready = inbox as? InboxLoad.Ready ?: return@LaunchedEffect
        if (request.requestedAt > openedAt) return@LaunchedEffect
        ready.items.firstOrNull { it.id == request.notificationId }?.let {
            tab = NotifTab.Inbox
            viewModel.prepareInvite(it)
            openId = it.id
        }
        viewModel.clearReopen()
    }

    fun open(entity: NotificationEntity) {
        when (entity.type) {
            NotificationType.COMMENT_REPLY -> {
                val target = CommentTarget.decode(entity.argExtra)
                if (target == null) {
                    openId = entity.id
                    return
                }
                if (checking) return
                checking = true
                scope.launch {
                    val check = try {
                        viewModel.checkComment(target)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        CommentCheck.Offline
                    } finally {
                        checking = false
                    }
                    when (check) {
                        CommentCheck.Visible -> {
                            viewModel.markRead(entity)
                            onOpenComment(target)
                        }
                        CommentCheck.Missing -> {
                            viewModel.markRead(entity)
                            missingOpen = true
                        }
                        else -> viewModel.commentUnavailable(check)
                    }
                }
            }
            NotificationType.CREW_JOINED ->
                if (entity.argExtra.isNotBlank()) {
                    viewModel.markRead(entity)
                    onOpenCrew(entity.argExtra)
                } else {
                    openId = entity.id
                }
            else -> {
                viewModel.prepareInvite(entity)
                openId = entity.id
            }
        }
    }

    NotificationsContent(
        ui = NotificationsUi(tab, inbox, unread, readingAll, notices, now, toast),
        onBack = onBack,
        onOpenSettings = onOpenSettings,
        onSelectTab = { tab = it },
        onOpen = ::open,
        onMarkAllRead = viewModel::markAllRead,
        onReload = viewModel::reload,
        onOpenNotice = onOpenNotice,
        onRetryNotices = { viewModel.refreshNotices() },
        inboxState = inboxState,
        noticeState = noticeState,
    )

    val opened = openId?.let { id -> (inbox as? InboxLoad.Ready)?.items?.firstOrNull { it.id == id } }
    if (opened != null) {
        // 내용 시트가 뜨면 그 알림만 읽음으로(읽음은 삭제 · 응답 · 지급과 별개)
        LaunchedEffect(opened.id) { viewModel.markRead(opened) }
        fun close() {
            viewModel.closeInvite(opened.id)
            openId = null
        }
        NotificationSheet(
            entity = opened,
            step = steps[opened.id],
            actions = NotificationSheetActions(
                dismiss = ::close,
                openWallet = { close(); onOpenWallet() },
                openShoes = { close(); onOpenShoes() },
                openChallenges = { close(); onOpenChallenges() },
                viewCrew = { crewId -> close(); onOpenCrew(crewId) },
                acceptCrew = { viewModel.acceptCrewInvite(opened) },
                checkCrew = { viewModel.checkCrewStatus(opened) },
                viewLobby = { crewId -> close(); onOpenLobby(crewId) },
                acceptParty = { viewModel.acceptPartyInvite(opened) { crewId -> close(); onOpenLobby(crewId) } },
                askDecline = { viewModel.askDecline(opened) },
                cancelDecline = { viewModel.cancelDecline(opened) },
                decline = { viewModel.decline(opened) },
                signIn = {
                    viewModel.rememberForSignIn(opened)
                    openId = null
                    onSignIn()
                },
                later = { viewModel.cancelDecline(opened) },
            ),
        )
    } else if (openId != null && inbox is InboxLoad.Ready) {
        // 거절해서 지워졌거나 기록 지우기로 없어진 알림 — 시트를 닫는다
        LaunchedEffect(openId) { openId = null }
    }
    if (missingOpen) {
        MissingTargetSheet(
            onOpenCommunity = {
                missingOpen = false
                onOpenCommunity()
            },
            onBack = { missingOpen = false },
        )
    }
}

@Composable
fun NotificationsContent(
    ui: NotificationsUi,
    onBack: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onSelectTab: (NotifTab) -> Unit = {},
    onOpen: (NotificationEntity) -> Unit = {},
    onMarkAllRead: () -> Unit = {},
    onReload: () -> Unit = {},
    onOpenNotice: (Long) -> Unit = {},
    onRetryNotices: () -> Unit = {},
    inboxState: LazyListState = rememberLazyListState(),
    noticeState: LazyListState = rememberLazyListState(),
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val p = settingsPalette()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = StepUpDesign.Gutter)) {
            SecondaryHeader(
                onBack = onBack, balance = null, onOpenWallet = null, title = stringResource(R.string.notif_title),
                trailing = {
                    Box(
                        Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp)
                            .feedbackClickable(role = Role.Button, onClick = onOpenSettings)
                            .padding(horizontal = 6.dp).testTag("inbox-settings"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(R.string.inbox_settings), color = p.accent, fontSize = 16.sp,
                            fontWeight = FontWeight.Medium)
                    }
                },
            )
            InboxTabs(ui.tab, onSelectTab)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (ui.tab) {
                    NotifTab.Inbox -> InboxPane(ui, onOpen, onMarkAllRead, onReload, onOpenSettings, inboxState, zone)
                    NotifTab.Notices -> NoticePane(ui.notices, onOpenNotice, onRetryNotices, noticeState, zone)
                }
            }
        }
        val message = ui.toast?.let { stringResource(it.message) }
        SettingsToast(message, Modifier.align(Alignment.BottomCenter).padding(horizontal = 24.dp, vertical = 16.dp),
            success = ui.toast?.success != false)
    }
}

/** 내 알림 · 공지 — 고른 쪽은 밑줄과 굵은 글자 */
@Composable
private fun InboxTabs(selected: NotifTab, onSelect: (NotifTab) -> Unit) {
    val p = settingsPalette()
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                NotifTab.Inbox to R.string.inbox_tab_mine,
                NotifTab.Notices to R.string.inbox_tab_notices,
            ).forEach { (tab, label) ->
                val on = tab == selected
                Column(
                    Modifier.width(IntrinsicSize.Max).heightIn(min = 52.dp)
                        .feedbackClickable(role = null) { onSelect(tab) }
                        .semantics {
                            role = Role.Tab
                            this.selected = on
                        }
                        .padding(horizontal = 16.dp).testTag("inbox-tab-${tab.name.lowercase()}"),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Box(Modifier.padding(top = 12.dp, bottom = 10.dp)) {
                        // 굵은 글자의 폭을 미리 잡아 둔다 — 고를 때 메뉴 폭 · 밑줄이 흔들리지 않는다
                        Text(stringResource(label), color = Color.Transparent, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clearAndSetSemantics { })
                        Text(stringResource(label), color = if (on) p.text else p.secondary, fontSize = 17.sp,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                    }
                    Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp))
                        .background(if (on) p.accent else Color.Transparent))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
    }
}

// ── 내 알림 ─────────────────────────────────────────────────────────

@Composable
private fun InboxPane(
    ui: NotificationsUi,
    onOpen: (NotificationEntity) -> Unit,
    onMarkAllRead: () -> Unit,
    onReload: () -> Unit,
    onOpenSettings: () -> Unit,
    state: LazyListState,
    zone: ZoneId,
) {
    when (val load = ui.inbox) {
        InboxLoad.Loading -> ListSkeleton(icons = true, modifier = Modifier.testTag("inbox-loading"),
            label = stringResource(R.string.inbox_loading))
        InboxLoad.Failed -> PaneState(
            icon = Icons.Filled.Refresh, title = stringResource(R.string.inbox_load_failed),
            body = stringResource(R.string.inbox_load_failed_body), actionLabel = stringResource(R.string.set_reload),
            onAction = onReload, modifier = Modifier.testTag("inbox-failed"),
        )
        is InboxLoad.Ready -> if (load.items.isEmpty()) {
            PaneState(
                icon = Icons.Outlined.Notifications, title = stringResource(R.string.inbox_empty_title),
                body = stringResource(R.string.inbox_empty_body), actionLabel = stringResource(R.string.inbox_open_settings),
                onAction = onOpenSettings, modifier = Modifier.testTag("inbox-empty"),
            )
        } else {
            val groups = remember(load.items, ui.now, zone) {
                load.items.groupBy { dayGroup(it.timestamp, ui.now, zone) }.toSortedMap().toList()
            }
            LazyColumn(
                state = state,
                modifier = Modifier.fillMaxSize().testTag("inbox-list"),
                contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp),
            ) {
                groups.forEachIndexed { index, (group, rows) ->
                    item(key = "group-${group.name}") {
                        GroupHeader(group, first = index == 0, unread = ui.unread, readingAll = ui.readingAll,
                            onMarkAllRead = onMarkAllRead)
                    }
                    items(rows, key = { it.id }) { entity ->
                        NotificationRow(entity, ui.now, zone) { onOpen(entity) }
                    }
                }
            }
        }
    }
}

/** 날짜 묶음 이름. 첫 묶음 줄에 새 알림 수와 "모두 읽음"을 둔다 */
@Composable
private fun GroupHeader(group: DayGroup, first: Boolean, unread: Int, readingAll: Boolean, onMarkAllRead: () -> Unit) {
    val p = settingsPalette()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = if (first) 0.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(
                when (group) {
                    DayGroup.Today -> R.string.inbox_group_today
                    DayGroup.Yesterday -> R.string.inbox_group_yesterday
                    DayGroup.Earlier -> R.string.inbox_group_earlier
                },
            ),
            color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.semantics { heading() },
        )
        if (first) {
            Spacer(Modifier.width(14.dp))
            Text(
                if (unread > 0) stringResource(R.string.inbox_new_count, unread) else stringResource(R.string.inbox_all_read),
                color = p.secondary, fontSize = 13.sp, modifier = Modifier.weight(1f).testTag("inbox-summary"),
            )
            if (unread > 0) {
                Box(
                    Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp)
                        .feedbackClickable(enabled = !readingAll, role = Role.Button, onClick = onMarkAllRead)
                        .padding(start = 8.dp).testTag("inbox-read-all"),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Text(stringResource(R.string.inbox_mark_all_read), color = if (readingAll) p.secondary else p.accent,
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** 알림 한 줄 — 줄 전체가 누르는 곳. 읽지 않은 알림은 점과 굵은 제목, 그리고 "읽지 않음"을 읽어 준다 */
@Composable
private fun NotificationRow(entity: NotificationEntity, now: Long, zone: ZoneId, onClick: () -> Unit) {
    val p = settingsPalette()
    val unreadLabel = stringResource(R.string.inbox_unread)
    Column(
        Modifier.fillMaxWidth()
            .feedbackClickable(role = Role.Button, onClick = onClick)
            .semantics { if (!entity.read) stateDescription = unreadLabel }
            .testTag("notif-row-${entity.id}"),
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 96.dp).padding(vertical = 20.dp)) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(p.surface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(iconFor(entity.type), contentDescription = null, tint = p.accent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    messageFor(entity), color = p.text, fontSize = 16.5.sp, lineHeight = 1.42.em,
                    fontWeight = if (entity.read) FontWeight.Normal else FontWeight.SemiBold,
                )
                Text(timeLabel(entity.timestamp, now, zone), color = p.secondary, fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.width(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.padding(top = 4.dp).size(7.dp).clip(CircleShape)
                        .background(if (entity.read) Color.Transparent else unreadDot())
                        .then(if (entity.read) Modifier else Modifier.testTag("notif-unread-${entity.id}")),
                )
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = p.secondary,
                    modifier = Modifier.padding(top = 10.dp).size(20.dp))
            }
        }
        Box(Modifier.padding(start = 50.dp).fillMaxWidth().height(1.dp).background(p.divider))
    }
}

@Composable
private fun unreadDot(): Color = if (StepUpColors.dark) Color(0xFF85ABFF) else settingsPalette().accent

// ── 공지 ────────────────────────────────────────────────────────────

@Composable
private fun NoticePane(
    board: NoticeBoard,
    onOpenNotice: (Long) -> Unit,
    onRetry: () -> Unit,
    state: LazyListState,
    zone: ZoneId,
) {
    val items = board.items
    when {
        items == null && board.refreshFailed && !board.loading -> PaneState(
            icon = Icons.Filled.Refresh, title = stringResource(R.string.notice_load_failed),
            body = stringResource(R.string.inbox_load_failed_body), actionLabel = stringResource(R.string.set_reload),
            onAction = onRetry, modifier = Modifier.testTag("notice-failed"),
        )
        items == null -> ListSkeleton(icons = false, modifier = Modifier.testTag("notice-loading"),
            label = stringResource(R.string.notice_loading))
        else -> {
            val p = settingsPalette()
            val language = LocalConfiguration.current.locales[0].language
            val date = rememberPattern(R.string.date_notice)
            LazyColumn(
                state = state,
                modifier = Modifier.fillMaxSize().testTag("notice-list"),
                contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
            ) {
                item(key = "notice-header") {
                    Text(stringResource(R.string.notice_header), color = p.secondary, fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 6.dp))
                }
                if (board.refreshFailed) {
                    item(key = "notice-stale") { NoticeStale(onRetry, retrying = board.loading) }
                }
                if (items.isEmpty()) {
                    item(key = "notice-empty") {
                        PaneState(
                            icon = Icons.AutoMirrored.Outlined.Article, title = stringResource(R.string.notice_empty_title),
                            body = stringResource(R.string.notice_empty_body), modifier = Modifier.testTag("notice-empty"),
                            scroll = false,
                        )
                    }
                }
                items(items, key = { it.id }) { notice ->
                    NoticeRow(notice, language, date, zone) { onOpenNotice(notice.id) }
                }
            }
        }
    }
}

/** 공지 한 줄 — 제목과 게시일. 임의의 "중요" 배지나 그림을 넣지 않는다 */
@Composable
private fun NoticeRow(notice: Announcement, language: String, date: DateTimeFormatter, zone: ZoneId, onClick: () -> Unit) {
    val p = settingsPalette()
    Column(Modifier.fillMaxWidth().feedbackClickable(role = Role.Button, onClick = onClick).testTag("notice-row-${notice.id}")) {
        Row(Modifier.fillMaxWidth().heightIn(min = 94.dp).padding(vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(notice.title(language), color = p.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                    lineHeight = 1.35.em)
                Text(date.format(Instant.ofEpochMilli(notice.publishedAt).atZone(zone)), color = p.secondary,
                    fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = p.secondary,
                modifier = Modifier.padding(start = 12.dp).size(20.dp))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
    }
}

/** 새로 받기 실패 — 받아 둔 공지는 그대로 보이고 다시 받을 수 있다(23) */
@Composable
private fun NoticeStale(onRetry: () -> Unit, retrying: Boolean) {
    val p = settingsPalette()
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(20.dp)).background(raisedSurface())
            .padding(start = 18.dp, end = 6.dp, top = 14.dp, bottom = 14.dp).testTag("notice-stale"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.notice_stale_title), color = p.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.notice_stale_body), color = p.secondary, fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp))
        }
        Box(
            Modifier.heightIn(min = 48.dp).widthIn(min = 56.dp)
                .feedbackClickable(enabled = !retrying, role = Role.Button, onClick = onRetry).testTag("notice-retry"),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.notice_retry), color = if (retrying) p.secondary else p.accent, fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── 공통 상태 ───────────────────────────────────────────────────────

/** 비었을 때 · 못 읽었을 때 — 가운데 아이콘 · 제목 · 한 줄 · 버튼 하나. 큰 글씨면 스크롤한다 */
@Composable
internal fun PaneState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    boxed: Boolean = true,
    scroll: Boolean = true,
) {
    val p = settingsPalette()
    Column(
        modifier.fillMaxWidth().then(if (scroll) Modifier.fillMaxSize().verticalScroll(rememberScrollState()) else Modifier)
            .padding(top = 96.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(if (boxed) p.surface else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = p.accent, modifier = Modifier.size(28.dp))
        }
        Text(title, color = p.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() })
        Text(body, color = p.secondary, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 1.5.em)
        if (actionLabel != null) {
            Spacer(Modifier.height(12.dp))
            SettingsPrimaryButton(actionLabel, onAction, Modifier.fillMaxWidth().testTag("pane-action"))
        }
    }
}

/** 불러오는 동안 — 빈 목록 대신 제목 · 날짜 자리만(11 · 20) */
@Composable
private fun ListSkeleton(icons: Boolean, label: String, modifier: Modifier = Modifier) {
    val p = settingsPalette()
    Column(modifier.fillMaxWidth().padding(top = 18.dp).semantics { stateDescription = label }) {
        repeat(4) {
            Row(Modifier.fillMaxWidth().padding(vertical = 22.dp)) {
                if (icons) {
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(p.skeleton))
                    Spacer(Modifier.width(16.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.fillMaxWidth(0.72f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(p.skeleton))
                    Box(Modifier.fillMaxWidth(0.5f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(p.skeleton))
                    Box(Modifier.fillMaxWidth(0.22f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(p.skeleton))
                }
            }
            Box(Modifier.padding(start = if (icons) 50.dp else 0.dp).fillMaxWidth().height(1.dp).background(p.divider))
        }
    }
}

/** 시트 안 · 공지의 한 칸 바탕 — 어두운 테마에서는 시트보다 한 단계 밝게 */
@Composable
internal fun raisedSurface(): Color = if (StepUpColors.dark) Color(0xFF1B2B45) else settingsPalette().surface

// ── 글 · 시간 ───────────────────────────────────────────────────────

/** 오늘 · 어제 · 이전 — 사용자 시간대의 날짜로 가른다 */
enum class DayGroup { Today, Yesterday, Earlier }

internal fun dayGroup(timestamp: Long, now: Long, zone: ZoneId): DayGroup {
    val day = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    return when {
        !day.isBefore(today) -> DayGroup.Today
        day == today.minusDays(1) -> DayGroup.Yesterday
        else -> DayGroup.Earlier
    }
}

/** 오늘은 "12분 전", 어제 · 이전은 날짜("9월 27일", 올해가 아니면 연도까지) */
@Composable
internal fun timeLabel(timestamp: Long, now: Long, zone: ZoneId): String {
    if (dayGroup(timestamp, now, zone) == DayGroup.Today) return relativeTime(timestamp, now)
    val date = Instant.ofEpochMilli(timestamp).atZone(zone)
    val sameYear = date.year == Instant.ofEpochMilli(now).atZone(zone).year
    return rememberPattern(if (sameYear) R.string.date_month_day else R.string.date_month_day_year).format(date)
}

@Composable
internal fun rememberPattern(@StringRes pattern: Int): DateTimeFormatter {
    val text = stringResource(pattern)
    val locale = LocalConfiguration.current.locales[0]
    return remember(text, locale) { DateTimeFormatter.ofPattern(text, locale) }
}

@Composable
private fun relativeTime(timestamp: Long, now: Long): String {
    val elapsed = (now - timestamp).coerceAtLeast(0L)
    val minutes = elapsed / 60_000L
    val hours = elapsed / 3_600_000L
    return when {
        minutes < 1L -> stringResource(R.string.time_just_now)
        hours < 1L -> stringResource(R.string.time_minutes_ago, minutes.toInt())
        else -> stringResource(R.string.time_hours_ago, hours.toInt())
    }
}

/** 지금 시각 — 화면에 돌아올 때와 30초마다 다시 읽는다(상대 시간 · 날짜 묶음이 오래 멈춰 있지 않게) */
@Composable
private fun rememberNow(): Long {
    val ticking = produceState(System.currentTimeMillis()) {
        while (true) {
            delay(30_000)
            value = System.currentTimeMillis()
        }
    }
    var resumedAt by remember { mutableStateOf(System.currentTimeMillis()) }
    LifecycleResumeEffect(Unit) {
        resumedAt = System.currentTimeMillis()
        onPauseOrDispose { }
    }
    return maxOf(ticking.value, resumedAt)
}

internal fun iconFor(type: String): ImageVector = when (type) {
    NotificationType.REWARD_EARNED -> Icons.AutoMirrored.Filled.DirectionsWalk
    NotificationType.GOAL_REACHED -> Icons.Filled.EmojiEvents
    NotificationType.SNEAKER_MINTED -> StepUpIcons.Shoe
    NotificationType.SNEAKER_UPGRADED -> Icons.Filled.TrendingUp
    NotificationType.BOOST_ACTIVATED -> Icons.Filled.Whatshot
    NotificationType.CREW_JOINED -> Icons.Filled.Shield
    NotificationType.PARTY_FINISHED -> Icons.Filled.MilitaryTech
    NotificationType.EVENT_CLAIMED -> Icons.Filled.CheckCircle
    NotificationType.PARTY_MEMBER_LEFT -> Icons.Filled.PersonOff
    NotificationType.CREW_INVITE -> Icons.Filled.GroupAdd
    NotificationType.PARTY_INVITE -> Icons.Filled.Bolt
    NotificationType.EVENT_REWARD -> Icons.Filled.Redeem
    NotificationType.COMMENT_REPLY -> Icons.AutoMirrored.Filled.Reply
    NotificationType.COURSE_COMPLETE -> Icons.Filled.Flag
    else -> Icons.Outlined.Notifications
}

/** 알림에 저장된 슬롯 키를 지금 언어의 신발 이름으로 되살린다 */
@Composable
private fun sneakerLabel(slotKey: String): String {
    val parsed = parseSlotKey(slotKey) ?: return slotKey
    val (faction, rarity, variant) = parsed
    return variantLabel(faction, rarity, variant)
}

/** 금액을 보이는 적립 알림인가 — 서버 경제에서 폰이 계산하던 때의 기록은 금액 없이 보인다 */
internal fun showsAmount(entity: NotificationEntity): Boolean =
    entity.type == NotificationType.EVENT_CLAIMED || !ServiceLocator.serverEconomyOn

/** 저장된 인자를 표시 시점 로케일로 조립한다. */
@Composable
internal fun messageFor(entity: NotificationEntity): String {
    val amount = "%,.2f".format(entity.argAmount)
    // 서버 경제에서는 아래 네 가지 알림을 더 만들지 않는다 — 남은 것은 폰이 계산하던 때의 기록이라
    // 서버가 인정한 금액이 아니다. 금액 없이 무슨 일이었는지만 보인다.
    val legacy = ServiceLocator.serverEconomyOn
    val steps = entity.argText.toLongOrNull()?.let { "%,d".format(it) } ?: entity.argText
    return when (entity.type) {
        NotificationType.REWARD_EARNED ->
            if (legacy) stringResource(R.string.notif_reward_earned_plain, steps)
            else stringResource(R.string.notif_reward_earned, steps, amount)

        NotificationType.GOAL_REACHED ->
            if (legacy) stringResource(R.string.notif_goal_reached_plain, entity.argText)
            else stringResource(R.string.notif_goal_reached, entity.argText, amount)

        NotificationType.SNEAKER_MINTED ->
            stringResource(R.string.notif_sneaker_minted, sneakerLabel(entity.argText))

        NotificationType.SNEAKER_UPGRADED ->
            stringResource(R.string.notif_sneaker_upgraded, sneakerLabel(entity.argText))

        NotificationType.BOOST_ACTIVATED ->
            stringResource(R.string.notif_boost_activated)

        NotificationType.CREW_JOINED ->
            stringResource(R.string.notif_crew_joined, entity.argText)

        NotificationType.PARTY_FINISHED ->
            if (legacy) stringResource(R.string.notif_party_finished_plain, entity.argText)
            else stringResource(R.string.notif_party_finished, entity.argText, amount)

        NotificationType.EVENT_CLAIMED ->
            stringResource(R.string.notif_event_claimed, entity.argText, amount)

        NotificationType.PARTY_MEMBER_LEFT ->
            stringResource(R.string.notif_party_member_left, entity.argText)

        NotificationType.COMMENT_REPLY ->
            stringResource(R.string.notif_comment_reply, entity.argText)

        NotificationType.COURSE_COMPLETE ->
            if (legacy) stringResource(R.string.notif_course_complete_plain, entity.argText)
            else stringResource(R.string.notif_course_complete, entity.argText, amount)

        NotificationType.CREW_INVITE ->
            stringResource(R.string.notif_crew_invite, entity.argText)

        NotificationType.PARTY_INVITE ->
            stringResource(R.string.notif_party_invite, entity.argText)

        // 처리하지 않은 이전 보상 알림은 금액만으로 지급을 단정하지 않는다 — 내용은 시트가 설명한다(09)
        NotificationType.EVENT_REWARD ->
            if (entity.actioned) stringResource(R.string.notif_event_reward, entity.argText, amount)
            else stringResource(R.string.reward_prev_title)

        else -> stringResource(R.string.notif_title)
    }
}
