package com.stepup.android.domain

import kotlin.math.abs
import kotlin.math.min

// 코스 거리 글자 — 앱이 담는 네 언어(ko · en · ja · zh)는 모두 소수점이 "." 라 기기 언어와 상관없이 같다(2026-11-03 공용으로)

/** 표시용 — 소수점 아래 자리가 흔들리지 않게 고정 */
fun formatKm(km: Double): String = formatFixed(min(abs(km), 999.99), 2)
