package com.stepup.android.domain

/*
 * 러닝은 위치가 잡혀야 시작한다(2026-10-16 사용자 결정 — "GPS 안 되면 못 하는 거야" → "그래 다 되도록 만들어").
 *
 * 위치는 구글 지도와 같은 엔진(구글 위치 서비스 — GPS · 와이파이 · 기지국을 섞는다)으로 받는다. 실내에서는 와이파이로,
 * 밖에서는 GPS 로 잡힌다. 휴대폰이 지금 자리를 알면 시작하고, 아무것으로도 모르면 시작하지 않는다(시간만 기록하는 길은 없다).
 * 화면(RunGpsGate.kt)은 이 값을 그리기만 하고, 여기서는 무엇을 "잡혔다"로 볼지 · 오래 걸리면 무엇을 알릴지만 정한다.
 */

/** 시작 전 위치를 기다리는 동안 휴대폰이 알려 준 것 */
data class GpsWait(
    /** 받아들일 만한 위치를 받았다 — 3-2-1 로 간다(한 번 잡히면 그대로) */
    val fixed: Boolean = false,
    /** 마지막으로 받은 위치의 정확도(m). 아직 못 받았으면 null */
    val accuracyM: Float? = null,
    /** 기다린 시간(초) */
    val elapsedSec: Int = 0,
    /** 가짜 위치(모의 위치 앱)만 들어왔다 — 받아들이지 않는다 */
    val mock: Boolean = false,
    /** 휴대폰 위치 기능이 켜져 있다 */
    val locationOn: Boolean = true,
)

object GpsFixRule {
    /** 휴대폰이 이미 알던 위치는 이만큼 새것일 때만 그대로 쓴다(ms) */
    const val FRESH_MS = 30_000L

    /** 이만큼 기다려도 위치가 없으면 할 일을 알린다(초) */
    const val HINT_AFTER_SEC = 20

    /**
     * 잡혔다고 볼 위치인가 — 가짜가 아니고 [FRESH_MS] 안의 것이면 GPS · 와이파이 · 기지국 어느 것이든 받는다(구글 지도와 같다).
     * 오차가 큰 위치로 시작해도 거리는 [TrackFixRule] 이 거른다.
     */
    fun accepts(mock: Boolean, ageMs: Long): Boolean = !mock && ageMs <= FRESH_MS
}

/** 위치 대기 화면이 알릴 것 */
enum class GpsHint {
    /** 아직 기다리는 중 — 따로 알릴 것이 없다 */
    NONE,

    /** 위치 기능이 꺼졌다 — 위치 설정에서 켠다 */
    LOCATION_OFF,

    /** 가짜 위치만 들어온다 — 모의 위치 앱을 끈다 */
    MOCK,

    /** 오래 기다려도 위치가 없다 — 와이파이 · 데이터를 켜거나 하늘이 보이는 곳으로 */
    NO_FIX,
}

/** 지금 알릴 것 — 막는 까닭(꺼짐 · 가짜)은 바로, 위치가 없는 것은 [GpsFixRule.HINT_AFTER_SEC] 뒤에 */
fun gpsHintOf(w: GpsWait): GpsHint = when {
    w.fixed -> GpsHint.NONE
    !w.locationOn -> GpsHint.LOCATION_OFF
    w.mock -> GpsHint.MOCK
    w.elapsedSec < GpsFixRule.HINT_AFTER_SEC -> GpsHint.NONE
    else -> GpsHint.NO_FIX
}

/**
 * 달리는 중 위성 GPS 가 아닌 위치(구글 위치 서비스 · 와이파이 · 기지국)를 경로 · 거리에 넣을지 — 실내에서 제자리에 있어도
 * 와이파이 위치는 수십 m 씩 흔들린다. 그 떨림이 거리로 쌓이지 않게, 오차보다 확실히 크게 움직였을 때만 넣는다.
 * 위성 GPS(LocationManager 의 GPS)는 예전 규칙 그대로다(이 규칙을 거치지 않는다).
 */
object TrackFixRule {
    /** 오차가 이보다 크면(m) 경로 · 거리에 쓰지 않고 지도의 지금 자리만 옮긴다 — 기지국 위치 · 아주 흐린 와이파이 위치 */
    const val MAX_ACCURACY_M = 50f

    /** 오차를 알려 주지 않는 위치는 이만큼으로 본다(m) */
    const val UNKNOWN_ACCURACY_M = 30f

    /** 경로 점 사이 가장 짧은 거리(m) — 서비스가 경로에 점을 더하는 기준과 같다 */
    const val MIN_STEP_M = 8.0

    enum class Fix {
        /** 오차가 너무 크다 — 지도의 지금 자리만 */
        MAP_ONLY,

        /** 기준점에서 오차만큼도 안 움직였다 — 지도의 지금 자리만(기준점은 그대로) */
        HOLD,

        /** 경로 · 거리에 넣는다(기준점을 옮긴다) */
        COMMIT,
    }

    /**
     * @param anchorDistanceM 지금 기준점에서 이 위치까지(m). 기준점이 없으면(첫 점 · 끊긴 뒤 새 구간) null
     * @param anchorAccuracyM 기준점 위치의 오차(m)
     */
    fun decide(accuracyM: Float?, anchorDistanceM: Double?, anchorAccuracyM: Float?): Fix {
        val accuracy = accuracyM ?: UNKNOWN_ACCURACY_M
        if (accuracy > MAX_ACCURACY_M) return Fix.MAP_ONLY
        if (anchorDistanceM == null) return Fix.COMMIT
        // 두 점의 오차 중 큰 쪽의 두 배 — 흔들림만으로는 넘기 어렵고, 실제로 달리면 몇 초 안에 넘는다
        val threshold = maxOf(MIN_STEP_M, 2.0 * maxOf(accuracy, anchorAccuracyM ?: accuracy))
        return if (anchorDistanceM >= threshold) Fix.COMMIT else Fix.HOLD
    }
}
