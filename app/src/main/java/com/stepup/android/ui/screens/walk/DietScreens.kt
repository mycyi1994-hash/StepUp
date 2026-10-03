package com.stepup.android.ui.screens.walk

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.ErrorOutline
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.BodyMath
import com.stepup.android.domain.DietRoutine
import com.stepup.android.domain.RunExperience
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunButtonKind
import com.stepup.android.ui.components.RunCard
import com.stepup.android.ui.components.RunChoiceRow
import com.stepup.android.ui.components.RunNumber
import com.stepup.android.ui.components.RunPage
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.RunSheetText
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.RunStateArt
import com.stepup.android.ui.components.RunTextAction
import com.stepup.android.ui.components.runNumberStyle
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 몸 정보 입력칸 하나의 확인 결과 — 비었으면 EMPTY, 범위를 벗어나면 RANGE */
internal enum class BodyInputError { EMPTY, RANGE }

internal object DietInput {
    fun heightError(text: String): BodyInputError? {
        val cm = text.toIntOrNull() ?: return if (text.isBlank()) BodyInputError.EMPTY else BodyInputError.RANGE
        return if (cm in BodyMath.MIN_HEIGHT..BodyMath.MAX_HEIGHT) null else BodyInputError.RANGE
    }

    fun weightError(text: String): BodyInputError? {
        val kg = text.toDoubleOrNull() ?: return if (text.isBlank()) BodyInputError.EMPTY else BodyInputError.RANGE
        return if (kg >= BodyMath.MIN_WEIGHT && kg <= BodyMath.MAX_WEIGHT) null else BodyInputError.RANGE
    }

    /** 키패드 한 글자 — 키는 정수 세 자리, 몸무게는 소수점 한 자리까지 */
    fun type(current: String, key: Char, decimal: Boolean): String {
        if (key == '.') {
            return if (!decimal || current.contains('.')) current else (current.ifEmpty { "0" } + ".")
        }
        val next = if (current == "0") key.toString() else current + key
        val whole = next.substringBefore('.')
        val fraction = next.substringAfter('.', "")
        return when {
            !decimal && next.length > 3 -> current
            whole.length > 3 -> current
            fraction.length > 1 -> current
            else -> next
        }
    }

    /**
     * 휴대폰 숫자 자판(D01)으로 들어온 글 — 숫자(0~9)만 남기고, 키는 정수 세 자리, 몸무게는 소수점 한 자리까지.
     * 쉼표를 소수점으로 쓰는 자판도 있어 쉼표는 소수점으로 읽는다. 붙여 넣은 글자도 같은 규칙으로 걸러진다.
     */
    fun clean(text: String, decimal: Boolean): String {
        val out = StringBuilder()
        var dot = false
        var whole = 0
        var fraction = 0
        for (c in text) {
            when {
                c in '0'..'9' && !dot && whole < 3 -> { out.append(c); whole++ }
                c in '0'..'9' && dot && fraction < 1 -> { out.append(c); fraction++ }
                (c == '.' || c == ',') && decimal && !dot -> {
                    if (whole == 0) { out.append('0'); whole = 1 }
                    out.append('.')
                    dot = true
                }
            }
        }
        return out.toString()
    }

    fun format(kg: Double): String = if (kg % 1.0 == 0.0) kg.toLong().toString() else "%.1f".format(java.util.Locale.ROOT, kg)
}

/** 입력 · 준비 중(D04) · 준비 실패(D05) */
private enum class DietPhase { Input, Preparing, Failed }

/**
 * 다이어트 모드 입력(시안 U05 · D01 · D02 · D03 · D04 · D05) — 키 · 몸무게 · 러닝 경험.
 * [editing] 이면 몸 정보 수정(D06): "수정하고 다시 추천".
 * 키 · 몸무게는 기록용(이 폰에만), 러닝 경험은 루틴을 고르는 기준이다. 숫자는 휴대폰 숫자 자판으로 적는다.
 */
