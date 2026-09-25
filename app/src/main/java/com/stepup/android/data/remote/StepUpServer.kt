package com.stepup.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * 서버가 세션을 받아 내린 결론.
 *
 * 적립액은 **서버가 계산한 값**이다. 앱이 보낸 금액이 아니다 —
 * `supabase/migrations/0003_ledger.sql` 의 `record_session()` 이 걸음 수를
 * 받아 직접 계산한다. 그래서 앱이 예상한 금액과 다를 수 있고, 다르면
 * 서버 쪽이 맞다.
 */
@Serializable
data class SessionRecorded(
    @SerialName("session_id") val sessionId: Long,
    /** CLEAN · FLAGGED · VOID */
    val verdict: String,
    @SerialName("points_awarded") val pointsAwarded: Double,
    /** 이 세션까지 반영한 잔고 */
    val balance: Double,
)

@Serializable
private data class BalanceRow(val balance: Double)

/**
 * 소식 한 줄.
 *
 * 본문은 없다 — 제목·출처·날짜·원문 링크까지만 담는다. 남의 기사를 옮겨
 * 오지 않고 읽으려면 원문으로 보낸다.
 */
@Serializable
data class NewsItemRow(
    val url: String,
    val title: String,
    val source: String,
    val summary: String = "",
    @SerialName("published_at") val publishedAt: String,
)

/**
 * 순위표 한 줄.
 *
 * `total` 은 받은 줄 수가 아니라 **순위에 오른 전체 인원**이다. 서버는 상위
 * 몇 명과 내 줄만 보내므로, 받은 줄을 세면 "21명 중 47등" 같은 말이 된다.
 */
@Serializable
data class LeaderboardRow(
    val rank: Int,
    @SerialName("user_id") val userId: String,
    val name: String,
    val monogram: String,
    @SerialName("top_speed_kmh") val topSpeedKmh: Double,
    @SerialName("active_sec") val activeSec: Long,
    val sup: Double,
    @SerialName("is_me") val isMe: Boolean,
    val total: Int,
)

/** 종족 순위 한 줄 */
@Serializable
data class FactionRankRow(
    val faction: String,
    val km: Double,
    @SerialName("my_km") val myKm: Double,
    val runners: Int,
)

/** 서버 호출의 결말 */
sealed interface ServerResult<out T> {
    data class Ok<T>(val value: T) : ServerResult<T>

    /** 서버가 거절했다. 다시 보내도 같다. */
    data class Rejected(val reason: String) : ServerResult<Nothing>

    /** 지금은 안 되지만 나중에는 된다. */
    data class Retry(val reason: String) : ServerResult<Nothing>

    /** 사용자가 다시 로그인해야 한다. */
    data class SignInRequired(val reason: String) : ServerResult<Nothing>
}

/**
 * StepUp 서버(Supabase).
 *
 * 부르는 것은 둘뿐이다 — 세션을 기록하고, 잔고를 읽는다. 표에 직접 쓰지
 * 않는 이유는 앱에 박힌 키를 누구나 꺼낼 수 있기 때문이다. 기록은 서버
 * 함수만 할 수 있고, 그 함수가 금액을 직접 정한다.
 */
