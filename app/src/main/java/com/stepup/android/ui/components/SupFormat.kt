package com.stepup.android.ui.components

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * SUP 잔고를 보일 때는 내린다. 서버는 소수 4자리까지 가지고 있어, 올려 보이면(499.6 → "500")
 * 500 SUP 짜리를 살 수 있는 것처럼 보이고 서버는 "SUP 가 부족합니다"로 거절한다.
 */
fun formatSupDown(value: Double, decimals: Int = 0): String =
    if (!value.isFinite()) "—"
    else "%,.${decimals}f".format(BigDecimal.valueOf(value).setScale(decimals, RoundingMode.DOWN))
