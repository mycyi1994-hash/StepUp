package com.stepup.android.ui.screens.gacha

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Inventory2
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.BoxStatus
import com.stepup.android.ui.components.ButtonState
import com.stepup.android.ui.components.RunDivider
import com.stepup.android.ui.components.RunNotice
import com.stepup.android.ui.components.RunNoticeKind
import com.stepup.android.ui.components.SmallButton
import com.stepup.android.ui.components.formatSupDown
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.feedbackClickable

/*
 * 상자 받는 곳(경제 4판 · 서버 0116) — 뽑기 메인의 한 줄([BoxSourcesEntry])과 시트([BoxSourcesSheet]).
 * 숫자 · 값 · 잔고는 모두 서버 `box_status` 값이다. 사기는 확인 한 번 뒤에만 보내고, 결과도 서버가 다시 준 현황으로만 보인다.
 * 시트의 주 행동은 "확인" 하나 — 사기는 칸마다 작은 버튼이다.
 */

private fun steps(n: Int): String = "%,d".format(n)

private fun sup(value: Double): String = formatSupDown(value, if (value % 1.0 == 0.0) 0 else 2)

/** 뽑기 메인의 "상자 받는 곳 ›" — 오늘 러닝 상자 · 이번 주 걸음(서버 값) 한 줄. 누르면 시트 */
@Composable
internal fun BoxSourcesEntry(boxes: BoxStatus, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val t = runTone()
    val shape = RoundedCornerShape(16.dp)
    val runs = boxes.runBoxViews()
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp).clip(shape).background(t.panel).border(1.dp, t.panelEdge, shape)
            .feedbackClickable(role = Role.Button, onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = t.cyan, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.boxes_title), style = runTextStyle(16.sp, t.text, FontWeight.Bold))
            val summary = buildList {
                if (runs.isNotEmpty()) add(stringResource(R.string.boxes_entry_runs, runs.count { it.got }, runs.size))
                if (boxes.weekTarget > 0) {
                    add(
                        if (boxes.weekGot) stringResource(R.string.boxes_entry_week_got)
                        else stringResource(R.string.boxes_entry_week, steps(boxes.stepsWeek), steps(boxes.weekTarget)),
                    )
                }
            }.joinToString(" · ")
            if (summary.isNotEmpty()) {
                Text(
                    summary, style = runTextStyle(13.sp, t.label, FontWeight.Medium), maxLines = 2,
                    modifier = Modifier.padding(top = 2.dp).testTag("draw-boxes-summary"),
                )
            }
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = t.label, modifier = Modifier.size(20.dp))
    }
}

/**
 * 상자 받는 곳 시트. [step] 이 확인 · 보내는 중이면 확인 창으로 바뀐다(보내는 동안은 닫히지 않는다).
 * 현황을 아직 못 읽었으면([boxes] = null) 불러오는 중 · 못 읽음과 다시 불러오기.
 */
