package com.stepup.android.data.remote

/**
 * 오늘의 보상(서버 0118) — `box_status` 를 받은 글 그대로 읽는다.
 *
 * 예전 읽기(`EconomyApi.boxStatus` → BoxStatus)는 상자 받는 곳의 칸만 남기고 0118 이 더한 칸(reward_steps_today · goal · ticks ·
 * streak · surge · recent)을 버린다. 한 번 받은 글로 상자 현황과 오늘의 보상([com.stepup.android.domain.DailyRewards])을 함께 읽고
 * 그 글을 폰에 남긴다(ShoeRulesRepository) — 같은 상태를 두 번 묻지 않는다.
 */
class DailyRewardsApi(private val server: StepUpServer) {

    val isConfigured: Boolean get() = server.isConfigured

    /** 상자 현황 · 오늘의 보상 — 응답 글 그대로. 비었으면 다시 묻는다 */
    suspend fun boxStatusRaw(): ServerResult<String> =
        server.authed { token -> server.http.post("${server.restUrl}/rpc/box_status", "{}", server.headers(token)) }
            .mapBody { body -> body.takeIf { it.isNotBlank() } }
}
