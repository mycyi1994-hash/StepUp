package com.stepup.android.ui.screens.community.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.ChatDelivery
import com.stepup.android.domain.ChatEvent
import com.stepup.android.domain.ChatKind
import com.stepup.android.domain.ChatMessage
import com.stepup.android.domain.ChatReply
import com.stepup.android.domain.ChatRoomMeta
import com.stepup.android.domain.ChatRules
import com.stepup.android.domain.ChatState
import com.stepup.android.domain.CrewCard
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.community.crew.CrewGutter
import com.stepup.android.ui.screens.community.crew.CrewImage
import com.stepup.android.ui.screens.community.crew.crewInk
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.StepUpDesign
import com.stepup.android.ui.theme.StepUpSans
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * 크루 채팅(2026-09-29 크루 채팅 패키지)의 부품 — 시안(design-tokens.json · 03-components)의 값을 옮겼다:
 * 대화 좌우 16 · 설정 화면 좌우 24 · 말풍선 모서리 14 · 본문 15 · 입력칸 48(모서리 22) · 누르는 곳 44 이상.
 * 크루 명함형의 색 · 버튼 · 시트([crewInk] · CrewButton · CrewSheet)를 그대로 쓰고, 채팅에만 있는 색을 더한다.
 */

/** 채팅에만 있는 색 — 어두운 테마는 시안 값, 밝은 테마는 같은 관계의 밝은 값 */
@Immutable
internal class ChatInk(
    val incoming: Color,
    val incomingText: Color,
    val outgoing: Color,
    val outgoingText: Color,
    /** 지운 · 숨긴 메시지 말풍선 */
    val gone: Color,
    val pinned: Color,
    val badge: Color,
    val badgeText: Color,
    val unread: Color,
    val unreadText: Color,
    val composer: Color,
    val sendOff: Color,
    val sendOffIcon: Color,
    val sendOn: Color,
    val sendOnIcon: Color,
    val reply: Color,
    val banner: Color,
    val jump: Color,
    val jumpText: Color,
    val failed: Color,
    val ownerFace: Color,
    val ownerFaceText: Color,
    val memberFace: Color,
    val memberFaceText: Color,
    val highlight: Color,
)

private val DarkChatInk = ChatInk(
    incoming = Color(0xFF17243A), incomingText = Color(0xFFF2F4FC), outgoing = Color(0xFFDCE7FB), outgoingText = Color(0xFF13243E),
    gone = Color(0xFF111B2B), pinned = Color(0xFF11243B), badge = Color(0xFF26394F), badgeText = Color(0xFFA3BFFE),
    unread = Color(0xFF4277EF), unreadText = Color(0xFFF2F4FC), composer = Color(0xFF172237), sendOff = Color(0xFF223047),
    sendOffIcon = Color(0xFF70819A), sendOn = Color(0xFFF2F4FC), sendOnIcon = Color(0xFF0B1423), reply = Color(0xFF142137),
    banner = Color(0xFF293044), jump = Color(0xFF274771), jumpText = Color(0xFFF2F4FC), failed = Color(0xFF322932),
    ownerFace = Color(0xFF3D302F), ownerFaceText = Color(0xFFF0D8C7), memberFace = Color(0xFF27374D), memberFaceText = Color(0xFFA3BFFE),
    highlight = Color(0x33A3BFFE),
)

