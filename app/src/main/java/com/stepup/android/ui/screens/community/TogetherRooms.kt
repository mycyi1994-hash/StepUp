package com.stepup.android.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.repo.BoardSyncState
import com.stepup.android.domain.CrewRoom
import com.stepup.android.domain.CrewRoomRules
import com.stepup.android.domain.CrewRooms
import com.stepup.android.domain.Post
import com.stepup.android.ui.components.GroupHeader
import com.stepup.android.ui.components.GroupRow
import com.stepup.android.ui.components.GroupedList
import com.stepup.android.ui.components.PrimaryButton
import com.stepup.android.ui.components.StepUpSpinner
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.components.pressable
import com.stepup.android.ui.screens.home.RoomRow
import com.stepup.android.ui.screens.home.RoomsState
import com.stepup.android.ui.screens.home.whenText
import com.stepup.android.ui.screens.walk.RoomsUi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.util.Locale

/*
 * 함께 뛰기 탭의 첫 칸 "모집 중"(2026-10-26 사용자 "크루달리기 · 번개달리기를 한 탭에서 다 해결") — 지금 들어갈 수 있는 모임을 한 목록에.
 *
 *   크루 러닝: 내가 든 크루 모두의 열린 대기실(서버 party_rooms, 크루마다) — 방 한 줄은 러닝 탭에 있던 [RoomRow] 그대로, 끝에 크루 이름.
 *   번개러닝: 전체 게시판의 앞으로 열릴 번개(같이 뛰기 · 원격 뛰기, 0076). "모두 보기"는 지난 · 찬 번개까지 모은 예전 목록.
 *   아래 주 버튼 하나 "함께 뛰기 만들기" → 누구와(크루마다 · 누구나) 고르는 시트 → 크루는 새 대기실, 누구나는 번개러닝 모집 글.
 *
 * 들어가기 · 출발 · 보상은 예전 길 그대로다 — 크루는 고른 방(party_join), 번개는 번개 상세 → 참가 → 번개 대기실.
 */

/** 다른 화면(러닝 시작 메뉴의 "크루")에서 함께 뛰기 탭의 "모집 중" 칸을 열어 달라는 표시 — 함께 뛰기 화면이 읽고 끈다 */
object TogetherFocus {
    var requested by mutableStateOf(false)
}

/** 함께 뛰기 목록의 크루 대기실 한 줄 — 어느 크루의 방인가 */
internal data class CrewRunEntry(val crewId: String, val crewName: String, val room: CrewRoom)

internal object TogetherList {
    /** 내 크루들의 열린 방 — 내 방 먼저, 그다음 최근에 연 방(크루를 섞어서). [crews] 는 (id, 이름) */
    fun crewRuns(crews: List<Pair<String, String>>, rooms: Map<String, CrewRooms>): List<CrewRunEntry> = crews
        .flatMap { (id, name) -> rooms[id]?.let { CrewRoomRules.ordered(it.rooms) }.orEmpty().map { CrewRunEntry(id, name, it) } }
        .sortedWith(compareByDescending<CrewRunEntry> { it.room.mine }.thenByDescending { it.room.createdAtMs }.thenByDescending { it.room.id })

    /** 앞으로 열릴 전체 번개 — 가까운 시각 먼저. 찬 번개는 내가 참가한 것만 남긴다 */
    fun flashes(posts: List<Post>, now: Long): List<Post> = posts
        .filter { it.isFlash && it.crewId.isBlank() && it.meetAt > now && (!it.isFull || it.joined) }
        .sortedWith(compareBy<Post> { it.meetAt }.thenBy { it.id })
}

/** 크루 방 목록을 다시 묻는 간격 — 화면이 보이는 동안만(인원 · 준비 수가 바뀐다) */
private const val ROOMS_REFRESH_MS = 10_000L

