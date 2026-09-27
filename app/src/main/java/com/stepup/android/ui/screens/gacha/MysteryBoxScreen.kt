package com.stepup.android.ui.screens.gacha

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.DrawDistance
import com.stepup.android.domain.DrawKind
import com.stepup.android.domain.DrawStatus
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.components.FactionChip
import com.stepup.android.ui.components.GhostButton
import com.stepup.android.ui.components.RarityChip
import com.stepup.android.ui.components.S2ShoesSections
import com.stepup.android.ui.components.SneakerFrame
import com.stepup.android.ui.components.VoltButton
import com.stepup.android.ui.components.celebrate
import com.stepup.android.ui.components.fullLabel
import com.stepup.android.ui.components.reveal
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.LocalFeedback
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.Carbon
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.StepUpDesign
import com.stepup.android.ui.theme.VoltText

/**
 * 신발 뽑기 — 무료 정책(2026-09-27, docs/redesign/shoe-draw). 신발 탭 안의 "뽑기" 칸이다.
 *
 * 위에서부터 무료 · 상급 두 칸 → 한 줄 제목 → StepUp 신발 상자와 받침(시안 그림 그대로) →
 * 남은 수와 출처(오늘 무료 · 가입 선물 / 지갑 선물 · 러닝으로 받은 기회) → 아래에 주 버튼 하나.
 * 가격 · SUP 잔액 · 결제 확인창은 없다 — 모든 뽑기가 무료다. 수는 모두 서버가 준 것만 보인다.
 */
@Composable
fun MysteryBoxScreen(
    state: DrawScreenState,
    tab: DrawKind,
    drawing: Boolean = false,
    onTab: (DrawKind) -> Unit = {},
    onDraw: () -> Unit = {},
    onConnectWallet: () -> Unit = {},
    onRetry: () -> Unit = {},
    /** 신발 탭 안의 "내 신발"로 돌아간다 */
    onOpenShoes: () -> Unit = {},
) {
    val feedback = LocalFeedback.current
    LaunchedEffect(feedback) { feedback?.play(FeedbackCue.DrawEnter) }
    val palette = drawPalette()
    val status = (state as? DrawScreenState.Ready)?.status
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val largeText = LocalDensity.current.fontScale > 1.2f

    Column(
        Modifier.fillMaxSize().padding(horizontal = StepUpDesign.Gutter).padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        S2ShoesSections(drawSelected = true, onShoes = onOpenShoes, onDraw = {})
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            DrawTabs(tab, onTab, palette)
            Spacer(Modifier.height(16.dp))
            Headline(tab, status, palette)
            Spacer(Modifier.height(4.dp))
            // 작은 화면 · 큰 글씨에서는 상자를 줄여 아래 수와 버튼 자리를 먼저 둔다
            val stageWidth = when {
                screenHeight < 700 || largeText -> 0.62f
                screenHeight < 800 -> 0.74f
                else -> 0.89f
            }
            Image(
                painter = painterResource(R.drawable.draw_shoebox_stage),
                contentDescription = stringResource(R.string.draw_box_description),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(stageWidth).aspectRatio(1170f / 1028f).testTag("draw-stage"),
            )
            Spacer(Modifier.height(12.dp))
            when {
                tab == DrawKind.FREE -> FreeDetails(state, status, palette, onOpenPremium = { onTab(DrawKind.PREMIUM) }, onRetry = onRetry)
                status?.walletLinked == true -> PremiumDetails(status, palette)
                else -> PremiumConnect(state, status, palette, onRetry = onRetry)
            }
            Spacer(Modifier.height(16.dp))
        }
        val action = drawAction(state, status, tab)
        DrawActionButton(
            label = stringResource(action.label),
            enabled = action.enabled && !drawing,
            loading = drawing,
            onClick = if (action.connect) onConnectWallet else onDraw,
            palette = palette,
            modifier = Modifier.padding(horizontal = 4.dp).testTag(if (action.connect) "draw-connect-wallet" else "draw-shoe"),
        )
        Text(
            captionFor(state, status, tab),
            color = palette.secondary, fontSize = 13.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp).testTag("draw-caption"),
        )
    }
}

// ── 위 두 칸 ─────────────────────────────────────────────────────

