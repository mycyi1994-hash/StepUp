package com.stepup.android.ui.screens.walk

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.BodyMath
import com.stepup.android.domain.DietRoutine
import com.stepup.android.domain.RunExperience
import com.stepup.android.ui.components.KitButton
import com.stepup.android.ui.components.KitChoice
import com.stepup.android.ui.components.KitDialog
import com.stepup.android.ui.components.KitGap
import com.stepup.android.ui.components.KitKeypad
import com.stepup.android.ui.components.KitMetricRow
import com.stepup.android.ui.components.KitNotice
import com.stepup.android.ui.components.KitNumberField
import com.stepup.android.ui.components.KitScreen
import com.stepup.android.ui.components.KitTone
import com.stepup.android.ui.components.RunKit
import com.stepup.android.ui.theme.Alert
import com.stepup.android.ui.theme.Carbon
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.VoltText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

    fun format(kg: Double): String = if (kg % 1.0 == 0.0) kg.toLong().toString() else "%.1f".format(java.util.Locale.ROOT, kg)
}

/**
 * 다이어트 모드 입력(시안 U05 · D01 · D02 · D03) — 키 · 몸무게 · 러닝 경험.
 * [editing] 이면 몸 정보 · 경험 수정(D06): "수정하고 다시 추천" · "취소".
 * 키 · 몸무게는 기록용(이 폰에만), 러닝 경험은 루틴을 고르는 기준이다.
 */