@Composable
internal fun TogetherRoomsSegment(
    viewModel: CommunityViewModel,
    onOpenFlash: (Long) -> Unit,
    onOpenRoom: (crewId: String, partyId: Long) -> Unit,
    onCreateCrewRoom: (crewId: String) -> Unit,
    onCreateFlash: () -> Unit,
    onAllMeetups: () -> Unit,
    onFindCrews: () -> Unit,
) {
    val posts by viewModel.boardPosts.collectAsStateWithLifecycle()
    val sync by viewModel.boardSync.collectAsStateWithLifecycle()
    val allCrews by viewModel.crews.collectAsStateWithLifecycle()
    val crews = remember(allCrews) { allCrews.filter { it.joined }.map { it.id to it.name } }
    val now by produceState(System.currentTimeMillis()) {
        while (true) { value = System.currentTimeMillis(); delay(30_000) }
    }
    // 크루마다의 방 목록 — 보이는 동안 10초마다(보기만 한다 — 들어가지 않는다)
    val rooms = remember { mutableStateMapOf<String, RoomsUi>() }
    var reload by remember { mutableStateOf(0) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    val crewRepo = ServiceLocator.crewRepository
    LaunchedEffect(crews, reload) {
        if (crews.isEmpty()) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            while (true) {
                coroutineScope {
                    crews.map { (id, _) ->
                        async {
                            val next = when (val result = crewRepo.rooms(id)) {
                                is ServerResult.Ok -> RoomsUi.Ready(result.value)
                                // 잠깐 끊기면 있던 목록을 그대로 둔다
                                is ServerResult.Retry -> rooms[id] as? RoomsUi.Ready ?: RoomsUi.Failed(signIn = false)
                                is ServerResult.SignInRequired -> RoomsUi.Failed(signIn = true)
                                is ServerResult.Rejected -> RoomsUi.Failed(signIn = false)
                            }
                            rooms[id] = next
                        }
                    }.awaitAll()
                }
                delay(ROOMS_REFRESH_MS)
            }
        }
    }
    var picking by rememberSaveable { mutableStateOf(false) }

    val ready = rooms.mapNotNull { (id, ui) -> (ui as? RoomsUi.Ready)?.let { id to it.rooms } }.toMap()
    val crewRuns = TogetherList.crewRuns(crews, ready)
    val flashes = TogetherList.flashes(posts, now)
    val loadingCrews = crews.isNotEmpty() && crews.any { (id, _) -> rooms[id] == null || rooms[id] == RoomsUi.Loading } && crewRuns.isEmpty()
    val failed = crews.mapNotNull { (id, _) -> rooms[id] as? RoomsUi.Failed }.firstOrNull()
    val doneToday = ready.mapValues { it.value.doneToday }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("community-meetups"), contentPadding = PaddingValues(bottom = 16.dp)) {
            if (sync != BoardSyncState.Ready) {
                item { Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { BoardSyncCard(sync, onRetry = viewModel::refreshBoard, compact = true) } }
            }
            // ── 크루 러닝 ──
            item { GroupHeader(stringResource(R.string.tg_section_crew)) }
            when {
                crews.isEmpty() -> item { NoCrewRow(onFindCrews) }
                loadingCrews -> item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) { StepUpSpinner(Modifier.size(28.dp)) }
                }
                crewRuns.isEmpty() && failed != null -> item {
                    RoomsState(
                        Icons.Outlined.CloudOff,
                        stringResource(if (failed.signIn) R.string.crew5_sign_in_title else R.string.crew5_rooms_failed),
                        null, tag = "crew-rooms-failed",
                        action = stringResource(R.string.crew5_rooms_reload) to { rooms.clear(); reload++ },
                    )
                }
                crewRuns.isEmpty() -> item {
                    Text(
                        stringResource(R.string.tg_crew_empty), style = StepUpUi.type.rowSubtitle, color = StepUpUi.colors.secondaryLabel,
                        modifier = Modifier.padding(horizontal = StepUpUi.TitleGutter, vertical = 8.dp).testTag("crew-rooms-empty"),
                    )
                }
                else -> items(crewRuns, key = { "room-${it.room.id}" }) { entry ->
                    Box(Modifier.padding(horizontal = StepUpUi.Gutter)) {
                        RoomRow(entry.room, entry.crewName, onOpen = { onOpenRoom(entry.crewId, entry.room.id) }, showCrew = true)
                    }
                }
            }
            // ── 번개러닝 ──
            item {
                GroupHeader(
                    stringResource(R.string.tg_section_flash), action = stringResource(R.string.ui_see_all),
                    actionTag = "community-all-meetups", onAction = onAllMeetups,
                )
            }
            if (flashes.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(horizontal = StepUpUi.TitleGutter, vertical = 8.dp).testTag("together-flash-empty")) {
                        Text(stringResource(R.string.ui_meetups_empty_title), style = StepUpUi.type.rowTitle, color = StepUpUi.colors.label)
                        Text(
                            stringResource(R.string.ui_meetups_empty_body), style = StepUpUi.type.rowSubtitle, color = StepUpUi.colors.secondaryLabel,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            } else {
                items(flashes, key = { "flash-${it.id}" }) { post ->
                    Box(Modifier.padding(horizontal = StepUpUi.Gutter)) { FlashRow(post, onOpen = { onOpenFlash(post.id) }) }
                }
            }
        }
        // 이 칸의 주 행동 하나 — 누구와 달릴지 고르고 만든다
        Box(Modifier.fillMaxWidth().padding(horizontal = StepUpUi.Gutter).padding(top = 8.dp, bottom = 12.dp)) {
            PrimaryButton(stringResource(R.string.tg_create), onClick = { picking = true }, modifier = Modifier.fillMaxWidth().testTag("together-create"))
        }
    }

    if (picking) {
        TogetherPickSheet(
            crews = crews,
            doneToday = doneToday,
            onCrew = { id -> picking = false; onCreateCrewRoom(id) },
            onAnyone = { picking = false; onCreateFlash() },
            onFindCrews = { picking = false; onFindCrews() },
            onDismiss = { picking = false },
        )
    }
}

