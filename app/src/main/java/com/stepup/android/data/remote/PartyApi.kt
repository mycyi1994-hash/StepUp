package com.stepup.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonNull

/** 파티 방 한 명 — `party_state` 의 members 한 줄 */
@Serializable
data class PartyMemberRow(
    @SerialName("user_id") val userId: String,
    val name: String = "",
    val ready: Boolean = false,
    @SerialName("is_me") val isMe: Boolean = false,
    @SerialName("is_host") val isHost: Boolean = false,
    /** 달리는 동안 보낸 마지막 위치. 로비에서는 null */
    val lat: Double? = null,
    val lng: Double? = null,
    /** 마지막으로 서버에 소식을 보낸 지 몇 초 */
    @SerialName("seen_sec") val seenSec: Int = 0,
    /** 달리는 동안 위치 · 거리를 같이 뛰는 사람에게 보이기로 했는가(0037) */
    val share: Boolean = false,
    /** 앱이 보낸 화면용 거리(km). 공유하지 않은 다른 사람이면 null */
    val km: Double? = null,
)

/** 파티 방의 지금 — `party_state` */
@Serializable
data class PartyStateRow(
    val id: Long,
    /** LOBBY · COUNTDOWN · RUNNING · FINISHED */
    val status: String,
    @SerialName("host_id") val hostId: String = "",
    @SerialName("crew_id") val crewId: String? = null,
    @SerialName("flash_post_id") val flashPostId: Long? = null,
    /** 모두 같이 출발하는 시각. 카운트다운 전에는 null */
    @SerialName("starts_at") val startsAt: String? = null,
    /** 서버의 지금. 폰 시계가 틀려도 카운트다운이 맞게 이것과 견준다. */
    @SerialName("server_now") val serverNow: String,
    val members: List<PartyMemberRow> = emptyList(),
    /** 대기실 이름(0072) — 예전 방(party_open) · 번개러닝은 null */
    val name: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    /** 함께 달릴 코스(0072) — 경로는 판이 바뀔 때만 [PartyApi.course] 로 받는다 */
    @SerialName("course_name") val courseName: String? = null,
    @SerialName("course_km") val courseKm: Double? = null,
    /** 코스 판 — 진행자가 코스를 바꿀 때마다 오른다. 코스 없는 방은 0 */
    @SerialName("course_rev") val courseRev: Int = 0,
    /** 달리는 방식 · 만나는 곳 · 출발 예정 시각 · 방의 정원(0074 — 번개 방은 글에서 받는다, 0076). 예전 서버면 없다 */
    @SerialName("run_mode") val runMode: String? = null,
    @SerialName("meet_place") val meetPlace: String? = null,
    @SerialName("plan_at") val planAt: String? = null,
    val capacity: Int? = null,
)

/** 열린 대기실 하나 — `party_rooms` 의 rooms 한 줄(0072) */
@Serializable
data class PartyRoomRow(
    val id: Long,
    val name: String? = null,
    /** LOBBY · COUNTDOWN · RUNNING(목록에는 열린 방과 내가 들어간 방만 온다) */
    val status: String = "LOBBY",
    @SerialName("host_id") val hostId: String = "",
    @SerialName("host_name") val hostName: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("members_count") val membersCount: Int = 0,
    @SerialName("ready_count") val readyCount: Int = 0,
    val capacity: Int = 20,
    @SerialName("course_name") val courseName: String? = null,
    @SerialName("course_km") val courseKm: Double? = null,
    @SerialName("course_rev") val courseRev: Int = 0,
    @SerialName("course_track") val courseTrack: String? = null,
    /** 내가 들어가 있는 방 */
    val mine: Boolean = false,
    /** 진행자가 나를 내보낸 방 — 다시 들어갈 수 없다 */
    val kicked: Boolean = false,
    /** 달리는 방식(0074) — TOGETHER(만나서) · REMOTE(각자 자리에서). 예전 방은 TOGETHER */
    @SerialName("run_mode") val runMode: String = "TOGETHER",
    /** 만나는 곳(0074, 같이 뛰기만) */
    @SerialName("meet_place") val meetPlace: String? = null,
    /** 출발 예정 시각(0074) — 안내일 뿐, 출발은 진행자가 누른다 */
    @SerialName("plan_at") val planAt: String? = null,
    /** 들어온 사람 이름 앞 넷(0074) — 목록의 머리글자 */
    @SerialName("member_names") val memberNames: List<String> = emptyList(),
)

