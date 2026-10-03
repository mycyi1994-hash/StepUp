package com.stepup.android.ui.components

import com.stepup.android.ui.theme.Silver
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.drawText
import androidx.compose.material3.Text
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.stepup.android.domain.GeoPoint
import com.stepup.android.ui.theme.Night
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.tan
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * 진짜 지도 위의 러닝 경로.
 *
 * 지도 타일([MapTiles] — MapTiler, 데이터는 OpenStreetMap)을 깔고 그 위에 볼트 네온 경로를 얹는다. 내가 달린
 * 길이 실제로 어느 도로였는지, 어느 공원을 돌았는지가 그대로 보인다.
 *
 * 타일은 원본이 밝은 지도라 그대로 쓰면 순블랙 테마와 부딪힌다. 색을 반전시켜
 * 어두운 지도로 바꾸고 채도를 죽여, 네온 경로만 화면에서 튀어나오게 한다.
 *
 * 의사 도로망([drawStreets])은 **항상 먼저 깔린다.** 타일이 도착하는 대로 그
 * 위를 덮으므로, 로딩 중에도 타일이 빠진 자리에도 검은 구멍이 생기지 않는다.
 */
@Composable
fun LiveRouteMap(
    points: List<GeoPoint>,
    modifier: Modifier = Modifier,
    /** 타일이 없는 자리에 깔 의사 도로망의 시드. 세션 내내 고정된 값을 넘겨야 한다. */
    seed: Int = 0,
    /** 0..1 — 경로 위 진행 지점에 러너 점을 찍는다. null이면 표시하지 않음 */
    progress: Float? = null,
    /**
     * 손가락으로 확대·축소·이동할 수 있게 한다.
     *
     * 목록 안에 들어가는 지도는 끄는 편이 낫다 — 지도를 잡으려다 목록이
     * 안 넘어가면 그게 더 답답하다. 러닝 화면처럼 지도가 주인공인 자리에서만 켠다.
     */
    interactive: Boolean = false,
    /** 같이 뛰는 사람(S2 시안 14) — 위치를 보이기로 한 사람만 넘어온다. 이름 첫 글자를 원 안에 쓴다. */
    others: List<Pair<GeoPoint, String>> = emptyList(),
    /** [interactive] 지도의 버튼 이름 — 화면 낭독이 읽는다. 없으면 예전처럼 그림만 */
    controlLabels: MapControlLabels? = null,
    /**
     * 달리는 중 — 경로 전체가 아니라 지금 자리(경로의 끝)를 한가운데 두고 가까이([MapTiles.FOLLOW_ZOOMS]) 따라간다.
     * 경로 전체에 맞추면 달릴수록 지도가 멀어져 지금 달리는 길이 점처럼 작아진다. 같이 뛰는 사람은 가까우면 함께 들어온다.
     */
    follow: Boolean = false,
    /** [follow] 일 때 지금 자리를 둘 높이(위에서 0..1) — 지도 중 화면에 보이는 쪽 가운데([followAnchor]) */
    followAt: Float = 0.5f,
    /** 위치 신호가 끊겼다 이어진 점 번호(RunTrack.segmentBreaks) — 그 앞 점과 잇지 않는다 */
    breaks: Set<Int> = emptySet(),
    /** 고른 코스 — 남은 안내 경로를 흐린 점선으로 먼저 깐다(달린 길과 구분) */
    course: List<GeoPoint> = emptyList(),
    /** 경로 색 — 러닝 화면은 시안(유효 경로) */
    routeColor: Color = Volt,
    /** 끝점을 깃발 대신 지금 자리 점(흰 테 파란 점)으로 — 달리는 중 */
    live: Boolean = false,
    /** 타일 색 — 러닝 화면은 [RunTone.mapFilter](남색 시안, [LocalMapTone]). null 이면 앱 테마의 지도 색 */
    tileFilter: ColorFilter? = LocalMapTone.current.filter,
    tileShade: Float = LocalMapTone.current.shade,
    /** 출발 · 도착 점 옆 작은 이름표("시작" · "도착", 지난 기록의 경로 확대 H12) — null 이면 두지 않는다 */
    endpointLabels: Pair<String, String>? = null,
) {
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val around = others.map { it.first }
    StepUpMap(
        focus = if (follow && points.isNotEmpty()) followFrame(points.last(), around) else points + around + course,
        modifier = modifier,
        seed = seed,
        interactive = interactive,
        controlLabels = controlLabels,
        zooms = if (follow) MapTiles.FOLLOW_ZOOMS else MapTiles.MIN_ZOOM..MapTiles.MAX_ZOOM,
        anchorY = if (follow) followAt else 0.5f,
        tileFilter = tileFilter,
        tileShade = tileShade,
    ) { plan ->
        if (course.size >= 2) drawCourseGuide(plan, course)
        drawRoute(plan, points, progress, routeColor, breaks, live)
        if (endpointLabels != null && points.size >= 2) {
            val start = plan.toScreen(points.first())
            val end = plan.toScreen(points.last())
            drawEndpointChip(measurer, start, endpointLabels.first, Color(0xFF48D9FA), below = true)
            // 한 바퀴 돌아 같은 자리에서 끝나면 도착 이름표는 위로 — 시작 이름표와 겹치지 않게
            drawEndpointChip(measurer, end, endpointLabels.second, Color(0xFFFF5C6B), below = false,
                above = (end - start).getDistance() < 36.dp.toPx())
        }
        others.forEach { (point, name) ->
            val at = plan.toScreen(point)
            drawCircle(Color(0xFF05080E), radius = 11.dp.toPx(), center = at)
            drawCircle(Color(0xFFF3F5FF), radius = 9.5f.dp.toPx(), center = at)
            val label = measurer.measure(
                // 이모지 · 확장 한자처럼 두 글자 단위(서로게이트)로 된 첫 글자를 반으로 자르지 않는다
                name.trim().takeIf { it.isNotEmpty() }?.let { String(Character.toChars(it.codePointAt(0))) } ?: "?",
                androidx.compose.ui.text.TextStyle(color = Color(0xFF070B12), fontSize = 10.sp),
            )
            drawText(label, topLeft = Offset(at.x - label.size.width / 2f, at.y - label.size.height / 2f))
        }
    }
}

