package com.stepup.android.ui.components

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.Faction
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.RunnerTitle
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.SneakerDesigns
import com.stepup.android.ui.theme.Carbon
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt

/** 희귀도 표시 색 — 속성색과 무관하게 등급을 한눈에 읽히게 한다. */
fun Rarity.tint(): Color = when (this) {
    Rarity.COMMON -> Color(0xFF7C8AA6)
    Rarity.RARE -> Color(0xFF1E8FE8)
    Rarity.EPIC -> Color(0xFF8B4DE8)
    Rarity.LEGENDARY -> Color(0xFFD99A00)
}

@Composable
fun Rarity.label(): String = stringResource(
    when (this) {
        Rarity.COMMON -> R.string.rarity_common
        Rarity.RARE -> R.string.rarity_rare
        Rarity.EPIC -> R.string.rarity_epic
        Rarity.LEGENDARY -> R.string.rarity_legendary
    }
)

/**
 * 속성 표시색.
 *
 * 도메인의 네온색([Faction.accent])은 신발 그림이 쓴다 — 검은 신발 위에서
 * 빛나야 하므로 형광이다. 그 색을 흰 바닥의 글자에 그대로 쓰면 노랑·연두가
 * 거의 안 보인다. 화면용은 같은 계열에서 한 단계 눌러 따로 둔다.
 */
fun Faction.tint(): Color = when (this) {
    Faction.FIRE -> Color(0xFFE8352F)
    Faction.WATER -> Color(0xFF1E8FE8)
    Faction.LIGHTNING -> Color(0xFFD99A00)
    Faction.WIND -> Color(0xFF4FA318)
}

@Composable
fun Faction.label(): String = stringResource(factionNameRes(this))

/**
 * 도감 한 칸의 모델명 리소스.
 *
 * 이름이 속성마다 다르므로 속성을 함께 받는다 — 불의 전설 1번은 "인페르노
 * 크라운", 물의 전설 1번은 "어비스 타이드"다. 등급만으로는 정할 수 없다.
 */
@StringRes
fun variantNameRes(faction: Faction, rarity: Rarity, variant: Int): Int {
    val index = SneakerDesigns.indexOf(rarity, variant.coerceIn(0, rarity.variantCount - 1))
    val names = when (faction) {
        Faction.FIRE -> listOf(
            R.string.nft_fire_01,
            R.string.nft_fire_02,
            R.string.nft_fire_03,
            R.string.nft_fire_04,
            R.string.nft_fire_05,
            R.string.nft_fire_06,
            R.string.nft_fire_07,
            R.string.nft_fire_08,
            R.string.nft_fire_09,
            R.string.nft_fire_10,
            R.string.nft_fire_11,
            R.string.nft_fire_12,
            R.string.nft_fire_13,
        )
        Faction.WATER -> listOf(
            R.string.nft_water_01,
            R.string.nft_water_02,
            R.string.nft_water_03,
            R.string.nft_water_04,
            R.string.nft_water_05,
            R.string.nft_water_06,
            R.string.nft_water_07,
            R.string.nft_water_08,
            R.string.nft_water_09,
            R.string.nft_water_10,
            R.string.nft_water_11,
            R.string.nft_water_12,
            R.string.nft_water_13,
        )
        Faction.LIGHTNING -> listOf(
            R.string.nft_lightning_01,
            R.string.nft_lightning_02,
            R.string.nft_lightning_03,
            R.string.nft_lightning_04,
            R.string.nft_lightning_05,
            R.string.nft_lightning_06,
            R.string.nft_lightning_07,
            R.string.nft_lightning_08,
            R.string.nft_lightning_09,
            R.string.nft_lightning_10,
            R.string.nft_lightning_11,
            R.string.nft_lightning_12,
            R.string.nft_lightning_13,
        )
        Faction.WIND -> listOf(
            R.string.nft_wind_01,
            R.string.nft_wind_02,
            R.string.nft_wind_03,
            R.string.nft_wind_04,
            R.string.nft_wind_05,
            R.string.nft_wind_06,
            R.string.nft_wind_07,
            R.string.nft_wind_08,
            R.string.nft_wind_09,
            R.string.nft_wind_10,
            R.string.nft_wind_11,
            R.string.nft_wind_12,
            R.string.nft_wind_13,
        )
    }
    return names[(index - 1).coerceIn(0, names.lastIndex)]
}

@StringRes
fun factionNameRes(faction: Faction): Int = when (faction) {
    Faction.FIRE -> R.string.faction_fire
    Faction.WATER -> R.string.faction_water
    Faction.LIGHTNING -> R.string.faction_lightning
    Faction.WIND -> R.string.faction_wind
}

/** 모델명 — "인페르노 크라운" */
@Composable
fun variantLabel(faction: Faction, rarity: Rarity, variant: Int): String =
    stringResource(variantNameRes(faction, rarity, variant))

/** 모델명만 — "인페르노 크라운" */
@Composable
fun Sneaker.variantLabel(): String = variantLabel(faction, rarity, variant)

/**
 * 화면에 쓰는 이름 — "인페르노 크라운".
 *
 * 속성을 앞에 붙이지 않는다. 도감의 이름 자체가 이미 속성을 담고 있어
 * ("인페르노"는 불이다) 붙이면 "불 인페르노 크라운"이 된다.
 */
@Composable
fun Sneaker.fullLabel(): String = variantLabel()

/** Composable 밖(토스트·알림)에서 쓰는 같은 이름 */
fun Sneaker.fullLabel(context: Context): String = fullSneakerLabel(context, faction, rarity, variant)

fun fullSneakerLabel(context: Context, faction: Faction, rarity: Rarity, variant: Int): String =
    context.getString(variantNameRes(faction, rarity, variant))

