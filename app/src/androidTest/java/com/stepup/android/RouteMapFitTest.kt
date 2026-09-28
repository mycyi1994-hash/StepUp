package com.stepup.android

import com.stepup.android.domain.GeoPoint
import com.stepup.android.ui.components.MapTiles
import com.stepup.android.ui.components.TilePlan
import com.stepup.android.ui.components.followFrame
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

    /** 러닝 중 지도 크기(폰 너비 × 280dp) */
    private val runW = 1080
    private val runH = 735

    private fun assertCentered(plan: TilePlan, point: GeoPoint, anchorY: Float = 0.5f) {
        val at = plan.toScreen(point)
        assertEquals(runW / 2f, at.x, 2f)
        assertEquals(runH * anchorY, at.y, 2f)
    }

    @Test fun onePointOpensClose() {
        // 위치만 있을 때 — 예전 줌 16은 폰 너비로 1.5km 가 보였다
        val plan = TilePlan.of(listOf(route.first()), runW, runH, density = 2.625f)
        assertEquals(MapTiles.DEFAULT_ZOOM, plan.zoom)
        assertTrue(plan.zoom >= 17)
        assertCentered(plan, route.first())
    }

    @Test fun runningMapFollowsTheRunner() {
        // 3km 넘게 달린 뒤에도 경로 전체로 물러나지 않고 지금 자리를 가운데 가깝게 본다
        val here = GeoPoint(lat = 37.5500, lng = 126.9600)
        val plan = TilePlan.of(followFrame(here, emptyList()), runW, runH, density = 2.625f, zooms = MapTiles.FOLLOW_ZOOMS)
        assertEquals(MapTiles.DEFAULT_ZOOM, plan.zoom)
        assertCentered(plan, here)

        // 같이 뛰는 사람이 가까우면(약 220m) 가운데는 그대로 두고 함께 보인다
        val near = GeoPoint(lat = here.lat + 0.002, lng = here.lng)
        val together = TilePlan.of(followFrame(here, listOf(near)), runW, runH, density = 2.625f, zooms = MapTiles.FOLLOW_ZOOMS)
        assertCentered(together, here)
        val at = together.toScreen(near)
        assertTrue("friend on screen: $at", at.x in 0f..runW.toFloat() && at.y in 0f..runH.toFloat())

        // 아주 멀면(약 22km) 15 아래로는 물러나지 않는다 — 그 사람은 아래 순위에서 본다
        val far = GeoPoint(lat = here.lat + 0.2, lng = here.lng)
        val apart = TilePlan.of(followFrame(here, listOf(far)), runW, runH, density = 2.625f, zooms = MapTiles.FOLLOW_ZOOMS)
        assertEquals(MapTiles.FOLLOW_ZOOMS.first, apart.zoom)
        assertCentered(apart, here)
        // 지도 아래가 잘려 보이면 지금 자리를 보이는 쪽(위에서 35%)에 — 곁의 사람도 그 높이에서 들어오게 맞춘다
        val raised = TilePlan.of(followFrame(here, listOf(near)), runW, runH, density = 2.625f, zooms = MapTiles.FOLLOW_ZOOMS, anchorY = 0.35f)
        assertCentered(raised, here, anchorY = 0.35f)
        val seen = raised.toScreen(near)
        assertTrue("friend on screen: $seen", seen.x in 0f..runW.toFloat() && seen.y in 0f..runH.toFloat())
    }

    @Test fun mapWithinTheBudgetKeepsItsScale() {
        // 상세 카드 크기 — 예전과 같은 배율 · 줌
        val plan = TilePlan.of(route, 900, 520, density = 2.625f)
        assertEquals(2.625f / 2f, plan.scale, 0.0001f)
        assertTrue(tiles(plan) <= MapTiles.MAX_TILES)
        assertTrue(plan.zoom >= 14)
    }
}
