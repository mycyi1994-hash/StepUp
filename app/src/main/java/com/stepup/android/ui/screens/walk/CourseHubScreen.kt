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
import androidx.compose.ui.semantics.heading
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Toll
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.fillMaxHeight
import com.stepup.android.ui.components.BottomActionBar
import com.stepup.android.ui.components.CircleIconButton
import com.stepup.android.ui.components.GroupHeader
import com.stepup.android.ui.components.GroupRow
import com.stepup.android.ui.components.GroupedList
import com.stepup.android.ui.components.NumberWithUnit
import com.stepup.android.ui.components.PlaceThumb
import com.stepup.android.ui.components.PrimaryButton
import com.stepup.android.ui.components.SecondaryButton
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.components.TextTabs
import com.stepup.android.ui.components.TitleSubtitle
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
    // 길 비교(encode)는 목록 · 고른 코스가 바뀔 때만 — 다시 그릴 때마다 줄마다 길 전체를 글자로 만들지 않게
    val sameTrackIds = remember(courses, boardCourses, selectedTrack) {
        val track = selectedTrack ?: return@remember emptySet()
        (courses + boardCourses).filter { it.encode() == track }.mapTo(HashSet()) { it.id }
    }
    fun isSelected(course: RunCourse): Boolean = course.id == selectedId || course.id in sameTrackIds

    // 게시판 정렬 — 좋아요 순(기본) · 가까운 순(내 위치에서 코스 출발점까지, 모르면 좋아요 순) · 짧은 코스
    var sort by rememberSaveable { mutableStateOf(0) }
    val here = com.stepup.android.ui.components.rememberCurrentLocation()
    // 게시판 — 검색어 · 정렬 · 목록이 바뀔 때만 거르고 줄 세운다
    val sortedBoard = remember(boardCourses, query, sort, here) {
        val liked = compareByDescending<RunCourse> { it.likes }.thenByDescending { it.runCount }.thenByDescending { it.createdAt }
        val list = boardCourses.filter { it.matches(query) }
        when {
            sort == 2 -> list.sortedWith(compareBy<RunCourse> { it.distanceKm }.then(liked))
            sort == 1 && here != null -> list.sortedWith(
                compareBy<RunCourse> { c -> c.points.firstOrNull()?.let { com.stepup.android.domain.haversineMeters(here, it) } ?: Double.MAX_VALUE }
                    .then(liked),
            )
            else -> list.sortedWith(liked)
        }
    }

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
            // 머리 — ← · "러닝 코스"(시안 courses) → 세그먼트 선택 · 만들기 · 게시판
            Box(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 12.dp)) {
                CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back), onBack,
                    modifier = Modifier.align(Alignment.CenterStart).testTag("kit-back"))
                Text(stringResource(R.string.courses_hub_title), style = StepUpUi.type.navTitle, color = StepUpUi.colors.label,
                    modifier = Modifier.align(Alignment.Center).semantics { heading() }.testTag("courses-title"))
            }
            RunTabs(
                labels = listOf(
                    stringResource(R.string.run_courses_tab_select),
                    stringResource(R.string.courses_make),
                    stringResource(R.string.courses_board),
                ),
                selected = tab, onSelect = { tab = it }, tagPrefix = "courses-tab",
                modifier = Modifier.padding(horizontal = StepUpUi.Gutter).padding(top = 6.dp),
            )
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth().testTag("courses-list"),
                contentPadding = PaddingValues(top = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                when (tab) {
                    // ── 코스 선택(K09) — 고른 코스(없으면 첫 코스)를 크게, 나머지는 "다른 코스" 목록 ─────────────
                    0 -> {
                        val hero = courses.firstOrNull { it.id == selectedId } ?: courses.firstOrNull()
                        if (hero == null) {
                            item {
                                RunEmptyState(Icons.Outlined.Map, stringResource(R.string.courses_empty),
                                    modifier = Modifier.padding(top = 24.dp).padding(horizontal = StepUpUi.Gutter).testTag("courses-empty"))
                            }
                        } else {
                            val heroSelected = hero.id == selectedId
                            item(key = "hero-${hero.id}") {
                                Column {
                                    CourseHero(hero, heroSelected, onOpen = { pendingId = hero.id }, onMore = { pendingId = hero.id })
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = StepUpUi.Gutter).padding(top = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        SecondaryButton(stringResource(R.string.courses_rank_button), onClick = { openRanking(hero) },
                                            icon = Icons.Outlined.EmojiEvents, modifier = Modifier.weight(.36f).testTag("course-rank-${hero.id}"))
                                        if (heroSelected && onRunCourse != null) {
                                            PrimaryButton(stringResource(R.string.run_course_run_this), onClick = onRunCourse,
                                                modifier = Modifier.weight(.64f).testTag("courses-run"))
                                        } else {
                                            PrimaryButton(
                                                stringResource(if (heroSelected) R.string.run_course_clear_do else R.string.courses_pick_this),
                                                onClick = { pendingId = hero.id }, modifier = Modifier.weight(.64f).testTag("course-pick-hero"),
                                            )
                                        }
                                    }
                                }
                            }
                            val others = courses.filter { it.id != hero.id }
                            if (others.isNotEmpty()) {
                                item(key = "others") {
                                    Column {
                                        GroupHeader(stringResource(R.string.courses_others))
                                        GroupedList(rows = others.map { course ->
                                            @Composable { CourseRow(course, onClick = { pendingId = course.id }) }
                                        })
                                    }
                                }
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
                        // 녹화를 걸지 않고 뛴 경우에도 방금 달린 길은 남아 있다 — 예전 등록 칸을 밑에 둔다
                        item {
                            Box(Modifier.padding(horizontal = StepUpUi.Gutter).padding(top = 28.dp)) {
                                CourseMaker(lastTrack = lastTrack, onCreate = { name, area, shared ->
                                    viewModel.create(name, area, lastTrack, shared)
                                    tab = 0
                                })
                            }
                        }
                    }

                    // ── 코스 게시판(K11 · K17 · K18) — 러너들이 공유한 코스(좋아요 순 · 가까운 순 · 짧은 코스) ─────────────
                    else -> {
                        item {
                            RunTextField(
                                value = query, onValueChange = { query = it },
                                placeholder = stringResource(R.string.run_course_search), leading = Icons.Outlined.Search,
                                clearLabel = stringResource(R.string.run_course_clear_text), fieldTag = "courses-search",
                                modifier = Modifier.padding(horizontal = StepUpUi.Gutter),
                            )
                        }
                        item {
                            TextTabs(
                                listOf(stringResource(R.string.courses_sort_likes), stringResource(R.string.courses_sort_near), stringResource(R.string.courses_sort_short)),
                                sort, { sort = it }, Modifier.padding(horizontal = StepUpUi.TitleGutter).padding(top = 10.dp, bottom = 10.dp),
                                tags = listOf("courses-sort-likes", "courses-sort-near", "courses-sort-short"),
                            )
                        }
                        val board = sortedBoard
                        val failed = boardSync is BoardSyncState.Failed || boardSync is BoardSyncState.SignInRequired
                        // 서버를 못 읽었으면 왜 못 읽었는지부터(K18). 폰의 코스는 그 아래에 그대로 둔다.
                        if (failed) {
                            item {
                                Box(Modifier.padding(horizontal = StepUpUi.Gutter)) {
                                    BoardFailed(signIn = boardSync is BoardSyncState.SignInRequired, onRetry = viewModel::refreshBoard, onMine = { tab = 0 })
                                }
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
                                Box(Modifier.padding(horizontal = StepUpUi.Gutter)) {
                                    if (query.isBlank()) {
                                        BoardEmpty(onUpload = { uploadPicker = true })
                                    } else {
                                        Text(stringResource(R.string.courses_search_empty), style = StepUpUi.type.body, color = StepUpUi.colors.secondaryLabel,
                                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 24.dp).testTag("courses-search-empty"))
                                    }
                                }
                            }
                        } else if (board.isNotEmpty()) {
                            item(key = "board") {
                                GroupedList(rows = board.mapIndexed { index, course ->
                                    @Composable { BoardRow(course, rank = index + 1, selected = isSelected(course), onOpen = { pendingId = course.id }) }
                                })
                            }
                        }
                    }
                }
            }
            // 게시판 — 내 코스 올리기(보조, 아래 한 줄)
            if (tab == 2) {
                BottomActionBar {
                    SecondaryButton(stringResource(R.string.run_course_upload), onClick = { uploadPicker = true }, icon = Icons.Rounded.Add,
                        modifier = Modifier.weight(1f).testTag("courses-upload"))
                }
            }
        }

        // K12 — 코스 순위
        val ranking by viewModel.ranking.collectAsStateWithLifecycle()
        ranking?.let { state ->
            val course = courses.firstOrNull { it.id == rankingCourseId } ?: boardCourses.firstOrNull { it.id == rankingCourseId }
            CourseRankingSheet(
                state = state, course = course, onDismiss = viewModel::closeRanking,
                onRun = if (onRunCourse != null && course != null && isSelected(course)) ({ viewModel.closeRanking(); onRunCourse() }) else null,
            )
        }

        // K13 · K14 — 이 코스를 선택할까요? · 선택한 코스를 해제할까요? 좋아요 · 순위 · 공유 · 지우기도 여기서
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
                    onLike = { viewModel.toggleLike(pending.id) },
                    onRanking = { val c = pending; pendingId = -1L; openRanking(c) },
                    onShareToggle = if (pending.mine) ({ viewModel.setShared(pending.id, !pending.shared) }) else null,
                    onDelete = if (pending.mine) ({ val id = pending.id; pendingId = -1L; viewModel.delete(id) }) else null,
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

