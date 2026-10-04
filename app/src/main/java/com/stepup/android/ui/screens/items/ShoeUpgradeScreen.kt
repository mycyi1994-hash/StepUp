package com.stepup.android.ui.screens.items

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.domain.ForgeMaterial
import com.stepup.android.domain.ForgeMaterialBlock
import com.stepup.android.domain.ForgeQuote
import com.stepup.android.domain.ForgeStats
import com.stepup.android.domain.ForgeTargetBlock
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.ShoeForge
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.tier
import com.stepup.android.ui.components.DetailPage
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunButtonKind
import com.stepup.android.ui.components.RunDivider
import com.stepup.android.ui.components.RunNotice
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.RunSheetText
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.RunTextAction
import com.stepup.android.ui.components.ShoeGradeBadge
import com.stepup.android.ui.components.ShoeNameWithBadge
import com.stepup.android.ui.components.SneakerGradeStage
import com.stepup.android.ui.components.SneakerGradeThumb
import com.stepup.android.ui.components.SneakerVisual
import com.stepup.android.ui.components.label
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpDesign
import com.stepup.android.ui.theme.StepUpSans

/*
 * 신발 강화 — 파란 톤 v4 강화 전달본(docs/redesign/blue-v4-2026-10/01-packages/stepup-upgrade-blue-claude-v19)의 UP01 ~ UP15 · UP18.
 * 강화 본문은 신발 상세의 "강화하기"로 여는 독립 화면이고, 재료 선택(UP02 · 09 · 10 · 11) · 최종 확인(UP04) · 재료 변경(UP12) ·
 * 연결 확인(UP14)은 하단 시트 하나씩이다. 판정 · 소각은 서버(0054 forge_*, 규칙 forge-v2)가 하고 화면은 서버 값만 그린다.
 *
 * 재료 규칙(2026-10-04 사용자 결정): 대상보다 한 등급 아래 · 같은 등급 · 더 높은 등급. 두 등급 이상 아래는 쓸 수 없다.
 * 그래서 UP17(일반 등급은 강화 불가)은 없다 — 일반 신발도 같은 등급 이상 재료로 강화한다.
 * 성공률 계산 안내(UP16) · 도움말 · "기본 + 보정" 같은 계산 내역 문장은 없다 — 재료별 추가 성공률 · 최종 성공률 · 소각 안내만.
 * 색 · 버튼 · 시트는 러닝 리메이크의 파란 부품(runTone · RunButton · RunSheet)을, 머리 · 세 줄은 상세의 부품(ShoeCareParts)을 쓴다.
 */

/** 화면의 모든 동작 — 테스트는 빈 동작으로 상태만 넣어 찍는다 */
class UpgradeActions(
    val onBack: () -> Unit = {},
    val onOpenOwned: () -> Unit = {},
    val onRetryLoad: () -> Unit = {},
    val onOpenPicker: () -> Unit = {},
    val onReloadPicker: () -> Unit = {},
    val onToggle: (Long) -> Unit = {},
    val onApply: () -> Unit = {},
    val onCloseSheet: () -> Unit = {},
    val onRemove: (Long) -> Unit = {},
    val onRequestConfirm: () -> Unit = {},
    val onStart: () -> Unit = {},
    val onCheckResult: () -> Unit = {},
    val onRecheckConnection: () -> Unit = {},
    val onAgain: () -> Unit = {},
)

