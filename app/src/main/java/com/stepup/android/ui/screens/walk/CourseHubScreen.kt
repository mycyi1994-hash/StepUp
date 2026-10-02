package com.stepup.android.ui.screens.walk

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.data.repo.BoardSyncState
import com.stepup.android.domain.CourseRewards
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.RunCourse
import com.stepup.android.domain.trackDistanceKm
import com.stepup.android.service.WalkSessionService
import com.stepup.android.ui.components.LiveRouteMap
import com.stepup.android.ui.components.RunBackdrop
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunButtonKind
import com.stepup.android.ui.components.RunCard
import com.stepup.android.ui.components.RunDivider
import com.stepup.android.ui.components.RunEmptyState
import com.stepup.android.ui.components.RunHeadline
import com.stepup.android.ui.components.RunInitials
import com.stepup.android.ui.components.RunMapFrame
import com.stepup.android.ui.components.RunNumber
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.RunSheetText
import com.stepup.android.ui.components.RunSpec
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.RunTabs
import com.stepup.android.ui.components.RunTextAction
import com.stepup.android.ui.components.RunTextField
import com.stepup.android.ui.components.RunTopBar
import com.stepup.android.ui.components.runNumberStyle
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.feedbackClickable

/**
 * 러닝 코스 허브(시안 K09 · K10 · K11 · K12~K18) — 코스 선택 · 코스 만들기(마지막 GPS 트랙 등록) · 코스 게시판.
 *
 * 게시판은 서버에 있다 — 러너들이 공유한 코스와 StepUp 이 까는 공원 코스.
 * 내 코스는 공유 토글로 서버에 올린다. 지도는 모두 실제 타일 위의 실제 좌표다.
 */
