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
import com.stepup.android.ui.screens.community.chat.BlueSheet
import com.stepup.android.ui.screens.community.chat.BlueSheetBody
import com.stepup.android.ui.screens.community.chat.blueInk
import com.stepup.android.ui.screens.community.chat.blueText
import com.stepup.android.ui.screens.walk.CrewNavy
import com.stepup.android.ui.screens.walk.NavyBar
import com.stepup.android.ui.screens.walk.NavyCard
import com.stepup.android.ui.screens.walk.NavyChip
import com.stepup.android.ui.screens.walk.NavyNote
import com.stepup.android.ui.screens.walk.NavyPrimaryButton
import com.stepup.android.ui.screens.walk.NavySheet
import com.stepup.android.ui.screens.walk.navyText

/*
 * 크루 러닝(서버 0071, 2026-10-22 핸드오프) — 크루 홈의 "오늘 크루 체력" 칸 · 레벨 업 시트 · 규칙 시트. 값은 서버(crew_run_today)가 준 그대로다.
 * 대기실(CrewLobby)도 같은 규칙 줄 · 인원 보너스 표를 쓴다([crewRunRuleLines] · [crewHeadcountRows]).
 */

/** 규칙 줄(제목 · 설명) — 신발 각자 · 분당 소모 · 0 이면 적립만 멈춤 · 하루 한 번 · 500m 2분 확인 · 자정 초기화 · 보상 · 방 인원 */
@Composable
internal fun crewRunRuleLines(rules: CrewRunRules): List<Pair<String, String>> = listOf(
    stringResource(R.string.crew_run_rule_shoes_t) to stringResource(R.string.crew_run_rule_shoes_b),
    stringResource(R.string.crew_run_rule_drain_t, CrewRunMath.amount(rules.drainPerMin)) to stringResource(R.string.crew_run_rule_drain_b),
    stringResource(R.string.crew_run_rule_empty_t) to stringResource(R.string.crew_run_rule_empty_b),
    stringResource(R.string.crew_run_rule_once_t) to stringResource(R.string.crew_run_rule_once_b),
    stringResource(R.string.crew_run_rule_near_t) to
        stringResource(R.string.crew_run_rule_near_b),
    stringResource(R.string.crew_run_rule_reset_t) to stringResource(R.string.crew_run_rule_reset_b),
    stringResource(R.string.crew_run_rule_reward_t) to stringResource(R.string.crew_run_rule_reward_b),
    stringResource(R.string.crew_run_rule_room, rules.roomMax) to "",
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

/**
 * 크루 홈 — 오늘 크루 체력(서버 0071): 남은 양 / 총량(기여 합) · 막대 · "함께 쓰는 내구도 · 매일 00:00 초기화" · 오늘 참가 인원 ·
 * 내 오늘 크루 러닝(할 수 있음 · 달리는 중 · 마침 · 다른 크루에서 마침) · 인증 · 레벨 보너스. 누르면 규칙.
 * 0/0(새 하루)과 0/816(다 씀)은 다르게 적는다 — 어느 쪽도 출발을 막지 않는다. 남색 카드는 핸드오프의 크루 러닝 색이다.
 */
@Composable
internal fun CrewRunTodayBlock(today: CrewRunToday, onClick: () -> Unit) {
    val pool = today.pool
    val mine = today.mine
    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp).testTag("home-crew-run")) {
        NavyCard(onClick = onClick, strong = true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.crew_run_pool_title), style = navyText(17.sp, CrewNavy.Sub, FontWeight.Bold),
                    modifier = Modifier.weight(1f))
                today.crewLevel?.let { NavyChip("Lv.$it", Modifier.testTag("home-crew-run-level-chip")) }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.crew_run_pool_value, CrewRunMath.amount(pool.remaining), CrewRunMath.amount(pool.total)),
                style = navyText(34.sp, CrewNavy.Text, FontWeight.ExtraBold, 1.15f), modifier = Modifier.testTag("home-crew-run-pool"),
            )
            Spacer(Modifier.height(10.dp))
            NavyBar(CrewRunMath.fraction(pool.remaining, pool.total), warn = pool.low || (pool.exhausted && !pool.fresh))
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.crew_run_pool_sub), style = navyText(14.sp, CrewNavy.Sub, FontWeight.Medium))
            when {
                pool.fresh -> Text(stringResource(R.string.crew_run_pool_fresh), style = navyText(14.sp, CrewNavy.Cyan, FontWeight.Medium, 1.45f),
                    modifier = Modifier.padding(top = 6.dp).testTag("home-crew-run-fresh"))
                pool.exhausted || pool.remaining <= 0.0 -> Text(stringResource(R.string.crew_run_pool_used_up),
                    style = navyText(14.sp, CrewNavy.Warn, FontWeight.Medium, 1.45f), modifier = Modifier.padding(top = 6.dp).testTag("home-crew-run-empty"))
            }
            if (pool.contributors > 0) {
                Text(stringResource(R.string.crew_run_today_contributors, pool.contributors), style = navyText(15.sp, CrewNavy.Text, FontWeight.SemiBold),
                    modifier = Modifier.padding(top = 6.dp).testTag("home-crew-run-contributors"))
            }
            Spacer(Modifier.height(12.dp))
            when {
                mine.active -> Text(stringResource(R.string.crew_run_running_now), style = navyText(18.sp, CrewNavy.Cyan, FontWeight.ExtraBold),
                    modifier = Modifier.testTag("home-crew-run-active"))
                today.usedElsewhere -> {
                    Text(stringResource(R.string.crew_run_done_elsewhere, mine.crewName.orEmpty()), style = navyText(18.sp, CrewNavy.Text, FontWeight.ExtraBold, 1.3f),
                        modifier = Modifier.testTag("home-crew-run-done"))
                    Text(stringResource(R.string.crew_run_reset), style = navyText(14.sp, CrewNavy.Sub, FontWeight.Medium, 1.45f))
                }
                mine.usedToday -> {
                    Text(stringResource(R.string.crew_run_done), style = navyText(18.sp, CrewNavy.Text, FontWeight.ExtraBold, 1.3f),
                        modifier = Modifier.testTag("home-crew-run-done"))
                    Text(stringResource(R.string.crew_run_done_detail, CrewRunMath.amount(mine.contribution), CrewRunMath.amount(mine.consumed)),
                        style = navyText(14.sp, CrewNavy.Text, FontWeight.Medium, 1.45f))
                    Text(stringResource(R.string.crew_run_reset), style = navyText(14.sp, CrewNavy.Sub, FontWeight.Medium, 1.45f))
                }
                else -> Text(stringResource(R.string.crew_run_can), style = navyText(18.sp, CrewNavy.Cyan, FontWeight.ExtraBold),
                    modifier = Modifier.testTag("home-crew-run-can"))
            }
            today.crewLevel?.let { level ->
                Text(
                    stringResource(R.string.crew_run_level_bonus, level, CrewRunMath.percent(today.staminaBonus), CrewRunMath.percent(today.rewardBonus)),
                    style = navyText(14.sp, CrewNavy.LevelInk, FontWeight.SemiBold, 1.45f), modifier = Modifier.padding(top = 6.dp).testTag("home-crew-run-level"),
                )
            }
        }
    }
}

