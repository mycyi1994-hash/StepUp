package com.stepup.android.ui.screens.walk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.CrewRoomRules
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.RoomCourse
import com.stepup.android.domain.RunCourse
import com.stepup.android.ui.components.LocalMapTone
import com.stepup.android.ui.components.MapControlLabels
import com.stepup.android.ui.components.StepUpMap
import com.stepup.android.ui.components.TilePlan
import com.stepup.android.ui.components.rememberCurrentLocation
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.experience.feedbackClickable

/*
 * 지도에서 코스 그리기 — 2026-10-24 사용자 "코스를 내가 선택할 수가 없네 … 정해준 것만 해야 되던데 그건 아닌 것 같다".
 *
 * 지도를 누를 때마다 점이 하나씩 이어진다(점 사이는 곧은 줄 — 굽은 길은 점을 촘촘히 찍는다). 끌면 지도가 움직이고, ± · 두 손가락으로
 * 키운다. 거리는 점 사이 거리의 합(CrewRoomRules.pathKm). 되돌리기 · 출발점으로 잇기(한 바퀴 코스) · 처음부터.
 * "이 코스로 정하기"를 눌러야 고른다 — 뒤로 나오면 원래 코스 그대로다. 그린 코스는 방의 코스 사본으로만 간다(내 코스 목록에는 넣지 않는다).
 */

/** 내 자리를 모를 때 처음 여는 자리(서울 시청) — 거리를 재는 데 쓰지 않는다 */
private val DRAW_FALLBACK = GeoPoint(37.5663, 126.9779)

/** 처음 맞출 범위의 반폭(도) — 약 650m, 폰에서 동네 한두 블록이 보인다 */
private const val DRAW_SPAN = 0.006

/** 찍은 점을 화면 회전 · 잠깐 꺼짐에도 지킨다("위도,경도;…") */
internal val DrawnPointsSaver = Saver<List<GeoPoint>, String>(
    save = { points -> points.joinToString(";") { "${it.lat},${it.lng}" } },
    restore = { saved -> if (saved.isBlank()) emptyList() else RunCourse.decode(saved) },
)

private val DrawCenterSaver = Saver<GeoPoint?, String>(
    save = { point -> point?.let { "${it.lat},${it.lng}" } },
    restore = { saved -> RunCourse.decode(saved).firstOrNull() },
)

/**
 * 코스 그리기 — 찍은 점은 부르는 쪽([points] · [onPoints])이 들고 있다: 목록으로 돌아갔다 다시 들어와도 그리던 코스가 남는다.
 * [onPick] 은 쓸 수 있는 코스(점 둘 이상 · 100km 안)일 때만 불린다.
 */
@Composable
internal fun CrewCourseDraw(
    points: List<GeoPoint>,
    onPoints: (List<GeoPoint>) -> Unit,
    onBack: () -> Unit,
    onPick: (RoomCourse) -> Unit,
) {
    val here = rememberCurrentLocation()
    // 처음 맞출 자리 — 내 자리를 알면 그곳(한 번만), 모르면 그린 첫 점 · 서울 시청. 점을 찍기 시작한 뒤에는 옮기지 않는다(누를 때마다 지도가 튀지 않게)
    var center by rememberSaveable(stateSaver = DrawCenterSaver) { mutableStateOf<GeoPoint?>(null) }
    LaunchedEffect(here) {
        if (center == null && points.isEmpty() && here != null) center = here
    }
    val name = stringResource(R.string.crew5_draw_name)
    val anchor = center ?: points.firstOrNull() ?: DRAW_FALLBACK
    CrewCourseDrawContent(
        points = points,
        here = here,
        center = anchor,
        onAdd = { point ->
            // 처음 찍는 순간 지금 보이는 자리로 묶는다 — 첫 점으로 지도가 옮겨 가지 않게(내 자리를 받기 전에 찍었을 때)
            if (center == null) center = anchor
            if (points.size < CrewRoomRules.DRAW_MAX_POINTS) onPoints(points + point)
        },
        onUndo = { onPoints(points.dropLast(1)) },
        onLoop = { if (points.size >= 2 && points.first() != points.last()) onPoints(points + points.first()) },
        onClear = { onPoints(emptyList()) },
        onBack = onBack,
        onUse = { if (CrewRoomRules.drawnUsable(points)) onPick(RoomCourse.drawn(name, points)) },
    )
}

