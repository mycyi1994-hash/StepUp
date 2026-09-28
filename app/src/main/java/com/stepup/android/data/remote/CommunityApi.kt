package com.stepup.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonNull

/**
 * 게시글 한 줄 — `post_feed` 뷰가 보는 사람 기준으로 만들어 내려준다.
 *
 * 좋아요·댓글·참가 수와 "내가 눌렀는지"가 한 줄에 다 있다. 앱이 글마다 따로
 * 물으면 목록 한 번에 요청이 수십 개가 된다.
 */
@Serializable
data class PostRow(
    val id: Long,
    val category: String,
    @SerialName("crew_id") val crewId: String? = null,
    @SerialName("author_id") val authorId: String = "",
    val author: String = "",
    val title: String,
    val body: String = "",
    val place: String = "",
    /** 번개 모임 장소. 없으면 null — 좌표 없이 쓴 번개 */
    val lat: Double? = null,
    val lng: Double? = null,
    /** 번개러닝에서 함께 달릴 거리(km) */
    @SerialName("distance_km") val distanceKm: Double = 0.0,
    @SerialName("meet_at") val meetAt: String? = null,
    val capacity: Int = 0,
    @SerialName("created_at") val createdAt: String,
    val likes: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("joined_count") val joinedCount: Int = 0,
    val liked: Boolean = false,
    val joined: Boolean = false,
    val mine: Boolean = false,
    /** 동네 이야기 장소의 주소(0041). 예전 서버 · 장소 없는 글은 빈 문자열 */
    @SerialName("place_address") val placeAddress: String = "",
    /** 글에 붙인 러닝(0046) — 서버 기록에서 옮겨 적은 값. 첨부가 없거나 예전 서버면 null */
    @SerialName("run_started_at") val runStartedAt: String? = null,
    @SerialName("run_ended_at") val runEndedAt: String? = null,
    @SerialName("run_distance_m") val runDistanceM: Int? = null,
    @SerialName("run_duration_s") val runDurationS: Int? = null,
    /** 코스 그림 "위도,경도;…" — 경로 없는 러닝이면 빈 문자열 */
    @SerialName("run_route") val runRoute: String? = null,
)

/** 글쓰기에 붙일 수 있는 러닝 한 줄 — `story_runs` 의 runs */
@Serializable
data class StoryRunRow(
    val id: Long,
    @SerialName("started_at") val startedAt: String,
    @SerialName("ended_at") val endedAt: String,
    @SerialName("distance_m") val distanceM: Int = 0,
    @SerialName("duration_s") val durationS: Int = 0,
    /** 끝난 날(한국 날짜, yyyy-MM-dd) */
    val day: String,
    val route: String = "",
)

/** 글쓰기 기록 칸(0046 `story_runs`) — 오늘(한국 날짜) · 전체 완료 수 · 마지막 완료 시각 · 붙일 수 있는 러닝 */
@Serializable
data class StoryRunsRow(
    val today: String,
    val total: Int = 0,
    val voided: Int = 0,
    @SerialName("last_ended_at") val lastEndedAt: String? = null,
    val runs: List<StoryRunRow> = emptyList(),
)

/** 댓글 한 줄 — `comment_feed`. 최상위 댓글은 parent_id 가 0 이다. */
@Serializable
data class CommentRow(
    val id: Long,
    @SerialName("post_id") val postId: Long,
    @SerialName("parent_id") val parentId: Long = 0,
    @SerialName("author_id") val authorId: String = "",
    val author: String = "",
    val body: String,
    @SerialName("created_at") val createdAt: String,
    val mine: Boolean = false,
)

/** 번개 참가자 한 줄 — `flash_roster`. 먼저 들어온 순서. */
@Serializable
data class RosterRow(
    @SerialName("user_id") val userId: String,
    val name: String = "",
    @SerialName("is_host") val isHost: Boolean = false,
    @SerialName("is_me") val isMe: Boolean = false,
)

/**
 * 게시판 — 서버와 말을 주고받는 쪽.
 *
 * 읽기는 뷰에서, 쓰기는 서버 함수(`supabase/migrations/0011_board.sql`)로 한다.
 * 도배 제한과 "번개를 쓰면 쓴 사람이 첫 참가자"는 서버가 지킨다.
 */
class CommunityApi(private val server: StepUpServer) {

    /**
     * 내가 볼 수 있는 글 — 전체 게시판과 내가 들어간 크루의 게시판.
     * 크루 글을 걸러 내는 일은 서버 규칙(RLS)이 한다.
     */
    suspend fun posts(limit: Int = 200): ServerResult<List<PostRow>> =
        server.authed { token ->
            server.http.get(
                "${server.restUrl}/post_feed?select=*&order=created_at.desc&limit=$limit",
                server.headers(token),
            )
        }.mapBody { serverJson.decodeFromString<List<PostRow>>(it) }

