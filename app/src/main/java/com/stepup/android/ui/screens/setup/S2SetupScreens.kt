package com.stepup.android.ui.screens.setup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.BmiBand
import com.stepup.android.domain.BodyMath
import com.stepup.android.domain.BodyProfile
import com.stepup.android.domain.RunMode
import com.stepup.android.ui.components.FocusHeader
import com.stepup.android.ui.components.S2ActionRow
import com.stepup.android.ui.components.S2Headline
import com.stepup.android.ui.components.S2Kicker
import com.stepup.android.ui.components.S2Number
import com.stepup.android.ui.components.S2RoundAction
import com.stepup.android.ui.components.S2SideInfo
import com.stepup.android.ui.components.S2Stage
import com.stepup.android.ui.components.S2Subtitle
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.StepUpDesign
import com.stepup.android.ui.theme.VoltText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DEFAULT_HEIGHT = 165
private const val DEFAULT_WEIGHT = 60.0
private const val DEFAULT_WEEKS = 12

/** 설정 중인 값 — 저장 전까지 화면 안에만 있다 */
private class BodyDraft(profile: BodyProfile) {
    var height by mutableIntStateOf(profile.heightCm ?: DEFAULT_HEIGHT)
    var weight by mutableDoubleStateOf(profile.weightKg ?: DEFAULT_WEIGHT)
    var goal by mutableDoubleStateOf(profile.goalWeightKg ?: profile.weightKg ?: DEFAULT_WEIGHT)
    var weeks by mutableIntStateOf(profile.goalWeeks ?: DEFAULT_WEEKS)

    fun body(base: BodyProfile) = base.copy(heightCm = height, weightKg = weight)
    fun withGoal(base: BodyProfile) = base.copy(goalWeightKg = goal, goalWeeks = weeks)
}

/**
 * S2 첫 설정(시안 16 · 17 · 19) — 새로 가입한 사람에게 한 번. 단계마다 건너뛸 수 있다.
 * 건너뛴 단계의 값은 저장하지 않는다. 끝나면(또는 끝까지 건너뛰면) [onDone].
 */
@Composable
fun S2SetupFlow(onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val prefs = ServiceLocator.userPrefs
    val stored by prefs.bodyProfile.collectAsState(initial = null)
    val storedMode by prefs.runMode.collectAsState(initial = RunMode.LITE)
    val loaded = stored ?: return
    var step by rememberSaveable { mutableIntStateOf(0) }
    var saved by remember { mutableStateOf(loaded) }
    val draft = remember { BodyDraft(loaded) }
    var mode by rememberSaveable { mutableStateOf(storedMode) }
    val finish: () -> Unit = {
        scope.launch {
            prefs.setRunMode(mode)
            prefs.setS2SetupSeen()
            onDone()
        }
    }
    val save: (BodyProfile) -> Unit = { next ->
        saved = next
        scope.launch { prefs.setBodyProfile(next) }
    }
    BackHandler(enabled = step > 0) { step -= 1 }
    Box(Modifier.fillMaxSize().testTag("s2-setup")) {
        S2Stage(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            when (step) {
                0 -> BodyStep(
                    draft = draft, progress = "1/3",
                    primaryLabel = stringResource(R.string.setup_next),
                    onPrimary = {
                        save(draft.body(saved))
                        if (saved.goalWeightKg == null) draft.goal = draft.weight
                        step = 1
                    },
                    endLabel = stringResource(R.string.setup_skip),
                    // 몸무게를 건너뛰면 목표 단계는 계산할 것이 없다 — 모드로
                    onEnd = { step = 2 },
                )
                1 -> GoalStep(
                    draft = draft, profile = saved, progress = "2/3",
                    primaryLabel = stringResource(R.string.setup_next),
                    onPrimary = { save(draft.withGoal(saved)); step = 2 },
                    endLabel = stringResource(R.string.setup_skip), onEnd = { step = 2 },
                )
                else -> ModeStep(
                    mode = mode, onMode = { mode = it }, progress = "3/3",
                    primaryLabel = stringResource(R.string.setup_continue), onPrimary = finish,
                    endLabel = stringResource(R.string.setup_skip),
                    // 모드를 건너뛰면 기본(라이트) 그대로 — 고른 값을 저장하지 않는다
                    onEnd = { mode = storedMode; finish() },
                )
            }
        }
    }
}