/** 크루의 열린 대기실 목록 — `party_rooms`(0072) */
@Serializable
data class PartyRoomsRow(
    @SerialName("server_now") val serverNow: String,
    val capacity: Int = 20,
    /** 오늘(한국 날짜) 크루 러닝을 이미 했다 — 어느 크루에서든 */
    @SerialName("done_today") val doneToday: Boolean = false,
    val rooms: List<PartyRoomRow> = emptyList(),
)

/** 방의 코스 — `party_course`(0072). 경로는 "위도,경도;위도,경도" */
@Serializable
data class PartyCourseRow(
    @SerialName("course_rev") val courseRev: Int = 0,
    @SerialName("course_name") val courseName: String? = null,
    @SerialName("course_km") val courseKm: Double? = null,
    @SerialName("course_track") val courseTrack: String? = null,
)

/**
 * 파티런 로비 — 서버와 말을 주고받는 쪽 (`supabase/migrations/0012_course_party.sql`).
 *
 * 방은 크루 하나 또는 번개 글 하나에 붙는다. 같은 크루의 로비를 연 사람들은
 * 같은 방에 모인다. 앱은 몇 초마다 [state] 를 물어 준비·출발·거리를 맞춘다.
 */
class PartyApi(
    private val server: StepUpServer,
    /**
     * 파티 물음을 보내는 쪽 — 오래 기다리지 않는다([TIMEOUT_MS]). 앱 기본(연결 20초 + 읽기 20초)을 기다리면 같이 뛰는 중
     * 끊긴 것을 늦게 알았고, 그동안 다른 사람의 옛 위치 · 인원이 지금 것처럼 보였다. 방 상태는 몇 초마다 다시 묻는다.
     * 서버에 흉내 낸 연결을 넣은 검사는 그것을 그대로 쓴다.
     */
    internal val http: HttpPoster = server.http.let { if (it is UrlConnectionPoster) UrlConnectionPoster(timeoutMillis = TIMEOUT_MS) else it },
) {

    /** 방에 들어간다. 열린 방이 없으면 새로 연다. 방 번호가 돌아온다. */
    suspend fun open(crewId: String?, flashPostId: Long?): ServerResult<Long> = rpc(
        "party_open",
        jsonBody {
            put("p_crew", crewId?.ifBlank { null } ?: JsonNull)
            put("p_post", flashPostId ?: JsonNull)
        },
    ) { it.trim().toLongOrNull() }

    /** 크루의 열린 대기실 목록(0072) — 크루원만. 보기만 하고 들어가지 않는다 */
    suspend fun rooms(crewId: String): ServerResult<PartyRoomsRow> = rpc(
        "party_rooms",
        jsonBody { put("p_crew", crewId) },
    ) { serverJson.decodeFromString<PartyRoomsRow>(it) }

    /**
     * 이름 · 코스를 붙여 새 대기실을 연다(0072) — 연 사람이 진행자. [requestKey] 가 같으면(답을 잃고 다시 보냄) 서버는 그때 연 방을 돌려준다.
     * 코스 칸을 모두 null 로 보내면 코스 없이 연다(0073 — 자유 코스, 코스 판 0).
     */
    suspend fun create(
        crewId: String,
        name: String,
        courseName: String?,
        courseKm: Double?,
        courseTrack: String?,
        requestKey: String,
    ): ServerResult<Long> = rpc(
        "party_create",
        jsonBody {
            put("p_crew", crewId)
            put("p_name", name)
            put("p_course_name", courseName ?: JsonNull)
            put("p_course_km", courseKm ?: JsonNull)
            put("p_course_track", courseTrack ?: JsonNull)
            put("p_request", requestKey)
        },
    ) { it.trim().toLongOrNull() }

    /**
     * 달리는 방식 · 만나는 곳 · 출발 시각 · 정원을 붙여 연다(0074 `party_create_v2`). 나머지는 [create] 와 같다.
     * 원격 방은 만나는 곳을 보내지 않는다(서버도 받지 않는다).
     */
    suspend fun createV2(
        crewId: String,
        name: String,
        courseName: String?,
        courseKm: Double?,
        courseTrack: String?,
        requestKey: String,
        mode: String,
        meetPlace: String?,
        planAt: String?,
        capacity: Int?,
    ): ServerResult<Long> = rpc(
        "party_create_v2",
        jsonBody {
            put("p_crew", crewId)
            put("p_name", name)
            put("p_course_name", courseName ?: JsonNull)
            put("p_course_km", courseKm ?: JsonNull)
            put("p_course_track", courseTrack ?: JsonNull)
            put("p_request", requestKey)
            put("p_mode", mode)
            put("p_meet_place", meetPlace ?: JsonNull)
            put("p_meet_lat", JsonNull)
            put("p_meet_lng", JsonNull)
            put("p_plan_at", planAt ?: JsonNull)
            put("p_capacity", capacity ?: JsonNull)
        },
    ) { it.trim().toLongOrNull() }

    /** 고른 대기실에 들어간다(0072) — 이미 들어가 있으면 그대로 */
    suspend fun join(partyId: Long): ServerResult<Long> = rpc(
        "party_join",
        jsonBody { put("p_party", partyId) },
    ) { it.trim().toLongOrNull() }

    /** 방의 코스 경로(0072) — 방 사람만 */
    suspend fun course(partyId: Long): ServerResult<PartyCourseRow> = rpc(
        "party_course",
        jsonBody { put("p_party", partyId) },
    ) { serverJson.decodeFromString<PartyCourseRow>(it) }

    /**
     * 진행자 — 코스를 바꾼다(0072). 새 코스 판이 돌아온다. 진행자를 뺀 모두의 준비가 풀린다.
     * 모두 null 이면 코스를 뺀다(0073). 지금과 같은 코스면 서버가 판 · 준비를 그대로 두고 지금 판을 돌려준다.
     */
    suspend fun setCourse(partyId: Long, courseName: String?, courseKm: Double?, courseTrack: String?): ServerResult<Int> = rpc(
        "party_set_course",
        jsonBody {
            put("p_party", partyId)
            put("p_course_name", courseName ?: JsonNull)
            put("p_course_km", courseKm ?: JsonNull)
            put("p_course_track", courseTrack ?: JsonNull)
        },
    ) { it.trim().toIntOrNull() }

    suspend fun state(partyId: Long): ServerResult<PartyStateRow> = rpc(
        "party_state",
        jsonBody { put("p_party", partyId) },
    ) { serverJson.decodeFromString<PartyStateRow>(it) }

    suspend fun ready(partyId: Long, ready: Boolean): ServerResult<Unit> = rpc(
        "party_ready",
        jsonBody {
            put("p_party", partyId)
            put("p_ready", ready)
        },
    ) { }

    suspend fun leave(partyId: Long): ServerResult<Unit> =
        rpc("party_leave", jsonBody { put("p_party", partyId) }) { }

    /** 방장 — 로비에서 한 사람을 내보낸다. */
    suspend fun kick(partyId: Long, userId: String): ServerResult<Unit> = rpc(
        "party_kick",
        jsonBody {
            put("p_party", partyId)
            put("p_user", userId)
        },
    ) { }

    /** 방장 — 준비한 사람들끼리 출발. 몇 초 뒤 모두 같이 출발한다. */
    suspend fun start(partyId: Long): ServerResult<Unit> =
        rpc("party_start", jsonBody { put("p_party", partyId) }) { }

    /** 진행자 — 내가 본 코스 판으로 출발(0072). 그사이 코스가 바뀌었으면 서버가 course_changed 로 거절한다 */
    suspend fun startRev(partyId: Long, courseRev: Int): ServerResult<Unit> = rpc(
        "party_start_rev",
        jsonBody {
            put("p_party", partyId)
            put("p_course_rev", courseRev)
        },
    ) { }

    /** 달리는 동안의 위치 보고 */
    suspend fun ping(partyId: Long, lat: Double?, lng: Double?): ServerResult<Unit> = rpc(
        "party_ping",
        jsonBody {
            put("p_party", partyId)
            put("p_lat", lat ?: JsonNull)
            put("p_lng", lng ?: JsonNull)
        },
    ) { }

    /** 달리는 동안 내 위치 · 거리를 같이 뛰는 사람에게 보일지(0037) */
    suspend fun share(partyId: Long, share: Boolean): ServerResult<Unit> = rpc(
        "party_share",
        jsonBody {
            put("p_party", partyId)
            put("p_share", share)
        },
    ) { }

    /** 화면용 거리 보고 — 적립 계산에는 쓰이지 않는다 */
    suspend fun live(partyId: Long, km: Double): ServerResult<Unit> = rpc(
        "party_live",
        jsonBody {
            put("p_party", partyId)
            put("p_km", km)
        },
    ) { }

    /** 방장 — 러닝을 마치고 방을 닫는다. */
    suspend fun finish(partyId: Long): ServerResult<Unit> =
        rpc("party_finish", jsonBody { put("p_party", partyId) }) { }

    private suspend fun <T> rpc(
        name: String,
        body: String,
        parse: (String) -> T?,
    ): ServerResult<T> =
        server.authed { token ->
            http.post("${server.restUrl}/rpc/$name", body, server.headers(token))
        }.mapBody(parse)

    companion object {
        /** 파티 물음 하나를 기다리는 시간(ms) — 연결 · 읽기 각각 */
        const val TIMEOUT_MS = 8_000
    }
}
