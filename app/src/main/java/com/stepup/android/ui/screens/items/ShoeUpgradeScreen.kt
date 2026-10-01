package com.stepup.android.ui.screens.items

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
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
import com.stepup.android.domain.ShoeTier
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.tier
import com.stepup.android.ui.components.DetailPage
import com.stepup.android.ui.components.GradeArtRatio
import com.stepup.android.ui.components.SettingsPrimaryButton
import com.stepup.android.ui.components.SettingsSecondaryButton
import com.stepup.android.ui.components.SettingsSheet
import com.stepup.android.ui.components.ShoeGradeBadge
import com.stepup.android.ui.components.SneakerGradeStage
import com.stepup.android.ui.components.SneakerGradeThumb
import com.stepup.android.ui.components.SneakerVisual
import com.stepup.android.ui.components.label
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.customize.StatScale
import com.stepup.android.ui.screens.customize.barFraction
import com.stepup.android.ui.theme.StepUpDesign

/*
 * 신발 강화(2026-10-01 지시서 v6, docs/redesign/shoe-upgrade-2026-10-01) — 시안 01 ~ 15 · 17 · 18 의 상태를 한 화면에서 그린다.
 * 강화 본문은 독립 화면이고 재료 선택(02 · 09 · 10 · 11) · 최종 확인(04) · 재료 변경(12) · 연결 확인(14)은 하단 시트 하나씩이다.
 * 성공률 계산 안내 화면(16) · 도움말 · 계산 내역 문장은 없다 — 재료별 추가 성공률 · 최종 성공률 · 소각 안내만.
 * 숫자 · 이름 · 버튼은 모두 Compose 글자다. 신발은 실제 모델 그림과 기존 등급 무대(SneakerGradeStage)를 쓴다.
 */