/**
 * 고른 코스(시안 courses 위) — 둥근 지도 카드 361 × 230 · 모서리 24, 실제 길(블루 실선) · 아래로 어둡게 · 이름 · 동네 · 큰 거리.
 * 누르면 고르기 · 풀기 확인(좋아요 · 순위 · 공유 · 지우기도 그 안에).
 */
@Composable
private fun CourseHero(course: RunCourse, selected: Boolean, onOpen: () -> Unit, onMore: () -> Unit) {
    val t = runTone()
    Box(
        Modifier.padding(horizontal = StepUpUi.Gutter).fillMaxWidth().height(230.dp).clip(RoundedCornerShape(24.dp))
            .background(StepUpUi.colors.mapBackground)
            .testTag("course-card-${course.id}"),
    ) {
        CompositionLocalProvider(com.stepup.android.ui.components.LocalMapTone provides com.stepup.android.ui.components.MapTone(t.mapFilter, t.mapShade, t.mapRoads)) {
            LiveRouteMap(points = course.points, modifier = Modifier.fillMaxSize(), routeColor = FreeRunRoute)
        }
        Box(Modifier.fillMaxWidth().fillMaxHeight(.52f).align(Alignment.BottomCenter)
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .62f)))))
        Box(
            Modifier.fillMaxSize().feedbackClickable(role = Role.RadioButton, onClick = onOpen)
                .semantics { this.selected = selected }.testTag("course-pick-${course.id}"),
        )
        CircleIconButton(Icons.Rounded.MoreHoriz, stringResource(R.string.courses_more), onMore, onPhoto = true,
            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).testTag("course-more-${course.id}"))
        Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 14.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(course.name, style = StepUpUi.type.section.copy(fontSize = 22.sp), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(courseMeta(course), style = StepUpUi.type.rowSubtitle, color = Color.White.copy(alpha = .75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            NumberWithUnit("%.2f".format(course.distanceKm), unit = "km", style = StepUpUi.type.numberStat.copy(fontSize = 52.sp),
                color = Color.White, unitColor = Color.White.copy(alpha = .75f), unitSize = 15.6.sp)
        }
    }
}

