package com.stepup.android.data.remote

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** 서버에 올릴 하루치 걸음 */
data class DaySteps(val epochDay: Long, val steps: Int, val goal: Int)

/**
 * 도전 보상 — 서버가 목표를 다시 재고 원장에 적는다
 * (`supabase/migrations/0014_events.sql`).
 */
class EventApi(private val server: StepUpServer) {

    companion object {
        /** 게임의 하루 · 주는 한국 시각이다(서버도 폰이 보낸 시간대를 쓰지 않는다) */
        const val SERVER_TIME_ZONE = "Asia/Seoul"
    }

    /** 폰의 일별 걸음을 올린다. 주간 걸음 도전은 서버가 이 값으로 잰다. */
    suspend fun syncSteps(days: List<DaySteps>): ServerResult<Unit> {
        val array: JsonArray = buildJsonArray {
            days.forEach { d ->
                add(
                    buildJsonObject {
                        put("epoch_day", d.epochDay)
                        put("steps", d.steps)
                        put("goal", d.goal)
                    },
                )
            }
        }
        // jsonBody 는 값을 글자로 감싸므로 배열은 직접 만든다
        val body = """{"p_days":$array}"""
        return server.authed { token ->
            server.http.post("${server.restUrl}/rpc/steps_sync", body, server.headers(token))
        }.mapBody { }
    }

    /**
     * 서버가 잰 도전 진행값 — 주간 걸음은 이번 주(한국 시각) 경로가 받쳐 준 러닝 걸음,
     * 나이트 러너는 밤에 시작한 러닝 거리(km). 받기 판정도 이 값으로 한다.
     */
    suspend fun progress(eventId: String): ServerResult<Double> =
        server.authed { token ->
            server.http.post(
                "${server.restUrl}/rpc/event_progress",
                jsonBody {
                    put("p_event", eventId)
                    put("p_tz", SERVER_TIME_ZONE)
                },
                server.headers(token),
            )
        }.mapBody { it.trim().toDoubleOrNull()?.takeIf { v -> v.isFinite() } }

    /**
     * 도전 보상 받기. 서버가 지급한 금액이 돌아온다.
     *
     * 목표 미달·이미 받음은 [ServerResult.Rejected] 로 온다 — 이유는 서버가 적는다.
     *
     * @param timeZone 기기 시간대(IANA). "저녁 8시 이후"와 "이번 주"를 이 시간대로 센다.
     */
    suspend fun claim(eventId: String, timeZone: String): ServerResult<Double> =
        server.authed { token ->
            server.http.post(
                "${server.restUrl}/rpc/event_claim",
                jsonBody {
                    put("p_event", eventId)
                    put("p_tz", timeZone)
                },
                server.headers(token),
            )
        }.mapBody { it.trim().toDoubleOrNull() }
}
