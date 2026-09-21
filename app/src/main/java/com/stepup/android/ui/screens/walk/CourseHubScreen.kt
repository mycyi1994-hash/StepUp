package com.stepup.android.ui.screens.walk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.domain.CourseRewards
import com.stepup.android.domain.RunCourse
import com.stepup.android.domain.trackDistanceKm
import com.stepup.android.service.WalkSessionService
import com.stepup.android.ui.components.CourseTrackMap
import com.stepup.android.ui.components.DarkIconButton
import com.stepup.android.ui.components.LiveRouteMap
import com.stepup.android.ui.components.Eyebrow
import com.stepup.android.ui.components.GhostButton
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.HexEmblem
import com.stepup.android.ui.components.VoltButton
import com.stepup.android.ui.components.quietClickable
import com.stepup.android.ui.screens.community.LabeledField
import com.stepup.android.ui.screens.community.SegmentedTabs
import com.stepup.android.ui.theme.Carbon
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.Night
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt

/**
 * 러닝 코스 허브 — 코스 선택 · 코스 만들기(마지막 GPS 트랙 등록) · 코스 게시판.
 *
 * 게시판은 shared 플래그가 켜진 코스들이다. 백엔드가 없으므로 다른 러너의
 * 코스는 시드로 채우고, 내 코스는 공유 토글로 게시판에 올린다.
 */
