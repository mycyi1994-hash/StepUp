package com.stepup.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * 로그인 세션. 서버에 요청할 때 쓰는 출입증이다.
 *
 * [accessToken] 은 한 시간쯤 살고, 만료되면 [refreshToken] 으로 새로 받는다.
 * 둘 다 기기에 저장해야 앱을 껐다 켜도 로그인이 유지된다.
 */
@Serializable
data class AuthSession(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    /** 남은 유효 시간(초) */
    @SerialName("expires_in") val expiresIn: Long = 3600,
    /** 만료 시각(epoch 초). 서버가 안 주면 발급 시각 + expiresIn 으로 채운다. */
    @SerialName("expires_at") val expiresAt: Long = 0,
    val user: AuthUser? = null,
) {
    /**
     * 곧 만료되는가.
     *
     * 딱 만료 시각에 맞춰 갱신하면 요청이 날아가는 중에 만료될 수 있다.
     * 미리 갱신해 그 틈을 없앤다.
     */
    fun needsRefresh(nowSeconds: Long, marginSeconds: Long = 120): Boolean =
        expiresAt <= 0 || nowSeconds >= expiresAt - marginSeconds
}

@Serializable
data class AuthUser(
    val id: String,
    val email: String? = null,
    /** 익명 계정인가. 구글 로그인을 붙이면 false 가 된다. */
    @SerialName("is_anonymous") val isAnonymous: Boolean = false,
)

/** 인증 요청의 결말 */
sealed interface AuthResult {
    data class Ok(val session: AuthSession) : AuthResult

    /** 설정이 틀렸거나 자격이 거절됐다. 다시 시도해도 같다. */
    data class Rejected(val reason: String) : AuthResult

    /** 네트워크나 서버 문제. 나중에 다시. */
    data class Retry(val reason: String) : AuthResult
}

/**
 * Supabase 인증.
 *
 * 라이브러리(supabase-kt)를 쓰지 않는다. 그쪽은 Kotlin 2.3 이상으로 빌드돼
 * 있어 이 프로젝트(2.0.21)의 컴파일러가 읽지 못한다. 툴체인 전체를 올리는
 * 것은 이 단계에서 감당할 위험이 아니고, 필요한 호출은 둘뿐이다.
 *
 *  - 구글 로그인   안드로이드가 준 ID 토큰을 세션으로 바꾼다
 *  - 토큰 갱신     한 시간마다
 *
 * **로그인 수단은 구글 하나다.** 익명 계정을 두지 않는 이유는 신원을 하나로
 * 묶어 두기 위해서다. 지갑(WalletConnect)은 v2 에서 "출금할 곳"으로 들어오지
 * 신원으로 들어오지 않는다 — 지갑이 곧 신원이면 지갑을 잃는 순간 계정도 잃는다.
 */
