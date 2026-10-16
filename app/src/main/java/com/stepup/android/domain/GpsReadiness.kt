package com.stepup.android.domain

/*
 * 러닝은 GPS 가 잡혀야 시작한다(2026-10-16 사용자 결정 — "GPS 안 되면 못 하는 거야").
 *
 * 권한 · 위치 기능 다음에 실제 GPS 신호를 기다린다. 화면(RunGpsGate.kt)은 이 값을 그리기만 하고, 여기서는
 * 무엇을 "잡혔다"로 볼지 · 오래 걸리면 무엇을 먼저 알릴지만 정한다. 시간만 기록하는 길은 없다.
 */

/** 시작 전 GPS 를 기다리는 동안 휴대폰이 알려 준 것 */
data class GpsWait(
    /** 받아들일 만한 GPS 위치를 받았다 — 3-2-1 로 간다(한 번 잡히면 그대로) */
    val fixed: Boolean = false,
    /** 마지막으로 받은 GPS 위치의 정확도(m). 아직 못 받았으면 null */
    val accuracyM: Float? = null,
    /** 신호를 받는 위성 수 · 위치 계산에 쓰는 위성 수. 휴대폰이 알려 주지 않으면 null */
    val satellitesSeen: Int? = null,
    val satellitesUsed: Int? = null,
    /** 기다린 시간(초) */
    val elapsedSec: Int = 0,
    /** 가짜 위치(모의 위치 앱)만 들어왔다 — 받아들이지 않는다 */
    val mock: Boolean = false,
    /** 휴대폰 위치 기능이 켜져 있다 */
    val locationOn: Boolean = true,
    /** GPS 가 켜져 있다 — Android 8 이하의 "절전" 위치 방식은 위치가 켜져 있어도 GPS 를 끈다 */
    val gpsOn: Boolean = true,
    /** 이 휴대폰에 GPS 가 있다 */
    val hasGps: Boolean = true,
)

object GpsFixRule {
    /** 정확도가 이 안이면 GPS 가 잡혔다고 본다(m) */
    const val MAX_ACCURACY_M = 50f

    /** 휴대폰이 이미 알던 GPS 위치는 이만큼 새것일 때만 그대로 쓴다(ms) */
    const val FRESH_MS = 30_000L

    /** 이만큼 기다려도 안 잡히면 까닭과 할 일을 알린다(초) */
    const val HINT_AFTER_SEC = 20

    /**
     * GPS 가 잡혔다고 볼 위치인가 — GPS 에서 왔고, 가짜가 아니고, [FRESH_MS] 안의 것이고, 정확도가 [MAX_ACCURACY_M] 안
     * (정확도를 알려 주지 않는 기기는 받는다 — 그 때문에 영영 시작하지 못하게 하지 않는다).
     */
    fun accepts(fromGps: Boolean, mock: Boolean, accuracyM: Float?, ageMs: Long): Boolean =
        fromGps && !mock && ageMs <= FRESH_MS && (accuracyM == null || accuracyM <= MAX_ACCURACY_M)
}

/** GPS 대기 화면이 먼저 알릴 것 */
enum class GpsHint {
    /** 아직 기다리는 중 — 따로 알릴 것이 없다 */
    NONE,

    /** 이 휴대폰에 GPS 가 없다 — 러닝을 시작할 수 없다 */
    NO_GPS,

    /** 위치 기능이 꺼졌다 — 위치 설정에서 켠다 */
    LOCATION_OFF,

    /** 위치는 켜져 있지만 GPS 가 꺼져 있다(Android 8 이하 절전 방식) — 위치 설정에서 바꾼다 */
    GPS_OFF,

    /** 가짜 위치만 들어온다 — 모의 위치 앱을 끈다 */
    MOCK,

    /** 받는 위성이 하나도 없다 — 실내 · 지하 */
    NO_SATELLITES,

    /** 위치는 오지만 아직 정확하지 않다 — 건물 · 나무에서 떨어진다 */
    WEAK,

    /** 위성은 보이는데 첫 위치가 늦다 — 유심 없는 휴대폰 · 오래 안 쓴 GPS */
    SLOW,
}

/** 지금 알릴 것 — 막는 까닭(GPS 없음 · 꺼짐 · 가짜)은 바로, 기다림의 까닭은 [GpsFixRule.HINT_AFTER_SEC] 뒤에 */
fun gpsHintOf(w: GpsWait): GpsHint = when {
    w.fixed -> GpsHint.NONE
    !w.hasGps -> GpsHint.NO_GPS
    !w.locationOn -> GpsHint.LOCATION_OFF
    !w.gpsOn -> GpsHint.GPS_OFF
    w.mock -> GpsHint.MOCK
    w.elapsedSec < GpsFixRule.HINT_AFTER_SEC -> GpsHint.NONE
    w.accuracyM != null -> GpsHint.WEAK
    w.satellitesSeen == 0 -> GpsHint.NO_SATELLITES
    else -> GpsHint.SLOW
}
