package com.stepup.android.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.domain.Comment
import com.stepup.android.domain.CommentThread
import com.stepup.android.domain.Post
import com.stepup.android.ui.components.quietClickable
import com.stepup.android.ui.theme.Carbon
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.OnVolt
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt

/**
 * 알림에서 눌러 들어온 댓글 창.
 *
 * 앱 껍데기(MainScaffold)에 붙인다. 게시판 화면 안에 두면 그 화면이 화면에
 * 올라와 있을 때만 열리는데, 알림함은 게시판이 아니라 그 위에 뜬 다른
 * 화면이다. 탭이 바뀌고 게시판이 다시 그려지기까지의 순서에 기대는 대신,
 * 어느 화면에 있든 열리게 한다 — 댓글 창은 어차피 화면 위에 뜨는 창이다.
 */
@Composable
fun FocusedCommentSheetHost() {
    val viewModel: CommunityViewModel = viewModel(factory = CommunityViewModel.Factory)
    val pending by viewModel.commentFocus.collectAsStateWithLifecycle()

    LaunchedEffect(pending) {
        pending?.let { viewModel.openCommentsFocused(it) }
    }

    CommentSheetHost(viewModel)
}

/**
 * 열려 있는 댓글 창을 화면에 붙여 주는 호스트.
 * 게시판·크루 게시판이 같은 뷰모델을 쓰므로 두 화면에서 그대로 재사용한다.
 */
@Composable
fun CommentSheetHost(viewModel: CommunityViewModel) {
    // 신고 창과 결과 알림은 댓글 창이 닫혀 있어도 뜬다 — 글 카드에서도 신고하기 때문이다.
    ReportDialogHost(viewModel)
    BoardNoticeToast(viewModel)

    val openId by viewModel.openCommentsFor.collectAsStateWithLifecycle()
    val posts by viewModel.allPosts.collectAsStateWithLifecycle()
    val focusId by viewModel.focusCommentId.collectAsStateWithLifecycle()
    val id = openId ?: return
    val post = posts.firstOrNull { it.id == id } ?: return
    val stream = remember(id) { viewModel.commentThreads(id) }
    val threads by stream.collectAsStateWithLifecycle(emptyList())

    CommentSheet(
        post = post,
        threads = threads,
        focusCommentId = focusId,
        onSend = { body, parentId -> viewModel.sendComment(id, body, parentId) },
        onDeleteComment = viewModel::deleteComment,
        onReportComment = { viewModel.askReport(it) },
        onDismiss = viewModel::closeComments,
    )
}

/**
 * 댓글 창 — 글 카드의 댓글 아이콘을 누르면 아래에서 올라온다.
 *
 * 답글은 부모 댓글 아래에 한 칸 들여 붙는다. 답글 버튼을 누르면
 * 입력창 위에 "○○님에게 답글" 배너가 붙고, 보내면 대댓글로 저장된다.
 */
