package com.stepup.android.ui.screens.community.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.Delete
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
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.community.crew.CrewImage
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.StepUpSans
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * 크루 채팅 부품 — 파란 톤 전달본(2026-10-03 v4, stepup-crew-chat-blue-claude-v19)의 색 · 크기:
 * 남색 바닥 · 파란 말풍선(남) · 밝은 말풍선(나) · 청록 강조 · 대화 좌우 16 · 말풍선 모서리 18 · 본문 16 · 입력칸 52 · 누르는 곳 48.
 * 두 영역 공통 부품([blueInk] · BlueButton · BlueSheet — CrewBlueKit.kt)을 쓰고, 채팅에만 있는 색을 더한다.
 * 이모지 · 왕관 · 온라인 수 · '입력 중' 표시는 두지 않는다(지원하지 않는 상태를 꾸미지 않는다).
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
    /** 전송 대기 · 실패 말풍선(내 것) — 아직 서버가 받았다고 확인하지 않은 상태 */
    val pending: Color,
    val pendingText: Color,
)

private val DarkChatInk = ChatInk(
    incoming = Color(0xFF0B2E5C), incomingText = Color(0xFFF5F8FF), outgoing = Color(0xFFE8F4FF), outgoingText = Color(0xFF06274D),
    gone = Color(0xFF0A2343), pinned = Color(0xFF0A2A55), badge = Color(0xFF0B3566), badgeText = Color(0xFF48D9FA),
    unread = Color(0xFF0754FF), unreadText = Color(0xFFF5F8FF), composer = Color(0xFF0B2B55), sendOff = Color(0xFF1A3E70),
    sendOffIcon = Color(0xFF7FA2D8), sendOn = Color(0xFFF5F8FF), sendOnIcon = Color(0xFF0754FF), reply = Color(0xFF0A2547),
    banner = Color(0xFF0A2547), jump = Color(0xFF061D3B), jumpText = Color(0xFF48D9FA), failed = Color(0xFF061D3B),
    ownerFace = Color(0xFF4A3330), ownerFaceText = Color(0xFFF3DCCB), memberFace = Color(0xFF0D3A75), memberFaceText = Color(0xFFBFD6FF),
    highlight = Color(0x2E48D9FA), pending = Color(0xFF9CC7F5), pendingText = Color(0xFF06274D),
)

private val LightChatInk = ChatInk(
    incoming = Color(0xFFFFFFFF), incomingText = Color(0xFF0B1E3F), outgoing = Color(0xFF0754FF), outgoingText = Color(0xFFFFFFFF),
    gone = Color(0xFFE8EEF8), pinned = Color(0xFFFFFFFF), badge = Color(0xFFE1EBFF), badgeText = Color(0xFF0748D6),
    unread = Color(0xFF0754FF), unreadText = Color(0xFFFFFFFF), composer = Color(0xFFFFFFFF), sendOff = Color(0xFFD5DCE8),
    sendOffIcon = Color(0xFF7B8AA0), sendOn = Color(0xFF0754FF), sendOnIcon = Color(0xFFFFFFFF), reply = Color(0xFFEDF2FC),
    banner = Color(0xFFEDF2FC), jump = Color(0xFFFFFFFF), jumpText = Color(0xFF0748D6), failed = Color(0xFFFFFFFF),
    ownerFace = Color(0xFFF1E1D8), ownerFaceText = Color(0xFF6B4533), memberFace = Color(0xFFE1EBFF), memberFaceText = Color(0xFF0748D6),
    highlight = Color(0x220754FF), pending = Color(0xFF8DB2F5), pendingText = Color(0xFF0B1E3F),
)

@Composable
internal fun chatInk(): ChatInk = if (StepUpColors.dark) DarkChatInk else LightChatInk

/** 대화 화면의 좌우 */
internal val ChatGutter = 16.dp

// ── 머리 ─────────────────────────────────────────────────────

