package com.stepup.android.data.repo

import kotlinx.coroutines.flow.StateFlow

/**
 * 오늘 걸음을 세는 기기 센서(안드로이드 StepTracker · iOS 걸음 측정) — StepRepository 가 이것만 본다(2026-11-03 iOS C4).
 */
interface DailyStepSource {
    val todaySteps: StateFlow<Int>
    val isAvailable: Boolean
    fun start()
    /** 시험용 걸음(데모 · 기기 검사) */
    fun simulateSteps(count: Int)
}