/**
 * 지금 자리를 한가운데 두는 틀 — 곁에 둘 점([around])마다 지금 자리 건너편에 거울 점을 하나씩 더한다.
 * 지도는 틀의 가운데를 화면 가운데 두므로 지금 자리가 늘 가운데 오고, 곁의 점도 함께 들어온다.
 */
internal fun followFrame(here: GeoPoint, around: List<GeoPoint>): List<GeoPoint> =
    listOf(here) + around.flatMap { listOf(it, mirror(it, here)) }

/**
 * [point] 를 [center] 건너편 같은 거리로 — 지도(메르카토르) 위에서. 위도는 지도에서 고르게 늘지 않아
 * 도(°)로 그냥 빼면 멀리 있는 점일수록 가운데가 비낀다(20km 에서 화면 10px).
 */
private fun mirror(point: GeoPoint, center: GeoPoint): GeoPoint {
    fun y(lat: Double) = ln(tan(PI / 4 + Math.toRadians(lat.coerceIn(-85.0, 85.0)) / 2))
    val mirrored = 2 * y(center.lat) - y(point.lat)
    return GeoPoint(lat = Math.toDegrees(2 * atan(exp(mirrored)) - PI / 2), lng = 2 * center.lng - point.lng)
}

/**
 * 따라가는 지도에서 지금 자리를 둘 높이(위에서 0..1).
 *
 * 러닝 화면의 지도는 아래가 버튼 뒤로 잘린 채 보이는 일이 많다(지도 가운데가 딱 잘리는 선). 지도 중 화면에 보이는 범위
 * [seenTop]..[seenBottom] 에서 위 · 아래가 바닥색에 녹아드는 띠를 빼고([clearTop]..[clearBottom] 만) 그 가운데에 둔다.
 * 스크롤할 때마다 지도가 흔들리지 않게 0.05 단위로 끊는다.
 */
internal fun followAnchor(seenTop: Float, seenBottom: Float, clearTop: Float, clearBottom: Float): Float {
    val top = seenTop.coerceIn(0f, 1f)
    val bottom = seenBottom.coerceIn(0f, 1f)
    val lo = maxOf(top, clearTop)
    val hi = minOf(bottom, clearBottom)
    val middle = if (hi > lo) (lo + hi) / 2 else (top + bottom) / 2
    return ((middle * 20).roundToInt() / 20f).coerceIn(0.2f, 0.8f)
}