class StepUpServer(
    private val baseUrl: String,
    private val apiKey: String,
    private val sessions: SessionHolder,
    internal val http: HttpPoster = UrlConnectionPoster(),
) {

    val isConfigured: Boolean get() = baseUrl.isNotBlank() && apiKey.isNotBlank()

    internal val restUrl get() = "${baseUrl.trimEnd('/')}/rest/v1"

    /**
     * 러닝 세션을 서버에 기록한다.
     *
     * 같은 세션을 다시 보내도 안전하다. 서버가 (사용자, 시작시각)으로
     * 중복을 걸러 내고 원래 결과를 그대로 돌려준다 — 지하철에서 응답을
     * 못 받고 재시도하는 일이 흔하기 때문이다.
     */
    suspend fun recordSession(
        startedAtMillis: Long,
        endedAtMillis: Long,
        steps: Int,
        durationSec: Long,
        track: String,
        boostBps: Int,
        partySize: Int,
        faction: String,
        expectedUserId: String,
        mockLocation: Boolean = false,
    ): ServerResult<SessionRecorded> {
        val body = jsonBody {
            put("p_started_at", startedAtMillis.toIsoInstant())
            put("p_ended_at", endedAtMillis.toIsoInstant())
            put("p_steps", steps)
            put("p_duration_sec", durationSec)
            put("p_track", track)
            put("p_boost_bps", boostBps)
            put("p_party_size", partySize)
            put("p_faction", faction)
            put("p_mock_location", mockLocation)
        }

        return authed(expectedUserId) { token ->
            http.post(
                url = "$restUrl/rpc/record_session",
                body = body,
                headers = headers(token),
            )
        }.mapBody { text ->
            // 이 함수는 표를 돌려주므로 배열로 온다. 행이 없으면 뭔가 잘못된 것이다.
            serverJson.decodeFromString<List<SessionRecorded>>(text).firstOrNull()
        }
    }

    /**
     * 개인 순위표.
     *
     * 상위 [limit] 명과 **내 줄**이 함께 온다. 상위만 받으면 300등인 사람은
     * 자기 자리를 영영 모르고, 그러면 순위표는 남의 이야기가 된다.
     *
     * @param board TOP_SPEED · LONGEST_TIME · TOTAL_SUP
     * @param period DAY · WEEK · MONTH · ALL. 그 기간 안의 기록만 센다.
     */
    suspend fun leaderboard(
        board: String,
        limit: Int = 20,
        period: String = "ALL",
    ): ServerResult<List<LeaderboardRow>> {
        val body = jsonBody {
            put("p_board", board)
            put("p_limit", limit)
            put("p_period", period)
        }
        return authed { token ->
            http.post("$restUrl/rpc/leaderboard", body, headers(token))
        }.mapBody { text -> serverJson.decodeFromString<List<LeaderboardRow>>(text) }
    }

    /** 종족 순위. 아무도 안 뛴 종족도 0으로 온다. */
    suspend fun factionLeaderboard(period: String = "ALL"): ServerResult<List<FactionRankRow>> {
        val body = jsonBody { put("p_period", period) }
        return authed { token ->
            http.post("$restUrl/rpc/faction_leaderboard", body, headers(token))
        }.mapBody { text -> serverJson.decodeFromString<List<FactionRankRow>>(text) }
    }

    /** 방금 올린 러닝이 어느 크루의 러닝이었는지 적는다(`session_tag_crew`). */
    suspend fun tagSessionCrew(startedAtMillis: Long, crewId: String, expectedUserId: String): ServerResult<Unit> {
        val body = jsonBody {
            put("p_started_at", startedAtMillis.toIsoInstant())
            put("p_crew", crewId)
        }
        return authed(expectedUserId) { token ->
            http.post("$restUrl/rpc/session_tag_crew", body, headers(token))
        }.mapBody { }
    }

    /**
     * 계정 삭제(`account_delete`). 서버의 프로필·러닝·원장·글·코스·땅 표시가 함께
     * 지워지고 되돌릴 수 없다. 크루장이면 가장 오래된 크루원에게 넘어간다.
     */
    suspend fun deleteAccount(): ServerResult<Unit> =
        authed { token ->
            http.post("$restUrl/rpc/account_delete", "{}", headers(token))
        }.mapBody { }

    /** 지금 잔고. 서버 원장의 합이다. */
    suspend fun balance(): ServerResult<Double> =
        authed { token ->
            http.get(
                url = "$restUrl/sup_balances?select=balance",
                headers = headers(token),
            )
        }.mapBody { text ->
            // 아직 한 번도 적립한 적이 없으면 행이 없다. 그건 오류가 아니라 0이다.
            serverJson.decodeFromString<List<BalanceRow>>(text).firstOrNull()?.balance ?: 0.0
        }

    /**
     * 러닝 소식. 로그인하지 않아도 읽힌다.
     *
     * 이 표는 하루 한 번 수집기가 채우고(.github/workflows/news-refresh.yml)
     * 앱은 읽기만 한다. 쓰기는 RLS 가 막아 두었다.
     */
    suspend fun newsItems(kind: String, limit: Int = 30): ServerResult<List<NewsItemRow>> =
        anonGet(
            "$restUrl/news_items?select=url,title,source,summary,published_at" +
                "&kind=eq.$kind&order=published_at.desc&limit=$limit"
        ).mapBody { serverJson.decodeFromString<List<NewsItemRow>>(it) }

    // ── 공통 ────────────────────────────────────────────────────────

    internal fun headers(token: String) = mapOf(
        "apikey" to apiKey,
        "Authorization" to "Bearer $token",
        "Content-Type" to "application/json",
    )

    /** 출입증을 챙겨서 요청하고, 응답을 결말로 옮긴다. */
    internal suspend fun authed(
        expectedUserId: String? = null,
        call: suspend (String) -> HttpResponse,
    ): ServerResult<String> {
        if (!isConfigured) return ServerResult.Retry("서버 주소가 설정되지 않았습니다")

        val token = when (val t = sessions.accessToken()) {
            is TokenResult.Ok -> {
                // Check the identity attached to this exact token, not an earlier store read.
                if (expectedUserId != null && (expectedUserId.isBlank() || t.userId != expectedUserId)) {
                    return ServerResult.SignInRequired("이 러닝을 시작한 계정으로 로그인해 주세요")
                }
                t.accessToken
            }
            is TokenResult.Unavailable -> return ServerResult.Retry(t.reason)
            is TokenResult.SignInRequired -> return ServerResult.SignInRequired(t.reason)
        }

        val response = call(token)
        return when {
            response.status in 200..299 -> ServerResult.Ok(response.body)
            response.status == 0 -> ServerResult.Retry(response.body)
            // 출입증이 방금 만료됐을 수 있다. 다음 차례에 갱신해서 다시 시도한다.
            response.status == 401 -> {
                sessions.markExpired(token)
                ServerResult.Retry("인증이 만료되었습니다")
            }
            // 429 는 요청이 몰린 것, 5xx 는 서버 문제 — 둘 다 나중에 다시.
            response.status == 429 || response.status >= 500 ->
                ServerResult.Retry("서버가 바쁩니다 (${response.status})")
            else -> ServerResult.Rejected(response.postgrestMessage())
        }
    }

    /**
     * 로그인 없이 읽는다.
     *
     * 소식은 가려 둘 것이 아니고, 로그인을 시켜야만 보인다면 처음 앱을 연
     * 사람에게 빈 탭을 보여 주게 된다. 표 쪽 RLS 가 읽기만 열어 두었다.
     */
    /**
     * 로그인했으면 그 자격으로, 아니면 anon 으로 부른다.
     *
     * 대회·뉴스는 로그인하지 않아도 볼 수 있어야 한다. 다만 로그인해 있으면
     * 저장 여부(관심 대회)가 함께 와야 하므로, 있을 때는 출입증을 쓴다.
     */
    internal suspend fun openPost(url: String, body: String): ServerResult<String> {
        if (!isConfigured) return ServerResult.Retry("서버 주소가 설정되지 않았습니다")
        val token = (sessions.accessToken() as? TokenResult.Ok)?.accessToken
        val response = http.post(
            url,
            body,
            if (token != null) headers(token) else anonHeaders(),
        )
        return response.toResult()
    }

    internal fun anonHeaders() = mapOf(
        "apikey" to apiKey,
        "Authorization" to "Bearer $apiKey",
        "Content-Type" to "application/json",
    )

    internal suspend fun anonGet(url: String): ServerResult<String> {
        if (!isConfigured) return ServerResult.Retry("서버 주소가 설정되지 않았습니다")
        return http.get(url, anonHeaders()).toResult()
    }
}