/** 02 · 03 머리 — 뒤로(공통 머리) · 크루 이미지 · 이름 · "크루원 25명"(지금 크루원 수) · 더보기(04 · 05) */
@Composable
internal fun ChatRoomHeader(card: CrewCard?, memberCount: Int?, onBack: () -> Unit, onInfo: (() -> Unit)?) {
    val ink = blueInk()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BlueBackButton(onBack)
        if (card == null) {
            // 36 — 방 정보가 아직 없다: 가짜 크루 이름 · 인원 없이 "크루 채팅"만
            Text(
                stringResource(R.string.chat_ended_bar), style = blueText(19.sp, ink.text, FontWeight.Bold, 1.25f),
                textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.weight(1f).testTag("chat-room-title"),
            )
        } else {
            Row(
                Modifier.weight(1f).heightIn(min = 60.dp).clip(RoundedCornerShape(12.dp))
                    .feedbackClickable(enabled = onInfo != null, role = Role.Button) { onInfo?.invoke() }
                    .padding(horizontal = 4.dp, vertical = 4.dp).testTag("chat-room-title"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CrewImage(card, 56.dp, 13.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(card.name, style = blueText(22.sp, ink.text, FontWeight.ExtraBold, 1.25f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (memberCount != null) {
                        Text(
                            stringResource(R.string.chat_members_count, memberCount), style = blueText(15.sp, ink.secondary, FontWeight.Medium, 1.3f),
                            maxLines = 1, modifier = Modifier.testTag("chat-room-count"),
                        )
                    }
                }
            }
        }
        if (onInfo != null) {
            BlueMoreButton(onInfo, Modifier.testTag("chat-room-more"))
        } else {
            Spacer(Modifier.size(48.dp))
        }
    }
}

// ── 고정 공지 · 재접속 · 새 메시지 ─────────────────────────────

/**
 * 상단 고정 공지 — 본문을 누르면 08 공지. 크루장에게만 오른쪽 "관리"를 따로 둔다(11). 관리를 누르면 본문 이동이 함께
 * 일어나지 않게 두 곳을 나란히 둔다.
 */
@Composable
internal fun ChatPinnedNotice(title: String, owner: Boolean, onOpen: () -> Unit, onManage: () -> Unit, modifier: Modifier = Modifier) {
    val ink = blueInk()
    val chat = chatInk()
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp).clip(shape).background(chat.pinned, shape).border(1.5.dp, ink.edgeStrong, shape)
            .testTag("chat-pinned"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).heightIn(min = 64.dp).feedbackClickable(role = Role.Button, onClick = onOpen)
                .padding(start = 16.dp, end = if (owner) 4.dp else 12.dp, top = 10.dp, bottom = 10.dp).testTag("chat-pinned-open"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MegaphoneIcon(ink.info, Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.chat_notice_label), style = blueText(13.sp, ink.info, FontWeight.Bold, 1.2f))
                Spacer(Modifier.height(2.dp))
                Text(title, style = blueText(16.sp, ink.text, FontWeight.Bold, 1.3f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!owner) Icon(Icons.Filled.ChevronRight, null, tint = ink.info, modifier = Modifier.size(24.dp))
        }
        if (owner) {
            Row(
                Modifier.heightIn(min = 64.dp).feedbackClickable(role = Role.Button, onClick = onManage)
                    .padding(start = 8.dp, end = 12.dp).testTag("chat-pinned-manage"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.chat_notice_manage), style = blueText(15.sp, ink.info, FontWeight.Bold))
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Filled.ChevronRight, null, tint = ink.info, modifier = Modifier.size(24.dp))
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
    val ink = blueInk()
    val chat = chatInk()
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp).clip(shape).background(chat.banner, shape).border(1.dp, ink.edgeStrong, shape)
            .padding(horizontal = 16.dp, vertical = 10.dp).testTag("chat-reconnecting")
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RunSpinner(Modifier.size(30.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.chat_blue_reconnecting_title), style = blueText(15.sp, ink.text, FontWeight.Bold, 1.3f))
            Text(stringResource(R.string.chat_blue_reconnecting_body), style = blueText(14.sp, ink.info, FontWeight.Medium, 1.3f))
        }
    }
}

