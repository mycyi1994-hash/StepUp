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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.collectAsState
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
import kotlin.math.roundToInt
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.domain.ForgeBar
import com.stepup.android.domain.ForgeKind
import com.stepup.android.domain.ForgeMaterial
import com.stepup.android.domain.ForgeMaterialBlock
import com.stepup.android.domain.ForgePrecious
import com.stepup.android.domain.ForgeXp
import com.stepup.android.domain.XpMaterial
import com.stepup.android.domain.XpQuote
import com.stepup.android.domain.preview
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
 * 신발 강화 — 2026-11-19 부터 경험치 강화(서버 0118, docs/forge-xp-daily-spec-2026-11-19.md 5.1)가 기본이다: 통(지금 → 넣은 뒤, 최소) ·
 * 재료 목록(값 낮은 것부터, 줄마다 +% · ★ · 내구도 · 막힌 까닭) · "남는 신발 넣기" · 주 버튼 하나("경험치 넣기 / 레벨업") ·
 * 아까운 재료는 시트로 한 번 더 · 결과(레벨업 / 경험치 +N%, 실패 없음). 서버에 0118 이 없으면 아래의 예전 확률 강화 화면이 그대로다.
 *
 * 예전 확률 강화 — 파란 톤 v4 강화 전달본(docs/redesign/blue-v4-2026-10/01-packages/stepup-upgrade-blue-claude-v19)의 UP01 ~ UP15 · UP18.
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
    // ── 경험치 강화(0118) ──
    val onXpToggle: (Long) -> Unit = {},
    val onXpBulk: () -> Unit = {},
    val onXpPrimary: () -> Unit = {},
    val onXpConfirm: () -> Unit = {},
    val onDismissNotice: () -> Unit = {},
)

/**
 * [material] 은 뽑기 결과의 "재료로 쓰면 ○○ +N%" 줄에서 왔을 때 골라 둘 신발(서버 번호, 0 이면 없음) — 쓸 수 있을 때만 고른다.
 */
@Composable
fun ShoeUpgradeScreen(
    sneakerId: Long,
    onBack: () -> Unit,
    onOpenOwned: () -> Unit,
    material: Long = 0L,
    viewModel: ShoeUpgradeViewModel = viewModel(
        key = "upgrade-$sneakerId-$material", factory = ShoeUpgradeViewModel.factory(sneakerId, material),
    ),
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
            onXpToggle = viewModel::xpToggle,
            onXpBulk = viewModel::xpBulk,
            onXpPrimary = viewModel::xpPrimary,
            onXpConfirm = viewModel::xpConfirm,
            onDismissNotice = viewModel::dismissNotice,
        ),
    )
}

