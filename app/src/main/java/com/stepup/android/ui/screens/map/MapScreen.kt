package com.stepup.android.ui.screens.map

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import com.stepup.android.ui.components.GhostButton
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.drawText
import com.stepup.android.data.remote.TerritoryCell
import com.stepup.android.data.remote.TerritoryStanding
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.Post
import com.stepup.android.domain.RunCourse
import com.stepup.android.domain.Territory
import com.stepup.android.domain.formatKm
import com.stepup.android.ui.components.SecondaryHeader
import com.stepup.android.ui.theme.StepUpDesign
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.StepUpMap
import com.stepup.android.ui.components.TilePlan
import com.stepup.android.ui.components.TwoWaySwitch
import com.stepup.android.ui.components.VoltButton
import com.stepup.android.ui.components.rememberCurrentLocation
import com.stepup.android.ui.theme.Cyan
import com.stepup.android.ui.theme.Night
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt
import kotlin.math.round

/** 지도 위에 꽂힌 것 하나 — 번개 또는 코스 출발점 */
private sealed interface Pin {
    val at: GeoPoint

    data class Flash(val post: Post) : Pin {
        override val at = GeoPoint(post.lat ?: 0.0, post.lng ?: 0.0)
    }

    data class Course(val course: RunCourse) : Pin {
        override val at = course.points.first()
    }
}

/**
 * 지도 — 내 주변 번개러닝·코스, 그리고 땅따먹기.
 *
 * 한 화면에 주 행동은 하나다: 고른 핀의 "자세히 보기". 핀을 고르기 전에는
 * 아래 카드가 무엇을 볼 수 있는지만 알려 준다.
 */