/** 21 새 메시지 N개 — 누르면 맨 아래(최신)로. 읽던 위치를 자동으로 밀지 않는다 */
@Composable
internal fun ChatNewMessagesButton(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ink = blueInk()
    val chat = chatInk()
    val shape = RoundedCornerShape(20.dp)
    Box(modifier.heightIn(min = 48.dp).feedbackClickable(role = Role.Button, onClick = onClick).testTag("chat-new-messages"), contentAlignment = Alignment.Center) {
        Row(
            Modifier.widthIn(min = 170.dp).height(40.dp).clip(shape).background(chat.jump, shape).border(1.5.dp, ink.info, shape)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(R.string.chat_new_messages, count), style = blueText(14.sp, chat.jumpText, FontWeight.Bold))
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = chat.jumpText, modifier = Modifier.size(20.dp))
        }
    }
}

// ── 배지 · 동그라미 ────────────────────────────────────────────

/** 크루장 라벨(40×19) */
@Composable
internal fun ChatOwnerBadge(modifier: Modifier = Modifier) {
    val chat = chatInk()
    Box(
        modifier.heightIn(min = 22.dp).widthIn(min = 44.dp).clip(RoundedCornerShape(6.dp)).background(chat.badge)
            .padding(horizontal = 7.dp, vertical = 2.dp).testTag("chat-owner-badge"),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.chat_owner_badge), style = blueText(12.sp, chat.badgeText, FontWeight.Bold, 1.2f), maxLines = 1)
    }
}

/** 목록의 미확인 수(21) — 100 이상은 99+ */
@Composable
internal fun ChatUnreadBadge(count: Int, modifier: Modifier = Modifier) {
    val chat = chatInk()
    Box(
        modifier.heightIn(min = 26.dp).widthIn(min = 26.dp).clip(RoundedCornerShape(13.dp)).background(chat.unread)
            .padding(horizontal = 7.dp).testTag("chat-unread"),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (count > 99) "99+" else count.toString(), style = blueText(13.sp, chat.unreadText, FontWeight.Bold, 1.2f), maxLines = 1)
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
            fontSize = (size.value * 0.38f).sp, fontWeight = FontWeight.Bold, maxLines = 1,
        )
    }
}

// ── 대화 줄 ────────────────────────────────────────────────────

/** 날짜 줄 — "9월 28일 월요일" */
@Composable
internal fun ChatDayLine(day: LocalDate) {
    val ink = blueInk()
    val pattern = stringResource(R.string.chat_day_pattern)
    val text = remember(day, pattern) { runCatching { day.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault())) }.getOrDefault(day.toString()) }
    Text(
        text, style = blueText(13.sp, ink.secondary, FontWeight.Medium), textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp).testTag("chat-day"),
    )
}