private val LightChatInk = ChatInk(
    incoming = Color(0xFFFFFFFF), incomingText = Color(0xFF10203B), outgoing = Color(0xFF1B2D4E), outgoingText = Color(0xFFFFFFFF),
    gone = Color(0xFFE7EDF6), pinned = Color(0xFFE3ECFB), badge = Color(0xFFDCE7FA), badgeText = Color(0xFF274C8F),
    unread = Color(0xFF2F63D8), unreadText = Color(0xFFFFFFFF), composer = Color(0xFFFFFFFF), sendOff = Color(0xFFD5DCE8),
    sendOffIcon = Color(0xFF7B8AA0), sendOn = Color(0xFF1B2D4E), sendOnIcon = Color(0xFFFFFFFF), reply = Color(0xFFE7EDF6),
    banner = Color(0xFFE2E9F4), jump = Color(0xFF1B2D4E), jumpText = Color(0xFFFFFFFF), failed = Color(0xFFF6E3DC),
    ownerFace = Color(0xFFF1E1D8), ownerFaceText = Color(0xFF6B4533), memberFace = Color(0xFFDCE7FA), memberFaceText = Color(0xFF274C8F),
    highlight = Color(0x33335EAB),
)

@Composable
internal fun chatInk(): ChatInk = if (StepUpColors.dark) DarkChatInk else LightChatInk

/** 대화 화면의 좌우 */
internal val ChatGutter = 16.dp

// ── 머리 ─────────────────────────────────────────────────────

