package com.stepup.android.ui.components

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.stepup.android.ui.experience.LocalMotion
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stepup.android.R
import com.stepup.android.domain.Faction
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.ShoeTier
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.tier
import com.stepup.android.domain.SneakerDesigns
import kotlin.math.roundToInt

/**
 * 도감 시트에서 잘라낸 실사 신발 그림.
 *
 * 속성마다 13종, 모두 52종이다. 파일 이름의 번호는 도감 번호와 같다 —
 * sneaker_fire_01 은 FIR-001 이다.
 *
 * ── 배경 ──
 *
 * 전부 **투명**이다. 신발 뒤에 앱 배경이 그대로 비치므로 흰 테마든 검은
 * 테마든 카드와 한 몸으로 읽힌다. 배경판을 깔면 테마를 바꿀 때 그 판만
 * 따로 남아 신발이 사각형 스티커처럼 보인다.
 *
 * 잘라 낼 때 가장자리 색을 2px 안쪽에서 끌어다 썼다. 원본이 어두운 바탕
 * 위에 얹혀 있어 경계의 반투명 픽셀에 그 어두운 색이 섞여 있는데, 그대로
 * 두면 흰 배경에서 신발 둘레에 검은 테가 생긴다 — "누끼 딴 티"는 거의
 * 이것 때문이다.
 *
 * ── 크기 ──
 *
 * 52장이 모두 같은 크기(640×640)이고, 신발은 시트에서 있던 자리 그대로
 * 들어 있다. 각자 제 몸에 맞춰 자르면 납작한 레이서가 두툼한 트레일화만큼
 * 커 보여서, 도감을 펼쳤을 때 크기 비교가 되지 않는다.
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

/**
 * 이 켤레의 그림 — 새 도감(0045) 신발은 그 모델 그림, 예전 52종은 속성 × 변형 그림([SneakerVisual] 과 같은 고르기).
 * 이 앱에 그림이 없는 새 모델이면 null — 보유 신발 상세 v1 은 다른 신발 그림으로 채우지 않고 "이미지를 불러오지 못했어요"(시안 16)를 보인다.
 */
@DrawableRes
fun sneakerArtRes(sneaker: Sneaker): Int? =
    sneaker.modelId?.let(::shoeModelImageRes)
        ?: if (sneaker.modelId == null) sneakerImageRes(sneaker.faction, sneaker.rarity, sneaker.variant) else null

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
    muted: Boolean = false,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        SneakerVisual(
            sneaker = sneaker,
            modifier = Modifier.fillMaxSize(),
            animate = animate,
            muted = muted,
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
    muted: Boolean = false,
    contentScale: ContentScale = ContentScale.Fit,
) {
    // 새 도감(0045) 신발은 그 모델 그림, 아니면 예전 52종 그림
    val res = sneaker.modelId?.let(::shoeModelImageRes)
        ?: sneakerImageRes(sneaker.faction, sneaker.rarity, sneaker.variant)
    if (res != null) {
        val float = if (animate && LocalMotion.current.decorative) ambientPhase(3600, reverse = true) else null
        Image(
            painter = painterResource(res),
            contentDescription = null,
            modifier = modifier.graphicsLayer {
                if (float != null) {
                    translationY = (float.value - .5f) * 5.dp.toPx()
                    rotationZ = (float.value - .5f) * .6f
                }
            },
            contentScale = contentScale,
            colorFilter = if (muted) ColorFilter.colorMatrix(
                ColorMatrix().apply { setToSaturation(0f) },
            ) else null,
        )
    } else if (animate) {
        SneakerHero(sneaker = sneaker, modifier = modifier)
    } else {
        SneakerArt(sneaker = sneaker, modifier = modifier)
    }
}

// ── 등급 프레임 v8 ── design/shoe-grade-frames-v8 · docs/redesign/shoe-grade-frames
//
// 시안의 세 레이어(뒤 효과 · 앞 효과 · 프레임)는 모두 440 × 418 한 좌표계에 그려져 있다. 화면에서는
// 무대 면 → 뒤 효과 → 실제 신발 → 앞 효과 → 프레임 순서로 겹치고, 등급 글자 · 숫자 · 버튼은 이 밖의
// Compose 가 그린다. 그림은 보이는 갈래 여섯(ShoeTier — 레드라인 · 피니시는 새 도감 레전더리의 시리즈)을 따른다.
// 서버 등급 · 확률은 그대로 넷이다(2026-09-28 신발 화면 확정안).

private const val GradeArtWidth = 440f
private const val GradeArtHeight = 418f

