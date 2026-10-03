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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.Post
import com.stepup.android.domain.StoryPlace
import com.stepup.android.domain.StoryText
import com.stepup.android.domain.formatStoryDistance
import com.stepup.android.ui.components.LiveRouteMap
import com.stepup.android.ui.components.LocalMapTone
import com.stepup.android.ui.components.MapTone
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunButtonKind
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.RunStateArt
import com.stepup.android.ui.components.StepUpMap
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.community.relativeTime
import com.stepup.android.ui.theme.StepUpSans

/*
 * 러닝 이야기(커뮤니티 기본 CM01~44 · 코스 글쓰기 WRITE01~20, 2026-10-03 파란 톤 전달본)의 공통 조각.
 *
 * 러닝 리메이크의 남색 부품([runTone] · [RunButton] · [RunSheet])을 그대로 쓴다 — 바닥 #031427, 면 #0B2B50,
 * 전기 파랑 #0754FF, 시안 #48D9FA, 흰 주 버튼. 글 행은 두꺼운 카드 없이 얇은 구분선, 오른쪽에 공개 장소의 실제 지도
 * 썸네일. 안쪽 화면의 머리는 뒤로 · 가운데 제목 · (더보기)이며 로고와 하단 탭을 반복하지 않는다.
 */

internal val StoryListGutter = 20.dp
internal val StoryFormGutter = 20.dp

/** 러닝 화면과 같은 남색 지도 색 — 실제 타일(길 · 물 · 이름)의 색만 옮긴다 */
@Composable
fun StoryMapTone(content: @Composable () -> Unit) {
    val t = runTone()
    CompositionLocalProvider(LocalMapTone provides MapTone(t.mapFilter, t.mapShade), content = content)
}

enum class StoryButtonStyle { PRIMARY, SECONDARY, DANGER }

/**
 * 버튼 — 주(흰 면 · 파란 아랫면), 보조(남색 면 · 파란 테두리), 위험(빨강). [compact] 는 입력 줄 옆 "등록"처럼 좁은 자리,
 * [hero] 는 목록 아래 큰 "+ 글쓰기"(기울인 굵은 글자).
 */
@Composable
fun StoryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: StoryButtonStyle = StoryButtonStyle.PRIMARY,
    enabled: Boolean = true,
    busy: Boolean = false,
    icon: ImageVector? = null,
    compact: Boolean = false,
    italic: Boolean = false,
) {
    if (compact) {
        StoryCompactButton(text, onClick, modifier, enabled, busy)
        return
    }
    RunButton(
        label = text,
        onClick = onClick,
        modifier = modifier,
        kind = when (style) {
            StoryButtonStyle.PRIMARY -> RunButtonKind.Primary
            StoryButtonStyle.SECONDARY -> RunButtonKind.Secondary
            StoryButtonStyle.DANGER -> RunButtonKind.Danger
        },
        icon = icon,
        enabled = enabled,
        busy = busy,
        italic = italic,
    )
}

/** 입력 줄 옆 좁은 버튼 — 켜지면 파란 면 · 흰 글자, 꺼지면 흐린 면. 보내는 동안은 도는 표시만 */
@Composable
private fun StoryCompactButton(text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean, busy: Boolean) {
    val t = runTone()
    val active = enabled && !busy
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier.widthIn(min = 72.dp).heightIn(min = 52.dp).clip(shape)
            .background(if (active) t.cobalt else t.disabledFace.copy(alpha = if (t.dark) 0.55f else 1f), shape)
            .feedbackClickable(enabled = active, role = Role.Button, onClick = onClick)
            .semantics { if (busy) contentDescription = text }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (busy) {
            RunSpinner(Modifier.size(20.dp))
        } else {
            Text(text, style = runTextStyle(17.sp, if (active) Color.White else t.disabledInk, FontWeight.Bold), maxLines = 1)
        }
    }
}

/** 글자 버튼 — "지역 직접 선택" · "저장하지 않고 나가기" 처럼 보조 행동. [color] 를 주지 않으면 보조 글자색 */
@Composable
fun StoryTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    val t = runTone()
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .feedbackClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, style = runTextStyle(16.sp, if (color == Color.Unspecified) t.label else color, FontWeight.SemiBold),
            textAlign = TextAlign.Center,
        )
    }
}

