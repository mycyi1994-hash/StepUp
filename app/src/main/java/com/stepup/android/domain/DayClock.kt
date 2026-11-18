package com.stepup.android.domain

import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

/**
 * 오늘 날짜(epoch day)가 바뀌면 다시 알려 주는 흐름 — 홈의 "오늘 걸음" · 연속 일수가 자정이 지나도 첫 걸음 전까지 어제 값이던 것
 * (점검 2026-11-17 #24). 폰이 잠들면 기다리는 시간이 늘어나므로 자정까지 한 번에 재우지 않고 [pollMs] 마다 날짜를 본다.
 * 화면이 다시 보기 시작하면(구독) 바로 지금 날짜부터 낸다.
 */
object DayClock {
    const val POLL_MS = 60_000L

    fun ticker(
        today: () -> Long = { LocalDate.now().toEpochDay() },
        pollMs: Long = POLL_MS,
    ): Flow<Long> = flow {
        while (true) {
            emit(today())
            delay(pollMs)
        }
    }.distinctUntilChanged()
}