@Composable
private fun DrawTabs(tab: DrawKind, onTab: (DrawKind) -> Unit, palette: DrawPalette) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(palette.track).padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DrawKind.entries.forEach { kind ->
            val selected = kind == tab
            Box(
                Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (selected) palette.selected else Color.Transparent)
                    .feedbackClickable(role = Role.Tab, onClick = { onTab(kind) })
                    .semantics { this.selected = selected; role = Role.Tab }
                    .testTag(if (kind == DrawKind.FREE) "draw-tab-free" else "draw-tab-premium"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(if (kind == DrawKind.FREE) R.string.draw_tab_free else R.string.draw_tab_premium),
                    color = if (selected) palette.primary else palette.secondary,
                    fontSize = 16.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 칸마다 한 줄 제목 — 무료는 정책 한 줄, 상급은 지갑 상태 */
@Composable
private fun Headline(tab: DrawKind, status: DrawStatus?, palette: DrawPalette) {
    val modifier = Modifier.fillMaxWidth().testTag("draw-headline")
    when {
        tab == DrawKind.FREE -> Text(
            if (status != null && status.signupGranted == 0) stringResource(R.string.draw_free_headline_daily, status.dailyTotal)
            else stringResource(R.string.draw_free_headline, status?.signupGranted ?: 10, status?.dailyTotal ?: 3),
            color = palette.primary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            modifier = modifier,
        )
        status?.walletLinked == true -> Row(modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.draw_wallet_linked), color = palette.accent, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
        else -> Text(
            stringResource(R.string.draw_premium_headline_connect),
            color = palette.primary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            modifier = modifier,
        )
    }
}

// ── 무료 칸 ──────────────────────────────────────────────────────

@Composable
private fun ColumnScope.FreeDetails(
    state: DrawScreenState,
    status: DrawStatus?,
    palette: DrawPalette,
    onOpenPremium: () -> Unit,
    onRetry: () -> Unit,
) {
    CountRow(stringResource(R.string.draw_free_left), status?.freeLeft, palette, valueTag = "draw-free-left")
    Divider(palette)
    // 가입 선물이 없던 예전 계정에는 그 줄을 두지 않는다
    if (status == null || status.signupGranted > 0) {
        DetailRow(stringResource(R.string.draw_signup_gift), status?.signupLeft?.let { count(it) }, palette)
    }
    DetailRow(stringResource(R.string.draw_daily_chance), status?.dailyLeft?.let { count(it) }, palette)
    if (state is DrawScreenState.Failed) FailedLine(palette, onRetry)
    Spacer(Modifier.height(18.dp))
    val step = DrawDistance.stepKm(status?.runStepMeters ?: 1000)
    val body = when {
        status?.walletLinked == true -> stringResource(R.string.draw_premium_benefit_linked, status.premiumLeft, step)
        status != null && status.giftOnLink > 0 -> stringResource(R.string.draw_wallet_benefit_body, status.giftOnLink, step)
        else -> stringResource(R.string.draw_wallet_benefit_run_only, step)
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(palette.card)
            .feedbackClickable(role = Role.Button, onClick = onOpenPremium)
            .padding(horizontal = 18.dp, vertical = 16.dp).testTag("draw-wallet-benefit"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                stringResource(if (status?.walletLinked == true) R.string.draw_premium_title else R.string.draw_wallet_benefit_title),
                color = palette.primary, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold,
            )
            Text(body, color = palette.accent, fontSize = 14.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = palette.secondary, modifier = Modifier.size(22.dp))
    }
}

// ── 상급 칸 — 지갑 연결 전 ─────────────────────────────────────────

@Composable
private fun ColumnScope.PremiumConnect(state: DrawScreenState, status: DrawStatus?, palette: DrawPalette, onRetry: () -> Unit) {
    Text(
        stringResource(R.string.draw_premium_free_title), color = palette.primary, fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(6.dp))
    Text(stringResource(R.string.draw_premium_free_body), color = palette.secondary, fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(10.dp))
    val gift = status?.giftOnLink
    if (gift == null || gift > 0) {
        DetailRow(stringResource(R.string.draw_premium_on_link), gift?.let { count(it) }, palette)
    }
    DetailRow(
        stringResource(R.string.draw_premium_per_km, DrawDistance.stepKm(status?.runStepMeters ?: 1000)),
        stringResource(R.string.draw_plus_one), palette,
    )
    Divider(palette)
    Text(stringResource(R.string.draw_premium_connect_hint), color = palette.secondary, fontSize = 14.sp,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
    if (state is DrawScreenState.Failed) FailedLine(palette, onRetry)
}

// ── 상급 칸 — 지갑 연결 뒤 ─────────────────────────────────────────

@Composable
private fun ColumnScope.PremiumDetails(status: DrawStatus, palette: DrawPalette) {
    CountRow(stringResource(R.string.draw_premium_left), status.premiumLeft, palette, valueTag = "draw-premium-left")
    Divider(palette)
    DetailRow(stringResource(R.string.draw_link_gift), count(status.giftLeft), palette)
    DetailRow(stringResource(R.string.draw_run_earned), count(status.runLeft), palette)
    Spacer(Modifier.height(16.dp))
    val step = DrawDistance.stepKm(status.runStepMeters)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(palette.card)
            .padding(horizontal = 18.dp, vertical = 16.dp).testTag("draw-run-progress"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.draw_next_premium), color = palette.secondary, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(
                stringResource(R.string.draw_km, DrawDistance.remainingKm(status.metersToNextPremium)),
                color = palette.primary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
            )
        }
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(palette.track)) {
            Box(
                Modifier.fillMaxWidth(status.progressFraction).fillMaxHeight().clip(RoundedCornerShape(3.dp))
                    .background(Brush.horizontalGradient(listOf(palette.barFrom, palette.accent))),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.draw_progress, DrawDistance.progressKm(status.runProgressMeters), step),
                color = palette.secondary, fontSize = 13.sp, modifier = Modifier.weight(1f),
            )
            Text(stringResource(R.string.draw_every_km, step), color = palette.accent, fontSize = 13.sp)
        }
        if (status.runCapReached) {
            Text(stringResource(R.string.draw_run_cap_reached), color = palette.secondary, fontSize = 13.sp)
        }
    }
    if (status.chainPaused) {
        Text(stringResource(R.string.draw_chain_paused), color = palette.secondary, fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
    }
}

