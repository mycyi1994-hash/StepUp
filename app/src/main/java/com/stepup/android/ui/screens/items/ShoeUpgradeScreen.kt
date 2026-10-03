package com.stepup.android.ui.screens.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.tier
import com.stepup.android.ui.components.DetailPage
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunButtonKind
import com.stepup.android.ui.components.RunDivider
import com.stepup.android.ui.components.RunNotice
import com.stepup.android.ui.components.RunNoticeKind
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.RunSheetText
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.RunTextAction
import com.stepup.android.ui.components.ShoeNameWithBadge
import com.stepup.android.ui.components.SneakerGradeStage
import com.stepup.android.ui.components.formatSupDown
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.theme.StepUpSans

/** 강화 화면 위의 시트 — 최종 확인(UP04) · 보내기 전 연결 없음(UP14) · 로그인(서버 거절) */
private enum class UpgradeSheet { None, Confirm, Offline }

/** 지금 보이는 강화 장면 — 테스트 · 캡처가 읽는다 */
private fun sceneOf(shoe: Sneaker, phase: UpgradePhase?): String = when {
    phase is UpgradePhase.Sending -> "upgrade-sending"
    phase is UpgradePhase.Success -> "upgrade-success"
    phase is UpgradePhase.Unknown -> "upgrade-unknown"
    phase is UpgradePhase.Rejected && phase.reason == UpgradeRejection.MAX_LEVEL -> "upgrade-max"
    phase is UpgradePhase.Rejected && phase.reason !in setOf(UpgradeRejection.NOT_ENOUGH_BALANCE, UpgradeRejection.SIGN_IN) ->
        "upgrade-blocked"
    upgradeBlockOf(shoe) != null -> "upgrade-blocked"
    atMaxLevel(shoe) -> "upgrade-max"
    else -> "upgrade-ready"
}

private fun rejectionBlock(reason: UpgradeRejection): CareBlock = when (reason) {
    UpgradeRejection.LISTED -> CareBlock.LISTED
    UpgradeRejection.ON_CHAIN -> CareBlock.ON_CHAIN
    UpgradeRejection.LEGACY -> CareBlock.LEGACY
    UpgradeRejection.MISSING -> CareBlock.MISSING
    else -> CareBlock.OTHER
}

/**
 * 신발 강화 — 신발 상세의 "강화하기"로 여는 독립 화면(같은 상세 경로 안에서 화면 전체를 바꾼다. 뒤로 가기는 상세로).
 *
 * 경제는 지금 서버 계약 그대로다: SUP 를 내고 레벨 +1(sneaker_upgrade p_id). 전달본의 재료 신발 3개 · 성공률 · 실패 소각은
 * 서버에 없는 계약이라 재료 칸 · 성공률 · 소각 안내를 그리지 않는다(UP02 · 07 · 08 ~ 12 · 17 은 미연결 — tracker/upgrade.csv).
 * 성공률 설명 창(UP16)은 만들지 않는다. 값은 실제 신발 · 잔고에서 오고 결과는 서버가 돌려준 값으로만 그린다.
 */
