package com.stepup.android.ui.screens.coaching

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.domain.coaching.CoachGoal
import com.stepup.android.domain.coaching.CoachPrefs
import com.stepup.android.domain.coaching.CoachResult
import com.stepup.android.domain.coaching.CoachingTemplates
import com.stepup.android.domain.coaching.WorkoutStep
import com.stepup.android.domain.coaching.WorkoutTemplate
import com.stepup.android.domain.coaching.paceText
import com.stepup.android.domain.coaching.weekPlan
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunButtonKind
import com.stepup.android.ui.components.RunChoiceRow
import com.stepup.android.ui.components.RunDivider
import com.stepup.android.ui.components.RunNotice
import com.stepup.android.ui.components.RunNoticeKind
import com.stepup.android.ui.components.RunNumber
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.RunSheetText
import com.stepup.android.ui.components.RunSpec
import com.stepup.android.ui.components.RunSwitch
import com.stepup.android.ui.components.RunTextAction
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.records.rememberFormatter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * 코칭의 설정 쪽 화면 — 동의(05) · 목표(06) · 다음 주 계획(13) · 설정(14)과 해제 확인(15) · 훈련 상세(02).
 * 여기서는 AI 를 부르지 않는다. 저장 · 적용은 서버가 받았다고 답한 뒤에만 성공이라고 말하고, 어느 것도 러닝을 시작하지 않는다
 * (02 의 "이 훈련으로 시작"만 사용자가 누른 기존 러닝 시작이다).
 */

/** 화면 아래 버튼 칸 — 키보드 · 시스템 아래 영역 위 */
@Composable
private fun BottomBar(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = RunSpec.Gutter).padding(top = 8.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/** 서버 상태를 못 읽은 이유 — 로그인 전 · 연결 없음 · 서버 없는 빌드(빈 화면으로 두지 않는다) */
@Composable
private fun AccountNotice(account: CoachAccount, onRetry: () -> Unit) {
    when (account) {
        CoachAccount.Loading -> CoachLoading()
        CoachAccount.SignIn -> RunNotice(stringResource(R.string.coach_signin), Modifier.padding(vertical = 8.dp), tag = "coach-account-signin")
        CoachAccount.Offline, CoachAccount.Failed -> RunNotice(
            stringResource(R.string.coach_load_failed), Modifier.padding(vertical = 8.dp), kind = RunNoticeKind.Warn,
            action = stringResource(R.string.coach_retry), onAction = onRetry, tag = "coach-account-failed",
        )
        CoachAccount.NotConfigured -> RunNotice(
            stringResource(R.string.coach_consent_unavailable), Modifier.padding(vertical = 8.dp), tag = "coach-account-unconfigured",
        )
        is CoachAccount.Ready -> Unit
    }
}

/** 저장 · 적용이 안 됐다 — 기존 값은 그대로라고 */
@Composable
private fun SaveNotice(state: SaveState) {
    when (state) {
        SaveState.Conflict -> RunNotice(stringResource(R.string.coach_conflict), kind = RunNoticeKind.Warn, tag = "coach-save-conflict")
        SaveState.Failed, SaveState.Offline ->
            RunNotice(stringResource(R.string.coach_save_failed), kind = RunNoticeKind.Error, tag = "coach-save-failed")
        SaveState.Idle, SaveState.Saving, SaveState.Saved -> Unit
    }
}

// ── 05 첫 사용 동의 ────────────────────────────────────────────────

/**
 * 첫 사용 동의(05) — 보낼 것 · 보내지 않을 것 · 분석 제공자. 코칭이 꺼져 있거나 공급자가 정해지지 않았으면 동의를 받지 않는다
 * (그 판의 문구가 없다). 동의하지 않아도 러닝과 기본 기록은 그대로다. 문구는 확정 전 초안이다.
 */
@Composable
fun CoachingConsentScreen(viewModel: CoachingSetupViewModel, onBack: () -> Unit, onGranted: (hasGoal: Boolean) -> Unit) {
    val t = runTone()
    val account by viewModel.account.collectAsStateWithLifecycle()
    val save by viewModel.save.collectAsStateWithLifecycle()
    var checked by rememberSaveable { mutableStateOf(false) }
    var dataSheet by rememberSaveable { mutableStateOf(false) }
    val snap = account.snapshot
    val available = snap != null && snap.enabled && providerKnown(snap.provider)
    val saving = save == SaveState.Saving
    CoachFrame(onBack = onBack) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = RunSpec.Gutter)) {
            CoachTitle(stringResource(R.string.coach_consent_title))
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.coach_consent_head), style = runTextStyle(27.sp, t.text, FontWeight.ExtraBold, 1.3f))
            Spacer(Modifier.height(22.dp))
            ConsentFlow()
            Spacer(Modifier.height(18.dp))
            ConsentRow(Icons.Outlined.Description, stringResource(R.string.coach_consent_send), stringResource(R.string.coach_consent_send_sub))
            RunDivider()
            ConsentRow(Icons.Outlined.VisibilityOff, stringResource(R.string.coach_consent_not_send), stringResource(R.string.coach_consent_not_send_sub))
            RunDivider()
            ConsentRow(Icons.Filled.AutoAwesome, stringResource(R.string.coach_consent_provider), providerName(snap?.provider))
            RunDivider()
            Spacer(Modifier.height(6.dp))
            RunTextAction(stringResource(R.string.coach_consent_data_link), { dataSheet = true }, Modifier.testTag("coach-consent-data"), chevron = true)
            Text(stringResource(R.string.coach_consent_optional), style = runTextStyle(14.sp, t.label, FontWeight.Medium, 1.45f))
            AccountNotice(account, viewModel::refreshAccount)
            if (snap != null && !available) {
                RunNotice(stringResource(R.string.coach_consent_unavailable), Modifier.padding(vertical = 8.dp), tag = "coach-consent-unavailable")
            }
            Spacer(Modifier.height(8.dp))
            CoachCheckRow(
                checked, { checked = !checked }, stringResource(R.string.coach_consent_check), "coach-consent-check",
                enabled = available && !saving,
            )
            Spacer(Modifier.height(8.dp))
        }
        BottomBar {
            SaveNotice(save)
            RunButton(
                stringResource(R.string.coach_consent_start), { viewModel.grantConsent(onGranted) }, Modifier.testTag("coach-consent-start"),
                enabled = available && checked && !saving, busy = saving,
            )
            RunTextAction(stringResource(R.string.coach_consent_later), onBack, Modifier.align(Alignment.CenterHorizontally).testTag("coach-consent-later"))
        }
    }
    if (dataSheet) CoachDataSheet { dataSheet = false }
}

