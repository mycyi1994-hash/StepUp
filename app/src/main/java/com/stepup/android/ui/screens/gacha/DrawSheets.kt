package com.stepup.android.ui.screens.gacha

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.DrawDistance
import com.stepup.android.domain.DrawStatus
import com.stepup.android.ui.components.SettingsSheet

/*
 * 메인 위의 안내창(10 · 11 · 13 · 14 · 15 · 16 · 17 · 18 · 21 · 24 · 25) — 설정 v1 시트 틀(SettingsSheet)에 담는다.
 * 순서는 제목 → 짧은 설명 · 상태 → 주 행동 → 보조 행동. X · 바깥 · 뒤로는 창만 닫고, 여닫아도 기회를 쓰지 않는다.
 * 내역의 숫자는 서버가 준 **남은** 수(출처별)만 — 처음 준 수나 평생 합과 섞지 않는다.
 */

/** 이름 — 값 한 줄. [strong] 이면 값이 강조색 */
@Composable
private fun SheetLine(label: String, value: String, strong: Boolean = false, tag: String? = null) {
    val p = drawPalette()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 42.dp).semantics(mergeDescendants = true) {}
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = p.secondary, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Text(value, color = if (strong) p.accent else p.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
    }
}

/** 합계 한 줄 — "지금 사용할 수 있는 기회 3회"(값은 강조색, 크게) */
@Composable
private fun SheetTotal(label: String, value: String, tag: String) {
    val p = drawPalette()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics(mergeDescendants = true) {}.testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = p.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(value, color = p.accent, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun SheetDivider() {
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(drawPalette().divider))
}

@Composable
private fun SheetBody(text: String, center: Boolean = false, modifier: Modifier = Modifier) {
    val p = drawPalette()
    Text(
        text, color = p.secondary, fontSize = 15.sp, lineHeight = 1.5.em,
        textAlign = if (center) TextAlign.Center else TextAlign.Start,
        modifier = modifier.fillMaxWidth(),
    )
}

/** 가운데 큰 그림(지갑 · 상자 · 느낌표) */
@Composable
private fun SheetHero(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = drawPalette().secondary, modifier = Modifier.size(52.dp))
    }
}

