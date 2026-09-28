package com.stepup.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * 크루 목록 한 줄 — `crew_feed` 뷰가 보는 사람 기준으로 만들어 내려준다.
 *
 * `joined`·`requested`·`owned` 는 같은 크루라도 누가 보느냐에 따라 다르다.
 * `pending_count` 는 크루장에게만 값이 있고 나머지에게는 0 이다.
 */
@Serializable
data class CrewRow(
    val id: String,
    val name: String,
    val monogram: String = "",
    val tagline: String = "",
    val area: String = "",
    @SerialName("join_policy") val joinPolicy: String = "OPEN",
    @SerialName("member_count") val memberCount: Int = 0,
    val roster: List<String> = emptyList(),
    val joined: Boolean = false,
    val requested: Boolean = false,
    @SerialName("pending_count") val pendingCount: Int = 0,
    val owned: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    // ── 0047 크루 명함형 — 서버가 아직 모르면 기본값(예전 크루처럼 보인다) ──
    @SerialName("owner_id") val ownerId: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    @SerialName("leader_name") val leaderName: String = "",
    @SerialName("leader_note") val leaderNote: String = "",
    @SerialName("image_bg") val imageBg: Int? = null,
    @SerialName("image_ver") val imageVer: Int = 0,
    @SerialName("has_image") val hasImage: Boolean = false,
    @SerialName("meet_days") val meetDays: Int = 0,
    @SerialName("meet_time") val meetTime: Int? = null,
    @SerialName("run_distance") val runDistance: String? = null,
    val moods: List<String> = emptyList(),
    val capacity: Int? = null,
    val recruiting: Boolean = true,
    @SerialName("recruit_changed_at") val recruitChangedAt: String = "",
    @SerialName("weekly_goal_km") val weeklyGoalKm: Int? = null,
    val level: Int? = null,
    @SerialName("week_km") val weekKm: Double = 0.0,
    @SerialName("week_runners") val weekRunners: Int = 0,
    @SerialName("my_application_id") val myApplicationId: Long? = null,
    @SerialName("my_application_status") val myApplicationStatus: String? = null,
    @SerialName("my_application_seen") val myApplicationSeen: Boolean = false,
)

/** 가입 신청서(crew_application · crew_pending_applications) */
@Serializable
data class CrewApplicationRow(
    val id: Long,
    @SerialName("crew_id") val crewId: String = "",
    @SerialName("user_id") val userId: String = "",
    val name: String = "",
    val status: String = "PENDING",
    val phrases: List<String> = emptyList(),
    val message: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("decided_at") val decidedAt: String? = null,
    val seen: Boolean = false,
)

/** 크루 멤버 한 줄(crew_roster) */
@Serializable
data class CrewMemberRow(
    @SerialName("user_id") val userId: String,
    val name: String = "",
    val role: String = "MEMBER",
    @SerialName("joined_at") val joinedAt: String = "",
    @SerialName("week_km") val weekKm: Double = 0.0,
    @SerialName("week_runs") val weekRuns: Int = 0,
)

/** 이 크루에서 본 한 사람(crew_person) */
@Serializable
data class CrewPersonRow(
    @SerialName("user_id") val userId: String,
    val name: String = "",
    val role: String = "NONE",
    @SerialName("joined_at") val joinedAt: String? = null,
    @SerialName("week_km") val weekKm: Double = 0.0,
    val application: CrewPersonApplicationRow? = null,
)

@Serializable
data class CrewPersonApplicationRow(
    val id: Long,
    val phrases: List<String> = emptyList(),
    val message: String = "",
    @SerialName("created_at") val createdAt: String = "",
)

/** 레벨 안내(crew_level) */
@Serializable
data class CrewLevelRow(
    val level: Int? = null,
    @SerialName("week_km") val weekKm: Double = 0.0,
    @SerialName("week_runners") val weekRunners: Int = 0,
    @SerialName("member_count") val memberCount: Int = 0,
)

/** 가입 신청의 결과(crew_apply) — PENDING · MEMBER */
@Serializable
data class CrewApplyRow(
    val result: String = "",
    @SerialName("application_id") val applicationId: Long? = null,
    val duplicate: Boolean = false,
)

/** 승인 · 미승인 뒤(crew_application_decide) */
@Serializable
data class CrewDecisionRow(
    val status: String = "",
    @SerialName("member_count") val memberCount: Int = 0,
    val capacity: Int? = null,
    @SerialName("pending_count") val pendingCount: Int = 0,
)

