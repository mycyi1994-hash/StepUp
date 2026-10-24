package com.stepup.android.ui.screens.walk

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.repo.BoardSyncState
import com.stepup.android.domain.CrewRoom
import com.stepup.android.domain.CrewRoomRules
import com.stepup.android.domain.CrewRooms
import com.stepup.android.domain.CrewRunMath
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.RoomCourse
import com.stepup.android.domain.RunCourse
import com.stepup.android.ui.components.LiveRouteMap
import com.stepup.android.ui.components.LocalMapTone
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpSans
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * 크루 러닝 — 열린 대기실(02) · 러닝 만들기(03 · 06) · 코스 고르기(04 · 09) · 코스 상세(05). 2026-10-24 핸드오프, 서버 0072.
 *
 * 배경은 앱의 기존 크루 화면 바탕(crewColors — APP_EXISTING)이다. 터널 사진은 대기실에만 둔다. 승인한 듀얼 레일 틀은 방 카드 ·
 * 코스 묶음에 쓴다. 목록은 보기만 한다 — 들어가는 것은 사용자가 방을 고를 때뿐이다. 방을 연 것만으로 하루 한 번은 쓰이지 않는다.
 */

/** 열린 대기실 목록의 상태 */
internal sealed interface RoomsUi {
    data object Loading : RoomsUi
    data class Failed(val signIn: Boolean) : RoomsUi
    data class Ready(val rooms: CrewRooms) : RoomsUi
}

/** 목록을 다시 묻는 간격 — 화면이 보이는 동안만(인원 · 준비 수가 바뀐다) */
private const val ROOMS_REFRESH_MS = 10_000L

/**
 * 열린 대기실(02) — 같은 크루의 대기실과 인원 · 코스 · 진행자. 방을 고르면 그 방으로([onOpenRoom]), "새 대기실 만들기"는 러닝 만들기로.
 * 오늘 이미 크루 러닝을 했으면 새로 열 수 없다(서버도 막는다 — 목록은 그대로 보인다).
 */
@Composable
fun CrewRoomsScreen(
    crewId: String,
    onBack: () -> Unit,
    onOpenRoom: (Long) -> Unit,
    onCreate: () -> Unit,
) {
    val repo = ServiceLocator.crewRepository
    var ui by remember { mutableStateOf<RoomsUi>(RoomsUi.Loading) }
    var reload by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(crewId, reload) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                ui = when (val result = repo.rooms(crewId)) {
                    is ServerResult.Ok -> RoomsUi.Ready(result.value)
                    // 받았던 목록이 있으면 잠깐 끊긴 것으로 목록을 비우지 않는다(오류를 빈 목록으로 바꾸지 않는다 — 처음이면 실패)
                    is ServerResult.Retry -> (ui as? RoomsUi.Ready) ?: RoomsUi.Failed(signIn = false)
                    is ServerResult.SignInRequired -> RoomsUi.Failed(signIn = true)
                    is ServerResult.Rejected -> RoomsUi.Failed(signIn = false)
                }
                delay(ROOMS_REFRESH_MS)
            }
        }
    }
    val crewName = repo.crewOf(crewId)?.name.orEmpty()
    CrewRoomsContent(
        ui = ui, crewName = crewName, onBack = onBack, onOpenRoom = onOpenRoom, onCreate = onCreate,
        onRetry = {
            ui = RoomsUi.Loading
            reload++
        },
    )
}

