package com.stepup.android.ui.screens.community.stories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.data.repo.BoardSyncState
import com.stepup.android.data.repo.CommunityRepository
import com.stepup.android.data.repo.PlaceSearch
import com.stepup.android.data.repo.StoryNotice
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.Post
import com.stepup.android.domain.StoryDraft
import com.stepup.android.domain.StoryOrigin
import com.stepup.android.domain.StoryPlace
import com.stepup.android.domain.StoryRange
import com.stepup.android.domain.haversineMeters
import com.stepup.android.domain.storyPlace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 목록 한 줄 — 글과 그 장소, 기준점에서 장소까지 거리(모르면 null) */
data class StoryItem(val post: Post, val place: StoryPlace?, val meters: Double?)

/** 목록 · 지도에 함께 쓰는 한 벌 — 같은 범위 · 장소 필터 · 숨김 기준을 쓴다 */
data class StoryListState(
    /** 범위 안(장소를 골랐으면 그 장소)의 글. 최신 순 */
    val nearby: List<StoryItem> = emptyList(),
    /** [nearby] 를 장소별로 센 핀 — 목록이 비었는데 핀에 글이 있다고 하지 않게 같은 목록에서 센다 */
    val pins: List<StoryPin> = emptyList(),
    /** 장소가 없는 예전 글(목록형 이전에 쓴 자유 · 꿀팁 글). 지도 · 거리에는 들어가지 않는다 */
    val legacy: List<StoryItem> = emptyList(),
    /** 이 목록을 거른 장소([StoryPlace.key]) — 전체 장소면 null */
    val filterKey: String? = null,
)

/** 기준점을 정할 수 있는가 */
enum class StoryLocationStatus {
    /** 기준점이 있다(내 위치 또는 고른 지역) */
    READY,

    /** 위치 권한이 없고 고른 지역도 없다 — 권한 안내 */
    NEED_PERMISSION,

    /** 권한은 있는데 아직 위치를 모른다 */
    LOCATING,
}

/**
 * 동네 이야기 목록 — 커뮤니티 탭에 묶여 상세를 다녀와도 범위 · 장소 필터 · 펼침이 그대로다.
 *
 * 기준점은 직접 고른 지역이 있으면 그 지역, 없으면 폰 위치다. 위치를 모르면 거리를 만들지 않는다.
 */
