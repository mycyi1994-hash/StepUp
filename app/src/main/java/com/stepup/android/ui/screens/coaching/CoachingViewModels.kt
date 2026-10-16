package com.stepup.android.ui.screens.coaching

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.local.RunRecordRow
import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.data.remote.CoachPrefsSave
import com.stepup.android.data.remote.CoachServerSnapshot
import com.stepup.android.data.repo.CoachLoad
import com.stepup.android.data.repo.CoachingRepository
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.RecordingOwner
import com.stepup.android.domain.TimeRange
import com.stepup.android.domain.coaching.CachedCoach
import com.stepup.android.domain.coaching.CoachAi
import com.stepup.android.domain.coaching.CoachGoal
import com.stepup.android.domain.coaching.CoachIds
import com.stepup.android.domain.coaching.CoachPrefs
import com.stepup.android.domain.coaching.CoachResult
import com.stepup.android.domain.coaching.CoachStatus
import com.stepup.android.domain.coaching.CoachingTemplates
import com.stepup.android.domain.coaching.WeekDay
import com.stepup.android.domain.coaching.WeekSummary
import com.stepup.android.domain.coaching.WorkoutTemplate
import com.stepup.android.domain.coaching.coachAiFor
import com.stepup.android.domain.coaching.defaultChangeWeek
import com.stepup.android.domain.coaching.recentWeeks
import com.stepup.android.domain.coaching.weekStrip
import com.stepup.android.domain.coaching.weekSummary
import com.stepup.android.domain.mondayOf
import com.stepup.android.domain.weekRange
import com.stepup.android.ui.screens.records.thin
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * AI 코칭(2026-10-16 인계 16장) 화면의 상태. 숫자 · 그래프 · 주간 띠 · 계획 날짜는 이 휴대폰의 기록과 코드가 만들고,
 * AI 칸은 서버(0060)의 답을 [coachAiFor] 로 줄인 것이다. 묻기(request = false)는 작업을 만들지 않는다 — 화면에 다시
 * 들어오거나 진행 중인 작업을 따라가도 새로 부르지 않는다. 새 작업은 사용자가 누른 "AI 분석 받기" 한 번뿐이다.
 */

/** 이 휴대폰에서 읽은 값 — 읽는 중 · 읽음 · 못 읽음(빈 것과 못 읽은 것을 같은 글자로 만들지 않는다) */
sealed interface Loaded<out T> {
    data object Loading : Loaded<Nothing>
    data class Ready<T>(val value: T) : Loaded<T>
    data object Failed : Loaded<Nothing>
}

/** 서버의 코칭 상태(동의 · 선호 · 계획) — 못 읽은 것 · 로그인 전 · 서버 없는 빌드를 서로 다른 말로 */
sealed interface CoachAccount {
    data object Loading : CoachAccount
    data class Ready(val snapshot: CoachServerSnapshot) : CoachAccount
    data object Offline : CoachAccount
    data object SignIn : CoachAccount
    data object NotConfigured : CoachAccount
    data object Failed : CoachAccount
}

val CoachAccount.snapshot: CoachServerSnapshot? get() = (this as? CoachAccount.Ready)?.snapshot

internal fun CoachLoad<CoachServerSnapshot>.toAccount(): CoachAccount = when (this) {
    is CoachLoad.Ready -> CoachAccount.Ready(value)
    is CoachLoad.Failed -> CoachAccount.Failed
    CoachLoad.Offline -> CoachAccount.Offline
    CoachLoad.SignInRequired -> CoachAccount.SignIn
    CoachLoad.NotConfigured -> CoachAccount.NotConfigured
}

/**
 * 지금 계정의 코칭 상태 — 마지막으로 이 계정에 대해 읽은 결과 위에, 다른 화면(동의 · 목표)이 새로 읽은 서버 상태를 겹친다.
 * 다른 계정에 대해 읽은 결과는 쓰지 않는다.
 */
