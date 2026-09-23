package com.stepup.android.ui.screens.community

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.core.ExternalIntents
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.Post
import com.stepup.android.domain.PostCategory
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.quietClickable
import com.stepup.android.ui.theme.Alert
import com.stepup.android.ui.theme.Carbon
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.OnVolt
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt

import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.Night

// ─────────────────────────────────────────────────────────────
// 세그먼트 컨트롤 — "게시판 / 크루"
// ─────────────────────────────────────────────────────────────

/** 두 개짜리 세그먼트. 선택된 쪽만 볼트 플레이트가 미끄러지듯 붙는다. */
@Composable
fun SegmentedTabs(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Carbon)
            .border(1.dp, Edge, RoundedCornerShape(50))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val active = index == selected
            val alpha by animateFloatAsState(
                targetValue = if (active) 1f else 0f,
                animationSpec = androidx.compose.animation.core.tween(LocalMotion.current.duration(180)),
                label = "segment$index",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(Volt.copy(alpha = alpha))
                    .semantics { this.selected = active }
                    .feedbackClickable(cue = FeedbackCue.Select, role = Role.Tab) { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (active) OnVolt else Silver,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.Black else FontWeight.SemiBold,
                    letterSpacing = 0.2.sp,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 카테고리
// ─────────────────────────────────────────────────────────────

/** 게시판 필터 칩에 쓰는 이름 */
@Composable
fun BoardFilter.label(): String = stringResource(
    when (this) {
        BoardFilter.ALL -> R.string.post_cat_all
        BoardFilter.FLASH -> R.string.post_cat_flash
        BoardFilter.HOT -> R.string.post_cat_hot
        BoardFilter.FREE -> R.string.post_cat_free
        BoardFilter.TIP -> R.string.post_cat_tip
    }
)

@Composable
fun PostCategory.label(): String = stringResource(
    when (this) {
        PostCategory.FLASH -> R.string.post_cat_flash
        PostCategory.FREE -> R.string.post_cat_free
        PostCategory.TIP -> R.string.post_cat_tip
    }
)

fun PostCategory.tint(): Color = when (this) {
    PostCategory.FLASH -> Volt
    PostCategory.FREE -> Color(0xFF1E8FE8)
    PostCategory.TIP -> Color(0xFFD99A00)
}

@Composable
fun CategoryChip(category: PostCategory, modifier: Modifier = Modifier) {
    val c = category.tint()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(c.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = category.label(),
            color = c,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
        )
    }
}

// ─────────────────────────────────────────────────────────────
// 게시글 카드
// ─────────────────────────────────────────────────────────────

/** 번개러닝 카드 — 거리·시간·정원이 앞에 오고 참가 버튼이 붙는다. */
@Composable
fun FlashRunCard(
    post: Post,
    onJoin: () -> Unit,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onOpen: () -> Unit = {},
    onReport: (() -> Unit)? = null,
    here: GeoPoint? = null,
) {
    val context = LocalContext.current
    val hasPlace = post.place.isNotBlank()
    // 모임 장소까지의 거리는 읽는 사람의 자리에서 잰다. 둘 중 하나라도 모르면
    // 대신 함께 달릴 거리를 적는다 — 지어낸 거리보다 낫다.
    val awayKm = post.awayKmFrom(here)
    GlowCard(
        // 카드 어디를 눌러도 상세로 — 참가/좋아요/장소 등 안쪽 클릭이 우선한다
        modifier = Modifier.quietClickable(onOpen),
        contentPadding = PaddingValues(15.dp),
        spacing = 10.dp,
        accent = post.joined,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            CategoryChip(PostCategory.FLASH)
            Text(
                text = if (awayKm != null) {
                    stringResource(R.string.post_km_away, "%.1f".format(awayKm))
                } else {
                    stringResource(R.string.post_run_km, "%.1f".format(post.distanceKm))
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Volt,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = relativeTime(post.createdAt),
                fontSize = 10.sp,
                color = Slate,
            )
            if (onDelete != null && post.mine) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.post_delete),
                    tint = Slate,
                    modifier = Modifier
                        .size(15.dp)
                        .quietClickable(onDelete),
                )
            }
            // 남의 글에는 신고. 신고가 5건 모이면 서버가 모두의 목록에서 내린다.
            if (onReport != null && !post.mine) {
                Icon(
                    Icons.Filled.Flag,
                    contentDescription = stringResource(R.string.report_title),
                    tint = Slate,
                    modifier = Modifier
                        .size(15.dp)
                        .quietClickable(onReport),
                )
            }
        }

        Text(
            text = post.title,
            style = MaterialTheme.typography.titleSmall,
            color = Snow,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (post.body.isNotBlank()) {
            Text(
                text = post.body,
                style = MaterialTheme.typography.bodySmall,
                color = Silver,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // 장소를 누르면 구글 지도에서 집결지를 연다
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CarbonHigh.copy(alpha = 0.55f))
                .then(
                    if (hasPlace) {
                        Modifier.quietClickable {
                            ExternalIntents.openPlaceInMaps(context, post.place)
                        }
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = null,
                tint = if (hasPlace) Volt else Slate,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = post.place.ifBlank { stringResource(R.string.post_place_tbd) },
                fontSize = 11.sp,
                color = Silver,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (hasPlace) {
                Icon(
                    Icons.Filled.Map,
                    contentDescription = stringResource(R.string.post_open_map),
                    tint = Volt,
                    modifier = Modifier.size(12.dp),
                )
            }
            Icon(
                Icons.Filled.Schedule,
                contentDescription = null,
                tint = Slate,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = startsInLabel(post.meetAt),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (post.isClosed) Slate else Volt,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.post_slots, post.joinedCount, post.capacity),
                fontSize = 11.sp,
                color = Silver,
            )
            LikeRow(
                post = post,
                onLike = onLike,
                onComment = onComment,
                modifier = Modifier.weight(1f),
            )
            JoinPill(
                joined = post.joined,
                enabled = !post.isClosed && (post.joined || !post.isFull),
                onClick = onJoin,
            )
        }
    }
}

