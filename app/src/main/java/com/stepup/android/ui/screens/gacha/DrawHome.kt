package com.stepup.android.ui.screens.gacha

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.stepup.android.ui.components.KoreanLineBreak
import com.stepup.android.ui.components.cachedPainterResource
import com.stepup.android.ui.components.label
import com.stepup.android.ui.components.runNumberStyle
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.DrawDistance
import com.stepup.android.domain.DrawKind
import com.stepup.android.domain.DrawStatus
import com.stepup.android.ui.components.RunTextAction
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpDesign

/*
 * 신발 뽑기 메인(2026-11-24 재디자인 08_뽑기 — 예전 v4 의 두 패널을 대신한다). 위 글자 탭 「무료 뽑기 · 상급 뽑기」(숫자 없이) →
 * 고른 종류의 흰 카드 하나(가운데: 남은 수 · 상자 그림 · 출처 한 줄 · 기회 내역) → 카드 밑 확률 한 줄 + 「기회 받는 방법」 →
 * 상자 받는 곳 → 아래에 그 종류의 주 버튼 하나. 공통 머리(로고 · 잔액)와 하단 탭은 앱 셸이 그린다.
 * 상급 탭은 [premiumPanelShown] 일 때만(스토어 판은 선물로 받은 상급 뽑기가 남았거나 상급 요청 결과를 기다릴 때만 — 0112).
 * 수 · 연결 상태는 서버 값만 — 모르는 수는 "—", 결과를 모르는 요청은 "확인 중"(0 이 아니다). 확률은 서버 표(0077)를 읽었을 때만.
 */

/**
 * 메인 한 화면. [premiumFocus] 가 늘 때마다 상급 탭을 고른다(무료 소진 안내의 "상급 뽑기 보기" · 지갑 연결 확인 ·
 * 상급 요청의 결과 확인).
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
    /** 확률 표를 이미 알면(캡처 검사) — 없으면 서버 표(0077)를 읽는다 */
    knownOdds: com.stepup.android.domain.DrawOdds? = null,
) {
    val t = runTone()
    // 상급 뽑기는 지갑 연결 혜택이다 — 블록체인 기능이 없는 스토어 판(WEB3 = false)은 선물로 받은 상급 뽑기가 남았을 때만(0112)
    val premiumShown = premiumPanelShown(web3, status, pending?.kind)
    var picked by rememberSaveable { mutableStateOf(DrawKind.FREE) }
    LaunchedEffect(premiumFocus) {
        if (premiumFocus > 0) picked = DrawKind.PREMIUM
    }
    val kind = if (premiumShown) picked else DrawKind.FREE
    val card = drawHomeCard(status, kind, pending?.kind, loading, signedOut, web3)
    val key = if (kind == DrawKind.FREE) "free" else "premium"
    val subtitle = when {
        status != null -> stringResource(R.string.draw_blue_subtitle_ready)
        signedOut -> stringResource(R.string.dv3_signed_out_title)
        loading -> stringResource(R.string.draw_blue_subtitle_loading)
        else -> stringResource(R.string.draw_blue_subtitle_failed)
    }
    // 등급별 확률 — 뽑기가 쓰는 서버 표 그대로(0077, 기회 받는 방법 창과 같은 값). 읽지 못하면 줄을 두지 않는다
    val odds by produceState(knownOdds, status != null) {
        if (status != null && value == null) {
            value = runCatching {
                (com.stepup.android.core.ServiceLocator.economyApi.drawOdds() as? com.stepup.android.data.remote.ServerResult.Ok)?.value
            }.getOrNull()
        }
    }
    Column(Modifier.fillMaxSize().padding(bottom = bottomInset).testTag("draw-home")) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).testTag("draw-scroll")
                .padding(horizontal = StepUpDesign.Gutter),
        ) {
            Spacer(Modifier.height(6.dp))
            DrawKindTabs(kind, premiumShown, onPick = { picked = it })
            Spacer(Modifier.height(14.dp))
            DrawKindPanel(card, status, onSheet, pendingAny = pending != null, web3 = web3, modifier = Modifier.testTag("draw-$key"))
            if (status != null) {
                // 카드 밑 한 줄 — 왼쪽 확률(그 종류의 표), 오른쪽 「기회 받는 방법」(DRAW05)
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    val line = odds?.let { o -> if (kind == DrawKind.FREE) o.free else o.premium }?.takeIf { it.isNotEmpty() }
                    if (line != null) {
                        Text(
                            line.map { (rarity, permille) -> rarity.label() + " " + com.stepup.android.domain.DrawOdds.percent(permille) + "%" }
                                .joinToString(" · "),
                            // 큰 글씨 · 좁은 폭에서는 두 줄로 — 등급을 말줄임으로 잃지 않는다
                            style = runTextStyle(13.5.sp, t.label, FontWeight.Medium, 1.3f).copy(lineBreak = KoreanLineBreak),
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(start = 4.dp).testTag("draw-odds-line"),
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    RunTextAction(
                        stringResource(R.string.dv2_rules_title), { onSheet(DrawSheet.Rules) },
                        Modifier.testTag("draw-rules"),
                    )
                }
            }
            if (status != null && boxes != null) {
                // 상자 받는 곳(경제 4판) — 출석 · 러닝 · 주간 걸음 · 연속 · 신발 레벨 · SUP 로 사기. 서버 값 한 줄, 누르면 시트
                Spacer(Modifier.height(8.dp))
                BoxSourcesEntry(boxes, onOpen = { onSheet(DrawSheet.Boxes) }, Modifier.testTag("draw-boxes"))
            }
            // 상태 한 줄(모두 무료 · 불러오는 중 · 실패 · 로그인 전) — 예전 제목 밑 줄을 여기로
            Text(
                subtitle, style = runTextStyle(13.5.sp, t.muted, FontWeight.Medium, 1.35f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp).semantics { liveRegion = LiveRegionMode.Polite }.testTag("draw-subtitle"),
            )
            Spacer(Modifier.height(16.dp))
        }
        // 주 버튼 하나 — 고른 종류의 행동. 스크롤 밖 아래에 두어 하단 탭 뒤로 숨지 않는다
        DrawMainButton(
            card, status, actions, onSheet,
            Modifier.padding(start = StepUpDesign.Gutter, end = StepUpDesign.Gutter, top = 8.dp, bottom = 12.dp).testTag("draw-$key-action"),
        )
    }
}

