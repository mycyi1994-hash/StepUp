package com.stepup.android.ui.screens.items

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.RewardEconomy
import com.stepup.android.domain.ShoeIndex
import com.stepup.android.domain.ShoeQuality
import com.stepup.android.domain.ShoeRules
import com.stepup.android.domain.ShoeType
import com.stepup.android.domain.Sneaker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import kotlin.math.roundToInt

/*
 * 신발 능력치를 화면 숫자로(경제 4판, 서버 0116 — docs/shoe-stats-boxes-2026-11-18.md).
 *
 * 효율 · 착화감은 지수(100 = 기준, 숫자가 2배면 2배), 내구도는 달릴 수 있는 분(1분마다 5 닳는다 — 서버 0119 부터 효율에 포인트를
 * 넣은 신발은 그 배수만큼 빨리 닳는다: 분 = 내구도 ÷ (5 × 배수), [lifeMinutes]).
 * 막대 끝은 그 등급에서 가장 잘 뽑힌 새 신발 — 같은 등급 안에서 얼마나 잘 뽑혔는지가 보인다. 포인트로 넘으면 막대가 가득 찬다.
 * 값은 모두 서버가 준 신발 값과 규칙([ShoeRules])에서 온다 — 숫자를 지어내지 않는다.
 */

private val FallbackRules: StateFlow<ShoeRules> = MutableStateFlow(ShoeRules.FALLBACK)

/** 화면이 보는 신발 규칙 — 앱이 아직 준비되지 않았으면(미리보기 · 기기 시험) 서버 0116 과 같은 값 */
@Composable
fun rememberShoeRules(): ShoeRules {
    val flow = remember { runCatching { ServiceLocator.shoeRules.rules }.getOrNull() ?: FallbackRules }
    return flow.collectAsState().value
}

/** 효율 지수 — 서버 신발은 100 + 적립 +bps ÷ 100, 폰에만 있던 예전 신발은 100 + 적립 보너스(%) */
fun efficiencyIndex(shoe: Sneaker): Double =
    shoe.server?.let { ShoeIndex.efficiency(it.efficiencyBps) } ?: (100.0 + shoe.boostPercent)

/** 착화감 지수 — 100 ÷ (1 − 에너지 절약). 같은 에너지로 몇 배를 걷는가 × 100 */
fun comfortIndex(shoe: Sneaker): Double =
    shoe.server?.let { ShoeIndex.comfort(it.comfortBps) }
        ?: (100.0 / shoe.energyEfficiency.coerceAtLeast(RewardEconomy.MIN_ENERGY_FACTOR))

/** 지수 "140" · "1,063" — 정수로 반올림 */
fun formatIndex(index: Double): String = if (index.isFinite()) formatCount(index.roundToInt()) else "—"

/**
 * 닳는 속도를 정하는 효율 포인트 — 서버 `economy.eff_point_mult` 처럼 폰에서 올린 예전 신발(IMPORT · MINT) · 폰에만 있던 신발은 0
 */
fun wearPointsOf(shoe: Sneaker): Int = shoe.server?.takeIf { it.upgradable }?.points?.efficiency?.coerceAtLeast(0) ?: 0

/**
 * 이 신발이 내구도 [units] 로 달리는 분 — 내구도 ÷ (분당 닳음 × 효율 포인트 배수)(서버 0119 — 효율에 포인트를 넣은 만큼 빨리 닳는다).
 * 효율 포인트가 없는 신발 · 0119 전 서버 규칙([com.stepup.android.domain.StatRule.LEGACY])이면 내구도 ÷ 5 그대로다.
 */
fun lifeMinutes(shoe: Sneaker, units: Int, rules: ShoeRules): Double =
    rules.lifeMinutes(units.coerceAtLeast(0), shoe.rarity.id, wearPointsOf(shoe))

/** 나눗셈의 끝자리 때문에 31분이 30.999… 로 내려가지 않게 */
private const val MINUTE_ROUNDING = 1e-6

/** 분 → 다 채운 분(내려서 — 남은 시간을 부풀리지 않는다) */
fun wholeMinutes(minutes: Double): Int =
    if (minutes.isFinite()) kotlin.math.floor(minutes.coerceAtLeast(0.0) + MINUTE_ROUNDING).toInt() else 0