private object UpgradeColors {
    val Text = CartoonColors.Text
    val Secondary = Color(0xFFA5B5CF)
    val Accent = Color(0xFF47B4FF)
    val Slot = Color(0xFF0F2742)
    val SlotBorder = Color(0xFF2E7BD6)
    val Danger = Color(0xFFFF8F80)
    val Panel = Color(0xFF0D2036)
}

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
            UpgradeButton(stringResource(R.string.upg_running_button), enabled = false, onClick = {}, tag = "upgrade-primary")
            UpgradeLink(stringResource(R.string.upg_later), actions.onBack, "upgrade-later")
        } }
        is UpgradePhase.Unknown -> { {
            UpgradeButton(
                stringResource(if (state.checking) R.string.upg_quoting else R.string.upg_unknown_retry),
                enabled = !state.checking, onClick = actions.onCheckResult, tag = "upgrade-primary",
            )
            UpgradeLink(stringResource(R.string.upg_later), actions.onBack, "upgrade-later")
        } }
        is UpgradePhase.Succeeded -> { {
            UpgradeButton(stringResource(R.string.upg_back_detail), enabled = true, onClick = actions.onBack, tag = "upgrade-primary")
            val after = phase.result.target ?: state.info?.stats
            if (after == null || after.level < after.maxLevel) {
                UpgradeLink(stringResource(R.string.upg_next), actions.onAgain, "upgrade-next")
            }
        } }
        is UpgradePhase.Failed -> { {
            UpgradeButton(stringResource(R.string.upg_reselect), enabled = true, onClick = actions.onAgain, tag = "upgrade-primary")
            UpgradeLink(stringResource(R.string.upg_back_detail), actions.onBack, "upgrade-detail")
        } }
        is UpgradePhase.Blocked -> { {
            when (phase.block) {
                ForgeTargetBlock.MAX_LEVEL ->
                    UpgradeButton(stringResource(R.string.upg_back_detail), enabled = true, onClick = actions.onBack, tag = "upgrade-primary")
                ForgeTargetBlock.LOWEST_GRADE ->
                    UpgradeButton(stringResource(R.string.upg_other_shoe), enabled = true, onClick = actions.onOpenOwned, tag = "upgrade-primary")
                else ->
                    UpgradeButton(stringResource(R.string.upg_pick_owned), enabled = true, onClick = actions.onOpenOwned, tag = "upgrade-primary")
            }
        } }
        else -> null
    }
    DetailPage(title = stringResource(R.string.upg_title), onBack = actions.onBack, bottomBar = bottom) {
        item(key = "body") {
            Column(Modifier.fillMaxWidth().testTag("upgrade-screen")) {
                when (phase) {
                    UpgradePhase.Loading -> LoadingBody()
                    UpgradePhase.LoadFailed -> ShoeStatePanel(
                        icon = Icons.Filled.Refresh,
                        title = stringResource(R.string.upg_load_failed_title),
                        body = stringResource(R.string.upg_load_failed_body),
                        primary = stringResource(R.string.upg_reload) to actions.onRetryLoad,
                        secondary = stringResource(R.string.upg_back_detail) to actions.onBack,
                        modifier = Modifier.testTag("upgrade-load-failed"),
                    )
                    UpgradePhase.SignIn -> ShoeStatePanel(
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
        UpgradeSheet.Offline -> OfflineSheet(actions)
    }
}

// ── 01 · 03 · 08 본문 ──────────────────────────────────────────

/** 선택에 따른 성공률 — 0 ~ 2개는 미리보기(서버가 준 기본 · 보정의 합), 3개는 서버 견적이 있으면 그 값 */
internal fun upgradeRate(state: ShoeUpgradeState): Int? {
    val info = state.info ?: return null
    val applied = state.applied
    state.quote?.takeIf { it.materialIds.sorted() == state.selection.applied.sorted() }?.let { return it.ratePermille }
    return ShoeForge.ratePermille(info.basePermille, applied.map { it.bonusPermille })
}

@Composable
private fun EditingBody(state: ShoeUpgradeState, actions: UpgradeActions) {
    val info = state.info
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TargetHeader(state.shoe, info?.stats?.level, maxLevel = null)
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
        // 성공 시 변화
        if (info != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(stringResource(R.string.upg_changes))
                val next = state.quote?.takeIf { it.materialIds.sorted() == state.selection.applied.sorted() }?.after
                    ?: info.previewNext()
                ChangeRows(before = info.stats, after = next)
            }
        }
        // 성공률 · 소각 안내
        RateLine(
            label = stringResource(if (state.selection.applied.size == ShoeForge.MATERIALS) R.string.upg_rate else R.string.upg_rate_preview),
            permille = upgradeRate(state),
        )
        Text(stringResource(R.string.upg_burn_note), color = UpgradeColors.Secondary, fontSize = 13.sp, lineHeight = 1.4.em,
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
    UpgradeButton(label, enabled = count == ShoeForge.MATERIALS && !state.quoting && state.info != null,
        onClick = actions.onRequestConfirm, tag = "upgrade-primary")
}

// ── 05 · 06 · 07 · 15 · 13 · 17 · 18 ─────────────────────────────

@Composable
private fun RunningBody(state: ShoeUpgradeState) {
    Column(Modifier.fillMaxWidth().testTag("upgrade-running"), verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        CenteredTarget(state.shoe, state.info?.stats?.level)
        Text(stringResource(R.string.upg_running_title), color = UpgradeColors.Text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite; heading() })
        CircularProgressIndicator(color = UpgradeColors.Accent, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
        if (state.running.isNotEmpty()) {
            SectionTitle(stringResource(R.string.upg_running_materials))
            MaterialCards(state.running, locked = false)
        }
        Text(stringResource(R.string.upg_running_note), color = UpgradeColors.Secondary, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SuccessBody(state: ShoeUpgradeState, phase: UpgradePhase.Succeeded) {
    val after = phase.result.target
    val before = phase.before
    val korean = LocalConfiguration.current.locales[0].language == "ko"
    val level = phase.result.levelAfter
    Column(Modifier.fillMaxWidth().testTag("upgrade-success"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.upg_success_kicker), color = UpgradeColors.Secondary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            val levelText = if (korean) "$level" + (if (level % 10 in setOf(2, 4, 5, 9)) "가" else "이") else "$level"
            Text(stringResource(R.string.upg_success_title, levelText), color = UpgradeColors.Text, fontSize = 28.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 1.25.em,
                modifier = Modifier.semantics { heading() }.testTag("upgrade-success-title"))
        }
        TargetHeader(state.shoe, level, after?.maxLevel)
        if (after != null) {
            ChangeRows(before = before ?: after, after = after, animate = before != null)
        }
        Divider()
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.upg_success_burned), color = UpgradeColors.Secondary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.upg_success_burned_body), color = UpgradeColors.Secondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
        if (after != null && after.level >= after.maxLevel) {
            Text(stringResource(R.string.upg_reached_max), color = UpgradeColors.Secondary, fontSize = 14.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().testTag("upgrade-reached-max"))
        }
    }
}

@Composable
private fun FailedBody(state: ShoeUpgradeState, phase: UpgradePhase.Failed) {
    val stats = phase.result.target ?: state.info?.stats
    Column(Modifier.fillMaxWidth().testTag("upgrade-failed"), verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.upg_failed_title), color = UpgradeColors.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.upg_failed_burned), color = UpgradeColors.Danger, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center)
        }
        CenteredTarget(state.shoe, phase.result.levelAfter)
        Text(stringResource(R.string.upg_failed_kept), color = UpgradeColors.Secondary, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (stats != null) StatRows(stats)
    }
}

@Composable
private fun UnknownBody(state: ShoeUpgradeState) {
    val stats = state.info?.stats
    Column(Modifier.fillMaxWidth().testTag("upgrade-unknown"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TargetHeader(state.shoe, stats?.level, maxLevel = null)
        Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.upg_unknown_title), color = UpgradeColors.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.upg_unknown_body), color = UpgradeColors.Secondary, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
        if (state.running.isNotEmpty()) {
            SectionTitle(stringResource(R.string.upg_unknown_materials))
            MaterialCards(state.running, locked = true)
        }
        if (stats != null) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(UpgradeColors.Panel).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.upg_unknown_last), color = UpgradeColors.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                StatRows(stats)
            }
        }
        NoteBox(stringResource(R.string.upg_unknown_note), tag = "upgrade-unknown-note")
    }
}

