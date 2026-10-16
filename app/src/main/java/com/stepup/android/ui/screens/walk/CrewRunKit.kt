package com.stepup.android.ui.screens.walk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.PeopleAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.data.repo.PartyMember
import com.stepup.android.ui.components.MapTone
import com.stepup.android.ui.components.RunBackButton
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.StepUpSans

/*
 * 크루 달리기 v2 — 2026-10-16 전달본(stepup-crew-running.html, SHA-256 2b24898e…)이 함께 쓰는 부품.
 *
 * 남색 바탕 · 크루 이름 머리 · 정확히 "크루 달리기"라고 적힌 파란 배지(누르는 것이 아니다 — 식별 표식) · 이니셜 원(나 · 진행자) ·
 * 테 두른 카드 · 안내 줄 · 스위치. 버튼과 지도 위 띠는 자유 러닝 v2 와 같은 부품(FreeRunButton · FreeRunBand)을 쓴다.
 * 시안의 이름 · 인원 · 수치 · 지도 · 경로는 예시다 — 여기 부품은 받은 값만 그린다. 사진 주소가 없어 이름 머리글자로 그린다.
 * 시안은 남색 한 벌이다 — 밝은 테마 값은 자유 러닝 · 러닝 화면의 밝은 값에서 끌어냈다.
 */

// ── 색 ─────────────────────────────────────────────────────────────

@Immutable
internal class CrewColors(
    val screen: Color,
    val card: Color,
    val inset: Color,
    val line: Color,
    val text: Color,
    val label: Color,
    val muted: Color,
    /** "크루 달리기" 배지 · 크루 표시 */
    val badge: Color,
    val badgeInk: Color,
    /** 연결된 동안의 작은 점 */
    val live: Color,
    /** 준비 완료 글 · 알약 */
    val ready: Color,
    val readyFace: Color,
    /** 개인 목표 진행 막대 */
    val goal: Color,
    val goalTrack: Color,
    val avatarEdge: Color,
    val avatarMeEdge: Color,
    val avatarMeFace: Color,
    val avatarFaces: List<Color>,
    val avatarInk: Color,
    /** 안내 줄(혼자 대기 · 연결 끊김 · 경고 아닌 알림) */
    val notice: Color,
    val noticeInk: Color,
    /** 일시정지 · 연결 끊김 · 방 닫힘 띠(지도 위 한 줄) */
    val band: Color,
    val bandInk: Color,
    val success: Color,
    val successInk: Color,
    val link: Color,
    val selectedFace: Color,
    val selectedEdge: Color,
    val switchOff: Color,
    val switchThumb: Color,
)

private val CrewDark = CrewColors(
    screen = Color(0xFF05213B), card = Color(0xFF0C2C4B), inset = Color(0xFF113758), line = Color(0xFF234866),
    text = Color(0xFFF5F8FF), label = Color(0xFFAAC3EA), muted = Color(0xFF8EACCA),
    badge = Color(0xFF0C59FF), badgeInk = Color.White,
    live = Color(0xFF0BEAA1), ready = Color(0xFF73EFCD), readyFace = Color(0xFF123F57),
    goal = Color(0xFF00CEE9), goalTrack = Color(0xFF123E64),
    avatarEdge = Color(0xFFB4CAE6), avatarMeEdge = Color(0xFF5385FF), avatarMeFace = Color(0xFF1B437F),
    avatarFaces = listOf(Color(0xFF193E60), Color(0xFF365B76), Color(0xFF345261), Color(0xFF4D546E), Color(0xFF29475E), Color(0xFF526575)),
    avatarInk = Color(0xFFEEF4FF),
    notice = Color(0xFF123951), noticeInk = Color(0xFFC9DEF2),
    band = Color(0xFF143D5E), bandInk = Color(0xFFD0E5F5),
    success = Color(0xFF124B50), successInk = Color(0xFF74EAC8),
    link = Color(0xFFA8C8FF), selectedFace = Color(0xFF103968), selectedEdge = Color(0xFF4D86FF),
    switchOff = Color(0xFF39536A), switchThumb = Color(0xFFE6F0FF),
)

