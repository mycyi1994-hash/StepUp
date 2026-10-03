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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.data.repo.StoryNotice
import com.stepup.android.domain.Comment
import com.stepup.android.domain.StoryComposeRules
import com.stepup.android.domain.StoryPlace
import com.stepup.android.domain.StoryReportReason
import com.stepup.android.domain.StoryRun
import com.stepup.android.domain.StoryText
import com.stepup.android.domain.storyPlace
import com.stepup.android.ui.components.runNumberStyle
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.community.relativeTime
import com.stepup.android.ui.theme.StepUpSans
import kotlinx.coroutines.delay

/**
 * 글 상세(CM08 · CM44 · WRITE13) — 글쓴이 → 제목 · 본문 → 달린 기록(붙였으면, 읽기 전용 공유 요약) → 공개 장소 카드 →
 * 반응 → 댓글, 아래에 댓글 입력. 상태 표본: 불러오는 중(CM42) · 볼 수 없는 글(CM38) · 통신 실패(다시 시도) ·
 * 좋아요 실패(CM39, 본문 안 한 줄) · 댓글 실패(CM11, 입력 위 안내 · 입력 보존) · 내 글 메뉴(CM22) · 다른 글 메뉴(CM26) ·
 * 삭제 확인 · 실패(CM24 · 25) · 신고 사유 · 접수 · 실패(CM27 · 28 · 40).
 */