@Composable
internal fun BoxSourcesSheet(
    boxes: BoxStatus?,
    step: BoxStep,
    loading: Boolean,
    failed: Boolean,
    onAsk: (BoxKind) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onReload: () -> Unit,
    onClose: () -> Unit,
) {
    when (step) {
        is BoxStep.Confirm -> if (boxes != null) {
            BoxConfirmSheet(boxes, step.kind, retry = step.retry, buying = false, onConfirm = onConfirm, onCancel = onCancel)
            return
        }
        is BoxStep.Buying -> if (boxes != null) {
            BoxConfirmSheet(boxes, step.kind, retry = false, buying = true, onConfirm = {}, onCancel = {})
            return
        }
        is BoxStep.Home -> Unit
    }
    DrawSheetFrame(
        title = stringResource(R.string.boxes_title), subtitle = stringResource(R.string.boxes_sub),
        onDismiss = onClose, modifier = Modifier.testTag("draw-sheet-boxes"),
        actions = {
            if (boxes == null && failed) {
                SheetActions(
                    stringResource(R.string.dv2_action_reload), onReload, "boxes-reload",
                    stringResource(R.string.dv2_close), onClose,
                )
            } else {
                SheetActions(stringResource(R.string.dv2_ok), onClose, "boxes-ok")
            }
        },
    ) {
        if (boxes == null) {
            SheetBody(stringResource(if (failed && !loading) R.string.boxes_failed else R.string.boxes_loading))
            return@DrawSheetFrame
        }
        (step as? BoxStep.Home)?.note?.let { BoxNoteLine(it, boxes) }

        // 일반 상자 — 출석 · 러닝 · 사기
        SheetGroupLabel(stringResource(R.string.boxes_normal))
        if (boxes.dailyFree > 0) SheetLine(stringResource(R.string.boxes_check_in), stringResource(R.string.boxes_per_day, boxes.dailyFree))
        boxes.runBoxViews().forEach { run ->
            ProgressLine(
                label = stringResource(R.string.boxes_run, steps(run.steps)),
                value = if (run.got) stringResource(R.string.boxes_got) else stringResource(R.string.boxes_steps_progress, steps(run.progress), steps(run.steps)),
                got = run.got, fraction = run.fraction, tag = "boxes-run-${run.steps}",
            )
        }
        BuyLine(boxes.buyView(BoxKind.NORMAL), onAsk)

        RunDivider(Modifier.padding(vertical = 2.dp))

        // 상급 상자 — 주간 걸음 · 연속 목표 · 신발 레벨 · 사기 · 가진 수
        SheetGroupLabel(stringResource(R.string.boxes_premium))
        if (boxes.weekTarget > 0) {
            ProgressLine(
                label = stringResource(R.string.boxes_week, steps(boxes.weekTarget)),
                value = if (boxes.weekGot) stringResource(R.string.boxes_got)
                else stringResource(R.string.boxes_steps_progress, steps(boxes.stepsWeek.coerceAtMost(boxes.weekTarget)), steps(boxes.weekTarget)),
                got = boxes.weekGot, fraction = boxes.weekFraction, tag = "boxes-week",
            )
        }
        if (boxes.streakDays > 0) {
            SheetLine(stringResource(R.string.boxes_streak, boxes.streakDays), stringResource(R.string.boxes_streak_value, boxes.streakDays))
        }
        SheetLine(stringResource(R.string.boxes_levels), stringResource(R.string.boxes_levels_value), tag = "boxes-levels")
        if (boxes.levelsGot.isNotEmpty()) {
            SheetBody(
                stringResource(R.string.boxes_levels_got, boxes.levelsGot.sorted().joinToString(" · ") { "Lv$it" }),
                modifier = Modifier.testTag("boxes-levels-got"),
            )
        }
        BuyLine(boxes.buyView(BoxKind.PREMIUM), onAsk)
        SheetLine(
            stringResource(R.string.boxes_premium_have), stringResource(R.string.boxes_count, boxes.giftLeft.coerceAtLeast(0)),
            strong = true, tag = "boxes-premium-have",
        )
        SheetBody(stringResource(R.string.boxes_premium_have_note))

        RunDivider(Modifier.padding(vertical = 2.dp))
        SheetLine(stringResource(R.string.boxes_balance), stringResource(R.string.price_sup, sup(boxes.balance)), tag = "boxes-balance")
    }
}

/** 방금 산 결과 · 거절 까닭 한 줄 */
@Composable
private fun BoxNoteLine(note: BoxNote, boxes: BoxStatus) {
    when (note) {
        is BoxNote.Bought -> RunNotice(
            stringResource(if (note.kind == BoxKind.NORMAL) R.string.boxes_bought_normal else R.string.boxes_bought_premium),
            kind = RunNoticeKind.Info, tag = "boxes-bought",
        )
        is BoxNote.Refused -> RunNotice(
            when (note.why) {
                BoxRefusal.NOT_ENOUGH -> stringResource(R.string.boxes_refused_not_enough)
                BoxRefusal.LIMIT -> if (note.kind == BoxKind.NORMAL) stringResource(R.string.boxes_refused_limit_day, boxes.normalLimit)
                else stringResource(R.string.boxes_refused_limit_week, boxes.premiumLimit)
                BoxRefusal.NOT_SELLING -> stringResource(R.string.boxes_block_selling)
                BoxRefusal.SIGN_IN -> stringResource(R.string.boxes_refused_sign_in)
                BoxRefusal.OFFLINE, BoxRefusal.OTHER -> stringResource(R.string.boxes_refused_other)
            },
            kind = RunNoticeKind.Warn, tag = "boxes-refused",
        )
    }
}