/** 이 신발의 내구도 [units] → 다 채운 분 */
fun wholeMinutes(shoe: Sneaker, units: Int, rules: ShoeRules): Int = wholeMinutes(lifeMinutes(shoe, units, rules))

/** 분 → 소수 한 자리까지(끝의 .0 은 뺀다) — 포인트 미리보기 · 부분 수리처럼 작은 차이를 보일 때. 내려서 쓴다 */
fun formatMinutes(minutes: Double): String {
    if (!minutes.isFinite()) return "—"
    val tenths = kotlin.math.floor(minutes.coerceAtLeast(0.0) * 10 + MINUTE_ROUNDING).toLong()
    return if (tenths % 10 == 0L) String.format(Locale.ROOT, "%,d", tenths / 10)
    else String.format(Locale.ROOT, "%,d.%d", tenths / 10, tenths % 10)
}

/** 이 신발의 내구도 [units] → 분(소수 한 자리까지) */
fun formatMinutes(shoe: Sneaker, units: Int, rules: ShoeRules): String = formatMinutes(lifeMinutes(shoe, units, rules))

/**
 * 능력치 글을 서버 0119 의 뜻으로 쓰는가 — 효율 = 더 빨리 벌고 그만큼 신발 · 에너지를 빨리 쓴다 · 내구도 = 가득 수리비는 그대로.
 * 서버가 능력치 규칙 묶음(`stats`)을 보냈고 효율 포인트가 닳는 속도를 올릴 때만. 0119 전 서버 · 아직 규칙을 못 읽었으면 예전 글 그대로.
 */
fun newStatMeaning(rules: ShoeRules): Boolean = rules.stats.fromServer && rules.stats.effWearExp > 0.0

/** 한 줄 설명(ⓘ) — 효율 · 착화감 · 내구도가 하는 일. [newMeaning]([newStatMeaning])이면 서버 0119 의 뜻으로 */
fun statMeaningRes(stat: PointStat, newMeaning: Boolean): Int = when (stat) {
    PointStat.EFFICIENCY -> if (newMeaning) R.string.shoe_info_efficiency_v2 else R.string.shoe_info_efficiency
    PointStat.COMFORT -> if (newMeaning) R.string.shoe_info_comfort_v2 else R.string.shoe_info_comfort
    PointStat.DURABILITY -> if (newMeaning) R.string.shoe_info_durability_v2 else R.string.shoe_info_durability
}

/**
 * 포인트를 다시 나눌 수 있게 되는 레벨 간격(5 — 서버 0119 `respec_every`). 서버가 능력치 규칙 묶음을 보냈을 때만 — 0119 전 서버 ·
 * 아직 규칙을 못 읽었으면 0("5레벨마다 다시 나눌 수 있어요"라고 말하지 않는다)
 */
fun respecEveryOf(rules: ShoeRules): Int = rules.stats.respecEvery.takeIf { rules.stats.fromServer && it > 0 } ?: 0

/** 하루 에너지 "2" · "0.5" — 소수 둘째 자리까지, 끝의 0 은 뺀다 */
fun formatEnergy(value: Double): String =
    if (!value.isFinite()) "0"
    else java.math.BigDecimal.valueOf(value).setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

/**
 * 이 신발의 수리비가 걸어서 버는 SUP 에서 차지하는 몫(0 ~ 1) — 수리 시트의 "약 N%". 서버 0119: 내구도 포인트로 늘어난 내구도는 가득
 * 수리비를 올리지 않아 그만큼 몫이 낮아진다(분당 수리비 ÷ 분당 버는 SUP = 설정 몫 ÷ 내구도 포인트 배수). 포인트가 없거나 0119 전
 * 서버면 설정 몫([ShoeRules.repairShare]) 그대로. 실제 수리비는 늘 서버가 준 1점 값으로 매긴다 — 이 값은 안내 글에만 쓴다.
 */
