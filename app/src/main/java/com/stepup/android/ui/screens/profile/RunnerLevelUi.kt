package com.stepup.android.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.RunnerLevel
import com.stepup.android.domain.RunnerLevelNotice
import com.stepup.android.ui.components.RimKind
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.RunSheetText
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.components.stepUpRim
import com.stepup.android.ui.experience.feedbackClickable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale

/*
 * 러너 레벨(서버 0121, docs/runner-level-contract-2026-11-21.md) — 내 정보 카드 · 설명 시트 · 레벨이 올랐다는 알림 · 이름 옆 표시.
 * 숫자는 모두 서버 `runner_me` · `runner_badges` 값이다. 서버가 러너 레벨을 주지 않으면(함수가 없음 · 꺼짐 · 아직 못 읽음) 아무것도 그리지 않는다.
 */

private val NoRunnerLevel: StateFlow<RunnerLevel?> = MutableStateFlow(null)

/** 화면이 보는 러너 레벨 — 서버가 켠 값을 받았을 때만, 아니면 null(칸을 숨기고 신발 화면은 예전 글) */
@Composable
fun rememberRunnerLevel(): RunnerLevel? {
    val flow = remember { runCatching { ServiceLocator.runnerLevel.level }.getOrNull() ?: NoRunnerLevel }
    return flow.collectAsState().value
}

/** 하루 에너지를 러너 레벨(걸음)이 정한다 — 신발 레벨로 에너지가 바뀐다고 말하지 않는다 */
@Composable
fun rememberRunnerEnergy(): Boolean = rememberRunnerLevel() != null

private fun steps(n: Long): String = String.format(Locale.ROOT, "%,d", n)

// ── 내 정보 카드 ─────────────────────────────────────────────────────

/**
 * 러너 Lv N(· LvN 배지) · 다음 레벨까지 막대 · "다음 레벨까지 N걸음" · "하루 에너지 N분(· 다음 레벨 N분)". 누르면 설명 시트.
 * 다음 레벨 분은 실제로 늘 때만(예전 보장이 더 크면 늘지 않는다 — 계약 "legacy_floor").
 */
@Composable
internal fun RunnerLevelCard(level: RunnerLevel, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val c = StepUpUi.colors
    val shape = RoundedCornerShape(StepUpUi.GroupedRadius)
    val info = stringResource(R.string.rl_card_info)
    Column(
        modifier.fillMaxWidth().padding(horizontal = StepUpUi.Gutter)
            .clip(shape).background(c.card).stepUpRim(shape, RimKind.Surface)
            .feedbackClickable(role = Role.Button, onClick = onOpen)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("profile-runner-level"),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.rl_card_title, level.level),
                style = StepUpUi.type.rowTitle.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = c.label,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false).testTag("profile-runner-title"),
            )
            if (level.badge > 0) {
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.rl_card_badge, level.badge), style = StepUpUi.type.rowSubtitle, color = c.secondaryLabel,
                    maxLines = 1, modifier = Modifier.testTag("profile-runner-badge"),
                )
            }
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Outlined.Info, contentDescription = info, tint = c.tertiaryLabel,
                modifier = Modifier.size(18.dp).testTag("profile-runner-info"),
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(c.fill).testTag("profile-runner-bar")) {
            Box(Modifier.fillMaxWidth(level.progress).height(5.dp).clip(RoundedCornerShape(3.dp)).background(c.primary))
        }
        Spacer(Modifier.height(8.dp))
        val toNext = level.stepsToNext
        Text(
            if (toNext != null) stringResource(R.string.rl_card_to_next, steps(toNext)) else stringResource(R.string.rl_card_max),
            style = StepUpUi.type.rowSubtitle, color = c.secondaryLabel, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("profile-runner-next"),
        )
        level.energyMinutes?.let { minutes ->
            val now = stringResource(R.string.rl_card_energy, minutes)
            val next = level.nextEnergyIfMore?.let { stringResource(R.string.rl_card_energy_next, it) }
            Text(
                listOfNotNull(now, next).joinToString(" · "),
                style = StepUpUi.type.rowSubtitle, color = c.label, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp).testTag("profile-runner-energy"),
            )
        }
    }
}

