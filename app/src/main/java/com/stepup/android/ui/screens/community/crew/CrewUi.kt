package com.stepup.android.ui.screens.community.crew

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
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
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.CrewCard
import com.stepup.android.domain.CrewGoalProgress
import com.stepup.android.domain.CrewRules
import com.stepup.android.ui.components.ChamferShape
import com.stepup.android.ui.components.RunBackdrop
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunButtonKind
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.StepUpSans

/*
 * 크루 화면(둘러보기 · 가입 · 만들기 · 운영)이 함께 쓰는 부품 — 2026-10 파란 톤 전달본(v19 크루 95장).
 *
 * 남색 바닥 #031427 · 파란 면 #0B2B50 · 전기 파랑 #0754FF · 청록 #48D9FA · 흰 글자 #F5F8FF · 보조 #AAC3EA.
 * 러닝 리메이크 부품(RunStyle.kt)의 색 · 주 버튼(흰 면 · 파란 아랫면) · 보조 버튼(남색 면 · 파란 테두리)을 그대로 쓰고,
 * 크루에만 있는 것(명함 · 레벨 칩 · 사람 줄 · 시트 · 확인 창)만 여기서 그린다. 왕관 · 방패 · 게임 등급 · XP 는 두지 않는다.
 * 같은 부품을 크루 채팅 · 내 크루 홈도 쓰므로 함수 모양(이름 · 인자)은 바꾸지 않고 더하기만 한다.
 */

/** 크루 화면의 색 — 어두운 테마는 시안 값, 밝은 테마는 러닝 리메이크의 밝은 값과 같은 관계 */
@Immutable
internal class CrewInk(
    val canvas: Color,
    /** 패널 면(아래쪽) · 위쪽 */
    val card: Color,
    val cardTop: Color,
    /** 패널 둘레 — 얇은 파란 선 */
    val edge: Color,
    /** 입력칸 면 · 둘레 */
    val field: Color,
    val fieldEdge: Color,
    val sheet: Color,
    val sheetTop: Color,
    val sheetEdge: Color,
    val text: Color,
    val secondary: Color,
    /** 강조 값 · 진행률(청록) */
    val info: Color,
    /** 글자 링크(크루장 소개 보기 · 24명 >) */
    val link: Color,
    val divider: Color,
    val tabMark: Color,
    /** 실패 · 주의 글자(코랄) */
    val warn: Color,
    val choice: Color,
    val choiceText: Color,
    val choiceOn: Color,
    val choiceOnText: Color,
    val level: Color,
    val levelText: Color,
    val track: Color,
    val bar: Color,
    val avatar: Color,
    val avatarText: Color,
    val secondaryButton: Color,
    val disabled: Color,
    val disabledText: Color,
    val danger: Color,
    val dangerText: Color,
    val dangerBase: Color,
    val primaryFace: Brush,
    val primaryText: Color,
    val skeleton: Color,
    val handle: Color,
    val scrim: Color,
)

private val DarkCrewInk = CrewInk(
    canvas = Color(0xFF031427), card = Color(0xFF0A2547), cardTop = Color(0xFF0D2E57), edge = Color(0xFF1C4E95),
    field = Color(0xFF08244A), fieldEdge = Color(0xFF1F5BD0),
    sheet = Color(0xFF0A2750), sheetTop = Color(0xFF0E3264), sheetEdge = Color(0xFF1F5BD0),
    text = Color(0xFFF5F8FF), secondary = Color(0xFFAAC3EA), info = Color(0xFF48D9FA), link = Color(0xFF5B95FF),
    divider = Color(0xFF1E3F72), tabMark = Color(0xFF48D9FA), warn = Color(0xFFFF6B78),
    choice = Color(0xFF0A2547), choiceText = Color(0xFFF5F8FF), choiceOn = Color(0xFF0754FF), choiceOnText = Color.White,
    level = Color(0xFF16386E), levelText = Color(0xFF8CB8FF), track = Color(0xFF15305A), bar = Color(0xFF48D9FA),
    avatar = Color(0xFF12335F), avatarText = Color(0xFF8CB8FF), secondaryButton = Color(0xFF061D3B),
    disabled = Color(0xFF2A4166), disabledText = Color(0xFF8FA5C7),
    danger = Color(0xFF4A1626), dangerText = Color(0xFFFF7A85), dangerBase = Color(0xFFE2505F),
    primaryFace = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFDCEBFD))), primaryText = Color(0xFF071B3D),
    skeleton = Color(0xFF17345F), handle = Color(0xFF3F74C8), scrim = Color(0xA8010A16),
)

