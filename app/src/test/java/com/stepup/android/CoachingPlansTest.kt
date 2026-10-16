package com.stepup.android

import com.stepup.android.domain.coaching.CoachGoal
import com.stepup.android.domain.coaching.CoachPrefs
import com.stepup.android.domain.coaching.CoachingTemplates
import com.stepup.android.domain.coaching.StepRole
import com.stepup.android.domain.coaching.WorkoutFamily
import com.stepup.android.domain.coaching.paceAxis
import com.stepup.android.domain.coaching.clockText
import com.stepup.android.domain.coaching.kmText
import com.stepup.android.domain.coaching.paceText
import com.stepup.android.domain.coaching.weekPlan
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** AI 코칭의 템플릿 · 선호 · 다음 주 계획 후보 · 숫자 글자 · 그래프 축 */
class CoachingPlansTest {
    @Test fun `25분 도식은 걷기5 달리기15 걷기5 — 단계 합과 전체 시간이 맞는다`() {
        val easy = CoachingTemplates.byId("easy_25")!!
        assertEquals(listOf(5, 15, 5), easy.steps.map { it.minutes })
        assertEquals(25, easy.minutes)
        assertEquals(listOf(0, 5, 20, 25), easy.boundaries)
        assertEquals(listOf(StepRole.WARMUP, StepRole.EASY_RUN, StepRole.COOLDOWN), easy.steps.map { it.role })
    }

    @Test fun `모든 템플릿은 단계 합이 전체 시간이고 반복은 나누어 떨어진다`() {
        CoachingTemplates.all.forEach { template ->
            assertEquals(template.boundaries.last(), template.minutes)
            assertTrue(template.minutes in CoachPrefs.MINUTES)
            template.steps.filter { it.role == StepRole.RUN_WALK }.forEach { step ->
                assertEquals(step.minutes, (step.runMinutes + step.walkMinutes) * step.rounds)
            }
        }
        assertEquals(CoachingTemplates.all.size, CoachingTemplates.all.map { it.id }.toSet().size)
        // 고를 수 있는 시간마다 두 가족이 모두 있다
        CoachPrefs.MINUTES.forEach { minutes ->
            WorkoutFamily.entries.forEach { family -> assertTrue(CoachingTemplates.of(family, minutes) != null) }
        }
    }

    @Test fun `선호는 셋을 모두 고른 것만 — 미리 골라 두지 않는다`() {
        assertNull(CoachPrefs.of(null, 3, 25))
        assertNull(CoachPrefs.of(CoachGoal.STEADY, null, 25))
        assertNull(CoachPrefs.of(CoachGoal.STEADY, 5, 25))
        assertNull(CoachPrefs.of(CoachGoal.STEADY, 3, 30))
        assertEquals(CoachPrefs(CoachGoal.STEADY, 3, 25), CoachPrefs.of(CoachGoal.STEADY, 3, 25))
        assertEquals(CoachGoal.FINISH_5K, CoachGoal.of("finish_5k"))
    }

    @Test fun `기본 훈련은 고른 시간의 편안한 러닝`() {
        assertEquals("easy_25", CoachingTemplates.basic(CoachPrefs(CoachGoal.STEADY, 3, 25)).id)
        assertEquals("easy_15", CoachingTemplates.basic(CoachPrefs(CoachGoal.IMPROVE, 2, 15)).id)
    }

    @Test fun `다음 주 계획 — 주 3회는 월 수 금, 편안한 러닝과 걷기와 달리기를 번갈아`() {
        val monday = LocalDate.of(2026, 10, 19)
        val plan = weekPlan(monday, CoachPrefs(CoachGoal.STEADY, 3, 25))
        assertEquals(listOf(19, 21, 23), plan.map { it.date.dayOfMonth })
        assertEquals(listOf("easy_25", "walk_run_25", "easy_25"), plan.map { it.template.id })
        assertEquals(
            listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
            weekPlan(monday, CoachPrefs(CoachGoal.STEADY, 2, 15)).map { it.date.dayOfWeek },
        )
        assertEquals(
            listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY),
            weekPlan(monday, CoachPrefs(CoachGoal.STEADY, 4, 40)).map { it.date.dayOfWeek },
        )
    }

    @Test fun `숫자 글자 — 페이스 · 시계 · 거리`() {
        assertEquals("7'30\"", paceText(450.0))
        assertEquals("7'12\"", paceText(432.0))
        assertNull(paceText(0.0))
        assertNull(paceText(Double.NaN))
        assertNull(paceText(null))
        assertEquals("24:18", clockText(1458))
        assertEquals("1:08:24", clockText(4104))
        assertEquals("3.24", kmText(3240.0))
    }

    @Test fun `구간 그래프 축 — 빠른 값이 위, 7분에서 8분으로 내려가면 느려지는 방향`() {
        val axis = paceAxis(listOf(432, 446, 472), 450.0)
        assertEquals(420L, axis.fastSec)
        assertEquals(480L, axis.slowSec)
        assertEquals(listOf(420L, 450L, 480L), axis.ticks)
        assertTrue(axis.fraction(432.0) < axis.fraction(472.0))
        assertEquals(0.5f, axis.fraction(450.0), 1e-6f)
    }

    @Test fun `구간이 고르면 축이 과장하지 않게 1분 폭을 둔다`() {
        val axis = paceAxis(listOf(450), 450.0)
        assertEquals(60L, axis.slowSec - axis.fastSec)
        assertTrue(450 in axis.fastSec..axis.slowSec)
        val wide = paceAxis(listOf(400, 560), 480.0)
        assertTrue(wide.ticks.size in 2..5)
        assertTrue(wide.fastSec <= 400 && wide.slowSec >= 560)
    }
}
