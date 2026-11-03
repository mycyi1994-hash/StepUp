package com.stepup.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

/**
 * 접속 코드(서버 0079) — 비공개 베타는 코드로만, 공개 베타는 고른 사전 예약자 + 코드.
 * 문 · 권한 · 코드는 모두 서버가 정한다. 앱 화면만 막는 것이 아니라 서버가 권한 없는 요청을 403 으로 막는다.
 */
class AccessApi(private val server: StepUpServer) {

    val isConfigured: Boolean get() = server.isConfigured

    /** 로그인 전에도 — 지금 문(OPEN · CLOSED_BETA · OPEN_BETA) */
    suspend fun mode(): ServerResult<String> =
        server.anonGet("${server.restUrl}/rpc/access_mode").mapBody { body ->
            runCatching { serverJson.decodeFromString<String>(body) }.getOrNull()
        }

    suspend fun status(): ServerResult<AccessStatusRow> =
        rpc("access_status", "{}") { serverJson.decodeFromString<AccessStatusRow>(it) }

    suspend fun redeem(code: String): ServerResult<AccessRedeemRow> =
        rpc("access_redeem", jsonBody { put("p_code", code) }) { serverJson.decodeFromString<AccessRedeemRow>(it) }

    suspend fun issue(): ServerResult<AccessIssueRow> =
        rpc("access_code_issue", "{}") { serverJson.decodeFromString<AccessIssueRow>(it) }

    suspend fun myCodes(): ServerResult<List<AccessCodeRow>> =
        rpc("my_access_codes", "{}") { serverJson.decodeFromString<List<AccessCodeRow>>(it) }

    private suspend fun <T> rpc(name: String, body: String, parse: (String) -> T?): ServerResult<T> =
        server.authed { token ->
            server.http.post("${server.restUrl}/rpc/$name", body, server.headers(token))
        }.mapBody(parse)
}

@Serializable
data class AccessStatusRow(
    val mode: String = "OPEN",
    val granted: Boolean = true,
    val source: String? = null,
    val revoked: Boolean = false,
    @SerialName("can_issue") val canIssue: Boolean = false,
    @SerialName("daily_limit") val dailyLimit: Int = 0,
    @SerialName("issued_today") val issuedToday: Int = 0,
)

@Serializable
data class AccessRedeemRow(
    val ok: Boolean = false,
    val error: String? = null,
)

@Serializable
data class AccessIssueRow(
    val code: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    val error: String? = null,
)

@Serializable
data class AccessCodeRow(
    val code: String,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("used_at") val usedAt: String? = null,
)
