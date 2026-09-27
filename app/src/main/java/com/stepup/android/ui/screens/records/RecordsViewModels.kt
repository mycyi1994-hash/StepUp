package com.stepup.android.ui.screens.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.local.RecordTotals
import com.stepup.android.data.local.RunRecordRow
import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.data.repo.RecordDeletion
import com.stepup.android.data.repo.RunRecordsRepository
import com.stepup.android.data.repo.isUploading
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.RecordPeriod
import com.stepup.android.domain.RunBar
import com.stepup.android.domain.TimeRange
import com.stepup.android.domain.hasNextMonth
import com.stepup.android.domain.hasNextWeek
import com.stepup.android.domain.mondayOf
import com.stepup.android.domain.monthBars
import com.stepup.android.domain.monthForWeek
import com.stepup.android.domain.range
import com.stepup.android.domain.weekBars
import com.stepup.android.domain.weekForMonth
import com.stepup.android.domain.weekRange
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 기록 목록을 읽은 결과(01 · 02 · 07 · 08 · 09 · 10) */
sealed interface RecordsLoad {
    data object Loading : RecordsLoad

    /**
     * @param totals 선택한 기간 전체의 합계(불러온 한 쪽이 아니라)
     * @param anyRecords 이 계정에 기록이 하나라도 있는가 — 없으면 07(첫 기록 전), 있는데 이 기간만 비었으면 08
     * @param more 더 불러올 줄이 있다
     * @param stale 다시 읽기가 실패해 앞서 읽은 것을 보이는 중
     */
    data class Ready(
        val period: RecordPeriod,
        val totals: RecordTotals,
        val rows: List<RunRecordRow>,
        val anyRecords: Boolean,
        val more: Boolean,
        val stale: Boolean = false,
    ) : RecordsLoad

    data object Failed : RecordsLoad
}

/** 목록 — 기간은 이번 달로 시작한다. 목록은 한 쪽(40줄)씩 늘리고, 합계는 기간 전체를 센다 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecordsViewModel(
    private val repo: RunRecordsRepository,
    private val zone: ZoneId = ZoneId.systemDefault(),
    today: LocalDate = LocalDate.now(zone),
) : ViewModel() {
    private val _period = MutableStateFlow<RecordPeriod>(RecordPeriod.Month(YearMonth.from(today)))
    val period: StateFlow<RecordPeriod> = _period.asStateFlow()
    private val limit = MutableStateFlow(PAGE)
    private val attempt = MutableStateFlow(0)
    private var lastReady: RecordsLoad.Ready? = null

    val load: StateFlow<RecordsLoad> = combine(_period, attempt) { p, _ -> p }.flatMapLatest { p ->
        val range = p.range(zone)
        combine(
            repo.totals(range),
            limit.flatMapLatest { n -> repo.rows(range, n).map { it to n } },
            repo.totals(RecordPeriod.All.range(zone)),
        ) { totals, (rows, n), all ->
            RecordsLoad.Ready(p, totals, rows, anyRecords = all.runs > 0, more = rows.size >= n) as RecordsLoad
        }
            .onStart { emit(lastReady?.takeIf { it.period == p } ?: RecordsLoad.Loading) }
            .catch { emit(lastReady?.takeIf { it.period == p }?.copy(stale = true) ?: RecordsLoad.Failed) }
    }
        .map { if (it is RecordsLoad.Ready && !it.stale) it.also { ready -> lastReady = ready } else it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordsLoad.Loading)

    /** 가장 오래된 러닝 — 기간 시트가 거슬러 갈 수 있는 해 */
    val firstRecordAt: StateFlow<Long?> = repo.firstRecordAt().catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 기간을 바꾼다(시트의 "적용" · 빈 기간의 "전체 기록 보기") */
    fun setPeriod(next: RecordPeriod) {
        limit.value = PAGE
        _period.value = next
    }

    fun loadMore() {
        val ready = load.value as? RecordsLoad.Ready ?: return
        if (ready.more && limit.value <= ready.rows.size) limit.value = ready.rows.size + PAGE
    }

    fun reload() {
        attempt.value++
    }

    /** 목록 썸네일 — 줄마다 한 번 읽어 둔다 */
    private val routes = HashMap<Long, List<GeoPoint>>()

    suspend fun route(id: Long): List<GeoPoint> = routes[id] ?: repo.route(id).let { thin(it) }.also { routes[id] = it }

    companion object {
        const val PAGE = 40
        val Factory = viewModelFactory { initializer { RecordsViewModel(ServiceLocator.runRecordsRepository) } }
    }
}

