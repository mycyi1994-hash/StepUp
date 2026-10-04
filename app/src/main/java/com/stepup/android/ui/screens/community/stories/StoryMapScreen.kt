package com.stepup.android.ui.screens.community.stories

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.domain.StoryPlace
import com.stepup.android.ui.StepPermissions
import com.stepup.android.ui.components.MapControlLabels
import com.stepup.android.ui.components.rememberCurrentLocation
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone

/** 목록 → 지도로 넘어갈 때 먼저 고를 장소(썸네일 · 상세의 장소 카드를 눌렀을 때). 한 번 읽으면 비운다 */
object StoryMapSeed {
    @Volatile var place: StoryPlace? = null

    fun take(): StoryPlace? = place.also { place = null }
}

/**
 * 주변 지도(CM05 · CM06) — 위는 실제 지도(390dp 폭 기준 높이 196dp 이하, 작은 화면은 지도부터 줄인다), 아래는 목록과 같은
 * 글 행. 장소 핀(또는 글의 장소 썸네일)을 누르면 아래 목록이 그 장소의 글로 바로 걸러지고 "전체 장소"가 그 필터만 푼다 —
 * "이 장소 글 보기" 같은 다음 단계를 두지 않는다. 뒤로 가기는 들어온 화면(목록 또는 상세)으로 간다.
 */
@Composable
fun StoryMapScreen(
    viewModel: StoriesViewModel,
    onBack: () -> Unit,
    onOpenPost: (Long) -> Unit,
    /** 예전 "이 장소 글 보기"의 목록 복귀 — 지금은 지도 안에서 거르므로 쓰지 않는다(경로 호환으로 남김) */
    @Suppress("UNUSED_PARAMETER") onPlaceFeed: () -> Unit = onBack,
) {
    val t = runTone()
    val context = LocalContext.current
    val allowed = StepPermissions.hasLocation(context)
    val here = rememberCurrentLocation(enabled = allowed)
    LaunchedEffect(here, allowed) { viewModel.onLocation(here, allowed) }
    val origin by viewModel.origin.collectAsStateWithLifecycle()
    val area by viewModel.area.collectAsStateWithLifecycle()
    val range by viewModel.range.collectAsStateWithLifecycle()
    var selected by rememberSaveable(stateSaver = StoryPlaceSaver) { mutableStateOf<StoryPlace?>(StoryMapSeed.take()) }
    val chosen = selected
    // 고른 장소의 글 — 범위 밖 장소(상세에서 연 먼 장소)여도 그 장소의 글을 보인다. 목록이 바뀌면(좋아요 · 숨김) 다시 센다
    val placeItems = remember(chosen, area) { chosen?.let(viewModel::postsAt).orEmpty() }
    val items = if (chosen != null) placeItems else area.nearby
    val shownPins = if (chosen != null && area.pins.none { it.place.key == chosen.key }) {
        area.pins + StoryPin(chosen, placeItems.size, null)
    } else {
        area.pins
    }
    val listState = rememberLazyListState()
    LaunchedEffect(chosen?.key) { listState.scrollToItem(0) }
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val mapHeight = min(196.dp, screenHeight * 0.26f)

    Box(Modifier.fillMaxSize().testTag("story-map")) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            StoryHeader(stringResource(R.string.story_blue_map_title), onBack = onBack)
            Box(Modifier.fillMaxWidth().height(mapHeight).background(t.inset)) {
                StoryMapTone {
                    StoryPinsMap(
                        pins = shownPins,
                        origin = origin?.point,
                        originLabel = stringResource(if (origin?.manual == true) R.string.story_origin_region else R.string.story_origin_me),
                        modifier = Modifier.fillMaxSize().testTag("story-map-canvas"),
                        selectedKey = chosen?.key,
                        interactive = true,
                        rangeMeters = range.meters,
                        dotSeparator = true,
                        onPin = { selected = it.place },
                        controlLabels = MapControlLabels(
                            zoomIn = stringResource(R.string.rec_zoom_in),
                            zoomOut = stringResource(R.string.rec_zoom_out),
                            recenter = stringResource(R.string.rec_map_recenter),
                        ),
                    )
                }
                StoryDivider(Modifier.align(Alignment.BottomCenter))
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().testTag("story-map-list"),
                contentPadding = PaddingValues(start = StoryListGutter, end = StoryListGutter, top = 18.dp, bottom = 24.dp),
            ) {
                item(key = "title") {
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (chosen != null) {
                            Text(
                                stringResource(R.string.story_blue_place_posts, chosen.name, placeItems.size),
                                style = runTextStyle(23.sp, t.text, FontWeight.ExtraBold), maxLines = 2, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).semantics { heading() }.testTag("story-map-selected"),
                            )
                            StoryLink(stringResource(R.string.story_all_places), { selected = null }, Modifier.testTag("story-map-all"),
                                trailing = null)
                        } else {
                            Text(
                                stringResource(R.string.story_blue_nearby_title), style = runTextStyle(23.sp, t.text, FontWeight.ExtraBold),
                                modifier = Modifier.weight(1f).semantics { heading() },
                            )
                            Text(stringResource(R.string.story_blue_latest), style = runTextStyle(14.sp, t.label, FontWeight.Medium))
                        }
                    }
                    val sub = if (chosen != null) {
                        distanceFrom(origin, chosen.point)
                    } else {
                        val from = origin
                        if (from == null) {
                            ""
                        } else {
                            listOf(
                                if (from.label.isBlank()) stringResource(R.string.story_area_unknown)
                                else stringResource(R.string.story_area_around, from.label),
                                stringResource(
                                    if (from.manual) R.string.story_range_from_region else R.string.story_range_from_me,
                                    rangeLabel(range),
                                ),
                            ).joinToString(" · ")
                        }
                    }
                    if (sub.isNotEmpty()) {
                        Text(sub, style = runTextStyle(16.sp, t.label, FontWeight.Medium), modifier = Modifier.testTag("story-map-basis"))
                    }
                    Spacer(Modifier.height(6.dp))
                }
                if (items.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            stringResource(if (chosen != null) R.string.story_place_no_posts else R.string.story_map_empty),
                            style = runTextStyle(16.sp, t.label, FontWeight.Medium),
                            modifier = Modifier.padding(vertical = 24.dp).testTag("story-map-empty"),
                        )
                    }
                }
                items(items, key = { it.post.id }) { item ->
                    StoryRow(
                        post = item.post, place = item.place, meters = item.meters,
                        onOpen = { onOpenPost(item.post.id) },
                        onOpenPlace = item.place?.let { place -> { selected = place } },
                        onLike = { viewModel.toggleLike(item.post.id) },
                    )
                    StoryDivider()
                }
            }
        }
    }
}