/** 내 정보 › 설정 › 신체 정보 · 목표 — 첫 설정과 같은 두 단계, 끝에 저장. 모두 지울 수 있다. */
@Composable
fun BodySettingsScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val prefs = ServiceLocator.userPrefs
    val stored by prefs.bodyProfile.collectAsState(initial = null)
    val loaded = stored ?: return
    var step by rememberSaveable { mutableIntStateOf(0) }
    val draft = remember(loaded == BodyProfile()) { BodyDraft(loaded) }
    BackHandler(enabled = step > 0) { step = 0 }
    Column(Modifier.fillMaxSize().testTag("settings-body")) {
        Box(Modifier.padding(horizontal = StepUpDesign.Gutter)) {
            FocusHeader(stringResource(R.string.settings_body), onBack = { if (step > 0) step = 0 else onBack() })
        }
        if (step == 0) {
            BodyStep(
                draft = draft, progress = null,
                primaryLabel = stringResource(R.string.setup_next),
                onPrimary = {
                    if (loaded.goalWeightKg == null) draft.goal = draft.weight
                    step = 1
                },
                endLabel = if (loaded != BodyProfile()) stringResource(R.string.settings_body_clear) else null,
                onEnd = { scope.launch { prefs.setBodyProfile(BodyProfile()); onBack() } },
                showKicker = false,
            )
        } else {
            val body = draft.body(loaded)
            GoalStep(
                draft = draft, profile = body, progress = null,
                primaryLabel = stringResource(R.string.setup_save),
                onPrimary = { scope.launch { prefs.setBodyProfile(draft.withGoal(body)); onBack() } },
                endLabel = null, onEnd = {}, showKicker = false,
            )
        }
    }
}

/** 내 정보 › 설정 › 모드 */
@Composable
fun ModeSettingsScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val prefs = ServiceLocator.userPrefs
    val stored by prefs.runMode.collectAsState(initial = null)
    val loaded = stored ?: return
    var mode by rememberSaveable(loaded) { mutableStateOf(loaded) }
    Column(Modifier.fillMaxSize().testTag("settings-mode")) {
        Box(Modifier.padding(horizontal = StepUpDesign.Gutter)) {
            FocusHeader(stringResource(R.string.settings_mode), onBack = onBack)
        }
        ModeStep(
            mode = mode, onMode = { mode = it }, progress = null,
            primaryLabel = stringResource(R.string.setup_save),
            onPrimary = { scope.launch { prefs.setRunMode(mode); onBack() } },
            endLabel = null, onEnd = {}, showKicker = false,
        )
    }
}

// ── 단계 ────────────────────────────────────────────────────────

@Composable
private fun SetupPage(
    kicker: String?,
    headline: String,
    subtitle: String,
    progress: String?,
    primaryIcon: ImageVector,
    primaryLabel: String,
    onPrimary: () -> Unit,
    endLabel: String?,
    onEnd: () -> Unit,
    startInfo: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = StepUpDesign.Gutter).padding(bottom = 12.dp)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(if (kicker != null) 32.dp else 8.dp))
            if (kicker != null) {
                S2Kicker(kicker)
                Spacer(Modifier.height(12.dp))
            }
            S2Headline(headline)
            Spacer(Modifier.height(10.dp))
            S2Subtitle(subtitle)
            Spacer(Modifier.height(20.dp))
            content()
        }
        S2ActionRow(
            start = {
                when {
                    startInfo != null -> startInfo()
                    progress != null -> S2SideInfo(stringResource(R.string.setup_step), value = progress)
                }
            },
            end = {
                if (endLabel != null) {
                    S2SideInfo(endLabel, end = true, onClick = onEnd, modifier = Modifier.testTag("setup-secondary"))
                }
            },
        ) {
            S2RoundAction(icon = primaryIcon, label = primaryLabel, onClick = onPrimary,
                modifier = Modifier.testTag("setup-primary"))
        }
    }
}