private val CrewLight = CrewColors(
    screen = Color(0xFFF5F8FF), card = Color.White, inset = Color(0xFFEDF2FC), line = Color(0xFFDCE4F2),
    text = Color(0xFF0B1E3F), label = Color(0xFF4A5F84), muted = Color(0xFF6A7C9C),
    badge = Color(0xFF0754FF), badgeInk = Color.White,
    live = Color(0xFF0E9F6E), ready = Color(0xFF0A7F5C), readyFace = Color(0xFFDDF5EC),
    goal = Color(0xFF0A8FB8), goalTrack = Color(0xFFDCE5F5),
    avatarEdge = Color(0xFF9DB2D3), avatarMeEdge = Color(0xFF0754FF), avatarMeFace = Color(0xFFD6E4FF),
    avatarFaces = listOf(Color(0xFFE3EBF8), Color(0xFFDCE7F1), Color(0xFFE0E6EE), Color(0xFFE4E3F0), Color(0xFFDAE4EE), Color(0xFFE2E8EE)),
    avatarInk = Color(0xFF1B2F55),
    notice = Color(0xFFE8F1FF), noticeInk = Color(0xFF2A3E62),
    band = Color(0xFFE3ECFA), bandInk = Color(0xFF0B1E3F),
    success = Color(0xFFDDF5EC), successInk = Color(0xFF0A7F5C),
    link = Color(0xFF0748D6), selectedFace = Color(0xFFE3EDFF), selectedEdge = Color(0xFF4D86FF),
    switchOff = Color(0xFFC9D3E3), switchThumb = Color.White,
)

@Composable
internal fun crewColors(): CrewColors = if (StepUpColors.dark) CrewDark else CrewLight

/**
 * 크루 달리기의 지도 색 — 밝은 실제 지도 그대로(2026-10-16 크루 전달본: 어두운 덧칠로 길 · 공원 · 경로를 가리지 않는다).
 * 러닝 화면의 남색 지도 필터(RunTone.mapFilter)는 크루 달리기 중 · 결과 지도에 쓰지 않는다.
 */
internal val CrewBrightMap = MapTone(filter = null, shade = 0f, roads = null)

// ── 머리 · 배지 ────────────────────────────────────────────────────

/**
 * 크루 화면 머리 — (뒤로) · 큰 제목 · 한 줄 설명(앞에 작은 점) · 오른쪽 "크루 달리기" 배지.
 * [trailing] 은 배지 앞에 두는 화면의 행동 하나(결과의 ⋯). 앱의 머리 · 아래 탭을 따로 만들지 않는다.
 */
@Composable
internal fun CrewHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    dot: Color? = null,
    trailing: (@Composable () -> Unit)? = null,
    badge: Boolean = true,
) {
    val c = crewColors()
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp)
            .padding(start = if (onBack != null) 4.dp else 20.dp, end = 16.dp, top = 6.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            RunBackButton(onBack, tag = "kit-back", tint = c.text)
            Spacer(Modifier.width(2.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = TextStyle(
                    fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp, lineHeight = 1.2.em,
                    letterSpacing = (-0.03).em, color = c.text,
                ),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }.testTag("crew-header-title"),
            )
            if (subtitle != null) {
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (dot != null) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        subtitle, style = runTextStyle(13.sp, c.label, FontWeight.Medium), maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("crew-header-sub"),
                    )
                }
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(6.dp))
            trailing()
        }
        if (badge) {
            Spacer(Modifier.width(8.dp))
            CrewBadge()
        }
    }
}

