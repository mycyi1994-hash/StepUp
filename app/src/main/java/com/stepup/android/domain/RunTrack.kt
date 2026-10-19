package com.stepup.android.domain

/**
 * 시각이 붙은 GPS 좌표.
 *
 * [GeoPoint]와 따로 두는 이유는 쓰임이 다르기 때문이다. 코스는 "어디를 지나는
 * 길인가"라는 모양만 필요하지만, 러닝 기록은 "언제 거기 있었는가"가 있어야
 * 구간 속도를 계산할 수 있다. 속도를 못 재면 그 세션이 사람이 뛴 것인지
 * 판정할 수 없고, 판정할 수 없는 세션은 온체인으로 청구할 수 없다.
 *
 * @property at epoch 밀리초. 어테스터(`attester/src/economy.js`)의 `track[].t`와
 *   같은 단위다 — 바꾸면 서버 판정이 조용히 틀어진다.
 */
data class TrackPoint(val lat: Double, val lng: Double, val at: Long) {
    fun toGeoPoint(): GeoPoint = GeoPoint(lat, lng)
}

/**
 * 러닝 경로를 한 줄 문자열로 담았다 꺼낸다.
 *
 * 형식은 `위도,경도,시각;위도,경도,시각` — 코스가 쓰는
 * [RunCourse.encode]와 같은 모양에 시각만 더했다. 바이너리로 줄일 수도 있지만,
 * 이 값은 나중에 서버로 올라가 분쟁의 근거가 된다. 사람이 열어서 읽을 수
 * 있다는 점이 몇 KB보다 가치 있다.
 *
 * 좌표는 소수점 6자리로 끊는다. 위도 1e-6도는 약 11cm라 GPS 오차(수 m)보다
 * 훨씬 정밀하다. 그 아래 자릿수는 정보가 아니라 잡음이다.
 */
object RunTrack {

    /** 저장할 때 남길 좌표 소수점 자리수 */
    const val COORD_DECIMALS = 6

    private const val POINT_SEPARATOR = ';'
    private const val FIELD_SEPARATOR = ','

    private fun Double.trimmed(): String {
        val factor = 1_000_000.0 // 10^COORD_DECIMALS
        return (Math.round(this * factor) / factor).toString()
    }

    fun encode(points: List<TrackPoint>): String = points.joinToString(POINT_SEPARATOR.toString()) {
        "${it.lat.trimmed()}${FIELD_SEPARATOR}${it.lng.trimmed()}${FIELD_SEPARATOR}${it.at}"
    }

    /**
     * 저장된 문자열을 좌표 목록으로 되돌린다.
     *
     * 읽을 수 없는 조각은 예외를 던지지 않고 건너뛴다. 기록 화면을 여는 것이
     * 앱이 죽을 만한 일은 아니고, 한 점이 깨졌다고 나머지 경로까지 버릴 이유도
     * 없다. 실제로 쓸 수 있는 경로인지는 읽은 쪽이 개수로 판단하면 된다.
     */
    fun decode(raw: String): List<TrackPoint> {
        if (raw.isBlank()) return emptyList()
        return raw.split(POINT_SEPARATOR).mapNotNull { chunk ->
            val parts = chunk.split(FIELD_SEPARATOR)
            if (parts.size != 3) return@mapNotNull null
            val lat = parts[0].toDoubleOrNull() ?: return@mapNotNull null
            val lng = parts[1].toDoubleOrNull() ?: return@mapNotNull null
            val at = parts[2].toLongOrNull() ?: return@mapNotNull null
            TrackPoint(lat, lng, at)
        }
    }
}

/** 시각을 떼고 모양만 넘긴다 — 지도 그리기와 코스 만들기가 쓰는 형태다. */
fun List<TrackPoint>.toGeoPoints(): List<GeoPoint> = map { it.toGeoPoint() }