@Composable
fun CourseHubScreen(
    onBack: () -> Unit = {},
    /** "이 코스로 달리기"(K09) — 고른 코스로 러닝 화면으로. 없으면 버튼을 두지 않는다 */
    onRunCourse: (() -> Unit)? = null,
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

    // 코스를 누르면 바로 적용하지 않고 확인부터 받는다(K13 · K14). id로만 들고 있어서
    // 목록이 갱신되면 내용도 따라가고, 그 코스가 지워지면 창이 저절로 닫힌다.
    var pendingId by rememberSaveable { mutableStateOf(-1L) }
    val pending = courses.firstOrNull { it.id == pendingId }
        ?: boardCourses.firstOrNull { it.id == pendingId }
    // 올릴 코스 고르기(K16)
    var uploadPicker by rememberSaveable { mutableStateOf(false) }
    // 순위(K12)를 연 코스 — 지도를 그리려고 길을 들고 있다
    var rankingCourseId by rememberSaveable { mutableStateOf(-1L) }

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
    val openRanking = { course: RunCourse ->
        rankingCourseId = course.id
        viewModel.openRanking(course)
    }

    Box(Modifier.fillMaxSize().testTag("courses")) {
        RunBackdrop(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            RunTopBar(onBack = onBack)
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth().testTag("courses-list"),
                contentPadding = PaddingValues(start = RunSpec.Gutter, end = RunSpec.Gutter, top = 4.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    RunHeadline(
                        stringResource(when (tab) {
                            0 -> R.string.run_courses_mine
                            1 -> R.string.courses_make
                            else -> R.string.courses_board
                        }),
                        titleTag = "courses-title",
                    )
                }
                item {
                    RunTabs(
                        labels = listOf(
                            stringResource(R.string.run_courses_tab_select),
                            stringResource(R.string.courses_make),
                            stringResource(R.string.courses_board),
                        ),
                        selected = tab, onSelect = { tab = it }, tagPrefix = "courses-tab",
                    )
                }

                when (tab) {
                    // ── 코스 선택(K09) — 달릴 수 있는 모든 코스 ─────────────
                    0 -> {
                        val list = courses
                        if (list.isEmpty()) {
                            item {
                                RunEmptyState(Icons.Outlined.Map, stringResource(R.string.courses_empty),
                                    modifier = Modifier.padding(top = 24.dp).testTag("courses-empty"))
                            }
                        } else {
                            items(list.size, key = { list[it].id }) { index ->
                                val course = list[index]
                                CourseSelectCard(
                                    course = course,
                                    selected = course.id == selectedId,
                                    onSelect = { pendingId = course.id },
                                    onLike = { viewModel.toggleLike(course.id) },
                                    onOpenRanking = { openRanking(course) },
                                    onShareToggle = if (course.mine) {
                                        { viewModel.setShared(course.id, !course.shared) }
                                    } else null,
                                    onDelete = if (course.mine) {
                                        { viewModel.delete(course.id) }
                                    } else null,
                                )
                            }
                        }
                    }

                    // ── 코스 만들기(K10) — 한 번 뛰어서 만든다 ─────────────
                    1 -> {
                        item {
                            CourseRecorderCard(
                                recording = recording,
                                onStart = { viewModel.startRecording(onReady = onBack) },
                                onCancel = viewModel::cancelRecording,
                            )
                        }
                        // 녹화를 걸지 않고 뛴 경우에도 방금 달린 길은 남아 있다.
                        // 그걸 버리게 할 이유는 없으므로 예전 등록 칸을 밑에 둔다.
                        item {
                            CourseMaker(lastTrack = lastTrack, onCreate = { name, area, shared ->
                                viewModel.create(name, area, lastTrack, shared)
                                tab = 0
                            })
                        }
                    }

                    // ── 코스 게시판(K11 · K17 · K18) — 러너들이 공유한 코스 ─────────────
                    //
                    // 하트 많은 순이다. 최신순으로 두면 방금 올라온 코스가 늘 맨
                    // 위를 차지하고, 여러 사람이 뛰어 보고 좋다고 한 코스는 아래로
                    // 밀린다 — 게시판을 보는 이유가 없어진다.
                    else -> {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RunTextField(
                                    value = query, onValueChange = { query = it },
                                    placeholder = stringResource(R.string.run_course_search), leading = Icons.Outlined.Search,
                                    clearLabel = stringResource(R.string.run_course_clear_text), fieldTag = "courses-search",
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(10.dp))
                                UploadChip(onClick = { uploadPicker = true })
                            }
                        }
                        val board = boardCourses
                            .filter { it.matches(query) }
                            .sortedWith(
                                compareByDescending<RunCourse> { it.likes }
                                    .thenByDescending { it.runCount }
                                    .thenByDescending { it.createdAt },
                            )
                        val failed = boardSync is BoardSyncState.Failed || boardSync is BoardSyncState.SignInRequired
                        // 서버를 못 읽었으면 왜 못 읽었는지부터(K18). 폰의 코스는 그 아래에 그대로 둔다.
                        if (failed) {
                            item {
                                BoardFailed(
                                    signIn = boardSync is BoardSyncState.SignInRequired,
                                    onRetry = viewModel::refreshBoard,
                                    onMine = { tab = 0 },
                                )
                            }
                        } else if (boardSync is BoardSyncState.Loading && board.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                                    RunSpinner(Modifier.size(56.dp).testTag("courses-board-loading"))
                                }
                            }
                        }
                        if (board.isEmpty() && !failed && boardSync !is BoardSyncState.Loading) {
                            item {
                                if (query.isBlank()) {
                                    // K17 — 아직 공유된 코스가 없어요
                                    Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        RunEmptyState(
                                            Icons.Outlined.Map, stringResource(R.string.run_course_board_empty_title),
                                            body = stringResource(R.string.run_course_board_empty_body),
                                            modifier = Modifier.testTag("courses-board-empty"),
                                        )
                                        Spacer(Modifier.height(20.dp))
                                        RunButton(stringResource(R.string.run_course_upload), { uploadPicker = true }, hero = true,
                                            modifier = Modifier.testTag("courses-board-empty-upload"))
                                    }
                                } else {
                                    Text(stringResource(R.string.courses_search_empty), style = runTextStyle(15.sp, runTone().label),
                                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                                            .testTag("courses-search-empty"))
                                }
                            }
                        } else if (board.isNotEmpty()) {
                            item {
                                Text(stringResource(R.string.run_course_sort_likes), style = runTextStyle(13.sp, runTone().label, FontWeight.SemiBold),
                                    textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                            }
                            items(board.size, key = { board[it].id }) { index ->
                                val course = board[index]
                                BoardCourseCard(
                                    course = course,
                                    rank = index + 1,
                                    selected = isSelected(course),
                                    onOpen = { pendingId = course.id },
                                    onLike = { viewModel.toggleLike(course.id) },
                                    onOpenRanking = { openRanking(course) },
                                )
                            }
                        }
                    }
                }
            }
            // K09 — 고른 코스로 달리기(코스 선택 탭에서만)
            if (tab == 0 && onRunCourse != null && courses.isNotEmpty()) {
                Box(Modifier.fillMaxWidth().padding(horizontal = RunSpec.Gutter).padding(top = 8.dp, bottom = 14.dp)) {
                    RunButton(
                        stringResource(R.string.run_course_run_this), onRunCourse, hero = true,
                        enabled = courses.any { it.id == selectedId },
                        modifier = Modifier.testTag("courses-run"),
                    )
                }
            }
        }

        // K12 — 코스 순위
        val ranking by viewModel.ranking.collectAsStateWithLifecycle()
        ranking?.let { state ->
            val course = courses.firstOrNull { it.id == rankingCourseId } ?: boardCourses.firstOrNull { it.id == rankingCourseId }
            CourseRankingSheet(state = state, course = course, onDismiss = viewModel::closeRanking)
        }

        // K13 · K14 — 이 코스를 선택할까요? · 선택한 코스를 해제할까요?
        if (pending != null) {
            val clearing = isSelected(pending)
            RunSheet(onDismiss = { pendingId = -1L }, modifier = Modifier.testTag(if (clearing) "course-clear-sheet" else "course-apply-sheet")) {
                CourseConfirmContent(
                    course = pending, clearing = clearing,
                    onConfirm = {
                        viewModel.select(pending.id)
                        pendingId = -1L
                    },
                    onCancel = { pendingId = -1L },
                )
            }
        }

        // K16 — 공유할 코스 선택
        if (uploadPicker) {
            RunSheet(onDismiss = { uploadPicker = false }, modifier = Modifier.testTag("course-upload-sheet")) {
                CourseUploadContent(
                    mine = courses.filter { it.mine && !it.shared },
                    onUpload = { id ->
                        viewModel.setShared(id, true)
                        uploadPicker = false
                    },
                    onMake = {
                        uploadPicker = false
                        tab = 1
                    },
                    onCancel = { uploadPicker = false },
                )
            }
        }
    }
}