@Composable
fun StoryDetailScreen(
    onBack: () -> Unit,
    onOpenPlace: (StoryPlace) -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: StoryDetailViewModel = viewModel(factory = StoryDetailViewModel.Factory),
) {
    val t = runTone()
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
    // 삭제 · 신고 실패는 그 시트 안에서 보인다 — 시트를 닫으면 함께 거둔다
    LaunchedEffect(sheet) {
        if (sheet == StoryDetailSheet.NONE && (error == StoryDetailError.DELETE || error == StoryDetailError.REPORT)) {
            viewModel.consumeError()
        }
    }
    // 좋아요 실패는 잠깐 보였다가 사라진다(직전 상태로 이미 되돌아가 있다)
    LaunchedEffect(error) {
        if (error == StoryDetailError.LIKE || error == StoryDetailError.SIGN_IN) {
            delay(3_500)
            viewModel.consumeError()
        }
    }

    Column(Modifier.fillMaxSize().imePadding().testTag("story-detail")) {
        StoryHeader(
            title = stringResource(R.string.story_detail_title),
            onBack = onBack,
            trailing = {
                if (post != null) {
                    val label = stringResource(R.string.story_menu)
                    Box(
                        Modifier.size(48.dp).clip(CircleShape)
                            .feedbackClickable(role = Role.Button, onClick = viewModel::openMenu)
                            .semantics { contentDescription = label }
                            .testTag("story-menu"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.MoreHoriz, contentDescription = null, tint = t.text, modifier = Modifier.size(28.dp))
                    }
                }
            },
        )
        val current = post
        if (current == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                when {
                    missing -> Column(Modifier.padding(horizontal = StoryFormGutter).padding(top = 80.dp)) {
                        StoryStateBlock(
                            Icons.AutoMirrored.Outlined.Article, stringResource(R.string.story_blue_missing_title),
                            stringResource(R.string.story_blue_missing_body), Modifier.testTag("story-missing"),
                        )
                        StoryButton(stringResource(R.string.story_blue_back_to_list), onBack, Modifier.testTag("story-back-to-list"))
                    }
                    offline -> Column(Modifier.padding(horizontal = StoryFormGutter).padding(top = 80.dp)) {
                        StoryStateBlock(
                            Icons.Filled.Refresh, stringResource(R.string.story_blue_detail_error_title),
                            stringResource(R.string.story_error_body), Modifier.testTag("story-detail-error"),
                        )
                        StoryButton(stringResource(R.string.story_retry), viewModel::loadIfNeeded, Modifier.testTag("story-detail-retry"))
                    }
                    else -> StoryDetailSkeleton(Modifier.padding(horizontal = StoryFormGutter))
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
                        StoryAvatar(storyAuthor(current), size = 48.dp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                listOfNotNull(storyAuthor(current), if (current.mine) stringResource(R.string.story_mine_tag) else null)
                                    .joinToString(" · "),
                                style = runTextStyle(17.sp, t.text, FontWeight.Bold),
                            )
                            Text(relativeTime(current.createdAt), style = runTextStyle(14.sp, t.label, FontWeight.Medium))
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    // 상세에서는 제목 · 본문을 전부 보인다
                    Text(
                        current.title,
                        style = TextStyle(
                            fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 1.3.em,
                            letterSpacing = (-0.02).em, color = t.text,
                        ),
                        modifier = Modifier.testTag("story-detail-title"),
                    )
                    val body = StoryText.bodyForDisplay(current.body)
                    if (body.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(body, style = runTextStyle(17.sp, t.label, FontWeight.Medium, 1.5f))
                    }
                    current.run?.let { run ->
                        Spacer(Modifier.height(18.dp))
                        StoryRunCard(run)
                    }
                    if (place != null) {
                        Spacer(Modifier.height(if (current.run != null) 12.dp else 18.dp))
                        StoryPlaceCard(place, distanceFrom(origin, place.point), compact = current.run != null, onClick = { onOpenPlace(place) })
                    }
                    Spacer(Modifier.height(8.dp))
                    StoryReactions(current.likes, current.liked, current.commentCount, size = 17, onLike = viewModel::toggleLike)
                    if (error == StoryDetailError.LIKE) {
                        StoryInlineNotice(stringResource(R.string.story_blue_like_failed), Modifier.padding(bottom = 6.dp), tag = "story-like-error")
                    }
                    Spacer(Modifier.height(6.dp))
                    StoryDivider()
                    Spacer(Modifier.height(20.dp))
                    // 댓글을 받기 전에는 목록이 알려 준 수, 받은 뒤에는 받은 수(받으면 둘이 같아진다)
                    val commentTotal = if (comments.isEmpty()) current.commentCount else comments.size
                    Text(stringResource(R.string.story_comment_count, commentTotal), style = runTextStyle(20.sp, t.text, FontWeight.ExtraBold),
                        modifier = Modifier.testTag("story-comment-count"))
                    if (commentTotal == 0) {
                        Text(
                            stringResource(R.string.story_blue_comment_empty), style = runTextStyle(16.sp, t.label, FontWeight.Medium),
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        )
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
            if (error == StoryDetailError.SIGN_IN) {
                StoryToast(
                    text = stringResource(R.string.story_error_sign_in),
                    modifier = Modifier.padding(horizontal = StoryFormGutter, vertical = 6.dp),
                    action = stringResource(R.string.common_close),
                    onAction = viewModel::consumeError,
                    error = true,
                )
            }
            // 댓글을 올리지 못했다 — 입력창을 가리지 않고 그 위에, 쓴 말은 그대로(CM11)
            if (error == StoryDetailError.COMMENT) {
                Text(
                    stringResource(R.string.story_blue_comment_failed),
                    style = runTextStyle(16.sp, t.dangerText, FontWeight.Bold, 1.45f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = StoryFormGutter, vertical = 8.dp).testTag("story-comment-error"),
                )
            }
            StoryCommentInput(
                value = draft,
                onValueChange = { next ->
                    draft = if (StoryText.length(next) <= StoryText.COMMENT_MAX_CHARS) next
                    else next.substring(0, next.offsetByCodePoints(0, StoryText.COMMENT_MAX_CHARS))
                },
                busy = busy,
                retry = error == StoryDetailError.COMMENT,
                onSend = {
                    if (error == StoryDetailError.COMMENT) viewModel.consumeError()
                    viewModel.sendComment(draft) { draft = "" }
                },
            )
        }
    }

    val current = post
    when (sheet) {
        StoryDetailSheet.OWN_MENU -> StorySheet(stringResource(R.string.story_own_menu_title), viewModel::closeSheet) {
            Spacer(Modifier.height(8.dp))
            StoryMenuRow(Icons.Outlined.Edit, stringResource(R.string.story_menu_edit), t.text, Modifier.testTag("story-menu-edit")) {
                viewModel.closeSheet()
                current?.let { onEdit(it.id) }
            }
            StoryDivider()
            StoryMenuRow(Icons.Outlined.Delete, stringResource(R.string.story_menu_delete), t.dangerText, Modifier.testTag("story-menu-delete")) {
                viewModel.openSheet(StoryDetailSheet.DELETE)
            }
        }
        StoryDetailSheet.OTHER_MENU -> StorySheet(stringResource(R.string.story_other_menu_title), viewModel::closeSheet) {
            Spacer(Modifier.height(8.dp))
            StoryMenuRow(Icons.Outlined.VisibilityOff, stringResource(R.string.story_menu_hide), t.text, Modifier.testTag("story-menu-hide"),
                hint = stringResource(R.string.story_blue_hide_hint)) {
                viewModel.hide(onBack)
            }
            StoryDivider()
            StoryMenuRow(Icons.Outlined.Flag, stringResource(R.string.story_menu_report), t.text, Modifier.testTag("story-menu-report")) {
                viewModel.openSheet(StoryDetailSheet.REPORT)
            }
        }
        StoryDetailSheet.DELETE -> {
            val failed = error == StoryDetailError.DELETE
            StoryConfirmDialog(
                title = stringResource(R.string.story_delete_title),
                body = stringResource(R.string.story_blue_delete_body),
                note = if (failed) null else stringResource(R.string.story_blue_delete_note),
                errorText = if (failed) stringResource(R.string.story_error_delete) else null,
                confirm = stringResource(if (failed) R.string.story_blue_delete_retry else R.string.story_blue_delete_confirm),
                onConfirm = {
                    viewModel.consumeError()
                    viewModel.delete(onBack)
                },
                onDismiss = viewModel::closeSheet,
                busy = busy,
            )
        }
        StoryDetailSheet.REPORT -> StoryReportSheet(
            busy = busy,
            failed = error == StoryDetailError.REPORT,
            onSubmit = { reason ->
                viewModel.consumeError()
                viewModel.report(reason)
            },
            onDismiss = viewModel::closeSheet,
        )
        // 신고가 접수되면 확인 후 글로 돌아간다(시안). 목록에서는 이미 빠졌다. 접수는 제재 · 삭제 완료가 아니다
        StoryDetailSheet.REPORTED -> StorySheet(title = null, onDismiss = viewModel::closeSheet, centered = true) {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp).testTag("story-reported"), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(64.dp).clip(CircleShape).border(3.dp, t.cyan, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = t.cyan, modifier = Modifier.size(34.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.story_blue_reported_title), style = runTextStyle(25.sp, t.text, FontWeight.ExtraBold),
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.story_blue_reported_body), style = runTextStyle(16.sp, t.label, FontWeight.Medium),
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(22.dp))
                StoryButton(stringResource(R.string.story_ok), viewModel::closeSheet, Modifier.testTag("story-reported-ok"))
            }
        }
        StoryDetailSheet.NONE -> Unit
    }
}

