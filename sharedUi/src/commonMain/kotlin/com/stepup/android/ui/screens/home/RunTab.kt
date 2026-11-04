package com.stepup.android.ui.screens.home

import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlinx.datetime.toLocalDateTime
import com.stepup.android.domain.plusDaysKx
import kotlinx.coroutines.IO
import com.stepup.android.domain.todayIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.ui.res.*
import com.stepup.android.domain.CrewRoom
import com.stepup.android.domain.RoomMode
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.components.pressable
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.stepup.android.ui.components.StopBrush

/*
 * 러닝 탭 — 2026-10-26 전체 시안 `run`. 혼자 달리기만 둔다: 오늘 걸음(고리) · 사람 없는 배경 사진(15장 중 하나) · GPS 준비 ·
 * 오늘 거리 / 적립 / 이번 주 러닝 · "러닝 시작". 크루 러닝 · 번개러닝(대기실 목록 · 만들기)은 함께 뛰기 탭 한 곳에 있다
 * (2026-10-26 사용자 "한 탭에서 다 해결" — `community/TogetherRooms.kt`). 방 한 줄([RoomRow])은 그 목록이 쓴다.
 * 신고 있는 신발은 여기 없다 — 러닝을 시작할 때 신발 점검(자유러닝 시작)과 신발 탭에서 본다(사용자 결정).
 * 모든 숫자 · 이름은 실제 기록 · 서버 값이다. 시안의 "망원 러닝클럽" · "3,420" 같은 예시를 쓰지 않는다.
 */

/** 출발 전 위치 — 러닝 탭이 미리 잡아 둔 위치(LocationWarmup)로 본다 */
enum class GpsReadiness { READY, WAITING, NO_PERMISSION }

/** 사람 없는 배경 사진 15장(2026-10-26 전달본 02-backgrounds-15) — assets/run_photos */
object RunPhotos {
    val names = listOf(
        "01-blue-track", "02-midnight-stadium", "03-riverside-dawn", "04-bridge-path", "05-coastal-road",
        "06-forest-path", "07-city-night", "08-stadium-stairs", "09-running-tunnel", "10-rain-track",
        "11-lakeside-path", "12-mountain-ridge", "13-blue-court", "14-snow-promenade", "15-track-curve",
    )
    private val cache = LinkedHashMap<String, ImageBitmap>()

    /** files/run_photos/<이름>.webp — 안드로이드는 앱 assets 의 같은 사진. 세 장까지 기억한다 */
    @OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
    suspend fun decode(name: String): ImageBitmap? {
        cache[name]?.let { return it }
        return runCatching {
            Res.readBytes("files/run_photos/$name.webp").decodeToImageBitmap()
        }.getOrNull()?.also {
            cache[name] = it
            while (cache.size > 3) cache.remove(cache.keys.first())
        }
    }
}

/** 큰 제목(시안 "러닝" · "크루 러닝") */
@Composable
fun RunTabTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = StepUpUi.type.largeTitle.copy(fontSize = 30.sp, letterSpacing = (-0.03).em), color = StepUpUi.colors.label,
        modifier = modifier.semantics { heading() }.testTag("run-tab-title"),
    )
}

/** 오늘 걸음 — 왼쪽 "오늘 걸음" · 큰 숫자 · "/ 목표 걸음", 오른쪽 목표까지 고리(발자국). 누르면 오늘의 활동(E01) */
@Composable
fun StepsRing(fraction: Float, modifier: Modifier = Modifier) {
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
            painterResource(Res.drawable.ic_footprints_d26), contentDescription = null, tint = c.primary, modifier = Modifier.size(24.dp),
        )
    }
}

/** 배경 사진 카드(361 × 178) — 사람 없는 15장 중 하나, 왼쪽 아래 "나만의 속도로 / 오늘의 러닝을 시작해 볼까요?" */
@Composable
fun RunPhotoCard(photoIndex: Int, modifier: Modifier = Modifier) {
    val name = RunPhotos.names[photoIndex.mod(RunPhotos.names.size)]
    val bitmap by produceState<ImageBitmap?>(initialValue = null, name) {
        value = withContext(Dispatchers.IO) { RunPhotos.decode(name) }
    }
    val shape = RoundedCornerShape(24.dp)
    Box(modifier.fillMaxWidth().height(178.dp).clip(shape).background(Color(0xFF0B1A33)).testTag("home-spot")) {
        bitmap?.let {
            Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().testTag("run-photo-$name"))
        }
        Box(
            Modifier.fillMaxSize().background(
                StopBrush.horizontalGradient(0f to Color.Black.copy(alpha = .55f), .7f to Color.Black.copy(alpha = .05f), 1f to Color.Transparent),
            ),
        )
        Column(Modifier.align(Alignment.CenterStart).padding(start = 20.dp, end = 20.dp, top = 24.dp)) {
            Text(stringResource(Res.string.d26_photo_kicker), style = StepUpUi.type.caption.copy(fontSize = 13.sp), color = Color.White.copy(alpha = .9f))
            Text(
                stringResource(Res.string.d26_photo_title),
                style = StepUpUi.type.largeTitle.copy(fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.03).em), color = Color.White,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** GPS 준비 줄 — 초록 점 · "GPS 준비 완료"(미리 잡은 위치가 10초 안 · 50m 안). 권한이 없으면 그렇게 쓴다 */
@Composable
fun GpsLine(state: GpsReadiness, modifier: Modifier = Modifier) {
    val c = StepUpUi.colors
    Row(modifier.testTag("run-gps-${state.name.lowercase()}"), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (state == GpsReadiness.READY) Color(0xFF3CC08A) else c.tertiaryLabel))
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(
                when (state) {
                    GpsReadiness.READY -> Res.string.d26_gps_ready
                    GpsReadiness.WAITING -> Res.string.d26_gps_waiting
                    GpsReadiness.NO_PERMISSION -> Res.string.d26_gps_permission
                },
            ),
            style = StepUpUi.type.rowSubtitle, color = c.secondaryLabel,
        )
    }
}