/** 상태만 돌려주는 답(crew_application_cancel) */
@Serializable
data class CrewStatusRow(val status: String = "")

/** 대표 사진(crew_images) */
@Serializable
data class CrewImageRow(val data: String = "")

/** 크루장이 보는 가입 신청 한 줄 */
@Serializable
data class CrewRequestRow(
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("requested_at") val requestedAt: String,
)

/** 크루 순위 한 줄 — `crew_leaderboard` */
@Serializable
data class CrewRankRow(
    @SerialName("crew_id") val crewId: String,
    val km: Double = 0.0,
    val runs: Int = 0,
    val runners: Int = 0,
)

/**
 * 크루 — 서버와 말을 주고받는 쪽.
 *
 * 쓰는 일은 전부 서버 함수(`supabase/migrations/0010_crew_join.sql`)가 한다.
 * "승인제인지 보고 → 신청을 넣는다"를 앱이 두 번에 나눠 하면, 그 사이에
 * 크루장이 방식을 바꿨을 때 어긋난다.
 */
class CrewApi(private val server: StepUpServer) {

    /** 크루 목록. 사람이 많은 크루가 위로 온다. */
    suspend fun crews(): ServerResult<List<CrewRow>> =
        server.authed { token ->
            server.http.get(
                "${server.restUrl}/crew_feed?select=*" +
                    "&order=member_count.desc,created_at.desc&limit=200",
                server.headers(token),
            )
        }.mapBody { serverJson.decodeFromString<List<CrewRow>>(it) }

    /** 크루를 만들고 새 크루의 id 를 돌려받는다. 만든 사람은 서버가 주인으로 넣는다. */
    suspend fun create(
        name: String,
        monogram: String,
        tagline: String,
        area: String,
        joinPolicy: String,
    ): ServerResult<String> = rpc(
        "crew_create",
        jsonBody {
            put("p_name", name)
            put("p_monogram", monogram)
            put("p_tagline", tagline)
            put("p_area", area)
            put("p_join_policy", joinPolicy)
        },
    ) { serverJson.decodeFromString<String>(it) }

    /** 가입. 자유 가입이면 `JOINED`, 승인제면 `REQUESTED` 가 온다. */
    suspend fun join(crewId: String): ServerResult<String> =
        rpc("crew_join", jsonBody { put("p_crew", crewId) }) {
            serverJson.decodeFromString<String>(it)
        }

    /** 탈퇴. 기다리던 가입 신청이 있으면 그것도 거둔다. */
    suspend fun leave(crewId: String): ServerResult<Unit> =
        rpc("crew_leave", jsonBody { put("p_crew", crewId) }) { }

    /** 가입 방식 바꾸기. 자유 가입으로 열면 받아 준 인원이 돌아온다. */
    suspend fun setJoinPolicy(crewId: String, joinPolicy: String): ServerResult<Int> =
        rpc(
            "crew_set_join_policy",
            jsonBody {
                put("p_crew", crewId)
                put("p_policy", joinPolicy)
            },
        ) { it.trim().toIntOrNull() }

    /** 크루장이 보는 가입 신청 목록. 먼저 신청한 사람이 위. */
    suspend fun requests(crewId: String): ServerResult<List<CrewRequestRow>> =
        rpc("crew_requests", jsonBody { put("p_crew", crewId) }) {
            serverJson.decodeFromString<List<CrewRequestRow>>(it)
        }

    /** 가입 신청 승인·거절. 어느 쪽이든 신청은 사라진다. */
    suspend fun decide(crewId: String, userId: String, approve: Boolean): ServerResult<Unit> =
        rpc(
            "crew_decide",
            jsonBody {
                put("p_crew", crewId)
                put("p_user", userId)
                put("p_approve", approve)
            },
        ) { }

    /**
     * 크루 순위 — 크루원 모두가 크루로 달린 거리(`crew_leaderboard`).
     *
     * @param period DAY · WEEK · MONTH · ALL
     */
    suspend fun leaderboard(period: String): ServerResult<List<CrewRankRow>> =
        rpc("crew_leaderboard", jsonBody { put("p_period", period) }) {
            serverJson.decodeFromString<List<CrewRankRow>>(it)
        }

    // ── 0047 크루 명함형 ─────────────────────────────────────────────