/** "동네 · 공유 중 · 최대 보상" — 코스 한 줄 설명(있는 값만) */
@Composable
private fun courseMeta(course: RunCourse): String = listOfNotNull(
    course.area.takeIf { it.isNotBlank() },
    if (course.mine && course.shared) stringResource(R.string.course_shared_badge) else null,
    if (course.serverReward > 0) stringResource(R.string.course_reward_upto, "%.0f".format(course.serverReward)) else null,
).joinToString(" · ").ifBlank { stringResource(R.string.course_runs, course.runCount) }

/** 다른 코스 한 줄(시안 courses 아래) — 장소 사진 · 이름 · 동네 · 거리. 누르면 고르기 확인 */
@Composable
private fun CourseRow(course: RunCourse, onClick: () -> Unit) {
    val photo = com.stepup.android.ui.background.rememberPlacePhoto(course.area, course.points.firstOrNull()?.lat, course.points.firstOrNull()?.lng,
        seed = "course-${course.id}", widthDp = 52)
    GroupRow(Modifier.testTag("course-pick-${course.id}"), onClick = onClick, minHeight = 76.dp) {
        PlaceThumb(photo?.painter)
        TitleSubtitle(course.name, courseMeta(course), Modifier.weight(1f))
        NumberWithUnit("%.2f".format(course.distanceKm), unit = "km", style = StepUpUi.type.numberRow.copy(fontSize = 28.sp))
    }
}

