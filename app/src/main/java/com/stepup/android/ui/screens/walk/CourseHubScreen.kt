package com.stepup.android.ui.screens.walk

import androidx.compose.material3.IconToggleButton
import com.stepup.android.ui.components.DialogPanel
import com.stepup.android.ui.components.FormField
import com.stepup.android.ui.components.DarkIconButton
import com.stepup.android.ui.components.HairlineDivider
import com.stepup.android.ui.components.StatePanel
import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.data.repo.BoardSyncState
import com.stepup.android.domain.CourseRewards
import com.stepup.android.domain.RunCourse
import com.stepup.android.domain.trackDistanceKm
import com.stepup.android.service.WalkSessionService
import com.stepup.android.ui.components.CourseTrackMap
import com.stepup.android.ui.components.DetailPage
import com.stepup.android.ui.components.LiveRouteMap
import com.stepup.android.ui.components.GhostButton
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.VoltButton
import com.stepup.android.ui.components.quietClickable
import com.stepup.android.ui.screens.community.BoardSyncCard
import com.stepup.android.ui.screens.community.SegmentedTabs
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.Night
import com.stepup.android.ui.theme.OnVolt
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt

/**
 * 러닝 코스 허브 — 코스 선택 · 코스 만들기(마지막 GPS 트랙 등록) · 코스 게시판.
 *
 * 게시판은 서버에 있다 — 러너들이 공유한 코스와 StepUp 이 까는 공원 코스.
 * 내 코스는 공유 토글로 서버에 올린다.
 */
@Composable
fun CourseHubScreen(
    onBack: () -> Unit = {},
    viewModel: CourseHubViewModel = viewModel(factory = CourseHubViewModel.Factory),
) {
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val boardCourses by viewModel.board.collectAsStateWithLifecycle()
    val boardSync by viewModel.boardSync.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedId.collectAsStateWithLifecycle()
    val selectedTrack by viewModel.selectedTrack.collectAsStateWithLifecycle()
    val lastTrack by WalkSessionService.lastTrack.collectAsStateWithLifecycle()
    val recording by viewModel.recording.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(0) }

    /** 게시판 검색어 — 이름 · 동네 · 만든 사람에서 찾는다 */
    var query by rememberSaveable { mutableStateOf("") }

    // 코스를 누르면 바로 적용하지 않고 확인부터 받는다. id로만 들고 있어서
    // 목록이 갱신되면 내용도 따라가고, 그 코스가 지워지면 창이 저절로 닫힌다.
    var pendingId by rememberSaveable { mutableStateOf(-1L) }
    val pending = courses.firstOrNull { it.id == pendingId }
        ?: boardCourses.firstOrNull { it.id == pendingId }

    // 게시판의 서버 코스는 폰에 받아 둔 코스와 번호가 다르다. 같은 길이면 고른 코스다.
    fun isSelected(course: RunCourse): Boolean =
        course.id == selectedId || (selectedTrack != null && course.encode() == selectedTrack)

    // 게시판 탭을 열 때마다 서버에서 새로 받는다.
    LaunchedEffect(tab) {
        if (tab == 2) viewModel.refreshBoard()
    }

    // 공유·하트·지우기가 서버에서 막혔으면 이유를 짧게 띄운다.
    val problem by viewModel.problem.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val problemText = problem?.let {
        stringResource(if (it.signIn) R.string.board_sign_in_needed else R.string.crew_notice_failed)
    }
    LaunchedEffect(problem) {
        if (problemText != null) {
            Toast.makeText(context, problemText, Toast.LENGTH_SHORT).show()
            viewModel.consumeProblem()
        }
    }

    Box(Modifier.fillMaxSize()) {
        DetailPage(title = stringResource(R.string.courses_title), onBack = onBack) {
            item {
                SegmentedTabs(
                    labels = listOf(
                        stringResource(R.string.courses_tab_select),
                        stringResource(R.string.courses_tab_make),
                        stringResource(R.string.courses_tab_board),
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
                                onOpenRanking = { viewModel.openRanking(course) },
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
                    val board = boardCourses
                        .filter { it.matches(query) }
                        .sortedWith(
                            compareByDescending<RunCourse> { it.likes }
                                .thenByDescending { it.runCount }
                                .thenByDescending { it.createdAt },
                        )
                    // 서버를 못 읽었으면 왜 못 읽었는지부터. 폰의 공원 코스는 그 아래에 그대로 둔다.
                    if (boardSync !is BoardSyncState.Ready && boardSync !is BoardSyncState.Idle) {
                        item { BoardSyncCard(boardSync, onRetry = viewModel::refreshBoard) }
                    }
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
                                selected = isSelected(course),
                                onSelect = { pendingId = course.id },
                                onLike = { viewModel.toggleLike(course.id) },
                                showAuthor = true,
                                rank = index + 1,
                                onOpenRanking = { viewModel.openRanking(course) },
                            )
                        }
                    }
                }
            }
        }

        val ranking by viewModel.ranking.collectAsStateWithLifecycle()
        ranking?.let { state ->
            CourseRankingDialog(state = state, onDismiss = viewModel::closeRanking)
        }

        if (pending != null) {
            val clearing = isSelected(pending)
            DialogPanel(
                title = stringResource(if (clearing) R.string.course_clear_title else R.string.course_apply_title),
                onDismiss = { pendingId = -1L },
                actions = {
                    VoltButton(stringResource(R.string.common_yes), onClick = {
                        viewModel.select(pending.id)
                        pendingId = -1L
                    }, modifier = Modifier.fillMaxWidth())
                    GhostButton(stringResource(R.string.common_no), onClick = { pendingId = -1L }, modifier = Modifier.fillMaxWidth())
                },
            ) {
                CourseTrackMap(points = remember(pending.id, pending.points) { pending.normalized() }, seed = pending.id.toInt(), modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp)))
                Text(
                    text = if (clearing) stringResource(R.string.course_clear_body, pending.name)
                    else stringResource(R.string.course_apply_body, pending.name, "%.2f".format(pending.distanceKm), "%.1f".format(pending.reward)),
                    style = MaterialTheme.typography.bodyLarge, color = Silver,
                )
            }
        }
    }
}