/** 동의 문구에 쓰는 공급자 이름 — 모의 공급자는 "모의 응답"(실제 AI 처럼 보이지 않게), 정해지지 않았으면 "—" */
@Composable
private fun providerName(provider: String?): String = when (provider) {
    "anthropic" -> stringResource(R.string.coach_consent_provider_name)
    "mock" -> stringResource(R.string.coach_ai_mock)
    else -> "—"
}

/** 러닝 기록 → 숫자 요약 → AI 코칭 */
@Composable
private fun ConsentFlow() {
    val t = runTone()
    Row(Modifier.fillMaxWidth().clearAndSetSemantics { }, verticalAlignment = Alignment.Top) {
        FlowStep(Icons.AutoMirrored.Outlined.DirectionsRun, stringResource(R.string.coach_flow_record), Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = t.cobaltText, modifier = Modifier.padding(top = 26.dp).size(22.dp))
        FlowStep(Icons.Filled.BarChart, stringResource(R.string.coach_flow_summary), Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = t.cobaltText, modifier = Modifier.padding(top = 26.dp).size(22.dp))
        FlowStep(Icons.Filled.AutoAwesome, stringResource(R.string.coach_flow_ai), Modifier.weight(1f))
    }
}

@Composable
private fun FlowStep(icon: ImageVector, label: String, modifier: Modifier) {
    val t = runTone()
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(t.panel).border(1.5.dp, t.secondaryEdge, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = t.text, modifier = Modifier.size(32.dp)) }
        Spacer(Modifier.height(8.dp))
        Text(label, style = runTextStyle(15.sp, t.text, FontWeight.SemiBold, 1.3f), textAlign = TextAlign.Center)
    }
}

