package com.stepup.android.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.data.repo.Crew
import com.stepup.android.domain.CrewRoom
import com.stepup.android.domain.CrewRoomRules
import com.stepup.android.domain.RoomMode
import com.stepup.android.ui.components.PrimaryButton
import com.stepup.android.ui.components.StepUpSpinner
import com.stepup.android.ui.components.StepUpTabs
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.components.pressable
import com.stepup.android.ui.screens.walk.RoomsUi
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * 러닝 탭 — 2026-10-26 전체 시안 `run` · `lobby`. 위의 세그먼트로 자유 러닝 · 크루 러닝을 가른다.
 *
 * 자유 러닝: 내 소속 크루 · 오늘 걸음(고리) · 사람 없는 배경 사진(15장 중 하나) · GPS 준비 · 오늘 거리 / 적립 / 이번 주 러닝 · "러닝 시작".
 * 크루 러닝: 내 소속 크루 · 진행 중인 대기실(같은 크루, 서버 party_rooms) · "새 대기실 만들기".
 * 신고 있는 신발은 여기 없다 — 러닝을 시작할 때 신발 점검(자유러닝 시작)과 신발 탭에서 본다(사용자 결정).
 * 모든 숫자 · 이름은 실제 기록 · 서버 값이다. 시안의 "망원 러닝클럽" · "3,420" 같은 예시를 쓰지 않는다.
 */

/** 러닝 탭 위의 두 칸 */
enum class RunSegment { FREE, CREW }

/** 출발 전 위치 — 러닝 탭이 미리 잡아 둔 위치(LocationWarmup)로 본다 */
enum class GpsReadiness { READY, WAITING, NO_PERMISSION }

/** 사람 없는 배경 사진 15장(2026-10-26 전달본 02-backgrounds-15) — assets/run_photos */
object RunPhotos {
    val names = listOf(
        "01-blue-track", "02-midnight-stadium", "03-riverside-dawn", "04-bridge-path", "05-coastal-road",
        "06-forest-path", "07-city-night", "08-stadium-stairs", "09-running-tunnel", "10-rain-track",
        "11-lakeside-path", "12-mountain-ridge", "13-blue-court", "14-snow-promenade", "15-track-curve",
    )
    private val cache = android.util.LruCache<String, ImageBitmap>(3)

    fun decode(context: android.content.Context, name: String): ImageBitmap? {
        cache.get(name)?.let { return it }
        return runCatching {
            context.assets.open("run_photos/$name.webp").use { android.graphics.BitmapFactory.decodeStream(it)?.asImageBitmap() }
        }.getOrNull()?.also { cache.put(name, it) }
    }
}

/** 자유 러닝 · 크루 러닝 세그먼트(시안 361 × 46) */
@Composable
internal fun RunSegments(selected: RunSegment, onSelect: (RunSegment) -> Unit, modifier: Modifier = Modifier) {
    StepUpTabs(
        items = listOf(stringResource(R.string.d26_seg_free), stringResource(R.string.d26_seg_crew)),
        selected = selected.ordinal,
        onSelect = { onSelect(RunSegment.entries[it]) },
        tags = listOf("run-seg-free", "run-seg-crew"),
        height = 46.dp,
        modifier = modifier.fillMaxWidth(),
    )
}

/** 큰 제목(시안 "러닝" · "크루 러닝") */
@Composable
internal fun RunTabTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = StepUpUi.type.largeTitle.copy(fontSize = 30.sp, letterSpacing = (-0.03).em), color = StepUpUi.colors.label,
        modifier = modifier.semantics { heading() }.testTag("run-tab-title"),
    )
}

/**
 * 내 소속 크루 카드 — 왼쪽 파란 띠 · 머리글자 원 · "내 소속 크루" · 이름 · 인원. 누르면 크루 홈.
 * 크루가 없으면 같은 자리에 "크루 찾기"(커뮤니티의 크루 목록).
 */