private val LightCrewInk = CrewInk(
    canvas = Color(0xFFF5F8FF), card = Color(0xFFF7FAFF), cardTop = Color(0xFFFFFFFF), edge = Color(0xFFC9D8F2),
    field = Color(0xFFEDF2FC), fieldEdge = Color(0xFF9DB8EE),
    sheet = Color(0xFFF5F8FF), sheetTop = Color(0xFFFFFFFF), sheetEdge = Color(0xFFC9D8F2),
    text = Color(0xFF0B1E3F), secondary = Color(0xFF4A5F84), info = Color(0xFF0A7FA6), link = Color(0xFF0748D6),
    divider = Color(0xFFDCE4F2), tabMark = Color(0xFF0754FF), warn = Color(0xFFC62337),
    choice = Color(0xFFFFFFFF), choiceText = Color(0xFF0B1E3F), choiceOn = Color(0xFF0754FF), choiceOnText = Color.White,
    level = Color(0xFFE1EBFF), levelText = Color(0xFF0748D6), track = Color(0xFFDCE5F5), bar = Color(0xFF0754FF),
    avatar = Color(0xFFE1EBFF), avatarText = Color(0xFF0748D6), secondaryButton = Color(0xFFFFFFFF),
    disabled = Color(0xFFC9D3E3), disabledText = Color(0xFF6E7D96),
    danger = Color(0xFFFFE5E8), dangerText = Color(0xFFC62337), dangerBase = Color(0xFFE59AA4),
    primaryFace = Brush.verticalGradient(listOf(Color(0xFF1C63FF), Color(0xFF0754FF))), primaryText = Color.White,
    skeleton = Color(0xFFDCE5F5), handle = Color(0xFFB5C3DB), scrim = Color(0xB3091A33),
)

@Composable
internal fun crewInk(): CrewInk = if (StepUpColors.dark) DarkCrewInk else LightCrewInk

internal val CrewGutter = 20.dp

/** 이름 이미지 바탕 네 가지(시안 30) — 글자는 옅은 초록빛 흰색. 크루가 고른 고유 색이라 앱 색으로 바꾸지 않는다 */
internal val CrewNamedColors = listOf(Color(0xFF263D47), Color(0xFF403343), Color(0xFF233C58), Color(0xFF383E32))
private val CrewNamedText = Color(0xFFE9EEE6)

/** 크루장 동그라미 — 크루 바탕보다 조금 밝은 톤(시안의 크루장 색과 같은 관계) */
internal val CrewLeaderColors = listOf(Color(0xFF34505B), Color(0xFF4B3D4E), Color(0xFF2E4A6A), Color(0xFF474D3F))

/** 사람 동그라미 — 이름마다 정해진 차분한 색(시안 명단의 갈색 · 파랑 · 초록 · 보라 · 붉은 톤) */
private val CrewPersonColors = listOf(
    Color(0xFF5A3A30), Color(0xFF1F4A7A), Color(0xFF2F5A47), Color(0xFF453A78), Color(0xFF6A3343), Color(0xFF1F5C66),
)

internal fun crewPersonFace(name: String): Color =
    CrewPersonColors[Math.floorMod(name.trim().hashCode(), CrewPersonColors.size)]

// ── 패널 ─────────────────────────────────────────────────────

/** 패널 면 — 남색 그라데이션 · 얇은 파란 둘레(카드 · 정보 칸 · 목표 칸) */
internal fun Modifier.crewPanel(ink: CrewInk, radius: Dp = 16.dp, selected: Boolean = false): Modifier {
    val shape = RoundedCornerShape(radius)
    return this.clip(shape)
        .background(Brush.verticalGradient(listOf(ink.cardTop, ink.card)), shape)
        .border(if (selected) 1.5.dp else 1.dp, if (selected) ink.info else ink.edge, shape)
}

// ── 대표 이미지 ───────────────────────────────────────────────

/**
 * 대표 이미지 — 크루의 사진, 없거나 못 받으면 크루 이름이 들어간 기본 이미지(크루가 고른 바탕).
 * 불러오는 동안에도 이름 이미지를 먼저 보인다(가짜 크루 정보를 먼저 보이지 않는다).
 */
@Composable
internal fun CrewImage(card: CrewCard, size: Dp, radius: Dp, modifier: Modifier = Modifier, textSize: TextUnit? = null) {
    var bitmap by remember(card.id) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(card.id, card.imageVer, card.hasImage) {
        bitmap = if (!card.hasImage) null else runCatching {
            ServiceLocator.crewCards.images.bitmap(card.id, card.imageVer)?.asImageBitmap()
        }.getOrNull()
    }
    CrewImageFace(card.name, card.bg, bitmap, size, radius, modifier.testTag("crew-image"), textSize)
}

/** 초안 · 미리보기의 이미지 — 고른 사진 또는 이름 이미지 */
@Composable
internal fun CrewImageFace(
    name: String,
    bg: Int,
    photo: ImageBitmap?,
    size: Dp,
    radius: Dp,
    modifier: Modifier = Modifier,
    textSize: TextUnit? = null,
) {
    val shape = RoundedCornerShape(radius)
    Box(modifier.size(size).clip(shape).background(CrewNamedColors[Math.floorMod(bg, CrewNamedColors.size)]), contentAlignment = Alignment.Center) {
        if (photo != null) {
            Image(photo, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            CrewNameText(name, size, textSize)
        }
    }
}

/** 이름 이미지의 글자 — 이름 그대로, 길면 빈칸에서 두 줄 · 긴 줄에 맞춰 작게. 이름이 비면 중립 점 하나(시안 36) */
@Composable
private fun CrewNameText(name: String, size: Dp, textSize: TextUnit?) {
    if (name.isBlank()) {
        Box(Modifier.size(size * 0.12f).clip(CircleShape).background(CrewNamedText.copy(alpha = 0.85f)))
        return
    }
    val label = CrewRules.nameImageText(name)
    val longest = label.lines().maxOf { it.length }
    val base = textSize ?: (size.value * 0.24f).sp
    val scaled = when {
        longest <= 4 -> base
        longest <= 6 -> base * 0.82f
        else -> base * 0.66f
    }
    Text(
        label,
        color = CrewNamedText,
        fontSize = scaled,
        lineHeight = scaled * 1.18f,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = (size.value * 0.08f).dp),
    )
}