/** 상태를 받아 그리기만 한다(기기 검사가 시안 장면을 바로 넣어 찍는다) */
@Composable
fun ShoeUpgradeContent(state: ShoeUpgradeState, actions: UpgradeActions) {
    val phase = state.phase
    val bottom: (@Composable ColumnScope.() -> Unit)? = when (phase) {
        UpgradePhase.Editing -> if (state.xp != null) { { XpEditingButton(state, actions) } } else { { EditingButton(state, actions) } }
        is UpgradePhase.XpDone -> { {
            RunButton(stringResource(R.string.upg_back_detail), actions.onBack, modifier = Modifier.testTag("upgrade-primary"))
            if (!phase.result.after.maxed) {
                Spacer(Modifier.height(10.dp))
                RunButton(stringResource(R.string.fxp_done_more), actions.onAgain, kind = RunButtonKind.Secondary,
                    modifier = Modifier.testTag("upgrade-next"))
            }
        } }
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
        // 크루 러닝 중 — 본문의 칸이 돌아가기를 둔다
        is UpgradePhase.Blocked -> if (phase.block == ForgeTargetBlock.RUNNING) null else { {
            if (phase.block == ForgeTargetBlock.MAX_LEVEL) {
                RunButton(stringResource(R.string.upg_back_detail), actions.onBack, modifier = Modifier.testTag("upgrade-primary"))
            } else {
                RunButton(stringResource(R.string.upg_pick_owned), actions.onOpenOwned, modifier = Modifier.testTag("upgrade-primary"))
            }
        } }
        else -> null
    }
    val xpEditing = phase == UpgradePhase.Editing
    DetailPage(
        title = stringResource(R.string.upg_title),
        onBack = actions.onBack,
        trailing = { Spacer(Modifier.size(StepUpDesign.TouchTarget)) },
        bottomBar = bottom,
    ) {
        item(key = "body") {
            Column(Modifier.fillMaxWidth().testTag("upgrade-screen")) {
                when (phase) {
                    UpgradePhase.Editing -> if (state.xp != null) XpEditingHead(state, actions) else EditingBody(state, actions)
                    is UpgradePhase.XpDone -> XpDoneBody(state, phase)
                    UpgradePhase.RunActive -> CareStatePanel(
                        icon = Icons.Outlined.Info,
                        title = stringResource(R.string.fxp_run_active_title),
                        body = stringResource(R.string.fxp_run_active_body),
                        primary = stringResource(R.string.upg_back_detail) to actions.onBack,
                        modifier = Modifier.testTag("upgrade-run-active"),
                    )
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
                    UpgradePhase.Running -> RunningBody(state)
                    is UpgradePhase.Succeeded -> SuccessBody(state, phase)
                    is UpgradePhase.Failed -> FailedBody(state, phase)
                    is UpgradePhase.Unknown -> UnknownBody(state)
                    is UpgradePhase.Blocked -> BlockedBody(state, phase.block, actions)
                }
            }
        }
        // 경험치 강화의 재료 목록 — 내 신발이 많아도 보이는 줄만 그린다
        val catalog = state.xp
        if (xpEditing && catalog != null) {
            val selected = state.xpSelection.ids
            items(catalog.items, key = { "xp-${it.id}" }) { material ->
                XpMaterialRow(
                    material = material,
                    selected = material.id in selected,
                    full = state.xpSelection.full,
                    max = catalog.maxMaterials,
                    precious = material.preciousWith(state.nicknames).isNotEmpty(),
                    onClick = { actions.onXpToggle(material.id) },
                )
            }
            item(key = "xp-foot") { XpEditingFoot(state) }
        }
    }

    when (val sheet = state.sheet) {
        UpgradeSheet.None -> Unit
        UpgradeSheet.Picker -> PickerSheet(state, actions)
        is UpgradeSheet.Confirm -> ConfirmSheet(state, sheet.quote, actions)
        is UpgradeSheet.Changed -> ChangedSheet(sheet.removed, actions)
        UpgradeSheet.Offline -> OfflineSheet(state.checking, actions)
        is UpgradeSheet.Rejected -> RejectedSheet(sheet.code, xpMax = state.xp?.maxMaterials?.takeIf { state.xpMode }, actions)
        is UpgradeSheet.Precious -> PreciousSheet(sheet, actions)
        is UpgradeSheet.XpChanged -> XpChangedSheet(sheet, actions)
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
    val rules = rememberShoeRules()
    val minDurability = rules.forgeMaterialMinDurability.roundToInt()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TargetHeader(state.shoe, info?.stats?.level?.let { stringResource(R.string.upg_level, it) })
        // 강화 재료 n / 3
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(
                title = stringResource(R.string.upg_materials),
                // 재료는 한 등급 아래 이상 · 내구도 설정 이상(경제 4판 — 서버 설정, 0118 부터 예전 길은 가득만)
                subtitle = when {
                    minDurability >= 100 -> stringResource(R.string.fxp_old_rule_full)
                    minDurability > 0 -> stringResource(R.string.v4s_upg_materials_rule, minDurability)
                    else -> stringResource(R.string.upg_materials_rule)
                },
                trailing = stringResource(R.string.upg_count, state.selection.applied.size),
                trailingTag = "upgrade-count",
            )
            MaterialSlots(state.applied, onAdd = actions.onOpenPicker, onRemove = actions.onRemove)
        }
        // 성공 시 변화 — 레벨 · 효율 · 착화감 세 줄(내구도 없음). 한 레벨이 주는 것(경제 4판): 스탯 포인트(등급마다) · 하루 에너지
        // (규칙이 레벨마다 늘릴 때만 — 서버 0119 부터는 없다) · 5의 배수 레벨이면 내구도 가득 · 상급 상자.
        // 효율 · 착화감은 저절로 오르지 않는다(포인트를 나눠야)
        if (info != null) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(stringResource(R.string.upg_changes))
                val next = state.quote?.takeIf { it.materialIds.sorted() == state.selection.applied.sorted() }?.after
                    ?: info.previewNext(rules)
                UpgradeStatRows(forgeRows(info.stats, next, info.rarity, rules), next.maxLevel, Modifier.testTag("upgrade-rows"))
                val boxes = rememberBoxStatus()
                ForgeRewardLines(
                    forgeRewards(info.rarity, next.level, rules, boxes?.levelsGot).map { reward ->
                        // 서버 견적이 보낸 포인트가 있으면 그 값
                        if (reward is ForgeReward.Points) ForgeReward.Points(forgePointsGain(info.stats, next, info.rarity, rules)) else reward
                    },
                )
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
        CenteredTarget(state.shoe, state.info?.stats?.level ?: state.xp?.target?.level)
        Headline(stringResource(if (state.xp != null) R.string.fxp_running_title else R.string.upg_running_title), body = null)
        RunSpinner(Modifier.size(44.dp))
        if (state.xpRunning.isNotEmpty()) {
            SectionTitle(stringResource(R.string.upg_running_materials))
            XpMaterialThumbs(state.xpRunning, crits = emptyMap(), locked = false)
        } else if (state.running.isNotEmpty()) {
            SectionTitle(stringResource(R.string.upg_running_materials))
            MaterialCards(state.running, locked = false)
        }
        Text(stringResource(R.string.upg_running_note), style = runTextStyle(15.sp, t.label, FontWeight.Medium),
            textAlign = TextAlign.Center)
    }
}

/**
 * 강화로 받는(받은) 것 한 줄 — 나누기는 신발 상세에서 한다. [strong] 이면 앞세운다(레벨업의 주인공은 포인트 — 2026-11-21)
 */
@Composable
private fun PointsGainLine(text: String, tag: String, strong: Boolean = false) {
    val t = runTone()
    Text(
        text,
        style = if (strong) runTextStyle(17.sp, t.cyan, FontWeight.Bold) else runTextStyle(14.sp, t.label, FontWeight.Medium),
        modifier = Modifier.testTag(tag),
    )
}

/** 지금 값만(실패 · 결과 미확인 · 막힘) — 막대 끝은 그 등급에서 가장 잘 뽑힌 새 신발 */
@Composable
private fun CurrentStatRows(state: ShoeUpgradeState, stats: com.stepup.android.domain.ForgeStats) {
    val rarity = state.shoe?.rarity ?: com.stepup.android.domain.Rarity.COMMON
    UpgradeStatRows(forgeRows(stats, null, rarity, rememberShoeRules()), stats.maxLevel, Modifier.testTag("upgrade-rows"), previewBars = false)
}

/** 상자 현황(이미 상자를 받은 레벨) — 앱이 준비되지 않았거나 아직 못 읽었으면 null */
@Composable
private fun rememberBoxStatus(): com.stepup.android.domain.BoxStatus? {
    val flow = androidx.compose.runtime.remember {
        runCatching { com.stepup.android.core.ServiceLocator.shoeRules.boxes }.getOrNull()
    } ?: return null
    return flow.collectAsState().value
}

/**
 * "성공하면" 아래 한 줄씩 — 스탯 포인트 +N · 하루 에너지(규칙이 레벨마다 늘릴 때만, 가진 신발 중 가장 높은 레벨일 때) · Lv5 · 10 …이면
 * 내구도 가득 · 상급 상자, 끝에 포인트가 하는 일 한 줄([StatNote]). 상자는 계정마다 그 레벨에 처음 닿을 때만이라 그렇게 말한다
 * (받는다고 단정하지 않는다).
 */