@Composable
private fun ConsentRow(icon: ImageVector, title: String, sub: String) {
    val t = runTone()
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(52.dp).clip(CircleShape).border(1.5.dp, t.secondaryEdge, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = t.cobaltText, modifier = Modifier.size(24.dp)) }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = runTextStyle(17.sp, t.text, FontWeight.Bold, 1.3f))
            Text(sub, style = runTextStyle(14.sp, t.label, FontWeight.Medium, 1.4f))
        }
    }
}

// ── 06 코칭 목표 ──────────────────────────────────────────────────

/**
 * 코칭 목표(06) — 목표 · 주간 횟수 · 한 번의 시간. 세 칸을 모두 골라야 저장할 수 있고(미리 골라 두지 않는다), 저장만 한다 —
 * 러닝을 시작하지 않는다. 실패하면 서버의 기존 목표가 그대로이고, 성공은 서버가 받았다고 답한 뒤에만.
 */
@Composable
fun CoachingGoalsScreen(viewModel: CoachingSetupViewModel, onBack: () -> Unit, onSaved: () -> Unit) {
    val t = runTone()
    val account by viewModel.account.collectAsStateWithLifecycle()
    val save by viewModel.save.collectAsStateWithLifecycle()
    val prefs = account.snapshot?.prefs
    var goal by rememberSaveable { mutableStateOf<String?>(null) }
    var runs by rememberSaveable { mutableStateOf<Int?>(null) }
    var minutes by rememberSaveable { mutableStateOf<Int?>(null) }
    // 이미 정한 목표가 있으면 그것으로 채운다(사용자가 고른 칸은 덮지 않는다)
    LaunchedEffect(prefs?.revision) {
        if (prefs != null) {
            if (goal == null) goal = prefs.goal
            if (runs == null) runs = prefs.runsPerWeek
            if (minutes == null) minutes = prefs.minutes
        }
    }
    val chosenGoal = CoachGoal.of(goal)
    val chosenRuns = runs?.takeIf { it in CoachPrefs.RUNS_PER_WEEK }
    val chosenMinutes = minutes?.takeIf { it in CoachPrefs.MINUTES }
    val saving = save == SaveState.Saving
    val first = account.snapshot?.chosenPrefs() == null
    CoachFrame(onBack = onBack) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = RunSpec.Gutter)) {
            CoachTitle(stringResource(R.string.coach_goals_title))
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.coach_goals_head), style = runTextStyle(26.sp, t.text, FontWeight.ExtraBold, 1.3f))
            Spacer(Modifier.height(14.dp))
            CoachGoal.entries.forEach { option ->
                RunChoiceRow(
                    title = stringResource(goalName(option)),
                    selected = option == chosenGoal,
                    onSelect = { goal = option.key },
                    compact = true,
                    modifier = Modifier.padding(vertical = 4.dp).testTag("coach-goal-${option.key}"),
                )
            }
            Spacer(Modifier.height(22.dp))
            CoachSection(stringResource(R.string.coach_goals_runs))
            Spacer(Modifier.height(10.dp))
            CoachOptions(
                CoachPrefs.RUNS_PER_WEEK.map { stringResource(R.string.coach_times, it) },
                chosenRuns?.let { CoachPrefs.RUNS_PER_WEEK.indexOf(it) }?.takeIf { it >= 0 },
                { runs = CoachPrefs.RUNS_PER_WEEK[it] },
                "coach-runs",
            )
            Spacer(Modifier.height(22.dp))
            CoachSection(stringResource(R.string.coach_goals_minutes))
            Spacer(Modifier.height(10.dp))
            CoachOptions(
                CoachPrefs.MINUTES.map { stringResource(R.string.coach_minutes, it) },
                chosenMinutes?.let { CoachPrefs.MINUTES.indexOf(it) }?.takeIf { it >= 0 },
                { minutes = CoachPrefs.MINUTES[it] },
                "coach-minutes",
            )
            Spacer(Modifier.height(18.dp))
            CoachQuote(null) { CoachQuoteBody(stringResource(R.string.coach_goals_note)) }
            AccountNotice(account, viewModel::refreshAccount)
            Spacer(Modifier.height(8.dp))
        }
        BottomBar {
            SaveNotice(save)
            RunButton(
                stringResource(if (first) R.string.coach_goals_start else R.string.coach_goals_save),
                {
                    if (chosenGoal != null && chosenRuns != null && chosenMinutes != null) {
                        viewModel.savePrefs(chosenGoal, chosenRuns, chosenMinutes, onSaved)
                    }
                },
                Modifier.testTag("coach-goals-save"),
                enabled = chosenGoal != null && chosenRuns != null && chosenMinutes != null && account is CoachAccount.Ready && !saving,
                busy = saving,
            )
        }
    }
}

