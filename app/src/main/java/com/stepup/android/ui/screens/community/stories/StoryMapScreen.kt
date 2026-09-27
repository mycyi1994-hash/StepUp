package com.stepup.android.ui.screens.community.stories

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.domain.StoryPlace
import com.stepup.android.ui.StepPermissions
import com.stepup.android.ui.components.rememberCurrentLocation
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.community.relativeTime
import com.stepup.android.ui.theme.Carbon
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.Night
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow

/** 목록 → 지도로 넘어갈 때 먼저 고를 장소(썸네일을 눌렀을 때). 한 번 읽으면 비운다 */
object StoryMapSeed {
    @Volatile var place: StoryPlace? = null

    fun take(): StoryPlace? = place.also { place = null }
}

/**
 * 내 주변 지도 — 범위 안의 장소와 장소마다 글 수. 장소를 누르면 이름 · 주소 · 거리 · 최근 글,
 * "이 장소 글 보기"로 목록이 그 장소의 글만 보인다. 목록과 같은 범위 · 숨김 기준을 쓴다.
 */
@Composable
fun StoryMapScreen(
    viewModel: StoriesViewModel,
    onBack: () -> Unit,
    onOpenPost: (Long) -> Unit,
    /** "이 장소 글 보기" — 상세에서 지도로 왔어도 목록까지 돌아간다 */
    onPlaceFeed: () -> Unit = onBack,
) {
    val context = LocalContext.current
    val allowed = StepPermissions.hasLocation(context)
    val here = rememberCurrentLocation(enabled = allowed)
    LaunchedEffect(here, allowed) { viewModel.onLocation(here, allowed) }
    val origin by viewModel.origin.collectAsStateWithLifecycle()
    val pins by viewModel.areaPins.collectAsStateWithLifecycle()
    val range by viewModel.range.collectAsStateWithLifecycle()
    var selected by rememberSaveable(stateSaver = StoryPlaceSaver) { mutableStateOf<StoryPlace?>(StoryMapSeed.take()) }
    BackHandler(enabled = selected != null) { selected = null }

    Box(Modifier.fillMaxSize().background(Night).testTag("story-map")) {
        val chosen = selected
        val shownPins = if (chosen != null && pins.none { it.place.key == chosen.key }) {
            pins + StoryPin(chosen, viewModel.postsAt(chosen).size, null)
        } else {
            pins
        }
        StoryPinsMap(
            pins = shownPins,
            origin = origin?.point,
            originLabel = stringResource(if (origin?.manual == true) R.string.story_origin_region else R.string.story_origin_me),
            modifier = Modifier.fillMaxSize(),
            selectedKey = chosen?.key,
            interactive = true,
            rangeMeters = range.meters,
            dotSeparator = true,
            onPin = { selected = it.place },
        )
        Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(0f to Night, 1f to Night.copy(alpha = 0f)))) {
            StoryHeader(stringResource(R.string.story_map_title), onBack = { if (selected != null) selected = null else onBack() })
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().heightIn(max = 380.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(Carbon)
                .padding(start = StoryFormGutter, end = StoryFormGutter, top = 10.dp, bottom = 16.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(Edge))
            }
            Spacer(Modifier.height(14.dp))
            if (chosen == null) {
                val area = origin?.label.orEmpty()
                Text(
                    if (area.isBlank()) stringResource(R.string.story_area_unknown) else stringResource(R.string.story_area_around, area),
                    color = Snow, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.story_map_hint), color = Silver, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (pins.isEmpty()) {
                        Text(stringResource(R.string.story_map_empty), color = Slate, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
                    }
                    pins.forEach { pin ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 50.dp)
                                .feedbackClickable(role = Role.Button, onClick = { selected = pin.place })
                                .testTag("story-map-place"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(pin.place.name, color = Snow, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.story_post_count, pin.count), color = Slate, fontSize = 12.sp)
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Filled.CallMade, contentDescription = null, tint = Silver, modifier = Modifier.size(14.dp))
                        }
                        StoryDivider()
                    }
                }
            } else {
                Text(chosen.name, color = Snow, fontSize = 23.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("story-map-selected"))
                if (chosen.address.isNotBlank()) Text(chosen.address, color = Slate, fontSize = 12.sp)
                val away = distanceFrom(origin, chosen.point)
                if (away.isNotEmpty()) Text(away, color = Slate, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                StoryDivider()
                val latest = viewModel.postsAt(chosen).firstOrNull()
                if (latest == null) {
                    Text(stringResource(R.string.story_place_no_posts), color = Silver, fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 14.dp))
                } else {
                    Column(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp)
                            .feedbackClickable(role = Role.Button, onClick = { onOpenPost(latest.post.id) })
                            .padding(vertical = 10.dp),
                    ) {
                        Text(latest.post.title, color = Snow, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${storyAuthor(latest.post)} · ${relativeTime(latest.post.createdAt)}", color = Slate, fontSize = 11.sp)
                    }
                    StoryDivider()
                }
                Spacer(Modifier.height(14.dp))
                StoryButton(
                    stringResource(R.string.story_map_place_posts),
                    {
                        viewModel.filterPlace(chosen)
                        viewModel.setExpanded(false)
                        onPlaceFeed()
                    },
                    Modifier.testTag("story-map-place-posts"),
                )
            }
        }
    }
}