/** 지도 틀 안의 코스 길 — 실제 타일 위에 시안 선(지도 출처 표기는 지도 부품이 둔다) */
@Composable
private fun CourseMap(points: List<GeoPoint>, modifier: Modifier) {
    RunMapFrame(modifier) {
        LiveRouteMap(points = points, modifier = Modifier.fillMaxSize(), routeColor = runTone().cyan)
    }
}

/** 서버가 주는 코스만 금액을 보인다(최대치) — 체험 · 내 코스는 "보상 없음" */
@Composable
private fun RewardLine(course: RunCourse) {
    val t = runTone()
    Text(
        if (course.serverReward > 0) stringResource(R.string.course_reward_upto, "%.0f".format(course.serverReward))
        else stringResource(R.string.course_reward_none),
        style = runTextStyle(13.sp, if (course.serverReward > 0) t.cyan else t.muted, FontWeight.SemiBold),
    )
}

/** 코스 한 장(K09) — 지도 · 고름 표시 · 이름 · 동네 · 거리, 아래 순위 보기 · 하트 · 내 코스로 공유 */
@Composable
private fun CourseSelectCard(
    course: RunCourse,
    selected: Boolean,
    onSelect: () -> Unit,
    onLike: () -> Unit,
    onOpenRanking: () -> Unit,
    onShareToggle: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    val t = runTone()
    val shape = RoundedCornerShape(RunSpec.CardRadius)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(t.panelTop, t.panel)), shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) Color(0xFF4D8BFF) else t.panelEdge, shape)
            .testTag("course-card-${course.id}"),
    ) {
        CourseMap(course.points, Modifier.fillMaxWidth().height(150.dp).padding(6.dp))
        Row(
            Modifier.fillMaxWidth().feedbackClickable(role = Role.RadioButton, onClick = onSelect)
                .semantics { this.selected = selected }
                .testTag("course-pick-${course.id}")
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(28.dp).clip(CircleShape)
                    .then(if (selected) Modifier.background(Color(0xFF0754FF)) else Modifier.border(2.dp, t.label.copy(alpha = 0.7f), CircleShape)),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(course.name, style = runTextStyle(18.sp, t.text, FontWeight.ExtraBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(course.area.takeIf { it.isNotBlank() },
                        if (course.mine && course.shared) stringResource(R.string.course_shared_badge) else null).joinToString(" · ")
                        .ifBlank { stringResource(R.string.course_runs, course.runCount) },
                    style = runTextStyle(13.sp, t.label), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                RewardLine(course)
            }
            RunNumber("%.1f".format(course.distanceKm), unit = "km", size = 30.sp, unitSize = 16.sp)
        }
        RunDivider(Modifier.padding(horizontal = 14.dp))
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RankLink(onOpenRanking, Modifier.testTag("course-rank-${course.id}"))
            Box(Modifier.width(1.dp).height(20.dp).background(t.divider))
            LikeButton(course, onLike)
            if (onShareToggle != null) {
                Box(Modifier.width(1.dp).height(20.dp).background(t.divider))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.run_course_share_mine), style = runTextStyle(12.sp, t.label, FontWeight.SemiBold),
                    modifier = Modifier.weight(1f), maxLines = 1)
                Switch(
                    checked = course.shared, onCheckedChange = { onShareToggle() },
                    colors = runSwitchColors(),
                    modifier = Modifier.testTag("course-share-${course.id}"),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
        }
        if (onDelete != null) {
            RunTextAction(stringResource(R.string.run_course_delete), onDelete, danger = true,
                modifier = Modifier.align(Alignment.End).padding(end = 6.dp).testTag("course-delete-${course.id}"))
        }
    }
}

