package com.stepup.android.ui.screens.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.remote.EventRow
import com.stepup.android.data.remote.NewsRow
import com.stepup.android.data.remote.RunningFeedApi
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.SourceRow
import com.stepup.android.data.repo.EventFilter
import com.stepup.android.data.repo.NewsFilter
import com.stepup.android.data.repo.RunningFeedRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** 못 가져왔을 때 화면이 할 말. 다음에 할 일이 저마다 다르다. */
enum class FeedProblem { OFFLINE, REJECTED }

data class EventsUi(
    val loading: Boolean = true,
    val problem: FeedProblem? = null,
    val rows: List<EventRow> = emptyList(),
    /** 이 목록을 받아 온 때. 화면이 "몇 시 기준"인지 말하는 데 쓴다. */
    val loadedAtMillis: Long = 0,
    val filter: EventFilter = EventFilter(),
    val total: Long = 0,
    /** 목록 보기 / 월별 달력 보기 */
    val calendar: Boolean = false,
    val month: YearMonth = YearMonth.now(),
    val pickedDay: LocalDate? = null,
)

data class NewsUi(
    val loading: Boolean = true,
    val problem: FeedProblem? = null,
    val rows: List<NewsRow> = emptyList(),
    val loadedAtMillis: Long = 0,
    val filter: NewsFilter = NewsFilter(),
    val total: Long = 0,
    /** 실린 기사에서 뽑은 언론사 목록. 없는 언론사로 거를 수는 없다. */
    val publishers: List<Pair<String, String>> = emptyList(),
)

/**
 * 뉴스 탭의 두 자리 — 러닝 이벤트와 러닝·건강 뉴스.
 *
 * 한 ViewModel 이 둘을 들고 있는 이유는 탭을 오갈 때 상태가 살아 있어야 하기
 * 때문이다. 탭마다 따로 두면 대회 쪽으로 갔다 돌아올 때마다 거르기와 스크롤이
 * 처음으로 돌아간다.
 *
 * ── 여기서 하지 않는 일 ──
 *
 * 보상을 주지 않는다. 대회를 누르거나 기사를 읽는 것은 바깥 사이트로 가는
 * 일이고, SUP 는 뛰어야 생긴다. 이 파일은 원장을 건드리는 코드를 갖고 있지
 * 않다.
 */
class RunningFeedViewModel(private val repo: RunningFeedRepository) : ViewModel() {

    val events = MutableStateFlow(EventsUi())
    val news = MutableStateFlow(NewsUi())
    val sources = MutableStateFlow<List<SourceRow>>(emptyList())

    /** 한 번 보여 주고 사라지는 말 */
    val message = MutableStateFlow<Int?>(null)

    init {
        loadEvents()
        loadNews()
        loadSources()
    }

    // ── 대회 ────────────────────────────────────────────────────────

    fun loadEvents(force: Boolean = false) {
        viewModelScope.launch {
            events.value = events.value.copy(loading = true, problem = null)
            when (val r = repo.events(events.value.filter, force = force)) {
                is ServerResult.Ok -> events.value = events.value.copy(
                    loading = false,
                    rows = r.value.value,
                    loadedAtMillis = r.value.loadedAtMillis,
                    total = r.value.value.firstOrNull()?.totalCount ?: 0,
                )
                else -> events.value = events.value.copy(loading = false, problem = r.problem())
            }
        }
    }

    fun editEvents(block: (EventFilter) -> EventFilter) {
        events.value = events.value.copy(filter = block(events.value.filter))
        loadEvents()
    }

    fun toggleCalendar() {
        events.value = events.value.copy(calendar = !events.value.calendar, pickedDay = null)
    }

    fun shiftMonth(delta: Long) {
        events.value = events.value.copy(
            month = events.value.month.plusMonths(delta),
            pickedDay = null,
        )
    }

    /** 달력에서 날짜를 누르면 그 날의 대회만 본다. 같은 날을 다시 누르면 푼다. */
    fun pickDay(day: LocalDate?) {
        events.value = events.value.copy(
            pickedDay = if (events.value.pickedDay == day) null else day,
        )
    }

    fun toggleSaveEvent(row: EventRow) {
        viewModelScope.launch {
            when (val r = repo.saveEvent(row.id, !row.saved)) {
                is ServerResult.Ok -> loadEvents(force = true)
                is ServerResult.SignInRequired ->
                    message.value = com.stepup.android.R.string.feed_sign_in_to_save
                else -> message.value = com.stepup.android.R.string.feed_save_failed
            }
        }
    }

    // ── 뉴스 ────────────────────────────────────────────────────────

    fun loadNews(force: Boolean = false) {
        viewModelScope.launch {
            news.value = news.value.copy(loading = true, problem = null)
            when (val r = repo.news(news.value.filter, force = force)) {
                is ServerResult.Ok -> {
                    val rows = r.value.value
                    news.value = news.value.copy(
                        loading = false,
                        rows = rows,
                        loadedAtMillis = r.value.loadedAtMillis,
                        total = rows.firstOrNull()?.totalCount ?: 0,
                        // 언론사 목록은 지금 실린 기사에서 뽑는다. 표에 있다는
                        // 이유로 기사 하나 없는 언론사를 칩으로 내놓지 않는다.
                        publishers = rows
                            .filter { it.publisherDomain.isNotBlank() }
                            .map { it.publisherDomain to it.publisherName }
                            .distinctBy { it.first }
                            .sortedBy { it.second },
                    )
                }
                else -> news.value = news.value.copy(loading = false, problem = r.problem())
            }
        }
    }

    fun editNews(block: (NewsFilter) -> NewsFilter) {
        news.value = news.value.copy(filter = block(news.value.filter))
        loadNews()
    }

    fun toggleSaveNews(row: NewsRow) {
        viewModelScope.launch {
            when (val r = repo.saveNews(row.id, !row.saved)) {
                is ServerResult.Ok -> loadNews(force = true)
                is ServerResult.SignInRequired ->
                    message.value = com.stepup.android.R.string.feed_sign_in_to_save
                else -> message.value = com.stepup.android.R.string.feed_save_failed
            }
        }
    }

    private fun loadSources() {
        viewModelScope.launch {
            (repo.sources() as? ServerResult.Ok)?.let { sources.value = it.value }
        }
    }

    fun consumeMessage() {
        message.value = null
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { RunningFeedViewModel(ServiceLocator.runningFeedRepository) }
        }

        /** 거리 필터 — 화면과 서버가 같은 값을 쓴다 */
        val DISTANCES = listOf(
            RunningFeedApi.ALL, "LTE_5K", "10K", "HALF", "FULL", "ULTRA", "OTHER",
        )

        /** 유형 필터. 거리와 별개다 — 트레일도 10km 가 있다. */
        val TYPES = listOf(
            RunningFeedApi.ALL, "ROAD", "TRAIL", "WALK", "FUNRUN", "CLASS",
        )

        val STATUSES = listOf(RunningFeedApi.ALL, "UPCOMING", "OPEN", "CLOSED")

        val CATEGORIES = listOf(
            RunningFeedApi.ALL, "RUNNING", "WALK_JOG", "TRAINING",
            "INJURY", "HEALTH", "RACE_NEWS",
        )

        /** 전국 시·도 */
        val REGIONS = listOf(
            RunningFeedApi.ALL, "서울", "부산", "대구", "인천", "광주", "대전", "울산",
            "세종", "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주",
        )
    }
}

private fun ServerResult<*>.problem(): FeedProblem =
    if (this is ServerResult.Rejected) FeedProblem.REJECTED else FeedProblem.OFFLINE
