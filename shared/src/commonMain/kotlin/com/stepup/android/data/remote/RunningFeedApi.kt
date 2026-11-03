package com.stepup.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

/**
 * 러닝 이벤트(대회)와 러닝·건강 뉴스 — 서버와 말하는 쪽.
 *
 * 앱은 언론사나 대회 사이트를 직접 읽지 않는다. 서버가 한 번 모아 둔 표만
 * 읽는다. 그래야 사이트가 모양을 바꿔도 새 APK 를 내지 않아도 되고, 사용자
 * 수만큼 남의 서버를 두드리지 않으며, 인증 정보가 앱에 들어가지 않는다.
 *
 * 거르기·정렬·쪽나눔은 서버 함수가 다시 검증한다
 * (`supabase/migrations/0009_running_feed.sql`). 여기서 보내는 값은 요청일
 * 뿐이고, 한도를 넘으면 서버가 잘라 준다.
 */
class RunningFeedApi(private val server: StepUpServer) {

    // ── 대회 ────────────────────────────────────────────────────────

    suspend fun events(
        query: String = "",
        eventType: String = ALL,
        distance: String = ALL,
        region: String = ALL,
        status: String = ALL,
        from: String? = null,
        to: String? = null,
        sort: String = "DATE",
        includePast: Boolean = false,
        limit: Int = 20,
        offset: Int = 0,
    ): ServerResult<List<EventRow>> = rpc(
        "list_running_events",
        jsonBody {
            put("p_query", query)
            put("p_event_type", eventType)
            put("p_distance", distance)
            put("p_region", region)
            put("p_status", status)
            from?.let { put("p_from", it) }
            to?.let { put("p_to", it) }
            put("p_sort", sort)
            put("p_include_past", includePast)
            put("p_limit", limit)
            put("p_offset", offset)
        },
    ) { serverJson.decodeFromString<List<EventRow>>(it) }

    suspend fun event(id: String): ServerResult<EventDetailRow> =
        when (
            val result = rpc("get_running_event", jsonBody { put("p_id", id) }) {
                serverJson.decodeFromString<List<EventDetailRow>>(it)
            }
        ) {
            // 행이 없으면 숨겨졌거나 지워진 대회다. "다시 시도"로 보이면 몇 번을 눌러도 같다.
            is ServerResult.Ok ->
                result.value.firstOrNull()?.let { ServerResult.Ok(it) }
                    ?: ServerResult.Rejected("대회를 찾을 수 없습니다")
            is ServerResult.Rejected -> result
            is ServerResult.Retry -> result
            is ServerResult.SignInRequired -> result
        }

    suspend fun disciplines(id: String): ServerResult<List<DisciplineRow>> =
        rpc("event_disciplines_of", jsonBody { put("p_id", id) }) {
            serverJson.decodeFromString<List<DisciplineRow>>(it)
        }

    // ── 뉴스 ────────────────────────────────────────────────────────

    suspend fun news(
        query: String = "",
        category: String = ALL,
        publisher: String = ALL,
        sort: String = "RECENT",
        limit: Int = 20,
        offset: Int = 0,
    ): ServerResult<List<NewsRow>> = rpc(
        "list_running_news",
        jsonBody {
            put("p_query", query)
            put("p_category", category)
            put("p_publisher", publisher)
            put("p_sort", sort)
            put("p_limit", limit)
            put("p_offset", offset)
        },
    ) { serverJson.decodeFromString<List<NewsRow>>(it) }

    /** 출처 목록. "어디서 온 정보인지"는 감출 것이 아니다. */
    suspend fun sources(): ServerResult<List<SourceRow>> =
        server.anonGet(
            "${server.restUrl}/running_sources_public?select=*&order=name"
        ).mapBody { serverJson.decodeFromString<List<SourceRow>>(it) }

    // ── 저장 ────────────────────────────────────────────────────────
    //
    // 켜고 끄기를 한 함수로 둔다. 두 번 눌러도 결과가 같아야 하는데, 넣기와
    // 빼기를 따로 두면 "이미 저장됨" 오류를 앱이 다시 해석해야 한다.

    suspend fun saveEvent(id: String, on: Boolean): ServerResult<Boolean> =
        rpc("save_event", jsonBody { put("p_id", id); put("p_on", on) }) {
            it.trim().equals("true", ignoreCase = true)
        }

    suspend fun saveNews(id: String, on: Boolean): ServerResult<Boolean> =
        rpc("save_news", jsonBody { put("p_id", id); put("p_on", on) }) {
            it.trim().equals("true", ignoreCase = true)
        }

    // ── 공통 ────────────────────────────────────────────────────────

    private suspend fun <T> rpc(
        name: String,
        body: String,
        parse: (String) -> T?,
    ): ServerResult<T> =
        server.openPost("${server.restUrl}/rpc/$name", body).mapBody(parse)

    companion object {
        const val ALL = "ALL"
    }
}