/** 고른 코스의 안내 선 — 흐린 점선(달린 길보다 아래, 시안 K04 의 남은 경로) */
private fun DrawScope.drawCourseGuide(plan: TilePlan, course: List<GeoPoint>) {
    val screen = course.map { plan.toScreen(it) }
    val path = Path().apply {
        moveTo(screen.first().x, screen.first().y)
        for (i in 1 until screen.size) lineTo(screen[i].x, screen[i].y)
    }
    drawPath(
        path, color = Color(0xFFB9CCEB).copy(alpha = 0.55f),
        style = Stroke(
            width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx())),
        ),
    )
}

/** 경로 한 줄 — 글로우 · 본선 · 출발점 · 도착 깃발(달리는 중이면 지금 자리 점) · 진행 점. [breaks] 앞에서는 끊는다 */
private fun DrawScope.drawRoute(
    plan: TilePlan,
    points: List<GeoPoint>,
    progress: Float?,
    color: Color = Volt,
    breaks: Set<Int> = emptySet(),
    live: Boolean = false,
) {
    if (points.isEmpty()) return
    if (points.size < 2) {
        val at = plan.toScreen(points.first())
        if (live) drawHereDot(at) else {
            drawCircle(color.copy(alpha = 0.30f), radius = 9.dp.toPx(), center = at)
            drawCircle(color, radius = 4.5f.dp.toPx(), center = at)
        }
        return
    }

    val screen = points.map { plan.toScreen(it) }
    val path = Path().apply {
        moveTo(screen.first().x, screen.first().y)
        for (i in 1 until screen.size) {
            if (i in breaks) moveTo(screen[i].x, screen[i].y) else lineTo(screen[i].x, screen[i].y)
        }
    }

    // 글로우(넓고 옅게) → 본선(가늘고 진하게)
    drawPath(
        path,
        color = color.copy(alpha = 0.22f),
        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    drawPath(
        path,
        brush = Brush.linearGradient(listOf(color.copy(alpha = 0.85f), color)),
        style = Stroke(width = 3.5f.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    drawCircle(color.copy(alpha = 0.28f), radius = 8.dp.toPx(), center = screen.first())
    drawCircle(color, radius = 4.5f.dp.toPx(), center = screen.first())
    drawCircle(Color.White, radius = 2.dp.toPx(), center = screen.first())
    if (live) drawHereDot(screen.last()) else drawRouteFlag(screen.last())

    progress?.let { f ->
        val at = pointAlongRoute(screen, f.coerceIn(0f, 1f))
        drawCircle(color.copy(alpha = 0.30f), radius = 9.dp.toPx(), center = at)
        drawCircle(color, radius = 4.5f.dp.toPx(), center = at)
        drawCircle(Color.White, radius = 2.dp.toPx(), center = at)
    }
}

/** 출발 · 도착 이름표 — 짙은 남색 면 · 색 테두리 · 흰 글자. 지도 밖으로 나가지 않게 안쪽으로 당긴다 */
private fun DrawScope.drawEndpointChip(
    measurer: androidx.compose.ui.text.TextMeasurer,
    at: Offset,
    text: String,
    edge: Color,
    below: Boolean,
    above: Boolean = false,
) {
    val label = measurer.measure(
        text,
        androidx.compose.ui.text.TextStyle(
            color = Color.White, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        ),
    )
    val padX = 8.dp.toPx()
    val padY = 3.dp.toPx()
    val w = label.size.width + padX * 2
    val h = label.size.height + padY * 2
    val gap = 12.dp.toPx()
    val margin = 4.dp.toPx()
    var x: Float
    var y: Float
    when {
        below -> { x = at.x - w / 2; y = at.y + gap }
        above -> { x = at.x - w / 2; y = at.y - gap - h }
        else -> {
            x = at.x + gap
            y = at.y - h / 2
            if (x + w > size.width - margin) x = at.x - gap - w
        }
    }
    x = x.coerceIn(margin, (size.width - w - margin).coerceAtLeast(margin))
    y = y.coerceIn(margin, (size.height - h - margin).coerceAtLeast(margin))
    val radius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
    val box = androidx.compose.ui.geometry.Size(w, h)
    drawRoundRect(Color(0xE6071C38), topLeft = Offset(x, y), size = box, cornerRadius = radius)
    drawRoundRect(edge, topLeft = Offset(x, y), size = box, cornerRadius = radius, style = Stroke(1.5.dp.toPx()))
    drawText(label, topLeft = Offset(x + padX, y + padY))
}

/** 지금 자리 — 흰 테 두른 파란 점과 옅은 빛 */
private fun DrawScope.drawHereDot(at: Offset) {
    drawCircle(Color(0xFF0754FF).copy(alpha = 0.25f), radius = 16.dp.toPx(), center = at)
    drawCircle(Color.White, radius = 9.dp.toPx(), center = at)
    drawCircle(Color(0xFF0754FF), radius = 6.5f.dp.toPx(), center = at)
}

/** 지도 타일 색 — 러닝 화면의 지도 틀([RunMapFrame])이 남색 시안 색으로 바꿔 넣는다. 지도 내용(길 · 물 · 이름)은 그대로다 */
@androidx.compose.runtime.Immutable
data class MapTone(val filter: ColorFilter? = null, val shade: Float = 0.30f)

val LocalMapTone = androidx.compose.runtime.staticCompositionLocalOf { MapTone() }

/**
 * 지도 한 칸 — 타일을 깔고 그 위에 [overlay] 를 그린다.
 *
 * 러닝 경로, 코스·번개 핀, 땅따먹기 칸, 기록 히트맵이 모두 이 부품 위에 그려진다.
 * 지도 엔진을 바꾸게 되면(예: 한국만 다른 지도) 이 안쪽만 갈아 끼우면 된다.
 *
 * @param focus 처음 화면에 다 들어오게 맞출 좌표들. 비어 있으면 지도를 그리지 않는다.
 * @param onTap 지도를 눌렀을 때 — 누른 화면 위치와 그때의 배치 계획
 * @param onViewport 보이는 범위가 바뀔 때 — (최소 위도, 최소 경도, 최대 위도, 최대 경도)
 * @param zooms [focus] 에 맞춘 줌이 들어갈 범위 — 사용자가 확대 · 축소하기 전
 * @param anchorY [focus] 의 가운데를 둘 높이(위에서 0..1)
 */
@Composable
fun StepUpMap(
    focus: List<GeoPoint>,
    modifier: Modifier = Modifier,
    seed: Int = 0,
    interactive: Boolean = false,
    onTap: ((Offset, TilePlan) -> Unit)? = null,
    onViewport: ((Double, Double, Double, Double) -> Unit)? = null,
    controlLabels: MapControlLabels? = null,
    zooms: IntRange = MapTiles.MIN_ZOOM..MapTiles.MAX_ZOOM,
    anchorY: Float = 0.5f,
    /** 타일 색 — null 이면 앱 테마의 지도 색(러닝 화면의 지도 틀은 [LocalMapTone] 으로 남색 시안 색을 넣는다) */
    tileFilter: ColorFilter? = LocalMapTone.current.filter,
    /** 타일 위에 덮는 바탕색의 짙기 */
    tileShade: Float = LocalMapTone.current.shade,
    overlay: DrawScope.(TilePlan) -> Unit = {},
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density

    // 사용자가 손가락으로 만든 줌·이동. 경로에 맞춘 기본값에서 얼마나
    // 벗어났는지를 담는다 — 절대 위치가 아니라 차이로 두면, 달리는 동안
    // 경로가 늘어나도 보고 있던 자리가 유지된다.
    var zoomDelta by rememberSaveable { mutableIntStateOf(0) }
    var panX by rememberSaveable { mutableStateOf(0.0) }
    var panY by rememberSaveable { mutableStateOf(0.0) }
    // 손가락을 오므렸다 폈다 하는 동안 쌓이는 배율. 일정 선을 넘으면
    // 줌 한 단계로 환산한다. 타일은 정수 줌으로만 존재하기 때문이다.
    var pinch by remember { mutableFloatStateOf(1f) }
    val moved = zoomDelta != 0 || panX != 0.0 || panY != 0.0

    // 타일은 256px 통째로 그려 칸 밖(위아래 · 좌우)으로 넘친다. Compose 는 그리기를 칸에 자르지 않으므로
    // 여기서 자른다 — 안 자르면 지도가 위의 글자 탭 · 머리를 덮는다(러닝 이야기 "지도 보기").
    BoxWithConstraints(modifier.clipToBounds()) {
        val widthPx = if (constraints.hasBoundedWidth) constraints.maxWidth else 0
        val heightPx = if (constraints.hasBoundedHeight) constraints.maxHeight else 0

        val plan = remember(focus, widthPx, heightPx, density, zoomDelta, panX, panY, zooms, anchorY) {
            if (focus.isEmpty() || widthPx <= 0 || heightPx <= 0) null
            else TilePlan.of(focus, widthPx, heightPx, density, zoomDelta, panX, panY, zooms, anchorY)
        }
        val currentPlan by rememberUpdatedState(plan)
        val currentOnTap by rememberUpdatedState(onTap)
        val currentOnViewport by rememberUpdatedState(onViewport)

        // 타일이 한 장 도착할 때마다 올라가는 카운터. Canvas가 이 값을 읽어
        // 스냅샷 의존성을 만들어 두므로, 도착이 곧 다시 그리기가 된다.
        var arrivals by remember { mutableIntStateOf(0) }

        // 달리는 동안 좌표는 계속 늘어나지만 대개 같은 타일 안이다. 효과를 좌표가
        // 아니라 **타일 범위**에 묶어, 몇 초마다 전체를 다시 받는 일을 막는다.
        val tileKey = plan?.rangeKey.orEmpty()

        LaunchedEffect(tileKey) {
            val current = plan ?: return@LaunchedEffect
            currentOnViewport?.let { report ->
                val (min, max) = current.bounds(widthPx, heightPx)
                report(min.lat, min.lng, max.lat, max.lng)
            }
            val wanted = buildList {
                for (ty in current.minTileY..current.maxTileY) {
                    for (tx in current.minTileX..current.maxTileX) {
                        add(current.wrapX(tx) to ty)
                    }
                }
            }.take(MapTiles.MAX_TILES)

            // 순차로 받으면 타임아웃 하나에 지도 전체가 멈춘다. 4개씩 병렬로 —
            // 타일 서버에 무리를 주지 않으면서 체감이 확 달라진다.
            for (chunk in wanted.chunked(4)) {
                coroutineScope {
                    chunk.map { (tx, ty) ->
                        async { MapTiles.load(context, current.zoom, tx, ty) }
                    }.awaitAll()
                }
                arrivals++
            }
        }

        val scale = (density / 2f).coerceIn(1f, 2f)

        Canvas(
            Modifier
                .fillMaxSize()
                .then(
                    if (!interactive) {
                        Modifier
                    } else {
                        Modifier.pointerInput(Unit) {
                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                // 지도가 손가락을 따라간다 — 종이 지도를 손으로
                                // 밀듯이. 손가락을 오른쪽으로 끌면 지도도 오른쪽으로
                                // 따라오고, 그만큼 시야는 왼쪽 땅으로 옮겨 간다.
                                //
                                // 반대로 두면(끄는 방향과 지도가 반대로 움직이면)
                                // 한 번에 원하는 쪽으로 못 가고 매번 두 번씩 끌게 된다.
                                //
                                // 끈 거리는 화면 픽셀이다. 타일 좌표계로 옮겨야
                                // 줌이 바뀌어도 손가락과 지도가 같은 만큼 움직인다.
                                val drawn = currentPlan?.scale ?: scale
                                panX += pan.x / drawn
                                panY += pan.y / drawn

                                pinch *= gestureZoom
                                // 타일은 정수 줌만 있다. 2배쯤 벌리면 한 단계 올린다.
                                while (pinch > 1.8f && zoomDelta < MAX_USER_ZOOM_IN) {
                                    zoomDelta++
                                    pinch /= 2f
                                    // 줌이 한 단계 오르면 세계 좌표가 두 배가 된다.
                                    // 보고 있던 자리를 유지하려면 이동량도 같이 키운다.
                                    panX *= 2
                                    panY *= 2
                                }
                                while (pinch < 0.55f && zoomDelta > MAX_USER_ZOOM_OUT) {
                                    zoomDelta--
                                    pinch *= 2f
                                    panX /= 2
                                    panY /= 2
                                }
                            }
                        }
                    },
                )
                .then(
                    if (onTap == null) {
                        Modifier
                    } else {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures { at ->
                                val plan = currentPlan ?: return@detectTapGestures
                                currentOnTap?.invoke(at, plan)
                            }
                        }
                    },
                ),
        ) {
            // arrivals를 읽어야 타일 도착이 다시 그리기로 이어진다
            val revision = arrivals

            // Unloaded tiles show only an abstract grid. Never imply invented streets
            // are real location data when the tile provider is unavailable.
            drawStreets(seed)
            val drawn = if (plan != null && revision >= 0) drawTiles(plan, tileFilter ?: StepUpColors.mapFilter) else 0
            if (drawn > 0 && tileShade > 0f) {
                // 지도를 한 겹 눌러(또는 띄워) 카드 배경과 붙인다.
                // Night 는 테마의 바닥색이라 밝은 테마에서는 흰 막, 어두운
                // 테마에서는 검은 막이 된다.
                drawRect(Night.copy(alpha = tileShade))
            }

            if (plan == null) return@Canvas
            overlay(plan)
        }

        if (interactive) {
            // ± 버튼. 손가락 두 개를 못 쓰는 상황(장갑, 한 손)이 러닝 중에는 흔하다.
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MapButton(Icons.Filled.Add, label = controlLabels?.zoomIn) {
                    if (zoomDelta < MAX_USER_ZOOM_IN) {
                        zoomDelta++
                        panX *= 2
                        panY *= 2
                    }
                }
                MapButton(Icons.Filled.Remove, label = controlLabels?.zoomOut) {
                    if (zoomDelta > MAX_USER_ZOOM_OUT) {
                        zoomDelta--
                        panX /= 2
                        panY /= 2
                    }
                }
            }

            // 지도를 옮겨 놓고 나면 경로를 다시 찾기 어렵다. 되돌아갈 자리를 준다.
            if (moved) {
                MapButton(
                    icon = Icons.Filled.MyLocation,
                    label = controlLabels?.recenter,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp),
                ) {
                    zoomDelta = 0
                    panX = 0.0
                    panY = 0.0
                    pinch = 1f
                }
            }
        }

        // 지도 출처. 타일 서비스가 요구하고, 지도를 쓰는 앱이 지켜야 할 최소 예의다.
        // 경로를 가리지 않게 구석에 작고 흐리게 둔다.
        if (plan != null) {
            Text(
                text = MapTiles.ATTRIBUTION,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                fontSize = 8.sp,
                color = Silver.copy(alpha = 0.75f),
                maxLines = 1,
            )
        }
    }
}