/** 초안 사진 파일을 읽는다 */
@Composable
internal fun rememberDraftPhoto(path: String?): ImageBitmap? {
    var bitmap by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(path) {
        bitmap = path?.let { p ->
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.stepup.android.data.repo.CrewPhotos.decodeFile(java.io.File(p))?.asImageBitmap()
            }
        }
    }
    return bitmap
}

// ── 레벨 · 인원 · 크루장 · 목표 ─────────────────────────────────

/** 레벨 칩 — 평문 "Lv.7", 저장된 레벨이 없으면 "새 크루"(청록 테두리) */
@Composable
internal fun CrewLevelChip(level: Int?, modifier: Modifier = Modifier) {
    val ink = crewInk()
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier.clip(shape)
            .then(
                if (level != null) Modifier.background(ink.level, shape)
                else Modifier.border(1.dp, ink.info, RoundedCornerShape(50)).clip(RoundedCornerShape(50)),
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (level != null) stringResource(R.string.crew_level_value, level) else stringResource(R.string.crew_level_new),
            color = if (level != null) ink.levelText else ink.info, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
        )
    }
}

/** 사람 둘 그림 + "24 / 30명" (정원이 없으면 "24명") */
@Composable
internal fun CrewMembersLabel(members: Int, capacity: Int?, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        PeopleIcon(color, Modifier.size(18.dp))
        Text(
            if (capacity != null) stringResource(R.string.crew_members_of, members, capacity) else stringResource(R.string.crew_members_only, members),
            color = color, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
        )
    }
}

/** 시안의 사람 둘 그림(선) */
@Composable
internal fun PeopleIcon(color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val s = size.width / 24f
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.6f * s * 1.5f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawCircle(color, radius = 3f * s, center = Offset(8f * s, 7f * s), style = stroke)
        drawCircle(color, radius = 2.5f * s, center = Offset(17f * s, 8f * s), style = stroke)
        val body = androidx.compose.ui.graphics.Path().apply {
            moveTo(2f * s, 21f * s); lineTo(2f * s, 18f * s)
            cubicTo(2f * s, 14.7f * s, 4.7f * s, 12f * s, 8f * s, 12f * s)
            cubicTo(11.3f * s, 12f * s, 14f * s, 14.7f * s, 14f * s, 18f * s); lineTo(14f * s, 21f * s)
            moveTo(15f * s, 14f * s)
            cubicTo(18.5f * s, 13.5f * s, 22f * s, 16f * s, 22f * s, 19f * s); lineTo(22f * s, 21f * s)
        }
        drawPath(body, color, style = stroke)
    }
}

/** 한 사람의 첫 글자 동그라미 */
@Composable
internal fun CrewAvatar(name: String, size: Dp, face: Color, textColor: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(face), contentAlignment = Alignment.Center) {
        Text(CrewRules.initial(name), color = textColor, fontSize = (size.value * 0.36f).sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** 크루장 동그라미 색 — 크루 바탕과 같은 결 */
internal fun leaderFace(card: CrewCard): Color = CrewLeaderColors[Math.floorMod(card.bg, CrewLeaderColors.size)]

/** 이번 주 목표 한 줄 — "이번 주 126 / 160km ━━━━━ 79%". 막대 길이는 실제 비율, 100% 를 넘으면 막대만 끝까지 */
@Composable
internal fun CrewWeeklyLine(progress: CrewGoalProgress, modifier: Modifier = Modifier) {
    val ink = crewInk()
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.crew_week_line, CrewRules.km(progress.doneKm), progress.goalKm),
            color = ink.secondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(10.dp))
        CrewBar(progress.fraction, Modifier.weight(0.85f).height(7.dp))
        Text(
            stringResource(R.string.crew_percent, progress.percent), color = ink.info, fontSize = 15.sp,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.widthIn(min = 48.dp),
        )
    }
}

/** 가는 진행 막대 — 100% 까지만 채운다 */
@Composable
internal fun CrewBar(fraction: Float, modifier: Modifier) {
    val ink = crewInk()
    Canvas(modifier) {
        val r = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(ink.track, cornerRadius = r)
        val w = size.width * fraction.coerceIn(0f, 1f)
        if (w > 0f) drawRoundRect(ink.bar, size = Size(w.coerceAtLeast(size.height), size.height), cornerRadius = r)
    }
}

// ── 버튼 ─────────────────────────────────────────────────────

internal enum class CrewButtonKind { PRIMARY, SECONDARY, DANGER, DISABLED }

