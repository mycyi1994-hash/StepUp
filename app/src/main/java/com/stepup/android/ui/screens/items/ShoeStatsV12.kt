package com.stepup.android.ui.screens.items

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.experience.feedbackClickable

/*
 * v12 디자인 시제품(2026-11-26)의 신발 능력치 줄 — "효율 140" · 얇은 막대(파랑), 착화감(보라), 내구도 "18 / 31분" + 수리 칩 · 막대(회색,
 * 곧 닳으면 주황), 레벨 "Lv.1" · 막대 · "1 / 20". 값 · 막대 길이 · ⓘ 설명은 예전 카툰 칸(ShoeStatCells)과 같은 [detailStatRows] 를 쓴다.
 * 이름을 누르면 ⓘ 한 줄 설명이 펼쳐진다(레벨은 없음). 테스트 이름(shoe-cell-*)도 예전 그대로.
 */

private val EffBar = Color(0xFF2F6BF0)
private val ComfortBar = Color(0xFF8B5CF6)
private val WearBar = Color(0xFF6F7B91)
private val UrgentBar = Color(0xFFFF7A1A)

@Composable
fun V12ShoeStats(
    shoe: Sneaker,
    modifier: Modifier = Modifier,
    onRepair: (() -> Unit)? = null,
    repairEnabled: Boolean = true,
    /** 13차 — 줄을 누르면 능력치 시트 하나(줄마다 ⓘ 대신). null 이면 예전처럼 줄마다 한 줄 설명을 펼친다 */
    onInfo: (() -> Unit)? = null,
) {
    val rules = rememberShoeRules()
    val rows = remember(shoe, rules) { detailStatRows(shoe, rules) }
    val urgent = repairUrgent(shoe, rules)
    var infoOpen by rememberSaveable(shoe.id) { mutableStateOf<DetailStat?>(null) }
    val newMeaning = newStatMeaning(rules)
    val c = StepUpUi.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        val order = listOf(DetailStat.EFFICIENCY, DetailStat.COMFORT, DetailStat.DURABILITY, DetailStat.LEVEL)
        order.mapNotNull { stat -> rows.firstOrNull { it.stat == stat } }.forEach { row ->
            val minutes = row.unit == com.stepup.android.ui.screens.customize.StatUnit.MINUTES
            val value = if (minutes) minutesText(row.value) else row.value
            val meaning = when (row.stat) {
                DetailStat.LEVEL -> null
                DetailStat.EFFICIENCY -> stringResource(statMeaningRes(PointStat.EFFICIENCY, newMeaning))
                DetailStat.COMFORT -> stringResource(statMeaningRes(PointStat.COMFORT, newMeaning))
                DetailStat.DURABILITY -> stringResource(statMeaningRes(PointStat.DURABILITY, newMeaning))
            }.takeIf { row.explained }
            val label = stringResource(
                when (row.stat) {
                    DetailStat.LEVEL -> R.string.sdv_info_level
                    DetailStat.EFFICIENCY -> R.string.my_shoes_stat_efficiency
                    DetailStat.COMFORT -> R.string.sdv_stat_comfort
                    DetailStat.DURABILITY -> R.string.sdv_stat_durability
                },
            )
            val tag = "shoe-cell-" + row.stat.name.lowercase()
            val open = infoOpen == row.stat
            val spoken = stringResource(R.string.sdv_bar_cd, label, value, if (minutes) minutesText(row.end) else row.end)
            Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = spoken }.testTag(tag)) {
                Row(Modifier.fillMaxWidth().heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier.weight(1f).then(
                            if (onInfo != null) Modifier.clip(RoundedCornerShape(8.dp)).feedbackClickable(role = Role.Button, onClick = onInfo)
                            else if (meaning != null) Modifier.clip(RoundedCornerShape(8.dp))
                                .feedbackClickable(role = Role.Button) { infoOpen = if (open) null else row.stat } else Modifier,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (row.stat == DetailStat.LEVEL) "Lv.${shoe.level}" else label,
                            color = c.label, fontSize = if (row.stat == DetailStat.LEVEL) 18.sp else 16.sp,
                            fontWeight = if (row.stat == DetailStat.LEVEL) FontWeight.ExtraBold else FontWeight.SemiBold,
                        )
                        if (meaning != null && onInfo == null) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Outlined.Info, null, tint = if (open) c.label else c.tertiaryLabel, modifier = Modifier.size(15.dp))
                        }
                    }
                    if (row.stat == DetailStat.LEVEL) {
                        Box(Modifier.weight(1.6f).padding(horizontal = 10.dp)) { ThinBar(row.fraction, c.primary) }
                    }
                    Text(
                        value, color = if (row.stat == DetailStat.DURABILITY && urgent) c.live else c.label,
                        fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.testTag("$tag-value"),
                    )
                    if (row.stat == DetailStat.DURABILITY && onRepair != null && repairable(shoe)) {
                        Spacer(Modifier.width(8.dp))
                        V12RepairChip(urgent, repairEnabled, onRepair)
                    }
                }
                if (row.stat != DetailStat.LEVEL) {
                    Spacer(Modifier.height(10.dp))
                    ThinBar(
                        row.fraction,
                        when (row.stat) {
                            DetailStat.EFFICIENCY -> EffBar
                            DetailStat.COMFORT -> ComfortBar
                            else -> if (urgent) UrgentBar else WearBar
                        },
                        Modifier.testTag("$tag-bar"),
                    )
                }
                if (open && meaning != null) {
                    Text(
                        meaning, color = c.secondaryLabel, fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp).semantics { liveRegion = LiveRegionMode.Polite }.testTag("$tag-info"),
                    )
                }
            }
        }
    }
}