/** 목록 상태 칸 — 아이콘 · 제목 · 설명 · 다시 시도(함께 뛰기 목록이 쓴다) */
@Composable
fun RoomsState(
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
fun whenText(ms: Long): String {
    val zone = TimeZone.currentSystemDefault()
    val at = kotlin.time.Instant.fromEpochMilliseconds(ms).toLocalDateTime(zone)
    val hm = com.stepup.android.domain.PatternFormatter("HH:mm", androidx.compose.ui.text.intl.Locale.current.language).format(at)
    val today = todayIn(zone)
    return when (at.date) {
        today -> stringResource(Res.string.d26_when_today, hm)
        today.plusDaysKx(1) -> stringResource(Res.string.d26_when_tomorrow, hm)
        else -> com.stepup.android.domain.PatternFormatter("M/d HH:mm", androidx.compose.ui.text.intl.Locale.current.language).format(at)
    }
}

/**
 * 방 한 줄(시안 `lobby`) — 모집 상태 · 방식(· 크루 이름), 이름, 시각 · 코스, 만나는 곳, 머리글자 · 인원, "대기실 보기".
 * 함께 뛰기 목록은 여러 크루의 방을 한 줄로 늘어놓으니 [showCrew] 면 상태 줄 끝에 크루 이름을 붙인다.
 */
@Composable
fun RoomRow(room: CrewRoom, crewName: String, onOpen: () -> Unit, showCrew: Boolean = false) {
    val c = StepUpUi.colors
    val canOpen = room.mine || !room.full
    Column(Modifier.fillMaxWidth().padding(vertical = 18.dp).testTag("crew-room-${room.id}")) {
        val modeText = stringResource(if (room.mode == RoomMode.REMOTE) Res.string.d26_mode_remote else Res.string.d26_mode_together)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(if (room.full && !room.mine) c.tertiaryLabel else c.primary))
            Spacer(Modifier.width(6.dp))
            Text(
                when {
                    room.mine -> stringResource(Res.string.crew5_rooms_mine) + " · " + modeText
                    room.full -> stringResource(Res.string.crew5_rooms_full) + " · " + modeText
                    else -> stringResource(Res.string.d26_room_status, modeText)
                } + if (showCrew && crewName.isNotBlank()) " · $crewName" else "",
                style = StepUpUi.type.caption.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                color = if (room.full && !room.mine) c.secondaryLabel else c.accentText,
                modifier = Modifier.testTag("crew-room-mode-${room.mode.name.lowercase()}"),
            )
        }
        Text(
            room.name.ifBlank { crewName }, style = StepUpUi.type.section.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold), color = c.label,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp),
        )
        val time = if (room.planAtMs > 0) whenText(room.planAtMs) else stringResource(Res.string.d26_when_open, whenText(room.createdAtMs))
        val course = room.course
        Text(
            if (course != null) stringResource(Res.string.d26_room_course, time) else stringResource(Res.string.d26_room_free, time),
            style = StepUpUi.type.rowSubtitle.copy(fontSize = 14.sp), color = c.secondaryLabel, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        val third = when {
            room.mode == RoomMode.REMOTE -> stringResource(Res.string.d26_room_remote_line)
            room.meetPlace != null -> stringResource(Res.string.d26_room_meet, room.meetPlace!!)
            course != null -> "${course.name} · ${com.stepup.android.ui.screens.walk.courseKm(course.km)}km"
            else -> stringResource(Res.string.d26_room_host, room.hostName)
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
                stringResource(Res.string.d26_room_count, room.members, room.capacity), style = StepUpUi.type.body, color = c.secondaryLabel,
                modifier = Modifier.padding(start = 2.dp).weight(1f).testTag("crew-room-count"),
            )
            if (canOpen) {
                val label = stringResource(if (room.mine) Res.string.crew5_rooms_back else Res.string.d26_room_view)
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

/** 러닝 탭이 화면에 있는 동안 미리 잡은 위치로 GPS 준비를 본다(2초마다) */
@Composable
fun rememberGpsReadiness(): GpsReadiness {
    val state by produceState(initialValue = GpsReadiness.WAITING) {
        while (true) {
            // 안드로이드: 정확한 위치 권한 · 미리 잡은 위치(10초 안 · 50m 안). 공용: 정확한 위치 권한이 있으면 바로 준비(iOS 는 러닝이 켜질 때 잡는다)
            value = if (com.stepup.android.ui.StepPermissions.hasPreciseLocation()) GpsReadiness.READY else GpsReadiness.NO_PERMISSION
            kotlinx.coroutines.delay(2_000)
        }
    }
    return state
}