fun repairShareOf(shoe: Sneaker, rules: ShoeRules): Double {
    val server = shoe.server?.takeIf { it.upgradable } ?: return rules.repairShare
    val plain = rules.repairUnit(server.efficiencyBps)
    if (plain <= 0.0) return rules.repairShare
    val effPoints = server.points.efficiency.coerceAtLeast(0)
    val unit = rules.repairUnit(server.efficiencyBps, shoe.rarity.id, effPoints, server.points.durability.coerceAtLeast(0))
    // 1점 값은 닳는 속도만큼 싸지고 분당 닳는 점수는 그만큼 는다 — 분당 수리비로 견준다
    return rules.repairShare * unit / plain * rules.stats.wearMult(rules.grade(shoe.rarity).pointRateBps, effPoints)
}

/** 효율 막대 끝 — 이 등급에서 가장 잘 뽑힌 새 신발의 효율 지수 */
fun efficiencyBarEnd(shoe: Sneaker, rules: ShoeRules = ShoeRules.FALLBACK): Double =
    ShoeIndex.efficiency(rules.grade(shoe.rarity).efficiencyHi)

/** 착화감 막대 끝 — 이 등급에서 가장 잘 뽑힌 새 신발의 착화감 지수 */
fun comfortBarEnd(shoe: Sneaker, rules: ShoeRules = ShoeRules.FALLBACK): Double =
    ShoeIndex.comfort(rules.grade(shoe.rarity).comfortHi)

/** 착화감 상한(에너지 절약 %, 서버 economy.comfort_cap_bps — 70) */
fun comfortCapPercent(rules: ShoeRules = ShoeRules.FALLBACK): Int = rules.comfortCapBps / 100

/**
 * 뽑힌 값이 이 등급 안에서 얼마나 좋은가(별 ★ ~ ★★★ · 효율형 · 절약형 · 지구력형 · 균형형) — 포인트 전 값으로 센다.
 * 뽑은 값을 모르는 신발(폰에만 있던 신발 · 폰에서 올린 예전 신발 · 예전 서버)은 null — 별을 지어내지 않는다.
 */
fun shoeQualityOf(shoe: Sneaker, rules: ShoeRules = ShoeRules.FALLBACK): ShoeQuality? {
    val s = shoe.server ?: return null
    if (!s.upgradable || !s.hasDurabilityMax || s.durabilityBase <= 0) return null
    return ShoeQuality.of(rules, shoe.rarity.id, s.efficiencyBaseBps, s.comfortBaseBps, s.durabilityBase)
}

/** "★★" — 별 수만큼(1 ~ 3) */
fun qualityStars(quality: ShoeQuality): String = "★".repeat(quality.stars.coerceIn(1, 3))

@Composable
fun shoeTypeLabel(type: ShoeType): String = stringResource(
    when (type) {
        ShoeType.EFFICIENCY -> R.string.v4s_type_efficiency
        ShoeType.COMFORT -> R.string.v4s_type_comfort
        ShoeType.ENDURANCE -> R.string.v4s_type_endurance
        ShoeType.BALANCED -> R.string.v4s_type_balanced
    },
)

/** "★★ 효율형" — 상세 · 뽑기 결과 등에 한 줄로 */
@Composable
fun qualityLabel(quality: ShoeQuality): String = qualityStars(quality) + " " + shoeTypeLabel(quality.type)

/** 읽기 도구용 "별 2개, 효율형" */
@Composable
fun qualityDescription(quality: ShoeQuality): String =
    stringResource(R.string.v4s_quality_cd, quality.stars.coerceIn(1, 3), shoeTypeLabel(quality.type))

/** "28분" · "28 / 31분" — 분 단위를 붙인다 */
@Composable
fun minutesText(value: String): String = stringResource(R.string.v4s_minutes, value)

/** 남은 · 최대 내구도 글 — 최대 내구도를 아는 신발은 "28 / 31분", 모르는 예전 신발은 "92 / 100" */
@Composable
fun durabilityText(shoe: Sneaker, rules: ShoeRules = rememberShoeRules()): String =
    if (shoe.server?.hasDurabilityMax == true) {
        minutesText("${wholeMinutes(shoe, durabilityLeft(shoe), rules)} / ${wholeMinutes(shoe, durabilityMax(shoe), rules)}")
    } else {
        formatDurability(shoe)
    }