/** 02 · 03 머리 — 뒤로 · 크루 이미지 · 이름 · "크루원 25명"(지금 크루원 수) · 더보기(04 · 05) */
@Composable
internal fun ChatRoomHeader(card: CrewCard?, memberCount: Int?, onBack: () -> Unit, onInfo: (() -> Unit)?) {
    val ink = crewInk()
    Row(
        Modifier.fillMaxWidth().heightIn(min = StepUpDesign.HeaderHeight).padding(start = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChatBackButton(onBack)
        Row(
            Modifier.weight(1f).heightIn(min = 58.dp).clip(RoundedCornerShape(12.dp))
                .feedbackClickable(enabled = onInfo != null, role = Role.Button) { onInfo?.invoke() }
                .padding(horizontal = 2.dp, vertical = 6.dp).testTag("chat-room-title"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (card != null) CrewImage(card, 42.dp, 11.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(card?.name.orEmpty(), color = ink.text, fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (memberCount != null) {
                    Text(
                        stringResource(R.string.chat_members_count, memberCount), color = ink.secondary, fontSize = 11.sp,
                        maxLines = 1, modifier = Modifier.testTag("chat-room-count"),
                    )
                }
            }
        }
        if (onInfo != null) {
            val label = stringResource(R.string.chat_more)
            Box(
                Modifier.size(width = 44.dp, height = 49.dp).clip(RoundedCornerShape(12.dp))
                    .feedbackClickable(role = Role.Button, onClick = onInfo).semantics { contentDescription = label }
                    .testTag("chat-room-more"),
                contentAlignment = Alignment.Center,
            ) { MoreDots(ink.text) }
        } else {
            Spacer(Modifier.size(44.dp))
        }
    }
}

@Composable
internal fun ChatBackButton(onBack: () -> Unit) {
    val ink = crewInk()
    Box(
        Modifier.size(width = 44.dp, height = 48.dp).clip(RoundedCornerShape(12.dp))
            .feedbackClickable(role = Role.Button, cue = FeedbackCue.Back, onClick = onBack).testTag("chat-back"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back), tint = ink.text, modifier = Modifier.size(22.dp))
    }
}

/** 가로 점 셋(시안의 •••) */
@Composable
private fun MoreDots(color: Color) {
    Canvas(Modifier.size(width = 22.dp, height = 6.dp)) {
        val r = 1.7.dp.toPx()
        val y = size.height / 2
        listOf(0.12f, 0.5f, 0.88f).forEach { drawCircle(color, r, Offset(size.width * it, y)) }
    }
}

// ── 고정 공지 · 재접속 · 새 메시지 ─────────────────────────────

/**
 * 상단 고정 공지 — 본문을 누르면 08 공지. 크루장에게만 오른쪽 "관리"를 따로 둔다(11). 관리를 누르면 본문 이동이 함께
 * 일어나지 않게 두 곳을 나란히 둔다.
 */
@Composable
internal fun ChatPinnedNotice(title: String, owner: Boolean, onOpen: () -> Unit, onManage: () -> Unit, modifier: Modifier = Modifier) {
    val ink = crewInk()
    val chat = chatInk()
    Row(
        modifier.fillMaxWidth().heightIn(min = 61.dp).clip(RoundedCornerShape(13.dp)).background(chat.pinned).testTag("chat-pinned"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).heightIn(min = 61.dp).feedbackClickable(role = Role.Button, onClick = onOpen)
                .padding(start = 13.dp, end = if (owner) 4.dp else 10.dp, top = 10.dp, bottom = 10.dp).testTag("chat-pinned-open"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MegaphoneIcon(ink.info, Modifier.size(21.dp))
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.chat_notice_label), color = ink.info, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(title, color = ink.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!owner) Icon(Icons.Filled.ChevronRight, null, tint = ink.secondary, modifier = Modifier.size(18.dp))
        }
        if (owner) {
            Box(
                Modifier.size(width = 58.dp, height = 61.dp).feedbackClickable(role = Role.Button, onClick = onManage)
                    .testTag("chat-pinned-manage"),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    stringResource(R.string.chat_notice_manage), color = ink.info, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

/** 시안의 확성기(선) */
@Composable
private fun MegaphoneIcon(color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val s = size.width / 24f
        val stroke = Stroke(width = 1.6f * s * 1.4f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(4f * s, 9f * s); lineTo(17f * s, 4f * s); lineTo(17f * s, 20f * s); lineTo(4f * s, 15f * s); close()
            moveTo(4f * s, 10f * s); lineTo(2f * s, 10f * s); lineTo(2f * s, 14f * s); lineTo(4f * s, 14f * s)
            moveTo(7f * s, 16f * s); lineTo(8f * s, 21f * s); lineTo(12f * s, 21f * s); lineTo(10f * s, 17f * s)
            moveTo(21f * s, 8f * s); lineTo(23f * s, 6f * s)
            moveTo(21f * s, 16f * s); lineTo(23f * s, 18f * s)
        }
        drawPath(path, color, style = stroke)
    }
}

/** 20 다시 연결하는 중 — 쓰던 글은 남아 있다 */
@Composable
internal fun ChatReconnectBanner(modifier: Modifier = Modifier) {
    val ink = crewInk()
    val chat = chatInk()
    Box(
        modifier.fillMaxWidth().heightIn(min = 29.dp).clip(RoundedCornerShape(8.dp)).background(chat.banner)
            .padding(horizontal = 12.dp, vertical = 6.dp).testTag("chat-reconnecting"),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.chat_reconnecting), color = ink.info, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

/** 21 새 메시지 N개 — 누르면 맨 아래(최신)로 */
@Composable
internal fun ChatNewMessagesButton(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val chat = chatInk()
    Box(modifier.heightIn(min = 44.dp).feedbackClickable(role = Role.Button, onClick = onClick).testTag("chat-new-messages"), contentAlignment = Alignment.Center) {
        Row(
            Modifier.widthIn(min = 178.dp).height(36.dp).clip(RoundedCornerShape(18.dp)).background(chat.jump).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(R.string.chat_new_messages, count), color = chat.jumpText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(10.dp))
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = chat.jumpText, modifier = Modifier.size(18.dp))
        }
    }
}

// ── 배지 · 동그라미 ────────────────────────────────────────────

/** 크루장 라벨(40×19) */
@Composable
internal fun ChatOwnerBadge(modifier: Modifier = Modifier) {
    val chat = chatInk()
    Box(
        modifier.heightIn(min = 19.dp).widthIn(min = 40.dp).clip(RoundedCornerShape(5.dp)).background(chat.badge)
            .padding(horizontal = 6.dp, vertical = 2.dp).testTag("chat-owner-badge"),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.chat_owner_badge), color = chat.badgeText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** 목록의 미확인 수(21) — 100 이상은 99+ */
@Composable
internal fun ChatUnreadBadge(count: Int, modifier: Modifier = Modifier) {
    val chat = chatInk()
    Box(
        modifier.heightIn(min = 21.dp).widthIn(min = 21.dp).clip(RoundedCornerShape(10.5.dp)).background(chat.unread)
            .padding(horizontal = 5.dp).testTag("chat-unread"),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (count > 99) "99+" else count.toString(), color = chat.unreadText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** 말풍선 옆 첫 글자 동그라미 — 지금의 크루장은 따뜻한 색 */
@Composable
internal fun ChatFace(name: String?, owner: Boolean, size: Dp, modifier: Modifier = Modifier) {
    val chat = chatInk()
    Box(
        modifier.size(size).clip(CircleShape).background(if (owner) chat.ownerFace else chat.memberFace),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            ChatRules.initial(name), color = if (owner) chat.ownerFaceText else chat.memberFaceText,
            fontSize = (size.value * 0.34f).sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
        )
    }
}

// ── 대화 줄 ────────────────────────────────────────────────────

/** 날짜 줄 — "9월 28일 월요일" */
@Composable
internal fun ChatDayLine(day: LocalDate) {
    val ink = crewInk()
    val pattern = stringResource(R.string.chat_day_pattern)
    val text = remember(day, pattern) { runCatching { day.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault())) }.getOrDefault(day.toString()) }
    Text(
        text, color = ink.secondary, fontSize = 11.sp, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 10.dp).testTag("chat-day"),
    )
}

/** 알림 줄 — 새 공지(정보색) · 크루를 떠남(보조색) */
@Composable
internal fun ChatSystemLine(message: ChatMessage) {
    val ink = crewInk()
    val name = message.eventName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.chat_unknown_name)
    val (text, color) = when (message.event) {
        ChatEvent.NOTICE_CREATED -> stringResource(R.string.chat_line_notice, name) to ink.info
        ChatEvent.MEMBER_LEFT -> stringResource(R.string.chat_line_left, name) to ink.secondary
        null -> return
    }
    Text(
        text, color = color, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 16.sp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp).testTag("chat-line"),
    )
}

