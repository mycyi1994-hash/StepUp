package com.stepup.android.data.repo

import com.stepup.android.data.local.RunRecordRow
import com.stepup.android.data.local.UploadState
import com.stepup.android.data.local.WalkSessionDao
import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.data.remote.CoachPlanApply
import com.stepup.android.data.remote.CoachPrefsSave
import com.stepup.android.data.remote.CoachServerSnapshot
import com.stepup.android.data.remote.CoachingApi
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.RecordingOwner
import com.stepup.android.domain.RunTrack
import com.stepup.android.domain.TimeRange
import com.stepup.android.domain.coaching.CachedCoach
import com.stepup.android.domain.coaching.CoachRun
import com.stepup.android.domain.coaching.CoachStatus
import com.stepup.android.domain.coaching.LapCodec
import com.stepup.android.domain.coaching.RunSplits
import com.stepup.android.domain.coaching.runSplits
import com.stepup.android.domain.toGeoPoints
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** 서버에 물어본 결과 — 연결 없음 · 다시 로그인 · 서버 없음 · 실패를 서로 다른 말로(모르는 것을 없는 것으로 만들지 않는다) */
sealed interface CoachLoad<out T> {
    data class Ready<T>(val value: T) : CoachLoad<T>

    /** 연결이 없다 — 저장된 사본만 보인다 */
    data object Offline : CoachLoad<Nothing>

    /** 계정 기록이 아니거나 다시 로그인해야 한다 — 외부로 보내지 않는다 */
    data object SignInRequired : CoachLoad<Nothing>

    /** 서버 주소가 없는 빌드(로컬 개발) */
    data object NotConfigured : CoachLoad<Nothing>

    /** 서버가 답하지 못했다(다시 시도) · 거절했다 */
    data class Failed(val reason: String, val rejected: Boolean = false) : CoachLoad<Nothing>
}

/**
 * AI 코칭(2026-10-16) — 이 휴대폰의 기록(지금 계정의 것만)과 서버 코칭(0060)을 잇는다.
 *
 * · 모든 서버 호출은 그 계정([RecordingOwner.userId])의 토큰으로만 한다. 부를 때의 계정을 결과와 함께 돌려주므로, 그 사이
 *   계정이 바뀌었으면 화면이 버린다(이전 계정의 응답을 새 계정 화면에 붙이지 않는다).
 * · 러닝은 시작 시각으로 가리킨다. 서버에 보내는 숫자는 랩(누적 활성 초)뿐이고, 서버가 자기 러닝과 맞대어 본다.
 * · AI 를 부르는 것(작업 만들기)은 [request] 가 true 일 때만 — 묻기만 하는 호출은 작업을 만들지 않는다.
 * · 코칭이 실패해도 러닝 저장 · 정산 · 올리기와 상관없다(그 경로에 끼어들지 않는다).
 */