/** 시안 글자 링크 — "지도 보기 >" · "지역 변경 >" · "범위 변경 ∨" */
@Composable
fun StoryLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    trailing: ImageVector? = Icons.Filled.ChevronRight,
) {
    val t = runTone()
    val ink = if (color == Color.Unspecified) t.cyan else color
    Row(
        modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .feedbackClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = runTextStyle(16.sp, ink, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (trailing != null) {
            Spacer(Modifier.width(4.dp))
            Icon(trailing, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
        }
    }
}

/** 안쪽 화면의 머리 — 왼쪽 뒤로, 가운데 제목, 오른쪽 행동 하나(글 메뉴). 로고 · SUP 잔액은 두지 않는다 */
@Composable
fun StoryHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val t = runTone()
    Box(modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 4.dp)) {
        Box(
            Modifier.align(Alignment.CenterStart).size(48.dp).clip(CircleShape)
                .feedbackClickable(cue = FeedbackCue.Back, onClick = onBack).testTag("story-back"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBackIos, stringResource(R.string.cd_back), tint = t.text,
                modifier = Modifier.size(20.dp).padding(start = 3.dp),
            )
        }
        Text(
            title,
            style = runTextStyle(19.sp, t.text, FontWeight.ExtraBold),
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 56.dp).semantics { heading() },
        )
        if (trailing != null) Box(Modifier.align(Alignment.CenterEnd)) { trailing() }
    }
}

/**
 * 아래에서 올라오는 시트 — 불투명 남색 면, 배경 전체를 덮는 어두운 가림막, 손잡이와 닫기(×). 뒤 화면은 눌리지 않는다.
 * 뒤로 가기 · 바깥 누르기 · × 가 모두 [onDismiss] 다. [dismissible] 이 false 면(보내는 중) 닫히지 않는다.
 */
@Composable
fun StorySheet(
    title: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissible: Boolean = true,
    centered: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val t = runTone()
    RunSheet(onDismiss = onDismiss, modifier = modifier, dismissible = dismissible, closeTag = "story-sheet-close") {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        ) {
            if (title != null) {
                Text(
                    title,
                    style = TextStyle(
                        fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp,
                        lineHeight = 1.3.em, letterSpacing = (-0.02).em, color = t.text,
                    ),
                    textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                    modifier = Modifier.padding(end = if (centered) 0.dp else 44.dp).semantics { heading() },
                )
                Spacer(Modifier.height(8.dp))
            }
            content()
        }
    }
}

/**
 * 되돌릴 수 없는 일(삭제) 확인 — 시트(CM24). 취소(보조) · 삭제하기. 보내는 동안은 닫히지 않는다.
 * 이름은 예전 창(Dialog) 그대로 둔다 — 꼬리표 "story-confirm-dialog" 도 그대로.
 */
@Composable
fun StoryConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    busy: Boolean = false,
    note: String? = null,
    /** 요청이 실패했다(CM25) — 빨간 한 줄. 글은 그대로 있다 */
    errorText: String? = null,
) {
    val t = runTone()
    StorySheet(title = title, onDismiss = onDismiss, dismissible = !busy, modifier = Modifier.testTag("story-confirm-dialog")) {
        Text(body, style = runTextStyle(17.sp, t.text, FontWeight.Bold, 1.45f))
        if (note != null) {
            Spacer(Modifier.height(6.dp))
            Text(note, style = runTextStyle(14.sp, t.label, FontWeight.Medium, 1.45f))
        }
        if (errorText != null) {
            Spacer(Modifier.height(6.dp))
            Text(errorText, style = runTextStyle(15.sp, t.dangerText, FontWeight.Bold, 1.45f), modifier = Modifier.testTag("story-confirm-error"))
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
            StoryButton(
                stringResource(R.string.common_cancel), onDismiss, Modifier.weight(1f),
                style = StoryButtonStyle.SECONDARY, enabled = !busy,
            )
            StoryButton(confirm, onConfirm, Modifier.weight(1f).testTag("story-confirm"), busy = busy)
        }
    }
}

/** 이름 첫 글자 원 — 이모지처럼 두 글자 단위인 첫 글자를 반으로 자르지 않는다 */
@Composable
fun StoryAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 30.dp) {
    val t = runTone()
    val initial = name.trim().takeIf { it.isNotEmpty() }?.let { String(Character.toChars(it.codePointAt(0))) } ?: "?"
    Box(
        modifier.size(size).clip(CircleShape)
            .background(if (t.dark) Color(0xFF0E3468) else t.chipFace),
        contentAlignment = Alignment.Center,
    ) {
        Text(initial, style = runTextStyle((size.value * 0.4f).sp, if (t.dark) Color(0xFFBFD6FF) else t.cobaltText, FontWeight.Bold, 1.1f))
    }
}

/** 글쓴이 이름 — 내 글이면 "나" */
@Composable
fun storyAuthor(post: Post): String =
    if (post.mine) stringResource(R.string.story_me) else post.author.ifBlank { stringResource(R.string.story_runner) }

