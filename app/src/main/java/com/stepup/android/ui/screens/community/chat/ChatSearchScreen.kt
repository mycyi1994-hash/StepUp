package com.stepup.android.ui.screens.community.chat

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.domain.ChatKind
import com.stepup.android.domain.ChatMessage
import com.stepup.android.domain.ChatRoomMeta
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.community.crew.CrewButton
import com.stepup.android.ui.screens.community.crew.CrewGutter
import com.stepup.android.ui.screens.community.crew.CrewPage
import com.stepup.android.ui.screens.community.crew.CrewTopBar
import com.stepup.android.ui.screens.community.crew.crewInk
import com.stepup.android.ui.theme.StepUpSans

/**
 * 22 대화 검색 · 23 결과 없음 — 이 방에서 지금 볼 수 있는 메시지만(지웠거나 숨긴 것 · 참여 전 방은 서버가 빼고 준다).
 * 결과를 누르면 대화의 그 메시지로 옮겨 잠깐 강조한다. 그사이 지워졌으면 대화에서 "지금은 볼 수 없는 메시지"라고 알린다.
 */
@Composable
fun ChatSearchScreen(viewModel: ChatSearchViewModel, onBack: () -> Unit, onOpen: (messageId: Long, seq: Long) -> Unit, onEnded: () -> Unit) {
    val ink = crewInk()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val meta by viewModel.meta.collectAsStateWithLifecycle()
    val ended by viewModel.ended.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var field by remember { mutableStateOf(TextFieldValue(query, TextRange(query.length))) }
    LaunchedEffect(ended) { if (ended) onEnded() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    CrewPage(Modifier.imePadding().testTag("chat-search")) {
        CrewTopBar(stringResource(R.string.chat_search_bar), onBack)
        Row(
            Modifier.padding(horizontal = CrewGutter).padding(top = 22.dp).fillMaxWidth().heightIn(min = 51.dp)
                .clip(RoundedCornerShape(14.dp)).background(ink.card).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Search, null, tint = ink.secondary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = field,
                onValueChange = { next ->
                    field = next
                    viewModel.setQuery(next.text)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    viewModel.search()
                    keyboard?.hide()
                }),
                textStyle = TextStyle(fontFamily = StepUpSans, color = ink.text, fontSize = 15.sp),
                cursorBrush = SolidColor(ink.info),
                modifier = Modifier.weight(1f).padding(vertical = 14.dp).focusRequester(focus).testTag("chat-search-field"),
                decorationBox = { inner ->
                    Box {
                        if (field.text.isEmpty()) Text(stringResource(R.string.chat_search_hint), color = ink.secondary, fontSize = 15.sp)
                        inner()
                    }
                },
            )
        }
        when (val current = state) {
            ChatSearchState.Idle -> Unit
            ChatSearchState.Searching -> Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(22.dp), color = ink.secondary, strokeWidth = 2.dp)
            }
            is ChatSearchState.Found -> LazyColumn(
                Modifier.weight(1f).fillMaxWidth().testTag("chat-search-results"),
                contentPadding = PaddingValues(start = CrewGutter, end = CrewGutter, bottom = 40.dp),
            ) {
                item(key = "count") {
                    Text(
                        stringResource(R.string.chat_search_count, current.results.size), color = ink.secondary, fontSize = 13.5.sp,
                        modifier = Modifier.padding(top = 28.dp, bottom = 14.dp),
                    )
                }
                items(current.results, key = { it.id ?: 0L }) { message ->
                    ChatSearchRow(message, meta) {
                        val id = message.id
                        val seq = message.seq
                        if (id != null && seq != null) onOpen(id, seq)
                    }
                }
            }
            ChatSearchState.Empty -> Column(Modifier.padding(horizontal = CrewGutter).testTag("chat-search-empty")) {
                Spacer(Modifier.height(96.dp))
                Text(stringResource(R.string.chat_search_empty_title), color = ink.text, fontSize = 26.sp, lineHeight = 33.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(18.dp))
                Text(stringResource(R.string.chat_search_empty_body), color = ink.secondary, fontSize = 14.sp)
                Spacer(Modifier.height(40.dp))
                CrewButton(
                    stringResource(R.string.chat_search_change),
                    {
                        field = field.copy(selection = TextRange(0, field.text.length))
                        runCatching { focus.requestFocus() }
                        keyboard?.show()
                    },
                    Modifier.testTag("chat-search-change"),
                )
            }
            ChatSearchState.Failed -> Column(Modifier.padding(horizontal = CrewGutter).padding(top = 40.dp).testTag("chat-search-failed")) {
                Text(stringResource(R.string.chat_search_error), color = ink.warn, fontSize = 14.sp)
                Spacer(Modifier.height(24.dp))
                CrewButton(stringResource(R.string.chat_error_retry), viewModel::search, Modifier.testTag("chat-search-retry"))
            }
        }
    }
}

@Composable
private fun ChatSearchRow(message: ChatMessage, meta: ChatRoomMeta?, onClick: () -> Unit) {
    val ink = crewInk()
    val owner = message.authorId != null && message.authorId == meta?.ownerId
    Row(
        Modifier.fillMaxWidth().feedbackClickable(role = Role.Button, onClick = onClick).padding(top = 18.dp).testTag("chat-search-row"),
        verticalAlignment = Alignment.Top,
    ) {
        ChatFace(message.authorName, owner, 32.dp)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    message.authorName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.chat_unknown_name),
                    color = ink.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (owner) {
                    Spacer(Modifier.width(10.dp))
                    ChatOwnerBadge()
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                message.body?.takeIf { it.isNotBlank() } ?: if (message.kind == ChatKind.IMAGE) stringResource(R.string.chat_photo) else "",
                color = ink.text, fontSize = 15.sp, lineHeight = 23.sp, maxLines = 3, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.chat_notice_meta, chatDate(message.createdAt), chatTime(message.createdAt)),
                color = ink.secondary, fontSize = 11.5.sp,
            )
            Spacer(Modifier.height(22.dp))
            Box(Modifier.fillMaxWidth().height(0.7.dp).background(ink.divider))
        }
    }
}