@Composable
private fun ForgeRewardLines(rewards: List<ForgeReward>) {
    val t = runTone()
    Column(Modifier.fillMaxWidth().testTag("upgrade-rewards"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rewards.forEach { reward ->
            val (text, tag) = when (reward) {
                is ForgeReward.Points -> stringResource(R.string.upg_points_gain, reward.count) to "upgrade-points-gain"
                is ForgeReward.Energy -> stringResource(R.string.v4s_upg_gain_energy, formatEnergy(reward.amount)) to "upgrade-gain-energy"
                is ForgeReward.Milestone -> stringResource(
                    if (reward.boxAlreadyGot) R.string.v4s_upg_gain_milestone_got else R.string.v4s_upg_gain_milestone, reward.level,
                ) to "upgrade-gain-milestone"
            }
            Text("· $text", style = runTextStyle(14.sp, if (reward is ForgeReward.Milestone) t.cyan else t.label, FontWeight.Medium),
                modifier = Modifier.testTag(tag))
        }
        StatNote()
    }
}

/**
 * 받은 포인트가 하는 일 한 줄 — 서버 0119: 효율에 넣으면 더 빨리 벌고 신발도 빨리 닳는다 · 가득 수리비는 그대로.
 * 0119 전 서버(규칙 묶음이 없다)면 예전 글 "효율을 올리면 수리비도 효율만큼 올라요"
 */
@Composable
private fun StatNote() {
    val newMeaning = newStatMeaning(rememberShoeRules())
    Text(
        stringResource(if (newMeaning) R.string.ers_upg_stat_note else R.string.v4s_upg_repair_note),
        style = runTextStyle(13.sp, runTone().label, FontWeight.Medium), modifier = Modifier.testTag("upgrade-repair-note"),
    )
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
            val rules = rememberShoeRules()
            val rarity = state.shoe?.rarity ?: com.stepup.android.domain.Rarity.COMMON
            val gained = before?.let { forgePointsGain(it, after, rarity, rules) }
                ?: com.stepup.android.domain.StatPoints.gainAt(rarity, level, rules)
            // 레벨업의 주인공은 포인트 — 맨 앞에, 크게
            PointsGainLine(stringResource(R.string.upg_points_got, gained), "upgrade-points-got", strong = true)
            UpgradeStatRows(forgeRows(before ?: after, after, rarity, rules), after.maxLevel, Modifier.testTag("upgrade-rows"))
            // 5 · 10 · 15 …레벨에 닿았다 — 그 신발 포인트를 한 번 다시 나눌 수 있다(서버 0119)
            if (respecEarned(before?.level ?: (level - 1), level, respecEveryOf(rules))) {
                PointsGainLine(stringResource(R.string.ers_upg_respec), "upgrade-got-respec")
            }
            // 5 · 10 · 15 …레벨 — 서버가 그 신발 내구도를 가득 채웠다. 상급 상자는 이 레벨에 처음 닿은 계정만(단정하지 않는다)
            if (level % 5 == 0 && (before == null || before.level / 5 < level / 5)) {
                PointsGainLine(stringResource(R.string.v4s_upg_got_milestone), "upgrade-got-milestone")
            }
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
        if (stats != null) CurrentStatRows(state, stats)
    }
}

@Composable
private fun UnknownBody(state: ShoeUpgradeState) {
    val t = runTone()
    val stats = state.info?.stats ?: state.xp?.target?.let { targetStats(state.shoe, it.level, it.maxLevel) }
    val xpRequest = (state.phase as? UpgradePhase.Unknown)?.pending?.kind == ForgeKind.XP
    Column(Modifier.fillMaxWidth().testTag("upgrade-unknown"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TargetHeader(state.shoe, stats?.level?.let { stringResource(R.string.upg_level, it) })
        Headline(stringResource(R.string.upg_unknown_title), stringResource(R.string.upg_unknown_body))
        if (state.xpRunning.isNotEmpty() && xpRequest) {
            SectionTitle(stringResource(R.string.upg_unknown_materials))
            XpMaterialThumbs(state.xpRunning, crits = emptyMap(), locked = true)
        } else if (state.running.isNotEmpty()) {
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
                CurrentStatRows(state, stats)
            }
        }
        RunNotice(stringResource(if (xpRequest) R.string.fxp_unknown_note else R.string.upg_unknown_note), tag = "upgrade-unknown-note")
    }
}