// ── 설명 시트 ───────────────────────────────────────────────────────

/** 무엇이 걸음으로 세지나 · 레벨이 주는 것(서버 `rewards`) · 예전 보장. 주 행동은 "확인" 하나 */
@Composable
internal fun RunnerLevelInfoSheet(level: RunnerLevel, onClose: () -> Unit) {
    val t = runTone()
    RunSheet(onDismiss = onClose, modifier = Modifier.testTag("runner-level-info")) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            RunSheetText(
                stringResource(R.string.rl_info_title),
                body = stringResource(R.string.rl_card_title, level.level) + " · " + stringResource(R.string.rl_info_steps, steps(level.steps)),
            )
            Spacer(Modifier.height(16.dp))
            InfoParagraph(stringResource(R.string.rl_info_how, steps(level.levelSteps), level.maxLevel))
            InfoParagraph(stringResource(R.string.rl_info_counts))
            InfoParagraph(stringResource(R.string.rl_info_not_counts))
            val r = level.rewards
            val gives = buildList {
                r.energyMinutesPerLevel?.let { m ->
                    add(level.energyCapLevel?.let { cap -> stringResource(R.string.rl_info_energy_cap, m, cap) } ?: stringResource(R.string.rl_info_energy, m))
                }
                r.freeEveryLevel?.let { add(stringResource(R.string.rl_info_free, it)) }
                r.giftEvery?.let { add(stringResource(R.string.rl_info_gift, it)) }
                if (r.badgeLevels.isNotEmpty()) add(stringResource(R.string.rl_info_badges, r.badgeLevels.joinToString(" · ")))
            }
            if (gives.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.rl_info_gives), style = runTextStyle(15.sp, t.text, FontWeight.Bold), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                Column(Modifier.fillMaxWidth().testTag("runner-level-gives")) {
                    gives.forEach { line ->
                        Text("· $line", style = runTextStyle(14.sp, t.text, FontWeight.Medium), modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            if (level.legacyFloor) {
                level.energyMinutes?.let { InfoParagraph(stringResource(R.string.rl_info_legacy, it), tag = "runner-level-legacy") }
            }
            InfoParagraph(stringResource(R.string.rl_info_shoes))
            Spacer(Modifier.height(12.dp))
            RunButton(stringResource(R.string.common_ok), onClose, Modifier.testTag("runner-level-info-ok"))
        }
    }
}

@Composable
private fun InfoParagraph(text: String, tag: String? = null) {
    val t = runTone()
    Text(
        text, style = runTextStyle(14.sp, t.label, FontWeight.Normal, 1.5f), textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).then(if (tag != null) Modifier.testTag(tag) else Modifier),
    )
}

// ── 레벨이 올랐어요 ─────────────────────────────────────────────────

/**
 * 러너 레벨이 올랐다 · 처음 본 러너 레벨 — 앱 셸이 탭 첫 화면에서 한 번 보인다([allowed]: 탭 첫 화면 · 첫 사용 안내 · 러닝 되살리기가
 * 없을 때). 달리는 동안에는 가리지 않는다. 닫으면 본 것으로 적는다(닫기 전에 앱이 꺼지면 다음에 다시).
 */
@Composable
fun RunnerLevelNoticeHost(allowed: Boolean) {
    val repo = remember { runCatching { ServiceLocator.runnerLevel }.getOrNull() } ?: return
    val notice by repo.notice.collectAsState()
    val runningFlow = remember {
        com.stepup.android.service.WalkSessionService.state.map { it.isActive }.distinctUntilChanged()
    }
    val running by runningFlow.collectAsState(initial = true)
    val scope = rememberCoroutineScope()
    val shown = notice ?: return
    if (!allowed || running) return
    RunnerLevelNoticeSheet(shown, onClose = { scope.launch { repo.consumeNotice() } })
}

