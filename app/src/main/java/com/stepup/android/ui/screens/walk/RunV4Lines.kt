package com.stepup.android.ui.screens.walk

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.stepup.android.R
import com.stepup.android.domain.RewardEconomy
import com.stepup.android.domain.RunLuck
import com.stepup.android.domain.ShoeRules
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.wornPercent
import com.stepup.android.ui.screens.items.durabilityEarnMinutes
import com.stepup.android.ui.screens.items.durabilityWornOut
import com.stepup.android.ui.screens.items.lowDurabilityMinutes
import com.stepup.android.ui.screens.items.rememberShoeRules

/*
 * 경제 4판(서버 0116)의 러닝 화면 한 줄들 — 출발 전 점검의 신발 · 적립 한도 줄, 결과 · 기록 상세의 운 줄.
 * 값은 모두 서버가 준 것(신발 내구도 · 남은 에너지 · 운)이고, 여기서는 어느 글을 보일지만 고른다.
 */

/**
 * 출발 전 점검의 신발 줄. 어느 줄이든 출발은 막지 않는다 — 글만 다르다.
 * 닳은 뒤에도 적립하는 규칙(서버 0119 — [com.stepup.android.domain.StatRule.earnsWhenWorn])이면 닳은 신발 · 수명이 끝난 뒤의 걸음은
 * 세기는 하고 일부(N%)만 적립된다. 그 전 규칙이면 적립이 없다.
 */
sealed interface ShoeCheckLine {
    /** 다 닳았다 — 그대로 달리면 N%만 적립(예전 규칙이면 보상 없음). 수리하면 다시 100% */
    data object WornOut : ShoeCheckLine

    /** 곧 닳는다 — 약 [minutes]분 뒤부터 N%만(예전 규칙이면 약 [minutes]분만 보상). 신발 탭 · 상세의 수리 강조와 같은 기준 [lowDurabilityMinutes] */
    data class Low(val minutes: Int) : ShoeCheckLine

    /** 이 신발은 약 [minutes]분 동안 가득 적립된다 — 새 신발(가득)도 */
    data class Earns(val minutes: Int) : ShoeCheckLine

    /** 남은 분을 모른다(예전 서버 · 폰에만 있던 신발) — "달릴 준비가 됐어요" */
    data object Ready : ShoeCheckLine
}

/**
 * 고른 신발의 줄 — 남은 내구도 1 = 12초 ÷ 효율 포인트 배수(1분마다 5 × 배수 닳는다, 서버 0119 — 포인트가 없으면 12초).
 * 닳는 양에도 운이 있어(0116) 분은 "약"이다.
 * [shoe] 는 아직 서버가 빼지 않은 러닝의 닳음을 미리 뺀 화면 사본이다(WalkScreen 의 withPendingWear).
 */
fun shoeCheckLine(shoe: Sneaker, rules: ShoeRules = ShoeRules.FALLBACK): ShoeCheckLine {
    if (durabilityWornOut(shoe)) return ShoeCheckLine.WornOut
    lowDurabilityMinutes(shoe, rules)?.let { return ShoeCheckLine.Low(it.coerceAtLeast(1)) }
    val minutes = durabilityEarnMinutes(shoe, rules)
    return if (minutes != null && minutes > 0) ShoeCheckLine.Earns(minutes) else ShoeCheckLine.Ready
}

/**
 * 출발 전 점검의 신발 줄 글 — 닳은 뒤에도 적립하는 규칙이면 "닳은 신발이에요 · 수리하면 100%, 그대로 달리면 N% 적립돼요" ·
 * "약 N분 뒤에 신발이 다 닳아요 …" · "약 N분 동안 100% 적립돼요 · 그 뒤로는 N%", 아니면 예전 글(보상이 없어요 · 약 N분만 · 약 N분 동안).
 */
@Composable
internal fun shoeCheckLineText(line: ShoeCheckLine, rules: ShoeRules): String {
    val stats = rules.stats
    val worn = stats.wornPercent
    return when (line) {
        ShoeCheckLine.WornOut ->
            if (stats.earnsWhenWorn) stringResource(R.string.roots_check_worn, worn)
            else stringResource(R.string.run_free_check_low_durability_none)
        is ShoeCheckLine.Low ->
            if (stats.earnsWhenWorn) stringResource(R.string.roots_check_low_minutes, line.minutes, worn)
            else stringResource(R.string.run_free_check_low_durability_minutes, line.minutes)
        is ShoeCheckLine.Earns ->
            if (stats.earnsWhenWorn) stringResource(R.string.roots_check_earn_minutes, line.minutes, worn)
            else stringResource(R.string.run_check_earn_minutes, line.minutes)
        ShoeCheckLine.Ready -> stringResource(R.string.run_free_check_ready)
    }
}

/**
 * 달리는 중 신발 내구도를 다 썼을 때의 띠 — 닳은 뒤에도 적립하는 규칙(서버 0119)이면 "신발이 다 닳았어요 · 이제부터 N%만 적립돼요",
 * 아니면 예전 글("이제 걸음은 보상이 없어요")
 */
@Composable
internal fun shoeWornBandText(rules: ShoeRules = rememberShoeRules()): String =
    if (rules.stats.earnsWhenWorn) stringResource(R.string.roots_run_worn_band, rules.stats.wornPercent)
    else stringResource(R.string.run_shoe_worn_band)

/** [shoeWornBandText] 아래 한 줄 — "걸음은 그대로 세요. 수리하면 다시 100% 적립돼요"(예전 규칙이면 "수리하면 다시 보상을 받아요 …") */
@Composable
internal fun shoeWornBodyText(rules: ShoeRules = rememberShoeRules()): String =
    stringResource(if (rules.stats.earnsWhenWorn) R.string.roots_run_worn_body else R.string.run_shoe_worn_body)

/**
 * 결과의 신발 줄 — 이 러닝에서 신발 내구도를 다 썼다. 닳은 뒤에도 적립하는 규칙이면 "그 뒤의 걸음은 N%만 적립돼요 · 걸음은 그대로 세요",
 * 아니면 예전 글("그 뒤 걸음은 보상에 들지 않았어요"). 서버가 확인하기 전에도 보이는 줄이라 규칙만 말한다(금액 · 확정을 말하지 않는다).
 */
@Composable
internal fun shoeWornResultText(rules: ShoeRules = rememberShoeRules()): String =
    if (rules.stats.earnsWhenWorn) stringResource(R.string.roots_run_worn_result, rules.stats.wornPercent)
    else stringResource(R.string.run_shoe_worn_result)

/**
 * 오늘 적립 한도(에너지)를 다 썼는가 — 남은 에너지를 읽었고([energy] 가 null 이 아니고) 그 에너지로 적립할 걸음이 0.
 * 다 썼어도 러닝은 그대로 시작한다(기록은 남고 적립만 없다). [energyMultiplier] 는 효율 포인트를 넣은 신발이 에너지를 더 쓰는 배수(0119).
 */
fun energyUsedUp(energy: Double?, energyEfficiency: Double = 1.0, energyMultiplier: Double = 1.0): Boolean =
    energy != null && RewardEconomy.earnableSteps(energy, energyEfficiency, energyMultiplier) <= 0

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