/** 대회 목록 한 줄 */
@Serializable
data class EventRow(
    val id: String,
    val title: String,
    val organizer: String = "",
    val region: String = "",
    val venue: String = "",
    /** 모르면 null. 모른다고 오늘로 치지 않는다. */
    @SerialName("event_date") val eventDate: String? = null,
    @SerialName("event_end_date") val eventEndDate: String? = null,
    /** 출발 시각을 아는가. 모르면 화면도 시각을 적지 않는다. */
    @SerialName("has_start_time") val hasStartTime: Boolean = false,
    @SerialName("date_precision") val datePrecision: String = "DATE",
    @SerialName("event_type") val eventType: String = "OTHER",
    @SerialName("registration_status") val registrationStatus: String = "UNKNOWN",
    @SerialName("registration_close_at") val registrationCloseAt: String? = null,
    @SerialName("fee_min") val feeMin: Double? = null,
    val currency: String = "KRW",
    /** REGISTRATION · OFFICIAL_INFO · SOURCE_ONLY — 버튼 문구를 정한다 */
    @SerialName("destination_type") val destinationType: String = "SOURCE_ONLY",
    @SerialName("target_url") val targetUrl: String? = null,
    @SerialName("last_verified_at") val lastVerifiedAt: String? = null,
    @SerialName("cancelled_or_postponed") val cancelled: String = "NONE",
    val distances: List<String> = emptyList(),
    @SerialName("discipline_names") val disciplineNames: List<String> = emptyList(),
    val saved: Boolean = false,
    @SerialName("total_count") val totalCount: Long = 0,
)

@Serializable
data class EventDetailRow(
    val id: String,
    val title: String,
    val organizer: String = "",
    val region: String = "",
    val venue: String = "",
    @SerialName("event_date") val eventDate: String? = null,
    @SerialName("event_end_date") val eventEndDate: String? = null,
    @SerialName("event_start_time") val eventStartTime: String? = null,
    @SerialName("date_precision") val datePrecision: String = "DATE",
    @SerialName("event_type") val eventType: String = "OTHER",
    @SerialName("registration_status") val registrationStatus: String = "UNKNOWN",
    @SerialName("registration_open_at") val registrationOpenAt: String? = null,
    @SerialName("registration_close_at") val registrationCloseAt: String? = null,
    @SerialName("fee_min") val feeMin: Double? = null,
    val currency: String = "KRW",
    @SerialName("official_url") val officialUrl: String? = null,
    @SerialName("registration_url") val registrationUrl: String? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("destination_type") val destinationType: String = "SOURCE_ONLY",
    @SerialName("source_name") val sourceName: String = "",
    @SerialName("attribution_text") val attributionText: String = "",
    @SerialName("last_verified_at") val lastVerifiedAt: String? = null,
    @SerialName("cancelled_or_postponed") val cancelled: String = "NONE",
    val saved: Boolean = false,
)

/** 종목 한 줄. 원문 이름과 정규화한 거리를 함께 둔다. */
@Serializable
data class DisciplineRow(
    @SerialName("raw_name") val rawName: String,
    @SerialName("distance_key") val distanceKey: String = "UNKNOWN",
    @SerialName("distance_meters") val distanceMeters: Int? = null,
    @SerialName("registration_open_at") val registrationOpenAt: String? = null,
    @SerialName("registration_close_at") val registrationCloseAt: String? = null,
    @SerialName("registration_status") val registrationStatus: String = "UNKNOWN",
    val fee: Double? = null,
)

/** 기사 한 줄 */
@Serializable
data class NewsRow(
    val id: String,
    val title: String,
    @SerialName("publisher_name") val publisherName: String = "",
    @SerialName("publisher_domain") val publisherDomain: String = "",
    @SerialName("original_url") val originalUrl: String,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("fetched_at") val fetchedAt: String? = null,
    /** 검색 설명. 본문을 읽고 쓴 글이 아니다. */
    val description: String = "",
    /** 본문 이용이 허락된 출처에서만 온다. */
    val summary: String? = null,
    /** NONE · SEARCH_DESCRIPTION · AI_SUMMARY */
    @SerialName("summary_type") val summaryType: String = "NONE",
    val category: String = "RUNNING",
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("attribution_text") val attributionText: String = "",
    val saved: Boolean = false,
    @SerialName("total_count") val totalCount: Long = 0,
)

@Serializable
data class SourceRow(
    val id: String,
    val name: String,
    @SerialName("homepage_url") val homepageUrl: String? = null,
    @SerialName("provider_type") val providerType: String = "MANUAL",
    val enabled: Boolean = false,
    @SerialName("can_discover") val canDiscover: Boolean = false,
    @SerialName("can_show_description") val canShowDescription: Boolean = false,
    @SerialName("can_summarize") val canSummarize: Boolean = false,
    @SerialName("can_use_image") val canUseImage: Boolean = false,
    @SerialName("attribution_text") val attributionText: String = "",
    @SerialName("verified_note") val verifiedNote: String = "",
    @SerialName("last_success_at") val lastSuccessAt: String? = null,
    val failing: Boolean = false,
)
