package com.stepup.android.domain

import platform.Foundation.NSDate
import platform.Foundation.NSDecimalNumber
import platform.Foundation.NSDecimalNumberHandler
import platform.Foundation.NSRoundingMode
import platform.Foundation.timeIntervalSince1970

actual fun epochMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()

// 안드로이드(BigDecimal.valueOf — Double.toString 의 십진 표기)와 같게 문자열 십진수에서 반올림한다
actual fun roundHalfUp2(value: Double): Double {
    if (value.isNaN() || value.isInfinite()) return value
    val handler = NSDecimalNumberHandler(
        roundingMode = NSRoundingMode.NSRoundPlain,
        scale = 2,
        raiseOnExactness = false,
        raiseOnOverflow = false,
        raiseOnUnderflow = false,
        raiseOnDivideByZero = false,
    )
    // doubleValue 를 쓰니 iOS 시뮬레이터의 PlatformTest(십진 반올림 기대값)가 깨졌다(2026-11-02 CI — 어느 값인지는 로그에 없다).
    // doubleValue 는 십진수를 가장 가까운 double 로 바꾼다는 보장이 없다. 반올림한 십진 문자열을 Kotlin 으로 다시 읽으면
    // BigDecimal.toDouble() 처럼 가장 가까운 double 이 된다.
    return NSDecimalNumber(string = value.toString()).decimalNumberByRoundingAccordingToBehavior(handler).stringValue.toDouble()
}

actual fun formatFixed(value: Double, decimals: Int): String = formatFixedPortable(value, decimals)

actual fun exactHalfUp(value: Double, scale: Int): String = exactHalfUpPortable(value, scale)
