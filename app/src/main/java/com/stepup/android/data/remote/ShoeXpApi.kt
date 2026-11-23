package com.stepup.android.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

/**
 * 내 신발마다 강화 경험치 통(서버 0118 `my_shoe_xp` — 신발 목록 `my_sneakers` 의 모양을 바꾸지 않으려고 따로 읽는다).
 * 1켤레값 = 1,000점. 만렙이면 `need` · `pct` 가 null. 서버에 0118 이 없으면 [ShoeXpResult.Missing] — 앱은 경험치 줄을 보이지 않는다.
 * 문서: docs/forge-xp-daily-contract-2026-11-19.md
 */
class ShoeXpApi(private val server: StepUpServer) {

    val isConfigured: Boolean get() = server.isConfigured

    suspend fun mine(): ShoeXpResult {
        val result = server.authed { token ->
            server.http.post("${server.restUrl}/rpc/my_shoe_xp", "{}", server.headers(token))
        }.mapBody { parse(it) }
        return when (result) {
            is ServerResult.Ok -> ShoeXpResult.Ok(result.value)
            is ServerResult.Rejected ->
                if (missingFunction(result.reason)) ShoeXpResult.Missing else ShoeXpResult.Failed
            is ServerResult.Retry, is ServerResult.SignInRequired -> ShoeXpResult.Failed
        }
    }

    companion object {
        /** 응답 → 신발 번호마다 통. 같은 번호가 두 번 오면 뒤의 것 */
        internal fun parse(body: String): Map<Long, ShoeXp>? = runCatching {
            serverJson.decodeFromString<List<ShoeXpRow>>(body).associate { row ->
                // 점수는 정수지만 서버가 numeric 으로 보내도(1200.0) 읽는다 — 버려서(통을 부풀리지 않게)
                row.id to ShoeXp(
                    xp = kotlin.math.floor(row.xp).toInt().coerceAtLeast(0),
                    need = row.need?.let { kotlin.math.floor(it).toInt() }?.takeIf { it > 0 },
                )
            }
        }.getOrNull()

        /** PostgREST 가 서버 함수를 못 찾았다(PGRST202 — 서버에 0118 이 아직 없다) */
        internal fun missingFunction(reason: String): Boolean =
            "Could not find the function" in reason || "PGRST202" in reason
    }
}

/** 한 켤레의 통 — 통에 든 점수 · 이 레벨의 통 크기(만렙이면 null) */
data class ShoeXp(val xp: Int, val need: Int?)

sealed interface ShoeXpResult {
    data class Ok(val byShoe: Map<Long, ShoeXp>) : ShoeXpResult

    /** 서버에 함수가 없다(0118 전) — 통을 모른다 */
    data object Missing : ShoeXpResult

    /** 연결이 끊겼거나 읽지 못했다 — 다음 동기화에 다시 */
    data object Failed : ShoeXpResult
}

@Serializable
private data class ShoeXpRow(
    val id: Long,
    val xp: Double = 0.0,
    val need: Double? = null,
)
