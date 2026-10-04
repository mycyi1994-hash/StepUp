package com.stepup.android.ui.screens.community.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.data.repo.ChatRoomsState
import com.stepup.android.domain.ChatPreview
import com.stepup.android.domain.ChatRole
import com.stepup.android.domain.ChatRoomSummary
import com.stepup.android.domain.ChatRules
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.community.crew.CrewImage
import com.stepup.android.ui.screens.community.crew.CrewStartSheets
import com.stepup.android.ui.screens.community.crew.SHEET_DRAFT_DISCARD
import com.stepup.android.ui.screens.community.crew.SHEET_DRAFT_RESUME
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 01 · 31 에서 나가는 곳 */
class ChatListActions(
    val onOpenRoom: (String) -> Unit,
    /** 31 크루 찾아보기 — 크루 모집 글자 탭 */
    val onFindCrews: () -> Unit,
    /** 31 내 크루 만들기 — resume 이면 남긴 만들기 초안을 이어 쓴다 */
    val onCreate: (resume: Boolean) -> Unit,
)

/**
 * 커뮤니티 세 번째 글자 탭 — 내 크루 대화(01). 지금 가입한 실제 크루의 방만 보인다(시안의 두 방은 예시다).
 * 방이 없으면 31, 혼자 만든 새 크루는 "새 크루"와 첫 인사 안내. 목록이 보이는 동안만 미확인 수 · 마지막 메시지를 다시 읽는다.
 */
@Composable
fun ChatListTab(actions: ChatListActions, viewModel: ChatListViewModel = viewModel(factory = ChatListViewModel.Factory)) {
    val ink = blueInk()
    val rooms by viewModel.rooms.collectAsStateWithLifecycle()
    val draft by viewModel.createDraft.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf("") }
    LifecycleResumeEffect(viewModel) {
        viewModel.setActive(true)
        onPauseOrDispose { viewModel.setActive(false) }
    }
    val create: () -> Unit = { if (draft != null) sheet = SHEET_DRAFT_RESUME else actions.onCreate(false) }

    Box(Modifier.fillMaxSize().testTag("chat-list")) {
        when (val state = rooms) {
            ChatRoomsState.SignIn -> BlueEmptyState(
                icon = { BlueStateIcon(Icons.Filled.Search) },
                title = stringResource(R.string.chat_signin_title),
                body = stringResource(R.string.chat_signin_body),
                modifier = Modifier.padding(top = 72.dp).testTag("chat-list-signin"),
            ) { com.stepup.android.ui.components.SignInAgainButton() }
            ChatRoomsState.Loading -> Column(Modifier.fillMaxWidth().padding(horizontal = BlueGutter).testTag("chat-list-loading")) {
                ChatListHeading()
                repeat(2) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        BlueSkeleton(Modifier.size(78.dp), 16.dp)
                        Column(Modifier.weight(1f).padding(start = 16.dp)) {
                            BlueSkeleton(Modifier.width(120.dp).height(20.dp))
                            Spacer(Modifier.height(10.dp))
                            BlueSkeleton(Modifier.fillMaxWidth().height(14.dp))
                        }
                    }
                }
            }
            is ChatRoomsState.Failed -> BlueEmptyState(
                icon = { BlueStateIcon(Icons.Filled.Refresh, ring = false) },
                title = stringResource(R.string.chat_list_error),
                body = stringResource(R.string.chat_error_body),
                modifier = Modifier.padding(top = 72.dp).testTag("chat-list-error"),
            ) { BlueButton(stringResource(R.string.chat_error_retry), viewModel::retry, Modifier.testTag("chat-list-retry")) }
            is ChatRoomsState.Ready -> if (state.rooms.isEmpty()) {
                ChatNoCrew(onFind = actions.onFindCrews, onCreate = create)
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().testTag("chat-list-scroll"),
                    contentPadding = PaddingValues(start = BlueGutter, end = BlueGutter, bottom = 40.dp),
                ) {
                    item(key = "heading") { ChatListHeading() }
                    items(state.rooms, key = { it.crewId }) { room ->
                        ChatRoomRow(room) { actions.onOpenRoom(room.crewId) }
                    }
                    item(key = "footer") {
                        Text(
                            stringResource(R.string.chat_list_footer), style = blueText(15.sp, ink.secondary, FontWeight.Medium),
                            modifier = Modifier.padding(top = 40.dp).testTag("chat-list-footer"),
                        )
                    }
                }
            }
        }
    }
    if (sheet == SHEET_DRAFT_RESUME || sheet == SHEET_DRAFT_DISCARD) {
        CrewStartSheets(draft, discard = sheet == SHEET_DRAFT_DISCARD, onStep = { sheet = it }, onCreate = actions.onCreate)
    }
}

