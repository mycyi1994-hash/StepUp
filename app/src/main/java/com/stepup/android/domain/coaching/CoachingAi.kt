package com.stepup.android.domain.coaching

/*
 * AI 코칭 — 서버가 돌려준 코칭 결과와 화면의 AI 칸 상태.
 *
 * 숫자는 코드가 만든다. 서버(0059)가 저장된 기록으로 근거(fact)와 허용 후보(candidate)를 숫자와 함께 만들고, AI 는 그중
 * 근거 id 하나 · 후보 id 하나를 고르고 짧은 해석 · 이유만 쓴다(지시서 7절). 화면의 "3km 구간이 첫 구간보다 40초 느렸어요"는
 * 고른 근거 id 를 앱의 번역 글자로 그린 것이다 — 모델이 쓴 숫자가 아니다. 모르는 id 는 그리지 않는다.
 */

/** 코칭 결과의 종류 — 기록 하나 · 한 주 · 다음 훈련 추천은 서로 다른 작업이다 */
enum class CoachKind(val key: String) {
    RUN("run"),
    WEEKLY("weekly"),
    RECOMMENDATION("recommendation"),
    ;

    companion object {
        fun of(key: String?): CoachKind? = entries.firstOrNull { it.key == key }
    }
}

/** 앱이 그릴 줄 아는 근거 · 후보 id 와 그 값의 이름. 서버(0059)와 같은 글자다 */
object CoachIds {
    /** 마지막 1km 구간 − 첫 1km 구간(초, +면 느렸다) — last_km · delta_sec */
    const val FACT_SPLIT_LAST_VS_FIRST = "run.split_last_vs_first"

    /** 1km 구간들이 고르다(가장 빠른 · 느린 차이 spread_sec 가 EVEN_SPLIT_SEC 이내) */
    const val FACT_SPLIT_EVEN = "run.split_even"

    /** 이 러닝의 평균 페이스 — pace_sec */
    const val FACT_AVG_PACE = "run.avg_pace"

    /** 러닝이 이어진 주 — weeks */
    const val FACT_WEEK_STREAK = "week.streak"

    /** 그 주의 러닝 횟수와 목표 — runs · goal(목표가 없으면 0) */
    const val FACT_WEEK_RUNS = "week.runs"

    /** 그 주의 거리 — meters */
    const val FACT_WEEK_DISTANCE = "week.distance"

    /** 첫 1km 를 조금 천천히 — pace_sec(이 러닝의 평균 페이스) */
    const val CAND_PACE_START_EASIER = "pace.start_easier"

    /** 지금 페이스를 이어가기 — pace_sec */
    const val CAND_PACE_KEEP = "pace.keep"

    /** 다음 주 계획(13) — runs · minutes */
    const val CAND_PLAN_NEXT_WEEK = "plan.next_week"

    /** 다음 훈련 템플릿 — "template." + 템플릿 id */
    const val CAND_TEMPLATE_PREFIX = "template."

    val knownFacts = setOf(
        FACT_SPLIT_LAST_VS_FIRST, FACT_SPLIT_EVEN, FACT_AVG_PACE, FACT_WEEK_STREAK, FACT_WEEK_RUNS, FACT_WEEK_DISTANCE,
    )

    fun knownCandidate(id: String): Boolean =
        id == CAND_PACE_START_EASIER || id == CAND_PACE_KEEP || id == CAND_PLAN_NEXT_WEEK ||
            (id.startsWith(CAND_TEMPLATE_PREFIX) && CoachingTemplates.byId(id.removePrefix(CAND_TEMPLATE_PREFIX)) != null)
}

/** 서버가 저장된 기록으로 만든 근거 하나 — 값은 정수(초 · 미터 · 횟수 · km 순번)뿐이다 */
data class CoachFact(val id: String, val values: Map<String, Long>)

/** 서버가 허용 목록으로 만든 다음 행동 후보 하나 */
data class CoachCandidate(val id: String, val values: Map<String, Long>)