/** 보낸 시각 "19:05" */
internal fun chatTime(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(millis).atZone(zone).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))

/** "9월 28일 · 19:05" 모양의 날짜 — 언어별 짧은 날짜 */
internal fun chatDate(millis: Long, locale: Locale = Locale.getDefault(), zone: ZoneId = ZoneId.systemDefault()): String {
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "MMMd")
    return Instant.ofEpochMilli(millis).atZone(zone).format(DateTimeFormatter.ofPattern(pattern, locale))
}

/** 말풍선의 글 — 지웠으면 "삭제된 메시지예요.", 숨겼으면 "크루장이 숨긴 메시지예요."(내용은 서버가 보내지 않는다) */
@Composable
internal fun chatBodyText(message: ChatMessage): String? = when (message.state) {
    ChatState.DELETED -> stringResource(R.string.chat_deleted)
    ChatState.HIDDEN -> stringResource(R.string.chat_hidden)
    ChatState.VISIBLE -> message.body?.takeIf { it.isNotBlank() }
}

/** 답장 원문 한 줄 — 지웠거나 숨겼으면 그 상태만 */
@Composable
internal fun chatReplyText(reply: ChatReply): String = when {
    reply.state == ChatState.DELETED -> stringResource(R.string.chat_deleted)
    reply.state == ChatState.HIDDEN -> stringResource(R.string.chat_hidden)
    reply.kind == ChatKind.IMAGE && reply.body.isNullOrBlank() -> stringResource(R.string.chat_photo)
    else -> ChatRules.oneLine(reply.body.orEmpty())
}