@Composable
internal fun CrewRoomsContent(
    ui: RoomsUi,
    crewName: String,
    onBack: () -> Unit,
    onOpenRoom: (Long) -> Unit,
    onCreate: () -> Unit,
    onRetry: () -> Unit,
) {
    val c = crewColors()
    val ready = ui as? RoomsUi.Ready
    val done = ready?.rooms?.doneToday == true
    CrewPage(
        title = stringResource(R.string.crew5_rooms_header), onBack = onBack, badge = false,
        modifier = Modifier.testTag(
            when (ui) {
                RoomsUi.Loading -> "crew-rooms-loading"
                is RoomsUi.Failed -> "crew-rooms-failed"
                is RoomsUi.Ready -> "crew-rooms"
            },
        ),
        bottom = {
            when (ui) {
                is RoomsUi.Failed -> FreeRunButton(stringResource(R.string.crew5_rooms_reload), onRetry, Modifier.fillMaxWidth().testTag("crew-rooms-retry"))
                // 주 행동 하나 — 새 대기실. 오늘 이미 했으면 막는다(이유는 위 안내)
                else -> FreeRunButton(
                    stringResource(R.string.crew5_rooms_new), onCreate, Modifier.fillMaxWidth().testTag("crew-rooms-new"),
                    enabled = ready != null && !done,
                )
            }
        },
    ) {
        Spacer(Modifier.height(8.dp))
        CrewBigTitle(stringResource(R.string.crew5_rooms_title), stringResource(R.string.crew5_rooms_sub))
        Spacer(Modifier.height(18.dp))
        when (ui) {
            RoomsUi.Loading -> Box(Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                RunSpinner(Modifier.size(40.dp))
            }
            is RoomsUi.Failed -> CrewCenterState(
                Icons.Outlined.CloudOff,
                stringResource(if (ui.signIn) R.string.crew5_sign_in_title else R.string.crew5_rooms_failed), null,
            )
            is RoomsUi.Ready -> {
                if (done) {
                    CrewNote(stringResource(R.string.crew5_rooms_done_today), tag = "crew-rooms-done-today")
                    Spacer(Modifier.height(14.dp))
                }
                val rooms = CrewRoomRules.ordered(ui.rooms.rooms)
                if (rooms.isEmpty()) {
                    CrewCenterState(
                        Icons.Outlined.Route, stringResource(R.string.crew5_rooms_empty_title),
                        stringResource(R.string.crew5_rooms_empty_body), modifier = Modifier.testTag("crew-rooms-empty"),
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        rooms.forEach { room -> RoomCard(room, crewName, onOpen = { onOpenRoom(room.id) }) }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/** 큰 제목 + 한 줄(시안 31sp / 37sp · 보조 12sp) — 앱 바탕 위 글자 색 */
@Composable
internal fun CrewBigTitle(title: String, sub: String?, modifier: Modifier = Modifier) {
    val c = crewColors()
    Column(modifier.fillMaxWidth()) {
        Text(
            title,
            style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 31.sp, lineHeight = 37.sp,
                letterSpacing = (-0.03).em, color = c.text),
            modifier = Modifier.semantics { heading() }.testTag("crew-big-title"),
        )
        if (sub != null) {
            Spacer(Modifier.height(7.dp))
            Text(sub, style = runTextStyle(13.sp, c.label, FontWeight.Medium, 1.55f))
        }
    }
}

/** 가운데 상태(빈 목록 · 실패) — 아이콘 · 제목 · 설명 */
@Composable
internal fun CrewCenterState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String?,
    modifier: Modifier = Modifier,
) {
    val c = crewColors()
    Column(modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(88.dp).clip(RoundedCornerShape(26.dp)).background(c.inset), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = c.link, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(19.dp))
        Text(
            title, style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 32.sp,
                letterSpacing = (-0.03).em, color = c.text),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.semantics { heading() },
        )
        if (body != null) {
            Spacer(Modifier.height(8.dp))
            Text(body, style = runTextStyle(14.sp, c.label, FontWeight.Medium, 1.55f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

/** 방을 연 시각(이 폰의 시각) — "19:30" */
internal fun roomTime(ms: Long): String =
    if (ms <= 0L) "" else DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

/** 거리 "5" · "5.2" — 소수 한 자리(0 이면 뗀다) */
internal fun courseKm(km: Double): String =
    if (km % 1.0 < 0.05 || km % 1.0 > 0.95) "%.0f".format(java.util.Locale.US, km) else "%.1f".format(java.util.Locale.US, km)

/** 방 카드(02) — 듀얼 레일 틀 · 이름 · 인원 칩 · 코스 · 지도 · 진행자. 가득 찬 방은 누르지 않는다(내 방이면 들어간다) */
@Composable
private fun RoomCard(room: CrewRoom, crewName: String, onOpen: () -> Unit) {
    val canOpen = room.mine || !room.full
    val enter = stringResource(if (room.mine) R.string.crew5_rooms_back else R.string.crew5_rooms_enter)
    DualRailFrame(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .then(if (canOpen) Modifier.feedbackClickable(role = Role.Button, onClick = onOpen) else Modifier.semantics { disabled() })
            .semantics { contentDescription = "${room.name} · $enter" }
            .testTag("crew-room-${room.id}"),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    room.name.ifBlank { crewName }, style = navyText(16.sp, LobbyInk.Text, FontWeight.Bold), maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                when {
                    room.mine -> LobbyChip(stringResource(R.string.crew5_rooms_mine), kind = LobbyChipKind.Soft)
                    room.full -> LobbyChip(stringResource(R.string.crew5_rooms_full), kind = LobbyChipKind.Warn)
                    else -> LobbyChip(stringResource(R.string.crew5_rooms_count, room.members, room.capacity), Modifier.testTag("crew-room-count"))
                }
            }
            room.course?.let { course ->
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.crew5_rooms_course, course.name, courseKm(course.km)),
                    style = navyText(14.sp, LobbyInk.Sub, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(12.dp))
                CrewCourseMap(course.points, Modifier.fillMaxWidth().height(150.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.crew5_rooms_host, room.hostName, roomTime(room.createdAtMs), room.ready),
                style = navyText(12.sp, LobbyInk.Sub, FontWeight.Medium, 1.5f), maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            if (canOpen) {
                Spacer(Modifier.height(4.dp))
                Text("$enter  ›", style = navyText(14.sp, LobbyInk.Ice, FontWeight.Bold), modifier = Modifier.heightIn(min = 24.dp))
            }
        }
    }
}

/** 코스 지도 — 실제 지도(밝은 거리 지도, 크루 러닝과 같다) 위 경로 · 출발 점. 경로를 아직 받지 못했으면 안내만 */
@Composable
internal fun CrewCourseMap(points: List<GeoPoint>, modifier: Modifier = Modifier, interactive: Boolean = false) {
    val shape = RoundedCornerShape(15.dp)
    Box(modifier.clip(shape).background(LobbyInk.Map, shape)) {
        if (points.size >= 2) {
            CompositionLocalProvider(LocalMapTone provides CrewBrightMap) {
                LiveRouteMap(points = points, modifier = Modifier.fillMaxSize(), routeColor = LobbyInk.Accent, interactive = interactive)
            }
        } else {
            Text(
                stringResource(R.string.crew5_course_loading), style = navyText(12.sp, LobbyInk.Sub, FontWeight.Medium),
                modifier = Modifier.align(Alignment.Center).padding(12.dp),
            )
        }
    }
}

// ── 러닝 만들기 ────────────────────────────────────────────────────

/** 고른 코스를 화면 회전 · 잠깐 꺼짐에도 지킨다(이름 · 거리 · 경로) */
private val RoomCourseSaver = Saver<RoomCourse?, List<String>>(
    save = { course -> course?.let { listOf(it.name, it.km.toString(), it.encodeTrack()) } ?: emptyList() },
    restore = { saved ->
        if (saved.size < 3) null else RoomCourse(0, saved[0], saved[1].toDoubleOrNull() ?: 0.0, RunCourse.decode(saved[2]))
    },
)

/** 러닝 만들기의 실패 — 입력 · 고른 코스는 그대로 둔다 */
internal sealed interface CreateProblem {
    data object DoneToday : CreateProblem
    data object Network : CreateProblem
    data class Rejected(val reason: String) : CreateProblem
}

/**
 * 러닝 만들기(03 · 06) — 이름 · 코스(목록 → 상세 → 고르기) · 공개 범위(이 크루) · 내 신발 기여 미리보기 → "대기실 열기".
 * 연타는 막고 서버가 방 번호를 준 뒤에만 대기실로 간다([onCreated]). 실패하면 이름 · 코스를 그대로 두고 까닭을 알린다.
 * 같은 이름 · 코스로 다시 누르면 같은 요청 번호 — 답을 잃었어도 방은 하나다.
 */
@Composable
fun CrewRoomCreateScreen(crewId: String, onBack: () -> Unit, onCreated: (Long) -> Unit) {
    val repo = ServiceLocator.crewRepository
    var name by rememberSaveable { mutableStateOf("") }
    var course by rememberSaveable(stateSaver = RoomCourseSaver) { mutableStateOf<RoomCourse?>(null) }
    var picking by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<CreateProblem?>(null) }
    // 같은 입력으로 다시 보내면 같은 번호(서버가 그때 연 방을 돌려준다), 입력이 바뀌면 새 번호
    val requestKey = rememberSaveable(name.trim(), course?.encodeTrack()?.hashCode(), course?.name) { UUID.randomUUID().toString() }
    val scope = rememberCoroutineScope()
    val crew = repo.crewOf(crewId)
    val today by remember(crewId) { ServiceLocator.crewRun.today(crewId) }
        .collectAsStateWithLifecycle(initialValue = ServiceLocator.crewRun.todayNow(crewId))
    LaunchedEffect(crewId) { ServiceLocator.crewRun.loadToday(crewId) }
    val shoes by ServiceLocator.sneakerRepository.inventory.collectAsStateWithLifecycle(initialValue = emptyList())
    val equipped = shoes.firstOrNull { it.equipped && it.server?.hasDurabilityMax == true }
    val level = today?.takeIf { it.isToday() }?.crewLevel ?: 1

    if (picking) {
        BackHandler { picking = false }
        CrewCoursePicker(
            current = course,
            onBack = { picking = false },
            onPick = {
                course = it
                picking = false
            },
        )
        return
    }
    CrewRoomCreateContent(
        crewName = crew?.name.orEmpty(), name = name, onName = {
            name = it
            problem = null
        },
        course = course, onPickCourse = { picking = true },
        shoeMax = equipped?.server?.durabilityMax, level = level,
        busy = busy, problem = problem, onBack = onBack,
        onOpen = {
            val chosen = course
            if (busy || chosen == null || !CrewRoomRules.nameValid(name)) return@CrewRoomCreateContent
            busy = true
            problem = null
            scope.launch {
                val result = repo.createRoom(crewId, CrewRoomRules.normalizeName(name), chosen, requestKey)
                busy = false
                when (result) {
                    is ServerResult.Ok -> onCreated(result.value)
                    is ServerResult.Retry -> problem = CreateProblem.Network
                    is ServerResult.Rejected -> problem =
                        if (result.reason.contains("crew_run_done_today")) CreateProblem.DoneToday else CreateProblem.Rejected(result.reason)
                    is ServerResult.SignInRequired -> problem = CreateProblem.Rejected(result.reason)
                }
            }
        },
    )
}

@Composable
internal fun CrewRoomCreateContent(
    crewName: String,
    name: String,
    onName: (String) -> Unit,
    course: RoomCourse?,
    onPickCourse: () -> Unit,
    shoeMax: Int?,
    level: Int,
    busy: Boolean,
    problem: CreateProblem?,
    onBack: () -> Unit,
    onOpen: () -> Unit,
) {
    val c = crewColors()
    val length = CrewRoomRules.nameLength(name)
    val nameOk = CrewRoomRules.nameValid(name)
    val focus = LocalFocusManager.current
    CrewPage(
        title = stringResource(R.string.crew5_create_header), onBack = onBack, badge = false,
        modifier = Modifier.testTag(if (course == null) "crew-create" else "crew-create-selected"),
        bottom = {
            FreeRunButton(
                stringResource(R.string.crew5_create_open), onOpen, Modifier.fillMaxWidth().testTag("crew-create-open"),
                enabled = course != null && nameOk, busy = busy,
            )
        },
    ) {
        Spacer(Modifier.height(8.dp))
        CrewBigTitle(stringResource(R.string.crew5_create_title), stringResource(R.string.crew5_create_sub, CrewRunMath.ROOM_MAX))
        Spacer(Modifier.height(20.dp))
        // 러닝 이름 — 1 ~ 30자(서버와 같은 셈)
        Text(stringResource(R.string.crew5_create_name), style = runTextStyle(13.sp, c.label, FontWeight.Medium))
        Spacer(Modifier.height(8.dp))
        val shape = RoundedCornerShape(10.dp)
        val nameLabel = stringResource(R.string.crew5_create_name)
        BasicTextField(
            value = name,
            onValueChange = { v -> onName(v.take(CrewRoomRules.NAME_MAX * 2)) },
            singleLine = true,
            textStyle = runTextStyle(16.sp, c.text, FontWeight.Medium),
            cursorBrush = SolidColor(c.link),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(shape).background(c.card, shape)
                .border(1.5.dp, if (name.isNotEmpty() && !nameOk) com.stepup.android.ui.components.runTone().errorEdge else c.line, shape)
                .semantics { contentDescription = nameLabel }
                .testTag("crew-create-name"),
            decorationBox = { inner ->
                Box(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), contentAlignment = Alignment.CenterStart) {
                    if (name.isEmpty()) Text(stringResource(R.string.crew5_create_name_hint), style = runTextStyle(16.sp, c.muted, FontWeight.Medium))
                    inner()
                }
            },
        )
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            if (name.isNotEmpty() && !nameOk) {
                Text(
                    stringResource(R.string.crew5_create_name_invalid, CrewRoomRules.NAME_MAX),
                    style = runTextStyle(12.sp, com.stepup.android.ui.components.runTone().errorIcon, FontWeight.Medium),
                    modifier = Modifier.weight(1f).testTag("crew-create-name-invalid"),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            Text(stringResource(R.string.crew5_create_name_count, length, CrewRoomRules.NAME_MAX), style = runTextStyle(12.sp, c.muted))
        }
        Spacer(Modifier.height(14.dp))
        // 함께 달릴 코스 — 듀얼 레일 묶음. 고르기 전에는 큰 "코스 선택하기", 고른 뒤에는 이름 · 거리 · 지도 · 다른 코스 고르기
        DualRailFrame(Modifier.fillMaxWidth().testTag("crew-create-course"), padding = androidx.compose.foundation.layout.PaddingValues(18.dp)) {
            Column(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.crew5_create_course), style = navyText(13.sp, LobbyInk.Sub, FontWeight.SemiBold))
                Spacer(Modifier.height(10.dp))
                if (course == null) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(RoundedCornerShape(14.dp)).background(LobbyInk.Text)
                            .feedbackClickable(role = Role.Button, onClick = onPickCourse).padding(horizontal = 18.dp)
                            .testTag("crew-create-pick-course"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.crew5_create_pick_course), style = navyText(18.sp, LobbyInk.PrimaryInk, FontWeight.ExtraBold),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(1f),
                        )
                        Icon(Icons.Outlined.Tune, contentDescription = null, tint = LobbyInk.Accent, modifier = Modifier.size(20.dp))
                    }
                } else {
                    Text(course.name, style = navyText(18.sp, LobbyInk.Text, FontWeight.ExtraBold), modifier = Modifier.testTag("crew-create-course-name"))
                    Text(stringResource(R.string.crew5_courses_item_km, courseKm(course.km)), style = navyText(13.sp, LobbyInk.Sub, FontWeight.Medium))
                    Spacer(Modifier.height(10.dp))
                    CrewCourseMap(course.points, Modifier.fillMaxWidth().height(150.dp))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.crew5_create_other_course) + "  ›", style = navyText(14.sp, LobbyInk.Ice, FontWeight.Bold),
                        modifier = Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp))
                            .feedbackClickable(role = Role.Button, onClick = onPickCourse).padding(vertical = 13.dp)
                            .testTag("crew-create-change-course"),
                    )
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        CreateRow(stringResource(R.string.crew5_create_scope), stringResource(R.string.crew5_create_scope_value, crewName), null)
        CrewLine()
        // 내 신발의 기여 미리보기 — 최대 내구도 × (1 + 레벨 효과). 남은 개인 내구도가 아니다. 출발할 때 서버가 실제 값을 더한다
        if (shoeMax != null) {
            val factor = 1 + CrewRunMath.levelStamina(level)
            CreateRow(
                stringResource(R.string.crew5_create_contribution),
                "+" + CrewRunMath.amount(CrewRunMath.contribution(shoeMax, level)),
                stringResource(R.string.crew5_create_contribution_sub, shoeMax, level, "%.2f".format(java.util.Locale.US, factor)),
            )
        } else {
            CreateRow(stringResource(R.string.crew5_create_contribution), "", stringResource(R.string.crew5_create_contribution_none))
        }
        Spacer(Modifier.height(12.dp))
        when (problem) {
            null -> CrewNote(stringResource(R.string.crew5_create_note), tag = "crew-create-note")
            CreateProblem.DoneToday -> CrewError(stringResource(R.string.crew5_create_failed_done), tag = "crew-create-done-today")
            CreateProblem.Network -> CrewError(stringResource(R.string.crew5_create_failed_network), tag = "crew-create-network")
            is CreateProblem.Rejected -> CrewError(stringResource(R.string.crew5_create_failed, problem.reason), tag = "crew-create-failed")
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun CreateRow(label: String, value: String, sub: String?) {
    val c = crewColors()
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = runTextStyle(14.sp, c.label, FontWeight.Medium))
            if (sub != null) Text(sub, style = runTextStyle(12.sp, c.muted, FontWeight.Medium, 1.4f))
        }
        if (value.isNotEmpty()) Text(value, style = runTextStyle(15.sp, c.text, FontWeight.Bold))
    }
}