/**
 * 저장된 코칭 결과. [explanation] · [actionReason] 은 AI 가 쓴 짧은 글(숫자 없음 — 서버가 검사했다).
 * [provider] 가 `mock` 이면 모의 공급자의 고정 문장이다 — 화면은 "모의 응답"으로 표시한다(실제 AI 처럼 보이지 않게).
 */
data class CoachResult(
    val id: Long,
    val kind: CoachKind,
    val generatedAt: Long,
    val locale: String,
    val facts: List<CoachFact>,
    val factId: String?,
    val explanation: String?,
    val candidates: List<CoachCandidate>,
    val candidateId: String?,
    val actionReason: String?,
    val limitation: String?,
    val provider: String,
) {
    /** 고른 근거 — 앱이 그릴 줄 아는 것만 */
    val fact: CoachFact? get() = facts.firstOrNull { it.id == factId && it.id in CoachIds.knownFacts }

    /** 고른 후보 — 앱이 아는 것만 */
    val candidate: CoachCandidate? get() = candidates.firstOrNull { it.id == candidateId && CoachIds.knownCandidate(it.id) }

    val mock: Boolean get() = provider == "mock"
}

/** 서버가 알려 준 코칭 작업 상태(0059 coaching_*_status · coaching_request) */
enum class CoachServerState(val key: String) {
    /** 운영자가 코칭을 꺼 두었다(준비 전 · 긴급 중지) */
    DISABLED("disabled"),
    NO_CONSENT("no_consent"),

    /** 서버에 이 계정의 그 러닝이 없다 */
    NOT_FOUND("not_found"),

    /** 기록이 기준에 못 미쳐 AI 를 부르지 않는다 */
    SKIPPED("skipped"),

    /** 아직 작업이 없다 — 요청할 수 있다 */
    NONE("none"),
    QUEUED("queued"),
    RUNNING("running"),
    READY("ready"),
    FAILED("failed"),

    /** 새로 만들 수 없다(사용자 · 서비스 한도) */
    LIMIT("limit"),
    ;

    companion object {
        fun of(key: String?): CoachServerState? = entries.firstOrNull { it.key == key }
    }
}

data class CoachStatus(
    val state: CoachServerState,
    val jobId: Long? = null,
    val reason: String? = null,
    val retryable: Boolean = false,
    val resetAt: Long? = null,
    val result: CoachResult? = null,
    /** 저장된 결과의 입력이 지금 기록과 다르다(기록 정정 · 시간대 변경) — 새 기록을 분석한 결과로 보이지 않는다 */
    val stale: Boolean = false,
)

/** 이 휴대폰에 저장해 둔 결과 — 계정 · 기록 · 언어별 */
data class CachedCoach(val result: CoachResult, val savedAt: Long)

/** 분석 칸(03 · 07 · 09 · 10 · 11 · 12)의 AI 부분. 숫자 · 그래프는 이 상태와 상관없이 먼저 그린다 */
sealed interface CoachAi {
    /** 서버에 상태를 묻는 중 — "분석 중"이라고 하지 않는다 */
    data object Loading : CoachAi

    /** AI 칸을 그리지 않는다 — 서버에 코칭이 꺼져 있다 */
    data object Hidden : CoachAi

    /** AI 이용 동의가 없다(05) — 러닝 · 기본 기록은 그대로 */
    data object NeedsConsent : CoachAi

    /** 계정의 기록이 아니다(로그인 전 · 옛 기록) — 외부로 보내지 않는다 */
    data object NeedsSignIn : CoachAi

    /** 이 휴대폰에는 저장됐고 서버 확인을 기다린다 — AI 는 아직 부르지 않았다("AI 분석 중"이 아니다) */
    data object AwaitingServer : CoachAi

    /** 서버가 이 러닝을 확인하지 않았다(거절 · 서버에 없음) — 분석하지 않는다 */
    data object NotVerified : CoachAi

