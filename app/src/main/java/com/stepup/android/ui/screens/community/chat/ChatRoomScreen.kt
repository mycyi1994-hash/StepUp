package com.stepup.android.ui.screens.community.chat

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.data.repo.CrewPhotos
import com.stepup.android.domain.ChatItem
import com.stepup.android.domain.ChatKind
import com.stepup.android.domain.ChatMessage
import com.stepup.android.domain.ChatReportReason
import com.stepup.android.domain.ChatRules
import com.stepup.android.domain.CrewCard
import com.stepup.android.ui.components.SettingsToast
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.community.crew.CrewButton
import com.stepup.android.ui.screens.community.crew.CrewButtonKind
import com.stepup.android.ui.screens.community.crew.CrewConfirmSheet
import com.stepup.android.ui.screens.community.crew.CrewImage
import com.stepup.android.ui.screens.community.crew.CrewSheet
import com.stepup.android.ui.screens.community.crew.crewInk
import java.io.File
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 대화에서 나가는 곳 */
class ChatRoomActions(
    val onBack: () -> Unit,
    val onInfo: () -> Unit,
    val onNotice: (Long) -> Unit,
    val onManageNotice: (Long) -> Unit,
    val onProfile: (String) -> Unit,
    val onMyProfile: () -> Unit,
    val onPhoto: () -> Unit,
    val onViewer: (Long) -> Unit,
    val onReported: () -> Unit,
    val onNoticeFromMessage: () -> Unit,
    val onRecruit: () -> Unit,
    val onEnded: () -> Unit,
)

/**
 * 기기 검사 — OS 사진 선택기 · 카메라(다른 앱 화면이라 검사가 누를 수 없다) 대신 고른 사진([answer])이나 사진 접근 불가
 * ([unavailable]). 앱에서는 늘 비어 있다.
 */
object ChatPhotoPickerForTest {
    @Volatile var answer: Uri? = null
    @Volatile var unavailable: Boolean = false
}

private const val PROBLEM_BROKEN = "broken"
private const val PROBLEM_DENIED = "denied"

/**
 * 02 크루원 · 03 크루장 대화 — 같은 대화를 작성자와 지금 사용자가 같은지로 좌우를 나눈다(크루장이라고 모두 오른쪽이 아니다).
 * 방이 보이는 동안만 서버를 따라오고(3초), 끊기면 20, 이전 대화를 보는 중에는 21 새 메시지 버튼. 읽음은 앱이 켜져 있고
 * 이 대화가 실제로 보일 때만 보낸다.
 */