// ── 코스 고르기 ────────────────────────────────────────────────────

/**
 * 코스 고르기(04 · 09) — 내 코스(이 폰)와 공유 코스(서버 게시판). 누르면 상세(05)가 뜨고, "이 코스로 달리기"를 눌러야 고른다.
 * 목록에서 스치거나 뒤로 나오면 원래 코스 그대로다. 빈 목록과 불러오기 실패를 따로 보인다.
 */
@Composable
internal fun CrewCoursePicker(current: RoomCourse?, onBack: () -> Unit, onPick: (RoomCourse) -> Unit) {
    val courseRepo = ServiceLocator.courseRepository
    val mine by courseRepo.courses.collectAsStateWithLifecycle(initialValue = emptyList())
    val board by courseRepo.board.collectAsStateWithLifecycle(initialValue = emptyList())
    val sync by courseRepo.boardSync.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { courseRepo.refreshBoard() }
    var detail by remember { mutableStateOf<RunCourse?>(null) }
    CrewCoursePickerContent(
        current = current,
        // 내 코스 = 이 폰에서 내가 만든 코스, 공유 코스 = 게시판(서버 · 기본 코스) 중 남이 만든 것
        mine = mine.filter { it.mine && CrewRoomRules.usable(it) },
        shared = board.filter { !it.mine && CrewRoomRules.usable(it) },
        sync = sync,
        onBack = onBack,
        onOpen = { detail = it },
        onRetry = { scope.launch { courseRepo.refreshBoard() } },
    )
    detail?.let { course ->
        CrewCourseDetailSheet(
            course = RoomCourse.of(course), area = course.area,
            onPick = {
                detail = null
                onPick(RoomCourse.of(course))
            },
            onDismiss = { detail = null },
        )
    }
}

