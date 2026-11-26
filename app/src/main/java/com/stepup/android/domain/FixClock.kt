package com.stepup.android.domain

/**
 * 위치를 **잰** 시각(2026-11-26 테스터 "백그라운드에서 GPS 가 잘못 그려진다 · 인식 문제 및 정확성").
 *
 * 러닝 서비스는 위치를 받은 순간의 폰 시계로 구간 속도를 쟀다. 화면이 꺼지면 휴대폰(특히 절전 설정)이 위치를 모아 한꺼번에 넘기는데,
 * 그러면 1분 동안 잰 점 스무 개가 같은 순간에 와 점 사이 시간이 0초 — 사람이 낼 수 없는 속도로 읽혀 경로 · 거리에서 빠지고
 * (지도에는 묶음 사이가 곧은 줄로), 과속 경고까지 떴다. 위치마다 잰 시각(부팅 뒤 시계 — 폰 시계를 바꿔도 흔들리지 않는다)을 폰 시계로 옮겨 쓴다.
 */
object FixClock {
    /** 이보다 오래된 위치는 잰 시각을 믿지 않는다(받은 시각을 쓴다) — 시계가 이상한 기기 */
    const val MAX_AGE_MS = 10 * 60_000L

    /**
     * [fixElapsedNanos](Location.elapsedRealtimeNanos — 부팅 뒤 잰 시각)를 폰 시계 ms 로. 모르면(0) · 미래 · 너무 오래됐으면 [wallNow].
     * [elapsedNowMs] 는 지금의 부팅 뒤 시각(SystemClock.elapsedRealtime).
     */
    fun wallTime(wallNow: Long, elapsedNowMs: Long, fixElapsedNanos: Long): Long {
        if (fixElapsedNanos <= 0L) return wallNow
        val ageMs = elapsedNowMs - fixElapsedNanos / 1_000_000
        return if (ageMs in 0..MAX_AGE_MS) wallNow - ageMs else wallNow
    }
}
