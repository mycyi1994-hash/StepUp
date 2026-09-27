package com.stepup.android.ui.screens.community.stories

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.domain.Comment
import com.stepup.android.data.repo.StoryNotice
import com.stepup.android.domain.Post
import com.stepup.android.domain.StoryReportReason
import com.stepup.android.domain.StoryText
import com.stepup.android.domain.storyPlace
import com.stepup.android.ui.components.DarkIconButton
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.community.relativeTime
import com.stepup.android.ui.theme.Alert
import com.stepup.android.ui.theme.Carbon
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.StepUpDesign
import com.stepup.android.ui.theme.VoltText
import kotlinx.coroutines.delay

/** 글 상세 — 글쓴이 → 제목 · 본문 → 장소 카드 → 반응 → 댓글, 아래에 댓글 입력 */
@Composable
fun StoryDetailScreen(
    onBack: () -> Unit,
    onOpenPlace: (com.stepup.android.domain.StoryPlace) -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: StoryDetailViewModel = viewModel(factory = StoryDetailViewModel.Factory),
) {
    val post by viewModel.post.collectAsStateWithLifecycle()
    val missing by viewModel.missing.collectAsStateWithLifecycle()
    val offline by viewModel.offline.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val sheet by viewModel.sheet.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val origin = rememberStoryOrigin()
    var draft by rememberSaveable { mutableStateOf("") }

    // 창이 열려 있으면 뒤로 가기는 창부터 닫는다
    BackHandler(enabled = sheet != StoryDetailSheet.NONE) { viewModel.closeSheet() }
    // 고친 글의 "수정했어요"는 이 화면이 보여 준다 — 목록으로 돌아가서 한 번 더 뜨지 않게 나갈 때 거둔다
    DisposableEffect(Unit) { onDispose { viewModel.consumeNotice() } }

    Column(Modifier.fillMaxSize().imePadding().testTag("story-detail")) {
        StoryHeader(
            title = stringResource(R.string.story_detail_title),
            onBack = onBack,
            trailing = {
                if (post != null) {
                    DarkIconButton(Icons.Filled.MoreHoriz, stringResource(R.string.story_menu), onClick = viewModel::openMenu,
                        modifier = Modifier.testTag("story-menu"))
                } else {
                    Spacer(Modifier.size(StepUpDesign.TouchTarget))
                }
            },
        )
        val current = post
        if (current == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (missing) {
                    StoryStateBlock(Icons.AutoMirrored.Outlined.Article, stringResource(R.string.story_missing_title),
                        stringResource(R.string.story_missing_body)) {
                        StoryButton(stringResource(R.string.story_back_to_list), onBack, Modifier.padding(horizontal = StoryFormGutter))
                    }
                } else if (offline) {
                    StoryStateBlock(Icons.Filled.Refresh, stringResource(R.string.story_error_title),
                        stringResource(R.string.story_error_body)) {
                        StoryButton(stringResource(R.string.story_retry), viewModel::loadIfNeeded,
                            Modifier.padding(horizontal = StoryFormGutter))
                    }
                } else {
                    StorySkeletonRow(Modifier.padding(horizontal = StoryFormGutter))
                }
            }
        } else {
            val place = current.storyPlace
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth().testTag("story-detail-list"),
                contentPadding = PaddingValues(start = StoryFormGutter, end = StoryFormGutter, top = 8.dp, bottom = 16.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StoryAvatar(storyAuthor(current), size = 32.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                listOfNotNull(storyAuthor(current), relativeTime(current.createdAt),
                                    if (current.mine) stringResource(R.string.story_mine_tag) else null).joinToString(" · "),
                                color = Snow, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                            )
                            if (place != null) {
                                val away = origin?.let { storyDistance(com.stepup.android.domain.haversineMeters(it.point, place.point)) }.orEmpty()
                                Text(listOf(place.name, away).filter { it.isNotEmpty() }.joinToString(" · "),
                                    color = Slate, fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    // 상세에서는 제목 · 본문을 전부 보인다
                    Text(current.title, color = Snow, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp,
                        modifier = Modifier.testTag("story-detail-title"))
                    val body = StoryText.bodyForDisplay(current.body)
                    if (body.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(body, color = Silver, fontSize = 15.sp, lineHeight = 23.sp)
                    }
                    if (place != null) {
                        Spacer(Modifier.height(18.dp))
                        StoryPlaceCard(place, distanceFrom(origin, place.point), onClick = { onOpenPlace(place) })
                    }
                    Spacer(Modifier.height(8.dp))
                    StoryReactions(current.likes, current.liked, current.commentCount, size = 13, onLike = viewModel::toggleLike)
                    Spacer(Modifier.height(6.dp))
                    StoryDivider()
                    Spacer(Modifier.height(18.dp))
                    // 댓글을 받기 전에는 목록이 알려 준 수, 받은 뒤에는 받은 수(받으면 둘이 같아진다)
                    val commentTotal = if (comments.isEmpty()) current.commentCount else comments.size
                    Text(stringResource(R.string.story_comment_count, commentTotal), color = Snow, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("story-comment-count"))
                    if (commentTotal == 0) {
                        Spacer(Modifier.height(10.dp))
                        Text(stringResource(R.string.story_comment_empty), color = Slate, fontSize = 13.sp)
                    }
                }
                items(comments, key = { it.id }) { comment -> StoryCommentRow(comment) }
            }
            val shown = notice
            if (shown != null && error == null) {
                LaunchedEffect(shown) {
                    delay(3_500)
                    viewModel.consumeNotice()
                }
                StoryToast(
                    text = stringResource(
                        if (shown is StoryNotice.DraftSaved) R.string.story_notice_draft_saved else R.string.story_notice_edited,
                    ),
                    modifier = Modifier.padding(horizontal = StoryFormGutter, vertical = 6.dp),
                    action = stringResource(R.string.common_close),
                    onAction = viewModel::consumeNotice,
                )
            }
            error?.let { kind ->
                LaunchedEffect(kind) {
                    delay(3_500)
                    viewModel.consumeError()
                }
                StoryToast(
                    text = stringResource(
                        when (kind) {
                            StoryDetailError.COMMENT -> R.string.story_error_comment
                            StoryDetailError.LIKE -> R.string.story_error_like
                            StoryDetailError.DELETE -> R.string.story_error_delete
                            StoryDetailError.REPORT -> R.string.story_error_report
                            StoryDetailError.SIGN_IN -> R.string.story_error_sign_in
                        },
                    ),
                    modifier = Modifier.padding(horizontal = StoryFormGutter, vertical = 6.dp),
                    action = stringResource(R.string.common_close),
                    onAction = viewModel::consumeError,
                )
            }
            StoryCommentInput(
                value = draft,
                onValueChange = { next ->
                    draft = if (StoryText.length(next) <= StoryText.COMMENT_MAX_CHARS) next
                    else next.substring(0, next.offsetByCodePoints(0, StoryText.COMMENT_MAX_CHARS))
                },
                busy = busy,
                onSend = { viewModel.sendComment(draft) { draft = "" } },
            )
        }
    }

    val current = post
    when (sheet) {
        StoryDetailSheet.OWN_MENU -> StorySheet(stringResource(R.string.story_own_menu_title), viewModel::closeSheet) {
            StoryMenuRow(Icons.Outlined.Edit, stringResource(R.string.story_menu_edit), Snow, Modifier.testTag("story-menu-edit")) {
                viewModel.closeSheet()
                current?.let { onEdit(it.id) }
            }
            StoryDivider()
            StoryMenuRow(Icons.Outlined.Delete, stringResource(R.string.story_menu_delete), Alert, Modifier.testTag("story-menu-delete")) {
                viewModel.openSheet(StoryDetailSheet.DELETE)
            }
            StoryDivider()
            Spacer(Modifier.height(14.dp))
            StoryButton(stringResource(R.string.common_close), viewModel::closeSheet, style = StoryButtonStyle.SECONDARY)
        }
        StoryDetailSheet.OTHER_MENU -> StorySheet(stringResource(R.string.story_other_menu_title), viewModel::closeSheet) {
            StoryMenuRow(Icons.Outlined.VisibilityOff, stringResource(R.string.story_menu_hide), Snow, Modifier.testTag("story-menu-hide")) {
                viewModel.hide(onBack)
            }
            StoryDivider()
            StoryMenuRow(Icons.Outlined.Flag, stringResource(R.string.story_menu_report), Snow, Modifier.testTag("story-menu-report")) {
                viewModel.openSheet(StoryDetailSheet.REPORT)
            }
            StoryDivider()
            Spacer(Modifier.height(14.dp))
            StoryButton(stringResource(R.string.common_close), viewModel::closeSheet, style = StoryButtonStyle.SECONDARY)
        }
        StoryDetailSheet.DELETE -> StoryConfirmDialog(
            title = stringResource(R.string.story_delete_title),
            body = stringResource(R.string.story_delete_body),
            confirm = stringResource(R.string.story_menu_delete_short),
            onConfirm = { viewModel.delete(onBack) },
            onDismiss = viewModel::closeSheet,
            busy = busy,
        )
        StoryDetailSheet.REPORT -> StoryReportSheet(busy = busy, onSubmit = viewModel::report, onDismiss = viewModel::closeSheet)
        // 신고가 접수되면 확인 후 글로 돌아간다(시안). 목록에서는 이미 빠졌다
        StoryDetailSheet.REPORTED -> StorySheet(title = null, onDismiss = viewModel::closeSheet) {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp).testTag("story-reported"), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(40.dp).clip(CircleShape).border(1.5.dp, VoltText, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = VoltText, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.story_reported_title), color = Snow, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.story_reported_body), color = Silver, fontSize = 13.sp)
                Spacer(Modifier.height(18.dp))
                StoryButton(stringResource(R.string.story_ok), viewModel::closeSheet, Modifier.testTag("story-reported-ok"))
            }
        }
        StoryDetailSheet.NONE -> Unit
    }
}

