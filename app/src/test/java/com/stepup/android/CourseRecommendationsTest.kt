package com.stepup.android

import com.stepup.android.domain.CourseRecommendations
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.RunCourse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 추천 코스(U04 · K02 · K03) — 지금 자리에서 가까운 코스만, 가까운 순으로. 체험 코스 · 겹치는 길은 빼고 */
class CourseRecommendationsTest {
    private val here = GeoPoint(37.5200, 126.9400)

    private fun course(id: Long, dLat: Double, dLng: Double, km: Double = 2.0, author: String = "runner") = RunCourse(
        id = id, name = "c$id", area = "", distanceKm = km, elevationM = 0,
        points = listOf(GeoPoint(here.lat + dLat, here.lng + dLng), GeoPoint(here.lat + dLat + 0.002, here.lng + dLng)),
        author = author, mine = false, shared = true, likes = 0, liked = false, runCount = 0, createdAt = 0,
    )

    @Test fun `가까운 코스부터 반경 밖은 뺀다`() {
        val near = course(1, 0.003, 0.0)       // 약 330m
        val nearer = course(2, 0.001, 0.0)     // 약 110m
        val far = course(3, 0.05, 0.0)         // 약 5.5km
        val picks = CourseRecommendations.near(here, listOf(near, far, nearer))
        assertEquals(listOf(2L, 1L), picks.map { it.course.id })
        assertTrue(picks.first().toCourseMeters < 150)
    }

    @Test fun `빼라고 한 코스와 같은 길은 한 번만`() {
        val demo = course(1, 0.001, 0.0, author = "StepUp")
        val mine = course(2, 0.002, 0.0)
        val copy = mine.copy(id = 9)
        val picks = CourseRecommendations.near(here, listOf(demo, mine, copy), exclude = { it.author == "StepUp" })
        assertEquals(listOf(2L), picks.map { it.course.id })
    }

    @Test fun `없으면 빈 목록`() {
        assertTrue(CourseRecommendations.near(here, emptyList()).isEmpty())
        assertTrue(CourseRecommendations.near(here, listOf(course(1, 0.2, 0.2))).isEmpty())
    }

    @Test fun `예상 시간과 거리 글`() {
        assertEquals(19, CourseRecommendations.minutes(2.4))
        assertEquals(1, CourseRecommendations.minutes(0.01))
        assertEquals("350m", CourseRecommendations.distanceLabel(347.0))
        assertEquals("1.2km", CourseRecommendations.distanceLabel(1_190.0))
    }
}
