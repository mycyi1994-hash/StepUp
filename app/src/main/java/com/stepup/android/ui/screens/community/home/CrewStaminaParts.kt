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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.CrewPool
import com.stepup.android.domain.CrewStamina
import com.stepup.android.domain.CrewStaminaRules
import com.stepup.android.ui.screens.community.chat.BlueBar
import com.stepup.android.ui.screens.community.chat.BlueSheet
import com.stepup.android.ui.screens.community.chat.BlueSheetBody
import com.stepup.android.ui.screens.community.chat.blueInk
import com.stepup.android.ui.screens.community.chat.blueText

/*
 * 크루 체력(서버 0069, 2026-10-21) — 크루 홈의 오늘 크루 체력 칸과 규칙 시트. 값은 서버(crew_stamina)가 정한 그대로 보인다.
 */

/** 내 몫 한 줄 — 남은 몫 · 다 씀 · 개인 에너지로 */
@Composable
internal fun crewStaminaMine(stamina: CrewStamina): String = when (stamina.pool) {
    CrewPool.CREW -> stringResource(
        R.string.crew_stamina_my_left, CrewStaminaRules.amount(stamina.me.left), CrewStaminaRules.amount(stamina.share),
    )
    CrewPool.CREW_EMPTY -> stringResource(R.string.crew_stamina_my_empty)
    CrewPool.PERSONAL -> stringResource(R.string.crew_stamina_personal)
}

@Composable
private fun crewStaminaMineColor(pool: CrewPool): Color {
    val ink = blueInk()
    return when (pool) {
        CrewPool.CREW -> ink.info
        CrewPool.CREW_EMPTY -> ink.warn
        CrewPool.PERSONAL -> ink.secondary
    }
}

/** 크루 홈 — 오늘 크루 체력: 남은 양 / 전체 · 나눠 받는 인원 · 막대 · 내 몫. 누르면 규칙 시트(주간 기록 칸과 같은 결) */
@Composable
internal fun CrewStaminaBlock(stamina: CrewStamina, onClick: () -> Unit) {
    val ink = blueInk()
    Column(Modifier.fillMaxWidth().homeClickable(onClick = onClick).padding(vertical = 22.dp).testTag("home-stamina")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.crew_stamina_title), style = blueText(18.sp, ink.text, FontWeight.Bold), modifier = Modifier.weight(1f))
            if (stamina.contributors > 0) {
                Text(
                    stringResource(R.string.crew_stamina_sharers, stamina.contributors), style = blueText(15.sp, ink.secondary, FontWeight.SemiBold),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.crew_stamina_amount_of, CrewStaminaRules.amount(stamina.left), CrewStaminaRules.amount(stamina.max)),
            style = blueText(36.sp, ink.text, FontWeight.ExtraBold, 1.15f), modifier = Modifier.testTag("home-stamina-left"),
        )
        Spacer(Modifier.height(14.dp))
        BlueBar(CrewStaminaRules.fraction(stamina.left, stamina.max), Modifier.fillMaxWidth().height(10.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            crewStaminaMine(stamina), style = blueText(15.sp, crewStaminaMineColor(stamina.pool), FontWeight.SemiBold, 1.45f),
            modifier = Modifier.testTag("home-stamina-mine"),
        )
    }
}

/** 크루 체력 규칙 — 오늘 값 · 레벨 보너스 · 내 몫, 규칙 네 줄, 나눠 받는 크루원과 보탠 양 */
@Composable
internal fun CrewStaminaSheet(stamina: CrewStamina, onDismiss: () -> Unit) {
    val ink = blueInk()
    BlueSheet(stringResource(R.string.crew_stamina_sheet_title), onDismiss, Modifier.testTag("home-stamina-sheet")) {
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()).padding(horizontal = 4.dp)) {
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.crew_stamina_amount_of, CrewStaminaRules.amount(stamina.left), CrewStaminaRules.amount(stamina.max)),
                style = blueText(32.sp, ink.text, FontWeight.ExtraBold, 1.15f),
            )
            if (stamina.levelBonus > 0.0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.crew_stamina_level_bonus, CrewStaminaRules.amount(stamina.levelBonus)),
                    style = blueText(15.sp, ink.info, FontWeight.SemiBold),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(crewStaminaMine(stamina), style = blueText(16.sp, crewStaminaMineColor(stamina.pool), FontWeight.Bold, 1.45f))
            Spacer(Modifier.height(18.dp))
            BlueSheetBody(stringResource(R.string.crew_stamina_rule), centered = false)
            Spacer(Modifier.height(10.dp))
            BlueSheetBody(stringResource(R.string.crew_stamina_rule_empty), centered = false)
            Spacer(Modifier.height(10.dp))
            BlueSheetBody(stringResource(R.string.crew_stamina_rule_who), centered = false)
            Spacer(Modifier.height(10.dp))
            BlueSheetBody(stringResource(R.string.crew_stamina_rule_solo), centered = false)
            if (stamina.members.isNotEmpty()) {
                Spacer(Modifier.height(22.dp))
                HomeLabel(stringResource(R.string.crew_stamina_members))
                Spacer(Modifier.height(4.dp))
                val meLabel = stringResource(R.string.crew_stamina_me)
                stamina.members.forEach { member ->
                    HomeValueRow(
                        label = if (member.userId == stamina.me.userId) "${member.name} · $meLabel" else member.name,
                        value = stringResource(R.string.crew_stamina_member_value, CrewStaminaRules.amount(member.contribution)),
                        modifier = Modifier.testTag("home-stamina-member"),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