// ── 14 AI 코칭 설정 · 15 해제 확인 ──────────────────────────────────

/**
 * AI 코칭 설정(14) — 사용(동의) · 자동 분석 · 주간 리포트 · 목표 · 데이터 · 해제. 자동 분석과 주간 리포트는 동의한 뒤에만,
 * 알림 권한과는 따로다. 끄기(해제)는 확인 시트(15)에서만.
 */
@Composable
fun CoachingSettingsScreen(viewModel: CoachingSetupViewModel, onBack: () -> Unit, onConsent: () -> Unit, onGoals: () -> Unit) {
    val t = runTone()
    val account by viewModel.account.collectAsStateWithLifecycle()
    val save by viewModel.save.collectAsStateWithLifecycle()
    val done by viewModel.withdrawDone.collectAsStateWithLifecycle()
    val deleted by viewModel.withdrawn.collectAsStateWithLifecycle()
    var off by rememberSaveable { mutableStateOf(false) }
    var dataSheet by rememberSaveable { mutableStateOf(false) }
    // 동의 · 목표에서 돌아오면 다시 읽는다
    var resumedOnce by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        if (resumedOnce) viewModel.refreshAccount() else resumedOnce = true
        onPauseOrDispose { }
    }
    val snap = account.snapshot
    val consented = snap?.consented == true
    val prefs = snap?.prefs
    val chosen = snap?.chosenPrefs()
    val saving = save == SaveState.Saving
    CoachFrame(onBack = onBack) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = RunSpec.Gutter)) {
            CoachTitle(stringResource(R.string.coach_settings_title))
            Spacer(Modifier.height(8.dp))
            AccountNotice(account, viewModel::refreshAccount)
            if (done) {
                val count = deleted
                RunNotice(
                    if (count != null) stringResource(R.string.coach_off_done_deleted, count) else stringResource(R.string.coach_off_done),
                    Modifier.padding(vertical = 8.dp), tag = "coach-off-done",
                )
            }
            SaveNotice(save)
            SettingsHeader(stringResource(R.string.coach_settings_analysis))
            ToggleRow(
                stringResource(R.string.coach_settings_use), null, consented, enabled = snap != null && !saving,
                onChange = { on -> if (on) onConsent() else off = true }, tag = "coach-use",
            )
            ToggleRow(
                stringResource(R.string.coach_settings_auto), stringResource(R.string.coach_settings_auto_sub),
                prefs?.autoAnalysis == true, enabled = consented && !saving,
                onChange = { on -> viewModel.setToggles(on, prefs?.weeklyReport == true) }, tag = "coach-auto",
            )
            ToggleRow(
                stringResource(R.string.coach_settings_weekly), stringResource(R.string.coach_settings_weekly_sub),
                prefs?.weeklyReport == true, enabled = consented && !saving,
                onChange = { on -> viewModel.setToggles(prefs?.autoAnalysis == true, on) }, tag = "coach-weekly",
            )
            SettingsHeader(stringResource(R.string.coach_settings_goal))
            SettingsLink(
                if (chosen != null) stringResource(goalName(chosen.goal)) else stringResource(R.string.coach_goal_unset),
                onGoals, "coach-settings-goal",
                sub = chosen?.let { stringResource(R.string.coach_goal_summary, it.runsPerWeek, it.minutes) },
            )
            val target = prefs?.targetPaceSec?.let { paceText(it.toDouble()) }
            if (target != null) {
                Text(
                    stringResource(R.string.coach_target_pace, target), style = runTextStyle(15.sp, t.label, FontWeight.Medium),
                    modifier = Modifier.padding(vertical = 10.dp).testTag("coach-target-pace"),
                )
                RunDivider()
            }
            SettingsHeader(stringResource(R.string.coach_settings_data))
            SettingsLink(stringResource(R.string.coach_settings_data_guide), { dataSheet = true }, "coach-settings-data")
            if (consented) SettingsLink(stringResource(R.string.coach_settings_off), { off = true }, "coach-settings-off", danger = true)
            Spacer(Modifier.height(16.dp))
            CoachQuote(null) { CoachQuoteBody(stringResource(R.string.coach_settings_note)) }
            Spacer(Modifier.height(20.dp))
        }
    }
    if (off) {
        WithdrawSheet(
            saving = saving,
            onKeep = { off = false },
            onConfirm = { deleteResults -> viewModel.withdraw(deleteResults) { off = false } },
            onData = { dataSheet = true },
        )
    }
    if (dataSheet) CoachDataSheet { dataSheet = false }
}