@Composable
fun MapScreen(
    onBack: () -> Unit = {},
    onOpenFlash: (Long) -> Unit = {},
    onOpenCourses: () -> Unit = {},
    viewModel: MapViewModel = viewModel(factory = MapViewModel.Factory),
) {
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val flashes by viewModel.flashes.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val territory by viewModel.territory.collectAsStateWithLifecycle()
    val standings by viewModel.standings.collectAsStateWithLifecycle()
    val here = rememberCurrentLocation()

    var selected by remember { mutableStateOf<Pin?>(null) }
    var selectedCell by rememberSaveable { mutableStateOf<String?>(null) }

    val pins: List<Pin> = remember(flashes, courses) {
        flashes.map { Pin.Flash(it) } + courses.map { Pin.Course(it) }
    }

    // 처음 맞출 범위 — 내 자리 둘레 약 1km(폰에서 화면 너비 약 1.4km). 자리를 모르면 첫 핀, 그것도 없으면 서울 시청.
    // 좌표를 0.01도로 반올림해 두어, GPS 가 조금 흔들릴 때마다 지도가 다시 맞춰지지 않게 한다.
    val focus = remember(here?.let { round(it.lat * 100) }, here?.let { round(it.lng * 100) }, pins.isEmpty()) {
        val center = here ?: pins.firstOrNull()?.at ?: GeoPoint(37.5665, 126.9780)
        listOf(
            GeoPoint(center.lat - FOCUS_SPAN, center.lng - FOCUS_SPAN),
            GeoPoint(center.lat + FOCUS_SPAN, center.lng + FOCUS_SPAN),
        )
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val c = com.stepup.android.ui.components.StepUpUi.colors
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val pinStyle = com.stepup.android.ui.components.StepUpUi.type.caption.copy(fontWeight = FontWeight.Bold, color = Color.White)
    // 위 알약 글 — 내 자리가 든 장소 이름(spots.json 반경 안) · 없으면 "지도"
    val near = remember(here?.let { round(it.lat * 200) }, here?.let { round(it.lng * 200) }) {
        here?.let { com.stepup.android.ui.background.Spots.nearest(context, it.lat, it.lng)?.name }
    }
    // 아래 카드 — 고른 핀, 고르지 않았으면 내 자리에서 가장 가까운 번개(시안 map "내 위치 근처 번개")
    val nearestFlash = remember(flashes, here) {
        flashes.filter { it.lat != null && it.lng != null }.minByOrNull { it.awayKmFrom(here) ?: Double.MAX_VALUE }
    }
    val shown: Pin? = selected ?: nearestFlash?.let { Pin.Flash(it) }

    // UI 최종 통합안 map · terr_* — 지도가 화면 가득, 위에 떠 있는 ← · 장소 알약 · 세그먼트(내 주변 · 땅따먹기), 아래 떠 있는 카드
    Box(Modifier.fillMaxSize().background(c.mapBackground).testTag("map-screen")) {
        val cells = (territory as? TerritoryState.Ready)?.cells.orEmpty()
        val tapRadius = with(LocalDensity.current) { 28.dp.toPx() }
        StepUpMap(
            focus = focus,
            modifier = Modifier.fillMaxSize(),
            interactive = true,
            onViewport = viewModel::onViewport,
            onTap = { at, plan ->
                if (mode == MapMode.NEARBY) {
                    selected = nearestPin(pins, at, plan, radiusPx = tapRadius)
                } else {
                    val point = plan.fromScreen(at)
                    val cell = Territory.cellOf(point.lat, point.lng)
                    selectedCell = cells.firstOrNull { it.cell == cell }?.cell
                }
            },
        ) { plan ->
            if (mode == MapMode.NEARBY) {
                drawPins(plan, pins, shown, textMeasurer, pinStyle)
            } else {
                drawCells(plan, cells, selectedCell)
            }
            here?.let { drawHere(plan, it) }
        }

        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.stepup.android.ui.components.BackButton(onBack, size = 44.dp, modifier = Modifier.testTag("map-back"))
                Spacer(Modifier.width(4.dp))
                Box(
                    Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(50)).background(c.tabBar).padding(horizontal = 18.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(near?.let { stringResource(R.string.map_near_place, it) } ?: stringResource(R.string.map_title),
                        style = com.stepup.android.ui.components.StepUpUi.type.body, color = c.secondaryLabel, maxLines = 1)
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.width(200.dp).clip(RoundedCornerShape(11.dp)).background(c.tabBar)) {
                com.stepup.android.ui.components.StepUpTabs(
                    listOf(stringResource(R.string.map_seg_nearby), stringResource(R.string.map_seg_territory)),
                    if (mode == MapMode.NEARBY) 0 else 1,
                    { i ->
                        selected = null
                        selectedCell = null
                        viewModel.select(if (i == 0) MapMode.NEARBY else MapMode.TERRITORY)
                    },
                    Modifier.fillMaxWidth(),
                    tags = listOf("map-seg-nearby", "map-seg-territory"),
                )
            }
        }

        Box(
            Modifier.align(Alignment.BottomCenter).padding(horizontal = 10.dp).padding(bottom = 10.dp)
                .heightIn(max = 380.dp).clip(RoundedCornerShape(24.dp)).background(c.card)
                .verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        ) {
            Column {
                com.stepup.android.ui.components.SheetHandle(Modifier.align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(10.dp))
                when (mode) {
                    MapMode.NEARBY -> NearbyCard(
                        selected = shown,
                        flashCount = flashes.size,
                        courseCount = courses.size,
                        here = here,
                        onOpenFlash = onOpenFlash,
                        onOpenCourses = onOpenCourses,
                    )
                    MapMode.TERRITORY -> {
                        val ready = territory as? TerritoryState.Ready
                        TerritoryStatus(state = territory, onRetry = viewModel::retryTerritory)
                        if (ready != null && (ready.cells.isNotEmpty() || standings.isNotEmpty())) {
                            TerritoryCard(cell = ready.cells.firstOrNull { it.cell == selectedCell }, standings = standings)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 처음 보여 줄 범위의 반폭(도). 0.005도 ≈ 550m — 폰에서 줌 16으로 열린다.
 * 예전 0.015도(1.6km)는 줌 14로 열려 화면 너비로 5km 넘게 보였다. 더 넓게는 손가락 · − 버튼으로 본다.
 */
private const val FOCUS_SPAN = 0.005

private fun nearestPin(pins: List<Pin>, at: Offset, plan: TilePlan, radiusPx: Float): Pin? =
    pins.minByOrNull { (plan.toScreen(it.at) - at).getDistance() }
        ?.takeIf { (plan.toScreen(it.at) - at).getDistance() <= radiusPx }

/** 지도 핀(시안 map) — 번개는 블루 알약(번개 · 집결 시각), 코스는 짙은 알약(거리). 고른 핀은 조금 크게 */
private fun DrawScope.drawPins(
    plan: TilePlan,
    pins: List<Pin>,
    selected: Pin?,
    measurer: androidx.compose.ui.text.TextMeasurer,
    style: androidx.compose.ui.text.TextStyle,
) {
    for (pin in pins) {
        val at = plan.toScreen(pin.at)
        if (at.x < -80 || at.y < -40 || at.x > size.width + 80 || at.y > size.height + 40) continue
        val flash = pin is Pin.Flash
        val label = when (pin) {
            is Pin.Flash -> java.time.Instant.ofEpochMilli(pin.post.meetAt).atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            is Pin.Course -> "%.2fkm".format(pin.course.distanceKm)
        }
        val text = measurer.measure(label, style)
        val big = pin == selected
        val h = (if (big) 36 else 32).dp.toPx()
        val padX = 12.dp.toPx()
        val w = text.size.width + padX * 2
        val topLeft = Offset(at.x - w / 2, at.y - h - 6.dp.toPx())
        drawRoundRect(Color.Black.copy(alpha = .35f), topLeft + Offset(0f, 4.dp.toPx()), androidx.compose.ui.geometry.Size(w, h),
            androidx.compose.ui.geometry.CornerRadius(h / 2))
        drawRoundRect(if (flash) Color(0xFF2F6BFF) else Color(0xFF151C2D), topLeft, androidx.compose.ui.geometry.Size(w, h),
            androidx.compose.ui.geometry.CornerRadius(h / 2))
        drawText(text, topLeft = Offset(topLeft.x + padX, topLeft.y + (h - text.size.height) / 2))
        // 꼭짓점 — 핀이 가리키는 자리
        drawCircle(if (flash) Color(0xFF2F6BFF) else Color(0xFF151C2D), radius = 3.dp.toPx(), center = at)
    }
}

/** 내 자리 — 블루 점 + 옅은 블루 고리(시안 map) */
private fun DrawScope.drawHere(plan: TilePlan, here: GeoPoint) {
    val at = plan.toScreen(here)
    drawCircle(Color(0xFF2F6BFF).copy(alpha = 0.25f), radius = 21.dp.toPx(), center = at)
    drawCircle(Color.White, radius = 12.dp.toPx(), center = at)
    drawCircle(Color(0xFF2F6BFF), radius = 9.dp.toPx(), center = at)
}

/** 크루 색 — 크루마다 늘 같은 색 */
private fun crewColor(crewId: String): Color = Color.hsv(Territory.crewHue(crewId), 0.62f, 0.95f)

private fun DrawScope.drawCells(plan: TilePlan, cells: List<TerritoryCell>, selected: String?) {
    for (cell in cells) {
        val corners = Territory.corners(cell.cell)
        if (corners.isEmpty()) continue
        val screen = corners.map { plan.toScreen(it) }
        val path = Path().apply {
            moveTo(screen[0].x, screen[0].y)
            for (i in 1 until screen.size) lineTo(screen[i].x, screen[i].y)
            close()
        }
        val color = crewColor(cell.crewId)
        // 점수가 높을수록 진하게 — 굳힌 땅과 막 칠한 땅이 구별된다
        val alpha = (0.22f + cell.score.coerceAtMost(10) * 0.035f).coerceAtMost(0.58f)
        drawPath(path, color.copy(alpha = alpha))
        drawPath(
            path,
            color = if (cell.mine) Volt else color.copy(alpha = 0.85f),
            style = Stroke(width = if (cell.cell == selected) 3.dp.toPx() else if (cell.mine) 1.6f.dp.toPx() else 1.dp.toPx()),
        )
    }
}

@Composable
private fun TerritoryStatus(state: TerritoryState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val c = com.stepup.android.ui.components.StepUpUi.colors
    val text = when (state) {
        TerritoryState.Loading -> stringResource(R.string.feed_loading)
        is TerritoryState.Ready -> if (state.cells.isEmpty()) stringResource(R.string.map_territory_empty) else return
        TerritoryState.SignIn -> stringResource(R.string.map_territory_sign_in)
        TerritoryState.ZoomIn -> stringResource(R.string.map_territory_zoom)
        TerritoryState.Failed -> stringResource(R.string.map_territory_failed)
    }
    Column(modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state == TerritoryState.Loading) {
                com.stepup.android.ui.components.RunSpinner(Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(text, style = com.stepup.android.ui.components.StepUpUi.type.body, color = c.label, modifier = Modifier.weight(1f))
        }
        if (state == TerritoryState.Failed) {
            com.stepup.android.ui.components.SecondaryButton(stringResource(R.string.map_retry), onClick = onRetry, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** 내 주변 카드(시안 map) — 번개: 장소 사진 · 제목 · "시각 · 장소 · 인원" · 같이 달리기, 코스: 거리 · 코스 보기, 없으면 요약 한 줄 */
@Composable
private fun NearbyCard(
    selected: Pin?,
    flashCount: Int,
    courseCount: Int,
    here: GeoPoint?,
    onOpenFlash: (Long) -> Unit,
    onOpenCourses: () -> Unit,
) {
    val c = com.stepup.android.ui.components.StepUpUi.colors
    val type = com.stepup.android.ui.components.StepUpUi.type
    when (selected) {
        null -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                if (flashCount + courseCount == 0) stringResource(R.string.map_empty_nearby)
                else stringResource(R.string.map_nearby_summary, flashCount, courseCount),
                style = type.body, color = c.secondaryLabel,
            )
            if (courseCount > 0) {
                com.stepup.android.ui.components.SecondaryButton(stringResource(R.string.map_open_courses), onClick = onOpenCourses, modifier = Modifier.fillMaxWidth())
            }
        }
        is Pin.Flash -> {
            val post = selected.post
            val photo = com.stepup.android.ui.background.rememberPlacePhoto(post.place, post.lat, post.lng, seed = "flash-${post.id}", widthDp = 56)
            val away = post.awayKmFrom(here)
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    com.stepup.android.ui.components.PlaceThumb(photo?.painter, size = 56.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(post.title, style = type.rowTitle.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold), color = c.label, maxLines = 1)
                        Text(
                            listOfNotNull(
                                java.time.Instant.ofEpochMilli(post.meetAt).atZone(java.time.ZoneId.systemDefault())
                                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                                post.place.ifBlank { null },
                                away?.let { stringResource(R.string.map_away_km, formatKm(it)) },
                                stringResource(R.string.map_flash_people, post.joinedCount, post.capacity),
                            ).joinToString(" · "),
                            style = type.rowSubtitle, color = c.secondaryLabel, maxLines = 1,
                        )
                    }
                }
                com.stepup.android.ui.components.PrimaryButton(stringResource(R.string.flash_join_go), onClick = { onOpenFlash(post.id) },
                    modifier = Modifier.fillMaxWidth().testTag("map-open-flash"))
            }
        }
        is Pin.Course -> {
            val course = selected.course
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(course.name, style = type.rowTitle.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold), color = c.label, maxLines = 1)
                        Text(stringResource(R.string.map_course_meta, formatKm(course.distanceKm), course.runCount), style = type.rowSubtitle, color = c.secondaryLabel)
                    }
                }
                com.stepup.android.ui.components.PrimaryButton(stringResource(R.string.map_open_courses), onClick = onOpenCourses,
                    modifier = Modifier.fillMaxWidth().testTag("map-open-courses"))
            }
        }
    }
}

/**
 * 땅따먹기 카드(시안 terr_cells · terr_cell) — 고른 칸이 있으면 그 칸의 크루 · 표시 수, 없으면 "최근 14일 · 우리 크루 땅" 큰 숫자 →
 * 크루 순위(색 · 이름 · 칸 수) → 규칙 한 줄. 시안의 "동네" 묶음 · 칸 안 거리 막대는 서버가 주지 않는 값이라 두지 않는다.
 */
@Composable
private fun TerritoryCard(cell: TerritoryCell?, standings: List<TerritoryStanding>) {
    val c = com.stepup.android.ui.components.StepUpUi.colors
    val type = com.stepup.android.ui.components.StepUpUi.type
    Column(Modifier.fillMaxWidth()) {
        if (cell != null) {
            Text(stringResource(R.string.map_cell_score, cell.score), style = type.rowSubtitle, color = c.secondaryLabel)
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(16.dp).clip(RoundedCornerShape(5.dp)).background(crewColor(cell.crewId)))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.map_cell_title, cell.crewName), style = type.section.copy(fontSize = 26.sp), color = c.label, maxLines = 1)
            }
        } else {
            val mine = standings.firstOrNull { it.mine }
            if (mine != null) {
                Text(stringResource(R.string.map_territory_mine), style = type.rowSubtitle, color = c.secondaryLabel)
                com.stepup.android.ui.components.NumberWithUnit(mine.cells.toString(), unit = stringResource(R.string.map_unit_cells),
                    style = type.numberLarge.copy(fontSize = 72.sp), unitSize = 21.6.sp)
            } else {
                Text(stringResource(R.string.map_territory_board), style = type.section, color = c.label)
            }
        }
        Spacer(Modifier.height(10.dp))
        standings.take(5).forEachIndexed { i, row ->
            if (i > 0) Box(Modifier.fillMaxWidth().height(.5.dp).background(c.separator))
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(crewColor(row.crewId)))
                Spacer(Modifier.width(10.dp))
                Text(row.crewName, style = type.rowTitle.copy(fontSize = 15.sp, fontWeight = if (row.mine) FontWeight.Bold else FontWeight.Normal),
                    color = if (row.mine) c.label else c.secondaryLabel, modifier = Modifier.weight(1f), maxLines = 1)
                com.stepup.android.ui.components.NumberWithUnit(row.cells.toString(), unit = stringResource(R.string.map_unit_cells),
                    style = type.numberRow.copy(fontSize = 24.sp))
            }
        }
        Text(stringResource(R.string.map_territory_hint), style = type.caption.copy(fontSize = 12.5.sp), color = c.secondaryLabel,
            modifier = Modifier.padding(top = 6.dp))
    }
}