/** 등급 그림의 가로 : 세로 — 늘이거나 줄여도 이 비율을 지켜 찌그러지지 않게 한다 */
const val GradeArtRatio = GradeArtWidth / GradeArtHeight

/** 무대 면 — 비교판 카드 안쪽 색. 효과가 어두운 면을 전제로 그려져 있어 밝은 테마에서도 이 면을 깐다 */
private val GradeStageFill = Color(0xFF081320)

/** 목록 칸의 면 — 모바일 시안의 보유 신발 칸 색 */
private val GradeThumbFill = Color(0xFF101E32)

/** 2배(880 × 836) 그림과 좁은 자리용 1배(440 × 418) 그림 — 격자 칸마다 2배 그림을 풀지 않게 */
private class GradeArt(
    @DrawableRes val back: Int,
    @DrawableRes val backSmall: Int,
    @DrawableRes val front: Int?,
    @DrawableRes val frontSmall: Int?,
    @DrawableRes val frame: Int,
    @DrawableRes val frameSmall: Int,
)

/** 일반 01 CARBON · 레어 02 VELOCITY · 에픽 03 PRISM · 레전더리 04 PODIUM · 레드라인 05 · 피니시 06. 일반은 앞 효과가 없다 */
private fun gradeArt(tier: ShoeTier): GradeArt = when (tier) {
    ShoeTier.COMMON -> GradeArt(
        R.drawable.shoe_grade_common_back, R.drawable.shoe_grade_common_back_small, null, null,
        R.drawable.shoe_grade_common_frame, R.drawable.shoe_grade_common_frame_small,
    )
    ShoeTier.RARE -> GradeArt(
        R.drawable.shoe_grade_rare_back, R.drawable.shoe_grade_rare_back_small,
        R.drawable.shoe_grade_rare_front, R.drawable.shoe_grade_rare_front_small,
        R.drawable.shoe_grade_rare_frame, R.drawable.shoe_grade_rare_frame_small,
    )
    ShoeTier.EPIC -> GradeArt(
        R.drawable.shoe_grade_epic_back, R.drawable.shoe_grade_epic_back_small,
        R.drawable.shoe_grade_epic_front, R.drawable.shoe_grade_epic_front_small,
        R.drawable.shoe_grade_epic_frame, R.drawable.shoe_grade_epic_frame_small,
    )
    ShoeTier.LEGENDARY -> GradeArt(
        R.drawable.shoe_grade_legendary_back, R.drawable.shoe_grade_legendary_back_small,
        R.drawable.shoe_grade_legendary_front, R.drawable.shoe_grade_legendary_front_small,
        R.drawable.shoe_grade_legendary_frame, R.drawable.shoe_grade_legendary_frame_small,
    )
    ShoeTier.REDLINE -> GradeArt(
        R.drawable.shoe_grade_redline_back, R.drawable.shoe_grade_redline_back_small,
        R.drawable.shoe_grade_redline_front, R.drawable.shoe_grade_redline_front_small,
        R.drawable.shoe_grade_redline_frame, R.drawable.shoe_grade_redline_frame_small,
    )
    ShoeTier.FINISH -> GradeArt(
        R.drawable.shoe_grade_finish_back, R.drawable.shoe_grade_finish_back_small,
        R.drawable.shoe_grade_finish_front, R.drawable.shoe_grade_finish_front_small,
        R.drawable.shoe_grade_finish_frame, R.drawable.shoe_grade_finish_frame_small,
    )
}

/** 1배 그림으로 충분한 폭(px) — 1배(440px)보다 1.3배 넓어지면 2배 그림을 쓴다(흐리지 않게) */
private const val GradeSmallMaxPx = GradeArtWidth * 1.3f

/** 프레임 바깥 팔각형 — 무대 면을 이 모양으로 깔면 프레임 띠가 그 가장자리를 덮는다 */
private val GradeOctagon = GenericShape { size, _ ->
    val x = size.width / GradeArtWidth
    val y = size.height / GradeArtHeight
    moveTo(53 * x, 25 * y)
    lineTo(387 * x, 25 * y)
    lineTo(415 * x, 53 * y)
    lineTo(415 * x, 365 * y)
    lineTo(387 * x, 393 * y)
    lineTo(53 * x, 393 * y)
    lineTo(25 * x, 365 * y)
    lineTo(25 * x, 53 * y)
    close()
}