@Composable
private fun SettingsHeader(text: String) {
    Spacer(Modifier.height(22.dp))
    CoachSection(text, Modifier.padding(bottom = 6.dp))
    RunDivider()
}

@Composable
private fun ToggleRow(title: String, sub: String?, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit, tag: String) {
    val t = runTone()
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = runTextStyle(18.sp, if (enabled) t.text else t.muted, FontWeight.Bold, 1.3f))
            if (sub != null) Text(sub, style = runTextStyle(14.sp, t.label, FontWeight.Medium, 1.35f))
        }
        RunSwitch(checked, if (enabled) onChange else null, Modifier.testTag(tag), enabled = enabled)
    }
    RunDivider()
}

@Composable
private fun SettingsLink(title: String, onClick: () -> Unit, tag: String, sub: String? = null, danger: Boolean = false) {
    val t = runTone()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).feedbackClickable(onClick = onClick).testTag(tag).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = runTextStyle(18.sp, if (danger) t.dangerText else t.text, FontWeight.Bold, 1.3f))
            if (sub != null) Text(sub, style = runTextStyle(14.sp, t.label, FontWeight.Medium))
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = t.label, modifier = Modifier.size(26.dp))
    }
    RunDivider()
}

/**
 * AI 코칭 끄기(15) — 하단 탭까지 덮는 시트. 저장된 분석 삭제는 따로 고르는 칸(기본은 고르지 않음). 대기 작업은 취소되지만
 * 이미 보낸 요청을 되돌린다고 말하지 않는다. 끄는 동안에는 닫을 수 없다.
 */
@Composable
private fun WithdrawSheet(saving: Boolean, onKeep: () -> Unit, onConfirm: (Boolean) -> Unit, onData: () -> Unit) {
    val t = runTone()
    var delete by rememberSaveable { mutableStateOf(false) }
    RunSheet(onDismiss = onKeep, modifier = Modifier.testTag("coach-off-sheet"), showClose = false, dismissible = !saving) {
        Icon(Icons.Outlined.Shield, contentDescription = null, tint = t.cobaltText, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(12.dp))
        RunSheetText(stringResource(R.string.coach_off_title), body = stringResource(R.string.coach_off_body))
        Spacer(Modifier.height(16.dp))
        CoachCheckRow(
            delete, { delete = !delete }, stringResource(R.string.coach_off_delete), "coach-off-delete",
            sub = stringResource(R.string.coach_off_delete_sub), enabled = !saving,
        )
        RunTextAction(stringResource(R.string.coach_off_data_link), onData, Modifier.testTag("coach-off-data"), chevron = true)
        Spacer(Modifier.height(16.dp))
        RunButton(stringResource(R.string.coach_off_keep), onKeep, Modifier.testTag("coach-off-keep"), kind = RunButtonKind.Secondary, enabled = !saving)
        Spacer(Modifier.height(10.dp))
        RunButton(
            stringResource(R.string.coach_off_confirm), { onConfirm(delete) }, Modifier.testTag("coach-off-confirm"),
            kind = RunButtonKind.Danger, enabled = !saving, busy = saving,
        )
    }
}

