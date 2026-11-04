package com.stepup.android.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URLEncoder
import java.util.Locale

// 안드로이드: 예전에 규칙 코드가 바로 부르던 Java 함수 그대로(동작이 바뀌지 않게).

actual fun nowMillis(): Long = System.currentTimeMillis()

actual fun formatRoot(pattern: String, vararg args: Any): String = String.format(Locale.ROOT, pattern, *args)

actual fun formatUs(pattern: String, vararg args: Any): String = String.format(Locale.US, pattern, *args)

actual fun formatLocal(pattern: String, vararg args: Any): String = String.format(pattern, *args)

actual fun toRadians(degrees: Double): Double = Math.toRadians(degrees)

actual fun toDegrees(radians: Double): Double = Math.toDegrees(radians)

actual fun urlEncodeUtf8(s: String): String = URLEncoder.encode(s, "UTF-8")

actual fun exactHalfUp(v: Double, scale: Int): String = BigDecimal(v).setScale(scale, RoundingMode.HALF_UP).toPlainString()

actual fun shortHalfUp(v: Double, scale: Int): Double = BigDecimal.valueOf(v).setScale(scale, RoundingMode.HALF_UP).toDouble()
