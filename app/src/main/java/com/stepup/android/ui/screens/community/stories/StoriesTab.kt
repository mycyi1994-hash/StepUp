package com.stepup.android.ui.screens.community.stories

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.stepup.android.ui.components.SignInAgainButton
import com.stepup.android.ui.components.rememberCurrentLocation
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.guide.GuideTour
import com.stepup.android.ui.guide.guideTarget
import com.stepup.android.ui.theme.Carbon
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.Night
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.StepUpDesign
import com.stepup.android.ui.theme.VoltText
import kotlinx.coroutines.delay

/**
 * 동네 이야기 — 커뮤니티 첫 화면(2026-09-27 사용자 결정 · 목록형).
 *
 * 위에는 지도(기준점 · 장소 핀), 아래에는 다른 사람의 글 목록. 행의 왼쪽 썸네일은 글쓴이가 고른
 * 공개 장소이고, 거리는 보는 사람의 위치(또는 고른 지역)에서 잰다. 글쓰기는 오른쪽 아래 흰 버튼.
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
    val expanded by viewModel.expanded.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()

    // 방금 올린 글 — 필터를 풀고 필요한 만큼 범위를 넓혀 목록 맨 위에 보이게 한다
    val revealId = (notice as? StoryNotice.Posted)?.postId
    LaunchedEffect(revealId) { revealId?.let(viewModel::reveal) }

    var showRange by rememberSaveable { mutableStateOf(false) }
    var showResume by rememberSaveable { mutableStateOf(false) }
    // 새 글은 지금 보는 장소(또는 300m 안의 글 있는 장소)를 먼저 넣어 둔다 — 없으면 비워 두고 고르게 한다
    val writeNew = {
        StoryComposeSeed.place = viewModel.suggestedPlace()
        onWrite(false)
    }
    val startWriting = { if (draft != null) showResume = true else writeNew() }

    // 권한 요청 — 거절 · 다시 묻지 않음이어도 "지역 직접 선택"으로 이어 갈 수 있다
    var denied by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        allowed = StepPermissions.hasLocation(context)
        denied = !allowed
    }
    val askLocation = {
        permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    Box(Modifier.fillMaxSize().testTag("stories-tab")) {
        if (status != StoryLocationStatus.READY || origin == null) {
            StoryLocationPrompt(
                locating = status == StoryLocationStatus.LOCATING,
                denied = denied,
                onUseLocation = askLocation,
                onChooseRegion = onOpenRegion,
                onOpenSettings = { ExternalIntents.openAppSettings(context) },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val from = origin!!
            StoryListBody(
                origin = from,
                list = list,
                sync = sync,
                range = range,
                filter = filter,
                expanded = expanded,
                onExpand = viewModel::setExpanded,
                onOpenPost = onOpenPost,
                onOpenMap = onOpenMap,
                onPin = { pin -> viewModel.filterPlace(pin.place) },
                onClearFilter = { viewModel.filterPlace(null) },
                onOpenRange = { showRange = true },
                onOpenLocation = onOpenLocation,
                onRetry = viewModel::refresh,
                onWrite = startWriting,
                onWiden = { nextRange(range)?.let(viewModel::setRange) },
                revealId = revealId,
                onLike = viewModel::toggleLike,
            )
            // 글쓰기 — 오른쪽 아래 흰 알약. 알림과 겹치지 않게 알림이 그 위에 선다
            StoryWriteButton(
                onClick = startWriting,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = StoryListGutter, bottom = 14.dp)
                    .guideTarget(GuideTour.Targets.COMMUNITY_WRITE),
            )
        }
        notice?.let { current ->
            StoryNoticeToast(
                notice = current,
                onDismiss = { viewModel.consumeNotice(current) },
                onUndo = { (current as? StoryNotice.Hidden)?.let(viewModel::undoHide) },
                modifier = Modifier.align(Alignment.BottomCenter)
                    .padding(start = StoryListGutter, end = StoryListGutter, bottom = 70.dp),
            )
        }
    }

    if (showRange) {
        StoryRangeSheet(
            current = range,
            manual = origin?.manual == true,
            onApply = { viewModel.setRange(it); showRange = false },
            onDismiss = { showRange = false },
        )
    }
    val pendingDraft = draft
    if (showResume && pendingDraft != null) {
        StorySheet(title = stringResource(R.string.story_resume_title), onDismiss = { showResume = false }) {
            Text(pendingDraft.preview, color = Silver, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
            Text(stringResource(R.string.story_resume_new_note), color = Slate, fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 18.dp))
            StoryButton(stringResource(R.string.story_resume_continue), {
                showResume = false
                onWrite(true)
            }, Modifier.testTag("story-resume-continue"))
            Spacer(Modifier.height(10.dp))
            StoryButton(stringResource(R.string.story_resume_new), {
                showResume = false
                viewModel.discardDraft()
                writeNew()
            }, Modifier.testTag("story-resume-new"), style = StoryButtonStyle.SECONDARY)
        }
    }
}

private fun nextRange(range: StoryRange): StoryRange? = when (range) {
    StoryRange.M500 -> StoryRange.KM1
    StoryRange.KM1 -> StoryRange.KM3
    StoryRange.KM3 -> null
}

@Composable
private fun StoryListBody(
    origin: StoryOrigin,
    list: StoryListState,
    sync: BoardSyncState,
    range: StoryRange,
    filter: StoryPlace?,
    expanded: Boolean,
    onExpand: (Boolean) -> Unit,
    onOpenPost: (Long) -> Unit,
    onOpenMap: (StoryPlace?) -> Unit,
    onPin: (StoryPin) -> Unit,
    onClearFilter: () -> Unit,
    onOpenRange: () -> Unit,
    onOpenLocation: () -> Unit,
    onRetry: () -> Unit,
    onWrite: () -> Unit,
    onWiden: () -> Unit,
    revealId: Long?,
    onLike: (Long) -> Unit,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val largeText = LocalDensity.current.fontScale > 1.3f
    // 지도 높이 — 시안 246/844 를 공통 머리 · 탭 아래로 옮겼다. 작은 화면 · 큰 글씨는 줄인다
    val mapHeight = when {
        expanded -> 0.dp
        screenHeight < 700 || largeText -> 150.dp
        else -> 190.dp
    }
    val animatedMap by animateDpAsState(mapHeight, tween(260), label = "storyMap")
    val listState = rememberLazyListState()
    // 방금 올린 글이 맨 위에 들어온 뒤에 올린다 — 먼저 올리면 새 줄이 화면 위로 밀려 가려진다
    val firstId = list.nearby.firstOrNull()?.post?.id
    LaunchedEffect(revealId, firstId) {
        if (revealId != null && firstId == revealId) listState.animateScrollToItem(0)
    }

    Box(Modifier.fillMaxSize()) {
        if (animatedMap > 0.dp) {
            Box(Modifier.fillMaxWidth().height(animatedMap + 24.dp)) {
                StoryPinsMap(
                    pins = list.pins,
                    origin = origin.point,
                    originLabel = stringResource(if (origin.manual) R.string.story_origin_region else R.string.story_origin_me),
                    modifier = Modifier.fillMaxSize().testTag("stories-map"),
                    selectedKey = filter?.key,
                    rangeMeters = range.meters,
                    onPin = onPin,
                )
                // 위아래를 바닥색으로 눌러 글자가 읽히게
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                    0f to Night.copy(alpha = 0.55f), 0.35f to Night.copy(alpha = 0f), 1f to Night.copy(alpha = 0.2f),
                )))
                StoryAreaLabel(origin, onOpenLocation, Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 6.dp))
                Row(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth()
                        .padding(start = 10.dp, end = 12.dp, bottom = 38.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StoryLink(
                        stringResource(
                            if (origin.manual) R.string.story_range_from_region else R.string.story_range_from_me,
                            rangeLabel(range),
                        ),
                        onOpenRange,
                        Modifier.testTag("stories-range"),
                        color = Snow.copy(alpha = 0.85f),
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.size(StepUpDesign.TouchTarget).clip(CircleShape)
                            .feedbackClickable(role = Role.Button, onClick = { onOpenMap(filter) })
                            .testTag("stories-open-map"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(Carbon.copy(alpha = 0.9f)).border(1.dp, Edge, CircleShape),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.CallMade, contentDescription = stringResource(R.string.story_open_map),
                                tint = Snow, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }
        // 목록 시트 — 지도 위로 조금 올라와 위 모서리가 둥글다
        Column(
            Modifier.fillMaxSize().padding(top = animatedMap)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Night),
        ) {
            if (expanded) StoryAreaLabel(origin, onOpenLocation, Modifier.padding(start = 14.dp, top = 4.dp))
            Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(width = 64.dp, height = 20.dp).clip(RoundedCornerShape(10.dp))
                        .feedbackClickable(role = Role.Button, onClick = { onExpand(!expanded) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(Edge))
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(start = StoryListGutter, end = StoryListGutter - 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (filter != null) {
                    Text(filter.name, color = Snow, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false).testTag("stories-title"))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.story_post_count, list.nearby.size), color = Slate, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    StoryTextButton(stringResource(R.string.story_all_places), onClearFilter,
                        Modifier.testTag("stories-clear-filter"))
                } else {
                    Text(stringResource(R.string.story_nearby_title), color = Snow, fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).testTag("stories-title"))
                    StoryLink(
                        stringResource(if (expanded) R.string.story_show_map else R.string.story_show_more),
                        { onExpand(!expanded) },
                        Modifier.testTag("stories-expand"),
                    )
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f).testTag("stories-list"),
                contentPadding = PaddingValues(start = StoryListGutter, end = StoryListGutter, bottom = 88.dp),
            ) {
                val empty = list.nearby.isEmpty()
                when {
                    empty && sync == BoardSyncState.Loading || empty && sync == BoardSyncState.Idle -> {
                        items(3) { StorySkeletonRow(); StoryDivider() }
                        item {
                            Text(stringResource(R.string.story_loading), color = Slate, fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).testTag("stories-loading"),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                    empty && sync == BoardSyncState.SignInRequired -> item {
                        StoryStateBlock(Icons.Outlined.Lock, stringResource(R.string.story_sign_in_title),
                            stringResource(R.string.story_sign_in_body)) { SignInAgainButton() }
                    }
                    empty && sync is BoardSyncState.Failed -> item {
                        StoryStateBlock(Icons.Filled.Refresh, stringResource(R.string.story_error_title),
                            stringResource(R.string.story_error_body), Modifier.testTag("stories-error")) {
                            StoryButton(stringResource(R.string.story_retry), onRetry, Modifier.testTag("stories-retry"))
                        }
                    }
                    empty -> item {
                        StoryStateBlock(Icons.Outlined.ChatBubbleOutline,
                            stringResource(if (filter != null) R.string.story_place_no_posts else R.string.story_empty_title),
                            stringResource(R.string.story_empty_body), Modifier.testTag("stories-empty")) {
                            StoryButton(stringResource(R.string.story_first_post), onWrite, icon = Icons.Filled.Add)
                            if (filter == null && nextRange(range) != null) {
                                Spacer(Modifier.height(6.dp))
                                StoryTextButton(stringResource(R.string.story_widen), onWiden, Modifier.testTag("stories-widen"))
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
                        Text(stringResource(R.string.story_legacy_header), color = Slate, fontSize = 12.sp,
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
        }
    }
}

/** 지도 위 "📍 여의도동 주변" — 누르면 내 위치 · 지역 선택 */
@Composable
private fun StoryAreaLabel(origin: StoryOrigin, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.heightIn(min = StepUpDesign.TouchTarget).clip(RoundedCornerShape(10.dp))
            .feedbackClickable(role = Role.Button, onClick = onClick).padding(horizontal = 6.dp)
            .testTag("stories-area"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = VoltText, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            if (origin.label.isBlank()) stringResource(R.string.story_area_unknown)
            else stringResource(R.string.story_area_around, origin.label),
            color = Snow, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
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

/** 오른쪽 아래 흰 "+ 글쓰기" — 높이 44 */
@Composable
private fun StoryWriteButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    val dark = com.stepup.android.ui.theme.StepUpColors.dark
    val face = if (dark) androidx.compose.ui.graphics.Color(0xFFF3F5FF) else Snow
    val ink = if (dark) androidx.compose.ui.graphics.Color(0xFF070B12) else androidx.compose.ui.graphics.Color.White
    Row(
        modifier.heightIn(min = 48.dp).clip(shape).background(face, shape)
            .feedbackClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp)
            .testTag("stories-write"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = ink, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.story_write), color = ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 주변 범위 — 고르는 동안은 임시 선택, "이 범위로 보기"를 눌러야 바뀐다. 닫으면 원래 범위 */
@Composable
fun StoryRangeSheet(current: StoryRange, manual: Boolean, onApply: (StoryRange) -> Unit, onDismiss: () -> Unit) {
    var picked by rememberSaveable { mutableStateOf(current) }
    StorySheet(title = stringResource(R.string.story_range_title), onDismiss = onDismiss) {
        Text(stringResource(if (manual) R.string.story_range_body_region else R.string.story_range_body), color = Silver,
            fontSize = 13.sp, modifier = Modifier.padding(bottom = 10.dp))
        StoryRange.entries.forEach { option ->
            StoryRadioRow(
                label = stringResource(R.string.story_range_option, rangeLabel(option)),
                hint = if (option == StoryRange.KM1) stringResource(R.string.story_range_1k_hint) else null,
                selected = picked == option,
                onClick = { picked = option },
                modifier = Modifier.testTag("story-range-${option.name}"),
            )
            if (option != StoryRange.entries.last()) StoryDivider()
        }
        Spacer(Modifier.height(18.dp))
        StoryButton(stringResource(R.string.story_range_apply), { onApply(picked) }, Modifier.testTag("story-range-apply"))
    }
}

/** 한 가지만 고르는 줄 — 범위 · 신고 사유 */
@Composable
fun StoryRadioRow(label: String, hint: String?, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp)
            .feedbackClickable(role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Snow, fontSize = 15.sp)
            if (hint != null) Text(hint, color = Slate, fontSize = 11.sp)
        }
        Box(
            Modifier.size(22.dp).clip(CircleShape).border(1.5.dp, if (selected) VoltText else Slate, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(12.dp).clip(CircleShape).background(VoltText))
        }
    }
}

