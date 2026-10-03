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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.DrawDistance
import com.stepup.android.domain.DrawStatus
import com.stepup.android.ui.components.RunDivider
import com.stepup.android.ui.components.RunNotice
import com.stepup.android.ui.components.RunNoticeKind
import com.stepup.android.ui.components.RunNumber
import com.stepup.android.ui.components.RunStateArt
import com.stepup.android.ui.components.RunTextAction
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone

/*
 * 메인 위의 안내창(DRAW03 · 04 · 05 · 06 · 08 · 16 · 17 · 19 · 23 · 30 · 31) — 남색 안내창 틀([DrawSheetFrame])에 담는다.
 * 순서는 기울인 제목 → 짧은 설명 · 상태 → 주 행동(흰 버튼) → 보조 행동(글자). ✕ · 바깥 · 뒤로는 창만 닫고, 여닫아도 기회를 쓰지 않는다.
 * 내역의 숫자는 서버가 준 **남은** 수(출처별)만 — 처음 준 수나 평생 합과 섞지 않는다. 시안의 13회 · 10회 · 0.6km 는 예시다.
 */

/** 이름 — 값 한 줄. [strong] 이면 값이 시안색 */
@Composable
private fun SheetLine(label: String, value: String, strong: Boolean = false, tag: String? = null) {
    val t = runTone()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 40.dp).semantics(mergeDescendants = true) {}
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = runTextStyle(17.sp, t.label, FontWeight.Medium), modifier = Modifier.weight(1f))
        Text(value, style = runTextStyle(19.sp, if (strong) t.cyan else t.text, FontWeight.Bold))
    }
}

/** 합계 한 줄 — "지금 사용할 수 있는 기회 13회"(값은 큰 시안색 기울인 수) */
@Composable
private fun SheetTotal(label: String, value: String, tag: String) {
    val t = runTone()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).semantics(mergeDescendants = true) {}.testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = runTextStyle(18.sp, t.text, FontWeight.SemiBold), modifier = Modifier.weight(1f))
        RunNumber(value, size = 32.sp, color = t.cyan)
    }
}

@Composable
private fun SheetBody(text: String, modifier: Modifier = Modifier, center: Boolean = false, strong: Boolean = false) {
    val t = runTone()
    Text(
        text, style = runTextStyle(if (strong) 17.sp else 15.sp, if (strong) t.text else t.label, if (strong) FontWeight.Bold else FontWeight.Medium, 1.5f),
        textAlign = if (center) TextAlign.Center else TextAlign.Start,
        modifier = modifier.fillMaxWidth(),
    )
}

/** 테두리 칸 안의 이름 · 값 — 연결 혜택 */
@Composable
private fun SheetBoxRow(label: String, value: String, tag: String? = null) {
    val t = runTone()
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 58.dp).clip(shape).background(t.inset).border(1.dp, t.panelEdge, shape)
            .padding(horizontal = 18.dp).semantics(mergeDescendants = true) {}
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = runTextStyle(16.sp, t.text, FontWeight.SemiBold), modifier = Modifier.weight(1f))
        RunNumber(value, size = 28.sp, color = t.cyan)
    }
}

@Composable
private fun SheetGroupLabel(text: String) {
    val t = runTone()
    Text(text, style = runTextStyle(18.sp, t.text, FontWeight.Bold), modifier = Modifier.padding(top = 4.dp))
}

/** "기회 받는 방법 ›" — DRAW03 · 04 에서 DRAW05 로 */
@Composable
private fun RulesLink(onRules: () -> Unit) {
    RunTextAction(
        stringResource(R.string.dv2_rules_title), onRules, Modifier.testTag("draw-sheet-rules-link"), chevron = true,
    )
}

/** 아래 두 행동 — 주 행동(흰 버튼) · 보조 행동(글자) */
@Composable
private fun ColumnScope.SheetActions(
    primary: String,
    onPrimary: () -> Unit,
    primaryTag: String,
    secondary: String? = null,
    onSecondary: () -> Unit = {},
    secondaryTag: String = "draw-sheet-close",
    primaryEnabled: Boolean = true,
    primaryIcon: Boolean = false,
) {
    DrawButton(
        primary, onPrimary, Modifier.testTag(primaryTag), enabled = primaryEnabled,
        icon = if (primaryIcon) Icons.Filled.PlayArrow else null,
    )
    if (secondary != null) {
        DrawButton(secondary, onSecondary, Modifier.testTag(secondaryTag), DrawButtonStyle.Ghost)
    }
}

@Composable
private fun count(value: Int): String = stringResource(R.string.dv2_count, value)