/** 알림 줄 — 새 공지(정보색) · 크루를 떠남(보조색) */
@Composable
internal fun ChatSystemLine(message: ChatMessage) {
    val ink = blueInk()
    val name = message.eventName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.chat_unknown_name)
    val (text, color) = when (message.event) {
        ChatEvent.NOTICE_CREATED -> stringResource(R.string.chat_line_notice, name) to ink.info
        ChatEvent.MEMBER_LEFT -> stringResource(R.string.chat_line_left, name) to ink.secondary
        null -> return
    }
    Text(
        text, style = blueText(13.5.sp, color, FontWeight.Medium), textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).testTag("chat-line"),
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
    val ink = blueInk()
    val chat = chatInk()
    val gone = message.state != ChatState.VISIBLE
    // 전송 대기 · 실패는 서버가 받았다고 확인하지 않은 내 말풍선 — 성공한 말풍선과 다른 옅은 파랑
    val unconfirmed = mine && !gone && (message.delivery == ChatDelivery.PENDING || message.delivery == ChatDelivery.FAILED)
    val background = when {
        gone -> chat.gone
        unconfirmed -> chat.pending
        mine -> chat.outgoing
        else -> chat.incoming
    }
    val textColor = when {
        gone -> ink.secondary
        unconfirmed -> chat.pendingText
        mine -> chat.outgoingText
        else -> chat.incomingText
    }
    val menuLabel = stringResource(R.string.chat_menu_title)
    Box(
        Modifier.fillMaxWidth().background(if (highlight) chat.highlight else Color.Transparent)
            .padding(horizontal = ChatGutter).padding(top = if (header) 12.dp else 4.dp, bottom = if (time) 8.dp else 2.dp)
            .testTag(if (mine) "chat-bubble-mine" else "chat-bubble-other"),
    ) {
        if (!mine) {
            Row(Modifier.fillMaxWidth()) {
                Box(Modifier.width(52.dp)) {
                    if (header) {
                        Box(
                            Modifier.size(width = 48.dp, height = 48.dp).offset(x = (-4).dp).clip(CircleShape)
                                .feedbackClickable(role = Role.Button, onClick = onFace).testTag("chat-face"),
                            contentAlignment = Alignment.Center,
                        ) { ChatFace(message.authorName, owner, 40.dp) }
                    }
                }
                Column(Modifier.weight(1f)) {
                    if (header) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                            Text(
                                message.authorName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.chat_unknown_name),
                                style = blueText(15.sp, ink.text, FontWeight.Bold, 1.25f), maxLines = 1, overflow = TextOverflow.Ellipsis,
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
                        Text(chatTime(message.createdAt), style = blueText(13.sp, ink.secondary, FontWeight.Medium), modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                Column(Modifier.width(IntrinsicSize.Max)) {
                    if (header) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                            Text(stringResource(R.string.chat_me), style = blueText(13.sp, ink.secondary, FontWeight.Medium))
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
                        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (unread != null) {
                                Text(unread.toString(), style = blueText(13.sp, ink.info, FontWeight.Bold), modifier = Modifier.testTag("chat-read-count"))
                                Spacer(Modifier.width(14.dp))
                            }
                            Text(chatTime(message.createdAt), style = blueText(13.sp, ink.secondary, FontWeight.Medium))
                        }
                    }
                    ChatDelivery.SENDING -> RunSpinner(Modifier.padding(top = 6.dp).size(14.dp).testTag("chat-sending"))
                    ChatDelivery.PENDING -> Text(
                        stringResource(R.string.chat_send_pending), style = blueText(13.sp, ink.secondary, FontWeight.Medium),
                        modifier = Modifier.padding(top = 4.dp).testTag("chat-pending"),
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
    val ink = blueInk()
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
            ChatPhotoBox(crewId, message, Modifier.width(maxBubble.coerceAtMost(220.dp)).then(press), mine)
            if (text != null) {
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier.width(maxBubble.coerceAtMost(220.dp)).clip(RoundedCornerShape(16.dp))
                        .background(background).then(press).padding(horizontal = 16.dp, vertical = 12.dp),
                ) { Text(text, style = blueText(15.sp, textColor, FontWeight.Medium, 1.45f)) }
            }
            return@Column
        }
        Column(
            Modifier.clip(RoundedCornerShape(18.dp)).background(background).then(press).padding(horizontal = 16.dp, vertical = 12.dp),
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
                            style = blueText(12.5.sp, if (mine) chat.outgoingText else ink.info, FontWeight.Bold, 1.3f), maxLines = 1,
                        )
                        Text(
                            chatReplyText(reply), style = blueText(13.sp, textColor.copy(alpha = .75f), FontWeight.Medium, 1.3f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Text(
                text ?: "", style = blueText(16.sp, textColor, FontWeight.Medium, 1.5f),
                modifier = Modifier.testTag(if (message.state == ChatState.VISIBLE) "chat-text" else "chat-text-gone"),
            )
        }
    }
}

/** 사진 말풍선 — 받은 사진은 방 권한으로 서버에서 읽는다(지웠거나 숨긴 사진은 서버가 주지 않는다) */
@Composable
private fun ChatPhotoBox(crewId: String, message: ChatMessage, modifier: Modifier, mine: Boolean) {
    val ink = blueInk()
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
    val shape = RoundedCornerShape(18.dp)
    val image = bitmap
    val ratio = if (image != null && image.width > 0) (image.height.toFloat() / image.width).coerceIn(0.56f, 1.3f) else 0.9f
    Box(modifier.aspectRatio(1f / ratio).clip(shape).background(if (mine) chat.gone else chat.incoming).testTag("chat-photo")) {
        when {
            image != null -> Image(image, contentDescription = stringResource(R.string.chat_photo), contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
            failed -> Text(stringResource(R.string.chat_photo), style = blueText(13.sp, ink.secondary), modifier = Modifier.align(Alignment.Center))
            else -> RunSpinner(Modifier.size(20.dp).align(Alignment.Center))
        }
    }
}

/** 19 전송 실패 — "전송 실패" 아래 [재전송 · 지우기](지우기는 이 폰의 실패 항목만) */
@Composable
private fun ChatFailedActions(onRetry: () -> Unit, onDiscard: () -> Unit) {
    val ink = blueInk()
    val chat = chatInk()
    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(top = 6.dp).testTag("chat-failed")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Error, null, tint = ink.warn, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.chat_send_failed), style = blueText(14.sp, ink.warn, FontWeight.SemiBold))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FailedAction(Icons.Filled.Refresh, stringResource(R.string.chat_retry), onRetry, "chat-retry", chat.failed)
            FailedAction(Icons.Outlined.Delete, stringResource(R.string.chat_discard), onDiscard, "chat-discard", chat.failed)
        }
    }
}

