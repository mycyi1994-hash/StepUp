package com.stepup.android.domain

actual fun epochMillis(): Long = System.currentTimeMillis()

actual fun roundHalfUp2(value: Double): Double =
    java.math.BigDecimal.valueOf(value).setScale(2, java.math.RoundingMode.HALF_UP).toDouble()

actual fun formatFixed(value: Double, decimals: Int): String = String.format(java.util.Locale.ROOT, "%.${decimals}f", value)

actual fun exactHalfUp(value: Double, scale: Int): String =
    java.math.BigDecimal(value).setScale(scale, java.math.RoundingMode.HALF_UP).toPlainString()