/** DRAW03 — 무료 뽑기 기회: 첫 가입 선물(남은 수) · 오늘 무료(남은 수) → 지금 쓸 수 있는 기회 */
@Composable
internal fun FreeChancesSheet(status: DrawStatus, canDraw: Boolean, onDraw: () -> Unit, onRules: () -> Unit, onClose: () -> Unit) {
    DrawSheetFrame(
        title = stringResource(R.string.dv2_free_sheet_title), subtitle = stringResource(R.string.dv2_free_sheet_sub),
        onDismiss = onClose, modifier = Modifier.testTag("draw-sheet-free"),
        actions = {
            SheetActions(
                stringResource(R.string.dv2_action_free), onDraw, "draw-sheet-draw",
                stringResource(R.string.dv2_close), onClose, primaryEnabled = canDraw, primaryIcon = true,
            )
        },
    ) {
        // 가입 선물이 없던 예전 계정에는 그 줄을 두지 않는다
        if (status.signupGranted > 0) SheetLine(stringResource(R.string.dv2_signup_gift), count(status.signupLeft))
        SheetLine(stringResource(R.string.dv2_daily), count(status.dailyLeft))
        RunDivider()
        SheetTotal(stringResource(R.string.dv2_free_now), count(status.freeLeft), "draw-sheet-free-total")
        RulesLink(onRules)
    }
}

/**
 * DRAW04 — 상급 뽑기 기회: 첫 연결 선물 · 러닝으로 받은 기회(남은 수) → 남은 상급 뽑기, 다음 1회까지(서버 거리).
 * DRAW30 — 오늘 러닝 지급 한도면 진행 막대 대신 알림. 이미 가진 기회는 그대로 쓸 수 있다(뽑기 버튼은 남는다).
 */
@Composable
internal fun PremiumChancesSheet(status: DrawStatus, canDraw: Boolean, onDraw: () -> Unit, onRules: () -> Unit, onClose: () -> Unit) {
    DrawSheetFrame(
        title = stringResource(R.string.dv2_premium_sheet_title),
        subtitle = if (status.walletLinked && status.runStepMeters > 0) {
            stringResource(R.string.draw_blue_premium_sheet_sub, DrawDistance.stepKm(status.runStepMeters))
        } else {
            null
        },
        onDismiss = onClose, modifier = Modifier.testTag("draw-sheet-premium"),
        actions = {
            SheetActions(
                stringResource(R.string.dv2_action_premium), onDraw, "draw-sheet-draw",
                stringResource(R.string.dv2_close), onClose, primaryEnabled = canDraw, primaryIcon = true,
            )
        },
    ) {
        SheetLine(stringResource(R.string.dv2_link_gift), count(status.giftLeft))
        SheetLine(stringResource(R.string.dv2_run_earned), count(status.runLeft))
        RunDivider()
        SheetTotal(stringResource(R.string.dv2_premium_now), count(status.premiumLeft), "draw-sheet-premium-total")
        if (status.walletLinked) {
            if (status.runCapReached) {
                // DRAW30 — 오늘 러닝 지급 한도. 남은 기회는 그대로다(0 으로 바꾸지 않는다)
                RunNotice(
                    stringResource(R.string.dv2_run_capped), body = stringResource(R.string.dv3_run_capped_sub),
                    kind = RunNoticeKind.Warn, tag = "draw-sheet-run-cap",
                )
            } else if (status.runStepMeters > 0) {
                RunProgress(status)
            }
        }
        if (status.chainPaused) RunNotice(stringResource(R.string.draw_chain_paused), kind = RunNoticeKind.Warn)
        RulesLink(onRules)
    }
}

