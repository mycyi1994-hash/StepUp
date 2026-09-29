package com.stepup.android.ui.screens.gacha

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.DrawKind
import com.stepup.android.domain.DrawStatus
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpDesign

/*
 * 신발 뽑기 메인(01 · 02 · 03 · 09 · 12 · 20 · 21 · 22 · 23) — 제목 · ⓘ → 무료/상급 글자 탭 → 상자 무대 → 남은 횟수 한 줄 →
 * 일반 크기 실행 버튼 하나. 공통 머리(로고 · 잔액)와 하단 탭은 앱 셸이 그린다. 일반 폰 높이에서는 스크롤 없이 한 화면에
 * 들어가도록 글자 칸을 먼저 재고 상자 무대에 남은 높이를 준다(150–312dp). 그래도 모자라면(작은 화면 · 큰 글씨) 스크롤한다.
 * 삭제된 하단 고정 설명("러닝 1km마다" · "오늘 남은 기회")은 두지 않는다 — 정책 설명은 ⓘ 가 여는 안내창 안에만.
 */

private val StageMax = 312.dp
private val StageMin = 150.dp

/** 메인 한 화면 — [tab] 은 고른 글자 탭. 수 · 연결 상태는 서버 값([status])만 */
@Composable
internal fun DrawHome(
    /** 아래에 비울 자리 — 짧은 알림(26)이 보이는 동안 */
    bottomInset: Dp,
    status: DrawStatus?,
    loading: Boolean,
    signedOut: Boolean,
    tab: DrawKind,
    pending: DrawPending?,
    actions: DrawActions,
    onTab: (DrawKind) -> Unit,
    onSheet: (DrawSheet) -> Unit,
) {
    val card = drawHomeCard(status, tab, pending?.kind, loading, signedOut)
    BoxWithConstraints(Modifier.fillMaxSize().padding(bottom = bottomInset).testTag("draw-home")) {
        val viewport = constraints.maxHeight
        DrawFitLayout(
            viewport = viewport,
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag("draw-scroll"),
            topGap = 6.dp, stageGap = 18.dp, endGap = 20.dp, minStage = StageMin, maxStage = StageMax,
            top = {
                Column(Modifier.fillMaxWidth().padding(horizontal = StepUpDesign.Gutter)) {
                    DrawTitleRow(onInfo = status?.let { s -> { onSheet(drawInfoSheet(s, tab)) } })
                    DrawKindTabs(tab, onTab, Modifier.padding(top = 14.dp))
                }
            },
            stage = { DrawStage(card, status, Modifier.fillMaxSize().padding(horizontal = StepUpDesign.Gutter)) },
            bottom = {
                Column(Modifier.fillMaxWidth().padding(horizontal = StepUpDesign.Gutter)) {
                    when (val row = card.row) {
                        is DrawRow.Gift -> DrawBenefitRow(row.value, Modifier.padding(top = 20.dp))
                        else -> DrawCountRow(card, Modifier.padding(top = 20.dp))
                    }
                    DrawMainButton(card, status, actions, onSheet, Modifier.padding(top = 16.dp))
                }
            },
        )
    }
}

/** 제목 "신발 뽑기"와 옆의 작은 ⓘ(안내창 — 기회 내역 · 받는 방법). 서버 값을 모르면 ⓘ 를 두지 않는다 */
@Composable
private fun DrawTitleRow(onInfo: (() -> Unit)?) {
    val p = drawPalette()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.dv2_title), color = p.text, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.4).sp, lineHeight = 40.sp,
            modifier = Modifier.semantics { heading() }.testTag("draw-title"),
        )
        if (onInfo != null) {
            val label = stringResource(R.string.dv3_info_cd)
            Box(
                Modifier.padding(start = 2.dp).size(48.dp).clip(RoundedCornerShape(24.dp))
                    .feedbackClickable(role = Role.Button, onClick = onInfo)
                    .semantics { contentDescription = label }.testTag("draw-info"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = p.secondary, modifier = Modifier.size(22.dp))
            }
        }
    }
}

