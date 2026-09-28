package com.stepup.android.ui.screens.community.stories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.stepup.android.R
import com.stepup.android.domain.Post
import com.stepup.android.domain.StoryPlace
import com.stepup.android.domain.StoryText
import com.stepup.android.domain.formatStoryDistance
import com.stepup.android.ui.components.StepUpMap
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.components.SecondaryHeader
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.community.relativeTime
import com.stepup.android.ui.theme.Alert
import com.stepup.android.ui.theme.Carbon
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.Night
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.StepUpDesign
import com.stepup.android.ui.theme.Volt
import com.stepup.android.ui.theme.VoltText

/*
 * 동네 이야기(목록형 커뮤니티)의 공통 조각. 시안(2026-09-27)의 수치를 앱 토큰으로 옮겼다:
 * 주요 버튼 높이 50 · 모서리 14, 하단 시트 위 모서리 24, 목록 좌우 여백 22, 장소 썸네일 92 × 101.
 */

internal val StoryListGutter = 22.dp
internal val StoryFormGutter = 24.dp
private val ButtonShape = RoundedCornerShape(14.dp)

/** 주요 버튼의 면 · 글자 — 어두운 테마는 흰 면에 짙은 글자, 밝은 테마는 남색 면에 흰 글자(PrimaryCta 와 같다) */
@Composable
private fun primaryFace(): Pair<Color, Color> =
    if (StepUpColors.dark) Color(0xFFF3F5FF) to Color(0xFF070B12) else Snow to Color.White

enum class StoryButtonStyle { PRIMARY, SECONDARY, DANGER }

/** 시안의 버튼 — 높이 50, 모서리 14. 비활성은 짙은 바탕 · 흐린 글자 */
@Composable
fun StoryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: StoryButtonStyle = StoryButtonStyle.PRIMARY,
    enabled: Boolean = true,
    busy: Boolean = false,
    icon: ImageVector? = null,
    /** 입력 줄 옆 "등록"처럼 좁은 자리 — 글자 폭만큼, 보내는 동안은 도는 표시만 */
    compact: Boolean = false,
) {
    val (face, ink) = when (style) {
        StoryButtonStyle.PRIMARY -> primaryFace()
        StoryButtonStyle.SECONDARY -> CarbonHigh to Snow
        StoryButtonStyle.DANGER -> Alert.copy(alpha = 0.78f) to Color(0xFF1A0707)
    }
    val active = enabled && !busy
    Row(
        modifier
            .then(if (compact) Modifier.widthIn(min = 64.dp) else Modifier.fillMaxWidth())
            .heightIn(min = 50.dp)
            .clip(ButtonShape)
            .background(if (active || style != StoryButtonStyle.PRIMARY) face else CarbonHigh, ButtonShape)
            .then(if (!active && style == StoryButtonStyle.PRIMARY) Modifier.border(1.dp, Edge, ButtonShape) else Modifier)
            .feedbackClickable(enabled = active, role = Role.Button, onClick = onClick)
            .semantics { if (busy && compact) contentDescription = text }
            .padding(horizontal = if (compact) 14.dp else 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val color = if (active || style != StoryButtonStyle.PRIMARY) ink else Slate
        if (busy) {
            CircularProgressIndicator(Modifier.size(16.dp), color = color, strokeWidth = 2.dp)
            if (!compact) Spacer(Modifier.width(8.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        if (!(busy && compact)) {
            Text(text, color = color, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                maxLines = if (compact) 1 else Int.MAX_VALUE)
        }
    }
}

/** 글자 버튼 — "지역 직접 선택" · "삭제하고 나가기" 처럼 보조 행동 */
@Composable
fun StoryTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Silver) {
    Box(
        modifier
            .heightIn(min = StepUpDesign.TouchTarget)
            .clip(RoundedCornerShape(10.dp))
            .feedbackClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = color, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    }
}

/** 오른쪽 위로 향한 화살표가 붙은 글자 링크 — "글 더 보기 ↗", "장소 변경 ↗" */
@Composable
fun StoryLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Silver) {
    Row(
        modifier
            .heightIn(min = StepUpDesign.TouchTarget)
            .clip(RoundedCornerShape(10.dp))
            .feedbackClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = color, fontSize = 12.sp)
        Spacer(Modifier.width(3.dp))
        Icon(Icons.AutoMirrored.Filled.CallMade, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
    }
}

/** 하위 화면의 머리 — 뒤로 · 가운데 제목 · 오른쪽 행동(없으면 빈 자리) */
@Composable
fun StoryHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    // 앱 공통 하위 화면 머리 — 뒤로 · 가운데 제목 · 오른쪽 보조 행동(글 메뉴)
    SecondaryHeader(
        onBack = onBack, balance = null, onOpenWallet = null, title = title, trailing = trailing,
        modifier = modifier.padding(horizontal = StepUpDesign.Gutter - 8.dp),
    )
}

/**
 * 아래에서 올라오는 시트 — 범위 · 메뉴 · 신고 · 임시저장. 위 모서리 24, 제목과 닫기(×).
 * 시트가 열리면 뒤 화면은 눌리지 않고, 뒤로 가기 · 바깥 누르기 · × 가 모두 [onDismiss] 다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorySheet(
    title: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    /** 시트 바탕 — 러닝 이야기 글쓰기의 시트는 시안의 색(#111D2E) */
    container: Color = Carbon,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = container,
        contentColor = Snow,
        modifier = modifier,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = StoryFormGutter, end = StoryFormGutter, bottom = 18.dp),
        ) {
            if (title != null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = Snow, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Box(
                        Modifier.size(StepUpDesign.TouchTarget).clip(CircleShape)
                            .feedbackClickable(role = Role.Button, cue = FeedbackCue.Back, onClick = onDismiss)
                            .testTag("story-sheet-close"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_close), tint = Silver, modifier = Modifier.size(20.dp))
                    }
                }
            }
            content()
        }
    }
}

