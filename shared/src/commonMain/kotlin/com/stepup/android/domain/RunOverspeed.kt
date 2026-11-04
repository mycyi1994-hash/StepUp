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
 *  - 오차가 큰 위치(두 끝 중 하나라도 [MAX_ACCURACY_M] 넘게 흐린 위치)의 튐은 과속으로 세지 않는다([counts]).
 *  - 30초를 채워도 처음 자리에서 [STOP_MIN_TRAVEL_M] 만큼 멀어지지 않았으면 멈추지 않고 처음부터 다시 센다 — 두 자리를 오간 튐이다.
 *    실내 와이파이 위치가 두 자리를 오가, 서 있는 러너가 멈추고 신발 내구도 절반 패널티를 받을 수 있었다(2026-10-19 점검).
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

    /** 과속으로 셀 위치의 가장 큰 오차(m) — 와이파이 · 기지국 위치는 수십 m 씩 튄다. 차 · 자전거는 바깥이라 GPS(몇 m)로 잡힌다 */
    const val MAX_ACCURACY_M = 20f

    /** 멈추려면 과속이 이어진 동안 처음 자리에서 이만큼(m) 멀어졌어야 한다 — 시속 25km 로 30초(208m)의 약 3/4(굽은 길) */
    const val STOP_MIN_TRAVEL_M = 150.0

    /**
     * 지금 이어지는 과속 — [since] 첫 과속 구간, [lastAt] 마지막 과속 구간(벽시계 ms).
     * [from] 첫 과속 구간이 시작된 자리, [to] 마지막 과속 구간이 끝난 자리 — 모르면 null(멀어졌는지 보지 않는다).
     */
    data class Streak(val since: Long, val lastAt: Long, val from: GeoPoint? = null, val to: GeoPoint? = null)

    /** 이 과속 구간을 과속으로 셀까 — 두 끝(기준점 [anchorAccuracyM] · 새 위치 [accuracyM])의 오차를 알고 둘 다 [MAX_ACCURACY_M] 안일 때만 */
    fun counts(accuracyM: Float?, anchorAccuracyM: Float?): Boolean =
        accuracyM != null && anchorAccuracyM != null && accuracyM <= MAX_ACCURACY_M && anchorAccuracyM <= MAX_ACCURACY_M

    /**
     * 과속 구간([from] → [to])이 들어왔다 — 이어 세거나(10초 안) 새로 센다. 30초를 채웠는데 처음 자리에서 [STOP_MIN_TRAVEL_M] 만큼
     * 멀어지지 않았으면 두 자리를 오간 위치 튐이다 — 멈추지 않고 이 구간부터 다시 센다.
     */
    fun onFlagged(streak: Streak?, at: Long, from: GeoPoint? = null, to: GeoPoint? = null): Streak {
        if (streak == null || at - streak.lastAt > GAP_MS || at < streak.lastAt) return Streak(at, at, from, to)
        val next = streak.copy(lastAt = at, to = to ?: streak.to)
        return if (next.lastAt - next.since >= STOP_MS && !next.travelled()) Streak(at, at, from, to) else next
    }

    /** 처음 자리에서 [STOP_MIN_TRAVEL_M] 만큼 멀어졌나 — 자리를 모르면 그렇다고 본다 */
    private fun Streak.travelled(): Boolean {
        val start = from ?: return true
        val end = to ?: return true
        return haversineMeters(start, end) >= STOP_MIN_TRAVEL_M
    }

    /** 지금(벽시계 [now]) 보아 아직 이어지는가 — 마지막 과속 구간이 10초 넘게 지났으면 끝났다 */
    fun live(streak: Streak?, now: Long): Streak? =
        streak?.takeIf { now - it.lastAt <= GAP_MS && now >= it.since }

    /** 과속 구간끼리 30초를 채웠다 — 러닝을 멈춘다. 구간이 들어오지 않는 시간만으로는 멈추지 않는다 */
    fun shouldStop(streak: Streak?): Boolean = streak != null && streak.lastAt - streak.since >= STOP_MS && streak.travelled()

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