@Composable
private fun BlockedBody(state: ShoeUpgradeState, block: ForgeTargetBlock) {
    val stats = state.info?.stats
    when (block) {
        ForgeTargetBlock.MAX_LEVEL -> Column(Modifier.fillMaxWidth().testTag("upgrade-max"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TargetHeader(state.shoe, stats?.level, maxLevel = null)
            StateTitle(stringResource(R.string.upg_max_title), stringResource(R.string.upg_max_body))
            if (stats != null) StatRows(stats)
        }
        ForgeTargetBlock.LOWEST_GRADE -> Column(Modifier.fillMaxWidth().testTag("upgrade-lowest"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TargetHeader(state.shoe, stats?.level, maxLevel = null)
            StateTitle(stringResource(R.string.upg_lowest_title), stringResource(R.string.upg_lowest_body))
            NoteBox(stringResource(R.string.upg_lowest_hint), tag = "upgrade-lowest-hint")
        }
        else -> Column(Modifier.fillMaxWidth().testTag("upgrade-unavailable"), verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            if (state.shoe != null && block != ForgeTargetBlock.TARGET_GONE) CenteredTarget(state.shoe, state.shoe.level)
            StateTitle(
                stringResource(R.string.upg_unavailable_title),
                stringResource(if (block == ForgeTargetBlock.LEGACY) R.string.upg_unavailable_legacy else R.string.upg_unavailable_body),
            )
            NoteBox(stringResource(R.string.upg_unavailable_note), tag = "upgrade-unavailable-note", icon = true)
        }
    }
}

@Composable
private fun LoadingBody() {
    Column(Modifier.fillMaxWidth().padding(top = 60.dp).testTag("upgrade-loading"), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CircularProgressIndicator(color = UpgradeColors.Accent, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
        Text(stringResource(R.string.upg_loading), color = UpgradeColors.Secondary, fontSize = 15.sp,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    }
}

// ── 대상 머리 ──────────────────────────────────────────────────

/** 무대(왼쪽) · 이름 · 등급 배지 + No. · Lv.(오른쪽). 이름은 길면 줄을 바꾸고 배지 · 번호는 함께 남는다 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TargetHeader(shoe: Sneaker?, level: Int?, maxLevel: Int?) {
    if (shoe == null) return
    Row(Modifier.fillMaxWidth().testTag("upgrade-target"), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(0.56f).aspectRatio(GradeArtRatio)) { SneakerGradeStage(shoe, Modifier.fillMaxSize()) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(0.44f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(shoe.shoeName(), color = UpgradeColors.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 1.2.em,
                modifier = Modifier.semantics { heading() }.testTag("upgrade-target-name"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ShoeGradeBadge(shoe.tier, Modifier.align(Alignment.CenterVertically))
                Text(formatShoeNumber(shoe.mintNumber), color = UpgradeColors.Secondary, fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.CenterVertically))
            }
            if (level != null) LevelText(level, maxLevel)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CenteredTarget(shoe: Sneaker?, level: Int?) {
    if (shoe == null) return
    Column(Modifier.fillMaxWidth().testTag("upgrade-target"), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.fillMaxWidth(0.6f).aspectRatio(GradeArtRatio)) { SneakerGradeStage(shoe, Modifier.fillMaxSize()) }
        Text(shoe.shoeName(), color = UpgradeColors.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }.testTag("upgrade-target-name"))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ShoeGradeBadge(shoe.tier, Modifier.align(Alignment.CenterVertically))
            Text(formatShoeNumber(shoe.mintNumber), color = UpgradeColors.Secondary, fontSize = 14.sp,
                modifier = Modifier.align(Alignment.CenterVertically))
        }
        if (level != null) LevelText(level, null)
    }
}

@Composable
private fun LevelText(level: Int, maxLevel: Int?) {
    Text(
        if (maxLevel != null) stringResource(R.string.upg_level_of, level, maxLevel) else stringResource(R.string.upg_level, level),
        color = Color(0xFFBFC9F5), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("upgrade-target-level"),
    )
}

// ── 재료 슬롯 · 카드 ────────────────────────────────────────────

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

private val SlotShape = RoundedCornerShape(12.dp)

@Composable
private fun EmptySlot(index: Int, onAdd: () -> Unit) {
    val description = stringResource(R.string.upg_add_cd, index + 1)
    Column(
        Modifier.fillMaxWidth().heightIn(min = 132.dp).clip(SlotShape).background(UpgradeColors.Slot)
            .border(BorderStroke(1.5.dp, UpgradeColors.SlotBorder), SlotShape)
            .feedbackClickable(role = Role.Button, onClick = onAdd)
            .semantics { contentDescription = description }
            .testTag("upgrade-slot-add-$index"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = UpgradeColors.Accent, modifier = Modifier.size(40.dp))
        Text(stringResource(R.string.upg_add), color = UpgradeColors.Secondary, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun FilledSlot(material: ForgeMaterial, onRemove: (Long) -> Unit) {
    val shoe = material.shoe
    val name = shoe.shoeName()
    Box(Modifier.fillMaxWidth().clip(SlotShape).background(UpgradeColors.Slot).border(BorderStroke(1.dp, UpgradeColors.SlotBorder), SlotShape)) {
        MaterialCardBody(material, compact = true, locked = false)
        val removeCd = stringResource(R.string.upg_remove_cd, name)
        Box(
            Modifier.align(Alignment.TopEnd).size(StepUpDesign.TouchTarget)
                .feedbackClickable(role = Role.Button) { onRemove(shoe.id) }
                .semantics { contentDescription = removeCd }
                .testTag("upgrade-remove-${shoe.id}"),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xCC0A1A2C)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = UpgradeColors.Text, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** 05 · 15 · 04 의 재료 카드 셋(누를 수 없음) */
@Composable
private fun MaterialCards(materials: List<ForgeMaterial>, locked: Boolean, showName: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        materials.take(ShoeForge.MATERIALS).forEach { material ->
            Box(Modifier.weight(1f).clip(SlotShape).background(UpgradeColors.Slot).border(BorderStroke(1.dp, UpgradeColors.SlotBorder), SlotShape)) {
                MaterialCardBody(material, compact = true, locked = locked, showName = showName, showBonus = !showName)
            }
        }
        repeat((ShoeForge.MATERIALS - materials.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun MaterialCardBody(material: ForgeMaterial, compact: Boolean, locked: Boolean, showName: Boolean = false, showBonus: Boolean = true) {
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
            SneakerVisual(shoe, Modifier.fillMaxSize().alpha(if (locked) 0.6f else 1f))
            if (locked) Icon(Icons.Filled.Lock, contentDescription = null, tint = UpgradeColors.Text, modifier = Modifier.size(26.dp))
        }
        if (showName) {
            Text(name, color = UpgradeColors.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, lineHeight = 1.25.em)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ShoeGradeBadge(shoe.tier, decorative = true)
            Column {
                Text(stringResource(R.string.upg_level, shoe.level), color = UpgradeColors.Text, fontSize = 12.sp, maxLines = 1)
                Text(formatShoeNumber(shoe.mintNumber), color = UpgradeColors.Secondary, fontSize = 11.sp, maxLines = 1)
            }
        }
        if (showBonus) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF0A1A2C)).padding(vertical = 4.dp),
                contentAlignment = Alignment.Center) {
                Text(bonus, color = UpgradeColors.Accent, fontSize = if (compact) 16.sp else 18.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, softWrap = false, modifier = Modifier.testTag("upgrade-bonus-${shoe.id}"))
            }
        }
    }
}

// ── 변화 줄 ───────────────────────────────────────────────────

private enum class UpgradeStat { LEVEL, EFFICIENCY, COMFORT }

private class ChangeRow(val stat: UpgradeStat, val now: Float, val next: Float?, val value: androidx.compose.ui.text.AnnotatedString, val plain: String)

private val NextLevelColor = Color(0xFFFFCB45)
private val NextEfficiencyColor = Color(0xFF4FB4FF)
private val NextComfortColor = Color(0xFFC3A1FF)

private fun eff(bps: Int) = formatBonus(bps / 100.0)
private fun cmf(bps: Int) = formatPercent(bps / 100.0)

/** 레벨 · 효율 · 착화감 — 현재값 → 다음값. 바 길이는 실제 값 ÷ 표시 스케일(레벨 상한 · 27.5% · 20%) */
@Composable
private fun ChangeRows(before: ForgeStats, after: ForgeStats, animate: Boolean = false) {
    val rows = listOf(
        ChangeRow(
            UpgradeStat.LEVEL, barFraction(before.level.toDouble(), after.maxLevel.toDouble()),
            barFraction(after.level.toDouble(), after.maxLevel.toDouble()),
            buildAnnotatedString {
                append("${before.level} → ")
                withStyle(SpanStyle(color = NextLevelColor, fontWeight = FontWeight.Bold)) { append("${after.level}") }
                withStyle(SpanStyle(color = UpgradeColors.Secondary)) { append(" / ${after.maxLevel}") }
            },
            "${before.level} → ${after.level} / ${after.maxLevel}",
        ),
        ChangeRow(
            UpgradeStat.EFFICIENCY, barFraction(before.efficiencyPercent, StatScale.EFFICIENCY_MAX_PERCENT),
            barFraction(after.efficiencyPercent, StatScale.EFFICIENCY_MAX_PERCENT),
            buildAnnotatedString {
                append("${eff(before.efficiencyBps)} → ")
                withStyle(SpanStyle(color = NextEfficiencyColor, fontWeight = FontWeight.Bold)) { append(eff(after.efficiencyBps)) }
            },
            "${eff(before.efficiencyBps)} → ${eff(after.efficiencyBps)}",
        ),
        ChangeRow(
            UpgradeStat.COMFORT, barFraction(before.comfortPercent, StatScale.ENERGY_MAX_PERCENT),
            barFraction(after.comfortPercent, StatScale.ENERGY_MAX_PERCENT),
            buildAnnotatedString {
                append("${cmf(before.comfortBps)} → ")
                withStyle(SpanStyle(color = NextComfortColor, fontWeight = FontWeight.Bold)) { append(cmf(after.comfortBps)) }
            },
            "${cmf(before.comfortBps)} → ${cmf(after.comfortBps)}",
        ),
    )
    if (animate) {
        // 06 — 결과가 온 뒤 바가 이전 값에서 실제 값으로(약 320ms, 동작 줄이기면 바로)
        val motion = LocalMotion.current
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { shown = true }
        StatTable(rows.map { row ->
            val progress by animateFloatAsState(if (shown) 1f else 0f, tween(motion.duration(320)), label = "upgrade-bar")
            ChangeRow(row.stat, row.now + ((row.next ?: row.now) - row.now) * progress, null, row.value, row.plain)
        })
    } else {
        StatTable(rows)
    }
}

/** 실패 · 최대 레벨 · 결과 미확인 — 지금 값만(증가 예정 구간 없음) */
@Composable
private fun StatRows(stats: ForgeStats) {
    StatTable(listOf(
        ChangeRow(UpgradeStat.LEVEL, barFraction(stats.level.toDouble(), stats.maxLevel.toDouble()), null,
            buildAnnotatedString {
                append("${stats.level}")
                withStyle(SpanStyle(color = UpgradeColors.Secondary)) { append(" / ${stats.maxLevel}") }
            }, "${stats.level} / ${stats.maxLevel}"),
        ChangeRow(UpgradeStat.EFFICIENCY, barFraction(stats.efficiencyPercent, StatScale.EFFICIENCY_MAX_PERCENT), null,
            buildAnnotatedString { append(eff(stats.efficiencyBps)) }, eff(stats.efficiencyBps)),
        ChangeRow(UpgradeStat.COMFORT, barFraction(stats.comfortPercent, StatScale.ENERGY_MAX_PERCENT), null,
            buildAnnotatedString { append(cmf(stats.comfortBps)) }, cmf(stats.comfortBps)),
    ))
}

private val RowLabelStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
private val RowValueStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum")

/** 항목명 — 입체 바 — 값 한 줄. 이름 · 값 열은 세 줄 중 가장 긴 글에 맞춘다. 바가 56dp 보다 좁아지면 바를 글 아래로 */
@Composable
private fun StatTable(rows: List<ChangeRow>) {
    val labels = rows.map { stringResource(statLabelRes(it.stat)) }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val base = LocalTextStyle.current
    val labelStyle = remember(base) { base.merge(RowLabelStyle) }
    val valueStyle = remember(base) { base.merge(RowValueStyle) }
    val (labelWidth, valueWidth) = remember(labels, rows.map { it.plain }, labelStyle, valueStyle, density) {
        fun widest(texts: List<String>, style: TextStyle): Dp = with(density) {
            (texts.maxOf { measurer.measure(it, style, softWrap = false).size.width } + 1).toDp()
        }
        maxOf(48.dp, widest(labels, labelStyle)) to widest(rows.map { it.plain }, valueStyle)
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = maxWidth - 24.dp - 16.dp - labelWidth - valueWidth < 56.dp
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEachIndexed { index, row ->
                val tag = "upgrade-row-" + row.stat.name.lowercase()
                val cd = stringResource(R.string.upg_bar_cd, labels[index], row.plain)
                val cell = Modifier.fillMaxWidth().heightIn(min = 52.dp).background(CartoonColors.Cell, RoundedCornerShape(14.dp))
                    .semantics(mergeDescendants = true) { contentDescription = cd }
                    .testTag(tag)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                val fill = when (row.stat) {
                    UpgradeStat.LEVEL -> CartoonColors.Level
                    UpgradeStat.EFFICIENCY -> CartoonColors.Efficiency
                    UpgradeStat.COMFORT -> CartoonColors.Comfort
                }
                if (stacked) {
                    Column(cell, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(labels[index], style = labelStyle, color = UpgradeColors.Text, modifier = Modifier.weight(1f))
                            Text(row.value, style = valueStyle, color = UpgradeColors.Text, modifier = Modifier.testTag("$tag-value"))
                        }
                        CartoonStatBar(row.now, fill, Modifier.fillMaxWidth().height(28.dp), next = row.next)
                    }
                } else {
                    Row(cell, verticalAlignment = Alignment.CenterVertically) {
                        Text(labels[index], style = labelStyle, color = UpgradeColors.Text, maxLines = 1, softWrap = false,
                            modifier = Modifier.width(labelWidth))
                        Spacer(Modifier.width(8.dp))
                        CartoonStatBar(row.now, fill, Modifier.weight(1f).height(28.dp).testTag("$tag-bar"), next = row.next)
                        Spacer(Modifier.width(8.dp))
                        Text(row.value, style = valueStyle, color = UpgradeColors.Text, maxLines = 1, softWrap = false,
                            textAlign = TextAlign.End, modifier = Modifier.width(valueWidth).testTag("$tag-value"))
                    }
                }
            }
        }
    }
}