@Composable
fun ChatRoomScreen(viewModel: ChatRoomViewModel, actions: ChatRoomActions) {
    val ink = crewInk()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val replyTo by viewModel.replyTo.collectAsStateWithLifecycle()
    val sheet by viewModel.sheet.collectAsStateWithLifecycle()
    val crewId = viewModel.crewId
    var toast by remember { mutableStateOf<String?>(null) }
    var resumed by remember { mutableStateOf(false) }
    var photoProblem by rememberSaveable { mutableStateOf("") }
    var cameraPath by rememberSaveable { mutableStateOf<String?>(null) }
    val ownerLostText = stringResource(R.string.chat_owner_lost)

    LifecycleResumeEffect(viewModel) {
        resumed = true
        viewModel.setActive(true)
        ChatVisibility.crewId = crewId
        ChatRoomNotes.takeAnchor(crewId)?.let { (id, seq) -> viewModel.reveal(id, seq) }
        if (ChatRoomNotes.takeOwnerLost(crewId)) toast = ownerLostText
        onPauseOrDispose {
            resumed = false
            viewModel.setActive(false)
            if (ChatVisibility.crewId == crewId) ChatVisibility.crewId = null
        }
    }
    LaunchedEffect(ui.load) { if (ui.load == ChatRoomLoad.ENDED) actions.onEnded() }
    val texts = ChatEventTexts(
        copied = stringResource(R.string.chat_copied),
        ownerLost = ownerLostText,
        gone = stringResource(R.string.chat_action_gone),
        searchGone = stringResource(R.string.chat_search_gone),
    )
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ChatEventOnce.Copied -> toast = texts.copied
                ChatEventOnce.OwnerLost -> toast = texts.ownerLost
                ChatEventOnce.Gone -> toast = texts.gone
                ChatEventOnce.SearchGone -> toast = texts.searchGone
                ChatEventOnce.Reported -> actions.onReported()
            }
        }
    }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2_400)
            toast = null
        }
    }

    // 고른 사진 → 보낼 모양으로 → 26 확인. 읽을 수 없거나 접근이 막혔으면 안내(글 대화는 그대로)
    fun handlePicked(uri: Uri) {
        scope.launch {
            when (prepareChatPhoto(context, crewId, uri)) {
                is CrewPhotos.Loaded.Ok -> actions.onPhoto()
                CrewPhotos.Loaded.Broken -> photoProblem = PROBLEM_BROKEN
                CrewPhotos.Loaded.Denied -> photoProblem = PROBLEM_DENIED
            }
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // 고르지 않고 돌아오면 쓰던 글 그대로
        if (uri != null) handlePicked(uri)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path = cameraPath
        if (ok && path != null) {
            runCatching { FileProvider.getUriForFile(context, "${context.packageName}.share", File(path)) }.getOrNull()?.let(::handlePicked)
        }
    }
    val cameraMissing = stringResource(R.string.chat_camera_missing)
    val pickPhoto: () -> Unit = {
        viewModel.closeSheet()
        val staged = ChatPhotoPickerForTest.answer
        when {
            ChatPhotoPickerForTest.unavailable -> photoProblem = PROBLEM_DENIED
            staged != null -> handlePicked(staged)
            else -> runCatching { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                .onFailure { photoProblem = PROBLEM_BROKEN }
        }
    }
    val takePhoto: () -> Unit = {
        viewModel.closeSheet()
        val file = runCatching {
            File(context.cacheDir, "camera").apply { mkdirs() }.let { dir -> File(dir, "chat-${System.currentTimeMillis()}.jpg") }
        }.getOrNull()
        val uri = file?.let { runCatching { FileProvider.getUriForFile(context, "${context.packageName}.share", it) }.getOrNull() }
        if (file == null || uri == null) {
            toast = cameraMissing
        } else {
            cameraPath = file.path
            try {
                camera.launch(uri)
            } catch (_: ActivityNotFoundException) {
                toast = cameraMissing
            } catch (_: SecurityException) {
                photoProblem = PROBLEM_DENIED
            }
        }
    }

    val meta = ui.meta
    val card: CrewCard? = meta?.card ?: viewModel.preview?.card
    val solo = meta != null && meta.memberCount <= 1 && ui.messages.none { it.kind != ChatKind.SYSTEM }

    Column(Modifier.fillMaxSize().background(ink.canvas).imePadding().testTag(if (meta?.owner == true) "chat-room-owner" else "chat-room")) {
        if (solo) {
            ChatSoloHeader(card?.name.orEmpty(), actions.onBack, actions.onInfo)
        } else {
            // 36 불러오기 실패에서도 채팅방 정보는 열린다(정보 화면이 서버에 다시 묻는다)
            ChatRoomHeader(card, meta?.memberCount ?: viewModel.preview?.memberCount, actions.onBack, onInfo = actions.onInfo)
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                ui.load == ChatRoomLoad.ERROR && meta == null -> ChatRetryState(
                    title = stringResource(R.string.chat_error_title),
                    body = stringResource(R.string.chat_error_body),
                    button = stringResource(R.string.chat_error_retry),
                    onRetry = viewModel::retryLoad,
                    modifier = Modifier.align(Alignment.Center),
                    tag = "chat-room-error",
                )
                meta == null -> CircularProgressIndicator(
                    Modifier.align(Alignment.Center).size(24.dp).testTag("chat-room-loading"), color = ink.secondary, strokeWidth = 2.dp,
                )
                else -> Column(Modifier.fillMaxSize()) {
                    meta.pinned?.let { pinned ->
                        ChatPinnedNotice(
                            pinned.title, meta.owner,
                            onOpen = { actions.onNotice(pinned.id) },
                            onManage = { actions.onManageNotice(pinned.id) },
                            modifier = Modifier.padding(horizontal = ChatGutter).padding(top = 4.dp),
                        )
                    }
                    if (!ui.online) ChatReconnectBanner(Modifier.padding(horizontal = ChatGutter).padding(top = 12.dp))
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        if (solo) {
                            ChatSoloIntro(meta.card, meta.owner, actions.onRecruit)
                        } else {
                            ChatMessages(ui, viewModel, actions, resumed)
                        }
                        SettingsToast(toast, Modifier.align(Alignment.BottomCenter).padding(horizontal = 24.dp, vertical = 12.dp), success = false)
                    }
                }
            }
            if (meta == null) SettingsToast(toast, Modifier.align(Alignment.BottomCenter).padding(horizontal = 24.dp, vertical = 12.dp), success = false)
        }
        Box(Modifier.navigationBarsPadding()) {
            ChatComposer(
                text = draft,
                onText = viewModel::setDraft,
                editable = ui.load != ChatRoomLoad.ERROR,
                active = ui.load == ChatRoomLoad.READY && ui.online,
                offline = ui.load == ChatRoomLoad.ERROR || !ui.online,
                onSend = viewModel::send,
                onAttach = viewModel::openAttach,
                replyTo = replyTo,
                onCancelReply = viewModel::cancelReply,
            )
        }
    }

    ChatRoomSheets(sheet, ui, viewModel, actions, context, pickPhoto, takePhoto)
    when (photoProblem) {
        PROBLEM_BROKEN, PROBLEM_DENIED -> ChatPhotoProblemSheet(
            denied = photoProblem == PROBLEM_DENIED,
            onPrimary = {
                val denied = photoProblem == PROBLEM_DENIED
                photoProblem = ""
                if (denied) {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                } else {
                    pickPhoto()
                }
            },
            onDismiss = { photoProblem = "" },
        )
    }
}