@Composable
private fun ChatListHeading() {
    val ink = blueInk()
    Column(Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 22.dp)) {
        Text(
            stringResource(R.string.chat_list_title), style = blueText(30.sp, ink.text, FontWeight.ExtraBold, 1.25f),
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.chat_list_sub), style = blueText(16.sp, ink.secondary, FontWeight.Medium))
    }
}

/** 방 한 줄 — 대표 이미지 · 크루 이름 · "크루원 · 25명" · 마지막 메시지 · 시각 · 미확인 수 */
@Composable
private fun ChatRoomRow(room: ChatRoomSummary, onClick: () -> Unit) {
    val ink = blueInk()
    Column(
        Modifier.fillMaxWidth().heightIn(min = 112.dp).feedbackClickable(role = Role.Button, onClick = onClick)
            .testTag("chat-row-${room.crewId}"),
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.Top) {
            CrewImage(room.card, 78.dp, 16.dp)
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(room.name, style = blueText(22.sp, ink.text, FontWeight.ExtraBold, 1.25f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(if (room.role == ChatRole.OWNER) R.string.chat_list_role_owner else R.string.chat_list_role_member, room.memberCount),
                            style = blueText(15.sp, ink.info, FontWeight.Bold), maxLines = 1,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp, top = 2.dp)) {
                        val last = room.last
                        Text(
                            if (last == null) stringResource(R.string.chat_list_new) else chatListTime(last.createdAt),
                            style = blueText(13.5.sp, ink.secondary, FontWeight.Medium), maxLines = 1,
                        )
                        if (room.unread > 0) {
                            Spacer(Modifier.height(8.dp))
                            ChatUnreadBadge(room.unread)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    chatPreviewText(room), style = blueText(15.sp, ink.text.copy(alpha = 0.86f), FontWeight.Medium), maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("chat-row-preview"),
                )
            }
        }
        BlueDivider()
    }
}

/** 목록 미리보기 한 줄 — 지웠거나 숨긴 메시지는 그 상태로(내용을 다시 보이지 않는다) */
@Composable
private fun chatPreviewText(room: ChatRoomSummary): String = when (val preview = ChatRules.preview(room.last)) {
    ChatPreview.Empty -> stringResource(R.string.chat_list_empty_line)
    is ChatPreview.Text -> stringResource(R.string.chat_preview_text, preview.author.ifBlank { stringResource(R.string.chat_unknown_name) }, preview.body)
    is ChatPreview.Photo -> stringResource(R.string.chat_preview_photo, preview.author.ifBlank { stringResource(R.string.chat_unknown_name) })
    ChatPreview.Deleted -> stringResource(R.string.chat_deleted)
    ChatPreview.Hidden -> stringResource(R.string.chat_hidden)
    is ChatPreview.Notice -> stringResource(R.string.chat_line_notice, preview.name.ifBlank { stringResource(R.string.chat_unknown_name) })
    is ChatPreview.Left -> stringResource(R.string.chat_line_left, preview.name.ifBlank { stringResource(R.string.chat_unknown_name) })
}

/** 오늘이면 "19:12", 올해면 "9월 28일", 그 전이면 "2025. 9. 28." */
internal fun chatListTime(millis: Long, now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): String {
    val at = Instant.ofEpochMilli(millis).atZone(zone)
    val today = Instant.ofEpochMilli(now).atZone(zone)
    return when {
        at.toLocalDate() == today.toLocalDate() -> chatTime(millis, zone)
        at.year == today.year -> chatDate(millis, locale, zone)
        else -> at.format(DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(locale, "yMd"), locale))
    }
}

/** 31 가입한 크루가 없다 — 크루 찾아보기(크루 모집) · 내 크루 만들기 */
@Composable
private fun ChatNoCrew(onFind: () -> Unit, onCreate: () -> Unit) {
    val ink = blueInk()
    // 가운데 놓되, 큰 글씨 · 작은 화면에서는 넘겨 본다
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight).padding(horizontal = BlueGutter)
            .testTag("chat-none"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(40.dp))
        Text(
            stringResource(R.string.chat_none_title), style = blueText(30.sp, ink.text, FontWeight.ExtraBold, 1.3f), textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.chat_none_body), style = blueText(16.sp, ink.secondary, FontWeight.Medium, 1.6f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Column(Modifier.widthIn(max = 360.dp)) {
            BlueButton(stringResource(R.string.chat_none_find), onFind, Modifier.testTag("chat-none-find"))
            Spacer(Modifier.height(12.dp))
            BlueButton(stringResource(R.string.chat_none_create), onCreate, Modifier.testTag("chat-none-create"), BlueKind.SECONDARY)
        }
        Spacer(Modifier.height(40.dp))
    }
    }
}
