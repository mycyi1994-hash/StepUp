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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Place
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.stepup.android.domain.StoryPlaceSource
import com.stepup.android.domain.haversineMeters
import com.stepup.android.domain.storyPlace
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.feedbackClickable
import kotlinx.coroutines.delay

/** 기준점 · 핀 · 코스가 모두 없을 때 지도에서 고르기를 여는 자리(서울 시청) — 거리를 재는 데 쓰지 않는다 */
private val DEFAULT_MAP_CENTER = GeoPoint(37.5663, 126.9779)

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

/**
 * 장소 선택을 어디서 열었는가 — 글쓰기의 장소 줄(기본) · 내 주변 · 코스 주변 · 최근 장소 · 직접 검색.
 * 고른 장소는 그 버튼을 고른 모습으로 보인다(장소 줄 · 지도에서 고른 것은 버튼 없이).
 */
enum class StoryPickMode(val source: StoryPlaceSource?) {
    DEFAULT(null),
    NEARBY(StoryPlaceSource.NEARBY),
    COURSE(StoryPlaceSource.COURSE),
    RECENT(StoryPlaceSource.RECENT),
    SEARCH(StoryPlaceSource.SEARCH),
}

/**
 * 붙인 코스 둘레의 장소 — 코스의 처음 · 가운데 · 끝 자리에 붙은 이름(지도 서비스)과, 그 곁(400m)에 글이 올라온
 * 공개 장소. 찾는 동안은 null.
 */
@Composable
private fun rememberCoursePlaces(course: List<GeoPoint>): List<Pair<StoryPlace, Double?>>? {
    val posts by ServiceLocator.communityRepository.posts.collectAsState()
    var named by remember(course) { mutableStateOf<List<StoryPlace>?>(null) }
    LaunchedEffect(course) {
        val probes = listOfNotNull(course.firstOrNull(), course.getOrNull(course.size / 2), course.lastOrNull()).distinct()
        named = probes.mapNotNull { runCatching { ServiceLocator.placeSearch.nameAt(it) }.getOrNull() }
    }
    val found = named ?: return null
    return remember(found, posts) {
        val near = posts.filter { !it.isFlash && it.crewId.isEmpty() }.mapNotNull { it.storyPlace }
            .filter { place -> course.any { haversineMeters(it, place.point) <= 400 } }
        (found + near).distinctBy { it.name }.map { it to null }
    }
}

/**
 * 장소 선택(CM14 · CM16) — 이름 검색 · 가까운 장소(또는 코스 주변 · 최근 장소) · 지도에서 고르기.
 * 검색 결과 · 장소 줄은 한 번 누르면 [onPick] 으로 바로 정해진다(확인 화면을 거치지 않는다). 지도에서 자리를 직접 짚을 때만
 * [onPickOnMap] → 장소 확인(CM15). [mode] 가 직접 검색이면 검색 칸에 바로 입력할 수 있게 연다.
 * [onQuick] 이 있으면 검색 칸 아래에 "내 주변 >" · "최근 장소 >" 바로가기(장소 쉽게 고르기 시트로).
 */
