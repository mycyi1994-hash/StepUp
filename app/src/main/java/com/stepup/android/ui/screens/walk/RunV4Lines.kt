package com.stepup.android.ui.screens.walk

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.stepup.android.R
import com.stepup.android.domain.RewardEconomy
import com.stepup.android.domain.RunLuck
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.screens.items.durabilityEarnMinutes
import com.stepup.android.ui.screens.items.durabilityWornOut
import com.stepup.android.ui.screens.items.lowDurabilityMinutes

/*
 * 경제 4판(서버 0116)의 러닝 화면 한 줄들 — 출발 전 점검의 신발 · 적립 한도 줄, 결과 · 기록 상세의 운 줄.
 * 값은 모두 서버가 준 것(신발 내구도 · 남은 에너지 · 운)이고, 여기서는 어느 글을 보일지만 고른다.
 */

/** 출발 전 점검의 신발 줄 */
sealed interface ShoeCheckLine {
    /** 다 닳았다 — 이번 러닝은 보상이 없다(수리하면 다시 받는다) */
    data object WornOut : ShoeCheckLine

    /** 곧 닳는다 — 약 [minutes]분만 보상(신발 탭 · 상세의 수리 강조와 같은 기준 [lowDurabilityMinutes]) */
    data class Low(val minutes: Int) : ShoeCheckLine

    /** 이 신발은 약 [minutes]분 동안 적립된다 — 새 신발(가득)도 */
    data class Earns(val minutes: Int) : ShoeCheckLine

    /** 남은 분을 모른다(예전 서버 · 폰에만 있던 신발) — "달릴 준비가 됐어요" */
    data object Ready : ShoeCheckLine
}

/**
 * 고른 신발의 줄 — 남은 내구도 1 = 12초(1분마다 5 닳는다). 닳는 양에도 운이 있어(0116) 분은 "약"이다.
 * [shoe] 는 아직 서버가 빼지 않은 러닝의 닳음을 미리 뺀 화면 사본이다(WalkScreen 의 withPendingWear).
 */
fun shoeCheckLine(shoe: Sneaker): ShoeCheckLine {
    if (durabilityWornOut(shoe)) return ShoeCheckLine.WornOut
    lowDurabilityMinutes(shoe)?.let { return ShoeCheckLine.Low(it.coerceAtLeast(1)) }
    val minutes = durabilityEarnMinutes(shoe)
    return if (minutes != null && minutes > 0) ShoeCheckLine.Earns(minutes) else ShoeCheckLine.Ready
}

/**
 * 오늘 적립 한도(에너지)를 다 썼는가 — 남은 에너지를 읽었고([energy] 가 null 이 아니고) 그 에너지로 적립할 걸음이 0.
 * 다 썼어도 러닝은 그대로 시작한다(기록은 남고 적립만 없다).
 */
fun energyUsedUp(energy: Double?, energyEfficiency: Double = 1.0): Boolean =
    energy != null && RewardEconomy.earnableSteps(energy, energyEfficiency) <= 0

/** 운 한 줄 — "운 ×1.12 · 오늘 운이 좋았어요". 서버가 준 값이 없으면 null(줄을 두지 않는다) */
@Composable
internal fun runLuckText(luck: Double?): String? {
    luck ?: return null
    val factor = RunLuck.factor(luck)
    return stringResource(
        when (RunLuck.tone(luck)) {
            RunLuck.Tone.GOOD -> R.string.run_luck_good
            RunLuck.Tone.EVEN -> R.string.run_luck_even
            RunLuck.Tone.LOW -> R.string.run_luck_low
        },
        factor,
    )
}
