package com.stepup.android.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.background.Spots
import com.stepup.android.ui.background.TimeOfDay
import com.stepup.android.ui.background.rememberSpotPhoto
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.LocalFeedback
import com.stepup.android.ui.experience.LocalMotion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** 기울기 값(손가락 위치 −1..1 → 각도 · 빛 · 시차) */
private class Tilt {
    val rx = Animatable(0f); val ry = Animatable(0f); val gx = Animatable(.22f); val gy = Animatable(0f); val s = Animatable(1f)
}

/**
 * 신발 카드 — UI 최종 통합안(2026-10-25 compose/ShoeCard.kt). 둥근 가로 카드(361 : 236 · 모서리 22) 바탕은 장소 사진
 * (다크블루 = 밤, 화이트블루 = 낮) 5dp 흐림 + 유리 광택. 만지면 손가락 쪽으로 기울고 빛이 따라오며, 신발은 카드보다 더 움직여
 * 떠 보인다(시차). 놓으면 살짝 튕기며 제자리. 동작 줄이기면 기울지 않는다.
 *
 * 왼쪽 위 [tag](착용 중 · 등급) · 오른쪽 위 번호 · 왼쪽 아래 Lv — 이 셋은 카드 아래에서 다시 쓰지 않는다(HANDOFF shoes).
 * [onClick] 은 손을 거의 움직이지 않고 뗐을 때만(기울이며 끈 것은 누름이 아니다).
 */
@Composable
fun ShoeCard(
    shoe: Sneaker,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    clickLabel: String? = null,
) {
    val radius = 22.dp
    val dark = StepUpUi.colors.dark
    val density = LocalDensity.current
    val context = LocalContext.current
    val feedback = LocalFeedback.current
    val reduced = LocalMotion.current.reduced
    val t = remember { Tilt() }
    val click by androidx.compose.runtime.rememberUpdatedState(onClick)
    val follow = spring<Float>(dampingRatio = 1f, stiffness = 420f) // 누르는 동안: 바로 따라감
    val back = spring<Float>(dampingRatio = .55f, stiffness = Spring.StiffnessMediumLow) // 놓으면: 살짝 튕기며 제자리
    val ink = if (dark) Color.White else Color(0xFF0B1530)
    val place = remember(context) { Spots.byId(context, "mangwon") }
    val photo = rememberSpotPhoto(place, time = if (dark) TimeOfDay.NIGHT else TimeOfDay.DAY, widthDp = 361)

    Box(
        modifier.fillMaxWidth().aspectRatio(361f / 236f)
            .pointerInput(reduced) {
                coroutineScope {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val slop = viewConfiguration.touchSlop
                        var moved = false
                        fun aim(p: Offset) {
                            if (reduced) return
                            val nx = (p.x / size.width * 2 - 1).coerceIn(-1f, 1f)
                            val ny = (p.y / size.height * 2 - 1).coerceIn(-1f, 1f)
                            launch { t.ry.animateTo(nx * 14f, follow) }; launch { t.rx.animateTo(-ny * 12f, follow) }
                            launch { t.gx.animateTo(.5f + nx * .4f, follow) }; launch { t.gy.animateTo(.3f + ny * .4f, follow) }
                            launch { t.s.animateTo(.97f, follow) }
                        }
                        aim(down.position)
                        var up: Offset = down.position
                        do {
                            val e = awaitPointerEvent()
                            val c = e.changes.first()
                            up = c.position
                            if ((c.position - down.position).getDistance() > slop) moved = true
                            aim(c.position)
                            if (moved) c.consume()
                        } while (e.changes.any { it.pressed })
                        launch { t.ry.animateTo(0f, back) }; launch { t.rx.animateTo(0f, back) }
                        launch { t.gx.animateTo(.22f, back) }; launch { t.gy.animateTo(0f, back) }; launch { t.s.animateTo(1f, back) }
                        if (!moved && click != null && up.x in 0f..size.width.toFloat() && up.y in 0f..size.height.toFloat()) {
                            feedback?.play(FeedbackCue.Tap)
                            click?.invoke()
                        }
                    }
                }
            }
            .then(
                if (onClick != null) Modifier.semantics {
                    role = Role.Button
                    onClick(label = clickLabel) { onClick(); true }
                } else Modifier,
            )
            .graphicsLayer {
                rotationX = t.rx.value; rotationY = t.ry.value; scaleX = t.s.value; scaleY = t.s.value
                cameraDistance = 12f * density.density
            }
    ) {
        // 바탕: 장소 사진 흐림 + 아래로 어둡게(다크) / 밝게(화이트)
        Box(Modifier.matchParentSize().clip(RoundedCornerShape(radius)).background(if (dark) Color(0xFF2A3850) else Color(0xFFDCE6F6))) {
            if (photo != null) {
                Image(photo.painter, null, contentScale = ContentScale.Crop, colorFilter = photo.colorFilter,
                    modifier = Modifier.matchParentSize().blur(5.dp))
            }
            Box(
                Modifier.matchParentSize().background(
                    if (dark) Brush.verticalGradient(0f to Color(0x14080E1E), .55f to Color(0x2E080E1E), 1f to Color(0x8C080E1E))
                    else Brush.verticalGradient(0f to Color(0x57FFFFFF), .5f to Color(0x24FFFFFF), 1f to Color(0x99E8F0FF)),
                ),
            )
            // 유리 광택: 손가락 쪽 둥근 빛 + 위 반사 + 사선 띠
            Box(
                Modifier.matchParentSize().drawWithContent {
                    drawContent()
                    val c = Offset(size.width * t.gx.value, size.height * t.gy.value)
                    drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha = .30f), Color.Transparent), center = c, radius = size.maxDimension * .58f))
                    drawRect(Brush.verticalGradient(0f to Color.White.copy(alpha = .16f), .4f to Color.Transparent))
                    val shift = (t.gx.value - .5f) * size.width * 1.4f
                    drawRect(
                        Brush.linearGradient(
                            0f to Color.Transparent, .47f to Color.White.copy(alpha = .28f), .52f to Color.White.copy(alpha = .05f), .57f to Color.Transparent,
                            start = Offset(shift - size.width * .3f, 0f), end = Offset(shift + size.width * .3f, size.height),
                        ),
                    )
                },
            )
            Text(tag, color = ink, style = StepUpUi.type.smallButton.copy(fontSize = 13.sp),
                modifier = Modifier.align(Alignment.TopStart).padding(start = 18.dp, top = 16.dp).testTag("shoe-card-tag"))
            Text("#%04d".format(shoe.mintNumber), color = ink.copy(alpha = .8f),
                style = StepUpUi.type.numberRow.copy(fontSize = 15.sp),
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 18.dp, top = 15.dp).testTag("shoe-card-no"))
            Text("Lv.${shoe.level}", color = ink.copy(alpha = .78f), style = StepUpUi.type.caption.copy(fontSize = 12.5.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 18.dp, bottom = 16.dp).testTag("shoe-card-level"))
        }
        // 신발: 카드보다 더 움직여 떠 보임(시차). 보이는 부분만 칸에 맞춘다(비율 그대로)
        ShoeArtThumbnail(
            shoe, sample = ShoeArtFull,
            modifier = Modifier.align(Alignment.Center).padding(top = 6.dp).fillMaxWidth(.76f).fillMaxHeight(.66f)
                .graphicsLayer {
                    translationX = t.ry.value / 14f * 10f * density.density
                    translationY = -t.rx.value / 12f * 6f * density.density
                    rotationZ = -8f
                },
        )
    }
}
