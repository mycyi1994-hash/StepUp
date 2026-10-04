package com.stepup.android.ui.screens.community.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.stepup.android.ui.components.RunSpinner
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
import com.stepup.android.ui.theme.StepUpSans

/**
 * 22 대화 검색 · 23 결과 없음 — 이 방에서 지금 볼 수 있는 메시지만(지웠거나 숨긴 것 · 참여 전 방은 서버가 빼고 준다).
 * 결과를 누르면 대화의 그 메시지로 옮겨 잠깐 강조한다. 그사이 지워졌으면 대화에서 "지금은 볼 수 없는 메시지"라고 알린다.
 */
@Composable
fun ChatSearchScreen(viewModel: ChatSearchViewModel, onBack: () -> Unit, onOpen: (messageId: Long, seq: Long) -> Unit, onEnded: () -> Unit) {
    val ink = blueInk()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val meta by viewModel.meta.collectAsStateWithLifecycle()
    val ended by viewModel.ended.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var field by remember { mutableStateOf(TextFieldValue(query, TextRange(query.length))) }
    LaunchedEffect(ended) { if (ended) onEnded() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    BluePage(Modifier.imePadding().testTag("chat-search")) {
        BlueTopBar(stringResource(R.string.chat_search_bar), onBack)
        val fieldShape = RoundedCornerShape(16.dp)
        Row(
            Modifier.padding(horizontal = BlueGutter).padding(top = 10.dp).fillMaxWidth().heightIn(min = 56.dp)
                .clip(fieldShape).background(Brush.verticalGradient(listOf(ink.cardTop, ink.card)), fieldShape)
                .border(1.5.dp, ink.edgeStrong, fieldShape).padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Search, null, tint = ink.info, modifier = Modifier.size(26.dp))
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
                textStyle = blueText(17.sp, ink.text, FontWeight.SemiBold, 1.3f),
                cursorBrush = SolidColor(ink.info),
                modifier = Modifier.weight(1f).padding(vertical = 14.dp).focusRequester(focus).testTag("chat-search-field"),
                decorationBox = { inner ->
                    Box {
                        if (field.text.isEmpty()) Text(stringResource(R.string.chat_search_hint), style = blueText(17.sp, ink.secondary, FontWeight.Medium, 1.3f))
                        inner()
                    }
                },
            )
            // 22 — 지우기 X 는 검색어만 비운다(기존 검색 상태를 그대로 쓴다)
            if (field.text.isNotEmpty()) {
                BlueClearButton(
                    stringResource(R.string.crew_blue_clear_query),
                    onClear = {
                        field = TextFieldValue("")
                        viewModel.setQuery("")
                        runCatching { focus.requestFocus() }
                    },
                    modifier = Modifier.testTag("chat-search-clear"),
                )
            }
        }
        when (val current = state) {
            ChatSearchState.Idle -> Unit
            ChatSearchState.Searching -> Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                RunSpinner(Modifier.size(26.dp))
            }
            is ChatSearchState.Found -> LazyColumn(
                Modifier.weight(1f).fillMaxWidth().testTag("chat-search-results"),
                contentPadding = PaddingValues(start = BlueGutter, end = BlueGutter, bottom = 40.dp),
            ) {
                item(key = "count") {
                    Text(
                        stringResource(R.string.chat_search_count, current.results.size), style = blueText(15.sp, ink.secondary, FontWeight.Medium),
                        modifier = Modifier.padding(top = 24.dp, bottom = 6.dp),
                    )
                }
                items(current.results, key = { it.id ?: 0L }) { message ->
                    ChatSearchRow(message, meta, query) {
                        val id = message.id
                        val seq = message.seq
                        if (id != null && seq != null) onOpen(id, seq)
                    }
                }
            }
            ChatSearchState.Empty -> Column(Modifier.padding(horizontal = BlueGutter).testTag("chat-search-empty")) {
                Spacer(Modifier.height(64.dp))
                Text(stringResource(R.string.chat_search_empty_title), style = blueText(27.sp, ink.text, FontWeight.ExtraBold, 1.3f))
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.chat_search_empty_body), style = blueText(16.sp, ink.secondary, FontWeight.Medium))
                Spacer(Modifier.height(26.dp))
                BlueButton(
                    stringResource(R.string.chat_search_change),
                    {
                        field = field.copy(selection = TextRange(0, field.text.length))
                        runCatching { focus.requestFocus() }
                        keyboard?.show()
                    },
                    Modifier.testTag("chat-search-change"),
                )
            }
            ChatSearchState.Failed -> Column(Modifier.padding(horizontal = BlueGutter).padding(top = 40.dp).testTag("chat-search-failed")) {
                Text(stringResource(R.string.chat_search_error), style = blueText(16.sp, ink.warn, FontWeight.SemiBold))
                Spacer(Modifier.height(24.dp))
                BlueButton(stringResource(R.string.chat_error_retry), viewModel::search, Modifier.testTag("chat-search-retry"))
            }
        }
    }
}

@Composable
private fun ChatSearchRow(message: ChatMessage, meta: ChatRoomMeta?, query: String, onClick: () -> Unit) {
    val ink = blueInk()
    val owner = message.authorId != null && message.authorId == meta?.ownerId
    val body = message.body?.takeIf { it.isNotBlank() } ?: if (message.kind == ChatKind.IMAGE) stringResource(R.string.chat_photo) else ""
    Column(Modifier.fillMaxWidth().feedbackClickable(role = Role.Button, onClick = onClick).testTag("chat-search-row")) {
        Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            ChatFace(message.authorName, owner, 50.dp)
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        message.authorName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.chat_unknown_name),
                        style = blueText(17.sp, ink.text, FontWeight.Bold, 1.3f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (owner) {
                        Spacer(Modifier.width(10.dp))
                        ChatOwnerBadge()
                    }
                }
                Spacer(Modifier.height(6.dp))
                // 22 — 검색어를 청록으로 강조(새 API 없이 받은 본문에서 찾는다)
                Text(
                    highlightQuery(body, query, ink.info), style = blueText(16.sp, ink.text, FontWeight.Medium, 1.45f),
                    maxLines = 3, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.chat_notice_meta, chatDate(message.createdAt), chatTime(message.createdAt)),
                    style = blueText(14.sp, ink.secondary, FontWeight.Medium),
                )
            }
            Icon(Icons.Filled.ChevronRight, null, tint = ink.secondary, modifier = Modifier.padding(start = 8.dp).size(24.dp))
        }
        BlueDivider()
    }
}

/** 본문에서 검색어(대소문자 무시)를 찾아 그 부분만 강조 */
internal fun highlightQuery(text: String, query: String, color: Color): AnnotatedString {
    val needle = query.trim()
    if (needle.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text)
        var from = 0
        while (true) {
            val at = text.indexOf(needle, from, ignoreCase = true)
            if (at < 0) break
            addStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold), at, at + needle.length)
            from = at + needle.length
        }
    }
}