/** 거리 문구 — 기준점이 없으면 빈 문자열(숫자를 지어내지 않는다) */
fun storyDistance(meters: Double?): String = meters?.let(::formatStoryDistance).orEmpty()

/** 썸네일 · 카드의 면 — 지도 위에 남색을 살짝 덮어 글자가 읽히게 */
private val ThumbShape = RoundedCornerShape(12.dp)

/**
 * 장소 지도 썸네일 — 글쓴이가 고른 공개 장소 둘레의 실제 지도(남색 타일)에 시안 핀 · 장소 이름 · 거리.
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
    val t = runTone()
    Box(
        modifier.clip(ThumbShape).background(t.inset).border(1.dp, t.panelEdge, ThumbShape)
            .then(if (onClick != null) Modifier.feedbackClickable(role = Role.Button, onClick = onClick) else Modifier),
    ) {
        StoryMapTone { StepUpMap(focus = listOf(place.point), modifier = Modifier.fillMaxSize(), seed = place.key.hashCode()) }
        Box(Modifier.fillMaxSize().background(t.screen.copy(alpha = if (t.dark) 0.30f else 0.12f)))
        Column(
            Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = t.cyan, modifier = Modifier.size(26.dp))
            if (showName) {
                Spacer(Modifier.height(4.dp))
                Text(place.name, style = runTextStyle(12.sp, t.text, FontWeight.SemiBold, 1.2f), maxLines = 1,
                    overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
            if (distance.isNotEmpty()) {
                Text(distance, style = runTextStyle(17.sp, t.text, FontWeight.ExtraBold, 1.2f), maxLines = 1)
            }
        }
    }
}

/**
 * 글에 붙은 코스 썸네일(WRITE12) — 저장된 실제 경로를 남색 지도 위에, 아래 "달린 코스 · 2.10km".
 * 경로 좌표가 둘 미만이면 부르지 않는다(가짜 코스를 그리지 않는다).
 */
@Composable
fun StoryCourseThumb(route: List<GeoPoint>, km: String, seed: Int, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val t = runTone()
    Box(
        modifier.clip(ThumbShape).background(t.inset).border(1.dp, t.panelEdge, ThumbShape)
            .then(if (onClick != null) Modifier.feedbackClickable(role = Role.Button, onClick = onClick) else Modifier),
    ) {
        if (route.size >= 2) StoryMapTone { LiveRouteMap(route, Modifier.fillMaxSize(), seed = seed) }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.45f to Color.Transparent, 1f to t.screen.copy(alpha = 0.85f)),
            ),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 8.dp, end = 8.dp)) {
            Text(stringResource(R.string.story_blue_course_label), style = runTextStyle(12.sp, t.text, FontWeight.SemiBold, 1.2f), maxLines = 1)
            Text("${km}km", style = runTextStyle(17.sp, t.text, FontWeight.ExtraBold, 1.2f), maxLines = 1)
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
    size: Int = 15,
    onLike: (() -> Unit)? = null,
) {
    val t = runTone()
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.then(
                if (onLike != null) Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp))
                    .feedbackClickable(role = Role.Button, onClick = onLike).padding(end = 10.dp)
                else Modifier,
            ).testTag("story-like"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = stringResource(if (liked) R.string.story_unlike else R.string.story_like),
                tint = if (liked) t.cyan else t.label, modifier = Modifier.size((size + 7).dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("$likes", style = runTextStyle(size.sp, t.label, FontWeight.Medium))
        }
        Spacer(Modifier.width(14.dp))
        Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = stringResource(R.string.story_comments_label),
            tint = t.label, modifier = Modifier.size((size + 5).dp))
        Spacer(Modifier.width(6.dp))
        Text("$comments", style = runTextStyle(size.sp, t.label, FontWeight.Medium))
    }
}

