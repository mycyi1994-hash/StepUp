package com.stepup.android.domain

actual fun epochMillis(): Long = System.currentTimeMillis()

actual fun roundHalfUp2(value: Double): Double =
    java.math.BigDecimal.valueOf(value).setScale(2, java.math.RoundingMode.HALF_UP).toDouble()