private class ChatEventTexts(val copied: String, val ownerLost: String, val gone: String, val searchGone: String)

/**
 * 대화 목록 — 최신이 아래(reverseLayout). 맨 아래를 보고 있으면 새 메시지가 이어지고, 이전 대화를 보는 중이면 위치를 두고
 * 21 새 메시지 버튼을 띄운다. 위로 이전 대화를 붙일 때는 보던 메시지가 그대로 남는다.
 */
@Composable
private fun ChatMessages(ui: ChatRoomUi, viewModel: ChatRoomViewModel, actions: ChatRoomActions, resumed: Boolean) {
    val meta = ui.meta ?: return
    val ink = crewInk()
    val listState = rememberLazyListState()
    val items = remember(ui.messages, meta, ui.me) { ChatRules.timeline(ui.messages, meta, ui.me, ZoneId.systemDefault()).reversed() }
    val latestItems by rememberUpdatedState(items)
    // 사용자가 스크롤을 멈춘 곳이 맨 아래인가 — 새 메시지가 들어오는 순간이 아니라 스크롤이 끝날 때만 바꾼다
    var stick by remember { mutableStateOf(true) }
    val newest = ChatRules.newestSeq(ui.messages)
    var seenSeq by remember { mutableLongStateOf(newest) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (!scrolling) stick = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 48
        }
    }
    val newestKey = items.firstOrNull()?.key
    val mineLast = ui.messages.lastOrNull()?.let { it.authorId == ui.me && it.kind != ChatKind.SYSTEM } == true
    LaunchedEffect(newestKey) {
        if (stick || mineLast) {
            listState.scrollToItem(0)
            stick = true
        }
    }
    LaunchedEffect(stick, newest) { if (stick) seenSeq = newest }
    // 이전 대화 — 위 끝에 가까워지면 더 읽는다
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { last ->
            if (last >= latestItems.size - 4) viewModel.loadOlder()
        }
    }
    // 읽음 — 앱이 켜져 있고 이 대화가 보이는 동안, 화면에 보이는 가장 늦은 메시지까지
    LaunchedEffect(listState, resumed) {
        if (!resumed) return@LaunchedEffect
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.maxOfOrNull { info ->
                when (val item = latestItems.getOrNull(info.index)) {
                    is ChatItem.Bubble -> item.message.seq ?: 0L
                    is ChatItem.Line -> item.message.seq ?: 0L
                    else -> 0L
                }
            } ?: 0L
        }.collect { seq -> if (seq > 0) viewModel.seen(seq) }
    }
    // 검색 결과 · 답장 원문으로 옮겨 와 잠깐 강조
    LaunchedEffect(ui.highlight) {
        val id = ui.highlight ?: return@LaunchedEffect
        val index = latestItems.indexOfFirst { (it as? ChatItem.Bubble)?.message?.id == id }
        if (index >= 0) {
            stick = false
            listState.animateScrollToItem(index)
        }
    }
    val unseen = if (stick) 0 else ui.messages.count { (it.seq ?: 0L) > seenSeq && it.authorId != ui.me && it.kind != ChatKind.SYSTEM }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val maxBubble = (maxWidth * 0.68f).coerceIn(180.dp, 420.dp)
        LazyColumn(
            Modifier.fillMaxSize().testTag("chat-messages"),
            state = listState,
            reverseLayout = true,
            contentPadding = PaddingValues(top = 4.dp, bottom = 10.dp),
        ) {
            items(items, key = { it.key }, contentType = { it::class.simpleName }) { item ->
                when (item) {
                    is ChatItem.Day -> ChatDayLine(item.day)
                    is ChatItem.Line -> ChatSystemLine(item.message)
                    is ChatItem.Bubble -> ChatBubbleRow(
                        crewId = viewModel.crewId,
                        message = item.message,
                        mine = item.mine,
                        header = item.header,
                        time = item.time,
                        owner = item.owner,
                        unread = item.unread,
                        highlight = ui.highlight != null && ui.highlight == item.message.id,
                        maxBubble = maxBubble,
                        onLongPress = { viewModel.openMenu(item.message) },
                        onFace = { item.message.authorId?.let(actions.onProfile) },
                        onPhoto = { item.message.id?.let(actions.onViewer) },
                        onReply = { reply -> viewModel.reveal(reply.id, reply.seq) },
                        onRetry = { viewModel.retry(item.message) },
                        onDiscard = { viewModel.discard(item.message) },
                    )
                }
            }
            if (ui.olderLoading) {
                item(key = "older") {
                    Box(Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = ink.secondary, strokeWidth = 2.dp)
                    }
                }
            }
        }
        if (unseen > 0) {
            val scope = rememberCoroutineScope()
            ChatNewMessagesButton(
                unseen,
                onClick = {
                    scope.launch {
                        listState.animateScrollToItem(0)
                        stick = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            )
        }
    }
}

