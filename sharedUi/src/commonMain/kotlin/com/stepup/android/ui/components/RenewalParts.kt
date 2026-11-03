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
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.ui.res.*
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
import com.stepup.android.ui.theme.Volt
import com.stepup.android.ui.theme.VoltPlate
import com.stepup.android.ui.theme.VoltText

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
        DarkIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.cd_back), onClick = onBack,
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
 * 보유 SUP 알약 — 통합안: 회색 채움 알약(높이 36) · 왼쪽 작은 동전 · "0 SUP"(14 · 600). 누르면 지갑.
 * 내림 — 499.6 을 500 으로 보이면 500 SUP 짜리를 살 수 있는 것처럼 보인다(서버는 소수 4자리).
 */
@Composable
fun SupPill(balance: Double?, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val ui = StepUpUi.colors
    val amount = balance?.let { formatSupDown(it, 2) } ?: "—"
    val label = stringResource(Res.string.cd_sup_balance, amount)
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
fun SupCoin(modifier: Modifier = Modifier) {
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
