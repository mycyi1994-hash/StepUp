package com.stepup.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.AvatarArt
import com.stepup.android.domain.AvatarArtCatalog
import com.stepup.android.domain.AvatarGender
import com.stepup.android.domain.AvatarLook
import com.stepup.android.domain.AvatarPose
import com.stepup.android.domain.AvatarRender
import com.stepup.android.domain.Outfits
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.ui.theme.Carbon
import com.stepup.android.ui.theme.Cyan
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.Night
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * 러너 캐릭터 — 디자인 패키지의 완성 그림을 **그대로** 띄운다.
 *
 * 캐릭터를 코드로 다시 그리지 않는다. 원·타원·막대로 만든 캐릭터는 받은
 * 그림의 입체감·재질을 흉내 낼 수 없고, 두 캐릭터가 화면마다 조금씩 다른
 * 사람처럼 보이게 된다. 코드가 그리는 것은 캐릭터 **뒤**의 조명과 바닥뿐이다.
 *
 * 그림은 ContentScale.Fit 이다 — 비율을 지키고, 모자와 신발 끝이 잘리지
 * 않는다. 어떤 그림을 쓸지와, 그 그림이 실제 착장과 맞는지는
 * [AvatarArtCatalog] 가 정한다.
 *
 * 캐릭터 그림 한 장 — 배경 없이.
 *
 * 작은 자리(성별 카드 · 내 정보 머리)에 쓴다. 전신을 Fit 으로 넣는다.
 */
@Composable
fun AvatarImage(
    art: AvatarArt,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val res = art.drawableRes() ?: return
    Image(
        painter = painterResource(res),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        alignment = Alignment.BottomCenter,
        modifier = modifier,
    )
}

/**
 * 실제 러닝 중 기본 착장에 한해서 보폭 프레임을 바꾼다. 다른 장비를 착용한
 * 캐릭터는 해당 장비가 그려진 기존 이미지를 유지한다.
 */
@Composable
fun RunningAvatarImage(
    look: AvatarLook,
    render: AvatarRender,
    running: Boolean,
    stridePhase: State<Float>?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    AvatarImage(render.art, modifier, contentDescription)
}

/**
 * 캐릭터 무대 — 뒤의 푸른 조명, 낮은 도시 실루엣, 바닥의 타원 무대와 접지
 * 그림자, 그 위의 캐릭터 그림.
 *
 * 도시는 사진이 아니라 저대비 사각형 실루엣이다. 패키지에 도시 배경이 없고,
 * 출처를 모르는 사진을 가져다 쓰지 않는다.
 *
 * @param characterFraction 무대 높이 중 캐릭터가 차지하는 비율
 * @param overlay 무대 위에 얹을 표시(착장 안내 등). 어떤 그림이 골라졌는지를 받는다.
 */
