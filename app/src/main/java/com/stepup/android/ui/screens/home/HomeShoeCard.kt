package com.stepup.android.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.Bag
import com.stepup.android.domain.ItemKind
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.tier
import com.stepup.android.ui.components.RimKind
import com.stepup.android.ui.components.ShoeArtFull
import com.stepup.android.ui.components.ShoeArtThumbnail
import com.stepup.android.ui.components.ShoeGradeBadge
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.screens.items.shoeName
import com.stepup.android.ui.components.stepUpRim
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.items.durabilityPercent
import com.stepup.android.ui.screens.items.durabilityText
import com.stepup.android.ui.screens.items.itemArt
import com.stepup.android.ui.screens.items.itemName
import com.stepup.android.ui.screens.items.rememberShoeRules
import com.stepup.android.ui.screens.items.repairUrgent
import com.stepup.android.ui.screens.items.repairable

/*
 * 러닝 홈의 "지금 신은 신발"(v12 디자인 시제품 2026-11-26 — 사진 카드 · 숫자 줄 자리).
 * 카드: 이름 ›(신발 상세) · 등급 · 오른쪽 위로 비어져 나온 신발 그림 · 내구도 "남은 / 가득 분" + 수리 · 막대 · 러닝 아이템 ›(아이템 탭).
 * 값은 모두 신발 탭과 같은 셈(durabilityText · repairUrgent) — 새로 셈하지 않는다. 신은 신발이 없으면 신발 탭으로 가는 한 줄.
 */

/** 다음 러닝에 쓸 아이템 줄 — 고른 것이 있으면 그것, 없으면 가방에 든 러닝 아이템 수, 그것도 없으면 줄을 두지 않는다 */
internal sealed interface HomeRunItem {
    data class Picked(val kind: ItemKind) : HomeRunItem
    data class Owned(val count: Int) : HomeRunItem

    companion object {
        fun of(bag: Bag?, picks: Set<ItemKind>): HomeRunItem? {
            val picked = picks.firstOrNull { bag == null || bag.qty(it) > 0 }
            if (picked != null) return Picked(picked)
            val owned = bag?.runPicks?.sumOf { it.qty } ?: 0
            return if (owned > 0) Owned(owned) else null
        }
    }
}

@Composable
internal fun HomeShoeSection(
    shoe: Sneaker?,
    runItem: HomeRunItem?,
    onOpenShoe: () -> Unit,
    onRepair: () -> Unit,
    onOpenItems: () -> Unit,
    onOpenShoes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = StepUpUi.colors
    Column(modifier.fillMaxWidth().testTag("home-shoe")) {
        Text(
            stringResource(R.string.v12_home_shoe_title), color = c.label, fontSize = 17.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(12.dp))
        if (shoe == null) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.card)
                    .stepUpRim(RoundedCornerShape(22.dp), RimKind.Surface)
                    .feedbackClickable(role = Role.Button, onClick = onOpenShoes).padding(20.dp).testTag("home-shoe-none"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.v12_home_shoe_none), color = c.secondaryLabel, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.v12_home_shoe_pick), color = c.accentText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.accentText, modifier = Modifier.size(20.dp))
            }
            return@Column
        }
        ShoeCard(shoe, runItem, onOpenShoe, onRepair, onOpenItems)
    }
}

@Composable
private fun ShoeCard(shoe: Sneaker, runItem: HomeRunItem?, onOpenShoe: () -> Unit, onRepair: () -> Unit, onOpenItems: () -> Unit) {
    val c = StepUpUi.colors
    val rules = rememberShoeRules()
    val urgent = repairUrgent(shoe, rules)
    val shape = RoundedCornerShape(24.dp)
    // 신발 그림이 카드 위로 비어져 나와 "지금 신은 신발" 제목 줄 옆까지 올라간다(시제품 v12_home) — Box 는 자르지 않는다
    Box(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().clip(shape).background(c.card).stepUpRim(shape, RimKind.Surface)
                .padding(horizontal = 22.dp, vertical = 20.dp),
        ) {
            // 이름 칸은 신발 그림이 차지한 오른쪽을 비운다 — 예전 절반 너비는 좁은 폰에서 그림이 "›"를 덮었다(2026-11-28 디자인 점검)
            Row(Modifier.fillMaxWidth()) {
            Column(
                Modifier.weight(1f).heightIn(min = 84.dp).clip(RoundedCornerShape(12.dp))
                    .feedbackClickable(role = Role.Button, onClick = onOpenShoe).testTag("home-shoe-name"),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        shoe.shoeName(), color = c.label, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                    )
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.secondaryLabel, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.height(10.dp))
                ShoeGradeBadge(shoe.tier)
            }
            Spacer(Modifier.width(ShoeArtReserve))
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth().testTag("home-shoe-durability"), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.sdv_stat_durability), color = c.label, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                Text(durabilityText(shoe, rules), color = if (urgent) c.live else c.label, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("home-shoe-durability-value"))
                if (repairable(shoe)) {
                    Spacer(Modifier.width(8.dp))
                    RepairChip(urgent, onRepair)
                }
            }
            Spacer(Modifier.height(12.dp))
            val fraction = durabilityPercent(shoe) / 100f
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(c.fill)) {
                if (fraction > 0f) {
                    Box(Modifier.fillMaxWidth(fraction).height(6.dp).clip(RoundedCornerShape(3.dp))
                        .background(if (urgent) RepairOrange else c.secondaryLabel.copy(alpha = .8f)))
                }
            }
            if (runItem != null) {
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.separator))
                RunItemLine(runItem, onOpenItems)
            }
        }
        ShoeArtThumbnail(
            shoe,
            Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-46).dp).width(176.dp).height(116.dp).testTag("home-shoe-art"),
            failedTint = c.tertiaryLabel, sample = ShoeArtFull,
        )
    }
}

private val RepairOrange = Color(0xFFFF7A1A)

/** 이름 칸 오른쪽에 비워 두는 너비 — 신발 그림(176dp, 카드 밖으로 4dp)이 카드 안쪽 여백(22dp) 너머로 차지하는 몫 */
private val ShoeArtReserve = 152.dp

@Composable
private fun RepairChip(urgent: Boolean, onClick: () -> Unit) {
    val c = StepUpUi.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.clip(shape).background(if (urgent) RepairOrange.copy(alpha = .14f) else c.fill)
            .feedbackClickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag("home-shoe-repair"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val ink = if (urgent) RepairOrange else c.label
        Icon(Icons.Outlined.Build, null, tint = ink, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(stringResource(R.string.v12_repair_short), color = ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RunItemLine(item: HomeRunItem, onClick: () -> Unit) {
    val c = StepUpUi.colors
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp).heightIn(min = 44.dp).clip(RoundedCornerShape(10.dp))
            .feedbackClickable(role = Role.Button, onClick = onClick).testTag("home-shoe-item"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.v12_home_run_item), color = c.secondaryLabel, fontSize = 15.sp, modifier = Modifier.weight(1f))
        when (item) {
            is HomeRunItem.Picked -> {
                Image(painterResource(itemArt(item.kind)), null, Modifier.size(34.dp))
                Spacer(Modifier.width(6.dp))
                Text(itemName(item.kind), color = c.label, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            is HomeRunItem.Owned -> Text(
                stringResource(R.string.v12_home_run_item_owned, item.count), color = c.label, fontSize = 15.sp, fontWeight = FontWeight.Bold,
            )
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.secondaryLabel, modifier = Modifier.size(20.dp))
    }
}