/** 30 머리 — 가운데 크루 이름, 오른쪽 "정보" */
@Composable
private fun ChatSoloHeader(name: String, onBack: () -> Unit, onInfo: () -> Unit) {
    val ink = crewInk()
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        ChatBackButton(onBack)
        Text(
            name, color = ink.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
        Box(
            Modifier.size(width = 56.dp, height = 48.dp).clip(RoundedCornerShape(12.dp)).feedbackClickable(role = Role.Button, onClick = onInfo)
                .testTag("chat-solo-info"),
            contentAlignment = Alignment.Center,
        ) { Text(stringResource(R.string.chat_solo_info), color = ink.info, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
    }
}

/** 30 혼자 있는 새 크루 — 첫 인사를 남길 수 있다. 크루장에게만 "크루 모집하기"(기존 모집 설정) */
@Composable
private fun ChatSoloIntro(card: CrewCard, owner: Boolean, onRecruit: () -> Unit) {
    val ink = crewInk()
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).testTag("chat-solo"), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        CrewImage(card, 120.dp, 22.dp)
        Spacer(Modifier.height(30.dp))
        Text(stringResource(R.string.chat_solo_title), color = ink.text, fontSize = 24.sp, lineHeight = 31.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.chat_solo_body), color = ink.secondary, fontSize = 13.5.sp, lineHeight = 25.sp, textAlign = TextAlign.Center)
        if (owner) {
            Spacer(Modifier.height(36.dp))
            CrewButton(stringResource(R.string.chat_solo_recruit), onRecruit, Modifier.width(232.dp).testTag("chat-solo-recruit"), CrewButtonKind.SECONDARY)
        }
        Spacer(Modifier.weight(1.3f))
    }
}