@Composable
fun CourseHubScreen(
    onBack: () -> Unit = {},
    viewModel: CourseHubViewModel = viewModel(factory = CourseHubViewModel.Factory),
) {
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedId.collectAsStateWithLifecycle()
    val lastTrack by WalkSessionService.lastTrack.collectAsStateWithLifecycle()
    val recording by viewModel.recording.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(0) }

    /** 게시판 검색어 — 이름 · 동네 · 만든 사람에서 찾는다 */
    var query by rememberSaveable { mutableStateOf("") }

    // 코스를 누르면 바로 적용하지 않고 확인부터 받는다. id로만 들고 있어서
    // 목록이 갱신되면 내용도 따라가고, 그 코스가 지워지면 창이 저절로 닫힌다.
    var pendingId by rememberSaveable { mutableStateOf(-1L) }
    val pending = courses.firstOrNull { it.id == pendingId }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DarkIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        onClick = onBack,
                    )
                    Column {
                        Eyebrow(text = stringResource(R.string.run_live))
                        Text(
                            text = stringResource(R.string.courses_title),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                            color = Snow,
                        )
                    }
                }
            }

            item {
                SegmentedTabs(
                    labels = listOf(
                        stringResource(R.string.courses_select),
                        stringResource(R.string.courses_make),
                        stringResource(R.string.courses_board),
                    ),
                    selected = tab,
                    onSelect = { tab = it },
                )
            }

            when (tab) {
                // ── 코스 선택 — 달릴 수 있는 모든 코스 ─────────────
                0 -> {
                    val list = courses
                    if (list.isEmpty()) {
                        item { EmptyCard(stringResource(R.string.courses_empty)) }
                    } else {
                        items(list.size, key = { list[it].id }) { index ->
                            val course = list[index]
                            CourseCard(
                                course = course,
                                selected = course.id == selectedId,
                                onSelect = { pendingId = course.id },
                                onLike = { viewModel.toggleLike(course.id) },
                                onShareToggle = if (course.mine) {
                                    { viewModel.setShared(course.id, !course.shared) }
                                } else {
                                    null
                                },
                                onDelete = if (course.mine) {
                                    { viewModel.delete(course.id) }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }

                // ── 코스 만들기 — 한 번 뛰어서 만든다 ─────────────
                1 -> {
                    item {
                        CourseRecorderCard(
                            recording = recording,
                            onStart = { viewModel.startRecording(onReady = onBack) },
                            onCancel = viewModel::cancelRecording,
                        )
                    }
                    // 녹화를 걸지 않고 뛴 경우에도 방금 달린 길은 남아 있다.
                    // 그걸 버리게 할 이유는 없으므로 예전 등록 창을 밑에 둔다.
                    item {
                        CourseMaker(lastTrack = lastTrack, onCreate = { name, area, shared ->
                            viewModel.create(name, area, lastTrack, shared)
                            tab = 0
                        })
                    }
                }

                // ── 코스 게시판 — 러너들이 공유한 코스 ─────────────
                //
                // 하트 많은 순이다. 최신순으로 두면 방금 올라온 코스가 늘 맨
                // 위를 차지하고, 여러 사람이 뛰어 보고 좋다고 한 코스는 아래로
                // 밀린다 — 게시판을 보는 이유가 없어진다.
                else -> {
                    item {
                        CourseUploadCard(
                            mine = courses.filter { it.mine && !it.shared },
                            onUpload = { id -> viewModel.setShared(id, true) },
                            onMake = { tab = 1 },
                        )
                    }
                    item {
                        CourseSearchField(value = query, onValueChange = { query = it })
                    }
                    val board = courses
                        .filter { it.shared && it.matches(query) }
                        .sortedWith(
                            compareByDescending<RunCourse> { it.likes }
                                .thenByDescending { it.runCount }
                                .thenByDescending { it.createdAt },
                        )
                    if (board.isEmpty()) {
                        item {
                            EmptyCard(
                                stringResource(
                                    if (query.isBlank()) {
                                        R.string.courses_board_empty
                                    } else {
                                        R.string.courses_search_empty
                                    },
                                ),
                            )
                        }
                    } else {
                        items(board.size, key = { board[it].id }) { index ->
                            val course = board[index]
                            CourseCard(
                                course = course,
                                selected = course.id == selectedId,
                                onSelect = { pendingId = course.id },
                                onLike = { viewModel.toggleLike(course.id) },
                                showAuthor = true,
                                rank = index + 1,
                            )
                        }
                    }
                }
            }
        }

        if (pending != null) {
            val clearing = pending.id == selectedId
            AlertDialog(
                onDismissRequest = { pendingId = -1L },
                containerColor = Carbon,
                titleContentColor = Snow,
                textContentColor = Silver,
                title = {
                    Text(
                        text = stringResource(
                            if (clearing) R.string.course_clear_title else R.string.course_apply_title,
                        ),
                        fontWeight = FontWeight.Black,
                    )
                },
                text = {
                    Text(
                        text = if (clearing) {
                            stringResource(R.string.course_clear_body, pending.name)
                        } else {
                            stringResource(
                                R.string.course_apply_body,
                                pending.name,
                                "%.2f".format(pending.distanceKm),
                                "%.1f".format(pending.reward),
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.select(pending.id)
                            pendingId = -1L
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.common_yes),
                            color = Volt,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingId = -1L }) {
                        Text(text = stringResource(R.string.common_no), color = Silver)
                    }
                },
            )
        }
    }
}

@Composable
private fun EmptyCard(text: String) {
    GlowCard(contentPadding = PaddingValues(24.dp)) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = Silver)
    }
}