    /** 기록이 기준에 못 미쳐 AI 를 건너뛰었다(09) */
    data class Skipped(val reason: String?) : CoachAi

    /** 직접 요청할 수 있다(자동 분석 꺼짐 — 기본) */
    data object Requestable : CoachAi

    /** 요청 접수 · 진행 중(07) — 화면을 떠나도 된다 */
    data class Working(val jobId: Long?, val running: Boolean) : CoachAi

    /** 결과(03) · 오프라인 저장 결과(10) */
    data class Ready(val result: CoachResult, val savedAt: Long?, val offline: Boolean, val stale: Boolean) : CoachAi

    /** 일시적 실패(11) — 기록은 그대로다 */
    data class Failed(val jobId: Long?, val retryable: Boolean) : CoachAi

    /** 새 분석 한도(12) — 저장된 분석 · 기본 기록은 그대로 */
    data class Limited(val resetAt: Long?, val saved: CoachResult?) : CoachAi

    /** 연결이 없고 저장된 분석도 없다 */
    data object Offline : CoachAi
}

/**
 * 기록 하나의 AI 칸 상태 — 판단 순서가 곧 규칙이다.
 *
 * 1. 계정 기록이 아니면 보내지 않는다. 2. 서버 확인 전이면 "서버 확인 대기"(AI 분석 중으로 꾸미지 않는다).
 * 3. 연결이 없으면 저장된 결과(생성 시각과 함께) 아니면 연결 안내 — 새로 부르지 않는다.
 * 4. 서버가 답하지 못했으면(오류) 저장된 결과 아니면 실패. 5. 나머지는 서버 상태대로.
 *
 * @param uploadState 기록의 서버 올리기 상태(PENDING · FAILED · SIGNED · REJECTED)
 * @param consent 이 휴대폰이 아는 동의(null = 아직 모름 — 서버가 정한다)
 * @param server 서버의 답(null = 아직 묻지 않았거나 묻지 못했다)
 */
fun coachAiFor(
    accountRecord: Boolean,
    uploadState: String,
    online: Boolean,
    consent: Boolean?,
    cache: CachedCoach?,
    server: CoachStatus?,
    serverFailed: Boolean,
): CoachAi {
    if (!accountRecord) return CoachAi.NeedsSignIn
    if (consent == false) return CoachAi.NeedsConsent
    when (uploadState) {
        "PENDING", "FAILED" -> return CoachAi.AwaitingServer
        "REJECTED" -> return CoachAi.NotVerified
    }
    if (!online) return cache?.let { CoachAi.Ready(it.result, it.savedAt, offline = true, stale = false) } ?: CoachAi.Offline
    if (server == null) {
        if (!serverFailed) return CoachAi.Loading
        return cache?.let { CoachAi.Ready(it.result, it.savedAt, offline = false, stale = false) } ?: CoachAi.Failed(null, retryable = true)
    }
    val saved = server.result ?: cache?.result
    return when (server.state) {
        CoachServerState.DISABLED -> saved?.let { CoachAi.Ready(it, cache?.savedAt, offline = false, stale = server.stale) } ?: CoachAi.Hidden
        CoachServerState.NO_CONSENT -> CoachAi.NeedsConsent
        CoachServerState.NOT_FOUND -> CoachAi.NotVerified
        CoachServerState.SKIPPED -> CoachAi.Skipped(server.reason)
        CoachServerState.NONE -> CoachAi.Requestable
        CoachServerState.QUEUED -> CoachAi.Working(server.jobId, running = false)
        CoachServerState.RUNNING -> CoachAi.Working(server.jobId, running = true)
        CoachServerState.READY -> server.result?.let { CoachAi.Ready(it, null, offline = false, stale = server.stale) }
            ?: CoachAi.Failed(server.jobId, retryable = true)
        CoachServerState.FAILED -> CoachAi.Failed(server.jobId, server.retryable)
        CoachServerState.LIMIT -> CoachAi.Limited(server.resetAt, saved)
    }
}
