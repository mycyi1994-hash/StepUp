package com.stepup.android.data.repo

import com.stepup.android.data.remote.DisciplineRow
import com.stepup.android.data.remote.EventDetailRow
import com.stepup.android.data.remote.EventRow
import com.stepup.android.data.remote.NewsRow
import com.stepup.android.data.remote.RunningFeedApi
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.SourceRow

/** 무엇을 거르고 어떻게 줄 세울지. 화면과 서버가 같은 말을 쓰게 한 묶음. */
data class EventFilter(
    val query: String = "",
    val eventType: String = RunningFeedApi.ALL,
    val distance: String = RunningFeedApi.ALL,
    val region: String = RunningFeedApi.ALL,
    val status: String = RunningFeedApi.ALL,
    val from: String? = null,
    val to: String? = null,
    val sort: String = "DATE",
    val includePast: Boolean = false,
)

data class NewsFilter(
    val query: String = "",
    val category: String = RunningFeedApi.ALL,
    val publisher: String = RunningFeedApi.ALL,
    val sort: String = "RECENT",
)

/**
 * 한 번 받아 온 목록과 받아 온 때.
 *
 * 받아 온 때를 함께 들고 다니는 이유는 화면이 "몇 시 기준"인지 말해야 하기
 * 때문이다. 못 받아 왔을 때 직전 것을 그대로 보여 주면서 시각을 감추면,
 * 어제 마감된 접수가 오늘도 열려 있는 것처럼 보인다.
 */
data class Cached<T>(val value: T, val loadedAtMillis: Long)

/**
 * 대회와 뉴스를 서버에서 가져온다.
 *
 * ── 캐시 ──
 *
 * 같은 조건으로 다시 물으면 짧은 동안은 받아 둔 것을 준다. 탭을 오갈 때마다
 * 서버를 부르면 사용자 수만큼 요청이 늘어나는데, 이 표는 한 시간에 한 번
 * 바뀌므로 그럴 이유가 없다.
 *
 * ── 실패했을 때 ──
 *
 * 직전 정상 데이터를 버리지 않는다. 지하철에서 앱을 열었다고 어제 본 목록까지
 * 사라지면, 고치기 전보다 나빠진다. 대신 "언제 받은 것인지"를 함께 넘긴다.
 */
class RunningFeedRepository(private val api: RunningFeedApi) {

    private val eventCache = HashMap<String, Cached<List<EventRow>>>()
    private val newsCache = HashMap<String, Cached<List<NewsRow>>>()
    private var sourceCache: Cached<List<SourceRow>>? = null

    suspend fun events(
        filter: EventFilter,
        offset: Int = 0,
        force: Boolean = false,
    ): ServerResult<Cached<List<EventRow>>> {
        val key = "$filter|$offset"
        val hit = eventCache[key]
        if (!force && hit != null && fresh(hit)) return ServerResult.Ok(hit)

        val result = api.events(
            query = filter.query,
            eventType = filter.eventType,
            distance = filter.distance,
            region = filter.region,
            status = filter.status,
            from = filter.from,
            to = filter.to,
            sort = filter.sort,
            includePast = filter.includePast,
            offset = offset,
        )
        return when (result) {
            is ServerResult.Ok -> {
                val cached = Cached(result.value, System.currentTimeMillis())
                eventCache[key] = cached
                ServerResult.Ok(cached)
            }
            // 못 받아 왔어도 들고 있던 것이 있으면 그것을 준다.
            else -> hit?.let { ServerResult.Ok(it) } ?: result.carry()
        }
    }

    suspend fun news(
        filter: NewsFilter,
        offset: Int = 0,
        force: Boolean = false,
    ): ServerResult<Cached<List<NewsRow>>> {
        val key = "$filter|$offset"
        val hit = newsCache[key]
        if (!force && hit != null && fresh(hit)) return ServerResult.Ok(hit)

        val result = api.news(
            query = filter.query,
            category = filter.category,
            publisher = filter.publisher,
            sort = filter.sort,
            offset = offset,
        )
        return when (result) {
            is ServerResult.Ok -> {
                val cached = Cached(result.value, System.currentTimeMillis())
                newsCache[key] = cached
                ServerResult.Ok(cached)
            }
            else -> hit?.let { ServerResult.Ok(it) } ?: result.carry()
        }
    }

    suspend fun event(id: String): ServerResult<EventDetailRow> = api.event(id)

    suspend fun disciplines(id: String): ServerResult<List<DisciplineRow>> = api.disciplines(id)

    suspend fun sources(): ServerResult<List<SourceRow>> {
        sourceCache?.takeIf { fresh(it, SOURCE_TTL_MS) }?.let { return ServerResult.Ok(it.value) }
        return when (val result = api.sources()) {
            is ServerResult.Ok -> {
                sourceCache = Cached(result.value, System.currentTimeMillis())
                result
            }
            else -> sourceCache?.let { ServerResult.Ok(it.value) } ?: result
        }
    }

    /**
     * 관심 대회 켜고 끄기.
     *
     * 성공하면 그 조건으로 받아 둔 목록을 버린다. 저장 여부가 목록에 함께
     * 실려 오므로, 버리지 않으면 하트만 눌리고 목록은 예전 값을 그대로 든다.
     */
    suspend fun saveEvent(id: String, on: Boolean): ServerResult<Boolean> =
        api.saveEvent(id, on).also { if (it is ServerResult.Ok) eventCache.clear() }

    suspend fun saveNews(id: String, on: Boolean): ServerResult<Boolean> =
        api.saveNews(id, on).also { if (it is ServerResult.Ok) newsCache.clear() }

    private fun fresh(c: Cached<*>, ttl: Long = LIST_TTL_MS): Boolean =
        System.currentTimeMillis() - c.loadedAtMillis < ttl

    @Suppress("UNCHECKED_CAST")
    private fun <T> ServerResult<*>.carry(): ServerResult<T> = this as ServerResult<T>

    private companion object {
        /** 목록은 10분. 서버 쪽이 한 시간에 한 번 바뀌므로 이보다 잦을 이유가 없다. */
        const val LIST_TTL_MS = 10 * 60 * 1000L

        /** 출처 목록은 거의 안 바뀐다. */
        const val SOURCE_TTL_MS = 60 * 60 * 1000L
    }
}
