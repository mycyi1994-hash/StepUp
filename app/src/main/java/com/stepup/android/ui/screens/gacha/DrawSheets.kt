package com.stepup.android.ui.screens.gacha

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.DrawDistance
import com.stepup.android.domain.DrawStatus
import com.stepup.android.ui.components.SettingsPrimaryButton
import com.stepup.android.ui.components.SettingsSheet
import com.stepup.android.ui.experience.feedbackClickable

/*
 * 두 칸 위의 시트 — 설정 v1 시트(SettingsSheet)에 담는다. X · 바깥 · 뒤로는 시트만 닫고, 여닫아도 기회를 쓰지 않는다.
 * 내역의 숫자는 서버가 준 **남은** 수(출처별)만 — 처음 준 수나 평생 합과 섞지 않는다.
 */

/** 이름 — 값 한 줄. [strong] 이면 값이 강조색(합계 · 받는 수) */
@Composable
private fun SheetLine(label: String, value: String, strong: Boolean = false, tag: String? = null) {
    val p = drawPalette()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 40.dp).semantics(mergeDescendants = true) {}
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = p.secondary, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(value, color = if (strong) p.accent else p.text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SheetDivider() {
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(drawPalette().divider))
}

@Composable
private fun SheetBody(text: String, bold: Boolean = false) {
    val p = drawPalette()
    Text(
        text, color = if (bold) p.text else p.secondary, fontSize = if (bold) 16.sp else 15.sp,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal, lineHeight = 1.5.em,
    )
}

