package com.stepup.android.ui.screens.notifications

import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Toll
import androidx.compose.material.icons.outlined.Whatshot
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
import com.stepup.android.domain.parseModelSlotKey
import com.stepup.android.domain.parseSlotKey
import com.stepup.android.ui.components.RunBackdrop
import com.stepup.android.ui.components.StepUpIcons
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.components.shoeModelNameRes
import com.stepup.android.ui.components.variantLabel
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.onboarding.BlueHeaderText
import com.stepup.android.ui.screens.onboarding.BlueRule
import com.stepup.android.ui.screens.onboarding.BlueTitleBar
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
 * 알림(알림·공지 v1, 2026-09-28 전달본 — docs/redesign/notifications-v1). 파란 톤 통합 전달본 v4(2026-10-03, NOT01~26)의
 * 남색 · 전기 파랑 · 청록 색감으로 다시 그렸다 — 동작(읽음 · 응답 · 실제 가입 구분, 금액 표시 조건)은 그대로다.
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
    Box(Modifier.fillMaxSize()) {
        RunBackdrop(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().padding(horizontal = StepUpDesign.Gutter)) {
            BlueTitleBar(
                stringResource(R.string.notif_title), onBack = onBack, backTag = "inbox-back",
                trailing = {
                    BlueHeaderText(stringResource(R.string.inbox_settings), onOpenSettings, Modifier.testTag("inbox-settings"))
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
        BlueToast(message, Modifier.align(Alignment.BottomCenter).padding(horizontal = StepUpDesign.Gutter, vertical = 16.dp),
            success = ui.toast?.success != false)
    }
}

/** 내 알림 · 공지 — 같은 폭 두 칸. 고른 쪽은 청록 글자와 글자 아래 짧은 청록 밑줄, 아래로 파란 가는 선 */
@Composable
private fun InboxTabs(selected: NotifTab, onSelect: (NotifTab) -> Unit) {
    val t = runTone()
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            listOf(
                NotifTab.Inbox to R.string.inbox_tab_mine,
                NotifTab.Notices to R.string.inbox_tab_notices,
            ).forEach { (tab, label) ->
                val on = tab == selected
                Box(
                    Modifier.weight(1f).heightIn(min = 56.dp)
                        .feedbackClickable(role = null, cue = FeedbackCue.Select) { onSelect(tab) }
                        .semantics {
                            role = Role.Tab
                            this.selected = on
                        }
                        .testTag("inbox-tab-${tab.name.lowercase()}"),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Column(Modifier.width(IntrinsicSize.Max), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.padding(top = 12.dp, bottom = 10.dp, start = 18.dp, end = 18.dp)) {
                            // 굵은 글자의 폭을 미리 잡아 둔다 — 고를 때 밑줄이 흔들리지 않는다
                            Text(stringResource(label), style = runTextStyle(19.sp, Color.Transparent, FontWeight.Bold),
                                modifier = Modifier.clearAndSetSemantics { })
                            Text(stringResource(label), style = runTextStyle(19.sp, if (on) t.cyan else t.label,
                                if (on) FontWeight.Bold else FontWeight.SemiBold))
                        }
                        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                            .background(if (on) t.cyan else Color.Transparent))
                    }
                }
            }
        }
        BlueRule(strong = true)
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
            icon = Icons.Outlined.ErrorOutline, title = stringResource(R.string.inbox_load_failed),
            body = stringResource(R.string.inbox_load_failed_body), actionLabel = stringResource(R.string.set_reload),
            onAction = onReload, modifier = Modifier.testTag("inbox-failed"),
        )
        is InboxLoad.Ready -> if (load.items.isEmpty()) {
            PaneState(
                icon = Icons.Outlined.NotificationsActive, title = stringResource(R.string.inbox_empty_title),
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
                contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
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

/** 날짜 묶음 이름. 첫 묶음 줄에 새 알림 수와 "모두 읽음"(누를 때만 — 들어온 것만으로 읽음 처리하지 않는다) */
@Composable
private fun GroupHeader(group: DayGroup, first: Boolean, unread: Int, readingAll: Boolean, onMarkAllRead: () -> Unit) {
    val t = runTone()
    Column(Modifier.fillMaxWidth().padding(top = if (first) 0.dp else 22.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(
                    when (group) {
                        DayGroup.Today -> R.string.inbox_group_today
                        DayGroup.Yesterday -> R.string.inbox_group_yesterday
                        DayGroup.Earlier -> R.string.inbox_group_earlier
                    },
                ),
                style = runTextStyle(19.sp, t.text, FontWeight.Bold),
                modifier = Modifier.semantics { heading() },
            )
            if (first) {
                Spacer(Modifier.width(16.dp))
                Text(
                    if (unread > 0) stringResource(R.string.inbox_new_count, unread) else stringResource(R.string.inbox_all_read),
                    style = runTextStyle(15.sp, t.label, FontWeight.Medium),
                    modifier = Modifier.weight(1f).testTag("inbox-summary"),
                )
                if (unread > 0) {
                    Box(
                        Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                            .feedbackClickable(enabled = !readingAll, role = Role.Button, onClick = onMarkAllRead)
                            .padding(start = 8.dp).testTag("inbox-read-all"),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Text(stringResource(R.string.inbox_mark_all_read),
                            style = runTextStyle(16.sp, if (readingAll) t.muted else t.label, FontWeight.SemiBold))
                    }
                }
            }
        }
        BlueRule(strong = true)
    }
}

/** 알림 한 줄 — 줄 전체가 누르는 곳. 선 그림 · 본문 · 시간 · 읽지 않은 청록 점 · 화살표. 읽지 않음을 읽어 준다 */
@Composable
private fun NotificationRow(entity: NotificationEntity, now: Long, zone: ZoneId, onClick: () -> Unit) {
    val t = runTone()
    val unreadLabel = stringResource(R.string.inbox_unread)
    Column(
        Modifier.fillMaxWidth()
            .feedbackClickable(role = Role.Button, onClick = onClick)
            .semantics { if (!entity.read) stateDescription = unreadLabel }
            .testTag("notif-row-${entity.id}"),
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 92.dp).padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Icon(iconFor(entity.type), contentDescription = null, tint = t.label, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    messageFor(entity),
                    style = runTextStyle(18.sp, t.text, if (entity.read) FontWeight.Medium else FontWeight.Bold, 1.4f),
                )
                Text(timeLabel(entity.timestamp, now, zone), style = runTextStyle(15.sp, t.label, FontWeight.Medium),
                    modifier = Modifier.padding(top = 6.dp))
            }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.size(12.dp).clip(CircleShape)
                    .background(if (entity.read) Color.Transparent else t.cyan)
                    .then(if (entity.read) Modifier else Modifier.testTag("notif-unread-${entity.id}")),
            )
            Spacer(Modifier.width(10.dp))
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = t.label, modifier = Modifier.size(28.dp))
        }
        BlueRule(strong = true)
    }
}

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
        // 받아 둔 공지가 없고 처음 받기도 실패(NOT18)
        items == null && board.refreshFailed && !board.loading -> PaneState(
            icon = Icons.Outlined.ErrorOutline, title = stringResource(R.string.notice_load_failed),
            body = stringResource(R.string.inbox_load_failed_body), actionLabel = stringResource(R.string.set_reload),
            onAction = onRetry, modifier = Modifier.testTag("notice-failed"),
        )
        // 받는 중(NOT20) — 빈 목록으로 보이지 않는다
        items == null -> ListSkeleton(icons = false, modifier = Modifier.testTag("notice-loading"),
            label = stringResource(R.string.notice_loading))
        else -> {
            val t = runTone()
            val language = LocalConfiguration.current.locales[0].language
            val date = rememberPattern(R.string.date_notice)
            LazyColumn(
                state = state,
                modifier = Modifier.fillMaxSize().testTag("notice-list"),
                contentPadding = PaddingValues(top = 14.dp, bottom = 28.dp),
            ) {
                if (board.refreshFailed) {
                    // 새로 받기 실패(NOT23) — 받아 둔 목록은 그대로, 같은 뜻의 소제목은 뺀다
                    item(key = "notice-stale") { NoticeStale(onRetry, retrying = board.loading) }
                } else {
                    item(key = "notice-header") {
                        Column {
                            Text(stringResource(R.string.notice_header), style = runTextStyle(16.sp, t.label, FontWeight.SemiBold),
                                modifier = Modifier.padding(top = 6.dp, bottom = 12.dp))
                            BlueRule(strong = true)
                        }
                    }
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

/** 공지 한 줄 — 제목과 게시일. 임의의 "중요" · NEW 배지나 그림을 넣지 않는다 */
@Composable
private fun NoticeRow(notice: Announcement, language: String, date: DateTimeFormatter, zone: ZoneId, onClick: () -> Unit) {
    val t = runTone()
    Column(Modifier.fillMaxWidth().feedbackClickable(role = Role.Button, onClick = onClick).testTag("notice-row-${notice.id}")) {
        Row(Modifier.fillMaxWidth().heightIn(min = 92.dp).padding(vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(notice.title(language), style = runTextStyle(20.sp, t.text, FontWeight.Bold, 1.35f))
                Text(date.format(Instant.ofEpochMilli(notice.publishedAt).atZone(zone)),
                    style = runTextStyle(16.sp, t.label, FontWeight.Medium), modifier = Modifier.padding(top = 8.dp))
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = t.label,
                modifier = Modifier.padding(start = 12.dp).size(28.dp))
        }
        BlueRule(strong = true)
    }
}

/** 새로 받기 실패(NOT23) — 받아 둔 공지는 그대로 보이고 "다시"로 다시 받는다. 받는 동안 "다시"는 잠근다 */
@Composable
private fun NoticeStale(onRetry: () -> Unit, retrying: Boolean) {
    val t = runTone()
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp).clip(shape).background(t.panel, shape)
            .border(1.dp, t.secondaryEdge, shape)
            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp).testTag("notice-stale"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = t.cobaltText, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.notice_stale_title), style = runTextStyle(17.sp, t.text, FontWeight.Bold, 1.35f))
            Text(stringResource(R.string.notice_stale_body), style = runTextStyle(15.sp, t.label, FontWeight.Medium),
                modifier = Modifier.padding(top = 4.dp))
        }
        Box(Modifier.padding(horizontal = 8.dp).width(1.dp).height(36.dp).background(t.panelEdge))
        Box(
            Modifier.heightIn(min = 48.dp).widthIn(min = 64.dp).clip(RoundedCornerShape(12.dp))
                .feedbackClickable(enabled = !retrying, role = Role.Button, onClick = onRetry).testTag("notice-retry"),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.notice_retry), style = runTextStyle(17.sp, if (retrying) t.muted else t.cyan, FontWeight.Bold))
        }
    }
}