/** 상세의 장소 카드 — 70 × 63 지도 썸네일 · 장소 이름 · "내 위치에서 300m · 지도 보기" */
@Composable
private fun StoryPlaceCard(place: com.stepup.android.domain.StoryPlace, distance: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(Carbon).border(1.dp, Edge, shape)
            .feedbackClickable(role = Role.Button, onClick = onClick).padding(10.dp)
            .testTag("story-place-card"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StoryPlaceThumb(place, "", Modifier.size(width = 70.dp, height = 63.dp), showName = false)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(place.name, color = Snow, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(
                listOf(distance, stringResource(R.string.story_view_map)).filter { it.isNotEmpty() }.joinToString(" · "),
                color = Slate, fontSize = 12.sp,
            )
        }
        Icon(Icons.AutoMirrored.Filled.CallMade, contentDescription = null, tint = Silver, modifier = Modifier.size(15.dp))
    }
}

@Composable
private fun StoryCommentRow(comment: Comment) {
    val name = if (comment.mine) stringResource(R.string.story_me) else comment.author.ifBlank { stringResource(R.string.story_runner) }
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().padding(top = 14.dp)
            .padding(start = if (comment.isReply) 24.dp else 0.dp)
            .then(if (comment.mine) Modifier.clip(shape).background(CarbonHigh).padding(12.dp) else Modifier)
            .testTag("story-comment-${comment.id}"),
    ) {
        StoryAvatar(name, size = 26.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, color = Snow, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                if (comment.mine) {
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.story_my_comment), color = VoltText, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(comment.body, color = Silver, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

/** 아래에 고정된 댓글 입력 — 공백을 뺀 내용이 있어야 "등록"이 켜진다. 200자까지 */
@Composable
private fun StoryCommentInput(value: String, onValueChange: (String) -> Unit, busy: Boolean, onSend: () -> Unit) {
    val canSend = value.isNotBlank() && !busy
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().padding(start = StoryFormGutter, end = StoryFormGutter, top = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.weight(1f).heightIn(min = 50.dp).clip(shape).background(Carbon)
                .border(1.dp, if (value.isNotEmpty()) VoltText.copy(alpha = 0.6f) else Edge, shape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) Text(stringResource(R.string.story_comment_hint), color = Slate, fontSize = 14.sp)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(color = Snow, fontSize = 14.sp),
                cursorBrush = SolidColor(VoltText),
                maxLines = 4,
                modifier = Modifier.fillMaxWidth().testTag("story-comment-input"),
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(Modifier.width(64.dp)) {
            StoryButton(stringResource(R.string.story_comment_send), onSend, Modifier.testTag("story-comment-send"),
                enabled = canSend, busy = busy)
        }
    }
}

@Composable
private fun StoryMenuRow(icon: ImageVector, label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 54.dp).feedbackClickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = color, fontSize = 15.sp)
    }
}

