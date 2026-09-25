package com.stepup.android.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.TerritoryApi
import com.stepup.android.data.remote.TerritoryCell
import com.stepup.android.data.remote.TerritoryStanding
import com.stepup.android.data.repo.CommunityRepository
import com.stepup.android.data.repo.CourseRepository
import com.stepup.android.domain.Post
import com.stepup.android.domain.RunCourse
import kotlin.math.floor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 지도 위에서 무엇을 보나 */
enum class MapMode { NEARBY, TERRITORY }

/** 땅따먹기 칸을 읽은 결과 */
sealed interface TerritoryState {
    data object Loading : TerritoryState
    data class Ready(val cells: List<TerritoryCell>) : TerritoryState
    data object SignIn : TerritoryState
    data object ZoomIn : TerritoryState
    data object Failed : TerritoryState
}

/**
 * 지도 화면 — 내 주변 번개·코스, 그리고 땅따먹기.
 *
 * 번개와 코스는 이미 게시판이 받아 둔 것을 지도에 꽂는다. 땅따먹기 칸은 보이는
 * 범위만큼 서버에 묻는다 — 서울 전체를 한 번에 받지 않는다.
 */
class MapViewModel(
    private val communityRepository: CommunityRepository,
    private val courseRepository: CourseRepository,
    private val territoryApi: TerritoryApi,
) : ViewModel() {

    val mode = MutableStateFlow(MapMode.NEARBY)

    /** 모이는 자리가 있는 번개러닝. 마감된 것은 뺀다. */
    val flashes: StateFlow<List<Post>> = communityRepository.posts
        .map { list -> list.filter { it.isFlash && !it.isClosed && it.lat != null && it.lng != null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 출발점이 있는 코스 — 게시판 코스와 내 코스 */
    val courses: StateFlow<List<RunCourse>> = courseRepository.board
        .map { list -> list.filter { it.hasTrack } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _territory = MutableStateFlow<TerritoryState>(TerritoryState.Loading)
    val territory: StateFlow<TerritoryState> = _territory

    private val _standings = MutableStateFlow<List<TerritoryStanding>>(emptyList())
    val standings: StateFlow<List<TerritoryStanding>> = _standings

    private var lastBox: String = ""
    private var lastViewport: DoubleArray? = null
    private var viewportJob: Job? = null

    init {
        viewModelScope.launch { communityRepository.refresh() }
        viewModelScope.launch { courseRepository.refreshBoard() }
    }

    fun select(next: MapMode) {
        mode.value = next
        if (next == MapMode.TERRITORY) {
            loadStandings()
            reloadViewport()
        }
    }

    /**
     * 지도가 움직였다. 땅따먹기를 보고 있으면 그 범위의 칸을 받는다.
     *
     * 범위를 약 1km 격자로 반올림해서 같은 범위를 두 번 묻지 않고, 손가락이
     * 멈출 때까지 잠깐 기다린다.
     */
    fun onViewport(minLat: Double, minLng: Double, maxLat: Double, maxLng: Double) {
        lastViewport = doubleArrayOf(minLat, minLng, maxLat, maxLng)
        if (mode.value != MapMode.TERRITORY) return
        val box = listOf(minLat, minLng, maxLat, maxLng).joinToString(",") { snap(it).toString() }
        if (box == lastBox && _territory.value is TerritoryState.Ready) return
        lastBox = box
        viewportJob?.cancel()
        viewportJob = viewModelScope.launch {
            delay(350)
            if (maxLat - minLat > MAX_SPAN || maxLng - minLng > MAX_SPAN) {
                _territory.value = TerritoryState.ZoomIn
                return@launch
            }
            val pad = 0.004
            _territory.value = when (
                val result = territoryApi.view(minLat - pad, minLng - pad, maxLat + pad, maxLng + pad)
            ) {
                is ServerResult.Ok -> TerritoryState.Ready(result.value)
                is ServerResult.SignInRequired -> TerritoryState.SignIn
                is ServerResult.Rejected -> TerritoryState.ZoomIn
                is ServerResult.Retry -> TerritoryState.Failed
            }
        }
    }

    fun retryTerritory() {
        loadStandings()
        reloadViewport()
    }

    /** 화면 검사용 — 지도 영역의 빈/오류 상태를 실제 지도 화면에 표시한다. */
    @androidx.annotation.VisibleForTesting
    fun showTerritoryForTest(state: TerritoryState) {
        viewportJob?.cancel()
        mode.value = MapMode.TERRITORY
        _territory.value = state
        _standings.value = emptyList()
    }

    private fun reloadViewport() {
        val v = lastViewport ?: return
        lastBox = ""
        onViewport(v[0], v[1], v[2], v[3])
    }

    private fun loadStandings() {
        viewModelScope.launch {
            val result = territoryApi.board(10)
            if (result is ServerResult.Ok) _standings.value = result.value
        }
    }

    private fun snap(v: Double): Double = floor(v * 100) / 100

    companion object {
        /** 서버가 받는 최대 범위(0.3도)보다 조금 좁게 */
        private const val MAX_SPAN = 0.25

        val Factory = viewModelFactory {
            initializer {
                MapViewModel(
                    ServiceLocator.communityRepository,
                    ServiceLocator.courseRepository,
                    ServiceLocator.territoryApi,
                )
            }
        }
    }
}