/** 지도 버튼(확대 · 축소 · 처음 자리로)의 읽을 이름 */
data class MapControlLabels(val zoomIn: String, val zoomOut: String, val recenter: String)

/** 사용자가 기본 줌에서 더 당길 수 있는 단계 */
private const val MAX_USER_ZOOM_IN = 4

/** 더 밀어낼 수 있는 단계 (음수) */
private const val MAX_USER_ZOOM_OUT = -3

/** 지도 위 원형 버튼. 보이는 원과 터치 영역을 모두 48dp로 유지한다. */
@Composable
private fun MapButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    label: String? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Night.copy(alpha = 0.95f))
            .border(1.dp, Volt.copy(alpha = 0.4f), CircleShape)
            .quietClickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Volt,
            modifier = Modifier.size(21.dp),
        )
    }
}

/**
 * 타일 배치 계획.
 *
 * 좌표 계산은 전부 **타일 픽셀 공간**(타일 한 장 = 256)에서 하고, 화면에 올릴 때만
 * [scale]을 곱한다. 이렇게 해야 3배 밀도 화면에서 타일이 실제 크기의 1/3로
 * 쪼그라들어 글씨를 못 읽는 일이 없다.
 */
data class TilePlan(
    val zoom: Int,
    val originX: Double,
    val originY: Double,
    val minTileX: Int,
    val maxTileX: Int,
    val minTileY: Int,
    val maxTileY: Int,
    val scale: Float,
) {
    /** 이 계획이 필요로 하는 타일 집합의 식별자 — 효과를 여기에 묶는다 */
    val rangeKey: String get() = "$zoom/$minTileX-$maxTileX/$minTileY-$maxTileY"

    /** 날짜변경선을 넘어간 타일 인덱스를 세계 범위 안으로 되돌린다 */
    fun wrapX(x: Int): Int {
        val n = 1 shl zoom
        return ((x % n) + n) % n
    }

    fun toScreen(point: GeoPoint): Offset {
        val x = (MapTiles.worldX(point.lng, zoom) - originX) * scale
        val y = (MapTiles.worldY(point.lat, zoom) - originY) * scale
        return Offset(x.toFloat(), y.toFloat())
    }

    /** 화면 위치 → 좌표. [toScreen] 의 반대 */
    fun fromScreen(at: Offset): GeoPoint = GeoPoint(
        lat = MapTiles.latOf(at.y / scale + originY, zoom),
        lng = MapTiles.lngOf(at.x / scale + originX, zoom),
    )

    /** 화면에 보이는 범위 — (남서쪽 끝, 북동쪽 끝) */
    fun bounds(widthPx: Int, heightPx: Int): Pair<GeoPoint, GeoPoint> {
        val topLeft = fromScreen(Offset.Zero)
        val bottomRight = fromScreen(Offset(widthPx.toFloat(), heightPx.toFloat()))
        return GeoPoint(bottomRight.lat, topLeft.lng) to GeoPoint(topLeft.lat, bottomRight.lng)
    }

    companion object {
        /**
         * 타일을 화면에 몇 배로 늘려 그릴지.
         *
         * 예전에는 화면 밀도를 그대로 썼다. 3배 밀도 폰에서는 256px 타일을
         * 768px로 늘려 그리는 셈이라, 글자와 도로가 뭉개져 보였다 — "지도가
         * 흐리다"의 정체가 이것이다.
         *
         * 밀도의 절반만 쓴다. 같은 화면에 더 높은 줌의 타일이 들어오므로
         * 글자와 도로가 또렷해지고, 받는 타일 수는 예산([MapTiles.MAX_TILES])
         * 안에 머문다. 1.0 아래로는 내리지 않는다 — 그러면 지도가 실제보다
         * 작게 그려져 읽기 어려워진다.
         */
        private fun tileScale(density: Float): Float = (density / 2f).coerceIn(1f, 2f)

        /**
         * 큰 지도(경로 확대처럼 화면 대부분)는 [tileScale] 로 덮으면 타일이 예산([MapTiles.MAX_TILES])을 넘는다. 그때는 타일을
         * 조금 더 크게 그려 예산 안에서 화면을 덮는다 — 넘는다고 줌을 낮추면(타일 수는 줌과 상관없이 화면 크기로 정해진다)
         * 세계 지도까지 내려가 경로가 점 하나가 된다. 예산 안의 지도는 그대로다.
         */
        private fun fitScale(widthPx: Int, heightPx: Int, base: Float): Float {
            var scale = base
            while (MapTiles.tileCount(widthPx / scale.toDouble(), heightPx / scale.toDouble()) > MapTiles.MAX_TILES && scale < 4f) {
                scale *= 1.05f
            }
            return scale
        }

        /**
         * @param zoomDelta 사용자가 손가락으로 더하거나 뺀 줌 단계
         * @param panX 사용자가 끌어 옮긴 거리(타일 픽셀). 화면 픽셀이 아니다.
         * @param zooms [points] 에 맞춘 줌이 들어갈 범위 — [zoomDelta] 를 더하기 전
         * @param anchorY [points] 의 가운데를 둘 높이(위에서 0..1). 가운데가 아니면 가까운 쪽 가장자리까지 남은 높이 안에 맞춘다
         */
        fun of(
            points: List<GeoPoint>,
            widthPx: Int,
            heightPx: Int,
            density: Float,
            zoomDelta: Int = 0,
            panX: Double = 0.0,
            panY: Double = 0.0,
            zooms: IntRange = MapTiles.MIN_ZOOM..MapTiles.MAX_ZOOM,
            anchorY: Float = 0.5f,
        ): TilePlan {
            val scale = fitScale(widthPx, heightPx, tileScale(density))
            // 뷰포트를 타일 픽셀 단위로 환산해서 줌과 원점을 잡는다
            val viewW = (widthPx / scale).toDouble()
            val viewH = (heightPx / scale).toDouble()

            val fitH = viewH * 2 * minOf(anchorY, 1f - anchorY).coerceIn(0.05f, 0.5f)
            val fitted = MapTiles.fitZoom(points, viewW.toInt(), fitH.toInt()).coerceIn(zooms)
            val zoom = (fitted + zoomDelta).coerceIn(MapTiles.MIN_ZOOM, MapTiles.MAX_ZOOM)
            val xs = points.map { MapTiles.worldX(it.lng, zoom) }
            val ys = points.map { MapTiles.worldY(it.lat, zoom) }
            // 경로의 한가운데가 화면 한가운데(세로는 [anchorY] 높이) 오도록 원점을 잡고, 사용자가
            // 끌어 옮긴 만큼 비킨다.
            val originX = (xs.min() + xs.max()) / 2 - viewW / 2 - panX
            val originY = (ys.min() + ys.max()) / 2 - viewH * anchorY - panY

            val maxTileIndex = (1 shl zoom) - 1
            val minTileX = floor(originX / MapTiles.TILE_SIZE).toInt()
            val maxTileX = floor((originX + viewW) / MapTiles.TILE_SIZE).toInt()
            val minTileY = floor(originY / MapTiles.TILE_SIZE).toInt().coerceIn(0, maxTileIndex)
            val maxTileY = floor((originY + viewH) / MapTiles.TILE_SIZE).toInt()
                .coerceIn(0, maxTileIndex)

            return TilePlan(zoom, originX, originY, minTileX, maxTileX, minTileY, maxTileY, scale)
        }
    }
}