/** 러닝 진행 — 다음 1회까지 남은 거리 · 막대 · 모은 거리 / 한 칸(모두 서버 값. 0.6 / 1km 면 60%) */
@Composable
private fun RunProgress(status: DrawStatus) {
    val t = runTone()
    val step = DrawDistance.stepKm(status.runStepMeters)
    Column(Modifier.fillMaxWidth().padding(top = 2.dp).semantics(mergeDescendants = true) {}.testTag("draw-run-progress")) {
        Text(
            stringResource(R.string.dv2_next_premium, DrawDistance.remainingKm(status.metersToNextPremium)),
            style = runTextStyle(16.sp, t.text, FontWeight.SemiBold),
        )
        DrawProgressBar(status.progressFraction, Modifier.padding(top = 10.dp))
        Text(
            stringResource(R.string.dv2_progress, DrawDistance.progressKm(status.runProgressMeters), step),
            style = runTextStyle(14.sp, t.label, FontWeight.Medium), modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** DRAW05 — 기회 받는 방법. 숫자는 서버가 준 것만(가입 선물이 없던 계정 · 이미 연결한 계정은 그 줄을 뺀다) */
@Composable
internal fun RulesSheet(status: DrawStatus, onClose: () -> Unit) {
    val step = DrawDistance.stepKm(status.runStepMeters)
    DrawSheetFrame(
        title = stringResource(R.string.dv2_rules_title), onDismiss = onClose, modifier = Modifier.testTag("draw-sheet-rules"),
        actions = { SheetActions(stringResource(R.string.dv2_ok), onClose, "draw-sheet-ok") },
    ) {
        SheetGroupLabel(stringResource(R.string.dv2_free))
        if (status.signupGranted > 0) SheetLine(stringResource(R.string.dv2_rules_signup), count(status.signupGranted))
        if (status.dailyTotal > 0) SheetLine(stringResource(R.string.dv2_rules_daily), count(status.dailyTotal))
        RunDivider()
        SheetGroupLabel(stringResource(R.string.dv2_premium))
        if (status.giftOnLink > 0) SheetLine(stringResource(R.string.dv2_rules_first_link), count(status.giftOnLink))
        if (status.runStepMeters > 0) SheetLine(stringResource(R.string.dv2_rules_run, step), stringResource(R.string.dv2_plus_one))
        SheetBody(stringResource(R.string.dv2_rules_note), modifier = Modifier.padding(top = 4.dp))
    }
}

/** DRAW06 — 연결 혜택. "WEB3 지갑 연결하기"는 기존 웹 지갑 페이지를 연다(가짜 지갑 목록 · 주소 · QR 없음) */
@Composable
internal fun WalletBenefitSheet(status: DrawStatus, onConnect: () -> Unit, onClose: () -> Unit) {
    val step = DrawDistance.stepKm(status.runStepMeters)
    DrawSheetFrame(
        title = stringResource(R.string.dv2_benefit_title), subtitle = stringResource(R.string.dv2_benefit_sub),
        onDismiss = onClose, modifier = Modifier.testTag("draw-sheet-benefit"),
        actions = {
            SheetActions(
                stringResource(R.string.dv2_connect), onConnect, "draw-sheet-connect",
                stringResource(R.string.dv2_later), onClose, secondaryTag = "draw-sheet-later",
            )
        },
    ) {
        if (status.giftOnLink > 0) SheetBoxRow(stringResource(R.string.dv2_benefit_first), count(status.giftOnLink))
        if (status.runStepMeters > 0) SheetBoxRow(stringResource(R.string.dv2_benefit_run, step), stringResource(R.string.dv2_plus_one))
        RunDivider(Modifier.padding(top = 4.dp))
        SheetBody(stringResource(R.string.draw_blue_benefit_note))
    }
}

/** DRAW08 — 돌아와 읽은 서버 값에서 연결과 첫 연결 선물 [gift] 회를 확인했다(다시 연결은 짧은 알림만, 선물 창을 띄우지 않는다) */
@Composable
internal fun WalletLinkedSheet(gift: Int, canDraw: Boolean, onDraw: () -> Unit, onClose: () -> Unit) {
    val t = runTone()
    DrawSheetFrame(
        title = stringResource(R.string.dv2_linked_title), onDismiss = onClose, modifier = Modifier.testTag("draw-sheet-linked"),
        actions = {
            SheetActions(
                stringResource(R.string.dv2_action_premium), onDraw, "draw-sheet-draw",
                stringResource(R.string.dv2_to_draw), onClose, primaryEnabled = canDraw,
            )
        },
    ) {
        Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).border(3.dp, t.cyan, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = t.cyan, modifier = Modifier.size(36.dp))
            }
            RunNumber(
                stringResource(R.string.dv2_count_plus, gift), size = 46.sp, align = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 12.dp), valueTag = "draw-sheet-linked-gift",
            )
            Text(
                stringResource(R.string.dv2_linked_body), style = runTextStyle(16.sp, t.label, FontWeight.Medium),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** DRAW16 — 무료 기회를 모두 썼다. 갱신 시각 · 소멸을 지어내지 않는다. "상급 뽑기 보기"는 같은 화면의 상급 패널로 */
@Composable
internal fun FreeEmptySheet(status: DrawStatus, onSeePremium: () -> Unit, onClose: () -> Unit) {
    DrawSheetFrame(
        title = stringResource(R.string.dv2_free_empty_title), onDismiss = onClose, modifier = Modifier.testTag("draw-sheet-free-empty"),
        actions = {
            SheetActions(
                stringResource(R.string.dv2_ok), onClose, "draw-sheet-ok",
                stringResource(R.string.dv2_see_premium), onSeePremium, secondaryTag = "draw-sheet-see-premium",
            )
        },
    ) {
        if (status.dailyTotal > 0) SheetBody(stringResource(R.string.draw_blue_free_empty_daily, status.dailyTotal), strong = true)
        SheetBody(stringResource(R.string.dv2_free_empty_body))
        Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
            RunStateArt(Icons.Outlined.ConfirmationNumber, size = 104.dp)
        }
    }
}

/** DRAW17 — 러닝으로 기회 받기(연결됨 · 상급 기회 없음). "러닝 시작"은 기존 자유 러닝 시작 흐름. 여기서 거리를 쌓거나 지급하지 않는다 */
@Composable
internal fun RunChancesSheet(status: DrawStatus, onStartRun: () -> Unit, onClose: () -> Unit) {
    val t = runTone()
    val step = DrawDistance.stepKm(status.runStepMeters)
    DrawSheetFrame(
        title = stringResource(R.string.dv2_run_title), subtitle = stringResource(R.string.draw_blue_run_sub),
        onDismiss = onClose, modifier = Modifier.testTag("draw-sheet-run"),
        actions = {
            SheetActions(stringResource(R.string.dv2_run_start), onStartRun, "draw-sheet-run-start", stringResource(R.string.dv2_close), onClose)
        },
    ) {
        if (status.runCapReached) {
            RunNotice(
                stringResource(R.string.dv2_run_capped), body = stringResource(R.string.dv3_run_capped_sub),
                kind = RunNoticeKind.Warn, tag = "draw-sheet-run-cap",
            )
        } else if (status.runStepMeters > 0) {
            Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
                Text(stringResource(R.string.dv2_run_until), style = runTextStyle(15.sp, t.text, FontWeight.Medium))
                RunNumber(
                    stringResource(R.string.dv2_km, DrawDistance.remainingKm(status.metersToNextPremium)), size = 48.sp, color = t.cyan,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            DrawProgressBar(status.progressFraction)
            Text(
                stringResource(R.string.dv2_progress, DrawDistance.progressKm(status.runProgressMeters), step),
                style = runTextStyle(14.sp, t.label, FontWeight.Medium),
            )
            SheetBody(stringResource(R.string.draw_blue_run_rule, step))
        }
    }
}

/**
 * DRAW19 — 뽑기가 시작되지 않았고 기회를 쓰지 않은 것이 확실할 때만(서버 거절 · 보내지 못함 · 확인해 보니 처리 안 됨).
 * "다시 뽑기"는 다시 해 볼 만한 까닭일 때만. DRAW31 — 발행 한도는 "지금은 뽑을 수 없어요"와 확인 하나(다시 시도 없음).
 * 통신 시간 초과 · 응답 유실은 이 창이 아니라 결과 확인(DRAW20)으로 간다.
 */
@Composable
internal fun NotStartedSheet(stop: DrawStop, canRetry: Boolean, onRetry: () -> Unit, onClose: () -> Unit) {
    val t = runTone()
    val blocked = stop == DrawStop.MintLimit
    DrawSheetFrame(
        title = null, onDismiss = onClose, modifier = Modifier.testTag("draw-sheet-not-started"),
        actions = {
            if (canRetry) {
                SheetActions(stringResource(R.string.dv2_retry_draw), onRetry, "draw-sheet-retry", stringResource(R.string.dv2_close), onClose)
            } else {
                SheetActions(stringResource(R.string.dv2_ok), onClose, "draw-sheet-ok")
            }
        },
    ) {
        Column(
            Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RunStateArt(if (blocked) Icons.Outlined.Info else Icons.Outlined.Sync, size = 92.dp)
            Text(
                stringResource(if (blocked) R.string.dv3_stop_title_blocked else R.string.dv2_stop_title),
                style = drawTitleStyle(25.sp, t.text), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.dv2_stop_unused), style = runTextStyle(17.sp, t.text, FontWeight.SemiBold),
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
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
                center = true,
            )
        }
    }
}

/** DRAW23 — 횟수를 불러오지 못했다(소진과 다르다). 닫으면 메인에 "—"와 "다시 불러오기"가 남는다(DRAW32) */
@Composable
internal fun LoadFailedSheet(onReload: () -> Unit, onBack: () -> Unit, onClose: () -> Unit) {
    DrawSheetFrame(
        title = stringResource(R.string.dv2_load_failed_title), onDismiss = onClose, modifier = Modifier.testTag("draw-sheet-load-failed"),
        actions = {
            SheetActions(
                stringResource(R.string.dv2_action_reload), onReload, "draw-sheet-reload",
                stringResource(R.string.dv2_back_to_shoes), onBack, secondaryTag = "draw-sheet-back",
            )
        },
    ) {
        SheetBody(stringResource(R.string.dv2_load_failed_body))
        Spacer(Modifier.height(4.dp))
    }
}