/**
 * 큰 버튼 — 러닝 리메이크의 주 버튼(흰 면 · 남색 글자 · 파란 아랫면)과 보조 버튼(남색 면 · 파란 테두리)을 그대로 쓴다.
 * 삭제성 행동(내보내기 · 나가기 · 해산 · 초안 지우기)은 어두운 코랄 면 · 코랄 글자. 처리 중이면 도는 표시와 함께 다시 누를 수 없다.
 */
@Composable
internal fun CrewButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: CrewButtonKind = CrewButtonKind.PRIMARY,
    busy: Boolean = false,
    enabled: Boolean = true,
) {
    when (kind) {
        CrewButtonKind.PRIMARY -> RunButton(text, onClick, modifier, RunButtonKind.Primary, enabled = enabled, busy = busy)
        CrewButtonKind.SECONDARY -> RunButton(text, onClick, modifier, RunButtonKind.Secondary, enabled = enabled, busy = busy)
        CrewButtonKind.DISABLED -> RunButton(text, onClick, modifier, RunButtonKind.Primary, enabled = false)
        CrewButtonKind.DANGER -> CrewDangerButton(text, onClick, modifier, busy = busy, enabled = enabled)
    }
}

/** 어두운 코랄 면 · 코랄 글자 · 코랄 아랫면 — 주 버튼과 같은 깎은 모양 · 높이 */
@Composable
private fun CrewDangerButton(text: String, onClick: () -> Unit, modifier: Modifier, busy: Boolean, enabled: Boolean) {
    val ink = crewInk()
    val active = enabled && !busy
    val shape: Shape = ChamferShape(16.dp)
    val face = if (active) ink.danger else ink.disabled
    val textColor = if (active) ink.dangerText else ink.disabledText
    Box(
        modifier.fillMaxWidth().heightIn(min = 62.dp)
            .feedbackClickable(enabled = active, role = Role.Button, onClick = onClick)
            .semantics { if (busy) contentDescription = text },
    ) {
        Box(Modifier.fillMaxWidth().height(56.dp).offset(y = 6.dp).clip(shape).background(if (active) ink.dangerBase else ink.disabled))
        Row(
            Modifier.fillMaxWidth().height(56.dp).clip(shape).background(face, shape).border(1.dp, if (active) ink.dangerBase else ink.disabled, shape)
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (busy) {
                RunSpinner(Modifier.size(22.dp), color = textColor, track = textColor.copy(alpha = 0.25f))
                Spacer(Modifier.width(12.dp))
            }
            Text(text, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** 글자만 있는 작은 행동 — 시트의 "취소" · 결과의 "크루 채팅 열기"(청록) */
@Composable
internal fun CrewTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color? = null,
) {
    val ink = crewInk()
    Box(
        modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(12.dp))
            .feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, color = if (enabled) color ?: ink.info else ink.disabledText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

/** 작은 테두리 버튼 — 카드의 "크루 보기"(청록 테두리 · 흰 글자, 누르는 곳 48dp) */
@Composable
internal fun CrewSmallButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ink = crewInk()
    Box(
        modifier.heightIn(min = 48.dp).feedbackClickable(role = Role.Button, onClick = onClick).padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        val shape = RoundedCornerShape(12.dp)
        Box(
            Modifier.widthIn(min = 104.dp).heightIn(min = 40.dp).clip(shape).background(ink.secondaryButton, shape)
                .border(1.dp, ink.info.copy(alpha = 0.75f), shape)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, color = ink.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

/**
 * 고르는 버튼 — 고르지 않으면 남색 면 · 파란 테두리, 고르면 전기 파랑 면 · 흰 글자 · 체크.
 * [enabled] 가 거짓이고 고른 상태면 누르지 않는 표시 칩(러닝 스타일 · 신청 문구)이다.
 */
@Composable
internal fun CrewChoice(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val ink = crewInk()
    val display = !enabled && selected
    Box(
        modifier.heightIn(min = 52.dp).feedbackClickable(enabled = enabled, role = Role.Checkbox, cue = FeedbackCue.Select, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        val shape = RoundedCornerShape(12.dp)
        val on = selected && !display
        Row(
            Modifier.fillMaxWidth().heightIn(min = 46.dp).clip(shape)
                .background(
                    if (on) Brush.horizontalGradient(listOf(Color(0xFF0754FF), Color(0xFF1F66FF)))
                    else Brush.verticalGradient(listOf(ink.cardTop, ink.choice)),
                    shape,
                )
                .border(1.dp, if (on) Color(0xFF4D8BFF) else ink.fieldEdge.copy(alpha = if (display) 1f else 0.85f), shape)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text, color = if (on) ink.choiceOnText else if (!enabled && !selected) ink.disabledText else ink.choiceText,
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 19.sp,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (on) {
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** 한 줄에 버튼 여러 개 — 폭을 나눈다 */
@Composable
internal fun CrewChoiceRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

// ── 머리 · 줄 · 시트 ───────────────────────────────────────────

/** 크루 화면 머리 — 왼쪽 뒤로(가는 꺾쇠) · 가운데 제목 · 오른쪽 더보기(•••) 하나 */
@Composable
internal fun CrewTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, onMore: (() -> Unit)? = null) {
    val ink = crewInk()
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        com.stepup.android.ui.components.RunBackButton(onBack, Modifier, tint = ink.text)
        Text(
            title, color = ink.text, fontSize = 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp).semantics { heading() },
        )
        if (onMore != null) {
            val more = stringResource(R.string.common_more)
            Box(
                Modifier.size(48.dp).clip(CircleShape).feedbackClickable(role = Role.Button, onClick = onMore)
                    .semantics { contentDescription = more }.testTag("crew-more"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.MoreHoriz, contentDescription = null, tint = ink.text, modifier = Modifier.size(26.dp))
            }
        } else {
            Spacer(Modifier.size(48.dp))
        }
    }
}

/** 설정 줄 — 이름, 오른쪽 값(청록), 꺾쇠, 아래 파란 선 */
@Composable
internal fun CrewRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    titleColor: Color? = null,
    titleSize: TextUnit = 17.sp,
    divider: Boolean = true,
) {
    val ink = crewInk()
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 60.dp).feedbackClickable(role = Role.Button, onClick = onClick)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title, color = titleColor ?: ink.text, fontSize = titleSize, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            if (value != null) {
                Text(
                    value, color = ink.info, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp).widthIn(max = 200.dp),
                )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = ink.text, modifier = Modifier.padding(start = 6.dp).size(22.dp))
        }
        if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(ink.divider))
    }
}

/** 더보기 · 관리 시트의 한 줄 — 테두리 있는 칸, 앞에 그림(있으면) · 이름 · 꺾쇠. 삭제성 행동은 코랄 글자 */
@Composable
internal fun CrewMenuRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    warn: Boolean = false,
    /** 오른쪽 작은 값(같이 달리기 › 대기실) */
    value: String? = null,
) {
    val ink = crewInk()
    Row(
        modifier.fillMaxWidth().heightIn(min = 60.dp).crewPanel(ink, 14.dp)
            .feedbackClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (warn) ink.warn else ink.info, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(14.dp))
        }
        Text(
            label, color = if (warn) ink.warn else ink.text, fontSize = 17.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        if (value != null) {
            Text(value, color = ink.info, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = ink.text, modifier = Modifier.padding(start = 4.dp).size(22.dp))
    }
}

/** 사람 한 줄 — 동그라미 · 이름(· 크루장 표시) · 역할 줄 · 파란 꺾쇠 */
@Composable
internal fun CrewPersonRow(
    name: String,
    sub: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subColor: Color? = null,
    /** 이름 옆 작은 파란 표시("크루장") */
    badge: String? = null,
) {
    val ink = crewInk()
    Row(
        modifier.fillMaxWidth().heightIn(min = 80.dp).feedbackClickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CrewAvatar(name, 50.dp, crewPersonFace(name), Color.White)
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            name, color = ink.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                        )
                        if (badge != null) {
                            Spacer(Modifier.width(8.dp))
                            CrewBadge(badge)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(sub, color = subColor ?: ink.secondary, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = ink.link, modifier = Modifier.size(24.dp))
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(ink.divider))
        }
    }
}

