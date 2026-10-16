package com.stepup.android

import com.stepup.android.data.remote.CoachPlanApplied
import com.stepup.android.data.remote.CoachServerPrefs
import com.stepup.android.data.remote.CoachServerSnapshot
import com.stepup.android.data.repo.CoachLoad
import com.stepup.android.domain.coaching.CachedCoach
import com.stepup.android.domain.coaching.CoachAi
import com.stepup.android.domain.coaching.CoachCandidate
import com.stepup.android.domain.coaching.CoachIds
import com.stepup.android.domain.coaching.CoachKind
import com.stepup.android.domain.coaching.CoachResult
import com.stepup.android.domain.coaching.CoachServerState
import com.stepup.android.domain.coaching.CoachStatus
import com.stepup.android.ui.screens.coaching.CoachAccount
import com.stepup.android.ui.screens.coaching.TodaySource
import com.stepup.android.ui.screens.coaching.accountOf
import com.stepup.android.ui.screens.coaching.aiOf
import com.stepup.android.ui.screens.coaching.providerKnown
import com.stepup.android.ui.screens.coaching.todayWorkout
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 코칭 화면 상태 — 계정 상태 겹치기 · 서버 답 → AI 칸 · 오늘 카드의 훈련 고르기. 화면이 없는 값을 만들어 보이지 않는지 본다
 * (목표가 없으면 훈련 없음 · 오래된 추천은 버림 · 다른 계정의 상태는 쓰지 않음).
 */
class CoachingScreenStateTest {
    private val me = "account:u1"
    private val other = "account:u2"
    private val today: LocalDate = LocalDate.of(2026, 10, 16) // 금요일
    private val monday: LocalDate = LocalDate.of(2026, 10, 12)

    private fun snapshot(
        consented: Boolean = true,
        prefs: CoachServerPrefs? = CoachServerPrefs("steady", 3, 25, false, false, null, 4),
        plans: List<CoachPlanApplied> = emptyList(),
    ) = CoachServerSnapshot(
        enabled = true, provider = "mock", consentVersion = "draft-2026-10-16", consented = consented, prefs = prefs, plans = plans,
    )

    private fun rec(candidateId: String, reason: String? = "가볍게 이어가요", provider: String = "mock") = CoachResult(
        id = 9, kind = CoachKind.RECOMMENDATION, generatedAt = 1_760_500_000_000, locale = "ko",
        facts = emptyList(), factId = null, explanation = null,
        candidates = listOf(CoachCandidate(candidateId, emptyMap())), candidateId = candidateId,
        actionReason = reason, limitation = null, provider = provider,
    )

    // ── 계정 상태 ────────────────────────────────────────────────

    @Test
    fun account_waits_for_owner_and_refuses_guests() {
        assertEquals(CoachAccount.Loading, accountOf(null, null, emptyMap()))
        assertEquals(CoachAccount.SignIn, accountOf("guest", null, emptyMap()))
        assertEquals(CoachAccount.SignIn, accountOf("legacy", me to CoachAccount.Ready(snapshot()), mapOf("legacy" to snapshot())))
    }

    @Test
    fun account_never_uses_another_accounts_answer() {
        // 앞 계정에 대해 읽은 Ready 는 새 계정에서 쓰지 않는다 — 아직 읽는 중
        assertEquals(CoachAccount.Loading, accountOf(me, other to CoachAccount.Ready(snapshot()), emptyMap()))
        assertEquals(CoachAccount.Loading, accountOf(me, null, mapOf(other to snapshot())))
    }

    @Test
    fun account_overlays_newer_server_state_only_on_ready() {
        val old = snapshot(consented = false)
        val fresh = snapshot(consented = true)
        assertEquals(CoachAccount.Ready(fresh), accountOf(me, me to CoachAccount.Ready(old), mapOf(me to fresh)))
        // 못 읽었으면 못 읽었다고(예전 사본을 지금 상태로 보이지 않는다)
        assertEquals(CoachAccount.Offline, accountOf(me, me to CoachAccount.Offline, mapOf(me to fresh)))
        assertEquals(CoachAccount.Failed, accountOf(me, me to CoachAccount.Failed, mapOf(me to fresh)))
        // 아직 이 화면이 읽기 전이면 다른 화면이 읽은 것을 쓴다
        assertEquals(CoachAccount.Ready(fresh), accountOf(me, null, mapOf(me to fresh)))
    }

    // ── 서버 답 → AI 칸 ─────────────────────────────────────────

    private val runResult = rec(CoachIds.CAND_PACE_START_EASIER).copy(kind = CoachKind.RUN)
    private val cached = CachedCoach(runResult, savedAt = 1_760_500_100_000)

    @Test
    fun answers_map_to_distinct_states() {
        assertEquals(CoachAi.Working(5, running = false), aiOf(CoachLoad.Ready(CoachStatus(CoachServerState.QUEUED, jobId = 5)), null, true, "SIGNED", true))
        assertEquals(CoachAi.Hidden, aiOf(CoachLoad.NotConfigured, cached, true, "SIGNED", true))
        assertEquals(CoachAi.NeedsSignIn, aiOf(CoachLoad.SignInRequired, cached, true, "SIGNED", true))
        assertEquals(CoachAi.Offline, aiOf(CoachLoad.Offline, null, true, "SIGNED", true))
    }