@Composable
private fun BlockedBody(state: ShoeUpgradeState, block: ForgeTargetBlock, actions: UpgradeActions) {
    // 경험치 강화는 서버가 대상을 막으면 능력치를 보내지 않는다 — 폰의 신발 사본(서버 값)으로
    val stats = state.info?.stats ?: targetStats(state.shoe, null, null)
    when (block) {
        ForgeTargetBlock.RUNNING -> CareStatePanel(
            icon = Icons.Outlined.Info,
            title = stringResource(R.string.fxp_crew_running_title),
            body = stringResource(R.string.fxp_crew_running_body),
            primary = stringResource(R.string.upg_back_detail) to actions.onBack,
            modifier = Modifier.testTag("upgrade-crew-running"),
        )
        ForgeTargetBlock.MAX_LEVEL -> Column(Modifier.fillMaxWidth().testTag("upgrade-max"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // 상한 숫자는 서버가 준 값(대개 20 — 금고 신발은 더 낮을 수 있다). 예전에 20을 넘은 레벨은 그대로 보인다
            TargetHeader(state.shoe, stats?.let { stringResource(R.string.upg_level_of, it.level, it.maxLevel) })
            Headline(stringResource(R.string.upg_max_title), stringResource(R.string.upg_max_body))
            if (stats != null) CurrentStatRows(state, stats)
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

/**
 * 재료 슬롯 면 — 위가 조금 밝은 패널 · 둥근 모서리 · 테두리. 아이템칸([ShoeItemSlots])도 같은 면을 쓴다.
 * [edge] 가 아니면 테두리를 그리지 않는다(아이템칸은 점선 테두리를 따로 그린다).
 */
@Composable
internal fun slotSurface(selected: Boolean = false, edge: Boolean = true): Modifier {
    val t = runTone()
    val face = Modifier.clip(SlotShape)
        .background(Brush.verticalGradient(listOf(if (t.dark) Color(0xFF0E3268) else t.panelTop, t.panel)), SlotShape)
    return if (!edge) face else face.border(if (selected) 1.5.dp else 1.dp, if (selected) t.cyan else t.panelEdge, SlotShape)
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

/** 재료가 될 수 없는 까닭 — 서버 코드와 앱만 아는 까닭(안 올린 러닝 · 열어 보지 않은 뽑기)마다 글이 따로 있다 */
@Composable
private fun blockLabel(block: ForgeMaterialBlock?): String = stringResource(
    when (block) {
        ForgeMaterialBlock.GRADE -> R.string.upg_block_grade
        ForgeMaterialBlock.EQUIPPED -> R.string.upg_block_equipped
        ForgeMaterialBlock.LISTED -> R.string.upg_block_listed
        ForgeMaterialBlock.ON_CHAIN -> R.string.upg_block_chain
        ForgeMaterialBlock.LEVEL -> R.string.upg_block_level
        // 예전 길 — 내구도가 설정보다 낮은 신발(0116 은 절반, 0118 부터 가득만). 수리하면 다시 재료가 된다
        ForgeMaterialBlock.WORN -> R.string.v4s_upg_block_worn
        ForgeMaterialBlock.NO_VALUE -> R.string.fxp_block_no_value
        ForgeMaterialBlock.IN_ROOM -> R.string.fxp_block_in_room
        ForgeMaterialBlock.PENDING_RUN -> R.string.fxp_block_pending_run
        ForgeMaterialBlock.UNREVEALED -> R.string.fxp_block_unrevealed
        ForgeMaterialBlock.GONE -> R.string.fxp_block_gone
        ForgeMaterialBlock.LOCKED -> R.string.fxp_block_locked
        ForgeMaterialBlock.TARGET, ForgeMaterialBlock.UNKNOWN, null -> R.string.upg_block_locked
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
                    is MaterialsLoad.Ready -> {
                        if (usable.isEmpty() && draft.isEmpty()) {
                            Column(Modifier.fillMaxWidth().padding(vertical = 24.dp).testTag("picker-empty"),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Headline(stringResource(R.string.upg_pick_empty_title), stringResource(R.string.upg_pick_empty_body))
                            }
                        }
                        // 쓸 수 있는 것 먼저(보정이 큰 순), 쓸 수 없는 것은 까닭과 함께 흐리게(고를 수 없다).
                        // 쓸 수 있는 것이 없어도 내 신발이 있으면 줄마다 까닭("두 등급 이상 낮아요" 등)을 보인다
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

// ─────────────────────────────────────────────────────────────
// 경험치 강화(서버 0118 — docs/forge-xp-daily-spec-2026-11-19.md 2 · 5.1). 숫자는 모두 서버 값이다:
// 통(%) · 재료마다 +% · 견적(최소) · 결과. 견적이 오기 전에는 서버가 준 재료 값만 더해 보이고, 통이 차면 "레벨업"만 말한다.
// ─────────────────────────────────────────────────────────────

/** 하루 적립 상한 배수 "1.36" · "1" — 서버 값 그대로(소수 둘째 자리까지, 끝의 0 은 뺀다) */
internal fun formatCap(value: Double): String =
    java.math.BigDecimal(value).setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

/** 견적(최소)으로 오르는 레벨들의 포인트 합 — 서버 표와 같은 [com.stepup.android.domain.ShoeRules.pointsAt] */
internal fun xpPointsGain(rarity: Rarity, fromLevel: Int, toLevel: Int, rules: com.stepup.android.domain.ShoeRules): Int =
    if (toLevel <= fromLevel) 0 else ((fromLevel + 1)..toLevel).sumOf { rules.pointsAt(rarity.id, it) }

/** 대상 머리 · 통 · 레벨이 오르면 · 재료 머리(목록 줄은 화면이 따로 그린다) */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun XpEditingHead(state: ShoeUpgradeState, actions: UpgradeActions) {
    val catalog = state.xp ?: return
    val target = catalog.target
    val t = runTone()
    val rules = rememberShoeRules()
    val quote = state.liveXpQuote
    val picked = state.xpSelection.ids
    val preview = catalog.preview(picked)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TargetHeader(state.shoe, stringResource(R.string.upg_level_of, target.level, target.maxLevel))
        if (state.notice == UpgradeNotice.QuoteChanged) {
            RunNotice(stringResource(R.string.fxp_quote_changed), kind = com.stepup.android.ui.components.RunNoticeKind.Warn,
                action = stringResource(R.string.common_close), onAction = actions.onDismissNotice, tag = "upgrade-quote-changed")
        }
        XpBarPanel(target.bar, target.maxLevel, quote, preview, picked.isNotEmpty())
        // 레벨이 오르면 — 견적이 레벨업이면 그 레벨까지(최소), 아니면 다음 한 레벨. 효율 · 착화감은 저절로 오르지 않는다(포인트를 나눠야)
        val before = targetStats(state.shoe, target.level, target.maxLevel)
        if (before != null && !target.bar.maxed) {
            val levels = quote?.levels?.takeIf { it > 0 }
            val toLevel = (target.level + (levels ?: 1)).coerceAtMost(target.maxLevel)
            val after = before.copy(level = toLevel)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(stringResource(if (levels != null) R.string.fxp_changes_levels else R.string.fxp_changes_next))
                UpgradeStatRows(forgeRows(before, after, target.rarity, rules), target.maxLevel, Modifier.testTag("upgrade-rows"),
                    levelAtLeast = levels != null)
                val boxes = rememberBoxStatus()
                val points = xpPointsGain(target.rarity, target.level, toLevel, rules)
                val milestones = ((target.level + 1)..toLevel).filter { it % 5 == 0 }
                // 하루 에너지 — 견적이 늘어난다고 할 때만(앞뒤가 같으면 줄이 없다). 견적 전에는 규칙이 레벨마다 늘릴 때만(서버 0119 부터 0)
                val energy = quote?.let { q -> q.energyBefore?.let { b -> q.energyAfter?.takeIf { it > b }?.let { b to it } } }
                val energyEach = energyPerLevel(rules)
                Column(Modifier.fillMaxWidth().testTag("upgrade-rewards"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (points > 0) {
                        Text("· " + stringResource(R.string.fxp_points_gain, points), style = runTextStyle(14.sp, t.label, FontWeight.Medium),
                            modifier = Modifier.testTag("upgrade-points-gain"))
                    }
                    if (energy != null) {
                        Text("· " + stringResource(R.string.fxp_energy_up, energy.first, energy.second),
                            style = runTextStyle(14.sp, t.label, FontWeight.Medium), modifier = Modifier.testTag("upgrade-gain-energy"))
                    } else if (quote == null && energyEach > 0.0) {
                        Text("· " + stringResource(R.string.v4s_upg_gain_energy, formatEnergy(energyEach)),
                            style = runTextStyle(14.sp, t.label, FontWeight.Medium), modifier = Modifier.testTag("upgrade-gain-energy"))
                    }
                    milestones.forEach { level ->
                        val got = boxes?.levelsGot?.contains(level) == true
                        Text("· " + stringResource(if (got) R.string.v4s_upg_gain_milestone_got else R.string.v4s_upg_gain_milestone, level),
                            style = runTextStyle(14.sp, t.cyan, FontWeight.Medium), modifier = Modifier.testTag("upgrade-gain-milestone"))
                    }
                    StatNote()
                }
            }
        }
        RunDivider()
        // 재료 머리 — 고른 수 / 한 번에 넣는 상한 · 남는 신발 넣기 · 대성공 확률(서버 값)
        SectionTitle(
            title = stringResource(R.string.fxp_materials_title),
            subtitle = stringResource(R.string.fxp_materials_rule, catalog.maxMaterials),
            trailing = stringResource(R.string.fxp_count, picked.size, catalog.maxMaterials),
            trailingTag = "upgrade-count",
        )
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            catalog.crit?.let { crit ->
                Row(Modifier.align(Alignment.CenterVertically).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = t.label, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.fxp_crit_line, ForgeXp.formatChance(crit.chance), ForgeXp.formatMult(crit.mult)),
                        style = runTextStyle(13.sp, t.label, FontWeight.Medium), modifier = Modifier.testTag("upgrade-crit-odds"),
                    )
                }
            }
            val canBulk = !target.bar.maxed && !state.xpSelection.full &&
                catalog.items.any { it.usable && it.id !in picked && it.preciousWith(state.nicknames).isEmpty() }
            RunTextAction(stringResource(R.string.fxp_bulk), actions.onXpBulk, enabled = canBulk,
                modifier = Modifier.testTag("upgrade-bulk"))
        }
        if (catalog.items.none { it.usable } && picked.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(vertical = 16.dp).testTag("upgrade-xp-empty"),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Headline(stringResource(R.string.fxp_empty_title), stringResource(R.string.fxp_empty_body))
            }
        }
    }
}

/** 목록 아래 — 만렙 넘침 · 사라짐 안내(주 버튼은 아래 고정 줄) */
@Composable
private fun XpEditingFoot(state: ShoeUpgradeState) {
    val t = runTone()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state.liveXpQuote?.overflowPossible == true) {
            RunNotice(stringResource(R.string.fxp_overflow), kind = com.stepup.android.ui.components.RunNoticeKind.Warn, tag = "upgrade-overflow")
        }
        Text(stringResource(R.string.fxp_burn_note), style = runTextStyle(14.sp, t.label, FontWeight.Medium),
            modifier = Modifier.testTag("upgrade-burn-note"))
    }
}

/**
 * 통 — "경험치 · 39.7% → 73.0%" · 막대(지금 + 넣으면) · "넣으면 최소 +33.3%". 견적이 레벨업이면 "→ 레벨업"과 오르는 레벨(최소).
 * 견적이 오기 전에는 서버가 준 재료 값만 더한다(통이 차면 레벨업만 — 몇 레벨인지는 견적이 말한다).
 */
@Composable
private fun XpBarPanel(bar: ForgeBar, maxLevel: Int, quote: XpQuote?, preview: com.stepup.android.domain.XpPreview?, picked: Boolean) {
    val t = runTone()
    val now = bar.shownTenths
    val nowText = now?.let(ForgeXp::formatPct) ?: stringResource(R.string.upg_rate_unknown)
    val levelUp = stringResource(R.string.fxp_bar_level_up)
    val (afterText, afterFraction, caption) = when {
        quote != null && quote.levels > 0 -> Triple(
            levelUp, 1f,
            if (quote.after.maxed) stringResource(R.string.fxp_bar_after_min, stringResource(R.string.upg_level_of, quote.after.level, maxLevel))
            else stringResource(R.string.fxp_bar_after_levels_min, quote.after.level, ForgeXp.formatPct(quote.after.shownTenths ?: 0)),
        )
        quote != null -> Triple(
            quote.after.shownTenths?.let(ForgeXp::formatPct), quote.after.fraction,
            stringResource(R.string.fxp_bar_after_min, ForgeXp.formatGain(ForgeXp.tenths(quote.gain, bar.need) ?: 0)),
        )
        preview != null -> Triple(
            if (preview.fills) levelUp else ForgeXp.formatPct(preview.afterTenths),
            if (preview.fills) 1f else preview.afterTenths / 1000f,
            stringResource(R.string.fxp_bar_after_min, ForgeXp.formatGain(preview.gainTenths)),
        )
        else -> Triple(null, null, if (picked) null else stringResource(R.string.fxp_bar_pick_hint))
    }
    val label = stringResource(R.string.fxp_bar_title)
    val valueText = if (afterText != null) "$nowText → $afterText" else nowText
    Column(
        Modifier.fillMaxWidth().background(CartoonColors.Cell, RoundedCornerShape(14.dp))
            .semantics(mergeDescendants = true) { contentDescription = "$label $valueText" + (caption?.let { ", $it" } ?: "") }
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("upgrade-xp-bar"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = runTextStyle(16.sp, CartoonColors.Text, FontWeight.SemiBold), modifier = Modifier.weight(1f))
            Text(
                buildAnnotatedString {
                    append(nowText)
                    if (afterText != null) {
                        append(" → ")
                        withStyle(SpanStyle(color = CartoonColors.Level.top, fontWeight = FontWeight.ExtraBold)) { append(afterText) }
                    }
                },
                style = runTextStyle(16.sp, CartoonColors.Text, FontWeight.SemiBold).copy(fontFeatureSettings = "tnum"),
                modifier = Modifier.testTag("upgrade-xp-value"),
            )
        }
        CartoonStatBar(bar.fraction, CartoonColors.Level, Modifier.fillMaxWidth().height(28.dp), preview = afterFraction)
        if (caption != null) {
            Text(caption, style = runTextStyle(14.sp, CartoonColors.Number, FontWeight.Medium), modifier = Modifier.testTag("upgrade-xp-caption"))
        }
    }
}