/** 무료 뽑기 / 상급 뽑기 글자 탭 — 신발 탭의 내 신발 / 신발 보관함과 다른 줄이다 */
@Composable
internal fun DrawKindTabs(selected: DrawKind, onSelect: (DrawKind) -> Unit, modifier: Modifier = Modifier) {
    val p = drawPalette()
    val outer = RoundedCornerShape(15.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp).clip(outer).background(p.tabFace).border(1.5.dp, p.tabEdge, outer).padding(3.dp),
    ) {
        listOf(DrawKind.FREE to R.string.dv2_free, DrawKind.PREMIUM to R.string.dv2_premium).forEach { (kind, label) ->
            val on = kind == selected
            val inner = RoundedCornerShape(12.dp)
            Box(
                Modifier.weight(1f).heightIn(min = 42.dp).clip(inner)
                    .then(if (on) Modifier.background(p.tabOn).border(1.dp, p.tabOnEdge, inner) else Modifier)
                    .feedbackClickable(role = Role.Tab, onClick = { onSelect(kind) })
                    .semantics { this.selected = on; role = Role.Tab }
                    .testTag(if (kind == DrawKind.FREE) "draw-tab-free" else "draw-tab-premium"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(label), color = if (on) Color.White else p.tabOffText, fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 상자 무대 — 누르는 곳이 아니다. 제목 · 한 줄 · 오른쪽 위 수(또는 지갑 그림) · 가운데 닫힌 상자 */
@Composable
private fun DrawStage(card: DrawHomeCard, status: DrawStatus?, modifier: Modifier) {
    val p = drawPalette()
    val shape = RoundedCornerShape(22.dp)
    val title = when (card.title) {
        DrawCardTitle.DailyFree -> stringResource(R.string.dv3_stage_daily_free, status?.dailyTotal ?: 0)
        DrawCardTitle.Free -> stringResource(R.string.dv2_free)
        DrawCardTitle.Premium -> stringResource(R.string.dv2_premium)
        DrawCardTitle.Pending -> stringResource(R.string.dv2_pending_label)
        DrawCardTitle.SignedOut -> stringResource(R.string.dv3_signed_out_title)
    }
    val sub = when (card.sub) {
        DrawCardSub.Linked -> stringResource(R.string.dv3_state_linked)
        DrawCardSub.WalletNeeded -> stringResource(R.string.dv3_state_wallet_needed)
        DrawCardSub.Paused -> stringResource(R.string.dv3_state_paused)
        null -> null
    }
    Box(
        modifier.clip(shape).background(Brush.verticalGradient(listOf(p.stageFrom, p.stageTo)))
            .testTag(if (card.kind == DrawKind.FREE) "draw-free" else "draw-premium"),
    ) {
        // 가운데 아래의 은은한 빛(시안의 radial 50% 62%)과 바닥 그림자
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.62f)
            val radius = size.width * 0.62f
            drawCircle(Brush.radialGradient(listOf(p.stageGlow, Color.Transparent), center, radius), radius, center)
            val floor = Offset(size.width / 2f, size.height - 38.dp.toPx())
            val w = minOf(280.dp.toPx(), size.width * 0.8f)
            drawOval(
                Brush.radialGradient(listOf(Color(0x8C0A1030), Color.Transparent), floor, w / 2f),
                topLeft = Offset(floor.x - w / 2f, floor.y - 20.dp.toPx()), size = Size(w, 40.dp.toPx()),
            )
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // 닫힌 상자 — 무대 높이에서 머리 자리(약 86dp)와 아래 여백(26dp)을 뺀 만큼, 폭은 292dp 까지
            val boxHeight = (maxHeight - 112.dp).coerceAtLeast(40.dp)
            val boxWidth = min(min(292.dp, maxWidth - 40.dp), boxHeight * BoxRatio)
            val matrix = remember(card.dim) {
                if (card.dim) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.7f); timesAssign(ColorMatrix().apply { setToScale(0.62f, 0.62f, 0.62f, 1f) }) }) else null
            }
            Image(
                painterResource(if (card.kind == DrawKind.PREMIUM) R.drawable.draw_box_closed_premium else R.drawable.draw_box_closed_free),
                contentDescription = stringResource(R.string.draw_box_description),
                contentScale = ContentScale.Fit, colorFilter = matrix,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp).width(boxWidth).height(boxWidth / BoxRatio),
            )
            if (card.loading) {
                Column(
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(Modifier.size(30.dp), color = Color(0xFF9FB3FF), strokeWidth = 3.dp, trackColor = Color(0x409FB3FF))
                    Text(
                        stringResource(R.string.dv2_action_loading), color = Color(0xFFC5CEF4), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 20.dp, top = 22.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(top = 4.dp)) {
                Text(
                    title, color = p.stageText, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp,
                    lineHeight = 30.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("draw-stage-title"),
                )
                if (sub != null) {
                    Text(
                        sub, color = p.stageSub, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp).testTag("draw-stage-sub"),
                    )
                }
            }
            DrawChipView(card.chip, Modifier.padding(start = 12.dp))
        }
    }
}

