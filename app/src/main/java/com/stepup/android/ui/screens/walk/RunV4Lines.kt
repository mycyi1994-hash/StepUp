package com.stepup.android.ui.screens.walk

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.stepup.android.R
import com.stepup.android.domain.RewardEconomy
import com.stepup.android.domain.RunLuck
import com.stepup.android.domain.ShoeRules
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.energyMultOf
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

    /**
     * 오늘 남은 에너지가 신발보다 먼저(또는 같이) 끝난다 — 약 [minutes]분 적립하고 그 뒤로는 내일까지 적립이 없다(0%, 닳은 뒤 비율이 아니다).
     * 서버는 에너지로 먼저 자르고 그 안에서 가득 · 닳은 구간을 가른다(record_session) — Lv1 하루 에너지 5.5 = 약 20분(0121)
     */
    data class EnergyEnds(val minutes: Int) : ShoeCheckLine

    /** 다 닳았고 오늘 에너지도 없다 — 이번 러닝은 적립이 없다(에너지 줄이 따로 알린다). "그대로 달리면 N%"를 말하지 않고 수리만 알린다 */
    data object WornNoEnergy : ShoeCheckLine

    /** 남은 분을 모른다(예전 서버 · 폰에만 있던 신발 · 오늘 에너지를 아직 모른다) — "달릴 준비가 됐어요" */
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
 * 출발 전 점검에 실제로 보일 신발 줄 — 신발 수명과 오늘 남은 에너지 가운데 먼저 끝나는 쪽으로(2026-11-22 경제 점검: 새 Lv1 계정에
 * 가득 찬 일반 신발로 "약 40분 동안 100% · 그 뒤 30%"라 했지만 에너지 5.5 는 약 20분이고 그 뒤는 0% 다).
 * [energySteps] 는 오늘 남은 에너지로 이 신발이 적립할 걸음([todayEarnableSteps]) — 모르면 null 이고, 그때는 적립 분을 약속하지 않는다
 * (닳은 · 곧 닳는 신발의 수리 안내는 그대로). 에너지 분은 [RewardEconomy.RUN_STEPS_PER_MINUTE](분당 165보, 서버 runner_me 와 같은 기준).
 * 신발이 에너지보다 먼저 닳을 때만 "그 뒤로는 N%"(닳은 뒤 비율)를 말한다.
 */
fun runCheckLine(shoe: Sneaker, rules: ShoeRules, energySteps: Int?): ShoeCheckLine {
    val byShoe = shoeCheckLine(shoe, rules)
    if (energySteps == null) return if (byShoe is ShoeCheckLine.Earns) ShoeCheckLine.Ready else byShoe
    // 오늘 에너지를 다 썼다 — 이 러닝은 적립이 없다(점검의 에너지 줄이 알린다). 닳은 신발은 수리 안내만, 나머지는 준비 줄
    if (energySteps <= 0) return if (byShoe == ShoeCheckLine.WornOut) ShoeCheckLine.WornNoEnergy else ShoeCheckLine.Ready
    val shoeMinutes = when (byShoe) {
        is ShoeCheckLine.Low -> byShoe.minutes
        is ShoeCheckLine.Earns -> byShoe.minutes
        // 다 닳은 신발(에너지가 남아 있는 동안 N%) · 수명을 모르는 신발 — 신발 줄 그대로
        else -> return byShoe
    }
    val energyMinutes = energySteps / RewardEconomy.RUN_STEPS_PER_MINUTE
    return if (energyMinutes <= shoeMinutes) ShoeCheckLine.EnergyEnds(energyMinutes.coerceAtLeast(1)) else byShoe
}

/**
 * 오늘 남은 에너지 [energy] 로 이 신발이 적립할 걸음 — 착화감 · 효율 포인트의 에너지 배수(0119) · 고른 에너지 드링크([energyFactor] —
 * 그 비율만큼만 쓴다, 0106 `run_energy_factor`)까지. 서버 record_session 과 같은 차례다. 에너지를 아직 모르면 null.
 */
fun todayEarnableSteps(energy: Double?, shoe: Sneaker?, rules: ShoeRules = ShoeRules.FALLBACK, energyFactor: Double = 1.0): Int? {
    energy ?: return null
    val factor = energyFactor.takeIf { it > 0.0 && it < 1.0 } ?: 1.0
    return RewardEconomy.earnableSteps(energy / factor, shoe?.energyEfficiency ?: 1.0, shoe?.let { rules.energyMultOf(it) } ?: 1.0)
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
        // 에너지가 먼저 끝난다 — "오늘은 약 N분 동안 적립돼요 · 그 뒤로는 에너지를 다 써 내일까지 적립이 멈춰요"
        is ShoeCheckLine.EnergyEnds -> stringResource(R.string.run_check_energy_minutes, line.minutes)
        // 다 닳았고 에너지도 없다 — "걸음은 그대로 세요. 수리하면 다시 100% 적립돼요"(예전 규칙이면 예전 글)
        ShoeCheckLine.WornNoEnergy ->
            if (stats.earnsWhenWorn) stringResource(R.string.roots_run_worn_body)
            else stringResource(R.string.run_free_check_low_durability_none)
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
