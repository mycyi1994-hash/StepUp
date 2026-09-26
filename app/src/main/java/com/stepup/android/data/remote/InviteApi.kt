package com.stepup.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

/**
 * 친구 초대(서버 0036). 코드 · 적립액 · 확정은 모두 서버가 정한다.
 * 적립액이 0이면 보상이 정해지지 않은 것이다 — 화면은 보상을 약속하지 않는다.
 */
class InviteApi(private val server: StepUpServer) {

    val isConfigured: Boolean get() = server.isConfigured

    suspend fun status(): ServerResult<InviteStatusRow> =
        rpc("invite_status", "{}") { serverJson.decodeFromString<List<InviteStatusRow>>(it).firstOrNull() }

    suspend fun invitees(): ServerResult<List<InviteeRow>> =
        rpc("invite_list", "{}") { serverJson.decodeFromString<List<InviteeRow>>(it) }

    /** 초대한 사람의 이름을 돌려준다. 막히면 서버가 적은 이유(Rejected)가 온다. */
    suspend fun redeem(code: String): ServerResult<String> =
        rpc("invite_redeem", jsonBody { put("p_code", code) }) { body ->
            runCatching { serverJson.decodeFromString<String>(body) }.getOrNull() ?: body.trim('"')
        }

    private suspend fun <T> rpc(name: String, body: String, parse: (String) -> T?): ServerResult<T> =
        server.authed { token ->
            server.http.post("${server.restUrl}/rpc/$name", body, server.headers(token))
        }.mapBody(parse)
}

@Serializable
data class InviteStatusRow(
    val code: String,
    @SerialName("reward_sup") val rewardSup: Double = 0.0,
    val invited: Int = 0,
    val rewarded: Int = 0,
    @SerialName("can_redeem") val canRedeem: Boolean = false,
    val redeemed: Boolean = false,
)

@Serializable
data class InviteeRow(
    @SerialName("display_name") val displayName: String,
    @SerialName("joined_at") val joinedAt: String,
    @SerialName("rewarded_at") val rewardedAt: String? = null,
)
