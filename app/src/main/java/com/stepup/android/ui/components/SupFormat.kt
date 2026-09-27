package com.stepup.android.ui.components

import java.math.BigDecimal
import java.math.RoundingMode

/** 서버 원장의 소수 자리(numeric(20, 4)) */
private const val LEDGER_SCALE = 4

/**
 * SUP 잔고를 보일 때는 내린다. 서버는 소수 4자리까지 가지고 있어, 올려 보이면(499.6 → "500")
 * 500 SUP 짜리를 살 수 있는 것처럼 보이고 서버는 "SUP 가 부족합니다"로 거절한다.
 *
 * 내리기 전에 서버의 소수 4자리로 먼저 맞춘다 — 폰이 더한 합(SUM)은 부동소수라 0.1 열 번이
 * 0.9999999999999999 로 오고, 그대로 내리면 받은 1 SUP 가 "0" 으로 보인다.
 */
fun formatSupDown(value: Double, decimals: Int = 0): String =
    if (!value.isFinite()) "—"
    else "%,.${decimals}f".format(
        BigDecimal.valueOf(value).setScale(LEDGER_SCALE, RoundingMode.HALF_UP).setScale(decimals, RoundingMode.DOWN),
    )