/** 정확히 "크루 달리기" — 파란 알약 · 사람들 아이콘. 누르는 것이 아니다(버튼처럼 보이지 않게 그림자 · 테가 없다) */
@Composable
internal fun CrewBadge(modifier: Modifier = Modifier) {
    val c = crewColors()
    Row(
        modifier.clip(RoundedCornerShape(18.dp)).background(c.badge).padding(horizontal = 11.dp, vertical = 7.dp)
            .testTag("crew-badge"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.PeopleAlt, contentDescription = null, tint = c.badgeInk, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(5.dp))
        Text(stringResource(R.string.run_crew_title), style = runTextStyle(13.sp, c.badgeInk, FontWeight.Bold, 1.2f), maxLines = 1)
    }
}

// ── 사람 ───────────────────────────────────────────────────────────

/** 이니셜 원 — 나는 "나"와 파란 테, 다른 사람은 이름 머리글자와 옅은 테. 바탕은 사람마다 정해진 남색 하나 */
@Composable
internal fun CrewAvatar(name: String, id: String, me: Boolean, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val c = crewColors()
    val face = if (me) c.avatarMeFace else c.avatarFaces[Math.floorMod(id.hashCode(), c.avatarFaces.size)]
    val text = if (me) stringResource(R.string.run_cr_me_only) else initialsOf(name)
    Box(
        modifier.size(size).clip(CircleShape).background(face).border(2.dp, if (me) c.avatarMeEdge else c.avatarEdge, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, style = runTextStyle(if (size >= 48.dp) 16.sp else if (text.length > 1) 12.sp else 14.sp, c.avatarInk, FontWeight.Bold, 1.1f),
            maxLines = 1,
        )
    }
}

/** 명단 순서 — 진행자, 나, 나머지(서버 순서). 진행자가 나면 나 한 번 */
internal fun crewOrder(members: List<PartyMember>): List<PartyMember> =
    members.sortedWith(compareByDescending<PartyMember> { it.isHost }.thenByDescending { it.isMe })

/**
 * 같이 달리는 사람 줄(CR09) — 실제 명단의 이니셜 원과 그 아래 진행자 · 나. 들어가지 않으면 앞사람들 + "+N".
 * 줄 전체가 버튼이다 — 누르면 함께 달리는 사람(CR10). 위치를 공유하지 않는 사람도 줄에는 있다(지도에는 없다).
 */
