package com.stepup.android.ui.screens.community.stories

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.WrongLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.core.ExternalIntents
import com.stepup.android.data.repo.BoardSyncState
import com.stepup.android.data.repo.StoryNotice
import com.stepup.android.domain.StoryOrigin
import com.stepup.android.domain.StoryPlace
import com.stepup.android.domain.StoryRange
import com.stepup.android.ui.StepPermissions
import com.stepup.android.ui.components.RunSheetPanel
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.SignInAgainButton
import com.stepup.android.ui.components.rememberCurrentLocation
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.guide.GuideTour
import com.stepup.android.ui.guide.guideTarget
import com.stepup.android.ui.theme.StepUpSans
import kotlinx.coroutines.delay

/**
 * 러닝 이야기 — 커뮤니티 첫 화면(커뮤니티 기본 CM01 · CM30~35 · CM43, 2026-10-03 파란 톤).
 *
 * 위는 왼쪽 지역 설명("도화동 / 주변 이야기", 거리 기준, 범위 변경)과 오른쪽 작은 실제 지도, 아래는 가까운 러너들의 글.
 * 글 행의 오른쪽 썸네일은 글쓴이가 고른 공개 장소이고, 거리는 보는 사람의 위치(또는 고른 지역)에서 잰다.
 * 화면의 큰 주 행동은 아래 "+ 글쓰기" 하나 — 글이 없거나 실패면 그 상황의 행동(첫 이야기 · 다시 불러오기)이 그 자리에 선다.
 */