/** 재료 한 줄 — 그림 · 이름 · 등급 · Lv · ★ · 내구도 · (막힌 까닭 · 아까운 신발), 오른쪽 +% · 체크 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun XpMaterialRow(material: XpMaterial, selected: Boolean, full: Boolean, max: Int, precious: Boolean, onClick: () -> Unit) {
    val t = runTone()
    val shoe = material.shoe
    val usable = material.usable
    val gain = ForgeXp.formatGain(material.tenths)
    val blockText = if (!usable) blockLabel(material.block ?: ForgeMaterialBlock.NO_VALUE) else null
    val stateText = when {
        blockText != null -> blockText
        selected -> stringResource(R.string.upg_pick_selected)
        full -> stringResource(R.string.fxp_row_full, max)
        else -> stringResource(R.string.upg_pick_not_selected)
    }
    val description = stringResource(R.string.fxp_row_cd, shoe.shoeName(), shoe.tier.label(), shoe.level, if (usable) gain else stateText)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 88.dp).then(slotSurface(selected))
            .feedbackClickable(enabled = (usable && !full) || selected, role = Role.Checkbox, onClick = onClick)
            .semantics { this.selected = selected; stateDescription = stateText; contentDescription = description }
            .alpha(if (usable || selected) 1f else 0.5f)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("xp-row-${shoe.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(84.dp).aspectRatio(1.45f)) { SneakerVisual(shoe, Modifier.fillMaxSize()) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(shoe.shoeName(), style = runTextStyle(15.sp, t.text, FontWeight.Bold, 1.25f), maxLines = 2)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ShoeGradeBadge(shoe.tier, decorative = true, modifier = Modifier.align(Alignment.CenterVertically))
                Text(stringResource(R.string.upg_level, shoe.level), style = runTextStyle(13.sp, t.label, FontWeight.Medium),
                    modifier = Modifier.align(Alignment.CenterVertically))
                material.stars?.let { stars ->
                    Text("★".repeat(stars), style = runTextStyle(13.sp, t.cyan, FontWeight.Bold),
                        modifier = Modifier.align(Alignment.CenterVertically).testTag("xp-stars-${shoe.id}"))
                }
                material.durabilityPct?.let { pct ->
                    Text(stringResource(R.string.fxp_row_durability, kotlin.math.floor(pct).toInt().coerceIn(0, 100)),
                        style = runTextStyle(13.sp, t.label, FontWeight.Medium), modifier = Modifier.align(Alignment.CenterVertically))
                }
            }
            when {
                blockText != null -> Text(blockText, style = runTextStyle(13.sp, t.dangerText, FontWeight.SemiBold),
                    modifier = Modifier.testTag("xp-block-${shoe.id}"))
                precious -> Text(stringResource(R.string.fxp_precious_tag), style = runTextStyle(13.sp, CartoonColors.Level.top, FontWeight.SemiBold),
                    modifier = Modifier.testTag("xp-precious-${shoe.id}"))
            }
        }
        Spacer(Modifier.width(8.dp))
        if (usable) {
            Text(gain, style = runTextStyle(16.sp, t.cyan, FontWeight.ExtraBold).copy(fontFeatureSettings = "tnum"), softWrap = false,
                modifier = Modifier.testTag("xp-gain-${shoe.id}"))
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

/** 주 버튼 하나 — 고르기 전 · 확인 중 · 다시 확인 · 경험치 넣기 · 레벨업(견적이 레벨업일 때) */
@Composable
private fun XpEditingButton(state: ShoeUpgradeState, actions: UpgradeActions) {
    val count = state.xpSelection.ids.size
    val quote = state.liveXpQuote
    val label = when {
        count == 0 -> stringResource(R.string.fxp_need_pick)
        state.quoting -> stringResource(R.string.upg_quoting)
        quote == null -> stringResource(R.string.fxp_action_retry)
        quote.levels > 0 -> stringResource(R.string.fxp_action_level_up)
        else -> stringResource(R.string.fxp_action_add)
    }
    RunButton(label, actions.onXpPrimary, enabled = count > 0 && !state.quoting, busy = state.quoting,
        modifier = Modifier.testTag("upgrade-primary"))
}