@Composable
internal fun MyCrewCard(crew: Crew?, onOpen: () -> Unit, onFind: () -> Unit, modifier: Modifier = Modifier) {
    val c = StepUpUi.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = 88.dp)
            .pressable(onClick = if (crew != null) onOpen else onFind)
            .clip(shape).background(c.card, shape).border(1.dp, c.separator, shape)
            .testTag(if (crew != null) "run-my-crew" else "run-no-crew"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(4.dp).height(88.dp).background(c.primary))
        Spacer(Modifier.width(14.dp))
        Box(Modifier.size(46.dp).clip(CircleShape).background(c.primary.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
            if (crew != null) {
                Text(
                    crew.monogram.ifBlank { crew.name.take(1) }.take(2).uppercase(),
                    style = StepUpUi.type.numberCard.copy(fontSize = 22.sp, fontStyle = FontStyle.Italic), color = c.primary,
                )
            } else {
                Icon(Icons.Outlined.Groups, contentDescription = null, tint = c.primary, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f).padding(vertical = 14.dp)) {
            Text(
                stringResource(if (crew != null) R.string.d26_my_crew else R.string.d26_no_crew_title),
                style = StepUpUi.type.caption, color = c.secondaryLabel,
            )
            Text(
                crew?.name ?: stringResource(R.string.d26_find_crew),
                style = StepUpUi.type.rowTitle.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold), color = c.label,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp).testTag("run-my-crew-name"),
            )
            Text(
                if (crew != null) stringResource(R.string.d26_crew_members, crew.memberCount) else stringResource(R.string.d26_no_crew_body),
                style = StepUpUi.type.rowSubtitle, color = c.secondaryLabel, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = c.label, modifier = Modifier.padding(end = 14.dp).size(22.dp))
    }
}

/** 오늘 걸음 — 왼쪽 "오늘 걸음" · 큰 숫자 · "/ 목표 걸음", 오른쪽 목표까지 고리(발자국). 누르면 오늘의 활동(E01) */
@Composable
internal fun StepsRing(fraction: Float, modifier: Modifier = Modifier) {
    val c = StepUpUi.colors
    Box(modifier.size(76.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = 5.dp.toPx()
            val inset = w / 2
            val arc = Size(size.width - w, size.height - w)
            drawArc(c.separator, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(w))
            if (fraction > 0f) drawArc(c.primary, -90f, 360f * fraction.coerceIn(0f, 1f), false, Offset(inset, inset), arc,
                style = Stroke(w, cap = StrokeCap.Round))
        }
        Icon(
            painterResource(R.drawable.ic_footprints_d26), contentDescription = null, tint = c.primary, modifier = Modifier.size(24.dp),
        )
    }
}

/** 배경 사진 카드(361 × 178) — 사람 없는 15장 중 하나, 왼쪽 아래 "나만의 속도로 / 오늘의 러닝을 시작해 볼까요?" */
@Composable
internal fun RunPhotoCard(photoIndex: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val name = RunPhotos.names[photoIndex.mod(RunPhotos.names.size)]
    val bitmap by produceState<ImageBitmap?>(initialValue = null, name) {
        value = withContext(Dispatchers.IO) { RunPhotos.decode(context, name) }
    }
    val shape = RoundedCornerShape(24.dp)
    Box(modifier.fillMaxWidth().height(178.dp).clip(shape).background(Color(0xFF0B1A33)).testTag("home-spot")) {
        bitmap?.let {
            Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().testTag("run-photo-$name"))
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(0f to Color.Black.copy(alpha = .55f), .7f to Color.Black.copy(alpha = .05f), 1f to Color.Transparent),
            ),
        )
        Column(Modifier.align(Alignment.CenterStart).padding(start = 20.dp, end = 20.dp, top = 24.dp)) {
            Text(stringResource(R.string.d26_photo_kicker), style = StepUpUi.type.caption.copy(fontSize = 13.sp), color = Color.White.copy(alpha = .9f))
            Text(
                stringResource(R.string.d26_photo_title),
                style = StepUpUi.type.largeTitle.copy(fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.03).em), color = Color.White,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** GPS 준비 줄 — 초록 점 · "GPS 준비 완료"(미리 잡은 위치가 10초 안 · 50m 안). 권한이 없으면 그렇게 쓴다 */
@Composable
internal fun GpsLine(state: GpsReadiness, modifier: Modifier = Modifier) {
    val c = StepUpUi.colors
    Row(modifier.testTag("run-gps-${state.name.lowercase()}"), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (state == GpsReadiness.READY) Color(0xFF3CC08A) else c.tertiaryLabel))
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(
                when (state) {
                    GpsReadiness.READY -> R.string.d26_gps_ready
                    GpsReadiness.WAITING -> R.string.d26_gps_waiting
                    GpsReadiness.NO_PERMISSION -> R.string.d26_gps_permission
                },
            ),
            style = StepUpUi.type.rowSubtitle, color = c.secondaryLabel,
        )
    }
}