@Composable
internal fun CrewRoster(members: List<PartyMember>, onOpen: () -> Unit, modifier: Modifier = Modifier, compact: Boolean = false) {
    val c = crewColors()
    val ordered = crewOrder(members)
    val spoken = stringResource(R.string.run_together_row, members.size)
    val size = if (compact) 36.dp else 42.dp
    BoxWithConstraints(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).feedbackClickable(onClick = onOpen)
            .semantics(mergeDescendants = true) { contentDescription = spoken }.testTag("run-together-row"),
    ) {
        val slot = size + 14.dp
        val fit = ((maxWidth + 6.dp) / slot).toInt().coerceAtLeast(2)
        val shown = if (ordered.size > fit) ordered.take(fit - 1) else ordered
        val rest = ordered.size - shown.size
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            shown.forEach { m ->
                Column(Modifier.width(slot - 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CrewAvatar(m.name, m.id, m.isMe, size = size)
                    val role = when {
                        m.isHost -> stringResource(R.string.run_cr_host)
                        m.isMe -> stringResource(R.string.run_cr_me_only)
                        else -> null
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        role ?: m.name, style = runTextStyle(11.sp, if (role != null) c.label else c.muted, FontWeight.Medium, 1.2f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    )
                }
            }
            if (rest > 0) {
                Column(Modifier.width(slot - 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(size).clip(CircleShape).background(c.inset).border(2.dp, c.line, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("+$rest", style = runTextStyle(13.sp, c.text, FontWeight.Bold, 1.1f), modifier = Modifier.testTag("crew-roster-more"))
                    }
                }
            }
        }
    }
}

/** 명단 한 줄 — 이니셜 · 이름 · 보조 글(진행자 · 크루원 · 내 러닝) · 오른쪽 상태 */
@Composable
internal fun CrewPersonRow(
    member: PartyMember,
    meta: String,
    modifier: Modifier = Modifier,
    avatar: Dp = 46.dp,
    trailing: @Composable () -> Unit = {},
) {
    val c = crewColors()
    Row(modifier.fillMaxWidth().heightIn(min = 60.dp).padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        CrewAvatar(member.name, member.id, member.isMe, size = avatar)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (member.isMe) stringResource(R.string.run_cr_me, member.name) else member.name,
                style = runTextStyle(15.sp, c.text, FontWeight.Bold, 1.25f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(meta, style = runTextStyle(12.sp, c.muted, FontWeight.Medium, 1.25f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        trailing()
    }
}

/** 준비 완료(초록 체크) · 준비 전(흐린 글) */
@Composable
internal fun CrewReadyMark(ready: Boolean, modifier: Modifier = Modifier) {
    val c = crewColors()
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (ready) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = c.ready, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(
            stringResource(if (ready) R.string.run_cr_ready else R.string.run_cr_not_ready),
            style = runTextStyle(13.sp, if (ready) c.ready else c.muted, FontWeight.SemiBold),
        )
    }
}

// ── 대기실 참가자 그리드(2026-10-16 승인 이미지) ─────────────────────

/**
 * 준비 표시 점 — 준비한 사람 수만큼 채운다(사람 수가 많으면 두지 않는다 — 글이 같은 수를 말한다). 화면 낭독은 글만 읽는다.
 */
@Composable
internal fun CrewReadyDots(ready: Int, total: Int, modifier: Modifier = Modifier) {
    val c = crewColors()
    Row(modifier.testTag("party-ready-dots").clearAndSetSemantics { }, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        repeat(total) { i ->
            Box(Modifier.size(11.dp).clip(CircleShape).background(if (i < ready) c.ready else c.switchOff))
        }
    }
}

/**
 * 참가자 한 칸 — 큰 이니셜 원(준비하면 파란 면) · 오른쪽 아래 준비 표시(체크 · 시계) · 이름(나는 "나") · 짧은 준비 상태.
 * 진행자는 "진행자 · 준비 완료"처럼 역할을 앞에. 일반 참가자에게 "크루원" 같은 역할 글을 되풀이하지 않는다.
 * 파티 응답에 사진 주소가 없어 이니셜로 그린다(예시 인물 사진을 넣지 않는다).
 */
@Composable
internal fun CrewGridCell(member: PartyMember, avatar: Dp, modifier: Modifier = Modifier) {
    val c = crewColors()
    val status = stringResource(if (member.ready) R.string.run_cr_ready else R.string.run_cr_not_ready)
    val line = if (member.isHost) stringResource(R.string.run_cr_host) + " · " + status else status
    val name = if (member.isMe) stringResource(R.string.run_cr_me_only) else member.name
    Column(
        modifier.semantics(mergeDescendants = true) {}.testTag("party-member-${member.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(avatar + 6.dp)) {
            // 준비하면 파란 면 · 흰 글자, 준비 전은 흐린 면(색만으로 가르지 않는다 — 오른쪽 아래 표시 · 아래 글이 같은 말을 한다)
            val face = if (member.ready) c.badge else c.switchOff
            Box(
                Modifier.size(avatar).align(Alignment.Center).clip(CircleShape).background(face)
                    .border(if (member.isMe) 3.dp else 1.dp, if (member.isMe) c.avatarMeEdge else c.line, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (member.isMe) stringResource(R.string.run_cr_me_only) else initialsOf(member.name),
                    style = runTextStyle((avatar.value * 0.3f).sp, if (member.ready) c.badgeInk else c.text, FontWeight.ExtraBold, 1.1f),
                    maxLines = 1,
                )
            }
            val mark = avatar * 0.34f
            Box(
                Modifier.size(mark).align(Alignment.BottomEnd).clip(CircleShape)
                    .background(if (member.ready) c.ready else c.label).border(2.dp, c.screen, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (member.ready) Icons.Filled.Check else Icons.Outlined.Schedule, contentDescription = null,
                    tint = c.screen, modifier = Modifier.size(mark * 0.62f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            name, style = runTextStyle(16.sp, c.text, FontWeight.Bold, 1.25f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            line, style = runTextStyle(13.sp, if (member.ready) c.ready else c.muted, FontWeight.SemiBold, 1.25f),
            maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
    }
}

/** "+N" 칸 — 그리드에 다 들어가지 않은 사람 수. 누르면 전체 명단 */
@Composable
internal fun CrewMoreCell(rest: Int, avatar: Dp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = crewColors()
    val label = stringResource(R.string.run_crew_roster_all)
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).feedbackClickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = label }.testTag("party-more"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(avatar + 6.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(avatar).clip(CircleShape).background(c.card).border(1.dp, c.line, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("+$rest", style = runTextStyle((avatar.value * 0.28f).sp, c.text, FontWeight.ExtraBold, 1.1f))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = runTextStyle(13.sp, c.link, FontWeight.SemiBold, 1.25f), maxLines = 2, textAlign = TextAlign.Center)
    }
}

// ── 판 · 줄 ────────────────────────────────────────────────────────

/** 테 두른 카드 — 명단 · 기록 · 미리보기 */
@Composable
internal fun CrewCard(modifier: Modifier = Modifier, padding: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    val c = crewColors()
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(c.card, shape).border(1.dp, c.line, shape).padding(horizontal = padding, vertical = 6.dp),
        content = content,
    )
}

/** 카드 안 줄 사이 가는 선 */
@Composable
internal fun CrewLine(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(crewColors().line))
}

/** 안내 줄 — 아이콘 + 짧은 글(혼자 대기 · 연결되면 · 다른 참가자). 바뀌면 화면 낭독이 읽는다 */
@Composable
internal fun CrewNote(text: String, modifier: Modifier = Modifier, icon: ImageVector = Icons.Outlined.PeopleAlt, tag: String? = null) {
    val c = crewColors()
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.notice).padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = c.noticeInk, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = runTextStyle(14.sp, c.noticeInk, FontWeight.Medium, 1.45f), modifier = Modifier.weight(1f))
    }
}

/** 실패 · 막힘 한 줄 — 붉은 면 · 경고 아이콘(색만으로 알리지 않는다). 바뀌면 화면 낭독이 읽는다 */
@Composable
internal fun CrewError(text: String, modifier: Modifier = Modifier, tag: String? = null) {
    val t = com.stepup.android.ui.components.runTone()
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier.fillMaxWidth().clip(shape).background(t.errorFace).border(1.dp, t.errorEdge, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = t.errorIcon, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = runTextStyle(14.sp, t.text, FontWeight.Medium, 1.45f), modifier = Modifier.weight(1f))
    }
}

/** 화면 폭 한 줄 띠 — 일시정지 · 연결 끊김 · 방 닫힘(지도 바로 위, 시안 CR11 · CR18) */
@Composable
internal fun CrewBand(text: String, icon: ImageVector, tag: String, modifier: Modifier = Modifier) {
    val c = crewColors()
    Row(
        modifier.fillMaxWidth().heightIn(min = 44.dp).background(c.band).padding(horizontal = 18.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }.testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = c.bandInk, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(9.dp))
        Text(text, style = runTextStyle(14.sp, c.bandInk, FontWeight.SemiBold, 1.3f), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** 켜고 끄는 줄 — 이름 · 한 줄 설명 · 스위치(위치 공유 · 경로 포함) */
@Composable
internal fun CrewToggleRow(
    title: String,
    body: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    switchTag: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val c = crewColors()
    Row(modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = c.label, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = runTextStyle(16.sp, c.text, FontWeight.SemiBold, 1.3f))
            if (body != null) Text(body, style = runTextStyle(13.sp, c.label, FontWeight.Normal, 1.4f))
        }
        Spacer(Modifier.width(12.dp))
        CrewSwitch(checked, onChange, Modifier.testTag(switchTag), enabled = enabled)
    }
}

@Composable
internal fun CrewSwitch(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = crewColors()
    Switch(
        checked = checked, onCheckedChange = onChange, enabled = enabled, modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White, checkedTrackColor = c.badge, checkedBorderColor = c.badge,
            uncheckedThumbColor = c.switchThumb, uncheckedTrackColor = c.switchOff, uncheckedBorderColor = c.switchOff,
            disabledUncheckedTrackColor = c.switchOff.copy(alpha = 0.5f), disabledUncheckedBorderColor = c.switchOff.copy(alpha = 0.5f),
            disabledUncheckedThumbColor = c.switchThumb.copy(alpha = 0.6f),
        ),
    )
}

