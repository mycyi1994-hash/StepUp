package com.stepup.android.ui.screens.gacha

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.DrawDistance
import com.stepup.android.domain.DrawKind
import com.stepup.android.domain.DrawStatus
import com.stepup.android.ui.components.RunDivider
import com.stepup.android.ui.components.RunNumber
import com.stepup.android.ui.components.RunPillKind
import com.stepup.android.ui.components.RunStatusPill
import com.stepup.android.ui.components.RunTextAction
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpDesign

/*
 * 신발 뽑기 메인(2026-10-03 파란 톤 통합 전달본 v4 · DRAW01 · 02 · 15 · 18 · 22 · 24 · 26 · 28 · 29 · 32) — 제목 "신발 뽑기"와 한 줄 →
 * 무료 패널 → 상급 패널 → "기회 받는 방법". 예전 무료 / 상급 글자 탭 대신 두 패널을 함께 보인다(각 패널의 상태 · 버튼은
 * 같은 규칙 [drawHomeCard] 를 종류마다 한 번씩 부른 것이다). 공통 머리(로고 · 잔액)와 하단 탭은 앱 셸이 그린다.
 * 상자는 왼쪽, 횟수 · 상태는 오른쪽, 주 버튼은 패널 아래. 작은 화면 · 큰 글씨에서는 넘긴다(버튼이 하단 탭 뒤로 숨지 않는다).
 * 수 · 연결 상태는 서버 값만 — 모르는 수는 "—", 결과를 모르는 요청은 "확인 중"(0 이 아니다). 가격 · 확률 · 나올 신발 목록은 없다.
 */

/**
 * 메인 한 화면. [premiumFocus] 가 늘 때마다 상급 패널을 화면 안으로 옮긴다(무료 소진 안내의 "상급 뽑기 보기" · 지갑 연결 확인 ·
 * 상급 요청의 결과 확인 — 예전에는 상급 탭을 고르던 자리).
 */