@Composable
fun ShoeUpgradeScreen(
    sneakerId: Long,
    onBack: () -> Unit,
    onOpenOwned: () -> Unit,
    viewModel: ShoeUpgradeViewModel = viewModel(key = "upgrade-$sneakerId", factory = ShoeUpgradeViewModel.factory(sneakerId)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ShoeUpgradeContent(
        state = state,
        actions = UpgradeActions(
            onBack = onBack,
            onOpenOwned = onOpenOwned,
            onRetryLoad = viewModel::retryLoad,
            onOpenPicker = viewModel::openPicker,
            onReloadPicker = viewModel::reloadPicker,
            onToggle = viewModel::toggle,
            onApply = viewModel::applyPicker,
            onCloseSheet = viewModel::closeSheet,
            onRemove = viewModel::removeMaterial,
            onRequestConfirm = viewModel::requestConfirm,
            onStart = viewModel::start,
            onCheckResult = viewModel::checkResult,
            onRecheckConnection = viewModel::recheckConnection,
            onAgain = viewModel::again,
        ),
    )
}

/** 상태를 받아 그리기만 한다(기기 검사가 시안 장면을 바로 넣어 찍는다) */
@Composable
fun ShoeUpgradeContent(state: ShoeUpgradeState, actions: UpgradeActions) {
    val phase = state.phase
    val bottom: (@Composable ColumnScope.() -> Unit)? = when (phase) {
        UpgradePhase.Editing -> { { EditingButton(state, actions) } }
        UpgradePhase.Running -> { {
            RunButton(stringResource(R.string.upg_running_button), {}, enabled = false, modifier = Modifier.testTag("upgrade-primary"))
            RunTextAction(stringResource(R.string.upg_later), actions.onBack,
                Modifier.align(Alignment.CenterHorizontally).testTag("upgrade-later"))
        } }
        is UpgradePhase.Unknown -> { {
            RunButton(
                stringResource(if (state.checking) R.string.upg_quoting else R.string.upg_unknown_retry),
                actions.onCheckResult, enabled = !state.checking, busy = state.checking, modifier = Modifier.testTag("upgrade-primary"),
            )
            Spacer(Modifier.height(10.dp))
            RunButton(stringResource(R.string.upg_later), actions.onBack, kind = RunButtonKind.Secondary,
                modifier = Modifier.testTag("upgrade-later"))
        } }
        is UpgradePhase.Succeeded -> { {
            RunButton(stringResource(R.string.upg_back_detail), actions.onBack, modifier = Modifier.testTag("upgrade-primary"))
            val after = phase.result.target ?: state.info?.stats
            if (after == null || after.level < after.maxLevel) {
                Spacer(Modifier.height(10.dp))
                RunButton(stringResource(R.string.upg_next), actions.onAgain, kind = RunButtonKind.Secondary,
                    modifier = Modifier.testTag("upgrade-next"))
            }
        } }
        is UpgradePhase.Failed -> { {
            RunButton(stringResource(R.string.upg_reselect), actions.onAgain, modifier = Modifier.testTag("upgrade-primary"))
            RunTextAction(stringResource(R.string.upg_back_detail), actions.onBack,
                Modifier.align(Alignment.CenterHorizontally).testTag("upgrade-detail"))
        } }
        is UpgradePhase.Blocked -> { {
            if (phase.block == ForgeTargetBlock.MAX_LEVEL) {
                RunButton(stringResource(R.string.upg_back_detail), actions.onBack, modifier = Modifier.testTag("upgrade-primary"))
            } else {
                RunButton(stringResource(R.string.upg_pick_owned), actions.onOpenOwned, modifier = Modifier.testTag("upgrade-primary"))
            }
        } }
        else -> null
    }
    DetailPage(
        title = stringResource(R.string.upg_title),
        onBack = actions.onBack,
        trailing = { Spacer(Modifier.size(StepUpDesign.TouchTarget)) },
        bottomBar = bottom,
    ) {
        item(key = "body") {
            Column(Modifier.fillMaxWidth().testTag("upgrade-screen")) {
                when (phase) {
                    UpgradePhase.Loading -> LoadingBody()
                    UpgradePhase.LoadFailed -> CareStatePanel(
                        icon = Icons.Filled.Refresh,
                        title = stringResource(R.string.upg_load_failed_title),
                        body = stringResource(R.string.upg_load_failed_body),
                        primary = stringResource(R.string.upg_reload) to actions.onRetryLoad,
                        secondary = stringResource(R.string.upg_back_detail) to actions.onBack,
                        modifier = Modifier.testTag("upgrade-load-failed"),
                    )
                    UpgradePhase.SignIn -> CareStatePanel(
                        icon = Icons.Outlined.Info,
                        title = stringResource(R.string.upg_sign_in_title),
                        body = stringResource(R.string.toast_sign_in_required),
                        primary = stringResource(R.string.upg_back_detail) to actions.onBack,
                        modifier = Modifier.testTag("upgrade-sign-in"),
                    )
                    UpgradePhase.Editing -> EditingBody(state, actions)
                    UpgradePhase.Running -> RunningBody(state)
                    is UpgradePhase.Succeeded -> SuccessBody(state, phase)
                    is UpgradePhase.Failed -> FailedBody(state, phase)
                    is UpgradePhase.Unknown -> UnknownBody(state)
                    is UpgradePhase.Blocked -> BlockedBody(state, phase.block)
                }
            }
        }
    }

    when (val sheet = state.sheet) {
        UpgradeSheet.None -> Unit
        UpgradeSheet.Picker -> PickerSheet(state, actions)
        is UpgradeSheet.Confirm -> ConfirmSheet(state, sheet.quote, actions)
        is UpgradeSheet.Changed -> ChangedSheet(sheet.removed, actions)
        UpgradeSheet.Offline -> OfflineSheet(state.checking, actions)
    }
}

// ── UP01 · UP03 · UP08 본문 ──────────────────────────────────────

/** 선택에 따른 성공률 — 0 ~ 2개는 미리보기(서버가 준 기본 · 보정의 합), 3개는 서버 견적이 있으면 그 값 */
internal fun upgradeRate(state: ShoeUpgradeState): Int? {
    val info = state.info ?: return null
    state.quote?.takeIf { it.materialIds.sorted() == state.selection.applied.sorted() }?.let { return it.ratePermille }
    return ShoeForge.ratePermille(info.basePermille, state.applied.map { it.bonusPermille })
}

@Composable
private fun EditingBody(state: ShoeUpgradeState, actions: UpgradeActions) {
    val info = state.info
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TargetHeader(state.shoe, info?.stats?.level?.let { stringResource(R.string.upg_level, it) })
        // 강화 재료 n / 3
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(
                title = stringResource(R.string.upg_materials),
                subtitle = stringResource(R.string.upg_materials_rule),
                trailing = stringResource(R.string.upg_count, state.selection.applied.size),
                trailingTag = "upgrade-count",
            )
            MaterialSlots(state.applied, onAdd = actions.onOpenPicker, onRemove = actions.onRemove)
        }
        // 성공 시 변화 — 레벨 · 효율 · 착화감 세 줄(내구도 없음)
        if (info != null) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(stringResource(R.string.upg_changes))
                val next = state.quote?.takeIf { it.materialIds.sorted() == state.selection.applied.sorted() }?.after
                    ?: info.previewNext()
                UpgradeStatRows(forgeRows(info.stats, next), next.maxLevel, Modifier.testTag("upgrade-rows"))
            }
        }
        RunDivider()
        RateLine(
            label = stringResource(if (state.selection.applied.size == ShoeForge.MATERIALS) R.string.upg_rate else R.string.upg_rate_preview),
            permille = upgradeRate(state),
        )
        Text(stringResource(R.string.upg_burn_note), style = runTextStyle(14.sp, runTone().label, FontWeight.Medium),
            modifier = Modifier.testTag("upgrade-burn-note"))
    }
}