/**
 * 말풍선 한 개 — 남의 메시지는 왼쪽(동그라미 · 이름 · 크루장 라벨), 내 메시지는 오른쪽. 크루장인 내 메시지 위에는 "나 크루장".
 * 길게 누르면 메뉴, 사진을 누르면 크게 보기, 답장 원문을 누르면 그 메시지로.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatBubbleRow(
    crewId: String,
    message: ChatMessage,
    mine: Boolean,
    header: Boolean,
    time: Boolean,
    owner: Boolean,
    unread: Int?,
    highlight: Boolean,
    maxBubble: Dp,
    onLongPress: () -> Unit,
    onFace: () -> Unit,
    onPhoto: () -> Unit,
    onReply: (ChatReply) -> Unit,
    onRetry: () -> Unit,
    onDiscard: () -> Unit,
) {
    val ink = crewInk()
    val chat = chatInk()
    val gone = message.state != ChatState.VISIBLE
    val background = if (gone) chat.gone else if (mine) chat.outgoing else chat.incoming
    val textColor = if (gone) ink.secondary else if (mine) chat.outgoingText else chat.incomingText
    val menuLabel = stringResource(R.string.chat_menu_title)
    Box(
        Modifier.fillMaxWidth().background(if (highlight) chat.highlight else Color.Transparent)
            .padding(horizontal = ChatGutter).padding(top = if (header) 12.dp else 4.dp, bottom = if (time) 8.dp else 2.dp)
            .testTag(if (mine) "chat-bubble-mine" else "chat-bubble-other"),
    ) {
        if (!mine) {
            Row(Modifier.fillMaxWidth()) {
                Box(Modifier.width(44.dp)) {
                    if (header) {
                        Box(
                            Modifier.size(width = 40.dp, height = 44.dp).offset(x = (-4).dp).clip(RoundedCornerShape(12.dp))
                                .feedbackClickable(role = Role.Button, onClick = onFace).testTag("chat-face"),
                            contentAlignment = Alignment.TopCenter,
                        ) { ChatFace(message.authorName, owner, 32.dp, Modifier.padding(top = 3.dp)) }
                    }
                }
                Column(Modifier.weight(1f)) {
                    if (header) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                            Text(
                                message.authorName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.chat_unknown_name),
                                color = ink.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (owner) {
                                Spacer(Modifier.width(8.dp))
                                ChatOwnerBadge()
                            }
                        }
                    }
                    BubbleBody(crewId, message, mine = false, background, textColor, maxBubble, onLongPress, onPhoto, onReply, menuLabel)
                    if (time) {
                        Text(chatTime(message.createdAt), color = ink.secondary, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                Column(Modifier.width(IntrinsicSize.Max)) {
                    if (header) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                            Text(stringResource(R.string.chat_me), color = ink.secondary, fontSize = 11.sp)
                            if (owner) {
                                Spacer(Modifier.width(8.dp))
                                ChatOwnerBadge()
                            }
                        }
                    }
                    BubbleBody(crewId, message, mine = true, background, textColor, maxBubble, onLongPress, onPhoto, onReply, menuLabel)
                }
                when (message.delivery) {
                    ChatDelivery.SENT -> if (time) {
                        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (unread != null) {
                                Text(unread.toString(), color = ink.info, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("chat-read-count"))
                                Spacer(Modifier.width(22.dp))
                            }
                            Text(chatTime(message.createdAt), color = ink.secondary, fontSize = 10.sp)
                        }
                    }
                    ChatDelivery.SENDING -> CircularProgressIndicator(
                        Modifier.padding(top = 6.dp).size(12.dp).testTag("chat-sending"), color = ink.secondary, strokeWidth = 1.5.dp,
                    )
                    ChatDelivery.PENDING -> Text(
                        stringResource(R.string.chat_send_pending), color = ink.secondary, fontSize = 10.sp,
                        modifier = Modifier.padding(top = 6.dp).testTag("chat-pending"),
                    )
                    ChatDelivery.FAILED -> ChatFailedActions(onRetry, onDiscard)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BubbleBody(
    crewId: String,
    message: ChatMessage,
    mine: Boolean,
    background: Color,
    textColor: Color,
    maxBubble: Dp,
    onLongPress: () -> Unit,
    onPhoto: () -> Unit,
    onReply: (ChatReply) -> Unit,
    menuLabel: String,
) {
    val ink = crewInk()
    val chat = chatInk()
    val text = chatBodyText(message)
    val photo = message.kind == ChatKind.IMAGE && message.state == ChatState.VISIBLE
    val press = Modifier.combinedClickable(
        enabled = message.id != null,
        role = Role.Button,
        onLongClickLabel = menuLabel,
        onLongClick = onLongPress,
        onClick = { if (photo) onPhoto() },
    )
    Column(Modifier.widthIn(max = maxBubble).testTag("chat-bubble")) {
        if (photo) {
            ChatPhotoBox(crewId, message, Modifier.width(maxBubble.coerceAtMost(200.dp)).then(press), mine)
            if (text != null) {
                Box(
                    Modifier.width(maxBubble.coerceAtMost(200.dp)).clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                        .background(background).then(press).padding(horizontal = 13.dp, vertical = 12.dp),
                ) { Text(text, color = textColor, fontSize = 13.sp, lineHeight = 19.sp) }
            }
            return@Column
        }
        Column(
            Modifier.clip(RoundedCornerShape(14.dp)).background(background).then(press).padding(horizontal = 14.dp, vertical = 11.dp),
        ) {
            val reply = message.reply
            if (reply != null && message.state == ChatState.VISIBLE) {
                Row(
                    Modifier.padding(bottom = 7.dp).clip(RoundedCornerShape(6.dp))
                        .feedbackClickable(role = Role.Button) { onReply(reply) }.padding(vertical = 2.dp).testTag("chat-quote"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(2.dp).height(28.dp).clip(RoundedCornerShape(1.dp)).background(if (mine) chat.outgoingText.copy(alpha = .5f) else ink.info))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            stringResource(R.string.chat_reply_to, reply.authorName.ifBlank { stringResource(R.string.chat_unknown_name) }),
                            color = if (mine) chat.outgoingText else ink.info, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                        )
                        Text(
                            chatReplyText(reply), color = textColor.copy(alpha = .72f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Text(
                text ?: "", color = textColor, fontSize = 15.sp, lineHeight = 23.sp,
                style = TextStyle(fontFamily = StepUpSans),
                modifier = Modifier.testTag(if (message.state == ChatState.VISIBLE) "chat-text" else "chat-text-gone"),
            )
        }
    }
}

/** 사진 말풍선 — 받은 사진은 방 권한으로 서버에서 읽는다(지웠거나 숨긴 사진은 서버가 주지 않는다) */
@Composable
private fun ChatPhotoBox(crewId: String, message: ChatMessage, modifier: Modifier, mine: Boolean) {
    val ink = crewInk()
    val chat = chatInk()
    val id = message.id
    var bitmap by remember(id, message.localImage) {
        mutableStateOf<ImageBitmap?>(id?.let { ServiceLocator.crewChat.imageNow(it)?.asImageBitmap() })
    }
    var failed by remember(id) { mutableStateOf(false) }
    LaunchedEffect(id, message.localImage) {
        if (bitmap != null) return@LaunchedEffect
        val local = message.localImage
        bitmap = if (id == null && local != null) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { ServiceLocator.crewChat.decodeFile(java.io.File(local))?.asImageBitmap() }
        } else if (id != null) {
            val outcome = ServiceLocator.crewChat.image(crewId, id)
            (outcome as? com.stepup.android.data.repo.ChatOutcome.Ok)?.value?.asImageBitmap().also { failed = it == null }
        } else null
    }
    val shape = if (message.body.isNullOrBlank()) RoundedCornerShape(14.dp) else RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    val image = bitmap
    val ratio = if (image != null && image.width > 0) (image.height.toFloat() / image.width).coerceIn(0.56f, 1.3f) else 0.9f
    Box(modifier.aspectRatio(1f / ratio).clip(shape).background(if (mine) chat.gone else chat.incoming).testTag("chat-photo")) {
        when {
            image != null -> Image(image, contentDescription = stringResource(R.string.chat_photo), contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
            failed -> Text(stringResource(R.string.chat_photo), color = ink.secondary, fontSize = 12.sp, modifier = Modifier.align(Alignment.Center))
            else -> CircularProgressIndicator(Modifier.size(18.dp).align(Alignment.Center), color = ink.secondary, strokeWidth = 2.dp)
        }
    }
}

