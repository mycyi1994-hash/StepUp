package com.stepup.android.ui.screens.community.crew

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.domain.CrewGoalProgress
import com.stepup.android.domain.CrewRole
import com.stepup.android.domain.CrewRules

/**
 * 13 주간 공동 목표 · 83 목표 변경 후 현황 — 이번 주(한국 월요일부터 일요일)의 크루 러닝 거리와 목표.
 * 100% 를 넘으면 숫자는 그대로 보이고 바는 100% 까지만 채운다. 목표가 없으면 나누지 않는다.
 */
@Composable
fun CrewGoalScreen(viewModel: CrewScreenViewModel, onBack: () -> Unit, onParticipants: () -> Unit, onSetGoal: () -> Unit) {
    val ink = crewInk()
    val card by viewModel.card.collectAsStateWithLifecycle()
    CrewPage(Modifier.testTag("crew-goal")) {
        CrewTopBar(stringResource(R.string.crew_goal_title), onBack)
        val crew = card ?: return@CrewPage
        val progress = CrewRules.progress(crew)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = CrewGutter)) {
            Spacer(Modifier.height(18.dp))
            CrewIdentityStrip(crew)
            Spacer(Modifier.height(34.dp))
            if (progress == null) {
                Text(stringResource(R.string.crew_goal_none_title), color = ink.text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.crew_goal_none_body, CrewRules.km(crew.weekKm)),
                    color = ink.secondary, fontSize = 14.sp, lineHeight = 25.sp, modifier = Modifier.testTag("crew-goal-none"),
                )
                if (crew.role == CrewRole.OWNER) {
                    Spacer(Modifier.height(30.dp))
                    CrewButton(stringResource(R.string.crew_goal_set), onSetGoal, Modifier.testTag("crew-goal-set"))
                }
            } else {
                CrewGoalBody(progress, crew.memberCount, crew.weekRunners)
                Spacer(Modifier.height(40.dp))
                CrewRow(
                    stringResource(R.string.crew_goal_participants), onParticipants, Modifier.testTag("crew-goal-participants"),
                    value = stringResource(R.string.crew_members_only, crew.weekRunners),
                )
            }
            Spacer(Modifier.height(56.dp))
            Text(stringResource(R.string.crew_goal_gathered), color = ink.secondary, fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun CrewGoalBody(progress: CrewGoalProgress, members: Int, runners: Int) {
    val ink = crewInk()
    Text(stringResource(R.string.crew_goal_run_together, progress.goalKm), color = ink.text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("crew-goal-heading"))
    Spacer(Modifier.height(12.dp))
    Text(stringResource(R.string.crew_goal_week_span), color = ink.secondary, fontSize = 13.5.sp)
    Spacer(Modifier.height(34.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(CrewRules.km(progress.doneKm), color = ink.text, fontSize = 60.sp, lineHeight = 62.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("crew-goal-done"))
        Text(
            stringResource(R.string.crew_goal_of, progress.goalKm), color = ink.secondary, fontSize = 24.sp,
            modifier = Modifier.padding(start = 14.dp, bottom = 8.dp).weight(1f),
        )
        Text(
            stringResource(R.string.crew_percent, progress.percent), color = ink.info, fontSize = 40.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End, modifier = Modifier.padding(bottom = 2.dp).testTag("crew-goal-percent"),
        )
    }
    Spacer(Modifier.height(24.dp))
    CrewBar(progress.fraction, Modifier.fillMaxWidth().height(10.dp))
    Spacer(Modifier.height(30.dp))
    Text(
        if (progress.reached) stringResource(R.string.crew_goal_reached)
        else stringResource(R.string.crew_goal_remaining, CrewRules.km(progress.remainingKm)),
        color = ink.secondary, fontSize = 14.sp, lineHeight = 25.sp, modifier = Modifier.testTag("crew-goal-remaining"),
    )
    Text(stringResource(R.string.crew_goal_joined, members, runners), color = ink.secondary, fontSize = 14.sp, lineHeight = 25.sp)
}

/**
 * 46 주간 목표 수정 · 47 변경 확인 — 누적 거리는 그대로 두고 목표만 바꾼다(이미 달린 거리는 러닝 기록에서 센다).
 * 적용하면 83(바뀐 달성률)로 간다.
 */
@Composable
fun CrewGoalEditScreen(viewModel: CrewScreenViewModel, onBack: () -> Unit, onApplied: () -> Unit) {
    val ink = crewInk()
    val card by viewModel.card.collectAsStateWithLifecycle()
    val op by viewModel.op.collectAsStateWithLifecycle()
    var text by rememberSaveable { mutableStateOf(card?.goalKm?.toString().orEmpty()) }
    var confirm by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(card?.goalKm) { if (text.isEmpty()) card?.goalKm?.let { text = it.toString() } }
    LaunchedEffect(op) {
        if (op.op == CrewOp.GOAL && op.done) {
            viewModel.consumeOp()
            confirm = false
            onApplied()
        }
    }
    val value = CrewRules.goal(text)
    CrewPage(Modifier.imePadding().testTag("crew-goal-edit")) {
        CrewTopBar(stringResource(R.string.crew_goal_edit_title), onBack)
        val crew = card ?: return@CrewPage
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = CrewGutter)) {
            Spacer(Modifier.height(18.dp))
            CrewIdentityStrip(crew)
            Spacer(Modifier.height(36.dp))
            Text(stringResource(R.string.crew_goal_edit_heading), color = ink.text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.crew_goal_edit_sub), color = ink.secondary, fontSize = 13.5.sp)
            Spacer(Modifier.height(26.dp))
            CrewChoiceRow {
                CrewRules.EDIT_GOALS.forEach { km ->
                    CrewChoice(stringResource(R.string.crew_km_value, km.toString()), value == km, { text = km.toString() }, Modifier.weight(1f).testTag("crew-goal-chip-$km"))
                }
            }
            Spacer(Modifier.height(30.dp))
            CrewFieldLabel(stringResource(R.string.crew_goal_field))
            Spacer(Modifier.height(12.dp))
            CrewTextField(
                text, { next -> text = next.filter(Char::isDigit).take(5) }, Modifier.testTag("crew-goal-input"),
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Spacer(Modifier.height(10.dp))
            CrewHelp(
                if (text.isNotEmpty() && value == null) stringResource(R.string.crew_goal_invalid)
                else stringResource(R.string.crew_goal_field_help, CrewRules.km(crew.weekKm)),
                error = text.isNotEmpty() && value == null,
            )
            Spacer(Modifier.height(46.dp))
            Text(stringResource(R.string.crew_goal_edit_note), color = ink.secondary, fontSize = 14.sp, lineHeight = 25.sp)
            Spacer(Modifier.height(24.dp))
        }
        CrewBottomBar {
            CrewButton(
                stringResource(R.string.crew_goal_change), { confirm = true }, Modifier.testTag("crew-goal-change"),
                enabled = value != null && value != crew.goalKm,
            )
        }
    }
    if (confirm && value != null) {
        val crew = card
        CrewConfirmSheet(
            title = stringResource(R.string.crew_goal_confirm_title, value),
            body = stringResource(R.string.crew_goal_confirm_body, CrewRules.km(crew?.weekKm ?: 0.0)),
            confirm = stringResource(R.string.crew_goal_apply),
            busy = op.op == CrewOp.GOAL && op.running,
            error = if (op.op == CrewOp.GOAL && op.problem != null) crewProblemText(op.problem!!) else null,
            tag = "crew-goal-confirm",
            onConfirm = { viewModel.setGoal(value) },
            onDismiss = { if (!op.running) { confirm = false; if (op.op == CrewOp.GOAL) viewModel.consumeOp() } },
        )
    }
}

/** 가운데 정렬 설명(작은 화면용) */
@Composable
internal fun CrewCenteredNote(text: String) {
    val ink = crewInk()
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(text, color = ink.secondary, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}