@Composable
fun ShoeUpgradeContent(
    state: ShoeDetailState,
    balance: Double?,
    phase: UpgradePhase?,
    onBack: () -> Unit,
    onOpenOwned: () -> Unit,
    onStart: (Sneaker) -> Unit,
    onRecheck: () -> Unit,
    onReset: () -> Unit,
    onSignIn: (() -> Unit)?,
    isOnline: () -> Boolean = { true },
    initialSheet: String? = null,
) {
    var sheet by rememberSaveable { mutableStateOf(initialSheet?.let(UpgradeSheet::valueOf) ?: UpgradeSheet.None) }
    var stillOffline by rememberSaveable { mutableStateOf(false) }
    val shoe = (state as? ShoeDetailState.Ready)?.shoe
    val scene = when {
        shoe != null -> sceneOf(shoe, phase)
        state == ShoeDetailState.Loading -> "upgrade-loading"
        else -> "upgrade-missing"
    }
    val title = stringResource(R.string.care_upgrade_title)

    val bottom: (@Composable ColumnScope.() -> Unit)? = when (scene) {
        "upgrade-ready" -> {
            {
                val cost = shoe!!.upgradeCost
                val enough = balance != null && balance >= cost
                RunButton(
                    when {
                        balance == null -> stringResource(R.string.care_balance_checking)
                        !enough -> stringResource(R.string.care_upgrade_short_action)
                        else -> stringResource(R.string.care_upgrade_open)
                    },
                    { sheet = UpgradeSheet.Confirm },
                    enabled = enough, busy = balance == null,
                    modifier = Modifier.testTag("upgrade-primary"),
                )
            }
        }
        "upgrade-sending" -> {
            {
                RunButton(stringResource(R.string.care_upgrading), {}, enabled = false, modifier = Modifier.testTag("upgrade-primary"))
                RunTextAction(stringResource(R.string.care_check_later), onBack,
                    Modifier.align(Alignment.CenterHorizontally).testTag("upgrade-later"))
            }
        }
        "upgrade-success" -> {
            {
                val after = (phase as UpgradePhase.Success).after
                RunButton(stringResource(R.string.care_view_detail), onBack, modifier = Modifier.testTag("upgrade-primary"))
                if (upgradeBlockOf(after) == null && !atMaxLevel(after)) {
                    RunButton(stringResource(R.string.care_next_level), onReset, kind = RunButtonKind.Secondary,
                        modifier = Modifier.testTag("upgrade-next"))
                }
            }
        }
        "upgrade-unknown" -> {
            {
                val unknown = phase as UpgradePhase.Unknown
                RunButton(stringResource(R.string.care_recheck), onRecheck, busy = unknown.checking,
                    modifier = Modifier.testTag("upgrade-primary"))
                RunButton(stringResource(R.string.care_check_later), onBack, kind = RunButtonKind.Secondary,
                    modifier = Modifier.testTag("upgrade-later"))
            }
        }
        "upgrade-max" -> {
            { RunButton(stringResource(R.string.care_view_detail), onBack, modifier = Modifier.testTag("upgrade-primary")) }
        }
        "upgrade-blocked", "upgrade-missing" -> {
            { RunButton(stringResource(R.string.care_view_owned), onOpenOwned, modifier = Modifier.testTag("upgrade-primary")) }
        }
        else -> null
    }

    DetailPage(title = title, onBack = onBack, trailing = { Spacer(Modifier.size(48.dp)) }, bottomBar = bottom) {
        item(key = scene) {
            Column(Modifier.fillMaxWidth().testTag(scene), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (scene) {
                    "upgrade-loading" -> Column(Modifier.fillMaxWidth().padding(top = 120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        RunSpinner(Modifier.size(40.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.sdv_loading), style = runTextStyle(15.sp, runTone().label, FontWeight.Medium))
                    }
                    "upgrade-missing" -> Blocked(null, CareBlock.MISSING, rejected = false)
                    "upgrade-ready" -> Ready(shoe!!, balance, phase as? UpgradePhase.Rejected, onReset)
                    "upgrade-sending" -> Sending((phase as UpgradePhase.Sending).before)
                    "upgrade-success" -> Success(phase as UpgradePhase.Success)
                    "upgrade-unknown" -> Unknown(phase as UpgradePhase.Unknown, shoe!!, onReset)
                    "upgrade-max" -> Max(shoe!!)
                    "upgrade-blocked" -> {
                        val rejected = phase as? UpgradePhase.Rejected
                        Blocked(shoe, rejected?.let { rejectionBlock(it.reason) } ?: upgradeBlockOf(shoe!!) ?: CareBlock.OTHER,
                            rejected = rejected != null)
                    }
                }
            }
        }
    }

    if (shoe != null && scene == "upgrade-ready") {
        when (sheet) {
            UpgradeSheet.Confirm -> ConfirmSheet(
                shoe = shoe, balance = balance,
                onCancel = { sheet = UpgradeSheet.None },
                onStart = {
                    sheet = UpgradeSheet.None
                    if (isOnline()) onStart(shoe) else {
                        stillOffline = false
                        sheet = UpgradeSheet.Offline
                    }
                },
            )
            UpgradeSheet.Offline -> OfflineSheet(
                stillOffline = stillOffline,
                onBack = { sheet = UpgradeSheet.None },
                onRecheck = {
                    // 연결이 돌아와도 자동으로 강화하지 않는다 — 준비 화면으로 돌아갈 뿐
                    if (isOnline()) sheet = UpgradeSheet.None else stillOffline = true
                },
            )
            UpgradeSheet.None -> Unit
        }
        if ((phase as? UpgradePhase.Rejected)?.reason == UpgradeRejection.SIGN_IN) {
            RunSheet(onDismiss = onReset, modifier = Modifier.testTag("upgrade-sign-in")) {
                RunSheetText(stringResource(R.string.care_sign_in_title), note = stringResource(R.string.care_sign_in_upgrade))
                Spacer(Modifier.height(20.dp))
                if (onSignIn != null) {
                    RunButton(stringResource(R.string.care_sign_in), onSignIn, modifier = Modifier.testTag("upgrade-sign-in-go"))
                    Spacer(Modifier.height(10.dp))
                }
                RunButton(stringResource(R.string.common_close), onReset, kind = RunButtonKind.Secondary)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    val t = runTone()
    Text(
        text,
        style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, color = t.text,
            letterSpacing = (-0.02).em),
        modifier = Modifier.padding(top = 4.dp).semantics { heading() },
    )
}

private fun sup(balance: Double): String = formatSupDown(balance, 2) + " SUP"

/** UP01 · UP03 에 해당하는 준비 화면 — 지금 계약(SUP)의 비용 · 잔고와 강화 뒤 세 줄 미리보기 */
@Composable
private fun Ready(shoe: Sneaker, balance: Double?, rejected: UpgradePhase.Rejected?, onDismissNotice: () -> Unit) {
    val t = runTone()
    CareShoeHeader(shoe, levelText = stringResource(R.string.level_chip, shoe.level) + " / " + shoe.maxLevel)
    SectionTitle(stringResource(R.string.care_upgrade_after))
    UpgradeStatRows(upgradeRows(shoe, upgradePreview(shoe)), shoe.maxLevel, Modifier.testTag("upgrade-rows"))
    RunDivider()
    val cost = shoe.upgradeCost
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CareAmountLine(stringResource(R.string.care_upgrade_cost), formatSupExact(cost) + " SUP", "upgrade-cost", accent = t.cyan)
        CareAmountLine(stringResource(R.string.care_balance), balance?.let(::sup) ?: "— SUP", "upgrade-balance")
        if (balance != null && balance >= cost) {
            CareAmountLine(stringResource(R.string.care_after_upgrade), sup(balance - cost), "upgrade-after")
        }
    }
    if (balance != null && balance < cost) {
        Text(
            stringResource(R.string.care_short, formatSupExact(cost - balance)),
            style = runTextStyle(15.sp, t.dangerText, FontWeight.Bold), modifier = Modifier.testTag("upgrade-short"),
        )
    }
    if (rejected?.reason == UpgradeRejection.NOT_ENOUGH_BALANCE) {
        RunNotice(
            stringResource(R.string.care_not_enough_upgrade), kind = RunNoticeKind.Error,
            body = stringResource(R.string.care_no_charge),
            action = stringResource(R.string.common_close), onAction = onDismissNotice, tag = "upgrade-rejected",
        )
    }
}

@Composable
private fun CenteredShoe(shoe: Sneaker, levelText: String) {
    val t = runTone()
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        SneakerGradeStage(shoe, Modifier.widthIn(max = 260.dp).fillMaxWidth(0.66f))
        Spacer(Modifier.height(12.dp))
        ShoeNameWithBadge(
            name = shoe.shoeName(), tier = shoe.tier,
            style = TextStyle(color = t.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 1.25.em),
            textAlign = TextAlign.Center, maxLines = 2,
            suffix = formatShoeNumber(shoe.mintNumber),
            suffixStyle = TextStyle(color = t.label, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
            modifier = Modifier.fillMaxWidth().testTag("care-shoe-name"),
        )
        Spacer(Modifier.height(6.dp))
        Text(levelText, style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = t.cyan),
            modifier = Modifier.testTag("care-shoe-level"))
    }
}

@Composable
private fun Headline(title: String, body: String?, kicker: String? = null) {
    val t = runTone()
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally) {
        if (kicker != null) {
            Text(kicker, style = runTextStyle(16.sp, t.cyan, FontWeight.Bold))
            Spacer(Modifier.height(4.dp))
        }
        Text(
            title, textAlign = TextAlign.Center,
            style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 1.25.em,
                letterSpacing = (-0.02).em, color = t.text),
            modifier = Modifier.semantics { heading() }.testTag("upgrade-headline"),
        )
        if (body != null) {
            Spacer(Modifier.height(8.dp))
            Text(body, style = runTextStyle(15.sp, t.label, FontWeight.Medium, 1.45f), textAlign = TextAlign.Center)
        }
    }
}