/** 이름 옆 작은 파란 표시 — "크루장" */
@Composable
internal fun CrewBadge(text: String, modifier: Modifier = Modifier) {
    Text(
        text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1,
        modifier = modifier.clip(RoundedCornerShape(50)).background(Color(0xFF0754FF)).padding(horizontal = 9.dp, vertical = 2.dp),
    )
}

/** 크루 머리 띠 — 이미지 · 이름 · "공덕 · 크루장 준호"(신청 · 관리 · 목표 화면 위) */
@Composable
internal fun CrewIdentityStrip(card: CrewCard, modifier: Modifier = Modifier, sub: String? = null, boxed: Boolean = true) {
    val ink = crewInk()
    Row(
        modifier.fillMaxWidth()
            .then(if (boxed) Modifier.crewPanel(ink, 18.dp).padding(horizontal = 14.dp, vertical = 14.dp) else Modifier.padding(vertical = 4.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CrewImage(card, if (boxed) 64.dp else 80.dp, 16.dp)
        Column(Modifier.weight(1f).padding(start = 18.dp)) {
            Text(card.name, style = crewTitleStyle(ink.text, if (boxed) 22.sp else 25.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(sub ?: crewAreaLeader(card), color = ink.text.copy(alpha = 0.86f), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun crewAreaLeader(card: CrewCard): String {
    val leader = stringResource(R.string.crew_leader_named, card.leaderName)
    return listOf(card.area.takeIf { it.isNotBlank() }, leader).filterNotNull().joinToString(" · ")
}

/** 큰 제목 · 설명(목록 · 명단 · 만들기 화면 위) */
@Composable
internal fun CrewHeading(title: String, modifier: Modifier = Modifier, sub: String? = null, titleTag: String? = null, eyebrow: String? = null) {
    val ink = crewInk()
    Column(modifier.fillMaxWidth()) {
        if (eyebrow != null) {
            Text(eyebrow, color = ink.info, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
        }
        Text(
            title, style = crewTitleStyle(ink.text, 28.sp),
            modifier = Modifier.semantics { heading() }.then(if (titleTag != null) Modifier.testTag(titleTag) else Modifier),
        )
        if (sub != null) {
            Spacer(Modifier.height(8.dp))
            Text(sub, color = ink.secondary, fontSize = 16.sp, lineHeight = 23.sp)
        }
    }
}

/** 굵은 제목 글자 — 시안의 큰 한국어 제목 */
internal fun crewTitleStyle(color: Color, size: TextUnit): TextStyle = TextStyle(
    fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = size, lineHeight = 1.3.em,
    letterSpacing = (-0.02).em, color = color,
)

/**
 * 시트 — 불투명 남색 면 · 파란 윗선 · 손잡이 · 오른쪽 위 닫기. 뒤 화면은 한 번만 어둡게 덮여 누를 수 없고,
 * 닫기 · 바깥 · 뒤로는 바꾸기 전으로 돌아간다. 보내는 중([dismissible] 거짓)에는 닫히지 않는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CrewSheet(
    title: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissible: Boolean = true,
    /** 제목 위 작은 청록 글자(크루 이름) */
    eyebrow: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val ink = crewInk()
    // confirmValueChange 는 시트 상태를 기억하는 열쇠다 — 보내는 중(dismissible)이 바뀔 때 새 상태로 다시 떠오르지 않게 한 번만 만든다
    val canDismiss by rememberUpdatedState(dismissible)
    val confirm = remember { { value: androidx.compose.material3.SheetValue -> canDismiss || value != androidx.compose.material3.SheetValue.Hidden } }
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = confirm)
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ModalBottomSheet(
        onDismissRequest = { if (dismissible) onDismiss() },
        sheetState = state,
        shape = shape,
        containerColor = Color.Transparent,
        contentColor = ink.text,
        scrimColor = ink.scrim,
        dragHandle = null,
        properties = androidx.compose.material3.ModalBottomSheetProperties(shouldDismissOnBackPress = dismissible),
        modifier = modifier,
    ) {
        Box(
            Modifier.fillMaxWidth().clip(shape)
                .background(Brush.verticalGradient(listOf(ink.sheetTop, ink.sheet)), shape)
                .border(1.dp, ink.sheetEdge, shape),
        ) {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().imePadding()
                    .padding(start = CrewGutter, end = CrewGutter, top = 12.dp, bottom = 18.dp),
            ) {
                Box(
                    Modifier.align(Alignment.CenterHorizontally).size(width = 44.dp, height = 5.dp)
                        .clip(RoundedCornerShape(3.dp)).background(ink.handle),
                )
                if (title != null) {
                    Spacer(Modifier.height(22.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f).padding(top = 4.dp)) {
                            if (eyebrow != null) {
                                Text(eyebrow, color = ink.info, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(4.dp))
                            }
                            Text(title, style = crewTitleStyle(ink.text, 26.sp), modifier = Modifier.semantics { heading() })
                        }
                        Box(
                            Modifier.size(48.dp).clip(CircleShape)
                                .feedbackClickable(enabled = dismissible, role = Role.Button, cue = FeedbackCue.Back, onClick = onDismiss)
                                .testTag("crew-sheet-close"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Close, stringResource(R.string.common_close), tint = ink.text, modifier = Modifier.size(26.dp))
                        }
                    }
                } else {
                    Spacer(Modifier.height(14.dp))
                }
                content()
            }
        }
    }
}

/** 시트 · 화면 안의 짧은 오류 줄 — 코랄 느낌표 · 글(색만으로 알리지 않게 그림을 함께) */
@Composable
internal fun CrewErrorLine(text: String, modifier: Modifier = Modifier, boxed: Boolean = false) {
    val ink = crewInk()
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier.fillMaxWidth()
            .then(
                if (boxed) Modifier.clip(shape).background(ink.danger.copy(alpha = if (StepUpColors.dark) 0.7f else 1f), shape)
                    .border(1.dp, ink.dangerBase, shape).padding(horizontal = 14.dp, vertical = 12.dp)
                else Modifier,
            ),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = ink.warn, modifier = Modifier.padding(top = 1.dp).size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = ink.warn, fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.weight(1f))
    }
}

/**
 * 확인 시트 — (크루 이름) · 큰 질문 · 설명, 아래 주 행동 하나와 글자 "취소"를 세로로. 보내는 중에는 닫히지 않고,
 * 실패하면 같은 시트 안에 오류를 보이고 그대로 다시 누를 수 있다.
 */
@Composable
internal fun CrewConfirmSheet(
    title: String,
    body: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    busy: Boolean = false,
    danger: Boolean = false,
    tag: String = "crew-confirm",
    error: String? = null,
    eyebrow: String? = null,
    /** 아래 글자 행동 — 기본은 "취소"(작성 중 나가기는 "계속 작성") */
    cancel: String? = null,
) {
    val ink = crewInk()
    CrewSheet(title, onDismiss, Modifier.testTag(tag), dismissible = !busy, eyebrow = eyebrow) {
        Spacer(Modifier.height(12.dp))
        Text(body, color = ink.text, fontSize = 16.sp, lineHeight = 25.sp)
        if (error != null) {
            Spacer(Modifier.height(14.dp))
            CrewErrorLine(error, Modifier.testTag("crew-confirm-error"), boxed = true)
        }
        Spacer(Modifier.height(28.dp))
        CrewButton(
            confirm, onConfirm, Modifier.testTag("$tag-yes"),
            if (danger) CrewButtonKind.DANGER else CrewButtonKind.PRIMARY, busy = busy,
        )
        Spacer(Modifier.height(4.dp))
        CrewTextAction(cancel ?: stringResource(R.string.common_cancel), onDismiss, Modifier.testTag("$tag-no"), enabled = !busy)
    }
}

/** 결과 화면의 동그라미 — 남색 원 · 청록 체크(또는 안내) · 은은한 빛 */
@Composable
internal fun CrewResultArt(info: Boolean = false, modifier: Modifier = Modifier) {
    val ink = crewInk()
    Box(
        modifier.size(112.dp).drawBehind {
            drawCircle(
                Brush.radialGradient(
                    0f to ink.info.copy(alpha = if (StepUpColors.dark) 0.22f else 0.12f), 1f to Color.Transparent,
                    center = center, radius = size.minDimension * 0.6f,
                ),
                radius = size.minDimension * 0.6f,
            )
            drawCircle(if (StepUpColors.dark) Color(0xFF0B2F5E) else Color(0xFFE1EBFF), radius = size.minDimension / 2.3f)
        },
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (info) Icons.Outlined.Info else Icons.Filled.Check, contentDescription = null, tint = ink.info, modifier = Modifier.size(54.dp))
    }
}

/**
 * 결과 화면(알림) — (크루 띠) · 동그라미 체크 · 제목 · 설명, 아래 큰 버튼 하나와 글자 행동 하나.
 * [info] 면 미승인처럼 왼쪽 정렬 큰 제목(그림 없음)으로 보인다.
 */
@Composable
internal fun CrewResultPage(
    title: String,
    body: String,
    button: String,
    onButton: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    info: Boolean = false,
    /** 큰 버튼 아래의 보조 행동 하나(예: 크루 채팅 열기) — 청록 글자 */
    secondary: String? = null,
    onSecondary: (() -> Unit)? = null,
    /** 위 크루 띠(가입 결과 18 · 19) */
    header: (@Composable () -> Unit)? = null,
    /** 왼쪽 정렬 결과의 작은 머리말("가입 신청 결과") */
    eyebrow: String? = null,
    /** 머리 제목 — 기본 "알림"(만들기 완료는 "크루 만들기") */
    topTitle: String? = null,
) {
    val ink = crewInk()
    CrewPage(modifier) {
        CrewTopBar(topTitle ?: stringResource(R.string.crew_notice_title), onBack)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = CrewGutter),
            horizontalAlignment = if (info) Alignment.Start else Alignment.CenterHorizontally,
        ) {
            if (header != null) {
                Spacer(Modifier.height(12.dp))
                header()
            }
            if (info) {
                Spacer(Modifier.height(if (header != null) 40.dp else 72.dp))
                if (eyebrow != null) {
                    Text(eyebrow, color = ink.secondary, fontSize = 15.sp)
                    Spacer(Modifier.height(10.dp))
                }
                Text(title, style = crewTitleStyle(ink.text, 32.sp), modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(16.dp))
                Text(body, color = ink.text, fontSize = 17.sp, lineHeight = 26.sp)
            } else {
                Spacer(Modifier.height(if (header != null) 56.dp else 96.dp))
                CrewResultArt()
                Spacer(Modifier.height(28.dp))
                Text(title, style = crewTitleStyle(ink.text, 27.sp), textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(14.dp))
                Text(body, color = ink.text, fontSize = 16.sp, lineHeight = 25.sp, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(24.dp))
        }
        CrewBottomBar {
            CrewButton(button, onButton, Modifier.testTag("crew-result-button"))
            if (secondary != null && onSecondary != null) {
                Spacer(Modifier.height(4.dp))
                CrewTextAction(secondary, onSecondary, Modifier.testTag("crew-result-secondary"))
            }
        }
    }
}

/** 가운데 안내(빈 목록 · 실패 · 볼 수 없음) — 그림 · 제목 · 설명 · 버튼들 */
@Composable
internal fun CrewEmptyState(
    icon: @Composable () -> Unit,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    buttons: @Composable ColumnScope.() -> Unit = {},
) {
    val ink = crewInk()
    Column(modifier.fillMaxWidth().padding(horizontal = CrewGutter), horizontalAlignment = Alignment.CenterHorizontally) {
        icon()
        Spacer(Modifier.height(22.dp))
        Text(
            title, style = crewTitleStyle(ink.text, 26.sp), textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        if (body.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(body, color = ink.secondary, fontSize = 16.sp, lineHeight = 24.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(28.dp))
        Column(
            Modifier.widthIn(max = 340.dp).fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = buttons,
        )
    }
}

/** 빈 화면 그림 — 옅은 파랑 선 그림(원 없이). [circled] 면 파란 원 안에(검색 결과 없음) */
@Composable
internal fun CrewStateIcon(icon: ImageVector, modifier: Modifier = Modifier, circled: Boolean = false) {
    val ink = crewInk()
    val tint = if (StepUpColors.dark) Color(0xFFBFD6FF) else ink.link
    if (circled) {
        Box(
            modifier.size(92.dp).clip(CircleShape).background(ink.avatar).border(1.5.dp, ink.fieldEdge, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = ink.info, modifier = Modifier.size(46.dp)) }
    } else {
        Icon(icon, contentDescription = null, tint = tint, modifier = modifier.size(64.dp))
    }
}

/** 불러오는 중의 빈 모양 — 가짜 크루 정보를 먼저 보이지 않는다 */
@Composable
internal fun CrewSkeletonBox(modifier: Modifier, radius: Dp = 10.dp) {
    val ink = crewInk()
    Box(modifier.clip(RoundedCornerShape(radius)).background(ink.skeleton))
}

/** 입력칸 이름 · 칸 · 도움말(또는 오류) */
@Composable
internal fun CrewFieldLabel(text: String, modifier: Modifier = Modifier) {
    val ink = crewInk()
    Text(text, color = ink.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = modifier)
}

@Composable
internal fun CrewHelp(text: String, error: Boolean = false, modifier: Modifier = Modifier) {
    val ink = crewInk()
    Text(
        text, color = if (error) ink.warn else ink.secondary, fontSize = 14.sp, lineHeight = 20.sp,
        fontWeight = if (error) FontWeight.SemiBold else FontWeight.Normal, modifier = modifier,
    )
}

/** 입력칸 — 남색 칸 · 파란 테두리(쓰는 중이면 밝게, 오류면 코랄) */
@Composable
internal fun CrewTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    minHeight: Dp = 56.dp,
    keyboard: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    maxChars: Int = Int.MAX_VALUE,
    suffix: String? = null,
    error: Boolean = false,
) {
    val ink = crewInk()
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = { next -> onValueChange(if (next.length > maxChars) next.take(maxChars) else next) },
        singleLine = singleLine,
        keyboardOptions = keyboard,
        textStyle = TextStyle(fontFamily = StepUpSans, color = ink.text, fontSize = 17.sp, lineHeight = 26.sp),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(ink.info),
        modifier = modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = minHeight).clip(shape).background(ink.field, shape)
                    .border(
                        if (focused || error) 1.5.dp else 1.dp,
                        when {
                            error -> ink.warn
                            focused -> Color(0xFF4D8BFF)
                            else -> ink.fieldEdge
                        },
                        shape,
                    )
                    .padding(horizontal = 18.dp, vertical = 15.dp),
                verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
            ) {
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) Text(placeholder, color = ink.secondary.copy(alpha = 0.8f), fontSize = 17.sp, lineHeight = 26.sp)
                    inner()
                }
                if (suffix != null) {
                    Box(Modifier.padding(start = 12.dp).width(1.dp).height(24.dp).background(ink.divider))
                    Text(suffix, color = ink.secondary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 14.dp))
                }
            }
        },
    )
}