@Composable
internal fun CrewCourseDrawContent(
    points: List<GeoPoint>,
    here: GeoPoint?,
    center: GeoPoint,
    onAdd: (GeoPoint) -> Unit,
    onUndo: () -> Unit,
    onLoop: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    onUse: () -> Unit,
) {
    val c = crewColors()
    val km = CrewRoomRules.pathKm(points)
    val tooLong = km > CrewRoomRules.COURSE_KM_MAX
    val full = points.size >= CrewRoomRules.DRAW_MAX_POINTS
    val canLoop = points.size >= 2 && points.first() != points.last()
    Box(Modifier.fillMaxSize().background(c.screen).testTag(if (points.isEmpty()) "crew-draw" else "crew-draw-drawing")) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            CrewHeader(stringResource(R.string.crew5_draw_header), onBack = onBack, badge = false)
            // 한 줄 — 점이 없으면 하는 법, 찍은 뒤에는 거리 · 점 수(바뀌면 화면 낭독이 읽는다)
            Text(
                if (points.isEmpty()) stringResource(R.string.crew5_draw_hint)
                else stringResource(R.string.crew5_draw_summary, courseKm(km), points.size),
                style = runTextStyle(if (points.isEmpty()) 14.sp else 18.sp, if (points.isEmpty()) c.label else c.text,
                    if (points.isEmpty()) FontWeight.Medium else FontWeight.ExtraBold, 1.45f),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).heightIn(min = 26.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }.testTag("crew-draw-summary"),
            )
            Spacer(Modifier.height(10.dp))
            val focus = remember(center) {
                listOf(
                    GeoPoint(center.lat - DRAW_SPAN, center.lng - DRAW_SPAN),
                    GeoPoint(center.lat + DRAW_SPAN, center.lng + DRAW_SPAN),
                )
            }
            val shape = RoundedCornerShape(18.dp)
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp).clip(shape).background(LobbyInk.Map, shape)
                    .testTag("crew-draw-map"),
            ) {
                CompositionLocalProvider(LocalMapTone provides CrewBrightMap) {
                    StepUpMap(
                        focus = focus,
                        modifier = Modifier.fillMaxSize(),
                        interactive = true,
                        controlLabels = MapControlLabels(
                            zoomIn = stringResource(R.string.rec_zoom_in), zoomOut = stringResource(R.string.rec_zoom_out),
                            recenter = stringResource(R.string.rec_map_recenter),
                        ),
                        onTap = { at, plan -> onAdd(plan.fromScreen(at)) },
                    ) { plan -> drawDrawnCourse(plan, points, here) }
                }
            }
            when {
                tooLong -> CrewError(
                    stringResource(R.string.crew5_draw_too_long, CrewRoomRules.COURSE_KM_MAX.toInt()),
                    Modifier.padding(horizontal = 20.dp).padding(top = 10.dp), tag = "crew-draw-too-long",
                )
                full -> CrewNote(
                    stringResource(R.string.crew5_draw_too_many, CrewRoomRules.DRAW_MAX_POINTS),
                    Modifier.padding(horizontal = 20.dp).padding(top = 10.dp), tag = "crew-draw-full",
                )
            }
            // 작은 행동 셋 — 되돌리기 · 출발점으로 잇기 · 처음부터(주 행동은 아래 큰 버튼 하나)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                DrawAction(Icons.AutoMirrored.Outlined.Undo, stringResource(R.string.crew5_draw_undo), points.isNotEmpty(), onUndo, "crew-draw-undo")
                DrawAction(Icons.Outlined.Loop, stringResource(R.string.crew5_draw_loop), canLoop, onLoop, "crew-draw-loop")
                DrawAction(Icons.Outlined.DeleteOutline, stringResource(R.string.crew5_draw_clear), points.isNotEmpty(), onClear, "crew-draw-clear")
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 12.dp)) {
                FreeRunButton(
                    stringResource(R.string.crew5_draw_use), onUse, Modifier.fillMaxWidth().testTag("crew-draw-use"),
                    enabled = CrewRoomRules.drawnUsable(points),
                )
            }
        }
    }
}

/** 지도 아래 작은 행동 — 아이콘 + 글자, 누르는 자리 48dp. 쓸 수 없으면 흐리게 */
@Composable
private fun DrawAction(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit, tag: String) {
    val c = crewColors()
    val ink = if (enabled) c.link else c.muted
    Row(
        Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp))
            .then(if (enabled) Modifier.feedbackClickable(role = Role.Button, onClick = onClick) else Modifier.semantics { disabled() })
            .padding(horizontal = 10.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = runTextStyle(14.sp, ink, FontWeight.SemiBold))
    }
}

/** 그리는 코스 — 흰 테두리 파란 줄 · 사이 점 · 출발(초록) · 끝(빨강) · 지금 내 자리(파란 점) */
private fun DrawScope.drawDrawnCourse(plan: TilePlan, points: List<GeoPoint>, here: GeoPoint?) {
    here?.let {
        val at = plan.toScreen(it)
        drawCircle(HereHalo, radius = 16.dp.toPx(), center = at)
        drawCircle(Color.White, radius = 7.dp.toPx(), center = at)
        drawCircle(HereDot, radius = 5.dp.toPx(), center = at)
    }
    if (points.size >= 2) {
        val path = Path()
        points.forEachIndexed { i, point ->
            val at = plan.toScreen(point)
            if (i == 0) path.moveTo(at.x, at.y) else path.lineTo(at.x, at.y)
        }
        drawPath(path, Color.White, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path, LobbyInk.Accent, style = Stroke(width = 4.5f.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    points.forEachIndexed { i, point ->
        val at = plan.toScreen(point)
        val edge = i == 0 || i == points.lastIndex
        if (!edge) {
            drawCircle(Color.White, radius = 5.dp.toPx(), center = at)
            drawCircle(LobbyInk.Accent, radius = 3.dp.toPx(), center = at)
        }
    }
    // 끝 점을 먼저, 출발 점을 위에 — 한 바퀴 코스(끝 = 출발)면 출발이 보인다
    if (points.size >= 2) {
        val end = plan.toScreen(points.last())
        drawCircle(Color.White, radius = 10.dp.toPx(), center = end)
        drawCircle(DrawEnd, radius = 7.dp.toPx(), center = end)
    }
    points.firstOrNull()?.let {
        val start = plan.toScreen(it)
        drawCircle(Color.White, radius = 10.dp.toPx(), center = start)
        drawCircle(DrawStart, radius = 7.dp.toPx(), center = start)
    }
}

private val DrawStart = Color(0xFF0E9F6E)
private val DrawEnd = Color(0xFFE5484D)
private val HereDot = Color(0xFF1A73E8)
private val HereHalo = Color(0x331A73E8)