/** 크루 레벨 업 — 서버가 적은 마지막 레벨 업을 한 번 보인다(앞뒤 체력 · 보상 효과) */
@Composable
internal fun CrewLevelUpSheet(event: com.stepup.android.domain.CrewLevelEvent, onDismiss: () -> Unit, onOpen: () -> Unit) {
    NavySheet(stringResource(R.string.crew_run_level_up_title), onDismiss, Modifier.testTag("home-crew-level-up")) {
        Text("Lv.${event.level}", style = navyText(44.sp, CrewNavy.Text, FontWeight.ExtraBold, 1.1f))
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.crew_run_level_up_body, event.level, CrewRunMath.percent(CrewRunMath.levelStamina(event.level)),
                CrewRunMath.percent(CrewRunMath.levelReward(event.level))),
            style = navyText(16.sp, CrewNavy.Cyan, FontWeight.SemiBold, 1.45f),
        )
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.crew_run_level_up_note), style = navyText(15.sp, CrewNavy.Sub, FontWeight.Medium, 1.45f))
        Spacer(Modifier.height(18.dp))
        NavyPrimaryButton(stringResource(R.string.crew_run_level_up_ok), onOpen, Modifier.testTag("home-crew-level-up-ok"))
    }
}

/** 크루 러닝 규칙 — 규칙 줄, 인원 보너스 표, 크루 레벨로 */
@Composable
internal fun CrewRunRulesSheet(rules: CrewRunRules, onDismiss: () -> Unit, onLevel: () -> Unit) {
    val ink = blueInk()
    BlueSheet(stringResource(R.string.crew_run_sheet_title), onDismiss, Modifier.testTag("home-crew-run-sheet")) {
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()).padding(horizontal = 4.dp)) {
            Spacer(Modifier.height(12.dp))
            crewRunRuleLines(rules).forEach { (title, body) ->
                Text(title, style = blueText(16.sp, ink.text, FontWeight.Bold, 1.35f))
                if (body.isNotEmpty()) BlueSheetBody(body, centered = false)
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(12.dp))
            HomeLabel(stringResource(R.string.crew_run_headcount_title))
            Spacer(Modifier.height(4.dp))
            BlueSheetBody(stringResource(R.string.crew_run_headcount_note), centered = false)
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
