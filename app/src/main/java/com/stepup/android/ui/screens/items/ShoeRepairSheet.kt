package com.stepup.android.ui.screens.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.stepup.android.R
import com.stepup.android.domain.ShoeRules
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunButtonKind
import com.stepup.android.ui.components.RunDivider
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.RunSheetText
import com.stepup.android.ui.components.formatSupDown
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone

/** 시트 고유 이름 — 테스트 · 캡처가 지금 어느 장면인지 본다 */
fun RepairPhase.sceneTag(): String = "repair-" + when (this) {
    RepairPhase.Loading -> "loading"
    is RepairPhase.Confirm -> when {
        changed -> "changed"
        quote.free -> "free"
        else -> "confirm"
    }
    is RepairPhase.Insufficient -> if (partial != null) "partial" else "insufficient"
    is RepairPhase.NotNeeded -> "not-needed"
    is RepairPhase.Blocked -> "blocked"
    RepairPhase.LoadFailed -> "load-failed"
    RepairPhase.SignIn -> "sign-in"
    is RepairPhase.Sending -> "sending"
    is RepairPhase.Unknown -> "unknown"
    is RepairPhase.Done -> "done"
}

private fun sup(balance: Double): String = formatSupDown(balance, 2) + " SUP"

@Composable
internal fun careBlockText(reason: CareBlock): String = stringResource(
    when (reason) {
        CareBlock.LISTED -> R.string.care_block_listed
        CareBlock.ON_CHAIN -> R.string.care_block_chain
        CareBlock.LEGACY -> R.string.care_block_legacy
        CareBlock.MISSING -> R.string.care_block_missing
        CareBlock.OTHER -> R.string.care_block_other
    },
)

/**
 * 수리 시트(RP01 ~ RP11) — 상세 위의 하단 시트 하나. 여는 것만으로 수리하지 않는다. 금액은 실제 잔고 · 서버 단가로 계산한
 * 견적이고, 처리 중의 "예상 잔액"은 완료된 잔고가 아니다. 닫기 · 뒤로는 서버 요청을 취소하지 않는다(처리 중이면 상세가 SD19 로 이어 보인다).
 */
@Composable
fun ShoeRepairSheet(
    shoe: Sneaker,
    phase: RepairPhase,
    onConfirm: () -> Unit,
    onRecheck: () -> Unit,
    onReload: () -> Unit,
    onSignIn: (() -> Unit)?,
    onClose: () -> Unit,
    /** 아직 올라가지 않은 러닝이 있다 — 금액 아래 한 줄로 알린다(수리는 그대로 할 수 있다) */
    unsentRuns: Boolean = ShoeCareStore.shared.unsentRuns.collectAsState().value,
    /** 무료 수리 규칙(경제 4판 — 몇 시간에 한 번 · 다음 무료 수리 시각) */
    rules: ShoeRules = rememberShoeRules(),
) {
    RunSheet(onDismiss = onClose, modifier = Modifier.testTag("repair-sheet"), closeTag = "repair-close") {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag(phase.sceneTag()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RepairBody(shoe, phase, onConfirm, onRecheck, onReload, onSignIn, onClose, unsentRuns, rules)
        }
    }
}

/** 무료 수리 한 줄 — 남았으면 "무료 수리 1회 남음", 아니면 "다음 무료 수리 N시간 뒤"(모르면 없음) */
@Composable
private fun FreeRepairLine(free: Boolean, rules: ShoeRules) {
    val t = runTone()
    if (free) {
        val hours = rules.freeRepairHours.roundToInt()
        Column(Modifier.fillMaxWidth().testTag("repair-free-left").semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.v4s_repair_free_left), style = runTextStyle(16.sp, t.cyan, FontWeight.Bold))
            if (hours > 0) {
                Text(stringResource(R.string.v4s_repair_free_body, hours), style = runTextStyle(14.sp, t.label, FontWeight.Medium))
            }
        }
        return
    }
    val left = freeRepairHoursLeft(rules.freeRepairNextMs, System.currentTimeMillis()) ?: return
    Text(stringResource(R.string.v4s_repair_free_next, left), style = runTextStyle(14.sp, t.label, FontWeight.Medium),
        modifier = Modifier.testTag("repair-free-next"))
}

