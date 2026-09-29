package com.stepup.android.ui.screens.community.chat

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.domain.ChatNotice
import com.stepup.android.domain.ChatRoomMeta
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.community.crew.CrewBottomBar
import com.stepup.android.ui.screens.community.crew.CrewButton
import com.stepup.android.ui.screens.community.crew.CrewButtonKind
import com.stepup.android.ui.screens.community.crew.CrewConfirmSheet
import com.stepup.android.ui.screens.community.crew.CrewFieldLabel
import com.stepup.android.ui.screens.community.crew.CrewGutter
import com.stepup.android.ui.screens.community.crew.CrewPage
import com.stepup.android.ui.screens.community.crew.CrewSheet
import com.stepup.android.ui.screens.community.crew.CrewSkeletonBox
import com.stepup.android.ui.screens.community.crew.CrewTextField
import com.stepup.android.ui.screens.community.crew.CrewTopBar
import com.stepup.android.ui.screens.community.crew.crewInk
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 공지 날짜 "9월 28일" */
@Composable
private fun noticeDay(millis: Long): String {
    val pattern = stringResource(R.string.chat_notice_date_pattern)
    return runCatching {
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    }.getOrDefault("")
}

// ─────────────────────────────────────────────────────────────
// 09 공지 모아보기
// ─────────────────────────────────────────────────────────────

/** 09 공지 모아보기 — 위에 고정 공지 하나, 아래 지난 공지. 각각 자기 공지 id 로 08 을 연다 */
@Composable
fun ChatNoticesScreen(viewModel: ChatNoticesViewModel, onBack: () -> Unit, onOpen: (Long) -> Unit, onEnded: () -> Unit) {
    val ink = crewInk()
    val notices by viewModel.notices.collectAsStateWithLifecycle()
    val meta by viewModel.meta.collectAsStateWithLifecycle()
    val ended by viewModel.ended.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.load()
        onPauseOrDispose {}
    }
    LaunchedEffect(ended) { if (ended) onEnded() }
    CrewPage(Modifier.testTag("chat-notices")) {
        CrewTopBar(stringResource(R.string.chat_notices_bar), onBack)
        when (val state = notices) {
            is ChatLoad.Ready -> LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = CrewGutter, end = CrewGutter, bottom = 40.dp)) {
                item(key = "heading") {
                    Text(
                        stringResource(R.string.chat_notices_heading, meta?.name.orEmpty()), color = ink.text, fontSize = 26.sp, lineHeight = 33.sp,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 22.dp, bottom = 34.dp),
                    )
                }
                if (state.value.isEmpty()) {
                    item(key = "empty") {
                        Text(stringResource(R.string.chat_notices_empty), color = ink.secondary, fontSize = 14.sp, modifier = Modifier.testTag("chat-notices-empty"))
                    }
                }
                val pinned = state.value.firstOrNull { it.pinned }
                if (pinned != null) {
                    item(key = "pinned") { PinnedNoticeCard(pinned) { onOpen(pinned.id) } }
                }
                items(state.value.filter { !it.pinned }, key = { it.id }) { notice -> NoticeRow(notice) { onOpen(notice.id) } }
            }
            ChatLoad.Failed -> ChatRetryState(
                title = stringResource(R.string.chat_notices_error),
                body = stringResource(R.string.chat_error_body),
                button = stringResource(R.string.chat_error_retry),
                onRetry = viewModel::load,
                modifier = Modifier.padding(top = 120.dp),
                tag = "chat-notices-error",
            )
            else -> Column(Modifier.padding(CrewGutter)) { CrewSkeletonBox(Modifier.fillMaxWidth().height(160.dp), 18.dp) }
        }
    }
}

