package com.stepup.android.data.repo

import com.stepup.android.data.remote.CrewHomeApi
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.domain.CrewHomeSummary
import com.stepup.android.domain.CrewMeeting
import com.stepup.android.domain.CrewRun
import com.stepup.android.domain.CrewRunDetail
import com.stepup.android.domain.CrewRunsScope
import com.stepup.android.domain.CrewWeek
import com.stepup.android.domain.MeetingAttendee
import com.stepup.android.domain.RecordingOwner
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** 크루 홈 한 동작의 결말 */
sealed interface HomeOutcome<out T> {
    data class Ok<T>(val value: T) : HomeOutcome<T>

    /** 지금 이 크루의 멤버가 아니다(탈퇴 · 내보내기 · 해산) — 접근 종료 화면으로 */
    data object Ended : HomeOutcome<Nothing>

    /** 모임 · 러닝 기록 · 공지가 없다(취소 · 삭제 · 무효 판정) */
    data object Missing : HomeOutcome<Nothing>

    data class Failed(val problem: HomeProblem) : HomeOutcome<Nothing>
}

enum class HomeProblem {
    /** 연결 · 서버 사정 — 다시 하면 될 수 있다 */
    NETWORK,
    SIGN_IN,

    /** 모임 정원이 찼다(기존 참가 규칙) */
    FULL,

    /** 이미 시작한 모임 — 응답을 받지 않는다 */
    CLOSED,
    OTHER,
}

/**
 * 내 크루 홈(확정 4번) — 홈 · 모임 · 참석 · 주간 기록 · 러닝 기록.
 *
 * 인원 · 내 응답 · 거리는 서버가 센 값만 쓴다. 참석 응답이 저장되면 서버가 돌려준 모임으로 홈의 모임도 바꾼다(28 · 29) —
 * 앱이 인원을 더하지 않는다. 크루마다 마지막으로 받은 홈을 들고 있다가 다시 열 때 먼저 보이고, 갱신이 실패해도 남긴다.
 * 멤버가 아니게 되면 그 크루의 홈을 지우고 [onEnded] 로 알린다(같은 크루의 채팅 캐시도 지운다).
 */