/** 게시판 한 줄(시안 course_board) — 순위(1위는 블루) · 장소 사진 · 이름 · "동네 · 거리" · 하트 수 */
@Composable
private fun BoardRow(course: RunCourse, rank: Int, selected: Boolean, onOpen: () -> Unit) {
    val c = StepUpUi.colors
    val photo = com.stepup.android.ui.background.rememberPlacePhoto(course.area, course.points.firstOrNull()?.lat, course.points.firstOrNull()?.lng,
        seed = "course-${course.id}", widthDp = 52)
    GroupRow(Modifier.semantics { this.selected = selected }.testTag("board-card-${course.id}"), onClick = onOpen, minHeight = 76.dp) {
        Text("$rank", style = StepUpUi.type.numberRow, color = if (rank == 1) c.accentText else c.tertiaryLabel,
            modifier = Modifier.width(16.dp))
        PlaceThumb(photo?.painter)
        TitleSubtitle(
            course.name,
            listOfNotNull(course.area.takeIf { it.isNotBlank() }, "%.2fkm".format(course.distanceKm), if (selected) stringResource(R.string.course_selected_badge) else null)
                .joinToString(" · "),
            Modifier.weight(1f),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (course.liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null,
                tint = if (course.liked) c.live else c.secondaryLabel, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("${course.likes}", style = StepUpUi.type.rowSubtitle, color = c.secondaryLabel)
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

/** K17 — 아직 공유된 코스가 없어요. 내 코스를 올리는 것이 이 자리의 큰 행동 */
@Composable
internal fun BoardEmpty(onUpload: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        RunEmptyState(
            Icons.Outlined.Map, stringResource(R.string.run_course_board_empty_title),
            body = stringResource(R.string.run_course_board_empty_body),
            modifier = Modifier.testTag("courses-board-empty"),
        )
        Spacer(Modifier.height(20.dp))
        RunButton(stringResource(R.string.run_course_upload), onUpload, hero = true,
            modifier = Modifier.testTag("courses-board-empty-upload"))
    }
}

/** K18 — 코스를 불러오지 못했어요. 저장한 내 코스는 볼 수 있다 */
@Composable
internal fun BoardFailed(signIn: Boolean, onRetry: () -> Unit, onMine: () -> Unit) {
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

/** K13 · K14 — 코스 고르기 · 풀기 확인. 아래 줄에 좋아요 · 순위, 내 코스면 공유 · 지우기(예전 카드 아래 줄을 옮겼다) */
@Composable
private fun CourseConfirmContent(
    course: RunCourse,
    clearing: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onLike: () -> Unit = {},
    onRanking: () -> Unit = {},
    onShareToggle: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val t = runTone()
    val c = StepUpUi.colors
    Text(
        stringResource(if (clearing) R.string.run_course_clear_title else R.string.run_course_apply_title),
        style = StepUpUi.type.section.copy(fontSize = 24.sp), color = c.label, modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    CourseMap(course.points, Modifier.fillMaxWidth().height(160.dp))
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(course.name, style = StepUpUi.type.rowTitle.copy(fontSize = 18.sp), color = c.label)
            Text(courseMeta(course), style = StepUpUi.type.rowSubtitle, color = c.secondaryLabel)
        }
        NumberWithUnit("%.2f".format(course.distanceKm), unit = "km", style = StepUpUi.type.numberStat)
    }
    Text(
        stringResource(if (clearing) R.string.run_course_clear_note else R.string.run_course_apply_note),
        style = StepUpUi.type.caption, color = c.secondaryLabel, modifier = Modifier.padding(top = 6.dp),
    )
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        LikeButton(course, onLike)
        RankLink(onRanking, Modifier.testTag("course-confirm-rank"))
        if (onShareToggle != null) {
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.run_course_share_mine), style = StepUpUi.type.caption, color = c.secondaryLabel, maxLines = 1)
            Spacer(Modifier.width(8.dp))
            Switch(checked = course.shared, onCheckedChange = { onShareToggle() }, colors = runSwitchColors(),
                modifier = Modifier.testTag("course-share-${course.id}"))
        }
    }
    Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SecondaryButton(stringResource(R.string.run_cancel), onClick = onCancel, modifier = Modifier.weight(.36f).testTag("course-confirm-cancel"))
        PrimaryButton(stringResource(if (clearing) R.string.run_course_clear_do else R.string.run_course_apply_do), onClick = onConfirm,
            modifier = Modifier.weight(.64f).testTag("course-confirm"))
    }
    if (onDelete != null) {
        Box(Modifier.fillMaxWidth().padding(top = 6.dp), contentAlignment = Alignment.Center) {
            RunTextAction(stringResource(R.string.run_course_delete), onDelete, danger = true, modifier = Modifier.testTag("course-delete-${course.id}"))
        }
    }
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
    val c = StepUpUi.colors
    // 시안 course_make — 큰 제목 → 번호 줄 묶음 → 보상 줄 → 기록 시작
    Column(Modifier.fillMaxWidth().testTag("course-recorder")) {
        Text(stringResource(R.string.courses_make_headline), style = StepUpUi.type.largeTitle.copy(fontSize = 32.sp), color = c.label,
            modifier = Modifier.padding(horizontal = StepUpUi.TitleGutter).padding(top = 12.dp, bottom = 22.dp).semantics { heading() })
        // 네 걸음을 번호로 적는다 — 흐름이 화면 여럿에 걸쳐 있어서, 지금 어디쯤인지 모르면 러닝 화면에서 "그래서 코스는?"이 된다
        GroupedList(rows = listOf(R.string.course_rec_step1, R.string.course_rec_step2, R.string.course_rec_step3, R.string.course_rec_step4)
            .mapIndexed { index, res ->
                @Composable {
                    GroupRow(minHeight = 66.dp) {
                        Text("${index + 1}", style = StepUpUi.type.numberRow.copy(fontSize = 30.sp), color = c.accentText, modifier = Modifier.width(22.dp))
                        Text(stringResource(res), style = StepUpUi.type.rowTitle, color = c.label, modifier = Modifier.weight(1f))
                    }
                }
            })
        Spacer(Modifier.height(12.dp))
        GroupedList(rows = listOf(@Composable {
            GroupRow(minHeight = 68.dp) {
                Icon(Icons.Outlined.Toll, null, tint = c.accentText, modifier = Modifier.size(24.dp))
                Text(
                    stringResource(R.string.course_per_km, "%.0f".format(CourseRewards.SUP_PER_KM), "%.0f".format(CourseRewards.MAX_REWARD)),
                    style = StepUpUi.type.rowSubtitle, color = c.label, modifier = Modifier.weight(1f),
                )
            }
        }))
        Box(Modifier.padding(horizontal = StepUpUi.Gutter).padding(top = 20.dp)) {
            if (recording) {
                Column {
                    Text(stringResource(R.string.course_rec_waiting), style = StepUpUi.type.rowTitle, color = c.accentText)
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton(stringResource(R.string.course_rec_stop), onClick = onCancel, modifier = Modifier.fillMaxWidth().testTag("course-recorder-stop"))
                }
            } else {
                PrimaryButton(stringResource(R.string.run_course_record_start), onClick = onStart, modifier = Modifier.fillMaxWidth().testTag("course-recorder-start"))
            }
        }
    }
}