/** 상세를 처음 받는 동안(CM42) — 글쓴이 · 제목 · 본문 · 장소 자리. 앞서 본 글을 새 글처럼 보이지 않는다 */
@Composable
private fun StoryDetailSkeleton(modifier: Modifier = Modifier) {
    val t = runTone()
    val bar = if (t.dark) Color(0xFF1C3A66) else t.track
    val label = stringResource(R.string.story_blue_detail_loading)
    Column(modifier.fillMaxWidth().padding(top = 12.dp).semantics { contentDescription = label }.testTag("story-detail-loading")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(bar))
            Spacer(Modifier.width(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.width(80.dp).height(14.dp).clip(RoundedCornerShape(7.dp)).background(bar))
                Box(Modifier.width(56.dp).height(12.dp).clip(RoundedCornerShape(6.dp)).background(bar))
            }
        }
        Spacer(Modifier.height(26.dp))
        Box(Modifier.fillMaxWidth(0.85f).height(26.dp).clip(RoundedCornerShape(10.dp)).background(bar))
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth(0.6f).height(26.dp).clip(RoundedCornerShape(10.dp)).background(bar))
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth(0.75f).height(16.dp).clip(RoundedCornerShape(8.dp)).background(bar))
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(16.dp)).background(bar.copy(alpha = 0.6f)))
    }
}