@Composable
private fun RankLink(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val t = runTone()
    Row(
        modifier.clip(RoundedCornerShape(10.dp)).feedbackClickable(onClick = onClick).heightIn(min = 44.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.EmojiEvents, null, tint = t.cobaltText, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.run_course_rank_short), style = runTextStyle(14.sp, t.text, FontWeight.SemiBold))
        Icon(Icons.Filled.ChevronRight, null, tint = t.label, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun LikeButton(course: RunCourse, onLike: () -> Unit) {
    val t = runTone()
    val label = stringResource(if (course.liked) R.string.course_unlike_action else R.string.course_like_action)
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).feedbackClickable(onClick = onLike).heightIn(min = 44.dp).padding(horizontal = 10.dp)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (course.liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, null,
            tint = if (course.liked) Color(0xFFFF4D5E) else t.label, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text("${course.likes}", style = runTextStyle(14.sp, t.text, FontWeight.SemiBold))
    }
}

@Composable
private fun runSwitchColors() = runTone().let { t ->
    SwitchDefaults.colors(
        checkedThumbColor = Color.White, checkedTrackColor = t.cobalt,
        uncheckedThumbColor = Color.White, uncheckedTrackColor = t.track, uncheckedBorderColor = t.panelEdge,
    )
}

/** 게시판 한 장(K11) — 순위 · 지도(왼쪽) · 이름 · 거리 · 동네 · 만든 사람 · 하트 */
@Composable
private fun BoardCourseCard(
    course: RunCourse,
    rank: Int,
    selected: Boolean,
    onOpen: () -> Unit,
    onLike: () -> Unit,
    onOpenRanking: () -> Unit,
) {
    val t = runTone()
    RunCard(padding = PaddingValues(8.dp), onClick = onOpen, selected = selected, tag = "board-card-${course.id}") {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Box(Modifier.weight(1f).height(BoardMapHeight)) {
                CourseMap(course.points, Modifier.fillMaxSize())
                Box(
                    Modifier.padding(6.dp).size(30.dp).clip(RoundedCornerShape(8.dp))
                        .background(if (rank <= 3) Color(0xFF0754FF) else t.inset).border(1.dp, Color(0xFF4D8BFF), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$rank", style = runNumberStyle(18.sp, Color.White))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                Text(course.name, style = runTextStyle(18.sp, t.text, FontWeight.ExtraBold), maxLines = 2, overflow = TextOverflow.Ellipsis)
                RunNumber("%.1f".format(course.distanceKm), unit = "km", size = 28.sp, unitSize = 15.sp)
                if (course.area.isNotBlank()) {
                    Text(course.area, style = runTextStyle(13.sp, t.label), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                RewardLine(course)
                if (course.author.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RunInitials(course.author.take(1), size = 26.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.course_by, course.author), style = runTextStyle(13.sp, t.text, FontWeight.SemiBold),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(4.dp))
                RunDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LikeButton(course, onLike)
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).feedbackClickable(onClick = onOpenRanking),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.EmojiEvents, stringResource(R.string.run_course_rank_short), tint = t.cobaltText, modifier = Modifier.size(20.dp)) }
                    Icon(Icons.Filled.ChevronRight, null, tint = t.label, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

/** 게시판 칸의 지도 높이 */
private val BoardMapHeight = 176.dp

/** 게시판 위 "내 코스 올리기" */
@Composable
private fun UploadChip(onClick: () -> Unit) {
    val t = runTone()
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.heightIn(min = 52.dp).clip(shape).background(t.secondaryFace, shape).border(1.5.dp, t.secondaryEdge, shape)
            .feedbackClickable(onClick = onClick).padding(horizontal = 14.dp).testTag("courses-upload"),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.run_course_upload), style = runTextStyle(14.sp, t.text, FontWeight.Bold), maxLines = 1)
    }
}

/** K18 — 코스를 불러오지 못했어요. 저장한 내 코스는 볼 수 있다 */
@Composable
private fun BoardFailed(signIn: Boolean, onRetry: () -> Unit, onMine: () -> Unit) {
    val t = runTone()
    Column(Modifier.fillMaxWidth().padding(top = 12.dp).testTag("courses-board-failed"), horizontalAlignment = Alignment.CenterHorizontally) {
        RunEmptyState(
            Icons.Outlined.CloudOff,
            stringResource(if (signIn) R.string.board_sign_in_needed else R.string.run_course_board_failed_title),
            body = if (signIn) null else stringResource(R.string.run_course_board_failed_body),
            alert = true,
        )
        Spacer(Modifier.height(18.dp))
        RunButton(stringResource(R.string.run_course_board_reload), onRetry, icon = Icons.Filled.Refresh,
            modifier = Modifier.testTag("courses-board-retry"))
        Spacer(Modifier.height(14.dp))
        RunDivider()
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.run_course_board_local), style = runTextStyle(14.sp, t.label, FontWeight.Medium))
        Spacer(Modifier.height(10.dp))
        RunButton(stringResource(R.string.run_course_board_mine), onMine, kind = RunButtonKind.Secondary,
            modifier = Modifier.testTag("courses-board-mine"))
    }
}

/** K13 · K14 — 코스 고르기 · 풀기 확인 */
@Composable
private fun CourseConfirmContent(course: RunCourse, clearing: Boolean, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val t = runTone()
    if (clearing) {
        RunSheetText(
            stringResource(R.string.run_course_clear_title), body = course.name,
            note = stringResource(R.string.run_course_clear_note),
        )
    } else {
        Text(stringResource(R.string.run_course_apply_title), style = runTextStyle(24.sp, t.text, FontWeight.ExtraBold),
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        CourseMap(course.points, Modifier.fillMaxWidth().height(170.dp))
        Spacer(Modifier.height(10.dp))
        Column(Modifier.fillMaxWidth()) {
            Text(course.name, style = runTextStyle(18.sp, t.text, FontWeight.ExtraBold))
            RunNumber("%.1f".format(course.distanceKm), unit = "km", size = 32.sp, unitSize = 17.sp)
            Text(stringResource(R.string.run_course_apply_note), style = runTextStyle(14.sp, t.label))
            RewardLine(course)
        }
    }
    Spacer(Modifier.height(18.dp))
    RunButton(
        stringResource(if (clearing) R.string.run_course_clear_do else R.string.run_course_apply_do), onConfirm,
        modifier = Modifier.testTag("course-confirm"),
    )
    Spacer(Modifier.height(10.dp))
    RunButton(stringResource(R.string.run_cancel), onCancel, kind = RunButtonKind.Secondary, modifier = Modifier.testTag("course-confirm-cancel"))
}

/** K16 — 공유할 코스 선택. 올릴 것이 없으면 만드는 쪽으로 보낸다(빈 목록만 두면 고장 난 버튼으로 읽힌다) */
@Composable
private fun CourseUploadContent(
    mine: List<RunCourse>,
    onUpload: (Long) -> Unit,
    onMake: () -> Unit,
    onCancel: () -> Unit,
) {
    val t = runTone()
    var picked by rememberSaveable { mutableStateOf(mine.firstOrNull()?.id ?: -1L) }
    Column(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.run_course_pick_upload_title), style = runTextStyle(24.sp, t.text, FontWeight.ExtraBold))
        Text(stringResource(R.string.run_course_pick_upload_sub), style = runTextStyle(15.sp, t.label, FontWeight.Medium))
    }
    Spacer(Modifier.height(14.dp))
    if (mine.isEmpty()) {
        Text(stringResource(R.string.course_upload_none), style = runTextStyle(15.sp, t.label), textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().testTag("course-upload-none"))
        Spacer(Modifier.height(18.dp))
        RunButton(stringResource(R.string.course_rec_card_title), onMake, modifier = Modifier.testTag("course-upload-make"))
    } else {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            mine.forEach { course ->
                val on = picked == course.id
                RunCard(padding = PaddingValues(10.dp), onClick = { picked = course.id }, selected = on, tag = "course-upload-${course.id}") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(24.dp).clip(CircleShape)
                                .border(2.dp, if (on) Color(0xFF4D8BFF) else t.label, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { if (on) Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF4D8BFF))) }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(course.name, style = runTextStyle(17.sp, t.text, FontWeight.ExtraBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            RunNumber("%.1f".format(course.distanceKm), unit = "km", size = 22.sp, unitSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    CourseMap(course.points, Modifier.fillMaxWidth().height(120.dp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        RunButton(stringResource(R.string.run_course_pick_upload_do), { if (picked >= 0) onUpload(picked) },
            enabled = picked >= 0, modifier = Modifier.testTag("course-upload-do"))
    }
    Spacer(Modifier.height(10.dp))
    RunButton(stringResource(R.string.run_cancel), onCancel, kind = RunButtonKind.Secondary, modifier = Modifier.testTag("course-upload-cancel"))
}

/** 코스 만들기(K10) — 마지막 러닝의 GPS 트랙을 이름 붙여 등록한다 */
@Composable
private fun CourseMaker(
    lastTrack: List<GeoPoint>,
    onCreate: (name: String, area: String, shared: Boolean) -> Unit,
) {
    val t = runTone()
    var name by rememberSaveable { mutableStateOf("") }
    var area by rememberSaveable { mutableStateOf("") }
    var share by rememberSaveable { mutableStateOf(true) }
    val km = remember(lastTrack) { lastTrack.trackDistanceKm() }

    Column(Modifier.fillMaxWidth().testTag("course-maker"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.run_course_recent), style = runTextStyle(18.sp, t.text, FontWeight.ExtraBold))
        if (lastTrack.size < 2 || km < 0.2) {
            Text(stringResource(R.string.run_course_recent_none), style = runTextStyle(14.sp, t.label),
                modifier = Modifier.testTag("course-maker-none"))
            Text(stringResource(R.string.course_make_none), style = runTextStyle(13.sp, t.muted))
        } else {
            RunMapFrame(Modifier.fillMaxWidth().height(170.dp)) {
                LiveRouteMap(points = lastTrack, modifier = Modifier.fillMaxSize(), routeColor = t.cyan)
            }
            Text(stringResource(R.string.course_make_last, "%.2f".format(km)), style = runTextStyle(14.sp, t.cyan, FontWeight.Bold))
            RunTextField(name, { name = it }, label = stringResource(R.string.run_course_field_name),
                clearLabel = stringResource(R.string.run_course_clear_text), fieldTag = "course-maker-name")
            RunTextField(area, { area = it }, label = stringResource(R.string.run_course_field_area),
                clearLabel = stringResource(R.string.run_course_clear_text), fieldTag = "course-maker-area")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.run_course_share_label), style = runTextStyle(15.sp, t.text, FontWeight.Bold))
                    Text(stringResource(R.string.run_course_share_desc), style = runTextStyle(12.sp, t.label))
                }
                Switch(checked = share, onCheckedChange = { share = it }, colors = runSwitchColors(),
                    modifier = Modifier.testTag("course-maker-share"))
            }
            RunButton(stringResource(R.string.run_course_save_this), { onCreate(name, area, share) }, kind = RunButtonKind.Secondary,
                enabled = name.isNotBlank(), modifier = Modifier.testTag("course-maker-save"))
        }
        Text(
            stringResource(R.string.course_per_km, "%.0f".format(CourseRewards.SUP_PER_KM), "%.0f".format(CourseRewards.MAX_REWARD)),
            style = runTextStyle(12.sp, t.muted),
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
 * 코스 만들기 입구(K10).
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
    val t = runTone()
    RunCard(padding = PaddingValues(18.dp), selected = recording, tag = "course-recorder") {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.run_course_make_card_title), style = runTextStyle(20.sp, t.text, FontWeight.ExtraBold))
                Text(stringResource(R.string.run_course_make_card_body), style = runTextStyle(14.sp, t.label, FontWeight.Medium),
                    modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.width(10.dp))
            Icon(Icons.Filled.Route, null, tint = t.cyan, modifier = Modifier.size(44.dp))
        }
        // 네 걸음을 번호로 적는다. 흐름이 화면 여럿에 걸쳐 있어서, 지금
        // 어디쯤인지 모르면 러닝 화면에서 "그래서 코스는?"이 된다.
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(
                R.string.course_rec_step1,
                R.string.course_rec_step2,
                R.string.course_rec_step3,
                R.string.course_rec_step4,
            ).forEachIndexed { index, res ->
                Row {
                    Text("${index + 1}", style = runTextStyle(13.sp, t.cyan, FontWeight.ExtraBold), modifier = Modifier.width(18.dp))
                    Text(stringResource(res), style = runTextStyle(13.sp, t.label))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        if (recording) {
            Text(stringResource(R.string.course_rec_waiting), style = runTextStyle(14.sp, t.cyan, FontWeight.Bold))
            Spacer(Modifier.height(10.dp))
            RunButton(stringResource(R.string.course_rec_stop), onCancel, kind = RunButtonKind.Secondary,
                modifier = Modifier.testTag("course-recorder-stop"))
        } else {
            RunButton(stringResource(R.string.run_course_record_start), onStart, hero = true, modifier = Modifier.testTag("course-recorder-start"))
        }
    }
}