@Composable
private fun BodyStep(
    draft: BodyDraft,
    progress: String?,
    primaryLabel: String,
    onPrimary: () -> Unit,
    endLabel: String?,
    onEnd: () -> Unit,
    showKicker: Boolean = true,
) {
    val bmi = BodyMath.bmi(draft.height, draft.weight)
    SetupPage(
        kicker = if (showKicker) stringResource(R.string.setup_body_kicker) else null,
        headline = stringResource(R.string.setup_body_title),
        subtitle = stringResource(R.string.setup_body_subtitle),
        progress = progress, primaryIcon = Icons.AutoMirrored.Filled.ArrowForward,
        primaryLabel = primaryLabel, onPrimary = onPrimary, endLabel = endLabel, onEnd = onEnd,
    ) {
        Text(stringResource(R.string.setup_bmi_caption), color = Silver, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(6.dp))
        S2Number(bmi?.let { "%.1f".format(it) } ?: "—", 56.sp, modifier = Modifier.testTag("setup-bmi"))
        Spacer(Modifier.height(12.dp))
        if (bmi != null) BmiScale(bmi)
        Spacer(Modifier.height(20.dp))
        StepperRow(
            label = stringResource(R.string.setup_height), value = draft.height.toString(), unit = "cm",
            onMinus = { draft.height = BodyMath.clampHeight(draft.height - 1) },
            onPlus = { draft.height = BodyMath.clampHeight(draft.height + 1) },
            tag = "setup-height",
        )
        StepperRow(
            label = stringResource(R.string.setup_weight), value = "%.1f".format(draft.weight), unit = "kg",
            onMinus = { draft.weight = BodyMath.clampWeight(draft.weight - 0.5) },
            onPlus = { draft.weight = BodyMath.clampWeight(draft.weight + 0.5) },
            tag = "setup-weight",
        )
    }
}

@Composable
private fun GoalStep(
    draft: BodyDraft,
    profile: BodyProfile,
    progress: String?,
    primaryLabel: String,
    onPrimary: () -> Unit,
    endLabel: String?,
    onEnd: () -> Unit,
    showKicker: Boolean = true,
) {
    val from = profile.weightKg ?: draft.weight
    val dailyGoal by ServiceLocator.userPrefs.dailyGoal.collectAsState(initial = null)
    val preview = profile.copy(weightKg = from, goalWeightKg = draft.goal, goalWeeks = draft.weeks)
    val diff = BodyMath.round1(draft.goal - from)
    val weekly = BodyMath.weeklyChange(from, draft.goal, draft.weeks) ?: 0.0
    SetupPage(
        kicker = if (showKicker) stringResource(R.string.setup_goal_kicker) else null,
        headline = stringResource(R.string.setup_goal_title),
        subtitle = dailyGoal?.let { stringResource(R.string.setup_goal_subtitle, "%,d".format(it)) } ?: " ",
        progress = progress, primaryIcon = if (progress == null) Icons.Filled.Check else Icons.AutoMirrored.Filled.ArrowForward,
        primaryLabel = primaryLabel, onPrimary = onPrimary, endLabel = endLabel, onEnd = onEnd,
    ) {
        Text("%.1fkg → %.1fkg".format(from, draft.goal), color = Silver, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            S2Number(
                when {
                    diff > 0 -> "+%.1f".format(diff)
                    diff < 0 -> "−%.1f".format(-diff)
                    else -> "0.0"
                },
                56.sp, modifier = Modifier.width(180.dp).testTag("setup-goal-diff"),
            )
            Text("kg", color = Silver, fontSize = 15.sp, modifier = Modifier.padding(bottom = 12.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            buildString {
                append(stringResource(R.string.setup_goal_weeks, draft.weeks))
                append(" · ")
                append(stringResource(R.string.setup_goal_weekly, "%.1f".format(weekly)))
                preview.goalBmi?.let { append(" · "); append(stringResource(R.string.setup_goal_bmi, "%.1f".format(it))) }
            },
            color = Silver, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
        )
        if (BodyMath.needsCaution(preview)) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.setup_goal_caution), color = StepUpColors.alert,
                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                modifier = Modifier.testTag("setup-goal-caution"))
        }
        Spacer(Modifier.height(20.dp))
        StepperRow(
            label = stringResource(R.string.setup_goal_target), value = "%.1f".format(draft.goal), unit = "kg",
            onMinus = { draft.goal = BodyMath.clampWeight(draft.goal - 0.5) },
            onPlus = { draft.goal = BodyMath.clampWeight(draft.goal + 0.5) },
            tag = "setup-goal",
        )
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.setup_goal_period), color = Silver, modifier = Modifier.width(72.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BodyMath.GOAL_WEEKS.forEach { weeks ->
                    com.stepup.android.ui.components.PillChip(
                        text = stringResource(R.string.setup_goal_weeks, weeks),
                        selected = draft.weeks == weeks,
                        onClick = { draft.weeks = weeks },
                        modifier = Modifier.testTag("setup-weeks-$weeks"),
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeStep(
    mode: RunMode,
    onMode: (RunMode) -> Unit,
    progress: String?,
    primaryLabel: String,
    onPrimary: () -> Unit,
    endLabel: String?,
    onEnd: () -> Unit,
    showKicker: Boolean = true,
) {
    SetupPage(
        kicker = if (showKicker) stringResource(R.string.setup_mode_kicker) else null,
        headline = stringResource(R.string.setup_mode_title),
        subtitle = stringResource(R.string.setup_mode_subtitle),
        progress = progress,
        primaryIcon = if (progress == null) Icons.Filled.Check else Icons.AutoMirrored.Filled.ArrowForward,
        primaryLabel = primaryLabel, onPrimary = onPrimary, endLabel = endLabel, onEnd = onEnd,
        startInfo = {
            S2SideInfo(stringResource(R.string.setup_mode_current), value = stringResource(modeName(mode)))
        },
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeCard(RunMode.LITE, R.string.setup_mode_lite_body, mode == RunMode.LITE) { onMode(RunMode.LITE) }
            ModeCard(RunMode.RUNNER, R.string.setup_mode_runner_body, mode == RunMode.RUNNER) { onMode(RunMode.RUNNER) }
            Text(stringResource(R.string.setup_mode_note), color = Slate,
                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
        }
    }
}

fun modeName(mode: RunMode): Int = when (mode) {
    RunMode.LITE -> R.string.setup_mode_lite
    RunMode.RUNNER -> R.string.setup_mode_runner
}

@Composable
private fun ModeCard(mode: RunMode, body: Int, picked: Boolean, onPick: () -> Unit) {
    val shape = RoundedCornerShape(StepUpDesign.PanelRadius)
    Column(
        Modifier.fillMaxWidth()
            .background(if (picked) StepUpColors.carbonHigh else StepUpColors.carbon, shape)
            .border(if (picked) 2.dp else 1.dp, if (picked) VoltText else StepUpColors.edge, shape)
            .selectable(selected = picked, role = Role.RadioButton, onClick = onPick)
            .testTag("mode-${mode.name.lowercase()}")
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(modeName(mode)), color = Snow, style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f))
            if (picked) Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Snow, modifier = Modifier.size(22.dp))
        }
        Text(stringResource(body), color = Silver, style = MaterialTheme.typography.bodyMedium)
    }
}