class StoriesViewModel(
    private val repository: CommunityRepository,
    prefs: UserPrefs,
    private val places: PlaceSearch,
    private val saved: SavedStateHandle,
) : ViewModel() {

    init {
        viewModelScope.launch { repository.refresh() }
    }

    val sync: StateFlow<BoardSyncState> = repository.sync

    fun refresh() {
        viewModelScope.launch { repository.refresh() }
    }

    // ── 기준점 ──────────────────────────────────────────────────

    private val here = MutableStateFlow<GeoPoint?>(null)
    private val locationAllowed = MutableStateFlow(false)
    private val areaName = MutableStateFlow("")
    private var areaLookedUpAt: GeoPoint? = null

    /**
     * 화면이 폰 위치와 권한을 알려 준다. 권한이 있는데 이번 화면이 아직 위치를 모르면(상세에서 돌아온
     * 첫 순간) 앞서 알던 자리를 그대로 둔다 — 목록이 권한 안내로 깜빡이며 스크롤을 잃지 않게.
     */
    fun onLocation(point: GeoPoint?, allowed: Boolean) {
        locationAllowed.value = allowed
        when {
            !allowed -> here.value = null
            point != null -> {
                here.value = point
                lookUpArea(point)
            }
        }
    }

    private fun lookUpArea(point: GeoPoint) {
        val last = areaLookedUpAt
        // 몇 걸음 움직일 때마다 묻지 않는다 — 300m 넘게 옮겼을 때만
        if (last != null && haversineMeters(last, point) < 300) return
        areaLookedUpAt = point
        viewModelScope.launch { areaName.value = places.areaName(point).orEmpty() }
    }

    val region: StateFlow<StoryPlace?> = prefs.storyRegion
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val origin: StateFlow<StoryOrigin?> = combine(here, region, areaName) { point, chosen, area ->
        when {
            chosen != null -> StoryOrigin(chosen.point, chosen.name, manual = true)
            point != null -> StoryOrigin(point, area, manual = false)
            else -> null
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val locationStatus: StateFlow<StoryLocationStatus> = combine(locationAllowed, here, region) { allowed, point, chosen ->
        when {
            chosen != null || point != null -> StoryLocationStatus.READY
            allowed -> StoryLocationStatus.LOCATING
            else -> StoryLocationStatus.NEED_PERMISSION
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StoryLocationStatus.NEED_PERMISSION)

    // ── 범위 · 장소 필터 · 펼침(상세를 다녀와도, 프로세스가 다시 떠도 유지) ─────────────

    val range: StateFlow<StoryRange> = saved.getStateFlow(KEY_RANGE, StoryRange.DEFAULT.name)
        .map { StoryRange.of(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, StoryRange.of(saved.get<String>(KEY_RANGE)))

    fun setRange(next: StoryRange) {
        saved[KEY_RANGE] = next.name
    }

    private val filterName = saved.getStateFlow(KEY_FILTER_NAME, "")
    private val filterAddress = saved.getStateFlow(KEY_FILTER_ADDRESS, "")
    private val filterLat = saved.getStateFlow(KEY_FILTER_LAT, Double.NaN)
    private val filterLng = saved.getStateFlow(KEY_FILTER_LNG, Double.NaN)

    /** 고른 장소 — 그 장소의 글만 본다. "전체 장소"로 푼다 */
    val placeFilter: StateFlow<StoryPlace?> = combine(filterName, filterAddress, filterLat, filterLng) { name, address, lat, lng ->
        if (name.isBlank() || lat.isNaN() || lng.isNaN()) null else StoryPlace(name, address, lat, lng)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun filterPlace(place: StoryPlace?) {
        saved[KEY_FILTER_NAME] = place?.name.orEmpty()
        saved[KEY_FILTER_ADDRESS] = place?.address.orEmpty()
        saved[KEY_FILTER_LAT] = place?.lat ?: Double.NaN
        saved[KEY_FILTER_LNG] = place?.lng ?: Double.NaN
    }

    val expanded: StateFlow<Boolean> = saved.getStateFlow(KEY_EXPANDED, false)

    fun setExpanded(next: Boolean) {
        saved[KEY_EXPANDED] = next
    }

    // ── 목록 ────────────────────────────────────────────────────

    /** 이 계정이 숨긴 글 — 목록 · 지도 · 미리보기가 같은 기준으로 거른다 */
    private val hidden: StateFlow<Set<Long>> = repository.hiddenStories
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val list: StateFlow<StoryListState> = combine(
        repository.posts, hidden, origin, range, placeFilter,
    ) { posts, hiddenIds, from, currentRange, filter ->
        buildStoryList(posts, hiddenIds, from, currentRange, filter)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StoryListState())

    /** 지도 전체 화면의 장소 목록 — 범위 안의 모든 장소(필터와 상관없이) */
    val areaPins: StateFlow<List<StoryPin>> = combine(
        repository.posts, hidden, origin, range,
    ) { posts, hiddenIds, from, currentRange ->
        buildStoryList(posts, hiddenIds, from, currentRange, null).pins
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** 한 장소의 글(지도에서 장소를 골랐을 때 미리보기) — 같은 숨김 기준 */
    fun postsAt(place: StoryPlace): List<StoryItem> {
        val from = origin.value
        val hiddenIds = hidden.value
        return repository.posts.value
            .filter { it.isVisibleStory(hiddenIds) && it.storyPlace?.key == place.key }
            .sortedByDescending { it.createdAt }
            .map { StoryItem(it, it.storyPlace, from?.let { o -> haversineMeters(o.point, place.point) }) }
    }

    /** 목록에서 좋아요 — 누르는 순간 바뀌고, 서버가 거절하면 저장소가 되돌린다 */
    fun toggleLike(id: Long) {
        viewModelScope.launch { repository.toggleStoryLike(id) }
    }

    // ── 확인 메시지 · 되돌리기 · 쓰다 만 글 ────────────────────────────

    val notice: StateFlow<StoryNotice?> = repository.storyNotice

    fun consumeNotice(notice: StoryNotice) = repository.consumeStoryNotice(notice)

    fun undoHide(notice: StoryNotice.Hidden) {
        repository.consumeStoryNotice(notice)
        viewModelScope.launch { repository.setStoryHidden(notice.postId, false) }
    }

    val draft: StateFlow<StoryDraft?> = repository.storyDraft
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** 새로 쓰기 — 쓰다 만 글을 지운다 */
    fun discardDraft() {
        viewModelScope.launch { repository.saveStoryDraft(null) }
    }

    /**
     * 방금 올린 글이 목록 맨 위에 보이게 한다 — 장소 필터를 풀고, 범위 밖이면 그 장소가 들어오는 가장
     * 좁은 범위로 넓힌다. 3km 밖이면 그 장소의 글로 보여 준다.
     */
    fun reveal(postId: Long) {
        val place = repository.posts.value.firstOrNull { it.id == postId }?.storyPlace ?: return
        val from = origin.value
        if (from == null) {
            filterPlace(place)
            return
        }
        val fits = rangeToReveal(haversineMeters(from.point, place.point), range.value)
        if (fits != null) {
            filterPlace(null)
            if (fits != range.value) setRange(fits)
        } else {
            filterPlace(place)
        }
    }

    /**
     * 새 글에 미리 붙일 장소 — 목록에서 직접 고른 장소를 보고 있을 때만 그곳. 위치로 가까운 장소를 저절로
     * 고르지 않는다(쉬운 글쓰기 상황별 지시서 2026-09-28: 위치를 동의 없이 자동으로 선택하지 않는다).
     */
    fun suggestedPlace(): StoryPlace? = placeFilter.value

    companion object {
        private const val KEY_RANGE = "story_range"
        private const val KEY_FILTER_NAME = "story_filter_name"
        private const val KEY_FILTER_ADDRESS = "story_filter_address"
        private const val KEY_FILTER_LAT = "story_filter_lat"
        private const val KEY_FILTER_LNG = "story_filter_lng"
        private const val KEY_EXPANDED = "story_expanded"

        val Factory = viewModelFactory {
            initializer {
                StoriesViewModel(
                    repository = ServiceLocator.communityRepository,
                    prefs = ServiceLocator.userPrefs,
                    places = ServiceLocator.placeSearch,
                    saved = createSavedStateHandle(),
                )
            }
        }
    }
}

/**
 * 목록 · 지도 핀 · 장소 없는 이전 글을 한 번에 만든다 — 셋이 같은 범위 · 장소 필터 · 숨김 기준을 쓴다.
 *
 * 범위는 기준점에서 글 장소까지. 기준점을 모르면 범위 안의 글이 없다(거리를 지어내지 않는다).
 * 장소를 골랐으면 범위와 상관없이 그 장소의 글이다 — 범위 밖 장소를 지도에서 골랐다고 빈 목록이 되지 않게.
 * 핀은 목록과 같은 글에서 센다(빈 목록인데 핀에 글이 있다고 하지 않는다).
 */
internal fun buildStoryList(
    posts: List<Post>,
    hidden: Set<Long>,
    from: StoryOrigin?,
    range: StoryRange,
    filter: StoryPlace?,
): StoryListState {
    val visiblePosts = posts.filter { it.isVisibleStory(hidden) }
    val withPlace = visiblePosts.mapNotNull { post ->
        val place = post.storyPlace ?: return@mapNotNull null
        StoryItem(post, place, from?.let { haversineMeters(it.point, place.point) })
    }
    val inRange = if (from == null) emptyList() else withPlace.filter { (it.meters ?: Double.MAX_VALUE) <= range.meters }
    val shown = (if (filter != null) withPlace.filter { it.place?.key == filter.key } else inRange)
        .sortedByDescending { it.post.createdAt }
    val pinSource = if (filter != null) (inRange + shown).distinctBy { it.post.id } else inRange
    val pins = pinSource.groupBy { it.place!!.key }.map { (_, items) ->
        StoryPin(items.first().place!!, items.size, items.first().meters)
    }.sortedBy { it.meters ?: Double.MAX_VALUE }
    val legacy = if (filter == null) {
        visiblePosts.filter { it.storyPlace == null }.sortedByDescending { it.createdAt }.map { StoryItem(it, null, null) }
    } else {
        emptyList()
    }
    return StoryListState(shown, pins, legacy, filter?.key)
}

/** 동네 이야기에 들어가는 글 — 전체 게시판의 번개가 아닌 글 중 숨기지 않은 것 */
internal fun Post.isVisibleStory(hidden: Set<Long>): Boolean = !isFlash && crewId.isEmpty() && id !in hidden

/** 방금 올린 글의 장소가 들어오는 가장 좁은 범위 — 지금 범위보다 좁히지 않는다. 3km 밖이면 null */
internal fun rangeToReveal(awayMeters: Double, current: StoryRange): StoryRange? =
    StoryRange.entries.firstOrNull { it.meters >= current.meters && awayMeters <= it.meters }
