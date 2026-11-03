package com.stepup.android.domain

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/** 공용 Decimal 이 java.math.BigDecimal(안드로이드 수리 견적이 쓰던 것)과 같은 값을 내는가 */
class DecimalJvmTest {
    private fun big(v: Double, s: Int) = BigDecimal.valueOf(v).setScale(s, RoundingMode.HALF_UP)

    @Test fun valueOfAndSetScaleMatchBigDecimal() {
        val rnd = Random(20261103)
        val samples = listOf(0.0, -0.0, 0.5, 1.005, 2.675, 12.5, 1234.56785, 0.000049999, 99.99995, -3.14159, 0.1 + 0.2) +
            List(5000) { (rnd.nextDouble() - 0.3) * 10.0.pow(rnd.nextInt(-6, 7)) }
        for (v in samples) for (s in listOf(0, 2, 4, 8)) {
            assertEquals(big(v, s).toPlainString(), Decimal.of(v, s).toPlainString(), "valueOf($v).setScale($s)")
        }
    }

    @Test fun arithmeticMatchesBigDecimal() {
        val rnd = Random(7)
        repeat(3000) {
            val pts = rnd.nextLong(0, 6000)
            val unit = rnd.nextDouble() * 0.5
            val balance = rnd.nextDouble() * 5000
            val cost = Decimal.of(pts).multiply(Decimal.of(unit, 8)).setScale(4)
            val bigCost = BigDecimal.valueOf(pts).multiply(big(unit, 8)).setScale(4, RoundingMode.HALF_UP)
            assertEquals(bigCost.toPlainString(), cost.toPlainString())
            val short = cost.subtract(Decimal.of(balance, 4)).max(Decimal.ZERO)
            val bigShort = bigCost.subtract(big(balance, 4)).max(BigDecimal.ZERO)
            assertEquals(bigShort.signum(), short.signum())
            assertEquals(0, bigShort.compareTo(BigDecimal(short.toPlainString())))
            assertEquals(big(balance, 4).subtract(bigCost).toDouble(), Decimal.of(balance, 4).subtract(cost).toDouble())
        }
    }

    private fun Double.pow(n: Int) = Math.pow(this, n.toDouble())
}