// ── 공통 상태 ───────────────────────────────────────────────────────

/** 불러오는 동안(NOT11 · NOT20) — 빈 목록이나 0 대신 "불러오고 있어요" 한 줄과 행 자리만 */
@Composable
private fun ListSkeleton(icons: Boolean, label: String, modifier: Modifier = Modifier) {
    val t = runTone()
    Column(modifier.fillMaxWidth().padding(top = 18.dp).semantics(mergeDescendants = true) { stateDescription = label }) {
        Text(label, style = runTextStyle(16.sp, t.label, FontWeight.Medium), modifier = Modifier.padding(bottom = 10.dp))
        repeat(4) {
            Row(Modifier.fillMaxWidth().padding(vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                if (icons) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(t.panelEdge.copy(alpha = 0.5f)))
                    Spacer(Modifier.width(18.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SkeletonBar(if (icons) 0.85f else 0.92f, 14.dp)
                    if (icons) SkeletonBar(0.6f, 14.dp)
                    SkeletonBar(if (icons) 0.2f else 0.3f, 10.dp)
                }
            }
            BlueRule(strong = true)
        }
    }
}

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
    NotificationType.REWARD_EARNED -> Icons.Outlined.Toll
    NotificationType.GOAL_REACHED -> Icons.Outlined.EmojiEvents
    NotificationType.SNEAKER_MINTED -> StepUpIcons.Shoe
    NotificationType.SNEAKER_UPGRADED -> Icons.AutoMirrored.Outlined.TrendingUp
    NotificationType.BOOST_ACTIVATED -> Icons.Outlined.Whatshot
    NotificationType.CREW_JOINED -> Icons.Outlined.Shield
    NotificationType.PARTY_FINISHED -> Icons.Outlined.MilitaryTech
    NotificationType.EVENT_CLAIMED -> Icons.Outlined.CheckCircle
    NotificationType.PARTY_MEMBER_LEFT -> Icons.Outlined.PersonOff
    NotificationType.CREW_INVITE -> Icons.Outlined.Group
    NotificationType.PARTY_INVITE -> Icons.AutoMirrored.Outlined.DirectionsRun
    NotificationType.EVENT_REWARD -> Icons.Outlined.Redeem
    NotificationType.COMMENT_REPLY -> Icons.Outlined.ChatBubbleOutline
    NotificationType.COURSE_COMPLETE -> Icons.Outlined.Flag
    else -> Icons.Outlined.Notifications
}

/** 알림에 저장된 슬롯 키를 지금 언어의 신발 이름으로 되살린다 */
@Composable
private fun sneakerLabel(slotKey: String): String {
    parseModelSlotKey(slotKey)?.let(::shoeModelNameRes)?.let { return stringResource(it) }
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
    // 서버가 인정한 금액이 아니다. 금액 없이 무슨 일이었는지만 보인다(NOT25). 시트도 같은 [showsAmount] 를 쓴다.
    val legacy = !showsAmount(entity)
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