@Composable
private fun ThinBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    val c = StepUpUi.colors
    Box(modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(c.fill)) {
        val f = fraction.coerceIn(0f, 1f)
        if (f > 0f) Box(Modifier.fillMaxWidth(f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(color))
    }
}

/** 내구도 줄의 "🔧 수리" 칩 — 급하면 주황. 못 누르면 흐리게 */
@Composable
fun V12RepairChip(urgent: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val c = StepUpUi.colors
    val ink = if (urgent) UrgentBar else c.label
    Row(
        Modifier.alpha(if (enabled) 1f else .5f).clip(RoundedCornerShape(14.dp))
            .background(if (urgent) UrgentBar.copy(alpha = .14f) else c.fill)
            .feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp).testTag("shoe-durability-repair"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Build, null, tint = ink, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(stringResource(R.string.v12_repair_short), color = ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

/** 스탯 포인트 줄(옅은 파란 면) — "스탯 포인트 2 · 나누기 ›". 못 나누면 흐리게 · 까닭 한 줄 */
@Composable
fun V12PointsRow(title: String, count: Int?, action: String, note: String?, enabled: Boolean, onClick: () -> Unit) {
    val c = StepUpUi.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.primary.copy(alpha = if (c.dark) .16f else .08f))
            .feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp).alpha(if (enabled) 1f else .55f).testTag("shoe-care-points"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = c.label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (count != null) {
                Spacer(Modifier.width(8.dp))
                Text("$count", color = c.accentText, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.weight(1f))
            Text(action, color = c.accentText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.accentText, modifier = Modifier.size(20.dp))
        }
        if (note != null) Text(note, color = c.secondaryLabel, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

/** 작은 글자 링크 "신발 기록 ›" */
@Composable
fun V12LinkText(text: String, enabled: Boolean, tag: String, onClick: () -> Unit, chevron: Boolean = true) {
    val c = StepUpUi.colors
    Row(
        Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(8.dp))
            .feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp).alpha(if (enabled) 1f else .45f).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = c.secondaryLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        if (chevron) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.secondaryLabel, modifier = Modifier.size(18.dp))
    }
}