/** 글자 행동(크루에게 알리기 › · 다시 연결하기) — 누르는 자리는 48dp */
@Composable
internal fun CrewTextAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    chevron: Boolean = false,
    center: Boolean = false,
) {
    val c = crewColors()
    Row(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp)).feedbackClickable(onClick = onClick).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (center) Arrangement.Center else Arrangement.Start,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = c.link, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, style = runTextStyle(15.sp, c.link, FontWeight.SemiBold))
        if (chevron) Text("  ›", style = runTextStyle(15.sp, c.link, FontWeight.SemiBold))
    }
}

/** 시트 맨 위 — 왼쪽 큰 제목(닫기 자리를 비운다) · 한두 줄 설명 */
@Composable
internal fun CrewSheetTitle(title: String, body: String? = null, modifier: Modifier = Modifier) {
    val c = crewColors()
    Column(modifier.fillMaxWidth().padding(end = 44.dp)) {
        Text(
            title,
            style = TextStyle(
                fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 1.3.em,
                letterSpacing = (-0.03).em, color = c.text,
            ),
            modifier = Modifier.semantics { heading() }.testTag("crew-sheet-title"),
        )
        if (body != null) {
            Spacer(Modifier.height(8.dp))
            Text(body, style = runTextStyle(15.sp, c.label, FontWeight.Medium, 1.5f))
        }
    }
}

