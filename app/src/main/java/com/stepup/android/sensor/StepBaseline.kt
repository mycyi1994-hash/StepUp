package com.stepup.android.sensor

/** 걸음 센서 값이 어느 날의 몇 걸음인지 — 날짜를 함께 들고 다녀야 자정 무렵 어제 값을 오늘 줄에 적지 않는다 */
data class DayReading(
    /** 이 값이 속한 날(기기 시간대 epochDay) */
    val day: Long,
    val steps: Int,
) {
    /** [onDay] 의 걸음 — 이 값이 다른 날 것이면(자정을 넘겨 아직 새 걸음이 없으면) 0 */
    fun stepsOn(onDay: Long): Int = if (day == onDay) steps else 0
}

/**
 * 날짜가 바뀔 때 오늘로 넘겨줄 걸음 수를 정하는 규칙(센서 · 저장소와 떼어 시험한다).
 *
 * 어제 마지막으로 본 누적값은 `어제 기준점 + 어제 저장된 걸음`으로 되살릴 수 있다.
 * 지금 누적값에서 그걸 빼면 "마지막 측정 이후 걸은 수"가 나온다.
 *
 * 어제 재부팅했으면 기준점이 음수(−부팅 전 걸음)다. 그래도 `기준점 + 저장된 걸음`은
 * 마지막 누적값 그대로라(저장된 걸음 = 누적값 − 기준점) 같은 셈이 맞다.
 */
internal object StepBaseline {
    /** 자정을 넘겨 넘겨받을 수 있는 걸음 상한 — 활동적인 하루치 */
    const val CARRY_OVER_CAP = 12_000L

    /**
     * @param yesterdaySteps 어제([savedDay]) 저장된 걸음
     * @param rebooted 부팅 횟수가 바뀌었는가(둘 다 읽힌 경우만) — 그러면 지금 누적값은 부팅 뒤 걸음이다
     */
    fun carryOver(
        savedDay: Long,
        savedBaseline: Long,
        today: Long,
        cumulative: Long,
        yesterdaySteps: Int,
        rebooted: Boolean,
    ): Long {
        // 며칠 만에 열었다면 그 사이 걸음이 전부 오늘로 쏟아지므로 그냥 새로 센다
        if (savedDay != today - 1 || cumulative <= 0L) return 0L
        // 밤사이 재부팅 — 누적값은 0부터 다시 쌓였으니 그 전부가 마지막 측정 뒤 걸음이다
        if (rebooted) return cumulative.coerceAtMost(CARRY_OVER_CAP)
        val lastSeen = savedBaseline + yesterdaySteps.coerceAtLeast(0)
        if (cumulative < lastSeen) return 0L
        return (cumulative - lastSeen).coerceAtMost(CARRY_OVER_CAP)
    }

    /** 부팅 횟수로 본 재부팅 — 둘 중 하나라도 못 읽었으면 모른다(false) */
    fun rebooted(boot: Int, savedBoot: Int): Boolean = boot >= 0 && savedBoot >= 0 && boot != savedBoot
}
