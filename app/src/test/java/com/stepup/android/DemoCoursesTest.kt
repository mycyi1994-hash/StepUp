package com.stepup.android

import com.stepup.android.domain.DemoCourses
import com.stepup.android.domain.haversineMeters
import com.stepup.android.domain.trackDistanceKm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 데모 코스가 지켜야 하는 것.
 *
 * 이 코스들은 지도 위에 그려진다. 선이 강이나 건물을 가로지르면 그건 코스가
 * 아니라 낙서고, 받은 사람은 그대로 뛸 수 없다. 예전 데모 코스는 400m쯤
 * 띄엄띄엄 찍은 열두 점을 직선으로 이은 것이라 한강을 가로질렀다.
 *
 * 여기서 못 박는 것은 **공원 상자 안에 있다**와 **선이 촘촘하다** 둘이다.
 * 실제 산책로 중심선 위에 있는지는 지도 데이터가 있어야 알 수 있고, 그건
 * 이 검사가 할 수 있는 일이 아니다 — 정확한 코스는 사용자가 직접 뛰어서
 * 만든다(코스 만들기).
 */
class DemoCoursesTest {

    @Test
    fun `모든 점이 공원 상자 안에 있다`() {
        DemoCourses.parks.forEach { park ->
            val outside = park.track().filterNot { park.contains(it) }
            assertTrue(
                "${park.name}: ${outside.size}개 점이 공원 밖으로 나갔다 — 강이나 찻길 위에 그려진다",
                outside.isEmpty(),
            )
        }
    }

    @Test
    fun `점 간격이 촘촘해 선이 각지지 않는다`() {
        DemoCourses.parks.forEach { park ->
            val track = park.track()
            val gaps = (1 until track.size).map { haversineMeters(track[it - 1], track[it]) }
            val worst = gaps.max()
            // 곡선을 조각내는 기준이 직선 거리라 실제 간격은 그보다 조금 길다.
            // 그래도 간격의 1.5배를 넘으면 눈에 띄게 각진다.
            assertTrue(
                "${park.name}: 가장 먼 점 사이가 %.0fm 다".format(worst),
                worst <= DemoCourses.SPACING_M * 1.5,
            )
        }
    }

    @Test
    fun `고리가 닫혀 있어 출발지로 돌아온다`() {
        DemoCourses.parks.forEach { park ->
            val track = park.track()
            assertEquals("${park.name}: 끝점이 시작점과 다르다", track.first(), track.last())
        }
    }

    @Test
    fun `러닝 코스라 할 만한 길이다`() {
        DemoCourses.parks.forEach { park ->
            val km = park.track().trackDistanceKm()
            // 1km 아래면 코스라기엔 짧고, 10km 위면 공원 상자를 의심해야 한다.
            assertTrue("${park.name}: %.2f km".format(km), km in 1.0..10.0)
        }
    }

    @Test
    fun `공원마다 다른 자리에 있다`() {
        val centers = DemoCourses.parks.map { it.center }
        assertEquals(centers.size, centers.toSet().size)
    }
}