/** 시안 크루 러닝(`lobby`) — 진행 중인 대기실 제목 · 개수와 방 목록. 목록은 보기만 한다(들어가는 것은 "대기실 보기") */
@Composable
internal fun CrewRoomsSection(ui: RoomsUi?, crewName: String, onOpenRoom: (Long) -> Unit, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val c = StepUpUi.colors
    Column(modifier.fillMaxWidth()) {
        val ready = ui as? RoomsUi.Ready
        val rooms = ready?.let { CrewRoomRules.ordered(it.rooms.rooms) }.orEmpty()
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.d26_rooms_title), style = StepUpUi.type.section.copy(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold),
                color = c.label, modifier = Modifier.weight(1f).semantics { heading() },
            )
            if (ready != null) Text(stringResource(R.string.d26_rooms_count, rooms.size), style = StepUpUi.type.body, color = c.secondaryLabel)
        }
        Spacer(Modifier.height(8.dp))
        when (ui) {
            null, RoomsUi.Loading -> Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                StepUpSpinner(Modifier.size(28.dp))
            }
            is RoomsUi.Failed -> RoomsState(
                Icons.Outlined.CloudOff,
                stringResource(if (ui.signIn) R.string.crew5_sign_in_title else R.string.crew5_rooms_failed),
                null, tag = "crew-rooms-failed",
                action = stringResource(R.string.crew5_rooms_reload) to onRetry,
            )
            is RoomsUi.Ready -> {
                if (ui.rooms.doneToday) {
                    Text(
                        stringResource(R.string.crew5_rooms_done_today), style = StepUpUi.type.rowSubtitle, color = c.accentText,
                        modifier = Modifier.padding(vertical = 6.dp).testTag("crew-rooms-done-today"),
                    )
                }
                if (rooms.isEmpty()) {
                    RoomsState(
                        Icons.Outlined.Route, stringResource(R.string.crew5_rooms_empty_title), stringResource(R.string.crew5_rooms_empty_body),
                        tag = "crew-rooms-empty",
                    )
                } else {
                    rooms.forEach { room -> RoomRow(room, crewName, onOpen = { onOpenRoom(room.id) }) }
                }
            }
        }
    }
}