    /**
     * 이 글을 지금 볼 수 있는가 — 지워졌거나 볼 수 없는 크루 글이면 서버(RLS)가 빈 목록을 준다.
     * 알림이 가리키는 글이 없어졌는지, 연결이 안 된 것인지 가르는 데 쓴다.
     */
    suspend fun postVisible(postId: Long): ServerResult<Boolean> =
        server.authed { token ->
            server.http.get(
                "${server.restUrl}/post_feed?select=id&id=eq.$postId",
                server.headers(token),
            )
        }.mapBody { serverJson.decodeFromString<List<PostIdRow>>(it).isNotEmpty() }

    /** 한 글의 댓글. 먼저 단 것이 위. */
    suspend fun comments(postId: Long): ServerResult<List<CommentRow>> =
        server.authed { token ->
            server.http.get(
                "${server.restUrl}/comment_feed?select=*&post_id=eq.$postId&order=created_at.asc",
                server.headers(token),
            )
        }.mapBody { serverJson.decodeFromString<List<CommentRow>>(it) }

    /** 번개 참가자 명단. 주최자가 첫 줄이다. */
    suspend fun roster(postId: Long): ServerResult<List<RosterRow>> =
        server.authed { token ->
            server.http.get(
                "${server.restUrl}/flash_roster?select=user_id,name,is_host,is_me" +
                    "&post_id=eq.$postId&order=is_host.desc,joined_at.asc",
                server.headers(token),
            )
        }.mapBody { serverJson.decodeFromString<List<RosterRow>>(it) }

    /**
     * 글쓰기. 새 글 번호를 돌려받는다.
     *
     * @param crewId 비어 있으면 전체 게시판
     * @param meetAtIso 번개러닝의 모임 시각(ISO-8601). 번개가 아니면 비워 둔다.
     * @param lat 번개 모임 장소. 모르면 null — 거리는 읽는 사람이 이 좌표에서 잰다.
     */
    suspend fun createPost(
        category: String,
        crewId: String,
        title: String,
        body: String,
        place: String,
        distanceKm: Double,
        meetAtIso: String,
        capacity: Int,
        lat: Double? = null,
        lng: Double? = null,
    ): ServerResult<Long> = rpc(
        "post_create",
        jsonBody {
            put("p_category", category)
            put("p_crew", crewId.ifBlank { null } ?: JsonNull)
            put("p_title", title)
            put("p_body", body)
            put("p_place", place)
            put("p_distance_km", distanceKm)
            put("p_meet_at", meetAtIso.ifBlank { null } ?: JsonNull)
            put("p_capacity", capacity)
            put("p_lat", lat ?: JsonNull)
            put("p_lng", lng ?: JsonNull)
        },
    ) { it.trim().toLongOrNull() }

    /**
     * 동네 이야기 쓰기(0041 story_create, 0046 러닝 첨부) — 장소가 있는 전체 게시판 자유 글. 새 글 번호를 돌려받는다.
     * [body] 는 제목 바로 뒤의 글자부터다(앞 줄바꿈 포함). 서버가 지우지 않는다.
     *
     * @param runId 붙일 러닝(서버 번호). 서버가 내 것 · 3일 안인지 다시 보고, 거리 · 시간은 서버 기록에서 옮긴다
     * @param clientKey 글쓰기마다 만든 요청 키 — 응답을 못 받고 다시 올려도 같은 글 번호가 온다
     */
    suspend fun createStory(
        title: String,
        body: String,
        place: String,
        placeAddress: String,
        lat: Double,
        lng: Double,
        runId: Long? = null,
        clientKey: String? = null,
    ): ServerResult<Long> {
        val result = rpc("story_create", storyBody(title, body, place, placeAddress, lat, lng) {
            if (runId != null) put("p_run", runId)
            if (!clientKey.isNullOrBlank()) put("p_client_key", clientKey)
        }) { it.trim().toLongOrNull() }
        // 서버에 0046 이 아직 없다 — 러닝 없는 글은 예전 모양으로 한 번 더(요청 키 없이) 보낸다
        if (runId == null && !clientKey.isNullOrBlank() && result.isMissingFunction()) {
            return rpc("story_create", storyBody(title, body, place, placeAddress, lat, lng) { }) { it.trim().toLongOrNull() }
        }
        return result
    }

