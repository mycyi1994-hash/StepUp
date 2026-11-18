package com.stepup.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

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

    /**
     * 내 코드 하나 만들기(0113 `access_code_issue_v2` — 하루 · 지난 7일 한도). 한도 · 권한 거절(23514 · 42501)은 서버가 붙인
     * 갈래(hint)를 [AccessIssueRow.error] 로 옮겨 돌려준다 — 서버 문장은 한국어라 화면은 갈래로 번역된 문구를 고른다.
     * 서버에 0113 이 아직 없으면(PGRST202) 예전 함수(하루 한도만, 거절도 답 안에)로 한 번 더.
     */
    suspend fun issue(): ServerResult<AccessIssueRow> {
        var raw: HttpResponse? = null
        val first = server.authed { token ->
            server.http.post("${server.restUrl}/rpc/access_code_issue_v2", "{}", server.headers(token)).also { raw = it }
        }
        if (first is ServerResult.Rejected) {
            if ("Could not find the function" in first.reason || "PGRST202" in first.reason) {
                return rpc("access_code_issue", "{}") { serverJson.decodeFromString<AccessIssueRow>(it) }
            }
            raw?.let(::issueRefusal)?.let { return ServerResult.Ok(it) }
        }
        return first.mapBody { serverJson.decodeFromString<AccessIssueRow>(it) }
    }

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
    // 0113 — 지난 7일(168시간) 한도. 예전 서버는 보내지 않는다(null — 하루 한도만 있다)
    @SerialName("weekly_limit") val weeklyLimit: Int? = null,
    @SerialName("issued_week") val issuedWeek: Int? = null,
    @SerialName("left_today") val leftToday: Int? = null,
    @SerialName("left_week") val leftWeek: Int? = null,
    /** 막혔을 때 다음 코드를 만들 수 있는 시각(서버 시각 글자). 막히지 않았거나 다시 열리지 않으면 null */
    @SerialName("next_at") val nextAt: String? = null,
    /** 막은 쪽 — DAY(오늘) · WEEK(지난 7일) · null */
    @SerialName("limited_by") val limitedBy: String? = null,
) {
    /** 오늘 더 만들 수 있는 수 — 예전 서버는 하루 한도에서 오늘 만든 수를 뺀다 */
    val codesLeftToday: Int get() = (leftToday ?: (dailyLimit - issuedToday)).coerceAtLeast(0)

    /** 지난 7일 창에서 더 만들 수 있는 수 — 예전 서버(7일 한도 없음)는 null */
    val codesLeftWeek: Int? get() = (leftWeek ?: weeklyLimit?.let { it - (issuedWeek ?: 0) })?.coerceAtLeast(0)

    /** 지금 만들 수 있는 수 — 두 한도 중 적은 쪽 */
    val codesLeft: Int get() = minOf(codesLeftToday, codesLeftWeek ?: Int.MAX_VALUE)
}

@Serializable
data class AccessRedeemRow(
    val ok: Boolean = false,
    val error: String? = null,
)

@Serializable
data class AccessIssueRow(
    val code: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    /** not_allowed · daily_limit · weekly_limit(0113) */
    val error: String? = null,
    /** 거절 때 다음 코드를 만들 수 있는 시각(0113) */
    @SerialName("next_at") val nextAt: String? = null,
)

/** 코드 만들기 거절의 갈래(0113 `access_code_issue_v2` 의 hint) — 이 셋이 아니면 거절을 그대로 둔다 */
private val ISSUE_REFUSALS = setOf("not_allowed", "daily_limit", "weekly_limit")

/** 서버가 23514 · 42501 로 거절한 답에서 갈래(hint) · 다음 시각(details)을 꺼낸다. 모르는 거절이면 null */
internal fun issueRefusal(response: HttpResponse): AccessIssueRow? {
    val obj = runCatching { Json.parseToJsonElement(response.body) }.getOrNull() as? JsonObject ?: return null
    fun text(key: String) = (obj[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
    val hint = text("hint")?.takeIf { it in ISSUE_REFUSALS } ?: return null
    return AccessIssueRow(error = hint, nextAt = text("details"))
}

@Serializable
data class AccessCodeRow(
    val code: String,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("used_at") val usedAt: String? = null,
)