@Composable
fun StoriesTab(
    viewModel: StoriesViewModel,
    onOpenPost: (Long) -> Unit,
    onOpenMap: (StoryPlace?) -> Unit,
    onWrite: (resume: Boolean) -> Unit,
    onOpenLocation: () -> Unit,
    onOpenRegion: () -> Unit,
) {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(StepPermissions.hasLocation(context)) }
    LifecycleResumeEffect(Unit) {
        allowed = StepPermissions.hasLocation(context)
        onPauseOrDispose { }
    }
    val here = rememberCurrentLocation(enabled = allowed)
    LaunchedEffect(here, allowed) { viewModel.onLocation(here, allowed) }

    val status by viewModel.locationStatus.collectAsStateWithLifecycle()
    val origin by viewModel.origin.collectAsStateWithLifecycle()
    val list by viewModel.list.collectAsStateWithLifecycle()
    val sync by viewModel.sync.collectAsStateWithLifecycle()
    val range by viewModel.range.collectAsStateWithLifecycle()
    val filter by viewModel.placeFilter.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()

    // 방금 올린 글 — 지금 목록(같은 지역 · 범위 · 장소 필터)에 들어오면 맨 위로 올려 보인다. 코스 글쓰기 지시서(WRITE12):
    // 게시했다고 고른 지역 · 범위 · 필터를 바꾸지 않고, 필터 밖의 글을 목록에 억지로 넣지 않는다(예전 reveal 은 범위를 넓혔다)
    val revealId = (notice as? StoryNotice.Posted)?.postId

    var showRange by rememberSaveable { mutableStateOf(false) }
    var showResume by rememberSaveable { mutableStateOf(false) }
    // 새 글은 지금 보는 장소(목록에서 직접 고른 장소)를 먼저 넣어 둔다 — 없으면 비워 두고 고르게 한다
    val writeNew = {
        StoryComposeSeed.place = viewModel.suggestedPlace()
        onWrite(false)
    }
    val startWriting = { if (draft != null) showResume = true else writeNew() }

    // 권한 요청 — 거절 · 다시 묻지 않음이어도 "지역 직접 선택"으로 이어 갈 수 있다. 거절된 뒤에는 다시 묻지 않고 설정으로
    var denied by rememberSaveable { mutableStateOf(false) }
    var askClosed by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        allowed = StepPermissions.hasLocation(context)
        denied = !allowed
    }
    val askLocation = {
        permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    val ready = status == StoryLocationStatus.READY && origin != null
    Box(Modifier.fillMaxSize().testTag("stories-tab")) {
        if (ready) {
            StoryListBody(
                origin = origin!!,
                list = list,
                sync = sync,
                range = range,
                filter = filter,
                onOpenPost = onOpenPost,
                onOpenMap = onOpenMap,
                onClearFilter = { viewModel.filterPlace(null) },
                onOpenRange = { showRange = true },
                onOpenLocation = onOpenLocation,
                onRetry = viewModel::refresh,
                onWrite = startWriting,
                onWiden = { nextRange(range)?.let(viewModel::setRange) },
                revealId = revealId,
                onLike = viewModel::toggleLike,
                notice = {
                    notice?.let { current ->
                        StoryNoticeToast(
                            notice = current,
                            onDismiss = { viewModel.consumeNotice(current) },
                            onUndo = { (current as? StoryNotice.Hidden)?.let(viewModel::undoHide) },
                        )
                    }
                },
            )
        } else {
            StoryLocationBody(
                locating = status == StoryLocationStatus.LOCATING,
                denied = denied,
                askOpen = status == StoryLocationStatus.NEED_PERMISSION && !askClosed,
                onCloseAsk = { askClosed = true },
                onUseLocation = askLocation,
                onChooseRegion = onOpenRegion,
                onOpenSettings = { ExternalIntents.openAppSettings(context) },
            )
        }
    }

    if (showRange) {
        StoryRangeSheet(
            current = range,
            origin = origin,
            onApply = { viewModel.setRange(it); showRange = false },
            onChangeRegion = { showRange = false; onOpenRegion() },
            onDismiss = { showRange = false },
        )
    }
    val pendingDraft = draft
    if (showResume && pendingDraft != null) {
        val t = runTone()
        StorySheet(title = stringResource(R.string.story_resume_title), onDismiss = { showResume = false }) {
            Spacer(Modifier.height(8.dp))
            val shape = RoundedCornerShape(14.dp)
            Column(
                Modifier.fillMaxWidth().clip(shape).background(t.inset).border(1.dp, t.panelEdge, shape)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Text(pendingDraft.preview, style = runTextStyle(17.sp, t.text, FontWeight.SemiBold), maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(
                    listOfNotNull(pendingDraft.place?.name, stringResource(R.string.story_blue_draft_saved_tag)).joinToString(" · "),
                    style = runTextStyle(14.sp, t.label, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(18.dp))
            StoryButton(stringResource(R.string.story_resume_continue), {
                showResume = false
                onWrite(true)
            }, Modifier.testTag("story-resume-continue"), italic = true)
            Spacer(Modifier.height(4.dp))
            StoryTextButton(stringResource(R.string.story_blue_resume_new), {
                showResume = false
                viewModel.discardDraft()
                writeNew()
            }, Modifier.align(Alignment.CenterHorizontally).testTag("story-resume-new"))
        }
    }
}

private fun nextRange(range: StoryRange): StoryRange? = when (range) {
    StoryRange.M500 -> StoryRange.KM1
    StoryRange.KM1 -> StoryRange.KM3
    StoryRange.KM3 -> null
}

/** 큰 제목 — "도화동 / 주변 이야기" */
@Composable
private fun storyTitleStyle(): TextStyle = TextStyle(
    fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 1.25.em,
    letterSpacing = (-0.03).em, color = runTone().text,
)

/**
 * 목록 위 머리 — 왼쪽 지역 설명, 오른쪽 작은 지도(또는 위치 확인 칸). 큰 글씨 · 좁은 화면에서도 지도는 줄고 글자는 넘김 안에 남는다.
 */
@Composable
private fun StoryHead(
    title: String,
    modifier: Modifier = Modifier,
    onTitle: (() -> Unit)? = null,
    details: @Composable () -> Unit,
    side: @Composable () -> Unit,
) {
    val large = LocalDensity.current.fontScale > 1.3f
    Row(modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                title, style = storyTitleStyle(),
                modifier = Modifier.semantics { heading() }
                    .then(
                        if (onTitle != null) Modifier.clip(RoundedCornerShape(10.dp))
                            .feedbackClickable(role = Role.Button, onClick = onTitle).testTag("stories-area")
                        else Modifier,
                    ),
            )
            Spacer(Modifier.height(8.dp))
            details()
        }
        Box(Modifier.weight(if (large) 0.75f else 0.85f)) { side() }
    }
}

/** 오른쪽 위 작은 칸 — 지도 자리. 지도를 아직 그릴 수 없으면(위치 모름) 가짜 지도 대신 안내 */
@Composable
private fun StoryHeadCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val t = runTone()
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier.fillMaxWidth().height(150.dp).clip(shape).background(t.inset)
            .border(1.dp, if (t.dark) t.cyan.copy(alpha = 0.45f) else t.panelEdge, shape),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** 목록 제목 줄 — "가까운 러너들의 글 · 최신순"(정렬 설명이지 메뉴가 아니다), 장소를 골랐으면 장소 이름 · 전체 장소 */
@Composable
private fun StorySectionTitle(filter: StoryPlace?, count: Int, onClearFilter: () -> Unit) {
    val t = runTone()
    Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            if (filter != null) {
                Text(
                    filter.name, style = runTextStyle(21.sp, t.text, FontWeight.ExtraBold), maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).semantics { heading() }.testTag("stories-title"),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.story_post_count, count), style = runTextStyle(14.sp, t.label, FontWeight.Medium))
                Spacer(Modifier.weight(1f))
                StoryLink(stringResource(R.string.story_all_places), onClearFilter, Modifier.testTag("stories-clear-filter"), trailing = null)
            } else {
                Text(
                    stringResource(R.string.story_blue_nearby_title), style = runTextStyle(21.sp, t.text, FontWeight.ExtraBold),
                    modifier = Modifier.weight(1f).semantics { heading() }.testTag("stories-title"),
                )
                Text(stringResource(R.string.story_blue_latest), style = runTextStyle(14.sp, t.label, FontWeight.Medium))
            }
        }
        Spacer(Modifier.height(6.dp))
        StoryDivider()
    }
}