@Composable
fun DietInputScreen(
    onBack: () -> Unit,
    onNext: (RunExperience) -> Unit,
    editing: Boolean = false,
) {
    val prefs = ServiceLocator.userPrefs
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var height by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var experienceKey by rememberSaveable { mutableStateOf<String?>(null) }
    var heightTouched by rememberSaveable { mutableStateOf(false) }
    var weightTouched by rememberSaveable { mutableStateOf(false) }
    var focusedField by remember { mutableStateOf(0) }
    var dirty by rememberSaveable { mutableStateOf(false) }
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    var loaded by rememberSaveable { mutableStateOf(false) }
    var phase by rememberSaveable { mutableStateOf(DietPhase.Input) }
    // 저장해 둔 값이 있으면 채워 둔다 — 다시 들어와도 처음부터 적지 않게
    LaunchedEffect(Unit) {
        if (!loaded) {
            val body = prefs.bodyProfile.first()
            body.heightCm?.let { height = it.toString() }
            body.weightKg?.let { weight = DietInput.format(it) }
            experienceKey = prefs.runExperience.first()?.key
            loaded = true
        }
    }
    val experience = RunExperience.of(experienceKey)
    val heightError = DietInput.heightError(height).takeIf { heightTouched }
    val weightError = DietInput.weightError(weight).takeIf { weightTouched }
    val valid = DietInput.heightError(height) == null && DietInput.weightError(weight) == null && experience != null
    val leave = { if (dirty) confirmLeave = true else onBack() }
    // 자판이 떠 있으면 시스템이 먼저 자판을 내린다 — 그다음 뒤로 가기에서 적던 내용이 있으면 묻는다(D03)
    BackHandler(enabled = dirty && phase == DietPhase.Input) { confirmLeave = true }
    BackHandler(enabled = phase != DietPhase.Input) { phase = DietPhase.Input }

    // 러닝 방법 준비(D04) — 몸 정보 · 경험을 이 폰에 저장하고 루틴을 고른다. 저장이 실패하면 D05(입력은 그대로)
    val submit: () -> Unit = submit@{
        val cm = height.toIntOrNull()
        val kg = weight.toDoubleOrNull()
        val chosen = experience
        if (!valid || cm == null || kg == null || chosen == null) {
            heightTouched = true
            weightTouched = true
            return@submit
        }
        focusManager.clearFocus()
        phase = DietPhase.Preparing
        scope.launch {
            val saved = runCatching {
                val body = prefs.bodyProfile.first()
                prefs.setBodyProfile(body.copy(heightCm = cm, weightKg = BodyMath.round1(kg)))
                prefs.setRunExperience(chosen)
            }.isSuccess
            // 저장은 다른 스레드에서 끝날 수 있다 — 화면 상태 · 다음 화면 이동은 메인에서
            withContext(Dispatchers.Main.immediate) {
                // 준비 중에 뒤로 가 입력으로 돌아왔으면 저장만 하고 다음 화면으로 가지 않는다
                val waiting = phase == DietPhase.Preparing
                if (saved) {
                    dirty = false
                    if (waiting) {
                        phase = DietPhase.Input
                        onNext(chosen)
                    }
                } else if (waiting) {
                    phase = DietPhase.Failed
                }
            }
        }
    }

    when (phase) {
        DietPhase.Preparing -> DietPreparingContent(onBack = { phase = DietPhase.Input })
        DietPhase.Failed -> DietFailedContent(onRetry = submit, onCheck = { phase = DietPhase.Input })
        DietPhase.Input -> DietInputContent(
            editing = editing,
            height = height, weight = weight,
            heightError = heightError, weightError = weightError,
            experience = experience, valid = valid, keyboardUp = focusedField != 0,
            onHeight = { height = it; heightTouched = true; dirty = true },
            onWeight = { weight = it; weightTouched = true; dirty = true },
            onFocus = { field, focused -> if (focused) focusedField = field else if (focusedField == field) focusedField = 0 },
            onExperience = { experienceKey = it.key; dirty = true },
            onDone = { focusManager.clearFocus() },
            onSubmit = submit,
            onBack = leave,
        )
    }

    // D03 — 입력을 그만할까요? 바깥 · 뒤로 · 취소는 계속 입력
    if (confirmLeave) {
        RunSheet(onDismiss = { confirmLeave = false }, modifier = Modifier.testTag("diet-leave-dialog")) {
            RunSheetText(stringResource(R.string.diet_leave_title), note = stringResource(R.string.run_diet_leave_note))
            Spacer(Modifier.height(20.dp))
            RunButton(stringResource(R.string.run_diet_leave_go), { confirmLeave = false; onBack() },
                modifier = Modifier.testTag("diet-leave-go"))
            Spacer(Modifier.height(10.dp))
            RunButton(stringResource(R.string.run_cancel), { confirmLeave = false }, kind = RunButtonKind.Secondary,
                modifier = Modifier.testTag("diet-leave-stay"))
        }
    }
}

