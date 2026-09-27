package com.stepup.android.ui.screens.community.stories

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.StoryPlace
import com.stepup.android.ui.components.StepUpMap
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt
import kotlin.math.cos

/** 지도 위 장소 하나 — 그곳에 올라온(지금 보이는) 글 수 */
data class StoryPin(val place: StoryPlace, val count: Int, val meters: Double?)

/** [center] 둘레 [meters] 거리의 네 점 — 지도가 범위를 다 담게 맞출 때 쓴다 */
fun rangeBox(center: GeoPoint, meters: Int): List<GeoPoint> {
    val dLat = meters / 111_320.0
    val dLng = meters / (111_320.0 * cos(Math.toRadians(center.lat)).coerceAtLeast(0.01))
    return listOf(
        GeoPoint(center.lat + dLat, center.lng), GeoPoint(center.lat - dLat, center.lng),
        GeoPoint(center.lat, center.lng + dLng), GeoPoint(center.lat, center.lng - dLng),
    )
}

/**
 * 장소 핀 지도 — 장소마다 "이름 · 글 수" 알약, 기준점(내 위치 · 선택한 지역) 점.
 *
 * 쓴 사람의 실시간 위치는 없다. 핀은 글에 붙은 공개 장소이고, 점은 보는 사람의 기준점이다.
 * 알약을 누르면 [onPin], 빈 자리를 누르면(지도에서 고르기) [onTapMap].
 */
@Composable
fun StoryPinsMap(
    pins: List<StoryPin>,
    origin: GeoPoint?,
    originLabel: String,
    modifier: Modifier = Modifier,
    selectedKey: String? = null,
    interactive: Boolean = false,
    rangeMeters: Int? = null,
    extraFocus: List<GeoPoint> = emptyList(),
    picked: GeoPoint? = null,
    dotSeparator: Boolean = false,
    onPin: ((StoryPin) -> Unit)? = null,
    onTapMap: ((GeoPoint) -> Unit)? = null,
) {
    val measurer = rememberTextMeasurer()
    // 마지막으로 그린 알약 자리 — 누른 자리가 어느 핀인지 찾는다(그린 것과 같은 자리로)
    val hitBoxes = remember { mutableListOf<Pair<Rect, StoryPin>>() }
    val focus = buildList {
        origin?.let { add(it) }
        if (origin != null && rangeMeters != null) addAll(rangeBox(origin, rangeMeters))
        addAll(pins.map { it.place.point })
        addAll(extraFocus)
        // 지도에서 고른 자리는 맞춤에 넣지 않는다 — 누를 때마다 지도가 다시 맞춰 흔들리지 않게
    }.ifEmpty { listOfNotNull(picked) }
    val labelStyle = TextStyle(color = Snow, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    val originStyle = TextStyle(color = Snow.copy(alpha = 0.85f), fontSize = 10.sp)
    val pinFace = Color(0xFF0C1729).copy(alpha = 0.92f)
    val pinEdge = Color(0xFF6E8FD8).copy(alpha = 0.55f)
    StepUpMap(
        focus = focus,
        modifier = modifier,
        seed = 7,
        interactive = interactive,
        onTap = if (onPin == null && onTapMap == null) null else { at, plan ->
            val hit = hitBoxes.lastOrNull { (box, _) -> box.inflate(8f).contains(at) }?.second
            if (hit != null) onPin?.invoke(hit) else onTapMap?.invoke(plan.fromScreen(at))
        },
    ) { plan ->
        hitBoxes.clear()
        // 기준점 — 파란 점과 흰 테, 옆에 "내 위치"
        origin?.let { point ->
            val at = plan.toScreen(point)
            drawCircle(Color.White, radius = 7.dp.toPx(), center = at)
            drawCircle(Volt, radius = 5.dp.toPx(), center = at)
            val label = measurer.measure(originLabel, originStyle)
            drawText(label, topLeft = Offset(at.x + 10.dp.toPx(), at.y - label.size.height / 2f))
        }
        // 고른 자리 — 지도에서 직접 고를 때
        picked?.let { point ->
            val at = plan.toScreen(point)
            drawCircle(Volt.copy(alpha = 0.25f), radius = 16.dp.toPx(), center = at)
            drawCircle(Volt, radius = 7.dp.toPx(), center = at)
            drawCircle(Color.White, radius = 7.dp.toPx(), center = at, style = Stroke(2.dp.toPx()))
        }
        pins.forEach { pin ->
            val at = plan.toScreen(pin.place.point)
            val text = if (pin.count > 0) {
                if (dotSeparator) "${pin.place.name} · ${pin.count}" else "${pin.place.name}  ${pin.count}"
            } else {
                pin.place.name
            }
            val measured = measurer.measure(text, labelStyle)
            val padX = 10.dp.toPx()
            val padY = 6.dp.toPx()
            val size = Size(measured.size.width + padX * 2, measured.size.height + padY * 2)
            val topLeft = Offset(at.x - size.width / 2f, at.y - size.height / 2f)
            val selected = pin.place.key == selectedKey
            val radius = CornerRadius(size.height / 2f, size.height / 2f)
            drawRoundRect(if (selected) Volt else pinFace, topLeft = topLeft, size = size, cornerRadius = radius)
            drawRoundRect(if (selected) Color.White.copy(alpha = 0.6f) else pinEdge, topLeft = topLeft, size = size,
                cornerRadius = radius, style = Stroke(1.dp.toPx()))
            drawText(measured, topLeft = Offset(topLeft.x + padX, topLeft.y + padY))
            hitBoxes += Rect(topLeft, size) to pin
        }
    }
}