@Composable
private fun EmptyCard(text: String) {
    StatePanel(message = text, icon = Icons.Filled.Route)
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
    /** 이 코스의 기록 순위 보기. null 이면 붙이지 않는다. */
    onOpenRanking: (() -> Unit)? = null,
) {
    GlowCard(accent = selected, contentPadding = PaddingValues(18.dp), spacing = 12.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            if (rank > 0) Text("$rank", style = MaterialTheme.typography.bodyMedium, color = if (rank <= 3) Volt else Silver)
            Text(course.name, style = MaterialTheme.typography.titleLarge, color = Snow)
            if (course.mine && course.shared) Text(stringResource(R.string.course_shared_badge), style = MaterialTheme.typography.bodyMedium, color = com.stepup.android.ui.theme.VoltText)
        }
        Text(
            text = buildString {
                if (course.area.isNotBlank()) append(course.area).append(" · ")
                append("%.2f km".format(course.distanceKm))
            }, style = MaterialTheme.typography.bodyMedium, color = Silver,
        )
        CourseTrackMap(
            points = remember(course.id, course.points) { course.normalized() }, seed = course.id.toInt(),
            modifier = Modifier.fillMaxWidth().height(136.dp).clip(RoundedCornerShape(16.dp)).quietClickable(onSelect),
        )
        Text(stringResource(R.string.course_reward_value, "%.1f".format(course.reward)), style = MaterialTheme.typography.titleMedium, color = com.stepup.android.ui.theme.VoltText)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.course_runs, course.runCount), style = MaterialTheme.typography.bodyMedium, color = Silver, modifier = Modifier.weight(1f))
            IconToggleButton(checked = course.liked, onCheckedChange = { onLike() }, modifier = Modifier.size(48.dp)) {
                Icon(if (course.liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, stringResource(if (course.liked) R.string.course_unlike_action else R.string.course_like_action), tint = if (course.liked) Volt else Silver)
            }
            Text("${course.likes}", style = MaterialTheme.typography.bodyMedium, color = Silver)
        }
        if (showAuthor && course.author.isNotBlank()) Text(stringResource(R.string.course_by, course.author), style = MaterialTheme.typography.bodyMedium, color = Silver)
        VoltButton(stringResource(if (selected) R.string.course_selected else R.string.course_pick), onClick = onSelect, modifier = Modifier.fillMaxWidth())
        if (onOpenRanking != null) GhostButton(stringResource(R.string.course_rank_open), onClick = onOpenRanking, modifier = Modifier.fillMaxWidth())
        if (onShareToggle != null || onDelete != null) {
            HairlineDivider()
            if (onShareToggle != null) GhostButton(stringResource(if (course.shared) R.string.course_unshare else R.string.course_share), onClick = onShareToggle, modifier = Modifier.fillMaxWidth())
            if (onDelete != null) GhostButton(stringResource(R.string.post_delete), onClick = onDelete, accent = Silver, modifier = Modifier.fillMaxWidth())
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

    GlowCard(contentPadding = PaddingValues(20.dp), spacing = 12.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.Route, contentDescription = null, tint = Volt, modifier = Modifier.size(20.dp))
            Text(
                text = stringResource(R.string.course_make_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = Silver,
                lineHeight = 22.sp,
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
            FormField(
                label = stringResource(R.string.course_name_hint),
                value = name,
                onValueChange = { name = it },
            )
            FormField(
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
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Snow,
                )
                Switch(
                    checked = share,
                    onCheckedChange = { share = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OnVolt,
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
            fontSize = 14.sp,
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
    GlowCard(accent = recording, contentPadding = PaddingValues(20.dp), spacing = 12.dp) {
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
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Volt,
                    )
                    Text(
                        text = stringResource(res),
                        fontSize = 14.sp,
                        color = Silver,
                        lineHeight = 21.sp,
                    )
                }
            }
        }
        if (recording) {
            Text(
                text = stringResource(R.string.course_rec_waiting),
                fontSize = 14.sp,
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
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FormField(label = stringResource(R.string.courses_search_hint), value = value, onValueChange = onValueChange, modifier = Modifier.weight(1f))
        if (value.isNotEmpty()) DarkIconButton(Icons.Filled.Close, stringResource(R.string.common_cancel), onClick = { onValueChange("") })
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
    GlowCard(contentPadding = PaddingValues(20.dp), spacing = 10.dp) {
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
                fontSize = 14.sp,
                color = Silver,
                lineHeight = 21.sp,
            )
            GhostButton(
                text = stringResource(R.string.course_rec_card_title),
                onClick = onMake,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            mine.forEach { course ->
                HairlineDivider()
                Text(course.name, style = MaterialTheme.typography.titleMedium, color = Snow)
                Text("%.2f km".format(course.distanceKm), style = MaterialTheme.typography.bodyMedium, color = Silver)
                GhostButton(stringResource(R.string.course_upload_do), onClick = { onUpload(course.id) }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** 코스 기록 순위 — 사람마다 가장 빠른 기록 하나. 서버가 경로로 확인한 기록만 올라온다. */
@Composable
private fun CourseRankingDialog(state: CourseRankingState, onDismiss: () -> Unit) {
    DialogPanel(
        title = stringResource(R.string.course_rank_title),
        onDismiss = onDismiss,
        actions = { VoltButton(stringResource(R.string.common_close), onClick = onDismiss, modifier = Modifier.fillMaxWidth()) },
    ) {
        Text(state.courseName, style = MaterialTheme.typography.titleMedium, color = Snow)
        when (state) {
            is CourseRankingState.Loading -> Text(stringResource(R.string.course_rank_loading), style = MaterialTheme.typography.bodyLarge, color = Silver)
            is CourseRankingState.Failed -> Text(stringResource(if (state.signIn) R.string.board_sign_in_needed else R.string.course_rank_failed), style = MaterialTheme.typography.bodyLarge, color = Silver)
            is CourseRankingState.Ready -> if (state.rows.isEmpty()) {
                Text(stringResource(R.string.course_rank_empty), style = MaterialTheme.typography.bodyLarge, color = Silver)
            } else {
                state.rows.forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("${row.rank}", style = MaterialTheme.typography.titleMedium, color = if (row.rank <= 3) Volt else Silver)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(if (row.isMe) stringResource(R.string.course_rank_me, row.displayName) else row.displayName, style = MaterialTheme.typography.bodyLarge, color = if (row.isMe) Volt else Snow)
                            Text(formatDuration(row.durationSec), style = MaterialTheme.typography.titleLarge, fontFamily = com.stepup.android.ui.theme.StepUpNumbers, color = Snow)
                        }
                    }
                    HairlineDivider()
                }
            }
        }
    }
}

/** 초 → "m:ss" 또는 "h:mm:ss" */
private fun formatDuration(sec: Int): String {
    val h = sec / 3600
    val m = sec % 3600 / 60
    val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