private fun statLabelRes(stat: UpgradeStat): Int = when (stat) {
    UpgradeStat.LEVEL -> R.string.upg_stat_level
    UpgradeStat.EFFICIENCY -> R.string.upg_stat_efficiency
    UpgradeStat.COMFORT -> R.string.upg_stat_comfort
}

// ── 공용 조각 ──────────────────────────────────────────────────

@Composable
private fun SectionTitle(title: String, subtitle: String? = null, trailing: String? = null, trailingTag: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(title, color = UpgradeColors.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
            if (subtitle != null) Text(subtitle, color = Color(0xFF8FA6E8), fontSize = 14.sp)
        }
        if (trailing != null) {
            Text(trailing, color = UpgradeColors.Accent, fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
                modifier = if (trailingTag != null) Modifier.testTag(trailingTag) else Modifier)
        }
    }
}

@Composable
private fun RateLine(label: String, permille: Int?) {
    Column {
        Divider()
        Row(Modifier.fillMaxWidth().padding(top = 12.dp).semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color(0xFF8FA6E8), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            // 조회되지 않은 값은 0% 로 보이지 않는다
            Text(permille?.let(ShoeForge::formatRate) ?: stringResource(R.string.upg_rate_unknown), color = UpgradeColors.Accent,
                fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("upgrade-rate"))
        }
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1D3A5C)))
}