/** K12 — 코스 기록 순위. 사람마다 가장 빠른 기록 하나 — 서버가 경로로 확인한 기록만 올라온다 */
@Composable
private fun CourseRankingSheet(state: CourseRankingState, course: RunCourse?, onDismiss: () -> Unit) {
    val t = runTone()
    RunSheet(onDismiss = onDismiss, modifier = Modifier.testTag("course-ranking")) {
        Text(stringResource(R.string.course_rank_title), style = runTextStyle(24.sp, t.text, FontWeight.ExtraBold),
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        if (course != null) {
            CourseMap(course.points, Modifier.fillMaxWidth().height(160.dp))
            Spacer(Modifier.height(10.dp))
        }
        Column(Modifier.fillMaxWidth()) {
            Text(state.courseName, style = runTextStyle(20.sp, t.text, FontWeight.ExtraBold))
            if (course != null) Text("%.1f km".format(course.distanceKm), style = runTextStyle(15.sp, t.cyan, FontWeight.Bold))
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (state) {
                is CourseRankingState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    RunSpinner(Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.course_rank_loading), style = runTextStyle(15.sp, t.label))
                }
                is CourseRankingState.Failed -> Text(
                    stringResource(if (state.signIn) R.string.board_sign_in_needed else R.string.course_rank_failed),
                    style = runTextStyle(15.sp, t.label),
                )
                is CourseRankingState.Ready -> if (state.rows.isEmpty()) {
                    Text(stringResource(R.string.course_rank_empty), style = runTextStyle(15.sp, t.label))
                } else {
                    state.rows.forEach { row ->
                        RunCard(padding = PaddingValues(horizontal = 14.dp, vertical = 10.dp), selected = row.isMe) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (row.isMe) {
                                    Text(stringResource(R.string.run_course_rank_my), style = runTextStyle(15.sp, t.cobaltText, FontWeight.Bold))
                                    Spacer(Modifier.width(10.dp))
                                } else {
                                    Text("${row.rank}", style = runNumberStyle(24.sp, t.text), modifier = Modifier.width(30.dp))
                                }
                                RunInitials(row.displayName.take(1), size = 36.dp, filled = row.rank <= 3)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    if (row.isMe) stringResource(R.string.course_rank_me, row.displayName) else row.displayName,
                                    style = runTextStyle(16.sp, t.text, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(formatDuration(row.durationSec), style = runNumberStyle(24.sp, t.text))
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        RunButton(stringResource(R.string.run_close), onDismiss, modifier = Modifier.testTag("course-ranking-close"))
    }
}

/** 초 → "m:ss" 또는 "h:mm:ss" */
private fun formatDuration(sec: Int): String {
    val h = sec / 3600
    val m = sec % 3600 / 60
    val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