@Composable
internal fun DrawHome(
    /** 아래에 비울 자리 — 짧은 알림(DRAW18)이 보이는 동안 */
    bottomInset: Dp,
    status: DrawStatus?,
    loading: Boolean,
    signedOut: Boolean,
    pending: DrawPending?,
    actions: DrawActions,
    onSheet: (DrawSheet) -> Unit,
    premiumFocus: Int = 0,
    /** 블록체인 기능이 있는 판인가 — 스토어 판(false)의 상급 칸은 선물로 받은 상급 뽑기만(지갑 · 러닝 안내 없이, 0112) */
    web3: Boolean = com.stepup.android.BuildConfig.WEB3,
    /** 상자 현황(0116) — 있으면 "상자 받는 곳" 줄 */
    boxes: com.stepup.android.domain.BoxStatus? = null,
) {
    val free = drawHomeCard(status, DrawKind.FREE, pending?.kind, loading, signedOut, web3)
    val premium = drawHomeCard(status, DrawKind.PREMIUM, pending?.kind, loading, signedOut, web3)
    val premiumView = remember { BringIntoViewRequester() }
    LaunchedEffect(premiumFocus) {
        if (premiumFocus > 0) runCatching { premiumView.bringIntoView() }
    }
    val subtitle = when {
        status != null -> stringResource(R.string.draw_blue_subtitle_ready)
        signedOut -> stringResource(R.string.dv3_signed_out_title)
        loading -> stringResource(R.string.draw_blue_subtitle_loading)
        else -> stringResource(R.string.draw_blue_subtitle_failed)
    }
    Box(Modifier.fillMaxSize().padding(bottom = bottomInset).testTag("draw-home")) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("draw-scroll")
                .padding(horizontal = StepUpDesign.Gutter),
        ) {
            Spacer(Modifier.height(4.dp))
            DrawHomeTitle(subtitle)
            Spacer(Modifier.height(14.dp))
            DrawKindPanel(free, status, actions, onSheet, pendingAny = pending != null, web3 = web3)
            // 상급 뽑기는 지갑 연결 혜택이다 — 블록체인 기능이 없는 스토어 판(WEB3 = false)은 선물로 받은 상급 뽑기가 남았을 때만(0112)
            if (premiumPanelShown(web3, status, pending?.kind)) {
                Spacer(Modifier.height(12.dp))
                DrawKindPanel(
                    premium, status, actions, onSheet, pendingAny = pending != null, web3 = web3,
                    modifier = Modifier.bringIntoViewRequester(premiumView),
                )
            }
            if (status != null && boxes != null) {
                // 상자 받는 곳(경제 4판) — 출석 · 러닝 · 주간 걸음 · 연속 · 신발 레벨 · SUP 로 사기. 서버 값 한 줄, 누르면 시트
                Spacer(Modifier.height(12.dp))
                BoxSourcesEntry(boxes, onOpen = { onSheet(DrawSheet.Boxes) }, Modifier.testTag("draw-boxes"))
            }
            if (status != null) {
                // 기회 받는 방법 — 서버 값으로 회원에게 해당하는 규칙만(DRAW05)
                RunTextAction(
                    stringResource(R.string.dv2_rules_title), { onSheet(DrawSheet.Rules) },
                    Modifier.fillMaxWidth().padding(top = 6.dp).testTag("draw-rules"),
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** 제목 "신발 뽑기"(굵게 기울인) · 상태 한 줄 */
@Composable
private fun DrawHomeTitle(subtitle: String) {
    val t = runTone()
    Column(Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.dv2_title), style = drawTitleStyle(34.sp, t.text),
            modifier = Modifier.semantics { heading() }.testTag("draw-title"),
        )
        Text(
            subtitle, style = runTextStyle(15.sp, t.label, FontWeight.Medium),
            modifier = Modifier.padding(top = 4.dp).semantics { liveRegion = LiveRegionMode.Polite }.testTag("draw-subtitle"),
        )
    }
}

/**
 * 한 종류의 패널 — 왼쪽 상자 무대, 오른쪽 이름 · 상태 · 수 · 한 줄 · 기회 내역 링크, 아래 주 버튼 하나.
 * 태그는 종류마다(draw-free · draw-free-left · draw-free-action / draw-premium …).
 */
@Composable
private fun DrawKindPanel(
    card: DrawHomeCard,
    status: DrawStatus?,
    actions: DrawActions,
    onSheet: (DrawSheet) -> Unit,
    pendingAny: Boolean,
    web3: Boolean,
    modifier: Modifier = Modifier,
) {
    val t = runTone()
    val key = if (card.kind == DrawKind.FREE) "free" else "premium"
    DrawPanel(modifier.testTag("draw-$key")) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            DrawBoxStage(card.kind, Modifier.weight(0.5f).height(146.dp), dim = card.dim)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(0.5f)) {
                Text(
                    stringResource(if (card.kind == DrawKind.FREE) R.string.dv2_free else R.string.dv2_premium),
                    style = drawTitleStyle(27.sp, t.text), maxLines = 2,
                    modifier = Modifier.semantics { heading() }.testTag("draw-$key-title"),
                )
                val sub = panelSub(card, status, web3)
                if (sub != null) {
                    Text(sub, style = runTextStyle(14.sp, t.label, FontWeight.SemiBold), modifier = Modifier.padding(top = 2.dp).testTag("draw-$key-sub"))
                }
                PanelValue(card, key, Modifier.padding(top = 8.dp))
                val detail = panelDetail(card, status, web3)
                if (detail != null) {
                    Text(
                        detail, style = runTextStyle(14.sp, t.label, FontWeight.Medium, 1.35f),
                        modifier = Modifier.padding(top = 2.dp).testTag("draw-$key-detail"),
                    )
                }
                if (card.sub == DrawCardSub.Paused) {
                    RunStatusPill(stringResource(R.string.dv3_state_paused), RunPillKind.Paused, Modifier.padding(top = 6.dp).testTag("draw-premium-paused"))
                }
                // 기회 내역 — 서버 값을 아는 동안만. 결과를 모르는 요청이 있거나 지갑을 연결한 적이 없으면 두지 않는다
                val history = status != null && !pendingAny && card.action != DrawCardAction.Connect
                if (history) {
                    RunDivider(Modifier.padding(top = 8.dp))
                    HistoryLink(
                        { onSheet(if (card.kind == DrawKind.FREE) DrawSheet.FreeChances else DrawSheet.PremiumChances) },
                        Modifier.testTag("draw-$key-history"),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        DrawMainButton(card, status, actions, onSheet, Modifier.testTag("draw-$key-action"))
    }
}

/** 이름 아래 상태 한 줄 — 상급의 지갑 상태(연결 전 · 연결 필요) · 선물 칸. 일시 중단은 따로 알약으로 */
@Composable
private fun panelSub(card: DrawHomeCard, status: DrawStatus?, web3: Boolean): String? = when {
    card.kind == DrawKind.PREMIUM && card.action == DrawCardAction.Connect -> stringResource(R.string.draw_blue_before_link)
    card.kind == DrawKind.PREMIUM && status?.premiumMode(web3) == PremiumMode.Reconnect -> stringResource(R.string.dv3_state_wallet_needed)
    // 선물로 받은 상급 뽑기(0112) — 결과를 기다리는 동안은 그 안내가 먼저다
    card.kind == DrawKind.PREMIUM && card.row != DrawRow.Pending && status?.premiumMode(web3) == PremiumMode.Gift ->
        stringResource(R.string.update_reward_panel_sub)
    else -> null
}

/** 작은 이름과 큰 수 — 남은 횟수 · 처음 연결하면 받을 수 · 보유한 상급 기회 · 뽑은 신발 확인 중 · 모르는 수 "—" */
@Composable
private fun PanelValue(card: DrawHomeCard, key: String, modifier: Modifier) {
    val t = runTone()
    val label = when (val row = card.row) {
        is DrawRow.Gift -> stringResource(R.string.draw_blue_on_first_link)
        DrawRow.Pending -> stringResource(R.string.dv2_pending_label)
        is DrawRow.Count -> if (row.label == DrawRowLabel.PremiumKept) stringResource(R.string.dv2_kept_premium) else stringResource(R.string.draw_blue_left_label)
        is DrawRow.Unknown -> stringResource(R.string.draw_blue_left_label)
    }
    val tag = if (card.row is DrawRow.Gift) "draw-premium-gift" else "draw-$key-value"
    // 이름과 수를 한 번에 읽는다("남은 횟수 13회")
    Column(modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.testTag(tag)) {
        Text(label, style = runTextStyle(14.sp, t.label, FontWeight.Medium))
        when (val row = card.row) {
            is DrawRow.Count -> RunNumber(
                stringResource(R.string.dv2_count, row.value), size = 40.sp, color = t.cyan,
                modifier = Modifier.fillMaxWidth(), valueTag = "draw-$key-left",
            )
            is DrawRow.Gift -> RunNumber(
                stringResource(R.string.dv2_count, row.value), size = 40.sp, color = t.cyan, modifier = Modifier.fillMaxWidth(),
            )
            DrawRow.Pending -> RunNumber(
                stringResource(R.string.dv2_pending_value), size = 32.sp, color = t.cyan,
                modifier = Modifier.fillMaxWidth(), valueTag = "draw-pending",
            )
            is DrawRow.Unknown -> DrawUnknownValue("draw-$key-unknown", Modifier.padding(top = 4.dp))
        }
    }
}

/** 수 아래 한 줄 — 무료는 출처별 남은 수, 상급은 다음 1회까지의 거리(서버 값) · 오늘 한도 · 연결 규칙 · 다시 연결 · 결과 대기 */
@Composable
private fun panelDetail(card: DrawHomeCard, status: DrawStatus?, web3: Boolean): String? {
    // 로그인 전 — 위 소개 줄이 이미 "로그인하면 볼 수 있어요."다. 칸마다 또 쓰면 한 화면에 세 번이었다(2026-11-28 디자인 점검)
    if (card.action == DrawCardAction.SignIn) return null
    if (status == null) return null
    if (card.row == DrawRow.Pending) return stringResource(R.string.draw_blue_pending_hint)
    return if (card.kind == DrawKind.FREE) {
        when {
            status.signupLeft > 0 -> stringResource(R.string.draw_blue_free_breakdown, status.signupLeft, status.dailyLeft.coerceAtLeast(0))
            status.dailyTotal > 0 -> stringResource(R.string.dv3_stage_daily_free, status.dailyTotal)
            else -> null
        }
    } else {
        when (status.premiumMode(web3)) {
            // 선물 칸 — 지갑 · 러닝 안내 없이 한 줄
            PremiumMode.Gift -> stringResource(R.string.update_reward_panel_detail)
            PremiumMode.Connect ->
                if (status.runStepMeters > 0) stringResource(R.string.draw_blue_run_per_step, DrawDistance.stepKm(status.runStepMeters)) else null
            PremiumMode.Reconnect -> stringResource(R.string.draw_blue_reconnect_hint)
            else -> when {
                status.runCapReached -> stringResource(R.string.dv2_run_capped)
                status.runStepMeters > 0 -> stringResource(R.string.dv2_next_premium, DrawDistance.remainingKm(status.metersToNextPremium))
                else -> null
            }
        }
    }
}

/** "기회 내역 보기 ›" — 왼쪽 정렬 글자 링크, 누르는 곳 48dp */
@Composable
private fun HistoryLink(onClick: () -> Unit, modifier: Modifier) {
    val t = runTone()
    Row(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp)).feedbackClickable(role = Role.Button, onClick = onClick)
            .padding(end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.draw_blue_history), style = runTextStyle(14.sp, t.label, FontWeight.SemiBold))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = t.label, modifier = Modifier.size(20.dp))
    }
}