@Composable
private fun EditingButton(state: ShoeUpgradeState, actions: UpgradeActions) {
    val count = state.selection.applied.size
    val label = when {
        state.quoting -> stringResource(R.string.upg_quoting)
        count == 0 -> stringResource(R.string.upg_need_3)
        count < ShoeForge.MATERIALS -> stringResource(R.string.upg_need_more, ShoeForge.MATERIALS - count)
        else -> stringResource(R.string.upg_action)
    }
    RunButton(
        label, actions.onRequestConfirm,
        enabled = count == ShoeForge.MATERIALS && !state.quoting && state.info != null,
        busy = state.quoting,
        modifier = Modifier.testTag("upgrade-primary"),
    )
}

// ── UP05 · 06 · 07 · 15 · 13 · 18 ─────────────────────────────────

@Composable
private fun RunningBody(state: ShoeUpgradeState) {
    val t = runTone()
    Column(Modifier.fillMaxWidth().testTag("upgrade-running"), verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        CenteredTarget(state.shoe, state.info?.stats?.level)
        Headline(stringResource(R.string.upg_running_title), body = null)
        RunSpinner(Modifier.size(44.dp))
        if (state.running.isNotEmpty()) {
            SectionTitle(stringResource(R.string.upg_running_materials))
            MaterialCards(state.running, locked = false)
        }
        Text(stringResource(R.string.upg_running_note), style = runTextStyle(15.sp, t.label, FontWeight.Medium),
            textAlign = TextAlign.Center)
    }
}

@Composable
private fun SuccessBody(state: ShoeUpgradeState, phase: UpgradePhase.Succeeded) {
    val t = runTone()
    val after = phase.result.target
    val before = phase.before
    val korean = LocalConfiguration.current.locales[0].language == "ko"
    val level = phase.result.levelAfter
    Column(Modifier.fillMaxWidth().testTag("upgrade-success"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.upg_success_kicker), style = runTextStyle(17.sp, t.cyan, FontWeight.Bold))
            Spacer(Modifier.height(4.dp))
            // 레벨 11이 · 레벨 12가 — 받침에 맞춘 조사
            val levelText = if (korean) "$level" + (if (level % 10 in setOf(2, 4, 5, 9)) "가" else "이") else "$level"
            val title = stringResource(R.string.upg_success_title, levelText)
            Text(
                buildAnnotatedString {
                    val at = title.indexOf(level.toString())
                    if (at >= 0) {
                        append(title.substring(0, at))
                        withStyle(SpanStyle(color = CartoonColors.Level.top)) { append(level.toString()) }
                        append(title.substring(at + level.toString().length))
                    } else {
                        append(title)
                    }
                },
                textAlign = TextAlign.Center,
                style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 1.25.em,
                    letterSpacing = (-0.02).em, color = t.text),
                modifier = Modifier.semantics { heading() }.testTag("upgrade-success-title"),
            )
        }
        TargetHeader(state.shoe, after?.let { stringResource(R.string.upg_level_of, level, it.maxLevel) }
            ?: stringResource(R.string.upg_level, level))
        if (after != null) {
            UpgradeStatRows(forgeRows(before ?: after, after), after.maxLevel, Modifier.testTag("upgrade-rows"))
        }
        RunDivider()
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.upg_success_burned), style = runTextStyle(18.sp, t.cyan, FontWeight.Bold),
                textAlign = TextAlign.Center)
            Text(stringResource(R.string.upg_success_burned_body), style = runTextStyle(14.sp, t.label, FontWeight.Medium),
                textAlign = TextAlign.Center)
        }
        if (after != null && after.level >= after.maxLevel) {
            RunNotice(stringResource(R.string.upg_reached_max), tag = "upgrade-reached-max")
        }
    }
}

@Composable
private fun FailedBody(state: ShoeUpgradeState, phase: UpgradePhase.Failed) {
    val t = runTone()
    val stats = phase.result.target ?: state.info?.stats
    Column(Modifier.fillMaxWidth().testTag("upgrade-failed"), verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.upg_failed_title), textAlign = TextAlign.Center,
                style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 1.25.em,
                    letterSpacing = (-0.02).em, color = t.text),
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.upg_failed_burned), style = runTextStyle(16.sp, t.dangerText, FontWeight.Bold),
                textAlign = TextAlign.Center)
        }
        CenteredTarget(state.shoe, phase.result.levelAfter)
        Text(stringResource(R.string.upg_failed_kept), style = runTextStyle(15.sp, t.label, FontWeight.Medium), textAlign = TextAlign.Center)
        // 실패에는 증가 예정 구간을 그리지 않는다
        if (stats != null) UpgradeStatRows(forgeRows(stats, null), stats.maxLevel, Modifier.testTag("upgrade-rows"), previewBars = false)
    }
}

