package com.stepup.android.ui.screens.setup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.BmiBand
import com.stepup.android.domain.BodyMath
import com.stepup.android.domain.BodyProfile
import com.stepup.android.domain.RunMode
import com.stepup.android.ui.components.RunBackdrop
import com.stepup.android.ui.components.RunCard
import com.stepup.android.ui.components.RunNotice
import com.stepup.android.ui.components.RunNoticeKind
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.screens.onboarding.BlueHeadline
import com.stepup.android.ui.screens.onboarding.BluePlainButton
import com.stepup.android.ui.screens.onboarding.BlueTextButton
import com.stepup.android.ui.screens.onboarding.BlueTitleBar
import com.stepup.android.ui.theme.StepUpSans
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

private const val DEFAULT_HEIGHT = 165
private const val DEFAULT_WEIGHT = 60.0
private const val DEFAULT_WEEKS = 12

/** 첫 설정 단계 수 — 몸 정보 · 목표 · 모드 */
private const val SETUP_STEPS = 3

/** 설정 중인 값 — 저장 전까지 화면 안에만 있다 */
private class BodyDraft(profile: BodyProfile) {
    var height by mutableIntStateOf(profile.heightCm ?: DEFAULT_HEIGHT)
    var weight by mutableDoubleStateOf(profile.weightKg ?: DEFAULT_WEIGHT)
    var goal by mutableDoubleStateOf(profile.goalWeightKg ?: profile.weightKg ?: DEFAULT_WEIGHT)
    var weeks by mutableIntStateOf(profile.goalWeeks ?: DEFAULT_WEEKS)

    fun body(base: BodyProfile) = base.copy(heightCm = height, weightKg = weight)
    fun withGoal(base: BodyProfile) = base.copy(goalWeightKg = goal, goalWeeks = weeks)
}

/** 이 기기에 저장된 몸 정보 읽기 — 읽는 중을 빈 값으로, 읽기 실패를 기본값으로 바꾸지 않는다 */
private sealed interface BodyLoad {
    data object Loading : BodyLoad
    data class Ready(val profile: BodyProfile) : BodyLoad
    data object Failed : BodyLoad
}

@Composable
private fun rememberBodyLoad(attempt: Int): BodyLoad {
    val source = remember(attempt) {
        ServiceLocator.userPrefs.bodyProfile
            .map<BodyProfile, BodyLoad> { BodyLoad.Ready(it) }
            .onStart { emit(BodyLoad.Loading) }
            .catch { emit(BodyLoad.Failed) }
    }
    val load by source.collectAsState(initial = BodyLoad.Loading)
    return load
}

/**
 * 첫 설정(파란 톤 v4 ONB21 몸 정보 · ONB22/24 목표 · ONB23/25 모드) — 로그인한 새 회원에게 한 번. 단계마다 건너뛸 수 있다.
 *
 * - 다음은 그 단계 값을 이 기기에 저장한 **뒤에** 넘어간다. 저장하는 동안 다시 누를 수 없고, 실패하면 머물며 알린다.
 * - 몸 정보를 건너뛰면 기본값(165cm · 60kg)을 저장하지 않고 모드로 간다(목표를 계산할 몸 정보가 없다).
 * - 목표를 건너뛰면 목표를 새로 저장하지 않는다. 이미 저장한 몸 정보는 지우지 않는다.
 * - 모드의 계속은 고른 모드와 "첫 설정 봤음"을 저장한 뒤 나간다. 건너뛰기는 임시 선택을 버리고 저장된 모드(처음은 라이트)를 그대로 둔다.
 * - 뒤로는 실제로 지나온 단계로 간다 — 몸 정보를 건너뛰고 모드에 왔으면 몸 정보로(입력하지 않은 몸 정보로 목표를 열지 않는다).
 * 내 정보 › 설정에서 다시 여는 몸 정보 · 모드([BodySettingsScreen] · [ModeSettingsScreen])는 단계 번호 · 건너뛰기가 없는 다른 맥락이다.
 */