/** 19 전송 실패 — "전송 실패" 아래 [재전송 · 지우기](지우기는 이 폰의 실패 항목만) */
@Composable
private fun ChatFailedActions(onRetry: () -> Unit, onDiscard: () -> Unit) {
    val ink = crewInk()
    val chat = chatInk()
    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(top = 6.dp).testTag("chat-failed")) {
        Text(stringResource(R.string.chat_send_failed), color = ink.secondary, fontSize = 10.sp)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(10.dp)).background(chat.failed),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.heightIn(min = 48.dp).feedbackClickable(role = Role.Button, onClick = onRetry).padding(start = 12.dp, end = 14.dp)
                    .testTag("chat-retry"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Refresh, null, tint = ink.warn, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(16.dp))
                Text(stringResource(R.string.chat_retry), color = ink.warn, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(
                Modifier.heightIn(min = 48.dp).widthIn(min = 64.dp).feedbackClickable(role = Role.Button, onClick = onDiscard)
                    .padding(horizontal = 14.dp).testTag("chat-discard"),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.chat_discard), color = ink.secondary, fontSize = 12.sp) }
        }
    }
}

// ── 입력 ─────────────────────────────────────────────────────

/**
 * 입력창 — [+ 사진] [여러 줄 글] [보내기]. 보내기 버튼을 눌렀을 때만 보낸다(Enter 는 줄바꿈 · 한국어 조합 그대로).
 * 공백만 있거나 연결이 끊겼으면 보내기 · 사진이 꺼진다. 쓰던 글은 끊겨도 계속 고칠 수 있다.
 */