@Composable
private fun UnknownBody(state: ShoeUpgradeState) {
    val t = runTone()
    val stats = state.info?.stats
    Column(Modifier.fillMaxWidth().testTag("upgrade-unknown"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TargetHeader(state.shoe, stats?.level?.let { stringResource(R.string.upg_level, it) })
        Headline(stringResource(R.string.upg_unknown_title), stringResource(R.string.upg_unknown_body))
        if (state.running.isNotEmpty()) {
            SectionTitle(stringResource(R.string.upg_unknown_materials))
            MaterialCards(state.running, locked = true)
        }
        if (stats != null) {
            val shape = RoundedCornerShape(16.dp)
            Column(
                Modifier.fillMaxWidth().clip(shape).background(t.panel, shape).border(1.dp, t.panelEdge, shape).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.upg_unknown_last), style = runTextStyle(15.sp, t.text, FontWeight.Bold))
                UpgradeStatRows(forgeRows(stats, null), stats.maxLevel, Modifier.testTag("upgrade-rows"), previewBars = false)
            }
        }
        RunNotice(stringResource(R.string.upg_unknown_note), tag = "upgrade-unknown-note")
    }
}

@Composable
private fun BlockedBody(state: ShoeUpgradeState, block: ForgeTargetBlock) {
    val stats = state.info?.stats
    when (block) {
        ForgeTargetBlock.MAX_LEVEL -> Column(Modifier.fillMaxWidth().testTag("upgrade-max"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // 상한 숫자는 서버가 준 값(대개 20 — 금고 신발은 더 낮을 수 있다). 예전에 20을 넘은 레벨은 그대로 보인다
            TargetHeader(state.shoe, stats?.let { stringResource(R.string.upg_level_of, it.level, it.maxLevel) })
            Headline(stringResource(R.string.upg_max_title), stringResource(R.string.upg_max_body))
            if (stats != null) UpgradeStatRows(forgeRows(stats, null), stats.maxLevel, Modifier.testTag("upgrade-rows"), previewBars = false)
        }
        else -> Column(Modifier.fillMaxWidth().testTag("upgrade-unavailable"), verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            if (state.shoe != null && block != ForgeTargetBlock.TARGET_GONE) CenteredTarget(state.shoe, stats?.level ?: state.shoe.level)
            Headline(
                stringResource(R.string.upg_unavailable_title),
                stringResource(if (block == ForgeTargetBlock.LEGACY) R.string.upg_unavailable_legacy else R.string.upg_unavailable_body),
            )
            RunNotice(stringResource(R.string.upg_unavailable_note), tag = "upgrade-unavailable-note")
        }
    }
}

@Composable
private fun LoadingBody() {
    val t = runTone()
    Column(Modifier.fillMaxWidth().padding(top = 120.dp).testTag("upgrade-loading"), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        RunSpinner(Modifier.size(40.dp))
        Text(stringResource(R.string.upg_loading), style = runTextStyle(15.sp, t.label, FontWeight.Medium),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    }
}

// ── 대상 머리 ──────────────────────────────────────────────────

/** 왼쪽 등급 무대 · 오른쪽 이름 · 등급 배지 + No. · Lv.(상세 · 수리의 CareShoeHeader 그대로) */
@Composable
private fun TargetHeader(shoe: Sneaker?, levelText: String?) {
    if (shoe == null) return
    CareShoeHeader(shoe, Modifier.testTag("upgrade-target"), stageWidth = 168.dp, showLevel = levelText != null, levelText = levelText)
}

/** 가운데 무대 · 이름 · 배지 + No. · Lv.(진행 중 · 실패 · 대상 사용 불가) */
@Composable
private fun CenteredTarget(shoe: Sneaker?, level: Int?) {
    if (shoe == null) return
    val t = runTone()
    Column(Modifier.fillMaxWidth().testTag("upgrade-target"), horizontalAlignment = Alignment.CenterHorizontally) {
        SneakerGradeStage(shoe, Modifier.widthIn(max = 280.dp).fillMaxWidth(0.7f))
        Spacer(Modifier.height(12.dp))
        ShoeNameWithBadge(
            name = shoe.shoeName(), tier = shoe.tier,
            style = TextStyle(color = t.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 1.25.em),
            textAlign = TextAlign.Center, maxLines = 2,
            suffix = formatShoeNumber(shoe.mintNumber),
            suffixStyle = TextStyle(color = t.label, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
            modifier = Modifier.fillMaxWidth().semantics { heading() }.testTag("care-shoe-name"),
        )
        if (level != null) {
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.upg_level, level),
                style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = t.cyan,
                    fontFeatureSettings = "tnum"),
                modifier = Modifier.testTag("care-shoe-level"))
        }
    }
}

@Composable
private fun Headline(title: String, body: String?) {
    val t = runTone()
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            title, textAlign = TextAlign.Center,
            style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 1.3.em,
                letterSpacing = (-0.02).em, color = t.text),
            modifier = Modifier.semantics { heading() }.testTag("upgrade-headline"),
        )
        if (body != null) {
            Spacer(Modifier.height(8.dp))
            Text(body, style = runTextStyle(15.sp, t.label, FontWeight.Medium, 1.45f), textAlign = TextAlign.Center)
        }
    }
}

// ── 재료 슬롯 · 카드 ────────────────────────────────────────────

private val SlotShape = RoundedCornerShape(14.dp)