@Composable
fun CommentSheet(
    post: Post,
    threads: List<CommentThread>,
    onSend: (body: String, parentId: Long) -> Unit,
    onDeleteComment: (Long) -> Unit,
    onDismiss: () -> Unit,
    onReportComment: (Comment) -> Unit = {},
    /** 알림에서 들어왔다면 그 댓글. 0이면 없음. */
    focusCommentId: Long = 0L,
) {
    var input by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<Comment?>(null) }
    val total = threads.sumOf { it.size }
    val listState = rememberLazyListState()

    // 알림을 눌러 들어왔으면 그 댓글이 보이는 자리까지 내려간다.
    //
    // 댓글이 200개인 글에서 맨 위만 보여주면, 사용자는 알림이 가리킨 댓글을
    // 직접 찾아야 한다. 그러면 알림을 누른 의미가 없다.
    LaunchedEffect(focusCommentId, threads.size) {
        if (focusCommentId == 0L || threads.isEmpty()) return@LaunchedEffect
        val index = threads.indexOfFirst { thread ->
            thread.comment.id == focusCommentId || thread.replies.any { it.id == focusCommentId }
        }
        if (index >= 0) listState.animateScrollToItem(index)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .quietClickable(onDismiss),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.82f)
                    // 시트 본체 탭이 배경으로 새어 나가 창이 닫히지 않게 흡수한다
                    .quietClickable { }
                    .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                    .background(Carbon)
                    .border(
                        1.dp,
                        Edge,
                        RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                    )
                    .imePadding(),
            ) {
                // 손잡이
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .align(Alignment.CenterHorizontally)
                        .size(width = 38.dp, height = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Edge),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Filled.ChatBubbleOutline,
                        contentDescription = null,
                        tint = Volt,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.comments_title, total),
                        style = MaterialTheme.typography.titleSmall,
                        color = Snow,
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, stringResource(R.string.common_close), tint = Silver, modifier = Modifier.size(22.dp))
                    }
                }

                Text(
                    text = post.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Silver,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )

                Spacer(Modifier.size(10.dp))

                if (threads.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            com.stepup.android.ui.components.IconSquare(Icons.Filled.ChatBubbleOutline, size = 56.dp)
                            Text(stringResource(R.string.comments_empty), style = MaterialTheme.typography.bodyLarge,
                                color = Silver, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(threads, key = { it.comment.id }) { thread ->
                            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                CommentRow(
                                    comment = thread.comment,
                                    onReply = { replyTo = thread.comment },
                                    onDelete = { onDeleteComment(thread.comment.id) },
                                    onReport = { onReportComment(thread.comment) },
                                    highlighted = thread.comment.id == focusCommentId,
                                )
                                thread.replies.forEach { reply ->
                                    CommentRow(
                                        comment = reply,
                                        onReply = { replyTo = thread.comment },
                                        onDelete = { onDeleteComment(reply.id) },
                                        onReport = { onReportComment(reply) },
                                        modifier = Modifier.padding(start = 30.dp),
                                        highlighted = reply.id == focusCommentId,
                                    )
                                }
                            }
                        }
                    }
                }

                replyTo?.let { target ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CarbonHigh)
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Reply,
                            contentDescription = null,
                            tint = Volt,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = stringResource(R.string.comments_replying_to, target.author),
                            fontSize = 14.sp,
                            color = Silver,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { replyTo = null }) {
                            Icon(Icons.Filled.Close, stringResource(R.string.common_cancel), tint = Silver, modifier = Modifier.size(22.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    com.stepup.android.ui.components.FormField(
                        label = stringResource(if (replyTo != null) R.string.comments_reply_hint else R.string.comments_hint),
                        value = input, onValueChange = { input = it },
                        modifier = Modifier.weight(1f), singleLine = false, minLines = 1, maxLines = 4,
                    )
                    val canSend = input.isNotBlank()
                    IconButton(
                        enabled = canSend,
                        onClick = {
                            onSend(input.trim(), replyTo?.id ?: 0L)
                            input = ""
                            replyTo = null
                        },
                        modifier = Modifier.size(48.dp).background(if (canSend) Volt else CarbonHigh, CircleShape),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.comments_send),
                            tint = if (canSend) OnVolt else Slate, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentRow(
    comment: Comment,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit,
    modifier: Modifier = Modifier,
    /** 알림이 가리킨 댓글 — 스크롤해 놓고 표시까지 해야 눈에 들어온다 */
    highlighted: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (highlighted) {
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Volt.copy(alpha = 0.10f))
                        .border(1.dp, Volt.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                        .padding(9.dp)
                } else {
                    Modifier
                },
            ),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(
            modifier = Modifier
                .size(if (comment.isReply) 30.dp else 36.dp)
                .clip(CircleShape)
                .background(if (comment.mine) Volt.copy(alpha = 0.18f) else CarbonHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = comment.author.take(1).uppercase(),
                color = if (comment.mine) Volt else Silver,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = comment.author,
                    modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (comment.mine) Volt else Snow,
                )
                Text(
                    text = relativeTime(comment.createdAt),
                    fontSize = 12.sp,
                    color = Slate,
                )
            }
            Text(
                text = comment.body,
                fontSize = 15.sp,
                color = Silver,
                lineHeight = 23.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!comment.isReply) {
                    TextButton(onClick = onReply) { Text(stringResource(R.string.comments_reply), color = Silver) }
                }
                TextButton(onClick = if (comment.mine) onDelete else onReport) {
                    Text(stringResource(if (comment.mine) R.string.post_delete else R.string.report_title), color = Silver)
                }
            }
        }
    }
}