/** K12 — 코스 기록 순위(시안 rank 시트). 사람마다 가장 빠른 기록 하나 — 서버가 경로로 확인한 기록만 올라온다 */
@Composable
private fun CourseRankingSheet(state: CourseRankingState, course: RunCourse?, onDismiss: () -> Unit, onRun: (() -> Unit)? = null) {
    val c = StepUpUi.colors
    RunSheet(onDismiss = onDismiss, modifier = Modifier.testTag("course-ranking")) {
        Text(stringResource(R.string.course_rank_title), style = StepUpUi.type.section.copy(fontSize = 24.sp), color = c.label,
            modifier = Modifier.fillMaxWidth())
        Text(
            listOfNotNull(state.courseName, course?.let { "%.2fkm".format(it.distanceKm) }).joinToString(" · "),
            style = StepUpUi.type.rowSubtitle.copy(fontSize = 14.sp), color = c.secondaryLabel, modifier = Modifier.padding(top = 3.dp),
        )
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            when (state) {
                is CourseRankingState.Loading -> Row(Modifier.padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    RunSpinner(Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.course_rank_loading), style = StepUpUi.type.body, color = c.secondaryLabel)
                }
                is CourseRankingState.Failed -> Text(
                    stringResource(if (state.signIn) R.string.board_sign_in_needed else R.string.course_rank_failed),
                    style = StepUpUi.type.body, color = c.secondaryLabel, modifier = Modifier.padding(vertical = 18.dp),
                )
                is CourseRankingState.Ready -> if (state.rows.isEmpty()) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.course_rank_empty_title), style = StepUpUi.type.rowTitle.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold), color = c.label)
                        Text(stringResource(R.string.course_rank_empty_body), style = StepUpUi.type.rowSubtitle.copy(fontSize = 14.sp), color = c.secondaryLabel,
                            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                    }
                } else {
                    state.rows.forEachIndexed { i, row ->
                        if (i > 0) Box(Modifier.fillMaxWidth().height(.5.dp).background(c.separator))
                        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (row.isMe) "·" else "${row.rank}", style = StepUpUi.type.numberRow,
                                color = if (row.rank == 1) c.accentText else c.tertiaryLabel, modifier = Modifier.width(30.dp))
                            Text(
                                if (row.isMe) stringResource(R.string.course_rank_me, row.displayName) else row.displayName,
                                style = StepUpUi.type.rowTitle, color = if (row.isMe) c.accentText else c.label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(formatDuration(row.durationSec), style = StepUpUi.type.numberRow, color = c.label)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (onRun != null) {
                SecondaryButton(stringResource(R.string.run_close), onClick = onDismiss, modifier = Modifier.weight(.36f).testTag("course-ranking-close"))
                PrimaryButton(stringResource(R.string.run_course_run_this), onClick = onRun, modifier = Modifier.weight(.64f).testTag("course-ranking-run"))
            } else {
                SecondaryButton(stringResource(R.string.run_close), onClick = onDismiss, modifier = Modifier.weight(1f).testTag("course-ranking-close"))
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