/** 닫힌 상자 그림의 가로:세로(943:501) */
private const val BoxRatio = 943f / 501f

/** 무대 오른쪽 위 — 수 · 확인 중 · 지갑 그림 · "—". 수는 한 번만 읽히게(아래 줄이 같은 수를 읽는다) */
@Composable
private fun DrawChipView(chip: DrawChip, modifier: Modifier) {
    val p = drawPalette()
    val shape = RoundedCornerShape(12.dp)
    when (chip) {
        is DrawChip.Count -> Box(
            modifier.heightIn(min = 36.dp).widthIn(min = 58.dp).clip(shape).background(p.chipFace)
                .clearAndSetSemantics { }.padding(horizontal = 14.dp).testTag("draw-chip"),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.dv2_count, chip.value), color = p.chipText, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        }
        DrawChip.Checking -> Box(
            modifier.heightIn(min = 36.dp).clip(shape).background(p.chipQuietFace).clearAndSetSemantics { }.padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.dv2_pending_value), color = p.chipQuietText, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        DrawChip.Unknown -> Box(
            modifier.heightIn(min = 36.dp).widthIn(min = 58.dp).clip(shape).background(p.chipFace.copy(alpha = 0.8f)).clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) {
            Text(DASH, color = p.chipText, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
        }
        DrawChip.Wallet -> Icon(
            Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = Color(0xFFC9D3FF),
            modifier = modifier.size(34.dp).testTag("draw-chip-wallet"),
        )
        DrawChip.None -> Unit
    }
}

/** 모르는 수 — 0 으로 보이지 않는다 */
internal const val DASH = "—"

/** 남은 횟수 한 줄 — 이름(작게)과 큰 수. 결과 확인 중이면 "확인 중" */
@Composable
private fun DrawCountRow(card: DrawHomeCard, modifier: Modifier) {
    val p = drawPalette()
    val row = card.row
    val label = when (row) {
        is DrawRow.Count -> row.label
        is DrawRow.Unknown -> row.label
        else -> DrawRowLabel.Result
    }
    val name = stringResource(
        when (label) {
            DrawRowLabel.FreeLeft -> R.string.dv2_result_free_left
            DrawRowLabel.PremiumLeft -> R.string.dv2_result_premium_left
            DrawRowLabel.PremiumKept -> R.string.dv2_kept_premium
            DrawRowLabel.Result -> R.string.dv3_pending_row
        },
    )
    val kindTag = if (card.kind == DrawKind.FREE) "draw-free-left" else "draw-premium-left"
    Row(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, color = p.secondary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        when (row) {
            is DrawRow.Count -> Text(
                stringResource(R.string.dv2_count, row.value), color = p.text, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp, lineHeight = 48.sp, maxLines = 1, modifier = Modifier.testTag(kindTag),
            )
            is DrawRow.Unknown -> Text(
                DASH, color = p.text, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 48.sp,
                modifier = Modifier.testTag("draw-left-unknown"),
            )
            DrawRow.Pending -> Text(
                stringResource(R.string.dv2_pending_value), color = p.text, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
                lineHeight = 48.sp, maxLines = 1, modifier = Modifier.testTag("draw-pending"),
            )
            is DrawRow.Gift -> Unit
        }
    }
}