class CrewHomeRepository(
    private val api: CrewHomeApi,
    private val owner: suspend () -> String,
    private val onEnded: suspend (String) -> Unit = {},
) {
    private val homes = MutableStateFlow<Map<String, CrewHomeSummary>>(emptyMap())

    /** 들고 있는 홈의 주인 — 계정이 바뀌면 앞사람의 홈을 지운다 */
    @Volatile private var holder: String? = null

    private suspend fun ownerNow() {
        val now = owner()
        val before = holder
        if (before != null && before != now) homes.value = emptyMap()
        holder = now
    }

    /** 지금 로그인한 사람의 서버 id — 크루원 목록의 "나" */
    suspend fun me(): String {
        ownerNow()
        return RecordingOwner.userId(owner()).orEmpty()
    }

    fun homeNow(crewId: String): CrewHomeSummary? = homes.value[crewId]

    fun home(crewId: String): Flow<CrewHomeSummary?> = homes.map { it[crewId] }.distinctUntilChanged()

    suspend fun loadHome(crewId: String): HomeOutcome<CrewHomeSummary> {
        ownerNow()
        return api.home(crewId).home(crewId) { it.toDomain() }.also { outcome ->
            if (outcome is HomeOutcome.Ok) homes.update { it + (crewId to outcome.value) }
        }
    }

    suspend fun meeting(crewId: String, meetingId: Long): HomeOutcome<CrewMeeting> =
        api.meeting(meetingId).home(crewId) { it.toDomain() }.also { outcome ->
            when (outcome) {
                is HomeOutcome.Ok -> patchMeeting(outcome.value)
                HomeOutcome.Missing -> dropMeeting(crewId, meetingId)
                else -> Unit
            }
        }

    /** 참석(true) · 불참(false) — 서버가 받은 뒤의 모임. 실패하면 앞의 응답 · 인원을 그대로 둔다 */
    suspend fun respond(crewId: String, meetingId: Long, attend: Boolean): HomeOutcome<CrewMeeting> =
        api.respond(meetingId, attend).home(crewId) { it.toDomain() }.also { outcome ->
            when (outcome) {
                is HomeOutcome.Ok -> patchMeeting(outcome.value)
                HomeOutcome.Missing -> dropMeeting(crewId, meetingId)
                else -> Unit
            }
        }

    suspend fun attendees(crewId: String, meetingId: Long): HomeOutcome<List<MeetingAttendee>> =
        api.attendees(meetingId).home(crewId) { rows -> rows.map { it.toDomain() } }

    /** 공지에 이을 수 있는 모임 */
    suspend fun upcoming(crewId: String): HomeOutcome<List<CrewMeeting>> =
        api.upcoming(crewId).home(crewId) { rows -> rows.map { it.toDomain() } }

    suspend fun week(crewId: String, start: LocalDate?): HomeOutcome<CrewWeek> =
        api.week(crewId, start).home(crewId) { it.toDomain() }

    /** 참여 기록 한 장 — [after] 는 앞 장의 마지막 줄 */
    suspend fun runs(crewId: String, scope: CrewRunsScope, after: CrewRun?, limit: Int = PAGE): HomeOutcome<List<CrewRun>> =
        api.runs(
            crewId, scope.week, scope.day, scope.userId,
            beforeAt = after?.endedCursor?.takeIf { it.isNotEmpty() }, beforeId = after?.id, limit = limit,
        ).home(crewId) { rows -> rows.map { it.toDomain() } }

    /** 크루원의 가장 최근 크루 러닝 — 없으면 Ok(null) */
    suspend fun lastRun(crewId: String, userId: String): HomeOutcome<CrewRun?> =
        when (val result = api.lastRun(crewId, userId)) {
            is ServerResult.Ok -> HomeOutcome.Ok(result.value.value?.toDomain())
            else -> result.home<CrewHomeApi.Maybe<com.stepup.android.data.remote.RunRow>, CrewRun>(crewId) { null }
        }

    suspend fun run(crewId: String, runId: Long): HomeOutcome<CrewRunDetail> =
        api.run(runId).home(crewId) { it.toDomain() }

    /** 이 크루의 홈을 지운다(나갔다 · 내보내졌다 · 해산됐다) */
    fun forget(crewId: String) {
        homes.update { it - crewId }
    }

    private fun patchMeeting(meeting: CrewMeeting) {
        homes.update { map ->
            val home = map[meeting.crewId]
            if (home?.meeting?.id == meeting.id) map + (meeting.crewId to home.copy(meeting = meeting)) else map
        }
    }

    /** 모임이 취소(삭제)됐다 — 홈에 남기지 않는다. 다음 유효 모임은 홈을 다시 읽을 때 온다 */
    private fun dropMeeting(crewId: String, meetingId: Long) {
        homes.update { map ->
            val home = map[crewId]
            if (home?.meeting?.id == meetingId) map + (crewId to home.copy(meeting = null)) else map
        }
    }

    private suspend fun <T, R> ServerResult<T>.home(crewId: String, transform: (T) -> R?): HomeOutcome<R> {
        val outcome: HomeOutcome<R> = when (this) {
            is ServerResult.Ok -> transform(value)?.let { HomeOutcome.Ok(it) } ?: HomeOutcome.Failed(HomeProblem.OTHER)
            is ServerResult.Rejected -> homeRejection(reason)
            is ServerResult.Retry -> HomeOutcome.Failed(HomeProblem.NETWORK)
            is ServerResult.SignInRequired -> HomeOutcome.Failed(HomeProblem.SIGN_IN)
        }
        if (outcome == HomeOutcome.Ended) {
            forget(crewId)
            onEnded(crewId)
        }
        return outcome
    }

    companion object {
        const val PAGE = 30
    }
}

/** 서버가 거절한 까닭(raise 의 첫 마디) → 화면이 고를 다음 */
internal fun homeRejection(reason: String): HomeOutcome<Nothing> = when {
    reason.contains("crew_not_member") || reason.contains("chat_not_member") -> HomeOutcome.Ended
    reason.contains("meeting_missing") || reason.contains("run_missing") || reason.contains("notice_missing") ||
        reason.contains("번개러닝 글을 찾을 수 없습니다") -> HomeOutcome.Missing
    reason.contains("meeting_closed") || reason.contains("이미 지난 모임") -> HomeOutcome.Failed(HomeProblem.CLOSED)
    reason.contains("정원이 찼습니다") -> HomeOutcome.Failed(HomeProblem.FULL)
    else -> HomeOutcome.Failed(HomeProblem.OTHER)
}