@Composable
private fun slotSurface(selected: Boolean = false): Modifier {
    val t = runTone()
    return Modifier.clip(SlotShape)
        .background(Brush.verticalGradient(listOf(if (t.dark) Color(0xFF0E3268) else t.panelTop, t.panel)), SlotShape)
        .border(if (selected) 1.5.dp else 1.dp, if (selected) t.cyan else t.panelEdge, SlotShape)
}

/** 같은 폭의 슬롯 셋 — 비면 + 신발 추가(어느 빈 슬롯이든 같은 선택창), 차면 그림 · 등급 · Lv · No. · 보정, 오른쪽 위 X */
@Composable
private fun MaterialSlots(applied: List<ForgeMaterial>, onAdd: () -> Unit, onRemove: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(ShoeForge.MATERIALS) { index ->
            val material = applied.getOrNull(index)
            Box(Modifier.weight(1f).testTag("upgrade-slot-$index")) {
                if (material == null) EmptySlot(index, onAdd) else FilledSlot(material, onRemove)
            }
        }
    }
}

@Composable
private fun EmptySlot(index: Int, onAdd: () -> Unit) {
    val t = runTone()
    val description = stringResource(R.string.upg_add_cd, index + 1)
    Column(
        Modifier.fillMaxWidth().heightIn(min = 140.dp).then(slotSurface())
            .feedbackClickable(role = Role.Button, onClick = onAdd)
            .semantics { contentDescription = description }
            .testTag("upgrade-slot-add-$index"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = t.cyan, modifier = Modifier.size(40.dp))
        Text(stringResource(R.string.upg_add), style = runTextStyle(14.sp, t.label, FontWeight.SemiBold), textAlign = TextAlign.Center)
    }
}

@Composable
private fun FilledSlot(material: ForgeMaterial, onRemove: (Long) -> Unit) {
    val t = runTone()
    val shoe = material.shoe
    val removeCd = stringResource(R.string.upg_remove_cd, shoe.shoeName())
    Box(Modifier.fillMaxWidth().then(slotSurface())) {
        MaterialCardBody(material, locked = false)
        Box(
            Modifier.align(Alignment.TopEnd).size(StepUpDesign.TouchTarget)
                .feedbackClickable(role = Role.Button) { onRemove(shoe.id) }
                .semantics { contentDescription = removeCd }
                .testTag("upgrade-remove-${shoe.id}"),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).background(t.inset), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = t.text, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** UP04 · UP05 · UP15 의 재료 카드 셋(누를 수 없음) */
@Composable
private fun MaterialCards(materials: List<ForgeMaterial>, locked: Boolean, showName: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        materials.take(ShoeForge.MATERIALS).forEach { material ->
            Box(Modifier.weight(1f).then(slotSurface()).testTag("upgrade-card-${material.id}")) {
                MaterialCardBody(material, locked = locked, showName = showName, showBonus = !showName)
            }
        }
        repeat((ShoeForge.MATERIALS - materials.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MaterialCardBody(material: ForgeMaterial, locked: Boolean, showName: Boolean = false, showBonus: Boolean = true) {
    val t = runTone()
    val shoe = material.shoe
    val name = shoe.shoeName()
    val tierLabel = shoe.tier.label()
    val bonus = ShoeForge.formatBonus(material.bonusPermille)
    val description = stringResource(R.string.upg_material_cd, name, tierLabel, shoe.level, shoe.mintNumber, bonus)
    Column(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = description }.padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.45f), contentAlignment = Alignment.Center) {
            SneakerVisual(shoe, Modifier.fillMaxSize().alpha(if (locked) 0.55f else 1f))
            if (locked) Icon(Icons.Filled.Lock, contentDescription = null, tint = t.text, modifier = Modifier.size(26.dp))
        }
        if (showName) {
            Text(name, style = runTextStyle(13.sp, t.text, FontWeight.SemiBold, 1.25f), maxLines = 2)
        }
        // 좁은 칸 · 큰 글씨에서는 배지 아래로 내려간다(레벨 · 번호가 잘리지 않게)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            ShoeGradeBadge(shoe.tier, decorative = true, modifier = Modifier.align(Alignment.CenterVertically))
            Column(Modifier.align(Alignment.CenterVertically)) {
                Text(stringResource(R.string.upg_level, shoe.level), style = runTextStyle(12.sp, t.text, FontWeight.SemiBold), softWrap = false)
                Text(formatShoeNumber(shoe.mintNumber), style = runTextStyle(11.sp, t.label, FontWeight.Medium), softWrap = false)
            }
        }
        if (showBonus) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(t.inset).padding(vertical = 4.dp),
                contentAlignment = Alignment.Center) {
                Text(bonus, style = runTextStyle(17.sp, t.cyan, FontWeight.ExtraBold).copy(fontFeatureSettings = "tnum"),
                    maxLines = 1, softWrap = false, modifier = Modifier.testTag("upgrade-bonus-${shoe.id}"))
            }
        }
    }
}

// ── 공용 조각 ──────────────────────────────────────────────────

@Composable
private fun SectionTitle(title: String, subtitle: String? = null, trailing: String? = null, trailingTag: String? = null) {
    val t = runTone()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, color = t.text,
                    letterSpacing = (-0.02).em),
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) Text(subtitle, style = runTextStyle(14.sp, t.label, FontWeight.Medium))
        }
        if (trailing != null) {
            Text(trailing, style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = t.cyan,
                fontFeatureSettings = "tnum"),
                modifier = if (trailingTag != null) Modifier.testTag(trailingTag) else Modifier)
        }
    }
}