// ── 13 다음 주 계획 ───────────────────────────────────────────────

/**
 * 다음 주 계획 검토(13) — 날짜와 훈련은 고른 목표로 코드가 만든 허용 계획(서버도 같은 식으로 만든다). 적용 전에는 기존 계획이
 * 바뀌지 않고, 적용해도 러닝을 시작하지 않는다. AI 이유는 끝난 지난주 정리가 이 계획을 골랐을 때만(저장된 사본).
 */
@Composable
fun CoachingPlanScreen(viewModel: CoachingSetupViewModel, onBack: () -> Unit, onGoals: () -> Unit) {
    val t = runTone()
    val account by viewModel.account.collectAsStateWithLifecycle()
    val save by viewModel.save.collectAsStateWithLifecycle()
    val reason by viewModel.reason.collectAsStateWithLifecycle()
    val locale = stringResource(R.string.coach_locale)
    LaunchedEffect(locale) { viewModel.loadPlanReason(locale) }
    val snap = account.snapshot
    val prefs = snap?.chosenPrefs()
    val monday = remember { viewModel.nextMonday() }
    val applied = snap?.plans?.firstOrNull { it.weekStart == monday }
    val saving = save == SaveState.Saving
    CoachFrame(onBack = onBack) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = RunSpec.Gutter)) {
            CoachTitle(stringResource(R.string.coach_plan_title))
            Spacer(Modifier.height(14.dp))
            when {
                snap == null -> AccountNotice(account, viewModel::refreshAccount)
                prefs == null -> {
                    CoachQuoteTitle(stringResource(R.string.coach_plan_need_goal))
                    Spacer(Modifier.height(14.dp))
                    RunButton(stringResource(R.string.coach_set_goal), onGoals, Modifier.testTag("coach-plan-goal"), kind = RunButtonKind.Secondary)
                }
                else -> {
                    val days: List<Pair<LocalDate, WorkoutTemplate>> = applied?.days
                        ?.mapNotNull { (date, id) -> CoachingTemplates.byId(id)?.let { date to it } }
                        ?: weekPlan(monday, prefs).map { it.date to it.template }
                    PlanContent(monday, days, applied != null, reason)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (prefs != null) {
            BottomBar {
                if (save == SaveState.Saved) {
                    RunNotice(stringResource(R.string.coach_plan_applied), tag = "coach-plan-applied")
                } else {
                    SaveNotice(save)
                }
                if (applied == null) {
                    Text(
                        stringResource(R.string.coach_plan_note), style = runTextStyle(14.sp, t.label, FontWeight.Medium),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    RunButton(
                        stringResource(R.string.coach_plan_apply), viewModel::applyPlan, Modifier.testTag("coach-plan-apply"),
                        enabled = account is CoachAccount.Ready && !saving, busy = saving,
                    )
                }
                RunTextAction(stringResource(R.string.coach_plan_later), onBack, Modifier.align(Alignment.CenterHorizontally).testTag("coach-plan-later"))
            }
        }
    }
}

@Composable
private fun PlanContent(monday: LocalDate, days: List<Pair<LocalDate, WorkoutTemplate>>, applied: Boolean, reason: CoachResult?) {
    val t = runTone()
    val spoken = rememberFormatter(R.string.coach_date_wd)
    CoachPill(
        stringResource(if (applied) R.string.coach_plan_applied_pill else R.string.coach_plan_pill),
        Modifier.testTag("coach-plan-pill"), accent = applied,
    )
    Spacer(Modifier.height(14.dp))
    Text(
        stringResource(R.string.coach_plan_head, days.size), style = runTextStyle(30.sp, t.text, FontWeight.ExtraBold, 1.25f),
        modifier = Modifier.testTag("coach-plan-head"),
    )
    Spacer(Modifier.height(6.dp))
    Text(weekRangeText(monday), style = runTextStyle(16.sp, t.label, FontWeight.Medium))
    Spacer(Modifier.height(18.dp))
    PlanStrip(monday, days.map { it.first }.toSet())
    Spacer(Modifier.height(14.dp))
    RunDivider()
    days.forEach { (date, template) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 15.dp).testTag("coach-plan-day-$date"), verticalAlignment = Alignment.CenterVertically) {
            Text(spoken.format(date), style = runTextStyle(17.sp, t.text, FontWeight.Bold), modifier = Modifier.weight(0.9f))
            Text(stringResource(familyName(template.family)), style = runTextStyle(17.sp, t.text, FontWeight.Bold), modifier = Modifier.weight(1.4f))
            Text(stringResource(R.string.coach_minutes, template.minutes), style = runTextStyle(17.sp, t.label, FontWeight.SemiBold))
        }
        RunDivider()
    }
    val because = reason?.actionReason
    if (reason != null && because != null) {
        Spacer(Modifier.height(18.dp))
        CoachQuote(
            stringResource(R.string.coach_ai_label),
            extra = if (reason.mock) {
                { CoachPill(stringResource(R.string.coach_ai_mock)) }
            } else {
                null
            },
        ) { CoachQuoteTitle(because) }
    }
}