@Composable
fun S2SetupFlow(onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val prefs = ServiceLocator.userPrefs
    var attempt by remember { mutableIntStateOf(0) }
    val load = rememberBodyLoad(attempt)
    val storedMode by remember { prefs.runMode.catch { } }.collectAsState(initial = null)
    // 지나온 단계 — "0" → "01" → "012", 몸 정보를 건너뛰면 "02"
    var trail by rememberSaveable { mutableStateOf("0") }
    val step = trail.last().digitToInt()
    var picked by rememberSaveable { mutableStateOf<RunMode?>(null) }
    val mode = picked ?: storedMode ?: RunMode.LITE
    var saving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    // 이번 첫 설정에서 저장한 몸 정보 — 저장이 끝난 값만
    var saved by remember { mutableStateOf<BodyProfile?>(null) }
    val ready = load as? BodyLoad.Ready
    val draft = remember(ready != null) { ready?.let { BodyDraft(it.profile) } }
    val base = saved ?: ready?.profile

    fun go(next: Int) {
        saveFailed = false
        trail += next.toString()
    }
    fun back() {
        if (trail.length > 1 && !saving) {
            saveFailed = false
            trail = trail.dropLast(1)
        }
    }
    /** 저장이 끝나야 다음으로 — 연타는 막고, 실패하면 머문다 */
    fun persist(work: suspend () -> Unit, then: () -> Unit) {
        if (saving) return
        saving = true
        saveFailed = false
        scope.launch {
            try {
                work()
                then()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                saveFailed = true
            } finally {
                saving = false
            }
        }
    }

    BackHandler(enabled = trail.length > 1) { back() }
    Box(Modifier.fillMaxSize().testTag("s2-setup")) {
        RunBackdrop(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            SetupTitleBar(
                title = stringResource(R.string.setup_first_title),
                onBack = if (trail.length > 1) ::back else null,
                progress = step + 1,
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    // 모드 단계는 몸 정보 없이도 고를 수 있다
                    step != 2 && load is BodyLoad.Loading -> SetupWaiting()
                    step != 2 && (load is BodyLoad.Failed || draft == null || base == null) -> SetupLoadFailed(
                        onRetry = { attempt++ },
                        onSkip = { go(2) },
                    )
                    step == 0 -> BodyStep(
                        draft = draft!!,
                        primaryLabel = stringResource(R.string.setup_next),
                        onPrimary = {
                            val next = draft.body(base!!)
                            persist({ prefs.setBodyProfile(next) }) {
                                saved = next
                                if (next.goalWeightKg == null) draft.goal = draft.weight
                                go(1)
                            }
                        },
                        endLabel = stringResource(R.string.setup_skip),
                        // 몸 정보를 건너뛰면 목표 단계는 계산할 것이 없다 — 모드로(입력 전 기본값은 저장하지 않는다)
                        onEnd = { if (!saving) go(2) },
                        busy = saving, failed = saveFailed,
                    )
                    step == 1 -> GoalStep(
                        draft = draft!!, profile = base!!,
                        primaryLabel = stringResource(R.string.setup_next),
                        onPrimary = {
                            val next = draft.withGoal(base)
                            persist({ prefs.setBodyProfile(next) }) {
                                saved = next
                                go(2)
                            }
                        },
                        endLabel = stringResource(R.string.setup_skip),
                        onEnd = { if (!saving) go(2) },
                        busy = saving, failed = saveFailed,
                    )
                    else -> ModeStep(
                        mode = mode, onMode = { picked = it },
                        primaryLabel = stringResource(R.string.setup_continue),
                        onPrimary = {
                            val chosen = mode
                            persist({
                                prefs.setRunMode(chosen)
                                prefs.setS2SetupSeen()
                            }) { onDone() }
                        },
                        endLabel = stringResource(R.string.setup_skip),
                        // 모드를 건너뛰면 저장된 모드(처음은 라이트) 그대로 — 고른 값을 버리고 모드를 쓰지 않는다
                        onEnd = {
                            picked = null
                            persist({ prefs.setS2SetupSeen() }) { onDone() }
                        },
                        busy = saving, failed = saveFailed,
                    )
                }
            }
        }
    }
}

