package com.stepup.android

import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.TrackPoint
import com.stepup.android.domain.haversineMeters
import com.stepup.android.ui.screens.walk.FREE_PACE_MIN_KMH
import com.stepup.android.ui.screens.walk.FreeBand
import com.stepup.android.ui.screens.walk.FreeDistance
import com.stepup.android.ui.screens.walk.SPEED_STILL_MS
import com.stepup.android.ui.screens.walk.formatPacePrime
import com.stepup.android.ui.screens.walk.freeAveragePaceSec
import com.stepup.android.ui.screens.walk.freeBand
import com.stepup.android.ui.screens.walk.freeDistance
import com.stepup.android.ui.screens.walk.freePaceSec
import com.stepup.android.ui.screens.walk.freeResultKm
import com.stepup.android.ui.screens.walk.freeShareTextRes
import com.stepup.android.ui.screens.walk.freeSpeedKmh
import com.stepup.android.ui.screens.walk.freeTimeDistance
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 자유 러닝 v2(2026-10-14) — 지도 위 띠 · 지금 속도 · 페이스 · GPS 거리 · 공유 글. 보상 UI 없음 */
class FreeRunUiTest {
    private val start = 1_700_000_000_000L
    private lateinit var before: Locale

    @Before fun setUp() {
        // 시트의 "24:18 · 3.24 km" 는 소수점을 로케일로 찍는다 — 점으로 고정해서 잰다
        before = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After fun tearDown() {
        Locale.setDefault(before)
    }

    /** 북쪽으로 [step] 도씩, [gapMs] 간격 — 0.000225 도는 25m 남짓이라 10초 간격이면 시속 9km 쯤 */
    private fun track(count: Int, gapMs: Long, step: Double = 0.000225): List<TrackPoint> =
        (0 until count).map { i -> TrackPoint(37.5 + step * i, 127.0, start + gapMs * i) }

    private fun meters(a: TrackPoint, b: TrackPoint) = haversineMeters(GeoPoint(a.lat, a.lng), GeoPoint(b.lat, b.lng))

    /** 기본은 문제 없이 달리는 중(위치 허용 · 켜짐 · 정확한 위치) — 띠가 없다 */
    private fun band(
        active: Boolean = true,
        paused: Boolean = false,
        asking: Boolean = false,
        locationAllowed: Boolean = true,
        locationOn: Boolean = true,
        gpsLost: Boolean = false,
        precise: Boolean = true,
        void: Boolean = false,
        speedFlagged: Boolean = false,
    ) = freeBand(
        active = active, paused = paused, asking = asking, locationAllowed = locationAllowed, locationOn = locationOn,
        gpsLost = gpsLost, precise = precise, void = void, speedFlagged = speedFlagged,
    )

    /** 기본은 위치를 재며 달리는 중 */
    private fun speed(
        track: List<TrackPoint>,
        now: Long,
        paused: Boolean = false,
        locationAllowed: Boolean = true,
        locationOn: Boolean = true,
        gpsLost: Boolean = false,
        void: Boolean = false,
        flaggedAt: Long = 0L,
    ) = freeSpeedKmh(
        paused = paused, locationAllowed = locationAllowed, locationOn = locationOn, gpsLost = gpsLost,
        void = void, track = track, now = now, flaggedAt = flaggedAt,
    )

    // ── 지도 위 띠 ──────────────────────────────────────────────

    @Test
    fun `문제 없이 달리면 띠가 없다 — 위치를 찾는 중에도`() {
        // 2026-10-17 "그 글도 없애고" — "GPS 찾는 중" · "대략적인 위치 · GPS 찾는 중" 띠는 없앴다(지도 앱처럼 잡히는 대로 내 점이 뜬다).
        // 띠는 GPS 를 잡았는지 보지 않는다
        assertEquals(FreeBand.None, band())
    }

    @Test
    fun `띠는 하나만 — 위치 꺼짐 · 측정 무효 · 속도 오류 · 신호 약함 · 대략 위치 · 일시정지 순서로 앞선다`() {
        // 문제를 모두 켠 뒤 앞선 것부터 하나씩 끄면 그다음 띠가 나온다
        var on = mapOf(
            "locationOn" to false, "void" to true, "speedFlagged" to true, "gpsLost" to true,
            "precise" to false, "paused" to true,
        )
        fun current() = band(
            paused = on.getValue("paused"), locationOn = on.getValue("locationOn"),
            gpsLost = on.getValue("gpsLost"), precise = on.getValue("precise"),
            void = on.getValue("void"), speedFlagged = on.getValue("speedFlagged"),
        )
        val steps = listOf(
            FreeBand.LocationOff to ("locationOn" to true),
            FreeBand.MeasureInvalid to ("void" to false),
            FreeBand.SpeedError to ("speedFlagged" to false),
            FreeBand.Weak to ("gpsLost" to false),
            FreeBand.Approx to ("precise" to true),
            FreeBand.Paused to ("paused" to false),
        )
        for ((expected, next) in steps) {
            assertEquals("${expected.name} 차례", expected, current())
            on = on + next
        }
        assertEquals(FreeBand.None, current())
    }

    @Test
    fun `달리다 위치 기능을 끄면 위치 꺼짐 띠`() {
        assertEquals(FreeBand.LocationOff, band(locationOn = false))
    }

    @Test
    fun `러닝 전과 마칠지 묻는 동안에는 띠가 없다`() {
        // 문제가 모두 있어도 — 시작 전이거나 마침 · 지우기 시트가 덮고 있다
        val all = { active: Boolean, asking: Boolean ->
            band(
                active = active, asking = asking, paused = true, locationOn = false, gpsLost = true,
                precise = false, void = true, speedFlagged = true,
            )
        }
        assertEquals(FreeBand.None, all(false, false))
        assertEquals(FreeBand.None, all(true, true))
        assertEquals(FreeBand.None, all(false, true))
        assertEquals(FreeBand.LocationOff, all(true, false))
    }

    @Test
    fun `위치 권한 없이 시간만 기록하면 띠가 없다 — 고른 방식이라 문제가 아니다`() {
        assertEquals(
            FreeBand.None,
            band(locationAllowed = false, locationOn = false, gpsLost = true, precise = false),
        )
    }

    @Test
    fun `위치 권한 없이 일시정지하면 일시정지 띠만`() {
        assertEquals(
            FreeBand.Paused,
            band(locationAllowed = false, paused = true, locationOn = false, gpsLost = true, precise = false),
        )
    }

    // ── 지금 속도 ──────────────────────────────────────────────

    @Test
    fun `달리는 중에는 경로의 점으로 잰 지금 속도`() {
        val points = track(6, 10_000)
        // 마지막 점과 그 앞 20초 안의 점(두 칸 앞까지) — 50m 남짓을 20초에
        val expected = (meters(points[3], points[4]) + meters(points[4], points[5])) / 20.0 * 3.6
        val kmh = speed(points, points.last().at + 1_000)!!
        assertEquals(expected, kmh, 0.01)
        assertEquals(9.0, kmh, 0.5)
    }

    @Test
    fun `측정 무효면 속도를 모른다`() {
        val points = track(6, 10_000)
        assertNull(speed(points, points.last().at + 1_000, void = true))
        // 일시정지 중이어도 — 무효가 앞선다
        assertNull(speed(points, points.last().at + 1_000, void = true, paused = true))
    }

    @Test
    fun `위치 권한이 없거나 위치 기능이 꺼졌으면 속도를 모른다`() {
        val points = track(6, 10_000)
        val now = points.last().at + 1_000
        assertNull(speed(points, now, locationAllowed = false))
        assertNull(speed(points, now, locationOn = false))
        // 일시정지 중이어도 0 이 아니라 모른다
        assertNull(speed(points, now, locationAllowed = false, paused = true))
        assertNull(speed(points, now, locationOn = false, paused = true))
    }

    @Test
    fun `위치를 재며 일시정지하면 속도는 0`() {
        val points = track(6, 10_000)
        assertEquals(0.0, speed(points, points.last().at + 1_000, paused = true)!!, 0.0)
        assertEquals(0.0, speed(emptyList(), start, paused = true)!!, 0.0)
        // 신호가 약해도 멈춰 있는 것은 안다
        assertEquals(0.0, speed(points, points.last().at + 1_000, paused = true, gpsLost = true)!!, 0.0)
    }

    @Test
    fun `신호가 약하면 속도를 모른다`() {
        val points = track(6, 10_000)
        assertNull(speed(points, points.last().at + 1_000, gpsLost = true))
    }

    @Test
    fun `방금 읽은 속도가 버려졌으면 모른다`() {
        val points = track(6, 10_000)
        assertNull(speed(points, points.last().at + 2_000, flaggedAt = points.last().at + 1_000))
    }

    @Test
    fun `새 점이 한동안 없으면 멈춰 선 것이라 0`() {
        val points = track(6, 10_000)
        assertEquals(0.0, speed(points, points.last().at + SPEED_STILL_MS + 1)!!, 0.0)
    }

    // ── 페이스 ──────────────────────────────────────────────────

    @Test
    fun `속도를 모르거나 시속 1km 아래면 페이스를 적지 않는다`() {
        assertNull(freePaceSec(null))
        assertNull(freePaceSec(0.0))
        assertNull(freePaceSec(0.99))
        assertNull(freePaceSec(-3.0))
        assertNull(freePaceSec(Double.NaN))
        assertEquals(3_600L, freePaceSec(FREE_PACE_MIN_KMH))
    }

    @Test
    fun `시속 8km 는 7분 30초 페이스`() {
        assertEquals(450L, freePaceSec(8.0))
    }

    @Test
    fun `페이스는 음수나 NaN 이 되지 않는다`() {
        for (kmh in listOf(1.0, 1.5, 3.3, 8.0, 12.7, 20.0, 45.0, Double.MAX_VALUE, Double.POSITIVE_INFINITY)) {
            val sec = freePaceSec(kmh)!!
            assertTrue("시속 $kmh → $sec 초", sec >= 0L)
        }
        for (kmh in listOf(Double.NEGATIVE_INFINITY, -0.0, -1.0, Double.NaN)) {
            assertNull("시속 $kmh", freePaceSec(kmh))
        }
    }

    @Test
    fun `페이스는 프라임 기호로 적는다`() {
        // ′(U+2032) · ″(U+2033) — 다른 모드의 7'30" 와 다르다
        assertEquals("7′30″", formatPacePrime(450))
        assertEquals("1′05″", formatPacePrime(65))
    }

    @Test
    fun `평균 페이스는 GPS 로 50m 를 넘게 달렸을 때만`() {
        assertNull(freeAveragePaceSec(null, 750))
        assertNull(freeAveragePaceSec(0.04, 750))
        assertNull(freeAveragePaceSec(1.56, 0))
        // 750초 / 1.56km = 480.7… → 버려서 8′00″
        assertEquals(480L, freeAveragePaceSec(1.56, 750))
    }

    // ── 거리 ────────────────────────────────────────────────────

    @Test
    fun `러닝 중 거리는 GPS 로 잰 것만 — 걸음 거리는 받지도 않는다`() {
        // 위치 없이 기록하면 GPS 거리가 있어도 모른다("—")
        assertEquals(FreeDistance.Unknown, freeDistance(timeOnly = true, gpsKm = 0.0))
        assertEquals(FreeDistance.Unknown, freeDistance(timeOnly = true, gpsKm = 1.56))
        // 위치를 쓰면 경로가 생기기 전에도 0.00 부터 적는다 — "경로 대기 중"은 없앴다(2026-10-17 지도 앱처럼)
        assertEquals(FreeDistance.Km(0.0), freeDistance(timeOnly = false, gpsKm = 0.0))
        assertEquals(FreeDistance.Km(1.56), freeDistance(timeOnly = false, gpsKm = 1.56))
        // 거리는 음수가 되지 않는다
        assertEquals(FreeDistance.Km(0.0), freeDistance(timeOnly = false, gpsKm = -0.01))
    }

    @Test
    fun `저장한 결과의 거리는 GPS 거리가 있을 때만`() {
        assertNull(freeResultKm(0.0))
        assertNull(freeResultKm(-0.5))
        assertEquals(1.56, freeResultKm(1.56)!!, 0.0)
    }

    @Test
    fun `시트의 한 줄은 시간 · 거리, 거리를 모르면 시간만`() {
        assertEquals("24:18 · 3.24 km", freeTimeDistance("24:18", FreeDistance.Km(3.24)))
        assertEquals("24:18", freeTimeDistance("24:18", FreeDistance.Unknown))
    }

    // ── 공유 글 ─────────────────────────────────────────────────

    @Test
    fun `공유 글에는 보상이 든 글을 쓰지 않는다`() {
        for (km in listOf(0.0, 1.56)) {
            assertNotEquals("${km}km", R.string.finish_share_text, freeShareTextRes(km))
        }
    }

    @Test
    fun `공유 글은 거리를 알면 거리 · 시간, 모르면 시간만`() {
        assertEquals(R.string.run_free_share_time, freeShareTextRes(0.0))
        assertEquals(R.string.finish_share_activity, freeShareTextRes(1.56))
    }
}