/**
 * 캐시에 있는 타일을 화면에 깐다. 그린 타일 수를 돌려준다.
 *
 * 비트맵을 컴포지션 상태로 따로 들지 않고 [MapTiles] 캐시에서 바로 읽는다.
 * 상태에 복사해 두면 LruCache가 비워도 컴포지션이 계속 붙잡고 있어, 크기 제한이
 * 무력해지고 줌을 오갈수록 메모리가 샌다.
 */
private fun DrawScope.drawTiles(plan: TilePlan, filter: ColorFilter): Int {
    var drawn = 0
    val side = (MapTiles.TILE_SIZE * plan.scale).toInt()
    for (ty in plan.minTileY..plan.maxTileY) {
        for (tx in plan.minTileX..plan.maxTileX) {
            val image = MapTiles.cached(plan.zoom, plan.wrapX(tx), ty) ?: continue
            // floor로 내림해야 음수 구간에서 타일 사이가 1px 벌어지지 않는다
            val left = floor((tx * MapTiles.TILE_SIZE - plan.originX) * plan.scale).toInt()
            val top = floor((ty * MapTiles.TILE_SIZE - plan.originY) * plan.scale).toInt()
            drawImage(
                image = image,
                dstOffset = IntOffset(left, top),
                dstSize = IntSize(side, side),
                colorFilter = filter,
            )
            drawn++
        }
    }
    return drawn
}

