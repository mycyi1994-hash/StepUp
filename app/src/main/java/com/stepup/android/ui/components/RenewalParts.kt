package com.stepup.android.ui.components

import com.stepup.android.ui.theme.StepUpDesign
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height

import androidx.compose.foundation.background
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.theme.Carbon
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.Cyan
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.OnVolt
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.StepUpNumbers
import com.stepup.android.ui.theme.Volt
import com.stepup.android.ui.theme.VoltPlate
import com.stepup.android.ui.theme.VoltText

/**
 * 리뉴얼 화면들이 함께 쓰는 조각.
 *
 * 러닝 · 꾸미기 · 커뮤니티 · 내 정보와 그 밑의 소식 · 챌린지 · 러너 마켓이
 * 같은 머리글과 같은 포인트 표시, 같은 큰 버튼을 쓴다. 화면마다 따로
 * 만들면 아홉 화면이 조금씩 다른 앱처럼 보인다.
 */

/**
 * 보유 SUP 알약 — 통합안: 회색 채움 알약(높이 36) · 왼쪽 작은 동전 · "0 SUP"(14 · 600). 누르면 지갑.
 * 내림 — 499.6 을 500 으로 보이면 500 SUP 짜리를 살 수 있는 것처럼 보인다(서버는 소수 4자리).
 */
@Composable
fun SupPill(balance: Double?, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val ui = StepUpUi.colors
    val amount = balance?.let { formatSupDown(it, 2) } ?: "—"
    val label = stringResource(R.string.cd_sup_balance, amount)
    Box(
        modifier = modifier
            .heightIn(min = StepUpDesign.TouchTarget)
            .widthIn(max = 190.dp)
            .then(if (onClick != null) Modifier.feedbackClickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = label }
            .testTag("sup-balance"),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .heightIn(min = 36.dp)
                .clip(RoundedCornerShape(50))
                .background(ui.fill)
                .padding(start = 9.dp, end = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            SupCoin(Modifier.size(20.dp))
            Text(
                text = "$amount SUP",
                modifier = Modifier.weight(1f, fill = false),
                overflow = TextOverflow.Ellipsis,
                style = StepUpUi.type.smallButton.copy(fontWeight = FontWeight.SemiBold),
                color = ui.label,
                maxLines = 1,
            )
        }
    }
}

/** SUP 동전 — 흰 테두리 파란 원 안의 S */
@Composable
private fun SupCoin(modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFFEAF3FF), Color(0xFF9EC4FF))))
            .padding(2.5.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFF2F7BFF), Color(0xFF0A49DA)))),
        contentAlignment = Alignment.Center,
    ) {
        Text("S", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
    }
}

/**
 * 하위 화면 머리글 — 뒤로 · 제목 · 보유 SUP.
 *
 * 소식 · 챌린지 · 러너 마켓이 쓴다. 뒤로 가기가 늘 같은 자리에 있어야
 * 어느 화면에서든 손이 먼저 간다.
 */
@Composable
fun FocusHeader(title: String, onBack: () -> Unit, action: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = StepUpDesign.HeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DarkIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back), onBack,
            cue = FeedbackCue.Back)
        Text(
            title, modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            fontSize = StepUpDesign.PrimaryLabel, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center, color = Snow,
        )
        if (action != null) action() else Spacer(Modifier.size(StepUpDesign.TouchTarget))
    }
}

@Composable
fun SubHeader(
    title: String,
    onBack: () -> Unit,
    balance: Double?,
    onOpenWallet: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DarkIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.cd_back),
            onClick = onBack,
            cue = FeedbackCue.Back,
        )
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.6).sp,
            color = Snow,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        trailing?.invoke(this)
        if (balance != null) SupPill(balance, onOpenWallet)
    }
}

