package com.stepup.android.data.remote

import com.stepup.android.domain.CrewHomeRules
import com.stepup.android.domain.CrewHomeSummary
import com.stepup.android.domain.CrewMeeting
import com.stepup.android.domain.CrewRun
import com.stepup.android.domain.CrewRunDetail
import com.stepup.android.domain.CrewWeek
import com.stepup.android.domain.CrewWeekMember
import com.stepup.android.domain.MeetingAttendee
import com.stepup.android.domain.MeetingFace
import com.stepup.android.domain.MeetingResponse
import java.time.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull

/**
 * 내 크루 홈(확정 4번) — 서버 함수(`supabase/migrations/0049_crew_home.sql`)만 부른다. 함수마다 지금의 가입 여부를
 * 서버가 다시 본다(멤버가 아니면 crew_not_member). 참석 인원 · 주간 거리는 서버가 센 값이다.
 */
class CrewHomeApi(private val server: StepUpServer) {

    /** 00 홈 — 내 역할 · 채팅 미확인 · 다음 러닝 · 이번 주 함께 · 대표 공지 */
    suspend fun home(crewId: String): ServerResult<CrewHomeRow> =
        rpc("crew_home", jsonBody { put("p_crew", crewId) }) { serverJson.decodeFromString<CrewHomeRow>(it) }

    /** 09 모임 한 건 */
    suspend fun meeting(meetingId: Long): ServerResult<MeetingRow> =
        rpc("crew_meeting", jsonBody { put("p_post", meetingId) }) { serverJson.decodeFromString<MeetingRow>(it) }

    /** 참석 · 불참 — 사람마다 응답 하나로 바꿔 적고, 서버가 받은 뒤의 모임을 돌려준다 */
    suspend fun respond(meetingId: Long, attend: Boolean): ServerResult<MeetingRow> =
        rpc(
            "crew_meeting_respond",
            jsonBody {
                put("p_post", meetingId)
                put("p_attend", attend)
            },
        ) { serverJson.decodeFromString<MeetingRow>(it) }

    /** 13 참석자 — 진행자 먼저 */
    suspend fun attendees(meetingId: Long): ServerResult<List<AttendeeRow>> =
        rpc("crew_meeting_attendees", jsonBody { put("p_post", meetingId) }) { serverJson.decodeFromString<List<AttendeeRow>>(it) }

    /** 공지에 이을 수 있는 모임 — 아직 시작하지 않은 이 크루의 모임 */
    suspend fun upcoming(crewId: String): ServerResult<List<MeetingRow>> =
        rpc("crew_meetings_upcoming", jsonBody { put("p_crew", crewId) }) { serverJson.decodeFromString<List<MeetingRow>>(it) }

    /** 15 · 17 한 주 — [weekStart] 가 없으면 이번 주 */
    suspend fun week(crewId: String, weekStart: LocalDate?): ServerResult<WeekRow> =
        rpc(
            "crew_week",
            jsonBody {
                put("p_crew", crewId)
                put("p_week", weekStart?.toString() ?: JsonNull)
            },
        ) { serverJson.decodeFromString<WeekRow>(it) }

    /** 18 참여 기록 — 늦게 끝난 순. [beforeAt] · [beforeId] 는 앞 장의 마지막 줄 */
    suspend fun runs(
        crewId: String,
        weekStart: LocalDate?,
        day: LocalDate?,
        userId: String?,
        beforeAt: String?,
        beforeId: Long?,
        limit: Int,
    ): ServerResult<List<RunRow>> =
        rpc(
            "crew_runs",
            jsonBody {
                put("p_crew", crewId)
                put("p_week", weekStart?.toString() ?: JsonNull)
                put("p_day", day?.toString() ?: JsonNull)
                put("p_user", userId ?: JsonNull)
                put("p_before_at", beforeAt ?: JsonNull)
                put("p_before_id", beforeId ?: JsonNull)
                put("p_limit", limit)
            },
        ) { serverJson.decodeFromString<List<RunRow>>(it) }

    /** 05 · 06 크루원이 가장 최근에 크루로 적은 러닝 — 없으면 Ok(null) */
    suspend fun lastRun(crewId: String, userId: String): ServerResult<Maybe<RunRow>> =
        rpc(
            "crew_member_last_run",
            jsonBody {
                put("p_crew", crewId)
                put("p_user", userId)
            },
        ) { Maybe(serverJson.decodeFromString<RunRow?>(it)) }

    /** 19 러닝 한 건 */
    suspend fun run(runId: Long): ServerResult<RunDetailRow> =
        rpc("crew_run", jsonBody { put("p_run", runId) }) { serverJson.decodeFromString<RunDetailRow>(it) }

    private suspend fun <T> rpc(name: String, body: String, parse: (String) -> T?): ServerResult<T> =
        server.authed { token ->
            server.http.post("${server.restUrl}/rpc/$name", body, server.headers(token))
        }.mapBody(parse)

    /** 비어 있는 답(null)도 성공으로 — [mapBody] 는 null 을 이해하지 못한 답으로 본다 */
    class Maybe<T>(val value: T?)
}

@Serializable
data class MeetingFaceRow(@SerialName("user_id") val userId: String, val name: String? = null)