/** 자유/꿀팁 글 카드 */
@Composable
fun TextPostCard(
    post: Post,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onReport: (() -> Unit)? = null,
) {
    GlowCard(contentPadding = PaddingValues(15.dp), spacing = 9.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(CarbonHigh, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = post.author.take(1).uppercase(),
                    color = Volt,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Text(
                text = post.author,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Snow,
            )
            CategoryChip(post.category)
            Spacer(Modifier.weight(1f))
            Text(text = relativeTime(post.createdAt), fontSize = 10.sp, color = Slate)
            if (onDelete != null && post.mine) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.post_delete),
                    tint = Slate,
                    modifier = Modifier
                        .size(15.dp)
                        .quietClickable(onDelete),
                )
            }
            // 남의 글에는 신고. 신고가 5건 모이면 서버가 모두의 목록에서 내린다.
            if (onReport != null && !post.mine) {
                Icon(
                    Icons.Filled.Flag,
                    contentDescription = stringResource(R.string.report_title),
                    tint = Slate,
                    modifier = Modifier
                        .size(15.dp)
                        .quietClickable(onReport),
                )
            }
        }

        Text(
            text = post.title,
            style = MaterialTheme.typography.titleSmall,
            color = Snow,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (post.body.isNotBlank()) {
            Text(
                text = post.body,
                style = MaterialTheme.typography.bodySmall,
                color = Silver,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        LikeRow(post = post, onLike = onLike, onComment = onComment)
    }
}

@Composable
private fun LikeRow(
    post: Post,
    onLike: () -> Unit,
    onComment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.quietClickable(onLike),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = if (post.liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = null,
                tint = if (post.liked) Alert else Silver,
                modifier = Modifier.size(15.dp),
            )
            Text("${post.likes}", fontSize = 11.sp, color = Silver)
        }
        Row(
            modifier = Modifier.quietClickable(onComment),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                Icons.Filled.ChatBubbleOutline,
                contentDescription = stringResource(R.string.comments_open),
                tint = Silver,
                modifier = Modifier.size(14.dp),
            )
            Text("${post.commentCount}", fontSize = 11.sp, color = Silver)
        }
    }
}

@Composable
private fun JoinPill(joined: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val bg = when {
        joined -> Volt
        !enabled -> CarbonHigh
        else -> Color.Transparent
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(
                width = 1.dp,
                color = if (joined || !enabled) Color.Transparent else Volt.copy(alpha = 0.55f),
                shape = RoundedCornerShape(50),
            )
            .then(if (enabled) Modifier.quietClickable(onClick) else Modifier)
            .padding(horizontal = 13.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            Icons.Filled.Bolt,
            contentDescription = null,
            tint = if (joined) OnVolt else if (enabled) Volt else Slate,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = stringResource(
                when {
                    joined -> R.string.post_joined
                    !enabled -> R.string.post_closed
                    else -> R.string.post_join
                }
            ),
            color = if (joined) OnVolt else if (enabled) Volt else Slate,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ─────────────────────────────────────────────────────────────
// 입력 필드
// ─────────────────────────────────────────────────────────────

@Composable
fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    minHeight: Int = 0,
    singleLine: Boolean = true,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = Slate,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CarbonHigh)
                .border(1.dp, Edge, RoundedCornerShape(14.dp))
                .padding(horizontal = 13.dp, vertical = 12.dp),
        ) {
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(placeholder, fontSize = 13.sp, color = Slate)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = TextStyle(fontFamily = com.stepup.android.ui.theme.StepUpSans, color = Snow, fontSize = 13.sp, lineHeight = 19.sp),
                cursorBrush = SolidColor(Volt),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (minHeight > 0) Modifier.height(minHeight.dp) else Modifier),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 시간 포맷
// ─────────────────────────────────────────────────────────────

/** "방금 · 3시간 전 · 2일 전" */
@Composable
fun relativeTime(timestamp: Long): String {
    val diff = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
    val minutes = diff / 60_000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> stringResource(R.string.time_just_now)
        hours < 1 -> stringResource(R.string.time_minutes_ago, minutes.toInt())
        days < 1 -> stringResource(R.string.time_hours_ago, hours.toInt())
        else -> stringResource(R.string.time_days_ago, days.toInt())
    }
}

/** 번개러닝 시작까지 남은 시간 */
@Composable
fun startsInLabel(meetAt: Long): String {
    val diff = meetAt - System.currentTimeMillis()
    if (diff <= 0) return stringResource(R.string.post_started)
    val minutes = diff / 60_000
    val hours = minutes / 60
    return if (hours >= 1) {
        stringResource(R.string.post_starts_in_hours, hours.toInt(), (minutes % 60).toInt())
    } else {
        stringResource(R.string.post_starts_in_minutes, minutes.toInt())
    }
}
