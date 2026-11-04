package com.stepup.android.domain

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import kotlin.math.PI

// 아이폰: Java 가 없으니 같은 결과를 내는 순수 Kotlin(JavaFormat · ExactDecimal · UrlEncoding)을 쓴다.
// 그 셋이 Java 와 같은 글자를 내는지는 안드로이드 단위 테스트(PlatformTwinTest)가 맞대어 본다.

actual fun nowMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()

actual fun formatRoot(pattern: String, vararg args: Any): String = JavaFormat.format(pattern, *args)

actual fun formatUs(pattern: String, vararg args: Any): String = JavaFormat.format(pattern, *args)

actual fun formatLocal(pattern: String, vararg args: Any): String = JavaFormat.format(pattern, *args)

// Java 9+ 와 같은 식(도 × π/180 상수)
actual fun toRadians(degrees: Double): Double = degrees * (PI / 180.0)

actual fun toDegrees(radians: Double): Double = radians * (180.0 / PI)

actual fun urlEncodeUtf8(s: String): String = UrlEncoding.encode(s)

actual fun exactHalfUp(v: Double, scale: Int): String = ExactDecimal.halfUp(v, scale)

actual fun shortHalfUp(v: Double, scale: Int): Double = JavaFormat.shortHalfUp(v, scale).toDouble()
