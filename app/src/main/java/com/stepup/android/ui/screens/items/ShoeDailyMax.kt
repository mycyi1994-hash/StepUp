package com.stepup.android.ui.screens.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.ShoeRules
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.feedbackClickable
import kotlin.math.roundToLong

/**
 * 이 신발로 하루에 받을 수 있는 최대 SUP(서버 0120 하루 최대) — 서버 규칙이 하루 최대(HOUR)이고 서버 신발일 때만.
 * 효율은 포인트까지 든 실효 값(서버 `shoe_effective` — [com.stepup.android.domain.ServerStats.efficiencyBps]).
 * 서버에서 규칙을 받지 못했거나 예전 규칙(CAP)이면 null — 줄을 두지 않는다(받지 않은 규칙으로 숫자를 짓지 않는다)
 */
internal fun shoeDailyMax(shoe: Sneaker, rules: ShoeRules): Double? {
    if (!rules.dailyMax.fromServer) return null
    val server = shoe.server ?: return null
    return rules.dailyMaxSup(server.efficiencyBps)?.takeIf { it > 0.0 }
}

/** "약 654" — 정수로 반올림하고 자리 점을 찍는다 */
internal fun dailyMaxNumber(sup: Double): String =
    java.text.NumberFormat.getIntegerInstance().format(sup.roundToLong())

/**
 * 능력치 칸 아래 조용한 한 줄 "하루 최대 약 N SUP" · ⓘ — 누르면 아래에 짧은 설명(능력치 칸의 ⓘ 와 같은 방식, 창 없음).
 * 적립 배수(이벤트 · 2배권) · 새 계정 절반은 이 숫자에 넣지 않고 설명으로만 말한다 — 정산은 늘 서버다.
 */
@Composable
internal fun ShoeDailyMaxLine(shoe: Sneaker, rules: ShoeRules, modifier: Modifier = Modifier) {
    val sup = shoeDailyMax(shoe, rules) ?: return
    val t = runTone()
    var open by rememberSaveable(shoe.id) { mutableStateOf(false) }
    val line = stringResource(R.string.tb_daily_max, dailyMaxNumber(sup))
    val info = stringResource(R.string.tb_daily_max_info)
    val action = stringResource(if (open) R.string.shoe_info_hide_cd else R.string.shoe_info_show_cd, line)
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .feedbackClickable(role = Role.Button, onClick = { open = !open })
            .semantics(mergeDescendants = true) { contentDescription = listOfNotNull(line, info.takeIf { open }, action).joinToString(". ") }
            .heightIn(min = 44.dp).padding(horizontal = 4.dp, vertical = 8.dp)
            .testTag("shoe-daily-max"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(line, style = runTextStyle(14.sp, t.label, FontWeight.Medium), modifier = Modifier.weight(1f, fill = false))
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Outlined.Info, contentDescription = null, tint = if (open) t.text else t.label, modifier = Modifier.size(16.dp))
        }
        if (open) {
            Text(
                info, style = runTextStyle(13.sp, t.label),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("shoe-daily-max-info"),
            )
        }
    }
}