// ── 부품 ────────────────────────────────────────────────────────

/** 대한비만학회 네 구간을 한 줄로, 지금 값이 든 구간만 밝게 */
@Composable
private fun BmiScale(bmi: Double) {
    val current = BodyMath.band(bmi)
    val bands = listOf(
        BmiBand.UNDER to R.string.setup_bmi_under,
        BmiBand.NORMAL to R.string.setup_bmi_normal,
        BmiBand.PRE_OBESE to R.string.setup_bmi_pre,
        BmiBand.OBESE to R.string.setup_bmi_obese,
    )
    Row(Modifier.fillMaxWidth().testTag("setup-bmi-scale"), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        bands.forEach { (band, label) ->
            val on = band == current
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.fillMaxWidth().height(3.dp)
                    .background(if (on) VoltText else StepUpColors.edge, RoundedCornerShape(2.dp)))
                Text(stringResource(label), color = if (on) Snow else Slate, fontSize = 11.sp,
                    textAlign = TextAlign.Center, lineHeight = 14.sp)
            }
        }
    }
}

/** 이름 · [−] 값 단위 [+]. 누르고 있으면 계속 바뀐다. */
@Composable
private fun StepperRow(label: String, value: String, unit: String, onMinus: () -> Unit, onPlus: () -> Unit, tag: String) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Silver, modifier = Modifier.width(72.dp))
        RepeatButton(Icons.Filled.Remove, stringResource(R.string.setup_decrease, label), onMinus, "$tag-minus")
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
            Text(value, color = Snow, fontSize = 26.sp, modifier = Modifier.testTag("$tag-value"))
            Text(" $unit", color = Silver, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
        }
        RepeatButton(Icons.Filled.Add, stringResource(R.string.setup_increase, label), onPlus, "$tag-plus")
    }
}

@Composable
private fun RepeatButton(icon: ImageVector, description: String, onStep: () -> Unit, tag: String) {
    val step by rememberUpdatedState(onStep)
    val scope = rememberCoroutineScope()
    Box(
        Modifier.size(48.dp)
            .semantics {
                role = Role.Button
                contentDescription = description
                onClick { step(); true }
            }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    step()
                    val repeat = scope.launch {
                        delay(400)
                        while (true) { step(); delay(70) }
                    }
                    tryAwaitRelease()
                    repeat.cancel()
                })
            }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(34.dp).border(1.dp, StepUpColors.edge, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Snow, modifier = Modifier.size(18.dp))
        }
    }
}