@Composable
fun CharacterStage(
    look: AvatarLook,
    pose: AvatarPose,
    modifier: Modifier = Modifier,
    characterFraction: Float = 0.88f,
    skyline: Boolean = true,
    animate: Boolean = true,
    contentDescription: String? = null,
    overlay: @Composable BoxScope.(AvatarRender) -> Unit = {},
) {
    val render = AvatarArtCatalog.resolve(look, pose)
    if (avatarArtResOrNull(render.art.key) == null) return
    val running = pose == AvatarPose.RUN
    // 모션 줄이기면 멈춘다
    val phase = if (animate && LocalMotion.current.decorative) {
        ambientPhase(if (running) 760 else 2800, reverse = !running)
    } else {
        null
    }
    val glow = Volt
    val cyan = Cyan
    val building = Carbon
    val window = Edge

    BoxWithConstraints(
        modifier = modifier.then(
            if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier,
        ),
    ) {
        val stageH = maxHeight
        val geometry = render.art.geometry()
        val imageHeight = minOf(stageH * characterFraction, maxWidth / geometry.aspectRatio)
        Canvas(Modifier.fillMaxSize()) {
            if (skyline) drawSkyline(building, window)
            drawBackGlow(glow, cyan)
            val p = phase?.value ?: 0.5f
            val lift = if (running && phase != null) abs(sin(p.toDouble() * 2.0 * PI)).toFloat() else p
            drawFloor(glow, cyan, lift)
        }
        // 발이 타원 무대의 한가운데에 닿도록 바닥에서 조금 띄운다
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = stageH * FLOOR_GAP)
                .fillMaxWidth()
                .height(stageH * characterFraction)
                .graphicsLayer {
                    val p = phase?.value ?: 0.5f
                    val stride = if (running && phase != null) sin(p.toDouble() * 2.0 * PI).toFloat() else 0f
                    // Fit includes transparent pixels below the soles. Anchor the visible feet,
                    // not the source rectangle, to the shared floor on every screen.
                    translationY = (imageHeight * geometry.bottomInsetFraction).toPx() +
                        if (running) -abs(stride) * 4.dp.toPx()
                        else (p - 0.5f) * 1.dp.toPx()
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    if (!running) {
                        rotationZ = (p - 0.5f) * 1.1f
                        scaleY = 0.995f + p * 0.01f
                    } else {
                        rotationZ = stride * 0.5f
                    }
                },
        ) {
            RunningAvatarImage(
                look = look, render = render, running = running, stridePhase = phase,
                modifier = Modifier.fillMaxSize(),
            )
        }
        overlay(render)
    }
}

/** 무대 바닥에서 캐릭터 발끝까지 — 무대 높이 대비 */
private const val FLOOR_GAP = 0.045f

/** 무대 타원의 세로 중심 — 무대 높이 대비 */
private const val FLOOR_Y = 0.955f

private fun DrawScope.drawSkyline(building: Color, window: Color) {
    // (가로 시작, 폭, 높이) — 무대 비율. 고정값이라 매번 같은 도시다.
    val blocks = listOf(
        Triple(0.00f, 0.10f, 0.42f), Triple(0.09f, 0.07f, 0.58f), Triple(0.15f, 0.09f, 0.36f),
        Triple(0.23f, 0.06f, 0.50f), Triple(0.70f, 0.07f, 0.47f), Triple(0.76f, 0.09f, 0.62f),
        Triple(0.84f, 0.06f, 0.40f), Triple(0.89f, 0.11f, 0.54f),
    )
    val baseY = size.height * FLOOR_Y
    blocks.forEach { (x, w, h) ->
        val left = size.width * x
        val width = size.width * w
        val top = baseY - size.height * h
        drawRect(
            brush = Brush.verticalGradient(
                0f to building.copy(alpha = 0.95f),
                1f to building.copy(alpha = 0.0f),
                startY = top,
                endY = baseY,
            ),
            topLeft = Offset(left, top),
            size = Size(width, baseY - top),
        )
        // 창문 몇 개 — 아주 흐리게
        val cols = 2
        val rows = (h * 10).toInt()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if ((r * 3 + c + (x * 100).toInt()) % 4 != 0) continue
                val wx = left + width * (0.22f + c * 0.40f)
                val wy = top + size.height * (0.04f + r * 0.055f)
                if (wy > baseY - size.height * 0.12f) continue
                drawRect(
                    color = window.copy(alpha = 0.55f),
                    topLeft = Offset(wx, wy),
                    size = Size(width * 0.16f, size.height * 0.018f),
                )
            }
        }
    }
}

