package com.stepup.android.domain

import kotlin.math.abs
import kotlin.math.min

// 코스 규칙(RunCourse)은 공용 모듈(shared)로 옮겼고, 기기 언어 설정을 따르는 화면 글자는 앱에 남긴다(2026-11-02 KMP 2단계)

/** 표시용 — 소수점 아래 자리가 흔들리지 않게 고정 */
fun formatKm(km: Double): String = "%.2f".format(min(abs(km), 999.99))