@Composable
private fun StateTitle(title: String, body: String) {
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = UpgradeColors.Text, fontSize = 23.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            lineHeight = 1.3.em, modifier = Modifier.semantics { heading() })
        Text(body, color = UpgradeColors.Secondary, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 1.45.em)
    }
}

@Composable
private fun NoteBox(text: String, tag: String, icon: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(UpgradeColors.Panel)
            .border(BorderStroke(1.dp, Color(0xFF1D3A5C)), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = UpgradeColors.Secondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = Color(0xFF8FA6E8), fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

/** 흰 큰 버튼(56dp) — 누를 수 없으면 어두운 면 */
@Composable
private fun UpgradeButton(label: String, enabled: Boolean, onClick: () -> Unit, tag: String) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape)
            .background(if (enabled) Color(0xFFF7F9FD) else Color(0xFF34506F), shape)
            .feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp).testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (enabled) Color(0xFF0B1A2E) else Color(0xFFA9BAD3), fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center)
    }
}

/** 버튼 아래 글자 버튼(나중에 확인 · 다음 레벨 강화 · 신발 상세 보기) */
@Composable
private fun UpgradeLink(label: String, onClick: () -> Unit, tag: String) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = StepUpDesign.TouchTarget)
            .feedbackClickable(role = Role.Button, onClick = onClick).testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color(0xFF8FA6E8), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── 시트 ──────────────────────────────────────────────────────