/** 코스 한 장 — 미니 지도 + 이름 · 거리 · 보상 · 선택 */
@Composable
private fun CourseCard(
    course: RunCourse,
    selected: Boolean,
    onSelect: () -> Unit,
    onLike: () -> Unit,
    showAuthor: Boolean = false,
    onShareToggle: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    /** 게시판 순위. 0이면 붙이지 않는다. */
    rank: Int = 0,
) {
    GlowCard(
        accent = selected,
        contentPadding = PaddingValues(14.dp),
        spacing = 10.dp,
    ) {
        Row(
            modifier = Modifier.quietClickable(onSelect),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Night)
                    .border(1.dp, Edge, RoundedCornerShape(16.dp)),
            ) {
                CourseTrackMap(
                    points = remember(course.id) { course.normalized() },
                    seed = course.id.toInt(),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (rank > 0) {
                        Text(
                            text = "$rank",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = if (rank <= 3) Volt else Slate,
                        )
                    }
                    Text(
                        text = course.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = Snow,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (course.mine && course.shared) {
                        Badge(stringResource(R.string.course_shared_badge))
                    }
                }
                Text(
                    text = buildString {
                        if (course.area.isNotBlank()) append(course.area).append(" · ")
                        append("%.2f km".format(course.distanceKm))
                    },
                    fontSize = 11.sp,
                    color = Silver,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    HexEmblem(size = 13.dp, glow = false)
                    Text(
                        text = stringResource(
                            R.string.course_reward_value,
                            "%.1f".format(course.reward),
                        ),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Volt,
                    )
                    Text(
                        text = stringResource(R.string.course_runs, course.runCount),
                        fontSize = 10.sp,
                        color = Slate,
                    )
                }
                if (showAuthor && course.author.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.course_by, course.author),
                        fontSize = 10.sp,
                        color = Slate,
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                SelectDot(selected = selected, onClick = onSelect)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.quietClickable(onLike),
                ) {
                    Icon(
                        imageVector = if (course.liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (course.liked) Volt else Slate,
                        modifier = Modifier.size(13.dp),
                    )
                    Text("${course.likes}", fontSize = 10.sp, color = Silver)
                }
            }
        }

        if (onShareToggle != null || onDelete != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (onShareToggle != null) {
                    GhostButton(
                        text = stringResource(
                            if (course.shared) R.string.course_unshare else R.string.course_share,
                        ),
                        onClick = onShareToggle,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (onDelete != null) {
                    GhostButton(
                        text = stringResource(R.string.post_delete),
                        onClick = onDelete,
                        accent = Slate,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Volt.copy(alpha = 0.14f))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(text = text, color = Volt, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

/** 선택 라디오 — 체크되면 볼트 원 */
@Composable
private fun SelectDot(selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(if (selected) Volt else CarbonHigh)
            .border(1.dp, if (selected) Volt else Edge, CircleShape)
            .quietClickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = stringResource(R.string.course_selected),
                tint = Night,
                modifier = Modifier.size(16.dp),
            )
        } else {
            Icon(
                Icons.Filled.Flag,
                contentDescription = null,
                tint = Slate,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/** 코스 만들기 — 마지막 러닝의 GPS 트랙을 이름 붙여 등록한다 */
@Composable
private fun CourseMaker(
    lastTrack: List<com.stepup.android.domain.GeoPoint>,
    onCreate: (name: String, area: String, shared: Boolean) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var area by rememberSaveable { mutableStateOf("") }
    var share by rememberSaveable { mutableStateOf(true) }
    val km = remember(lastTrack) { lastTrack.trackDistanceKm() }

    GlowCard(contentPadding = PaddingValues(16.dp), spacing = 12.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.Route, contentDescription = null, tint = Volt, modifier = Modifier.size(20.dp))
            Text(
                text = stringResource(R.string.course_make_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Silver,
                lineHeight = 18.sp,
            )
        }

        if (lastTrack.size < 2 || km < 0.2) {
            Text(
                text = stringResource(R.string.course_make_none),
                style = MaterialTheme.typography.bodyMedium,
                color = Slate,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Night)
                    .border(1.dp, Edge, RoundedCornerShape(18.dp)),
            ) {
                LiveRouteMap(
                    points = lastTrack,
                    seed = lastTrack.size,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Text(
                text = stringResource(R.string.course_make_last, "%.2f".format(km)),
                style = MaterialTheme.typography.titleSmall,
                color = Volt,
            )
            LabeledField(
                label = stringResource(R.string.course_name_hint),
                value = name,
                onValueChange = { name = it },
            )
            LabeledField(
                label = stringResource(R.string.course_area_hint),
                value = area,
                onValueChange = { area = it },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.course_share_toggle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Snow,
                )
                Switch(
                    checked = share,
                    onCheckedChange = { share = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Night,
                        checkedTrackColor = Volt,
                        uncheckedThumbColor = Silver,
                        uncheckedTrackColor = CarbonHigh,
                    ),
                )
            }
            VoltButton(
                text = stringResource(R.string.course_register),
                onClick = { onCreate(name, area, share) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.width(1.dp))
        Text(
            text = stringResource(
                R.string.course_per_km,
                "%.0f".format(CourseRewards.SUP_PER_KM),
                "%.0f".format(CourseRewards.MAX_REWARD),
            ),
            fontSize = 10.sp,
            color = Slate,
        )
    }
}

/** 검색어가 이름 · 동네 · 만든 사람 중 하나에 걸리는지. 빈 검색어는 전부 통과. */
private fun RunCourse.matches(query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    return name.contains(q, ignoreCase = true) ||
        area.contains(q, ignoreCase = true) ||
        author.contains(q, ignoreCase = true)
}

/**
 * 코스 만들기 입구.
 *
 * 코스는 실제로 뛴 길이라야 한다. 지도 위에 손으로 선을 그으면 건물이나
 * 강을 가로지르는 코스가 나오고, 그걸 받은 사람은 그대로 뛸 수 없다.
 * 그래서 만들기는 러닝 화면으로 보내 한 번 뛰게 하고, 끝난 자리에서 저장한다.
 */
@Composable
private fun CourseRecorderCard(
    recording: Boolean,
    onStart: () -> Unit,
    onCancel: () -> Unit,
) {
    GlowCard(accent = recording, contentPadding = PaddingValues(16.dp), spacing = 12.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Route,
                contentDescription = null,
                tint = Volt,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.course_rec_card_title),
                style = MaterialTheme.typography.titleSmall,
                color = Snow,
            )
        }
        // 네 걸음을 번호로 적는다. 흐름이 화면 여럿에 걸쳐 있어서, 지금
        // 어디쯤인지 모르면 러닝 화면에서 "그래서 코스는?"이 된다.
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf(
                R.string.course_rec_step1,
                R.string.course_rec_step2,
                R.string.course_rec_step3,
                R.string.course_rec_step4,
            ).forEachIndexed { index, res ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${index + 1}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Volt,
                    )
                    Text(
                        text = stringResource(res),
                        fontSize = 11.sp,
                        color = Silver,
                        lineHeight = 17.sp,
                    )
                }
            }
        }
        if (recording) {
            Text(
                text = stringResource(R.string.course_rec_waiting),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Volt,
            )
            GhostButton(
                text = stringResource(R.string.course_rec_stop),
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            VoltButton(
                text = stringResource(R.string.course_rec_start),
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 게시판 검색 — 이름 · 동네 · 만든 사람 */
@Composable
private fun CourseSearchField(value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CarbonHigh)
            .border(1.dp, Edge, RoundedCornerShape(14.dp))
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = Slate,
            modifier = Modifier.size(16.dp),
        )
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = stringResource(R.string.courses_search_hint),
                    fontSize = 13.sp,
                    color = Slate,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = Snow, fontSize = 13.sp, lineHeight = 19.sp),
                cursorBrush = SolidColor(Volt),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.common_cancel),
                tint = Slate,
                modifier = Modifier
                    .size(16.dp)
                    .quietClickable { onValueChange("") },
            )
        }
    }
}