@Composable
internal fun CrewCoursePickerContent(
    current: RoomCourse?,
    mine: List<RunCourse>,
    shared: List<RunCourse>,
    sync: BoardSyncState,
    onBack: () -> Unit,
    onOpen: (RunCourse) -> Unit,
    onRetry: () -> Unit,
) {
    CrewPage(
        title = stringResource(R.string.crew5_courses_header), onBack = onBack, badge = false,
        modifier = Modifier.testTag(if (mine.isEmpty() && shared.isEmpty()) "crew-courses-empty" else "crew-courses"),
    ) {
        val c = crewColors()
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.crew5_courses_mine), style = runTextStyle(15.sp, c.text, FontWeight.Bold), modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(8.dp))
        if (mine.isEmpty()) {
            CrewCenterState(
                Icons.Outlined.Route, stringResource(R.string.crew5_courses_empty_title), stringResource(R.string.crew5_courses_empty_body),
                modifier = Modifier.testTag("crew-courses-mine-empty"),
            )
        } else {
            mine.forEach { CourseRow(it, current, onOpen) }
        }
        Spacer(Modifier.height(18.dp))
        Text(stringResource(R.string.crew5_courses_shared), style = runTextStyle(15.sp, c.text, FontWeight.Bold), modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(8.dp))
        when {
            shared.isNotEmpty() -> shared.forEach { CourseRow(it, current, onOpen) }
            sync is BoardSyncState.Failed || sync == BoardSyncState.SignInRequired -> {
                CrewError(stringResource(R.string.crew5_courses_shared_failed), tag = "crew-courses-shared-failed")
                CrewTextAction(stringResource(R.string.crew5_rooms_reload), onRetry, Modifier.testTag("crew-courses-retry"), chevron = true)
            }
            sync == BoardSyncState.Loading || sync == BoardSyncState.Idle ->
                Row(Modifier.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    RunSpinner(Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.crew5_courses_loading), style = runTextStyle(14.sp, c.label, FontWeight.Medium))
                }
            else -> Text(stringResource(R.string.crew5_courses_empty_title), style = runTextStyle(14.sp, c.label, FontWeight.Medium))
        }
        Spacer(Modifier.height(20.dp))
    }
}