@Serializable
data class MeetingRow(
    val id: Long,
    @SerialName("crew_id") val crewId: String? = null,
    val title: String? = null,
    val body: String? = null,
    val place: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    @SerialName("distance_km") val distanceKm: Double? = null,
    @SerialName("meet_at") val meetAt: String? = null,
    val capacity: Int? = null,
    @SerialName("host_id") val hostId: String? = null,
    @SerialName("host_name") val hostName: String? = null,
    @SerialName("host_owner") val hostOwner: Boolean = false,
    val attendees: Int = 0,
    val faces: List<MeetingFaceRow> = emptyList(),
    @SerialName("my_response") val myResponse: String? = null,
    val open: Boolean = true,
) {
    fun toDomain() = CrewMeeting(
        id = id,
        crewId = crewId.orEmpty(),
        title = title.orEmpty(),
        body = body.orEmpty(),
        place = place.orEmpty(),
        lat = lat,
        lng = lng,
        distanceKm = distanceKm ?: 0.0,
        meetAt = meetAt?.chatMillis()?.takeIf { it > 0 },
        capacity = capacity ?: 0,
        hostId = hostId.orEmpty(),
        hostName = hostName.orEmpty(),
        hostOwner = hostOwner,
        attendees = attendees,
        faces = faces.map { MeetingFace(it.userId, it.name.orEmpty()) },
        myResponse = MeetingResponse.of(myResponse),
        open = open,
    )
}

@Serializable
data class AttendeeRow(
    @SerialName("user_id") val userId: String,
    val name: String? = null,
    val host: Boolean = false,
    val owner: Boolean = false,
) {
    fun toDomain() = MeetingAttendee(userId, name.orEmpty(), host, owner)
}

@Serializable
data class HomeWeekRow(
    @SerialName("week_start") val weekStart: String? = null,
    val km: Double = 0.0,
    val runners: Int = 0,
    @SerialName("goal_km") val goalKm: Int? = null,
)

@Serializable
data class CrewHomeRow(
    @SerialName("crew_id") val crewId: String,
    val role: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("member_count") val memberCount: Int = 0,
    val unread: Int = 0,
    val meeting: MeetingRow? = null,
    val week: HomeWeekRow = HomeWeekRow(),
    val notice: ChatNoticeRow? = null,
) {
    fun toDomain() = CrewHomeSummary(
        crewId = crewId,
        owner = role == "OWNER",
        ownerId = ownerId.orEmpty(),
        memberCount = memberCount,
        unread = unread,
        meeting = meeting?.toDomain(),
        weekKm = week.km,
        weekRunners = week.runners,
        goalKm = week.goalKm,
        notice = notice?.toDomain(),
    )
}

@Serializable
data class WeekMemberRow(
    @SerialName("user_id") val userId: String,
    val name: String? = null,
    val km: Double = 0.0,
    val runs: Int = 0,
    @SerialName("last_km") val lastKm: Double? = null,
    @SerialName("last_at") val lastAt: String? = null,
) {
    fun toDomain() = CrewWeekMember(userId, name.orEmpty(), km, runs, lastKm, lastAt?.chatMillis()?.takeIf { it > 0 })
}

@Serializable
data class WeekRow(
    @SerialName("week_start") val weekStart: String,
    @SerialName("this_week") val thisWeek: Boolean = false,
    @SerialName("goal_km") val goalKm: Int? = null,
    val km: Double = 0.0,
    val runners: Int = 0,
    val days: List<Double?> = emptyList(),
    val members: List<WeekMemberRow> = emptyList(),
    val weeks: List<String> = emptyList(),
) {
    fun toDomain(): CrewWeek? {
        val start = runCatching { LocalDate.parse(weekStart) }.getOrNull() ?: return null
        return CrewWeek(
            start = start,
            thisWeek = thisWeek,
            goalKm = goalKm,
            km = km,
            runners = runners,
            // 월 → 일 일곱 칸 — 모자라면 오지 않은 날로 채운다
            days = List(7) { days.getOrNull(it) },
            members = members.map { it.toDomain() },
            weeks = weeks.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() },
        )
    }
}

@Serializable
data class RunRow(
    val id: Long,
    @SerialName("user_id") val userId: String = "",
    val name: String? = null,
    @SerialName("distance_m") val distanceM: Double = 0.0,
    @SerialName("duration_s") val durationS: Int = 0,
    @SerialName("started_at") val startedAt: String = "",
    @SerialName("ended_at") val endedAt: String = "",
) {
    fun toDomain() = CrewRun(id, userId, name.orEmpty(), distanceM, durationS, startedAt.chatMillis(), endedAt.chatMillis(), endedAt)
}

@Serializable
data class RunDetailRow(
    val id: Long,
    @SerialName("crew_id") val crewId: String = "",
    @SerialName("user_id") val userId: String = "",
    val name: String? = null,
    @SerialName("distance_m") val distanceM: Double = 0.0,
    @SerialName("duration_s") val durationS: Int = 0,
    @SerialName("started_at") val startedAt: String = "",
    @SerialName("ended_at") val endedAt: String = "",
    val route: String? = null,
) {
    fun toDomain() = CrewRunDetail(
        run = CrewRun(id, userId, name.orEmpty(), distanceM, durationS, startedAt.chatMillis(), endedAt.chatMillis()),
        crewId = crewId,
        route = CrewHomeRules.route(route.orEmpty()),
    )
}
