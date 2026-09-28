package com.stepup.android

import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.haversineMeters
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
}