/** 09 — 받을 수 있는 첫 연결 선물(이미 가진 횟수가 아니다) */
@Composable
private fun DrawBenefitRow(gift: Int, modifier: Modifier) {
    val p = drawPalette()
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape).background(p.row).border(1.dp, p.rowEdge, shape)
            .padding(horizontal = 16.dp).semantics(mergeDescendants = true) {}.testTag("draw-premium-gift"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.CardGiftcard, contentDescription = null, tint = Color(0xFFD6DDFF), modifier = Modifier.size(24.dp))
        Text(stringResource(R.string.dv3_gift_available), color = p.text.copy(alpha = 0.9f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(stringResource(R.string.dv2_count, gift), color = p.text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

/** 실행 버튼 하나 — 고른 탭의 행동. 태그는 종류마다(draw-free-action · draw-premium-action) */
@Composable
private fun DrawMainButton(card: DrawHomeCard, status: DrawStatus?, actions: DrawActions, onSheet: (DrawSheet) -> Unit, modifier: Modifier) {
    val tag = modifier.testTag(if (card.kind == DrawKind.FREE) "draw-free-action" else "draw-premium-action")
    when (card.action) {
        DrawCardAction.Draw -> DrawButton(
            stringResource(if (card.kind == DrawKind.FREE) R.string.dv2_action_free else R.string.dv2_action_premium),
            { actions.onDraw(card.kind) }, tag,
        )
        DrawCardAction.FreeInfo -> DrawButton(stringResource(R.string.dv2_action_free_info), { onSheet(DrawSheet.FreeEmpty) }, tag, DrawButtonStyle.Ghost)
        DrawCardAction.RunInfo -> DrawButton(stringResource(R.string.dv2_action_run), { onSheet(DrawSheet.RunChances) }, tag, DrawButtonStyle.Ghost)
        DrawCardAction.Connect -> DrawButton(
            stringResource(R.string.dv2_action_connect, status?.giftOnLink ?: 0), { onSheet(DrawSheet.WalletBenefit) }, tag,
        )
        // 12 — 선물은 다시 주지 않는다. 연결은 기존 웹 지갑 페이지에서
        DrawCardAction.Reconnect -> DrawButton(stringResource(R.string.dv2_action_reconnect), actions.onConnectWallet, tag)
        DrawCardAction.Paused -> DrawButton(stringResource(R.string.dv2_action_paused), {}, tag, DrawButtonStyle.Off)
        DrawCardAction.Check -> DrawButton(stringResource(R.string.dv2_action_check), actions.onCheckPending, tag)
        DrawCardAction.Loading -> DrawButton(stringResource(R.string.dv2_action_loading), {}, tag, DrawButtonStyle.Off)
        DrawCardAction.Reload -> DrawButton(stringResource(R.string.dv2_action_reload), actions.onRetry, tag)
        // 22 — 로그인 화면으로(로그인할 수 없는 빌드면 누를 수 없다)
        DrawCardAction.SignIn -> {
            val signIn = actions.onSignIn
            if (signIn != null) DrawButton(stringResource(R.string.dv3_action_sign_in), signIn, tag)
            else DrawButton(stringResource(R.string.dv2_action_sign_in), {}, tag, DrawButtonStyle.Off)
        }
    }
}

/** 18 러닝 반영 · 다시 연결 — 서버 값이 바뀐 것을 확인했을 때만 잠깐(26). 하단 탭 위 12dp */
@Composable
internal fun DrawToast(text: String?, modifier: Modifier = Modifier) {
    if (text == null) return
    val p = drawPalette()
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp).clip(shape).background(p.toastFace).border(1.dp, p.toastEdge, shape)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(22.dp).clip(RoundedCornerShape(11.dp)).background(p.primaryFace), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
        Text(text, color = Color(0xFFF2F4FC), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}