/** 강화 성공률 · 최종 수치. 조회되지 않은 값은 0% 로 보이지 않는다 */
@Composable
private fun RateLine(label: String, permille: Int?) {
    val t = runTone()
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = runTextStyle(18.sp, t.label, FontWeight.Bold), modifier = Modifier.weight(1f))
        Text(
            permille?.let(ShoeForge::formatRate) ?: stringResource(R.string.upg_rate_unknown),
            style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, color = t.cyan,
                fontFeatureSettings = "tnum"),
            modifier = Modifier.testTag("upgrade-rate"),
        )
    }
}

/** 등급 차이를 말로 — 한 등급 아래 · 같은 등급 · 한(두 · 세) 등급 위. 두 등급 이상 아래는 쓸 수 없다 */
@Composable
private fun relationLabel(target: Rarity, material: Rarity): String = stringResource(
    when (ShoeForge.gradeDiff(target, material)) {
        -1 -> R.string.upg_rel_down1
        0 -> R.string.upg_rel_same
        1 -> R.string.upg_rel_up1
        2 -> R.string.upg_rel_up2
        else -> if (ShoeForge.gradeAllowed(target, material)) R.string.upg_rel_up3 else R.string.upg_block_grade
    },
)

@Composable
private fun blockLabel(block: ForgeMaterialBlock?): String = stringResource(
    when (block) {
        ForgeMaterialBlock.GRADE -> R.string.upg_block_grade
        ForgeMaterialBlock.EQUIPPED -> R.string.upg_block_equipped
        ForgeMaterialBlock.LISTED -> R.string.upg_block_listed
        ForgeMaterialBlock.ON_CHAIN -> R.string.upg_block_chain
        ForgeMaterialBlock.LEVEL -> R.string.upg_block_level
        else -> R.string.upg_block_locked
    },
)

// ── 시트 ──────────────────────────────────────────────────────

/** 시트 머리 — 왼쪽 큰 제목 · 작은 줄, 오른쪽 n / 3(닫기 X 아래) */
@Composable
private fun SheetHead(title: String, subtitle: String?, count: Int?) {
    val t = runTone()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        CareSheetTitle(title, subtitle, Modifier.weight(1f))
        if (count != null) {
            Text(stringResource(R.string.upg_count, count),
                style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = t.cyan,
                    fontFeatureSettings = "tnum"),
                modifier = Modifier.testTag("picker-count"))
        }
    }
}