/** U05 · D01 · D02 · D06 — 상태 없이 그리는 입력 화면 */
@Composable
internal fun DietInputContent(
    editing: Boolean,
    height: String,
    weight: String,
    heightError: BodyInputError?,
    weightError: BodyInputError?,
    experience: RunExperience?,
    valid: Boolean,
    keyboardUp: Boolean,
    onHeight: (String) -> Unit,
    onWeight: (String) -> Unit,
    onFocus: (field: Int, focused: Boolean) -> Unit,
    onExperience: (RunExperience) -> Unit,
    onDone: () -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
    val t = runTone()
    val weightFocus = remember { FocusRequester() }
    RunPage(
        onBack = onBack,
        modifier = Modifier.testTag("diet-input"),
        title = stringResource(if (editing) R.string.run_diet_edit_title else R.string.runflow_diet),
        subtitle = stringResource(if (editing) R.string.run_diet_edit_sub else R.string.run_diet_input_sub),
        display = true,
        bottom = {
            if (keyboardUp) {
                // 숫자 자판 위 "완료"(D01) — 자판을 내린다
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    RunTextAction(stringResource(R.string.diet_input_done), onDone, modifier = Modifier.testTag("diet-input-done"))
                }
            } else {
                RunButton(
                    stringResource(if (editing) R.string.diet_edit_apply else R.string.run_diet_see_plan), onSubmit,
                    enabled = valid, chevron = true, modifier = Modifier.testTag("diet-input-next"),
                )
            }
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BodyField(
                label = stringResource(R.string.diet_height), value = height, unit = "cm", decimal = false,
                error = heightError != null, onValue = onHeight, onFocus = { onFocus(1, it) },
                imeAction = ImeAction.Next, onImeAction = { weightFocus.requestFocus() },
                modifier = Modifier.weight(1f), tag = "diet-height",
            )
            BodyField(
                label = stringResource(R.string.diet_weight), value = weight, unit = "kg", decimal = true,
                error = weightError != null, onValue = onWeight, onFocus = { onFocus(2, it) },
                imeAction = ImeAction.Done, onImeAction = onDone,
                modifier = Modifier.weight(1f), tag = "diet-weight", focusRequester = weightFocus,
            )
        }
        val errors = listOfNotNull(
            heightError?.let {
                stringResource(if (it == BodyInputError.EMPTY) R.string.diet_height_empty else R.string.diet_height_range,
                    BodyMath.MIN_HEIGHT, BodyMath.MAX_HEIGHT)
            },
            weightError?.let {
                stringResource(if (it == BodyInputError.EMPTY) R.string.diet_weight_empty else R.string.diet_weight_range,
                    BodyMath.MIN_WEIGHT.toInt(), BodyMath.MAX_WEIGHT.toInt())
            },
        )
        errors.forEach { line ->
            Row(Modifier.padding(top = 8.dp).testTag("diet-input-error"), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.ErrorOutline, null, tint = t.dangerText, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(line, style = runTextStyle(14.sp, t.dangerText, FontWeight.SemiBold))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.run_diet_exp_title), style = runTextStyle(22.sp, t.text, FontWeight.ExtraBold))
        Text(stringResource(R.string.run_diet_exp_sub), style = runTextStyle(15.sp, t.label, FontWeight.Medium),
            modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RunExperience.entries.forEach { option ->
                RunChoiceRow(
                    title = experienceTitle(option),
                    description = stringResource(when (option) {
                        RunExperience.FIRST -> R.string.diet_exp_first_desc
                        RunExperience.SOMETIMES -> R.string.diet_exp_sometimes_desc
                        RunExperience.STEADY -> R.string.diet_exp_steady_desc
                    }),
                    selected = experience == option,
                    onSelect = { onExperience(option) },
                    modifier = Modifier.testTag("diet-exp-${option.key}"),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Info, null, tint = t.label, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.run_diet_body_kept), style = runTextStyle(14.sp, t.label, FontWeight.Medium))
        }
    }
}

