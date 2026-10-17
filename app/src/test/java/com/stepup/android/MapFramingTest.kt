package com.stepup.android

import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.haversineMeters
import com.stepup.android.ui.components.MapTiles
import com.stepup.android.ui.components.TilePlan
import com.stepup.android.ui.components.followAnchor
import com.stepup.android.ui.components.followFrame
import com.stepup.android.ui.screens.community.stories.rangeSpan
import com.stepup.android.ui.screens.profile.historyFocus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.tan

/** 지도가 처음 무엇에 맞춰 열리나 — 그 틀로 고른 줌은 기기 검사(RouteMapFitTest)가 본다 */
class MapFramingTest {
    private val here = GeoPoint(lat = 37.5260, lng = 126.9260)

    /** 지도(메르카토르)의 세로 좌표 — 지도는 이 값으로 틀의 가운데를 잡는다 */
    private fun mapY(lat: Double) = ln(tan(PI / 4 + Math.toRadians(lat) / 2))

    @Test fun followFrameKeepsTheRunnerInTheMiddle() {
        assertEquals(listOf(here), followFrame(here, emptyList()))
        // 곁에 있는 사람(약 400m)도, 아주 먼 사람(약 22km)도 — 틀의 가운데는 지금 자리
        for (friend in listOf(GeoPoint(lat = 37.5290, lng = 126.9300), GeoPoint(lat = 37.7260, lng = 126.9260))) {
            val frame = followFrame(here, listOf(friend))
            assertTrue(friend in frame)
            assertEquals(mapY(here.lat), (mapY(frame.minOf { it.lat }) + mapY(frame.maxOf { it.lat })) / 2, 1e-12)
            assertEquals(here.lng, (frame.minOf { it.lng } + frame.maxOf { it.lng }) / 2, 1e-9)
        }
    }

    @Test fun followAnchorStaysInTheVisibleClearPart() {
        // 위 14% · 아래 28% 가 바닥색에 녹아드는 러닝 지도
        fun anchor(top: Float, bottom: Float) = followAnchor(top, bottom, 0.14f, 0.72f)
        assertEquals(0.45f, anchor(0f, 1f), 0f) // 다 보이면 또렷한 띠의 가운데
        assertEquals(0.35f, anchor(0f, 0.54f), 0f) // 390×844dp — 아래 절반이 버튼 뒤로 잘림(QA 캡처)
        assertEquals(0.25f, anchor(0f, 0.33f), 0f) // 360×780dp — 3분의 1만 보임
        assertEquals(0.65f, anchor(0.6f, 1f), 0f) // 아래로 넘겨 위가 잘림
        assertEquals(0.2f, anchor(0f, 0f), 0f) // 화면 밖
    }

    @Test fun storyRangeSpansTheWidthOnly() {
        val span = rangeSpan(here, 1_000)
        assertEquals(2, span.size)
        span.forEach {
            assertEquals(here.lat, it.lat, 0.0)
            assertEquals(1_000.0, haversineMeters(here, it), 5.0)
        }
    }

    @Test fun historyFitsTheLatestAreaNotFarTrips() {
        val latest = listOf(here, GeoPoint(lat = 37.5290, lng = 126.9290))
        val busan = listOf(GeoPoint(lat = 35.1580, lng = 129.1600), GeoPoint(lat = 35.1600, lng = 129.1620))
        // 약 2.5km 떨어진 동네 — 같은 생활권이라 함께 맞춘다
        val nearby = listOf(GeoPoint(lat = 37.5400, lng = 126.9500), GeoPoint(lat = 37.5420, lng = 126.9520))
        assertEquals(latest + nearby, historyFocus(listOf(latest, busan, nearby)))
        // 가장 최근이 여행지면 그 동네를 연다
        assertEquals(busan, historyFocus(listOf(busan, latest)))
        assertTrue(historyFocus(emptyList()).isEmpty())
    }

    @Test fun followMapIsThirtyPercentCloserByDefault() {
        // 2026-10-17 "초반에 내가 어디 있는지 지도 자체가 너무 뒤로 · 지금 기본보다 약 30% 더 확대"
        val plain = TilePlan.of(listOf(here), 1080, 1600, density = 3f)
        val closer = TilePlan.of(listOf(here), 1080, 1600, density = 3f, magnify = MapTiles.FOLLOW_MAGNIFY)
        assertEquals(MapTiles.DEFAULT_ZOOM, plain.zoom)
        assertEquals(plain.zoom, closer.zoom)
        assertEquals(plain.scale * 1.3f, closer.scale, 1e-4f)
        // 지금 자리는 그대로 화면 가운데
        assertEquals(540f, closer.toScreen(here).x, 0.5f)
        assertEquals(800f, closer.toScreen(here).y, 0.5f)
    }

    @Test fun zoomingPastTheClosestTilesKeepsMagnifying() {
        // 가장 가까운 타일 줌(18)보다 더 당기면 줌을 막지 않고 타일을 크게 그린다 — 배율이 갑자기 줄지 않게
        val at18 = TilePlan.of(listOf(here), 1080, 1600, density = 3f, zoomDelta = 1)
        val past = TilePlan.of(listOf(here), 1080, 1600, density = 3f, zoomDelta = 2)
        assertEquals(MapTiles.MAX_ZOOM, at18.zoom)
        assertEquals(MapTiles.MAX_ZOOM, past.zoom)
        assertEquals(at18.scale * 2f, past.scale, 1e-4f)
    }
}