internal fun accountOf(owner: String?, loaded: Pair<String, CoachAccount>?, latest: Map<String, CoachServerSnapshot>): CoachAccount {
    if (owner == null) return CoachAccount.Loading
    if (RecordingOwner.userId(owner) == null) return CoachAccount.SignIn
    val known = latest[owner]
    val mine = loaded?.takeIf { it.first == owner }?.second
    return when {
        mine == null -> if (known != null) CoachAccount.Ready(known) else CoachAccount.Loading
        mine is CoachAccount.Ready -> CoachAccount.Ready(known ?: mine.snapshot)
        else -> mine
    }
}

/** 서버의 답을 화면의 AI 칸으로 — 못 받은 답은 저장된 사본(있으면) 아니면 실패 · 연결 없음으로 */
internal fun aiOf(
    load: CoachLoad<CoachStatus>,
    cached: CachedCoach?,
    accountRecord: Boolean,
    uploadState: String,
    consent: Boolean?,
): CoachAi = when (load) {
    is CoachLoad.Ready -> coachAiFor(accountRecord, uploadState, online = true, consent = consent, cache = cached, server = load.value, serverFailed = false)
    is CoachLoad.Failed ->
        if (load.rejected && cached == null) CoachAi.Failed(null, retryable = false)
        else coachAiFor(accountRecord, uploadState, online = true, consent = consent, cache = cached, server = null, serverFailed = true)
    CoachLoad.Offline -> coachAiFor(accountRecord, uploadState, online = false, consent = consent, cache = cached, server = null, serverFailed = false)
    CoachLoad.SignInRequired -> CoachAi.NeedsSignIn
    CoachLoad.NotConfigured -> CoachAi.Hidden
}

/** 오늘 카드(01)의 훈련이 어디서 왔는가 — 이름표("오늘의 계획" · "오늘의 추천" · "기본 훈련")가 다르다 */
enum class TodaySource { PLAN, AI, BASIC }

data class TodayWorkout(val template: WorkoutTemplate, val source: TodaySource, val reason: String?, val mock: Boolean)

/**
 * 오늘 카드의 훈련. 사용자가 적용한 이번 주 계획에 오늘이 있으면 그것, 아니면 AI 가 허용 목록에서 고른 추천(오래되지 않은 것),
 * 아니면 고른 시간의 기본 훈련. 목표를 정하지 않았으면 null — 없는 훈련을 만들어 보이지 않는다.
 */
internal fun todayWorkout(snapshot: CoachServerSnapshot?, today: LocalDate, rec: CoachAi): TodayWorkout? {
    val planned = snapshot?.plans?.firstOrNull { it.weekStart == mondayOf(today) }
        ?.days?.firstOrNull { it.first == today }?.second?.let { CoachingTemplates.byId(it) }
    if (planned != null) return TodayWorkout(planned, TodaySource.PLAN, null, false)
    val ready = (rec as? CoachAi.Ready)?.takeIf { !it.stale }
    if (ready != null) {
        val picked = ready.result.candidate?.id
            ?.takeIf { it.startsWith(CoachIds.CAND_TEMPLATE_PREFIX) }
            ?.let { CoachingTemplates.byId(it.removePrefix(CoachIds.CAND_TEMPLATE_PREFIX)) }
        if (picked != null) return TodayWorkout(picked, TodaySource.AI, ready.result.actionReason, ready.result.mock)
    }
    val prefs = snapshot?.prefs ?: return null
    val chosen = CoachPrefs.of(CoachGoal.of(prefs.goal), prefs.runsPerWeek, prefs.minutes) ?: return null
    return TodayWorkout(CoachingTemplates.basic(chosen), TodaySource.BASIC, null, false)
}

/** 고른 목표(세 칸이 모두 있을 때만) */
internal fun CoachServerSnapshot.chosenPrefs(): CoachPrefs? =
    prefs?.let { CoachPrefs.of(CoachGoal.of(it.goal), it.runsPerWeek, it.minutes) }