@Composable
fun StoryPlacePicker(
    origin: StoryOrigin?,
    onBack: () -> Unit,
    onPick: (StoryPlace) -> Unit,
    onPickOnMap: () -> Unit,
    mode: StoryPickMode = StoryPickMode.DEFAULT,
    course: List<GeoPoint> = emptyList(),
    recent: List<StoryPlace> = emptyList(),
    onQuick: ((StoryPickMode) -> Unit)? = null,
) {
    val t = runTone()
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
    val around = rememberNearbyPlaces(origin)
    val coursePlaces = if (mode == StoryPickMode.COURSE) rememberCoursePlaces(course) else emptyList()
    val nearby = when (mode) {
        StoryPickMode.COURSE -> coursePlaces.orEmpty()
        StoryPickMode.RECENT -> recent.map { it to origin?.let { o -> haversineMeters(o.point, it.point) } }
        else -> around
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(mode) { if (mode == StoryPickMode.SEARCH) runCatching { focus.requestFocus() } }

    Column(Modifier.fillMaxSize().imePadding().testTag("story-place-picker")) {
        StoryHeader(stringResource(R.string.story_place_picker_title), onBack)
        StorySearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(R.string.story_place_search_hint),
            modifier = Modifier.padding(horizontal = StoryFormGutter),
            tag = "story-place-search",
            focusRequester = focus,
        )
        if (onQuick != null) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = StoryFormGutter), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOfNotNull(StoryPlaceSource.NEARBY, StoryPlaceSource.RECENT.takeIf { recent.isNotEmpty() }).forEach { source ->
                    val (label, icon) = placeSourceLabel(source)
                    val shape = RoundedCornerShape(14.dp)
                    Row(
                        Modifier.weight(1f).heightIn(min = 56.dp).clip(shape).background(t.panel, shape).border(1.dp, t.panelEdge, shape)
                            .feedbackClickable(role = Role.Button, onClick = {
                                onQuick(if (source == StoryPlaceSource.NEARBY) StoryPickMode.NEARBY else StoryPickMode.RECENT)
                            })
                            .padding(horizontal = 14.dp)
                            .testTag("story-place-quick-${source.name}"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(icon, contentDescription = null, tint = t.cyan, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(label, style = runTextStyle(16.sp, t.text, FontWeight.Bold), modifier = Modifier.weight(1f), maxLines = 1)
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = t.label, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(start = StoryFormGutter, end = StoryFormGutter, top = 18.dp, bottom = 24.dp),
        ) {
            // 같은 열쇠가 두 번 오면 목록이 멈춘다 — 한 번만
            val found = (result as? PlaceSearchResult.Found)?.places?.distinctBy { it.key }
            when {
                query.isBlank() -> {
                    item {
                        Text(
                            stringResource(
                                when (mode) {
                                    StoryPickMode.COURSE -> R.string.story_place_course_heading
                                    StoryPickMode.RECENT -> R.string.story_place_recent_heading
                                    else -> R.string.story_place_nearby
                                },
                            ),
                            style = runTextStyle(15.sp, t.label, FontWeight.Medium), modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    if (mode == StoryPickMode.COURSE && coursePlaces == null) {
                        item {
                            Text(stringResource(R.string.story_place_course_finding), style = runTextStyle(15.sp, t.label, FontWeight.Medium),
                                modifier = Modifier.padding(vertical = 10.dp))
                        }
                    } else if (nearby.isEmpty()) {
                        item {
                            Text(
                                stringResource(if (mode == StoryPickMode.COURSE) R.string.story_place_course_empty else R.string.story_place_nearby_empty),
                                style = runTextStyle(15.sp, t.label, FontWeight.Medium, 1.5f), modifier = Modifier.padding(vertical = 10.dp),
                            )
                        }
                    }
                    items(nearby, key = { it.first.key }) { (place, meters) ->
                        StoryPlaceRow(place, storyDistance(meters)) { onPick(place) }
                        StoryDivider()
                    }
                }
                searching || result == null -> item {
                    Row(Modifier.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        RunSpinner(Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.story_place_searching), style = runTextStyle(15.sp, t.label, FontWeight.Medium))
                    }
                }
                result is PlaceSearchResult.Offline -> item {
                    StoryStateBlock(Icons.Filled.Refresh, stringResource(R.string.story_place_offline_title),
                        stringResource(R.string.story_error_body)) {
                        StoryButton(stringResource(R.string.story_retry), { attempt++ })
                    }
                }
                found.isNullOrEmpty() -> item {
                    Column {
                        Text(stringResource(R.string.story_region_results), style = runTextStyle(15.sp, t.label, FontWeight.Medium))
                        StoryStateBlock(Icons.Filled.Search, stringResource(R.string.story_blue_place_empty_title),
                            stringResource(R.string.story_blue_place_empty_body), Modifier.testTag("story-place-empty")) {
                            StoryTextButton(stringResource(R.string.story_place_clear), { query = "" }, color = t.cyan)
                        }
                    }
                }
                else -> {
                    item {
                        Text(stringResource(R.string.story_region_results), style = runTextStyle(15.sp, t.label, FontWeight.Medium),
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
                Column {
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.story_blue_place_pick_note), style = runTextStyle(15.sp, t.label, FontWeight.Medium))
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(top = 6.dp).clip(RoundedCornerShape(12.dp))
                        .feedbackClickable(role = Role.Button, onClick = onPickOnMap).testTag("story-place-on-map"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Map, contentDescription = null, tint = t.cyan, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.story_blue_pick_on_map), style = runTextStyle(16.sp, t.cyan, FontWeight.Bold))
                }
                }
            }
        }
    }
}

/**
 * 장소 쉽게 고르기(WRITE10 · WRITE17) — 코스 주변 · 내 주변 · 최근 장소를 오가며 장소 줄을 한 번 누르면 바로 정해지고 작성으로
 * 돌아간다. 코스 주변은 유효한 경로가 있을 때만, 최근 장소는 고른 적이 있을 때만 버튼이 있다. 아래 "장소 이름으로 검색"과
 * "지도에서 직접 고르기"(이것만 확인 화면을 거친다). ×/뒤로는 작성 내용을 그대로 둔 채 닫는다.
 */
@Composable
fun StoryQuickPlaceSheet(
    mode: StoryPickMode,
    sources: List<StoryPlaceSource>,
    origin: StoryOrigin?,
    locating: Boolean,
    course: List<GeoPoint>,
    recent: List<StoryPlace>,
    current: StoryPlace?,
    onMode: (StoryPickMode) -> Unit,
    onPick: (StoryPlace) -> Unit,
    onSearch: () -> Unit,
    onMap: () -> Unit,
    onDismiss: () -> Unit,
) {
    val t = runTone()
    val around = rememberNearbyPlaces(origin)
    val coursePlaces = if (mode == StoryPickMode.COURSE) rememberCoursePlaces(course) else emptyList()
    val rows: List<Pair<StoryPlace, Double?>>? = when (mode) {
        StoryPickMode.COURSE -> coursePlaces
        StoryPickMode.RECENT -> recent.map { it to origin?.let { o -> haversineMeters(o.point, it.point) } }
        else -> if (origin == null && locating) null else around
    }
    StorySheet(stringResource(R.string.story_blue_quick_title), onDismiss, modifier = Modifier.testTag("story-place-quick")) {
        Text(stringResource(R.string.story_blue_quick_body), style = runTextStyle(16.sp, t.label, FontWeight.Medium))
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            sources.filter { it != StoryPlaceSource.SEARCH }.forEach { source ->
                val (label, icon) = placeSourceLabel(source)
                ComposeChip(
                    text = label,
                    onClick = {
                        onMode(
                            when (source) {
                                StoryPlaceSource.COURSE -> StoryPickMode.COURSE
                                StoryPlaceSource.RECENT -> StoryPickMode.RECENT
                                else -> StoryPickMode.NEARBY
                            },
                        )
                    },
                    modifier = Modifier.weight(1f).testTag("story-quick-${source.name}"),
                    selected = mode.source == source,
                    icon = icon,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when {
                rows == null -> Row(Modifier.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    RunSpinner(Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(if (mode == StoryPickMode.COURSE) R.string.story_place_course_finding else R.string.story_blue_locating_title),
                        style = runTextStyle(15.sp, t.label, FontWeight.Medium),
                    )
                }
                rows.isEmpty() -> Text(
                    stringResource(if (mode == StoryPickMode.COURSE) R.string.story_place_course_empty else R.string.story_place_nearby_empty),
                    style = runTextStyle(15.sp, t.label, FontWeight.Medium, 1.5f), modifier = Modifier.padding(vertical = 8.dp),
                )
                else -> rows.forEach { (place, meters) ->
                    StoryPlaceCardRow(place, storyDistance(meters), chosen = current?.key == place.key) { onPick(place) }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        StoryButton(stringResource(R.string.story_noloc_search), onSearch, Modifier.testTag("story-quick-search"))
        Spacer(Modifier.height(10.dp))
        StoryButton(stringResource(R.string.story_blue_pick_on_map), onMap, Modifier.testTag("story-quick-map"),
            style = StoryButtonStyle.SECONDARY)
    }
}

/** 시트 안의 장소 칸 — 남색 면 · 파란 테두리, 핀 · 이름 · 주소, 지금 고른 장소면 시안 체크 */
@Composable
private fun StoryPlaceCardRow(place: StoryPlace, distance: String, chosen: Boolean, onClick: () -> Unit) {
    val t = runTone()
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 68.dp).clip(shape).background(t.panel, shape)
            .border(if (chosen) 1.5.dp else 1.dp, if (chosen) t.cyan else t.panelEdge, shape)
            .feedbackClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp).testTag("story-place-row"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Place, contentDescription = null, tint = t.cyan, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(place.name, style = runTextStyle(17.sp, t.text, FontWeight.ExtraBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = listOf(place.address, distance).filter { it.isNotBlank() }.joinToString(" · ")
            if (sub.isNotEmpty()) Text(sub, style = runTextStyle(14.sp, t.label, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (chosen) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(t.cyan), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = t.screen, modifier = Modifier.size(20.dp))
            }
        } else {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = t.label, modifier = Modifier.size(24.dp))
        }
    }
}

/** 검색 칸 — 돋보기 · 입력 · 지우기(×). 남색 칸 · 파란 테두리, 누르면 밝은 파랑 */
@Composable
fun StorySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    tag: String = "story-search",
    focusRequester: FocusRequester? = null,
) {
    val t = runTone()
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = 60.dp).clip(shape).background(t.panel, shape)
            .border(if (focused) 1.5.dp else 1.dp, if (focused) t.cobaltText else t.panelEdge, shape)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = t.label, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text(placeholder, style = runTextStyle(17.sp, t.muted, FontWeight.Medium), maxLines = 1)
            BasicTextField(
                value = value,
                onValueChange = { onValueChange(it.take(60)) },
                singleLine = true,
                textStyle = runTextStyle(18.sp, t.text, FontWeight.Bold),
                cursorBrush = SolidColor(t.cyan),
                modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                    .testTag(tag),
            )
        }
        if (value.isNotEmpty()) {
            val clear = stringResource(R.string.story_place_clear)
            Box(
                Modifier.size(48.dp).clip(CircleShape).feedbackClickable(role = Role.Button, onClick = { onValueChange("") })
                    .semantics { contentDescription = clear },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = t.label, modifier = Modifier.size(22.dp))
            }
        }
    }
}

