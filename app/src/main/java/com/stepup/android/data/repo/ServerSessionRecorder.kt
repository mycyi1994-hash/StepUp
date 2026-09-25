package com.stepup.android.data.repo

import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.data.remote.CourseApi
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.SessionRecorded
import com.stepup.android.data.remote.StepUpServer

/**
 * 저장된 세션 한 줄을 서버 호출로 옮긴다.
 *
 * 저장소(Room)의 모양과 서버가 받는 모양을 갈라 두기 위한 얇은 층이다.
 * 둘 중 하나가 바뀌어도 다른 쪽이 따라 흔들리지 않는다.
 */
class ServerSessionRecorder(
    private val server: StepUpServer,
    private val courseApi: CourseApi? = null,
    private val readCourseRun: suspend (startedAt: Long) -> String? = { null },
    private val acknowledgeCourseRun: suspend (startedAt: Long) -> Unit = {},
) : SessionRecorder {

    override val isConfigured: Boolean get() = server.isConfigured

    override suspend fun record(session: WalkSessionEntity): ServerResult<SessionRecorded> {
        val owner = com.stepup.android.domain.RecordingOwner.userId(session.recordingOwner)
            ?: return ServerResult.SignInRequired("이 러닝의 계정 소유권을 확인해야 합니다. 기록은 기기에 보관됩니다")
        val result = server.recordSession(
            expectedUserId = owner,
            startedAtMillis = session.startedAt,
            endedAtMillis = session.endedAt,
            steps = session.steps,
            durationSec = session.durationSec,
            track = session.track,
            boostBps = session.boostBps,
            partySize = session.partySize,
            faction = session.faction,
        )
        // 크루 러닝이었으면 어느 크루였는지 적는다. 크루 순위가 이 값으로 센다.
        // Retry the idempotent run when a follow-up is temporarily unavailable.
        // Never send a follow-up using another account's token.
        // 다만 몇 번 해도 안 되면 포기한다. 대기열은 오래된 것부터 올리고 실패하면 멈추므로,
        // 곁가지 하나가 계속 실패하면 뒤의 러닝이 전부 막혀 7일 청구 창을 넘긴다.
        val keepTrying = session.uploadAttempts < SIDE_CALL_MAX_ATTEMPTS
        if (result is ServerResult.Ok && session.crewId.isNotBlank()) {
            when (val tagged = server.tagSessionCrew(session.startedAt, session.crewId, owner)) {
                is ServerResult.SignInRequired -> return tagged
                is ServerResult.Retry -> if (keepTrying) return tagged else Unit
                else -> Unit
            }
        }
        // 코스를 완주한 러닝이었으면 코스 기록으로 낸다. 서버가 올라온 경로로
        // 코스를 따라갔는지 직접 보고 순위에 넣는다. 성공 전에는 대기 항목을 지우지 않는다.
        if (result is ServerResult.Ok && courseApi != null) {
            readCourseRun(session.startedAt)?.let { track ->
                when (val submitted = courseApi.submitRun(track, session.startedAt, owner)) {
                    is ServerResult.Ok -> acknowledgeCourseRun(session.startedAt)
                    is ServerResult.SignInRequired -> return submitted
                    is ServerResult.Retry -> if (keepTrying) return submitted else Unit
                    is ServerResult.Rejected -> Unit // Preserve the course entry for recovery.
                }
            }
        }
        return result
    }

    private companion object {
        /** 크루 표시·코스 기록 때문에 러닝 업로드를 미루는 최대 횟수 */
        const val SIDE_CALL_MAX_ATTEMPTS = 5
    }
}
