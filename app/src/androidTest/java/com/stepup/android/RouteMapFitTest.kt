package com.stepup.android

import com.stepup.android.domain.GeoPoint
import com.stepup.android.ui.components.MapTiles
import com.stepup.android.ui.components.TilePlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 지도 맞추기 — 화면을 덮는 타일이 예산을 넘는 큰 지도(러닝 기록의 경로 확대)도 경로에 맞춘 줌으로 연다.
 * 예전에는 예산을 넘으면 모든 줌에서 떨어져 세계 지도(줌 3)로 열렸다.
 */
class RouteMapFitTest {
    /** 여의도 한 바퀴쯤(약 700m 사각형) */
    private val route = listOf(
        GeoPoint(lat = 37.5260, lng = 126.9230),
        GeoPoint(lat = 37.5290, lng = 126.9260),
        GeoPoint(lat = 37.5260, lng = 126.9290),
        GeoPoint(lat = 37.5230, lng = 126.9260),
    )

    private fun tiles(plan: TilePlan) = (plan.maxTileX - plan.minTileX + 1) * (plan.maxTileY - plan.minTileY + 1)

    @Test fun largeMapStillFitsTheRoute() {
        val width = 1080
        val height = 1400
        val plan = TilePlan.of(route, width, height, density = 2.625f)
        assertTrue("fitted zoom, not the world: ${plan.zoom}", plan.zoom >= 14)
        assertTrue("tiles within the budget: ${tiles(plan)}", tiles(plan) <= MapTiles.MAX_TILES)
        route.forEach { point ->
            val at = plan.toScreen(point)
            assertTrue("route on screen: $at", at.x in 0f..width.toFloat() && at.y in 0f..height.toFloat())
        }
    }

    @Test fun mapWithinTheBudgetKeepsItsScale() {
        // 상세 카드 크기 — 예전과 같은 배율 · 줌
        val plan = TilePlan.of(route, 900, 520, density = 2.625f)
        assertEquals(2.625f / 2f, plan.scale, 0.0001f)
        assertTrue(tiles(plan) <= MapTiles.MAX_TILES)
        assertTrue(plan.zoom >= 14)
    }
}
