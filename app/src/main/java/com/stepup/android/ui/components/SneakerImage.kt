package com.stepup.android.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stepup.android.R
import com.stepup.android.domain.Faction
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.SneakerDesigns

/**
 * 도감 시트에서 잘라낸 실사 신발 그림.
 *
 * 속성마다 13종, 모두 52종이다. 파일 이름의 번호는 도감 번호와 같다 —
 * sneaker_fire_01 은 FIR-001 이다.
 *
 * 배경은 **투명**이다. 예전 그림은 검은 배경이 옅게 깔려 있어 밝은 테마에서
 * 회색 얼룩처럼 보였다. 투명하게 두면 신발 뒤에 앱 배경이 그대로 비쳐서,
 * 흰 배경이든 검은 배경이든 카드와 한 몸으로 읽힌다.
 */
@DrawableRes
fun sneakerImageRes(faction: Faction, rarity: Rarity, variant: Int): Int? {
    val index = SneakerDesigns.indexOf(rarity, variant.coerceIn(0, rarity.variantCount - 1))
    val list = when (faction) {
        Faction.FIRE -> listOf(
            R.drawable.sneaker_fire_01,
            R.drawable.sneaker_fire_02,
            R.drawable.sneaker_fire_03,
            R.drawable.sneaker_fire_04,
            R.drawable.sneaker_fire_05,
            R.drawable.sneaker_fire_06,
            R.drawable.sneaker_fire_07,
            R.drawable.sneaker_fire_08,
            R.drawable.sneaker_fire_09,
            R.drawable.sneaker_fire_10,
            R.drawable.sneaker_fire_11,
            R.drawable.sneaker_fire_12,
            R.drawable.sneaker_fire_13,
        )
        Faction.WATER -> listOf(
            R.drawable.sneaker_water_01,
            R.drawable.sneaker_water_02,
            R.drawable.sneaker_water_03,
            R.drawable.sneaker_water_04,
            R.drawable.sneaker_water_05,
            R.drawable.sneaker_water_06,
            R.drawable.sneaker_water_07,
            R.drawable.sneaker_water_08,
            R.drawable.sneaker_water_09,
            R.drawable.sneaker_water_10,
            R.drawable.sneaker_water_11,
            R.drawable.sneaker_water_12,
            R.drawable.sneaker_water_13,
        )
        Faction.LIGHTNING -> listOf(
            R.drawable.sneaker_lightning_01,
            R.drawable.sneaker_lightning_02,
            R.drawable.sneaker_lightning_03,
            R.drawable.sneaker_lightning_04,
            R.drawable.sneaker_lightning_05,
            R.drawable.sneaker_lightning_06,
            R.drawable.sneaker_lightning_07,
            R.drawable.sneaker_lightning_08,
            R.drawable.sneaker_lightning_09,
            R.drawable.sneaker_lightning_10,
            R.drawable.sneaker_lightning_11,
            R.drawable.sneaker_lightning_12,
            R.drawable.sneaker_lightning_13,
        )
        Faction.WIND -> listOf(
            R.drawable.sneaker_wind_01,
            R.drawable.sneaker_wind_02,
            R.drawable.sneaker_wind_03,
            R.drawable.sneaker_wind_04,
            R.drawable.sneaker_wind_05,
            R.drawable.sneaker_wind_06,
            R.drawable.sneaker_wind_07,
            R.drawable.sneaker_wind_08,
            R.drawable.sneaker_wind_09,
            R.drawable.sneaker_wind_10,
            R.drawable.sneaker_wind_11,
            R.drawable.sneaker_wind_12,
            R.drawable.sneaker_wind_13,
        )
    }
    return list.getOrNull(index - 1)
}

/** 이미지가 있는 모든 도감 슬롯 — 스플래시 로테이션 등에 쓴다 */
val AllSneakerImages: List<Int> by lazy {
    buildList {
        for (f in Faction.entries) {
            for (r in Rarity.entries) {
                for (v in 0 until r.variantCount) {
                    sneakerImageRes(f, r, v)?.let(::add)
                }
            }
        }
    }
}

/**
 * 신발 비주얼의 표준 프레임.
 *
 * 배경판을 깔지 않는다. 그림의 바탕이 투명이라 카드 색이 그대로 비치고,
 * 그 카드 색은 테마를 따라간다 — 밝은 테마에서는 흰 바탕, 어두운 테마에서는
 * 검은 바탕 위에 신발만 얹힌다. 배경판을 깔면 테마를 바꿀 때마다 그 판만
 * 따로 남아 신발이 사각형 스티커처럼 보인다.
 *
 * ContentScale.Fit 이라 신발이 잘리지 않고, 남는 여백은 투명이다.
 */
@Composable
fun SneakerFrame(
    sneaker: Sneaker,
    modifier: Modifier = Modifier,
    corner: Dp = 14.dp,
    animate: Boolean = false,
    fade: Boolean = true,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        SneakerVisual(
            sneaker = sneaker,
            modifier = Modifier.fillMaxSize(),
            animate = animate,
            contentScale = ContentScale.Fit,
        )
    }
}

/**
 * 신발 비주얼 — 포스터 이미지가 있으면 이미지를, 없으면 Canvas 아트를 그린다.
 * 기본은 Fit이라 이미지가 잘리지 않는다.
 */
@Composable
fun SneakerVisual(
    sneaker: Sneaker,
    modifier: Modifier = Modifier,
    animate: Boolean = false,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val res = sneakerImageRes(sneaker.faction, sneaker.rarity, sneaker.variant)
    if (res != null) {
        Image(
            painter = painterResource(res),
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
        )
    } else if (animate) {
        SneakerHero(sneaker = sneaker, modifier = modifier)
    } else {
        SneakerArt(sneaker = sneaker, modifier = modifier)
    }
}
