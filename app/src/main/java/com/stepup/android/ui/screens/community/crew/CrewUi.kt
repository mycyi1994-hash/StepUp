package com.stepup.android.ui.screens.community.crew

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.CrewCard
import com.stepup.android.domain.CrewGoalProgress
import com.stepup.android.domain.CrewRules
import com.stepup.android.ui.components.SecondaryHeader
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.StepUpDesign

/*
 * 크루 명함형 — 화면들이 함께 쓰는 부품. 시안(design-tokens.json · 03-components)의 값을 그대로 옮겼다:
 * 좌우 24 · 카드 모서리 18 · 입력칸 14 · 시트 26 · 주 버튼 52(모서리 15) · 선택 버튼 42(모서리 12).
 * 금속 배지 · 방패 · 왕관 · 게임 등급 · XP 바는 두지 않는다 — 레벨은 짙은 파랑 칩 하나, 목표는 가는 진행 바 하나.
 */

/** 시안 색 — 어두운 테마는 시안 값, 밝은 테마는 같은 관계의 밝은 값 */
@Immutable
internal class CrewInk(
    val canvas: Color,
    val card: Color,
    val sheet: Color,
    val text: Color,
    val secondary: Color,
    /** 정보 · 링크(푸른 정보색) */
    val info: Color,
    val divider: Color,
    val tabMark: Color,
    /** 실패 · 주의 글자 */
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
    val primaryFace: Brush,
    val primaryText: Color,
    val skeleton: Color,
    val handle: Color,
    val scrim: Color,
)

private val DarkCrewInk = CrewInk(
    canvas = Color(0xFF050912), card = Color(0xFF0D1829), sheet = Color(0xFF111C2D), text = Color(0xFFF2F4FC),
    secondary = Color(0xFF98A8C0), info = Color(0xFFA3BFFE), divider = Color(0xFF1D2B3F), tabMark = Color(0xFF467CFF),
    warn = Color(0xFFE5B49B), choice = Color(0xFF16253A), choiceText = Color(0xFF98A8C0), choiceOn = Color(0xFFDCE8FF),
    choiceOnText = Color(0xFF132641), level = Color(0xFF223851), levelText = Color(0xFFC3D8FF), track = Color(0xFF253852),
    bar = Color(0xFF99B9FF), avatar = Color(0xFF263B55), avatarText = Color(0xFFA3BFFE), secondaryButton = Color(0xFF1D304A),
    disabled = Color(0xFF202C3D), disabledText = Color(0xFF8795AA), danger = Color(0xFF503332), dangerText = Color(0xFFFFDED4),
    primaryFace = Brush.verticalGradient(listOf(Color(0xFFF8F9FF), Color(0xFFEDF0F9))), primaryText = Color(0xFF0B1423),
    skeleton = Color(0xFF1A2940), handle = Color(0xFF46576F), scrim = Color(0xB3050912),
)

private val LightCrewInk = CrewInk(
    canvas = Color(0xFFF4F6FB), card = Color(0xFFFFFFFF), sheet = Color(0xFFFFFFFF), text = Color(0xFF10203B),
    secondary = Color(0xFF536580), info = Color(0xFF335EAB), divider = Color(0xFFDCE3EE), tabMark = Color(0xFF2F63D8),
    warn = Color(0xFF94561C), choice = Color(0xFFE7EDF6), choiceText = Color(0xFF536580), choiceOn = Color(0xFF1B2D4E),
    choiceOnText = Color(0xFFFFFFFF), level = Color(0xFFDCE7FA), levelText = Color(0xFF274C8F), track = Color(0xFFDCE4F0),
    bar = Color(0xFF335EAB), avatar = Color(0xFFDCE7FA), avatarText = Color(0xFF274C8F), secondaryButton = Color(0xFFE2E9F4),
    disabled = Color(0xFFD5DCE8), disabledText = Color(0xFF7B8AA0), danger = Color(0xFFF6DDD6), dangerText = Color(0xFF7A2E1E),
    primaryFace = Brush.verticalGradient(listOf(Color(0xFF1B2D4E), Color(0xFF10203B))), primaryText = Color(0xFFFFFFFF),
    skeleton = Color(0xFFDCE4F0), handle = Color(0xFFB8C3D4), scrim = Color(0x80101828),
)