/** 크루가 없을 때 크루 러닝 칸 — "크루 찾기"(크루 칸으로) */
@Composable
private fun NoCrewRow(onFindCrews: () -> Unit) {
    val c = StepUpUi.colors
    GroupedList(
        rows = listOf {
            GroupRow(onClick = onFindCrews, modifier = Modifier.testTag("together-find-crew")) {
                Icon(Icons.Outlined.Groups, contentDescription = null, tint = c.accentText, modifier = Modifier.size(24.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.d26_find_crew), style = StepUpUi.type.rowTitle, color = c.label)
                    Text(stringResource(R.string.tg_no_crew), style = StepUpUi.type.rowSubtitle, color = c.secondaryLabel, modifier = Modifier.padding(top = 2.dp))
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.tertiaryLabel, modifier = Modifier.size(20.dp))
            }
        },
    )
}

/** 번개 한 줄 — 크루 방 줄과 같은 모양: 번개 · 방식, 제목, 시각 · 거리, 만나는 곳(원격이면 각자의 위치), 인원, "자세히" */
@Composable
private fun FlashRow(post: Post, onOpen: () -> Unit) {
    val c = StepUpUi.colors
    val modeText = stringResource(if (post.remote) R.string.d26_mode_remote else R.string.d26_mode_together)
    Column(Modifier.fillMaxWidth().padding(vertical = 18.dp).testTag("together-flash-${post.id}")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Bolt, contentDescription = null, tint = c.accentText, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                stringResource(if (post.joined) R.string.tg_flash_joined else R.string.tg_flash_status, modeText),
                style = StepUpUi.type.caption.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = c.accentText,
                modifier = Modifier.testTag("together-flash-mode-${if (post.remote) "remote" else "together"}"),
            )
        }
        Text(
            post.title, style = StepUpUi.type.section.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold), color = c.label,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            stringResource(R.string.tg_flash_line, whenText(post.meetAt), String.format(Locale.ROOT, "%.1f", post.distanceKm)),
            style = StepUpUi.type.rowSubtitle.copy(fontSize = 14.sp), color = c.secondaryLabel, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            when {
                post.remote -> stringResource(R.string.d26_room_remote_line)
                post.place.isNotBlank() -> stringResource(R.string.d26_room_meet, post.place)
                else -> stringResource(R.string.d26_room_host, post.author)
            },
            style = StepUpUi.type.rowSubtitle.copy(fontSize = 14.sp), color = c.secondaryLabel, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp),
        )
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.d26_room_count, post.joinedCount, post.capacity), style = StepUpUi.type.body, color = c.secondaryLabel,
                modifier = Modifier.weight(1f).testTag("together-flash-count"),
            )
            val label = stringResource(R.string.tg_flash_view)
            val shape = RoundedCornerShape(50)
            Text(
                label, style = StepUpUi.type.smallButton, color = c.label,
                modifier = Modifier.heightIn(min = 40.dp).pressable(onClick = onOpen).clip(shape).border(1.dp, c.separator, shape)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .semantics { contentDescription = "${post.title} · $label" }
                    .testTag("together-flash-open-${post.id}"),
            )
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.separator))
}