/** 넣은(넣는 · 확인 중) 재료 — 작은 그림들, 대성공이면 "×2" */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun XpMaterialThumbs(materials: List<XpMaterial>, crits: Map<Long, String>, locked: Boolean) {
    val t = runTone()
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        materials.forEach { m ->
            val crit = crits[m.id]
            val name = m.shoe.shoeName()
            Column(
                Modifier.width(96.dp).then(slotSurface()).padding(6.dp)
                    .semantics(mergeDescendants = true) { contentDescription = listOfNotNull(name, crit).joinToString(", ") }
                    .testTag("xp-thumb-${m.id}"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.fillMaxWidth().aspectRatio(1.45f), contentAlignment = Alignment.Center) {
                    SneakerVisual(m.shoe, Modifier.fillMaxSize().alpha(if (locked) 0.55f else 1f))
                    if (locked) Icon(Icons.Filled.Lock, contentDescription = null, tint = t.text, modifier = Modifier.size(22.dp))
                }
                Text(name, style = runTextStyle(12.sp, t.text, FontWeight.SemiBold, 1.2f), maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                if (crit != null) {
                    Text(crit, style = runTextStyle(12.sp, CartoonColors.Level.top, FontWeight.ExtraBold), maxLines = 1,
                        modifier = Modifier.testTag("xp-crit-${m.id}"))
                }
            }
        }
    }
}