/** 그림 칩과 이름 · 값이 있는 한 줄 — 받는 방법 · 연결 혜택 */
@Composable
private fun SheetIconRow(icon: ImageVector, label: String, value: String, strong: Boolean = false, tag: String? = null) {
    val p = drawPalette()
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 54.dp).clip(shape).background(p.row).border(1.dp, p.rowEdge, shape)
            .padding(horizontal = 12.dp).semantics(mergeDescendants = true) {}
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(p.tabFace), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color(0xFFD6DDFF), modifier = Modifier.size(20.dp))
        }
        Text(label, color = p.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(value, color = if (strong) p.accent else p.text, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun SheetGroupLabel(text: String) {
    Text(text, color = drawPalette().secondary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
}

/** 알림 상자 — 오늘 러닝 기회를 모두 받았다 · 체인 작업 멈춤 */
@Composable
private fun SheetInfoBox(title: String, sub: String?, tag: String? = null) {
    val p = drawPalette()
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(p.row).border(1.dp, p.rowEdge, shape).padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics(mergeDescendants = true) {}.then(if (tag != null) Modifier.testTag(tag) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = p.secondary, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = p.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (sub != null) Text(sub, color = p.secondary, fontSize = 13.5.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun SheetNote(text: String) {
    Text(text, color = drawPalette().note, fontSize = 13.5.sp, lineHeight = 1.45.em)
}

/** "기회 받는 방법 ›" — 15 · 16 에서 17 로 */
@Composable
private fun RulesLink(onRules: () -> Unit) {
    DrawLink(stringResource(R.string.dv2_rules_title), onClick = onRules, chevron = true, fontSize = 15f, modifier = Modifier.testTag("draw-sheet-rules-link"))
}

/** 아래 두 버튼 — 주 행동(파란 면) · 보조 행동(테두리) */
@Composable
private fun ColumnScope.SheetActions(
    primary: String,
    onPrimary: () -> Unit,
    primaryTag: String,
    secondary: String? = null,
    onSecondary: () -> Unit = {},
    secondaryTag: String = "draw-sheet-close",
    primaryEnabled: Boolean = true,
) {
    DrawButton(primary, onPrimary, Modifier.testTag(primaryTag), enabled = primaryEnabled)
    if (secondary != null) {
        Spacer(Modifier.height(10.dp))
        DrawButton(secondary, onSecondary, Modifier.testTag(secondaryTag), DrawButtonStyle.Ghost)
    }
}

@Composable
private fun count(value: Int): String = stringResource(R.string.dv2_count, value)

/** 15 — 무료 뽑기 기회: 첫 가입 선물(남은 수) · 오늘 무료(남은 수) → 지금 쓸 수 있는 기회 */
@Composable
internal fun FreeChancesSheet(status: DrawStatus, canDraw: Boolean, onDraw: () -> Unit, onRules: () -> Unit, onClose: () -> Unit) {
    SettingsSheet(
        title = stringResource(R.string.dv2_free_sheet_title), onDismiss = onClose, modifier = Modifier.testTag("draw-sheet-free"),
        actions = {
            SheetActions(
                stringResource(R.string.dv2_action_free), onDraw, "draw-sheet-draw",
                stringResource(R.string.dv2_close), onClose, primaryEnabled = canDraw,
            )
        },
    ) {
        SheetBody(stringResource(R.string.dv2_free_sheet_sub))
        // 가입 선물이 없던 예전 계정에는 그 줄을 두지 않는다
        if (status.signupGranted > 0) SheetLine(stringResource(R.string.dv2_signup_gift), count(status.signupLeft))
        SheetLine(stringResource(R.string.dv2_daily), count(status.dailyLeft))
        SheetDivider()
        SheetTotal(stringResource(R.string.dv2_free_now), count(status.freeLeft), "draw-sheet-free-total")
        RulesLink(onRules)
    }
}

/** 16 — 상급 뽑기 기회: 첫 연결 선물 · 러닝으로 받은 기회(남은 수) → 남은 상급 뽑기, 다음 1회까지(24 오늘 한도면 알림 상자) */
@Composable
internal fun PremiumChancesSheet(status: DrawStatus, canDraw: Boolean, onDraw: () -> Unit, onRules: () -> Unit, onClose: () -> Unit) {
    SettingsSheet(
        title = stringResource(R.string.dv2_premium_sheet_title), onDismiss = onClose, modifier = Modifier.testTag("draw-sheet-premium"),
        actions = {
            SheetActions(
                stringResource(R.string.dv2_action_premium), onDraw, "draw-sheet-draw",
                stringResource(R.string.dv2_close), onClose, primaryEnabled = canDraw,
            )
        },
    ) {
        SheetLine(stringResource(R.string.dv2_link_gift), count(status.giftLeft))
        SheetLine(stringResource(R.string.dv2_run_earned), count(status.runLeft))
        SheetDivider()
        SheetLine(stringResource(R.string.dv2_premium_now), count(status.premiumLeft), tag = "draw-sheet-premium-total")
        if (status.walletLinked) {
            if (status.runCapReached) {
                // 24 — 오늘 러닝 지급 한도. 남은 기회는 그대로다(0 으로 바꾸지 않는다)
                SheetInfoBox(stringResource(R.string.dv2_run_capped), stringResource(R.string.dv3_run_capped_sub), "draw-sheet-run-cap")
            } else {
                RunProgress(status)
            }
        }
        if (status.chainPaused) SheetInfoBox(stringResource(R.string.draw_chain_paused), null)
        RulesLink(onRules)
    }
}

/** 러닝 진행 — 다음 1회까지 남은 거리 · 막대 · 모은 거리 / 한 칸 */
@Composable
private fun RunProgress(status: DrawStatus) {
    val p = drawPalette()
    val step = DrawDistance.stepKm(status.runStepMeters)
    Column(Modifier.fillMaxWidth().padding(top = 4.dp).testTag("draw-run-progress")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.dv2_next_premium, DrawDistance.remainingKm(status.metersToNextPremium)),
                color = p.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.dv2_progress, DrawDistance.progressKm(status.runProgressMeters), step),
                color = p.secondary, fontSize = 13.sp,
            )
        }
        DrawProgressBar(status.progressFraction, Modifier.padding(top = 10.dp))
    }
}

/** 17 — 기회 받는 방법. 숫자는 서버가 준 것만(가입 선물이 없던 계정 · 이미 연결한 계정은 그 줄을 뺀다) */
@Composable
internal fun RulesSheet(status: DrawStatus, onClose: () -> Unit) {
    val step = DrawDistance.stepKm(status.runStepMeters)
    SettingsSheet(
        title = stringResource(R.string.dv2_rules_title), onDismiss = onClose, modifier = Modifier.testTag("draw-sheet-rules"),
        actions = { SheetActions(stringResource(R.string.dv2_ok), onClose, "draw-sheet-ok") },
    ) {
        SheetGroupLabel(stringResource(R.string.dv2_free))
        if (status.signupGranted > 0) SheetIconRow(Icons.Outlined.PersonOutline, stringResource(R.string.dv2_rules_signup), count(status.signupGranted), strong = true)
        if (status.dailyTotal > 0) SheetIconRow(Icons.Outlined.CalendarMonth, stringResource(R.string.dv2_rules_daily), count(status.dailyTotal), strong = true)
        SheetGroupLabel(stringResource(R.string.dv2_premium))
        if (status.giftOnLink > 0) {
            SheetIconRow(Icons.Outlined.AccountBalanceWallet, stringResource(R.string.dv2_rules_first_link), count(status.giftOnLink), strong = true)
        }
        SheetIconRow(Icons.AutoMirrored.Outlined.DirectionsRun, stringResource(R.string.dv2_rules_run, step), stringResource(R.string.dv2_plus_one), strong = true)
        SheetNote(stringResource(R.string.dv2_rules_note))
    }
}

/** 10 — 연결 혜택. "WEB3 지갑 연결하기"는 기존 웹 지갑 페이지를 연다(가짜 지갑 목록 · 주소 · QR 없음) */
@Composable
internal fun WalletBenefitSheet(status: DrawStatus, onConnect: () -> Unit, onClose: () -> Unit) {
    val step = DrawDistance.stepKm(status.runStepMeters)
    SettingsSheet(
        title = stringResource(R.string.dv2_benefit_title), onDismiss = onClose, modifier = Modifier.testTag("draw-sheet-benefit"),
        actions = {
            SheetActions(
                stringResource(R.string.dv2_connect), onConnect, "draw-sheet-connect",
                stringResource(R.string.dv2_later), onClose, secondaryTag = "draw-sheet-later",
            )
        },
    ) {
        SheetHero(Icons.Outlined.AccountBalanceWallet)
        SheetBody(stringResource(R.string.dv2_benefit_sub), center = true)
        Spacer(Modifier.height(4.dp))
        if (status.giftOnLink > 0) SheetIconRow(Icons.Outlined.CardGiftcard, stringResource(R.string.dv2_benefit_first), count(status.giftOnLink))
        SheetIconRow(Icons.AutoMirrored.Outlined.DirectionsRun, stringResource(R.string.dv2_benefit_run, step), stringResource(R.string.dv2_plus_one), strong = true)
    }
}

/** 11 — 돌아와 읽은 서버 값에서 연결과 첫 연결 선물 [gift] 회를 확인했다(다시 연결은 짧은 알림만) */
@Composable
internal fun WalletLinkedSheet(gift: Int, canDraw: Boolean, onDraw: () -> Unit, onClose: () -> Unit) {
    val p = drawPalette()
    SettingsSheet(
        title = stringResource(R.string.dv2_linked_title), onDismiss = onClose, modifier = Modifier.testTag("draw-sheet-linked"),
        actions = {
            SheetActions(
                stringResource(R.string.dv2_action_premium), onDraw, "draw-sheet-draw",
                stringResource(R.string.dv2_to_draw), onClose, primaryEnabled = canDraw,
            )
        },
    ) {
        Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(54.dp).clip(CircleShape).background(p.primaryFace), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Text(
                stringResource(R.string.dv2_count_plus, gift), color = p.text, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 14.dp).testTag("draw-sheet-linked-gift"),
            )
            Text(stringResource(R.string.dv2_linked_body), color = p.secondary, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** 13 — 무료 기회를 모두 썼다. 갱신 시각 · 소멸을 지어내지 않는다 */
@Composable
internal fun FreeEmptySheet(status: DrawStatus, onSeePremium: () -> Unit, onClose: () -> Unit) {
    SettingsSheet(
        title = stringResource(R.string.dv2_free_empty_title), onDismiss = onClose, modifier = Modifier.testTag("draw-sheet-free-empty"),
        actions = {
            SheetActions(
                stringResource(R.string.dv2_ok), onClose, "draw-sheet-ok",
                stringResource(R.string.dv2_see_premium), onSeePremium, secondaryTag = "draw-sheet-see-premium",
            )
        },
    ) {
        SheetHero(Icons.Outlined.Inventory2)
        SheetBody(stringResource(R.string.dv2_free_empty_body), center = true)
        if (status.dailyTotal > 0) SheetIconRow(Icons.Outlined.CalendarMonth, stringResource(R.string.dv3_daily_free), count(status.dailyTotal))
    }
}

/** 14 — 러닝으로 기회 받기(연결됨 · 상급 기회 없음). "러닝 시작"은 기존 자유 러닝 시작 흐름 */
@Composable
internal fun RunChancesSheet(status: DrawStatus, onStartRun: () -> Unit, onClose: () -> Unit) {
    val p = drawPalette()
    val step = DrawDistance.stepKm(status.runStepMeters)
    SettingsSheet(
        title = stringResource(R.string.dv2_run_title), onDismiss = onClose, modifier = Modifier.testTag("draw-sheet-run"),
        actions = {
            SheetActions(stringResource(R.string.dv2_run_start), onStartRun, "draw-sheet-run-start", stringResource(R.string.dv2_close), onClose)
        },
    ) {
        SheetBody(stringResource(R.string.dv2_stop_no_premium))
        if (status.runCapReached) {
            SheetInfoBox(stringResource(R.string.dv2_run_capped), stringResource(R.string.dv3_run_capped_sub), "draw-sheet-run-cap")
        } else {
            Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.dv2_run_until), color = p.secondary, fontSize = 14.sp)
                Text(
                    stringResource(R.string.dv2_km, DrawDistance.remainingKm(status.metersToNextPremium)),
                    color = p.text, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 2.dp),
                )
            }
            DrawProgressBar(status.progressFraction)
            Text(
                stringResource(R.string.dv2_progress, DrawDistance.progressKm(status.runProgressMeters), step),
                color = p.secondary, fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * 18 — 뽑기가 시작되지 않았고 기회를 쓰지 않은 것이 확실할 때만. "다시 뽑기"는 다시 해 볼 만한 까닭일 때만.
 * 25 — 발행 한도는 "지금은 뽑을 수 없어요"와 확인 하나.
 */
@Composable
internal fun NotStartedSheet(stop: DrawStop, canRetry: Boolean, onRetry: () -> Unit, onClose: () -> Unit) {
    val p = drawPalette()
    val blocked = stop == DrawStop.MintLimit
    SettingsSheet(
        title = stringResource(if (blocked) R.string.dv3_stop_title_blocked else R.string.dv2_stop_title), onDismiss = onClose,
        modifier = Modifier.testTag("draw-sheet-not-started"),
        actions = {
            if (canRetry) {
                SheetActions(stringResource(R.string.dv2_retry_draw), onRetry, "draw-sheet-retry", stringResource(R.string.dv2_close), onClose)
            } else {
                SheetActions(stringResource(R.string.dv2_ok), onClose, "draw-sheet-ok")
            }
        },
    ) {
        if (!blocked) SheetHero(Icons.Outlined.ErrorOutline)
        Text(
            stringResource(R.string.dv2_stop_unused), color = p.text, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            textAlign = if (blocked) TextAlign.Start else TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        SheetBody(
            stringResource(
                when (stop) {
                    DrawStop.Network -> R.string.dv2_stop_network
                    DrawStop.NoFree -> R.string.dv2_stop_no_free
                    DrawStop.NoPremium -> R.string.dv2_stop_no_premium
                    DrawStop.Wallet -> R.string.dv2_stop_wallet
                    DrawStop.MintLimit -> R.string.toast_mint_limit
                    DrawStop.ChainPaused -> R.string.draw_chain_paused
                    DrawStop.SignIn -> R.string.dv2_stop_sign_in
                    DrawStop.Other -> R.string.dv2_stop_other
                },
            ),
            center = !blocked,
        )
    }
}

/** 21 — 횟수를 불러오지 못했다(소진과 다르다). 닫아도 메인에 "—"와 "다시 불러오기"가 남는다 */
@Composable
internal fun LoadFailedSheet(onReload: () -> Unit, onBack: () -> Unit, onClose: () -> Unit) {
    SettingsSheet(
        title = stringResource(R.string.dv2_load_failed_title), onDismiss = onClose, modifier = Modifier.testTag("draw-sheet-load-failed"),
        actions = {
            SheetActions(
                stringResource(R.string.dv2_action_reload), onReload, "draw-sheet-reload",
                stringResource(R.string.dv2_back_to_shoes), onBack, secondaryTag = "draw-sheet-back",
            )
        },
    ) {
        SheetBody(stringResource(R.string.dv2_load_failed_body))
    }
}
