package com.stepup.android.domain.coaching

import java.time.DayOfWeek
import java.time.LocalDate

/*
 * AI 코칭 — 선호(06) · 훈련 템플릿(01 · 02) · 다음 주 계획 후보(13).
 *
 * 훈련의 시간 · 순서 · 강도는 여기 적힌 템플릿에서만 나온다. AI 는 서버가 이 목록으로 만든 후보 중 하나를 고르고 이유만 쓴다
 * (지시서 3절 — LLM 이 처방을 만들지 않는다). 서버(0059 coaching.templates)에도 같은 목록이 있고
 * scripts/check-coaching-twin.py 가 둘을 맞대어 본다.
 *
 * ⚠ 템플릿 목록은 제품 검토 전 초안이다(지시서 14절 "훈련 템플릿 · 강도 범위"). 25분 = 걷기 5 · 달리기 15 · 걷기 5 는 시안의
 * 예시이고 모든 사용자에게 자동으로 주는 처방이 아니다 — AI 가 고르지 않은 템플릿은 화면에서 "기본 훈련"으로 부른다.
 */

/** 코칭 목표(06) — 사용자가 고른 것만 저장한다 */
enum class CoachGoal(val key: String) {
    STEADY("steady"),
    FINISH_5K("finish_5k"),
    IMPROVE("improve"),
    ;

    companion object {
        fun of(key: String?): CoachGoal? = entries.firstOrNull { it.key == key }
    }
}

/** 06 의 선택 — 셋을 모두 고른 것만 선호가 된다(미리 골라 두지 않는다) */
data class CoachPrefs(val goal: CoachGoal, val runsPerWeek: Int, val minutes: Int) {
    init {
        require(runsPerWeek in RUNS_PER_WEEK) { "runsPerWeek $runsPerWeek" }
        require(minutes in MINUTES) { "minutes $minutes" }
    }

    companion object {
        val RUNS_PER_WEEK = listOf(2, 3, 4)
        val MINUTES = listOf(15, 25, 40)

        /** 고르다 만 값은 선호가 아니다 — 하나라도 비었거나 목록 밖이면 null */
        fun of(goal: CoachGoal?, runsPerWeek: Int?, minutes: Int?): CoachPrefs? =
            if (goal != null && runsPerWeek in RUNS_PER_WEEK && minutes in MINUTES) CoachPrefs(goal, runsPerWeek!!, minutes!!) else null
    }
}

/** 훈련 한 단계의 할 일 */
enum class StepRole { WARMUP, EASY_RUN, RUN_WALK, COOLDOWN }

/**
 * 훈련 한 단계 — [minutes] 분. 걷기 · 달리기 반복([RUN_WALK])이면 [runMinutes] 달리고 [walkMinutes] 걷기를 [rounds] 번.
 */
data class WorkoutStep(val role: StepRole, val minutes: Int, val runMinutes: Int = 0, val walkMinutes: Int = 0, val rounds: Int = 0) {
    init {
        require(minutes > 0)
        require(role != StepRole.RUN_WALK || (rounds > 0 && (runMinutes + walkMinutes) * rounds == minutes))
    }

    /** 걷는 단계인가 — 막대 색 · 아이콘 */
    val walking: Boolean get() = role == StepRole.WARMUP || role == StepRole.COOLDOWN
}

enum class WorkoutFamily(val key: String) {
    /** 편안한 러닝 — 걷기 · 편하게 달리기 · 걷기 */
    EASY("easy"),

    /** 걷기와 달리기 — 걷기 · (달리기 · 걷기) 반복 · 걷기 */
    WALK_RUN("walk_run"),
}

data class WorkoutTemplate(val id: String, val family: WorkoutFamily, val steps: List<WorkoutStep>) {
    val minutes: Int get() = steps.sumOf { it.minutes }

    /** 단계마다 시작 · 끝 분(0, 5, 20, 25) — 02 의 막대 눈금 */
    val boundaries: List<Int> get() = steps.runningFold(0) { at, step -> at + step.minutes }
}

object CoachingTemplates {
    /** 템플릿 목록의 판 — 결과 · 계획에 같이 남겨 목록이 바뀐 뒤의 옛 계획을 알아본다 */
    const val CATALOG_VERSION = "templates-v1-draft"

    private fun easy(warm: Int, run: Int, cool: Int) = WorkoutTemplate(
        "easy_${warm + run + cool}", WorkoutFamily.EASY,
        listOf(WorkoutStep(StepRole.WARMUP, warm), WorkoutStep(StepRole.EASY_RUN, run), WorkoutStep(StepRole.COOLDOWN, cool)),
    )

    private fun walkRun(warm: Int, run: Int, walk: Int, rounds: Int, cool: Int) = WorkoutTemplate(
        "walk_run_${warm + (run + walk) * rounds + cool}", WorkoutFamily.WALK_RUN,
        listOf(
            WorkoutStep(StepRole.WARMUP, warm),
            WorkoutStep(StepRole.RUN_WALK, (run + walk) * rounds, run, walk, rounds),
            WorkoutStep(StepRole.COOLDOWN, cool),
        ),
    )

    val all: List<WorkoutTemplate> = listOf(
        easy(3, 9, 3), easy(5, 15, 5), easy(5, 30, 5),
        walkRun(3, 2, 1, 3, 3), walkRun(5, 2, 1, 5, 5), walkRun(5, 4, 1, 6, 5),
    )

    fun byId(id: String?): WorkoutTemplate? = all.firstOrNull { it.id == id }

    fun of(family: WorkoutFamily, minutes: Int): WorkoutTemplate? = all.firstOrNull { it.family == family && it.minutes == minutes }

    /** 추천이 없을 때 보이는 "기본 훈련" — 고른 시간의 편안한 러닝 */
    fun basic(prefs: CoachPrefs): WorkoutTemplate = checkNotNull(of(WorkoutFamily.EASY, prefs.minutes))
}

/** 계획의 하루 — 그날 할 템플릿 */
data class PlanDay(val date: LocalDate, val template: WorkoutTemplate)

/**
 * 다음 주 계획 후보(13) — 고른 횟수만큼 쉬는 날을 사이에 두고, 편안한 러닝과 걷기와 달리기를 번갈아.
 * 2회 월 · 목, 3회 월 · 수 · 금, 4회 월 · 화 · 목 · 토. 후보일 뿐이다 — 사용자가 "적용"하기 전에는 아무것도 바꾸지 않는다.
 */
fun weekPlan(monday: LocalDate, prefs: CoachPrefs): List<PlanDay> {
    require(monday.dayOfWeek == DayOfWeek.MONDAY)
    val days = when (prefs.runsPerWeek) {
        2 -> listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)
        3 -> listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        else -> listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY)
    }
    return days.mapIndexed { i, day ->
        val family = if (i % 2 == 0) WorkoutFamily.EASY else WorkoutFamily.WALK_RUN
        PlanDay(monday.plusDays((day.value - 1).toLong()), checkNotNull(CoachingTemplates.of(family, prefs.minutes)))
    }
}