/** 결과 — 레벨업이면 "레벨업! Lv. 3 → Lv. 4"(받은 포인트 · 내구도 가득 · 상자), 아니면 "경험치 +N% · 지금 M%". 실패 화면은 없다 */
@Composable
private fun XpDoneBody(state: ShoeUpgradeState, phase: UpgradePhase.XpDone) {
    val t = runTone()
    val result = phase.result
    val rules = rememberShoeRules()
    val rarity = state.shoe?.rarity ?: state.xp?.target?.rarity ?: Rarity.COMMON
    val maxLevel = state.xp?.target?.maxLevel ?: phase.beforeStats?.maxLevel ?: state.shoe?.server?.maxLevel ?: rarity.maxLevel
    val critMult = state.xp?.crit?.mult
    // 대성공 배수 — 서버가 준 값(목록의 대성공 칸, 없으면 결과의 재료 값에서)
    val crits = result.materials.filter { it.crit }.associate { m ->
        val mult = critMult ?: if (m.own > 0) (m.value - m.inherit).toDouble() / m.own else null
        m.id to (mult?.let { stringResource(R.string.fxp_crit_tag, ForgeXp.formatMult(it)) } ?: stringResource(R.string.fxp_bar_level_up))
    }
    val shown = state.xpRunning.ifEmpty { result.materialIds.mapNotNull { state.xp?.item(it) } }
    Column(Modifier.fillMaxWidth().testTag("upgrade-xp-done"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(if (result.leveledUp) R.string.fxp_done_level_kicker else R.string.fxp_done_xp_kicker),
                style = runTextStyle(17.sp, t.cyan, FontWeight.Bold))
            Spacer(Modifier.height(4.dp))
            val title = if (result.leveledUp) stringResource(R.string.fxp_done_level_title, result.levelBefore, result.levelAfter)
            else stringResource(R.string.fxp_done_xp_title,
                ForgeXp.formatGain(ForgeXp.tenths(result.gain, phase.before?.need ?: result.after.need) ?: 0))
            Text(
                title, textAlign = TextAlign.Center,
                style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 1.25.em,
                    letterSpacing = (-0.02).em, color = t.text),
                modifier = Modifier.semantics { heading() }.testTag("upgrade-xp-done-title"),
            )
            val nowText = result.after.shownTenths?.let(ForgeXp::formatPct)
            if (nowText != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    if (result.leveledUp) stringResource(R.string.fxp_done_now_level, result.levelAfter, nowText)
                    else stringResource(R.string.fxp_done_now, nowText),
                    style = runTextStyle(16.sp, t.label, FontWeight.SemiBold), modifier = Modifier.testTag("upgrade-xp-done-now"),
                )
            }
        }
        TargetHeader(state.shoe, stringResource(R.string.upg_level_of, result.levelAfter, maxLevel))
        // 통 — 레벨이 오르면 새 레벨의 통, 아니면 넣기 전 → 넣은 뒤
        CartoonStatBar(
            if (result.leveledUp) result.after.fraction else phase.before?.fraction ?: result.after.fraction,
            CartoonColors.Level, Modifier.fillMaxWidth().height(28.dp).testTag("upgrade-xp-done-bar"),
            preview = if (result.leveledUp) null else result.after.fraction,
        )
        val before = phase.beforeStats
        if (result.leveledUp && before != null) {
            val after = before.copy(level = result.levelAfter)
            // 레벨업의 주인공은 포인트 — 맨 앞에, 크게
            val gained = forgePointsGain(before, after, rarity, rules)
            if (gained > 0) PointsGainLine(stringResource(R.string.upg_points_got, gained), "upgrade-points-got", strong = true)
            UpgradeStatRows(forgeRows(before, after, rarity, rules), maxLevel, Modifier.testTag("upgrade-rows"))
        }
        // 5 · 10 · 15 …레벨을 지났다 — 그 신발 포인트를 한 번 다시 나눌 수 있다(서버 0119)
        if (result.leveledUp && respecEarned(result.levelBefore, result.levelAfter, respecEveryOf(rules))) {
            PointsGainLine(stringResource(R.string.ers_upg_respec), "upgrade-got-respec")
        }
        if (result.leveledUp && before != null) {
            // 5 · 10 · 15 …레벨을 지났다 — 서버가 그 신발 내구도를 가득 채웠다. 받은 상자는 서버가 알려 준 만큼만
            if (((result.levelBefore + 1)..result.levelAfter).any { it % 5 == 0 }) {
                PointsGainLine(stringResource(R.string.fxp_done_full), "upgrade-got-milestone")
            }
        }
        if (result.levelBoxes.isNotEmpty()) {
            PointsGainLine(stringResource(R.string.fxp_done_boxes, result.levelBoxes.size), "upgrade-got-boxes")
        }
        if (result.crits > 0) {
            val mult = critMult ?: result.materials.firstOrNull { it.crit && it.own > 0 }?.let { (it.value - it.inherit).toDouble() / it.own }
            if (mult != null) {
                Text(stringResource(R.string.fxp_done_crit, result.crits, ForgeXp.formatMult(mult)),
                    style = runTextStyle(15.sp, CartoonColors.Level.top, FontWeight.Bold), modifier = Modifier.testTag("upgrade-crits"))
            }
        }
        if (shown.isNotEmpty()) XpMaterialThumbs(shown, crits, locked = false)
        RunDivider()
        Text(stringResource(R.string.fxp_done_burned), style = runTextStyle(14.sp, t.label, FontWeight.Medium),
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        if (result.overflow > 0) {
            RunNotice(stringResource(R.string.fxp_done_overflow), tag = "upgrade-xp-overflow")
        } else if (result.after.maxed) {
            RunNotice(stringResource(R.string.upg_reached_max), tag = "upgrade-reached-max")
        }
    }
}