/** 실패한 말풍선 아래 작은 테두리 버튼(재전송 · 지우기) */
@Composable
private fun FailedAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, tag: String, face: Color) {
    val ink = blueInk()
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.heightIn(min = 48.dp).clip(shape).background(face, shape).border(1.5.dp, ink.edgeStrong, shape)
            .feedbackClickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = ink.info, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = blueText(15.sp, ink.info, FontWeight.Bold))
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
    val ink = blueInk()
    val chat = chatInk()
    val canSend = active && ChatRules.sendable(text)
    val canAttach = active
    Column(Modifier.fillMaxWidth().testTag("chat-composer")) {
        Box(Modifier.padding(horizontal = ChatGutter).fillMaxWidth().height(1.dp).background(ink.edgeStrong.copy(alpha = 0.7f)))
        if (replyTo != null) {
            val shape = RoundedCornerShape(14.dp)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = ChatGutter).padding(top = 10.dp).clip(shape).background(chat.reply, shape)
                    .border(1.5.dp, ink.info, shape).padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(Modifier.weight(1f).heightIn(min = 44.dp).testTag("chat-reply-bar"), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(3.dp).height(32.dp).clip(RoundedCornerShape(2.dp)).background(ink.info))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.chat_reply_to, replyTo.authorName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.chat_unknown_name)),
                            style = blueText(14.sp, ink.info, FontWeight.Bold, 1.3f), maxLines = 1,
                        )
                        Text(
                            if (replyTo.kind == ChatKind.IMAGE && replyTo.body.isNullOrBlank()) stringResource(R.string.chat_photo)
                            else ChatRules.oneLine(replyTo.body.orEmpty()),
                            style = blueText(13.5.sp, ink.text.copy(alpha = 0.9f), FontWeight.Medium, 1.3f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Box(
                    Modifier.size(48.dp).clip(CircleShape).feedbackClickable(role = Role.Button, cue = FeedbackCue.Back, onClick = onCancelReply)
                        .testTag("chat-reply-cancel"),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Close, stringResource(R.string.chat_reply_cancel), tint = ink.info, modifier = Modifier.size(24.dp)) }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 6.dp, end = ChatGutter, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(
                Modifier.size(48.dp).clip(CircleShape)
                    .feedbackClickable(enabled = canAttach, role = Role.Button, onClick = onAttach).testTag("chat-attach"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Add, stringResource(R.string.chat_attach), tint = if (canAttach) ink.info else ink.secondary.copy(alpha = .45f),
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(Modifier.width(6.dp))
            BasicTextField(
                value = text,
                onValueChange = onText,
                enabled = editable,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                textStyle = blueText(16.sp, ink.text, FontWeight.Medium, 1.4f),
                cursorBrush = SolidColor(ink.info),
                modifier = Modifier.weight(1f).testTag("chat-input"),
                decorationBox = { inner ->
                    Box(
                        Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(26.dp)).background(chat.composer)
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (text.isEmpty()) {
                            Text(
                                stringResource(if (offline) R.string.chat_input_offline_hint else R.string.chat_input_hint),
                                style = blueText(16.sp, ink.secondary, FontWeight.Medium, 1.4f), maxLines = 1,
                            )
                        }
                        inner()
                    }
                },
            )
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.size(52.dp).clip(CircleShape).feedbackClickable(enabled = canSend, role = Role.Button, onClick = onSend)
                    .testTag("chat-send"),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(if (canSend) chat.sendOn else chat.sendOff),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.ArrowUpward, stringResource(R.string.chat_send), tint = if (canSend) chat.sendOnIcon else chat.sendOffIcon,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }
}