@Composable
fun MainHeader(
    balance: Double?,
    onOpenWallet: (() -> Unit)?,
    modifier: Modifier = Modifier,
    balanceModifier: Modifier = Modifier,
    /** 읽지 않은 알림 수 — 0 보다 크면 종에 작은 점 */
    unread: Int = 0,
    /** 알림함 — null 이면 종을 두지 않는다 */
    onOpenNotifications: (() -> Unit)? = null,
    /** 잔액 알약을 둔다(내 정보는 머리에 두지 않고 목록의 "지갑" 줄로) */
    showBalance: Boolean = true,
    /** 오른쪽 끝 원 버튼 하나(내 정보의 설정 톱니) — 종 대신 */
    trailing: (@Composable () -> Unit)? = null,
) {
    // UI 최종 통합안(2026-10-25) — 머리 줄 44: 왼쪽 워드마크(115×20), 오른쪽 회색 알약(SUP) · 회색 원 36(알림 · 설정)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag("main-header"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Wordmark(modifier = Modifier.weight(1f, fill = false).height(20.dp).width(115.dp))
        Spacer(Modifier.weight(1f))
        if (showBalance) SupPill(balance = balance, onClick = onOpenWallet, modifier = balanceModifier)
        if (trailing != null) trailing()
        else if (onOpenNotifications != null) NotificationBell(unread, onOpenNotifications)
    }
}

/** 머리글의 알림 종 — 회색 원 36(터치 48), 읽지 않은 알림이 있으면 오른쪽 위 작은 파란 점(수는 알림함에서) */
@Composable
private fun NotificationBell(unread: Int, onClick: () -> Unit) {
    val label = stringResource(R.string.cd_notifications) +
        if (unread > 0) " · " + stringResource(R.string.cd_notifications_unread, unread) else ""
    Box(
        Modifier.size(StepUpDesign.TouchTarget).clip(CircleShape)
            .feedbackClickable(onClick = onClick)
            .semantics { contentDescription = label }
            .testTag("header-notifications"),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(StepUpUi.colors.fill), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Notifications, contentDescription = null, tint = StepUpUi.colors.label, modifier = Modifier.size(20.dp))
        }
        if (unread > 0) {
            Box(
                Modifier.align(Alignment.Center).offset(x = 9.dp, y = (-10).dp).size(8.dp).clip(CircleShape)
                    .background(StepUpUi.colors.primary).testTag("header-notifications-dot"),
            )
        }
    }
}

/**
 * 하위 화면의 머리글 — 뒤로 · 가운데 로고 · 보유 SUP.
 *
 * 러너 마켓 · 소식 · 챌린지가 쓴다. 화면 이름은 이 줄이 아니라 아래 큰 제목이
 * 말한다([PageHero]).
 */
@Composable
fun SecondaryHeader(
    onBack: () -> Unit,
    balance: Double?,
    onOpenWallet: (() -> Unit)?,
    modifier: Modifier = Modifier,
    title: String? = null,
    /** 오른쪽 자리 — 글 메뉴(⋯)처럼 그 화면의 보조 행동 하나. 지갑 자리와 함께 쓰지 않는다 */
    trailing: (@Composable () -> Unit)? = null,
    /** 가운데 대신 쓸 제목 자리(예: 크루 채팅 방 — 대표 이미지 · 이름 · 인원). 있으면 [title] 은 쓰지 않는다 */
    titleContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = com.stepup.android.ui.theme.StepUpDesign.HeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DarkIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back), onClick = onBack,
            cue = FeedbackCue.Back)
        Box(Modifier.weight(1f), contentAlignment = if (titleContent != null) Alignment.CenterStart else Alignment.Center) {
            when {
                titleContent != null -> titleContent()
                title == null -> Wordmark()
                else -> Text(title, color = Snow, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
        }
        // Wallet-enabled headers keep the same slot while the balance is loading.
        if (trailing != null) trailing()
        else if (balance != null || onOpenWallet != null) SupPill(balance, onOpenWallet)
        else Spacer(Modifier.size(StepUpDesign.TouchTarget))
    }
}

/**
 * 하위 화면의 큰 제목 — 제목 · 한 줄 소개, 오른쪽에 캐릭터 그림.
 *
 * 글자가 크면 그림을 빼고 제목에 폭을 다 준다.
 */