/** UP05 — 보냈다. 작은 도는 표시만, 타이머로 결과를 만들지 않는다 */
@Composable
private fun Sending(before: Sneaker) {
    CenteredShoe(before, stringResource(R.string.level_chip, before.level))
    Headline(stringResource(R.string.care_upgrading_title), stringResource(R.string.care_upgrading_body))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { RunSpinner(Modifier.size(44.dp)) }
}

/** UP06 — 서버가 돌려준 실제 레벨 · 능력치 */
@Composable
private fun Success(phase: UpgradePhase.Success) {
    val after = phase.after
    Headline(
        stringResource(R.string.care_upgrade_done_title, after.level),
        if (phase.observed) stringResource(R.string.care_upgrade_observed) else null,
        kicker = stringResource(R.string.care_upgrade_done_kicker),
    )
    CareShoeHeader(after, levelText = stringResource(R.string.level_chip, after.level) + " / " + after.maxLevel)
    UpgradeStatRows(upgradeRows(phase.before, after), after.maxLevel, Modifier.testTag("upgrade-rows"))
}

/** UP15 — 결과를 모른다. 새 강화를 보내지 않고 같은 신발의 값을 다시 읽기만 한다 */
@Composable
private fun Unknown(phase: UpgradePhase.Unknown, latest: Sneaker, onReset: () -> Unit) {
    val t = runTone()
    CareShoeHeader(phase.before)
    Headline(
        stringResource(R.string.care_upgrade_unknown_title),
        stringResource(if (phase.accepted) R.string.care_upgrade_unknown_accepted else R.string.care_upgrade_unknown_body),
    )
    Text(stringResource(R.string.care_last_record), style = runTextStyle(15.sp, t.text, FontWeight.Bold))
    UpgradeStatRows(upgradeRows(latest, null), latest.maxLevel, Modifier.testTag("upgrade-rows"), previewBars = false)
    RunNotice(stringResource(R.string.care_recheck_note), tag = "upgrade-recheck-note")
    if (phase.synced && !phase.accepted) {
        Text(stringResource(R.string.care_upgrade_unchanged), style = runTextStyle(14.sp, t.text, FontWeight.Medium),
            modifier = Modifier.testTag("upgrade-unchanged"))
        RunTextAction(stringResource(R.string.care_back_to_upgrade), onReset, Modifier.testTag("upgrade-reset"), chevron = true)
    }
}