/** 내 정보 › 설정 › 신체 정보 · 목표 — 첫 설정과 같은 두 화면, 끝에 저장. 모두 지울 수 있다(단계 번호 · 건너뛰기 없음). */
@Composable
fun BodySettingsScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val prefs = ServiceLocator.userPrefs
    var attempt by remember { mutableIntStateOf(0) }
    val load = rememberBodyLoad(attempt)
    var step by rememberSaveable { mutableIntStateOf(0) }
    var saving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    val loaded = (load as? BodyLoad.Ready)?.profile
    val draft = remember(loaded == null, loaded == BodyProfile()) { loaded?.let { BodyDraft(it) } }
    fun persist(next: BodyProfile) {
        if (saving) return
        saving = true
        saveFailed = false
        scope.launch {
            try {
                prefs.setBodyProfile(next)
                onBack()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                saveFailed = true
            } finally {
                saving = false
            }
        }
    }
    BackHandler(enabled = step > 0) { step = 0 }
    Box(Modifier.fillMaxSize().testTag("settings-body")) {
        RunBackdrop(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            SetupTitleBar(stringResource(R.string.settings_body), onBack = { if (step > 0) step = 0 else onBack() }, progress = null)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    load is BodyLoad.Loading -> SetupWaiting()
                    loaded == null || draft == null -> SetupLoadFailed(onRetry = { attempt++ }, onSkip = null)
                    step == 0 -> BodyStep(
                        draft = draft,
                        primaryLabel = stringResource(R.string.setup_next),
                        onPrimary = {
                            if (loaded.goalWeightKg == null) draft.goal = draft.weight
                            step = 1
                        },
                        endLabel = if (loaded != BodyProfile()) stringResource(R.string.settings_body_clear) else null,
                        onEnd = { persist(BodyProfile()) },
                        busy = saving, failed = saveFailed,
                    )
                    else -> {
                        val body = draft.body(loaded)
                        GoalStep(
                            draft = draft, profile = body,
                            primaryLabel = stringResource(R.string.setup_save),
                            onPrimary = { persist(draft.withGoal(body)) },
                            endLabel = null, onEnd = {},
                            busy = saving, failed = saveFailed,
                        )
                    }
                }
            }
        }
    }
}

/** 내 정보 › 설정 › 모드 — 고른 뒤 저장 */
@Composable
fun ModeSettingsScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val prefs = ServiceLocator.userPrefs
    val stored by remember { prefs.runMode.catch { } }.collectAsState(initial = null)
    var picked by rememberSaveable { mutableStateOf<RunMode?>(null) }
    var saving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().testTag("settings-mode")) {
        RunBackdrop(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            SetupTitleBar(stringResource(R.string.settings_mode), onBack = onBack, progress = null)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val loaded = stored
                if (loaded == null) {
                    SetupWaiting()
                } else {
                    val mode = picked ?: loaded
                    ModeStep(
                        mode = mode, onMode = { picked = it },
                        primaryLabel = stringResource(R.string.setup_save),
                        onPrimary = {
                            if (!saving) {
                                saving = true
                                saveFailed = false
                                scope.launch {
                                    try {
                                        prefs.setRunMode(mode)
                                        onBack()
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (_: Exception) {
                                        saveFailed = true
                                    } finally {
                                        saving = false
                                    }
                                }
                            }
                        },
                        endLabel = null, onEnd = {},
                        busy = saving, failed = saveFailed,
                    )
                }
            }
        }
    }
}

// ── 머리 · 틀 ───────────────────────────────────────────────────

/** 가운데 "처음 설정", 오른쪽 "1 / 3"(첫 설정만) */
@Composable
private fun SetupTitleBar(title: String, onBack: (() -> Unit)?, progress: Int?) {
    val t = runTone()
    BlueTitleBar(
        title = title, onBack = onBack, backTag = "setup-back",
        trailing = progress?.let {
            {
                val description = stringResource(R.string.setup_progress_desc, it, SETUP_STEPS)
                Text(
                    "$it / $SETUP_STEPS", style = runTextStyle(18.sp, t.cobaltText, FontWeight.Bold),
                    modifier = Modifier.padding(end = 16.dp).semantics { contentDescription = description }.testTag("setup-progress"),
                )
            }
        },
    )
}