/** 아래 큰 버튼 자리 — 한 화면에 큰 주 행동 하나 */
@Composable
private fun StoryBottomAction(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(start = StoryListGutter, end = StoryListGutter, top = 8.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

@Composable
private fun StoryListBody(
    origin: StoryOrigin,
    list: StoryListState,
    sync: BoardSyncState,
    range: StoryRange,
    filter: StoryPlace?,
    onOpenPost: (Long) -> Unit,
    onOpenMap: (StoryPlace?) -> Unit,
    onClearFilter: () -> Unit,
    onOpenRange: () -> Unit,
    onOpenLocation: () -> Unit,
    onRetry: () -> Unit,
    onWrite: () -> Unit,
    onWiden: () -> Unit,
    revealId: Long?,
    onLike: (Long) -> Unit,
    notice: @Composable () -> Unit,
) {
    val t = runTone()
    val listState = rememberLazyListState()
    // 장소를 고르거나 "전체 장소"로 풀면 새 목록의 처음부터 본다. 상세에 다녀올 때는(같은 목록) 보던 자리를 지킨다.
    var shownFilter by rememberSaveable { mutableStateOf(list.filterKey) }
    LaunchedEffect(list.filterKey) {
        if (list.filterKey != shownFilter) {
            shownFilter = list.filterKey
            listState.scrollToItem(0)
        }
    }
    // 방금 올린 글이 맨 위에 들어온 뒤에 올린다 — 먼저 올리면 새 줄이 화면 위로 밀려 가려진다
    val firstId = list.nearby.firstOrNull()?.post?.id
    LaunchedEffect(revealId, firstId) {
        if (revealId != null && firstId == revealId) listState.animateScrollToItem(0)
    }
    val empty = list.nearby.isEmpty()
    val loading = empty && (sync == BoardSyncState.Loading || sync == BoardSyncState.Idle)
    val failed = empty && sync is BoardSyncState.Failed
    val signIn = empty && sync == BoardSyncState.SignInRequired

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().testTag("stories-list"),
                contentPadding = PaddingValues(start = StoryListGutter, end = StoryListGutter, bottom = 24.dp),
            ) {
                item(key = "head") {
                    StoryHead(
                        title = if (origin.label.isBlank()) stringResource(R.string.story_blue_area_unknown_title)
                        else stringResource(R.string.story_blue_area_title, origin.label),
                        onTitle = onOpenLocation,
                        details = {
                            Text(
                                stringResource(
                                    if (origin.manual) R.string.story_range_from_region else R.string.story_range_from_me,
                                    rangeLabel(range),
                                ),
                                style = runTextStyle(16.sp, if (origin.manual) t.label else t.text, FontWeight.Medium),
                            )
                            if (origin.manual) {
                                Spacer(Modifier.height(2.dp))
                                Text(stringResource(R.string.story_blue_region_basis), style = runTextStyle(13.sp, t.label, FontWeight.Medium))
                            }
                            Spacer(Modifier.height(4.dp))
                            StoryLink(
                                stringResource(R.string.story_blue_change_range), onOpenRange, Modifier.testTag("stories-range"),
                                trailing = Icons.Filled.KeyboardArrowDown,
                            )
                        },
                        side = {
                            Column(horizontalAlignment = Alignment.End) {
                                val mapLabel = stringResource(R.string.story_open_map)
                                StoryHeadCard(
                                    Modifier.clip(RoundedCornerShape(18.dp))
                                        .feedbackClickable(role = Role.Button, onClick = { onOpenMap(null) })
                                        .semantics { contentDescription = mapLabel }
                                        .testTag("stories-mini-map"),
                                ) {
                                    StoryMapTone {
                                        StoryPinsMap(
                                            pins = list.pins,
                                            origin = origin.point,
                                            originLabel = stringResource(
                                                if (origin.manual) R.string.story_origin_region else R.string.story_origin_me,
                                            ),
                                            modifier = Modifier.fillMaxSize().testTag("stories-map"),
                                            selectedKey = filter?.key,
                                            rangeMeters = range.meters,
                                            fitPins = false,
                                            dotSeparator = true,
                                        )
                                    }
                                }
                                StoryLink(
                                    stringResource(R.string.story_show_map), { onOpenMap(filter) },
                                    Modifier.testTag("stories-open-map"),
                                )
                            }
                        },
                    )
                }
                item(key = "section") { StorySectionTitle(filter, list.nearby.size, onClearFilter) }
                // 이미 보이는 글이 있는데 새로 받기만 실패했다 — 글은 두고 따로 알린다(CM35)
                if (!empty && sync is BoardSyncState.Failed) {
                    item(key = "refresh-failed") {
                        StoryToast(
                            stringResource(R.string.story_blue_refresh_failed), Modifier.padding(top = 12.dp),
                            action = stringResource(R.string.story_retry), onAction = onRetry, error = true,
                        )
                    }
                }
                when {
                    loading -> item(key = "loading") {
                        val label = stringResource(R.string.story_loading)
                        Column(Modifier.semantics { contentDescription = label }.testTag("stories-loading")) {
                            repeat(3) { StorySkeletonRow(); StoryDivider() }
                        }
                    }
                    signIn -> item(key = "sign-in") {
                        StoryStateBlock(
                            Icons.Outlined.Lock, stringResource(R.string.story_sign_in_title),
                            stringResource(R.string.story_sign_in_body),
                        ) { SignInAgainButton() }
                    }
                    failed -> item(key = "error") {
                        StoryStateBlock(
                            Icons.Filled.Refresh, stringResource(R.string.story_error_title),
                            stringResource(R.string.story_error_body), Modifier.testTag("stories-error"),
                        )
                    }
                    empty -> item(key = "empty") {
                        StoryStateBlock(
                            Icons.Outlined.ChatBubbleOutline,
                            stringResource(if (filter != null) R.string.story_place_no_posts else R.string.story_blue_empty_title),
                            stringResource(R.string.story_blue_empty_body), Modifier.testTag("stories-empty"),
                        ) {
                            if (filter == null && nextRange(range) != null) {
                                StoryTextButton(stringResource(R.string.story_widen), onWiden, Modifier.testTag("stories-widen"), color = t.cyan)
                            }
                        }
                    }
                    else -> items(list.nearby, key = { it.post.id }) { item ->
                        StoryRow(
                            post = item.post, place = item.place, meters = item.meters,
                            onOpen = { onOpenPost(item.post.id) },
                            onOpenPlace = item.place?.let { place -> { onOpenMap(place) } },
                            onLike = { onLike(item.post.id) },
                        )
                        StoryDivider()
                    }
                }
                // 장소가 없는 예전 글 — 지도 · 거리에 들어가지 않는다. 지우지 않고 목록 끝에 둔다
                if (list.legacy.isNotEmpty() && sync == BoardSyncState.Ready) {
                    item(key = "legacy-header") {
                        Text(stringResource(R.string.story_legacy_header), style = runTextStyle(14.sp, t.label, FontWeight.SemiBold),
                            modifier = Modifier.padding(top = 22.dp, bottom = 2.dp))
                    }
                    items(list.legacy, key = { "legacy-${it.post.id}" }) { item ->
                        StoryRow(post = item.post, place = null, meters = null,
                            onOpen = { onOpenPost(item.post.id) }, onOpenPlace = null,
                            onLike = { onLike(item.post.id) })
                        StoryDivider()
                    }
                }
            }
            Box(Modifier.align(Alignment.BottomCenter).padding(horizontal = StoryListGutter, vertical = 8.dp)) { notice() }
        }
        when {
            signIn -> Unit
            failed -> StoryBottomAction {
                StoryButton(stringResource(R.string.story_card_reload), onRetry, Modifier.testTag("stories-retry"), italic = true)
            }
            empty && !loading -> StoryBottomAction {
                StoryButton(
                    stringResource(R.string.story_blue_first_post), onWrite, Modifier.testTag("stories-first-post"),
                    icon = Icons.Filled.Add, italic = true,
                )
            }
            else -> StoryBottomAction {
                StoryButton(
                    stringResource(R.string.story_write), onWrite,
                    Modifier.testTag("stories-write").guideTarget(GuideTour.Targets.COMMUNITY_WRITE),
                    icon = Icons.Filled.Add, italic = true,
                )
            }
        }
    }
}