@Composable
internal fun ChatComposer(
    text: String,
    onText: (String) -> Unit,
    /** 글을 쓸 수 있다(대화를 못 불러왔으면 36 처럼 막는다) */
    editable: Boolean,
    /** 보내기 · 사진을 쓸 수 있다(방을 받았고 연결돼 있다) */
    active: Boolean,
    /** 빈 입력칸에 "연결되면 보낼 수 있어요" */
    offline: Boolean,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    replyTo: ChatMessage?,
    onCancelReply: () -> Unit,
) {
    val ink = crewInk()
    val chat = chatInk()
    val canSend = active && ChatRules.sendable(text)
    val canAttach = active
    Column(Modifier.fillMaxWidth().background(ink.canvas).testTag("chat-composer")) {
        Box(Modifier.padding(horizontal = ChatGutter).fillMaxWidth().height(0.7.dp).background(ink.divider))
        if (replyTo != null) {
            Row(Modifier.fillMaxWidth().padding(start = 59.dp, end = 8.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).heightIn(min = 47.dp).clip(RoundedCornerShape(10.dp)).background(chat.reply)
                        .padding(horizontal = 10.dp, vertical = 8.dp).testTag("chat-reply-bar"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(2.dp).height(28.dp).clip(RoundedCornerShape(1.dp)).background(ink.info))
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.chat_reply_to, replyTo.authorName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.chat_unknown_name)),
                            color = ink.info, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                        )
                        Text(
                            if (replyTo.kind == ChatKind.IMAGE && replyTo.body.isNullOrBlank()) stringResource(R.string.chat_photo)
                            else ChatRules.oneLine(replyTo.body.orEmpty()),
                            color = ink.secondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Box(
                    Modifier.size(44.dp).clip(CircleShape).feedbackClickable(role = Role.Button, cue = FeedbackCue.Back, onClick = onCancelReply)
                        .testTag("chat-reply-cancel"),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Close, stringResource(R.string.chat_reply_cancel), tint = ink.secondary, modifier = Modifier.size(20.dp)) }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 10.dp, end = ChatGutter, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(
                Modifier.size(width = 44.dp, height = 48.dp).clip(RoundedCornerShape(12.dp))
                    .feedbackClickable(enabled = canAttach, role = Role.Button, onClick = onAttach).testTag("chat-attach"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Add, stringResource(R.string.chat_attach), tint = if (canAttach) ink.secondary else ink.secondary.copy(alpha = .45f),
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(5.dp))
            BasicTextField(
                value = text,
                onValueChange = onText,
                enabled = editable,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                textStyle = TextStyle(fontFamily = StepUpSans, color = ink.text, fontSize = 14.sp, lineHeight = 21.sp),
                cursorBrush = SolidColor(ink.info),
                modifier = Modifier.weight(1f).testTag("chat-input"),
                decorationBox = { inner ->
                    Box(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(22.dp)).background(chat.composer)
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (text.isEmpty()) {
                            Text(
                                stringResource(if (offline) R.string.chat_input_offline_hint else R.string.chat_input_hint),
                                color = ink.secondary, fontSize = 14.sp, maxLines = 1,
                            )
                        }
                        inner()
                    }
                },
            )
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier.size(width = 44.dp, height = 48.dp).feedbackClickable(enabled = canSend, role = Role.Button, onClick = onSend)
                    .testTag("chat-send"),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(width = 40.dp, height = 44.dp).clip(RoundedCornerShape(20.dp)).background(if (canSend) chat.sendOn else chat.sendOff),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.ArrowUpward, stringResource(R.string.chat_send), tint = if (canSend) chat.sendOnIcon else chat.sendOffIcon,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

// ── 시트 줄 · 결과 ──────────────────────────────────────────────

/** 시트 안의 한 줄(메시지 메뉴 · 신고 사유 · 알림) — 이름, 오른쪽 "선택됨", 꺾쇠, 아래 선 */
@Composable
internal fun ChatSheetRow(title: String, onClick: () -> Unit, tag: String, warn: Boolean = false, selected: Boolean = false, enabled: Boolean = true) {
    val ink = crewInk()
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(vertical = 10.dp).testTag(tag),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title, color = if (warn) ink.warn else ink.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            if (selected) Text(stringResource(R.string.chat_selected), color = ink.info, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
            Icon(Icons.Filled.ChevronRight, null, tint = ink.secondary, modifier = Modifier.padding(start = 4.dp).size(18.dp))
        }
        Box(Modifier.fillMaxWidth().height(0.7.dp).background(ink.divider))
    }
}

/** 크루의 방 정보를 크루 명함형 부품 모양으로 */
internal fun ChatRoomMeta?.cardOr(card: CrewCard?): CrewCard? = this?.card ?: card

/** 불러오기 실패(36)의 가운데 — 다시 불러오기 */
@Composable
internal fun ChatRetryState(title: String, body: String, button: String, onRetry: () -> Unit, modifier: Modifier = Modifier, tag: String) {
    val ink = crewInk()
    Column(modifier.fillMaxWidth().padding(horizontal = CrewGutter).testTag(tag), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.Refresh, null, tint = ink.info, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(26.dp))
        Text(title, color = ink.text, fontSize = 24.sp, lineHeight = 31.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text(body, color = ink.secondary, fontSize = 13.5.sp, lineHeight = 21.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        com.stepup.android.ui.screens.community.crew.CrewButton(button, onRetry, Modifier.testTag("$tag-retry"))
    }
}

/** 복사 — 고른 메시지 내용만 */
internal fun copyChatText(context: Context, text: String) {
    runCatching {
        context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("StepUp", text))
    }
}