class SupabaseAuth(
    private val baseUrl: String,
    private val apiKey: String,
    private val http: HttpPoster = UrlConnectionPoster(),
    private val now: () -> Long = { System.currentTimeMillis() / 1000 },
) {

    val isConfigured: Boolean get() = baseUrl.isNotBlank() && apiKey.isNotBlank()

    private val authUrl get() = "${baseUrl.trimEnd('/')}/auth/v1"

    /**
     * 만료된 출입증을 새로 받는다.
     *
     * 거절([AuthResult.Rejected] — 다시 로그인)은 Supabase 가 "이 refresh token 은 죽었다"고 분명히 말할 때만이다
     * ([refreshTokenDead]). 그 밖의 4xx(공유기 · 프록시 · 와이파이 로그인 쪽지, 403 · 404 · 407 · 408, 모르는 오류 코드)는
     * 잠깐의 문제로 보고 나중에 다시 한다 — 예전엔 429 를 뺀 모든 4xx 를 죽은 토큰으로 보아 멀쩡한 사람을 로그아웃시켰다.
     */
    suspend fun refresh(refreshToken: String): AuthResult =
        post(
            "$authUrl/token?grant_type=refresh_token",
            """{"refresh_token":${refreshToken.asJsonString()}}""",
            refreshing = true,
        )

    /**
     * 구글로 로그인한다.
     *
     * 브라우저를 띄우는 방식이 아니라 안드로이드가 준 ID 토큰을 그대로 보내는
     * 방식이다. 앱을 벗어나지 않으므로 로그인 도중에 이탈하는 사람이 적다.
     */
    suspend fun signInWithGoogle(idToken: String, nonce: String? = null): AuthResult {
        val body = buildString {
            append("""{"provider":"google","id_token":""")
            append(idToken.asJsonString())
            if (!nonce.isNullOrBlank()) {
                append(""","nonce":""")
                append(nonce.asJsonString())
            }
            append("}")
        }
        return post("$authUrl/token?grant_type=id_token", body)
    }

    private suspend fun post(url: String, body: String, refreshing: Boolean = false): AuthResult {
        if (!isConfigured) return AuthResult.Retry("서버 주소가 설정되지 않았습니다")

        val response = http.post(
            url = url,
            body = body,
            headers = mapOf(
                "apikey" to apiKey,
                "Content-Type" to "application/json",
            ),
        )

        return when {
            response.status in 200..299 -> {
                val session = runCatching { json.decodeFromString<AuthSession>(response.body) }
                    .getOrNull()
                    ?: return AuthResult.Retry("응답을 이해할 수 없습니다")
                AuthResult.Ok(session.withExpiryFilled(now()))
            }

            response.status == 0 -> AuthResult.Retry(response.body)

            // 출입증 갱신 — 죽은 토큰이라고 분명할 때만 거절. 나머지는 나중에 다시(쉬는 시간은 SessionHolder 가 둔다)
            refreshing ->
                if (refreshTokenDead(response.status, response.body)) AuthResult.Rejected(response.errorMessage())
                else AuthResult.Retry("서버 오류 (${response.status})")

            // 400 은 대개 설정 문제다 — 익명 로그인이 꺼져 있거나 토큰이 틀렸다.
            // 401·403 은 키가 틀렸다. 어느 쪽도 재시도로 풀리지 않는다.
            response.status in 400..499 && response.status != 429 ->
                AuthResult.Rejected(response.errorMessage())

            else -> AuthResult.Retry("서버 오류 (${response.status})")
        }
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}

/**
 * 만료 시각을 **이 폰의 시계로** 적는다.
 *
 * `expires_in`(남은 초)만 오는 경우가 있는데, 그대로 두면 앱이 언제 갱신해야
 * 하는지 알 수 없다. 받은 시점을 기준으로 절대 시각을 만들어 둔다.
 *
 * 서버가 준 `expires_at` 은 서버 시계 기준이라, 폰 시계가 몇 분 늦으면 만료된
 * 토큰을 계속 보내 모든 요청이 401 로 돌아오고, 한 시간 넘게 빠르면 요청마다
 * 갱신한다. 남은 초가 있으면 그것으로 폰 시계 기준 시각을 만든다.
 */
internal fun AuthSession.withExpiryFilled(nowSeconds: Long): AuthSession =
    if (expiresIn > 0) copy(expiresAt = nowSeconds + expiresIn)
    else if (expiresAt > 0) this
    else copy(expiresAt = nowSeconds + 3600)

/** JSON 문자열 리터럴로 감싼다 — 토큰에 따옴표나 역슬래시가 들어가도 깨지지 않게. */
internal fun String.asJsonString(): String =
    Json.encodeToString(String.serializer(), this)

/** Supabase Auth 가 "이 refresh token 으로는 더 갱신할 수 없다"고 말하는 오류 코드 */
private val DEAD_REFRESH_CODES = setOf(
    "refresh_token_not_found",
    "refresh_token_already_used",
    "session_not_found",
    "session_expired",
)

/**
 * 출입증 갱신이 거절된 까닭이 **죽은 refresh token** 인가 — 그때만 다시 로그인하게 한다.
 *
 * 400 · 401 이고, 본문이 Supabase Auth 의 JSON 오류이고, 그 오류가 죽은 토큰을 가리킬 때만 그렇다:
 *  - 오류 코드(`error_code` · 새 판의 글자 `code` · 예전 OAuth 판의 `error`)가 [DEAD_REFRESH_CODES] 중 하나
 *  - `invalid_grant` 이거나 코드가 없는데 글(`msg` · `message` · `error_description`)이 "Invalid Refresh Token"
 *
 * 그 밖은 모두 잠깐의 문제다 — JSON 이 아닌 본문(공유기 · 프록시 · 와이파이 로그인 쪽지), 403 · 404 · 407 · 408 · 5xx,
 * 모르는 오류 코드. 이것들로 로그아웃시키면 연결이 돌아와도 이유 없이 로그인 화면을 다시 본다.
 */
internal fun refreshTokenDead(status: Int, body: String): Boolean {
    if (status != 400 && status != 401) return false
    val obj = runCatching { Json.parseToJsonElement(body) }.getOrNull() as? JsonObject ?: return false
    // 숫자(예전 판의 "code":400 은 HTTP 상태다)는 오류 코드로 읽지 않는다
    fun text(key: String): String? =
        (obj[key] as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
    val code = text("error_code") ?: text("code") ?: text("error")
    val message = listOf("msg", "message", "error_description").firstNotNullOfOrNull(::text).orEmpty()
    return when {
        code == null || code == "invalid_grant" -> "invalid refresh token" in message
        else -> code in DEAD_REFRESH_CODES
    }
}

private fun HttpResponse.errorMessage(): String {
    // Supabase 는 실패 이유를 본문에 담아 보낸다. 흘리면 "왜 안 되는지 모르는" 상태가 된다.
    val obj = runCatching { Json.parseToJsonElement(body) }.getOrNull() as? JsonObject
    val message = listOf("error_description", "msg", "message", "error")
        .firstNotNullOfOrNull { key -> (obj?.get(key) as? JsonPrimitive)?.contentOrNull }
    return message ?: "요청이 거절되었습니다 ($status)"
}
