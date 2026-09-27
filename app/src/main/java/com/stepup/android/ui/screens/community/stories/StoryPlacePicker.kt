package com.stepup.android.ui.screens.community.stories

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.repo.PlaceSearchResult
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.StoryOrigin
import com.stepup.android.domain.StoryPlace
import com.stepup.android.domain.haversineMeters
import com.stepup.android.domain.storyPlace
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.Carbon
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.Night
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.VoltText
import kotlinx.coroutines.delay

/** 장소를 저장 상태에 담는다(화면을 돌려도 고르던 장소가 남게) */
val StoryPlaceSaver: Saver<StoryPlace?, Any> = Saver(
    save = { place -> place?.let { arrayListOf<Any>(it.name, it.address, it.lat, it.lng) } ?: arrayListOf<Any>() },
    restore = { raw ->
        (raw as? List<*>)?.takeIf { it.size == 4 }?.let { StoryPlace(it[0] as String, it[1] as String, it[2] as Double, it[3] as Double) }
    },
)

/**
 * 다른 사람의 글에 붙은 장소 중 기준점 가까운 곳 — "가까운 장소". 지어낸 장소 목록이 아니라
 * 실제로 글이 올라온 공개 장소다. 기준점을 모르면 최근 글의 장소 순.
 */
@Composable
fun rememberNearbyPlaces(origin: StoryOrigin?, limit: Int = 8): List<Pair<StoryPlace, Double?>> {
    val posts by ServiceLocator.communityRepository.posts.collectAsState()
    return remember(posts, origin) {
        val places = posts.filter { !it.isFlash && it.crewId.isEmpty() }
            .sortedByDescending { it.createdAt }
            .mapNotNull { it.storyPlace }
            .distinctBy { it.key }
        val withDistance = places.map { it to origin?.let { o -> haversineMeters(o.point, it.point) } }
        (if (origin != null) withDistance.filter { (it.second ?: 0.0) <= 3_000 }.sortedBy { it.second } else withDistance).take(limit)
    }
}

/** 장소 선택 — 이름 검색 · 가까운 장소 · 지도에서 고르기 */
@Composable
fun StoryPlacePicker(
    origin: StoryOrigin?,
    onBack: () -> Unit,
    onPick: (StoryPlace) -> Unit,
    onPickOnMap: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<PlaceSearchResult?>(null) }
    var searching by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(query, attempt) {
        result = null
        if (query.isBlank()) return@LaunchedEffect
        searching = true
        delay(350) // 한 글자마다 묻지 않는다
        result = ServiceLocator.placeSearch.search(query, origin?.point)
        searching = false
    }
    val nearby = rememberNearbyPlaces(origin)

    Column(Modifier.fillMaxSize().imePadding().testTag("story-place-picker")) {
        StoryHeader(stringResource(R.string.story_place_picker_title), onBack)
        StorySearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(R.string.story_place_search_hint),
            modifier = Modifier.padding(horizontal = StoryFormGutter),
            tag = "story-place-search",
        )
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(start = StoryFormGutter, end = StoryFormGutter, top = 14.dp, bottom = 24.dp),
        ) {
            // 같은 열쇠가 두 번 오면 목록이 멈춘다 — 한 번만
            val found = (result as? PlaceSearchResult.Found)?.places?.distinctBy { it.key }
            when {
                query.isBlank() -> {
                    item {
                        Text(stringResource(R.string.story_place_nearby), color = Slate, fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 4.dp))
                    }
                    if (nearby.isEmpty()) {
                        item {
                            Text(stringResource(R.string.story_place_nearby_empty), color = Silver, fontSize = 13.sp,
                                lineHeight = 20.sp, modifier = Modifier.padding(vertical = 10.dp))
                        }
                    }
                    items(nearby, key = { it.first.key }) { (place, meters) ->
                        StoryPlaceRow(place, storyDistance(meters)) { onPick(place) }
                        StoryDivider()
                    }
                }
                searching || result == null -> item {
                    Text(stringResource(R.string.story_place_searching), color = Slate, fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 12.dp))
                }
                result is PlaceSearchResult.Offline -> item {
                    StoryStateBlock(Icons.Filled.Refresh, stringResource(R.string.story_place_offline_title),
                        stringResource(R.string.story_error_body)) {
                        StoryButton(stringResource(R.string.story_retry), { attempt++ })
                    }
                }
                found.isNullOrEmpty() -> item {
                    StoryStateBlock(Icons.Filled.Search, stringResource(R.string.story_place_empty_title),
                        stringResource(R.string.story_place_empty_body), Modifier.testTag("story-place-empty")) {
                        StoryTextButton(stringResource(R.string.story_place_clear), { query = "" }, color = VoltText)
                    }
                }
                else -> {
                    item {
                        Text(stringResource(R.string.story_place_result_count, found.size), color = Slate, fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 4.dp))
                    }
                    items(found, key = { it.key }) { place ->
                        val meters = origin?.let { haversineMeters(it.point, place.point) }
                        StoryPlaceRow(place, storyDistance(meters)) { onPick(place) }
                        StoryDivider()
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(top = 6.dp)
                        .feedbackClickable(role = Role.Button, onClick = onPickOnMap).testTag("story-place-on-map"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Silver, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.story_place_on_map), color = Silver, fontSize = 13.sp)
                }
            }
        }
    }
}

