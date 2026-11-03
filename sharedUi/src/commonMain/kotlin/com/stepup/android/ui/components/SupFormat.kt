package com.stepup.android.ui.components

import com.stepup.android.domain.formatFixed

/** 서버 원장의 소수 자리(numeric(20, 4)) */
private const val LEDGER_SCALE = 4

/**
 * SUP 잔고를 보일 때는 내린다(안드로이드 SupFormat.kt 와 같은 값 — 먼저 원장의 소수 4자리로 반올림, 그다음 [decimals] 자리로 내림,
 * 천 단위 쉼표). 올려 보이면(499.6 → "500") 살 수 있는 것처럼 보이고 서버는 거절한다.
 */
fun formatSupDown(value: Double, decimals: Int = 0): String {
    if (!value.isFinite()) return "—"
    // 소수 4자리 반올림(BigDecimal.valueOf(v).setScale(4, HALF_UP) 와 같은 글자 — 공용 formatFixed)
    val ledger = formatFixed(value, LEDGER_SCALE)
    val negative = ledger.startsWith("-")
    val digits = ledger.removePrefix("-")
    val whole = digits.substringBefore('.')
    val fraction = digits.substringAfter('.', "").take(decimals).padEnd(decimals, '0')
    val grouped = whole.reversed().chunked(3).joinToString(",").reversed()
    val body = if (decimals > 0) "$grouped.$fraction" else grouped
    val zero = body.all { it == '0' || it == ',' || it == '.' }
    return if (negative && !zero) "-$body" else body
}