// ── 줄 · 부품 ────────────────────────────────────────────────────

/** 남은 수 — 큰 숫자. 읽기 전에는 "—"(0회로 보이지 않는다) */
@Composable
private fun CountRow(label: String, value: Int?, palette: DrawPalette, valueTag: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = palette.primary, fontSize = 17.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Text(
            value?.let { count(it) } ?: "—",
            color = palette.primary, fontSize = 38.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.8).sp,
            maxLines = 1, modifier = Modifier.testTag(valueTag),
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String?, palette: DrawPalette) {
    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = palette.secondary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(value ?: "—", color = palette.primary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun Divider(palette: DrawPalette) {
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp).background(palette.divider))
}

@Composable
private fun FailedLine(palette: DrawPalette, onRetry: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.draw_status_failed), color = palette.secondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Box(
            Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(10.dp))
                .feedbackClickable(role = Role.Button, onClick = onRetry).padding(horizontal = 8.dp)
                .testTag("draw-retry"),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.draw_retry), color = palette.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun count(value: Int): String = stringResource(R.string.draw_count, value)

/** 아래 주 버튼 — 56dp, 모서리 18(시안). 누른 뒤 결과가 올 때까지 돌고 눌리지 않는다 */
@Composable
private fun DrawActionButton(
    label: String,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    palette: DrawPalette,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    val face = if (enabled || loading) palette.buttonFace else SolidColor(palette.buttonDisabled)
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape).background(face)
            .feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), color = palette.buttonText, strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
        }
        Text(
            label, color = if (enabled || loading) palette.buttonText else palette.buttonDisabledText,
            fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
        )
    }
}