/**
 * "수리비는 신발의 효율만큼 — 걸어서 버는 돈의 약 45%". 서버 0119: 내구도에 포인트를 넣은 신발은 가득 수리비가 그대로라 그만큼 몫이 낮다
 * ([repairShareOf] — 안내 글의 숫자일 뿐, 수리비는 서버가 준 1점 값으로 매긴다)
 */
@Composable
private fun RepairCostNote(shoe: Sneaker, rules: ShoeRules) {
    val share = (repairShareOf(shoe, rules) * 100).roundToInt()
    if (share <= 0) return
    Text(stringResource(R.string.v4s_repair_cost_note, share), style = runTextStyle(13.sp, runTone().label, FontWeight.Medium),
        modifier = Modifier.testTag("repair-cost-note"))
}

@Composable
private fun ColumnScope.RepairBody(
    shoe: Sneaker,
    phase: RepairPhase,
    onConfirm: () -> Unit,
    onRecheck: () -> Unit,
    onReload: () -> Unit,
    onSignIn: (() -> Unit)?,
    onClose: () -> Unit,
    unsentRuns: Boolean,
    rules: ShoeRules,
) {
    val t = runTone()
    // 금액을 보이는 장면의 한 줄 — 서버가 그 러닝을 받으면 닳은 만큼 금액이 바뀐다. 금액은 지금 서버 값 그대로 둔다
    val unsentNote: @Composable () -> Unit = {
        if (unsentRuns) {
            Text(stringResource(R.string.care_repair_unsent_runs), style = runTextStyle(14.sp, t.cyan, FontWeight.SemiBold),
                modifier = Modifier.testTag("repair-unsent-runs").semantics { liveRegion = LiveRegionMode.Polite })
        }
    }
    val title = stringResource(R.string.care_repair_title)
    val subtitle = shoe.nameAndNumber()
    // 마지막으로 확인한 남은 · 최대 내구도(1 = 10m) — 견적이 없는 장면(읽는 중 · 실패 · 가득 · 완료)에 보인다
    val lastNow = durabilityLeft(shoe)
    val lastMax = durabilityMax(shoe)
    // 최대 내구도를 아는 신발은 달릴 수 있는 분으로 보인다(경제 4판) — 분 = 내구도 ÷ (5 × 효율 포인트 배수)(서버 0119)
    val knowsMax = shoe.server?.hasDurabilityMax == true
    val minutes: ((Int) -> Double)? = { units: Int -> lifeMinutes(shoe, units, rules) }.takeIf { knowsMax }
    when (phase) {
        RepairPhase.Loading -> {
            CareSheetTitle(title, subtitle)
            RepairDurabilityRow(lastNow, lastMax, if (lastNow < lastMax) lastMax else null, minutesOf = minutes)
            Text(stringResource(R.string.care_repair_loading), style = runTextStyle(14.sp, t.label, FontWeight.Medium),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            RunDivider()
            CareAmountLine(stringResource(R.string.care_balance), "— SUP", "repair-balance")
            CareAmountLine(stringResource(R.string.care_after_repair), "— SUP", "repair-after")
            RunButton(stringResource(R.string.care_repair_checking_cost), {}, busy = true, modifier = Modifier.testTag("repair-confirm"))
        }
        is RepairPhase.Confirm -> {
            CareSheetTitle(title, subtitle)
            if (phase.changed) {
                Text(stringResource(R.string.care_repair_changed), style = runTextStyle(14.sp, t.cyan, FontWeight.SemiBold),
                    modifier = Modifier.testTag("repair-changed").semantics { liveRegion = LiveRegionMode.Polite })
            }
            RepairDurabilityRow(phase.quote.durabilityNow, phase.quote.durabilityMax, phase.quote.targetNow,
                minutesOf = minutes.takeIf { phase.quote.absolute })
            Text(stringResource(R.string.care_repair_full), style = runTextStyle(14.sp, t.label, FontWeight.Medium))
            FreeRepairLine(phase.quote.free, rules)
            unsentNote()
            RunDivider()
            CareAmountLine(stringResource(R.string.care_balance), sup(phase.balance), "repair-balance")
            CareAmountLine(stringResource(R.string.care_after_repair), sup(balanceAfter(phase.balance, phase.quote.cost)), "repair-after")
            if (!phase.quote.free) RepairCostNote(shoe, rules)
            RunButton(
                if (phase.quote.free) stringResource(R.string.v4s_repair_free_action)
                else stringResource(R.string.care_repair_action, formatSupExact(phase.quote.cost)),
                onConfirm,
                modifier = Modifier.testTag("repair-confirm"),
            )
            if (phase.changed) {
                RunButton(stringResource(R.string.common_cancel), onClose, kind = RunButtonKind.Secondary,
                    modifier = Modifier.testTag("repair-cancel"))
            }
        }
        is RepairPhase.Insufficient -> {
            CareSheetTitle(title, subtitle)
            if (phase.changed) {
                Text(stringResource(R.string.care_repair_changed), style = runTextStyle(14.sp, t.cyan, FontWeight.SemiBold),
                    modifier = Modifier.testTag("repair-changed").semantics { liveRegion = LiveRegionMode.Polite })
            }
            val partial = phase.partial
            RepairDurabilityRow(phase.quote.durabilityNow, phase.quote.durabilityMax, (partial ?: phase.quote).targetNow,
                minutesOf = minutes.takeIf { phase.quote.absolute })
            Text(stringResource(R.string.care_repair_full), style = runTextStyle(14.sp, t.label, FontWeight.Medium))
            FreeRepairLine(false, rules)
            unsentNote()
            RunDivider()
            CareAmountLine(stringResource(R.string.care_balance), sup(phase.balance), "repair-balance")
            Text(
                stringResource(R.string.care_short, formatSupExact(repairShortfall(phase.quote, phase.balance))),
                style = runTextStyle(15.sp, t.dangerText, FontWeight.Bold), modifier = Modifier.testTag("repair-short"),
            )
            if (partial != null) {
                // 막다른 길 대신 — 가진 SUP 만큼만 고친다(가득은 SUP 를 모은 뒤에)
                Text(
                    stringResource(R.string.v4s_repair_partial_body, formatMinutes(shoe, partial.points.toInt(), rules)),
                    style = runTextStyle(14.sp, t.text, FontWeight.SemiBold), modifier = Modifier.testTag("repair-partial-note"),
                )
                RepairCostNote(shoe, rules)
                RunButton(
                    stringResource(R.string.v4s_repair_partial_action, formatSupExact(partial.cost)), onConfirm,
                    modifier = Modifier.testTag("repair-confirm"),
                )
            } else {
                RunButton(
                    stringResource(R.string.care_repair_action, formatSupExact(phase.quote.cost)), {}, enabled = false,
                    modifier = Modifier.testTag("repair-confirm"),
                )
            }
            RunButton(stringResource(R.string.common_close), onClose, kind = RunButtonKind.Secondary,
                modifier = Modifier.testTag("repair-dismiss"))
        }
        is RepairPhase.NotNeeded -> {
            CareSheetTitle(title, subtitle)
            RepairDurabilityRow(lastNow, lastMax, null, minutesOf = minutes)
            Spacer(Modifier.height(4.dp))
            RunSheetText(stringResource(R.string.care_repair_not_needed_title), body = null,
                note = stringResource(R.string.care_repair_not_needed_body))
            unsentNote()
            RunButton(stringResource(R.string.common_close), onClose, modifier = Modifier.testTag("repair-dismiss"))
        }
        is RepairPhase.Blocked -> {
            Spacer(Modifier.height(12.dp))
            RunSheetText(stringResource(R.string.care_repair_blocked_title), note = careBlockText(phase.reason))
            Spacer(Modifier.height(8.dp))
            RunButton(stringResource(R.string.common_close), onClose, modifier = Modifier.testTag("repair-dismiss"))
        }
        RepairPhase.LoadFailed -> {
            RunSheetText(stringResource(R.string.care_repair_load_failed_title), body = subtitle,
                note = stringResource(R.string.care_repair_load_failed_body))
            RepairDurabilityRow(lastNow, lastMax, null, minutesOf = minutes)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RunButton(stringResource(R.string.common_close), onClose, kind = RunButtonKind.Secondary,
                    modifier = Modifier.weight(1f).testTag("repair-dismiss"))
                RunButton(stringResource(R.string.sdv_reload), onReload, modifier = Modifier.weight(1f).testTag("repair-reload"))
            }
        }
        RepairPhase.SignIn -> {
            Spacer(Modifier.height(12.dp))
            RunSheetText(stringResource(R.string.care_sign_in_title), note = stringResource(R.string.care_sign_in_repair))
            Spacer(Modifier.height(8.dp))
            if (onSignIn != null) {
                RunButton(stringResource(R.string.care_sign_in), onSignIn, modifier = Modifier.testTag("repair-sign-in"))
            }
            RunButton(stringResource(R.string.common_close), onClose, kind = RunButtonKind.Secondary,
                modifier = Modifier.testTag("repair-dismiss"))
        }
        is RepairPhase.Sending -> {
            CareSheetTitle(title, subtitle)
            RepairDurabilityRow(phase.quote.durabilityNow, phase.quote.durabilityMax, phase.quote.targetNow,
                minutesOf = minutes.takeIf { phase.quote.absolute })
            Text(stringResource(R.string.care_repair_sending_note), style = runTextStyle(14.sp, t.label, FontWeight.Medium),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            RunDivider()
            CareAmountLine(stringResource(R.string.care_balance_before), sup(phase.balance), "repair-before")
            CareAmountLine(stringResource(R.string.care_repair_cost), formatSupExact(phase.quote.cost) + " SUP", "repair-cost")
            CareAmountLine(stringResource(R.string.care_balance_expected), sup(balanceAfter(phase.balance, phase.quote.cost)), "repair-expected")
            RunButton(stringResource(R.string.care_repairing), {}, busy = true, modifier = Modifier.testTag("repair-confirm"))
            RunButton(stringResource(R.string.care_check_later), onClose, kind = RunButtonKind.Secondary,
                modifier = Modifier.testTag("repair-later"))
        }
        is RepairPhase.Unknown -> {
            CareSheetTitle(stringResource(R.string.care_repair_unknown_title), null)
            Text(
                stringResource(if (phase.accepted) R.string.care_repair_unknown_accepted else R.string.care_repair_unknown_body),
                style = runTextStyle(16.sp, t.cyan, FontWeight.SemiBold),
            )
            Text(stringResource(R.string.care_repair_unknown_note), style = runTextStyle(14.sp, t.label, FontWeight.Medium))
            CareInfoRow(stringResource(R.string.care_repair_state), stringResource(R.string.care_needs_check), "repair-state")
            val observed = phase.observed
            if (observed != null) {
                Text(
                    if (observed >= 100.0) stringResource(R.string.care_repair_observed_full)
                    else stringResource(R.string.care_repair_observed_short, durabilityText(shoe, rules)),
                    style = runTextStyle(14.sp, t.text, FontWeight.Medium),
                    modifier = Modifier.testTag("repair-observed").semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RunButton(stringResource(R.string.care_later), onClose, kind = RunButtonKind.Secondary,
                    modifier = Modifier.weight(1f).testTag("repair-later"))
                RunButton(stringResource(R.string.care_check_result), onRecheck, busy = phase.checking,
                    modifier = Modifier.weight(1f).testTag("repair-recheck"))
            }
        }
        is RepairPhase.Done -> {
            CareSheetTitle(stringResource(R.string.care_repair_done_title), subtitle)
            RepairDurabilityRow(lastNow, lastMax, null, minutesOf = minutes)
            Text(
                stringResource(
                    when {
                        phase.quote.partial -> R.string.v4s_repair_partial_done
                        phase.quote.free -> R.string.v4s_repair_free_done
                        else -> R.string.care_repair_done_body
                    },
                ),
                style = runTextStyle(16.sp, t.text, FontWeight.SemiBold),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            RunDivider()
            CareAmountLine(stringResource(R.string.care_repair_cost), formatSupExact(phase.quote.cost) + " SUP", "repair-spent")
            CareAmountLine(stringResource(R.string.care_balance_left), sup(phase.balance), "repair-left")
            RunButton(stringResource(R.string.care_done), onClose, modifier = Modifier.testTag("repair-done"))
        }
    }
}