/** 계획 띠 — 계획한 날은 파란 원(실제 러닝 체크가 아니다) */
@Composable
private fun PlanStrip(monday: LocalDate, planned: Set<LocalDate>) {
    val t = runTone()
    val locale = LocalConfiguration.current.locales[0]
    val weekday = remember(locale) { DateTimeFormatter.ofPattern("E", locale) }
    val spoken = rememberFormatter(R.string.coach_date_wd)
    Row(Modifier.fillMaxWidth()) {
        (0L until 7L).forEach { offset ->
            val date = monday.plusDays(offset)
            val on = date in planned
            val cd = if (on) stringResource(R.string.coach_plan_day_cd, spoken.format(date)) else spoken.format(date)
            Column(
                Modifier.weight(1f).clearAndSetSemantics { contentDescription = cd },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(weekday.format(date), style = runTextStyle(14.sp, t.label, FontWeight.Medium))
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.size(40.dp).clip(CircleShape)
                        .background(if (on) t.cobalt else Color.Transparent)
                        .border(1.dp, if (on) t.cobalt else t.panelEdge, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${date.dayOfMonth}", style = runTextStyle(16.sp, if (on) Color.White else t.label, FontWeight.Bold, 1.1f))
                }
            }
        }
    }
}

// ── 02 훈련 상세 ──────────────────────────────────────────────────

/**
 * 훈련 상세(02) — 시간 · 순서 · 강도는 검토된 템플릿(코드). AI 이유는 이번 주 추천이 이 훈련을 골랐을 때만(저장된 사본).
 * "이 훈련으로 시작"은 기존 자유 러닝 시작이다 — 단계 안내는 러닝 화면에 나오지 않는다고 미리 말한다.
 */