/** 방금 뽑은 신발 — 상자 열기 연출(나타나기 · 반짝임)과 결과. 신기지 않는다, 내 신발에서 고른다 */
@Composable
fun DrawResultDialog(sneaker: Sneaker, onOpenShoes: () -> Unit, onClose: () -> Unit) {
    com.stepup.android.ui.components.DialogPanel(
        title = stringResource(R.string.draw_result_title),
        onDismiss = onClose,
        actions = {
            VoltButton(stringResource(R.string.draw_result_open_shoes), onOpenShoes,
                Modifier.fillMaxWidth().testTag("draw-result-shoes"))
            GhostButton(stringResource(R.string.common_close), onClose, Modifier.fillMaxWidth().testTag("draw-result-close"))
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().reveal(sneaker.id).celebrate(sneaker.id).testTag("draw-result"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FactionChip(sneaker.faction)
                RarityChip(sneaker.rarity)
            }
            SneakerFrame(sneaker = sneaker, modifier = Modifier.fillMaxWidth().height(180.dp), corner = 16.dp, animate = true)
            Text(sneaker.fullLabel(), style = MaterialTheme.typography.titleLarge, color = Snow, textAlign = TextAlign.Center)
            Text(stringResource(R.string.sneaker_mint_no, sneaker.mintNumber), fontSize = 14.sp, color = Silver)
        }
    }
}

// ── 상태 → 버튼 · 안내 ────────────────────────────────────────────

private data class DrawAction(val label: Int, val enabled: Boolean, val connect: Boolean = false)

private fun drawAction(state: DrawScreenState, status: DrawStatus?, tab: DrawKind): DrawAction = when {
    state is DrawScreenState.SignedOut -> DrawAction(R.string.draw_action_sign_in, enabled = false)
    tab == DrawKind.FREE -> when {
        status == null -> DrawAction(R.string.draw_action_free, enabled = false)
        status.freeLeft > 0 -> DrawAction(R.string.draw_action_free, enabled = true)
        else -> DrawAction(R.string.draw_action_free_empty, enabled = false)
    }
    status == null -> DrawAction(R.string.draw_action_premium, enabled = false)
    !status.walletLinked -> DrawAction(R.string.draw_action_connect, enabled = true, connect = true)
    status.chainPaused -> DrawAction(R.string.draw_action_paused, enabled = false)
    status.premiumLeft > 0 -> DrawAction(R.string.draw_action_premium, enabled = true)
    else -> DrawAction(R.string.draw_action_premium_empty, enabled = false)
}

@Composable
private fun captionFor(state: DrawScreenState, status: DrawStatus?, tab: DrawKind): String = when {
    state is DrawScreenState.SignedOut -> stringResource(R.string.draw_caption_sign_in)
    tab == DrawKind.FREE && status != null && status.freeLeft == 0 ->
        stringResource(R.string.draw_caption_free_tomorrow, status.dailyTotal)
    tab == DrawKind.PREMIUM && status?.walletLinked != true -> stringResource(R.string.draw_caption_no_wallet)
    tab == DrawKind.PREMIUM && status != null && status.premiumLeft == 0 ->
        stringResource(R.string.draw_caption_premium_run, DrawDistance.stepKm(status.runStepMeters))
    else -> stringResource(R.string.draw_caption_added)
}

// ── 색 ─────────────────────────────────────────────────────────

/** 어두운 테마는 시안 값(#050912 바탕 위), 밝은 테마는 앱 토큰(흰 주 버튼 → 남색 면에 흰 글자) */
private class DrawPalette(
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val track: Color,
    val selected: Color,
    val card: Color,
    val divider: Color,
    val barFrom: Color,
    val buttonFace: Brush,
    val buttonText: Color,
    val buttonDisabled: Color,
    val buttonDisabledText: Color,
)

@Composable
private fun drawPalette(): DrawPalette = if (StepUpColors.dark) {
    DrawPalette(
        primary = Color(0xFFF2F4FC), secondary = Color(0xFF98A8C0), accent = Color(0xFFA3BFFE),
        track = Color(0xFF0E1727), selected = Color(0xFF22324A), card = Color(0xFF0F1A2B),
        divider = Color(0xFF1C2638), barFrom = Color(0xFF5F86E8),
        buttonFace = Brush.verticalGradient(listOf(Color(0xFFF7F8FF), Color(0xFFE8EDFA))),
        buttonText = Color(0xFF0B1220), buttonDisabled = Color(0xFF1A2335), buttonDisabledText = Color(0xFF7D8BA2),
    )
} else {
    DrawPalette(
        primary = Snow, secondary = Silver, accent = VoltText,
        track = CarbonHigh, selected = Carbon, card = CarbonHigh,
        divider = Edge, barFrom = VoltText.copy(alpha = 0.55f),
        buttonFace = SolidColor(Snow), buttonText = Color.White,
        buttonDisabled = CarbonHigh, buttonDisabledText = Silver,
    )
}