/** 장소 한 줄(CM14 · CM03) — 핀 · 이름 · 주소, 오른쪽 거리(모르면 화살표) */
@Composable
fun StoryPlaceRow(place: StoryPlace, distance: String, onClick: () -> Unit) {
    val t = runTone()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 76.dp).feedbackClickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp).testTag("story-place-row"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Place, contentDescription = null, tint = t.cyan, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(place.name, style = runTextStyle(19.sp, t.text, FontWeight.ExtraBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (place.address.isNotBlank()) {
                Text(place.address, style = runTextStyle(15.sp, t.label, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(8.dp))
        if (distance.isNotEmpty()) {
            Text(distance, style = runTextStyle(16.sp, t.label, FontWeight.Bold))
        } else {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = t.label, modifier = Modifier.size(24.dp))
        }
    }
}

/**
 * 지도에서 장소 직접 고르기 · 확인(CM15) — 위는 실제 지도, 아래는 장소 이름 · 주소 · 거리와 "이 장소로 선택".
 * [pickOnMap] 이면 지도를 눌러 자리를 고르고, 그 자리의 이름을 지도 서비스에서 찾는다(못 찾으면 고를 수 없다).
 */
@Composable
fun StoryPlaceConfirm(
    initial: StoryPlace?,
    pickOnMap: Boolean,
    origin: StoryOrigin?,
    onBack: () -> Unit,
    onChoose: (StoryPlace) -> Unit,
    /** 기준점이 없을 때(위치 없음 · 지역 안 고름) 지도를 맞출 자리 — 붙인 코스 · 최근 장소 */
    hint: List<GeoPoint> = emptyList(),
) {
    val t = runTone()
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
    Column(Modifier.fillMaxSize().testTag("story-place-confirm")) {
        StoryHeader(stringResource(R.string.story_blue_place_confirm_title), onBack)
        Box(Modifier.weight(1f).fillMaxWidth().background(t.inset)) {
            StoryMapTone {
                StoryPinsMap(
                    pins = pins,
                    origin = origin?.point,
                    originLabel = stringResource(if (origin?.manual == true) R.string.story_origin_region else R.string.story_origin_me),
                    modifier = Modifier.fillMaxSize(),
                    selectedKey = chosen?.key,
                    interactive = true,
                    extraFocus = (if (!pickOnMap) listOfNotNull(initial?.point) else emptyList()) +
                        (if (origin == null && pins.isEmpty()) hint.ifEmpty { listOf(DEFAULT_MAP_CENTER) } else emptyList()),
                    picked = if (pickOnMap) chosen?.point ?: tapped else null,
                    onPin = { pin -> candidate = pin.place; lookupFailed = false },
                    onTapMap = if (pickOnMap) { point -> tapped = point } else null,
                )
            }
            StoryDivider(Modifier.align(Alignment.BottomCenter))
        }
        Column(Modifier.fillMaxWidth().padding(start = StoryFormGutter, end = StoryFormGutter, top = 18.dp, bottom = 12.dp)) {
            if (chosen != null) {
                Text(chosen.name, style = runTextStyle(28.sp, t.text, FontWeight.ExtraBold), maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("story-confirm-name"))
                if (chosen.address.isNotBlank()) Text(chosen.address, style = runTextStyle(16.sp, t.label, FontWeight.Medium))
                val away = distanceFrom(origin, chosen.point)
                if (away.isNotEmpty()) Text(away, style = runTextStyle(15.sp, t.label, FontWeight.Medium))
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.story_blue_place_confirm_note), style = runTextStyle(15.sp, t.label, FontWeight.Medium))
            } else {
                Text(
                    stringResource(
                        when {
                            lookingUp -> R.string.story_place_looking_up
                            lookupFailed -> R.string.story_place_lookup_failed
                            else -> R.string.story_place_tap_map
                        },
                    ),
                    style = runTextStyle(17.sp, t.text, FontWeight.SemiBold, 1.5f),
                )
            }
            Spacer(Modifier.height(16.dp))
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