/** 02 · 09 · 10 · 11 — 하나의 재료 선택 시트. 목록과 아래 요약이 같은 임시 선택(소유 id)을 본다 */
@Composable
private fun PickerSheet(state: ShoeUpgradeState, actions: UpgradeActions) {
    val draft = state.selection.draft.orEmpty()
    val load = state.candidates
    val targetRarity = state.info?.rarity ?: state.shoe?.rarity ?: Rarity.LEGENDARY
    val usable = (load as? MaterialsLoad.Ready)?.list?.filter { it.usable }.orEmpty()
    SettingsSheet(
        title = stringResource(R.string.upg_pick_title),
        onDismiss = actions.onCloseSheet,
        modifier = Modifier.testTag("upgrade-picker"),
        actions = {
            when {
                load is MaterialsLoad.Loading ->
                    UpgradeButton(stringResource(R.string.upg_pick_loading_button), enabled = false, onClick = {}, tag = "picker-apply")
                load is MaterialsLoad.Failed -> {
                    UpgradeButton(stringResource(R.string.upg_reload), enabled = true, onClick = actions.onReloadPicker, tag = "picker-reload")
                    UpgradeLink(stringResource(R.string.common_close), actions.onCloseSheet, "picker-close")
                    Text(stringResource(R.string.upg_pick_kept), color = UpgradeColors.Secondary, fontSize = 13.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                usable.isEmpty() && draft.isEmpty() -> {
                    UpgradeButton(stringResource(R.string.upg_pick_owned), enabled = true, onClick = actions.onOpenOwned, tag = "picker-owned")
                    UpgradeLink(stringResource(R.string.common_close), actions.onCloseSheet, "picker-close")
                }
                else -> {
                    val chosen = draft.mapNotNull { id -> state.known[id] }
                    val rate = state.info?.let { ShoeForge.ratePermille(it.basePermille, chosen.map { m -> m.bonusPermille }) }
                    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(if (draft.size == ShoeForge.MATERIALS) R.string.upg_pick_final else R.string.upg_rate_preview),
                            color = UpgradeColors.Text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
                        )
                        Text(rate?.let(ShoeForge::formatRate) ?: stringResource(R.string.upg_rate_unknown), color = UpgradeColors.Accent,
                            fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("picker-rate"))
                    }
                    Text(stringResource(R.string.upg_pick_burn), color = UpgradeColors.Secondary, fontSize = 13.sp)
                    UpgradeButton(
                        if (draft.isEmpty()) stringResource(R.string.upg_pick_apply_none) else stringResource(R.string.upg_pick_apply, draft.size),
                        enabled = draft.isNotEmpty(), onClick = actions.onApply, tag = "picker-apply",
                    )
                }
            }
        },
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (load is MaterialsLoad.Loading) stringResource(R.string.upg_pick_loading)
                else stringResource(R.string.upg_pick_rule, ShoeTier.valueOf(targetRarity.name).label()),
                color = Color(0xFF8FA6E8), fontSize = 15.sp, modifier = Modifier.weight(1f),
            )
            Text(stringResource(R.string.upg_count, draft.size), color = UpgradeColors.Accent, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("picker-count"))
        }
        when (load) {
            MaterialsLoad.Loading -> repeat(3) {
                Box(Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(14.dp)).background(UpgradeColors.Slot).testTag("picker-skeleton"))
            }
            MaterialsLoad.Failed -> Column(Modifier.fillMaxWidth().padding(vertical = 24.dp).testTag("picker-failed"),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(72.dp).clip(CircleShape).background(UpgradeColors.Slot), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.WifiOff, contentDescription = null, tint = UpgradeColors.Secondary, modifier = Modifier.size(34.dp))
                }
                StateTitle(stringResource(R.string.upg_pick_failed_title), stringResource(R.string.upg_pick_failed_body))
            }
            is MaterialsLoad.Ready -> if (usable.isEmpty() && draft.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(vertical = 20.dp).testTag("picker-empty"),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    StateTitle(stringResource(R.string.upg_pick_empty_title), stringResource(R.string.upg_pick_empty_body))
                }
            } else {
                // 쓸 수 있는 것 먼저, 쓸 수 없는 것은 까닭과 함께 흐리게(고를 수 없다)
                val ordered = load.list.filter { it.usable || it.id in draft } + load.list.filterNot { it.usable || it.id in draft }
                ordered.forEach { material ->
                    PickerRow(material, selected = material.id in draft, full = draft.size >= ShoeForge.MATERIALS,
                        onClick = { actions.onToggle(material.id) })
                }
            }
        }
    }
}