private fun DrawScope.drawBackGlow(glow: Color, cyan: Color) {
    val center = Offset(size.width / 2f, size.height * 0.52f)
    val radius = maxOf(size.minDimension * 0.62f, 1f)
    drawCircle(
        brush = Brush.radialGradient(
            0f to glow.copy(alpha = 0.34f),
            0.45f to glow.copy(alpha = 0.12f),
            1f to Color.Transparent,
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
    drawCircle(
        brush = Brush.radialGradient(
            0f to cyan.copy(alpha = 0.10f),
            1f to Color.Transparent,
            center = center,
            radius = radius * 0.55f,
        ),
        radius = radius * 0.55f,
        center = center,
    )
}

private fun DrawScope.drawFloor(glow: Color, cyan: Color, phase: Float) {
    val cy = size.height * FLOOR_Y
    val ringW = size.width * 0.68f
    val ringH = size.height * 0.09f
    // 접지 그림자
    val shadowW = ringW * (0.56f + (1f - phase) * 0.04f)
    val shadowH = ringH * 0.48f
    drawOval(
        brush = Brush.radialGradient(
            0f to Color.Black.copy(alpha = 0.76f - phase * 0.06f),
            1f to Color.Transparent,
            center = Offset(size.width / 2f, cy),
            radius = shadowW / 2f,
        ),
        topLeft = Offset((size.width - shadowW) / 2f, cy - shadowH / 2f),
        size = Size(shadowW, shadowH),
    )
    // 타원 무대 — 넓고 흐린 번짐 위에 가는 선
    val topLeft = Offset((size.width - ringW) / 2f, cy - ringH / 2f)
    drawOval(color = glow.copy(alpha = 0.10f), topLeft = topLeft, size = Size(ringW, ringH), style = Stroke(width = 16.dp.toPx()))
    drawOval(color = glow.copy(alpha = 0.22f), topLeft = topLeft, size = Size(ringW, ringH), style = Stroke(width = 6.dp.toPx()))
    drawOval(color = glow.copy(alpha = 0.95f), topLeft = topLeft, size = Size(ringW, ringH), style = Stroke(width = 2.dp.toPx()))
    val innerW = ringW * 0.72f
    val innerH = ringH * 0.62f
    drawOval(
        color = cyan.copy(alpha = 0.35f),
        topLeft = Offset((size.width - innerW) / 2f, cy - innerH / 2f),
        size = Size(innerW, innerH),
        style = Stroke(width = 1.dp.toPx()),
    )
}

/**
 * 착장 안내 — 그림 속 착장이 실제 착장과 다를 때만 보인다.
 *
 * 예: 클라우드 러너를 신고 있지만 캐릭터 그림은 기본 운동화다. 그 사실을
 * 감추지 않고, 실제로 신은 신발의 그림을 옆에 작게 둔다.
 */
@Composable
fun AvatarLookNote(
    look: AvatarLook,
    render: AvatarRender,
    modifier: Modifier = Modifier,
) {
    if (avatarArtResOrNull(render.art.key) == null || render.lookShown) return
    val shape = RoundedCornerShape(12.dp)
    val outfitName = stringResource(outfitNameRes(look.outfit))
    val shoe = look.shoe
    val shoeName = shoe?.variantLabel()
    val wearing = when {
        !render.outfitShown && !render.shoeShown && shoeName != null -> "$outfitName · $shoeName"
        !render.shoeShown && shoeName != null -> shoeName
        else -> outfitName
    }
    Row(
        modifier = modifier
            .clip(shape)
            .background(Night.copy(alpha = 0.82f), shape)
            .border(1.dp, Edge, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (shoe != null && !render.shoeShown) {
            SneakerFrame(sneaker = shoe, modifier = Modifier.size(width = 34.dp, height = 22.dp))
        }
        Column {
            Text(
                text = stringResource(R.string.avatar_art_wearing, wearing),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Snow,
            )
            Text(
                text = stringResource(
                    when {
                        !render.shoeShown && render.art.shoeCode != null -> R.string.wardrobe_shoes_preview_pending
                        !render.outfitShown && !render.shoeShown -> R.string.avatar_art_base_look
                        !render.shoeShown -> R.string.avatar_art_base_shoes
                        else -> R.string.avatar_art_base_outfit
                    },
                ),
                fontSize = 13.sp,
                color = Silver,
            )
        }
    }
}
