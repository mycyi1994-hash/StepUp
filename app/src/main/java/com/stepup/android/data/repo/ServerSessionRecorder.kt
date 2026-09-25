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
    /** 코스를 완주한 러닝이면 그 코스의 길(지우지 않고 읽기만) */
    private val peekCourseRun: suspend (startedAt: Long) -> String? = { null },
    /** 코스 기록을 다 냈으면 목록에서 지운다 */
    private val takeCourseRun: suspend (startedAt: Long) -> String? = { null },
) : SessionRecorder {

    override val isConfigured: Boolean get() = server.isConfigured

    override suspend fun record(session: WalkSessionEntity): ServerResult<SessionRecorded> {
        val result = server.recordSession(
            startedAtMillis = session.startedAt,
            endedAtMillis = session.endedAt,
            steps = session.steps,
            durationSec = session.durationSec,
            track = session.track,
            boostBps = session.boostBps,
            partySize = session.partySize,
            faction = session.faction,
        )
        if (result !is ServerResult.Ok) return result

        // 크루 러닝이었으면 어느 크루였는지 적는다. 크루 순위가 이 값으로 센다.
        // 서버가 거절하면(크루를 떠났다 등) 러닝 기록과 적립은 그대로 둔다. 연결이
        // 끊겨 못 적었으면 이 러닝을 다시 올린다 — 서버는 같은 러닝을 다시 받아도
        // 원래 결과를 돌려주고, 크루는 한 번만 적힌다. 여기서 넘어가면 다시 적을 기회가 없다.
        // 다만 몇 번 해도 안 되면 포기한다. 대기열은 오래된 것부터 올리고 실패하면
        // 멈추므로, 곁가지 하나가 계속 실패하면 뒤의 러닝이 전부 막혀 7일을 넘긴다.
        val keepTrying = session.uploadAttempts < SIDE_CALL_MAX_ATTEMPTS
        if (session.crewId.isNotBlank()) {
            when (val tagged = server.tagSessionCrew(session.startedAt, session.crewId)) {
                is ServerResult.Retry -> if (keepTrying) return tagged
                is ServerResult.SignInRequired -> return tagged
                else -> Unit
            }
        }
        // 코스를 완주한 러닝이었으면 코스 기록으로 낸다. 서버가 올라온 경로로
        // 코스를 따라갔는지 직접 보고 순위에 넣는다. 크루와 같은 이유로, 연결 문제로
        // 못 냈으면 코스 길을 지우지 않고 다음에 다시 낸다.
        if (courseApi != null) {
            peekCourseRun(session.startedAt)?.let { track ->
                when (val submitted = courseApi.submitRun(track, session.startedAt)) {
                    is ServerResult.Retry ->
                        if (keepTrying) return submitted else takeCourseRun(session.startedAt)
                    is ServerResult.SignInRequired -> return submitted
                    else -> takeCourseRun(session.startedAt)
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
