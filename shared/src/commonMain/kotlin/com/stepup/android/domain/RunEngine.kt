package com.stepup.android.domain

/**
 * 러닝 한 번의 셈(2026-11-03 iOS C2) — 위치 · 걸음 · 시계를 받아 시간 · 경로 · 거리 · 페이스 · 서 있음을 낸다.
 * 규칙은 안드로이드 러닝 서비스와 같은 공용 규칙을 쓴다: 위치 거르기 [TrackFixRule], 경로 점 [TrackPoint], 거리 [RunDistance],
 * 페이스 [RunPace], 서 있음 [RunStill]. 기기 코드(iOS Core Location · Core Motion)는 값을 넣기만 한다.
 *
 * 시간은 달린 시간만 센다(일시정지 동안은 세지 않는다). 걸음은 러닝을 시작한 뒤 기기가 센 누적값을 그대로 넣는다.
 */
class RunEngine(private val startedAtMs: Long) {
    data class State(
        val startedAt: Long,
        val paused: Boolean,
        val elapsedSec: Long,
        val steps: Int,
        /** 경로로 잰 거리(km) — 거른 점만 */
        val gpsKm: Double,
        /** 화면 거리(km) — 경로 100m 전에는 걸음 거리([RunDistance.liveKm]) */
        val liveKm: Double,
        val track: List<TrackPoint>,
        /** 지금 자리(지도 점) — 흐린 위치도 지도만 옮긴다 */
        val here: GeoPoint?,
        /** [here] 를 받은 시각 · 흐리지 않은 위치였는가(지도의 내 점 [liveHead] 가 본다) */
        val hereAt: Long,
        val hereAccurate: Boolean,
        val paceSecPerKm: Double?,
        val still: Boolean,
    )

    private var paused = false
    private var runMs = 0L
    private var lastTickMs = startedAtMs
    private var steps = 0
    private var gpsMeters = 0.0
    private val track = ArrayList<TrackPoint>()
    private var anchor: GeoPoint? = null
    private var anchorAccuracy: Float? = null
    private var here: GeoPoint? = null
    private var hereAt = 0L
    private var hereAccurate = true
    private var goodHereAt = 0L
    private var lastMoveMs = startedAtMs

    /** 시계 — 1초마다 넣는다. 일시정지가 아니면 달린 시간에 더한다 */
    fun tick(nowMs: Long) {
        if (!paused && nowMs > lastTickMs) runMs += nowMs - lastTickMs
        lastTickMs = nowMs
    }

    fun pause(nowMs: Long) {
        tick(nowMs)
        paused = true
    }

    fun resume(nowMs: Long) {
        tick(nowMs)
        paused = false
        lastMoveMs = nowMs
        // 다시 달릴 때는 새 구간 — 멈춘 사이 움직인 거리를 잇지 않는다
        anchor = null
        anchorAccuracy = null
    }

    /** 러닝을 시작한 뒤 걸음(누적) */
    fun steps(total: Int, nowMs: Long) {
        if (paused) return
        if (total > steps) lastMoveMs = nowMs
        steps = maxOf(steps, total)
    }

    /** 위치 하나 — [accuracyM] 는 수평 오차(m), 모르면 null */
    fun fix(lat: Double, lng: Double, accuracyM: Float?, atMs: Long) {
        val point = GeoPoint(lat, lng)
        val a = anchor
        val decision = TrackFixRule.decide(accuracyM, a?.let { haversineMeters(it, point) }, anchorAccuracy)
        val good = decision != TrackFixRule.Fix.MAP_ONLY
        // 경로가 생긴 뒤의 흐린 위치는 좋은 위치를 받은 지 30초 안이면 지금 자리를 옮기지 않는다(안드로이드 러닝 서비스와 같다)
        if (!good && TrackFixRule.keepHere(track.isNotEmpty(), atMs - goodHereAt)) return
        if (good) goodHereAt = atMs
        here = point
        hereAt = atMs
        hereAccurate = good
        if (paused) return
        when (decision) {
            TrackFixRule.Fix.MAP_ONLY, TrackFixRule.Fix.HOLD -> Unit
            TrackFixRule.Fix.REFINE -> {
                anchor = point
                anchorAccuracy = accuracyM
                if (track.isNotEmpty()) track[track.lastIndex] = TrackPoint(lat, lng, atMs)
            }
            TrackFixRule.Fix.COMMIT -> {
                if (a != null) gpsMeters += haversineMeters(a, point)
                anchor = point
                anchorAccuracy = accuracyM
                track += TrackPoint(lat, lng, atMs)
                lastMoveMs = atMs
            }
        }
    }

    fun state(nowMs: Long): State {
        val elapsed = runMs / 1000
        val gpsKm = gpsMeters / 1000
        val km = RunDistance.liveKm(gpsKm, steps)
        return State(
            startedAt = startedAtMs, paused = paused, elapsedSec = elapsed, steps = steps,
            gpsKm = gpsKm, liveKm = km, track = track.toList(), here = here, hereAt = hereAt, hereAccurate = hereAccurate,
            paceSecPerKm = RunPace.secPerKm(elapsed, km * 1000),
            still = RunStill.isStill(recording = !paused, sinceMoveMs = nowMs - lastMoveMs),
        )
    }

    /** 저장할 거리(m) — 서버가 쳐 주는 거리와 같다([RunDistance.creditedMeters]) */
    fun creditedMeters(): Double = RunDistance.creditedMeters(gpsMeters / 1000, steps)
}