/**
 * 한 단계의 틀 — 큰 제목 · 내용(넘김) · 아래 주 버튼 하나와 글자 보조 행동. 버튼은 넘기지 않아도 늘 보인다.
 * 큰 글씨 · 작은 화면이면 내용만 넘긴다(잘라 맞추지 않는다).
 */
@Composable
private fun SetupPage(
    headline: String,
    subtitle: String?,
    primaryLabel: String,
    onPrimary: () -> Unit,
    endLabel: String?,
    onEnd: () -> Unit,
    busy: Boolean,
    failed: Boolean,
    aboveButton: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            BlueHeadline(headline, subtitle = subtitle, size = 34)
            Spacer(Modifier.height(26.dp))
            content()
            Spacer(Modifier.height(16.dp))
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (failed) {
                RunNotice(
                    stringResource(R.string.setup_save_failed), kind = RunNoticeKind.Error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }, tag = "setup-save-failed",
                )
            }
            aboveButton?.invoke()
            BluePlainButton(primaryLabel, onPrimary, Modifier.fillMaxWidth().testTag("setup-primary"), busy = busy)
            if (endLabel != null) {
                BlueTextButton(endLabel, onEnd, Modifier.fillMaxWidth().testTag("setup-secondary"), enabled = !busy)
            }
        }
    }
}

/** 저장된 몸 정보를 읽는 동안 — 빈 값 · 예시값을 미리 보이지 않는다 */
@Composable
private fun SetupWaiting() {
    val t = runTone()
    Column(
        Modifier.fillMaxSize().testTag("setup-loading"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        RunSpinner(Modifier.size(36.dp))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.setup_loading), style = runTextStyle(16.sp, t.label, FontWeight.Medium), textAlign = TextAlign.Center)
    }
}

/** 저장된 몸 정보를 읽지 못했다 — 다시 시도, 첫 설정이면 이 단계를 건너뛸 수도 있다(기본값으로 바꿔 저장하지 않는다) */
@Composable
private fun SetupLoadFailed(onRetry: () -> Unit, onSkip: (() -> Unit)?) {
    val t = runTone()
    Column(Modifier.fillMaxSize().testTag("setup-load-failed")) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                stringResource(R.string.setup_load_failed_title), style = runTextStyle(22.sp, t.text, FontWeight.ExtraBold, 1.3f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.setup_load_failed_body), style = runTextStyle(16.sp, t.label, FontWeight.Medium, 1.45f),
                textAlign = TextAlign.Center,
            )
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BluePlainButton(stringResource(R.string.feed_retry), onRetry, Modifier.fillMaxWidth().testTag("setup-retry"))
            if (onSkip != null) {
                BlueTextButton(stringResource(R.string.setup_skip), onSkip, Modifier.fillMaxWidth().testTag("setup-secondary"))
            }
        }
    }
}

// ── 단계 ────────────────────────────────────────────────────────

/** ONB21 — 키 · 몸무게와 BMI(대한비만학회 네 구간) */
@Composable
private fun BodyStep(
    draft: BodyDraft,
    primaryLabel: String,
    onPrimary: () -> Unit,
    endLabel: String?,
    onEnd: () -> Unit,
    busy: Boolean,
    failed: Boolean,
) {
    val t = runTone()
    val bmi = BodyMath.bmi(draft.height, draft.weight)
    SetupPage(
        headline = stringResource(R.string.setup_body_title),
        subtitle = stringResource(R.string.setup_body_subtitle),
        primaryLabel = primaryLabel, onPrimary = onPrimary, endLabel = endLabel, onEnd = onEnd, busy = busy, failed = failed,
    ) {
        RunCard(padding = PaddingValues(horizontal = 18.dp, vertical = 20.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.setup_bmi_caption), style = runTextStyle(15.sp, t.label, FontWeight.SemiBold))
                Spacer(Modifier.height(4.dp))
                Text(
                    bmi?.let { "%.1f".format(it) } ?: "—",
                    style = bigNumberStyle(66, t.cyan),
                    modifier = Modifier.testTag("setup-bmi"),
                )
                Spacer(Modifier.height(14.dp))
                if (bmi != null) BmiScale(bmi)
            }
        }
        Spacer(Modifier.height(14.dp))
        StepperCard(
            label = stringResource(R.string.setup_height), value = draft.height.toString(), unit = "cm",
            onMinus = { draft.height = BodyMath.clampHeight(draft.height - 1) },
            onPlus = { draft.height = BodyMath.clampHeight(draft.height + 1) },
            tag = "setup-height",
        )
        Spacer(Modifier.height(12.dp))
        StepperCard(
            label = stringResource(R.string.setup_weight), value = "%.1f".format(draft.weight), unit = "kg",
            onMinus = { draft.weight = BodyMath.clampWeight(draft.weight - 0.5) },
            onPlus = { draft.weight = BodyMath.clampWeight(draft.weight + 0.5) },
            tag = "setup-weight",
        )
    }
}

