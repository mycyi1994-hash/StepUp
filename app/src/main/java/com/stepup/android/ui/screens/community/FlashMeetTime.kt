package com.stepup.android.ui.screens.community

import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * 번개러닝 출발 시각 — 고른 시각 그 자체(epoch ms)를 들고 있다가 그대로 보낸다(2026-11-17 점검 #4).
 *
 * 예전에는 "지금부터 N분"만 들고 있다가 올릴 때 다시 더해, 19:00 을 고르면 칸에 18:59 로 보이고 19:02 같은 시각으로 저장됐다.
 * "N분 뒤"는 보여 주기만 한다. 앱 규칙은 지금부터 5분 ~ 24시간(서버는 앞으로의 시각이면 받는다).
 */
internal object FlashMeetTime {
    const val MIN_MINUTES = 5L
    const val MAX_MINUTES = 24L * 60

    /** 이만큼 안으로 지난 시각을 고르면 "내일"이 아니라 가장 이른 시각으로 맞춘다(시계를 지금 시각 언저리로 둔 것) */
    const val JUST_PASSED_MINUTES = 30L

    /** 고른 결과 — [clamped] 면 고른 시각이 너무 가까워(5분 안) 가장 이른 시각으로 맞췄다(화면이 알린다) */
    data class Pick(val atMillis: Long, val clamped: Boolean = false)

    /** 가장 이른 출발 — 지금부터 5분 뒤를 분 단위로 올린 시각 */
    fun earliest(nowMillis: Long): Long = ceilMinute(nowMillis + MIN_MINUTES * 60_000L)

    /** 처음 보이는 출발 — 지금부터 한 시간 뒤(분 단위로 올림) */
    fun initial(nowMillis: Long): Long = ceilMinute(nowMillis + 60 * 60_000L)

    /**
     * 시계에서 고른 시 · 분 → 출발 시각. 오늘 그 시각이 5분 뒤보다 늦으면 그대로, 5분 안이거나 막 지났으면(30분 안) 가장 이른 시각으로
     * 맞추고 알린다(예전에는 말없이 내일이 됐다), 그보다 앞서 지났으면 내일 그 시각.
     */
    fun pick(hour: Int, minute: Int, now: ZonedDateTime): Pick {
        val nowMillis = now.toInstant().toEpochMilli()
        val today = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        val todayMillis = today.toInstant().toEpochMilli()
        val earliest = earliest(nowMillis)
        return when {
            todayMillis >= earliest -> Pick(todayMillis)
            todayMillis > nowMillis - JUST_PASSED_MINUTES * 60_000L -> Pick(earliest, clamped = true)
            else -> Pick(today.plusDays(1).toInstant().toEpochMilli().coerceAtMost(nowMillis + MAX_MINUTES * 60_000L))
        }
    }

    /**
     * 올리기 직전 — 화면을 오래 열어 두어 고른 시각이 5분 안으로 들어왔으면 가장 이른 시각으로 맞추고 알린다(올리지 않는다 — 다시 누른다).
     * 5분 넘게 남았으면 그대로.
     */
    fun beforeSubmit(atMillis: Long, nowMillis: Long): Pick =
        if (atMillis < nowMillis + MIN_MINUTES * 60_000L) Pick(earliest(nowMillis), clamped = true) else Pick(atMillis)

    /** 화면의 "N분 뒤" — 남은 시간을 분으로 올린다(19:00 · 지금 18:37:20 → 23분). 0 아래로는 가지 않는다 */
    fun minutesLeft(atMillis: Long, nowMillis: Long): Int =
        ((atMillis - nowMillis + 59_999L) / 60_000L).coerceAtLeast(0L).toInt()

    private fun ceilMinute(millis: Long): Long {
        val down = Instant.ofEpochMilli(millis).truncatedTo(ChronoUnit.MINUTES).toEpochMilli()
        return if (down == millis) millis else down + 60_000L
    }
}