class CoachingRepository(
    private val dao: WalkSessionDao,
    private val api: CoachingApi,
    private val cache: CoachingCacheStore,
    /** 지금 로그인한 계정의 기록 주인(account:… · guest · legacy) */
    val owner: Flow<String>,
    private val online: () -> Boolean,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    private val _snapshots = MutableStateFlow<Map<String, CoachServerSnapshot>>(emptyMap())

    /** 계정별로 마지막에 받은 서버 상태(동의 · 선호 · 계획) — 화면들이 함께 본다 */
    val snapshots: StateFlow<Map<String, CoachServerSnapshot>> = _snapshots.asStateFlow()

    /** 이번 실행에서 서버가 이미 받은 랩(계정 · 시작 시각) — 같은 랩을 거듭 보내지 않는다 */
    private val splitsSent = mutableSetOf<Pair<String, Long>>()
    private val splitsMutex = Mutex()

    // ── 이 휴대폰의 기록(지금 계정만) ─────────────────────────────────

    fun runs(owner: String, range: TimeRange): Flow<List<CoachRun>> = dao.observeCoachRuns(owner, range.from, range.until)

    fun rows(owner: String, limit: Int): Flow<List<RunRecordRow>> = dao.observeCoachRows(owner, limit)

    fun record(owner: String, id: Long): Flow<WalkSessionEntity?> = dao.observeCoachRecord(owner, id)

    /** 경로 썸네일 — 그 계정의 기록일 때만(긴 러닝은 점이 수천 개라 화면 스레드 밖에서) */
    suspend fun route(owner: String, id: Long): List<GeoPoint> {
        val record = dao.observeCoachRecord(owner, id).first() ?: return emptyList()
        return withContext(Dispatchers.Default) { RunTrack.decode(record.track).toGeoPoints() }
    }

    fun zoneId(): String = zone().id

    // ── 서버 상태 · 동의 · 선호 ──────────────────────────────────────

    suspend fun loadState(owner: String): CoachLoad<CoachServerSnapshot> = call(owner) { user ->
        api.state(user, zone().id).also { if (it is ServerResult.Ok) _snapshots.update { map -> map + (owner to it.value) } }
    }

    suspend fun grantConsent(owner: String, version: String): CoachLoad<Unit> = call(owner) { user -> api.grantConsent(user, version) }

    /** 해제(15) — [deleteResults] 면 서버의 저장된 분석과 이 휴대폰의 사본도 지운다 */
    suspend fun withdraw(owner: String, deleteResults: Boolean): CoachLoad<Int> {
        val result = call(owner) { user -> api.withdraw(user, deleteResults) }
        if (result is CoachLoad.Ready && deleteResults) cache.clear(owner)
        return result
    }

    suspend fun savePrefs(
        owner: String,
        goal: String?,
        runsPerWeek: Int?,
        minutes: Int?,
        autoAnalysis: Boolean,
        weeklyReport: Boolean,
        revision: Long,
    ): CoachLoad<CoachPrefsSave> = call(owner) { user ->
        api.savePrefs(user, goal, runsPerWeek, minutes, autoAnalysis, weeklyReport, revision)
    }

    suspend fun applyPace(owner: String, resultId: Long, revision: Long): CoachLoad<CoachPrefsSave> =
        call(owner) { user -> api.applyPace(user, resultId, revision) }

    suspend fun applyPlan(owner: String, monday: LocalDate, revision: Long): CoachLoad<CoachPlanApply> =
        call(owner) { user -> api.applyPlan(user, monday, zone().id, revision) }

    // ── 코칭 결과 ────────────────────────────────────────────────────

    /**
     * 기록 하나의 코칭(03 · 07 · 09 · 11 · 12). 서버 확인(SIGNED) 전이면 부르지 않는다. 구간을 보일 수 있는 기록이면
     * 서버에 랩을 먼저 맞대어 보게 한다(없으면 서버가 "구간 없음"으로 건너뛴다). 결과가 오면 사본을 남긴다.
     */
    suspend fun runStatus(owner: String, record: WalkSessionEntity, locale: String, request: Boolean): CoachLoad<CoachStatus> {
        if (record.recordingOwner != owner) return CoachLoad.SignInRequired
        if (record.uploadState != UploadState.SIGNED.name) return CoachLoad.Failed("not_signed", rejected = true)
        return call(owner) { user ->
            sendSplits(owner, user, record)?.let { return@call it }
            api.run(user, record.startedAt, locale, request)
        }.also { cacheResult(owner, CoachingCacheStore.runKey(record.startedAt, locale), it) }
    }

    suspend fun weekStatus(owner: String, monday: LocalDate, locale: String, request: Boolean): CoachLoad<CoachStatus> {
        val zoneId = zone().id
        return call(owner) { user -> api.week(user, monday, zoneId, locale, request) }
            .also { cacheResult(owner, CoachingCacheStore.weekKey(monday, zoneId, locale), it) }
    }

    suspend fun recommendation(owner: String, monday: LocalDate, locale: String, request: Boolean): CoachLoad<CoachStatus> =
        call(owner) { user -> api.recommend(user, zone().id, locale, request) }
            .also { cacheResult(owner, CoachingCacheStore.recKey(monday, locale), it) }

    suspend fun retry(owner: String, jobId: Long): CoachLoad<CoachStatus> = call(owner) { user -> api.retry(user, jobId) }

    suspend fun cachedRun(owner: String, startedAt: Long, locale: String): CachedCoach? =
        cache.get(owner, CoachingCacheStore.runKey(startedAt, locale))

    suspend fun cachedWeek(owner: String, monday: LocalDate, locale: String): CachedCoach? =
        cache.get(owner, CoachingCacheStore.weekKey(monday, zone().id, locale))

    suspend fun cachedRecommendation(owner: String, monday: LocalDate, locale: String): CachedCoach? =
        cache.get(owner, CoachingCacheStore.recKey(monday, locale))

    /** 로컬 기록을 지웠다 — 그 기록의 사본을 지운다(서버의 분석은 그대로다 — 서버에 러닝 삭제 계약이 없다) */
    suspend fun forgetRun(record: WalkSessionEntity) = cache.removeRun(record.recordingOwner, record.startedAt)

    /**
     * 자동 분석(14 "러닝 후 자동 분석") — 서버가 러닝을 확인한 뒤 올리기 일꾼이 부른다. 동의 · 자동 분석이 모두 켜져 있을 때만,
     * 최근 사흘의 서버 확인된 러닝을 요청한다(같은 러닝은 서버가 같은 작업으로 돌려준다). 실패해도 올리기에 영향이 없다.
     */
    suspend fun autoAnalyzeRecent(owner: String, locale: String) {
        val user = RecordingOwner.userId(owner) ?: return
        if (!api.isConfigured || !online()) return
        val state = (api.state(user, zone().id) as? ServerResult.Ok)?.value ?: return
        _snapshots.update { it + (owner to state) }
        if (!state.enabled || !state.consented || state.prefs?.autoAnalysis != true) return
        dao.recentSignedWithLaps(owner, now() - AUTO_WINDOW_MS, AUTO_LIMIT).forEach { record ->
            runStatus(owner, record, locale, request = true)
        }
    }

    // ── 안쪽 ─────────────────────────────────────────────────────────

    /** 랩을 서버에 맞대어 보게 한다 — 보일 수 있는 구간이 없으면 보내지 않는다. 막혔으면 그 답을 돌려준다(null 이면 계속) */
    private suspend fun sendSplits(owner: String, user: String, record: WalkSessionEntity): ServerResult<CoachStatus>? {
        if (runSplits(record.distanceMeters, record.durationSec, record.laps) !is RunSplits.Ready) return null
        val laps = LapCodec.decode(record.laps)?.map { it.splitSec } ?: return null
        val key = owner to record.startedAt
        if (splitsMutex.withLock { key in splitsSent }) return null
        return when (val sent = api.putSplits(user, record.startedAt, laps)) {
            is ServerResult.Ok -> {
                // stored · exists 면 받았다. 그 밖(mismatch · conflict …)은 서버가 이 구간을 믿지 않는다 — 상태를 물으면 "구간 없음"이다
                splitsMutex.withLock { splitsSent += key }
                null
            }
            is ServerResult.SignInRequired -> sent
            is ServerResult.Retry -> sent
            is ServerResult.Rejected -> sent
        }
    }

    private suspend fun cacheResult(owner: String, key: String, load: CoachLoad<CoachStatus>) {
        val result = (load as? CoachLoad.Ready)?.value?.result ?: return
        // 같은 결과를 이미 남겼으면 저장 시각을 바꾸지 않는다(언제 받아 둔 사본인지가 화면에 보인다)
        if (cache.get(owner, key)?.result == result) return
        cache.put(owner, key, result, now())
    }

    private suspend fun <T> call(owner: String, block: suspend (String) -> ServerResult<T>): CoachLoad<T> {
        val user = RecordingOwner.userId(owner) ?: return CoachLoad.SignInRequired
        if (!api.isConfigured) return CoachLoad.NotConfigured
        if (!online()) return CoachLoad.Offline
        return when (val result = block(user)) {
            is ServerResult.Ok -> CoachLoad.Ready(result.value)
            is ServerResult.SignInRequired -> CoachLoad.SignInRequired
            is ServerResult.Retry -> if (!online()) CoachLoad.Offline else CoachLoad.Failed(result.reason)
            is ServerResult.Rejected -> CoachLoad.Failed(result.reason, rejected = true)
        }
    }

    private companion object {
        const val AUTO_WINDOW_MS = 3L * 24 * 60 * 60 * 1000
        const val AUTO_LIMIT = 5
    }
}

/** 화면 글자의 언어 → 서버가 받는 언어(ko · en · ja · zh) */
fun coachLocale(language: String): String = when (language.lowercase()) {
    "ko", "ja", "zh" -> language.lowercase()
    else -> "en"
}