/** UP13 — 지금 상한(서버가 준 maxLevel)에 닿았다. 상한 숫자를 20 으로 바꾸지 않는다 */
@Composable
private fun Max(shoe: Sneaker) {
    CareShoeHeader(shoe, levelText = stringResource(R.string.level_chip, shoe.level) + " / " + shoe.maxLevel)
    Headline(stringResource(R.string.care_upgrade_max_title), stringResource(R.string.care_upgrade_max_body))
    UpgradeStatRows(upgradeRows(shoe, null), shoe.maxLevel, Modifier.testTag("upgrade-rows"), previewBars = false)
}

/** UP18 — 확인된 까닭으로 강화할 수 없다. 서버가 거절했으면 SUP 는 나가지 않았다 */
@Composable
private fun Blocked(shoe: Sneaker?, reason: CareBlock, rejected: Boolean) {
    if (shoe != null) CenteredShoe(shoe, stringResource(R.string.level_chip, shoe.level))
    Headline(stringResource(R.string.care_upgrade_blocked_title), careBlockText(reason))
    if (rejected) RunNotice(stringResource(R.string.care_no_charge), tag = "upgrade-no-charge")
}

/** UP04 — 실행 전 마지막 확인. 이 단계에서만 서버에 보낸다 */
@Composable
private fun ConfirmSheet(shoe: Sneaker, balance: Double?, onCancel: () -> Unit, onStart: () -> Unit) {
    val t = runTone()
    val cost = shoe.upgradeCost
    RunSheet(onDismiss = onCancel, modifier = Modifier.testTag("upgrade-confirm-sheet")) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            RunSheetText(stringResource(R.string.care_confirm_title))
            CareShoeHeader(shoe, stageWidth = 120.dp, levelText = stringResource(R.string.care_confirm_level, shoe.level, shoe.level + 1))
            RunDivider()
            CareAmountLine(stringResource(R.string.care_upgrade_cost), formatSupExact(cost) + " SUP", "upgrade-confirm-cost", accent = t.cyan)
            if (balance != null) {
                CareAmountLine(stringResource(R.string.care_balance), sup(balance), "upgrade-confirm-balance")
                CareAmountLine(stringResource(R.string.care_after_upgrade), sup(balance - cost), "upgrade-confirm-after")
            }
            Text(stringResource(R.string.care_confirm_note, formatSupExact(cost)),
                style = runTextStyle(14.sp, t.label, FontWeight.Medium), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RunButton(stringResource(R.string.common_cancel), onCancel, kind = RunButtonKind.Secondary,
                    modifier = Modifier.weight(0.4f).testTag("upgrade-cancel"))
                RunButton(stringResource(R.string.care_start), onStart, modifier = Modifier.weight(0.6f).testTag("upgrade-start"))
            }
        }
    }
}