/**
 * 440 × 418 좌표의 정사각형 자리(왼쪽, 위, 한 변)에 둔다 — 등급 그림과 같은 비율로 커지고 줄어든다.
 *
 * 신발 52장은 같은 판(640 × 640)에 시트의 자리 그대로 들어 있어, 한 켤레에 맞추지 않고 판 전체를 한 자리에 둔다.
 * 자리는 52장의 테두리를 모두 재서 정했다 — 가로는 시안 신발과 같은 폭, 가운데 바닥 선이 바닥광 위에 온다.
 */
private fun Modifier.gradeSlot(left: Float, top: Float, side: Float) = layout { measurable, constraints ->
    val width = constraints.maxWidth
    val height = constraints.maxHeight
    val placeable = measurable.measure(
        Constraints.fixed((width * side / GradeArtWidth).roundToInt(), (height * side / GradeArtHeight).roundToInt()),
    )
    layout(width, height) {
        placeable.place((width * left / GradeArtWidth).roundToInt(), (height * top / GradeArtHeight).roundToInt())
    }
}

@Composable
private fun GradeLayer(@DrawableRes res: Int) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        // 상자가 이미 그림 비율(440:418)이라 늘어나지 않는다 — 반 픽셀 차이만 메운다
        contentScale = ContentScale.FillBounds,
    )
}

/**
 * 등급 무대 — 큰 신발 한 켤레(신발 탭 · 보관함의 착용 카드 · 신발 상세 · 모델 · 뽑기 결과).
 * 무대 면 → 뒤 효과(바닥 그림자 · 바닥광 · 후광 · 스포트라이트) → 실제 신발 → 앞 효과(광점 · 반사광) → 프레임.
 */
@Composable
fun SneakerGradeStage(
    sneaker: Sneaker,
    modifier: Modifier = Modifier,
    animate: Boolean = false,
    /** 신발 층을 그릴지 — 신발 상세가 그림을 읽지 못했을 때(다시 불러오기 안내를 둘 자리) 끈다. 무대 · 프레임은 그대로 */
    showShoe: Boolean = true,
) {
    val tier = sneaker.tier
    val art = gradeArt(tier)
    BoxWithConstraints(modifier.aspectRatio(GradeArtRatio).testTag("grade-stage-" + tier.key)) {
        // 보관함 격자처럼 좁은 자리는 같은 그림의 1배 판 — 효과는 그대로, 풀어 두는 메모리는 1/4
        val small = constraints.maxWidth <= GradeSmallMaxPx
        Box(Modifier.fillMaxSize().background(GradeStageFill, GradeOctagon))
        GradeLayer(if (small) art.backSmall else art.back)
        if (showShoe) GradeShoe(sneaker, Modifier.fillMaxSize().gradeSlot(61f, 58f, 320f), animate = animate)
        (if (small) art.frontSmall else art.front)?.let { GradeLayer(it) }
        GradeLayer(if (small) art.frameSmall else art.frame)
    }
}

/**
 * 목록 칸 — 모바일 시안의 보유 신발처럼 프레임만 두르고 효과는 뺀다. 신발은 무대보다 조금 작게.
 * 두 줄 칸(폭 150dp 안팎)은 1배 프레임을, 한 줄로 넓어지면 2배 프레임을 쓴다 — 흐리지 않게, 칸마다 큰 그림을 풀지 않게.
 */
@Composable
fun SneakerGradeThumb(sneaker: Sneaker, modifier: Modifier = Modifier) {
    val tier = sneaker.tier
    val art = gradeArt(tier)
    BoxWithConstraints(modifier.aspectRatio(GradeArtRatio).testTag("grade-thumb-" + tier.key)) {
        val wide = constraints.maxWidth > GradeSmallMaxPx
        Box(Modifier.fillMaxSize().background(GradeThumbFill, GradeOctagon))
        GradeShoe(sneaker, Modifier.fillMaxSize().gradeSlot(69f, 69f, 296f))
        GradeLayer(if (wide) art.frame else art.frameSmall)
    }
}

/**
 * 프레임 속 신발 층. 이 앱에 그림이 없는 새 모델 번호면 다른 신발 그림(예전 52종 · 그린 신발)으로 채우지 않고
 * 빈 그림 표시만 둔다 — 보유 신발 상세 v1 시안 16과 같은 규칙.
 */
@Composable
private fun GradeShoe(sneaker: Sneaker, modifier: Modifier, animate: Boolean = false) {
    val model = sneaker.modelId
    if (model != null && shoeModelImageRes(model) == null) {
        Box(modifier.testTag("grade-art-missing"), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Image, contentDescription = null, tint = Color(0xFF7E8FA8), modifier = Modifier.fillMaxSize(0.26f))
        }
    } else {
        SneakerVisual(sneaker, modifier, animate = animate)
    }
}