@Composable
fun CoachingWorkoutScreen(templateId: String, viewModel: CoachingSetupViewModel, onBack: () -> Unit, onStart: () -> Unit) {
    val t = runTone()
    val template = remember(templateId) { CoachingTemplates.byId(templateId) }
    val reason by viewModel.reason.collectAsStateWithLifecycle()
    val session by com.stepup.android.service.WalkSessionService.state.collectAsStateWithLifecycle()
    val locale = stringResource(R.string.coach_locale)
    LaunchedEffect(templateId, locale) { viewModel.loadWorkoutReason(templateId, locale) }
    CoachFrame(onBack = onBack) {
        if (template == null) {
            Text(
                stringResource(R.string.coach_workout_missing), style = runTextStyle(17.sp, t.label, FontWeight.Medium),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = RunSpec.Gutter, vertical = 40.dp).testTag("coach-workout-missing"),
            )
        } else {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = RunSpec.Gutter)) {
                CoachTitle(stringResource(R.string.coach_workout_title))
                Spacer(Modifier.height(16.dp))
                Text(stringResource(familyName(template.family)), style = runTextStyle(17.sp, t.cobaltText, FontWeight.SemiBold))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RunNumber(
                        "${template.minutes}", unit = stringResource(R.string.coach_unit_minutes), size = 80.sp, unitSize = 28.sp,
                        italicUnit = false, valueTag = "coach-workout-minutes",
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        stringResource(familySub(template.family)), style = runTextStyle(16.sp, t.label, FontWeight.Medium, 1.45f),
                        textAlign = TextAlign.End,
                    )
                }
                Spacer(Modifier.height(16.dp))
                WorkoutBar(template, height = 46.dp, labels = true)
                Spacer(Modifier.height(6.dp))
                BarAxis(template)
                Spacer(Modifier.height(18.dp))
                template.steps.forEachIndexed { index, step ->
                    StepRow(index, step, last = index == template.steps.lastIndex)
                }
                val because = reason?.actionReason
                val picked = reason
                if (picked != null && because != null) {
                    Spacer(Modifier.height(18.dp))
                    CoachQuote(
                        stringResource(R.string.coach_ai_coach_label),
                        extra = if (picked.mock) {
                            { CoachPill(stringResource(R.string.coach_ai_mock)) }
                        } else {
                            null
                        },
                    ) { CoachQuoteTitle(because) }
                }
                Spacer(Modifier.height(16.dp))
            }
            BottomBar {
                if (reason == null) {
                    Text(stringResource(R.string.coach_workout_basic_note), style = runTextStyle(14.sp, t.label, FontWeight.Medium))
                }
                Text(stringResource(R.string.coach_workout_free_note), style = runTextStyle(14.sp, t.label, FontWeight.Medium))
                RunButton(
                    stringResource(if (session.isActive) R.string.coach_workout_continue else R.string.coach_workout_start),
                    onStart, Modifier.testTag("coach-workout-start"), chevron = true,
                )
            }
        }
    }
}

/** 막대 아래 눈금 — 단계가 바뀌는 분(0 · 5 · 20 · 25분) */
@Composable
private fun BarAxis(template: WorkoutTemplate) {
    val t = runTone()
    val bounds = template.boundaries
    Row(Modifier.fillMaxWidth().clearAndSetSemantics { }, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        template.steps.forEachIndexed { index, step ->
            Box(Modifier.weight(step.minutes.toFloat())) {
                Text("${bounds[index]}", style = runTextStyle(13.sp, t.label, FontWeight.Medium), modifier = Modifier.align(Alignment.TopStart))
                if (index == template.steps.lastIndex) {
                    Text(
                        stringResource(R.string.coach_minutes, template.minutes), style = runTextStyle(13.sp, t.label, FontWeight.Medium),
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
            }
        }
    }
}

/** 단계 한 줄 — 순번 · 그림 · 이름 · 설명 · 분. 왼쪽에 이어진 점선 대신 점과 세로줄 */
@Composable
private fun StepRow(index: Int, step: WorkoutStep, last: Boolean) {
    val t = runTone()
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).testTag("coach-step-$index")) {
        Box(Modifier.width(20.dp).fillMaxHeight()) {
            if (!last) {
                Box(Modifier.align(Alignment.TopCenter).padding(top = 18.dp).width(2.dp).fillMaxHeight().background(t.cobaltText.copy(alpha = 0.5f)))
            }
            Box(Modifier.align(Alignment.TopCenter).padding(top = 12.dp).size(12.dp).clip(CircleShape).background(t.cobaltText))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    String.format(Locale.ROOT, "%02d", index + 1), style = runTextStyle(15.sp, t.label, FontWeight.Bold),
                    modifier = Modifier.width(30.dp),
                )
                Icon(stepIcon(step), contentDescription = null, tint = t.text, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(stepTitle(step.role)), style = runTextStyle(18.sp, t.text, FontWeight.Bold, 1.3f))
                    Text(stepSub(step), style = runTextStyle(14.sp, t.label, FontWeight.Medium, 1.35f))
                }
                RunNumber("${step.minutes}", unit = stringResource(R.string.coach_unit_minutes), size = 28.sp, unitSize = 15.sp, italicUnit = false)
            }
            if (!last) {
                Spacer(Modifier.height(12.dp))
                RunDivider()
            }
        }
    }
}