/** 메시지 메뉴(14 · 15 · 38) · 삭제(17) · 숨김(39) · 신고(28) · 사진 보내기(25) */
@Composable
private fun ChatRoomSheets(
    sheet: ChatSheet?,
    ui: ChatRoomUi,
    viewModel: ChatRoomViewModel,
    actions: ChatRoomActions,
    context: android.content.Context,
    onPick: () -> Unit,
    onCamera: () -> Unit,
) {
    val owner = ui.meta?.owner == true
    when (sheet) {
        null -> Unit
        is ChatSheet.Menu -> {
            val message = sheet.message
            val mine = message.authorId == ui.me
            val body = message.body?.takeIf { it.isNotBlank() }
            ChatMenuSheet(
                title = stringResource(
                    when {
                        mine -> R.string.chat_menu_mine_title
                        owner -> R.string.chat_menu_owner_title
                        else -> R.string.chat_menu_title
                    },
                ),
                tag = when {
                    mine -> "chat-menu-mine"
                    owner -> "chat-menu-owner"
                    else -> "chat-menu"
                },
                onDismiss = viewModel::closeSheet,
            ) {
                ChatSheetRow(stringResource(R.string.chat_menu_reply), { viewModel.reply(message) }, "chat-menu-reply")
                if (body != null) {
                    ChatSheetRow(stringResource(R.string.chat_menu_copy), {
                        copyChatText(context, body)
                        viewModel.copied()
                    }, "chat-menu-copy")
                }
                if (mine) {
                    // 서버가 허락한 내 메시지만(삭제 가능 시간을 앱이 새로 정하지 않는다)
                    if (message.canDelete) ChatSheetRow(stringResource(R.string.chat_menu_delete), { viewModel.askDelete(message) }, "chat-menu-delete", warn = true)
                } else {
                    if (owner) {
                        if (body != null) {
                            ChatSheetRow(stringResource(R.string.chat_menu_notice), {
                                viewModel.seedNotice(message)
                                actions.onNoticeFromMessage()
                            }, "chat-menu-notice")
                        }
                        ChatSheetRow(stringResource(R.string.chat_menu_hide), { viewModel.askHide(message) }, "chat-menu-hide", warn = true)
                    }
                    ChatSheetRow(stringResource(R.string.chat_menu_report), { viewModel.askReport(message) }, "chat-menu-report")
                }
            }
        }
        is ChatSheet.Delete -> CrewConfirmSheet(
            title = stringResource(R.string.chat_delete_title),
            body = stringResource(R.string.chat_delete_body),
            confirm = stringResource(R.string.chat_delete_confirm),
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::closeSheet,
            busy = sheet.state.busy,
            danger = true,
            tag = "chat-delete",
            error = sheet.state.error?.let { chatConfirmErrorText(it) },
        )
        is ChatSheet.Hide -> CrewConfirmSheet(
            title = stringResource(R.string.chat_hide_title),
            body = stringResource(R.string.chat_hide_body),
            confirm = stringResource(R.string.chat_hide_confirm),
            onConfirm = viewModel::confirmHide,
            onDismiss = viewModel::closeSheet,
            busy = sheet.state.busy,
            danger = true,
            tag = "chat-hide",
            error = sheet.state.error?.let { chatConfirmErrorText(it) },
        )
        is ChatSheet.Report -> ChatReportSheet(sheet, onPick = viewModel::pickReason, onSend = viewModel::confirmReport, onDismiss = viewModel::closeSheet)
        ChatSheet.Attach -> ChatMenuSheet(stringResource(R.string.chat_attach_title), "chat-attach-sheet", viewModel::closeSheet) {
            ChatSheetRow(stringResource(R.string.chat_attach_pick), onPick, "chat-attach-pick")
            ChatSheetRow(stringResource(R.string.chat_attach_camera), onCamera, "chat-attach-camera")
            Spacer(Modifier.height(26.dp))
            Text(stringResource(R.string.chat_attach_note), color = crewInk().secondary, fontSize = 12.5.sp)
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
internal fun chatConfirmErrorText(error: ChatConfirmError): String = stringResource(
    when (error) {
        ChatConfirmError.FAILED -> R.string.chat_action_error
        ChatConfirmError.GONE -> R.string.chat_action_gone
    },
)

/** 메뉴 시트 — 제목과 줄들 */
@Composable
internal fun ChatMenuSheet(title: String, tag: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    CrewSheet(title, onDismiss, Modifier.testTag(tag)) {
        Spacer(Modifier.height(12.dp))
        content()
        Spacer(Modifier.height(44.dp))
    }
}

/** 28 메시지 신고 — 사유를 고르고 신고하기. 실패하면 고른 사유 그대로 같은 시트에서 다시 */
@Composable
private fun ChatReportSheet(sheet: ChatSheet.Report, onPick: (ChatReportReason) -> Unit, onSend: () -> Unit, onDismiss: () -> Unit) {
    val ink = crewInk()
    CrewSheet(stringResource(R.string.chat_report_title), onDismiss, Modifier.testTag("chat-report"), dismissible = !sheet.state.busy) {
        Spacer(Modifier.height(12.dp))
        listOf(
            ChatReportReason.ABUSE to R.string.chat_report_abuse,
            ChatReportReason.SPAM to R.string.chat_report_spam,
            ChatReportReason.CONTENT to R.string.chat_report_content,
        ).forEach { (reason, label) ->
            ChatSheetRow(
                stringResource(label), { onPick(reason) }, "chat-report-${reason.name.lowercase()}",
                selected = sheet.reason == reason, enabled = !sheet.state.busy,
            )
        }
        if (sheet.state.error != null) {
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(if (sheet.state.error == ChatConfirmError.GONE) R.string.chat_action_gone else R.string.chat_report_error),
                color = ink.warn, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.testTag("chat-report-error"),
            )
        }
        Spacer(Modifier.height(96.dp))
        CrewButton(
            stringResource(R.string.chat_report_send), onSend, Modifier.testTag("chat-report-send"),
            kind = if (sheet.reason == null) CrewButtonKind.DISABLED else CrewButtonKind.PRIMARY,
            busy = sheet.state.busy,
            enabled = sheet.reason != null,
        )
    }
}

/** 사진을 읽을 수 없다 · 사진 접근이 막혔다 — 다른 사진 · 기기 설정, 글 대화는 그대로 */
@Composable
private fun ChatPhotoProblemSheet(denied: Boolean, onPrimary: () -> Unit, onDismiss: () -> Unit) {
    val ink = crewInk()
    CrewSheet(
        stringResource(if (denied) R.string.chat_photo_denied_title else R.string.chat_photo_broken_title), onDismiss,
        Modifier.testTag(if (denied) "chat-photo-denied" else "chat-photo-broken"),
    ) {
        Spacer(Modifier.height(18.dp))
        Text(
            stringResource(if (denied) R.string.chat_photo_denied_body else R.string.chat_photo_broken_body),
            color = ink.secondary, fontSize = 15.sp, lineHeight = 25.sp,
        )
        Spacer(Modifier.height(64.dp))
        CrewButton(
            stringResource(if (denied) R.string.chat_photo_settings else R.string.chat_photo_other),
            onPrimary, Modifier.testTag("chat-photo-primary"),
        )
        Spacer(Modifier.height(16.dp))
        CrewButton(stringResource(R.string.chat_cancel), onDismiss, Modifier.testTag("chat-photo-cancel"), CrewButtonKind.SECONDARY)
    }
}