/** 저장 · 적용의 단계 — 성공은 서버가 받았다고 답한 뒤에만 */
enum class SaveState { Idle, Saving, Saved, Conflict, Failed, Offline }

internal fun CoachLoad<*>.failure(): SaveState = if (this == CoachLoad.Offline) SaveState.Offline else SaveState.Failed

/** 코칭 화면들이 함께 하는 일 — 지금 계정 · 서버 코칭 상태를 읽어 둔다 */
abstract class CoachingBaseViewModel(protected val repo: CoachingRepository) : ViewModel() {
    /** 지금 계정의 기록 주인 — 바뀌면 그 계정 것으로 다시 읽는다 */
    protected val owner: StateFlow<String?> = repo.owner.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val loaded = MutableStateFlow<Pair<String, CoachAccount>?>(null)

    val account: StateFlow<CoachAccount> = combine(owner, loaded, repo.snapshots) { o, l, latest -> accountOf(o, l, latest) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CoachAccount.Loading)

    /** 이 휴대폰이 아는 동의(모르면 null — 서버가 정한다) */
    protected val consent: Flow<Boolean?> = account.map { it.snapshot?.consented }.distinctUntilChanged()

    init {
        viewModelScope.launch { owner.filterNotNull().distinctUntilChanged().collect { reloadAccount(it) } }
    }

    /** 다시 읽기(돌아올 때 · 다시 시도) */
    fun refreshAccount() {
        val o = owner.value ?: return
        viewModelScope.launch { reloadAccount(o) }
    }

    /** 이 계정의 서버 상태를 다시 읽는다 — 읽은 것을 돌려준다([account] 는 한 박자 뒤에 바뀐다) */
    protected suspend fun reloadAccount(o: String): CoachAccount {
        val fresh = if (RecordingOwner.userId(o) == null) CoachAccount.SignIn else guard { repo.loadState(o) }.toAccount()
        loaded.value = o to fresh
        return fresh
    }

    protected suspend fun <T> guard(block: suspend () -> CoachLoad<T>): CoachLoad<T> = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        CoachLoad.Failed("error")
    }

    /** 진행 중인 작업을 따라간다 — 처음 한 번만 [call] 의 첫 호출이 요청 · 재시도일 수 있고, 그 뒤로는 묻기만 한다 */
    protected fun poll(
        cached: CachedCoach?,
        accountRecord: Boolean,
        uploadState: String,
        consent: Boolean?,
        call: suspend (first: Boolean) -> CoachLoad<CoachStatus>,
    ): Flow<CoachAi> = flow {
        var first = true
        var wait = POLL_FIRST_MS
        while (true) {
            val load = guard { call(first) }
            first = false
            val ai = aiOf(load, cached, accountRecord, uploadState, consent)
            emit(ai)
            if (ai !is CoachAi.Working) break
            delay(wait)
            wait = (wait * 2).coerceAtMost(POLL_MAX_MS)
        }
    }

    protected suspend fun cachedOrNull(block: suspend () -> CachedCoach?): CachedCoach? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    companion object {
        const val POLL_FIRST_MS = 3_000L
        const val POLL_MAX_MS = 30_000L
    }
}

/** 오늘 탭(01 · 08) — 이번 주 띠와 가장 최근 러닝 */
data class TodayData(val days: List<WeekDay>, val latest: RunRecordRow?)

/** 변화 탭(04) — 고른 주의 합계와 그 주까지 4주 */
data class ChangeData(val summary: WeekSummary, val recent: List<WeekSummary>)

