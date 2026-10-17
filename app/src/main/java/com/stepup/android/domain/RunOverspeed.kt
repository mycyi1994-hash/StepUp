package com.stepup.android.domain

/**
 * 러닝 중 과속(2026-10-17 사용자 결정 "빼는게 아니라 애초에 경고가 뜨고 그렇게 유지되면 강제로 멈추고 패널티").
 *
 * 사람 한계([RunIntegrity.MAX_SPEED_KMH], 25km/h)를 넘는 구간이 오면 바로 경고하고, 그 상태가 [STOP_MS](30초) 이어지면
 * 러닝을 멈춘다. 멈춘 러닝에는 패널티 — 신은 신발의 남은 내구도 절반(서버 0065), 멈춘 때부터 [BAN_MS](10분) 러닝 금지.
 *
 * "이어진다"는 과속 구간이 끊기지 않고 계속 들어오는 것이다.
 *  - 사람 속도로 움직인 구간(5m · 1초 이상, [RunIntegrity.isPlausible])이 오면 경고가 풀린다 — 속도를 줄였다.
 *  - 과속 구간 사이가 [GAP_MS](10초)보다 벌어지면 처음부터 다시 센다 — 터널 · 신호 재획득의 튐 두 번이 30초 떨어져
 *    왔다고 서 있는 러너를 멈추지 않게. 차가 신호등에 10초 넘게 서면 그때도 다시 센다.
 *  - 제자리 노이즈(5m 아래)는 경고를 풀지도, 늘리지도 않는다.
 *
 * 모든 함수가 순수 함수라 단위 테스트로 검증한다(RunOverspeedTest).
 */
object RunOverspeed {
    /** 과속이 이만큼(ms) 이어지면 러닝을 멈춘다 */
    const val STOP_MS = 30_000L

    /** 과속 구간 사이가 이보다(ms) 벌어지면 이어진 것으로 보지 않는다 */
    const val GAP_MS = 10_000L

    /** 멈춘 뒤 러닝 금지 시간(ms) — 서버 economy.overspeed_ban_min() 과 같다 */
    const val BAN_MS = 10 * 60_000L

    /** 지금 이어지는 과속 — [since] 첫 과속 구간, [lastAt] 마지막 과속 구간(벽시계 ms) */
    data class Streak(val since: Long, val lastAt: Long)

    /** 과속 구간이 들어왔다 — 이어 세거나(10초 안) 새로 센다 */
    fun onFlagged(streak: Streak?, at: Long): Streak =
        if (streak == null || at - streak.lastAt > GAP_MS || at < streak.lastAt) Streak(at, at) else streak.copy(lastAt = at)

    /** 지금(벽시계 [now]) 보아 아직 이어지는가 — 마지막 과속 구간이 10초 넘게 지났으면 끝났다 */
    fun live(streak: Streak?, now: Long): Streak? =
        streak?.takeIf { now - it.lastAt <= GAP_MS && now >= it.since }

    /** 과속 구간끼리 30초를 채웠다 — 러닝을 멈춘다. 구간이 들어오지 않는 시간만으로는 멈추지 않는다 */
    fun shouldStop(streak: Streak?): Boolean = streak != null && streak.lastAt - streak.since >= STOP_MS

    /** 멈추기까지 남은 초(올림, 0 ~ 30) — 경고 글에 쓴다 */
    fun secondsLeft(streak: Streak, now: Long): Int {
        val left = STOP_MS - (now - streak.since).coerceAtLeast(0L)
        return ((left.coerceAtLeast(0L) + 999) / 1000).toInt()
    }

    /** 러닝 금지가 남은 시간(ms) — 없으면 0 */
    fun banLeftMs(banUntil: Long, now: Long): Long = (banUntil - now).coerceAtLeast(0L)

    /** 러닝 금지가 남은 분(올림) — 안내 글에 쓴다 */
    fun banLeftMinutes(banUntil: Long, now: Long): Int = ((banLeftMs(banUntil, now) + 59_999) / 60_000).toInt()
}
