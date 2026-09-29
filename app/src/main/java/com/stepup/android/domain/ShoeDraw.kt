package com.stepup.android.domain

import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

/** 뽑기 종류 — 뽑기 화면 위의 글자 탭(무료 · 상급). 2026-09-27 무료 정책(서버 0042) */
enum class DrawKind { FREE, PREMIUM }

/**
 * 뽑기 현황(서버 draw_status). 출처마다 **남은** 수다 — 처음 준 수나 평생 합과 섞지 않는다.
 *
 * 무료는 오늘 몫([dailyLeft], 자정에 새로 생기고 넘어가지 않는다)과 첫 가입 선물([signupLeft]).
 * 상급은 지갑 첫 연결 선물([giftLeft])과 연결 뒤 러닝으로 받은 기회([runLeft]).
 * 수를 주고 줄이는 것은 모두 서버다 — 폰은 이 값을 비출 뿐이다.
 */
data class DrawStatus(
    val dailyLeft: Int,
    val dailyTotal: Int,
    val signupLeft: Int,
    val signupGranted: Int,
    val walletLinked: Boolean,
    /** 아직 지갑을 연결한 적이 없으면 첫 연결에 받을 상급 수, 있으면 0 */
    val giftOnLink: Int,
    val giftLeft: Int,
    val runLeft: Int,
    val genesisLeft: Int,
    /** 다음 상급 뽑기까지 모은 거리(m) — 한 칸에 못 미친 나머지 */
    val runProgressMeters: Double,
    val runStepMeters: Int,
    val runToday: Int,
    val runDailyCap: Int,
    val chainPaused: Boolean,
) {
    val freeLeft: Int get() = dailyLeft.coerceAtLeast(0) + signupLeft.coerceAtLeast(0)
    val premiumLeft: Int get() = giftLeft.coerceAtLeast(0) + runLeft.coerceAtLeast(0)

    fun left(kind: DrawKind): Int = if (kind == DrawKind.FREE) freeLeft else premiumLeft

    /** 이 종류로 지금 한 번 뽑을 수 있는가 — 상급은 지갑이 연결돼 있고 체인이 멈추지 않았을 때만 */
    fun canDraw(kind: DrawKind): Boolean = when (kind) {
        DrawKind.FREE -> freeLeft > 0
        DrawKind.PREMIUM -> walletLinked && !chainPaused && premiumLeft > 0
    }

    /** 모은 거리의 몫(0~1) — 진행 막대 */
    val progressFraction: Float
        get() = if (runStepMeters <= 0) 0f else (runProgressMeters / runStepMeters).toFloat().coerceIn(0f, 1f)

    /** 다음 상급 뽑기까지 남은 거리(m) */
    val metersToNextPremium: Double
        get() = (runStepMeters - runProgressMeters).coerceIn(0.0, runStepMeters.coerceAtLeast(0).toDouble())

    /** 오늘 러닝으로 받을 수 있는 상급 뽑기를 다 받았다 */
    val runCapReached: Boolean get() = runDailyCap in 1..runToday
}

/**
 * 뽑기 화면의 거리 글자 — km, 소수 첫째 자리. 모은 거리는 내려서(999m 를 "1.0"이라 하지 않게),
 * 남은 거리는 올려서(50m 남았는데 "0.0km"라 하지 않게) 적는다. 딱 떨어지면 소수 없이("1km").
 */
object DrawDistance {
    fun progressKm(meters: Double): String = tenths(floor(meters.coerceAtLeast(0.0) / 100) / 10)

    fun remainingKm(meters: Double): String = tenths(ceil(meters.coerceAtLeast(0.0) / 100) / 10)

    fun stepKm(meters: Int): String = tenths(meters.coerceAtLeast(0) / 1000.0)

    private fun tenths(km: Double): String =
        if (km == floor(km)) String.format(Locale.ROOT, "%d", km.toLong())
        else String.format(Locale.ROOT, "%.1f", km)
}