/** 되돌릴 수 없는 일(삭제)만 가운데 확인 창으로 묻는다 */
@Composable
fun StoryConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    busy: Boolean = false,
) {
    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.padding(horizontal = StoryFormGutter).widthIn(max = 420.dp).fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)).background(Carbon).border(1.dp, Edge, RoundedCornerShape(20.dp))
                .padding(20.dp)
                .testTag("story-confirm-dialog"),
        ) {
            Text(title, color = Snow, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Text(body, color = Silver, fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StoryButton(stringResource(R.string.common_cancel), onDismiss, Modifier.weight(1f),
                    style = StoryButtonStyle.SECONDARY, enabled = !busy)
                StoryButton(confirm, onConfirm, Modifier.weight(1f).testTag("story-confirm"),
                    style = StoryButtonStyle.DANGER, busy = busy)
            }
        }
    }
}

/** 이름 첫 글자 원 — 이모지처럼 두 글자 단위인 첫 글자를 반으로 자르지 않는다 */
@Composable
fun StoryAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 30.dp) {
    val initial = name.trim().takeIf { it.isNotEmpty() }?.let { String(Character.toChars(it.codePointAt(0))) } ?: "?"
    Box(
        modifier.size(size).clip(CircleShape).background(CarbonHigh).border(1.dp, Edge, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(initial, color = Silver, fontSize = (size.value * 0.4f).sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 글쓴이 이름 — 내 글이면 "나" */
@Composable
fun storyAuthor(post: Post): String =
    if (post.mine) stringResource(R.string.story_me) else post.author.ifBlank { stringResource(R.string.story_runner) }

/** 거리 문구 — 기준점이 없으면 빈 문자열(숫자를 지어내지 않는다) */
fun storyDistance(meters: Double?): String = meters?.let(::formatStoryDistance).orEmpty()

/**
 * 장소 지도 썸네일 — 글쓴이가 고른 공개 장소 둘레의 실제 지도(MapTiler 타일)에 핀 · 장소 이름 · 거리.
 * 누르면 그 장소의 지도로 간다(글 제목과 다른 터치 영역).
 */
@Composable
fun StoryPlaceThumb(
    place: StoryPlace,
    distance: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showName: Boolean = true,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier.clip(shape).background(Night).border(1.dp, Edge, shape)
            .then(if (onClick != null) Modifier.feedbackClickable(role = Role.Button, onClick = onClick) else Modifier),
    ) {
        StepUpMap(focus = listOf(place.point), modifier = Modifier.fillMaxSize(), seed = place.key.hashCode())
        Box(Modifier.fillMaxSize().background(Night.copy(alpha = 0.35f)))
        Column(
            Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = VoltText, modifier = Modifier.size(20.dp))
            if (showName) {
                Spacer(Modifier.height(6.dp))
                Text(place.name, color = Snow, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
            if (distance.isNotEmpty()) {
                Text(distance, color = Snow, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

/** 좋아요 · 댓글 수 한 줄 */
@Composable
fun StoryReactions(
    likes: Int,
    liked: Boolean,
    comments: Int,
    modifier: Modifier = Modifier,
    size: Int = 11,
    onLike: (() -> Unit)? = null,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.then(
                if (onLike != null) Modifier.heightIn(min = StepUpDesign.TouchTarget).clip(RoundedCornerShape(10.dp))
                    .feedbackClickable(role = Role.Button, onClick = onLike).padding(end = 8.dp)
                else Modifier,
            ).testTag("story-like"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = stringResource(if (liked) R.string.story_unlike else R.string.story_like),
                tint = if (liked) Volt else Silver, modifier = Modifier.size((size + 3).dp),
            )
            Spacer(Modifier.width(3.dp))
            Text("$likes", color = Silver, fontSize = size.sp)
        }
        Spacer(Modifier.width(10.dp))
        Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = stringResource(R.string.story_comments_label),
            tint = Silver, modifier = Modifier.size((size + 3).dp))
        Spacer(Modifier.width(3.dp))
        Text("$comments", color = Silver, fontSize = size.sp)
    }
}

/**
 * 목록 한 줄 — 왼쪽 장소 지도 썸네일(장소 이름 · 거리), 오른쪽 제목 · 본문 한 줄 · 글쓴이 · 시간 · 반응.
 * 제목 쪽을 누르면 상세, 썸네일을 누르면 그 장소의 지도.
 */
@Composable
fun StoryRow(
    post: Post,
    place: StoryPlace?,
    meters: Double?,
    onOpen: () -> Unit,
    onOpenPlace: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onLike: (() -> Unit)? = null,
) {
    val (title, body) = post.title to StoryText.bodyForDisplay(post.body)
    Row(modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.Top) {
        if (place != null) {
            StoryPlaceThumb(
                place, storyDistance(meters),
                Modifier.size(width = 92.dp, height = 101.dp).testTag("story-thumb-${post.id}"),
                onClick = onOpenPlace,
            )
            Spacer(Modifier.width(14.dp))
        }
        Column(
            Modifier.weight(1f).heightIn(min = 101.dp).clip(RoundedCornerShape(8.dp))
                .feedbackClickable(role = Role.Button, onClick = onOpen).testTag("story-row-${post.id}"),
        ) {
            Text(title, color = Snow, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 2,
                overflow = TextOverflow.Ellipsis, lineHeight = 22.sp)
            if (body.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(body.lineSequence().first(), color = Silver, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                listOfNotNull(
                    storyAuthor(post), relativeTime(post.createdAt),
                    post.run?.let { stringResource(R.string.story_row_run, com.stepup.android.domain.StoryComposeRules.km(it.distanceMeters)) },
                    if (post.mine) stringResource(R.string.story_mine_tag) else null,
                ).joinToString(" · "),
                color = Slate, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            StoryReactions(post.likes, post.liked, post.commentCount, Modifier.align(Alignment.End).padding(top = 2.dp), onLike = onLike)
        }
    }
}

/** 얇은 구분선 — 글마다 상자를 두르지 않는다 */
@Composable
fun StoryDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Edge.copy(alpha = 0.7f)))
}

/** 정보가 없거나 실패했을 때의 가운데 안내 — 아이콘 · 제목 · 설명 · 행동 */
@Composable
fun StoryStateBlock(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    Column(modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = VoltText, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(14.dp))
        Text(title, color = Snow, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, color = Silver, fontSize = 13.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        actions()
    }
}

/** 불러오는 동안 목록 자리를 지키는 빈 줄 */
@Composable
fun StorySkeletonRow(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Row(modifier.fillMaxWidth().padding(vertical = 14.dp)) {
        Box(Modifier.size(width = 92.dp, height = 101.dp).clip(shape).background(CarbonHigh))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f).padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.fillMaxWidth(0.85f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(CarbonHigh))
            Box(Modifier.fillMaxWidth(0.45f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(CarbonHigh))
            Box(Modifier.fillMaxWidth(0.65f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(CarbonHigh))
        }
    }
}

/** 짧은 확인 메시지 — 게시 · 수정 · 숨김 · 삭제. [action] 이 있으면 오른쪽에 글자 버튼(닫기 · 되돌리기) */
@Composable
fun StoryToast(
    text: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier.fillMaxWidth().clip(shape).background(CarbonHigh).border(1.dp, Edge, shape)
            .padding(start = 14.dp, end = 4.dp).heightIn(min = 48.dp)
            .testTag("story-toast"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = VoltText, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Snow, fontSize = 13.sp, modifier = Modifier.weight(1f))
        if (action != null) StoryTextButton(action, onAction, color = VoltText)
    }
}
