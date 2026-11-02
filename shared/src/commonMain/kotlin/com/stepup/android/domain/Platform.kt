package com.stepup.android.domain

/**
 * 공용 규칙이 쓰는 JVM 함수의 대신(2026-11-02 KMP 2단계). 안드로이드에서 예전과 비트까지 같은 값을 내야 한다 —
 * 땅따먹기 칸(Territory)은 서버(Postgres)와 같은 칸 이름을 만들어야 하고, 기록 좌표 문자열도 같아야 한다.
 */

/** java.lang.Math.toRadians 와 같다(곱하는 상수가 같다 — PI / 180) */
internal fun toRadians(degrees: Double): Double = degrees * DEGREES_TO_RADIANS

/** java.lang.Math.toDegrees 와 같다(180 / PI) */
internal fun toDegrees(radians: Double): Double = radians * RADIANS_TO_DEGREES

private const val DEGREES_TO_RADIANS = 0.017453292519943295
private const val RADIANS_TO_DEGREES = 57.29577951308232

/** 지금 시각(1970-01-01 UTC 부터 ms) — 안드로이드는 System.currentTimeMillis() */
expect fun epochMillis(): Long

/**
 * 소수 둘째 자리 반올림(0.5 는 0 에서 먼 쪽) — 십진수로 본 값에서 자른다(java.math.BigDecimal.valueOf(v).setScale(2, HALF_UP)).
 * 이진 부동소수로 곱하고 나누면 2.675 처럼 경계 값이 달라져 서버 좌표(crew_coarse)와 어긋날 수 있어 기기마다 십진 계산을 쓴다.
 */
expect fun roundHalfUp2(value: Double): Double