@Composable
private fun PinnedNoticeCard(notice: ChatNotice, onClick: () -> Unit) {
    val ink = crewInk()
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ink.card).feedbackClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 24.dp).testTag("chat-notices-pinned"),
    ) {
        Text(stringResource(R.string.chat_notices_pinned), color = ink.info, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Text(notice.title, color = ink.text, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (notice.preview.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(notice.preview, color = ink.secondary, fontSize = 13.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.chat_notice_meta, notice.authorName, noticeDay(notice.createdAt)), color = ink.secondary, fontSize = 11.5.sp)
    }
    Spacer(Modifier.height(18.dp))
}

@Composable
private fun NoticeRow(notice: ChatNotice, onClick: () -> Unit) {
    val ink = crewInk()
    Column(Modifier.fillMaxWidth().feedbackClickable(role = Role.Button, onClick = onClick).testTag("chat-notices-row")) {
        Spacer(Modifier.height(18.dp))
        Text(notice.title, color = ink.text, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (notice.preview.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(notice.preview, color = ink.secondary, fontSize = 13.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.chat_notice_meta, notice.authorName, noticeDay(notice.createdAt)), color = ink.secondary, fontSize = 11.5.sp)
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth().height(0.7.dp).background(ink.divider))
    }
}

// ─────────────────────────────────────────────────────────────
// 08 공지
// ─────────────────────────────────────────────────────────────

/** 08 공지 — 제목 · 작성자(지금의 크루장이면 라벨) · 작성 시각 · 전문, 고정 공지면 안내. 크루장은 ••• 로 11 수정 */
@Composable
fun ChatNoticeScreen(viewModel: ChatNoticesViewModel, onBack: () -> Unit, onRoom: () -> Unit, onEdit: (Long) -> Unit, onEnded: () -> Unit) {
    val ink = crewInk()
    val notices by viewModel.notices.collectAsStateWithLifecycle()
    val meta by viewModel.meta.collectAsStateWithLifecycle()
    val ended by viewModel.ended.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.load()
        onPauseOrDispose {}
    }
    LaunchedEffect(ended) { if (ended) onEnded() }
    val notice = (notices as? ChatLoad.Ready)?.value?.firstOrNull { it.id == viewModel.noticeId }
    val owner = meta?.owner == true
    CrewPage(Modifier.testTag("chat-notice")) {
        CrewTopBar(stringResource(R.string.chat_notice_bar), onBack, onMore = if (owner && notice != null) ({ onEdit(notice.id) }) else null)
        when {
            notice != null -> NoticeBody(notice, meta, Modifier.weight(1f))
            notices is ChatLoad.Ready -> Column(Modifier.weight(1f).padding(horizontal = CrewGutter)) {
                Spacer(Modifier.height(80.dp))
                Text(stringResource(R.string.chat_notice_missing), color = ink.secondary, fontSize = 15.sp, modifier = Modifier.testTag("chat-notice-missing"))
            }
            notices is ChatLoad.Failed -> ChatRetryState(
                title = stringResource(R.string.chat_notices_error),
                body = stringResource(R.string.chat_error_body),
                button = stringResource(R.string.chat_error_retry),
                onRetry = viewModel::load,
                modifier = Modifier.weight(1f).padding(top = 120.dp),
                tag = "chat-notice-error",
            )
            else -> Column(Modifier.weight(1f).padding(CrewGutter)) { CrewSkeletonBox(Modifier.fillMaxWidth().height(200.dp), 16.dp) }
        }
        CrewBottomBar { CrewButton(stringResource(R.string.chat_back_to_room), onRoom, Modifier.testTag("chat-notice-room")) }
    }
}

@Composable
private fun NoticeBody(notice: ChatNotice, meta: ChatRoomMeta?, modifier: Modifier) {
    val ink = crewInk()
    val authorOwner = notice.authorId != null && notice.authorId == meta?.ownerId
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = CrewGutter)) {
        Spacer(Modifier.height(36.dp))
        Text(notice.title, color = ink.text, fontSize = 28.sp, lineHeight = 35.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("chat-notice-title"))
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChatFace(notice.authorName, authorOwner, 32.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(notice.authorName, color = ink.text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                    if (authorOwner) {
                        Spacer(Modifier.width(10.dp))
                        ChatOwnerBadge()
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.chat_notice_meta, noticeDay(notice.createdAt), chatTime(notice.createdAt)),
                    color = ink.secondary, fontSize = 11.5.sp,
                )
            }
        }
        Spacer(Modifier.height(34.dp))
        Text(notice.body, color = ink.text, fontSize = 15.sp, lineHeight = 25.sp, modifier = Modifier.testTag("chat-notice-body"))
        if (notice.pinned) {
            Spacer(Modifier.height(40.dp))
            Box(
                Modifier.fillMaxWidth().heightIn(min = 66.dp).clip(RoundedCornerShape(14.dp)).background(ink.card).padding(horizontal = 22.dp, vertical = 18.dp)
                    .testTag("chat-notice-pinned-note"),
                contentAlignment = Alignment.CenterStart,
            ) { Text(stringResource(R.string.chat_notice_pinned_note), color = ink.info, fontSize = 13.5.sp) }
        }
        Spacer(Modifier.height(32.dp))
    }
}

// ─────────────────────────────────────────────────────────────
// 10 등록 · 11 수정 · 12 삭제 · 43 저장 실패 · 44 초안
// ─────────────────────────────────────────────────────────────

/**
 * 10 공지 등록 · 11 수정(같은 구조, 그 공지의 내용을 그대로) — 서버가 받은 뒤에만 대화로 돌아간다(13). 실패하면 쓴 내용과
 * 기존 공지 · 고정이 그대로(43). 바뀐 채 나가려 하면 44(초안으로 남기기 — 쓴 사람 · 방 · 새 공지/수정마다).
 */
