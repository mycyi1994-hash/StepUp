package com.stepup.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

/** 지도에 보이는 칸 하나와 그 주인 — `territory_view` */
@Serializable
data class TerritoryCell(
    val cell: String,
    val lat: Double,
    val lng: Double,
    @SerialName("crew_id") val crewId: String,
    @SerialName("crew_name") val crewName: String = "",
    /** 최근 14일 동안 주인 크루가 이 칸에 남긴 표시 수 */
    val score: Int = 0,
    /** 내가 속한 크루의 칸인가 */
    val mine: Boolean = false,
)

/** 크루별 차지한 칸 수 — `territory_board` */
@Serializable
data class TerritoryStanding(
    @SerialName("crew_id") val crewId: String,
    @SerialName("crew_name") val crewName: String = "",
    val cells: Int = 0,
    val mine: Boolean = false,
)

/**
 * 땅따먹기 — 서버와 말을 주고받는 쪽 (`supabase/migrations/0020_territory.sql`).
 *
 * 앱은 칸을 칠하지 않는다. 러닝이 서버에 올라가면 서버가 경로를 보고 칠한다.
 * 여기서는 지금 보고 있는 지도 범위의 칸과 크루 순위를 읽기만 한다.
 */
class TerritoryApi(private val server: StepUpServer) {

    suspend fun view(
        minLat: Double,
        minLng: Double,
        maxLat: Double,
        maxLng: Double,
    ): ServerResult<List<TerritoryCell>> = server.authed { token ->
        server.http.post(
            "${server.restUrl}/rpc/territory_view",
            jsonBody {
                put("p_min_lat", minLat)
                put("p_min_lng", minLng)
                put("p_max_lat", maxLat)
                put("p_max_lng", maxLng)
            },
            server.headers(token),
        )
    }.mapBody { serverJson.decodeFromString<List<TerritoryCell>>(it) }

    suspend fun board(limit: Int = 20): ServerResult<List<TerritoryStanding>> = server.authed { token ->
        server.http.post(
            "${server.restUrl}/rpc/territory_board",
            jsonBody { put("p_limit", limit) },
            server.headers(token),
        )
    }.mapBody { serverJson.decodeFromString<List<TerritoryStanding>>(it) }
}
