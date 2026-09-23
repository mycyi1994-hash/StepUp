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
)

/**
 * 파티런 로비 — 서버와 말을 주고받는 쪽 (`supabase/migrations/0012_course_party.sql`).
 *
 * 방은 크루 하나 또는 번개 글 하나에 붙는다. 같은 크루의 로비를 연 사람들은
 * 같은 방에 모인다. 앱은 몇 초마다 [state] 를 물어 준비·출발·거리를 맞춘다.
 */
class PartyApi(private val server: StepUpServer) {

    /** 방에 들어간다. 열린 방이 없으면 새로 연다. 방 번호가 돌아온다. */
    suspend fun open(crewId: String?, flashPostId: Long?): ServerResult<Long> = rpc(
        "party_open",
        jsonBody {
            put("p_crew", crewId?.ifBlank { null } ?: JsonNull)
            put("p_post", flashPostId ?: JsonNull)
        },
    ) { it.trim().toLongOrNull() }

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

    /** 달리는 동안의 위치 보고 */
    suspend fun ping(partyId: Long, lat: Double?, lng: Double?): ServerResult<Unit> = rpc(
        "party_ping",
        jsonBody {
            put("p_party", partyId)
            put("p_lat", lat ?: JsonNull)
            put("p_lng", lng ?: JsonNull)
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
            server.http.post("${server.restUrl}/rpc/$name", body, server.headers(token))
        }.mapBody(parse)
}