/**
 * 기준점을 아직 모를 때(CM30 · CM31 · CM32 · CM37) — 같은 목록 틀에 "내 주변 / 이야기", 위치 확인 칸.
 * 권한을 묻기 전에는 아래 안내 시트(내 위치 사용하기 · 지역 직접 선택), 거절됐으면 설정 열기. 위치를 찾는 중 · 찾지 못함은
 * 가운데 안내와 아래 "지역 직접 선택". 임의의 도시 · 거리 · 현재 위치 점을 만들지 않는다.
 */
@Composable
private fun StoryLocationBody(
    locating: Boolean,
    denied: Boolean,
    askOpen: Boolean,
    onCloseAsk: () -> Unit,
    onUseLocation: () -> Unit,
    onChooseRegion: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val t = runTone()
    // 위치를 오래 못 찾으면(위치 기능 꺼짐 · 실내) 찾는 중에 머물지 않고 지역 선택을 권한다
    var attempt by rememberSaveable { mutableIntStateOf(0) }
    var waitedTooLong by remember { mutableStateOf(false) }
    LaunchedEffect(locating, attempt) {
        waitedTooLong = false
        if (locating) {
            delay(10_000)
            waitedTooLong = true
        }
    }
    val searching = locating && !waitedTooLong
    Box(Modifier.fillMaxSize().testTag("stories-location-prompt")) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = StoryListGutter)) {
                StoryHead(
                    title = stringResource(R.string.story_blue_area_unknown_title),
                    details = {
                        Text(stringResource(R.string.story_blue_need_area), style = runTextStyle(16.sp, t.label, FontWeight.Medium))
                    },
                    side = {
                        StoryHeadCard {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (searching) RunSpinner(Modifier.size(40.dp))
                                else Icon(Icons.Outlined.LocationOn, null, tint = t.cobaltText, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    stringResource(if (searching) R.string.story_blue_locating_short else R.string.story_blue_need_location),
                                    style = runTextStyle(14.sp, t.text, FontWeight.SemiBold),
                                )
                            }
                        }
                    },
                )
                StorySectionTitle(null, 0) {}
                if (!askOpen) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        when {
                            searching -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                RunSpinner(Modifier.size(52.dp))
                                Spacer(Modifier.height(18.dp))
                                Text(stringResource(R.string.story_blue_locating_title), style = runTextStyle(21.sp, t.text, FontWeight.ExtraBold),
                                    textAlign = TextAlign.Center)
                                Spacer(Modifier.height(6.dp))
                                Text(stringResource(R.string.story_blue_locating_body), style = runTextStyle(16.sp, t.label, FontWeight.Medium),
                                    textAlign = TextAlign.Center)
                            }
                            locating -> StoryStateBlock(
                                Icons.Outlined.WrongLocation, stringResource(R.string.story_blue_location_failed_title),
                                stringResource(R.string.story_blue_location_failed_body),
                            )
                            else -> StoryStateBlock(
                                Icons.Outlined.LocationOn, stringResource(R.string.story_blue_ask_closed_title),
                                stringResource(R.string.story_blue_ask_closed_body),
                            )
                        }
                    }
                }
            }
            if (!askOpen) {
                StoryBottomAction {
                    StoryButton(stringResource(R.string.story_location_region), onChooseRegion,
                        Modifier.testTag("stories-choose-region"), italic = true)
                    when {
                        locating && !searching -> StoryTextButton(stringResource(R.string.story_blue_locate_again), { attempt++ },
                            Modifier.testTag("stories-locate-again"), color = t.cyan)
                        !locating -> StoryTextButton(
                            stringResource(if (denied) R.string.cd_open_settings else R.string.story_location_use),
                            if (denied) onOpenSettings else onUseLocation,
                            Modifier.testTag("stories-use-location"), color = t.cyan,
                        )
                    }
                }
            }
        }
        if (askOpen) {
            // 처음 묻는 안내 · 권한 꺼짐 — 아래 시트. 닫아도 동의를 강요하지 않는다(가운데 안내와 지역 선택으로)
            Box(Modifier.fillMaxSize().background(t.scrim))
            RunSheetPanel(onClose = onCloseAsk, modifier = Modifier.align(Alignment.BottomCenter), closeTag = "stories-ask-close") {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(if (denied) R.string.story_blue_denied_title else R.string.story_blue_ask_title),
                        style = TextStyle(
                            fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 1.3.em,
                            letterSpacing = (-0.02).em, color = t.text,
                        ),
                        modifier = Modifier.padding(end = 40.dp).semantics { heading() },
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(if (denied) R.string.story_blue_denied_body else R.string.story_blue_ask_body),
                        style = runTextStyle(16.sp, t.text, FontWeight.Medium, 1.55f),
                    )
                    Spacer(Modifier.height(22.dp))
                    if (denied) {
                        StoryButton(stringResource(R.string.cd_open_settings), onOpenSettings, Modifier.testTag("stories-open-settings"), italic = true)
                    } else {
                        StoryButton(stringResource(R.string.story_location_use), onUseLocation, Modifier.testTag("stories-use-location"), italic = true)
                    }
                    Spacer(Modifier.height(10.dp))
                    StoryButton(stringResource(R.string.story_location_region), onChooseRegion,
                        Modifier.testTag("stories-choose-region"), style = StoryButtonStyle.SECONDARY)
                }
            }
        }
    }
}