    @Test
    fun offline_shows_the_saved_copy_with_its_time() {
        val ai = aiOf(CoachLoad.Offline, cached, true, "SIGNED", true)
        assertTrue(ai is CoachAi.Ready)
        ai as CoachAi.Ready
        assertTrue(ai.offline)
        assertEquals(cached.savedAt, ai.savedAt)
    }

    @Test
    fun refused_request_without_copy_is_not_retryable() {
        assertEquals(CoachAi.Failed(null, retryable = false), aiOf(CoachLoad.Failed("not_signed", rejected = true), null, true, "SIGNED", true))
        // 일시적 실패는 다시 시도할 수 있고, 저장된 사본이 있으면 그것을 보인다
        assertEquals(CoachAi.Failed(null, retryable = true), aiOf(CoachLoad.Failed("timeout"), null, true, "SIGNED", true))
        assertTrue(aiOf(CoachLoad.Failed("timeout"), cached, true, "SIGNED", true) is CoachAi.Ready)
    }

    @Test
    fun not_yet_verified_record_is_never_called_analysing() {
        // 서버가 무슨 답을 줘도 서버 확인 전 기록은 "서버 확인 대기"다
        val queued = CoachLoad.Ready(CoachStatus(CoachServerState.RUNNING, jobId = 1))
        assertEquals(CoachAi.AwaitingServer, aiOf(queued, null, true, "PENDING", true))
        assertEquals(CoachAi.NotVerified, aiOf(queued, null, true, "REJECTED", true))
    }

    // ── 오늘 카드 ───────────────────────────────────────────────

    @Test
    fun no_goal_means_no_workout() {
        assertNull(todayWorkout(snapshot(prefs = null), today, CoachAi.Hidden))
        assertNull(todayWorkout(snapshot(prefs = CoachServerPrefs("steady", 3, null, false, false, null, 1)), today, CoachAi.Hidden))
        assertNull(todayWorkout(null, today, CoachAi.Loading))
    }

    @Test
    fun basic_workout_follows_chosen_minutes_and_is_labelled_basic() {
        val workout = todayWorkout(snapshot(), today, CoachAi.Requestable)!!
        assertEquals(TodaySource.BASIC, workout.source)
        assertEquals("easy_25", workout.template.id)
        assertNull(workout.reason)
    }

    @Test
    fun ai_pick_needs_a_known_fresh_template() {
        val picked = todayWorkout(snapshot(), today, CoachAi.Ready(rec("template.walk_run_25"), null, offline = false, stale = false))!!
        assertEquals(TodaySource.AI, picked.source)
        assertEquals("walk_run_25", picked.template.id)
        assertEquals("가볍게 이어가요", picked.reason)
        assertTrue(picked.mock)
        // 오래된 추천 · 모르는 템플릿 · 페이스 후보는 기본 훈련으로
        val stale = todayWorkout(snapshot(), today, CoachAi.Ready(rec("template.walk_run_25"), null, offline = false, stale = true))!!
        assertEquals(TodaySource.BASIC, stale.source)
        val unknown = todayWorkout(snapshot(), today, CoachAi.Ready(rec("template.sprint_99"), null, offline = false, stale = false))!!
        assertEquals(TodaySource.BASIC, unknown.source)
        val pace = todayWorkout(snapshot(), today, CoachAi.Ready(rec(CoachIds.CAND_PACE_KEEP), null, offline = false, stale = false))!!
        assertEquals(TodaySource.BASIC, pace.source)
    }

    @Test
    fun applied_plan_wins_on_its_days_only() {
        val plan = CoachPlanApplied(monday, listOf(monday to "easy_25", monday.plusDays(2) to "walk_run_25", today to "walk_run_40"))
        val onPlanDay = todayWorkout(snapshot(plans = listOf(plan)), today, CoachAi.Ready(rec("template.easy_15"), null, false, false))!!
        assertEquals(TodaySource.PLAN, onPlanDay.source)
        assertEquals("walk_run_40", onPlanDay.template.id)
        assertNull(onPlanDay.reason)
        // 계획에 오늘이 없으면 계획을 만들어 넣지 않는다 — 추천 · 기본으로
        val restDay = todayWorkout(snapshot(plans = listOf(plan)), today.plusDays(1), CoachAi.Hidden)!!
        assertEquals(TodaySource.BASIC, restDay.source)
        // 다른 주의 계획은 쓰지 않는다
        val lastWeek = CoachPlanApplied(monday.minusWeeks(1), listOf(today.minusWeeks(1) to "walk_run_40"))
        assertEquals(TodaySource.BASIC, todayWorkout(snapshot(plans = listOf(lastWeek)), today, CoachAi.Hidden)!!.source)
    }

    @Test
    fun consent_needs_a_named_provider() {
        assertTrue(providerKnown("anthropic"))
        assertTrue(providerKnown("mock"))
        assertFalse(providerKnown("none"))
        assertFalse(providerKnown(""))
    }
}
