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

    /** 한 글의 댓글. 먼저 단 것이 위. */
    suspend fun comments(postId: Long): ServerResult<List<CommentRow>> =
        server.authed { token ->
            server.http.get(
                "${server.restUrl}/comment_feed?select=*&post_id=eq.$postId&order=created_at.asc",
                server.headers(token),
            )
        }.mapBody { serverJson.decodeFromString<List<CommentRow>>(it) }

    /**
     * 글쓰기. 새 글 번호를 돌려받는다.
     *
     * @param crewId 비어 있으면 전체 게시판
     * @param meetAtIso 번개러닝의 모임 시각(ISO-8601). 번개가 아니면 비워 둔다.
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
        },
    ) { it.trim().toLongOrNull() }

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
     */
    suspend fun report(targetType: String, targetId: String, reason: String): ServerResult<Unit> =
        rpc(
            "content_report",
            jsonBody {
                put("p_type", targetType)
                put("p_target", targetId)
                put("p_reason", reason)
                put("p_note", "")
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