@Composable
internal fun RunnerLevelNoticeSheet(notice: RunnerLevelNotice, onClose: () -> Unit) {
    val t = runTone()
    RunSheet(onDismiss = onClose, modifier = Modifier.testTag("runner-level-notice")) {
        when (notice) {
            is RunnerLevelNotice.LevelUp -> {
                RunSheetText(
                    stringResource(R.string.rl_up_title),
                    body = stringResource(R.string.rl_up_levels, notice.from, notice.to),
                )
                val lines = buildList {
                    if (notice.energyFrom != null && notice.energyTo != null) {
                        add(stringResource(R.string.rl_up_energy, notice.energyFrom, notice.energyTo))
                    }
                    boxesLine(notice.freeBoxes, notice.giftBoxes)?.let { add(it) }
                    notice.badge?.let { add(stringResource(R.string.rl_up_badge, it)) }
                }
                if (lines.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Column(Modifier.fillMaxWidth().testTag("runner-level-notice-lines"), horizontalAlignment = Alignment.CenterHorizontally) {
                        lines.forEach { line ->
                            Text(line, style = runTextStyle(15.sp, t.text, FontWeight.SemiBold), textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
                if (notice.freeBoxes > 0 || notice.giftBoxes > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.boxes_sub), style = runTextStyle(13.5.sp, t.label), textAlign = TextAlign.Center)
                }
            }
            is RunnerLevelNotice.Welcome -> {
                val body = when {
                    notice.freeBoxes > 0 && notice.giftBoxes > 0 ->
                        stringResource(R.string.rl_welcome_both, notice.level, notice.freeBoxes, notice.giftBoxes)
                    notice.freeBoxes > 0 -> stringResource(R.string.rl_welcome_free, notice.level, notice.freeBoxes)
                    notice.giftBoxes > 0 -> stringResource(R.string.rl_welcome_gift, notice.level, notice.giftBoxes)
                    else -> stringResource(R.string.rl_welcome_none, notice.level)
                }
                RunSheetText(stringResource(R.string.rl_welcome_title), body = body, note = stringResource(R.string.rl_welcome_note))
            }
        }
        Spacer(Modifier.height(20.dp))
        RunButton(stringResource(R.string.common_ok), onClose, Modifier.testTag("runner-level-notice-ok"))
    }
}

/** 받은 상자 한 줄 — 받은 것이 없으면 null */
@Composable
private fun boxesLine(free: Int, gift: Int): String? = when {
    free > 0 && gift > 0 -> stringResource(R.string.rl_up_boxes_both, free, gift)
    free > 0 -> stringResource(R.string.rl_up_boxes_free, free)
    gift > 0 -> stringResource(R.string.rl_up_boxes_gift, gift)
    else -> null
}

// ── 이름 옆 표시(크루 명단 · 대기실) ────────────────────────────────

/**
 * [ids] 의 배지(Lv10 · 20 · 30) — 목록마다 한 번에 묻고(받아 둔 값은 바로) 목록을 기다리게 하지 않는다. 내 러너 레벨이 꺼졌거나 서버에
 * 없으면 빈 값(표시를 두지 않는다).
 */
@Composable
fun rememberRunnerBadges(ids: List<String>): Map<String, Int> {
    val repo = remember { runCatching { ServiceLocator.runnerLevel }.getOrNull() } ?: return emptyMap()
    val on = rememberRunnerLevel() != null
    val key = remember(ids) { ids.distinct().sorted() }
    var badges by remember { mutableStateOf(repo.cachedBadges(key)) }
    LaunchedEffect(key, on) {
        if (!on) return@LaunchedEffect
        badges = repo.cachedBadges(key)
        badges = repo.badges(key)
    }
    return if (on) badges else emptyMap()
}

/** 이름 옆 작은 글자 "러너 Lv10" — 배지가 없으면 null(색 면 · 알약 없이 글자만) */
@Composable
fun runnerNameMark(badge: Int?): String? = badge?.takeIf { it > 0 }?.let { stringResource(R.string.rl_name_mark, it) }
