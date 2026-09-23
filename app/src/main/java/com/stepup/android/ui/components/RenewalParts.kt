package com.stepup.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.stepup.android.ui.theme.CarbonHigh
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
fun SupPill(balance: Double, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    val label = stringResource(R.string.cd_sup_balance, "%,.0f".format(balance))
    Row(
        modifier = modifier
            .clip(shape)
            .background(CarbonHigh, shape)
            .border(1.dp, Volt.copy(alpha = 0.45f), shape)
            .then(if (onClick != null) Modifier.feedbackClickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = label }
            .heightIn(min = 40.dp)
            .padding(start = 8.dp, end = if (onClick != null) 6.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HexEmblem(size = 20.dp, glow = false)
        Text(
            text = "%,.0f".format(balance),
            fontFamily = StepUpNumbers,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Snow,
            maxLines = 1,
        )
        Text(text = "SUP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Silver)
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
            .heightIn(min = 60.dp)
            .clip(shape)
            .then(
                if (enabled) Modifier.background(VoltPlate, shape).sheen(alpha = 0.18f)
                else Modifier.background(CarbonHigh, shape).border(1.dp, Edge, shape),
            )
            .feedbackClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (enabled) OnVolt else Slate, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = text,
            modifier = Modifier.weight(1f, fill = false),
            fontSize = 19.sp,
            fontWeight = FontWeight.Black,
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
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(CarbonHigh, shape)
            .border(1.dp, Edge, shape)
            .feedbackClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = VoltText, modifier = Modifier.size(20.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Snow,
            maxLines = 2,
        )
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Silver, modifier = Modifier.size(18.dp))
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
        BadgeTone.Glow -> Cyan
        BadgeTone.Muted -> Silver
        BadgeTone.Nft -> NftPurple
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            // 신발·옷 그림 위에 얹힐 때가 있다. 반투명만 두면 흰 모자 위에서
            // 글자가 사라진다 — 불투명한 바닥을 먼저 깔고 그 위에 색을 얹는다.
            .background(CarbonHigh)
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
    }
}

enum class BadgeTone { Accent, Glow, Muted, Nft }

/** NFT 표시 — 보라. 블루(누르는 것)·시안(빛나는 것)과 겹치지 않는 색이라야 "소유물"로 읽힌다. */
private val NftPurple = androidx.compose.ui.graphics.Color(0xFF9B6BFF)

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
                    .heightIn(min = 44.dp)
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
                    fontSize = 15.sp,
                    fontWeight = if (on) FontWeight.Black else FontWeight.SemiBold,
                    color = if (on) OnVolt else Silver,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
 * 원형 아바타 틀 — 내 정보 · 커뮤니티 머리의 캐릭터.
 *
 * 전신을 작게 넣으면 얼굴이 점만 해진다. 캐릭터를 원보다 크게 그리고 아래로
 * 내려, 원 안에는 머리와 어깨만 보이게 한다.
 */
@Composable
fun AvatarBadge(
    look: com.stepup.android.domain.AvatarLook,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .clip(CircleShape)
            .background(CarbonHigh, CircleShape)
            .border(2.dp, Volt.copy(alpha = 0.7f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        val d = maxWidth
        RunnerAvatar(
            look = look,
            modifier = Modifier
                .requiredSize(d * 1.7f, d * 1.7f * 1.42f)
                .offset(y = d * 0.49f),
            showGround = false,
            animate = false,
        )
    }
}