    /** 크루 한 곳 — 해산 · 숨김이면 null(접근 불가) */
    suspend fun crew(crewId: String): ServerResult<CrewRow?> =
        server.authed { token ->
            server.http.get("${server.restUrl}/crew_feed?select=*&id=eq.${enc(crewId)}", server.headers(token))
        }.mapBody { body -> Wrapped(serverJson.decodeFromString<List<CrewRow>>(body).firstOrNull()) }
            .let { result -> if (result is ServerResult.Ok) ServerResult.Ok(result.value.value) else result.cast() }

    /** 대표 사진(base64 JPEG). 사진이 없으면 빈 문자열 */
    suspend fun image(crewId: String): ServerResult<String> =
        server.authed { token ->
            server.http.get("${server.restUrl}/crew_images?select=data&crew_id=eq.${enc(crewId)}", server.headers(token))
        }.mapBody { body -> serverJson.decodeFromString<List<CrewImageRow>>(body).firstOrNull()?.data.orEmpty() }

    /** 크루 만들기(3단계 초안을 한 번에). 같은 [clientKey] 는 같은 크루를 돌려받는다 */
    suspend fun createCard(
        name: String,
        tagline: String,
        leaderNote: String,
        imageBg: Int?,
        image: String?,
        area: String,
        lat: Double?,
        lng: Double?,
        meetDays: Int,
        meetTime: Int?,
        distance: String?,
        moods: List<String>,
        capacity: Int,
        recruiting: Boolean,
        goalKm: Int?,
        clientKey: String,
    ): ServerResult<String> = rpc(
        "crew_create_card",
        body {
            put("p_name", name)
            put("p_tagline", tagline)
            put("p_leader_note", leaderNote)
            put("p_image_bg", imageBg)
            put("p_image", image)
            put("p_area", area)
            put("p_lat", lat)
            put("p_lng", lng)
            put("p_meet_days", meetDays)
            put("p_meet_time", meetTime)
            put("p_distance", distance)
            put("p_moods", JsonArray(moods.map(::JsonPrimitive)))
            put("p_capacity", capacity)
            put("p_recruiting", recruiting)
            put("p_goal_km", goalKm)
            put("p_client_key", clientKey)
        },
    ) { serverJson.decodeFromString<String>(it) }

    /** 소개 고치기. [imageAction] KEEP · SET([image]) · REMOVE */
    suspend fun updateProfile(
        crewId: String,
        name: String,
        tagline: String,
        leaderNote: String,
        imageBg: Int?,
        imageAction: String,
        image: String?,
    ): ServerResult<Unit> = rpc(
        "crew_update_profile",
        body {
            put("p_crew", crewId)
            put("p_name", name)
            put("p_tagline", tagline)
            put("p_leader_note", leaderNote)
            put("p_image_bg", imageBg)
            put("p_image_action", imageAction)
            put("p_image", image)
        },
    ) { }

    /** 모임 정보 고치기 */
    suspend fun updateRunning(
        crewId: String,
        area: String,
        lat: Double?,
        lng: Double?,
        meetDays: Int,
        meetTime: Int?,
        distance: String?,
        moods: List<String>,
    ): ServerResult<Unit> = rpc(
        "crew_update_running",
        body {
            put("p_crew", crewId)
            put("p_area", area)
            put("p_lat", lat)
            put("p_lng", lng)
            put("p_meet_days", meetDays)
            put("p_meet_time", meetTime)
            put("p_distance", distance)
            put("p_moods", JsonArray(moods.map(::JsonPrimitive)))
        },
    ) { }

    /** 모집 설정 — 정원 · 모집 상태 · (goalChange 면) 목표 */
    suspend fun updateRecruit(
        crewId: String,
        capacity: Int,
        recruiting: Boolean,
        goalChange: Boolean,
        goalKm: Int?,
    ): ServerResult<Unit> = rpc(
        "crew_update_recruit",
        body {
            put("p_crew", crewId)
            put("p_capacity", capacity)
            put("p_recruiting", recruiting)
            put("p_goal_change", goalChange)
            put("p_goal_km", goalKm)
        },
    ) { }

    /** 모집 멈추기 · 다시 시작 */
    suspend fun setRecruiting(crewId: String, open: Boolean): ServerResult<Unit> =
        rpc("crew_set_recruiting", body { put("p_crew", crewId); put("p_open", open) }) { }

    /** 주간 목표 — null 이면 목표 없음 */
    suspend fun setGoal(crewId: String, goalKm: Int?): ServerResult<Unit> =
        rpc("crew_set_goal", body { put("p_crew", crewId); put("p_goal_km", goalKm) }) { }

