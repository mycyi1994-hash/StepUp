package com.stepup.android

import com.stepup.android.ui.screens.rewards.amountText
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** 지갑 v1 금액 글자 — 기존 정밀도(두 자리, 0 쪽으로), 부호는 +/−(누계는 음수에만), 0 은 부호 없이 */
class WalletAmountTest {
    private lateinit var before: Locale

    @Before fun setUp() {
        before = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After fun tearDown() {
        Locale.setDefault(before)
    }

    @Test fun signedAmountsUseTheLedgerPrecision() {
        assertEquals("+3.50", amountText(3.5, signed = true))
        assertEquals("−20.00", amountText(-20.0, signed = true))
        assertEquals("+9.47", amountText(9.4799, signed = true))
        assertEquals("+1,250.00", amountText(1250.0, signed = true))
    }

    @Test fun zeroHasNoSign() {
        assertEquals("0.00", amountText(0.0, signed = true))
        assertEquals("0.00", amountText(-0.0, signed = true))
        // 두 자리 아래만 남는 음수도 "-0.00" 이 되지 않는다
        assertEquals("0.00", amountText(-0.004, signed = true))
        assertEquals("0.00", amountText(0.004, signed = true))
    }

    @Test fun unsignedTotalsKeepTheExistingFormat() {
        assertEquals("83.07", amountText(83.07, signed = false))
        assertEquals("0.00", amountText(0.0, signed = false))
        // 잔액이 음수면 부호를 숨기지 않는다
        assertEquals("−5.25", amountText(-5.25, signed = false))
        assertEquals("0.00", amountText(-0.001, signed = false))
    }
}