/** ONB22 · ONB24 — 목표 몸무게와 기간. 주의 안내(ONB24)는 설명일 뿐 다음을 막지 않는다 */
@Composable
private fun GoalStep(
    draft: BodyDraft,
    profile: BodyProfile,
    primaryLabel: String,
    onPrimary: () -> Unit,
    endLabel: String?,
    onEnd: () -> Unit,
    busy: Boolean,
    failed: Boolean,
) {
    val t = runTone()
    val from = profile.weightKg ?: draft.weight
    // 하루 걸음 목표는 저장값을 읽은 뒤에만 — 읽는 동안 예시 숫자를 확정 값처럼 보이지 않는다
    val dailyGoal by remember { ServiceLocator.userPrefs.dailyGoal.catch { } }.collectAsState(initial = null)
    val preview = profile.copy(weightKg = from, goalWeightKg = draft.goal, goalWeeks = draft.weeks)
    val diff = BodyMath.round1(draft.goal - from)
    val weekly = BodyMath.weeklyChange(from, draft.goal, draft.weeks) ?: 0.0
    SetupPage(
        headline = stringResource(R.string.setup_goal_title),
        subtitle = dailyGoal?.let { stringResource(R.string.setup_goal_subtitle, "%,d".format(it)) } ?: " ",
        primaryLabel = primaryLabel, onPrimary = onPrimary, endLabel = endLabel, onEnd = onEnd, busy = busy, failed = failed,
    ) {
        RunCard(padding = PaddingValues(horizontal = 18.dp, vertical = 20.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("%.1fkg → %.1fkg".format(from, draft.goal), style = runTextStyle(17.sp, t.label, FontWeight.SemiBold))
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        when {
                            diff > 0 -> "+%.1f".format(diff)
                            diff < 0 -> "−%.1f".format(-diff)
                            else -> "0.0"
                        },
                        style = bigNumberStyle(60, t.cyan),
                        modifier = Modifier.testTag("setup-goal-diff"),
                    )
                    Text(" kg", style = bigNumberStyle(40, t.cyan), modifier = Modifier.padding(bottom = 4.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.setup_goal_weeks, draft.weeks) + " · " +
                        stringResource(R.string.setup_goal_weekly, "%.1f".format(weekly)),
                    style = runTextStyle(17.sp, t.label, FontWeight.SemiBold), textAlign = TextAlign.Center,
                )
                preview.goalBmi?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.setup_goal_bmi, "%.1f".format(it)),
                        style = runTextStyle(16.sp, t.label, FontWeight.Medium), textAlign = TextAlign.Center)
                }
                if (BodyMath.needsCaution(preview)) {
                    Spacer(Modifier.height(14.dp))
                    GoalCaution()
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        StepperCard(
            label = stringResource(R.string.setup_goal_target), value = "%.1f".format(draft.goal), unit = "kg",
            onMinus = { draft.goal = BodyMath.clampWeight(draft.goal - 0.5) },
            onPlus = { draft.goal = BodyMath.clampWeight(draft.goal + 0.5) },
            tag = "setup-goal",
        )
        Spacer(Modifier.height(12.dp))
        RunCard(padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.setup_goal_period), style = runTextStyle(19.sp, t.text, FontWeight.Bold),
                    modifier = Modifier.padding(start = 4.dp, end = 12.dp),
                )
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BodyMath.GOAL_WEEKS.forEach { weeks ->
                        WeeksChip(
                            stringResource(R.string.setup_goal_weeks, weeks), selected = draft.weeks == weeks,
                            onClick = { draft.weeks = weeks }, modifier = Modifier.weight(1f).testTag("setup-weeks-$weeks"),
                        )
                    }
                }
            }
        }
    }
}