/** 코칭 탭 셋(오늘 · 분석 · 변화)과 그 시트(기록 고르기 · 목표 페이스 적용 16) */
@OptIn(ExperimentalCoroutinesApi::class)
class CoachingHubViewModel(
    repo: CoachingRepository,
    initialRun: Long?,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val clock: () -> LocalDate = { LocalDate.now(zone) },
) : CoachingBaseViewModel(repo) {
    /** 서버에 보낼 언어 — 화면이 자기 글자의 언어를 알려 준다. 알기 전에는 AI 를 묻지 않는다 */
    private val locale = MutableStateFlow<String?>(null)

    fun setLocale(value: String) {
        locale.value = value
    }

    private fun <T> Flow<T>.loaded(): StateFlow<Loaded<T>> =
        map<T, Loaded<T>> { Loaded.Ready(it) }
            .catch { emit(Loaded.Failed) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Loaded.Loading)

    // ── 오늘(01 · 08) ────────────────────────────────────────────────

    val today: StateFlow<Loaded<TodayData>> = owner.filterNotNull().flatMapLatest { o ->
        val now = clock()
        combine(repo.runs(o, weekRange(mondayOf(now), zone)), repo.rows(o, 1)) { runs, rows ->
            TodayData(weekStrip(now, runs, zone), rows.firstOrNull())
        }
    }.loaded()

    fun todayDate(): LocalDate = clock()

    private val recAsk = MutableStateFlow(0)
    private var recRequested = false

    /** 오늘의 추천(01) — 이번 주의 추천 작업. 목표가 바뀌면(선호 판) 다시 묻는다 */
    val recommendation: StateFlow<CoachAi> = combine(
        owner.filterNotNull(), consent, locale.filterNotNull(), recAsk,
        account.map { it.snapshot?.prefs?.revision }.distinctUntilChanged(),
    ) { o, c, l, ask, revision -> RecInput(o, c, l, ask, revision) }
        .distinctUntilChanged()
        .flatMapLatest { input ->
            flow {
                val monday = mondayOf(clock())
                val accountRecord = RecordingOwner.userId(input.owner) != null
                val cached = cachedOrNull { repo.cachedRecommendation(input.owner, monday, input.locale) }
                val request = recRequested
                recRequested = false
                emit(CoachAi.Loading)
                emitAll(
                    poll(cached, accountRecord, SIGNED, input.consent) { first ->
                        repo.recommendation(input.owner, monday, input.locale, request && first)
                    },
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoachAi.Loading)

    /** "AI 추천 받기" — 사용자가 누른 한 번만 작업을 만든다 */
    fun requestRecommendation() {
        recRequested = true
        recAsk.value++
    }

    // ── 분석(03 · 07 · 09 · 10 · 11 · 12 · 16) ───────────────────────

    /** 고를 수 있는 기록 — 최근 먼저 */
    val rows: StateFlow<Loaded<List<RunRecordRow>>> =
        owner.filterNotNull().flatMapLatest { o -> repo.rows(o, PICK_LIMIT) }.loaded()

    private val picked = MutableStateFlow(initialRun)

    fun pick(id: Long) {
        picked.value = id
    }

    /** 고른 기록이 사라졌다(지움) — 가장 최근 기록으로 */
    fun pickLatest() {
        picked.value = null
    }

    /** 보이는 기록 — 고른 것이 없으면 가장 최근 것. 다른 계정의 기록은 저장소가 돌려주지 않는다(null) */
    val record: StateFlow<Loaded<WalkSessionEntity?>> = combine(owner.filterNotNull(), picked, rows) { o, p, r ->
        o to (p ?: (r as? Loaded.Ready)?.value?.firstOrNull()?.id)
    }
        .distinctUntilChanged()
        .flatMapLatest { (o, id) -> if (id == null) flowOf(null) else repo.record(o, id) }
        .loaded()

    private val runAsk = MutableStateFlow(0)
    private var runRequest: Long? = null
    private var runRetry: Long? = null

    /** 그 기록의 AI 칸 — 기록이 바뀌면(서버 확인 · 정정) 다시 판단한다 */
    val runAi: StateFlow<CoachAi> = combine(owner.filterNotNull(), record, consent, locale.filterNotNull(), runAsk) { o, r, c, l, ask ->
        RunInput(o, (r as? Loaded.Ready)?.value, c, l, ask)
    }
        .distinctUntilChanged()
        .flatMapLatest { input -> runAiFlow(input) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoachAi.Loading)

    private fun runAiFlow(input: RunInput): Flow<CoachAi> = flow {
        val shown = input.record
        if (shown == null) {
            emit(CoachAi.Hidden)
            return@flow
        }
        val accountRecord = RecordingOwner.userId(shown.recordingOwner) != null
        val cached = cachedOrNull { repo.cachedRun(input.owner, shown.startedAt, input.locale) }
        // 보낼 수 없는 기록(계정 기록이 아님 · 동의 없음 · 서버 확인 전 · 거절)은 서버에 묻지 않는다
        val local = coachAiFor(accountRecord, shown.uploadState, online = true, consent = input.consent, cache = cached, server = null, serverFailed = false)
        if (local !is CoachAi.Loading) {
            emit(local)
            return@flow
        }
        emit(CoachAi.Loading)
        val request = runRequest == shown.startedAt
        if (request) runRequest = null
        val retry = runRetry
        runRetry = null
        emitAll(
            poll(cached, accountRecord, shown.uploadState, input.consent) { first ->
                if (first && retry != null) repo.retry(input.owner, retry)
                else repo.runStatus(input.owner, shown, input.locale, request && first)
            },
        )
    }

    /** "AI 분석 받기" · "다시 분석" — 보이는 기록에 대해 한 번 */
    fun requestRun() {
        val shown = (record.value as? Loaded.Ready)?.value ?: return
        runRequest = shown.startedAt
        runAsk.value++
    }

    /** "분석 다시 시도"(11) — 같은 작업을 다시. 작업 번호를 모르면 상태만 다시 묻는다 */
    fun retryRun(jobId: Long?) {
        runRetry = jobId
        runAsk.value++
    }

    /** 목록 썸네일과 같은 저장 경로(계정 · 기록별로 한 번 읽어 둔다) */
    private val routes = HashMap<Pair<String, Long>, List<GeoPoint>>()

    fun cachedRoute(id: Long): List<GeoPoint>? = owner.value?.let { routes[it to id] }

    suspend fun route(id: Long): List<GeoPoint> {
        val o = owner.value ?: return emptyList()
        routes[o to id]?.let { return it }
        val shown = try {
            withContext(Dispatchers.Default) { thin(repo.route(o, id)) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return emptyList()
        }
        routes[o to id] = shown
        return shown
    }

    private val _pace = MutableStateFlow(SaveState.Idle)

    /** 목표 페이스 적용(16)의 단계 */
    val pace: StateFlow<SaveState> = _pace.asStateFlow()

    /** 고른 제안의 페이스를 목표로 — 러닝을 시작하지 않는다. 서버가 받았다고 답한 뒤에만 성공 */
    fun applyPace(resultId: Long) {
        if (_pace.value == SaveState.Saving) return
        val o = owner.value ?: return
        val revision = account.value.snapshot?.prefs?.revision ?: 0L
        _pace.value = SaveState.Saving
        viewModelScope.launch {
            val load = guard { repo.applyPace(o, resultId, revision) }
            _pace.value = when {
                load is CoachLoad.Ready && load.value.ok -> SaveState.Saved
                load is CoachLoad.Ready && load.value.conflict -> SaveState.Conflict
                else -> load.failure()
            }
            reloadAccount(o)
        }
    }

    fun closePace() {
        if (_pace.value != SaveState.Saving) _pace.value = SaveState.Idle
    }

    // ── 변화(04) ─────────────────────────────────────────────────────

    private val changeWeek = MutableStateFlow(defaultChangeWeek(clock()))

    /** 보이는 주(월요일) — 처음은 끝난 지난주 */
    val week: StateFlow<LocalDate> = changeWeek.asStateFlow()

    fun thisMonday(): LocalDate = mondayOf(clock())

    fun previousWeek() {
        changeWeek.value = changeWeek.value.minusWeeks(1)
    }

    /** 다음 주로 — 이번 주보다 앞으로는 가지 않는다(오지 않은 주를 0회로 그리지 않는다) */
    fun nextWeek() {
        val next = changeWeek.value.plusWeeks(1)
        if (!next.isAfter(thisMonday())) changeWeek.value = next
    }

    val change: StateFlow<Loaded<ChangeData>> = combine(owner.filterNotNull(), changeWeek) { o, w -> o to w }
        .flatMapLatest { (o, w) ->
            val now = clock()
            val range = TimeRange(weekRange(w.minusWeeks(RECENT_WEEKS - 1L), zone).from, weekRange(w, zone).until)
            repo.runs(o, range).map { runs -> ChangeData(weekSummary(w, runs, now, zone), recentWeeks(w, RECENT_WEEKS, runs, now, zone)) }
        }
        .loaded()

    private val weekAsk = MutableStateFlow(0)
    private var weekRequest: LocalDate? = null
    private var weekRetry: Long? = null

    /** 고른 주의 AI 정리 — 진행 중인 주는 서버가 건너뛴다(week_in_progress) */
    val weekAi: StateFlow<CoachAi> = combine(owner.filterNotNull(), changeWeek, consent, locale.filterNotNull(), weekAsk) { o, w, c, l, ask ->
        WeekInput(o, w, c, l, ask)
    }
        .distinctUntilChanged()
        .flatMapLatest { input ->
            flow {
                val accountRecord = RecordingOwner.userId(input.owner) != null
                val cached = cachedOrNull { repo.cachedWeek(input.owner, input.monday, input.locale) }
                val request = weekRequest == input.monday
                if (request) weekRequest = null
                val retry = weekRetry
                weekRetry = null
                emit(CoachAi.Loading)
                emitAll(
                    poll(cached, accountRecord, SIGNED, input.consent) { first ->
                        if (first && retry != null) repo.retry(input.owner, retry)
                        else repo.weekStatus(input.owner, input.monday, input.locale, request && first)
                    },
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoachAi.Loading)

    fun requestWeek() {
        weekRequest = changeWeek.value
        weekAsk.value++
    }

    fun retryWeek(jobId: Long?) {
        weekRetry = jobId
        weekAsk.value++
    }

    init {
        // 계정이 바뀌면 앞 계정에서 고른 기록 번호를 버린다(그 계정의 가장 최근 기록부터)
        viewModelScope.launch { owner.filterNotNull().distinctUntilChanged().drop(1).collect { picked.value = null } }
    }

    private data class RecInput(val owner: String, val consent: Boolean?, val locale: String, val ask: Int, val revision: Long?)

    private data class RunInput(val owner: String, val record: WalkSessionEntity?, val consent: Boolean?, val locale: String, val ask: Int)

    private data class WeekInput(val owner: String, val monday: LocalDate, val consent: Boolean?, val locale: String, val ask: Int)

    companion object {
        /** 분석 탭에서 고를 수 있는 기록 수 */
        const val PICK_LIMIT = 60
        const val RECENT_WEEKS = 4

        /** 서버 확인을 따지지 않는 칸(주간 · 추천)이 [coachAiFor] 에 넘기는 값 */
        private const val SIGNED = "SIGNED"

        fun factory(initialRun: Long?) = viewModelFactory {
            initializer { CoachingHubViewModel(ServiceLocator.coachingRepository, initialRun) }
        }
    }
}

/** 동의(05) · 목표(06) · 계획(13) · 설정(14 · 15) · 훈련(02) — 서버 상태를 바꾸는 화면들. AI 를 부르지 않는다 */
class CoachingSetupViewModel(
    repo: CoachingRepository,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val clock: () -> LocalDate = { LocalDate.now(zone) },
) : CoachingBaseViewModel(repo) {
    private val _save = MutableStateFlow(SaveState.Idle)
    val save: StateFlow<SaveState> = _save.asStateFlow()

    /** 해제(15)를 마쳤다 — 지운 분석 수(지우지 않았으면 null) */
    private val _withdrawn = MutableStateFlow<Int?>(null)
    val withdrawn: StateFlow<Int?> = _withdrawn.asStateFlow()
    private val _withdrawDone = MutableStateFlow(false)
    val withdrawDone: StateFlow<Boolean> = _withdrawDone.asStateFlow()

    fun clearSave() {
        if (_save.value != SaveState.Saving) _save.value = SaveState.Idle
    }

    /** 동의(05) — 지금 판의 문구에. 코칭이 꺼져 있거나 공급자가 정해지지 않았으면 누를 수 없다 */
    fun grantConsent(onGranted: (hasGoal: Boolean) -> Unit) {
        val o = owner.value ?: return
        val snap = account.value.snapshot ?: return
        if (!snap.enabled || !providerKnown(snap.provider) || _save.value == SaveState.Saving) return
        _save.value = SaveState.Saving
        viewModelScope.launch {
            val load = guard { repo.grantConsent(o, snap.consentVersion) }
            if (load is CoachLoad.Ready) {
                val fresh = reloadAccount(o)
                _save.value = SaveState.Idle
                onGranted(fresh.snapshot?.chosenPrefs() != null)
            } else {
                _save.value = load.failure()
            }
        }
    }

    /** 목표(06) — 저장만 한다. 러닝을 시작하지 않는다. 실패하면 서버의 기존 목표가 그대로다 */
    fun savePrefs(goal: CoachGoal, runsPerWeek: Int, minutes: Int, onSaved: () -> Unit) {
        val o = owner.value ?: return
        val snap = account.value.snapshot ?: return
        if (_save.value == SaveState.Saving) return
        val prefs = snap.prefs
        _save.value = SaveState.Saving
        viewModelScope.launch {
            val load = guard {
                repo.savePrefs(
                    o, goal.key, runsPerWeek, minutes,
                    autoAnalysis = prefs?.autoAnalysis ?: false,
                    weeklyReport = prefs?.weeklyReport ?: false,
                    revision = prefs?.revision ?: 0L,
                )
            }
            afterPrefs(o, load, onSaved)
        }
    }

    /** 자동 분석 · 주간 리포트(14) — 다른 칸(목표)은 지금 서버 값 그대로 */
    fun setToggles(autoAnalysis: Boolean, weeklyReport: Boolean) {
        val o = owner.value ?: return
        val snap = account.value.snapshot ?: return
        if (_save.value == SaveState.Saving) return
        val prefs = snap.prefs
        _save.value = SaveState.Saving
        viewModelScope.launch {
            val load = guard {
                repo.savePrefs(o, prefs?.goal, prefs?.runsPerWeek, prefs?.minutes, autoAnalysis, weeklyReport, prefs?.revision ?: 0L)
            }
            afterPrefs(o, load) {}
        }
    }

    private suspend fun afterPrefs(o: String, load: CoachLoad<CoachPrefsSave>, onSaved: () -> Unit) {
        when {
            load is CoachLoad.Ready && load.value.ok -> {
                reloadAccount(o)
                _save.value = SaveState.Saved
                onSaved()
            }
            load is CoachLoad.Ready && load.value.conflict -> {
                // 다른 기기가 먼저 바꿨다 — 서버 값을 다시 읽어 보인다(내 선택은 저장되지 않았다)
                reloadAccount(o)
                _save.value = SaveState.Conflict
            }
            else -> _save.value = load.failure()
        }
    }

    /** 해제(15) — 새 분석 · 자동 분석을 멈춘다. [deleteResults] 면 저장된 분석과 이 휴대폰의 사본도 지운다 */
    fun withdraw(deleteResults: Boolean, onDone: () -> Unit) {
        val o = owner.value ?: return
        if (_save.value == SaveState.Saving) return
        _save.value = SaveState.Saving
        viewModelScope.launch {
            val load = guard { repo.withdraw(o, deleteResults) }
            if (load is CoachLoad.Ready) {
                reloadAccount(o)
                _withdrawn.value = if (deleteResults) load.value else null
                _withdrawDone.value = true
                _save.value = SaveState.Idle
                onDone()
            } else {
                _save.value = load.failure()
            }
        }
    }

    fun clearWithdrawn() {
        _withdrawDone.value = false
        _withdrawn.value = null
    }

    /** 다음 주 계획(13)의 월요일 */
    fun nextMonday(): LocalDate = mondayOf(clock()).plusWeeks(1)

    /** 계획 적용(13) — 서버가 지금 목표로 다시 만들어 적는다. 러닝을 시작하지 않는다 */
    fun applyPlan() {
        val o = owner.value ?: return
        val snap = account.value.snapshot ?: return
        val prefs = snap.prefs ?: return
        if (_save.value == SaveState.Saving) return
        _save.value = SaveState.Saving
        val monday = nextMonday()
        viewModelScope.launch {
            val load = guard { repo.applyPlan(o, monday, prefs.revision) }
            _save.value = when {
                load is CoachLoad.Ready && load.value.plan != null -> SaveState.Saved
                load is CoachLoad.Ready && load.value.conflict -> SaveState.Conflict
                else -> load.failure()
            }
            reloadAccount(o)
        }
    }

    private val _reason = MutableStateFlow<CoachResult?>(null)

    /** 저장된 AI 이유(02 훈련 · 13 계획) — 이 휴대폰의 사본만 읽는다(이 화면들은 AI 를 부르지 않는다) */
    val reason: StateFlow<CoachResult?> = _reason.asStateFlow()

    /** 02 — 이번 주 추천이 이 훈련을 골랐으면 그 이유 */
    fun loadWorkoutReason(templateId: String, locale: String) {
        viewModelScope.launch {
            val o = owner.filterNotNull().first()
            val result = cachedOrNull { repo.cachedRecommendation(o, mondayOf(clock()), locale) }?.result
            _reason.value = result?.takeIf { it.candidateId == CoachIds.CAND_TEMPLATE_PREFIX + templateId }
        }
    }

    /** 13 — 끝난 지난주 정리가 다음 주 계획을 골랐으면 그 이유 */
    fun loadPlanReason(locale: String) {
        viewModelScope.launch {
            val o = owner.filterNotNull().first()
            val result = cachedOrNull { repo.cachedWeek(o, mondayOf(clock()).minusWeeks(1), locale) }?.result
            _reason.value = result?.takeIf { it.candidateId == CoachIds.CAND_PLAN_NEXT_WEEK }
        }
    }

    companion object {
        val Factory = viewModelFactory { initializer { CoachingSetupViewModel(ServiceLocator.coachingRepository) } }
    }
}

/** 동의 문구에 이름을 쓸 수 있는 공급자 — 정해지지 않았으면(none) 동의를 받지 않는다 */
internal fun providerKnown(provider: String): Boolean = provider == "anthropic" || provider == "mock"

/**
 * 화면에 보일 AI 칸 — 운영자가 코칭을 꺼 두었거나(coaching_enabled=false) 공급자가 정해지지 않았으면 동의를 받을 수 없다
 * (동의 화면은 "준비 중"). 그동안 "동의 필요" · "AI 추천 받기" 같은 칸을 숨긴다 — 서버는 동의를 먼저 보아(no_consent) 꺼진 것을
 * 알리지 않으므로 화면이 가린다. 전에 받아 둔 결과(Ready)는 그대로 보인다. 서버 상태를 모르면(아직 · 오프라인) 그대로 둔다.
 */
internal fun shownAi(ai: CoachAi, snapshot: CoachServerSnapshot?): CoachAi =
    if (snapshot != null && !(snapshot.enabled && providerKnown(snapshot.provider)) && ai !is CoachAi.Ready) CoachAi.Hidden else ai