/** 키 · 몸무게 칸 — 이름 · 큰 수 · 단위. 누르면 휴대폰 숫자 자판(D01), 잘못되면 빨간 테두리(D02) */
@Composable
private fun BodyField(
    label: String,
    value: String,
    unit: String,
    decimal: Boolean,
    error: Boolean,
    onValue: (String) -> Unit,
    onFocus: (Boolean) -> Unit,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
    modifier: Modifier,
    tag: String,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val t = runTone()
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    val edge = when {
        error -> t.dangerText
        focused -> Color(0xFF4D8BFF)
        else -> t.panelEdge
    }
    val placeholder = stringResource(R.string.run_diet_field_empty)
    Column(
        modifier.clip(shape).background(Brush.verticalGradient(listOf(t.panelTop, t.panel)), shape)
            .border(if (focused || error) 2.dp else 1.dp, edge, shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { focusRequester.requestFocus() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(label, style = runTextStyle(15.sp, t.text, FontWeight.Bold))
        Spacer(Modifier.height(4.dp))
        // 수 · 단위 한 줄 — 단위(cm · kg)는 글 너비 바로 뒤에 붙는다(시안). 비었으면 "입력하기"를 그 자리에
        Row(verticalAlignment = Alignment.Bottom) {
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(placeholder, style = runTextStyle(22.sp, if (error) t.dangerText else t.muted, FontWeight.Bold),
                        maxLines = 1, modifier = Modifier.padding(bottom = 6.dp))
                }
                BasicTextField(
                    value = value,
                    onValueChange = { onValue(DietInput.clean(it, decimal)) },
                    singleLine = true,
                    textStyle = runNumberStyle(54.sp, t.text),
                    cursorBrush = SolidColor(t.cyan),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number, imeAction = imeAction,
                    ),
                    keyboardActions = KeyboardActions(onNext = { onImeAction() }, onDone = { onImeAction() }),
                    modifier = Modifier.width(IntrinsicSize.Min).widthIn(min = 4.dp).focusRequester(focusRequester)
                        .onFocusChanged { focused = it.isFocused; onFocus(it.isFocused) }
                        .semantics {
                            contentDescription = label
                            if (error) error(label)
                        }
                        .testTag(tag),
                )
            }
            Text(" $unit", style = runNumberStyle(20.sp, t.label, FontWeight.Bold), maxLines = 1,
                modifier = Modifier.padding(bottom = 10.dp))
        }
    }
}

/** D04 — 러닝 방법을 준비하고 있어요(몸 정보를 저장하는 동안) */
@Composable
internal fun DietPreparingContent(onBack: () -> Unit) {
    val t = runTone()
    RunPage(
        onBack = onBack,
        modifier = Modifier.testTag("diet-preparing"),
        title = stringResource(R.string.runflow_diet),
        subtitle = stringResource(R.string.run_diet_preparing_sub),
        display = true,
        bottom = { RunButton(stringResource(R.string.run_diet_back_to_input), onBack, modifier = Modifier.testTag("diet-preparing-back")) },
    ) {
        RunCard(padding = androidx.compose.foundation.layout.PaddingValues(vertical = 24.dp, horizontal = 16.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                RunSpinner(Modifier.size(96.dp))
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.run_diet_preparing_title), style = runTextStyle(22.sp, t.text, FontWeight.ExtraBold),
                    textAlign = TextAlign.Center)
                Text(stringResource(R.string.run_diet_preparing_body), style = runTextStyle(15.sp, t.label, FontWeight.Medium),
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.run_diet_upcoming), style = runTextStyle(20.sp, t.text, FontWeight.ExtraBold))
        Text(stringResource(R.string.run_diet_upcoming_sub), style = runTextStyle(15.sp, t.label, FontWeight.Medium))
        Spacer(Modifier.height(12.dp))
        // 아직 정해지지 않은 줄 — 숫자만 두고 내용은 흐린 막대(가짜 글을 적지 않는다)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            (1..3).forEach { n ->
                RunCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StepBadge(n)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.fillMaxWidth(0.8f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(t.track))
                            Box(Modifier.fillMaxWidth(0.5f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(t.track))
                        }
                    }
                }
            }
        }
    }
}