@Composable
private fun PickerRow(material: ForgeMaterial, selected: Boolean, full: Boolean, onClick: () -> Unit) {
    val shoe = material.shoe
    val name = shoe.shoeName()
    val usable = material.usable
    val shape = RoundedCornerShape(14.dp)
    val stateText = when {
        !usable -> blockLabel(material.block)
        selected -> stringResource(R.string.upg_pick_selected)
        full -> stringResource(R.string.upg_pick_full)
        else -> stringResource(R.string.upg_pick_not_selected)
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 88.dp).clip(shape).background(UpgradeColors.Slot)
            .border(BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) UpgradeColors.Accent else Color(0xFF1D3A5C)), shape)
            .feedbackClickable(enabled = usable || selected, role = Role.Checkbox, onClick = onClick)
            .semantics { this.selected = selected; stateDescription = stateText }
            .alpha(if (usable || selected) 1f else 0.5f)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("picker-row-${shoe.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(92.dp).aspectRatio(1.45f)) { SneakerVisual(shoe, Modifier.fillMaxSize()) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(name, color = UpgradeColors.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 1.25.em)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ShoeGradeBadge(shoe.tier, decorative = true)
                Text(stringResource(R.string.upg_lv_no, shoe.level, shoe.mintNumber), color = UpgradeColors.Secondary, fontSize = 12.sp)
            }
            if (!usable) Text(blockLabel(material.block), color = UpgradeColors.Danger, fontSize = 12.sp)
        }
        Spacer(Modifier.width(8.dp))
        if (usable) {
            Text(ShoeForge.formatBonus(material.bonusPermille), color = UpgradeColors.Accent, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                softWrap = false)
            Spacer(Modifier.width(8.dp))
        }
        Box(
            Modifier.size(30.dp).clip(CircleShape)
                .background(if (selected) Color(0xFF2B7BFF) else Color.Transparent)
                .border(BorderStroke(1.5.dp, if (selected) Color(0xFF2B7BFF) else Color(0xFF4A6688)), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun blockLabel(block: ForgeMaterialBlock?): String = stringResource(
    when (block) {
        ForgeMaterialBlock.EQUIPPED -> R.string.upg_block_equipped
        ForgeMaterialBlock.LISTED -> R.string.upg_block_listed
        ForgeMaterialBlock.ON_CHAIN -> R.string.upg_block_chain
        ForgeMaterialBlock.LEVEL -> R.string.upg_block_level
        else -> R.string.upg_block_locked
    },
)

/** 04 — 대상 · 소각할 재료 3개 · 최종 성공률 · 소각 안내, 취소 / 강화 시작 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfirmSheet(state: ShoeUpgradeState, quote: ForgeQuote, actions: UpgradeActions) {
    val shoe = state.shoe
    val materials = quote.materialIds.mapNotNull { state.known[it] }
    SettingsSheet(
        title = stringResource(R.string.upg_confirm_title),
        onDismiss = actions.onCloseSheet,
        modifier = Modifier.testTag("upgrade-confirm"),
        actions = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsSecondaryButton(stringResource(R.string.common_cancel), actions.onCloseSheet,
                    Modifier.weight(0.42f).heightIn(min = 56.dp).testTag("confirm-cancel"))
                Box(Modifier.weight(0.58f)) {
                    UpgradeButton(stringResource(R.string.upg_confirm_start), enabled = true, onClick = actions.onStart, tag = "confirm-start")
                }
            }
        },
    ) {
        if (shoe != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(120.dp)) { SneakerGradeThumb(shoe, Modifier.fillMaxWidth()) }
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(shoe.shoeName(), color = UpgradeColors.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShoeGradeBadge(shoe.tier, Modifier.align(Alignment.CenterVertically))
                        Text(formatShoeNumber(shoe.mintNumber), color = UpgradeColors.Secondary, fontSize = 14.sp,
                            modifier = Modifier.align(Alignment.CenterVertically))
                    }
                    Text(
                        buildAnnotatedString {
                            val text = stringResource(R.string.upg_confirm_level, quote.before.level, quote.after.level)
                            val cut = text.lastIndexOf(quote.after.level.toString())
                            if (cut > 0) {
                                append(text.substring(0, cut))
                                withStyle(SpanStyle(color = NextLevelColor, fontWeight = FontWeight.Bold)) { append(text.substring(cut)) }
                            } else append(text)
                        },
                        color = UpgradeColors.Text, fontSize = 18.sp, modifier = Modifier.testTag("confirm-level"),
                    )
                }
            }
        }
        Divider()
        Text(stringResource(R.string.upg_confirm_burn_title), color = UpgradeColors.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        MaterialCards(materials, locked = false, showName = true)
        Divider()
        Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.upg_confirm_rate), color = Color(0xFF8FA6E8), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(14.dp))
            Text(ShoeForge.formatRate(quote.ratePermille), color = UpgradeColors.Accent, fontSize = 38.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("confirm-rate"))
        }
        Text(stringResource(R.string.upg_burn_note) + "\n" + stringResource(R.string.upg_confirm_irreversible),
            color = Color(0xFF8FA6E8), fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 1.45.em,
            modifier = Modifier.fillMaxWidth())
    }
}

/** 12 — 실행 전 검증에서 쓸 수 없게 된 재료(그것만 뺐다). 강화는 시작되지 않았다 */
@Composable
private fun ChangedSheet(removed: List<ForgeMaterial>, actions: UpgradeActions) {
    SettingsSheet(
        title = stringResource(R.string.upg_changed_title),
        onDismiss = actions.onCloseSheet,
        modifier = Modifier.testTag("upgrade-changed"),
        actions = {
            UpgradeButton(stringResource(R.string.upg_changed_pick), enabled = true,
                onClick = { actions.onCloseSheet(); actions.onOpenPicker() }, tag = "changed-pick")
            UpgradeLink(stringResource(R.string.upg_back), actions.onCloseSheet, "changed-back")
        },
    ) {
        Text(stringResource(R.string.upg_changed_body, removed.size.coerceAtLeast(1)), color = Color(0xFF8FA6E8), fontSize = 15.sp)
        removed.forEach { material ->
            val shoe = material.shoe
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(UpgradeColors.Slot).padding(10.dp)
                    .testTag("changed-row-${shoe.id}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(72.dp).aspectRatio(1.45f)) { SneakerVisual(shoe, Modifier.fillMaxSize()) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(shoe.shoeName(), color = UpgradeColors.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ShoeGradeBadge(shoe.tier, decorative = true)
                        Text(stringResource(R.string.upg_lv_no, shoe.level, shoe.mintNumber), color = UpgradeColors.Secondary, fontSize = 12.sp)
                    }
                }
                Text(stringResource(R.string.upg_changed_tag), color = UpgradeColors.Danger, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x33FF8F80)).padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
        Text(stringResource(R.string.upg_changed_note), color = UpgradeColors.Secondary, fontSize = 13.sp)
    }
}

/** 14 — 요청을 보내지 않았다. 연결 다시 확인은 준비 상태로만 돌아간다(자동 실행 없음) */
@Composable
private fun OfflineSheet(actions: UpgradeActions) {
    SettingsSheet(
        title = stringResource(R.string.upg_offline_title),
        onDismiss = actions.onCloseSheet,
        modifier = Modifier.testTag("upgrade-offline"),
        actions = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsSecondaryButton(stringResource(R.string.upg_back), actions.onCloseSheet,
                    Modifier.weight(1f).heightIn(min = 56.dp).testTag("offline-back"))
                Box(Modifier.weight(1f)) {
                    UpgradeButton(stringResource(R.string.upg_offline_retry), enabled = true, onClick = actions.onRecheckConnection, tag = "offline-retry")
                }
            }
        },
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.WifiOff, contentDescription = null, tint = UpgradeColors.Danger, modifier = Modifier.size(40.dp))
        }
        Text(stringResource(R.string.upg_offline_body), color = UpgradeColors.Text, fontSize = 15.sp, textAlign = TextAlign.Center,
            lineHeight = 1.45.em, modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.upg_offline_note), color = UpgradeColors.Secondary, fontSize = 13.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth())
    }
}
