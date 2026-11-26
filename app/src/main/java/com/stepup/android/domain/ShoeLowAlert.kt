package com.stepup.android.domain

import kotlin.math.floor

/**
 * 달리는 중 신발 내구도 알림(2026-11-26 테스터 "잠금화면에서 내구도가 몇 % 남았는지 막대로 · 20% / 10% 에서 알림음").
 *
 * 출발할 때 신은 신발의 남은 내구도 · 최대 내구도 · 1분마다 닳는 양([ShoeWearSnapshot])을 적어 두고, 달린 시간으로 지금 남은 %를 센다 —
 * 신발 화면 · 달리는 중 신발 칸([com.stepup.android.ui.screens.items.durabilityPercentDuringRun])과 같은 규칙(다 채운 1분마다 닳는다).
 * 정산 때 서버가 같은 규칙으로 뺀다. 화면 · 알림용일 뿐 서버에 보내지 않는다.
 */
data class ShoeWearSnapshot(
    /** 출발 때 남은 내구도(서버 값) */
    val durabilityNow: Int,
    /** 최대 내구도(서버 0059) */
    val durabilityMax: Int,
    /** 다 채운 1분마다 닳는 양 — 5 × 효율 포인트 배수(서버 0119 `wear_mult`) */
    val wearPerMinute: Double,
) {
    /** 달린 [activeSec] 초 뒤 남은 % (내려서, 0 ~ 100) */
    fun percentAt(activeSec: Long): Int {
        if (durabilityMax <= 0) return 0
        val wear = wearPerMinute * (activeSec.coerceAtLeast(0) / 60)
        val left = (durabilityNow - wear).coerceIn(0.0, durabilityMax.toDouble())
        return floor(100.0 * left / durabilityMax + 1e-9).toInt().coerceIn(0, 100)
    }
}

object ShoeLowAlert {
    /** 알리는 남은 % — 큰 것부터 */
    val THRESHOLDS = listOf(20, 10)

    /** 아직 알리지 않은 것으로 두는 바닥 값 — 모든 기준보다 크다 */
    const val NONE = 101

    /**
     * 출발할 때 이미 지난 기준은 알리지 않는다 — 15% 로 출발하면 10% 만, 8% 로 출발하면 둘 다 알리지 않는다(다 닳음은 따로 알린다).
     * 돌려준 값보다 작은 기준만 알린다.
     */
    fun startFloor(startPercent: Int): Int = THRESHOLDS.filter { it >= startPercent }.minOrNull() ?: NONE

    /**
     * [floor] 아래의 기준 가운데 [nowPercent] 가 닿은 것 — 한 번에 둘을 지났으면 낮은 것 하나만 알린다. 없으면 null.
     * 0% 는 알리지 않는다(다 닳음 알림이 따로 있다).
     */
    fun crossed(floor: Int, nowPercent: Int): Int? {
        if (nowPercent <= 0) return null
        return THRESHOLDS.filter { it < floor && nowPercent <= it }.minOrNull()
    }
}