/** 위 글자 탭 「무료 뽑기 · 상급 뽑기」 — 숫자 없이, 고른 탭만 굵고 밑줄. 상급 칸이 없으면 무료 하나만 */
@Composable
private fun DrawKindTabs(selected: DrawKind, premiumShown: Boolean, onPick: (DrawKind) -> Unit) {
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        DrawKindTab(stringResource(R.string.dv2_free), selected == DrawKind.FREE, { onPick(DrawKind.FREE) }, "draw-tab-free")
        if (premiumShown) {
            DrawKindTab(stringResource(R.string.dv2_premium), selected == DrawKind.PREMIUM, { onPick(DrawKind.PREMIUM) }, "draw-tab-premium")
        }
    }
}

@Composable
private fun DrawKindTab(label: String, selected: Boolean, onClick: () -> Unit, tag: String) {
    val t = runTone()
    Column(
        Modifier.width(IntrinsicSize.Max).heightIn(min = 44.dp)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .testTag(tag),
    ) {
        Text(
            label, style = runTextStyle(17.sp, if (selected) t.text else t.label, FontWeight.Bold, 1.3f),
            maxLines = 1, softWrap = false, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
        )
        Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(if (selected) t.text else Color.Transparent))
    }
}

/**
 * 고른 종류의 카드 — 흰 카드(어두운 테마 panel) 가운데 정렬: 이름 아래 상태 · 남은 수 · 상자 그림 · 출처 한 줄 · 기회 내역 링크.
 * 태그는 종류마다(draw-free · draw-free-left · draw-free-detail / draw-premium …).
 */
@Composable
private fun DrawKindPanel(
    card: DrawHomeCard,
    status: DrawStatus?,
    onSheet: (DrawSheet) -> Unit,
    pendingAny: Boolean,
    web3: Boolean,
    modifier: Modifier = Modifier,
) {
    val t = runTone()
    val key = if (card.kind == DrawKind.FREE) "free" else "premium"
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(t.panel).border(1.dp, t.panelEdge, shape)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val sub = panelSub(card, status, web3)
        if (sub != null) {
            Text(
                sub, style = runTextStyle(14.sp, t.cobaltText, FontWeight.SemiBold, 1.3f), textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp).testTag("draw-$key-sub"),
            )
        }
        PanelValue(card, key)
        Image(
            cachedPainterResource(if (card.kind == DrawKind.PREMIUM) R.drawable.draw_box_closed_premium else R.drawable.draw_box_closed_free),
            contentDescription = stringResource(R.string.draw_box_description),
            contentScale = ContentScale.Fit,
            modifier = Modifier.padding(top = 18.dp).widthIn(max = 236.dp).fillMaxWidth().heightIn(max = 170.dp)
                .alpha(if (card.dim) 0.45f else 1f),
        )
        val detail = panelDetail(card, status, web3)
        if (detail != null) {
            Text(
                boldNumbers(detail, t.text), style = runTextStyle(14.5.sp, t.label, FontWeight.Medium, 1.35f).copy(lineBreak = KoreanLineBreak),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp).testTag("draw-$key-detail"),
            )
        }
        if (card.sub == DrawCardSub.Paused) {
            Text(
                stringResource(R.string.dv3_state_paused), style = runTextStyle(14.sp, t.gold, FontWeight.SemiBold, 1.3f),
                modifier = Modifier.padding(top = 6.dp).testTag("draw-premium-paused"),
            )
        }
        // 기회 내역 — 서버 값을 아는 동안만. 결과를 모르는 요청이 있거나 지갑을 연결한 적이 없으면 두지 않는다
        val history = status != null && !pendingAny && card.action != DrawCardAction.Connect
        if (history) {
            HistoryLink(
                { onSheet(if (card.kind == DrawKind.FREE) DrawSheet.FreeChances else DrawSheet.PremiumChances) },
                Modifier.padding(top = 4.dp).testTag("draw-$key-history"),
            )
        }
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