/** 코스 한 줄 — 이름 · 거리 · 동네. 지금 코스면 표시(같은 경로) */
@Composable
private fun CourseRow(course: RunCourse, current: RoomCourse?, onOpen: (RunCourse) -> Unit) {
    val c = crewColors()
    val isCurrent = current != null && current.name == CrewRoomRules.courseName(course.name) &&
        current.points.size == CrewRoomRules.fitTrack(course.points).size
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(12.dp))
            .feedbackClickable(role = Role.Button, onClick = { onOpen(course) }).padding(horizontal = 4.dp, vertical = 10.dp)
            .testTag("crew-course-${course.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(course.name, style = runTextStyle(16.sp, c.text, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (course.area.isNotBlank()) stringResource(R.string.crew5_courses_item, courseKm(course.distanceKm), course.area)
                else stringResource(R.string.crew5_courses_item_km, courseKm(course.distanceKm)),
                style = runTextStyle(13.sp, c.label, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (isCurrent) {
            Text(stringResource(R.string.crew5_courses_current), style = runTextStyle(12.sp, c.link, FontWeight.Bold))
            Spacer(Modifier.width(8.dp))
        }
        Text("›", style = runTextStyle(18.sp, c.muted, FontWeight.Bold))
    }
    CrewLine()
}