@Composable
fun rangeLabel(range: StoryRange): String = stringResource(
    when (range) {
        StoryRange.M500 -> R.string.story_range_500
        StoryRange.KM1 -> R.string.story_range_1k
        StoryRange.KM3 -> R.string.story_range_3k
    },
)

/**
 * 범위 변경(CM02) — 고르는 동안은 임시 선택, "이 범위로 보기"를 눌러야 바뀐다. 닫으면 원래 범위.
 * 위에 지금 기준 지역과 "지역 변경 >"(지역 검색).
 */
@Composable
fun StoryRangeSheet(
    current: StoryRange,
    origin: StoryOrigin?,
    onApply: (StoryRange) -> Unit,
    onChangeRegion: () -> Unit,
    onDismiss: () -> Unit,
) {
    val t = runTone()
    var picked by rememberSaveable { mutableStateOf(current) }
    StorySheet(title = stringResource(R.string.story_blue_range_title), onDismiss = onDismiss) {
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.story_blue_current_area), style = runTextStyle(14.sp, t.label, FontWeight.Medium))
                Text(
                    origin?.label?.takeIf { it.isNotBlank() } ?: stringResource(R.string.story_area_unknown),
                    style = runTextStyle(24.sp, t.text, FontWeight.ExtraBold), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            StoryLink(stringResource(R.string.story_blue_change_area), onChangeRegion, Modifier.testTag("story-range-region"))
        }
        Spacer(Modifier.height(10.dp))
        StoryDivider()
        StoryRange.entries.forEach { option ->
            StoryRadioRow(
                label = rangeLabel(option),
                hint = stringResource(
                    when {
                        option == current -> R.string.story_blue_range_current
                        option == StoryRange.M500 -> R.string.story_blue_range_500_hint
                        option == StoryRange.KM1 -> R.string.story_blue_range_1k_hint
                        else -> R.string.story_blue_range_3k_hint
                    },
                ),
                selected = picked == option,
                onClick = { picked = option },
                modifier = Modifier.testTag("story-range-${option.name}"),
                large = true,
            )
            if (option != StoryRange.entries.last()) StoryDivider()
        }
        Spacer(Modifier.height(16.dp))
        StoryButton(stringResource(R.string.story_range_apply), { onApply(picked) }, Modifier.testTag("story-range-apply"))
    }
}

