package com.stepup.android

import com.stepup.android.domain.coaching.CachedCoach
import com.stepup.android.domain.coaching.CoachAi
import com.stepup.android.domain.coaching.CoachCandidate
import com.stepup.android.domain.coaching.CoachFact
import com.stepup.android.domain.coaching.CoachIds
import com.stepup.android.domain.coaching.CoachKind
import com.stepup.android.domain.coaching.CoachResult
import com.stepup.android.domain.coaching.CoachServerState
import com.stepup.android.domain.coaching.CoachStatus
import com.stepup.android.domain.coaching.coachAiFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 분석 칸의 AI 상태 — 상태표(지시서 8절): 미동의 · 서버 확인 대기 · 처리 중 · 품질 부족 · 오프라인(캐시 있음 · 없음) ·
 * 실패 · 한도 · 오래된 캐시. 어느 경우에도 없는 AI 문장을 만들지 않는다.
 */
class CoachingAiStateTest {
    private val result = CoachResult(
        id = 7, kind = CoachKind.RUN, generatedAt = 1_760_400_000_000, locale = "ko",
        facts = listOf(CoachFact(CoachIds.FACT_SPLIT_LAST_VS_FIRST, mapOf("last_km" to 3L, "delta_sec" to 40L))),
        factId = CoachIds.FACT_SPLIT_LAST_VS_FIRST, explanation = null,
        candidates = listOf(CoachCandidate(CoachIds.CAND_PACE_START_EASIER, mapOf("pace_sec" to 450L))),
        candidateId = CoachIds.CAND_PACE_START_EASIER, actionReason = null, limitation = null, provider = "mock",
    )
    private val cache = CachedCoach(result, savedAt = 1_760_400_100_000)

    private fun ai(
        account: Boolean = true,
        upload: String = "SIGNED",
        online: Boolean = true,
        consent: Boolean? = true,
        cached: CachedCoach? = null,
        server: CoachStatus? = null,
        failed: Boolean = false,
    ) = coachAiFor(account, upload, online, consent, cached, server, failed)

    @Test fun `계정 기록이 아니면 보내지 않는다 — 옛 기록 · 손님`() {
        assertEquals(CoachAi.NeedsSignIn, ai(account = false, server = CoachStatus(CoachServerState.READY, result = result)))
    }

    @Test fun `미동의면 동의 화면 — 서버가 미동의라고 해도 같다`() {
        assertEquals(CoachAi.NeedsConsent, ai(consent = false))
        assertEquals(CoachAi.NeedsConsent, ai(consent = null, server = CoachStatus(CoachServerState.NO_CONSENT)))
    }

    @Test fun `서버 확인 전에는 분석 중이 아니라 서버 확인 대기`() {
        assertEquals(CoachAi.AwaitingServer, ai(upload = "PENDING"))
        assertEquals(CoachAi.AwaitingServer, ai(upload = "FAILED", server = CoachStatus(CoachServerState.QUEUED, jobId = 3)))
        assertEquals(CoachAi.NotVerified, ai(upload = "REJECTED"))
        assertEquals(CoachAi.NotVerified, ai(server = CoachStatus(CoachServerState.NOT_FOUND)))
    }

    @Test fun `오프라인 — 저장된 분석이 있으면 생성 시각과 함께, 없으면 연결 안내`() {
        val offline = ai(online = false, cached = cache) as CoachAi.Ready
        assertTrue(offline.offline)
        assertEquals(cache.savedAt, offline.savedAt)
        assertEquals(CoachAi.Offline, ai(online = false))
    }

    @Test fun `서버에 묻는 중에는 분석 중이라고 하지 않는다`() {
        assertEquals(CoachAi.Loading, ai())
    }

    @Test fun `서버가 답하지 못하면 저장된 분석, 없으면 재시도할 수 있는 실패`() {
        assertTrue(ai(failed = true, cached = cache) is CoachAi.Ready)
        assertEquals(CoachAi.Failed(null, retryable = true), ai(failed = true))
    }

    @Test fun `서버 상태 — 요청 가능 · 접수 · 진행 · 결과 · 실패 · 한도 · 품질 부족`() {
        assertEquals(CoachAi.Requestable, ai(server = CoachStatus(CoachServerState.NONE)))
        assertEquals(CoachAi.Working(4, running = false), ai(server = CoachStatus(CoachServerState.QUEUED, jobId = 4)))
        assertEquals(CoachAi.Working(4, running = true), ai(server = CoachStatus(CoachServerState.RUNNING, jobId = 4)))
        assertEquals(CoachAi.Failed(4, retryable = true), ai(server = CoachStatus(CoachServerState.FAILED, jobId = 4, retryable = true)))
        assertEquals(CoachAi.Skipped("no_splits"), ai(server = CoachStatus(CoachServerState.SKIPPED, reason = "no_splits")))
        val limited = ai(cached = cache, server = CoachStatus(CoachServerState.LIMIT, resetAt = 99)) as CoachAi.Limited
        assertEquals(99L, limited.resetAt)
        assertEquals(result, limited.saved)
    }

    @Test fun `결과는 입력이 바뀌었으면 오래된 결과로 표시한다`() {
        val ready = ai(server = CoachStatus(CoachServerState.READY, result = result, stale = true)) as CoachAi.Ready
        assertTrue(ready.stale)
        // 서버는 새 작업이 없다고(none) 하면서 옛 결과를 함께 준다 — 결과를 보이되 오래된 결과로
        val none = ai(server = CoachStatus(CoachServerState.NONE, result = result, stale = true)) as CoachAi.Ready
        assertTrue(none.stale)
        // 판만 바뀌었으면(입력 같음) 그냥 결과
        val sameInput = ai(server = CoachStatus(CoachServerState.NONE, result = result, stale = false)) as CoachAi.Ready
        assertTrue(!sameInput.stale)
    }

    @Test fun `결과라고 했는데 내용이 없으면 성공으로 보이지 않는다`() {
        assertEquals(CoachAi.Failed(5, retryable = true), ai(server = CoachStatus(CoachServerState.READY, jobId = 5)))
    }

    @Test fun `운영자가 꺼 두면 AI 칸을 숨기고, 이미 저장된 분석은 읽게 둔다`() {
        assertEquals(CoachAi.Hidden, ai(server = CoachStatus(CoachServerState.DISABLED)))
        assertTrue(ai(server = CoachStatus(CoachServerState.DISABLED, result = result)) is CoachAi.Ready)
    }

    @Test fun `모르는 근거 · 후보 id 는 그리지 않는다`() {
        val odd = result.copy(factId = "run.made_up", candidateId = "pace.free_form")
        assertNull(odd.fact)
        assertNull(odd.candidate)
        assertEquals(40L, result.fact!!.values["delta_sec"])
        assertTrue(result.mock)
        assertTrue(CoachIds.knownCandidate("template.easy_25"))
        assertTrue(!CoachIds.knownCandidate("template.easy_26"))
    }
}
