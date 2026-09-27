package com.stepup.android

import com.stepup.android.domain.GoalAttempt
import com.stepup.android.domain.RunGoal
import com.stepup.android.domain.RunPlan
import com.stepup.android.domain.RunPlans
import com.stepup.android.service.RunSaveStatus
import com.stepup.android.service.WalkSessionService
import com.stepup.android.service.WalkSessionState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunPlanTest {
    @After fun reset() = RunPlans.clear()

    @Test
    fun timeGoalCountsSecondsAndDistanceGoalCountsKilometres() {
        assertFalse(RunGoal.TEN_MIN.reached(599, 5.0))
        assertTrue(RunGoal.TEN_MIN.reached(600, 0.0))
        assertEquals(0.5f, RunGoal.TEN_MIN.fraction(300, 0.0), 1e-6f)

        assertFalse(RunGoal.ONE_KM.reached(3_600, 0.99))
        assertTrue(RunGoal.ONE_KM.reached(10, 1.0))
        assertTrue(RunGoal.THREE_KM.reached(10, 3.2))
        assertEquals(1f, RunGoal.THREE_KM.fraction(0, 9.0), 1e-6f)
    }

    @Test
    fun planStaysWithTheRunItWasChosenFor() {
        RunPlans.set(RunPlan.Goal(RunGoal.ONE_KM))
        // 아직 러닝을 만나지 않았다 — 어느 러닝이든 이 계획
        assertEquals(RunPlan.Goal(RunGoal.ONE_KM), RunPlans.planFor(100))
        RunPlans.bind(100)
        assertEquals(RunPlan.Goal(RunGoal.ONE_KM), RunPlans.planFor(100))
        // 결과 화면은 끝난 러닝의 시작 시각으로 같은 계획을 읽는다
        assertEquals(RunPlan.Goal(RunGoal.ONE_KM), RunPlans.planFor(100))
        // 다른 길(모임 · 이어 달리기)로 시작한 다른 러닝은 자유 러닝이다
        assertEquals(RunPlan.Free, RunPlans.planFor(200))
        RunPlans.bind(200)
        assertEquals(RunPlan.Free, RunPlans.current.value)
    }

    @Test
    fun attemptsKeepOneRowPerRunNewestFirst() {
        val a = GoalAttempt(1_000, RunGoal.TEN_MIN, false, 384, 0.82)
        val b = GoalAttempt(2_000, RunGoal.ONE_KM, true, 492, 1.0)
        var rows = GoalAttempt.merge(emptyList(), a)
        rows = GoalAttempt.merge(rows, b)
        rows = GoalAttempt.merge(rows, a.copy(achieved = true, elapsedSec = 600))
        assertEquals(listOf(2_000L, 1_000L), rows.map { it.startedAt })
        assertTrue(rows.last().achieved)

        val decoded = GoalAttempt.decodeAll(rows.joinToString("\n") { it.encode() })
        assertEquals(rows, decoded)
        assertEquals(emptyList<GoalAttempt>(), GoalAttempt.decodeAll("garbage\n\n1\tunknown\t1\t2\t3"))
    }

    @Test
    fun attemptHistoryIsBounded() {
        var rows = emptyList<GoalAttempt>()
        repeat(GoalAttempt.KEEP + 10) { i -> rows = GoalAttempt.merge(rows, GoalAttempt(i.toLong(), RunGoal.TEN_MIN, true, 600, 1.0)) }
        assertEquals(GoalAttempt.KEEP, rows.size)
        assertEquals((GoalAttempt.KEEP + 9).toLong(), rows.first().startedAt)
    }

    @Test
    fun onlySoloUnsavedRunsCanBeDiscarded() {
        assertTrue(WalkSessionService.canDiscard(WalkSessionState(isActive = true)))
        assertFalse(WalkSessionService.canDiscard(WalkSessionState()))
        assertFalse(WalkSessionService.canDiscard(WalkSessionState(isActive = true, partySize = 3)))
        assertFalse(WalkSessionService.canDiscard(WalkSessionState(isActive = true, saveStatus = RunSaveStatus.SAVING)))
        assertFalse(WalkSessionService.canDiscard(WalkSessionState(isActive = true, saveStatus = RunSaveStatus.FAILED)))
    }

    @Test
    fun gpsLostNeedsSilenceWhileWalking() {
        // 제자리에 서 있으면(걸음이 거의 없으면) 좌표가 안 와도 끊김이 아니다
        assertFalse(com.stepup.android.service.GpsSignal.isLost(120_000, 5))
        // 걷는데 30초 넘게 좌표가 없으면 끊김
        assertTrue(com.stepup.android.service.GpsSignal.isLost(31_000, 60))
        assertFalse(com.stepup.android.service.GpsSignal.isLost(29_000, 60))
    }
}