/** 러너 칭호 */
@Composable
fun RunnerTitle.label(): String = stringResource(
    when (this) {
        RunnerTitle.ROOKIE -> R.string.tier_rookie
        RunnerTitle.STRIDER -> R.string.tier_strider
        RunnerTitle.TRAILBLAZER -> R.string.tier_trailblazer
        RunnerTitle.PACESETTER -> R.string.tier_pacesetter
        RunnerTitle.ELITE -> R.string.tier_elite
        RunnerTitle.MASTER -> R.string.tier_master
        RunnerTitle.LEGEND -> R.string.tier_legend
    }
)

/** 속성 pill 배지 */
@Composable
fun FactionChip(faction: Faction, modifier: Modifier = Modifier, small: Boolean = false) {
    val c = faction.tint()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(c.copy(alpha = 0.14f))
            .border(1.dp, c.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(horizontal = if (small) 7.dp else 10.dp, vertical = if (small) 2.dp else 4.dp),
    ) {
        Text(
            text = faction.label(),
            color = c,
            fontSize = if (small) 8.5.sp else 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
    }
}

/** 희귀도 pill 배지 */
@Composable
fun RarityChip(rarity: Rarity, modifier: Modifier = Modifier, small: Boolean = false) {
    val c = rarity.tint()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(c.copy(alpha = 0.15f))
            .border(1.dp, c.copy(alpha = 0.55f), RoundedCornerShape(50))
            .padding(horizontal = if (small) 7.dp else 10.dp, vertical = if (small) 2.dp else 4.dp),
    ) {
        Text(
            text = rarity.label(),
            color = c,
            fontSize = if (small) 8.5.sp else 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
    }
}

/**
 * 컬렉션 그리드에 쓰는 스니커즈 카드.
 * 희귀도가 카드 테두리와 배경 그라데이션을 결정해 수집 진열대처럼 보이게 한다.
 */
@Composable
fun SneakerCollectionCard(
    sneaker: Sneaker,
    modifier: Modifier = Modifier,
    count: Int = 1,
    onClick: () -> Unit = {},
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.clip(shape).background(CarbonHigh).border(1.dp, if (sneaker.equipped) Volt else Edge, shape)
            .quietClickable(onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SneakerFrame(sneaker, modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f), corner = 14.dp)
        Text(sneaker.variantLabel(), style = MaterialTheme.typography.titleMedium, color = Snow)
        Text(sneaker.rarity.label() + " · " + sneaker.faction.label(), style = MaterialTheme.typography.bodyMedium, color = Silver)
        if (sneaker.equipped) Text(stringResource(R.string.items_equipped), style = MaterialTheme.typography.bodyMedium, color = com.stepup.android.ui.theme.VoltText)
        if (count > 1) Text("×$count", style = MaterialTheme.typography.bodyMedium, color = Snow)
        Text(stringResource(R.string.level_chip, sneaker.level) + " · +%.1f%%".format(sneaker.boostPercent), style = MaterialTheme.typography.bodyMedium, color = Snow)
        Text(stringResource(R.string.sneaker_mint_no, sneaker.mintNumber), style = MaterialTheme.typography.bodyMedium, color = Silver)
    }
}

/** 아직 획득하지 못한 도감 슬롯 */
@Composable
fun SneakerLockedSlot(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Carbon, shape)
            .border(1.dp, Edge, shape)
            .height(210.dp),
        contentAlignment = Alignment.Center,
    ) {
        HexEmblem(size = 40.dp, glow = false)
    }
}

/** 스탯 한 줄 — 이름 · 막대 · 값 */
@Composable
fun StatBar(
    label: String,
    value: Double,
    max: Double,
    modifier: Modifier = Modifier,
    accent: Color = Volt,
    display: String = "%.2f".format(value),
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Silver)
            Text(
                text = display,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Snow,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Snow.copy(alpha = 0.08f)),
        ) {
            val fraction = (value / max).coerceIn(0.0, 1.0).toFloat()
            if (fraction > 0.002f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(listOf(accent.copy(alpha = 0.7f), accent)),
                        ),
                )
            }
        }
    }
}

/** 착용 중인 스니커즈 히어로 카드 */
@Composable
fun EquippedSneakerCard(
    sneaker: Sneaker,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    // S2 — 착용 중 · 등급 → 이름 → 민팅 번호 · 레벨 → 기울인 파란 면 위 신발 → 능력치 한 줄
    androidx.compose.foundation.layout.Column(
        modifier.fillMaxWidth().quietClickable(onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        S2Kicker(stringResource(R.string.items_equipped) + " · " + sneaker.rarity.label())
        androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp))
        S2Headline(sneaker.fullLabel())
        androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
        S2Subtitle(stringResource(R.string.sneaker_mint_no, sneaker.mintNumber) + " · " +
            stringResource(R.string.level_chip, sneaker.level))
        androidx.compose.foundation.layout.Spacer(Modifier.height(14.dp))
        S2ShoeStage(sneaker, Modifier.fillMaxWidth(0.86f))
        androidx.compose.foundation.layout.Spacer(Modifier.height(14.dp))
        S2Stats(listOf(
            stringResource(R.string.stat_boost) to "+%.1f%%".format(sneaker.boostPercent),
            stringResource(R.string.stat_luck) to "%.2f".format(sneaker.luck),
            stringResource(R.string.stat_comfort) to "%.2f".format(sneaker.comfort),
        ), valueSize = 22.sp)
    }
}

@Composable
private fun SneakerStatColumn(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, color = Slate, fontSize = 9.5.sp)
        Text(value, color = Snow, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SneakerMiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CarbonHigh)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, color = Slate, fontSize = 10.sp)
        Text(value, color = Snow, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}