@Composable
fun PageHero(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    setting: RunnerSetting = RunnerSetting.RunNight,
    art: (@Composable BoxScope.() -> Unit)? = null,
) {
    val large = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f
    val shape = RoundedCornerShape(22.dp)
    BoxWithConstraints(modifier.fillMaxWidth().clip(shape).border(1.dp, Volt.copy(alpha = 0.32f), shape)) {
        val showArt = art != null && !large && maxWidth >= 280.dp
        val artWidth = (maxWidth * 0.36f).coerceAtMost(146.dp)
        RunnerScene(Modifier.matchParentSize(), setting = setting, home = true)
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
            // Keep the text surface paired with its theme, even when scenery changes.
            0f to Carbon, 0.6f to Carbon.copy(alpha = 0.98f),
            1f to Carbon.copy(alpha = if (showArt) 0.65f else 0.98f),
        )))
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 190.dp)
                .padding(start = 18.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(title, fontSize = 30.sp, fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp, color = Snow, lineHeight = 34.sp)
                Text(subtitle, fontSize = 14.sp, color = Silver, lineHeight = 21.sp)
            }
            if (art != null && showArt) {
                Box(modifier = Modifier.size(width = artWidth, height = 162.dp), content = art)
            }
        }
    }
}

/**
 * 한 화면에서 가장 먼저 눌러야 하는 버튼 — 통합안 블루 알약 54. [showArrow] 면 끝에 흰 원 화살표.
 *
 * "러닝 시작"처럼 그 화면의 이유가 되는 행동 하나에만 쓴다. 한 화면에
 * 두 개가 있으면 어느 쪽도 먼저 보이지 않는다.
 */
@Composable
fun PrimaryCta(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    showArrow: Boolean = true,
) {
    PrimaryButton(
        text, onClick, modifier.fillMaxWidth(),
        state = if (enabled) ButtonState.Idle else ButtonState.Disabled,
        icon = icon, arrow = showArrow,
    )
}

/** 숫자 하나 — 아이콘 · 큰 값 · 단위 · 이름 */
@Composable
fun StatCell(
    icon: ImageVector,
    value: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = VoltText, modifier = Modifier.size(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = value,
                    fontFamily = StepUpNumbers,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Snow,
                    maxLines = 1,
                )
                if (unit.isNotEmpty()) {
                    Text(
                        text = unit,
                        fontSize = 12.sp,
                        color = Silver,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
            }
            Text(text = label, fontSize = 12.sp, color = Silver, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** 작은 바로가기 — 아이콘 · 이름 · 꺾쇠 */
@Composable
fun ShortcutButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** 이름 아래 흐린 한 줄 — 시안의 "지금 도전하세요!" 자리 */
    subtitle: String? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            // 보조 진입점도 공통 터치 높이를 확보한다.
            .heightIn(min = StepUpDesign.TouchTarget)
            .clip(shape)
            .background(CarbonHigh, shape)
            .border(1.dp, Edge, shape)
            .feedbackClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = VoltText, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Snow,
            )
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 14.sp, lineHeight = 20.sp, color = Silver)
            }
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Silver, modifier = Modifier.size(18.dp))
    }
}

/** A quiet destination row for long menus and profile shortcuts. */
@Composable
fun QuietListRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = 56.dp)
                .feedbackClickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(icon, contentDescription = null, tint = Silver, modifier = Modifier.size(22.dp))
            Text(label, modifier = Modifier.weight(1f), fontSize = 16.sp,
                fontWeight = FontWeight.Medium, color = Snow)
            Icon(Icons.Filled.ChevronRight, contentDescription = null,
                tint = Silver, modifier = Modifier.size(20.dp))
        }
        HairlineDivider()
    }
}