/** 입력처럼 보이는 고르기 줄(활동 지역 · 정기 모임) */
@Composable
internal fun CrewPickerField(text: String, placeholder: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val ink = crewInk()
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = 58.dp).clip(shape).background(ink.field, shape)
            .border(1.dp, if (enabled) ink.fieldEdge else ink.divider, shape)
            .feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text.ifEmpty { placeholder }, color = if (text.isEmpty() || !enabled) ink.secondary else ink.text, fontSize = 17.sp,
            fontWeight = if (text.isEmpty()) FontWeight.Normal else FontWeight.SemiBold,
            modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = if (enabled) ink.text else ink.secondary, modifier = Modifier.size(22.dp))
    }
}

/** 화면 바탕 — 남색, 위가 조금 밝고 오른쪽 위에서 파란 빛이 번진다(러닝 리메이크와 같은 바닥) */
@Composable
internal fun CrewPage(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxSize()) {
        RunBackdrop(Modifier.matchParentSize())
        Column(Modifier.fillMaxSize(), content = content)
    }
}

/** 아래 고정 버튼 자리 — 위 한 줄 설명(오류면 코랄 느낌표) · 버튼. 시스템 아래 영역 위에 선다 */
@Composable
internal fun CrewBottomBar(caption: String? = null, captionError: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val ink = crewInk()
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = CrewGutter).padding(top = 8.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (caption != null) {
            if (captionError) {
                CrewErrorLine(caption, Modifier.padding(bottom = 12.dp).testTag("crew-footer-caption"))
            } else {
                Text(
                    caption, color = ink.text.copy(alpha = 0.86f), fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp).testTag("crew-footer-caption"),
                )
            }
        }
        content()
    }
}

/** 화면이 너비를 모를 때 쓰는 비율(작은 화면) */
@Composable
internal fun rememberCompact(content: @Composable (Boolean) -> Unit) {
    BoxWithConstraints { content(maxWidth < 360.dp) }
}

/** 비트맵이 없을 때의 편의 */
internal fun Bitmap?.orImage(): ImageBitmap? = this?.asImageBitmap()

/** 글자 스타일 — 러닝 리메이크와 같은 글꼴 · 줄 간격 */
internal fun crewText(size: TextUnit, color: Color, weight: FontWeight = FontWeight.Normal): TextStyle = runTextStyle(size, color, weight)