/** UP02 · 09 · 10 · 11 — 하나의 재료 선택 시트. 목록과 아래 요약이 같은 임시 선택(소유 id)을 본다 */
@Composable
private fun PickerSheet(state: ShoeUpgradeState, actions: UpgradeActions) {
    val t = runTone()
    val draft = state.selection.draft.orEmpty()
    val load = state.candidates
    val targetRarity = state.info?.rarity ?: state.shoe?.rarity ?: Rarity.LEGENDARY
    val lowest = Rarity.entries[(targetRarity.ordinal - 1).coerceAtLeast(0)]
    val usable = (load as? MaterialsLoad.Ready)?.list?.filter { it.usable }.orEmpty()
    RunSheet(onDismiss = actions.onCloseSheet, modifier = Modifier.testTag("upgrade-picker"), closeTag = "picker-close-x") {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SheetHead(
                stringResource(R.string.upg_pick_title),
                if (load is MaterialsLoad.Loading) stringResource(R.string.upg_pick_loading)
                else stringResource(R.string.upg_pick_rule, lowest.label()),
                count = draft.size,
            )
            // 목록만 넘어간다 — 아래 요약 · 버튼이 마지막 줄을 가리지 않는다
            Column(
                Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (load) {
                    MaterialsLoad.Loading -> repeat(3) {
                        Box(Modifier.fillMaxWidth().height(96.dp).then(slotSurface()).alpha(0.6f).testTag("picker-skeleton"))
                    }
                    MaterialsLoad.Failed -> Column(Modifier.fillMaxWidth().padding(vertical = 20.dp).testTag("picker-failed"),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(72.dp).clip(CircleShape).background(t.inset), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.WifiOff, contentDescription = null, tint = t.label, modifier = Modifier.size(34.dp))
                        }
                        Headline(stringResource(R.string.upg_pick_failed_title), stringResource(R.string.upg_pick_failed_body))
                    }
                    is MaterialsLoad.Ready -> if (usable.isEmpty() && draft.isEmpty()) {
                        Column(Modifier.fillMaxWidth().padding(vertical = 24.dp).testTag("picker-empty"),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Headline(stringResource(R.string.upg_pick_empty_title), stringResource(R.string.upg_pick_empty_body))
                        }
                    } else {
                        // 쓸 수 있는 것 먼저(보정이 큰 순), 쓸 수 없는 것은 까닭과 함께 흐리게(고를 수 없다)
                        val ordered = load.list.filter { it.usable || it.id in draft } + load.list.filterNot { it.usable || it.id in draft }
                        ordered.forEach { material ->
                            PickerRow(material, targetRarity, selected = material.id in draft, full = draft.size >= ShoeForge.MATERIALS,
                                onClick = { actions.onToggle(material.id) })
                        }
                    }
                }
            }
            when {
                load is MaterialsLoad.Loading ->
                    RunButton(stringResource(R.string.upg_pick_loading_button), {}, enabled = false, busy = true,
                        modifier = Modifier.testTag("picker-apply"))
                load is MaterialsLoad.Failed -> {
                    Text(stringResource(R.string.upg_pick_kept), style = runTextStyle(14.sp, t.label, FontWeight.Medium),
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    RunButton(stringResource(R.string.upg_reload), actions.onReloadPicker, modifier = Modifier.testTag("picker-reload"))
                    RunTextAction(stringResource(R.string.common_close), actions.onCloseSheet,
                        Modifier.align(Alignment.CenterHorizontally).testTag("picker-close"))
                }
                usable.isEmpty() && draft.isEmpty() -> {
                    RunButton(stringResource(R.string.upg_pick_owned), actions.onOpenOwned, modifier = Modifier.testTag("picker-owned"))
                    RunTextAction(stringResource(R.string.common_close), actions.onCloseSheet,
                        Modifier.align(Alignment.CenterHorizontally).testTag("picker-close"))
                }
                else -> {
                    val chosen = draft.mapNotNull { id -> state.known[id] }
                    val rate = state.info?.let { ShoeForge.ratePermille(it.basePermille, chosen.map { m -> m.bonusPermille }) }
                    val shape = RoundedCornerShape(14.dp)
                    Row(
                        Modifier.fillMaxWidth().clip(shape).background(t.inset, shape).border(1.dp, t.panelEdge, shape)
                            .padding(horizontal = 16.dp, vertical = 12.dp).semantics(mergeDescendants = true) {},
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(if (draft.size == ShoeForge.MATERIALS) R.string.upg_pick_final else R.string.upg_rate_preview),
                            style = runTextStyle(17.sp, t.text, FontWeight.Bold), modifier = Modifier.weight(1f),
                        )
                        Text(rate?.let(ShoeForge::formatRate) ?: stringResource(R.string.upg_rate_unknown),
                            style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = t.cyan,
                                fontFeatureSettings = "tnum"),
                            modifier = Modifier.testTag("picker-rate"))
                    }
                    Text(stringResource(R.string.upg_pick_burn), style = runTextStyle(13.sp, t.label, FontWeight.Medium))
                    RunButton(
                        if (draft.isEmpty()) stringResource(R.string.upg_pick_apply_none) else stringResource(R.string.upg_pick_apply, draft.size),
                        actions.onApply, enabled = draft.isNotEmpty(), modifier = Modifier.testTag("picker-apply"),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PickerRow(material: ForgeMaterial, target: Rarity, selected: Boolean, full: Boolean, onClick: () -> Unit) {
    val t = runTone()
    val shoe = material.shoe
    val usable = material.usable
    val relation = relationLabel(target, shoe.rarity)
    val stateText = when {
        !usable -> blockLabel(material.block)
        selected -> stringResource(R.string.upg_pick_selected)
        full -> stringResource(R.string.upg_pick_full)
        else -> stringResource(R.string.upg_pick_not_selected)
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 92.dp).then(slotSurface(selected))
            .feedbackClickable(enabled = usable || selected, role = Role.Checkbox, onClick = onClick)
            .semantics { this.selected = selected; stateDescription = stateText }
            .alpha(if (usable || selected) 1f else 0.5f)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("picker-row-${shoe.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(92.dp).aspectRatio(1.45f)) { SneakerVisual(shoe, Modifier.fillMaxSize()) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(shoe.shoeName(), style = runTextStyle(16.sp, t.text, FontWeight.Bold, 1.25f))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ShoeGradeBadge(shoe.tier, decorative = true, modifier = Modifier.align(Alignment.CenterVertically))
                Text(stringResource(R.string.upg_lv_no, shoe.level, shoe.mintNumber), style = runTextStyle(13.sp, t.label, FontWeight.Medium),
                    modifier = Modifier.align(Alignment.CenterVertically))
            }
            // 대상과의 등급 차이 — 쓸 수 없으면 그 까닭(두 등급 이상 낮아요 · 착용 중 …)
            if (usable) {
                Text(relation, style = runTextStyle(13.sp, t.label, FontWeight.Medium), modifier = Modifier.testTag("picker-relation-${shoe.id}"))
            } else {
                Text(blockLabel(material.block), style = runTextStyle(13.sp, t.dangerText, FontWeight.SemiBold),
                    modifier = Modifier.testTag("picker-block-${shoe.id}"))
            }
        }
        Spacer(Modifier.width(8.dp))
        if (usable) {
            Text(ShoeForge.formatBonus(material.bonusPermille),
                style = runTextStyle(17.sp, t.cyan, FontWeight.ExtraBold).copy(fontFeatureSettings = "tnum"), softWrap = false,
                modifier = Modifier.testTag("picker-bonus-${shoe.id}"))
            Spacer(Modifier.width(8.dp))
        }
        Box(
            Modifier.size(30.dp).clip(CircleShape)
                .background(if (selected) t.cobalt else Color.Transparent)
                .border(1.5.dp, if (selected) t.cobalt else t.panelEdge, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

/** UP04 — 대상 · 소각할 재료 3개 · 최종 성공률 · 소각 안내, 취소 / 강화 시작. 이 단계만 서버를 바꾼다 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfirmSheet(state: ShoeUpgradeState, quote: ForgeQuote, actions: UpgradeActions) {
    val t = runTone()
    val shoe = state.shoe
    val materials = quote.materialIds.mapNotNull { state.known[it] }
    RunSheet(onDismiss = actions.onCloseSheet, modifier = Modifier.testTag("upgrade-confirm")) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            RunSheetText(stringResource(R.string.upg_confirm_title))
            if (shoe != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(120.dp)) { SneakerGradeThumb(shoe, Modifier.fillMaxWidth()) }
                    Spacer(Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ShoeNameWithBadge(
                            name = shoe.shoeName(), tier = shoe.tier,
                            style = TextStyle(color = t.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 1.25.em),
                            maxLines = 2,
                            suffix = formatShoeNumber(shoe.mintNumber),
                            suffixStyle = TextStyle(color = t.label, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
                        )
                        Text(
                            buildAnnotatedString {
                                val text = stringResource(R.string.upg_confirm_level, quote.before.level, quote.after.level)
                                val cut = text.lastIndexOf(quote.after.level.toString())
                                if (cut > 0) {
                                    append(text.substring(0, cut))
                                    withStyle(SpanStyle(color = CartoonColors.Level.top, fontWeight = FontWeight.ExtraBold)) { append(text.substring(cut)) }
                                } else {
                                    append(text)
                                }
                            },
                            style = runTextStyle(18.sp, t.text, FontWeight.Bold), modifier = Modifier.testTag("confirm-level"),
                        )
                    }
                }
            }
            RunDivider()
            Text(stringResource(R.string.upg_confirm_burn_title), style = runTextStyle(16.sp, t.text, FontWeight.Bold))
            MaterialCards(materials, locked = false, showName = true)
            RunDivider()
            Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.upg_confirm_rate), style = runTextStyle(18.sp, t.label, FontWeight.Bold))
                Spacer(Modifier.width(14.dp))
                Text(ShoeForge.formatRate(quote.ratePermille),
                    style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, color = t.cyan,
                        fontFeatureSettings = "tnum"),
                    modifier = Modifier.testTag("confirm-rate"))
            }
            Text(stringResource(R.string.upg_burn_note) + "\n" + stringResource(R.string.upg_confirm_irreversible),
                style = runTextStyle(14.sp, t.label, FontWeight.Medium, 1.45f), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RunButton(stringResource(R.string.common_cancel), actions.onCloseSheet, kind = RunButtonKind.Secondary,
                    modifier = Modifier.weight(0.4f).testTag("confirm-cancel"))
                RunButton(stringResource(R.string.upg_confirm_start), actions.onStart,
                    modifier = Modifier.weight(0.6f).testTag("confirm-start"))
            }
        }
    }
}

/** UP12 — 실행 전 검증에서 쓸 수 없게 된 재료(그것만 뺐다). 강화는 시작되지 않았다 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChangedSheet(removed: List<ForgeMaterial>, actions: UpgradeActions) {
    val t = runTone()
    RunSheet(onDismiss = actions.onCloseSheet, modifier = Modifier.testTag("upgrade-changed")) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CareSheetTitle(stringResource(R.string.upg_changed_title),
                stringResource(R.string.upg_changed_body, removed.size.coerceAtLeast(1)))
            removed.forEach { material ->
                val shoe = material.shoe
                Row(
                    Modifier.fillMaxWidth().then(slotSurface()).padding(10.dp).testTag("changed-row-${shoe.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(72.dp).aspectRatio(1.45f)) { SneakerVisual(shoe, Modifier.fillMaxSize()) }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(shoe.shoeName(), style = runTextStyle(15.sp, t.text, FontWeight.Bold))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ShoeGradeBadge(shoe.tier, decorative = true, modifier = Modifier.align(Alignment.CenterVertically))
                            Text(stringResource(R.string.upg_lv_no, shoe.level, shoe.mintNumber),
                                style = runTextStyle(12.sp, t.label, FontWeight.Medium), modifier = Modifier.align(Alignment.CenterVertically))
                        }
                    }
                    Text(stringResource(R.string.upg_changed_tag), style = runTextStyle(13.sp, t.dangerText, FontWeight.Bold),
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(t.errorFace)
                            .border(1.dp, t.errorEdge, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            Text(stringResource(R.string.upg_changed_note), style = runTextStyle(14.sp, t.label, FontWeight.Medium))
            RunButton(stringResource(R.string.upg_changed_pick), {
                actions.onCloseSheet()
                actions.onOpenPicker()
            }, modifier = Modifier.testTag("changed-pick"))
            RunButton(stringResource(R.string.upg_back), actions.onCloseSheet, kind = RunButtonKind.Secondary,
                modifier = Modifier.testTag("changed-back"))
        }
    }
}

/** UP14 — 요청을 보내지 않았다. 연결 다시 확인은 준비 상태로만 돌아간다(자동 실행 없음) */
@Composable
private fun OfflineSheet(checking: Boolean, actions: UpgradeActions) {
    val t = runTone()
    RunSheet(onDismiss = actions.onCloseSheet, modifier = Modifier.testTag("upgrade-offline")) {
        Icon(Icons.Filled.WifiOff, contentDescription = null, tint = t.dangerText, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        RunSheetText(stringResource(R.string.upg_offline_title), body = stringResource(R.string.upg_offline_body),
            note = stringResource(R.string.upg_offline_note))
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RunButton(stringResource(R.string.upg_back), actions.onCloseSheet, kind = RunButtonKind.Secondary,
                modifier = Modifier.weight(1f).testTag("offline-back"))
            RunButton(stringResource(R.string.upg_offline_retry), actions.onRecheckConnection, busy = checking, enabled = !checking,
                modifier = Modifier.weight(1f).testTag("offline-retry"))
        }
    }
}