/** "함께 뛰기 만들기" — 누구와 달릴까요? 크루마다 한 줄(크루 러닝 새 대기실) · 누구나(번개러닝 모집 글) */
@Composable
private fun TogetherPickSheet(
    crews: List<Pair<String, String>>,
    doneToday: Map<String, Boolean>,
    onCrew: (String) -> Unit,
    onAnyone: () -> Unit,
    onFindCrews: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = StepUpUi.colors
    com.stepup.android.ui.components.RunSheet(onDismiss = onDismiss, modifier = Modifier.testTag("together-pick-sheet")) {
        com.stepup.android.ui.components.RunSheetText(stringResource(R.string.tg_pick_title))
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.card),
            verticalArrangement = Arrangement.Top,
        ) {
            crews.forEach { (id, name) ->
                val done = doneToday[id] == true
                PickRow(
                    icon = Icons.Outlined.Groups,
                    title = stringResource(R.string.tg_pick_crew, name),
                    body = stringResource(if (done) R.string.tg_pick_crew_done else R.string.tg_pick_crew_body),
                    enabled = !done,
                    onClick = { onCrew(id) },
                    tag = "together-pick-crew-$id",
                )
                Box(Modifier.padding(start = 16.dp).fillMaxWidth().height(.5.dp).background(c.separator))
            }
            PickRow(
                icon = Icons.Rounded.Bolt,
                title = stringResource(R.string.tg_pick_anyone),
                body = stringResource(R.string.tg_pick_anyone_body),
                enabled = true,
                onClick = onAnyone,
                tag = "together-pick-anyone",
            )
            if (crews.isEmpty()) {
                Box(Modifier.padding(start = 16.dp).fillMaxWidth().height(.5.dp).background(c.separator))
                PickRow(
                    icon = Icons.Outlined.Groups,
                    title = stringResource(R.string.d26_find_crew),
                    body = stringResource(R.string.tg_no_crew),
                    enabled = true,
                    onClick = onFindCrews,
                    tag = "together-pick-find-crew",
                )
            }
        }
    }
}

@Composable
private fun PickRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tag: String,
) {
    val c = StepUpUi.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 68.dp).pressable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(c.primary.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = if (enabled) c.primary else c.tertiaryLabel, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = StepUpUi.type.rowTitle, color = if (enabled) c.label else c.secondaryLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(body, style = StepUpUi.type.rowSubtitle, color = c.secondaryLabel, modifier = Modifier.padding(top = 2.dp))
        }
        if (enabled) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.tertiaryLabel, modifier = Modifier.size(20.dp))
    }
}
