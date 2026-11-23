package com.stepup.android.data.remote

import com.stepup.android.domain.PointsPreview

/**
 * 포인트 미리보기(서버 0119 `sneaker_points_preview` — 읽기만). 내 지난 14일 러닝으로 능력치마다 "1포인트 = 하루 약 +N SUP"와
 * 남은 포인트를 나누는 추천을 받는다. 어림값이다 — 화면은 "약"을 붙여 보이고, 못 받으면 그 칸을 숨긴다(포인트 나누기는 그대로 된다).
 * 서버에 0119 가 없으면 PostgREST 가 함수를 못 찾는다(PGRST202 · 404) — [PointsPreviewResult.Missing].
 * 문서: docs/economy-roots-contract-2026-11-21.md 3장
 */
class ShoePointsPreviewApi(private val server: StepUpServer) {

    val isConfigured: Boolean get() = server.isConfigured

    /** [sneakerId] 는 서버의 신발 번호 */
    suspend fun preview(sneakerId: Long): PointsPreviewResult =
        resultOf(
            server.authed { token ->
                server.http.post("${server.restUrl}/rpc/sneaker_points_preview", "{\"p_id\":$sneakerId}", server.headers(token))
            },
        )

    companion object {
        /** 서버의 대답 → 미리보기. `ok` 가 아니거나 읽지 못한 글은 [PointsPreviewResult.None](보일 것이 없다) */
        internal fun resultOf(result: ServerResult<String>): PointsPreviewResult = when (result) {
            is ServerResult.Ok -> PointsPreview.parse(result.value)?.let { PointsPreviewResult.Ok(it) } ?: PointsPreviewResult.None
            is ServerResult.Rejected -> if (missingFunction(result.reason)) PointsPreviewResult.Missing else PointsPreviewResult.None
            is ServerResult.Retry, is ServerResult.SignInRequired -> PointsPreviewResult.Failed
        }

        /** PostgREST 가 서버 함수를 못 찾았다(PGRST202 — 서버에 0119 가 아직 없다) */
        internal fun missingFunction(reason: String): Boolean =
            "Could not find the function" in reason || "PGRST202" in reason
    }
}

sealed interface PointsPreviewResult {
    data class Ok(val preview: PointsPreview) : PointsPreviewResult

    /** 이 신발에는 미리보기가 없다(내 신발이 아님 · 예전 신발 · 서버가 거절) */
    data object None : PointsPreviewResult

    /** 서버에 함수가 없다(0119 전) — 다시 묻지 않아도 된다 */
    data object Missing : PointsPreviewResult

    /** 연결이 끊겼거나 로그인이 필요하다 — 다음에 시트를 열 때 다시 */
    data object Failed : PointsPreviewResult
}
