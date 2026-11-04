package com.stepup.android.domain

/**
 * 서버가 적은 판정 까닭(record_session verdict_reason, 서버 0089)을 기록 상세의 한 줄로 옮긴다.
 *
 * 서버 글은 여러 까닭을 " · " 로 잇는다. 아는 까닭만 골라 사람 말로 보이고, 모르는 글은 버린다 — 까닭을 지어내지 않는다.
 * 서버가 답한 러닝(확인 SIGNED · 거절 REJECTED)에만 쓴다. 금액 · 판정은 서버 값 그대로 읽기만 한다.
 */
object RunVerdictReason {

    /** 서버 까닭 하나 — 화면 글은 `run_reason_*` 리소스(4개 언어) */
    enum class Kind {
        MOCK, BANNED, CADENCE, SPEED, TRACK_TIME, DUPLICATE_ROUTE, OVERLAP, NO_STEPS, STEPS_OVER_GPS,
        OVERSPEED_STOP, CREW_DONE, DAILY_STEPS, HELD, CREW_EMPTY, CREW_AFTER, WORN_AFTER, ENERGY, WORN,
        DAILY_SUP, NO_GPS_CAP, UPLOAD_CAP, TOO_OLD,
    }

    /** 서버 글(supabase/migrations 0071 · 0089 record_session) → 까닭. 서버 글을 바꾸면 여기도 */
    private val KNOWN: Map<String, Kind> = mapOf(
        "가짜 위치가 감지되었습니다" to Kind.MOCK,
        "과속으로 멈춘 뒤 러닝 금지 시간에 시작한 러닝입니다" to Kind.BANNED,
        "케이던스가 사람 범위를 벗어납니다" to Kind.CADENCE,
        "이동 속도가 사람 범위를 벗어납니다" to Kind.SPEED,
        "경로 시각이 러닝 시간과 맞지 않습니다" to Kind.TRACK_TIME,
        "이미 올린 경로입니다" to Kind.DUPLICATE_ROUTE,
        "다른 러닝과 시간이 겹칩니다" to Kind.OVERLAP,
        "걸음 없이 이동한 거리입니다" to Kind.NO_STEPS,
        "걸음 수가 GPS 거리보다 많습니다" to Kind.STEPS_OVER_GPS,
        "과속이 이어져 러닝이 멈췄습니다" to Kind.OVERSPEED_STOP,
        "오늘 크루 러닝을 이미 했습니다" to Kind.CREW_DONE,
        "하루 적립 상한에 걸렸습니다" to Kind.DAILY_STEPS,
        "최근 무효 러닝이 많아 적립을 보류합니다" to Kind.HELD,
        "크루 체력을 다 썼습니다" to Kind.CREW_EMPTY,
        "크루 러닝이 끝난 뒤의 기록은 크루 보상에서 뺐습니다" to Kind.CREW_AFTER,
        "내구도가 다 닳은 뒤의 걸음은 적립하지 않습니다" to Kind.WORN_AFTER,
        "에너지를 다 썼습니다" to Kind.ENERGY,
        "내구도가 다 닳아 적립이 없습니다" to Kind.WORN,
        "하루 적립 금액 상한에 걸렸습니다" to Kind.DAILY_SUP,
        "경로 없는 러닝의 하루 상한에 걸렸습니다" to Kind.NO_GPS_CAP,
        "하루에 올릴 수 있는 적립 상한에 걸렸습니다" to Kind.UPLOAD_CAP,
        // 거절(REJECTED) — 서버가 받지 않은 러닝의 까닭(uploadError)
        "너무 오래된 러닝입니다" to Kind.TOO_OLD,
    )

    /** 기록 상세에 보일 것 — 없으면 null(줄을 두지 않는다) */
    data class Note(
        /** 적립이 0 이었다(무효 · 거절 포함) — "적립 0 · 까닭", 까닭을 모르면 "적립되지 않았어요" */
        val zero: Boolean,
        /** 아는 까닭(앞에서부터, 겹치지 않게 최대 [MAX]개) — 비어 있으면 까닭을 모른다 */
        val kinds: List<Kind>,
    )

    const val MAX = 2

    /** 서버 글을 까닭으로 — 모르는 조각은 버린다 */
    fun kinds(reason: String): List<Kind> =
        reason.split(" · ").mapNotNull { KNOWN[it.trim()] }.distinct().take(MAX)

    /**
     * 러닝 한 줄의 안내.
     *
     * @param uploadState [com.stepup.android.data.local.UploadState] 이름 — SIGNED · REJECTED 만 본다(서버가 답한 러닝)
     * @param points 서버가 정한 적립액(SIGNED 의 pointsEarned)
     * @param verdictReason 서버 판정 까닭(SIGNED) — 판 20 전 러닝은 빈 글자
     * @param uploadError 서버가 거절한 까닭(REJECTED)
     */
    fun note(uploadState: String, points: Double, verdictReason: String, uploadError: String): Note? = when (uploadState) {
        "SIGNED" -> {
            val kinds = kinds(verdictReason)
            when {
                points <= 0.0 -> Note(zero = true, kinds = kinds)
                // 적립은 됐지만 줄어든(상한 · 내구도 등) 까닭이 있으면 그것만
                kinds.isNotEmpty() -> Note(zero = false, kinds = kinds)
                else -> null
            }
        }
        "REJECTED" -> Note(zero = true, kinds = kinds(uploadError))
        else -> null
    }
}