/** 패널의 주 버튼 하나 — 그 종류의 행동 */
@Composable
private fun DrawMainButton(card: DrawHomeCard, status: DrawStatus?, actions: DrawActions, onSheet: (DrawSheet) -> Unit, modifier: Modifier) {
    when (card.action) {
        DrawCardAction.Draw -> {
            // 13차 — 남은 수(서버 값)가 둘 이상이면 왼쪽에 "10회 뽑기"(남은 수까지). 하나면 한 번 뽑기만
            val many = status?.shownLeft(card.kind, com.stepup.android.BuildConfig.WEB3)?.coerceAtMost(DrawViewModel.BATCH) ?: 0
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (many >= 2) {
                    val key = if (card.kind == DrawKind.FREE) "free" else "premium"
                    val t = runTone()
                    Box(
                        Modifier.heightIn(min = 56.dp).clip(RoundedCornerShape(50)).background(t.panel)
                            .border(1.dp, t.panelEdge, RoundedCornerShape(50))
                            .feedbackClickable(role = Role.Button) { actions.onDrawMany(card.kind, many) }
                            .padding(horizontal = 16.dp).testTag("draw-$key-many"),
                        contentAlignment = Alignment.Center,
                    ) { Text(stringResource(R.string.v13_draw_n, many), style = runTextStyle(15.sp, t.cyan, FontWeight.Bold), maxLines = 1) }
                }
                DrawButton(
                    stringResource(if (card.kind == DrawKind.FREE) R.string.dv2_action_free else R.string.dv2_action_premium),
                    { actions.onDraw(card.kind) }, modifier.weight(1f), icon = Icons.Filled.PlayArrow,
                )
            }
        }
        DrawCardAction.FreeInfo -> DrawButton(stringResource(R.string.dv2_action_free_info), { onSheet(DrawSheet.FreeEmpty) }, modifier)
        DrawCardAction.RunInfo -> DrawButton(stringResource(R.string.dv2_action_run), { onSheet(DrawSheet.RunChances) }, modifier)
        DrawCardAction.Connect -> DrawButton(
            stringResource(R.string.dv2_action_connect, status?.giftOnLink ?: 0), { onSheet(DrawSheet.WalletBenefit) }, modifier,
        )
        // DRAW24 — 선물은 다시 주지 않는다. 연결은 기존 웹 지갑 페이지에서
        DrawCardAction.Reconnect -> DrawButton(stringResource(R.string.dv2_action_reconnect), actions.onConnectWallet, modifier)
        DrawCardAction.Paused -> DrawButton(stringResource(R.string.dv2_action_paused), {}, modifier, DrawButtonStyle.Off)
        DrawCardAction.Spent -> DrawButton(stringResource(R.string.update_reward_spent), {}, modifier, DrawButtonStyle.Off)
        DrawCardAction.Check -> DrawButton(stringResource(R.string.dv2_action_check), actions.onCheckPending, modifier)
        DrawCardAction.Loading -> DrawButton(stringResource(R.string.dv2_action_loading), {}, modifier, DrawButtonStyle.Off)
        DrawCardAction.Reload -> DrawButton(stringResource(R.string.dv2_action_reload), actions.onRetry, modifier)
        // DRAW28 — 로그인 화면으로(로그인할 수 없는 빌드면 누를 수 없다)
        DrawCardAction.SignIn -> {
            val signIn = actions.onSignIn
            if (signIn != null) DrawButton(stringResource(R.string.dv3_action_sign_in), signIn, modifier)
            else DrawButton(stringResource(R.string.dv2_action_sign_in), {}, modifier, DrawButtonStyle.Off)
        }
    }
}

/** DRAW18 러닝 반영 · 다시 연결 — 서버 값이 바뀐 것을 확인했을 때만 잠깐. 하단 탭 위 12dp, 버튼을 가리지 않는다 */
@Composable
internal fun DrawToast(text: String?, modifier: Modifier = Modifier) {
    if (text == null) return
    val t = runTone()
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp).clip(shape).background(t.panel).border(1.dp, t.panelEdge, shape)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(22.dp).clip(RoundedCornerShape(11.dp)).background(t.cyan.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = t.cyan, modifier = Modifier.size(14.dp))
        }
        Text(text, style = runTextStyle(15.sp, t.text, FontWeight.SemiBold))
    }
}