/** 아까운 재료 — 까닭마다 한 줄(가장 높은 레벨 · 효율은 서버 견적의 앞뒤 값), 취소 / 그래도 넣기 */
@Composable
private fun PreciousSheet(sheet: UpgradeSheet.Precious, actions: UpgradeActions) {
    val t = runTone()
    val quote = sheet.quote
    RunSheet(onDismiss = actions.onCloseSheet, modifier = Modifier.testTag("upgrade-precious")) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CareSheetTitle(stringResource(R.string.fxp_precious_title), stringResource(R.string.fxp_precious_body))
            sheet.picks.forEach { (material, reasons) ->
                val shoe = material.shoe
                Row(Modifier.fillMaxWidth().then(slotSurface()).padding(10.dp).testTag("precious-row-${shoe.id}"),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(72.dp).aspectRatio(1.45f)) { SneakerVisual(shoe, Modifier.fillMaxSize()) }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(shoe.shoeName(), style = runTextStyle(15.sp, t.text, FontWeight.Bold))
                        reasons.forEach { reason ->
                            Text("· " + preciousLabel(reason, quote), style = runTextStyle(13.sp, CartoonColors.Level.top, FontWeight.SemiBold),
                                modifier = Modifier.testTag("precious-why-${shoe.id}-${reason.name.lowercase()}"))
                        }
                    }
                }
            }
            if (quote.overflowPossible) {
                RunNotice(stringResource(R.string.fxp_overflow), kind = com.stepup.android.ui.components.RunNoticeKind.Warn, tag = "precious-overflow")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RunButton(stringResource(R.string.common_cancel), actions.onCloseSheet, kind = RunButtonKind.Secondary,
                    modifier = Modifier.weight(0.4f).testTag("precious-cancel"))
                RunButton(stringResource(R.string.fxp_precious_confirm), actions.onXpConfirm,
                    modifier = Modifier.weight(0.6f).testTag("precious-confirm"))
            }
        }
    }
}

@Composable
private fun preciousLabel(reason: ForgePrecious, quote: XpQuote): String = when (reason) {
    ForgePrecious.HIGHER_GRADE -> stringResource(R.string.fxp_why_higher_grade)
    ForgePrecious.LEVELED -> stringResource(R.string.fxp_why_leveled)
    ForgePrecious.STARS3 -> stringResource(R.string.fxp_why_stars3)
    ForgePrecious.LAST_MODEL -> stringResource(R.string.fxp_why_last_model)
    ForgePrecious.SELL_UNLOCKED -> stringResource(R.string.fxp_why_sell_unlocked)
    ForgePrecious.TOP_LEVEL -> {
        val before = quote.energyBefore
        val after = quote.energyAfter
        when {
            before != null && after != null && after < before -> stringResource(R.string.fxp_why_top_level, before, after)
            // 하루 에너지가 레벨과 상관없는 규칙(서버 0119)이면 에너지가 준다고 말하지 않는다
            energyPerLevel(rememberShoeRules()) <= 0.0 -> stringResource(R.string.ers_why_top_level)
            else -> stringResource(R.string.fxp_why_top_level_plain)
        }
    }
    ForgePrecious.TOP_EFFICIENCY -> {
        val before = quote.capBefore
        val after = quote.capAfter
        if (before != null && after != null && after < before) stringResource(R.string.fxp_why_top_efficiency, formatCap(before), formatCap(after))
        else stringResource(R.string.fxp_why_top_efficiency_plain)
    }
    ForgePrecious.NICKNAMED -> stringResource(R.string.fxp_why_nicknamed)
    ForgePrecious.OTHER -> stringResource(R.string.fxp_why_other)
}

/** 서버가 쓸 수 없다고 확인해 뺀 재료(까닭과 함께) — 넣지 않았다 */
@Composable
private fun XpChangedSheet(sheet: UpgradeSheet.XpChanged, actions: UpgradeActions) {
    val t = runTone()
    RunSheet(onDismiss = actions.onCloseSheet, modifier = Modifier.testTag("upgrade-xp-changed")) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CareSheetTitle(
                stringResource(R.string.fxp_changed_title),
                if (sheet.kind == XpChange.NO_VALUE) stringResource(R.string.fxp_changed_no_value_body)
                else stringResource(R.string.fxp_changed_body, sheet.removed.size.coerceAtLeast(1)),
            )
            sheet.removed.forEach { material ->
                val shoe = material.shoe
                Row(Modifier.fillMaxWidth().then(slotSurface()).padding(10.dp).testTag("xp-changed-row-${shoe.id}"),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(72.dp).aspectRatio(1.45f)) { SneakerVisual(shoe, Modifier.fillMaxSize()) }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(shoe.shoeName(), style = runTextStyle(15.sp, t.text, FontWeight.Bold))
                        Text(blockLabel(material.block), style = runTextStyle(13.sp, t.dangerText, FontWeight.SemiBold))
                    }
                }
            }
            Text(stringResource(R.string.fxp_changed_note), style = runTextStyle(14.sp, t.label, FontWeight.Medium))
            RunButton(stringResource(R.string.upg_back), actions.onCloseSheet, modifier = Modifier.testTag("xp-changed-back"))
        }
    }
}

/** 서버가 거절했다 — 연결 끊김이 아니다. 코드마다 글을 고르고, 모르는 코드는 "잠시 뒤 다시" */
@Composable
private fun RejectedSheet(code: String, xpMax: Int?, actions: UpgradeActions) {
    val t = runTone()
    val body = when {
        // "한 번에 1 ~ N켤레"(서버 값)는 경험치 강화의 규칙 — 예전 강화(3켤레)에는 일반 글
        code == ShoeUpgradeViewModel.REJECT_INVALID && xpMax != null -> stringResource(R.string.fxp_rej_invalid, xpMax)
        code == ShoeUpgradeViewModel.REJECT_KEY_REUSED -> stringResource(R.string.fxp_rej_key)
        else -> stringResource(R.string.fxp_rej_other)
    }
    RunSheet(onDismiss = actions.onCloseSheet, modifier = Modifier.testTag("upgrade-rejected")) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = t.label, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        RunSheetText(stringResource(R.string.fxp_rejected_title), body = body, note = stringResource(R.string.fxp_rejected_note))
        Spacer(Modifier.height(20.dp))
        RunButton(stringResource(R.string.upg_back), actions.onCloseSheet, modifier = Modifier.testTag("rejected-back"))
    }
}