/** 작은 표시 — "NFT", "체험 중", "데모", "예시 일정" 같은 것 */
@Composable
fun SmallBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: BadgeTone = BadgeTone.Accent,
) {
    val color = when (tone) {
        BadgeTone.Accent -> VoltText
        BadgeTone.Glow -> if (StepUpColors.dark) Cyan else Color(0xFF086B83)
        BadgeTone.Muted -> Silver
        BadgeTone.Nft -> if (StepUpColors.dark) Color(0xFFAC90FF) else Color(0xFF5D2ABD)
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            // 신발·옷 그림 위에 얹힐 때가 있다. 반투명만 두면 흰 모자 위에서
            // 글자가 사라진다 — 불투명한 바닥을 먼저 깔고 그 위에 색을 얹는다.
            .background(Carbon)
            .background(color.copy(alpha = 0.04f))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text = text, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

enum class BadgeTone { Accent, Glow, Muted, Nft }

/**
 * 분류 탭 — 의상 / 신발, 피드 / 내 크루, 대회 / 러닝·건강.
 *
 * S2 모양: 면을 두르지 않은 글자 탭. 고른 칸은 밝은 글자와 아래 짧은 파란 선,
 * 나머지는 흐린 글자. 칸 폭은 똑같이 나눈다.
 */
@Composable
fun TwoWaySwitch(
    labels: List<String>,
    icons: List<ImageVector?> = labels.map { null },
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            labels.forEachIndexed { index, label ->
                val on = index == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = StepUpDesign.TouchTarget)
                        .clip(RoundedCornerShape(10.dp))
                        .feedbackClickable(role = Role.Tab) { onSelect(index) }
                        .semantics { this.selected = on }
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        icons.getOrNull(index)?.let {
                            Icon(it, contentDescription = null, tint = if (on) Snow else Silver, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f, fill = false),
                            fontSize = 15.sp,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (on) Snow else Silver,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                    S2TabMark(on)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Edge.copy(alpha = 0.6f)))
    }
}

/** 카드 안쪽 여백 — 리뉴얼 화면 공통 */
val RenewalCardPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)

/** 진행 막대 한 줄 — 이름 · 값 · 막대 */
@Composable
fun GoalBar(
    icon: ImageVector,
    title: String,
    value: String,
    fraction: Float,
    modifier: Modifier = Modifier,
    /** 값 뒤에 흐리게 붙는 말 — " / 8,000 걸음" 같은 것 */
    suffix: String = "",
) {
    // 큰 글자에서는 한 줄에 제목과 값이 다 들어가지 않는다. 그때 제목이 "…"로
    // 줄어들면 무엇의 진행인지가 사라진다 — 두 줄로 나눈다.
    val stacked = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(icon, contentDescription = null, tint = VoltText, modifier = Modifier.size(20.dp))
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Snow,
                maxLines = if (stacked) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!stacked) GoalValue(value, suffix)
        }
        if (stacked) GoalValue(value, suffix)
        BarMeter(fraction = fraction, height = 8.dp)
    }
}

/**
 * 작은 캐릭터 액자 — 내 정보 머리.
 *
 * 전신을 Fit 으로 넣는다. 얼굴만 오려 내면 신발이 잘리고, 캐릭터 그림을
 * 자르지 않는다는 원칙이 화면마다 달라진다.
 */
@Composable
fun AvatarBadge(
    look: com.stepup.android.domain.AvatarLook,
    modifier: Modifier = Modifier,
    pose: com.stepup.android.domain.AvatarPose = com.stepup.android.domain.AvatarPose.IDLE,
) {
    val render = com.stepup.android.domain.AvatarArtCatalog.resolve(look, pose)
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                androidx.compose.ui.graphics.Brush.radialGradient(
                    0f to Volt.copy(alpha = 0.30f),
                    1f to CarbonHigh,
                ),
                shape,
            )
            .border(1.dp, Volt.copy(alpha = 0.55f), shape)
            .padding(6.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AvatarImage(art = render.art, modifier = Modifier.matchParentSize())
    }
}

/** 진행 막대의 값 — "12,840" 과 흐린 " / 8,000 걸음" */
@Composable
private fun GoalValue(value: String, suffix: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = value,
            fontFamily = StepUpNumbers,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Snow,
            maxLines = 1,
        )
        if (suffix.isNotEmpty()) {
            Text(text = suffix, fontSize = 13.sp, color = Silver, maxLines = 1)
        }
    }
}
