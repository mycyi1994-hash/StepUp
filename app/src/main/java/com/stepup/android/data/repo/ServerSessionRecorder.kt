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
    /** 코스를 완주한 러닝이면 그 코스의 길을 꺼내 준다(한 번만) */
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
        // 크루 러닝이었으면 어느 크루였는지 적는다. 크루 순위가 이 값으로 센다.
        // 못 적어도 러닝 기록과 적립은 이미 끝났으므로 실패를 되돌리지 않는다.
        if (result is ServerResult.Ok && session.crewId.isNotBlank()) {
            server.tagSessionCrew(session.startedAt, session.crewId)
        }
        // 코스를 완주한 러닝이었으면 코스 기록으로 낸다. 서버가 올라온 경로로
        // 코스를 따라갔는지 직접 보고 순위에 넣는다. 크루와 같은 이유로 실패해도
        // 러닝 기록은 되돌리지 않는다.
        if (result is ServerResult.Ok && courseApi != null) {
            takeCourseRun(session.startedAt)?.let { track -> courseApi.submitRun(track, session.startedAt) }
        }
        return result
    }
}
