package com.stepup.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

/** 코스 한 줄 — `course_feed`. 좋아요 수와 "내가 눌렀는지"가 함께 온다. */
@Serializable
data class CourseRow(
    val id: Long,
    val author: String = "",
    val name: String,
    val area: String = "",
    @SerialName("distance_km") val distanceKm: Double = 0.0,
    @SerialName("elevation_m") val elevationM: Int = 0,
    /** RunCourse.encode 형식 — "위도,경도;위도,경도" */
    val track: String = "",
    @SerialName("run_count") val runCount: Int = 0,
    @SerialName("created_at") val createdAt: String,
    val likes: Int = 0,
    val liked: Boolean = false,
    val mine: Boolean = false,
)

/**
 * 코스 게시판 — 서버와 말을 주고받는 쪽 (`supabase/migrations/0012_course_party.sql`).
 *
 * 코스는 폰에 먼저 있다. 서버에는 "공유"한 코스만 올라가고, 게시판은 그걸
 * 읽는다. 같은 길은 한 사람에 한 번만 올라간다 — 서버가 경로의 지문으로 막는다.
 */
class CourseApi(private val server: StepUpServer) {

    /** 게시판 — 하트 많은 순 */
    suspend fun board(limit: Int = 200): ServerResult<List<CourseRow>> =
        server.authed { token ->
            server.http.get(
                "${server.restUrl}/course_feed?select=*&shared=eq.true" +
                    "&order=likes.desc,created_at.desc&limit=$limit",
                server.headers(token),
            )
        }.mapBody { serverJson.decodeFromString<List<CourseRow>>(it) }

    /** 코스 올리기. 이미 올린 길이면 이름만 고쳐 다시 공개한다. 서버 번호가 돌아온다. */
    suspend fun share(
        name: String,
        area: String,
        distanceKm: Double,
        elevationM: Int,
        track: String,
    ): ServerResult<Long> = rpc(
        "course_share",
        jsonBody {
            put("p_name", name)
            put("p_area", area)
            put("p_distance_km", distanceKm)
            put("p_elevation_m", elevationM)
            put("p_track", track)
        },
    ) { it.trim().toLongOrNull() }

    /** 올린 코스 내리기. 폰에는 그대로 남는다. */
    suspend fun unshare(track: String): ServerResult<Unit> =
        rpc("course_unshare", jsonBody { put("p_track", track) }) { }

    /** 하트를 누르거나 거둔다. 누른 뒤의 상태가 돌아온다. */
    suspend fun toggleLike(courseId: Long): ServerResult<Boolean> =
        rpc("course_toggle_like", jsonBody { put("p_course", courseId) }) {
            it.trim().toBooleanStrictOrNull()
        }

    private suspend fun <T> rpc(
        name: String,
        body: String,
        parse: (String) -> T?,
    ): ServerResult<T> =
        server.authed { token ->
            server.http.post("${server.restUrl}/rpc/$name", body, server.headers(token))
        }.mapBody(parse)
}
