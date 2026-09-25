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

    // 처음 맞출 범위 — 내 자리 둘레 약 3km. 자리를 모르면 핀들, 그것도 없으면 서울 시청.
    // 좌표를 0.01도로 반올림해 두어, GPS 가 조금 흔들릴 때마다 지도가 다시 맞춰지지 않게 한다.
    val focus = remember(here?.let { round(it.lat * 100) }, here?.let { round(it.lng * 100) }, pins.isEmpty()) {
        val center = here ?: pins.firstOrNull()?.at ?: GeoPoint(37.5665, 126.9780)
        listOf(
            GeoPoint(center.lat - FOCUS_SPAN, center.lng - FOCUS_SPAN),
            GeoPoint(center.lat + FOCUS_SPAN, center.lng + FOCUS_SPAN),
        )
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val detailsMaxHeight = maxHeight * 0.42f
        Column(Modifier.fillMaxSize()) {
            SecondaryHeader(
                title = stringResource(R.string.map_title),
                onBack = onBack, balance = null, onOpenWallet = null,
                modifier = Modifier.padding(horizontal = StepUpDesign.Gutter),
            )

            TwoWaySwitch(
                labels = listOf(stringResource(R.string.map_seg_nearby), stringResource(R.string.map_seg_territory)),
                selected = if (mode == MapMode.NEARBY) 0 else 1,
                onSelect = {
                    selected = null
                    selectedCell = null
                    viewModel.select(if (it == 0) MapMode.NEARBY else MapMode.TERRITORY)
                },
                modifier = Modifier.padding(horizontal = StepUpDesign.Gutter, vertical = 12.dp),
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = StepUpDesign.Gutter)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, Volt.copy(alpha = 0.28f), RoundedCornerShape(18.dp)),
            ) {
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
                        drawPins(plan, pins, selected)
                    } else {
                        drawCells(plan, cells, selectedCell)
                    }
                    here?.let { drawHere(plan, it) }
                }

            }

            Box(Modifier.heightIn(max = detailsMaxHeight).verticalScroll(rememberScrollState()).padding(start = StepUpDesign.Gutter, end = StepUpDesign.Gutter, top = 12.dp, bottom = 18.dp)) {
                when (mode) {
                    MapMode.NEARBY -> NearbyCard(
                        selected = selected,
                        flashCount = flashes.size,
                        courseCount = courses.size,
                        here = here,
                        onOpenFlash = onOpenFlash,
                        onOpenCourses = onOpenCourses,
                    )

                    MapMode.TERRITORY -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TerritoryStatus(state = territory, onRetry = viewModel::retryTerritory)
                        val ready = territory as? TerritoryState.Ready
                        if (ready != null && (ready.cells.isNotEmpty() || standings.isNotEmpty())) {
                            TerritoryCard(cell = ready.cells.firstOrNull { it.cell == selectedCell }, standings = standings)
                        }
                    }
                }
            }
        }
    }
}

/** 처음 보여 줄 범위의 반폭(도). 0.015도 ≈ 1.6km */
private const val FOCUS_SPAN = 0.015

private fun nearestPin(pins: List<Pin>, at: Offset, plan: TilePlan, radiusPx: Float): Pin? =
    pins.minByOrNull { (plan.toScreen(it.at) - at).getDistance() }
        ?.takeIf { (plan.toScreen(it.at) - at).getDistance() <= radiusPx }

private fun DrawScope.drawPins(plan: TilePlan, pins: List<Pin>, selected: Pin?) {
    for (pin in pins) {
        val at = plan.toScreen(pin.at)
        if (at.x < -40 || at.y < -40 || at.x > size.width + 40 || at.y > size.height + 40) continue
        val color = if (pin is Pin.Flash) Volt else Cyan
        val big = pin == selected
        drawCircle(color.copy(alpha = 0.28f), radius = (if (big) 16 else 11).dp.toPx(), center = at)
        drawCircle(color, radius = (if (big) 7f else 5.5f).dp.toPx(), center = at)
        drawCircle(Night, radius = 2.dp.toPx(), center = at)
    }
}