/** 작은 이름과 큰 수(가운데) — 남은 횟수 · 처음 연결하면 받을 수 · 보유한 상급 기회 · 뽑은 신발 확인 중 · 모르는 수 "—" */
@Composable
private fun PanelValue(card: DrawHomeCard, key: String) {
    val t = runTone()
    val label = when (val row = card.row) {
        is DrawRow.Gift -> stringResource(R.string.draw_blue_on_first_link)
        DrawRow.Pending -> stringResource(R.string.dv2_pending_label)
        is DrawRow.Count -> when {
            row.label == DrawRowLabel.PremiumKept -> stringResource(R.string.dv2_kept_premium)
            card.kind == DrawKind.FREE -> stringResource(R.string.rd_c_draw_free_left)
            else -> stringResource(R.string.draw_blue_left_label)
        }
        is DrawRow.Unknown -> stringResource(R.string.draw_blue_left_label)
    }
    val tag = if (card.row is DrawRow.Gift) "draw-premium-gift" else "draw-$key-value"
    // 이름과 수를 한 번에 읽는다("남은 횟수 13회")
    Column(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = runTextStyle(14.5.sp, t.label, FontWeight.Medium, 1.3f), textAlign = TextAlign.Center)
        when (val row = card.row) {
            is DrawRow.Count -> CountNumber(row.value, Modifier.padding(top = 4.dp), valueTag = "draw-$key-left")
            is DrawRow.Gift -> CountNumber(row.value, Modifier.padding(top = 4.dp))
            DrawRow.Pending -> Text(
                stringResource(R.string.dv2_pending_value), style = runNumberStyle(36.sp, t.text), maxLines = 1, softWrap = false,
                modifier = Modifier.padding(top = 6.dp).testTag("draw-pending"),
            )
            is DrawRow.Unknown -> DrawUnknownValue("draw-$key-unknown", Modifier.padding(top = 6.dp))
        }
    }
}

/**
 * 큰 수 + 단위 — 「16 회」 단위는 6dp 띄운다([PlainNumber]). 읽기 · 찾기용 글은 한 덩어리 그대로("16회" — [valueTag] 가 붙은 줄).
 * 단위는 서버 수를 넣은 문구(dv2_count)에서 수를 뺀 나머지다(영어는 단위 없음).
 */
@Composable
private fun CountNumber(value: Int, modifier: Modifier = Modifier, valueTag: String? = null) {
    val whole = stringResource(R.string.dv2_count, value)
    val number = value.toString()
    val unit = whole.replaceFirst(number, "").trim()
    val t = runTone()
    // 시안 08 — 큰 수는 본문 글꼴(페이퍼로지) ExtraBold · 숫자 폭 고정, 단위는 6dp 띄우고 같은 색 굵게
    Row(
        modifier.clearAndSetSemantics { text = AnnotatedString(whole) }
            .then(if (valueTag != null) Modifier.testTag(valueTag) else Modifier),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            number, style = runTextStyle(64.sp, t.text, FontWeight.ExtraBold, 1.1f).copy(fontFeatureSettings = "tnum", letterSpacing = (-0.02).em),
            maxLines = 1, softWrap = false,
        )
        if (unit.isNotEmpty()) {
            Spacer(Modifier.width(6.dp))
            Text(
                unit, style = runTextStyle(22.sp, t.text, FontWeight.ExtraBold, 1.2f), maxLines = 1, softWrap = false,
                modifier = Modifier.padding(bottom = 9.dp),
            )
        }
    }
}

/** 수 아래 한 줄 — 무료는 출처별 남은 수, 상급은 다음 1회까지의 거리(서버 값) · 오늘 한도 · 연결 규칙 · 다시 연결 · 결과 대기 */
@Composable
private fun panelDetail(card: DrawHomeCard, status: DrawStatus?, web3: Boolean): String? {
    if (card.action == DrawCardAction.SignIn) return stringResource(R.string.dv3_signed_out_title)
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

/** 한 줄 안의 수만 굵게 · 본문 색(시안 08 「받아 둔 상자 **15**회」) — 글자는 그대로 */
private fun boldNumbers(text: String, color: Color): AnnotatedString = androidx.compose.ui.text.buildAnnotatedString {
    var last = 0
    Regex("[0-9][0-9.,]*").findAll(text).forEach { m ->
        append(text.substring(last, m.range.first))
        withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = color)) { append(m.value) }
        last = m.range.last + 1
    }
    append(text.substring(last))
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
        DrawCardAction.Draw -> DrawButton(
            stringResource(if (card.kind == DrawKind.FREE) R.string.dv2_action_free else R.string.dv2_action_premium),
            { actions.onDraw(card.kind) }, modifier,
        )
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