/** [liveHead] 가 내 점을 경로 끝에서 떼어 둘 가장 먼 거리(m) — 경로 점이 생기기 전 거리([TrackFixRule], 오차 50m 의 두 배)까지 */
const val LIVE_HEAD_MAX_M = 100.0

/** 경로 끝이 이만큼(ms) 늘지 않는 동안 새로 받은 위치는 [LIVE_HEAD_MAX_M] 보다 멀어도 내 점을 옮긴다 */
const val LIVE_HEAD_STALE_MS = 20_000L

/**
 * 달리는 중 지도의 내 점 · 화살표를 둘 자리 — 받은 위치([here]). 그리기만 하고 거리 · 저장에는 쓰지 않는다.
 *
 * 구글 위치 서비스의 위치는 기준점에서 오차보다 크게 움직여야 경로 점이 된다([TrackFixRule]) — 그동안 내 점이 경로 끝에
 * 멈춰 있지 않고 받은 위치마다 따라간다(구글 지도처럼). 경로가 없거나 · 지금 자리를 모르면 null — 경로 끝에 둔다.
 * 경로 끝에서 [LIVE_HEAD_MAX_M] 보다 먼 위치는 튄 위치일 수 있어 경로 끝에 두되, 경로 끝이 [LIVE_HEAD_STALE_MS] 넘게 늘지 않은
 * 뒤에 받은 정확한 위치면 옮긴다 — 경로 점이 드문 동안(인터넷이 끊겼다 돌아온 뒤 등) 내 점이 옛 경로 끝에 묶여 "아무리 움직여도
 * 그 자리"이던 것(2026-10-17). 흐린 위치(오차 50m 초과, [hereAccurate] false)로는 멀리 옮기지 않는다 — 기지국 위치는 수백 m
 * 틀려, 지도가 엉뚱한 곳으로 뛰었다(그동안은 "신호 약함"이 알린다).
 *
 * @param hereAt [here] 를 받은 시각(폰 시계 ms) — [TrackPoint.at] 과 같은 시계
 * @param hereAccurate [here] 가 오차 50m 안의 위치인가
 */
fun liveHead(track: List<TrackPoint>, here: GeoPoint?, hereAt: Long, hereAccurate: Boolean = true): GeoPoint? {
    val last = track.lastOrNull() ?: return null
    val at = here ?: return null
    if (haversineMeters(last.toGeoPoint(), at) <= LIVE_HEAD_MAX_M) return at
    return at.takeIf { hereAccurate && hereAt - last.at >= LIVE_HEAD_STALE_MS }
}

/**
 * 위치 신호가 끊겼다 돌아온 자리(시안 L04) — 앞 점과 이어 그리지 않을 점의 번호들.
 *
 * 저장된 경로에는 끊김 표시가 없다. 앞 점에서 [gapMs] 넘게 지나 [jumpMeters] 넘게 떨어진 점은 신호가 끊긴 사이에
 * 움직인 것이다 — 그 사이를 직선으로 이으면 달리지 않은 길이 그려진다. 제자리에 섰다 다시 출발한 점(가까운 점)은 잇는다.
 *
 * [gapMs] 는 서버가 거리를 재지 않는 빈 구간(economy.track_gap_sec, 60초)과 같다. 예전 30초는 구글 위치 서비스로 천천히 걸을 때의
 * 보통 점 간격(오차 15m 넘으면 30초 남짓)까지 끊김으로 그려, 걸은 길의 선이 대부분 사라졌다(2026-10-17 점검).
 */
fun segmentBreaks(points: List<TrackPoint>, gapMs: Long = 60_000L, jumpMeters: Double = 30.0): Set<Int> {
    if (points.size < 2) return emptySet()
    val breaks = mutableSetOf<Int>()
    for (i in 1 until points.size) {
        val a = points[i - 1]
        val b = points[i]
        if (b.at - a.at > gapMs && haversineMeters(a.toGeoPoint(), b.toGeoPoint()) > jumpMeters) breaks += i
    }
    return breaks
}