/** 목표 주의(ONB24) — 이 묶음에서 경고 색은 여기만. 기존 조건(주당 1kg 초과 · 목표 BMI 저체중)의 안내 그대로 */
@Composable
private fun GoalCaution() {
    val t = runTone()
    val shape = RoundedCornerShape(14.dp)
    val ink = if (t.dark) Color(0xFFF4C95D) else Color(0xFF7A5200)
    Text(
        stringResource(R.string.setup_goal_caution),
        style = runTextStyle(15.sp, ink, FontWeight.SemiBold, 1.5f), textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clip(shape)
            .background(if (t.dark) Color(0xFF2A2716) else Color(0xFFFFF4D6), shape)
            .border(1.5.dp, if (t.dark) Color(0xFFD9A62A) else Color(0xFFE0B040), shape)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("setup-goal-caution"),
    )
}

/** ONB23 · ONB25 — 라이트 · 러너 중 하나(라디오). 두 모드의 SUP 적립 규칙은 같다 */
@Composable
private fun ModeStep(
    mode: RunMode,
    onMode: (RunMode) -> Unit,
    primaryLabel: String,
    onPrimary: () -> Unit,
    endLabel: String?,
    onEnd: () -> Unit,
    busy: Boolean,
    failed: Boolean,
) {
    val t = runTone()
    SetupPage(
        headline = stringResource(R.string.setup_mode_title),
        subtitle = stringResource(R.string.setup_mode_subtitle),
        primaryLabel = primaryLabel, onPrimary = onPrimary, endLabel = endLabel, onEnd = onEnd, busy = busy, failed = failed,
        aboveButton = {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 4.dp).semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.setup_mode_current), style = runTextStyle(16.sp, t.label, FontWeight.Medium))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(modeName(mode)), style = runTextStyle(18.sp, t.cyan, FontWeight.ExtraBold))
            }
        },
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ModeCard(RunMode.LITE, R.string.setup_mode_lite_body, mode == RunMode.LITE) { onMode(RunMode.LITE) }
            ModeCard(RunMode.RUNNER, R.string.setup_mode_runner_body, mode == RunMode.RUNNER) { onMode(RunMode.RUNNER) }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.setup_mode_note), style = runTextStyle(15.sp, t.label, FontWeight.Medium),
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

fun modeName(mode: RunMode): Int = when (mode) {
    RunMode.LITE -> R.string.setup_mode_lite
    RunMode.RUNNER -> R.string.setup_mode_runner
}

@Composable
private fun ModeCard(mode: RunMode, body: Int, picked: Boolean, onPick: () -> Unit) {
    val t = runTone()
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(
                if (picked) Brush.verticalGradient(listOf(if (t.dark) Color(0xFF0A3B80) else Color(0xFFE3EDFF), t.panel))
                else Brush.verticalGradient(listOf(t.panelTop, t.panel)),
                shape,
            )
            .border(if (picked) 2.dp else 1.dp, if (picked) Color(0xFF2B6DFF) else t.panelEdge, shape)
            .selectable(selected = picked, role = Role.RadioButton, onClick = onPick)
            .testTag("mode-${mode.name.lowercase()}")
            .padding(horizontal = 22.dp, vertical = 22.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(modeName(mode)), style = runTextStyle(28.sp, t.text, FontWeight.ExtraBold, 1.2f))
            Text(stringResource(body), style = runTextStyle(16.sp, t.label, FontWeight.Medium, 1.5f))
        }
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier.padding(top = 4.dp).size(30.dp).clip(CircleShape)
                .border(2.dp, if (picked) t.cyan else t.label.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (picked) Box(Modifier.size(16.dp).clip(CircleShape).background(t.cyan))
        }
    }
}

