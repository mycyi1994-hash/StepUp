package com.stepup.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 주간 리더보드 — 서버 함수(`supabase/migrations/0061_weekly_leaderboards.sql`)만 부른다. 순위 · 앞뒤 · 기간은 서버가 한 번에
 * 같은 집계로 센 값이다(앱은 받은 목록 안에서 순위를 다시 세지 않는다).
 */
class LeaderboardApi(private val server: StepUpServer) {

    /** 개인 러닝 순위 — 이번 주 누적 거리. 상위 [top] 명 · 내 줄 · 바로 앞 · 내 앞뒤 둘 · 이번 주 내 러닝 */
    suspend fun runners(top: Int): ServerResult<RunnerBoardRow> =
        rpc("weekly_runner_board", jsonBody { put("p_top", top) }) { serverJson.decodeFromString<RunnerBoardRow>(it) }

    /** 크루 순위 — 이번 주 크루 전체 거리. 상위 [top] 크루 · 우리 크루 · 그 앞뒤 둘 */
    suspend fun crews(top: Int): ServerResult<CrewBoardRow> =
        rpc("weekly_crew_board", jsonBody { put("p_top", top) }) { serverJson.decodeFromString<CrewBoardRow>(it) }

    private suspend fun <T> rpc(name: String, body: String, parse: (String) -> T?): ServerResult<T> =
        server.authed { token ->
            server.http.post("${server.restUrl}/rpc/$name", body, server.headers(token))
        }.mapBody(parse)
}

@Serializable
data class RunnerRow(
    @SerialName("user_id") val userId: String,
    val name: String = "",
    val monogram: String? = null,
    val rank: Int,
    @SerialName("previous_rank") val previousRank: Int? = null,
    val meters: Double = 0.0,
    val runs: Int = 0,
    @SerialName("is_me") val isMe: Boolean = false,
)

@Serializable
data class LeaderRunRow(
    @SerialName("started_at") val startedAt: String,
    val meters: Double = 0.0,
)

@Serializable
data class RunnerBoardRow(
    @SerialName("week_start") val weekStart: String,
    @SerialName("week_end") val weekEnd: String,
    val timezone: String = "Asia/Seoul",
    @SerialName("as_of") val asOf: String? = null,
    val total: Int = 0,
    val me: RunnerRow? = null,
    val ahead: RunnerRow? = null,
    val nearby: List<RunnerRow> = emptyList(),
    val top: List<RunnerRow> = emptyList(),
    @SerialName("my_name") val myName: String? = null,
    @SerialName("my_meters") val myMeters: Double = 0.0,
    @SerialName("my_runs") val myRuns: Int = 0,
    @SerialName("my_run_list") val myRunList: List<LeaderRunRow> = emptyList(),
)

@Serializable
data class CrewBoardEntryRow(
    @SerialName("crew_id") val crewId: String,
    val name: String = "",
    val monogram: String? = null,
    @SerialName("image_bg") val imageBg: Int = 0,
    @SerialName("image_ver") val imageVer: Int = 0,
    @SerialName("has_image") val hasImage: Boolean = false,
    val rank: Int? = null,
    @SerialName("previous_rank") val previousRank: Int? = null,
    val meters: Double = 0.0,
    val runners: Int = 0,
    @SerialName("is_mine") val isMine: Boolean = false,
)

@Serializable
data class CrewBoardRow(
    @SerialName("week_start") val weekStart: String,
    @SerialName("week_end") val weekEnd: String,
    val timezone: String = "Asia/Seoul",
    @SerialName("as_of") val asOf: String? = null,
    val total: Int = 0,
    val top: List<CrewBoardEntryRow> = emptyList(),
    @SerialName("my_crew") val myCrew: CrewBoardEntryRow? = null,
    val nearby: List<CrewBoardEntryRow> = emptyList(),
)