@Composable
private fun RoomsState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String?,
    tag: String,
    action: Pair<String, () -> Unit>? = null,
) {
    val c = StepUpUi.colors
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp).testTag(tag), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = c.primary, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, style = StepUpUi.type.rowTitle.copy(fontWeight = FontWeight.Bold), color = c.label)
        if (body != null) {
            Text(body, style = StepUpUi.type.rowSubtitle, color = c.secondaryLabel, modifier = Modifier.padding(top = 4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        if (action != null) {
            Text(
                action.first + "  ›", style = StepUpUi.type.rowTitle.copy(fontWeight = FontWeight.Bold), color = c.accentText,
                modifier = Modifier.padding(top = 8.dp).heightIn(min = 44.dp).pressable(onClick = action.second).padding(8.dp)
                    .testTag("crew-rooms-retry"),
            )
        }
    }
}

/** 시각 — 오늘 · 내일이면 "오늘 19:00", 그 뒤면 "10/27 19:00" */
@Composable
private fun whenText(ms: Long): String {
    val zone = ZoneId.systemDefault()
    val at = Instant.ofEpochMilli(ms).atZone(zone)
    val hm = DateTimeFormatter.ofPattern("HH:mm").format(at)
    val today = LocalDate.now(zone)
    return when (at.toLocalDate()) {
        today -> stringResource(R.string.d26_when_today, hm)
        today.plusDays(1) -> stringResource(R.string.d26_when_tomorrow, hm)
        else -> DateTimeFormatter.ofPattern("M/d HH:mm").format(at)
    }
}

/** 방 한 줄(시안 `lobby`) — 모집 상태 · 방식, 이름, 시각 · 코스, 만나는 곳, 머리글자 · 인원, "대기실 보기" */
@Composable
private fun RoomRow(room: CrewRoom, crewName: String, onOpen: () -> Unit) {
    val c = StepUpUi.colors
    val canOpen = room.mine || !room.full
    Column(Modifier.fillMaxWidth().padding(vertical = 18.dp).testTag("crew-room-${room.id}")) {
        val modeText = stringResource(if (room.mode == RoomMode.REMOTE) R.string.d26_mode_remote else R.string.d26_mode_together)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(if (room.full && !room.mine) c.tertiaryLabel else c.primary))
            Spacer(Modifier.width(6.dp))
            Text(
                when {
                    room.mine -> stringResource(R.string.crew5_rooms_mine) + " · " + modeText
                    room.full -> stringResource(R.string.crew5_rooms_full) + " · " + modeText
                    else -> stringResource(R.string.d26_room_status, modeText)
                },
                style = StepUpUi.type.caption.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                color = if (room.full && !room.mine) c.secondaryLabel else c.accentText,
                modifier = Modifier.testTag("crew-room-mode-${room.mode.name.lowercase()}"),
            )
        }
        Text(
            room.name.ifBlank { crewName }, style = StepUpUi.type.section.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold), color = c.label,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp),
        )
        val time = if (room.planAtMs > 0) whenText(room.planAtMs) else stringResource(R.string.d26_when_open, whenText(room.createdAtMs))
        val course = room.course
        Text(
            if (course != null) stringResource(R.string.d26_room_course, time) else stringResource(R.string.d26_room_free, time),
            style = StepUpUi.type.rowSubtitle.copy(fontSize = 14.sp), color = c.secondaryLabel, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        val third = when {
            room.mode == RoomMode.REMOTE -> stringResource(R.string.d26_room_remote_line)
            room.meetPlace != null -> stringResource(R.string.d26_room_meet, room.meetPlace)
            course != null -> "${course.name} · ${com.stepup.android.ui.screens.walk.courseKm(course.km)}km"
            else -> stringResource(R.string.d26_room_host, room.hostName)
        }
        Text(
            third, style = StepUpUi.type.rowSubtitle.copy(fontSize = 14.sp), color = c.secondaryLabel, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp),
        )
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            room.memberNames.take(4).forEach { member ->
                Box(Modifier.padding(end = 6.dp).size(30.dp).clip(CircleShape).background(c.primary.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                    Text(member.trim().take(1).uppercase(), style = StepUpUi.type.caption.copy(fontWeight = FontWeight.Bold), color = c.primary)
                }
            }
            Text(
                stringResource(R.string.d26_room_count, room.members, room.capacity), style = StepUpUi.type.body, color = c.secondaryLabel,
                modifier = Modifier.padding(start = 2.dp).weight(1f).testTag("crew-room-count"),
            )
            if (canOpen) {
                val label = stringResource(if (room.mine) R.string.crew5_rooms_back else R.string.d26_room_view)
                val shape = RoundedCornerShape(50)
                Text(
                    label, style = StepUpUi.type.smallButton, color = c.label,
                    modifier = Modifier.heightIn(min = 40.dp).pressable(onClick = onOpen).clip(shape).border(1.dp, c.separator, shape)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .semantics { contentDescription = "${room.name} · $label" }
                        .testTag("crew-room-open-${room.id}"),
                )
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.separator))
}

/** 크루 러닝 칸의 주 행동 — "새 대기실 만들기" */
@Composable
internal fun NewRoomButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PrimaryButton(
        stringResource(R.string.d26_create_header), onClick,
        state = if (enabled) com.stepup.android.ui.components.ButtonState.Idle else com.stepup.android.ui.components.ButtonState.Disabled,
        modifier = modifier.fillMaxWidth().testTag("crew-rooms-new"),
    )
}

/** 러닝 탭이 화면에 있는 동안 미리 잡은 위치로 GPS 준비를 본다(2초마다) */
@Composable
internal fun rememberGpsReadiness(): GpsReadiness {
    val context = LocalContext.current
    val state by produceState(initialValue = GpsReadiness.WAITING) {
        while (true) {
            val fine = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            value = when {
                !fine -> GpsReadiness.NO_PERMISSION
                com.stepup.android.core.LocationWarmup.fresh(10_000)?.let { !it.hasAccuracy() || it.accuracy <= 50f } == true -> GpsReadiness.READY
                else -> GpsReadiness.WAITING
            }
            kotlinx.coroutines.delay(2_000)
        }
    }
    return state
}