/**
 * 내 코스 올리기.
 *
 * 올릴 수 있는 것만 보여 준다 — 내가 만들었고 아직 게시판에 없는 코스.
 * 하나도 없으면 만드는 쪽으로 보낸다. 빈 목록만 두면 "올리기"가 고장 난
 * 버튼으로 읽힌다.
 */
@Composable
private fun CourseUploadCard(
    mine: List<RunCourse>,
    onUpload: (Long) -> Unit,
    onMake: () -> Unit,
) {
    GlowCard(contentPadding = PaddingValues(14.dp), spacing = 10.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Upload,
                contentDescription = null,
                tint = Volt,
                modifier = Modifier.size(17.dp),
            )
            Text(
                text = stringResource(R.string.course_upload_title),
                style = MaterialTheme.typography.titleSmall,
                color = Snow,
            )
        }
        if (mine.isEmpty()) {
            Text(
                text = stringResource(R.string.course_upload_none),
                fontSize = 11.sp,
                color = Silver,
                lineHeight = 17.sp,
            )
            GhostButton(
                text = stringResource(R.string.course_rec_card_title),
                onClick = onMake,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            mine.forEach { course ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CarbonHigh)
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = course.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Snow,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "%.2f km".format(course.distanceKm),
                            fontSize = 10.sp,
                            color = Slate,
                        )
                    }
                    Text(
                        text = stringResource(R.string.course_upload_do),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Volt,
                        modifier = Modifier
                            .quietClickable { onUpload(course.id) }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}