/** 상세의 장소 카드 — 실제 지도 썸네일 · 장소 이름 · "내 위치에서 2.6km" · 확대 표시. 누르면 그 장소의 지도(CM06) */
@Composable
private fun StoryPlaceCard(place: StoryPlace, distance: String, compact: Boolean, onClick: () -> Unit) {
    val t = runTone()
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(t.panelTop, t.panel)), shape).border(1.dp, t.panelEdge, shape)
            .feedbackClickable(role = Role.Button, onClick = onClick).padding(12.dp)
            .testTag("story-place-card"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (compact) {
            Icon(Icons.Outlined.Place, contentDescription = null, tint = t.cyan,
                modifier = Modifier.padding(horizontal = 8.dp).size(30.dp))
        } else {
            StoryPlaceThumb(place, "", Modifier.size(width = 104.dp, height = 80.dp), showName = false)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(place.name, style = runTextStyle(18.sp, t.text, FontWeight.ExtraBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (distance.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(distance, style = runTextStyle(15.sp, t.label, FontWeight.Medium))
            }
        }
        val label = stringResource(R.string.story_view_map)
        Icon(Icons.Filled.OpenInFull, contentDescription = label, tint = t.cyan, modifier = Modifier.padding(8.dp).size(24.dp))
    }
}

/**
 * 글에 붙은 러닝(WRITE13) — 코스 그림(경로가 있을 때) · 날짜 · 거리 · 시간 · 평균 페이스. 경로 없는 러닝은 그림 없이 실제 값만.
 * 읽기 전용 공유 요약이다 — 눌러도 글쓴이의 사적인 기록 화면으로 가지 않는다. 이미 올라간 글의 첨부는 기간이 지나도 그대로 보인다.
 */
@Composable
private fun StoryRunCard(run: StoryRun) {
    val t = runTone()
    val words = rememberStoryWords()
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(t.panelTop, t.panel)), shape)
            .border(1.5.dp, t.cobalt.copy(alpha = 0.8f), shape).padding(12.dp)
            .testTag("story-run-card"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (run.hasRoute) {
            StoryMapTone {
                // 지도(SubcomposeLayout)는 고유 높이를 물을 수 없다 — 줄 높이를 지도에 맞추지 않고 지도 높이를 정해 둔다
                StoryRouteThumb(run.route, Modifier.weight(0.9f).height(130.dp), seed = run.endedAt.hashCode(), radius = 12.dp)
            }
        } else {
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(t.inset),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Outlined.DirectionsRun, contentDescription = null, tint = t.label, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.story_blue_run_record), style = runTextStyle(18.sp, t.text, FontWeight.ExtraBold))
            Text(words.date(run.day), style = runTextStyle(14.sp, t.label, FontWeight.Medium))
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(StoryComposeRules.km(run.distanceMeters), style = runNumberStyle(34.sp, t.text), modifier = Modifier.alignByBaseline())
                Spacer(Modifier.width(6.dp))
                Text("km", style = runTextStyle(18.sp, t.text, FontWeight.Bold), modifier = Modifier.alignByBaseline())
            }
            Spacer(Modifier.height(4.dp))
            Row {
                StoryRunStat(words.duration(run.durationSec), stringResource(R.string.story_blue_time), Modifier.weight(1f))
                StoryComposeRules.pace(run.paceSecPerKm)?.let { pace ->
                    StoryRunStat(pace, stringResource(R.string.story_blue_avg_pace), Modifier.weight(1f))
                }
            }
            if (!run.hasRoute) {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.story_run_card_no_route), style = runTextStyle(13.sp, t.label, FontWeight.Medium))
            }
        }
    }
}