/**
 * 목록 한 줄(CM01 · CM05 · WRITE12) — 왼쪽 제목 · 본문 한 줄 · 글쓴이 · 시간 · 반응, 오른쪽 공개 장소 썸네일
 * (코스가 붙은 글은 코스 썸네일). 제목 쪽을 누르면 상세, 장소 썸네일을 누르면 그 장소의 지도. 코스 썸네일은 상세로 간다.
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
    val t = runTone()
    val title = post.title
    val body = StoryText.bodyForDisplay(post.body)
    val run = post.run
    Row(modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.Top) {
        Column(
            Modifier.weight(1f).heightIn(min = 104.dp).clip(RoundedCornerShape(8.dp))
                .feedbackClickable(role = Role.Button, onClick = onOpen).testTag("story-row-${post.id}"),
        ) {
            Text(title, style = runTextStyle(18.sp, t.text, FontWeight.ExtraBold, 1.3f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            val second = when {
                body.isNotEmpty() -> body.lineSequence().first()
                run != null && place != null -> place.name
                else -> ""
            }
            if (second.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(second, style = runTextStyle(15.sp, t.label, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    listOfNotNull(
                        storyAuthor(post), relativeTime(post.createdAt),
                        if (post.mine) stringResource(R.string.story_mine_tag) else null,
                    ).joinToString(" · "),
                    style = runTextStyle(14.sp, t.label, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                StoryReactions(post.likes, post.liked, post.commentCount, onLike = onLike)
            }
        }
        val thumb = Modifier.padding(start = 14.dp).size(width = 96.dp, height = 96.dp)
        when {
            run != null && run.hasRoute -> StoryCourseThumb(
                run.route, com.stepup.android.domain.StoryComposeRules.km(run.distanceMeters), seed = run.endedAt.hashCode(),
                modifier = thumb.testTag("story-thumb-${post.id}"), onClick = onOpen,
            )
            place != null -> StoryPlaceThumb(
                place, storyDistance(meters), thumb.testTag("story-thumb-${post.id}"), onClick = onOpenPlace,
            )
        }
    }
}

/** 얇은 구분선 — 글마다 상자를 두르지 않는다 */
@Composable
fun StoryDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(runTone().divider))
}

/** 정보가 없거나 실패했을 때의 가운데 안내 — 그림 · 제목 · 설명 · 행동 */
@Composable
fun StoryStateBlock(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    alert: Boolean = false,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    val t = runTone()
    Column(modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        RunStateArt(icon, size = 88.dp, alert = alert)
        Spacer(Modifier.height(16.dp))
        Text(title, style = runTextStyle(21.sp, t.text, FontWeight.ExtraBold, 1.35f), textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(8.dp))
        Text(body, style = runTextStyle(16.sp, t.label, FontWeight.Medium, 1.5f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        actions()
    }
}

/** 불러오는 동안 목록 자리를 지키는 빈 줄(CM34) — 왼쪽 글 막대 셋, 오른쪽 썸네일 자리 */
@Composable
fun StorySkeletonRow(modifier: Modifier = Modifier) {
    val t = runTone()
    val bar = if (t.dark) Color(0xFF1C3A66) else t.track
    Row(modifier.fillMaxWidth().padding(vertical = 14.dp)) {
        Column(Modifier.weight(1f).padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth(0.92f).height(16.dp).clip(RoundedCornerShape(8.dp)).background(bar))
            Box(Modifier.fillMaxWidth(0.62f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(bar))
            Box(Modifier.fillMaxWidth(0.24f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(bar))
        }
        Spacer(Modifier.width(14.dp))
        Box(Modifier.size(96.dp).clip(ThumbShape).background(bar))
    }
}

/**
 * 짧은 안내 — 게시 · 수정 · 숨김 · 삭제의 확인, 또는 [error] 면 실패(좋아요 · 댓글). 글 읽기를 막지 않는 띠로 보이고,
 * [action] 이 있으면 오른쪽에 글자 버튼(닫기 · 되돌리기).
 */
@Composable
fun StoryToast(
    text: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
    error: Boolean = false,
) {
    val t = runTone()
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.fillMaxWidth().clip(shape).background(Brush.verticalGradient(listOf(t.sheetTop, t.sheet)))
            .border(1.dp, if (error) t.errorEdge else t.panelEdge, shape)
            .padding(start = 14.dp, end = 4.dp).heightIn(min = 52.dp)
            .testTag("story-toast"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (error) Icons.Outlined.ErrorOutline else Icons.Filled.Check, contentDescription = null,
            tint = if (error) t.errorIcon else t.cyan, modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(text, style = runTextStyle(15.sp, t.text, FontWeight.SemiBold), modifier = Modifier.weight(1f).padding(vertical = 8.dp))
        if (action != null) StoryTextButton(action, onAction, color = t.cyan)
    }
}

/** 본문 안 한 줄 안내(CM39 좋아요 실패) — 아이콘 · 시안 글자. 확인 창을 띄우지 않는다 */
@Composable
fun StoryInlineNotice(text: String, modifier: Modifier = Modifier, tag: String? = null) {
    val t = runTone()
    Row(modifier.fillMaxWidth().then(if (tag != null) Modifier.testTag(tag) else Modifier), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = t.cyan, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = runTextStyle(14.sp, t.cyan, FontWeight.SemiBold))
    }
}