/** 한 가지만 고르는 줄 — 범위 · 신고 사유. 고르면 파란 원과 흰 체크 */
@Composable
fun StoryRadioRow(
    label: String,
    hint: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    leading: Boolean = false,
) {
    val t = runTone()
    val mark = @Composable {
        Box(
            Modifier.size(28.dp).clip(CircleShape)
                .then(if (selected) Modifier.background(t.cobalt) else Modifier.border(2.dp, t.label.copy(alpha = 0.8f), CircleShape)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(Icons.Filled.Check, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(18.dp))
        }
    }
    Row(
        modifier.fillMaxWidth().heightIn(min = if (large) 72.dp else 60.dp)
            .feedbackClickable(role = Role.RadioButton, cue = FeedbackCue.Select, onClick = onClick)
            .semantics { this.selected = selected },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading) {
            mark()
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(label, style = runTextStyle(if (large) 22.sp else 17.sp, t.text, if (large) FontWeight.ExtraBold else FontWeight.SemiBold))
            if (hint != null) Text(hint, style = runTextStyle(15.sp, t.label, FontWeight.Medium))
        }
        if (!leading) mark()
    }
}

/**
 * 위치를 모를 때의 전체 화면 안내 — 목록 위 동네 이름을 눌러 온 "내 주변" 화면이 쓴다.
 * 내 위치 사용(실제 권한 요청) · 지역 직접 선택(권한 없이). 거절된 뒤에는 설정 열기.
 */