@Composable
fun ChatNoticeEditScreen(
    viewModel: ChatNoticeEditViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onOwnerLost: () -> Unit,
    onEnded: () -> Unit,
) {
    val ink = crewInk()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val load by viewModel.load.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val sheet by viewModel.sheet.collectAsStateWithLifecycle()
    val done by viewModel.done.collectAsStateWithLifecycle()
    val ended by viewModel.ended.collectAsStateWithLifecycle()
    val ownerLost by viewModel.ownerLost.collectAsStateWithLifecycle()
    LaunchedEffect(done) { if (done) onDone() }
    LaunchedEffect(ended) { if (ended) onEnded() }
    LaunchedEffect(ownerLost) { if (ownerLost) onOwnerLost() }
    val edit = viewModel.noticeId != null
    BackHandler(enabled = !saving) { viewModel.back(onBack) }

    CrewPage(Modifier.imePadding().testTag(if (edit) "chat-notice-edit" else "chat-notice-write")) {
        CrewTopBar(stringResource(if (edit) R.string.chat_edit_bar else R.string.chat_write_bar), { if (!saving) viewModel.back(onBack) })
        when (load) {
            is ChatLoad.Ready -> {
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = CrewGutter)) {
                    Spacer(Modifier.height(38.dp))
                    CrewFieldLabel(stringResource(R.string.chat_notice_title_label))
                    Spacer(Modifier.height(12.dp))
                    CrewTextField(
                        form.title, viewModel::setTitle, Modifier.testTag("chat-notice-title-field"),
                        placeholder = stringResource(R.string.chat_notice_title_hint),
                    )
                    Spacer(Modifier.height(34.dp))
                    CrewFieldLabel(stringResource(R.string.chat_notice_body_label))
                    Spacer(Modifier.height(12.dp))
                    CrewTextField(
                        form.body, viewModel::setBody, Modifier.testTag("chat-notice-body-field"),
                        placeholder = stringResource(R.string.chat_notice_body_hint), singleLine = false, minHeight = 220.dp,
                    )
                    Spacer(Modifier.height(30.dp))
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.chat_notice_pin), color = ink.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Switch(
                            checked = form.pinned,
                            onCheckedChange = viewModel::setPinned,
                            modifier = Modifier.testTag("chat-notice-pin"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White, checkedTrackColor = ink.tabMark, checkedBorderColor = ink.tabMark,
                                uncheckedThumbColor = ink.secondary, uncheckedTrackColor = ink.choice, uncheckedBorderColor = ink.divider,
                            ),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.chat_notice_pin_note), color = ink.secondary, fontSize = 12.sp)
                    if (edit) {
                        Spacer(Modifier.height(40.dp))
                        Box(
                            Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(8.dp)).feedbackClickable(role = Role.Button, onClick = viewModel::askDelete)
                                .padding(end = 12.dp).testTag("chat-notice-delete"),
                            contentAlignment = Alignment.CenterStart,
                        ) { Text(stringResource(R.string.chat_notice_delete), color = ink.warn, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                    }
                    Spacer(Modifier.height(32.dp))
                }
                CrewBottomBar {
                    CrewButton(
                        stringResource(if (edit) R.string.chat_notice_save else R.string.chat_notice_publish), viewModel::save,
                        Modifier.testTag("chat-notice-save"),
                        kind = if (form.ready && (!edit || form.changed)) CrewButtonKind.PRIMARY else CrewButtonKind.DISABLED,
                        busy = saving && sheet == null,
                        enabled = form.ready && (!edit || form.changed),
                    )
                }
            }
            ChatLoad.Failed -> Column(Modifier.weight(1f).padding(horizontal = CrewGutter)) {
                Spacer(Modifier.height(80.dp))
                Text(stringResource(R.string.chat_notice_missing), color = ink.secondary, fontSize = 15.sp, modifier = Modifier.testTag("chat-notice-missing"))
            }
            else -> Column(Modifier.weight(1f).padding(CrewGutter)) { CrewSkeletonBox(Modifier.fillMaxWidth().height(260.dp), 16.dp) }
        }
    }

    when (val current = sheet) {
        null -> Unit
        is ChatNoticeSheet.SaveFailed -> CrewSheet(
            stringResource(R.string.chat_notice_error_title), viewModel::closeSheet, Modifier.testTag("chat-notice-failed"), dismissible = !current.busy,
        ) {
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.chat_notice_error_body), color = ink.secondary, fontSize = 15.sp, lineHeight = 25.sp)
            Spacer(Modifier.height(96.dp))
            CrewButton(stringResource(R.string.chat_notice_retry), viewModel::save, Modifier.testTag("chat-notice-retry"), busy = current.busy || saving)
        }
        ChatNoticeSheet.KeepDraft -> CrewSheet(stringResource(R.string.chat_notice_draft_title), viewModel::closeSheet, Modifier.testTag("chat-notice-draft")) {
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.chat_notice_draft_body), color = ink.secondary, fontSize = 15.sp, lineHeight = 25.sp)
            Spacer(Modifier.height(96.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                CrewButton(stringResource(R.string.chat_notice_continue), viewModel::closeSheet, Modifier.weight(1f).testTag("chat-notice-continue"), CrewButtonKind.SECONDARY)
                CrewButton(stringResource(R.string.chat_notice_keep), { viewModel.keepDraft(onBack) }, Modifier.weight(1f).testTag("chat-notice-keep"))
            }
        }
        is ChatNoticeSheet.Delete -> CrewConfirmSheet(
            title = stringResource(R.string.chat_notice_delete_title),
            body = stringResource(R.string.chat_notice_delete_body),
            confirm = stringResource(R.string.chat_notice_delete),
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::closeSheet,
            busy = current.state.busy,
            danger = true,
            tag = "chat-notice-delete-confirm",
            error = current.state.error?.let { chatConfirmErrorText(it) },
        )
    }
}