/**
 * 코스 상세(05) — 전체 경로 · 거리 · 출발 점. "이 코스로 달리기"를 눌러야 고른다(닫기 · 바깥 · 뒤로는 그대로).
 * 대기실의 코스 보기(25) · 바뀐 코스(08)도 이 모습이다 — 그때는 [onPick] 이 없고 확인만 있다.
 */
@Composable
internal fun CrewCourseDetailSheet(
    course: RoomCourse,
    area: String?,
    onPick: (() -> Unit)?,
    onDismiss: () -> Unit,
    title: String = course.name,
    note: String? = null,
    confirm: String? = null,
) {
    val c = crewColors()
    RunSheet(onDismiss = onDismiss, modifier = Modifier.testTag(if (onPick != null) "crew-course-detail" else "crew-course-view")) {
        CrewSheetTitle(title)
        Spacer(Modifier.height(6.dp))
        Text(
            listOfNotNull(stringResource(R.string.crew5_course_detail_meta, courseKm(course.km)), area?.takeIf { it.isNotBlank() }).joinToString(" · "),
            style = runTextStyle(13.sp, c.label, FontWeight.Medium),
        )
        if (title != course.name) {
            Text(course.name, style = runTextStyle(16.sp, c.text, FontWeight.Bold), modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(12.dp))
        CrewCourseMap(course.points, Modifier.fillMaxWidth().height(280.dp), interactive = true)
        if (note != null) {
            Spacer(Modifier.height(12.dp))
            CrewNote(note)
        }
        Spacer(Modifier.height(16.dp))
        if (onPick != null) {
            FreeRunButton(stringResource(R.string.crew5_course_pick), onPick, Modifier.fillMaxWidth().testTag("crew-course-pick"))
            Spacer(Modifier.height(8.dp))
            FreeRunButton(stringResource(R.string.crew5_course_other), onDismiss, Modifier.fillMaxWidth().testTag("crew-course-other"),
                kind = FreeButtonKind.Secondary)
        } else {
            FreeRunButton(confirm ?: stringResource(R.string.common_confirm), onDismiss, Modifier.fillMaxWidth().testTag("crew-course-ok"))
        }
    }
}