@Composable
fun StoryLocationPrompt(
    locating: Boolean,
    denied: Boolean,
    onUseLocation: () -> Unit,
    onChooseRegion: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = runTone()
    Column(modifier.padding(horizontal = StoryFormGutter).testTag("stories-location-prompt")) {
        Spacer(Modifier.weight(0.5f))
        Text(
            stringResource(if (denied) R.string.story_blue_denied_title else R.string.story_blue_ask_title),
            style = storyTitleStyle(), modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(
                when {
                    locating -> R.string.story_location_locating
                    denied -> R.string.story_blue_denied_body
                    else -> R.string.story_blue_ask_body
                },
            ),
            style = runTextStyle(16.sp, t.label, FontWeight.Medium, 1.55f),
        )
        Spacer(Modifier.weight(1f))
        if (!locating) {
            if (denied) {
                StoryButton(stringResource(R.string.cd_open_settings), onOpenSettings, Modifier.testTag("stories-open-settings"), italic = true)
            } else {
                StoryButton(stringResource(R.string.story_location_use), onUseLocation, Modifier.testTag("stories-use-location"), italic = true)
            }
            Spacer(Modifier.height(10.dp))
        }
        StoryButton(stringResource(R.string.story_location_region), onChooseRegion,
            Modifier.testTag("stories-choose-region"), style = StoryButtonStyle.SECONDARY)
        Spacer(Modifier.height(14.dp))
    }
}

/** 확인 메시지 한 줄 — 몇 초 뒤 저절로 사라진다(숨김은 되돌리기를 누를 시간을 더 준다) */
@Composable
private fun StoryNoticeToast(
    notice: StoryNotice,
    onDismiss: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(notice) {
        delay(if (notice is StoryNotice.Hidden) 6_000 else 3_500)
        onDismiss()
    }
    val text = stringResource(
        when (notice) {
            is StoryNotice.Posted -> R.string.story_notice_posted
            is StoryNotice.Edited -> R.string.story_notice_edited
            is StoryNotice.Deleted -> R.string.story_notice_deleted
            is StoryNotice.Hidden -> R.string.story_notice_hidden
            is StoryNotice.DraftSaved -> R.string.story_notice_draft_saved
        },
    )
    StoryToast(
        text = text,
        modifier = modifier,
        action = stringResource(if (notice is StoryNotice.Hidden) R.string.story_undo else R.string.common_close),
        onAction = if (notice is StoryNotice.Hidden) onUndo else onDismiss,
    )
}
