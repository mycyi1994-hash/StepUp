package com.stepup.android.data.remote

import com.stepup.android.domain.RunnerBadges
import com.stepup.android.domain.RunnerLevel

/**
 * 러너 레벨(서버 0121 — 읽기만). 걸음으로 오르는 계정 레벨과 크루 명단 · 대기실의 이름 옆 배지.
 * 서버에 0121 이 없으면 PostgREST 가 함수를 못 찾는다(PGRST202 · 404) — [RunnerMeResult.Missing] · [RunnerBadgesResult.Missing](칸을 숨긴다).
 * 문서: docs/runner-level-contract-2026-11-21.md
 */
class RunnerLevelApi(private val server: StepUpServer) {

    val isConfigured: Boolean get() = server.isConfigured

    /** 내 러너 레벨 — 걸음 · 다음 레벨까지 · 하루 에너지(분) · 배지 · 러너 레벨로 받은 상자 */
    suspend fun me(): RunnerMeResult =
        meResultOf(server.authed { token -> server.http.post("${server.restUrl}/rpc/runner_me", "{}", server.headers(token)) })

    /** [userIds] 가운데 배지(Lv10 이상)가 있는 사람만 — 한 번에 [RunnerBadges.MAX_PER_CALL] 명까지(부르는 쪽이 나눈다) */
    suspend fun badges(userIds: List<String>): RunnerBadgesResult {
        val ids = userIds.filter(RunnerBadges::isUserId).distinct().take(RunnerBadges.MAX_PER_CALL)
        if (ids.isEmpty()) return RunnerBadgesResult.Ok(emptyMap())
        return badgesResultOf(
            server.authed { token -> server.http.post("${server.restUrl}/rpc/runner_badges", badgesBody(ids), server.headers(token)) },
        )
    }

    companion object {
        /** 서버의 대답 → 러너 레벨. 읽지 못한 글은 [RunnerMeResult.Failed](다음에 다시) */
        internal fun meResultOf(result: ServerResult<String>): RunnerMeResult = when (result) {
            is ServerResult.Ok -> RunnerLevel.parse(result.value)?.let { RunnerMeResult.Ok(it) } ?: RunnerMeResult.Failed
            is ServerResult.Rejected -> if (missingFunction(result.reason)) RunnerMeResult.Missing else RunnerMeResult.Failed
            is ServerResult.Retry, is ServerResult.SignInRequired -> RunnerMeResult.Failed
        }

        internal fun badgesResultOf(result: ServerResult<String>): RunnerBadgesResult = when (result) {
            is ServerResult.Ok -> RunnerBadges.parse(result.value)?.let { RunnerBadgesResult.Ok(it) } ?: RunnerBadgesResult.Failed
            is ServerResult.Rejected -> if (missingFunction(result.reason)) RunnerBadgesResult.Missing else RunnerBadgesResult.Failed
            is ServerResult.Retry, is ServerResult.SignInRequired -> RunnerBadgesResult.Failed
        }

        /** `{"p_users":["…","…"]}` — 사용자 id 는 uuid 모양만 담는다(이스케이프할 글자가 없다) */
        internal fun badgesBody(ids: List<String>): String =
            ids.joinToString(",", prefix = "{\"p_users\":[", postfix = "]}") { "\"$it\"" }

        /** PostgREST 가 서버 함수를 못 찾았다(PGRST202 — 서버에 0121 이 아직 없다) */
        internal fun missingFunction(reason: String): Boolean =
            "Could not find the function" in reason || "PGRST202" in reason
    }
}

sealed interface RunnerMeResult {
    /** 받은 값 — [RunnerLevel.enabled] 가 false 면(서버가 러너 레벨을 껐다) 화면은 칸을 숨긴다 */
    data class Ok(val level: RunnerLevel) : RunnerMeResult

    /** 서버에 함수가 없다(0121 전) — 앱이 켜진 동안 다시 묻지 않는다 */
    data object Missing : RunnerMeResult

    /** 연결이 끊겼거나 · 로그인이 필요하거나 · 읽지 못했다 — 다음에 다시 */
    data object Failed : RunnerMeResult
}

sealed interface RunnerBadgesResult {
    /** 사용자 id → 배지(10 · 20 · 30). 배지가 없는 사람은 들어 있지 않다 */
    data class Ok(val badges: Map<String, Int>) : RunnerBadgesResult

    data object Missing : RunnerBadgesResult

    data object Failed : RunnerBadgesResult
}
