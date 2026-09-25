package com.stepup.android.ui.components

import com.stepup.android.ui.theme.StepUpDesign
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.widthIn

import androidx.compose.foundation.background
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
 * 보유 SUP — 머리글 오른쪽의 작은 알약.
 *
 * 누르면 지갑이 열린다. 숫자를 크게 반복하지 않는다 — 보유 포인트는
 * 여기 한 군데에서 작게 보이면 충분하다.
 */
@Composable
fun SupPill(balance: Double?, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    val amount = balance?.let { "%,.0f".format(it) } ?: "—"
    val label = stringResource(R.string.cd_sup_balance, amount)
    Row(
        modifier = modifier
            .clip(shape)
            .background(CarbonHigh, shape)
            .border(1.dp, Volt.copy(alpha = 0.45f), shape)
            .then(if (onClick != null) Modifier.feedbackClickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = label }
            .heightIn(min = StepUpDesign.BalanceHeight)
            .widthIn(max = 160.dp)
            .testTag("sup-balance")
            .padding(start = 8.dp, end = if (onClick != null) 6.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HexEmblem(size = 20.dp, glow = false)
        Text(
            text = amount,
            modifier = Modifier.weight(1f, fill = false),
            overflow = TextOverflow.Ellipsis,
            fontFamily = StepUpNumbers,
            fontSize = StepUpDesign.BalanceAmount,
            fontWeight = FontWeight.Bold,
            color = Snow,
            maxLines = 1,
        )
        Text(text = "SUP", fontSize = StepUpDesign.BalanceUnit, fontWeight = FontWeight.Bold, color = Silver)
        if (onClick != null) {
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Silver, modifier = Modifier.size(16.dp))
        }
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

/**
 * 탭 첫 화면의 머리글 — 왼쪽 로고, 오른쪽 보유 SUP. 시안의 네 탭이 모두 이 모양이다.
 */
@Composable
fun MainHeader(
    balance: Double?,
    onOpenWallet: (() -> Unit)?,
    modifier: Modifier = Modifier,
    balanceModifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = StepUpDesign.HeaderHeight)
            .testTag("main-header"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Wordmark(modifier = Modifier.weight(1f, fill = false))
        Spacer(Modifier.weight(1f))
        SupPill(balance = balance, onClick = onOpenWallet, modifier = balanceModifier)
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
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = com.stepup.android.ui.theme.StepUpDesign.HeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DarkIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back), onClick = onBack,
            cue = FeedbackCue.Back)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (title == null) Wordmark() else Text(title, color = Snow, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
        // Wallet-enabled headers keep the same slot while the balance is loading.
        if (balance != null || onOpenWallet != null) SupPill(balance, onOpenWallet)
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
 * 한 화면에서 가장 먼저 눌러야 하는 버튼 — 큰 파란 알약.
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
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = StepUpDesign.PrimaryHeight)
            // 화면의 주 행동 하나에만 푸른 번짐을 준다
            .then(
                if (enabled) {
                    Modifier.shadow(elevation = 14.dp, shape = shape, ambientColor = Volt, spotColor = Volt)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .then(
                if (enabled) Modifier.background(VoltPlate, shape).sheen(alpha = 0.08f)
                else Modifier.background(CarbonHigh, shape).border(1.dp, Edge, shape),
            )
            .feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (enabled) OnVolt else Slate, modifier = Modifier.size(StepUpDesign.ControlIcon))
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = text,
            modifier = Modifier.weight(1f, fill = false),
            fontSize = StepUpDesign.PrimaryLabel,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) OnVolt else Slate,
            textAlign = TextAlign.Center,
        )
        if (showArrow && enabled) {
            Spacer(Modifier.width(12.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = OnVolt,
                modifier = Modifier.size(20.dp),
            )
        }
    }
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

/** 두 칸짜리 분류 탭 — 의상 / 신발, 피드 / 내 크루, 대회 / 러닝·건강 */
@Composable
fun TwoWaySwitch(
    labels: List<String>,
    icons: List<ImageVector?> = labels.map { null },
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CarbonHigh, shape)
            .border(1.dp, Edge, shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val on = index == selected
            val itemShape = RoundedCornerShape(12.dp)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = StepUpDesign.TouchTarget)
                    .clip(itemShape)
                    .then(if (on) Modifier.background(VoltPlate, itemShape) else Modifier)
                    .feedbackClickable(role = Role.Tab) { onSelect(index) }
                    .semantics { this.selected = on }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                icons.getOrNull(index)?.let {
                    Icon(it, contentDescription = null, tint = if (on) OnVolt else Silver, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = label,
                    modifier = Modifier.weight(1f, fill = false),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (on) OnVolt else Silver,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
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