/** 이름 — 값 한 줄과 아래 막대(받았으면 가득) */
@Composable
private fun ProgressLine(label: String, value: String, got: Boolean, fraction: Float, tag: String) {
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.testTag(tag)) {
        SheetLine(label, value, strong = got)
        DrawProgressBar(if (got) 1f else fraction, Modifier.padding(bottom = 4.dp))
    }
}

/** 사기 칸 — "SUP 로 사기 · 40 SUP", 아래 오늘(이번 주) 산 수 또는 못 사는 까닭, 오른쪽 작은 "사기" */
@Composable
private fun BuyLine(view: BoxBuyView, onAsk: (BoxKind) -> Unit) {
    val t = runTone()
    val key = if (view.kind == BoxKind.NORMAL) "normal" else "premium"
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("boxes-buy-$key"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
            Text(
                stringResource(R.string.boxes_buy_line, sup(view.price)),
                style = runTextStyle(17.sp, t.label, FontWeight.Medium),
            )
            val sub = when (view.block) {
                BoxBuyBlock.NOT_SELLING -> stringResource(R.string.boxes_block_selling)
                BoxBuyBlock.LIMIT -> stringResource(if (view.kind == BoxKind.NORMAL) R.string.boxes_block_limit_day else R.string.boxes_block_limit_week)
                BoxBuyBlock.BALANCE -> stringResource(R.string.boxes_block_balance)
                null -> stringResource(
                    if (view.kind == BoxKind.NORMAL) R.string.boxes_buy_today else R.string.boxes_buy_week, view.bought, view.limit,
                )
            }
            Text(sub, style = runTextStyle(14.sp, if (view.block == null) t.label else t.text, FontWeight.Medium),
                modifier = Modifier.padding(top = 2.dp).testTag("boxes-buy-$key-sub"))
        }
        if (view.block != BoxBuyBlock.NOT_SELLING) {
            SmallButton(
                stringResource(R.string.boxes_buy), { onAsk(view.kind) },
                Modifier.testTag("boxes-buy-$key-action"),
                kind = if (view.canBuy) ButtonState.Idle else ButtonState.Disabled,
            )
        }
    }
}

/**
 * "N SUP 로 살까요?" — 확인을 눌러야 보낸다. [buying] 이면 버튼이 도는 표시이고 닫히지 않는다.
 * [retry] 면 앞 요청의 답을 받지 못했다 — 다시 눌러도 한 번만 산다(같은 요청 번호).
 */
@Composable
private fun BoxConfirmSheet(
    boxes: BoxStatus,
    kind: BoxKind,
    retry: Boolean,
    buying: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val view = boxes.buyView(kind)
    val price = sup(view.price)
    DrawSheetFrame(
        title = stringResource(if (kind == BoxKind.NORMAL) R.string.boxes_confirm_title_normal else R.string.boxes_confirm_title_premium),
        onDismiss = { if (!buying) onCancel() },
        modifier = Modifier.testTag("draw-sheet-box-confirm"),
        actions = {
            DrawButton(
                if (retry) stringResource(R.string.boxes_retry) else stringResource(R.string.boxes_confirm_buy, price),
                onConfirm, Modifier.testTag("boxes-confirm"), loading = buying,
            )
            DrawButton(stringResource(R.string.common_cancel), onCancel, Modifier.testTag("boxes-cancel"), DrawButtonStyle.Ghost, enabled = !buying)
        },
    ) {
        SheetBody(
            stringResource(if (kind == BoxKind.NORMAL) R.string.boxes_confirm_body_normal else R.string.boxes_confirm_body_premium, price),
            strong = true,
        )
        SheetLine(
            stringResource(if (kind == BoxKind.NORMAL) R.string.boxes_buy_today_label else R.string.boxes_buy_week_label),
            stringResource(R.string.boxes_of, view.bought, view.limit),
        )
        SheetLine(stringResource(R.string.boxes_balance), stringResource(R.string.price_sup, sup(boxes.balance)))
        if (retry) {
            RunNotice(stringResource(R.string.boxes_retry_note), kind = RunNoticeKind.Warn, tag = "boxes-retry-note")
        }
    }
}