@Composable
private fun StoryRunStat(value: String, label: String, modifier: Modifier = Modifier) {
    val t = runTone()
    Column(modifier) {
        Text(value, style = runTextStyle(15.sp, t.text, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = runTextStyle(12.sp, t.label, FontWeight.Medium), maxLines = 1)
    }
}

@Composable
private fun StoryCommentRow(comment: Comment) {
    val t = runTone()
    val name = if (comment.mine) stringResource(R.string.story_me) else comment.author.ifBlank { stringResource(R.string.story_runner) }
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp)
            .padding(start = if (comment.isReply) 28.dp else 0.dp)
            .testTag("story-comment-${comment.id}"),
    ) {
        StoryAvatar(name, size = 40.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = runTextStyle(16.sp, t.text, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false))
                if (comment.mine) {
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.story_my_comment), style = runTextStyle(13.sp, t.cyan, FontWeight.SemiBold))
                }
                Spacer(Modifier.weight(1f))
                Text(relativeTime(comment.createdAt), style = runTextStyle(13.sp, t.label, FontWeight.Medium))
            }
            Spacer(Modifier.height(4.dp))
            Text(comment.body, style = runTextStyle(16.sp, t.text, FontWeight.Medium, 1.45f))
        }
    }
}

/** 아래에 고정된 댓글 입력 — 공백을 뺀 내용이 있어야 "등록"이 켜진다. 200자까지. 실패 뒤에는 "재시도" */
@Composable
private fun StoryCommentInput(value: String, onValueChange: (String) -> Unit, busy: Boolean, retry: Boolean, onSend: () -> Unit) {
    val t = runTone()
    val canSend = value.isNotBlank() && !busy
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().padding(start = StoryFormGutter, end = StoryFormGutter, top = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.weight(1f).heightIn(min = 52.dp).clip(shape).background(t.inset)
                .border(1.dp, if (value.isNotEmpty()) t.cobalt else t.panelEdge, shape)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) Text(stringResource(R.string.story_blue_comment_hint), style = runTextStyle(16.sp, t.muted, FontWeight.Medium))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = runTextStyle(16.sp, t.text, FontWeight.Medium),
                cursorBrush = SolidColor(t.cyan),
                maxLines = 4,
                modifier = Modifier.fillMaxWidth().testTag("story-comment-input"),
            )
        }
        Spacer(Modifier.width(10.dp))
        StoryButton(
            stringResource(if (retry) R.string.story_blue_comment_retry else R.string.story_comment_send),
            onSend, Modifier.testTag("story-comment-send"), enabled = canSend, busy = busy, compact = true,
        )
    }
}

@Composable
private fun StoryMenuRow(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    hint: String? = null,
    onClick: () -> Unit,
) {
    val t = runTone()
    Row(
        modifier.fillMaxWidth().heightIn(min = 68.dp).feedbackClickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(18.dp))
        Column {
            Text(label, style = runTextStyle(19.sp, color, FontWeight.Bold))
            if (hint != null) Text(hint, style = runTextStyle(14.sp, t.label, FontWeight.Medium))
        }
    }
}

/**
 * 신고(CM27 · 40) — 사유 하나를 고른 뒤 접수. 신고한 사람은 글쓴이에게 보이지 않는다(서버 content_reports 는 본인만 읽는다).
 * 보내는 중 · 실패 뒤에도 고른 사유는 그대로다. 접수 성공은 서버가 받아 준 뒤에만(CM28).
 */
@Composable
private fun StoryReportSheet(busy: Boolean, failed: Boolean, onSubmit: (StoryReportReason) -> Unit, onDismiss: () -> Unit) {
    val t = runTone()
    var picked by rememberSaveable { mutableStateOf<StoryReportReason?>(null) }
    StorySheet(stringResource(R.string.story_blue_report_title), onDismiss, dismissible = !busy) {
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
                onClick = { if (!busy) picked = reason },
                modifier = Modifier.testTag("story-report-${reason.name}"),
                leading = true,
            )
            StoryDivider()
        }
        Spacer(Modifier.height(10.dp))
        if (failed) {
            Text(stringResource(R.string.story_error_report), style = runTextStyle(15.sp, t.dangerText, FontWeight.Bold),
                modifier = Modifier.testTag("story-report-error"))
        } else {
            Text(stringResource(R.string.story_report_private), style = runTextStyle(14.sp, t.label, FontWeight.Medium))
        }
        Spacer(Modifier.height(14.dp))
        StoryButton(
            stringResource(if (failed) R.string.story_blue_report_retry else R.string.story_blue_report_submit),
            { picked?.let(onSubmit) },
            Modifier.testTag("story-report-submit"),
            enabled = picked != null,
            busy = busy,
        )
    }
}