/** 위치를 모를 때 — 내 위치 사용(실제 권한 요청) · 지역 직접 선택(권한 없이) */
@Composable
fun StoryLocationPrompt(
    locating: Boolean,
    denied: Boolean,
    onUseLocation: () -> Unit,
    onChooseRegion: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 위치를 오래 못 찾으면(위치 기능 꺼짐 · 실내) 찾는 중에 머물지 않고 지역 선택을 권한다
    var waitedTooLong by remember { mutableStateOf(false) }
    LaunchedEffect(locating) {
        waitedTooLong = false
        if (locating) {
            delay(10_000)
            waitedTooLong = true
        }
    }
    Column(modifier.padding(horizontal = StoryFormGutter).testTag("stories-location-prompt")) {
        Spacer(Modifier.weight(0.6f))
        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = VoltText, modifier = Modifier.size(34.dp))
        Spacer(Modifier.height(18.dp))
        Text(stringResource(R.string.story_location_title), color = Snow, fontSize = 25.sp,
            fontWeight = FontWeight.SemiBold, lineHeight = 33.sp)
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(
                when {
                    locating && waitedTooLong -> R.string.story_location_not_found
                    locating -> R.string.story_location_locating
                    denied -> R.string.story_location_denied
                    else -> R.string.story_location_body
                },
            ),
            color = Silver, fontSize = 14.sp, lineHeight = 21.sp,
        )
        Spacer(Modifier.weight(1f))
        if (!locating) {
            StoryButton(stringResource(R.string.story_location_use), onUseLocation, Modifier.testTag("stories-use-location"))
            if (denied) {
                Spacer(Modifier.height(4.dp))
                StoryTextButton(stringResource(R.string.cd_open_settings), onOpenSettings, Modifier.align(Alignment.CenterHorizontally))
            }
        }
        StoryTextButton(stringResource(R.string.story_location_region), onChooseRegion,
            Modifier.align(Alignment.CenterHorizontally).testTag("stories-choose-region"))
        Spacer(Modifier.height(18.dp))
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