/** 서버 응답(JSON 글자)을 화면이 쓰는 값으로. 못 읽으면 나중에 다시 한다. */
internal fun <T> ServerResult<String>.mapBody(transform: (String) -> T?): ServerResult<T> =
    when (this) {
        is ServerResult.Ok -> {
            val parsed = runCatching { transform(value) }.getOrNull()
            if (parsed == null) ServerResult.Retry("응답을 이해할 수 없습니다")
            else ServerResult.Ok(parsed)
        }
        is ServerResult.Rejected -> this
        is ServerResult.Retry -> this
        is ServerResult.SignInRequired -> this
    }

internal val serverJson = Json { ignoreUnknownKeys = true }

/** 응답 코드를 결말로. 다시 해 볼 것과 그래 봐야 같은 것을 가른다. */
internal fun HttpResponse.toResult(): ServerResult<String> = when {
    status in 200..299 -> ServerResult.Ok(body)
    status == 0 -> ServerResult.Retry(body)
    status == 401 -> ServerResult.Retry("인증이 만료되었습니다")
    status == 429 || status >= 500 -> ServerResult.Retry("서버가 바쁩니다 ($status)")
    else -> ServerResult.Rejected(postgrestMessage())
}

/**
 * Postgres 가 보낸 실패 이유를 꺼낸다.
 *
 * 함수에서 raise 한 메시지(예: "SUP가 부족합니다")가 여기 들어 있다.
 * 흘리면 사용자에게 "알 수 없는 오류"만 보여주게 된다.
 */
internal fun HttpResponse.postgrestMessage(): String {
    val obj = runCatching { Json.parseToJsonElement(body) }.getOrNull() as? JsonObject
    val message = listOf("message", "hint", "details")
        .firstNotNullOfOrNull { key -> (obj?.get(key) as? JsonPrimitive)?.contentOrNull }
    return message ?: "요청이 거절되었습니다 ($status)"
}

/** epoch 밀리초를 Postgres 가 읽는 ISO-8601 UTC 문자열로. */
internal fun Long.toIsoInstant(): String = java.time.Instant.ofEpochMilli(this).toString()

/** 손으로 JSON 을 만들 때 따옴표·역슬래시로 깨지지 않게. */
internal fun jsonBody(build: MutableMap<String, Any>.() -> Unit): String {
    val map = LinkedHashMap<String, Any>().apply(build)
    return map.entries.joinToString(",", "{", "}") { (key, value) ->
        val encoded = when (value) {
            is String -> value.asJsonString()
            is Number -> value.toString()
            is Boolean -> value.toString()
            // 서버 함수에 "값 없음"을 넘길 때. 문자열 "null" 과는 다르다.
            is JsonNull -> "null"
            else -> value.toString().asJsonString()
        }
        "${key.asJsonString()}:$encoded"
    }
}
