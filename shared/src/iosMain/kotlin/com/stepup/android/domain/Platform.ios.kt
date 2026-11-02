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
    return NSDecimalNumber(string = value.toString()).decimalNumberByRoundingAccordingToBehavior(handler).doubleValue
}