/** UP14 — 연결이 없어 요청을 보내지 않았다(보내기 전에 확인). 연결이 돌아와도 자동으로 강화하지 않는다 */
@Composable
private fun OfflineSheet(stillOffline: Boolean, onBack: () -> Unit, onRecheck: () -> Unit) {
    val t = runTone()
    RunSheet(onDismiss = onBack, modifier = Modifier.testTag("upgrade-offline-sheet")) {
        Icon(Icons.Filled.WifiOff, contentDescription = null, tint = t.dangerText, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        RunSheetText(stringResource(R.string.care_offline_title), body = stringResource(R.string.care_offline_body),
            note = stringResource(R.string.care_offline_note))
        if (stillOffline) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.care_still_offline), style = runTextStyle(14.sp, t.dangerText, FontWeight.SemiBold),
                modifier = Modifier.testTag("upgrade-still-offline").semantics { liveRegion = LiveRegionMode.Polite })
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RunButton(stringResource(R.string.care_go_back), onBack, kind = RunButtonKind.Secondary,
                modifier = Modifier.weight(1f).testTag("upgrade-offline-back"))
            RunButton(stringResource(R.string.care_check_connection), onRecheck, modifier = Modifier.weight(1f).testTag("upgrade-offline-recheck"))
        }
        Spacer(Modifier.width(1.dp))
    }
}