@Composable
fun DietInputScreen(
    onBack: () -> Unit,
    onNext: (RunExperience) -> Unit,
    editing: Boolean = false,
) {
    val prefs = ServiceLocator.userPrefs
    val scope = rememberCoroutineScope()
    var height by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var experienceKey by rememberSaveable { mutableStateOf<String?>(null) }
    var focused by rememberSaveable { mutableIntStateOf(0) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var dirty by rememberSaveable { mutableStateOf(false) }
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    var loaded by rememberSaveable { mutableStateOf(false) }
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
    val heightError = DietInput.heightError(height).takeIf { showErrors }
    val weightError = DietInput.weightError(weight).takeIf { showErrors }
    val experienceMissing = showErrors && experience == null
    val leave = { if (dirty) confirmLeave = true else onBack() }
    BackHandler(enabled = focused != 0 || dirty) { if (focused != 0) focused = 0 else confirmLeave = true }

    KitScreen(
        title = stringResource(if (editing) R.string.diet_edit_title else R.string.runflow_diet),
        onBack = leave,
        headline = stringResource(when {
            editing -> R.string.diet_edit_headline
            focused != 0 -> R.string.diet_input_numbers_headline
            showErrors && (heightError != null || weightError != null || experienceMissing) -> R.string.diet_input_check_headline
            else -> R.string.diet_input_headline
        }),
        subtitle = stringResource(when {
            editing -> R.string.diet_edit_sub
            focused != 0 -> R.string.diet_input_numbers_sub
            else -> R.string.diet_input_sub
        }),
        modifier = Modifier.testTag("diet-input"),
        bottom = {
            if (focused != 0) {
                KitButton(stringResource(R.string.diet_input_done), { focused = 0 }, modifier = Modifier.testTag("diet-input-done"))
            } else {
                KitButton(
                    stringResource(if (editing) R.string.diet_edit_apply else R.string.diet_input_next),
                    onClick = {
                        showErrors = true
                        val cm = height.toIntOrNull()
                        val kg = weight.toDoubleOrNull()
                        val chosen = experience
                        if (DietInput.heightError(height) == null && DietInput.weightError(weight) == null &&
                            cm != null && kg != null && chosen != null) {
                            scope.launch {
                                val body = prefs.bodyProfile.first()
                                prefs.setBodyProfile(body.copy(heightCm = cm, weightKg = BodyMath.round1(kg)))
                                prefs.setRunExperience(chosen)
                                dirty = false
                                onNext(chosen)
                            }
                        }
                    },
                    modifier = Modifier.testTag("diet-input-next"),
                )
                if (editing) {
                    KitButton(stringResource(R.string.common_cancel), leave, tone = KitTone.Ghost)
                } else {
                    Text(stringResource(R.string.diet_input_later), color = Silver, fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        },
        footer = if (focused != 0) {
            {
                KitKeypad(
                    onKey = { key ->
                        dirty = true
                        if (focused == 1) height = DietInput.type(height, key, decimal = false)
                        else weight = DietInput.type(weight, key, decimal = true)
                    },
                    onDelete = {
                        dirty = true
                        if (focused == 1) height = height.dropLast(1) else weight = weight.dropLast(1)
                    },
                    deleteLabel = stringResource(R.string.diet_input_delete),
                )
            }
        } else null,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(KitGap)) {
            KitNumberField(
                stringResource(R.string.diet_height), height, "cm", focused = focused == 1,
                onFocus = { focused = 1 },
                error = heightError?.let {
                    stringResource(if (it == BodyInputError.EMPTY) R.string.diet_height_empty else R.string.diet_height_range,
                        BodyMath.MIN_HEIGHT, BodyMath.MAX_HEIGHT)
                },
                modifier = Modifier.weight(1f).testTag("diet-height"),
            )
            KitNumberField(
                stringResource(R.string.diet_weight), weight, "kg", focused = focused == 2,
                onFocus = { focused = 2 },
                error = weightError?.let {
                    stringResource(if (it == BodyInputError.EMPTY) R.string.diet_weight_empty else R.string.diet_weight_range,
                        BodyMath.MIN_WEIGHT.toInt(), BodyMath.MAX_WEIGHT.toInt())
                },
                modifier = Modifier.weight(1f).testTag("diet-weight"),
            )
        }
        Spacer(Modifier.height(12.dp))
        if (focused != 0) {
            KitNotice(stringResource(R.string.diet_body_note_title), stringResource(R.string.diet_body_note))
        } else {
            Text(stringResource(R.string.diet_body_note), color = Silver, fontSize = 14.sp)
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.diet_experience), color = Snow, fontSize = 20.sp)
            if (experienceMissing) {
                Text(stringResource(R.string.diet_experience_missing), color = Alert, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(KitGap)) {
                RunExperience.entries.forEach { option ->
                    KitChoice(
                        title = experienceTitle(option),
                        description = stringResource(when (option) {
                            RunExperience.FIRST -> R.string.diet_exp_first_desc
                            RunExperience.SOMETIMES -> R.string.diet_exp_sometimes_desc
                            RunExperience.STEADY -> R.string.diet_exp_steady_desc
                        }),
                        selected = experience == option,
                        onSelect = { experienceKey = option.key; dirty = true },
                        modifier = Modifier.testTag("diet-exp-${option.key}"),
                    )
                }
            }
        }
    }

    // D03 — 입력을 그만할까요? 바깥을 누르면 계속 입력
    if (confirmLeave) {
        KitDialog(
            title = stringResource(R.string.diet_leave_title),
            body = stringResource(R.string.diet_leave_body),
            onDismiss = { confirmLeave = false },
            modifier = Modifier.testTag("diet-leave-dialog"),
        ) {
            KitButton(stringResource(R.string.diet_leave_stay), { confirmLeave = false })
            KitButton(stringResource(R.string.diet_leave_go), { confirmLeave = false; onBack() }, tone = KitTone.Secondary)
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

/** 러닝 방법(시안 U06) — 고른 경험의 고정 루틴을 보여 주고 시작한다 */
@Composable
fun DietPlanScreen(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onStart: (RunExperience) -> Unit,
) {
    val saved by ServiceLocator.userPrefs.runExperience.collectAsState(initial = null)
    val experience = saved ?: RunExperience.FIRST
    val routine = DietRoutine.forExperience(experience)
    KitScreen(
        title = stringResource(R.string.runflow_diet),
        onBack = onBack,
        headline = stringResource(when (experience) {
            RunExperience.FIRST -> R.string.diet_plan_first_headline
            RunExperience.SOMETIMES -> R.string.diet_plan_sometimes_headline
            RunExperience.STEADY -> R.string.diet_plan_steady_headline
        }),
        subtitle = stringResource(R.string.diet_plan_sub, experienceTitle(experience), (routine.totalSec / 60).toInt()),
        modifier = Modifier.testTag("diet-plan"),
        bottom = {
            KitButton(stringResource(R.string.diet_plan_edit), onEdit, tone = KitTone.Ghost,
                modifier = Modifier.testTag("diet-plan-edit"))
            KitButton(stringResource(R.string.diet_plan_start), { onStart(experience) },
                modifier = Modifier.testTag("diet-plan-start"))
        },
    ) {
        KitNotice(stringResource(R.string.diet_plan_note_title), stringResource(R.string.diet_plan_note_body))
        Spacer(Modifier.height(KitGap))
        val shape = RoundedCornerShape(RunKit.CardRadius)
        Column(
            Modifier.fillMaxWidth().clip(shape).background(Carbon, shape).padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            PlanRow(stringResource(R.string.diet_seg_warmup), minutesText(routine.warmupSec))
            PlanRow(
                stringResource(R.string.diet_plan_interval, minutesText(routine.runSec), minutesText(routine.walkSec)),
                stringResource(R.string.diet_rounds, routine.rounds),
            )
            PlanRow(stringResource(R.string.diet_seg_cooldown), minutesText(routine.cooldownSec))
        }
        Spacer(Modifier.height(24.dp))
        KitMetricRow(
            stringResource(R.string.diet_total_run) to minutesText(routine.totalRunSec),
            stringResource(R.string.diet_total_walk) to minutesText(routine.totalWalkSec),
        )
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.diet_plan_disclaimer), color = Silver, fontSize = 12.sp)
    }
}

@Composable
private fun PlanRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(label, color = Snow, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(value, color = VoltText, fontSize = 16.sp)
    }
}