/** 통계 창 — 주(월요일 시작) 또는 달 */
sealed interface StatWindow {
    data class Week(val monday: LocalDate) : StatWindow
    data class Month(val month: YearMonth) : StatWindow
}

/** 통계 화면이 보일 것(04 · 05 · 06 · 15) */
data class StatsUi(
    val window: StatWindow,
    val totals: RecordTotals?,
    val bars: List<RunBar>,
    val canGoNext: Boolean,
    val thisMonth: YearMonth,
    val failed: Boolean = false,
)

/** 통계 — 목록에서 고른 달(전체 기간이면 이번 달)의 월간으로 시작한다 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecordStatsViewModel(
    private val repo: RunRecordsRepository,
    start: YearMonth,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val today: () -> LocalDate = { LocalDate.now(zone) },
) : ViewModel() {
    private val _window = MutableStateFlow<StatWindow>(StatWindow.Month(minOf(start, YearMonth.from(today()))))
    val window: StateFlow<StatWindow> = _window.asStateFlow()
    private val attempt = MutableStateFlow(0)

    val ui: StateFlow<StatsUi?> = combine(_window, attempt) { w, _ -> w }.flatMapLatest { w ->
        val range = w.range()
        combine(repo.totals(range), repo.marks(range)) { totals, marks ->
            val now = today()
            when (w) {
                is StatWindow.Week -> StatsUi(w, totals, weekBars(w.monday, marks, now, zone), hasNextWeek(w.monday, now), YearMonth.from(now))
                is StatWindow.Month -> StatsUi(w, totals, monthBars(w.month, marks, now, zone), hasNextMonth(w.month, now), YearMonth.from(now))
            }
        }.catch { emit(StatsUi(w, null, emptyList(), canGoNext = false, thisMonth = YearMonth.from(today()), failed = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun StatWindow.range(): TimeRange = when (this) {
        is StatWindow.Week -> weekRange(monday, zone)
        is StatWindow.Month -> RecordPeriod.Month(month).range(zone)
    }

    /** 주간 ↔ 월간 — 주간은 그 달의 알맞은 주(이번 달이면 이번 주), 월간은 그 주의 마지막 날(미래면 오늘)의 달 */
    fun showWeekly() {
        val w = _window.value as? StatWindow.Month ?: return
        _window.value = StatWindow.Week(weekForMonth(w.month, today()))
    }

    fun showMonthly() {
        val w = _window.value as? StatWindow.Week ?: return
        _window.value = StatWindow.Month(monthForWeek(w.monday, today()))
    }

    fun previous() {
        _window.value = when (val w = _window.value) {
            is StatWindow.Week -> StatWindow.Week(w.monday.minusWeeks(1))
            is StatWindow.Month -> StatWindow.Month(w.month.minusMonths(1))
        }
    }

    fun next() {
        val now = today()
        _window.value = when (val w = _window.value) {
            is StatWindow.Week -> if (hasNextWeek(w.monday, now)) StatWindow.Week(w.monday.plusWeeks(1)) else w
            is StatWindow.Month -> if (hasNextMonth(w.month, now)) StatWindow.Month(w.month.plusMonths(1)) else w
        }
    }

    fun reload() {
        attempt.value++
    }

    companion object {
        fun factory(start: YearMonth) = viewModelFactory {
            initializer { RecordStatsViewModel(ServiceLocator.runRecordsRepository, start) }
        }
    }
}