// ── 시트 줄 · 결과 ──────────────────────────────────────────────

/** 시트 안의 메뉴 한 줄(메시지 메뉴 · 사진 보내기 · 모임 고르기) — 이름, 오른쪽 "선택됨", 꺾쇠, 아래 선 */
@Composable
internal fun ChatSheetRow(title: String, onClick: () -> Unit, tag: String, warn: Boolean = false, selected: Boolean = false, enabled: Boolean = true) {
    val ink = blueInk()
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 60.dp).feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(start = 4.dp, end = 2.dp, top = 10.dp, bottom = 10.dp).testTag(tag),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title, style = blueText(17.sp, if (!enabled) ink.secondary else if (warn) ink.warn else ink.text, FontWeight.Bold, 1.3f),
                modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            if (selected) Text(stringResource(R.string.chat_selected), style = blueText(14.sp, ink.info, FontWeight.Bold), modifier = Modifier.padding(start = 8.dp))
            Icon(Icons.Filled.ChevronRight, null, tint = ink.secondary, modifier = Modifier.padding(start = 6.dp).size(24.dp))
        }
        BlueDivider()
    }
}

/** 시트 메뉴의 첫 선 — 제목과 줄 사이 */
@Composable
internal fun ChatSheetTopLine() {
    Spacer(Modifier.height(14.dp))
    BlueDivider()
}

/** 크루의 방 정보를 크루 명함형 부품 모양으로 */
internal fun ChatRoomMeta?.cardOr(card: CrewCard?): CrewCard? = this?.card ?: card

/** 불러오기 실패(36)의 가운데 — 다시 불러오기 */
@Composable
internal fun ChatRetryState(title: String, body: String, button: String, onRetry: () -> Unit, modifier: Modifier = Modifier, tag: String) {
    val ink = blueInk()
    Column(modifier.fillMaxWidth().padding(horizontal = BlueGutter).testTag(tag), horizontalAlignment = Alignment.CenterHorizontally) {
        BlueStateIcon(Icons.Filled.Refresh, ring = false)
        Spacer(Modifier.height(18.dp))
        Text(title, style = blueText(26.sp, ink.text, FontWeight.ExtraBold, 1.3f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(body, style = blueText(15.5.sp, ink.secondary, FontWeight.Medium, 1.5f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        BlueButton(button, onRetry, Modifier.widthIn(max = 320.dp).testTag("$tag-retry"))
    }
}

/** 복사 — 고른 메시지 내용만 */
internal fun copyChatText(context: Context, text: String) {
    runCatching {
        context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("StepUp", text))
    }
}