/** D05 — 러닝 방법을 불러오지 못했어요. 입력한 내용은 그대로 */
@Composable
internal fun DietFailedContent(onRetry: () -> Unit, onCheck: () -> Unit) {
    val t = runTone()
    RunPage(
        onBack = onCheck,
        modifier = Modifier.testTag("diet-failed"),
        title = stringResource(R.string.runflow_diet),
        subtitle = stringResource(R.string.run_diet_failed_sub),
        display = true,
        bottom = {
            RunButton(stringResource(R.string.run_diet_retry), onRetry, modifier = Modifier.testTag("diet-failed-retry"))
            RunButton(stringResource(R.string.run_diet_check_body), onCheck, kind = RunButtonKind.Secondary,
                modifier = Modifier.testTag("diet-failed-check"))
        },
    ) {
        RunCard(padding = androidx.compose.foundation.layout.PaddingValues(vertical = 36.dp, horizontal = 16.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                RunStateArt(Icons.Outlined.ErrorOutline, size = 104.dp, alert = true)
                Spacer(Modifier.height(18.dp))
                Text(stringResource(R.string.run_diet_failed_title), style = runTextStyle(24.sp, t.text, FontWeight.ExtraBold),
                    textAlign = TextAlign.Center)
                Text(stringResource(R.string.run_diet_failed_body), style = runTextStyle(15.sp, t.label, FontWeight.Medium),
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
internal fun experienceTitle(experience: RunExperience): String = stringResource(when (experience) {
    RunExperience.FIRST -> R.string.diet_exp_first
    RunExperience.SOMETIMES -> R.string.diet_exp_sometimes
    RunExperience.STEADY -> R.string.diet_exp_steady
})

/** 분 단위 — "3분" */
@Composable
internal fun minutesText(sec: Long): String = stringResource(R.string.diet_minutes, (sec / 60).toInt())

/** 러닝 방법 추천(시안 U06) — 고른 경험의 고정 루틴을 보여 주고 시작한다 */
@Composable
fun DietPlanScreen(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onStart: (RunExperience) -> Unit,
) {
    val saved by ServiceLocator.userPrefs.runExperience.collectAsState(initial = null)
    val experience = saved ?: RunExperience.FIRST
    DietPlanContent(experience, onBack, onEdit, onStart = { onStart(experience) })
}

@Composable
internal fun DietPlanContent(
    experience: RunExperience,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onStart: () -> Unit,
) {
    val t = runTone()
    val routine = DietRoutine.forExperience(experience)
    RunPage(
        onBack = onBack,
        modifier = Modifier.testTag("diet-plan"),
        title = stringResource(R.string.run_diet_plan_title),
        subtitle = stringResource(when (experience) {
            RunExperience.FIRST -> R.string.run_diet_plan_sub_first
            RunExperience.SOMETIMES -> R.string.run_diet_plan_sub_sometimes
            RunExperience.STEADY -> R.string.run_diet_plan_sub_steady
        }),
        display = true,
        bottom = {
            RunButton(stringResource(R.string.diet_plan_start), onStart, modifier = Modifier.testTag("diet-plan-start"))
            RunButton(stringResource(R.string.diet_plan_edit), onEdit, kind = RunButtonKind.Secondary,
                modifier = Modifier.testTag("diet-plan-edit"))
        },
    ) {
        RunCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 16.dp), tag = "diet-plan-total") {
            Text(stringResource(R.string.diet_total_time), style = runTextStyle(15.sp, t.label, FontWeight.SemiBold))
            RunNumber(stringResource(R.string.run_diet_min_big, (routine.totalSec / 60).toInt()), size = 72.sp,
                align = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(), valueTag = "diet-plan-minutes")
            Spacer(Modifier.height(10.dp))
            RoutineBar(routine)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TotalLine(stringResource(R.string.run_diet_kind_run), minutesText(routine.totalRunSec), Modifier.weight(1f))
                Box(Modifier.width(1.dp).height(24.dp).background(t.divider))
                TotalLine(stringResource(R.string.run_diet_kind_walk), minutesText(routine.totalWalkSec), Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.run_diet_parts), style = runTextStyle(20.sp, t.text, FontWeight.ExtraBold))
        Text(stringResource(R.string.run_diet_parts_sub), style = runTextStyle(15.sp, t.label, FontWeight.Medium))
        Spacer(Modifier.height(12.dp))
        StepRow(1, stringResource(R.string.run_diet_step_warmup), stringResource(R.string.run_diet_step_warmup_desc),
            stringResource(R.string.run_diet_kind_walk) + " " + minutesText(routine.warmupSec))
        StepArrow()
        StepRow(
            2, stringResource(R.string.run_diet_step_interval),
            stringResource(R.string.run_diet_step_interval_desc, minutesText(routine.runSec), minutesText(routine.walkSec), routine.rounds),
            value = stringResource(R.string.run_diet_kind_run) + " " + minutesText(routine.runSec) + "\n" +
                stringResource(R.string.run_diet_kind_walk) + " " + minutesText(routine.walkSec),
            times = stringResource(R.string.run_diet_times, routine.rounds),
        )
        StepArrow()
        StepRow(3, stringResource(R.string.run_diet_step_cooldown), stringResource(R.string.run_diet_step_cooldown_desc),
            stringResource(R.string.run_diet_kind_walk) + " " + minutesText(routine.cooldownSec))
        Spacer(Modifier.height(16.dp))
        // 개인 처방이 아니라는 안내는 남긴다(시안에는 없지만 지우지 않는다)
        Text(stringResource(R.string.diet_plan_disclaimer), style = runTextStyle(12.sp, t.muted), modifier = Modifier.testTag("diet-plan-note"))
    }
}

/** 루틴 막대 — 준비 · 마무리 걷기(시안) · 러닝(파랑) · 걷기(밝은 파랑)를 시간 비율대로 */
@Composable
private fun RoutineBar(routine: DietRoutine) {
    val t = runTone()
    val parts = buildList {
        add(routine.warmupSec to t.cyan)
        repeat(routine.rounds) {
            add(routine.runSec to t.cobalt)
            add(routine.walkSec to Color(0xFF2E6BFF))
        }
        add(routine.cooldownSec to t.cyan)
    }
    val total = parts.sumOf { it.first }.toFloat()
    Canvas(Modifier.fillMaxWidth().height(16.dp)) {
        val gap = 2.dp.toPx()
        val usable = size.width - gap * (parts.size - 1)
        var x = 0f
        parts.forEachIndexed { i, (sec, color) ->
            val w = usable * sec / total
            drawRoundRect(color, Offset(x, 0f), Size(w, size.height), CornerRadius(if (i == 0 || i == parts.lastIndex) size.height / 2 else 2.dp.toPx()))
            x += w + gap
        }
    }
}

@Composable
private fun TotalLine(label: String, value: String, modifier: Modifier) {
    val t = runTone()
    Row(modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = runTextStyle(15.sp, t.text, FontWeight.SemiBold))
        Spacer(Modifier.width(8.dp))
        Text(value, style = runNumberStyle(22.sp, t.cyan))
    }
}

@Composable
private fun StepBadge(n: Int) {
    val t = runTone()
    Box(
        Modifier.size(34.dp).clip(CircleShape).background(t.inset).border(1.5.dp, t.cobaltText, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Text("$n", style = runTextStyle(16.sp, t.text, FontWeight.ExtraBold, 1.0f)) }
}

@Composable
private fun StepRow(n: Int, title: String, description: String, value: String, times: String? = null) {
    val t = runTone()
    RunCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp), tag = "diet-step-$n") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepBadge(n)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = runTextStyle(16.sp, t.text, FontWeight.Bold))
                Text(description, style = runTextStyle(13.sp, t.label))
            }
            Spacer(Modifier.width(8.dp))
            Text(value, style = runTextStyle(14.sp, t.cyan, FontWeight.Bold), textAlign = TextAlign.End)
            if (times != null) {
                Spacer(Modifier.width(10.dp))
                Text(times, style = runNumberStyle(22.sp, t.cyan))
            }
        }
    }
}

@Composable
private fun StepArrow() {
    val t = runTone()
    Box(Modifier.fillMaxWidth().heightIn(min = 20.dp), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.KeyboardArrowDown, null, tint = t.cobaltText, modifier = Modifier.size(20.dp))
    }
}