// ── 부품 ────────────────────────────────────────────────────────

/** 큰 숫자 — 기울이지 않은 가장 굵은 글자(시안의 BMI · 목표 차이) */
private fun bigNumberStyle(size: Int, color: Color): TextStyle = TextStyle(
    fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = size.sp, color = color,
    letterSpacing = (-0.02).em, lineHeight = 1.1.em, fontFeatureSettings = "tnum",
)

/** 대한비만학회 네 구간을 한 줄로, 지금 값이 든 구간만 청록 */
@Composable
private fun BmiScale(bmi: Double) {
    val t = runTone()
    val current = BodyMath.band(bmi)
    val bands = listOf(
        BmiBand.UNDER to R.string.setup_bmi_under,
        BmiBand.NORMAL to R.string.setup_bmi_normal,
        BmiBand.PRE_OBESE to R.string.setup_bmi_pre,
        BmiBand.OBESE to R.string.setup_bmi_obese,
    )
    Row(Modifier.fillMaxWidth().testTag("setup-bmi-scale"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        bands.forEach { (band, label) ->
            val on = band == current
            Column(
                Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(if (on) t.cyan else t.muted.copy(alpha = 0.55f)))
                Text(
                    stringResource(label), style = runTextStyle(13.sp, if (on) t.cyan else t.label, if (on) FontWeight.Bold else FontWeight.Medium, 1.35f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** 이름 · (−) 값 단위 (+). 누르고 있으면 계속 바뀐다(400ms 뒤 70ms 마다) */
@Composable
private fun StepperCard(label: String, value: String, unit: String, onMinus: () -> Unit, onPlus: () -> Unit, tag: String) {
    val t = runTone()
    RunCard(padding = PaddingValues(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 10.dp), tag = tag) {
        Row(Modifier.fillMaxWidth().heightIn(min = 60.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = runTextStyle(19.sp, t.text, FontWeight.Bold), modifier = Modifier.weight(1f).padding(end = 8.dp))
            RepeatButton(Icons.Filled.Remove, stringResource(R.string.setup_decrease, label), onMinus, "$tag-minus")
            Row(
                Modifier.widthIn(min = 112.dp).padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom,
            ) {
                Text(value, style = runTextStyle(27.sp, t.text, FontWeight.ExtraBold, 1.15f), maxLines = 1,
                    modifier = Modifier.testTag("$tag-value"))
                Text(" $unit", style = runTextStyle(24.sp, t.text, FontWeight.Bold, 1.15f), maxLines = 1)
            }
            RepeatButton(Icons.Filled.Add, stringResource(R.string.setup_increase, label), onPlus, "$tag-plus")
        }
    }
}

/** 기간 하나(8 · 12 · 16주) — 고르면 파란 면 */
@Composable
private fun WeeksChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val t = runTone()
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier.heightIn(min = 54.dp).clip(shape)
            .background(if (selected) t.cobalt else t.secondaryFace, shape)
            .border(1.5.dp, if (selected) t.cobalt else t.label.copy(alpha = 0.7f), shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = runTextStyle(18.sp, if (selected) Color.White else t.text, FontWeight.Bold), textAlign = TextAlign.Center, maxLines = 1)
    }
}

/** 한 번 누르면 한 칸, 누르고 있으면 400ms 뒤 70ms 마다. 손을 떼거나 화면을 나가면 멈춘다. 화면 낭독기는 한 번 누르기로 한 칸 */
@Composable
private fun RepeatButton(icon: ImageVector, description: String, onStep: () -> Unit, tag: String) {
    val t = runTone()
    val step by rememberUpdatedState(onStep)
    val scope = rememberCoroutineScope()
    Box(
        Modifier.size(56.dp)
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
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(t.secondaryFace)
                .border(1.5.dp, t.label.copy(alpha = 0.85f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = t.text, modifier = Modifier.size(24.dp))
        }
    }
}