private fun DrawScope.drawHere(plan: TilePlan, here: GeoPoint) {
    val at = plan.toScreen(here)
    drawCircle(Color.White.copy(alpha = 0.20f), radius = 15.dp.toPx(), center = at)
    drawCircle(Night, radius = 10.dp.toPx(), center = at)
    drawCircle(Color.White, radius = 8.dp.toPx(), center = at, style = Stroke(width = 2.dp.toPx()))
    drawCircle(Color(0xFF3B82F6), radius = 4.dp.toPx(), center = at)
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
    val text = when (state) {
        TerritoryState.Loading -> stringResource(R.string.feed_loading)
        is TerritoryState.Ready -> if (state.cells.isEmpty()) stringResource(R.string.map_territory_empty) else return
        TerritoryState.SignIn -> stringResource(R.string.map_territory_sign_in)
        TerritoryState.ZoomIn -> stringResource(R.string.map_territory_zoom)
        TerritoryState.Failed -> stringResource(R.string.map_territory_failed)
    }
    GlowCard(modifier, contentPadding = PaddingValues(18.dp), spacing = 12.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state == TerritoryState.Loading) {
                CircularProgressIndicator(Modifier.size(22.dp), color = Volt, strokeWidth = 2.dp)
            } else {
                Icon(if (state == TerritoryState.Failed) Icons.Filled.Warning else Icons.Filled.Info, null, tint = Silver, modifier = Modifier.size(22.dp))
            }
            Text(text, style = MaterialTheme.typography.bodyMedium, color = Snow, modifier = Modifier.weight(1f))
        }
        if (state == TerritoryState.Failed) {
            GhostButton(stringResource(R.string.map_retry), onClick = onRetry, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun NearbyCard(
    selected: Pin?,
    flashCount: Int,
    courseCount: Int,
    here: GeoPoint?,
    onOpenFlash: (Long) -> Unit,
    onOpenCourses: () -> Unit,
) {
    GlowCard(contentPadding = PaddingValues(20.dp), spacing = 10.dp) {
        when (selected) {
            null -> {
                Text(
                    text = if (flashCount + courseCount == 0) {
                        stringResource(R.string.map_empty_nearby)
                    } else {
                        stringResource(R.string.map_nearby_summary, flashCount, courseCount)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Silver,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Legend(Volt, stringResource(R.string.post_cat_flash))
                    Legend(Cyan, stringResource(R.string.map_legend_course))
                }
                if (courseCount > 0) {
                    GhostButton(
                        text = stringResource(R.string.map_open_courses),
                        onClick = onOpenCourses,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            is Pin.Flash -> {
                val post = selected.post
                Text(post.title, color = Snow, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                val away = post.awayKmFrom(here)
                Text(
                    text = listOfNotNull(
                        post.place.ifBlank { null },
                        away?.let { stringResource(R.string.map_away_km, formatKm(it)) },
                        stringResource(R.string.map_flash_people, post.joinedCount, post.capacity),
                    ).joinToString(" · "),
                    fontSize = 14.sp,
                    color = Silver,
                )
                VoltButton(
                    text = stringResource(R.string.map_open_flash),
                    onClick = { onOpenFlash(post.id) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is Pin.Course -> {
                val course = selected.course
                Text(course.name, color = Snow, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(
                    text = stringResource(R.string.map_course_meta, formatKm(course.distanceKm), course.runCount),
                    fontSize = 14.sp,
                    color = Silver,
                )
                VoltButton(
                    text = stringResource(R.string.map_open_courses),
                    onClick = onOpenCourses,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(label, fontSize = 14.sp, color = Silver)
    }
}

@Composable
private fun TerritoryCard(cell: TerritoryCell?, standings: List<TerritoryStanding>) {
    GlowCard(contentPadding = PaddingValues(20.dp), spacing = 8.dp) {
        if (cell != null) {
            Text(
                text = stringResource(R.string.map_territory_cell, cell.crewName, cell.score),
                color = Snow,
                fontWeight = FontWeight.Bold,
            )
        } else {
            Text(
                text = stringResource(R.string.map_territory_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = Silver,
            )
        }
        if (standings.isNotEmpty()) {
            Text(
                text = stringResource(R.string.map_territory_board),
                style = MaterialTheme.typography.titleSmall,
                color = Snow,
            )
            standings.take(5).forEachIndexed { i, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("${i + 1}", color = Slate, fontSize = 14.sp)
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(crewColor(row.crewId)),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(row.crewName, color = if (row.mine) Volt else Snow, fontSize = 16.sp)
                        Text(stringResource(R.string.map_territory_cells, row.cells), color = Silver, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