    /** 가입 신청 — 고른 문구와 한마디를 따로 보낸다 */
    suspend fun apply(crewId: String, phrases: List<String>, message: String, clientKey: String): ServerResult<CrewApplyRow> =
        rpc(
            "crew_apply",
            body {
                put("p_crew", crewId)
                put("p_phrases", JsonArray(phrases.map(::JsonPrimitive)))
                put("p_message", message)
                put("p_client_key", clientKey)
            },
        ) { serverJson.decodeFromString<CrewApplyRow>(it) }

    /** 신청서 한 장(신청한 사람 · 그 크루의 크루장) */
    suspend fun application(applicationId: Long): ServerResult<CrewApplicationRow> =
        rpc("crew_application", body { put("p_application", applicationId) }) {
            serverJson.decodeFromString<CrewApplicationRow>(it)
        }

    /** 신청 취소 — 지금 상태를 돌려받는다(그 사이 승인됐으면 APPROVED) */
    suspend fun cancelApplication(applicationId: Long): ServerResult<String> =
        rpc("crew_application_cancel", body { put("p_application", applicationId) }) {
            serverJson.decodeFromString<CrewStatusRow>(it).status
        }

    /** 결과를 봤다 */
    suspend fun seenApplication(applicationId: Long): ServerResult<Unit> =
        rpc("crew_application_seen", body { put("p_application", applicationId) }) { }

    /** 크루장이 보는 기다리는 신청 */
    suspend fun pendingApplications(crewId: String): ServerResult<List<CrewApplicationRow>> =
        rpc("crew_pending_applications", body { put("p_crew", crewId) }) {
            serverJson.decodeFromString<List<CrewApplicationRow>>(it)
        }

    /** 승인 · 미승인 */
    suspend fun decideApplication(applicationId: Long, approve: Boolean): ServerResult<CrewDecisionRow> =
        rpc("crew_application_decide", body { put("p_application", applicationId); put("p_approve", approve) }) {
            serverJson.decodeFromString<CrewDecisionRow>(it)
        }

    /** 멤버 — 크루장 먼저 */
    suspend fun roster(crewId: String): ServerResult<List<CrewMemberRow>> =
        rpc("crew_roster", body { put("p_crew", crewId) }) { serverJson.decodeFromString<List<CrewMemberRow>>(it) }

    /** 이 크루에서 본 한 사람 */
    suspend fun person(crewId: String, userId: String): ServerResult<CrewPersonRow> =
        rpc("crew_person", body { put("p_crew", crewId); put("p_user", userId) }) {
            serverJson.decodeFromString<CrewPersonRow>(it)
        }

    /** 레벨 안내 — 목록과 따로 읽는다 */
    suspend fun level(crewId: String): ServerResult<CrewLevelRow> =
        rpc("crew_level", body { put("p_crew", crewId) }) { serverJson.decodeFromString<CrewLevelRow>(it) }

    suspend fun removeMember(crewId: String, userId: String): ServerResult<Unit> =
        rpc("crew_member_remove", body { put("p_crew", crewId); put("p_user", userId) }) { }

    suspend fun transferOwner(crewId: String, userId: String): ServerResult<Unit> =
        rpc("crew_transfer_owner", body { put("p_crew", crewId); put("p_user", userId) }) { }

    suspend fun dissolve(crewId: String): ServerResult<Unit> =
        rpc("crew_dissolve", body { put("p_crew", crewId) }) { }

    /** 크루 신고 — 기존 신고함(content_report) */
    suspend fun report(crewId: String, reason: String, note: String): ServerResult<Unit> =
        rpc(
            "content_report",
            body { put("p_type", "CREW"); put("p_target", crewId); put("p_reason", reason); put("p_note", note) },
        ) { }

    private fun body(build: JsonObjectBuilder.() -> Unit): String = buildJsonObject(build).toString()

    private fun enc(value: String): String = java.net.URLEncoder.encode(value, "UTF-8")

    /** null 을 담은 성공을 [mapBody] 가 실패로 보지 않게 감싼다 */
    private class Wrapped<T>(val value: T)

    @Suppress("UNCHECKED_CAST")
    private fun <T> ServerResult<*>.cast(): ServerResult<T> = this as ServerResult<T>

    private suspend fun <T> rpc(
        name: String,
        body: String,
        parse: (String) -> T?,
    ): ServerResult<T> =
        server.authed { token ->
            server.http.post("${server.restUrl}/rpc/$name", body, server.headers(token))
        }.mapBody(parse)
}