/** 아래 글자 버튼 — "닫기" · "나중에" · "뽑기 화면으로". 누르는 곳은 48dp */
@Composable
private fun ColumnScope.SheetTextAction(text: String, onClick: () -> Unit, tag: String) {
    val p = drawPalette()
    Box(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).feedbackClickable(role = Role.Button, onClick = onClick).testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = p.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun count(value: Int): String = stringResource(R.string.dv2_count, value)

/** 03 — 무료 기회 내역: 첫 가입 선물(남은 수) · 오늘 무료(남은 수) → 지금 쓸 수 있는 기회 */
@Composable
internal fun FreeChancesSheet(status: DrawStatus, canDraw: Boolean, onDraw: () -> Unit, onClose: () -> Unit) {
    SettingsSheet(
        title = stringResource(R.string.dv2_free_sheet_title), onDismiss = onClose, modifier = Modifier.testTag("draw-sheet-free"),
        actions = {
            SettingsPrimaryButton(stringResource(R.string.dv2_action_free), onDraw, Modifier.fillMaxWidth().testTag("draw-sheet-draw"), enabled = canDraw)
            SheetTextAction(stringResource(R.string.dv2_close), onClose, "draw-sheet-close")
        },
    ) {
        SheetBody(stringResource(R.string.dv2_free_sheet_sub))
        // 가입 선물이 없던 예전 계정에는 그 줄을 두지 않는다
        if (status.signupGranted > 0) SheetLine(stringResource(R.string.dv2_signup_gift), count(status.signupLeft))
        SheetLine(stringResource(R.string.dv2_daily), count(status.dailyLeft))
        SheetDivider()
        SheetLine(stringResource(R.string.dv2_free_now), count(status.freeLeft), strong = true, tag = "draw-sheet-free-total")
    }
}

/** 04 — 상급 기회 내역 · 러닝 진행: 첫 연결 선물(남은 수) · 러닝으로 받은 기회(남은 수) → 남은 상급 뽑기, 다음 1회까지 */
@Composable
internal fun PremiumChancesSheet(status: DrawStatus, canDraw: Boolean, onDraw: () -> Unit, onClose: () -> Unit) {
    val step = DrawDistance.stepKm(status.runStepMeters)
    SettingsSheet(
        title = stringResource(R.string.dv2_premium_sheet_title), onDismiss = onClose, modifier = Modifier.testTag("draw-sheet-premium"),
        actions = {
            SettingsPrimaryButton(stringResource(R.string.dv2_action_premium), onDraw, Modifier.fillMaxWidth().testTag("draw-sheet-draw"), enabled = canDraw)
            SheetTextAction(stringResource(R.string.dv2_close), onClose, "draw-sheet-close")
        },
    ) {
        SheetBody(stringResource(R.string.dv2_premium_sheet_sub, step))
        SheetLine(stringResource(R.string.dv2_link_gift), count(status.giftLeft))
        SheetLine(stringResource(R.string.dv2_run_earned), count(status.runLeft))
        SheetDivider()
        SheetLine(stringResource(R.string.dv2_premium_now), count(status.premiumLeft), strong = true, tag = "draw-sheet-premium-total")
        RunProgress(status)
        if (status.chainPaused) SheetBody(stringResource(R.string.draw_chain_paused))
    }
}

/** 러닝 진행 — 다음 1회까지 남은 거리 · 막대 · 모은 거리 / 한 칸. 오늘 한도를 채웠으면 그 말 */
@Composable
private fun RunProgress(status: DrawStatus) {
    val p = drawPalette()
    val step = DrawDistance.stepKm(status.runStepMeters)
    Column(Modifier.fillMaxWidth().padding(top = 6.dp).testTag("draw-run-progress")) {
        Text(
            stringResource(R.string.dv2_next_premium, DrawDistance.remainingKm(status.metersToNextPremium)),
            color = p.secondary, fontSize = 15.sp,
        )
        DrawProgressBar(status.progressFraction, Modifier.padding(top = 12.dp))
        Text(
            stringResource(R.string.dv2_progress, DrawDistance.progressKm(status.runProgressMeters), step),
            color = p.secondary, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp),
        )
        if (status.runCapReached) {
            Text(stringResource(R.string.draw_run_cap_reached), color = p.secondary, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/** 05 — 받는 방법. 숫자는 서버가 준 것만(가입 선물이 없던 계정 · 이미 연결한 계정은 그 줄을 뺀다) */
@Composable
internal fun RulesSheet(status: DrawStatus, onClose: () -> Unit) {
    val p = drawPalette()
    val step = DrawDistance.stepKm(status.runStepMeters)
    SettingsSheet(
        title = stringResource(R.string.dv2_rules_title), onDismiss = onClose, modifier = Modifier.testTag("draw-sheet-rules"),
        actions = { SettingsPrimaryButton(stringResource(R.string.dv2_ok), onClose, Modifier.fillMaxWidth().testTag("draw-sheet-ok")) },
    ) {
        Text(stringResource(R.string.dv2_free), color = p.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        if (status.signupGranted > 0) SheetLine(stringResource(R.string.dv2_rules_signup), count(status.signupGranted))
        if (status.dailyTotal > 0) SheetLine(stringResource(R.string.dv2_rules_daily), count(status.dailyTotal))
        SheetDivider()
        Text(stringResource(R.string.dv2_premium), color = p.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        if (status.giftOnLink > 0) SheetLine(stringResource(R.string.dv2_rules_first_link), count(status.giftOnLink))
        SheetLine(stringResource(R.string.dv2_rules_run, step), stringResource(R.string.dv2_plus_one))
        SheetBody(stringResource(R.string.dv2_rules_note))
    }
}

/** 06 — 연결 혜택. "WEB3 지갑 연결하기"는 기존 웹 지갑 페이지를 연다(가짜 지갑 목록 · 주소 · QR 없음) */
@Composable
internal fun WalletBenefitSheet(status: DrawStatus, onConnect: () -> Unit, onClose: () -> Unit) {
    val step = DrawDistance.stepKm(status.runStepMeters)
    SettingsSheet(
        title = stringResource(R.string.dv2_benefit_title), onDismiss = onClose, modifier = Modifier.testTag("draw-sheet-benefit"),
        actions = {
            SettingsPrimaryButton(stringResource(R.string.dv2_connect), onConnect, Modifier.fillMaxWidth().testTag("draw-sheet-connect"))
            SheetTextAction(stringResource(R.string.dv2_later), onClose, "draw-sheet-later")
        },
    ) {
        SheetBody(stringResource(R.string.dv2_benefit_sub))
        if (status.giftOnLink > 0) SheetLine(stringResource(R.string.dv2_benefit_first), count(status.giftOnLink), strong = true)
        SheetLine(stringResource(R.string.dv2_benefit_run, step), stringResource(R.string.dv2_plus_one), strong = true)
        SheetDivider()
        SheetBody(stringResource(R.string.dv2_benefit_note))
    }
}

/** 08 — 돌아와 읽은 서버 값에서 연결과 첫 연결 선물 [gift] 회를 확인했다 */
@Composable
internal fun WalletLinkedSheet(gift: Int, canDraw: Boolean, onDraw: () -> Unit, onClose: () -> Unit) {
    val p = drawPalette()
    SettingsSheet(
        title = stringResource(R.string.dv2_linked_title), onDismiss = onClose, modifier = Modifier.testTag("draw-sheet-linked"),
        actions = {
            SettingsPrimaryButton(stringResource(R.string.dv2_action_premium), onDraw, Modifier.fillMaxWidth().testTag("draw-sheet-draw"), enabled = canDraw)
            SheetTextAction(stringResource(R.string.dv2_to_draw), onClose, "draw-sheet-close")
        },
    ) {
        Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(p.quietFace), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = p.accent, modifier = Modifier.size(28.dp))
            }
            Text(
                stringResource(R.string.dv2_count_plus, gift), color = p.text, fontSize = 40.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 14.dp).testTag("draw-sheet-linked-gift"),
            )
            Text(stringResource(R.string.dv2_linked_body), color = p.secondary, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

/** 16 — 무료 기회 소진. 갱신 시각 · 소멸을 지어내지 않는다 */
@Composable
internal fun FreeEmptySheet(status: DrawStatus, onSeePremium: () -> Unit, onClose: () -> Unit) {
    SettingsSheet(
        title = stringResource(R.string.dv2_free_empty_title), onDismiss = onClose, modifier = Modifier.testTag("draw-sheet-free-empty"),
        actions = {
            SettingsPrimaryButton(stringResource(R.string.dv2_ok), onClose, Modifier.fillMaxWidth().testTag("draw-sheet-ok"))
            SheetTextAction(stringResource(R.string.dv2_see_premium), onSeePremium, "draw-sheet-see-premium")
        },
    ) {
        if (status.dailyTotal > 0) SheetBody(stringResource(R.string.dv2_free_empty_head, status.dailyTotal), bold = true)
        SheetBody(stringResource(R.string.dv2_free_empty_body))
    }
}

/** 17 — 러닝으로 상급 기회 받기. "러닝 시작"은 기존 자유 러닝 시작 흐름 */
@Composable
internal fun RunChancesSheet(status: DrawStatus, onStartRun: () -> Unit, onClose: () -> Unit) {
    val p = drawPalette()
    val step = DrawDistance.stepKm(status.runStepMeters)
    SettingsSheet(
        title = stringResource(R.string.dv2_run_title), onDismiss = onClose, modifier = Modifier.testTag("draw-sheet-run"),
        actions = {
            SettingsPrimaryButton(stringResource(R.string.dv2_run_start), onStartRun, Modifier.fillMaxWidth().testTag("draw-sheet-run-start"))
            SheetTextAction(stringResource(R.string.dv2_close), onClose, "draw-sheet-close")
        },
    ) {
        SheetBody(stringResource(R.string.dv2_run_sub))
        Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
            Text(stringResource(R.string.dv2_run_until), color = p.secondary, fontSize = 14.sp)
            Text(
                stringResource(R.string.dv2_km, DrawDistance.remainingKm(status.metersToNextPremium)),
                color = p.text, fontSize = 36.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp),
            )
        }
        DrawProgressBar(status.progressFraction)
        Text(
            stringResource(R.string.dv2_progress, DrawDistance.progressKm(status.runProgressMeters), step),
            color = p.secondary, fontSize = 13.sp,
        )
        SheetBody(stringResource(R.string.dv2_run_rule, step))
        if (status.runCapReached) SheetBody(stringResource(R.string.draw_run_cap_reached))
    }
}

/** 19 — 뽑기가 시작되지 않았고 기회를 쓰지 않은 것이 확실할 때만. "다시 뽑기"는 다시 해 볼 만한 까닭일 때만 */
@Composable
internal fun NotStartedSheet(stop: DrawStop, canRetry: Boolean, onRetry: () -> Unit, onClose: () -> Unit) {
    SettingsSheet(
        title = stringResource(R.string.dv2_stop_title), onDismiss = onClose, modifier = Modifier.testTag("draw-sheet-not-started"),
        actions = {
            if (canRetry) {
                SettingsPrimaryButton(stringResource(R.string.dv2_retry_draw), onRetry, Modifier.fillMaxWidth().testTag("draw-sheet-retry"))
                SheetTextAction(stringResource(R.string.dv2_close), onClose, "draw-sheet-close")
            } else {
                SettingsPrimaryButton(stringResource(R.string.dv2_ok), onClose, Modifier.fillMaxWidth().testTag("draw-sheet-ok"))
            }
        },
    ) {
        SheetBody(stringResource(R.string.dv2_stop_unused), bold = true)
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
        )
    }
}

/** 23 — 횟수를 불러오지 못했다(소진과 다르다). 닫아도 칸에 설명과 "다시 불러오기"가 남는다 */
@Composable
internal fun LoadFailedSheet(onReload: () -> Unit, onBack: () -> Unit, onClose: () -> Unit) {
    SettingsSheet(
        title = stringResource(R.string.dv2_load_failed_title), onDismiss = onClose, modifier = Modifier.testTag("draw-sheet-load-failed"),
        actions = {
            SettingsPrimaryButton(stringResource(R.string.dv2_action_reload), onReload, Modifier.fillMaxWidth().testTag("draw-sheet-reload"))
            SheetTextAction(stringResource(R.string.dv2_back_to_shoes), onBack, "draw-sheet-back")
        },
    ) {
        SheetBody(stringResource(R.string.dv2_load_failed_body))
    }
}