/** 신고 — 사유 하나를 고른 뒤 접수. 신고한 사람은 글쓴이에게 보이지 않는다(서버 content_reports 는 본인만 읽는다) */
@Composable
private fun StoryReportSheet(busy: Boolean, onSubmit: (StoryReportReason) -> Unit, onDismiss: () -> Unit) {
    var picked by rememberSaveable { mutableStateOf<StoryReportReason?>(null) }
    StorySheet(stringResource(R.string.story_report_title), onDismiss) {
        Text(stringResource(R.string.story_report_body), color = Silver, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
        StoryReportReason.entries.forEach { reason ->
            StoryRadioRow(
                label = stringResource(
                    when (reason) {
                        StoryReportReason.SPAM -> R.string.story_report_spam
                        StoryReportReason.ABUSE -> R.string.story_report_abuse
                        StoryReportReason.PRIVACY -> R.string.story_report_privacy
                        StoryReportReason.OTHER -> R.string.story_report_other
                    },
                ),
                hint = null,
                selected = picked == reason,
                onClick = { picked = reason },
                modifier = Modifier.testTag("story-report-${reason.name}"),
            )
            StoryDivider()
        }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.story_report_private), color = Slate, fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))
        StoryButton(
            stringResource(R.string.story_report_submit),
            { picked?.let(onSubmit) },
            Modifier.testTag("story-report-submit"),
            enabled = picked != null,
            busy = busy,
        )
    }
}
