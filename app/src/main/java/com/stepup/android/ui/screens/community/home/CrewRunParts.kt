package com.stepup.android.ui.screens.community.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.CrewRunMath
import com.stepup.android.domain.CrewRunRules
import com.stepup.android.domain.CrewRunToday
import com.stepup.android.ui.screens.community.chat.BlueBar
import com.stepup.android.ui.screens.community.chat.BlueSheet
import com.stepup.android.ui.screens.community.chat.BlueSheetBody
import com.stepup.android.ui.screens.community.chat.blueInk
import com.stepup.android.ui.screens.community.chat.blueText

/*
 * 크루 러닝 3판(서버 0070, 2026-10-21) — 크루 홈의 "오늘 크루 러닝" 칸과 규칙 시트. 값은 서버(crew_run_today)가 준 그대로다.
 * 대기실(CrewLobby)도 같은 규칙 줄 · 인원 보너스 표를 쓴다([crewRunRuleLines] · [crewHeadcountRows]).
 */

/** 규칙 줄 — 체력 · 다 쓰면 · 내 에너지 · 보상 · 누가 · 하루 한 번 · 혼자 · 방 인원 */
@Composable
internal fun crewRunRuleLines(rules: CrewRunRules): List<String> = listOf(
    stringResource(R.string.crew_run_rule_pool, CrewRunMath.amount(rules.drainPerMin)),
    stringResource(R.string.crew_run_rule_empty),
    stringResource(R.string.crew_run_rule_own),
    stringResource(R.string.crew_run_rule_reward),
    stringResource(R.string.crew_run_rule_who, rules.confirmSec / 60, rules.nearM),
    stringResource(R.string.crew_run_rule_once),
    stringResource(R.string.crew_run_rule_alone),
    stringResource(R.string.crew_run_rule_room, rules.roomMax),
)

/**
 * 인원 보너스 표 — 2 ~ 6명은 한 줄씩, 그다음 한 명마다 같은 폭으로 오르는 구간은 한 줄로 묶고, 마지막(20명)은 따로.
 * 서버가 준 표 그대로 읽는다.
 */
@Composable
internal fun crewHeadcountRows(rules: CrewRunRules): List<Pair<String, String>> {
    val steps = rules.headcount.filter { it.n >= 2 }.sortedBy { it.n }
    val rows = mutableListOf<Pair<String, String>>()
    var i = 0
    while (i < steps.size) {
        // 이 줄부터 한 명마다 같은 폭으로 오르는 구간을 찾는다(마지막 줄은 늘 따로)
        var j = i
        val step = steps.getOrNull(i + 1)?.let { it.bonus - steps[i].bonus }
        if (step != null && step > 0) {
            while (j + 1 < steps.size - 1 && kotlin.math.abs((steps[j + 1].bonus - steps[j].bonus) - step) < 1e-9 &&
                steps[j + 1].n == steps[j].n + 1
            ) j++
        }
        if (j - i >= 2) {
            rows += stringResource(R.string.crew_run_headcount_range, steps[i].n, steps[j].n) to stringResource(
                R.string.crew_run_bonus_range_value,
                CrewRunMath.percent(steps[i].bonus), CrewRunMath.percent(steps[j].bonus), CrewRunMath.percent(step ?: 0.0),
            )
            i = j + 1
        } else {
            rows += stringResource(R.string.crew_run_headcount_n, steps[i].n) to
                stringResource(R.string.crew_run_bonus_value, CrewRunMath.percent(steps[i].bonus))
            i++
        }
    }
    return rows
}

/** 크루 홈 — 오늘 크루 러닝: 할 수 있음 · 마침(함께한 인원 · 쓴 체력), 다시 열리는 때, 오늘 달린 사람, 레벨 보너스. 누르면 규칙 */
@Composable
internal fun CrewRunTodayBlock(today: CrewRunToday, onClick: () -> Unit) {
    val ink = blueInk()
    Column(Modifier.fillMaxWidth().homeClickable(onClick = onClick).padding(vertical = 22.dp).testTag("home-crew-run")) {
        Text(stringResource(R.string.crew_run_title), style = blueText(18.sp, ink.text, FontWeight.Bold))
        Spacer(Modifier.height(10.dp))
        val mine = today.mine
        if (today.ranToday) {
            Text(
                stringResource(R.string.crew_run_done), style = blueText(24.sp, ink.text, FontWeight.ExtraBold, 1.25f),
                modifier = Modifier.testTag("home-crew-run-done"),
            )
            if (mine != null) {
                Spacer(Modifier.height(12.dp))
                BlueBar(if (mine.share > 0) (mine.used / mine.share).toFloat().coerceIn(0f, 1f) else 0f, Modifier.fillMaxWidth().height(10.dp))
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.crew_run_done_detail, mine.runners, CrewRunMath.amount(mine.used), CrewRunMath.amount(mine.share)),
                    style = blueText(15.sp, ink.text, FontWeight.SemiBold, 1.45f),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.crew_run_reset), style = blueText(15.sp, ink.secondary, FontWeight.Medium, 1.45f))
        } else {
            Text(
                stringResource(R.string.crew_run_can), style = blueText(24.sp, ink.info, FontWeight.ExtraBold, 1.25f),
                modifier = Modifier.testTag("home-crew-run-can"),
            )
        }
        if (today.todayRunners > 0) {
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.crew_run_today_runners, today.todayRunners), style = blueText(15.sp, ink.secondary, FontWeight.Medium, 1.45f))
        }
        today.crewLevel?.let { level ->
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.crew_run_level_bonus, level, CrewRunMath.percent(today.staminaBonus), CrewRunMath.percent(today.rewardBonus)),
                style = blueText(15.sp, ink.info, FontWeight.SemiBold, 1.45f), modifier = Modifier.testTag("home-crew-run-level"),
            )
        }
    }
}

/** 크루 러닝 규칙 — 규칙 줄, 인원 보너스 표, 크루 레벨로 */
@Composable
internal fun CrewRunRulesSheet(rules: CrewRunRules, onDismiss: () -> Unit, onLevel: () -> Unit) {
    val ink = blueInk()
    BlueSheet(stringResource(R.string.crew_run_sheet_title), onDismiss, Modifier.testTag("home-crew-run-sheet")) {
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()).padding(horizontal = 4.dp)) {
            Spacer(Modifier.height(12.dp))
            crewRunRuleLines(rules).forEach { line ->
                BlueSheetBody(line, centered = false)
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(12.dp))
            HomeLabel(stringResource(R.string.crew_run_headcount_title))
            Spacer(Modifier.height(4.dp))
            crewHeadcountRows(rules).forEach { (label, value) ->
                HomeValueRow(label = label, value = value, modifier = Modifier.testTag("home-crew-run-headcount"))
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).homeClickable(onClick = onLevel).testTag("home-crew-run-level-link"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.crew_run_level_link), style = blueText(16.sp, ink.info, FontWeight.Bold))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
