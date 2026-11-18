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
 * 효율 · 착화감은 지수(100 = 기준, 숫자가 2배면 2배), 내구도는 달릴 수 있는 분(1분마다 5 닳는다).
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

/** 내구도 → 다 채운 분(내려서 — 남은 시간을 부풀리지 않는다) */
fun wholeMinutes(units: Int): Int = units.coerceAtLeast(0) / RUN_UNITS_PER_MINUTE

/** 내구도 → 분, 소수 한 자리까지(끝의 .0 은 뺀다) — 포인트 미리보기 · 부분 수리처럼 작은 차이를 보일 때. 내려서 쓴다 */
fun formatMinutes(units: Int): String {
    val tenths = units.coerceAtLeast(0) * 10 / RUN_UNITS_PER_MINUTE
    return if (tenths % 10 == 0) formatCount(tenths / 10) else String.format(Locale.ROOT, "%,d.%d", tenths / 10, tenths % 10)
}

/** 달린 1분에 닳는 내구도 — 서버 0064(운으로 러닝마다 조금씩 다르지만 평균이 이 값이다, 0116) */
const val RUN_UNITS_PER_MINUTE = DURABILITY_WEAR_PER_MINUTE

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
fun durabilityText(shoe: Sneaker): String =
    if (shoe.server?.hasDurabilityMax == true) {
        minutesText("${wholeMinutes(durabilityLeft(shoe))} / ${wholeMinutes(durabilityMax(shoe))}")
    } else {
        formatDurability(shoe)
    }
