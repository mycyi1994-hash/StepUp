package com.stepup.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

/**
 * 크루 목록 한 줄 — `crew_feed` 뷰가 보는 사람 기준으로 만들어 내려준다.
 *
 * `joined`·`requested`·`owned` 는 같은 크루라도 누가 보느냐에 따라 다르다.
 * `pending_count` 는 크루장에게만 값이 있고 나머지에게는 0 이다.
 */
@Serializable
data class CrewRow(
    val id: String,
    val name: String,
    val monogram: String = "",
    val tagline: String = "",
    val area: String = "",
    @SerialName("join_policy") val joinPolicy: String = "OPEN",
    @SerialName("member_count") val memberCount: Int = 0,
    val roster: List<String> = emptyList(),
    val joined: Boolean = false,
    val requested: Boolean = false,
    @SerialName("pending_count") val pendingCount: Int = 0,
    val owned: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
)

/** 크루장이 보는 가입 신청 한 줄 */
@Serializable
data class CrewRequestRow(
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("requested_at") val requestedAt: String,
)

/**
 * 크루 — 서버와 말을 주고받는 쪽.
 *
 * 쓰는 일은 전부 서버 함수(`supabase/migrations/0010_crew_join.sql`)가 한다.
 * "승인제인지 보고 → 신청을 넣는다"를 앱이 두 번에 나눠 하면, 그 사이에
 * 크루장이 방식을 바꿨을 때 어긋난다.
 */
class CrewApi(private val server: StepUpServer) {

    /** 크루 목록. 사람이 많은 크루가 위로 온다. */
    suspend fun crews(): ServerResult<List<CrewRow>> =
        server.authed { token ->
            server.http.get(
                "${server.restUrl}/crew_feed?select=*" +
                    "&order=member_count.desc,created_at.desc&limit=200",
                server.headers(token),
            )
        }.mapBody { serverJson.decodeFromString<List<CrewRow>>(it) }

    /** 크루를 만들고 새 크루의 id 를 돌려받는다. 만든 사람은 서버가 주인으로 넣는다. */
    suspend fun create(
        name: String,
        monogram: String,
        tagline: String,
        area: String,
        joinPolicy: String,
    ): ServerResult<String> = rpc(
        "crew_create",
        jsonBody {
            put("p_name", name)
            put("p_monogram", monogram)
            put("p_tagline", tagline)
            put("p_area", area)
            put("p_join_policy", joinPolicy)
        },
    ) { serverJson.decodeFromString<String>(it) }

    /** 가입. 자유 가입이면 `JOINED`, 승인제면 `REQUESTED` 가 온다. */
    suspend fun join(crewId: String): ServerResult<String> =
        rpc("crew_join", jsonBody { put("p_crew", crewId) }) {
            serverJson.decodeFromString<String>(it)
        }

    /** 탈퇴. 기다리던 가입 신청이 있으면 그것도 거둔다. */
    suspend fun leave(crewId: String): ServerResult<Unit> =
        rpc("crew_leave", jsonBody { put("p_crew", crewId) }) { }

    /** 가입 방식 바꾸기. 자유 가입으로 열면 받아 준 인원이 돌아온다. */
    suspend fun setJoinPolicy(crewId: String, joinPolicy: String): ServerResult<Int> =
        rpc(
            "crew_set_join_policy",
            jsonBody {
                put("p_crew", crewId)
                put("p_policy", joinPolicy)
            },
        ) { it.trim().toIntOrNull() }

    /** 크루장이 보는 가입 신청 목록. 먼저 신청한 사람이 위. */
    suspend fun requests(crewId: String): ServerResult<List<CrewRequestRow>> =
        rpc("crew_requests", jsonBody { put("p_crew", crewId) }) {
            serverJson.decodeFromString<List<CrewRequestRow>>(it)
        }

    /** 가입 신청 승인·거절. 어느 쪽이든 신청은 사라진다. */
    suspend fun decide(crewId: String, userId: String, approve: Boolean): ServerResult<Unit> =
        rpc(
            "crew_decide",
            jsonBody {
                put("p_crew", crewId)
                put("p_user", userId)
                put("p_approve", approve)
            },
        ) { }

    private suspend fun <T> rpc(
        name: String,
        body: String,
        parse: (String) -> T?,
    ): ServerResult<T> =
        server.authed { token ->
            server.http.post("${server.restUrl}/rpc/$name", body, server.headers(token))
        }.mapBody(parse)
}
