package com.stepup.android.domain.coaching

import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToLong

/*
 * AI 코칭 — 숫자 글자와 그래프 축. 페이스는 초/km 이고 작은 값이 빠르다.
 * 그래프는 빠른 쪽을 위에 둔다 — 7'00" 에서 8'00" 로 내려가는 선은 느려지는 방향이다.
 */

/** 7'30" — 초/km 를 반올림한 분 · 초. 계산할 수 없으면 null */
fun paceText(secPerKm: Double?): String? {
    val total = secPerKm?.takeIf { it.isFinite() && it > 0 }?.roundToLong() ?: return null
    return String.format(Locale.ROOT, "%d'%02d\"", total / 60, total % 60)
}

/** 24:18 · 1:08:24 — 활성 시간 */
fun clockText(seconds: Long): String {
    val s = seconds.coerceAtLeast(0)
    return if (s >= 3600) String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
    else String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60)
}

/** 3.24 — 러닝 한 번의 거리(km, 소수 둘째 자리 내림 아닌 반올림 — 내 러닝 기록과 같은 자리수) */
fun kmText(meters: Double): String = String.format(Locale.ROOT, "%,.2f", meters / 1000.0)

/** 9.0 — 주간 합계 거리(km, 한 자리) */
fun kmTextShort(meters: Double): String = String.format(Locale.ROOT, "%,.1f", meters / 1000.0)

/**
 * 구간 페이스 그래프의 세로축 — [fastSec](위) ~ [slowSec](아래), 눈금 [ticks]. 값 · 평균이 모두 들어가고 30초 단위로 끊는다.
 * 값이 몰려 있어도 1분 폭은 둔다(점 몇 초 차이가 화면 끝에서 끝으로 과장되지 않게).
 */
data class PaceAxis(val fastSec: Long, val slowSec: Long, val ticks: List<Long>) {
    /** 0 = 맨 위(빠름) … 1 = 맨 아래(느림) */
    fun fraction(secPerKm: Double): Float =
        ((secPerKm - fastSec) / (slowSec - fastSec)).toFloat().coerceIn(0f, 1f)
}

fun paceAxis(values: List<Long>, average: Double?): PaceAxis {
    val all = values.map { it.toDouble() } + listOfNotNull(average?.takeIf { it.isFinite() && it > 0 })
    require(all.isNotEmpty())
    val minimum = all.min()
    val maximum = all.max()
    val step = listOf(30L, 60L, 120L, 300L, 600L).first { step ->
        val fast = floor(minimum / step).toLong() * step
        val slow = ceil(maximum / step).toLong() * step
        (slow - fast) / step <= 4 || step == 600L
    }
    var fast = floor(minimum / step).toLong() * step
    var slow = ceil(maximum / step).toLong() * step
    if (slow - fast < 60) {
        // 1분 폭을 채운다 — 값이 눈금 위에 딱 걸치면(450 → 450~450) 위 · 아래로 나눠 넓힌다
        val missing = 60 - (slow - fast)
        fast -= (missing / step / 2) * step
        slow = fast + 60
        if (maximum > slow) slow += step
    }
    fast = fast.coerceAtLeast(0)
    return PaceAxis(fast, slow, (fast..slow step step).toList())
}
