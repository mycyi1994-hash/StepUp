package com.stepup.android.domain

/**
 * 연속 목표 달성(스트릭) — 화면에 보일 값(2026-10-17 점검).
 *
 * 저장된 값(UserPrefs 의 streak · last_goal_met_day)은 목표를 채운 날에만 바뀐다(StepRepository.onSteps). 그래서 하루를
 * 놓쳐도 다음에 목표를 채울 때까지 옛 값("연속 12일")이 그대로 보였다. 날짜는 저장값과 같은 폰의 날짜(LocalDate.now().toEpochDay())다.
 */
object StreakRules {

    /**
     * 끊기지 않았으면 저장된 값, 끊겼으면 0.
     *
     * 오늘이나 어제 목표를 채웠으면 이어진다. 그제 채우고 어제를 스트릭 보호막이 덮었으면([shieldCoveredYesterday]) 오늘 채울 때
     * 이어 세므로(StepRepository.onSteps 와 같은 규칙) 아직 이어진 것으로 본다. 마지막으로 채운 날이 오늘보다 뒤면(폰 날짜를
     * 되돌림) 끊었다고 하지 않는다.
     */
    fun shown(stored: Int, lastMetDay: Long, today: Long, shieldCoveredYesterday: Boolean): Int {
        if (stored <= 0 || lastMetDay < 0) return 0
        val alive = lastMetDay >= today - 1 || (lastMetDay == today - 2 && shieldCoveredYesterday)
        return if (alive) stored else 0
    }

    /** 보호막이 이어 줄 수 있는 자리인가 — 그제 채우고 어제를 놓쳤다. 이때만 보호막을 물어본다(DB) */
    fun needsShield(lastMetDay: Long, today: Long): Boolean = lastMetDay >= 0 && lastMetDay == today - 2
}