    /**
     * 내 동네 이야기 고치기(0041 story_update) — 같은 글 번호라 댓글 · 좋아요가 그대로 남는다.
     * [runChange] 가 false 면 붙어 있던 러닝을 그대로 둔다(예전 서버와 같은 모양으로 보낸다). true 면 [runId] 로
     * 바꾸거나(null 이면 뺀다).
     */
    suspend fun updateStory(
        postId: Long,
        title: String,
        body: String,
        place: String,
        placeAddress: String,
        lat: Double,
        lng: Double,
        runChange: Boolean = false,
        runId: Long? = null,
    ): ServerResult<Unit> = rpc(
        "story_update",
        storyBody(title, body, place, placeAddress, lat, lng) {
            put("p_post", postId)
            if (runChange) {
                put("p_run", runId ?: JsonNull)
                put("p_run_change", true)
            }
        },
    ) { }

    /** 글쓰기 기록 칸(0046 story_runs) */
    suspend fun storyRuns(): ServerResult<StoryRunsRow> =
        rpc("story_runs", "{}") { serverJson.decodeFromString<StoryRunsRow>(it) }

    private fun storyBody(
        title: String,
        body: String,
        place: String,
        placeAddress: String,
        lat: Double,
        lng: Double,
        extra: MutableMap<String, Any>.() -> Unit,
    ): String = jsonBody {
        extra()
        put("p_title", title)
        put("p_body", body)
        put("p_place", place)
        put("p_place_address", placeAddress)
        put("p_lat", lat)
        put("p_lng", lng)
    }

    suspend fun deletePost(postId: Long): ServerResult<Unit> =
        rpc("post_delete", jsonBody { put("p_post", postId) }) { }

    /** 좋아요를 누르거나 거둔다. 누른 뒤의 상태가 돌아온다. */
    suspend fun toggleLike(postId: Long): ServerResult<Boolean> =
        rpc("post_toggle_like", jsonBody { put("p_post", postId) }) { it.trim().toBooleanStrictOrNull() }

    /** 번개러닝 참가. 참가 뒤의 인원이 돌아온다. 정원이 차면 거절된다. */
    suspend fun joinFlash(postId: Long): ServerResult<Int> =
        rpc("join_flash", jsonBody { put("p_post_id", postId) }) { it.trim().toIntOrNull() }

    suspend fun leaveFlash(postId: Long): ServerResult<Int> =
        rpc("leave_flash", jsonBody { put("p_post_id", postId) }) { it.trim().toIntOrNull() }

    /** 댓글·답글 쓰기. [parentId] 가 0 이면 새 댓글. */
    suspend fun createComment(postId: Long, parentId: Long, body: String): ServerResult<Long> =
        rpc(
            "comment_create",
            jsonBody {
                put("p_post", postId)
                put("p_parent", parentId)
                put("p_body", body)
            },
        ) { it.trim().toLongOrNull() }

    suspend fun deleteComment(commentId: Long): ServerResult<Unit> =
        rpc("comment_delete", jsonBody { put("p_comment", commentId) }) { }

    /**
     * 신고. 5건이 모이면 서버가 목록에서 숨긴다.
     *
     * @param targetType POST · COMMENT · CREW · COURSE · USER
     * @param reason SPAM · ABUSE · SEXUAL · DANGER · FRAUD · OTHER
     * @param note 서버 목록에 없는 사유를 OTHER 로 보낼 때 원래 사유(검토하는 사람이 본다)
     */
    suspend fun report(targetType: String, targetId: String, reason: String, note: String = ""): ServerResult<Unit> =
        rpc(
            "content_report",
            jsonBody {
                put("p_type", targetType)
                put("p_target", targetId)
                put("p_reason", reason)
                put("p_note", note)
            },
        ) { }

    /** 차단. 그 사람의 글·댓글이 내 화면에서 사라진다. */
    suspend fun block(userId: String): ServerResult<Unit> =
        rpc("user_block", jsonBody { put("p_user", userId) }) { }

    private suspend fun <T> rpc(
        name: String,
        body: String,
        parse: (String) -> T?,
    ): ServerResult<T> =
        server.authed { token ->
            server.http.post("${server.restUrl}/rpc/$name", body, server.headers(token))
        }.mapBody(parse)
}

/** PostgREST 가 이름에 맞는 서버 함수를 못 찾았다(서버에 새 함수가 아직 없다) */
private fun ServerResult<*>.isMissingFunction(): Boolean =
    this is ServerResult.Rejected && "Could not find the function" in reason

/** 글이 있는지만 볼 때 받는 한 줄 */
@Serializable
data class PostIdRow(val id: Long)