/**
 * 지우기 단계(14 · 16) — Confirm · Deleting 은 확인 시트, Failed · Uploading 은 시트를 닫고 화면 아래 한 줄로 알린다
 * (기록은 그대로). 휴지통을 다시 누르면 새로 묻는다.
 */
enum class DeleteState { Closed, Confirm, Deleting, Failed, Uploading }

/** 지난 러닝 상세(11 · 13 · 14 · 16)와 경로 확대(12) */
@OptIn(ExperimentalCoroutinesApi::class)
class RunRecordViewModel(
    private val repo: RunRecordsRepository,
    private val id: Long,
) : ViewModel() {
    /** 지우는 중 · 지운 뒤 — 목록으로 돌아가는 사이에 "찾을 수 없어요"가 비치지 않게 마지막 기록을 둔다 */
    private val leaving = MutableStateFlow(false)
    private var lastFound: RecordLookup.Found? = null

    val record: StateFlow<RecordLookup> = combine(
        repo.record(id).map { if (it == null) RecordLookup.Missing else RecordLookup.Found(it) }
            .catch { emit(RecordLookup.Failed) },
        leaving,
    ) { lookup, away ->
        if (lookup is RecordLookup.Found) lastFound = lookup
        if (away && lookup !is RecordLookup.Found) lastFound ?: lookup else lookup
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordLookup.Loading)

    /** 저장된 경로 — null 은 읽는 중, 빈 목록은 경로 없음(또는 읽을 수 없음) */
    private val _route = MutableStateFlow<List<GeoPoint>?>(null)
    val route: StateFlow<List<GeoPoint>?> = _route.asStateFlow()

    private val _delete = MutableStateFlow(DeleteState.Closed)
    val delete: StateFlow<DeleteState> = _delete.asStateFlow()

    init {
        viewModelScope.launch {
            _route.value = try {
                repo.route(id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    /** 휴지통 — 서버 확인을 기다리는 러닝이면 묻지 않고 아직 지울 수 없다고 알린다 */
    fun askDelete() {
        if (_delete.value == DeleteState.Deleting) return
        val shown = (record.value as? RecordLookup.Found)?.session
        _delete.value = if (shown != null && shown.isUploading()) DeleteState.Uploading else DeleteState.Confirm
    }

    fun closeDelete() {
        if (_delete.value != DeleteState.Deleting) _delete.value = DeleteState.Closed
    }

    /** 실제로 지운 뒤에만 [onDeleted] — 못 지우면 기록을 두고 알린다. 지우는 동안 다시 누를 수 없다 */
    fun confirmDelete(onDeleted: () -> Unit) {
        if (_delete.value == DeleteState.Deleting) return
        _delete.value = DeleteState.Deleting
        leaving.value = true
        viewModelScope.launch {
            val result = try {
                repo.delete(id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            when (result) {
                RecordDeletion.Deleted, RecordDeletion.Missing -> {
                    _delete.value = DeleteState.Closed
                    onDeleted()
                }
                RecordDeletion.Uploading -> {
                    leaving.value = false
                    _delete.value = DeleteState.Uploading
                }
                null -> {
                    leaving.value = false
                    _delete.value = DeleteState.Failed
                }
            }
        }
    }

    companion object {
        fun factory(id: Long) = viewModelFactory { initializer { RunRecordViewModel(ServiceLocator.runRecordsRepository, id) } }
    }
}

sealed interface RecordLookup {
    data object Loading : RecordLookup
    data class Found(val session: WalkSessionEntity) : RecordLookup
    data object Missing : RecordLookup
    data object Failed : RecordLookup
}

/** 썸네일용으로 점을 줄인다 — 모양은 그대로(간격을 고르게 건너뛴다), 첫 점 · 마지막 점은 남긴다 */
internal fun thin(points: List<GeoPoint>, max: Int = 160): List<GeoPoint> {
    if (points.size <= max) return points
    val step = points.size.toDouble() / max
    return (0 until max).map { points[(it * step).toInt()] } + points.last()
}