/** 검색 칸 — 돋보기 · 입력 · 지우기(×) */
@Composable
fun StorySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    tag: String = "story-search",
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = 50.dp).clip(shape).background(Carbon).border(1.dp, Edge, shape)
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = Silver, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text(placeholder, color = Slate, fontSize = 15.sp)
            BasicTextField(
                value = value,
                onValueChange = { onValueChange(it.take(60)) },
                singleLine = true,
                textStyle = TextStyle(color = Snow, fontSize = 15.sp),
                cursorBrush = SolidColor(VoltText),
                modifier = Modifier.fillMaxWidth().testTag(tag),
            )
        }
        if (value.isNotEmpty()) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).feedbackClickable(role = Role.Button, onClick = { onValueChange("") }),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(20.dp).clip(CircleShape).background(CarbonHigh), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.story_place_clear), tint = Silver,
                        modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

/** 장소 한 줄 — 핀 원 · 이름 · 주소 · 거리 */
@Composable
fun StoryPlaceRow(place: StoryPlace, distance: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).feedbackClickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp).testTag("story-place-row"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(CarbonHigh), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = VoltText, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(place.name, color = Snow, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (place.address.isNotBlank()) {
                Text(place.address, color = Slate, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (distance.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Text(distance, color = Silver, fontSize = 12.sp)
        }
    }
}

/**
 * 고른 장소 확인 — 지도에서 장소 이름 · 주소 · 거리를 보고 "이 장소로 선택".
 * [pickOnMap] 이면 지도를 눌러 자리를 고르고, 그 자리의 이름을 지도 서비스에서 찾는다(못 찾으면 고를 수 없다).
 */
@Composable
fun StoryPlaceConfirm(
    initial: StoryPlace?,
    pickOnMap: Boolean,
    origin: StoryOrigin?,
    onBack: () -> Unit,
    onChoose: (StoryPlace) -> Unit,
) {
    var candidate by rememberSaveable(stateSaver = StoryPlaceSaver) { mutableStateOf(initial) }
    var tapped by remember { mutableStateOf<GeoPoint?>(null) }
    var lookingUp by remember { mutableStateOf(false) }
    var lookupFailed by remember { mutableStateOf(false) }
    LaunchedEffect(tapped) {
        val point = tapped ?: return@LaunchedEffect
        // 새 자리를 눌렀다 — 앞서 찾은 이름을 새 자리에 붙여 보이지 않게 먼저 비운다
        candidate = null
        lookingUp = true
        lookupFailed = false
        val named = ServiceLocator.placeSearch.nameAt(point)
        lookingUp = false
        if (named == null) lookupFailed = true else candidate = named
    }
    val nearby = rememberNearbyPlaces(origin)
    val chosen = candidate
    val pins = remember(nearby, chosen) {
        val base = nearby.map { (place, meters) -> StoryPin(place, 0, meters) }
        if (chosen == null || base.any { it.place.key == chosen.key } || pickOnMap) base
        else base + StoryPin(chosen, 0, origin?.let { haversineMeters(it.point, chosen.point) })
    }
    Box(Modifier.fillMaxSize().background(Night).testTag("story-place-confirm")) {
        StoryPinsMap(
            pins = pins,
            origin = origin?.point,
            originLabel = stringResource(if (origin?.manual == true) R.string.story_origin_region else R.string.story_origin_me),
            modifier = Modifier.fillMaxSize(),
            selectedKey = chosen?.key,
            interactive = true,
            extraFocus = if (!pickOnMap) listOfNotNull(initial?.point) else emptyList(),
            picked = if (pickOnMap) chosen?.point ?: tapped else null,
            onPin = { pin -> candidate = pin.place; lookupFailed = false },
            onTapMap = if (pickOnMap) { point -> tapped = point } else null,
        )
        Box(
            Modifier.fillMaxWidth().background(Brush.verticalGradient(0f to Night, 1f to Night.copy(alpha = 0f)))
                .statusBarsPadding(),
        ) {
            StoryHeader(stringResource(R.string.story_place_confirm_title), onBack)
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(Carbon)
                .navigationBarsPadding()
                .padding(start = StoryFormGutter, end = StoryFormGutter, top = 10.dp, bottom = 16.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(Edge))
            }
            Spacer(Modifier.height(14.dp))
            if (chosen != null) {
                Text(chosen.name, color = Snow, fontSize = 23.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("story-confirm-name"))
                if (chosen.address.isNotBlank()) Text(chosen.address, color = Slate, fontSize = 12.sp)
                val away = distanceFrom(origin, chosen.point)
                if (away.isNotEmpty()) Text(away, color = Slate, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.story_place_confirm_note), color = Silver, fontSize = 12.sp)
            } else {
                Text(
                    stringResource(
                        when {
                            lookingUp -> R.string.story_place_looking_up
                            lookupFailed -> R.string.story_place_lookup_failed
                            else -> R.string.story_place_tap_map
                        },
                    ),
                    color = Silver, fontSize = 14.sp, lineHeight = 21.sp,
                )
            }
            Spacer(Modifier.height(14.dp))
            StoryButton(
                stringResource(R.string.story_place_choose),
                { chosen?.let(onChoose) },
                Modifier.testTag("story-place-choose"),
                enabled = chosen != null && !lookingUp,
                busy = lookingUp,
            )
        }
    }
}