/** 큰 수 한 칸 — 이름 위, 수 + 작은 단위(시안의 거리 · 시간 · 페이스). [center] 는 칸 가운데(달리는 중 기록 카드) */
@Composable
internal fun androidx.compose.foundation.layout.RowScope.CrewStat(
    label: String,
    value: String,
    unit: String?,
    tag: String,
    size: TextUnit = 28.sp,
    center: Boolean = false,
) {
    val c = crewColors()
    Column(
        Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = "$label $value ${unit.orEmpty()}".trim() },
        horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Text(label, style = runTextStyle(13.sp, c.label, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
        // 가운데면 수의 폭만큼(칸보다 넓으면 줄인다) — 칸 폭을 다 쓰면 왼쪽에 붙는다
        FitNumber(value, unit, valueSize = size, unitSize = size * 0.45f, valueColor = c.text, unitColor = c.label,
            modifier = if (center) Modifier else Modifier.fillMaxWidth(), valueTag = tag)
    }
}

/** 크루 표시 — 파란 둥근 네모 안에 크루 이니셜(크루가 정한 글자) */
@Composable
internal fun CrewMark(text: String, size: Dp) {
    val c = crewColors()
    val shape = RoundedCornerShape(size * 0.28f)
    Box(Modifier.size(size).clip(shape).background(c.badge), contentAlignment = Alignment.Center) {
        Text(
            text.take(2).uppercase(),
            style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.36f).sp,
                color = c.badgeInk, letterSpacing = (-0.04).em),
            maxLines = 1,
        )
    }
}