@Composable
internal fun crewInk(): CrewInk = if (StepUpColors.dark) DarkCrewInk else LightCrewInk

internal val CrewGutter = 24.dp

/** 이름 이미지 바탕 네 가지(시안 30) — 글자는 옅은 초록빛 흰색 */
internal val CrewNamedColors = listOf(Color(0xFF263D47), Color(0xFF403343), Color(0xFF233C58), Color(0xFF383E32))
private val CrewNamedText = Color(0xFFE9EEE6)

/** 크루장 동그라미 — 크루 바탕보다 조금 밝은 톤(시안의 크루장 색과 같은 관계) */
internal val CrewLeaderColors = listOf(Color(0xFF34505B), Color(0xFF4B3D4E), Color(0xFF2E4A6A), Color(0xFF474D3F))

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

/** 이름 이미지의 글자 — 이름 그대로, 길면 두 줄 · 작게 */
@Composable
private fun CrewNameText(name: String, size: Dp, textSize: TextUnit?) {
    val label = name.trim().ifEmpty { "·" }
    val base = textSize ?: (size.value * 0.24f).sp
    val scaled = when {
        label.length <= 4 -> base
        label.length <= 6 -> base * 0.82f
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

/** 레벨 칩 — 저장된 레벨이 없으면 "새 크루" */
@Composable
internal fun CrewLevelChip(level: Int?, modifier: Modifier = Modifier) {
    val ink = crewInk()
    Box(
        modifier.clip(RoundedCornerShape(7.dp)).background(ink.level).padding(horizontal = 12.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (level != null) stringResource(R.string.crew_level_value, level) else stringResource(R.string.crew_level_new),
            color = ink.levelText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
        )
    }
}

/** 사람 둘 그림 + "24 / 30명" (정원이 없으면 "24명") */
@Composable
internal fun CrewMembersLabel(members: Int, capacity: Int?, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        PeopleIcon(color, Modifier.size(16.dp))
        Text(
            if (capacity != null) stringResource(R.string.crew_members_of, members, capacity) else stringResource(R.string.crew_members_only, members),
            color = color, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
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
        Text(CrewRules.initial(name), color = textColor, fontSize = (size.value * 0.34f).sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** 크루장 동그라미 색 — 크루 바탕과 같은 결 */
internal fun leaderFace(card: CrewCard): Color = CrewLeaderColors[Math.floorMod(card.bg, CrewLeaderColors.size)]

/** 이번 주 목표 한 줄 — "이번 주 126 / 160km ━━━━━ 79%" */
@Composable
internal fun CrewWeeklyLine(progress: CrewGoalProgress, modifier: Modifier = Modifier) {
    val ink = crewInk()
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.crew_week_line, CrewRules.km(progress.doneKm), progress.goalKm),
            color = ink.secondary, fontSize = 11.5.sp, maxLines = 1, modifier = Modifier.weight(1f),
        )
        CrewBar(progress.fraction, Modifier.width(121.dp).height(5.dp))
        Text(
            stringResource(R.string.crew_percent, progress.percent), color = ink.info, fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.widthIn(min = 40.dp),
        )
    }
}

/** 가는 진행 바 — 100% 까지만 채운다 */
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

/** 주 버튼 52dp · 모서리 15. 처리 중이면 도는 표시와 함께 다시 누를 수 없다 */
@Composable
internal fun CrewButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: CrewButtonKind = CrewButtonKind.PRIMARY,
    busy: Boolean = false,
    enabled: Boolean = true,
) {
    val ink = crewInk()
    val shape = RoundedCornerShape(15.dp)
    val active = enabled && !busy && kind != CrewButtonKind.DISABLED
    val face: Modifier = when {
        kind == CrewButtonKind.DISABLED || !enabled -> Modifier.background(ink.disabled, shape)
        kind == CrewButtonKind.PRIMARY -> Modifier.background(ink.primaryFace, shape)
        kind == CrewButtonKind.DANGER -> Modifier.background(ink.danger, shape)
        else -> Modifier.background(ink.secondaryButton, shape)
    }
    val color = when {
        kind == CrewButtonKind.DISABLED || !enabled -> ink.disabledText
        kind == CrewButtonKind.PRIMARY -> ink.primaryText
        kind == CrewButtonKind.DANGER -> ink.dangerText
        else -> ink.text
    }
    Row(
        modifier.fillMaxWidth().heightIn(min = 52.dp).clip(shape).then(face)
            .feedbackClickable(enabled = active, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(16.dp), color = color, strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = color, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

/** 작은 흰 버튼 — 카드의 "크루 보기"(32dp 면, 누르는 곳 44dp) */
@Composable
internal fun CrewSmallButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ink = crewInk()
    Box(
        modifier.heightIn(min = 44.dp).feedbackClickable(role = Role.Button, onClick = onClick).padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.widthIn(min = 101.dp).heightIn(min = 32.dp).clip(RoundedCornerShape(12.dp)).background(ink.primaryFace)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, color = ink.primaryText, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

/** 고르는 버튼(42dp · 모서리 12) — 고르면 옅은 파랑 면 · 짙은 글자 */
@Composable
internal fun CrewChoice(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val ink = crewInk()
    Box(
        modifier.heightIn(min = 48.dp).feedbackClickable(enabled = enabled, role = Role.Checkbox, onClick = onClick)
            .padding(vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.fillMaxWidth().heightIn(min = 42.dp).clip(RoundedCornerShape(12.dp))
                .background(if (selected) ink.choiceOn else ink.choice).padding(horizontal = 8.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text, color = if (selected) ink.choiceOnText else ink.choiceText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, lineHeight = 17.sp,
            )
        }
    }
}

/** 한 줄에 버튼 여러 개 — 폭을 나눈다 */
@Composable
internal fun CrewChoiceRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp), content = content)
}

// ── 머리 · 줄 · 시트 ───────────────────────────────────────────

/** 크루 화면 머리 — 앱 공통 하위 화면 머리(뒤로 · 가운데 제목 · 오른쪽 보조 행동 하나) */
@Composable
internal fun CrewTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, onMore: (() -> Unit)? = null) {
    val ink = crewInk()
    SecondaryHeader(
        onBack = onBack, balance = null, onOpenWallet = null, title = title,
        modifier = modifier.padding(horizontal = 8.dp),
        trailing = onMore?.let { more ->
            {
                Box(
                    Modifier.size(width = 56.dp, height = StepUpDesign.TouchTarget).clip(RoundedCornerShape(12.dp))
                        .feedbackClickable(role = Role.Button, onClick = more).testTag("crew-more"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("•••", color = ink.info, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
    )
}

/** 설정 줄 — 이름, 오른쪽 값(푸른 정보색), 꺾쇠, 아래 선 */
@Composable
internal fun CrewRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    titleColor: Color? = null,
    titleSize: TextUnit = 16.sp,
    divider: Boolean = true,
) {
    val ink = crewInk()
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 59.dp).feedbackClickable(role = Role.Button, onClick = onClick)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title, color = titleColor ?: ink.text, fontSize = titleSize, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            if (value != null) {
                Text(value, color = ink.info, fontSize = 13.sp, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = ink.secondary, modifier = Modifier.padding(start = 4.dp).size(18.dp))
        }
        if (divider) Box(Modifier.fillMaxWidth().height(0.7.dp).background(ink.divider))
    }
}

/** 사람 한 줄 — 동그라미 · 이름 · 역할 줄 · 꺾쇠 */
@Composable
internal fun CrewPersonRow(
    name: String,
    sub: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subColor: Color? = null,
) {
    val ink = crewInk()
    Row(
        modifier.fillMaxWidth().heightIn(min = 75.dp).feedbackClickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CrewAvatar(name, 36.dp, ink.avatar, ink.avatarText)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                    Text(name, color = ink.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(5.dp))
                    Text(sub, color = subColor ?: ink.secondary, fontSize = 12.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = ink.secondary, modifier = Modifier.size(18.dp))
            }
            Box(Modifier.fillMaxWidth().height(0.7.dp).background(ink.divider))
        }
    }
}

/** 크루 머리 띠 — 이미지 · 이름 · "공덕 · 크루장 준호"(신청 · 관리 · 목표 화면 위) */
@Composable
internal fun CrewIdentityStrip(card: CrewCard, modifier: Modifier = Modifier, sub: String? = null) {
    val ink = crewInk()
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ink.card).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CrewImage(card, 60.dp, 14.dp)
        Column(Modifier.weight(1f).padding(start = 15.dp)) {
            Text(card.name, color = ink.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(sub ?: crewAreaLeader(card), color = ink.secondary, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun crewAreaLeader(card: CrewCard): String {
    val leader = stringResource(R.string.crew_leader_named, card.leaderName)
    return listOf(card.area.takeIf { it.isNotBlank() }, leader).filterNotNull().joinToString(" · ")
}

/** 시트 — 배경은 누를 수 없고, 닫기 · 취소는 바꾸기 전으로 돌아간다 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CrewSheet(
    title: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val ink = crewInk()
    // confirmValueChange 는 시트 상태를 기억하는 열쇠다 — 보내는 중(dismissible)이 바뀔 때 새 상태로 다시 떠오르지 않게 한 번만 만든다
    val canDismiss by rememberUpdatedState(dismissible)
    val confirm = remember { { value: androidx.compose.material3.SheetValue -> canDismiss || value != androidx.compose.material3.SheetValue.Hidden } }
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = confirm)
    ModalBottomSheet(
        onDismissRequest = { if (dismissible) onDismiss() },
        sheetState = state,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        containerColor = ink.sheet,
        contentColor = ink.text,
        scrimColor = ink.scrim,
        dragHandle = {
            Box(Modifier.padding(top = 12.dp, bottom = 6.dp).size(width = 42.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(ink.handle))
        },
        modifier = modifier,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = CrewGutter, end = CrewGutter, bottom = 16.dp)) {
            if (title != null) {
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = ink.text, fontSize = 24.sp, lineHeight = 31.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Box(
                        Modifier.size(StepUpDesign.TouchTarget).clip(CircleShape)
                            .feedbackClickable(enabled = dismissible, role = Role.Button, cue = FeedbackCue.Back, onClick = onDismiss)
                            .testTag("crew-sheet-close"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Close, stringResource(R.string.common_close), tint = ink.secondary, modifier = Modifier.size(20.dp))
                    }
                }
            }
            content()
        }
    }
}

/** 확인 시트 — 설명 두 줄과 [취소 · 행동] 버튼 */
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
) {
    val ink = crewInk()
    CrewSheet(title, onDismiss, Modifier.testTag(tag), dismissible = !busy) {
        Spacer(Modifier.height(18.dp))
        Text(body, color = ink.secondary, fontSize = 15.sp, lineHeight = 25.sp)
        if (error != null) {
            Spacer(Modifier.height(14.dp))
            Text(error, color = ink.warn, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.testTag("crew-confirm-error"))
        }
        Spacer(Modifier.height(96.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            CrewButton(stringResource(R.string.common_cancel), onDismiss, Modifier.weight(1f), CrewButtonKind.SECONDARY, enabled = !busy)
            CrewButton(
                confirm, onConfirm, Modifier.weight(1f).testTag("$tag-yes"),
                if (danger) CrewButtonKind.DANGER else CrewButtonKind.PRIMARY, busy = busy,
            )
        }
    }
}

/** 결과 화면(알림) — 동그라미 표시 · 제목 · 설명, 아래 큰 버튼 하나 */
@Composable
internal fun CrewResultPage(
    title: String,
    body: String,
    button: String,
    onButton: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    info: Boolean = false,
) {
    val ink = crewInk()
    Column(modifier.fillMaxSize().background(ink.canvas)) {
        CrewTopBar(stringResource(R.string.crew_notice_title), onBack)
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = CrewGutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(110.dp))
            Box(Modifier.size(66.dp).clip(CircleShape).background(ink.avatar), contentAlignment = Alignment.Center) {
                Icon(if (info) Icons.Outlined.Info else Icons.Filled.Check, contentDescription = null, tint = ink.info, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(30.dp))
            Text(title, color = ink.text, fontSize = 24.sp, lineHeight = 31.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Text(body, color = ink.secondary, fontSize = 14.sp, lineHeight = 26.sp, textAlign = TextAlign.Center)
        }
        CrewButton(button, onButton, Modifier.padding(horizontal = CrewGutter).padding(bottom = 24.dp).navigationBarsPadding().testTag("crew-result-button"))
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
        Spacer(Modifier.height(24.dp))
        Text(title, color = ink.text, fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(body, color = ink.secondary, fontSize = 13.5.sp, lineHeight = 21.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(44.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp), content = buttons)
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
    Text(text, color = ink.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = modifier)
}

@Composable
internal fun CrewHelp(text: String, error: Boolean = false, modifier: Modifier = Modifier) {
    val ink = crewInk()
    Text(text, color = if (error) ink.warn else ink.secondary, fontSize = 12.sp, lineHeight = 18.sp, modifier = modifier)
}

/** 입력칸 — 모서리 14 · 51dp 이상 */
@Composable
internal fun CrewTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    minHeight: Dp = 51.dp,
    keyboard: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    maxChars: Int = Int.MAX_VALUE,
    suffix: String? = null,
) {
    val ink = crewInk()
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = { next -> onValueChange(if (next.length > maxChars) next.take(maxChars) else next) },
        singleLine = singleLine,
        keyboardOptions = keyboard,
        textStyle = TextStyle(fontFamily = com.stepup.android.ui.theme.StepUpSans, color = ink.text, fontSize = 15.sp, lineHeight = 24.sp),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(ink.info),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = minHeight).clip(RoundedCornerShape(14.dp)).background(ink.card)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
            ) {
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) Text(placeholder, color = ink.secondary, fontSize = 15.sp, lineHeight = 24.sp)
                    inner()
                }
                if (suffix != null && value.isNotEmpty()) Text(suffix, color = ink.text, fontSize = 15.sp, modifier = Modifier.padding(start = 2.dp))
            }
        },
    )
}

/** 입력처럼 보이는 고르기 줄(활동 지역 · 정기 모임) */
@Composable
internal fun CrewPickerField(text: String, placeholder: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val ink = crewInk()
    Row(
        modifier.fillMaxWidth().heightIn(min = 51.dp).clip(RoundedCornerShape(14.dp)).background(ink.card)
            .feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text.ifEmpty { placeholder }, color = if (text.isEmpty() || !enabled) ink.secondary else ink.text, fontSize = 15.sp,
            modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = ink.secondary, modifier = Modifier.size(18.dp))
    }
}

/** 화면 바탕 — 시안의 검정에 가까운 남색 */
@Composable
internal fun CrewPage(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val ink = crewInk()
    Column(modifier.fillMaxSize().background(ink.canvas), content = content)
}

/** 아래 고정 버튼 자리 — 본문이 가려지지 않게 본문 쪽이 이만큼 여백을 둔다 */
@Composable
internal fun CrewBottomBar(caption: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val ink = crewInk()
    Column(
        Modifier.fillMaxWidth().background(ink.canvas).navigationBarsPadding().padding(horizontal = CrewGutter).padding(top = 8.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (caption != null) {
            Text(caption, color = ink.secondary, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 12.dp).testTag("crew-footer-caption"))
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