/** 폴리라인 전체 길이 기준 f(0..1) 지점의 좌표 */
private fun pointAlongRoute(pts: List<Offset>, f: Float): Offset {
    if (pts.size < 2) return pts.firstOrNull() ?: Offset.Zero
    val segs = FloatArray(pts.size - 1)
    var total = 0f
    for (i in 0 until pts.size - 1) {
        val d = (pts[i + 1] - pts[i]).getDistance()
        segs[i] = d
        total += d
    }
    if (total <= 0f) return pts.first()
    var remain = total * f
    for (i in segs.indices) {
        if (remain <= segs[i]) {
            val t = if (segs[i] > 0f) remain / segs[i] else 0f
            return pts[i] + (pts[i + 1] - pts[i]) * t
        }
        remain -= segs[i]
    }
    return pts.last()
}

/** 도착 깃발 — 막대 + 삼각 깃발 */
private fun DrawScope.drawRouteFlag(at: Offset) {
    val h = 13.dp.toPx()
    drawCircle(Volt.copy(alpha = 0.30f), radius = 8.dp.toPx(), center = at)
    drawLine(
        color = Snow,
        start = at,
        end = Offset(at.x, at.y - h),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round,
    )
    val flag = Path().apply {
        moveTo(at.x, at.y - h)
        lineTo(at.x + h * 0.62f, at.y - h * 0.78f)
        lineTo(at.x, at.y - h * 0.56f)
        close()
    }
    drawPath(flag, Volt)
}
